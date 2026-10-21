package br.sig.sped;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Teste de regressao sem interfaces graficas e sem dados reais de contribuintes. */
public final class SpedFunctionalTest {
    private static void ok(boolean condition,String msg) {if(!condition)throw new AssertionError(msg);}
    public static void main(String[] args) throws Exception {
        SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        ok(r.tem0000,"registro 0000");
        ok(r.documentos.size()==3,"C100 lidos");
        ok(r.analiticos.size()==2,"C190 lidos");
        ok(r.apuracoes.size()==1,"E110 lido");
        ok(r.analiticos.get(0).documento==r.documentos.get(0),"vinculo C100-C190");
        ok(r.analiticos.get(0).icms.compareTo(new BigDecimal("180.00"))==0,"ICMS C190");
        SpedConsultas.Filtro todas=new SpedConsultas.Filtro("","","Todas");
        ok(SpedConsultas.filtrar(r.analiticos,todas).size()==2,"todos");
        ok(SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("1102","000","Entrada")).size()==1,"CFOP+CST+Entrada");
        ok(SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("5102","000","Entrada")).isEmpty(),"AND dos filtros");
        ok(SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("51","","Saída")).size()==1,"pesquisa por trecho");
        ok(SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("","060","Todas")).isEmpty(),"CST ausente");
        ok(SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("","","Todas","1102","000")).size()==1,"CFOP/CST exatos");
        ok(SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("51","","Todas","1102","")).isEmpty(),"intersecao texto + exato");
        List<SpedParser.Analitico> filtradas=SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("","000","Saída"));
        ok(SpedConsultas.totalizar(filtradas).icms.compareTo(new BigDecimal("450.00"))==0,"soma filtrada");
        ok(SpedConsultas.agrupar(r.analiticos).size()==2,"agrupar operacao CFOP CST");
        SpedParser.Analitico invalido=new SpedParser.Analitico();
        invalido.documento=r.documentos.get(0);invalido.cfop="1102";invalido.cst="000";
        invalido.icms=null;
        r.analiticos.add(invalido);
        SpedConsultas.Totais t=SpedConsultas.totalizar(SpedConsultas.filtrar(r.analiticos,new SpedConsultas.Filtro("1102","000","Entrada")));
        ok(t.quantidade==2 && t.linhasInvalidas==1,"contagem de invalidos");
        ok(t.icms.compareTo(new BigDecimal("180.00"))==0,"nao soma invalido como 0 sem aviso");
        DefaultTableModel m=new DefaultTableModel(new Object[]{"CFOP","Valor"},0);
        m.addRow(new Object[]{"1102","100,00"});m.addRow(new Object[]{"5102","=HYPERLINK(\"x\")"});
        JTable tab=new JTable(m);tab.setAutoCreateRowSorter(true);
        @SuppressWarnings("unchecked") TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)tab.getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("5102",0));
        File temp=File.createTempFile("extrator_sped_teste_",".csv");
        int n=CsvExporter.exportar(tab,temp);
        String txt=new String(Files.readAllBytes(temp.toPath()),StandardCharsets.UTF_8);
        ok(n==1 && !txt.contains("1102") && txt.contains("5102"),"exportacao respeita filtro");
        ok(txt.contains("'=HYPERLINK"),"sanitizacao de formulas CSV");
        ok(txt.charAt(0)=='\uFEFF',"BOM UTF-8");
        Files.deleteIfExists(temp.toPath());
        System.out.println("PASSOU: parser, relacoes, CFOP, CST, operacao, combinacao, totais, invalidos e CSV filtrado (UTF-8/BOM/injecao).");
    }
}
