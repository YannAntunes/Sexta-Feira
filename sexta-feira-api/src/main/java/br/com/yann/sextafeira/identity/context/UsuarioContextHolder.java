package br.com.yann.sextafeira.identity.context;

import br.com.yann.sextafeira.identity.domain.Usuario;

/**
 * Mantém o usuário autenticado/identificado na thread da requisição atual.
 */
public final class UsuarioContextHolder {

    private static final ThreadLocal<Usuario> CONTEXT = new ThreadLocal<>();

    private UsuarioContextHolder() {
    }

    public static void setUsuario(Usuario usuario) {
        CONTEXT.set(usuario);
    }

    public static Usuario getUsuario() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
