package com.turismo.api.model;

import java.util.Objects;

/**
 * Turista.
 * Corresponde à tabela Turista.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Turista {
    private String idTurista; // PK
    private String nomeTurista;
    private String nacionalidade;

    // Apenas leitura
    private Integer totalReservas; // número de reservas do turista

    public Turista() {}

    public Turista(String idTurista, String nomeTurista, String nacionalidade) {
        this.idTurista = idTurista;
        this.nomeTurista = nomeTurista;
        this.nacionalidade = nacionalidade;
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

    public String getNacionalidade() {
        return nacionalidade;
    }

    public void setNacionalidade(String nacionalidade) {
        this.nacionalidade = nacionalidade;
    }

    public Integer getTotalReservas() {
        return totalReservas;
    }

    public void setTotalReservas(Integer totalReservas) {
        this.totalReservas = totalReservas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Turista other = (Turista) o;
        return Objects.equals(idTurista, other.idTurista);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTurista);
    }

    @Override
    public String toString() {
        return "Turista{" +
                "idTurista='" + idTurista + '\'' +
                ", nomeTurista='" + nomeTurista + '\'' +
                ", nacionalidade='" + nacionalidade + '\'' +
                '}';
    }
}
