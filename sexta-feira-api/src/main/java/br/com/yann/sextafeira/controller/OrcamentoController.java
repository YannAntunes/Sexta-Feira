package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.domain.model.Orcamento;
import br.com.yann.sextafeira.dto.OrcamentoRequest;
import br.com.yann.sextafeira.dto.StatusOrcamentoDTO;
import br.com.yann.sextafeira.service.OrcamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orcamentos")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Orcamento definir(@RequestBody OrcamentoRequest request) {
        return orcamentoService.definirOrcamento(request);
    }

    @GetMapping("/status")
    public StatusOrcamentoDTO status(@RequestParam Integer ano,
                                     @RequestParam Integer mes,
                                     @RequestParam CategoriaTransacao categoria) {
        return orcamentoService.consultarStatus(ano, mes, categoria);
    }
}
