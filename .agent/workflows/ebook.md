---
description: executar o próximo batch do roadmap de produção do E-Book GorvaxMC
---

# /ebook — Produção do E-Book GorvaxMC

## Passo 1: Ler o roadmap
// turbo
Leia o arquivo `EBOOK_ROADMAP.md` na raiz do projeto para entender o estado atual.

## Passo 2: Identificar o próximo batch
Encontre o próximo batch com status `[ ]` (pendente) na tabela de progresso.

## Passo 3: Ler arquivos de configuração necessários
Antes de qualquer implementação ou geração de imagem, leia os arquivos de configuração relevantes ao batch:
- `src/main/resources/boss_settings.yml` — Bosses (HP, skills, partículas, escala, equipamento)
- `src/main/resources/custom_items.yml` — Itens lendários (base, enchants, on-hit, passive)
- `src/main/resources/cosmetics.yml` — Cosméticos (partículas, trails, tags, kill effects)
- `src/main/resources/crates.yml` — Crates (rewards, peso, broadcast)
- `src/main/resources/mini_bosses.yml` — Mini-bosses (entity, bioma, skills, loot)
- `src/main/resources/achievements.yml` — Conquistas (trigger, meta, reward)
- `src/main/resources/config.yml` — Configuração geral
- `src/main/resources/market_global.yml` — Mercado (itens, preços)
- `src/main/resources/messages.yml` — Mensagens
- `src/main/resources/battlepass.yml` — Battle Pass
- `src/main/resources/quests.yml` — Quests
- `src/main/resources/boss_rewards.yml` — Loot dos bosses
- `MANUAL.md` — Manual completo (2700 linhas, 46 seções)

Só leia os arquivos relevantes ao batch atual. Não é necessário ler todos se o batch não os utiliza.

## Passo 4: Executar o batch
Execute TODAS as tarefas do batch identificado conforme detalhado no `EBOOK_ROADMAP.md`.

### Regras de execução:

**Para batches de conteúdo (E1-E4, E7):**
- Escrever o HTML diretamente no arquivo correspondente (`ebook_jogador.html` ou `ebook_staff.html`)
- Todo conteúdo em **Português (Brasil)**
- Usar os componentes CSS definidos em E1
- Representar fielmente os dados dos configs (HP, skills, rewards, etc.)

**Para batches de imagens (E5-E6):**
- Gerar no **máximo 2-3 imagens** por execução do workflow
- Se o serviço retornar erro 503, NÃO tentar repetidamente — informar o usuário e continuar com CSS-only placeholders
- Salvar imagens no mesmo diretório dos HTMLs
- Descrever no prompt EXATAMENTE os dados do config (partículas, equipamento, escala, etc.)

**Para polimento (E8):**
- Verificar cross-reference entre HTML e MANUAL.md
- Testar abertura no navegador
- Verificar links internos e tabelas

## Passo 5: Atualizar o roadmap
Após completar TODAS as tarefas do batch:
1. Marque o batch como `[x]` no `EBOOK_ROADMAP.md`
2. Atualize "Último batch executado" e "Data da última execução"
3. Atualize "Próximo batch a executar"

## Passo 6: Notificar o usuário
Informe o que foi feito, mostre preview se aplicável, e pergunte se quer ajustes antes de prosseguir para o próximo batch.

---

## Notas importantes

- **Tema**: Medieval / RPG / Fantasia / Semi-Anárquico
- **Paleta**: Preto, roxo escuro, dourado, carmesim, azul-cyan
- **Idioma**: 100% Português (Brasil)
- **Dois e-books**: `ebook_jogador.html` (34 seções) e `ebook_staff.html` (15 seções)
- **Imagens**: Fiéis aos configs reais do servidor, estilo Minecraft render + fantasia épica
