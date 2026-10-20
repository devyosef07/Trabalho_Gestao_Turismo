package com.turismo.frontend.modules;

/** Ecrã da janela principal. Os atalhos de teclado globais são encaminhados para o ecrã visível. */
public interface Page {

    /** Chamado sempre que o ecrã passa a ser o visível. */
    void onShow();

    default void onNew() {
    }

    default void onSearch() {
    }

    default void onRefresh() {
    }

    /** Põe o foco no primeiro controlo útil do ecrã (ex.: ao abrir a janela). */
    default void focusDefault() {
    }
}
