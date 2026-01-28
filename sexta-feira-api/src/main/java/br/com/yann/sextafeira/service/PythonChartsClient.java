package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.dto.ChartBudgetRequestDTO;
import br.com.yann.sextafeira.dto.ChartCategoriaRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PythonChartsClient {

    private final RestTemplate restTemplate;

    @Value("${ia.base-url:http://127.0.0.1:8000}")
    private String iaBaseUrl;

    public PythonChartsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] gerarGraficoGastosPorCategoria(ChartCategoriaRequestDTO req) {
        String url = iaBaseUrl + "/ia/charts/gastos-por-categoria";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChartCategoriaRequestDTO> entity = new HttpEntity<>(req, headers);

        ResponseEntity<byte[]> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                byte[].class
        );

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Falha ao gerar gráfico no serviço Python");
        }

        return resp.getBody();
    }

    public byte[] gerarGraficoOrcamentoVsGasto(ChartBudgetRequestDTO req) {
        String url = iaBaseUrl + "/ia/charts/orcamento-vs-gasto";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChartBudgetRequestDTO> entity = new HttpEntity<>(req, headers);

        ResponseEntity<byte[]> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                byte[].class
        );

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Falha ao gerar gráfico orçamento vs gasto no serviço Python");
        }

        return resp.getBody();
    }

    public byte[] gerarGraficoSerieLinha(br.com.yann.sextafeira.dto.ChartSerieRequestDTO req) {
        String url = iaBaseUrl + "/ia/charts/serie-linha";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<br.com.yann.sextafeira.dto.ChartSerieRequestDTO> entity = new HttpEntity<>(req, headers);

        ResponseEntity<byte[]> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                byte[].class
        );

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Falha ao gerar gráfico de série (linha) no serviço Python");
        }

        return resp.getBody();
    }

}
