package com.turismo.frontend.modules;

/** View da base de dados mostrada no separador "Relatórios" de um módulo. */
public final class ReportSpec {

    final String view;
    final String title;
    final String description;

    public ReportSpec(String view, String title, String description) {
        this.view = view;
        this.title = title;
        this.description = description;
    }

    public String view() {
        return view;
    }

    public String title() {
        return title;
    }
}
