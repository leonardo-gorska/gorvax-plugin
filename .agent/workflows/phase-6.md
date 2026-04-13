---
description: FASE 6 — Discovery de Inovação & Gap Analysis (Features Novas)
---

// turbo-all

# 🚀 FASE 6: DISCOVERY DE INOVAÇÃO & GAP ANALYSIS (O DIFERENCIAL COMPETITIVO)

> **📖 PRÉ-REQUISITO OBRIGATÓRIO — LEIA ANTES DE TUDO:**
> 1. Leia os Knowledge Items `gorvax-project-rules`, `gorvax-architecture` e `gorvax-boss-system`.
> 2. **🇧🇷 IDIOMA OBRIGATÓRIO:** PT-BR em tudo exceto código. VIOLAÇÃO = FALHA CRÍTICA.
>
> **Referências obrigatórias:** `@Codebase` + TODOS os Knowledge Items `gorvax-*`

> ⚠️ **STATUS: DESATIVADA**
> Mude para `ATIVADA` quando o Evolution Engine auto-ativar, e de volta para `DESATIVADA` após conclusão.

═══════════════════════════════════════════════════

## STEP A — AUDITORIA DE INOVAÇÃO (Documentar no `auditoria-logs.md`)

> **⚠️ REGRA ABSOLUTA: NÃO ESCREVA CÓDIGO NESTE STEP.**
> Você é CPO (Chief Product Officer) e Game Designer Sênior.
> A fundação técnica está impecável. Chegou a hora de inovar.
> Compare o GorvaxCore com plugins AAA (EssentialsX, Towny, mcMMO, MythicMobs) e servidores
> de referência (Hypixel, Hermitcraft, 2b2t) — encontre o GAP.
> O que falta para o GorvaxMC ser o servidor que jogadores PAGAM para jogar?

### 1. Mapeamento do Teto Atual
- Leia módulos atuais — entenda a maturidade de cada sistema
- **NÃO sugira coisas já implementadas** — prove que leu o código
- Mapeie capacidades por domínio (Kingdoms, Bosses, Market, Claims, Chat, BattlePass, Ranks)

### 2. Benchmarking (Onde o mercado nos vence?)
- O que jogadores hardcore sentiriam falta? Quests narrativas? Dungeons procedurais?
- Lacunas em engajamento de longo prazo (retenção pós-endgame)?
- Lacunas em sistemas sociais (alianças dinâmicas, diplomacia, guerras)?
- Lacunas em personalização (cosmetics, titles, partículas)?
- **Descubra sozinho** — a análise é 100% sua

### 3. Viabilidade Técnica
- Cada feature é construível com a stack atual (Paper API + YAML + Vault)?
- Estimativa de complexidade de cada sugestão
- Impacto em performance com 100+ jogadores

### Entrega do Step A — MASTERPLAN:

Documente no `auditoria-logs.md` sob header `FASE 6A — MASTERPLAN DE INOVAÇÃO`:
1. **Diagnóstico de Maturidade:** Provando que leu o código
2. **Top 5 Killer Features:** Nome, Impact psicológico no jogador, Viabilidade, Complexidade
3. **Ordem de Implementação:** Por impacto × viabilidade

═══════════════════════════════════════════════════

## STEPS B/C — IMPLEMENTAÇÃO ITERATIVA (Feature por Feature)

> Fluxo especial: planeja 1 feature → executa 1 feature → próxima.

### 🔵 STEP B — PLANO DA FEATURE #N (plano nativo da IDE)

> Foque em UMA feature. `implementation_plan` nativo. NÃO documente no log.

Plano deve conter: Escopo, Arquivos novos/modificados, Data flow (Manager→Command→Listener→GUI), Checklist.

> **🛑 PARADA:** NÃO codifique. NÃO execute. NÃO pergunte. PARE.

### 🟢 STEP C — EXECUÇÃO DA FEATURE #N

> Verificação pré-execução → checklist item a item → `gradlew build` → log.
> Volte ao Step B para próxima feature.

### Fluxo Visual:
```
STEP A: Auditoria → Top 5 Features
  ↓
STEP B: Plano Feature #1 → STEP C: Executa → build ✅
  ↓
STEP B: Plano Feature #2 → STEP C: Executa → build ✅
  ↓ (repete até #5)
REVISÃO GERAL → Volta ao evolution-engine.md
```

═══════════════════════════════════════════════════

## REVISÃO GERAL (Após todas as 5 features)

1. Releia cada feature implementada
2. Valide integração entre features
3. `gradlew build` final
4. Documente sob `FASE 6 — REVISÃO GERAL CONCLUÍDA`

---

**ROTEAMENTO:** Ao concluir, **pare e aguarde o próximo prompt**.
