package br.com.yann.sextafeira.dto;

import br.com.yann.sextafeira.domain.model.ClasseAtivo;

import java.math.BigDecimal;

public class CarteiraAtivoItemDTO {

    private ClasseAtivo classe;
    private String ticker;
    private BigDecimal quantidade;
    private BigDecimal precoAtual;
    private BigDecimal valorEstimado;

    public CarteiraAtivoItemDTO() {}

    public CarteiraAtivoItemDTO(ClasseAtivo classe, String ticker, BigDecimal quantidade,
                                BigDecimal precoAtual, BigDecimal valorEstimado) {
        this.classe = classe;
        this.ticker = ticker;
        this.quantidade = quantidade;
        this.precoAtual = precoAtual;
        this.valorEstimado = valorEstimado;
    }

    public ClasseAtivo getClasse() { return classe; }
    public String getTicker() { return ticker; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getPrecoAtual() { return precoAtual; }
    public BigDecimal getValorEstimado() { return valorEstimado; }
}
