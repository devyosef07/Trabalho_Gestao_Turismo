package com.turismo.frontend.modules;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Spinner;
import com.turismo.frontend.ui.components.Surface;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.theme.Icons;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Formulário genérico de criação/edição, construído a partir dos {@link FieldSpec}.
 *
 * As opções das listas são carregadas em segundo plano ao abrir; a gravação
 * também. Enquanto grava, o botão fica desativado (evita submissões duplicadas)
 * e os erros devolvidos pela API aparecem junto ao campo respetivo.
 */
public class FormDialog extends JDialog {

    private static final Pattern HORA = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");

    private final ModuleSpec spec;
    private final Lookups lookups;
    private final JsonObject existing;
    private final Consumer<JsonObject> onSaved;
    private final Map<String, Input> inputs = new LinkedHashMap<>();
    private final Map<String, JLabel> errors = new LinkedHashMap<>();
    private final JLabel banner = new JLabel();
    private final JButton save;
    private final JButton cancel = UI.secondary("Cancelar", null);
    private final Spinner busy = new Spinner(18);
    private boolean optionsReady;

    public FormDialog(Window owner, ModuleSpec spec, Lookups lookups, JsonObject existing, Consumer<JsonObject> onSaved) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.spec = spec;
        this.lookups = lookups;
        this.existing = existing;
        this.onSaved = onSaved;
        boolean creating = existing == null;
        setTitle(creating ? spec.newLabel() : "Editar " + spec.singular + " " + spec.keyText(existing));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        Surface root = new Surface(new BorderLayout(), () -> Theme.p().card);
        root.setBorder(UI.pad(UI.S5, UI.S5, UI.S4, UI.S5));
        setContentPane(root);

