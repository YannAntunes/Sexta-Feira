package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import br.com.yann.sextafeira.dto.ChartBudgetItemDTO;
import br.com.yann.sextafeira.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;


@Service
public class ChartsService {

    private final TransacaoRepository transacaoRepository;
    private final OrcamentoService orcamentoService;


    public ChartsService(TransacaoRepository repo, OrcamentoService orcamentoService) {
        this.transacaoRepository = repo;
        this.orcamentoService = orcamentoService;
    }

    public Map<String, BigDecimal> gastosPorCategoriaDoMesAtual() {
        YearMonth ym = YearMonth.from(LocalDate.now());
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByDataBetween(inicio, fim);

        Map<String, BigDecimal> soma = new HashMap<>();

        for (Transacao t : transacoes) {
            if (t.getTipo() != TipoTransacao.DESPESA) continue;

            String cat = t.getCategoria().name();
            soma.putIfAbsent(cat, BigDecimal.ZERO);
            soma.put(cat, soma.get(cat).add(t.getValor()));
        }

        // ordenar por maior gasto
        return soma.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        Map::putAll);
    }

    public List<ChartBudgetItemDTO> orcamentoVsGastoMesAtual() {
        YearMonth ym = YearMonth.from(LocalDate.now());

        // gasto por categoria (já ordenado desc)
        var gastosMap = gastosPorCategoriaDoMesAtual();

        List<ChartBudgetItemDTO> items = new ArrayList<>();

        // percorre todas as categorias (para aparecer mesmo as que não tem gasto)
        for (CategoriaTransacao cat : CategoriaTransacao.values()) {

            BigDecimal gasto = gastosMap.getOrDefault(cat.name(), BigDecimal.ZERO);

            // aqui eu uso seu orcamentoService; precisa estar injetado no ChartsService
            var status = orcamentoService.consultarStatus(ym.getYear(), ym.getMonthValue(), cat);
            BigDecimal limite = status.getLimite(); // se não existir, ajuste conforme seu DTO

            items.add(new ChartBudgetItemDTO(cat.name(), limite, gasto));
        }

        // opcional: ordenar por quem mais estourou (gasto/limite)
        items.sort((a, b) -> b.getGasto().compareTo(a.getGasto()));

        return items;
    }

    public Map<LocalDate, BigDecimal> serieGastosDiariosMesAtual() {

        YearMonth ym = YearMonth.from(LocalDate.now());
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByDataBetween(inicio, fim);

        // inicializa todos os dias com 0 (pra linha ficar contínua)
        Map<LocalDate, BigDecimal> serie = new LinkedHashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            serie.put(ym.atDay(d), BigDecimal.ZERO);
        }

        for (Transacao t : transacoes) {
            if (t.getTipo() != TipoTransacao.DESPESA) continue;

            LocalDate dia = t.getData();
            if (!serie.containsKey(dia)) continue;

            serie.put(dia, serie.get(dia).add(t.getValor()));
        }

        return serie;
    }

    public List<Map.Entry<String, BigDecimal>> topCategoriasGastoMesAtual(int topN) {
        var mapa = gastosPorCategoriaDoMesAtual();
        return mapa.entrySet().stream().limit(topN).toList();
    }

    public Map<String, BigDecimal> gastosPorCategoria(LocalDate inicio, LocalDate fim) {
        List<Transacao> transacoes = transacaoRepository.findByDataBetween(inicio, fim);

        Map<String, BigDecimal> soma = new HashMap<>();

        for (Transacao t : transacoes) {
            if (t.getTipo() != TipoTransacao.DESPESA) continue;

            String cat = t.getCategoria().name();
            soma.put(cat, soma.getOrDefault(cat, BigDecimal.ZERO).add(t.getValor()));
        }

        return soma;
    }
}
