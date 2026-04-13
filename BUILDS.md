# 🏗 BUILDS DO GORVAXMC — Plano de Estruturas Procedurais

> Cada batch é executado via `/build`. Uma estrutura por execução.
> Script: `tools/gorvax_builder.py` | Helpers: `tools/build_helpers.py`
> Saída: `tools/output/*.schem`

---

## BATCH 1 — Portal das Cinzas (Spawn)
- **Arquivo de saída:** `tools/output/gorvax_spawn.schem`
- [x] Gerar estrutura

### Specs
- **Dimensões:** 120×80×120 blocos
- **Tema:** Portal dimensional, sombrio mas iluminado
- **Lore:** Ruínas do portal que conectava os dois Pilares (Chamas e Vazio). Trégua mágica impede combate.

### Layout
```
    ┌─────────────────────────┐
    │  Torre NW    Torre NE   │
    │  ┌─── Muralha Norte ──┐ │
    │  │                     │ │
    │  │  ┌─Biblioteca──┐   │ │
    │  │  │ de Étheris   │   │ │
    │  │  │ (15×10×8)    │   │ │
    │  │  └──────────────┘   │ │
    │  │                     │ │
    │  │ ┌─NPC──┐ ┌─NPC──┐  │ │
    │  │ │Ferr. │ │Sábio │  │ │
    │  │ └──────┘ └──────┘  │ │
    │  │                     │ │
    │  │    ★ PORTAL ★       │ │
    │  │  (plat. circular    │ │
    │  │   raio 8, obsidiana │ │
    │  │   + crying obsidian)│ │
    │  │                     │ │
    │  │  ┌─ Fonte ─┐       │ │
    │  │  │ (raio 3) │       │ │
    │  │  └──────────┘       │ │
    │  │                     │ │
    │  │ ┌──Crates──┐        │ │
    │  │ │4 Ender   │        │ │
    │  │ │Chests    │        │ │
    │  │ └──────────┘        │ │
    │  │                     │ │
    │  │ ┌─Tutorial Mural──┐ │ │
    │  │ │ (signs + bell)  │ │ │
    │  │ └─────────────────┘ │ │
    │  └─── Muralha Sul ────┘ │
    │  Torre SW    Torre SE   │
    └─────────────────────────┘
          ↓ Portão Sul → Valheim
```

### Elementos Obrigatórios
1. **Plataforma do Portal** (centro): Circulo raio 8 de `obsidian` + `deepslate_tiles`, com `crying_obsidian` e `netherrack` + `fire` no centro. 4 pilares de `polished_blackstone` (h=12) com `soul_lantern` no topo
2. **Muralhas**: `deepslate_bricks` base, `polished_blackstone` detalhes, h=8 + merlões h=2
3. **4 Torres**: Octogonais raio 4, h=15, telhado cônico `dark_oak_stairs`
4. **Portão Sul**: Arco triplo `deepslate_tiles` (5×6) — saída para Valheim
5. **Portão Norte**: Arco menor (3×4) — saída mundo selvagem
6. **Biblioteca de Étheris** (NW, 15×10×8): `spruce_planks` piso, `dark_oak` estrutura, paredes de `bookshelves`, `lectern` central, `enchanting_table`
7. **Área de Crates** (SE): Plataforma elevada +3, 4 `ender_chest` em pedestais de `quartz_pillar`
8. **Espaço de NPCs** (CN): Bancada ferreiro (furnace+anvil+grindstone) e mesa alquimista (brewing_stand+cauldron)
9. **Mural Tutorial** (S): 5×3 de `oak_sign` com arco de `polished_granite` e `bell`
10. **Fonte** (pátio): Circular raio 3, `prismarine` + `water`
11. **Piso**: Mix de `stone_bricks`, `cracked_stone_bricks`, `mossy_stone_bricks`
12. **Iluminação**: `soul_torch` a cada 4 blocos nas muralhas, `lantern` + `chain` interiores, `candle` na biblioteca
13. **Vegetação**: `azalea`, `moss_block` bordas, `hanging_roots` tetos

### Paleta de Blocos
| Uso | Blocos |
|-----|--------|
| Estrutural | `deepslate_bricks`, `polished_blackstone`, `tuff_bricks` |
| Portal | `obsidian`, `crying_obsidian`, `deepslate_tiles` |
| Biblioteca | `spruce_planks`, `dark_oak_planks`, `bookshelf` |
| Crates | `quartz_pillar`, `amethyst_cluster`, `ender_chest` |
| Iluminação | `soul_lantern`, `soul_torch`, `lantern`, `candle` |
| Decoração | `end_rod`, `moss_block`, `azalea`, `chain` |

---

## BATCH 2 — Valheim, a Cidade dos Aventureiros (Reino Inicial)
- **Arquivo de saída:** `tools/output/gorvax_valheim.schem`
- [x] Gerar estrutura

### Specs
- **Dimensões:** 300×80×300 blocos
- **Tema:** Medieval-rústico, ruínas restauradas de Ashvale
- **Lore:** Primeiro assentamento dos aventureiros sobre ruínas do Domínio de Ashvale (ferreiros guerreiros)

