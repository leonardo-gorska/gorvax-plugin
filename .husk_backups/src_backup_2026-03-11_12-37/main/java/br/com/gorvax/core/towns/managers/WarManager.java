package br.com.gorvax.core.towns.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.Relation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B15 — Gerencia o ciclo de vida completo de guerras entre reinos.
 * Fases: PREPARAÇÃO → ATIVA → ENCERRADA.
 * Persistência delegada ao KingdomManager (towns.yml, seção "guerras").
 */
public class WarManager {

    private final GorvaxCore plugin;

    // Cache em memória das guerras ativas (warId -> War)
    private final Map<String, War> activeWars = new ConcurrentHashMap<>();

    // Contadores para gerar IDs únicos
    private int warCounter;

    public WarManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadWars();
        startTimerTask();
    }

    // ========================================================================
    // RECORDS / ENUMS
    // ========================================================================

    public enum WarState {
        PREPARATION, ACTIVE, ENDED
    }

    /**
     * Representa uma guerra entre dois reinos.
     * Mutável para atualizar pontos e estado durante a guerra.
     */
    public static class War {
        private final String id;
        private final String attackerId;
        private final String defenderId;
        private final long declaredAt;
        private final long startsAt;
        private final long endsAt;
        private int attackerPoints;
        private int defenderPoints;
        private WarState state;

        public War(String id, String attackerId, String defenderId,
                long declaredAt, long startsAt, long endsAt,
                int attackerPoints, int defenderPoints, WarState state) {
            this.id = id;
            this.attackerId = attackerId;
            this.defenderId = defenderId;
            this.declaredAt = declaredAt;
            this.startsAt = startsAt;
            this.endsAt = endsAt;
            this.attackerPoints = attackerPoints;
            this.defenderPoints = defenderPoints;
            this.state = state;
        }

        public String getId() {
            return id;
        }

        public String getAttackerId() {
            return attackerId;
        }

        public String getDefenderId() {
            return defenderId;
        }

        public long getDeclaredAt() {
            return declaredAt;
        }

        public long getStartsAt() {
            return startsAt;
        }

        public long getEndsAt() {
            return endsAt;
        }

        public int getAttackerPoints() {
            return attackerPoints;
        }

        public int getDefenderPoints() {
            return defenderPoints;
        }

        public WarState getState() {
            return state;
        }

        public void setState(WarState state) {
            this.state = state;
        }

        public void addAttackerPoints(int pts) {
            this.attackerPoints += pts;
        }

        public void addDefenderPoints(int pts) {
            this.defenderPoints += pts;
        }

        /** Retorna o ID do reino oponente dado um dos lados */
        public String getOpponent(String kingdomId) {
            return kingdomId.equals(attackerId) ? defenderId : attackerId;
        }

        /** Verifica se um reino participa desta guerra */
        public boolean involves(String kingdomId) {
            return attackerId.equals(kingdomId) || defenderId.equals(kingdomId);
        }
    }

    // ========================================================================
    // DECLARAÇÃO DE GUERRA
    // ========================================================================

    /**
     * Declara guerra entre dois reinos.
     * Retorna null em caso de sucesso, ou uma chave de mensagem de erro.
     */
    public String declareWar(String attackerId, String defenderId) {
        if (!plugin.getConfig().getBoolean("war.enabled", true)) {
            return "war.error_disabled";
        }

        KingdomManager km = plugin.getKingdomManager();

        // Validação: mesma guerra já ativa
        if (areAtWar(attackerId, defenderId)) {
            return "war.error_already_at_war";
        }

        // Validação: aliados não podem guerrear
        if (km.areAllied(attackerId, defenderId)) {
            return "war.error_allied";
        }

        // Validação: nível mínimo
        int minLevel = plugin.getConfig().getInt("war.min_kingdom_level", 3);
        if (km.getKingdomLevel(attackerId) < minLevel) {
            return "war.error_min_level";
        }

        // Validação: membros mínimos
        int minMembers = plugin.getConfig().getInt("war.min_members", 3);
        if (km.getSuditosCount(attackerId) < minMembers) {
            return "war.error_min_members";
        }

        // Validação: cooldown
        long cooldownMs = plugin.getConfig().getLong("war.cooldown_days", 30) * 86400000L;
        long lastWarEnd = getLastWarEnd(attackerId, defenderId);
        if (lastWarEnd > 0 && System.currentTimeMillis() - lastWarEnd < cooldownMs) {
            return "war.error_cooldown";
        }

        // Validação: custo
        double cost = plugin.getConfig().getDouble("war.declaration_cost", 10000.0);
        if (km.getBankBalance(attackerId) < cost) {
            return "war.error_insufficient_funds";
        }

        // B19 — Evento customizado: KingdomWarDeclareEvent (cancelável)
        br.com.gorvax.core.events.KingdomWarDeclareEvent warEvent = new br.com.gorvax.core.events.KingdomWarDeclareEvent(
                attackerId, defenderId);
        org.bukkit.Bukkit.getPluginManager().callEvent(warEvent);
        if (warEvent.isCancelled()) {
            return "war.error_disabled";
        }

        // Cobrar custo
        km.withdrawFromBank(attackerId, cost);

        // Criar guerra
        long now = System.currentTimeMillis();
        long prepHours = plugin.getConfig().getLong("war.preparation_hours", 24);
        long durationDays = plugin.getConfig().getLong("war.max_duration_days", 7);
        long startsAt = now + (prepHours * 3600000L);
        long endsAt = startsAt + (durationDays * 86400000L);

        warCounter++;
        String warId = "war_" + warCounter;

        War war = new War(warId, attackerId, defenderId, now, startsAt, endsAt,
                0, 0, WarState.PREPARATION);

        activeWars.put(warId, war);

        // Setar relação como WAR para ambos os reinos
        km.setRelation(attackerId, defenderId, Relation.WAR);

        // Persistir
        saveWar(war);

        return null; // sucesso
    }

    // ========================================================================
    // COMBATE — PONTUAÇÃO
    // ========================================================================

    /**
     * Registra um kill na guerra entre dois reinos.
     * 
     * @param killerKingdomId Reino do killer
     * @param victimKingdomId Reino da vítima
     */
    public void addKill(String killerKingdomId, String victimKingdomId) {
        War war = getWarBetween(killerKingdomId, victimKingdomId);
        if (war == null || war.getState() != WarState.ACTIVE)
            return;

        int pointsPerKill = plugin.getConfig().getInt("war.points_per_kill", 1);
        int pointsPerDeath = plugin.getConfig().getInt("war.points_per_death", -1);

        if (killerKingdomId.equals(war.getAttackerId())) {
            war.addAttackerPoints(pointsPerKill);
            war.addDefenderPoints(pointsPerDeath);
        } else {
            war.addDefenderPoints(pointsPerKill);
            war.addAttackerPoints(pointsPerDeath);
        }

        saveWar(war);
    }

    // ========================================================================
    // RENDIÇÃO
    // ========================================================================

    /**
     * Processa a rendição de um reino.
     * Retorna null em caso de sucesso, ou uma chave de mensagem de erro.
     */
    public String surrender(String loserKingdomId) {
        War war = getWarForKingdom(loserKingdomId);
        if (war == null || war.getState() == WarState.ENDED) {
            return "war.error_not_at_war";
        }

        String winnerId = war.getOpponent(loserKingdomId);
        endWar(war, winnerId, loserKingdomId, true);
        return null;
    }

    // ========================================================================
    // FIM DA GUERRA E ESPÓLIOS
    // ========================================================================

    /**
     * Finaliza uma guerra, aplica espólios e limpa estado.
     */
    private void endWar(War war, String winnerId, String loserId, boolean isSurrender) {
        war.setState(WarState.ENDED);

        KingdomManager km = plugin.getKingdomManager();
        var msg = plugin.getMessageManager();

        // Calcular espólios
        double spoilPercent = plugin.getConfig().getDouble("war.spoils_bank_percent", 25.0);
        if (isSurrender) {
            spoilPercent += plugin.getConfig().getDouble("war.surrender_penalty_percent", 10.0);
        }
        double loserBalance = km.getBankBalance(loserId);
        double spoilAmount = loserBalance * (spoilPercent / 100.0);

        // Transferir fundos
        if (spoilAmount > 0 && km.withdrawFromBank(loserId, spoilAmount)) {
            km.depositToBank(winnerId, spoilAmount);
        }

        // Aplicar debuff ao perdedor
        int debuffDays = plugin.getConfig().getInt("war.loser_debuff_days", 3);
        long debuffUntil = System.currentTimeMillis() + (debuffDays * 86400000L);
        km.getData().set("reino." + loserId + ".war_debuff_until", debuffUntil);

        // Registrar cooldown
        long cooldownMs = plugin.getConfig().getLong("war.cooldown_days", 30) * 86400000L;
        long cooldownUntil = System.currentTimeMillis() + cooldownMs;
        setWarCooldown(war.getAttackerId(), war.getDefenderId(), cooldownUntil);

        // Captura de outpost (opcional)
        if (plugin.getConfig().getBoolean("war.outpost_capture_enabled", true)) {
            captureOutpost(winnerId, loserId);
        }

        // Resetar relação para NEUTRAL
        km.setRelation(war.getAttackerId(), war.getDefenderId(), Relation.NEUTRAL);

        // Broadcast resultado
        String winnerName = km.getNome(winnerId);
        String loserName = km.getNome(loserId);

        if (winnerId != null && loserId != null) {
            int winPoints, losePoints;
            if (winnerId.equals(war.getAttackerId())) {
                winPoints = war.getAttackerPoints();
                losePoints = war.getDefenderPoints();
            } else {
                winPoints = war.getDefenderPoints();
                losePoints = war.getAttackerPoints();
            }

            if (isSurrender) {
                msg.broadcast("war.surrender", loserName, winnerName);
            }
            msg.broadcast("war.war_ended", winnerName, loserName,
                    String.valueOf(winPoints), String.valueOf(losePoints));

            // Notificar membros do vencedor
            notifyKingdomMembers(winnerId, "war.spoils_winner",
                    String.format("%.2f", spoilAmount));

            // Notificar membros do perdedor
            notifyKingdomMembers(loserId, "war.spoils_loser",
                    String.format("%.2f", spoilAmount), String.valueOf(debuffDays));
        }

        // Persistir estado final e remover do cache ativo
        saveWar(war);
        activeWars.remove(war.getId());

        // Audit log
        if (plugin.getAuditManager() != null) {
            plugin.getAuditManager().log(
                    br.com.gorvax.core.managers.AuditManager.AuditAction.KINGDOM_DELETE, // Reusa ação genérica
                    null, "Sistema",
                    "Guerra " + war.getId() + " finalizada. Vencedor: " + winnerName
                            + ", Perdedor: " + loserName + ". Espólios: $" + String.format("%.2f", spoilAmount));
        }

        km.save();
    }

    /**
     * Captura o menor outpost (em área) do perdedor e transfere para o vencedor.
     */
    private void captureOutpost(String winnerId, String loserId) {
        List<Claim> loserOutposts = plugin.getClaimManager().getOutpostsForKingdom(loserId);
        if (loserOutposts.isEmpty())
            return;

        // Encontrar o menor outpost sem subplots
        Claim smallest = null;
        int smallestArea = Integer.MAX_VALUE;

        for (Claim outpost : loserOutposts) {
            if (outpost.getSubPlots() != null && !outpost.getSubPlots().isEmpty()) {
                continue; // Pular outposts com subplots
            }
            int area = (outpost.getMaxX() - outpost.getMinX() + 1) *
                    (outpost.getMaxZ() - outpost.getMinZ() + 1);
            if (area < smallestArea) {
                smallestArea = area;
                smallest = outpost;
            }
        }

        if (smallest == null)
            return;

        // Transferir outpost: mudar o parentKingdomId
        smallest.setParentKingdomId(winnerId);
        plugin.getClaimManager().saveClaims();

        String outpostName = smallest.getName() != null ? smallest.getName() : "Outpost";

        notifyKingdomMembers(winnerId, "war.outpost_captured", outpostName);
        notifyKingdomMembers(loserId, "war.outpost_lost", outpostName);
    }

    // ========================================================================
    // QUERIES
    // ========================================================================

    /** Verifica se dois reinos estão em guerra ativa */
    public boolean areAtWar(String kA, String kB) {
        return getWarBetween(kA, kB) != null;
    }

    /** Verifica se um reino está em qualquer guerra ativa */
    public boolean isAtWar(String kingdomId) {
        for (War war : activeWars.values()) {
            if (war.getState() != WarState.ENDED && war.involves(kingdomId)) {
                return true;
            }
        }
        return false;
    }

    /** Retorna a guerra ativa entre dois reinos, ou null */
    public War getWarBetween(String kA, String kB) {
        for (War war : activeWars.values()) {
            if (war.getState() != WarState.ENDED && war.involves(kA) && war.involves(kB)) {
                return war;
            }
        }
        return null;
    }

    /** Retorna a primeira guerra ativa para um reino */
    public War getWarForKingdom(String kingdomId) {
        for (War war : activeWars.values()) {
            if (war.getState() != WarState.ENDED && war.involves(kingdomId)) {
                return war;
            }
        }
        return null;
    }

    /** Retorna todas as guerras ativas */
    public List<War> getActiveWars() {
        List<War> result = new ArrayList<>();
        for (War war : activeWars.values()) {
            if (war.getState() != WarState.ENDED) {
                result.add(war);
            }
        }
        return result;
    }

    /** Verifica se um reino está sob debuff de guerra */
    public boolean isWarDebuffed(String kingdomId) {
        long debuffUntil = plugin.getKingdomManager().getData()
                .getLong("reino." + kingdomId + ".war_debuff_until", 0);
        return debuffUntil > System.currentTimeMillis();
    }

    /** Retorna o timestamp de fim do debuff, ou 0 */
    public long getWarDebuffEnd(String kingdomId) {
        return plugin.getKingdomManager().getData()
                .getLong("reino." + kingdomId + ".war_debuff_until", 0);
    }

    // ========================================================================
    // TIMER TASK — TRANSIÇÕES DE FASE
    // ========================================================================

    private void startTimerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (War war : new ArrayList<>(activeWars.values())) {
                    if (war.getState() == WarState.ENDED)
                        continue;

                    // PREPARATION → ACTIVE
                    if (war.getState() == WarState.PREPARATION && now >= war.getStartsAt()) {
                        war.setState(WarState.ACTIVE);
                        saveWar(war);

                        KingdomManager km = plugin.getKingdomManager();
                        String attackerName = km.getNome(war.getAttackerId());
                        String defenderName = km.getNome(war.getDefenderId());

                        // Broadcast
                        plugin.getMessageManager().broadcast("war.war_started",
                                attackerName, defenderName);

                        // Title para membros de ambos os reinos
                        notifyKingdomTitle(war.getAttackerId(),
                                "war.war_started_title", "war.war_started_subtitle",
                                attackerName, defenderName);
                        notifyKingdomTitle(war.getDefenderId(),
                                "war.war_started_title", "war.war_started_subtitle",
                                attackerName, defenderName);

                        plugin.getLogger().info("[B15] Guerra " + war.getId()
                                + " ativada: " + attackerName + " vs " + defenderName);
                    }

                    // ACTIVE → ENDED (tempo esgotado)
                    if (war.getState() == WarState.ACTIVE && now >= war.getEndsAt()) {
                        // Determinar vencedor por pontos
                        String winnerId, loserId;
                        if (war.getAttackerPoints() > war.getDefenderPoints()) {
                            winnerId = war.getAttackerId();
                            loserId = war.getDefenderId();
                        } else if (war.getDefenderPoints() > war.getAttackerPoints()) {
                            winnerId = war.getDefenderId();
                            loserId = war.getAttackerId();
                        } else {
                            // Empate — nenhum espólio, ambos voltam a NEUTRAL
                            handleDraw(war);
                            continue;
                        }

                        endWar(war, winnerId, loserId, false);

                        plugin.getLogger().info("[B15] Guerra " + war.getId()
                                + " encerrada por tempo.");
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // A cada 60 segundos
    }

    /** Trata empate — sem espólios, relação volta a NEUTRAL */
    private void handleDraw(War war) {
        war.setState(WarState.ENDED);

        KingdomManager km = plugin.getKingdomManager();
        km.setRelation(war.getAttackerId(), war.getDefenderId(), Relation.NEUTRAL);

        String attackerName = km.getNome(war.getAttackerId());
        String defenderName = km.getNome(war.getDefenderId());

        plugin.getMessageManager().broadcast("war.war_ended_draw",
                attackerName, defenderName);

        // Registrar cooldown mesmo em empate
        long cooldownMs = plugin.getConfig().getLong("war.cooldown_days", 30) * 86400000L;
        setWarCooldown(war.getAttackerId(), war.getDefenderId(),
                System.currentTimeMillis() + cooldownMs);

        saveWar(war);
        activeWars.remove(war.getId());
        km.save();
    }

    // ========================================================================
    // BROADCAST DE PLACAR PERIÓDICO
    // ========================================================================

    /** Chamado pelo timer task para broadcast do placar */
    public void broadcastScores() {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        for (War war : activeWars.values()) {
            if (war.getState() != WarState.ACTIVE)
                continue;

            String attackerName = km.getNome(war.getAttackerId());
            String defenderName = km.getNome(war.getDefenderId());
            long remaining = war.getEndsAt() - System.currentTimeMillis();
            String timeRemaining = formatDuration(remaining);

            msg.broadcast("war.score_broadcast",
                    attackerName, String.valueOf(war.getAttackerPoints()),
                    defenderName, String.valueOf(war.getDefenderPoints()),
                    timeRemaining);
        }
    }

    // ========================================================================
    // PERSISTÊNCIA (via KingdomManager towns.yml)
    // ========================================================================

    private void loadWars() {
        var data = plugin.getKingdomManager().getData();
        var section = data.getConfigurationSection("guerras");
        if (section == null) {
            warCounter = 0;
            return;
        }

        int maxCounter = 0;
        for (String warId : section.getKeys(false)) {
            String prefix = "guerras." + warId + ".";
            String attackerId = data.getString(prefix + "attacker");
            String defenderId = data.getString(prefix + "defender");
            long declaredAt = data.getLong(prefix + "declared_at");
            long startsAt = data.getLong(prefix + "starts_at");
            long endsAt = data.getLong(prefix + "ends_at");
            int attackerPts = data.getInt(prefix + "attacker_points");
            int defenderPts = data.getInt(prefix + "defender_points");
            String stateStr = data.getString(prefix + "state", "ENDED");

            WarState state;
            try {
                state = WarState.valueOf(stateStr);
            } catch (IllegalArgumentException e) {
                state = WarState.ENDED;
            }

            if (state == WarState.ENDED)
                continue; // Não carregar guerras finalizadas

            War war = new War(warId, attackerId, defenderId, declaredAt, startsAt, endsAt,
                    attackerPts, defenderPts, state);
            activeWars.put(warId, war);

            // Extrair índice numérico para o counter
            try {
                int idx = Integer.parseInt(warId.replace("war_", ""));
                if (idx > maxCounter)
                    maxCounter = idx;
            } catch (NumberFormatException ignored) {
            }
        }

        warCounter = maxCounter;
        plugin.getLogger().info("[B15] Carregadas " + activeWars.size() + " guerras ativas.");
    }

    private void saveWar(War war) {
        var data = plugin.getKingdomManager().getData();
        String prefix = "guerras." + war.getId() + ".";

        data.set(prefix + "attacker", war.getAttackerId());
        data.set(prefix + "defender", war.getDefenderId());
        data.set(prefix + "declared_at", war.getDeclaredAt());
        data.set(prefix + "starts_at", war.getStartsAt());
        data.set(prefix + "ends_at", war.getEndsAt());
        data.set(prefix + "attacker_points", war.getAttackerPoints());
        data.set(prefix + "defender_points", war.getDefenderPoints());
        data.set(prefix + "state", war.getState().name());

        plugin.getKingdomManager().save();
    }

    // ========================================================================
    // COOLDOWN
    // ========================================================================

    private void setWarCooldown(String kA, String kB, long until) {
        var data = plugin.getKingdomManager().getData();
        // Armazenar cooldown em ambas as direções
        String key1 = "war_cooldowns." + kA + "." + kB;
        String key2 = "war_cooldowns." + kB + "." + kA;
        data.set(key1, until);
        data.set(key2, until);
        plugin.getKingdomManager().save();
    }

    private long getLastWarEnd(String kA, String kB) {
        var data = plugin.getKingdomManager().getData();
        return data.getLong("war_cooldowns." + kA + "." + kB, 0);
    }

    /** Retorna dias restantes de cooldown, ou 0 se expirado */
    public long getCooldownDaysRemaining(String kA, String kB) {
        long cooldownUntil = getLastWarEnd(kA, kB);
        if (cooldownUntil <= System.currentTimeMillis())
            return 0;
        return (cooldownUntil - System.currentTimeMillis()) / 86400000L + 1;
    }

    // ========================================================================
    // UTILIDADES
    // ========================================================================

    /** Notifica todos os membros online de um reino com uma mensagem */
    private void notifyKingdomMembers(String kingdomId, String messageKey, Object... args) {
        KingdomManager km = plugin.getKingdomManager();
        var msg = plugin.getMessageManager();

        // Rei
        UUID king = km.getRei(kingdomId);
        if (king != null) {
            Player p = Bukkit.getPlayer(king);
            if (p != null && p.isOnline()) {
                msg.send(p, messageKey, args);
            }
        }

        // Súditos
        List<String> members = km.getSuditosList(kingdomId);
        if (members != null) {
            for (String memberStr : members) {
                try {
                    UUID memberId = UUID.fromString(memberStr);
                    Player p = Bukkit.getPlayer(memberId);
                    if (p != null && p.isOnline()) {
                        msg.send(p, messageKey, args);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    /** Envia title para todos os membros online de um reino */
    private void notifyKingdomTitle(String kingdomId,
            String titleKey, String subtitleKey,
            Object... args) {
        KingdomManager km = plugin.getKingdomManager();
        var msg = plugin.getMessageManager();

        UUID king = km.getRei(kingdomId);
        if (king != null) {
            Player p = Bukkit.getPlayer(king);
            if (p != null && p.isOnline()) {
                msg.sendTitle(p, titleKey, subtitleKey, 10, 70, 20, args);
            }
        }

        List<String> members = km.getSuditosList(kingdomId);
        if (members != null) {
            for (String memberStr : members) {
                try {
                    UUID memberId = UUID.fromString(memberStr);
                    Player p = Bukkit.getPlayer(memberId);
                    if (p != null && p.isOnline()) {
                        msg.sendTitle(p, titleKey, subtitleKey, 10, 70, 20, args);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    /** Formata milissegundos restantes em texto legível (ex: "2d 5h", "3h 20m") */
    public static String formatDuration(long ms) {
        if (ms <= 0)
            return "0m";

        long totalMinutes = ms / 60000;
        long days = totalMinutes / 1440;
        long hours = (totalMinutes % 1440) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0 || sb.isEmpty())
            sb.append(minutes).append("m");

        return sb.toString().trim();
    }

    /** Retorna o custo de declaração */
    public double getDeclarationCost() {
        return plugin.getConfig().getDouble("war.declaration_cost", 10000.0);
    }
}
