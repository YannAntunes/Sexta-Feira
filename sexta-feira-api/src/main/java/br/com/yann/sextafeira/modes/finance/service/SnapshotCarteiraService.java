package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.repository.UsuarioRepository;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.AtivoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.domain.model.SnapshotCarteira;
import br.com.yann.sextafeira.modes.finance.dto.PatrimonioDiaDTO;
import br.com.yann.sextafeira.modes.finance.dto.RankingContextDTO;
import br.com.yann.sextafeira.modes.finance.domain.repository.SnapshotCarteiraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SnapshotCarteiraService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotCarteiraService.class);

    private final SnapshotCarteiraRepository repo;
    private final CarteiraService carteiraService;
    private final CotacaoService cotacaoService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    // [CONC-01] Contexto do último ranking isolado por usuário (antes era um campo mutável global do singleton)
    private final Map<Long, RankingContextDTO> ultimoRankingPorUsuario = new ConcurrentHashMap<>();

    public SnapshotCarteiraService(SnapshotCarteiraRepository repo,
                                   CarteiraService carteiraService,
                                   CotacaoService cotacaoService,
                                   UsuarioService usuarioService,
                                   UsuarioRepository usuarioRepository) {
        this.repo = repo;
        this.carteiraService = carteiraService;
        this.cotacaoService = cotacaoService;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    public RankingContextDTO getUltimoRankingContext() {
        return getUltimoRankingContext(usuarioService.obterUsuarioAtual());
    }

    public RankingContextDTO getUltimoRankingContext(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) return null;
        return ultimoRankingPorUsuario.get(usuario.getId());
    }

    /** Gera o snapshot diário para todos os usuários. Falha de um usuário não interrompe os demais. */
    public void gerarSnapshotDiarioTodosUsuarios(LocalDate data) {
        for (Usuario usuario : usuarioRepository.findAll()) {
            try {
                gerarSnapshotDoDia(usuario, data, null);
            } catch (Exception ex) {
                log.warn("Falha ao gerar snapshot do usuário id={}: {}", usuario.getId(), ex.getMessage());
            }
        }
    }

    /** Cria/atualiza snapshot exatamente na data informada (replace do dia). */
    public BigDecimal gerarSnapshotDoDia(LocalDate data, ClasseAtivo filtro) {
        return gerarSnapshotDoDia(usuarioService.obterUsuarioAtual(), data, filtro);
    }

    @Transactional
    public BigDecimal gerarSnapshotDoDia(Usuario usuario, LocalDate data, ClasseAtivo filtro) {
        if (repo.existsByUsuarioAndData(usuario, data)) {
            repo.deleteAll(repo.findByUsuarioAndData(usuario, data));
            repo.flush();
        }

        List<AtivoCarteira> ativos = (filtro == null)
                ? carteiraService.listarTudo(usuario)
                : carteiraService.listarPorClasse(usuario, filtro);

        BigDecimal total = BigDecimal.ZERO;

        for (AtivoCarteira a : ativos) {
            BigDecimal preco = cotacaoService.cotacaoAtualSeguro(a.getTicker(), a.getClasse());
            BigDecimal valor = a.getQuantidade().multiply(preco).setScale(2, RoundingMode.HALF_UP);
            total = total.add(valor);

            repo.save(new SnapshotCarteira(
                    usuario,
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
        return dataSnapshotMaisProxima(usuarioService.obterUsuarioAtual(), alvo, filtro);
    }

    public LocalDate dataSnapshotMaisProxima(Usuario usuario, LocalDate alvo, ClasseAtivo filtro) {
        return (filtro == null
                ? repo.findTopByUsuarioAndDataLessThanEqualOrderByDataDesc(usuario, alvo)
                : repo.findTopByUsuarioAndClasseAndDataLessThanEqualOrderByDataDesc(usuario, filtro, alvo)
        ).map(SnapshotCarteira::getData).orElse(null);
    }

    /** Valor total do snapshot numa data EXATA (se não existir, 0). */
    public BigDecimal valorTotalNoDia(LocalDate data, ClasseAtivo filtro) {
        return valorTotalNoDia(usuarioService.obterUsuarioAtual(), data, filtro);
    }

    public BigDecimal valorTotalNoDia(Usuario usuario, LocalDate data, ClasseAtivo filtro) {
        var lista = (filtro == null)
                ? repo.findByUsuarioAndData(usuario, data)
                : repo.findByUsuarioAndDataAndClasse(usuario, data, filtro);

        BigDecimal total = BigDecimal.ZERO;
        for (var s : lista) total = total.add(s.getValorBRL());

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Política B: se não existir snapshot anterior, cria HOJE e retorna HOJE. */
    public LocalDate garantirSnapshotAte(LocalDate alvo, ClasseAtivo filtro) {
        return garantirSnapshotAte(usuarioService.obterUsuarioAtual(), alvo, filtro);
    }

    public LocalDate garantirSnapshotAte(Usuario usuario, LocalDate alvo, ClasseAtivo filtro) {
        LocalDate data = dataSnapshotMaisProxima(usuario, alvo, filtro);
        if (data != null) return data;

        LocalDate hoje = LocalDate.now();
        gerarSnapshotDoDia(usuario, hoje, filtro);
        return hoje;
    }

    /** Série (data -> valor total) a partir de snapshots. */
    public List<PatrimonioDiaDTO> evolucaoPatrimonio(LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {
        return evolucaoPatrimonio(usuarioService.obterUsuarioAtual(), inicio, fim, filtro);
    }

    public List<PatrimonioDiaDTO> evolucaoPatrimonio(Usuario usuario, LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {

        var lista = (filtro == null)
                ? repo.findByUsuarioAndDataBetween(usuario, inicio, fim)
                : repo.findByUsuarioAndClasseAndDataBetween(usuario, filtro, inicio, fim);

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
        return topMelhoresEPioresNoPeriodo(inicio, fim, filtro, 3); // default
    }

    public String topMelhoresEPioresNoPeriodo(LocalDate inicio, LocalDate fim, ClasseAtivo filtro, int topN) {
        return topMelhoresEPioresNoPeriodo(usuarioService.obterUsuarioAtual(), inicio, fim, filtro, topN);
    }

    public String topMelhoresEPioresNoPeriodo(Usuario usuario, LocalDate inicio, LocalDate fim, ClasseAtivo filtro, int topN) {

        // saneamento
        if (topN < 1) topN = 1;
        if (topN > 20) topN = 20;

        LocalDate d0 = garantirSnapshotAte(usuario, inicio, filtro);
        LocalDate d1 = garantirSnapshotAte(usuario, fim, filtro);

        // guarda contexto pro gráfico depois (por usuário)
        if (usuario != null && usuario.getId() != null) {
            ultimoRankingPorUsuario.put(usuario.getId(), new RankingContextDTO(
                    d0,
                    d1,
                    filtro == null ? null : filtro.name()
            ));
        }

        if (d0.equals(d1)) {
            return "🏁 Ranking:\nAinda não tenho snapshots suficientes pra ranquear o período. Eu não adivinho o passado. 😏\n";
        }

        var s0 = (filtro == null) ? repo.findByUsuarioAndData(usuario, d0) : repo.findByUsuarioAndDataAndClasse(usuario, d0, filtro);
        var s1 = (filtro == null) ? repo.findByUsuarioAndData(usuario, d1) : repo.findByUsuarioAndDataAndClasse(usuario, d1, filtro);

        Map<String, SnapshotCarteira> m0 = new HashMap<>();
        for (var s : s0) m0.put(s.getTicker(), s);

        Map<String, SnapshotCarteira> m1 = new HashMap<>();
        for (var s : s1) m1.put(s.getTicker(), s);

        List<RankItem> itens = new java.util.ArrayList<>();

        for (String ticker : m0.keySet()) {
            if (!m1.containsKey(ticker)) continue;

            var a0 = m0.get(ticker);
            var a1 = m1.get(ticker);

            BigDecimal v0 = a0.getValorBRL() == null ? BigDecimal.ZERO : a0.getValorBRL();
            BigDecimal v1 = a1.getValorBRL() == null ? BigDecimal.ZERO : a1.getValorBRL();

            // ignora ativo zerado nos dois lados
            if (v0.compareTo(BigDecimal.ZERO) == 0 && v1.compareTo(BigDecimal.ZERO) == 0) continue;

            itens.add(new RankItem(ticker, a1.getClasse(), v0, v1));
        }

        if (itens.isEmpty()) {
            return "🏁 Ranking:\nNada comparável nesse período (sem interseção de snapshots). 😏\n";
        }

        // ordena por delta desc
        itens.sort((a, b) -> b.delta.compareTo(a.delta));

        int n = Math.min(topN, itens.size());

        // MELHORES
        List<RankItem> melhores = itens.stream().limit(n).toList();

        // set de tickers usados nos melhores (pra não repetir)
        java.util.Set<String> usados = new java.util.HashSet<>();
        for (var it : melhores) usados.add(it.ticker);

        // PIORES (ignorando melhores)
        List<RankItem> piores = itens.stream()
                .sorted((a, b) -> a.delta.compareTo(b.delta))
                .filter(it -> !usados.contains(it.ticker))
                .limit(n)
                .toList();

        String filtroLabel = (filtro == null) ? "" : " — filtro: " + filtro.name();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🏁 Ranking (snapshots %s → %s)%s\n", d0, d1, filtroLabel));

        sb.append("\n🥇 Melhores (top ").append(melhores.size()).append("):\n");
        for (var it : melhores) appendItem(sb, it);

        sb.append("\n💀 Piores (top ").append(piores.size()).append("):\n");
        if (piores.isEmpty()) {
            sb.append("- Sem piores distintos (poucos ativos no período).\n");
        } else {
            for (var it : piores) appendItem(sb, it);
        }

        return sb.toString();
    }

    private void appendItem(StringBuilder sb, RankItem it) {
        if (it.pct != null) {
            sb.append(String.format("- %s (%s): Δ R$ %.2f (%.2f%%)\n",
                    it.ticker, it.classe.name(), it.delta, it.pct));
        } else {
            sb.append(String.format("- %s (%s): Δ R$ %.2f\n",
                    it.ticker, it.classe.name(), it.delta));
        }
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
