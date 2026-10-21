package br.sig.sped;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JTable;

/** Escritor XLSX (OOXML) usando apenas Java 8: texto como inlineStr, números como células numéricas. */
public final class XlsxExporter {
    private XlsxExporter() {}
    static final class Celula {
        final String texto;final BigDecimal numero;
        Celula(String texto,BigDecimal numero){this.texto=texto;this.numero=numero;}
        static Celula t(String s){return new Celula(s==null?"":s,null);}
        static Celula n(BigDecimal n){return n==null?t("INVÁLIDO"):new Celula(null,n);}
        static Celula n(long n){return n(BigDecimal.valueOf(n));}
    }
    static final class Aba {
        final String nome;
        final List<Celula[]> linhas=new ArrayList<Celula[]>();
        final int cabecalho;
        Aba(String nome,int header){this.nome=nome;this.cabecalho=header;}
        void add(Celula...c){linhas.add(c);}
        void text(String... strs){Celula[] cells=new Celula[strs.length];for(int i=0;i<strs.length;i++)cells[i]=Celula.t(strs[i]);add(cells);}
    }
    /** Respeita ordenação e linhas visíveis do JTable, sem converter identificadores fiscais em números. */
    public static int tabela(JTable tabela,File destino,String titulo) throws IOException {
        if(tabela==null)throw new IllegalArgumentException("Nenhuma tabela selecionada.");
        Aba a=new Aba("Consulta",4);
        a.text(titulo);
        a.text("Linhas visíveis exportadas: "+tabela.getRowCount()+". Ordem da tabela preservada.");
        a.text("Códigos fiscais, notas, CNPJs e chaves são exportados como TEXTO para preservar zeros à esquerda.");
        Celula[] headers=new Celula[tabela.getColumnCount()];
        for(int c=0;c<headers.length;c++)headers[c]=Celula.t(tabela.getColumnName(c));
        a.add(headers);
        for(int r=0;r<tabela.getRowCount();r++){
            Celula[] cells=new Celula[headers.length];
            for(int c=0;c<cells.length;c++){
                Object value=tabela.getValueAt(r,c);
                // CNPJ, CPF, CST, CFOP, numero, serie e chave sao sempre TEXTO;
                // colunas explicitamente monetarias sao numericas e calculaveis no Excel.
                String str=value==null?"":String.valueOf(value);
                String nome=tabela.getColumnName(c).toLowerCase(java.util.Locale.ROOT);
                boolean moeda=nome.contains("valor ") || nome.contains("base ") || nome.equals("icms") ||
                    nome.equals("icms-st") || nome.equals("ipi") || nome.equals("débitos") ||
                    nome.equals("créditos") || nome.contains("saldo apurado") || nome.contains("deduções") ||
                    nome.contains("credor a transportar") || nome.contains("a recolher") || nome.startsWith("vl_") || nome.startsWith("bc ") || nome.equals("qtd");
                boolean inteiro=nome.equals("linha")||nome.equals("linhas c190")||nome.equals("linhas inválidas")||nome.equals("qt. c190");
                if(moeda && str.matches("-?[0-9.]+,[0-9]{1,5}")) {
                    try{cells[c]=Celula.n(new BigDecimal(str.replace(".","").replace(',','.')));}
                    catch(NumberFormatException ignored){cells[c]=Celula.t(str);}
                } else if(inteiro && str.matches("[0-9]+")){
                    try{cells[c]=Celula.n(new BigDecimal(str));}
                    catch(NumberFormatException ignored){cells[c]=Celula.t(str);}
                } else cells[c]=Celula.t(str);
            }
            a.add(cells);
        }
        escrever(destino,Arrays.asList(a));
        return tabela.getRowCount();
    }
    public static void relatorio(RelatorioFiscal.Dados d,File destino) throws IOException {
        Aba resumo=new Aba("Resumo",5);
        resumo.text("EXTRATOR SPED FISCAL — RELATÓRIO DESCRITIVO");
        resumo.text(d.sped.nome+" | CNPJ "+d.sped.cnpj+" | UF "+d.sped.uf);
        resumo.text("Período "+RelatorioFiscal.data(d.sped.periodoInicio)+" a "+RelatorioFiscal.data(d.sped.periodoFim)+" | Leiaute "+d.sped.codVer);
        resumo.text("Filtros C190: "+d.filtro);
        resumo.text("Indicador","Valor");
        resumo.add(Celula.t("Documentos C100 (todos; NÃO filtrados por C190)"),Celula.n(d.sped.documentos.size()));
        resumo.add(Celula.t("Registros C190 considerados"),Celula.n(d.c190Considerados));
        resumo.add(Celula.t("Registros C190 originais"),Celula.n(d.c190Originais));
        resumo.add(Celula.t("Grupos operação/CFOP/CST"),Celula.n(d.grupos.size()));
        resumo.add(Celula.t("Operações C190 - R$"),Celula.n(d.totais.operacoes));
        resumo.add(Celula.t("Base ICMS C190 - R$"),Celula.n(d.totais.baseIcms));
        resumo.add(Celula.t("ICMS C190 - R$"),Celula.n(d.totais.icms));
        resumo.add(Celula.t("Base ICMS-ST C190 - R$"),Celula.n(d.totais.baseSt));
        resumo.add(Celula.t("ICMS-ST C190 - R$"),Celula.n(d.totais.icmsSt));
        resumo.add(Celula.t("IPI C190 - R$"),Celula.n(d.totais.ipi));
        resumo.add(Celula.t("Linhas C190 com algum valor inválido"),Celula.n(d.totais.linhasInvalidas));
        resumo.add(Celula.t("Documentos C100 com valor inválido"),Celula.n(d.contarC100Invalidos()));
        resumo.add(Celula.t("Documentos C100 com situação diferente de 00"),Celula.n(d.situacoesEspeciais));
        resumo.add(Celula.t("Ocorrências de leitura"),Celula.n(d.sped.ocorrencias.size()));
        resumo.text("");resumo.text("ATENÇÃO: "+RelatorioFiscal.AVISO);
        resumo.text(RelatorioFiscal.RESSALVA);
        resumo.text(RelatorioFiscal.INVALIDOS);

        Aba cfop=new Aba("CFOP_CST",5);
        cfop.text("DETALHAMENTO C190 — OPERAÇÃO + CFOP + CST");
        cfop.text(d.sped.nome+" — "+d.sped.cnpj);
        cfop.text("Período: "+RelatorioFiscal.data(d.sped.periodoInicio)+" a "+RelatorioFiscal.data(d.sped.periodoFim));
        cfop.text("Critérios: "+d.filtro);
        cfop.text("Operação","CFOP","CST","Registros","Inválidos","Valor operação","Base ICMS","ICMS","Base ST","ICMS-ST","IPI");
        for(RelatorioFiscal.Linha g:d.grupos)cfop.add(Celula.t(g.operacao),Celula.t(g.cfop),Celula.t(g.cst),
            Celula.n(g.quantidade),Celula.n(g.invalidas),Celula.n(g.valor),Celula.n(g.base),Celula.n(g.icms),
            Celula.n(g.baseSt),Celula.n(g.icmsSt),Celula.n(g.ipi));
        cfop.add(Celula.t("TOTAL (C190 filtrado)"),Celula.t(""),Celula.t(""),Celula.n(d.totais.quantidade),Celula.n(d.totais.linhasInvalidas),
            Celula.n(d.totais.operacoes),Celula.n(d.totais.baseIcms),Celula.n(d.totais.icms),Celula.n(d.totais.baseSt),Celula.n(d.totais.icmsSt),Celula.n(d.totais.ipi));

        Aba c100=new Aba("C100_Operacao",5);
        c100.text("DOCUMENTOS C100 — CONTAGEM E VALOR BRUTO DECLARADO");
        c100.text("NÃO aplica filtros de C190. Inclui cancelados e outras situações especiais.");
        c100.text("Não somar valores C100 aos valores C190.");
        c100.text("Fonte: registros C100 declarados no SPED.");
        c100.text("Operação","Qtd. C100","VL_DOC bruto - R$","VL_DOC inválido");
        for(RelatorioFiscal.ResumoOperacao v:d.documentos)c100.add(Celula.t(v.operacao),Celula.n(v.c100),Celula.n(v.valorC100),Celula.n(v.c100Invalidos));

        Aba e110=new Aba("E110_Declarado",5);
        e110.text("APURAÇÃO DE ICMS PRÓPRIO — E110 DECLARADO");
        e110.text(d.sped.nome+" — CNPJ "+d.sped.cnpj);
        e110.text("Dados declarados; não há reconciliação automática com C190.");
        e110.text("Não somar E110 aos valores de C100/C190.");
        e110.text("Início","Fim","Débitos","Créditos","Saldo apurado","Deduções","ICMS a recolher","Credor a transportar");
        for(SpedParser.Apuracao a:d.sped.apuracoes)e110.add(Celula.t(RelatorioFiscal.data(a.inicio)),Celula.t(RelatorioFiscal.data(a.fim)),
            Celula.n(a.debitos),Celula.n(a.creditos),Celula.n(a.saldoApurado),Celula.n(a.deducoes),Celula.n(a.recolher),Celula.n(a.credorTransportar));
        escrever(destino,Arrays.asList(resumo,cfop,c100,e110));
    }
    private static void escrever(File file,List<Aba> sheets)throws IOException{
        try(OutputStream out=Files.newOutputStream(file.toPath());ZipOutputStream zip=new ZipOutputStream(out,StandardCharsets.UTF_8)){
            StringBuilder types=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
            for(int i=1;i<=sheets.size();i++)types.append("<Override PartName=\"/xl/worksheets/sheet").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            put(zip,"[Content_Types].xml",types.append("</Types>").toString());
            put(zip,"_rels/.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            StringBuilder w=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rels=new StringBuilder("<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            for(int i=0;i<sheets.size();i++){
                w.append("<sheet name=\"").append(xml(sheets.get(i).nome)).append("\" sheetId=\"").append(i+1).append("\" r:id=\"rId").append(i+1).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(i+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i+1).append(".xml\"/>");
                put(zip,"xl/worksheets/sheet"+(i+1)+".xml",sheetXml(sheets.get(i)));
            }
            rels.append("<Relationship Id=\"rId").append(sheets.size()+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            put(zip,"xl/workbook.xml",w.append("</sheets></workbook>").toString());
            put(zip,"xl/_rels/workbook.xml.rels",rels.toString());
            put(zip,"xl/styles.xml",styles());
        }
    }
    private static String styles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"+
            "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.00\"/></numFmts>"+
            "<fonts count=\"3\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"14\"/><color rgb=\"FF16375E\"/><name val=\"Calibri\"/></font></fonts>"+
            "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF204A76\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"+
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"+
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"+
            "<cellXfs count=\"4\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/><xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs>"+
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";
    }
    private static String sheetXml(Aba a) {
        StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        int cols=1;for(Celula[] row:a.linhas)cols=Math.max(cols,row.length);
        xml.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"").append(a.cabecalho).append("\" topLeftCell=\"A").append(a.cabecalho+1).append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        xml.append("<cols>");for(int c=1;c<=cols;c++){
            int width=(c==1?Math.min(60,a.nome.equals("Resumo")?57:27):(a.nome.equals("CFOP_CST")?19:21));
            xml.append("<col min=\"").append(c).append("\" max=\"").append(c).append("\" width=\"").append(width).append("\" customWidth=\"1\"/>");
        }xml.append("</cols><sheetData>");
        for(int r=0;r<a.linhas.size();r++){
            Celula[] cells=a.linhas.get(r);xml.append("<row r=\"").append(r+1).append("\"").append(r==a.cabecalho-1?" ht=\"26\" customHeight=\"1\"":"").append(">");
            for(int c=0;c<cells.length;c++){
                Celula cell=cells[c];if(cell==null)continue;
                String ref=colName(c+1)+(r+1);int style=(r==a.cabecalho-1)?1:(r==0?2:(cell.numero!=null && cell.numero.scale()>0?3:0));
                if(cell.numero!=null) xml.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"><v>").append(cell.numero.toPlainString()).append("</v></c>");
                else xml.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(cell.texto)).append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("</sheetData>");
        if(a.linhas.size()>=a.cabecalho && cols>1){
            xml.append("<autoFilter ref=\"A").append(a.cabecalho).append(":").append(colName(cols)).append(Math.max(a.cabecalho,a.linhas.size())).append("\"/>");
        }
        return xml.append("</worksheet>").toString();
    }
    private static String colName(int n){StringBuilder b=new StringBuilder();while(n>0){n--;b.insert(0,(char)('A'+n%26));n/=26;}return b.toString();}
    private static String xml(String s){
        StringBuilder b=new StringBuilder();if(s==null)return "";
        for(int i=0;i<s.length();i++){
            char ch=s.charAt(i);
            if(ch=='&')b.append("&amp;");else if(ch=='<')b.append("&lt;");else if(ch=='>')b.append("&gt;");
            else if(ch=='\"')b.append("&quot;");else if(ch=='\'')b.append("&apos;");
            else if(ch=='\n'||ch=='\r'||ch=='\t'||ch>=32 && !(ch>=0xd800 && ch<=0xdfff))b.append(ch);
        }return b.toString();
    }
    private static void put(ZipOutputStream z,String name,String text)throws IOException{
        z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();
    }
}
