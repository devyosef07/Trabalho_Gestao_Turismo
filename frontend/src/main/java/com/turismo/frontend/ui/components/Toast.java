package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;

/**
 * Notificação breve no canto superior direito da janela, que desaparece
 * sozinha. A mensagem também é enviada para a barra de estado (ouvinte), para
 * ficar visível depois de a notificação fechar.
 */
public final class Toast extends JComponent {

    private static final int WIDTH = 380;
    private static Consumer<String> statusListener;

    private final UI.Tone tone;
    private final Timer hideTimer;

    private Toast(String message, UI.Tone tone) {
        this.tone = tone;
        setLayout(new BorderLayout(UI.S2, 0));
        setBorder(UI.pad(UI.S3, UI.S3, UI.S3, UI.S4));
        org.kordamp.ikonli.Ikon icon = tone == UI.Tone.DANGER ? MaterialDesignA.ALERT_CIRCLE_OUTLINE
                : tone == UI.Tone.SUCCESS ? MaterialDesignC.CHECK_CIRCLE_OUTLINE : MaterialDesignI.INFORMATION_OUTLINE;
        add(new JLabel(Icons.of(icon, 18, () -> UI.toneText(tone))), BorderLayout.WEST);
        JLabel text = UI.body("<html><div style='width:300px'>" + StateView.escape(message) + "</div></html>");
        add(text, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName(message);
        hideTimer = new Timer(tone == UI.Tone.DANGER ? 7000 : 3500, e -> close());
        hideTimer.setRepeats(false);
    }

    public static void setStatusListener(Consumer<String> listener) {
        statusListener = listener;
    }

    public static void success(JComponent anchor, String message) {
        show(anchor, message, UI.Tone.SUCCESS);
    }

    public static void error(JComponent anchor, String message) {
        show(anchor, message, UI.Tone.DANGER);
    }

    public static void info(JComponent anchor, String message) {
        show(anchor, message, UI.Tone.INFO);
    }

    public static void show(JComponent anchor, String message, UI.Tone tone) {
        if (statusListener != null) {
            statusListener.accept(message);
        }
        JRootPane root = anchor == null ? null : SwingUtilities.getRootPane(anchor);
        if (root == null) {
            return;
        }
        JLayeredPane layers = root.getLayeredPane();
        for (java.awt.Component c : layers.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
            if (c instanceof Toast) {
                ((Toast) c).close();
            }
        }
        Toast toast = new Toast(message, tone);
        Dimension size = toast.getPreferredSize();
        int width = Math.max(WIDTH, size.width);
        toast.setBounds(layers.getWidth() - width - UI.S5, UI.S5, width, size.height);
        layers.add(toast, JLayeredPane.POPUP_LAYER);
        layers.revalidate();
        layers.repaint();
        toast.hideTimer.start();
    }

    void close() {
        hideTimer.stop();
        java.awt.Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.p().card);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g2.setColor(Theme.p().border);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g2.setColor(UI.toneText(tone));
        g2.fillRoundRect(0, 0, 4, getHeight() - 1, 4, 4);
        g2.dispose();
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            // JComponent não cria contexto de acessibilidade; os leitores de ecrã precisam dele.
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.ALERT;
                }
            };
        }
        return accessibleContext;
    }
}
