# Auditoria Completa do Projeto — Sexta-Feira (`PROJECT_AUDIT.md`)

**Data da Auditoria:** 23/10/2026  
**Repositório Local:** `C:\Users\User\Desktop\Projetos TI\sexta-feira`  
**Escopo desta Etapa:** Etapa 0 — Diagnóstico completo de arquitetura, código, integrações, banco de dados, segurança, testes e plano de execução priorizado (nenhuma linha de código funcional foi alterada).

---

## 1. Arquitetura Atual Confirmada

O repositório é um monorepo contendo dois módulos independentes que se comunicam via HTTP síncrono (JSON / PNG bytes):

```mermaid
flowchart LR
    subgraph Clientes / Entradas
        WA["Webhook WhatsApp\nPOST /whatsapp/webhook"]
        API_CLIENT["Cliente HTTP / REST\nPOST /api/v1/chat\n/api/v1/transacoes, etc."]
        SCHED["Schedulers Internos\n(@Scheduled)"]
    end

    subgraph "sexta-feira-api (Java 17 / Spring Boot 4.0.1 :8080)"
        CTRL["Controllers REST\n(9 Controllers)"]
        ASSIST["AssistenteService\n(Orquestrador Central)"]
        DOM_FIN["Serviços Financeiros\nTransacao / Orcamento / Carteira\nRelatorio / Snapshot / Monitoramento"]
        DOM_ID["Identity\nUsuarioService"]
        REPOS["Spring Data JPA\n(7 Repositories)"]
    end

    subgraph "sexta-feira-ia (Python 3.13 / FastAPI :8000)"
        PY_ROUTER["POST /ia/router\nPOST /ia/transacoes/interpretar"]
        PY_CHARTS["POST /ia/charts/*\n(Matplotlib PNG)"]
    end

    subgraph "Persistência & Serviços Externos"
        MYSQL[("MySQL 8.0.44\nsexta_feira_db")]
        BRAPI["Brapi API\n(Ações / FIIs)"]
        COINGECKO["CoinGecko API\n(Cripto BTC/ETH)"]
        FRANK["Frankfurter API\n(Câmbio ECB)"]
        WA_OUT["WhatsApp Provider\n(Simulado via stdout)"]
    end

    WA --> CTRL
    API_CLIENT --> CTRL
    SCHED --> DOM_FIN
    CTRL --> DOM_ID
    CTRL --> ASSIST
    CTRL --> DOM_FIN
    ASSIST --> PY_ROUTER
    ASSIST --> DOM_FIN
    DOM_FIN --> PY_CHARTS
    DOM_FIN --> REPOS
    DOM_ID --> REPOS
    REPOS --> MYSQL
    DOM_FIN --> BRAPI
    DOM_FIN --> COINGECKO
    DOM_FIN --> FRANK
    DOM_FIN --> WA_OUT
```

### 1.1. Módulo Backend (`sexta-feira-api`)
- **Stack:** Java 17 (Temurin 17.0.17), Spring Boot `4.0.1`, Spring Data JPA (Hibernate ORM `7.2.0.Final`), Bean Validation, MySQL Connector/J.
- **Total de arquivos Java:** 83 classes de produção (`src/main/java`) + 1 classe de teste (`src/test/java`).
- **Organização de Pacotes (`br.com.yann.sextafeira`):**
  - `core`: Configuração HTTP (`RestTemplate`), controllers gerais (`ChatController`, `PingController`, `ToolsController`), webhook (`WhatsappWebhookController`), orquestrador (`AssistenteService`, `FrasesService`), integração Python (`PythonChartsClient`) e WhatsApp (`WhatsappService`).
  - `identity`: Entidade `Usuario`, `UsuarioRepository` e `UsuarioService`.
  - `modes.finance`: Domínio financeiro completo (7 controllers, 11 modelos/enums, 6 repositories, 20 DTOs, 3 schedulers, 12 services e integração Brapi).

### 1.2. Módulo de Inteligência e Gráficos (`sexta-feira-ia`)
- **Stack:** Python `3.13.14` (em `sexta-feira-ia/venv`), FastAPI `0.128.0`, Pydantic `2.12.5`, Uvicorn `0.40.0`, Matplotlib `3.10.8`.
- **Estado Estrutural:** Todo o código funcional (794 linhas) reside em `sexta-feira-ia/main.py`. Há uma árvore de pacotes preparada (`app/api/routes.py`, `app/charts/plotter.py`, `app/core/dates.py`, `app/core/normalize.py`, `app/core/parsers.py`, `app/models/*`, `app/routers/finance_router.py`), porém **todos esses 9 arquivos dentro de `app/` estão com 0 bytes (vazios)**. Além disso, `git status` aponta que `app/main.py` foi movido para `main.py` na raiz do módulo sem commit.

---

## 2. Mapeamento de Endpoints e Contratos

