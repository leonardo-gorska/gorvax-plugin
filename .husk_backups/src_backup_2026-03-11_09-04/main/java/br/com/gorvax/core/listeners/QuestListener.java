package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.QuestManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * B16 — Listener para tracking automático de progresso de quests.
 * Escuta eventos de kill, mineração e login para incrementar progresso.
 */
public class QuestListener implements Listener {

    private final GorvaxCore plugin;

    public QuestListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Track KILL_MOB e KILL_PLAYER via EntityDeathEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        QuestManager qm = plugin.getQuestManager();
        if (qm == null)
            return;

        Player killer = e.getEntity().getKiller();
        if (killer == null)
            return;

        if (e.getEntityType() == EntityType.PLAYER) {
            // KILL_PLAYER
            qm.addProgress(killer.getUniqueId(), QuestManager.QuestType.KILL_PLAYER, "PLAYER", 1);
            qm.addLoreProgress(killer.getUniqueId(), QuestManager.QuestType.KILL_PLAYER, "PLAYER", 1);
        } else {
            // KILL_MOB — enviar o nome do EntityType como target
            String mobType = e.getEntityType().name();
            qm.addProgress(killer.getUniqueId(), QuestManager.QuestType.KILL_MOB, mobType, 1);
            qm.addLoreProgress(killer.getUniqueId(), QuestManager.QuestType.KILL_MOB, mobType, 1);
            // Também incrementar para quests com target "ANY"
            if (!mobType.equals("ANY")) {
                qm.addProgress(killer.getUniqueId(), QuestManager.QuestType.KILL_MOB, "ANY", 1);
                qm.addLoreProgress(killer.getUniqueId(), QuestManager.QuestType.KILL_MOB, "ANY", 1);
            }
        }
    }

    /**
     * Track MINE_BLOCK via BlockBreakEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        QuestManager qm = plugin.getQuestManager();
        if (qm == null)
            return;

        Player player = e.getPlayer();
        String blockType = e.getBlock().getType().name();

        // Mapear variantes de minério (deepslate)
        String normalizedBlock = normalizeOre(blockType);

        qm.addProgress(player.getUniqueId(), QuestManager.QuestType.MINE_BLOCK, normalizedBlock, 1);
        qm.addLoreProgress(player.getUniqueId(), QuestManager.QuestType.MINE_BLOCK, normalizedBlock, 1);
    }

    /**
     * Normaliza variantes de minério Deepslate para o tipo base.
     * Ex: DEEPSLATE_DIAMOND_ORE → DIAMOND_ORE
     */
    private String normalizeOre(String blockType) {
        if (blockType.startsWith("DEEPSLATE_") && blockType.endsWith("_ORE")) {
            return blockType.substring("DEEPSLATE_".length());
        }
        return blockType;
    }

    /**
     * Reset de quests no login e boas-vindas.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        QuestManager qm = plugin.getQuestManager();
        if (qm == null)
            return;

        Player player = e.getPlayer();
        var pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        qm.checkAndResetIfNeeded(pd);
    }
}
