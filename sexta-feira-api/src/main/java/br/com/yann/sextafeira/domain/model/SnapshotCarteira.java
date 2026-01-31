package br.com.yann.sextafeira.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "snapshots_carteira",
        uniqueConstraints = @UniqueConstraint(name = "uk_snapshot", columnNames = {"data", "classe", "ticker"})
)
public class SnapshotCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClasseAtivo classe;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantidade;

    @Column(name = "preco_unit_brl", nullable = false, precision = 19, scale = 8)
    private BigDecimal precoUnitBRL;

    @Column(name = "valor_brl", nullable = false, precision = 19, scale = 8)
    private BigDecimal valorBRL;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public SnapshotCarteira() {}

    public SnapshotCarteira(LocalDate data, ClasseAtivo classe, String ticker,
                            BigDecimal quantidade, BigDecimal precoUnitBRL, BigDecimal valorBRL) {
        this.data = data;
        this.classe = classe;
        this.ticker = ticker;
        this.quantidade = quantidade;
        this.precoUnitBRL = precoUnitBRL;
        this.valorBRL = valorBRL;
    }

    public Long getId() { return id; }
    public LocalDate getData() { return data; }
    public ClasseAtivo getClasse() { return classe; }
    public String getTicker() { return ticker; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitBRL() { return precoUnitBRL; }
    public BigDecimal getValorBRL() { return valorBRL; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
