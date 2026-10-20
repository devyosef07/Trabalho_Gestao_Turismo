package com.turismo.frontend.modules;

import com.turismo.frontend.ui.components.Badge;
import com.turismo.frontend.ui.components.UI;

import java.awt.Graphics2D;

/** Acesso ao desenho das etiquetas de estado a partir das tabelas. */
final class BadgePainter {

    private static final Badge BADGE = new Badge();

    private BadgePainter() {
    }

    static void paint(Graphics2D g, String text, UI.Tone tone, int x, int y) {
        BADGE.set(text, tone);
        g.translate(x, y);
        BADGE.setSize(BADGE.getPreferredSize());
        BADGE.paint(g);
        g.translate(-x, -y);
    }
}
