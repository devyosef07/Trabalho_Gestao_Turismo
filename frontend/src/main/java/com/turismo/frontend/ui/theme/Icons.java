package com.turismo.frontend.ui.theme;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.function.Supplier;

/**
 * Ícones Material Design (Ikonli), desenhados como fonte: nítidos em qualquer
 * escala e iguais em macOS e Windows. A cor é lida no momento de pintar, por
 * isso acompanha a mudança de tema.
 */
public final class Icons {

    private Icons() {
    }

    public static Icon of(Ikon ikon, int size, Supplier<Color> color) {
        return new ThemedIcon(ikon, size, color);
    }

    public static Icon text(Ikon ikon, int size) {
        return of(ikon, size, () -> Theme.p().textSecondary);
    }

    private static final class ThemedIcon implements Icon {
        private final FontIcon icon;
        private final Supplier<Color> color;

        ThemedIcon(Ikon ikon, int size, Supplier<Color> color) {
            this.icon = FontIcon.of(ikon, size);
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color col = color.get();
            if (c != null && !c.isEnabled()) {
                col = new Color(col.getRed(), col.getGreen(), col.getBlue(), 110);
            }
            icon.setIconColor(col);
            icon.paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return icon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return icon.getIconHeight();
        }
    }
}
