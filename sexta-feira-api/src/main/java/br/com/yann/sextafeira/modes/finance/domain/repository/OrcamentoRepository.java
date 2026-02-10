package br.com.yann.sextafeira.modes.finance.domain.repository;

import br.com.yann.sextafeira.modes.finance.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    Optional<Orcamento> findByAnoAndMesAndCategoria(Integer ano, Integer mes, CategoriaTransacao categoria);

    List<Orcamento> findByAnoAndMes(Integer ano, Integer mes);
}