### 2.1. Endpoints Java (`sexta-feira-api` — Porta `8080`)

| Controller | Método e Rota | Entrada | Saída | Efeito Colateral / Observação |
| :--- | :--- | :--- | :--- | :--- |
| `PingController` | `GET /api/v1/ping` | — | `String` (`"SEXTA-FEIRA online"`) | Healthcheck simples. |
| `ChatController` | `POST /api/v1/chat` | `ChatRequest` (`mensagem`) | `ChatResponse` (`resposta`) | Chama Python `/ia/router` e executa ação (cria/remove transação, altera carteira, define orçamento, consulta relatórios). |
| `WhatsappWebhookController` | `POST /whatsapp/webhook` | `WhatsappIncomingMessageDTO` (`from`, `message`) | `200 OK` (`void`) | Cria/busca `Usuario` pelo `from`, chama `assistenteService.processarMensagem(texto)` **sem passar o usuário**, e imprime resposta no console. |
| `ToolsController` | `POST /api/v1/tools/convert` | `ConvertRequest` (`amount`, `from`, `to`) | `ConvertResponse` | Consulta taxa em `api.frankfurter.dev`. |
| `TransacaoController` | `GET /api/v1/transacoes` | — | `List<Transacao>` | Retorna **todas** as transações do banco (`findAll()`). |
| `TransacaoController` | `POST /api/v1/transacoes` | `Transacao` (entidade JPA direta) | `Transacao` (`201`) | Salva transação diretamente sem DTO de entrada nem `@Valid`. |
| `TransacaoController` | `GET /api/v1/transacoes/resumo` | `@RequestParam ano, mes` | `ResumoMensalDTO` | Soma receitas, despesas e saldo do mês. |
| `TransacaoController` | `POST /api/v1/transacoes/natural` | `NaturalLanguageTransacaoRequest` | `NaturalLanguageTransacaoResponse` (`201`) | Chama Python `/ia/transacoes/interpretar` e salva no MySQL. |
| `TransacaoController` | `DELETE /api/v1/transacoes/ultima` | — | `Transacao` | Apaga a última transação global (`findTopByOrderByIdDesc()`). |
| `TransacaoController` | `POST /api/v1/transacoes/remover-por-texto` | `DeletePorTextoRequest` | `Transacao` | Interpreta texto no Python e apaga transação candidata dos últimos 60 dias. |
| `OrcamentoController` | `POST /api/v1/orcamentos` | `OrcamentoRequest` | `Orcamento` (`201`) | Cria ou atualiza limite mensal por categoria. |
| `OrcamentoController` | `GET /api/v1/orcamentos/status` | `@RequestParam ano, mes, categoria` | `StatusOrcamentoDTO` | Calcula gasto vs limite da categoria no mês. |
| `RelatoriosController` | `GET /api/v1/relatorios/mensal` | — | `RelatorioMensalResponse` | Gera resumo mensal, top 3 categorias, alertas e URLs de gráficos. |
| `MonitoramentoController` | `POST /api/v1/monitoramento/checar-agora` | — | `String` | Dispara verificação imediata de estouro/80%/90% de orçamentos e grava `AlertaOrcamento`. |
| `ChartsController` | `GET /api/v1/charts/gastos-mes.png` | — | `byte[]` (`image/png`) | Busca gastos do mês e gera gráfico de barras no Python. |
| `ChartsController` | `GET /api/v1/charts/orcamento-vs-gasto.png` | — | `byte[]` (`image/png`) | Gera gráfico comparativo Orçamento vs Gasto no Python. |
| `ChartsController` | `GET /api/v1/charts/evolucao-gastos-mes.png` | — | `byte[]` (`image/png`) | Gera série diária de gastos em linha no Python. |
| `CarteiraGraficoController` | `GET /api/v1/carteira/grafico` | `@RequestParam range, days, classe` | `byte[]` (`image/png`) | Gera gráfico de linha da evolução patrimonial a partir de `SnapshotCarteira`. |
| `PortfolioChartController` | `GET /api/v1/charts/portfolio/last` | — | `byte[]` (`image/png`) | Gera gráfico usando o estado em memória `ultimoRankingContext` do singleton `SnapshotCarteiraService`. |

### 2.2. Endpoints Python (`sexta-feira-ia` — Porta `8000`)

| Método e Rota | Entrada (Pydantic) | Saída | Função |
| :--- | :--- | :--- | :--- |
| `GET /` | — | `{"status": "ok", "service": "sexta-feira-ia"}` | Healthcheck. |
| `POST /ia/transacoes/interpretar` | `MensagemRequest` (`mensagem`) | `TransacaoInterpretada` (`valor`, `data`, `tipo`, `categoria`, `descricao`) | Extração baseada em regex e palavras-chave. |
| `POST /ia/router` | `MensagemRequest` (`mensagem`) | `RouterResponse` (`intent`, `entities`, `lang`) | Classifica entre 16 intenções + `UNKNOWN` e extrai entidades. |
| `POST /ia/charts/gastos-por-categoria` | `ChartCategoriaRequest` | `StreamingResponse` (`image/png`) | Gráfico de barras simples via Matplotlib (`plt`). |
| `POST /ia/charts/orcamento-vs-gasto` | `ChartBudgetRequest` | `StreamingResponse` (`image/png`) | Gráfico de barras duplas via Matplotlib (`plt`). |
| `POST /ia/charts/serie-linha` | `ChartSerieRequest` | `StreamingResponse` (`image/png`) | Gráfico de linha temporal via Matplotlib (`plt`). |

