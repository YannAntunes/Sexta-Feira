package br.com.yann.sextafeira.identity.dto;

import br.com.yann.sextafeira.core.domain.model.Personalidade;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    private String email;

    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres.")
    private String nome;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
    private String senha;

    private String idiomaPreferido = "pt-BR";

    private Personalidade personalidade = Personalidade.SEXTA_FEIRA;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String nome, String senha) {
        this.email = email;
        this.nome = nome;
        this.senha = senha;
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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getIdiomaPreferido() {
        return idiomaPreferido;
    }

    public void setIdiomaPreferido(String idiomaPreferido) {
        this.idiomaPreferido = idiomaPreferido;
    }

    public Personalidade getPersonalidade() {
        return personalidade;
    }

    public void setPersonalidade(Personalidade personalidade) {
        this.personalidade = personalidade;
    }
}
