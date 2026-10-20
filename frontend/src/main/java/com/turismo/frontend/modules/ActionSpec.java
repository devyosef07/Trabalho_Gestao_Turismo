package com.turismo.frontend.modules;

import com.google.gson.JsonObject;
import org.kordamp.ikonli.Ikon;

import java.util.function.Predicate;

/**
 * Ação específica de um módulo (ex.: concluir pagamento, cancelar reserva),
 * mostrada no painel de detalhe do registo selecionado.
 */
public final class ActionSpec {

    @FunctionalInterface
    public interface Handler {
        void run(CrudPage page, JsonObject row);
    }

    final String label;
    final Ikon icon;
    final String tooltip;
    final Predicate<JsonObject> enabled;
    final Handler handler;

    public ActionSpec(String label, Ikon icon, String tooltip, Predicate<JsonObject> enabled, Handler handler) {
        this.label = label;
        this.icon = icon;
        this.tooltip = tooltip;
        this.enabled = enabled;
        this.handler = handler;
    }
}