---

## 3. Estado das Funcionalidades

### 3.1. Funcionalidades Confirmadas e Implementadas
- **Registro e exclusão de transações em linguagem natural:** Implementado via `/ia/router`, `/ia/transacoes/interpretar`, `AssistenteService` e `TransacaoService`.
- **Consultas de gastos, resumos mensais e por período:** Implementado (`TODAY`, `YESTERDAY`, `THIS_WEEK`, `LAST_WEEK`, `THIS_MONTH`, `LAST_MONTH`, `LAST_N_DAYS`).
- **Dicas de economia comparando semana atual vs semana passada:** Implementado em `EconomiaService.sugerirCortesSemana()`.
- **Orçamentos por categoria e alertas de limite (80%, 90% e estouro):** Implementado tanto no momento de inserção via chat (`AssistenteService`) quanto em background (`MonitoramentoService` / `MonitoramentoScheduler`) com deduplicação mensal na tabela `alertas_orcamento`.
- **Carteira de investimentos (Ações, FIIs, Cripto):** Adição/venda por quantidade (`ADD_HOLDING_QTY`, `SELL_HOLDING_QTY`) e aporte por valor em reais (`ADD_HOLDING_VALUE`) convertendo pela cotação atual.
- **Snapshots diários, cálculo de rentabilidade e ranking (Top N melhores/piores):** Implementado em `SnapshotCarteiraService` e `CarteiraPerformanceService`.
- **Cotações externas e câmbio:** Integração implementada com Brapi (`CotacaoService`), CoinGecko (`CotacaoService`) e Frankfurter (`CurrencyService`).
- **Geração de gráficos PNG:** Implementada ponta a ponta entre Java e Python.

### 3.2. Funcionalidades Parcialmente Implementadas ou Desconectadas
1. **Isolamento Multi-Usuário e Vínculo com `Usuario` (0% conectado ao domínio financeiro):**
   - A entidade `Usuario` (`usuarios`) e o `UsuarioService.buscarOuCriarPorWhatsapp()` existem.
   - No entanto, **nenhuma entidade financeira** (`Transacao`, `Orcamento`, `AlertaOrcamento`, `AtivoCarteira`, `MovimentoCarteira`, `SnapshotCarteira`) possui chave estrangeira (`usuario_id` / `@ManyToOne Usuario`).
   - O próprio `WhatsappWebhookController` contém o comentário explícito na linha 41: `// 2) processar mensagem (por enquanto ainda sem usar o usuário)`.
2. **Modos de Personalidade (`Personalidade.java`):**
   - O enum `Personalidade` (`SEXTA_FEIRA`, `FORMAL`, `AMIGAVEL`, `JARVIS`) existe e está mapeado na entidade `Usuario`.
   - Porém, **não é lido em nenhum serviço**. Todas as respostas do `AssistenteService`, `FrasesService`, `EconomiaService`, `RelatorioService` e `CarteiraPerformanceService` têm frases sarcásticas fixas (hardcoded) no código Java.
3. **Integração de Envio WhatsApp (`WhatsappService.java`):**
   - É apenas um stub que executa `System.out.println("Simulando envio para WhatsApp...")`. O `RestTemplate`, `whatsappApiUrl` e `whatsappApiToken` são injetados na classe, mas nunca são utilizados.
4. **Modularização do Serviço Python (`sexta-feira-ia/app/*`):**
   - A estrutura de pastas `app/api`, `app/charts`, `app/core`, `app/models`, `app/routers` foi criada, mas todos os arquivos estão vazios; o código permanece monolítico em `main.py`.
5. **DTO duplicado/não utilizado:**
   - `DefinirOrcamentoRequest.java` e o enum `TipoAtivo.java` existem no projeto, mas não são usados por nenhuma classe.

### 3.3. Funcionalidades que Não Puderam Ser Verificadas Ponta a Ponta nesta Auditoria
- **Envio real para provedor WhatsApp:** Não há implementação HTTP real em `WhatsappService` e a URL configurada é um placeholder (`https://SUA_URL_DO_PROVEDOR_AQUI`).
- **Cotação Brapi autenticada com múltiplos ativos:** `brapi.token` não está definido no `application.properties`, dependendo do tier público da Brapi.

---

## 4. Testes Executados e Resultados Reais

