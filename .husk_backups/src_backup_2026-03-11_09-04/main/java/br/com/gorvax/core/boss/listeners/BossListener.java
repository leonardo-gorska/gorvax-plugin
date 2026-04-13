package br.com.gorvax.core.boss.listeners;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import br.com.gorvax.core.boss.model.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Warden;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BossListener implements Listener {

    private final GorvaxCore plugin;

    // Cooldown para Easter Egg de sinergia (evita spam)
    private final Map<UUID, Long> synergyCooldown = new ConcurrentHashMap<>();

    // Palavras-chave para detectar itens de cada boss
    private static final String[] GORVAX_KEYWORDS = {
            "Juramento Real", "Executor da Aliança", "Sentença Soberana",
            "Aliança Eterna", "Pacto Real", "Soberano", "Conquistador", "Trono Imortal",
            "Cetro Minerador", "Machado do Mandato", "IMPERIAL"
    };
    private static final String[] INDRAX_KEYWORDS = {
            "Horizonte Sombrio", "Conselheiro", "Eclipse Abissal",
            "Pacto Sombrio", "Vigília", "entre Mundos", "Vazio Protetor",
            "Abismo Profundo", "Sentença Sombria", "ABISSAL"
    };

    public BossListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        // === Dano RECEBIDO pelo boss ===
        if (plugin.getBossManager().getActiveBosses().containsKey(e.getEntity().getUniqueId())) {
            WorldBoss boss = plugin.getBossManager().getActiveBosses().get(e.getEntity().getUniqueId());
            if (boss == null)
                return;
            boss.updateLastDamage();

            if (e.getDamager() instanceof Player p) {
                double damage = Math.max(0, e.getFinalDamage());
                boss.getDamageDealt().merge(p.getUniqueId(), damage, Double::sum);

                // Atualização instantânea da BossBar
                boss.updateBossBar();

                // === DECRETO REAL - Reflexão de dano ===
                if (boss instanceof KingGorvax) {
                    UUID decreeTarget = boss.getRoyalDecreeTarget();
                    long decreeUntil = boss.getRoyalDecreeUntil();

                    if (decreeTarget != null && decreeTarget.equals(p.getUniqueId())
                            && System.currentTimeMillis() < decreeUntil) {
                        double reflectPercent = plugin.getBossManager().getConfigManager().getSettings()
                                .getDouble("bosses.rei_gorvax.skills.royal_decree.reflect_percent", 0.5);
                        double reflectedDamage = e.getFinalDamage() * reflectPercent;

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (p.isOnline() && !p.isDead()) {
                                p.damage(reflectedDamage);
                                p.sendMessage(plugin.getMessageManager().get("boss_listener.gorvax_reflect"));
                                p.spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3,
                                        new Particle.DustOptions(Color.RED, 1.5f));
                                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.2f);
                            }
                        }, 1L);
                    }
                }

                // === EASTER EGG: Sinergia de Lore ===
                checkAllianceSynergy(p);
            }
        }

        // === Dano CAUSADO pelo Indrax (Warden) ===
        if (e.getDamager() instanceof Warden warden) {
            if (plugin.getBossManager().getActiveBosses().containsKey(warden.getUniqueId())) {
                WorldBoss boss = plugin.getBossManager().getActiveBosses().get(warden.getUniqueId());

                if (boss.getId().equals("indrax")) {
                    if (e.getEntity().hasMetadata("indrax_minion")) {
                        e.setCancelled(true);
                        return;
                    }

                    if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                        e.setDamage(8.0);
                        if (e.getEntity() instanceof Player p) {
                            p.sendMessage(plugin.getMessageManager().get("boss_dialogue.indrax_damage"));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (plugin.getBossManager().getActiveBosses().containsKey(e.getEntity().getUniqueId())) {
            WorldBoss boss = plugin.getBossManager().getActiveBosses().get(e.getEntity().getUniqueId());
            if (boss == null)
                return;

            e.getDrops().clear();
            e.setDroppedExp(0); // Previne XP vanilla

            // === GRITOS DE GUERRA NA MORTE (Aliança) ===
            try {
                broadcastDeathGreeting(boss);
            } catch (Exception ex) {
                plugin.getLogger().warning("[BossListener] Erro ao exibir grito de morte: " + ex.getMessage());
            }

            try {
                plugin.getBossManager().rewardPlayers(boss);
            } catch (Exception ex) {
                plugin.getLogger().severe("[BossListener] Erro ao distribuir recompensas: " + ex.getMessage());
            }

            // B11 — Notifica BossRaidManager sobre morte (avanço de onda)
            try {
                if (plugin.getBossManager().getRaidManager().isRaidActive() && boss.getEntity() != null) {
                    plugin.getBossManager().getRaidManager().onBossDeath(boss.getEntity().getUniqueId());
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("[BossListener] Erro ao notificar raid: " + ex.getMessage());
            }

            // Limpeza completa e centralizada via BossManager
            plugin.getBossManager().removeBoss(boss);

            // BUG-09 FIX: Limpar cooldowns de sinergia 5 minutos após boss morrer
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                long now = System.currentTimeMillis();
                synergyCooldown.entrySet().removeIf(entry -> now - entry.getValue() > 300_000L); // 5 min
            }, 6000L); // 5 min = 6000 ticks
        }
    }

    /**
     * Listener de segurança para capturar remoção de entidades via comandos ou
     * despawn (Paper API).
     */
    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent e) {
        if (plugin.getBossManager().getActiveBosses().containsKey(e.getEntity().getUniqueId())) {
            plugin.getBossManager().removeBossById(e.getEntity().getUniqueId());
        }
    }

    /**
     * Gritos de guerra na morte. Um boss saúda o aliado ao cair.
     */
    private void broadcastDeathGreeting(WorldBoss boss) {
        if (boss instanceof KingGorvax) {
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_dialogue.gorvax_death_1");
            plugin.getMessageManager().broadcast("boss_dialogue.gorvax_death_2");
            Bukkit.broadcast(Component.text(" "));

            // Efeito sonoro solene
            if (boss.getEntity() != null) {
                boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                        Sound.ENTITY_WITHER_DEATH, 2.0f, 0.3f);
                boss.getEntity().getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        boss.getEntity().getLocation().add(0, 1.5, 0), 50, 1, 2, 1, 0.05);
            }
        } else if (boss instanceof IndraxAbissal) {
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_dialogue.indrax_death_1");
            plugin.getMessageManager().broadcast("boss_dialogue.indrax_death_2");
            Bukkit.broadcast(Component.text(" "));

            // Efeito sonoro abissal
            if (boss.getEntity() != null) {
                boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                        Sound.ENTITY_WARDEN_DEATH, 2.0f, 0.3f);
                boss.getEntity().getWorld().spawnParticle(Particle.SOUL,
                        boss.getEntity().getLocation().add(0, 1.5, 0), 50, 1, 2, 1, 0.05);
            }
        } else if (boss instanceof Zarith) {
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_dialogue.zarith_death_1");
            plugin.getMessageManager().broadcast("boss_dialogue.zarith_death_2");
            Bukkit.broadcast(Component.text(" "));
            if (boss.getEntity() != null) {
                boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                        Sound.ENTITY_SPIDER_DEATH, 2.0f, 0.5f);
                boss.getEntity().getWorld().spawnParticle(Particle.ITEM_SLIME,
                        boss.getEntity().getLocation().add(0, 1.5, 0), 40, 1, 2, 1, 0.05);
            }
        } else if (boss instanceof Kaldur) {
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_dialogue.kaldur_death_1");
            plugin.getMessageManager().broadcast("boss_dialogue.kaldur_death_2");
            Bukkit.broadcast(Component.text(" "));
            if (boss.getEntity() != null) {
                boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                        Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                boss.getEntity().getWorld().spawnParticle(Particle.SNOWFLAKE,
                        boss.getEntity().getLocation().add(0, 1.5, 0), 60, 2, 3, 2, 0.1);
            }
        } else if (boss instanceof Skulkor) {
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_dialogue.skulkor_death_1");
            plugin.getMessageManager().broadcast("boss_dialogue.skulkor_death_2");
            Bukkit.broadcast(Component.text(" "));
            if (boss.getEntity() != null) {
                boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                        Sound.ENTITY_SKELETON_DEATH, 2.0f, 0.3f);
                boss.getEntity().getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        boss.getEntity().getLocation().add(0, 1.5, 0), 40, 1, 2, 1, 0.05);
            }
        } else if (boss instanceof XylosDevorador) {
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_dialogue.xylos_death_1");
            plugin.getMessageManager().broadcast("boss_dialogue.xylos_death_2");
            Bukkit.broadcast(Component.text(" "));
            if (boss.getEntity() != null) {
                boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                        Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.3f);
                boss.getEntity().getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        boss.getEntity().getLocation().add(0, 1.5, 0), 80, 2, 3, 2, 0.5);
            }
        } else if (boss instanceof Vulgathor) {
            Bukkit.broadcast(Component.text(" "));
            plugin.getMessageManager().broadcast("boss_dialogue.vulgathor_death_1");
            plugin.getMessageManager().broadcast("boss_dialogue.vulgathor_death_2");
            Bukkit.broadcast(Component.text(" "));
            if (boss.getEntity() != null) {
                boss.getEntity().getWorld().playSound(boss.getEntity().getLocation(),
                        Sound.ENTITY_BLAZE_DEATH, 2.0f, 0.3f);
                boss.getEntity().getWorld().spawnParticle(Particle.FLAME,
                        boss.getEntity().getLocation().add(0, 1.5, 0), 80, 2, 3, 2, 0.1);
                boss.getEntity().getWorld().spawnParticle(Particle.LAVA,
                        boss.getEntity().getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.05);
            }
        }
    }

    /**
     * Easter Egg de Sinergia: Se o jogador tem um item do Gorvax E um do Indrax
     * no inventário, exibe uma mensagem sutil. Cooldown de 60s por jogador.
     */
    private void checkAllianceSynergy(Player p) {
        long now = System.currentTimeMillis();
        Long lastCheck = synergyCooldown.get(p.getUniqueId());
        if (lastCheck != null && now - lastCheck < 60000)
            return; // 60s cooldown

        boolean hasGorvax = false;
        boolean hasIndrax = false;

        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta())
                continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                continue;

            String displayName = meta.hasDisplayName()
                    ? LegacyComponentSerializer.legacySection().serialize(meta.displayName())
                    : "";
            List<String> lore = meta.hasLore()
                    ? meta.lore().stream().map(c -> LegacyComponentSerializer.legacySection().serialize(c)).toList()
                    : null;

            // Check display name and lore for keywords
            String combined = displayName;
            if (lore != null) {
                combined += " " + String.join(" ", lore);
            }

            if (!hasGorvax) {
                for (String keyword : GORVAX_KEYWORDS) {
                    if (combined.contains(keyword)) {
                        hasGorvax = true;
                        break;
                    }
                }
            }
            if (!hasIndrax) {
                for (String keyword : INDRAX_KEYWORDS) {
                    if (combined.contains(keyword)) {
                        hasIndrax = true;
                        break;
                    }
                }
            }

            if (hasGorvax && hasIndrax)
                break; // Encontrou ambos, pode sair cedo
        }

        if (hasGorvax && hasIndrax) {
            synergyCooldown.put(p.getUniqueId(), now);
            p.sendMessage(plugin.getMessageManager().get("boss_listener.lore_crown_shadow"));
            p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.5f);
            p.spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.3);
        }
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("boss_loot_chest")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessageManager().get("boss_listener.treasure_no_break"));
        }
    }

    @EventHandler
    public void onLootOpen(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType() == Material.CHEST) {
                boolean isLootChest = e.getClickedBlock().hasMetadata("boss_loot_chest");

                // Fallback: Check PersistentDataContainer
                if (!isLootChest && e.getClickedBlock().getState() instanceof org.bukkit.block.Chest chest) {
                    isLootChest = chest.getPersistentDataContainer().has(
                            new org.bukkit.NamespacedKey(plugin, "boss_loot_chest"),
                            org.bukkit.persistence.PersistentDataType.BYTE);
                }

                if (isLootChest) {
                    Player p = e.getPlayer();
                    e.setCancelled(true);

                    // DEBUG LOG
                    plugin.getLogger().info("Player " + p.getName() + " opened Loot Chest at " +
                            e.getClickedBlock().getLocation().toString());

                    if (plugin.getBossManager().getLootManager().hasLoot(p)) {
                        plugin.getBossManager().getLootManager().openPersonalLoot(p);
                    } else {
                        p.sendMessage(plugin.getMessageManager().get("boss_listener.treasure_no_participation"));
                        p.sendMessage(plugin.getMessageManager().get("boss_listener.treasure_tip_damage"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onLootClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player))
            return;
        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (title.startsWith("§8Recompensa")) {
            // Prevent putting items IN
            if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) {
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                }
                return;
            }

            if (e.getAction().name().contains("PLACE") || e.getAction().name().contains("DROP")
                    || e.getAction().name().contains("SWAP")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLootDrag(InventoryDragEvent e) {
        if (LegacyComponentSerializer.legacySection().serialize(e.getView().title()).startsWith("§8Recompensa")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLootClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (LegacyComponentSerializer.legacySection().serialize(e.getView().title()).startsWith("§8Recompensa")) {
            // Verificar se ainda há itens no baú
            Inventory inv = e.getInventory();
            boolean hasItems = false;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    hasItems = true;
                    break;
                }
            }

            if (hasItems) {
                long remaining = plugin.getBossManager().getLootManager().getRemainingSeconds(p.getUniqueId());
                long min = remaining / 60;
                long sec = remaining % 60;
                p.sendMessage(plugin.getMessageManager().get("boss_listener.treasure_items_remain", min, sec));
                p.sendMessage(plugin.getMessageManager().get("boss_listener.treasure_reopen_hint"));
            } else {
                // Jogador pegou tudo, limpar
                plugin.getBossManager().getLootManager().removeLoot(p.getUniqueId());
                p.sendMessage(plugin.getMessageManager().get("boss_listener.treasure_all_collected"));
            }
        }
    }

}