package br.com.yann.sextafeira;

import br.com.yann.sextafeira.core.dto.ConvertRequest;
import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.security.JwtService;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.domain.model.MovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.NotificacaoUsuario;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoMovimentoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.modes.finance.domain.repository.MovimentoCarteiraRepository;
import br.com.yann.sextafeira.modes.finance.dto.OrcamentoRequest;
import br.com.yann.sextafeira.modes.finance.service.CarteiraService;
import br.com.yann.sextafeira.modes.finance.service.CotacaoService;
import br.com.yann.sextafeira.modes.finance.service.CurrencyService;
import br.com.yann.sextafeira.modes.finance.service.NotificacaoService;
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
 * Testes de isolamento multiusuário, segurança JWT e integridade financeira (Fase 2).
 * Todos os dados são fictícios; cotações e NLP são simulados (sem rede, sem dados reais).
 */
@SpringBootTest
@Transactional
class IsolamentoUsuarioIntegrationTest {

    @Autowired UsuarioService usuarioService;
    @Autowired TransacaoService transacaoService;
    @Autowired OrcamentoService orcamentoService;
    @Autowired CarteiraService carteiraService;
    @Autowired SnapshotCarteiraService snapshotCarteiraService;
    @Autowired MovimentoCarteiraRepository movimentoRepo;
    @Autowired CurrencyService currencyService;
    @Autowired JwtService jwtService;
    @Autowired NotificacaoService notificacaoService;

    @MockitoBean CotacaoService cotacaoService;
    @MockitoBean TransacaoIaService transacaoIaService;

    Usuario usuarioA;
    Usuario usuarioB;
    LocalDate hoje;

    @BeforeEach
    void setUp() {
        usuarioA = usuarioService.buscarOuCriarPorEmail("user-a@teste.local", "Usuário A");
        usuarioB = usuarioService.buscarOuCriarPorEmail("user-b@teste.local", "Usuário B");
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
    void usuarioANaoLeTransacoesDoUsuarioB() {
        transacaoService.salvar(usuarioA, despesa("100.00", CategoriaTransacao.ALIMENTACAO));
        transacaoService.salvar(usuarioB, despesa("250.00", CategoriaTransacao.TRANSPORTE));

        var listaA = transacaoService.listarTodas(usuarioA);
        var listaB = transacaoService.listarTodas(usuarioB);

        assertEquals(1, listaA.size());
        assertEquals(new BigDecimal("100.00"), listaA.get(0).getValor());

        assertEquals(1, listaB.size());
        assertEquals(new BigDecimal("250.00"), listaB.get(0).getValor());
    }

    @Test
    void usuarioANaoRemoveTransacaoDoUsuarioB() {
        transacaoService.salvar(usuarioA, despesa("50.00", CategoriaTransacao.LAZER));
        transacaoService.salvar(usuarioB, despesa("300.00", CategoriaTransacao.MORADIA));

        var removida = transacaoService.removerUltimaTransacao(usuarioA);
        assertEquals(new BigDecimal("50.00"), removida.getValor());

        var listaB = transacaoService.listarTodas(usuarioB);
        assertEquals(1, listaB.size());
        assertEquals(new BigDecimal("300.00"), listaB.get(0).getValor());
    }

    @Test
    void resumoMensalCalculaApenasDadosDoProprioUsuario() {
        transacaoService.salvar(usuarioA, despesa("80.00", CategoriaTransacao.ALIMENTACAO));
        transacaoService.salvar(usuarioB, despesa("999.00", CategoriaTransacao.ALIMENTACAO));

        var resumoA = transacaoService.calcularResumoMensal(usuarioA, hoje.getYear(), hoje.getMonthValue());
        assertEquals(new BigDecimal("80.00"), resumoA.getTotalDespesas());
    }

    // ===== [SEC-02] Orçamentos =====

    @Test
    void orcamentosSaoIndependentesPorUsuario() {
        OrcamentoRequest reqA = new OrcamentoRequest();
        reqA.setAno(hoje.getYear());
        reqA.setMes(hoje.getMonthValue());
        reqA.setCategoria(CategoriaTransacao.ALIMENTACAO);
        reqA.setValor(new BigDecimal("500.00"));
        orcamentoService.definirOrcamento(usuarioA, reqA);

        OrcamentoRequest reqB = new OrcamentoRequest();
        reqB.setAno(hoje.getYear());
        reqB.setMes(hoje.getMonthValue());
        reqB.setCategoria(CategoriaTransacao.ALIMENTACAO);
        reqB.setValor(new BigDecimal("1500.00"));
        orcamentoService.definirOrcamento(usuarioB, reqB);

        var statusA = orcamentoService.consultarStatus(usuarioA, hoje.getYear(), hoje.getMonthValue(), CategoriaTransacao.ALIMENTACAO);
        var statusB = orcamentoService.consultarStatus(usuarioB, hoje.getYear(), hoje.getMonthValue(), CategoriaTransacao.ALIMENTACAO);

        assertEquals(new BigDecimal("500.00"), statusA.getLimite());
        assertEquals(new BigDecimal("1500.00"), statusB.getLimite());
    }

    // ===== [SEC-02] Carteira de Investimentos =====

    @Test
    void doisUsuariosPodemTerOMesmoAtivoSemColisaoDeSchema() {
        carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "PETR4", new BigDecimal("10"));
        carteiraService.adicionarQuantidade(usuarioB, ClasseAtivo.ACAO, "PETR4", new BigDecimal("25"));

        assertEquals(new BigDecimal("10"), carteiraService.quantidadeAtual(usuarioA, ClasseAtivo.ACAO, "PETR4"));
        assertEquals(new BigDecimal("25"), carteiraService.quantidadeAtual(usuarioB, ClasseAtivo.ACAO, "PETR4"));

        var listaA = carteiraService.listarTudo(usuarioA);
        var listaB = carteiraService.listarTudo(usuarioB);
        assertEquals(1, listaA.size());
        assertEquals(1, listaB.size());
    }

