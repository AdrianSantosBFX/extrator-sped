package br.sig.sped;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/** Testa a nova tela e produz captura real (SPED fictício). */
public final class RelatorioUiTest {
    static void ok(boolean ok,String err){if(!ok)throw new AssertionError(err);}
    private static Object field(Object instance,String name)throws Exception{
        Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(instance);
    }
    private static void call(Object instance,String name,Class<?>[] types,Object...args)throws Exception{
        Method m=instance.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(instance,args);
    }
    public static void main(String[] args)throws Exception{
        SpedParser.Resultado data=SpedParser.ler(new File(args[0]));
        SwingUtilities.invokeAndWait(()->{
            try{
                SpedWindow w=new SpedWindow();w.setSize(1400,850);w.setVisible(true);
                Field ref=w.getClass().getDeclaredField("resultado");ref.setAccessible(true);ref.set(w,data);
                call(w,"preencher",new Class<?>[0]);call(w,"setHasData",new Class<?>[]{boolean.class},true);
                call(w,"mostrar",new Class<?>[]{String.class},"reports");
                JButton excel=(JButton)field(w,"relatorioXlsxButton");
                JButton pdf=(JButton)field(w,"relatorioPdfButton");
                JButton table=(JButton)field(w,"tabelaXlsxButton");
                ok(excel.isEnabled() && pdf.isEnabled() && table.isEnabled(),"Botoes ativados");
                ok(excel.getActionListeners().length>0 && pdf.getActionListeners().length>0,"acoes implementadas");
                JTable detail=(JTable)field(w,"tabRelatorios");ok(detail.getRowCount()==2,"tabela consolida 2 grupos");
                w.validate();
                if(args.length>1){
                    java.awt.Dimension d=w.getContentPane().getSize();BufferedImage img=new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g=img.createGraphics();w.getContentPane().printAll(g);g.dispose();ImageIO.write(img,"png",new File(args[1]));
                }
                w.dispose();System.out.println("PASSOU: pagina de relatorios, tabela C190, 3 acoes e abertura da interface.");
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
