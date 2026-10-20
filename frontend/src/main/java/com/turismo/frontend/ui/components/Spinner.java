package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;

/**
 * Indicador de carregamento. O temporizador só corre enquanto o componente
 * está visível no ecrã, para não gastar CPU nem deixar timers pendurados.
 */
public class Spinner extends JComponent {

    private final int size;
    private final Timer timer;
    private int angle;

    public Spinner(int size) {
        this.size = size;
        this.timer = new Timer(16, e -> {
            angle = (angle + 8) % 360;
            repaint();
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        });
        getAccessibleContext().setAccessibleName("A carregar");
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float stroke = Math.max(2f, size / 10f);
        int d = (int) (Math.min(getWidth(), getHeight()) - stroke * 2);
        int x = (getWidth() - d) / 2;
        int y = (getHeight() - d) / 2;
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.p().line);
        g2.drawOval(x, y, d, d);
        g2.setColor(Theme.p().primary);
        g2.drawArc(x, y, d, d, -angle, 100);
        g2.dispose();
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            // JComponent não cria contexto de acessibilidade; os leitores de ecrã precisam dele.
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PROGRESS_BAR;
                }
            };
        }
        return accessibleContext;
    }
}
