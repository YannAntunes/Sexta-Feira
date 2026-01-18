package br.com.yann.sextafeira.dto;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;

import java.math.BigDecimal;

public class OrcamentoRequest {

    private Integer ano;
    private Integer mes;
    private CategoriaTransacao categoria;
    private BigDecimal valorLimite;

    public OrcamentoRequest() {
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public CategoriaTransacao getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaTransacao categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getValorLimite() {
        return valorLimite;
    }

    public void setValorLimite(BigDecimal valorLimite) {
        this.valorLimite = valorLimite;
    }
}
