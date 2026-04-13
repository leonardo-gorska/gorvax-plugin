# 📖 ROADMAP DE PRODUÇÃO — E-Book GorvaxMC

> **Instruções para a IA**: Leia este arquivo completamente. Encontre o próximo batch com status `[ ]` (pendente).
> Execute TODAS as tarefas desse batch. Ao terminar, marque-o como `[x]` e atualize a seção "Progresso".
> **A interação DEVE ser 100% em Português (Brasil).**
> **Antes de gerar imagens, leia os arquivos de configuração** (`boss_settings.yml`, `custom_items.yml`, `cosmetics.yml`, `crates.yml`, `mini_bosses.yml`, `achievements.yml`, `config.yml`) para representar fielmente cada entidade, item e mecânica.

---

## 📐 Diretrizes do E-Book

### Tema Visual
- **Estilo**: Reino Medieval / RPG / Fantasia com toque Semi-Anárquico
- **Paleta**: Preto, roxo escuro, dourado, carmesim, com detalhes em azul-cyan
- **Fontes**: Pixel art / medieval fantasy (ex: Press Start 2P, MedievalSharp)
- **Decorações**: Bordas de pedra/obsidian, ícones de espada/escudo/coroa, separadores temáticos

### Separação de Conteúdo
- **📗 Manual do Jogador** (arquivo principal): Todas as funcionalidades acessíveis por jogadores comuns
- **📕 Manual da Staff** (arquivo separado): Comandos admin, configurações de servidor, storage, permissões avançadas

### Seções do Manual do Jogador (📗) — 40 seções
1. Terrenos (Claims) e Lotes
2. Sistema de Confiança (Trust)
3. Reinos (criação, membros, upgrades, visitas)
4. Nações (meta-reinos)
5. Diplomacia (alianças, guerras, tratados)
6. Guerra entre Reinos (mecânicas de guerra, roubo de chunks, peace treaty)
7. Mercado Global (preço dinâmico, oferta/demanda)
8. Mercado Local (comércio entre jogadores próximos)
9. Leilão Global
10. Histórico de Preços (gráfico de oferta/demanda)
11. World Bosses (7 bosses: Gorvax, Indrax, Zarith, Kaldur, Skulkor, Xylos, Vulgathor)
12. Mini-Bosses por Bioma (4: Guardião do Deserto, Sentinela Gélida, Araña da Selva, Fantasma do Nether)
13. Sistema de Combate (Combat Tag, Kill Streaks, Proteção de Spawn)
14. Sistema de Duelos
15. Conquistas
16. Postos Avançados (Outposts)
17. Bounties
18. Correio (Mailbox)
19. Votação no Reino
20. Chat (Global, Local, Reino, Nação)
21. Scoreboard / HUD (ativar, desativar, configurar)
22. Estatísticas do Jogador (kills, mortes, KDR, playtime, etc.)
23. Proteção do seu Claim (o que o claim protege: Wither, Crystal, TNT, Bed Bomb, etc.)
24. Itens Lendários (5 itens customizados)
25. Crates / Keys (4 tipos)
26. Cosméticos (25 cosméticos em 5 categorias)
27. VIP & Ranks Premium (4 tiers)
28. Battle Pass Sazonal
29. Daily Rewards & Login Streak
30. Tutorial Interativo + Welcome Kit
31. Menu Central
32. Leaderboards & Rankings
33. Discord Integration
34. Impostos & Taxas do Reino
35. Reputação / Karma (5 ranks, descontos/penalidades, bounty automática)
36. Sistema de Estruturas (POIs, /estrutura, themes)
37. Códex de Gorvax (Enciclopédia Interativa)
38. Ranks de Progressão e Kits
39. Lore — Livros, Totems e Quests Narrativas
40. RTP (Teleporte Aleatório)

### Seções do Manual da Staff (📕) — 18 seções
1. Comandos Admin (`/gorvax admin`, `/gorvax reload`, etc.)
2. Configuração do Servidor — `config.yml` detalhado (TODAS as chaves)
3. Arquivos de Configuração de Conteúdo (`boss_settings.yml`, `custom_items.yml`, `cosmetics.yml`, `crates.yml`, `battlepass.yml`, `achievements.yml`, `mini_bosses.yml`, `market_global.yml`, `boss_rewards.yml`, `quests.yml`)
4. Sistema de Mensagens (`messages.yml` — personalização completa)
5. Storage & Banco de Dados (YAML/SQLite/MySQL, migração, HikariCP)
6. Arquivos de Dados Runtime (`kingdoms.yml`, `claims.yml`, `playerdata.yml`, `nations.yml`, `votes.yml`, `bounties.yml`, `mail.yml`, `boss_data.yml`)
7. Permissões Completas (todas as nodes `gorvax.*`)
8. Placeholders (PlaceholderAPI — lista completa)
9. Proteção Anti-Exploit (configuração técnica de cada proteção)
10. Integração Web Map (Dynmap/BlueMap)
11. Reset do End (configuração e agendamento)
12. Audit Logs (rastreamento de ações)
13. Compatibilidade Bedrock (Floodgate forms, fallbacks, limitações)
14. Dependências e Plugins Externos (Vault, WorldGuard, LuckPerms, etc.)
15. Conflitos de Plugins (GuildPlugin vs Reinos, LPC vs ChatManager, BedrockFormShop vs Mercado)
16. Métricas Anônimas (bStats) — o que é coletado e como desabilitar
17. Migração Automática de Configs — configuração e extensibilidade
18. Códex de Gorvax — configuração de categorias e entradas (`codex.yml`)

