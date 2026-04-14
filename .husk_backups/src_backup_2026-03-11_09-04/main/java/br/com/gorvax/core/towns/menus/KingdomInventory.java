package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.SubPlot;
import org.bukkit.entity.Player;

/**
 * Fachada (Facade) para o sistema de menus do reino.
 * Mantém a mesma API pública para retrocompatibilidade com MenuListener,
 * KingdomCommand e GorvaxCore. Delega para sub-menus especializados.
 *
 * B10 — Refatoração de 1314 linhas em 7 classes menores.
 */
public class KingdomInventory {

    private final KingdomMainMenu mainMenu;
    private final KingdomAdminMenu adminMenu;
    private final KingdomMembersMenu membersMenu;
    private final KingdomLotsMenu lotsMenu;
    private final KingdomPermissionsMenu permissionsMenu;
    private final KingdomConfirmationMenu confirmationMenu;
    private final KingdomDiplomacyMenu diplomacyMenu;

    public KingdomInventory(GorvaxCore plugin) {
        this.mainMenu = new KingdomMainMenu(plugin);
        this.adminMenu = new KingdomAdminMenu(plugin);
        this.membersMenu = new KingdomMembersMenu(plugin);
        this.lotsMenu = new KingdomLotsMenu(plugin);
        this.permissionsMenu = new KingdomPermissionsMenu(plugin);
        this.confirmationMenu = new KingdomConfirmationMenu(plugin);
        this.diplomacyMenu = new KingdomDiplomacyMenu(plugin);
    }

    // === Menu Principal ===

    public void openMainMenu(Player player) {
        mainMenu.openMainMenu(player);
    }

    public void openKingdomMenu(Player player, Claim claim) {
        mainMenu.openKingdomMenu(player, claim);
    }

    public void openFoundationMenu(Player player, Claim claim) {
        mainMenu.openFoundationMenu(player, claim);
    }

    // === Admin ===

    public void openAdminMenu(Player player, String kingdomId) {
        adminMenu.openAdminMenu(player, kingdomId);
    }

    // === Membros ===

    public void openMembersMenu(Player player, String kingdomId) {
        membersMenu.openMembersMenu(player, kingdomId);
    }

    public void openMembersMenu(Player player, String kingdomId, int page) {
        membersMenu.openMembersMenu(player, kingdomId, page);
    }

    public void openMemberActionMenu(Player player, String kingdomId, String targetUuidStr) {
        membersMenu.openMemberActionMenu(player, kingdomId, targetUuidStr);
    }

    // === Lotes ===

    public void openLotsMenu(Player player, String kingdomId) {
        lotsMenu.openLotsMenu(player, kingdomId);
    }

    public void openLotsMenu(Player player, String kingdomId, int page) {
        lotsMenu.openLotsMenu(player, kingdomId, page);
    }

    public void openPlotManager(Player player, String kingdomId, String plotId) {
        lotsMenu.openPlotManager(player, kingdomId, plotId);
    }

    public void openMyPlotsMenu(Player player, String kingdomId) {
        lotsMenu.openMyPlotsMenu(player, kingdomId);
    }

    // === Permissões ===

    public void openTrustMenu(Player player, String claimId, String subPlotId) {
        permissionsMenu.openTrustMenu(player, claimId, subPlotId);
    }

    public void openTrustMenu(Player player, String claimId, String subPlotId, int page) {
        permissionsMenu.openTrustMenu(player, claimId, subPlotId, page);
    }

    public void openPlayerPermissionsMenu(Player player, String kingdomId, String targetUuidStr, String subPlotId) {
        permissionsMenu.openPlayerPermissionsMenu(player, kingdomId, targetUuidStr, subPlotId);
    }

    public void openGeneralPermissionsMenu(Player player, String kingdomId) {
        permissionsMenu.openGeneralPermissionsMenu(player, kingdomId);
    }

    public void openGeneralPermissionsMenu(Player player, String kingdomId, int page) {
        permissionsMenu.openGeneralPermissionsMenu(player, kingdomId, page);
    }

    // === Confirmações ===

    public void openDeleteConfirmationMenu(Player player, String kingdomId, String plotId) {
        confirmationMenu.openDeleteConfirmationMenu(player, kingdomId, plotId);
    }

    public void openDeleteTerrainConfirmationMenu(Player player, String claimId) {
        confirmationMenu.openDeleteTerrainConfirmationMenu(player, claimId);
    }

    public void openAbandonConfirmationMenu(Player player, String kingdomId, String plotId) {
        confirmationMenu.openAbandonConfirmationMenu(player, kingdomId, plotId);
    }

    public void openDeleteCityConfirmationMenu(Player player, String kingdomId) {
        confirmationMenu.openDeleteCityConfirmationMenu(player, kingdomId);
    }

    // === B7 — Diplomacia ===

    public void openDiplomacyMenu(Player player, String kingdomId) {
        diplomacyMenu.openDiplomacyMenu(player, kingdomId);
    }

    public void openDiplomacyMenu(Player player, String kingdomId, int page) {
        diplomacyMenu.openDiplomacyMenu(player, kingdomId, page);
    }
}
