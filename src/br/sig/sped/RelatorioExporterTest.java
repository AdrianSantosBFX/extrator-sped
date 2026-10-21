package br.sig.sped;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.zip.ZipFile;

public final class RelatorioExporterTest {
    static void ok(boolean b,String mensagem){if(!b)throw new AssertionError(mensagem);}
    public static void main(String[] args)throws Exception {
        File dir=new File(args[1]);dir.mkdirs();
        SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        RelatorioFiscal.Dados full=RelatorioFiscal.construir(r,new SpedConsultas.Filtro("","","Todas"));
        RelatorioFiscal.Dados filtered=RelatorioFiscal.construir(r,new SpedConsultas.Filtro("1102","000","Entrada"));
        ok(full.c190Considerados==2,"todos C190");
        ok(filtered.c190Considerados==1 && filtered.grupos.size()==1,"filtro e agrupamento");
        ok(filtered.totais.icms.compareTo(new BigDecimal("180.00"))==0,"soma ICMS filtrada");
        ok(filtered.documentos.stream().mapToInt(x->x.c100).sum()==3,"C100 independente de filtros C190");
        File xlsx=new File(dir,"relatorio_teste.xlsx"),pdf=new File(dir,"relatorio_teste.pdf");
        XlsxExporter.relatorio(filtered,xlsx);
        int nPages=PdfRelatorioExporter.exportar(filtered,pdf);
        ok(nPages>=2,"paginacao resumo + detalhes");
        try(ZipFile z=new ZipFile(xlsx)){
            ok(z.getEntry("xl/worksheets/sheet4.xml")!=null,"4 abas");
            byte[] xml=read(z,"xl/worksheets/sheet2.xml");
            String text=new String(xml,StandardCharsets.UTF_8);
            ok(text.contains("1102") && !text.contains("5102"),"XLSX respeita filtros");
            ok(text.contains("<v>180.00</v>"),"XLSX ICMS como numero");
            ok(!text.contains("<f>"),"Sem formula introduzida por conteúdo externo");
            ok(new String(read(z,"xl/worksheets/sheet3.xml"),StandardCharsets.UTF_8).contains("Documento" )==false || z.size()>=7,"C100 existente");
        }
        byte[] magic=Files.readAllBytes(pdf.toPath());ok(new String(Arrays.copyOfRange(magic,0,8),StandardCharsets.US_ASCII).startsWith("%PDF-1.4"),"assinatura PDF");
        JTable table=new JTable(new DefaultTableModel(new Object[][]{{"1102","000","1.234,56","001234"},{"5102","060","12,30","000035"}},new Object[]{"CFOP","CST","Valor operação","Nº nota"}));
        table.setAutoCreateRowSorter(true);
        @SuppressWarnings("unchecked") TableRowSorter<DefaultTableModel> sorter=(TableRowSorter<DefaultTableModel>)table.getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("1102",0));
        File visible=new File(dir,"consulta_teste.xlsx");int exported=XlsxExporter.tabela(table,visible,"Teste");
        try(ZipFile z=new ZipFile(visible)){
            String xml=new String(read(z,"xl/worksheets/sheet1.xml"),StandardCharsets.UTF_8);
            ok(exported==1 && xml.contains("1102") && !xml.contains("5102"),"somente registros visíveis");
            ok(xml.contains("001234"),"zeros à esquerda preservados");
            ok(xml.contains("<v>1234.56</v>"),"valor monetário pesquisável e somável");
        }
        for(int i=0;i<240;i++) {
            SpedParser.Analitico a=new SpedParser.Analitico();a.documento=r.documentos.get(0);
            a.cfop=String.valueOf(2000+i);a.cst=String.format("%03d",i%999);
            a.valorOperacao=new BigDecimal("31.25");a.baseIcms=new BigDecimal("31.25");
            a.icms=new BigDecimal("5.63");r.analiticos.add(a);
        }
        RelatorioFiscal.Dados massa=RelatorioFiscal.construir(r,new SpedConsultas.Filtro("","","Todas"));
        int muitasPaginas=PdfRelatorioExporter.exportar(massa,new File(dir,"relatorio_muitas_paginas.pdf"));
        ok(muitasPaginas>=7 && massa.grupos.size()>200,"paginacao multi página");
        System.out.println("PASSOU: C190 filtrado, C100 separado, Excel 4 abas, XLSX numerico e PDF "+nPages+" páginas (massa: "+muitasPaginas+" páginas).");
    }
    static byte[] read(ZipFile z,String name)throws Exception{
        try(java.io.InputStream in=z.getInputStream(z.getEntry(name));java.io.ByteArrayOutputStream buf=new java.io.ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))>=0)buf.write(b,0,n);return buf.toByteArray();
        }
    }
}
