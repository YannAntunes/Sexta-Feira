# Sexta-Feira — Assistente Financeiro e de Investimentos

Monorepo da assistente virtual **Sexta-Feira**, composta por um backend em **Java 17 + Spring Boot 4 (`sexta-feira-api`)** e um serviço de interpretação de linguagem natural e geração de gráficos em **Python 3.13 + FastAPI (`sexta-feira-ia`)**.

---

## 1. Pré-requisitos de Execução Local

- **Java:** JDK 17+ (testado com OpenJDK Temurin 17.0.17)
- **Maven:** Incluído via Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Banco de Dados (Desenvolvimento/Produção):** MySQL 8.0+ rodando em `localhost:3306` com o banco `sexta_feira_db` criado:
  ```sql
  CREATE DATABASE IF NOT EXISTS sexta_feira_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- **Python:** Python 3.11+ (testado com Python 3.13.14 em `sexta-feira-ia/venv`)

---

## 2. Como Executar o Serviço Python (`sexta-feira-ia`)

No diretório `sexta-feira-ia`:

```powershell
cd sexta-feira-ia

# Criar ambiente virtual (caso ainda não exista)
python -m venv venv

# Ativar o ambiente virtual (Windows PowerShell)
.\venv\Scripts\Activate.ps1

# Instalar dependências
pip install -r requirements.txt

# Iniciar o servidor FastAPI na porta 8000
.\venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

- **Healthcheck:** `GET http://127.0.0.1:8000/`
- **Documentação interativa (Swagger):** `http://127.0.0.1:8000/docs`

### Executar Testes do Serviço Python
```powershell
cd sexta-feira-ia
.\venv\Scripts\python.exe -m unittest discover -s tests -v
```

---

## 3. Como Executar o Backend Java (`sexta-feira-api`)

No diretório `sexta-feira-api`:

```powershell
cd sexta-feira-api

# Executar a aplicação (requer MySQL ativo na porta 3306)
.\mvnw.cmd spring-boot:run
```

- **Healthcheck:** `GET http://localhost:8080/api/v1/ping`

### Executar Testes do Backend Java
Os testes automatizados utilizam banco em memória **H2** (configurado em `src/test/resources/application.properties`), sem tocar no banco MySQL local e com schedulers desativados:

```powershell
cd sexta-feira-api
.\mvnw.cmd test
```

---

## 4. Integração entre os Módulos (`sexta-feira-api` ↔ `sexta-feira-ia`)

O backend Java (`sexta-feira-api`) se comunica com o serviço Python (`sexta-feira-ia`) via HTTP JSON usando a propriedade unificada `sexta.ia.url` (padrão `http://127.0.0.1:8000`):

| Cliente Java | Endpoint Python | Finalidade |
| :--- | :--- | :--- |
| `TransacaoIaService.rotearMensagem()` | `POST /ia/router` | Identificar a intenção (`intent`), idioma (`lang`) e entidades (`entities`). |
| `TransacaoIaService.interpretarMensagem()` | `POST /ia/transacoes/interpretar` | Extrair valor, data, tipo e categoria de uma transação em linguagem natural. |
| `PythonChartsClient.gerarGraficoGastosPorCategoria()` | `POST /ia/charts/gastos-por-categoria` | Gerar PNG de barras com gastos por categoria. |
| `PythonChartsClient.gerarGraficoOrcamentoVsGasto()` | `POST /ia/charts/orcamento-vs-gasto` | Gerar PNG comparativo de Orçamento vs Gasto. |
| `PythonChartsClient.gerarGraficoSerieLinha()` / `ChartsService.gerarSerieLinhaPng()` | `POST /ia/charts/serie-linha` | Gerar PNG de série temporal (evolução de gastos ou patrimônio). |

### Timeouts e Tratamento de Indisponibilidade
- O cliente HTTP (`HttpConfig.java`) possui timeouts configuráveis:
  - `http.client.connect-timeout-ms` (padrão: `5000` ms)
  - `http.client.read-timeout-ms` (padrão: `15000` ms)
