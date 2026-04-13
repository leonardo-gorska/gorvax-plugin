# Roadmap de Rebalanceamento de Bosses

> Cada batch é executado via `/bosses`. Um batch por execução.

---

## BATCH 1 — Rei Gorvax (Boss Principal)
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `rei_gorvax`
- [x] Aplicar alterações

### Alterações:
```yaml
rei_gorvax:
  hp: 15000.0            # era 1200.0
  normal_damage: 28.0     # era 18.0
  visual_effects:
    scale: 2.2            # era 1.6
  skills:
    cooldown_base: 5000   # era 8000
    cooldown_rage: 2500   # era 5000
    salto:
      dano: 40.0          # era 22.0
      raio: 14            # era 10
    meteor:
      explosao_yield: 3.5 # era 2.5
    prison:
      duracao_ticks: 140   # era 80
    teleport:
      range: 35            # era 25
      focus_duration_ms: 8000 # era 5000
    # visual_effects sub-skills:
    repel:
      raio: 12             # era 8
      forca: 3.0           # era 2.2
    pull:
      raio: 22             # era 15
      forca: 2.5           # era 1.8
    royal_roar:
      dano: 18.0           # era 8.0
      raio: 18             # era 12
      weakness_duration: 120 # era 80
      nausea_duration: 60   # manter
    throne_flame:
      dano_dentro: 8.0     # era 4.0
      dano_fora: 14.0      # era 6.0
      raio_interno: 8      # era 6
      raio_externo: 16     # era 10
      fire_ticks: 300      # era 200
    royal_decree:
      reflect_percent: 0.7 # era 0.5
      glowing_duration: 200 # era 140
      slowness_allies_duration: 100 # era 60
      raio_aliados: 12     # era 8
  ai_targeting:
    weights:
      dps: 50              # era 40
      fleeing: 40          # era 30
      healer: 30           # era 20
      proximity: 10        # manter
      low_hp_penalty: -10  # manter
    ranges:
      close: 12            # era 10
      mid: 25              # manter
      far: 60              # era 40
```

---

## BATCH 2 — Indrax Abissal (Boss Principal)
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `indrax`
- [x] Aplicar alterações

### Alterações:
```yaml
indrax:
  hp: 14000.0             # era 1100.0
  normal_damage: 25.0     # era 16.0
  visual_effects:
    scale: 2.0            # era 1.3
  skills:
    cooldown_base: 4500   # era 7000
    cooldown_rage: 2000   # era 5000
    pulso:
      dano: 6.0           # era 3.0
      raio: 16            # era 12
      interval_ticks: 40  # era 60
    dreno:
      dano: 18.0          # era 10.0
      cura_boss: 200.0    # era 50.0
      raio: 14            # era 10
    singularidade:
      dano: 20.0          # era 10.0
      raio: 22            # era 16
      pull_force: 2.5     # era 1.5
      pull_y: 0.8         # era 0.4
    rage:
      nivel_forca: 3      # era 2
      duracao_ticks: 200   # era 140
    void_erase:
      raio: 16            # era 12
      wither_duration: 200 # era 120
      wither_level: 3     # era 2
    dark_paralysis:
      raio: 18            # era 14
      slow_duration: 160   # era 100
      slow_level: 5       # manter
      blind_duration: 200  # era 120
    abyss_collapse:
      raio: 22            # era 18
      levitation_duration: 50 # era 30
      levitation_level: 2 # era 1
    contamination:
      radius_spawn: 16    # era 12
      radius_periodic: 12 # era 8
      chance: 0.5         # era 0.35
    minions:
      count_75: 8         # era 4
      count_50: 12        # era 6
      count_25: 16        # era 8
```

---

## BATCH 3 — Vulgathor, Arauto das Cinzas (T2-1)
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `vulgathor`
- [x] Aplicar alterações

### Alterações:
```yaml
vulgathor:
  hp: 8000.0              # era 900.0
  normal_damage: 22.0     # era 14.0
  movement_speed: 0.34    # era 0.32
  visual_effects:
    scale: 2.0            # era 1.8
  skills:
    cooldown_base: 5500   # era 7000
    cooldown_rage: 3000   # era 4500
    fire_aura:
      raio: 10.0          # era 6.0
      dano: 6.0           # era 3.0
      fire_ticks: 100     # era 60
    incendiary_rain:
      raio: 18.0          # era 14.0
      count: 12           # era 8
      dano: 10.0          # era 5.0
    ash_explosion:
      raio: 14.0          # era 10.0
      dano: 16.0          # era 8.0
      blindness_duration: 60 # manter
      knockback: 2.5      # era 1.8
    igneous_fury:
      duracao_ticks: 240   # era 160
    magma_pillar:
      dano: 14.0          # era 7.0
      raio: 6.0           # era 4.0
    inferno:
      raio: 20.0          # era 16.0
      dano: 20.0          # era 10.0
    minions:
      count_75: 4         # era 2
      count_50: 6         # era 4
      count_25: 10        # era 6
```

