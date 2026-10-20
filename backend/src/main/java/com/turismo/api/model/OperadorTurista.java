package com.turismo.api.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Operador que acompanha um turista (chave composta).
 * Corresponde à tabela Operador_Turista.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class OperadorTurista {
    private String idOperador; // PK, FK
    private String idTurista; // PK, FK
    private BigDecimal avaliacaoOperador; // 0.0 a 5.0, opcional

    // Apenas leitura
    private String nomeOperador; // nome do operador
    private String nomeTurista; // nome do turista

    public OperadorTurista() {}

    public OperadorTurista(String idOperador, String idTurista, BigDecimal avaliacaoOperador) {
        this.idOperador = idOperador;
        this.idTurista = idTurista;
        this.avaliacaoOperador = avaliacaoOperador;
    }

    public String getIdOperador() {
        return idOperador;
    }

    public void setIdOperador(String idOperador) {
        this.idOperador = idOperador;
    }

    public String getIdTurista() {
        return idTurista;
    }

    public void setIdTurista(String idTurista) {
        this.idTurista = idTurista;
    }

    public BigDecimal getAvaliacaoOperador() {
        return avaliacaoOperador;
    }

    public void setAvaliacaoOperador(BigDecimal avaliacaoOperador) {
        this.avaliacaoOperador = avaliacaoOperador;
    }

    public String getNomeOperador() {
        return nomeOperador;
    }

    public void setNomeOperador(String nomeOperador) {
        this.nomeOperador = nomeOperador;
    }

    public String getNomeTurista() {
        return nomeTurista;
    }

    public void setNomeTurista(String nomeTurista) {
        this.nomeTurista = nomeTurista;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperadorTurista other = (OperadorTurista) o;
        return Objects.equals(idOperador, other.idOperador)
                && Objects.equals(idTurista, other.idTurista);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idOperador, idTurista);
    }

    @Override
    public String toString() {
        return "OperadorTurista{" +
                "idOperador='" + idOperador + '\'' +
                ", idTurista='" + idTurista + '\'' +
                ", avaliacaoOperador=" + avaliacaoOperador +
                '}';
    }
}
