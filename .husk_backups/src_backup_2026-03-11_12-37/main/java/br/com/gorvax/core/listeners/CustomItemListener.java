package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.CustomItemManager;
import br.com.gorvax.core.managers.CustomItemManager.OnHitEffect;
import br.com.gorvax.core.managers.CustomItemManager.ParticleEffect;
import br.com.gorvax.core.managers.CustomItemManager.PassiveEffect;
import br.com.gorvax.core.managers.CustomItemManager.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * B10 — Listener para efeitos de custom items.
 * - On-Hit: aplica efeitos de poção ao atacar com arma customizada (chance %).
 * - On-Equip: aplica efeitos passivos enquanto armadura customizada está
 * equipada.
 */
public class CustomItemListener implements Listener {

    private final GorvaxCore plugin;
    private final CustomItemManager customItemManager;
    private BukkitTask particleTask;

    // Cache: mapeia UUID do jogador → conjunto de IDs de custom items atualmente
    // equipados
    // Usado para detectar troca de armadura e remover/reaplicar efeitos passivos
    private final Map<UUID, Set<String>> equippedCache = new ConcurrentHashMap<>();

    public CustomItemListener(GorvaxCore plugin) {
        this.plugin = plugin;
        this.customItemManager = plugin.getCustomItemManager();
        startPassiveEffectTask();
        startItemParticleTask();
    }

