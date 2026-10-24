package br.com.yann.sextafeira;

import br.com.yann.sextafeira.core.domain.model.MensagemHistorico;
import br.com.yann.sextafeira.core.domain.model.PapelMensagem;
import br.com.yann.sextafeira.core.domain.model.Personalidade;
import br.com.yann.sextafeira.core.domain.repository.MensagemHistoricoRepository;
import br.com.yann.sextafeira.core.dto.ChatResponse;
import br.com.yann.sextafeira.core.dto.HistoricoMensagemDTO;
import br.com.yann.sextafeira.core.dto.IaRouterResponse;
import br.com.yann.sextafeira.core.integration.whatsapp.WhatsappService;
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
 * Testes automatizados da Etapa 4:
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
    @MockitoBean WhatsappService whatsappService;

    Usuario usuarioA;
    Usuario usuarioB;

    @BeforeEach
    void setUp() {
        usuarioA = usuarioService.buscarOuCriarPorWhatsapp("user-ia-test-a");
        usuarioB = usuarioService.buscarOuCriarPorWhatsapp("user-ia-test-b");
    }

    // ===== 1. TESTES DO CATÁLOGO DE FRASES POR PERSONALIDADE =====

    @Test
    void frasesServiceRetornaFrasesDistintasPorPersonalidade() {
        String msgFormal = frasesService.escolher(Personalidade.FORMAL, "SET_PERSONALIDADE_OK");
        String msgJarvis = frasesService.escolher(Personalidade.JARVIS, "SET_PERSONALIDADE_OK");
        String msgAmigavel = frasesService.escolher(Personalidade.AMIGAVEL, "SET_PERSONALIDADE_OK");
        String msgSexta = frasesService.escolher(Personalidade.SEXTA_FEIRA, "SET_PERSONALIDADE_OK");

        assertTrue(msgFormal.toUpperCase().contains("FORMAL"));
        assertTrue(msgJarvis.contains("J.A.R.V.I.S.") || msgJarvis.toLowerCase().contains("senhor"));
        assertTrue(msgAmigavel.toUpperCase().contains("AMIGÁVEL") || msgAmigavel.contains("carinho"));
        assertTrue(msgSexta.toUpperCase().contains("SEXTA-FEIRA") || msgSexta.contains("sarcasmo"));
    }

    // ===== 2. TESTES DE TROCA DE PERSONALIDADE (SET_PERSONALITY) =====

    @Test
    void trocaDePersonalidadeParaFormalAtualizaUsuarioEIsolaEntreUsuarios() {
        when(transacaoIaService.rotearMensagem(anyString(), any(), any()))
                .thenReturn(new IaRouterResponse("SET_PERSONALITY", Map.of("personality", "FORMAL"), "pt"));

        ChatResponse respA = assistenteService.processarMensagem(usuarioA, "mude para o modo formal");
        assertTrue(respA.getResposta().contains("FORMAL"));

        // Recarrega usuário do banco para conferir persistência
        Usuario atualizadoA = usuarioRepository.findById(usuarioA.getId()).orElseThrow();
        assertEquals(Personalidade.FORMAL, atualizadoA.getPersonalidade());

        // Usuário B permanece inalterado com a personalidade padrão
        Usuario atualizadoB = usuarioRepository.findById(usuarioB.getId()).orElseThrow();
        assertEquals(Personalidade.SEXTA_FEIRA, atualizadoB.getPersonalidade());
    }

    @Test
    void trocaDePersonalidadeParaJarvisEAmigavel() {
        when(transacaoIaService.rotearMensagem(anyString(), any(), any()))
                .thenReturn(new IaRouterResponse("SET_PERSONALITY", Map.of("personality", "JARVIS"), "pt"));

        ChatResponse respJarvis = assistenteService.processarMensagem(usuarioA, "ative o jarvis");
        assertTrue(respJarvis.getResposta().contains("J.A.R.V.I.S."));
        assertEquals(Personalidade.JARVIS, usuarioRepository.findById(usuarioA.getId()).orElseThrow().getPersonalidade());

        when(transacaoIaService.rotearMensagem(anyString(), any(), any()))
                .thenReturn(new IaRouterResponse("SET_PERSONALITY", Map.of("personality", "AMIGAVEL"), "pt"));

        ChatResponse respAmigavel = assistenteService.processarMensagem(usuarioA, "seja mais amigável");
        assertTrue(respAmigavel.getResposta().contains("AMIGÁVEL"));
        assertEquals(Personalidade.AMIGAVEL, usuarioRepository.findById(usuarioA.getId()).orElseThrow().getPersonalidade());
    }

    // ===== 3. TESTES DE MEMÓRIA CONVERSACIONAL ISOLADA POR USUÁRIO =====

    @Test
    void mensagensSaoGravadasNoHistoricoIsoladoPorUsuario() {
        when(transacaoIaService.rotearMensagem(anyString(), any(), any()))
                .thenReturn(new IaRouterResponse("UNKNOWN", Map.of(), "pt"));

        assistenteService.processarMensagem(usuarioA, "olá assistente");
        assistenteService.processarMensagem(usuarioA, "segunda mensagem do usuário A");

        assertEquals(4, historicoRepository.countByUsuario(usuarioA)); // 2 do usuário + 2 respostas do bot
        assertEquals(0, historicoRepository.countByUsuario(usuarioB)); // Usuário B não tem mensagens

        List<HistoricoMensagemDTO> janelaA = historicoService.obterHistoricoRecente(usuarioA, 10);
        assertEquals(4, janelaA.size());
        assertEquals("user", janelaA.get(0).getRole());
        assertEquals("olá assistente", janelaA.get(0).getContent());
        assertEquals("assistant", janelaA.get(1).getRole());
    }

    @Test
    void mensagemVaziaRetornaMensagemDePersonalidadeSemChamarIA() {
        ChatResponse respVazia = assistenteService.processarMensagem(usuarioA, "   ");
        assertNotNull(respVazia);
        assertFalse(respVazia.getResposta().isBlank());
    }
}
