package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.domain.model.ClasseAtivo;
import br.com.yann.sextafeira.dto.BrapiQuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CotacaoService {

    private final RestTemplate restTemplate;

    @Value("${brapi.base-url:https://brapi.dev/api}")
    private String brapiBaseUrl;

    @Value("${brapi.token:}")
    private String brapiToken;

    @Value("${coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String coingeckoBaseUrl;

    public CotacaoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ✅ Método principal: decide por classe
    public BigDecimal cotacaoAtual(String ticker, ClasseAtivo classe) {
        if (classe == ClasseAtivo.CRIPTO) {
            return cotacaoCriptoBRL(ticker); // BTC/ETH -> BRL
        }
        // ACAO/FII
        return cotacaoBrapi(ticker);
    }

    // ✅ Método “seguro” para não quebrar relatório inteiro
    public BigDecimal cotacaoAtualSeguro(String ticker, ClasseAtivo classe) {
        try {
            return cotacaoAtual(ticker, classe);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    // ===== BRAPI (Ações/FIIs) =====
    private BigDecimal cotacaoBrapi(String ticker) {
        String t = ticker.toUpperCase().trim();
        String url = brapiBaseUrl + "/quote/" + t;

        if (brapiToken != null && !brapiToken.isBlank()) {
            url += "?token=" + brapiToken;
        }

        BrapiQuoteResponse resp = restTemplate.getForObject(url, BrapiQuoteResponse.class);
        if (resp == null || resp.getResults() == null || resp.getResults().isEmpty()) {
            throw new RuntimeException("Não consegui cotação (BRAPI) de " + t);
        }

        Double price = resp.getResults().get(0).getRegularMarketPrice();
        if (price == null) throw new RuntimeException("Cotação (BRAPI) vazia de " + t);

        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }

    // ===== CoinGecko (Cripto BTC/ETH em BRL) =====
    @SuppressWarnings("unchecked")
    private BigDecimal cotacaoCriptoBRL(String ticker) {
        String t = ticker.toUpperCase().trim();

        // CoinGecko usa ids, não ticker direto
        String id = switch (t) {
            case "BTC", "BITCOIN" -> "bitcoin";
            case "ETH", "ETHEREUM" -> "ethereum";
            // depois a gente amplia (SOL, XRP, etc)
            default -> throw new RuntimeException("Cripto não suportada ainda: " + t);
        };

        String url = coingeckoBaseUrl + "/simple/price?ids=" + id + "&vs_currencies=brl";
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);

        if (resp == null || !resp.containsKey(id)) {
            throw new RuntimeException("Não consegui cotação (CoinGecko) de " + t);
        }

        Object inner = resp.get(id);
        if (!(inner instanceof Map<?, ?> innerMap) || !innerMap.containsKey("brl")) {
            throw new RuntimeException("Cotação (CoinGecko) inválida de " + t);
        }

        Object brl = innerMap.get("brl");
        return new BigDecimal(brl.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
