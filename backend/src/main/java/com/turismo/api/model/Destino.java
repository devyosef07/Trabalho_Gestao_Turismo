package com.turismo.api.model;

import java.util.Objects;

/**
 * Destino turístico.
 * Corresponde à tabela Destino.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Destino {
    private String idDestino; // PK
    private String nomeDestino;
    private String regiao;
    private Integer capacidadeVisitantes;

    // Apenas leitura
    private Integer totalAtracoes; // número de atrações do destino
    private Integer totalAlojamentos; // número de alojamentos do destino

    public Destino() {}

    public Destino(String idDestino, String nomeDestino, String regiao, Integer capacidadeVisitantes) {
        this.idDestino = idDestino;
        this.nomeDestino = nomeDestino;
        this.regiao = regiao;
        this.capacidadeVisitantes = capacidadeVisitantes;
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

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public Integer getCapacidadeVisitantes() {
        return capacidadeVisitantes;
    }

    public void setCapacidadeVisitantes(Integer capacidadeVisitantes) {
        this.capacidadeVisitantes = capacidadeVisitantes;
    }

    public Integer getTotalAtracoes() {
        return totalAtracoes;
    }

    public void setTotalAtracoes(Integer totalAtracoes) {
        this.totalAtracoes = totalAtracoes;
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
        Destino other = (Destino) o;
        return Objects.equals(idDestino, other.idDestino);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDestino);
    }

    @Override
    public String toString() {
        return "Destino{" +
                "idDestino='" + idDestino + '\'' +
                ", nomeDestino='" + nomeDestino + '\'' +
                ", regiao='" + regiao + '\'' +
                ", capacidadeVisitantes=" + capacidadeVisitantes +
                '}';
    }
}
