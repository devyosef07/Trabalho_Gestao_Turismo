package com.turismo.frontend.modules;

import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Formata cada célula pelo tipo da coluna: números alinhados à direita,
 * valores vazios discretos e estados como etiqueta (texto + cor).
 */
public class CellRenderer extends DefaultTableCellRenderer {

    private final StatusCell status = new StatusCell();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        RowsTableModel model = (RowsTableModel) table.getModel();
        ColumnSpec spec = model.column(table.convertColumnIndexToModel(column));
        if (spec.type == ColumnSpec.Type.STATUS && value != null) {
            status.text = value.toString();
            status.tone = spec.tone.apply(status.text);
            status.selected = isSelected;
            status.table = table;
            status.setToolTipText(status.text);
            return status;
        }
        super.getTableCellRendererComponent(table, RowsTableModel.display(spec, value), isSelected, false, row, column);
        setBorder(UI.pad(0, UI.S3, 0, UI.S3));
        setFont(Theme.body());
        boolean numeric = spec.type == ColumnSpec.Type.INTEGER || spec.type == ColumnSpec.Type.MONEY
                || spec.type == ColumnSpec.Type.RATING || spec.type == ColumnSpec.Type.DECIMAL;
        setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
        if (!isSelected) {
            setForeground(value == null ? Theme.p().textMuted : Theme.p().text);
        }
        return this;
    }

    private static final class StatusCell extends JComponent {
        String text;
        UI.Tone tone;
        boolean selected;
        JTable table;

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                g.setColor(table.getSelectionBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            BadgePainter.paint((Graphics2D) g, text, tone, UI.S3, (getHeight() - 22) / 2);
        }
    }
}
