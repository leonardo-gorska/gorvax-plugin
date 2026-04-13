# 🐉 ROADMAP DE EVOLUÇÃO — GorvaxCore

> **Instruções para a IA**: Leia este arquivo completamente. Encontre o próximo batch com status `[ ]` (pendente).
> Execute TODAS as correções/implementações desse batch. Ao terminar, marque-o como `[x]` e atualize a seção "Progresso".
> **A interação DEVE ser 100% em Português (Brasil).**
> **Leia o Knowledge Item `gorvax-project-rules` antes de qualquer alteração.**

---

## 📘 Regra Obrigatória: Atualização do Manual

> [!IMPORTANT]
> **A cada batch concluído**, a IA **DEVE** atualizar o arquivo `MANUAL.md` na raiz do projeto.

### O que fazer:

1. **Se funcionalidades novas foram criadas**: adicionar uma nova seção (ou subseção) no `MANUAL.md` documentando:
   - O que a funcionalidade faz
   - Comandos disponíveis e como usá-los
   - Permissões necessárias
   - Exemplos práticos de uso
   - Qualquer configuração relevante

2. **Se funcionalidades existentes foram alteradas**: atualizar a seção correspondente no `MANUAL.md` refletindo as mudanças.

3. **Se funcionalidades foram removidas**: remover ou marcar como obsoleta a seção correspondente.

4. **Atualizar o "Histórico de Atualizações do Manual"** no final do arquivo com a data, batch e resumo das alterações.

### Formato:

- O manual é **100% em Português (Brasil)**
- Organizado por **seções temáticas** (Reinos, Mercado, Bosses, Chat, etc.)
- Cada seção deve conter: descrição, comandos, exemplos, permissões
- Usar tabelas e formatação markdown para clareza

---

## 🔌 Plugins instalados no servidor (atualizado 2026-03-10)

> **Esses plugins JÁ existem no servidor e NÃO devem ser reimplementados.**

| Plugin | Categoria | O que cobre |
|--------|-----------|-------------|
| Essentials + EssentialsSpawn | Utilidades | /home, /tpa, /warp, /spawn, /nick, etc. |
| BetterRTP | Teleporte | /rtp, /wild |
| Jobs | Profissões | Sistema de jobs com XP e recompensas |
| TAB | TABList | Formatação da tab list |
| LuckPerms | Permissões | Grupos, ranks, permissões |
| nLogin | Auth | Sistema de login/registro |
| SkinsRestorer | Cosmético | Gerenciamento de skins |
| Floodgate + Geyser | Bedrock | Compatibilidade Java/Bedrock |
| WorldGuard + FAWE | Proteção | Regiões e edição de mundo |
| Vault | Economia | API de economia |
| PlaceholderAPI | Placeholders | Placeholders globais |
| ProtocolLib | Library | Manipulação de pacotes |
| LPC | Chat | Formatação de chat LuckPerms |
| Grim | Anticheat | Anti-cheat de movimentação/combate |
| CoreProtect | Segurança | Logging de eventos e rollback |
| BlueMap | Mapa web | Mapa 3D interativo (integrado via BlueMapHook) |
| spark | Debug | Profiling de performance (temporário) |

> [!NOTE]
> **Removidos em 2026-03-10**: Multiverse-Core (causava reset de gamemode), DeluxeMenus (redundante), BedrockFormShop (não configurado), BedrockPlayerManager (todos módulos desativados), CMILib (Foi restaurado por causa do jobs)

---

## 📊 Progresso Geral

