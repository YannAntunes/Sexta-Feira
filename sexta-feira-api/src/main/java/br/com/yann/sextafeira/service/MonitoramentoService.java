package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.AlertaOrcamento;
import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.repository.AlertaOrcamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class MonitoramentoService {

    private static final Logger log = LoggerFactory.getLogger(MonitoramentoService.class);

    private final OrcamentoService orcamentoService;
    private final AlertaOrcamentoRepository alertaRepo;
    private final WhatsappService whatsappService;

    @Value("${monitoramento.whatsapp.to:}")
    private String whatsappTo;

    public MonitoramentoService(OrcamentoService orcamentoService,
                                AlertaOrcamentoRepository alertaRepo,
                                WhatsappService whatsappService) {
        this.orcamentoService = orcamentoService;
        this.alertaRepo = alertaRepo;
        this.whatsappService = whatsappService;
    }

    public void checarOrcamentosEAlertar() {

        YearMonth ym = YearMonth.from(LocalDate.now());
        int ano = ym.getYear();
        int mes = ym.getMonthValue();

        log.info("MONITORAMENTO RODOU - checando orçamentos {}/{}", mes, ano);

        for (CategoriaTransacao cat : CategoriaTransacao.values()) {

            var status = orcamentoService.consultarStatus(ano, mes, cat);

            BigDecimal limite = status.getLimite();
            BigDecimal gasto = status.getGasto();

            if (limite == null || limite.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 1) Estouro
            if (gasto.compareTo(limite) > 0) {
                BigDecimal excedeu = gasto.subtract(limite);

                enviarUmaVezPorMes(ano, mes, cat, "ESTOURO",
                        String.format(
                                "🚨 %s estourou o orçamento.\nLimite: R$ %.2f\nGasto: R$ %.2f\nExcedeu: R$ %.2f",
                                cat.name(), limite, gasto, excedeu
                        )
                );
                continue;
            }

            // 2) Percentual (90% / 80%)
            BigDecimal perc = gasto.divide(limite, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (perc.compareTo(BigDecimal.valueOf(90)) >= 0) {
                enviarUmaVezPorMes(ano, mes, cat, "PERCENT_90",
                        String.format(
                                "🔥 %s passou de 90%% do orçamento.\nLimite: R$ %.2f\nGasto: R$ %.2f\nSobra: R$ %.2f",
                                cat.name(), limite, gasto, limite.subtract(gasto)
                        )
                );
            } else if (perc.compareTo(BigDecimal.valueOf(80)) >= 0) {
                enviarUmaVezPorMes(ano, mes, cat, "PERCENT_80",
                        String.format(
                                "⚠️ %s passou de 80%% do orçamento.\nLimite: R$ %.2f\nGasto: R$ %.2f\nSobra: R$ %.2f",
                                cat.name(), limite, gasto, limite.subtract(gasto)
                        )
                );
            }
        }
    }

    private void enviarUmaVezPorMes(int ano, int mes, CategoriaTransacao cat, String tipo, String texto) {

        var existente = alertaRepo.findByAnoAndMesAndCategoriaAndTipoAlerta(ano, mes, cat, tipo);
        if (existente.isPresent()) return;

        alertaRepo.save(new AlertaOrcamento(ano, mes, cat, tipo, LocalDateTime.now()));

        if (whatsappTo != null && !whatsappTo.isBlank()) {
            whatsappService.enviarMensagem(new br.com.yann.sextafeira.dto.WhatsappOutgoingMessageDTO(whatsappTo, texto));
        } else {
            log.info("[ALERTA] {}", texto);
        }
    }
}
