package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.service.MonitoramentoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monitoramento")
public class MonitoramentoController {

    private final MonitoramentoService monitoramentoService;

    public MonitoramentoController(MonitoramentoService monitoramentoService) {
        this.monitoramentoService = monitoramentoService;
    }

    @PostMapping("/checar-agora")
    public String checarAgora() {
        monitoramentoService.checarOrcamentosEAlertar();
        return "Ok. Se tinha algo errado, eu te avisei. 😌";
    }
}
