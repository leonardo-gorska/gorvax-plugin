# 📔 CHECKLIST DE TESTES — GorvaxCore v1.0.0

Este guia serve para validar se todas as funcionalidades estão operando conforme o planejado.

---

## 🏰 Sistema de Reinos e Claims
- [ ] Criar um claim usando a Pá de Ouro (2 cantos)
- [ ] Usar `/confirmar` para oficializar o território
- [ ] Renomear com `/reinonome <nome>`
- [ ] Verificar proteção contra break/place/interact por não-membros
- [ ] Testar `/permitir <nick> CONSTRUÇÃO` e `/remover <nick>`
- [ ] Testar `/reino spawn` e `/reino setspawn`
- [ ] Verificar `/reino info` e `/reino membros`
- [ ] Deletar reino com `/reino deletar confirmar`

## 🏘️ Sistema de Feudos (Lotes)
- [ ] Criar lote dentro do reino com Pá de Ferro + `/lote criar`
- [ ] Definir preço com `/lote preco <valor>` e aluguel com `/lote aluguel <valor>`
- [ ] Comprar lote como jogador (`/lote comprar`)
- [ ] Testar cobrança automática de aluguel (24h)
- [ ] Testar `/lote abandonar` e `/lote retomar` (rei)

## 🤝 Diplomacia e Guerra
- [ ] Propor aliança (`/reino alianca propor <reino>`) e aceitar
- [ ] Declarar inimizade (`/reino inimigo <reino>`)
- [ ] Declarar guerra (`/reino guerra declarar <reino>`)
- [ ] Verificar sistema de pontos e rendição

## 🌐 Nações
- [ ] Criar nação (`/nacao criar <nome>`)
- [ ] Convidar e aceitar reino
- [ ] Depositar/sacar no banco nacional
- [ ] Chat de nação (`/nc`)

## 🏕️ Outposts e Votação
- [ ] Criar outpost (`/reino outpost criar`)
- [ ] Criar votação (`/reino voto criar <titulo>`) e votar

## 💰 Mercado Global e Local
- [ ] Abrir mercado global (`/market`) e comprar/vender
- [ ] Verificar variação dinâmica de preços
- [ ] Abrir mercado local (`/market local`) dentro de um reino
- [ ] Verificar histórico (`/market historico`)

## 🔨 Leilão
- [ ] Iniciar leilão (`/leilao iniciar <preco>`)
- [ ] Dar lance (`/leilao lance <valor>`)
- [ ] Coletar itens após finalização (`/leilao coletar`)

## 💬 Chat (6 canais)
- [ ] Testar cada canal: `/g`, `/l`, `/rc`, `/ac`, `/tc`, `/nc`
- [ ] Alternar canal padrão (`/chat <canal>`)
- [ ] Testar `/ignore <nick>` e desbloquear
- [ ] Verificar formatação de hierarquia (Rei 👑, Vice ⚔️)

## ⚔️ Combate e Duelos
- [ ] Verificar combat tag ao atacar outro jogador
- [ ] Testar kill streak (bônus por sequência de abates)
- [ ] Desafiar duelo (`/duel desafiar <nick>`) e aceitar
- [ ] Verificar aposta e recompensas do duelo

## 👹 World Bosses
- [ ] Spawnar boss (`/boss spawn rei_gorvax`)
- [ ] Validar habilidades e fases de combate
- [ ] Matar boss e verificar ranking de dano + loot
- [ ] Verificar baú com holograma
- [ ] Testar `/boss status`, `/boss next`, `/boss list`

## 🐾 Mini-Bosses
- [ ] Verificar spawn natural de mini-bosses por bioma
- [ ] Testar `/miniboss` para informações

## 🗡️ Custom Items e Crates
- [ ] Dar item customizado (`/customitem give <nick> <id>`)
- [ ] Verificar atributos e lore do item
- [ ] Abrir crate com key (`/crate`)
- [ ] Verificar preview de chances

## 🏆 Conquistas e Títulos
- [ ] Verificar desbloqueio automático de conquistas (`/conquistas`)
- [ ] Equipar título (`/titulos`)
- [ ] Verificar exibição do título no chat

## ✉️ Correio e Bounty
- [ ] Enviar carta (`/carta enviar <nick> <msg>`)
- [ ] Ler cartas (`/carta ler`)
- [ ] Colocar bounty (`/bounty colocar <nick> <valor>`)
- [ ] Verificar bounty listado (`/bounty listar`)

## 📖 Tutorial e Daily Rewards
- [ ] Jogador novo → verificar tutorial automático + Welcome Kit
- [ ] Recolher recompensa diária (`/daily`)
- [ ] Verificar login streak

## 🎮 Menu Central e Rankings
- [ ] Abrir menu central (`/menu`)
- [ ] Verificar leaderboards (`/top`)
- [ ] Navegar entre categorias do ranking

## ✨ Cosméticos e VIP
- [ ] Abrir menu de cosméticos (`/cosmetics`)
- [ ] Equipar partícula, trail, tag e kill effect
- [ ] Verificar informações VIP (`/vip`)

## 🎫 Battle Pass e Quests
- [ ] Abrir Battle Pass (`/pass`)
- [ ] Verificar progressão de XP por ações (kill, mineração)
- [ ] Abrir menu de quests (`/quests`)
- [ ] Completar uma quest diária e verificar recompensa

## 🎃 Eventos Sazonais e Karma
- [ ] Verificar evento ativo (`/evento`)
- [ ] Verificar karma (`/karma`)
- [ ] Fazer ações que alteram karma (PvP, ajudar) e verificar mudança

## 🏗️ Estruturas, Ranks, Kits e RTP
- [ ] Verificar estruturas no mapa (`/estrutura`)
- [ ] Verificar progresso de rank (`/rank`)
- [ ] Abrir menu de kits (`/kit`)
- [ ] Teleporte aleatório (`/rtp`)

## 📚 Códex de Gorvax
- [ ] Abrir códex (`/codex`)
- [ ] Verificar que entradas são desbloqueáveis por ações
- [ ] Verificar progresso por categoria

## 🌌 The End Reset
- [ ] Verificar reset automático do dragão
- [ ] Forçar reset (`/gorvax reset end`)
- [ ] Verificar avisos antes do reset

## 🔄 Config Migration e bStats
- [ ] Verificar migração automática ao atualizar config
- [ ] Verificar que bStats envia métricas anônimas

## 📊 Placeholders
- [ ] Verificar `%gorvax_reino_nome%` e `%gorvax_next_boss%`
- [ ] Verificar placeholders de VIP, BP, Karma, Codex
- [ ] Usar PAPI para testar 5+ placeholders variados

## 🔧 Administração
- [ ] Recarregar configs (`/gorvax reload`)
- [ ] Migrar storage (`/gorvax migrate yaml sqlite`)
- [ ] Verificar auditoria (`/gorvax audit`)
- [ ] Dar blocos extras (`/reino darblocos <nick> <qtd>`)

## 📱 Compatibilidade Bedrock
- [ ] Jogador Bedrock → conectar via Geyser
- [ ] Verificar fallback de AnvilGUI para chat input
- [ ] Testar GUIs principais (menu, mercado, leilão, conquistas)
- [ ] Verificar Forms nativos do Bedrock

---

*GorvaxCore v1.0.0 — Última atualização: 2026-03-10*
