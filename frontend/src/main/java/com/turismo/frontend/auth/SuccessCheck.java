package com.turismo.frontend.auth;

import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.theme.Palette;
import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * Confirmação animada: o círculo cresce e o visto desenha-se (cerca de meio
 * segundo, uma única vez). Com movimento reduzido aparece já completo.
 */
class SuccessCheck extends JComponent {

    private final int size;
    private float progress = 1f;

    SuccessCheck(int size) {
        this.size = size;
        setOpaque(false);
    }

    void play() {
        Motion.animate(520, 80, t -> {
            progress = t;
            repaint();
        }, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float circle = Math.min(1f, progress / 0.55f);
        float tick = Math.max(0f, (progress - 0.4f) / 0.6f);
        float d = size * (0.7f + 0.3f * circle);
        float o = (size - d) / 2;
        g2.setColor(Palette.alpha(Theme.p().success, Math.round(40 * circle)));
        g2.fill(new Ellipse2D.Float(0, 0, size, size));
        g2.setColor(Palette.alpha(Theme.p().success, Math.round(255 * circle)));
        g2.fill(new Ellipse2D.Float(o + size * 0.12f, o + size * 0.12f, d - size * 0.24f, d - size * 0.24f));
        if (tick > 0) {
            float s = size;
            float x1 = s * 0.33f, y1 = s * 0.52f;
            float x2 = s * 0.45f, y2 = s * 0.63f;
            float x3 = s * 0.68f, y3 = s * 0.40f;
            Path2D path = new Path2D.Float();
            path.moveTo(x1, y1);
            float first = Math.min(1f, tick / 0.4f);
            path.lineTo(x1 + (x2 - x1) * first, y1 + (y2 - y1) * first);
            if (tick > 0.4f) {
                float second = (tick - 0.4f) / 0.6f;
                path.lineTo(x2 + (x3 - x2) * second, y2 + (y3 - y2) * second);
            }
            g2.setColor(java.awt.Color.WHITE);
            g2.setStroke(new BasicStroke(s * 0.075f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }
        g2.dispose();
    }
}
