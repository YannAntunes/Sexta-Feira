package br.com.yann.sextafeira.dto;

import java.util.List;

public class RelatorioMensalResponse {
    private String mesAno;
    private String resumoTexto;
    private List<String> alertas;
    private List<String> topCategorias;
    private List<String> linksGraficos;

    public RelatorioMensalResponse() {}

    public RelatorioMensalResponse(String mesAno, String resumoTexto, List<String> alertas,
                                   List<String> topCategorias, List<String> linksGraficos) {
        this.mesAno = mesAno;
        this.resumoTexto = resumoTexto;
        this.alertas = alertas;
        this.topCategorias = topCategorias;
        this.linksGraficos = linksGraficos;
    }

    public String getMesAno() { return mesAno; }
    public String getResumoTexto() { return resumoTexto; }
    public List<String> getAlertas() { return alertas; }
    public List<String> getTopCategorias() { return topCategorias; }
    public List<String> getLinksGraficos() { return linksGraficos; }
}
