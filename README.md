# 🐉 GorvaxCore - O Coração do Seu Mundo

**Versão:** 1.0.0  
**Minecraft:** Paper 1.21+  
**Autores:** Gorska

Bem-vindo ao **GorvaxCore**, o plugin definitivo para transformar seu servidor de Minecraft em um reino vivo, dinâmico e repleto de desafios lendários. Este é o motor de civilizações, economia pulsante e invocador de bestas ancestrais.

---

## ✨ Características Principais

### 🏰 Sistema de Reinos (Kingdoms)
- **Proteção Total de Território** com claims personalizáveis
- **Hierarquia Completa**: Rei → Vice → Súditos
- **Sistema de Feudos (Lotes)**: Divida seu reino e venda/alugue terrenos
- **Economia Imobiliária**: Venda e aluguel automático com cobrança diária
- **Buffs de Reino**: Bônus passivos para súditos (Speed, Sorte, Preservação)
- **Diplomacia**: Aliança, rivalidade e neutralidade entre reinos
- **Guerra entre Reinos**: Sistema de pontos, rendição e espólios
- **Outposts**: Postos avançados não contíguos ao território principal
- **PvP Configurável**: Controle total sobre combate no reino

### 🌐 Sistema de Nações
- **Meta-Reinos**: Agrupe reinos em nações com Imperador no comando
- **Banco Nacional**: Depósitos e saques separados do reino
- **Buffs por Nível**: Velocidade e resistência compartilhadas
- **Chat de Nação**: Canal exclusivo (`/nc`)

### 💰 Economia Dinâmica
- **Mercado Global**: Preços que variam com oferta e demanda em tempo real
- **Mercado Local**: Venda entre jogadores com taxas municipais
- **Sistema de Leilão**: Leilões com anti-snipe, GUI paginada e broadcast
- **Histórico de Preços**: Tendências e variações nas últimas 24h
- **Sistema de Impostos**: Taxas diárias, upkeep por chunk, banco do reino
- **Normalização Automática**: Economia se auto-equilibra

### 💬 Chat Expandido
- **6 Canais**: Global, Local, Reino, Aliança, Comércio, Nação
- **Formatação por Hierarquia**: Rei 👑, Vice ⚔️, Nobre 🛡️, Súdito 🏠
- **Anti-Spam & Filtros**: Proteção contra flood e palavras proibidas
- **`/ignore`**: Silenciar jogadores indesejados
- **Títulos e Tags**: Integração com conquistas e reinos

### ⚔️ Sistema de Combate
- **Combat Tag**: Marcação de combate com penalidades por logout
- **PvP Logger**: Registro detalhado de combates
- **Kill Streaks**: Bônus por sequência de abates
- **Sistema de Duelos**: Lutas 1v1 com aposta de dinheiro (`/duel`)

### 👹 World Bosses & Mini-Bosses
- **7 Bosses Legendários**: Rei Gorvax, Indrax Abissal, Vulgathor, Xylos, Skulkor, Kaldur, Zarith
- **4 Mini-Bosses por Bioma**: Encontros espontâneos pelo mundo
- **Sistema de Dano Ranking**: Recompensas baseadas em contribuição
- **Loot Personalizado**: Itens lendários exclusivos por ranking
- **Agendamento**: Horários fixos semanais + spawn aleatório
- **Raids**: Ondas cooperativas de bosses com loot acumulativo
- **IA Avançada**: Anti-kite, fases de combate, targeting inteligente

### 🗡️ Custom Items & Crates
- **Armas e Armaduras Lendárias**: Itens exclusivos com habilidades especiais
- **Sistema de Crates**: 4 raridades (Comum, Raro, Lendário, Sazonal)
- **Keys**: Obtidas por gameplay, bosses, eventos ou loja
- **Preview de Chances**: Transparência total (`/crate preview`)

### 🏆 Progressão & Rankings
- **30+ Conquistas** desbloqueáveis com recompensas
- **Títulos Cosméticos** exibidos no chat
- **Leaderboards & Rankings** (`/top`) — kills, riqueza, playtime e mais
- **Sistema de Ranks**: Progressão por playtime, kills e conquistas
- **Kits por Rank**: Recompensas desbloqueáveis por nível
- **Estatísticas Completas**: Playtime, kills, mortes, KDR, dinheiro

### ✨ Cosméticos & VIP
- **Partículas**: Trails de caminhada e projétil
- **Kill Effects**: Efeitos visuais ao eliminar jogadores
- **Tags de Chat**: Emblemas cosméticos exclusivos
- **4 Tiers VIP**: VIP, VIP+, Elite, Lendário (conveniência, sem P2W)

