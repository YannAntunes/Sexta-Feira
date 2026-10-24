# Sexta-Feira — Evolução da Inteligência, Voz, Gráficos e Interface

## 1. Visão Geral

Este documento consolida a evolução completa da assistente **Sexta-Feira**, transformando-a de uma interface visual em uma assistente pessoal verdadeiramente funcional, com processamento de linguagem natural refinado em português brasileiro e inglês, voz feminina natural com síntese fonética higienizada, personalidade unificada coerente, telemetria gráfica interativa e layout responsivo com holograma 3D expansível.

---

## 2. Diagnóstico Técnico e Resolução das Causas Raízes

### 2.1. Comandos Financeiros Coloquiais Não Reconhecidos
- **Causa Comprovada:** No arquivo `sexta-feira-ia/main.py`, a função `detectar_intencao()` restringia o reconhecimento de movimentações a verbos estritos (`gastei`, `paguei`, `comprei`, `recebi`, `ganhei`). Expressões cotidianas brasileiras como *"torrei"*, *"saiu da conta"*, *"caiu um dinheiro"*, gírias de valores (*"quinhentão"*, *"80 conto"*, *"dois barões"*) e números por extenso (*"dois mil"*) eram classificados como `UNKNOWN`. Adicionalmente, quando `extrair_valor()` não encontrava dígitos numéricos literais, retornava silenciosamente `Decimal("0.00")`.
- **Resolução Implementada:**
  - Criação da base de vocabulário e gírias estruturada em `sexta-feira-ia/data/vocabulario_financeiro.json`.
  - Construção do motor de NLU avançado em `sexta-feira-ia/core/nlu_engine.py` com conversão de números por extenso, resolução de termos monetários colloquiais, detecção de intenção semântica e proteção de ambiguidade.
  - Integração no roteador oficial `sexta-feira-ia/main.py`.

### 2.2. Resposta Repetitiva ("IA Milionária")
- **Causa Comprovada:** O catálogo `catalogo.put(Personalidade.SEXTA_FEIRA, ...)` em `FrasesService.java` continha uma única string estática para a chave `ERRO_UNKNOWN`. Sempre que o roteador de IA retornava `UNKNOWN`, o backend repetia invariavelmente a mesma frase sarcástica.
- **Resolução Implementada:**
  - Substituição da entrada estática por um conjunto variado de respostas adaptativas e orientadoras em `FrasesService.java`, que explicam o que faltou e orientam o usuário a fornecer valor e categoria de forma natural.

### 2.3. Síntese de Voz Masculina e Pausas Artificiais
- **Causa Comprovada:** Em `frontend/src/services/voice.ts`, a seleção de voz utilizava `voices.find(v => v.lang.includes('pt-BR'))`, o que no Windows selecionava a primeira voz da lista: `Microsoft Daniel` (voz masculina). Além disso, o texto das mensagens era enviado diretamente ao sintetizador contendo marcações de Markdown (`**`, `-`, `#`), URLs completas (`http://...`) e valores formatados como `R$ 500,00` (pronunciado de forma robótica como "erre cifrão quinhentos vírgula zero zero").
- **Resolução Implementada:**
  - Implementação do seletor inteligente `selectBestFemaleVoice()` priorizando vozes femininas naturais em pt-BR (`Microsoft Francisca Online`, `Microsoft Maria`, `Google português do Brasil`, `Luciana`, `Helena`).
  - Função de higienização fonética `sanitizeForSpeech()`: conversão automática de moedas (`R$ 500,00` -> `500 reais`, `R$ 80,50` -> `80 reais e 50 centavos`), remoção de URLs, eliminação de sintaxe markdown e atenuação de pontuação para pausas de respiração orgânicas.
  - Suporte a Barge-in instantâneo em qualquer estado.