Durante a auditoria, as ferramentas e testes existentes foram executados no ambiente local:

### 4.1. Ambiente Detectado
- **Java:** OpenJDK Temurin `17.0.17+10` (64-bit).
- **Banco de Dados Local:** MySQL `8.0.44` ativo em `localhost:3306` com schema `sexta_feira_db` acessível.
- **Python:** `Python 3.13.14` no ambiente virtual `sexta-feira-ia\venv`.

### 4.2. Execução do Maven (`sexta-feira-api`)
1. **Comando padrão (`mvnw.cmd test`):**
   - **Resultado:** **FALHOU (`BUILD FAILURE`)** logo na fase `maven-resources-plugin:3.3.1:resources`.
   - **Causa:** `MalformedInputException: Input length = 1` ao filtrar `src/main/resources/application.properties`. O arquivo `application.properties` está salvo em codificação `ISO-8859-1` / `Windows-1252` (contém caracteres acentuados como `padrão`, `Configuração`, `serviço` em 1 byte), enquanto o Spring Boot 4.0.1 / Maven tenta filtrá-lo como `UTF-8`.
2. **Comando contornando encoding (`mvnw.cmd test -Dproject.build.sourceEncoding=ISO-8859-1`):**
   - **Resultado:** **PASSOU (`BUILD SUCCESS` — 1 teste executado, 0 falhas, 36.1s)**.
   - **Observação crítica sobre o teste existente:** O único teste (`SextaFeiraApiApplicationTests.contextLoads()`) sobe o contexto real do Spring Boot conectando ao banco MySQL local (`sexta_feira_db`), executa `Hibernate ddl-auto=update` (rodando `ALTER TABLE` reais) e dispara o `MonitoramentoScheduler` durante o teste! Não há isolamento de banco de testes nem testes unitários das regras de negócio.

### 4.3. Verificação do Serviço Python (`sexta-feira-ia`)
1. **Testes automatizados existentes:** **0 arquivos de teste** (`pytest` sequer está listado no `requirements.txt`).
2. **Diagnóstico de dependências (`requirements.txt` vs `venv`):**
   - `matplotlib` (e seus pacotes transitivos `numpy`, `pillow`, `contourpy`, `kiwisolver`, `pyparsing`, `python-dateutil`, `cycler`, `fonttools`) está instalado no `venv`, mas **NÃO consta no `requirements.txt`**. Uma instalação limpa via `pip install -r requirements.txt` falhará ao iniciar o `main.py` com `ModuleNotFoundError: No module named 'matplotlib'`.
3. **Smoke test funcional executado na auditoria:**
   - Foi executado um teste de importação e chamada direta das funções de `main.py` usando o Python do `venv`.
   - **Descoberta de Bug Real no Router:** Ao testar `main.router(MensagemRequest(mensagem='quanto gastei essa semana'))`, o serviço retornou **`intent='ADD_TRANSACTION'`** com `valor=0.0` em vez de `ASK_PERIOD_SUMMARY`!
   - **Causa raiz do bug:** Na função `detectar_intencao(mensagem)` (`main.py` linhas 5411-5415), a verificação de `ADD_TRANSACTION` testa se `"gastei"` está na frase **antes** de verificar `ASK_PERIOD_SUMMARY` (linhas 5456-5463), que procura `"quanto gastei"` / `"gastei quanto"`. Como `"quanto gastei essa semana"` contém `"gastei"`, toda pergunta do usuário com a palavra `"gastei"` é interceptada como adição de despesa de R$ 0,00!

---

## 5. Riscos de Segurança e Problemas Identificados (Classificados por Severidade)

### 🔴 Severidade CRÍTICA (Bloqueantes — Segurança e Isolamento)

1. **[SEC-01] Credenciais e Dados Pessoais Expostos em Texto Puro no Código e no Histórico Git**
   - **Onde:** `sexta-feira-api/src/main/resources/application.properties` (presente desde o commit inicial `000a467` até `HEAD`).
   - **Impacto:** Senha do banco de dados MySQL (`spring.datasource.password`), token da API do WhatsApp (`whatsapp.api.token`) e número de telefone pessoal (`monitoramento.whatsapp.to`) estão hardcoded e versionados no repositório público do GitHub.
   - **Ação necessária:** Substituir por variáveis de ambiente, criar `.env.example`, e **rotacionar imediatamente** a senha do banco e o token do provedor.

2. **[SEC-02] Ausência Absoluta de Isolamento de Dados por Usuário (Multi-Tenancy Inexistente)**
   - **Onde:** Todas as entidades financeiras (`Transacao`, `Orcamento`, `AlertaOrcamento`, `AtivoCarteira`, `MovimentoCarteira`, `SnapshotCarteira`), repositórios, serviços e controllers.
   - **Impacto:** O sistema opera como um banco global único. Qualquer mensagem recebida no webhook de qualquer número de WhatsApp lê, cria, altera ou deleta transações, orçamentos e ativos da mesma carteira global. Constraints únicas como `@UniqueConstraint(columnNames = {"classe", "ticker"})` em `AtivoCarteira` e `uk_snapshot (data, classe, ticker)` em `SnapshotCarteira` inclusive **impedem** a nível de schema que dois usuários tenham o mesmo ativo.

