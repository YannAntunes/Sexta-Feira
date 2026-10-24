package br.com.yann.sextafeira.identity.controller;

import br.com.yann.sextafeira.identity.dto.AtualizarPerfilRequest;
import br.com.yann.sextafeira.identity.dto.UsuarioPerfilDTO;
import br.com.yann.sextafeira.identity.service.AuthService;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuario")
public class UsuarioPerfilController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public UsuarioPerfilController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil")
    public UsuarioPerfilDTO obterPerfil() {
        return authService.obterPerfil(usuarioService.obterUsuarioAtual());
    }

    @PutMapping("/perfil")
    public UsuarioPerfilDTO atualizarPerfil(@Valid @RequestBody AtualizarPerfilRequest request) {
        return authService.atualizarPerfil(usuarioService.obterUsuarioAtual(), request);
    }
}
