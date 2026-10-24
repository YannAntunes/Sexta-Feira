package br.com.yann.sextafeira.identity.service;

import br.com.yann.sextafeira.identity.context.UsuarioContextHolder;
import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Value("${sexta.auth.default-email:admin@sextafeira.local}")
    private String defaultEmail;

    @Value("${sexta.auth.require-header:false}")
    private boolean requireHeader;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return usuarioRepository.findByEmail(email.trim().toLowerCase());
    }

    @Transactional
    public Usuario buscarOuCriarPorEmail(String email, String nome) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail não pode ser vazio.");
        }
        final String normalizado = email.trim().toLowerCase();
        return usuarioRepository.findByEmail(normalizado)
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setEmail(normalizado);
                    u.setNome(nome != null && !nome.isBlank() ? nome.trim() : normalizado);
                    u.setCriadoEm(LocalDateTime.now());
                    return usuarioRepository.save(u);
                });
    }

    @Deprecated
    @Transactional
    public Usuario buscarOuCriarPorWhatsapp(String whatsappNumber) {
        if (whatsappNumber == null || whatsappNumber.isBlank()) {
            throw new IllegalArgumentException("Identificador de WhatsApp do usuário não pode ser vazio.");
        }
        final String normalizado = whatsappNumber.trim();
        return usuarioRepository.findByWhatsappNumber(normalizado)
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setWhatsappNumber(normalizado);
                    u.setEmail(normalizado + "@migracao.local");
                    u.setNome("Usuário " + normalizado);
                    u.setCriadoEm(LocalDateTime.now());
                    return usuarioRepository.save(u);
                });
    }

    @Transactional
    public Usuario obterUsuarioAtual() {
        Usuario ctx = UsuarioContextHolder.getUsuario();
        if (ctx != null) {
            return ctx;
        }
        if (requireHeader) {
            throw new SecurityException("Autenticação obrigatória (Token Bearer ausente ou inválido).");
        }
        return buscarOuCriarPorEmail(defaultEmail, "Administrador Local");
    }
}
