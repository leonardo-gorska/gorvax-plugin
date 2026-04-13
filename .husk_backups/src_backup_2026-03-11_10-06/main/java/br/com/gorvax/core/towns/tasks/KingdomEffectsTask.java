package br.com.gorvax.core.towns.tasks;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class KingdomEffectsTask extends BukkitRunnable {

    private final GorvaxCore plugin;

    public KingdomEffectsTask(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    private final java.util.Map<java.util.UUID, Long> lastBlockGive = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void run() {
        int intervalMinutes = plugin.getConfig().getInt("gameplay.blocks_gain_interval", 60);
        int amount = plugin.getConfig().getInt("gameplay.blocks_gain_amount", 100);
        long now = System.currentTimeMillis();
        long intervalMillis = intervalMinutes * 60 * 1000L;

        for (Player player : Bukkit.getOnlinePlayers()) {

            // --- SISTEMA DE GANHO DE BLOCOS ---
            if (!lastBlockGive.containsKey(player.getUniqueId())) {
                lastBlockGive.put(player.getUniqueId(), now);
            } else {
                long last = lastBlockGive.get(player.getUniqueId());
                if (now - last >= intervalMillis) {
                    final UUID uuid = player.getUniqueId();
                    plugin.getPlayerDataManager().getData(uuid).addClaimBlocks(amount);

                    // Salva de forma assíncrona para evitar lag de I/O (Diagnóstico item 1)
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        plugin.getPlayerDataManager().saveData(uuid);
                    });

                    plugin.getMessageManager().send(player, "kingdom_effects.effect_applied", amount);
                    lastBlockGive.put(uuid, now);
                }
            }

            // Conjunto de reinos que podem afetar o jogador
            java.util.Set<String> activeKingdoms = new java.util.HashSet<>();

            // ... (rest of logic)

            // 1. Reino atual (Local)
            Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
            if (claim != null && (claim.isKingdom() || claim.getKingdomName() != null)) {
                String kingdomId = claim.getId();
                if (claim.isKingdom()) {
                    activeKingdoms.add(kingdomId);
                }
            }

            // 2. Reino Natal (Extensão)
            Claim homeKingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
            if (homeKingdom != null) {
                int extLevel = plugin.getKingdomManager().getNivel(homeKingdom.getId(), "extension");
                if (extLevel > 0) {
                    double dist = getDistanceToClaim(player.getLocation(), homeKingdom);
                    // Nível 1 = 20 blocos, Nível 2 = 40 blocos, etc.
                    if (dist <= (extLevel * 20.0)) {
                        activeKingdoms.add(homeKingdom.getId());
                    }
                }
            }

            // Verifica os maiores buffs aplicáveis
            int maxSpeed = 0;

            for (String kingdomId : activeKingdoms) {
                // Checa se o jogador tem acesso aos buffs desse reino
                if (plugin.getKingdomManager().hasBuffAccess(kingdomId, player.getUniqueId())) {

                    // B5.2 — Reinos em Decadência não aplicam buffs
                    if (plugin.getKingdomManager().isDecaying(kingdomId)) {
                        continue;
                    }

                    int nivelSpeed = plugin.getKingdomManager().getNivel(kingdomId, "speed");
                    if (nivelSpeed > maxSpeed) {
                        maxSpeed = nivelSpeed;
                    }
                }
            }

            // Aplica os Efeitos
            if (maxSpeed > 0) {
                applyEffect(player, PotionEffectType.SPEED, maxSpeed - 1);
            }
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier) {
        // ambient: true, particles: false, icon: true
        player.addPotionEffect(new PotionEffect(type, 100, amplifier, true, false, true));
    }

    private double getDistanceToClaim(org.bukkit.Location loc, Claim claim) {
        if (!loc.getWorld().getName().equals(claim.getWorldName())) {
            return Double.MAX_VALUE;
        }

        double dx = 0;
        double dz = 0;

        double x = loc.getX();
        double z = loc.getZ();

        double minX = claim.getMinX();
        double maxX = claim.getMaxX() + 1; // +1 para considerar o bloco inteiro
        double minZ = claim.getMinZ();
        double maxZ = claim.getMaxZ() + 1;

        if (x < minX)
            dx = minX - x;
        else if (x > maxX)
            dx = x - maxX;

        if (z < minZ)
            dz = minZ - z;
        else if (z > maxZ)
            dz = z - maxZ;

        return Math.sqrt(dx * dx + dz * dz);
    }
}