package br.com.gorvax.core.towns.menus;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.SubPlot;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu principal, contextual, visão do reino e fundação.
 */
public class KingdomMainMenu extends BaseMenu {

        public KingdomMainMenu(GorvaxCore plugin) {
                super(plugin);
        }

        public void openMainMenu(Player player) {
                Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());

                if (claim == null) {
                        plugin.getMessageManager().send(player, "menu.error_no_territory");
                        return;
                }

                String kingdomId = claim.getId();
                String nomeReino = claim.getKingdomName();

                // Se não há nome ou não é reino oficial, abre o menu de fundação
                if (nomeReino == null || !claim.isKingdom()) {
                        openFoundationMenu(player, claim);
                        return;
                }

                // Verificar se está em um SubPlot (Lote)
                SubPlot subPlot = claim.getSubPlotAt(player.getLocation());

                if (subPlot != null) {
                        // Está em um lote -> Menu Contextual (Hub)
                        openContextualMenu(player, claim, subPlot);
                } else {
                        // Está no reino (ruas/praça) -> Menu do Reino
                        openKingdomMenu(player, claim);
                }
        }

        private void openContextualMenu(Player player, Claim claim, SubPlot subPlot) {
                Inventory gui = Bukkit.createInventory(new KingdomHolder(claim.getId(), "CONTEXTUAL"), 27,
                                LegacyComponentSerializer.legacySection().deserialize("§8Menu Principal"));
                fillBorders(gui, claim.getId());

                String kingdomName = claim.getKingdomName();
                String plotName = subPlot.getName();

                // [11] Reino
                gui.setItem(11, createItem(Material.BEACON, "§b§lReino: " + kingdomName, "OPEN_KINGDOM_MENU", null,
                                "§7Acesse o painel do reino.",
                                "",
                                "§e[Clique para Abrir]"));

                // [15] Lote
                gui.setItem(15, createItem(Material.OAK_SIGN, "§a§lLote: " + plotName, "OPEN_PLOT_MENU",
                                subPlot.getId(),
                                "§7Gerencie este lote.",
                                "",
                                "§e[Clique para Abrir]"));

                // [13] Permissões Rápidas (Atalho) -> Redireciona para o Menu do Lote
                boolean isPlotOwner = subPlot.getOwner() != null && subPlot.getOwner().equals(player.getUniqueId());
                boolean isPlotRenter = subPlot.getRenter() != null && subPlot.getRenter().equals(player.getUniqueId());
                boolean canManagePlot = isPlotOwner || isPlotRenter
                                || claim.hasPermission(player.getUniqueId(), Claim.TrustType.VICE)
                                || player.isOp() || player.hasPermission("gorvax.admin");

                if (canManagePlot) {
                        gui.setItem(13,
                                        createItem(Material.WRITABLE_BOOK, "§d§lGerenciar Lote", "OPEN_PLOT_MENU",
                                                        subPlot.getId(),
                                                        "§7Defina permissões e configurações",
                                                        "§7deste lote.",
                                                        "",
                                                        "§e[Clique para Configurar]"));
                } else {
                        gui.setItem(13, createItem(Material.BOOK, "§7Informações", null, null,
                                        "§7Você está no lote: §f" + plotName,
                                        "§7Dono: §f" + getPlayerName(subPlot.getOwner())));
                }

                player.openInventory(gui);
        }

        public void openKingdomMenu(Player player, Claim claim) {
                String kingdomId = claim.getId();
                String nomeReino = claim.getKingdomName();

                String rank = plugin.getKingdomManager().getKingdomRank(kingdomId);
                int suditos = plugin.getKingdomManager().getSuditosCount(kingdomId);

                boolean isRei = plugin.getKingdomManager().isRei(kingdomId, player.getUniqueId());
                boolean isDuque = claim.hasPermission(player.getUniqueId(), Claim.TrustType.VICE);
                boolean isSudito = plugin.getKingdomManager().isSudito(kingdomId, player.getUniqueId());

                // B35 — Layout expandido: 54 slots para melhor organização
                boolean isBedrock = isBedrock(player);
                Inventory gui = Bukkit.createInventory(new KingdomHolder(kingdomId, "MAIN"), 54,
                                LegacyComponentSerializer.legacySection().deserialize("§8Reino: §b" + nomeReino));
                fillBorders(gui, kingdomId);

                boolean canUpgrade = isRei || isDuque;
                String acao = isBedrock ? "Toque" : "Clique";

                // ===== LINHA 1 (slots 1-7): INFO DO REINO =====

                // [4] Info do Reino (centralizado)
                gui.setItem(4, createItem(Material.BOOK, "§b§l" + nomeReino.toUpperCase(), null, null,
                                "§7Status: §e" + rank,
                                "§7Rei: §f" + getPlayerName(claim.getOwner()),
                                isBedrock ? "§7Pop: §f" + suditos + " súditos"
                                                : "§7População: §f" + suditos + " súditos",
                                "",
                                "§7ID do Trono: §8#" + kingdomId));

                // ===== LINHA 2 (slots 10-16): AÇÕES DE GESTÃO =====

                // [10] Spawn do Reino
                gui.setItem(10, createItem(Material.COMPASS, "§b§lSpawn do Reino", "SPAWN", null,
                                "§7Teleporte para a sede do reino.",
                                "",
                                "§b🏠 Destino: §fMarco Zero do reino",
                                "§b⏱ Warmup: §f3s §7(cancelado se mover)",
                                "",
                                isSudito || isRei ? "§e[" + acao + " para Ir]"
                                                : "§cPago para Visitantes"));

                // [11] Diplomacia
                int totalAllies = plugin.getKingdomManager().getAllianceCount(kingdomId);
                int totalEnemies = plugin.getKingdomManager().getEnemyCount(kingdomId);
                gui.setItem(11, createItem(Material.SHIELD, "§b§lDiplomacia", "OPEN_DIPLOMACY",
                                kingdomId,
                                isBedrock ? "§7Aliancas, inimizades e guerras"
                                                : "§7Alianças, inimizades e guerras",
                                "§7com outros reinos.",
                                "",
                                "§aAliados: §f" + totalAllies,
                                "§cInimigos: §f" + totalEnemies,
                                "",
                                isBedrock ? "§e💡 Dica: §fToque no reino para agir"
                                                : "§e💡 Dica: §fEsq=Aliança, Dir=Inimigo",
                                "",
                                "§e[" + acao + " para Acessar]"));

                // [12] Lotes & Feudos
                gui.setItem(12, createItem(Material.OAK_DOOR, "§a§lLotes & Feudos", "VIEW_LOTS",
                                null,
                                "§7Crie lotes dentro do reino para",
                                "§7vender ou alugar a outros jogadores.",
                                "",
                                isBedrock ? "§e💡 Dica: §fUse a Pa de Ferro!"
                                                : "§e💡 Dica: §fUse a §ePá de Ferro §fpara demarcar!",
                                "",
                                "§e[" + acao + " para Acessar]"));

                // [13] Banco Real
                double bankBalance = plugin.getKingdomManager().getBankBalance(kingdomId);
                boolean decaying = plugin.getKingdomManager().isDecaying(kingdomId);
                String decayTag = decaying
                                ? (isBedrock ? " §c[DECADENCIA]" : " §c[DECADÊNCIA]")
                                : "";
                gui.setItem(13, createItem(Material.GOLD_INGOT, "§6§lBanco Real" + decayTag,
                                "OPEN_BANK", null,
                                "§7Saldo: §6$" + String.format("%.2f", bankBalance),
                                "",
                                "§7Use §e/reino banco §7para detalhes.",
                                "§7Use §e/reino depositar §7para adicionar.",
                                isRei ? "§7Use §e/reino sacar §7para retirar." : "",
                                "",
                                "§e[" + acao + " para Ver]"));

                // [14] Minhas Terras
                if (isSudito || isRei || isDuque) {
                        gui.setItem(14, createItem(Material.OAK_SIGN, "§a§lMinhas Terras",
                                        "VIEW_MY_PLOTS", null,
                                        isBedrock ? "§7Gerencie seus imoveis"
                                                        : "§7Gerencie seus imóveis",
                                        "§7neste reino.",
                                        "",
                                        "§e[" + acao + " para Acessar]"));
                }

                // [15] Convidar Cidadão (apenas Rei/Duque)
                if (isRei || isDuque) {
                        gui.setItem(15, createItem(Material.PLAYER_HEAD,
                                        isBedrock ? "§a§lConvidar Cidadao" : "§a§lConvidar Cidadão",
                                        "INVITE_MEMBER", null,
                                        "§7Convide jogadores para se tornarem",
                                        isBedrock ? "§7suditos do seu reino."
                                                        : "§7súditos do seu reino.",
                                        "",
                                        "§fComando: §e/reino convidar <nome>",
                                        "",
                                        "§8Membros não precisam comprar um",
                                        "§8lote para fazer parte do reino.",
                                        "",
                                        "§e[" + acao + " para Convidar]"));
                }

                // [16] Corte Real (Admin)
                if (isRei || isDuque || player.isOp() || player.hasPermission("gorvax.admin")) {
                        gui.setItem(16, createItem(Material.NETHER_STAR, "§c§lCorte Real",
                                        "ADMIN_PANEL", null,
                                        "§7PvP, impostos, spawn e visuais.",
                                        "",
                                        isBedrock ? "§e💡 Dica: §fAcesso exclusivo da nobreza!"
                                                        : "§e💡 Dica: §fAcesso exclusivo para Rei/Duque!",
                                        "",
                                        "§e[" + acao + " para Governar]"));
                } else {
                        gui.setItem(16, createItem(Material.PAPER, "§7Decreto Real", null, null,
                                        "§7Taxa: " + claim.getTax() + "%",
                                        "§7PVP: " + (claim.isPvp() ? "§cON" : "§aOFF")));
                }

                // B40 — [25] Visitar Reinos (acesso universal)
                gui.setItem(25, createItem(Material.FILLED_MAP,
                                isBedrock ? "§6§lVisitar Reinos" : "§6§l🌍 Visitar Reinos",
                                "VISIT_KINGDOMS", null,
                                "§7Visite reinos públicos de outros jogadores!",
                                "",
                                isBedrock ? "§e📋 Comando: §f/reino visitar <nome>"
                                                : "§e📋 Comando: §f/reino visitar <nome>",
                                "§e💰 Custo: §f$500 §7| §e⏱ Cooldown: §f60s",
                                "",
                                "§e[" + acao + " para Ver Lista]"));

                // ===== LINHA 3 (slots 19-25): UPGRADES =====

                int nivelPres = plugin.getKingdomManager().getNivelPreservacao(kingdomId);
                String precoPres = canUpgrade ? "§6$" + ((nivelPres + 1) * 15000) : "§cSomente Nobres";
                gui.setItem(19, createItem(Material.TOTEM_OF_UNDYING,
                                isBedrock ? "§d§lPreservacao" : "§d§lPreservação Divina",
                                "UPGRADE_PRESERVACAO", null,
                                "§7Evita perda de itens ao morrer",
                                "§7nas terras do reino.",
                                "",
                                isBedrock ? "§fNiv: §e" + nivelPres + " §7(§a" + (nivelPres * 10) + "%§7)"
                                                : "§fNível: §e" + nivelPres + " §7| §fChance: §a"
                                                                + (nivelPres * 10) + "%",
                                "§7Oferenda: " + precoPres,
                                canUpgrade ? "§e[" + acao + " para Evoluir]" : "§7Efeito ativo"));

                int nivelXp = plugin.getKingdomManager().getNivel(kingdomId, "xp");
                String precoXp = canUpgrade ? "§6$" + ((nivelXp + 1) * 5000) : "§cSomente Nobres";
                gui.setItem(20, createItem(Material.EXPERIENCE_BOTTLE,
                                isBedrock ? "§e§lSabedoria" : "§e§lSabedoria Ancestral",
                                "UPGRADE_XP", null,
                                isBedrock ? "§7Aumenta o XP ganho dentro do reino."
                                                : "§7Aumenta o XP ganho nas terras do reino.",
                                "",
                                isBedrock ? "§fNiv: §b" + nivelXp + " §7(§a+" + (20 + (nivelXp * 5)) + "%§7)"
                                                : "§fNível: §b" + nivelXp + " §7| §fBônus: §a+"
                                                                + (20 + (nivelXp * 5)) + "%",
                                "§7Oferenda: " + precoXp,
                                canUpgrade ? "§e[" + acao + " para Evoluir]" : "§7Efeito ativo"));

                int nivelSorte = plugin.getKingdomManager().getNivelSorte(kingdomId);
                String precoSorte = canUpgrade ? "§6$" + ((nivelSorte + 1) * 20000) : "§cSomente Nobres";
                gui.setItem(21, createItem(Material.GOLDEN_PICKAXE,
                                isBedrock ? "§e§lFortuna" : "§e§lFortuna da Terra",
                                "UPGRADE_SORTE", null,
                                isBedrock ? "§7Chance de drops duplos ao minerar."
                                                : "§7Chance de drops duplos ao minerar nas terras.",
                                "",
                                isBedrock ? "§fNiv: §e" + nivelSorte + " §7(§a" + (nivelSorte * 5) + "%§7)"
                                                : "§fNível: §e" + nivelSorte + " §7| §fChance: §a"
                                                                + (nivelSorte * 5) + "%",
                                "§7Oferenda: " + precoSorte,
                                canUpgrade ? "§e[" + acao + " para Evoluir]" : "§7Efeito ativo"));

                int nivelSpeed = plugin.getKingdomManager().getNivel(kingdomId, "speed");
                String precoSpeed = canUpgrade ? "§6$" + ((nivelSpeed + 1) * 30000) : "§cSomente Nobres";
                gui.setItem(22, createItem(Material.SUGAR,
                                isBedrock ? "§b§lVentos" : "§b§lVentos Favoráveis",
                                "UPGRADE_SPEED", null,
                                isBedrock ? "§7Velocidade de movimento no reino."
                                                : "§7Aumenta a velocidade de movimento no reino.",
                                "",
                                isBedrock ? "§fNiv: §e" + nivelSpeed
                                                : "§fNível: §e" + nivelSpeed,
                                "§fEfeito: §aVelocidade "
                                                + (nivelSpeed == 1 ? "I" : (nivelSpeed == 2 ? "II" : "Nulo")),
                                "§7Oferenda: " + precoSpeed,
                                canUpgrade ? "§e[" + acao + " para Evoluir]" : "§7Efeito ativo"));

                int nivelExt = plugin.getKingdomManager().getNivel(kingdomId, "extension");
                String precoExt = canUpgrade ? "§6$" + ((nivelExt + 1) * 50000) : "§cSomente Nobres";
                gui.setItem(23, createItem(Material.SPYGLASS,
                                isBedrock ? "§b§lExpansao" : "§b§lExpansão de Fronteiras",
                                "UPGRADE_EXTENSION", null,
                                isBedrock ? "§7Expande a area de efeito"
                                                : "§7Expande a área de efeito dos buffs",
                                isBedrock ? "§7dos buffs do reino."
                                                : "§7para além das muralhas.",
                                "",
                                isBedrock ? "§fNiv: §e" + nivelExt
                                                : "§fNível: §e" + nivelExt,
                                isBedrock ? "§fAlcance Extra: §a+" + (nivelExt * 20) + " blocos"
                                                : "§fAlcance Extra: §a+" + (nivelExt * 20) + " blocos",
                                "§7Oferenda: " + precoExt,
                                canUpgrade ? "§e[" + acao + " para Evoluir]" : "§7Efeito ativo"));

                // [24] Blocos de Proteção
                gui.setItem(24, createItem(Material.GOLD_BLOCK,
                                isBedrock ? "§6§lExpandir Dominio" : "§6§lExpandir Domínio",
                                "BUY_BLOCKS", null,
                                isBedrock ? "§7Compre blocos de protecao."
                                                : "§7Compre blocos de proteção.",
                                "",
                                isBedrock ? "§fPreco: §6$5.000 §7(100 blocos)"
                                                : "§fPreço: §6$5.000 §7(100 blocos)",
                                isRei || isDuque ? "§e[" + acao + " para Comprar]"
                                                : "§7Apenas a nobreza pode comprar"));

                // ===== LINHA 4 (slot 31): CARGO DO JOGADOR =====

                if (isRei) {
                        gui.setItem(31, createItem(Material.DIAMOND_HELMET,
                                        isBedrock ? "§b§lCargo: Rei" : "§b§lSeu Cargo: Rei",
                                        null, null,
                                        isBedrock ? "§7Soberania total sobre"
                                                        : "§7Você tem soberania total sobre",
                                        "§7este reino.",
                                        "",
                                        "§aAcesso Real Liberado"));
                } else if (isDuque) {
                        gui.setItem(31, createItem(Material.GOLDEN_HELMET,
                                        isBedrock ? "§6§lCargo: Duque" : "§6§lSeu Cargo: Duque",
                                        null, null,
                                        isBedrock ? "§7Auxilia na administracao."
                                                        : "§7Você auxilia na administração real.",
                                        "",
                                        "§aAcesso de Nobre Liberado"));
                } else if (isSudito) {
                        gui.setItem(31, createItem(Material.IRON_HELMET,
                                        isBedrock ? "§e§lCargo: Sudito" : "§e§lSeu Cargo: Súdito",
                                        null, null,
                                        isBedrock ? "§7Cidadao leal."
                                                        : "§7Você é um cidadão leal.",
                                        "",
                                        "§7Use §b/rc §7para o chat do reino."));
                } else {
                        gui.setItem(31, createItem(Material.LEATHER_HELMET, "§7§lViajante",
                                        null, null,
                                        isBedrock ? "§7Apenas de passagem."
                                                        : "§7Você está apenas de passagem.",
                                        isBedrock ? "§7Confira as terras a venda!"
                                                        : "§7Confira as terras à venda!"));
                }

                player.openInventory(gui);
        }

        public void openFoundationMenu(Player player, Claim claim) {
                Inventory gui = Bukkit.createInventory(new KingdomHolder(claim.getId(), "FOUNDATION"), 27,
                                LegacyComponentSerializer.legacySection().deserialize("§8Fundação de Reino"));

                fillBorders(gui, claim.getId());
                boolean isDono = player.getUniqueId().equals(claim.getOwner());
                PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

                gui.setItem(4, createItem(Material.MAP, "§6§lComo fundar um reino?", null, null,
                                "§7Siga os passos abaixo para estabelecer sua soberania:",
                                "",
                                "§f1. §eReivindique a Coroa (Rei) §7(Clique abaixo)",
                                "§f2. §eNomeie suas terras (§b/reino nome <nome>§e)",
                                "",
                                "§b§lVantagens da Soberania:",
                                "§8- §fHabilita criação de §aFeudos (Lotes)",
                                "§8- §fBônus passivo de XP e Sorte",
                                "§8- §fAparece no menu global de reinos"));

                if (isDono) {
                        String claimId = claim.getId();
                        boolean hasKingPerm = player.hasPermission("gorvax.king");

                        if (!hasKingPerm && !pd.hasKingRank()) {
                                gui.setItem(13,
                                                createItem(Material.GOLDEN_HELMET, "§a§lPASSO 1: §fCoroação (Rei)",
                                                                "BUY_MAYOR", null,
                                                                "§7Preço: §6$50.000",
                                                                "",
                                                                "§eClique aqui para comprar o título!",
                                                                "§8(Necessário para oficializar o reino)"));
                        } else {
                                // Se já tem, mas o dado não salvou, atualiza
                                if (hasKingPerm && !pd.hasKingRank()) {
                                        pd.setKingRank(true);
                                        plugin.getPlayerDataManager().saveData(player.getUniqueId());
                                }
                                gui.setItem(13, createItem(Material.NAME_TAG, "§b§lPASSO 2: §fNomeie seu Reino", null,
                                                null,
                                                "§7Você já é um Rei! Agora falta o nome.",
                                                "",
                                                "§fDigite no chat: §e/reino nome <Nome_Aqui>",
                                                "",
                                                "§c[!] §7Use nomes sem espaços (ex: ReinoGorvax)"));
                        }
                } else {
                        gui.setItem(13, createItem(Material.IRON_BARS, "§c§lTerritório Particular", null, null,
                                        "§7Este terreno pertence a outro jogador."));
                }

                // [20] Botão de Regras (Só para dono)
                if (isDono || player.isOp() || player.hasPermission("gorvax.admin")) {
                        gui.setItem(20, createItem(Material.WRITABLE_BOOK, "§d§lRegras da Terra",
                                        "OPEN_GENERAL_PERMS", "MAIN",
                                        "§7Defina o que aliados podem",
                                        "§7fazer na sua terra.",
                                        "",
                                        "§7(Construção, Baús, Portas)",
                                        "",
                                        "§e[Clique para Configurar]"));

                        // [24] Botão de Aliados (Atalho direto)
                        gui.setItem(24, createItem(Material.PLAYER_HEAD, "§a§lGerenciar Aliados",
                                        "OPEN_TRUST_MENU", "MAIN",
                                        "§7Adicione amigos para que",
                                        "§7possam interagir na sua terra.",
                                        "",
                                        "§7Use §e/trust <nome> §7como atalho.",
                                        "",
                                        "§e[Clique para Gerenciar]"));
                }

                // [22] Comprar Blocos de Proteção (Acesso Universal)
                gui.setItem(22, createItem(Material.GOLD_BLOCK, "§6§lBlocos de Proteção", "BUY_BLOCKS", null,
                                "§7Adquira mais blocos para",
                                "§7expandir seus domínios.",
                                "",
                                "§fQuantidade: §e100 blocos",
                                "§fPreço: §6$5.000",
                                "",
                                "§e[Clique para Acessar]"));

                // [26] Botão de Deletar/Abandonar Terreno (Perigo)
                if (isDono || player.isOp() || player.hasPermission("gorvax.admin")) {
                        gui.setItem(26, createItem(Material.BARRIER, "§c§lAbandonar Terras", "DELETE_TERRAIN_CONFIRM",
                                        null,
                                        "§7Remove sua posse deste terreno.",
                                        "§7Todos os blocos e construções",
                                        "§7ficarão desprotegidos.",
                                        "",
                                        "§4[Clique para Abandonar]"));
                }

                player.openInventory(gui);
        }
}
