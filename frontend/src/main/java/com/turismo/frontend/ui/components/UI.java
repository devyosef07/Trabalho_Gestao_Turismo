package com.turismo.frontend.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Palette;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.Ikon;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

    public static PrimaryButton primary(String text, Ikon icon) {
        PrimaryButton b = new PrimaryButton(text);
        if (icon != null) {
            b.setIcon(Icons.of(icon, 16, () -> Theme.p().onPrimary));
        }
        b.putClientProperty(FlatClientProperties.STYLE, "borderWidth: 0; focusWidth: 2; font: $semibold.font;"
                + " disabledText: #FFFFFFD0");
        finish(b);
        return b;
    }

    /**
     * Botão translúcido para usar sobre os fundos escuros da autenticação
     * (texto branco, preenchimento de "vidro" e contorno claro).
     */
    public static JButton glass(String text, Ikon icon) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int fill = getModel().isPressed() ? 46 : getModel().isRollover() ? 34 : 20;
                g2.setColor(new Color(255, 255, 255, fill));
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
                g2.setColor(new Color(255, 255, 255, isFocusOwner() ? 230 : 90));
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(Theme.strong());
        b.setBorder(pad(10, 22, 10, 22));
        if (icon != null) {
            b.setIcon(Icons.of(icon, 16, () -> Color.WHITE));
        }
        return finish(b);
    }

    /**
     * Botão principal: gradiente azul, ligeira redução ao carregar e estado
     * "a processar" (indicador animado, texto próprio e cliques ignorados).
     */
    public static class PrimaryButton extends JButton {
        private boolean loading;
        private String idleText;
        private javax.swing.Icon idleIcon;
        private javax.swing.Timer spin;
        private int angle;

        public PrimaryButton(String text) {
            super(text);
            setContentAreaFilled(false);
        }

        @Override
        public Color getForeground() {
            return Theme.p().onPrimary;
        }

        public boolean isLoading() {
            return loading;
        }

        /** Mostra o indicador e {@code text}; com false repõe o texto e o ícone originais. */
        public void setLoading(boolean value, String text) {
            if (value == loading) {
                return;
            }
            loading = value;
            if (value) {
                idleText = getText();
                idleIcon = getIcon();
                setText(text);
                setIcon(new SpinIcon());
                spin = new javax.swing.Timer(16, e -> {
                    angle = (angle + 9) % 360;
                    repaint();
                });
                spin.start();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            } else {
                if (spin != null) {
                    spin.stop();
                }
                setText(idleText);
                setIcon(idleIcon);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            getAccessibleContext().setAccessibleDescription(value ? text : null);
        }

        @Override
        protected void fireActionPerformed(java.awt.event.ActionEvent event) {
            if (!loading) {
                super.fireActionPerformed(event);
            }
        }

        @Override
        public void removeNotify() {
            if (spin != null) {
                spin.stop();
            }
            super.removeNotify();
        }

        @Override
        public void paint(Graphics g) {
            if (getModel().isPressed() && !loading) {
                Graphics2D g2 = (Graphics2D) g.create();
                double s = 0.975;
                g2.translate(getWidth() * (1 - s) / 2, getHeight() * (1 - s) / 2);
                g2.scale(s, s);
                super.paint(g2);
                g2.dispose();
            } else {
                super.paint(g);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int f = 2;
            int w = getWidth() - f * 2;
            int h = getHeight() - f * 2;
            Color top = getModel().isRollover() || loading ? Theme.p().primaryHover : Theme.p().primary;
            Color bottom = getModel().isRollover() || loading ? Theme.p().brandMid : Theme.p().primaryHover;
            if (!isEnabled()) {
                top = Palette.alpha(Theme.p().primary, 120);
                bottom = Palette.alpha(Theme.p().primaryHover, 120);
            }
            g2.setPaint(new GradientPaint(0, f, top, 0, f + h, bottom));
            g2.fillRoundRect(f, f, w, h, 12, 12);
            if (isEnabled()) {
                g2.setColor(new Color(255, 255, 255, 38));
                g2.drawLine(f + 6, f + 1, f + w - 7, f + 1);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.p().focusRing);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        private final class SpinIcon implements javax.swing.Icon {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 255, 255, 70));
                g2.drawOval(x + 1, y + 1, 13, 13);
                g2.setColor(Color.WHITE);
                g2.drawArc(x + 1, y + 1, 13, 13, -angle, 100);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        }
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

    /** Ligação em texto (ex.: "Criar conta"): cor principal, sublinhada ao passar o rato. */
    public static JButton link(String text, Supplier<Color> color) {
        JButton b = new JButton(text) {
            @Override
            public Color getForeground() {
                return color.get();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getModel().isRollover() || isFocusOwner()) {
                    java.awt.FontMetrics fm = g.getFontMetrics(getFont());
                    java.awt.Insets in = getInsets();
                    int iconW = getIcon() == null ? 0 : getIcon().getIconWidth() + getIconTextGap();
                    int textW = fm.stringWidth(getText());
                    int x = in.left + iconW + (getWidth() - in.left - in.right - iconW - textW) / 2;
                    int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2 + 2;
                    g.setColor(getForeground());
                    g.drawLine(x, y, x + textW, y);
                }
            }
        };
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        b.putClientProperty(FlatClientProperties.STYLE, "hoverBackground: null; pressedBackground: null;"
                + " margin: 2,2,2,2; focusWidth: 0");
        b.setFont(Theme.strong());
        b.setContentAreaFilled(false);
        return finish(b);
    }

    public static JButton link(String text) {
        return link(text, () -> Theme.isDark() ? Theme.p().accent : Theme.p().primary);
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

    /**
     * Deixa um texto ocupar toda a largura num BoxLayout vertical: com a largura
     * máxima igual à preferida, o último carácter pode ficar cortado quando a
     * medição e a pintura usam métricas ligeiramente diferentes.
     */
    public static <T extends JComponent> T stretch(T c) {
        c.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    public static void setTooltip(JComponent c, String text) {
        c.setToolTipText(text);
        c.getAccessibleContext().setAccessibleDescription(text);
    }
}
