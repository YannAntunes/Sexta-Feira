package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.domain.model.MovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoMovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.repository.MovimentoCarteiraRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class CarteiraPerformanceService {

    private final MovimentoCarteiraRepository movRepo;
    private final CarteiraService carteiraService;
    private final CotacaoService cotacaoService;
    private final SnapshotCarteiraService snapshotCarteiraService;
    private final UsuarioService usuarioService;

    public CarteiraPerformanceService(MovimentoCarteiraRepository movRepo,
                                      CarteiraService carteiraService,
                                      CotacaoService cotacaoService,
                                      SnapshotCarteiraService snapshotCarteiraService,
                                      UsuarioService usuarioService) {
        this.movRepo = movRepo;
        this.carteiraService = carteiraService;
        this.cotacaoService = cotacaoService;
        this.snapshotCarteiraService = snapshotCarteiraService;
        this.usuarioService = usuarioService;
    }

    public String gerarRelatorioPeriodo(LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {
        return gerarRelatorioPeriodo(usuarioService.obterUsuarioAtual(), inicio, fim, filtro);
    }

    public String gerarRelatorioPeriodo(Usuario usuario, LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {

        List<MovimentoCarteira> movs = (filtro == null)
                ? movRepo.findByUsuarioAndDataBetweenOrderByDataAscIdAsc(usuario, inicio, fim)
                : movRepo.findByUsuarioAndClasseAndDataBetweenOrderByDataAscIdAsc(usuario, filtro, inicio, fim);

        BigDecimal aportes = BigDecimal.ZERO;
        BigDecimal compras = BigDecimal.ZERO;
        BigDecimal vendas = BigDecimal.ZERO;

        for (MovimentoCarteira m : movs) {
            BigDecimal v = m.getValorBRL() == null ? BigDecimal.ZERO : m.getValorBRL();

            if (m.getTipo() == TipoMovimentoCarteira.APORTE_BRL) {
                aportes = aportes.add(v);
            } else if (m.getTipo() == TipoMovimentoCarteira.COMPRA) {
                compras = compras.add(v);
            } else if (m.getTipo() == TipoMovimentoCarteira.VENDA) {
                vendas = vendas.add(v);
            }
        }

        // saldo/fluxo do período (pragmático)
        BigDecimal saldoPeriodo = aportes
                .subtract(compras)
                .add(vendas)
                .setScale(2, RoundingMode.HALF_UP);

        String filtroLabel = (filtro == null) ? "" : " — filtro: " + filtro.name();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📊 Carteira (%s → %s)%s\n\n", inicio, fim, filtroLabel));

        if (movs.isEmpty()) {
            sb.append("Nenhuma movimentação no período. Parabéns por não mexer no que não entende. 😏\n");
            return sb.toString();
        }

        sb.append("🧾 Movimentação:\n");
        sb.append(String.format("- Aportes: R$ %.2f\n", aportes));
        sb.append(String.format("- Compras: R$ %.2f\n", compras));
        sb.append(String.format("- Vendas:  R$ %.2f\n", vendas));
        sb.append(String.format("- Saldo do período: R$ %.2f\n", saldoPeriodo));

        return sb.toString();
    }

    public String gerarRentabilidadePeriodo(LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {
        return gerarRentabilidadePeriodo(usuarioService.obterUsuarioAtual(), inicio, fim, filtro);
    }

    public String gerarRentabilidadePeriodo(Usuario usuario, LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {

        // Política B: usa o snapshot mais próximo <= data; se não houver, cria um snapshot HOJE.
        LocalDate d0 = snapshotCarteiraService.garantirSnapshotAte(usuario, inicio, filtro);
        LocalDate d1 = snapshotCarteiraService.garantirSnapshotAte(usuario, fim, filtro);

        BigDecimal v0 = snapshotCarteiraService.valorTotalNoDia(usuario, d0, filtro);
        BigDecimal v1 = snapshotCarteiraService.valorTotalNoDia(usuario, d1, filtro);

        // aportes entre (d0, d1]  -> começa no dia seguinte ao d0
        BigDecimal aportes = somarAportes(usuario, filtro, d0.plusDays(1), d1);

        if (v0 == null) v0 = BigDecimal.ZERO;
        if (v1 == null) v1 = BigDecimal.ZERO;

        BigDecimal lucro = v1.subtract(v0).subtract(aportes);
        BigDecimal rentPct = rentabilidadePct(lucro, v0.add(aportes));

        return String.format(
                "📈 Rentabilidade (snapshots %s → %s)%s\n\n" +
                        "• Valor inicial (V0): R$ %.2f\n" +
                        "• Aportes (A):        R$ %.2f\n" +
                        "• Valor final (V1):   R$ %.2f\n\n" +
                        "✅ Lucro/Prejuízo: R$ %.2f\n" +
                        "✅ Rentabilidade:  %.2f%%\n",
                d0, d1,
                (filtro == null ? "" : " — filtro: " + filtro.name()),
                v0, aportes, v1,
                lucro, rentPct
        );
    }

    public String gerarBlocoRentabilidade(LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {
        return gerarBlocoRentabilidade(usuarioService.obterUsuarioAtual(), inicio, fim, filtro);
    }

    public String gerarBlocoRentabilidade(Usuario usuario, LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {

        // 1) snapshot mais próximo <= início/fim
        LocalDate d0 = snapshotCarteiraService.dataSnapshotMaisProxima(usuario, inicio, filtro);
        LocalDate d1 = snapshotCarteiraService.dataSnapshotMaisProxima(usuario, fim, filtro);

        // 2) política B: se faltar snapshot, cria HOJE (data real de captura)
        LocalDate hoje = LocalDate.now();

        if (d1 == null) {
            snapshotCarteiraService.gerarSnapshotDoDia(usuario, hoje, filtro);
            d1 = hoje;
        }

        if (d0 == null) {
            // se não tinha nenhum snapshot anterior, baseline também vira "hoje"
            // (só gera de novo se o de cima não tiver sido gerado)
            if (!hoje.equals(d1)) snapshotCarteiraService.gerarSnapshotDoDia(usuario, hoje, filtro);
            d0 = hoje;
        }

        String filtroLabel = (filtro == null) ? "" : " — filtro: " + filtro.name();

        // 3) Se d0 == d1, não existe histórico suficiente pra calcular período
        if (d0.equals(d1)) {
            return String.format(
                    "📈 Rentabilidade (snapshots %s → %s)%s\n" +
                            "Eu até posso fazer mágica, mas voltar no tempo não está habilitado. 😏\n" +
                            "Ainda não tenho snapshots suficientes pra calcular esse período.\n" +
                            "Dica pragmática: depois que você usar por alguns dias, eu calculo isso bonitinho.\n",
                    d0, d1, filtroLabel
            );
        }

        // 4) Totais dos snapshots
        BigDecimal v0 = snapshotCarteiraService.valorTotalNoDia(usuario, d0, filtro);
        BigDecimal v1 = snapshotCarteiraService.valorTotalNoDia(usuario, d1, filtro);

        // 5) Aportes entre (d0, d1] — começa no dia seguinte ao snapshot inicial usado
        BigDecimal aportes = somarAportes(usuario, filtro, d0.plusDays(1), d1);

        // 6) Lucro aprox = V1 - V0 - Aportes
        BigDecimal lucro = v1.subtract(v0).subtract(aportes);

        // 7) Rentabilidade % aprox = lucro / (V0 + aportes)
        BigDecimal rentPct = rentabilidadePct(lucro, v0.add(aportes));

        return String.format(
                "📈 Rentabilidade (snapshots %s → %s)%s\n" +
                        "• Valor inicial:   R$ %.2f\n" +
                        "• Aportes:         R$ %.2f\n" +
                        "• Valor final:     R$ %.2f\n" +
                        "• Lucro/Prejuízo:  R$ %.2f\n" +
                        "• Rentabilidade:   %.2f%%\n" +
                        "\nObs.: isso usa snapshots reais (datas acima). Sem histórico, sem milagre.\n",
                d0, d1, filtroLabel,
                v0, aportes, v1, lucro, rentPct
        );
    }

    public String detalharAtivoNoPeriodo(LocalDate inicio, LocalDate fim, ClasseAtivo classe, String ticker) {
        return detalharAtivoNoPeriodo(usuarioService.obterUsuarioAtual(), inicio, fim, classe, ticker);
    }

    public String detalharAtivoNoPeriodo(Usuario usuario, LocalDate inicio, LocalDate fim, ClasseAtivo classe, String ticker) {

        ticker = ticker.toUpperCase();

        var movs = movRepo.findByUsuarioAndClasseAndTickerAndDataBetweenOrderByDataAscIdAsc(usuario, classe, ticker, inicio, fim);

        BigDecimal aportes = BigDecimal.ZERO;
        BigDecimal compras = BigDecimal.ZERO;
        BigDecimal vendas = BigDecimal.ZERO;

        BigDecimal qtdComprada = BigDecimal.ZERO;
        BigDecimal qtdVendida = BigDecimal.ZERO;
        BigDecimal qtdAporte = BigDecimal.ZERO;

        for (var m : movs) {
            BigDecimal v = m.getValorBRL() == null ? BigDecimal.ZERO : m.getValorBRL();
            BigDecimal q = m.getQuantidade() == null ? BigDecimal.ZERO : m.getQuantidade();

            switch (m.getTipo()) {
                case APORTE_BRL -> { aportes = aportes.add(v); qtdAporte = qtdAporte.add(q); }
                case COMPRA -> { compras = compras.add(v); qtdComprada = qtdComprada.add(q); }
                case VENDA -> { vendas = vendas.add(v); qtdVendida = qtdVendida.add(q); }
            }
        }

        BigDecimal saldoPeriodo = aportes.subtract(compras).add(vendas).setScale(2, RoundingMode.HALF_UP);

        // posição atual do ativo
        BigDecimal qtdAtual = carteiraService.quantidadeAtual(usuario, classe, ticker);
        BigDecimal precoAtual = cotacaoService.cotacaoAtualSeguro(ticker, classe);
        BigDecimal valorAtual = qtdAtual.multiply(precoAtual).setScale(2, RoundingMode.HALF_UP);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📌 %s (%s) — %s → %s\n\n", ticker, classe.name(), inicio, fim));

        if (movs.isEmpty()) {
            sb.append("Nenhum movimento no período. O ativo só existiu e você ficou olhando. 😏\n\n");
        } else {
            sb.append("🧾 Movimentação no período:\n");
            sb.append(String.format("- Aportes: R$ %.2f (qtd %.8f)\n", aportes, qtdAporte));
            sb.append(String.format("- Compras: R$ %.2f (qtd %.8f)\n", compras, qtdComprada));
            sb.append(String.format("- Vendas:  R$ %.2f (qtd %.8f)\n", vendas, qtdVendida));
            sb.append(String.format("- Saldo:   R$ %.2f\n\n", saldoPeriodo));
        }

        sb.append("📍 Agora:\n");
        sb.append(String.format("- Quantidade: %.8f\n", qtdAtual));
        sb.append(String.format("- Cotação:   R$ %.2f\n", precoAtual));
        sb.append(String.format("- Valor:     R$ %.2f\n", valorAtual));

        // 🎯 diagnóstico rápido (por que mexeu)
        BigDecimal inflow = compras.add(aportes);   // dinheiro entrando
        BigDecimal outflow = vendas;                // dinheiro saindo (venda)
        BigDecimal netFlow = outflow.subtract(inflow).setScale(2, RoundingMode.HALF_UP);
        // netFlow > 0 = você tirou dinheiro (vendeu mais do que comprou)
        // netFlow < 0 = você colocou dinheiro (comprou/aportou mais)

        sb.append("\n🧠 Diagnóstico (bem direto):\n");

        if (movs.isEmpty()) {
            sb.append("- Você não mexeu. Foi só variação de preço. 😏\n");
        } else {
            if (netFlow.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("- Você realizou R$ %.2f (mais vendas do que compras/aportes).\n", netFlow));
                sb.append("- Se caiu depois, pelo menos você saiu antes. Um raro momento de lucidez. 😌\n");
            } else if (netFlow.compareTo(BigDecimal.ZERO) < 0) {
                sb.append(String.format("- Você aumentou a posição: colocou ~R$ %.2f no ativo.\n", netFlow.abs()));
                sb.append("- Se caiu, você basicamente comprou convicção… ou teimosia. 😏\n");
            } else {
                sb.append("- Fluxo neutro (comprou e vendeu quase igual). Foi mais ajuste do que estratégia.\n");
            }
        }

        sb.append("\nSe quiser eu comparo com snapshots quando tiver histórico suficiente. 😉");
        return sb.toString();
    }

    private BigDecimal somarAportes(Usuario usuario, ClasseAtivo filtro, LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) return BigDecimal.ZERO;
        BigDecimal v = (filtro == null)
                ? movRepo.somarValorPorTipoNoPeriodo(usuario, TipoMovimentoCarteira.APORTE_BRL, inicio, fim)
                : movRepo.somarValorPorTipoClasseNoPeriodo(usuario, TipoMovimentoCarteira.APORTE_BRL, filtro, inicio, fim);
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal rentabilidadePct(BigDecimal lucro, BigDecimal base) {
        if (base.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return lucro.divide(base, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
