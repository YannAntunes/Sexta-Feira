# Estratégia Oficial de Testes — Sexta-Feira (`TESTING_STRATEGY.md`)

Este documento define a estratégia de testes automatizados para garantir que nenhuma alteração quebre funcionalidades existentes, corrompa cálculos financeiros ou exponha dados entre usuários.

---

## 1. Princípios Diretores

1. **Isolamento Total do Ambiente de Desenvolvimento:** Nenhum teste automatizado pode conectar, ler, inserir, alterar ou apagar dados do banco MySQL local (`sexta_feira_db`). Todos os testes do `sexta-feira-api` rodam obrigatoriamente sobre o banco em memória **H2** (`MODE=MySQL`) configurado em `sexta-feira-api/src/test/resources/application.properties`.
2. **Proibição de Dados Reais:** É proibido utilizar números de telefone reais, tokens reais ou históricos financeiros pessoais reais em fixtures e casos de teste.
3. **Independência de Rede Externa:** Testes unitários e de integração não devem depender de disponibilidade de APIs externas (Brapi, CoinGecko, Frankfurter) nem exigir que o processo Uvicorn esteja aberto manualmente em outro terminal; clientes HTTP externos devem ser mockados (`Mockito`) nos testes Java.

---

## 2. Níveis de Teste Exigidos

### 2.1. Testes Unitários (Java e Python)
- **Objetivo:** Validar cálculos financeiros, regras de domínio e parsers de linguagem natural de forma rápida (< 1s por teste).
- **No Python (`sexta-feira-ia/tests/`):**
  - Testar cada intenção do `router` (`ADD_TRANSACTION`, `DELETE_TRANSACTION`, `ASK_MONTH_SUMMARY`, `ASK_PERIOD_SUMMARY`, `SET_BUDGET`, `ASK_BUDGET_STATUS`, `ASK_BUDGET_OVERVIEW`, `CONVERT_CURRENCY`, `ASK_SAVING_TIPS`, `ASK_PORTFOLIO_*`, `ADD_HOLDING_*`, `SELL_HOLDING_QTY`).
  - Testar casos de colisão (ex: `"quanto gastei hoje"` **não** pode retornar `ADD_TRANSACTION`; `"dia 15 gastei 40 reais"` deve extrair valor `40` e dia `15`, nunca valor `15`).
  - Testar geração de gráficos PNG verificando `media_type == "image/png"` e assinatura de bytes PNG (`\x89PNG`).
- **No Java (`sexta-feira-api/src/test/java/`):**
  - Testar cálculos de `TransacaoService` (resumo mensal, remoção por texto com tolerância, resumo por intervalo).
  - Testar `OrcamentoService` e `MonitoramentoService` (limites de 80%, 90%, estouro e não-repetição de alerta no mesmo mês).
  - Testar `CarteiraService` e `CarteiraPerformanceService` (compra, venda, aporte em BRL sem duplicidade de movimento, cálculo de rentabilidade e ranking).

### 2.2. Testes de Isolamento Multi-Usuário e Autorização (Obrigatórios na Etapa 3)
- Toda entidade financeira deve possuir testes automatizados que criem **dois usuários distintos** (`Usuario A` e `Usuario B`) e comprovem que:
  1. Transações criadas pelo `Usuario A` não aparecem no resumo nem na listagem do `Usuario B`.
  2. O comando de remover última transação ou remover por texto acionado pelo `Usuario B` **jamais** apaga uma transação do `Usuario A`.
  3. Orçamentos, alertas, ativos de carteira, movimentos e snapshots do `Usuario A` são completamente invisíveis e inacessíveis ao `Usuario B`.
  4. Dois usuários diferentes conseguem cadastrar o mesmo ativo (ex: ambos compram `PETR4` ou `BTC`) sem violação de constraint única (`DataIntegrityViolationException`).

### 2.3. Testes de Contrato e Resiliência (Java ↔ Python e APIs Externas)
- Validar que os DTOs enviados/recebidos pelo `TransacaoIaService` e `PythonChartsClient` correspondem exatamente à estrutura JSON produzida pelo FastAPI.
- Validar que timeouts ou erros `500`/`Connection refused` vindos do Python, Brapi, CoinGecko ou Frankfurter são capturados graciosamente, retornando mensagens seguras sem derrubar a aplicação.

---

## 3. Critérios de Cobertura e Priorização

Não exigimos 100% de cobertura artificial em getters/setters ou classes triviais. A cobertura obrigatória concentra-se nos fluxos de risco:

| Camada / Componente | Meta Mínima de Cobertura de Cenários |
| :--- | :--- |
| Isolamento de Dados entre Usuários (`Usuario A` vs `Usuario B`) | **100%** dos serviços financeiros que leem ou alteram dados |
| Cálculos Financeiros (`TransacaoService`, `OrcamentoService`, `CarteiraService`, `CarteiraPerformanceService`) | **100%** das operações de mutação e cálculo de saldo/rentabilidade |
| Roteador de Intenções e Parsers (`sexta-feira-ia/main.py`) | **100%** das intenções suportadas + testes de regressão para bugs corrigidos |
| Tratamento de Falhas de Integração (`TransacaoIaService`, `CotacaoService`, `GlobalExceptionHandler`) | Cenários de sucesso + falha/indisponibilidade testados |

---

## 4. Comandos Oficiais de Execução

### Execução Completa (Quality Gate Runner)
Na raiz do repositório:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-quality-gates.ps1
```

### Execução Individual — Backend Java
```powershell
cd sexta-feira-api
.\mvnw.cmd test
```

### Execução Individual — Serviço Python
```powershell
cd sexta-feira-ia
.\venv\Scripts\python.exe -m unittest discover -s tests -v
```

---

## 5. Critérios para Aprovar ou Reprovar uma Alteração

Uma alteração será **REPROVADA (Bloqueada)** se:
1. Qualquer teste existente em Java ou Python falhar.
2. Uma correção de bug (como `[BUG-01]` ou `[BUG-02]`) for implementada sem um teste automatizado que reproduza o bug antes e comprove a correção depois.
3. Um teste depender de conexão com o MySQL real ou com APIs externas reais sem mock.
4. Algum teste for comentado, removido ou marcado com `@Disabled` / `@unittest.skip` para forçar a passagem da suíte.
