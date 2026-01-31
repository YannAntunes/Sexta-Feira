package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.service.ChartsService;
import br.com.yann.sextafeira.service.PeriodoService;
import br.com.yann.sextafeira.service.SnapshotCarteiraService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/carteira")
public class CarteiraGraficoController {

    private final SnapshotCarteiraService snapshotCarteiraService;
    private final ChartsService chartsService;

    public CarteiraGraficoController(SnapshotCarteiraService snapshotCarteiraService,
                                     ChartsService chartsService) {
        this.snapshotCarteiraService = snapshotCarteiraService;
        this.chartsService = chartsService;
    }

    @GetMapping(value = "/grafico", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graficoCarteira(
            @RequestParam(defaultValue = "UNSPECIFIED") String range,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String classe
    ) {
        var datas = PeriodoService.resolverRange(range, days);
        LocalDate inicio = datas.get("inicio");
        LocalDate fim = datas.get("fim");

        ClasseAtivo filtro = null;
        if (classe != null && !classe.isBlank()) {
            filtro = ClasseAtivo.valueOf(classe.toUpperCase());
        }

        var serie = snapshotCarteiraService.evolucaoPatrimonio(inicio, fim, filtro);

        Map<String, Object> payload = new HashMap<>();
        payload.put("titulo", "Evolução do patrimônio (carteira)");
        payload.put("y_label", "R$");

        List<Map<String, Object>> items = new ArrayList<>();
        for (var p : serie) {
            Map<String, Object> it = new HashMap<>();
            it.put("label", p.getData().toString());
            it.put("value", p.getTotal().doubleValue());
            items.add(it);
        }
        payload.put("items", items);

        byte[] png = chartsService.gerarSerieLinhaPng(payload);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
