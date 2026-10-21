package br.sig.sped;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.Icon;

/**
 * Adaptador Swing do Tabler Icons (MIT): imagens rasterizadas dos SVG oficiais,
 * armazenadas dentro do JAR. Colorizacao por mascara alfa e cache em memoria.
 * Sem bibliotecas externas, acesso a rede ou JavaFX. Compativel com Java 8.
 */
final class TablerIcons {
    private TablerIcons() {}
    private static final Map<String, String> ALIASES = new HashMap<String, String>();
    private static final Map<String, BufferedImage> MASKS = new HashMap<String, BufferedImage>();
    private static final ConcurrentHashMap<String, Icon> CACHE = new ConcurrentHashMap<String, Icon>();
    static {
        ALIASES.put("home", "layout-dashboard");
        ALIASES.put("file", "file-invoice");
        ALIASES.put("analysis", "report-analytics");
        ALIASES.put("chart", "chart-bar");
        ALIASES.put("grid", "table");
        ALIASES.put("calculator", "calculator");
        ALIASES.put("alert", "alert-triangle");
        ALIASES.put("building", "building-warehouse");
        ALIASES.put("filter", "filter");
        ALIASES.put("close", "x");
        ALIASES.put("info", "info-circle");
        ALIASES.put("check", "circle-check");
        ALIASES.put("brand", "file-spreadsheet");
        ALIASES.put("database", "database");
    }

    static Icon icon(String type, int size, Color color) {
        if(size < 1 || color == null) throw new IllegalArgumentException("Icone invalido");
        String id = ALIASES.containsKey(type) ? ALIASES.get(type) : type;
        String key = id + "/" + size + "/" + color.getRGB();
        Icon cached = CACHE.get(key);
        if(cached != null) return cached;
        BufferedImage source = mask(id);
        // Melhor enxergar falha de pacote em desenvolvimento que mostrar um quadrado generico.
        if(source == null) throw new IllegalStateException("Icone Tabler nao foi empacotado: " + id);
        BufferedImage colored = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = colored.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, size, size, null);
        g.setComposite(AlphaComposite.SrcIn);
        g.setColor(color);
        g.fillRect(0, 0, size, size);
        g.dispose();
        Icon result = new ImageIconLite(colored);
        Icon previously = CACHE.putIfAbsent(key, result);
        return previously != null ? previously : result;
    }

    private static synchronized BufferedImage mask(String name) {
        if(MASKS.containsKey(name)) return MASKS.get(name);
        String resource = "/br/sig/sped/icons/" + name + ".png";
        try (InputStream stream = TablerIcons.class.getResourceAsStream(resource)) {
            if(stream == null) return null;
            BufferedImage source = ImageIO.read(stream);
            if(source != null) MASKS.put(name, source);
            return source;
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel ler o icone: " + name, ex);
        }
    }

    private static final class ImageIconLite implements Icon {
        final BufferedImage image;
        ImageIconLite(BufferedImage image) { this.image = image; }
        public int getIconWidth() { return image.getWidth(); }
        public int getIconHeight() { return image.getHeight(); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.drawImage(image,x,y,null);
        }
    }
}
