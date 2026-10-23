package com.turismo.frontend.auth;

import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Indicador da força da palavra-passe: barra de quatro segmentos que enche
 * com uma transição suave, a palavra (Fraca, Razoável, Boa, Forte) e os três
 * requisitos obrigatórios, que ficam verdes à medida que são cumpridos.
 */
public class PasswordStrength extends JPanel {

    private static final String[] LABELS = {" ", "Fraca", "Razoável", "Boa", "Forte"};

    private final Bar bar = new Bar();
    private final JLabel word = UI.text(" ", Theme::label, this::scoreColor);
    private final JLabel ruleLength = rule("8+ caracteres");
    private final JLabel ruleLetter = rule("uma letra");
    private final JLabel ruleDigit = rule("um número");
    private int score;
    private float shown;
    private Motion.Handle anim;

    public PasswordStrength() {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(UI.S3, 0));
        top.setOpaque(false);
        top.add(bar, BorderLayout.CENTER);
        word.setPreferredSize(new Dimension(64, word.getPreferredSize().height));
        top.add(word, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JPanel rules = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rules.setOpaque(false);
        rules.add(ruleLength);
        rules.add(ruleLetter);
        rules.add(ruleDigit);
        add(rules, BorderLayout.CENTER);
        update("");
    }

    private static JLabel rule(String text) {
        JLabel l = UI.text(text, Theme::small, () -> Theme.p().textMuted);
        l.setBorder(UI.pad(0, 0, 0, UI.S3));
        l.setIconTextGap(3);
        return l;
    }

    public void update(String password) {
        int next = AccountRules.strength(password);
        mark(ruleLength, password.length() >= AccountRules.PASSWORD_MIN);
        mark(ruleLetter, AccountRules.hasLetter(password));
        mark(ruleDigit, AccountRules.hasDigit(password));
        word.setText(LABELS[next]);
        getAccessibleContext().setAccessibleDescription("Força da palavra-passe: " + LABELS[next]);
        if (next == score) {
            return;
        }
        float from = shown;
        score = next;
        if (anim != null) {
            anim.cancel();
        }
        anim = Motion.animate(Motion.NORMAL, t -> {
            shown = Motion.lerp(from, score, t);
            bar.repaint();
        }, null);
    }

    public int score() {
        return score;
    }

    private void mark(JLabel l, boolean ok) {
        l.setIcon(Icons.of(ok ? MaterialDesignC.CHECK_CIRCLE : MaterialDesignC.CHECKBOX_BLANK_CIRCLE_OUTLINE, 13,
                () -> ok ? Theme.p().success : Theme.p().textMuted));
    }

    private Color scoreColor() {
        switch (score) {
            case 1: return Theme.p().danger;
            case 2: return Theme.p().warning;
            case 3: return Theme.p().primary;
            case 4: return Theme.p().success;
            default: return Theme.p().textMuted;
        }
    }

    private final class Bar extends JComponent {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(120, 6);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int gap = 4;
            int h = 6;
            int y = (getHeight() - h) / 2;
            float seg = (getWidth() - gap * 3) / 4f;
            for (int i = 0; i < 4; i++) {
                int x = Math.round(i * (seg + gap));
                g2.setColor(Theme.p().line);
                g2.fillRoundRect(x, y, Math.round(seg), h, h, h);
                float fill = Math.max(0f, Math.min(1f, shown - i));
                if (fill > 0) {
                    g2.setColor(scoreColor());
                    g2.fillRoundRect(x, y, Math.round(seg * fill), h, h, h);
                }
            }
            g2.dispose();
        }
    }
}
