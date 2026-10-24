package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.AtivoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.domain.model.MovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoMovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.repository.AtivoCarteiraRepository;
import br.com.yann.sextafeira.modes.finance.domain.repository.MovimentoCarteiraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class CarteiraService {

    private final AtivoCarteiraRepository repo;
    private final CotacaoService cotacaoService;
    private final MovimentoCarteiraRepository movRepo;
    private final UsuarioService usuarioService;

    public CarteiraService(AtivoCarteiraRepository repo,
                           MovimentoCarteiraRepository movRepo,
                           CotacaoService cotacaoService,
                           UsuarioService usuarioService) {
        this.repo = repo;
        this.movRepo = movRepo;
        this.cotacaoService = cotacaoService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public AtivoCarteira adicionarQuantidade(
            ClasseAtivo classe,
            String ticker,
            BigDecimal quantidadeSigned
    ) {
        return adicionarQuantidade(usuarioService.obterUsuarioAtual(), classe, ticker, quantidadeSigned);
    }

    @Transactional
    public AtivoCarteira adicionarQuantidade(
            Usuario usuario,
            ClasseAtivo classe,
            String ticker,
            BigDecimal quantidadeSigned
    ) {
        if (quantidadeSigned == null || quantidadeSigned.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("A quantidade movimentada deve ser diferente de zero.");
        }

        final String tickerFinal = validarTicker(ticker);

        AtivoCarteira ativo = repo.findByUsuarioAndClasseAndTicker(usuario, classe, tickerFinal)
                .orElseGet(() -> new AtivoCarteira(usuario, classe, tickerFinal));

        BigDecimal qtdAtual = ativo.getQuantidade() == null ? BigDecimal.ZERO : ativo.getQuantidade();
        BigDecimal novaQtd = qtdAtual.add(quantidadeSigned);

        // [FIN-02] Impede venda de ativo sem saldo suficiente
        if (quantidadeSigned.compareTo(BigDecimal.ZERO) < 0 && novaQtd.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    String.format("Saldo insuficiente de %s para venda (disponível: %.8f).", tickerFinal, qtdAtual)
            );
        }

        ativo.setQuantidade(novaQtd);

        registrarMovimentoQuantidade(
                usuario,
                classe,
                tickerFinal,
                quantidadeSigned,
                "ajuste por quantidade"
        );

        if (novaQtd.compareTo(BigDecimal.ZERO) == 0) {
            if (ativo.getId() != null) repo.delete(ativo);
            return ativo;
        }

        return repo.save(ativo);
    }

    private AtivoCarteira atualizarPosicaoSemMovimento(
            Usuario usuario,
            ClasseAtivo classe,
            String ticker,
            BigDecimal quantidadeSigned
    ) {
        final String tickerFinal = validarTicker(ticker);

        AtivoCarteira ativo = repo.findByUsuarioAndClasseAndTicker(usuario, classe, tickerFinal)
                .orElseGet(() -> new AtivoCarteira(usuario, classe, tickerFinal));

        BigDecimal novaQtd = ativo.getQuantidade().add(quantidadeSigned);
        ativo.setQuantidade(novaQtd);

        if (novaQtd.compareTo(BigDecimal.ZERO) <= 0) {
            if (ativo.getId() != null) repo.delete(ativo);
            return ativo;
        }

        return repo.save(ativo);
    }

    @Transactional
    public AtivoCarteira adicionarPorValorBRL(
            ClasseAtivo classe,
            String ticker,
            BigDecimal valorBRL
    ) {
        return adicionarPorValorBRL(usuarioService.obterUsuarioAtual(), classe, ticker, valorBRL);
    }

    // [BUG-02] Corrige dupla contabilização: registra APORTE_BRL e atualiza posição sem gerar COMPRA duplicada
    @Transactional
    public AtivoCarteira adicionarPorValorBRL(
            Usuario usuario,
            ClasseAtivo classe,
            String ticker,
            BigDecimal valorBRL
    ) {
        if (valorBRL == null || valorBRL.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do aporte em BRL deve ser maior que zero.");
        }

        final String tickerFinal = validarTicker(ticker);

        BigDecimal precoUnit = cotacaoService.cotacaoAtual(tickerFinal, classe);
        if (precoUnit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Não consegui pegar cotação de " + tickerFinal);
        }

        BigDecimal qtd = valorBRL.divide(precoUnit, 8, RoundingMode.HALF_UP);

        registrarMovimentoAporteBRL(
                usuario,
                classe,
                tickerFinal,
                valorBRL,
                precoUnit,
                qtd,
                "aporte em BRL"
        );

        return atualizarPosicaoSemMovimento(usuario, classe, tickerFinal, qtd);
    }

    public List<AtivoCarteira> listarTudo() {
        return listarTudo(usuarioService.obterUsuarioAtual());
    }

    public List<AtivoCarteira> listarTudo(Usuario usuario) {
        return repo.findByUsuarioOrderByTickerAsc(usuario);
    }

    public List<AtivoCarteira> listarPorClasse(ClasseAtivo classe) {
        return listarPorClasse(usuarioService.obterUsuarioAtual(), classe);
    }

    public List<AtivoCarteira> listarPorClasse(Usuario usuario, ClasseAtivo classe) {
        return repo.findByUsuarioAndClasseOrderByTickerAsc(usuario, classe);
    }

    public BigDecimal quantidadeAtual(ClasseAtivo classe, String ticker) {
        return quantidadeAtual(usuarioService.obterUsuarioAtual(), classe, ticker);
    }

    public BigDecimal quantidadeAtual(Usuario usuario, ClasseAtivo classe, String ticker) {
        return repo.findByUsuarioAndClasseAndTicker(usuario, classe, validarTicker(ticker))
                .map(AtivoCarteira::getQuantidade)
                .orElse(BigDecimal.ZERO);
    }

    private void registrarMovimentoQuantidade(Usuario usuario,
                                              ClasseAtivo classe,
                                              String ticker,
                                              BigDecimal quantidadeSigned,
                                              String obs) {
        if (quantidadeSigned == null || quantidadeSigned.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal preco = cotacaoService.cotacaoAtualSeguro(ticker, classe);

        TipoMovimentoCarteira tipo = quantidadeSigned.compareTo(BigDecimal.ZERO) > 0
                ? TipoMovimentoCarteira.COMPRA
                : TipoMovimentoCarteira.VENDA;

        MovimentoCarteira mov = new MovimentoCarteira(usuario, classe, ticker, tipo);
        mov.setData(LocalDate.now());
        mov.setQuantidade(quantidadeSigned.abs());
        mov.setPrecoUnitBRL(preco);

        if (preco != null && preco.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal valor = quantidadeSigned.abs().multiply(preco).setScale(2, RoundingMode.HALF_UP);
            mov.setValorBRL(valor);
        }

        mov.setObservacao(obs);
        movRepo.save(mov);
    }

    private void registrarMovimentoAporteBRL(Usuario usuario,
                                             ClasseAtivo classe,
                                             String ticker,
                                             BigDecimal valorBRL,
                                             BigDecimal precoUnit,
                                             BigDecimal qtd,
                                             String obs) {
        MovimentoCarteira mov = new MovimentoCarteira(usuario, classe, ticker, TipoMovimentoCarteira.APORTE_BRL);
        mov.setData(LocalDate.now());
        mov.setValorBRL(valorBRL.setScale(2, RoundingMode.HALF_UP));
        mov.setPrecoUnitBRL(precoUnit.setScale(2, RoundingMode.HALF_UP));
        mov.setQuantidade(qtd);
        mov.setObservacao(obs);
        movRepo.save(mov);
    }

    private String validarTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker do ativo é obrigatório.");
        }
        String limpo = ticker.trim().toUpperCase();
        if (!limpo.matches("^[A-Z0-9]{2,15}$")) {
            throw new IllegalArgumentException("Formato de ticker inválido.");
        }
        return limpo;
    }
}