### Layout
```
                    N (portão mundo)
    ┌──────────────────────────────────┐
    │          MURALHA NORTE            │
    │  Torre NW              Torre NE  │
    │                                   │
    │   ┌── DISTRITO MERCANTIL ───┐    │
    │   │ Banco(15×12) Leilão(10×8)│    │
    │   │ Mercado Central          │    │
    │   │ (12 barracas de 3×3)     │    │
    │   └──────────────────────────┘    │
    │                                   │
    │      PRAÇA CENTRAL (raio 20)     │
    │      Fonte 3 andares + Obelisco  │
    │      Estátua Gorvax (h=12)       │
    │                                   │
    │   ┌── DISTRITO RESIDENCIAL ──┐   │
    │   │ 20-30 casas procedurais   │   │
    │   │ (peq 7×7, med 9×9, grd   │   │
    │   │  11×11 — variações)       │   │
    │   │ Taverna "Chama & Bigorna" │   │
    │   │ (20×15×10)               │   │
    │   └──────────────────────────┘   │
    │                                   │
    │   ┌── DISTRITO MILITAR ──────┐   │
    │   │ Forja de Ashvale (18×14)  │   │
    │   │ Campo Treino (20×20)      │   │
    │   │ Quartel (12×10)           │   │
    │   └──────────────────────────┘   │
    │  Torre SW              Torre SE  │
    │          MURALHA SUL             │
    └──────────────────────────────────┘
              ↓ S (estrada → Spawn)
```

### Elementos Obrigatórios
1. **Muralha**: `stone_bricks` + `mossy_stone_bricks`, h=10 + merlões h=3
2. **8 Torres**: 4 cantos + 4 portões, octogonais raio 5, h=18
3. **4 Portões**: Arcos `stone_brick_stairs` com portcullis (`iron_bars` + `oak_fence`)
4. **Fosso**: Canal 3 blocos largura × 3 profundidade ao redor, pontes nos portões
5. **Mercado Central**: Telhado aberto `dark_oak`, 12 barracas com `trapdoor` balcões
6. **Banco**: Edifício `polished_granite` + `quartz_block` (15×12×9) com cofres e mesa
7. **Leiloeiro**: `stripped_birch_log` (10×8×7) com `lectern` púlpito
8. **Praça Central**: Circular raio 20, `polished_andesite` + `smooth_stone`, fonte de `prismarine` 3 andares, obelisco `blackstone` h=12 com `gold_block` topo + `soul_fire`
9. **20-30 casas procedurais**: 3 templates (peq/med/grd) com variação de madeira (oak/spruce/birch/dark_oak) + fundação (cobblestone/stone_bricks)
10. **Taverna "Chama & Bigorna"** (20×15×10): Bar com `barrel`, `cauldron`, `brewing_stand`, lareira `campfire`, 2º andar com quartos
11. **Forja de Ashvale** (18×14×9): `blackstone` + `deepslate`, `magma_block` no chão, `blast_furnace`×4, `anvil`×2, `smithing_table`, `lava` em canais, `cracked_deepslate_bricks` nas paredes, `lectern` com lore
12. **Campo Treino** (20×20 aberto): `hay_bale` + `target` alvos, `armor_stand` bonecos, `dark_oak_fence` cercado
13. **Quartel** (12×10×7): Beliches (camas empilhadas + trapdoors)
14. **Ruas**: `gravel` + `coarse_dirt` (2-3 blocos largura)
15. **Iluminação**: `lantern` ruas, `soul_lantern` muralhas, `candle` interiores

### Paleta de Blocos
| Uso | Blocos |
|-----|--------|
| Muralha | `stone_bricks`, `mossy_stone_bricks`, `cobblestone_wall` |
| Casas | `oak_planks`, `spruce_planks`, `birch_planks`, `dark_oak_planks` |
| Forja | `blackstone`, `deepslate`, `magma_block`, `cracked_deepslate_bricks` |
| Praça | `polished_andesite`, `smooth_stone`, `prismarine` |
| Ruas | `gravel`, `coarse_dirt`, `dirt_path` |
| Decoração | `barrel`, `hay_bale`, `flower_pot`, `composter` |

---

## BATCH 3 — Arena do Crepúsculo (PvP / Duelos)
- **Arquivo de saída:** `tools/output/gorvax_arena.schem`
- [x] Gerar estrutura

### Specs
- **Dimensões:** 150×60×150 blocos
- **Tema:** Coliseu antigo, arena de combate épica
- **Lore:** Local onde Gorvax e Skulkor travaram a primeira batalha. Solo impregnado com energia de combate.

### Layout
```
                N (Entrada Principal)
    ┌──────────────────────────┐
    │   LOBBY / RECEPÇÃO       │
    │   (20×10×8) Arco triplo  │
    │   Hall + Mural recordes  │
    │   + NPC Árbitro           │
    │                          │
    │   ┌── BANCADAS ──────┐   │
    │   │ 6 andares         │   │
    │   │ escalonados       │   │
    │   │  ┌────────────┐  │   │
    │   │  │            │  │   │
    │   │  │   ARENA    │  │   │
    │   │  │  CENTRAL   │  │   │
    │   │  │  (oct 40)  │  │   │
    │   │  │            │  │   │
    │   │  └────────────┘  │   │
    │   │                  │   │
    │   │ Bancada N: Geral │   │
    │   │ Bancada S: VIP   │   │
    │   │ Camarote (topo)  │   │
    │   └──────────────────┘   │
    │                          │
    │  ┌─Sala──┐  ┌─Sala──┐   │
    │  │ Prep  │  │ Prep  │   │
    │  │ Time1 │  │ Time2 │   │
    │  │(10×10)│  │(10×10)│   │
    │  └───────┘  └───────┘   │
    │                          │
    │   PORTÃO SUL (VIPs)      │
    └──────────────────────────┘
                S
```

