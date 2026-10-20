package com.turismo.api.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Alojamento (hotel, pousada...) de um destino.
 * Corresponde à tabela Alojamento.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Alojamento {
    private String idAlojamento; // PK
    private String nomeAlojamento;
    private BigDecimal classAlojamento; // 0.0 a 5.0
    private String tipoAlojamento; // FK to TipoAlojamento
    private String idDestino; // FK to Destino

    // Apenas leitura
    private String nomeDestino; // nome do destino
    private Integer capAlojamento; // capacidade do tipo de alojamento
    private BigDecimal mediaAvaliacoes; // Media_Avaliacao_Alojamento

    public Alojamento() {}

    public Alojamento(String idAlojamento, String nomeAlojamento, BigDecimal classAlojamento, String tipoAlojamento, String idDestino) {
        this.idAlojamento = idAlojamento;
        this.nomeAlojamento = nomeAlojamento;
        this.classAlojamento = classAlojamento;
        this.tipoAlojamento = tipoAlojamento;
        this.idDestino = idDestino;
    }

    public String getIdAlojamento() {
        return idAlojamento;
    }

    public void setIdAlojamento(String idAlojamento) {
        this.idAlojamento = idAlojamento;
    }

    public String getNomeAlojamento() {
        return nomeAlojamento;
    }

    public void setNomeAlojamento(String nomeAlojamento) {
        this.nomeAlojamento = nomeAlojamento;
    }

    public BigDecimal getClassAlojamento() {
        return classAlojamento;
    }

    public void setClassAlojamento(BigDecimal classAlojamento) {
        this.classAlojamento = classAlojamento;
    }

    public String getTipoAlojamento() {
        return tipoAlojamento;
    }

    public void setTipoAlojamento(String tipoAlojamento) {
        this.tipoAlojamento = tipoAlojamento;
    }

    public String getIdDestino() {
        return idDestino;
    }

    public void setIdDestino(String idDestino) {
        this.idDestino = idDestino;
    }

    public String getNomeDestino() {
        return nomeDestino;
    }

    public void setNomeDestino(String nomeDestino) {
        this.nomeDestino = nomeDestino;
    }

    public Integer getCapAlojamento() {
        return capAlojamento;
    }

    public void setCapAlojamento(Integer capAlojamento) {
        this.capAlojamento = capAlojamento;
    }

    public BigDecimal getMediaAvaliacoes() {
        return mediaAvaliacoes;
    }

    public void setMediaAvaliacoes(BigDecimal mediaAvaliacoes) {
        this.mediaAvaliacoes = mediaAvaliacoes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alojamento other = (Alojamento) o;
        return Objects.equals(idAlojamento, other.idAlojamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAlojamento);
    }

    @Override
    public String toString() {
        return "Alojamento{" +
                "idAlojamento='" + idAlojamento + '\'' +
                ", nomeAlojamento='" + nomeAlojamento + '\'' +
                ", classAlojamento=" + classAlojamento +
                ", tipoAlojamento='" + tipoAlojamento + '\'' +
                ", idDestino='" + idDestino + '\'' +
                '}';
    }
}
