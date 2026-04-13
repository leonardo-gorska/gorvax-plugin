---
description: FASE 1 — Integridade Arquitetural & Coesão de Módulos
---

// turbo-all

# 🏗️ FASE 1: INTEGRIDADE ARQUITETURAL & COESÃO DE MÓDULOS (O RAIO-X DO ESQUELETO)

> **📖 PRÉ-REQUISITO OBRIGATÓRIO — LEIA ANTES DE TUDO:**
> 1. Leia os Knowledge Items `gorvax-project-rules` e `gorvax-architecture` — são a constituição e o mapa do projeto.
> 2. **🇧🇷 IDIOMA OBRIGATÓRIO:** TODA interação, análise, plano, log, comentário e até seus PENSAMENTOS internos devem ser em **PORTUGUÊS BRASILEIRO (PT-BR)**. Código em inglês, TODO o resto em PT-BR. NUNCA mude para inglês no meio da execução. Se perceber que mudou, CORRIJA imediatamente.
>
> **Referências obrigatórias:** `@Codebase` `KI: gorvax-project-rules` `KI: gorvax-architecture`

═══════════════════════════════════════════════════

## STEP A — AUDITORIA PROFUNDA (Documentar no `auditoria-logs.md`)

> **⚠️ REGRA ABSOLUTA: NÃO ESCREVA CÓDIGO NESTE STEP.**
> Você é Arquiteto de Software Sênior e Tech Lead Elite.
> Estamos construindo um plugin enterprise-grade para centenas de jogadores simultâneos.
> Plugins profissionais como EssentialsX, LuckPerms e WorldGuard dominam a arte de equilibrar
> modularidade com performance. Audite como nossa arquitetura se compara.

### 1. Mapeamento de Pacotes e Módulos
- Varra TODA a estrutura de pacotes (`br.com.gorvax.core.*`)
- Use `view_file` e `grep_search` obrigatoriamente — **releia tudo mesmo em ciclos posteriores**
- Liste: managers, commands, listeners, GUIs, utils
- Mapeie cada classe e seu papel no ecossistema (Boss, Kingdom, Market, Claims, Chat, etc.)
- Identifique classes "orfãs" sem função clara ou duplicadas

### 2. Ciclo de Vida dos Managers (O Coração do Plugin)
- Cada Manager segue o padrão `load → runtime → save`?
- `onEnable()` carrega todos os dados do YAML antes de registrar listeners?
- `onDisable()` salva todos os dados (incluindo shutdown hooks para crash safety)?
- Existe risco de `NullPointerException` se um Manager for acessado antes de `load()`?
- A ordem de inicialização respeita dependências entre Managers (ex: `ClaimManager` depende de `KingdomManager`)?

### 3. Contratos de Config e YAML
- `config.yml` tem todas as chaves documentadas e com valores default sensatos?
- `messages.yml` centraliza TODAS as mensagens? Ou existem strings hardcoded no Java?
- Todos os arquivos YAML (`kingdoms.yml`, `claims.yml`, `playerdata.yml`, `market_global.yml`, etc.) são carregados com fallbacks seguros?
- Existe validação de input ao ler configs (números negativos, strings vazias, chaves ausentes)?

### 4. Organização de Commands e Listeners
- Cada comando segue a cadeia: `Command → Manager → Response`?
- Listeners estão isolados por módulo ou existe um "God Listener" monolítico?
- Permissões estão declaradas no `paper-plugin.yml` e verificadas no código?
- Existe duplicação de lógica entre Commands e Listeners?
- Tab completion está implementado corretamente com `TabCompleter`?

### 5. Compatibilidade Bedrock (Geyser/Floodgate)
- Formulários Bedrock (SimpleForm, CustomForm) cobrem todas as GUIs Java?
- Fallback existe quando `Floodgate.isFloodgatePlayer()` detecta Bedrock?
- AnvilGUI tem fallback para chat input no Bedrock?
- Custom items com `CustomModelData` funcionam via Geyser (campo `cmd` nos configs)?

### Entrega do Step A — RELATÓRIO EXECUTIVO:

Documente no `auditoria-logs.md` sob header `FASE 1A — RAIO-X ARQUITETURAL`:

1. **Raio-X Atual:** Resumo de como a arquitetura está distribuída hoje + maior falha sob ótica enterprise
2. **Mapa de Módulos:** Tabela com todos os módulos e suas classes, organizada por domínio
3. **Diagnóstico de Lifecycle:** Pontos fortes e fracos do ciclo load→runtime→save
4. **Issues Identificadas:** Lista numerada e priorizada de cada problema encontrado com:
   - Criticidade (🔴 Alta / 🟡 Média / 🟢 Baixa)
   - Arquivo(s) afetado(s)
   - Descrição clara do problema

> **FORMATO DA LISTA É OBRIGATÓRIO** — O Step B precisa consumir uma lista estruturada, não texto livre.

### ⚠️ REGRA ANTI-PREGUIÇA (INVIOLÁVEL)
> Você DEVE ler os arquivos de código relevantes usando `view_file` e `grep_search` a cada ciclo.
> NUNCA diga "confirmado em ciclo anterior" ou "mantido sem alteração" sem ter RE-LIDO o código.
> Se auditou `GorvaxCore.java`, `ClaimManager.java` e `KingdomManager.java` no Ciclo #1, releia-os no Ciclo #2.
> Qualquer problema encontrado DEVE ser corrigido NESTE ciclo — NUNCA adie como "dívida técnica".

═══════════════════════════════════════════════════

## STEP B — PLANO DE IMPLEMENTAÇÃO (Gerar plano nativo da IDE)

> **REGRA:** Leia INTEGRALMENTE o relatório do Step A antes de criar o plano.
> Cada issue listada no Step A DEVE ter uma ação correspondente aqui.
> **⚠️ Gere o plano usando o mecanismo NATIVO da IDE (implementation_plan). NÃO documente o plano no `auditoria-logs.md`.**

### Para cada issue do Step A, defina:
- **Issue #N** → Ação proposta (Refatoração / Criação / Correção / Remoção)
- **Arquivos envolvidos:** lista exata de caminhos
- **O que fazer:** descrição técnica precisa
- **Risco:** impacto no build, em outros módulos, em dados persistidos

### Seções obrigatórias no plano nativo:
1. **Correções de Lifecycle:** Managers com load/save incompleto ou ordem errada
2. **Correções de Config/YAML:** Chaves faltantes, defaults ausentes, validação inexistente
3. **Refatorações de Organização:** Classes mal posicionadas, duplicações, "God Classes"
4. **Compatibilidade Bedrock:** Gaps nos formulários Bedrock, fallbacks faltantes
5. **Checklist de Execução:** Lista ordenada por prioridade de cada ação — Step C seguirá item a item
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

1. Execute cada item **PENDENTE ou PARCIAL** do plano do Step B
2. Rode `gradlew build` — se falhar, corrija e re-verifique
3. Registre RESUMO BREVE no `auditoria-logs.md` sob header `FASE 1C — EXECUÇÃO`:
   - Lista simples: `✅ [arquivo] — [o que foi feito] — [método: criação/refatoração/correção]`
   - Para itens já executados pelo auto-proceed: `✅ [arquivo] — [já executado pelo auto-proceed] — validado`
   - Build status: ✅ ou ❌ + erro
   - **NÃO duplique o plano completo aqui — apenas o que foi executado e o método usado**

---

**ROTEAMENTO:** Ao concluir este step, **pare e aguarde o próximo prompt**. A orquestração entre fases é gerenciada pela extensão Gorvax.
