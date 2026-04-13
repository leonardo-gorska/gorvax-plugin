package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B14 — Gerencia ranks VIP e seus benefícios.
 * Os tiers são determinados pelo grupo LuckPerms do jogador.
 * Benefícios incluem blocos extras, homes extras, keys mensais e desconto no mercado.
 */
public class VipManager {

    // Enum dos tiers VIP disponíveis
    public enum VipTier {
        NONE("Nenhum", "§7Nenhum", 0),
        VIP("VIP", "§a[✦ VIP]", 1),
        VIP_PLUS("VIP+", "§b[✦ VIP+]", 2),
        ELITE("ELITE", "§6[⚡ ELITE]", 3),
        LENDARIO("LENDÁRIO", "§d[🐉 LENDÁRIO]", 4);

        private final String label;
        private String displayName;
        private final int priority;

        VipTier(String label, String defaultDisplay, int priority) {
            this.label = label;
            this.displayName = defaultDisplay;
            this.priority = priority;
        }

        public String getLabel() { return label; }
        public String getDisplayName() { return displayName; }
        public int getPriority() { return priority; }
        void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    // Dados de benefício por tier (carregados do config)
    public record TierBenefits(
            int extraClaimBlocks,
            int extraHomes,
            Map<String, Integer> monthlyKeys,
            double marketDiscountPercent
    ) {}

    private final GorvaxCore plugin;
    private boolean enabled;

    // Mapeamento grupo LuckPerms → VipTier
    private final Map<String, VipTier> groupToTier = new HashMap<>();

    // Benefícios por tier (carregados do config)
    private final EnumMap<VipTier, TierBenefits> tierBenefits = new EnumMap<>(VipTier.class);

    // Cache de blocos já aplicados (evita dar blocos duplicados ao relogar)
    // Chave: UUID do jogador, Valor: tier que já recebeu blocos
    private final ConcurrentHashMap<UUID, VipTier> appliedBlocks = new ConcurrentHashMap<>();

    // Dia do mês para distribuir keys mensais
    private int monthlyKeyDay;

