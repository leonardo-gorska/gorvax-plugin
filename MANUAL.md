# 📖 Manual Completo — GorvaxCore

> **Versão do Plugin**: v1.0.0
> **Última atualização**: 2026-02-24
> **Idioma**: Português (Brasil)

Este manual documenta **todas** as funcionalidades atualmente implementadas no GorvaxCore.
Cada nova atualização (batch) acrescenta ou modifica seções conforme necessário.

> 📌 **Legenda de Teste**: Seções marcadas com 👥 **requerem 2 contas/jogadores** para testar completamente.

---

## 📋 Índice

1. [Primeiros Passos](#-1-primeiros-passos)
2. [Sistema de Reinos (Cidades)](#-2-sistema-de-reinos-cidades)
3. [Lotes e Feudos (SubPlots)](#-3-lotes-e-feudos-subplots)
4. [Sistema de Permissões (Trust)](#-4-sistema-de-permissões-trust)
5. [Mercado Global (Economia Dinâmica)](#-5-mercado-global-economia-dinâmica)
6. [Mercado Local (Comércio entre Jogadores)](#-6-mercado-local-comércio-entre-jogadores)
7. [World Bosses (Eventos de Boss)](#-7-world-bosses-eventos-de-boss)
7.1. [Mini-Bosses por Bioma](#-71-mini-bosses-por-bioma)
8. [Sistema de Impostos, Banco e Upkeep](#-8-sistema-de-impostos-banco-e-upkeep)
9. [Diplomacia — Alianças e Rivalidades](#-9-diplomacia--alianças-e-rivalidades)
10. [Chat Expandido (Canais de Comunicação)](#-10-chat-expandido-canais-de-comunicação)
11. [Scoreboard Dinâmico e HUD](#-11-scoreboard-dinâmico-e-hud)
12. [Visualização e Proteção de Claims](#-12-visualização-e-proteção-de-claims)
13. [Sistema de Conquistas e Títulos](#-13-sistema-de-conquistas-e-títulos)
14. [Postos Avançados (Outposts)](#-14-postos-avançados-outposts)
15. [Leilão Global](#-15-leilão-global)
16. [Histórico de Preços](#-16-histórico-de-preços)
17. [Estatísticas do Jogador (PlayerData)](#-17-estatísticas-do-jogador-playerdata)
18. [Mensagens Configuráveis](#-18-mensagens-configuráveis-messagesyml)
19. [Reset do The End](#-19-reset-do-the-end)
20. [Log de Auditoria](#-20-log-de-auditoria)
21. [Suporte Bedrock (Floodgate)](#-21-suporte-bedrock-floodgate)
22. [Comandos Administrativos](#-22-comandos-administrativos)
23. [Placeholders (PlaceholderAPI)](#-23-placeholders-placeholderapi)
24. [Permissões do Plugin](#-24-permissões-do-plugin)
25. [Arquivos de Configuração](#-25-arquivos-de-configuração)
26. [Dependências](#-26-dependências)
27. [Guerra entre Reinos](#-27-guerra-entre-reinos)
28. [Integração com Mapa Web (Dynmap / BlueMap)](#-28-integração-com-mapa-web-dynmap--bluemap)
29. [Correio (Mailbox)](#-29-correio-mailbox--carta)
30. [Votação no Reino](#-30-votação-no-reino--reino-votar)
31. [Bounties](#-31-bounties--bounty)
32. [Storage / Banco de Dados](#-32-storage--banco-de-dados)
33. [Sistema de Nações](#-33-sistema-de-nações)
34. [Proteção Anti-Exploit (Semi-Anárquico)](#-34-proteção-anti-exploit-semi-anárquico)
35. [Sistema de Combate](#-35-sistema-de-combate)
36. [Sistema de Duelos](#-36-sistema-de-duelos)
37. [Tutorial Interativo + Welcome Kit](#-37-tutorial-interativo--welcome-kit)
38. [Recompensas Diárias (Daily Rewards)](#-38-recompensas-diárias-daily-rewards--login-streak)
39. [Menu Central GUI](#-39-menu-central-gui)
40. [Leaderboards & Rankings](#-40-leaderboards--rankings)
41. [Integração Discord (Webhook)](#-41-integração-discord-webhook)
42. [Custom Items — Armas Lendárias](#-42-custom-items--armas-e-armaduras-lendárias)
43. [Sistema de Crates / Keys](#-43-sistema-de-crates--keys)
44. [Sistema de Cosméticos](#-44-sistema-de-cosméticos)
45. [Sistema VIP & Ranks Premium](#-45-sistema-vip--ranks-premium)
46. [Battle Pass Sazonal](#-46-battle-pass-sazonal)
47. [Sistema de Reputação / Karma](#-47-sistema-de-reputação--karma)
48. [Resource Pack & Texturas Customizadas](#-48-resource-pack--texturas-customizadas)
49. [Sistema de Estruturas](#-49-sistema-de-estruturas)
50. [Sistema de Lore — Livros & Totems](#-50-sistema-de-lore--livros--totems)
51. [Crates Físicas no Spawn](#-51-crates-físicas-no-spawn)
52. [Quests de Lore — Missões Narrativas](#-52-quests-de-lore--missões-narrativas)
53. [Métricas Anônimas (bStats)](#-53-métricas-anônimas-bstats--b32)
54. [Migração Automática de Configurações (B33)](#-54-migração-automática-de-configurações--b33)
55. [Códex de Gorvax (Enciclopédia Interativa)](#-55-códex-de-gorvax-enciclopédia-interativa)
56. [Sistema de Ranks e Kits](#-56-sistema-de-ranks-e-kits)

---

## 🚀 1. Primeiros Passos

### O que é o GorvaxCore?

O GorvaxCore é um plugin all-in-one para servidores Minecraft Paper 1.21+ que oferece:
- Sistema completo de **reinos e cidades** com claims de terreno
- **Mercado global** com economia dinâmica (oferta/demanda)
- **World Bosses** épicos com loot e ranking de dano
- **Diplomacia** entre reinos (alianças/rivalidades)
- **Chat expandido** com múltiplos canais
- **Conquistas e títulos** desbloqueáveis
- Suporte nativo a jogadores **Java + Bedrock** (via Floodgate)

### Ferramentas Especiais (Itens)

| Item | Como Usar | Função |
|------|-----------|--------|
| 🥇 **Pá de Ouro** | Clique direito em 2 blocos | Seleciona os cantos para criar um **Reino** |
| 🥈 **Pá de Ferro** | Clique direito em 2 blocos | Seleciona os cantos para criar um **Lote (Feudo)** |
| 🪵 **Graveto (Stick)** | Clique direito | Visualiza as bordas dos terrenos próximos |

---

## 🏰 2. Sistema de Reinos (Cidades) 👥

Reinos são a base do GorvaxCore. Um jogador pode criar seu próprio reino, convidar membros (súditos) e governar como Rei.

### Como Criar um Reino

1. Segure uma **Pá de Ouro** na mão
2. Use `/reino criar` para ver as instruções
3. Clique com o botão direito no **primeiro canto** da área desejada
4. Clique com o botão direito no **canto oposto** (diagonal)
5. As bordas da seleção irão brilhar para confirmar
6. Digite `/confirmar` (ou `/c`) para finalizar a criação

> ⚠️ **Custo**: A criação de um reino consome blocos de proteção. Quanto maior a área, mais blocos são necessários.

### Comandos do Sistema de Reinos

#### Comandos para Todos os Jogadores

| Comando | Aliases | O que faz |
|---------|---------|-----------|
| `/reino` | `/kingdom`, `/k`, `/cidade`, `/city`, `/town` | Abre o **menu principal** do reino (GUI interativa) |
| `/reino criar` | — | Mostra tutorial de criação de reino |
| `/reino lista` | — | Lista todos os reinos e terrenos que você possui |
| `/reino spawn` | — | Teleporta ao spawn do seu reino |
| `/reino membros` | — | Lista os súditos e aliados do reino |
| `/reino info` | — | Mostra informações detalhadas do reino atual |
| `/reino aceitar` | — | Aceita um convite pendente para entrar em um reino |
| `/reino recusar` | — | Recusa um convite pendente |
| `/reino debugxp` | — | Mostra tempo de atividade no reino (debug) |

#### Comandos Exclusivos do Rei (Dono)

| Comando | O que faz |
|---------|-----------|
| `/reinonome <nome>` | Define o nome do reino (alias: `/cidadenome`) |
| `/reino setspawn` | Define o ponto de spawn (deve estar dentro do reino) |
| `/reino deletar` | **Deleta permanentemente** o reino (pede confirmação) |
| `/reino transferir <nick>` | Transfere a coroa para outro jogador |
| `/reino convidar <nick>` | Envia convite para um jogador entrar no reino (expira em 60s) |
| `/reino pvp global <on/off>` | Ativa/desativa PvP geral no reino |
| `/reino pvp moradores <on/off>` | Ativa/desativa PvP entre súditos |
| `/reino pvp externo <on/off>` | Ativa/desativa PvP de súditos fora do reino |
| `/reino visitar <nome>` | Teleporta ao spawn de outro reino (se público) |

### Menu GUI do Reino

Ao digitar `/reino`, abre-se um inventário interativo com os seguintes menus:

- **Menu Principal** — Visão geral do reino com ações rápidas
- **Menu de Membros** — Lista de súditos, convites, promoções/rebaixamentos
- **Menu de Lotes** — Gerenciamento de lotes/feudos dentro do reino
- **Menu Administrativo** — Configurações avançadas (PvP, impostos, spawn)
- **Menu de Permissões** — Controle de permissões por cargo
- **Menu de Diplomacia** — Alianças, rivalidades e neutralidade
- **Menu de Confirmação** — Confirmações de ações perigosas (deletar, transferir)

#### ❓ Guia do Rei (B40)

No menu administrativo (Corte Real), existe um botão **❓ Guia do Rei** que envia no chat um guia completo com todos os comandos de governança organizados por categoria (Gerenciamento, PvP, Visitas, Economia, Lotes, Diplomacia, Votação).

#### 🌍 Visitar Reinos (B40)

O menu principal do reino possui um botão **🌍 Visitar Reinos** que mostra informações sobre como visitar reinos públicos de outros jogadores (comando, custo de $500, cooldown de 60s).

#### Lores Descritivas (B39/B40)

Todos os botões dos menus de reino possuem lores detalhadas com:
- Descrição do que cada botão faz
- Dicas de uso (como criar lotes, quais permissões existem)
- Status atual (ex: permissões PERMITIDO/NEGADO)
- Atalhos e comandos relevantes


### Ranking de Reinos

O nível do reino é calculado automaticamente pelo número de súditos:

| Súditos | Rank |
|---------|------|
| 0–2 | ⛺ Acampamento |
| 3–5 | 🏘️ Vila |
| 6–10 | 🏙️ Cidade |
| 11–15 | 🌆 Metrópole |
| 16+ | 👑 Império |

### Convites

- O Rei pode convidar jogadores com `/reino convidar <nick>`
- O convidado recebe uma notificação com som (`ENTITY_PLAYER_LEVELUP`)
- O convite expira em **60 segundos**
- O jogador aceita com `/reino aceitar` ou recusa com `/reino recusar`
- Também é possível convidar via menu GUI (botão "Convidar Súdito")

### Visitas

- `/reino visitar <nome>` teleporta ao spawn de um reino **público**
- **Custo** configurável (padrão: $500)
- **Cooldown** de 60s entre teleportes
- **Warmup** de 5s (cancelado se o jogador se mover)
- O Rei pode tornar o reino privado via GUI (flag `isPublic`)

---

## 🏘️ 3. Lotes e Feudos (SubPlots)

Dentro de um reino, o Rei ou Vice pode dividir a área em **lotes** (subplots) que podem ser vendidos ou alugados a outros jogadores.

### Como Criar um Lote

1. Você deve ser **Rei** ou **Vice** do reino
2. Esteja dentro do território do reino
3. Segure uma **Pá de Ferro** na mão
4. Clique nos **dois cantos** da área interna desejada
5. Digite `/lote criar`
6. Configure preço/aluguel depois

### Compra e Aluguel

- **Comprar**: `/lote comprar` — compra o lote se estiver à venda
- **Alugar**: `/lote alugar` — aluga o lote (pagamento diário automático)
  - Aluguel é cobrado a cada **24 horas** automaticamente
  - Se o jogador não tiver dinheiro, **perde o lote**
  - O valor do aluguel vai para o Rei

### Comandos de Lotes

| Comando | Aliases | O que faz | Quem pode |
|---------|---------|-----------|-----------|
| `/lote` | `/plot`, `/subplot`, `/terreno`, `/feudo` | Abre o menu do lote | Todos (dentro do lote) |
| `/lote info` | — | Informações detalhadas do lote | Todos |
| `/lote comprar` | — | Compra o lote atual | Jogadores com dinheiro |
| `/lote alugar` | — | Aluga o lote atual | Jogadores com dinheiro |
| `/lote abandonar` | — | Abandona o lote | Dono do lote |
| `/lote amigo <nick>` | — | Dá permissão GERAL no lote | Dono do lote |
| `/lote criar` | — | Cria um novo lote na seleção | Rei ou Vice |
| `/lote retomar` | — | Confisca o lote de alguém | Rei ou Vice |
| `/lote preco <valor>` | — | Define preço de venda | Rei, Vice ou Dono |
| `/lote aluguel <valor>` | — | Define valor diário do aluguel | Rei, Vice ou Dono |
| `/lote deletar` | — | Deleta o lote permanentemente | Rei |

---

## 🔐 4. Sistema de Permissões (Trust) 👥

O GorvaxCore possui um sistema de confiança (trust) que permite compartilhar acesso a terrenos com outros jogadores.

### Tipos de Permissão

| Tipo | O que permite |
|------|---------------|
| **GERAL** | Tudo (construção + contêiner + acesso). Permissão superior. |
| **CONSTRUÇÃO** | Colocar e quebrar blocos |
| **CONTÊINER** | Abrir baús, fornalhas, barris, funis, etc. |
| **ACESSO** | Usar portas, botões, alavancas, placas de pressão |
| **VICE** | Gerenciar lotes, permissões e membros (somente em reinos) |

### Comandos

| Comando | Aliases | O que faz |
|---------|---------|-----------|
| `/permitir <nick> [tipo]` | `/trust` | Dá permissão no terreno (padrão: GERAL) |
| `/remover <nick>` | `/untrust` | Remove todas as permissões de um jogador |

### Exemplos

```
/permitir Steve           → Dá GERAL para Steve
/permitir Alex CONTÊINER  → Dá apenas acesso a baús para Alex
/permitir Herobrine VICE  → Torna Herobrine Vice (apenas em reinos)
/remover Steve            → Remove tudo de Steve
```

---

## 💰 5. Mercado Global (Economia Dinâmica)

O Mercado Global é um sistema de compra e venda de itens com **preços que variam automaticamente** baseados na oferta e demanda.

### Como Acessar

| Comando | Aliases |
|---------|---------|
| `/market` | `/loja`, `/mercado` |

### Como Funciona

- **Compras** fazem o preço **subir** (alta demanda)
- **Vendas** fazem o preço **cair** (excesso de oferta)
- Preços se **normalizam automaticamente** (decay de 10% a cada 5 minutos)
- **Multiplicador máximo**: 3.0x do preço base
- **Multiplicador mínimo**: 0.2x do preço base

### Categorias

Os itens são organizados em categorias no menu GUI:
- ⛏️ Minerais (diamante, ferro, ouro, etc.)
- 🌾 Farm (trigo, cenoura, batata, etc.)
- 💀 Drops de Mob (pólvora, osso, pérola do End, etc.)
- 🪵 Madeiras e Construção
- E outras categorias conforme configuração

### Interface

- Menu GUI organizado por categorias
- Clique esquerdo: **comprar**
- Clique direito: **vender**
- Shift+Clique: comprar/vender em **lotes maiores**
- Preços atualizados em tempo real

### Feedback Sonoro

- **Compra bem-sucedida**: som `ENTITY_ITEM_PICKUP`
- **Venda bem-sucedida**: som `ENTITY_VILLAGER_YES` + partículas de esmeralda

---

## 🏪 6. Mercado Local (Comércio entre Jogadores) 👥

O Mercado Local permite que jogadores vendam itens entre si dentro de um reino.

### Como Acessar

| Comando | O que faz |
|---------|-----------|
| `/market local` | Abre o mercado do reino onde você está |
| `/market historico` | Mostra as últimas 50 transações pessoais |

### Como Funciona

- Jogadores definem seus próprios preços
- **Taxa municipal**: uma porcentagem vai para o Rei do reino
- Sistema de **busca por nome** de item
- Itens reais do jogador são usados (o item é removido ao listar)

---

## 👹 7. World Bosses (Eventos de Boss)

O GorvaxCore possui um sistema completo de World Bosses — criaturas épicas que spawnam no mundo e requerem cooperação para derrotar.

### Bosses Disponíveis

| Boss | Descrição |
|------|-----------|
| 👑 **Rei Gorvax** (`rei_gorvax`) | O tirano antigo (5000+ HP). Boss principal do servidor. |
| 🌊 **Indrax Abissal** (`indrax_abissal`) | Entidade sombria das profundezas |
| 🔥 **Vulgathor** (`vulgathor`) | Demônio vulcânico |
| 💀 **Skulkor** (`skulkor`) | Esqueleto colossal |
| 🌿 **Xylos Devorador** (`xylos_devorador`) | Criatura devoura-mundos |
| ⚡ **Zarith** (`zarith`) | Entidade elemental |
| 🧊 **Kaldur** (`kaldur`) | Senhor do gelo |

### Como Funciona

1. Bosses spawnam **automaticamente** entre 3-7 horas (aleatório por padrão)
2. **Avisos globais** são emitidos antes do spawn
3. Jogadores causam dano e o boss usa ataques em **fases** (HP diminuindo)
4. Ao morrer, cria um **baú de loot** com recompensas
5. Loot é distribuído por **ranking de dano** (quem mais contribuiu ganha mais)

### Ranking de Loot

| Posição | Qualidade do Loot |
|---------|-------------------|
| 🥇 1º lugar | Melhor loot (itens raros/únicos) |
| 🥈 2º lugar | Loot excelente |
| 🥉 3º lugar | Loot bom |
| 4º–5º lugar | Loot moderado |
| 6º+ | Loot de participação |

### Agendamento de Bosses (B11)

- Bosses podem ter **horários fixos** semanais (ex: "SATURDAY 20:00 king_gorvax")
- Avisos automáticos: **30min**, **10min**, **5min** antes do evento
- O spawn aleatório é mantido como opção complementar

### Raids (Ondas de Bosses)

- **Boss Raid**: múltiplos bosses spawnam em sequência (ondas)
- BossBar global mostrando "Onda 1/5", "Onda 2/5", etc.
- Loot acumulativo — mais ondas = melhor loot
- Requer mínimo de **3 jogadores** na área para iniciar

### Bosses Sazonais/Temáticos

- Bosses especiais para datas comemorativas (Natal, Halloween, Páscoa)
- Ativos apenas em datas configuráveis
- Loot temático exclusivo com lore especial

### Atmosfera de Boss

Quando um boss está ativo, a região ganha efeitos ambientais:
- 🌑 Escuridão/trevas no entorno
- 🌩️ Raios e partículas
- 🔊 Sons ambiente imersivos
- 💨 Neblina visual

### Comandos de Boss

| Comando | O que faz | Quem pode |
|---------|-----------|-----------|
| `/boss next` | Mostra tempo para o próximo boss | Todos |
| `/boss list` | Lista bosses vivos no mundo | Todos |
| `/boss start` | Inicia evento de spawn aleatório | Admin |
| `/boss spawn [id]` | Spawna um boss específico | Admin |
| `/boss kill` | Remove todos os bosses ativos | Admin |
| `/boss status` | Status detalhado dos bosses vivos | Admin |
| `/boss reload` | Recarrega configs de boss e loot | Admin |
| `/boss testloot <boss> <rank>` | Testa o loot de um boss | Admin |

---

## ⚔️ 7.1. Mini-Bosses por Bioma

Mini-Bosses são criaturas poderosas que spawnam **naturalmente** em biomas específicos. Diferente dos World Bosses, são combates menores pensados para **1-3 jogadores**.

### Características

- **Spawn natural**: aparecem automaticamente perto de jogadores no bioma correto
- **HP reduzido**: 250-500 (vs. 4000+ dos World Bosses)
- **Sem BossBar global**: nome com HP visível acima da entidade
- **Loot direto**: itens dropam no chão ao morrer
- **Recompensa proporcional**: dinheiro distribuído pelo dano causado
- **Habilidades únicas**: cada mini-boss tem skills especiais

### Mini-Bosses Disponíveis

| Mini-Boss | Biomas | HP | Habilidades |
|-----------|--------|---:|-------------|
| 🏜️ Guardião do Deserto | Deserto, Badlands | 300 | Tempestade de Areia, Invocar Servos |
| ❄️ Sentinela Gélida | Neve, Gelo, Taiga Nevada | 400 | Nova Glacial, Flechas de Gelo |
| 🕷️ Araña da Selva | Selva, Bambuzal | 250 | Armadilha de Teias, Nuvem Venenosa |
| 🔥 Fantasma do Nether | Nether (todos biomas) | 500 | Chuva de Fogo, Escudo de Chamas |

### Recompensas

- **Dinheiro**: $450-800 (proporcional ao dano causado)
- **XP**: 120-250 pontos
- **Loot**: itens temáticos com chance configurável (ex: Netherite Scrap 10%, Nether Star 2%)

### Comandos de Mini-Boss

| Comando | O que faz | Quem pode |
|---------|-----------|-----------|
| `/miniboss list` | Lista mini-bosses ativos | Todos |
| `/miniboss spawn <id>` | Spawna um mini-boss na sua posição | Admin |
| `/miniboss kill` | Remove todos os mini-bosses ativos | Admin |
| `/miniboss reload` | Recarrega configurações | Admin |

### Configuração

As definições de mini-bosses estão em `mini_bosses.yml`, incluindo:
- Tipo de entidade, HP, dano, velocidade, escala
- Biomas de spawn, equipamento, efeitos on-hit
- Habilidades com cooldown e raio de alcance
- Tabela de loot com chances individuais

---

## 🏦 8. Sistema de Impostos, Banco e Upkeep

Os reinos possuem um sistema econômico completo de manutenção.

### Banco do Reino

Cada reino tem um **banco próprio** separado do saldo pessoal do Rei.

| Comando | O que faz | Quem pode |
|---------|-----------|-----------|
| `/reino depositar <valor>` | Deposita dinheiro pessoal no banco do reino | Qualquer membro |
| `/reino sacar <valor>` | Saca dinheiro do banco | Apenas o Rei |
| `/reino banco` | Mostra saldo, entradas e saídas recentes | Qualquer membro |

### Imposto Diário sobre Súditos

- Cada súdito paga uma **taxa diária** ao banco do reino (configurável, padrão: $50)
- O Rei pode ajustar a taxa via menu GUI
- Jogador sem saldo recebe **aviso**
- **3 dias** sem pagar = expulso automaticamente do reino

### Upkeep (Custo por Chunk)

- Cada chunk claimado tem um custo diário (configurável, padrão: $10/chunk)
- Cobrado do **banco do reino** (não do Rei pessoalmente)
- Se o banco ficar zerado:
  - **7 dias** sem pagar → reino entra em "Decadência" (perde buffs)
  - **14 dias** sem pagar → claims começam a ser liberados (do mais novo ao mais antigo)

### Limite de Membros por Nível

O número máximo de membros cresce com o nível do reino:

| Nível | Máximo de membros |
|-------|-------------------|
| 1 | 5 |
| 2 | 8 |
| 3 | 11 |
| N | 5 + (N-1) × 3 |

O nível é calculado por: chunks claimados + buffs + tempo ativo. Slots podem ser comprados via banco do reino.

---

## 🤝 9. Diplomacia — Alianças e Rivalidades 👥

Reinos podem manter relações diplomáticas entre si.

### Tipos de Relação

| Relação | Efeito |
|---------|--------|
| **Neutro** 🟡 | Regras normais de PvP (padrão) |
| **Aliado** 🟢 | PvP desativado entre súditos, acesso a terrenos aliados, chat de aliança compartilhado |
| **Inimigo** 🔴 | PvP sempre ativo (mesmo em claims com PvP off), sem acesso a terrenos |
| **Guerra** ⚔️ | PvP forçado, sistema de pontos, espólios ao final (ver seção 27) |
| **Trégua** ⚪ | Estado temporário pós-guerra |

### Comandos de Diplomacia

| Comando | O que faz |
|---------|-----------|
| `/reino alianca <reino>` | Envia proposta de aliança (o outro reino precisa aceitar) |
| `/reino alianca aceitar` | Aceita uma proposta de aliança pendente |
| `/reino alianca recusar` | Recusa uma proposta de aliança |
| `/reino inimigo <reino>` | Declara inimizade (unilateral — não precisa de aceite) |
| `/reino neutro <reino>` | Retorna a relação ao neutro |

### Menu de Diplomacia

Acessível pelo menu principal do reino:
- Lista todos os reinos com ícone de relação (🟢 aliado, 🔴 inimigo, ⚪ neutro)
- Clique para propor aliança, declarar guerra ou resetar relação
- Mostra estatísticas de cada reino (membros, nível, força)

### Efeitos no Jogo

- Ao entrar em território inimigo: mensagem de aviso "território inimigo"
- Aliados podem acessar terrenos aliados (ACESSO trust automático)
- Chat de aliança disponível (`/ac`)

---

## 💬 10. Chat Expandido (Canais de Comunicação) 👥

O GorvaxCore possui um sistema completo de canais de chat com formatação por hierarquia.

### Canais Disponíveis

| Canal | Comando Rápido | Alcance | Cor |
|-------|----------------|---------|-----|
| 🌍 **Global** | `/g <msg>` | Todos os jogadores | Branco |
| 🏰 **Reino** | `/rc <msg>` | Membros do mesmo reino | Ciano |
| 🤝 **Aliança** | `/ac <msg>` | Membros de reinos aliados | Verde |
| 📍 **Local** | `/l <msg>` | Jogadores em 200 blocos (configurável) | Amarelo |
| 💰 **Comércio** | `/tc <msg>` | Todos (canal de negociações) | Dourado |

### Como Usar

**Enviar mensagem rápida** (sem mudar canal padrão):
```
/g Olá a todos!          → Global
/rc Reunião no castelo!  → Só membros do reino
/l Alguém aqui perto?    → Local (200 blocos)
```

**Mudar canal padrão** (todas suas mensagens vão para aquele canal):
```
/chat global     → Muda para Global
/chat reino      → Muda para Reino
/chat alianca    → Muda para Aliança
/chat local      → Muda para Local
/chat comercio   → Muda para Comércio
/chat            → Mostra qual canal está ativo
```

### Formatação do Chat

As mensagens são formatadas com a hierarquia do reino:

```
[Título] [Tag do Reino] 👑 Rei NomeJogador: mensagem
[Título] [Tag do Reino] ⚔️ Vice NomeJogador: mensagem
[Título] [Tag do Reino] 🛡️ Nobre NomeJogador: mensagem
[Título] [Tag do Reino] 🏠 Súdito NomeJogador: mensagem
```

### Anti-Spam e Filtro de Palavras (B9)

O chat possui proteção automática contra spam e conteúdo proibido:

#### Rate Limiter (Anti-Spam)

- Máximo de **3 mensagens a cada 5 segundos** (configurável)
- **Mensagens duplicadas** consecutivas são bloqueadas
- Jogadores com **mute temporário** não podem enviar mensagens

#### Filtro de Palavras

Palavras/padrões configurados em `config.yml` são filtrados automaticamente:

| Ação | Comportamento |
|------|---------------|
| `block` | Mensagem bloqueada completamente |
| `censor` | Palavras proibidas substituídas por `***` |
| `mute` | Jogador mutado temporariamente (ex: 300s) |

#### Configuração (`config.yml`)

```yaml
chat:
  rate_limit:
    messages: 3       # Máx mensagens na janela
    seconds: 5        # Janela em segundos
  blocked_words:
    - "(?i)palavrao"  # Padrões regex
  filter:
    action: "censor"       # block, censor, mute
    mute_duration: 300     # Segundos (só para action: mute)
```

> 💡 Jogadores com permissão `gorvax.chat.bypass` ignoram todas as verificações.

### Sistema de Ignorar (/ignore)

Jogadores podem ocultar mensagens de outros jogadores em **todos os canais de chat**.

#### Comandos

| Comando | Aliases | O que faz |
|---------|---------|-----------|
| `/ignore <nick>` | `/ignorar` | Adiciona/remove jogador da lista de ignorados (toggle) |
| `/ignore list` | `/ignorar list` | Lista todos os jogadores ignorados |

#### Como Funciona

- **Limite**: máximo de **50 jogadores** ignorados
- **Persistente**: a lista é salva no `playerdata.yml` e mantida entre sessões
- **Toggle**: usar `/ignore <nick>` novamente remove o jogador da lista
- **Abrange todos os canais**: Global, Reino, Aliança, Local, Comércio e Nação
- **Não afeta staff**: admins não podem ser ignorados (futuro)

#### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.chat.bypass` | Ignora rate limit, filtro e checagens de spam |
| `gorvax.ignore` | Permite usar o comando `/ignore` |

---

## 📊 11. Scoreboard Dinâmico e HUD

O GorvaxCore exibe informações relevantes na sidebar e actionbar do jogador.

### Sidebar Contextual

A sidebar muda automaticamente conforme o contexto do jogador:

**🟢 Padrão** (em qualquer lugar):
- Nome do reino (ou "Sem Reino")
- Blocos de proteção disponíveis
- Saldo (Vault)
- Jogadores online

**📍 Em Claim** (ao entrar em terreno):
- Nome do terreno/lote
- Dono do terreno
- Tipo de permissão do jogador

**👹 Durante Boss** (perto de um World Boss):
- Nome do boss + barra de HP
- Ranking de dano do jogador
- Tempo restante do evento

### ActionBar

- Ao entrar em claim: `§a[Entrando] Reino Imperial`
- Ao sair de claim: `§c[Saindo] Zona Selvagem`
- Durante combate com boss: dano total acumulado

### Toggle

O jogador pode ligar/desligar o HUD:
```
/gorvax hud
```

---

## 🛡️ 12. Visualização e Proteção de Claims

### Proteção Automática

Dentro de claims, o plugin protege automaticamente contra:
- ❌ Quebrar/colocar blocos (sem permissão de CONSTRUÇÃO)
- ❌ Abrir baús, fornalhas, barris (sem permissão de CONTÊINER)
- ❌ Usar portas, botões, alavancas (sem permissão de ACESSO)
- ❌ PvP (se desativado nas configurações do reino)
- ❌ Pisoteio de Farmland (crops protegidas)
- ❌ Dano causado por mobs em claims
- ❌ Explosões (TNT, Creepers, End Crystal, Bed/Anchor)
- ❌ Fluxo de água/lava de fora do claim
- ❌ Spawn de Wither dentro de claims
- ❌ TNT Cannon de fora do claim (tracking de origem)
- ❌ Teleporte via Chorus Fruit para dentro de claim alheio
- ❌ Lag machines de redstone (limite por chunk)

### Visualização de Fronteiras

Segure um **Graveto (Stick)** e clique com o botão direito para ver as bordas dos terrenos próximos:

| Tipo de Terreno | Cor das Partículas |
|-----------------|-------------------|
| Reinos | 🟠 Laranja (`FLAME`) |
| Lotes à venda | 🟢 Verde (`HAPPY_VILLAGER`) |
| Lotes alugados | 🔵 Azul (`DRIP_WATER`) |
| Claims pessoais | ⚪ Branco (`END_ROD`) |
| Claims de inimigos | 🔴 Vermelho (`REDSTONE`) |

**Duração**: 15 segundos (configurável)

### Mapa de Claims

Digite `/gorvax mapa` para ver um mini-mapa ASCII do entorno (15×15 chunks):

```
█ = seu claim    ▓ = aliado    ░ = inimigo    · = sem dono
```

### Feedback ao Cruzar Fronteiras

- **Entrar em claim**: Title suave com nome do terreno/reino + dono
- **Sair de claim**: ActionBar "§7Saindo de [Nome do Terreno]"
- **Som** sutil ao cruzar fronteira (configurável on/off por jogador)

### Feedback Sonoro por Ação

| Ação | Som |
|------|-----|
| Claim criado | `ENTITY_EXPERIENCE_ORB_PICKUP` + partículas verdes |
| Entrar em reino | `BLOCK_NOTE_BLOCK_CHIME` + title com nome |
| Permissão negada | `ENTITY_VILLAGER_NO` |

---

## 🏆 13. Sistema de Conquistas e Títulos

### Conquistas

O GorvaxCore possui um sistema de conquistas desbloqueáveis definidas em `achievements.yml`.

**Categorias**: Exploração, Combate, Economia, Social, Reinos

**Exemplos de conquistas**:

| Conquista | Condição | Recompensa |
|-----------|----------|------------|
| 🏗️ Primeiro Passo | Criar primeiro claim | 50 blocos |
| 👑 Rei dos Reis | Criar primeiro reino | $1.000 |
| 🐉 Caçador de Dragões | Participar de 5 bosses | Título "Caçador" |
| 💰 Magnata | Ganhar $50.000 no mercado | Título "Magnata" |
| 👥 Socialite | Ter 10+ súditos no reino | Título "Líder Nato" |
| ⏰ Veterano | 100h de jogo | Título "Veterano" |

**Ao desbloquear**: som + title na tela + fogos de artifício

### Menu de Conquistas

```
/conquistas
```

Abre um menu GUI de 54 slots com todas as conquistas, organizadas por categoria, mostrando barra de progresso.

### Títulos

Títulos são desbloqueados via conquistas e exibidos no chat:

```
/titulos
```

Abre menu de seleção de títulos. O título ativo aparece no chat:

```
[Magnata] [Atlantis] 👑 Steve: Olá!
```

---

## ⚔️ 14. Postos Avançados (Outposts)

Outposts permitem que reinos reivindiquem áreas **não contíguas** ao território principal. Ideal para proteger minas distantes, farms externas ou bases secretas.

### Requisitos

- Ser **Rei** do reino
- Nível mínimo do reino (configurável)
- Distância mínima do claim principal: **200 blocos** (configurável)
- Blocos de proteção suficientes (custo = área × 3.0 multiplicador)

### Limite de Outposts por Nível

| Nível do Reino | Outposts Permitidos |
|----------------|---------------------|
| 1–2 | 0 |
| 3–4 | 1 |
| 5–6 | 2 |
| 7+ | 3 |

### Como Criar um Outpost

1. Vá até a área desejada (longe do claim principal)
2. Digite `/reino outpost criar`
3. Demarque a área com a **Pá de Ouro** (como um claim normal)
4. Confirme com `/confirmar` (ou `/c`)
5. O custo em blocos será: área × multiplicador de custo

### Comandos de Outpost

| Comando | Aliases | O que faz |
|---------|---------|-----------|
| `/reino outpost criar` | `/reino posto criar` | Inicia fluxo de criação de outpost |
| `/reino outpost listar` | `/reino posto listar` | Lista todos os outposts do reino |
| `/reino outpost spawn [nome]` | — | Teleporta para um outpost |
| `/reino outpost setspawn` | — | Define o spawn do outpost (esteja dentro dele) |
| `/reino outpost deletar` | — | Deleta o outpost onde você está (reembolsa blocos) |

### Configuração (`config.yml`)

```yaml
outposts:
  cost_multiplier: 3.0       # Custo = área × multiplicador
  min_distance: 200           # Distância mínima do claim principal
  max_per_level:
    1: 0    # Nível 1-2: sem outpost
    3: 1    # Nível 3-4: 1 outpost
    5: 2    # Nível 5-6: 2 outposts
    7: 3    # Nível 7+: 3 outposts
```

### Comportamento

- Outposts são **removidos automaticamente** quando o reino é deletado
- Chunks de outposts contam para o **nível do reino**
- Blocos são **reembolsados** ao deletar um outpost
- Proteção e permissões funcionam como claims normais

---

## 🔨 15. Leilão Global

O sistema de leilão permite que jogadores leiloem itens para todos no servidor.

### Como Funciona

1. Segure o item na mão e use `/leilao iniciar <preço> [duração]`
2. Outros jogadores dão lances com `/leilao lance <valor>`
3. Nos últimos 30s, o leilão é anunciado (broadcast)
4. **Anti-snipe**: lances nos últimos 15 segundos estendem o tempo em 15s
5. Vencedor coleta o item; vendedor recebe dinheiro (menos taxa de 5%)

### Comandos

| Comando | O que faz |
|---------|-----------|
| `/leilao iniciar <preço> [duração]` | Cria um leilão com o item da mão |
| `/leilao lance <valor> [ID]` | Dá um lance em um leilão |
| `/leilao listar` | Abre GUI com leilões ativos |
| `/leilao coletar` | Resgata itens e dinheiro pendentes |
| `/leilao cancelar <ID>` | Cancela um leilão (sem lances) |

> ℹ️ **Aliases**: `/auction`, `/leilão`

### GUI de Leilões

A GUI paginada mostra:
- Item leiloado com lore detalhada
- Preço atual e lance mínimo
- Tempo restante
- Nome do vendedor e do maior lance
- Botões de navegação e ações rápidas

### Configuração (`config.yml`)

```yaml
auction:
  enabled: true
  default_duration: 300        # 5 minutos
  max_duration: 3600            # 1 hora máximo
  min_price: 10.0               # Preço mínimo
  bid_increment: 1.0            # Incremento mínimo
  tax_percent: 5.0              # Taxa sobre venda
  anti_snipe_seconds: 15        # Extensão anti-snipe
  broadcast_last_seconds: 30    # Broadcast nos últimos X segundos
  max_active_per_player: 3      # Máximo de leilões simultâneos
```

### Persistência

Dados salvos em `auction_data.yml` automaticamente.

---

## 📊 16. Histórico de Preços

O sistema de histórico de preços registra snapshots dos preços do mercado global e exibe tendências.

### Indicador de Tendência (GUI)

Na GUI do mercado global (tela de categoria), cada item mostra:

```
§7Tendência: §a↑ +15.3%    (subiu)
§7Tendência: §c↓ -8.2%     (caiu)
§7Tendência: §7— Estável    (sem variação)
```

### Comando de Histórico

| Comando | O que faz |
|---------|-----------|
| `/market preco <item>` | Mostra histórico textual de preços |

Exibe snapshots recentes com preços de compra e venda e variações.

### Configuração (`config.yml`)

```yaml
price_history:
  enabled: true
  snapshot_interval: 6000       # Ticks entre snapshots (5 min)
  retention_hours: 24           # Horas de retenção
  trend_hours: 6                # Janela de cálculo de tendência
```

### Persistência

Dados salvos em `price_history.yml` automaticamente.

---

## 📈 17. Estatísticas do Jogador (PlayerData)

Cada jogador tem um perfil completo de estatísticas persistentes:

| Estatística | Descrição |
|-------------|-----------|
| `firstJoin` | Data do primeiro login |
| `totalPlayTime` | Tempo total jogado |
| `lastLogin` | Último login |
| `totalBlocksBroken` | Blocos quebrados |
| `totalBlocksPlaced` | Blocos colocados |
| `totalKills` | Kills PvP |
| `totalDeaths` | Mortes |
| `bossesKilled` | Bosses mortos (participação) |
| `bossTopDamage` | Vezes no top 3 de dano |
| `totalMoneyEarned` | Dinheiro total ganho no mercado |
| `totalMoneySpent` | Dinheiro total gasto |
| `activeTitle` | Título ativo no chat |
| `unlockedTitles` | Títulos desbloqueados |
| `achievements` | Conquistas com progresso |
| `claimBlocks` | Blocos de proteção disponíveis |

**Salvamento automático**: a cada 5 minutos (async, sem lag)

### Ganho de Blocos de Proteção

- Jogadores ganham blocos automaticamente: **100 blocos a cada 60 minutos** de jogo (configurável)
- Administradores podem dar blocos: `/reino darblocos <nick> <qtd>`

---

## ✉️ 18. Mensagens Configuráveis (messages.yml)

Todas as mensagens exibidas pelo plugin são **completamente configuráveis** pelo arquivo `messages.yml`.

### Estrutura

O `messages.yml` organiza as mensagens por seção:
- `protection` — mensagens de proteção
- `kingdom` — mensagens de reinos
- `plot` — mensagens de lotes
- `trust` — mensagens de permissões
- `market` — mensagens do mercado
- `boss` — mensagens de bosses
- `admin` — mensagens administrativas
- `end_reset` — mensagens do reset do End
- `scoreboard` — linhas do scoreboard
- `chat` — mensagens dos canais de chat
- `achievement` — mensagens de conquistas
- `audit` — mensagens de auditoria
- `outpost` — mensagens de outposts
- `auction` — mensagens do sistema de leilão
- `price_history` — mensagens do histórico de preços
- `war` — mensagens do sistema de guerra
- `combat` — mensagens do sistema de combate (B2)

### Placeholders nas Mensagens

As mensagens suportam placeholders com `{0}`, `{1}`, etc.:

```yaml
kingdom.invite_sent: "§b[Gorvax] §fConvite enviado para §a{0}§f!"
```

---

## 🌀 19. Reset do The End

O GorvaxCore gerencia o **reset automático** da dimensão The End.

### Funcionalidades

- **Reset agendado** em horários configuráveis
- **Avisos automáticos** antes do reset (broadcast para todos os jogadores)
- **Teleporte de segurança** de jogadores que estejam no The End antes do reset
- **Regeneração completa** da dimensão (deleta e regenera os chunks)
- **Reset da batalha do Ender Dragon** (regenera cristais, obsidiana, portal)

### Comandos

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/gorvax reset dragon` | Força reset da batalha do dragão | Admin |
| `/gorvax reset end` | Regenera completamente o The End | Admin |

---

## 📋 20. Log de Auditoria

O GorvaxCore mantém um log interno de ações importantes para análise administrativa.

### O que é Registrado

- Criação e deleção de claims
- Mudanças de rei (transferências)
- Transações de mercado acima de um valor mínimo
- Kicks e bans de reino
- Mudanças de permissões
- Mudanças de diplomacia

### Consulta de Logs

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/gorvax audit [reino\|player] [filtro]` | Consulta logs de auditoria | Admin |
| `/market historico` | Histórico de transações pessoais | Todos |

### Limites

- Máximo de **1000 entradas** no arquivo `audit_log.yml`
- Rotação automática (entradas antigas são descartadas)
- Salvamento assíncrono (sem perda de performance)

---

## 📱 21. Suporte Bedrock (Floodgate)

O GorvaxCore detecta automaticamente jogadores **Bedrock** (mobile/console) via Floodgate API e adapta a experiência.

### Adaptações para Bedrock

| Funcionalidade | Java Edition | Bedrock Edition |
|---------------|-------------|-----------------|
| Input de texto | AnvilGUI | Formulários nativos (CustomForm) |
| Menus de seleção | Inventário GUI | SimpleForm nativo |
| Confirmações | GUI de 2 opções | ModalForm (Sim/Não) |
| Tamanho de menu | 54 slots (6 linhas) | 27 slots (3 linhas) com paginação |
| Mensagens de ação | "Clique direito" | "Toque no bloco" |
| Shift+Clique | "Shift+Clique" | "Agache + Toque" |

### Detecção Automática

O plugin detecta automaticamente se o jogador é Bedrock através de `FloodgateApi.getInstance().isFloodgatePlayer(uuid)` — não é necessário nenhuma configuração manual.

---

## 🔧 22. Comandos Administrativos

Comandos restritos para operadores e administradores do servidor.

### Geral

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/gorvax reload` | Recarrega todas as configurações | `gorvax.admin` |
| `/gorvax help [página]` | Mostra ajuda paginada | Todos |
| `/gorvax hud` | Liga/desliga o scoreboard HUD | Todos |
| `/gorvax mapa` | Mostra mapa ASCII de claims (15×15 chunks) | Todos |
| `/gorvax som` | Liga/desliga som ao cruzar fronteira de claim | Todos |
| `/gorvax audit` | Consulta logs de auditoria | `gorvax.admin` |
| `/gorvax migrate <origem> <destino>` | Migra dados entre backends (yaml/sqlite/mysql) | `gorvax.admin` |
| `/confirmar` (ou `/c`) | Confirma uma ação pendente | Contexto |

### Reinos (Admin)

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/reino reload` | Recarrega configs de reinos | `gorvax.admin` |
| `/reino darblocos <nick> <qtd>` | Dá blocos de proteção extras | `gorvax.admin` |
| `/reino adm-manutencao` | Força varredura de manutenção | `gorvax.admin` |

### Mercado (Admin)

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/market reload` | Recarrega configs do mercado | `gorvax.admin` |

### The End (Admin)

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/gorvax reset dragon` | Força reset da batalha do dragão | `gorvax.admin` |
| `/gorvax reset end` | Regenera completamente o The End | `gorvax.admin` |

### Bosses (Admin)

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/boss start` | Inicia evento de spawn aleatório | `gorvax.admin` |
| `/boss spawn [id]` | Spawna um boss específico ou aleatório | `gorvax.admin` |
| `/boss kill` | Remove todos os bosses ativos | `gorvax.admin` |
| `/boss status` | Status detalhado de bosses vivos | `gorvax.admin` |
| `/boss reload` | Recarrega configurações de boss | `gorvax.admin` |
| `/boss testloot <boss> <rank>` | Testa loot de um boss | `gorvax.admin` |

---

## 🧩 23. Placeholders (PlaceholderAPI)

> **Requisito**: Plugin **PlaceholderAPI** instalado no servidor. O GorvaxCore registra os placeholders automaticamente.

### Lista Completa

#### Localização e Status

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_localizacao_label%` | Nome formatado do local atual | `§aReino Imperial §8(§b✔§8)` |
| `%gorvax_localizacao_tab%` | Versão compacta para TAB | `§bReino Imperial §8\| §eSede Real` |
| `%gorvax_status_protecao%` | Status de proteção | `§a§lPROTEGIDO` |

#### Blocos de Proteção

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_blocos_total%` | Total de blocos de proteção | `2500` |
| `%gorvax_blocos_disponiveis%` | Blocos disponíveis | `1200` |
| `%gorvax_blocos_usados%` | Blocos já utilizados | `1300` |
| `%gorvax_territorio_desc%` | Descrição curta | `§fDisp: §a1200` |

#### Reino

| Placeholder | Alias Legado | Descrição |
|-------------|-------------|-----------|
| `%gorvax_reino_nome%` | `%gorvax_cidade_nome%` | Nome do reino |
| `%gorvax_reino_rei%` | `%gorvax_cidade_prefeito%` | Nome do Rei |
| `%gorvax_reino_suditos%` | `%gorvax_cidade_moradores%` | Número de súditos |
| `%gorvax_reino_rank%` | `%gorvax_cidade_rank%` | Rank do reino |
| `%gorvax_reino_tag%` | — | Tag (abreviação) do reino |
| `%gorvax_reino_tag_color%` | — | Cor da tag |

#### Estatísticas do Jogador

| Placeholder | Descrição |
|-------------|-----------|
| `%gorvax_playtime%` | Tempo total jogado formatado (ex: "3d 5h") |
| `%gorvax_kills%` | Kills PvP |
| `%gorvax_deaths%` | Mortes |
| `%gorvax_kdr%` | Kill/Death Ratio |
| `%gorvax_bosses_killed%` | Bosses mortos |
| `%gorvax_blocks_broken%` | Blocos quebrados |
| `%gorvax_blocks_placed%` | Blocos colocados |
| `%gorvax_title%` | Título ativo |
| `%gorvax_money_earned%` | Dinheiro ganho |
| `%gorvax_money_spent%` | Dinheiro gasto |

#### Bosses

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_next_boss%` | Tempo para o próximo boss | `15m 30s` |

#### Conquistas e Títulos

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_achievements%` | Conquistas desbloqueadas / total | `5/20` |

#### Features Sociais (B17)

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_bounty%` | Valor da bounty na cabeça do jogador | `500.00` |
| `%gorvax_mail_unread%` | Cartas não lidas | `3` |

#### Nações (B19)

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_nation%` | Nome da nação (ou "Nenhuma") | `Império do Norte` |
| `%gorvax_nation_level%` | Nível da nação | `2` |
| `%gorvax_nation_kingdoms%` | Reinos na nação | `4` |
| `%gorvax_nation_bank%` | Saldo do banco da nação | `15000.00` |

#### Reputação / Karma (B18)

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_karma%` | Pontos de karma atuais | `350` |
| `%gorvax_karma_rank%` | Rank de karma (Herói, Bom, Neutro, Vilão, Procurado) | `Herói` |
| `%gorvax_karma_title%` | Título formatado com cor do rank | `§a✦ Herói` |

---

## 🔑 24. Permissões do Plugin

### Jogadores

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.player` | Permissões básicas de jogador |
| `gorvax.city.join` | Pode entrar em reinos |
| `gorvax.city.leave` | Pode sair de reinos |
| `gorvax.plot.buy` | Pode comprar lotes |
| `gorvax.plot.sell` | Pode vender lotes |
| `gorvax.reino.nome` | Pode renomear reino (apenas Rei) |

### Rei

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.mayor` | Permissões de Rei |
| `gorvax.city.create` | Pode criar reinos |
| `gorvax.city.rename` | Pode renomear reinos |
| `gorvax.city.delete` | Pode deletar reinos |
| `gorvax.city.claim` | Pode expandir território |
| `gorvax.city.invite` | Pode convidar membros |

### Administrador

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.admin` | Acesso total de administrador |

---

## 📁 25. Arquivos de Configuração

| Arquivo | Descrição |
|---------|-----------|
| `config.yml` | Configurações gerais (economia, limites, timers, etc.) |
| `messages.yml` | Todas as mensagens exibidas pelo plugin |
| `market_global.yml` | Itens e preços base do mercado global |
| `boss_settings.yml` | Configurações de spawn, HP, fases dos bosses |
| `boss_rewards.yml` | Tabelas de loot por ranking dos bosses |
| `achievements.yml` | Definições de conquistas e recompensas |
| `plugin.yml` | Registro de comandos e permissões |

### Arquivos de Dados (Runtime)

| Arquivo | Descrição |
|---------|-----------|
| `claims.yml` | Dados de todos os claims/reinos |
| `kingdoms.yml` | Dados de reinos (membros, diplomacia, banco) |
| `playerdata.yml` | Estatísticas e dados de jogadores |
| `local_market.yml` | Itens listados no mercado local |
| `boss_data.yml` | Estado dos bosses (spawn, cooldown) |
| `audit_log.yml` | Log de auditoria |
| `auction_data.yml` | Leilões ativos e pendentes |
| `price_history.yml` | Snapshots de preços do mercado |

---

## 📦 26. Dependências

### Obrigatórias

| Plugin | Versão | Para que serve |
|--------|--------|---------------|
| **Vault** | 1.7+ | Economia (saldo, transações) |
| **WorldGuard** | 7.0+ | Consulta e compatibilidade de regiões |
| **PlaceholderAPI** | 2.11+ | Placeholders no chat, scoreboard, TAB |

### Opcionais

| Plugin | Para que serve |
|--------|---------------|
| **LuckPerms** | Prefixos, grupos e permissões avançadas |
| **Floodgate** | Detecção e formulários nativos para jogadores Bedrock |

### Embarcadas (Incluídas no JAR)

| Biblioteca | Para que serve |
|-----------|---------------|
| **AnvilGUI** | Inputs de texto via GUI de bigorna (Java Edition) |
| **HikariCP** | Connection pooling para MySQL (B18) |

---

## ⚔️ 27. Guerra entre Reinos 👥


O sistema de guerra permite que reinos declarem guerra formal, disputem pontos por kills e recebam espólios ao final.

### Ciclo de Guerra

```
Declaração → Preparação (24h) → Guerra Ativa (7d máx) → Fim
                                       ↓
                                Kills = +1 ponto
                                Deaths = -1 ponto
                                       ↓
                          Tempo esgota OU rendição
                                       ↓
                   Espólios (25-35% do banco) + Debuff (3d)
                         + Captura de outpost (opcional)
```

### Requisitos para Declarar Guerra

| Requisito | Padrão |
|-----------|--------|
| Nível mínimo do reino | 3 |
| Membros mínimos | 3 |
| Custo (banco do reino) | $10.000 |
| Cooldown entre guerras | 30 dias |
| Não pode declarar a aliados | ✅ |

### Comandos de Guerra

| Comando | O que faz | Quem pode |
|---------|-----------|-----------|
| `/reino guerra declarar <reino>` | Declara guerra a outro reino | Rei |
| `/reino guerra renderse confirmar` | Rende-se na guerra atual (pede confirmação) | Rei |
| `/reino guerra status` | Mostra guerras ativas (próprias + admin vê todas) | Qualquer membro |

> ℹ️ **Aliases**: `/reino war declarar`, `/reino war status`, `/reino war surrender`

### Fases da Guerra

| Fase | Duração | O que acontece |
|------|---------|----------------|
| **Preparação** | 24h (configurável) | Aviso global, relação muda para WAR, PvP forçado |
| **Ativa** | 7 dias (configurável) | Kills contam pontos, broadcast periódico de placar |
| **Encerrada** | — | Espólios transferidos, debuff aplicado, relação volta a Neutro |

### Espólios de Guerra

- O **vencedor** recebe uma % do banco do perdedor (padrão: 25%)
- Em caso de **rendição**, a penalidade sobe (padrão: +10%, total 35%)
- O **perdedor** recebe debuff (buffs de reino desativados por 3 dias)
- O vencedor pode **capturar o menor outpost** do perdedor (configurável)

### Empate

- Se os pontos forem **iguais** ao fim do tempo, é empate
- Sem espólios, sem debuffs
- Relação volta a Neutro
- Cooldown de 30 dias é aplicado normalmente

### Configuração (`config.yml`)

```yaml
war:
  enabled: true
  declaration_cost: 10000.0
  preparation_hours: 24
  max_duration_days: 7
  min_kingdom_level: 3
  min_members: 3
  points_per_kill: 1
  points_per_death: -1
  spoils_bank_percent: 25.0
  surrender_penalty_percent: 10.0
  loser_debuff_days: 3
  cooldown_days: 30
  outpost_capture_enabled: true
```

---

## 🗺️ 28. Integração com Mapa Web (Dynmap / BlueMap)

O GorvaxCore se integra opcionalmente com **Dynmap** ou **BlueMap** para exibir reinos, outposts e claims no mapa web do servidor.

### Como funciona

- O plugin **auto-detecta** qual mapa web está instalado (Dynmap tem prioridade sobre BlueMap)
- **Reinos**, **outposts** e opcionalmente **terrenos pessoais** aparecem como áreas coloridas no mapa
- Popups exibem nome, dono, membros e nível do reino
- Os marcadores são atualizados **periodicamente** e **instantaneamente** quando claims são criados ou deletados

### Requisitos

- Instalar **Dynmap** ou **BlueMap** no servidor (nenhum é obrigatório — o recurso desliga sozinho se nenhum estiver presente)
- Não é necessário nenhum comando adicional — a integração é automática

### Configuração (`config.yml`)

```yaml
webmap:
  # Ativar/desativar a integração com mapa web
  enabled: true

  # Intervalo de atualização dos marcadores (em ticks, 1200 = 1 minuto)
  update_interval: 1200

  # Label do conjunto de marcadores no mapa
  marker_set_label: "GorvaxCore — Reinos"

  # Cores em hexadecimal para cada tipo de claim
  colors:
    kingdom: "#00AA00"    # Verde — reinos
    outpost: "#FF8800"    # Laranja — postos avançados
    personal: "#5555FF"   # Azul — terrenos pessoais

  # Exibir terrenos pessoais (não-reinos) no mapa
  show_personal_claims: false
```

### Cores dos Marcadores

| Tipo | Cor Padrão | Hex |
|------|------------|-----|
| Reino | 🟢 Verde | `#00AA00` |
| Outpost | 🟠 Laranja | `#FF8800` |
| Terreno Pessoal | 🔵 Azul | `#5555FF` |

### Popup / Tooltip

Ao clicar em um marcador no mapa web, o jogador vê:

- **Nome** do reino/terreno
- **Tipo** (Reino, Outpost, Terreno)
- **Dono** (nome do jogador)
- **Membros** (quantidade, apenas para reinos)
- **Nível** do reino

### Dynmap vs BlueMap

| Característica | Dynmap | BlueMap |
|----------------|--------|---------|
| Detecção | Automática | Automática |
| Tipo de marcador | AreaMarker (2D) | ShapeMarker (2D) |
| Popup HTML | ✅ Sim | ✅ Via label |
| Dependência no build | Nenhuma (reflexão) | `BlueMapAPI:2.7.2` |

### Mensagens do Console

| Mensagem | Significado |
|----------|-------------|
| `Dynmap detectado! Reinos visíveis no mapa web.` | Dynmap encontrado e conectado |
| `BlueMap detectado! Reinos serão visíveis no mapa web quando pronto.` | BlueMap encontrado (aguardando callback) |
| `Nenhum plugin de mapa web encontrado.` | Nenhum mapa instalado — integração desativada |
| `Integração com mapa web desativada no config.yml.` | Feature desligada by config |

### Dicas

- Para ver terrenos pessoais no mapa, ative `show_personal_claims: true` no `config.yml`
- Reduza `update_interval` para atualizações mais rápidas (mais uso de CPU)
- As cores suportam qualquer valor hexadecimal (ex: `"#FF0000"` para vermelho)
- A integração é **100% opcional** — sem Dynmap/BlueMap, o plugin funciona normalmente

---

## 📬 29. Correio (Mailbox) — `/carta` 👥

> **B17** — Sistema de cartas offline entre jogadores.

### Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/carta enviar <nick> <mensagem>` | Envia uma carta para qualquer jogador (online ou offline) | `gorvax.carta` |
| `/carta ler [página]` | Lê suas cartas recebidas (paginado) | `gorvax.carta` |
| `/carta deletar <número>` | Deleta uma carta específica | `gorvax.carta` |
| `/carta limpar` | Remove todas as cartas já lidas | `gorvax.carta` |

### Configuração (`config.yml`)

```yaml
mail:
  max_unread: 20           # Máximo de cartas não lidas
  max_message_length: 200  # Caracteres máximos por carta
```

### Comportamento

- Ao logar, o jogador recebe notificação se há cartas não lidas
- Cartas não lidas são marcadas com `§c[NÃO LIDA]`
- Persistência em `mail.yml`
- Placeholder PAPI: `%gorvax_mail_unread%`

---

## 🗳️ 30. Votação no Reino — `/reino votar` 👥

> **B17** — Sistema de votação interna para reinos.

### Comandos

| Comando | Descrição | Quem pode |
|---------|-----------|-----------|
| `/reino votar criar <pergunta>` | Cria uma votação no reino | Apenas o Rei |
| `/reino votar sim` | Vota sim na votação ativa | Membros do reino |
| `/reino votar nao` | Vota não na votação ativa | Membros do reino |
| `/reino votar resultado` | Mostra resultado parcial | Membros do reino |
| `/reino votar cancelar` | Cancela a votação ativa | Apenas o Rei |

### Configuração (`config.yml`)

```yaml
vote:
  expire_hours: 24  # Horas até a votação expirar automaticamente
```

### Comportamento

- Apenas uma votação ativa por reino
- Ao expirar, o resultado é enviado automaticamente aos membros online
- Cada membro só pode votar uma vez
- Persistência em `votes.yml`

---

## 💀 31. Bounties — `/bounty` 👥

> **B17** — Sistema de recompensas por PvP.

### Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/bounty colocar <nick> <valor>` | Coloca bounty na cabeça de um jogador | `gorvax.bounty` |
| `/bounty listar` | Lista todas as bounties ativas | `gorvax.bounty` |
| `/bounty remover <nick>` | Remove sua contribuição (sem reembolso) | `gorvax.bounty` |

### Configuração (`config.yml`)

```yaml
bounty:
  enabled: true
  min_value: 100.0
  max_value: 1000000.0
  same_kingdom_allowed: false
```

### Comportamento

- Bounties acumulam: múltiplos jogadores podem apostar no mesmo alvo
- Ao matar o alvo em PvP, o assassino recebe o valor total via Vault
- O alvo é notificado quando uma bounty é colocada nele
- Broadcast global quando bounty é cobrada
- Não é possível colocar bounty em si mesmo
- Se `same_kingdom_allowed` é false, membros do mesmo reino não podem colocar bounty entre si
- Persistência em `bounties.yml`
- Placeholder PAPI: `%gorvax_bounty%` (valor da bounty na cabeça do jogador)

---

## 📝 Histórico de Atualizações do Manual

| Data | Batch | Alterações no Manual |
|------|-------|---------------------|
| 2026-02-23 | Inicial | Criação do manual com auditoria completa de todas as funcionalidades (B1–B12) |
| 2026-02-23 | B13 | Adicionada seção 14 — Postos Avançados (Outposts) |
| 2026-02-23 | B14 | Adicionadas seções 15 (Leilão Global) e 16 (Histórico de Preços). Atualizados: índice, mensagens, arquivos de dados. |
| 2026-02-23 | B15 | Adicionada seção 27 (Guerra entre Reinos). Atualizada seção 9 (Diplomacia) com relação WAR. Atualizada seção de mensagens. |
| 2026-02-23 | B16 | Adicionada seção 28 (Integração com Mapa Web — Dynmap / BlueMap). Configuração, cores, popups, dicas. |
| 2026-02-23 | B17 | Adicionadas seções 29 (Correio), 30 (Votação no Reino) e 31 (Bounties). Novos placeholders PAPI. |
| 2026-02-23 | B18 | Adicionada seção 32 (Storage / Banco de Dados). Atualizada seção de dependências (HikariCP). |
| 2026-02-23 | B19 | Adicionada seção 33 (Sistema de Nações). Novos placeholders PAPI (nation, nation_level, nation_kingdoms, nation_bank). Chat `/nc`. |
| 2026-02-23 | B20 | Validação final. Renumeração de seções, adição de placeholders faltantes na seção centralizada, uniformização de emojis. |

---

## 🗄️ 32. Storage / Banco de Dados

> **B18** — Sistema de persistência abstrato com suporte a múltiplos backends.

### Backends Suportados

| Backend | Quando Usar | Arquivo/Conexão |
|---------|-------------|----------------|
| **YAML** (padrão) | Servidor simples, poucos jogadores | Arquivos `.yml` na pasta do plugin |
| **SQLite** | Servidor solo, performance melhor | `gorvax.db` na pasta do plugin |
| **MySQL** | Multi-servidor, rede de servidores | Servidor MySQL externo |

### Configuração (`config.yml`)

```yaml
storage:
  type: yaml      # yaml, sqlite ou mysql
  sqlite:
    file: gorvax.db
  mysql:
    host: localhost
    port: 3306
    database: gorvax
    username: root
    password: ''
    pool_size: 10
```

### Migração de Dados

Para migrar dados de um backend para outro:

```
/gorvax migrate <origem> <destino>
```

**Exemplos**:
- `/gorvax migrate yaml sqlite` — migra dados YAML para SQLite
- `/gorvax migrate yaml mysql` — migra dados YAML para MySQL
- `/gorvax migrate sqlite mysql` — migra SQLite para MySQL

> ⚠️ **Importante**: Após migrar, altere `storage.type` no `config.yml` e reinicie o servidor.

### Comportamento

- Se o backend configurado falhar ao iniciar, o plugin **reverte automaticamente** para YAML
- A migração é executada em **thread assíncrona** para não travar o servidor
- Backends SQLite/MySQL usam **transações** para garantir integridade
- MySQL usa **HikariCP** para connection pooling eficiente
- Permissão necessária: `gorvax.admin`

---

## 🏛️ 33. Sistema de Nações (B19) 👥

Nações são **meta-reinos** — agrupamentos de reinos sob um líder chamado **Imperador** (o Rei do reino fundador).

### Benefícios

| Benefício | Descrição |
|-----------|-----------|
| **Chat de Nação** | Canal `/nc` para comunicação entre todos os reinos da nação |
| **Banco da Nação** | Cofre compartilhado (apenas Imperador saca) |
| **Buffs por Nível** | Nível 2: Velocidade, Nível 3: Resistência a dano |
| **Desconto de Visita** | -50% de custo ao visitar reinos da mesma nação |

### Comandos

| Comando | Descrição |
|---------|-----------|
| `/nacao criar <nome>` | Funda uma nação (custo: $50.000) |
| `/nacao dissolver` | Dissolve a nação (apenas Imperador) |
| `/nacao convidar <reino>` | Convida um reino para a nação (apenas Imperador) |
| `/nacao aceitar` | Aceita um convite de nação (apenas Rei) |
| `/nacao recusar` | Recusa um convite de nação |
| `/nacao sair` | Seu reino sai da nação (apenas Rei) |
| `/nacao expulsar <reino>` | Expulsa um reino da nação (apenas Imperador) |
| `/nacao depositar <valor>` | Deposita no banco da nação |
| `/nacao sacar <valor>` | Saca do banco da nação (apenas Imperador) |
| `/nacao banco` | Mostra o saldo do banco |
| `/nacao info` | Mostra informações da nação |
| `/nacao lista` | Lista todas as nações existentes |
| `/nacao ajuda` | Menu de ajuda completo |
| `/nc <mensagem>` | Envia mensagem no canal de nação |

### Nível da Nação

O nível é calculado pela quantidade total de membros de todos os reinos:

| Nível | Requisito | Buff |
|-------|-----------|------|
| 1 | 0-9 membros | Nenhum |
| 2 | 10-14 membros | Velocidade I |
| 3 | 15+ membros | Velocidade I + Resistência I |

### Placeholders (PlaceholderAPI)

| Placeholder | Resultado |
|-------------|-----------|
| `%gorvax_nation%` | Nome da nação (ou "Nenhuma") |
| `%gorvax_nation_level%` | Nível da nação |
| `%gorvax_nation_kingdoms%` | Número de reinos na nação |
| `%gorvax_nation_bank%` | Saldo do banco da nação |

### Configuração (`config.yml`)

```yaml
nations:
  creation_cost: 50000.0        # Custo para criar
  invite_expire: 60             # Tempo do convite (segundos)
  max_kingdoms: 10              # Limite de reinos por nação
  visit_cost_reduction: 0.5     # Desconto de visita (50%)
  buffs:
    level_2: "SPEED"            # Buff nível 2
    level_3: "DAMAGE_RESISTANCE" # Buff nível 3
```

### Persistência

Dados salvos em `nations.yml` automaticamente.

---

> 📌 **Nota**: Este manual é atualizado a cada batch do roadmap. Consulte o `ROADMAP.md` para ver o que está planejado para as próximas versões.

---

## 🛡️ 34. Proteção Anti-Exploit (Semi-Anárquico)

O GorvaxCore adiciona proteções específicas contra exploits comuns em servidores semi-anárquicos. Todas as proteções são **configuráveis** e podem ser ativadas/desativadas individualmente.

### Proteções Disponíveis

| Proteção | Descrição | Config |
|----------|-----------|--------|
| 🐉 **Anti-Wither** | Bloqueia spawn de Wither dentro de qualquer claim | `protection.block_wither_spawn` |
| 💎 **Anti-Crystal** | Bloqueia colocação e dano de End Crystal em claims alheios | `protection.block_crystal_placement` / `protection.block_crystal_damage` |
| 💣 **Anti-TNT Cannon** | Rastreia origem da TNT; se foi spawnada fora do claim, protege blocos dentro | `protection.block_tnt_from_outside` |
| 🛏️ **Anti-Bed/Anchor Bomb** | Protege claims contra explosões de camas (Nether) e Respawn Anchors (Overworld) | `protection.block_bed_explosions` |
| ⚡ **Limite de Redstone** | Limita ticks de redstone por chunk para evitar lag machines | `protection.redstone_tick_limit` |
| 🍇 **Anti-Chorus Fruit** | Bloqueia teleporte via Chorus Fruit para dentro de claims sem permissão | `protection.block_chorus_teleport` |

### Configuração (`config.yml`)

```yaml
protection:
  block_wither_spawn: true           # Impedir spawn de Wither dentro de claims
  block_crystal_placement: true      # Impedir colocação de End Crystal em claims alheios
  block_crystal_damage: true         # Proteger blocos de claims contra explosão de Crystal
  block_tnt_from_outside: true       # Rastrear TNT e bloquear se origina fora do claim
  block_bed_explosions: true         # Proteger claims contra explosões de cama/âncora
  redstone_tick_limit: 100           # Máximo de ticks de redstone por chunk por ciclo (0 = desativado)
  redstone_reset_interval: 20        # Ticks para resetar contadores (20 = 1 segundo)
  block_chorus_teleport: true        # Impedir Chorus Fruit para dentro de claims alheios
```

### Comportamento Detalhado

**Anti-TNT Cannon**: O sistema rastreia a localização onde cada TNT é spawnada. Quando ela explode, verifica se a origem e o destino estão em claims diferentes — se sim, os blocos protegidos são removidos da lista de destruição.

**Limite de Redstone**: Contadores atômicos por chunk são incrementados a cada tick de redstone. Quando o limite é atingido, o sinal é zerado e o dono do claim recebe um aviso. Os contadores são resetados periodicamente (padrão: a cada 1 segundo).

**Anti-Chorus Fruit**: Verifica se o jogador tem permissão de ACESSO no claim de destino. Se não tiver, o teleporte é cancelado.

### Permissões

Nenhuma permissão extra é necessária — as proteções são automáticas para todos os claims. Administradores podem desativar proteções individuais via `config.yml`.

---

## ⚔️ 35. Sistema de Combate (B2) 👥

O GorvaxCore adiciona um sistema de combate completo para a experiência semi-anárquica: Combat Tag, PvP Logger, Kill Streaks e Proteção de Spawn.

### Combat Tag (B2.1)

Ao atacar ou ser atacado por outro jogador, ambos entram em **modo de combate** por **15s** (configurável).

| Restrição | Descrição |
|-----------|----------|
| 🚫 **Comandos bloqueados** | `/home`, `/tpa`, `/spawn`, `/warp`, `/rtp`, `/back`, `/logout` |
| 🚫 **GUIs bloqueadas** | Menus, leilão, mercado (inventário próprio permitido) |
| 📊 **ActionBar** | `§c⚔ Em Combate (12s)` (contador regressivo) |

O timer reseta a cada hit dado/recebido. Permissão `gorvax.combat.bypass` ignora restrições.

### PvP Logger NPC (B2.2)

Se um jogador **deslogar durante combat tag**, um **NPC Villager** é spawnado no local:

- NPC tem o equipamento e HP do jogador
- Fica parado por **15s** (configurável)
- Pode ser morto → **dropa o inventário** inteiro do jogador
- Se não for morto, desaparece e inventário é mantido
- Ao logar, jogador morto via NPC recebe mensagem e é morto
- Broadcast global quando alguém desconecta em combate

### Kill Streaks (B2.3)

Contagem de kills PvP sem morrer:

| Streak | Efeito |
|--------|--------|
| A cada **5 kills** | 📢 Broadcast no chat |
| **5 kills** | Título `§c⚔ Imparável` |
| **10 kills** | Título `§6⚔ Lendário` + Bounty automática de $500 |
| **20 kills** | Título `§4⚔ Deus da Guerra` |

Streak reseta ao morrer. A maior streak histórica é salva no PlayerData.

### Proteção de Spawn (B2.4)

Ao respawnar após morrer, o jogador tem **5s de invulnerabilidade** (configurável):

- Dano recebido é cancelado durante o período
- **Cancela** se o jogador atacar alguém
- ActionBar: `§a✦ Proteção (4s)`

### Configuração (`config.yml`)

```yaml
combat:
  tag_duration: 15                     # Segundos de combat tag
  blocked_commands: [home, tpa, spawn, warp, rtp, back, logout]
  block_gui_in_combat: true            # Bloquear GUIs durante combate
  logger_enabled: true                 # NPC ao deslogar em combate
  logger_duration: 15                  # Duração do NPC (segundos)
  spawn_protection: 5                  # Invulnerabilidade pós-respawn
  killstreak:
    broadcast_interval: 5              # Broadcast a cada N kills
    auto_bounty_at: 10                 # Streak para bounty automática
    auto_bounty_value: 500.0           # Valor da bounty
    titles:                            # Títulos por streak
      5: "§c⚔ Imparável"
      10: "§6⚔ Lendário"
      20: "§4⚔ Deus da Guerra"
```

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.combat.bypass` | Ignora restrições de combat tag (permite comandos e GUIs) |

---

## ⚔️ 36. Sistema de Duelos 👥

Sistema de PvP 1v1 organizado com aposta em dinheiro.

### Comandos

| Comando | Descrição |
|---------|-----------|
| `/duel <nick>` | Desafia um jogador para duelo (sem aposta) |
| `/duel <nick> <valor>` | Desafia um jogador com aposta em $ |
| `/duel aceitar` | Aceita o desafio pendente |
| `/duel recusar` | Recusa o desafio |
| `/duel ajuda` | Exibe menu de ajuda |

**Aliases:** `/duelo`, `/pvp1v1`

### Regras do Duelo

1. **Desafio**: O jogador envia o desafio. O alvo recebe notificação no chat com botões clicáveis `[ACEITAR]` `[RECUSAR]`. Jogadores Bedrock recebem um **ModalForm** nativo.
2. **Expiração**: O convite expira em **30 segundos** se não for respondido.
3. **Distância**: Os jogadores devem estar a ≤50 blocos de distância.
4. **Combat Tag**: Nenhum dos jogadores pode estar em combat tag.
5. **Countdown**: Ao aceitar, contagem regressiva de **5 segundos** (Título: 3... 2... 1... ⚔ LUTE!)
6. **PvP Forçado**: Durante o duelo, PvP é forçado entre os dois participantes. Jogadores externos **não podem** atacar ou ser atacados pelos duelistas.
7. **Duração Máxima**: **5 minutos**. Se ninguém morrer, o duelo termina em **empate** e as apostas são devolvidas.
8. **Inventário**: O perdedor **mantém** seu inventário (sem drops).
9. **Desconexão**: Se um jogador desconectar, o oponente **vence automaticamente**.
10. **Comandos Bloqueados**: Os mesmos comandos bloqueados pelo combat tag (home, tpa, spawn, etc.) são bloqueados durante o duelo.

### Apostas

- Aposta mínima: **$100.00** (configurável)
- Aposta máxima: **$100,000.00** (configurável)
- O vencedor recebe aposta × 2, menos **5% de taxa**
- Ambos os jogadores devem ter saldo suficiente para a aposta

### Configuração (`config.yml`)

```yaml
duel:
  enabled: true
  challenge_expire: 30        # Segundos para aceitar
  countdown: 5                # Contagem regressiva
  max_duration: 300            # Duração máxima (segundos)
  max_distance: 50             # Distância máxima para desafiar
  min_bet: 100.0               # Aposta mínima
  max_bet: 100000.0            # Aposta máxima
  tax_percent: 5.0             # Taxa sobre o prêmio (%)
  keep_inventory: true         # Manter inventário do perdedor
```

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.duel` | Permite usar o comando `/duel` |

---

## 🎓 37. Tutorial Interativo + Welcome Kit

O sistema de tutorial guia jogadores novos pelos primeiros passos no servidor, garantindo que entendam as mecânicas principais.

### Como Funciona

Ao logar pela **primeira vez** (quando `firstJoin == 0`), o jogador recebe:
1. **Welcome Kit** — itens configuráveis entregues automaticamente
2. **Tutorial em 6 passos** — mensagens no chat + BossBar progressiva

### Passos do Tutorial

| Passo | Descrição | Avanço |
|-------|-----------|--------|
| 1 | Boas-vindas ao Gorvax | Automático (8s) |
| 2 | Receba seu kit inicial | Automático (8s) |
| 3 | Use `/rtp` para sair do spawn | Jogador executa `/rtp` |
| 4 | Segure a Pá de Ouro e selecione | Jogador seleciona |
| 5 | Confirme com `/confirmar` | Jogador confirma |
| 6 | Use `/gorvax menu` | Automático (10s) |

### Recompensas de Conclusão

- **$500** de bônus
- **200 blocos extras** de claim

### Comandos

| Comando | O que faz | Permissão |
|---------|-----------|-----------|
| `/tutorial` | Mostra o passo atual | `gorvax.tutorial` |
| `/tutorial pular` | Pula o tutorial | `gorvax.tutorial` |

### Welcome Kit (padrão)

Configurável em `config.yml` → `tutorial.welcome_kit`:

| Item | Quantidade |
|------|------------|
| Pá de Ouro | 1 |
| Pá de Ferro | 1 |
| Graveto | 1 |
| Escudo | 1 |
| Filé Mignon (Cooked Beef) | 16 |
| Elmo de Ferro | 1 |
| Peitoral de Ferro | 1 |
| Calça de Ferro | 1 |
| Botas de Ferro | 1 |

### Configuração (`config.yml`)

```yaml
tutorial:
  enabled: true
  rewards:
    money: 500.0
    claim_blocks: 200
  welcome_kit:
    - "GOLDEN_SHOVEL:1"
    - "IRON_SHOVEL:1"
    - "STICK:1"
    - "SHIELD:1"
    - "COOKED_BEEF:16"
    - "IRON_HELMET:1"
    - "IRON_CHESTPLATE:1"
    - "IRON_LEGGINGS:1"
    - "IRON_BOOTS:1"
```

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.tutorial` | Permite usar o comando `/tutorial` |

---

## 🎁 38. Recompensas Diárias (Daily Rewards & Login Streak)

O sistema de recompensas diárias incentiva o login consistente dos jogadores, oferecendo prêmios progressivos em um ciclo de 7 dias.

### Como Funciona

1. Ao **logar no servidor**, o jogador recebe uma notificação se há recompensa disponível
2. O jogador usa **`/daily`** para abrir a GUI de recompensas
3. Clica no dia atual para **resgatar** a recompensa
4. A sequência avança de **Dia 1 a Dia 7**, depois reinicia
5. Se o jogador **não logar em 48h**, a sequência é resetada ao Dia 1

### Mecânica de Streak

| Situação | Resultado |
|----------|-----------|
| Login dentro de 20-48h do último resgate | Sequência mantida, próximo dia disponível |
| Login após 48h sem resgatar | Sequência resetada para Dia 1 |
| Resgate tentado antes de 20h | Mensagem "já resgatou hoje" |

### Recompensas Padrão

| Dia | Recompensa |
|-----|------------|
| 1 | §6$100 |
| 2 | §a50 blocos de claim |
| 3 | §6$250 |
| 4 | §a100 blocos de claim |
| 5 | §6$500 |
| 6 | §a200 blocos de claim |
| 7 | §6$1.000 + §dTítulo "⭐ Fiel" |

### Comandos

| Comando | Aliases | O que faz | Permissão |
|---------|---------|-----------|-----------|
| `/daily` | `/diario`, `/dailyreward` | Abre a GUI de recompensas diárias | `gorvax.daily` |

### GUI de Recompensas (Java)

A GUI de 27 slots (3 linhas) mostra o progresso visual:

| Ícone | Significado |
|-------|-------------|
| 🟩 Vidro Verde | Dia já resgatado |
| 📦 Baú (brilhando) | Dia atual — clique para resgatar |
| ⬜ Vidro Cinza | Dia futuro — ainda não disponível |
| ⬛ Vidro Preto | Borda decorativa |

### Jogadores Bedrock

Jogadores Bedrock (via Floodgate) recebem um **SimpleForm nativo** com:
- Lista dos 7 dias e seus estados (✅ resgatado / 🎁 disponível / ⏳ futuro)
- Botão para resgatar o dia atual

### Configuração (`config.yml`)

```yaml
daily_rewards:
  enabled: true
  min_hours: 20              # Mínimo de horas entre resgates
  max_hours: 48              # Horas para resetar streak
  day_1:
    money: 100
  day_2:
    claim_blocks: 50
  day_3:
    money: 250
  day_4:
    claim_blocks: 100
  day_5:
    money: 500
  day_6:
    claim_blocks: 200
  day_7:
    money: 1000
    title: "§e⭐ Fiel"
```

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.daily` | Permite usar o comando `/daily` |

---

## 📋 39. Menu Central GUI (B6/B38)

O Menu Central é o ponto de acesso unificado para todos os sistemas do GorvaxCore. Organizado em **2 páginas** com 25 botões no total.

### Como Acessar

| Comando | Aliases | O que faz |
|---------|---------|-----------|
| `/menu` | `/gorvaxmenu`, `/gorvax menu` | Abre o Menu Central |

### Página 1 — Sistemas Principais

| Ícone | Ação |
|-------|------|
| 🏰 **Meu Reino** | Abre o menu principal do reino |
| 💰 **Mercado Global** | Abre o mercado (`/market`) |
| 🏪 **Leilão** | Abre leilões ativos (`/leilao`) |
| 👹 **World Bosses** | Mostra informações do próximo boss |
| 🏆 **Conquistas** | Abre o menu de conquistas |
| 📊 **Estatísticas** | Mostra kills, mortes, KDR, playtime |
| 🎁 **Recompensa Diária** | Abre a GUI de Daily Reward |
| ✉ **Correio** | Abre a mailbox (`/carta`) |
| ⚙ **Configurações** | Toggle HUD/scoreboard |
| 📋 **Rankings** | Abre `/top` — rankings do servidor |
| ▶ **Próxima Página** | Navega para a Página 2 |

### Página 2 — Sistemas Adicionais (B38)

| Ícone | Ação |
|-------|------|
| ⚔ **Duelos** | Ajuda de duelos (`/duel ajuda`) |
| 💀 **Bounties** | Lista bounties ativas (`/bounty listar`) |
| ✨ **Cosméticos** | Menu de cosméticos (`/cosmetics`) |
| 📚 **Códex** | Enciclopédia interativa (`/codex`) |
| 🎰 **Crates** | Seleção de crates para abrir |
| ⭐ **Battle Pass** | Menu do Battle Pass (`/pass`) |
| 🏅 **Ranks & Kits** | Progressão de ranks (`/rank`) |
| ⚖ **Karma** | Status de reputação (`/karma`) |
| 💎 **VIP** | Informações VIP (`/vip info`) |
| 📜 **Quests** | Menu de missões (`/quests`) |
| 🏛 **Nações** | Ajuda de nações (`/nacao ajuda`) |
| 🏷 **Títulos** | Menu de títulos desbloqueados |
| 🧭 **Hub de Teleportes** | Abre o hub de teleportes |
| 🎄 **Eventos Sazonais** | Info do evento ativo (`/evento info`) |
| 💬 **Chat & Social** | Guia de canais de chat |
| ◀ **Página Anterior** | Volta para a Página 1 |

### Bedrock

Jogadores Bedrock recebem um **SimpleForm nativo** (via Floodgate) com todos os 25 botões em lista sequencial (sem paginação, scroll nativo).

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.menu` | Permite usar o comando `/menu` |

---

## 🏆 40. Leaderboards & Rankings (B7)

O sistema de rankings exibe os **melhores jogadores** do servidor em diversas categorias, incentivando competição e engajamento.

### Categorias Disponíveis

| Categoria | Descrição | Dados |
|-----------|-----------|-------|
| `kills` | Kills PvP | `PlayerData.totalKills` |
| `mortes` | Mortes totais | `PlayerData.totalDeaths` |
| `kdr` | Kill/Death Ratio | kills ÷ deaths |
| `riqueza` | Saldo via Vault | `Economy.getBalance()` |
| `playtime` | Tempo jogado | `PlayerData.totalPlayTime` |
| `bosses` | Bosses abatidos | `PlayerData.bossesKilled` |
| `reinos` | Poder do reino | membros × chunks |
| `streak` | Maior kill streak | `PlayerData.highestKillStreak` |

### Comandos

| Comando | Aliases | O que faz | Permissão |
|---------|---------|-----------|-----------|
| `/top` | `/ranking`, `/rankings` | Lista categorias disponíveis | `gorvax.top` |
| `/top <categoria>` | — | Exibe o Top 10 da categoria | `gorvax.top` |

### Exemplo de Saída

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🏆 RANKING — ⚔ Kills
  
  #1 Player1 — 150
  #2 Player2 — 120
  #3 Player3 — 98
  ...
  
  Sua posição: #5
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Placeholders (PlaceholderAPI)

Formato: `%gorvax_top_<categoria>_<posição>_<tipo>%`

| Placeholder | Resultado |
|-------------|-----------|
| `%gorvax_top_kills_1_name%` | Nome do #1 em kills |
| `%gorvax_top_kills_1_value%` | Valor do #1 em kills |
| `%gorvax_top_riqueza_3_name%` | Nome do #3 em riqueza |
| `%gorvax_top_kdr_5_value%` | KDR do #5 |

> Funciona para todas as 8 categorias, posições de 1 a 10.

### Cache de Rankings

- Rankings são **atualizados a cada 5 minutos** (configurável) por performance
- Não são recalculados em tempo real — evita lag em servidores grandes
- Primeiro rebuild: **10 segundos** após o servidor ligar
- Cache em `ConcurrentHashMap` thread-safe

### Configuração (`config.yml`)

```yaml
leaderboard:
  refresh_interval_minutes: 5    # Intervalo de atualização do cache
  top_size: 10                    # Posições no ranking
```

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.top` | Permite usar o comando `/top` |

---

## 🔗 41. Integração Discord (Webhook)

Ponte unidirecional **Minecraft → Discord** via webhooks HTTP. Sem necessidade de bot — basta configurar a URL do webhook no canal desejado.

### Funcionalidades

| Funcionalidade | Descrição |
|----------------|-----------|
| **Chat Sync** | Mensagens do chat GLOBAL são enviadas ao Discord. Formato: `**[TagReino] Jogador**: mensagem` |
| **Boss Spawn** | Alerta com embed quando um World Boss spawna |
| **Guerra** | Alerta quando um reino declara guerra contra outro |
| **Kill Streak** | Alerta quando um jogador atinge threshold de kills consecutivos |
| **Conquistas** | Alerta quando um jogador desbloqueia uma conquista |
| **Boss Raid** | Alerta quando uma raid de boss é iniciada |

### Configuração (`config.yml`)

```yaml
discord:
  enabled: false           # Ativar/desativar integração Discord
  webhook_url: ""          # URL do webhook do Discord
  chat_sync: true          # Enviar chat global ao Discord
  alerts:
    boss_spawn: true       # Alerta de boss spawn
    war_declare: true      # Alerta de guerra declarada
    raid_start: true       # Alerta de raid iniciada
    killstreak: true       # Alerta de killstreak
    achievement_rare: true # Alerta de conquista
  killstreak_threshold: 5  # Mínimo de kills para alertar
```

### Como configurar

1. No Discord, vá em **Definições do Canal → Integrações → Webhooks**
2. Clique **Novo Webhook** e copie a URL
3. Cole a URL em `discord.webhook_url` no `config.yml`
4. Defina `discord.enabled: true`
5. Reinicie o servidor ou use `/gorvax reload`

### Exemplo de alertas no Discord

```
🐉 **Rei Gorvax** surgiu no world! 5000 HP
⚔️ **Dragões** declarou guerra contra **Fênix**!
🔥 **Jogador1** está com 10 kills seguidas!
🏆 **Jogador2** desbloqueou a conquista **Caçador de Dragões**!
```

---

## ⚔️ 42. Custom Items — Armas e Armaduras Lendárias (B10)

Itens únicos com efeitos especiais, configuráveis via YAML. Identificados por `PersistentDataContainer` com chave `gorvax:custom_item_id`.

### Características dos Itens

| Característica | Descrição |
|----------------|-----------|
| **Nome colorido** | Nomes com formatação `§` (cores e estilos) |
| **Lore descritiva** | Descrição, stats e origem do item |
| **Enchants** | Encantamentos (podendo exceder nível vanilla) |
| **Atributos** | Modificadores (dano, HP, velocidade de movimento, etc.) |
| **Efeito On-Hit** | Ao atacar, aplica poção no alvo com chance configurável |
| **Efeito Passivo** | Ao vestir armadura, aplica poção permanente ao portador |

### Efeitos On-Hit

Ao atacar com arma customizada, cada efeito tem uma **chance configurável** de ser aplicado. Exemplo: Wither com 30% de chance por hit.

### Efeitos Passivos (On-Equip)

Ao vestir armadura customizada, efeitos passivos são aplicados automaticamente via task periódica. Ao remover a armadura, os efeitos são removidos.

### Itens Padrão (9 itens lendários)

| Item | Base | Efeito Principal | Fonte |
|------|------|-------------------|-------|
| ⚔ **Lâmina de Gorvax** | Netherite Sword | Wither on-hit (30%) | Rei Gorvax Top 1 |
| 👑 **Coroa de Indrax** | Netherite Helmet | Respiração Aquática passiva, +4 HP | Indrax Top 1-3 |
| 💨 **Botas Velocistas** | Diamond Boots | Velocidade passiva | Conquista: 50 bosses |
| 🏹 **Arco do Caçador** | Bow | Poison + Slowness on-hit | Zarith Top 1-3 |
| 🛡️ **Escudo do Guardião** | Shield | KB Resist 40%, +2 HP | Conquista: Muro de Ferro |
| 🔥 **Manto de Vulgathor** | Netherite Chestplate | Fire Resistance passiva, Dano on-hit (35%) | Vulgathor Top 1-3 |
| 🧊 **Lança de Kaldur** | Trident | Slowness on-hit (40%), Mining Fatigue (20%) | Kaldur Top 1-3 |
| 💀 **Elmo de Skulkor** | Netherite Helmet | +6 HP, Strength on-hit (15%) | Skulkor Top 1-3 |
| 🌀 **Pérola de Xylos** | Netherite Axe | Blindness on-hit (25%), Levitation (10%) | Xylos Top 1-3 |

### Comandos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/customitem give <jogador> <id> [qtd]` | `/ci give` | Dá um item customizado a um jogador | `gorvax.admin` |
| `/customitem list` | `/ci list` | Lista todos os IDs de itens disponíveis | `gorvax.admin` |

### Configuração (`config.yml`)

```yaml
custom_items:
  enabled: true
  passive_effect_interval: 60  # Ticks para reaplicar efeitos passivos (60 = 3s)
```

### Configuração dos Itens (`custom_items.yml`)

Cada item é definido sob a chave `items:` com as seguintes propriedades:

```yaml
items:
  lamina_gorvax:
    base: NETHERITE_SWORD
    name: "§4§l⚔ Lâmina de Gorvax"
    lore:
      - "§7Forjada no fogo ancestral do Rei Gorvax."
      - "§c+15% dano a jogadores"
    enchants: { SHARPNESS: 7, FIRE_ASPECT: 3 }
    attributes: { GENERIC_ATTACK_DAMAGE: 12.0 }
    on_hit_effects:
      - { type: WITHER, duration: 60, amplifier: 1, chance: 30 }
    source: "Drop do Rei Gorvax (Top 1)"
```

### Reload

Custom items são recarregados via `/gorvax reload` junto com os demais sistemas.

---

## 🎰 43. Sistema de Crates / Keys (B12)

O GorvaxCore possui um sistema de caixas de recompensa aleatória com chaves, animação de roleta e integração completa com Daily Rewards e economia.

### Tipos de Crate

| Tipo | Cor | Conteúdo Típico |
|------|-----|-----------------|
| ⬜ **Comum** | Cinza | Dinheiro, blocos de claim, itens básicos |
| � **Raro** | Azul | Itens valiosos, blocos extras, dinheiro maior |
| 🟡 **Lendário** | Dourado | Custom items de boss, títulos exclusivos, muitos blocos |
| 🟣 **Sazonal** | Roxo | Itens temáticos exclusivos por temporada |

### Como Obter Chaves

| Fonte | Tipo de Chave |
|-------|---------------|
| Daily Rewards (Dia 7) | Comum |
| Conquistas difíceis | Raro / Lendário |
| Kill streaks (15+) | Raro |
| Boss kill (Top 1-3) | Lendário |
| Loja VIP (B14) | Qualquer tipo |
| Admin (`/crate give`) | Qualquer tipo |

### Como Abrir Crates

1. **Via bloco**: Clique em um **Ender Chest** na área do spawn
2. **Via comando**: `/crate abrir <tipo>` de qualquer lugar
3. **Via GUI**: Selecione a crate no menu e clique para abrir

### Animação de Roleta

Ao abrir uma crate, uma animação de roleta é exibida:
- Itens giram no inventário com som (`BLOCK_NOTE_BLOCK_PLING`)
- A velocidade diminui gradualmente até parar
- O item vencedor é revelado com som épico (`UI_TOAST_CHALLENGE_COMPLETE`)
- Se for item raro/lendário, é feito broadcast no chat

### Comandos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/crate abrir <tipo>` | `/crate open` | Abre uma crate do tipo especificado | `gorvax.player` |
| `/crate preview <tipo>` | — | Mostra as recompensas possíveis e chances | `gorvax.player` |
| `/crate chaves` | `/crate keys` | Lista suas chaves por tipo | `gorvax.player` |
| `/crate lista` | `/crate list` | Lista todos os tipos de crate disponíveis | `gorvax.player` |
| `/crate give <nick> <tipo> [qtd]` | — | Dá chaves a um jogador | `gorvax.admin` |
| `/crate reload` | — | Recarrega configurações de crates | `gorvax.admin` |
| `/crate ajuda` | `/crate help` | Mostra ajuda de comandos | `gorvax.player` |

### Configuração (`config.yml`)

```yaml
crates:
  enabled: true
  block_material: ENDER_CHEST    # Bloco que abre o menu
  spawn_only: true                # Restringir ao spawn
  spawn_radius: 50                # Raio da área de spawn
```

### Configuração dos Tipos (`crates.yml`)

Cada tipo de crate é definido com rewards ponderados:

```yaml
types:
  comum:
    display_name: "§7§lCrate Comum"
    icon: CHEST
    broadcast_rewards: false
    rewards:
      - type: money
        amount: 200
        weight: 40
      - type: claim_blocks
        amount: 50
        weight: 30
      - type: item
        material: DIAMOND
        amount: 3
        weight: 20
```

**Tipos de reward suportados**: `money`, `claim_blocks`, `item`, `title`, `crate_key`

### Preview

O comando `/crate preview <tipo>` abre uma GUI mostrando todos os rewards possíveis com a chance percentual de cada um (calculada pelo peso relativo).

### Compatibilidade Bedrock

- **GUI de seleção**: SimpleForm com botões por tipo de crate
- **Preview**: SimpleForm com lista de rewards
- **Abertura**: funciona normalmente via inventário
- **Fallback**: AnvilGUI para digitação de tipo se necessário

### Persistência

As chaves são salvas no `playerdata.yml` sob a chave `crate_keys`:

```yaml
<uuid>:
  crate_keys:
    comum: 3
    raro: 1
```

### Reload

Crates são recarregadas via `/gorvax reload` junto com os demais sistemas.

---

## 44. ✨ Sistema de Cosméticos (B13)

O sistema de cosméticos permite que jogadores customizem sua aparência visual sem afetar gameplay. São efeitos puramente estéticos.

### Tipos de Cosméticos

| Tipo | Descrição |
|------|-----------|
| **WALK_PARTICLE** | Partículas geradas ao caminhar |
| **ARROW_TRAIL** | Trilha de partículas em flechas disparadas |
| **CHAT_TAG** | Prefixo colorido exibido antes do nome no chat |
| **KILL_EFFECT** | Efeito visual ao eliminar um jogador (relâmpago, fogos, explosão) |
| **KILL_PARTICLE** | Partículas na localização da morte do alvo |

### Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/cosmetics` | Abre o menu de cosméticos (GUI) | `gorvax.player` |
| `/cosmetics listar` | Lista cosméticos desbloqueados | `gorvax.player` |
| `/cosmetics ativar <id>` | Ativa um cosmético por ID | `gorvax.player` |
| `/cosmetics desativar <tipo>` | Desativa o cosmético do tipo | `gorvax.player` |
| `/cosmetics give <nick> <id>` | Desbloqueia cosmético para um jogador | `gorvax.admin` |

**Aliases**: `/cosmeticos`, `/cosmetic`

### GUI de Cosméticos

O menu principal exibe 4 categorias com ícones distintos:

- 🔥 **Partículas de Caminhada** — Blaze Powder
- ✨ **Trails de Flecha** — Spectral Arrow
- 🏷 **Tags de Chat** — Name Tag
- 💥 **Efeitos de Kill** — TNT (inclui kill effects e kill particles)

Ao clicar em uma categoria, abre o menu de itens daquele tipo:
- **Verde (Lime Dye)** → cosmético ativo (clique para desativar)
- **Azul (Light Blue Dye)** → desbloqueado (clique para ativar)
- **Cinza (Gray Dye)** → bloqueado (mostra como desbloquear)

### Configuração (`cosmetics.yml`)

Cada cosmético é definido com as seguintes propriedades:

```yaml
particula_fogo:
  type: WALK_PARTICLE
  name: "§6🔥 Rastro de Fogo"
  description: "Deixa um rastro de chamas ao caminhar."
  source: achievement         # achievement, shop, crate, vip, admin
  price: 0.0
  particle: FLAME
  count: 3
  speed: 0.02
  offset_y: 0.1
```

Para **CHAT_TAG**, use o campo `display`:

```yaml
tag_vip:
  type: CHAT_TAG
  name: "§a✦ VIP Tag"
  display: "§a[VIP] "
  source: vip
```

Para **KILL_EFFECT**, use o campo `effect`:

```yaml
efeito_relampago:
  type: KILL_EFFECT
  name: "§e⚡ Relâmpago"
  effect: LIGHTNING            # LIGHTNING, FIREWORK, EXPLOSION
  source: achievement
```

### Fontes de Desbloqueio

| Fonte | Descrição |
|-------|-----------|
| `achievement` | Desbloqueado via sistema de conquistas |
| `shop` | Comprável na loja (campo `price`) |
| `crate` | Obtido em crates |
| `vip` | Exclusivo para VIPs |
| `admin` | Dado manualmente via `/cosmetics give` |

### Persistência

Dados de cosméticos são salvos no `playerdata.yml`:

```yaml
<uuid>:
  unlocked_cosmetics:
    - particula_fogo
    - tag_guerreiro
  active_cosmetics:
    WALK_PARTICLE: particula_fogo
    CHAT_TAG: tag_guerreiro
```

### Chat Tags

Tags cosméticas aparecem no chat **antes** do nome do jogador, entre o ícone de rank e o nome. Também aparecem no display name (tab list) e são integradas no `formatMessage` do ChatManager.

### Reload

Cosméticos são recarregados via `/gorvax reload` junto com os demais sistemas.

---

## 45. 🐉 Sistema VIP & Ranks Premium (B14)

O sistema de VIP oferece benefícios premium para jogadores que apoiam o servidor, sem vantagem PvP direta (conformidade EULA Minecraft).

### Tiers VIP

| Tier | Prefixo | Blocos Extras | Homes Extras | Keys Mensais | Desconto Mercado |
|------|---------|:------------:|:------------:|:------------:|:---------------:|
| ✦ VIP | §a[✦ VIP] | +500 | +2 | 1 Raro | 0% |
| ✦ VIP+ | §b[✦ VIP+] | +1.500 | +5 | 2 Raro + 1 Lendário | 5% |
| ⚡ ELITE | §6[⚡ ELITE] | +3.000 | +10 | 3 Raro + 1 Lendário | 10% |
| 🐉 LENDÁRIO | §d[🐉 LENDÁRIO] | +5.000 | +15 | 3 Raro + 2 Lendário | 15% |

### Comandos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/vip` | `/ranks`, `/premium` | Mostra tabela de benefícios VIP | `gorvax.player` |
| `/vip info` | — | Tabela detalhada de todos os tiers | `gorvax.player` |
| `/vip status` | — | Exibe seu rank VIP atual e benefícios | `gorvax.player` |
| `/vip set <nick> <tier>` | — | Define o rank VIP de um jogador | `gorvax.admin` |
| `/vip remove <nick>` | — | Remove o rank VIP de um jogador | `gorvax.admin` |
| `/vip keys` | — | Distribui keys mensais para todos os VIPs online | `gorvax.admin` |
| `/vip reload` | — | Recarrega configurações VIP | `gorvax.admin` |

### Como Funciona

1. **Aplicação de Rank**: Via loja web (Tebex) ou manualmente com `/vip set`
2. **Grupos LuckPerms**: Cada tier corresponde a um grupo LP (`vip`, `vip-plus`, `elite`, `lendario`)
3. **Blocos extras**: Aplicados ao claim pool do jogador ao logar pela primeira vez com o rank
4. **Keys mensais**: Distribuídas automaticamente no dia 1 de cada mês, ou manualmente com `/vip keys`
5. **Desconto no mercado**: Aplicado automaticamente nas transações do mercado global

### Placeholders (PlaceholderAPI)

| Placeholder | Resultado |
|-------------|-----------|
| `%gorvax_vip_tier%` | Nome do tier (ex: "VIP+", "ELITE") |
| `%gorvax_vip_display%` | Nome formatado com cores (ex: "§b[✦ VIP+]") |
| `%gorvax_vip_blocks%` | Blocos extras do tier atual |

### Configuração (`config.yml`)

```yaml
vip:
  enabled: true
  groups:
    vip: "vip"
    vip_plus: "vip-plus"
    elite: "elite"
    lendario: "lendario"
  tiers:
    vip:
      display_name: "§a[✦ VIP]"
      extra_claim_blocks: 500
      extra_homes: 2
      monthly_keys:
        raro: 1
      market_discount_percent: 0
    # ... (demais tiers com valores crescentes)
  monthly_key_day: 1
```

### Integração com Loja Web

Comandos executados via console ao comprar no Tebex/CraftingStore:

```
lp user {username} parent set vip        # Ao comprar VIP
lp user {username} parent set vip-plus   # Ao comprar VIP+
lp user {username} parent set elite      # Ao comprar ELITE
lp user {username} parent set lendario   # Ao comprar LENDÁRIO
lp user {username} parent set default    # Ao expirar
```

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.vip` | Permissão base de VIP (todos os tiers) |
| `gorvax.vip.plus` | Permissão VIP+ |
| `gorvax.vip.elite` | Permissão ELITE |
| `gorvax.vip.lendario` | Permissão LENDÁRIO |
| `gorvax.admin` | Permite subcomandos admin do /vip |

### Documento de Precificação

Consulte `VIP_PRICING.md` na raiz do projeto para a tabela de preços sugeridos, comparação de mercado, promoções de lançamento e integração Tebex.

---

## ⭐ 46. Battle Pass Sazonal (B15)

Sistema de progressão sazonal com 30 níveis, track Free + Premium, GUI interativa e recompensas configuráveis.

### Como Funciona

1. Jogadores ganham **XP** ao realizar ações no servidor (kills, mineração, login)
2. XP acumula e desbloqueia **níveis** (1 a 30)
3. Cada nível tem **recompensas Free** (para todos) e **Premium** (para quem ativou o pass premium)
4. A temporada dura **30 dias** (configurável), após os quais o progresso reseta

### Fontes de XP

| Ação | XP Padrão |
|------|-----------|
| Kill (PvP) | 50 XP |
| Minerar diamante | 10 XP |
| Minerar netherite (Ancient Debris) | 25 XP |
| Login diário (1x por dia) | 30 XP |

### Tipos de Recompensa

| Tipo | Descrição |
|------|-----------|
| `money` | Dinheiro (Vault) |
| `claim_blocks` | Blocos de claim extras |
| `crate_key` | Chaves de crate (campo `key_type`) |
| `title` | Título desbloqueável (campo `title_id`) |
| `cosmetic` | Cosmético desbloqueável (campo `cosmetic_id`) |
| `custom_item` | Item lendário (campo `item_id`) |

### Comandos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/pass` | `/battlepass`, `/bp` | Abre a GUI do Battle Pass | `gorvax.player` |
| `/pass info` | — | Mostra info da temporada no chat | `gorvax.player` |
| `/pass premium <nick>` | — | Ativa premium para um jogador | `gorvax.admin` |
| `/pass reset <nick>` | — | Reseta progresso de um jogador | `gorvax.admin` |
| `/pass reload` | — | Recarrega configurações | `gorvax.admin` |

### GUI do Battle Pass (Java)

GUI de 54 slots (6 linhas) com paginação:

| Área | Descrição |
|------|-----------|
| **Linha 0** | Barra de progresso XP (painéis verde/cinza) + Nether Star central com info |
| **Linhas 1-5** | 5 níveis por página: coluna 2 = Free, coluna 4 = indicador de nível, coluna 6 = Premium |
| **Linha 6** | Navegação: ◀ Anterior, ✖ Fechar, ▶ Próxima |

**Cores dos slots de reward:**
- 🟩 Verde = já resgatado
- 📦 Baú = disponível para resgate (clique!)
- ⬜ Cinza = nível não atingido
- 🚫 Barreira = premium não ativado

### Jogadores Bedrock

Jogadores Bedrock recebem um **SimpleForm nativo** via Floodgate com botões por nível. Ao clicar em um nível desbloqueado, o reward é resgatado automaticamente (Free primeiro, depois Premium).

### Configuração (`battlepass.yml`)

```yaml
enabled: true
season:
  number: 1
  name: "§6⚔ Temporada 1"
  start_date: "01/03/2026"
  duration_days: 30
xp_sources:
  kill: 50
  mine_diamond: 10
  mine_netherite: 25
  daily_login: 30
xp_per_level:
  base: 100
  increment: 20
rewards:
  1:
    free:
      - { type: money, amount: 500 }
    premium:
      - { type: crate_key, amount: 1, key_type: raro }
  # ... (até o nível 30)
```

### Placeholders (PlaceholderAPI)

| Placeholder | Resultado |
|-------------|-----------|
| `%gorvax_bp_level%` | Nível atual do Battle Pass |
| `%gorvax_bp_xp%` | XP atual |
| `%gorvax_bp_season%` | Número da temporada |
| `%gorvax_bp_premium%` | "Sim" ou "Não" |
| `%gorvax_bp_days%` | Dias restantes |

### Permissões

| Permissão | Descrição |
|-----------|-----------|
| `gorvax.player` | Permite usar `/pass` e resgatar rewards |
| `gorvax.admin` | Permite subcomandos admin (premium, reset, reload) |

### Persistência

Dados do Battle Pass são salvos no `playerdata.yml`:

```yaml
<uuid>:
  battle_pass_level: 12
  battle_pass_xp: 85
  battle_pass_premium: true
  battle_pass_season: 1
  battle_pass_claimed_free: [1, 2, 3, 4, 5]
  battle_pass_claimed_premium: [1, 2, 3]
```

### Reload

Battle Pass é recarregado via `/gorvax reload` ou `/pass reload`.

---

## ⚖️ 47. Sistema de Reputação / Karma (B18) 👥

O sistema de Karma reflete o comportamento de cada jogador no servidor. Ações positivas (participar de bosses, vender no mercado, completar quests) aumentam karma; ações negativas (matar jogadores) diminuem.

### Como Funciona

- Cada jogador começa com **karma 0** (Neutro)
- Ações do jogador modificam o karma ao longo do tempo
- O karma determina um **rank de reputação** que afeta preços, interações e visibilidade

### Ranks de Karma

| Karma | Rank | Ícone | Efeito |
|------:|------|-------|--------|
| ≥ 100 | **Herói** | §a✦ | Desconto de 5% no mercado e em claims |
| ≥ 50 | **Bom** | §2☘ | Nenhum efeito especial |
| -49 a 49 | **Neutro** | §7⚖ | Padrão |
| ≤ -50 | **Vilão** | §c☠ | Preços 10% mais caros no mercado |
| ≤ -100 | **Procurado** | §4💀 | Preços 25% mais caros, bounty automática, localização revelada periodicamente |

### Ações que Alteram Karma

| Ação | Karma | Direção |
|------|------:|--------|
| Matar outro jogador | -3 | ⬇ Perde |
| Participar de boss | +5 | ⬆ Ganha |
| Vender no mercado | +1 | ⬆ Ganha |
| Completar quest | +2 | ⬆ Ganha |

> Os valores são configuráveis em `config.yml` (seção `karma.*`).

### Efeitos Detalhados

#### Desconto de Mercado (Herói)
Jogadores com karma ≥ 100 recebem **5% de desconto** em todas as compras no mercado global e no custo de claims.

#### Penalidade de Preço (Vilão / Procurado)
- **Vilão** (karma ≤ -50): preços **10% mais caros**
- **Procurado** (karma ≤ -100): preços **25% mais caros**

#### Bounty Automática (Procurado)
Ao atingir karma ≤ -100, o sistema automaticamente coloca uma **bounty de $500** na cabeça do jogador (configurável). Todos são notificados.

#### Revelação de Localização (Procurado)
Jogadores "Procurados" têm sua **localização (mundo, X, Z)** revelada periodicamente via broadcast global (a cada 5 minutos, configurável).

### Comandos

| Comando | Aliases | O que faz | Quem pode |
|---------|---------|-----------|----------|
| `/karma` | `/reputacao`, `/rep` | Mostra seu karma, rank e efeitos ativos | Todos |
| `/karma <nick>` | — | Mostra karma de outro jogador | Todos |
| `/karma top` | `/karma ranking` | Ranking: Top 10 Heróis e Top 10 Vilões | Todos |
| `/karma set <nick> <valor>` | `/karma setar` | Define o karma de um jogador | Admin |
| `/karma add <nick> <valor>` | `/karma adicionar` | Adiciona/remove karma | Admin |

### Configuração (`config.yml`)

```yaml
karma:
  enabled: true
  kill_penalty: 3           # Karma perdido por kill PvP
  boss_reward: 5            # Karma ganho por participar de boss
  market_reward: 1          # Karma ganho por venda no mercado
  quest_reward: 2           # Karma ganho por completar quest
  hero_discount: 5.0        # % desconto no mercado para Heróis
  hero_claim_discount: 5.0  # % desconto em claims para Heróis
  bounty_threshold: -100    # Karma para bounty automática
  auto_bounty_value: 500.0  # Valor da bounty automática
  reveal_interval_minutes: 5 # Intervalo de reveal de Procurados
```

### Placeholders (PlaceholderAPI)

| Placeholder | Resultado |
|-------------|----------|
| `%gorvax_karma%` | Valor numérico do karma |
| `%gorvax_karma_rank%` | Nome do rank (ex: Herói, Vilão) |
| `%gorvax_karma_label%` | Label colorida do rank (ex: §a✦ Herói) |

### Permissões

| Permissão | Descrição |
|-----------|----------|
| `gorvax.player` | Permite usar `/karma` e ver karma de outros |
| `gorvax.admin` | Permite subcomandos admin (set, add) |

### Persistência

O karma é salvo no `playerdata.yml`:

```yaml
<uuid>:
  karma: 42
```

### Reload

O sistema de karma é recarregado via `/gorvax reload`.

---

## 🔔 48. API de Eventos Customizados (B19)

O GorvaxCore expõe eventos Bukkit customizados que podem ser escutados por plugins externos ou futuras expansões do próprio GorvaxCore.

### Pacote

Todos os eventos ficam em `br.com.gorvax.core.events`.

### Classes Base

| Classe | Descrição |
|--------|-----------|
| `GorvaxEvent` | Classe abstrata base. Todos os eventos estendem dela. Campo `timestamp`. |
| `GorvaxCancellableEvent` | Estende `GorvaxEvent` + implementa `Cancellable`. Permite que listeners cancelem a ação. |

### Eventos Disponíveis

| Evento | Cancelável | Campos Principais | Onde é Disparado |
|--------|:----------:|-------------------|------------------|
| `KingdomCreateEvent` | ❌ | `Player founder`, `kingdomId`, `kingdomName` | `ConfirmCommand` (criação de claim/reino) |
| `KingdomDeleteEvent` | ❌ | `kingdomId`, `kingdomName`, `kingUUID` | `KingdomManager.deleteKingdom()` |
| `KingdomWarDeclareEvent` | ✅ | `attackerKingdomId`, `defenderKingdomId` | `WarManager.declareWar()` |
| `BossSpawnEvent` | ✅ | `bossId`, `location` | `BossManager.spawnBoss()` |
| `BossDeathEvent` | ❌ | `bossId`, `deathLocation`, `topDamagers` | `BossManager.rewardPlayers()` |
| `RaidStartEvent` | ✅ | `location` | `BossRaidManager.startRaid()` |
| `MarketTransactionEvent` | ✅ | `player`, `itemStack`, `price`, `type` | Mercado (compra/venda) |
| `AuctionEndEvent` | ❌ | `seller`, `winner`, `finalPrice`, `item` | Leilão (fim) |
| `ClaimCreateEvent` | ✅ | `player`, `claim` | `ClaimManager.createClaim()` |
| `ClaimEnterEvent` | ✅ | `player`, `claim` | `KingdomListener.onMove()` |
| `ClaimLeaveEvent` | ❌ | `player`, `fromClaim` | `KingdomListener.onMove()` |
| `DuelEndEvent` | ❌ | `winner`, `loser`, `betAmount`, `isDraw` | `DuelManager.endDuel()` / `endDuelDraw()` |
| `KillStreakEvent` | ❌ | `player`, `streak` | `CombatManager.incrementKillStreak()` |

### Exemplo de Uso (para desenvolvedores)

```java
import br.com.gorvax.core.events.BossSpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MeuListener implements Listener {

    @EventHandler
    public void onBossSpawn(BossSpawnEvent event) {
        // Exemplo: cancelar spawn de boss em um mundo específico
        if (event.getLocation().getWorld().getName().equals("lobby")) {
            event.setCancelled(true);
        }
    }
}
```

### Comportamento de Eventos Canceláveis

Quando um evento cancelável (`isCancelled() == true`) é disparado:
- **KingdomWarDeclareEvent**: guerra não é declarada, custo não é cobrado
- **BossSpawnEvent**: boss não é spawnado
- **RaidStartEvent**: raid não inicia
- **ClaimCreateEvent**: claim não é registrado
- **ClaimEnterEvent**: movimento é cancelado (jogador não entra)
- **MarketTransactionEvent**: transação não é processada

---

## 🎨 48. Resource Pack & Texturas Customizadas (B21)

O GorvaxCore inclui um resource pack completo com modelos 3D para itens lendários e texturas customizadas para mini-bosses e world bosses.

### Estrutura do Resource Pack

```
resourcepack/
├── java/                     # Resource pack Java Edition
│   ├── pack.mcmeta           # pack_format: 34 (1.21+)
│   └── assets/
│       ├── minecraft/models/item/    # Overrides vanilla (CustomModelData)
│       ├── gorvax/models/item/       # Modelos 3D customizados
│       ├── gorvax/textures/item/     # Texturas pixel art 16×16
│       └── minecraft/optifine/cit/   # CIT configs para entidades
├── bedrock/                  # Resource pack Bedrock Edition
│   ├── manifest.json
│   └── textures/
│       ├── items/            # Texturas de itens
│       └── entity/           # Texturas de entidades
├── geyser_mappings.json      # Mapeamento Java→Bedrock
├── pack.bat                  # Script de empacotamento (Windows)
└── pack.sh                   # Script de empacotamento (Linux/Mac)
```

### Itens com Modelos 3D Customizados

Todos os 9 itens lendários possuem **CustomModelData** e modelos/texturas únicos:

| Item | Material Base | CustomModelData | Modelo |
|------|---------------|:---------------:|--------|
| ⚔️ Lâmina de Gorvax | Netherite Sword | 1001 | `gorvax_blade.json` |
| 👑 Coroa de Indrax | Netherite Helmet | 1001 | `indrax_crown.json` |
| 💨 Botas Velocistas | Diamond Boots | 1001 | `speed_boots.json` |
| 🏹 Arco do Caçador | Bow | 1001 | `hunter_bow.json` |
| 🛡️ Escudo do Guardião | Shield | 1001 | `guardian_shield.json` |
| 🔥 Manto de Vulgathor | Netherite Chestplate | 1002 | `vulgathor_mantle.json` |
| 🧊 Lança de Kaldur | Trident | 1002 | `kaldur_lance.json` |
| 💀 Elmo de Skulkor | Netherite Helmet | 1002 | `skulkor_helm.json` |
| 🌀 Pérola de Xylos | Netherite Axe | 1002 | `xylos_pearl.json` |

### Texturas de Mini-Bosses (OptiFine CIT)

Mini-bosses recebem texturas customizadas 64×64 via OptiFine CIT (por CustomName):

| Mini-Boss | Entidade Base | Textura |
|-----------|---------------|--------|
| 🏜️ Guardião do Deserto | Husk | `guardiao_deserto.png` |
| ❄️ Sentinela Gélida | Stray | `sentinela_gelida.png` |
| 🕷️ Araña da Selva | Cave Spider | `aranha_selva.png` |
| 🔥 Fantasma do Nether | Blaze | `fantasma_nether.png` |

### Texturas de World Bosses (OptiFine CIT)

| World Boss | Entidade Base | Textura |
|------------|---------------|--------|
| 👑 Rei Gorvax | Wither Skeleton | `king_gorvax.png` |
| 🌊 Indrax Abissal | Drowned | `indrax_abissal.png` |
| 🔥 Vulgathor | Blaze | `vulgathor.png` |
| 🌿 Zarith | Witch | `zarith.png` |
| 🧊 Kaldur | Stray | `kaldur.png` |
| 💀 Skulkor | Enderman | `skulkor.png` |
| 🐗 Xylos Devorador | Hoglin | `xylos.png` |

### Como Empacotar

1. Na pasta `resourcepack/`, execute `pack.bat` (Windows) ou `bash pack.sh` (Linux/Mac)
2. Serão gerados `GorvaxCore-Java.zip` e `GorvaxCore-Bedrock.zip`
3. Configure `server.properties` → `resource-pack=URL_DO_ZIP`
4. Para Bedrock: copie `GorvaxCore-Bedrock.zip` e `geyser_mappings.json` para o Geyser

### Compatibilidade Bedrock (Geyser)

O arquivo `geyser_mappings.json` traduz automaticamente os CustomModelData do Java para identificadores Bedrock, permitindo que jogadores Bedrock vejam os modelos customizados sem configuração extra.

---

## 💾 59. Autosave Periódico Global (B44)

O GorvaxCore agora salva **todos os dados automaticamente** a cada 5 minutos (configurável), protegendo contra perda de dados em caso de crash ou `kill -9`.

### O que é salvo

Todos os managers com persistência YAML:

| Manager | Dados |
|---------|-------|
| `ClaimManager` | Claims e terrenos |
| `PlayerDataManager` | Dados de jogadores |
| `KingdomManager` | Reinos e configurações |
| `MarketManager` | Mercado global e local |
| `AuditManager` | Log de auditoria |
| `AuctionManager` | Leilões ativos |
| `PriceHistoryManager` | Histórico de preços |
| `MailManager` | Correio entre jogadores |
| `VoteManager` | Votações |
| `BountyManager` | Recompensas por cabeça |
| `NationManager` | Nações |
| `StructureManager` | Estruturas/POIs |

### Configuração (`config.yml`)

```yaml
autosave:
  enabled: true
  interval_minutes: 5     # Intervalo entre autosaves (em minutos)
```

### Comportamento

- A task roda **assíncronamente** para não causar lag na main thread
- Log no console após cada autosave: `§b[Gorvax] §aAutosave concluído em Xms.`
- **Cancelada automaticamente** no `onDisable()` antes do save manual
- Se desativada via config (`autosave.enabled: false`), o plugin informa no console

### Mensagens (`messages.yml`)

```yaml
admin:
  autosave_complete: "§b[Gorvax] §aAutosave concluído em {0}ms."
```

---

## 📋 Histórico de Atualizações do Manual

| Data | Versão/Batch | Alterações |
|------|-------------|------------|
| 2026-02-23 | v1.0.0 | Manual criado com todas as funcionalidades do lançamento |
| 2026-02-23 | B1 (v1.0.0) | Adicionada seção 34 "Proteção Anti-Exploit", atualizada lista de proteções na seção 12 |
| 2026-02-23 | B2 (v1.0.0) | Adicionada seção 35 "Sistema de Combate" (Combat Tag, PvP Logger, Kill Streaks, Proteção de Spawn). Atualizada lista de mensagens |
| 2026-02-23 | B3 (v1.0.0) | Adicionada seção 36 "Sistema de Duelos" (PvP 1v1 com aposta, integração Bedrock ModalForm) |
| 2026-02-24 | B4 (v1.0.0) | Adicionada seção 37 "Tutorial Interativo + Welcome Kit" (tutorial em 6 passos, kit de boas-vindas, recompensas de conclusão) |
| 2026-02-24 | B5 (v1.0.0) | Adicionada seção 38 "Recompensas Diárias" (Daily Rewards & Login Streak com GUI, ciclo de 7 dias, adaptação Bedrock) |
| 2026-02-24 | B6 (v1.0.0) | Adicionada seção 39 "Menu Central GUI" (/menu com 54 slots, 10 ícones, adaptação Bedrock SimpleForm) |
| 2026-02-24 | B7 (v1.0.0) | Adicionada seção 40 "Leaderboards & Rankings" (/top com 8 categorias, PAPI placeholders, cache periódico) |
| 2026-02-24 | B8 (v1.0.0) | Adicionada seção 41 "Integração Discord (Webhook)" — webhook MC→Discord com chat sync e 5 tipos de alertas |
| 2026-02-24 | B10 (v1.0.0) | Adicionada seção 42 "Custom Items" — Armas e armaduras lendárias com efeitos on-hit e passivos, /customitem give/list |
| 2026-02-24 | B12 (v1.0.0) | Adicionada seção 43 "Sistema de Crates/Keys" — 4 tipos de crate, GUI roleta animada, preview, integração Daily Rewards, /crate com 7 subcomandos, Bedrock forms |
| 2026-02-24 | B13 (v1.0.0) | Adicionada seção 44 "Sistema de Cosméticos" — partículas de caminhada, arrow trails, chat tags, kill effects/particles, GUI de categorias, /cosmetics com 5 subcomandos, cosmetics.yml com 25 cosméticos |
| 2026-02-24 | B14 (v1.0.0) | Adicionada seção 45 "Sistema VIP & Ranks Premium" — 4 tiers (VIP/VIP+/ELITE/LENDÁRIO), benefícios escaláveis, integração LuckPerms, /vip com 7 subcomandos, placeholders PAPI, VIP_PRICING.md |
| 2026-02-24 | B15 (v1.0.0) | Adicionada seção 46 "Battle Pass Sazonal" — 30 níveis, track Free/Premium, GUI 54 slots com paginação, Bedrock SimpleForm, /pass com 5 subcomandos, XP por kills/mineração/login, 6 tipos de reward, 5 placeholders PAPI |
| 2026-02-28 | B18 (v1.0.0) | Adicionada seção 47 "Sistema de Reputação / Karma" — 5 ranks (Herói/Bom/Neutro/Vilão/Procurado), descontos e penalidades no mercado, bounty automática para Procurados, reveal de localização, /karma com 5 subcomandos, 3 placeholders PAPI, 10 configs em config.yml |
| 2026-02-28 | B19 (v1.0.0) | Adicionada seção 48 "API de Eventos Customizados" — 2 classes base + 13 eventos customizados, disparos em 9 classes, exemplo de uso para desenvolvedores |
| 2026-02-28 | B20 (v1.0.0) | Refatoração de God Classes: `KingdomCommand.java` reduzido de 1557→430 linhas (~72% redução). 6 subcomandos extraídos para `towns/commands/`: Bank, Diplomacy, Member, Outpost, War, Vote. Sem novas seções — refatoração interna sem impacto visível ao usuário |
| 2026-02-28 | B21 (v1.0.0) | Adicionada seção 48 "Resource Pack & Texturas Customizadas" — 5 modelos 3D de itens lendários, 4 texturas de mini-bosses, 7 texturas de world bosses, OptiFine CIT configs, mapeamento Geyser, scripts de empacotamento |
| 2026-02-28 | B22 (v1.0.0) | Adicionada seção 49 "Sistema de Estruturas" — StructureManager para mapear reinos pré-construídos no mundo, /estrutura com 6 subcomandos (criar/deletar/lista/tp/info/reload), detecção de entrada/saída com Title/ActionBar, persistencia YAML, structures.yml |
| 2026-03-10 | B25 (v1.0.0) | Deduplicação de `isBedrockPlayer()` em 4 classes + seção 53 "Teleporte Aleatório (/rtp)" — /rtp nativo com cooldown, avoid claims, blocos inseguros, combat tag check |
| 2026-03-11 | B44 (v1.0.0) | Adicionada seção 59 "Autosave Periódico Global" — task async salvando 12 managers a cada 5 min (configurável), crash safety, config em `autosave.enabled`/`interval_minutes` |

---

## 🏠 49. Sistema de Estruturas (B22)

Permite mapear locais de builds (reinos, vilas, cidades) no mundo como pontos de interesse.
Prepara infraestrutura para NPCs, quests e mercados futuros.

### Comandos

| Comando | Descrição | Permissão |
|---------|-----------|----------|
| `/estrutura lista` | Lista todas as estruturas com distância | Todos |
| `/estrutura tp <id>` | Teleporta ao centro da estrutura | Todos |
| `/estrutura info [id]` | Mostra informações (ou da que você está dentro) | Todos |
| `/estrutura criar <id> <nome> <tema> <raio>` | Cria estrutura na posição atual | Admin |
| `/estrutura deletar <id>` | Remove uma estrutura | Admin |
| `/estrutura reload` | Recarrega structures.yml | Admin |

### Temas disponíveis

`deserto`, `gelo`, `nether`, `floresta`, `medieval`, `porto`, `montanha`, `pantano`

### Eventos ao jogador

- **Entrando em uma estrutura**: Aparece um Title bonito com o nome e tema
- **Saindo de uma estrutura**: ActionBar informando a saída

### Config (structures.yml)

```yaml
estruturas:
  reino_deserto:
    nome: "§6Reino do Deserto"
    tema: deserto
    mundo: world
    centro:
      x: 1500.0
      y: 72.0
      z: 2300.0
      yaw: 0.0
      pitch: 0.0
    raio: 150
    criado_por: Gorska
    criado_em: "2026-02-28"
```

### Aliases

`/estrutura`, `/structure`, `/estruturas`, `/poi`

---

## 📖 50. Sistema de Lore — Livros & Totems

O sistema de lore enriquece o mundo com **livros interativos** em estantes e **totems narrativos** em biomas específicos.

### Visão Geral

| Recurso | Descrição |
|---------|----------|
| **Livros de Lore** | Livros escritos com histórias do universo GorvaxMC. Encontrados em estantes especiais. |
| **Estantes de Lore** | Blocos de `BOOKSHELF` em coordenadas configuradas. Clique direito → recebe o livro. |
| **Totems de Lore** | Blocos especiais (Lodestone, etc.) em biomas. Clique direito → texto narrativo no chat. |

### Como Funciona

1. **Estantes**: Clique direito numa estante em coordenada registrada → recebe `WRITTEN_BOOK` com a história. Se já possui, recebe aviso.
2. **Totems**: Clique direito num bloco de totem → texto de lore no chat. Cooldown de **30s** para evitar spam.
3. **Partículas**: Estantes e totems emitem partículas de encantamento para indicar que são interativos (✨).

### Configuração (`lore_books.yml`)

```yaml
# Definição dos livros
books:
  ascensao_gorvax:
    title: "§6§lA Ascensão de Gorvax"
    author: "§7Cronista Aldric"
    pages:
      - "&0Há séculos, quando o mundo..."
      - "&0O Rei Gorvax ergueu sua..."

# Estantes (coordenadas do bloco BOOKSHELF)
bookshelves:
  - book: ascensao_gorvax
    world: world
    x: 100
    y: 65
    z: 200

# Totems de bioma
totems:
  totem_deserto:
    world: world
    x: 500
    y: 72
    z: -300
    block: LODESTONE
    lore:
      - "§7§o[Sussurros do Deserto]"
      - "§fAs areias lembram..."
```

### Como Testar

1. Defina as coordenadas reais no `lore_books.yml`
2. Coloque um bloco de `BOOKSHELF` na coordenada configurada
3. Clique direito na estante → deve receber o livro
4. Clique novamente → mensagem "já possui"
5. Para totems: coloque o bloco (ex: `LODESTONE`) na coordenada, clique direito → texto no chat
6. Aguarde cooldown e tente novamente

### Indicadores Visuais

- ✨ Estantes de lore emitem **partículas de encantamento** (`ENCHANT`) a cada 2s
- 🗿 Totems de lore emitem **partículas de portal do End** (`PORTAL`) a cada 2s
- Isso permite que jogadores identifiquem blocos interativos antes de clicar

### Mensagens Configuráveis (`messages.yml`)

```yaml
lore:
  book_received: "§a§l📖 LORE §8» §fVocê encontrou um tomo antigo!"
  already_has_book: "§e§l📖 LORE §8» §7Você já possui este livro."
  inventory_full: "§c§lERRO §8» §7Sem espaço no inventário."
  totem_cooldown: "§e§l🗿 TOTEM §8» §7Aguarde §e{0}s §7para interagir."
```

### Arquivos

| Arquivo | Função |
|---------|--------|
| `lore_books.yml` | Config de livros, estantes e totems |
| `LoreManager.java` | Carrega config, cria livros |
| `LoreListener.java` | Eventos de interação + partículas |

---

## 🎁 51. Crates Físicas no Spawn

Além da GUI de seleção, o GorvaxCore suporta **crates físicas** — blocos no spawn que abrem diretamente um tipo específico de crate.

### Como Funciona

- Cada tipo de crate (Comum, Raro, Lendário, Sazonal) tem um **bloco físico** em coordenadas fixas
- Ao clicar com botão direito, o **tipo é identificado pela coordenada** e abre diretamente
- Se o bloco não está registrado, abre a **GUI de seleção** (comportamento normal)
- O jogador precisa ter uma **key** do tipo correspondente
- Partículas de chama indicam visualmente onde ficam as crates

### Configuração (`crates.yml`)

```yaml
# Na raiz do crates.yml:
physical_crates:
  comum:
    world: world
    x: 10
    y: 65
    z: 20
  raro:
    world: world
    x: 15
    y: 65
    z: 20
  lendario:
    world: world
    x: 20
    y: 65
    z: 20
  sazonal:
    world: world
    x: 25
    y: 65
    z: 20
```

### Como Testar

1. Configure coordenadas reais em `crates.yml` → `physical_crates`
2. Coloque Ender Chests nas coordenadas
3. Dê keys: `/crate give <jogador> comum 1`
4. Clique direito no Ender Chest registrado → abre a crate diretamente
5. Clique num Ender Chest NÃO registrado → abre GUI de seleção

### Indicadores Visuais

- 🔥 Crates físicas emitem **partículas de chama** (`FLAME`) em espiral a cada 2s

### Arquivos

| Arquivo | Função |
|---------|--------|
| `crates.yml` → `physical_crates` | Coordenadas das crates físicas |
| `CrateManager.java` | Lookup por coordenada |
| `CrateListener.java` | Interação + abertura direta |

---

## ⚔ 52. Quests de Lore — Missões Narrativas

Missões narrativas permanentes que contam a história do universo GorvaxMC por objetivos multi-step.

### Diferenças vs Quests Diárias/Semanais

| Recurso | Diárias/Semanais | Lore Quests |
|---------|-----------------|-------------|
| Reset | Diário/Semanal | **Nunca** (permanente) |
| Progresso | Por quest | **Por step** (cadeia) |
| Seleção | Aleatória por seed | **Todas ativas** desde o 1º login |
| Narrativa | Sem diálogo | **Diálogo a cada step** |
| Rewards | Dinheiro, keys, blocos | Dinheiro, **karma, títulos, livros de lore** |

### Quests Disponíveis

#### ⚒ O Ferreiro Ancestral
- **Steps**: Mine 2x Ancient Debris → Mate 10x Wither Skeleton → Participe de 1 boss
- **Rewards**: $5.000 + 10 karma + título `§6[⚒ Herdeiro de Ashvale]` + livro "A Ascensão de Gorvax"

#### ❄ O Segredo de Glacius
- **Steps**: Mine 16x Blue Ice → Mate 15x Stray → Mate 50 mobs quaisquer
- **Rewards**: $4.000 + 8 karma + título `§b[❄ Discípulo de Glacius]` + livro "A Queda de Indrax"

#### ✦ Fragmentos de Étheris
- **Steps**: Mate 10x Enderman → Mine 32x Obsidian → Participe de 2 boss fights
- **Rewards**: $7.500 + 15 karma + título `§5[✦ Guardião dos Fragmentos]` + livro "A Profecia"

### Fluxo Interno

1. Jogador realiza ações normais (matar, minerar, boss) → hooks automáticos
2. `QuestManager` verifica step atual de cada lore quest ativa
3. Se `type` + `target` combinam → incrementa progresso
4. **Step completo** → diálogo narrativo no chat + avança step
5. **Quest completa** → som de conquista + rewards automáticos

### Configuração (`quests.yml`)

```yaml
lore_quests:
  ferreiro_ancestral:
    name: "§6§lO Ferreiro Ancestral"
    description: "Descubra a história de Ashvale."
    icon: ANVIL
    steps:
      - type: MINE_BLOCK
        target: ANCIENT_DEBRIS
        amount: 2
        dialogue: "§7§oFerreiro: §f'Esses restos...'"
      - type: KILL_MOB
        target: WITHER_SKELETON
        amount: 10
        dialogue: "§7§oFerreiro: §f'Os servos do Rei...'"
      - type: BOSS_PARTICIPATE
        target: ANY
        amount: 1
        dialogue: "§7§oFerreiro: §f'Você sobreviveu...'"
    reward:
      money: 5000
      karma: 10
      title: "§6[⚒ Herdeiro de Ashvale]"
      book: ascensao_gorvax   # bookId do lore_books.yml
```

#### Tipos de Step Suportados

| Tipo | Descrição | Target |
|------|-----------|--------|
| `MINE_BLOCK` | Minerar blocos | `ANCIENT_DEBRIS`, `BLUE_ICE`, `OBSIDIAN` |
| `KILL_MOB` | Matar mobs | `WITHER_SKELETON`, `STRAY`, `ENDERMAN`, `ANY` |
| `BOSS_PARTICIPATE` | Participar de boss fight | `ANY` |
| `KILL_PLAYER` | Matar jogadores | `PLAYER` |
| `SELL_MARKET` | Vender no mercado | `ANY` |

### Como Testar

1. **Carregamento**: Console → `[Lore] 3 lore quests carregadas.`
2. **Step 1**: Mine os blocos exigidos → diálogo + mensagem de step completo
3. **Step 2**: Mate os mobs exigidos → diálogo do step 2
4. **Step 3**: Participe de boss → som de conquista + rewards
5. **Persistência**: Saia e entre → progresso mantido
6. **playerdata.yml**: Verifique `lore_quest_step`, `lore_quest_step_progress`, `completed_lore_quests`

### Persistência (`playerdata.yml`)

```yaml
<uuid>:
  lore_quest_step:
    ferreiro_ancestral: 1      # Step 2 (0-indexed)
    segredo_glacius: 0          # Step 1
  lore_quest_step_progress:
    ferreiro_ancestral: 5       # 5/10 Wither Skeletons
    segredo_glacius: 3          # 3/16 Blue Ice
  completed_lore_quests:
    - fragmentos_etheris
```

### Mensagens (`messages.yml`)

```yaml
quests:
  lore_quest_completed: "§6§l⚔ LORE QUEST COMPLETA §8» §fVocê finalizou §e{0}§f!"
  lore_step_completed: "§a§l📜 PASSO CONCLUÍDO §8» §f{0} §7— Passo §e{1}§7/§e{2}"
```

### Diálogos de Boss (Complemento à Seção 7)

Cada boss agora possui diálogos de lore automáticos:
- **No spawn**: Diálogo aleatório de `dialogues.spawn` exibido para todos
- **Ao matar jogador**: Diálogo de `dialogues.kill_player` enviado à vítima

Exemplo em `boss_settings.yml`:

```yaml
gorvax:
  dialogues:
    spawn:
      - "§6§l[Rei Gorvax] §f'Ajoelhem-se... o fogo retorna!'"
    kill_player:
      - "§6§l[Rei Gorvax] §f'Fraco demais para meu reino.'"
```

### Arquivos

| Arquivo | Função |
|---------|--------|
| `quests.yml` → `lore_quests` | Definição das quests, steps, diálogos e rewards |
| `QuestManager.java` | `addLoreProgress()`, `applyLoreReward()` |
| `QuestListener.java` | Hooks automáticos para kills/mines |
| `BossManager.java` | Hook `BOSS_PARTICIPATE` na morte de bosses |
| `PlayerData.java` | Campos de progresso de lore quests |
| `PlayerDataManager.java` | Save/load de progresso |

---

## 📊 53. Métricas Anônimas (bStats) — B32

O GorvaxCore coleta **métricas anônimas e agregadas** de uso do plugin via [bStats](https://bstats.org/).

### O que é coletado

| Métrica | Tipo | Exemplo |
|---------|------|---------|
| Versão do servidor | Texto | `Paper 1.21` |
| Bedrock habilitado | Sim/Não | `Sim` |
| Plugin de economia | Texto | `EssentialsX Economy` |
| Total de jogadores registrados | Número | `42` |
| Total de reinos | Número | `8` |
| Total de claims | Número | `15` |
| Bosses ativos | Número | `2` |
| Temporada de Battle Pass | Ativa/Inativa | `Ativa` |
| Features habilitadas | Lista | `Crates, Quests, Códex...` |
| Distribuição de bosses | World/Mini | `3 World, 1 Mini` |

### O que **NÃO** é coletado

- ❌ Nomes de jogadores
- ❌ IPs de jogadores
- ❌ Dados pessoais de qualquer tipo
- ❌ Inventários, saldos ou itens

### Como desabilitar

Edite `plugins/bStats/config.yml`:

```yaml
enabled: false
```

> 💡 Este arquivo é gerenciado pelo bStats e não pelo GorvaxCore. O opt-out é global para todos os plugins do servidor.

### Dashboard

As métricas podem ser visualizadas publicamente no [dashboard do bStats](https://bstats.org/) (após registro do plugin).

---

## 🔄 54. Migração Automática de Configurações — B33

O GorvaxCore inclui um sistema de **migração automática de configurações** que garante compatibilidade ao atualizar o plugin. Configs antigos são migrados automaticamente para a versão mais recente, preservando os valores customizados do administrador.

### Como Funciona

1. Ao iniciar, o plugin verifica o campo `config_version` no `config.yml`
2. Se a versão é menor que a mais recente, cria um **backup automático** (`config.yml.backup-YYYY-MM-DD`)
3. Aplica **migrações sequenciais** (V0→V1, V1→V2, etc.) — cada uma adiciona novas chaves sem sobrescrever valores existentes
4. Atualiza `config_version` e salva o config
5. Também migra `messages.yml` com novas mensagens de versões recentes

### Backup Automático

- Backup criado antes de qualquer alteração: `config.yml.backup-2026-03-10`
- Se já existe backup do mesmo dia, sufixo numérico é adicionado: `config.yml.backup-2026-03-10-1`
- Se o backup falha, a migração é **cancelada por segurança**

### Comando Admin

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/gorvax migrateconfig` | Força re-migração de configurações | `gorvax.admin` |

> 💡 Útil para re-aplicar migrações após restaurar um backup antigo ou resolver problemas.

### Mensagens no Console

```
[ConfigMigrator] config.yml está na v0, versão mais recente é v1.
[ConfigMigrator] Backup criado: config.yml.backup-2026-03-10
[ConfigMigrator] Aplicando migração: V0 → V1: Adiciona config_version e chaves padrão B32+
[ConfigMigrator] Migração V0 → V1 aplicada com sucesso.
[ConfigMigrator] Migração concluída! 1 step(s) aplicado(s). Versão atual: v1
```

### Extensibilidade

Para adicionar novas migrações em versões futuras:

1. Criar nova classe em `migration/migrations/` (ex: `V1_to_V2.java`)
2. Implementar `MigrationStep` com `fromVersion()`, `toVersion()` e `apply()`
3. Registrar no `ConfigMigrator.registerSteps()`
4. Incrementar `LATEST_VERSION` no `ConfigMigrator`

### Arquivos

| Arquivo | Função |
|---------|--------|
| `MigrationStep.java` | Interface para cada patch de migração |
| `ConfigMigrator.java` | Motor principal (backup, sequência, save) |
| `migrations/V0_to_V1.java` | Primeira migração (config_version + chaves B32+) |

---

## 📚 55. Códex de Gorvax (Enciclopédia Interativa)

O Códex é uma enciclopédia in-game que os jogadores desbloqueiam progressivamente ao interagir com o mundo.

### Visão Geral

- **Entradas organizadas por categorias** (Mobs, Biomas, Itens, Lore, etc.)
- **Desbloqueio por ações**: matar mobs, explorar biomas, encontrar itens, completar quests
- **Barra de progresso** por categoria e global
- **Recompensas** ao completar categorias inteiras

### Comandos

| Comando | Aliases | O que faz | Permissão |
|---------|---------|-----------|-----------|
| `/codex` | `/enciclopedia`, `/wiki` | Abre o Códex (GUI interativa) | `gorvax.player` |

### Menu GUI

- **Tela principal**: Lista de categorias com barra de progresso
- **Por categoria**: Entradas desbloqueadas (iluminadas) e bloqueadas (vidro cinza)
- **Detalhes**: Clique numa entrada desbloqueada para ver descrição, dicas e lore

### Configuração (`codex.yml`)

```yaml
categories:
  mobs:
    name: "§c⚔ Bestiário"
    icon: ZOMBIE_HEAD
    entries:
      zombie:
        name: "§fZombi"
        description: "O morto-vivo mais comum..."
        unlock_type: KILL_MOB
        unlock_target: ZOMBIE
        unlock_amount: 10
      enderman:
        name: "§5Enderman"
        description: "Criatura misteriosa do End..."
        unlock_type: KILL_MOB
        unlock_target: ENDERMAN
        unlock_amount: 5
```

### Placeholders (PlaceholderAPI)

| Placeholder | Descrição | Exemplo |
|-------------|-----------|---------|
| `%gorvax_codex_unlocked%` | Entradas desbloqueadas | `18` |
| `%gorvax_codex_total%` | Total de entradas | `42` |
| `%gorvax_codex_percent%` | Percentual de completude | `42` |
| `%gorvax_codex_category_<catId>%` | Progresso por categoria | `5/8` |

### Persistência

Entradas desbloqueadas são salvas no `playerdata.yml`:

```yaml
<uuid>:
  unlockedCodex:
    - "mobs.zombie"
    - "mobs.enderman"
    - "biomes.desert"
```

### Arquivos

| Arquivo | Função |
|---------|--------|
| `codex.yml` | Definição de categorias e entradas |
| `CodexManager.java` | Lógica de unlock, progresso e recompensas |
| `CodexGUI.java` | Interface gráfica do Códex |
| `CodexCommand.java` | Comando `/codex` |
| `CodexListener.java` | Hooks automáticos (kills, mining, etc.) |

---

## 🏅 56. Sistema de Ranks e Kits

Sistema de progressão de rank por gameplay, com kits desbloqueáveis por nível.

### Ranks de Progressão

| Rank | Requisitos |
|------|------------|
| ⛺ Aventureiro | Início (padrão) |
| ⚔️ Guerreiro | 10h playtime, 50 kills |
| 🛡️ Cavaleiro | 50h playtime, 200 kills, 3 bosses |
| 👑 Herói | 100h playtime, 500 kills, 10 bosses, $50.000 |
| 🐉 Lendário | 250h playtime, 1000 kills, 25 bosses, $200.000 |

### Comandos

| Comando | Aliases | O que faz | Permissão |
|---------|---------|-----------|-----------|
| `/rank` | `/ranks`, `/progresso`, `/rankup` | Ver progressão e requisitos | `gorvax.player` |
| `/kit` | `/kits` | Abre menu de kits disponíveis por rank | `gorvax.player` |

### Kits por Rank

Cada rank desbloqueia um kit com cooldown:

| Rank | Kit | Cooldown |
|------|-----|----------|
| Aventureiro | Ferramentas de pedra, comida | 24h |
| Guerreiro | Ferramentas de ferro, poções | 24h |
| Cavaleiro | Ferramentas de diamante, golden apples | 48h |
| Herói | Netherite parcial, ender pearls | 72h |
| Lendário | Netherite completo, totems | 7 dias |

### Archivos

| Arquivo | Função |
|---------|--------|
| `RankManager.java` | Lógica de progressão e verificação de requisitos |
| `KitManager.java` | Entrega de kits com cooldown |
| `RankCommand.java` | Comando `/rank` |
| `KitCommand.java` | Comando `/kit` |

---

## 🎨 57. Tooltips Detalhados nos Menus (B39)

Todos os botões de menu possuem **tooltips expandidos** (lore) com 4-6 linhas de informação:

### Estrutura dos Tooltips

Cada botão segue o padrão:
1. **Descrição** — O que a funcionalidade faz
2. **Linha em branco** — Separador visual
3. **Comandos** — Como acessar a funcionalidade
4. **Detalhes** — Informações úteis sobre mecânicas
5. **💡 Dica** — Sugestão rápida para o jogador

### Menus Afetados

- **Menu Central** (`/gorvax menu`) — 25 botões em 2 páginas
- **Hub de Teleportes** (`/tp-hub`) — 5 botões de destino
- **Recompensas Diárias** (`/daily`) — Hint de streak nos itens
- **Battle Pass** (`/pass`) — Hint de XP no item de temporada
- **Crates** (`/crate`) — Hint de preview nos slots
- **Códex** (`/codex`) — Hint de exploração no progresso

### Personalização

Todos os textos ficam em `messages.yml`. Linhas são separadas por `\n`:

```yaml
# Exemplo:
main_menu:
  kingdom_lore: "§7Gerencie seu reino.\n§7\n§e💡 Dica: §fUse /reino"
```

---

## ⚙ 58. Menu de Configurações Expandido (B41)

O botão **⚙ Configurações** no menu principal agora abre um **submenu dedicado** com todos os toggles pessoais do jogador, em vez de apenas alternar o HUD.

### GUI (Java)

Menu de 27 slots (3 linhas) com 3 opções + botão de voltar:

| Slot | Botão | Ação | Estado Visual |
|------|-------|------|---------------|
| 10 | 📊 HUD / Scoreboard | Liga/desliga a sidebar | `LIME_DYE` (✅) / `GRAY_DYE` (❌) |
| 12 | 🔊 Som de Fronteira | Liga/desliga som ao cruzar claims | `LIME_DYE` (✅) / `GRAY_DYE` (❌) |
| 14 | 💬 Canal de Chat | Cicla entre canais (Global → Reino → ...) | `OAK_SIGN` com canal atual na lore |
| 22 | ◀ Voltar | Retorna ao menu principal | `ARROW` |

### Comportamento

- **Toggles in-place**: ao clicar em HUD ou Som, o menu recarrega imediatamente mostrando o novo estado (sem fechar e reabrir)
- **Chat**: cicla pelos 6 canais: Global → Reino → Aliança → Local → Comércio → Nação
- **Bedrock**: jogadores no Bedrock recebem um SimpleForm com botões que mostram estado (✅/❌)

### Comandos Equivalentes

| Botão | Comando alternativo |
|-------|---------------------|
| HUD | `/gorvax hud` |
| Som | `/gorvax som` |
| Chat | `/chat <canal>` |

