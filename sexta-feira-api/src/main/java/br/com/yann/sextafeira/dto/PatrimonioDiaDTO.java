package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PatrimonioDiaDTO {
    private LocalDate data;
    private BigDecimal total;

    public PatrimonioDiaDTO(LocalDate data, BigDecimal total) {
        this.data = data;
        this.total = total;
    }

    public LocalDate getData() { return data; }
    public BigDecimal getTotal() { return total; }
}
