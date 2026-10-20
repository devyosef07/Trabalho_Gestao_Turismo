package com.turismo.api.model;

import java.util.Objects;

/**
 * Preferência de viagem de um turista (chave composta).
 * Corresponde à tabela Turista_Preferencia.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class TuristaPreferencia {
    private String idTurista; // PK, FK
    private String preferencia; // PK

    // Apenas leitura
    private String nomeTurista; // nome do turista

    public TuristaPreferencia() {}

    public TuristaPreferencia(String idTurista, String preferencia) {
        this.idTurista = idTurista;
        this.preferencia = preferencia;
    }

    public String getIdTurista() {
        return idTurista;
    }

    public void setIdTurista(String idTurista) {
        this.idTurista = idTurista;
    }

    public String getPreferencia() {
        return preferencia;
    }

    public void setPreferencia(String preferencia) {
        this.preferencia = preferencia;
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
        TuristaPreferencia other = (TuristaPreferencia) o;
        return Objects.equals(idTurista, other.idTurista)
                && Objects.equals(preferencia, other.preferencia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTurista, preferencia);
    }

    @Override
    public String toString() {
        return "TuristaPreferencia{" +
                "idTurista='" + idTurista + '\'' +
                ", preferencia='" + preferencia + '\'' +
                '}';
    }
}
