package com.turismo.frontend.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.Ikon;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.util.function.Supplier;

/**
 * Peças básicas do design system: texto com papel tipográfico, botões com
 * hierarquia (primário, secundário, perigo, discreto) e espaçamentos.
 */
public final class UI {

    /** Escala de espaçamento (px). */
    public static final int S1 = 4, S2 = 8, S3 = 12, S4 = 16, S5 = 24, S6 = 32;

    public enum Tone { NEUTRAL, INFO, SUCCESS, WARNING, DANGER }

    private UI() {
    }

    public static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    public static Border pad(int all) {
        return pad(all, all, all, all);
    }

    // ---------------------------------------------------------------- texto

    public static JLabel text(String s, Supplier<Font> font, Supplier<Color> color) {
        return new ThemedLabel(s, font, color);
    }

    public static JLabel body(String s) {
        return text(s, Theme::body, () -> Theme.p().text);
    }

    public static JLabel secondary(String s) {
        return text(s, Theme::body, () -> Theme.p().textSecondary);
    }

    public static JLabel muted(String s) {
        return text(s, Theme::small, () -> Theme.p().textMuted);
    }

    public static JLabel fieldLabel(String s) {
        return text(s, Theme::label, () -> Theme.p().textSecondary);
    }

    public static JLabel title(String s) {
        return text(s, Theme::title, () -> Theme.p().text);
    }

    public static JLabel section(String s) {
        return text(s, Theme::section, () -> Theme.p().text);
    }

    /** Texto que relê fonte e cor do tema sempre que é pintado. */
    public static class ThemedLabel extends JLabel {
        private final Supplier<Font> font;
        private final Supplier<Color> color;

        public ThemedLabel(String text, Supplier<Font> font, Supplier<Color> color) {
            super(text);
            this.font = font;
            this.color = color;
        }

        @Override
        public Font getFont() {
            return font == null ? super.getFont() : font.get();
        }

        @Override
        public Color getForeground() {
            return color == null ? super.getForeground() : color.get();
        }

        /**
         * A largura é medida sem métricas fracionárias mas o texto é pintado com
         * elas; esta folga evita que o último carácter seja cortado ("…").
         */
        @Override
        public java.awt.Dimension getPreferredSize() {
            java.awt.Dimension d = super.getPreferredSize();
            if (!isPreferredSizeSet() && getText() != null && !getText().startsWith("<html>")) {
                d.width += 3;
            }
            return d;
        }
    }

    // --------------------------------------------------------------- botões

    public static JButton primary(String text, Ikon icon) {
        JButton b = new JButton(text) {
            @Override
            public Color getBackground() {
                return getModel().isRollover() ? Theme.p().primaryHover : Theme.p().primary;
            }

            @Override
            public Color getForeground() {
                return Theme.p().onPrimary;
            }
        };
        if (icon != null) {
            b.setIcon(Icons.of(icon, 16, () -> Theme.p().onPrimary));
        }
        b.putClientProperty(FlatClientProperties.STYLE, "borderWidth: 0; focusWidth: 2; font: $semibold.font");
        return finish(b);
    }

    public static JButton secondary(String text, Ikon icon) {
        JButton b = new JButton(text);
        if (icon != null) {
            b.setIcon(Icons.of(icon, 16, () -> Theme.p().textSecondary));
        }
        return finish(b);
    }

    public static JButton danger(String text, Ikon icon) {
        JButton b = new JButton(text) {
            @Override
            public Color getForeground() {
                return isEnabled() ? Theme.p().danger : super.getForeground();
            }
        };
        if (icon != null) {
            b.setIcon(Icons.of(icon, 16, () -> Theme.p().danger));
        }
        return finish(b);
    }

    /** Botão só com ícone (barra de ferramentas); o texto vai para a dica e para leitores de ecrã. */
    public static JButton iconButton(Ikon icon, String tooltip) {
        JButton b = new JButton(Icons.of(icon, 18, () -> Theme.p().textSecondary));
        b.setToolTipText(tooltip);
        b.getAccessibleContext().setAccessibleName(tooltip);
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        return finish(b);
    }

    private static JButton finish(JButton b) {
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusable(true);
        return b;
    }

    // ------------------------------------------------------------- cores

    public static Color toneText(Tone tone) {
        switch (tone) {
            case INFO: return Theme.p().info;
            case SUCCESS: return Theme.p().success;
            case WARNING: return Theme.p().warning;
            case DANGER: return Theme.p().danger;
            default: return Theme.p().textSecondary;
        }
    }

    public static Color toneSurface(Tone tone) {
        switch (tone) {
            case INFO: return Theme.p().infoSurface;
            case SUCCESS: return Theme.p().successSurface;
            case WARNING: return Theme.p().warningSurface;
            case DANGER: return Theme.p().dangerSurface;
            default: return Theme.p().neutralSurface;
        }
    }

    public static void setTooltip(JComponent c, String text) {
        c.setToolTipText(text);
        c.getAccessibleContext().setAccessibleDescription(text);
    }
}
