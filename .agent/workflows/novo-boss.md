---
description: como adicionar um novo world boss ao sistema de bosses
---

# Novo World Boss — Passo a Passo

## 1. Criar a Classe do Boss

Crie uma nova classe em `src/main/java/br/com/gorvax/core/boss/model/` estendendo `WorldBoss`:

```java
package br.com.gorvax.core.boss.model;

public class NovoBoss extends WorldBoss {
    // Implementar métodos obrigatórios:
    // - spawn(Location)
    // - update() — tick a cada 1s
    // - onPhaseChange(int phase) — fases 1, 2, 3
    // - playSpecialAttack(Player target)
    // - cleanup() — limpeza ao morrer
}
```

## 2. Configurar Stats

Adicione a configuração do boss em `boss_settings.yml`:

```yaml
novo_boss:
  name: "Nome do Boss"
  type: ZOMBIE  # Tipo de entidade base
  hp: 500
  damage_base: 15
  phases:
    phase2_threshold: 0.5
    phase3_threshold: 0.25
  skills:
    # Definir skills específicas
  ai:
    targeting:
      # Pesos de targeting
  visual:
    scale: 2.0
    # Partículas e efeitos
```

## 3. Configurar Recompensas

Adicione o loot em `boss_rewards.yml`:

```yaml
novo_boss:
  weapons:
    weapon_1:
      name: "§6Nome da Arma"
      material: NETHERITE_SWORD
      lore: [...]
      enchantments: [...]
  armor:
    # Set de armadura
  tools:
    # Ferramentas
  common_items:
    # Itens comuns
```

## 4. Registrar no BossManager

No `BossManager.java`, registre o novo boss no mapa de tipos disponíveis.

## 5. Testar

Teste com o comando:
```
/boss spawn novo_boss
/boss testloot novo_boss 1
```

## Notas Importantes
- Cada boss DEVE ter anti-kite se for melee
- Cada boss DEVE ter 3 fases com mudança de cor na BossBar
- IA de Targeting deve usar Threat Score
- Loot DEVE ter lore rico com narrativa
- Efeitos visuais obrigatórios (partículas, sons)
