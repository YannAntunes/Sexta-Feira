package br.com.yann.sextafeira.repository;

import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.domain.model.SnapshotCarteira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SnapshotCarteiraRepository extends JpaRepository<SnapshotCarteira, Long> {

    List<SnapshotCarteira> findByData(LocalDate data);

    List<SnapshotCarteira> findByDataAndClasse(LocalDate data, ClasseAtivo classe);

    boolean existsByData(LocalDate data);

    Optional<SnapshotCarteira> findTopByDataLessThanEqualOrderByDataDesc(LocalDate alvo);

    Optional<SnapshotCarteira> findTopByClasseAndDataLessThanEqualOrderByDataDesc(ClasseAtivo classe, LocalDate alvo);

    List<SnapshotCarteira> findByDataBetween(LocalDate inicio, LocalDate fim);

    List<SnapshotCarteira> findByClasseAndDataBetween(ClasseAtivo classe, LocalDate inicio, LocalDate fim);


}