### Geração de Imagens — Diretrizes

> [!IMPORTANT]
> **Cada imagem DEVE ser fiel ao servidor.** Antes de gerar qualquer imagem, a IA DEVE ler:
> - `boss_settings.yml` — Para HP, skills, partículas, equipamento e escala exata de cada boss
> - `custom_items.yml` — Para material base, enchants, lore e efeitos de cada item lendário
> - `cosmetics.yml` — Para tipo de partícula, contagem e efeitos visuais de cada cosmético
> - `mini_bosses.yml` — Para entity_type, equipamento, bioma, skills e loot
> - `crates.yml` — Para rewards, raridades e ícones de cada crate
> - `achievements.yml` — Para ícone, descrição e recompensas de cada conquista

**Estilo das imagens:**
- Mix de Minecraft render (3D blocky) com arte fantasia épica
- Cenários dramáticos com iluminação cinematográfica
- Mobs demonstrando partículas e skills fielmente
- Itens com brilho de encantamento e aura especial
- Panoramas de reinos com castelos e bandeiras

**Gerar com calma**: Espaçar gerações para não sobrecarregar o serviço. Se retornar erro 503, aguardar e tentar novamente mais tarde.

---

## 📊 Progresso Geral

| Batch | Descrição | Status |
|-------|-----------|--------|
| | **— FASE 1: Fundação HTML/CSS —** | |
| E1 | Estrutura HTML + CSS temático completo | `[x]` Concluído |
| E2 | Conteúdo do Manual do Jogador (Parte 1: Território & Social) | `[x]` Concluído |
| | **— FASE 2: Conteúdo do Jogador —** | |
| E3 | Conteúdo do Manual do Jogador (Parte 2: Combate & Bosses) | `[x]` Concluído |
| E4 | Conteúdo do Manual do Jogador (Parte 3: Economia & Progressão) | `[x]` Concluído |
| | **— FASE 3: Imagens & Arte —** | |
| E5 | Imagens dos World Bosses + Mini-Bosses (usar texturas do resource pack) | `[x]` Concluído |
| E6 | Imagens de Itens Lendários (usar texturas do resource pack) + Crates/VIP | `[x]` Concluído |
| | **— FASE 4: Manual da Staff + Polimento —** | |
| E7 | Manual da Staff (📕) — arquivo separado | `[x]` Concluído |
| E8 | Polimento final, capa, índice interativo, versão PDF | `[x]` Concluído |
| | **— FASE 5: Atualização v1.0.0 —** | |
| E9 | Conteúdo novo v1.0.0 (Códex, Ranks/Kits, Lore, RTP, bStats, Config Migration) | `[x]` Concluído |

**Último batch executado**: E9 — Conteúdo novo v1.0.0
**Data da última execução**: 2026-03-10
**Próximo batch a executar**: Nenhum — Roadmap 100% concluído! 🎉

---

# 🏗️ FASE 1 — Fundação HTML/CSS (E1)

---

## E1 — Estrutura HTML + CSS Temático `[x]`

**Escopo**: Criar o esqueleto HTML do e-book com todo o CSS temático medieval/RPG.
**Arquivo**: `ebook_jogador.html`
**Risco**: Baixo

### Implementações:

#### E1.1 — CSS Design System
- Paleta de cores: `--obsidian: #1a1a2e`, `--nether-gold: #d4a017`, `--ender-purple: #9b59b6`, `--blood-red: #c0392b`, `--diamond-blue: #3498db`
- Fontes: Google Fonts `Press Start 2P` para títulos, `Inter` ou `Merriweather` para corpo
- Gradientes: fundo de obsidian para bedrock, bordas com brilho de encantamento
- Pixel borders com CSS `box-shadow` simulando textura de pedra
- Scroll suave, links internos do índice

#### E1.2 — Layout Base
- Página de capa decorativa (título épico, subtítulo "Manual do Aventureiro", versão do plugin)
- Índice interativo com links para cada seção
- Header fixo por seção com ícone + título
- Separadores temáticos entre seções (SVG espadas cruzadas, ou CSS art)
- Footer decorativo
- Seção "Sobre o servidor" com resumo