---

## BATCH 4 — Xylos, Devorador de Éter (T2-2)
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `xylos`
- [x] Aplicar alterações

### Alterações:
```yaml
xylos:
  hp: 6500.0              # era 750.0
  normal_damage: 20.0     # era 12.0
  movement_speed: 0.36    # era 0.35
  visual_effects:
    scale: 2.0            # manter
  skills:
    cooldown_base: 5500   # era 7000
    cooldown_rage: 3000   # era 4500
    spatial_rupture:
      raio: 18.0          # era 15.0
      dano: 12.0          # era 6.0
      tp_range: 15.0      # era 10.0
    gravitational_implosion:
      raio: 18.0          # era 14.0
      dano: 10.0          # era 5.0
      levitation_duration: 60 # era 40
    ether_fragment:
      count: 5            # era 3
      dano: 10.0          # era 5.0
      raio_impacto: 6.0   # era 4.0
    distortion:
      raio: 16.0          # era 12.0
      nausea_duration: 160 # era 100
      darkness_duration: 140 # era 80
    void_phase:
      duracao_ticks: 120   # era 80
      dano: 15.0          # era 8.0
    minions:
      count_75: 3         # era 2
      count_50: 5         # era 3
      count_25: 8         # era 4
```

---

## BATCH 5 — Skulkor, General Reerguido (T2-3)
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `skulkor`
- [x] Aplicar alterações

### Alterações:
```yaml
skulkor:
  hp: 5000.0              # era 650.0
  normal_damage: 18.0     # era 11.0
  movement_speed: 0.32    # era 0.30
  visual_effects:
    scale: 1.9            # era 1.8
  skills:
    cooldown_base: 6000   # era 7500
    cooldown_rage: 3500   # era 5000
    call_of_dead:
      count: 6            # era 3
    arrow_rain:
      raio: 18.0          # era 14.0
      arrows_per_player: 6 # era 4
      dano: 7.0           # era 3.0
    bone_shield:
      duracao_ticks: 160   # era 100
    war_cry:
      raio: 16.0          # era 12.0
      dano: 14.0          # era 7.0
      weakness_duration: 120 # era 80
    death_march:
      range: 25            # era 20
      dano: 16.0          # era 9.0
      focus_duration_ms: 6000 # era 4000
    minions:
      count_75: 6         # era 4
      count_50: 10        # era 6
      count_25: 14        # era 8
```

---

## BATCH 6 — Kaldur, Coração de Gelo (T2-4)
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `kaldur`
- [x] Aplicar alterações

### Alterações:
```yaml
kaldur:
  hp: 4000.0              # era 550.0
  normal_damage: 16.0     # era 10.0
  movement_speed: 0.30    # era 0.28
  visual_effects:
    scale: 1.8            # era 1.7
  skills:
    cooldown_base: 6500   # era 8000
    cooldown_rage: 4000   # era 5500
    frost_aura:
      raio: 10.0          # era 8.0
      dano: 4.0           # era 2.0
    blizzard:
      raio: 18.0          # era 14.0
      dano: 10.0          # era 5.0
      slow_duration: 140   # era 100
    ice_lance:
      dano: 14.0          # era 8.0
    glacial_prison:
      duracao_ticks: 80   # era 40
    frost_shield:
      duracao_ticks: 180   # era 120
      resistance_level: 3 # era 2
    ice_storm:
      raio: 20.0          # era 16.0
      dano: 14.0          # era 7.0
    minions:
      count_75: 4         # era 2
      count_50: 6         # era 3
      count_25: 10        # era 5
```

---

## BATCH 7 — Zarith, Presa da Selva (T2-5)
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `zarith`
- [x] Aplicar alterações

