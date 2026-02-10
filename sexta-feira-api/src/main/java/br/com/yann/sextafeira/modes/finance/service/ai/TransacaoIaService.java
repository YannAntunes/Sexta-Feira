package br.com.yann.sextafeira.modes.finance.service.ai;

import br.com.yann.sextafeira.modes.finance.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.core.dto.IaRouterResponse;
import br.com.yann.sextafeira.modes.finance.dto.IaTransacaoRequest;
import br.com.yann.sextafeira.modes.finance.dto.IaTransacaoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
public class TransacaoIaService {

    private final RestTemplate restTemplate;

    @Value("${sexta.ia.url}")
    private String iaBaseUrl;

    public TransacaoIaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Transacao interpretarMensagem(String mensagem) {
        IaTransacaoRequest request = new IaTransacaoRequest(mensagem);

        // Chama o serviço Python
        IaTransacaoResponse response = restTemplate.postForObject(
                iaBaseUrl + "/ia/transacoes/interpretar",
                request,
                IaTransacaoResponse.class
        );

        if (response == null) {
            throw new RuntimeException("Falha ao interpretar mensagem na IA");
        }

        Transacao transacao = new Transacao();
        transacao.setValor(response.getValor());

        // data vem como string "2026-01-17"
        transacao.setData(LocalDate.parse(response.getData()));

        // Converte strings pra enums
        transacao.setTipo(TipoTransacao.valueOf(response.getTipo().toUpperCase()));
        transacao.setCategoria(CategoriaTransacao.valueOf(response.getCategoria().toUpperCase()));

        transacao.setDescricao(response.getDescricao());

        return transacao;
    }

    public IaRouterResponse rotearMensagem(String mensagem) {
        IaTransacaoRequest request = new IaTransacaoRequest(mensagem);

        IaRouterResponse response = restTemplate.postForObject(
                iaBaseUrl + "/ia/router",
                request,
                IaRouterResponse.class
        );

        if (response == null) throw new RuntimeException("Falha ao rotear mensagem na IA");
        return response;
    }


}