#### E1.3 — Componentes Reutilizáveis
- `.command-table` — tabela estilizada para comandos (borda de pedra, hover com brilho)
- `.config-block` — bloco de código YAML estilizado (fundo escuro, syntax highlight)
- `.boss-card` — card de boss com espaço para imagem, HP bar, skills
- `.item-card` — card de item lendário com lore estilizada
- `.tip-box` — dica com ícone de tocha
- `.warning-box` — aviso com ícone de TNT
- `.info-box` — informação com ícone de livro
- `.crate-card` — card de crate com cor do tier

#### E1.4 — Responsividade e Print
- Media queries para telas pequenas
- `@media print` para exportação PDF limpa (sem sombras pesadas, fontes legíveis)
- Quebras de página inteligentes entre seções

---

# 📗 FASE 2 — Conteúdo do Manual do Jogador (E2-E4)

---

## E2 — Território, Reinos & Social `[x]`

**Escopo**: Popular o e-book com conteúdo dos sistemas de terreno, reinos, nações e social.
**Arquivo**: `ebook_jogador.html`
**Risco**: Baixo

### Seções incluídas:

#### E2.1 — Terrenos (Claims)
- Baseado na seção 1 do MANUAL.md
- Como criar um claim (pá de ouro + 2 cliques)
- Tipos de trust (Acesso, Construção, Contêiner, Gerente)
- Comandos: `/confirmar`, `/abandonar`, `/verificar`, etc.
- Tabela de preços de blocos

#### E2.2 — Lotes (Subplots)
- Criação, venda, aluguel
- Comandos de lotes

#### E2.3 — Reinos
- Fundação, membros, cargos (Rei, Oficial, Membro)
- Sede, visitas, upgrades
- Impostos e banco do reino
- Tabela completa de `/reino` subcomandos

#### E2.4 — Nações
- Meta-reinos, Imperador
- Buffs por nível (Speed, Resistance)
- Banco, convites, chat `/nc`

#### E2.5 — Diplomacia
- Alianças, Inimizades, Neutro, Guerra
- Tratados de paz
- Tabela de relações

#### E2.6 — Chat & Comunicação
- Canais: Global, Local, Reino, Nação
- Formatação, comandos
- Sistema de ignore

#### E2.7 — Correio & Votação
- `/carta` sistema de cartas offline
- `/reino votar` sistema de votação interna

#### E2.8 — Scoreboard / HUD
- Como ativar/desativar o scoreboard
- Informações exibidas (reino, saldo, kills, mortes)
- Comando de toggle

#### E2.9 — Proteção do seu Claim
- O que seu terreno protege automaticamente (perspectiva do jogador)
- Anti-Wither, Anti-Crystal, Anti-TNT Cannon, Anti-Bed Bomb
- Anti-Chorus Fruit, Limite de Redstone
- Como funciona sem que o jogador precise fazer nada

---

## E3 — Combate, Bosses & PvP `[x]`

**Escopo**: Conteúdo dos sistemas de combate, bosses e PvP.
**Arquivo**: `ebook_jogador.html`
**Risco**: Baixo

### Seções incluídas:

#### E3.1 — Sistema de Combate
- Combat Tag (15s, comandos bloqueados, ActionBar)
- PvP Logger NPC (deslog → NPC com inventário)
- Kill Streaks (5, 10, 20 kills — títulos e bounty)
- Proteção de Spawn (5s invulnerabilidade)

#### E3.2 — Sistema de Duelos
- `/duel` — desafio, aposta, countdown
- Regras completas (distância, duração, PvP forçado)
- Tabela de configuração

#### E3.3 — World Bosses (7 bosses)
Para CADA boss, incluir card detalhado com:
- **Rei Gorvax** — Wither Skeleton, HP 1200, 6 skills (Salto Esmagador, Chuva de Meteoros, Prisão de Almas, Teleporte, Rugido do Soberano, Chama do Trono, Decreto Real). Escala 1.6x. Aura: flame+smoke+soul. Equipamento: Golden Helmet + Golden Sword.
- **Indrax Abissal** — Warden(?), HP 1100, 8 skills (Pulso do Vazio, Dreno de Almas, Singularidade, Fúria, Void Erase, Dark Paralysis, Abyss Collapse, Contamination). Escala 1.3x. Aura: sculk_soul+vibration+end_rod.
- **Zarith** — Spider, HP 450, 5 skills (Poison Web, Predator Leap, Ambush, Toxic Burst, Frenzy). Escala 1.8x. Aura: slime+smoke.
- **Kaldur** — Stray, HP 550, 6 skills (Frost Aura, Blizzard, Ice Lance, Glacial Prison, Frost Shield, Ice Storm). Escala 1.7x. Aura: snowflake+end_rod.
- **Skulkor** — Skeleton, HP 650, 5 skills (Call of Dead, Arrow Rain, Bone Shield, War Cry, Death March). Escala 1.8x. Aura: smoke+soul.
- **Xylos** — Enderman, HP 750, 5 skills (Spatial Rupture, Gravitational Implosion, Ether Fragment, Distortion, Void Phase). Escala 2.0x. Aura: reverse_portal+portal.
- **Vulgathor** — Blaze, HP 900, 6 skills (Fire Aura, Incendiary Rain, Ash Explosion, Igneous Fury, Magma Pillar, Inferno). Escala 1.8x. Aura: flame+smoke+lava.

