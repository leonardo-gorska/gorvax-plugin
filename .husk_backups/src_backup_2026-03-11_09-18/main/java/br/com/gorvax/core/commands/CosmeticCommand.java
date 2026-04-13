package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.CosmeticManager;
import br.com.gorvax.core.managers.CosmeticManager.CosmeticEntry;
import br.com.gorvax.core.managers.CosmeticManager.CosmeticType;
import br.com.gorvax.core.managers.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * B13 — Comando /cosmetics com GUI de seleção e subcomandos.
 */
public class CosmeticCommand implements CommandExecutor, TabCompleter, Listener {

    private static final String GUI_TITLE = "§5✦ Cosméticos";
    private static final String GUI_CATEGORY_TITLE = "§5✦ Cosméticos — ";
    private static final LegacyComponentSerializer LCS = LegacyComponentSerializer.legacySection();

    private final GorvaxCore plugin;
    private final NamespacedKey cosmeticKey;

    public CosmeticCommand(GorvaxCore plugin) {
        this.plugin = plugin;
        this.cosmeticKey = new NamespacedKey(plugin, "cosmetic_id");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        var msg = plugin.getMessageManager();

        if (!(sender instanceof Player player)) {
            msg.send(sender, "general.player_only");
            return true;
        }

        CosmeticManager cm = plugin.getCosmeticManager();
        if (cm == null) {
            player.sendMessage("§c[Cosméticos] Sistema não inicializado.");
            return true;
        }

        if (args.length == 0) {
            openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "ativar", "activate", "equip" -> {
                if (args.length < 2) {
                    msg.send(player, "cosmetics.usage_activate");
                    return true;
                }
                String cosId = args[1].toLowerCase();
                CosmeticEntry entry = cm.getCosmetic(cosId);
                if (entry == null) {
                    msg.send(player, "cosmetics.not_found");
                    return true;
                }
                if (!cm.isUnlocked(player, cosId)) {
                    msg.send(player, "cosmetics.locked");
                    return true;
                }
                if (cm.activateCosmetic(player, cosId)) {
                    msg.send(player, "cosmetics.activated", entry.name());
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                } else {
                    msg.send(player, "cosmetics.activate_failed");
                }
            }

            case "desativar", "deactivate", "unequip" -> {
                if (args.length < 2) {
                    msg.send(player, "cosmetics.usage_deactivate");
                    return true;
                }
                String typeName = args[1].toUpperCase();
                try {
                    CosmeticType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    msg.send(player, "cosmetics.invalid_type");
                    return true;
                }
                cm.deactivateCosmetic(player, typeName);
                msg.send(player, "cosmetics.deactivated", typeName);
            }

            case "listar", "list" -> {
                List<CosmeticEntry> available = cm.getAvailableCosmetics(player);
                if (available.isEmpty()) {
                    msg.send(player, "cosmetics.none_unlocked");
                    return true;
                }
                player.sendMessage(msg.get("cosmetics.list_header"));
                PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                for (CosmeticEntry entry : available) {
                    String activeId = pd.getActiveCosmetics().get(entry.type().name());
                    boolean isActive = entry.id().equals(activeId);
                    String status = isActive ? "§a[ATIVO]" : "§7[—]";
                    player.sendMessage("  " + status + " " + entry.name() + " §8(" + entry.type().name() + ")");
                }
                player.sendMessage(msg.get("cosmetics.list_footer"));
            }

            // Admin: dar/tirar cosmético
            case "give", "dar" -> {
                if (!player.hasPermission("gorvax.admin")) {
                    msg.send(player, "general.no_permission");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§b[Cosméticos] §fUso: §e/cosmetics give <nick> <id>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    msg.send(player, "general.player_not_found");
                    return true;
                }
                String giveId = args[2].toLowerCase();
                if (cm.getCosmetic(giveId) == null) {
                    msg.send(player, "cosmetics.not_found");
                    return true;
                }
                cm.unlockCosmetic(target, giveId);
                CosmeticEntry givenEntry = cm.getCosmetic(giveId);
                player.sendMessage(
                        "§a[Cosméticos] ✓ " + givenEntry.name() + " §adesbloqueado para §f" + target.getName());
                msg.send(target, "cosmetics.unlocked", givenEntry.name());
                target.playSound(target.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            }

            default -> {
                msg.send(player, "cosmetics.usage");
            }
        }

        return true;
    }

    // ──────────────── GUI Principal ────────────────

    private void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, LCS.deserialize(GUI_TITLE));

        // Ícones por categoria
        gui.setItem(10, createCategoryIcon(player, CosmeticType.WALK_PARTICLE,
                Material.BLAZE_POWDER, "§6🔥 Partículas de Caminhada",
                "§7Efeitos visuais ao caminhar."));