### 🎫 Battle Pass & Quests
- **Battle Pass Sazonal**: Track Free + Premium, 30 níveis, renovação mensal
- **Quests Diárias e Semanais**: Missões com recompensas variadas
- **XP por Ações**: PvP, PvE, mineração e mais

### 🎃 Eventos Sazonais & Karma
- **8 Eventos Temáticos**: Natal, Halloween, Páscoa e mais
- **Bosses Sazonais**: Mobs exclusivos durante eventos
- **Sistema de Karma/Reputação**: Ações afetam sua reputação no servidor

### 📚 Descoberta & Exploração
- **Códex de Gorvax**: Enciclopédia interativa desbloqueável
- **Sistema de Estruturas**: POIs mapeados no mundo
- **Tutorial Interativo**: Guia para novos jogadores + Welcome Kit
- **Daily Rewards & Login Streak**: Recompensas por login diário
- **RTP**: Teleporte aleatório para locais seguros (`/rtp`)
- **Menu Central**: Hub de navegação para todos os sistemas (`/menu`)

### ✉️ Features Sociais
- **Correio (Cartas)**: Envie mensagens offline e online
- **Votação**: Decisões democráticas no reino
- **Bounties**: Recompensas por eliminação de jogadores

### 🌌 The End Dinâmico
- **Reset Automático**: Regeneração programada do The End
- **Ressurreição do Dragão**: Ender Dragon renasce automaticamente
- **Avisos e Teleporte**: Segurança para jogadores antes do reset

### 🔧 Integrações
- **Vault**: Economia completa
- **WorldGuard**: Proteção avançada
- **PlaceholderAPI**: 50+ placeholders para Scoreboards, TAB, Chat
- **LuckPerms**: Gerenciamento de permissões e ranks
- **GeyserMC/Floodgate**: Compatibilidade Bedrock com Forms nativos
- **Dynmap/BlueMap**: Visualização de reinos no mapa web
- **bStats**: Métricas anônimas de uso
- **Discord Webhooks**: Eventos do servidor no Discord

### 💾 Armazenamento
- **3 Backends**: YAML (legado), SQLite (padrão), MySQL (multi-servidor)
- **Migração Automática**: `/gorvax migrate <origem> <destino>`
- **Config Migration**: Atualização automática de configs entre versões
- **HikariCP**: Connection pooling para MySQL

### 🧪 Testes
- **880+ testes** unitários e de integração
- Cobertura de todos os sistemas principais

---

## 📦 Instalação

### Requisitos
- **Servidor**: Paper 1.21+ (Java 21)
- **Dependências Obrigatórias**:
  - Vault
  - WorldGuard
  - PlaceholderAPI
- **Dependências Opcionais**:
  - LuckPerms (Recomendado)
  - Floodgate/GeyserMC (Para Bedrock)
  - Dynmap ou BlueMap (Para mapa web)

### Passos
1. Coloque `GorvaxCore.jar` na pasta `plugins/`
2. Instale as dependências obrigatórias
3. Reinicie o servidor
4. Configure `config.yml` conforme necessário
5. Use `/gorvax reload` para aplicar mudanças

---

## 🎮 Guia Rápido

### Para Jogadores

**Criar um Reino:**
1. Segure uma Pá de Ouro
2. Use `/reino criar`
3. Clique nos dois cantos da área desejada
4. Use `/confirmar` ou `/c`
5. Nomeie com `/reinonome <nome>`

**Comprar/Vender no Mercado:**
- `/market` - Mercado global
- `/market local` - Mercado do seu reino

**Chat Privado do Reino:**
- `/rc <mensagem>` - Falar com seus súditos

### Para Reis

**Gerenciar Lotes:**
1. Segure uma Pá de Ferro
2. Selecione área dentro do reino
3. Use `/lote criar`
4. Configure `/lote preco <valor>`
5. Configure `/lote aluguel <valor>`

**Controlar PvP:**
```
/reino pvp global on/off       → PvP dentro do reino
/reino pvp moradores on/off    → PvP entre súditos
/reino pvp externo on/off      → Súditos podem PvP fora
```

### Para Administradores

**Gerenciar Sistema:**
```
/gorvax reload                 → Recarregar configs
/boss spawn [id]               → Spawnar boss
/gorvax reset end              → Resetar The End
/reino darblocos <nick> <qtd>  → Dar blocos extras
```

---

## 🧩 Placeholders (PlaceholderAPI)

### Exemplos de Uso

