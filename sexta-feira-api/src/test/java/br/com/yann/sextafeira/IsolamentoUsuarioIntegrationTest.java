package br.com.yann.sextafeira;

import br.com.yann.sextafeira.core.dto.ConvertRequest;
import br.com.yann.sextafeira.core.integration.whatsapp.WhatsappService;
import br.com.yann.sextafeira.core.integration.whatsapp.dto.WhatsappIncomingMessageDTO;
import br.com.yann.sextafeira.core.webhook.WhatsappWebhookController;
import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.domain.model.MovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoMovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.modes.finance.domain.repository.MovimentoCarteiraRepository;
import br.com.yann.sextafeira.modes.finance.dto.OrcamentoRequest;
import br.com.yann.sextafeira.modes.finance.service.CarteiraService;
import br.com.yann.sextafeira.modes.finance.service.CotacaoService;
import br.com.yann.sextafeira.modes.finance.service.CurrencyService;
import br.com.yann.sextafeira.modes.finance.service.OrcamentoService;
import br.com.yann.sextafeira.modes.finance.service.SnapshotCarteiraService;
import br.com.yann.sextafeira.modes.finance.service.TransacaoService;
import br.com.yann.sextafeira.modes.finance.service.ai.TransacaoIaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Testes de isolamento multiusuário e integridade financeira (Etapa 3).
 * Todos os dados são fictícios; cotações e NLP são simulados (sem rede, sem dados reais).
 */
@SpringBootTest(properties = "whatsapp.webhook.secret=segredo-de-teste-ficticio")
@Transactional
class IsolamentoUsuarioIntegrationTest {

    @Autowired UsuarioService usuarioService;
    @Autowired TransacaoService transacaoService;
    @Autowired OrcamentoService orcamentoService;
    @Autowired CarteiraService carteiraService;
    @Autowired SnapshotCarteiraService snapshotCarteiraService;
    @Autowired MovimentoCarteiraRepository movimentoRepo;
    @Autowired WhatsappWebhookController webhookController;
    @Autowired CurrencyService currencyService;

    @MockitoBean CotacaoService cotacaoService;
    @MockitoBean TransacaoIaService transacaoIaService;
    @MockitoBean WhatsappService whatsappService;

    Usuario usuarioA;
    Usuario usuarioB;
    LocalDate hoje;

    @BeforeEach
    void setUp() {
        usuarioA = usuarioService.buscarOuCriarPorWhatsapp("teste-usuario-a");
        usuarioB = usuarioService.buscarOuCriarPorWhatsapp("teste-usuario-b");
        hoje = LocalDate.now();
        when(cotacaoService.cotacaoAtual(anyString(), any())).thenReturn(new BigDecimal("10.00"));
        when(cotacaoService.cotacaoAtualSeguro(anyString(), any())).thenReturn(new BigDecimal("10.00"));
    }

    private Transacao despesa(String valor, CategoriaTransacao cat) {
        Transacao t = new Transacao();
        t.setValor(new BigDecimal(valor));
        t.setTipo(TipoTransacao.DESPESA);
        t.setCategoria(cat);
        t.setData(hoje);
        t.setDescricao("lançamento fictício de teste");
        return t;
    }

    private List<MovimentoCarteira> movimentosHoje(Usuario u) {
        return movimentoRepo.findByUsuarioAndDataBetweenOrderByDataAscIdAsc(u, hoje.minusDays(1), hoje.plusDays(1));
    }

    // ===== [SEC-02] Transações =====

    @Test
    void transacoesSaoIsoladasPorUsuario() {
        transacaoService.salvar(usuarioA, despesa("100.00", CategoriaTransacao.ALIMENTACAO));
        transacaoService.salvar(usuarioB, despesa("50.00", CategoriaTransacao.ALIMENTACAO));

        assertEquals(1, transacaoService.listarTodas(usuarioA).size());
        assertEquals(1, transacaoService.listarTodas(usuarioB).size());

        YearMonth ym = YearMonth.from(hoje);
        assertEquals(0, new BigDecimal("50.00").compareTo(
                transacaoService.calcularResumoMensal(usuarioB, ym.getYear(), ym.getMonthValue()).getTotalDespesas()));
        assertEquals(0, new BigDecimal("100.00").compareTo(
                transacaoService.calcularResumoMensal(usuarioA, ym.getYear(), ym.getMonthValue()).getTotalDespesas()));
    }

    @Test
    void usuarioBNaoRemoveUltimaTransacaoDoUsuarioA() {
        transacaoService.salvar(usuarioA, despesa("100.00", CategoriaTransacao.ALIMENTACAO));

        assertThrows(RuntimeException.class, () -> transacaoService.removerUltimaTransacao(usuarioB));
        assertEquals(1, transacaoService.listarTodas(usuarioA).size());
    }

    @Test
    void usuarioBNaoRemovePorTextoTransacaoDoUsuarioA() {
        transacaoService.salvar(usuarioA, despesa("100.00", CategoriaTransacao.ALIMENTACAO));
        when(transacaoIaService.interpretarMensagem(anyString()))
                .thenReturn(despesa("100.00", CategoriaTransacao.ALIMENTACAO));

        assertThrows(RuntimeException.class,
                () -> transacaoService.removerPorTexto(usuarioB, "apaga o gasto de 100 no mercado"));
        assertEquals(1, transacaoService.listarTodas(usuarioA).size());
    }

