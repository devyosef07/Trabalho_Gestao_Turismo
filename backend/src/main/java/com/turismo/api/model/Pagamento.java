package com.turismo.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Pagamento associado a uma reserva.
 * Corresponde à tabela Pagamento.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Pagamento {
    private String idPagamento; // PK
    private BigDecimal valor;
    private LocalDate dataPagamento;
    private String metodoPagamento;
    private String statusPagamento;
    private String idRecibo; // UNIQUE
    private String detalhesTransacao;
    private String idReserva; // FK to Reserva

    // Apenas leitura
    private String nomeTurista; // turista da reserva

    public Pagamento() {}

    public Pagamento(String idPagamento, BigDecimal valor, LocalDate dataPagamento, String metodoPagamento, String statusPagamento, String idRecibo, String detalhesTransacao, String idReserva) {
        this.idPagamento = idPagamento;
        this.valor = valor;
        this.dataPagamento = dataPagamento;
        this.metodoPagamento = metodoPagamento;
        this.statusPagamento = statusPagamento;
        this.idRecibo = idRecibo;
        this.detalhesTransacao = detalhesTransacao;
        this.idReserva = idReserva;
    }

    public String getIdPagamento() {
        return idPagamento;
    }

    public void setIdPagamento(String idPagamento) {
        this.idPagamento = idPagamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(String metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public String getStatusPagamento() {
        return statusPagamento;
    }

    public void setStatusPagamento(String statusPagamento) {
        this.statusPagamento = statusPagamento;
    }

    public String getIdRecibo() {
        return idRecibo;
    }

    public void setIdRecibo(String idRecibo) {
        this.idRecibo = idRecibo;
    }

    public String getDetalhesTransacao() {
        return detalhesTransacao;
    }

    public void setDetalhesTransacao(String detalhesTransacao) {
        this.detalhesTransacao = detalhesTransacao;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(String idReserva) {
        this.idReserva = idReserva;
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
        Pagamento other = (Pagamento) o;
        return Objects.equals(idPagamento, other.idPagamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPagamento);
    }

    @Override
    public String toString() {
        return "Pagamento{" +
                "idPagamento='" + idPagamento + '\'' +
                ", valor=" + valor +
                ", dataPagamento=" + dataPagamento +
                ", metodoPagamento='" + metodoPagamento + '\'' +
                ", statusPagamento='" + statusPagamento + '\'' +
                ", idRecibo='" + idRecibo + '\'' +
                ", detalhesTransacao='" + detalhesTransacao + '\'' +
                ", idReserva='" + idReserva + '\'' +
                '}';
    }
}
