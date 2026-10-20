package com.turismo.api.model;

import java.util.Objects;

/**
 * Atração turística de um destino.
 * Corresponde à tabela Atracao.
 * Os campos marcados como "apenas leitura" vêm de joins ou funções da BD e são ignorados nas escritas.
 */
public class Atracao {
    private String idAtracao; // PK
    private String tipoAtracao;
    private String descricao;
    private String horaAbertura; // HH:MM
    private String horaFecho; // HH:MM
    private String horaAberturaEspecial; // HH:MM, opcional
    private String horaFechoEspecial; // HH:MM, opcional
    private String detalhesHorarioEspecial;
    private String idDestino; // FK to Destino

    // Apenas leitura
    private String nomeDestino; // nome do destino

    public Atracao() {}

    public Atracao(String idAtracao, String tipoAtracao, String descricao, String horaAbertura, String horaFecho, String horaAberturaEspecial, String horaFechoEspecial, String detalhesHorarioEspecial, String idDestino) {
        this.idAtracao = idAtracao;
        this.tipoAtracao = tipoAtracao;
        this.descricao = descricao;
        this.horaAbertura = horaAbertura;
        this.horaFecho = horaFecho;
        this.horaAberturaEspecial = horaAberturaEspecial;
        this.horaFechoEspecial = horaFechoEspecial;
        this.detalhesHorarioEspecial = detalhesHorarioEspecial;
        this.idDestino = idDestino;
    }

    public String getIdAtracao() {
        return idAtracao;
    }

    public void setIdAtracao(String idAtracao) {
        this.idAtracao = idAtracao;
    }

    public String getTipoAtracao() {
        return tipoAtracao;
    }

    public void setTipoAtracao(String tipoAtracao) {
        this.tipoAtracao = tipoAtracao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getHoraAbertura() {
        return horaAbertura;
    }

    public void setHoraAbertura(String horaAbertura) {
        this.horaAbertura = horaAbertura;
    }

    public String getHoraFecho() {
        return horaFecho;
    }

    public void setHoraFecho(String horaFecho) {
        this.horaFecho = horaFecho;
    }

    public String getHoraAberturaEspecial() {
        return horaAberturaEspecial;
    }

    public void setHoraAberturaEspecial(String horaAberturaEspecial) {
        this.horaAberturaEspecial = horaAberturaEspecial;
    }

    public String getHoraFechoEspecial() {
        return horaFechoEspecial;
    }

    public void setHoraFechoEspecial(String horaFechoEspecial) {
        this.horaFechoEspecial = horaFechoEspecial;
    }

    public String getDetalhesHorarioEspecial() {
        return detalhesHorarioEspecial;
    }

    public void setDetalhesHorarioEspecial(String detalhesHorarioEspecial) {
        this.detalhesHorarioEspecial = detalhesHorarioEspecial;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Atracao other = (Atracao) o;
        return Objects.equals(idAtracao, other.idAtracao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAtracao);
    }

    @Override
    public String toString() {
        return "Atracao{" +
                "idAtracao='" + idAtracao + '\'' +
                ", tipoAtracao='" + tipoAtracao + '\'' +
                ", descricao='" + descricao + '\'' +
                ", horaAbertura='" + horaAbertura + '\'' +
                ", horaFecho='" + horaFecho + '\'' +
                ", horaAberturaEspecial='" + horaAberturaEspecial + '\'' +
                ", horaFechoEspecial='" + horaFechoEspecial + '\'' +
                ", detalhesHorarioEspecial='" + detalhesHorarioEspecial + '\'' +
                ", idDestino='" + idDestino + '\'' +
                '}';
    }
}
