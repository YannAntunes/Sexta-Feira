package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.domain.model.Orcamento;
import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import br.com.yann.sextafeira.dto.OrcamentoRequest;
import br.com.yann.sextafeira.dto.StatusOrcamentoDTO;
import br.com.yann.sextafeira.repository.OrcamentoRepository;
import br.com.yann.sextafeira.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final TransacaoRepository transacaoRepository;

    public OrcamentoService(OrcamentoRepository orcamentoRepository,
                            TransacaoRepository transacaoRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public Orcamento definirOrcamento(OrcamentoRequest request) {
        Orcamento orcamento = orcamentoRepository
                .findByAnoAndMesAndCategoria(request.getAno(), request.getMes(), request.getCategoria())
                .orElse(new Orcamento());

        orcamento.setAno(request.getAno());
        orcamento.setMes(request.getMes());
        orcamento.setCategoria(request.getCategoria());
        orcamento.setValorLimite(request.getValorLimite());

        return orcamentoRepository.save(orcamento);
    }

    public StatusOrcamentoDTO consultarStatus(Integer ano, Integer mes, CategoriaTransacao categoria) {
        Orcamento orcamento = orcamentoRepository
                .findByAnoAndMesAndCategoria(ano, mes, categoria)
                .orElse(null);

        if (orcamento == null) {
            return new StatusOrcamentoDTO(
                    ano, mes, categoria,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false
            );
        }

        YearMonth ym = YearMonth.of(ano, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByDataBetween(inicio, fim);

        BigDecimal gastoCategoria = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .filter(t -> t.getCategoria() == categoria)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal restante = orcamento.getValorLimite().subtract(gastoCategoria);
        boolean estourado = restante.compareTo(BigDecimal.ZERO) < 0;

        return new StatusOrcamentoDTO(
                ano,
                mes,
                categoria,
                orcamento.getValorLimite(),
                gastoCategoria,
                restante,
                estourado
        );
    }

    public List<StatusOrcamentoDTO> resumoGeral(int ano, int mes) {

        List<StatusOrcamentoDTO> lista = new java.util.ArrayList<>();

        for (CategoriaTransacao cat : CategoriaTransacao.values()) {
            lista.add(consultarStatus(ano, mes, cat));
        }

        return lista;
    }

}
