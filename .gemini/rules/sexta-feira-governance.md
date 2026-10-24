# Regras Automáticas de Governança — Projeto Sexta-Feira

Sempre que atuar neste repositório, cumpra obrigatoriamente:

1. **Consulte `AGENTS.md` e `docs/QUALITY_GATES.md`** antes de planejar ou modificar qualquer arquivo de código.
2. **Fronteira Arquitetural:**
   - `sexta-feira-api` (Java 17 / Spring Boot) detém o banco de dados, segurança, isolamento por `Usuario` e regras financeiras.
   - `sexta-feira-ia` (Python / FastAPI) é stateless e nunca acessa o banco de dados diretamente.
   - **Proibido criar frontend** (HTML/CSS/JS/React/Vue/Angular).
3. **Segurança e Precisão Financeira:**
   - Nunca grave senhas, tokens ou telefones reais em código ou arquivos `.properties` versionados.
   - Use sempre `BigDecimal` (Java) e `Decimal` (Python) para valores monetários.
   - Toda consulta ou mutação financeira deve respeitar o isolamento por `Usuario` (`docs/SECURITY.md`).
4. **Verificação Obrigatória (Quality Gates):**
   - Nenhuma alteração está concluída sem executar `powershell -ExecutionPolicy Bypass -File .\scripts\verify-quality-gates.ps1` e comprovar que os testes Java (`mvnw.cmd test`) e Python (`unittest`) passaram.
