---
description: FASE 3 — Persistência de Dados & Integridade de Estado
---

// turbo-all

# 🔗 FASE 3: PERSISTÊNCIA DE DADOS & INTEGRIDADE DE ESTADO (O COFRE DE DADOS)

> **📖 PRÉ-REQUISITO OBRIGATÓRIO — LEIA ANTES DE TUDO:**
> 1. Leia os Knowledge Items `gorvax-project-rules` e `gorvax-architecture` — são a constituição e o mapa do projeto.
> 2. **🇧🇷 IDIOMA OBRIGATÓRIO:** TODA interação, análise, plano, log, comentário e até seus PENSAMENTOS internos devem ser em **PORTUGUÊS BRASILEIRO (PT-BR)**. Código em inglês, TODO o resto em PT-BR. NUNCA mude para inglês no meio da execução. Se perceber que mudou, CORRIJA imediatamente.
>
> **Referências obrigatórias:** `@Codebase` `KI: gorvax-project-rules` `KI: gorvax-architecture`

═══════════════════════════════════════════════════

## STEP A — AUDITORIA DE DADOS PROFUNDA (Documentar no `auditoria-logs.md`)

> **⚠️ REGRA ABSOLUTA: NÃO ESCREVA CÓDIGO NESTE STEP.**
> Você é Principal Data Engineer e Especialista em Sistemas Distribuídos.
> **PRÉ-REQUISITO:** Fases 1-2 validaram arquitetura e qualidade de código. Se encontrar problemas de organização aqui, corrija e documente como **regressão**.
> Em um servidor Minecraft, dados são SAGRADOS. Se o `kingdoms.yml` corromper durante um crash,
> reinos inteiros desaparecem. Se o `playerdata.yml` perder entries, jogadores perdem progresso.
> Audite TODA a cadeia de persistência: load → modify → save → recovery.

### 1. Cadeia de Persistência (O Fluxo Crítico)
- Varre TODOS os Managers que manipulam YAML — algum lê/escreve diretamente sem abstração?
- **O padrão correto é:** `YAML file → Manager.load() → ConcurrentHashMap (cache) → Manager.save() → YAML file`
- Use `grep_search` para buscar `YamlConfiguration`, `FileConfiguration`, `save(`, `load(`
- Mapeie cada Manager: qual arquivo YAML gerencia, quando carrega, quando salva
- Algum Manager carrega dados na thread principal causando lag?

### 2. Async I/O (A Regra de Ouro)
- Todas as operações de `save()` rodam em `Bukkit.getScheduler().runTaskAsynchronously()`?
- **NUNCA** salvar na thread principal (causa TPS drops com arquivos grandes)
- Exceção: `onDisable()` pode ser síncrono (servidor já está parando)
- `load()` pode ser síncrono no startup, mas NÃO em hot-reload
- Operações de `set()` no `FileConfiguration` são thread-safe? `ConcurrentHashMap` protege o cache?

### 3. Integridade e Corrupção
- Se o servidor crashar DURANTE um `save()`, o arquivo pode ficar incompleto/corrompido?
- **Save atômico:** Write to temp file (`.tmp`) → rename to real file — está implementado?
- Existe backup automático dos YAML antes de saves grandes (ex: migration)?
- Se um campo obrigatório estiver ausente no YAML, o Manager trata graciosamente ou NPE?

### 4. Autosave e Recovery
- Existe autosave periódico? (Se B44 não foi implementado, documente como issue)
- Se sim, intervalo configurável? Não causa lag em pico de jogadores?
- Shutdown hook garante save de TODOS os managers?
- Ordem de save respeita dependências (ex: Claims antes de Kingdoms)?

### 5. Migração de Dados
- Se o schema do YAML mudar entre versões, existe migração automática?
- Campos deprecated são removidos limpa e automaticamente?
- Novos campos são adicionados com defaults seguros ao carregar YAML antigo?
- Log informativo quando migração ocorre?

### Entrega do Step A — RELATÓRIO DE INTEGRIDADE DE DADOS:

Documente no `auditoria-logs.md` sob header `FASE 3A — VEREDITO DE PERSISTÊNCIA`:

1. **Veredito:** O quão seguro estão os dados? O que acontece se o servidor crashar AGORA?
2. **Mapa de Persistência:** Tabela mostrando cada Manager → YAML → Thread (sync/async) → Status (✅/⚠️/🔴)
3. **Issues Identificadas:** Lista numerada e priorizada com:
   - Criticidade (🔴 Alta / 🟡 Média / 🟢 Baixa)
   - Manager/arquivo afetado
   - Tipo (sync na main thread, sem fallback, sem atômico, sem autosave)
   - Cenário de falha (o que acontece se crashar neste momento)

> **FORMATO DA LISTA É OBRIGATÓRIO** — O Step B precisa consumir issues estruturadas.

