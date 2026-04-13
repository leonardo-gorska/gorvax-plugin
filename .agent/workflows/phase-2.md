---
description: FASE 2 — Qualidade de Código & Padrões de Engenharia
---

// turbo-all

# 🎨 FASE 2: QUALIDADE DE CÓDIGO & PADRÕES DE ENGENHARIA (O BANHO DE LOJA)

> **📖 PRÉ-REQUISITO OBRIGATÓRIO — LEIA ANTES DE TUDO:**
> 1. Leia os Knowledge Items `gorvax-project-rules` e `gorvax-architecture` — são a constituição e o mapa do projeto.
> 2. **🇧🇷 IDIOMA OBRIGATÓRIO:** TODA interação, análise, plano, log, comentário e até seus PENSAMENTOS internos devem ser em **PORTUGUÊS BRASILEIRO (PT-BR)**. Código em inglês, TODO o resto em PT-BR. NUNCA mude para inglês no meio da execução. Se perceber que mudou, CORRIJA imediatamente.
>
> **Referências obrigatórias:** `@Codebase` `KI: gorvax-project-rules` `KI: gorvax-architecture`

═══════════════════════════════════════════════════

## STEP A — AUDITORIA DE CÓDIGO PROFUNDA (Documentar no `auditoria-logs.md`)

> **⚠️ REGRA ABSOLUTA: NÃO ESCREVA CÓDIGO NESTE STEP.**
> Você é Principal Java Engineer e Code Quality Evangelist.
> **PRÉ-REQUISITO:** A Fase 1 validou a Arquitetura. Se encontrar problemas de lifecycle ou módulos aqui, corrija e documente como **regressão da Fase 1**.
> Agora olhamos para a **qualidade interna** do código. Pegue as classes e avalie o quão distantes estão
> do padrão de plugins AAA (EssentialsX, LuckPerms, WorldGuard). O código deve gritar:
> "Manutenível", "Robusto" e "Performático".

### 1. Padrões de Nomenclatura e Convenções Java
- Classes, métodos e variáveis seguem camelCase/PascalCase consistente?
- Constantes estão em `UPPER_SNAKE_CASE` e são `static final`?
- Nomes comunicam intenção? (`handlePlayerClick` vs `click`, `isKingdomOwner` vs `check`)?
- Enums estão sendo usados onde cabem (tipos de proteção, tipos de market, canais de chat)?
- Magic numbers soltos no código? Devem ser constantes nomeadas.

### 2. Tratamento de Erros e Robustez
- try/catch genéricos (`catch(Exception e)`) em vez de específicos?
- Exceções sendo engolidas silenciosamente (`catch(e) {}`) sem log?
- Operações YAML com fallback seguro (`.getOrDefault()`, `Optional`)?
- Null checks adequados antes de acessar dados de jogadores offline?
- `ConcurrentModificationException` protegido em iterações de collections compartilhadas?

### 3. Princípios SOLID e DRY
- **Single Responsibility:** Existem "God Classes" fazendo tudo (>500 linhas)?
- **Open/Closed:** Novos bosses/itens/ranks podem ser adicionados sem alterar código existente?
- **DRY:** Código duplicado entre managers, commands ou GUIs? (ex: lógica de permissão repetida)
- Utility methods extraídos para classes `*Utils`?
- Padrões repetitivos de GUI (criação de itens, bordas, paginação) centralizados?

### 4. Segurança de Dados e Validações
- Inputs de comandos validados (números, nomes de reino, coordenadas)?
- Valores financeiros (economia Vault) protegidos contra overflow/underflow?
- Saldos verificados ANTES de transações (double-check anti-duplication)?
- Claims protegidos contra race conditions (dois jogadores claimando o mesmo chunk)?
- Operações de arquivo YAML protegidas contra corrupção (save atômico)?

### 5. Documentação e Comentários
- JavaDoc nos métodos públicos de Managers?
- Comentários em PT-BR explicando lógica complexa (conforme regras do projeto)?
- TODO/FIXME abandonados no código?
- Métodos públicos que deveriam ser `private` ou `package-private`?

### Entrega do Step A — RELATÓRIO DE QUALIDADE:

Documente no `auditoria-logs.md` sob header `FASE 2A — VEREDITO DE QUALIDADE`:

1. **Veredito:** O quão distante estamos de "código enterprise"? O que grita "código amador"?
2. **Mapa de Qualidade:** Tabela das principais classes com nota de 1-10 em qualidade
3. **Issues Identificadas:** Lista numerada e priorizada com:
   - Criticidade (🔴 Alta / 🟡 Média / 🟢 Baixa)
   - Classe/arquivo afetado
   - O que está errado (má prática, violação SOLID, segurança)
   - Como deveria ser (referência: EssentialsX/LuckPerms/WorldGuard)

> **FORMATO DA LISTA É OBRIGATÓRIO** — O Step B precisa consumir issues estruturadas.

### ⚠️ REGRA ANTI-PREGUIÇA (INVIOLÁVEL)
> Você DEVE ler os arquivos de código relevantes usando `view_file` e `grep_search` a cada ciclo.
> NUNCA diga "confirmado em ciclo anterior" ou "mantido sem alteração" sem ter RE-LIDO o código.
> Qualquer problema encontrado DEVE ser corrigido NESTE ciclo — NUNCA adie como "dívida técnica".

═══════════════════════════════════════════════════

## STEP B — PLANO DE CIRURGIA DE CÓDIGO (Gerar plano nativo da IDE)

> **REGRA:** Leia INTEGRALMENTE o relatório do Step A antes de criar o plano.
> Cada issue do Step A DEVE ter uma ação correspondente aqui.
> **⚠️ Gere o plano usando o mecanismo NATIVO da IDE (implementation_plan). NÃO documente o plano no `auditoria-logs.md`.**

### Para cada issue do Step A, defina:
- **Issue #N** → Cirurgia proposta
- **Classe/Arquivo:** caminho exato
- **Antes:** o que está errado (snippet ou descrição)
- **Depois:** como ficará (snippet corrigido)
- **Padrão aplicado:** qual princípio (DRY, SRP, validação, etc.)

### Seções obrigatórias no plano nativo:
1. **Top 5 Cirurgias Urgentes:** As 5 classes mais críticas, ordenadas por impacto
2. **Refatorações DRY:** Códigos duplicados a centralizar (ComboAction propostas)
3. **Validação e Segurança:** Inputs sem validação, operações sem null-check
4. **Naming e Convenção:** Correções de nomenclatura e visibilidade
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

1. Execute cada item **PENDENTE ou PARCIAL** do plano do Step B
2. Rode `gradlew build` — se falhar, corrija e re-verifique
3. Registre RESUMO BREVE no `auditoria-logs.md` sob header `FASE 2C — EXECUÇÃO`:
   - Lista simples: `✅ [arquivo] — [o que foi feito] — [método: criação/refatoração/correção]`
   - Para itens já executados pelo auto-proceed: `✅ [arquivo] — [já executado pelo auto-proceed] — validado`
   - Build status: ✅ ou ❌ + erro
   - **NÃO duplique o plano completo aqui — apenas o que foi executado e o método usado**

---

**ROTEAMENTO:** Ao concluir este step, **pare e aguarde o próximo prompt**. A orquestração entre fases é gerenciada pela extensão Gorvax.
