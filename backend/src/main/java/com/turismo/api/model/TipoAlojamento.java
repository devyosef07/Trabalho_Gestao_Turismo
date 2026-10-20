package com.turismo.api.model;

import java.util.Objects;

/**
 * Tipo de alojamento e respetiva capacidade.
 * Corresponde à tabela Tipo_Alojamento.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class TipoAlojamento {
    private String tipoAlojamento; // PK
    private Integer capAlojamento;

    // Apenas leitura
    private Integer totalAlojamentos; // alojamentos deste tipo

    public TipoAlojamento() {}

    public TipoAlojamento(String tipoAlojamento, Integer capAlojamento) {
        this.tipoAlojamento = tipoAlojamento;
        this.capAlojamento = capAlojamento;
    }

    public String getTipoAlojamento() {
        return tipoAlojamento;
    }

    public void setTipoAlojamento(String tipoAlojamento) {
        this.tipoAlojamento = tipoAlojamento;
    }

    public Integer getCapAlojamento() {
        return capAlojamento;
    }

    public void setCapAlojamento(Integer capAlojamento) {
        this.capAlojamento = capAlojamento;
    }

    public Integer getTotalAlojamentos() {
        return totalAlojamentos;
    }

    public void setTotalAlojamentos(Integer totalAlojamentos) {
        this.totalAlojamentos = totalAlojamentos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TipoAlojamento other = (TipoAlojamento) o;
        return Objects.equals(tipoAlojamento, other.tipoAlojamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipoAlojamento);
    }

    @Override
    public String toString() {
        return "TipoAlojamento{" +
                "tipoAlojamento='" + tipoAlojamento + '\'' +
                ", capAlojamento=" + capAlojamento +
                '}';
    }
}
