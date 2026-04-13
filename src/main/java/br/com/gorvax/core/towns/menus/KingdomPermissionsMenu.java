package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.SubPlot;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Menus de permissões: trust, permissões individuais e leis gerais do reino.
 */
public class KingdomPermissionsMenu extends BaseMenu {

    public KingdomPermissionsMenu(GorvaxCore plugin) {
        super(plugin);
    }

    public void openTrustMenu(Player player, String claimId, String subPlotId) {
        openTrustMenu(player, claimId, subPlotId, 0);
    }

    public void openTrustMenu(Player player, String claimId, String subPlotId, int page) {
        Claim claim = plugin.getClaimManager().getClaimById(claimId);
        if (claim == null)
            return;

        String title = "§8Permissões: ";
        Map<UUID, Set<Claim.TrustType>> trusts;

        if (subPlotId != null && !subPlotId.equals("MAIN")) {
            SubPlot plot = null;
            for (SubPlot p : claim.getSubPlots()) {
                if (p.getId().equals(subPlotId)) {
                    plot = p;
                    break;
                }
            }
            if (plot == null)
                return;
            title += plot.getName();
            trusts = plot.getTrustedPlayers();
        } else {
            title += "Território";
            trusts = claim.getTrustedPlayers();
        }

        Inventory gui = Bukkit.createInventory(
                new KingdomHolder(claimId, "TRUST_MENU_" + (subPlotId != null ? subPlotId : "MAIN"), page), 54,
                LegacyComponentSerializer.legacySection().deserialize(title));
        fillBorders(gui, claimId);

        List<UUID> trustedList = new ArrayList<>(trusts.keySet());
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int itemsPerPage = slots.length;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, trustedList.size());

        for (int i = start; i < end; i++) {
            UUID uuid = trustedList.get(i);
            Set<Claim.TrustType> types = trusts.get(uuid);
            String name = getPlayerName(uuid);

            List<String> lore = new ArrayList<>();
            lore.add("§7Permissões:");
            if (types.contains(Claim.TrustType.VICE))
                lore.add("§6- Duque (Vice)");
            if (types.contains(Claim.TrustType.GERAL))
                lore.add("§b- Acesso Total (Geral)");
            if (types.contains(Claim.TrustType.CONSTRUCAO))
                lore.add("§e- Construção");
            if (types.contains(Claim.TrustType.CONTEINER))
                lore.add("§e- Baús/Conteiners");
            if (types.contains(Claim.TrustType.ACESSO))
                lore.add("§e- Interação");
            lore.add("");
            lore.add("§eClique para Gerenciar");

            gui.setItem(slots[i - start], createSkull(uuid, "§e" + name, "OPEN_PLAYER_PERMS",
                    (subPlotId != null ? subPlotId : "MAIN") + ":" + uuid.toString(), lore.toArray(new String[0])));
        }

        if (page > 0)
            gui.setItem(45, createItem(Material.ARROW, "§aPágina Anterior", "PREVIOUS_PAGE", "TRUST",
                    "§7Ir para página " + page));
        if (end < trustedList.size())
            gui.setItem(53, createItem(Material.ARROW, "§aPróxima Página", "NEXT_PAGE", "TRUST",
                    "§7Ir para página " + (page + 2)));

        gui.setItem(48, createItem(Material.EMERALD, "§a§lNomear Aliado", "ADD_TRUST_INPUT",
                subPlotId != null ? subPlotId : "MAIN", "§7Digite o nome do jogador", "§7para adicionar.", "",
                "§e[Clique para Adicionar]"));

        String backAction = "BACK";
        String backData = null;
        if (subPlotId != null && !subPlotId.equals("MAIN")) {
            backAction = "OPEN_PLOT_MENU";
            backData = subPlotId;
        } else {
            // B35 — Voltar para o menu correto: reino vs terra normal
            boolean isKingdomClaim = claim.isKingdom() && claim.getKingdomName() != null;
            backAction = isKingdomClaim ? "OPEN_KINGDOM_MENU" : "OPEN_FOUNDATION";
        }
        gui.setItem(49, createItem(Material.ARROW, "§cVoltar", backAction, backData, "§7Voltar ao menu anterior"));