### Elementos Obrigatórios
1. **Arena Central**: Octogonal inscrita em 40×40, piso `smooth_sandstone` + `red_sandstone` xadrez, bordas `deepslate_wall` h=3, símbolo central de `gold_block` (espadas cruzadas), 2 gates (E/W) com `iron_bars`, 4 pilares `stone_bricks` (2×2×3) como cobertura tática
2. **Bancadas** (anfiteatro 6 andares): `stone_brick_stairs` assentos + `polished_andesite` corredores
3. **Bancada Norte (Geral)**: 4 fileiras, acesso livre
4. **Bancada Sul (VIP)**: `quartz_stairs` 2 fileiras, toldo de `wool` colorida
5. **Camarote Real** (topo sul): Box fechado com `glass_pane` + `dark_oak`, 3×5×4
6. **Lobby** (N, 20×10×8): Arco triplo `quartz_block`, piso `polished_granite` + `polished_diorite`, mural de `oak_sign` para recordes, stand para NPC
7. **2 Salas de Preparação** (S, 10×10×6 cada): `stripped_spruce_log` paredes, `cobblestone` chão, `crafting_table`, `anvil`, `ender_chest`, `armor_stand`, `bed`, porta `iron_door` para arena
8. **Fachada externa**: Coliseu octogonal `stone_bricks` + `chiseled_stone_bricks`, 8 banners alternados (fogo/vazio)
9. **Iluminação**: `soul_torch` muralhas, `lantern` interiores, `redstone_lamp` + `sea_lantern` bancadas
10. **Decoração**: `cobweb` nos cantos superiores, `cracked_stone_bricks` espalhados, mob heads (`wither_skeleton_skull`, `skeleton_skull`) em `item_frame` no lobby
11. **Exterior**: Cercado cavalos (`oak_fence` + `hay_bale`), estrada `dirt_path`

### Paleta de Blocos
| Uso | Blocos |
|-----|--------|
| Arena piso | `smooth_sandstone`, `red_sandstone`, `gold_block` |
| Bordas | `deepslate_wall`, `deepslate_tiles` |
| Bancadas | `stone_brick_stairs`, `quartz_stairs`, `polished_andesite` |
| Lobby | `quartz_block`, `polished_granite`, `polished_diorite` |
| Prep rooms | `stripped_spruce_log`, `cobblestone` |
| Fachada | `stone_bricks`, `chiseled_stone_bricks` |
| Iluminação | `redstone_lamp`, `sea_lantern`, `soul_torch`, `lantern` |

---

## 📦 Após Gerar Todos os .schem

### Mapa Geral do Mundo
![Mapa do mundo GorvaxMC com todas as estruturas](c:\Users\Gorska\Desktop\gorvax-plugin\tools\world_layout_map.png)

### Como usar no servidor:
```bash
# 1. Copiar para pasta do WorldEdit
cp tools/output/*.schem plugins/WorldEdit/schematics/

# 2. No Minecraft (com op):
/tp 0 70 0
//schem load gorvax_spawn
//paste -a

/tp 0 70 500
//schem load gorvax_valheim
//paste -a

/tp 400 70 700
//schem load gorvax_arena
//paste -a

# Para as montanhas (atrás de Valheim, NE):
/tp 200 70 350
//schem load gorvax_vulcoes
//paste -a
//smooth 5   (suavizar transição com terreno)

# Para a dungeon Ashvale (base das montanhas):
/tp 200 60 400
//schem load gorvax_ashvale
//paste -a

# Para os totems (ir até o bioma correto antes):
//schem load gorvax_totem_deserto
//paste -a
# (repetir para cada totem no bioma correspondente)

# Para o monastério Glacius (picos gelados):
/tp -300 90 -200
//schem load gorvax_glacius
//paste -a

# Para a biblioteca Étheris (zona isolada):
/tp -200 70 400
//schem load gorvax_etheris
//paste -a

# 3. Registrar no GorvaxCore:
/estrutura criar spawn "§6Portal das Cinzas" portal 60
/estrutura criar valheim "§e⚔ Valheim" medieval 150
/estrutura criar arena "§c⚔ Arena do Crepúsculo" arena 75
/estrutura criar vulcoes "§4🌋 Montanhas de Ashvale" vulcao 100
```

---

## BATCH 4 — Montanhas Vulcânicas de Ashvale
- **Arquivo de saída:** `tools/output/gorvax_vulcoes.schem`
- [x] Gerar estrutura

### Specs
- **Dimensões:** 200×90×150 blocos (2-3 montanhas em grupo)
- **Tema:** Vulcões ativos, forjas ancestrais, cinzas e lava
- **Lore:** As montanhas onde o Domínio de Ashvale forjava armas contra Gorvax. O Pilar de Chamas rachado enviou energia para cá.
- **Posição:** Nordeste de Valheim (~200, 70, 350) — visíveis das muralhas norte da cidade

