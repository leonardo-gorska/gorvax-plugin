# 🎨 PROMPTS DE IMAGENS — E-Book GorvaxMC

> **Instruções para a IA**: Leia este arquivo completamente. Encontre o próximo prompt com status `[ ]` (pendente).
> Execute **APENAS UM prompt** por execução do `/img`.
>
> **⚠️ REGRA OBRIGATÓRIA — LIMITE DE 3 IMAGENS POR CONVERSA:**
> 1. Gere **exatamente 3 variações** da mesma imagem usando `generate_image` (mesmo prompt, 3 chamadas)
> 2. Visualize as 3 imagens geradas
> 3. **Escolha a melhor** das 3 (a mais bonita, mais fiel ao estilo, melhor composição)
> 4. Salve **apenas a vencedora** na pasta `Imagens Ebook/` com o nome indicado
> 5. **PARE AQUI** — NÃO gere mais imagens, NÃO avance para o próximo prompt
> 6. Integre a imagem vencedora no `ebook_jogador.html` no local indicado
> 7. Marque como `[x]`, atualize progresso, e notifique o usuário
>
> **🚫 NUNCA gere mais de 3 imagens por conversa. Isso é um hard limit para preservar tokens.**

---

## 📊 Progresso

| Campo                     | Valor                |
|---------------------------|----------------------|
| Total de prompts          | 21                   |
| Concluídos                | 17 / 21              |
| Último prompt executado   | IMG-17               |
| Data da última execução   | 2026-02-25           |
| Próximo prompt a executar | **IMG-18**           |

---

## 🎨 Estilo Padrão (aplicar a TODOS os prompts)

- **Estilo**: Minecraft voxel render style, epic fantasy lighting, dramatic composition
- **Qualidade**: Highly detailed, 4K quality, professional game art
- **Tom**: Dark medieval fantasy with vibrant magical effects
- **NÃO incluir**: Texto/letras na imagem (exceto quando indicado), molduras, bordas, logos
- **Resolução**: Square 1:1

---

## 📋 Lista de Prompts

### 🏰 CAPA & BANNERS DE SEÇÃO

---

#### IMG-01 — Capa do Ebook (Cover Art)
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → `<div class="cover" id="top">` (antes do h1)
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `cover_gorvaxmc.png`

**Prompt**:
> A breathtaking Minecraft-style epic landscape artwork for a medieval RPG game server called GorvaxMC. Show a dark, imposing medieval castle with obsidian towers and purple glowing windows perched on a volcanic cliff edge. In the foreground, a lone Minecraft knight character in netherite armor holds a glowing enchanted sword, silhouetted against a dramatic crimson and gold sunset. The sky has auroral purple lights and floating End-like particles. Lava flows in the canyon below. Dark clouds with lightning in the distance. Minecraft voxel art style but with cinematic epic fantasy lighting. No text or letters anywhere in the image.

---

#### IMG-02 — Banner Reinos & Nações
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s3 (Reinos), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_reinos.png`

**Prompt**:
> A panoramic Minecraft voxel art scene showing a grand medieval kingdom. Three distinct castle fortresses with different colored banners (gold, blue, and crimson) are built on hills connected by stone bridges over a river. Minecraft villager-like players in royal armor stand on ramparts. The scene has warm sunset lighting with dramatic clouds. Flags and banners wave in the wind. Pathways with glowstone lanterns connect the kingdoms. Epic fantasy medieval atmosphere with Minecraft block aesthetic. No text.

---

#### IMG-03 — Banner Guerra entre Reinos
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s6 (Guerra), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_guerra.png`

**Prompt**:
> An epic Minecraft voxel art battle scene between two armies. Two lines of Minecraft knight characters in diamond and netherite armor charge at each other across a burning battlefield. TNT explosions and fire arrows fill the sky. Castle walls crumble in the background. One side carries golden banners, the other crimson. Dramatic dark stormy sky with lightning. Debris and blocks scatter from impacts. War drums and chaos atmosphere. Minecraft block style with cinematic action lighting. No text.

---

#### IMG-04 — Banner Mercado & Economia
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s7 (Mercado Global), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_mercado.png`

**Prompt**:
> A vibrant Minecraft voxel art medieval marketplace bustling with activity. Wooden market stalls with colorful awnings display diamond blocks, emeralds, golden apples, and enchanted items. Minecraft villager and player characters trade goods. Chest shops line the cobblestone street. Hanging lanterns cast warm golden light. Signs show prices. A grand trading hall with arched windows rises in the background. Warm evening atmosphere with fireflies. Minecraft block aesthetic with rich detail. No text.

---

#### IMG-05 — Banner Sistema de Combate
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s13 (Sistema de Combate), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_combate.png`

