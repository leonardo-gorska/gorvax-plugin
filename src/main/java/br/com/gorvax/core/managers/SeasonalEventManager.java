package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * B17 — Gerenciador de Eventos Sazonais Expandidos.
 * Carrega eventos temáticos mensais de seasonal_events.yml,
 * detecta automaticamente qual evento está ativo pela data,
 * e fornece multiplicadores de XP/loot/dinheiro.
 */
public class SeasonalEventManager {

    private final GorvaxCore plugin;
    private File configFile;
    private FileConfiguration config;

    // Todos os eventos configurados
    private final Map<String, SeasonalEvent> events = new LinkedHashMap<>();

    // Evento ativo atualmente (null se nenhum)
    private SeasonalEvent activeEvent = null;

    // Evento forçado manualmente por admin (null = automático)
    private SeasonalEvent forcedEvent = null;

    // Task periódica de verificação
    private BukkitTask checkTask;

    public SeasonalEventManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        detectActiveEvent();
        startPeriodicCheck();
    }

    // ===== Configuração =====

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "seasonal_events.yml");
        if (!configFile.exists()) {
            plugin.saveResource("seasonal_events.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        events.clear();

        ConfigurationSection eventsSec = config.getConfigurationSection("events");
        if (eventsSec == null) {
            plugin.getLogger().warning("[B17] Nenhum evento sazonal configurado em seasonal_events.yml.");
            return;
        }

        for (String id : eventsSec.getKeys(false)) {
            ConfigurationSection sec = eventsSec.getConfigurationSection(id);
            if (sec == null)
                continue;

            String name = sec.getString("name", "§eEvento " + id);
            String description = sec.getString("description", "");
            int month = sec.getInt("month", 1);
            int startDay = sec.getInt("start_day", 1);
            int endDay = sec.getInt("end_day", 28);

            String bossId = sec.getString("boss_id", null);
            double xpMultiplier = sec.getDouble("xp_multiplier", 1.0);
            double lootMultiplier = sec.getDouble("loot_multiplier", 1.0);
            double moneyMultiplier = sec.getDouble("money_multiplier", 1.0);

            List<String> achievementIds = sec.getStringList("achievement_ids");

            String broadcastStart = sec.getString("broadcast_start",
                    "§6§l⚡ EVENTO SAZONAL §8» §e" + name + " §fcomeçou!");
            String broadcastEnd = sec.getString("broadcast_end",
                    "§6§l⚡ EVENTO SAZONAL §8» §e" + name + " §fterminou!");

            Material icon = Material.NETHER_STAR;
            try {
                icon = Material.valueOf(sec.getString("icon", "NETHER_STAR").toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }

            events.put(id, new SeasonalEvent(
                    id, name, description, month, startDay, endDay,
                    bossId, xpMultiplier, lootMultiplier, moneyMultiplier,
                    achievementIds, broadcastStart, broadcastEnd, icon));
        }

        plugin.getLogger().info("[B17] Eventos sazonais carregados: " + events.size());
    }

    // ===== Detecção de Evento Ativo =====

    /**
     * Detecta qual evento deve estar ativo com base na data atual.
     * Se um evento foi forçado manualmente, ele tem prioridade.
     */
    private void detectActiveEvent() {
        if (forcedEvent != null) {
            if (activeEvent != forcedEvent) {
                setActiveEvent(forcedEvent);
            }
            return;
        }

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();

        SeasonalEvent matchedEvent = null;
        for (SeasonalEvent event : events.values()) {
            if (event.month() == currentMonth
                    && currentDay >= event.startDay()
                    && currentDay <= event.endDay()) {
                matchedEvent = event;
                break;
            }
        }

        if (matchedEvent != null && activeEvent == null) {
            // Novo evento começou
            setActiveEvent(matchedEvent);
        } else if (matchedEvent == null && activeEvent != null) {
            // Evento acabou
            endActiveEvent();
        } else if (matchedEvent != null && !matchedEvent.id().equals(activeEvent.id())) {
            // Mudou de evento (transição)
            endActiveEvent();
            setActiveEvent(matchedEvent);
        }
    }

    private void setActiveEvent(SeasonalEvent event) {
        this.activeEvent = event;
        plugin.getLogger().info("[B17] Evento sazonal ativado: " + event.name());

        // Broadcast de início para todos os jogadores online
        String startMsg = event.broadcastStart();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(startMsg);
        }

        // Log Discord (sem método genérico no DiscordManager)
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
            plugin.getLogger().info("[B17][Discord] Evento sazonal iniciado: " + stripColors(event.name()));
        }
    }

    private void endActiveEvent() {
        if (activeEvent == null)
            return;

        String endMsg = activeEvent.broadcastEnd();
        plugin.getLogger().info("[B17] Evento sazonal encerrado: " + activeEvent.name());

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(endMsg);
        }

        // Log Discord (sem método genérico no DiscordManager)
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
            plugin.getLogger().info("[B17][Discord] Evento sazonal encerrado: " + stripColors(activeEvent.name()));
        }

        this.activeEvent = null;
    }

    // ===== Task Periódica =====

    private void startPeriodicCheck() {
        // Verifica a cada 60 segundos (1200 ticks)
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::detectActiveEvent, 1200L, 1200L);
    }

    // ===== API Pública =====

    /**
     * Retorna o evento ativo atual, ou null se nenhum.
     */
    public SeasonalEvent getActiveEvent() {
        return activeEvent;
    }

    /**
     * Verifica se há um evento ativo.
     */
    public boolean isEventActive() {
        return activeEvent != null;
    }

    /**
     * Retorna o multiplicador de XP do evento ativo (1.0 se nenhum).
     */
    public double getXpMultiplier() {
        return activeEvent != null ? activeEvent.xpMultiplier() : 1.0;
    }

    /**
     * Retorna o multiplicador de loot do evento ativo (1.0 se nenhum).
     */
    public double getLootMultiplier() {
        return activeEvent != null ? activeEvent.lootMultiplier() : 1.0;
    }

    /**
     * Retorna o multiplicador de dinheiro do evento ativo (1.0 se nenhum).
     */
    public double getMoneyMultiplier() {
        return activeEvent != null ? activeEvent.moneyMultiplier() : 1.0;
    }

    /**
     * Retorna os dias restantes do evento ativo, ou 0.
     */
    public int getDaysRemaining() {
        if (activeEvent == null)
            return 0;
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();

        if (currentMonth != activeEvent.month())
            return 0;

        // Calcula data final do evento no ano atual
        LocalDate endDate;
        try {
            endDate = LocalDate.of(today.getYear(), activeEvent.month(), activeEvent.endDay());
        } catch (Exception e) {
            // Dia inválido para o mês (ex: 31 de fev)
            endDate = today.withMonth(activeEvent.month()).withDayOfMonth(
                    today.withMonth(activeEvent.month()).lengthOfMonth());
        }

        long remaining = ChronoUnit.DAYS.between(today, endDate);
        return Math.max(0, (int) remaining);
    }

    /**
     * Retorna todos os eventos configurados.
     */
    public Map<String, SeasonalEvent> getEvents() {
        return Collections.unmodifiableMap(events);
    }

    /**
     * Retorna um evento por ID.
     */
    public SeasonalEvent getEvent(String id) {
        return events.get(id);
    }

    /**
     * Força início de um evento (admin).
     */
    public boolean forceStartEvent(String eventId) {
        SeasonalEvent event = events.get(eventId);
        if (event == null)
            return false;

        this.forcedEvent = event;
        if (activeEvent != null && !activeEvent.id().equals(eventId)) {
            endActiveEvent();
        }
        setActiveEvent(event);
        return true;
    }

    /**
     * Para o evento ativo (admin).
     */
    public void forceStopEvent() {
        this.forcedEvent = null;
        endActiveEvent();
    }

    /**
     * Recarrega a configuração.
     */
    public void reload() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        loadConfig();
        // Resetar evento forçado no reload
        forcedEvent = null;
        activeEvent = null;
        detectActiveEvent();
        startPeriodicCheck();
    }

    /**
     * Cleanup ao desabilitar o plugin.
     */
    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
        }
    }

    // ===== Utilitários =====

    private String stripColors(String text) {
        if (text == null)
            return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    /**
     * Retorna o nome do mês em português.
     */
    public static String getMonthName(int month) {
        return switch (month) {
            case 1 -> "Janeiro";
            case 2 -> "Fevereiro";
            case 3 -> "Março";
            case 4 -> "Abril";
            case 5 -> "Maio";
            case 6 -> "Junho";
            case 7 -> "Julho";
            case 8 -> "Agosto";
            case 9 -> "Setembro";
            case 10 -> "Outubro";
            case 11 -> "Novembro";
            case 12 -> "Dezembro";
            default -> "Desconhecido";
        };
    }

    // ===== Record =====

    public record SeasonalEvent(
            String id,
            String name,
            String description,
            int month,
            int startDay,
            int endDay,
            String bossId,
            double xpMultiplier,
            double lootMultiplier,
            double moneyMultiplier,
            List<String> achievementIds,
            String broadcastStart,
            String broadcastEnd,
            Material icon) {
    }
}
