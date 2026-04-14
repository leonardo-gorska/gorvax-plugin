package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B12 — GUI para abertura de crates com animação de roleta
 * e preview dos rewards possíveis de cada tipo.
 */
public class CrateGUI implements Listener {

    private final GorvaxCore plugin;

    // Rastreia jogadores em animação para bloquear cliques e fechar
    private final Set<UUID> animatingPlayers = ConcurrentHashMap.newKeySet();

    public CrateGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // --- Holder para identificar inventários de crate ---

    /** Holder para a GUI de animação de abertura. */
    public static class CrateOpenHolder implements InventoryHolder {
        private final String crateId;

        public CrateOpenHolder(String crateId) {
            this.crateId = crateId;
        }

        public String getCrateId() {
            return crateId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** Holder para a GUI de preview. */
    public static class CratePreviewHolder implements InventoryHolder {
        private final String crateId;

        public CratePreviewHolder(String crateId) {
            this.crateId = crateId;
        }

        public String getCrateId() {
            return crateId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** Holder para a GUI de seleção de crate. */
    public static class CrateSelectHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    // --- Abertura com animação ---

    /**
     * Abre a GUI de animação de roleta para o jogador.
     * Consome a key e faz a animação antes de revelar o reward.
     */
    public void openWithAnimation(Player player, String crateId) {
        CrateManager crateManager = plugin.getCrateManager();
        var msg = plugin.getMessageManager();
        CrateManager.CrateType crate = crateManager.getCrateType(crateId);
        if (crate == null) {
            msg.send(player, "crate.invalid_type");
            return;
        }

        int keys = crateManager.getKeyCount(player, crateId);
        if (keys <= 0) {
            msg.send(player, "crate.no_keys", crate.name());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consumir key antes da animação
        crateManager.removeKey(player, crateId);

        // Sortear reward agora (antes da animação)
        CrateManager.CrateReward finalReward = crateManager.getRandomReward(crateId);
        if (finalReward == null) {
            msg.send(player, "crate.error");
            return;
        }

        // Criar inventário de 27 slots
        Component title = LegacyComponentSerializer.legacySection().deserialize(crate.name());
        Inventory inv = Bukkit.createInventory(new CrateOpenHolder(crateId), 27, title);

        // Preencher bordas com vidro
        ItemStack border = createGlassPane(crate.color());
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        player.openInventory(inv);
        animatingPlayers.add(player.getUniqueId());

        // Animação de roleta: troca itens nos slots 10-16 (linha central)
        startAnimation(player, inv, crate, finalReward);
    }

    /**
     * Inicia a animação de roleta que desacelera progressivamente.
     */
    private void startAnimation(Player player, Inventory inv, CrateManager.CrateType crate,
            CrateManager.CrateReward finalReward) {
        // Fases da animação: velocidade vai diminuindo
        // Fase 1: 2 ticks (rápido, ~10 iterações)
        // Fase 2: 4 ticks (médio, ~5 iterações)
        // Fase 3: 8 ticks (lento, ~3 iterações)
        // Fase 4: reveal final

        final int[] phase = { 0 };
        final int[] iteration = { 0 };

        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Verifica se o jogador ainda está online e com o inventário aberto
            if (!player.isOnline() || !animatingPlayers.contains(player.getUniqueId())) {
                taskRef[0].cancel();
                animatingPlayers.remove(player.getUniqueId());
                return;
            }

            iteration[0]++;

            // Determinar limites de cada fase
            int maxIterations;
            long delay;
            if (iteration[0] <= 10) {
                maxIterations = 10;
                delay = 2; // Não usado aqui, mas indica a velocidade visual
            } else if (iteration[0] <= 15) {
                maxIterations = 15;
                delay = 4;
            } else if (iteration[0] <= 18) {
                maxIterations = 18;
                delay = 8;
            } else {
                // Fim da animação — revelar reward
                taskRef[0].cancel();
                revealReward(player, inv, crate, finalReward);
                return;
            }

            // Preencher slots centrais (10-16) com rewards aleatórios
            for (int slot = 10; slot <= 16; slot++) {
                CrateManager.CrateReward randomReward = plugin.getCrateManager().getRandomReward(crate.id());
                if (randomReward != null) {
                    inv.setItem(slot, createRewardDisplayItem(randomReward));
                }
            }

            // Som de clique
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);

        }, 2L, 3L); // Tick fixo de 3, a "desaceleração" é visual pelo número de mudanças
    }

    /**
     * Revela o reward final na GUI.
     */
    private void revealReward(Player player, Inventory inv, CrateManager.CrateType crate,
            CrateManager.CrateReward finalReward) {
        var msg = plugin.getMessageManager();

        // Limpar slots centrais
        ItemStack border = createGlassPane(crate.color());
        for (int slot = 10; slot <= 16; slot++) {
            inv.setItem(slot, border);
        }

        // Colocar reward no centro (slot 13)
        ItemStack rewardItem = createRewardDisplayItem(finalReward);
        // Adicionar lore extra "§a✓ RECOMPENSA!"
        ItemMeta meta = rewardItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null)
                lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§a✓ §lRECOMPENSA!"));
            meta.lore(lore);
            rewardItem.setItemMeta(meta);
        }
        inv.setItem(13, rewardItem);

