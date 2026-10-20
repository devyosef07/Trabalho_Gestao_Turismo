package com.turismo.frontend.modules;

import com.google.gson.JsonObject;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabela sobre as linhas JSON devolvidas pela API. Devolve valores
 * tipados (datas, números) para a ordenação por coluna ser correta.
 */
public class RowsTableModel extends AbstractTableModel {

    private final List<ColumnSpec> columns = new ArrayList<>();
    private List<JsonObject> rows = new ArrayList<>();

    public RowsTableModel(List<ColumnSpec> all) {
        for (ColumnSpec c : all) {
            if (!c.detailOnly) {
                columns.add(c);
            }
        }
    }

    public void setRows(List<JsonObject> rows) {
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public JsonObject rowAt(int modelIndex) {
        return rows.get(modelIndex);
    }

    public List<JsonObject> rows() {
        return rows;
    }

    public ColumnSpec column(int index) {
        return columns.get(index);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).header;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (columns.get(column).type) {
            case INTEGER: return Integer.class;
            case DECIMAL:
            case MONEY:
            case RATING: return BigDecimal.class;
            case DATE: return LocalDate.class;
            default: return String.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return value(columns.get(columnIndex), rows.get(rowIndex));
    }

    static Object value(ColumnSpec c, JsonObject row) {
        if (c.derived != null) {
            return c.derived.apply(row);
        }
        switch (c.type) {
            case INTEGER: return Format.integer(row, c.key);
            case DECIMAL:
            case MONEY:
            case RATING: return Format.dec(row, c.key);
            case DATE: return Format.parseIsoDate(Format.str(row, c.key));
            default: return Format.str(row, c.key);
        }
    }

    /** Texto mostrado ao utilizador (usado também pela pesquisa e pelo detalhe). */
    static String display(ColumnSpec c, Object value) {
        if (value == null) {
            return Format.EMPTY;
        }
        switch (c.type) {
            case MONEY: return Format.money((BigDecimal) value);
            case RATING: return Format.rating((BigDecimal) value);
            case DECIMAL: return Format.number((BigDecimal) value);
            case DATE: return Format.date((LocalDate) value);
            default: return value.toString();
        }
    }
}
