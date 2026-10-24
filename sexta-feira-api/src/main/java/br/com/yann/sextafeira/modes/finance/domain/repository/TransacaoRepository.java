package br.com.yann.sextafeira.modes.finance.domain.repository;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByUsuarioOrderByDataDescIdDesc(Usuario usuario);

    Optional<Transacao> findTopByUsuarioOrderByIdDesc(Usuario usuario);

    List<Transacao> findByUsuarioAndDataBetween(Usuario usuario, LocalDate inicio, LocalDate fim);

    List<Transacao> findByUsuarioAndDataBetweenOrderByDataDescIdDesc(Usuario usuario, LocalDate inicio, LocalDate fim);

    @Query("""
        select t.categoria as categoria, coalesce(sum(t.valor), 0)
        from Transacao t
        where t.usuario = :usuario
          and t.tipo = :tipo
          and t.data between :inicio and :fim
        group by t.categoria
    """)
    List<Object[]> somarPorCategoriaNoPeriodo(@Param("usuario") Usuario usuario,
                                              @Param("tipo") TipoTransacao tipo,
                                              @Param("inicio") LocalDate inicio,
                                              @Param("fim") LocalDate fim);
}