### ⚠️ REGRA ANTI-PREGUIÇA (INVIOLÁVEL)
> Você DEVE ler os arquivos de código relevantes usando `view_file` e `grep_search` a cada ciclo.
> NUNCA diga "confirmado em ciclo anterior" ou "mantido sem alteração" sem ter RE-LIDO o código.
> Qualquer problema encontrado DEVE ser corrigido NESTE ciclo — NUNCA adie como "dívida técnica".

═══════════════════════════════════════════════════

## STEP B — PLANO DE BLINDAGEM DE DADOS (Gerar plano nativo da IDE)

> **REGRA:** Leia INTEGRALMENTE o relatório do Step A antes de criar o plano.
> Cada issue do Step A DEVE ter uma ação correspondente aqui.
> **⚠️ Gere o plano usando o mecanismo NATIVO da IDE (implementation_plan). NÃO documente o plano no `auditoria-logs.md`.**

### Para cada issue do Step A, defina:
- **Issue #N** → Ação proposta (async migration, atomic save, add fallback, fix thread)
- **Manager/Arquivo:** caminho exato
- **Cenário de falha:** o que quebra se não corrigir
- **Solução técnica:** snippet ou pseudocódigo da correção

### Seções obrigatórias no plano nativo:
1. **Fixes de Thread Safety:** Saves na main thread → async; loads sem proteção
2. **Atomic Saves:** Managers que escrevem diretamente no YAML final
3. **Fallbacks e Defaults:** Campos sem tratamento de ausência
4. **Autosave:** Se inexistente, planejar implementação
5. **Checklist de Execução:** Lista ordenada por prioridade — Step C seguirá item a item
6. **Se nenhuma ação for necessária**, documente o porquê detalhadamente e prossiga

> **🛑 PARADA OBRIGATÓRIA DO STEP B:**
> - **NÃO CODIFIQUE NADA.** O Step B é APENAS para gerar o plano.
> - **NÃO EXECUTE o plano.** A execução será feita no Step C, que virá como um comando separado.
> - **NÃO PERGUNTE se pode executar.** Apenas gere o plano e PARE.
> - **NÃO USE `notify_user`.** NÃO gere caixa de aprovação, review, confirmação ou qualquer solicitação de feedback.
> - **NÃO USE `PathsToReview` nem `BlockedOnUser`.** Nenhum mecanismo de aprovação deve ser acionado.
> - Após gerar o plano, ENCERRE SUA RESPOSTA. Não escreva mais nada. Não sugira próximos passos. PARE.

═══════════════════════════════════════════════════

## STEP C — EXECUÇÃO

> **⚠️ VERIFICAÇÃO PRÉ-EXECUÇÃO (OBRIGATÓRIA):**
> O plano do Step B pode já ter sido executado parcial ou totalmente (auto-proceed da IDE).
> **ANTES de executar qualquer item**, faça esta verificação:
>
> 1. **Leia os arquivos do plano** com `view_file` — verifique se as mudanças já foram aplicadas
> 2. **Rode `gradlew build`** — se buildar sem erros, as mudanças podem já estar feitas
> 3. **Classifique cada item do checklist** como:
>    - ✅ **JÁ EXECUTADO** — código já modificado conforme o plano → **NÃO refaça, apenas registre**
>    - 🔧 **PARCIAL** — executado mas incompleto ou com problemas → **complete/corrija**
>    - ⬜ **PENDENTE** — não executado → **execute normalmente**
> 4. **Se TUDO já foi executado**: valide com build, registre no log e encerre
> 5. **Se NADA foi executado**: execute tudo normalmente seguindo o checklist

> **REGRA:** Siga o Checklist de Execução do Step B **item a item, na ordem definida**.
> NÃO improvise, NÃO pule itens, NÃO adicione mudanças que não estão no plano.
> Ordem sugerida: Thread Safety → Atomic Saves → Fallbacks → Autosave (de baixo pra cima).

1. Execute cada item **PENDENTE ou PARCIAL** do plano do Step B
2. Rode `gradlew build` — se falhar, corrija e re-verifique
3. Registre RESUMO BREVE no `auditoria-logs.md` sob header `FASE 3C — EXECUÇÃO`:
   - Lista simples: `✅ [arquivo] — [o que foi feito] — [método: criação/refatoração/correção]`
   - Para itens já executados pelo auto-proceed: `✅ [arquivo] — [já executado pelo auto-proceed] — validado`
   - Build status: ✅ ou ❌ + erro
   - **NÃO duplique o plano completo aqui — apenas o que foi executado e o método usado**

---

**ROTEAMENTO:** Ao concluir este step, **pare e aguarde o próximo prompt**. A orquestração entre fases é gerenciada pela extensão Gorvax.
