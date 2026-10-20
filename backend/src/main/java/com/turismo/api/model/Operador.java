package com.turismo.api.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Operador turístico.
 * Corresponde à tabela Operador.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Operador {
    private String idOperador; // PK
    private String nomeOperador;
    private String especialidade;
    private BigDecimal classOperador; // 0.0 a 5.0

    // Apenas leitura
    private Integer totalTuristas; // turistas acompanhados
    private BigDecimal mediaAvaliacao; // Media_Avaliacao_Operador

    public Operador() {}

    public Operador(String idOperador, String nomeOperador, String especialidade, BigDecimal classOperador) {
        this.idOperador = idOperador;
        this.nomeOperador = nomeOperador;
        this.especialidade = especialidade;
        this.classOperador = classOperador;
    }

    public String getIdOperador() {
        return idOperador;
    }

    public void setIdOperador(String idOperador) {
        this.idOperador = idOperador;
    }

    public String getNomeOperador() {
        return nomeOperador;
    }

    public void setNomeOperador(String nomeOperador) {
        this.nomeOperador = nomeOperador;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public BigDecimal getClassOperador() {
        return classOperador;
    }

    public void setClassOperador(BigDecimal classOperador) {
        this.classOperador = classOperador;
    }

    public Integer getTotalTuristas() {
        return totalTuristas;
    }

    public void setTotalTuristas(Integer totalTuristas) {
        this.totalTuristas = totalTuristas;
    }

    public BigDecimal getMediaAvaliacao() {
        return mediaAvaliacao;
    }

    public void setMediaAvaliacao(BigDecimal mediaAvaliacao) {
        this.mediaAvaliacao = mediaAvaliacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operador other = (Operador) o;
        return Objects.equals(idOperador, other.idOperador);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idOperador);
    }

    @Override
    public String toString() {
        return "Operador{" +
                "idOperador='" + idOperador + '\'' +
                ", nomeOperador='" + nomeOperador + '\'' +
                ", especialidade='" + especialidade + '\'' +
                ", classOperador=" + classOperador +
                '}';
    }
}