### Elementos Obrigatórios
1. **Montanha Principal** (h=70-80): `basalt` + `blackstone` + `deepslate` com lava escorrendo em 3-4 cascatas. Cratera no topo com `lava` + `magma_block` + `crying_obsidian`
2. **Montanha Secundária** (h=50-60): Menor, mais rochosa, sem lava ativa. `tuff` + `andesite` + `diorite`
3. **Montanha Terciária** (h=40): Colina vulcânica morta com `coal_ore` exposto e `dead_bush`
4. **Fumaça**: `campfire` escondidos sob blocos no topo de cada montanha (gera partículas de fumaça naturalmente!)
5. **Base**: Terreno desolado — `coarse_dirt`, `soul_sand`, `gravel`, `dead_bush`, sem grama
6. **Detalhes de Ashvale**: Ruínas de 2-3 fornalhas gigantes (blast_furnace + anvil + abandonados) na base da montanha principal — restos da civilização
7. **Rios de lava**: Canais de `lava` descendo as montanhas até poças na base, com `basalt` e `obsidian` nas bordas
8. **Transição**: Bordas com `gravel` + `cobblestone` + `mossy_cobblestone` para suavizar com o terreno natural

### Paleta de Blocos
| Uso | Blocos |
|-----|--------|
| Montanha | `basalt`, `smooth_basalt`, `blackstone`, `deepslate` |
| Vulcânico | `magma_block`, `lava`, `crying_obsidian`, `obsidian` |
| Detalhes | `tuff`, `andesite`, `diorite`, `coal_ore` |
| Base | `coarse_dirt`, `soul_sand`, `gravel`, `dead_bush` |
| Ruínas | `blast_furnace`, `anvil`, `cracked_stone_bricks`, `chain` |

---

## BATCH 5 — 4 Totems de Bioma (Mini-Bosses)
- **Arquivo de saída:** `tools/output/gorvax_totems.schem` (4 totems em um arquivo, separados por distância)
- [x] Gerar estrutura

### Specs
- **Dimensões:** 7×10×7 blocos cada (4 totems)
- **Tema:** Totems antigos de lore — cada um tematizado pro mini-boss do bioma
- **Lore:** Estruturas que contam a história dos guardiões naturais dos biomas. Exigem exploração real.

### Os 4 Totems

#### Totem do Deserto (Guardião do Deserto)
- **Bioma:** Desert
- **Materiais:** `sandstone` + `smooth_sandstone` + `gold_block` detalhes
- **Forma:** Obelisco de arenito (h=8) com cabeça de husk no topo (`zombie_head`)
- **Decoração:** `dead_bush` ao redor, `orange_terracotta` base, `soul_torch` × 4

#### Totem Gélido (Sentinela Gélida)
- **Bioma:** Snowy Peaks / Ice Spikes
- **Materiais:** `packed_ice` + `blue_ice` + `prismarine` detalhes
- **Forma:** Coluna de gelo (h=8) com cristal de `sea_lantern` no topo
- **Decoração:** `snow_block` base, `powder_snow` ao redor, `soul_lantern` × 4

#### Totem da Selva (Aranha da Selva)
- **Bioma:** Jungle
- **Materiais:** `mossy_stone_bricks` + `mossy_cobblestone` + `dark_oak_log`
- **Forma:** Ruína coberta de vegetação (h=7) com teia no topo (`cobweb`)
- **Decoração:** `vine` cobrindo, `jungle_leaves`, `glow_lichen`, `mushroom` na base

#### Totem do Nether (Fantasma do Nether)
- **Bioma:** Nether Wastes (ou perto de portal no Overworld)
- **Materiais:** `nether_bricks` + `blackstone` + `soul_sand`
- **Forma:** Altar sombrio (h=6) com `soul_fire` no topo e `wither_skeleton_skull`
- **Decoração:** `nether_wart` na base, `chain` penduradas, `crimson_fungus`

### Cada Totem deve ter
- 1 `lectern` com espaço para livro de lore
- 2-4 `sign` com texto placeholder (será preenchido manualmente)
- Iluminação temática (`soul_torch` ou `soul_lantern`)
- Aparência de estrutura **antiga e desgastada** (cracked/mossy variants)

---

## 🛤 GUIA MANUAL — Estrada Spawn → Valheim

> **⚠️ Esta estrada NÃO é gerada via schematic** porque precisa se adaptar ao terreno natural.
> Deve ser construída manualmente no servidor usando WorldEdit brushes.

### Blueprint Visual
![Blueprint da estrada](c:\Users\Gorska\Desktop\gorvax-plugin\tools\road_blueprint.png)

### Diagramas Bloco-por-Bloco (cada caractere = 1 bloco)

#### Estrada — Vista de Cima (seção de 15 blocos)
```
Legenda: C=cobblestone  P=dirt_path  .=grama  L=poste  F=flower_pot

  . . C P P P C . .    ← Largura: 5 blocos (1C + 3P + 1C)
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . L C P P P C . .    ← Poste no lado esquerdo
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C . .
  . . C P P P C L .    ← Poste no lado direito (alternado)
```

#### Poste de Luz — Vista Frontal (1×5 blocos)
```
  [lantern ]   ← Y+4: lantern (pendurada)
  [chain   ]   ← Y+3: chain
  [dk_fence]   ← Y+2: dark_oak_fence
  [dk_fence]   ← Y+1: dark_oak_fence
  [dk_fence]   ← Y+0: dark_oak_fence (no chão)
```

