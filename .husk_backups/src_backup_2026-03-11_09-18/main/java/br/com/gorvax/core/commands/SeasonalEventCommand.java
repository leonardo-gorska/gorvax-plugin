package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.SeasonalEventManager;
import br.com.gorvax.core.managers.SeasonalEventManager.SeasonalEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * B17 — Comando /evento para sistema de Eventos Sazonais.
 * Subcomandos:
 * /evento — mostra evento ativo
 * /evento info — mostra evento ativo
 * /evento lista — lista todos os eventos do calendário
 * /evento iniciar <id> — força início de um evento (admin)
 * /evento parar — para evento ativo (admin)
 * /evento reload — recarrega config (admin)
 */
public class SeasonalEventCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public SeasonalEventCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        var msg = plugin.getMessageManager();
        SeasonalEventManager sem = plugin.getSeasonalEventManager();

        if (sem == null) {
            sender.sendMessage("§c[Gorvax] Sistema de eventos sazonais não inicializado.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            showEventInfo(sender, sem);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "lista":
            case "list":
            case "calendario":
                showEventList(sender, sem);
                return true;

            case "iniciar":
            case "start":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(sender, "seasonal_event.start_usage");
                    return true;
                }
                String eventId = args[1].toLowerCase();
                if (sem.forceStartEvent(eventId)) {
                    msg.send(sender, "seasonal_event.start_success", sem.getEvent(eventId).name());
                } else {
                    msg.send(sender, "seasonal_event.event_not_found", eventId);
                }
                return true;

            case "parar":
            case "stop":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (!sem.isEventActive()) {
                    msg.send(sender, "seasonal_event.no_active_event");
                    return true;
                }
                sem.forceStopEvent();
                msg.send(sender, "seasonal_event.stop_success");
                return true;

            case "reload":
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                sem.reload();
                msg.send(sender, "seasonal_event.reload_success");
                return true;

            default:
                showEventInfo(sender, sem);
                return true;
        }
    }

    private void showEventInfo(CommandSender sender, SeasonalEventManager sem) {
        var msg = plugin.getMessageManager();

        sender.sendMessage(msg.get("seasonal_event.info_header"));

        if (!sem.isEventActive()) {
            sender.sendMessage(msg.get("seasonal_event.no_active_event"));
            sender.sendMessage("");

            // Mostrar próximo evento
            SeasonalEvent next = findNextEvent(sem);
            if (next != null) {
                sender.sendMessage(msg.get("seasonal_event.next_event",
                        next.name(),
                        SeasonalEventManager.getMonthName(next.month())));
            }
        } else {
            SeasonalEvent event = sem.getActiveEvent();
            sender.sendMessage(msg.get("seasonal_event.active_name", event.name()));
            sender.sendMessage(msg.get("seasonal_event.active_description", event.description()));
            sender.sendMessage(msg.get("seasonal_event.active_days_remaining",
                    String.valueOf(sem.getDaysRemaining())));
            sender.sendMessage("");

            // Multiplicadores
            if (event.xpMultiplier() != 1.0) {
                sender.sendMessage(msg.get("seasonal_event.multiplier_xp",
                        String.format("%.1fx", event.xpMultiplier())));
            }
            if (event.lootMultiplier() != 1.0) {
                sender.sendMessage(msg.get("seasonal_event.multiplier_loot",
                        String.format("%.1fx", event.lootMultiplier())));
            }
            if (event.moneyMultiplier() != 1.0) {
                sender.sendMessage(msg.get("seasonal_event.multiplier_money",
                        String.format("%.1fx", event.moneyMultiplier())));
            }

            // Boss sazonal
            if (event.bossId() != null && !event.bossId().isEmpty()) {
                sender.sendMessage(msg.get("seasonal_event.boss_info", event.bossId()));
            }
        }

        sender.sendMessage(msg.get("seasonal_event.info_footer"));
    }

    private void showEventList(CommandSender sender, SeasonalEventManager sem) {
        var msg = plugin.getMessageManager();
        Map<String, SeasonalEvent> events = sem.getEvents();

        sender.sendMessage(msg.get("seasonal_event.list_header"));
        sender.sendMessage("");

        if (events.isEmpty()) {
            sender.sendMessage("  §7Nenhum evento sazonal configurado.");
        } else {
            for (SeasonalEvent event : events.values()) {
                String monthName = SeasonalEventManager.getMonthName(event.month());
                boolean isActive = sem.isEventActive()
                        && sem.getActiveEvent().id().equals(event.id());

                String status = isActive ? "§a§l[ATIVO]" : "§7[—]";
                sender.sendMessage(String.format(
                        "  %s §e%s §8| §f%s §7(%s %d-%d)",
                        status, event.name(), event.description(),
                        monthName, event.startDay(), event.endDay()));

                // Multiplicadores resumidos
                StringBuilder mults = new StringBuilder("    §7Multiplicadores: ");
                if (event.xpMultiplier() != 1.0)
                    mults.append("§bXP:").append(String.format("%.1fx ", event.xpMultiplier()));
                if (event.lootMultiplier() != 1.0)
                    mults.append("§dLoot:").append(String.format("%.1fx ", event.lootMultiplier()));
                if (event.moneyMultiplier() != 1.0)
                    mults.append("§aMoney:").append(String.format("%.1fx ", event.moneyMultiplier()));
                if (event.xpMultiplier() == 1.0 && event.lootMultiplier() == 1.0
                        && event.moneyMultiplier() == 1.0)
                    mults.append("§7Nenhum");

                sender.sendMessage(mults.toString());
            }
        }

        sender.sendMessage("");
        sender.sendMessage(msg.get("seasonal_event.list_footer"));
    }

    /**
     * Encontra o próximo evento baseado na data atual.
     */
    private SeasonalEvent findNextEvent(SeasonalEventManager sem) {
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int currentDay = java.time.LocalDate.now().getDayOfMonth();

        SeasonalEvent nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (SeasonalEvent event : sem.getEvents().values()) {
            int distance;
            if (event.month() > currentMonth
                    || (event.month() == currentMonth && event.startDay() > currentDay)) {
                // Evento futuro neste ano
                distance = (event.month() - currentMonth) * 30 + (event.startDay() - currentDay);
            } else {
                // Evento no próximo ano
                distance = (12 - currentMonth + event.month()) * 30 + event.startDay();
            }

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = event;
            }
        }

        return nearest;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("info");
            completions.add("lista");
            if (sender.hasPermission("gorvax.admin")) {
                completions.add("iniciar");
                completions.add("parar");
                completions.add("reload");
            }
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("iniciar") || args[0].equalsIgnoreCase("start"))) {
            if (sender.hasPermission("gorvax.admin")) {
                SeasonalEventManager sem = plugin.getSeasonalEventManager();
                if (sem != null) {
                    completions.addAll(sem.getEvents().keySet());
                }
                return filterCompletions(completions, args[1]);
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
