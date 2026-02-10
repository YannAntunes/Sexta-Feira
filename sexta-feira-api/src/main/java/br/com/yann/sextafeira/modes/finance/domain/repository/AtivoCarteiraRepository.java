package br.com.yann.sextafeira.modes.finance.domain.repository;

import br.com.yann.sextafeira.modes.finance.domain.model.AtivoCarteira;
import br.com.yann.sextafeira.modes.finance.domain.model.ClasseAtivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtivoCarteiraRepository extends JpaRepository<AtivoCarteira, Long> {
    Optional<AtivoCarteira> findByClasseAndTicker(ClasseAtivo classe, String ticker);
    List<AtivoCarteira> findByClasseOrderByTickerAsc(ClasseAtivo classe);
}