#### Área de Descanso — Vista de Cima (9×9 blocos, quadrada)
```
Legenda: A=polished_andesite  S=smooth_stone  W=water  B=banco(oak_stairs)
         Z=azalea  L=poste  ■=stone_brick

  A A A A A A A A A
  A S S S S S S S A
  A S Z S L S Z S A
  A S S S B S S S A    ← banco (stairs viradas pro centro)
  A S L B W B L S A    ← W=fonte (1 bloco de água)
  A S S S B S S S A    ← banco (stairs viradas pro centro)
  A S Z S L S Z S A
  A S S S S S S S A
  A A A A A A A A A
```

#### Arco de Valheim — Vista Frontal (7 largura × 8 altura)
```
Legenda: ■=stone_bricks  ◣◢=stone_brick_stairs  ▓=banner  s=sign  .=ar

  . . . s . . .    ← Y+7: sign "§6⚔ Valheim"
  . . ◣ ■ ◢ . .    ← Y+6: arco (stairs + bloco + stairs)
  ■ . . . . . ■    ← Y+5: pilar
  ■ ▓ . . . ▓ ■    ← Y+4: pilar + banner
  ■ . . . . . ■    ← Y+3: pilar
  ■ . . . . . ■    ← Y+2: pilar
  ■ . . . . . ■    ← Y+1: pilar
  ■ C P P P C ■    ← Y+0: base (estrada passa por baixo)
```

### Especificações
- **Largura:** 5 blocos (1 `cobblestone` + 3 `dirt_path` + 1 `cobblestone`)
- **Comprimento:** ~500 blocos (de 0,0 até 0,500)
- **Direção:** Norte → Sul (eixo Z positivo)
- **Postes:** A cada ~15 blocos, alternando lados (esquerda/direita)
- **Áreas de descanso:** 2-3 ao longo do caminho (quadradas 9×9, sem elementos redondos)
- **Arco:** 1 no ponto médio (~250 blocos)

### Como Construir

#### 1. Traçar o caminho
```
# Vá para o portão sul do spawn e comece em direção ao sul
# Use F3 para monitorar posição Z (deve ir de ~60 até ~440)

# Opção A: Brush (mais rápido)
//br sphere grass_path 2
# Voe pela rota desejada clicando para "pintar" o chão

# Opção B: Manual (mais bonito)
# Coloque dirt_path (3 centro) + cobblestone (bordas) bloco a bloco
```

#### 2. Bordas de cobblestone
```
//br sphere cobblestone 1
# Passe pelas bordas do caminho
```

#### 3. Postes de iluminação (a cada ~15 blocos)
```
# Segue o diagrama acima: 3× dark_oak_fence + 1× chain + 1× lantern
# Alternar lados: esquerda, direita, esquerda...
```

#### 4. Áreas de descanso (2-3 ao longo do caminho)
```
# Segue o diagrama 9×9 acima
# Piso polished_andesite, bancos oak_stairs viradas, água no centro
```

#### 5. Arco de Valheim (ponto médio, ~250 blocos)
```
# Segue o diagrama 7×8 acima
# 2 pilares stone_bricks com arco e sign "§6⚔ Valheim"
```

#### 6. Adaptação ao terreno
```
# Em subidas:  Use stone_brick_stairs como degraus
# Em descidas: Use stone_brick_stairs invertidas
# Em água:     Ponte de oak_planks + oak_fence como corrimão
# Em ravinas:  Ponte de stone_bricks + cobblestone_wall como corrimão
```

### Mini-Schematics de apoio (gerados no /build)
O script pode gerar estas peças separadas para colar ao longo da estrada:
- `gorvax_lamppost.schem` — Poste com lanterna (3×5×3)
- `gorvax_road_arch.schem` — Arco decorativo (7×8×3)
- `gorvax_rest_area.schem` — Área de descanso (9×5×9)

---

## BATCH 6 — Ruínas de Ashvale (Dungeon Vulcânica)
- **Arquivo de saída:** `tools/output/gorvax_ashvale.schem`
- [x] Gerar estrutura

### Specs
- **Dimensões:** 100×50×80 blocos
- **Tema:** Dungeon vulcânica, ruínas de uma fortaleza de ferreiros
- **Lore:** O Domínio de Ashvale — reino de ferreiros guerreiros que forjavam armas para combater Gorvax. A Lâmina de Gorvax foi forjada aqui antes de ser corrompida.
- **Posição:** Perto das montanhas vulcânicas, base da montanha principal (~200, 60, 400)

### Layout
```
                N (Sala do Trono)
    ┌──────────────────────────────┐
    │  Torre NW          Torre NE  │
    │                               │
    │   ┌── SALA DO TRONO ────┐    │
    │   │ Trono do Rei        │    │
    │   │ Ferreiro (36×17)    │    │
    │   │ Pilares + Banners   │    │
    │   └─────────────────────┘    │
    │                               │
    │   ┌─ CÂMARA ─┐  ┌─ CÂMARA ─┐│
    │   │ Mortos   │  │ Pestilên.││
    │   │ (skeltn) │  │ (zombie) ││
    │   └──────────┘  └──────────┘│
    │                               │
    │ ┌── GRANDE ──┐  ┌─ ARSENAL ─┐│
    │ │   FORJA    │  │ Armaduras ││
    │ │ 4 estações │  │ + Armas   ││
    │ │ + fornalha │  │ (30×20)   ││
    │ │ (30×30)    │  └───────────┘│
    │ └────────────┘               │
    │          CORREDOR N-S         │
    │                               │
    │   ┌── CÂMARA DAS SOMBRAS ──┐ │
    │   │ (wither_skeleton)      │ │
    │   └────────────────────────┘ │
    │                               │
    │   ┌── HALL DE ENTRADA ─────┐ │
    │   │ Arco triplo + Lore     │ │
    │   │ Banners + Armor Stands │ │
    │   └────────────────────────┘ │
    │  Torre SW          Torre SE  │
    └──────────────────────────────┘
                S (Entrada)
```