    // ========== ON-HIT EFFECTS ==========

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim))
            return;

        Player attacker = resolveAttacker(event);
        if (attacker == null)
            return;

        // Verificar item na mão principal
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        String itemId = customItemManager.getCustomItemId(weapon);
        if (itemId == null)
            return;

        List<OnHitEffect> effects = customItemManager.getOnHitEffects(itemId);
        if (effects.isEmpty())
            return;

        // Aplicar cada efeito com a chance configurada
        for (OnHitEffect effect : effects) {
            if (ThreadLocalRandom.current().nextInt(100) < effect.chance()) {
                victim.addPotionEffect(new PotionEffect(
                        effect.type(),
                        effect.duration(),
                        effect.amplifier(),
                        false, // ambient
                        true, // particles
                        true // icon
                ));
            }
        }

        // Tocar som de swing/ataque
        playSounds(attacker, customItemManager.getSwingSounds(itemId));
    }

    /**
     * Resolve o atacante como Player (suporta dano direto e projéteis).
     */
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p)
            return p;
        if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player p)
                return p;
        }
        return null;
    }

    // ========== SOUND EFFECTS (EQUIP / SWING / BLOCK) ==========

    /**
     * Toca som de equip quando o jogador troca o item na hotbar para um custom
     * item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem == null)
            return;

        String itemId = customItemManager.getCustomItemId(newItem);
        if (itemId == null)
            return;

        playSounds(player, customItemManager.getEquipSounds(itemId));
    }

    /**
     * Toca som de equip quando o jogador equipa armadura via inventário.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // Delay de 1 tick para pegar o estado final do inventário
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                if (piece == null)
                    continue;
                String itemId = customItemManager.getCustomItemId(piece);
                if (itemId == null)
                    continue;
                // Só toca se o jogador acabou de equipar (verificar cache)
                Set<String> cached = equippedCache.getOrDefault(player.getUniqueId(), Collections.emptySet());
                if (!cached.contains(itemId)) {
                    playSounds(player, customItemManager.getEquipSounds(itemId));
                }
            }
        }, 1L);
    }

    /**
     * Toca som de block e aplica feedback visual quando o jogador defende com
     * um escudo custom. Inclui partículas, action bar e knockback no atacante.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageBlock(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (!player.isBlocking())
            return;

        // Verificar escudo na offhand ou mainhand
        ItemStack shield = player.getInventory().getItemInOffHand();
        String itemId = customItemManager.getCustomItemId(shield);
        if (itemId == null) {
            shield = player.getInventory().getItemInMainHand();
            itemId = customItemManager.getCustomItemId(shield);
        }
        if (itemId == null)
            return;

        // Som de bloqueio customizado
        playSounds(player, customItemManager.getBlockSounds(itemId));

        // --- Feedback visual de bloqueio ---

        // Partículas de impacto ao redor do escudo
        Location loc = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.ENCHANT, loc, 15, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.3, 0.3, 0.3, 0.05);

        // Knockback no atacante (se houver)
        if (event instanceof EntityDamageByEntityEvent dmgByEntity) {
            var damager = dmgByEntity.getDamager();
            if (damager instanceof LivingEntity attacker) {
                Vector knockback = attacker.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize()
                        .multiply(0.5)
                        .setY(0.15);
                attacker.setVelocity(attacker.getVelocity().add(knockback));
            }
        }
    }

    /**
     * Toca uma lista de efeitos sonoros para o jogador.
     * Usa playSound com SoundCategory.PLAYERS para que outros jogadores também
     * ouçam.
     */
    private void playSounds(Player player, List<SoundEffect> sounds) {
        if (sounds == null || sounds.isEmpty())
            return;
        Location loc = player.getLocation();
        for (SoundEffect se : sounds) {
            player.getWorld().playSound(loc, se.sound(), SoundCategory.PLAYERS, se.volume(), se.pitch());
        }
    }

    // ========== PASSIVE / ON-EQUIP EFFECTS ==========

    /**
     * Task periódica que verifica a armadura de todos os jogadores online
     * e aplica/remove efeitos passivos de custom items.
     */
    private void startPassiveEffectTask() {
        int interval = plugin.getConfig().getInt("custom_items.passive_effect_interval", 60);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    processPassiveEffects(player);
                }
            }
        }.runTaskTimer(plugin, 20L, interval);
    }

    /**
     * Verifica a armadura atual do jogador e aplica/remove efeitos conforme
     * necessário.
     */
    private void processPassiveEffects(Player player) {
        Set<String> currentEquipped = new HashSet<>();
        Set<PassiveEffect> effectsToApply = new HashSet<>();

        // Verificar cada slot de armadura + offhand
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        // Checar armor slots
        for (ItemStack piece : armorContents) {
            checkPieceForPassive(piece, currentEquipped, effectsToApply);
        }
        // Checar offhand (escudo)
        checkPieceForPassive(offhand, currentEquipped, effectsToApply);

        // Atualizar cache
        equippedCache.put(player.getUniqueId(), currentEquipped);

        // Aplicar efeitos passivos (duração de interval + margem para cobrir o gap)
        int duration = plugin.getConfig().getInt("custom_items.passive_effect_interval", 60) + 20;
        for (PassiveEffect effect : effectsToApply) {
            // Só aplica se o jogador não tem um efeito mais forte do mesmo tipo
            PotionEffect existing = player.getPotionEffect(effect.type());
            if (existing == null || existing.getAmplifier() <= effect.amplifier()) {
                player.addPotionEffect(new PotionEffect(
                        effect.type(),
                        duration,
                        effect.amplifier(),
                        true, // ambient (sem partículas grossas)
                        false, // particles (discreto)
                        true // icon (mostra ícone no HUD)
                ));
            }
        }
    }

    private void checkPieceForPassive(ItemStack piece, Set<String> currentEquipped, Set<PassiveEffect> effectsToApply) {
        if (piece == null)
            return;
        String itemId = customItemManager.getCustomItemId(piece);
        if (itemId == null)
            return;

        currentEquipped.add(itemId);
        List<PassiveEffect> passives = customItemManager.getPassiveEffects(itemId);
        effectsToApply.addAll(passives);
    }

    /**
     * Limpa o cache ao jogador sair (chamado pelo gerenciamento de eventos de quit
     * se necessário).
     */
    public void removeFromCache(UUID uuid) {
        equippedCache.remove(uuid);
    }

    // ========== ITEM PARTICLE EFFECTS ==========

    /**
     * Task periódica que renderiza partículas visuais de custom items
     * que o jogador está segurando na mão ou vestindo como armadura.
     */
    private void startItemParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Main hand
                    renderHandParticles(player, player.getInventory().getItemInMainHand(), 0.8);
                    // Off hand
                    renderHandParticles(player, player.getInventory().getItemInOffHand(), -0.8);
                    // Armadura (helmet, chestplate, leggings, boots)
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    double[] armorOffsetY = { 0.1, 1.0, 0.7, 1.6 }; // boots, leggings, chest, helmet
                    for (int i = 0; i < armor.length; i++) {
                        renderArmorParticles(player, armor[i], armorOffsetY[i]);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 4L); // A cada 4 ticks (~200ms)
    }

    private void renderHandParticles(Player player, ItemStack item, double sideOffset) {
        if (item == null)
            return;
        String itemId = customItemManager.getCustomItemId(item);
        if (itemId == null)
            return;

        List<ParticleEffect> effects = customItemManager.getParticleEffects(itemId);
        if (effects.isEmpty())
            return;

        // Calcular posição da mão (offset lateral baseado no yaw do jogador)
        double yawRad = Math.toRadians(player.getLocation().getYaw());
        double offsetX = -Math.sin(yawRad) * sideOffset * 0.4;
        double offsetZ = Math.cos(yawRad) * sideOffset * 0.4;
        Location loc = player.getLocation().add(offsetX, 0.8, offsetZ);

        for (ParticleEffect pe : effects) {
            player.getWorld().spawnParticle(
                    pe.particle(), loc, pe.count(),
                    pe.offsetX(), pe.offsetY(), pe.offsetZ(),
                    pe.speed());
        }
    }

    private void renderArmorParticles(Player player, ItemStack item, double heightOffset) {
        if (item == null)
            return;
        String itemId = customItemManager.getCustomItemId(item);
        if (itemId == null)
            return;

        List<ParticleEffect> effects = customItemManager.getParticleEffects(itemId);
        if (effects.isEmpty())
            return;

        Location loc = player.getLocation().add(0, heightOffset, 0);

        for (ParticleEffect pe : effects) {
            player.getWorld().spawnParticle(
                    pe.particle(), loc, pe.count(),
                    pe.offsetX(), pe.offsetY(), pe.offsetZ(),
                    pe.speed());
        }
    }

    /**
     * Desliga a task de partículas de itens (usado no onDisable).
     */
    public void shutdown() {
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
    }
}