| Batch | Descrição | Status |
|-------|-----------|--------|
| | **— v1.0.0: Lançamento —** | ✅ Pronto |
| B36–B41 | Batches anteriores (Hub TP, Menu, Lores, Guia, Settings) | ✅ Pronto |
| | **— Pré-Lançamento: Auditoria Final —** | |
| B42 | Corrigir Banner de Versão no Startup | ✅ Pronto |
| B43 | Migrar Strings Hardcoded para `messages.yml` | ✅ Pronto |
| B44 | Autosave Periódico Global (Crash Safety) | ✅ Pronto |
| B45 | Registrar bStats Plugin ID (Manual) | ✅ Pronto |
| B46 | Corrigir Versão, Import Duplicado e `printStackTrace()` | ✅ Pronto |
| B47 | Limpar Catch Blocks Vazios e Código Deprecated | ✅ Pronto |
| | **— Pré-Lançamento: Deploy & Configuração —** | |
| B48 | Setup In-Game (Spawn, WorldGuard, Coordenadas) | ⬜ Pendente |
| B49 | Permissões LuckPerms (Grupos e Nodes) | ⬜ Pendente |
| B50 | Loja do Servidor (Tebex + Integração) | ✅ Pronto |
| B51 | Testes End-to-End (Validação Completa) | ⬜ Pendente |
| B52 | Segurança Operacional (Backups e Ops) | ⬜ Pendente |
| | **— Pós-MVP: Features Extras —** | |
| B53 | Sistema de Top Doadores (Tags Automáticas) | ⬜ Pendente |


**Versão atual**: 2.2.0
**Último batch executado**: B47
**Próximo batch a executar**: B48

---

## B42 — Corrigir Banner de Versão no Startup `[x]`

**Escopo**: O banner de startup em `GorvaxCore.java` linha 204 exibe `"GORVAX PLUGIN v1.0.0"` hardcoded enquanto a versão real é lida do `getPluginMeta().getVersion()` na linha seguinte. Corrigir para usar a versão dinâmica em ambos os locais, evitando drift.

**Arquivos afetados**: `GorvaxCore.java`
**Risco**: Baixo

### Implementações:

#### B42.1 — Usar versão dinâmica no banner
- Substituir o literal `"v1.0.0"` no banner por `getPluginMeta().getVersion()`
- Garantir que o banner e a mensagem seguinte usem a mesma fonte de versão

---

## B43 — Migrar Strings Hardcoded para `messages.yml` `[x]`

**Escopo**: ~100+ mensagens estão hardcoded com `sendMessage("§...")` direto no código Java. Migrar todas para chaves no `messages.yml`, permitindo customização sem recompilação. Executar em sub-batches por arquivo.

**Arquivos afetados**: `VipCommand.java`, `StructureCommand.java`, `RankCommand.java`, `KitCommand.java`, `CosmeticCommand.java`, `ReputationCommand.java`, `SeasonalEventCommand.java`, `GorvaxCommand.java`, `messages.yml`
**Risco**: Baixo (apenas texto, lógica inalterada)

### Implementações:

#### B43.1 — `VipCommand.java` (~24 strings)
- Migrar todas as mensagens de `/vip info` e `/vip status` para `messages.yml`
- Chaves: `vip.info_header`, `vip.info_tier`, `vip.info_blocks`, `vip.info_homes`, `vip.info_discount`, `vip.info_footer`, `vip.status_header`, `vip.status_none`, `vip.status_rank`, `vip.status_benefits`, etc.

#### B43.2 — `StructureCommand.java` (~18 strings)
- Migrar todas as mensagens de `/estrutura` (criar, deletar, listar, tp)
- Chaves: `structure.player_only`, `structure.usage_create`, `structure.created`, `structure.deleted`, `structure.list_header`, `structure.list_entry`, `structure.not_found`, etc.

#### B43.3 — `RankCommand.java` + `KitCommand.java` (~12 strings)
- Migrar mensagens de `/rank` e `/kit`
- Chaves: `rank.player_only`, `rank.disabled`, `rank.current`, `rank.next`, `rank.max_rank`, `rank.no_permission`, `rank.not_ready`, `kit.player_only`, `kit.disabled`

#### B43.4 — `CosmeticCommand.java` (~8 strings)
- Migrar mensagens de `/cosmetics`
- Chaves: `cosmetics.not_initialized`, `cosmetics.give_usage`, etc.

#### B43.5 — `ReputationCommand.java` + `SeasonalEventCommand.java` (~15 strings)
- Migrar mensagens de `/karma` e `/evento`
- Chaves: `karma.disabled`, `karma.data_unavailable`, `karma.top_heroes`, `karma.top_villains`, `events.not_initialized`, `events.none_configured`, etc.

