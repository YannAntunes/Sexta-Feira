package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.dto.ChartCategoriaItemDTO;
import br.com.yann.sextafeira.dto.ChartCategoriaRequestDTO;
import br.com.yann.sextafeira.service.ChartsService;
import br.com.yann.sextafeira.service.PythonChartsClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/charts")
public class ChartsController {

    private final ChartsService chartsService;
    private final PythonChartsClient pythonChartsClient;

    public ChartsController(ChartsService chartsService, PythonChartsClient pythonChartsClient) {
        this.chartsService = chartsService;
        this.pythonChartsClient = pythonChartsClient;
    }

    @GetMapping(value = "/gastos-mes.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> gastosMesPng() {
        var mapa = chartsService.gastosPorCategoriaDoMesAtual();

        List<ChartCategoriaItemDTO> items = mapa.entrySet().stream()
                .map(e -> new ChartCategoriaItemDTO(e.getKey(), e.getValue()))
                .toList();

        YearMonth ym = YearMonth.from(LocalDate.now());
        String titulo = "Gastos por categoria - " + String.format("%02d/%d", ym.getMonthValue(), ym.getYear());

        var req = new ChartCategoriaRequestDTO(titulo, items);

        byte[] png = pythonChartsClient.gerarGraficoGastosPorCategoria(req);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @GetMapping(value = "/orcamento-vs-gasto.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> orcamentoVsGastoPng() {

        var items = chartsService.orcamentoVsGastoMesAtual();

        YearMonth ym = YearMonth.from(LocalDate.now());
        String titulo = "Orçamento vs Gasto - " + String.format("%02d/%d", ym.getMonthValue(), ym.getYear());

        var req = new br.com.yann.sextafeira.dto.ChartBudgetRequestDTO(titulo, items);

        byte[] png = pythonChartsClient.gerarGraficoOrcamentoVsGasto(req);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @GetMapping(value = "/evolucao-gastos-mes.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> evolucaoGastosMesPng() {

        var serie = chartsService.serieGastosDiariosMesAtual();

        var items = serie.entrySet().stream()
                .map(e -> new br.com.yann.sextafeira.dto.ChartSerieItemDTO(
                        e.getKey().toString(), // "2026-01-28"
                        e.getValue()
                ))
                .toList();

        var ym = java.time.YearMonth.from(java.time.LocalDate.now());
        String titulo = "Evolução de gastos - " + String.format("%02d/%d", ym.getMonthValue(), ym.getYear());

        var req = new br.com.yann.sextafeira.dto.ChartSerieRequestDTO(titulo, "R$", items);

        byte[] png = pythonChartsClient.gerarGraficoSerieLinha(req);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }


}
