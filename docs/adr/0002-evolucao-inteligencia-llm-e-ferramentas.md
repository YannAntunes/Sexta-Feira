# ADR-0002: Arquitetura Híbrida de Inteligência Artificial, Memória Conversacional e Motor de Personalidades

- **Status:** Proposto e Implementado
- **Data:** 23/10/2026
- **Contexto:** Projeto Sexta-Feira (Etapa 4 — Evolução da Inteligência)
- **Decisores:** Arquiteto de Sistemas, Engenheiro de Software Sênior

---

## 1. Contexto e Diagnóstico da Abordagem Anterior

A auditoria e estabilização inicial (Etapas 0 a 3) estabeleceram uma base sólida de execução, testes com H2 e isolamento estrito de dados por `Usuario`.

No entanto, a camada de interpretação de mensagens em `sexta-feira-ia/main.py` era puramente baseada em expressões regulares e palavras-chave. Uma avaliação rigorosa dessa abordagem identificou as seguintes limitações críticas:

1. **Fragilidade Sintática e Gírias Não Catalogadas:** Variações idiomáticas comuns (ex.: *"salva aí 30 contos que paguei no pastel"*, *"torrei 120 no shopping"*, *"comprei 5 papéis de Itaú"*) falham ou caem em `UNKNOWN` por ausência de correspondência exata.
2. **Inexistência de Memória Conversacional (Anáfora e Continuidade):** Perguntas de acompanhamento como *"quanto gastei em alimentação?"* seguidas de *"e no lazer?"* ou *"apaga esse último"* falham porque o serviço é completamente stateless e não recebe nem armazena o histórico recente do diálogo.
3. **Personalidade Inerte:** Embora o enum `Personalidade` (`SEXTA_FEIRA`, `FORMAL`, `AMIGAVEL`, `JARVIS`) já existisse no modelo de domínio de `Usuario`, ele não era operacionalizado: o sistema sempre respondia com strings fixas sarcásticas, sem possibilidade de personalização pelo usuário.
4. **Acoplamento e Risco de Indisponibilidade de LLMs:** Depender 100% de chamadas síncronas a uma API externa de LLM (OpenAI, Anthropic, Gemini, Groq) introduz riscos de latência, custos imprevisíveis e falha de comunicação caso a chave expire ou a rede oscile.

---

## 2. Decisão Arquitetural

Adotar uma **Arquitetura Híbrida de Dois Níveis com Fallback Determinístico Garantido**, composta por:

### 2.1. Roteamento Híbrido no Módulo Python (`sexta-feira-ia`)
1. **Cliente Genérico e Leve para LLMs (`LlmRouter`):**
   - Implementado utilizando a biblioteca padrão do Python (`urllib.request` e `json`), sem adicionar dependências pesadas ou arriscar conflitos de versão no ambiente.
   - Suporte nativo ao protocolo padrão da indústria (**OpenAI-compatible Chat Completions API**), permitindo conexão direta com provedores na nuvem (OpenAI, Groq, Gemini via endpoint compatível) ou modelos locais totalmente gratuitos e offline (Ollama, vLLM, LM Studio).
   - Controlado via variáveis de ambiente com defaults seguros para desenvolvimento offline:
     - `LLM_ENABLED=false` (padrão de desenvolvimento e testes; permite rodar sem internet e sem custo).
     - `LLM_BASE_URL=https://api.openai.com/v1` (ou `http://localhost:11434/v1` para Ollama local).
     - `LLM_API_KEY=` (vazia por padrão).
     - `LLM_MODEL=gpt-4o-mini` (ou `llama3`, `mistral`, etc.).
     - `LLM_TIMEOUT_SECONDS=3.0` (timeout rígido para evitar lentidão).
2. **Fallback Determinístico Automático e Silencioso:**
   - Se `LLM_ENABLED=false`, se `LLM_API_KEY` não for configurada para endpoints externos, se houver timeout ou se o modelo retornar uma resposta inválida, o sistema aciona imediatamente as regras e expressões regulares consolidadas na Etapa 3.
   - **Garantia:** O assistente **nunca** fica fora do ar por falha de provedor de IA.

### 2.2. Memória Conversacional Isolada por Usuário (`sexta-feira-api`)
1. **Persistência de Histórico de Diálogos:**
   - Criação da entidade JPA `MensagemHistorico` associada obrigatoriamente ao `Usuario`, com campos: `usuario_id`, `papel` (`USER`, `ASSISTANT`), `conteudo`, `intencao`, `criado_em`.
   - Repositório `MensagemHistoricoRepository` com busca ordenada por data decrescente e filtrada por `Usuario`.
2. **Janela Deslizante de Contexto:**
   - Para controlar custos de tokens e evitar estouro de janela de contexto, o serviço recupera no máximo as últimas $N$ mensagens (padrão: 10 mensagens) do usuário específico para envio ao roteador de IA.
   - Os dados conversacionais são estritamente isolados: o histórico do Usuário A jamais é enviado para inferência de mensagens do Usuário B.

### 2.3. Motor de Personalidades Operacional (`PersonalidadeEngine`)
1. **Ativação Real dos Modos de Personalidade:**
   - O `FrasesService` e o `AssistenteService` passam a consultar a `personalidade` do `Usuario` autenticado na requisição.
   - Suporte aos 4 perfis:
     - `SEXTA_FEIRA`: Sarcástica, ácida, irônica e pragmática.
     - `FORMAL`: Executiva, concisa, polida e técnica.
     - `AMIGAVEL`: Empática, calorosa, incentivadora e compreensiva.
     - `JARVIS`: Extremamente formal, britânica, polida e servil ("Ao seu dispor, senhor(a)").
2. **Comando de Linguagem Natural para Troca de Personalidade (`SET_PERSONALITY`):**
   - Detecção de intenção para comandos como *"mude para o modo formal"*, *"ative o jarvis"*, *"quero modo amigável"*, *"volte para o modo sexta-feira"*.
   - A alteração é persistida no cadastro do `Usuario` no banco de dados.

### 2.4. Camada de Ferramentas Controladas (Tool Calling Determinístico)
- A LLM ou as regras apenas **identificam a intenção e os parâmetros estruturados** (`intent` e `entities`).
- A execução das ações financeiras (salvar despesa, excluir, consultar status, comprar ativo) é executada **exclusivamente pelos serviços Java já testados e autenticados**, com transações controladas (`@Transactional`) e isolamento no `UsuarioContextHolder`.
- Nenhuma LLM executa SQL ou código diretamente no banco de dados.

---

## 3. Consequências e Trade-Offs

- **Positivas:**
  - O sistema passa a compreender frases complexas, contextuais e com gírias quando um provedor de LLM estiver ativo.
  - O projeto funciona 100% offline e passa em todos os testes mesmo sem qualquer chave de API de LLM configurada (graças ao fallback determinístico).
  - A experiência do usuário torna-se verdadeiramente personalizada com a alternância de modos de personalidade.
  - Segurança de dados garantida: o isolamento multiusuário da Etapa 3 é preservado em todas as camadas da memória conversacional.
- **Negativas / Mitigações:**
  - *Latência da rede:* Mitigada por timeout agressivo de 3 a 4 segundos na chamada da LLM, com fallback imediato.
  - *Custo de API:* Mitigado pelo uso de modelos leves (`gpt-4o-mini`, `llama-3-8b`) e pela janela deslizante curta de histórico (10 mensagens).
