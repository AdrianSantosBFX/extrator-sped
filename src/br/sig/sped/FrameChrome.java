package br.sig.sped;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Moldura visual do aplicativo (v0.1.2): barra superior/rodape inspirados na referencia aprovada.
 *  Java 8 + Swing puro; nenhum recurso externo e nenhum acesso a rede.
 */
final class FrameChrome {
    private FrameChrome() {}

    static final Color TEXT = new Color(25, 56, 101);
    static final Color EDGE = new Color(165, 187, 209);

    static JMenuBar decorateMenu(JMenuBar menu) {
        menu.setBackground(new Color(238,245,252));
        menu.setOpaque(true);
        menu.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(197,214,231)),
            BorderFactory.createEmptyBorder(0,13,0,9)));
        menu.setPreferredSize(new Dimension(100,27));
        for(Component c: menu.getComponents()) {
            if(c instanceof JMenu) {
                JMenu m=(JMenu)c;
                m.setFont(UiKit.font(12,false));
                m.setForeground(TEXT);
                m.setOpaque(false);
                m.setBorder(BorderFactory.createEmptyBorder(2,9,2,9));
            }
        }
        return menu;
    }

    static JButton toolbarButton(String name,String glyph,String first,String second) {
        return new ToolbarButton(name,glyph,first,second);
    }

    static JPanel ribbon(JButton open,JButton refresh,JButton export,JButton help) {
        RibbonSurface top=new RibbonSurface();
        top.setLayout(new BorderLayout());
        top.setPreferredSize(new Dimension(10,96));
        top.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(138,170,200)));

        JPanel tools=new JPanel();
        tools.setOpaque(false);
        tools.setLayout(new javax.swing.BoxLayout(tools,javax.swing.BoxLayout.X_AXIS));
        tools.setBorder(BorderFactory.createEmptyBorder(9,26,9,0));
        tools.add(separator());
        tools.add(open);
        tools.add(separator());
        tools.add(refresh);
        tools.add(separator());
        tools.add(export);
        tools.add(separator());
        tools.add(help);
        tools.add(separator());
        top.add(tools,BorderLayout.WEST);

        // Identidade corporativa: simbolo especifico de planilha fiscal (Tabler),
        // nome com hierarquia e subtitulo, sem alterar os botoes da toolbar.
        JPanel brand=new JPanel(new BorderLayout(12,0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(8,12,8,23));
        JLabel file=new JLabel(new BrandGlyph());
        brand.add(file,BorderLayout.WEST);
        JPanel name=new JPanel();
        name.setOpaque(false);
        name.setLayout(new javax.swing.BoxLayout(name,javax.swing.BoxLayout.Y_AXIS));
        JLabel n1=new JLabel("SPED");
        n1.setFont(UiKit.font(18,true));
        n1.setForeground(new Color(204,232,255));
        JLabel n2=new JLabel("FISCAL");
        n2.setFont(UiKit.font(22,true));
        n2.setForeground(Color.WHITE);
        JLabel n3=new JLabel("EFD ICMS/IPI  •  LEITURA LOCAL");
        n3.setFont(UiKit.font(9,false));
        n3.setForeground(new Color(194,223,243));
        name.add(n1);name.add(n2);name.add(javax.swing.Box.createVerticalStrut(3));name.add(n3);
        brand.add(name,BorderLayout.CENTER);
        top.add(brand,BorderLayout.EAST);
        return top;
    }

    static JPanel statusBar(JLabel icon,JLabel status,String version) {
        StatusSurface p=new StatusSurface();p.setLayout(new BorderLayout());
        p.setPreferredSize(new Dimension(100,28));
        p.setBorder(BorderFactory.createEmptyBorder(3,13,3,15));
        JPanel left=new JPanel(new BorderLayout(7,0));left.setOpaque(false);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(19,17));
        left.add(icon,BorderLayout.WEST);
        status.setFont(UiKit.font(11,false));status.setForeground(TEXT);
        left.add(status,BorderLayout.CENTER);
        p.add(left,BorderLayout.CENTER);
        JLabel right=new JLabel("Extrator SPED Fiscal    v"+version);
        right.setFont(UiKit.font(10,false));right.setForeground(new Color(64,96,132));
        right.setBorder(BorderFactory.createEmptyBorder(0,24,0,0));
        p.add(right,BorderLayout.EAST);
        return p;
    }

    static Icon statusIcon(String kind) {return new StatusGlyph(kind);}

    static BufferedImage appIcon() {
        BufferedImage im=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=im.createGraphics();antialias(g);
        g.setPaint(new GradientPaint(0,0,new Color(44,128,206),32,32,new Color(16,59,116)));
        g.fillRoundRect(1,1,30,30,7,7);
        g.setColor(new Color(209,240,255,170));g.setStroke(new BasicStroke(.9f));
        g.drawRoundRect(1,1,29,29,7,7);
        TablerIcons.icon("brand",24,Color.WHITE).paintIcon(null,g,4,4);
        g.dispose();return im;
    }

    private static JPanel separator() {
        JPanel sep=new JPanel(){
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(167,191,213,180));
                g.drawLine(0,1,0,getHeight()-2);
                g.setColor(new Color(255,255,255,140));
                g.drawLine(1,1,1,getHeight()-2);
            }
        };
        sep.setOpaque(false);
        Dimension d=new Dimension(2,77);
        sep.setMinimumSize(d);sep.setMaximumSize(d);sep.setPreferredSize(d);
        return sep;
    }

    private static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static final class RibbonSurface extends JPanel {
        private static final long serialVersionUID=1L;
        RibbonSurface(){setOpaque(false);}
        protected void paintComponent(Graphics src) {
            Graphics2D g=(Graphics2D)src.create();antialias(g);
            int w=getWidth(),h=getHeight();
            g.setPaint(new GradientPaint(0,0,new Color(248,252,255),0,h,new Color(208,227,245)));
            g.fillRect(0,0,w,h);
            // Tres camadas geometricas suaves no lado direito (modelo aprovado).
            Path2D shade=new Path2D.Double();
            shade.moveTo(w*.72,0);shade.lineTo(w,0);shade.lineTo(w,h);shade.lineTo(w*.48,h);
            shade.curveTo(w*.58,h*.53,w*.64,h*.32,w*.72,0);shade.closePath();
            g.setPaint(new GradientPaint(w*.57f,h*.6f,new Color(150,198,235),w,h*.55f,new Color(28,80,132)));
            g.fill(shade);
            Path2D wave=new Path2D.Double();
            wave.moveTo(w*.60,h);wave.curveTo(w*.73,h*.37,w*.82,h*.44,w,h*.08);
            wave.lineTo(w,0);wave.lineTo(w,h);wave.closePath();
            g.setPaint(new GradientPaint(w*.59f,h,new Color(119,177,226,145),w,0,new Color(8,62,117,45)));
            g.fill(wave);
            Path2D light=new Path2D.Double();
            light.moveTo(w*.50,h);light.curveTo(w*.69,h*.87,w*.83,h*.04,w,0);
            light.lineTo(w*.78,0);light.curveTo(w*.69,h*.35,w*.61,h*.76,w*.50,h);light.closePath();
            g.setColor(new Color(203,229,249,43));g.fill(light);
            g.dispose();super.paintComponent(src);
        }
    }

    private static final class ToolbarButton extends JButton {
        private static final long serialVersionUID=1L;
        private final String glyph,first,second;
        ToolbarButton(String name,String glyph,String first,String second) {
            super(name);this.glyph=glyph;this.first=first;this.second=second;
            setOpaque(false);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);
            setRolloverEnabled(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(TEXT);setFont(UiKit.font(13,false));
            setToolTipText(name);
            Dimension d=new Dimension(112,77);
            setPreferredSize(d);setMinimumSize(d);setMaximumSize(d);
        }
        protected void paintComponent(Graphics src) {
            Graphics2D g=(Graphics2D)src.create();antialias(g);
            if(getModel().isRollover() && isEnabled()) {
                g.setColor(new Color(255,255,255,135));g.fillRoundRect(4,0,getWidth()-8,getHeight()-1,5,5);
                g.setColor(new Color(122,171,219,115));g.drawRoundRect(4,0,getWidth()-9,getHeight()-2,5,5);
            }
            if(getModel().isPressed() && isEnabled()) {
                g.setColor(new Color(151,194,227,75));g.fillRoundRect(4,0,getWidth()-8,getHeight()-1,5,5);
            }
            int x=(getWidth()-35)/2;
            Graphics2D glyphG=(Graphics2D)g.create();
            if(!isEnabled())glyphG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.38f));
            drawGlyph(glyphG,glyph,x,7,35);glyphG.dispose();
            FontMetrics fm=g.getFontMetrics(getFont());
            g.setFont(getFont());g.setColor(isEnabled()?TEXT:new Color(128,149,169));
            int y=58;
            g.drawString(first,(getWidth()-fm.stringWidth(first))/2,y);
            if(second!=null && !second.isEmpty())g.drawString(second,(getWidth()-fm.stringWidth(second))/2,y+15);
            g.dispose();
        }
    }

    private static void drawGlyph(Graphics2D orig,String kind,int x,int y,int side) {
        Graphics2D g=(Graphics2D)orig.create();antialias(g);
        g.translate(x,y);g.scale(side/36.0,side/36.0);
        if("folder".equals(kind)) {
            g.setColor(new Color(74,152,225));g.fillRoundRect(2,9,32,23,4,4);
            g.setColor(new Color(33,112,199));g.fillRoundRect(2,8,32,23,4,4);
            g.setColor(new Color(71,157,226));g.fillRoundRect(4,4,14,7,3,3);
            g.setPaint(new GradientPaint(6,13,new Color(98,177,238),29,30,new Color(19,100,183)));
            g.fillRoundRect(3,12,30,19,3,3);
            g.setColor(new Color(159,213,249,150));g.drawLine(6,14,29,14);
        } else if("refresh".equals(kind)) {
            g.setColor(new Color(28,113,208));g.setStroke(new BasicStroke(4.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.drawArc(6,5,24,24,45,205);g.drawArc(6,5,24,24,224,164);
            Path2D a=new Path2D.Double();a.moveTo(25,4);a.lineTo(32,13);a.lineTo(21,13);a.closePath();g.fill(a);
            Path2D b=new Path2D.Double();b.moveTo(11,32);b.lineTo(4,23);b.lineTo(15,23);b.closePath();g.fill(b);
        } else if("download".equals(kind)) {
            g.setColor(new Color(19,110,80));g.fillRoundRect(7,3,23,30,3,3);
            g.setColor(new Color(28,156,103));g.fillRoundRect(12,3,18,30,2,2);
            g.setColor(new Color(157,221,194));g.fillRect(23,8,4,2);g.fillRect(23,13,4,2);g.fillRect(23,18,4,2);
            g.setColor(new Color(12,121,74));g.fillRoundRect(3,9,22,20,2,2);
            g.setFont(new Font("Arial",Font.BOLD,18));g.setColor(Color.WHITE);g.drawString("X",7,25);
        } else if("help".equals(kind)) {
            g.setPaint(new GradientPaint(4,3,new Color(69,144,230),32,31,new Color(25,65,161)));
            g.fillOval(3,2,30,30);
            g.setColor(new Color(203,232,254,90));g.setStroke(new BasicStroke(1.2f));g.drawOval(4,3,28,28);
            g.setColor(Color.WHITE);g.setFont(new Font("Segoe UI",Font.BOLD,26));g.drawString("?",11,26);
        }
        g.dispose();
    }

    private static final class BrandGlyph implements Icon {
        public int getIconWidth(){return 60;}
        public int getIconHeight(){return 64;}
        public void paintIcon(Component c,Graphics src,int x,int y) {
            Graphics2D g=(Graphics2D)src.create();antialias(g);
            g.translate(x,y);
            g.setPaint(new GradientPaint(5,6,new Color(160,208,248,128),54,57,new Color(39,104,169,170)));
            g.fillRoundRect(2,3,56,58,13,13);
            g.setColor(new Color(219,240,255,135));
            g.setStroke(new BasicStroke(1.1f));g.drawRoundRect(3,4,54,56,12,12);
            TablerIcons.icon("brand",40,new Color(243,251,255)).paintIcon(c,g,10,12);
            // Traço de assinatura no canto do icone sem competir com o titulo.
            g.setColor(new Color(85,220,198));g.fillRoundRect(21,58,18,2,2,2);
            g.dispose();
        }
    }

    private static final class StatusSurface extends JPanel {
        private static final long serialVersionUID=1L;
        StatusSurface(){setOpaque(false);}
        protected void paintComponent(Graphics src) {
            Graphics2D g=(Graphics2D)src.create();
            g.setPaint(new GradientPaint(0,0,new Color(219,233,245),0,getHeight(),new Color(245,250,255)));
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(EDGE);g.drawLine(0,0,getWidth(),0);
            g.setColor(new Color(255,255,255,185));g.drawLine(0,1,getWidth(),1);
            g.dispose();super.paintComponent(src);
        }
    }

    private static final class StatusGlyph implements Icon {
        private final String kind;
        StatusGlyph(String kind){this.kind=kind;}
        public int getIconWidth(){return 17;}
        public int getIconHeight(){return 17;}
        public void paintIcon(Component c,Graphics src,int x,int y) {
            Graphics2D g=(Graphics2D)src.create();antialias(g);g.translate(x,y);
            Color fill="alert".equals(kind)?new Color(207,71,72):"busy".equals(kind)?new Color(48,122,197):new Color(38,163,38);
            g.setColor(fill);g.fillOval(2,2,13,13);
            g.setColor(Color.WHITE);g.setStroke(new BasicStroke(1.9f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            if("alert".equals(kind)){g.drawLine(8,5,8,10);g.fillOval(7,12,2,2);}
            else if("busy".equals(kind)){g.drawLine(8,5,8,9);g.fillOval(7,11,2,2);}
            else {g.drawLine(5,8,7,11);g.drawLine(7,11,12,6);}
            g.dispose();
        }
    }
}
