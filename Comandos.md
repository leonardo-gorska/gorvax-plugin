# 📜 Comandos Completos do GorvaxCore Plugin

Documentação completa de todos os comandos disponíveis no GorvaxCore v1.0.0.

---

## 📖 Índice
- [Comandos Globais](#-comandos-globais)
- [Sistema de Reinos](#-sistema-de-reinos)
- [Sistema de Feudos (Lotes)](#️-sistema-de-feudos-lotes)
- [Sistema de Permissões](#-sistema-de-permissões)
- [Sistema de Chat](#-sistema-de-chat)
- [Sistema de Mercado](#-sistema-de-mercado)
- [Sistema de Leilão](#-sistema-de-leilão)
- [Sistema de Bosses](#-sistema-de-bosses)
- [Mini-Bosses](#-mini-bosses)
- [Sistema de Conquistas e Títulos](#-sistema-de-conquistas-e-títulos)
- [Sistema de Correio](#-sistema-de-correio)
- [Sistema de Bounty](#-sistema-de-bounty)
- [Sistema de Nações](#-sistema-de-nações)
- [Sistema de Duelos](#️-sistema-de-duelos)
- [Tutorial e Daily Rewards](#-tutorial-e-daily-rewards)
- [Menu Central e Rankings](#-menu-central-e-rankings)
- [Custom Items e Crates](#-custom-items-e-crates)
- [Cosméticos e VIP](#-cosméticos-e-vip)
- [Battle Pass e Quests](#-battle-pass-e-quests)
- [Eventos Sazonais e Karma](#-eventos-sazonais-e-karma)
- [Estruturas, Ranks, Kits e RTP](#️-estruturas-ranks-kits-e-rtp)
- [Códex de Gorvax](#-códex-de-gorvax)
- [Comandos Administrativos](#️-comandos-administrativos)
- [Permissões](#️-permissões)

---

## 🌍 Comandos Globais

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/confirmar` | `/c` | Confirma seleção de terreno ou ação | Nenhuma |
| `/gorvax` | `/gcore`, `/gorvaxcore` | Menu de ajuda principal | Nenhuma |
| `/gorvax hud` | - | Liga/desliga a scoreboard lateral | Nenhuma |
| `/gorvax mapa` | `/gorvax map` | Mostra mapa ASCII dos claims próximos (15×15 chunks) | Nenhuma |
| `/gorvax som` | `/gorvax sound` | Liga/desliga som ao cruzar fronteira de claim | Nenhuma |

### 🛠️ Ferramentas Especiais

| Item | Ação | Descrição |
|------|------|-----------|
| **Pá de Ouro** | Clique Direito (2×) | Seleciona cantos para criar um **Reino** |
| **Pá de Ferro** | Clique Direito (2×) | Seleciona cantos para criar um **Lote (Feudo)** |
| **Graveto (Stick)** | Clique Direito | Visualiza bordas de terrenos próximos por 10s |

---

## 🏰 Sistema de Reinos

### 👥 Comandos para Todos os Jogadores

| Comando | Aliases | Descrição | Requisitos |
|---------|---------|-----------|------------|
| `/reino` | `/kingdom`, `/k`, `/cidade`, `/city`, `/town` | Abre menu principal do reino | Nenhum |
| `/reino criar` | - | Mostra instruções para criar um novo reino | Pá de Ouro na mão |
| `/reino lista` | - | Lista todos os reinos e terrenos que você possui | Nenhum |
| `/reino spawn` | - | Teleporta para o spawn do seu reino | Membro do reino |
| `/reino membros` | - | Lista súditos e aliados do reino | Estar em um reino |
| `/reino info` | - | Mostra informações detalhadas do reino | Estar em um reino |
| `/reino debugxp` | - | Mostra tempo de atividade no reino (debug) | Membro |
| `/reino visitar <nome>` | - | Teleporta para o spawn de um reino público | Nenhum |
| `/reino aceitar` | - | Aceita convite para um reino | Ter convite pendente |
| `/reino recusar` | - | Recusa convite para um reino | Ter convite pendente |

### 👑 Comandos Exclusivos do Rei (Dono do Reino)

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/reinonome <nome>` | Define o nome do seu reino | `gorvax.reino.nome` (Rei) |
| `/reino setspawn` | Define ponto de spawn do reino | Rei + Estar no reino |
| `/reino deletar` | **PERMANENTEMENTE** deleta o reino | Rei + Confirmação |
| `/reino deletar confirmar` | Confirma a deleção do reino | Rei |
| `/reino transferir <nick>` | Transfere a coroa para outro jogador | Rei |
| `/reino convidar <nick>` | Convida um jogador para o reino | Rei ou Vice |
| `/reino pvp global <on/off>` | Ativa/Desativa PvP geral no reino | Rei |
| `/reino pvp moradores <on/off>` | Ativa/Desativa PvP entre súditos | Rei |
| `/reino pvp externo <on/off>` | Ativa/Desativa PvP de súditos fora do reino | Rei |

### 🏦 Banco do Reino

| Comando | Descrição | Requisitos |
|---------|-----------|------------|
| `/reino banco depositar <valor>` | Deposita no cofre do reino | Membro |
| `/reino banco sacar <valor>` | Saca do cofre do reino | Rei ou Vice |
| `/reino banco info` | Mostra saldo do cofre | Membro |

### 🤝 Diplomacia

| Comando | Descrição | Requisitos |
|---------|-----------|------------|
| `/reino alianca propor <reino>` | Propõe aliança para outro reino | Rei |
| `/reino alianca aceitar` | Aceita proposta de aliança | Rei |
| `/reino alianca recusar` | Recusa proposta de aliança | Rei |
| `/reino inimigo <reino>` | Declara inimizade contra outro reino | Rei |
| `/reino neutro <reino>` | Volta relação à neutralidade | Rei |

### ⚔️ Guerra

| Comando | Descrição | Requisitos |
|---------|-----------|------------|
| `/reino guerra declarar <reino>` | Declara guerra a um reino inimigo | Rei |
| `/reino guerra aceitar` | Aceita declaração de guerra | Rei |
| `/reino guerra recusar` | Recusa declaração de guerra | Rei |
| `/reino guerra rendição` | Rende-se na guerra atual | Rei |
| `/reino guerra status` | Mostra status da guerra | Membro |

### 📊 Votação

| Comando | Descrição | Requisitos |
|---------|-----------|------------|
| `/reino voto criar <titulo>` | Cria uma nova votação no reino | Rei ou Vice |
| `/reino voto sim` | Vota "sim" na votação ativa | Membro |
| `/reino voto nao` | Vota "não" na votação ativa | Membro |
| `/reino voto status` | Mostra status da votação atual | Membro |
| `/reino voto fechar` | Encerra votação antecipadamente | Rei |

### 🏕️ Outposts

| Comando | Descrição | Requisitos |
|---------|-----------|------------|
| `/reino outpost criar` | Cria outpost na seleção de Pá de Ouro | Rei |
| `/reino outpost setspawn` | Define spawn do outpost atual | Rei + Estar no outpost |
| `/reino outpost spawn [id]` | Teleporta para um outpost | Membro |
| `/reino outpost lista` | Lista outposts do reino | Membro |
| `/reino outpost deletar` | Deleta o outpost onde está pisando | Rei |

---

## 🏘️ Sistema de Feudos (Lotes)

### 👤 Comandos para Jogadores

| Comando | Aliases | Descrição | Requisitos |
|---------|---------|-----------|------------|
| `/lote` | `/plot`, `/subplot`, `/terreno`, `/feudo` | Abre menu de gerenciamento do lote | Estar em um lote |
| `/lote info` | - | Mostra informações detalhadas do lote | Estar em um lote |
| `/lote comprar` | - | Compra o lote atual (se à venda) | Dinheiro suficiente |
| `/lote alugar` | - | Aluga o lote atual (se disponível) | Dinheiro suficiente |
| `/lote abandonar` | - | Abandona o lote (devolve para o reino) | Ser dono do lote |
| `/lote amigo <nick>` | - | Dá permissão GERAL no lote para um jogador | Ser dono do lote |

### 👑 Comandos Exclusivos do Rei/Vice

| Comando | Descrição | Requer |
|---------|-----------|--------|
| `/lote criar` | Cria novo lote na área selecionada | Pá de Ferro + Seleção válida |
| `/lote retomar` | Remove o dono do lote forçadamente (confiscar) | Rei ou Vice |
| `/lote preco <valor>` | Define preço de venda do lote | Rei ou Vice ou Dono |
| `/lote aluguel <valor>` | Define valor do aluguel diário | Rei ou Vice ou Dono |
| `/lote deletar` | Deleta permanentemente o lote | Rei |

---

## 🔐 Sistema de Permissões

### Tipos de Permissão

| Tipo | Descrição | Permite |
|------|-----------|---------|
| **GERAL** | Permissão superior | Tudo (exceto admin) |
| **CONSTRUÇÃO** | Construir/destruir | Colocar e quebrar blocos |
| **CONTÊINER** | Usar containers | Abrir baús, fornalhas, barris, etc. |
| **ACESSO** | Interagir com mecanismos | Portas, botões, alavancas, etc. |
| **VICE** | Administrador (só reinos) | Gerenciar lotes, permissões, membros |

### Comandos

| Comando | Aliases | Descrição |
|---------|---------|-----------|
| `/permitir <nick> [tipo]` | `/trust` | Dá permissão no terreno (padrão: GERAL) |
| `/remover <nick>` | `/untrust` | Remove todas as permissões de um jogador |

**Exemplos:**
```
/permitir Steve           → Dá GERAL para Steve
/permitir Alex CONTÊINER  → Dá apenas CONTÊINER para Alex
/permitir Herobrine VICE  → Torna Herobrine um Vice (apenas em reinos)
/remover Steve            → Remove todas as permissões de Steve
```

---

## 💬 Sistema de Chat

### Canais Disponíveis

| Canal | Comando Direto | Aliases | Descrição |
|-------|----------------|---------|-----------|
| **Global** | `/g <msg>` | `/global` | Mensagem para todos no servidor |
| **Local** | `/l <msg>` | `/local` | Mensagem para jogadores próximos (raio configurável) |
| **Comércio** | `/tc <msg>` | `/trade`, `/comercio` | Canal de negociações |
| **Reino** | `/rc <msg>` | `/cc` | Chat privado para súditos do reino |
| **Aliança** | `/ac <msg>` | `/alliancechat` | Chat entre reinos aliados |
| **Nação** | `/nc <msg>` | - | Chat entre reinos da mesma nação (B19) |

### Gerenciamento de Canal

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/chat <canal>` | `/canal` | Alterna o canal padrão de digitação | `gorvax.chat` |
| `/chat` | - | Mostra canal atual | `gorvax.chat` |

---

## 💰 Sistema de Mercado

### 🌍 Mercado Global

| Comando | Aliases | Descrição |
|---------|---------|-----------|
| `/market` | `/loja`, `/mercado` | Abre mercado global com economia dinâmica |
| `/market historico` | - | Mostra suas transações recentes |

**Características:**
- Preços variam com oferta e demanda (multiplicador 0.2× — 3.0×)
- Normalização automática (10% decay / 5 min)
- Categorias: Minerais, Farm, Drops, etc.

### 🏘️ Mercado Local

| Comando | Descrição | Requisitos |
|---------|-----------|------------|
| `/market local` | Abre o mercado do reino atual | Estar em um reino |

**Características:** Venda P2P, taxa municipal, busca por nome.

---

## 🔨 Sistema de Leilão

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/leilao` | `/auction`, `/leilão` | Abre a GUI de leilões ativos | `gorvax.leilao` |
| `/leilao iniciar <preco> [duracao]` | `start`, `criar` | Cria leilão com item na mão | `gorvax.leilao` |
| `/leilao lance <valor> [id]` | `bid` | Dá lance em leilão (mais recente ou por ID) | `gorvax.leilao` |
| `/leilao listar` | `list` | Lista leilões ativos (GUI) | `gorvax.leilao` |
| `/leilao coletar` | `collect` | Coleta itens/dinheiro de leilões finalizados | `gorvax.leilao` |
| `/leilao cancelar <id>` | `cancel` | Cancela seu leilão ativo | `gorvax.leilao` |
| `/leilao ajuda` | `help` | Mostra ajuda do leilão | `gorvax.leilao` |

---

## 👹 Sistema de Bosses

### 🎮 Comandos de Jogador

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/boss next` | Mostra tempo para o próximo boss | Nenhuma |
| `/boss list` | Lista bosses vivos no mundo | Nenhuma |

### ⚔️ Comandos Administrativos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/boss start` | Inicia evento de spawn aleatório | `gorvax.admin` |
| `/boss spawn [id]` | Spawna um boss específico ou aleatório | `gorvax.admin` |
| `/boss kill` | Remove todos os bosses ativos | `gorvax.admin` |
| `/boss status` | Mostra status detalhado dos bosses | `gorvax.admin` |
| `/boss reload` | Recarrega configurações de boss e loot | `gorvax.admin` |
| `/boss testloot <boss> <rank>` | Testa o loot de um boss | `gorvax.admin` |

**IDs de Boss:** `rei_gorvax`, `indrax_abissal`
**Ranks de Loot:** 1 (melhor) → 6+ (participação)

---

## 🏆 Sistema de Conquistas e Títulos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/conquistas` | `/achievements`, `/ach` | Abre menu de conquistas | `gorvax.conquistas` |
| `/titulos` | `/titles` | Abre menu de seleção de títulos | `gorvax.titulos` |

---

## ✉️ Sistema de Correio

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/carta` | `/mail`, `/correio` | Mostra ajuda do correio | `gorvax.carta` |
| `/carta enviar <nick> <msg>` | `send` | Envia carta a um jogador (offline ou online) | `gorvax.carta` |
| `/carta ler` | `read` | Lista cartas recebidas | `gorvax.carta` |
| `/carta ler <id>` | - | Lê uma carta específica | `gorvax.carta` |
| `/carta deletar <id>` | `delete` | Deleta uma carta | `gorvax.carta` |
| `/carta limpar` | `clear` | Deleta todas as cartas | `gorvax.carta` |

---

## 💀 Sistema de Bounty

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/bounty` | `/recompensa` | Mostra ajuda do bounty | `gorvax.bounty` |
| `/bounty colocar <nick> <valor>` | `place` | Coloca recompensa sobre um jogador | `gorvax.bounty` |
| `/bounty listar` | `list` | Lista todas as recompensas ativas | `gorvax.bounty` |
| `/bounty remover <nick>` | `remove` | Remove recompensa que você colocou | `gorvax.bounty` |

---

## 🌐 Sistema de Nações

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/nacao` | `/nation`, `/nação` | Mostra ajuda | `gorvax.nacao` |
| `/nacao criar <nome>` | `create` | Cria uma nação (requer ser Rei) | `gorvax.nacao` |
| `/nacao dissolver` | `disband` | Dissolve a nação (líder) | `gorvax.nacao` |
| `/nacao convidar <reino>` | `invite` | Convida um reino para a nação | `gorvax.nacao` |
| `/nacao aceitar` | `accept` | Aceita convite para nação | `gorvax.nacao` |
| `/nacao recusar` | `deny` | Recusa convite | `gorvax.nacao` |
| `/nacao sair` | `leave` | Sai da nação | `gorvax.nacao` |
| `/nacao expulsar <reino>` | `kick` | Expulsa um reino da nação | `gorvax.nacao` |
| `/nacao depositar <valor>` | `deposit` | Deposita no banco da nação | `gorvax.nacao` |
| `/nacao sacar <valor>` | `withdraw` | Saca do banco da nação (líder) | `gorvax.nacao` |
| `/nacao banco` | `bank` | Mostra saldo do banco | `gorvax.nacao` |
| `/nacao info` | - | Mostra informações da nação | `gorvax.nacao` |
| `/nacao lista` | `list` | Lista todas as nações | `gorvax.nacao` |

---

## ⚔️ Sistema de Duelos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/duel` | `/duelo`, `/pvp1v1` | Abre menu de duelos | `gorvax.duel` |
| `/duel desafiar <nick>` | `challenge` | Desafia um jogador para duelo | `gorvax.duel` |
| `/duel aceitar` | `accept` | Aceita desafio de duelo | `gorvax.duel` |
| `/duel recusar` | `deny` | Recusa desafio de duelo | `gorvax.duel` |

---

## 📖 Tutorial e Daily Rewards

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/tutorial` | `/tuto` | Gerencia o tutorial do jogador | `gorvax.tutorial` |
| `/daily` | `/diario`, `/dailyreward` | Recolhe recompensa diária de login | `gorvax.daily` |

---

## 🎮 Menu Central e Rankings

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/menu` | `/gorvaxmenu` | Abre o menu central do GorvaxCore | `gorvax.menu` |
| `/top` | `/ranking`, `/rankings` | Abre leaderboards e rankings do servidor | `gorvax.top` |
| `/ignore` | `/ignorar` | Ignora um jogador no chat | `gorvax.player` |

---

## 🗡️ Custom Items e Crates

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/customitem` | `/ci`, `/customitems` | Gerenciamento de itens customizados lendários | `gorvax.admin` |
| `/crate` | `/crates`, `/chave`, `/chaves` | Sistema de crates e chaves | `gorvax.player` |

---

## ✨ Cosméticos e VIP

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/cosmetics` | `/cosmeticos`, `/cosmetic` | Abre menu de cosméticos (partículas, trails, tags) | `gorvax.player` |
| `/vip` | `/premium` | Mostra informações e status do VIP | `gorvax.player` |

---

## 🎫 Battle Pass e Quests

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/pass` | `/battlepass`, `/bp` | Abre o Battle Pass sazonal | `gorvax.player` |
| `/quests` | `/missoes`, `/quest`, `/missao` | Abre menu de quests diárias e semanais | `gorvax.player` |

---

## 🎃 Eventos Sazonais e Karma

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/evento` | `/event`, `/eventos` | Mostra evento sazonal ativo e recompensas | `gorvax.player` |
| `/karma` | `/reputacao`, `/rep` | Mostra karma/reputação do jogador | `gorvax.player` |

---

## 🏗️ Estruturas, Ranks, Kits e RTP

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/estrutura` | `/structure`, `/poi` | Gerencia estruturas mapeadas no mundo | `gorvax.player` |
| `/rank` | `/ranks`, `/progresso`, `/rankup` | Ver progressão de ranks e requisitos | `gorvax.player` |
| `/kit` | `/kits` | Abre menu de kits disponíveis por rank | `gorvax.player` |
| `/rtp` | `/wild`, `/aleatorio` | Teleporte aleatório para localização segura | `gorvax.player` |

---

## 📚 Códex de Gorvax

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/codex` | `/enciclopedia`, `/wiki` | Abre o Códex de Gorvax (enciclopédia interativa) | `gorvax.player` |

---

## 🛡️ Comandos Administrativos

### Sistema Geral

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/gorvax reload` | Recarrega todas as configurações | `gorvax.admin` |
| `/reino reload` | Recarrega configurações de reinos | `gorvax.admin` |
| `/market reload` | Recarrega configurações do mercado | `gorvax.admin` |

### Sistema de Reinos (Admin)

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/reino darblocos <nick> <qtd>` | Dá blocos de proteção extras | `gorvax.admin` |
| `/reino adm-manutencao` | Força varredura de manutenção | `gorvax.admin` |

### Sistema do The End

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/gorvax reset dragon` | Força reset da batalha do dragão | `gorvax.admin` |
| `/gorvax reset end` | Regenera completamente o The End | `gorvax.admin` |

### Auditoria

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/gorvax audit` | Consulta o log de auditoria | `gorvax.admin` |

### Migração de Storage

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/gorvax migrate <origem> <destino>` | Migra dados entre backends (yaml/sqlite/mysql) | `gorvax.admin` |

---

## ⚙️ Permissões

### Permissões de Jogador

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.player` | Permissões básicas de jogador |
| `gorvax.city.join` | Pode entrar em reinos |
| `gorvax.city.leave` | Pode sair de reinos |
| `gorvax.plot.buy` | Pode comprar lotes |
| `gorvax.plot.sell` | Pode vender lotes |
| `gorvax.reino.nome` | Pode renomear reino (Rei) |
| `gorvax.chat` | Pode usar /chat |
| `gorvax.conquistas` | Pode ver conquistas |
| `gorvax.titulos` | Pode selecionar títulos |
| `gorvax.leilao` | Pode usar sistema de leilão |
| `gorvax.carta` | Pode usar o correio |
| `gorvax.bounty` | Pode usar sistema de bounty |
| `gorvax.nacao` | Pode usar sistema de nações |

### Permissões de Rei

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.mayor` | Permissões de Rei |
| `gorvax.city.create` | Pode criar reinos |
| `gorvax.city.rename` | Pode renomear reinos |
| `gorvax.city.delete` | Pode deletar reinos |
| `gorvax.city.claim` | Pode expandir território |
| `gorvax.city.invite` | Pode convidar membros |

### Permissões Administrativas

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.admin` | Acesso total de administrador |

---

## 📚 Notas Importantes

### 🏗️ Criação de Reinos
1. Segure uma **Pá de Ouro** → `/reino criar` para orientações
2. Clique com botão direito nos **2 cantos opostos** para definir a área
3. Use `/confirmar` ou `/c` para criar

### 🏘️ Criação de Lotes/Feudos
1. Seja Rei ou Vice, esteja dentro do seu reino
2. Segure uma **Pá de Ferro** → Marque os 2 cantos internos
3. Use `/lote criar` para finalizar

### 🔍 Verificação de Terrenos
- **Graveto** + clique direito → Partículas mostram bordas por 10s
- Lotes: **Azul** | Reinos: **Laranja**

### 💰 Economia Dinâmica
- Preços do Mercado Global variam com compras/vendas
- Normalização: 10% decay a cada 5 min
- Range: 0.2× (baixa demanda) — 3.0× (alta demanda)

### 🏠 Sistema de Aluguel
- Cobrança automática **a cada 24h**
- Sem dinheiro → perde o lote
- Rei recebe o valor do aluguel

---

**Versão da Documentação:** 1.0.0  
**Última Atualização:** 2026-03-10  
**Plugin:** GorvaxCore v1.0.0
