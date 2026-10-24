package br.com.yann.sextafeira.identity.domain;

import br.com.yann.sextafeira.core.domain.model.Personalidade;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String nome;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Column(name = "idioma_preferido", nullable = false)
    private String idiomaPreferido = "pt-BR";

    /**
     * @deprecated O campo whatsapp_number foi mantido apenas para migracao retroativa.
     * A nova identidade e baseada exclusivamente em email/senha e tokens JWT.
     */
    @Deprecated
    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Personalidade personalidade = Personalidade.SEXTA_FEIRA;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public Usuario() {
    }

    public Usuario(String email, String nome, String senhaHash) {
        this.email = email;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getIdiomaPreferido() {
        return idiomaPreferido;
    }

    public void setIdiomaPreferido(String idiomaPreferido) {
        this.idiomaPreferido = idiomaPreferido;
    }

    @Deprecated
    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    @Deprecated
    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public Personalidade getPersonalidade() {
        return personalidade;
    }

    public void setPersonalidade(Personalidade personalidade) {
        this.personalidade = personalidade;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
