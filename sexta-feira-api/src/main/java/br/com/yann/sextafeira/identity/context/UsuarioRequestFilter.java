package br.com.yann.sextafeira.identity.context;

import br.com.yann.sextafeira.identity.domain.Usuario;
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

    public static final String HEADER_USER_WHATSAPP = "X-User-Whatsapp";

    private final UsuarioService usuarioService;

    public UsuarioRequestFilter(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String whatsappHeader = request.getHeader(HEADER_USER_WHATSAPP);
            if (whatsappHeader != null && !whatsappHeader.isBlank()) {
                Usuario usuario = usuarioService.buscarOuCriarPorWhatsapp(whatsappHeader.trim());
                UsuarioContextHolder.setUsuario(usuario);
            }
            filterChain.doFilter(request, response);
        } finally {
            UsuarioContextHolder.clear();
        }
    }
}
