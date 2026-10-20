package com.turismo.frontend.ui.components;

import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.Ikon;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.function.Supplier;

/**
 * Barra inferior: estado da ligação (API e base de dados), avisos do esquema e
 * a mensagem da última operação.
 */
public class StatusBar extends JPanel {

    private final JLabel connection = UI.text("A verificar ligação…", Theme::small, () -> Theme.p().textSecondary);
    private final JButton schema = new JButton();
    private final JLabel lastMessage = UI.text(" ", Theme::small, () -> Theme.p().textMuted);

    public StatusBar() {
        super(new BorderLayout(UI.S4, 0));
        setOpaque(true);
        setBorder(UI.pad(UI.S1, UI.S4, UI.S1, UI.S4));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, UI.S3, 0));
        left.setOpaque(false);
        left.add(connection);
        schema.setVisible(false);
        schema.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
                com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        schema.setFont(Theme.small());
        left.add(schema);
        add(left, BorderLayout.WEST);
        add(lastMessage, BorderLayout.EAST);
    }

    @Override
    public Color getBackground() {
        return Theme.p().card;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Theme.p().border);
        g.drawLine(0, 0, getWidth(), 0);
    }

    public void setConnection(String text, Ikon icon, Supplier<Color> color) {
        connection.setText(text);
        connection.setIcon(Icons.of(icon, 14, color));
        connection.getAccessibleContext().setAccessibleName(text);
    }

    /** Aviso clicável (ex.: objetos inválidos na BD); null esconde-o. */
    public void setSchemaWarning(String text, Ikon icon, Runnable onClick) {
        for (java.awt.event.ActionListener l : schema.getActionListeners()) {
            schema.removeActionListener(l);
        }
        if (text == null) {
            schema.setVisible(false);
            return;
        }
        schema.setText(text);
        schema.setIcon(Icons.of(icon, 14, () -> Theme.p().warning));
        schema.setToolTipText("Ver detalhes do estado da base de dados");
        schema.addActionListener(e -> onClick.run());
        schema.setVisible(true);
    }

    public void setMessage(String text) {
        lastMessage.setText(text == null || text.isBlank() ? " " : text);
    }

    public String getConnectionText() {
        return connection.getText();
    }
}
