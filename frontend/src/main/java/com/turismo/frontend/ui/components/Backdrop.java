package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Palette;
import com.turismo.frontend.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Fundos decorativos desenhados em código (sem imagens externas).
 *
 * <ul>
 *   <li>{@link Style#EXPRESSIVE}: ecrãs de autenticação — gradiente azul profundo,
 *   luzes difusas, anéis concêntricos, grelha de pontos e uma rota tracejada
 *   com três marcos (o tema da aplicação: destinos e viagens).</li>
 *   <li>{@link Style#SUBTLE}: áreas de trabalho — cor de página com uma luz azul
 *   muito discreta no canto, para não competir com tabelas e formulários.</li>
 * </ul>
 *
 * O desenho é guardado numa imagem e só é refeito quando muda o tamanho ou o
 * tema; pintar o fundo é, no resto do tempo, copiar essa imagem.
 */
public final class Backdrop {

    public enum Style { EXPRESSIVE, SUBTLE }

    private final Style style;
    private BufferedImage cache;
    private Palette cachedFor;

    public Backdrop(Style style) {
        this.style = style;
    }

    public void paint(Graphics g, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Palette p = Theme.p();
        if (cache == null || cache.getWidth() != w || cache.getHeight() != h || cachedFor != p) {
            cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = cache.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (style == Style.EXPRESSIVE) {
                paintExpressive(g2, w, h, p);
            } else {
                paintSubtle(g2, w, h, p);
            }
            g2.dispose();
            cachedFor = p;
        }
        g.drawImage(cache, 0, 0, null);
    }

    static void paintExpressive(Graphics2D g, int w, int h, Palette p) {
        Color mid = mix(p.brandDeep, p.brandMid, 0.55f);
        g.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.55f, 1f},
                new Color[]{p.brandDeep, mid, p.brandMid}));
        g.fillRect(0, 0, w, h);

        float big = Math.max(w, h);
        glow(g, w * 0.86f, h * 0.12f, big * 0.62f, Palette.alpha(p.glow, p.dark ? 70 : 95));
        glow(g, w * 0.08f, h * 0.98f, big * 0.55f, Palette.alpha(p.brandBright, p.dark ? 110 : 140));
        glow(g, w * 0.55f, h * 0.55f, big * 0.35f, Palette.alpha(p.brandBright, 40));

        // Grelha de pontos que se desvanece a partir do canto superior esquerdo
        int step = 22;
        float reach = Math.min(w, h) * 0.75f;
        for (int y = step; y < h; y += step) {
            for (int x = step; x < w; x += step) {
                float d = (float) Math.hypot(x, y) / reach;
                if (d < 1f) {
                    g.setColor(new Color(255, 255, 255, Math.round(34 * (1 - d))));
                    g.fill(new Ellipse2D.Float(x - 1f, y - 1f, 2f, 2f));
                }
            }
        }

        // Anéis concêntricos a sair do canto inferior direito
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 4; i++) {
            float r = big * (0.28f + i * 0.13f);
            g.setColor(new Color(255, 255, 255, 26 - i * 5));
            g.draw(new Ellipse2D.Float(w * 1.02f - r, h * 1.04f - r, r * 2, r * 2));
        }

        // Rota tracejada com três marcos
        Path2D route = new Path2D.Float();
        route.moveTo(w * 0.06, h * 0.78);
        route.curveTo(w * 0.30, h * 0.60, w * 0.46, h * 0.98, w * 0.70, h * 0.84);
        route.curveTo(w * 0.86, h * 0.74, w * 0.84, h * 0.34, w * 0.96, h * 0.22);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 7f}, 0f));
        g.setColor(new Color(255, 255, 255, 70));
        g.draw(route);
        pin(g, w * 0.06f, h * 0.78f, p);
        pin(g, w * 0.70f, h * 0.84f, p);
        pin(g, w * 0.96f, h * 0.22f, p);
    }

    static void paintSubtle(Graphics2D g, int w, int h, Palette p) {
        g.setColor(p.page);
        g.fillRect(0, 0, w, h);
        float big = Math.max(w, h);
        glow(g, w * 0.98f, -h * 0.05f, big * 0.55f, Palette.alpha(p.primary, p.dark ? 30 : 20));
        glow(g, -w * 0.05f, h * 1.05f, big * 0.40f, Palette.alpha(p.glow, p.dark ? 14 : 18));
    }

    private static void glow(Graphics2D g, float cx, float cy, float radius, Color color) {
        if (radius <= 0) {
            return;
        }
        Color clear = Palette.alpha(color, 0);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), radius, new float[]{0f, 1f},
                new Color[]{color, clear}));
        g.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    private static void pin(Graphics2D g, float x, float y, Palette p) {
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(new Color(255, 255, 255, 46));
        g.fill(new Ellipse2D.Float(x - 11, y - 11, 22, 22));
        g.setColor(new Color(255, 255, 255, 120));
        g.draw(new Ellipse2D.Float(x - 11, y - 11, 22, 22));
        g.setColor(p.glow);
        g.fill(new Ellipse2D.Float(x - 4, y - 4, 8, 8));
    }

    static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
