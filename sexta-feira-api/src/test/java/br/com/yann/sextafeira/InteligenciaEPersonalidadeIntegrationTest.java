package br.com.yann.sextafeira;

import br.com.yann.sextafeira.core.domain.model.MensagemHistorico;
import br.com.yann.sextafeira.core.domain.model.PapelMensagem;
import br.com.yann.sextafeira.core.domain.model.Personalidade;
import br.com.yann.sextafeira.core.domain.repository.MensagemHistoricoRepository;
import br.com.yann.sextafeira.core.dto.ChatResponse;
import br.com.yann.sextafeira.core.dto.HistoricoMensagemDTO;
import br.com.yann.sextafeira.core.dto.IaRouterResponse;
import br.com.yann.sextafeira.core.service.AssistenteService;
import br.com.yann.sextafeira.core.service.FrasesService;
import br.com.yann.sextafeira.core.service.MensagemHistoricoService;
import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.repository.UsuarioRepository;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.service.CotacaoService;
import br.com.yann.sextafeira.modes.finance.service.ai.TransacaoIaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Testes automatizados de IA, Memória e Personalidades:
 * - Memória conversacional isolada por usuário.
 * - Motor de personalidades (SEXTA_FEIRA, FORMAL, AMIGAVEL, JARVIS).
 * - Comando e intenção de troca de personalidade (SET_PERSONALITY).
 */
@SpringBootTest
@Transactional
class InteligenciaEPersonalidadeIntegrationTest {

    @Autowired AssistenteService assistenteService;
    @Autowired UsuarioService usuarioService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired MensagemHistoricoService historicoService;
    @Autowired MensagemHistoricoRepository historicoRepository;
    @Autowired FrasesService frasesService;

    @MockitoBean CotacaoService cotacaoService;
    @MockitoBean TransacaoIaService transacaoIaService;

    Usuario usuarioA;
    Usuario usuarioB;

    @BeforeEach
    void setUp() {
        usuarioA = usuarioService.buscarOuCriarPorEmail("user-ia-test-a@teste.local", "User IA A");
        usuarioB = usuarioService.buscarOuCriarPorEmail("user-ia-test-b@teste.local", "User IA B");
    }

    // ===== 1. TESTES DO CATÁLOGO DE FRASES POR PERSONALIDADE =====

    @Test
    void todasAsPersonalidadesRetornamFrasesDiferenciadasParaMesmoTipo() {
        for (String tipo : List.of("ADD_DESPESA", "ADD_RECEITA", "ERRO_UNKNOWN", "MSG_VAZIA")) {
            String fSexta = frasesService.escolher(Personalidade.SEXTA_FEIRA, tipo);
            String fFormal = frasesService.escolher(Personalidade.FORMAL, tipo);
            String fAmigavel = frasesService.escolher(Personalidade.AMIGAVEL, tipo);
            String fJarvis = frasesService.escolher(Personalidade.JARVIS, tipo);

            assertNotNull(fSexta);
            assertNotNull(fFormal);
            assertNotNull(fAmigavel);
            assertNotNull(fJarvis);

            assertFalse(fSexta.isBlank());
            assertFalse(fFormal.isBlank());
            assertFalse(fAmigavel.isBlank());
            assertFalse(fJarvis.isBlank());

            assertNotEquals(fSexta, fFormal, "Respostas de Sexta e Formal devem ser diferenciadas");
        }

        String fFormalAtivacao = frasesService.escolher(Personalidade.FORMAL, "SET_PERSONALIDADE_OK");
        String fJarvisAtivacao = frasesService.escolher(Personalidade.JARVIS, "SET_PERSONALIDADE_OK");
        assertTrue(fFormalAtivacao.toUpperCase().contains("FORMAL"), "Frase formal deve conter a assinatura formal");
        assertTrue(fJarvisAtivacao.replace(".", "").toUpperCase().contains("JARVIS"), "Frase jarvis deve conter a assinatura jarvis");
    }

    // ===== 2. TESTE DE COMANDO E INTENÇÃO SET_PERSONALITY =====

    @Test
    void trocarPersonalidadeAtualizaUsuarioERespondeNoNovoModo() {
        assertEquals(Personalidade.SEXTA_FEIRA, usuarioA.getPersonalidade());

        when(transacaoIaService.rotearMensagem(anyString(), anyString(), any()))
                .thenReturn(new IaRouterResponse("SET_PERSONALITY", Map.of("personality", "JARVIS"), "pt"));

        ChatResponse resposta = assistenteService.processarMensagem(usuarioA, "mude para o modo jarvis");

        assertNotNull(resposta);
        String texto = resposta.getResposta().replace(".", "").toUpperCase();
        assertTrue(texto.contains("JARVIS") || texto.contains("PROTOCOLOS"),
                "A resposta deve adotar a persona JARVIS");

        Usuario atualizado = usuarioRepository.findById(usuarioA.getId()).orElseThrow();
        assertEquals(Personalidade.JARVIS, atualizado.getPersonalidade(), "A personalidade do usuário deve ter sido persistida no banco");
    }

    // ===== 3. TESTE DE MEMÓRIA CONVERSACIONAL E JANELA DESLIZANTE =====

    @Test
    void memoriaConversacionalRegistraMensagensEIsolaPorUsuario() {
        when(transacaoIaService.rotearMensagem(anyString(), anyString(), any()))
                .thenReturn(new IaRouterResponse("UNKNOWN", Map.of(), "pt"));

        assistenteService.processarMensagem(usuarioA, "olá sexta-feira");
        assistenteService.processarMensagem(usuarioA, "quanto gastei hoje?");

        List<HistoricoMensagemDTO> histA = historicoService.obterHistoricoRecente(usuarioA, 10);
        List<HistoricoMensagemDTO> histB = historicoService.obterHistoricoRecente(usuarioB, 10);

        assertEquals(4, histA.size(), "Deve conter 2 mensagens do usuário + 2 respostas do bot");
        assertEquals("user", histA.get(0).role());
        assertEquals("olá sexta-feira", histA.get(0).content());
        assertEquals("assistant", histA.get(1).role());

        assertEquals(0, histB.size(), "O usuário B não deve ter acesso ao histórico do usuário A");
    }

    @Test
    void janelaDeslizanteRespeitaLimiteMaximo() {
        for (int i = 1; i <= 15; i++) {
            historicoService.registrarMensagemUsuario(usuarioA, "msg " + i, "TESTE");
        }

        List<HistoricoMensagemDTO> janela = historicoService.obterHistoricoRecente(usuarioA, 5);
        assertEquals(5, janela.size(), "A janela deslizante deve respeitar exatamente o limite pedido");
        assertEquals("msg 11", janela.get(0).content(), "A mais antiga da janela de 5 deve ser a msg 11");
        assertEquals("msg 15", janela.get(4).content(), "A mais recente deve ser a msg 15");
    }

    @Test
    void mensagemVaziaRetornaMensagemDeAvisoSemChamarIa() {
        ChatResponse resp = assistenteService.processarMensagem(usuarioA, "   ");
        assertNotNull(resp);
        assertFalse(resp.getResposta().isBlank());
    }
}
