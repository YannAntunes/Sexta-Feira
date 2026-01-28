package br.com.yann.sextafeira.dto;

import java.util.List;

public class ChartSerieRequestDTO {
    private String titulo;
    private String y_label;
    private List<ChartSerieItemDTO> items;

    public ChartSerieRequestDTO() {}

    public ChartSerieRequestDTO(String titulo, String y_label, List<ChartSerieItemDTO> items) {
        this.titulo = titulo;
        this.y_label = y_label;
        this.items = items;
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getY_label() { return y_label; }
    public void setY_label(String y_label) { this.y_label = y_label; }

    public List<ChartSerieItemDTO> getItems() { return items; }
    public void setItems(List<ChartSerieItemDTO> items) { this.items = items; }
}
