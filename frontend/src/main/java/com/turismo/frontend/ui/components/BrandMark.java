package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Símbolo da aplicação: quadrado arredondado em gradiente azul com um sol a
 * nascer sobre duas ondas (a costa de Moçambique). Desenhado em vetor, fica
 * nítido em qualquer tamanho.
 */
public final class BrandMark implements Icon {

    private final int size;

    public BrandMark(int size) {
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        float s = size;
        g2.setPaint(new GradientPaint(0, 0, Theme.p().brandBright, s, s, Theme.p().brandMid));
        g2.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.32f, s * 0.32f));
        g2.setColor(new Color(255, 255, 255, 40));
        g2.setStroke(new BasicStroke(Math.max(1f, s / 40f)));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, s - 1, s - 1, s * 0.32f, s * 0.32f));

        // Sol
        g2.setColor(new Color(0xBF, 0xDB, 0xFE));
        float r = s * 0.17f;
        g2.fill(new Ellipse2D.Float(s * 0.5f - r, s * 0.40f - r, r * 2, r * 2));

        // Ondas
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(s * 0.075f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(wave(s, 0.62f));
        g2.setColor(new Color(255, 255, 255, 150));
        g2.draw(wave(s, 0.76f));
        g2.dispose();
    }

    private static Path2D wave(float s, float y) {
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.22, s * y);
        p.curveTo(s * 0.34, s * (y - 0.07), s * 0.42, s * (y + 0.07), s * 0.5, s * y);
        p.curveTo(s * 0.58, s * (y - 0.07), s * 0.66, s * (y + 0.07), s * 0.78, s * y);
        return p;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
