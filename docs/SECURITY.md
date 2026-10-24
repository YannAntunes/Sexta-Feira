# Política de Segurança e Proteção de Dados — Sexta-Feira (`SECURITY.md`)

Este documento define os controles obrigatórios de segurança da informação, privacidade e desenvolvimento seguro aplicáveis ao projeto **Sexta-Feira**, alinhados ao OWASP Top 10 e à arquitetura real Java/Spring Boot + Python/FastAPI.

---

## 1. Gestão de Segredos e Credenciais

1. **Proibição de Segredos Hardcoded:**
   - Credenciais de banco de dados (`spring.datasource.password`), tokens de integração (`whatsapp.api.token`, `brapi.token`), chaves de futuros provedores de LLM e números de telefone pessoais (`monitoramento.whatsapp.to`) **nunca** devem ser gravados em texto puro em `application.properties`, código-fonte ou documentação.
2. **Uso de Variáveis de Ambiente:**
   - Toda configuração sensível deve utilizar interpolação de variáveis de ambiente no Spring Boot (ex: `spring.datasource.password=${DB_PASSWORD}`) e `os.getenv()` no Python.
   - O repositório deve manter um arquivo `.env.example` na raiz contendo **apenas** os nomes das variáveis e valores fictícios claramente identificados (ex: `DB_PASSWORD=change_me_local_password`).
   - Arquivos `.env` reais devem estar obrigatoriamente listados no `.gitignore`.
3. **Procedimento para Credenciais Expostas no Histórico Git:**
   - Conforme identificado em [docs/PROJECT_AUDIT.md](PROJECT_AUDIT.md) (`[SEC-01]`), credenciais foram versionadas em commits anteriores.
   - Remover a senha do arquivo atual **não** a remove do histórico público do Git.
   - **Ação Obrigatória:** Todas as senhas e tokens que já tocaram o histórico Git devem ser considerados comprometidos e **rotacionados imediatamente** nos respectivos serviços (MySQL e provedor de WhatsApp). Operações de reescrita de histórico (`git filter-repo`) só devem ser executadas com autorização explícita do proprietário.

---

## 2. Isolamento de Dados por Usuário (Multi-Tenancy e Prevenção de BOLA / OWASP API1)

O risco mais crítico de um assistente financeiro é o vazamento ou manipulação cruzada de dados entre usuários (**Broken Object Level Authorization — BOLA**).

1. **Vínculo Obrigatório no Modelo de Dados:**
   - Todas as entidades financeiras (`Transacao`, `Orcamento`, `AlertaOrcamento`, `AtivoCarteira`, `MovimentoCarteira`, `SnapshotCarteira`) devem possuir relacionamento obrigatório (`@ManyToOne(optional = false, fetch = FetchType.LAZY)`) com a entidade `Usuario`.
   - Todas as *Unique Constraints* de negócio devem incluir `usuario_id` (ex: `(usuario_id, classe, ticker)` em `ativos_carteira` e `(usuario_id, data, classe, ticker)` em `snapshots_carteira`).
2. **Filtragem Obrigatória na Camada de Persistência e Serviço:**
   - Todas as consultas em repositórios (`TransacaoRepository`, `OrcamentoRepository`, `AtivoCarteiraRepository`, etc.) devem exigir o `Usuario` ou `usuarioId` como parâmetro de filtro.
   - Métodos globais como `findAll()` ou `findTopByOrderByIdDesc()` são **proibidos** em fluxos de usuário; utilize `findByUsuario...` ou `findTopByUsuarioOrderByIdDesc(Usuario usuario)`.
3. **Verificação de Propriedade antes de Mutação/Exclusão:**
   - Antes de atualizar ou deletar qualquer registro financeiro, o serviço deve garantir que o registro pertence ao usuário autenticado/identificado na requisição.

---

## 3. Autenticação, Autorização e Segurança de Webhooks

