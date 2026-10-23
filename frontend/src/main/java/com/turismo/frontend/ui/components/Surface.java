package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.util.function.Supplier;

/**
 * Painel opaco cuja cor de fundo acompanha o tema. Sem cor indicada é uma
 * página da área de trabalho e pinta o fundo discreto de {@link Backdrop}.
 */
public class Surface extends JPanel {

    private final Supplier<Color> color;
    private final Backdrop backdrop;

    public Surface(LayoutManager layout) {
        super(layout);
        this.color = () -> Theme.p().page;
        this.backdrop = new Backdrop(Backdrop.Style.SUBTLE);
        setOpaque(true);
    }

    public Surface(LayoutManager layout, Supplier<Color> color) {
        super(layout);
        this.color = color;
        this.backdrop = null;
        setOpaque(true);
    }

    @Override
    public Color getBackground() {
        return color == null ? super.getBackground() : color.get();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (backdrop == null) {
            super.paintComponent(g);
        } else {
            backdrop.paint(g, getWidth(), getHeight());
        }
    }
}
