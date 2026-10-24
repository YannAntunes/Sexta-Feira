package br.com.yann.sextafeira.modes.finance.domain.repository;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.modes.finance.domain.model.NotificacaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoUsuarioRepository extends JpaRepository<NotificacaoUsuario, Long> {

    List<NotificacaoUsuario> findByUsuarioOrderByCriadoEmDescIdDesc(Usuario usuario);

    List<NotificacaoUsuario> findByUsuarioAndLidaFalseOrderByCriadoEmDescIdDesc(Usuario usuario);

    long countByUsuarioAndLidaFalse(Usuario usuario);
}
