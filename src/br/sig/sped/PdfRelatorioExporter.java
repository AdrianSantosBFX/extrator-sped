package br.sig.sped;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Relatório PDF A4 paisagem, paginado, com fontes PDF padrão e sem bibliotecas externas. */
public final class PdfRelatorioExporter {
    private PdfRelatorioExporter() {}
    private static final Charset PDF_CHARSET=Charset.forName("windows-1252");
    private static final float W=842,H=595;
    private static final String[] HEADERS={"Operação","CFOP","CST","Reg.","Inv.","VL_OPR","BC_ICMS","ICMS","ICMS-ST","IPI"};
    private static final float[] LEFT={32,98,145,192,226,260,378,496,602,714};
    private static final float[] MAX_WIDTH={62,43,43,30,30,112,112,100,106,92};
    private static class Canvas {
        final StringBuilder s=new StringBuilder();
        Canvas() {s.append("1 J 1 j\n");}
        void fill(int r,int g,int b){s.append(f(r/255d)+" "+f(g/255d)+" "+f(b/255d)+" rg\n");}
        void stroke(int r,int g,int b){s.append(f(r/255d)+" "+f(g/255d)+" "+f(b/255d)+" RG\n");}
        void rect(float x,float top,float w,float h) {s.append(f(x)+" "+f(H-top-h)+" "+f(w)+" "+f(h)+" re f\n");}
        void line(float x,float y,float x2,float y2){s.append(f(x)+" "+f(H-y)+" m "+f(x2)+" "+f(H-y2)+" l S\n");}
        void text(String value,float x,float y,int size,boolean bold,int r,int g,int b) {
            fill(r,g,b);
            s.append("BT /F").append(bold?"2":"1").append(" ").append(size).append(" Tf 1 0 0 1 ").append(f(x)).append(" ").append(f(H-y)).append(" Tm (").append(pdfEsc(value)).append(") Tj ET\n");
        }
        byte[] bytes(){return s.toString().getBytes(PDF_CHARSET);}
    }
    private static String f(double v){return String.format(java.util.Locale.US,"%.2f",v);}
    private static String pdfEsc(String text) {
        StringBuilder b=new StringBuilder();
        if(text==null)return "";
        for(char c:text.toCharArray()){
            if(c=='(' || c==')' || c=='\\')b.append('\\').append(c);
            else if(c>=32 && c<127 || c>=160 && c<=255 || c=='€')b.append(c);
            else if(c=='\u2013'||c=='\u2014')b.append('-');
            else if(c=='\u2018'||c=='\u2019')b.append('\'');
            else if(c=='\u2022')b.append('-');
            else if(c=='\n'||c=='\r'||c=='\t')b.append(' ');
            else b.append('?');
        }return b.toString();
    }
    private static String limitar(String s,int chars) {
        if(s==null)return "";return s.length()<=chars?s:s.substring(0,Math.max(0,chars-3))+"...";
    }
    private static String valor(BigDecimal b,int max) {
        String s=RelatorioFiscal.br(b);
        return s.length()>max?limitar(s,max):s;
    }
    private static void base(Canvas c,RelatorioFiscal.Dados d,int num) {
        c.fill(24,57,94);c.rect(0,0,W,67);
        c.text("EXTRATOR SPED FISCAL",32,28,15,true,255,255,255);
        c.text("Relatório descritivo | EFD ICMS/IPI",33,47,9,false,215,234,251);
        c.text("EMPRESA  "+limitar(d.sped.nome,65),32,88,10,true,24,57,94);
        c.text("CNPJ "+d.sped.cnpj+"  |  UF "+d.sped.uf+"  |  Período "+RelatorioFiscal.data(d.sped.periodoInicio)+" a "+RelatorioFiscal.data(d.sped.periodoFim)+"  |  Leiaute "+d.sped.codVer,32,105,9,false,72,90,109);
        c.fill(227,234,244);c.rect(0,565,W,30);
        c.text("Fonte: arquivo SPED local • Sem validação fiscal, apuração ou consulta à SEFAZ",32,583,8,false,48,75,103);
        c.text("Página "+num,763,583,8,true,48,75,103);
    }
    private static void section(Canvas c,String s,float y) {
        c.fill(229,239,249);c.rect(31,y-16,780,24);
        c.text(s,39,y,11,true,27,66,108);
    }
    private static void header(Canvas c,float y) {
        c.fill(31,76,119);c.rect(31,y-15,780,25);
        for(int i=0;i<HEADERS.length;i++)c.text(HEADERS[i],LEFT[i]+2,y,8,true,255,255,255);
    }
    private static void line(Canvas c,RelatorioFiscal.Linha row,int index,float y) {
        if(index%2==0){c.fill(246,249,253);c.rect(31,y-13,780,20);}
        String[] vals={row.operacao,row.cfop,row.cst,String.valueOf(row.quantidade),String.valueOf(row.invalidas),
            valor(row.valor,16),valor(row.base,16),valor(row.icms,15),valor(row.icmsSt,15),valor(row.ipi,13)};
        for(int i=0;i<vals.length;i++)c.text(limitar(vals[i],i==0?11:i>=5?16:10),LEFT[i]+2,y,8,false,33,57,80);
    }
    public static int exportar(RelatorioFiscal.Dados d,File destino)throws IOException{
        if(d==null)throw new IllegalArgumentException("Sem dados para relatório.");
        List<byte[]> pages=new ArrayList<byte[]>();Canvas c=new Canvas();base(c,d,1);
        section(c,"1  VISÃO GERAL DA EXTRAÇÃO",135);
        c.text("C100 encontrados: "+d.sped.documentos.size()+"   |   C190 considerados: "+d.c190Considerados+" / "+d.c190Originais+"   |   Grupos: "+d.grupos.size(),40,166,10,true,24,55,89);
        c.text("Valores C190 sujeitos aos filtros abaixo. Não somar ao C100 nem ao E110.",40,184,9,false,76,94,113);
        c.text("VL_OPR: R$ "+RelatorioFiscal.br(d.totais.operacoes)+"   |   BC ICMS: R$ "+RelatorioFiscal.br(d.totais.baseIcms)+"   |   ICMS: R$ "+RelatorioFiscal.br(d.totais.icms),40,205,10,true,24,59,103);
        c.text("Base ST: R$ "+RelatorioFiscal.br(d.totais.baseSt)+"   |   ICMS-ST: R$ "+RelatorioFiscal.br(d.totais.icmsSt)+"   |   IPI: R$ "+RelatorioFiscal.br(d.totais.ipi)+"   |   Inválidas: "+d.totais.linhasInvalidas,40,224,10,true,24,59,103);
        section(c,"2  FILTROS C190",256);
        String fil=d.filtro;
        // Separacao dos criterios para nao cortar informacao em PDF.
        String[] partes=fil.split(" \\| ");int yy=280;
        for(int i=0;i<partes.length;i+=2){
            String pair=partes[i]+(i+1<partes.length?"    •    "+partes[i+1]:"");
            c.text(limitar(pair,130),40,yy,9,false,34,57,83);yy+=17;
        }
        section(c,"3  DOCUMENTOS C100 (SEM FILTROS C190)",349);
        for(int i=0;i<d.documentos.size();i++){
            RelatorioFiscal.ResumoOperacao op=d.documentos.get(i);
            c.text(op.operacao+": "+op.c100+" documento(s), VL_DOC bruto R$ "+RelatorioFiscal.br(op.valorC100)+"; valores inválidos "+op.c100Invalidos,40,374+i*17,9,false,35,63,88);
        }
        c.text("Situações C100 diferentes de '00': "+d.situacoesEspeciais+" (não excluídas do quadro C100).",40,431,9,true,136,72,30);
        section(c,"4  OBSERVAÇÕES",463);
        c.text("C100 e C190 não devem ser somados. C190 não constitui apuração; E110 contém os valores declarados.",40,489,9,false,56,75,97);
        c.text("Linhas com valor inválido são parcialmente totalizadas; números de outros registros não foram incluídos.",40,507,9,false,56,75,97);
        c.text("A consulta inclui CFOP/CST escriturados mesmo quando o documento possua situação especial.",40,525,9,false,56,75,97);
        pages.add(c.bytes());
        c=new Canvas();int page=2;int idx=0;float y=154;
        base(c,d,page);section(c,"5  DETALHAMENTO — OPERAÇÃO / CFOP / CST",132);header(c,y);y+=25;
        for(RelatorioFiscal.Linha row:d.grupos) {
            if(y>528){pages.add(c.bytes());page++;c=new Canvas();base(c,d,page);section(c,"5  DETALHAMENTO — CONTINUAÇÃO",132);y=154;header(c,y);y+=25;}
            line(c,row,idx++,y);y+=20;
        }
        if(d.grupos.isEmpty())c.text("Nenhum registro C190 corresponde aos filtros selecionados.",45,198,10,false,99,112,131);
        if(y+34<552){
            c.fill(222,235,248);c.rect(31,y-11,780,25);
            c.text("TOTAL C190 FILTRADO",35,y+5,9,true,27,60,99);
            c.text("R$ "+RelatorioFiscal.br(d.totais.operacoes)+" em operações",260,y+5,8,true,27,60,99);
            c.text("ICMS R$ "+RelatorioFiscal.br(d.totais.icms),500,y+5,8,true,27,60,99);
        }
        pages.add(c.bytes());
        if(!d.sped.apuracoes.isEmpty()){
            c=new Canvas();page++;base(c,d,page);section(c,"6  APURAÇÃO E110 — VALORES DECLARADOS",135);
            int row=0;
            for(SpedParser.Apuracao a:d.sped.apuracoes){
                int ry=167+row*87;
                if(ry>490){pages.add(c.bytes());c=new Canvas();page++;base(c,d,page);section(c,"6  APURAÇÃO E110 — CONTINUAÇÃO",135);row=0;ry=167;}
                c.text("Período "+RelatorioFiscal.data(a.inicio)+" a "+RelatorioFiscal.data(a.fim),40,ry,10,true,26,65,104);
                c.text("Débitos R$ "+RelatorioFiscal.br(a.debitos)+"    Créditos R$ "+RelatorioFiscal.br(a.creditos)+"    Saldo apurado R$ "+RelatorioFiscal.br(a.saldoApurado),40,ry+20,9,false,45,64,86);
                c.text("Deduções R$ "+RelatorioFiscal.br(a.deducoes)+"    ICMS a recolher R$ "+RelatorioFiscal.br(a.recolher)+"    Credor a transportar R$ "+RelatorioFiscal.br(a.credorTransportar),40,ry+39,9,false,45,64,86);
                row++;
            }
            c.text("Os valores E110 acima não foram recalculados ou reconciliados automaticamente com C190.",40,548,8,false,137,65,42);
            pages.add(c.bytes());
        }
        gravarPdf(destino,pages);
        return pages.size();
    }
    private static byte[] ascii(String s){return s.getBytes(java.nio.charset.StandardCharsets.US_ASCII);}
    private static void write(ByteArrayOutputStream out,String s)throws IOException{out.write(ascii(s));}
    private static void gravarPdf(File target,List<byte[]> pages)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();write(out,"%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        int n=5+2*pages.size();int[] offsets=new int[n+1];
        offsets[1]=out.size();write(out,"1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        offsets[2]=out.size();StringBuilder kids=new StringBuilder();for(int i=0;i<pages.size();i++)kids.append(4+2*i).append(" 0 R ");
        write(out,"2 0 obj << /Type /Pages /Kids ["+kids+"] /Count "+pages.size()+" >> endobj\n");
        offsets[3]=out.size();write(out,"3 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >> endobj\n");
        offsets[4+pages.size()*2]=0; // placeholder not needed; objects are inserted below.
        for(int i=0;i<pages.size();i++){
            int pageId=4+2*i,streamId=5+2*i;byte[] stream=pages.get(i);
            offsets[pageId]=out.size();write(out,pageId+" 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 3 0 R /F2 "+(4+2*pages.size())+" 0 R >> >> /Contents "+streamId+" 0 R >> endobj\n");
            offsets[streamId]=out.size();write(out,streamId+" 0 obj << /Length "+stream.length+" >> stream\n");out.write(stream);write(out,"\nendstream endobj\n");
        }
        int boldId=4+2*pages.size();offsets[boldId]=out.size();write(out,boldId+" 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >> endobj\n");
        int xref=out.size();write(out,"xref\n0 "+(boldId+1)+"\n0000000000 65535 f \n");
        for(int i=1;i<=boldId;i++)write(out,String.format(java.util.Locale.US,"%010d 00000 n \n",offsets[i]));
        write(out,"trailer << /Size "+(boldId+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n");
        Files.write(target.toPath(),out.toByteArray());
    }
}
