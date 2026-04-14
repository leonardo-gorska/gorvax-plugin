# 🏗️ Arquitetura do GorvaxCore v1.0.0

## Visão Geral

GorvaxCore é um plugin monolítico organizado em módulos, com um padrão central de **Manager + Listener + Command** para cada sistema. Possui **84 classes Java** distribuídas em 8 pacotes principais.

## Estrutura de Pacotes

```
br.com.gorvax.core/
├── GorvaxCore.java              (Main Class — inicializa tudo)
│
├── boss/                        (Sistema de Bosses)
│   ├── BossTask.java            (Tick loop 1s — IA, fases, targeting)
│   ├── commands/
│   │   └── BossCommand.java     (/boss)
│   ├── listeners/
│   │   └── BossListener.java    (eventos de boss: dano, morte)
│   ├── managers/
│   │   ├── AtmosphereManager    (partículas, clima)
│   │   ├── BossManager          (ciclo de vida, spawn, despawn)
│   │   ├── BossRaidManager      (raids cooperativas)
│   │   ├── BossScheduleManager  (agendamento automático)
│   │   ├── ConfigManager        (boss_settings.yml)
│   │   └── LootManager          (drops por ranking)
│   ├── miniboss/
│   │   ├── MiniBoss, MiniBossCommand, MiniBossListener, MiniBossManager
│   └── model/
│       ├── WorldBoss.java       (classe abstrata)
│       ├── KingGorvax, IndraxAbissal, Vulgathor, XylosDevorador
│       ├── Skulkor, Kaldur, Zarith
│       ├── HalloweenBoss, NatalBoss   (bosses sazonais)
│
├── commands/                    (Comandos standalone)
│   ├── BattlePassCommand, ChatCommand, CodexCommand
│   ├── CosmeticCommand, GorvaxCommand, KingdomChatCommand
│   ├── KitCommand, QuestCommand, RankCommand
│   ├── ReputationCommand, RtpCommand, SeasonalEventCommand
│
├── events/                      (Eventos customizados)
│   ├── GorvaxEvent, GorvaxCancellableEvent   (base)
│   ├── BossSpawnEvent, BossDeathEvent
│   ├── ClaimCreateEvent, ClaimEnterEvent, ClaimLeaveEvent
│   ├── KingdomCreateEvent, KingdomDeleteEvent, KingdomWarDeclareEvent
│   ├── DuelEndEvent, KillStreakEvent
│   ├── AuctionEndEvent, MarketTransactionEvent
│
├── listeners/                   (Listeners globais)
│   ├── ChatListener, MarketListener, ProtectionListener
│   ├── VisualizationListener, KingdomListener
│   ├── KingdomSkillsListener, MenuListener, BossListener
│
├── managers/                    (Managers de sistemas core)
│   ├── ClaimManager, ClaimCommand, SubPlot, Claim
│   ├── PlayerDataManager, PlayerData
│   ├── MarketManager, EndResetManager
│   ├── SelectionManager, InputManager, PermissionManager
│   ├── AchievementManager, AuditManager
│   ├── BattlePassManager, BountyManager
│   ├── ChatManager, CombatTagManager
│   ├── CodexManager, ConfigMigrator
│   ├── CosmeticManager, CrateManager
│   ├── CustomItemManager, DailyRewardManager
│   ├── DuelManager, IgnoreManager
│   ├── KitManager, LeaderboardManager
│   ├── MailManager, MenuManager
│   ├── NationManager, QuestManager
│   ├── RankManager, ReputationManager
│   ├── RtpManager, SeasonalEventManager
│   ├── StructureManager, TutorialManager, VipManager
│
├── migration/                   (Migração de config)
│   └── ConfigMigrator.java
│
├── towns/                       (Sistema de Reinos)
│   ├── listeners/, managers/, menus/, tasks/
│
└── utils/                       (Utilitários)
    ├── GorvaxExpansion.java      (PlaceholderAPI — 50+ placeholders)
    └── WorldGuardHook.java      (integração WorldGuard)
```

## Diagrama de Dependências

