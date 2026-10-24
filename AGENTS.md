# AGENTS.md — Diretrizes Oficiais para Agentes de Engenharia de IA

Este documento é a **fonte primária de governança** para qualquer agente de IA (Antigravity, Claude Code, Codex, Cursor, Copilot, etc.) que atue no repositório **Sexta-Feira**.

**Regra Zero:** Nenhuma alteração é considerada concluída apenas porque o código foi gerado ou compila. Toda modificação deve cumprir os critérios de [docs/QUALITY_GATES.md](docs/QUALITY_GATES.md) e [docs/DEFINITION_OF_DONE.md](docs/DEFINITION_OF_DONE.md), comprovados pela execução real do script `scripts/verify-quality-gates.ps1`.

---

## 1. Mapa de Documentação Oficial (Leitura Obrigatória por Contexto)

Antes de planejar ou alterar qualquer arquivo, consulte o documento oficial correspondente:

| Documento | Quando Consultar | Responsabilidade Oficial |
| :--- | :--- | :--- |
| [docs/PROJECT_AUDIT.md](docs/PROJECT_AUDIT.md) | Antes de iniciar uma nova etapa do roadmap | Diagnóstico inicial, bugs conhecidos e prioridades. |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Ao criar classes, endpoints, integrações ou alterar fluxos | Arquitetura real Java/Python, contratos e limites de módulos. |
| [docs/CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | Ao escrever ou refatorar código Java ou Python | Convenções de pacotes, DTOs, `BigDecimal`, tipagem e tratamento de erros. |
| [docs/SECURITY.md](docs/SECURITY.md) | Ao tocar em configurações, usuários, queries, webhooks ou IA | Gestão de segredos, isolamento por `Usuario`, validação e logs seguros. |
| [docs/TESTING_STRATEGY.md](docs/TESTING_STRATEGY.md) | Antes e depois de implementar qualquer mudança | Regras para testes unitários, integração, isolamento e comandos de teste. |
| [docs/QUALITY_GATES.md](docs/QUALITY_GATES.md) | Antes de declarar qualquer tarefa concluída | Critérios objetivos que aprovam ou bloqueiam uma entrega. |
| [docs/DEFINITION_OF_DONE.md](docs/DEFINITION_OF_DONE.md) | No fechamento de cada tarefa ou fase | Checklist final de conclusão e formato de relatório. |
| [docs/DEVELOPMENT_WORKFLOW.md](docs/DEVELOPMENT_WORKFLOW.md) | Durante todo o ciclo de vida da tarefa | Passo a passo obrigatório do requisito até o relatório final. |

---

## 2. Como Compreender o Projeto Antes de Alterar Arquivos

1. **Verifique o módulo afetado:**
   - `sexta-feira-api/`: Backend principal em Java 17 + Spring Boot 4.0.1. Detém o banco de dados MySQL, as regras financeiras (`modes/finance`), identidade (`identity`) e orquestração (`core`).
   - `sexta-feira-ia/`: Serviço auxiliar em Python 3.13 + FastAPI. Responsável apenas por classificar intenções/entidades (`/ia/router`), extrair dados de transações (`/ia/transacoes/interpretar`) e plotar gráficos PNG (`/ia/charts/*`).
2. **Respeite a fronteira de responsabilidades:**
   - O serviço Python (`sexta-feira-ia`) **NUNCA** acessa o banco de dados diretamente nem executa regras de negócio financeiras.
   - O backend Java (`sexta-feira-api`) é a **única autoridade** sobre persistência, saldos, cálculos financeiros, permissões e isolamento de dados por usuário.
3. **Inspecione antes de assumir:** Leia os repositórios, serviços e testes relacionados antes de propor mudanças. Nunca presuma que um campo ou filtro existe sem verificar a classe JPA correspondente.

---

## 3. Regras Inegociáveis de Engenharia e Segurança

1. **Isolamento Multi-Usuário (`Usuario`):**
   - Toda operação sobre transações, orçamentos, alertas, ativos de carteira, movimentos e snapshots deve estar vinculada a um `Usuario` válido.
   - É proibido usar `findAll()` irrestrito ou `findTopByOrderByIdDesc()` global para operações de usuário final.
   - Nunca confie em um identificador enviado pelo cliente sem validação conforme definido em [docs/SECURITY.md](docs/SECURITY.md).
2. **Integridade Monetária:**
   - No Java, valores monetários e quantidades de ativos **DEVEM** usar `BigDecimal` com `RoundingMode.HALF_UP` explícito em divisões. É proibido usar `float` ou `double` para cálculos financeiros.
   - No Python, valores monetários extraídos devem usar `Decimal`.
3. **Gestão de Segredos:**
   - É terminantemente proibido inserir senhas, tokens de API, chaves de LLM ou números de telefone reais em `application.properties`, código-fonte, testes ou arquivos Markdown.
   - Segredos devem ser lidos via variáveis de ambiente (`${VAR_NAME}`) e documentados apenas com placeholders fictícios em `.env.example`.
4. **Proibição de Frontend:**
   - **NÃO** crie, redesenhe nem adicione código de frontend (React, Angular, Vue, HTML/CSS/JS) neste repositório sem ordem explícita do proprietário do projeto.

---

## 4. Como Executar Testes e Verificações Obrigatórias

Toda alteração deve ser verificada executando o script oficial na raiz do repositório:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-quality-gates.ps1
```

Ou individualmente por módulo:
- **Backend Java (`sexta-feira-api`):**
  ```powershell
  cd sexta-feira-api
  .\mvnw.cmd test
  ```
- **Serviço Python (`sexta-feira-ia`):**
  ```powershell
  cd sexta-feira-ia
  .\venv\Scripts\python.exe -m unittest discover -s tests -v
  ```

> **Importante:** Os testes Java rodam sobre H2 em memória (`src/test/resources/application.properties`). Nunca altere a configuração de testes para apontar para o banco MySQL real do usuário.

---

## 5. Ações Proibidas Sem Autorização Explícita

Um agente de IA **NÃO PODE**, sob nenhuma circunstância, sem autorização expressa do usuário na conversa atual:

1. Executar `git commit`, `git push`, `git reset --hard`, `git rebase` ou reescrever o histórico Git (`git filter-branch` / `git filter-repo`).
2. Executar comandos destrutivos no banco de dados MySQL (`DROP DATABASE`, `DROP TABLE`, `TRUNCATE`).
3. Remover funcionalidades existentes do assistente ou alterar contratos públicos de API sem documentar e justificar o impacto.
4. Desativar testes (`@Disabled`, `unittest.skip`), suprimir falhas no script de Quality Gates ou comentar asserções para forçar um build verde.
5. Instalar novas bibliotecas ou provedores externos (ex: SDKs de LLM pagos) sem antes aprovar a proposta técnica com o usuário.

---

## 6. Como Reportar Resultados e Limitações

Ao concluir qualquer tarefa ou fase, o agente deve apresentar o relatório estruturado exigido em [docs/DEFINITION_OF_DONE.md](docs/DEFINITION_OF_DONE.md), diferenciando claramente:
- Problemas que **já existiam** no código;
- Problemas **corrigidos** na tarefa atual;
- Verificações **realmente executadas** (com comando e saída real);
- Verificações que **não puderam ser executadas** e o motivo.
