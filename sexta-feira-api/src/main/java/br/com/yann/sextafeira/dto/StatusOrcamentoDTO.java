package br.com.yann.sextafeira.dto;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;

import java.math.BigDecimal;

public class StatusOrcamentoDTO {

    private Integer ano;
    private Integer mes;
    private CategoriaTransacao categoria;
    private BigDecimal limite;
    private BigDecimal gasto;
    private BigDecimal restante;
    private boolean estourado;

    public StatusOrcamentoDTO(Integer ano, Integer mes, CategoriaTransacao categoria,
                              BigDecimal limite, BigDecimal gasto, BigDecimal restante,
                              boolean estourado) {
        this.ano = ano;
        this.mes = mes;
        this.categoria = categoria;
        this.limite = limite;
        this.gasto = gasto;
        this.restante = restante;
        this.estourado = estourado;
    }

    public Integer getAno() { return ano; }
    public Integer getMes() { return mes; }
    public CategoriaTransacao getCategoria() { return categoria; }
    public BigDecimal getLimite() { return limite; }
    public BigDecimal getGasto() { return gasto; }
    public BigDecimal getRestante() { return restante; }
    public boolean isEstourado() { return estourado; }
}