#### E3.4 — Mini-Bosses por Bioma (4 bosses)
- **Guardião do Deserto** — Husk, HP 300, armadura dourada, Sandstorm + Summon Minions
- **Sentinela Gélida** — Stray, HP 400, diamond/chainmail, Frost Nova + Ice Arrows
- **Araña da Selva** — Cave Spider, HP 250, escala 2.0x, Web Trap + Poison Cloud
- **Fantasma do Nether** — Blaze, HP 500, Fire Rain + Flame Shield

#### E3.5 — Outposts & Bounties
- Postos avançados (conquista e pontos)
- Sistema de bounties (`/bounty`)

#### E3.6 — Guerra entre Reinos
- Como declarar guerra (`/reino guerra`)
- Mecânicas de guerra (roubo de chunks, dano ao banco)
- Duração, condições de vitória/derrota
- Tratado de paz
- Relação com Diplomacia

---

## E4 — Economia, Progressão & Cosméticos `[x]`

**Escopo**: Conteúdo dos sistemas de economia, progressão e personalização.
**Arquivo**: `ebook_jogador.html`
**Risco**: Baixo

### Seções incluídas:

#### E4.1 — Mercado Global
- Compra/venda com preço dinâmico (oferta/demanda)
- Taxa de transação, preços mínimos e máximos
- Comandos `/market`

#### E4.2 — Mercado Local
- Comércio entre jogadores próximos
- Como criar loja local, comprar/vender
- Diferenças do mercado global

#### E4.3 — Leilão Global
- `/leilao` — criar, dar lance, buyout
- Duração, taxas, limites

#### E4.4 — Histórico de Preços
- Gráfico de evolução de preços
- Comando `/historico`
- Como interpretar tendências de oferta/demanda

#### E4.5 — Itens Lendários (5 itens)
Cards detalhados para cada item real do servidor:
- **⚔ Lâmina de Gorvax** — Netherite Sword, Sharpness VII, Fire Aspect III, Wither on-hit 30%. Drop: Rei Gorvax Top 1.
- **👑 Coroa de Indrax** — Netherite Helmet, Protection VI, +4 HP, Water Breathing passiva. Drop: Indrax Top 1-3.
- **💨 Botas Velocistas** — Diamond Boots, Protection V, Feather Falling V, Speed passiva. Conquista: 50 bosses.
- **🏹 Arco do Caçador** — Bow, Power VI, Infinity, Poison 25% + Slowness 40%. Drop: Zarith Top 1-3.
- **🛡 Escudo do Guardião** — Shield, Unbreaking VI, Mending, +2 HP, Knockback Resistance 40%. Conquista: Muro de Ferro.

