package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import br.com.yann.sextafeira.dto.*;
import br.com.yann.sextafeira.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@Service
public class AssistenteService {

    private final TransacaoIaService transacaoIaService;
    private final TransacaoRepository transacaoRepository;
    private final TransacaoService transacaoService;
    private final OrcamentoService orcamentoService;
    private final CurrencyService currencyService;
    private final RelatorioService relatorioService;
    private final EconomiaService economiaService;
    private final FrasesService frasesService;
    private final CarteiraRelatorioService carteiraRelatorioService;
    private final CarteiraService carteiraService;

    public AssistenteService(TransacaoIaService transacaoIaService,
                             TransacaoRepository transacaoRepository,
                             TransacaoService transacaoService,
                             OrcamentoService orcamentoService,
                             CurrencyService currencyService,
                             RelatorioService relatorioService,
                             EconomiaService economiaService,
                             FrasesService frasesService,
                             CarteiraRelatorioService carteiraRelatorioService,
                             CarteiraService carteiraService) {
        this.transacaoIaService = transacaoIaService;
        this.transacaoRepository = transacaoRepository;
        this.transacaoService = transacaoService;
        this.orcamentoService = orcamentoService;
        this.currencyService = currencyService;
        this.relatorioService = relatorioService;
        this.economiaService = economiaService;
        this.frasesService = frasesService;
        this.carteiraRelatorioService = carteiraRelatorioService;
        this.carteiraService = carteiraService;
    }

