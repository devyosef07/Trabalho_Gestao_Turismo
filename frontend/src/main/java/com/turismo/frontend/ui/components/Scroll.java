package com.turismo.frontend.ui.components;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * ScrollPane sem contorno e transparente. O contorno é retirado também em
 * updateUI(), porque a troca de tema do FlatLaf repõe a borda por omissão.
 */
public class Scroll extends JScrollPane {

    public Scroll(Component view) {
        super(view);
        getVerticalScrollBar().setUnitIncrement(16);
        getHorizontalScrollBar().setUnitIncrement(16);
        strip();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        strip();
    }

    private void strip() {
        setBorder(null);
        setViewportBorder(null);
        if (getViewport() != null) {
            getViewport().setOpaque(false);
        }
        setOpaque(false);
    }

    /** Scroll vertical apenas: o conteúdo acompanha a largura disponível. */
    public static Scroll vertical(Component view) {
        Scroll s = new Scroll(view);
        s.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        return s;
    }

    /** Painel que se ajusta à largura do viewport (sem scroll horizontal). */
    public static class WidthTrackingPanel extends JPanel implements Scrollable {

        public WidthTrackingPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 32, 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
