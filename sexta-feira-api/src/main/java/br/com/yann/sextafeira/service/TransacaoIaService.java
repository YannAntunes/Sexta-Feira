package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import br.com.yann.sextafeira.dto.IaTransacaoRequest;
import br.com.yann.sextafeira.dto.IaTransacaoResponse;
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
}
