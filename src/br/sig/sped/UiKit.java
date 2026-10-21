package br.sig.sped;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** Componentes graficos simples e icones vetoriais. Nao exige dependencias externas. */
final class UiKit {
    static final Color NAVY = new Color(29,63,105);
    static final Color DEEP = new Color(24,51,83);
    static final Color BLUE = new Color(43,130,202);
    static final Color SKY = new Color(233,244,255);
    static final Color BG = new Color(241,246,251);
    static final Color WHITE = Color.WHITE;
    static final Color BORDER = new Color(218,228,239);
    static final Color SECONDARY = new Color(100,120,145);
    static final Color GREEN = new Color(26,151,111);
    static final Color ORANGE = new Color(221,132,34);
    static final Color RED = new Color(210,66,71);
    static Font font(int size, boolean bold) {return new Font("Segoe UI", bold?Font.BOLD:Font.PLAIN,size);}
    static JLabel text(String content,int size,boolean bold,Color color) {
        JLabel l=new JLabel(content);l.setFont(font(size,bold));l.setForeground(color);return l;
    }
    static JLabel caption(String s) {return text(s,12,false,SECONDARY);}
    static JPanel space(int w,int h) {JPanel p=new JPanel();p.setOpaque(false);p.setPreferredSize(new Dimension(w,h));return p;}
    static Icon icon(String type, int size, Color color) {return TablerIcons.icon(type,size,color);}
    static void aa(Graphics2D g) {g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
    static final class Panel extends JPanel {
        private final Color bg, edge;
        private final int radius;
        Panel(Color bg,Color edge,int radius) {this.bg=bg;this.edge=edge;this.radius=radius;setOpaque(false);}
        protected void paintComponent(Graphics g) {
            Graphics2D a=(Graphics2D)g.create();aa(a);
            a.setColor(bg);a.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);
            if(edge!=null) {a.setColor(edge);a.setStroke(new BasicStroke(1));a.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);}
            a.dispose();super.paintComponent(g);
        }
    }
    static final class GradientPanel extends JPanel {
        private final Color a,b; private final int radius;
        GradientPanel(Color a,Color b,int radius){this.a=a;this.b=b;this.radius=radius;setOpaque(false);}
        protected void paintComponent(Graphics g) {
            Graphics2D d=(Graphics2D)g.create();aa(d);
            d.setPaint(new GradientPaint(0,0,a,getWidth(),getHeight(),b));
            d.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);
            d.dispose(); super.paintComponent(g);
        }
    }
    static final class ActionButton extends JButton {
        private final Color fill; private final Color hoverFill;
        ActionButton(String label,Icon icon, Color fill, Color hoverFill, Color fg) {
            super(label,icon); this.fill=fill;this.hoverFill=hoverFill;
            setFont(font(13,true));setForeground(fg);setFocusPainted(false);
            setContentAreaFilled(false);setOpaque(false);setBorderPainted(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.CENTER);
            setIconTextGap(9);
            setBorder(new EmptyBorder(10,14,10,14));
            setRolloverEnabled(true);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D d=(Graphics2D)g.create();aa(d);
            Color bg = getModel().isRollover()?hoverFill:fill;
            if (!isEnabled()) bg=new Color(234,238,243);
            d.setColor(bg);
            d.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
            d.dispose();super.paintComponent(g);
        }
    }
    static final class NavButton extends JButton {
        final String type;
        boolean active;
        NavButton(String label,String type) {
            super(label);this.type=type;setForeground(new Color(222,236,251));
            setFont(font(14,false));setOpaque(false);setContentAreaFilled(false);
            setFocusPainted(false);setBorderPainted(false);setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(14);setBorder(new EmptyBorder(9,18,9,8));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(210,46));
            setMinimumSize(new Dimension(210,46));
            setMaximumSize(new Dimension(210,46));
            setIcon(UiKit.icon(type,19,new Color(190,218,243)));
        }
        void setActive(boolean active){this.active=active;setFont(font(14,active));
            setIcon(UiKit.icon(type,19,active?WHITE:new Color(190,218,243)));repaint();}
        protected void paintComponent(Graphics g) {
            Graphics2D d=(Graphics2D)g.create();aa(d);
            if (active || getModel().isRollover()) {
                d.setColor(active?new Color(55,142,212):new Color(45,87,131));
                d.fillRoundRect(6,2,getWidth()-12,getHeight()-4,12,12);
                if(active){d.setColor(new Color(199,231,255));d.fillRoundRect(7,8,3,getHeight()-16,3,3);}
            }
            d.dispose();setForeground(active?WHITE:new Color(219,231,243));
            super.paintComponent(g);
        }
    }
    /** Fundo discreto para os icones dos indicadores, preservando area de respiro. */
    static JPanel iconTile(String type, int glyphSize, final Color accent) {
        JPanel p = new JPanel(new java.awt.GridBagLayout()) {
            @Override protected void paintComponent(Graphics old) {
                Graphics2D g=(Graphics2D)old.create(); aa(g);
                Color pale=new Color(Math.min(255,accent.getRed()+170),
                    Math.min(255,accent.getGreen()+110),Math.min(255,accent.getBlue()+105));
                g.setColor(new Color(pale.getRed(),pale.getGreen(),pale.getBlue(),145));
                g.fillRoundRect(2,2,getWidth()-4,getHeight()-4,12,12);
                g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),70));
                g.drawRoundRect(2,2,getWidth()-5,getHeight()-5,12,12);
                g.dispose();super.paintComponent(old);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(53,53));
        p.add(new JLabel(icon(type,glyphSize,accent)));
        return p;
    }
}
