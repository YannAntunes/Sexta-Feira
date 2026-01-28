package br.com.yann.sextafeira.repository;

import br.com.yann.sextafeira.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByWhatsappNumber(String whatsappNumber);
}
