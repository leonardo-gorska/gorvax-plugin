package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;

public class InputManager implements Listener {

    private final GorvaxCore plugin;

    public InputManager(GorvaxCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // --- ANVIL / CHAT INPUT (Dual Support for Bedrock) ---
    private final Map<UUID, Consumer<String>> chatInputQueue = Collections.synchronizedMap(new HashMap<>());

    public void openAnvilInput(Player p, String title, String initialText, Consumer<String> onComplete) {
        boolean isBedrock = isBedrockPlayer(p);

        if (isBedrock) {
            // B4.1 — Tentar Form nativo do Floodgate primeiro
            BedrockFormManager formManager = plugin.getBedrockFormManager();
            if (formManager != null && formManager.isAvailable()) {
                boolean sent = formManager.sendCustomForm(p, title,
                        plugin.getMessageManager().get("input.bedrock_form_label"),
                        initialText != null ? initialText : "",
                        (result) -> {
                            if (result == null || result.equalsIgnoreCase("cancelar")
                                    || result.equalsIgnoreCase("cancel")) {
                                p.sendMessage(plugin.getMessageManager().get("general.operation_cancelled"));
                                return;
                            }
                            onComplete.accept(result);
                        });
                if (sent)
                    return;
                // Se falhou, cai no fallback chat abaixo
            }

            // Fallback: input via chat (funcional mesmo sem Floodgate)
            p.closeInventory();
            p.sendMessage("");
            p.sendMessage(plugin.getMessageManager().get("input.bedrock_header", title));
            p.sendMessage(plugin.getMessageManager().get("input.bedrock_instruction"));
            p.sendMessage(plugin.getMessageManager().get("input.bedrock_cancel_hint"));
            p.sendMessage(plugin.getMessageManager().get("input.bedrock_timeout_hint"));
            p.sendMessage("");

            plugin.getMessageManager().sendTitle(p, "input.bedrock_title", "input.bedrock_subtitle", 10, 120, 20,
                    title);
            p.sendActionBar(LegacyComponentSerializer.legacySection()
                    .deserialize(plugin.getMessageManager().get("input.actionbar_waiting")));

            chatInputQueue.put(p.getUniqueId(), onComplete);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (chatInputQueue.remove(p.getUniqueId()) != null) {
                    p.sendMessage(plugin.getMessageManager().get("input.timeout_expired"));
                    plugin.getMessageManager().sendTitle(p, "input.timeout_title", "input.timeout_subtitle", 10, 40,
                            10);
                }
            }, 1200L);
            return;
        }