**Prompt**:
> An intense Minecraft voxel art PvP combat scene. Two Minecraft players in full netherite armor clash swords in a dramatic arena with obsidian pillars and lava moats. Enchantment glints sparkle on their weapons. Critical hit particles and sweeping edge effects fly through the air. A kill streak counter effect glows above one player (just particle effects, no text). The arena has dark stone walls with redstone torch lighting. Epic close-quarters combat with dynamic poses. Minecraft style with dramatic action lighting. No text.

---

#### IMG-06 — Banner Battle Pass
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s28 (Battle Pass), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_battlepass.png`

**Prompt**:
> A Minecraft voxel art progression reward showcase. A golden staircase of 30 ascending steps made of different blocks (wood, stone, iron, gold, diamond, netherite) leads upward toward a glowing treasure at the top. Each step has a small floating reward icon above it (diamonds, swords, keys, armor pieces). The background has dark purple velvet curtains with golden trim, like a royal reward ceremony. Sparkle and enchantment particles float everywhere. Premium golden glow on the upper half. Epic achievement atmosphere. Minecraft block style. No text.

---

#### IMG-07 — Banner Conquistas
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s15 (Conquistas), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_conquistas.png`

**Prompt**:
> A Minecraft voxel art achievement hall of fame. A grand dark stone hall with golden trophy pedestals displaying various Minecraft achievements: a diamond sword, a dragon egg, a beacon, a nether star, and enchanted armor. Golden achievement frames float in the air with sparkle particles. The hall has tall obsidian pillars with purple crystal chandeliers. A red carpet leads to the main trophy. Dramatic spotlight lighting from above. Museum/hall of fame atmosphere. Minecraft block style. No text.

---

### 📦 CRATES INDIVIDUAIS

---

#### IMG-08 — Crate Comum
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s25, dentro do `<div class="crate-card common">`
- **Classe CSS**: `showcase-image` com `style="max-height:160px"`
- **Nome do arquivo**: `crate_comum.png`

**Prompt**:
> A single Minecraft wooden chest (oak chest) with a white/silver glow effect. The chest is slightly open with a soft white light emanating from inside. Simple iron coins, basic diamonds, and golden apples float gently above it. Subtle white sparkle particles surround it. Plain dark background with a slight stone floor. Clean and simple design representing a common-tier reward crate. Minecraft voxel block art style. No text.

---

#### IMG-09 — Crate Raro
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s25, dentro do `<div class="crate-card rare">`
- **Classe CSS**: `showcase-image` com `style="max-height:160px"`
- **Nome do arquivo**: `crate_raro.png`

**Prompt**:
> A single Minecraft ender chest with a vibrant blue glow effect. The chest is opening with electric blue energy swirling out. Netherite ingots, an elytra, enchanted golden apples, and rare keys float above it with blue sparkle trails. Blue lightning bolts arc between the items. Dark background with deep blue particle fog. Mid-tier rarity design with magical blue theme. Minecraft voxel block art style. No text.

---

#### IMG-10 — Crate Lendário
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s25, dentro do `<div class="crate-card legendary">`
- **Classe CSS**: `showcase-image` com `style="max-height:160px"`
- **Nome do arquivo**: `crate_lendario.png`

**Prompt**:
> A single ornate golden Minecraft chest with brilliant golden and amber glow effects. The chest bursts open with golden energy beams shooting upward. Legendary items float above it: a glowing netherite sword with fire aspect, enchanted armor pieces, a nether star, and golden keys. Swirling golden vortex of particles. Dark background with golden ambient light. Premium legendary tier design with divine radiance. Minecraft voxel block art style. No text.

---

#### IMG-11 — Crate Sazonal
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s25, dentro do `<div class="crate-card seasonal">`
- **Classe CSS**: `showcase-image` com `style="max-height:160px"`
- **Nome do arquivo**: `crate_sazonal.png`

**Prompt**:
> A single mystical Minecraft chest made of purple-stained blocks with swirling seasonal magic effects. The chest is surrounded by autumn leaves, snowflakes, cherry blossoms, and summer fireflies representing all four seasons simultaneously. Purple and mystical multicolored energy flows from the opening chest. Holiday-themed items float above: a totem of undying, god apples, a mysterious legendary key. Dark background with aurora-like colorful particle effects. Minecraft voxel block art style. No text.

---

