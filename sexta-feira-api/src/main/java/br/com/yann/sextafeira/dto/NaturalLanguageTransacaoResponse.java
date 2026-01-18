package br.com.yann.sextafeira.dto;

import br.com.yann.sextafeira.domain.model.Transacao;

public class NaturalLanguageTransacaoResponse {

    private String mensagemOriginal;
    private Transacao transacaoCriada;

    public NaturalLanguageTransacaoResponse() {
    }

    public NaturalLanguageTransacaoResponse(String mensagemOriginal, Transacao transacaoCriada) {
        this.mensagemOriginal = mensagemOriginal;
        this.transacaoCriada = transacaoCriada;
    }

    public String getMensagemOriginal() {
        return mensagemOriginal;
    }

    public void setMensagemOriginal(String mensagemOriginal) {
        this.mensagemOriginal = mensagemOriginal;
    }

    public Transacao getTransacaoCriada() {
        return transacaoCriada;
    }

    public void setTransacaoCriada(Transacao transacaoCriada) {
        this.transacaoCriada = transacaoCriada;
    }
}
