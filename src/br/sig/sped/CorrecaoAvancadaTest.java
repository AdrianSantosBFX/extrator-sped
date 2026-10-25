package br.sig.sped;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.math.*;

/** Ensaios em cópias temporárias. Não altera EFD original nem o XML do fornecedor. */
public final class CorrecaoAvancadaTest {
    static void ok(boolean cond,String msg){if(!cond)throw new AssertionError(msg);}
    static String[] campo(String l){return l.split("\\|",-1);}
    static BigDecimal n(String v){return new BigDecimal(v.replace(',','.'));}
    static List<String[]> registros(Path p)throws Exception {
        List<String[]> r=new ArrayList<String[]>();
        SpedParser.Resultado d=SpedParser.ler(p.toFile());
        for(String line:Files.readAllLines(p,Charset.forName(d.charset)))if(!line.isEmpty())r.add(campo(line));
        return r;
    }
    static long count(List<String[]> dados,String r){long n=0;for(String[] row:dados)if(row.length>1&&r.equals(row[1]))n++;return n;}
    static String[] doc(List<String[]> r,String nota){for(String[] x:r)if(x.length>8&&x[1].equals("C100")&&x[8].equals(nota))return x;return null;}
    public static void main(String[] args)throws Exception {
        File origem=new File(args[0]);
        byte[] original=Files.readAllBytes(origem.toPath());
        SpedParser.Resultado importado=SpedParser.ler(origem);
        Set<Integer> selecao=new LinkedHashSet<Integer>();
        for(CreditoUsoConsumo.Caso c:CreditoUsoConsumo.analisar(importado))if(c.elegivel)selecao.add(c.linhaC190);
        ok(selecao.size()==19,"19 grupos elegiveis");
        CreditoUsoConsumo.Plano all=CreditoUsoConsumo.planejar(importado,selecao,true);
        Path temp=Files.createTempFile("sped-v019-", ".sped");Files.delete(temp);
        CreditoUsoConsumo.salvar(all,temp.toFile());
        List<String[]> dados=registros(temp);
        SpedParser.Resultado parsed=SpedParser.ler(temp.toFile());
        File trilha=CreditoUsoConsumo.salvarRelatorio(all,temp.toFile());
        String rel=new String(Files.readAllBytes(trilha.toPath()),StandardCharsets.UTF_8);
        ok(rel.contains("SHA256 origem")&&rel.contains("SHA256 saída")
           &&rel.contains("PENDÊNCIAS APÓS A CORREÇÃO"),"trilha de auditoria com hash e pendencias");
        ok(PendenciasTributarias.analisar(parsed).basesSemCredito>0,
           "bases remanescentes devem continuar destacadas");
        ok("1".equals(parsed.codFin),"COD_FIN=1");
        ok(parsed.apuracoes.get(0).creditos.signum()==0,"E110 zero");
        ok(parsed.apuracoes.get(0).credorTransportar.signum()==0,"saldo zero");
        ok(parsed.ocorrencias.isEmpty(),"sem alertas sintaticos adicionais");
        ok(CreditoUsoConsumo.analisar(parsed).isEmpty(),"sem credito em CFOP de uso consumo");
        ok(doc(dados,"8974")[12].equals("62000"),"valor NF preservado");
        ok(doc(dados,"8974")[21].equals("30838,95"),"base remanescente nao usada e preservada para revisao");
        ok(count(dados,"C170")==importado.itens.size(),"preserva itens");
        for(String[] l:dados)if(l.length>15&&"C170".equals(l[1])&&"1556".equals(l[11])){
            if("090".equals(l[10])||"290".equals(l[10])){
                ok(n(l[13]).signum()==0,"base 0 nos reclassificados");
                ok(n(l[14]).signum()==0,"aliquota 0 nos reclassificados");
                ok(n(l[15]).signum()==0,"credito 0 nos reclassificados");
            }
        }
        Map<String,Integer> keys=new HashMap<String,Integer>();
        String note="";
        for(String[] l:dados){
            if(l[1].equals("C100"))note=l[8];
            if(l[1].equals("C190")){
                String key=note+"|"+l[2]+"|"+l[3]+"|"+n(l[4]).stripTrailingZeros().toPlainString();
                ok(!keys.containsKey(key),"C190 duplicado "+key);
                keys.put(key,1);
            }
        }
        for(String[] l:dados){
            if(l[1].equals("C990"))ok(Integer.parseInt(l[2])==countBlock(dados,'C'),"C990 correto");
            if(l[1].equals("9900")&&l[2].equals("C190"))ok(Integer.parseInt(l[3])==count(dados,"C190"),"9900 C190 atualizado");
            if(l[1].equals("9999"))ok(Integer.parseInt(l[2])==dados.size(),"9999 atualizado");
        }
        ok(Arrays.equals(Files.readAllBytes(origem.toPath()),original),"origem preservada");
        boolean blocked=false;try{CreditoUsoConsumo.salvar(all,origem);}catch(IOException e){blocked=true;}ok(blocked,"nao sobrescreve original");
        blocked=false;try{CreditoUsoConsumo.salvar(all,temp.toFile());}catch(IOException e){blocked=true;}ok(blocked,"nao sobrescreve existente");
        // Plano a partir de arquivo já retificador mantém COD_FIN 1.
        Path temp2=Files.createTempFile("sped-v019-ret-", ".sped");Files.delete(temp2);
        CreditoUsoConsumo.Plano parcial=CreditoUsoConsumo.planejar(importado,Collections.singleton(344),true);
        CreditoUsoConsumo.salvar(parcial,temp2.toFile());
        SpedParser.Resultado ret=SpedParser.ler(temp2.toFile());
        CreditoUsoConsumo.Caso restante=null;
        for(CreditoUsoConsumo.Caso c:CreditoUsoConsumo.analisar(ret))if(c.elegivel){restante=c;break;}
        ok(restante!=null,"arquivo retificadora nao bloqueia edicao");
        CreditoUsoConsumo.Plano retPlan=CreditoUsoConsumo.planejar(ret,Collections.singleton(restante.linhaC190),true);
        ok(retPlan.valorCredito.signum()>0,"permite correcoes sucessivas em retificadora");
        if(args.length>1){
            File legado=new File(args[1]);
            SpedParser.Resultado antigo=SpedParser.ler(legado);
            ok("1".equals(antigo.codFin),"legado deve ser substituta");
            Set<Integer> corrigirLegado=new LinkedHashSet<Integer>();
            for(CreditoUsoConsumo.Caso c:CreditoUsoConsumo.analisar(antigo))if(c.elegivel)corrigirLegado.add(c.linhaC190);
            ok(corrigirLegado.size()==19,"legado v018 com 19 grupos CST/ALIQ pendentes: "+corrigirLegado.size());
            CreditoUsoConsumo.Plano planoLegado=CreditoUsoConsumo.planejar(antigo,corrigirLegado,true);
            ok(planoLegado.valorCredito.signum()==0,"créditos já zero no legado");
            Path out=Files.createTempFile("sped-legado-migrado-", ".sped");Files.delete(out);
            CreditoUsoConsumo.salvar(planoLegado,out.toFile());
            SpedParser.Resultado migrado=SpedParser.ler(out.toFile());
            ok("1".equals(migrado.codFin),"mantém substituta");
            ok(CreditoUsoConsumo.analisar(migrado).isEmpty(),"legado migrado sem pendências selecionáveis");
            ok(migrado.apuracoes.get(0).creditos.signum()==0,"créditos continuam zero");
            Files.delete(out);
            System.out.println("PASSOU: retificadora produzida pela v0.1.8 migrada para CST/ALIQ da 0.1.9");
        }
        System.out.println("PASSOU: 19 grupos, CST/ALIQUOTA, somatorios, C190 "+importado.analiticos.size()+"->"+parsed.analiticos.size()+", C990/9900/9999, retificadora, original intacto.");
        Files.delete(temp);Files.delete(temp2);Files.delete(trilha.toPath());
    }
    static long countBlock(List<String[]> linhas,char prefix){long n=0;for(String[] l:linhas)if(l[1].charAt(0)==prefix)n++;return n;}
}
