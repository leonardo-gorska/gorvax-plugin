---
description: CONTEXT CLEANER — Protocolo de Reset Cognitivo Antes do Ciclo de Confirmação
---

// turbo-all

# 🧹 CONTEXT CLEANER — RESET COGNITIVO

> **📖 PRÉ-REQUISITO OBRIGATÓRIO — LEIA ANTES DE TUDO:**
> 1. Leia os Knowledge Items `gorvax-project-rules` e `gorvax-architecture`.
> 2. **🇧🇷 IDIOMA OBRIGATÓRIO:** PT-BR em tudo exceto código.

> **Quando usar:** Executar SEMPRE que um ciclo terminar com **0 edits**, ANTES do Ciclo de Confirmação.

**PROPÓSITO:** Após múltiplos ciclos, o agente acumula "memória viciada" — conclusões anteriores que mascaram problemas reais. Este protocolo força um reset cognitivo total.

═══════════════════════════════════════════════════

## 🧠 POR QUE ISSO EXISTE

Em ciclos subsequentes, é tentador escrever "confirmado em ciclo anterior" sem re-ler código. Isso gera auditorias progressivamente mais superficiais. O Context Cleaner **proíbe** esse comportamento.

**Analogia:** Inspetor de aviação que verifica TODOS os parafusos antes de cada voo.

═══════════════════════════════════════════════════

## 📋 CHECKLIST DE LIMPEZA DE CONTEXTO

### STEP 1 — Re-leitura Integral de Documentos Fundacionais

Leia integralmente com `view_file`:

1. **KI `gorvax-project-rules`** — Regras obrigatórias do projeto
2. **KI `gorvax-architecture`** — Arquitetura de pacotes e fluxo de dados
3. **`GorvaxCore.java`** — Classe principal, onEnable/onDisable, registro de managers
4. **`config.yml`** — Configurações globais
5. **`messages.yml`** — Centralização de mensagens

**Documente no `auditoria-logs.md`:**
```markdown
### 🧹 CONTEXT CLEANER — LEITURA FUNDACIONAL
- [x] gorvax-project-rules (N linhas lidas)
- [x] gorvax-architecture (N linhas lidas)
- [x] GorvaxCore.java (N linhas lidas)
- [x] config.yml (N linhas lidas)
- [x] messages.yml (N linhas lidas)
```

### STEP 2 — Descarte Mental Obrigatório

1. **NÃO CONFIE** em conclusões de ciclos anteriores — trate o código como NOVO
2. **NÃO CITE** auditorias passadas como justificativa para "0 problemas"
3. **RELEIA** cada arquivo que auditar — `view_file` ou `grep_search` obrigatórios
4. **QUESTIONE** — "Se eu lesse este arquivo pela primeira vez, o que me incomodaria?"

### STEP 3 — Requisitos Mínimos de Profundidade

| Fase | Requisito Mínimo |
|---|---|
| FASE 1 (Arquitetura) | Ler `GorvaxCore.java` + 3 Managers + `paper-plugin.yml` |
| FASE 2 (Qualidade) | Ler 3 Commands + 3 Managers + grep `catch(Exception` |
| FASE 3 (Dados) | Grep `YamlConfiguration`/`save(`/`load(` + ler 2 Managers + verificar async |
| FASE 4 (Performance) | Grep `BukkitRunnable`/`runTaskTimer` + verificar cleanup + ler todos listeners |
| FASE 5 (Completude) | Ler 3 GUIs + 3 Commands + verificar feedback/validação |

**Qualquer fase que não cumprir o mínimo é automaticamente INVÁLIDA.**

### STEP 4 — Registrar no Log

```markdown
### 🧹 CONTEXT CLEANER CONCLUÍDO
**Documentos relidos:** 5/5
**Regras de descarte aplicadas:** Sim
**Próximo ciclo:** Ciclo de Confirmação #[N]
**Modo:** Auditoria total fresca — nenhuma conclusão anterior será reaproveitada
```

═══════════════════════════════════════════════════

## ⚠️ REGRAS INVIOLÁVEIS

1. Cada `view_file` e `grep_search` do Ciclo de Confirmação deve ler direto do filesystem — nunca cache
2. Se encontrar QUALQUER problema, corrija E reinicie contagem
3. Ciclo de Confirmação é REAL — backup + 5 fases completas + build check

═══════════════════════════════════════════════════

**ROTEAMENTO:** Ao concluir, **pare e aguarde o próximo prompt**.
