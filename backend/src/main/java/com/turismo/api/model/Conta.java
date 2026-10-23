package com.turismo.api.model;

import java.time.LocalDate;

/**
 * Dados públicos de uma conta de utilizador (tabela Conta_Utilizador, lida
 * através da view v_sessao_ativa). Nunca inclui o hash da palavra-passe.
 */
public class Conta {
    private long idConta;
    private String nomeUtilizador;
    private String nomeCompleto;
    private LocalDate dataNascimento;
    private String dataCriacao; // ISO-8601 (data e hora)

    public Conta() {}

    public long getIdConta() {
        return idConta;
    }

    public void setIdConta(long idConta) {
        this.idConta = idConta;
    }

    public String getNomeUtilizador() {
        return nomeUtilizador;
    }

    public void setNomeUtilizador(String nomeUtilizador) {
        this.nomeUtilizador = nomeUtilizador;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(String dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
