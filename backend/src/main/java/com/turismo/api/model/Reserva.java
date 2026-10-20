package com.turismo.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Reserva de um turista.
 * Corresponde à tabela Reserva.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Reserva {
    private String idReserva; // PK
    private LocalDate dataChegada;
    private LocalDate dataPartida;
    private String statusReserva;
    private String idTurista; // FK to Turista

    // Apenas leitura
    private String nomeTurista; // nome do turista
    private Integer noites; // data_partida - data_chegada
    private BigDecimal totalPago; // Total_Pago_Reserva
    private String estadoCompletude; // Reserva_Completa

    public Reserva() {}

    public Reserva(String idReserva, LocalDate dataChegada, LocalDate dataPartida, String statusReserva, String idTurista) {
        this.idReserva = idReserva;
        this.dataChegada = dataChegada;
        this.dataPartida = dataPartida;
        this.statusReserva = statusReserva;
        this.idTurista = idTurista;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(String idReserva) {
        this.idReserva = idReserva;
    }

    public LocalDate getDataChegada() {
        return dataChegada;
    }

    public void setDataChegada(LocalDate dataChegada) {
        this.dataChegada = dataChegada;
    }

    public LocalDate getDataPartida() {
        return dataPartida;
    }

    public void setDataPartida(LocalDate dataPartida) {
        this.dataPartida = dataPartida;
    }

    public String getStatusReserva() {
        return statusReserva;
    }

    public void setStatusReserva(String statusReserva) {
        this.statusReserva = statusReserva;
    }

    public String getIdTurista() {
        return idTurista;
    }

    public void setIdTurista(String idTurista) {
        this.idTurista = idTurista;
    }

    public String getNomeTurista() {
        return nomeTurista;
    }

    public void setNomeTurista(String nomeTurista) {
        this.nomeTurista = nomeTurista;
    }

    public Integer getNoites() {
        return noites;
    }

    public void setNoites(Integer noites) {
        this.noites = noites;
    }

    public BigDecimal getTotalPago() {
        return totalPago;
    }

    public void setTotalPago(BigDecimal totalPago) {
        this.totalPago = totalPago;
    }

    public String getEstadoCompletude() {
        return estadoCompletude;
    }

    public void setEstadoCompletude(String estadoCompletude) {
        this.estadoCompletude = estadoCompletude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reserva other = (Reserva) o;
        return Objects.equals(idReserva, other.idReserva);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReserva);
    }

    @Override
    public String toString() {
        return "Reserva{" +
                "idReserva='" + idReserva + '\'' +
                ", dataChegada=" + dataChegada +
                ", dataPartida=" + dataPartida +
                ", statusReserva='" + statusReserva + '\'' +
                ", idTurista='" + idTurista + '\'' +
                '}';
    }
}
