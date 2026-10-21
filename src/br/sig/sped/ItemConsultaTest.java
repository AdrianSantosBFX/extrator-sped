package br.sig.sped;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class ItemConsultaTest {
    private static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        ok(r.documentos.size()==3,"3 C100");
        ok(r.itens.size()==2,"2 C170");
        ok(r.produtos.size()==4,"4 0200 inclusive produto sem movimento");
        ok(r.documentos.get(0).quantidadeC170==2,"vinculo do doc 1 com C170");
        ok(r.documentos.get(1).quantidadeC170==0,"saida propria sem item C170 nao vira erro");
        ok(r.itens.get(0).documento==r.documentos.get(0),"C170 doc correto");
        ok(r.itens.get(1).produto!=null && r.itens.get(1).produto.codigo.equals("MAT002"),"vinculo 0200");
        ok("73181500".equals(r.itens.get(0).ncm()),"NCM de 0200");
        ok(r.itens.get(0).quantidade.compareTo(new BigDecimal("12.00000"))==0,"decimal com 5 casas");
        ok(r.itens.get(0).valor.compareTo(new BigDecimal("600.00"))==0,"valor item correto");
        ok(r.itens.get(0).icms.compareTo(new BigDecimal("108.00"))==0,"ICMS de C170 correto");
        ok(ItemConsulta.documentosSemItens(r)==2,"C100 sem C170 contado sem ser rotulado erro");
        ok(r.ocorrencias.isEmpty(),"amostra sem ocorrencias");
        ItemConsulta.Filtro todos=new ItemConsulta.Filtro("","","","","Todas");
        List<SpedParser.Item> encontrados=ItemConsulta.filtrar(r.itens,todos);
        ok(encontrados.size()==2,"todos itens");
        ItemConsulta.Totais totals=ItemConsulta.totalizar(encontrados);
        ok(totals.valor.compareTo(new BigDecimal("1000.00"))==0,"total declarado em itens");
        ok(totals.icms.compareTo(new BigDecimal("180.00"))==0,"soma ICMS de itens");
        ok(ItemConsulta.filtrar(r.itens,new ItemConsulta.Filtro("BUCHA","3926","1102","000","Entrada")).size()==1,"filtros combinados + acento");
        ok(ItemConsulta.filtrar(r.itens,new ItemConsulta.Filtro("PARAFUSO","3926","","","Todas")).isEmpty(),"intersecao nao confunde produto");
        ok(ItemConsulta.filtrar(r.itens,new ItemConsulta.Filtro("","","","","Saída")).isEmpty(),"sem C170 de saída");
        ok(ItemConsulta.corresponde(r.produtos.get("SEMUSO"),"chapa","7208"),"busca produto sem C170");
        // Confere que COD_ITEM sem 0200 gera alerta cadastral e ainda conserva os dados brutos do C170.
        String raw=new String(Files.readAllBytes(new File(args[0]).toPath()),StandardCharsets.UTF_8);
        raw=raw.replace("|C170|1|MAT001|","|C170|1|AUSENTE| ");
        File scratch=File.createTempFile("sped016-sem-0200", ".txt");
        try{
            Files.write(scratch.toPath(),raw.getBytes(StandardCharsets.UTF_8));
            SpedParser.Resultado second=SpedParser.ler(scratch);
            ok(second.itens.size()==2 && second.itens.get(0).produto==null,"C170 preservado sem 0200");
            ok(ItemConsulta.totalizar(second.itens).semCadastro==1,"indicador sem cadastro");
            ok(second.ocorrencias.stream().anyMatch(o->o.registro.equals("C170") && o.descricao.contains("0200")),"alerta de nao encontrado");
        }finally {Files.deleteIfExists(scratch.toPath());}
        // QTD nao informada pode existir em situacoes especiais do leiaute 2026;
        // nao deve virar QTD=0, nem erro numerico, nem falso valor declarado.
        String semQtd=new String(Files.readAllBytes(new File(args[0]).toPath()),StandardCharsets.UTF_8)
            .replace("|MAT001||12,00000|UN|", "|MAT001|||UN|");
        File temporario=File.createTempFile("sped016-qtd-vazia", ".txt");
        try {
            Files.write(temporario.toPath(),semQtd.getBytes(StandardCharsets.UTF_8));
            SpedParser.Resultado comAusencia=SpedParser.ler(temporario);
            ok(comAusencia.itens.get(0).quantidade==null && comAusencia.itens.get(0).ausentes.contains("QTD"),"QTD vazia diferente de zero");
            ItemConsulta.Totais indicador=ItemConsulta.totalizar(comAusencia.itens);
            ok(indicador.semCamposPrincipais==1 && indicador.camposInvalidos==0,"campo nao informado diferente de erro numerico");
        }finally {Files.deleteIfExists(temporario.toPath());}
        System.out.println("PASSOU: C170, 0200, NCM, vinculacao ao C100, campo QTD 5 casas, filtros, totais e alerta cadastral.");
    }
}
