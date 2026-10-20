package com.turismo.api.model;

import java.util.Objects;

/**
 * Transporte gerido por um operador (chave composta).
 * Corresponde à tabela Operador_Transporte.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class OperadorTransporte {
    private String idOperador; // PK, FK
    private String idTransporte; // PK, FK

    // Apenas leitura
    private String nomeOperador; // nome do operador
    private String tipoTransporte; // tipo do transporte

    public OperadorTransporte() {}

    public OperadorTransporte(String idOperador, String idTransporte) {
        this.idOperador = idOperador;
        this.idTransporte = idTransporte;
    }

    public String getIdOperador() {
        return idOperador;
    }

    public void setIdOperador(String idOperador) {
        this.idOperador = idOperador;
    }

    public String getIdTransporte() {
        return idTransporte;
    }

    public void setIdTransporte(String idTransporte) {
        this.idTransporte = idTransporte;
    }

    public String getNomeOperador() {
        return nomeOperador;
    }

    public void setNomeOperador(String nomeOperador) {
        this.nomeOperador = nomeOperador;
    }

    public String getTipoTransporte() {
        return tipoTransporte;
    }

    public void setTipoTransporte(String tipoTransporte) {
        this.tipoTransporte = tipoTransporte;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperadorTransporte other = (OperadorTransporte) o;
        return Objects.equals(idOperador, other.idOperador)
                && Objects.equals(idTransporte, other.idTransporte);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idOperador, idTransporte);
    }

    @Override
    public String toString() {
        return "OperadorTransporte{" +
                "idOperador='" + idOperador + '\'' +
                ", idTransporte='" + idTransporte + '\'' +
                '}';
    }
}
