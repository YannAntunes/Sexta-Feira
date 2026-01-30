package br.com.yann.sextafeira.repository;

import br.com.yann.sextafeira.domain.model.MovimentoCarteira;
import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MovimentoCarteiraRepository extends JpaRepository<MovimentoCarteira, Long> {

    List<MovimentoCarteira> findByDataBetweenOrderByDataAscIdAsc(LocalDate inicio, LocalDate fim);

    List<MovimentoCarteira> findByClasseAndDataBetweenOrderByDataAscIdAsc(
            ClasseAtivo classe,
            LocalDate inicio,
            LocalDate fim
    );

    List<MovimentoCarteira> findByTickerAndDataBetweenOrderByDataAscIdAsc(
            String ticker,
            LocalDate inicio,
            LocalDate fim
    );

    List<MovimentoCarteira> findByClasseAndTickerAndDataBetweenOrderByDataAscIdAsc(
            ClasseAtivo classe,
            String ticker,
            LocalDate inicio,
            LocalDate fim
    );
}
