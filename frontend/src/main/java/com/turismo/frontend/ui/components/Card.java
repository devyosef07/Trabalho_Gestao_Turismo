package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.theme.Palette;
import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Superfície elevada: fundo de cartão, contorno de 1 px e uma sombra suave
 * (três camadas translúcidas, sem desfoque — barata de pintar).
 *
 * A sombra é desenhada dentro dos limites do próprio cartão; por isso
 * {@link #getInsets()} acrescenta essa margem ao border definido por quem usa
 * o cartão, e o conteúdo nunca fica por cima da sombra.
 *
 * Opcional: {@link #setHoverable} realça o cartão ao passar o rato (cartões
 * clicáveis) e {@link #reveal} fá-lo aparecer com fade e deslocamento.
 */
public class Card extends JPanel {

    /** Margem reservada para a sombra (esquerda/direita, cima, baixo). */
    private static final int SIDE = 2, TOP = 1, BOTTOM = 4;

    private final int arc;
    private boolean hoverable;
    private boolean hover;
    private float alpha = 1f;
    private float dy;
    private Motion.Handle running;

    public Card(LayoutManager layout) {
        this(layout, 14);
    }

    public Card(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
    }

    @Override
    public Color getBackground() {
        return Theme.p().card;
    }

    @Override
    public Insets getInsets() {
        Insets in = super.getInsets();
        return new Insets(in.top + TOP, in.left + SIDE, in.bottom + BOTTOM, in.right + SIDE);
    }

    /** Realce ao passar o rato (contorno azul e sombra mais forte). */
    public void setHoverable(boolean hoverable) {
        if (this.hoverable == hoverable) {
            return;
        }
        this.hoverable = hoverable;
        if (hoverable) {
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setHover(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // Sair para um filho não conta como sair do cartão.
                    if (!contains(e.getPoint())) {
                        setHover(false);
                    }
                }
            };
            addMouseListener(m);
        }
    }

    private void setHover(boolean value) {
        if (hover != value) {
            hover = value;
            repaint();
        }
    }

    /** Aparece com fade e uma subida de 12 px, depois de {@code delayMs}. */
    public void reveal(int delayMs) {
        if (running != null) {
            running.cancel();
        }
        running = Motion.animate(Motion.NORMAL + 60, delayMs, t -> {
            alpha = t;
            dy = 12 * (1 - t);
            repaint();
        }, () -> running = null);
    }

    @Override
    public boolean isPaintingOrigin() {
        return alpha < 1f || dy != 0;
    }

    @Override
    public void paint(Graphics g) {
        if (alpha >= 1f && dy == 0) {
            super.paint(g);
            return;
        }
        if (alpha <= 0f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.translate(0, Math.round(dy));
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = SIDE;
        int y = TOP;
        int w = getWidth() - SIDE * 2;
        int h = getHeight() - TOP - BOTTOM;
        Palette p = Theme.p();

        int base = p.dark ? 40 : (hover ? 16 : 9);
        for (int i = 3; i >= 1; i--) {
            g2.setColor(Palette.alpha(p.shadow, base / i));
            g2.fillRoundRect(x - i + 1, y + i, w + (i - 1) * 2, h + i, arc + i * 2, arc + i * 2);
        }

        g2.setColor(getBackground());
        g2.fillRoundRect(x, y, w - 1, h - 1, arc, arc);
        g2.setColor(hover ? Palette.alpha(p.primary, 140) : p.border);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
