package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;

public class ChartSerieItemDTO {
    private String label;
    private BigDecimal value;

    public ChartSerieItemDTO() {}

    public ChartSerieItemDTO(String label, BigDecimal value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}
