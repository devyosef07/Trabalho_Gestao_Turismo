package com.turismo.frontend.modules;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.ui.async.Task;
import com.turismo.frontend.ui.components.Card;
import com.turismo.frontend.ui.components.SearchField;
import com.turismo.frontend.ui.components.Spinner;
import com.turismo.frontend.ui.components.StateView;
import com.turismo.frontend.ui.components.Surface;
import com.turismo.frontend.ui.components.Toast;
import com.turismo.frontend.ui.components.UI;
import com.turismo.frontend.ui.theme.Theme;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Ecrã genérico de um módulo: tabela pesquisável e ordenável, painel com o
 * detalhe do registo selecionado e as suas ações, formulários de criação e
 * edição, e (quando existem) os relatórios das views associadas.
 *
 * Todos os pedidos à API correm em {@link Task}; a EDT nunca espera pela rede.
 */
public class CrudPage extends Surface implements Page {

    private static final String LOADING = "loading";
    private static final String ERROR = "error";
    private static final String EMPTY = "empty";
    private static final String TABLE = "table";
    private static final String NO_MATCH = "nomatch";

    private final ModuleSpec spec;
    private final Lookups lookups;
    private final RowsTableModel model;
    private final JTable table;
    private final TableRowSorter<RowsTableModel> sorter;
    private final SearchField search;
    private final JLabel count = UI.muted(" ");
    private final Spinner refreshing = new Spinner(14);
    private final JButton refresh = UI.iconButton(MaterialDesignR.REFRESH, "Atualizar (F5)");
    private final JButton create;
    private final CardLayout states = new CardLayout();
    private final JPanel stateHost = new JPanel(states);
    private final StateView loadingView = new StateView();
    private final StateView errorView = new StateView();
    private final StateView emptyView = new StateView();
    private final StateView noMatchView = new StateView();
    private final DetailPanel detail = new DetailPanel();
    private final ReportPanel reports;
    private final JTabbedPane tabs;

    private boolean loading;
    private boolean loadedOnce;

    public CrudPage(ModuleSpec spec, Lookups lookups) {
        super(new BorderLayout());
        this.spec = spec;
        this.lookups = lookups;
        setBorder(UI.pad(UI.S5, UI.S6, UI.S5, UI.S6));
        setName("page-" + spec.id);

        // ---------------------------------------------------- cabeçalho
        JPanel header = new JPanel(new BorderLayout(UI.S4, 0));
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UI.title(spec.title));
        titles.add(Box.createVerticalStrut(2));
        titles.add(UI.secondary(spec.description));
        header.add(titles, BorderLayout.CENTER);

        create = UI.primary(spec.newLabel(), MaterialDesignP.PLUS);
        create.setName("novo");
        create.setToolTipText(spec.newLabel() + " (" + shortcut("N") + ")");
        create.addActionListener(e -> onNew());
        create.setVisible(spec.canCreate);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerActions.add(create);
        header.add(headerActions, BorderLayout.EAST);
        header.setBorder(UI.pad(0, 0, UI.S4, 0));
        add(header, BorderLayout.NORTH);

