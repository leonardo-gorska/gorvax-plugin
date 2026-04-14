package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu administrativo (Corte Real) com configurações do reino.
 */
public class KingdomAdminMenu extends BaseMenu {

        public KingdomAdminMenu(GorvaxCore plugin) {
                super(plugin);
        }

        public void openAdminMenu(Player player, String kingdomId) {
                String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
                // B4.2 — Menu adaptativo: 45 slots (Bedrock) vs 54 slots (Java)
                int size = getMenuSize(player, 54, 45);
                Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "ADMIN"), size,
                                LegacyComponentSerializer.legacySection()
                                                .deserialize("§4§lCorte Real: §8" + nomeReino));

                fillBorders(gui, kingdomId);

                Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
                boolean isRei = plugin.getKingdomManager().isRei(kingdomId, player.getUniqueId());

                if (size <= 45) {
                        // ========== LAYOUT COMPACTO (Bedrock 5 rows) ==========
                        // Linha 2: Identidade e Spawn (10-16)

                        // [10] Definir Spawn
                        gui.setItem(10, createItem(Material.RECOVERY_COMPASS, "§e§lMarco Zero", "SET_SPAWN", null,
                                        "§7Define o ponto de teleporte", "§7do reino.", "", "§eToque para Definir"));

                        // [11] Tag + Cores
                        gui.setItem(11, createItem(Material.NAME_TAG, "§b§lBrasao (TAG)", "SET_TAG", null,
                                        "§7Altera a sigla [" + claim.getTag() + "]", "", "§eToque para Alterar"));

                        // [12] Mensagens Entrada/Saída
                        gui.setItem(12, createItem(Material.OAK_SIGN, "§a§lAnuncios", "MANAGE_ENTER_MSG", null,
                                        "§7Editar mensagens de", "§7entrada e saida.", "", "§eToque para Editar"));

                        // [13] PvP
                        gui.setItem(13, createItem(Material.IRON_SWORD, "§c§lGuerra (PvP)", "TOGGLE_PVP_GLOBAL", null,
                                        "§fStatus: " + (claim.isPvp() ? "§cATIVADO" : "§aDESATIVADO"),
                                        "", "§eToque para Alternar"));

                        // [14] Permissões
                        gui.setItem(14, createItem(Material.WRITABLE_BOOK, "§d§lPermissoes", "OPEN_GENERAL_PERMS", null,
                                        "§7Configure as leis do reino.", "", "§eToque para Configurar"));

                        // [15] Economia
                        gui.setItem(15, createItem(Material.GOLD_INGOT, "§6§lImpostos", "ADJUST_ECONOMY", null,
                                        "§fTaxa: §a" + claim.getTax() + "%", "", "§eToque para Ajustar"));

                        // [16] Blocos
                        gui.setItem(16, createItem(Material.GOLD_ORE, "§6§lBlocos §b(50%OFF)", "BUY_BLOCKS_DISCOUNT",
                                        null,
                                        "§f100 blocos por §a$2.500", "", "§eToque para Comprar"));

                        // Linha 3: Visual e Cores (19-25)
                        gui.setItem(19, createItem(Material.MAGENTA_DYE, "§5§lCor do Brasao", "SET_TAG_COLOR", null,
                                        "§7Altera a cor da sigla [TAG]",
                                        "§7no chat e tab.",
                                        "",
                                        "§fAtual: " + claim.getTagColor() + "[" + claim.getTag() + "]",
                                        "",
                                        "§eToque para Alternar"));

                        gui.setItem(20, createItem(Material.PEONY, "§d§lCor do Estandarte", "TOGGLE_WELCOME_COLOR",
                                        null,
                                        "§7Altera a cor da mensagem",
                                        "§7de entrada/saida.",
                                        "",
                                        "§fAtual: " + claim.getWelcomeColor() + "Exemplo",
                                        "",
                                        "§eToque para Alternar"));

                        gui.setItem(21, createItem(Material.BLUE_DYE, "§9§lCor do Chat Real", "TOGGLE_CHAT_COLOR",
                                        null,
                                        "§7Altera a cor do nome do reino",
                                        "§7no chat global.",
                                        "",
                                        "§fAtual: " + claim.getChatColor() + "Exemplo",
                                        "",
                                        "§eToque para Alternar"));

                        // Guia do Rei
                        gui.setItem(22, createItem(Material.WRITTEN_BOOK, "§e§lGuia do Rei", "KING_GUIDE", null,
                                        "§7Veja todos os comandos",
                                        "§7e dicas para governar.",
                                        "",
                                        "§eToque para Ver"));

                        // Rodapé
                        if (isRei || player.isOp() || player.hasPermission("gorvax.admin")) {
                                gui.setItem(36, createItem(Material.LAVA_BUCKET, "§4§lDISSOLVER", "DELETE_CITY_CHECK",
                                                null,
                                                "§7Apaga o reino.", "§cIrreversivel.", "", "§4[TOQUE P/ DELETAR]"));
                        }
                        gui.setItem(44, createItem(Material.ARROW, "§cVoltar", "BACK", null,
                                        "§7Voltar ao menu principal"));

                } else {
                        // ========== LAYOUT COMPLETO (Java 6 rows) ==========

                        // --- LINHA 1: IDENTIDADE & SPAWN ---
                        gui.setItem(10, createItem(Material.RECOVERY_COMPASS, "§e§lDefinir Marco Zero", "SET_SPAWN",
                                        null,
                                        "§7Define o ponto de teleporte",
                                        "§7do reino para sua posição atual.",
                                        "",
                                        "§e[Clique para Definir]"));

                        gui.setItem(11, createItem(Material.NAME_TAG, "§b§lDefinir Brasão (TAG)", "SET_TAG", null,
                                        "§7Altera a sigla do reino",
                                        "§7exibida no chat.",
                                        "",
                                        "§fAtual: §e[" + claim.getTag() + "]",
                                        "",
                                        "§e[Clique para Alterar]"));

                        // --- LINHA 2: VISUAL & CHAT ---
                        gui.setItem(19, createItem(Material.OAK_SIGN, "§a§lAnúncio de Chegada", "MANAGE_ENTER_MSG",
                                        null,
                                        "§7Título exibido ao entrar.",
                                        "",
                                        "§fAtual: §7" + (claim.getEnterTitle() == null ? "Padrão"
                                                        : claim.getEnterTitle()),
                                        "",
                                        "§e[Clique para Editar]"));

                        gui.setItem(20, createItem(Material.WARPED_SIGN, "§c§lAnúncio de Partida", "MANAGE_EXIT_MSG",
                                        null,
                                        "§7Título exibido ao sair.",
                                        "",
                                        "§fAtual: §7" + (claim.getExitTitle() == null ? "Padrão"
                                                        : claim.getExitTitle()),
                                        "",
                                        "§e[Clique para Editar]"));

                        gui.setItem(21, createItem(Material.PEONY, "§d§lCor do Estandarte", "TOGGLE_WELCOME_COLOR",
                                        null,
                                        "§7Altera a cor da mensagem",
                                        "§7de entrada/saída.",
                                        "",
                                        "§fAtual: " + claim.getWelcomeColor() + "Exemplo",
                                        "",
                                        "§e[Clique para Alternar]"));

                        gui.setItem(24, createItem(Material.BLUE_DYE, "§9§lCor do Chat Real (/rc)", "TOGGLE_CHAT_COLOR",
                                        null,
                                        "§7Altera a cor do nome do reino",
                                        "§7no chat global.",
                                        "",
                                        "§fAtual: " + claim.getChatColor() + "Exemplo",
                                        "",
                                        "§e[Clique para Alternar]"));

                        gui.setItem(25, createItem(Material.MAGENTA_DYE, "§5§lCor do Brasão", "SET_TAG_COLOR", null,
                                        "§7Altera a cor da sigla [TAG]",
                                        "§7no chat e tab.",
                                        "",
                                        "§fAtual: " + claim.getTagColor() + "[" + claim.getTag() + "]",
                                        "",
                                        "§e[Clique para Alternar]"));

                        // --- LINHA 3: CONFIGURAÇÕES (PVP & REGRAS) ---
                        gui.setItem(28, createItem(Material.IRON_SWORD, "§c§lGuerra Aberta (PvP)", "TOGGLE_PVP_GLOBAL",
                                        null,
                                        "§7Combate contra invasores.",
                                        "",
                                        "§fStatus: " + (claim.isPvp() ? "§cATIVADO" : "§aDESATIVADO"),
                                        "",
                                        "§e[Clique para Alternar]"));

                        gui.setItem(29, createItem(Material.GOLDEN_SWORD, "§6§lDuelos Internos", "TOGGLE_PVP_RESIDENTS",
                                        null,
                                        "§7Combate entre súditos no reino.",
                                        "",
                                        "§fStatus: " + (claim.isResidentsPvp() ? "§cATIVADO" : "§aDESATIVADO"),
                                        "",
                                        "§e[Clique para Alternar]"));

                        gui.setItem(30,
                                        createItem(Material.STONE_SWORD, "§7§lDuelos Externos",
                                                        "TOGGLE_PVP_RESIDENTS_OUTSIDE", null,
                                                        "§7Combate entre súditos",
                                                        "§7NO MUNDO (Fora do reino).",
                                                        "",
                                                        "§fStatus: " + (claim.isResidentsPvpOutside() ? "§cATIVADO"
                                                                        : "§aDESATIVADO"),
                                                        "",
                                                        "§e[Clique para Alternar]"));

                        // --- LINHA 4: GESTÃO ---
                        gui.setItem(40, createItem(Material.WRITABLE_BOOK, "§d§lDefinir Permissões",
                                        "OPEN_GENERAL_PERMS", null,
                                        "§7Configure as leis do reino",
                                        "§7e gerencie seus aliados.",
                                        "",
                                        "§e[Clique para Configurar]"));

                        gui.setItem(31, createItem(Material.GOLD_INGOT, "§6§lImpostos & Tributos", "ADJUST_ECONOMY",
                                        null,
                                        "§7Defina a taxa de imposto",
                                        "§7sobre vendas no reino.",
                                        "",
                                        "§fTaxa Atual: §a" + claim.getTax() + "%",
                                        "",
                                        "§e[Clique para Ajustar]"));

                        gui.setItem(42, createItem(Material.GOLD_ORE, "§6§lEstoque Real (Blocos)",
                                        "BUY_BLOCKS_DISCOUNT", null,
                                        "§7Oferta exclusiva para Reis:",
                                        "§b50% de Desconto!",
                                        "",
                                        "§f100 blocos por §a$2.500",
                                        "",
                                        "§e[Clique para Comprar]"));

                        // --- GUIA DO REI ---
                        gui.setItem(38, createItem(Material.WRITTEN_BOOK, "§e§l❓ Guia do Rei", "KING_GUIDE", null,
                                        "§7Manual completo de governança!",
                                        "",
                                        "§e📋 Comandos: §fRenomear, spawn, PvP",
                                        "§e🏘 Lotes: §fCriar e gerenciar feudos",
                                        "§e🤝 Diplomacia: §fAlianças e guerras",
                                        "",
                                        "§e[Clique para Ver no Chat]"));

                        // --- RODAPÉ: PERIGO ---
                        if (isRei || player.isOp() || player.hasPermission("gorvax.admin")) {
                                gui.setItem(49, createItem(Material.LAVA_BUCKET, "§4§lDISSOLVER REINO",
                                                "DELETE_CITY_CHECK", null,
                                                "§7Apaga permanentemente o reino.",
                                                "§cNão há volta.",
                                                "",
                                                "§4[CLIQUE PARA DELETAR]"));
                        }

                        gui.setItem(53, createItem(Material.ARROW, "§cVoltar", "BACK", null,
                                        "§7Voltar ao menu principal"));
                }

                player.openInventory(gui);
        }
}