### 2.4. Gráficos Não Interativos e Botão Inoperante
- **Causa Comprovada:** O assistente respondia a pedidos de gráficos retornando URLs em texto puro (`http://localhost:8080/api/v1/carteira/grafico... Abre no navegador`), e o botão da barra superior apenas alternava um painel lateral textual sem nenhuma renderização gráfica real.
- **Resolução Implementada:**
  - Criação do modal interativo `frontend/src/components/hud/HudChartsModal.tsx` com renderização de gráficos vetoriais em SVG (Donut de Despesas por Categoria, Evolução Diária de Gastos, Comparativo Orçamento vs Gasto) e renderização gráfica de alta resolução via Python Matplotlib.
  - Novos endpoints JSON em `ChartsController.java` (`/gastos-categoria/dados`, `/orcamento-gasto/dados`, `/serie-gastos/dados`).
  - Abertura automática do modal quando o usuário solicita gráficos por voz ou chat.

### 2.5. Personalidade Unificada da Sexta-Feira
- **Causa Comprovada:** O cabeçalho exibia seletores manuais para `FORMAL`, `AMIGAVEL`, `JARVIS` e `SEXTA_FEIRA`, diluindo a identidade da assistente.
- **Resolução Implementada:**
  - Remoção dos seletores secundários do HUD, consolidando a identidade única **Sexta-Feira**: humor inteligente, sarcasmo elegante e empatia financeira.

### 2.6. Holograma Imersivo e Janela de Conversa Recolhível
- **Causa Comprovada:** O layout mantinha o chat fixo na lateral, limitando o holograma 3D a um quadrante da tela.
- **Resolução Implementada:**
  - Adição de botão de recolhimento no topo direito do holograma.
  - No modo recolhido, o holograma 3D expande para 100% da tela, com controles flutuantes de microfone, barge-in e abertura rápida do chat.
  - Refinamento das animações nos estados `IDLE`, `LISTENING`, `THINKING`, `SPEAKING` e `ERROR`, com suporte completo a `prefers-reduced-motion`.

---

## 3. Matriz de Testes e Evidências

| Teste / Cenário | Componente | Resultado |
|---|---|---|
| "Hoje torrei 500 reais." | Python NLU + Spring Boot API | **PASS** (ADD_TRANSACTION, R$ 500.00, DESPESA) |
| "Gastei quinhentão com roupa." | Python NLU + Spring Boot API | **PASS** (ADD_TRANSACTION, R$ 500.00, DESPESA, OUTROS) |
| "Paguei 80 conto no almoço." | Python NLU + Spring Boot API | **PASS** (ADD_TRANSACTION, R$ 80.00, DESPESA, ALIMENTACAO) |
| "Saiu 120 da minha conta." | Python NLU + Spring Boot API | **PASS** (ADD_TRANSACTION, R$ 120.00, DESPESA) |
| "Recebi dois mil do trabalho." | Python NLU + Spring Boot API | **PASS** (ADD_TRANSACTION, R$ 2000.00, RECEITA) |
| "Caiu um dinheiro na conta." | Python NLU + Spring Boot API | **PASS** (CLARIFICATION_REQUIRED, pergunta valor exato) |
| "Gastei com roupa." | Python NLU + Spring Boot API | **PASS** (CLARIFICATION_REQUIRED, pergunta valor exato) |
| "Não, eu quis dizer 50, não 500." | Python NLU + Spring Boot API | **PASS** (CORRECTION, atualiza último lançamento) |
| "Desfaz o último lançamento." | Python NLU + Spring Boot API | **PASS** (DELETE_TRANSACTION, remove última transação) |
| "Vendi 5 ações por 200 reais cada." | Python NLU + Spring Boot API | **PASS** (ADD_TRANSACTION, R$ 1000.00, RECEITA, INVESTIMENTOS) |
| "vendi cinco acoes por duzentos reais cada" | Python NLU (Transcrição de voz por extenso) | **PASS** (ADD_TRANSACTION, R$ 1000.00, RECEITA) |
| "Me mostra o gráfico das minhas despesas." | Python NLU + Frontend | **PASS** (ASK_FINANCES_CHART, abre HudChartsModal) |
| Suíte de Testes Python (`test_nlu_avancado.py`) | sexta-feira-ia | **PASS** (34 testes executados, 0 falhas) |
| Suíte de Testes Java (`mvnw.cmd test`) | sexta-feira-api | **PASS** (28 testes executados, 0 falhas) |
| Compilação Frontend (`tsc -b && vite build`) | frontend | **PASS** (Zero erros de compilação) |
| Quality Gates Automatizados (`verify-quality-gates.ps1`) | Repositório | **PASS** (6/6 Gates aprovados, 0 falhas, 0 avisos) |

