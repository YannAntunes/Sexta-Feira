package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;

public class ResumoMensalDTO {

    private int ano;
    private int mes;
    private BigDecimal totalDespesas;
    private BigDecimal totalReceitas;
    private BigDecimal saldo;

    public ResumoMensalDTO(int ano, int mes,
                           BigDecimal totalDespesas,
                           BigDecimal totalReceitas,
                           BigDecimal saldo) {
        this.ano = ano;
        this.mes = mes;
        this.totalDespesas = totalDespesas;
        this.totalReceitas = totalReceitas;
        this.saldo = saldo;
    }

    public int getAno() {
        return ano;
    }

    public int getMes() {
        return mes;
    }

    public BigDecimal getTotalDespesas() {
        return totalDespesas;
    }

    public BigDecimal getTotalReceitas() {
        return totalReceitas;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