**Status do Jogador:**
```
%gorvax_blocos_total%          → 2500
%gorvax_blocos_disponiveis%    → 1200
%gorvax_blocos_usados%         → 1300
%gorvax_localizacao_label%     → §aReino Imperial §8(§b✔§8)
```

**Informações de Reino:**
```
%gorvax_reino_nome%            → Reino Imperial
%gorvax_reino_rei%             → Steve
%gorvax_reino_suditos%         → 15
%gorvax_reino_rank%            → Metrópole
%gorvax_reino_tag%             → [RI]
%gorvax_reino_tag_color%       → §b
```

**Eventos:**
```
%gorvax_next_boss%             → 15m 30s
```

📖 **Documentação Completa**: Veja [Placeholders.md](Placeholders.md) para lista completa

---

## ⚙️ Configuração

### config.yml - Principais Opções

```yaml
# Blocos de Claims
gameplay:
  blocks_gain_interval: 60      # Minutos para ganhar blocos
  blocks_gain_amount: 100       # Quantidade de blocos ganhos

# Reset do The End
end_reset:
  enabled: true
  dragon_reset_time: "03:00"    # Horário do reset do dragão
  dimension_reset_day: "MONDAY" # Dia do reset total
  dimension_reset_time: "04:00"
  warning_intervals: [60, 30, 10, 5, 2]  # Avisos em minutos

# World Bosses
boss:
  auto_spawn: true
  spawn_interval: 180           # Minutos entre spawns
  min_players: 3                # Mínimo de jogadores online
```

### boss_settings.yml - Configurar Bosses

```yaml
rei_gorvax:
  health: 5000
  damage: 25
  name: "§4§lREI GORVAX"
  
indrax_abissal:
  health: 4000
  damage: 20
  name: "§5§lINDRAX ABISSAL"
```

### boss_rewards.yml - Loot dos Bosses

Configure itens lendários, chances de drop e rewards por ranking.

---

## 🛡️ Permissões

### Grupos Padrão (LuckPerms)

**default** (Peso: 0)
- `gorvax.player`
- `gorvax.city.join/leave`
- `gorvax.plot.buy/sell`

**rei** (Peso: 10)
- `gorvax.mayor`
- `gorvax.city.create/rename/delete`
- `gorvax.city.claim/invite`

**gorvax-admin** (Peso: 100)
- `gorvax.admin`
- `minecraft.command.op`

📖 **Lista Completa**: Veja [Comandos.md](Comandos.md#️-permissões)

---

## 🐛 Problemas Conhecidos & FAQ

### Boss não spawna?
- Verifique `boss_settings.yml` e `boss_rewards.yml`
- Certifique-se que existem jogadores suficientes online
- Use `/boss start` para forçar spawn (admin)

### Aluguel não cobra?
- Sistema roda a cada intervalo configurado
- Verifique se player tem dinheiro
- Logs aparecem no console

### The End não reseta?
- Confirme horário em `config.yml`
- Use `/gorvax reset end` para forçar (admin)
- Verifique logs do console

---

## 📊 Performance

- **Otimizado para servidores grandes** (100+ jogadores)
- **Cálculos assíncronos** para economia dinâmica
- **Cache inteligente** para claims e permissões
- **Salvamento automático** sem lag spikes

---

## 🔄 Changelog

### v1.0.0 — Lançamento
- 🐉 Release inicial do GorvaxCore
- Reinos, Nações, Mercado, Leilão, Bosses, Chat, Diplomacia, Guerra
- Conquistas, Cosméticos, Battle Pass, Quests, Crates, Custom Items
- Mini-Bosses, Sistema de Karma, Eventos Sazonais, Códex
- Compatibilidade Java + Bedrock (Geyser/Floodgate)
- API Adventure, bStats, Config Migration automática
- 880+ testes unitários e de integração

---

## 📚 Documentação Adicional

- **[Comandos.md](Comandos.md)** - Lista completa de comandos
- **[Placeholders.md](Placeholders.md)** - Todos os placeholders PlaceholderAPI
- **[MANUAL.md](MANUAL.md)** - Manual completo do plugin
- **[ROADMAP.md](ROADMAP.md)** - Roadmap de evolução

---

## 🤝 Suporte & Contribuição

**Desenvolvido por:** Gorska  
**Versão:** 1.0.0  
**Última Atualização:** 2026-03-10

---

## 📜 Licença

Este plugin é fornecido "como está", sem garantias de qualquer tipo.

---

**GorvaxCore** - Onde impérios nascem e lendas são forjadas. 🐉👑
