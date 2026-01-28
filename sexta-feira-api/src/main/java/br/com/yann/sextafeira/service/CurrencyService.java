package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.dto.ConvertRequest;
import br.com.yann.sextafeira.dto.ConvertResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CurrencyService {

    private final RestTemplate restTemplate;

    public CurrencyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public ConvertResponse converter(ConvertRequest req) {
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("amount inválido");
        if (req.getFrom() == null || req.getFrom().isBlank())
            throw new RuntimeException("from inválido");
        if (req.getTo() == null || req.getTo().isBlank())
            throw new RuntimeException("to inválido");

        String from = req.getFrom().trim().toUpperCase();
        String to = req.getTo().trim().toUpperCase();

        // usa amount=1 pra pegar a taxa unitária
        String urlRate = "https://api.frankfurter.dev/v1/latest?amount=1&from=" + from + "&to=" + to;
        Map<String, Object> rateResp = restTemplate.getForObject(urlRate, Map.class);
        if (rateResp == null) throw new RuntimeException("Falha consultando câmbio");

        Map<String, Object> rates = (Map<String, Object>) rateResp.get("rates");
        if (rates == null || !rates.containsKey(to)) throw new RuntimeException("Moeda não suportada");

        BigDecimal rate = new BigDecimal(rates.get(to).toString());

        BigDecimal result = req.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        String date = String.valueOf(rateResp.get("date"));

        return new ConvertResponse(
                from, to, req.getAmount().setScale(2, RoundingMode.HALF_UP),
                result, rate, date,
                "Frankfurter (ECB)"
        );
    }
}
