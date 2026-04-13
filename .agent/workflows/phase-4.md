---
description: FASE 4 — Performance & Thread Safety (A Malha Fina)
---

// turbo-all

# 🔍 FASE 4: PERFORMANCE & THREAD SAFETY (A MALHA FINA DO TPS)

> **📖 PRÉ-REQUISITO OBRIGATÓRIO — LEIA ANTES DE TUDO:**
> 1. Leia os Knowledge Items `gorvax-project-rules` e `gorvax-architecture`.
> 2. **🇧🇷 IDIOMA OBRIGATÓRIO:** PT-BR em tudo exceto código. VIOLAÇÃO = FALHA CRÍTICA.
>
> **Referências obrigatórias:** `@Codebase` `KI: gorvax-project-rules` `KI: gorvax-architecture`

═══════════════════════════════════════════════════

## STEP A — AUDITORIA DE PERFORMANCE PROFUNDA (Documentar no `auditoria-logs.md`)

> **⚠️ REGRA ABSOLUTA: NÃO ESCREVA CÓDIGO NESTE STEP.**
> Você é Lead Performance Engineer e Especialista em JVM/Minecraft Server Internals.
> **PRÉ-REQUISITO:** Fases 1-3 validaram arquitetura, código e dados. Regressões = documentar.
> Em um servidor com 100+ jogadores, cada tick (50ms) é sagrado.
> Um handler O(n²) em `PlayerMoveEvent` pode matar o TPS de 20 para 5.

### 1. Event Handlers Pesados (Os Assassinos de TPS)
- Varra TODOS os listeners: `PlayerMoveEvent`, `BlockBreakEvent`, `EntityDamageEvent`
- Handlers de alta frequência fazem IO, iteração pesada, cálculos complexos?
- Early-returns para filtrar? Cache por chunks para claim lookup em O(1)?
- Boss AI tick roda a cada tick ou tem intervalo configurável?

### 2. Schedulers e Tasks (Memory Leaks)
- `BukkitRunnable`, `runTaskTimer`, `runTaskAsynchronously` — cancelados no `onDisable()`?
- Maps crescendo sem cleanup? Cooldown maps limpam ao sair?
- BossTask, BorderTask, AutosaveTask — lifecycle gerenciado?

### 3. Thread Safety (A Bomba-Relógio)
- Bukkit API chamada de threads async? (sendMessage, setBlock de async = crash)
- `ConcurrentHashMap` onde necessário? HashMap regular multi-thread?
- Economia atômica? Dois compradores simultâneos do último item?
- Claims: dois jogadores claimando o mesmo chunk simultaneamente?

### 4. Complexidade Algorítmica
- Loops O(n) iterando TODOS kingdoms/claims/players? (Deveria ser O(1) com Map)
- String concatenation com `+=` em loops? (`StringBuilder`!)
- `getEntities()` chamado repetidamente? Inventário iterado múltiplas vezes?

### 5. GC Pressure
- `Location`/`ItemStack`/`ItemMeta` criados em loops de alta frequência?
- BukkitRunnable anônimas capturando Player que pode ficar offline?

### Entrega do Step A — LAUDO DE PERFORMANCE:

Documente no `auditoria-logs.md` sob header `FASE 4A — LAUDO DE PERFORMANCE`:
1. **Ameaças Críticas (🔴):** Assassinos de TPS, memory leaks
2. **Otimizações (🟡):** Cache, early-return, async migration
3. **Concorrência (🟢):** Race conditions, acesso não-thread-safe
4. **Issues Identificadas:** Lista numerada com Criticidade, Arquivo/linha, Tipo, Impacto

> **FORMATO DA LISTA É OBRIGATÓRIO.**

### ⚠️ REGRA ANTI-PREGUIÇA (INVIOLÁVEL)
> Releia arquivos a cada ciclo. NUNCA "confirmado em ciclo anterior". Problema → corrigir AGORA.

═══════════════════════════════════════════════════

## STEP B — PLANO DE OTIMIZAÇÃO (Gerar plano nativo da IDE)

> Leia o laudo do Step A. Cada issue DEVE ter solução. Use `implementation_plan` nativo.
> NÃO documente o plano no `auditoria-logs.md`.

### Seções obrigatórias:
1. **Correções Críticas (🔴):** TPS killers, memory leaks
2. **Otimizações (🟡):** Cache, early-return, async
3. **Thread Safety (🟢):** Guards, ConcurrentHashMap
4. **Checklist de Execução ordenado**
5. Se nenhuma ação necessária, documente porquê

> **🛑 PARADA:** NÃO codifique. NÃO execute. NÃO pergunte. NÃO use notify_user. PARE.

═══════════════════════════════════════════════════

## STEP C — EXECUÇÃO

> Verificação pré-execução: leia arquivos, rode `gradlew build`, classifique itens (✅/🔧/⬜).
> Siga o checklist item a item. Ordem: 🔴 Críticos → 🟡 Otimizações → 🟢 Thread Safety.

1. Execute itens PENDENTES/PARCIAIS
2. `gradlew build` — falhou? Corrija e re-verifique
3. Registre no `auditoria-logs.md` sob `FASE 4C — EXECUÇÃO`

---

**ROTEAMENTO:** Ao concluir, **pare e aguarde o próximo prompt**.
