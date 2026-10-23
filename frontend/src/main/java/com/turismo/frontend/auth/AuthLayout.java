package com.turismo.frontend.auth;

import com.turismo.frontend.ui.components.BrandMark;
import com.turismo.frontend.ui.components.Scroll;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.components.WrapText;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Palette;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.Ikon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Disposição dos ecrãs de início de sessão e de registo.
 *
 * Janela larga (≥ 960 px): à esquerda a marca sobre o fundo azul (pintado por
 * {@link AuthShell}); à direita uma folha clara, de cantos arredondados, com o
 * formulário. Janela estreita: só o formulário, num cartão centrado sobre o
 * fundo azul. Em ambos os casos o formulário tem largura máxima de 400 px e
 * faz scroll se a janela for mais baixa do que ele.
 */
class AuthLayout extends JPanel {

    static final int WIDE = 960;
    private static final int FORM_WIDTH = 400;
    private static final int INSET = 14;
    private static final int ARC = 32;

    private final JComponent brand;
    private final Sheet sheet;

    AuthLayout(JComponent brand, JComponent form) {
        super(null);
        setOpaque(false);
        this.brand = brand;
        this.sheet = new Sheet(form);
        add(brand);
        add(sheet);
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w >= WIDE) {
            int sheetW = Math.max(480, Math.min(640, Math.round(w * 0.46f)));
            brand.setVisible(true);
            brand.setBounds(0, 0, w - sheetW - INSET, h);
            sheet.setBounds(w - sheetW - INSET, INSET, sheetW, h - INSET * 2);
        } else {
            brand.setVisible(false);
            int sheetW = Math.min(FORM_WIDTH + 72, w - 32);
            int sheetH = Math.min(sheet.preferredHeight(sheetW), h - 32);
            sheet.setBounds((w - sheetW) / 2, (h - sheetH) / 2, sheetW, sheetH);
        }
        sheet.doLayout();
    }

    /** Folha clara (janela larga) ou cartão (janela estreita) com o formulário centrado. */
    private static final class Sheet extends JPanel {
        private final JPanel center;

        Sheet(JComponent form) {
            super(new BorderLayout());
            setOpaque(false);
            JPanel limit = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(FORM_WIDTH, super.getPreferredSize().height);
                }
            };
            limit.setOpaque(false);
            limit.add(form);
            // Ocupa a altura do viewport quando o formulário cabe, para ficar centrado na vertical.
            center = new Scroll.WidthTrackingPanel(new GridBagLayout()) {
                @Override
                public boolean getScrollableTracksViewportHeight() {
                    return getParent() instanceof javax.swing.JViewport
                            && getParent().getHeight() > getPreferredSize().height;
                }
            };
            center.setBorder(UI.pad(UI.S6, UI.S6, UI.S6, UI.S6));
            center.add(limit);
            JScrollPane scroll = Scroll.vertical(center);
            add(scroll, BorderLayout.CENTER);
        }

        int preferredHeight(int width) {
            center.setSize(width, Short.MAX_VALUE);
            return center.getPreferredSize().height + 4;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Palette p = Theme.p();
            for (int i = 4; i >= 1; i--) {
                g2.setColor(new Color(0, 0, 0, 10 + (4 - i) * 6));
                g2.fillRoundRect(-i, i * 2 - 2, getWidth() + i * 2, getHeight(), ARC + i * 2, ARC + i * 2);
            }
            g2.setColor(p.dark ? p.card : p.page);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            g2.setColor(Palette.alpha(Color.WHITE, p.dark ? 18 : 120));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Coluna da marca (texto branco sobre o fundo azul)
    // ------------------------------------------------------------------

    /** Painel da marca: símbolo e nome no topo, mensagem e pontos-chave ao centro, rodapé em baixo. */
    static JComponent brandColumn(String headline, String text, String[][] points) {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(UI.pad(40, 56, 36, 48));

        JLabel name = onDark("Gestão de Turismo", Theme.section().deriveFont(17f), 255);
        name.setIcon(new BrandMark(36));
        name.setIconTextGap(UI.S3);
        root.add(name, BorderLayout.NORTH);

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        JLabel title = onDark("<html>" + headline + "</html>", Theme.display().deriveFont(34f), 255);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        middle.add(title);
        middle.add(Box.createVerticalStrut(UI.S3));
        WrapText lead = new WrapText(text, () -> Theme.body().deriveFont(15f), () -> new Color(255, 255, 255, 205));
        lead.widthHint(420);
        lead.setAlignmentX(Component.LEFT_ALIGNMENT);
        lead.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));
        middle.add(lead);
        middle.add(Box.createVerticalStrut(UI.S6));
        for (String[] point : points) {
            JComponent row = point(point[0], point[1], iconFor(point[2]));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            middle.add(row);
            middle.add(Box.createVerticalStrut(UI.S4));
        }
        JPanel centerHolder = new JPanel(new GridBagLayout());
        centerHolder.setOpaque(false);
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.weightx = 1;
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        centerHolder.add(middle, c);
        root.add(centerHolder, BorderLayout.CENTER);

        JLabel foot = onDark("ISCTEM 2026 · Base de Dados I · Grupo 1", Theme.small(), 150);
        root.add(foot, BorderLayout.SOUTH);
        return root;
    }

    private static JComponent point(String title, String text, Ikon icon) {
        JPanel row = new JPanel(new BorderLayout(UI.S4, 0));
        row.setOpaque(false);
        JLabel badge = new JLabel(Icons.of(icon, 18, () -> Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 55));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setHorizontalAlignment(JLabel.CENTER);
        badge.setPreferredSize(new Dimension(40, 40));
        JPanel badgeHolder = new JPanel(new BorderLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(badge, BorderLayout.NORTH);
        row.add(badgeHolder, BorderLayout.WEST);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(onDark(title, Theme.strong().deriveFont(14f), 255));
        texts.add(Box.createVerticalStrut(2));
        texts.add(onDark(text, Theme.body(), 185));
        row.add(texts, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(480, 48));
        return row;
    }

    private static Ikon iconFor(String name) {
        switch (name) {
            case "mapa": return org.kordamp.ikonli.materialdesign2.MaterialDesignM.MAP_MARKER_RADIUS_OUTLINE;
            case "reserva": return org.kordamp.ikonli.materialdesign2.MaterialDesignC.CALENDAR_CHECK_OUTLINE;
            case "escudo": return org.kordamp.ikonli.materialdesign2.MaterialDesignS.SHIELD_CHECK_OUTLINE;
            case "chave": return org.kordamp.ikonli.materialdesign2.MaterialDesignK.KEY_OUTLINE;
            case "pessoa": return org.kordamp.ikonli.materialdesign2.MaterialDesignA.ACCOUNT_CHECK_OUTLINE;
            default: return org.kordamp.ikonli.materialdesign2.MaterialDesignD.DATABASE_OUTLINE;
        }
    }

    static JLabel onDark(String text, java.awt.Font font, int alpha) {
        return UI.text(text, () -> font, () -> new Color(255, 255, 255, alpha));
    }

    /** Área que o formulário ocupa no ecrã (para testes de responsividade). */
    Rectangle sheetBounds() {
        return sheet.getBounds();
    }

    boolean isBrandVisible() {
        return brand.isVisible();
    }
}
