package br.com.yann.sextafeira.dto;

public class ChatRequest {

    private String mensagem;

    public ChatRequest() {
    }

    public ChatRequest(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