    @Test
    void transacaoComValorNaoPositivoERejeitada() {
        assertThrows(IllegalArgumentException.class,
                () -> transacaoService.salvar(usuarioA, despesa("0", CategoriaTransacao.OUTROS)));
        assertThrows(IllegalArgumentException.class,
                () -> transacaoService.salvar(usuarioA, despesa("-5", CategoriaTransacao.OUTROS)));
    }

    // ===== [SEC-02] Orçamentos =====

    @Test
    void orcamentosSaoIsoladosEMesmaCategoriaNaoColide() {
        YearMonth ym = YearMonth.from(hoje);
        orcamentoService.definirOrcamento(usuarioA, orcamento(ym, "1000.00"));

        var statusB = orcamentoService.consultarStatus(usuarioB, ym.getYear(), ym.getMonthValue(), CategoriaTransacao.ALIMENTACAO);
        assertEquals(0, BigDecimal.ZERO.compareTo(statusB.getLimite()));

        assertDoesNotThrow(() -> orcamentoService.definirOrcamento(usuarioB, orcamento(ym, "300.00")));

        var statusA = orcamentoService.consultarStatus(usuarioA, ym.getYear(), ym.getMonthValue(), CategoriaTransacao.ALIMENTACAO);
        assertEquals(0, new BigDecimal("1000.00").compareTo(statusA.getLimite()));
    }

    private OrcamentoRequest orcamento(YearMonth ym, String valor) {
        OrcamentoRequest req = new OrcamentoRequest();
        req.setAno(ym.getYear());
        req.setMes(ym.getMonthValue());
        req.setCategoria(CategoriaTransacao.ALIMENTACAO);
        req.setValorLimite(new BigDecimal(valor));
        return req;
    }

    // ===== [SEC-02] Carteira =====

    @Test
    void doisUsuariosPodemTerOMesmoTickerSemColisaoEIsolados() {
        carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "PETR4", new BigDecimal("10"));
        assertDoesNotThrow(() ->
                carteiraService.adicionarQuantidade(usuarioB, ClasseAtivo.ACAO, "PETR4", new BigDecimal("3")));

        assertEquals(1, carteiraService.listarTudo(usuarioB).size());
        assertEquals(0, new BigDecimal("3").compareTo(
                carteiraService.quantidadeAtual(usuarioB, ClasseAtivo.ACAO, "PETR4")));

        carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "PETR4", new BigDecimal("-10"));
        assertTrue(carteiraService.listarTudo(usuarioA).isEmpty());
        assertEquals(1, carteiraService.listarTudo(usuarioB).size());
    }

    // ===== [BUG-02] Aporte por valor gera um único movimento =====

    @Test
    void aportePorValorRegistraApenasUmMovimento() {
        carteiraService.adicionarPorValorBRL(usuarioA, ClasseAtivo.CRIPTO, "BTC", new BigDecimal("100.00"));

        List<MovimentoCarteira> movs = movimentosHoje(usuarioA);
        assertEquals(1, movs.size(), "Aporte por valor não pode gerar COMPRA duplicada");
        assertEquals(TipoMovimentoCarteira.APORTE_BRL, movs.get(0).getTipo());
        assertEquals(0, new BigDecimal("10").compareTo(
                carteiraService.quantidadeAtual(usuarioA, ClasseAtivo.CRIPTO, "BTC")));
    }

    // ===== [FIN-02] Venda sem saldo =====

    @Test
    void vendaSemSaldoERejeitadaENaoRegistraMovimento() {
        assertThrows(IllegalArgumentException.class, () ->
                carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "BBAS3", new BigDecimal("-5")));
        assertTrue(movimentosHoje(usuarioA).isEmpty());
    }

    @Test
    void tickerInvalidoERejeitado() {
        assertThrows(IllegalArgumentException.class, () ->
                carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "../x?y=1", BigDecimal.ONE));
    }

    // ===== [CONC-01] Contexto de ranking por usuário =====

    @Test
    void contextoDeRankingNaoVazaEntreUsuarios() {
        snapshotCarteiraService.topMelhoresEPioresNoPeriodo(usuarioA, hoje.minusDays(7), hoje, null, 3);

        assertNotNull(snapshotCarteiraService.getUltimoRankingContext(usuarioA));
        assertNull(snapshotCarteiraService.getUltimoRankingContext(usuarioB));
    }

    // ===== [SEC-03] Webhook e validação de entradas externas =====

    @Test
    void webhookRejeitaTokenInvalidoOuAusente() {
        var msg = new WhatsappIncomingMessageDTO("teste-usuario-a", "quanto gastei hoje");
        assertThrows(SecurityException.class, () -> webhookController.receberMensagem("token-errado", msg));
        assertThrows(SecurityException.class, () -> webhookController.receberMensagem(null, msg));
    }

    @Test
    void conversaoRejeitaCodigoDeMoedaMalicioso() {
        ConvertRequest req = new ConvertRequest();
        req.setAmount(BigDecimal.TEN);
        req.setFrom("USD&x=1");
        req.setTo("BRL");
        assertThrows(IllegalArgumentException.class, () -> currencyService.converter(req));
    }
}
