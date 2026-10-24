# Quality Gates Oficiais — Sexta-Feira (`QUALITY_GATES.md`)

Este documento define os **12 portões de qualidade (Quality Gates)** obrigatórios do projeto Sexta-Feira. Nenhuma tarefa, correção ou evolução pode ser classificada como **Aprovada** se violar qualquer critério bloqueante abaixo.

A verificação automatizada destes critérios é executada por:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-quality-gates.ps1
```

---

## Matriz de Critérios por Categoria

### 1. Correção Funcional
- **O que verificar:** O requisito solicitado funciona ponta a ponta sem quebrar intenções ou fluxos existentes.
- **Como verificar:** Execução dos testes unitários e de integração (`mvnw.cmd test` e `unittest`).
- **Resultado aprovado:** 100% dos testes da funcionalidade nova e das funcionalidades existentes passando (`0 Failures, 0 Errors`).
- **Falhas que bloqueiam:** Qualquer regressão funcional ou cálculo incorreto.

### 2. Padrões de Código
- **O que verificar:** Conformidade com [docs/CODING_STANDARDS.md](CODING_STANDARDS.md) (UTF-8, injeção via construtor, ausência de estado mutável em singletons, uso de `BigDecimal`/`Decimal`).
- **Como verificar:** Inspeção estática no `verify-quality-gates.ps1` + compilação limpa sem erros de encoding.
- **Resultado aprovado:** Arquivos em UTF-8 válido, sem `new BigDecimal(double)` e sem atributos mutáveis de sessão em `@Service`/`@Controller`.
- **Falhas que bloqueiam:** `MalformedInputException`, uso de `double`/`float` para cálculo monetário no Java ou estado mutável compartilhado entre usuários.

### 3. Arquitetura e Manutenção
- **O que verificar:** Respeito às fronteiras definidas em [docs/ARCHITECTURE.md](ARCHITECTURE.md) (Python stateless focado em IA/gráficos; Java detentor do banco e regras financeiras; sem criação de frontend).
- **Como verificar:** Revisão dos arquivos criados/modificados e checagem de escopo.
- **Resultado aprovado:** Nenhuma conexão de banco no Python; nenhum arquivo de frontend criado; responsabilidades preservadas.
- **Falhas que bloqueiam:** Acesso direto ao MySQL pelo Python ou introdução de código de frontend.

### 4. Testes e Regressão
- **O que verificar:** Existência e execução real de testes automatizados para toda mudança de código, conforme [docs/TESTING_STRATEGY.md](TESTING_STRATEGY.md).
- **Como verificar:** Execução de `scripts/verify-quality-gates.ps1`.
- **Resultado aprovado:** `BUILD SUCCESS` no Maven (usando H2 in-memory) e `OK` na suíte Python (`unittest`), com novos testes cobrindo a alteração realizada.
- **Falhas que bloqueiam:** Testes falhando, testes desativados (`@Disabled`/`skip`) ou testes conectando ao banco MySQL real.

### 5. Segurança
- **O que verificar:** Ausência de credenciais expostas, isolamento de dados por `Usuario` e proteção de endpoints/webhooks conforme [docs/SECURITY.md](SECURITY.md).
- **Como verificar:** Scanner de segredos em `scripts/verify-quality-gates.ps1` + testes automatizados de isolamento entre `Usuario A` e `Usuario B` (a partir da Etapa 3).
- **Resultado aprovado:** Zero segredos em texto puro; um usuário nunca acessa nem modifica dados de outro.
- **Falhas que bloqueiam:** Senha/token hardcoded em arquivos rastreados pelo Git ou vazamento de dados entre usuários.

### 6. Validação de Entradas e Tratamento de Erros
- **O que verificar:** Validação de payloads nulos/vazios, limites de tamanho de mensagem, validação de tickers/moedas e respostas de erro padronizadas pelo `GlobalExceptionHandler`.
- **Como verificar:** Testes de entradas inválidas e indisponibilidade do serviço Python.
- **Resultado aprovado:** Entradas inválidas retornam `400 Bad Request` ou mensagem orientativa no chat; falhas de integração retornam `503` ou mensagem amigável sem stack trace.
- **Falhas que bloqueiam:** Exposição de stack trace ou `NullPointerException` não tratado ao receber entrada malformada.

### 7. Banco de Dados e Integridade dos Dados
- **O que verificar:** Uso de `@Transactional` em operações multi-tabela, precisão `DECIMAL` compatível e constraints únicas coerentes com multi-usuário.
- **Como verificar:** Subida do schema JPA no H2 (modo MySQL) durante os testes de integração e verificação de rollback em falhas.
- **Resultado aprovado:** Schema sobe sem erros de DDL e operações de carteira/transação mantêm consistência atômica.
- **Falhas que bloqueiam:** Duplicidade de lançamentos financeiros (ex: `adicionarPorValorBRL` duplicando movimento) ou corrupção parcial por falta de transação.

### 8. Desempenho e Consumo de Recursos
- **O que verificar:** Timeouts configurados em todas as chamadas HTTP externas e liberação de memória na geração de gráficos Matplotlib.
- **Como verificar:** Verificação de `HttpConfig.java` (`connectTimeout` e `readTimeout`) e uso de `Figure` orientada a objetos no Python.
- **Resultado aprovado:** Nenhuma chamada HTTP sem timeout; gráficos gerados sem acumular figuras abertas na memória global do `pyplot`.
- **Falhas que bloqueiam:** `new RestTemplate()` sem timeout ou uso de `plt.figure()` sem fechamento/isolamento thread-safe.

### 9. Logs, Observabilidade e Diagnóstico
- **O que verificar:** Uso de `Logger` (SLF4J) para eventos importantes e erros de integração, sem expor credenciais ou dados sensíveis.
- **Como verificar:** Revisão dos logs emitidos durante a suíte de testes e inspeção de código.
- **Resultado aprovado:** Falhas de integração logadas com contexto claro e sem vazamento de tokens.
- **Falhas que bloqueiam:** Logar tokens de API, senhas de banco ou usar `e.printStackTrace()`.

### 10. Documentação
- **O que verificar:** Sincronia entre o código implementado e os documentos em `docs/` e `README.md`.
- **Como verificar:** Verificação de existência e integridade dos arquivos Markdown obrigatórios no `verify-quality-gates.ps1`.
- **Resultado aprovado:** Alterações de arquitetura, endpoints ou variáveis de ambiente refletidas na documentação na mesma entrega.
- **Falhas que bloqueiam:** Alterar contratos ou variáveis de ambiente sem atualizar a documentação correspondente.

### 11. Compatibilidade entre Módulos (Java ↔ Python)
- **O que verificar:** Alinhamento entre os DTOs Java (`IaRouterResponse`, `IaTransacaoResponse`, `Chart*RequestDTO`) e os modelos Pydantic em `sexta-feira-ia`.
- **Como verificar:** Testes de contrato/serialização em ambos os lados.
- **Resultado aprovado:** Tipos, nomes de propriedades JSON e enums (`CategoriaTransacao`, `TipoTransacao`, `ClasseAtivo`) 100% compatíveis.
- **Falhas que bloqueiam:** Divergência de nomes de campos JSON ou adição de categoria/intenção no Python que cause `IllegalArgumentException` no `Enum.valueOf()` do Java.

### 12. Configuração e Implantação
- **O que verificar:** Capacidade de configurar a aplicação inteiramente via variáveis de ambiente com defaults seguros de desenvolvimento e `.env.example` atualizado.
- **Como verificar:** Checagem das propriedades em `application.properties` e `requirements.txt`.
- **Resultado aprovado:** `requirements.txt` contém todas as dependências reais importadas; `application.properties` parametrizado.
- **Falhas que bloqueiam:** Dependência importada no Python ausente do `requirements.txt` ou URL hardcoded ignorando configuração.

---

## Regras para Exceções
Se algum critério não puder ser verificado automaticamente em determinada etapa (por exemplo, um serviço externo de terceiro indisponível ou uma credencial que ainda será rotacionada na Etapa 3), a exceção **deve ser explicitamente declarada e justificada** no relatório da fase, e o estado da fase deve ser classificado no máximo como **Aprovado com ressalvas** (nunca como **Aprovado** pleno se houver pendência relevante).
