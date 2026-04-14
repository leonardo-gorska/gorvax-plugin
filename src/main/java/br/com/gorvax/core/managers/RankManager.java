package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Sistema de Ranks Gratuitos com progressão automática.
 * Gerencia critérios de avanço, kits por rank, GUI de progresso,
 * e verificação periódica de elegibilidade.
 */
public class RankManager {

    private static final LegacyComponentSerializer LCS = LegacyComponentSerializer.legacySection();

    private final GorvaxCore plugin;
    private boolean enabled = true;

    // --- Definição dos ranks ---
    public enum GameRank {
        AVENTUREIRO(0, "§7[Aventureiro]", "rank_aventureiro", Material.STONE_SWORD),
        EXPLORADOR(1, "§a[Explorador]", "rank_explorador", Material.IRON_SWORD),
        GUERREIRO(2, "§c[Guerreiro]", "rank_guerreiro", Material.DIAMOND_SWORD),
        LENDARIO(3, "§6[Lendario]", "rank_lendario", Material.NETHERITE_SWORD);

        private final int order;
        private final String displayName;
        private final String groupName;
        private final Material icon;

        GameRank(int order, String displayName, String groupName, Material icon) {
            this.order = order;
            this.displayName = displayName;
            this.groupName = groupName;
            this.icon = icon;
        }

        public int getOrder() {
            return order;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getGroupName() {
            return groupName;
        }

        public Material getIcon() {
            return icon;
        }

        public GameRank next() {
            for (GameRank r : values()) {
                if (r.order == this.order + 1)
                    return r;
            }
            return null; // já é o máximo
        }
    }

    // --- Requisitos configuráveis ---
    public static class RankRequirement {
        public final long playtimeHours;
        public final int blocksBroken;
        public final int kills;
        public final int bossesKilled;
        public final double balance;
        public final int kingdomLevel; // 0 = não precisa

        public RankRequirement(long playtimeHours, int blocksBroken, int kills,
                int bossesKilled, double balance, int kingdomLevel) {
            this.playtimeHours = playtimeHours;
            this.blocksBroken = blocksBroken;
            this.kills = kills;
            this.bossesKilled = bossesKilled;
            this.balance = balance;
            this.kingdomLevel = kingdomLevel;
        }
    }

    private final Map<GameRank, RankRequirement> requirements = new EnumMap<>(GameRank.class);

    // --- Kits por rank ---
    public static class RankKit {
        public final List<ItemStack> items;
        public final long cooldownSeconds;
        public final String label;

        public RankKit(List<ItemStack> items, long cooldownSeconds, String label) {
            this.items = items;
            this.cooldownSeconds = cooldownSeconds;
            this.label = label;
        }
    }

    private final Map<GameRank, RankKit> kits = new EnumMap<>(GameRank.class);

    // VIP kits (chave = nome do tier VIP)
    private final Map<String, RankKit> vipKits = new LinkedHashMap<>();
    private static final String[] VIP_TIERS = { "vip", "mvp", "elite" };
    private static final String[] VIP_DISPLAYS = { "§a[✦ VIP]", "§b[✦ MVP]", "§6[⚡ ELITE]" };
    private static final Material[] VIP_ICONS = { Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
            Material.ELYTRA };

    // Cooldowns de kit por jogador (UUID -> kit key -> timestamp)
    private final Map<UUID, Map<String, Long>> kitCooldowns = new HashMap<>();

    public RankManager(GorvaxCore plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Carrega configurações de requisitos do config.yml.
     */
    public void reload() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks");
        if (section == null) {
            // Usar defaults
            loadDefaults();
            return;
        }
        this.enabled = section.getBoolean("enabled", true);

        // Carregar requisitos
        ConfigurationSection reqSection = section.getConfigurationSection("requirements");
        if (reqSection != null) {
            loadRequirement(reqSection, "explorador", GameRank.EXPLORADOR);
            loadRequirement(reqSection, "guerreiro", GameRank.GUERREIRO);
            loadRequirement(reqSection, "lendario", GameRank.LENDARIO);
        } else {
            loadDefaults();
        }

        // Carregar kits
        loadKits();
    }

