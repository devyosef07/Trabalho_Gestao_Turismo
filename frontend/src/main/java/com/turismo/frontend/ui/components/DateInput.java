package com.turismo.frontend.ui.components;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Data escolhida em três listas (dia, mês, ano). A lista dos dias acompanha o
 * mês e o ano (28 a 31 dias), por isso é impossível escolher uma data que não
 * existe, como 31 de fevereiro. Os anos vão do atual até 1900.
 */
public class DateInput extends JPanel {

    static final String[] MONTHS = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho",
            "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    private final JComboBox<String> day = new JComboBox<>();
    private final JComboBox<String> month = new JComboBox<>();
    private final JComboBox<String> year = new JComboBox<>();
    private Runnable onChange = () -> { };
    private boolean updating;

    public DateInput(LocalDate today) {
        super(new GridBagLayout());
        setOpaque(false);
        month.addItem("Mês");
        for (String m : MONTHS) {
            month.addItem(m);
        }
        year.addItem("Ano");
        for (int y = today.getYear(); y >= 1900; y--) {
            year.addItem(String.valueOf(y));
        }
        year.setMaximumRowCount(12);
        month.setMaximumRowCount(13);
        day.setMaximumRowCount(12);
        fillDays(31);

        day.getAccessibleContext().setAccessibleName("Dia de nascimento");
        month.getAccessibleContext().setAccessibleName("Mês de nascimento");
        year.getAccessibleContext().setAccessibleName("Ano de nascimento");
        day.setName("nascimento-dia");
        month.setName("nascimento-mes");
        year.setName("nascimento-ano");

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.weightx = 0.26;
        c.insets = new Insets(0, 0, 0, UI.S2);
        add(day, c);
        c.weightx = 0.44;
        add(month, c);
        c.weightx = 0.30;
        c.insets = new Insets(0, 0, 0, 0);
        add(year, c);

        month.addActionListener(e -> adjustDays());
        year.addActionListener(e -> adjustDays());
        day.addActionListener(e -> fire());
    }

    public void setOnChange(Runnable r) {
        this.onChange = r;
    }

    /** A data escolhida, ou null enquanto faltar o dia, o mês ou o ano. */
    public LocalDate getDate() {
        if (day.getSelectedIndex() <= 0 || month.getSelectedIndex() <= 0 || year.getSelectedIndex() <= 0) {
            return null;
        }
        return LocalDate.of(Integer.parseInt((String) year.getSelectedItem()), month.getSelectedIndex(),
                day.getSelectedIndex());
    }

    public boolean isPartial() {
        int chosen = (day.getSelectedIndex() > 0 ? 1 : 0) + (month.getSelectedIndex() > 0 ? 1 : 0)
                + (year.getSelectedIndex() > 0 ? 1 : 0);
        return chosen > 0 && chosen < 3;
    }

    public void setDate(LocalDate date) {
        updating = true;
        try {
            year.setSelectedItem(String.valueOf(date.getYear()));
            month.setSelectedIndex(date.getMonthValue());
            fillDays(YearMonth.of(date.getYear(), date.getMonthValue()).lengthOfMonth());
            day.setSelectedIndex(date.getDayOfMonth());
        } finally {
            updating = false;
        }
        fire();
    }

    public JComboBox<String> dayBox() {
        return day;
    }

    private void adjustDays() {
        if (updating) {
            return;
        }
        int length = 31;
        if (month.getSelectedIndex() > 0) {
            int y = year.getSelectedIndex() > 0 ? Integer.parseInt((String) year.getSelectedItem()) : 2000;
            length = YearMonth.of(y, month.getSelectedIndex()).lengthOfMonth();
        }
        if (day.getItemCount() - 1 != length) {
            updating = true;
            try {
                int selected = day.getSelectedIndex();
                fillDays(length);
                day.setSelectedIndex(Math.min(selected, length));
            } finally {
                updating = false;
            }
        }
        fire();
    }

    private void fillDays(int length) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Dia");
        for (int d = 1; d <= length; d++) {
            model.addElement(String.valueOf(d));
        }
        day.setModel(model);
    }

    private void fire() {
        if (!updating) {
            onChange.run();
        }
    }
}
