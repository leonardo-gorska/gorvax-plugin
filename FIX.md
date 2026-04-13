# 🔧 FIX — Resource Pack & Custom Items Fix Roadmap

> Plano completo para tornar itens customizados, bosses e texturas 100% funcionais em **Java** e **Bedrock**.

---

## 📊 Status Geral

| Batch | Descrição | Status |
|-------|-----------|--------|
| 1 | Java Pack: Validar modelos 3D + verificar SHA1 do server.properties | ✅ |
| 2 | Bedrock Attachables: Criar arquivos de geometria para itens de mão (espada, arco, machado, tridente, escudo) | ✅ |
| 3 | Bedrock Attachables: Criar arquivos de geometria para armaduras (elmos, botas, peitoral) | ✅ |
| 4 | Geyser Mappings: Ajustar render_offsets + wearable componentes para armaduras | ✅ |
| 5 | Rebuild, deploy e teste final de todos os 9 itens em ambas plataformas | ✅ |

---

## 🎯 Inventário de Itens (9 itens lendários)

| # | Item | Material Base | CMD | Tipo | Precisa Attachable? |
|---|------|--------------|-----|------|---------------------|
| 1 | ⚔ Lâmina de Gorvax | `NETHERITE_SWORD` | 1001 | Arma de mão | Sim (handheld) |
| 2 | 👑 Coroa de Indrax | `NETHERITE_HELMET` | 1001 | Armadura (head) | Sim (helmet attach) |
| 3 | 💨 Botas Velocistas | `DIAMOND_BOOTS` | 1001 | Armadura (feet) | Sim (boots attach) |
| 4 | 🏹 Arco do Caçador | `BOW` | 1001 | Arma de mão | Sim (bow attach) |
| 5 | 🛡 Escudo do Guardião | `SHIELD` | 1001 | Off-hand | Sim (shield attach) |
| 6 | 🔥 Manto de Vulgathor | `NETHERITE_CHESTPLATE` | 1002 | Armadura (chest) | Sim (chest attach) |
| 7 | 🧊 Lança de Kaldur | `TRIDENT` | 1002 | Arma de mão | Sim (handheld) |
| 8 | 💀 Elmo de Skulkor | `NETHERITE_HELMET` | 1002 | Armadura (head) | Sim (helmet attach) |
| 9 | 🌀 Pérola de Xylos | `NETHERITE_AXE` | 1002 | Arma de mão | Sim (handheld) |

---

## BATCH 1 — Java Pack: Validação e SHA1 ✅

### Problemas Identificados
- ✅ Modelos 3D `.json` existem para todos os 9 itens (Voxel com `elements`)
- ✅ Override JSONs existem para todos os 8 materiais base
- ✅ Texturas PNG 64x64 existem para todos os 9 itens
- ✅ `pack_format: 34` correto para 1.21+
- ⚠️ SHA1 em `server.properties` pode estar desatualizado
- ⚠️ ZIP no GitHub pode estar desatualizado (não inclui itens novos: vulgathor, kaldur, skulkor, xylos)

### Tarefas
1. [x] Verificar se os 4 novos itens (CMD 1002) estão no ZIP do GitHub
2. [x] Reconstruir `GorvaxCore-ResourcePack-Java.zip` com todos os arquivos
3. [x] Calcular SHA1 do novo ZIP
4. [x] Atualizar `server.properties` com novo SHA1
5. [x] Validar que cada override JSON aponta para o modelo correto

### Arquivos Envolvidos
- `resourcepack/java/` (todo o diretório)
- `server.properties` (linhas 59, 62)
- `pack.bat` / `pack.sh` (scripts de empacotamento)

---

## BATCH 2 — Bedrock Attachables: Armas de Mão ✅

### O que são Attachables?
Attachables são definições JSON Bedrock que dizem ao jogo como renderizar um item customizado quando o jogador o segura ou equipa. Sem eles, itens Geyser aparecem como ícones flat 2D.

### Itens deste Batch
| Item | Identifier Geyser | Geometria |
|------|-------------------|-----------|
| Lâmina de Gorvax | `geyser_custom:gorvax_blade` | `geometry.gorvax_blade` |
| Arco do Caçador | `geyser_custom:hunter_bow` | `geometry.hunter_bow` |
| Lança de Kaldur | `geyser_custom:kaldur_lance` | `geometry.kaldur_lance` |
| Pérola de Xylos | `geyser_custom:xylos_pearl` | `geometry.xylos_pearl` |
| Escudo do Guardião | `geyser_custom:guardian_shield` | `geometry.guardian_shield` |

### Tarefas
1. [x] Criar `resourcepack/bedrock/attachables/gorvax_blade.json`
2. [x] Criar `resourcepack/bedrock/attachables/hunter_bow.json`
3. [x] Criar `resourcepack/bedrock/attachables/kaldur_lance.json`
4. [x] Criar `resourcepack/bedrock/attachables/xylos_pearl.json`
5. [x] Criar `resourcepack/bedrock/attachables/guardian_shield.json`

### Estrutura de cada Attachable (arma de mão)
```json
{
  "format_version": "1.10.0",
  "minecraft:attachable": {
    "description": {
      "identifier": "geyser_custom:<item_id>",
      "materials": { "default": "entity_alphatest", "enchanted": "entity_alphatest_glint" },
      "textures": { "default": "textures/items/<item_id>" },
      "geometry": { "default": "geometry.geyser_custom.<item_id>" },
      "render_controllers": ["controller.render.item_default"],
      "scripts": { "pre_animation": ["v.is_first_person = c.is_first_person;"] },
      "animations": { "wield": "animation.item_default.wield" }
    }
  }
}
```

### Arquivos a Criar
- `resourcepack/bedrock/attachables/` (novo diretório — 5 arquivos)