### 🖼️ SEÇÕES EXTRAS

---

#### IMG-12 — Banner Claims & Terrenos
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s1 (Terrenos), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_claims.png`

**Prompt**:
> A Minecraft voxel art overhead view of a well-developed player claim territory. A cozy medieval house with farm plots, animal pens, and a watchtower sits inside a clearly defined claim boundary shown by subtle golden particle lines. The claim area contrasts with the wild, untamed terrain outside. A Minecraft player character places a golden shovel to mark territory. Warm daytime lighting with puffy clouds. The protected area feels safe and organized while wilderness beyond looks dangerous. Minecraft block style. No text.

---

#### IMG-13 — Banner Duelos
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s14 (Duelos), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_duelos.png`

**Prompt**:
> A Minecraft voxel art gladiator-style duel arena. A circular colosseum made of dark stone bricks and quartz with spectator stands filled with Minecraft players. In the center, two warriors face off: one with a diamond sword and shield, the other with a netherite axe. A glowing barrier dome surrounds the duel zone. Redstone torches and soul lanterns light the arena. Dramatic spotlight from above. The atmosphere is tense and gladiatorial. Minecraft block aesthetic with epic arena lighting. No text.

---

#### IMG-14 — Banner Bounties & Recompensas
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s17 (Bounties), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_bounties.png`

**Prompt**:
> A Minecraft voxel art wanted-poster scene in a dark medieval tavern. A shadowy bounty board made of dark oak wood displays multiple bounty targets (represented by paper items with glowing red markers). Gold coins and emeralds are stacked on a nearby table as reward money. A hooded Minecraft assassin character sits in the corner sharpening a netherite sword. Candlelight flickers creating dramatic shadows. The atmosphere is dark, mysterious, and dangerous. Minecraft block style with moody tavern lighting. No text or readable words.

---

#### IMG-15 — Banner Daily Rewards
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s29 (Daily Rewards), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_daily_rewards.png`

**Prompt**:
> A Minecraft voxel art daily login reward calendar display. Seven floating treasure chests arranged in a row, progressively getting more ornate from left (simple wooden) to right (golden with particles). The chests glow with increasing intensity: dim white, soft blue, green, yellow, orange, red, to brilliant golden/rainbow for day 7. Each chest has items floating above it getting rarer. A cozy Minecraft spawn area with warm lighting in the background. Celebratory particle effects on the final chest. Minecraft block art style. No text.

---

### 🆕 SEÇÕES EXTRAS (Batch 2)

---

#### IMG-16 — Banner Nações
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s4 (Nações), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_nacoes.png`

**Prompt**:
> A grand Minecraft voxel art scene depicting an empire formed by multiple allied kingdoms. A massive imperial throne room with obsidian walls and golden pillars. In the center, an Emperor character in golden netherite armor and a glowing golden crown sits on an elevated throne. Below, several Kings in different colored armor (each representing a different kingdom) kneel in allegiance, presenting their banners. The room has a large round table with a map of the realm etched on its surface. Purple and gold particle effects radiate from the Emperor, representing nation-level buffs (Speed and Resistance aura effects). Stained glass windows showing different kingdom emblems line the walls. Dark medieval atmosphere with rich golden and purple lighting. Minecraft block style. No text.

---

#### IMG-17 — Banner Postos Avançados (Outposts)
- **Status**: `[x]`
- **Destino**: `ebook_jogador.html` → Seção s16 (Outposts), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_outposts.png`

**Prompt**:
> A Minecraft voxel art scene showing a fortified military outpost built far from the main kingdom. A small but heavily defended stone brick fortress with watchtowers and iron fences sits on a remote hilltop surrounded by wilderness. An orange glowing particle boundary line marks the claimed territory around the outpost. In the far distance across plains and forests, the main kingdom castle can be seen on the horizon, connected by a path. A Minecraft player character in iron armor stands guard on the watchtower, holding a gold shovel. The outpost has a mining entrance below suggesting resource extraction. The surroundings are wild and untamed, contrasting with the fortified structure. Sunset lighting with orange hues matching the outpost markers. Minecraft block aesthetic. No text.

---

