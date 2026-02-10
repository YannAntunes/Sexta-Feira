package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.modes.finance.domain.model.AtivoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.dto.CarteiraAtivoItemDTO;
import br.com.yann.sextafeira.modes.finance.dto.CarteiraResumoDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarteiraRelatorioService {

    private final CarteiraService carteiraService;
    private final CotacaoService cotacaoService;

    public CarteiraRelatorioService(CarteiraService carteiraService, CotacaoService cotacaoService) {
        this.carteiraService = carteiraService;
        this.cotacaoService = cotacaoService;
    }

    public CarteiraResumoDTO gerarResumo(String periodoLabel, ClasseAtivo filtro) {

        List<AtivoCarteira> ativos = (filtro == null)
                ? carteiraService.listarTudo()
                : carteiraService.listarPorClasse(filtro);
        List<CarteiraAtivoItemDTO> itens = new ArrayList<>();

        BigDecimal totalAcoes = BigDecimal.ZERO;
        BigDecimal totalFiis = BigDecimal.ZERO;
        BigDecimal totalCripto = BigDecimal.ZERO;

        for (AtivoCarteira a : ativos) {

            BigDecimal precoAtual = cotacaoService.cotacaoAtualSeguro(a.getTicker(), a.getClasse());
            BigDecimal valor = a.getQuantidade().multiply(precoAtual).setScale(2, RoundingMode.HALF_UP);

            itens.add(new CarteiraAtivoItemDTO(
                    a.getClasse(),
                    a.getTicker(),
                    a.getQuantidade(),
                    precoAtual,
                    valor
            ));

            switch (a.getClasse()) {
                case ACAO -> totalAcoes = totalAcoes.add(valor);
                case FII -> totalFiis = totalFiis.add(valor);
                case CRIPTO -> totalCripto = totalCripto.add(valor);
            }
        }

        BigDecimal totalGeral = totalAcoes.add(totalFiis).add(totalCripto);

        return new CarteiraResumoDTO(periodoLabel, totalAcoes, totalFiis, totalCripto, totalGeral, itens);
    }

    public String gerarRelatorio(String range, Integer days) {
        // por enquanto o range/days não muda cálculo (a carteira é estado atual)
        // depois a gente usa isso pra "performance" por período

        List<AtivoCarteira> ativos = carteiraService.listarTudo();
        if (ativos.isEmpty()) return "Sua carteira está vazia. Um minimalismo financeiro admirável. 😏";

        BigDecimal total = BigDecimal.ZERO;

        StringBuilder sb = new StringBuilder();
        sb.append("📈 Relatório da carteira (posição atual)\n\n");

        for (AtivoCarteira a : ativos) {
            BigDecimal preco = cotacaoService.cotacaoAtualSeguro(a.getTicker(), a.getClasse());
            BigDecimal valor = preco.multiply(a.getQuantidade()).setScale(2, RoundingMode.HALF_UP);
            total = total.add(valor);

            sb.append(String.format("- %s (%s): qtd=%.8f | cotação=R$ %.2f | valor=R$ %.2f\n",
                    a.getTicker(),
                    a.getClasse().name(),
                    a.getQuantidade(),
                    preco,
                    valor
            ));
        }

        sb.append(String.format("\nTotal estimado: R$ %.2f", total));
        sb.append("\n\nSe quiser, eu separo por AÇÕES/FII/CRIPTO. 😉");

        return sb.toString();
    }
}

