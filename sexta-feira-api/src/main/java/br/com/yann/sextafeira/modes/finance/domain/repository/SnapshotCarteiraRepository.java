package br.com.yann.sextafeira.modes.finance.domain.repository;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.modes.finance.domain.model.SnapshotCarteira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SnapshotCarteiraRepository extends JpaRepository<SnapshotCarteira, Long> {

    List<SnapshotCarteira> findByUsuarioAndData(Usuario usuario, LocalDate data);

    List<SnapshotCarteira> findByUsuarioAndDataAndClasse(Usuario usuario, LocalDate data, ClasseAtivo classe);

    boolean existsByUsuarioAndData(Usuario usuario, LocalDate data);

    Optional<SnapshotCarteira> findTopByUsuarioAndDataLessThanEqualOrderByDataDesc(Usuario usuario, LocalDate alvo);

    Optional<SnapshotCarteira> findTopByUsuarioAndClasseAndDataLessThanEqualOrderByDataDesc(
            Usuario usuario, ClasseAtivo classe, LocalDate alvo
    );

    List<SnapshotCarteira> findByUsuarioAndDataBetween(Usuario usuario, LocalDate inicio, LocalDate fim);

    List<SnapshotCarteira> findByUsuarioAndClasseAndDataBetween(
            Usuario usuario, ClasseAtivo classe, LocalDate inicio, LocalDate fim
    );
}
