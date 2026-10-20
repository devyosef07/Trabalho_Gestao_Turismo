package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Gráfico de barras de uma só série, desenhado com Java2D.
 *
 * Barras horizontais para categorias (nomes longos cabem à esquerda) e colunas
 * para séries temporais. Barras com no máximo 24 px, ponta arredondada (4 px)
 * e base reta, grelha de 1 px discreta, valor na ponta e dica com o valor exato.
 * Uma série não precisa de legenda: o título do cartão diz o que está desenhado.
 */
public class BarChart extends JComponent {

    public enum Orientation { BARS, COLUMNS }

    public static final class Point {
        final String label;
        final double value;

        public Point(String label, double value) {
            this.label = label;
            this.value = value;
        }

        public String label() {
            return label;
        }

        public double value() {
            return value;
        }
    }

    private static final int BAR = 24;
    private static final int GAP = 10;

    private final Orientation orientation;
    private final Function<Double, String> format;
    private final Function<Double, String> axisFormat;
    private List<Point> points = new ArrayList<>();
    private final List<Rectangle> hitAreas = new ArrayList<>();
    private int hover = -1;

    public BarChart(Orientation orientation, Function<Double, String> format, Function<Double, String> axisFormat) {
        this.orientation = orientation;
        this.format = format;
        this.axisFormat = axisFormat;
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = indexAt(e.getX(), e.getY());
                if (index != hover) {
                    hover = index;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setData(List<Point> data) {
        this.points = new ArrayList<>(data);
        StringBuilder summary = new StringBuilder();
        for (Point p : data) {
            summary.append(p.label).append(": ").append(format.apply(p.value)).append(". ");
        }
        getAccessibleContext().setAccessibleDescription(summary.toString());
        revalidate();
        repaint();
    }

    public int pointCount() {
        return points.size();
    }

    @Override
    public Dimension getPreferredSize() {
        if (orientation == Orientation.BARS) {
            return new Dimension(320, Math.max(120, points.size() * (BAR + GAP) + 36));
        }
        return new Dimension(Math.max(320, points.size() * 56), 240);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int index = indexAt(e.getX(), e.getY());
        if (index < 0) {
            return null;
        }
        Point p = points.get(index);
        return p.label + ": " + format.apply(p.value);
    }

    private int indexAt(int x, int y) {
        for (int i = 0; i < hitAreas.size(); i++) {
            if (hitAreas.get(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        hitAreas.clear();
        if (points.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double max = 0;
        for (Point p : points) {
            max = Math.max(max, p.value);
        }
        double top = niceMax(max);
        if (orientation == Orientation.BARS) {
            paintBars(g2, top);
        } else {
            paintColumns(g2, top);
        }
        g2.dispose();
    }

    private void paintBars(Graphics2D g, double top) {
        g.setFont(Theme.small());
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = 0;
        for (Point p : points) {
            labelWidth = Math.max(labelWidth, fm.stringWidth(p.label));
        }
        labelWidth = Math.min(labelWidth, Math.max(80, getWidth() / 3));
        int valueRoom = 0;
        for (Point p : points) {
            valueRoom = Math.max(valueRoom, fm.stringWidth(format.apply(p.value)));
        }
        int x0 = labelWidth + UI.S3;
        int plotWidth = Math.max(10, getWidth() - x0 - valueRoom - UI.S3);
        int y = 4;

        // Grelha vertical em 4 divisões
        g.setColor(Theme.p().chartGrid);
        int bottom = y + points.size() * (BAR + GAP);
        for (int i = 0; i <= 4; i++) {
            int gx = x0 + (int) Math.round(plotWidth * i / 4.0);
            g.drawLine(gx, y, gx, bottom - GAP);
        }
        g.setColor(Theme.p().textMuted);
        for (int i = 0; i <= 4; i++) {
            String tick = axisFormat.apply(top * i / 4.0);
            int gx = x0 + (int) Math.round(plotWidth * i / 4.0);
            g.drawString(tick, Math.min(gx - fm.stringWidth(tick) / 2, getWidth() - fm.stringWidth(tick)),
                    bottom + fm.getAscent());
        }

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            int by = y + i * (BAR + GAP);
            int w = top <= 0 ? 0 : (int) Math.round(plotWidth * p.value / top);
            g.setColor(Theme.p().textSecondary);
            g.drawString(ellipsize(p.label, fm, labelWidth), 0, by + (BAR - fm.getHeight()) / 2 + fm.getAscent());
            g.setColor(Theme.p().chartSeries);
            if (hover >= 0 && hover != i) {
                g.setColor(withAlpha(Theme.p().chartSeries, 150));
            }
            g.fill(roundedEnd(x0, by, Math.max(w, 2), BAR, true));
            g.setColor(Theme.p().text);
            g.drawString(format.apply(p.value), x0 + w + UI.S2, by + (BAR - fm.getHeight()) / 2 + fm.getAscent());
            hitAreas.add(new Rectangle(0, by - GAP / 2, getWidth(), BAR + GAP));
        }
    }

    private void paintColumns(Graphics2D g, double top) {
        g.setFont(Theme.small());
        FontMetrics fm = g.getFontMetrics();
        int axisWidth = fm.stringWidth(axisFormat.apply(top)) + UI.S2;
        int x0 = axisWidth;
        int yTop = fm.getHeight() + 4;
        int yBase = getHeight() - fm.getHeight() - UI.S2;
        int plotHeight = Math.max(10, yBase - yTop);
        int slot = Math.max(1, (getWidth() - x0) / points.size());
        int barWidth = Math.min(BAR, Math.max(4, slot - 12));

        for (int i = 0; i <= 4; i++) {
            int gy = yBase - (int) Math.round(plotHeight * i / 4.0);
            g.setColor(Theme.p().chartGrid);
            g.drawLine(x0, gy, getWidth(), gy);
            g.setColor(Theme.p().textMuted);
            String tick = axisFormat.apply(top * i / 4.0);
            g.drawString(tick, axisWidth - UI.S2 - fm.stringWidth(tick), gy + fm.getAscent() / 2 - 1);
        }
        int maxIndex = 0;
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).value > points.get(maxIndex).value) {
                maxIndex = i;
            }
        }
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            int cx = x0 + slot * i + slot / 2;
            int h = top <= 0 ? 0 : (int) Math.round(plotHeight * p.value / top);
            g.setColor(hover >= 0 && hover != i ? withAlpha(Theme.p().chartSeries, 150) : Theme.p().chartSeries);
            g.fill(roundedEnd(cx - barWidth / 2, yBase - Math.max(h, 2), barWidth, Math.max(h, 2), false));
            g.setColor(Theme.p().textSecondary);
            String label = ellipsize(p.label, fm, slot - 4);
            g.drawString(label, cx - fm.stringWidth(label) / 2, yBase + fm.getAscent() + 4);
            // Rótulo direto só no máximo e no ponto sob o rato: os restantes ficam na dica e na tabela.
            if (i == maxIndex || i == hover) {
                g.setColor(Theme.p().text);
                String v = format.apply(p.value);
                g.drawString(v, cx - fm.stringWidth(v) / 2, yBase - h - 6);
            }
            hitAreas.add(new Rectangle(cx - slot / 2, yTop, slot, plotHeight + fm.getHeight() + UI.S2));
        }
    }

    /** Retângulo com cantos de 4 px só na ponta dos dados; a base fica reta. */
    private static Path2D roundedEnd(double x, double y, double w, double h, boolean horizontal) {
        double r = Math.min(4, Math.min(w, h) / 2);
        Path2D path = new Path2D.Double();
        if (horizontal) {
            path.moveTo(x, y);
            path.lineTo(x + w - r, y);
            path.quadTo(x + w, y, x + w, y + r);
            path.lineTo(x + w, y + h - r);
            path.quadTo(x + w, y + h, x + w - r, y + h);
            path.lineTo(x, y + h);
        } else {
            path.moveTo(x, y + h);
            path.lineTo(x, y + r);
            path.quadTo(x, y, x + r, y);
            path.lineTo(x + w - r, y);
            path.quadTo(x + w, y, x + w, y + r);
            path.lineTo(x + w, y + h);
        }
        path.closePath();
        return path;
    }

    static double niceMax(double max) {
        if (max <= 0) {
            return 1;
        }
        double exp = Math.pow(10, Math.floor(Math.log10(max)));
        double f = max / exp;
        double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
        return nice * exp;
    }

    private static String ellipsize(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) {
            return s;
        }
        String dots = "…";
        int end = s.length();
        while (end > 0 && fm.stringWidth(s.substring(0, end) + dots) > width) {
            end--;
        }
        return s.substring(0, end) + dots;
    }

    private static java.awt.Color withAlpha(java.awt.Color c, int alpha) {
        return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            // JComponent não cria contexto de acessibilidade; os leitores de ecrã precisam dele.
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PANEL;
                }
            };
        }
        return accessibleContext;
    }
}
