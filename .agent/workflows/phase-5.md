---
description: FASE 5 — Completude Funcional & Polimento (Zero Pontas Soltas)
---

// turbo-all

# ✅ FASE 5: COMPLETUDE FUNCIONAL & POLIMENTO (ZERO PONTAS SOLTAS)

> **📖 PRÉ-REQUISITO OBRIGATÓRIO — LEIA ANTES DE TUDO:**
> 1. Leia os Knowledge Items `gorvax-project-rules` e `gorvax-architecture`.
> 2. **🇧🇷 IDIOMA OBRIGATÓRIO:** PT-BR em tudo exceto código. VIOLAÇÃO = FALHA CRÍTICA.
>
> **Referências obrigatórias:** `@Codebase` `KI: gorvax-project-rules` `KI: gorvax-architecture`

═══════════════════════════════════════════════════

## STEP A — AUDITORIA DE COMPLETUDE PROFUNDA (Documentar no `auditoria-logs.md`)

> **⚠️ REGRA ABSOLUTA: NÃO ESCREVA CÓDIGO NESTE STEP.**
> Você é QA Director e Inspetor de Qualidade C-Level.
> **PRÉ-REQUISITO:** Fases 1-4 validaram tudo. Regressões = documentar.
> Política "Zero Broken Windows": em um servidor de Minecraft com jogadores pagantes,
> um único comando sem feedback ou uma GUI com slot vazio destrói a confiança.
> **NÃO invente funcionalidade nova.** Tarefa: encontrar o que está "pela metade".

### 1. Comandos "Fantasma" e Becos Sem Saída
- Varra TODOS os comandos registrados no `paper-plugin.yml`
- Existem subcomandos sem implementação? (`/gorvax ???` que retorna nada)
- Todos os comandos respondem com mensagem clara (sucesso OU erro)?
- Usage/help text está definido e é informativo?
- Todos os comandos têm permissão documentada?

### 2. GUIs com "Buracos" (Java + Bedrock)
- Varra TODAS as GUIs Java (classes `Listener` com `InventoryClickEvent`)
- Existem slots vazios onde deveria haver botões?
- Todos os cliques em itens fazem algo? (Sem "ghost buttons" que não respondem)
- Bordas com vidro tem proteção contra click/shift-click?
- Paginação funciona nos extremos? (Página 1 com "Anterior", última com "Próximo"?)
- **Bedrock:** SimpleForm/CustomForm cobre TODAS as opções do inventário Java?

### 3. Mensagens e Feedback ao Jogador
- TODA ação do jogador gera feedback imediato? (chat message, action bar, sound)
- Mensagens de erro são claras e em PT-BR? ("§cVocê não tem permissão" vs silêncio)
- `messages.yml` — existem chaves definidas mas nunca usadas? Chaves usadas mas não definidas?
- Cores e formatação consistentes? (§b para info, §c para erro, §a para sucesso)

### 4. Validação de Inputs em Comandos
- `/reino criar <nome>` — valida tamanho, caracteres especiais, nomes duplicados?
- `/mercado vender <preço>` — valida negativos, zero, overflow?
- Valores financeiros arredondados corretamente? (Sem 1.0000000001)
- Coordenadas e dimensões validadas (mundo errado, coordenadas absurdas)?

### 5. Edge Cases de Gameplay
- Jogador morre dentro de claim protegido — items protegidos?
- Jogador sai do servidor com baú de boss aberto — baú é limpo?
- Boss morre sem ninguém por perto — loot drop funciona?
- Jogador tenta entrar em reino inexistente — feedback claro?
- Jogador Bedrock recebe itens customizados com textura (CustomModelData via Geyser)?
- Dois jogadores editam o mesmo reino simultaneamente — race condition?

### Entrega do Step A — LAUDO DE COMPLETUDE:

Documente no `auditoria-logs.md` sob header `FASE 5A — LAUDO DE COMPLETUDE`:
1. **Veredito:** Quantas "janelas quebradas"? O plugin inspira confiança profissional?
2. **Mapa de Completude:** Tabela por módulo (✅ Completo / ⚠️ Parcial / 🔴 Incompleto)
3. **Issues Identificadas:** Lista numerada com Criticidade, Módulo, Tipo, Solução sugerida

> **FORMATO DA LISTA É OBRIGATÓRIO.**

### ⚠️ REGRA ANTI-PREGUIÇA (INVIOLÁVEL)
> Releia arquivos a cada ciclo. NUNCA "confirmado em ciclo anterior". Problema → corrigir AGORA.

═══════════════════════════════════════════════════

## STEP B — PLANO DE POLIMENTO (Gerar plano nativo da IDE)

> Leia o laudo do Step A. Cada issue DEVE ter solução. Use `implementation_plan` nativo.

### Seções obrigatórias:
1. **Comandos Fantasma → Implementações Reais**
2. **GUIs Incompletas → Slots Funcionais**
3. **Mensagens Faltantes → Feedback Completo**
4. **Validações → Guards Robustos**
5. **Checklist de Execução ordenado**
6. Se nenhuma ação necessária, documente porquê

> **🛑 PARADA:** NÃO codifique. NÃO execute. NÃO pergunte. NÃO use notify_user. PARE.

═══════════════════════════════════════════════════

## STEP C — EXECUÇÃO

> Verificação pré-execução: leia arquivos, rode `gradlew build`, classifique itens (✅/🔧/⬜).
> Siga o checklist item a item.

1. Execute itens PENDENTES/PARCIAIS
2. `gradlew build` — falhou? Corrija e re-verifique
3. Registre no `auditoria-logs.md` sob `FASE 5C — EXECUÇÃO`

---

**ROTEAMENTO:** Ao concluir, **pare e aguarde o próximo prompt**.
