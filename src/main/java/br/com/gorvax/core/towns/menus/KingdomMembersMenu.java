package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menus de gestão de membros (súditos) do reino.
 */
public class KingdomMembersMenu extends BaseMenu {

    public KingdomMembersMenu(GorvaxCore plugin) {
        super(plugin);
    }

    public void openMembersMenu(Player player, String kingdomId) {
        openMembersMenu(player, kingdomId, 0);
    }

    public void openMembersMenu(Player player, String kingdomId, int page) {
        String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
        // B4.2 — Menu adaptativo: 27 slots para Bedrock, 54 para Java
        int size = getMenuSize(player, 54, 27);
        Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "MEMBERS", page), size,
                LegacyComponentSerializer.legacySection()
                        .deserialize("§8Súditos: §b" + (nomeReino != null ? nomeReino : kingdomId)));

        fillBorders(gui, kingdomId);

        List<String> suditos = plugin.getKingdomManager().getSuditosList(kingdomId);
        UUID reiUUID = plugin.getKingdomManager().getRei(kingdomId);
        boolean isViewerRei = plugin.getKingdomManager().isRei(kingdomId, player.getUniqueId());

        // Header: Rei (Slot 4)
        if (reiUUID != null) {
            ItemStack skull = createSkull(reiUUID,
                    "§b§l[Rei] " + getPlayerName(reiUUID),
                    "MEMBER_ACTION", reiUUID.toString(),
                    "§7O Soberano.");
            gui.setItem(4, skull);
        }

        // B4.2 — Slots de conteúdo adaptativos
        int[] slots = getContentSlots(size);

        // Filtra o rei da lista para não repetir na paginação
        List<String> filteredSuditos = new ArrayList<>(suditos);
        if (reiUUID != null)
            filteredSuditos.remove(reiUUID.toString());

        int itemsPerPage = slots.length;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredSuditos.size());

        for (int i = start; i < end; i++) {
            UUID uuid = UUID.fromString(filteredSuditos.get(i));
            String name = getPlayerName(uuid);
            boolean isDuque = plugin.getClaimManager().getClaimById(kingdomId).hasPermission(uuid,
                    Claim.TrustType.VICE);

            String role = isDuque ? "§6Duque (Vice)" : "§7Súdito";

            List<String> lore = new ArrayList<>();
            lore.add(role);
            if (isViewerRei) {
                lore.add("");
                lore.add("§eClique para Gerenciar");
            }

            ItemStack skull = createSkull(uuid, (isDuque ? "§6" : "§f") + name, "MEMBER_ACTION", uuid.toString(),
                    lore.toArray(new String[0]));
            gui.setItem(slots[i - start], skull);
        }

        // Navegação — posição adaptativa ao tamanho do menu
        int navPrev = size <= 27 ? 18 : 45;
        int navNext = size - 1;
        int navBack = size <= 27 ? 22 : 49;

        if (page > 0) {
            gui.setItem(navPrev, createItem(Material.ARROW, "§aPágina Anterior", "PREVIOUS_PAGE", "MEMBERS",
                    "§7Ir para página " + page));
        }
        if (end < filteredSuditos.size()) {
            gui.setItem(navNext, createItem(Material.ARROW, "§aPróxima Página", "NEXT_PAGE", "MEMBERS",
                    "§7Ir para página " + (page + 2)));
        }

        gui.setItem(navBack, createItem(Material.ARROW, "§cVoltar", "BACK_ADMIN", null, "§7Voltar a Corte Real"));
        player.openInventory(gui);
    }

    public void openMemberActionMenu(Player player, String kingdomId, String targetUuidStr) {
        UUID targetUUID = UUID.fromString(targetUuidStr);
        String targetName = getPlayerName(targetUUID);

        Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "MEMBER_ACTION"), 27,
                LegacyComponentSerializer.legacySection().deserialize("§8Gerenciar: " + targetName));
        fillBorders(gui, kingdomId);

        // Center Head
        gui.setItem(4, createSkull(targetUUID, "§e" + targetName, null, null));

        // [11] Expulsar - Exilar
        gui.setItem(11, createItem(Material.BARRIER, "§c§lExilar do Reino", "KICK_MEMBER", targetUuidStr,
                "§7Remove o jogador do reino.",
                "§7Se ele tiver terras, elas",
                "§7serão confiscadas pela coroa.",
                "",
                "§4[Clique para Confirmar]"));

        // [13] Cargo / Rank
        boolean isRei = false;
        boolean isDuque = false;
        Claim c = plugin.getClaimManager().getClaimById(kingdomId);

        if (c != null) {
            isRei = c.getOwner() != null && c.getOwner().equals(targetUUID);
            if (!isRei) {
                isDuque = c.hasPermission(targetUUID, Claim.TrustType.VICE);
            }
        }

        if (isRei) {
            gui.setItem(13, createItem(Material.DIAMOND_CHESTPLATE, "§b§lTítulo: Rei", null, null,
                    "§7Este jogador é o §bRei §7destas terras.",
                    "§7Não é possível alterar este título.",
                    "",
                    "§cTítulo Vitalício"));
        } else {
            gui.setItem(13, createItem(Material.GOLDEN_CHESTPLATE,
                    "§6§lTítulo: " + (isDuque ? "Duque" : "Súdito"),
                    "TOGGLE_VICE", targetUuidStr,
                    "§7Status atual: " + (isDuque ? "§e§lDUQUE" : "§7Súdito"),
                    "",
                    "§7Duques podem gerenciar",
                    "§7feudos e convidar membros.",
                    "",
                    "§e[Clique para Alternar]"));
        }

        gui.setItem(15, createItem(Material.WRITABLE_BOOK, "§e§lGerenciar Permissões",
                "OPEN_PLAYER_PERMS", "MAIN:" + targetUuidStr,
                "§7Defina permissões individuais",
                "§7(Construir, Baús, Interagir)",
                "",
                "§e[Clique para Abrir]"));

        gui.setItem(22, createItem(Material.ARROW, "§cVoltar", "BACK_MEMBERS", null, "§7Voltar a lista"));

        player.openInventory(gui);
    }
}
