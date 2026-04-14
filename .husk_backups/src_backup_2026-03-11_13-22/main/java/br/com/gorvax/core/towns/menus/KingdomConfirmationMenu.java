package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menus de confirmação para ações destrutivas (exclusão de lotes, terrenos e
 * reinos).
 */
public class KingdomConfirmationMenu extends BaseMenu {

        public KingdomConfirmationMenu(GorvaxCore plugin) {
                super(plugin);
        }

        public void openDeleteConfirmationMenu(Player player, String kingdomId, String plotId) {
                Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "CONFIRM_DELETE"), 27,
                                LegacyComponentSerializer.legacySection().deserialize("§4Confirmar Exclusão?"));
                fillBorders(gui, kingdomId);

                gui.setItem(13, createItem(Material.PAPER, "§eFeudo a ser apagado", null, null, "§7ID: " + plotId));
                gui.setItem(11, createItem(Material.LIME_WOOL, "§a§lCONFIRMAR", "CONFIRM_DELETE_PLOT", plotId,
                                "§7Sim, quero apagar este lote."));
                gui.setItem(15, createItem(Material.RED_WOOL, "§c§lCANCELAR", "CANCEL_DELETE_PLOT", plotId,
                                "§7Não, volte ao menu anterior."));

                player.openInventory(gui);
        }

        public void openDeleteTerrainConfirmationMenu(Player player, String claimId) {
                Inventory gui = Bukkit.createInventory(new KingdomHolder(claimId, "CONFIRM_DELETE_TERRAIN"), 27,
                                LegacyComponentSerializer.legacySection().deserialize("§4Abandonar Território?"));
                fillBorders(gui, claimId);

                gui.setItem(13, createItem(Material.PAPER, "§eTerreno a ser abandonado", null, null,
                                "§7ID: " + claimId));
                gui.setItem(11, createItem(Material.LIME_WOOL, "§a§lCONFIRMAR", "CONFIRM_DELETE_TERRAIN", claimId,
                                "§7Sim, quero abandonar esta terra."));
                gui.setItem(15, createItem(Material.RED_WOOL, "§c§lCANCELAR", "CANCEL_DELETE", null,
                                "§7Não, manter terreno."));

                player.openInventory(gui);
        }

        public void openAbandonConfirmationMenu(Player player, String kingdomId, String plotId) {
                Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "CONFIRM_ABANDON"), 27,
                                LegacyComponentSerializer.legacySection().deserialize("§4Abandonar Feudo?"));
                fillBorders(gui, kingdomId);

                gui.setItem(13, createItem(Material.PAPER, "§eFeudo a ser abandonado", null, null, "§7ID: " + plotId));
                gui.setItem(11, createItem(Material.LIME_WOOL, "§a§lCONFIRMAR", "CONFIRM_ABANDON_PLOT", plotId,
                                "§7Sim, quero abandonar este feudo."));
                gui.setItem(15, createItem(Material.RED_WOOL, "§c§lCANCELAR", "CANCEL_DELETE_PLOT", plotId,
                                "§7Cancelar"));

                player.openInventory(gui);
        }

        public void openDeleteCityConfirmationMenu(Player player, String kingdomId) {
                String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
                Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "CONFIRM_DELETE_CITY"), 27,
                                LegacyComponentSerializer.legacySection().deserialize("§4DISSOLVER REINO?"));
                fillBorders(gui, kingdomId);

                gui.setItem(13, createItem(Material.PAPER, "§eReino a ser dissolvido", null, null,
                                "§7Nome: " + nomeReino,
                                "§c§lATENÇÃO: AÇÃO IRREVERSÍVEL"));
                gui.setItem(11, createItem(Material.LIME_WOOL, "§a§lCONFIRMAR EXCLUSÃO", "CONFIRM_DELETE_CITY",
                                kingdomId,
                                "§7Sim, dissolver o reino para sempre."));
                gui.setItem(15, createItem(Material.RED_WOOL, "§c§lCANCELAR", "BACK_ADMIN", null,
                                "§7Não, voltar a corte."));

                player.openInventory(gui);
        }
}
