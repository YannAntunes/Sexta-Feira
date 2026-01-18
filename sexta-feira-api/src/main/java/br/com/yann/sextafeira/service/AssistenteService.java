package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import br.com.yann.sextafeira.dto.ChatResponse;
import br.com.yann.sextafeira.dto.ResumoMensalDTO;
import br.com.yann.sextafeira.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class AssistenteService {

    private final TransacaoIaService transacaoIaService;
    private final TransacaoRepository transacaoRepository;
    private final TransacaoService transacaoService;
    private final OrcamentoService orcamentoService;


    public AssistenteService(TransacaoIaService transacaoIaService,
                             TransacaoRepository transacaoRepository,
                             TransacaoService transacaoService,
                             OrcamentoService orcamentoService) {
        this.transacaoIaService = transacaoIaService;
        this.transacaoRepository = transacaoRepository;
        this.transacaoService = transacaoService;
        this.orcamentoService = orcamentoService;
    }

    public ChatResponse processarMensagem(String mensagem) {
        String lower = mensagem.toLowerCase().trim();

        // 1) PERGUNTA: "quanto gastei esse mês?"
        if (lower.contains("quanto gastei") && (lower.contains("mes") || lower.contains("mês"))) {
            YearMonth agora = YearMonth.from(LocalDate.now());
            ResumoMensalDTO resumo = transacaoService.calcularResumoMensal(agora.getYear(), agora.getMonthValue());

            String resposta = String.format(
                    "Resumo de %02d/%d: " +
                            "- Despesas: R$ %.2f " +
                            "- Receitas: R$ %.2f " +
                            "- Saldo: R$ %.2f  " +
                            "Tradução: %s",
                    resumo.getMes(),
                    resumo.getAno(),
                    resumo.getTotalDespesas(),
                    resumo.getTotalReceitas(),
                    resumo.getSaldo(),
                    resumo.getSaldo().signum() >= 0 ?
                            "você está no azul, não estrague isso." :
                            "você está no vermelho."
            );

            return new ChatResponse(resposta);
        }

        // 2) PERGUNTA mais geral: "como estão minhas finanças?"
        if (lower.contains("como estao minhas financas") ||
                lower.contains("como estão minhas finanças")) {

            var agora = YearMonth.from(LocalDate.now());
            ResumoMensalDTO resumo = transacaoService.calcularResumoMensal(
                    agora.getYear(), agora.getMonthValue()
            );

            String resposta = String.format(
                    "Suas finanças deste mês (%02d/%d): " +
                            "- Despesas: R$ %.2f " +
                            "- Receitas: R$ %.2f " +
                            "- Saldo: R$ %.2f ",
                    resumo.getMes(),
                    resumo.getAno(),
                    resumo.getTotalDespesas(),
                    resumo.getTotalReceitas(),
                    resumo.getSaldo()
            );

            return new ChatResponse(resposta);
        }

        // DEFINIR ORÇAMENTO: "definir orçamento de 500 para alimentação"
        if (lower.contains("definir orçamento") || lower.contains("definir orcamento") || lower.contains("defina")) {
            // pega primeiro número que aparecer
            var partes = lower.replace(",", ".").split(" ");
            java.math.BigDecimal valor = null;
            for (String p : partes) {
                try {
                    valor = new java.math.BigDecimal(p);
                    break;
                } catch (NumberFormatException e) {
                    // ignora
                }
            }

            if (valor == null) {
                return new ChatResponse("Não entendi o valor do orçamento. Pode repetir com o valor em reais?");
            }

            // categoria bem simplificada
            CategoriaTransacao categoria = CategoriaTransacao.OUTROS;
            if (lower.contains("alimentacao") || lower.contains("alimentação") || lower.contains("comida")) {
                categoria = CategoriaTransacao.ALIMENTACAO;
            } else if (lower.contains("transporte") || lower.contains("uber")) {
                categoria = CategoriaTransacao.TRANSPORTE;
            }

            var agora = java.time.YearMonth.from(java.time.LocalDate.now());

            var req = new br.com.yann.sextafeira.dto.OrcamentoRequest();
            req.setAno(agora.getYear());
            req.setMes(agora.getMonthValue());
            req.setCategoria(categoria);
            req.setValorLimite(valor);

            orcamentoService.definirOrcamento(req);

            String resposta = String.format(
                    "Beleza! Defini um orçamento de R$ %.2f para %s em %02d/%d.",
                    valor,
                    categoria.name(),
                    req.getMes(),
                    req.getAno()
            );

            return new ChatResponse(resposta);
        }

        // STATUS DO ORÇAMENTO: "como está meu orçamento de alimentação?"
        if (lower.contains("orcamento de") || lower.contains("orçamento de")) {

            CategoriaTransacao categoria = CategoriaTransacao.OUTROS;
            if (lower.contains("alimentacao") || lower.contains("alimentação") || lower.contains("comida")) {
                categoria = CategoriaTransacao.ALIMENTACAO;
            } else if (lower.contains("transporte") || lower.contains("uber")) {
                categoria = CategoriaTransacao.TRANSPORTE;
            }

            var agora = java.time.YearMonth.from(java.time.LocalDate.now());
            var status = orcamentoService.consultarStatus(
                    agora.getYear(), agora.getMonthValue(), categoria
            );

            String categoriaFormatada = status.getCategoria().name()
                    .toLowerCase().replace("_", " ");
            categoriaFormatada = Character.toUpperCase(categoriaFormatada.charAt(0))
                    + categoriaFormatada.substring(1);

            String mesAno = String.format("%02d/%d", status.getMes(), status.getAno());

            if (status.getLimite().compareTo(java.math.BigDecimal.ZERO) == 0) {
                return new ChatResponse(
                        "Você ainda não definiu um orçamento para " + categoriaFormatada +
                                " em " + mesAno + ".\nSe a ideia é viver no modo freestyle financeiro, está funcionando. Quer que eu defina um limite?"
                );
            }

            java.math.BigDecimal limite = status.getLimite();
            java.math.BigDecimal gasto = status.getGasto();
            java.math.BigDecimal restante = status.getRestante();

            // porcentagem usada
            java.math.BigDecimal perc = java.math.BigDecimal.ZERO;
            if (limite.compareTo(java.math.BigDecimal.ZERO) > 0) {
                perc = gasto
                        .divide(limite, 2, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100));
            }

            StringBuilder resposta = new StringBuilder();
            resposta.append(String.format(
                    "Orçamento de %s — %s\n\n", categoriaFormatada, mesAno));
            resposta.append(String.format(
                    "Limite: R$ %.2f\nGasto: R$ %.2f\n", limite, gasto));

            if (status.isEstourado()) {
                java.math.BigDecimal excedente = gasto.subtract(limite);
                resposta.append(String.format(
                        "Você passou do limite em R$ %.2f.\n", excedente));
                resposta.append("Resumindo: o orçamento morreu antes do mês acabar. Talvez seja hora de frear um pouco. 😉");
            } else {
                resposta.append(String.format(
                        "Disponível: R$ %.2f (%.0f%% do orçamento ainda vivo).\n",
                        restante, java.math.BigDecimal.valueOf(100).subtract(perc)));
                if (perc.compareTo(java.math.BigDecimal.valueOf(80)) >= 0) {
                    resposta.append("Você já usou boa parte do limite. Não é crítico ainda, mas o futuro eu pode não gostar dessas decisões.");
                } else {
                    resposta.append("Por enquanto está sob controle. Não me dê motivos pra enviar alerta dramático.");
                }
            }

            return new ChatResponse(resposta.toString());
        }

        // 3) LANÇAMENTO: "gastei", "paguei", "comprei", "recebi", "ganhei"
        if (lower.contains("gastei") ||
                lower.contains("paguei") ||
                lower.contains("comprei") ||
                lower.contains("recebi") ||
                lower.contains("ganhei")) {

            Transacao interpretada = transacaoIaService.interpretarMensagem(mensagem);
            Transacao salva = transacaoRepository.save(interpretada);

            String categoriaFormatada = salva.getCategoria().name()
                    .toLowerCase().replace("_", " ");
            categoriaFormatada = Character.toUpperCase(categoriaFormatada.charAt(0))
                    + categoriaFormatada.substring(1);

            String resposta;
            if (salva.getTipo() == TipoTransacao.RECEITA) {
                resposta = String.format(
                        "Entrada registrada: +R$ %.2f em %s.\nBom ver dinheiro vindo **pra você** e não indo embora.",
                        salva.getValor(),
                        categoriaFormatada
                );
            } else {
                resposta = String.format(
                        "Gasto registrado: -R$ %.2f em %s.\nAnotado. Só não transforma isso em esporte, combinado?",
                        salva.getValor(),
                        categoriaFormatada
                );
            }

            return new ChatResponse(resposta);
        }

        // 4) fallback
        return new ChatResponse(
                "Ainda não sei responder isso, mas já consigo registrar gastos/receitas e te dizer quanto você gastou no mês. 😊"
        );
    }
        }

