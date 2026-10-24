# Definição de Pronto (`DEFINITION_OF_DONE.md`)

Uma tarefa, correção de bug ou fase de evolução no projeto **Sexta-Feira** só está concluída (**Done**) quando **todos** os itens aplicáveis abaixo forem cumpridos e comprovados com evidências reais.

---

## 1. Checklist de Conclusão (Definition of Done)

### Implementação e Arquitetura
- [ ] O código resolve a causa raiz do problema sem introduzir gambiarras ou duplicação desnecessária.
- [ ] As fronteiras arquiteturais entre `sexta-feira-api` (Java) e `sexta-feira-ia` (Python) foram respeitadas ([docs/ARCHITECTURE.md](ARCHITECTURE.md)).
- [ ] Nenhum arquivo fora do escopo da tarefa foi modificado sem justificativa.
- [ ] Nenhum componente de frontend foi criado.

### Qualidade e Padrões de Código
- [ ] O código segue rigorosamente [docs/CODING_STANDARDS.md](CODING_STANDARDS.md) (`BigDecimal`/`Decimal` para valores financeiros, injeção por construtor, sem estado mutável em singletons).
- [ ] Todos os arquivos criados ou editados estão em codificação `UTF-8`.

### Testes Automatizados
- [ ] Novos testes unitários e/ou de integração foram escritos cobrindo os caminhos felizes, casos de borda e cenários de erro da alteração ([docs/TESTING_STRATEGY.md](TESTING_STRATEGY.md)).
- [ ] Se foi uma correção de bug, um teste automatizado reproduziu a falha antes e passou após a correção.
- [ ] A suíte completa do Maven (`mvnw.cmd test`) e a suíte do Python (`unittest`) foram executadas e passaram com **0 falhas**.

### Segurança e Isolamento
- [ ] Nenhum segredo, senha, token ou dado pessoal real foi exposto no código, propriedades, logs ou documentação ([docs/SECURITY.md](SECURITY.md)).
- [ ] Os dados financeiros permanecem isolados por `Usuario` (obrigatório a partir da Etapa 3).
- [ ] Entradas externas são validadas e erros não expõem stack traces.

### Verificação Automatizada dos Quality Gates
- [ ] O script oficial `powershell -ExecutionPolicy Bypass -File .\scripts\verify-quality-gates.ps1` foi executado e seu resultado real foi registrado.

### Documentação
- [ ] Qualquer mudança em endpoints, contratos, variáveis de ambiente ou arquitetura foi atualizada em `README.md` e nos documentos em `docs/`.

---

## 2. Formato Obrigatório do Relatório de Conclusão

Ao encerrar cada fase ou entrega, o agente deve apresentar ao usuário um relatório contendo obrigatoriamente as 11 seções abaixo e a classificação de estado:

1. **Estado da Fase:** `Aprovado` | `Aprovado com ressalvas` | `Bloqueado`
2. **Objetivo da Fase**
3. **Arquivos Analisados**
4. **Arquivos Criados ou Modificados**
5. **Problemas Encontrados** (diferenciando preexistentes de novos)
6. **Decisões Técnicas Tomadas e Justificativas**
7. **Testes Executados** (com comandos exatos e resultados reais)
8. **Verificações de Segurança Realizadas**
9. **Problemas Corrigidos**
10. **Problemas Ainda Pendentes e Riscos Residuais**
11. **Próximos Passos Recomendados**
