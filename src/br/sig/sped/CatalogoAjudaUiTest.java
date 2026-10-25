package br.sig.sped;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** Regressão da v0.1.11: catálogo, ajuda no workspace e EFD substituta. */
public final class CatalogoAjudaUiTest {
    static Object f(Object o,String name)throws Exception {
        Field field=o.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(o);
    }
    static void check(boolean yes,String what){if(!yes)throw new AssertionError(what);}
    public static void main(String[] args)throws Exception {
        final Throwable[] failure=new Throwable[1];
        final SpedParser.Resultado original=SpedParser.ler(new File(args[0]));
        final SpedParser.Resultado substituta=SpedParser.ler(new File(args[1]));
        SwingUtilities.invokeAndWait(()->{
            try {
                SpedWindow w=new SpedWindow();w.setSize(1420,920);w.setVisible(true);
                Method mostrar=w.getClass().getDeclaredMethod("mostrar",String.class);mostrar.setAccessible(true);
                Method preencher=w.getClass().getDeclaredMethod("preencher");preencher.setAccessible(true);
                Method hasData=w.getClass().getDeclaredMethod("setHasData",boolean.class);hasData.setAccessible(true);
                Field resultado=w.getClass().getDeclaredField("resultado");resultado.setAccessible(true);
                @SuppressWarnings("unchecked") List<JButton> buttons=(List<JButton>)f(w,"acoesCatalogo");
                check(buttons.size()==CatalogoAuditoria.todas().size(),"catálogo incompleto");
                for(JButton b:buttons)check(!b.isEnabled(),"botão sem arquivo deve estar indisponível");
                mostrar.invoke(w,"audit");
                check("audit".equals(f(w,"currentPage")),"catálogo inicial");
                mostrar.invoke(w,"help");
                check("help".equals(f(w,"currentPage")),"ajuda dentro da área");
                Method topic=w.getClass().getDeclaredMethod("abrirAjuda",String.class);topic.setAccessible(true);
                topic.invoke(w,"creditos");
                JEditorPane content=(JEditorPane)f(w,"helpText");
                check(content.getText().contains("RN07"),"RN07 precisa aparecer no manual");
                resultado.set(w,original);preencher.invoke(w);hasData.invoke(w,true);
                for(int i=0;i<buttons.size();i++){
                    check(buttons.get(i).isEnabled()==CatalogoAuditoria.todas().get(i).executavel(true),"estado incorreto do módulo "+i);
                }
                mostrar.invoke(w,"audit-credit");
                check("audit-credit".equals(f(w,"currentPage")),"módulo RN07 não abre");
                mostrar.invoke(w,"audit-diagnostic");
                JTextArea diagnostics=(JTextArea)f(w,"diagnosticoTexto");
                check(diagnostics.getText().contains("DIAGNÓSTICO COMPLEMENTAR"),"diagnóstico não embutido");
                resultado.set(w,substituta);preencher.invoke(w);hasData.invoke(w,true);
                JLabel state=(JLabel)f(w,"catalogStatus");
                check(state.getText().contains("SUBSTITUTA"),"COD_FIN=1 não reconhecido no catálogo");
                mostrar.invoke(w,"audit");
                if(args.length>2){
                    java.awt.image.BufferedImage shot=new java.awt.image.BufferedImage(w.getWidth(),w.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g=shot.createGraphics();w.paint(g);g.dispose();
                    javax.imageio.ImageIO.write(shot,"png",new File(args[2]));
                }
                w.dispose();System.out.println("PASSOU: catálogo, ajuda interna, ferramentas, original e substituta.");
            } catch(Throwable e){failure[0]=e;}
        });
        if(failure[0]!=null)throw new RuntimeException(failure[0]);
    }
}
