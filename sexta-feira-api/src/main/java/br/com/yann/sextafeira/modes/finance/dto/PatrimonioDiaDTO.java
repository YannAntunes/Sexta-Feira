package br.com.yann.sextafeira.modes.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PatrimonioDiaDTO {
    private LocalDate data;
    private BigDecimal valor;

    public PatrimonioDiaDTO(LocalDate data, BigDecimal valor) {
        this.data = data;
        this.valor = valor;
    }

    public LocalDate getData() { return data; }
    public BigDecimal getValor() { return valor; }
}