3. **[SEC-03] Ausência de Autenticação e Autorização nos Endpoints REST e no Webhook**
   - **Onde:** Todos os 9 `@RestController` em `sexta-feira-api`.
   - **Impacto:** Qualquer cliente com acesso à porta 8080 pode listar todas as transações (`GET /api/v1/transacoes`), apagar lançamentos (`DELETE /api/v1/transacoes/ultima`) ou forjar requisições para `POST /whatsapp/webhook` informando qualquer número no campo `from`.

---

### 🟠 Severidade ALTA (Bugs Funcionais, Integridade Financeira e Concorrência)

4. **[BUG-01] Colisão de Intenção no Python: `"quanto gastei..."` Cadastra Transação de R$ 0,00**
   - **Onde:** `sexta-feira-ia/main.py` (`detectar_intencao`, linhas 5411 vs 5456).
   - **Impacto:** Perguntas legítimas de consulta (`"quanto gastei hoje"`, `"quanto eu gastei essa semana"`, `"gastei quanto ontem"`) caem no `if` de `ADD_TRANSACTION` porque contêm a substring `"gastei"`, salvando transações lixo de `R$ 0.00` no banco em vez de retornar o resumo do período.

5. **[BUG-02] Dupla Contabilização de Movimentos e Cotações Duplicadas em `adicionarPorValorBRL`**
   - **Onde:** `CarteiraService.java` (linhas 3844-3869).
   - **Impacto:** Quando o usuário aporta em reais (ex: `"adicione 20 reais em BTC"`), o método `adicionarPorValorBRL`:
     1. Consulta a cotação externa;
     2. Grava um registro `MovimentoCarteira` do tipo `APORTE_BRL` com `valorBRL = 20.00`;
     3. Em seguida, chama `adicionarQuantidade(classe, tickerFinal, qtd)` (linha 3868) em vez de `atualizarPosicaoSemMovimento()` (que inclusive já existe na linha 3825 e está sem uso!).
     4. Como consequência, `adicionarQuantidade` consulta a cotação externa novamente e grava um **segundo** `MovimentoCarteira` do tipo `COMPRA` para a mesma operação!
     5. No relatório (`CarteiraPerformanceService.gerarRelatorioPeriodo`, linhas 3402-3405), o cálculo `saldoPeriodo = aportes.subtract(compras).add(vendas)` fica distorcido e a quantidade comprada em `detalharAtivoNoPeriodo` (`inflow = compras.add(aportes)`) é **dobrada**.

6. **[BUG-03] Quebra do Build Maven por Encoding Inválido em `application.properties`**
   - **Onde:** `sexta-feira-api/src/main/resources/application.properties` e `pom.xml`.
   - **Impacto:** `mvnw test` ou `mvnw package` falha com `MalformedInputException: Input length = 1`.

7. **[BUG-04] `requirements.txt` Incompleto e `main.py` Desalinhado no Git**
   - **Onde:** `sexta-feira-ia/requirements.txt` e `sexta-feira-ia/main.py`.
   - **Impacto:** Falta `matplotlib` no `requirements.txt`. Além disso, `app/main.py` consta como deletado (`D`) e `main.py` como untracked (`??`) no Git.

8. **[CONC-01] Estado Mutável Compartilhado em Singleton (`SnapshotCarteiraService`)**
   - **Onde:** `SnapshotCarteiraService.java` (linha 4733: `private RankingContextDTO ultimoRankingContext;`) e `PortfolioChartController.java`.
   - **Impacto:** Condição de corrida severa entre requisições/usuários.

9. **[CONC-02] Dois Schedulers Duplicados para Snapshot da Carteira**
   - **Onde:** `SnapshotCarteiraScheduler.java` (roda às 02:00 sem timezone definido) e `SnapshotSchedulerService.java` (roda às 18:00 `America/Sao_Paulo`).
   - **Impacto:** Execução redundante da geração diária de snapshots.

10. **[CONC-03] Uso da Interface Estadual `matplotlib.pyplot` (`plt`) em Servidor Web Concorrente**
    - **Onde:** `sexta-feira-ia/main.py` (linhas 5887-5952).
    - **Impacto:** `plt.figure()`, `plt.bar()`, `plt.plot()` manipulam a máquina de estados global do Matplotlib, que não é thread-safe nem tinha backend `Agg` forçado explicitamente. Requisições simultâneas de gráficos podem misturar séries de dados na mesma imagem ou causar crash da thread.

---

### 🟡 Severidade MÉDIA (Resiliência, Precisão e Dívida Técnica)

