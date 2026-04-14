package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.managers.BossManager;
import br.com.gorvax.core.managers.ChatManager.ChatChannel;
import br.com.gorvax.core.utils.MenuUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * B6/B38 — Menu Central GUI (54 slots / 6 linhas / 2 páginas).
 * Ponto de acesso unificado para todos os sistemas do GorvaxCore.
 * Acessível via /gorvax menu ou /menu.
 *
 * Página 1: Sistemas Principais (Reino, Mercado, Leilão, Bosses, Conquistas, Stats, Daily, Correio, Config, Rankings)
 * Página 2: Sistemas Adicionais (Duelos, Bounties, Cosméticos, Códex, Crates, BattlePass, Ranks, Karma, VIP, Quests, Nações, Títulos, Hub TP, Eventos, Chat)
 */
public class MainMenuGUI implements Listener {

    private final GorvaxCore plugin;

    public MainMenuGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre o menu central para o jogador (Página 1).
     * Se Bedrock, usa SimpleForm via BedrockFormManager.
     */
    public void open(Player player) {
        // Verificar se é Bedrock — usar SimpleForm
        BedrockFormManager bedrock = plugin.getBedrockFormManager();
        if (bedrock != null && bedrock.isAvailable() && InputManager.isBedrockPlayer(player)) {
            openBedrockForm(player);
            return;
        }

        // Java: abrir GUI inventário (Página 1)
        openPage1(player);
    }

    // ─── GUI Java: Página 1 — Sistemas Principais ──────────────────

