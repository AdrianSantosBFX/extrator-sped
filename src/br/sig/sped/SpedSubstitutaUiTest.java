package br.sig.sped;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JCheckBox;

/** Confirma que uma EFD retificadora, mesmo sem creditos RN07, pode ser aberta e exportada. */
public final class SpedSubstitutaUiTest {
    private static Object get(Object o,String nome) throws Exception {
        Field f=o.getClass().getDeclaredField(nome);f.setAccessible(true);return f.get(o);
    }
    public static void main(String[] args) throws Exception {
        final SpedParser.Resultado r=SpedParser.ler(new File(args[0]));
        if(!"1".equals(r.codFin))throw new AssertionError("Arquivo de teste precisa ter COD_FIN=1");
        final Throwable[] fail=new Throwable[1];
        SwingUtilities.invokeAndWait(()->{
            try{
                SpedWindow w=new SpedWindow();w.setSize(1400,900);w.setVisible(true);
                Field f=w.getClass().getDeclaredField("resultado");f.setAccessible(true);f.set(w,r);
                Method m=w.getClass().getDeclaredMethod("preencher");m.setAccessible(true);m.invoke(w);
                Method show=w.getClass().getDeclaredMethod("mostrar",String.class);show.setAccessible(true);show.invoke(w,"audit-credit");
                JCheckBox finalidade=(JCheckBox)get(w,"auditSubstituta");
                JButton copia=(JButton)get(w,"auditSaveCopy");
                JTable tab=(JTable)get(w,"tabAuditoria");
                if(!finalidade.isSelected() || finalidade.isEnabled())throw new AssertionError("COD_FIN=1 deve ser exibido como ja substituto");
                if(!copia.isEnabled())throw new AssertionError("Botao copia devera estar disponivel sem casos RN07");
                if(tab.getRowCount()!=19)throw new AssertionError("Retificadora legada deve mostrar 19 grupos para ajuste de CST/ALIQ; encontrado "+tab.getRowCount());
                w.dispose();System.out.println("PASSOU: EFD substituta legada importada; grupos CST/ALIQ detectados, COD_FIN e cópia preservados.");
            }catch(Throwable e){fail[0]=e;}
        });
        if(fail[0]!=null)throw new RuntimeException(fail[0]);
    }
}