#### E4.6 — Crates & Keys (4 tiers)
Para CADA crate, mostrar tabela de rewards com peso/chance:
- **Crate Comum** (§7, Chest) — Dinheiro, blocos, diamantes, maçãs, ferro, XP
- **Crate Raro** (§9, Ender Chest) — Dinheiro maior, netherite, god apples, elytra, keys, título "Sortudo"
- **Crate Lendário** (§6, Dragon Egg) — $5000, 1000 blocos, itens lendários, netherite, elytra, keys raros, títulos "Lendário" e "Abençoado"
- **Crate Sazonal** (§c, Jack O'Lantern) — $3000, 500 blocos, totem, god apples, key lendário, título "Evento"

#### E4.7 — Cosméticos (25 cosméticos)
Organizado por categoria com cards visuais:
- **Partículas ao Caminhar** (6): Caminho de Fogo (FLAME), Corações (HEART), Encantamento (ENCHANTMENT_TABLE), Musical (NOTE), Portal do End (PORTAL), Nevasca (SNOWFLAKE)
- **Trails de Flecha** (4): Trilha Mágica (CRIT_MAGIC), Trilha de Fogo (FLAME), Trilha das Almas (SOUL_FIRE_FLAME), Trilha Aquática (DRIPPING_WATER)
- **Tags de Chat** (5): VIP, ELITE, LENDÁRIO, GUERREIRO, CONSTRUTOR
- **Kill Effects** (3): Relâmpago (LIGHTNING), Fogos (FIREWORK), Onda de Choque (EXPLOSION)
- **Kill Particles** (3): Totem Divino (TOTEM_OF_UNDYING), Chuva de Lava (LAVA), Sopro do Dragão (DRAGON_BREATH)

#### E4.8 — VIP & Ranks Premium (4 tiers)
Tabela comparativa dos tiers:
- ✦ VIP — +500 blocos, +2 homes, 1 key raro/mês
- ✦ VIP+ — +1500 blocos, +5 homes, 2 raros + 1 lendário/mês, 5% desconto
- ⚡ ELITE — +3000 blocos, +10 homes, 3 raros + 1 lendário/mês, 10% desconto
- 🐉 LENDÁRIO — +5000 blocos, +15 homes, 3 raros + 2 lendários/mês, 15% desconto

#### E4.9 — Battle Pass, Daily Rewards, Conquistas
- Battle Pass: 30 níveis, track Free/Premium, fontes de XP
- Daily Rewards: ciclo de 7 dias, streak mechanics
- Conquistas: 11 achievements com ícones e recompensas

#### E4.10 — Menu Central, Rankings, Discord, Tutorial
- `/menu` — 10 ícones, layout 54 slots
- `/top` — 8 categorias de ranking
- Discord Webhooks — alertas e chat sync
- Tutorial de primeiro login

#### E4.11 — Estatísticas & Impostos
- `/stats` — kills, mortes, KDR, playtime, bosses abatidos
- Sistema de impostos do reino (como funciona para o membro)
- Taxas de transação (mercado, leilão)

---

# 🎨 FASE 3 — Imagens & Arte (E5-E6)

> **Regra de ouro**: NUNCA gerar uma imagem sem ter lido o arquivo de config correspondente primeiro.
> **Pacing**: Gerar no máximo 2-3 imagens por conversa para evitar erro 503.

---

## E5 — Imagens de Bosses `[x]`

**Escopo**: Embeddar as texturas reais do resource pack para cada World Boss e Mini-Boss no ebook.
**Fonte das imagens**: `resourcepack/java/assets/minecraft/optifine/cit/gorvax_bosses/` (11 PNGs já existentes)
**Arquivo alvo**: `ebook_jogador.html`
**Risco**: Baixo (imagens já existem, só precisam ser embeddadas)

> [!IMPORTANT]
> **NÃO gerar imagens novas!** Usar as texturas que já existem no resource pack.
> Copiar os PNGs de `resourcepack/java/assets/minecraft/optifine/cit/gorvax_bosses/` para o mesmo diretório do ebook,
> e referenciá-los com `<img>` nos boss-cards do HTML.

### Texturas disponíveis:

#### E5.1 — World Bosses (7 texturas)

| Boss | Arquivo da textura | Entidade |
|------|--------------------|----------|
| Rei Gorvax | `king_gorvax.png` | Wither Skeleton 1.6x |
| Indrax Abissal | `indrax_abissal.png` | Drowned 1.3x |
| Zarith | `zarith.png` | Spider 1.8x |
| Kaldur | `kaldur.png` | Stray 1.7x |
| Skulkor | `skulkor.png` | Skeleton 1.8x |
| Xylos | `xylos.png` | Enderman 2.0x |
| Vulgathor | `vulgathor.png` | Blaze 1.8x |

#### E5.2 — Mini-Bosses (4 texturas)

| Boss | Arquivo da textura | Entidade |
|------|--------------------|----------|
| Guardião do Deserto | `guardiao_deserto.png` | Husk 1.5x |
| Sentinela Gélida | `sentinela_gelida.png` | Stray 1.6x |
| Araña da Selva | `aranha_selva.png` | Cave Spider 2.0x |
| Fantasma do Nether | `fantasma_nether.png` | Blaze 1.8x |

### Implementação:
1. Copiar os 11 PNGs para o mesmo diretório do `ebook_jogador.html`
2. Substituir os placeholders CSS-only por `<img src="NOME.png">` nos boss-cards
3. Ajustar CSS dos boss-cards para acomodar a imagem (max-width: 200px, border-radius, sombra)

---

## E6 — Imagens de Itens, Crates & Cosméticos `[x]`

**Escopo**: Embeddar texturas reais dos itens lendários do resource pack + gerar imagens para crates, cosméticos e VIP.
**Fonte das texturas de itens**: `resourcepack/java/assets/gorvax/textures/item/` (9 PNGs já existentes)
**Arquivo alvo**: `ebook_jogador.html`
**Risco**: Baixo para itens (texturas existem), Médio para crates/VIP (geradas)

> [!IMPORTANT]
> **Para itens lendários: NÃO gerar imagens novas!** Usar as texturas do resource pack.
> Para crates, cosméticos e VIP: gerar imagens novas pois não existem texturas pré-feitas.

### E6.1 — Itens Lendários (9 texturas já existentes)
Copiar de `resourcepack/java/assets/gorvax/textures/item/`:

| Item | Arquivo |
|------|---------|
| ⚔ Lâmina de Gorvax | `gorvax_blade.png` |
| 👑 Coroa de Indrax | `indrax_crown.png` |
| 💨 Botas Velocistas | `speed_boots.png` |
| 🏹 Arco do Caçador | `hunter_bow.png` |
| 🛡 Escudo do Guardião | `guardian_shield.png` |
| 🔥 Manto de Vulgathor | `vulgathor_mantle.png` |
| 🧊 Lança de Kaldur | `kaldur_lance.png` |
| 💀 Elmo de Skulkor | `skulkor_helm.png` |
| 🌀 Pérola de Xylos | `xylos_pearl.png` |

### Implementação dos itens:
1. Copiar os 9 PNGs para o mesmo diretório do `ebook_jogador.html`
2. Substituir os placeholders CSS-only por `<img>` nos item-cards
3. Manter imagem em tamanho adequado (max-width: 120px)

#### E6.2 — Crates (1 imagem com 4 crates)
Ler `crates.yml`:
- 4 crates lado a lado: Comum (cinza, Chest), Raro (azul, Ender Chest), Lendário (dourado, Dragon Egg glow), Sazonal (vermelho, Jack O'Lantern)
- Partículas e itens saindo de cada uma

#### E6.3 — Cosméticos (1-2 imagens)
Ler `cosmetics.yml`:
- Montagem mostrando jogador com diferentes cosméticos: partículas de fogo nos pés, trilha na flecha, tag no chat, relâmpago ao matar
- Demonstrar visualmente cada tipo

#### E6.4 — VIP Showcase (1 imagem)
- 4 jogadores lado a lado, cada um com a tag do seu tier (VIP verde, VIP+ azul, ELITE dourado, LENDÁRIO roxo)
- Efeitos visuais crescentes

#### E6.5 — Capa do E-Book (1 imagem)
- Arte épica combinando: castelo num penhasco, Rei Gorvax ao fundo com aura, espadas cruzadas na frente
- Título "GorvaxMC — Manual do Aventureiro" em estilo medieval
- Semi-anárquico: elementos de caos controlado (fogo, escuridão, mas com ordem)

---

# 📕 FASE 4 — Manual da Staff & Polimento (E7-E8)

---

## E7 — Manual da Staff `[x]`

**Escopo**: Criar e-book separado com conteúdo exclusivo para administradores.
**Arquivo**: `ebook_staff.html`
**Risco**: Baixo

### Seções incluídas:

#### E7.1 — Painel Admin
- Comandos `/gorvax admin` detalhados
- `/gorvax reload`, `/gorvax migrate`, `/gorvax debug`
- Gerenciamento de claims, reinos, bosses
- Comandos de custom items: `/customitem give`, `/customitem list`
- Comandos de crates: `/crate give`, `/crate reload`
- Comandos de cosméticos: `/cosmetics give`
- Comandos de VIP: `/vip set`, `/vip remove`, `/vip keys`
- Comandos de Battle Pass: `/pass premium`, `/pass reset`, `/pass reload`

#### E7.2 — Configuração Principal (`config.yml`)
- TODAS as chaves documentadas com valores padrão e descrição
- Organizado por módulo (claims, kingdoms, combat, bosses, market, etc.)
- Dicas de performance e tuning

#### E7.3 — Arquivos de Configuração de Conteúdo
Para CADA arquivo, documentar formato, como adicionar/editar itens:
- `boss_settings.yml` — como configurar bosses (HP, skills, partículas, escala)
- `custom_items.yml` — como adicionar itens lendários (base, enchants, on-hit, passive)
- `cosmetics.yml` — como adicionar cosméticos (partículas, trails, tags, kill effects)
- `crates.yml` — como configurar crates (rewards, peso, broadcast)
- `battlepass.yml` — como configurar temporadas (XP, rewards por nível)
- `achievements.yml` — como adicionar/modificar conquistas (trigger, meta, reward)
- `mini_bosses.yml` — como configurar mini-bosses (entity, bioma, skills, loot)
- `market_global.yml` — como configurar itens do mercado (preços base, min/max)
- `boss_rewards.yml` — configuração de loot dos world bosses
- `quests.yml` — como configurar quests diárias e semanais

#### E7.4 — Sistema de Mensagens (`messages.yml`)
- Como personalizar todas as mensagens do plugin
- Variáveis disponíveis por contexto
- Formatação com cores (§) e placeholders

#### E7.5 — Storage & Infraestrutura
- Storage: YAML vs SQLite vs MySQL (quando usar cada um)
- Migração de dados (`/gorvax migrate yaml sqlite`, etc.)
- HikariCP connection pooling (configuração MySQL)
- Performance e cache

#### E7.6 — Arquivos de Dados Runtime
- O que são e onde ficam:
  - `kingdoms.yml` — dados de todos os reinos
  - `claims.yml` — terrenos registrados
  - `playerdata.yml` — dados de cada jogador
  - `nations.yml` — dados de nações
  - `votes.yml` — votações ativas
  - `bounties.yml` — bounties ativas
  - `mail.yml` — cartas entre jogadores
  - `boss_data.yml` — estado atual dos bosses
  - `local_market.yml` — lojas locais
- Como fazer backup
- O que NUNCA editar manualmente

#### E7.7 — Referência de Permissões
- Tabela completa de TODAS as nodes `gorvax.*`
- Organizada por módulo
- Sugestão de grupos LuckPerms

#### E7.8 — Referência de Placeholders
- Tabela completa de todos os placeholders PAPI
- Organizados por categoria
- Exemplos de uso com TAB e DeluxeMenus

#### E7.9 — Proteção Anti-Exploit (Configuração Técnica)
- Cada proteção individualmente com config e comportamento
- Anti-Wither, Anti-Crystal, Anti-TNT Cannon, Anti-Bed Bomb
- Limite de Redstone por chunk
- Anti-Chorus Fruit

#### E7.10 — Web Map (Dynmap / BlueMap)
- Como ativar/configurar
- Cores dos reinos, popups HTML
- Diferenças entre Dynmap e BlueMap

#### E7.11 — Reset do End
- Configuração e agendamento
- Comandos forçados

#### E7.12 — Audit Logs
- Rastreamento de ações admin
- Formato e armazenamento

#### E7.13 — Compatibilidade Bedrock
- Floodgate forms (SimpleForm, ModalForm)
- Fallbacks para inventário
- Limitações conhecidas
- Skins e heads no Bedrock

#### E7.14 — Dependências e Plugins Externos
- Obrigatórias: Vault, WorldGuard, PlaceholderAPI
- Opcionais: LuckPerms, Floodgate
- Incluídas: AnvilGUI, HikariCP

#### E7.15 — Conflitos de Plugins Conhecidos
- **GuildPlugin** vs Sistema de Reinos/Nações — avaliar remoção
- **LPC** vs ChatManager do GorvaxCore — avaliar integração
- **BedrockFormShop** vs Mercado Bedrock do GorvaxCore — potencialmente redundante
- Como diagnosticar e resolver conflitos

---

## E8 — Polimento Final `[x]`

**Escopo**: Revisão geral, ajustes finais, capa, índice e versão PDF.
**Risco**: Baixo

### Implementações:

#### E8.1 — Revisão de Conteúdo
- Verificar se TODAS as 34 seções do jogador estão completas
- Verificar se TODAS as 15 seções da staff estão completas
- Cross-reference com MANUAL.md para nada faltar
- Cross-reference com todos os 13 arquivos .yml
- Corrigir ortografia e formatação

#### E8.2 — Índice Interativo
- Links clicáveis para cada seção
- Scroll suave com animação
- Breadcrumbs no topo

#### E8.3 — Capas Finais
- Capa do Manual do Jogador (com imagem gerada em E6.5)
- Capa do Manual da Staff (tema similar, mais sóbrio)
- Contracapa com créditos

#### E8.4 — Exportação PDF
- Testar print do navegador
- Ajustar `@media print` para qualidade
- Verificar quebras de página
- Gerar PDF final de cada manual

#### E8.5 — Verificação no Navegador
- Abrir HTML no browser e verificar visual
- Testar links internos
- Testar responsividade
- Verificar todas as imagens carregando
- Validar tabelas e blocos de código

---

## 📋 Inventário de Assets do Servidor

> Referência rápida do que existe nos configs para geração fiel de imagens.

### World Bosses (boss_settings.yml)

| Boss | Entity | HP | Escala | Aura Principal | Skills |
|------|--------|----|--------|----------------|--------|
| Rei Gorvax | Wither Skeleton | 1200 | 1.6x | Flame+Smoke+Soul | 7 skills |
| Indrax Abissal | — | 1100 | 1.3x | Sculk Soul+Vibration+End Rod | 8 skills |
| Zarith | Spider | 450 | 1.8x | Slime+Smoke | 5 skills |
| Kaldur | Stray | 550 | 1.7x | Snowflake+End Rod | 6 skills |
| Skulkor | Skeleton | 650 | 1.8x | Smoke+Soul | 5 skills |
| Xylos | Enderman | 750 | 2.0x | Reverse Portal+Portal | 5 skills |
| Vulgathor | Blaze | 900 | 1.8x | Flame+Smoke+Lava | 6 skills |

### Mini-Bosses (mini_bosses.yml)

| Boss | Entity | HP | Escala | Biomas | Equipamento |
|------|--------|----|--------|--------|-------------|
| Guardião do Deserto | Husk | 300 | 1.5x | Desert, Badlands | Full Gold + Gold Sword |
| Sentinela Gélida | Stray | 400 | 1.6x | Snow Plains, Ice Spikes | Diamond+Chainmail + Bow |
| Araña da Selva | Cave Spider | 250 | 2.0x | Jungle, Bamboo Jungle | — |
| Fantasma do Nether | Blaze | 500 | 1.8x | Nether Wastes, Soul Sand Valley | — |

### Itens Lendários (custom_items.yml)

| Item | Base | Enchants Principais | Efeito Especial | Fonte |
|------|------|---------------------|-----------------|-------|
| ⚔ Lâmina de Gorvax | Netherite Sword | Sharpness VII, Fire Aspect III | Wither on-hit 30% | Rei Gorvax (Top 1) |
| 👑 Coroa de Indrax | Netherite Helmet | Protection VI | Water Breathing passiva, +4 HP | Indrax (Top 1-3) |
| 💨 Botas Velocistas | Diamond Boots | Protection V, Feather Falling V | Speed passiva | 50 bosses mortos |
| 🏹 Arco do Caçador | Bow | Power VI, Infinity | Poison 25% + Slow 40% | Zarith (Top 1-3) |
| 🛡 Escudo do Guardião | Shield | Unbreaking VI, Mending | Weakness 20%, +2 HP, 40% KB resist | Conquista |
| 🔥 Manto de Vulgathor | Netherite Chestplate | Protection VI, Fire Protection V | Fire Resistance passiva, HARM on-hit 35% | Vulgathor (Top 1-3) |
| 🧊 Lança de Kaldur | Trident | Sharpness VI, Loyalty III, Impaling V | Slowness on-hit 40%, Mining Fatigue 20% | Kaldur (Top 1-3) |
| 💀 Elmo de Skulkor | Netherite Helmet | Protection VI, Thorns II | +6 HP, Strength on-hit 15% | Skulkor (Top 1-3) |
| 🌀 Pérola de Xylos | Netherite Axe | Sharpness VII, Efficiency VI | Blindness on-hit 25%, Levitation 10% | Xylos (Top 1-3) |

### Cosméticos (cosmetics.yml) — 25 totais

| Categoria | Qtd | Exemplos |
|-----------|-----|----------|
| Walk Particles | 6 | Fogo, Corações, Encantamento, Musical, Portal, Nevasca |
| Arrow Trails | 4 | Mágica, Fogo, Almas, Aquática |
| Chat Tags | 5 | VIP, ELITE, LENDÁRIO, GUERREIRO, CONSTRUTOR |
| Kill Effects | 3 | Relâmpago, Fogos, Onda de Choque |
| Kill Particles | 3 | Totem, Lava, Sopro do Dragão |

### Crates (crates.yml) — 4 tipos

| Tipo | Ícone | Broadcast | Rewards Principais |
|------|-------|-----------|-------------------|
| Comum | Chest (§7) | Não | $100-250, 50-100 blocos, diamantes, ferro |
| Raro | Ender Chest (§9) | Não | $500-1000, netherite, god apples, elytra (2%) |
| Lendário | Dragon Egg (§6) | Sim | $2500-5000, itens lendários, netherite, títulos |
| Sazonal | Jack O'Lantern (§c) | Sim | $1000-3000, totem, key lendário |

---

> 📌 **Nota**: Este roadmap é executado via `/ebook` em cada conversa. A IA deve ler este arquivo, encontrar o próximo batch `[ ]`, e executá-lo completamente antes de marcar como `[x]`.

---

# 📚 FASE 5 — Atualização v1.0.0 (E9)

---

## E9 — Conteúdo novo v1.0.0 `[x]`

**Escopo**: Adicionar seções para features implementadas após o E-Book original (B28-B35).
**Arquivos**: `ebook_jogador.html`, `ebook_staff.html`
**Risco**: Baixo

### Seções a adicionar no Manual do Jogador (📗):

#### E9.1 — Códex de Gorvax (Enciclopédia Interativa)
- Categorias (Bestírio, Biomas, Itens, Lore)
- Desbloqueio por ações: kills, mineração, exploração, quests
- Barra de progresso por categoria e global
- Comando `/codex` com GUI
- Placeholders PAPI

#### E9.2 — Ranks de Progressão e Kits
- 5 ranks (Aventureiro → Lendário) com requisitos
- Kits por rank com cooldown
- Comandos `/rank` e `/kit`

#### E9.3 — Lore — Livros, Totems e Quests Narrativas
- Estantes de lore com livros interativos
- Totems de bioma com texto narrativo
- 3 quests de lore multi-step com diálogos e recompensas

#### E9.4 — RTP (Teleporte Aleatório)
- `/rtp` com cooldown, evita claims, blocos inseguros

### Seções a adicionar no Manual da Staff (📕):

#### E9.5 — Métricas Anônimas (bStats)
- O que é coletado (tabela completa)
- Como desabilitar
- Dashboard público

#### E9.6 — Migração Automática de Configs
- Como funciona (backup + migração sequencial)
- Comando `/gorvax migrateconfig`
- Como criar novas migrações (extensibilidade)

#### E9.7 — Códex de Gorvax (Admin)
- Configuração de `codex.yml`: categorias, entradas, unlock types
- Como adicionar novas entradas