        try {
            new AnvilGUI.Builder()
                    .onClick((slot, stateSnapshot) -> {
                        if (slot != AnvilGUI.Slot.OUTPUT) {
                            return java.util.Collections.emptyList();
                        }
                        chatInputQueue.remove(p.getUniqueId());
                        onComplete.accept(stateSnapshot.getText());
                        return java.util.Collections.singletonList(AnvilGUI.ResponseAction.close());
                    })
                    .onClose(stateSnapshot -> {
                        // Se fechou sem completar, remove do chat queue apenas se não for Bedrock
                        // No Bedrock mantemos a queue ativa por alguns segundos/minutos para o chat
                    })
                    .text(initialText)
                    .title(title)
                    .plugin(plugin)
                    .open(p);
        } catch (Throwable t) {
            // Fallback for Java players if AnvilGUI fails (e.g. version mismatch)
            plugin.getLogger().warning(
                    "AnvilGUI failed to open for " + p.getName() + ": " + t.getMessage() + ". Using chat fallback.");

            p.closeInventory();
            p.sendMessage("");
            p.sendMessage(plugin.getMessageManager().get("input.fallback_header", title));
            p.sendMessage(plugin.getMessageManager().get("input.fallback_error"));
            p.sendMessage(plugin.getMessageManager().get("input.bedrock_cancel_hint"));
            p.sendMessage(plugin.getMessageManager().get("input.bedrock_timeout_hint"));
            p.sendMessage("");

            plugin.getMessageManager().sendTitle(p, "input.bedrock_title", "input.bedrock_subtitle", 10, 120, 20,
                    title);

            chatInputQueue.put(p.getUniqueId(), onComplete);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (chatInputQueue.remove(p.getUniqueId()) != null) {
                    p.sendMessage(plugin.getMessageManager().get("input.timeout_expired"));
                }
            }, 1200L);
        }
    }

    /**
     * Verifica se o jogador é Bedrock via Floodgate.
     * Público e estático para uso em todo o plugin (menus, mensagens, etc.).
     */
    public static boolean isBedrockPlayer(Player p) {
        try {
            if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
                return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
            }
        } catch (NoClassDefFoundError ignored) {
        }
        return false;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onChatInput(org.bukkit.event.player.AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (chatInputQueue.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
            Consumer<String> callback = chatInputQueue.remove(p.getUniqueId());
            String text = e.getMessage();

            if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel")) {
                p.sendMessage(plugin.getMessageManager().get("general.operation_cancelled"));
                p.showTitle(net.kyori.adventure.title.Title.title(Component.empty(), Component.empty(),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ZERO, java.time.Duration.ofMillis(50), java.time.Duration.ZERO))); // Clear
                                                                                                                      // title
                return;
            }

            // Run on primary thread for safety as callbacks often touch Bukkit API
            Bukkit.getScheduler().runTask(plugin, () -> {
                callback.accept(text);
                // p.sendMessage("§a§lOK! §fTexto recebido: §b" + text); // Feedback redundante
                // se o callback já fizer algo
            });
        }
    }

    // --- NUMERIC INPUT ---
    public void openNumericInput(Player p, String title, double initialValue, Consumer<Double> onComplete) {
        // B4.1 — Bedrock: usar Form nativo ou chat ao invés de GUI de keypad
        if (isBedrockPlayer(p)) {
            BedrockFormManager formManager = plugin.getBedrockFormManager();
            if (formManager != null && formManager.isAvailable()) {
                boolean sent = formManager.sendCustomForm(p, title,
                        plugin.getMessageManager().get("input.bedrock_numeric_label"),
                        initialValue == 0 ? "" : String.valueOf(initialValue),
                        (result) -> {
                            if (result == null || result.isEmpty()) {
                                p.sendMessage(plugin.getMessageManager().get("general.operation_cancelled"));
                                return;
                            }
                            try {
                                double val = Double.parseDouble(result.trim());
                                onComplete.accept(val);
                            } catch (NumberFormatException ex) {
                                p.sendMessage(plugin.getMessageManager().get("input.invalid_value"));
                            }
                        });
                if (sent)
                    return;
            }
            // Fallback: usar chat input para número
            openAnvilInput(p, title, initialValue == 0 ? "" : String.valueOf(initialValue), (text) -> {
                try {
                    double val = Double.parseDouble(text.trim());
                    onComplete.accept(val);
                } catch (NumberFormatException ex) {
                    p.sendMessage(plugin.getMessageManager().get("input.invalid_value"));
                }
            });
            return;
        }

        NumericHolder holder = new NumericHolder(initialValue, onComplete);
        Inventory inv = Bukkit.createInventory(holder, 54,
                LegacyComponentSerializer.legacySection().deserialize(title));
        holder.setInventory(inv);
        holder.update();
        p.openInventory(inv);
    }

    // --- PLAYER SELECT INPUT ---
    public void openPlayerSelectInput(Player p, String title, Consumer<UUID> onSelect) {
        // B4.1 — Bedrock: usar SimpleForm com lista de jogadores
        if (isBedrockPlayer(p)) {
            BedrockFormManager formManager = plugin.getBedrockFormManager();
            if (formManager != null && formManager.isAvailable()) {
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (online.isEmpty()) {
                    p.sendMessage(plugin.getMessageManager().get("general.player_not_found"));
                    return;
                }
                List<String> names = new ArrayList<>();
                for (Player op : online) {
                    names.add(op.getName());
                }
                boolean sent = formManager.sendSimpleForm(p, title,
                        plugin.getMessageManager().get("input.bedrock_select_player"),
                        names, (index) -> {
                            if (index < 0 || index >= online.size())
                                return;
                            onSelect.accept(online.get(index).getUniqueId());
                        });
                if (sent)
                    return;
            }
        }

        PlayerSelectHolder holder = new PlayerSelectHolder(this, onSelect, 0);
        Inventory inv = Bukkit.createInventory(holder, 54,
                LegacyComponentSerializer.legacySection().deserialize(title));
        holder.setInventory(inv);
        holder.update();
        p.openInventory(inv);
    }

    // --- LISTENERS ---

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        // Verifica o inventário superior (o menu aberto)
        Inventory topInv = e.getView().getTopInventory();
        InventoryHolder topHolder = topInv.getHolder();

        // Se o menu aberto for um dos nossos, cancela TUDO por segurança PRIMEIRA COISA
        if (topHolder instanceof NumericHolder || topHolder instanceof PlayerSelectHolder) {
            e.setCancelled(true); // TRAVA O MENU: Ninguém mexe em nada a menos que a gente deixe

            // Só processa a lógica se clicou no inventário de cima (GUI)
            if (e.getClickedInventory() != null && e.getClickedInventory().equals(topInv)) {
                if (topHolder instanceof NumericHolder) {
                    ((NumericHolder) topHolder).handleClick(e);
                } else if (topHolder instanceof PlayerSelectHolder) {
                    ((PlayerSelectHolder) topHolder).handleClick(e);
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        Inventory topInv = e.getView().getTopInventory();
        InventoryHolder topHolder = topInv.getHolder();

        if (topHolder instanceof NumericHolder || topHolder instanceof PlayerSelectHolder) {
            e.setCancelled(true);
        }
    }

    // --- HOLDERS ---

    public static class NumericHolder implements InventoryHolder {
        private Inventory inventory;
        private double value;
        private final Consumer<Double> callback;
        private boolean isDecimal = false;
        private String currentInput = "";

        public NumericHolder(double initial, Consumer<Double> callback) {
            this.value = initial;
            this.callback = callback;
            this.currentInput = (initial == 0) ? "" : String.valueOf(initial).replace(".0", "");
        }

        public void setInventory(Inventory inv) {
            this.inventory = inv;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void update() {
            // Fill Background
            ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta bgMeta = bg.getItemMeta();
            bgMeta.displayName(LegacyComponentSerializer.legacySection().deserialize(" "));
            bg.setItemMeta(bgMeta);
            for (int i = 0; i < 54; i++)
                inventory.setItem(i, bg);

            // Display Value
            ItemStack display = new ItemStack(Material.PAPER);
            ItemMeta meta = display.getItemMeta();
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize("§eValor: §f" + (currentInput.isEmpty() ? "0" : currentInput)));
            display.setItemMeta(meta);
            inventory.setItem(13, display);

            // Keypad
            // 7 8 9
            // 4 5 6
            // 1 2 3
            // 0
            int[] slots = { 20, 21, 22, 29, 30, 31, 38, 39, 40 };
            int[] nums = { 7, 8, 9, 4, 5, 6, 1, 2, 3 };

            for (int i = 0; i < nums.length; i++) {
                inventory.setItem(slots[i], createNumItem(nums[i]));
            }
            inventory.setItem(49, createNumItem(0));

            // Actions
            ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta cm = confirm.getItemMeta();
            cm.displayName(LegacyComponentSerializer.legacySection().deserialize("§aConfirmar"));
            confirm.setItemMeta(cm);
            inventory.setItem(51, confirm); // Right side

            ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
            ItemMeta can = cancel.getItemMeta();
            can.displayName(LegacyComponentSerializer.legacySection().deserialize("§cCancelar"));
            cancel.setItemMeta(can);
            inventory.setItem(47, cancel); // Left side

            ItemStack backspace = new ItemStack(Material.BARRIER);
            ItemMeta bm = backspace.getItemMeta();
            bm.displayName(LegacyComponentSerializer.legacySection().deserialize("§cApagar"));
            backspace.setItemMeta(bm);
            inventory.setItem(50, backspace);

            ItemStack dot = new ItemStack(Material.STONE_BUTTON);
            ItemMeta dm = dot.getItemMeta();
            dm.displayName(LegacyComponentSerializer.legacySection().deserialize("§f."));
            dot.setItemMeta(dm);
            inventory.setItem(48, dot);
        }

        private ItemStack createNumItem(int num) {
            // Se for 0, usamos 1 como amount mas exibimos 0 no display.
            // Para o número 1, o amount 1 não aparece no canto do item.
            // Para todos os outros, o amount mostra o número.
            ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE, num == 0 ? 1 : num);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§a§l" + num));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Valor: §f" + num));
            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§eClique para selecionar"));
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        public void handleClick(InventoryClickEvent e) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            Player p = (Player) e.getWhoClicked();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                // Number
                int num = clicked.getAmount();
                if (num == 1 && PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())
                        .endsWith("0"))
                    num = 0; // Fix 0 case

                if (currentInput.equals("0"))
                    currentInput = "";
                if (currentInput.length() < 10) { // Limit length
                    currentInput += num;
                    update();
                }
            } else if (clicked.getType() == Material.STONE_BUTTON) {
                if (!currentInput.contains(".")) {
                    if (currentInput.isEmpty())
                        currentInput = "0";
                    currentInput += ".";
                    update();
                }
            } else if (clicked.getType() == Material.BARRIER) {
                if (currentInput.length() > 0) {
                    currentInput = currentInput.substring(0, currentInput.length() - 1);
                    update();
                }
            } else if (clicked.getType() == Material.RED_CONCRETE) {
                p.closeInventory();
            } else if (clicked.getType() == Material.LIME_CONCRETE) {
                try {
                    double val = currentInput.isEmpty() ? 0 : Double.parseDouble(currentInput);
                    p.closeInventory();
                    callback.accept(val);
                } catch (NumberFormatException ex) {
                    p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("input.invalid_value"));
                }
            }
        }
    }

    public static class PlayerSelectHolder implements InventoryHolder {
        private final InputManager manager;
        private Inventory inventory;
        private final Consumer<UUID> callback;
        private int page;

        public PlayerSelectHolder(InputManager manager, Consumer<UUID> callback, int page) {
            this.manager = manager;
            this.callback = callback;
            this.page = page;
        }

        public void setInventory(Inventory inv) {
            this.inventory = inv;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void update() {
            inventory.clear();

            // Background
            ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta bgMeta = bg.getItemMeta();
            bgMeta.displayName(LegacyComponentSerializer.legacySection().deserialize(" "));
            bg.setItemMeta(bgMeta);
            // Border logic or just fill check later

            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            int slotsPerPage = 45;
            int start = page * slotsPerPage;
            int end = Math.min(start + slotsPerPage, players.size());

            for (int i = start; i < end; i++) {
                Player p = players.get(i);
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(p);
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§e" + p.getName()));
                skull.setItemMeta(meta);
                inventory.addItem(skull);
            }

            // Controls
            if (page > 0) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta pm = prev.getItemMeta();
                pm.displayName(LegacyComponentSerializer.legacySection().deserialize("§cPágina Anterior"));
                prev.setItemMeta(pm);
                inventory.setItem(45, prev);
            }

            if (end < players.size()) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta nm = next.getItemMeta();
                nm.displayName(LegacyComponentSerializer.legacySection().deserialize("§aPróxima Página"));
                next.setItemMeta(nm);
                inventory.setItem(53, next);
            }

            // Search Button
            ItemStack search = new ItemStack(Material.OAK_SIGN);
            ItemMeta sm = search.getItemMeta();
            sm.displayName(LegacyComponentSerializer.legacySection().deserialize("§eBuscar Jogador Offline"));
            search.setItemMeta(sm);
            inventory.setItem(49, search);

            // Fill empty slots with bg?
            for (int i = 0; i < 54; i++) {
                if (inventory.getItem(i) == null)
                    inventory.setItem(i, bg);
            }
        }

        public void handleClick(InventoryClickEvent e) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR
                    || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE)
                return;

            Player p = (Player) e.getWhoClicked();

            if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta.getOwningPlayer() != null) {
                    p.closeInventory();
                    callback.accept(meta.getOwningPlayer().getUniqueId());
                }
            } else if (clicked.getType() == Material.ARROW) {
                // BUG-06 FIX: Separar paginação da busca offline
                if (clicked.getItemMeta() != null && clicked.getItemMeta().hasDisplayName()) {
                    String displayText = PlainTextComponentSerializer.plainText()
                            .serialize(clicked.getItemMeta().displayName());
                    if (displayText.contains("Próxima")) {
                        page++;
                        update();
                    } else if (displayText.contains("Anterior")) {
                        page--;
                        update();
                    }
                }
            } else if (clicked.getType() == Material.OAK_SIGN) {
                // BUG-06 FIX: Busca offline agora só no botão correto (slot 49)
                manager.openAnvilInput(p, "Nome do Jogador", "", (name) -> {
                    if (name == null || name.trim().isEmpty()) {
                        p.sendMessage(manager.plugin.getMessageManager().get("market.error_invalid_name"));
                        return;
                    }

                    p.sendMessage(manager.plugin.getMessageManager().get("market.search_searching"));
                    p.closeInventory();

                    Bukkit.getScheduler().runTaskAsynchronously(manager.plugin, () -> {
                        @SuppressWarnings("deprecation")
                        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(name);

                        Bukkit.getScheduler().runTask(manager.plugin, () -> {
                            if (!op.hasPlayedBefore() && !op.isOnline()) {
                                p.sendMessage(manager.plugin.getMessageManager().get("market.search_not_found"));
                                return;
                            }
                            callback.accept(op.getUniqueId());
                        });
                    });
                });
            }
        }
    }
}
