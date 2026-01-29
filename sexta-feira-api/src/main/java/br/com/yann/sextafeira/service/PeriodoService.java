package br.com.yann.sextafeira.service;

import java.time.*;
import java.util.Map;

public class PeriodoService {

    public static Map<String, LocalDate> resolverRange(String range, Integer days) {
        LocalDate hoje = LocalDate.now();

        return switch (range) {
            case "TODAY" -> Map.of("inicio", hoje, "fim", hoje);
            case "YESTERDAY" -> {
                LocalDate d = hoje.minusDays(1);
                yield Map.of("inicio", d, "fim", d);
            }
            case "THIS_WEEK" -> {
                LocalDate inicio = hoje.with(DayOfWeek.MONDAY);
                LocalDate fim = hoje;
                yield Map.of("inicio", inicio, "fim", fim);
            }
            case "LAST_WEEK" -> {
                LocalDate inicio = hoje.with(DayOfWeek.MONDAY).minusWeeks(1);
                LocalDate fim = inicio.plusDays(6);
                yield Map.of("inicio", inicio, "fim", fim);
            }
            case "THIS_MONTH" -> {
                YearMonth ym = YearMonth.from(hoje);
                yield Map.of("inicio", ym.atDay(1), "fim", hoje);
            }
            case "LAST_MONTH" -> {
                YearMonth ym = YearMonth.from(hoje).minusMonths(1);
                yield Map.of("inicio", ym.atDay(1), "fim", ym.atEndOfMonth());
            }
            case "LAST_N_DAYS" -> {
                int n = (days == null || days < 1) ? 7 : days;
                yield Map.of("inicio", hoje.minusDays(n - 1), "fim", hoje);
            }
            default -> Map.of("inicio", hoje.minusDays(6), "fim", hoje); // fallback: 7 dias
        };
    }
}
