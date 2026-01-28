package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;

public class ConvertResponse {
    private String from;
    private String to;
    private BigDecimal amount;
    private BigDecimal result;
    private BigDecimal rate; // 1 FROM -> TO
    private String date;
    private String source;

    public ConvertResponse() {}

    public ConvertResponse(String from, String to, BigDecimal amount, BigDecimal result,
                           BigDecimal rate, String date, String source) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.result = result;
        this.rate = rate;
        this.date = date;
        this.source = source;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getResult() { return result; }
    public BigDecimal getRate() { return rate; }
    public String getDate() { return date; }
    public String getSource() { return source; }
}
