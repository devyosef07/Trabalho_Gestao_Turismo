package com.turismo.frontend.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.turismo.frontend.ui.motion.Motion;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Palette;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

/**
 * Campo de formulário: rótulo, controlo e uma linha de ajuda que passa a
 * mensagem de erro (com ícone e contorno vermelho no controlo) quando o valor
 * é inválido. A linha tem sempre a mesma altura, por isso mostrar ou limpar
 * um erro não faz saltar o resto do formulário. O erro aparece com um fade
 * curto.
 */
public class FormField extends JPanel {

    private final JComponent input;
    private final String hint;
    private final JLabel message;
    private String error;
    private float messageAlpha = 1f;
    private final JPanel extraSlot = new JPanel(new java.awt.BorderLayout());

    public FormField(String label, JComponent input, String hint) {
        this.input = input;
        this.hint = hint;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        extraSlot.setOpaque(false);
        extraSlot.setAlignmentX(Component.LEFT_ALIGNMENT);
        extraSlot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));

        JLabel title = UI.stretch(UI.fieldLabel(label));
        title.setLabelFor(input);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(6));

        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, input.getPreferredSize().height));
        add(input);
        add(extraSlot);
        add(Box.createVerticalStrut(5));

        message = UI.text(" ", Theme::small, () -> {
            Color c = error != null ? Theme.p().danger : Theme.p().textMuted;
            return Palette.alpha(c, Math.round(255 * messageAlpha));
        });
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.setIconTextGap(5);
        Dimension line = new Dimension(Integer.MAX_VALUE, Theme.small().getSize() + 8);
        message.setMinimumSize(new Dimension(0, line.height));
        message.setPreferredSize(new Dimension(10, line.height));
        message.setMaximumSize(line);
        add(message);
        showHint();
    }

    public JComponent input() {
        return input;
    }

    /** Componente entre o controlo e a linha de ajuda/erro (ex.: força da palavra-passe). */
    public void setExtra(JComponent extra) {
        extraSlot.removeAll();
        extraSlot.setOpaque(false);
        extraSlot.setBorder(UI.pad(UI.S2, 0, 0, 0));
        extraSlot.add(extra);
        extraSlot.setAlignmentX(Component.LEFT_ALIGNMENT);
        extraSlot.setMaximumSize(new Dimension(Integer.MAX_VALUE, extraSlot.getPreferredSize().height));
        revalidate();
    }

    public String error() {
        return error;
    }

    /** Mostra o erro junto do campo; null volta a mostrar a ajuda. */
    public void setError(String text) {
        String value = text == null || text.isBlank() ? null : text;
        if (java.util.Objects.equals(value, error)) {
            return;
        }
        boolean appearing = error == null && value != null;
        error = value;
        outline(input, value == null ? null : FlatClientProperties.OUTLINE_ERROR);
        if (value == null) {
            showHint();
            input.getAccessibleContext().setAccessibleDescription(hint);
        } else {
            message.setText(value);
            message.setIcon(Icons.of(MaterialDesignA.ALERT_CIRCLE_OUTLINE, 14, () -> Theme.p().danger));
            input.getAccessibleContext().setAccessibleDescription("Erro: " + value);
            if (appearing) {
                Motion.animate(Motion.FAST, t -> {
                    messageAlpha = t;
                    message.repaint();
                }, null);
            }
        }
    }

    private void showHint() {
        messageAlpha = 1f;
        message.setIcon(null);
        message.setText(hint == null ? " " : hint);
    }

    /** Aplica o contorno também aos controlos dentro de um painel (ex.: data em três listas). */
    private static void outline(Component c, Object value) {
        if (c instanceof JComponent && !(c instanceof JPanel)) {
            ((JComponent) c).putClientProperty(FlatClientProperties.OUTLINE, value);
        }
        if (c instanceof JPanel) {
            for (Component child : ((Container) c).getComponents()) {
                outline(child, value);
            }
        }
    }
}
