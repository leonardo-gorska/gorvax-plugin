---
description: gerar a próxima imagem do ebook GorvaxMC (uma por vez)
---

# /img — Geração de Imagem do E-Book GorvaxMC

## Passo 1: Ler o arquivo de prompts
// turbo
Leia o arquivo `PROMPTS_IMAGENS.md` na raiz do projeto para ver o estado atual.

## Passo 2: Identificar o próximo prompt
Encontre o próximo prompt com status `[ ]` (pendente) na lista.

## Passo 3: Gerar 3 variações
Use a tool `generate_image` **3 vezes** com o mesmo prompt indicado no arquivo.
- Gere **exatamente 3 variações** — nem mais, nem menos.
- Use nomes temporários distintos (ex: `img_var1`, `img_var2`, `img_var3`).

## Passo 4: Escolher a melhor
Visualize as 3 imagens geradas e **escolha a melhor** (composição, fidelidade ao estilo, qualidade geral).

## Passo 5: Salvar a vencedora
Copie **apenas a imagem vencedora** para a pasta `Imagens Ebook/` com o nome indicado no prompt.

## Passo 6: Integrar no HTML
Insira a tag `<img>` no `ebook_jogador.html` conforme indicado em "Destino" e "Classe CSS" do prompt.
Siga as instruções de integração no final do `PROMPTS_IMAGENS.md`.

## Passo 7: Atualizar o arquivo de prompts
1. Marque o prompt como `[x]` no `PROMPTS_IMAGENS.md`
2. Atualize "Último prompt executado" e "Data da última execução"
3. Atualize "Próximo prompt a executar" para o próximo `[ ]`
4. Atualize o contador de "Concluídos"

## Passo 8: Notificar o usuário
Mostre a imagem vencedora e informe onde foi integrada.

---

## ⚠️ LIMITES OBRIGATÓRIOS

- **🚫 MÁXIMO 3 imagens geradas por conversa (1 prompt × 3 variações)**
- **1 prompt por `/img`** — o usuário chamará `/img` novamente para o próximo
- NUNCA avance para o próximo prompt na mesma conversa
- NUNCA gere mais de 3 imagens — isso é um hard limit para evitar estouro de tokens

## Notas de estilo

- **Estilo**: Minecraft voxel render + fantasia épica (descrito no PROMPTS_IMAGENS.md)
- **Sem texto/letras** nas imagens (exceto quando explicitamente indicado)
- **Salvar em**: `Imagens Ebook/` com o nome do campo "Nome do arquivo"
