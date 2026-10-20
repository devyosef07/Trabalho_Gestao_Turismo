package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.LayoutManager;
import java.util.function.Supplier;

/** Painel opaco cuja cor de fundo acompanha o tema. */
public class Surface extends JPanel {

    private final Supplier<Color> color;

    public Surface(LayoutManager layout) {
        this(layout, () -> Theme.p().page);
    }

    public Surface(LayoutManager layout, Supplier<Color> color) {
        super(layout);
        this.color = color;
        setOpaque(true);
    }

    @Override
    public Color getBackground() {
        return color == null ? super.getBackground() : color.get();
    }
}
