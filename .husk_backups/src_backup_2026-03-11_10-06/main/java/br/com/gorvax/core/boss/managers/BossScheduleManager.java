package br.com.gorvax.core.boss.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * B11.1 — Gerenciador de agenda semanal de bosses.
 * Permite configurar horários fixos para spawn de bosses e envia avisos
 * automáticos antes do evento.
 */
public class BossScheduleManager {

    private final GorvaxCore plugin;
    private BukkitTask checkTask;

    // Lista de eventos agendados (parseados do config)
    private final List<ScheduledEvent> events = new ArrayList<>();

    // Intervalos de aviso em minutos (ex: 30, 10, 5, 1)
    private List<Integer> warningIntervals = List.of(30, 10, 5, 1);

    // Rastreia avisos já enviados para evitar duplicatas (chave:
    // "eventHash_minuteWarning")
    private final Set<String> sentWarnings = Collections.synchronizedSet(new HashSet<>());

    // Rastreia eventos já disparados nesta janela (para evitar spawn duplicado)
    private final Set<String> firedEvents = Collections.synchronizedSet(new HashSet<>());

    public BossScheduleManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        startCheckTask();
        plugin.getLogger()
                .info("§a[BossScheduleManager] Sistema de agendamento inicializado com " + events.size() + " eventos.");
    }

    /**
     * Carrega eventos do config.yml seção boss.schedule.
     */
    private void loadConfig() {
        events.clear();
        sentWarnings.clear();
        firedEvents.clear();

        boolean enabled = plugin.getConfig().getBoolean("boss.schedule.enabled", false);
        if (!enabled)
            return;

        List<String> eventLines = plugin.getConfig().getStringList("boss.schedule.events");
        List<Integer> warnings = plugin.getConfig().getIntegerList("boss.schedule.warnings");
        if (!warnings.isEmpty()) {
            warningIntervals = warnings;
        }

        for (String line : eventLines) {
            try {
                // Formato: "DAY HH:MM boss_id"
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 3) {
                    plugin.getLogger().warning(
                            "[BossScheduleManager] Formato inválido: '" + line + "' (esperado: DAY HH:MM boss_id)");
                    continue;
                }

                DayOfWeek day = DayOfWeek.valueOf(parts[0].toUpperCase());
                String[] timeParts = parts[1].split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                String bossId = parts[2].toLowerCase();

                events.add(new ScheduledEvent(day, LocalTime.of(hour, minute), bossId));
            } catch (Exception e) {
                plugin.getLogger()
                        .warning("[BossScheduleManager] Erro ao parsear evento: '" + line + "': " + e.getMessage());
            }
        }
    }

    /**
     * Task que verifica a cada 30 segundos se algum evento está próximo.
     */
    private void startCheckTask() {
        // A cada 600 ticks (30 segundos)
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (events.isEmpty())
                return;

            LocalDateTime now = LocalDateTime.now();

            for (ScheduledEvent event : events) {
                LocalDateTime nextOccurrence = event.getNextOccurrence(now);
                long minutesUntil = java.time.Duration.between(now, nextOccurrence).toMinutes();

                // Envia avisos
                for (int warningMin : warningIntervals) {
                    if (minutesUntil == warningMin || (minutesUntil == warningMin + 1 && now.getSecond() >= 30)) {
                        String warningKey = event.hashKey() + "_" + warningMin + "_" + nextOccurrence.getDayOfWeek();
                        if (sentWarnings.add(warningKey)) {
                            sendWarning(event, warningMin);
                        }
                    }
                }

                // Verifica se está no momento de spawn (janela de 60 segundos)
                long secondsUntil = java.time.Duration.between(now, nextOccurrence).toSeconds();
                if (secondsUntil >= 0 && secondsUntil <= 30) {
                    String fireKey = event.hashKey() + "_" + nextOccurrence.getDayOfWeek() + "_"
                            + nextOccurrence.getHour() + "_" + nextOccurrence.getMinute();
                    if (firedEvents.add(fireKey)) {
                        spawnScheduledBoss(event);
                    }
                }
            }

            // Limpa avisos/fires antigos (mais de 2 horas)
            cleanupOldEntries();

        }, 600L, 600L);
    }

    /**
     * Envia aviso broadcast sobre evento próximo.
     */
    private void sendWarning(ScheduledEvent event, int minutesUntil) {
        String bossName = resolveBossDisplayName(event.bossId);

        if (minutesUntil >= 30) {
            plugin.getMessageManager().broadcast("boss_schedule.warning_30",
                    bossName, event.day.name(), event.time.toString());
        } else if (minutesUntil >= 10) {
            plugin.getMessageManager().broadcast("boss_schedule.warning_10",
                    bossName, minutesUntil);
        } else if (minutesUntil >= 5) {
            plugin.getMessageManager().broadcast("boss_schedule.warning_5",
                    bossName, minutesUntil);
        } else {
            plugin.getMessageManager().broadcast("boss_schedule.warning_1",
                    bossName, minutesUntil);
        }

        // Som de aviso para todos
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
        }
    }

    /**
     * Spawna o boss agendado.
     */
    private void spawnScheduledBoss(ScheduledEvent event) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            plugin.getLogger().info("[BossScheduleManager] Evento agendado pulado: nenhum jogador online.");
            return;
        }

        plugin.getMessageManager().broadcast("boss_schedule.event_starting", resolveBossDisplayName(event.bossId));

        // Som épico para todos
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_GOAT_HORN_SOUND_0, 1.5f, 0.8f);
        }

        // Usa o sistema de spawn existente (localização aleatória)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Verifica se é sazonal
            String bossId = resolveSeasonalBoss(event.bossId);
            plugin.getBossManager().spawnBoss(bossId, generateBossLocation());
        }, 60L); // 3 segundos de delay para drama
    }

    /**
     * Gera uma localização aleatória para o boss (mesmo algoritmo do BossManager).
     */
    private org.bukkit.Location generateBossLocation() {
        org.bukkit.World world = Bukkit.getWorld("world");
        if (world == null)
            return null;

        org.bukkit.Location spawn = world.getSpawnLocation();
        Random random = new Random();
        int x = spawn.getBlockX() + (random.nextInt(20000) - 10000);
        int z = spawn.getBlockZ() + (random.nextInt(20000) - 10000);
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new org.bukkit.Location(world, x, y, z);
    }

    /**
     * Verifica se há boss sazonal ativo que substitui o boss agendado.
     */
    private String resolveSeasonalBoss(String bossId) {
        if (!plugin.getConfig().getBoolean("boss.seasonal.enabled", false))
            return bossId;

        var seasonalSection = plugin.getConfig().getConfigurationSection("boss.seasonal.events");
        if (seasonalSection == null)
            return bossId;

        LocalDateTime now = LocalDateTime.now();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();

        for (String key : seasonalSection.getKeys(false)) {
            var eventSection = seasonalSection.getConfigurationSection(key);
            if (eventSection == null)
                continue;

            String configBossId = eventSection.getString("boss", "");
            if (!configBossId.equalsIgnoreCase(bossId))
                continue;

            // Verificar se está no período sazonal
            String startStr = eventSection.getString("start", "");
            String endStr = eventSection.getString("end", "");

            if (isInSeasonalPeriod(month, day, startStr, endStr)) {
                return bossId; // Boss correto, mas BossManager aplicará override sazonal
            }
        }

        return bossId;
    }

    /**
     * Verifica se a data atual está no período sazonal (formato MM-DD).
     */
    private boolean isInSeasonalPeriod(int month, int day, String startStr, String endStr) {
        try {
            String[] startParts = startStr.split("-");
            String[] endParts = endStr.split("-");
            int startMonth = Integer.parseInt(startParts[0]);
            int startDay = Integer.parseInt(startParts[1]);
            int endMonth = Integer.parseInt(endParts[0]);
            int endDay = Integer.parseInt(endParts[1]);

            int current = month * 100 + day;
            int start = startMonth * 100 + startDay;
            int end = endMonth * 100 + endDay;

            if (start <= end) {
                return current >= start && current <= end;
            } else {
                // Cruza virada de ano (ex: 12-20 a 01-05)
                return current >= start || current <= end;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve nome de exibição do boss para mensagens.
     */
    private String resolveBossDisplayName(String bossId) {
        return switch (bossId.toLowerCase()) {
            case "rei_gorvax" -> "§6Rei Gorvax";
            case "indrax_abissal", "indrax" -> "§5Indrax Abissal";
            case "vulgathor" -> "§cVulgathor";
            case "xylos" -> "§dXylos Devorador";
            case "skulkor" -> "§7Skulkor";
            case "kaldur" -> "§bKaldur";
            case "zarith" -> "§aZar'ith";
            case "rei_indrax" -> "§4Ruptura Temporal";
            case "halloween_boss" -> "§5Ceifador das Sombras";
            case "natal_boss" -> "§bRei do Gelo Eterno";
            default -> "§eBoss Desconhecido";
        };
    }

    /**
     * Retorna os próximos N eventos agendados, ordenados por horário.
     */
    public List<UpcomingEvent> getNextEvents(int count) {
        if (events.isEmpty())
            return Collections.emptyList();

        LocalDateTime now = LocalDateTime.now();
        List<UpcomingEvent> upcoming = new ArrayList<>();

        for (ScheduledEvent event : events) {
            LocalDateTime next = event.getNextOccurrence(now);
            long minutesUntil = java.time.Duration.between(now, next).toMinutes();
            upcoming.add(new UpcomingEvent(event, next, minutesUntil));
        }

        upcoming.sort(Comparator.comparingLong(e -> e.minutesUntil));
        return upcoming.subList(0, Math.min(count, upcoming.size()));
    }

    /**
     * Limpa entradas antigas de avisos e fires.
     */
    private void cleanupOldEntries() {
        // Limpa a cada hora (a set pode crescer ao longo da semana)
        if (sentWarnings.size() > 500) {
            sentWarnings.clear();
        }
        if (firedEvents.size() > 100) {
            firedEvents.clear();
        }
    }

    /**
     * Recarrega a configuração de agendamento.
     */
    public void reload() {
        loadConfig();
        plugin.getLogger().info("[BossScheduleManager] Configuração recarregada: " + events.size() + " eventos.");
    }

    /**
     * Para a task de verificação (usado no shutdown).
     */
    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
        }
    }

    /**
     * Verifica se o sistema de agendamento está ativo.
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("boss.schedule.enabled", false) && !events.isEmpty();
    }

    // ========== Classes internas ==========

    /**
     * Representa um evento agendado na configuração.
     */
    public static class ScheduledEvent {
        public final DayOfWeek day;
        public final LocalTime time;
        public final String bossId;

        public ScheduledEvent(DayOfWeek day, LocalTime time, String bossId) {
            this.day = day;
            this.time = time;
            this.bossId = bossId;
        }

        /**
         * Calcula a próxima ocorrência deste evento a partir de 'now'.
         */
        public LocalDateTime getNextOccurrence(LocalDateTime now) {
            LocalDateTime candidate = now.toLocalDate()
                    .with(TemporalAdjusters.nextOrSame(day))
                    .atTime(time);

            // Se o horário já passou hoje, pegar da próxima semana
            if (candidate.isBefore(now) || candidate.isEqual(now)) {
                candidate = now.toLocalDate()
                        .with(TemporalAdjusters.next(day))
                        .atTime(time);
            }

            return candidate;
        }

        public String hashKey() {
            return day.name() + "_" + time.getHour() + "_" + time.getMinute() + "_" + bossId;
        }
    }

    /**
     * Representa um evento futuro calculado (para exibição).
     */
    public static class UpcomingEvent {
        public final ScheduledEvent event;
        public final LocalDateTime dateTime;
        public final long minutesUntil;

        public UpcomingEvent(ScheduledEvent event, LocalDateTime dateTime, long minutesUntil) {
            this.event = event;
            this.dateTime = dateTime;
            this.minutesUntil = minutesUntil;
        }

        /**
         * Formata o tempo restante em formato legível.
         */
        public String formatTimeRemaining() {
            long hours = minutesUntil / 60;
            long mins = minutesUntil % 60;
            if (hours > 24) {
                long days = hours / 24;
                hours = hours % 24;
                return days + "d " + hours + "h " + mins + "m";
            } else if (hours > 0) {
                return hours + "h " + mins + "m";
            } else {
                return mins + "m";
            }
        }
    }
}
