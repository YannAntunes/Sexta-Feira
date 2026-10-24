package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.modes.finance.domain.repository.TransacaoRepository;
import br.com.yann.sextafeira.modes.finance.dto.ResumoMensalDTO;
import br.com.yann.sextafeira.modes.finance.service.ai.TransacaoIaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoIaService transacaoIaService;
    private final UsuarioService usuarioService;

    public TransacaoService(
            TransacaoRepository transacaoRepository,
            TransacaoIaService transacaoIaService,
            UsuarioService usuarioService
    ) {
        this.transacaoRepository = transacaoRepository;
        this.transacaoIaService = transacaoIaService;
        this.usuarioService = usuarioService;
    }

    public List<Transacao> listarTodas() {
        return listarTodas(usuarioService.obterUsuarioAtual());
    }

    public List<Transacao> listarTodas(Usuario usuario) {
        return transacaoRepository.findByUsuarioOrderByDataDescIdDesc(usuario);
    }

    @Transactional
    public Transacao salvar(Transacao transacao) {
        return salvar(usuarioService.obterUsuarioAtual(), transacao);
    }

    @Transactional
    public Transacao salvar(Usuario usuario, Transacao transacao) {
        if (transacao.getValor() == null || transacao.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }
        transacao.setUsuario(usuario);
        if (transacao.getData() == null) {
            transacao.setData(LocalDate.now());
        }
        return transacaoRepository.save(transacao);
    }

    public ResumoMensalDTO calcularResumoMensal(int ano, int mes) {
        return calcularResumoMensal(usuarioService.obterUsuarioAtual(), ano, mes);
    }

    public ResumoMensalDTO calcularResumoMensal(Usuario usuario, int ano, int mes) {
        YearMonth anoMes = YearMonth.of(ano, mes);
        LocalDate inicio = anoMes.atDay(1);
        LocalDate fim = anoMes.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuario, inicio, fim);

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

    @Transactional
    public Transacao removerUltimaTransacao() {
        return removerUltimaTransacao(usuarioService.obterUsuarioAtual());
    }

    @Transactional
    public Transacao removerUltimaTransacao(Usuario usuario) {
        var ultima = transacaoRepository.findTopByUsuarioOrderByIdDesc(usuario)
                .orElseThrow(() -> new RuntimeException("Não encontrei nenhuma transação para remover."));
        transacaoRepository.delete(ultima);
        return ultima;
    }

    @Transactional
    public Transacao removerPorTexto(String mensagem) {
        return removerPorTexto(usuarioService.obterUsuarioAtual(), mensagem);
    }

    @Transactional
    public Transacao removerPorTexto(Usuario usuario, String mensagem) {
        Transacao alvo = transacaoIaService.interpretarMensagem(mensagem);

        boolean temValor = alvo.getValor() != null && alvo.getValor().compareTo(BigDecimal.ZERO) > 0;
        boolean temCategoria = alvo.getCategoria() != null;
        boolean temData = alvo.getData() != null;

        if (!temValor && !temCategoria && !temData) {
            throw new RuntimeException("Não consegui identificar o que você quer apagar. Me dá pelo menos um valor ou categoria.");
        }

        LocalDate fim = LocalDate.now();
        LocalDate inicio = fim.minusDays(60);

        List<Transacao> recentes = transacaoRepository.findByUsuarioAndDataBetweenOrderByDataDescIdDesc(usuario, inicio, fim);

        BigDecimal tolerancia = new BigDecimal("0.01");

        List<Transacao> candidatos = recentes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .filter(t -> !temCategoria || t.getCategoria() == alvo.getCategoria())
                .filter(t -> !temValor || t.getValor().subtract(alvo.getValor()).abs().compareTo(tolerancia) <= 0)
                .filter(t -> !temData || t.getData().equals(alvo.getData()))
                .toList();

        if (candidatos.isEmpty()) {
            throw new RuntimeException("Não achei nenhum lançamento que bata com isso. Talvez você esteja tentando apagar algo que não existe (ainda).");
        }

        Transacao escolhido = candidatos.get(0);
        transacaoRepository.delete(escolhido);
        return escolhido;
    }

    public ResumoMensalDTO calcularResumoPorIntervalo(LocalDate inicio, LocalDate fim) {
        return calcularResumoPorIntervalo(usuarioService.obterUsuarioAtual(), inicio, fim);
    }

    public ResumoMensalDTO calcularResumoPorIntervalo(Usuario usuario, LocalDate inicio, LocalDate fim) {
        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuario, inicio, fim);

        BigDecimal despesas = BigDecimal.ZERO;
        BigDecimal receitas = BigDecimal.ZERO;

        for (Transacao t : transacoes) {
            if (t.getTipo() == TipoTransacao.DESPESA) {
                despesas = despesas.add(t.getValor());
            } else {
                receitas = receitas.add(t.getValor());
            }
        }

        BigDecimal saldo = receitas.subtract(despesas);

        return new ResumoMensalDTO(
                inicio.getYear(),
                inicio.getMonthValue(),
                despesas,
                receitas,
                saldo
        );
    }
}
