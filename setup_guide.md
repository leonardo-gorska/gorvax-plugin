# 🗺️ Guia de Setup In-Game — GorvaxMC

> Execute estes comandos em ordem quando ligar o servidor.
> Use `/op SeuNome` no console antes de começar.

---

## 📦 PASSO 1 — Pré-gerar o Mundo

```
/chunky radius 2000
/chunky start
```
> Espere terminar (~5-10 min). Use `/chunky progress` para acompanhar.
> Depois: `/bluemap render` para gerar o mapa web em `http://localhost:8100`

---

## 🏰 PASSO 2 — Colar Schematics

### 2.1 Spawn (centro do mundo)
```
/tp 0 100 0
//schematic load gorvax_spawn
//paste -a
/setworldspawn
```

### 2.2 Cidade Inicial (perto do spawn)
```
/tp 200 100 200
//schematic load gorvax_cidade
//paste -a
```

### 2.3 Arena PvP
```
/tp 0 100 -300
//schematic load gorvax_arena
//paste -a
```

### 2.4 Estrutura de Ashvale (bioma deserto)
```
/locate biome minecraft:desert
```
> Anote as coordenadas X, Z que aparecem no chat, depois:
```
/tp X 100 Z
//schematic load gorvax_ashvale
//paste -a
```

### 2.5 Estrutura de Glacius (bioma neve)
```
/locate biome minecraft:snowy_plains
```
```
/tp X 100 Z
//schematic load gorvax_glacius
//paste -a
```

### 2.6 Estrutura de Étheris (End — ou bioma escuro)
```
/locate biome minecraft:dark_forest
```
```
/tp X 100 Z
//schematic load gorvax_etheris
//paste -a
```

### 2.7 Vulcões
```
/locate biome minecraft:badlands
```
```
/tp X 100 Z
//schematic load gorvax_vulcoes
//paste -a
```

### 2.8 Valheim
```
/locate biome minecraft:old_growth_spruce_taiga
```
```
/tp X 100 Z
//schematic load gorvax_valheim
//paste -a
```

---

## 🗿 PASSO 3 — Colocar Totems de Bioma

Cada totem é um bloco de **LODESTONE** que o jogador clica para ver lore.
Já existe `gorvax_totems.schem` com 4 totems pré-construídos.

### Opção A: Usar a schematic de totem
Para cada bioma, vá até o local e cole o totem:
```
//schematic load gorvax_totems
//paste
```

### Opção B: Colocar manualmente
Coloque 1 LODESTONE em cada bioma e anote as coordenadas.

### Locais dos 4 Totems

**Totem do Deserto (Guardião do Deserto):**
```
/locate biome minecraft:desert
/tp X 100 Z
```
> Coloque LODESTONE. Anote: `mundo, X, Y, Z`

**Totem do Gelo (Sentinela Gélida):**
```
/locate biome minecraft:snowy_plains
/tp X 100 Z
```
> Coloque LODESTONE. Anote: `mundo, X, Y, Z`

**Totem da Selva (Araña da Selva):**
```
/locate biome minecraft:jungle
/tp X 100 Z
```
> Coloque LODESTONE. Anote: `mundo, X, Y, Z`

**Totem do Nether (Fantasma do Nether):**
```
/locate biome minecraft:nether_wastes
/tp X 100 Z
```
> ⚠ Este fica no NETHER! Ou coloque no overworld perto de um portal.
> Coloque LODESTONE. Anote: `mundo, X, Y, Z`

---

## 📚 PASSO 4 — Colocar Estantes de Lore (no Spawn)

Coloque **7 blocos de BOOKSHELF** no spawn (tipicamente numa biblioteca ou sala de leitura).
Cada estante dá um livro de lore diferente ao clicar.

Sugestão: coloque as 7 estantes em fileira ou em círculo na estrutura do spawn.

Para cada estante colocada, anote as coordenadas `X, Y, Z`.

Os livros são:
1. 📕 Gênesis do Mundo
2. 📗 A Ascensão do Rei
3. 📘 A Queda de Indrax
4. 📙 As Bestas do Mundo
5. 📒 Os Reinos Perdidos
6. 📓 A Profecia dos Pilares
7. 📔 Os Guardiões dos Biomas

---

## 🔥 PASSO 5 — Colocar Crates Físicas (no Spawn)

Coloque **4 blocos de ENDER_CHEST** no spawn em locais visíveis.
Cada um abre um tipo diferente de crate.

Sugestão de posicionamento:
- 1 na entrada principal do spawn
- 1 na praça central
- 1 perto da arena
- 1 na área VIP

Para cada ender chest colocada, anote as coordenadas `X, Y, Z`.

---

## 📋 PASSO 6 — Me Passe as Coordenadas!

Depois de tudo colocado, me envie uma lista com:

```
ESTANTES:
1. Gênesis:    X, Y, Z
2. Ascensão:   X, Y, Z
3. Indrax:     X, Y, Z
4. Bestas:     X, Y, Z
5. Reinos:     X, Y, Z
6. Profecia:   X, Y, Z
7. Guardiões:  X, Y, Z

TOTEMS:
1. Deserto:    mundo, X, Y, Z
2. Gelo:       mundo, X, Y, Z
3. Selva:      mundo, X, Y, Z
4. Nether:     mundo, X, Y, Z

CRATES FÍSICAS:
1. Crate 1:    X, Y, Z (tipo: ?)
2. Crate 2:    X, Y, Z (tipo: ?)
3. Crate 3:    X, Y, Z (tipo: ?)
4. Crate 4:    X, Y, Z (tipo: ?)
```

Eu atualizo o `lore_books.yml` e `crates.yml` com as coordenadas reais!

---

## 🔒 PASSO 7 — Proteções

```
# Proteger spawn (WorldGuard)
//wand
# Selecione dois cantos opostos do spawn
/rg define spawn
/rg flag spawn pvp deny
/rg flag spawn build deny
/rg flag spawn mob-spawning deny
/rg flag spawn creeper-explosion deny

# Proteger arena se necessário
/rg define arena
/rg flag arena pvp allow
```

---

## ✅ PASSO 8 — Verificação Final

```
/bluemap render
```
> Abra `http://localhost:8100` e verifique se tudo aparece no mapa.

```
/gorvax reload
```
> Recarrega o GorvaxCore com as novas configs.
