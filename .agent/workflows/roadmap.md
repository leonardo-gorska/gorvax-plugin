---
description: executar o próximo batch do roadmap de auditoria
---

# /roadmap — Roadmap de Melhorias do GorvaxCore

// turbo-all

## Instruções

1. Abra o arquivo `ROADMAP.md` na raiz do projeto
2. Vá até a seção **v3.1: Polimento & Modernização** (após o B22)
3. Identifique o próximo batch com status `[ ]` (pendente)
4. Execute **apenas** esse batch, seguindo todas as tarefas listadas
5. Ao concluir, marque como `[x]` no ROADMAP.md e atualize o Log de Execução
6. Atualize o `MANUAL.md` se necessário
7. Faça build (`.\gradlew.bat shadowJar`) e deploy para `C:\Users\Gorska\Desktop\Gorvax\plugins\`
8. Informe o resultado ao usuário

## Regras

- Execute **um batch por vez**, nunca mais de um
- Siga a ordem numérica dos batches
- Cada batch é dimensionado para NÃO exceder o contexto da IA (~15 arquivos max por batch)
- Sempre fazer build e deploy ao final
- Sempre usar APIs modernas do Paper/Adventure (Component API, não String legada)
- Código em inglês, comentários e mensagens em PT-BR
- O servidor fica em `C:\Users\Gorska\Desktop\Gorvax\`
- O plugin fica em `c:\Users\Gorska\Desktop\gorvax-plugin\`

## Deploy

```powershell
# Build
.\gradlew.bat shadowJar

# Deploy (remove JARs antigos para evitar "Ambiguous plugin name")
Remove-Item "C:\Users\Gorska\Desktop\Gorvax\plugins\GorvaxCore*.jar" -Force -ErrorAction SilentlyContinue
Copy-Item -Path "build\libs\GorvaxCore-1.0.0-dev-all.jar" -Destination "C:\Users\Gorska\Desktop\Gorvax\plugins\GorvaxCore-1.0.0-dev-all.jar" -Force
```
