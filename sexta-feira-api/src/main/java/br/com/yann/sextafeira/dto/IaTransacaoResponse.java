package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;

public class IaTransacaoResponse {

    private BigDecimal valor;
    private String data;      // ISO: "2026-01-17"
    private String tipo;      // "DESPESA" ou "RECEITA"
    private String categoria; // "ALIMENTACAO", etc.
    private String descricao;

    public IaTransacaoResponse() {
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