    public ChatResponse processarMensagem(String mensagem) {

        IaRouterResponse rota = transacaoIaService.rotearMensagem(mensagem);
        String intent = rota.getIntent();

        return switch (intent) {

            case "ADD_TRANSACTION" -> {
                Transacao interpretada = transacaoIaService.interpretarMensagem(mensagem);
                Transacao salva = transacaoRepository.save(interpretada);

                String resposta;
                if (salva.getTipo() == TipoTransacao.RECEITA) {
                    resposta = String.format(
                            frasesService.escolher("ADD_RECEITA"),
                            salva.getValor(),
                            formatarCategoria(salva.getCategoria())
                    );
                } else {
                    resposta = String.format(
                            frasesService.escolher("ADD_DESPESA"),
                            salva.getValor(),
                            formatarCategoria(salva.getCategoria())
                    );
                }

                // alerta imediato (só para DESPESA)
                if (salva.getTipo() == TipoTransacao.DESPESA) {

                    YearMonth ym = YearMonth.from(LocalDate.now());
                    var status = orcamentoService.consultarStatus(
                            ym.getYear(),
                            ym.getMonthValue(),
                            salva.getCategoria()
                    );

                    BigDecimal limite = status.getLimite();
                    BigDecimal gasto = status.getGasto();

                    if (limite != null && limite.compareTo(BigDecimal.ZERO) > 0) {

                        if (gasto.compareTo(limite) > 0) {
                            resposta += "\n\n" + String.format(
                                    frasesService.escolher("BUDGET_ESTOURO"),
                                    formatarCategoria(salva.getCategoria())
                            );
                        } else {
                            BigDecimal perc = gasto.divide(limite, 2, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));

                            if (perc.compareTo(BigDecimal.valueOf(90)) >= 0) {
                                resposta += "\n\n" + String.format(
                                        frasesService.escolher("BUDGET_90"),
                                        formatarCategoria(salva.getCategoria())
                                );
                            } else if (perc.compareTo(BigDecimal.valueOf(80)) >= 0) {
                                resposta += "\n\n" + String.format(
                                        frasesService.escolher("BUDGET_80"),
                                        formatarCategoria(salva.getCategoria())
                                );
                            }
                        }
                    }
                }

                yield new ChatResponse(resposta);
            }

            case "DELETE_TRANSACTION" -> {
                Transacao removida = transacaoService.removerPorTexto(mensagem);
                yield new ChatResponse(String.format(
                        "Feito. Apaguei: %s de R$ %.2f em %s (%s).\n%s",
                        removida.getTipo().name(),
                        removida.getValor(),
                        formatarCategoria(removida.getCategoria()),
                        removida.getData(),
                        frasesService.escolher("DELETE_OK")
                ));

            }

            case "ASK_MONTH_SUMMARY" -> {
                YearMonth agora = YearMonth.from(LocalDate.now());
                ResumoMensalDTO resumo = transacaoService.calcularResumoMensal(agora.getYear(), agora.getMonthValue());

                String traducao = resumo.getSaldo().signum() >= 0
                        ? "você está no azul. Tenta não estragar isso."
                        : "você está no vermelho. Não é uma vibe bonita.";

                yield new ChatResponse(String.format(
                        "Resumo %02d/%d:\n" +
                                "- Despesas: R$ %.2f\n" +
                                "- Receitas: R$ %.2f\n" +
                                "- Saldo: R$ %.2f\n\n" +
                                "Tradução: %s 😌",
                        resumo.getMes(), resumo.getAno(),
                        resumo.getTotalDespesas(), resumo.getTotalReceitas(), resumo.getSaldo(),
                        traducao
                ));
            }

            case "ASK_BUDGET_STATUS" -> {
                CategoriaTransacao categoria = inferirCategoriaPorTexto(mensagem);

                YearMonth agora = YearMonth.from(LocalDate.now());
                var status = orcamentoService.consultarStatus(agora.getYear(), agora.getMonthValue(), categoria);

                String categoriaFormatada = formatarCategoria(categoria);
                String mesAno = String.format("%02d/%d", status.getMes(), status.getAno());

                if (status.getLimite().compareTo(BigDecimal.ZERO) == 0) {
                    yield new ChatResponse(
                            "Você ainda não definiu um orçamento para " + categoriaFormatada + " em " + mesAno + ".\n" +
                                    "Se a ideia é viver no modo freestyle financeiro, está funcionando. Quer que eu defina um limite? 😏"
                    );
                }

                BigDecimal limite = status.getLimite();
                BigDecimal gasto = status.getGasto();
                BigDecimal restante = status.getRestante();

                BigDecimal perc = BigDecimal.ZERO;
                if (limite.compareTo(BigDecimal.ZERO) > 0) {
                    perc = gasto.divide(limite, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                }

                StringBuilder resposta = new StringBuilder();
                resposta.append(String.format("Orçamento de %s — %s\n\n", categoriaFormatada, mesAno));
                resposta.append(String.format("Limite: R$ %.2f\nGasto: R$ %.2f\n", limite, gasto));

                if (status.isEstourado()) {
                    BigDecimal excedente = gasto.subtract(limite);
                    resposta.append(String.format("Excedente: R$ %.2f\n\n", excedente));
                    resposta.append("Você estourou o orçamento. Não julgo… mas seu eu do futuro vai. 😉");
                } else {
                    resposta.append(String.format("Disponível: R$ %.2f\n\n", restante));

                    if (perc.compareTo(BigDecimal.valueOf(80)) >= 0) {
                        resposta.append("Você já usou boa parte do limite. Não é crítico ainda… mas eu ficaria esperta.");
                    } else {
                        resposta.append("Por enquanto está sob controle. Não me dê motivo pra drama.");
                    }
                }

                yield new ChatResponse(resposta.toString());
            }

            case "CONVERT_CURRENCY" -> {
                Map<String, Object> e = rota.getEntities();

                if (e == null) {
                    yield new ChatResponse("Você quer converter quanto e pra qual moeda? Eu não leio mentes (ainda). 😏");
                }

                Object amountObj = e.get("amount");
                Object fromObj = e.get("from");
                Object toObj = e.get("to");

                if (amountObj == null || fromObj == null || toObj == null) {
                    yield new ChatResponse("Faltou informação. Ex: \"converte 10 USD para BRL\". Eu facilito, você coopera. 😉");
                }

                BigDecimal amount = new BigDecimal(amountObj.toString());
                String from = fromObj.toString();
                String to = toObj.toString();

                ConvertRequest req = new ConvertRequest();
                req.setAmount(amount);
                req.setFrom(from);
                req.setTo(to);

                var resp = currencyService.converter(req);

                String resposta = String.format(
                        "Converti %s %.2f → %s %.2f.\nTaxa (1 %s): %.4f (%s).\n" +
                                "De nada. Agora tenta não gastar tudo em besteira. 😌",
                        resp.getFrom(), resp.getAmount(),
                        resp.getTo(), resp.getResult(),
                        resp.getFrom(), resp.getRate(),
                        resp.getDate()
                );

                yield new ChatResponse(resposta);
            }

            case "ASK_MONTH_REPORT" -> {
                var rel = relatorioService.gerarRelatorioMesAtual();

                StringBuilder sb = new StringBuilder();
                sb.append(rel.getResumoTexto()).append("\n\n");

                sb.append("🏷️ Top categorias:\n");
                for (String t : rel.getTopCategorias()) sb.append("- ").append(t).append("\n");
                sb.append("\n");

                sb.append("🚦 Alertas:\n");
                for (String a : rel.getAlertas()) sb.append("- ").append(a).append("\n");
                sb.append("\n");

                sb.append("📊 Gráficos:\n");
                for (String link : rel.getLinksGraficos()) sb.append("- ").append(link).append("\n");

                sb.append("\nPronto. Agora vai lá e faz escolhas financeiras minimamente sensatas. 😌");

                yield new ChatResponse(sb.toString());
            }

            case "ASK_PERIOD_SUMMARY" -> {
                var e = rota.getEntities();

                String range = e != null && e.get("range") != null ? e.get("range").toString() : "UNSPECIFIED";
                Integer days = null;
                if (e != null && e.get("days") != null) {
                    days = Integer.valueOf(e.get("days").toString());
                }

                var datas = PeriodoService.resolverRange(range, days);
                var inicio = datas.get("inicio");
                var fim = datas.get("fim");

                var resumo = transacaoService.calcularResumoPorIntervalo(inicio, fim);

                String label = switch (range) {
                    case "TODAY" -> "hoje";
                    case "YESTERDAY" -> "ontem";
                    case "THIS_WEEK" -> "essa semana";
                    case "LAST_WEEK" -> "semana passada";
                    case "THIS_MONTH" -> "esse mês";
                    case "LAST_MONTH" -> "mês passado";
                    case "LAST_N_DAYS" -> "últimos " + (days == null ? 7 : days) + " dias";
                    default -> "últimos dias";
                };

                String resposta = String.format(
                        "📌 Resumo %s (%s → %s)\n\n" +
                                "Despesas: R$ %.2f\nReceitas: R$ %.2f\nSaldo: R$ %.2f\n\n" +
                                "Tradução: %s 😌",
                        label,
                        inicio, fim,
                        resumo.getTotalDespesas(),
                        resumo.getTotalReceitas(),
                        resumo.getSaldo(),
                        resumo.getSaldo().signum() >= 0 ? "nada pegando fogo (por enquanto)" : "você está gastando mais do que entra"
                );

                yield new ChatResponse(resposta);
            }

            case "ASK_SAVING_TIPS" ->
                    new ChatResponse(economiaService.sugerirCortesSemana());

            case "SET_BUDGET" -> {
                var e = rota.getEntities();

                if (e == null || e.get("amount") == null || e.get("category") == null) {
                    yield new ChatResponse(
                            "Diz direito: categoria e valor. Ex: \"orçamento de alimentação 1000 reais\". 😉"
                    );
                }

                BigDecimal valor = new BigDecimal(e.get("amount").toString());
                CategoriaTransacao categoria = CategoriaTransacao.valueOf(e.get("category").toString());

                YearMonth ym = YearMonth.from(LocalDate.now());

                OrcamentoRequest req = new OrcamentoRequest();
                req.setAno(ym.getYear());
                req.setMes(ym.getMonthValue());
                req.setCategoria(categoria);
                req.setValorLimite(valor);

                orcamentoService.definirOrcamento(req);

                yield new ChatResponse(String.format(
                        "Orçamento definido.\n%s: R$ %.2f em %02d/%d.\nAgora tenta não sabotar isso em 3 dias. 😌",
                        categoria.name(),
                        valor,
                        ym.getMonthValue(),
                        ym.getYear()
                ));
            }

            case "ASK_BUDGET_OVERVIEW" -> {
                YearMonth ym = YearMonth.from(LocalDate.now());
                int ano = ym.getYear();
                int mes = ym.getMonthValue();

                var statusList = orcamentoService.resumoGeral(ano, mes);

                BigDecimal totalLimites = BigDecimal.ZERO;
                BigDecimal totalGastos = BigDecimal.ZERO;

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("📌 Resumo geral de orçamentos — %02d/%d\n\n", mes, ano));

                for (var st : statusList) {
                    BigDecimal limite = st.getLimite() == null ? BigDecimal.ZERO : st.getLimite();
                    BigDecimal gasto = st.getGasto() == null ? BigDecimal.ZERO : st.getGasto();

                    // Só mostra categorias que tenham orçamento definido OU que tiveram gasto
                    if (limite.compareTo(BigDecimal.ZERO) <= 0 && gasto.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    totalLimites = totalLimites.add(limite);
                    totalGastos = totalGastos.add(gasto);

                    String catFmt = formatarCategoria(st.getCategoria());

                    String badge;
                    if (limite.compareTo(BigDecimal.ZERO) <= 0) {
                        badge = "⚪ sem limite";
                    } else if (gasto.compareTo(limite) > 0) {
                        badge = "🚨 estourado";
                    } else {
                        BigDecimal perc = gasto.divide(limite, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                        if (perc.compareTo(BigDecimal.valueOf(90)) >= 0) badge = "🔥 " + perc.intValue() + "%";
                        else if (perc.compareTo(BigDecimal.valueOf(80)) >= 0) badge = "⚠️ " + perc.intValue() + "%";
                        else badge = "✅ " + perc.intValue() + "%";
                    }

                    sb.append(String.format(
                            "- %s: R$ %.2f / R$ %.2f (%s)\n",
                            catFmt, gasto, limite, badge
                    ));
                }

                sb.append("\n");

                if (totalLimites.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percTotal = totalGastos.divide(totalLimites, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    sb.append(String.format("Total: R$ %.2f de R$ %.2f (%.0f%%)\n",
                            totalGastos, totalLimites, percTotal));
                } else {
                    sb.append(String.format("Total gasto no mês: R$ %.2f\n", totalGastos));
                    sb.append("Você ainda não definiu limites gerais. Corajosa essa sua fé no destino. 😏\n");
                }

                sb.append("\nSe quiser, eu detalho uma categoria específica. 😉");

                yield new ChatResponse(sb.toString());
            }

            case "ASK_PORTFOLIO_REPORT" -> {
                var e = rota.getEntities();

                String range = e != null && e.get("range") != null ? e.get("range").toString() : "UNSPECIFIED";
                Integer days = null;
                if (e != null && e.get("days") != null) {
                    days = Integer.valueOf(e.get("days").toString());
                }

                String periodoLabel = switch (range) {
                    case "TODAY" -> "hoje";
                    case "YESTERDAY" -> "ontem";
                    case "THIS_WEEK" -> "essa semana";
                    case "LAST_WEEK" -> "semana passada";
                    case "THIS_MONTH" -> "esse mês";
                    case "LAST_MONTH" -> "mês passado";
                    case "LAST_N_DAYS" -> "últimos " + (days == null ? 7 : days) + " dias";
                    default -> "no período recente";
                };

                // filtro por texto (fiis/ações/cripto)
                ClasseAtivo filtro = inferirFiltroCarteira(mensagem);

                var resumo = carteiraRelatorioService.gerarResumo(periodoLabel, filtro);

                StringBuilder sb = new StringBuilder();
                sb.append("📊 Carteira de investimentos — ").append(resumo.getPeriodoLabel()).append("\n\n");

                if (resumo.getItens().isEmpty()) {
                    sb.append("Sua carteira está vazia. Isso é paz… ou procrastinação. 😏");
                    yield new ChatResponse(sb.toString());
                }

                for (var it : resumo.getItens()) {
                    sb.append(String.format(
                            "- %s (%s): %.8f | Cotação: R$ %.2f | Valor: R$ %.2f\n",
                            it.getTicker(),
                            it.getClasse().name(),
                            it.getQuantidade(),
                            it.getPrecoAtual(),
                            it.getValorEstimado()
                    ));
                }

                sb.append("\n📌 Totais:\n");
                sb.append(String.format("- Ações: R$ %.2f\n", resumo.getTotalAcoes()));
                sb.append(String.format("- FIIs: R$ %.2f\n", resumo.getTotalFiis()));
                sb.append(String.format("- Cripto: R$ %.2f\n", resumo.getTotalCripto()));
                sb.append(String.format("\n💰 Total geral: R$ %.2f\n", resumo.getTotalGeral()));

                sb.append("\nSe quiser eu separo por classe: “relatório de fiis” / “relatório de cripto”. 😉");

                yield new ChatResponse(sb.toString());
            }

            case "ADD_HOLDING_QTY" -> {
                Map<String, Object> e = rota.getEntities();
                if (e == null || e.get("ticker") == null || e.get("classe") == null || e.get("qty") == null) {
                    yield new ChatResponse("Me diz o ativo e a quantidade. Ex: \"adicionar 10 ações da PETR4\".");
                }

                String ticker = e.get("ticker").toString().toUpperCase();
                ClasseAtivo classe = ClasseAtivo.valueOf(e.get("classe").toString().toUpperCase());
                BigDecimal qty = new BigDecimal(e.get("qty").toString());

                carteiraService.adicionarQuantidade(classe, ticker, qty);

                yield new ChatResponse(String.format(
                        "Beleza. Adicionei %.8f de %s (%s) na sua carteira. ✅",
                        qty, ticker, classe.name()
                ));
            }

            case "SELL_HOLDING_QTY" -> {
                Map<String, Object> e = rota.getEntities();
                if (e == null || e.get("ticker") == null || e.get("classe") == null || e.get("qty") == null) {
                    yield new ChatResponse("Me diz o ativo e a quantidade. Ex: \"vendi 5 ações da BBAS3\".");
                }

                String ticker = e.get("ticker").toString().toUpperCase();
                ClasseAtivo classe = ClasseAtivo.valueOf(e.get("classe").toString().toUpperCase());
                BigDecimal qty = new BigDecimal(e.get("qty").toString());

                // vender = adicionar quantidade negativa
                carteiraService.adicionarQuantidade(classe, ticker, qty.negate());

                yield new ChatResponse(String.format(
                        "Ok. Dei baixa de %.8f de %s (%s). ✅",
                        qty, ticker, classe.name()
                ));
            }

            case "ADD_HOLDING_VALUE" -> {
                Map<String, Object> e = rota.getEntities();
                if (e == null || e.get("ticker") == null || e.get("classe") == null || e.get("value_brl") == null) {
                    yield new ChatResponse("Me diz o ativo e o valor em reais. Ex: \"adicione 20 reais em BTC\".");
                }

                String ticker = e.get("ticker").toString().toUpperCase();
                ClasseAtivo classe = ClasseAtivo.valueOf(e.get("classe").toString().toUpperCase());
                BigDecimal valorBRL = new BigDecimal(e.get("value_brl").toString());

                carteiraService.adicionarPorValorBRL(classe, ticker, valorBRL);

                yield new ChatResponse(String.format(
                        "Fechado. Converti R$ %.2f em %s (%s) e somei na sua carteira. ✅",
                        valorBRL, ticker, classe.name()
                ));
            }

            default -> new ChatResponse("Não entendi. Reformula isso como se eu fosse uma IA milionária, por favor. 😏");
        };
    }

    private String formatarCategoria(CategoriaTransacao categoria) {
        String s = categoria.name().toLowerCase().replace("_", " ");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private CategoriaTransacao inferirCategoriaPorTexto(String mensagem) {
        String lower = mensagem.toLowerCase();

        if (lower.contains("alimentacao") || lower.contains("alimentação") || lower.contains("comida") || lower.contains("mercado")) {
            return CategoriaTransacao.ALIMENTACAO;
        }
        if (lower.contains("uber") || lower.contains("transporte") || lower.contains("gasolina") || lower.contains("ônibus") || lower.contains("onibus")) {
            return CategoriaTransacao.TRANSPORTE;
        }
        if (lower.contains("aluguel") || lower.contains("condominio") || lower.contains("condomínio") || lower.contains("moradia")) {
            return CategoriaTransacao.MORADIA;
        }
        if (lower.contains("netflix") || lower.contains("cinema") || lower.contains("lazer") || lower.contains("show")) {
            return CategoriaTransacao.LAZER;
        }
        if (lower.contains("farmacia") || lower.contains("farmácia") || lower.contains("remedio") || lower.contains("remédio") || lower.contains("saude") || lower.contains("saúde")) {
            return CategoriaTransacao.SAUDE;
        }
        if (lower.contains("curso") || lower.contains("faculdade") || lower.contains("educacao") || lower.contains("educação")) {
            return CategoriaTransacao.EDUCACAO;
        }

        return CategoriaTransacao.OUTROS;
    }

    private ClasseAtivo inferirFiltroCarteira(String mensagem) {
        String m = mensagem.toLowerCase();

        if (m.contains("cripto") || m.contains("bitcoin") || m.contains("btc") || m.contains("eth")) {
            return ClasseAtivo.CRIPTO;
        }
        if (m.contains("fii") || m.contains("fiis") || m.contains("fundo imobili") ) {
            return ClasseAtivo.FII;
        }
        if (m.contains("acao") || m.contains("ações") || m.contains("acoes") || m.contains("bolsa")) {
            return ClasseAtivo.ACAO;
        }

        return null; // sem filtro => tudo
    }

}
