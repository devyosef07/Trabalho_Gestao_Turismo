package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Avatar circular com as iniciais do nome (primeiro e último nome) sobre um
 * gradiente azul. Pode mostrar um ponto verde de "sessão iniciada".
 */
public class Avatar extends JComponent {

    private final int size;
    private final Supplier<Color> ring;
    private String initials = "";
    private boolean online;

    /** {@code ring}: cor do fundo onde o avatar está (contorno do ponto verde). */
    public Avatar(int size, Supplier<Color> ring) {
        this.size = size;
        this.ring = ring;
        setOpaque(false);
    }

    public void setPerson(String fullName, boolean online) {
        this.initials = initials(fullName);
        this.online = online;
        getAccessibleContext().setAccessibleName(fullName == null ? "Sem conta" : "Conta de " + fullName);
        repaint();
    }

    public static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    public String initials() {
        return initials;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int d = Math.min(getWidth(), getHeight());
        g2.setPaint(new GradientPaint(0, 0, Theme.p().brandBright, d, d, Theme.p().brandMid));
        g2.fill(new Ellipse2D.Float(0, 0, d, d));
        g2.setColor(new Color(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Ellipse2D.Float(0.5f, 0.5f, d - 1, d - 1));

        Font font = Theme.strong().deriveFont(d * 0.38f);
        g2.setFont(font);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int tx = (d - fm.stringWidth(initials)) / 2;
        int ty = (d - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(initials, tx, ty);

        if (online) {
            float dot = Math.max(8f, d * 0.26f);
            float ox = d - dot + 1;
            float oy = d - dot + 1;
            g2.setColor(ring.get());
            g2.fill(new Ellipse2D.Float(ox - 2, oy - 2, dot + 2, dot + 2));
            g2.setColor(new Color(0x22, 0xC5, 0x5E));
            g2.fill(new Ellipse2D.Float(ox, oy, dot - 2, dot - 2));
        }
        g2.dispose();
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            // JComponent não cria contexto de acessibilidade; os leitores de ecrã precisam dele.
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.ICON;
                }
            };
        }
        return accessibleContext;
    }
}
