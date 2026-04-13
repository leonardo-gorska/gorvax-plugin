package br.com.gorvax.core.towns.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.SubPlot;
import br.com.gorvax.core.towns.menus.BaseMenu;
import br.com.gorvax.core.towns.menus.KingdomInventory;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerQuitEvent;

public class MenuListener implements Listener {

    private final GorvaxCore plugin;

    private final Map<UUID, Long> clickCooldown = new ConcurrentHashMap<>();

    // Cached Keys for Performance
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey DATA_KEY;
    private final NamespacedKey PLOT_ID_KEY;

    public MenuListener(GorvaxCore plugin) {
        this.plugin = plugin;
        this.ACTION_KEY = new NamespacedKey(plugin, "gorvax_action");
        this.DATA_KEY = new NamespacedKey(plugin, "gorvax_data");
        this.PLOT_ID_KEY = new NamespacedKey(plugin, "plot_id");
        plugin.getLogger().info("§a[Debug] MenuListener inicializado e pronto para capturar cliques!");
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();

        boolean isKingdomMenu = (holder instanceof BaseMenu.KingdomHolder)
                || (holder != null && holder.getClass().getSimpleName().equals("KingdomHolder"));

        if (!isKingdomMenu) {
            ItemStack checkItem = inv.getItem(0);
            if (checkItem != null && checkItem.getType() != Material.AIR) {
                ItemMeta checkMeta = checkItem.getItemMeta();
                if (checkMeta != null) {
                    String marker = getPdcString(checkMeta, ACTION_KEY);
                    if ("MENU_BORDER".equals(marker)) {
                        isKingdomMenu = true;
                    }
                }
            }
        }

        if (isKingdomMenu) {
            e.setCancelled(true); // Bloqueia arrastar itens no menu
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clickCooldown.remove(e.getPlayer().getUniqueId());
    }

    private void handleTrustInput(Player p, String cityId, String subPlotId, UUID targetUUID) {
        Claim claim = plugin.getClaimManager().getClaimById(cityId);
        if (claim == null)
            return;

        if (targetUUID.equals(p.getUniqueId())) {
            plugin.getMessageManager().send(p, "menu.error_self_add");
            plugin.getKingdomInventory().openTrustMenu(p, cityId, subPlotId);
            return;
        }

        Player target = Bukkit.getPlayer(targetUUID);
        String name = target != null ? target.getName() : Bukkit.getOfflinePlayer(targetUUID).getName();

        // Add Logic
        if (subPlotId == null || subPlotId.equals("MAIN")) {
            claim.addTrust(targetUUID, Claim.TrustType.GERAL);
        } else {
            for (SubPlot sp : claim.getSubPlots()) {
                if (sp.getId().equals(subPlotId)) {
                    sp.addTrust(targetUUID, Claim.TrustType.GERAL);
                    break;
                }
            }
        }

        plugin.getClaimManager().saveClaims();
        plugin.getMessageManager().send(p, "menu.friend_added", name);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Re-open Menu
        plugin.getKingdomInventory().openTrustMenu(p, cityId, subPlotId);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null)
            return;

        // Tenta pegar o holder de várias formas para garantir compatibilidade
        Inventory topInv = e.getView().getTopInventory();
        InventoryHolder holder = topInv.getHolder();

        // Debug simples no console
        // plugin.getLogger().info("Clique detectado! Holder: " + (holder != null ?
        // holder.getClass().getName() : "null"));

        // VERIFICAÇÃO ROBUSTA: Holder OU Marcador PDC
        boolean isKingdomMenu = (holder instanceof BaseMenu.KingdomHolder)
                || (holder != null && holder.getClass().getSimpleName().equals("KingdomHolder"));

        String fallbackCityId = null;

        if (!isKingdomMenu) {
            // Check PDC fallback on slot 0 (Border)
            ItemStack checkItem = topInv.getItem(0);
            if (checkItem != null && checkItem.getType() != Material.AIR) {
                ItemMeta checkMeta = checkItem.getItemMeta();
                if (checkMeta != null) {
                    String marker = getPdcString(checkMeta, ACTION_KEY);
                    if ("MENU_BORDER".equals(marker)) {
                        isKingdomMenu = true;
                        fallbackCityId = getPdcString(checkMeta, DATA_KEY);
                    }
                }
            }
        }

        if (isKingdomMenu) {
            e.setCancelled(true);

            Player p = (Player) e.getWhoClicked();

            // Debounce: 200ms
            long now = System.currentTimeMillis();
            if (clickCooldown.getOrDefault(p.getUniqueId(), 0L) > now) {
                return;
            }
            clickCooldown.put(p.getUniqueId(), now + 500);

            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
                return;

            BaseMenu.KingdomHolder cityHolder = null;
            if (holder instanceof BaseMenu.KingdomHolder) {
                cityHolder = (BaseMenu.KingdomHolder) holder;
            }

            // Variável p já definida na linha 139
            ItemStack item = e.getCurrentItem();
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                return;

            String action = getPdcString(meta, ACTION_KEY);
            String data = getPdcString(meta, DATA_KEY);

            if (action == null) {
                action = getPdcString(meta, PLOT_ID_KEY);
            }

            // Se ainda for null, talvez não seja um item clicável de verdade
            if (action == null)
                return;

            // Define cityId com fallback se necessário
            String cityId = (cityHolder != null) ? cityHolder.getKingdomId()
                    : (fallbackCityId != null ? fallbackCityId : "unknown");

            // Log de execução
            // plugin.getLogger().info("Ação do Menu: " + action + " (City: " + cityId +
            // ")");

            handleAction(p, cityId, action, data, item, e.getClick());
        }
    }

