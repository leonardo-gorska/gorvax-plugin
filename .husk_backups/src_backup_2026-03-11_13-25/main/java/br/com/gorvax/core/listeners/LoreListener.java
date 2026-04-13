package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.LoreManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener para interações de lore: estantes de livros e totems de bioma.
 * Ao clicar com botão direito em blocos registrados nas coordenadas do
 * lore_books.yml, entrega livros ou exibe mensagens de lore.
 */
public class LoreListener implements Listener {

    private final GorvaxCore plugin;
    /** Cooldown por jogador por totem (totemId → timestamp) */
    private final Map<UUID, Map<String, Long>> totemCooldowns = new ConcurrentHashMap<>();
    private static final long TOTEM_COOLDOWN_MS = 60_000L; // 60 segundos

    public LoreListener(GorvaxCore plugin) {
        this.plugin = plugin;
        startParticleTask();
    }

    /**
     * Task repetitiva que spawna partículas em estantes e totems de lore
     * para indicar visualmente que são interativos.
     */
    private void startParticleTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            LoreManager loreManager = plugin.getLoreManager();
            if (loreManager == null)
                return;

            // Partículas em estantes de lore (ENCHANT)
            for (Location loc : loreManager.getBookshelfLocations()) {
                if (loc.getWorld() == null)
                    continue;
                if (!loc.isWorldLoaded())
                    continue;
                // Só spawnar se há jogadores por perto (otimização)
                boolean hasNearby = loc.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(loc) < 2500); // 50 blocos
                if (hasNearby) {
                    loc.getWorld().spawnParticle(Particle.ENCHANT,
                            loc.clone().add(0.5, 1.2, 0.5), 5, 0.3, 0.2, 0.3, 0.5);
                }
            }

            // Partículas em totems de lore (PORTAL)
            for (Location loc : loreManager.getTotemLocations()) {
                if (loc.getWorld() == null)
                    continue;
                if (!loc.isWorldLoaded())
                    continue;
                boolean hasNearby = loc.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(loc) < 2500);
                if (hasNearby) {
                    loc.getWorld().spawnParticle(Particle.PORTAL,
                            loc.clone().add(0.5, 1.5, 0.5), 8, 0.3, 0.3, 0.3, 0.3);
                }
            }
        }, 40L, 40L); // A cada 2 segundos
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Player player = event.getPlayer();
        Location loc = block.getLocation();
        LoreManager loreManager = plugin.getLoreManager();

        // === ESTANTES DE LORE ===
        if (block.getType() == Material.CHISELED_BOOKSHELF || block.getType() == Material.BOOKSHELF) {
            if (loreManager.isLoreBookshelf(loc)) {
                event.setCancelled(true);

                String bookId = loreManager.getBookIdAt(loc);
                if (bookId == null)
                    return;

                // Verificar se o jogador já tem esse livro
                if (loreManager.playerHasBook(player, bookId)) {
                    player.sendMessage(plugin.getMessageManager().get("lore.already_has_book"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }

                // Criar e entregar o livro
                ItemStack book = loreManager.createBook(bookId);
                if (book == null) {
                    plugin.getLogger().warning("[LoreManager] Livro '" + bookId + "' não encontrado.");
                    return;
                }

                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(plugin.getMessageManager().get("lore.inventory_full"));
                    return;
                }

                player.getInventory().addItem(book);
                player.sendMessage(plugin.getMessageManager().get("lore.book_received"));

                // Efeitos visuais e sonoros
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.5f);
                player.spawnParticle(Particle.ENCHANT, loc.clone().add(0.5, 1.5, 0.5),
                        30, 0.5, 0.5, 0.5, 0.5);
                return;
            }
        }

        // === TOTEMS DE BIOMA ===
        LoreManager.TotemData totem = loreManager.getTotemAt(loc);
        if (totem != null && block.getType() == totem.block()) {
            event.setCancelled(true);

            // Verificar cooldown
            UUID uuid = player.getUniqueId();
            Map<String, Long> playerCooldowns = totemCooldowns.computeIfAbsent(uuid,
                    k -> new ConcurrentHashMap<>());
            Long lastUse = playerCooldowns.get(totem.id());
            long now = System.currentTimeMillis();

            if (lastUse != null && now - lastUse < TOTEM_COOLDOWN_MS) {
                long remaining = (TOTEM_COOLDOWN_MS - (now - lastUse)) / 1000;
                player.sendMessage(plugin.getMessageManager().get("lore.totem_cooldown",
                        remaining));
                return;
            }

            // Enviar lore ao jogador
            for (String line : totem.lore()) {
                player.sendMessage(line);
            }

            // Cooldown e efeitos
            playerCooldowns.put(totem.id(), now);

            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
            player.playSound(player.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.5f, 0.8f);
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0.5, 1.5, 0.5),
                    40, 0.5, 1.0, 0.5, 0.02);
            player.spawnParticle(Particle.ENCHANT, loc.clone().add(0.5, 2.0, 0.5),
                    25, 0.3, 0.5, 0.3, 0.5);
        }
    }
}
