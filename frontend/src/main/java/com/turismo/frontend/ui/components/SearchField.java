package com.turismo.frontend.ui.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.turismo.frontend.ui.theme.Icons;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;

import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Campo de pesquisa com ícone, texto de ajuda e botão para limpar. Aplica o
 * filtro 150 ms depois de a pessoa parar de escrever.
 */
public class SearchField extends JTextField {

    private final Timer debounce;

    public SearchField(String placeholder, Consumer<String> onChange) {
        super(22);
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.text(MaterialDesignM.MAGNIFY, 16));
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        getAccessibleContext().setAccessibleName(placeholder);
        debounce = new Timer(150, e -> onChange.accept(getText().trim()));
        debounce.setRepeats(false);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounce.restart();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 260), Math.max(d.height, 34));
    }
}
