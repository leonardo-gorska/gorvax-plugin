---
description: executar o próximo batch do rebalanceamento de bosses/mini bosses
---

# Workflow: Rebalanceamento de Bosses

// turbo-all

## Instruções

1. Leia o arquivo de roadmap de bosses em `c:\Users\Gorska\Desktop\gorvax-plugin\.agents\workflows\bosses_roadmap.md`
2. Identifique o **próximo batch** que ainda está marcado como `[ ]` (não concluído)
3. Leia o arquivo de configuração correspondente:
   - Para bosses principais e T2: `src/main/resources/boss_settings.yml`
   - Para mini bosses de bioma: `src/main/resources/mini_bosses.yml`
4. Aplique **TODAS** as alterações descritas no batch identificado, editando o arquivo YAML correspondente
5. Após aplicar as mudanças, atualize o roadmap (`bosses_roadmap.md`) marcando o batch como `[x]`
6. Compile o projeto executando o workflow `/build` para verificar que nada quebrou
7. Informe ao usuário o que foi alterado e qual é o próximo batch pendente

## Regras

- **UM batch por execução** — Nunca faça mais de um batch por vez
- Sempre preserve comentários existentes nos YAML
- Não altere recompensas (`boss_rewards.yml`) — apenas stats de combate
- Sempre compile após as alterações para validar
- Se o roadmap indicar que todos os batches estão `[x]`, informe ao usuário que o rebalanceamento está completo
