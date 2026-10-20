package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Etiqueta de estado: ponto + texto sobre fundo suave. O texto diz sempre o
 * estado, por isso a informação não depende só da cor.
 */
public class Badge extends JComponent {

    private String text = "";
    private UI.Tone tone = UI.Tone.NEUTRAL;

    public void set(String text, UI.Tone tone) {
        this.text = text == null ? "" : text;
        this.tone = tone;
        getAccessibleContext().setAccessibleName(this.text);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Theme.label());
        return new Dimension(fm.stringWidth(text) + 26, 22);
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintBadge((Graphics2D) g, text, tone, 0, (getHeight() - 22) / 2);
    }

    /** Também usado pelo renderer das tabelas. */
    static void paintBadge(Graphics2D g0, String text, UI.Tone tone, int x, int y) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(Theme.label());
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 26;
        g.setColor(UI.toneSurface(tone));
        g.fillRoundRect(x, y, w, 22, 22, 22);
        g.setColor(UI.toneText(tone));
        g.fillOval(x + 9, y + 8, 6, 6);
        g.drawString(text, x + 19, y + (22 - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            // JComponent não cria contexto de acessibilidade; os leitores de ecrã precisam dele.
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.LABEL;
                }
            };
        }
        return accessibleContext;
    }
}
