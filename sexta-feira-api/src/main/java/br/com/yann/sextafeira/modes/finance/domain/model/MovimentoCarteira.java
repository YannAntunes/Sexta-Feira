package br.com.yann.sextafeira.modes.finance.domain.model;

import br.com.yann.sextafeira.identity.domain.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_carteira")
public class MovimentoCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClasseAtivo classe; // ACAO/FII/CRIPTO

    @Column(nullable = false, length = 20)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentoCarteira tipo; // COMPRA/VENDA/APORTE_BRL

    @Column(precision = 19, scale = 8)
    private BigDecimal quantidade;

    @Column(name = "preco_unit_brl", precision = 19, scale = 8)
    private BigDecimal precoUnitBRL;

    @Column(name = "valor_brl", precision = 19, scale = 8)
    private BigDecimal valorBRL;

    private LocalDate data;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(length = 300)
    private String observacao;

    public MovimentoCarteira() {}

    public MovimentoCarteira(ClasseAtivo classe, String ticker, TipoMovimentoCarteira tipo) {
        this.classe = classe;
        this.ticker = ticker;
        this.tipo = tipo;
    }

    public MovimentoCarteira(Usuario usuario, ClasseAtivo classe, String ticker, TipoMovimentoCarteira tipo) {
        this.usuario = usuario;
        this.classe = classe;
        this.ticker = ticker;
        this.tipo = tipo;
    }

    public MovimentoCarteira(ClasseAtivo classe,
                             String ticker,
                             TipoMovimentoCarteira tipo,
                             BigDecimal quantidade,
                             BigDecimal precoUnitBRL,
                             BigDecimal valorBRL,
                             LocalDate data,
                             String observacao) {
        this.classe = classe;
        this.ticker = ticker;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoUnitBRL = precoUnitBRL;
        this.valorBRL = valorBRL;
        this.data = data;
        this.observacao = observacao;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public ClasseAtivo getClasse() { return classe; }
    public void setClasse(ClasseAtivo classe) { this.classe = classe; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public TipoMovimentoCarteira getTipo() { return tipo; }
    public void setTipo(TipoMovimentoCarteira tipo) { this.tipo = tipo; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitBRL() { return precoUnitBRL; }
    public void setPrecoUnitBRL(BigDecimal precoUnitBRL) { this.precoUnitBRL = precoUnitBRL; }

    public BigDecimal getValorBRL() { return valorBRL; }
    public void setValorBRL(BigDecimal valorBRL) { this.valorBRL = valorBRL; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