        gui.setItem(12, createCategoryIcon(player, CosmeticType.ARROW_TRAIL,
                Material.SPECTRAL_ARROW, "§d✨ Trails de Flecha",
                "§7Trilhas em seus projéteis."));

        gui.setItem(14, createCategoryIcon(player, CosmeticType.CHAT_TAG,
                Material.NAME_TAG, "§b🏷 Tags de Chat",
                "§7Prefixos customizados no chat."));

        gui.setItem(16, createCategoryIcon(player, CosmeticType.KILL_EFFECT,
                Material.TNT, "§c💥 Efeitos de Kill",
                "§7Efeitos visuais ao eliminar."));

        // KILL_PARTICLE compartilha o menu de KILL_EFFECT

        // Borda decorativa
        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(LCS.deserialize(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.2f);
    }

    private ItemStack createCategoryIcon(Player player, CosmeticType type, Material material,
            String name, String description) {
        CosmeticManager cm = plugin.getCosmeticManager();
        List<CosmeticEntry> unlocked = new ArrayList<>(cm.getAvailableByType(player, type));
        List<CosmeticEntry> all = new ArrayList<>(cm.getAllByType(type));

        // Inclui KILL_PARTICLE na contagem de kill effects
        if (type == CosmeticType.KILL_EFFECT) {
            unlocked.addAll(cm.getAvailableByType(player, CosmeticType.KILL_PARTICLE));
            all.addAll(cm.getAllByType(CosmeticType.KILL_PARTICLE));
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LCS.deserialize(name));

        List<String> lore = new ArrayList<>();
        lore.add(description);
        lore.add("");
        lore.add("§7Desbloqueados: §f" + unlocked.size() + "§7/§f" + all.size());
        lore.add("");
        lore.add("§eClique para ver!");
        meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());

