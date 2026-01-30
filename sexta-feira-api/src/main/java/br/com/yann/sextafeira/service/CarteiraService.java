package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.AtivoCarteira;
import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.repository.AtivoCarteiraRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CarteiraService {

    private final AtivoCarteiraRepository repo;
    private final CotacaoService cotacaoService;

    public CarteiraService(AtivoCarteiraRepository repo, CotacaoService cotacaoService) {
        this.repo = repo;
        this.cotacaoService = cotacaoService;
    }

    public AtivoCarteira adicionarQuantidade(ClasseAtivo classe, String ticker, BigDecimal quantidade) {
        final String tickerFinal = ticker.toUpperCase().trim();

        AtivoCarteira ativo = repo.findByClasseAndTicker(classe, tickerFinal)
                .orElseGet(() -> new AtivoCarteira(classe, tickerFinal));

        BigDecimal novaQtd = ativo.getQuantidade().add(quantidade);
        ativo.setQuantidade(novaQtd);

        // se zerou ou ficou negativo, remove pra não ficar lixo
        if (novaQtd.compareTo(BigDecimal.ZERO) <= 0) {
            if (ativo.getId() != null) repo.delete(ativo);
            return ativo;
        }

        return repo.save(ativo);
    }

    // ✅ “adicione mais 20 reais em BTC” => converte para quantidade usando cotação atual (BRL/unidade)
    public AtivoCarteira adicionarPorValorBRL(ClasseAtivo classe, String ticker, BigDecimal valorBRL) {
        BigDecimal preco = cotacaoService.cotacaoAtual(ticker, classe); // ✅ AGORA COM 2 ARGUMENTOS
        if (preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Não consegui pegar cotação de " + ticker + " agora.");
        }

        BigDecimal qtd = valorBRL.divide(preco, 8, RoundingMode.HALF_UP);
        return adicionarQuantidade(classe, ticker, qtd);
    }

    public List<AtivoCarteira> listarTudo() {
        return repo.findAll();
    }

    public List<AtivoCarteira> listarPorClasse(ClasseAtivo classe) {
        return repo.findByClasseOrderByTickerAsc(classe);
    }
}
