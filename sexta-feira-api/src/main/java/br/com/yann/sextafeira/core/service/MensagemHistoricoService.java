package br.com.yann.sextafeira.core.service;

import br.com.yann.sextafeira.core.domain.model.MensagemHistorico;
import br.com.yann.sextafeira.core.domain.model.PapelMensagem;
import br.com.yann.sextafeira.core.domain.repository.MensagemHistoricoRepository;
import br.com.yann.sextafeira.core.dto.HistoricoMensagemDTO;
import br.com.yann.sextafeira.identity.domain.Usuario;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MensagemHistoricoService {

    private final MensagemHistoricoRepository repository;

    public MensagemHistoricoService(MensagemHistoricoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MensagemHistorico registrarMensagemUsuario(Usuario usuario, String mensagem, String intencao) {
        if (usuario == null || mensagem == null || mensagem.isBlank()) return null;
        MensagemHistorico registro = new MensagemHistorico(
                usuario,
                PapelMensagem.USER,
                mensagem.trim(),
                intencao
        );
        return repository.save(registro);
    }

    @Transactional
    public MensagemHistorico registrarRespostaAssistente(Usuario usuario, String resposta, String intencao) {
        if (usuario == null || resposta == null || resposta.isBlank()) return null;
        MensagemHistorico registro = new MensagemHistorico(
                usuario,
                PapelMensagem.ASSISTANT,
                resposta.trim(),
                intencao
        );
        return repository.save(registro);
    }

    /**
     * Retorna a janela deslizante de mensagens recentes do usuário em ordem cronológica (mais antiga para mais recente),
     * pronta para alimentar o contexto conversacional da LLM.
     */
    @Transactional(readOnly = true)
    public List<HistoricoMensagemDTO> obterHistoricoRecente(Usuario usuario, int limite) {
        if (usuario == null || limite <= 0) return List.of();

        List<MensagemHistorico> lista = repository.findByUsuarioOrderByCriadoEmDescIdDesc(
                usuario,
                PageRequest.of(0, limite)
        );

        if (lista.isEmpty()) return List.of();

        List<HistoricoMensagemDTO> resultado = new ArrayList<>(lista.size());
        // Inverte para ficar em ordem cronológica ASC (diálogo natural)
        for (int i = lista.size() - 1; i >= 0; i--) {
            MensagemHistorico m = lista.get(i);
            String role = (m.getPapel() == PapelMensagem.USER) ? "user" : "assistant";
            resultado.add(new HistoricoMensagemDTO(role, m.getConteudo()));
        }

        return resultado;
    }
}
