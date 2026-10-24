# ADR-0001: Governança de Qualidade e Separação de Responsabilidades Java/Python

- **Status:** Aceito
- **Data:** 23/10/2026
- **Contexto:** Projeto Sexta-Feira (Recuperação, Engenharia de Software, Segurança e Evolução)

---

## Contexto e Problema

O projeto Sexta-Feira cresceu de forma orgânica com dois módulos (`sexta-feira-api` em Java/Spring Boot e `sexta-feira-ia` em Python/FastAPI). A auditoria inicial ([docs/PROJECT_AUDIT.md](../PROJECT_AUDIT.md)) revelou:
1. Falta de isolamento do banco de dados durante os testes (os testes alteravam o MySQL local e disparavam schedulers).
2. Ausência de testes automatizados no serviço Python.
3. Uso de regras apenas descritivas sem verificação automatizada, permitindo que problemas como encoding inválido (`ISO-8859-1`), credenciais hardcoded e falta de isolamento por `Usuario` passassem despercebidos.

## Decisão

1. **Manter a arquitetura dual Java + Python com fronteira estrita:**
   - `sexta-feira-api` (Java/Spring Boot) é o único detentor do banco de dados MySQL, da autenticação/autorização, do isolamento por `Usuario` e das regras financeiras determinísticas.
   - `sexta-feira-ia` (Python/FastAPI) permanece stateless, atuando exclusivamente na interpretação de linguagem natural (`/ia/router`, `/ia/transacoes/interpretar`) e na geração de gráficos PNG (`/ia/charts/*`).
2. **Isolamento de Testes com H2 em modo MySQL:**
   - Todos os testes automatizados do Spring Boot rodam sobre H2 em memória (`MODE=MySQL`) com `spring.task.scheduling.enabled=false`, garantindo execução rápida, reproduzível e sem risco aos dados locais.
3. **Verificação Automatizada dos Documentos de Governança (`scripts/verify-quality-gates.ps1`):**
   - Os documentos Markdown (`AGENTS.md`, `SECURITY.md`, `TESTING_STRATEGY.md`, `QUALITY_GATES.md`, etc.) são acompanhados por um script executável que verifica a integridade documental, codificação UTF-8, presença de credenciais expostas, padrões estáticos de segurança/arquitetura e a execução real das suítes de teste Java e Python.

## Consequências

- **Positivas:** Qualquer regressão funcional, quebra de compilação, falha de teste ou introdução de segredo em texto puro é detectada automaticamente antes de aprovar uma fase.
- **Negativas / Trade-offs:** Exige que novas dependências Python sejam registradas imediatamente em `requirements.txt` e que qualquer alteração em entidades JPA seja compatível tanto com MySQL 8 quanto com H2 (`MODE=MySQL`).
