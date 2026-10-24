package br.com.yann.sextafeira.modes.finance.dto;

import java.math.BigDecimal;

public class BalancoMensalDTO {

    private BigDecimal totalReceitas;
    private BigDecimal totalDespesas;
    private BigDecimal saldo;
    private BigDecimal taxaPoupanca;
    private int totalTransacoes;
    private String mesAno;

    public BalancoMensalDTO() {
    }

    public BalancoMensalDTO(BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldo,
                            BigDecimal taxaPoupanca, int totalTransacoes, String mesAno) {
        this.totalReceitas = totalReceitas;
        this.totalDespesas = totalDespesas;
        this.saldo = saldo;
        this.taxaPoupanca = taxaPoupanca;
        this.totalTransacoes = totalTransacoes;
        this.mesAno = mesAno;
    }

    public BigDecimal getTotalReceitas() {
        return totalReceitas;
    }

    public void setTotalReceitas(BigDecimal totalReceitas) {
        this.totalReceitas = totalReceitas;
    }

    public BigDecimal getTotalDespesas() {
        return totalDespesas;
    }

    public void setTotalDespesas(BigDecimal totalDespesas) {
        this.totalDespesas = totalDespesas;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getTaxaPoupanca() {
        return taxaPoupanca;
    }

    public void setTaxaPoupanca(BigDecimal taxaPoupanca) {
        this.taxaPoupanca = taxaPoupanca;
    }

    public int getTotalTransacoes() {
        return totalTransacoes;
    }

    public void setTotalTransacoes(int totalTransacoes) {
        this.totalTransacoes = totalTransacoes;
    }

    public String getMesAno() {
        return mesAno;
    }

    public void setMesAno(String mesAno) {
        this.mesAno = mesAno;
    }
}
