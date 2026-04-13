---
description: como adicionar uma nova feature seguindo os padrões do GorvaxCore
---

# Nova Feature — Checklist de Desenvolvimento

## Antes de Começar

1. Leia o `.cursorrules` para garantir que está seguindo todas as regras do projeto.

2. Identifique em qual módulo a feature se encaixa:
   - `boss/` — Bosses e combate
   - `managers/` — Claims, Mercado, End Reset, Player Data
   - `towns/` — Reinos, tasks, menus
   - `listeners/` — Eventos do Bukkit
   - `commands/` — Comandos
   - `utils/` — Utilitários e hooks

## Passos de Implementação

3. Crie as classes necessárias no pacote correto seguindo as convenções:
   - Nomes em inglês (PascalCase para classes, camelCase para métodos)
   - Comentários em Português (Brasil)
   - Thread-safety com `ConcurrentHashMap` se necessário

4. Se a feature tem persistência de dados:
   - Use YAML (`FileConfiguration`)
   - Implemente save assíncrono (snapshot na Main → I/O em Async)
   - Adicione `reload()` para suportar `/gorvax reload`

5. Se a feature tem novos comandos:
   - Adicione em `plugin.yml` (com aliases em português)
   - Registre no `GorvaxCore.onEnable()`
   - Atualize `Comandos.md`

6. Se a feature tem novos placeholders:
   - Adicione em `GorvaxExpansion.java`
   - Documente no `Comandos.md` (seção Placeholders)

7. Garanta compatibilidade com Bedrock:
   - Inputs: AnvilGUI com fallback via chat para Floodgate
   - GUIs: Teste se funcionam em Bedrock
   - Sem features exclusivas Java Edition sem fallback

## Validação

8. Verifique que o build compila sem erros:
```
./gradlew clean shadowJar
```

9. Atualize a documentação:
   - `Comandos.md` (se novos comandos/placeholders)
   - `README.md` (se feature visível ao jogador)
   - `plugin.yml` (se novos comandos/permissões)
