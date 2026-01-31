package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.AtivoCarteira;
import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.domain.model.MovimentoCarteira;
import br.com.yann.sextafeira.domain.model.TipoMovimentoCarteira;
import br.com.yann.sextafeira.repository.AtivoCarteiraRepository;
import br.com.yann.sextafeira.repository.MovimentoCarteiraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class CarteiraService {

    private final AtivoCarteiraRepository repo;
    private final CotacaoService cotacaoService;
    private final MovimentoCarteiraRepository movRepo;

    public CarteiraService(AtivoCarteiraRepository repo,
                           MovimentoCarteiraRepository movRepo,
                           CotacaoService cotacaoService) {
        this.repo = repo;
        this.movRepo = movRepo;
        this.cotacaoService = cotacaoService;
    }

    // ✅ Compra/venda por quantidade (qty pode ser negativa)
    public AtivoCarteira adicionarQuantidade(
            ClasseAtivo classe,
            String ticker,
            BigDecimal quantidadeSigned
    ) {
        final String tickerFinal = ticker.toUpperCase().trim();

        AtivoCarteira ativo = repo.findByClasseAndTicker(classe, tickerFinal)
                .orElseGet(() -> new AtivoCarteira(classe, tickerFinal));

        BigDecimal novaQtd = ativo.getQuantidade().add(quantidadeSigned);
        ativo.setQuantidade(novaQtd);

        // ✅ registra movimento
        registrarMovimentoQuantidade(
                classe,
                tickerFinal,
                quantidadeSigned,
                "ajuste por quantidade"
        );

        if (novaQtd.compareTo(BigDecimal.ZERO) <= 0) {
            if (ativo.getId() != null) repo.delete(ativo);
            return ativo;
        }

        return repo.save(ativo);
    }


    private AtivoCarteira atualizarPosicaoSemMovimento(ClasseAtivo classe, String ticker, BigDecimal quantidadeSigned) {
        final String tickerFinal = ticker.toUpperCase().trim();

        AtivoCarteira ativo = repo.findByClasseAndTicker(classe, tickerFinal)
                .orElseGet(() -> new AtivoCarteira(classe, tickerFinal));

        BigDecimal novaQtd = ativo.getQuantidade().add(quantidadeSigned);
        ativo.setQuantidade(novaQtd);

        if (novaQtd.compareTo(BigDecimal.ZERO) <= 0) {
            if (ativo.getId() != null) repo.delete(ativo);
            return ativo;
        }

        return repo.save(ativo);
    }


    // ✅ “adicione mais 20 reais em BTC” => converte para quantidade e registra APORTE_BRL
    public AtivoCarteira adicionarPorValorBRL(
            ClasseAtivo classe,
            String ticker,
            BigDecimal valorBRL
    ) {
        final String tickerFinal = ticker.toUpperCase().trim();

        BigDecimal precoUnit = cotacaoService.cotacaoAtual(tickerFinal, classe);
        if (precoUnit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Não consegui pegar cotação de " + tickerFinal);
        }

        BigDecimal qtd = valorBRL.divide(precoUnit, 8, RoundingMode.HALF_UP);

        // ✅ registra APORTE_BRL
        registrarMovimentoAporteBRL(
                classe,
                tickerFinal,
                valorBRL,
                precoUnit,
                qtd,
                "aporte em BRL"
        );

        return adicionarQuantidade(classe, tickerFinal, qtd);
    }



    public List<AtivoCarteira> listarTudo() {
        return repo.findAll();
    }

    public List<AtivoCarteira> listarPorClasse(ClasseAtivo classe) {
        return repo.findByClasseOrderByTickerAsc(classe);
    }

    // ===== REGISTROS =====

    private void registrarMovimentoQuantidade(ClasseAtivo classe,
                                              String ticker,
                                              BigDecimal quantidadeSigned,
                                              String obs) {

        if (quantidadeSigned == null || quantidadeSigned.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal preco = cotacaoService.cotacaoAtualSeguro(ticker, classe);

        TipoMovimentoCarteira tipo = quantidadeSigned.compareTo(BigDecimal.ZERO) > 0
                ? TipoMovimentoCarteira.COMPRA
                : TipoMovimentoCarteira.VENDA;

        MovimentoCarteira mov = new MovimentoCarteira(classe, ticker, tipo);
        mov.setData(LocalDate.now());
        mov.setQuantidade(quantidadeSigned.abs());

        // ⚠️ nomes dos setters: BRL maiúsculo
        mov.setPrecoUnitBRL(preco);

        // se tiver preço, calcula valor (qtd * preço)
        if (preco != null && preco.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal valor = quantidadeSigned.abs().multiply(preco).setScale(2, RoundingMode.HALF_UP);
            mov.setValorBRL(valor);
        }

        mov.setObservacao(obs);
        movRepo.save(mov);
    }

    private void registrarMovimentoAporteBRL(ClasseAtivo classe,
                                             String ticker,
                                             BigDecimal valorBRL,
                                             BigDecimal precoUnit,
                                             BigDecimal qtd,
                                             String obs) {

        MovimentoCarteira mov = new MovimentoCarteira(classe, ticker, TipoMovimentoCarteira.APORTE_BRL);
        mov.setData(LocalDate.now());

        mov.setValorBRL(valorBRL.setScale(2, RoundingMode.HALF_UP));
        mov.setPrecoUnitBRL(precoUnit.setScale(2, RoundingMode.HALF_UP));
        mov.setQuantidade(qtd);

        mov.setObservacao(obs);
        movRepo.save(mov);
    }

    @Autowired
    private AtivoCarteiraRepository ativoCarteiraRepository;

    public BigDecimal quantidadeAtual(ClasseAtivo classe, String ticker) {
        return ativoCarteiraRepository
                .findByClasseAndTicker(classe, ticker.toUpperCase())
                .map(AtivoCarteira::getQuantidade)
                .orElse(BigDecimal.ZERO);
    }


}