### Elementos Obrigatórios
1. **Muralhas externas**: `deepslate_bricks` + variações, h=12, parcialmente destruídas com dano crescente em altura
2. **4 Torres**: Circulares raio 4, h=14-18, parcialmente em ruínas com chains penduradas
3. **Hall de Entrada** (24×13): Arco triplo `polished_deepslate`, mural de lore (3 signs), armor_stands, banners vermelho/cinza
4. **A Grande Forja** (30×30): 4 estações de forja (`blast_furnace` + `anvil` + `smithing_table`), fornalha central circular com fogo, canais de lava laterais, `lectern` de lore
5. **Arsenal** (30×20): 6 armor_stands, item_frames, `grindstone` + `smithing_table`, barrils de suprimentos
6. **3 Câmaras de Spawners**: Skeleton (17×15), Zombie (20×15), Wither Skeleton (24×15) — cada uma com spawner, soul_sand, chests, cobwebs
7. **Sala do Trono** (36×17, h=14): Plataforma elevada (+3), trono de `polished_blackstone_stairs`, encosto com `wither_skeleton_skull`, 8 pilares laterais, carpete vermelho, banners de Ashvale, lectern de lore, chests reais
8. **Corredores**: Principal N-S (5 blocos largura), laterais E-W conectando às salas
9. **Terreno vulcânico**: `blackstone` + `basalt` + `tuff`, lava pools exteriores, campfires para fumaça
10. **Detalhes atmosféricos**: `vine`, `hanging_roots`, `glow_lichen`, `cobweb`, chains, candles, skulls, dead_bush

### Paleta de Blocos
| Uso | Blocos |
|-----|--------|
| Estrutural | `deepslate_bricks`, `cracked_deepslate_bricks`, `deepslate_tiles`, `polished_deepslate` |
| Forja | `blackstone`, `polished_blackstone`, `polished_blackstone_bricks`, `magma_block` |
| Trono | `polished_blackstone`, `polished_blackstone_stairs`, `red_carpet`, `red_banner` |
| Spawners | `soul_sand`, `soul_torch`, `cobweb`, `spawner` |
| Vulcânico | `basalt`, `smooth_basalt`, `tuff`, `lava`, `obsidian`, `netherrack` |
| Decoração | `chain`, `soul_lantern`, `lantern`, `vine`, `glow_lichen`, `armor_stand` |

---

## 🔮 Batches Futuros (Post-Launch)

## BATCH 7 — Monastério de Glacius (Dungeon Gelada)
- **Arquivo de saída:** `tools/output/gorvax_glacius.schem`
- [x] Gerar estrutura

### Specs
- **Dimensões:** 120×55×100 blocos
- **Tema:** Monastério congelado, tumba gelada, parkour e puzzles
- **Lore:** Monges que estudavam o frio absoluto de Kaldur, tentando canalizar seu poder para o bem. O monastério congelou completamente quando Kaldur despertou, matando todos dentro. Hoje é uma tumba gelada com segredos sobre magia de gelo.
- **Posição:** Picos gelados, longe de Valheim (~-300, 90, -200) — bioma Snowy Peaks

### Layout
```
                N (Entrada — Portão Gelado)
    ┌──────────────────────────────────┐
    │  Torre NW              Torre NE  │
    │          CLAUSTRO EXTERNO        │
    │   ┌───────────────────────┐      │
    │   │   JARDIM CONGELADO    │      │
    │   │ (fonte congelada      │      │
    │   │  estátuas de gelo)    │      │
    │   └───────────────────────┘      │
    │                                   │
    │   ┌── BIBLIOTECA DOS MONGES ──┐  │
    │   │ Lecterns + Bookshelves    │  │
    │   │ Lore do Kaldur (25×15)   │  │
    │   └──────────────────────────┘  │
    │                                   │
    │   ┌── SALÃO DE MEDITAÇÃO ────┐  │
    │   │ Altar central de gelo    │  │
    │   │ Pilares de packed_ice    │  │
    │   │ (30×20, h=15)            │  │
    │   └──────────────────────────┘  │
    │                                   │
    │   ┌─ CÂMARA ──┐  ┌─ CÂMARA ──┐  │
    │   │ Parkour 1 │  │ Parkour 2 │  │
    │   │ (gelo)    │  │ (vento)   │  │
    │   └───────────┘  └───────────┘  │
    │                                   │
    │   ┌── CRIPTA DO ABADE ────────┐  │
    │   │ Sarcófago + relíquias     │  │
    │   │ Lore final + baú (25×15) │  │
    │   └──────────────────────────┘  │
    │                                   │
    │   ┌── CÂMARA DE KALDUR ──────┐  │
    │   │ Arena do mini-boss       │  │
    │   │ Gelo + neve + spawner    │  │
    │   │ (30×30, h=18)            │  │
    │   └──────────────────────────┘  │
    │  Torre SW              Torre SE  │
    └──────────────────────────────────┘
                S (Saída Secreta)
```

