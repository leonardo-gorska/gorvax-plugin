package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.AuditManager;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.migration.ConfigMigrator;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.storage.DataStore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GorvaxCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final List<HelpItem> helpItems = new ArrayList<>();

    public GorvaxCommand(GorvaxCore plugin) {
        this.plugin = plugin;
        setupHelp();
    }

    private void setupHelp() {
        // Comandos de Jogador
        helpItems.add(new HelpItem("/reino", "Abre o menu principal do seu reino.", null));
        helpItems.add(new HelpItem("/reinonome <nome>", "Define o nome oficial do seu reino.", null));
        helpItems.add(new HelpItem("/lote info", "Mostra detalhes do terreno/lote atual.", null));
        helpItems.add(new HelpItem("/lote comprar", "Compra o lote onde você está pisando.", null));
        helpItems.add(new HelpItem("/lote abandonar", "Deixa de ser dono de um lote na cidade.", null));
        helpItems.add(new HelpItem("/lote alugar", "Aluga o lote atual (se disponível).", null));
        helpItems.add(new HelpItem("/lote amigo <nick>", "Dá permissão GERAL no lote.", null));

        helpItems.add(new HelpItem("/market", "Abre o mercado global.", null));
        helpItems.add(new HelpItem("/market local", "Abre o mercado do reino atual.", null));

        helpItems.add(new HelpItem("/rc <mensagem>", "Envia mensagem no chat do reino.", null));
        helpItems.add(new HelpItem("/ac <mensagem>", "Envia mensagem no chat de aliança.", null));
        helpItems.add(new HelpItem("/g <mensagem>", "Envia mensagem no chat global.", null));
        helpItems.add(new HelpItem("/l <mensagem>", "Envia mensagem no chat local.", null));
        helpItems.add(new HelpItem("/tc <mensagem>", "Envia mensagem no chat de comércio.", null));
        helpItems.add(new HelpItem("/chat <canal>", "Alterna canal de chat padrão.", null));
        helpItems.add(new HelpItem("/confirmar", "Confirma ação ou seleção (alias: /c).", null));

        helpItems.add(new HelpItem("/permitir <nick> [tipo]", "Dá permissão no terreno.", null));
        helpItems.add(new HelpItem("/remover <nick>", "Remove permissões de um jogador.", null));

        // COMANDOS DE REI
        helpItems.add(new HelpItem("/reino setspawn", "Define spawn do reino.", null));
        helpItems.add(new HelpItem("/reino transferir <nick>", "Transfere a coroa.", null));
        helpItems.add(new HelpItem("/reino deletar", "Deleta o reino (PERMANENTE).", null));
        helpItems.add(new HelpItem("/reino pvp global <on/off>", "Controla PvP no reino.", null));

        helpItems.add(new HelpItem("/lote criar", "Cria lote na área selecionada (Pá de Ferro).", null));
        helpItems.add(new HelpItem("/lote retomar", "Confisca lote de volta à coroa.", null));
        helpItems.add(new HelpItem("/lote preco <valor>", "Define preço de venda.", null));
        helpItems.add(new HelpItem("/lote aluguel <valor>", "Define aluguel diário.", null));

        // COMANDOS DE BOSS
        helpItems.add(new HelpItem("/boss next", "Mostra tempo para próximo boss.", null));
        helpItems.add(new HelpItem("/boss list", "Lista bosses vivos no mundo.", null));

        // COMANDOS ADMIN
        helpItems.add(new HelpItem("/boss start", "Inicia evento de boss.", "gorvax.admin"));
        helpItems.add(new HelpItem("/boss spawn <id>", "Spawna um boss manualmente.", "gorvax.admin"));
        helpItems.add(new HelpItem("/boss kill", "Remove todos os bosses ativos.", "gorvax.admin"));
        helpItems.add(new HelpItem("/boss testloot <boss> <rank>", "Testa loot de boss.", "gorvax.admin"));

        helpItems.add(new HelpItem("/gorvax reload", "Recarrega todas as configurações.", "gorvax.admin"));
        helpItems.add(new HelpItem("/gorvax hud", "Liga/desliga a scoreboard lateral.", null));
        helpItems.add(new HelpItem("/gorvax reset dragon", "Força reset da batalha do dragão.", "gorvax.admin"));
        helpItems.add(new HelpItem("/gorvax reset end", "Regenera completamente o The End.", "gorvax.admin"));

        // B9 — Visualização de Claims
        helpItems.add(new HelpItem("/gorvax mapa", "Mostra mapa ASCII dos claims próximos.", null));
        helpItems.add(new HelpItem("/gorvax som", "Liga/desliga som ao cruzar fronteira.", null));

        // B10 — Log de Auditoria
        helpItems.add(new HelpItem("/gorvax audit", "Consulta o log de auditoria.", "gorvax.admin"));
        helpItems.add(new HelpItem("/market historico", "Mostra suas transações recentes.", null));

        // B18 — Migração de Storage
        helpItems
                .add(new HelpItem("/gorvax migrate <origem> <destino>", "Migra dados entre backends.", "gorvax.admin"));

        // B33 — Migração de Configurações
        helpItems.add(new HelpItem("/gorvax migrateconfig", "Força re-migração de configurações.", "gorvax.admin"));

        // B11 — Mini-Bosses
        helpItems.add(new HelpItem("/miniboss list", "Lista mini-bosses ativos.", null));
        helpItems.add(new HelpItem("/miniboss spawn <id>", "Spawna um mini-boss.", "gorvax.admin"));
        helpItems.add(new HelpItem("/miniboss kill", "Remove todos os mini-bosses.", "gorvax.admin"));

        // B12 — Crates / Keys
        helpItems.add(new HelpItem("/crate", "Sistema de crates e chaves.", null));

        // B13 — Cosméticos
        helpItems.add(new HelpItem("/cosmetics", "Menu de cosméticos (partículas, trails, tags).", null));
        helpItems.add(new HelpItem("/cosmetics listar", "Lista cosméticos desbloqueados.", null));

        // B14-VIP — Ranks Premium
        helpItems.add(new HelpItem("/vip", "Mostra tabela de benefícios VIP.", null));
        helpItems.add(new HelpItem("/vip status", "Mostra seu rank VIP atual.", null));

        // B15 — Battle Pass
        helpItems.add(new HelpItem("/pass", "Abre menu do Battle Pass sazonal.", null));
        helpItems.add(new HelpItem("/pass info", "Informações da temporada atual.", null));
        helpItems.add(new HelpItem("/pass nivel", "Mostra seu nível e XP.", null));

        // B17 — Eventos Sazonais
        helpItems.add(new HelpItem("/evento", "Info sobre o evento sazonal ativo.", null));
        helpItems.add(new HelpItem("/evento lista", "Lista todos os eventos do calendário.", null));
        helpItems.add(new HelpItem("/evento iniciar <id>", "Força início de um evento.", "gorvax.admin"));
        helpItems.add(new HelpItem("/evento parar", "Para o evento ativo.", "gorvax.admin"));

        // B18 — Karma / Reputação
        helpItems.add(new HelpItem("/karma", "Ver seu karma e reputação.", null));
        helpItems.add(new HelpItem("/karma top", "Ranking de karma do servidor.", null));
        helpItems.add(new HelpItem("/karma set <nick> <valor>", "Define karma de um jogador.", "gorvax.admin"));
        helpItems.add(new HelpItem("/karma add <nick> <valor>", "Altera karma de um jogador.", "gorvax.admin"));

        helpItems.add(new HelpItem("/reino darblocos <nick> <qtd>", "Dá blocos extras.", "gorvax.admin"));
        helpItems.add(new HelpItem("/reino reload", "Recarrega sistema de reinos.", "gorvax.admin"));
        helpItems.add(new HelpItem("/market reload", "Recarrega sistema de mercado.", "gorvax.admin"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        // B6.1 — Removido bloco morto do /market (duplicava MarketCommand)

        if (args.length == 0) {
            showHelp(sender, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        var msg = plugin.getMessageManager();

        switch (sub) {
            case "help":
            case "ajuda":
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                showHelp(sender, page);
                return true;

            case "hud":
                if (!(sender instanceof Player hudPlayer)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().toggleHud(hudPlayer);
                }
                return true;

            case "reload":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                plugin.reloadConfig();

                // Recarrega todos os sistemas dependentes
                if (plugin.getMessageManager() != null) {
                    plugin.getMessageManager().reload();
                }
                if (plugin.getBossManager() != null && plugin.getBossManager().getConfigManager() != null) {
                    plugin.getBossManager().getConfigManager().reload();
                }
                if (plugin.getKingdomManager() != null) {
                    plugin.getKingdomManager().reload();
                }
                if (plugin.getClaimManager() != null) {
                    plugin.getClaimManager().reload();
                }
                if (plugin.getMarketManager() != null) {
                    plugin.getMarketManager().reload();
                }
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().reload();
                }
                if (plugin.getChatManager() != null) {
                    plugin.getChatManager().reload();
                }
                if (plugin.getAuditManager() != null) {
                    plugin.getAuditManager().reload();
                }
                // B10 — Custom Items
                if (plugin.getCustomItemManager() != null) {
                    plugin.getCustomItemManager().reload();
                }
                // B11 — Mini-Bosses
                if (plugin.getMiniBossManager() != null) {
                    plugin.getMiniBossManager().reload();
                }
                // B12 — Crates / Keys
                if (plugin.getCrateManager() != null) {
                    plugin.getCrateManager().reload();
                }
                // B13 — Cosméticos
                if (plugin.getCosmeticManager() != null) {
                    plugin.getCosmeticManager().reload();
                }
                // B14-VIP — VIP & Ranks Premium
                if (plugin.getVipManager() != null) {
                    plugin.getVipManager().reload();
                }
                // B15 — Battle Pass
                if (plugin.getBattlePassManager() != null) {
                    plugin.getBattlePassManager().reload();
                }
                // B16 — Quests
                if (plugin.getQuestManager() != null) {
                    plugin.getQuestManager().reload();
                }
                // B17 — Eventos Sazonais
                if (plugin.getSeasonalEventManager() != null) {
                    plugin.getSeasonalEventManager().reload();
                }
                // B18 — Reputação / Karma
                if (plugin.getReputationManager() != null) {
                    plugin.getReputationManager().reload();
                }
                // B2 — Combat Manager
                if (plugin.getCombatManager() != null) {
                    plugin.getCombatManager().reload();
                }
                // B28 — Códex de Gorvax
                if (plugin.getCodexManager() != null) {
                    plugin.getCodexManager().reload();
                }
                msg.send(sender, "admin.reload_success");
                return true;

            case "reset":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(sender, "admin.reset_usage");
                    return true;
                }

                String target = args[1].toLowerCase();

                if (target.equals("dragon")) {
                    if (plugin.getEndResetManager() != null) {
                        try {
                            plugin.getEndResetManager().resetDragonBattle();
                            msg.send(sender, "admin.reset_dragon_sent");
                        } catch (Exception e) {
                            org.bukkit.Bukkit.getLogger()
                                    .severe("[GorvaxCore] Erro ao resetar batalha do dragão: " + e.getMessage());
                            msg.send(sender, "admin.reset_dragon_error", e.getMessage());
                        }
                    } else {
                        msg.send(sender, "admin.reset_manager_null");
                    }
                } else if (target.equals("end")) {
                    if (plugin.getEndResetManager() != null) {
                        try {
                            msg.send(sender, "admin.reset_end_starting");
                            plugin.getEndResetManager().resetEndDimension();
                            msg.send(sender, "admin.reset_end_sent");
                        } catch (Exception e) {
                            org.bukkit.Bukkit.getLogger()
                                    .severe("[GorvaxCore] Erro ao resetar dimensão do End: " + e.getMessage());
                            msg.send(sender, "admin.reset_end_error", e.getMessage());
                        }
                    } else {
                        msg.send(sender, "admin.reset_manager_null");
                    }
                } else {
                    msg.send(sender, "admin.reset_invalid_option");
                }
                return true;

            // B9 — Mapa ASCII de claims
            case "mapa":
            case "map":
                if (!(sender instanceof Player mapPlayer)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                showClaimMap(mapPlayer);
                return true;

            // B9 — Toggle de som de fronteira
            case "som":
            case "sound":
                if (!(sender instanceof Player soundPlayer)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                PlayerData spd = plugin.getPlayerDataManager().getData(soundPlayer.getUniqueId());
                boolean newState = !spd.isBorderSound();
                spd.setBorderSound(newState);
                plugin.getPlayerDataManager().saveData(soundPlayer.getUniqueId());
                msg.send(soundPlayer, newState ? "visualization.sound_enabled" : "visualization.sound_disabled");
                return true;

            // B10 — Log de Auditoria
            case "audit":
            case "auditoria":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                handleAuditCommand(sender, args);
                return true;

            // B18 — Migração de Storage
            case "migrate":
            case "migrar":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                handleMigrateCommand(sender, args);
                return true;

            // B33 — Migração de Configurações
            case "migrateconfig":
            case "migrarconfig":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                handleMigrateConfigCommand(sender);
                return true;

            // B6 — Menu Central GUI
            case "menu":
                if (!(sender instanceof Player menuPlayer)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                if (plugin.getMainMenuGUI() != null) {
                    plugin.getMainMenuGUI().open(menuPlayer);
                }
                return true;

            default:
                showHelp(sender, 1);
                return true;
        }
    }

    // B9.3 — Mapa ASCII de claims (15x15 chunks)
    private void showClaimMap(Player p) {
        var msg = plugin.getMessageManager();
        Location pLoc = p.getLocation();
        int centerChunkX = pLoc.getBlockX() >> 4;
        int centerChunkZ = pLoc.getBlockZ() >> 4;
        int mapRadius = 7; // 15x15 = -7 to +7

        Claim playerKingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        String playerKingdomId = playerKingdom != null ? playerKingdom.getId() : null;
        java.util.List<String> alliances = playerKingdomId != null
                ? plugin.getKingdomManager().getAlliances(playerKingdomId)
                : new java.util.ArrayList<>();

        p.sendMessage(msg.get("visualization.map_header"));
        p.sendMessage("");

        StringBuilder sb = new StringBuilder();

        for (int dz = -mapRadius; dz <= mapRadius; dz++) {
            sb.setLength(0);
            sb.append("  "); // Indentação

            for (int dx = -mapRadius; dx <= mapRadius; dx++) {
                if (dx == 0 && dz == 0) {
                    sb.append("§f✦"); // Posição do jogador
                    continue;
                }

                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;

                // Centro do chunk → coordenada de bloco
                int blockX = (chunkX << 4) + 8;
                int blockZ = (chunkZ << 4) + 8;

                Location chunkCenter = new Location(pLoc.getWorld(), blockX, 64, blockZ);
                Claim claim = plugin.getClaimManager().getClaimAt(chunkCenter);

                if (claim == null) {
                    sb.append("§7·"); // Livre
                } else if (claim.getOwner().equals(p.getUniqueId())
                        || claim.hasPermission(p.getUniqueId(), Claim.TrustType.CONSTRUCAO)) {
                    sb.append("§a█"); // Seu claim
                } else if (claim.isKingdom() && playerKingdomId != null) {
                    if (claim.getId().equals(playerKingdomId)) {
                        sb.append("§a█"); // Seu reino
                    } else if (alliances.contains(claim.getId())) {
                        sb.append("§b▓"); // Aliado
                    } else if (plugin.getKingdomManager().areEnemies(playerKingdomId, claim.getId())) {
                        sb.append("§c░"); // Inimigo
                    } else {
                        sb.append("§e▒"); // Outros
                    }
                } else {
                    sb.append("§e▒"); // Outro jogador
                }
            }

            p.sendMessage(sb.toString());
        }

        p.sendMessage("");
        p.sendMessage(msg.get("visualization.map_legend"));
        p.sendMessage(msg.get("visualization.map_coords", centerChunkX * 16, centerChunkZ * 16));
    }

    private void showHelp(CommandSender sender, int page) {
        var msg = plugin.getMessageManager();
        List<HelpItem> visibleItems = helpItems.stream()
                .filter(item -> item.permission == null || sender.hasPermission(item.permission))
                .toList();

        int maxPages = (int) Math.ceil(visibleItems.size() / 10.0);
        if (page > maxPages)
            page = maxPages;
        if (page < 1)
            page = 1;

        sender.sendMessage(msg.get("admin.help_header"));
        sender.sendMessage(msg.get("admin.help_title", page, maxPages));
        sender.sendMessage(msg.get("admin.help_subtitle"));
        sender.sendMessage("");

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, visibleItems.size());

        for (int i = start; i < end; i++) {
            HelpItem item = visibleItems.get(i);
            sender.sendMessage(msg.get("admin.help_entry", item.command, item.description));
        }

        sender.sendMessage("");
        if (page < maxPages) {
            sender.sendMessage(msg.get("admin.help_next_page", page + 1));
        }
        sender.sendMessage(msg.get("admin.help_header"));
    }

    // B10 — Subcomando /gorvax audit
    private void handleAuditCommand(CommandSender sender, String[] args) {
        var msg = plugin.getMessageManager();
        AuditManager audit = plugin.getAuditManager();

        if (audit == null) {
            sender.sendMessage("§c[Auditoria] Sistema de auditoria não inicializado.");
            return;
        }

        // /gorvax audit — mostra últimas 10
        if (args.length == 1) {
            showAuditEntries(sender, audit.getRecentEntries(10), msg.get("audit.header_recent"));
            return;
        }

        String subArg = args[1].toLowerCase();

        // /gorvax audit <número> — mostra últimas N
        try {
            int count = Integer.parseInt(subArg);
            count = Math.min(count, 50);
            count = Math.max(count, 1);
            showAuditEntries(sender, audit.getRecentEntries(count), msg.get("audit.header_recent"));
            return;
        } catch (NumberFormatException ignored) {
        }

        switch (subArg) {
            case "player":
            case "jogador":
                if (args.length < 3) {
                    sender.sendMessage(msg.get("audit.usage_player"));
                    return;
                }
                String playerName = args[2];
                org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(playerName);
                if (target.getUniqueId() != null) {
                    List<AuditManager.AuditEntry> playerEntries = audit.getPlayerHistory(target.getUniqueId(), 20);
                    if (playerEntries.isEmpty()) {
                        sender.sendMessage(msg.get("audit.no_entries_player", playerName));
                    } else {
                        showAuditEntries(sender, playerEntries, msg.get("audit.header_player", playerName));
                    }
                } else {
                    sender.sendMessage(msg.get("audit.player_not_found"));
                }
                return;

            case "market":
            case "mercado":
                List<AuditManager.AuditEntry> marketEntries = audit.getAllMarketHistory(20);
                if (marketEntries.isEmpty()) {
                    sender.sendMessage(msg.get("audit.no_entries_market"));
                } else {
                    showAuditEntries(sender, marketEntries, msg.get("audit.header_market"));
                }
                return;

            default:
                // Tentar buscar como texto livre
                List<AuditManager.AuditEntry> searchResults = audit.search(subArg, 20);
                if (searchResults.isEmpty()) {
                    sender.sendMessage(msg.get("audit.no_results", subArg));
                } else {
                    showAuditEntries(sender, searchResults, msg.get("audit.header_search", subArg));
                }
        }
    }

    /**
     * Exibe uma lista de entradas de auditoria formatadas.
     */
    private void showAuditEntries(CommandSender sender, List<AuditManager.AuditEntry> entries, String header) {
        var msg = plugin.getMessageManager();
        sender.sendMessage(msg.get("audit.separator"));
        sender.sendMessage(header);
        sender.sendMessage("");

        for (AuditManager.AuditEntry entry : entries) {
            String valueStr = entry.value != 0.0 ? String.format(" §6$%.2f", entry.value) : "";
            sender.sendMessage(String.format(
                    "  %s §7%s §f%s §8» §7%s%s",
                    entry.getActionIcon(),
                    entry.getFormattedDate(),
                    entry.playerName,
                    entry.details,
                    valueStr));
        }

        sender.sendMessage("");
        sender.sendMessage(msg.get("audit.separator"));
        sender.sendMessage(msg.get("audit.footer", entries.size()));
    }

    // B18 — Subcomando /gorvax migrate <origem> <destino>
    private void handleMigrateCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§b[Gorvax] §fUso: §e/gorvax migrate <origem> <destino>");
            sender.sendMessage("§7Tipos válidos: §fyaml§7, §fsqlite§7, §fmysql");
            sender.sendMessage("§7Exemplo: §f/gorvax migrate yaml sqlite");
            return;
        }

        DataStore.StorageType sourceType;
        DataStore.StorageType targetType;
        try {
            sourceType = DataStore.StorageType.valueOf(args[1].toUpperCase());
            targetType = DataStore.StorageType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c[Gorvax] Tipo inválido! Use: yaml, sqlite ou mysql");
            return;
        }

        if (sourceType == targetType) {
            sender.sendMessage("§c[Gorvax] Origem e destino não podem ser iguais.");
            return;
        }

        sender.sendMessage("§b[Gorvax] §eIniciando migração: §f" + sourceType + " §e→ §f" + targetType);
        sender.sendMessage("§7Isso pode levar alguns segundos...");

        // Executar migração em thread async para não travar o servidor
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDatabaseManager().migrateData(sourceType, targetType);

                // Voltar para a main thread para enviar mensagem
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§a[Gorvax] ✓ Migração concluída com sucesso!");
                    sender.sendMessage(
                            "§7Para usar o novo backend, altere §fstorage.type§7 no §fconfig.yml§7 e reinicie.");
                });
            } catch (Exception e) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§c[Gorvax] ✗ Erro na migração: " + e.getMessage());
                    plugin.getLogger().severe("[Storage] Erro na migração: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }

    // B33 — Subcomando /gorvax migrateconfig
    private void handleMigrateConfigCommand(CommandSender sender) {
        var msg = plugin.getMessageManager();
        ConfigMigrator migrator = new ConfigMigrator(plugin);

        if (migrator.getCurrentVersion() >= migrator.getLatestVersion()) {
            sender.sendMessage(
                    "§e[Gorvax] Configurações já estão atualizadas (v" + migrator.getCurrentVersion() + ").");
            sender.sendMessage("§7Forçando re-migração...");
        }

        int applied = migrator.forceMigrate();
        migrator.migrateMessages();

        if (applied > 0) {
            sender.sendMessage("§a[Gorvax] ✓ Re-migração concluída! " + applied + " step(s) aplicado(s).");
            sender.sendMessage("§7Versão atual do config: v" + migrator.getCurrentVersion());
        } else {
            sender.sendMessage("§e[Gorvax] Nenhuma migração pendente.");
        }
    }

    private record HelpItem(String command, String description, String permission) {
    }

    // B6.2 — Tab Completion para /gorvax
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
            completions.add("hud");
            completions.add("menu");
            completions.add("mapa");
            completions.add("som");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("reload");
                completions.add("reset");
                completions.add("audit");
                completions.add("migrate");
                completions.add("migrateconfig");
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reset") && sender.hasPermission("gorvax.admin")) {
            completions.add("dragon");
            completions.add("end");
            return filterCompletions(completions, args[1]);
        }

        // B10 — Tab completion para /gorvax audit
        if (args.length == 2 && args[0].equalsIgnoreCase("audit") && sender.hasPermission("gorvax.admin")) {
            completions.add("player");
            completions.add("market");
            return filterCompletions(completions, args[1]);
        }

        // B18 — Tab completion para /gorvax migrate
        if (args[0].equalsIgnoreCase("migrate") && sender.hasPermission("gorvax.admin")) {
            if (args.length == 2 || args.length == 3) {
                completions.add("yaml");
                completions.add("sqlite");
                completions.add("mysql");
                return filterCompletions(completions, args[args.length - 1]);
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
