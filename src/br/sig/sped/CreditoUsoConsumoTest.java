package br.sig.sped;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Teste de integração com SPED fornecido para ensaio; original jamais é alterado. */
public final class CreditoUsoConsumoTest {
    private static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    public static void main(String[] args) throws Exception {
        File entrada=new File(args[0]);
        SpedParser.Resultado resultado=SpedParser.ler(entrada);
        System.out.println("Registro e arquivo: "+resultado.codVer+" "+resultado.uf+
                ", alertas="+resultado.ocorrencias.size());
        List<CreditoUsoConsumo.Caso> casos=CreditoUsoConsumo.analisar(resultado);
        BigDecimal somatorio=BigDecimal.ZERO;
        Set<Integer> todos=new HashSet<Integer>();
        for(CreditoUsoConsumo.Caso c:casos){
            System.out.println(c.nota+" C190 L"+c.linhaC190+" R$"+c.credito+" "+c.motivo);
            somatorio=somatorio.add(c.credito);
            if(c.elegivel)todos.add(c.linhaC190);
        }
        System.out.println("Candidatos="+casos.size()+" conciliados="+todos.size()+" credito="+somatorio);
        check(somatorio.compareTo(new BigDecimal("9070.45"))==0,"total previsto");
        check(casos.size()==19 && todos.size()==19,"19 agrupamentos devem conciliar");
        byte[] original=Files.readAllBytes(entrada.toPath());
        CreditoUsoConsumo.Plano um=CreditoUsoConsumo.planejar(resultado,Collections.singleton(344),true);
        check(um.grupos==1 && um.documentos==1 && um.valorCredito.compareTo(new BigDecimal("3571.92"))==0,"um grupo");
        File output=Files.createTempFile("sped-audit-one-", ".txt").toFile(); Files.delete(output.toPath());
        CreditoUsoConsumo.salvar(um,output);
        SpedParser.Resultado parcial=SpedParser.ler(output);
        String primeiraLinha=Files.readAllLines(output.toPath(),java.nio.charset.Charset.forName(parcial.charset)).get(0);
        check(primeiraLinha.startsWith("|0000|020|1|"),"COD_FIN substituta=1");
        check(parcial.apuracoes.get(0).creditos.compareTo(new BigDecimal("5498.53"))==0,"E110 parcial");
        check(parcial.apuracoes.get(0).credorTransportar.compareTo(new BigDecimal("5498.53"))==0,"saldo parcial");
        check(parcial.analiticos.size()==resultado.analiticos.size(),"C190 preservados");
        check(parcial.itens.size()==resultado.itens.size(),"C170 preservados");
        File outputAll=Files.createTempFile("sped-audit-all-", ".txt").toFile();Files.delete(outputAll.toPath());
        CreditoUsoConsumo.Plano tudo=CreditoUsoConsumo.planejar(resultado,todos);
        CreditoUsoConsumo.salvar(tudo,outputAll);
        SpedParser.Resultado zerado=SpedParser.ler(outputAll);
        check(zerado.apuracoes.get(0).creditos.compareTo(BigDecimal.ZERO)==0,"E110 zerado");
        check(zerado.apuracoes.get(0).credorTransportar.compareTo(BigDecimal.ZERO)==0,"saldo zerado");
        check(CreditoUsoConsumo.analisar(zerado).isEmpty(),"achados zerados");
        check(Files.readAllBytes(entrada.toPath()).length==original.length,"original tamanho");
        check(java.util.Arrays.equals(Files.readAllBytes(entrada.toPath()),original),"original intocado");
        boolean bloqueado=false;
        try{CreditoUsoConsumo.salvar(tudo,entrada);}catch(java.io.IOException ex){bloqueado=true;}
        check(bloqueado,"sobrescrita original bloqueada");
        bloqueado=false;
        try{CreditoUsoConsumo.salvar(tudo,outputAll);}catch(java.io.IOException ex){bloqueado=true;}
        check(bloqueado,"sobrescrita arquivo existente bloqueada");
        Files.delete(output.toPath());Files.delete(outputAll.toPath());
        System.out.println("PASSOU: 19 grupos, 1 nota, 14 notas, parcial/total, E110, proteção do original e arquivos existentes.");
    }
}