        player.openInventory(gui);
    }

    public void openPlayerPermissionsMenu(Player player, String kingdomId, String targetUuidStr, String subPlotId) {
        UUID targetUUID = UUID.fromString(targetUuidStr);
        String targetName = getPlayerName(targetUUID);

        Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
        Set<Claim.TrustType> perms = new HashSet<>();

        if (subPlotId != null && !subPlotId.equals("MAIN")) {
            for (SubPlot p : claim.getSubPlots()) {
                if (p.getId().equals(subPlotId)) {
                    if (p.getTrustedPlayers().containsKey(targetUUID)) {
                        perms = p.getTrustedPlayers().get(targetUUID);
                    }
                    break;
                }
            }
        } else {
            if (claim.getTrustedPlayers().containsKey(targetUUID)) {
                perms = claim.getTrustedPlayers().get(targetUUID);
            }
        }

        Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "PLAYER_PERMS"), 27,
                LegacyComponentSerializer.legacySection().deserialize("§8Permissões: " + targetName));
        fillBorders(gui, kingdomId);

        String contextData = (subPlotId != null ? subPlotId : "MAIN") + ":" + targetUuidStr;

        boolean hasGeral = perms.contains(Claim.TrustType.GERAL);
        gui.setItem(10,
                createItem(Material.NETHER_STAR, "§b§lAcesso Total", "TOGGLE_PLAYER_PERM", "GERAL:" + contextData,
                        "§7Permite TUDO (exceto admin).",
                        "§7Inclui construção, baús e interação.",
                        "",
                        "§7Status: " + (hasGeral ? "§aPERMITIDO" : "§cNEGADO"), "",
                        "§e[Clique para Alternar]"));

        boolean hasBuild = perms.contains(Claim.TrustType.CONSTRUCAO) || hasGeral;
        gui.setItem(12, createItem(Material.BRICK, "§e§lConstrução", "TOGGLE_PLAYER_PERM", "CONSTRUCAO:" + contextData,
                "§7Permite colocar e quebrar blocos.",
                "§7Inclui plantar e colher.",
                "",
                "§7Status: " + (hasBuild ? "§aPERMITIDO" : "§cNEGADO"), "",
                "§e[Clique para Alternar]"));

        boolean hasContainer = perms.contains(Claim.TrustType.CONTEINER) || hasGeral;
        gui.setItem(14, createItem(Material.CHEST, "§6§lConteiners", "TOGGLE_PLAYER_PERM", "CONTEINER:" + contextData,
                "§7Permite abrir baús, fornalhas,",
                "§7funis, barris e dispensadores.",
                "",
                "§7Status: " + (hasContainer ? "§aPERMITIDO" : "§cNEGADO"), "",
                "§e[Clique para Alternar]"));

        boolean hasAccess = perms.contains(Claim.TrustType.ACESSO) || hasGeral;
        gui.setItem(16, createItem(Material.LEVER, "§a§lInteração", "TOGGLE_PLAYER_PERM", "ACESSO:" + contextData,
                "§7Permite usar botões, alavancas,",
                "§7portas, portões e alçapões.",
                "",
                "§7Status: " + (hasAccess ? "§aPERMITIDO" : "§cNEGADO"),
                "", "§e[Clique para Alternar]"));

        gui.setItem(22, createItem(Material.ARROW, "§cVoltar", "OPEN_TRUST_MENU",
                subPlotId != null ? subPlotId : "MAIN", "§7Voltar para lista"));

        player.openInventory(gui);
    }

    public void openGeneralPermissionsMenu(Player player, String kingdomId) {
        openGeneralPermissionsMenu(player, kingdomId, 0);
    }

    public void openGeneralPermissionsMenu(Player player, String kingdomId, int page) {
        Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
        boolean isKingdom = claim.isKingdom();

        // Textos contextuais: reino vs terra simples
        String menuTitle = isKingdom ? "§8Leis do Reino (Súditos)" : "§8Regras da Terra (Aliados)";
        String roleLabel = isKingdom ? "Súditos" : "Aliados";

        Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "GENERAL_PERMS", page), 45,
                LegacyComponentSerializer.legacySection().deserialize(menuTitle));
        fillBorders(gui, kingdomId);

        boolean build = claim.isResidentsBuild();
        gui.setItem(11, createItem(Material.BRICK, "§e§lConstrução", "TOGGLE_RES_BUILD", null,
                "§7" + roleLabel + " podem construir",
                "§7e quebrar blocos em terras comuns?",
                "",
                "§7Inclui: blocos, plantações, decoração",
                "",
                "§7Status: " + (build ? "§aPERMITIDO" : "§cNEGADO"), "",
                "§e[Clique para Alternar]"));

        boolean container = claim.isResidentsContainer();
        gui.setItem(13, createItem(Material.CHEST, "§6§lConteiners", "TOGGLE_RES_CONTAINER", null,
                "§7" + roleLabel + " podem abrir baús,",
                "§7fornalhas e dispensadores públicos?",
                "",
                "§7Status: " + (container ? "§aPERMITIDO" : "§cNEGADO"), "",
                "§e[Clique para Alternar]"));

        boolean sw = claim.isResidentsSwitch();
        gui.setItem(15, createItem(Material.LEVER, "§a§lInteração", "TOGGLE_RES_SWITCH", null,
                "§7" + roleLabel + " podem usar botões,",
                "§7portas e alavancas públicas?",
                "",
                "§7Status: " + (sw ? "§aPERMITIDO" : "§cNEGADO"), "",
                "§e[Clique para Alternar]"));

        gui.setItem(21, createItem(Material.LIME_DYE, "§a§lATIVAR TUDO", "TOGGLE_ALL_PERMS_ON", null,
                "§7Permite tudo para " + roleLabel.toLowerCase() + ".", "", "§e[Clique para Ativar]"));

        gui.setItem(23, createItem(Material.RED_DYE, "§c§lDESATIVAR TUDO", "TOGGLE_ALL_PERMS_OFF", null,
                "§7Nega tudo para " + roleLabel.toLowerCase() + ".", "", "§c[Clique para Desativar]"));

        String backAction = isKingdom ? "ADMIN_PANEL" : "OPEN_FOUNDATION";
        String backDesc = isKingdom ? "§7Voltar a corte" : "§7Voltar ao menu da terra";
        gui.setItem(22, createItem(Material.ARROW, "§cVoltar", backAction, null, backDesc));

        String individualAction = isKingdom ? "MANAGE_MEMBERS" : "OPEN_TRUST_MENU";
        String individualTitle = isKingdom ? "§b§lGerenciar Cidadãos" : "§a§lGerenciar Aliados";
        String[] individualLore;
        if (isKingdom) {
            individualLore = new String[]{"§7Gerencie cargos e expulsões.", "", "§e[Clique para Acessar]"};
        } else {
            individualLore = new String[]{
                "§7Adicione amigos para interagir",
                "§7na sua terra.",
                "",
                "§7Atalho: §e/trust <nome>",
                "",
                "§e[Clique para Acessar]"
            };
        }

        // Para terras comuns, slot 29 (mais visível); para reinos, slot 31
        int individualSlot = isKingdom ? 31 : 29;
        gui.setItem(individualSlot, createItem(Material.PLAYER_HEAD, individualTitle, individualAction, "MAIN",
                individualLore));

        player.openInventory(gui);
    }
}
