package br.com.yann.sextafeira.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FrasesService {

    private final Map<String, List<String>> frases = Map.of(
            "ADD_RECEITA", List.of(
                    "Entrada registrada: +R$ %.2f em %s. Bom ver dinheiro chegando. 😌",
                    "Anotei o crédito: +R$ %.2f em %s. Isso sim é música. 🎶",
                    "+R$ %.2f em %s confirmado. Agora não gasta por impulso, hein. 😉"
            ),
            "ADD_DESPESA", List.of(
                    "Gasto registrado: -R$ %.2f em %s. Anotado. 😏",
                    "Ok. Saiu -R$ %.2f em %s. Seu orçamento sentiu daqui. 🥲",
                    "Lancei -R$ %.2f em %s. Vamos fingir que foi necessário. 😌"
            ),
            "DELETE_OK", List.of(
                    "Feito. Apaguei isso do seu histórico. 😉",
                    "Ok, removido. Menos bagunça. 😌",
                    "Deletado. Agora tenta não viver no modo 'Ctrl+Z'. 😏"
            ),
            "BUDGET_80", List.of(
                    "⚠️ Você já passou de 80%% em %s. Vai com calma. 😌",
                    "👀 %s já está acima de 80%%. Eu não chamaria de 'controle'… ainda.",
                    "⚠️ Atenção: %s bateu 80%%. Pisa no freio. 😏"
            ),
            "BUDGET_90", List.of(
                    "🔥 %s já passou de 90%%. Última chamada antes do estrago. 😏",
                    "🚨 %s em 90%%+. Seu eu do futuro está julgando.",
                    "🔥 %s tá no limite do limite. Segura a mão. 😌"
            ),
            "BUDGET_ESTOURO", List.of(
                    "🚨 Estourou %s. Parabéns… eu acho. 😏",
                    "🚨 %s passou do limite. Agora é modo contenção. 😌",
                    "🚨 Orçamento de %s foi pro espaço. Vamos respirar. 🥲"
            )
    );

    public String escolher(String chave) {
        List<String> lista = frases.get(chave);
        if (lista == null || lista.isEmpty()) return "";
        int i = ThreadLocalRandom.current().nextInt(lista.size());
        return lista.get(i);
    }
}
