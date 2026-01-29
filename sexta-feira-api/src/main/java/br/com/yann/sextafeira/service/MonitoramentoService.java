package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.AlertaOrcamento;
import br.com.yann.sextafeira.domain.model.CategoriaTransacao;
import br.com.yann.sextafeira.repository.AlertaOrcamentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class MonitoramentoService {

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
        System.out.println("MONITORAMENTO RODOU");

        YearMonth ym = YearMonth.from(LocalDate.now());
        int ano = ym.getYear();
        int mes = ym.getMonthValue();

        for (CategoriaTransacao cat : CategoriaTransacao.values()) {

            var status = orcamentoService.consultarStatus(ano, mes, cat);

            BigDecimal limite = status.getLimite();
            BigDecimal gasto = status.getGasto();

            if (limite == null || limite.compareTo(BigDecimal.ZERO) <= 0) continue;

            // estourou
            if (status.isEstourado()) {
                enviarUmaVezPorMes(ano, mes, cat, "ESTOURO",
                        String.format("🚨 %s estourou o orçamento. Parabéns.\nLimite: R$ %.2f\nGasto: R$ %.2f\nExcedeu: R$ %.2f 😌",
                                cat.name(), limite, gasto, gasto.subtract(limite)));
                continue;
            }

            // 80%+
            BigDecimal perc = gasto.divide(limite, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            if (perc.compareTo(BigDecimal.valueOf(80)) >= 0) {

                enviarUmaVezPorMes(ano, mes, cat, "PERCENT_80",
                        String.format("⚠️ %s já está em %.0f%% do orçamento.\nLimite: R$ %.2f\nGasto: R$ %.2f\nSobra: R$ %.2f\nVai com calma. 😏",
                                cat.name(), perc, limite, gasto, limite.subtract(gasto)));
            }
        }
    }

    private void enviarUmaVezPorMes(int ano, int mes, CategoriaTransacao cat, String tipo, String texto) {

        var existente = alertaRepo.findByAnoAndMesAndCategoriaAndTipoAlerta(ano, mes, cat, tipo);

        if (existente.isPresent()) {
            // já avisei este mês pra esse tipo/categoria -> não spamma
            return;
        }

        alertaRepo.save(new AlertaOrcamento(ano, mes, cat, tipo, LocalDateTime.now()));

        // envia (por enquanto pode ser só log via WhatsappService)
        if (whatsappTo != null && !whatsappTo.isBlank()) {
            whatsappService.enviarMensagem(new br.com.yann.sextafeira.dto.WhatsappOutgoingMessageDTO(whatsappTo, texto));
        } else {
            System.out.println("[ALERTA] " + texto);
        }
    }
}
