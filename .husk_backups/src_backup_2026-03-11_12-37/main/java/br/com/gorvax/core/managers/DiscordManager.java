package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * B8 — Manager central da integração Discord via Webhook.
 * Gerencia configuração, formatação e envio de mensagens e alertas ao Discord.
 * Ponte unidirecional: Minecraft → Discord.
 */
public class DiscordManager {

    private final GorvaxCore plugin;

    private boolean enabled;
    private boolean chatSync;
    private boolean alertBossSpawn;
    private boolean alertWarDeclare;
    private boolean alertRaidStart;
    private boolean alertKillStreak;
    private boolean alertAchievementRare;
    private int killStreakThreshold;

    private DiscordWebhook webhook;

    public DiscordManager(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa o manager lendo as configurações de discord do config.yml.
     */
    public void init() {
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("discord.enabled", false);
        String url = config.getString("discord.webhook_url", "");
        this.chatSync = config.getBoolean("discord.chat_sync", true);
        this.alertBossSpawn = config.getBoolean("discord.alerts.boss_spawn", true);
        this.alertWarDeclare = config.getBoolean("discord.alerts.war_declare", true);
        this.alertRaidStart = config.getBoolean("discord.alerts.raid_start", true);
        this.alertKillStreak = config.getBoolean("discord.alerts.killstreak", true);
        this.alertAchievementRare = config.getBoolean("discord.alerts.achievement_rare", true);
        this.killStreakThreshold = config.getInt("discord.killstreak_threshold", 5);

        if (!enabled) {
            plugin.getLogger().info(plugin.getMessageManager().get("discord.disabled"));
            return;
        }

        if (url == null || url.isEmpty()) {
            plugin.getLogger().warning(plugin.getMessageManager().get("discord.error_no_url"));
            this.enabled = false;
            return;
        }

        this.webhook = new DiscordWebhook(url, plugin.getLogger());
        plugin.getLogger().info(plugin.getMessageManager().get("discord.enabled"));
    }

    /**
     * Verifica se a integração Discord está ativa.
     */
    public boolean isEnabled() {
        return enabled && webhook != null;
    }

    // ========================================================================
    // Chat Sync (MC → Discord)
    // ========================================================================

    /**
     * Envia uma mensagem do chat global do Minecraft para o Discord.
     *
     * @param playerName nome do jogador
     * @param kingdomTag tag do reino (pode ser vazio)
     * @param message    conteúdo da mensagem
     */
    public void sendChatMessage(String playerName, String kingdomTag, String message) {
        if (!isEnabled() || !chatSync) return;

        String tag = (kingdomTag != null && !kingdomTag.isEmpty()) ? kingdomTag : "Sem Reino";
        String formatted = plugin.getMessageManager().get("discord.chat_format", tag, playerName, message);
        // Remove códigos de cor do Minecraft (§x)
        formatted = stripMinecraftColors(formatted);

        webhook.send(formatted);
    }

    // ========================================================================
    // Alertas automáticos
    // ========================================================================

    /**
     * Alerta de spawn de World Boss.
     */
    public void sendBossSpawnAlert(String bossName, String world, int hp) {
        if (!isEnabled() || !alertBossSpawn) return;

        String msg = plugin.getMessageManager().get("discord.boss_spawn", bossName, world, String.valueOf(hp));
        msg = stripMinecraftColors(msg);

        webhook.sendEmbed("👹 Boss Spawn!", msg, 0xFF5555); // Vermelho
    }

    /**
     * Alerta de boss derrotado.
     */
    public void sendBossDeathAlert(String bossName) {
        if (!isEnabled() || !alertBossSpawn) return;

        String msg = plugin.getMessageManager().get("discord.boss_death", bossName);
        msg = stripMinecraftColors(msg);

        webhook.sendEmbed("💀 Boss Derrotado!", msg, 0x55FF55); // Verde
    }

    /**
     * Alerta de declaração de guerra entre reinos.
     */
    public void sendWarDeclaredAlert(String attackerKingdom, String defenderKingdom) {
        if (!isEnabled() || !alertWarDeclare) return;

        String msg = plugin.getMessageManager().get("discord.war_declared", attackerKingdom, defenderKingdom);
        msg = stripMinecraftColors(msg);

        webhook.sendEmbed("⚔️ Guerra Declarada!", msg, 0xAA0000); // Vermelho escuro
    }

    /**
     * Alerta de raid iniciada.
     */
    public void sendRaidStartAlert(int totalWaves, int minPlayers) {
        if (!isEnabled() || !alertRaidStart) return;

        String msg = plugin.getMessageManager().get("discord.raid_started",
                String.valueOf(totalWaves), String.valueOf(minPlayers));
        msg = stripMinecraftColors(msg);

        webhook.sendEmbed("💀 Boss Raid!", msg, 0xAA00AA); // Roxo
    }

    /**
     * Alerta de kill streak.
     */
    public void sendKillStreakAlert(String playerName, int streak) {
        if (!isEnabled() || !alertKillStreak) return;
        if (streak < killStreakThreshold) return;

        String msg = plugin.getMessageManager().get("discord.killstreak", playerName, String.valueOf(streak));
        msg = stripMinecraftColors(msg);

        webhook.sendEmbed("🔥 Kill Streak!", msg, 0xFFAA00); // Laranja
    }

    /**
     * Alerta de conquista rara desbloqueada.
     */
    public void sendAchievementAlert(String playerName, String achievementName) {
        if (!isEnabled() || !alertAchievementRare) return;

        String msg = plugin.getMessageManager().get("discord.achievement", playerName, achievementName);
        msg = stripMinecraftColors(msg);

        webhook.sendEmbed("🏆 Conquista!", msg, 0x55FFFF); // Ciano
    }

    // ========================================================================
    // Utilitário
    // ========================================================================

    /**
     * Remove códigos de cor do Minecraft (§x) de uma string.
     */
    private String stripMinecraftColors(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
