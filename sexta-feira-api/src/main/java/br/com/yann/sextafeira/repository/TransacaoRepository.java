package br.com.yann.sextafeira.repository;

import br.com.yann.sextafeira.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByDataBetween(LocalDate dataInicio, LocalDate dataFim);

}
