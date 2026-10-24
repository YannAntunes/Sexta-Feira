package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.modes.finance.dto.BalancoMensalDTO;
import br.com.yann.sextafeira.modes.finance.dto.ChartBudgetItemDTO;
import br.com.yann.sextafeira.modes.finance.domain.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Value;
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
    private final RestTemplate restTemplate;
    private final UsuarioService usuarioService;

    @Value("${sexta.ia.url:http://127.0.0.1:8000}")
    private String iaBaseUrl;

    public ChartsService(TransacaoRepository repo, OrcamentoService orcamentoService, RestTemplate restTemplate,
                         UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        this.transacaoRepository = repo;
        this.orcamentoService = orcamentoService;
        this.restTemplate = restTemplate;
    }

    public Map<String, BigDecimal> gastosPorCategoriaDoMesAtual() {
        YearMonth ym = YearMonth.from(LocalDate.now());
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuarioService.obterUsuarioAtual(), inicio, fim);

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

        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuarioService.obterUsuarioAtual(), inicio, fim);

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
        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuarioService.obterUsuarioAtual(), inicio, fim);

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

        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    iaBaseUrl + "/ia/charts/serie-linha",
                    HttpMethod.POST,
                    req,
                    byte[].class
            );
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("Falha ao gerar gráfico de série no serviço Python.");
            }
            return resp.getBody();
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new IllegalStateException("Serviço Python de gráficos indisponível.");
        }
    }
    public Map<String, BigDecimal> receitasPorCategoriaDoMesAtual() {
        YearMonth ym = YearMonth.from(LocalDate.now());
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuarioService.obterUsuarioAtual(), inicio, fim);

        Map<String, BigDecimal> soma = new HashMap<>();

        for (Transacao t : transacoes) {
            if (t.getTipo() != TipoTransacao.RECEITA) continue;

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

    public BalancoMensalDTO balancoReceitasVsDespesasMesAtual() {
        YearMonth ym = YearMonth.from(LocalDate.now());
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuarioService.obterUsuarioAtual(), inicio, fim);

        BigDecimal totalReceitas = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;

        for (Transacao t : transacoes) {
            if (t.getTipo() == TipoTransacao.RECEITA) {
                totalReceitas = totalReceitas.add(t.getValor());
            } else if (t.getTipo() == TipoTransacao.DESPESA) {
                totalDespesas = totalDespesas.add(t.getValor());
            }
        }

        BigDecimal saldo = totalReceitas.subtract(totalDespesas);
        BigDecimal taxaPoupanca = BigDecimal.ZERO;
        if (totalReceitas.compareTo(BigDecimal.ZERO) > 0) {
            taxaPoupanca = saldo.max(BigDecimal.ZERO)
                    .divide(totalReceitas, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, java.math.RoundingMode.HALF_UP);
        }

        String mesAno = String.format("%02d/%d", ym.getMonthValue(), ym.getYear());
        return new BalancoMensalDTO(totalReceitas, totalDespesas, saldo, taxaPoupanca, transacoes.size(), mesAno);
    }
}
