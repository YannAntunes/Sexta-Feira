package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;

public class ConvertRequest {
    private BigDecimal amount;
    private String from; // "USD"
    private String to;   // "BRL"

    public ConvertRequest() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
}
