package br.com.yann.sextafeira.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_orcamento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ano", "mes", "categoria", "tipoAlerta"}))
public class AlertaOrcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int ano;
    private int mes;

    @Enumerated(EnumType.STRING)
    private CategoriaTransacao categoria;

    // "PERCENT_80" ou "ESTOURO"
    private String tipoAlerta;

    private LocalDateTime enviadoEm;

    public AlertaOrcamento() {}

    public AlertaOrcamento(int ano, int mes, CategoriaTransacao categoria, String tipoAlerta, LocalDateTime enviadoEm) {
        this.ano = ano;
        this.mes = mes;
        this.categoria = categoria;
        this.tipoAlerta = tipoAlerta;
        this.enviadoEm = enviadoEm;
    }

    public Long getId() { return id; }
    public int getAno() { return ano; }
    public int getMes() { return mes; }
    public CategoriaTransacao getCategoria() { return categoria; }
    public String getTipoAlerta() { return tipoAlerta; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
    public void setEnviadoEm(LocalDateTime enviadoEm) { this.enviadoEm = enviadoEm; }
}
