package br.com.yann.sextafeira.identity.context;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.security.JwtService;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UsuarioRequestFilter extends OncePerRequestFilter {

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * @deprecated Mantido temporariamente para testes legados e migracao gradual.
     */
    @Deprecated
    public static final String HEADER_USER_WHATSAPP = "X-User-Whatsapp";

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public UsuarioRequestFilter(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HEADER_AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                Long userId = jwtService.extrairUserId(token);
                if (userId != null) {
                    usuarioService.buscarPorId(userId).ifPresent(UsuarioContextHolder::setUsuario);
                }
            }

            // Fallback para cabecalho legado (enquanto clientes antigos e testes de transicao migram)
            if (UsuarioContextHolder.getUsuario() == null) {
                String whatsappHeader = request.getHeader(HEADER_USER_WHATSAPP);
                if (whatsappHeader != null && !whatsappHeader.isBlank()) {
                    Usuario usuario = usuarioService.buscarOuCriarPorWhatsapp(whatsappHeader.trim());
                    UsuarioContextHolder.setUsuario(usuario);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            UsuarioContextHolder.clear();
        }
    }
}
