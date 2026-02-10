package br.com.yann.sextafeira.identity.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario buscarOuCriarPorWhatsapp(String whatsappNumber) {
        return usuarioRepository.findByWhatsappNumber(whatsappNumber)
                .orElseGet(() -> criarNovoUsuario(whatsappNumber));
    }

    private Usuario criarNovoUsuario(String whatsappNumber) {
        Usuario u = new Usuario();
        u.setWhatsappNumber(whatsappNumber);
        // personalidade padrão já é SEXTA_FEIRA
        return usuarioRepository.save(u);
    }
}
