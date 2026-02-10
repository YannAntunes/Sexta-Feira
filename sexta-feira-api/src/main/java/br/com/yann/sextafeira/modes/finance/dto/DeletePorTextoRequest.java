package br.com.yann.sextafeira.modes.finance.dto;

public class DeletePorTextoRequest {
    private String mensagem;

    public DeletePorTextoRequest() {}
    public DeletePorTextoRequest(String mensagem) { this.mensagem = mensagem; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
}
