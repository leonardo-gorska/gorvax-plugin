package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B3 — Gerencia o ciclo de vida dos duelos PvP 1v1 com aposta.
 * Responsável por desafios, aceite/recusa, countdown, duelo ativo e resolução.
 */
public class DuelManager {

    private final GorvaxCore plugin;
    private final MessageManager msg;

    // Desafios pendentes: chave = UUID do desafiado
    private final ConcurrentHashMap<UUID, DuelChallenge> pendingChallenges = new ConcurrentHashMap<>();
    // Duelos ativos: chave = UUID de cada participante (ambos mapeiam pro mesmo
    // ActiveDuel)
    private final ConcurrentHashMap<UUID, ActiveDuel> activeDuels = new ConcurrentHashMap<>();

    // Configurações
    private boolean enabled;
    private int challengeExpire;
    private int countdown;
    private int maxDuration;
    private int maxDistance;
    private double minBet;
    private double maxBet;
    private double taxPercent;
    private boolean keepInventory;

    private BukkitTask expiryTask;

    public DuelManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
        loadConfig();
        startExpiryTask();
    }

    /**
     * Carrega configurações do config.yml.
     */
    public void loadConfig() {
        var config = plugin.getConfig();
        this.enabled = config.getBoolean("duel.enabled", true);
        this.challengeExpire = config.getInt("duel.challenge_expire", 30);
        this.countdown = config.getInt("duel.countdown", 5);
        this.maxDuration = config.getInt("duel.max_duration", 300);
        this.maxDistance = config.getInt("duel.max_distance", 50);
        this.minBet = config.getDouble("duel.min_bet", 100.0);
        this.maxBet = config.getDouble("duel.max_bet", 100000.0);
        this.taxPercent = config.getDouble("duel.tax_percent", 5.0);
        this.keepInventory = config.getBoolean("duel.keep_inventory", true);
    }

    /**
     * Task que roda a cada segundo para limpar desafios expirados.
     */
    private void startExpiryTask() {
        this.expiryTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                pendingChallenges.entrySet().removeIf(entry -> {
                    DuelChallenge challenge = entry.getValue();
                    if (now >= challenge.expiresAt) {
                        Player challenger = Bukkit.getPlayer(challenge.challenger);
                        Player challenged = Bukkit.getPlayer(challenge.challenged);
                        if (challenger != null) {
                            msg.send(challenger, "duel.challenge_expired");
                        }
                        if (challenged != null) {
                            msg.send(challenged, "duel.challenge_expired");
                        }
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 20L, 20L); // A cada 1 segundo
    }

    // ======================== DESAFIO ========================

    /**
     * Desafia um jogador para um duelo 1v1.
     */
    public void challenge(Player challenger, Player target, double bet) {
        if (!enabled) {
            msg.send(challenger, "duel.disabled");
            return;
        }

        UUID challengerUUID = challenger.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        // Validações
        if (challengerUUID.equals(targetUUID)) {
            msg.send(challenger, "duel.self_challenge");
            return;
        }

        if (isInDuel(challengerUUID)) {
            msg.send(challenger, "duel.already_in_duel");
            return;
        }

        if (isInDuel(targetUUID)) {
            msg.send(challenger, "duel.target_in_duel");
            return;
        }

        if (pendingChallenges.containsKey(targetUUID)) {
            msg.send(challenger, "duel.target_in_duel");
            return;
        }

        // Check combat tag
        CombatManager combatManager = plugin.getCombatManager();
        if (combatManager != null) {
            if (combatManager.isInCombat(challengerUUID)) {
                msg.send(challenger, "duel.challenger_in_combat");
                return;
            }
            if (combatManager.isInCombat(targetUUID)) {
                msg.send(challenger, "duel.target_in_combat");
                return;
            }
        }

        // Check distance
        if (!challenger.getWorld().equals(target.getWorld()) ||
                challenger.getLocation().distance(target.getLocation()) > maxDistance) {
            msg.send(challenger, "duel.too_far", maxDistance);
            return;
        }

        // Check economy
        Economy economy = GorvaxCore.getEconomy();
        if (bet > 0) {
            if (bet < minBet) {
                msg.send(challenger, "duel.bet_too_low", String.format("%.2f", minBet));
                return;
            }
            if (bet > maxBet) {
                msg.send(challenger, "duel.bet_too_high", String.format("%.2f", maxBet));
                return;
            }
            if (economy == null) {
                msg.send(challenger, "duel.disabled");
                return;
            }
            if (!economy.has(challenger, bet)) {
                msg.send(challenger, "duel.insufficient_funds", String.format("%.2f", bet));
                return;
            }
            if (!economy.has(target, bet)) {
                msg.send(challenger, "duel.target_insufficient_funds", String.format("%.2f", bet));
                return;
            }
        }

        // Criar desafio
        DuelChallenge challenge = new DuelChallenge(challengerUUID, targetUUID, bet,
                System.currentTimeMillis() + (challengeExpire * 1000L));
        pendingChallenges.put(targetUUID, challenge);

        // Notificar challenger
        msg.send(challenger, "duel.challenge_sent", target.getName(),
                bet > 0 ? String.format("%.2f", bet) : "0");

        // Notificar target
        if (bet > 0) {
            msg.send(target, "duel.challenge_received_bet", challenger.getName(),
                    String.format("%.2f", bet));
        } else {
            msg.send(target, "duel.challenge_received", challenger.getName());
        }

        // Chat clicável para Java ou ModalForm para Bedrock
        if (InputManager.isBedrockPlayer(target)) {
            BedrockFormManager formManager = plugin.getBedrockFormManager();
            if (formManager != null && formManager.isAvailable()) {
                String content = bet > 0
                        ? challenger.getName() + " te desafiou para um duelo! Aposta: $" + String.format("%.2f", bet)
                        : challenger.getName() + " te desafiou para um duelo!";
                formManager.sendModalForm(target, "⚔ Duelo", content,
                        "Aceitar", "Recusar", accepted -> {
                            if (accepted != null && accepted) {
                                accept(target);
                            } else {
                                refuse(target);
                            }
                        });
            }
        } else {
            // Chat clicável para Java
            sendClickableChallenge(target, challenger.getName());
        }

        // Som
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    /**
     * Envia mensagem clicável [ACEITAR] [RECUSAR] no chat via componentes JSON.
     */
    private void sendClickableChallenge(Player target, String challengerName) {
        net.kyori.adventure.text.Component acceptBtn = net.kyori.adventure.text.Component.text(" [ACEITAR] ")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/duel aceitar"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        net.kyori.adventure.text.Component.text("Clique para aceitar o duelo")));

        net.kyori.adventure.text.Component refuseBtn = net.kyori.adventure.text.Component.text(" [RECUSAR] ")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/duel recusar"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        net.kyori.adventure.text.Component.text("Clique para recusar o duelo")));

        net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text("§e§l⚔ DUELO §8» §7")
                .append(acceptBtn)
                .append(refuseBtn);

        target.sendMessage(message);
    }

    // ======================== ACEITE / RECUSA ========================

    /**
     * Aceita um desafio pendente.
     */
    public void accept(Player target) {
        UUID targetUUID = target.getUniqueId();
        DuelChallenge challenge = pendingChallenges.remove(targetUUID);

        if (challenge == null) {
            msg.send(target, "duel.no_pending");
            return;
        }

        Player challenger = Bukkit.getPlayer(challenge.challenger);
        if (challenger == null || !challenger.isOnline()) {
            msg.send(target, "duel.player_offline");
            return;
        }

        // Debitar aposta de ambos
        Economy economy = GorvaxCore.getEconomy();
        if (challenge.betAmount > 0 && economy != null) {
            if (!economy.has(challenger, challenge.betAmount)) {
                msg.send(target, "duel.challenger_insufficient");
                msg.send(challenger, "duel.insufficient_funds", String.format("%.2f", challenge.betAmount));
                return;
            }
            if (!economy.has(target, challenge.betAmount)) {
                msg.send(target, "duel.insufficient_funds", String.format("%.2f", challenge.betAmount));
                msg.send(challenger, "duel.target_insufficient_funds", String.format("%.2f", challenge.betAmount));
                return;
            }
            economy.withdrawPlayer(challenger, challenge.betAmount);
            economy.withdrawPlayer(target, challenge.betAmount);
        }

        // Criar duelo ativo
        ActiveDuel duel = new ActiveDuel(challenge.challenger, challenge.challenged, challenge.betAmount);
        activeDuels.put(challenge.challenger, duel);
        activeDuels.put(challenge.challenged, duel);

        // Iniciar countdown
        startCountdown(duel, challenger, target);
    }

    /**
     * Recusa um desafio pendente.
     */
    public void refuse(Player target) {
        UUID targetUUID = target.getUniqueId();
        DuelChallenge challenge = pendingChallenges.remove(targetUUID);

        if (challenge == null) {
            msg.send(target, "duel.no_pending");
            return;
        }

        Player challenger = Bukkit.getPlayer(challenge.challenger);
        msg.send(target, "duel.challenge_refused");
        if (challenger != null) {
            msg.send(challenger, "duel.challenge_refused_notify", target.getName());
        }
    }

    // ======================== COUNTDOWN ========================

    /**
     * Inicia contagem regressiva de 5 segundos antes do duelo.
     */
    private void startCountdown(ActiveDuel duel, Player p1, Player p2) {
        duel.taskId = new BukkitRunnable() {
            int count = countdown;

            @Override
            public void run() {
                if (count > 0) {
                    // Título com contagem
                    String color = count <= 2 ? "§c" : count <= 3 ? "§e" : "§a";
                    Title title = Title.title(
                            LegacyComponentSerializer.legacySection().deserialize(color + "§l" + count),
                            LegacyComponentSerializer.legacySection().deserialize("§7Prepare-se!"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1250), Duration.ofMillis(250)));
                    p1.showTitle(title);
                    p2.showTitle(title);
                    p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    count--;
                } else {
                    // LUTE!
                    duel.started = true;
                    duel.startedAt = System.currentTimeMillis();
                    Title fightTitle = Title.title(
                            LegacyComponentSerializer.legacySection().deserialize("§c§l⚔ LUTE!"),
                            LegacyComponentSerializer.legacySection().deserialize("§7Que o melhor vença!"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(2000), Duration.ofMillis(500)));
                    p1.showTitle(fightTitle);
                    p2.showTitle(fightTitle);
                    p1.playSound(p1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
                    p2.playSound(p2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);

                    // Agendar timeout
                    scheduleTimeout(duel);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    /**
     * Agenda timeout do duelo (5 minutos = empate).
     */
    private void scheduleTimeout(ActiveDuel duel) {
        duel.timeoutTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeDuels.containsValue(duel) && duel.started) {
                    endDuelDraw(duel);
                }
            }
        }.runTaskLater(plugin, maxDuration * 20L).getTaskId();
    }

    // ======================== RESOLUÇÃO ========================

    /**
     * Finaliza o duelo com um vencedor.
     */
    public void endDuel(ActiveDuel duel, UUID winnerUUID) {
        if (!activeDuels.containsValue(duel))
            return; // Já finalizado

        UUID loserUUID = winnerUUID.equals(duel.player1) ? duel.player2 : duel.player1;

        // Remover do mapa
        activeDuels.remove(duel.player1);
        activeDuels.remove(duel.player2);

        // Cancelar tasks
        cancelDuelTasks(duel);

        Player winner = Bukkit.getPlayer(winnerUUID);
        Player loser = Bukkit.getPlayer(loserUUID);

        // Distribuir prêmio
        Economy economy = GorvaxCore.getEconomy();
        if (duel.betAmount > 0 && economy != null) {
            double totalPot = duel.betAmount * 2;
            double tax = totalPot * (taxPercent / 100.0);
            double prize = totalPot - tax;

            if (winner != null) {
                economy.depositPlayer(winner, prize);
                msg.send(winner, "duel.duel_win", loser != null ? loser.getName() : "???",
                        String.format("%.2f", prize));
            }
            if (loser != null) {
                msg.send(loser, "duel.duel_lose", winner != null ? winner.getName() : "???",
                        String.format("%.2f", duel.betAmount));
            }
        } else {
            if (winner != null) {
                msg.send(winner, "duel.duel_win_no_bet", loser != null ? loser.getName() : "???");
            }
            if (loser != null) {
                msg.send(loser, "duel.duel_lose_no_bet", winner != null ? winner.getName() : "???");
            }
        }

        // Broadcast
        if (winner != null) {
            msg.broadcast("duel.duel_win_broadcast", winner.getName(),
                    loser != null ? loser.getName() : "???",
                    duel.betAmount > 0 ? "$" + String.format("%.2f", duel.betAmount * 2) : "");
        }

        // Sons
        if (winner != null) {
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        if (loser != null) {
            loser.playSound(loser.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        // B19 — Evento customizado: DuelEndEvent
        Bukkit.getPluginManager().callEvent(
                new br.com.gorvax.core.events.DuelEndEvent(winnerUUID, loserUUID, duel.betAmount, false));

        // Atualizar stats
        updateDuelStats(winnerUUID, loserUUID, duel.betAmount);
    }

    /**
     * Finaliza o duelo em empate (timeout).
     */
    public void endDuelDraw(ActiveDuel duel) {
        if (!activeDuels.containsValue(duel))
            return;

        activeDuels.remove(duel.player1);
        activeDuels.remove(duel.player2);
        cancelDuelTasks(duel);

        Player p1 = Bukkit.getPlayer(duel.player1);
        Player p2 = Bukkit.getPlayer(duel.player2);

        // Devolver apostas
        Economy economy = GorvaxCore.getEconomy();
        if (duel.betAmount > 0 && economy != null) {
            if (p1 != null)
                economy.depositPlayer(p1, duel.betAmount);
            if (p2 != null)
                economy.depositPlayer(p2, duel.betAmount);
        }

        if (p1 != null)
            msg.send(p1, "duel.duel_draw");
        if (p2 != null)
            msg.send(p2, "duel.duel_draw");

        // B19 — Evento customizado: DuelEndEvent (empate)
        Bukkit.getPluginManager().callEvent(
                new br.com.gorvax.core.events.DuelEndEvent(null, null, duel.betAmount, true));
    }

    /**
     * Cancela duelo (disconnect de um participante — o outro vence).
     */
    public void handleDisconnect(UUID disconnectedUUID) {
        ActiveDuel duel = activeDuels.get(disconnectedUUID);
        if (duel == null)
            return;

        UUID winnerUUID = disconnectedUUID.equals(duel.player1) ? duel.player2 : duel.player1;

        Player winner = Bukkit.getPlayer(winnerUUID);
        if (winner != null) {
            msg.send(winner, "duel.duel_disconnect",
                    GorvaxCore.getInstance().getPlayerName(disconnectedUUID));
        }

        endDuel(duel, winnerUUID);

        // Também limpar desafios pendentes do jogador
        pendingChallenges.remove(disconnectedUUID);
        pendingChallenges.values().removeIf(c -> c.challenger.equals(disconnectedUUID));
    }

    /**
     * Atualiza estatísticas de duelo no PlayerData.
     */
    private void updateDuelStats(UUID winnerUUID, UUID loserUUID, double betAmount) {
        PlayerDataManager pdm = plugin.getPlayerDataManager();
        if (pdm == null)
            return;

        PlayerData winnerData = pdm.getData(winnerUUID);
        PlayerData loserData = pdm.getData(loserUUID);

        if (winnerData != null) {
            winnerData.incrementDuelWins();
            if (betAmount > 0) {
                double totalPot = betAmount * 2;
                double tax = totalPot * (taxPercent / 100.0);
                winnerData.addDuelMoneyWon(totalPot - tax);
            }
        }
        if (loserData != null) {
            loserData.incrementDuelLosses();
        }
    }

    /**
     * Cancela tasks de scheduler dum duelo.
     */
    private void cancelDuelTasks(ActiveDuel duel) {
        if (duel.taskId != -1) {
            Bukkit.getScheduler().cancelTask(duel.taskId);
            duel.taskId = -1;
        }
        if (duel.timeoutTaskId != -1) {
            Bukkit.getScheduler().cancelTask(duel.timeoutTaskId);
            duel.timeoutTaskId = -1;
        }
    }

    // ======================== QUERIES ========================

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public ActiveDuel getActiveDuel(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public boolean hasPendingChallenge(UUID targetUUID) {
        return pendingChallenges.containsKey(targetUUID);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    // ======================== CLEANUP ========================

    /**
     * Chamado no onDisable — cancela tudo e devolve apostas.
     */
    public void cleanup() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }

        // Devolver apostas de desafios pendentes (não necessário, nada foi debitado)
        pendingChallenges.clear();

        // Devolver apostas de duelos ativos
        Economy economy = GorvaxCore.getEconomy();
        activeDuels.values().stream().distinct().forEach(duel -> {
            cancelDuelTasks(duel);
            if (duel.betAmount > 0 && economy != null) {
                Player p1 = Bukkit.getPlayer(duel.player1);
                Player p2 = Bukkit.getPlayer(duel.player2);
                if (p1 != null)
                    economy.depositPlayer(p1, duel.betAmount);
                if (p2 != null)
                    economy.depositPlayer(p2, duel.betAmount);
            }
        });
        activeDuels.clear();
    }

    // ======================== INNER CLASSES ========================

    /**
     * Desafio pendente aguardando aceite.
     */
    public static class DuelChallenge {
        public final UUID challenger;
        public final UUID challenged;
        public final double betAmount;
        public final long expiresAt;

        public DuelChallenge(UUID challenger, UUID challenged, double betAmount, long expiresAt) {
            this.challenger = challenger;
            this.challenged = challenged;
            this.betAmount = betAmount;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Duelo ativo em andamento.
     */
    public static class ActiveDuel {
        public final UUID player1;
        public final UUID player2;
        public final double betAmount;
        public int taskId = -1;
        public int timeoutTaskId = -1;
        public long startedAt = 0;
        public boolean started = false;

        public ActiveDuel(UUID player1, UUID player2, double betAmount) {
            this.player1 = player1;
            this.player2 = player2;
            this.betAmount = betAmount;
        }

        public boolean isParticipant(UUID uuid) {
            return player1.equals(uuid) || player2.equals(uuid);
        }

        public UUID getOpponent(UUID uuid) {
            return player1.equals(uuid) ? player2 : player1;
        }
    }
}