```
GorvaxCore.java (Main Class)
│
├── Dependências Externas
│   ├── Vault (Economy) ──────────── obrigatório
│   ├── WorldGuard ───────────────── obrigatório
│   ├── PlaceholderAPI ───────────── obrigatório
│   ├── LuckPerms ────────────────── opcional
│   ├── Floodgate ────────────────── opcional
│   ├── bStats (3.0.2) ──────────── embarcado
│   ├── HikariCP (5.1.0) ────────── embarcado (MySQL)
│   └── AnvilGUI ─────────────────── embarcado (Shadow)
│
├── Storage Backends
│   ├── YAML ──── legado, portátil
│   ├── SQLite ── padrão (sem setup)
│   └── MySQL ─── multi-servidor (HikariCP)
│
└── 30+ Managers (inicializados em onEnable)
    ├── Core: ClaimManager, KingdomManager, MarketManager
    ├── Boss: BossManager, LootManager, BossScheduleManager, BossRaidManager
    ├── Player: PlayerDataManager, AchievementManager, RankManager
    ├── Social: MailManager, BountyManager, NationManager, DuelManager
    ├── Economy: VipManager, CrateManager, BattlePassManager
    ├── Content: QuestManager, SeasonalEventManager, CodexManager
    ├── Combat: CombatTagManager, IgnoreManager
    ├── UI: MenuManager, TutorialManager, CosmeticManager
    ├── Infra: ConfigMigrator, AuditManager, LeaderboardManager
    └── Utils: SelectionManager, InputManager, RtpManager
```

## Fluxo de Dados (Persistência)

```
Main Thread                     Async Thread
    │                               │
    ├─ snapshot() ──────────────────>│
    │  (copia dados para Map)       ├─ saveToBackend()
    │                               │  (YAML/SQLite/MySQL)
    │<──────────────── done ────────┤
    │                               │
```

**Regra**: Nunca fazer I/O de disco na Main Thread. Sempre usar snapshot + async save.

## Fluxo de Proteção de Claims

```
Evento (BlockBreak, Place, etc.)
    │
    ├─ getClaimAt(location)  ← Cache por chunk (O(1))
    │
    ├─ Se não há claim → permite
    │
    ├─ Se há claim:
    │   ├─ getSubPlotAt(location)
    │   │   ├─ Se há subplot → verifica permissão do subplot
    │   │   └─ Se não há → verifica permissão do claim
    │   │
    │   ├─ hasPermission(player, trustType)
    │   │   ├─ É owner? → permite
    │   │   ├─ É vice? → permite
    │   │   ├─ Tem trust adequado? → permite
    │   │   └─ Não → bloqueia + mensagem
    │   │
    │   └─ Verifica Kingdom scope (membros do reino)
    │
    └─ Retorna resultado
```

## Sistema de Bosses — Ciclo de Vida

```
BossManager.startTasks()
    │
    ├─ Timer (3-7h) ──> spawnRandomBoss()
    │                       │
    │                       ├─ Escolhe boss disponível
    │                       ├─ Escolhe localização (jogador online aleatório + offset)
    │                       ├─ Cria instância WorldBoss
    │                       ├─ Inicia BossTask (tick a cada 1s)
    │                       └─ Anuncia no chat + BossBar
    │
    ├─ BossTask.run() (cada 1s)
    │   ├─ Avalia threats (DPS, distância, healer, low HP)
    │   ├─ Escolhe target
    │   ├─ Executa IA (mover, atacar, anti-kite)
    │   ├─ Verifica fases (50%, 25%)
    │   └─ Atualiza BossBar
    │
    └─ Boss morre
        ├─ Calcula ranking de dano
        ├─ Distribui loot (top 5 + participação)
        ├─ Spawna baú com holograma
        ├─ Limpa entidade e efeitos
        └─ Anuncia vencedores
```

## Decisões Técnicas

| Decisão | Motivo |
|---|---|
| 3 Backends (YAML/SQLite/MySQL) | Flexibilidade: portabilidade local → escala multi-servidor |
| HikariCP | Connection pooling eficiente para MySQL em produção |
| ConcurrentHashMap | Thread-safety para dados acessados por Main + Async threads |
| Cache por chunks | O(1) lookup de claims em eventos frequentes (break/place) |
| EnumSet para Materials | Verificação O(1) para containers e interactables |
| AnvilGUI com fallback chat | Bedrock não suporta AnvilGUI nativamente |
| Shadow relocation | Evita conflito de versões com outros plugins |
| WorldBoss abstrata | Cada boss pode ter mecânicas únicas sem alterar a base |
| Adventure API | Componentes ricos de texto, MiniMessage para formatação |
| bStats embarcado | Métricas anônimas sem dependência externa |
| Config Migration | Atualização automática de configs entre versões |
| Custom Events | Extensibilidade para integrações futuras (14 eventos) |