---

## 4. Correções das Causas Raízes Identificadas em Produção

### 4.1. Defeito na Venda de Ativos e Omissão de Receitas nos Gráficos
- **Causa Comprovada no NLU:** Frases como *"Vendi 5 ações por 200 reais cada"* extraíam apenas o número 200 como quantidade ou eram classificadas como `DESPESA` porque `determinar_tipo()` verificava apenas `recebi/salario/ganhei`.
- **Causa Comprovada nos Gráficos:** O `ChartsService.java` filtrava explicitamente `if (t.getTipo() != TipoTransacao.DESPESA) continue;` em todos os métodos existentes (`gastosPorCategoriaDoMesAtual`, `serieGastosDiariosMesAtual`). Não havia nenhum endpoint nem representação visual para entradas, vendas ou receitas nos gráficos.
- **Correções Aplicadas:**
  - `core/nlu_engine.py`: Adicionado extrator para vendas de ativos com quantidade e preço unitário (tanto por dígitos numéricos quanto por palavras por extenso: *"cinco"*, *"duzentos"*), multiplicando `qty * unit_price = 1000.0` com `RECEITA` em `INVESTIMENTOS`.
  - `main.py`: Atualizada função `determinar_tipo()` para incluir verbos e substantivos de entradas financeiras (`vendi`, `venda`, `dividendos`, `rendimento`, etc.). Implementado logger de diagnóstico seguro com mascaramento de CPFs e números de cartão.
  - `ChartsService.java` & `ChartsController.java`: Implementados métodos e endpoints `GET /api/v1/charts/receitas-categoria/dados` e `GET /api/v1/charts/balanco-mensal/dados` com `BalancoMensalDTO` (totalReceitas, totalDespesas, saldo, taxaPoupanca).
  - `HudChartsModal.tsx`: Adicionada nova aba principal **"Balanço (Receitas vs Despesas)"** com barras comparativas proporcionais SVG e KPIs de saúde financeira, além de alternador **Despesas / Receitas** na aba de categorias e auto-recarga quando novas transações ocorrem.

### 4.2. Seletor de Vozes, Amostras Acústicas e Arquitetura TTS
- **Implementação:** Criação do modal acessível `HudVoiceSettingsModal.tsx` integrado ao `HudHeader.tsx`. O modal enumera as vozes do sistema operacional, destaca as vozes femininas naturais recomendadas para pt-BR, permite prévia acústica com frases em pt-BR e en, interrompe imediatamente o áudio (Barge-in instantâneo), salva a preferência em `localStorage` e apresenta comparativo entre Web Speech nativo e Neural TTS Streaming.

### 4.3. Estabilização do Holograma 3D durante a Fala
- **Causa Comprovada:** O serviço `voice.ts` emitia números pseudoaleatórios abruptos (`0.35 + Math.random() * 0.45`) a cada 70ms via `onAudioLevel`, disparando ~14 re-renderizações por segundo da árvore inteira do React em `App.tsx`. Ao mesmo tempo, o `HologramCanvas.tsx` deformava os vértices da malha sem amortecimento temporal nem trava de amplitude, gerando serrilhamento e deformação errática.
- **Correções Aplicadas:**
  - `voice.ts`: Substituído ruído branco aleatório por um envelope senoidal suave que imita o perfil harmônico da fala humana.
  - `HologramCanvas.tsx`: Implementado filtro passa-baixas temporal (`lerp 0.12`), trava estrita de amplitude máxima de deslocamento (`Math.min(pulseAmp, 0.20)`) e deformação harmônica esférica suave.
  - `App.tsx`: Implementado throttling de atualizações de telemetria de áudio via `useRef`, desacoplando os loops Three.js e Web Audio das re-renderizações da árvore React.

### 4.4. Abertura da Aplicação com Chat Recolhido por Padrão
- **Implementação:** Estado inicial `isChatCollapsed: true` em `App.tsx`. Na abertura da aplicação, o holograma 3D ocupa 100% da área visível com grade tática, visualizador de frequências e botão flutuante para abertura rápida do chat e acionamento por voz.
