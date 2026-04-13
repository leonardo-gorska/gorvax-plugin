package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.SubPlot;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Menus de lotes (feudos), gerenciamento individual e "minhas terras".
 */
public class KingdomLotsMenu extends BaseMenu {

    public KingdomLotsMenu(GorvaxCore plugin) {
        super(plugin);
    }

    public void openLotsMenu(Player player, String kingdomId) {
        openLotsMenu(player, kingdomId, 0);
    }

    public void openLotsMenu(Player player, String kingdomId, int page) {
        String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
        // B4.2 — Menu adaptativo
        int size = getMenuSize(player, 54, 27);
        Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "LOTS", page), size,
                LegacyComponentSerializer.legacySection()
                        .deserialize("§8Lotes: §b" + (nomeReino != null ? nomeReino : kingdomId)));
        fillBorders(gui, kingdomId);

        Claim mainClaim = plugin.getClaimManager().getClaimById(kingdomId);
        if (mainClaim == null)
            return;

        List<SubPlot> subPlots = mainClaim.getSubPlots();
        if (subPlots.isEmpty()) {
            gui.setItem(size <= 27 ? 13 : 22, createItem(Material.BARRIER, "§c§lNenhuma Terra Demarcada", null, null,
                    "§7Este reino ainda não possui",
                    "§7lotes cadastrados no sistema.",
                    "",
                    "§e💡 Como criar lotes:",
                    "§f  1. Selecione com a §ePá de Ferro",
                    "§f  2. Use §e/lote criar <nome>",
                    "§f  3. Defina preço com §e/lote preco"));
        } else {
            // B4.2 — Slots adaptativos
            int[] slots = getContentSlots(size);
            int itemsPerPage = slots.length;
            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, subPlots.size());

            for (int i = start; i < end; i++) {
                SubPlot plot = subPlots.get(i);
                boolean isRented = plot.isForRent() && plot.getRenter() != null;
                boolean isAvailableRent = plot.isForRent() && plot.getRenter() == null;
                boolean isForSale = plot.isForSale() && plot.getOwner() == null;

                Material mat;
                String display;
                if (isAvailableRent) {
                    mat = Material.EMERALD_BLOCK;
                    display = "§a§l" + plot.getName() + " §7(Para Arrendar)";
                } else if (isForSale) {
                    mat = Material.GOLD_BLOCK;
                    display = "§e§l" + plot.getName() + " §7(À Venda)";
                } else if (isRented) {
                    mat = Material.REDSTONE_BLOCK;
                    display = "§c§l" + plot.getName() + " §7(Arrendado)";
                } else {
                    mat = Material.BRICKS;
                    display = "§7§l" + plot.getName() + " §7(Ocupado)";
                }

                List<String> lore = new ArrayList<>();
                lore.add("§fProprietário: §7" + getPlayerName(plot.getOwner()));
                if (isRented)
                    lore.add("§fArrendatário: §e" + getPlayerName(plot.getRenter()));
                lore.add("§fÁrea: §e" + ((plot.getMaxX() - plot.getMinX() + 1) * (plot.getMaxZ() - plot.getMinZ() + 1))
                        + " blocos");
                lore.add("§7Localização: " + plot.getMinX() + ", " + plot.getMinZ());

                if (isAvailableRent) {
                    lore.add("§fAluguel: §6$" + (int) plot.getRentPrice() + "/dia");
                    lore.add("");
                    lore.add("§b§l> §fClique para arrendar");
                } else if (isForSale) {
                    lore.add("§fPreço: §6$" + (int) plot.getPrice());
                    lore.add("");
                    lore.add("§b§l> §fClique para teletransportar/comprar");
                } else {
                    lore.add(isRented ? "§c§lARRENDADO" : "§c§lOCUPADO");
                    lore.add("");
                    lore.add("§7Clique para visitar");
                }

                boolean canManage = mainClaim.hasPermission(player.getUniqueId(), Claim.TrustType.VICE);
                if (!canManage && plot.getOwner() != null && plot.getOwner().equals(player.getUniqueId()))
                    canManage = true;
                if (!canManage && plot.getRenter() != null && plot.getRenter().equals(player.getUniqueId()))
                    canManage = true;
                if (canManage) {
                    lore.add("");
                    lore.add("§dShift + Click para gerenciar este feudo.");
                }

                gui.setItem(slots[i - start],
                        createItem(mat, display, "TELEPORT_LOT", plot.getId(), lore.toArray(new String[0])));
            }

            // Navegação adaptativa
            int navPrev = size <= 27 ? 18 : 45;
            int navNext = size - 1;

            if (page > 0)
                gui.setItem(navPrev, createItem(Material.ARROW, "§aPágina Anterior", "PREVIOUS_PAGE", "LOTS",
                        "§7Ir para página " + page));
            if (end < subPlots.size())
                gui.setItem(navNext, createItem(Material.ARROW, "§aPróxima Página", "NEXT_PAGE", "LOTS",
                        "§7Ir para página " + (page + 2)));
        }

        int navBack = size <= 27 ? 22 : 49;
        gui.setItem(navBack, createItem(Material.ARROW, "§cVoltar", "BACK", null, "§7Voltar ao menu principal"));
        player.openInventory(gui);
    }

    public void openPlotManager(Player player, String kingdomId, String plotId) {
        Claim kingdomClaim = plugin.getClaimManager().getClaimById(kingdomId);
        if (kingdomClaim == null)
            return;

        SubPlot plot = null;
        for (SubPlot p : kingdomClaim.getSubPlots()) {
            if (p.getId().equals(plotId)) {
                plot = p;
                break;
            }
        }
        if (plot == null)
            return;

        Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "PLOT_MANAGER"), 27,
                LegacyComponentSerializer.legacySection().deserialize("§8Gerenciar Feudo: " + plot.getName()));
        fillBorders(gui, kingdomId);

        gui.setItem(4, createItem(Material.PAPER, "§e§lInfo do Feudo", null, null,
                "§7Nome: §f" + plot.getName(),
                "§7Senhor: §f" + getPlayerName(plot.getOwner()),
                "§7Arrendatário: §f" + getPlayerName(plot.getRenter())));

        gui.setItem(10, createItem(Material.EMERALD, "§a§lDefinir Preço (Venda)", "MANAGE_PLOT_PRICE", plotId,
                "§7Preço atual: §6$" + plot.getPrice(),
                "§7Status: " + (plot.isForSale() ? "§aÀ Venda" : "§cNão Listado"), "", "§e[Clique para Alterar]"));

        gui.setItem(11, createItem(Material.LEVER, "§7Alternar Venda", "TOGGLE_PLOT_SALE", plotId,
                "§7Ativar/Desativar modo de venda.", "§7Status: " + (plot.isForSale() ? "§aATIVADO" : "§cDESATIVADO")));

        gui.setItem(13, createItem(Material.GOLD_NUGGET, "§6§lDefinir Arrendamento", "MANAGE_PLOT_RENT", plotId,
                "§7Valor atual: §6$" + plot.getRentPrice() + "/dia",
                "§7Status: " + (plot.isForRent() ? "§aPara Arrendar" : "§cNão Listado"), "",
                "§e[Clique para Alterar]"));

        gui.setItem(14, createItem(Material.LEVER, "§7Alternar Arrendamento", "TOGGLE_PLOT_RENT", plotId,
                "§7Ativar/Desativar modo aluguel.", "§7Status: " + (plot.isForRent() ? "§aATIVADO" : "§cDESATIVADO")));

        boolean isKingdomAdmin = kingdomClaim.getOwner().equals(player.getUniqueId())
                || kingdomClaim.hasPermission(player.getUniqueId(), Claim.TrustType.VICE);
        boolean isPlotOwner = plot.getOwner() != null && plot.getOwner().equals(player.getUniqueId());

        if (isKingdomAdmin) {
            gui.setItem(16, createItem(Material.TNT, "§c§lConfiscar/Resetar", "EVICT_PLOT", plotId,
                    "§7Remove senhor e arrendatário.", "§7Retorna a terra à coroa.", "", "§4[Clique para Confirmar]"));
            gui.setItem(17, createItem(Material.LAVA_BUCKET, "§4§lAPAGAR FEUDO", "DELETE_PLOT_CONFIRM", plotId,
                    "§7Exclui permanentemente este lote.", "§7Esta ação não pode ser desfeita.", "",
                    "§4[Clique para Apagar]"));
        } else if (isPlotOwner) {
            gui.setItem(16, createItem(Material.RED_SHULKER_BOX, "§c§lAbandonar Feudo", "ABANDON_PLOT_CONFIRM", plotId,
                    "§7Renuncia a posse da terra.", "§7Ela voltará para a coroa.", "", "§4[Clique para Confirmar]"));
        }

        gui.setItem(25, createItem(Material.WRITABLE_BOOK, "§d§lPermissões do Feudo", "OPEN_TRUST_MENU", plotId,
                "§7Adicione aliados a esta terra.", "", "§e[Clique para Gerenciar]"));
        gui.setItem(26, createItem(Material.ENDER_PEARL, "§b§lTeleportar", "EXECUTE_TELEPORT", plotId,
                "§7Teleportar-se para o lote.", "", "§e[Clique para Ir]"));
        gui.setItem(22, createItem(Material.ARROW, "§cVoltar", "VIEW_LOTS", null, "§7Voltar a lista de feudos"));

        player.openInventory(gui);
    }

    public void openMyPlotsMenu(Player player, String kingdomId) {
        String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
        // B4.2 — Menu adaptativo
        int size = getMenuSize(player, 54, 27);
        Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "MY_PLOTS"), size,
                LegacyComponentSerializer.legacySection()
                        .deserialize("§8Minhas Terras: §b" + (nomeReino != null ? nomeReino : kingdomId)));
        fillBorders(gui, kingdomId);

        Claim mainClaim = plugin.getClaimManager().getClaimById(kingdomId);
        if (mainClaim == null)
            return;

        List<SubPlot> subPlots = mainClaim.getSubPlots();
        int found = 0;

        for (SubPlot plot : subPlots) {
            boolean isOwner = plot.getOwner() != null && plot.getOwner().equals(player.getUniqueId());
            boolean isRenter = plot.getRenter() != null && plot.getRenter().equals(player.getUniqueId());
            if (isOwner || isRenter) {
                found++;
                Material mat = isOwner ? Material.OAK_SIGN : Material.BIRCH_SIGN;
                List<String> lore = new ArrayList<>();
                lore.add("§fStatus: " + (isOwner ? "§aProprietário" : "§bArrendatário"));
                lore.add("§fÁrea: §e" + ((plot.getMaxX() - plot.getMinX() + 1) * (plot.getMaxZ() - plot.getMinZ() + 1))
                        + " blocos");
                lore.add("§7Localização: " + plot.getMinX() + ", " + plot.getMinZ());
                if (isRenter)
                    lore.add("§fArrendamento: §6$" + plot.getRentPrice() + "/dia");
                lore.add("");
                lore.add("§bClique para abrir o menu");
                gui.addItem(createItem(mat, "§e§l" + plot.getName(), "TELEPORT_LOT", plot.getId(),
                        lore.toArray(new String[0])));
            }
        }

        if (found == 0) {
            gui.setItem(size <= 27 ? 13 : 22, createItem(Material.BARRIER, "§cNenhum lote encontrado", null, null,
                    "§7Você não possui lotes neste reino."));
        }

        int navBack = size <= 27 ? 22 : 49;
        gui.setItem(navBack, createItem(Material.ARROW, "§cVoltar", "BACK", null, "§7Voltar ao menu principal"));
        player.openInventory(gui);
    }
}
