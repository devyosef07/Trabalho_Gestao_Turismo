package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Mensagem dentro de um formulário (ex.: "credenciais incorretas"): discreta,
 * no sítio onde a pessoa está a olhar e sem janelas por cima. Aparece com um
 * fade curto e fica escondida (sem ocupar espaço) quando não há mensagem.
 */
public class Alert extends JPanel {

    private final JLabel icon = new JLabel();
    private final WrapText text = new WrapText(" ", Theme::body, () -> Theme.p().text);
    private UI.Tone tone = UI.Tone.DANGER;
    private float alpha = 1f;
    /** Espaço livre por baixo da mensagem, para não colar ao campo seguinte. */
    private static final int GAP = 12;

    public Alert() {
        super(new BorderLayout(UI.S2, 0));
        setOpaque(false);
        setBorder(UI.pad(10, 12, 10 + GAP, 12));
        icon.setVerticalAlignment(JLabel.TOP);
        add(icon, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        setVisible(false);
    }

    public void show(UI.Tone tone, String message) {
        this.tone = tone;
        org.kordamp.ikonli.Ikon ikon = tone == UI.Tone.DANGER || tone == UI.Tone.WARNING
                ? MaterialDesignA.ALERT_CIRCLE_OUTLINE
                : tone == UI.Tone.SUCCESS ? MaterialDesignC.CHECK_CIRCLE_OUTLINE : MaterialDesignI.INFORMATION_OUTLINE;
        icon.setIcon(Icons.of(ikon, 18, () -> UI.toneText(this.tone)));
        text.setText(message);
        getAccessibleContext().setAccessibleName(message);
        boolean wasVisible = isVisible();
        setVisible(true);
        revalidate();
        if (!wasVisible) {
            Motion.animate(Motion.NORMAL, t -> {
                alpha = t;
                repaint();
            }, null);
        }
    }

    public void hideMessage() {
        setVisible(false);
        revalidate();
    }

    public String message() {
        return isVisible() ? getAccessibleContext().getAccessibleName() : null;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (alpha < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UI.toneSurface(tone));
        g2.fillRoundRect(0, 0, getWidth(), getHeight() - GAP, 12, 12);
        g2.setColor(UI.toneText(tone));
        g2.fillRoundRect(0, 0, 4, getHeight() - GAP, 4, 4);
        g2.dispose();
    }

    @Override
    public boolean isPaintingOrigin() {
        return alpha < 1f;
    }
}
