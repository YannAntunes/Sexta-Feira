package br.com.yann.sextafeira.dto;

public class NaturalLanguageTransacaoRequest {

    private String mensagem;

    public NaturalLanguageTransacaoRequest() {
    }

    public NaturalLanguageTransacaoRequest(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
