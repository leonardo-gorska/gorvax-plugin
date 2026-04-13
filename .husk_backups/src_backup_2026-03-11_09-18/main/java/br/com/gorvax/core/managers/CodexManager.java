package br.com.gorvax.core.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import br.com.gorvax.core.GorvaxCore;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * B28 — Códex de Gorvax (Enciclopédia Interativa).
 * Gerencia categorias, entradas e desbloqueios do Códex.
 */
public class CodexManager {

    private final GorvaxCore plugin;
    private FileConfiguration codexConfig;

    // Dados parseados do codex.yml
    private final Map<String, CodexCategory> categories = new LinkedHashMap<>();

    // Cache: chave completa "categoria.entryId" → CodexEntry
    private final Map<String, CodexEntry> allEntries = new ConcurrentHashMap<>();

    // Mapa reverso: trigger → lista de chaves "categoria.entryId"
    private final Map<String, List<String>> triggerIndex = new ConcurrentHashMap<>();

    public CodexManager(GorvaxCore plugin) {
        this.plugin = plugin;
        reload();
    }

    // ==================== Reload ====================

    public void reload() {
        categories.clear();
        allEntries.clear();
        triggerIndex.clear();

        File file = new File(plugin.getDataFolder(), "codex.yml");
        if (!file.exists()) {
            plugin.saveResource("codex.yml", false);
        }
        codexConfig = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection catSection = codexConfig.getConfigurationSection("categorias");
        if (catSection == null) {
            plugin.getLogger().warning("[Códex] Seção 'categorias' não encontrada em codex.yml");
            return;
        }

        for (String catId : catSection.getKeys(false)) {
            ConfigurationSection cs = catSection.getConfigurationSection(catId);
            if (cs == null)
                continue;

            String nome = cs.getString("nome", catId);
            Material icone = parseMaterial(cs.getString("icone", "BOOK"));
            String descricao = cs.getString("descricao", "");

            Map<String, CodexEntry> entries = new LinkedHashMap<>();
            ConfigurationSection entrySection = cs.getConfigurationSection("entradas");
            if (entrySection != null) {
                for (String entryId : entrySection.getKeys(false)) {
                    ConfigurationSection es = entrySection.getConfigurationSection(entryId);
                    if (es == null)
                        continue;

                    String entryNome = es.getString("nome", entryId);
                    Material entryIcone = parseMaterial(es.getString("icone", "PAPER"));
                    List<String> loreBloqueado = es.getStringList("lore_bloqueado");
                    List<String> loreDesbloqueado = es.getStringList("lore_desbloqueado");

                    String tipo = "";
                    String trigger = "";
                    ConfigurationSection desbloqueio = es.getConfigurationSection("desbloqueio");
                    if (desbloqueio != null) {
                        tipo = desbloqueio.getString("tipo", "");
                        trigger = desbloqueio.getString("trigger", "");
                    }

                    CodexEntry entry = new CodexEntry(entryId, entryNome, entryIcone,
                            loreBloqueado, loreDesbloqueado, tipo, trigger);
                    entries.put(entryId, entry);

                    String fullKey = catId + "." + entryId;
                    allEntries.put(fullKey, entry);

                    // Indexar por trigger
                    triggerIndex.computeIfAbsent(trigger, k -> new ArrayList<>()).add(fullKey);
                }
            }

            categories.put(catId, new CodexCategory(catId, nome, icone, descricao, entries));
        }

        plugin.getLogger().info("[Códex] Carregadas " + categories.size() + " categorias, "
                + allEntries.size() + " entradas.");
    }

    // ==================== Desbloqueio ====================

    /**
     * Tenta desbloquear uma entrada do Códex para o jogador.
     * 
     * @return true se desbloqueou (era novo), false se já tinha
     */
    public boolean tryUnlock(Player player, String fullKey) {
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (pd == null)
            return false;
        if (pd.hasCodexEntry(fullKey))
            return false;

        CodexEntry entry = allEntries.get(fullKey);
        if (entry == null)
            return false;

        pd.unlockCodexEntry(fullKey);

        // Notificação visual
        String msg = plugin.getMessageManager().get("codex.unlocked")
                .replace("{entry}", entry.nome())
                .replace("{category}", getCategoryNameForKey(fullKey));

        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        return true;
    }

    /**
     * Tenta desbloquear todas as entradas com o trigger fornecido.
     */
    public void tryUnlockByTrigger(Player player, String triggerType, String triggerValue) {
        List<String> keys = triggerIndex.get(triggerValue);
        if (keys == null)
            return;

        for (String fullKey : keys) {
            CodexEntry entry = allEntries.get(fullKey);
            if (entry != null && entry.tipo().equalsIgnoreCase(triggerType)) {
                tryUnlock(player, fullKey);
            }
        }
    }

    // ==================== Progresso ====================

    /**
     * Retorna o progresso geral do jogador: [desbloqueados, total].
     */
    public int[] getProgress(UUID uuid) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        if (pd == null)
            return new int[] { 0, allEntries.size() };
        int unlocked = (int) pd.getUnlockedCodex().stream()
                .filter(allEntries::containsKey)
                .count();
        return new int[] { unlocked, allEntries.size() };
    }

    /**
     * Retorna o progresso do jogador para uma categoria específica.
     */
    public int[] getCategoryProgress(UUID uuid, String categoryId) {
        CodexCategory cat = categories.get(categoryId);
        if (cat == null)
            return new int[] { 0, 0 };

        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        if (pd == null)
            return new int[] { 0, cat.entries().size() };

        int unlocked = 0;
        for (String entryId : cat.entries().keySet()) {
            if (pd.hasCodexEntry(categoryId + "." + entryId)) {
                unlocked++;
            }
        }
        return new int[] { unlocked, cat.entries().size() };
    }

    // ==================== Acesso público ====================

    public Map<String, CodexCategory> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public CodexEntry getEntry(String fullKey) {
        return allEntries.get(fullKey);
    }

    public int getTotalEntries() {
        return allEntries.size();
    }

    /**
     * Retorna o nome da categoria a partir de uma chave completa "cat.entry".
     */
    private String getCategoryNameForKey(String fullKey) {
        String catId = fullKey.contains(".") ? fullKey.substring(0, fullKey.indexOf('.')) : fullKey;
        CodexCategory cat = categories.get(catId);
        return cat != null ? cat.nome() : catId;
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Material.PAPER;
        }
    }

    // ==================== Records ====================

    public record CodexCategory(
            String id,
            String nome,
            Material icone,
            String descricao,
            Map<String, CodexEntry> entries) {
    }

    public record CodexEntry(
            String id,
            String nome,
            Material icone,
            List<String> loreBloqueado,
            List<String> loreDesbloqueado,
            String tipo,
            String trigger) {
    }
}
