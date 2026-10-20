package com.turismo.api.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Alojamento escolhido numa reserva (chave composta).
 * Corresponde à tabela Reserva_Alojamento.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class ReservaAlojamento {
    private String idReserva; // PK, FK
    private String idAlojamento; // PK, FK
    private BigDecimal avaliacaoAlojamento; // 0.0 a 5.0, opcional

    // Apenas leitura
    private String nomeAlojamento; // nome do alojamento
    private String nomeTurista; // turista da reserva

    public ReservaAlojamento() {}

    public ReservaAlojamento(String idReserva, String idAlojamento, BigDecimal avaliacaoAlojamento) {
        this.idReserva = idReserva;
        this.idAlojamento = idAlojamento;
        this.avaliacaoAlojamento = avaliacaoAlojamento;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(String idReserva) {
        this.idReserva = idReserva;
    }

    public String getIdAlojamento() {
        return idAlojamento;
    }

    public void setIdAlojamento(String idAlojamento) {
        this.idAlojamento = idAlojamento;
    }

    public BigDecimal getAvaliacaoAlojamento() {
        return avaliacaoAlojamento;
    }

    public void setAvaliacaoAlojamento(BigDecimal avaliacaoAlojamento) {
        this.avaliacaoAlojamento = avaliacaoAlojamento;
    }

    public String getNomeAlojamento() {
        return nomeAlojamento;
    }

    public void setNomeAlojamento(String nomeAlojamento) {
        this.nomeAlojamento = nomeAlojamento;
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
        ReservaAlojamento other = (ReservaAlojamento) o;
        return Objects.equals(idReserva, other.idReserva)
                && Objects.equals(idAlojamento, other.idAlojamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReserva, idAlojamento);
    }

    @Override
    public String toString() {
        return "ReservaAlojamento{" +
                "idReserva='" + idReserva + '\'' +
                ", idAlojamento='" + idAlojamento + '\'' +
                ", avaliacaoAlojamento=" + avaliacaoAlojamento +
                '}';
    }
}