        // ---------------------------------------------------- tabela
        model = new RowsTableModel(spec.columns);
        table = new JTable(model);
        table.setName("tabela");
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.setDefaultRenderer(String.class, new CellRenderer());
        table.setDefaultRenderer(Integer.class, new CellRenderer());
        table.setDefaultRenderer(java.math.BigDecimal.class, new CellRenderer());
        table.setDefaultRenderer(java.time.LocalDate.class, new CellRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.label());
        table.getAccessibleContext().setAccessibleName("Registos de " + spec.title);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        for (int i = 0; i < model.getColumnCount(); i++) {
            ColumnSpec c = model.column(i);
            TableColumn col = table.getColumnModel().getColumn(i);
            if (c.width > 0) {
                // Colunas curtas (códigos, datas, números) não encolhem: o texto livre é que cede espaço.
                col.setPreferredWidth(c.width);
                col.setMinWidth(Math.min(c.width, 110));
            } else {
                col.setPreferredWidth(200);
                col.setMinWidth(120);
            }
            col.setHeaderRenderer(new HeaderRenderer(table, numeric(c)));
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                detail.show(selectedRow());
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && selectedRow() != null) {
                    onEdit(selectedRow());
                }
            }
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editar");
        table.getActionMap().put("editar", action(() -> {
            if (selectedRow() != null) {
                onEdit(selectedRow());
            }
        }));
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminar");
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "eliminar");
        table.getActionMap().put("eliminar", action(() -> {
            if (selectedRow() != null && spec.canDelete) {
                onDelete(selectedRow());
            }
        }));

        JScrollPane tableScroll = new com.turismo.frontend.ui.components.Scroll(table);
        Card tableCard = new Card(new BorderLayout());
        tableCard.setBorder(UI.pad(1));
        tableCard.add(tableScroll, BorderLayout.CENTER);

        search = new SearchField("Pesquisar em " + spec.title.toLowerCase(Format.PT), this::applyFilter);
        search.setName("pesquisa");
        refresh.setName("atualizar");
        refresh.addActionListener(e -> reload());
        refreshing.setVisible(false);
        JPanel toolbar = new JPanel(new BorderLayout(UI.S3, 0));
        toolbar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, UI.S2, 0));
        left.setOpaque(false);
        left.add(search);
        left.add(refresh);
        left.add(refreshing);
        toolbar.add(left, BorderLayout.WEST);
        count.setName("contagem");
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(count);
        toolbar.add(right, BorderLayout.EAST);
        toolbar.setBorder(UI.pad(0, 0, UI.S3, 0));

        stateHost.setOpaque(false);
        stateHost.add(loadingView.loading("A carregar " + spec.title.toLowerCase(Format.PT) + "…"), LOADING);
        stateHost.add(errorView, ERROR);
        stateHost.add(emptyView, EMPTY);
        stateHost.add(noMatchView, NO_MATCH);
        stateHost.add(tableCard, TABLE);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stateHost, detail);
        split.setResizeWeight(1);
        split.setBorder(null);
        split.setOpaque(false);
        split.setContinuousLayout(true);
        split.setDividerSize(UI.S4);
        detail.setMinimumSize(new Dimension(280, 100));
        detail.setPreferredSize(new Dimension(320, 100));
        stateHost.setMinimumSize(new Dimension(360, 100));

        JPanel records = new JPanel(new BorderLayout());
        records.setOpaque(false);
        records.add(toolbar, BorderLayout.NORTH);
        records.add(split, BorderLayout.CENTER);

        if (spec.reports.isEmpty()) {
            reports = null;
            tabs = null;
            add(records, BorderLayout.CENTER);
        } else {
            reports = new ReportPanel(lookups.api(), spec.reports);
            tabs = new JTabbedPane();
            tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_AREA_INSETS, new Insets(0, 0, UI.S3, 0));
            records.setBorder(UI.pad(UI.S3, 0, 0, 0));
            tabs.addTab("Registos", records);
            tabs.addTab("Relatórios", reports);
            tabs.setToolTipTextAt(1, "Dados das views da base de dados relacionadas com " + spec.title.toLowerCase(Format.PT));
            tabs.addChangeListener(e -> {
                if (tabs.getSelectedComponent() == reports) {
                    reports.onShow();
                }
            });
            add(tabs, BorderLayout.CENTER);
        }
        states.show(stateHost, LOADING);
    }

    // ------------------------------------------------------------ Page

    @Override
    public void onShow() {
        reload();
        if (tabs != null && tabs.getSelectedComponent() == reports) {
            reports.onShow();
        }
    }

    @Override
    public void onNew() {
        if (!spec.canCreate) {
            return;
        }
        new FormDialog(SwingUtilities.getWindowAncestor(this), spec, lookups, null, result -> {
            toastResult(result, spec.newLabel() + " criado.");
            reloadSelecting(data(result));
        }).setVisible(true);
    }

    @Override
    public void onSearch() {
        if (tabs != null) {
            tabs.setSelectedIndex(0);
        }
        search.requestFocusInWindow();
        search.selectAll();
    }

    @Override
    public void onRefresh() {
        reload();
        if (reports != null && tabs.getSelectedComponent() == reports) {
            reports.reload();
        }
    }

    @Override
    public void focusDefault() {
        search.requestFocusInWindow();
    }

    public ModuleSpec spec() {
        return spec;
    }

    public JTable table() {
        return table;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean hasLoaded() {
        return loadedOnce;
    }

    /** Estado visível da área de registos: loading, error, empty, nomatch ou table. */
    public String visibleState() {
        for (Component c : stateHost.getComponents()) {
            if (c.isVisible()) {
                if (c == loadingView) return LOADING;
                if (c == errorView) return ERROR;
                if (c == emptyView) return EMPTY;
                if (c == noMatchView) return NO_MATCH;
                return TABLE;
            }
        }
        return LOADING;
    }

    // ---------------------------------------------------------- dados

    public void reload() {
        reloadSelecting(selectedRow());
    }

    private void reloadSelecting(JsonObject keep) {
        if (loading) {
            return;
        }
        loading = true;
        refresh.setEnabled(false);
        refreshing.setVisible(loadedOnce);
        if (!loadedOnce) {
            states.show(stateHost, LOADING);
        }
        Task.run(() -> lookups.rows(spec.endpoint), rows -> {
            loadedOnce = true;
            model.setRows(rows);
            count.setText(Format.plural(rows.size(), "registo", "registos"));
            updateState();
            if (keep != null) {
                select(keep);
            }
            detail.show(selectedRow());
        }, error -> {
            model.setRows(List.of());
            count.setText(" ");
            detail.show(null);
            showError(error);
        }, () -> {
            loading = false;
            refresh.setEnabled(true);
            refreshing.setVisible(false);
        });
    }

    private void showError(Exception error) {
        String message = error.getMessage();
        if (error instanceof ApiException && ((ApiException) error).isUnreachable()) {
            errorView.unreachable(message, this::reload);
        } else {
            errorView.error("Não foi possível carregar " + spec.title.toLowerCase(Format.PT), message, this::reload);
        }
        states.show(stateHost, ERROR);
    }

    private void updateState() {
        if (model.getRowCount() == 0) {
            JButton action = null;
            if (spec.canCreate) {
                action = UI.primary(spec.newLabel(), MaterialDesignP.PLUS);
                action.addActionListener(e -> onNew());
            }
            emptyView.empty(spec.icon, "Ainda não há " + spec.title.toLowerCase(Format.PT),
                    spec.emptyMessage != null ? spec.emptyMessage
                            : "A tabela está vazia na base de dados. Os registos que criar aparecem aqui.", action);
            states.show(stateHost, EMPTY);
        } else if (table.getRowCount() == 0) {
            JButton clear = UI.secondary("Limpar pesquisa", MaterialDesignC.CLOSE);
            clear.addActionListener(e -> search.setText(""));
            noMatchView.empty(MaterialDesignM.MAGNIFY, "Sem resultados",
                    "Nenhum registo corresponde a “" + search.getText().trim() + "”.", clear);
            states.show(stateHost, NO_MATCH);
        } else {
            states.show(stateHost, TABLE);
        }
    }

    private void applyFilter(String text) {
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            String needle = Format.fold(text);
            sorter.setRowFilter(new RowFilter<RowsTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends RowsTableModel, ? extends Integer> entry) {
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        ColumnSpec c = model.column(i);
                        if (Format.fold(RowsTableModel.display(c, entry.getValue(i))).contains(needle)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        if (loadedOnce) {
            int shown = table.getRowCount();
            int total = model.getRowCount();
            count.setText(text.isEmpty() ? Format.plural(total, "registo", "registos")
                    : shown + " de " + Format.plural(total, "registo", "registos"));
            updateState();
        }
    }

    public JsonObject selectedRow() {
        int view = table.getSelectedRow();
        return view < 0 ? null : model.rowAt(table.convertRowIndexToModel(view));
    }

    private void select(JsonObject keep) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (spec.sameKey(model.rowAt(i), keep)) {
                int view = table.convertRowIndexToView(i);
                if (view >= 0) {
                    table.setRowSelectionInterval(view, view);
                    table.scrollRectToVisible(table.getCellRect(view, 0, true));
                }
                return;
            }
        }
    }

    // -------------------------------------------------------- operações

    public void onEdit(JsonObject row) {
        if (!spec.hasEditFields()) {
            return;
        }
        if (!spec.editable.test(row)) {
            Toast.info(this, spec.notEditableReason);
            return;
        }
        new FormDialog(SwingUtilities.getWindowAncestor(this), spec, lookups, row, result -> {
            toastResult(result, "Alterações guardadas.");
            JsonObject updated = data(result);
            reloadSelecting(updated != null ? updated : row);
        }).setVisible(true);
    }

    public void onDelete(JsonObject row) {
        if (spec.deleteWarning == null) {
            confirmDelete(row, "Esta ação não pode ser desfeita.");
            return;
        }
        detail.setBusy(true);
        Task.run(() -> spec.deleteWarning.build(lookups.api(), row),
                warning -> confirmDelete(row, warning),
                error -> Toast.error(this, error.getMessage()),
                () -> detail.setBusy(false));
    }

    private void confirmDelete(JsonObject row, String warning) {
        String name = spec.titleOf.apply(row);
        Object[] options = {spec.deleteLabel, "Cancelar"};
        int choice = JOptionPane.showOptionDialog(this,
                "<html><b>" + spec.deleteLabel + " " + escape(name) + "?</b><br><br>"
                        + "<div style='width:340px'>" + escape(warning) + "</div></html>",
                spec.deleteLabel + " " + spec.singular, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (choice != 0) {
            return;
        }
        request(() -> lookups.api().delete(spec.pathOf(row)), spec.deleteLabel + " concluído.", null);
    }

    /**
     * Executa um pedido de escrita (ação do módulo): bloqueia os botões do
     * detalhe enquanto corre, mostra o resultado e recarrega a lista.
     */
    public void request(Task.Work<JsonObject> work, String successFallback, JsonObject keepSelection) {
        detail.setBusy(true);
        Task.run(work, result -> {
            toastResult(result, successFallback);
            JsonObject updated = data(result);
            reloadSelecting(updated != null ? updated : keepSelection);
        }, error -> Toast.error(this, error.getMessage()), () -> detail.setBusy(false));
    }

    public Lookups lookups() {
        return lookups;
    }

    private void toastResult(JsonObject result, String fallback) {
        String message = result != null && result.has("message") && !result.get("message").isJsonNull()
                ? result.get("message").getAsString() : fallback;
        Toast.success(this, message);
    }

    private static JsonObject data(JsonObject result) {
        if (result == null || !result.has("data")) {
            return null;
        }
        JsonElement d = result.get("data");
        return d.isJsonObject() ? d.getAsJsonObject() : null;
    }

    static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    private static AbstractAction action(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }

    static boolean numeric(ColumnSpec c) {
        return c.type == ColumnSpec.Type.INTEGER || c.type == ColumnSpec.Type.MONEY
                || c.type == ColumnSpec.Type.RATING || c.type == ColumnSpec.Type.DECIMAL;
    }

    /** Cabeçalho alinhado como os valores da coluna (números à direita). */
    static final class HeaderRenderer implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer base;
        private final boolean right;

        HeaderRenderer(JTable table, boolean right) {
            this.base = table.getTableHeader().getDefaultRenderer();
            this.right = right;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setHorizontalAlignment(right ? JLabel.RIGHT : JLabel.LEFT);
                l.setFont(Theme.label());
                l.setForeground(Theme.p().textSecondary);
                l.setBorder(UI.pad(0, UI.S3, 0, UI.S3));
            }
            return c;
        }
    }

    static String shortcut(String key) {
        boolean mac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        return (mac ? "⌘" : "Ctrl+") + key;
    }

    // ------------------------------------------------------- detalhe

    /** Painel lateral com todos os campos do registo selecionado e as suas ações. */
    private final class DetailPanel extends Card {
        private final JPanel body = new com.turismo.frontend.ui.components.Scroll.WidthTrackingPanel(new GridBagLayout());
        private final JPanel actions = new JPanel();
        private final List<JButton> buttons = new ArrayList<>();
        private final Spinner busy = new Spinner(16);
        private JsonObject row;

        DetailPanel() {
            super(new BorderLayout());
            setName("detalhe");
            setBorder(UI.pad(UI.S5, UI.S5, UI.S5, UI.S5));
            body.setOpaque(false);
            add(com.turismo.frontend.ui.components.Scroll.vertical(body), BorderLayout.CENTER);
            actions.setOpaque(false);
            actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
            actions.setBorder(UI.pad(UI.S4, 0, 0, 0));
            add(actions, BorderLayout.SOUTH);
            show(null);
        }

        void show(JsonObject row) {
            this.row = row;
            body.removeAll();
            actions.removeAll();
            buttons.clear();
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            if (row == null) {
                c.gridy = 0;
                body.add(UI.section("Detalhe"), c);
                c.gridy = 1;
                c.insets = new Insets(UI.S2, 0, 0, 0);
                body.add(new com.turismo.frontend.ui.components.WrapText("Selecione um registo na tabela para ver "
                        + "todos os campos e as ações disponíveis.", Theme::body, () -> Theme.p().textSecondary), c);
            } else {
                int y = 0;
                c.gridy = y++;
                com.turismo.frontend.ui.components.WrapText title = new com.turismo.frontend.ui.components.WrapText(
                        spec.titleOf.apply(row), Theme::section, () -> Theme.p().text);
                title.setName("detalhe-titulo");
                body.add(title, c);
                c.gridy = y++;
                c.insets = new Insets(2, 0, UI.S4, 0);
                body.add(UI.muted(spec.keyText(row)), c);
                for (ColumnSpec col : spec.columns) {
                    if (spec.keyFields.contains(col.key) && col.derived == null) {
                        continue;
                    }
                    c.gridy = y++;
                    c.insets = new Insets(UI.S2, 0, 0, 0);
                    body.add(UI.fieldLabel(col.header), c);
                    c.gridy = y++;
                    c.insets = new Insets(1, 0, 0, 0);
                    Object value = RowsTableModel.value(col, row);
                    String text = RowsTableModel.display(col, value);
                    com.turismo.frontend.ui.components.WrapText v = new com.turismo.frontend.ui.components.WrapText(
                            text, Theme::body, () -> value == null ? Theme.p().textMuted : Theme.p().text);
                    body.add(v, c);
                }
                buildActions(row);
            }
            c.gridy = 999;
            c.weighty = 1;
            body.add(Box.createVerticalGlue(), c);
            body.revalidate();
            body.repaint();
            actions.revalidate();
            actions.repaint();
        }

        private void buildActions(JsonObject row) {
            if (spec.hasEditFields()) {
                JButton edit = UI.secondary(spec.editLabel, MaterialDesignP.PENCIL_OUTLINE);
                edit.setName("editar");
                edit.addActionListener(e -> onEdit(row));
                if (!spec.editable.test(row)) {
                    edit.setEnabled(false);
                    edit.setToolTipText(spec.notEditableReason);
                } else {
                    edit.setToolTipText(spec.editLabel + " (Enter ou duplo clique)");
                }
                addButton(edit, true);
            }
            for (ActionSpec a : spec.actions) {
                JButton b = UI.secondary(a.label, a.icon);
                b.setName("acao-" + a.label);
                b.setToolTipText(a.tooltip);
                b.addActionListener(e -> a.handler.run(CrudPage.this, row));
                addButton(b, a.enabled.test(row));
            }
            if (spec.canDelete) {
                JButton delete = UI.danger(spec.deleteLabel, MaterialDesignT.TRASH_CAN_OUTLINE);
                delete.setName("eliminar");
                delete.setToolTipText(spec.deleteLabel + " (Delete)");
                delete.addActionListener(e -> onDelete(row));
                actions.add(Box.createVerticalStrut(UI.S3));
                addButton(delete, true);
            }
            busy.setVisible(false);
            busy.setAlignmentX(Component.LEFT_ALIGNMENT);
            actions.add(busy);
        }

        private void addButton(JButton b, boolean enabled) {
            b.setEnabled(enabled);
            b.putClientProperty("turismo.enabled", enabled);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
            b.setHorizontalAlignment(JButton.LEFT);
            if (!buttons.isEmpty()) {
                actions.add(Box.createVerticalStrut(UI.S2));
            }
            buttons.add(b);
            actions.add(b);
        }

        void setBusy(boolean value) {
            busy.setVisible(value);
            for (JButton b : buttons) {
                b.setEnabled(!value && Boolean.TRUE.equals(b.getClientProperty("turismo.enabled")));
            }
        }
    }
}
