---
description: gerar a próxima estrutura/build do GorvaxMC (uma por vez, via script Python procedural)
---

# Workflow: Geração de Estruturas (.schem)

// turbo-all

## Instruções

1. Leia o plano de builds em `c:\Users\Gorska\Desktop\gorvax-plugin\BUILDS.md`
2. Identifique o **próximo BATCH** que ainda está marcado como `[ ]` (não gerado)
3. Leia a seção correspondente do BUILDS.md para entender todos os detalhes da estrutura (layout, materiais, dimensões, elementos)
4. Leia a lore em `c:\Users\Gorska\Desktop\gorvax-plugin\LORE.md` para garantir coerência temática
5. Crie (ou atualize) o script Python `c:\Users\Gorska\Desktop\gorvax-plugin\tools\gorvax_builder.py` com a lógica de geração da estrutura:
   - Use a biblioteca `mcschematic` para gerar o `.schem`
   - O script deve importar e usar funções helper do módulo `c:\Users\Gorska\Desktop\gorvax-plugin\tools\build_helpers.py`
   - Se `build_helpers.py` ainda não existir, crie-o com as funções utilitárias necessárias (circle, wall, tower, house, arch, fill_floor, etc.)
   - O script deve gerar apenas a estrutura do batch atual
   - Salvar o `.schem` em `c:\Users\Gorska\Desktop\gorvax-plugin\tools\output\`
6. Instale dependências se necessário: `pip install mcschematic`
7. Execute o script: `python c:\Users\Gorska\Desktop\gorvax-plugin\tools\gorvax_builder.py`
8. Verifique que o arquivo `.schem` foi gerado com sucesso na pasta `tools/output/`
9. Atualize o `BUILDS.md` marcando o batch como `[x]` (gerado)
10. Informe ao usuário:
    - Qual estrutura foi gerada
    - Caminho do arquivo `.schem`
    - Como colar no servidor (`//schem load <nome>` → `//paste -a`)
    - Qual é o próximo batch pendente

## Regras

- **UMA estrutura por execução** — Nunca gere mais de uma por vez
- O script deve ser **idempotente** — re-executar gera o mesmo resultado (ou com seed configurável)
- Use blocos válidos do Minecraft 1.21+ (IDs no formato `minecraft:stone_bricks`)
- Gere estruturas centradas na coordenada 0,0,0 do schematic (o jogador escolhe onde colar)
- Incluir chão/fundação — não gerar estruturas "flutuantes"
- Se o BUILDS.md indicar que todos os batches estão `[x]`, informe ao usuário que todas as estruturas foram geradas
- Sempre teste se o `.schem` foi criado e tem tamanho > 0 antes de marcar como concluído
- Se der erro na geração, reporte o erro ao usuário e **NÃO** marque como `[x]`

## Qualidade e Estilo Visual

- **OBRIGATÓRIO**: As estruturas devem ser **extremamente detalhadas e bonitas**. Nada de caixas simples ou builds genéricas.
- **Tema**: RPG/Fantasia medieval sombrio. Inspirado em Dark Souls, Elden Ring, Lord of the Rings.
- **Variação de blocos**: NUNCA use um único tipo de bloco em áreas grandes. Sempre misture variações (ex: `stone_bricks` + `cracked_stone_bricks` + `mossy_stone_bricks` aleatoriamente).
- **Profundidade nas paredes**: Use `stairs`, `slabs`, `walls` e `trapdoors` para criar profundidade e textura nas fachadas. Paredes planas são proibidas.
- **Detalhes decorativos**: Incluir `flower_pots`, `candles`, `lanterns`, `chains`, `banners`, `armor_stands`, `item_frames`, `barrels`, `anvils`, etc. em interiores.
- **Telhados**: Sempre com inclinação usando `stairs` + `slabs`. Nunca telhados planos (exceto em torres, que podem ter merlões).
- **Iluminação**: Abundante, variada e temática. Usar `soul` variants para áreas mais sombrias, `lanterns` para áreas habitáveis.
- **Vegetação**: Incluir `azalea`, `moss_block`, `vines`, `leaves`, `flowers` para dar vida. Usar `hanging_roots` e `glow_lichen` em áreas subterrâneas/antigas.
- **Textos em Português (PT-BR)**: Qualquer sign, lectern, ou texto deve ser em português, usando § para cores do Minecraft.
- **VARIEDADE OBRIGATÓRIA**: Casas e edifícios NUNCA devem ser iguais. Cada casa deve variar em pelo menos 3 destes aspectos: dimensão, tipo de madeira, formato do telhado, número de andares, decoração interna, jardim/cerca, chaminé, varanda, janelas. Use randomização controlada para garantir que nenhuma construção seja clone de outra. O jogador deve sentir que cada casa foi construída por um morador diferente.
- **Templates com variações**: Ter 3 templates base (pequena, média, grande) mas cada instância deve ter variações únicas: espelhar horizontalmente, trocar materiais, adicionar/remover elementos decorativos, mudar posição de portas/janelas.

## Estrutura de Arquivos

```
tools/
├── gorvax_builder.py        # Script principal de geração
├── build_helpers.py         # Funções utilitárias (circle, wall, tower, house, etc.)
└── output/
    ├── gorvax_spawn.schem   # BATCH 1
    ├── gorvax_valheim.schem # BATCH 2
    └── gorvax_arena.schem   # BATCH 3
```
