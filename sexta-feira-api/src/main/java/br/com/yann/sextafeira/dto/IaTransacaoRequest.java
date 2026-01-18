package br.com.yann.sextafeira.dto;

public class IaTransacaoRequest {

    private String mensagem;

    public IaTransacaoRequest() {
    }

    public IaTransacaoRequest(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
