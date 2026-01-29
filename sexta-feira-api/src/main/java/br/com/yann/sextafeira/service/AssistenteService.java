package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
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

    public AssistenteService(TransacaoIaService transacaoIaService,
                             TransacaoRepository transacaoRepository,
                             TransacaoService transacaoService,
                             OrcamentoService orcamentoService,
                             CurrencyService currencyService,
                             RelatorioService relatorioService,
                             EconomiaService economiaService) {
        this.transacaoIaService = transacaoIaService;
        this.transacaoRepository = transacaoRepository;
        this.transacaoService = transacaoService;
        this.orcamentoService = orcamentoService;
        this.currencyService = currencyService;
        this.relatorioService = relatorioService;
        this.economiaService = economiaService;
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
                            "Entrada registrada: +R$ %.2f em %s.\nBom ver dinheiro vindo **pra você** e não indo embora.",
                            salva.getValor(),
                            formatarCategoria(salva.getCategoria())
                    );
                } else {
                    resposta = String.format(
                            "Gasto registrado: -R$ %.2f em %s.\nAnotado. Só não transforma isso em esporte, combinado?",
                            salva.getValor(),
                            formatarCategoria(salva.getCategoria())
                    );
                }

                yield new ChatResponse(resposta);
            }

            case "DELETE_TRANSACTION" -> {
                Transacao removida = transacaoService.removerPorTexto(mensagem);
                yield new ChatResponse(String.format(
                        "Feito. Apaguei: %s de R$ %.2f em %s (%s).\n" +
                                "Tente não transformar isso em edição de histórico. 😉",
                        removida.getTipo().name(),
                        removida.getValor(),
                        formatarCategoria(removida.getCategoria()),
                        removida.getData()
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
}
