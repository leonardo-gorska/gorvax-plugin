---
description: executar o próximo batch de correções do resource pack (Java + Bedrock)
---

# /fix — Resource Pack Fix Workflow

## Instruções

1. Abra o arquivo `FIX.md` na raiz do projeto
2. Identifique o próximo batch com status `⬜` (não iniciado)
3. Execute **apenas** esse batch, seguindo todas as tarefas listadas
4. Ao concluir todas as tarefas do batch, marque os checkboxes `[x]` em `FIX.md`
5. Atualize o status do batch na tabela de `⬜` para `✅`

## Regras

- Execute **um batch por vez**, nunca mais de um
- Siga a ordem numérica dos batches (1 → 2 → 3 → 4 → 5)
- Para cada arquivo criado/modificado:
  - Valide a sintaxe JSON antes de salvar
  - Use os padrões documentados em `FIX.md` (templates de attachable, mappings, etc.)
- Após modificar arquivos do resource pack:
  - Reconstrua o `.mcpack` usando Python zipfile
  - Copie para `C:\Users\Gorska\Desktop\Gorvax\plugins\Geyser-Spigot\packs\GorvaxCore.mcpack`
  - Copie `geyser_mappings.json` para `C:\Users\Gorska\Desktop\Gorvax\plugins\Geyser-Spigot\custom_mappings\`
- Após modificar o Java pack:
  - Reconstrua o ZIP
  - Calcule SHA1: `python -c "import hashlib; print(hashlib.sha1(open('path','rb').read()).hexdigest().upper())"`
  - Atualize `server.properties` no servidor
- Após compilar o plugin JAR:
  - Copie para `C:\Users\Gorska\Desktop\Gorvax\plugins\`
- O servidor fica em `C:\Users\Gorska\Desktop\Gorvax\`
- O plugin fica em `c:\Users\Gorska\Desktop\gorvax-plugin\`
- O resource pack bedrock fica em `resourcepack/bedrock/`
- O resource pack java fica em `resourcepack/java/`
- O mapping do geyser fica em `resourcepack/geyser_mappings.json`

## Referências Técnicas

### Bedrock Attachables
- Documentação: https://wiki.bedrock.dev/items/attachables.html
- Formato: JSON com `minecraft:attachable` e `description`
- Materiais: `entity_alphatest` (itens) ou `armor` (armaduras)
- Render controllers: `controller.render.item_default`

### Geyser Custom Items
- Documentação: https://wiki.geysermc.org/geyser/custom-items/
- Formato: JSON v2 com `type: "legacy"` para items com CustomModelData
- Wearable: Adicionar `components.minecraft:wearable.slot` para armaduras

### Caminhos do Servidor
- Geyser packs: `C:\Users\Gorska\Desktop\Gorvax\plugins\Geyser-Spigot\packs\`
- Geyser mappings: `C:\Users\Gorska\Desktop\Gorvax\plugins\Geyser-Spigot\custom_mappings\`
- Server plugins: `C:\Users\Gorska\Desktop\Gorvax\plugins\`
- Server properties: `C:\Users\Gorska\Desktop\Gorvax\server.properties`

// turbo-all