11. **[RES-01] `RestTemplate` sem Timeouts Configurados e Duplicado**
    - **Onde:** `HttpConfig.java` instancia `new RestTemplate()` sem `connectTimeout` ou `readTimeout`. Além disso, `ChartsService.java` (linha 3968) ignora o bean do Spring e instancia seu próprio `new RestTemplate()`.
    - **Impacto:** Se Brapi, CoinGecko, Frankfurter ou o serviço Python travarem, as threads do Tomcat ficam bloqueadas indefinidamente.

12. **[CFG-01] Inconsistência nas Propriedades de URL do Serviço Python**
    - **Onde:**
      - `TransacaoIaService` usa `@Value("${sexta.ia.url}")` (configurado no `application.properties` como `http://127.0.0.1:8000`).
      - `PythonChartsClient` usa `@Value("${ia.base-url:http://127.0.0.1:8000}")` (chave diferente!).
      - `ChartsService` usa constante hardcoded `private static final String IA_BASE = "http://localhost:8000";`.
      - Links gerados em relatórios (`AssistenteService`, `RelatorioService`) usam `http://localhost:8080/...` hardcoded.

13. **[FIN-01] Uso de `float` / `double` em Contratos Intermediários e Extração Regex Frágil**
    - **Onde:**
      - Em `main.py` (`extrair_valor`, linha 5236): `re.search(r"\d+[.,]?\d*", mensagem)` captura o **primeiro número que encontrar na frase**. Exemplo: na frase `"no dia 15 gastei 40 reais no mercado"`, `extrair_valor` extrairá **`15`** como sendo o valor da transação em reais! E na frase `"comprei 2 lanches por 35,90"`, extrairá **`2`** em vez de `35.90`!
      - Em `CarteiraGraficoController.java` (linha 1531) e nos modelos de gráfico do Python (`ChartCategoriaItem`, `ChartBudgetItem`, `ChartSerieItem`), valores monetários são convertidos para `double`/`float`. Para plotagem visual isso é aceitável, mas no `router` do Python (`main.py` linha 5841 e `parse_amount` linha 5538), `amount` e `valor` trafegam como `float` no dicionário `entities`.

14. **[FIN-02] Permite Venda de Ativo Sem Saldo ou Quantidade Negativa Sem Validação Clara**
    - **Onde:** `CarteiraService.adicionarQuantidade` (linhas 3805-3819).
    - **Impacto:** Se o usuário não tem `BBAS3` (qtd = 0) e diz `"vendi 5 ações da BBAS3"`, `novaQtd` fica `-5`. O método ainda assim grava um `MovimentoCarteira` de `VENDA` de 5 ações (contaminando o relatório de vendas do período) e só depois descarta o `AtivoCarteira`.

15. **[DB-01] Ausência de Transações (`@Transactional`) em Operações Multi-Tabela**
    - **Onde:** `CarteiraService` (`adicionarQuantidade`, `adicionarPorValorBRL`) e `SnapshotCarteiraService` (`gerarSnapshotDoDia`, que faz `deleteAll` seguido de vários `save` em loop com chamadas HTTP externas no meio!).
    - **Impacto:** Se uma chamada HTTP de cotação ou banco falhar no meio do `gerarSnapshotDoDia`, os snapshots anteriores daquele dia já foram deletados (`repo.deleteAll`) e não sofrem rollback.

---

## 6. Plano de Execução Priorizado

Seguindo estritamente a governança definida para o projeto, as próximas fases devem ser executadas na ordem abaixo, **uma fase por vez**, com revisão e aprovação ao final de cada etapa:

### Etapa 1 — Estabilização e Recuperação da Capacidade de Execução
1. Converter `application.properties` para `UTF-8` e fixar `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` no `pom.xml`.
2. Configurar perfil de teste (`src/test/resources/application-test.properties` + H2/isolamento ou desativação de schedulers em teste) para que `mvnw test` não altere o banco de desenvolvimento nem dispare schedulers.
3. Regularizar o Git e dependências do `sexta-feira-ia`: consolidar localização de `main.py`, atualizar `requirements.txt` (incluindo `matplotlib`, `pytest`, `httpx`) e remover ou estruturar os arquivos vazios em `app/`.
4. Unificar a propriedade `sexta.ia.url`, injetar `RestTemplate` único com timeouts (`connectTimeout` e `readTimeout`) e adicionar tratamento de falha de comunicação (`RestClientException`) entre Java e Python sem expor stack traces.

### Etapa 2 — Documentação Técnica, Governança e Quality Gates Automatizados
1. Criar os documentos oficiais:
   - `AGENTS.md` (na raiz)
   - `docs/ARCHITECTURE.md`
   - `docs/CODING_STANDARDS.md`
   - `docs/TESTING_STRATEGY.md`
   - `docs/SECURITY.md`
   - `docs/QUALITY_GATES.md`
   - `docs/DEFINITION_OF_DONE.md`
   - `docs/DEVELOPMENT_WORKFLOW.md`
