package br.com.yann.sextafeira.dto;

import java.math.BigDecimal;
import java.util.List;

public class CarteiraResumoDTO {

    private String periodoLabel;
    private BigDecimal totalAcoes;
    private BigDecimal totalFiis;
    private BigDecimal totalCripto;
    private BigDecimal totalGeral;
    private List<CarteiraAtivoItemDTO> itens;

    public CarteiraResumoDTO(String periodoLabel,
                             BigDecimal totalAcoes,
                             BigDecimal totalFiis,
                             BigDecimal totalCripto,
                             BigDecimal totalGeral,
                             List<CarteiraAtivoItemDTO> itens) {
        this.periodoLabel = periodoLabel;
        this.totalAcoes = totalAcoes;
        this.totalFiis = totalFiis;
        this.totalCripto = totalCripto;
        this.totalGeral = totalGeral;
        this.itens = itens;
    }

    public String getPeriodoLabel() { return periodoLabel; }
    public BigDecimal getTotalAcoes() { return totalAcoes; }
    public BigDecimal getTotalFiis() { return totalFiis; }
    public BigDecimal getTotalCripto() { return totalCripto; }
    public BigDecimal getTotalGeral() { return totalGeral; }
    public List<CarteiraAtivoItemDTO> getItens() { return itens; }
}
