package br.com.yann.sextafeira.repository;

import br.com.yann.sextafeira.domain.model.MovimentoCarteira;
import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.yann.sextafeira.domain.model.TipoMovimentoCarteira;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        select coalesce(sum(m.valorBRL), 0)
        from MovimentoCarteira m
        where m.tipo = :tipo
          and m.data between :inicio and :fim
    """)
    BigDecimal somarValorPorTipoNoPeriodo(
            @Param("tipo") TipoMovimentoCarteira tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim
    );

    @Query("""
        select coalesce(sum(m.valorBRL), 0)
        from MovimentoCarteira m
        where m.tipo = :tipo
          and m.classe = :classe
          and m.data between :inicio and :fim
    """)
    BigDecimal somarValorPorTipoClasseNoPeriodo(
            @Param("tipo") TipoMovimentoCarteira tipo,
            @Param("classe") ClasseAtivo classe,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim
    );
}
