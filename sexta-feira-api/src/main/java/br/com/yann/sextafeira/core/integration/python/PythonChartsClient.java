package br.com.yann.sextafeira.core.integration.python;

import br.com.yann.sextafeira.modes.finance.dto.ChartBudgetRequestDTO;
import br.com.yann.sextafeira.modes.finance.dto.ChartCategoriaRequestDTO;
import br.com.yann.sextafeira.modes.finance.dto.ChartSerieRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PythonChartsClient {

    private static final Logger log = LoggerFactory.getLogger(PythonChartsClient.class);

    private final RestTemplate restTemplate;

    @Value("${sexta.ia.url:http://127.0.0.1:8000}")
    private String iaBaseUrl;

    public PythonChartsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] gerarGraficoGastosPorCategoria(ChartCategoriaRequestDTO req) {
        return postForPng("/ia/charts/gastos-por-categoria", req, "Falha ao gerar gráfico de gastos por categoria");
    }

    public byte[] gerarGraficoOrcamentoVsGasto(ChartBudgetRequestDTO req) {
        return postForPng("/ia/charts/orcamento-vs-gasto", req, "Falha ao gerar gráfico de orçamento vs gasto");
    }

    public byte[] gerarGraficoSerieLinha(ChartSerieRequestDTO req) {
        return postForPng("/ia/charts/serie-linha", req, "Falha ao gerar gráfico de série (linha)");
    }

    private <T> byte[] postForPng(String path, T payload, String erroMsg) {
        String url = iaBaseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException(erroMsg + " no serviço Python.");
            }
            return resp.getBody();
        } catch (RestClientException ex) {
            log.error("{}: {}", erroMsg, ex.getMessage());
            throw new IllegalStateException(erroMsg + ": serviço Python indisponível.");
        }
    }
}