    private void handleAction(Player p, String cityId, String action, String data, ItemStack item,
            ClickType clickType) {
        boolean isShiftClick = clickType.isShiftClick(); // Manter compatibilidade com código existente
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        if (action.equals("BACK")) {
            plugin.getKingdomInventory().openMainMenu(p);
            return;
        }

        if (action.equals("NEXT_PAGE") || action.equals("PREVIOUS_PAGE")) {
            // Recalculate page
            InventoryHolder topHolder = p.getOpenInventory().getTopInventory().getHolder();
            if (!(topHolder instanceof BaseMenu.KingdomHolder))
                return;

            BaseMenu.KingdomHolder kh = (BaseMenu.KingdomHolder) topHolder;
            int currentPage = kh.getPage();
            int newPage = action.equals("NEXT_PAGE") ? currentPage + 1 : currentPage - 1;

            if (newPage < 0)
                newPage = 0;

            // Redirect based on data (Menu Type)
            if (data != null) {
                if (data.equals("MEMBERS")) {
                    plugin.getKingdomInventory().openMembersMenu(p, cityId, newPage);
                } else if (data.equals("LOTS")) {
                    plugin.getKingdomInventory().openLotsMenu(p, cityId, newPage);
                } else if (data.equals("TRUST")) {
                    // Para TRUST, o menuType no KingdomHolder contém o PlotID
                    String menuType = kh.getMenuType();
                    String subId = menuType.replace("TRUST_MENU_", "");
                    plugin.getKingdomInventory().openTrustMenu(p, cityId, subId.equals("MAIN") ? null : subId, newPage);
                }
            }
            return;
        }

        // --- SUB-MENUS ---
        if (action.equals("VIEW_LOTS")) {
            plugin.getKingdomInventory().openLotsMenu(p, cityId);
            return;
        }

        if (action.equals("VIEW_MY_PLOTS")) {
            plugin.getKingdomInventory().openMyPlotsMenu(p, cityId);
            return;
        }

        if (action.equals("ADMIN_PANEL")) {
            plugin.getKingdomInventory().openAdminMenu(p, cityId);
            return;
        }

        // B40 — Guia do Rei (envia guia completo no chat via messages.yml)
        if (action.equals("KING_GUIDE")) {
            p.closeInventory();
            var msg = plugin.getMessageManager();
            p.sendMessage("");
            p.sendMessage(msg.get("king_guide.header"));
            p.sendMessage(msg.get("king_guide.management_header"));
            p.sendMessage(msg.get("king_guide.management_rename"));
            p.sendMessage(msg.get("king_guide.management_spawn"));
            p.sendMessage(msg.get("king_guide.management_invite"));
            p.sendMessage(msg.get("king_guide.management_kick"));
            p.sendMessage("");
            p.sendMessage(msg.get("king_guide.pvp_header"));
            p.sendMessage(msg.get("king_guide.pvp_global"));
            p.sendMessage(msg.get("king_guide.pvp_residents"));
            p.sendMessage(msg.get("king_guide.pvp_external"));
            p.sendMessage("");
            p.sendMessage(msg.get("king_guide.visit_header"));
            p.sendMessage(msg.get("king_guide.visit_info"));
            p.sendMessage(msg.get("king_guide.visit_command"));
            p.sendMessage("");
            p.sendMessage(msg.get("king_guide.economy_header"));
            p.sendMessage(msg.get("king_guide.economy_info"));
            p.sendMessage(msg.get("king_guide.economy_deposit"));
            p.sendMessage(msg.get("king_guide.economy_withdraw"));
            p.sendMessage("");
            p.sendMessage(msg.get("king_guide.lots_header"));
            p.sendMessage(msg.get("king_guide.lots_tool"));
            p.sendMessage(msg.get("king_guide.lots_price"));
            p.sendMessage(msg.get("king_guide.lots_create"));
            p.sendMessage("");
            p.sendMessage(msg.get("king_guide.diplomacy_header"));
            p.sendMessage(msg.get("king_guide.diplomacy_alliance"));
            p.sendMessage(msg.get("king_guide.diplomacy_enemy"));
            p.sendMessage(msg.get("king_guide.diplomacy_war"));
            p.sendMessage("");
            p.sendMessage(msg.get("king_guide.vote_header"));
            p.sendMessage(msg.get("king_guide.vote_create"));
            p.sendMessage(msg.get("king_guide.footer"));
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            return;
        }

        // B5.3 — Banco Real (redireciona para /reino banco)
        if (action.equals("OPEN_BANK")) {
            p.closeInventory();
            p.performCommand("reino banco");
            return;
        }

        // B40 — Visitar Reinos (envia info via messages.yml)
        if (action.equals("VISIT_KINGDOMS")) {
            p.closeInventory();
            var msg = plugin.getMessageManager();
            p.sendMessage("");
            p.sendMessage(msg.get("visit_kingdoms.header"));
            p.sendMessage(msg.get("visit_kingdoms.info"));
            p.sendMessage(msg.get("visit_kingdoms.command"));
            p.sendMessage("");
            p.sendMessage(msg.get("visit_kingdoms.list_info"));
            p.sendMessage(msg.get("visit_kingdoms.list_command"));
            p.sendMessage("");
            p.sendMessage(msg.get("visit_kingdoms.cost_info"));
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }


        // B7 — Diplomacia
        if (action.equals("OPEN_DIPLOMACY")) {
            plugin.getKingdomInventory().openDiplomacyMenu(p, cityId);
            return;
        }

        // B35 — Convidar Cidadão
        if (action.equals("INVITE_MEMBER")) {
            p.closeInventory();
            p.sendMessage(plugin.getMessageManager().get("kingdom.invite_hint"));
            p.sendMessage("§7Use: §e/reino convidar <nome do jogador>");
            return;
        }

        if (action.equals("DIPLOMACY_KINGDOM")) {
            // data = ID do reino alvo
            // Clique esquerdo = propor aliança; Clique direito = declarar inimigo
            // Shift+Clique = voltar ao neutro
            p.closeInventory();
            if (clickType.isShiftClick()) {
                p.performCommand("reino neutro " + plugin.getKingdomManager().getNome(data));
            } else if (clickType.isRightClick()) {
                p.performCommand("reino inimigo " + plugin.getKingdomManager().getNome(data));
            } else {
                p.performCommand("reino alianca " + plugin.getKingdomManager().getNome(data));
            }
            return;
        }

        if (action.equals("PAGE_PREV") || action.equals("PAGE_NEXT")) {
            int newPage = Integer.parseInt(data);
            InventoryHolder topHolder = p.getOpenInventory().getTopInventory().getHolder();
            if (topHolder instanceof BaseMenu.KingdomHolder kh) {
                if (kh.getMenuType().equals("DIPLOMACY")) {
                    plugin.getKingdomInventory().openDiplomacyMenu(p, cityId, newPage);
                    return;
                }
            }
        }

        if (action.equals("OPEN_GENERAL_PERMS")) {
            // VERIFICAÇÃO DE SEGURANÇA: Apenas Dono ou Vice
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                boolean isStaff = p.isOp() || p.hasPermission("gorvax.admin");
                boolean isOwner = claim.getOwner().equals(p.getUniqueId())
                        || claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE);

                if (!isStaff && !isOwner) {
                    plugin.getMessageManager().send(p, "menu.error_crown_only");
                    return;
                }

                if (isStaff && !isOwner) {
                    plugin.getMessageManager().send(p, "menu.staff_edit_warning");
                }
            }
            plugin.getKingdomInventory().openGeneralPermissionsMenu(p, cityId);
            return;
        }

        if (action.equals("OPEN_FOUNDATION")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                plugin.getKingdomInventory().openFoundationMenu(p, claim);
            }
            return;
        }

        if (action.equals("OPEN_KINGDOM_MENU")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                plugin.getKingdomInventory().openKingdomMenu(p, claim);
            }
            return;
        }

        if (action.equals("OPEN_PLOT_MENU")) {
            // VERIFICAÇÃO DE SEGURANÇA
            Claim cityClaim = plugin.getClaimManager().getClaimById(cityId);
            boolean isStaff = p.isOp() || p.hasPermission("gorvax.admin");
            boolean canManage = isStaff;

            if (cityClaim != null) {
                br.com.gorvax.core.managers.SubPlot plot = cityClaim.getSubPlot(data);
                if (plugin.getKingdomManager().isRei(cityId, p.getUniqueId()))
                    canManage = true;
                if (cityClaim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE))
                    canManage = true;
                if (plot != null && plot.getOwner() != null && plot.getOwner().equals(p.getUniqueId()))
                    canManage = true;

                if (!canManage) {
                    plugin.getMessageManager().send(p, "menu.error_no_plot_permission");
                    p.closeInventory();
                    return;
                }

                if (isStaff && !plugin.getKingdomManager().isRei(cityId, p.getUniqueId()) &&
                        !cityClaim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE) &&
                        !(plot != null && plot.getOwner() != null && plot.getOwner().equals(p.getUniqueId()))) {
                    plugin.getMessageManager().send(p, "menu.staff_edit_warning");
                }
            }

            plugin.getKingdomInventory().openPlotManager(p, cityId, data);
            return;
        }

        // --- AÇÕES GERAIS ---
        if (action.equals("SPAWN")) {
            p.closeInventory();
            p.performCommand("cidade spawn");
            return;
        }

        if (action.equals("BUY_BLOCKS")) {
            double cost = 5000;
            int amount = 100;

            EconomyResponse response = GorvaxCore.getEconomy().withdrawPlayer(p, cost);
            if (response.transactionSuccess()) {
                PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());
                pd.addClaimBlocks(amount);
                plugin.getPlayerDataManager().saveData(p.getUniqueId());

                plugin.getMessageManager().send(p, "menu.blocks_purchased");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } else {
                plugin.getMessageManager().send(p, "menu.error_economy", response.errorMessage);
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            }
            return;
        }

        if (action.equals("BUY_BLOCKS_DISCOUNT")) {
            // Oferta de Prefeito: 50% OFF
            double cost = 2500;
            int amount = 100;

            EconomyResponse response = GorvaxCore.getEconomy().withdrawPlayer(p, cost);
            if (response.transactionSuccess()) {
                PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());
                pd.addClaimBlocks(amount);
                plugin.getPlayerDataManager().saveData(p.getUniqueId());

                plugin.getMessageManager().send(p, "menu.blocks_purchased_discount");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            } else {
                plugin.getMessageManager().send(p, "menu.error_economy", response.errorMessage);
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            }
            return;
        }

        if (action.equals("BUY_MAYOR")) {
            p.closeInventory();
            PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());
            if (!pd.hasKingRank()) {
                EconomyResponse response = GorvaxCore.getEconomy().withdrawPlayer(p, 50000);
                if (response.transactionSuccess()) {
                    // Start promotion - Wait for completion for feedback
                    plugin.getKingdomManager().promoteToKing(p.getUniqueId());

                    // Persist Data locally
                    pd.setKingRank(true);
                    plugin.getPlayerDataManager().saveData(p.getUniqueId());

                    // Link Existing Claims (Nomad -> King)
                    for (Claim c : plugin.getClaimManager().getClaims()) {
                        if (c.getOwner().equals(p.getUniqueId())) {
                            plugin.getKingdomManager().setRei(c.getId(), p.getUniqueId());
                        }
                    }

                    plugin.getMessageManager().send(p, "menu.crown_purchased");
                    plugin.getMessageManager().send(p, "menu.crown_purchased_hint_1");
                    plugin.getMessageManager().send(p, "menu.crown_purchased_hint_2");

                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 50, 0.5, 1, 0.5, 0.1);
                } else {
                    plugin.getMessageManager().send(p, "menu.error_economy", response.errorMessage);
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
                }
            } else {
                plugin.getMessageManager().send(p, "menu.crown_already_owned");
            }
            return;
        }

        if (action.equals("SET_TAG")) {
            p.closeInventory();
            plugin.getInputManager().openAnvilInput(p, "Definir TAG", "", (tag) -> {
                if (tag.length() > 3 || !tag.matches("[a-zA-Z0-9]+")) {
                    plugin.getMessageManager().send(p, "menu.tag_invalid");
                    return;
                }
                Claim claim = plugin.getClaimManager().getClaimById(cityId);
                if (claim != null) {
                    claim.setTag(tag.toUpperCase());
                    plugin.getClaimManager().saveClaims();
                    plugin.getMessageManager().send(p, "menu.tag_success", claim.getTag());
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    plugin.refreshPlayerName(p);
                }
            });
            return;
        }

        if (action.equals("SET_TAG_COLOR")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                String[] colors = { "§f", "§e", "§6", "§b", "§3", "§a", "§2", "§d", "§5", "§c", "§4" };
                String current = claim.getTagColor();
                int index = 0;
                for (int i = 0; i < colors.length; i++) {
                    if (colors[i].equals(current)) {
                        index = (i + 1) % colors.length;
                        break;
                    }
                }
                claim.setTagColor(colors[index]);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                plugin.getKingdomInventory().openAdminMenu(p, cityId);

                // Refresh all residents online
                for (Player online : Bukkit.getOnlinePlayers()) {
                    plugin.refreshPlayerName(online);
                }
            }
            return;
        }

        // --- SISTEMA DE TRUST (GENÉRICO) ---
        if (action.equals("OPEN_TRUST_MENU")) {
            // data = "MAIN" or SubPlotID. But wait, openTrustMenu expects ClaimID
            // separately.
            // The item stores action data. For OPEN_TRUST_MENU in existing calls:
            // In Foundation Menu: data="MAIN" (implied claimId is cityId which is the
            // inventory holder)
            // In Plot Menu: data=plotId

            String subPlotId = data.equals("MAIN") ? null : data;
            plugin.getKingdomInventory().openTrustMenu(p, cityId, subPlotId);
            return;
        }

        if (action.equals("ADD_TRUST_INPUT")) {
            p.closeInventory();
            // Data is SubPlotID or "MAIN"
            String subPlotId = (data == null || data.equals("MAIN")) ? "MAIN" : data;

            // Usando AnvilGUI ou Chat Input conforme disponível no InputManager
            plugin.getInputManager().openPlayerSelectInput(p, "Adicionar Amigo", (uuid) -> {
                if (uuid == null) {
                    plugin.getMessageManager().send(p, "general.player_not_found");
                    // Reabre para tentar novamente
                    plugin.getKingdomInventory().openTrustMenu(p, cityId, subPlotId.equals("MAIN") ? null : subPlotId);
                    return;
                }
                handleTrustInput(p, cityId, subPlotId.equals("MAIN") ? null : subPlotId, uuid);
            });
            return;
        }

        if (action.equals("OPEN_PLAYER_PERMS")) {
            // data format: "MAIN:UUID" or "SubPlotID:UUID"
            String[] parts = data.split(":");
            if (parts.length >= 2) {
                String subId = parts[0];
                String targetUuidStr = parts[1];
                plugin.getKingdomInventory().openPlayerPermissionsMenu(p, cityId, targetUuidStr,
                        subId.equals("MAIN") ? null : subId);
            }
            return;
        }

        if (action.equals("TOGGLE_PLAYER_PERM")) {
            // data format: "PERM_TYPE:Context" -> "PERM_TYPE:SubId:UUID"
            // Ex: "GERAL:MAIN:uuid-1234..."
            String[] parts = data.split(":");
            if (parts.length >= 3) {
                String permTypeStr = parts[0];
                String subId = parts[1];
                String targetUuidStr = parts[2];
                UUID targetId = UUID.fromString(targetUuidStr);
                Claim.TrustType permType = Claim.TrustType.valueOf(permTypeStr);

                Claim claim = plugin.getClaimManager().getClaimById(cityId);
                if (claim != null) {
                    if (subId.equals("MAIN")) {
                        // Toggle Logic
                        if (claim.hasPermission(targetId, permType)) {
                            claim.removeTrust(targetId, permType);
                        } else {
                            claim.addTrust(targetId, permType);
                        }
                    } else {
                        // Subplot
                        for (br.com.gorvax.core.managers.SubPlot plot : claim.getSubPlots()) {
                            if (plot.getId().equals(subId)) {
                                if (plot.hasPermission(targetId, permType)) {
                                    plot.removeTrust(targetId, permType);
                                } else {
                                    plot.addTrust(targetId, permType);
                                }
                                break;
                            }
                        }
                    }
                    plugin.getClaimManager().saveClaims();
                    // Refresh
                    plugin.getKingdomInventory().openPlayerPermissionsMenu(p, cityId, targetUuidStr,
                            subId.equals("MAIN") ? null : subId);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
            }
            return;
        }

        // --- ADMINISTRAÇÃO ---
        if (action.equals("MANAGE_MEMBERS")) {
            plugin.getKingdomInventory().openMembersMenu(p, cityId); // Changed to new menu
            return;
        }

        if (action.equals("BACK_MEMBERS")) {
            plugin.getKingdomInventory().openMembersMenu(p, cityId);
            return;
        }

        if (action.equals("BACK_ADMIN")) {
            plugin.getKingdomInventory().openAdminMenu(p, cityId);
            return;
        }

        if (action.equals("MEMBER_ACTION")) {
            // Data contains target UUID
            plugin.getKingdomInventory().openMemberActionMenu(p, cityId, data);
            return;
        }

        if (action.equals("KICK_MEMBER")) {
            p.closeInventory();
            java.util.UUID targetId = java.util.UUID.fromString(data);

            // Validate if user is mayor
            if (!plugin.getKingdomManager().isRei(cityId, p.getUniqueId())) {
                plugin.getMessageManager().send(p, "kingdom.kick_not_mayor");
                return;
            }

            // Execute Kick
            plugin.getKingdomManager().removeSudito(cityId, targetId);

            // Remove from plots
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                for (br.com.gorvax.core.managers.SubPlot plot : claim.getSubPlots()) {
                    if (targetId.equals(plot.getOwner())) {
                        plot.setOwner(null);
                        plot.setForSale(true); // Reset to sale
                    }
                    if (targetId.equals(plot.getRenter())) {
                        plot.setRenter(null);
                    }
                }
                plugin.getClaimManager().saveClaims();
            }

            plugin.getMessageManager().send(p, "kingdom.kick_success");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                target.sendMessage(
                        plugin.getMessageManager().get("kingdom.kick_notify", plugin.getKingdomManager().getNome(cityId)));
            }
            return;
        }

        if (action.equals("TOGGLE_WELCOME_COLOR")) {
            // Alterna cor de boas vindas
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                String[] colors = { "§b", "§a", "§e", "§6", "§d", "§f" };
                String current = claim.getWelcomeColor();
                String next = colors[0];
                for (int i = 0; i < colors.length; i++) {
                    if (colors[i].equals(current)) {
                        next = colors[(i + 1) % colors.length];
                        break;
                    }
                }
                claim.setWelcomeColor(next);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openAdminMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_CHAT_COLOR")) {
            // Alterna cor do chat
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                // Mesma paleta da TAG para consistência, removendo cores muito escuras se
                // necessário
                String[] colors = { "§f", "§e", "§6", "§b", "§3", "§a", "§2", "§d", "§5", "§c", "§4" };
                String current = claim.getChatColor();
                String next = colors[0];
                for (int i = 0; i < colors.length; i++) {
                    if (colors[i].equals(current)) {
                        next = colors[(i + 1) % colors.length];
                        break;
                    }
                }
                claim.setChatColor(next);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openAdminMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("ADJUST_ECONOMY")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                double current = claim.getTax();
                double next = (current == 2.0) ? 5.0 : (current == 5.0 ? 8.0 : (current == 8.0 ? 10.0 : 2.0));
                claim.setTax(next);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openAdminMenu(p, cityId);
            }
            return;
        }

        if (action.equals("TOGGLE_PVP_GLOBAL")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                boolean newState = !claim.isPvp();
                claim.setPvp(newState);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openAdminMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_PVP_RESIDENTS")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                boolean newState = !claim.isResidentsPvp();
                claim.setResidentsPvp(newState);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openAdminMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_PVP_RESIDENTS_OUTSIDE")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                boolean newState = !claim.isResidentsPvpOutside();
                claim.setResidentsPvpOutside(newState);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openAdminMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_RES_BUILD")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                claim.setResidentsBuild(!claim.isResidentsBuild());
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openGeneralPermissionsMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_RES_CONTAINER")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                claim.setResidentsContainer(!claim.isResidentsContainer());
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openGeneralPermissionsMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_RES_SWITCH")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                claim.setResidentsSwitch(!claim.isResidentsSwitch());
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openGeneralPermissionsMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_ALL_PERMS_ON")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                claim.setResidentsBuild(true);
                claim.setResidentsContainer(true);
                claim.setResidentsSwitch(true);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openGeneralPermissionsMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_ALL_PERMS_OFF")) {
            Claim claim = plugin.getClaimManager().getClaimById(cityId);
            if (claim != null) {
                claim.setResidentsBuild(false);
                claim.setResidentsContainer(false);
                claim.setResidentsSwitch(false);
                plugin.getClaimManager().saveClaims();
                plugin.getKingdomInventory().openGeneralPermissionsMenu(p, cityId);
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            }
            return;
        }

        if (action.equals("TOGGLE_VICE")) {
            java.util.UUID targetId = java.util.UUID.fromString(data);
            Claim claim = plugin.getClaimManager().getClaimById(cityId);

            if (claim != null) {
                boolean isVice = claim.hasPermission(targetId, Claim.TrustType.VICE);
                if (isVice) {
                    // Demote (Remove specific trust, relying on general resident status)
                    claim.removeTrust(targetId);
                    plugin.getMessageManager().send(p, "menu.demoted");
                } else {
                    // Promote
                    claim.addTrust(targetId, Claim.TrustType.VICE);
                    plugin.getMessageManager().send(p, "menu.promoted_vice");
                }
                plugin.getClaimManager().saveClaims();

                // Refresh Menu
                plugin.getKingdomInventory().openMemberActionMenu(p, cityId, data);
            }
            return;
        }

        // --- UPGRADES ---
        if (action.startsWith("UPGRADE_")) {
            handleUpgrade(p, cityId, action);
            return;
        }

        // --- SISTEMA DE LOTES (Action TELEPORT_LOT, Data = PlotID) ---
        if (action.equals("TELEPORT_LOT") && data != null) {
            // Verifica permissão de gerenciamento (Dono, Prefeito ou Vice)
            boolean canManage = false;
            Claim cityClaim = plugin.getClaimManager().getClaimById(cityId);
            if (cityClaim != null) {
                // É Admin da cidade?
                if (plugin.getKingdomManager().isRei(cityId, p.getUniqueId()) ||
                        cityClaim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE)) {
                    canManage = true;
                }
                // É dono do lote?
                br.com.gorvax.core.managers.SubPlot plot = null;
                for (br.com.gorvax.core.managers.SubPlot sp : cityClaim.getSubPlots()) {
                    if (sp.getId().equals(data)) {
                        plot = sp;
                        break;
                    }
                }
                if (plot != null) {
                    if (plot.getOwner() != null && plot.getOwner().equals(p.getUniqueId())) {
                        canManage = true;
                    }
                }
            }

            // Lógica: Se pode gerenciar, Clique Esquerdo (Padrão) abre Gerenciador.
            // Para teleportar, usa o botão DENTRO do gerenciador.
            // Visitantes apenas teleportam.

            if (canManage) {
                // Se for Shift ou Direito, ou se for Bedrock (que só manda Esquerdo), abrimos o
                // Manager.
                // Na verdade, para quem pode gerenciar, o Manager é o padrão agora.
                plugin.getKingdomInventory().openPlotManager(p, cityId, data);

            } else {
                executeTeleport(p, cityId, data);
            }
            return;
        }

        if (action.equals("EXECUTE_TELEPORT") && data != null) {
            executeTeleport(p, cityId, data);
            return;
        }

        // --- PLOT MANAGEMENT ACTIONS ---
        if (action.equals("MANAGE_PLOT_PRICE")) {
            p.closeInventory();
            plugin.getInputManager().openNumericInput(p, "Preço do Lote", 0, (value) -> {
                handlePlotInput(p, cityId, data, "PLOT_PRICE", value);
            });
            return;
        }

        if (action.equals("MANAGE_PLOT_RENT")) {
            p.closeInventory();
            plugin.getInputManager().openNumericInput(p, "Aluguel Diário", 0, (value) -> {
                handlePlotInput(p, cityId, data, "PLOT_RENT", value);
            });
            return;
        }

        if (action.equals("TOGGLE_PLOT_SALE")) {
            togglePlotMode(p, cityId, data, "SALE");
            return;
        }

        if (action.equals("TOGGLE_PLOT_RENT")) {
            togglePlotMode(p, cityId, data, "RENT");
            return;
        }

        if (action.equals("EVICT_PLOT")) {
            evictPlot(p, cityId, data);
            return;
        }

        if (action.equals("DELETE_PLOT_CONFIRM")) {
            plugin.getKingdomInventory().openDeleteConfirmationMenu(p, cityId, data);
            return;
        }

        if (action.equals("CONFIRM_DELETE_PLOT")) {
            deletePlot(p, cityId, data);
            return;
        }

        if (action.equals("ABANDON_PLOT_CONFIRM")) {
            plugin.getKingdomInventory().openAbandonConfirmationMenu(p, cityId, data);
            return;
        }

        if (action.equals("CONFIRM_ABANDON_PLOT")) {
            abandonPlot(p, cityId, data);
            return;
        }

        if (action.equals("CANCEL_DELETE_PLOT")) {
            // Return to plot manager
            plugin.getKingdomInventory().openPlotManager(p, cityId, data);
            return;
        }

        if (action.equals("SET_SPAWN")) {
            p.closeInventory();
            p.performCommand("cidade setspawn");
            return;
        }

        if (action.equals("MANAGE_ENTER_MSG")) {
            if (isShiftClick) {
                // Toggle Color logic
                Claim claim = plugin.getClaimManager().getClaimById(cityId);
                if (claim != null) {
                    String[] colors = { "§b", "§a", "§e", "§6", "§d", "§f" };
                    String current = claim.getWelcomeColor();
                    String next = colors[0];
                    for (int i = 0; i < colors.length; i++) {
                        if (colors[i].equals(current)) {
                            next = colors[(i + 1) % colors.length];
                            break;
                        }
                    }
                    claim.setWelcomeColor(next);
                    plugin.getClaimManager().saveClaims();
                    plugin.getKingdomInventory().openAdminMenu(p, cityId);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
            } else {
                p.closeInventory();
                plugin.getInputManager().openAnvilInput(p, "Msg Entrada", "", (msg) -> {
                    if (msg.length() > 50) {
                        plugin.getMessageManager().send(p, "menu.msg_entry_too_long");
                        return;
                    }
                    Claim claim = plugin.getClaimManager().getClaimById(cityId);
                    if (claim != null) {
                        claim.setEnterTitle(msg);
                        plugin.getClaimManager().saveClaims();
                        plugin.getMessageManager().send(p, "menu.msg_entry_set");
                        plugin.getKingdomInventory().openAdminMenu(p, cityId);
                    }
                });
            }
            return;
        }

        if (action.equals("MANAGE_EXIT_MSG")) {
            p.closeInventory();
            plugin.getInputManager().openAnvilInput(p, "Msg Saída", "", (msg) -> {
                if (msg.length() > 50) {
                    plugin.getMessageManager().send(p, "menu.msg_exit_too_long");
                    return;
                }
                Claim claim = plugin.getClaimManager().getClaimById(cityId);
                if (claim != null) {
                    claim.setExitTitle(msg);
                    plugin.getClaimManager().saveClaims();
                    plugin.getMessageManager().send(p, "menu.msg_exit_set");
                    plugin.getKingdomInventory().openAdminMenu(p, cityId);
                }
            });
            return;
        }

        // --- SISTEMA DE DELEÇÃO DE TERRENO/PLOT (NORMAL PLAYER) ---
        if (action.equals("DELETE_TERRAIN_CONFIRM")) {
            // Abre menu de confirmação
            // O cityId aqui é o ID do Claim (pois estamos no Foundation Menu)
            plugin.getKingdomInventory().openDeleteTerrainConfirmationMenu(p, cityId);
            return;
        }

        if (action.equals("CANCEL_DELETE")) {
            p.closeInventory();
            plugin.getMessageManager().send(p, "general.operation_cancelled");
            return;
        }

        if (action.equals("CONFIRM_DELETE_TERRAIN")) {
            p.closeInventory();
            // data = claimId
            Claim claim = plugin.getClaimManager().getClaimById(data);
            if (claim != null) {
                if (!claim.getOwner().equals(p.getUniqueId()) && !p.hasPermission("gorvax.admin")) {
                    plugin.getMessageManager().send(p, "menu.error_not_owner");
                    return;
                }

                // Remove claim
                // Reembolsar blocos de proteção
                int width = claim.getMaxX() - claim.getMinX() + 1;
                int length = claim.getMaxZ() - claim.getMinZ() + 1;
                int area = width * length;
                plugin.getKingdomManager().devolverBlocos(claim.getOwner(), area);

                // Remove claim
                plugin.getClaimManager().removeClaim(claim);
                plugin.getClaimManager().saveClaims(); // Save changes

                plugin.getMessageManager().send(p, "menu.abandon_success");
                plugin.getMessageManager().send(p, "menu.abandon_hint");
                p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1f, 0.5f);
            } else {
                plugin.getMessageManager().send(p, "menu.error_terrain_not_found");
            }
            return;
        }

        // --- SISTEMA DE DELEÇÃO DE CIDADE (MAYOR) ---
        if (action.equals("DELETE_CITY_CHECK")) {
            // Verificar moradores
            int moradores = plugin.getKingdomManager().getSuditosCount(cityId);
            // moradores count usually includes the mayor? Let's check logic.
            // MenuListener display logic: "População: X cidadãos"
            // Usually we want to allow delete if ONLY the mayor is there.

            // Vamos checar a lista de moradores manualmente para ter certeza
            java.util.UUID prefeito = plugin.getKingdomManager().getRei(cityId);
            java.util.List<String> residents = plugin.getKingdomManager().getSuditosList(cityId);

            // Se tiver alguém que NÃO é o prefeito, bloqueia.
            boolean hasResidents = false;
            for (String uuidStr : residents) {
                if (!uuidStr.equals(prefeito.toString())) {
                    hasResidents = true;
                    break;
                }
            }

            if (hasResidents) {
                plugin.getMessageManager().send(p, "menu.error_active_residents");
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
                p.closeInventory();
            } else {
                plugin.getKingdomInventory().openDeleteCityConfirmationMenu(p, cityId);
            }
            return;
        }

        if (action.equals("CONFIRM_DELETE_CITY")) {
            p.closeInventory();
            // Executar deleção da cidade
            // 1. Remover city status do claim
            // 2. Apagar dados da cidade no CityManager
            // 3. Remover claim

            Claim claim = plugin.getClaimManager().getClaimById(cityId); // cityId == claimId for main claim
            if (claim != null) {
                if (!plugin.getKingdomManager().isRei(cityId, p.getUniqueId()) && !p.hasPermission("gorvax.admin")) {
                    plugin.getMessageManager().send(p, "menu.error_mayor_only");
                    return;
                }

                String nomeCidade = plugin.getKingdomManager().getNome(cityId);

                // Remove City Data & Claim (Handled by CityManager)
                // Reembolso de blocos agora é centralizado dentro de deleteKingdom()
                plugin.getKingdomManager().deleteKingdom(cityId);
                plugin.getMessageManager().send(p, "menu.refund_blocks");

                // Anúncio Global
                plugin.getMessageManager().broadcast("menu.delete_broadcast", nomeCidade);

                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);

                // Refresh player to remove tag immediately
                plugin.refreshPlayerName(p);

                // Refresh all just in case (e.g. tablist updates)
                for (Player online : Bukkit.getOnlinePlayers()) {
                    plugin.refreshPlayerName(online);
                }

            } else {
                plugin.getMessageManager().send(p, "menu.error_city_not_found");
            }
            return;
        }
    }

    private void handleUpgrade(Player p, String cityId, String action) {
        // PERMISSION CHECK: Apenas o Prefeito pode evoluir a cidade
        if (!plugin.getKingdomManager().isRei(cityId, p.getUniqueId())) {
            plugin.getMessageManager().send(p, "menu.error_mayor_upgrade");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            return;
        }

        // action ex: "UPGRADE_XP"
        String type = action.replace("UPGRADE_", "");
        double custo = 0;
        String nome = "";
        int nivelAtual = 0;

        if (type.equals("PRESERVACAO")) {
            nivelAtual = plugin.getKingdomManager().getNivelPreservacao(cityId);
            custo = (nivelAtual + 1) * 15000.0;
            nome = "Preservação de Alma";
        } else if (type.equals("XP")) {
            nivelAtual = plugin.getKingdomManager().getNivel(cityId, "xp");
            custo = (nivelAtual + 1) * 5000.0;
            nome = "Bônus de XP";
        } else if (type.equals("SORTE")) {
            nivelAtual = plugin.getKingdomManager().getNivelSorte(cityId);
            custo = (nivelAtual + 1) * 20000.0;
            nome = "Sorte do Trabalhador";
        } else if (type.equals("SPEED")) {
            nivelAtual = plugin.getKingdomManager().getNivel(cityId, "speed");
            if (nivelAtual >= 2)
                return; // Max level
            custo = (nivelAtual + 1) * 30000.0;
            nome = "Agilidade Urbana";
        } else if (type.equals("EXTENSION")) {
            nivelAtual = plugin.getKingdomManager().getNivel(cityId, "extension");
            // Max Level check? User didn't specify, but 3 seems reasonable or maybe
            // unlimited.
            // Let's implement up to level 5 just in case.
            if (nivelAtual >= 5)
                return;
            custo = (nivelAtual + 1) * 50000.0;
            nome = "Expansão de Influência";
        }

        EconomyResponse response = GorvaxCore.getEconomy().withdrawPlayer(p, custo);
        if (response.transactionSuccess()) {
            // Aplica
            if (type.equals("PRESERVACAO")) {
                plugin.getKingdomManager().setNivelPreservacao(cityId, nivelAtual + 1);
            } else if (type.equals("SORTE")) {
                plugin.getKingdomManager().setNivelSorte(cityId, nivelAtual + 1);
            } else {
                // XP e SPEED usam o padrão "nivel_tipo" (lowercase)
                plugin.getKingdomManager().setNivel(cityId, type.toLowerCase(), nivelAtual + 1);
            }
            plugin.getKingdomManager().saveData();

            plugin.getMessageManager().send(p, "menu.upgrade_success", nome, nivelAtual + 1);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            plugin.getKingdomInventory().openMainMenu(p);
        } else {
            plugin.getMessageManager().send(p, "menu.upgrade_no_money", response.errorMessage);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void executeTeleport(Player p, String cityId, String plotId) {
        // Encontra o lote dentro da cidade
        Claim cityClaim = plugin.getClaimManager().getClaimById(cityId);
        if (cityClaim == null)
            return;

        br.com.gorvax.core.managers.SubPlot target = null;
        for (br.com.gorvax.core.managers.SubPlot sub : cityClaim.getSubPlots()) {
            if (sub.getId().equals(plotId)) {
                target = sub;
                break;
            }
        }

        if (target != null) {
            // Calcula centro do lote
            int x = (target.getMinX() + target.getMaxX()) / 2;
            int z = (target.getMinZ() + target.getMaxZ()) / 2;
            org.bukkit.World w = Bukkit.getWorld(cityClaim.getWorldName());
            int y = w.getHighestBlockYAt(x, z) + 1;

            p.teleport(new Location(w, x, y, z));
            plugin.getMessageManager().send(p, "menu.teleport_plot", target.getName());
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            p.closeInventory();
        } else {
            plugin.getMessageManager().send(p, "menu.error_plot_not_found");
        }
    }

    private boolean canManagePlot(Player p, Claim city, br.com.gorvax.core.managers.SubPlot plot) {
        if (p.isOp() || p.hasPermission("gorvax.admin"))
            return true;
        if (city.getOwner().equals(p.getUniqueId()))
            return true; // Mayor
        if (city.hasPermission(p.getUniqueId(), Claim.TrustType.VICE))
            return true; // Vice
        // Owner only if plot is theirs
        return plot.getOwner() != null && plot.getOwner().equals(p.getUniqueId());
    }

    private void handlePlotInput(Player p, String cityId, String plotId, String type, double value) {
        if (value < 0) {
            plugin.getMessageManager().send(p, "general.invalid_value");
            return;
        }

        Claim claim = plugin.getClaimManager().getClaimById(cityId);
        if (claim != null) {
            for (br.com.gorvax.core.managers.SubPlot plot : claim.getSubPlots()) {
                if (plot.getId().equals(plotId)) {
                    // SECURITY CHECK
                    if (!canManagePlot(p, claim, plot)) {
                        plugin.getMessageManager().send(p, "menu.error_no_plot_permission");
                        return;
                    }

                    if (type.equals("PLOT_PRICE")) {
                        plot.setPrice(value);
                        plugin.getMessageManager().send(p, "plot.price_set", value);
                    } else if (type.equals("PLOT_RENT")) {
                        plot.setRentPrice(value);
                        plugin.getMessageManager().send(p, "plot.rent_price_set", value);
                    }
                    plugin.getClaimManager().saveClaims();

                    // Re-open menu on sync thread
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.getKingdomInventory().openPlotManager(p, cityId, plotId);
                        }
                    }.runTask(plugin);
                    return;
                }
            }
        }
    }

    private void togglePlotMode(Player p, String cityId, String plotId, String mode) {
        Claim claim = plugin.getClaimManager().getClaimById(cityId);
        if (claim != null) {
            for (br.com.gorvax.core.managers.SubPlot plot : claim.getSubPlots()) {
                if (plot.getId().equals(plotId)) {
                    // SECURITY CHECK
                    if (!canManagePlot(p, claim, plot)) {
                        plugin.getMessageManager().send(p, "menu.error_no_plot_permission");
                        return;
                    }

                    if (mode.equals("SALE")) {
                        plot.setForSale(!plot.isForSale());
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    } else if (mode.equals("RENT")) {
                        plot.setForRent(!plot.isForRent());
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                    plugin.getClaimManager().saveClaims();
                    plugin.getKingdomInventory().openPlotManager(p, cityId, plotId);
                    return;
                }
            }
        }
    }

    private void evictPlot(Player p, String cityId, String plotId) {
        Claim claim = plugin.getClaimManager().getClaimById(cityId);
        if (claim != null) {
            // SECURITY CHECK: Apenas Admin/Prefeito/Vice pode despejar (Menu já filtra, mas
            // proteção extra é boa)
            boolean isAdmin = p.hasPermission("gorvax.admin") ||
                    claim.getOwner().equals(p.getUniqueId()) ||
                    claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE);

            if (!isAdmin) {
                plugin.getMessageManager().send(p, "general.no_permission");
                return;
            }

            for (br.com.gorvax.core.managers.SubPlot plot : claim.getSubPlots()) {
                if (plot.getId().equals(plotId)) {
                    // Refund if there is an owner
                    if (plot.getOwner() != null) {
                        double price = plot.getPrice();
                        if (price > 0) {
                            GorvaxCore.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(plot.getOwner()), price);
                            Player target = Bukkit.getPlayer(plot.getOwner());
                            if (target != null) {
                                target.sendMessage(plugin.getMessageManager().get("maintenance.evicted", plot.getName()));
                            }
                        }
                    }

                    plot.setOwner(null);
                    plot.setRenter(null);
                    plot.setForSale(true); // Reset to sale automatically
                    plugin.getClaimManager().saveClaims();

                    plugin.getMessageManager().send(p, "menu.teleport_plot", plot.getName());
                    p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    plugin.getKingdomInventory().openPlotManager(p, cityId, plotId);
                    return;
                }
            }
        }
    }

    private void deletePlot(Player p, String cityId, String plotId) {
        Claim claim = plugin.getClaimManager().getClaimById(cityId);
        if (claim != null) {
            // SECURITY CHECK: Admin/Prefeito/Vice only
            boolean isAdmin = p.hasPermission("gorvax.admin") ||
                    claim.getOwner().equals(p.getUniqueId()) ||
                    claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE);
            if (!isAdmin) {
                plugin.getMessageManager().send(p, "general.no_permission");
                return;
            }

            br.com.gorvax.core.managers.SubPlot toRemove = null;
            for (br.com.gorvax.core.managers.SubPlot plot : claim.getSubPlots()) {
                if (plot.getId().equals(plotId)) {
                    toRemove = plot;
                    break;
                }
            }

            if (toRemove != null) {
                // Refund before delete? Safe removal usually implies cleaning up.
                if (toRemove.getOwner() != null) {
                    double price = toRemove.getPrice();
                    if (price > 0) {
                        GorvaxCore.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(toRemove.getOwner()), price);
                    }
                }

                claim.removeSubPlot(toRemove);
                plugin.getClaimManager().saveClaims();

                p.sendMessage(plugin.getMessageManager().get("plot.confiscate_success"));
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 1f);
                plugin.getKingdomInventory().openLotsMenu(p, cityId);
            }
        }
    }

    private void abandonPlot(Player p, String cityId, String plotId) {
        Claim claim = plugin.getClaimManager().getClaimById(cityId);
        if (claim == null)
            return;

        br.com.gorvax.core.managers.SubPlot plot = null;
        for (br.com.gorvax.core.managers.SubPlot s : claim.getSubPlots()) {
            if (s.getId().equals(plotId)) {
                plot = s;
                break;
            }
        }

        if (plot == null) {
            plugin.getMessageManager().send(p, "menu.error_plot_not_found");
            return;
        }

        // SECURITY CHECK implicitly handled but explicit is better
        if (plot.getOwner() == null || !plot.getOwner().equals(p.getUniqueId())) {
            plugin.getMessageManager().send(p, "plot.error_not_owner");
            return;
        }

        plot.setOwner(null);
        plot.setRenter(null);
        plot.setForSale(false);
        plot.setPrice(0);
        plot.getTrustedPlayers().clear();

        plugin.getClaimManager().saveClaims();
        plugin.getKingdomManager().setPrecoLote(cityId, plotId, 0);

        plugin.getMessageManager().send(p, "plot.abandon_success");
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

        boolean hasOtherPlots = false;
        for (br.com.gorvax.core.managers.SubPlot s : claim.getSubPlots()) {
            if (s.getOwner() != null && s.getOwner().equals(p.getUniqueId())) {
                hasOtherPlots = true;
                break;
            }
        }

        if (!hasOtherPlots) {
            if (!plugin.getKingdomManager().isRei(cityId, p.getUniqueId())) {
                plugin.getKingdomManager().removeSudito(cityId, p.getUniqueId());
                p.sendMessage(plugin.getMessageManager().get("menu.abandon_success"));
            }
        }

        p.closeInventory();
    }

    private String getPdcString(ItemMeta meta, NamespacedKey key) {
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }
        return null;
    }
}