    private void loadDefaults() {
        // Explorador: 10h jogadas, 500 blocos, $2k
        requirements.put(GameRank.EXPLORADOR, new RankRequirement(10, 500, 0, 0, 2000, 0));
        // Guerreiro: 30h, 50 kills, 5 bosses, $10k
        requirements.put(GameRank.GUERREIRO, new RankRequirement(30, 0, 50, 5, 10000, 0));
        // Lendário: 80h, 200 kills, 20 bosses, reino nível 3, $50k
        requirements.put(GameRank.LENDARIO, new RankRequirement(80, 0, 200, 20, 50000, 3));
    }

    private void loadRequirement(ConfigurationSection section, String key, GameRank rank) {
        ConfigurationSection rs = section.getConfigurationSection(key);
        if (rs == null)
            return;
        requirements.put(rank, new RankRequirement(
                rs.getLong("playtime_hours", 10),
                rs.getInt("blocks_broken", 0),
                rs.getInt("kills", 0),
                rs.getInt("bosses_killed", 0),
                rs.getDouble("balance", 0),
                rs.getInt("kingdom_level", 0)));
    }

    /**
     * Carrega kits fixos por rank.
     */
    private void loadKits() {
        // Kit Aventureiro (24h cooldown)
        List<ItemStack> aventureiroItems = new ArrayList<>();
        aventureiroItems.add(new ItemStack(Material.STONE_SWORD, 1));
        aventureiroItems.add(new ItemStack(Material.STONE_PICKAXE, 1));
        aventureiroItems.add(new ItemStack(Material.STONE_AXE, 1));
        aventureiroItems.add(new ItemStack(Material.COOKED_BEEF, 16));
        kits.put(GameRank.AVENTUREIRO, new RankKit(aventureiroItems, 86400, "Aventureiro"));

        // Kit Explorador (24h cooldown)
        List<ItemStack> exploradorItems = new ArrayList<>();
        exploradorItems.add(new ItemStack(Material.IRON_SWORD, 1));
        exploradorItems.add(new ItemStack(Material.IRON_PICKAXE, 1));
        exploradorItems.add(new ItemStack(Material.IRON_AXE, 1));
        exploradorItems.add(new ItemStack(Material.CHAINMAIL_HELMET, 1));
        exploradorItems.add(new ItemStack(Material.COOKED_BEEF, 32));
        kits.put(GameRank.EXPLORADOR, new RankKit(exploradorItems, 86400, "Explorador"));

        // Kit Guerreiro (12h cooldown)
        List<ItemStack> guerreiroItems = new ArrayList<>();
        ItemStack gSword = new ItemStack(Material.DIAMOND_SWORD, 1);
        gSword.addEnchantment(Enchantment.SHARPNESS, 1);
        guerreiroItems.add(gSword);
        guerreiroItems.add(createProtArmor(Material.IRON_HELMET, 1));
        guerreiroItems.add(createProtArmor(Material.IRON_CHESTPLATE, 1));
        guerreiroItems.add(createProtArmor(Material.IRON_LEGGINGS, 1));
        guerreiroItems.add(createProtArmor(Material.IRON_BOOTS, 1));
        guerreiroItems.add(new ItemStack(Material.GOLDEN_APPLE, 5));
        kits.put(GameRank.GUERREIRO, new RankKit(guerreiroItems, 43200, "Guerreiro"));

        // Kit Lendário (12h cooldown)
        List<ItemStack> lendarioItems = new ArrayList<>();
        ItemStack lSword = new ItemStack(Material.DIAMOND_SWORD, 1);
        lSword.addEnchantment(Enchantment.SHARPNESS, 2);
        lSword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
        lendarioItems.add(lSword);
        ItemStack lPick = new ItemStack(Material.DIAMOND_PICKAXE, 1);
        lPick.addEnchantment(Enchantment.EFFICIENCY, 2);
        lendarioItems.add(lPick);
        lendarioItems.add(createProtArmor(Material.DIAMOND_HELMET, 2));
        lendarioItems.add(createProtArmor(Material.DIAMOND_CHESTPLATE, 2));
        lendarioItems.add(createProtArmor(Material.DIAMOND_LEGGINGS, 2));
        lendarioItems.add(createProtArmor(Material.DIAMOND_BOOTS, 2));
        lendarioItems.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
        kits.put(GameRank.LENDARIO, new RankKit(lendarioItems, 43200, "Lendário"));

        // ============ KITS VIP ============
        loadVipKits();
    }