### Elementos Obrigatórios
1. **Muralhas externas**: `stone_bricks` + `prismarine_bricks` cobertas de `snow` e `ice`, h=10 + merlões de `packed_ice`
2. **4 Torres**: Circulares raio 4, h=16, telhado cônico `spruce_stairs`, cobertas de neve
3. **Portão Norte (Entrada)**: Arco triplo `polished_diorite` + `packed_ice` pilares, `ice` detalhes, portão de `iron_bars` congelado
4. **Claustro Externo** (perímetro interno): Corredor com arcadas de `stone_brick_stairs`, `lantern` a cada 4 blocos, piso `polished_diorite`
5. **Jardim Congelado** (centro, 20×15): Fonte circular raio 4 de `packed_ice` (água congelada), árvores mortas de `stripped_spruce_log` + `spruce_leaves`, `powder_snow` no chão, `blue_ice` esculturas
6. **Biblioteca dos Monges** (NE, 25×15×8): `spruce_planks` piso, `bookshelf` paredes, `lectern`×4 com espaço para lore sobre Kaldur, `enchanting_table`, `candle` iluminação
7. **Salão de Meditação** (centro, 30×20, h=15): Altar central de `blue_ice` + `sea_lantern`, 8 pilares de `packed_ice` + `prismarine`, tapete de `light_blue_carpet`, `soul_lantern` iluminação atmosférica
8. **2 Câmaras de Parkour** (15×20 cada): Plataformas de `ice` + `packed_ice` em alturas variadas, `powder_snow` armadilhas, `blue_ice` escorregadio, recompensa no topo (chest)
9. **Cripta do Abade** (25×15, h=8): Sarcófago de `polished_deepslate` + `wither_skeleton_skull`, 4 `armor_stand` monges, `soul_torch` iluminação, `lectern` com lore final, baú de relíquias
10. **Câmara de Kaldur** (30×30, h=18): Arena circular com piso de `blue_ice` + `packed_ice`, coluna de gelo central h=15, spawner de stray, `snow_block` + `powder_snow` bordas, `chains` + `soul_lantern` do teto, sinais de lore do Kaldur
11. **Piso geral**: `stone_bricks` + `cracked_stone_bricks` cobertos de `snow` parcial, `ice` em áreas expostas
12. **Iluminação**: `soul_lantern` (fria), `lantern` interiores, `sea_lantern` detalhes, `candle` bibliotecas
13. **Vegetação gelada**: `spruce_leaves`, `azalea`, `dead_bush`, `sweet_berry_bush` congeladas, `powder_snow` nas bordas
14. **Detalhes atmosféricos**: `cobweb` como geada, `glass_pane` como gelo fino, `hanging_roots` como estalactites, `dripstone` nos tetos, `snow` camada em toda superfície exposta

### Paleta de Blocos
| Uso | Blocos |
|-----|--------|
| Estrutural | `stone_bricks`, `cracked_stone_bricks`, `mossy_stone_bricks`, `polished_diorite` |
| Gelo | `ice`, `packed_ice`, `blue_ice`, `snow_block`, `powder_snow` |
| Detalhes | `prismarine`, `prismarine_bricks`, `dark_prismarine`, `sea_lantern` |
| Madeira | `spruce_planks`, `spruce_log`, `stripped_spruce_log`, `spruce_stairs` |
| Decoração | `bookshelf`, `lectern`, `enchanting_table`, `candle`, `armor_stand` |
| Iluminação | `soul_lantern`, `soul_torch`, `sea_lantern`, `lantern`, `candle` |
| Atmosfera | `cobweb`, `snow`, `powder_snow`, `dead_bush`, `dripstone_block` |
## BATCH 8 — Biblioteca Dimensional de Étheris (End/Overworld)
- **Arquivo de saída:** `tools/output/gorvax_etheris.schem`
- [x] Gerar estrutura

### Specs
- **Dimensões:** 140×65×120 blocos
- **Tema:** Biblioteca dimensional, arcana, entre Overworld e End
- **Lore:** Sábios que tentaram entender Xylos e o Vazio. Construíram uma biblioteca dimensional que existe parcialmente no Overworld e parcialmente no End. Seus livros contêm segredos sobre todos os bosses — mas estão em ruínas e espalhados.
- **Posição:** Zona isolada, entre biomas (~-200, 70, 400) — transição dimensional visível

### Layout
```
                N (Portão Arcano — Entrada)
    ┌────────────────────────────────────┐
    │  Torre NW                Torre NE  │
    │          CLAUSTRO DOS SÁBIOS       │
    │   ┌───────────────────────────┐    │
    │   │     ÁTRIO DIMENSIONAL     │    │
    │   │ Piso xadrez end/overworld │    │
    │   │ Portal flutuante central  │    │
    │   │ (30×25, h=20)             │    │
    │   └───────────────────────────┘    │
    │                                     │
    │   ┌── ALA OVERWORLD ──────────┐    │
    │   │ Sala dos Elementos (W)    │    │
    │   │ Estantes + lecterns       │    │
    │   │ Lore de Gorvax/Ashvale    │    │
    │   │ (25×20)                   │    │
    │   └───────────────────────────┘    │
    │                                     │
    │   ┌── ALA DO END ─────────────┐    │
    │   │ Sala do Vazio (E)         │    │
    │   │ End stone + purpur        │    │
    │   │ Lore de Indrax/Xylos      │    │
    │   │ (25×20)                   │    │
    │   └───────────────────────────┘    │
    │                                     │
    │   ┌── OBSERVATÓRIO ───────────┐    │
    │   │ Cúpula de vidro (topo)    │    │
    │   │ Mapa estelar no chão      │    │
    │   │ (raio 12, h=18)           │    │
    │   └───────────────────────────┘    │
    │                                     │
    │   ┌── ARQUIVO SELADO ─────────┐    │
    │   │ Sala secreta subterrânea  │    │
    │   │ Spawner + loot + profecia │    │
    │   │ (20×15)                   │    │
    │   └───────────────────────────┘    │
    │                                     │
    │   ┌── CÂMARA DE XYLOS ────────┐    │
    │   │ Arena do mini-boss        │    │
    │   │ End/Void + teleportes     │    │
    │   │ (30×30, h=20)             │    │
    │   └───────────────────────────┘    │
    │  Torre SW                Torre SE  │
    └────────────────────────────────────┘
                S (Saída — Portão do Vazio)
```

