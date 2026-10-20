package com.turismo.api.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Visita de um turista a um destino (chave composta).
 * Corresponde à tabela Turista_Destino.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class TuristaDestino {
    private String idDestino; // PK, FK
    private String idTurista; // PK, FK
    private LocalDate dataVisita; // PK

    // Apenas leitura
    private String nomeDestino; // nome do destino
    private String nomeTurista; // nome do turista

    public TuristaDestino() {}

    public TuristaDestino(String idDestino, String idTurista, LocalDate dataVisita) {
        this.idDestino = idDestino;
        this.idTurista = idTurista;
        this.dataVisita = dataVisita;
    }

    public String getIdDestino() {
        return idDestino;
    }

    public void setIdDestino(String idDestino) {
        this.idDestino = idDestino;
    }

    public String getIdTurista() {
        return idTurista;
    }

    public void setIdTurista(String idTurista) {
        this.idTurista = idTurista;
    }

    public LocalDate getDataVisita() {
        return dataVisita;
    }

    public void setDataVisita(LocalDate dataVisita) {
        this.dataVisita = dataVisita;
    }

    public String getNomeDestino() {
        return nomeDestino;
    }

    public void setNomeDestino(String nomeDestino) {
        this.nomeDestino = nomeDestino;
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
        TuristaDestino other = (TuristaDestino) o;
        return Objects.equals(idDestino, other.idDestino)
                && Objects.equals(idTurista, other.idTurista)
                && Objects.equals(dataVisita, other.dataVisita);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDestino, idTurista, dataVisita);
    }

    @Override
    public String toString() {
        return "TuristaDestino{" +
                "idDestino='" + idDestino + '\'' +
                ", idTurista='" + idTurista + '\'' +
                ", dataVisita=" + dataVisita +
                '}';
    }
}
