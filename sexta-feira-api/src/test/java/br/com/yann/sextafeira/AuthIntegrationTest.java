package br.com.yann.sextafeira;

import br.com.yann.sextafeira.core.controller.MensagemHistoricoController;
import br.com.yann.sextafeira.core.domain.model.Personalidade;
import br.com.yann.sextafeira.core.dto.HistoricoMensagemDTO;
import br.com.yann.sextafeira.core.service.MensagemHistoricoService;
import br.com.yann.sextafeira.identity.context.UsuarioContextHolder;
import br.com.yann.sextafeira.identity.controller.AuthController;
import br.com.yann.sextafeira.identity.controller.UsuarioPerfilController;
import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.dto.*;
import br.com.yann.sextafeira.identity.repository.UsuarioRepository;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.controller.NotificacaoController;
import br.com.yann.sextafeira.modes.finance.domain.model.NotificacaoUsuario;
import br.com.yann.sextafeira.modes.finance.service.NotificacaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthIntegrationTest {

    @Autowired AuthController authController;
    @Autowired UsuarioPerfilController perfilController;
    @Autowired MensagemHistoricoController historicoController;
    @Autowired NotificacaoController notificacaoController;
    @Autowired UsuarioService usuarioService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired MensagemHistoricoService mensagemHistoricoService;
    @Autowired NotificacaoService notificacaoService;

    @BeforeEach
    @AfterEach
    void cleanup() {
        UsuarioContextHolder.clear();
    }

    @Test
    void registroComDadosValidosGeraTokensEPerfil() {
        RegisterRequest req = new RegisterRequest("novo-user@sextafeira.local", "Novo Usuário", "senhaForte123");
        req.setPersonalidade(Personalidade.JARVIS);
        req.setIdiomaPreferido("en-US");

        AuthResponse res = authController.registrar(req);

        assertNotNull(res);
        assertNotNull(res.getAccessToken());
        assertNotNull(res.getRefreshToken());
        assertEquals("Bearer", res.getTokenType());
        assertEquals("novo-user@sextafeira.local", res.getUsuario().getEmail());
        assertEquals("Novo Usuário", res.getUsuario().getNome());
        assertEquals(Personalidade.JARVIS, res.getUsuario().getPersonalidade());
        assertEquals("en-US", res.getUsuario().getIdiomaPreferido());
    }

    @Test
    void registroComEmailDuplicadoFalha() {
        RegisterRequest req1 = new RegisterRequest("duplicado@sextafeira.local", "User Um", "senha123");
        authController.registrar(req1);

        RegisterRequest req2 = new RegisterRequest("duplicado@sextafeira.local", "User Dois", "senha456");
        assertThrows(IllegalArgumentException.class, () -> authController.registrar(req2));
    }

    @Test
    void loginComCredenciaisCorretasRetornaTokens() {
        RegisterRequest reg = new RegisterRequest("login-test@sextafeira.local", "User Login", "segredoValido");
        authController.registrar(reg);

        LoginRequest login = new LoginRequest("login-test@sextafeira.local", "segredoValido");
        AuthResponse authRes = authController.login(login);

        assertNotNull(authRes.getAccessToken());
        assertNotNull(authRes.getRefreshToken());
        assertEquals("login-test@sextafeira.local", authRes.getUsuario().getEmail());
    }

    @Test
    void loginComSenhaIncorretaFalha() {
        RegisterRequest reg = new RegisterRequest("login-fail@sextafeira.local", "User Fail", "senhaCorreta");
        authController.registrar(reg);

        LoginRequest loginErrado = new LoginRequest("login-fail@sextafeira.local", "senhaErrada");
        assertThrows(SecurityException.class, () -> authController.login(loginErrado));
    }

    @Test
    void renovacaoComRefreshTokenValidoGeraNovoAccessToken() {
        RegisterRequest reg = new RegisterRequest("refresh-test@sextafeira.local", "User Refresh", "senha123");
        AuthResponse authRes = authController.registrar(reg);

        AuthResponse renovado = authController.refresh(Map.of("refreshToken", authRes.getRefreshToken()));

        assertNotNull(renovado.getAccessToken());
        assertNotNull(renovado.getRefreshToken());
        assertEquals("refresh-test@sextafeira.local", renovado.getUsuario().getEmail());
    }

    @Test
    void atualizacaoDePerfilModificaPersonalidadeEIdioma() {
        RegisterRequest reg = new RegisterRequest("perfil-test@sextafeira.local", "User Perfil", "senha123");
        AuthResponse authRes = authController.registrar(reg);

        Usuario usuario = usuarioRepository.findById(authRes.getUsuario().getId()).orElseThrow();
        UsuarioContextHolder.setUsuario(usuario);

        AtualizarPerfilRequest updateReq = new AtualizarPerfilRequest(
                "Nome Alterado",
                Personalidade.AMIGAVEL,
                "en-US"
        );
        UsuarioPerfilDTO atualizado = perfilController.atualizarPerfil(updateReq);

        assertEquals("Nome Alterado", atualizado.getNome());
        assertEquals(Personalidade.AMIGAVEL, atualizado.getPersonalidade());
        assertEquals("en-US", atualizado.getIdiomaPreferido());
    }

    @Test
    void consultaDeHistoricoPorEndpointRestRetornaMensagensEmOrdemCronologica() {
        Usuario u = usuarioService.buscarOuCriarPorEmail("hist-rest@teste.local", "Hist User");
        UsuarioContextHolder.setUsuario(u);

        mensagemHistoricoService.registrarMensagemUsuario(u, "mensagem 1", "CHAT");
        mensagemHistoricoService.registrarRespostaAssistente(u, "resposta 1", "CHAT");
        mensagemHistoricoService.registrarMensagemUsuario(u, "mensagem 2", "CHAT");
        mensagemHistoricoService.registrarRespostaAssistente(u, "resposta 2", "CHAT");

        List<HistoricoMensagemDTO> historico = historicoController.obterHistorico(10);

        assertEquals(4, historico.size());
        assertEquals("user", historico.get(0).role());
        assertEquals("mensagem 1", historico.get(0).content());
        assertEquals("assistant", historico.get(3).role());
        assertEquals("resposta 2", historico.get(3).content());
    }

    @Test
    void gerenciamentoDeNotificacoesFuncionaCorretamente() {
        Usuario u = usuarioService.buscarOuCriarPorEmail("notif-rest@teste.local", "Notif User");
        UsuarioContextHolder.setUsuario(u);

        notificacaoService.criarNotificacao(u, "Aviso 1", "Mensagem 1", "INFO");
        notificacaoService.criarNotificacao(u, "Aviso 2", "Mensagem 2", "ESTOURO");

        List<NotificacaoUsuario> lista = notificacaoController.listar(false);
        assertEquals(2, lista.size());

        Map<String, Long> count = notificacaoController.contarNaoLidas();
        assertEquals(2L, count.get("count"));

        notificacaoController.marcarComoLida(lista.get(0).getId());

        count = notificacaoController.contarNaoLidas();
        assertEquals(1L, count.get("count"));

        notificacaoController.marcarTodasComoLidas();
        count = notificacaoController.contarNaoLidas();
        assertEquals(0L, count.get("count"));
    }
}
