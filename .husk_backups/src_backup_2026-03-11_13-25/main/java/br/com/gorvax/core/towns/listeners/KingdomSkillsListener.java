package br.com.gorvax.core.towns.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class KingdomSkillsListener implements Listener {

    private final GorvaxCore plugin;
    private final Random random = new Random();

    public KingdomSkillsListener(GorvaxCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // --- SORTE DO TRABALHADOR (Itens em dobro) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWork(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // GorvaxCore ClaimManager
        Claim claim = plugin.getClaimManager().getClaimAt(block.getLocation());
        if (claim == null)
            return;

        String kingdomId = claim.getId();

        if (plugin.getKingdomManager().getNome(kingdomId) == null)
            return;

        // Verifica acesso aos buffs (Rei ou Súdito)
        if (!plugin.getKingdomManager().hasBuffAccess(kingdomId, player.getUniqueId()))
            return;

        int nivelSorte = plugin.getKingdomManager().getNivelSorte(kingdomId);
        if (nivelSorte > 0) {
            double chance = nivelSorte * 5.0;

            if (random.nextDouble() * 100 <= chance) {
                for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand())) {
                    if (drop == null || drop.getType() == Material.AIR)
                        continue;
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }

                player.sendActionBar(LegacyComponentSerializer.legacySection()
                        .deserialize("§6§l⭐ SORTE! §fItens duplicados pelo reino."));
            }
        }
    }

    // --- PRESERVAÇÃO DE ALMA (Redução de perda de XP) ---
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (claim == null)
            return;

        String kingdomId = claim.getId();

        if (plugin.getKingdomManager().getNome(kingdomId) == null)
            return;

        if (!plugin.getKingdomManager().hasBuffAccess(kingdomId, player.getUniqueId()))
            return;

        int nivelPres = plugin.getKingdomManager().getNivelPreservacao(kingdomId);
        if (nivelPres > 0) {
            double chance = nivelPres * 10.0; // 10% por nível (Máx 100% no nível 10)

            if (random.nextDouble() * 100 <= chance) {
                // Mantém XP e Itens
                event.setKeepLevel(true);
                event.setKeepInventory(true);
                event.setDroppedExp(0);
                event.getDrops().clear();

                player.sendMessage(
                        "§d§lALMA PRESERVADA! §7Suas preces foram ouvidas e seus bens protegidos pelo reino. ("
                                + (int) chance + "% de chance)");
            }
        }
    }
}