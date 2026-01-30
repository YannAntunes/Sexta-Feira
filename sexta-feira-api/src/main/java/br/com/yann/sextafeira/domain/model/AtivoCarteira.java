package br.com.yann.sextafeira.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ativos_carteira",
        uniqueConstraints = @UniqueConstraint(columnNames = {"classe", "ticker"})
)
public class AtivoCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClasseAtivo classe;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(precision = 19, scale = 8)
    private BigDecimal precoMedio; // opcional (pode ficar null no começo)

    @Column(length = 10)
    private String moedaBase = "BRL"; // BRL, USD (cripto pode ser BRL)

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public AtivoCarteira() {}

    public AtivoCarteira(ClasseAtivo classe, String ticker) {
        this.classe = classe;
        this.ticker = ticker;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ClasseAtivo getClasse() { return classe; }
    public void setClasse(ClasseAtivo classe) { this.classe = classe; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }

    public String getMoedaBase() { return moedaBase; }
    public void setMoedaBase(String moedaBase) { this.moedaBase = moedaBase; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
