package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.dto.ChatResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class EconomiaService {

    private final ChartsService chartsService;

    public EconomiaService(ChartsService chartsService) {
        this.chartsService = chartsService;
    }

    public String sugerirCortesSemana() {
        LocalDate hoje = LocalDate.now();

        LocalDate inicioSemanaAtual = hoje.with(DayOfWeek.MONDAY);
        LocalDate fimSemanaAtual = hoje;

        LocalDate inicioSemanaPassada = inicioSemanaAtual.minusWeeks(1);
        LocalDate fimSemanaPassada = inicioSemanaPassada.plusDays(6);

        var atual = chartsService.gastosPorCategoria(inicioSemanaAtual, fimSemanaAtual);
        var passada = chartsService.gastosPorCategoria(inicioSemanaPassada, fimSemanaPassada);

        // juntar categorias
        Set<String> cats = new HashSet<>();
        cats.addAll(atual.keySet());
        cats.addAll(passada.keySet());

        record Linha(String cat, BigDecimal atual, BigDecimal passada, BigDecimal diff, BigDecimal perc) {}

        List<Linha> linhas = new ArrayList<>();

        for (String c : cats) {
            BigDecimal a = atual.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal p = passada.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal diff = a.subtract(p);

            BigDecimal perc;
            if (p.compareTo(BigDecimal.ZERO) == 0) {
                perc = a.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(999); // “explodiu”
            } else {
                perc = diff.divide(p, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }

            linhas.add(new Linha(c, a, p, diff, perc));
        }

        // ordenar por aumento (diff desc)
        linhas.sort((x, y) -> y.diff.compareTo(x.diff));

        // pega top 3 aumentos positivos
        List<Linha> top = linhas.stream()
                .filter(l -> l.diff.compareTo(BigDecimal.ZERO) > 0)
                .limit(3)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("📉 Onde dá pra economizar (semana atual vs semana passada)\n");
        sb.append(String.format("Semana atual: %s → %s\n", inicioSemanaAtual, fimSemanaAtual));
        sb.append(String.format("Semana passada: %s → %s\n\n", inicioSemanaPassada, fimSemanaPassada));

        if (top.isEmpty()) {
            sb.append("Por incrível que pareça, seus gastos não aumentaram nas categorias principais.\n");
            sb.append("Ou você melhorou... ou só esqueceu de lançar. 😌");
            return sb.toString();
        }

        sb.append("Top aumentos:\n");
        for (Linha l : top) {
            sb.append(String.format(
                    "- %s: R$ %.2f → R$ %.2f (↑ R$ %.2f)\n",
                    formatarCategoria(l.cat),
                    l.passada, l.atual, l.diff
            ));
        }

        sb.append("\nSugestões objetivas:\n");
        for (Linha l : top) {
            sb.append("- ").append(sugestaoPorCategoria(l.cat)).append("\n");
        }

        sb.append("\nPronto. Economizar dói menos do que ficar pobre. 😏");
        return sb.toString();
    }

    private String sugestaoPorCategoria(String cat) {
        return switch (cat) {
            case "ALIMENTACAO" -> "Alimentação: define um teto por dia e corta iFood essa semana (sim, você sobrevive).";
            case "TRANSPORTE" -> "Transporte: agrupa saídas e evita corrida curta de app. Se der, anda 10 min.";
            case "LAZER" -> "Lazer: escolhe 1 rolê pago na semana e o resto é modo econômico.";
            case "CONTAS_FIXAS" -> "Contas fixas: revisa assinaturas e cancela 1 coisa que você nem usa.";
            case "INVESTIMENTOS" -> "Investimentos: se isso foi 'aporte', ok. Se foi 'trade emocional', para.";
            case "SAUDE" -> "Saúde: compara preços e compra genérico quando for seguro.";
            case "EDUCACAO" -> "Educação: mantém, mas evita compras impulsivas de curso que você não vai terminar.";
            case "MORADIA" -> "Moradia: difícil cortar rápido, mas dá pra reduzir custos de mercado/energia.";
            default -> "Geral: escolhe um gasto que você repete e corta pela metade por 7 dias.";
        };
    }

    private String formatarCategoria(String categoriaEnum) {
        String s = categoriaEnum.toLowerCase().replace("_", " ");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
