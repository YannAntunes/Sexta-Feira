package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.modes.finance.domain.model.AlertaOrcamento;
import br.com.yann.sextafeira.modes.finance.dto.RelatorioMensalResponse;
import br.com.yann.sextafeira.modes.finance.dto.ResumoMensalDTO;
import br.com.yann.sextafeira.modes.finance.domain.repository.AlertaOrcamentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RelatorioService {

    private final TransacaoService transacaoService;
    private final ChartsService chartsService;
    private final AlertaOrcamentoRepository alertaRepo;

    public RelatorioService(TransacaoService transacaoService,
                            ChartsService chartsService,
                            AlertaOrcamentoRepository alertaRepo) {
        this.transacaoService = transacaoService;
        this.chartsService = chartsService;
        this.alertaRepo = alertaRepo;
    }

    public RelatorioMensalResponse gerarRelatorioMesAtual() {

        YearMonth ym = YearMonth.from(LocalDate.now());
        int ano = ym.getYear();
        int mes = ym.getMonthValue();

        ResumoMensalDTO resumo = transacaoService.calcularResumoMensal(ano, mes);

        String mesAno = String.format("%02d/%d", mes, ano);

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

        // Top categorias (3) - AQUI o key é String mesmo
        var top = chartsService.topCategoriasGastoMesAtual(3);
        List<String> topCategorias = new ArrayList<>();
        for (var e : top) {
            String catRaw = e.getKey(); // String tipo "ALIMENTACAO"
            topCategorias.add(String.format("%s: R$ %.2f", formatarCategoria(catRaw), e.getValue()));
        }
        if (topCategorias.isEmpty()) {
            topCategorias.add("Sem gastos registrados no mês (milagre raro).");
        }

        // Alertas reais do banco (já enviados pelo monitoramento)
        List<AlertaOrcamento> alertasDb = alertaRepo.findByAnoAndMes(ano, mes);
        alertasDb.sort(Comparator.comparing(AlertaOrcamento::getEnviadoEm).reversed());

        List<String> alertas = new ArrayList<>();
        for (AlertaOrcamento a : alertasDb) {
            String cat = formatarCategoria(a.getCategoria().name());

            switch (a.getTipoAlerta()) {
                case "ESTOURO" -> alertas.add("🚨 " + cat + " estourou o orçamento.");
                case "PERCENT_90" -> alertas.add("🔥 " + cat + " passou de 90% do orçamento.");
                case "PERCENT_80" -> alertas.add("⚠️ " + cat + " passou de 80% do orçamento.");
                default -> alertas.add("🚦 " + cat + " alerta: " + a.getTipoAlerta());
            }
        }
        if (alertas.isEmpty()) {
            alertas.add("Sem alertas no orçamento. Continue fingindo que é adulto responsável. 😉");
        }

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
