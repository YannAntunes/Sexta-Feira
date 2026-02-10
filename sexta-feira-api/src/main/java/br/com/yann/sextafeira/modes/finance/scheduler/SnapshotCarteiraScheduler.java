package br.com.yann.sextafeira.modes.finance.scheduler;

import br.com.yann.sextafeira.modes.finance.service.SnapshotCarteiraService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SnapshotCarteiraScheduler {

    private final SnapshotCarteiraService snapshotService;

    public SnapshotCarteiraScheduler(SnapshotCarteiraService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * Gera snapshot diário da carteira inteira.
     * Roda todo dia às 02:00 da manhã.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void gerarSnapshotDiario() {
        snapshotService.gerarSnapshotDoDia(LocalDate.now(), null);
    }
}
