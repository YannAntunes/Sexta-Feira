package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.dto.RelatorioMensalResponse;
import br.com.yann.sextafeira.service.RelatorioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatoriosController {

    private final RelatorioService relatorioService;

    public RelatoriosController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/mensal")
    public RelatorioMensalResponse relatorioMensal() {
        return relatorioService.gerarRelatorioMesAtual();
    }
}