#### IMG-18 — Banner Leilão Global
- **Status**: `[ ]`
- **Destino**: `ebook_jogador.html` → Seção s9 (Leilão), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_leilao.png`

**Prompt**:
> A Minecraft voxel art auction house scene with intense bidding energy. A grand octagonal hall made of polished dark stone and gold blocks with tiered spectator balconies. In the center, a podium holds a glowing enchanted netherite sword on display as the current auction item. Multiple Minecraft player characters in the audience raise their hands excitedly to place bids. An auctioneer NPC character stands at the podium with a golden gavel. Floating holographic-style timer particles count down in the air (just visual particle effects, no actual numbers). Gold coins and emeralds are piled on bidding tables. Dramatic focused lighting on the auction item with ambient purple and gold glow in the hall. Excitement and competition atmosphere. Minecraft block style. No text.

---

#### IMG-19 — Banner Tutorial & Welcome Kit
- **Status**: `[ ]`
- **Destino**: `ebook_jogador.html` → Seção s30 (Tutorial), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_tutorial.png`

**Prompt**:
> A warm and welcoming Minecraft voxel art scene at a server spawn area. A new Minecraft player character in default Steve skin stands at a grand medieval spawn gate decorated with glowstone lanterns and purple banners. In front of them, a friendly NPC guide villager holds out a gift bundle containing starter items: a golden shovel, an iron shovel, a stick (inspection tool), a shield, an iron sword, cooked beef steaks, and a full set of iron armor (helmet, chestplate, leggings, boots), all floating in a semicircle with sparkle particles. A glowing blue BossBar-style progress indicator arcs above the scene showing tutorial step progression (just visual particle trail, no text). A path of glowing stepping stones leads from spawn toward the open Minecraft wilderness ahead, suggesting the adventure beginning. Warm sunrise lighting creates a hopeful, inviting atmosphere. Minecraft block aesthetic. No text.

---

#### IMG-20 — Banner Leaderboards & Rankings
- **Status**: `[ ]`
- **Destino**: `ebook_jogador.html` → Seção s32 (Leaderboards), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_leaderboards.png`

**Prompt**:
> A Minecraft voxel art competitive podium and hall of champions scene. A grand three-tiered podium made of gold blocks (1st place center, tallest), iron blocks (2nd place left), and copper blocks (3rd place right). Three Minecraft player characters stand on the podium in full netherite armor, the winner holding a diamond sword triumphantly overhead. Behind them, a massive dark stone wall displays eight vertical ranking categories represented by floating item icons: a diamond sword (kills), a skeleton skull (deaths), crossed swords (KDR), gold ingots (wealth), a clock (playtime), a wither skull (bosses killed), a crown (kingdoms), and a fire charge (kill streak). Each category has 10 glowing slots stacked vertically suggesting the top 10 positions. Confetti particles and golden firework effects celebrate in the air. Dark arena with dramatic spotlight lighting from above. Competitive tournament atmosphere. Minecraft block art style. No text.

---

#### IMG-21 — Banner Diplomacia
- **Status**: `[ ]`
- **Destino**: `ebook_jogador.html` → Seção s5 (Diplomacia), após o `<p>` introdutório
- **Classe CSS**: `showcase-image`
- **Nome do arquivo**: `banner_diplomacia.png`

**Prompt**:
> A Minecraft voxel art scene of a diplomatic summit between rival kingdoms. A grand open-air stone pavilion with quartz pillars and colored banners serves as neutral ground. On the left side, two Kings in green-tinted diamond armor shake hands across a dark oak table, symbolizing an alliance (green particle effects and a green banner behind them). On the right side, two other Kings in red-tinted netherite armor point swords at each other across a similar table, representing enmity (red particle effects and crimson banners). In the center, a neutral arbiter figure stands between both sides. The alliance side has warm golden lighting and peaceful vibes, while the enemy side has dark stormy red lighting. The ground shows subtle yellow neutral zone markings. Dramatic split lighting effect: warm on the left, ominous on the right. Minecraft block aesthetic with epic fantasy atmosphere. No text.

---

## 📝 Instruções de Integração

Após gerar cada imagem:

1. **Salve** na pasta `Imagens Ebook/` com o nome indicado
2. **Insira** no `ebook_jogador.html` no local indicado em "Destino"
3. **Use** a classe CSS indicada em "Classe CSS"
4. Para **banners de seção**, insira logo após o parágrafo `<p>` introdutório da seção
5. Para **crates individuais**, insira dentro do `<div class="crate-card ...">` antes do `<div class="crate-icon">`
6. Para a **capa**, insira dentro do `<div class="cover">` antes do `<h1>`

### Exemplo de integração (banner):
```html
<img src="Imagens Ebook/banner_reinos.png" alt="Reinos" class="showcase-image">
```

### Exemplo de integração (crate individual):
```html
<img src="Imagens Ebook/crate_comum.png" alt="Crate Comum" class="showcase-image" style="max-height:160px">
```
