package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.dto.RelatorioMensalResponse;
import br.com.yann.sextafeira.dto.ResumoMensalDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class RelatorioService {

    private final TransacaoService transacaoService;
    private final ChartsService chartsService;
    private final OrcamentoService orcamentoService;

    public RelatorioService(TransacaoService transacaoService,
                            ChartsService chartsService,
                            OrcamentoService orcamentoService) {
        this.transacaoService = transacaoService;
        this.chartsService = chartsService;
        this.orcamentoService = orcamentoService;
    }

    public RelatorioMensalResponse gerarRelatorioMesAtual() {

        YearMonth ym = YearMonth.from(LocalDate.now());
        int ano = ym.getYear();
        int mes = ym.getMonthValue();

        ResumoMensalDTO resumo = transacaoService.calcularResumoMensal(ano, mes);

        String mesAno = String.format("%02d/%d", mes, ano);

        // Texto principal (SEXTÊIRA mode B)
        String humor = resumo.getSaldo().signum() >= 0
                ? "Você tá no azul. Por favor, não estraga isso."
                : "Você tá no vermelho. A matemática não perdoa, só observa.";

        String resumoTexto = String.format(
                "📌 Relatório %s\n\n" +
                        "Despesas: R$ %.2f\n" +
                        "Receitas: R$ %.2f\n" +
                        "Saldo: R$ %.2f\n\n" +
                        "Tradução: %s 😌",
                mesAno,
                resumo.getTotalDespesas(),
                resumo.getTotalReceitas(),
                resumo.getSaldo(),
                humor
        );

        // Top categorias (3)
        var top = chartsService.topCategoriasGastoMesAtual(3);
        List<String> topCategorias = new ArrayList<>();
        for (var e : top) {
            topCategorias.add(String.format("%s: R$ %.2f", formatarCategoria(e.getKey()), e.getValue()));
        }
        if (topCategorias.isEmpty()) {
            topCategorias.add("Sem gastos registrados no mês (milagre raro).");
        }

        // Alertas de orçamento
        List<String> alertas = new ArrayList<>();
        for (CategoriaTransacao cat : CategoriaTransacao.values()) {

            var status = orcamentoService.consultarStatus(ano, mes, cat);
            BigDecimal limite = status.getLimite();
            BigDecimal gasto = status.getGasto();

            if (limite == null || limite.compareTo(BigDecimal.ZERO) <= 0) continue; // sem orçamento definido

            if (status.isEstourado()) {
                BigDecimal excedente = gasto.subtract(limite);
                alertas.add(String.format("🚨 %s estourou: +R$ %.2f", formatarCategoria(cat.name()), excedente));
                continue;
            }

            BigDecimal perc = gasto.divide(limite, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            if (perc.compareTo(BigDecimal.valueOf(80)) >= 0) {
                alertas.add(String.format("⚠️ %s em %.0f%% do orçamento", formatarCategoria(cat.name()), perc));
            }
        }
        if (alertas.isEmpty()) {
            alertas.add("Sem alertas no orçamento. Continue fingindo que é adulto responsável. 😉");
        }

        // Links (seu host é localhost; se depois tiver front, muda fácil)
        List<String> links = List.of(
                "http://localhost:8080/api/v1/charts/gastos-mes.png",
                "http://localhost:8080/api/v1/charts/orcamento-vs-gasto.png",
                "http://localhost:8080/api/v1/charts/evolucao-gastos-mes.png"
        );

        return new RelatorioMensalResponse(mesAno, resumoTexto, alertas, topCategorias, links);
    }

    private String formatarCategoria(String categoriaEnum) {
        String s = categoriaEnum.toLowerCase().replace("_", " ");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
