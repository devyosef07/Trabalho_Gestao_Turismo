package com.turismo.frontend.ui.motion;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/**
 * Contentor que consegue aparecer com fade e um pequeno deslocamento
 * (entrada de ecrãs, cartões e formulários).
 *
 * Só se animam a opacidade e a translação da pintura: o layout não muda,
 * por isso os componentes continuam no sítio certo e utilizáveis durante a
 * animação. Enquanto anima é a "origem de pintura" dos filhos, para que um
 * filho que se repinte sozinho (ex.: indicador de carregamento) também
 * respeite a opacidade.
 */
public class MotionPanel extends JPanel {

    private float alpha = 1f;
    private float dx;
    private float dy;
    private Motion.Handle running;

    public MotionPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    /** Aparece vindo de baixo (dy &gt; 0) ou de lado (dx), com fade. */
    public Motion.Handle reveal(int durationMs, int delayMs, float fromDx, float fromDy) {
        if (running != null) {
            running.cancel();
        }
        running = Motion.animate(durationMs, delayMs, t -> {
            alpha = t;
            dx = fromDx * (1 - t);
            dy = fromDy * (1 - t);
            repaint();
        }, () -> running = null);
        return running;
    }

    public Motion.Handle reveal(int delayMs) {
        return reveal(Motion.NORMAL, delayMs, 0, 10);
    }

    public boolean isAnimating() {
        return running != null && running.isRunning();
    }

    /** Termina já a animação em curso (ex.: o ecrã vai ser trocado). */
    public void settle() {
        if (running != null) {
            running.finish();
        }
    }

    @Override
    public boolean isPaintingOrigin() {
        return alpha < 1f || dx != 0 || dy != 0;
    }

    @Override
    public void paint(Graphics g) {
        if (alpha >= 1f && dx == 0 && dy == 0) {
            super.paint(g);
            return;
        }
        if (alpha <= 0f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.translate(Math.round(dx), Math.round(dy));
        super.paint(g2);
        g2.dispose();
    }
}
