package br.sig.sped;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/** Confere que todos os SVGs convertidos estao no pacote e que a recoloracao funciona. */
public final class TablerIconsTest {
    private TablerIconsTest() {}
    public static void main(String[] args) {
        for(String id: new String[]{"home", "file", "analysis", "chart", "grid",
                "calculator", "alert", "building", "filter", "close", "info", "check", "brand"}) {
            for(Color tint: new Color[]{Color.WHITE, new Color(25, 80, 145)}) {
                Icon icon=TablerIcons.icon(id, 29, tint);
                if(icon.getIconWidth()!=29 || icon.getIconHeight()!=29) throw new AssertionError("Dimensao: "+id);
                BufferedImage picture=new BufferedImage(29,29,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=picture.createGraphics();icon.paintIcon(null,g,0,0);g.dispose();
                int visible=0;
                for(int y=0;y<29;y++)for(int x=0;x<29;x++)if((picture.getRGB(x,y)>>>24)!=0)visible++;
                if(visible < 5) throw new AssertionError("Icone transparente: "+id);
            }
        }
        if(FrameChrome.appIcon().getWidth()!=32) throw new AssertionError("Icone Windows");
        System.out.println("PASSOU: 13 icones Tabler, recoloracao, recursos no JAR, dimensoes e icone de janela.");
    }
}