- Caso o serviço Python esteja offline ou exceda o timeout, o `AssistenteService` e o `GlobalExceptionHandler` capturam a falha e retornam uma mensagem amigável (`503 Service Unavailable` nos endpoints de recursos ou resposta textual no `/api/v1/chat`), sem expor stack traces internos.

---

## 5. Inteligência Artificial, Personalidades e Memória Conversacional

O projeto utiliza uma **arquitetura híbrida de IA** projetada para alta resiliência, isolamento e custo controlado:

### Roteador Híbrido (LLM + Fallback Determinístico)
- O serviço `sexta-feira-ia` tenta primeiro classificar a intenção via LLM (OpenAI, Groq, Ollama, Gemini ou qualquer endpoint compatível com o formato OpenAI Chat Completions).
- Se a LLM estiver desabilitada (`LLM_ENABLED=false`), sem credencial configurada ou indisponível/com timeout, o sistema realiza **fallback silencioso e instantâneo** para o motor determinístico baseado em regex e palavras-chave.
- Zero dependências externas novas no Python: a comunicação com o provedor de LLM utiliza `urllib.request` da biblioteca padrão.

### Variáveis de Ambiente para IA (`.env`)
```bash
LLM_ENABLED=false
LLM_PROVIDER=openai
LLM_BASE_URL=https://api.openai.com/v1
LLM_API_KEY=
LLM_MODEL=gpt-4o-mini
LLM_TIMEOUT_SECONDS=10
```

### Modos de Personalidade do Assistente
O assistente suporta 4 personas distintas configuradas por usuário em `Usuario.personalidade`:
1. `SEXTA_FEIRA`: Ágil, inteligente, sarcástica e espirituosa.
2. `FORMAL`: Polida, técnica, estritamente profissional e corporativa.
3. `AMIGAVEL`: Gentil, acolhedora, encorajadora e empática com emojis.
4. `JARVIS`: Sofisticado, britânico, altamente refinado e focado em eficiência.

Para alternar a personalidade pelo chat ou WhatsApp, basta enviar:
> *"Mude para o modo jarvis"*, *"Quero a personalidade formal"*, *"Ative o modo amigável"* ou *"Volte para a sexta-feira"*.

### Memória Conversacional Multiusuário
- Todas as interações (`USER` e `ASSISTANT`) são persistidas na entidade `MensagemHistorico`, associada estritamente ao `Usuario`.
- Uma janela deslizante configurável (padrão: 10 mensagens mais recentes) é injetada nas requisições do roteador para manter a coerência temporal do diálogo sem consumir tokens excessivos.

---

## 6. Governança, Qualidade e Documentação Técnica

O projeto segue padrões estritos de governança e engenharia de software:

- [Instruções para Agentes de IA (`AGENTS.md`)](AGENTS.md)
- [Arquitetura do Sistema (`docs/ARCHITECTURE.md`)](docs/ARCHITECTURE.md)
- [Padrões de Código (`docs/CODING_STANDARDS.md`)](docs/CODING_STANDARDS.md)
- [Estratégia de Testes (`docs/TESTING_STRATEGY.md`)](docs/TESTING_STRATEGY.md)
- [Diretrizes de Segurança (`docs/SECURITY.md`)](docs/SECURITY.md)
- [Portões de Qualidade (`docs/QUALITY_GATES.md`)](docs/QUALITY_GATES.md)
- [Definição de Pronto (`docs/DEFINITION_OF_DONE.md`)](docs/DEFINITION_OF_DONE.md)
- [Fluxo de Desenvolvimento (`docs/DEVELOPMENT_WORKFLOW.md`)](docs/DEVELOPMENT_WORKFLOW.md)
- [Auditoria e Diagnóstico do Projeto (`docs/PROJECT_AUDIT.md`)](docs/PROJECT_AUDIT.md)
- [ADR 0001: Governança e Arquitetura Base](docs/adr/0001-governanca-e-arquitetura-base.md)
- [ADR 0002: Evolução da Inteligência (LLM, Memória e Personalidade)](docs/adr/0002-evolucao-inteligencia-llm-e-ferramentas.md)

### Execução dos Portões de Qualidade Automatizados
Para verificar a integridade completa do projeto (testes Java, testes Python, build Maven, verificação de segredos e aderência às regras):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-quality-gates.ps1
```