    public VipManager(GorvaxCore plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Recarrega configurações de VIP do config.yml.
     */
    public void reload() {
        groupToTier.clear();
        tierBenefits.clear();

        this.enabled = plugin.getConfig().getBoolean("vip.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("[VIP] Sistema VIP desativado no config.");
            return;
        }

        // Carregar mapeamento grupo → tier
        ConfigurationSection groupsSection = plugin.getConfig().getConfigurationSection("vip.groups");
        if (groupsSection != null) {
            mapGroup(groupsSection, "vip", VipTier.VIP);
            mapGroup(groupsSection, "vip_plus", VipTier.VIP_PLUS);
            mapGroup(groupsSection, "elite", VipTier.ELITE);
            mapGroup(groupsSection, "lendario", VipTier.LENDARIO);
        } else {
            // Padrões
            groupToTier.put("vip", VipTier.VIP);
            groupToTier.put("vip-plus", VipTier.VIP_PLUS);
            groupToTier.put("elite", VipTier.ELITE);
            groupToTier.put("lendario", VipTier.LENDARIO);
        }

        // Carregar benefícios por tier
        ConfigurationSection tiersSection = plugin.getConfig().getConfigurationSection("vip.tiers");
        if (tiersSection != null) {
            loadTierBenefits(tiersSection, "vip", VipTier.VIP);
            loadTierBenefits(tiersSection, "vip_plus", VipTier.VIP_PLUS);
            loadTierBenefits(tiersSection, "elite", VipTier.ELITE);
            loadTierBenefits(tiersSection, "lendario", VipTier.LENDARIO);
        } else {
            // Padrões
            tierBenefits.put(VipTier.VIP, new TierBenefits(500, 2, Map.of("raro", 1), 0));
            tierBenefits.put(VipTier.VIP_PLUS, new TierBenefits(1500, 5, Map.of("raro", 2, "lendario", 1), 5));
            tierBenefits.put(VipTier.ELITE, new TierBenefits(3000, 10, Map.of("raro", 3, "lendario", 1), 10));
            tierBenefits.put(VipTier.LENDARIO, new TierBenefits(5000, 15, Map.of("raro", 3, "lendario", 2), 15));
        }

        // NONE sempre tem 0 benefícios
        tierBenefits.put(VipTier.NONE, new TierBenefits(0, 0, Collections.emptyMap(), 0));

        this.monthlyKeyDay = plugin.getConfig().getInt("vip.monthly_key_day", 1);

        plugin.getLogger().info("[VIP] Sistema VIP carregado — " + groupToTier.size() + " tiers configurados.");
    }

    private void mapGroup(ConfigurationSection section, String configKey, VipTier tier) {
        String groupName = section.getString(configKey);
        if (groupName != null && !groupName.isEmpty()) {
            groupToTier.put(groupName.toLowerCase(), tier);
        }
    }

    private void loadTierBenefits(ConfigurationSection tiersSection, String key, VipTier tier) {
        ConfigurationSection sec = tiersSection.getConfigurationSection(key);
        if (sec == null) return;

        // Atualizar display name do tier se configurado
        String display = sec.getString("display_name");
        if (display != null) {
            tier.setDisplayName(display);
        }

        int blocks = sec.getInt("extra_claim_blocks", 0);
        int homes = sec.getInt("extra_homes", 0);
        double discount = sec.getDouble("market_discount_percent", 0);

        // Carregar keys mensais
        Map<String, Integer> keys = new HashMap<>();
        ConfigurationSection keysSection = sec.getConfigurationSection("monthly_keys");
        if (keysSection != null) {
            for (String keyType : keysSection.getKeys(false)) {
                keys.put(keyType, keysSection.getInt(keyType, 0));
            }
        }

        tierBenefits.put(tier, new TierBenefits(blocks, homes, keys, discount));
    }

    /**
     * Determina o VipTier de um jogador baseado nos seus grupos LuckPerms.
     * Retorna o tier de maior prioridade encontrado.
     */
    public VipTier getVipTier(Player player) {
        if (!enabled || player == null) return VipTier.NONE;

        LuckPerms lp = plugin.getLuckPerms();
        if (lp == null) return VipTier.NONE;

        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null) return VipTier.NONE;

        VipTier highest = VipTier.NONE;

        for (var node : user.getNodes()) {
            if (node.getKey().startsWith("group.")) {
                String groupName = node.getKey().substring(6).toLowerCase();
                VipTier tier = groupToTier.get(groupName);
                if (tier != null && tier.getPriority() > highest.getPriority()) {
                    highest = tier;
                }
            }
        }

        return highest;
    }

    /**
     * Determina o VipTier de um jogador offline baseado nos seus grupos LuckPerms.
     */
    public VipTier getVipTier(UUID uuid) {
        if (!enabled || uuid == null) return VipTier.NONE;

        LuckPerms lp = plugin.getLuckPerms();
        if (lp == null) return VipTier.NONE;

        User user = lp.getUserManager().getUser(uuid);
        if (user == null) return VipTier.NONE;

        VipTier highest = VipTier.NONE;

        for (var node : user.getNodes()) {
            if (node.getKey().startsWith("group.")) {
                String groupName = node.getKey().substring(6).toLowerCase();
                VipTier tier = groupToTier.get(groupName);
                if (tier != null && tier.getPriority() > highest.getPriority()) {
                    highest = tier;
                }
            }
        }

        return highest;
    }

    /**
     * Obtém os benefícios de um tier.
     */
    public TierBenefits getBenefits(VipTier tier) {
        return tierBenefits.getOrDefault(tier, tierBenefits.get(VipTier.NONE));
    }

    /**
     * Obtém blocos extras que o tier garante.
     */
    public int getExtraClaimBlocks(VipTier tier) {
        TierBenefits b = getBenefits(tier);
        return b != null ? b.extraClaimBlocks() : 0;
    }

    /**
     * Obtém porcentagem de desconto no mercado para o tier.
     */
    public double getMarketDiscount(VipTier tier) {
        TierBenefits b = getBenefits(tier);
        return b != null ? b.marketDiscountPercent() : 0;
    }

