package br.com.yann.sextafeira.modes.finance.controller;

import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.dto.ChartSerieItemDTO;
import br.com.yann.sextafeira.modes.finance.dto.ChartSerieRequestDTO;
import br.com.yann.sextafeira.modes.finance.dto.RankingContextDTO;
import br.com.yann.sextafeira.core.integration.python.PythonChartsClient;
import br.com.yann.sextafeira.modes.finance.service.SnapshotCarteiraService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioChartController {

    private final SnapshotCarteiraService snapshotCarteiraService;
    private final PythonChartsClient pythonChartsClient;

    public PortfolioChartController(SnapshotCarteiraService snapshotCarteiraService,
                                    PythonChartsClient pythonChartsClient) {
        this.snapshotCarteiraService = snapshotCarteiraService;
        this.pythonChartsClient = pythonChartsClient;
    }

    @GetMapping(value = "/api/v1/charts/portfolio/last", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> chartPortfolioLastRanking() {

        RankingContextDTO ctx = snapshotCarteiraService.getUltimoRankingContext();
        if (ctx == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Sem ranking recente. Rode: \"top 5 da carteira\" primeiro.".getBytes());
        }

        ClasseAtivo filtro = (ctx.filtro() == null) ? null : ClasseAtivo.valueOf(ctx.filtro());

        var dados = snapshotCarteiraService.evolucaoPatrimonio(ctx.inicio(), ctx.fim(), filtro);

        var items = dados.stream()
                .map(d -> new ChartSerieItemDTO(
                        d.getData().toString(),
                        d.getValor()
                ))
                .toList();

        String titulo = "Evolução da carteira (" + ctx.inicio() + " → " + ctx.fim() + ")" +
                (ctx.filtro() == null ? "" : " — " + ctx.filtro());

        ChartSerieRequestDTO req = new ChartSerieRequestDTO(titulo, "R$", items);

        byte[] png = pythonChartsClient.gerarGraficoSerieLinha(req);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
