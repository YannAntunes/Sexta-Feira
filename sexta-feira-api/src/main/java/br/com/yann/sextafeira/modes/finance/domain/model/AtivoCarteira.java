package br.com.yann.sextafeira.modes.finance.domain.model;

import br.com.yann.sextafeira.identity.domain.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ativos_carteira",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "classe", "ticker"})
)
public class AtivoCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClasseAtivo classe;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(precision = 19, scale = 8)
    private BigDecimal precoMedio;

    @Column(length = 10)
    private String moedaBase = "BRL";

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public AtivoCarteira() {}

    public AtivoCarteira(ClasseAtivo classe, String ticker) {
        this.classe = classe;
        this.ticker = ticker;
    }

    public AtivoCarteira(Usuario usuario, ClasseAtivo classe, String ticker) {
        this.usuario = usuario;
        this.classe = classe;
        this.ticker = ticker;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
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
