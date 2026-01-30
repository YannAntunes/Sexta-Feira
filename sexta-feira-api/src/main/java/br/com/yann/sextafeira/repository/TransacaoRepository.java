package br.com.yann.sextafeira.repository;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    Optional<Transacao> findTopByOrderByIdDesc();

    List<Transacao> findByDataBetween(LocalDate inicio, LocalDate fim);

    List<Transacao> findByDataBetweenOrderByDataDescIdDesc(LocalDate inicio, LocalDate fim);

    @Query("""
        select t.categoria as categoria, coalesce(sum(t.valor), 0)
        from Transacao t
        where t.tipo = :tipo
          and t.data between :inicio and :fim
        group by t.categoria
    """)
    List<Object[]> somarPorCategoriaNoPeriodo(@Param("tipo") TipoTransacao tipo,
                                              @Param("inicio") LocalDate inicio,
                                              @Param("fim") LocalDate fim);
}
