package com.turismo.api.model;

import java.util.Objects;

/**
 * Alojamento fornecido por um operador (chave composta).
 * Corresponde à tabela Operador_Alojamento.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class OperadorAlojamento {
    private String idOperador; // PK, FK
    private String idAlojamento; // PK, FK

    // Apenas leitura
    private String nomeOperador; // nome do operador
    private String nomeAlojamento; // nome do alojamento

    public OperadorAlojamento() {}

    public OperadorAlojamento(String idOperador, String idAlojamento) {
        this.idOperador = idOperador;
        this.idAlojamento = idAlojamento;
    }

    public String getIdOperador() {
        return idOperador;
    }

    public void setIdOperador(String idOperador) {
        this.idOperador = idOperador;
    }

    public String getIdAlojamento() {
        return idAlojamento;
    }

    public void setIdAlojamento(String idAlojamento) {
        this.idAlojamento = idAlojamento;
    }

    public String getNomeOperador() {
        return nomeOperador;
    }

    public void setNomeOperador(String nomeOperador) {
        this.nomeOperador = nomeOperador;
    }

    public String getNomeAlojamento() {
        return nomeAlojamento;
    }

    public void setNomeAlojamento(String nomeAlojamento) {
        this.nomeAlojamento = nomeAlojamento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperadorAlojamento other = (OperadorAlojamento) o;
        return Objects.equals(idOperador, other.idOperador)
                && Objects.equals(idAlojamento, other.idAlojamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idOperador, idAlojamento);
    }

    @Override
    public String toString() {
        return "OperadorAlojamento{" +
                "idOperador='" + idOperador + '\'' +
                ", idAlojamento='" + idAlojamento + '\'' +
                '}';
    }
}