---

## BATCH 3 — Bedrock Attachables: Armaduras ✅

### Itens deste Batch
| Item | Identifier Geyser | Slot | Geometria |
|------|-------------------|------|-----------|
| Coroa de Indrax | `geyser_custom:indrax_crown` | head | `geometry.indrax_crown` |
| Elmo de Skulkor | `geyser_custom:skulkor_helm` | head | `geometry.skulkor_helm` |
| Botas Velocistas | `geyser_custom:speed_boots` | feet | `geometry.speed_boots` |
| Manto de Vulgathor | `geyser_custom:vulgathor_mantle` | chest | `geometry.vulgathor_mantle` |

### Tarefas
1. [x] Criar `resourcepack/bedrock/attachables/indrax_crown.json`
2. [x] Criar `resourcepack/bedrock/attachables/skulkor_helm.json`
3. [x] Criar `resourcepack/bedrock/attachables/speed_boots.json`
4. [x] Criar `resourcepack/bedrock/attachables/vulgathor_mantle.json`

### Estrutura de cada Attachable (armadura)
```json
{
  "format_version": "1.10.0",
  "minecraft:attachable": {
    "description": {
      "identifier": "geyser_custom:<item_id>",
      "materials": { "default": "armor", "enchanted": "armor_enchanted" },
      "textures": { "default": "textures/items/<item_id>" },
      "geometry": { "default": "geometry.geyser_custom.<item_id>" },
      "render_controllers": ["controller.render.item_default"],
      "scripts": { "parent_setup": "v.chest_layer_visible = 0.0;" }
    }
  }
}
```

### Arquivos a Criar
- `resourcepack/bedrock/attachables/` (4 arquivos adicionais)

---

## BATCH 4 — Geyser Mappings: Wearable + Render Offsets ✅

### Problemas Atuais
- Itens de armadura mapeados como custom items Geyser **não são equipáveis** como armadura no Bedrock
- Itens de mão aparecem com escala/posição incorreta

### Tarefas
1. [x] Adicionar `bedrock_options.render_offsets` para cada arma de mão no `geyser_mappings.json`
2. [x] Adicionar `components.minecraft:wearable` para armaduras no `geyser_mappings.json`
3. [x] Verificar que `bedrock_options.display_handheld` está correto para armas
4. [x] Testar que armaduras são equipáveis no slot correto no Bedrock

### Modificações em `geyser_mappings.json`
Para cada armadura, adicionar:
```json
"components": {
  "minecraft:wearable": {
    "slot": "slot.armor.head"
  }
}
```

Slots válidos: `slot.armor.head`, `slot.armor.chest`, `slot.armor.legs`, `slot.armor.feet`

### Arquivos Envolvidos
- `resourcepack/geyser_mappings.json`

---

## BATCH 5 — Rebuild, Deploy e Teste Final ✅

### Tarefas
1. [x] Reconstruir `GorvaxCore.mcpack` (Bedrock) com todos os attachables
2. [x] Reconstruir `GorvaxCore-ResourcePack-Java.zip` (Java)
3. [x] Calcular novo SHA1 do Java pack
4. [x] Copiar `.mcpack` para `Geyser-Spigot/packs/`
5. [x] Copiar `geyser_mappings.json` para `Geyser-Spigot/custom_mappings/`
6. [x] Rebuildar `GorvaxCore-2.2-dev-all.jar` e copiar para `plugins/`
7. [x] Atualizar `server.properties` com novo SHA1 (já estava correto)

### Verificação

#### Teste Java (Manual)
1. Entrar no servidor com client Java
2. Aceitar o resource pack quando solicitado
3. Executar `/gorvax give lamina_gorvax` — verificar textura 3D na mão
4. Executar `/gorvax give coroa_indrax` — verificar modelo no inventário
5. Equipar a Coroa — verificar visualização na cabeça
6. Testar os outros 7 itens da mesma forma

#### Teste Bedrock (Manual)
1. Conectar via Bedrock (Geyser)
2. Aceitar download do resource pack
3. Executar `/gorvax give lamina_gorvax` — verificar ícone com textura custom
4. Segurar na mão — verificar que renderiza corretamente (sem fundo quadriculado)
5. Equipar Coroa de Indrax — verificar que aparece na cabeça do personagem
6. Testar armaduras (botas, peitoral, elmos) nos slots corretos
7. Testar `/boss spawn rei_gorvax` — verificar spawn sem erro
8. Testar `/cosmetics` — verificar GUI sem crash

---

## 📁 Estrutura Final Esperada

```
resourcepack/
├── bedrock/
│   ├── manifest.json
│   ├── pack_icon.png
│   ├── attachables/              ← NOVO (Batch 2-3)
│   │   ├── gorvax_blade.json
│   │   ├── hunter_bow.json
│   │   ├── kaldur_lance.json
│   │   ├── xylos_pearl.json
│   │   ├── guardian_shield.json
│   │   ├── indrax_crown.json
│   │   ├── skulkor_helm.json
│   │   ├── speed_boots.json
│   │   └── vulgathor_mantle.json
│   ├── entity/                   (9 boss entity JSONs)
│   ├── render_controllers/
│   ├── textures/
│   │   ├── item_texture.json     ← Já criado
│   │   ├── entity/gorvax_bosses/ (11 texturas)
│   │   └── items/                (9 texturas PNG)
├── java/
│   ├── pack.mcmeta
│   └── assets/
│       ├── gorvax/models/item/   (9 modelos 3D)
│       ├── gorvax/textures/item/ (9 texturas PNG)
│       ├── minecraft/models/item/(8 overrides)
│       └── minecraft/optifine/   (boss CIT configs)
├── geyser_mappings.json          ← Atualizado (Batch 4)
├── pack.bat
└── pack.sh
```
