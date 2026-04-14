# 🚀 Checklist de Lançamento — GorvaxMC

> Última atualização: 2026-03-11
> Servidor: `C:\Users\Gorska\Desktop\Gorvax\`

---

## 🔴 1. Infraestrutura do Servidor

### 1.1 Software Base
- [x] Paper 1.21+ instalado (`server.jar`)
- [x] EULA aceita
- [x] Porta 25565 configurada
- [x] Script de inicialização (`ligar.bat`)
- [x] Dificuldade hard, survival, PvP on
- [x] MOTD configurado: `§6§lGorvax§aCore §7- §eO Despertar dos Bosses`

### 1.2 Plugins Instalados
- [x] **Vault** — economia
- [x] **WorldGuard** + WorldEdit — proteção
- [x] **PlaceholderAPI** — placeholders
- [x] **LuckPerms** — permissões (com DB H2 ativa)
- [x] **Geyser + Floodgate** — suporte Bedrock
- [x] **EssentialsX** + EssentialsXSpawn — comandos básicos
- [x] **GorvaxCore** — plugin principal (v1.0.0)
- [x] **nLogin** — autenticação (online-mode=false)
- [x] **TAB** — tablist customizada
- [x] **ProtocolLib** — protocolo
- [x] **SkinsRestorer** — skins
- [x] **spark** — profiling
- [x] **BetterRTP** — teleporte aleatório
- [x] **Jobs** — empregos


### 1.3 Resource Pack
- [x] Resource Pack Java hospedado (GitHub releases)
- [x] SHA1 hash configurado no `server.properties`
- [x] `require-resource-pack=true`
- [x] Prompt customizado para download
- [x] Pack Bedrock via Geyser — configurado

---

## 🟠 2. Configuração do GorvaxCore

### 2.1 Configs já deployadas no servidor (29 arquivos)
- [x] `config.yml` (15KB)
- [x] `messages.yml` (105KB)
- [x] `boss_settings.yml` + `boss_rewards.yml`
- [x] `mini_bosses.yml`
- [x] `custom_items.yml`
- [x] `crates.yml`
- [x] `quests.yml`
- [x] `market_global.yml` + `price_history.yml`
- [x] `battlepass.yml`
- [x] `achievements.yml`
- [x] `cosmetics.yml`
- [x] `seasonal_events.yml`
- [x] `structures.yml`
- [x] `playerdata.yml` (já tem dados de teste)
- [x] `claims.yml` + `towns.yml`

### 2.2 Arquivos NOVOS para deployar (desta sessão)
- [x] Copiar novo `GorvaxCore-1.0.0-all.jar`
- [x] Copiar novo `messages.yml` (com mensagens de lore quest)
- [x] Copiar `lore_books.yml` (NOVO — livros + estantes + totems)
- [x] Copiar `quests.yml` atualizado (com lore_quests)

### 2.3 Coordenadas para definir (precisa estar in-game)
- [ ] `lore_books.yml` → coordenadas das 7 estantes
- [ ] `lore_books.yml` → coordenadas dos 4 totems
- [ ] `crates.yml` → coordenadas das 4 crates físicas no spawn
- [ ] Colocar blocos reais nas coordenadas (BOOKSHELF, LODESTONE, ENDER_CHEST)

### 2.4 Mundo
- [x] Gerar mundo novo
- [x] Colar schematics do spawn e cidade
- [x] `/setworldspawn` no centro do spawn
- [x] WorldGuard: região `spawn` protegida

---

## 🟡 3. Permissões (LuckPerms)

LuckPerms está instalado com DB H2 ativa. Os grupos base (`default`, `vip`, `admin`) são configurados automaticamente pelo GorvaxCore ao iniciar.

- [x] Grupo `default` com `gorvax.player.*` (auto-configurado pelo plugin)
- [x] Grupo `vip` com `gorvax.vip.*` (auto-configurado pelo plugin)
- [x] Grupo `admin` com `gorvax.admin.*` (auto-configurado pelo plugin)
- [ ] Testar que jogador default consegue usar `/reino`, `/quest`, `/mercado`
- [ ] Testar que comandos admin são restritos

---

## 🟢 4. Testes End-to-End

- [ ] Jogador novo → tutorial + Welcome Kit
- [ ] Criar reino → claim funciona
- [ ] Quests diárias → progresso atualiza
- [ ] Boss spawn → loot + ranking + diálogos
- [ ] Lore → estantes, totems, quests narrativas
- [ ] Mercado → comprar/vender
- [ ] Duelo + Karma + Bounty
- [ ] Jogador Bedrock via Geyser

---

## 🔵 5. Segurança

- [x] nLogin (autenticação)
- [x] spark (profiling)
- [x] **Grim AC v2.3.73** — anti-cheat (fly, speed, killaura, reach, etc.)
- [x] **CoreProtect CE v23.1** — rollback/logging (detecta e reverte dupes)
- [x] **BlueMap v5.16** — mapa web interativo do mundo


---

## ⚪ 6. Pós-Launch

- [ ] NPCs de lore (Ferreiro, Monge, Sábio)
- [ ] Mais conteúdo de lore
- [ ] Discord webhook
- [ ] Dynmap/BlueMap
- [ ] Divulgação
- [ ] Script de backup automático (`.bat` + Agendador de Tarefas)
- [ ] Revisar `ops.json` (quem é OP)

---

## 📊 Progresso Geral

| Fase | Status |
|------|--------|
| Plugin (código) | ✅ Pronto |
| Spawn + Cidade | ✅ Construídos |
| Resource Pack | ✅ Java configurado |
| Plugins instalados | ✅ 23 plugins |
| GorvaxCore configs | ✅ 29 arquivos deployados |
| Deploy novo jar + lore | ✅ 5 arquivos copiados |
| Mundo | ✅ Gerado + schematics colados |
| Coordenadas lore/crates | ⬜ Definir in-game |
| Permissões LuckPerms | ✅ Auto-configurado pelo plugin |
| Resource Pack Bedrock | ✅ Configurado via Geyser |
| Anti-cheat + Mapa | ✅ Grim + CoreProtect + BlueMap |
| Testes end-to-end | ⬜ Pendente |