1. **Identidade Nunca Confiada Cegamente ao Cliente:**
   - Nunca confie apenas em um `userId` passado livremente no corpo/query por um cliente não autenticado.
2. **Proteção do Webhook do WhatsApp (`POST /whatsapp/webhook`):**
   - Webhooks expostos sem validação permitem que qualquer atacante envie requisições forjadas (`spoofing`) passando o número de telefone de outra pessoa no campo `from`.
   - O webhook deve validar um token/segredo de verificação via cabeçalho HTTP (ex: `X-Webhook-Token` ou assinatura HMAC configurada via variável de ambiente `WHATSAPP_WEBHOOK_SECRET`), rejeitando com `401 Unauthorized` ou `403 Forbidden` chamadas sem credencial válida.
3. **Proteção dos Endpoints da API REST (`/api/v1/**`):**
   - Endpoints administrativos ou de acesso direto aos dados financeiros devem exigir identificação explícita do contexto de usuário e chave de API / autenticação configurada para o ambiente.
4. **Comunicação Interna Java ↔ Python:**
   - O serviço `sexta-feira-ia` deve escutar apenas em interface local (`127.0.0.1`) ou rede interna privada de containers, nunca exposto diretamente à internet pública.

---

## 4. Validação de Entradas, Prevenção de Injeção e Tratamento de Erros

1. **Prevenção contra SQL Injection:**
   - Utilize sempre queries parametrizadas do Spring Data JPA (`@Query` com `:param` e `@Param`). É terminantemente proibido concatenar strings de entrada do usuário em consultas SQL/JPQL.
2. **Validação e Sanitização de Entradas:**
   - Valide limites numéricos (valores de transação e orçamento devem ser `> 0`), tamanhos máximos de texto (`mensagem` limitada para evitar payloads gigantes / DoS em regex) e formatos de tickers/moedas (`^[A-Z0-9]{2,10}$` antes de interpolar em URLs da Brapi/CoinGecko/Frankfurter para evitar **SSRF / Path Traversal**).
3. **Prevenção de ReDoS (Regular Expression Denial of Service):**
   - No `sexta-feira-ia`, limite o tamanho máximo de `MensagemRequest.mensagem` (ex: máx. 1000 caracteres via Pydantic `Field(max_length=1000)`) antes de aplicar expressões regulares.
4. **Respostas de Erro Seguras (`GlobalExceptionHandler`):**
   - Nunca retorne stack traces Java, nomes de tabelas SQL, caminhos de diretórios do servidor ou URLs contendo tokens de API para o cliente HTTP ou mensagem do WhatsApp.

---

## 5. Logs Seguros e Proteção de Dados Pessoais (LGPD)

1. **Proibição de Dados Sensíveis em Logs:**
   - Nunca registre senhas, tokens (`whatsapp.api.token`, `brapi.token`) ou chaves de API nos logs.
   - Evite logar o conteúdo financeiro completo ou número de telefone integral em ambientes de produção sem mascaramento (ex: prefira logar `usuarioId=42` ou telefone mascarado `***8825`).
2. **Desativação de `show-sql` em Produção:**
   - `spring.jpa.show-sql=true` é permitido apenas em desenvolvimento local; deve ser parametrizável (`${SHOW_SQL:false}`) para não despejar dados financeiros em stdout em produção.

---

## 6. Checklist Obrigatório de Segurança para Alterações de Código

Antes de concluir qualquer alteração, verifique:
- [ ] Nenhum segredo, token, senha ou telefone real foi adicionado ao código ou arquivos de configuração?
- [ ] Todas as queries JPA novas ou modificadas filtram explicitamente pelo `Usuario`?
- [ ] Parâmetros externos interpolados em URLs (tickers, moedas) são validados contra caracteres especiais (`?`, `&`, `/`, `..`)?
- [ ] O tamanho e formato dos DTOs de entrada são validados?
- [ ] Os testes automatizados de segurança/isolamento passaram no script `scripts/verify-quality-gates.ps1`?
