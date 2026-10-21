package br.sig.sped;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.TableRowSorter;
import javax.swing.SortOrder;
import java.util.Arrays;
import javax.swing.RowSorter.SortKey;

/** Integracao GUI dos filtros, ordenacao, detalhe modal e layout real. */
public final class DocumentoUiTest {
    static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
    static Object get(Object instance,String name)throws Exception{
        Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(instance);
    }
    static void call(Object instance,String name,Class<?>[] types,Object...args)throws Exception{
        Method m=instance.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(instance,args);
    }
    static void snap(Container w,String path)throws Exception{
        BufferedImage img=new BufferedImage(w.getWidth(),w.getHeight(),BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics=img.createGraphics();w.printAll(graphics);graphics.dispose();
        ImageIO.write(img,"png",new File(path));
    }
    public static void main(String[] args)throws Exception{
        final SpedParser.Resultado parsed=SpedParser.ler(new File(args[0]));
        SwingUtilities.invokeAndWait(()->{
            try{
                SpedWindow win=new SpedWindow();win.setVisible(true);
                Field result=SpedWindow.class.getDeclaredField("resultado");result.setAccessible(true);result.set(win,parsed);
                call(win,"preencher",new Class<?>[0]);
                call(win,"setHasData",new Class<?>[]{boolean.class},true);
                call(win,"mostrar",new Class<?>[]{String.class},"docs");
                JTable notes=(JTable)get(win,"tabNotas");
                check(notes.getRowCount()==3,"all docs listed");
                JTextField number=(JTextField)get(win,"docNumero");
                number.setText("23456");
                check(notes.getRowCount()==1,"doc number filter after typing");
                number.setText("");
                JTextField p=(JTextField)get(win,"docParticipante");
                p.setText("FORNECEDOR");
                check(notes.getRowCount()==1,"participant search");
                p.setText("");
                JTextField date=(JTextField)get(win,"docData");
                date.setText("15/09/2026");
                check(notes.getRowCount()==1,"date search");
                date.setText("");
                @SuppressWarnings("unchecked") TableRowSorter<?> sorter=(TableRowSorter<?>)notes.getRowSorter();
                sorter.setSortKeys(Arrays.asList(new SortKey(10,SortOrder.DESCENDING)));
                sorter.sort();
                check("23456".equals(notes.getValueAt(0,5)),"money column sorted numerically");
                sorter.setSortKeys(Arrays.asList(new SortKey(6,SortOrder.DESCENDING)));
                sorter.sort();
                check("23457".equals(notes.getValueAt(0,5)),"date column sorted chronologically");
                sorter.setSortKeys(null);
                notes.setRowSelectionInterval(0,0);
                win.validate();
                if(args.length>1)snap(win.getContentPane(),args[1]);
                if(args.length>2){
                    // JDialog modal roda loop EDT proprio, Timer fecha apos gerar captura.
                    final boolean[] captured={false};
                    Timer timer=new Timer(400,e->{
                        try{
                            for(java.awt.Window w:java.awt.Window.getWindows()){
                                if(w instanceof JDialog && w.isShowing()){
                                    snap((JDialog)w,args[2]);
                                    JDialog d=(JDialog)w;
                                    check(d.getTitle().contains("Documento C100"),"document detail title");
                                    captured[0]=true;d.dispose();((Timer)e.getSource()).stop();break;
                                }
                            }
                        }catch(Exception err){throw new RuntimeException(err);}
                    });
                    timer.start();
                    call(win,"abrirDetalhes",new Class<?>[0]);
                    timer.stop();check(captured[0],"modal opened");
                }
                win.dispose();
                System.out.println("PASSOU: filtros UI, ordenacao numerica, data, selecao C100 e detalhes modais.");
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
    }
}
