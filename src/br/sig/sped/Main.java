package br.sig.sped;

import java.awt.EventQueue;
import javax.swing.UIManager;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
                catch (Exception ignored) { }
                new SpedWindow().setVisible(true);
            }
        });
    }
}
