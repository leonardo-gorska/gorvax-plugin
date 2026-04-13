package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.BattlePassManager;
import br.com.gorvax.core.managers.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * B15 — Listener para ganho de XP do Battle Pass.
 * Monitora kills, mineração e login diário.
 */
public class BattlePassListener implements Listener {

    private final GorvaxCore plugin;

    /**
     * Filtro anti-exploit: apenas blocos valiosos concedem XP de mineração.
     * Inclui todos os minérios (normal + deepslate), ancient debris e obsidian.
     */
    private static final Set<Material> MINING_XP_BLOCKS = EnumSet.of(
            // Carvão
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            // Ferro
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            // Cobre
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            // Ouro
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE,
            // Diamante
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            // Esmeralda
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            // Lapis
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            // Redstone
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            // Quartzo do Nether
            Material.NETHER_QUARTZ_ORE,
            // Raros
            Material.ANCIENT_DEBRIS,
            // Utilitários
            Material.OBSIDIAN);

    public BattlePassListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * XP por matar entidades (mobs e jogadores).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        BattlePassManager bpm = plugin.getBattlePassManager();
        if (bpm == null || !bpm.isEnabled())
            return;

        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        if (event.getEntity() instanceof Player) {
            // Kill de jogador
            int xp = bpm.getXpForSource("kill_player");
            if (xp > 0) {
                bpm.addXp(killer, xp, "Kill PvP");
            }
        } else {
            // Kill de mob
            int xp = bpm.getXpForSource("kill_mob");
            if (xp > 0) {
                bpm.addXp(killer, xp, "Kill Mob");
            }
        }
    }

    /**
     * XP por minerar blocos.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        BattlePassManager bpm = plugin.getBattlePassManager();
        if (bpm == null || !bpm.isEnabled())
            return;

        // Anti-exploit: apenas minérios e blocos valiosos concedem XP
        if (!MINING_XP_BLOCKS.contains(event.getBlock().getType()))
            return;

        int xp = bpm.getXpForSource("mine_block");
        if (xp > 0) {
            bpm.addXp(event.getPlayer(), xp, "Mineração");
        }
    }

    /**
     * XP por login diário (1x por dia).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        BattlePassManager bpm = plugin.getBattlePassManager();
        if (bpm == null || !bpm.isEnabled() || !bpm.isSeasonActive())
            return;

        Player player = event.getPlayer();
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        // Verificar se já recebeu XP de login hoje (intervalo mínimo de 20h)
        long now = System.currentTimeMillis();
        long lastLoginXp = pd.getBattlePassLastLoginXp();
        long TWENTY_HOURS_MS = 20L * 60 * 60 * 1000;

        if (now - lastLoginXp >= TWENTY_HOURS_MS) {
            int xp = bpm.getXpForSource("login");
            if (xp > 0) {
                // Atrasar um pouco para que o jogador veja a mensagem
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    bpm.addXp(player, xp, "Login Diário");
                    pd.setBattlePassLastLoginXp(now);
                    plugin.getPlayerDataManager().saveData(player.getUniqueId());
                }, 60L); // 3 segundos depois do login
            }
        }
    }
}
