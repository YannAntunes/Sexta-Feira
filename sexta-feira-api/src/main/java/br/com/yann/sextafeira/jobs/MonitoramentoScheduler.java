package br.com.yann.sextafeira.jobs;

import br.com.yann.sextafeira.service.MonitoramentoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitoramentoScheduler {

    private final MonitoramentoService monitoramentoService;

    @Value("${monitoramento.enabled:true}")
    private boolean enabled;

    public MonitoramentoScheduler(MonitoramentoService monitoramentoService) {
        this.monitoramentoService = monitoramentoService;
    }

    @Scheduled(fixedDelayString = "${monitoramento.delay-ms:600000}")
    public void rodar() {
        if (!enabled) return;
        monitoramentoService.checarOrcamentosEAlertar();
    }
}
