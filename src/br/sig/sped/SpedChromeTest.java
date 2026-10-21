package br.sig.sped;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Teste de fumaca visual (exige ambiente grafico), usando apenas o SPED ficticio. */
public final class SpedChromeTest {
    private SpedChromeTest() {}
    private static void check(boolean good,String message) {
        if(!good) throw new AssertionError(message);
    }
    private static JButton button(Container parent,String caption) {
        for(Component child:parent.getComponents()) {
            if(child instanceof JButton && caption.equals(((JButton)child).getText())) return (JButton)child;
            if(child instanceof Container) {
                JButton b=button((Container)child,caption);
                if(b!=null)return b;
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        final SpedParser.Resultado parsed=SpedParser.ler(new File(args[0]));
        SwingUtilities.invokeAndWait(new Runnable() {public void run() {
            try {
                SpedWindow window=new SpedWindow();
                window.setVisible(true);
                JMenuBar menu=window.getJMenuBar();
                check(menu.getMenuCount()==5,"cinco menus da referencia");
                for(String name:new String[]{"Arquivo","Consulta","Exportação","Ferramentas","Ajuda"}) {
                    boolean found=false;
                    for(int i=0;i<menu.getMenuCount();i++) if(name.equals(menu.getMenu(i).getText()))found=true;
                    check(found,"menu "+name);
                }
                for(String name:new String[]{"Abrir SPED (.txt)","Atualizar consulta","Exportar CSV","Ajuda"}) {
                    JButton b=button(window.getContentPane(),name);
                    check(b!=null,"botao "+name);
                    check(b.getActionListeners().length>0,"acao vinculada "+name);
                }
                JPanel content=(JPanel)window.getContentPane();
                JPanel top=(JPanel)content.getComponent(0);
                check(top.getPreferredSize().height==96,"altura do ribbon");
                JPanel bottom=(JPanel)((BorderLayout)content.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
                check(bottom.getPreferredSize().height==28,"altura do rodape");

                Field field=SpedWindow.class.getDeclaredField("resultado");
                field.setAccessible(true);field.set(window,parsed);
                Method render=SpedWindow.class.getDeclaredMethod("preencher");
                render.setAccessible(true);render.invoke(window);
                Method enable=SpedWindow.class.getDeclaredMethod("setHasData",boolean.class);
                enable.setAccessible(true);enable.invoke(window,true);
                Method status=SpedWindow.class.getDeclaredMethod("atualizarStatus",String.class,String.class);
                status.setAccessible(true);
                status.invoke(window,"Leitura concluída: "+parsed.linhasLidas+" linhas | "+parsed.documentos.size()+" C100 | "+parsed.analiticos.size()+" C190 | "+parsed.ocorrencias.size()+" ocorrências | Charset "+parsed.charset,"ok");
                Field statusText=SpedWindow.class.getDeclaredField("status");statusText.setAccessible(true);
                check(((JLabel)statusText.get(window)).getText().contains("Leitura concluída"),"status carregado");
                window.validate();
                if(args.length>1) {
                    Dimension d=window.getContentPane().getSize();
                    BufferedImage shot=new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g=shot.createGraphics();
                    window.getContentPane().printAll(g);g.dispose();
                    ImageIO.write(shot,"png",new File(args[1]));
                }
                window.dispose();
                System.out.println("PASSOU: 5 menus, 4 botoes conectados, ribbon de 96px, rodape 28px, arquivo e status.");
            } catch(Exception e){throw new RuntimeException(e);}
        }});
    }
}
