package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.Relation;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * B7 — Menu de Diplomacia do Reino.
 * Exibe todos os reinos do servidor com ícones coloridos baseados na relação.
 * Suporta paginação e layout adaptado para Bedrock.
 * B35 — Melhorado com instruções de uso e estado vazio.
 */
public class KingdomDiplomacyMenu extends BaseMenu {

    public KingdomDiplomacyMenu(GorvaxCore plugin) {
        super(plugin);
    }

    public void openDiplomacyMenu(Player player, String kingdomId) {
        openDiplomacyMenu(player, kingdomId, 0);
    }

    public void openDiplomacyMenu(Player player, String kingdomId, int page) {
        int size = getMenuSize(player, 54, 27);
        boolean isBedrock = isBedrock(player);
        String acao = isBedrock ? "Toque" : "Clique";

        // Obter todos os reinos exceto o próprio
        List<String> allKingdomIds = plugin.getKingdomManager().getAllKingdomIds();
        allKingdomIds.remove(kingdomId);

        int totalKingdoms = allKingdomIds.size();

        String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
        Inventory gui = Bukkit.createInventory(
                new KingdomHolder(kingdomId, "DIPLOMACY", page), size,
                LegacyComponentSerializer.legacySection()
                        .deserialize("§8Diplomacia: §b" + (nomeReino != null ? nomeReino : "?")));

        fillBorders(gui, kingdomId);

        // [4] Instruções de Uso — sempre visível
        gui.setItem(4, createItem(Material.BOOK, "§e§lComo Usar a Diplomacia", null, null,
                "§7Gerencie as relações do seu",
                "§7reino com outros reinos.",
                "",
                "§a▸ " + acao + " Esquerdo §7→ Propor Aliança",
                "§c▸ " + acao + " Direito §7→ Declarar Inimigo",
                "§7▸ Shift+" + acao + " §7→ Voltar ao Neutro",
                "",
                "§8Alianças devem ser aceitas pelo outro rei."));

        if (totalKingdoms == 0) {
            // Estado vazio: nenhum outro reino encontrado
            gui.setItem(size == 54 ? 22 : 13, createItem(Material.BARRIER,
                    "§c§lNenhum Reino Encontrado", null, null,
                    "§7Não existem outros reinos",
                    "§7no servidor no momento.",
                    "",
                    "§7Quando outros jogadores fundarem",
                    "§7reinos, eles aparecerão aqui."));
        } else {
            // Slots de conteúdo (excluindo linha 1 que tem instruções)
            int[] contentSlots;
            if (size == 54) {
                contentSlots = new int[]{
                        10, 11, 12, 13, 14, 15, 16,
                        19, 20, 21, 22, 23, 24, 25,
                        28, 29, 30, 31, 32, 33, 34,
                        37, 38, 39, 40, 41, 42, 43
                };
            } else {
                contentSlots = new int[]{10, 11, 12, 13, 14, 15, 16};
            }
            int perPage = contentSlots.length;
            int totalPages = Math.max(1, (int) Math.ceil((double) totalKingdoms / perPage));
            page = Math.max(0, Math.min(page, totalPages - 1));

            int startIndex = page * perPage;
            int endIndex = Math.min(startIndex + perPage, totalKingdoms);

            for (int i = startIndex; i < endIndex; i++) {
                String otherId = allKingdomIds.get(i);
                Relation relation = plugin.getKingdomManager().getRelation(kingdomId, otherId);

                String otherName = plugin.getKingdomManager().getNome(otherId);
                if (otherName == null) otherName = otherId;

                int level = plugin.getKingdomManager().getKingdomLevel(otherId);
                int members = plugin.getKingdomManager().getSuditosCount(otherId);

                Material mat;
                String relationTag;
                String relationColor;
                String actionHint;

                switch (relation) {
                    case ALLY -> {
                        mat = Material.LIME_WOOL;
                        relationTag = "§a§lALIADO";
                        relationColor = "§a";
                        actionHint = "§c▸ " + acao + " Direito §7para romper aliança";
                    }
                    case ENEMY -> {
                        mat = Material.RED_WOOL;
                        relationTag = "§c§lINIMIGO";
                        relationColor = "§c";
                        actionHint = "§7▸ Shift+" + acao + " §7para declarar trégua";
                    }
                    case TRUCE -> {
                        mat = Material.YELLOW_WOOL;
                        relationTag = "§e§lTRÉGUA";
                        relationColor = "§e";
                        actionHint = "§a▸ " + acao + " Esquerdo §7para propor aliança";
                    }
                    default -> {
                        mat = Material.WHITE_WOOL;
                        relationTag = "§7NEUTRO";
                        relationColor = "§7";
                        actionHint = "§a▸ " + acao + " Esquerdo §7para propor aliança";
                    }
                }

                String rank = plugin.getKingdomManager().getKingdomRank(otherId);

                int slotIndex = i - startIndex;
                if (slotIndex < contentSlots.length) {
                    gui.setItem(contentSlots[slotIndex], createItem(mat,
                            relationColor + "§l" + otherName,
                            "DIPLOMACY_KINGDOM", otherId,
                            "§7" + rank + " • Nível §f" + level,
                            "§7Membros: §f" + members,
                            "",
                            "§7Relação: " + relationTag,
                            "",
                            actionHint,
                            "§c▸ " + acao + " Direito §7→ Inimigo",
                            "§7▸ Shift+" + acao + " §7→ Neutro"));
                }
            }

            // Navegação (paginação)
            if (page > 0) {
                gui.setItem(size == 54 ? 45 : 18, createItem(Material.ARROW,
                        "§a← Página Anterior", "PAGE_PREV", String.valueOf(page - 1)));
            }
            if (page < totalPages - 1) {
                gui.setItem(size == 54 ? 53 : 26, createItem(Material.ARROW,
                        "§a→ Próxima Página", "PAGE_NEXT", String.valueOf(page + 1)));
            }
        }

        // Botão de voltar ao menu principal
        gui.setItem(size == 54 ? 49 : 22, createItem(Material.ARROW,
                "§c§lVoltar", "OPEN_KINGDOM_MENU", kingdomId,
                "§7Voltar ao menu do reino"));

        player.openInventory(gui);
    }
}
