package br.com.yann.sextafeira.core.domain.model;

import br.com.yann.sextafeira.identity.domain.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens_historico", indexes = {
        @Index(name = "idx_msg_hist_usuario_data", columnList = "usuario_id, criado_em")
})
public class MensagemHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelMensagem papel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(length = 60)
    private String intencao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public MensagemHistorico() {}

    public MensagemHistorico(Usuario usuario, PapelMensagem papel, String conteudo, String intencao) {
        this.usuario = usuario;
        this.papel = papel;
        this.conteudo = conteudo;
        this.intencao = intencao;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public PapelMensagem getPapel() {
        return papel;
    }

    public void setPapel(PapelMensagem papel) {
        this.papel = papel;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getIntencao() {
        return intencao;
    }

    public void setIntencao(String intencao) {
        this.intencao = intencao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