        JPanel head = new JPanel(new BorderLayout(0, UI.S1));
        head.setOpaque(false);
        head.add(UI.section(getTitle()), BorderLayout.NORTH);
        if (!creating) {
            head.add(UI.muted("Campos da chave não podem ser alterados."), BorderLayout.CENTER);
        }
        banner.setVisible(false);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.p().danger), UI.pad(UI.S2, UI.S3, UI.S2, UI.S3)));
        banner.setForeground(Theme.p().danger);
        head.add(banner, BorderLayout.SOUTH);
        head.setBorder(UI.pad(0, 0, UI.S4, 0));
        root.add(head, BorderLayout.NORTH);

        JPanel form = new com.turismo.frontend.ui.components.Scroll.WidthTrackingPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;
        for (FieldSpec f : spec.fields) {
            if (creating ? !f.onCreate : !f.onEdit) {
                continue;
            }
            Input input = create(f);
            inputs.put(f.key, input);
            JLabel label = UI.fieldLabel(f.label + (f.required ? " *" : ""));
            label.setLabelFor(input.focusTarget());
            c.gridy = row++;
            c.insets = new Insets(row == 1 ? 0 : UI.S3, 0, UI.S1, 0);
            form.add(label, c);
            c.gridy = row++;
            c.insets = new Insets(0, 0, 0, 0);
            form.add(input.component(), c);
            JLabel hint = UI.muted(f.help == null ? " " : f.help);
            JLabel error = new JLabel(" ");
            error.setFont(Theme.small());
            error.setForeground(Theme.p().danger);
            errors.put(f.key, error);
            JPanel below = new JPanel(new BorderLayout());
            below.setOpaque(false);
            below.add(error, BorderLayout.NORTH);
            if (f.help != null) {
                below.add(hint, BorderLayout.SOUTH);
            }
            c.gridy = row++;
            c.insets = new Insets(2, 0, 0, 0);
            form.add(below, c);
        }
        c.gridy = row;
        c.weighty = 1;
        form.add(Box.createVerticalGlue(), c);

        root.add(com.turismo.frontend.ui.components.Scroll.vertical(form), BorderLayout.CENTER);

        save = UI.primary(creating ? "Criar " + spec.singular : "Guardar alterações", null);
        save.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        busy.setVisible(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UI.S2, 0));
        buttons.setOpaque(false);
        buttons.setBorder(UI.pad(UI.S4, 0, 0, 0));
        buttons.add(busy);
        buttons.add(cancel);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setMinimumSize(new Dimension(460, 300));
        pack();
        // Altura suficiente para todos os campos, até 85% do ecrã (depois disso o formulário faz scroll).
        int screen = getGraphicsConfiguration() == null ? 900 : getGraphicsConfiguration().getBounds().height;
        int wanted = form.getPreferredSize().height + head.getPreferredSize().height
                + buttons.getPreferredSize().height + UI.S5 + UI.S4 + 48;
        setSize(new Dimension(Math.max(520, getWidth()), Math.min(wanted, (int) (screen * 0.85))));
        setLocationRelativeTo(owner);
        loadOptions();
    }

    // ------------------------------------------------------------- opções

    private void loadOptions() {
        List<Input> needing = new ArrayList<>();
        for (Input i : inputs.values()) {
            if (i.needsOptions()) {
                needing.add(i);
            }
        }
        if (needing.isEmpty()) {
            optionsReady = true;
            fill();
            return;
        }
        setBusy(true);
        Task.run(() -> {
            Map<Input, Object> loaded = new LinkedHashMap<>();
            for (Input i : needing) {
                FieldSpec f = i.field;
                if (f.type == FieldSpec.Type.DOMAIN) {
                    List<String> values = new ArrayList<>(lookups.domain(f.domainKey));
                    values.removeAll(f.excluded);
                    loaded.put(i, values);
                } else {
                    loaded.put(i, lookups.rows(f.fkEndpoint));
                }
            }
            return loaded;
        }, loaded -> {
            loaded.forEach(Input::setOptions);
            optionsReady = true;
            fill();
            setBusy(false);
        }, error -> {
            setBusy(false);
            save.setEnabled(false);
            showBanner("Não foi possível carregar as opções do formulário: " + error.getMessage());
        });
    }

    private void fill() {
        for (Input i : inputs.values()) {
            if (existing != null) {
                i.set(existing);
            } else if (i.field.defaultValue != null) {
                JsonObject d = new JsonObject();
                d.addProperty(i.field.key, i.field.defaultValue);
                i.set(d);
            }
        }
        Input first = inputs.values().stream().findFirst().orElse(null);
        if (first != null) {
            first.focusTarget().requestFocusInWindow();
        }
    }

    // ----------------------------------------------------------- gravação

    private void submit() {
        if (!optionsReady || !save.isEnabled()) {
            return;
        }
        clearErrors();
        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, String> problems = new LinkedHashMap<>();
        for (Input i : inputs.values()) {
            try {
                Object v = i.value();
                validate(i.field, v, problems);
                values.put(i.field.key, v);
            } catch (IllegalArgumentException e) {
                problems.put(i.field.key, e.getMessage());
            }
        }
        if (problems.isEmpty()) {
            problems.putAll(spec.crossValidation.apply(values));
        }
        if (!problems.isEmpty()) {
            showErrors(problems);
            return;
        }
        JsonObject body = toJson(values);
        setBusy(true);
        Task.run(() -> existing == null
                        ? lookups.api().post(spec.endpoint, body)
                        : lookups.api().put(spec.pathOf(existing), body),
                result -> {
                    dispose();
                    onSaved.accept(result);
                },
                error -> {
                    setBusy(false);
                    if (error instanceof ApiException && !((ApiException) error).getFields().isEmpty()) {
                        showErrors(((ApiException) error).getFields());
                        showBanner(error.getMessage());
                    } else {
                        showBanner(error.getMessage());
                    }
                });
    }

    static void validate(FieldSpec f, Object v, Map<String, String> problems) {
        if (v == null || (v instanceof String && ((String) v).isEmpty())) {
            if (f.required) {
                problems.put(f.key, "Campo obrigatório.");
            }
            return;
        }
        switch (f.type) {
            case TEXT:
            case TEXTAREA:
                if (f.maxLength > 0 && ((String) v).length() > f.maxLength) {
                    problems.put(f.key, "Máximo de " + f.maxLength + " caracteres (tem " + ((String) v).length() + ").");
                }
                break;
            case INTEGER:
                int n = (Integer) v;
                if (n <= 0) {
                    problems.put(f.key, "Tem de ser maior que zero.");
                } else if (f.maxDigits != null && String.valueOf(n).length() > f.maxDigits) {
                    problems.put(f.key, "Máximo de " + f.maxDigits + " dígitos.");
                }
                break;
            case MONEY:
                BigDecimal m = (BigDecimal) v;
                if (m.signum() <= 0) {
                    problems.put(f.key, "Tem de ser maior que zero.");
                } else if (m.stripTrailingZeros().scale() > 2) {
                    problems.put(f.key, "Use no máximo duas casas decimais.");
                } else if (m.compareTo(new BigDecimal("99999999.99")) > 0) {
                    problems.put(f.key, "Valor demasiado elevado.");
                }
                break;
            case RATING:
                BigDecimal r = (BigDecimal) v;
                if (r.compareTo(BigDecimal.ZERO) < 0 || r.compareTo(new BigDecimal("5")) > 0) {
                    problems.put(f.key, "Tem de estar entre 0,0 e 5,0.");
                } else if (r.stripTrailingZeros().scale() > 1) {
                    problems.put(f.key, "Use no máximo uma casa decimal.");
                }
                break;
            case TIME:
                if (!HORA.matcher((String) v).matches()) {
                    problems.put(f.key, "Use o formato HH:MM (00:00 a 23:59).");
                }
                break;
            default:
                break;
        }
    }

    private static JsonObject toJson(Map<String, Object> values) {
        JsonObject o = new JsonObject();
        values.forEach((k, v) -> {
            if (v == null) {
                o.add(k, com.google.gson.JsonNull.INSTANCE);
            } else if (v instanceof Number) {
                o.addProperty(k, (Number) v);
            } else if (v instanceof LocalDate) {
                o.addProperty(k, v.toString());
            } else if (v instanceof List) {
                JsonArray a = new JsonArray();
                for (Object item : (List<?>) v) {
                    a.add(item.toString());
                }
                o.add(k, a);
            } else {
                o.addProperty(k, v.toString());
            }
        });
        return o;
    }

    private void setBusy(boolean busyNow) {
        busy.setVisible(busyNow);
        save.setEnabled(!busyNow && optionsReady);
        cancel.setEnabled(true);
        for (Input i : inputs.values()) {
            i.component().setEnabled(!busyNow);
        }
        if (!busyNow && !optionsReady) {
            save.setEnabled(false);
        }
    }

    private void clearErrors() {
        banner.setVisible(false);
        for (Map.Entry<String, JLabel> e : errors.entrySet()) {
            // O espaço da mensagem de erro existe sempre, para o formulário não "saltar".
            e.getValue().setText(" ");
            e.getValue().setIcon(null);
            Input i = inputs.get(e.getKey());
            i.focusTarget().putClientProperty(FlatClientProperties.OUTLINE, null);
        }
    }

    private void showErrors(Map<String, String> problems) {
        JComponent firstInvalid = null;
        for (Map.Entry<String, String> e : problems.entrySet()) {
            JLabel label = errors.get(e.getKey());
            Input input = inputs.get(e.getKey());
            if (label == null || input == null) {
                showBanner(e.getValue());
                continue;
            }
            label.setText(e.getValue());
            label.setIcon(Icons.of(MaterialDesignA.ALERT_CIRCLE_OUTLINE, 13, () -> Theme.p().danger));
            label.setVisible(true);
            input.focusTarget().putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            input.focusTarget().getAccessibleContext().setAccessibleDescription(e.getValue());
            if (firstInvalid == null) {
                firstInvalid = input.focusTarget();
            }
        }
        if (firstInvalid != null) {
            firstInvalid.requestFocusInWindow();
        }
        revalidate();
    }

    private void showBanner(String text) {
        banner.setText("<html><div style='width:380px'>" + text.replace("<", "&lt;") + "</div></html>");
        banner.setVisible(true);
        revalidate();
    }

    /** Para os testes de interface. */
    public JComponent inputFor(String key) {
        Input i = inputs.get(key);
        return i == null ? null : i.focusTarget();
    }

    public JButton saveButton() {
        return save;
    }

    public String errorFor(String key) {
        JLabel l = errors.get(key);
        return l != null && !l.getText().isBlank() ? l.getText() : null;
    }

    public boolean isReady() {
        return optionsReady;
    }

    // --------------------------------------------------------- controlos

    private Input create(FieldSpec f) {
        switch (f.type) {
            case TEXTAREA: return new AreaInput(f);
            case DATE: return new DateInput(f);
            case DOMAIN: return new ComboInput(f);
            case FK: return new ComboInput(f);
            case MULTI_FK: return new MultiInput(f);
            default: return new TextInput(f);
        }
    }

    private abstract static class Input {
        final FieldSpec field;

        Input(FieldSpec field) {
            this.field = field;
        }

        abstract JComponent component();

        JComponent focusTarget() {
            return component();
        }

        abstract Object value();

        abstract void set(JsonObject row);

        boolean needsOptions() {
            return false;
        }

        void setOptions(Object options) {
        }
    }

    private static final class TextInput extends Input {
        final JTextField text = new JTextField(24);

        TextInput(FieldSpec f) {
            super(f);
            text.getAccessibleContext().setAccessibleName(f.label);
            if (f.type == FieldSpec.Type.TIME) {
                text.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "HH:MM");
                text.setColumns(8);
            } else if (f.type == FieldSpec.Type.MONEY) {
                text.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0,00");
                text.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, UI.muted("MT  "));
            } else if (f.type == FieldSpec.Type.RATING) {
                text.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, f.required ? "0,0 a 5,0" : "Sem avaliação");
            }
        }

        @Override
        JComponent component() {
            return text;
        }

        @Override
        Object value() {
            String s = text.getText().trim();
            if (s.isEmpty()) {
                return null;
            }
            switch (field.type) {
                case INTEGER:
                    try {
                        return Integer.parseInt(s.replace(" ", "").replace(".", ""));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Indique um número inteiro.");
                    }
                case MONEY:
                case RATING:
                    try {
                        return new BigDecimal(s.replace(" ", "").replace(",", "."));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Indique um número (ex.: " + (field.type == FieldSpec.Type.MONEY ? "1500,00" : "4,5") + ").");
                    }
                default:
                    return s;
            }
        }

        @Override
        void set(JsonObject row) {
            String v = Format.str(row, field.key);
            if (v != null && (field.type == FieldSpec.Type.MONEY || field.type == FieldSpec.Type.RATING)) {
                BigDecimal d = new BigDecimal(v);
                v = field.type == FieldSpec.Type.MONEY ? d.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                        : d.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
                v = v.replace('.', ',');
            }
            text.setText(v == null ? "" : v);
        }
    }

    private static final class AreaInput extends Input {
        final JTextArea area = new JTextArea(3, 24);
        final JScrollPane scroll = new JScrollPane(area);

        AreaInput(FieldSpec f) {
            super(f);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.getAccessibleContext().setAccessibleName(f.label);
            // Tab muda de campo em vez de inserir um tabulador
            area.setFocusTraversalKeys(java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            area.setFocusTraversalKeys(java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        }

        @Override
        JComponent component() {
            return scroll;
        }

        @Override
        JComponent focusTarget() {
            return area;
        }

        @Override
        Object value() {
            String s = area.getText().trim();
            return s.isEmpty() ? null : s;
        }

        @Override
        void set(JsonObject row) {
            String v = Format.str(row, field.key);
            area.setText(v == null ? "" : v);
        }
    }

    private static final class DateInput extends Input {
        final JSpinner spinner = new JSpinner(new SpinnerDateModel());

        DateInput(FieldSpec f) {
            super(f);
            spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
            spinner.getAccessibleContext().setAccessibleName(f.label + " (dia/mês/ano)");
            setDate(LocalDate.now());
        }

        void setDate(LocalDate d) {
            spinner.setValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        @Override
        JComponent component() {
            return spinner;
        }

        @Override
        JComponent focusTarget() {
            return ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        }

        @Override
        Object value() {
            try {
                spinner.commitEdit();
            } catch (java.text.ParseException e) {
                throw new IllegalArgumentException("Data inválida (use dd/mm/aaaa).");
            }
            Date d = (Date) spinner.getValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        @Override
        void set(JsonObject row) {
            LocalDate d = Format.parseIsoDate(Format.str(row, field.key));
            if (d != null) {
                setDate(d);
            }
        }
    }

    /** Opção de uma lista: valor guardado + texto mostrado. */
    static final class Option {
        final String id;
        final String label;

        Option(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ComboInput extends Input {
        final JComboBox<Option> combo = new JComboBox<>();

        ComboInput(FieldSpec f) {
            super(f);
            combo.addItem(new Option(null, "A carregar…"));
            combo.getAccessibleContext().setAccessibleName(f.label);
        }

        @Override
        boolean needsOptions() {
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        void setOptions(Object options) {
            combo.removeAllItems();
            combo.addItem(new Option(null, field.required ? "Selecione…" : "Nenhum"));
            if (field.type == FieldSpec.Type.DOMAIN) {
                for (String v : (List<String>) options) {
                    combo.addItem(new Option(v, v));
                }
            } else {
                for (JsonObject row : (List<JsonObject>) options) {
                    combo.addItem(new Option(Format.str(row, field.fkIdKey), field.fkLabel.apply(row)));
                }
            }
        }

        @Override
        JComponent component() {
            return combo;
        }

        @Override
        Object value() {
            Option o = (Option) combo.getSelectedItem();
            return o == null ? null : o.id;
        }

        @Override
        void set(JsonObject row) {
            String v = Format.str(row, field.key);
            for (int i = 0; i < combo.getItemCount(); i++) {
                Option o = combo.getItemAt(i);
                if (v == null ? o.id == null : v.equals(o.id)) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
            if (v != null) {
                // Valor que já não consta das opções (ex.: estado CONCLUIDO): mostra-o sem o perder.
                Option keep = new Option(v, v);
                combo.addItem(keep);
                combo.setSelectedItem(keep);
            }
        }
    }

    /** Lista com caixas de seleção (ex.: alojamentos de uma nova reserva). */
    private static final class MultiInput extends Input {
        final DefaultListModel<Option> model = new DefaultListModel<>();
        final JList<Option> list = new JList<>(model);
        final JScrollPane scroll = new JScrollPane(list);

        MultiInput(FieldSpec f) {
            super(f);
            list.setSelectionModel(new DefaultListSelectionModel() {
                @Override
                public void setSelectionInterval(int index0, int index1) {
                    if (isSelectedIndex(index0)) {
                        super.removeSelectionInterval(index0, index1);
                    } else {
                        super.addSelectionInterval(index0, index1);
                    }
                }
            });
            list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.setVisibleRowCount(5);
            list.getAccessibleContext().setAccessibleName(f.label);
            JCheckBox box = new JCheckBox();
            ListCellRenderer<Option> renderer = (l, value, index, selected, focus) -> {
                box.setText(value.label);
                box.setSelected(selected);
                box.setOpaque(false);
                box.setFont(Theme.body());
                box.setForeground(Theme.p().text);
                box.setBorder(UI.pad(4, 8, 4, 8));
                return box;
            };
            list.setCellRenderer(renderer);
            list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "alternar");
            list.getActionMap().put("alternar", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int i = list.getLeadSelectionIndex();
                    if (i >= 0) {
                        list.getSelectionModel().setSelectionInterval(i, i);
                    }
                }
            });
            model.addElement(new Option(null, "A carregar…"));
        }

        @Override
        boolean needsOptions() {
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        void setOptions(Object options) {
            model.clear();
            for (JsonObject row : (List<JsonObject>) options) {
                model.addElement(new Option(Format.str(row, field.fkIdKey), field.fkLabel.apply(row)));
            }
        }

        @Override
        JComponent component() {
            return scroll;
        }

        @Override
        JComponent focusTarget() {
            return list;
        }

        @Override
        Object value() {
            List<String> ids = new ArrayList<>();
            for (Option o : list.getSelectedValuesList()) {
                if (o.id != null) {
                    ids.add(o.id);
                }
            }
            return ids;
        }

        @Override
        void set(JsonObject row) {
        }
    }

}
