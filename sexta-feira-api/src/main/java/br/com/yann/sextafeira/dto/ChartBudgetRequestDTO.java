package br.com.yann.sextafeira.dto;

import java.util.List;

public class ChartBudgetRequestDTO {
    private String titulo;
    private List<ChartBudgetItemDTO> items;

    public ChartBudgetRequestDTO() {}

    public ChartBudgetRequestDTO(String titulo, List<ChartBudgetItemDTO> items) {
        this.titulo = titulo;
        this.items = items;
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public List<ChartBudgetItemDTO> getItems() { return items; }
    public void setItems(List<ChartBudgetItemDTO> items) { this.items = items; }
}
