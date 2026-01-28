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
    private final TransacaoIaService transacaoIaService;


    public TransacaoService(TransacaoRepository transacaoRepository, TransacaoIaService transacaoIaService) {
        this.transacaoRepository = transacaoRepository;
        this.transacaoIaService = transacaoIaService;
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

    public Transacao removerUltimaTransacao() {
        var ultima = transacaoRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Não encontrei nenhuma transação para remover."));
        transacaoRepository.delete(ultima);
        return ultima;
    }

    public Transacao removerPorTexto(String mensagem) {
        // 1) interpretar a mensagem (vai tentar extrair valor/categoria/tipo/data)
        Transacao alvo = transacaoIaService.interpretarMensagem(mensagem);

        // segurança: não deixa remover sem nenhum “sinal forte”
        boolean temValor = alvo.getValor() != null && alvo.getValor().compareTo(BigDecimal.ZERO) > 0;
        boolean temCategoria = alvo.getCategoria() != null;
        boolean temData = alvo.getData() != null;

        if (!temValor && !temCategoria && !temData) {
            throw new RuntimeException("Não consegui identificar o que você quer apagar. Me dá pelo menos um valor ou categoria.");
        }

        // 2) buscar transações recentes (ex: últimos 60 dias)
        LocalDate fim = LocalDate.now();
        LocalDate inicio = fim.minusDays(60);

        List<Transacao> recentes = transacaoRepository.findByDataBetweenOrderByDataDescIdDesc(inicio, fim);

        // 3) filtrar candidatos (matching simples + tolerância no valor)
        BigDecimal tolerancia = new BigDecimal("0.01");

        List<Transacao> candidatos = recentes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA) // remover "gasto" normalmente é despesa
                .filter(t -> !temCategoria || t.getCategoria() == alvo.getCategoria())
                .filter(t -> !temValor || t.getValor().subtract(alvo.getValor()).abs().compareTo(tolerancia) <= 0)
                .filter(t -> !temData || t.getData().equals(alvo.getData()))
                .toList();

        if (candidatos.isEmpty()) {
            throw new RuntimeException("Não achei nenhum lançamento que bata com isso. Talvez você esteja tentando apagar algo que não existe (ainda).");
        }

        // 4) escolher o “mais provável”: o mais recente (já vem ordenado desc)
        Transacao escolhido = candidatos.get(0);

        // 5) apagar
        transacaoRepository.delete(escolhido);

        // (opcional) você pode retornar e também devolver quantos candidatos existiam
        return escolhido;
    }
}


