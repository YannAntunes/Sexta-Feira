package br.com.yann.sextafeira.modes.finance.scheduler;

import br.com.yann.sextafeira.modes.finance.service.SnapshotCarteiraService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class SnapshotSchedulerService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final SnapshotCarteiraService snapshotCarteiraService;

    public SnapshotSchedulerService(SnapshotCarteiraService snapshotCarteiraService) {
        this.snapshotCarteiraService = snapshotCarteiraService;
    }

    // Todo dia 18:00 (horário de São Paulo).
    // [CONC-02] Único agendador de snapshot: o antigo SnapshotCarteiraScheduler (02:00) foi removido
    // porque gerava snapshots duplicados/sobrescritos para o mesmo dia.
    @Scheduled(cron = "0 0 18 * * *", zone = "America/Sao_Paulo")
    public void tirarSnapshotDiario() {
        snapshotCarteiraService.gerarSnapshotDiarioTodosUsuarios(LocalDate.now(ZONA));
    }
}
