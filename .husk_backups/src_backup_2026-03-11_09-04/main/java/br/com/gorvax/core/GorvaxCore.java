package br.com.gorvax.core;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import br.com.gorvax.core.boss.commands.BossCommand;
import br.com.gorvax.core.boss.listeners.BossListener;
import br.com.gorvax.core.boss.managers.BossManager;
import br.com.gorvax.core.commands.GorvaxCommand;
import br.com.gorvax.core.commands.ChatCommand;
import br.com.gorvax.core.commands.KingdomChatCommand;
import br.com.gorvax.core.listeners.ChatListener;
import br.com.gorvax.core.listeners.BountyListener;
import br.com.gorvax.core.listeners.CombatListener;
import br.com.gorvax.core.listeners.MailListener;
import br.com.gorvax.core.listeners.MarketListener;
import br.com.gorvax.core.listeners.ProtectionListener;
import br.com.gorvax.core.listeners.ScoreboardListener;
import br.com.gorvax.core.listeners.WarListener;
import br.com.gorvax.core.listeners.WelcomeListener; // B4
import br.com.gorvax.core.listeners.StatisticsListener;
import br.com.gorvax.core.listeners.AchievementListener;
import br.com.gorvax.core.listeners.VisualizationListener;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.ConfirmCommand;
import br.com.gorvax.core.managers.KingdomCommand;
import br.com.gorvax.core.managers.PlotCommand;
import br.com.gorvax.core.managers.RenameCommand;
import br.com.gorvax.core.managers.TrustCommand;
import br.com.gorvax.core.managers.ClaimManager;
import br.com.gorvax.core.managers.EndResetManager;
import br.com.gorvax.core.managers.InputManager;
import br.com.gorvax.core.managers.BedrockFormManager;
import br.com.gorvax.core.managers.AuditManager;
import br.com.gorvax.core.managers.AchievementManager;
import br.com.gorvax.core.managers.AuctionManager;
import br.com.gorvax.core.storage.DatabaseManager;
import br.com.gorvax.core.managers.AuctionCommand;
import br.com.gorvax.core.managers.BountyManager;
import br.com.gorvax.core.managers.BountyCommand;
import br.com.gorvax.core.managers.CombatManager;
import br.com.gorvax.core.managers.DuelManager; // B3
import br.com.gorvax.core.managers.DuelCommand; // B3
import br.com.gorvax.core.managers.TutorialManager; // B4
import br.com.gorvax.core.managers.DailyRewardManager; // B5
import br.com.gorvax.core.managers.DailyRewardGUI; // B5
import br.com.gorvax.core.managers.MainMenuGUI; // B6
import br.com.gorvax.core.managers.LeaderboardManager; // B7
import br.com.gorvax.core.managers.TopCommand; // B7
import br.com.gorvax.core.managers.DiscordManager; // B8
import br.com.gorvax.core.managers.CustomItemManager; // B10
import br.com.gorvax.core.managers.CustomItemCommand; // B10
import br.com.gorvax.core.listeners.CustomItemListener; // B10
import br.com.gorvax.core.boss.miniboss.MiniBossManager; // B11
import br.com.gorvax.core.boss.miniboss.MiniBossListener; // B11
import br.com.gorvax.core.boss.miniboss.MiniBossCommand; // B11
import br.com.gorvax.core.managers.CrateManager; // B12
import br.com.gorvax.core.managers.CrateGUI; // B12
import br.com.gorvax.core.managers.CrateCommand; // B12
import br.com.gorvax.core.listeners.CrateListener; // B12
import br.com.gorvax.core.managers.CosmeticManager; // B13
import br.com.gorvax.core.commands.CosmeticCommand; // B13
import br.com.gorvax.core.listeners.CosmeticListener; // B13
import br.com.gorvax.core.managers.VipManager; // B14-VIP
import br.com.gorvax.core.managers.VipCommand; // B14-VIP
import br.com.gorvax.core.managers.BattlePassManager; // B15
import br.com.gorvax.core.managers.BattlePassGUI; // B15
import br.com.gorvax.core.commands.BattlePassCommand; // B15
import br.com.gorvax.core.listeners.BattlePassListener; // B15
import br.com.gorvax.core.managers.QuestManager; // B16
import br.com.gorvax.core.managers.QuestGUI; // B16
import br.com.gorvax.core.commands.QuestCommand; // B16
import br.com.gorvax.core.listeners.QuestListener; // B16
import br.com.gorvax.core.managers.SeasonalEventManager; // B17
import br.com.gorvax.core.commands.SeasonalEventCommand; // B17
import br.com.gorvax.core.listeners.SeasonalEventListener; // B17
import br.com.gorvax.core.managers.ReputationManager; // B18
import br.com.gorvax.core.commands.ReputationCommand; // B18
import br.com.gorvax.core.listeners.ReputationListener; // B18
import br.com.gorvax.core.managers.StructureManager; // B22
import br.com.gorvax.core.managers.StructureCommand; // B22
import br.com.gorvax.core.listeners.StructureListener; // B22
import br.com.gorvax.core.managers.LoreManager; // Lore
import br.com.gorvax.core.listeners.LoreListener; // Lore
import br.com.gorvax.core.managers.RankManager; // Ranks
import br.com.gorvax.core.commands.RankCommand; // Ranks
import br.com.gorvax.core.commands.KitCommand; // Ranks
import br.com.gorvax.core.commands.RtpCommand; // B25
import br.com.gorvax.core.listeners.RankListener; // Ranks
import br.com.gorvax.core.managers.CodexManager; // B28
import br.com.gorvax.core.managers.CodexGUI; // B28
import br.com.gorvax.core.commands.CodexCommand; // B28
import br.com.gorvax.core.listeners.CodexListener; // B28
import br.com.gorvax.core.managers.TeleportHubManager; // B37
import br.com.gorvax.core.managers.TeleportHubGUI; // B37
import br.com.gorvax.core.managers.TeleportHubKingdomsGUI; // B37
import br.com.gorvax.core.managers.TeleportHubStructuresGUI; // B37
import br.com.gorvax.core.listeners.TeleportHubListener; // B37
import br.com.gorvax.core.commands.TeleportHubCommand; // B37
import br.com.gorvax.core.managers.NationCommand; // B19
import br.com.gorvax.core.migration.ConfigMigrator; // B33
import br.com.gorvax.core.managers.MailManager;
import br.com.gorvax.core.managers.MailCommand;
import br.com.gorvax.core.managers.VoteManager;
import br.com.gorvax.core.managers.ChatManager;
import br.com.gorvax.core.managers.MessageManager;
import br.com.gorvax.core.managers.MarketCommand;
import br.com.gorvax.core.managers.MarketManager;
import br.com.gorvax.core.managers.PriceHistoryManager;
import br.com.gorvax.core.managers.PermissionManager;
import br.com.gorvax.core.managers.PlayerDataManager;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.ScoreboardManager;
import br.com.gorvax.core.managers.SelectionManager;
import br.com.gorvax.core.managers.WebMapManager;
import br.com.gorvax.core.towns.listeners.KingdomListener;
import br.com.gorvax.core.towns.listeners.KingdomSkillsListener;
import br.com.gorvax.core.towns.listeners.MenuListener;
import br.com.gorvax.core.towns.managers.KingdomManager;
import br.com.gorvax.core.towns.managers.WarManager;
import br.com.gorvax.core.towns.managers.NationManager; // B19
import br.com.gorvax.core.towns.menus.KingdomInventory;
import br.com.gorvax.core.towns.tasks.KingdomEffectsTask;
import br.com.gorvax.core.towns.tasks.KingdomMaintenanceTask;
import br.com.gorvax.core.utils.GorvaxExpansion;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GorvaxCore extends JavaPlugin {

    private static GorvaxCore instance;
    private static Economy econ = null;
    private LuckPerms luckPerms;

    // Campos de instância (B4.5 — movidos para o topo da classe)
    private DatabaseManager databaseManager;
    private ClaimManager claimManager;
    private SelectionManager selectionManager;
    private PlayerDataManager playerDataManager;
    private KingdomManager kingdomManager;
    private KingdomInventory kingdomInventory;
    private BossManager bossManager;
    private MarketManager marketManager;
    private InputManager inputManager;
    private EndResetManager endResetManager;
    private MessageManager messageManager;
    private BedrockFormManager bedrockFormManager;
    private ScoreboardManager scoreboardManager;
    private ChatManager chatManager;
    private AuditManager auditManager;
    private AchievementManager achievementManager;
    private AuctionManager auctionManager;
    private PriceHistoryManager priceHistoryManager;
    private WarManager warManager;
    private WebMapManager webMapManager;
    private MailManager mailManager;
    private VoteManager voteManager;
    private BountyManager bountyManager;
    private NationManager nationManager; // B19
    private CombatManager combatManager; // B2
    private DuelManager duelManager; // B3
    private TutorialManager tutorialManager; // B4
    private DailyRewardManager dailyRewardManager; // B5
    private MainMenuGUI mainMenuGUI; // B6
    private LeaderboardManager leaderboardManager; // B7
    private DiscordManager discordManager; // B8
    private CustomItemManager customItemManager; // B10
    private CustomItemListener customItemListener; // B10 — partículas de itens
    private MiniBossManager miniBossManager; // B11
    private CrateManager crateManager; // B12
    private CrateGUI crateGUI; // B12
    private CosmeticManager cosmeticManager; // B13
    private VipManager vipManager; // B14-VIP
    private BattlePassManager battlePassManager; // B15
    private BattlePassGUI battlePassGUI; // B15
    private QuestManager questManager; // B16
    private QuestGUI questGUI; // B16
    private SeasonalEventManager seasonalEventManager; // B17
    private ReputationManager reputationManager; // B18
    private StructureManager structureManager; // B22
    private LoreManager loreManager; // Lore
    private RankManager rankManager; // Ranks
    private CodexManager codexManager; // B28
    private CodexGUI codexGUI; // B28
    private TeleportHubManager teleportHubManager; // B37
    private TeleportHubGUI teleportHubGUI; // B37

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("=========================================");
        getLogger().info(" GORVAX PLUGIN v1.0.0 ");
        getLogger().info("=========================================");
        getLogger().info("[GorvaxCore] Inicializando sistema v" + getPluginMeta().getVersion() + "...");

        // 1. Configurar
        saveDefaultConfig();

        // B33 — Migração automática de configurações (antes dos managers)
        ConfigMigrator configMigrator = new ConfigMigrator(this);
        configMigrator.migrate();
        configMigrator.migrateMessages();

        if (setupEconomy()) {
            getLogger().info("[GorvaxCore] Vault e economia integrados com sucesso!");
        } else {
            getLogger().severe("[GorvaxCore] Vault não encontrado ou economia não configurada! Desabilitando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (setupLuckPerms()) {
            getLogger().info("[GorvaxCore] LuckPerms integrado com sucesso!");
        } else {
            getLogger().warning("[GorvaxCore] LuckPerms não encontrado. Algumas funções de cargo podem falhar.");
        }

        // B18 — Inicializar Storage (antes dos managers para que o DataStore esteja
        // pronto)
        this.databaseManager = new DatabaseManager(this);
        databaseManager.init();

        // 2. Inicializar Managers
        this.messageManager = new MessageManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.claimManager = new ClaimManager(this);
        this.selectionManager = new SelectionManager(this);
        this.kingdomManager = new KingdomManager(this);
        this.kingdomInventory = new KingdomInventory(this);
        this.bossManager = new BossManager(this);
        getServer().getPluginManager().registerEvents(new BossListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        new VisualizationListener(this);
        getServer().getPluginManager().registerEvents(new KingdomListener(this), this);
        new KingdomSkillsListener(this); // Auto-registra via construtor
        this.marketManager = new MarketManager(this);
        this.inputManager = new InputManager(this);
        getServer().getPluginManager().registerEvents(new MarketListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new StatisticsListener(this), this);

        // B6 — Scoreboard Dinâmico
        this.scoreboardManager = new ScoreboardManager(this);
        getServer().getPluginManager().registerEvents(new ScoreboardListener(this), this);

        // B8 — Chat Expandido
        this.chatManager = new ChatManager(this);

        // B10 — Log de Auditoria
        this.auditManager = new AuditManager(this);

        // B12 — Conquistas e Títulos
        this.achievementManager = new AchievementManager(this);

        // B14 — Leilão e Histórico de Preços
        this.auctionManager = new AuctionManager(this);
        this.priceHistoryManager = new PriceHistoryManager(this);
        getServer().getPluginManager().registerEvents(new AchievementListener(this), this);

        // B15 — Sistema de Guerra
        this.warManager = new WarManager(this);
        getServer().getPluginManager().registerEvents(new WarListener(this), this);

        // B4.1 — Bedrock Forms (soft-dependency do Floodgate)
        this.bedrockFormManager = new BedrockFormManager(this);
        if (bedrockFormManager.isAvailable()) {
            getLogger().info("[GorvaxCore] Floodgate detectado! Forms nativos para Bedrock ativados.");
        } else {
            getLogger().info("[GorvaxCore] Floodgate não encontrado. Usando fallback chat/GUI para Bedrock.");
        }

        // B16 — Integração com Mapa Web (Dynmap/BlueMap)
        this.webMapManager = new WebMapManager(this);

        // B17 — Features Sociais (Correio, Votação, Bounties)
        this.mailManager = new MailManager(this);
        this.voteManager = new VoteManager(this);
        this.bountyManager = new BountyManager(this);
        getServer().getPluginManager().registerEvents(new MailListener(this), this);
        getServer().getPluginManager().registerEvents(new BountyListener(this), this);

        // B19 — Sistema de Nações
        this.nationManager = new NationManager(this);

        // B2 — Sistema de Combate
        this.combatManager = new CombatManager(this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        // B3 — Sistema de Duelos
        this.duelManager = new DuelManager(this);
        getServer().getPluginManager().registerEvents(new br.com.gorvax.core.listeners.DuelListener(this), this);

        // B4 — Tutorial Interativo + Welcome Kit
        this.tutorialManager = new TutorialManager(this);
        getServer().getPluginManager().registerEvents(new WelcomeListener(this), this);

        // B5 — Daily Rewards & Login Streak
        this.dailyRewardManager = new DailyRewardManager(this);
        DailyRewardGUI dailyRewardGUI = new DailyRewardGUI(this, dailyRewardManager);
        getServer().getPluginManager().registerEvents(dailyRewardManager, this);
        getServer().getPluginManager().registerEvents(dailyRewardGUI, this);

        // B6 — Menu Central GUI
        this.mainMenuGUI = new MainMenuGUI(this);

        // B7 — Leaderboards & Rankings
        this.leaderboardManager = new LeaderboardManager(this);
        getServer().getPluginManager().registerEvents(mainMenuGUI, this);

        // B10 — Custom Items (Armas e Armaduras Lendárias)
        this.customItemManager = new CustomItemManager(this);
        this.customItemListener = new CustomItemListener(this);
        getServer().getPluginManager().registerEvents(customItemListener, this);

        // B11 — Mini-Bosses por Bioma
        this.miniBossManager = new MiniBossManager(this);
        getServer().getPluginManager().registerEvents(new MiniBossListener(this), this);

        // B8 — Integração Discord (Webhook)
        this.discordManager = new DiscordManager(this);
        discordManager.init();

        // B12 — Crates / Keys
        this.crateManager = new CrateManager(this);
        this.crateGUI = new CrateGUI(this);
        getServer().getPluginManager().registerEvents(crateGUI, this);
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);

        // B13 — Cosméticos (Partículas, Trails, Tags, Kill Effects)
        this.cosmeticManager = new CosmeticManager(this);
        CosmeticCommand cosmeticCmd = new CosmeticCommand(this);
        getServer().getPluginManager().registerEvents(new CosmeticListener(this), this);
        getServer().getPluginManager().registerEvents(cosmeticCmd, this);

        // B14-VIP — Sistema de VIP & Ranks Premium
        this.vipManager = new VipManager(this);

        // B15 — Battle Pass Sazonal
        this.battlePassManager = new BattlePassManager(this);
        this.battlePassGUI = new BattlePassGUI(this);
        getServer().getPluginManager().registerEvents(battlePassGUI, this);
        getServer().getPluginManager().registerEvents(new BattlePassListener(this), this);

        // B16 — Quests Diárias e Semanais
        this.questManager = new QuestManager(this);
        this.questGUI = new QuestGUI(this);
        getServer().getPluginManager().registerEvents(questGUI, this);
        getServer().getPluginManager().registerEvents(new QuestListener(this), this);

        // B17 — Eventos Sazonais Expandidos
        this.seasonalEventManager = new SeasonalEventManager(this);
        getServer().getPluginManager().registerEvents(new SeasonalEventListener(this), this);

        // B18 — Sistema de Reputação / Karma
        this.reputationManager = new ReputationManager(this);
        getServer().getPluginManager().registerEvents(new ReputationListener(this), this);

        // B22 — Sistema de Estruturas (Reinos pré-construídos)
        this.structureManager = new StructureManager(this);
        getServer().getPluginManager().registerEvents(new StructureListener(this), this);

        // Lore — Sistema de Livros e Totems
        this.loreManager = new LoreManager(this);
        getServer().getPluginManager().registerEvents(new LoreListener(this), this);

        // B37 — Hub de Teleportes & Bússola
        this.teleportHubManager = new TeleportHubManager(this);
        this.teleportHubGUI = new TeleportHubGUI(this);
        TeleportHubKingdomsGUI teleportHubKingdomsGUI = new TeleportHubKingdomsGUI(this);
        TeleportHubStructuresGUI teleportHubStructuresGUI = new TeleportHubStructuresGUI(this);
        getServer().getPluginManager().registerEvents(teleportHubGUI, this);
        getServer().getPluginManager().registerEvents(teleportHubKingdomsGUI, this);
        getServer().getPluginManager().registerEvents(teleportHubStructuresGUI, this);
        getServer().getPluginManager().registerEvents(new TeleportHubListener(this), this);

        // 2.2 Iniciar Tarefas
        new KingdomEffectsTask(this).runTaskTimer(this, 20L, 40L);
        new KingdomMaintenanceTask(this).runTaskTimer(this, 6000L, 72000L); // 5min delay, 1h interval

        // 2.1 Configurar Permissões Automáticas
        if (this.luckPerms != null) {
            new PermissionManager(this).setupGroups();
        }

        this.endResetManager = new EndResetManager(this);
        this.endResetManager.startScheduler();

        // 3. Registrar Comandos
        // B9 — Subcomandos extraídos do antigo ClaimCommand
        TrustCommand trustCmd = new TrustCommand(this);
        RenameCommand renameCmd = new RenameCommand(this);
        KingdomCommand kingdomCmd = new KingdomCommand(this, renameCmd);
        PlotCommand plotCmd = new PlotCommand(this, trustCmd);
        ConfirmCommand confirmCmd = new ConfirmCommand(this);

        getCommand("reino").setExecutor(kingdomCmd);
        getCommand("reino").setTabCompleter(kingdomCmd);
        getCommand("lote").setExecutor(plotCmd);
        getCommand("lote").setTabCompleter(plotCmd);
        getCommand("reinonome").setExecutor(renameCmd);
        getCommand("reinonome").setTabCompleter(renameCmd);
        getCommand("confirmar").setExecutor(confirmCmd);
        getCommand("confirmar").setTabCompleter(confirmCmd);
        getCommand("permitir").setExecutor(trustCmd);
        getCommand("permitir").setTabCompleter(trustCmd);
        getCommand("remover").setExecutor(trustCmd);
        getCommand("remover").setTabCompleter(trustCmd);

        BossCommand bossCmd = new BossCommand(this);
        getCommand("boss").setExecutor(bossCmd);
        getCommand("boss").setTabCompleter(bossCmd);

        MarketCommand marketCmd = new MarketCommand(this);
        getCommand("market").setExecutor(marketCmd);
        getCommand("market").setTabCompleter(marketCmd);

        // B14 — Comando de leilão
        AuctionCommand auctionCmd = new AuctionCommand(this);
        if (getCommand("leilao") != null) {
            getCommand("leilao").setExecutor(auctionCmd);
            getCommand("leilao").setTabCompleter(auctionCmd);
        }

        GorvaxCommand gorvaxCmd = new GorvaxCommand(this);
        getCommand("gorvax").setExecutor(gorvaxCmd);
        getCommand("gorvax").setTabCompleter(gorvaxCmd);

        // B8 — Chat Expandido (comandos /chat, /g, /l, /tc, /ac, /rc)
        ChatCommand chatCmd = new ChatCommand(this);
        ChatListener chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        if (getCommand("rc") != null) {
            getCommand("rc").setExecutor(chatCmd);
        }
        if (getCommand("ac") != null) {
            getCommand("ac").setExecutor(chatCmd);
        }
        if (getCommand("g") != null) {
            getCommand("g").setExecutor(chatCmd);
        }
        if (getCommand("l") != null) {
            getCommand("l").setExecutor(chatCmd);
        }
        if (getCommand("tc") != null) {
            getCommand("tc").setExecutor(chatCmd);
        }
        if (getCommand("chat") != null) {
            getCommand("chat").setExecutor(chatCmd);
            getCommand("chat").setTabCompleter(chatCmd);
        }
        // B19 — /nc (chat de nação)
        if (getCommand("nc") != null) {
            getCommand("nc").setExecutor(chatCmd);
        }
        // B9 — /ignore (ignorar jogadores no chat)
        if (getCommand("ignore") != null) {
            getCommand("ignore").setExecutor(chatCmd);
            getCommand("ignore").setTabCompleter(chatCmd);
        }

        // B12 — Comandos de conquistas e títulos
        if (getCommand("conquistas") != null) {
            getCommand("conquistas").setExecutor((sender, cmd, label, args) -> {
                if (sender instanceof Player p) {
                    achievementManager.openAchievementMenu(p);
                } else {
                    messageManager.send(sender, "general.player_only");
                }
                return true;
            });
        }
        if (getCommand("titulos") != null) {
            getCommand("titulos").setExecutor((sender, cmd, label, args) -> {
                if (sender instanceof Player p) {
                    achievementManager.openTitleMenu(p);
                } else {
                    messageManager.send(sender, "general.player_only");
                }
                return true;
            });
        }

        // B17 — Comandos de correio e bounties
        MailCommand mailCmd = new MailCommand(this);
        if (getCommand("carta") != null) {
            getCommand("carta").setExecutor(mailCmd);
            getCommand("carta").setTabCompleter(mailCmd);
        }

        BountyCommand bountyCmd = new BountyCommand(this);
        if (getCommand("bounty") != null) {
            getCommand("bounty").setExecutor(bountyCmd);
            getCommand("bounty").setTabCompleter(bountyCmd);
        }

        // B19 — Comando da Nação
        NationCommand nationCmd = new NationCommand(this);
        if (getCommand("nacao") != null) {
            getCommand("nacao").setExecutor(nationCmd);
            getCommand("nacao").setTabCompleter(nationCmd);
        }

        // B3 — Comando de Duelo
        DuelCommand duelCmd = new DuelCommand(this);
        if (getCommand("duel") != null) {
            getCommand("duel").setExecutor(duelCmd);
            getCommand("duel").setTabCompleter(duelCmd);
        }

        // B4 — Comando de Tutorial
        if (getCommand("tutorial") != null) {
            getCommand("tutorial").setExecutor((sender, cmd, label, args) -> {
                if (!(sender instanceof Player p)) {
                    messageManager.send(sender, "general.player_only");
                    return true;
                }
                if (args.length > 0 && args[0].equalsIgnoreCase("pular")) {
                    if (tutorialManager.isInTutorial(p)) {
                        tutorialManager.skipTutorial(p);
                    } else {
                        messageManager.send(p, "tutorial.not_in_tutorial");
                    }
                } else {
                    if (tutorialManager.isInTutorial(p)) {
                        tutorialManager.showStep(p, tutorialManager.getCurrentStep(p));
                    } else {
                        messageManager.send(p, "tutorial.not_in_tutorial");
                    }
                }
                return true;
            });
        }

        // B5 — Comando de Daily Rewards
        if (getCommand("daily") != null) {
            getCommand("daily").setExecutor(dailyRewardManager);
        }

        // B6 — Comando /menu
        if (getCommand("menu") != null) {
            getCommand("menu").setExecutor((sender, cmd, label, args) -> {
                if (!(sender instanceof Player p)) {
                    messageManager.send(sender, "general.player_only");
                    return true;
                }
                mainMenuGUI.open(p);
                return true;
            });
        }

        // B7 — Comando /top (Rankings)
        TopCommand topCmd = new TopCommand(this);
        if (getCommand("top") != null) {
            getCommand("top").setExecutor(topCmd);
            getCommand("top").setTabCompleter(topCmd);
        }

        // B10 — Comando /customitem
        CustomItemCommand customItemCmd = new CustomItemCommand(this);
        if (getCommand("customitem") != null) {
            getCommand("customitem").setExecutor(customItemCmd);
            getCommand("customitem").setTabCompleter(customItemCmd);
        }

        // B11 — Comando /miniboss
        MiniBossCommand miniBossCmd = new MiniBossCommand(this);
        if (getCommand("miniboss") != null) {
            getCommand("miniboss").setExecutor(miniBossCmd);
            getCommand("miniboss").setTabCompleter(miniBossCmd);
        }

        // B12 — Comando /crate
        CrateCommand crateCmd = new CrateCommand(this);
        if (getCommand("crate") != null) {
            getCommand("crate").setExecutor(crateCmd);
            getCommand("crate").setTabCompleter(crateCmd);
        }

        // B13 — Comando /cosmetics
        if (getCommand("cosmetics") != null) {
            getCommand("cosmetics").setExecutor(cosmeticCmd);
            getCommand("cosmetics").setTabCompleter(cosmeticCmd);
        }

        // B14-VIP — Comando /vip
        VipCommand vipCmd = new VipCommand(this);
        if (getCommand("vip") != null) {
            getCommand("vip").setExecutor(vipCmd);
            getCommand("vip").setTabCompleter(vipCmd);
        }

        // B15 — Comando /pass
        BattlePassCommand passCmd = new BattlePassCommand(this);
        if (getCommand("pass") != null) {
            getCommand("pass").setExecutor(passCmd);
            getCommand("pass").setTabCompleter(passCmd);
        }

        // B16 — Comando /quests
        QuestCommand questCmd = new QuestCommand(this, questGUI);
        if (getCommand("quests") != null) {
            getCommand("quests").setExecutor(questCmd);
            getCommand("quests").setTabCompleter(questCmd);
        }

        // B17 — Comando /evento
        SeasonalEventCommand eventoCmd = new SeasonalEventCommand(this);
        if (getCommand("evento") != null) {
            getCommand("evento").setExecutor(eventoCmd);
            getCommand("evento").setTabCompleter(eventoCmd);
        }

        // B18 — Comando /karma
        ReputationCommand karmaCmd = new ReputationCommand(this);
        if (getCommand("karma") != null) {
            getCommand("karma").setExecutor(karmaCmd);
            getCommand("karma").setTabCompleter(karmaCmd);
        }

        // B22 — Comando /estrutura
        StructureCommand structureCmd = new StructureCommand(this);
        if (getCommand("estrutura") != null) {
            getCommand("estrutura").setExecutor(structureCmd);
            getCommand("estrutura").setTabCompleter(structureCmd);
        }

        // Ranks — Sistema de progressão de ranks gratuitos
        rankManager = new RankManager(this);
        RankCommand rankCmd = new RankCommand(this);
        if (getCommand("rank") != null) {
            getCommand("rank").setExecutor(rankCmd);
            getCommand("rank").setTabCompleter(rankCmd);
        }
        KitCommand kitCmd = new KitCommand(this);
        if (getCommand("kit") != null) {
            getCommand("kit").setExecutor(kitCmd);
        }

        // B25 — Comando /rtp nativo
        RtpCommand rtpCmd = new RtpCommand(this);
        if (getCommand("rtp") != null) {
            getCommand("rtp").setExecutor(rtpCmd);
            getCommand("rtp").setTabCompleter(rtpCmd);
        }

        getServer().getPluginManager().registerEvents(new RankListener(this), this);

        // B28 — Códex de Gorvax
        codexManager = new CodexManager(this);
        codexGUI = new CodexGUI(this);
        getServer().getPluginManager().registerEvents(codexGUI, this);
        getServer().getPluginManager().registerEvents(new CodexListener(this), this);
        CodexCommand codexCmd = new CodexCommand(this);
        if (getCommand("codex") != null) {
            getCommand("codex").setExecutor(codexCmd);
            getCommand("codex").setTabCompleter(codexCmd);
        }

        // B37 — Comando /tp-hub
        TeleportHubCommand tpHubCmd = new TeleportHubCommand(this);
        if (getCommand("tp-hub") != null) {
            getCommand("tp-hub").setExecutor(tpHubCmd);
            getCommand("tp-hub").setTabCompleter(tpHubCmd);
        }

        // Task periódica de verificação de rank (a cada 5 min)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (rankManager != null && rankManager.isEnabled()) {
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    rankManager.checkAndPromote(p);
                }
            }
        }, 6000L, 6000L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GorvaxExpansion(this).register();
        }

        // B32 — Métricas bStats (analytics anônimas de uso)
        setupBStats();

        getLogger().info("[GorvaxCore] Plugin carregado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) {
            bossManager.removeAllBosses();
        }
        if (claimManager != null) {
            claimManager.saveSync();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        if (kingdomManager != null) {
            kingdomManager.saveSync();
        }
        if (marketManager != null) {
            marketManager.saveMarketData();
            marketManager.saveLocalMarketSync();
        }
        if (bossManager != null && bossManager.getLootManager() != null) {
            bossManager.getLootManager().saveLoot();
        }
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }
        if (auditManager != null) {
            auditManager.saveSync();
        }
        if (auctionManager != null) {
            auctionManager.saveSync();
        }
        if (priceHistoryManager != null) {
            priceHistoryManager.saveSync();
        }
        // B15 — WarManager persiste via KingdomManager (towns.yml), já salvo acima

        // B16 — WebMapManager cleanup
        if (webMapManager != null) {
            webMapManager.shutdown();
        }

        // B17 — Features Sociais
        if (mailManager != null) {
            mailManager.saveSync();
        }
        if (voteManager != null) {
            voteManager.saveSync();
        }
        if (bountyManager != null) {
            bountyManager.saveSync();
        }

        // B19 — Nações
        if (nationManager != null) {
            nationManager.saveSync();
        }

        // B2 — Sistema de Combate (limpar NPCs de logger)
        if (combatManager != null) {
            combatManager.cleanup();
        }

        // B3 — Sistema de Duelos (devolver apostas de duelos ativos)
        if (duelManager != null) {
            duelManager.cleanup();
        }

        // B4 — Tutorial (remover BossBars)
        if (tutorialManager != null) {
            tutorialManager.cleanup();
        }

        // B11 — Mini-Bosses
        if (miniBossManager != null) {
            miniBossManager.cleanup();
        }

        // B13 — Cosméticos (cancela task de partículas)
        if (cosmeticManager != null) {
            cosmeticManager.shutdown();
        }

        // B10 — Custom Item Particles (cancela task de partículas de itens)
        if (customItemListener != null) {
            customItemListener.shutdown();
        }

        // B22 — Estruturas
        if (structureManager != null) {
            structureManager.saveSync();
        }

        // B17 — Eventos Sazonais
        if (seasonalEventManager != null) {
            seasonalEventManager.shutdown();
        }

        // B18 — Shutdown do Storage (depois que todos os managers salvaram)
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        // B4.1 — Limpar campo static para evitar vazamento em reload
        econ = null;
        instance = null;
        getLogger().info("[GorvaxCore] Plugin desabilitado com sucesso!");
    }

    public static Economy getEconomy() {
        return econ;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager()
                .getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            return true;
        }
        return false;
    }

    // B32 — Métricas bStats (analytics anônimas de uso do plugin)
    private void setupBStats() {
        // NOTA: Substitua 99999 pelo pluginId real após registrar em
        // https://bstats.org/
        int pluginId = 99999;
        Metrics metrics = new Metrics(this, pluginId);

        // 1. Versão do servidor
        metrics.addCustomChart(new SimplePie("server_version", () -> Bukkit.getVersion()));

        // 2. Bedrock habilitado (Floodgate/Geyser)
        metrics.addCustomChart(new SimplePie("bedrock_enabled",
                () -> bedrockFormManager != null && bedrockFormManager.isAvailable() ? "Sim" : "Não"));

        // 3. Plugin de economia (provider do Vault)
        metrics.addCustomChart(new SimplePie("economy_plugin", () -> {
            if (econ == null)
                return "Nenhum";
            return econ.getName();
        }));

        // 4. Total de jogadores registrados
        metrics.addCustomChart(new SingleLineChart("total_players", () -> {
            if (playerDataManager == null)
                return 0;
            var cfg = playerDataManager.getDataConfig();
            return cfg != null ? cfg.getKeys(false).size() : 0;
        }));

        // 5. Total de reinos
        metrics.addCustomChart(new SingleLineChart("total_kingdoms", () -> {
            if (claimManager == null)
                return 0;
            return (int) claimManager.getClaims().stream().filter(Claim::isKingdom).count();
        }));

        // 6. Total de claims
        metrics.addCustomChart(new SingleLineChart("total_claims", () -> {
            if (claimManager == null)
                return 0;
            return claimManager.getClaims().size();
        }));

        // 7. Bosses ativos no momento
        metrics.addCustomChart(new SingleLineChart("active_bosses", () -> {
            if (bossManager == null)
                return 0;
            return bossManager.getActiveBosses().size();
        }));

        // 8. Temporada de Battle Pass ativa
        metrics.addCustomChart(new SimplePie("battlepass_season", () -> {
            if (battlePassManager == null)
                return "Desabilitado";
            return battlePassManager.isSeasonActive() ? "Ativa" : "Inativa";
        }));

        // 9. Features habilitadas (Advanced Pie)
        metrics.addCustomChart(new AdvancedPie("features_enabled", () -> {
            Map<String, Integer> features = new HashMap<>();
            if (dailyRewardManager != null)
                features.put("Daily Rewards", 1);
            if (crateManager != null)
                features.put("Crates", 1);
            if (cosmeticManager != null)
                features.put("Cosméticos", 1);
            if (questManager != null)
                features.put("Quests", 1);
            if (codexManager != null)
                features.put("Códex", 1);
            if (battlePassManager != null)
                features.put("Battle Pass", 1);
            if (seasonalEventManager != null)
                features.put("Eventos Sazonais", 1);
            if (reputationManager != null)
                features.put("Reputação", 1);
            if (duelManager != null)
                features.put("Duelos", 1);
            if (loreManager != null)
                features.put("Lore", 1);
            return features;
        }));

        // 10. Distribuição de bosses (Drilldown Pie)
        metrics.addCustomChart(new DrilldownPie("boss_types", () -> {
            Map<String, Map<String, Integer>> drilldown = new HashMap<>();
            Map<String, Integer> worldBosses = new HashMap<>();
            if (bossManager != null) {
                worldBosses.put("Ativos", bossManager.getActiveBosses().size());
            }
            drilldown.put("World Bosses", worldBosses);

            Map<String, Integer> miniBosses = new HashMap<>();
            if (miniBossManager != null) {
                miniBosses.put("Ativos", miniBossManager.getActiveMiniBosses().size());
            }
            drilldown.put("Mini-Bosses", miniBosses);
            return drilldown;
        }));

        getLogger().info("[GorvaxCore] bStats métricas ativadas.");
    }

    // B4.4 — Removido nameCache (Paper já cacheia internamente via OfflinePlayer)
    public String getPlayerName(UUID uuid) {
        if (uuid == null)
            return "Ninguém";
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName();
        return name != null ? name : "Desconhecido";
    }

    public static GorvaxCore getInstance() {
        return instance;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public KingdomManager getKingdomManager() {
        return kingdomManager;
    }

    public KingdomInventory getKingdomInventory() {
        return kingdomInventory;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public EndResetManager getEndResetManager() {
        return endResetManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public BedrockFormManager getBedrockFormManager() {
        return bedrockFormManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public AuditManager getAuditManager() {
        return auditManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public PriceHistoryManager getPriceHistoryManager() {
        return priceHistoryManager;
    }

    public WarManager getWarManager() {
        return warManager;
    }

    public WebMapManager getWebMapManager() {
        return webMapManager;
    }

    public MailManager getMailManager() {
        return mailManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    // B18 — Storage
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // B19 — Nações
    public NationManager getNationManager() {
        return nationManager;
    }

    // B2 — Sistema de Combate
    public CombatManager getCombatManager() {
        return combatManager;
    }

    // B3 — Sistema de Duelos
    public DuelManager getDuelManager() {
        return duelManager;
    }

    // B4 — Tutorial Interativo
    public TutorialManager getTutorialManager() {
        return tutorialManager;
    }

    // B5 — Daily Rewards
    public DailyRewardManager getDailyRewardManager() {
        return dailyRewardManager;
    }

    // B6 — Menu Central GUI
    public MainMenuGUI getMainMenuGUI() {
        return mainMenuGUI;
    }

    // B7 — Leaderboards & Rankings
    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    // B8 — Integração Discord (Webhook)
    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    // B10 — Custom Items
    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    // B11 — Mini-Bosses por Bioma
    public MiniBossManager getMiniBossManager() {
        return miniBossManager;
    }

    // B12 — Crates / Keys
    public CrateManager getCrateManager() {
        return crateManager;
    }

    public CrateGUI getCrateGUI() {
        return crateGUI;
    }

    // B13 — Cosméticos
    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    // B14-VIP — VipManager
    public VipManager getVipManager() {
        return vipManager;
    }

    // B15 — Battle Pass
    public BattlePassManager getBattlePassManager() {
        return battlePassManager;
    }

    public BattlePassGUI getBattlePassGUI() {
        return battlePassGUI;
    }

    // B16 — Quests
    public QuestManager getQuestManager() {
        return questManager;
    }

    public QuestGUI getQuestGUI() {
        return questGUI;
    }

    // B17 — Eventos Sazonais
    public SeasonalEventManager getSeasonalEventManager() {
        return seasonalEventManager;
    }

    // B18 — Reputação / Karma
    public ReputationManager getReputationManager() {
        return reputationManager;
    }

    // B22 — Estruturas
    public StructureManager getStructureManager() {
        return structureManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    // Lore — Livros e Totems
    public LoreManager getLoreManager() {
        return loreManager;
    }

    // B28 — Códex de Gorvax
    public CodexManager getCodexManager() {
        return codexManager;
    }

    public CodexGUI getCodexGUI() {
        return codexGUI;
    }

    // B37 — Hub de Teleportes
    public TeleportHubManager getTeleportHubManager() {
        return teleportHubManager;
    }

    public TeleportHubGUI getTeleportHubGUI() {
        return teleportHubGUI;
    }

    public void refreshPlayerName(Player p) {
        Claim kingdom = getKingdomManager().getKingdom(p.getUniqueId());
        String prefix = "";

        // B13 — Tag cosmética antes de tudo
        if (cosmeticManager != null) {
            String cosTag = cosmeticManager.getActiveChatTag(p);
            if (!cosTag.isEmpty()) {
                prefix += cosTag;
            }
        }

        // B12 — Título ativo
        PlayerData pd = getPlayerDataManager().getData(p.getUniqueId());
        String title = pd.getActiveTitle();
        if (title != null && !title.isEmpty()) {
            prefix += title + " ";
        }

        // Game Rank (Aventureiro, Explorador, Guerreiro, Lendário)
        if (rankManager != null && rankManager.isEnabled()) {
            RankManager.GameRank gameRank = rankManager.getPlayerRank(p);
            prefix += gameRank.getDisplayName() + " ";
        }

        if (kingdom != null && kingdom.getTag() != null && !kingdom.getTag().isEmpty()) {
            String tag = kingdom.getTag();
            String color = kingdom.getTagColor();
            prefix += String.format("§f[%s%s§f] §r", color, tag);
        }

        if (!prefix.isEmpty()) {
            p.displayName(LegacyComponentSerializer.legacySection().deserialize(prefix + p.getName()));
            p.playerListName(LegacyComponentSerializer.legacySection().deserialize(prefix + p.getName()));
        } else {
            p.displayName(net.kyori.adventure.text.Component.text(p.getName()));
            p.playerListName(net.kyori.adventure.text.Component.text(p.getName()));
        }
    }
}