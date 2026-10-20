package com.turismo.frontend.modules;

import com.google.gson.JsonObject;
import com.turismo.frontend.api.ApiClient;
import com.turismo.frontend.api.ApiException;
import com.turismo.frontend.ui.components.UI;
import org.kordamp.ikonli.Ikon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Descrição declarativa de um módulo (uma tabela da BD): endpoint, chave,
 * colunas, campos do formulário, ações e relatórios. A {@link CrudPage}
 * genérica constrói o ecrã a partir disto.
 */
public final class ModuleSpec {

    /** Texto de aviso antes de eliminar; pode consultar a API (corre fora da EDT). */
    @FunctionalInterface
    public interface DeleteWarning {
        String build(ApiClient api, JsonObject row) throws ApiException;
    }

    final String id;
    final String title;
    final String singular;
    final boolean feminine;
    final Ikon icon;
    final String endpoint;
    String description = "";
    final List<String> keyFields = new ArrayList<>();
    final List<ColumnSpec> columns = new ArrayList<>();
    final List<FieldSpec> fields = new ArrayList<>();
    final List<ActionSpec> actions = new ArrayList<>();
    final List<ReportSpec> reports = new ArrayList<>();
    Function<JsonObject, String> titleOf;
    Predicate<JsonObject> editable = r -> true;
    String notEditableReason = "";
    boolean canCreate = true;
    boolean canDelete = true;
    String deleteLabel = "Eliminar";
    String editLabel = "Editar";
    DeleteWarning deleteWarning;
    Function<Map<String, Object>, Map<String, String>> crossValidation = v -> Map.of();
    String emptyMessage;

    public ModuleSpec(String id, String title, String singular, boolean feminine, Ikon icon, String endpoint) {
        this.id = id;
        this.title = title;
        this.singular = singular;
        this.feminine = feminine;
        this.icon = icon;
        this.endpoint = endpoint;
        this.titleOf = r -> keyText(r);
    }

    public ModuleSpec description(String text) {
        description = text;
        return this;
    }

    public ModuleSpec key(String... keys) {
        keyFields.addAll(List.of(keys));
        return this;
    }

    public ModuleSpec column(String key, String header, ColumnSpec.Type type, int width) {
        columns.add(new ColumnSpec(key, header, type, width));
        return this;
    }

    public ModuleSpec column(String key, String header) {
        return column(key, header, ColumnSpec.Type.TEXT, 0);
    }

    public ModuleSpec status(String key, String header, Function<String, UI.Tone> tone) {
        ColumnSpec c = new ColumnSpec(key, header, ColumnSpec.Type.STATUS, 130);
        c.tone = tone;
        columns.add(c);
        return this;
    }

    public ModuleSpec derived(String key, String header, Function<JsonObject, String> value) {
        ColumnSpec c = new ColumnSpec(key, header, ColumnSpec.Type.TEXT, 0);
        c.derived = value;
        columns.add(c);
        return this;
    }

    /** Campo mostrado só no painel de detalhe. */
    public ModuleSpec detail(String key, String header, ColumnSpec.Type type) {
        ColumnSpec c = new ColumnSpec(key, header, type, 0);
        c.detailOnly = true;
        columns.add(c);
        return this;
    }

    public ModuleSpec field(FieldSpec f) {
        fields.add(f);
        return this;
    }

    public ModuleSpec action(ActionSpec a) {
        actions.add(a);
        return this;
    }

    public ModuleSpec report(String view, String title, String description) {
        reports.add(new ReportSpec(view, title, description));
        return this;
    }

    public ModuleSpec titleOf(Function<JsonObject, String> f) {
        titleOf = f;
        return this;
    }

    public ModuleSpec editableWhen(Predicate<JsonObject> p, String reason) {
        editable = p;
        notEditableReason = reason;
        return this;
    }

    public ModuleSpec deleteWarning(DeleteWarning w) {
        deleteWarning = w;
        return this;
    }

    public ModuleSpec deleteLabel(String label) {
        deleteLabel = label;
        return this;
    }

    public ModuleSpec editLabel(String label) {
        editLabel = label;
        return this;
    }

    public ModuleSpec crossValidation(Function<Map<String, Object>, Map<String, String>> f) {
        crossValidation = f;
        return this;
    }

    public ModuleSpec emptyMessage(String text) {
        emptyMessage = text;
        return this;
    }

    // ------------------------------------------------------------------

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public Ikon icon() {
        return icon;
    }

    public String endpoint() {
        return endpoint;
    }

    public List<ColumnSpec> columns() {
        return columns;
    }

    public List<FieldSpec> fields() {
        return fields;
    }

    public List<ReportSpec> reports() {
        return reports;
    }

    public List<String> keyFields() {
        return keyFields;
    }

    /** "Novo destino" / "Nova atração". */
    public String newLabel() {
        return (feminine ? "Nova " : "Novo ") + singular;
    }

    public boolean hasEditFields() {
        return fields.stream().anyMatch(f -> f.onEdit);
    }

    /** Caminho do registo: /destinos/DT-001 ou /operadorTuristas/OP-001/TR-002. */
    public String pathOf(JsonObject row) {
        StringBuilder sb = new StringBuilder(endpoint);
        for (String k : keyFields) {
            sb.append('/').append(ApiClient.segment(Format.str(row, k)));
        }
        return sb.toString();
    }

    public String keyText(JsonObject row) {
        StringBuilder sb = new StringBuilder();
        for (String k : keyFields) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            String v = Format.str(row, k);
            LocalDateOrText.append(sb, v);
        }
        return sb.toString();
    }

    public boolean sameKey(JsonObject a, JsonObject b) {
        for (String k : keyFields) {
            String va = Format.str(a, k);
            String vb = Format.str(b, k);
            if (va == null ? vb != null : !va.equals(vb)) {
                return false;
            }
        }
        return true;
    }

    /** Datas ISO nas chaves compostas aparecem no formato dd/MM/aaaa. */
    private static final class LocalDateOrText {
        static void append(StringBuilder sb, String v) {
            java.time.LocalDate d = v != null && v.matches("\\d{4}-\\d{2}-\\d{2}") ? Format.parseIsoDate(v) : null;
            sb.append(d != null ? Format.date(d) : v);
        }
    }
}
