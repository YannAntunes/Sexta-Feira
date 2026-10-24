package br.com.yann.sextafeira.identity.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.dto.*;
import br.com.yann.sextafeira.identity.repository.UsuarioRepository;
import br.com.yann.sextafeira.identity.security.JwtService;
import br.com.yann.sextafeira.identity.security.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${sexta.security.jwt.access-token-expiration-seconds:3600}")
    private long accessTokenExpiresIn;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse cadastrar(RegisterRequest request) {
        final String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("O e-mail informado já está cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(request.getNome().trim());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        if (request.getIdiomaPreferido() != null && !request.getIdiomaPreferido().isBlank()) {
            usuario.setIdiomaPreferido(request.getIdiomaPreferido());
        }
        if (request.getPersonalidade() != null) {
            usuario.setPersonalidade(request.getPersonalidade());
        }
        usuario.setCriadoEm(LocalDateTime.now());

        Usuario salvo = usuarioRepository.save(usuario);

        String accessToken = jwtService.gerarAccessToken(salvo.getId(), salvo.getEmail());
        String refreshToken = jwtService.gerarRefreshToken(salvo.getId(), salvo.getEmail());

        return new AuthResponse(accessToken, refreshToken, accessTokenExpiresIn, new UsuarioPerfilDTO(salvo));
    }

    @Transactional(readOnly = true)
    public AuthResponse autenticar(LoginRequest request) {
        final String email = request.getEmail().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("Credenciais inválidas."));

        if (usuario.getSenhaHash() == null || !passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            throw new SecurityException("Credenciais inválidas.");
        }

        String accessToken = jwtService.gerarAccessToken(usuario.getId(), usuario.getEmail());
        String refreshToken = jwtService.gerarRefreshToken(usuario.getId(), usuario.getEmail());

        return new AuthResponse(accessToken, refreshToken, accessTokenExpiresIn, new UsuarioPerfilDTO(usuario));
    }

    @Transactional(readOnly = true)
    public AuthResponse renovarToken(String refreshToken) {
        var claims = jwtService.validarToken(refreshToken);
        if (claims == null || !"refresh".equals(claims.get("type"))) {
            throw new SecurityException("Token de atualização (refresh) inválido ou expirado.");
        }

        Long userId = Long.valueOf(claims.get("sub").toString());
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("Usuário não encontrado para este token."));

        String novoAccessToken = jwtService.gerarAccessToken(usuario.getId(), usuario.getEmail());
        String novoRefreshToken = jwtService.gerarRefreshToken(usuario.getId(), usuario.getEmail());

        return new AuthResponse(novoAccessToken, novoRefreshToken, accessTokenExpiresIn, new UsuarioPerfilDTO(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilDTO obterPerfil(Usuario usuario) {
        if (usuario == null) throw new SecurityException("Usuário não autenticado.");
        return new UsuarioPerfilDTO(usuario);
    }

    @Transactional
    public UsuarioPerfilDTO atualizarPerfil(Usuario usuario, AtualizarPerfilRequest request) {
        if (usuario == null) throw new SecurityException("Usuário não autenticado.");

        Usuario entidade = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (request.getNome() != null && !request.getNome().isBlank()) {
            entidade.setNome(request.getNome().trim());
        }
        if (request.getPersonalidade() != null) {
            entidade.setPersonalidade(request.getPersonalidade());
        }
        if (request.getIdiomaPreferido() != null && !request.getIdiomaPreferido().isBlank()) {
            entidade.setIdiomaPreferido(request.getIdiomaPreferido().trim());
        }
        entidade.setAtualizadoEm(LocalDateTime.now());

        Usuario salvo = usuarioRepository.save(entidade);
        return new UsuarioPerfilDTO(salvo);
    }
}
