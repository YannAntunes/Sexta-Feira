package br.com.yann.sextafeira.dto;

import java.util.List;

public class BrapiQuoteResponse {
    private List<Result> results;

    public List<Result> getResults() { return results; }
    public void setResults(List<Result> results) { this.results = results; }

    public static class Result {
        private String symbol;
        private Double regularMarketPrice;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public Double getRegularMarketPrice() { return regularMarketPrice; }
        public void setRegularMarketPrice(Double regularMarketPrice) { this.regularMarketPrice = regularMarketPrice; }
    }
}