    private void openPage1(Player player) {
        MessageManager msg = plugin.getMessageManager();
        String title = msg.get("main_menu.title");
        Inventory inv = Bukkit.createInventory(new Page1Holder(), 54,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // Preencher bordas com vidro preto
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // ---- Linha 2 (slots 10, 12, 14, 16) ----
        inv.setItem(10, createMenuItem(Material.GOLDEN_SWORD,
                msg.get("main_menu.kingdom_name"),
                msg.get("main_menu.kingdom_lore")));

        inv.setItem(12, createMenuItem(Material.EMERALD,
                msg.get("main_menu.market_name"),
                msg.get("main_menu.market_lore")));

        inv.setItem(14, createMenuItem(Material.GOLD_INGOT,
                msg.get("main_menu.auction_name"),
                msg.get("main_menu.auction_lore")));

        inv.setItem(16, createMenuItem(Material.WITHER_SKELETON_SKULL,
                msg.get("main_menu.bosses_name"),
                msg.get("main_menu.bosses_lore")));

        // ---- Linha 4 (slots 28, 30, 32, 34) ----
        inv.setItem(28, createMenuItem(Material.DIAMOND,
                msg.get("main_menu.achievements_name"),
                msg.get("main_menu.achievements_lore")));

        inv.setItem(30, createMenuItem(Material.BOOK,
                msg.get("main_menu.stats_name"),
                msg.get("main_menu.stats_lore")));

        inv.setItem(32, createMenuItem(Material.CHEST,
                msg.get("main_menu.daily_name"),
                msg.get("main_menu.daily_lore")));

        inv.setItem(34, createMenuItem(Material.FILLED_MAP,
                msg.get("main_menu.mail_name"),
                msg.get("main_menu.mail_lore")));

        // ---- Linha 6 (slots 45, 49, 53) ----
        inv.setItem(45, createMenuItem(Material.REDSTONE_TORCH,
                msg.get("main_menu.settings_name"),
                msg.get("main_menu.settings_lore")));

        inv.setItem(49, createMenuItem(Material.PAINTING,
                msg.get("main_menu.rankings_name"),
                msg.get("main_menu.rankings_lore")));

        // B38 — Botão "Próxima Página ▶"
        inv.setItem(53, createMenuItem(Material.ARROW,
                msg.get("main_menu.page_next_name"),
                msg.get("main_menu.page_next_lore")));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ─── GUI Java: Página 2 — Sistemas Adicionais ──────────────────

    private void openPage2(Player player) {
        MessageManager msg = plugin.getMessageManager();
        String title = msg.get("main_menu.title_page2");
        Inventory inv = Bukkit.createInventory(new Page2Holder(), 54,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // Preencher bordas com vidro preto
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // ---- Linha 2 (slots 10, 12, 14, 16) ----
        inv.setItem(10, createMenuItem(Material.IRON_SWORD,
                msg.get("main_menu.duel_name"),
                msg.get("main_menu.duel_lore")));

        inv.setItem(12, createMenuItem(Material.SKELETON_SKULL,
                msg.get("main_menu.bounty_name"),
                msg.get("main_menu.bounty_lore")));

        inv.setItem(14, createMenuItem(Material.ARMOR_STAND,
                msg.get("main_menu.cosmetics_name"),
                msg.get("main_menu.cosmetics_lore")));

        inv.setItem(16, createMenuItem(Material.ENCHANTED_BOOK,
                msg.get("main_menu.codex_name"),
                msg.get("main_menu.codex_lore")));

        // ---- Linha 4 (slots 28, 30, 32, 34) ----
        inv.setItem(28, createMenuItem(Material.ENDER_CHEST,
                msg.get("main_menu.crate_name"),
                msg.get("main_menu.crate_lore")));

        inv.setItem(30, createMenuItem(Material.NETHER_STAR,
                msg.get("main_menu.battlepass_name"),
                msg.get("main_menu.battlepass_lore")));

        inv.setItem(32, createMenuItem(Material.EXPERIENCE_BOTTLE,
                msg.get("main_menu.ranks_name"),
                msg.get("main_menu.ranks_lore")));

        inv.setItem(34, createMenuItem(Material.HEART_OF_THE_SEA,
                msg.get("main_menu.karma_name"),
                msg.get("main_menu.karma_lore")));

        // ---- Linha 6 (slots 45–53) ----
        // Slot 45: ◀ Página Anterior
        inv.setItem(45, createMenuItem(Material.ARROW,
                msg.get("main_menu.page_prev_name"),
                msg.get("main_menu.page_prev_lore")));

        // Slot 46: VIP
        inv.setItem(46, createMenuItem(Material.DIAMOND,
                msg.get("main_menu.vip_name"),
                msg.get("main_menu.vip_lore")));

        // Slot 47: Quests
        inv.setItem(47, createMenuItem(Material.WRITABLE_BOOK,
                msg.get("main_menu.quests_name"),
                msg.get("main_menu.quests_lore")));

        // Slot 48: Nações
        inv.setItem(48, createMenuItem(Material.ORANGE_BANNER,
                msg.get("main_menu.nation_name"),
                msg.get("main_menu.nation_lore")));

        // Slot 49: Títulos
        inv.setItem(49, createMenuItem(Material.NAME_TAG,
                msg.get("main_menu.titles_name"),
                msg.get("main_menu.titles_lore")));

        // Slot 50: Hub de Teleportes
        inv.setItem(50, createMenuItem(Material.COMPASS,
                msg.get("main_menu.hub_name"),
                msg.get("main_menu.hub_lore")));

        // Slot 51: Eventos Sazonais
        inv.setItem(51, createMenuItem(Material.JACK_O_LANTERN,
                msg.get("main_menu.events_name"),
                msg.get("main_menu.events_lore")));

        // Slot 53: Chat & Social
        inv.setItem(53, createMenuItem(Material.OAK_SIGN,
                msg.get("main_menu.chat_name"),
                msg.get("main_menu.chat_lore")));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f);
    }

    // ─── Listener de Cliques ────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        boolean isPage1 = event.getInventory().getHolder() instanceof Page1Holder;
        boolean isPage2 = event.getInventory().getHolder() instanceof Page2Holder;
        boolean isSettings = event.getInventory().getHolder() instanceof SettingsHolder;



        if (!isPage1 && !isPage2 && !isSettings) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Ignorar bordas (vidro preto)
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        if (isSettings) {
            handleSettingsClick(player, slot);
        } else if (isPage1) {
            handlePage1Click(player, slot);
        } else {
            handlePage2Click(player, slot);
        }
    }

    /**
     * Trata cliques na Página 1 (Sistemas Principais).
     */
    private void handlePage1Click(Player player, int slot) {
        switch (slot) {
            case 10 -> { // Meu Reino
                player.closeInventory();
                if (plugin.getKingdomInventory() != null) {
                    plugin.getKingdomInventory().openMainMenu(player);
                } else {
                    player.performCommand("reino");
                }
            }
            case 12 -> { // Mercado Global
                player.closeInventory();
                player.performCommand("market");
            }
            case 14 -> { // Leilão
                player.closeInventory();
                player.performCommand("leilao");
            }
            case 16 -> { // Bosses
                player.closeInventory();
                showBossInfo(player);
            }
            case 28 -> { // Conquistas
                player.closeInventory();
                if (plugin.getAchievementManager() != null) {
                    plugin.getAchievementManager().openAchievementMenu(player);
                }
            }
            case 30 -> { // Estatísticas
                player.closeInventory();
                showStats(player);
            }
            case 32 -> { // Daily Reward
                player.closeInventory();
                if (plugin.getDailyRewardManager() != null) {
                    plugin.getDailyRewardManager().openGUI(player);
                }
            }
            case 34 -> { // Correio
                player.closeInventory();
                player.performCommand("carta");
            }
            case 45 -> { // B41 — Configurações (abre submenu)
                openSettings(player);
            }
            case 49 -> { // Rankings
                player.closeInventory();
                showTopKills(player);
            }
            case 53 -> { // B38 — Próxima Página ▶
                openPage2(player);
            }
        }
    }

    /**
     * B38 — Trata cliques na Página 2 (Sistemas Adicionais).
     */
    private void handlePage2Click(Player player, int slot) {
        switch (slot) {
            case 10 -> { // ⚔ Duelos
                player.closeInventory();
                player.performCommand("duel ajuda");
            }
            case 12 -> { // 💀 Bounties
                player.closeInventory();
                player.performCommand("bounty listar");
            }
            case 14 -> { // ✨ Cosméticos
                player.closeInventory();
                player.performCommand("cosmetics");
            }
            case 16 -> { // 📚 Códex
                player.closeInventory();
                player.performCommand("codex");
            }
            case 28 -> { // 🎰 Crates
                player.closeInventory();
                if (plugin.getCrateGUI() != null) {
                    plugin.getCrateGUI().openCrateSelection(player);
                }
            }
            case 30 -> { // ⭐ Battle Pass
                player.closeInventory();
                player.performCommand("pass");
            }
            case 32 -> { // 🏅 Ranks & Kits
                player.closeInventory();
                player.performCommand("rank");
            }
            case 34 -> { // ⚖ Karma
                player.closeInventory();
                player.performCommand("karma");
            }
            case 45 -> { // ◀ Página Anterior
                openPage1(player);
            }
            case 46 -> { // 💎 VIP
                player.closeInventory();
                player.performCommand("vip info");
            }
            case 47 -> { // 📜 Quests
                player.closeInventory();
                player.performCommand("quests");
            }
            case 48 -> { // 🏛 Nações
                player.closeInventory();
                player.performCommand("nacao ajuda");
            }
            case 49 -> { // 🏷 Títulos
                player.closeInventory();
                if (plugin.getAchievementManager() != null) {
                    plugin.getAchievementManager().openTitleMenu(player);
                }
            }
            case 50 -> { // 🧭 Hub de Teleportes
                player.closeInventory();
                if (plugin.getTeleportHubGUI() != null) {
                    plugin.getTeleportHubGUI().open(player);
                }
            }
            case 51 -> { // 🎄 Eventos Sazonais
                player.closeInventory();
                player.performCommand("evento info");
            }
            case 53 -> { // 💬 Chat & Social
                player.closeInventory();
                showChatInfo(player);
            }
        }
    }

    // ─── Bedrock SimpleForm ─────────────────────────────────────────

    /**
     * B38 — SimpleForm com todos os botões (sem paginação, scroll nativo).
     */
    private void openBedrockForm(Player player) {
        MessageManager msg = plugin.getMessageManager();
        List<String> buttons = new ArrayList<>();

        // Página 1 — Sistemas Principais
        buttons.add(msg.get("main_menu.kingdom_name"));    // 0
        buttons.add(msg.get("main_menu.market_name"));     // 1
        buttons.add(msg.get("main_menu.auction_name"));    // 2
        buttons.add(msg.get("main_menu.bosses_name"));     // 3
        buttons.add(msg.get("main_menu.achievements_name")); // 4
        buttons.add(msg.get("main_menu.stats_name"));      // 5
        buttons.add(msg.get("main_menu.daily_name"));      // 6
        buttons.add(msg.get("main_menu.mail_name"));       // 7
        buttons.add(msg.get("main_menu.settings_name"));   // 8
        buttons.add(msg.get("main_menu.rankings_name"));   // 9

        // Página 2 — Sistemas Adicionais (B38)
        buttons.add(msg.get("main_menu.duel_name"));       // 10
        buttons.add(msg.get("main_menu.bounty_name"));     // 11
        buttons.add(msg.get("main_menu.cosmetics_name"));  // 12
        buttons.add(msg.get("main_menu.codex_name"));      // 13
        buttons.add(msg.get("main_menu.crate_name"));      // 14
        buttons.add(msg.get("main_menu.battlepass_name")); // 15
        buttons.add(msg.get("main_menu.ranks_name"));      // 16
        buttons.add(msg.get("main_menu.karma_name"));      // 17
        buttons.add(msg.get("main_menu.vip_name"));        // 18
        buttons.add(msg.get("main_menu.quests_name"));     // 19
        buttons.add(msg.get("main_menu.nation_name"));     // 20
        buttons.add(msg.get("main_menu.titles_name"));     // 21
        buttons.add(msg.get("main_menu.hub_name"));        // 22
        buttons.add(msg.get("main_menu.events_name"));     // 23
        buttons.add(msg.get("main_menu.chat_name"));       // 24

        plugin.getBedrockFormManager().sendSimpleForm(player,
                msg.get("main_menu.title"),
                msg.get("main_menu.bedrock_content"),
                buttons,
                index -> {
                    if (index < 0) return; // Fechou o form
                    switch (index) {
                        // ── Página 1 (originais) ──
                        case 0 -> { // Meu Reino
                            if (plugin.getKingdomInventory() != null) {
                                plugin.getKingdomInventory().openMainMenu(player);
                            } else {
                                player.performCommand("reino");
                            }
                        }
                        case 1 -> player.performCommand("market");
                        case 2 -> player.performCommand("leilao");
                        case 3 -> showBossInfo(player);
                        case 4 -> {
                            if (plugin.getAchievementManager() != null) {
                                plugin.getAchievementManager().openAchievementMenu(player);
                            }
                        }
                        case 5 -> showStats(player);
                        case 6 -> {
                            if (plugin.getDailyRewardManager() != null) {
                                plugin.getDailyRewardManager().openGUI(player);
                            }
                        }
                        case 7 -> player.performCommand("carta");
                        case 8 -> openBedrockSettingsForm(player); // B41 — Submenu de configurações Bedrock
                        case 9 -> showTopKills(player);
                        // ── Página 2 (B38 — novos) ──
                        case 10 -> player.performCommand("duel ajuda");
                        case 11 -> player.performCommand("bounty listar");
                        case 12 -> player.performCommand("cosmetics");
                        case 13 -> player.performCommand("codex");
                        case 14 -> {
                            if (plugin.getCrateGUI() != null) {
                                plugin.getCrateGUI().openCrateSelection(player);
                            }
                        }
                        case 15 -> player.performCommand("pass");
                        case 16 -> player.performCommand("rank");
                        case 17 -> player.performCommand("karma");
                        case 18 -> player.performCommand("vip info");
                        case 19 -> player.performCommand("quests");
                        case 20 -> player.performCommand("nacao ajuda");
                        case 21 -> {
                            if (plugin.getAchievementManager() != null) {
                                plugin.getAchievementManager().openTitleMenu(player);
                            }
                        }
                        case 22 -> {
                            if (plugin.getTeleportHubGUI() != null) {
                                plugin.getTeleportHubGUI().open(player);
                            }
                        }
                        case 23 -> player.performCommand("evento info");
                        case 24 -> showChatInfo(player);
                    }
                });
    }

    // ─── Ações auxiliares ───────────────────────────────────────────

    /**
     * Exibe informações do próximo boss no chat.
     */
    private void showBossInfo(Player player) {
        MessageManager msg = plugin.getMessageManager();
        BossManager bm = plugin.getBossManager();
        if (bm == null) {
            player.sendMessage(msg.get("main_menu.bosses_unavailable"));
            return;
        }

        // Verificar se há boss ativo
        if (!bm.getActiveBosses().isEmpty()) {
            player.performCommand("boss list");
        } else {
            player.performCommand("boss next");
        }
    }

    /**
     * Exibe estatísticas do jogador no chat.
     */
    private void showStats(Player player) {
        MessageManager msg = plugin.getMessageManager();
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (pd == null) {
            player.sendMessage(msg.get("main_menu.stats_unavailable"));
            return;
        }

        int kills = pd.getTotalKills();
        int deaths = pd.getTotalDeaths();
        String kdr = deaths > 0 ? String.format("%.2f", (double) kills / deaths) : String.valueOf(kills);
        long playtimeMs = pd.getTotalPlayTime();
        long hours = playtimeMs / (1000 * 60 * 60);
        long minutes = (playtimeMs / (1000 * 60)) % 60;

        player.sendMessage(msg.get("main_menu.stats_header"));
        player.sendMessage(msg.get("main_menu.stats_kills", kills));
        player.sendMessage(msg.get("main_menu.stats_deaths", deaths));
        player.sendMessage(msg.get("main_menu.stats_kdr", kdr));
        player.sendMessage(msg.get("main_menu.stats_playtime", hours, minutes));
        player.sendMessage(msg.get("main_menu.stats_blocks", pd.getClaimBlocks()));
        player.sendMessage(msg.get("main_menu.stats_footer"));
    }

    /**
     * Exibe ranking de kills no chat (placeholder até B7).
     */
    private void showTopKills(Player player) {
        MessageManager msg = plugin.getMessageManager();
        player.sendMessage(msg.get("main_menu.rankings_header"));
        player.sendMessage(msg.get("main_menu.rankings_coming_soon"));
        player.sendMessage(msg.get("main_menu.rankings_footer"));
    }

    /**
     * B38 — Exibe informações sobre canais de chat no chat.
     */
    private void showChatInfo(Player player) {
        MessageManager msg = plugin.getMessageManager();
        player.sendMessage(msg.get("main_menu.chat_info_header"));
        player.sendMessage(msg.get("main_menu.chat_info_global"));
        player.sendMessage(msg.get("main_menu.chat_info_kingdom"));
        player.sendMessage(msg.get("main_menu.chat_info_alliance"));
        player.sendMessage(msg.get("main_menu.chat_info_local"));
        player.sendMessage(msg.get("main_menu.chat_info_trade"));
        player.sendMessage(msg.get("main_menu.chat_info_footer"));
    }

    // ─── B41: Submenu de Configurações ─────────────────────────────

    /**
     * B41 — Abre o submenu de configurações (27 slots / 3 linhas).
     * Mostra estado atual de HUD, Som de Fronteira e Canal de Chat.
     */
    private void openSettings(Player player) {
        MessageManager msg = plugin.getMessageManager();
        String title = msg.get("main_menu.settings_title");
        Inventory inv = Bukkit.createInventory(new SettingsHolder(), 27,
                LegacyComponentSerializer.legacySection().deserialize(title));

        // Preencher com vidro preto
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // ---- Slot 10: HUD / Scoreboard ----
        boolean hudOn = plugin.getScoreboardManager() != null && plugin.getScoreboardManager().isHudEnabled(player);
        inv.setItem(10, createMenuItem(
                hudOn ? Material.LIME_DYE : Material.GRAY_DYE,
                msg.get("main_menu.settings_hud_name"),
                hudOn ? msg.get("main_menu.settings_hud_on") : msg.get("main_menu.settings_hud_off")));

        // ---- Slot 12: Som de Fronteira ----
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        boolean soundOn = pd != null && pd.isBorderSound();
        inv.setItem(12, createMenuItem(
                soundOn ? Material.LIME_DYE : Material.GRAY_DYE,
                msg.get("main_menu.settings_sound_name"),
                soundOn ? msg.get("main_menu.settings_sound_on") : msg.get("main_menu.settings_sound_off")));

        // ---- Slot 14: Canal de Chat ----
        ChatChannel currentChannel = ChatChannel.GLOBAL;
        if (plugin.getChatManager() != null) {
            currentChannel = plugin.getChatManager().getChannel(player.getUniqueId());
        }
        String channelLabel = currentChannel.getLabel();
        String chatLore = msg.get("main_menu.settings_chat_current", channelLabel) + "\n" +
                msg.get("main_menu.settings_chat_info");
        inv.setItem(14, createMenuItem(Material.OAK_SIGN,
                msg.get("main_menu.settings_chat_name"),
                chatLore));

        // ---- Slot 22: Voltar ----
        inv.setItem(22, createMenuItem(Material.ARROW,
                msg.get("main_menu.settings_back_name"),
                ""));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * B41 — Trata cliques no submenu de configurações.
     * Toggle reabrem o menu para atualizar o estado visual.
     */
    private void handleSettingsClick(Player player, int slot) {
        switch (slot) {
            case 10 -> { // 📊 HUD toggle
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().toggleHud(player);
                }
                // Reabrir para atualizar estado visual
                openSettings(player);
            }
            case 12 -> { // 🔊 Som de Fronteira toggle
                PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (pd != null) {
                    boolean newState = !pd.isBorderSound();
                    pd.setBorderSound(newState);
                    plugin.getPlayerDataManager().saveData(player.getUniqueId());
                    MessageManager msg = plugin.getMessageManager();
                    msg.send(player, newState ? "visualization.sound_enabled" : "visualization.sound_disabled");
                }
                openSettings(player);
            }
            case 14 -> { // 💬 Canal de Chat — ciclar (filtra canais disponíveis)
                if (plugin.getChatManager() != null) {
                    ChatChannel current = plugin.getChatManager().getChannel(player.getUniqueId());
                    ChatChannel next = getNextAvailableChannel(player, current);
                    plugin.getChatManager().setChannel(player.getUniqueId(), next);
                    MessageManager msg = plugin.getMessageManager();
                    player.sendMessage(msg.get("main_menu.settings_chat_current", next.getLabel()));
                }
                openSettings(player);
            }
            case 22 -> { // ◀ Voltar
                openPage1(player);
            }
        }
    }

    /**
     * B41 — SimpleForm de configurações para Bedrock.
     * Botões com estado visual (✅/❌) no texto.
     */
    private void openBedrockSettingsForm(Player player) {
        MessageManager msg = plugin.getMessageManager();
        List<String> buttons = new ArrayList<>();

        // HUD
        boolean hudOn = plugin.getScoreboardManager() != null && plugin.getScoreboardManager().isHudEnabled(player);
        buttons.add(msg.get("main_menu.settings_hud_name") + (hudOn ? " ✅" : " ❌")); // 0

        // Som de Fronteira
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        boolean soundOn = pd != null && pd.isBorderSound();
        buttons.add(msg.get("main_menu.settings_sound_name") + (soundOn ? " ✅" : " ❌")); // 1

        // Canal de Chat
        ChatChannel currentChannel = ChatChannel.GLOBAL;
        if (plugin.getChatManager() != null) {
            currentChannel = plugin.getChatManager().getChannel(player.getUniqueId());
        }
        buttons.add(msg.get("main_menu.settings_chat_name") + " (" + currentChannel.getLabel() + ")"); // 2

        // Voltar
        buttons.add(msg.get("main_menu.settings_back_name")); // 3

        plugin.getBedrockFormManager().sendSimpleForm(player,
                msg.get("main_menu.settings_title"),
                msg.get("main_menu.settings_bedrock_content"),
                buttons,
                index -> {
                    if (index < 0) return; // Fechou o form
                    switch (index) {
                        case 0 -> { // HUD toggle
                            if (plugin.getScoreboardManager() != null) {
                                plugin.getScoreboardManager().toggleHud(player);
                            }
                        }
                        case 1 -> { // Som de Fronteira toggle
                            PlayerData spd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                            if (spd != null) {
                                boolean ns = !spd.isBorderSound();
                                spd.setBorderSound(ns);
                                plugin.getPlayerDataManager().saveData(player.getUniqueId());
                                msg.send(player, ns ? "visualization.sound_enabled" : "visualization.sound_disabled");
                            }
                        }
                        case 2 -> { // Canal de Chat — ciclar (filtra canais disponíveis)
                            if (plugin.getChatManager() != null) {
                                ChatChannel ch = plugin.getChatManager().getChannel(player.getUniqueId());
                                ChatChannel nxt = getNextAvailableChannel(player, ch);
                                plugin.getChatManager().setChannel(player.getUniqueId(), nxt);
                                player.sendMessage(msg.get("main_menu.settings_chat_current", nxt.getLabel()));
                            }
                        }
                        case 3 -> open(player); // Voltar ao menu principal
                    }
                });
    }

    // ─── Utilitários ────────────────────────────────────────────────

    /** Delegação para MenuUtils (mantido privado para retrocompatibilidade). */
    private ItemStack createItem(Material material, String name) {
        return MenuUtils.createItem(material, name);
    }

    /** Delegação para MenuUtils (mantido privado para retrocompatibilidade). */
    private ItemStack createMenuItem(Material material, String name, String loreLine) {
        return MenuUtils.createMenuItem(material, name, loreLine);
    }

    /**
     * B41-fix — Retorna o próximo canal de chat disponível para o jogador.
     * Pula canais que requerem contexto (KINGDOM, ALLIANCE, NATION) se o jogador não qualifica.
     */
    private ChatChannel getNextAvailableChannel(Player player, ChatChannel current) {
        ChatChannel[] all = ChatChannel.values();
        int startIndex = (current.ordinal() + 1) % all.length;

        for (int i = 0; i < all.length; i++) {
            ChatChannel candidate = all[(startIndex + i) % all.length];

            // GLOBAL, LOCAL e TRADE são sempre disponíveis
            if (candidate == ChatChannel.GLOBAL || candidate == ChatChannel.LOCAL || candidate == ChatChannel.TRADE) {
                return candidate;
            }

            // KINGDOM requer pertencer a um reino
            if (candidate == ChatChannel.KINGDOM) {
                if (plugin.getKingdomManager().getKingdom(player.getUniqueId()) != null) {
                    return candidate;
                }
                continue;
            }

            // ALLIANCE requer aliados
            if (candidate == ChatChannel.ALLIANCE) {
                Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
                if (kingdom != null && !plugin.getKingdomManager().getAlliances(kingdom.getId()).isEmpty()) {
                    return candidate;
                }
                continue;
            }

            // NATION requer pertencer a uma nação (verificado via chat handler)
            if (candidate == ChatChannel.NATION) {
                // Pular se não há sistema de nações ou jogador não pertence a uma
                Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
                if (kingdom != null) {
                    // Verificar se o chat handler aceita o canal NATION para este jogador
                    // Por segurança, disponibilizar se tem reino (o handler já valida no envio)
                    return candidate;
                }
                continue;
            }
        }

        // Fallback seguro
        return ChatChannel.GLOBAL;
    }

    // ─── Holders (Identificadores de GUI) ───────────────────────────

    /**
     * InventoryHolder para Página 1 do menu.
     */
    public static class Page1Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // Apenas marcador
        }
    }

    /**
     * B38 — InventoryHolder para Página 2 do menu.
     */
    public static class Page2Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // Apenas marcador
        }
    }

    /**
     * B41 — InventoryHolder para submenu de Configurações.
     */
    public static class SettingsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // Apenas marcador
        }
    }

}

