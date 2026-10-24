package br.com.yann.sextafeira.identity.dto;

import br.com.yann.sextafeira.core.domain.model.Personalidade;
import br.com.yann.sextafeira.identity.domain.Usuario;

import java.time.LocalDateTime;

public class UsuarioPerfilDTO {

    private Long id;
    private String email;
    private String nome;
    private Personalidade personalidade;
    private String idiomaPreferido;
    private LocalDateTime criadoEm;

    public UsuarioPerfilDTO() {
    }

    public UsuarioPerfilDTO(Usuario usuario) {
        if (usuario != null) {
            this.id = usuario.getId();
            this.email = usuario.getEmail();
            this.nome = usuario.getNome();
            this.personalidade = usuario.getPersonalidade();
            this.idiomaPreferido = usuario.getIdiomaPreferido();
            this.criadoEm = usuario.getCriadoEm();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Personalidade getPersonalidade() {
        return personalidade;
    }

    public void setPersonalidade(Personalidade personalidade) {
        this.personalidade = personalidade;
    }

    public String getIdiomaPreferido() {
        return idiomaPreferido;
    }

    public void setIdiomaPreferido(String idiomaPreferido) {
        this.idiomaPreferido = idiomaPreferido;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
