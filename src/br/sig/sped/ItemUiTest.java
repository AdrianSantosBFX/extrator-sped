package br.sig.sped;

import java.awt.Container;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;

/** UI sem acesso a dados reais: busca C170/0200, filtros, Excel e renderizacao. */
public final class ItemUiTest {
    static void ok(boolean x,String msg){if(!x)throw new AssertionError(msg);}
    static Object get(Object o,String prop)throws Exception{Field f=o.getClass().getDeclaredField(prop);f.setAccessible(true);return f.get(o);}
    static void invoke(Object o,String method,Class<?>[] a,Object...args)throws Exception{
        Method m=o.getClass().getDeclaredMethod(method,a);m.setAccessible(true);m.invoke(o,args);
    }
    static void snap(Container p,String dest)throws Exception{
        BufferedImage image=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g=image.createGraphics();p.printAll(g);g.dispose();ImageIO.write(image,"png",new File(dest));
    }
    public static void main(String[] args)throws Exception{
        final SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        SwingUtilities.invokeAndWait(()->{
            try{
                SpedWindow app=new SpedWindow();app.setVisible(true);
                Field data=SpedWindow.class.getDeclaredField("resultado");data.setAccessible(true);data.set(app,r);
                invoke(app,"preencher",new Class<?>[0]);
                invoke(app,"setHasData",new Class<?>[]{boolean.class},true);
                invoke(app,"atualizarStatus",new Class<?>[]{String.class,String.class},"Leitura concluída: "+r.linhasLidas+" linhas | "+r.documentos.size()+" C100 | "+r.itens.size()+" C170 | "+r.produtos.size()+" 0200 | 0 ocorrências | Charset "+r.charset,"ok");
                invoke(app,"mostrar",new Class<?>[]{String.class},"items");
                JTable items=(JTable)get(app,"tabItens");
                ok(items.getRowCount()==2,"two items visible");
                ok(items.getColumnCount()==20,"20 item columns incl ICMS/ST/IPI");
                ok("MAT002".equals(items.getValueAt(1,4)),"second item code");
                ok("39269090".equals(items.getValueAt(1,6)),"linked NCM");
                String shot=args.length>1?args[1]:null;
                if(shot!=null)snap(app.getContentPane(),shot);
                JTextField text=(JTextField)get(app,"itemTexto");text.setText("BUCHA");
                ok(items.getRowCount()==1,"search descr");
                JTextField ncm=(JTextField)get(app,"itemNcm");ncm.setText("3926");
                ok(items.getRowCount()==1,"description and NCM");
                JTextField cfop=(JTextField)get(app,"itemCfop");cfop.setText("5102");
                ok(items.getRowCount()==0,"combinacao AND");
                cfop.setText("1102");ok(items.getRowCount()==1,"CFOP right");
                File excel=File.createTempFile("sped016itens", ".xlsx");
                try {
                    int exported=XlsxExporter.tabela(items,excel,"Consulta C170");
                    ok(exported==1,"filtered export one row");
                    try(ZipFile zip=new ZipFile(excel)) {
                        java.io.InputStream stream=zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml"));
                        byte[] bytes=new byte[250000];int n=stream.read(bytes);stream.close();
                        String sheet=new String(bytes,0,n,StandardCharsets.UTF_8);
                        ok(sheet.contains("MAT002")&&!sheet.contains("MAT001"),"XLSX matches visible rows");
                        ok(sheet.contains("39269090"),"NCM exported");
                        ok(sheet.contains("<v>400.00</v>"),"VL_ITEM numeric in XLSX");
                    }
                } finally {excel.delete();}
                invoke(app,"limparBuscaItens",new Class<?>[0]);
                JComboBox<?> op=(JComboBox<?>)get(app,"itemOperacao");op.setSelectedItem("Saída");
                ok(items.getRowCount()==0,"output C170 missing correctly filtered");
                op.setSelectedItem("Todas");
                invoke(app,"mostrar",new Class<?>[]{String.class},"products");
                JTable products=(JTable)get(app,"tabProdutos");
                ok(products.getRowCount()==4,"4 product entries");
                JTextField productSearch=(JTextField)get(app,"produtoTexto");productSearch.setText("chapa");
                ok(products.getRowCount()==1,"unreferenced product searchable");
                if(args.length>2)snap(app.getContentPane(),args[2]);
                app.dispose();
                System.out.println("PASSOU: telas C170/0200, NCM, busca, intersecao, operacao, XLSX numerico e captura.");
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
    }
}