2. Implementar script executável de verificação automatizada (Quality Gate Runner) que valide compilação, testes Java, testes Python e ausência de segredos hardcoded.

### Etapa 3 — Segurança, Isolamento Multi-Usuário e Integridade Financeira (CONCLUÍDA)
1. **Gestão de Segredos:** Externalizar `spring.datasource.password`, `whatsapp.api.token` e `monitoramento.whatsapp.to` para variáveis de ambiente; criar `.env.example` e atualizar `.gitignore`. *(CONCLUÍDO)*
2. **Isolamento de Dados por Usuário (`Usuario`):**
   - Associar `Usuario` a `Transacao`, `Orcamento`, `AlertaOrcamento`, `AtivoCarteira`, `MovimentoCarteira` e `SnapshotCarteira` (ajustando unique constraints para incluir `usuario_id`). *(CONCLUÍDO)*
   - Propagar o contexto do usuário autenticado/identificado em `AssistenteService` e em todos os serviços e repositórios financeiros via `UsuarioContextHolder` e `UsuarioRequestFilter`. *(CONCLUÍDO)*
   - Escrever testes automatizados provando que o Usuário A não lê, altera ou deleta dados do Usuário B. *(CONCLUÍDO - 12 testes em `IsolamentoUsuarioIntegrationTest`)*
3. **Correção dos Bugs de Integridade Financeira e Concorrência:**
   - Corrigir a dupla contabilização em `CarteiraService.adicionarPorValorBRL` ([BUG-02]) e validar saldo antes de venda ([FIN-02]). *(CONCLUÍDO)*
   - Corrigir a precedência de intenção `"quanto gastei"` ([BUG-01]) e o parser de valor com números de data/quantidade ([FIN-01]) em `sexta-feira-ia`. *(CONCLUÍDO)*
   - Adicionar `@Transactional` onde aplicável, eliminar `ultimoRankingContext` global ([CONC-01]), remover scheduler duplicado ([CONC-02]) e usar API Orientada a Objetos (`Figure`) no Matplotlib ([CONC-03]). *(CONCLUÍDO)*
4. **Script de Migração e Backfill:** `docs/migrations/etapa3-multiusuario.sql` gerado com instruções manuais para associar dados pré-existentes ao usuário dono e remover constraints legadas no MySQL. *(CONCLUÍDO)*

---

## 7. Matriz de Rastreabilidade e Resolução dos Problemas Identificados

| ID | Classificação | Problema | Etapa de Resolução | Status | Evidência de Verificação |
|---|---|---|---|---|---|
| `[SEC-01]` | 🔴 Crítica | Segredos expostos em `application.properties` | Etapa 3 | **RESOLVIDO** | `verify-quality-gates.ps1` (GATE-03) executado sem `-AllowLegacySecrets` |
| `[SEC-02]` | 🔴 Crítica | Falta de isolamento multiusuário nas entidades financeiras | Etapa 3 | **RESOLVIDO** | `IsolamentoUsuarioIntegrationTest` (6 entidades isoladas por `Usuario`) |
| `[SEC-03]` | 🔴 Crítica | Endpoints sem auth / webhook sem validação / SSRF | Etapa 3 | **RESOLVIDO** | Validação de token em `WhatsappWebhookController`, regex em tickers e moedas |
| `[BUG-01]` | 🟠 Alta | `"quanto gastei"` cria transação zerada | Etapa 3 | **RESOLVIDO** | `sexta-feira-ia/tests/test_main.py` (teste de intenção `ASK_PERIOD_SUMMARY`) |
| `[BUG-02]` | 🟠 Alta | Duplo movimento e cotação em `adicionarPorValorBRL` | Etapa 3 | **RESOLVIDO** | `IsolamentoUsuarioIntegrationTest.aportePorValorRegistraApenasUmMovimento` |
| `[BUG-03]` | 🟠 Alta | Build Maven falha por encoding ISO-8859-1 | Etapa 1 | **RESOLVIDO** | `pom.xml` e `application.properties` convertidos para UTF-8 |
| `[BUG-04]` | 🟠 Alta | `requirements.txt` incompleto e divergência de entrypoint | Etapa 1 | **RESOLVIDO** | `requirements.txt` atualizado e `app/main.py` sincronizado com `main.py` |
| `[CONC-01]` | 🟠 Alta | `ultimoRankingContext` mutável em singleton | Etapa 3 | **RESOLVIDO** | Contexto movido para mapa concorrente isolado por `Usuario` |
| `[CONC-02]` | 🟠 Alta | Schedulers duplicados (02:00 e 18:00) | Etapa 3 | **RESOLVIDO** | `SnapshotCarteiraScheduler.java` removido; scheduler único às 18:00 |
| `[CONC-03]` | 🟠 Alta | `plt.figure()` em web server multi-thread | Etapa 1 | **RESOLVIDO** | `matplotlib.figure.Figure` com backend `Agg` explícito |
| `[RES-01]` | 🟡 Média | `RestTemplate` sem timeouts configurados | Etapa 1 | **RESOLVIDO** | `HttpConfig.java` configurado com timeouts de conexão e leitura |
| `[RES-02]` | 🟡 Média | Base URL do Python descentralizada | Etapa 1 | **RESOLVIDO** | Unificado em `${sexta.ia.url:http://127.0.0.1:8000}` em todos os serviços |
| `[FIN-01]` | 🟡 Média | Parser de valor confunde datas e tickers com quantias | Etapa 3 | **RESOLVIDO** | `_limpar_tokens_nao_monetarios` em `main.py` e testes unitários |
| `[FIN-02]` | 🟡 Média | Venda sem saldo cria movimento fantasma | Etapa 3 | **RESOLVIDO** | `IsolamentoUsuarioIntegrationTest.vendaSemSaldoERejeitadaENaoRegistraMovimento` |
| `[DB-01]` | 🟡 Média | Falta de `@Transactional` em serviços multi-tabela | Etapa 3 | **RESOLVIDO** | `@Transactional` em `TransacaoService`, `OrcamentoService`, `CarteiraService`, `SnapshotCarteiraService` |
| `[IA-01]` | 🟡 Média | Classificação puramente estática e sem LLM | Etapa 4 | **RESOLVIDO** | Roteador híbrido em `main.py`, suporte a OpenAI-compatible endpoints e fallback seguro |
| `[IA-02]` | 🟡 Média | Personalidades inoperantes em `FrasesService` | Etapa 4 | **RESOLVIDO** | `FrasesService` reimplementado com os 4 modos operacionais (`SEXTA_FEIRA`, `FORMAL`, `AMIGAVEL`, `JARVIS`) e intenção `SET_PERSONALITY` |
| `[IA-03]` | 🟡 Média | Ausência de memória conversacional contextual | Etapa 4 | **RESOLVIDO** | Entidade `MensagemHistorico` isolada por `Usuario`, janela deslizante em `MensagemHistoricoService` |

