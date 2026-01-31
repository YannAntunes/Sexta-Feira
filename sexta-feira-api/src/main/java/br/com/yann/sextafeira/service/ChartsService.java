package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import br.com.yann.sextafeira.dto.ChartBudgetItemDTO;
import br.com.yann.sextafeira.repository.TransacaoRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class ChartsService {

    private final TransacaoRepository transacaoRepository;
    private final OrcamentoService orcamentoService;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String IA_BASE = "http://localhost:8000";

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

        return soma.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        Map::putAll);
    }

    public List<ChartBudgetItemDTO> orcamentoVsGastoMesAtual() {
        YearMonth ym = YearMonth.from(LocalDate.now());

        var gastosMap = gastosPorCategoriaDoMesAtual();

        List<ChartBudgetItemDTO> items = new ArrayList<>();

        for (CategoriaTransacao cat : CategoriaTransacao.values()) {

            BigDecimal gasto = gastosMap.getOrDefault(cat.name(), BigDecimal.ZERO);

            var status = orcamentoService.consultarStatus(ym.getYear(), ym.getMonthValue(), cat);
            BigDecimal limite = status.getLimite();

            items.add(new ChartBudgetItemDTO(cat.name(), limite, gasto));
        }

        items.sort((a, b) -> b.getGasto().compareTo(a.getGasto()));
        return items;
    }

    public Map<LocalDate, BigDecimal> serieGastosDiariosMesAtual() {

        YearMonth ym = YearMonth.from(LocalDate.now());
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByDataBetween(inicio, fim);

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

    // ✅ NOVO: chama o Python e recebe a imagem PNG (bytes)
    public byte[] gerarSerieLinhaPng(Map<String, Object> payload) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

        ResponseEntity<byte[]> resp = restTemplate.exchange(
                IA_BASE + "/ia/charts/serie-linha",
                HttpMethod.POST,
                req,
                byte[].class
        );

        return resp.getBody();
    }
}
