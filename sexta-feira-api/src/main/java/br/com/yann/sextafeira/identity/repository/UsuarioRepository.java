package br.com.yann.sextafeira.identity.repository;

import br.com.yann.sextafeira.identity.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByWhatsappNumber(String whatsappNumber);
}
