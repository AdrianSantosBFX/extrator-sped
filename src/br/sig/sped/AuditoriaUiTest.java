package br.sig.sped;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

/** Smoke test do painel Auditoria Fiscal e seleção. */
public final class AuditoriaUiTest {
    private static Object get(Object o,String nome) throws Exception {
        Field f=o.getClass().getDeclaredField(nome); f.setAccessible(true); return f.get(o);
    }
    public static void main(String[] args) throws Exception {
        final SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        final Throwable[] error=new Throwable[1];
        SwingUtilities.invokeAndWait(()->{
            try{
                SpedWindow w=new SpedWindow();w.setSize(1440,900);w.setVisible(true);
                Field field=w.getClass().getDeclaredField("resultado");field.setAccessible(true);field.set(w,r);
                Method m=w.getClass().getDeclaredMethod("preencher");m.setAccessible(true);m.invoke(w);
                Method show=w.getClass().getDeclaredMethod("mostrar",String.class);show.setAccessible(true);show.invoke(w,"audit-credit");
                JTable t=(JTable)get(w,"tabAuditoria");
                if(t.getRowCount()!=19)throw new AssertionError("19 linhas: "+t.getRowCount());
                DefaultTableModel mdl=(DefaultTableModel)t.getModel();
                if(!mdl.isCellEditable(0,0))throw new AssertionError("seleção inativa");
                mdl.setValueAt(Boolean.TRUE,0,0);
                if(!Boolean.TRUE.equals(mdl.getValueAt(0,0)))throw new AssertionError("seleção não atualizou");
                JLabel resumo=(JLabel)get(w,"auditTotal");
                if(!resumo.getText().contains("9.070,45"))throw new AssertionError("total inesperado: "+resumo.getText());
                // foto de validação visual, fora do pacote
                if(args.length>1){
                    java.awt.image.BufferedImage shot=new java.awt.image.BufferedImage(w.getWidth(),w.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g=shot.createGraphics();w.paint(g);g.dispose();
                    javax.imageio.ImageIO.write(shot,"png",new File(args[1]));
                }
                w.dispose();System.out.println("PASSOU: painel MG-RN07, 19 registros, checkbox e total.");
            }catch(Throwable e){error[0]=e;}
        });
        if(error[0]!=null)throw new RuntimeException(error[0]);
    }
}
