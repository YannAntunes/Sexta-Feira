package br.com.yann.sextafeira.identity.dto;

import br.com.yann.sextafeira.core.domain.model.Personalidade;
import jakarta.validation.constraints.Size;

public class AtualizarPerfilRequest {

    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres.")
    private String nome;

    private Personalidade personalidade;

    private String idiomaPreferido;

    public AtualizarPerfilRequest() {
    }

    public AtualizarPerfilRequest(String nome, Personalidade personalidade, String idiomaPreferido) {
        this.nome = nome;
        this.personalidade = personalidade;
        this.idiomaPreferido = idiomaPreferido;
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
}
