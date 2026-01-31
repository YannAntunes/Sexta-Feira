package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SnapshotSchedulerService {

    private final SnapshotCarteiraService snapshotCarteiraService;

    public SnapshotSchedulerService(SnapshotCarteiraService snapshotCarteiraService) {
        this.snapshotCarteiraService = snapshotCarteiraService;
    }

    // Todo dia 18:00 (horário de São Paulo)
    @Scheduled(cron = "0 0 18 * * *", zone = "America/Sao_Paulo")
    public void tirarSnapshotDiario() {
        LocalDate hoje = LocalDate.now();
        snapshotCarteiraService.gerarSnapshotDoDia(hoje, null);
    }
}
