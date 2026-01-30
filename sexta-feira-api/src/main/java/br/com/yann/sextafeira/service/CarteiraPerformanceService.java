package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.*;
import br.com.yann.sextafeira.repository.MovimentoCarteiraRepository;
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

    public CarteiraPerformanceService(MovimentoCarteiraRepository movRepo,
                                      CarteiraService carteiraService,
                                      CotacaoService cotacaoService) {
        this.movRepo = movRepo;
        this.carteiraService = carteiraService;
        this.cotacaoService = cotacaoService;
    }

    public String gerarRelatorioPeriodo(LocalDate inicio, LocalDate fim, ClasseAtivo filtro) {

        List<MovimentoCarteira> movs = (filtro == null)
                ? movRepo.findByDataBetweenOrderByDataAscIdAsc(inicio, fim)
                : movRepo.findByClasseAndDataBetweenOrderByDataAscIdAsc(filtro, inicio, fim);

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

        // posição atual (estado atual)
        var ativos = (filtro == null)
                ? carteiraService.listarTudo()
                : carteiraService.listarPorClasse(filtro);

        BigDecimal valorAtual = BigDecimal.ZERO;
        StringBuilder posicao = new StringBuilder();

        for (AtivoCarteira a : ativos) {
            BigDecimal preco = cotacaoService.cotacaoAtualSeguro(a.getTicker(), a.getClasse());
            BigDecimal valor = a.getQuantidade().multiply(preco).setScale(2, RoundingMode.HALF_UP);
            valorAtual = valorAtual.add(valor);

            posicao.append(String.format(
                    "- %s (%s): %.8f | cotação R$ %.2f | valor R$ %.2f\n",
                    a.getTicker(), a.getClasse().name(), a.getQuantidade(), preco, valor
            ));
        }

        BigDecimal fluxoLiquido = aportes.add(compras).subtract(vendas).setScale(2, RoundingMode.HALF_UP);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📆 Relatório da carteira (%s → %s)\n\n", inicio, fim));

        sb.append("🧾 Movimentação no período:\n");
        sb.append(String.format("- Aportes (BRL): R$ %.2f\n", aportes));
        sb.append(String.format("- Compras:       R$ %.2f\n", compras));
        sb.append(String.format("- Vendas:        R$ %.2f\n", vendas));
        sb.append(String.format("- Fluxo líquido: R$ %.2f\n\n", fluxoLiquido));

        sb.append("📌 Posição atual:\n");
        if (ativos.isEmpty()) sb.append("— vazia —\n");
        else sb.append(posicao);

        sb.append(String.format("\n💰 Valor atual estimado: R$ %.2f\n", valorAtual));
        sb.append("\nSe quiser, eu faço por ativo (PETR4/BTC) e te digo o que mais mexeu no período. 😉");

        return sb.toString();
    }
}