    // ===== [BUG-02] Dupla contabilização de aporte em reais =====

    @Test
    void aportePorValorRegistraApenasUmMovimento() {
        carteiraService.adicionarPorValorBRL(usuarioA, ClasseAtivo.ACAO, "VALE3", new BigDecimal("50.00"));

        var movs = movimentosHoje(usuarioA);
        assertEquals(1, movs.size(), "Deve registrar exatamente 1 movimento para a compra por valor BRL");
        assertEquals(TipoMovimentoCarteira.APORTE_BRL, movs.get(0).getTipo());
        assertEquals(new BigDecimal("50.00"), movs.get(0).getValorBRL());
    }

    // ===== [FIN-02] Venda sem saldo =====

    @Test
    void vendaSemSaldoERejeitadaENaoRegistraMovimento() {
        assertThrows(IllegalArgumentException.class, () ->
                carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "ITUB4", new BigDecimal("-5")));

        var movs = movimentosHoje(usuarioA);
        assertTrue(movs.isEmpty(), "Não deve haver movimentos gravados para tentativa de venda sem saldo");
    }

    // ===== [SEC-02] Snapshots da Carteira =====

    @Test
    void snapshotDoDiaSubstituiApenasDoUsuarioSolicitante() {
        carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "BBDC4", new BigDecimal("10"));
        carteiraService.adicionarQuantidade(usuarioB, ClasseAtivo.ACAO, "BBDC4", new BigDecimal("30"));

        snapshotCarteiraService.gerarSnapshotDoDia(usuarioA, hoje, null);
        snapshotCarteiraService.gerarSnapshotDoDia(usuarioB, hoje, null);

        var snapA1 = snapshotCarteiraService.valorTotalNoDia(usuarioA, hoje, null);
        var snapB1 = snapshotCarteiraService.valorTotalNoDia(usuarioB, hoje, null);
        assertEquals(new BigDecimal("100.00"), snapA1); // 10 * 10.00
        assertEquals(new BigDecimal("300.00"), snapB1); // 30 * 10.00

        // Atualiza posição apenas de A e regenera snapshot de A
        carteiraService.adicionarQuantidade(usuarioA, ClasseAtivo.ACAO, "BBDC4", new BigDecimal("5"));
        snapshotCarteiraService.gerarSnapshotDoDia(usuarioA, hoje, null);

        var snapA2 = snapshotCarteiraService.valorTotalNoDia(usuarioA, hoje, null);
        var snapB2 = snapshotCarteiraService.valorTotalNoDia(usuarioB, hoje, null);
        assertEquals(new BigDecimal("150.00"), snapA2); // 15 * 10.00
        assertEquals(new BigDecimal("300.00"), snapB2, "Snapshot de B não deve ser afetado pela regeracao de A");
    }

    // ===== [SEC-03] Sanitização contra SSRF / Ticker Malicioso =====

    @Test
    void tickerComCaracteresInvalidosERejeitado() {
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

    // ===== [SEC-03] Autenticação JWT e Notificações =====

    @Test
    void tokenJwtValidoIdentificaUsuarioETokenInvalidoERejeitado() {
        String tokenA = jwtService.gerarAccessToken(usuarioA.getId(), usuarioA.getEmail());
        assertNotNull(tokenA);
        assertTrue(jwtService.isTokenValido(tokenA));
        assertEquals(usuarioA.getId(), jwtService.extrairUserId(tokenA));
        assertEquals(usuarioA.getEmail(), jwtService.extrairEmail(tokenA));

        assertFalse(jwtService.isTokenValido("token-completamente-invalido"));
        assertFalse(jwtService.isTokenValido(tokenA + "-adulterado"));
    }

    @Test
    void notificacoesSaoIsoladasEntreUsuarios() {
        notificacaoService.criarNotificacao(usuarioA, "Aviso A", "Mensagem privada A", "INFO");
        notificacaoService.criarNotificacao(usuarioB, "Aviso B", "Mensagem privada B", "ESTOURO");

        List<NotificacaoUsuario> notifsA = notificacaoService.listarTodas(usuarioA);
        List<NotificacaoUsuario> notifsB = notificacaoService.listarTodas(usuarioB);

        assertEquals(1, notifsA.size());
        assertEquals("Aviso A", notifsA.get(0).getTitulo());

        assertEquals(1, notifsB.size());
        assertEquals("Aviso B", notifsB.get(0).getTitulo());

        assertThrows(SecurityException.class, () ->
                notificacaoService.marcarComoLida(usuarioB, notifsA.get(0).getId()));
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
