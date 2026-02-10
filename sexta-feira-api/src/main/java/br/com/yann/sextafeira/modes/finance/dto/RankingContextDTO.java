package br.com.yann.sextafeira.modes.finance.dto;

import java.time.LocalDate;

public record RankingContextDTO(
        LocalDate inicio,
        LocalDate fim,
        String filtro // ACAO, FII, CRIPTO ou null
) {}