#### B43.6 — `GorvaxCommand.java` (mensagens admin, ~15 strings)
- Migrar mensagens de `/gorvax migrate`, `/gorvax remigrate`, auditoria
- Chaves: `admin.migrate_usage`, `admin.migrate_invalid`, `admin.migrate_start`, `admin.migrate_success`, `admin.migrate_error`, `admin.audit_disabled`, etc.

---

## B44 — Autosave Periódico Global (Crash Safety) `[x]`

**Escopo**: Se o servidor sofrer crash ou kill -9, todos os dados desde o último save manual se perdem. Adicionar um autosave global periódico (padrão: 5 minutos) que salva todos os managers com persistência YAML. Configurável via `config.yml`.

**Arquivos afetados**: `GorvaxCore.java`, `config.yml`
**Risco**: Baixo

### Implementações:

#### B44.1 — Task periódica de autosave
- Criar uma `BukkitRunnable` que executa a cada `autosave.interval` ticks (padrão: 6000L = 5min)
- Na task, chamar `saveSync()` de todos os managers que possuem dados YAML:
  - `claimManager`, `playerDataManager`, `kingdomManager`, `marketManager`, `auditManager`, `auctionManager`, `priceHistoryManager`, `mailManager`, `voteManager`, `bountyManager`, `nationManager`, `structureManager`
  - Salvar ASYNC (`runTaskAsynchronously`) para evitar lag
- Log no console: `[GorvaxCore] Autosave concluído (Xms)`

#### B44.2 — Config
```yaml
autosave:
  enabled: true
  interval_minutes: 5
```

#### B44.3 — Mensagem em `messages.yml`
- `admin.autosave_complete: "§b[Gorvax] §aAutosave concluído em {0}ms."`
- Mensagem apenas no console (não spammar jogadores)

---

## B45 — Registrar bStats Plugin ID `[x]`

