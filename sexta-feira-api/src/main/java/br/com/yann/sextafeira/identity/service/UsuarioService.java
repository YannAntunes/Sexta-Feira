package br.com.yann.sextafeira.identity.service;

import br.com.yann.sextafeira.identity.context.UsuarioContextHolder;
import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Value("${sexta.auth.default-whatsapp:local-dev-user}")
    private String defaultWhatsapp;

    @Value("${sexta.auth.require-header:false}")
    private boolean requireHeader;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario buscarOuCriarPorWhatsapp(String whatsappNumber) {
        if (whatsappNumber == null || whatsappNumber.isBlank()) {
            throw new IllegalArgumentException("Identificador de WhatsApp do usuário não pode ser vazio.");
        }
        final String normalizado = whatsappNumber.trim();
        return usuarioRepository.findByWhatsappNumber(normalizado)
                .orElseGet(() -> criarNovoUsuario(normalizado));
    }

    @Transactional
    public Usuario obterUsuarioAtual() {
        Usuario ctx = UsuarioContextHolder.getUsuario();
        if (ctx != null) {
            return ctx;
        }
        if (requireHeader) {
            throw new SecurityException("Identificação de usuário obrigatória (cabeçalho X-User-Whatsapp ausente).");
        }
        return buscarOuCriarPorWhatsapp(defaultWhatsapp);
    }

    private Usuario criarNovoUsuario(String whatsappNumber) {
        Usuario u = new Usuario();
        u.setWhatsappNumber(whatsappNumber);
        return usuarioRepository.save(u);
    }
}