        item.setItemMeta(meta);
        return item;
    }

    // ──────────────── GUI de Categoria ────────────────

    private void openCategoryMenu(Player player, CosmeticType type) {
        CosmeticManager cm = plugin.getCosmeticManager();

        List<CosmeticEntry> cosmetics = new ArrayList<>(cm.getAllByType(type));
        if (type == CosmeticType.KILL_EFFECT) {
            cosmetics.addAll(cm.getAllByType(CosmeticType.KILL_PARTICLE));
        }

        String typeName = switch (type) {
            case WALK_PARTICLE -> "Partículas";
            case ARROW_TRAIL -> "Trails";
            case CHAT_TAG -> "Tags";
            case KILL_EFFECT -> "Kill Effects";
            default -> type.name();
        };

        int size = Math.min(54, ((cosmetics.size() / 9) + 1) * 9 + 9);
        if (size < 27)
            size = 27;

        Inventory gui = Bukkit.createInventory(null, size, LCS.deserialize(GUI_CATEGORY_TITLE + typeName));
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        int slot = 0;
        for (CosmeticEntry entry : cosmetics) {
            if (slot >= size - 9)
                break; // Última linha para navegação

            boolean unlocked = cm.isUnlocked(player, entry.id());
            String activeId = pd.getActiveCosmetics().get(entry.type().name());
            boolean isActive = entry.id().equals(activeId);

            Material iconMat;
            if (isActive) {
                iconMat = Material.LIME_DYE;
            } else if (unlocked) {
                iconMat = Material.LIGHT_BLUE_DYE;
            } else {
                iconMat = Material.GRAY_DYE;
            }

            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(LCS.deserialize(entry.name()));

            List<String> lore = new ArrayList<>();
            if (entry.description() != null && !entry.description().isEmpty()) {
                lore.add("§7" + entry.description());
            }
            lore.add("");
            if (isActive) {
                lore.add("§a✓ ATIVO");
                lore.add("");
                lore.add("§eClique para desativar");
            } else if (unlocked) {
                lore.add("§b✓ Desbloqueado");
                lore.add("");
                lore.add("§eClique para ativar");
            } else {
                lore.add("§c✗ Bloqueado");
                String sourceHint = switch (entry.source()) {
                    case "achievement" -> "§7Desbloqueie via conquistas";
                    case "shop" -> "§7Disponível na loja por §6$" + String.format("%.0f", entry.price());
                    case "crate" -> "§7Encontrado em crates";
                    case "vip" -> "§7Exclusivo para VIP";
                    default -> "§7Admin";
                };
                lore.add(sourceHint);
            }

            meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
            // Armazenar ID via PDC (PersistentDataContainer) — robusto e imune a color
            // codes
            meta.getPersistentDataContainer().set(cosmeticKey, PersistentDataType.STRING, entry.id());
            item.setItemMeta(meta);

            gui.setItem(slot, item);
            slot++;
        }

        // Botão voltar
        int backSlot = size - 5;
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(LCS.deserialize("§c← Voltar"));
        back.setItemMeta(backMeta);
        gui.setItem(backSlot, back);

        // Desativar todos
        int clearSlot = size - 3;
        ItemStack clear = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clear.getItemMeta();
        clearMeta.displayName(LCS.deserialize("§c✗ Desativar " + typeName));
        List<String> clearLore = new ArrayList<>();
        clearLore.add("§7Remove o cosmético ativo deste tipo.");
        clearMeta.lore(clearLore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
        // Armazenar ação de clear via PDC
        clearMeta.getPersistentDataContainer().set(cosmeticKey, PersistentDataType.STRING, "__clear__" + type.name());
        clear.setItemMeta(clearMeta);
        gui.setItem(clearSlot, clear);

        player.openInventory(gui);
    }

    // ──────────────── Click Handler ────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title;
        try {
            // Use Adventure API title() which returns Component, serialize to compare
            Component titleComponent = event.getView().title();
            title = LCS.serialize(titleComponent);
        } catch (Exception e) {
            // Paper 1.21+ InventoryView interface compatibility
            return;
        }

        if (title.equals(GUI_TITLE)) {
            event.setCancelled(true);
            handleMainMenuClick(player, event.getSlot());
        } else if (title.startsWith(GUI_CATEGORY_TITLE)) {
            event.setCancelled(true);
            handleCategoryClick(player, event);
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 10 -> openCategoryMenu(player, CosmeticType.WALK_PARTICLE);
            case 12 -> openCategoryMenu(player, CosmeticType.ARROW_TRAIL);
            case 14 -> openCategoryMenu(player, CosmeticType.CHAT_TAG);
            case 16 -> openCategoryMenu(player, CosmeticType.KILL_EFFECT);
        }
    }

    private void handleCategoryClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        var msg = plugin.getMessageManager();
        CosmeticManager cm = plugin.getCosmeticManager();
        ItemMeta meta = clicked.getItemMeta();

        // Botão voltar
        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }

        // Extrair ID via PDC (PersistentDataContainer)
        String hiddenId = meta.getPersistentDataContainer().get(cosmeticKey, PersistentDataType.STRING);
        if (hiddenId == null || hiddenId.isEmpty())
            return;

        // Botão desativar todos
        if (hiddenId.startsWith("__clear__")) {
            String typeName = hiddenId.substring(9);
            cm.deactivateCosmetic(player, typeName);
            msg.send(player, "cosmetics.deactivated", typeName);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
            // Reabrir menu atualizado
            try {
                CosmeticType reopenType = CosmeticType.valueOf(typeName);
                openCategoryMenu(player, reopenType);
            } catch (IllegalArgumentException ignored) {
                openMainMenu(player);
            }
            return;
        }

        // Cosmético clicado
        CosmeticEntry entry = cm.getCosmetic(hiddenId);
        if (entry == null)
            return;

        if (!cm.isUnlocked(player, hiddenId)) {
            msg.send(player, "cosmetics.locked");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        // Toggle: se já ativo, desativa; senão ativa
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
        String activeId = pd.getActiveCosmetics().get(entry.type().name());

        if (hiddenId.equals(activeId)) {
            cm.deactivateCosmetic(player, entry.type().name());
            msg.send(player, "cosmetics.deactivated", entry.type().name());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
        } else {
            cm.activateCosmetic(player, hiddenId);
            msg.send(player, "cosmetics.activated", entry.name());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        }

        // Reabrir menu da categoria
        openCategoryMenu(player, entry.type() == CosmeticType.KILL_PARTICLE ? CosmeticType.KILL_EFFECT : entry.type());
    }

    // ──────────────── Tab Complete ────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        List<String> completions = new ArrayList<>();
        CosmeticManager cm = plugin.getCosmeticManager();

        if (args.length == 1) {
            completions.add("ativar");
            completions.add("desativar");
            completions.add("listar");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("give");
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("ativar") || sub.equals("activate") || sub.equals("equip")) {
                // Sugerir cosméticos desbloqueados
                if (sender instanceof Player player && cm != null) {
                    for (CosmeticEntry e : cm.getAvailableCosmetics(player)) {
                        completions.add(e.id());
                    }
                }
            } else if (sub.equals("desativar") || sub.equals("deactivate") || sub.equals("unequip")) {
                for (CosmeticType type : CosmeticType.values()) {
                    completions.add(type.name());
                }
            } else if (sub.equals("give") || sub.equals("dar")) {
                // Sugerir jogadores online
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
            return filterCompletions(completions, args[1]);
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("dar"))) {
            if (cm != null) {
                completions.addAll(cm.getRegistry().keySet());
            }
            return filterCompletions(completions, args[2]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
