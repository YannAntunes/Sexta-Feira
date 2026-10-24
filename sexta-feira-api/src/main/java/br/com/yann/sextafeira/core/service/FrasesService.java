package br.com.yann.sextafeira.core.service;

import br.com.yann.sextafeira.core.domain.model.Personalidade;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Map.entry;

/**
 * Catálogo de frases e tom de voz por personalidade do assistente.
 * Suporta os 4 modos configuráveis no cadastro do usuário:
 * - SEXTA_FEIRA: sarcástica, irônica e pragmática
 * - FORMAL: executiva, objetiva e profissional
 * - AMIGAVEL: empática, calorosa e encorajadora
 * - JARVIS: britânica, cerimoniosa e altamente polida
 */
@Service
public class FrasesService {

    private final Map<Personalidade, Map<String, List<String>>> catalogo = new EnumMap<>(Personalidade.class);

    public FrasesService() {
        // -------------------------------------------------------------
        // MODO: SEXTA_FEIRA (Padrão: sarcástica e pragmática)
        // -------------------------------------------------------------
        catalogo.put(Personalidade.SEXTA_FEIRA, Map.ofEntries(
                entry("ADD_RECEITA", List.of(
                        "Entrada registrada: +R$ %.2f em %s. Bom ver dinheiro chegando. 😌",
                        "Anotei o crédito: +R$ %.2f em %s. Isso sim é música. 🎶",
                        "+R$ %.2f em %s confirmado. Agora não gasta por impulso, hein. 😉"
                )),
                entry("ADD_DESPESA", List.of(
                        "Gasto registrado: -R$ %.2f em %s. Anotado. 😏",
                        "Ok. Saiu -R$ %.2f em %s. Seu orçamento sentiu daqui. 🥲",
                        "Lancei -R$ %.2f em %s. Vamos fingir que foi necessário. 😌"
                )),
                entry("DELETE_OK", List.of(
                        "Feito. Apaguei isso do seu histórico. 😉",
                        "Ok, removido. Menos bagunça. 😌",
                        "Deletado. Agora tenta não viver no modo 'Ctrl+Z'. 😏"
                )),
                entry("BUDGET_80", List.of(
                        "⚠️ Você já passou de 80%% em %s. Vai com calma. 😌",
                        "👀 %s já está acima de 80%%. Eu não chamaria de 'controle'… ainda.",
                        "⚠️ Atenção: %s bateu 80%%. Pisa no freio. 😏"
                )),
                entry("BUDGET_90", List.of(
                        "🔥 %s já passou de 90%%. Última chamada antes do estrago. 😏",
                        "🚨 %s em 90%%+. Seu eu do futuro está julgando.",
                        "🔥 %s tá no limite do limite. Segura a mão. 😌"
                )),
                entry("BUDGET_ESTOURO", List.of(
                        "🚨 Estourou %s. Parabéns… eu acho. 😏",
                        "🚨 %s passou do limite. Agora é modo contenção. 😌",
                        "🚨 Orçamento de %s foi pro espaço. Vamos respirar. 🥲"
                )),
                entry("SET_PERSONALIDADE_OK", List.of(
                        "Modo SEXTA-FEIRA ativado. Meu sarcasmo afiado está de volta ao seu dispor. 😏",
                        "Modo SEXTA-FEIRA ativado. Menos burocracia, mais verdade nua e crua. 😌"
                )),
                entry("MSG_VAZIA", List.of(
                        "Mensagem vazia? Até meu sarcasmo precisa de pelo menos uma palavra pra trabalhar. 😏"
                )),
                entry("ERRO_UNKNOWN", List.of(
                        "Não entendi. Reformula isso como se eu fosse uma IA milionária, por favor. 😏"
                )),
                entry("SALDO_POSITIVO", List.of(
                        "você está no azul. Tenta não estragar isso. 😌"
                )),
                entry("SALDO_NEGATIVO", List.of(
                        "você está no vermelho. Não é uma vibe bonita. 🥲"
                ))
        ));

        // -------------------------------------------------------------
        // MODO: FORMAL (Executiva, clara, profissional e sem gírias)
        // -------------------------------------------------------------
        catalogo.put(Personalidade.FORMAL, Map.ofEntries(
                entry("ADD_RECEITA", List.of(
                        "Receita confirmada: R$ %.2f registrada na categoria %s.",
                        "Lançamento de crédito efetuado com sucesso: +R$ %.2f em %s."
                )),
                entry("ADD_DESPESA", List.of(
                        "Despesa processada: R$ %.2f debitada na categoria %s.",
                        "Registro efetuado: -R$ %.2f alocado na categoria %s."
                )),
                entry("DELETE_OK", List.of(
                        "Operação concluída. O registro financeiro foi removido do sistema.",
                        "Registro excluído com sucesso do banco de dados."
                )),
                entry("BUDGET_80", List.of(
                        "Aviso: O orçamento para a categoria %s atingiu 80%% do limite estabelecido.",
                        "Alerta de monitoramento: consumo de 80%% registrado em %s."
                )),
                entry("BUDGET_90", List.of(
                        "Alerta crítico: A categoria %s ultrapassou 90%% do orçamento mensal previsto.",
                        "Notificação: O limite da categoria %s está próximo da exaustão (90%%+)."
                )),
                entry("BUDGET_ESTOURO", List.of(
                        "Alerta de estouro: O limite orçamentário para %s foi excedido.",
                        "Violação de limite: Gastos em %s ultrapassaram o teto estipulado."
                )),
                entry("SET_PERSONALIDADE_OK", List.of(
                        "Modo FORMAL ativado com sucesso. Comunicação ajustada para padrão corporativo e direto.",
                        "Modo FORMAL configurado. As respostas seguirão padrão técnico e executivo."
                )),
                entry("MSG_VAZIA", List.of(
                        "Mensagem vazia recebida. Por favor, forneça o comando ou consulta desejada."
                )),
                entry("ERRO_UNKNOWN", List.of(
                        "Não foi possível interpretar a solicitação. Por favor, reformule com termos financeiros claros."
                )),
                entry("SALDO_POSITIVO", List.of(
                        "o saldo do período permanece positivo e equilibrado."
                )),
                entry("SALDO_NEGATIVO", List.of(
                        "o saldo do período apresenta déficit orçamentário."
                ))
        ));

        // -------------------------------------------------------------
        // MODO: AMIGAVEL (Empática, calorosa, animada e encorajadora)
        // -------------------------------------------------------------
        catalogo.put(Personalidade.AMIGAVEL, Map.ofEntries(
                entry("ADD_RECEITA", List.of(
                        "Que notícia maravilhosa! Registrei +R$ %.2f em %s. Parabéns pelo ganho! 🎉",
                        "Oba, dim-dim na conta! Anotei +R$ %.2f em %s com muito carinho. 😊"
                )),
                entry("ADD_DESPESA", List.of(
                        "Tudo certo! Anotei seu gasto de R$ %.2f em %s. Vamos continuar acompanhando juntos! 💪",
                        "Prontinho! Lancei -R$ %.2f em %s. Cuidar das finanças é um passo de cada vez! ✨"
                )),
                entry("DELETE_OK", List.of(
                        "Tudo bem, já apaguei esse registro para você! Histórico limpinho. 🧹😊",
                        "Pronto! Exclusão feita com sucesso. Pode contar comigo sempre! 🤝"
                )),
                entry("BUDGET_80", List.of(
                        "Opa, um lembrete amigo: você já usou 80%% do orçamento de %s. Vamos com cautela? 🤗",
                        "Atenção com carinho: os gastos em %s estão em 80%% do limite. Você consegue segurar até o fim do mês! 💛"
                )),
                entry("BUDGET_90", List.of(
                        "Atenção redobrada, amigo(a)! %s passou de 90%%. Falta pouco para o fim do mês, força aí! ⚠️😊",
                        "Quase lá no topo: %s bateu 90%%. Vamos dar uma segurada nos gastos essa semana? 💛"
                )),
                entry("BUDGET_ESTOURO", List.of(
                        "Poxa, o orçamento de %s estourou. Não se desespere, imprevistos acontecem! Vamos ajustar o foco! 🤝",
                        "Passou do limite em %s, mas não desanime! O importante é manter o controle a partir de agora. 💛"
                )),
                entry("SET_PERSONALIDADE_OK", List.of(
                        "Modo AMIGÁVEL ativado! Estou super feliz em poder te ajudar na sua jornada financeira! Conte sempre comigo! 🥰✨",
                        "Modo AMIGÁVEL ativado! A partir de agora vamos conversar com muito otimismo e carinho! 💛"
                )),
                entry("MSG_VAZIA", List.of(
                        "Oi! Você não digitou nada. Como posso te ajudar a organizar seu dinheiro hoje? 😊"
                )),
                entry("ERRO_UNKNOWN", List.of(
                        "Puxa, não consegui entender bem o que você quis dizer. Pode me explicar de outro jeitinho? Estou aqui para ajudar! 🤗"
                )),
                entry("SALDO_POSITIVO", List.of(
                        "você está no azul! Que orgulho da sua disciplina financeira! 🌟"
                )),
                entry("SALDO_NEGATIVO", List.of(
                        "você está um pouquinho no vermelho, mas com calma a gente coloca tudo em ordem! 💪"
                ))
        ));

        // -------------------------------------------------------------
        // MODO: JARVIS (Britânica, cerimoniosa, polida e tecnológica)
        // -------------------------------------------------------------
        catalogo.put(Personalidade.JARVIS, Map.ofEntries(
                entry("ADD_RECEITA", List.of(
                        "Crédito computado nos sistemas, senhor(a). +R$ %.2f alocado à categoria %s. Excelente gestão de capital.",
                        "Receita processada e indexada aos registros: R$ %.2f em %s. Ao seu dispor para novas entradas."
                )),
                entry("ADD_DESPESA", List.of(
                        "Débito devidamente registrado, senhor(a): R$ %.2f na categoria %s. Parâmetros orçamentários atualizados.",
                        "Lançamento de saída catalogado: -R$ %.2f em %s. Monitoramento ativo."
                )),
                entry("DELETE_OK", List.of(
                        "Instrução executada. O registro solicitado foi expurgado com sucesso da memória operacional.",
                        "Operação confirmada. O lançamento em questão foi removido, senhor(a)."
                )),
                entry("BUDGET_80", List.of(
                        "Aviso preventivo, senhor(a): a alocação para %s atingiu 80%% da cota mensal estipulada.",
                        "Notificação de consumo: a categoria %s ultrapassou o patamar de 80%% de tolerância."
                )),
                entry("BUDGET_90", List.of(
                        "Alerta prioritário: consumo de %s em 90%%. Sugiro restrição imediata de novos fluxos de saída.",
                        "Aproximação de limiar crítico: %s excedeu 90%% da capacidade autorizada, senhor(a)."
                )),
                entry("BUDGET_ESTOURO", List.of(
                        "Protocolo de estouro: a cota financeira designada para %s foi integralmente superada.",
                        "Atenção, senhor(a): verifiquei que os gastos em %s sobrepujaram o limite previamente programado."
                )),
                entry("SET_PERSONALIDADE_OK", List.of(
                        "Protocolos J.A.R.V.I.S. online. Sistemas prontos, calibração britânica ativa. Ao seu dispor, senhor(a). 🎩",
                        "Interface J.A.R.V.I.S. inicializada. Todos os subsistemas operando em máxima eficiência para auxiliá-lo(a)."
                )),
                entry("MSG_VAZIA", List.of(
                        "Nenhuma instrução detectada na transmissão, senhor(a). Aguardo suas diretrizes."
                )),
                entry("ERRO_UNKNOWN", List.of(
                        "Receio não ter decodificado sua solicitação com precisão. Solicito que reformule seus parâmetros, senhor(a)."
                )),
                entry("SALDO_POSITIVO", List.of(
                        "a posição patrimonial do período mantém-se em superávit recomendável."
                )),
                entry("SALDO_NEGATIVO", List.of(
                        "a posição do período indica déficit operacional exigindo atenção de vossa senhoria."
                ))
        ));
    }

    public String escolher(String chave) {
        return escolher(Personalidade.SEXTA_FEIRA, chave);
    }

    public String escolher(Personalidade personalidade, String chave) {
        Personalidade p = (personalidade != null) ? personalidade : Personalidade.SEXTA_FEIRA;
        Map<String, List<String>> mapa = catalogo.getOrDefault(p, catalogo.get(Personalidade.SEXTA_FEIRA));

        List<String> lista = mapa.get(chave);
        if (lista == null || lista.isEmpty()) {
            lista = catalogo.get(Personalidade.SEXTA_FEIRA).get(chave);
        }
        if (lista == null || lista.isEmpty()) return "";

        int i = ThreadLocalRandom.current().nextInt(lista.size());
        return lista.get(i);
    }
}
