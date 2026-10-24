package br.com.yann.sextafeira.modes.finance.service.ai;

import br.com.yann.sextafeira.core.dto.HistoricoMensagemDTO;
import br.com.yann.sextafeira.core.dto.IaRouterResponse;
import br.com.yann.sextafeira.modes.finance.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.modes.finance.dto.IaTransacaoRequest;
import br.com.yann.sextafeira.modes.finance.dto.IaTransacaoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransacaoIaService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoIaService.class);

    private final RestTemplate restTemplate;

    @Value("${sexta.ia.url:http://127.0.0.1:8000}")
    private String iaBaseUrl;

    public TransacaoIaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Transacao interpretarMensagem(String mensagem) {
        IaTransacaoRequest request = new IaTransacaoRequest(mensagem);

        IaTransacaoResponse response;
        try {
            response = restTemplate.postForObject(
                    iaBaseUrl + "/ia/transacoes/interpretar",
                    request,
                    IaTransacaoResponse.class
            );
        } catch (RestClientException ex) {
            log.error("Falha de comunicacao com servico Python em /ia/transacoes/interpretar: {}", ex.getMessage());
            throw new IllegalStateException("Serviço de interpretação (IA) temporariamente indisponível.");
        }

        if (response == null || response.getData() == null || response.getTipo() == null || response.getCategoria() == null) {
            throw new IllegalStateException("Resposta inválida do serviço de interpretação (IA).");
        }

        Transacao transacao = new Transacao();
        transacao.setValor(response.getValor());
        transacao.setData(LocalDate.parse(response.getData()));
        transacao.setTipo(TipoTransacao.valueOf(response.getTipo().toUpperCase()));
        transacao.setCategoria(CategoriaTransacao.valueOf(response.getCategoria().toUpperCase()));
        transacao.setDescricao(response.getDescricao());

        return transacao;
    }

    public IaRouterResponse rotearMensagem(String mensagem) {
        return rotearMensagem(mensagem, null, null);
    }

    public IaRouterResponse rotearMensagem(String mensagem, String personalidade, List<HistoricoMensagemDTO> historico) {
        IaTransacaoRequest request = new IaTransacaoRequest(mensagem, personalidade, historico);

        try {
            IaRouterResponse response = restTemplate.postForObject(
                    iaBaseUrl + "/ia/router",
                    request,
                    IaRouterResponse.class
            );

            if (response == null || response.getIntent() == null) {
                throw new IllegalStateException("Resposta vazia ao rotear mensagem na IA.");
            }
            return response;
        } catch (RestClientException ex) {
            log.error("Falha de comunicacao com servico Python em /ia/router: {}", ex.getMessage());
            throw new IllegalStateException("Serviço de inteligência temporariamente indisponível.");
        }
    }
}
