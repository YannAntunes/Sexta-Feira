package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.TipoTransacao;
import br.com.yann.sextafeira.domain.model.Transacao;
import br.com.yann.sextafeira.dto.ResumoMensalDTO;
import br.com.yann.sextafeira.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;

    public TransacaoService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public ResumoMensalDTO calcularResumoMensal(int ano, int mes) {
        YearMonth anoMes = YearMonth.of(ano, mes);
        LocalDate inicio = anoMes.atDay(1);
        LocalDate fim = anoMes.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByDataBetween(inicio, fim);

        BigDecimal totalDespesas = BigDecimal.ZERO;
        BigDecimal totalReceitas = BigDecimal.ZERO;

        for (Transacao t : transacoes) {
            if (t.getTipo() == TipoTransacao.DESPESA) {
                totalDespesas = totalDespesas.add(t.getValor());
            } else if (t.getTipo() == TipoTransacao.RECEITA) {
                totalReceitas = totalReceitas.add(t.getValor());
            }
        }

        BigDecimal saldo = totalReceitas.subtract(totalDespesas);

        return new ResumoMensalDTO(ano, mes, totalDespesas, totalReceitas, saldo);
    }
}
