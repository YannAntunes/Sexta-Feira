package br.com.yann.sextafeira.repository;

import br.com.yann.sextafeira.domain.model.AlertaOrcamento;
import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaOrcamentoRepository extends JpaRepository<AlertaOrcamento, Long> {

    Optional<AlertaOrcamento> findByAnoAndMesAndCategoriaAndTipoAlerta(
            int ano, int mes, CategoriaTransacao categoria, String tipoAlerta
    );

    List<AlertaOrcamento> findByAnoAndMes(int ano, int mes);
}
