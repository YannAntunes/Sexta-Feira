package br.com.yann.sextafeira.modes.finance.dto;

import br.com.yann.sextafeira.core.dto.HistoricoMensagemDTO;

import java.util.List;

public class IaTransacaoRequest {

    private String mensagem;
    private String personalidade;
    private List<HistoricoMensagemDTO> historico;

    public IaTransacaoRequest() {
    }

    public IaTransacaoRequest(String mensagem) {
        this.mensagem = mensagem;
    }

    public IaTransacaoRequest(String mensagem, String personalidade, List<HistoricoMensagemDTO> historico) {
        this.mensagem = mensagem;
        this.personalidade = personalidade;
        this.historico = historico;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getPersonalidade() {
        return personalidade;
    }

    public void setPersonalidade(String personalidade) {
        this.personalidade = personalidade;
    }

    public List<HistoricoMensagemDTO> getHistorico() {
        return historico;
    }

    public void setHistorico(List<HistoricoMensagemDTO> historico) {
        this.historico = historico;
    }
}
