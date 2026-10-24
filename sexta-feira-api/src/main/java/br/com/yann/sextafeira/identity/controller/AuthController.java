package br.com.yann.sextafeira.identity.controller;

import br.com.yann.sextafeira.identity.dto.*;
import br.com.yann.sextafeira.identity.service.AuthService;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authService.cadastrar(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.autenticar(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken é obrigatório.");
        }
        return authService.renovarToken(refreshToken);
    }

    @GetMapping("/me")
    public UsuarioPerfilDTO me() {
        return authService.obterPerfil(usuarioService.obterUsuarioAtual());
    }
}