        // Indicadores visuais nos slots adjacentes
        ItemStack indicator = createNamedItem(Material.LIME_STAINED_GLASS_PANE, "§a▶");
        inv.setItem(12, indicator);
        inv.setItem(14, indicator);

        // Som de vitória
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // Aplicar reward
        plugin.getCrateManager().applyReward(player, finalReward);

        // Mensagem
        msg.send(player, "crate.reward_received", finalReward.display());

        // Broadcast se configurado
        if (crate.broadcastOnOpen()) {
            String broadcast = msg.get("crate.broadcast_reward", player.getName(), crate.name(), finalReward.display());
            Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(broadcast));
        }

        // Permitir interação após 2s e fechar após 3s
        animatingPlayers.remove(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()
                    && player.getOpenInventory().getTopInventory().getHolder() instanceof CrateOpenHolder) {
                player.closeInventory();
            }
        }, 60L); // 3 segundos
    }

    // --- Preview ---

    /**
     * Abre a GUI de preview mostrando todos os rewards possíveis de uma crate.
     */
    public void openPreview(Player player, String crateId) {
        CrateManager crateManager = plugin.getCrateManager();
        var msg = plugin.getMessageManager();
        CrateManager.CrateType crate = crateManager.getCrateType(crateId);
        if (crate == null) {
            msg.send(player, "crate.invalid_type");
            return;
        }

        List<CrateManager.CrateReward> rewards = crate.rewards();
        int size = Math.min(54, ((rewards.size() + 8) / 9) * 9); // Arredonda para próximo múltiplo de 9
        if (size < 9)
            size = 9;

        String titleStr = msg.get("crate.preview_title", crate.name());
        Component title = LegacyComponentSerializer.legacySection().deserialize(titleStr);
        Inventory inv = Bukkit.createInventory(new CratePreviewHolder(crateId), size, title);

        for (int i = 0; i < rewards.size() && i < size; i++) {
            CrateManager.CrateReward reward = rewards.get(i);
            ItemStack item = createRewardDisplayItem(reward);

            // Adicionar chance no lore
            double chance = (reward.weight() * 100.0) / crate.totalWeight();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null)
                    lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
                lore.add(LegacyComponentSerializer.legacySection().deserialize(
                        String.format("§7Chance: §e%.1f%%", chance)));
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    // --- Seleção de crate ---

    /**
     * Abre a GUI de seleção de tipo de crate.
     */
    public void openCrateSelection(Player player) {
        var msg = plugin.getMessageManager();
        CrateManager crateManager = plugin.getCrateManager();
        Map<String, CrateManager.CrateType> types = crateManager.getAllCrateTypes();

        int size = Math.max(9, ((types.size() + 8) / 9) * 9);
        String titleStr = msg.get("crate.select_title");
        Component title = LegacyComponentSerializer.legacySection().deserialize(titleStr);
        Inventory inv = Bukkit.createInventory(new CrateSelectHolder(), size, title);

        int slot = 0;
        for (Map.Entry<String, CrateManager.CrateType> entry : types.entrySet()) {
            CrateManager.CrateType crate = entry.getValue();
            int keys = crateManager.getKeyCount(player, entry.getKey());

            ItemStack item = createNamedItem(crate.icon(), crate.name());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize(
                        "§eKeys: " + (keys > 0 ? "§a" + keys : "§c0")));
                lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§eClique esquerdo §7→ Abrir"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§eClique direito §7→ Preview"));
                meta.lore(lore);

                // Usar PDC para guardar o ID do crate
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(new org.bukkit.NamespacedKey(plugin, "crate_id"),
                        org.bukkit.persistence.PersistentDataType.STRING, entry.getKey());

                item.setItemMeta(meta);
            }

            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    // --- Bedrock Forms ---

    /**
     * Abre formulário nativo para jogadores Bedrock.
     */
    public void openBedrockForm(Player player) {
        if (!plugin.getBedrockFormManager().isAvailable()) {
            // Fallback: usar GUI normal
            openCrateSelection(player);
            return;
        }

        CrateManager crateManager = plugin.getCrateManager();
        var msg = plugin.getMessageManager();

        org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
        org.geysermc.cumulus.form.SimpleForm.Builder builder = org.geysermc.cumulus.form.SimpleForm.builder()
                .title(msg.get("crate.select_title"));

        List<String> crateIds = new ArrayList<>(crateManager.getCrateTypeIds());

        for (String id : crateIds) {
            CrateManager.CrateType crate = crateManager.getCrateType(id);
            int keys = crateManager.getKeyCount(player, id);
            builder.button(crate.name() + "\n§eKeys: " + keys);
        }

        builder.validResultHandler(response -> {
            int idx = response.clickedButtonId();
            if (idx >= 0 && idx < crateIds.size()) {
                String selectedId = crateIds.get(idx);
                // Na Bedrock, abre direto (sem animação visual)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    crateManager.openCrate(player, selectedId);
                });
            }
        });

        api.sendForm(player.getUniqueId(), builder.build());
    }

    // --- Event listeners ---

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Inventory topInv = event.getView().getTopInventory();

        // Bloquear cliques durante animação
        if (topInv.getHolder() instanceof CrateOpenHolder) {
            event.setCancelled(true);
            return;
        }

        // Preview: apenas cancelar cliques
        if (topInv.getHolder() instanceof CratePreviewHolder) {
            event.setCancelled(true);
            return;
        }

        // Seleção de crate
        if (topInv.getHolder() instanceof CrateSelectHolder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta())
                return;

            ItemMeta meta = clicked.getItemMeta();
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "crate_id");
            String crateId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            if (crateId == null)
                return;

            if (event.isLeftClick()) {
                player.closeInventory();
                // Abrir com animação
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openWithAnimation(player, crateId);
                }, 1L);
            } else if (event.isRightClick()) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openPreview(player, crateId);
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // Limpar flag de animação se fechar durante animação
            animatingPlayers.remove(player.getUniqueId());
        }
    }

    // --- Utilitários ---

    /**
     * Cria um ItemStack para representar visualmente um reward na GUI.
     */
    private ItemStack createRewardDisplayItem(CrateManager.CrateReward reward) {
        Material mat;
        int amount = Math.max(1, Math.min(64, (int) reward.amount()));

        switch (reward.type()) {
            case "money":
                mat = Material.GOLD_INGOT;
                amount = 1;
                break;
            case "claim_blocks":
                mat = Material.GRASS_BLOCK;
                amount = 1;
                break;
            case "item":
                mat = reward.material() != null ? reward.material() : Material.STONE;
                break;
            case "custom_item":
                // Tentar pegar o item real
                if (reward.itemId() != null && plugin.getCustomItemManager() != null) {
                    ItemStack custom = plugin.getCustomItemManager().getItem(reward.itemId());
                    if (custom != null)
                        return custom.clone();
                }
                mat = Material.DIAMOND_SWORD;
                amount = 1;
                break;
            case "title":
                mat = Material.NAME_TAG;
                amount = 1;
                break;
            case "crate_key":
                mat = Material.TRIPWIRE_HOOK;
                amount = (int) reward.amount();
                break;
            default:
                mat = Material.BARRIER;
                amount = 1;
        }

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(reward.display()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGlassPane(String color) {
        // Escolher cor do vidro baseado na cor do crate
        Material glassMat = switch (color) {
            case "§7" -> Material.GRAY_STAINED_GLASS_PANE;
            case "§9" -> Material.BLUE_STAINED_GLASS_PANE;
            case "§6" -> Material.ORANGE_STAINED_GLASS_PANE;
            case "§c" -> Material.RED_STAINED_GLASS_PANE;
            default -> Material.BLACK_STAINED_GLASS_PANE;
        };
        return createNamedItem(glassMat, " ");
    }

    private ItemStack createNamedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
