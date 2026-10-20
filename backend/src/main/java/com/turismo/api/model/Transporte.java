package com.turismo.api.model;

import java.util.Objects;

/**
 * Meio de transporte.
 * Corresponde à tabela Transporte.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Transporte {
    private String idTransporte; // PK
    private String tipoTransporte;
    private Integer capTransporte;
    private String disponibilidade; // Sim ou Nao

    // Apenas leitura
    private Integer totalOperadores; // operadores associados

    public Transporte() {}

    public Transporte(String idTransporte, String tipoTransporte, Integer capTransporte, String disponibilidade) {
        this.idTransporte = idTransporte;
        this.tipoTransporte = tipoTransporte;
        this.capTransporte = capTransporte;
        this.disponibilidade = disponibilidade;
    }

    public String getIdTransporte() {
        return idTransporte;
    }

    public void setIdTransporte(String idTransporte) {
        this.idTransporte = idTransporte;
    }

    public String getTipoTransporte() {
        return tipoTransporte;
    }

    public void setTipoTransporte(String tipoTransporte) {
        this.tipoTransporte = tipoTransporte;
    }

    public Integer getCapTransporte() {
        return capTransporte;
    }

    public void setCapTransporte(Integer capTransporte) {
        this.capTransporte = capTransporte;
    }

    public String getDisponibilidade() {
        return disponibilidade;
    }

    public void setDisponibilidade(String disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    public Integer getTotalOperadores() {
        return totalOperadores;
    }

    public void setTotalOperadores(Integer totalOperadores) {
        this.totalOperadores = totalOperadores;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transporte other = (Transporte) o;
        return Objects.equals(idTransporte, other.idTransporte);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTransporte);
    }

    @Override
    public String toString() {
        return "Transporte{" +
                "idTransporte='" + idTransporte + '\'' +
                ", tipoTransporte='" + tipoTransporte + '\'' +
                ", capTransporte=" + capTransporte +
                ", disponibilidade='" + disponibilidade + '\'' +
                '}';
    }
}