**Escopo**: O pluginId do bStats está como placeholder `99999` (linha 840 de `GorvaxCore.java`). Precisa ser substituído pelo ID real após registro em [bstats.org](https://bstats.org/).

**Arquivos afetados**: `GorvaxCore.java`
**Risco**: Baixo

> [!IMPORTANT]
> **Ação manual necessária**: O usuário precisa acessar https://bstats.org/, registrar o plugin, e obter o pluginId real. A IA apenas substitui o valor `99999` pelo ID fornecido.

### Implementações:

#### B45.1 — Substituir pluginId
- Substituir `int pluginId = 99999;` pelo ID real fornecido pelo usuário
- Alternativa: se o usuário não quiser métricas, comentar/remover o bloco `setupBStats()`

---

## B46 — Corrigir Versão, Import Duplicado e `printStackTrace()` `[x]`

**Escopo**: Correções de qualidade de código identificadas na auditoria pré-lançamento. Inclui versão errada no `plugin.yml`, import duplicado e uso proibido de `printStackTrace()`.

**Arquivos afetados**: `plugin.yml`, `config.yml`, `ClaimManager.java`, `MarketData.java`, `EndResetManager.java`, `GorvaxCommand.java`, `BossManager.java`
**Risco**: Baixo

### Implementações:

#### B46.1 — Corrigir versão no `plugin.yml`
- Alterar `version: '1.0.0'` para `version: '2.2.0'` (linha 2)
- Atualizar comentário `# v1.0.0` no `config.yml` (linha 2) para `# v2.2.0`

#### B46.2 — Remover import duplicado em `ClaimManager.java`
- Remover `import org.bukkit.Location;` duplicado (linha 22, já importado na linha 4)

#### B46.3 — Substituir `printStackTrace()` por `getLogger()` (5 ocorrências)
- `MarketData.java:241` → `plugin.getLogger().log(Level.SEVERE, "Erro ao salvar market_local.yml", e);`
- `EndResetManager.java:408` → `plugin.getLogger().log(Level.WARNING, "Falha ao limpar dragonUUID via reflection", e);`
- `ClaimManager.java:406` → `plugin.getLogger().log(Level.SEVERE, "Erro ao salvar claims.yml", e);`
- `GorvaxCommand.java:602` → `plugin.getLogger().log(Level.SEVERE, "Erro no comando admin", e);`
- `BossManager.java:311` → `plugin.getLogger().log(Level.SEVERE, "Erro em Boss I/O", e);`

---

## B47 — Limpar Catch Blocks Vazios e Código Deprecated `[x]`

**Escopo**: Catch blocks que engolem exceções silenciosamente dificultam debug em produção. Adicionar logging mínimo nos catch blocks vazios e avaliar remoção de código deprecated.

**Arquivos afetados**: `ClaimManager.java`, `NationManager.java`, `WorldBoss.java`, `LootManager.java`, `BossCommand.java`, `PriceHistoryManager.java`, `YamlDataStore.java`, `SQLiteDataStore.java`, `MySQLDataStore.java`, `MainMenuGUI.java`
**Risco**: Baixo

### Implementações:

#### B47.1 — Adicionar logging em catch blocks vazios (~15 ocorrências)
- Catch blocks `catch (Exception e) {}` → adicionar `plugin.getLogger().fine("...: " + e.getMessage());`
- Catch blocks `catch (Exception ignored)` → avaliar se devem logar em `FINE`/`WARNING`
- Exceções em parsing de enums (`Claim.Type`, `TrustType`): logar como `FINE` (esperado em migração)
- Exceções em `/* ignora */` nos DataStores: logar como `FINE` com tipo inválido

#### B47.2 — Limpar código deprecated em `MainMenuGUI.java`
- Verificar se o método `@Deprecated` na linha 814 ("Usar Page1Holder") ainda é referenciado
- Se não for usado: remover completamente
- Se for usado: atualizar chamadores para usar `Page1Holder`

---

## B48 — Setup In-Game (Spawn, WorldGuard, Coordenadas) `[ ]`

**Escopo**: Configurar o mundo do servidor: colar schematics, definir spawn, proteger regiões e posicionar todos os blocos interativos (estantes de lore, totems, crates) nas coordenadas corretas.
**Arquivos afetados**: `lore_books.yml`, `crates.yml`, WorldGuard regions
**Risco**: Médio (requer trabalho in-game)

### Implementações:

#### B48.1 — Colar Schematics
- Colar `gorvax_spawn.schem` com FAWE (`//paste`)
- Colar `gorvax_cidade.schem` adjacente ao spawn
- Verificar que ambos carregaram corretamente

#### B48.2 — Configurar Spawn
- `/setworldspawn` no centro do spawn
- Criar região `spawn` no WorldGuard com flags: `pvp deny`, `build deny`, `mob-spawning deny`, `creeper-explosion deny`

#### B48.3 — Coordenadas de Lore e Crates
- Definir coordenadas reais no `lore_books.yml` para estantes e totems de bioma
- Definir coordenadas no `crates.yml` para crates físicas no spawn
- Colocar os blocos físicos (BOOKSHELF, LODESTONE, ENDER_CHEST) nas coordenadas definidas

---

## B49 — Permissões LuckPerms (Grupos e Nodes) `[ ]`

**Escopo**: Configurar toda a hierarquia de permissões do servidor usando LuckPerms. Criar os grupos de rank e VIP, atribuir nodes de permissão do GorvaxCore.
**Arquivos afetados**: LuckPerms (configuração in-game)
**Risco**: Médio (permissões erradas = jogadores sem acesso)

### Implementações:

#### B49.1 — Criar Grupos de Rank
- `/lp creategroup default` (jogador base)
- `/lp creategroup vip` (herda de default)
- `/lp creategroup mvp` (herda de vip)
- `/lp creategroup elite` (herda de mvp)
- `/lp creategroup admin` (todas as permissões)

#### B49.2 — Permissões do Grupo Default
- `gorvax.player.*` — comandos básicos (/menu, /rank, /kit, /reino, etc.)
- `gorvax.market.*` — acesso ao mercado
- `gorvax.quest.*` — acesso às quests
- `gorvax.cosmetic.use` — usar cosméticos desbloqueados

#### B49.3 — Permissões VIP/MVP/Elite
- `gorvax.vip.*` — benefícios VIP (homes extras, claims extras, desconto no mercado)
- `gorvax.mvp.*` — benefícios MVP
- `gorvax.elite.*` — benefícios Elite
- Configurar prefixos/sufixos de chat via LuckPerms meta

#### B49.4 — Permissões Admin
- `gorvax.admin.*` — todos os comandos admin
- `gorvax.boss.spawn` — spawnar bosses manualmente
- `gorvax.bypass.*` — bypass de cooldowns e limites

---

## B50 — Loja do Servidor (Tebex + Integração) `[x]`

**Escopo**: Configurar a loja online do servidor usando Tebex (Buycraft). Criar pacotes de VIP, Crate Keys e Cosméticos. Integrar com o plugin para entrega automática de comandos.
**Arquivos afetados**: Tebex (painel web), plugin Tebex no servidor
**Risco**: Médio (envolve configuração de pagamento)

### Implementações:

#### B50.1 — Criar Conta e Webstore no Tebex
- Registrar em [tebex.io](https://tebex.io)
- Criar webstore do GorvaxMC
- Configurar métodos de pagamento (PIX, cartão, PayPal)
- Personalizar aparência da loja (cores, logo, banner)

#### B50.2 — Instalar Plugin Tebex no Servidor
- Baixar plugin Tebex para Paper
- Configurar secret key via `/tebex secret <key>`
- Testar conexão com `/tebex info`

#### B50.3 — Criar Categorias e Pacotes
- **Categoria: VIPs** (comando: `/vip set {username} <tier>`)
  - Pacote VIP (30 dias): `vip set {username} vip` + `lp user {username} parent addtemp vip 30d`
  - Pacote VIP+ (30 dias): `vip set {username} vip_plus` + `lp user {username} parent addtemp mvp 30d`
  - Pacote Elite (30 dias): `vip set {username} elite` + `lp user {username} parent addtemp elite 30d`
  - Pacote Lendário (30 dias): `vip set {username} lendario` + `lp user {username} parent addtemp elite 30d`
- **Categoria: Crate Keys** (comando: `/crate give {username} <tipo> <qtd>`)
  - Pack 5 Keys Comuns: `crate give {username} comum 5`
  - Pack 3 Keys Raras: `crate give {username} raro 3`
  - Pack 1 Key Lendária: `crate give {username} lendario 1`
  - Pack 1 Key Sazonal: `crate give {username} sazonal 1`
- **Categoria: Cosméticos** (comando: `/cosmetics give {username} <id>`)
  - Tags de Chat (CHAT_TAG)
  - Partículas de Caminhada (WALK_PARTICLE)
  - Trilhas de Flecha (ARROW_TRAIL)
  - Efeitos de Kill (KILL_EFFECT)

#### B50.4 — Configurar Preços e Descrições
- Definir preços em BRL para cada pacote
- Escrever descrições atrativas com lista de benefícios
- Adicionar imagens/ícones aos pacotes
- Sugestão de preços iniciais:
  - VIP: R$14,90 | VIP+: R$24,90 | Elite: R$39,90 | Lendário: R$59,90
  - 5 Keys Comuns: R$4,90 | 3 Keys Raras: R$9,90 | 1 Key Lendária: R$14,90
  - Cosméticos individuais: R$2,90 ~ R$9,90

#### B50.5 — Testar Fluxo Completo
- Realizar compra de teste (modo sandbox do Tebex)
- Verificar que o comando foi executado no servidor
- Confirmar que o jogador recebeu o benefício

---

## B51 — Testes End-to-End (Validação Completa) `[ ]`

**Escopo**: Validar todos os sistemas do servidor jogando como um jogador novo. Testar cada fluxo crítico para garantir que tudo funciona antes de abrir ao público.
**Arquivos afetados**: Nenhum (apenas testes)
**Risco**: Baixo

### Implementações:

#### B51.1 — Fluxo de Novo Jogador
- Entrar como jogador novo (sem permissões admin)
- Verificar Tutorial automático + Welcome Kit
- Verificar menu principal (`/menu`)

#### B51.2 — Sistema de Reinos
- Criar um reino
- Fazer claim de chunks
- Convidar membro e testar trust
- Verificar proteção contra griefing

#### B51.3 — Combate e Bosses
- Spawnar um World Boss
- Lutar e verificar mecânicas (fases, skills)
- Verificar loot e baú de recompensas
- Confirmar ranking de dano

#### B51.4 — Economia e Mercado
- Vender item no mercado
- Comprar item de outro jogador
- Verificar saldo e histórico

#### B51.5 — Lore e Quests
- Interagir com estante de lore
- Ativar totem de bioma
- Verificar progresso de quest

#### B51.6 — Bedrock (Geyser)
- Conectar via Bedrock Edition
- Verificar menus (SimpleForm fallback)
- Testar combate e interações

---

## B52 — Segurança Operacional (Backups e Ops) `[ ]`

**Escopo**: Configurar a segurança operacional do servidor para produção. Backup automático, revisão de ops e hardening.
**Arquivos afetados**: Scripts de servidor, `ops.json`
**Risco**: Baixo

### Implementações:

#### B52.1 — Script de Backup Automático
- Criar script (`.bat` ou `.sh`) que compacta a pasta do servidor
- Agendar execução diária (Task Scheduler no Windows / cron no Linux)
- Manter últimos 7 backups (rotação automática)

#### B52.2 — Revisar `ops.json`
- Garantir que apenas admins reais estão como OP
- Remover OPs de teste/temporários

#### B52.3 — Hardening Final
- Verificar `server.properties`: `white-list`, `enforce-secure-profile`
- Confirmar que `online-mode=false` está correto (nLogin gerencia auth)
- Revisar configs do Grim AC para evitar falsos positivos

---

## B53 — Sistema de Top Doadores (Tags Automáticas) `[ ]`

**Escopo**: Implementar sistema de tracking de doações com tags dinâmicas no chat para os top 3 doadores do servidor. Integra com o `LeaderboardManager` e `ChatManager` existentes.
**Arquivos afetados**: `PlayerData.java`, `LeaderboardManager.java`, `ChatManager.java`, `GorvaxExpansion.java`, `cosmetics.yml`, `messages.yml`, `config.yml`, novo `DonationManager.java`
**Risco**: Baixo

### Implementações:

#### B53.1 — DonationManager + PlayerData
- Novo campo `total_donated` (double) em `PlayerData`
- Novo `DonationManager.java` com:
  - `addDonation(UUID player, double amount)` — registra doação
  - `getTopDonors(int count)` → List<DonorEntry> — retorna top N
  - `getDonorRank(UUID player)` → int (1, 2, 3 ou 0)
  - Persistência em `playerdata.yml`
  - Cache com refresh a cada 5 minutos (reutiliza pattern do LeaderboardManager)

#### B53.2 — Tags Dinâmicas no Chat
- Novas tags em `cosmetics.yml`:
  - `tag_donor_1`: `§6[🏆 TOP DOADOR]` — source: `admin`
  - `tag_donor_2`: `§e[🥈 DOADOR]` — source: `admin`
  - `tag_donor_3`: `§c[🥉 DOADOR]` — source: `admin`
- `ChatManager.formatMessage()` prioriza tag de doador sobre cosmética comum
- Tags atribuídas/removidas automaticamente no `rebuild()` do cache

#### B53.3 — Comando Admin
- `/gorvax donor add <player> <valor>` — registra doação (perm: `gorvax.admin.donor`)
- `/gorvax donor remove <player> <valor>` — remove valor
- `/gorvax donor top` — mostra ranking top 10
- `/gorvax donor check <player>` — mostra total de um jogador

#### B53.4 — PlaceholderAPI
- `%gorvax_top_doacoes_1_name%` — nome do top 1 doador
- `%gorvax_top_doacoes_1_value%` — total doado pelo top 1
- `%gorvax_donor_total%` — total doado pelo jogador atual
- `%gorvax_donor_rank%` — posição do jogador no ranking

#### B53.5 — Integração com Leaderboard
- Nova categoria `doacoes` no `LeaderboardManager.CATEGORIES`
- Exibe no `/ranking doacoes`
- Holograma opcional com os top 3

---

## 📝 Template de Batch

> Use este template ao adicionar novos batches ao roadmap.

<!--
## BXX — Nome do Batch `[ ]`

**Escopo**: Descrição do que será feito.
**Arquivos afetados**: Lista de arquivos novos/modificados.
**Risco**: Baixo/Médio/Alto

### Implementações:

#### BXX.1 — Sub-tarefa 1
- Detalhes da implementação
- Config: `chave.no.config: valor`

#### BXX.2 — Sub-tarefa 2
- Detalhes da implementação

#### Log de Execução (YYYY-MM-DD)

| Tarefa | Status |
|--------|--------|
| Descrição da tarefa | ✅/❌ |
-->

---

## 📝 Log de Execução

> A IA deve adicionar uma entrada aqui após completar cada batch.

| Data | Batch | Executado por | Notas |
|------|-------|---------------|-------|
| 2026-03-11 | B38 | IA | Expansão do menu para 2 páginas com 25 botões. Paginação Java + SimpleForm Bedrock. 48 novas chaves em messages.yml. |
| 2026-03-11 | B39 | IA | Melhoria de lores/tooltips em todos os menus. createMenuItem() refatorado para multi-line (\n split). 25+ lores expandidos com comandos, detalhes e dicas. Hints em sub-GUIs (Daily, BattlePass, Crate, Codex). MANUAL.md atualizado. |
| 2026-03-11 | B40 | IA | Guia do Rei (WRITTEN_BOOK) no KingdomAdminMenu Java+Bedrock. Handler KING_GUIDE envia guia completo no chat (8 categorias, 20+ comandos). Lores melhoradas em KingdomMainMenu (Spawn, Diplomacia, Lotes, Admin), KingdomPermissionsMenu (individual+geral), KingdomLotsMenu (estado vazio). Botão "Visitar Reinos" no KingdomMainMenu. MANUAL.md atualizado. |
| 2026-03-11 | B41 | IA | Menu de Configurações expandido para submenu (SettingsHolder, 27 slots). Toggles: HUD (LIME_DYE/GRAY_DYE), Som de Fronteira (PlayerData), Canal de Chat (cicla 6 canais via ChatManager). Bedrock SimpleForm com estado ✅/❌. 13 novas chaves em messages.yml. MANUAL.md §58 adicionado. |
| 2026-03-11 | B42 | IA | Banner de startup corrigido: versão hardcoded `v1.0.0` substituída por `getPluginMeta().getVersion()`. Variável `version` extraída para evitar drift entre banner e mensagem de inicialização. |
| 2026-03-11 | B43 | IA | ~100 strings hardcoded migradas de 7 arquivos Java para `messages.yml`. Arquivos: VipCommand (~24), StructureCommand (~50), RankCommand (~10), KitCommand (~2), CosmeticCommand (~3), ReputationCommand (~4), SeasonalEventCommand (~2), GorvaxCommand (~15). ~140 novas chaves adicionadas. |
| 2026-03-11 | B44 | IA | Autosave periódico global implementado. Task async salva 12 managers (claims, playerdata, kingdoms, market, audit, auction, priceHistory, mail, vote, bounty, nations, structures) a cada 5min configurável. Config `autosave.enabled`/`interval_minutes` em config.yml. 1 nova chave em messages.yml. MANUAL.md §59 adicionado. |
| 2026-03-11 | B45 | IA | Plugin registrado em bstats.org (ID: 30054). Placeholder `99999` substituído pelo ID real em `GorvaxCore.java` e `V0_to_V1.java`. Teste `ConfigMigratorTest` atualizado. |
| 2026-03-11 | B46 | IA | Versão corrigida em plugin.yml (1.0.0→2.2.0) e config.yml. Import duplicado `org.bukkit.Location` removido de ClaimManager.java. 5 ocorrências de `printStackTrace()` substituídas por `getLogger().log()` em MarketData, EndResetManager, ClaimManager, GorvaxCommand, BossManager. |
| 2026-03-11 | B47 | IA | 15 catch blocks vazios em 9 arquivos agora logam em `FINE` (ClaimManager ×4, NationManager ×2, WorldBoss ×1, LootManager ×3, BossCommand ×1, PriceHistoryManager ×1, YamlDataStore ×1, SQLiteDataStore ×1, MySQLDataStore ×1). Classe deprecated `MainMenuHolder` e check de compatibilidade removidos de MainMenuGUI.java (pré-lançamento, sem instâncias em cache). |
