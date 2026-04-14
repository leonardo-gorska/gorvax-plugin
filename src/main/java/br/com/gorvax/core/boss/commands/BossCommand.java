package br.com.gorvax.core.boss.commands;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import br.com.gorvax.core.boss.managers.BossScheduleManager;
import br.com.gorvax.core.boss.managers.BossRaidManager;
import br.com.gorvax.core.boss.model.*;
import br.com.gorvax.core.managers.MessageManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.stream.Collectors;

public class BossCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public BossCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        var msg = plugin.getMessageManager();

        if (!sender.hasPermission("gorvax.admin")) {
            msg.send(sender, "boss.no_permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, msg);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                // 1. Recarrega o arquivo físico (YAML)
                plugin.getBossManager().getConfigManager().reload();

                // 2. Limpa o cache de loots pendentes na memória para aplicar as novas configs
                plugin.getBossManager().getLootManager().clearLootCache();

                // 3. B11 — Recarrega schedule e raid managers
                plugin.getBossManager().getScheduleManager().reload();
                plugin.getBossManager().getRaidManager().reload();

                msg.send(sender, "boss.reload_success");
                break;

            case "start":
                plugin.getBossManager().spawnRandomBoss();
                msg.send(sender, "boss.start_success");
                break;

            case "spawn":
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "boss.spawn_player_only");
                    return true;
                }

                if (args.length > 1) {
                    String bossId = args[1];
                    java.util.Set<String> validBosses = java.util.Set.of(
                            "rei_gorvax", "indrax_abissal", "indrax",
                            "vulgathor", "xylos", "skulkor", "kaldur", "zarith",
                            "rei_indrax");
                    if (validBosses.contains(bossId.toLowerCase())) {
                        plugin.getBossManager().spawnBoss(bossId, player.getLocation());
                        msg.send(sender, "boss.spawn_success", bossId);
                    } else {
                        msg.send(sender, "boss.spawn_not_found");
                        msg.send(sender, "boss.spawn_available_list");
                    }
                } else {
                    plugin.getBossManager().spawnRandomBossAt(player.getLocation());
                    msg.send(sender, "boss.spawn_random_success");
                }
                break;

            case "next":
                long diff = plugin.getBossManager().getNextSpawnTime() - System.currentTimeMillis();

                if (diff <= 0) {
                    msg.send(sender, "boss.next_imminent");
                    return true;
                }

                long minutos = (diff / 1000) / 60;
                long segundos = (diff / 1000) % 60;

                sender.sendMessage(" ");
                msg.send(sender, "boss.next_title");
                msg.send(sender, "boss.next_time", minutos, segundos);
                msg.send(sender, "boss.next_hint");
                sender.sendMessage(" ");
                break;

            case "kill":
                Map<UUID, WorldBoss> active = plugin.getBossManager().getActiveBosses();
                int killed = 0;

                for (UUID id : new HashSet<>(active.keySet())) {
                    WorldBoss boss = active.get(id);
                    if (boss != null) {
                        // Usa removeBoss() centralizado (cleanup, BossBar, AtmosphereManager)
                        plugin.getBossManager().removeBoss(boss);

                        if (boss.getEntity() != null)
                            boss.getEntity().remove();

                        org.bukkit.Bukkit
                                .broadcast(LegacyComponentSerializer.legacySection().deserialize(msg.get("boss.kill_broadcast", boss.getName())));
                        killed++;
                    }
                }
                // Forçar restauração atmosférica após remover todos
                plugin.getBossManager().getAtmosphereManager().restoreNormal();
                msg.send(sender, "boss.kill_success", killed);
                break;

            case "list":
            case "status":
                Map<UUID, WorldBoss> activeList = plugin.getBossManager().getActiveBosses();
                if (activeList.isEmpty()) {
                    msg.send(sender, "boss.list_empty");
                    return true;
                }
                msg.send(sender, "boss.list_header");
                activeList.values().forEach(boss -> {
                    if (boss.getEntity() != null) {
                        sender.sendMessage(msg.get("boss.list_entry",
                                boss.getName(),
                                (int) boss.getEntity().getHealth(),
                                boss.getEntity().getLocation().getBlockX(),
                                boss.getEntity().getLocation().getBlockZ()));
                    }
                });
                break;

            case "testloot":
                if (!(sender instanceof Player)) {
                    msg.send(sender, "boss.testloot_player_only");
                    return true;
                }

                Player testPlayer = (Player) sender;

                if (args.length < 3) {
                    msg.send(testPlayer, "boss.testloot_usage");
                    return true;
                }

                String arg1 = args[1];
                String arg2 = args[2];

                String bossId = null;
                int rank = -1;

                // Tenta detectar qual é o número e qual é o texto
                if (arg1.matches("\\d+")) {
                    rank = Integer.parseInt(arg1);
                    bossId = arg2.toLowerCase();
                } else if (arg2.matches("\\d+")) {
                    bossId = arg1.toLowerCase();
                    rank = Integer.parseInt(arg2);
                } else {
                    msg.send(testPlayer, "boss.testloot_rank_nan");
                    return true;
                }

                if (!plugin.getBossManager().getConfigManager().getRewards().contains(bossId)) {
                    msg.send(testPlayer, "boss.testloot_boss_not_found", bossId);
                    return true;
                }

                if (rank > 5) {
                    msg.send(testPlayer, "boss.testloot_participation", bossId);
                    msg.send(testPlayer, "boss.testloot_participation_hint");
                } else {
                    msg.send(testPlayer, "boss.testloot_success", rank, bossId);
                }

                plugin.getBossManager().getLootManager().generateLoot(testPlayer, rank, bossId);
                plugin.getBossManager().getLootManager().openPersonalLoot(testPlayer);
                break;

            // ===== B11 — Boss Events e Calendário =====

            case "schedule":
            case "agenda":
                handleSchedule(sender, msg);
                break;

            case "raid":
                handleRaid(sender, args, msg);
                break;

            case "seasonal":
            case "sazonal":
                handleSeasonal(sender, msg);
                break;

            default:
                sendHelp(sender, msg);
                break;
        }
        return true;
    }

    /**
     * B11.1 — Mostra próximos eventos agendados.
     */
    private void handleSchedule(CommandSender sender, MessageManager msg) {
        BossScheduleManager scheduleManager = plugin.getBossManager().getScheduleManager();
        if (!scheduleManager.isEnabled()) {
            msg.send(sender, "boss_schedule.disabled");
            return;
        }

        List<BossScheduleManager.UpcomingEvent> events = scheduleManager.getNextEvents(5);
        if (events.isEmpty()) {
            msg.send(sender, "boss_schedule.list_empty");
            return;
        }

        sender.sendMessage(" ");
        msg.send(sender, "boss_schedule.list_header");
        int i = 1;
        for (BossScheduleManager.UpcomingEvent event : events) {
            String dayName = event.event.day.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
            String bossName = event.event.bossId;
            String timeStr = event.event.time.toString();
            String remaining = event.formatTimeRemaining();
            msg.send(sender, "boss_schedule.list_entry", i, bossName + " §7(" + dayName + " " + timeStr + ")", dayName, remaining);
            i++;
        }
        sender.sendMessage(" ");
    }

    /**
     * B11.2 — Gerencia raids.
     */
    private void handleRaid(CommandSender sender, String[] args, MessageManager msg) {
        BossRaidManager raidManager = plugin.getBossManager().getRaidManager();

        if (args.length < 2) {
            // Mostrar status da raid
            if (raidManager.isRaidActive()) {
                msg.send(sender, "boss_raid.status_header");
                msg.send(sender, "boss_raid.status_active", raidManager.getCurrentWave(), raidManager.getTotalWaves());
                msg.send(sender, "boss_raid.status_participants", raidManager.getParticipants().size());
            } else {
                msg.send(sender, "boss_raid.help_start");
                msg.send(sender, "boss_raid.help_stop");
            }
            return;
        }

        switch (args[1].toLowerCase()) {
            case "start":
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "boss.spawn_player_only");
                    return;
                }

                if (!raidManager.isEnabled()) {
                    msg.send(sender, "boss_raid.disabled");
                    return;
                }

                if (raidManager.isRaidActive()) {
                    msg.send(sender, "boss_raid.already_active");
                    return;
                }

                boolean started = raidManager.startRaid(player.getLocation());
                if (!started) {
                    msg.send(sender, "boss_raid.not_enough_players", raidManager.getMinPlayers());
                }
                break;

            case "stop":
                if (raidManager.isRaidActive()) {
                    raidManager.endRaid(false);
                    msg.send(sender, "boss_raid.stopped");
                } else {
                    msg.send(sender, "boss_raid.no_active");
                }
                break;

            default:
                msg.send(sender, "boss_raid.help_usage");
                break;
        }
    }

    /**
     * B11.3 — Mostra bosses sazonais ativos.
     */
    private void handleSeasonal(CommandSender sender, MessageManager msg) {
        if (!plugin.getConfig().getBoolean("boss.seasonal.enabled", false)) {
            msg.send(sender, "boss_seasonal.disabled");
            return;
        }

        var seasonalSection = plugin.getConfig().getConfigurationSection("boss.seasonal.events");
        if (seasonalSection == null) {
            msg.send(sender, "boss_seasonal.none_active");
            return;
        }

        java.time.LocalDate now = java.time.LocalDate.now();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int current = month * 100 + day;

        sender.sendMessage(" ");
        msg.send(sender, "boss_seasonal.active_header");
        int count = 0;

        for (String key : seasonalSection.getKeys(false)) {
            var eventSection = seasonalSection.getConfigurationSection(key);
            if (eventSection == null) continue;

            String startStr = eventSection.getString("start", "");
            String endStr = eventSection.getString("end", "");
            String nameOverride = eventSection.getString("name_override", key);

            try {
                String[] startParts = startStr.split("-");
                String[] endParts = endStr.split("-");
                int start = Integer.parseInt(startParts[0]) * 100 + Integer.parseInt(startParts[1]);
                int end = Integer.parseInt(endParts[0]) * 100 + Integer.parseInt(endParts[1]);

                boolean active;
                if (start <= end) {
                    active = current >= start && current <= end;
                } else {
                    active = current >= start || current <= end;
                }

                if (active) {
                    count++;
                    msg.send(sender, "boss_seasonal.active_entry", count, nameOverride, startStr, endStr);
                }
            } catch (Exception ignored) {
                plugin.getLogger().fine("Evento sazonal inválido '" + key + "': " + ignored.getMessage());
            }
        }

        if (count == 0) {
            msg.send(sender, "boss_seasonal.none_active");
        }
        sender.sendMessage(" ");
    }

    private void sendHelp(CommandSender s, MessageManager msg) {
        s.sendMessage(" ");
        msg.send(s, "boss.help_header");
        msg.send(s, "boss.help_next");
        msg.send(s, "boss.help_reload");
        msg.send(s, "boss.help_start");
        msg.send(s, "boss.help_spawn");
        msg.send(s, "boss.help_spawn_list");
        msg.send(s, "boss.help_kill");
        msg.send(s, "boss.help_status");
        msg.send(s, "boss.help_testloot");
        // B11
        msg.send(s, "boss.help_schedule");
        msg.send(s, "boss.help_raid");
        msg.send(s, "boss.help_seasonal");
        s.sendMessage(" ");
    }

    // B6.2 — Tab Completion para /boss
    private static final List<String> BOSS_IDS = List.of(
            "rei_gorvax", "indrax_abissal", "vulgathor", "xylos", "skulkor", "kaldur", "zarith", "rei_indrax");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("next", "list", "reload", "start", "spawn", "kill", "testloot", "status",
                    "schedule", "agenda", "raid", "seasonal", "sazonal"));
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("spawn") || sub.equals("testloot")) {
                return filterCompletions(new ArrayList<>(BOSS_IDS), args[1]);
            }
            if (sub.equals("raid")) {
                return filterCompletions(new ArrayList<>(List.of("start", "stop")), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("testloot")) {
            completions.addAll(List.of("1", "2", "3", "4", "5"));
            return filterCompletions(completions, args[2]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}