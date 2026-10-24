# Padrões de Código e Engenharia — Sexta-Feira (`CODING_STANDARDS.md`)

Este documento estabelece as convenções obrigatórias de código para os módulos **Java (`sexta-feira-api`)** e **Python (`sexta-feira-ia`)**. As regras abaixo foram desenhadas para evitar erros financeiros, condições de corrida e acoplamento indevido, sem criar burocracia desnecessária.

---

## 1. Regras Gerais (Ambos os Módulos)

1. **Codificação de Arquivos:** Todos os arquivos de código, propriedades, scripts e documentação devem ser salvos estritamente em **UTF-8**.
2. **Proibição de Valores Monetários em Ponto Flutuante Binário:**
   - **Java:** Use sempre `java.math.BigDecimal`. Nunca instancie com `new BigDecimal(double)`; use `new BigDecimal("10.50")`, `BigDecimal.valueOf(double)` ou converta a partir de `String`. Toda divisão (`divide`) **deve** especificar escala e `RoundingMode.HALF_UP`.
   - **Python:** Use `decimal.Decimal` para representação e extração de valores financeiros. Conversão para `float` é permitida exclusivamente na camada final de plotagem do Matplotlib.
3. **Sem Segredos no Código:** Nenhuma chave de API, senha de banco, token ou telefone pessoal pode constar como literal ou valor default real em anotações `@Value` ou arquivos `.properties` / `.py`.
4. **Preservação de Contratos:** Não renomeie campos JSON de DTOs usados na integração entre `sexta-feira-api` e `sexta-feira-ia` sem atualizar simultaneamente o modelo Pydantic correspondente e os testes de contrato.

---

## 2. Padrões para Backend Java (`sexta-feira-api`)

### 2.1. Organização de Pacotes e Camadas
Respeite a estrutura existente sob `br.com.yann.sextafeira`:
- `core`: Componentes transversais (`config`, `controller`, `dto`, `integration`, `service`, `webhook`).
- `identity`: Gestão de usuários e autenticação (`domain`, `repository`, `service`).
- `modes.finance`: Domínio de finanças pessoais e investimentos (`controller`, `domain.model`, `domain.repository`, `dto`, `integration`, `scheduler`, `service`).

### 2.2. Separação de Responsabilidades e Injeção de Dependências
- **Controllers (`@RestController`):** Devem apenas receber e validar requisições (`@Valid`), delegar para um `@Service` e retornar DTOs ou `ResponseEntity`. Controllers **não devem** conter cálculos financeiros nem chamar `JpaRepository` diretamente (nota: o uso direto de `TransacaoRepository` em `TransacaoController` e `AssistenteService` é uma dívida técnica que deve ser gradualmente encapsulada em `TransacaoService`).
- **Injeção via Construtor:** Use exclusivamente injeção por construtor com campos `private final`. **Não** use `@Autowired` em atributos de classe (como o atributo duplicado encontrado em `CarteiraService`).
- **Proibição de Estado Mutável em Singletons:** Classes `@Service`, `@Component` e `@RestController` são singletons no Spring. É **proibido** armazenar dados de requisição ou de usuário em atributos de instância mutáveis (ex: `private RankingContextDTO ultimoRankingContext`).

### 2.3. Banco de Dados, JPA e Transações
- Operações de escrita que alterem mais de uma entidade ou executem `delete` + `save` (ex: `CarteiraService.adicionarQuantidade`, `CarteiraService.adicionarPorValorBRL`, `SnapshotCarteiraService.gerarSnapshotDoDia`) devem ser anotadas com `@Transactional`.
- Sempre filtre consultas de repositório pelo usuário proprietário dos dados (a ser tornado obrigatório na Etapa 3).

### 2.4. Tratamento de Exceções e Logs
- Use SLF4J (`LoggerFactory.getLogger(...)`) em vez de `System.out.println()`.
- Nunca exponha `ex.printStackTrace()` ou detalhes internos de SQL/stack trace no corpo da resposta HTTP; deixe o `GlobalExceptionHandler` formatar a resposta de erro.
- Ao chamar APIs externas (`RestTemplate`), capture `RestClientException` e lance exceção de domínio clara (`IllegalStateException` ou equivalente) com log informativo sem expor tokens na URL logada.

---

## 3. Padrões para Serviço Python (`sexta-feira-ia`)

### 3.1. Tipagem e Modelos Pydantic
- Todo endpoint FastAPI deve declarar modelos de entrada e saída explícitos herdando de `pydantic.BaseModel` (ou `StreamingResponse` documentado para imagens PNG).
- Todas as funções auxiliares de parsing, normalização e inferência devem conter *type hints* explícitos nos parâmetros e no retorno (ex: `def extrair_valor(mensagem: str) -> Decimal | None:`).

### 3.2. Concorrência e Geração de Gráficos (Matplotlib)
- O backend `Agg` (`matplotlib.use("Agg")`) deve permanecer configurado antes de qualquer importação de plotagem.
- **Proibido usar `plt.figure()` / `plt.plot()` / `plt.bar()` em endpoints:** Use exclusivamente a interface orientada a objetos (`fig = Figure(...)`, `ax = fig.subplots()`, `fig.savefig(buf, format="png")`) para garantir isolamento entre threads/requisições simultâneas.

### 3.3. Regras Determinísticas e Expressões Regulares
- A ordem de avaliação das intenções em `detectar_intencao()` é crítica: intenções específicas de consulta (ex: `"quanto gastei"`, `"relatório"`, `"orçamento"`) devem sempre preceder intenções genéricas de mutação (ex: `"gastei"` -> `ADD_TRANSACTION`).
- Toda nova regra ou expressão regular adicionada em `sexta-feira-ia` **exige** no mínimo um caso de teste positivo e um caso de teste negativo (anti-colisão) em `sexta-feira-ia/tests/`.

---

## 4. Convenções de Commits (Quando Autorizados)

Quando o usuário autorizar commits, utilize o padrão **Conventional Commits** em português ou inglês consistente com o histórico:
- `feat(finance): ...` — nova funcionalidade financeira
- `fix(ia): ...` — correção de bug no roteador/parser Python
- `security(auth): ...` — correção de segurança ou isolamento de usuário
- `docs: ...` — criação ou atualização de documentação técnica
- `test: ...` — adição ou correção de testes automatizados
- `refactor: ...` — melhoria estrutural sem alteração de comportamento externo