    /**
     * Aplica benefícios VIP ao jogador ao logar.
     * Dá blocos extras apenas na primeira vez que o tier é aplicado.
     */
    public void applyVipBenefits(Player player) {
        if (!enabled) return;

        VipTier tier = getVipTier(player);
        if (tier == VipTier.NONE) return;

        UUID uuid = player.getUniqueId();
        VipTier previousTier = appliedBlocks.get(uuid);

        // Se o jogador subiu de tier, dar a diferença de blocos
        if (previousTier == null || previousTier.getPriority() < tier.getPriority()) {
            int previousBlocks = previousTier != null ? getExtraClaimBlocks(previousTier) : 0;
            int newBlocks = getExtraClaimBlocks(tier);
            int diff = newBlocks - previousBlocks;

            if (diff > 0) {
                PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
                if (pd != null) {
                    pd.addClaimBlocks(diff);
                    var msg = plugin.getMessageManager();
                    msg.send(player, "vip.blocks_applied", String.valueOf(diff), tier.getDisplayName());
                }
            }

            appliedBlocks.put(uuid, tier);
        }
    }

    /**
     * Distribui keys mensais para todos os VIPs online.
     * Deve ser chamada via task agendada ou comando admin.
     */
    public void distributeMonthlyKeys() {
        if (!enabled) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            distributeKeysToPlayer(player);
        }

        plugin.getLogger().info("[VIP] Keys mensais distribuídas para todos os VIPs online.");
    }

    /**
     * Distribui keys mensais para um jogador específico.
     */
    public void distributeKeysToPlayer(Player player) {
        VipTier tier = getVipTier(player);
        if (tier == VipTier.NONE) return;

        TierBenefits benefits = getBenefits(tier);
        if (benefits == null || benefits.monthlyKeys().isEmpty()) return;

        CrateManager crateManager = plugin.getCrateManager();
        if (crateManager == null) return;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (pd == null) return;

        var msg = plugin.getMessageManager();
        for (Map.Entry<String, Integer> entry : benefits.monthlyKeys().entrySet()) {
            pd.addCrateKey(entry.getKey(), entry.getValue());
            msg.send(player, "vip.monthly_key_received",
                    String.valueOf(entry.getValue()), entry.getKey(), tier.getDisplayName());
        }
    }

    /**
     * Verifica se hoje é o dia de distribuição mensal e executa.
     * Chamada pela task periódica.
     */
    public void checkMonthlyDistribution() {
        if (!enabled) return;

        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() == monthlyKeyDay) {
            distributeMonthlyKeys();
        }
    }

    /**
     * Define manualmente o grupo VIP de um jogador via LuckPerms.
     */
    public boolean setVipTier(UUID uuid, VipTier tier) {
        LuckPerms lp = plugin.getLuckPerms();
        if (lp == null) return false;

        // Primeiro remover todos os grupos VIP existentes
        lp.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            // Remover grupos VIP anteriores
            for (String groupName : groupToTier.keySet()) {
                user.data().remove(net.luckperms.api.node.Node.builder("group." + groupName).build());
            }

            // Adicionar novo grupo se não for NONE
            if (tier != VipTier.NONE) {
                String groupName = getGroupName(tier);
                if (groupName != null) {
                    user.data().add(net.luckperms.api.node.Node.builder("group." + groupName).value(true).build());
                }
            }

            lp.getUserManager().saveUser(user);

            // Atualizar cache
            if (tier == VipTier.NONE) {
                appliedBlocks.remove(uuid);
            }
        });

        return true;
    }

    /**
     * Obtém o nome do grupo LuckPerms para um tier.
     */
    private String getGroupName(VipTier tier) {
        for (Map.Entry<String, VipTier> entry : groupToTier.entrySet()) {
            if (entry.getValue() == tier) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Converte um nome (string) para VipTier.
     */
    public VipTier parseTier(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase().replace("-", "_").replace("+", "_plus")) {
            case "vip" -> VipTier.VIP;
            case "vip_plus", "vipplus", "vip+" -> VipTier.VIP_PLUS;
            case "elite" -> VipTier.ELITE;
            case "lendario", "lendário" -> VipTier.LENDARIO;
            case "none", "nenhum" -> VipTier.NONE;
            default -> null;
        };
    }

    /**
     * Retorna todos os tiers com seus benefícios para exibição.
     */
    public Map<VipTier, TierBenefits> getAllTierBenefits() {
        return Collections.unmodifiableMap(tierBenefits);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
