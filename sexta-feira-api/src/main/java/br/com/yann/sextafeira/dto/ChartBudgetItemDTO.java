package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;

public class ChartBudgetItemDTO {
    private String categoria;
    private BigDecimal orcamento;
    private BigDecimal gasto;

    public ChartBudgetItemDTO() {}

    public ChartBudgetItemDTO(String categoria, BigDecimal orcamento, BigDecimal gasto) {
        this.categoria = categoria;
        this.orcamento = orcamento;
        this.gasto = gasto;
    }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getOrcamento() { return orcamento; }
    public void setOrcamento(BigDecimal orcamento) { this.orcamento = orcamento; }

    public BigDecimal getGasto() { return gasto; }
    public void setGasto(BigDecimal gasto) { this.gasto = gasto; }
}