### Alterações:
```yaml
zarith:
  hp: 3000.0              # era 450.0
  normal_damage: 14.0     # era 9.0
  # movement_speed: 0.38  — manter
  # scale: 1.8            — manter
  skills:
    cooldown_base: 6500   # era 7000
    cooldown_rage: 4000   # era 4500
    poison_web:
      raio: 12.0          # era 10.0
      dano: 8.0           # era 4.0
      poison_duration: 140 # era 100
      slow_duration: 120   # era 80
    predator_leap:
      raio: 6.0           # era 5.0
      dano: 14.0          # era 8.0
      knockback: 2.0      # era 1.5
    ambush:
      range: 25            # era 20
      dano: 12.0          # era 6.0
      focus_duration_ms: 5000 # era 4000
    toxic_burst:
      raio: 14.0          # era 12.0
      dano: 10.0          # era 6.0
      wither_duration: 120 # era 80
    frenzy:
      duracao_ticks: 160   # era 120
      speed_level: 2      # manter
      strength_level: 1   # era 0
    minions:
      count_75: 4         # era 3
      count_50: 6         # era 4
      count_25: 8         # era 6
```

---

## BATCH 8 — Mini Bosses de Bioma (Todos de uma vez)
- **Arquivo:** `src/main/resources/mini_bosses.yml`
- [x] Aplicar alterações

### Guardião do Deserto:
```yaml
guardiao_deserto:
  hp: 250.0               # era 300.0
  damage: 10.0            # era 8.0
  scale: 1.4              # era 1.5
  movement_speed: 0.30    # era 0.28
  # money_reward e xp_reward — manter
  effects_on_hit:
    - "SLOWNESS:40:1"     # era 60:1
    - "HUNGER:60:1"       # era 100:1
  skills:
    sand_storm:
      radius: 8.0         # era 10.0
      damage: 5.0         # era 4.0
      cooldown: 10000     # era 8000
    summon_minions:
      count: 2            # era 3
      hp_threshold: 0.4   # era 0.5
```

### Sentinela Gélida:
```yaml
sentinela_gelida:
  hp: 300.0               # era 400.0
  damage: 9.0             # era 7.0
  scale: 1.5              # era 1.6
  # movement_speed: 0.30  — manter
  money_reward: 600.0     # era 650.0
  xp_reward: 175          # era 200
  effects_on_hit:
    - "SLOWNESS:60:1"     # era 80:2 (nível reduzido!)
    - "MINING_FATIGUE:40:1" # era 60:1
  passive_effects:
    - "FIRE_RESISTANCE:999999:0"
    # REMOVIDO: RESISTANCE:999999:0 (muito tanky pro solo)
  skills:
    frost_nova:
      radius: 8.0         # era 12.0
      damage: 4.0         # era 5.0
      cooldown: 12000     # era 10000
    ice_arrows:
      count: 3            # era 5
      damage: 3.0         # era 4.0
      cooldown: 8000      # era 6000
```

### Aranha da Selva:
```yaml
aranha_selva:
  hp: 200.0               # era 250.0
  damage: 8.0             # era 10.0
  scale: 1.8              # era 2.0
  movement_speed: 0.34    # era 0.35
  money_reward: 400.0     # era 450.0
  # xp_reward: 120        — manter
  effects_on_hit:
    - "POISON:80:1"       # era 100:1
    - "WEAKNESS:40:0"     # era 60:0
  skills:
    web_trap:
      radius: 6.0         # era 8.0
      web_count: 8        # era 12
      cooldown: 14000     # era 12000
    poison_cloud:
      radius: 5.0         # era 6.0
      damage: 3.0         # manter
      cooldown: 10000     # era 8000
```

### Fantasma do Nether:
```yaml
fantasma_nether:
  hp: 350.0               # era 500.0
  damage: 10.0            # era 12.0
  scale: 1.6              # era 1.8
  movement_speed: 0.26    # era 0.25
  money_reward: 700.0     # era 800.0
  xp_reward: 200          # era 250
  effects_on_hit:
    - "WITHER:40:1"       # era 60:1
  passive_effects:
    - "FIRE_RESISTANCE:999999:0"
    # REMOVIDO: RESISTANCE:999999:1 (muito tanky pro solo)
  skills:
    fire_rain:
      radius: 10.0        # era 14.0
      fireball_count: 5   # era 8
      cooldown: 10000     # era 7000
    flame_shield:
      duration: 80        # era 100
      resistance_level: 1 # era 2
      cooldown: 18000     # era 15000
```

---

## BATCH 9 — Sinergia de Aliança e Eventos Atmosféricos
- **Arquivo:** `src/main/resources/boss_settings.yml` — seção `alliance_synergy`
- [x] Aplicar alterações

### Alterações:
```yaml
alliance_synergy:
  enabled: true
  max_distance: 80.0      # era 50.0
  response_cooldown_ms: 1500 # era 3000
```

---
