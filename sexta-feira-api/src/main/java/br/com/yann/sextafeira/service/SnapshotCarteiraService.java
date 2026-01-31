package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.AtivoCarteira;
import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.domain.model.SnapshotCarteira;
import br.com.yann.sextafeira.dto.PatrimonioDiaDTO;
import br.com.yann.sextafeira.repository.SnapshotCarteiraRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SnapshotCarteiraService {

    private final SnapshotCarteiraRepository repo;
    private final CarteiraService carteiraService;
    private final CotacaoService cotacaoService;

    public SnapshotCarteiraService(SnapshotCarteiraRepository repo,
                                   CarteiraService carteiraService,
                                   CotacaoService cotacaoService) {
        this.repo = repo;
        this.carteiraService = carteiraService;
        this.cotacaoService = cotacaoService;
    }

    /** Cria/atualiza snapshot exatamente na data informada (replace do dia). */
    public BigDecimal gerarSnapshotDoDia(LocalDate data, ClasseAtivo filtro) {
        if (repo.existsByData(data)) {
            repo.deleteAll(repo.findByData(data));
        }

        List<AtivoCarteira> ativos = (filtro == null)
                ? carteiraService.listarTudo()
                : carteiraService.listarPorClasse(filtro);

        BigDecimal total = BigDecimal.ZERO;

        for (AtivoCarteira a : ativos) {
            BigDecimal preco = cotacaoService.cotacaoAtualSeguro(a.getTicker(), a.getClasse());
            BigDecimal valor = a.getQuantidade().multiply(preco).setScale(2, RoundingMode.HALF_UP);
            total = total.add(valor);

            repo.save(new SnapshotCarteira(
                    data,
                    a.getClasse(),
                    a.getTicker().toUpperCase(),
                    a.getQuantidade(),
                    preco,
                    valor
            ));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Retorna a data do snapshot mais recente <= alvo. Se não existir, null. */
    public LocalDate dataSnapshotMaisProxima(LocalDate alvo, ClasseAtivo filtro) {
        return (filtro == null
                ? repo.findTopByDataLessThanEqualOrderByDataDesc(alvo)
                : repo.findTopByClasseAndDataLessThanEqualOrderByDataDesc(filtro, alvo)
        ).map(SnapshotCarteira::getData).orElse(null);
    }

    /** Valor total do snapshot numa data EXATA (se não existir, 0). */
    public BigDecimal valorTotalNoDia(LocalDate data, ClasseAtivo filtro) {
        var lista = (filtro == null)
                ? repo.findByData(data)
                : repo.findByDataAndClasse(data, filtro);

        BigDecimal total = BigDecimal.ZERO;
        for (var s : lista) total = total.add(s.getValorBRL());

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Política B: se não existir snapshot anterior, cria HOJE e retorna HOJE. */
    public LocalDate garantirSnapshotAte(LocalDate alvo, ClasseAtivo filtro) {
        LocalDate data = dataSnapshotMaisProxima(alvo, filtro);
        if (data != null) return data;

        LocalDate hoje = LocalDate.now();
        gerarSnapshotDoDia(hoje, filtro);
        return hoje;
    }

    /** Série (data -> valor total) a partir de snapshots. */
    public List<PatrimonioDiaDTO> evolucaoPatrimonio(LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {

        var lista = (filtro == null)
                ? repo.findByDataBetween(inicio, fim)
                : repo.findByClasseAndDataBetween(filtro, inicio, fim);

        Map<LocalDate, BigDecimal> porDia = new HashMap<>();
        for (var s : lista) {
            porDia.merge(s.getData(), s.getValorBRL(), BigDecimal::add);
        }

        return porDia.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new PatrimonioDiaDTO(
                        e.getKey(),
                        e.getValue().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    public String topMelhoresEPioresNoPeriodo(LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {

        LocalDate d0 = garantirSnapshotAte(inicio, filtro);
        LocalDate d1 = garantirSnapshotAte(fim, filtro);

        if (d0.equals(d1)) {
            return "🏁 Top ativos:\nAinda não tenho snapshots suficientes pra ranquear o período. Eu não adivinho o passado. 😏\n";
        }

        var s0 = (filtro == null) ? repo.findByData(d0) : repo.findByDataAndClasse(d0, filtro);
        var s1 = (filtro == null) ? repo.findByData(d1) : repo.findByDataAndClasse(d1, filtro);

        java.util.Map<String, SnapshotCarteira> m0 = new java.util.HashMap<>();
        for (var s : s0) m0.put(s.getTicker(), s);

        java.util.Map<String, SnapshotCarteira> m1 = new java.util.HashMap<>();
        for (var s : s1) m1.put(s.getTicker(), s);

        java.util.List<RankItem> itens = new java.util.ArrayList<>();

        for (String ticker : m0.keySet()) {
            if (!m1.containsKey(ticker)) continue;

            var a0 = m0.get(ticker);
            var a1 = m1.get(ticker);

            BigDecimal v0 = a0.getValorBRL() == null ? BigDecimal.ZERO : a0.getValorBRL();
            BigDecimal v1 = a1.getValorBRL() == null ? BigDecimal.ZERO : a1.getValorBRL();

            // ignora ativo “zerado” nos dois lados
            if (v0.compareTo(BigDecimal.ZERO) == 0 && v1.compareTo(BigDecimal.ZERO) == 0) continue;

            itens.add(new RankItem(ticker, a1.getClasse(), v0, v1));
        }

        if (itens.isEmpty()) {
            return "🏁 Top ativos:\nNada comparável nesse período (sem interseção de snapshots). 😏\n";
        }

        // ordena por delta (R$)
        itens.sort((a, b) -> b.delta.compareTo(a.delta));

        var melhores = itens.stream().limit(3).toList();

        // piores = do final
        var piores = itens.stream()
                .sorted((a, b) -> a.delta.compareTo(b.delta))
                .limit(3)
                .toList();

        String filtroLabel = (filtro == null) ? "" : " — filtro: " + filtro.name();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🏁 Top ativos (snapshots %s → %s)%s\n", d0, d1, filtroLabel));

        sb.append("\n🥇 Melhores (top 3):\n");
        for (var it : melhores) {
            if (it.pct != null) {
                sb.append(String.format("- %s (%s): Δ R$ %.2f (%.2f%%)\n",
                        it.ticker, it.classe.name(), it.delta, it.pct));
            } else {
                sb.append(String.format("- %s (%s): Δ R$ %.2f\n",
                        it.ticker, it.classe.name(), it.delta));
            }
        }

        sb.append("\n💀 Piores (top 3):\n");
        for (var it : piores) {
            if (it.pct != null) {
                sb.append(String.format("- %s (%s): Δ R$ %.2f (%.2f%%)\n",
                        it.ticker, it.classe.name(), it.delta, it.pct));
            } else {
                sb.append(String.format("- %s (%s): Δ R$ %.2f\n",
                        it.ticker, it.classe.name(), it.delta));
            }
        }

        return sb.toString();
    }


    private static class RankItem {
        String ticker;
        ClasseAtivo classe;
        BigDecimal v0;
        BigDecimal v1;
        BigDecimal delta;
        BigDecimal pct;

        RankItem(String ticker, ClasseAtivo classe, BigDecimal v0, BigDecimal v1) {
            this.ticker = ticker;
            this.classe = classe;
            this.v0 = v0;
            this.v1 = v1;
            this.delta = v1.subtract(v0);

            if (v0.compareTo(BigDecimal.ZERO) > 0) {
                this.pct = this.delta
                        .divide(v0, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                this.pct = null; // sem base pra %
            }
        }
    }


}
