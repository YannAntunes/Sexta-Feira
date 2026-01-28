package br.com.yann.sextafeira.dto;

import java.util.List;

public class ChartCategoriaRequestDTO {
    private String titulo;
    private List<ChartCategoriaItemDTO> items;

    public ChartCategoriaRequestDTO() {}

    public ChartCategoriaRequestDTO(String titulo, List<ChartCategoriaItemDTO> items) {
        this.titulo = titulo;
        this.items = items;
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public List<ChartCategoriaItemDTO> getItems() { return items; }
    public void setItems(List<ChartCategoriaItemDTO> items) { this.items = items; }
}
