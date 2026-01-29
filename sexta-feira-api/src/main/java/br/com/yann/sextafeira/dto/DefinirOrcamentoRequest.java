package br.com.yann.sextafeira.dto;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;

import java.math.BigDecimal;

public class DefinirOrcamentoRequest {

    private int ano;
    private int mes;
    private CategoriaTransacao categoria;
    private BigDecimal valor;

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }

    public CategoriaTransacao getCategoria() { return categoria; }
    public void setCategoria(CategoriaTransacao categoria) { this.categoria = categoria; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
