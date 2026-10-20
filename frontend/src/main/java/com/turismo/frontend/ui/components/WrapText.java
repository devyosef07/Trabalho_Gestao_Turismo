package com.turismo.frontend.ui.components;

import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Supplier;

/**
 * Texto de várias linhas que quebra à largura disponível (ao contrário de um
 * JLabel com HTML, que precisa de uma largura fixa). Só de leitura, mas pode
 * ser selecionado e copiado.
 */
public class WrapText extends JTextArea {

    private final Supplier<Font> font;
    private final Supplier<Color> color;
    private int widthHint = 220;

    public WrapText(String text, Supplier<Font> font, Supplier<Color> color) {
        super(text);
        this.font = font;
        this.color = color;
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(null);
        setHighlighter(null);
        getAccessibleContext().setAccessibleName(text);
    }

    @Override
    public Font getFont() {
        return font == null ? super.getFont() : font.get();
    }

    @Override
    public Color getForeground() {
        return color == null ? super.getForeground() : color.get();
    }

    /** Largura usada para calcular a altura enquanto o componente ainda não tem tamanho. */
    public WrapText widthHint(int width) {
        widthHint = width;
        return this;
    }

    @Override
    public java.awt.Dimension getPreferredSize() {
        int width = getWidth() > 0 ? getWidth() : widthHint;
        javax.swing.text.View root = getUI().getRootView(this);
        java.awt.Insets in = getInsets();
        root.setSize(Math.max(1, width - in.left - in.right), Float.MAX_VALUE);
        int height = (int) Math.ceil(root.getPreferredSpan(javax.swing.text.View.Y_AXIS)) + in.top + in.bottom;
        return new java.awt.Dimension(width, height);
    }

    @Override
    public java.awt.Dimension getMinimumSize() {
        return new java.awt.Dimension(80, super.getMinimumSize().height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean widthChanged = width != getWidth();
        super.setBounds(x, y, width, height);
        if (widthChanged) {
            // A altura depende da largura: pede novo layout com a altura certa.
            javax.swing.SwingUtilities.invokeLater(this::revalidate);
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setOpaque(false);
        setBorder(null);
        // O caret do FlatLaf faz scroll até ao texto e deslocava o painel que o contém.
        // Como este texto é só de leitura, usa-se um caret que nunca faz scroll.
        setCaret(new NoScrollCaret());
    }

    private static final class NoScrollCaret extends javax.swing.text.DefaultCaret {
        NoScrollCaret() {
            setUpdatePolicy(NEVER_UPDATE);
        }

        @Override
        protected void adjustVisibility(java.awt.Rectangle nloc) {
        }
    }
}