### Elementos Obrigatórios
1. **Muralhas externas**: Transição dimensional — metade oeste `stone_bricks` + `mossy_stone_bricks` (Overworld), metade leste `end_stone_bricks` + `purpur_block` (End), h=12, com rachaduras dimensionais de `crying_obsidian`
2. **4 Torres**: Circulares raio 5, h=18. Torres NW/SW em estilo Overworld (`stone_bricks`), Torres NE/SE em estilo End (`end_stone_bricks` + `purpur_pillar`), telhados com `end_rod` no topo
3. **Portão Arcano (N, entrada)**: Arco triplo `chiseled_stone_bricks` + `purpur_pillar`, portal de `crying_obsidian` + `end_gateway` detalhes, `end_rod` iluminação
4. **Átrio Dimensional** (centro, 30×25, h=20): Piso xadrez `polished_blackstone` + `end_stone` (transição visual), coluna central flutuante de `obsidian` + `crying_obsidian` h=15 com `end_crystal` efeito (beacon), 4 `lectern` cardinais com espaço para lore dos bosses, `enchanting_table` em cada canto
5. **Ala Overworld** (W, 25×20×10): Paredes de `bookshelf` + `oak_planks`, `lectern`×6 com lore sobre Gorvax, Ashvale, Skulkor, Vulgathor, piso `spruce_planks`, `lantern` iluminação quente, `flower_pot` com flores, `candle` nos desks
6. **Ala do End** (E, 25×20×10): Paredes de `end_stone_bricks` + `purpur_block`, `bookshelf` com `chorus_flower` decoração, `lectern`×6 com lore sobre Indrax, Xylos, Kaldur, Zar'ith, piso `purpur_block`, `end_rod` iluminação fria, `dragon_head` display
7. **Observatório** (topo, raio 12, h=18): Cúpula semi-esférica de `glass` + `tinted_glass` (céu dimensional), "mapa estelar" no chão com `gold_block` + `diamond_block` + `lapis_block` padrões, `spyglass` em `item_frame`, telescópio de `lightning_rod` + `chain`
8. **Arquivo Selado** (subterrâneo, 20×15, h=8): Sala secreta sob chave, `deepslate_tiles` paredes, `sculk` + `sculk_sensor` ambiente, baú com profecia final, spawner `endermite`, `redstone_lamp` iluminação, `lectern` com lore da profecia dos Pilares
9. **Câmara de Xylos** (S interior, 30×30, h=20): Arena octogonal, piso de `end_stone` + `obsidian` xadrez, 4 plataformas de `purpur_block` flutuantes em alturas diferentes, `chorus_plant` decoração, spawner `enderman`, `end_rod` + `soul_lantern` iluminação, `beacon` central (efeito dimensional)
10. **Corredores**: Principal N-S (5 blocos largura) com transição gradual Overworld→End, laterais E-W conectando as alas
11. **Estantes especiais**: `chiseled_bookshelf` intercalados com `bookshelf` normais nas duas alas
12. **Iluminação dual**: `lantern` + `candle` na Ala Overworld, `end_rod` + `soul_lantern` na Ala do End, `sea_lantern` nos corredores
13. **Detalhes atmosféricos**: `amethyst_cluster` nas rachaduras dimensionais, `chorus_flower` + `chorus_plant` no lado End, `vine` + `moss_block` no lado Overworld, `sculk_vein` no Arquivo Selado
14. **Textos em PT-BR**: Signs com lore sobre todos os bosses maiores e menores, profecia dos Pilares no Arquivo Selado

### Paleta de Blocos
| Uso | Blocos |
|-----|--------|
| Overworld | `stone_bricks`, `mossy_stone_bricks`, `oak_planks`, `spruce_planks`, `bookshelf` |
| End | `end_stone_bricks`, `purpur_block`, `purpur_pillar`, `chorus_plant`, `end_rod` |
| Dimensional | `obsidian`, `crying_obsidian`, `amethyst_block`, `amethyst_cluster`, `tinted_glass` |
| Observatório | `glass`, `tinted_glass`, `gold_block`, `diamond_block`, `lapis_block`, `lightning_rod` |
| Arquivo | `deepslate_tiles`, `sculk`, `sculk_sensor`, `sculk_vein`, `redstone_lamp` |
| Iluminação | `lantern`, `candle`, `end_rod`, `soul_lantern`, `sea_lantern` |
| Decoração | `lectern`, `enchanting_table`, `bookshelf`, `chiseled_bookshelf`, `flower_pot` |

---



