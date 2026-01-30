package br.com.yann.sextafeira.jobs;

import br.com.yann.sextafeira.service.MonitoramentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitoramentoScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoramentoScheduler.class);

    private final MonitoramentoService monitoramentoService;

    @Value("${monitoramento.enabled:true}")
    private boolean enabled;

    public MonitoramentoScheduler(MonitoramentoService monitoramentoService) {
        this.monitoramentoService = monitoramentoService;
    }

    @Scheduled(fixedDelayString = "${monitoramento.delay-ms:600000}")
    public void rodar() {
        if (!enabled) {
            log.debug("Monitoramento desativado (monitoramento.enabled=false)");
            return;
        }
        log.debug("Executando MonitoramentoScheduler...");
        monitoramentoService.checarOrcamentosEAlertar();
    }
}
