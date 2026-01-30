package br.com.yann.sextafeira.domain.model;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClasseAtivo classe; // ACAO/FII/CRIPTO

    @Column(nullable = false, length = 20)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentoCarteira tipo; // COMPRA/VENDA/APORTE_BRL

    // Quantidade movimentada (ex: 10 PETR4, 0.00009 BTC)
    @Column(precision = 19, scale = 8)
    private BigDecimal quantidade;

    // Preço unitário em BRL no momento do movimento (se conseguir cotação)
    @Column(name = "preco_unit_brl", precision = 19, scale = 8)
    private BigDecimal precoUnitBRL;

    // Valor total em BRL (ex: 10 * 38.40 = 384.00) OU o aporte informado
    @Column(name = "valor_brl", precision = 19, scale = 8)
    private BigDecimal valorBRL;

    // Data "do usuário" (se veio do texto) e também timestamp do registro
    private LocalDate data;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // Texto original (opcional, mas ajuda demais a debugar)
    @Column(length = 300)
    private String observacao;

    public MovimentoCarteira() {}

    public MovimentoCarteira(ClasseAtivo classe, String ticker, TipoMovimentoCarteira tipo) {
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