    private void loadVipKits() {
        // Kit VIP (24h)
        List<ItemStack> vipItems = new ArrayList<>();
        ItemStack vs = new ItemStack(Material.DIAMOND_SWORD, 1);
        vs.addEnchantment(Enchantment.SHARPNESS, 3);
        vs.addEnchantment(Enchantment.UNBREAKING, 2);
        vipItems.add(vs);
        ItemStack vp = new ItemStack(Material.DIAMOND_PICKAXE, 1);
        vp.addEnchantment(Enchantment.EFFICIENCY, 3);
        vp.addEnchantment(Enchantment.UNBREAKING, 2);
        vipItems.add(vp);
        vipItems.add(createProtArmor(Material.DIAMOND_HELMET, 3));
        vipItems.add(createProtArmor(Material.DIAMOND_CHESTPLATE, 3));
        vipItems.add(createProtArmor(Material.DIAMOND_LEGGINGS, 3));
        vipItems.add(createProtArmor(Material.DIAMOND_BOOTS, 3));
        vipItems.add(new ItemStack(Material.GOLDEN_APPLE, 16));
        vipItems.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 32));
        vipKits.put("vip", new RankKit(vipItems, 86400, "VIP"));

        // Kit MVP (24h)
        List<ItemStack> mvpItems = new ArrayList<>();
        ItemStack ms = new ItemStack(Material.NETHERITE_SWORD, 1);
        ms.addEnchantment(Enchantment.SHARPNESS, 3);
        mvpItems.add(ms);
        ItemStack mp = new ItemStack(Material.NETHERITE_PICKAXE, 1);
        mp.addEnchantment(Enchantment.EFFICIENCY, 4);
        mvpItems.add(mp);
        mvpItems.add(createProtArmor(Material.NETHERITE_HELMET, 3));
        mvpItems.add(createProtArmor(Material.NETHERITE_CHESTPLATE, 3));
        mvpItems.add(createProtArmor(Material.NETHERITE_LEGGINGS, 3));
        mvpItems.add(createProtArmor(Material.NETHERITE_BOOTS, 3));
        mvpItems.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5));
        mvpItems.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        vipKits.put("mvp", new RankKit(mvpItems, 86400, "MVP"));

        // Kit Elite (48h)
        List<ItemStack> eliteItems = new ArrayList<>();
        ItemStack es = new ItemStack(Material.NETHERITE_SWORD, 1);
        es.addEnchantment(Enchantment.SHARPNESS, 5);
        es.addEnchantment(Enchantment.FIRE_ASPECT, 2);
        eliteItems.add(es);
        ItemStack ep = new ItemStack(Material.NETHERITE_PICKAXE, 1);
        ep.addEnchantment(Enchantment.EFFICIENCY, 5);
        ep.addEnchantment(Enchantment.FORTUNE, 3);
        eliteItems.add(ep);
        eliteItems.add(createProtArmor(Material.NETHERITE_HELMET, 4));
        eliteItems.add(createProtArmor(Material.NETHERITE_CHESTPLATE, 4));
        eliteItems.add(createProtArmor(Material.NETHERITE_LEGGINGS, 4));
        eliteItems.add(createProtArmor(Material.NETHERITE_BOOTS, 4));
        eliteItems.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 10));
        ItemStack elytra = new ItemStack(Material.ELYTRA, 1);
        elytra.addEnchantment(Enchantment.UNBREAKING, 3);
        elytra.addEnchantment(Enchantment.MENDING, 1);
        eliteItems.add(elytra);
        eliteItems.add(new ItemStack(Material.FIREWORK_ROCKET, 64));
        vipKits.put("elite", new RankKit(eliteItems, 172800, "Elite"));
    }

    private ItemStack createProtArmor(Material material, int level) {
        ItemStack item = new ItemStack(material, 1);
        item.addEnchantment(Enchantment.PROTECTION, level);
        return item;
    }

    // =========================================================================
    // Rank Detection & Progression
    // =========================================================================

    /**
     * Detecta o rank atual de um jogador pelos seus grupos LuckPerms.
     * Retorna o rank de maior prioridade encontrado.
     */
    public GameRank getPlayerRank(Player player) {
        LuckPerms lp = plugin.getLuckPerms();
        if (lp == null)
            return GameRank.AVENTUREIRO;

        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return GameRank.AVENTUREIRO;

        GameRank highest = GameRank.AVENTUREIRO;
        for (GameRank rank : GameRank.values()) {
            if (rank == GameRank.AVENTUREIRO)
                continue;
            boolean has = user.getInheritedGroups(user.getQueryOptions())
                    .stream().anyMatch(g -> g.getName().equalsIgnoreCase(rank.getGroupName()));
            if (has && rank.getOrder() > highest.getOrder()) {
                highest = rank;
            }
        }
        return highest;
    }

    /**
     * Verifica se um jogador pode avançar para o próximo rank.
     * Se sim, aplica o rank automaticamente.
     */
    public boolean checkAndPromote(Player player) {
        if (!enabled)
            return false;

        GameRank current = getPlayerRank(player);
        GameRank next = current.next();
        if (next == null)
            return false; // já é Lendário

        RankRequirement req = requirements.get(next);
        if (req == null)
            return false;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (pd == null)
            return false;

        // Verificar todos os requisitos
        long hoursPlayed = TimeUnit.MILLISECONDS.toHours(pd.getTotalPlayTime());
        if (hoursPlayed < req.playtimeHours)
            return false;
        if (pd.getTotalBlocksBroken() < req.blocksBroken)
            return false;
        if (pd.getTotalKills() < req.kills)
            return false;
        if (pd.getBossesKilled() < req.bossesKilled)
            return false;

        // Verificar saldo Vault
        Economy econ = GorvaxCore.getEconomy();
        if (req.balance > 0) {
            if (econ == null)
                return false; // Economia indisponível, não promover
            if (econ.getBalance(player) < req.balance)
                return false;
        }

        // Verificar nível do reino se necessário
        if (req.kingdomLevel > 0) {
            var km = plugin.getKingdomManager();
            if (km != null) {
                var kingdom = km.getKingdom(player.getUniqueId());
                if (kingdom == null)
                    return false;
                int kLevel = km.getKingdomLevel(kingdom.getId());
                if (kLevel < req.kingdomLevel)
                    return false;
            } else {
                return false;
            }
        }

        // Todos os requisitos atingidos — promover!
        applyRank(player, next);
        return true;
    }

    /**
     * Aplica um rank ao jogador via LuckPerms.
     */
    private void applyRank(Player player, GameRank rank) {
        LuckPerms lp = plugin.getLuckPerms();
        if (lp == null)
            return;

        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return;

        // Remover ranks anteriores
        for (GameRank r : GameRank.values()) {
            if (r == GameRank.AVENTUREIRO)
                continue;
            InheritanceNode node = InheritanceNode.builder(r.getGroupName()).build();
            user.data().remove(node);
        }

        // Adicionar novo rank
        if (rank != GameRank.AVENTUREIRO) {
            InheritanceNode node = InheritanceNode.builder(rank.getGroupName()).build();
            user.data().add(node);
        }

        lp.getUserManager().saveUser(user);

        // Notificação épica
        player.showTitle(Title.title(
                LCS.deserialize("§6§l⬆ RANK UP!"),
                LCS.deserialize(rank.getDisplayName()),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Broadcast global
        Bukkit.broadcast(LCS.deserialize("§8§m━━━━━━━━━━━━━━━━━━━━━━━"));
        Bukkit.broadcast(LCS.deserialize("§6§l  ⬆ RANK UP!"));
        Bukkit.broadcast(LCS.deserialize("§f  " + player.getName() + " §7avançou para " + rank.getDisplayName()));
        Bukkit.broadcast(LCS.deserialize("§8§m━━━━━━━━━━━━━━━━━━━━━━━"));

        plugin.getLogger().info("[Ranks] " + player.getName() + " promovido para " + rank.name());
    }

    // =========================================================================
    // Kit System
    // =========================================================================

    /**
     * Tenta dar o kit do rank para o jogador.
     * Retorna true se deu o kit, false se em cooldown ou sem espaço.
     */
    public boolean giveKit(Player player, GameRank rank) {
        GameRank playerRank = getPlayerRank(player);
        if (playerRank.getOrder() < rank.getOrder()) {
            player.sendMessage(
                    "§c§lRANK §8» §7Você precisa do rank " + rank.getDisplayName() + " §7para usar este kit.");
            return false;
        }

        RankKit kit = kits.get(rank);
        if (kit == null)
            return false;

        return deliverKit(player, kit, "rank_" + rank.name());
    }

    /**
     * Tenta dar um kit VIP ao jogador.
     */
    public boolean giveVipKit(Player player, String tier) {
        RankKit kit = vipKits.get(tier);
        if (kit == null)
            return false;

        // Verificar se o jogador tem o tier VIP via VipManager
        VipManager vm = plugin.getVipManager();
        if (vm == null || !vm.isEnabled()) {
            player.sendMessage("§c§lKIT §8» §7Sistema VIP não está ativo.");
            return false;
        }

        VipManager.VipTier playerTier = vm.getVipTier(player);
        if (playerTier == VipManager.VipTier.NONE) {
            player.sendMessage("§c§lKIT §8» §7Você precisa de um rank §dVIP §7para usar este kit.");
            return false;
        }

        // Verificar hierarquia: VIP < VIP_PLUS (MVP) < ELITE < LENDARIO
        int requiredPriority = getVipPriority(tier);
        if (playerTier.getPriority() < requiredPriority) {
            player.sendMessage("§c§lKIT §8» §7Seu rank VIP não é alto o suficiente para este kit.");
            return false;
        }

        return deliverKit(player, kit, "vip_" + tier);
    }

    private int getVipPriority(String tier) {
        return switch (tier) {
            case "vip" -> 1;
            case "mvp" -> 2;
            case "elite" -> 3;
            default -> 99;
        };
    }

    /**
     * Entrega um kit ao jogador (lógica compartilhada entre rank e VIP).
     */
    private boolean deliverKit(Player player, RankKit kit, String cooldownKey) {
        // Verificar cooldown
        Map<String, Long> playerCds = kitCooldowns.computeIfAbsent(player.getUniqueId(),
                k -> new HashMap<>());
        Long lastUse = playerCds.get(cooldownKey);
        if (lastUse != null) {
            long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
            if (elapsed < kit.cooldownSeconds) {
                long remaining = kit.cooldownSeconds - elapsed;
                String time = formatTime(remaining);
                player.sendMessage("§c§lKIT §8» §7Aguarde §e" + time + " §7para usar este kit novamente.");
                return false;
            }
        }

        // Verificar espaço no inventário
        int emptySlots = 0;
        for (ItemStack is : player.getInventory().getStorageContents()) {
            if (is == null || is.getType() == Material.AIR)
                emptySlots++;
        }
        if (emptySlots < kit.items.size()) {
            player.sendMessage("§c§lKIT §8» §7Você precisa de pelo menos §e" + kit.items.size()
                    + " slots §7livros no inventário.");
            return false;
        }

        // Dar itens
        for (ItemStack item : kit.items) {
            player.getInventory().addItem(item.clone());
        }
        playerCds.put(cooldownKey, System.currentTimeMillis());

        player.sendMessage("§a§lKIT §8» §fVocê recebeu o kit §f" + kit.label + "§f!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        return true;
    }

    // =========================================================================
    // GUI de Progresso de Rank
    // =========================================================================

    /**
     * Abre a GUI de progresso de ranks para o jogador.
     */
    public void openProgressGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LCS.deserialize("§8⬆ Progressão de Ranks"));
        GameRank currentRank = getPlayerRank(player);
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        Economy econ = GorvaxCore.getEconomy();

        // Linha decorativa superior (slot 0-8)
        ItemStack glass = createGlass(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)
            inv.setItem(i, glass);

        // Ranks nas posições 10, 12, 14, 16 (centrados na segunda linha)
        int[] rankSlots = { 19, 21, 23, 25 }; // linha 3, bem espaçados

        for (int i = 0; i < GameRank.values().length; i++) {
            GameRank rank = GameRank.values()[i];
            int slot = rankSlots[i];

            if (rank.getOrder() < currentRank.getOrder()) {
                // Rank já conquistado — verde
                inv.setItem(slot, createRankItem(rank, pd, econ, player, "§a§l✔ CONQUISTADO", Material.EMERALD_BLOCK));
            } else if (rank == currentRank) {
                // Rank atual — esmeralda brilhante
                inv.setItem(slot,
                        createRankItem(rank, pd, econ, player, "§a§l★ SEU RANK ATUAL", Material.EMERALD_BLOCK));
            } else if (rank.getOrder() == currentRank.getOrder() + 1) {
                // Próximo rank — ouro com barra de progresso
                inv.setItem(slot, createNextRankItem(rank, pd, econ, player));
            } else {
                // Rank futuro — bloqueado
                inv.setItem(slot, createRankItem(rank, pd, econ, player, "§c§l🔒 BLOQUEADO", Material.IRON_BLOCK));
            }
        }

        // Linha de kits (slot 37, 39, 41, 43)
        int[] kitSlots = { 37, 39, 41, 43 };
        for (int i = 0; i < GameRank.values().length; i++) {
            GameRank rank = GameRank.values()[i];
            int slot = kitSlots[i];
            if (rank.getOrder() <= currentRank.getOrder()) {
                inv.setItem(slot, createKitPreviewItem(rank, true));
            } else {
                inv.setItem(slot, createKitPreviewItem(rank, false));
            }
        }

        // Info central (slot 22)
        ItemStack info = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(LCS.deserialize("§e§lSeu Progresso"));
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Rank atual: " + currentRank.getDisplayName());
        if (currentRank.next() != null) {
            infoLore.add("§7Próximo rank: " + currentRank.next().getDisplayName());
        } else {
            infoLore.add("§a§lVocê atingiu o rank máximo!");
        }
        infoLore.add("");
        long hours = pd != null ? TimeUnit.MILLISECONDS.toHours(pd.getTotalPlayTime()) : 0;
        infoLore.add("§7⏱ Tempo jogado: §f" + hours + "h");
        infoLore.add("§7⛏ Blocos quebrados: §f" + (pd != null ? pd.getTotalBlocksBroken() : 0));
        infoLore.add("§7⚔ Kills PvP: §f" + (pd != null ? pd.getTotalKills() : 0));
        infoLore.add("§7👹 Bosses participados: §f" + (pd != null ? pd.getBossesKilled() : 0));
        if (econ != null) {
            infoLore.add("§7💰 Saldo: §fR$" + String.format("%.2f", econ.getBalance(player)));
        }
        infoMeta.lore(infoLore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);

        // Linha decorativa inferior
        for (int i = 45; i < 54; i++)
            inv.setItem(i, glass);

        // Setas conectando ranks (slots 20, 22, 24 na linha dos ranks)
        ItemStack arrow = createGlass(Material.LIME_STAINED_GLASS_PANE, "§a→");
        inv.setItem(20, arrow);
        inv.setItem(24, arrow);

        player.openInventory(inv);
    }

    private ItemStack createRankItem(GameRank rank, PlayerData pd, Economy econ,
            Player player, String statusLine, Material blockMat) {
        ItemStack item = new ItemStack(blockMat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LCS.deserialize(rank.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(statusLine);
        lore.add("");

        RankRequirement req = requirements.get(rank);
        if (req != null && rank != GameRank.AVENTUREIRO) {
            lore.add("§e§lRequisitos:");
            if (req.playtimeHours > 0)
                lore.add("§7  ⏱ " + req.playtimeHours + "h jogadas");
            if (req.blocksBroken > 0)
                lore.add("§7  ⛏ " + req.blocksBroken + " blocos quebrados");
            if (req.kills > 0)
                lore.add("§7  ⚔ " + req.kills + " kills PvP");
            if (req.bossesKilled > 0)
                lore.add("§7  👹 " + req.bossesKilled + " bosses participados");
            if (req.balance > 0)
                lore.add("§7  💰 R$" + String.format("%.0f", req.balance) + " de saldo");
            if (req.kingdomLevel > 0)
                lore.add("§7  🏰 Reino nível " + req.kingdomLevel + "+");
        } else if (rank == GameRank.AVENTUREIRO) {
            lore.add("§7  Rank inicial — automático ao entrar.");
        }

        lore.add("");
        lore.add("§8Kit: " + rank.getGroupName());
        meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextRankItem(GameRank rank, PlayerData pd, Economy econ, Player player) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LCS.deserialize(rank.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add("§6§l⬆ PRÓXIMO RANK");
        lore.add("");

        RankRequirement req = requirements.get(rank);
        if (req != null && pd != null) {
            // Calcular progresso individual
            int totalReqs = 0;
            int metReqs = 0;

            long hours = TimeUnit.MILLISECONDS.toHours(pd.getTotalPlayTime());
            if (req.playtimeHours > 0) {
                totalReqs++;
                String check = hours >= req.playtimeHours ? "§a✔" : "§c✘";
                lore.add(check + " §7⏱ " + hours + "/" + req.playtimeHours + "h jogadas");
                if (hours >= req.playtimeHours)
                    metReqs++;
            }
            if (req.blocksBroken > 0) {
                totalReqs++;
                String check = pd.getTotalBlocksBroken() >= req.blocksBroken ? "§a✔" : "§c✘";
                lore.add(check + " §7⛏ " + pd.getTotalBlocksBroken() + "/" + req.blocksBroken + " blocos");
                if (pd.getTotalBlocksBroken() >= req.blocksBroken)
                    metReqs++;
            }
            if (req.kills > 0) {
                totalReqs++;
                String check = pd.getTotalKills() >= req.kills ? "§a✔" : "§c✘";
                lore.add(check + " §7⚔ " + pd.getTotalKills() + "/" + req.kills + " kills");
                if (pd.getTotalKills() >= req.kills)
                    metReqs++;
            }
            if (req.bossesKilled > 0) {
                totalReqs++;
                String check = pd.getBossesKilled() >= req.bossesKilled ? "§a✔" : "§c✘";
                lore.add(check + " §7👹 " + pd.getBossesKilled() + "/" + req.bossesKilled + " bosses");
                if (pd.getBossesKilled() >= req.bossesKilled)
                    metReqs++;
            }
            if (req.balance > 0 && econ != null) {
                totalReqs++;
                double bal = econ.getBalance(player);
                String check = bal >= req.balance ? "§a✔" : "§c✘";
                lore.add(check + " §7💰 R$" + String.format("%.0f", bal) + "/R$" + String.format("%.0f", req.balance));
                if (bal >= req.balance)
                    metReqs++;
            }
            if (req.kingdomLevel > 0) {
                totalReqs++;
                int kLevel = 0;
                var km = plugin.getKingdomManager();
                if (km != null) {
                    var kingdom = km.getKingdom(player.getUniqueId());
                    if (kingdom != null)
                        kLevel = km.getKingdomLevel(kingdom.getId());
                }
                String check = kLevel >= req.kingdomLevel ? "§a✔" : "§c✘";
                lore.add(check + " §7🏰 Reino nível " + kLevel + "/" + req.kingdomLevel);
                if (kLevel >= req.kingdomLevel)
                    metReqs++;
            }

            // Barra de progresso
            lore.add("");
            double progress = totalReqs > 0 ? (double) metReqs / totalReqs : 0;
            int filled = (int) (progress * 10);
            StringBuilder bar = new StringBuilder("§a");
            for (int i = 0; i < 10; i++) {
                if (i == filled)
                    bar.append("§7");
                bar.append("█");
            }
            int pct = (int) (progress * 100);
            lore.add("§7Progresso: " + bar + " §f" + pct + "%");
        }

        meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createKitPreviewItem(GameRank rank, boolean available) {
        RankKit kit = kits.get(rank);
        Material mat = available ? Material.CHEST : Material.BARRIER;
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LCS.deserialize((available ? "§a" : "§c") + "Kit " + rank.getDisplayName()));
        List<String> lore = new ArrayList<>();
        if (available && kit != null) {
            lore.add("§a§lDISPONÍVEL — Clique para resgatar!");
            lore.add("");
            lore.add("§7Itens:");
            for (ItemStack ki : kit.items) {
                String name = ki.getType().name().replace("_", " ").toLowerCase();
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                lore.add("§f  • " + name + " x" + ki.getAmount());
            }
            lore.add("");
            lore.add("§7Cooldown: §f" + formatTime(kit.cooldownSeconds));
        } else {
            lore.add("§c§l🔒 Rank insuficiente");
        }
        meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlass(Material mat, String name) {
        ItemStack glass = new ItemStack(mat, 1);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(LCS.deserialize(name));
        glass.setItemMeta(meta);
        return glass;
    }

    // =========================================================================
    // Kit GUI (Menu /kit)
    // =========================================================================

    /**
     * Abre a GUI de seleção de kits.
     */
    public void openKitGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, LCS.deserialize("§8📦 Seus Kits"));
        GameRank currentRank = getPlayerRank(player);

        ItemStack glass = createGlass(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++)
            inv.setItem(i, glass);

        // Título: Kits de Rank (linha 1)
        ItemStack rankTitle = createGlass(Material.LIME_STAINED_GLASS_PANE, "§a§l⚔ Kits de Rank");
        inv.setItem(4, rankTitle);

        // Rank kits nos slots 10, 12, 14, 16 (linha 2)
        int[] rankSlots = { 10, 12, 14, 16 };
        for (int i = 0; i < GameRank.values().length; i++) {
            GameRank rank = GameRank.values()[i];
            boolean available = rank.getOrder() <= currentRank.getOrder();
            inv.setItem(rankSlots[i], createKitItem(rank, player, available));
        }

        // Título: Kits VIP (linha 3)
        ItemStack vipTitle = createGlass(Material.MAGENTA_STAINED_GLASS_PANE, "§d§l✦ Kits VIP");
        inv.setItem(22, vipTitle);

        // VIP kits nos slots 29, 31, 33 (linha 4)
        VipManager vm = plugin.getVipManager();
        VipManager.VipTier playerVipTier = (vm != null && vm.isEnabled())
                ? vm.getVipTier(player)
                : VipManager.VipTier.NONE;

        int[] vipSlots = { 29, 31, 33 };
        for (int i = 0; i < VIP_TIERS.length; i++) {
            String tier = VIP_TIERS[i];
            RankKit vKit = vipKits.get(tier);
            boolean hasAccess = playerVipTier.getPriority() >= getVipPriority(tier);
            inv.setItem(vipSlots[i], createVipKitItem(tier, i, vKit, player, hasAccess));
        }

        player.openInventory(inv);
    }

    private ItemStack createKitItem(GameRank rank, Player player, boolean available) {
        RankKit kit = kits.get(rank);
        ItemStack item = new ItemStack(available ? rank.getIcon() : Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LCS.deserialize((available ? "§a" : "§c") + "Kit " + rank.getDisplayName()));
        List<String> lore = new ArrayList<>();

        if (available && kit != null) {
            lore.add(getCooldownLine(player, "rank_" + rank.name(), kit.cooldownSeconds));
            lore.add("");
            addItemsLore(lore, kit);
            lore.add("");
            lore.add("§7Cooldown: §f" + formatTime(kit.cooldownSeconds));
        } else {
            lore.add("§c§l🔒 Requer rank: " + rank.getDisplayName());
        }

        meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
        if (available)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createVipKitItem(String tier, int index, RankKit kit, Player player, boolean available) {
        ItemStack item = new ItemStack(available ? VIP_ICONS[index] : Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LCS.deserialize((available ? "§d" : "§c") + "Kit " + VIP_DISPLAYS[index]));
        List<String> lore = new ArrayList<>();

        if (available && kit != null) {
            lore.add(getCooldownLine(player, "vip_" + tier, kit.cooldownSeconds));
            lore.add("");
            addItemsLore(lore, kit);
            lore.add("");
            lore.add("§7Cooldown: §f" + formatTime(kit.cooldownSeconds));
        } else {
            lore.add("§c§l🔒 Requer rank: " + VIP_DISPLAYS[index]);
        }

        meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
        if (available)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private String getCooldownLine(Player player, String cooldownKey, long cdSeconds) {
        Map<String, Long> cds = kitCooldowns.get(player.getUniqueId());
        Long lastUse = cds != null ? cds.get(cooldownKey) : null;
        if (lastUse != null) {
            long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
            if (elapsed < cdSeconds) {
                long remaining = cdSeconds - elapsed;
                return "§c⏳ Disponível em: §f" + formatTime(remaining);
            }
        }
        return "§a✔ DISPONÍVEL — Clique para resgatar!";
    }

    private void addItemsLore(List<String> lore, RankKit kit) {
        lore.add("§7Itens:");
        for (ItemStack ki : kit.items) {
            String name = formatItemName(ki.getType());
            lore.add("§f  • " + name + " x" + ki.getAmount());
        }
    }

    // Expose VIP tier constants for the listener
    public String[] getVipTiers() {
        return VIP_TIERS;
    }

    public Map<String, RankKit> getVipKits() {
        return vipKits;
    }

    // =========================================================================
    // Utilitários
    // =========================================================================

    public String formatTime(long seconds) {
        if (seconds >= 3600) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            return h + "h " + m + "min";
        } else if (seconds >= 60) {
            return (seconds / 60) + "min";
        }
        return seconds + "s";
    }

    private String formatItemName(Material mat) {
        String name = mat.name().replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!sb.isEmpty())
                sb.append(" ");
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return sb.toString();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<GameRank, RankRequirement> getRequirements() {
        return requirements;
    }

    public Map<GameRank, RankKit> getKits() {
        return kits;
    }
}
