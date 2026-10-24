package br.com.yann.sextafeira.core.domain.repository;

import br.com.yann.sextafeira.core.domain.model.MensagemHistorico;
import br.com.yann.sextafeira.identity.domain.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemHistoricoRepository extends JpaRepository<MensagemHistorico, Long> {

    List<MensagemHistorico> findByUsuarioOrderByCriadoEmDescIdDesc(Usuario usuario, Pageable pageable);

    long countByUsuario(Usuario usuario);
}