---

### Etapa 4 — Evolução da Inteligência (IA, Ferramentas Controladas, Memória e Personalidade) (CONCLUÍDA E VERIFICADA)
1. **Decisão Arquitetural (ADR 0002):**
   - Especificada em `docs/adr/0002-evolucao-inteligencia-llm-e-ferramentas.md`. Adotada arquitetura híbrida (LLM com Function Calling/Structured Outputs + fallback determinístico por regex).
   - Zero dependências externas novas no Python (utilização de `urllib.request` da biblioteca padrão para comunicação com provedores OpenAI-compatible).
2. **Roteador Híbrido e Fallback no Python (`sexta-feira-ia`):**
   - `main.py` atualizado com suporte a `personalidade` e `historico` no `MensagemRequest`.
   - Implementada a função `tentar_roteamento_llm(request)` com proteção contra injeção de prompt e fallback automático e silencioso para o motor regex se a LLM estiver desabilitada, sem chave ou com falha de rede/timeout.
   - Adicionada a intenção `SET_PERSONALITY` e a função auxiliar `parse_personality`.
   - 11 testes unitários em `sexta-feira-ia/tests/test_main.py` cobrindo o roteamento, intenções, parsers e fallback.
3. **Memória Conversacional e Janela Deslizante (`sexta-feira-api`):**
   - Criada a entidade JPA `MensagemHistorico` associada obrigatoriamente a `Usuario`, com enum `PapelMensagem` (`USER`, `ASSISTANT`, `SYSTEM`) e índice temporal.
   - Criado `MensagemHistoricoRepository` e `MensagemHistoricoService` provendo histórico recente limitado (janela deslizante) com isolamento rigoroso por usuário.
4. **Motor de Personalidade com 4 Modos Operacionais (`sexta-feira-api`):**
   - `FrasesService.java` reescrito utilizando `Map.ofEntries`, com respostas contextuais variadas para `SEXTA_FEIRA`, `FORMAL`, `AMIGAVEL` e `JARVIS` em todas as situações de negócio.
   - `AssistenteService.java` atualizado para gravar mensagens no histórico, enviar histórico recente ao roteador, tratar `SET_PERSONALITY` com persistência na preferência do usuário e formatar as respostas na personalidade ativa.
5. **Testes Automatizados e Quality Gates:**
   - 5 novos testes de integração criados em `InteligenciaEPersonalidadeIntegrationTest.java` (totalizando 19 testes Java, 0 falhas).
   - 11 testes Python em `test_main.py` (0 falhas).
   - Todos os 5 Quality Gates validados com sucesso em `scripts/verify-quality-gates.ps1` (`GATE-01` a `GATE-05`, 0 falhas, 0 avisos).
