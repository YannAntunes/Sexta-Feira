package br.com.yann.sextafeira.modes.finance.controller;

import br.com.yann.sextafeira.modes.finance.domain.model.Transacao;
import br.com.yann.sextafeira.modes.finance.dto.DeletePorTextoRequest;
import br.com.yann.sextafeira.modes.finance.dto.NaturalLanguageTransacaoRequest;
import br.com.yann.sextafeira.modes.finance.dto.NaturalLanguageTransacaoResponse;
import br.com.yann.sextafeira.modes.finance.dto.ResumoMensalDTO;
import br.com.yann.sextafeira.modes.finance.service.ai.TransacaoIaService;
import br.com.yann.sextafeira.modes.finance.service.TransacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;
    private final TransacaoIaService transacaoIaService;


    public TransacaoController(TransacaoService transacaoService, TransacaoIaService transacaoIaService) {
        this.transacaoService = transacaoService;
        this.transacaoIaService = transacaoIaService;
    }

    @GetMapping
    public List<Transacao> listarTodas() {
        return transacaoService.listarTodas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transacao criar(@RequestBody Transacao transacao) {
        return transacaoService.salvar(transacao);
    }

    @GetMapping("/resumo")
    public ResumoMensalDTO resumoMensal(
            @RequestParam int ano,
            @RequestParam int mes) {
        return transacaoService.calcularResumoMensal(ano, mes);
    }

    @PostMapping("/natural")
    @ResponseStatus(HttpStatus.CREATED)
    public NaturalLanguageTransacaoResponse criarAPartirDeLinguagemNatural(
            @RequestBody NaturalLanguageTransacaoRequest request) {

        // 1. Interpretar mensagem (simulação de IA por enquanto)
        var transacaoInterpretada = transacaoIaService.interpretarMensagem(request.getMensagem());

        // 2. Salvar como transação normal
        var transacaoSalva = transacaoService.salvar(transacaoInterpretada);

        // 3. Devolver resposta
        return new NaturalLanguageTransacaoResponse(
                request.getMensagem(),
                transacaoSalva
        );
    }

    @DeleteMapping("/ultima")
    public Transacao removerUltima() {
        return transacaoService.removerUltimaTransacao();
    }

    @PostMapping("/remover-por-texto")
    public Transacao removerPorTexto(@RequestBody DeletePorTextoRequest request) {
        return transacaoService.removerPorTexto(request.getMensagem());
    }


}
