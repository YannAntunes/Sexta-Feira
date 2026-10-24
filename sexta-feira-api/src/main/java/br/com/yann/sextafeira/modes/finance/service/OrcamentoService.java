package br.com.yann.sextafeira.modes.finance.service;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.modes.finance.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Orcamento;
import br.com.yann.sextafeira.modes.finance.domain.model.TipoTransacao;
import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.modes.finance.domain.repository.OrcamentoRepository;
import br.com.yann.sextafeira.modes.finance.domain.repository.TransacaoRepository;
import br.com.yann.sextafeira.modes.finance.dto.OrcamentoRequest;
import br.com.yann.sextafeira.modes.finance.dto.StatusOrcamentoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final TransacaoRepository transacaoRepository;
    private final UsuarioService usuarioService;

    public OrcamentoService(OrcamentoRepository orcamentoRepository,
                            TransacaoRepository transacaoRepository,
                            UsuarioService usuarioService) {
        this.orcamentoRepository = orcamentoRepository;
        this.transacaoRepository = transacaoRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public Orcamento definirOrcamento(OrcamentoRequest request) {
        return definirOrcamento(usuarioService.obterUsuarioAtual(), request);
    }

    @Transactional
    public Orcamento definirOrcamento(Usuario usuario, OrcamentoRequest request) {
        if (request.getValorLimite() == null || request.getValorLimite().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor limite do orçamento não pode ser negativo.");
        }
        Orcamento orcamento = orcamentoRepository
                .findByUsuarioAndAnoAndMesAndCategoria(usuario, request.getAno(), request.getMes(), request.getCategoria())
                .orElse(new Orcamento());

        orcamento.setUsuario(usuario);
        orcamento.setAno(request.getAno());
        orcamento.setMes(request.getMes());
        orcamento.setCategoria(request.getCategoria());
        orcamento.setValorLimite(request.getValorLimite());

        return orcamentoRepository.save(orcamento);
    }

    public StatusOrcamentoDTO consultarStatus(Integer ano, Integer mes, CategoriaTransacao categoria) {
        return consultarStatus(usuarioService.obterUsuarioAtual(), ano, mes, categoria);
    }

    public StatusOrcamentoDTO consultarStatus(Usuario usuario, Integer ano, Integer mes, CategoriaTransacao categoria) {
        Orcamento orcamento = orcamentoRepository
                .findByUsuarioAndAnoAndMesAndCategoria(usuario, ano, mes, categoria)
                .orElse(null);

        YearMonth ym = YearMonth.of(ano, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.findByUsuarioAndDataBetween(usuario, inicio, fim);

        BigDecimal gastoCategoria = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .filter(t -> t.getCategoria() == categoria)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (orcamento == null) {
            return new StatusOrcamentoDTO(
                    ano, mes, categoria,
                    BigDecimal.ZERO,
                    gastoCategoria,
                    BigDecimal.ZERO,
                    false
            );
        }

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
        return resumoGeral(usuarioService.obterUsuarioAtual(), ano, mes);
    }

    public List<StatusOrcamentoDTO> resumoGeral(Usuario usuario, int ano, int mes) {
        List<StatusOrcamentoDTO> lista = new ArrayList<>();
        for (CategoriaTransacao cat : CategoriaTransacao.values()) {
            lista.add(consultarStatus(usuario, ano, mes, cat));
        }
        return lista;
    }
}
