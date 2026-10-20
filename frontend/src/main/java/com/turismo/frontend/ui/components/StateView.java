package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.GridBagLayout;

/**
 * Estados de uma área de conteúdo: a carregar, vazio e erro.
 * "Vazio" (a consulta correu bem e não há registos) e "erro" (a consulta
 * falhou) têm sempre aparência e texto diferentes.
 */
public class StateView extends JPanel {

    private final JPanel column = new JPanel();
    private JLabel messageLabel;
    private String message;
    private int messageWidth;

    public StateView() {
        super(new GridBagLayout());
        setOpaque(false);
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        add(column);
    }

    public StateView loading(String message) {
        column.removeAll();
        Spinner spinner = new Spinner(28);
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(spinner);
        column.add(Box.createVerticalStrut(UI.S3));
        column.add(centered(UI.secondary(message)));
        refresh();
        return this;
    }

    public StateView empty(Ikon icon, String title, String message, JButton action) {
        return show(icon, () -> Theme.p().textMuted, title, message, action);
    }

    public StateView error(String title, String message, Runnable retry) {
        JButton button = null;
        if (retry != null) {
            button = UI.secondary("Tentar novamente", MaterialDesignR.REFRESH);
            button.addActionListener(e -> retry.run());
        }
        return show(MaterialDesignA.ALERT_CIRCLE_OUTLINE, () -> Theme.p().danger, title, message, button);
    }

    /** Dados indisponíveis por um problema conhecido do esquema (ex.: view em falta). */
    public StateView unavailable(String title, String message) {
        return show(org.kordamp.ikonli.materialdesign2.MaterialDesignD.DATABASE_ALERT_OUTLINE,
                () -> Theme.p().warning, title, message, null);
    }

    public StateView unreachable(String message, Runnable retry) {
        JButton button = UI.secondary("Tentar novamente", MaterialDesignR.REFRESH);
        button.addActionListener(e -> retry.run());
        return show(MaterialDesignC.CLOUD_OFF_OUTLINE, () -> Theme.p().warning, "Sem ligação", message, button);
    }

    private StateView show(Ikon icon, java.util.function.Supplier<java.awt.Color> color, String title,
                           String message, JButton action) {
        column.removeAll();
        JLabel iconLabel = new JLabel(Icons.of(icon, 40, color));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(iconLabel);
        column.add(Box.createVerticalStrut(UI.S3));
        column.add(centered(UI.section(title)));
        this.message = message;
        this.messageLabel = null;
        if (message != null && !message.isBlank()) {
            column.add(Box.createVerticalStrut(UI.S1));
            messageLabel = UI.secondary("");
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageWidth = -1;
            updateMessageWidth();
            column.add(centered(messageLabel));
        }
        if (action != null) {
            column.add(Box.createVerticalStrut(UI.S4));
            action.setAlignmentX(Component.CENTER_ALIGNMENT);
            column.add(action);
        }
        refresh();
        return this;
    }

    /** A mensagem quebra à largura disponível (máximo 360 px), sem ser cortada. */
    private void updateMessageWidth() {
        if (messageLabel == null) {
            return;
        }
        int width = Math.max(160, Math.min(360, (getWidth() > 0 ? getWidth() : 400) - 48));
        if (width != messageWidth) {
            messageWidth = width;
            messageLabel.setText("<html><div style='text-align:center;width:" + width + "px'>" + escape(message) + "</div></html>");
        }
    }

    @Override
    public void doLayout() {
        updateMessageWidth();
        super.doLayout();
    }

    private static JLabel centered(JLabel label) {
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    private void refresh() {
        revalidate();
        repaint();
    }
}
