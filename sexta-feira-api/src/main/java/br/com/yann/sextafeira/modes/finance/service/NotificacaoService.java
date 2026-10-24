package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.NotificacaoUsuario;
import br.com.yann.sextafeira.modes.finance.domain.repository.NotificacaoUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacaoService {

    private final NotificacaoUsuarioRepository repository;
    private final UsuarioService usuarioService;

    public NotificacaoService(NotificacaoUsuarioRepository repository, UsuarioService usuarioService) {
        this.repository = repository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public NotificacaoUsuario criarNotificacao(Usuario usuario, String titulo, String mensagem, String tipo) {
        if (usuario == null) throw new IllegalArgumentException("Usuário obrigatório para criar notificação.");
        NotificacaoUsuario n = new NotificacaoUsuario(usuario, titulo, mensagem, tipo);
        return repository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificacaoUsuario> listarTodas() {
        return listarTodas(usuarioService.obterUsuarioAtual());
    }

    @Transactional(readOnly = true)
    public List<NotificacaoUsuario> listarTodas(Usuario usuario) {
        return repository.findByUsuarioOrderByCriadoEmDescIdDesc(usuario);
    }

    @Transactional(readOnly = true)
    public List<NotificacaoUsuario> listarNaoLidas() {
        return repository.findByUsuarioAndLidaFalseOrderByCriadoEmDescIdDesc(usuarioService.obterUsuarioAtual());
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas() {
        return repository.countByUsuarioAndLidaFalse(usuarioService.obterUsuarioAtual());
    }

    @Transactional
    public void marcarComoLida(Long notificacaoId) {
        marcarComoLida(usuarioService.obterUsuarioAtual(), notificacaoId);
    }

    @Transactional
    public void marcarComoLida(Usuario usuario, Long notificacaoId) {
        NotificacaoUsuario n = repository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada."));

        if (!n.getUsuario().getId().equals(usuario.getId())) {
            throw new SecurityException("Acesso negado: a notificação pertence a outro usuário.");
        }

        n.setLida(true);
        repository.save(n);
    }

    @Transactional
    public void marcarTodasComoLidas() {
        marcarTodasComoLidas(usuarioService.obterUsuarioAtual());
    }

    @Transactional
    public void marcarTodasComoLidas(Usuario usuario) {
        List<NotificacaoUsuario> naoLidas = repository.findByUsuarioAndLidaFalseOrderByCriadoEmDescIdDesc(usuario);
        for (NotificacaoUsuario n : naoLidas) {
            n.setLida(true);
        }
        repository.saveAll(naoLidas);
    }
}
