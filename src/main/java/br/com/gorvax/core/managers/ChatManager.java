package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * B8 — Gerencia canais de chat e formatação por hierarquia.
 * Canais: GLOBAL, KINGDOM, ALLIANCE, LOCAL, TRADE, NATION (B19).
 */
public class ChatManager {

    /**
     * Canal de chat disponível no servidor.
     */
    public enum ChatChannel {
        GLOBAL("global", "§f§l[GLOBAL] ", "§f"),
        KINGDOM("reino", "§b§l[REINO] ", "§b"),
        ALLIANCE("alianca", "§a§l[ALIANÇA] ", "§a"),
        LOCAL("local", "§e§l[LOCAL] ", "§e"),
        TRADE("comercio", "§6§l[COMÉRCIO] ", "§6"),
        NATION("nacao", "§d§l[NAÇÃO] ", "§d");  // B19 — Sistema de Nações

        private final String label;
        private final String defaultPrefix;
        private final String defaultColor;

        ChatChannel(String label, String defaultPrefix, String defaultColor) {
            this.label = label;
            this.defaultPrefix = defaultPrefix;
            this.defaultColor = defaultColor;
        }

        public String getLabel() {
            return label;
        }

        public String getDefaultPrefix() {
            return defaultPrefix;
        }

        public String getDefaultColor() {
            return defaultColor;
        }

        /**
         * Resolve o canal a partir de uma string (case-insensitive).
         * Aceita aliases em português e inglês.
         */
        public static ChatChannel fromString(String input) {
            if (input == null) return null;
            return switch (input.toLowerCase()) {
                case "global", "g" -> GLOBAL;
                case "reino", "kingdom", "k", "rc" -> KINGDOM;
                case "alianca", "aliança", "alliance", "ac" -> ALLIANCE;
                case "local", "l" -> LOCAL;
                case "comercio", "comércio", "trade", "tc" -> TRADE;
                case "nacao", "nação", "nation", "nc" -> NATION;  // B19
                default -> null;
            };
        }
    }

    private final GorvaxCore plugin;
    private final Map<UUID, ChatChannel> activeChannels = new ConcurrentHashMap<>();

    // B9 — Rate limiter: timestamps das últimas mensagens por jogador
    private final Map<UUID, Deque<Long>> messageTimestamps = new ConcurrentHashMap<>();
    // B9 — Última mensagem por jogador (anti-duplicata)
    private final Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
    // B9 — Mute temporário: UUID → timestamp de expiração
    private final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();

    // Configurações carregadas do config.yml
    private int localRadius;
    private String chatFormat;
    private String rankIconKing;
    private String rankIconVice;
    private String rankIconNoble;
    private String rankIconResident;
    private String colorGlobal;
    private String colorKingdom;
    private String colorAlliance;
    private String colorLocal;
    private String colorTrade;
    private String colorNation;  // B19

    // B9 — Configurações de rate limit
    private int rateLimitMessages;
    private int rateLimitSeconds;

    // B9 — Filtro de palavras
    private final List<Pattern> blockedPatterns = new ArrayList<>();
    private String filterAction; // block, censor, mute
    private int filterMuteDuration; // em segundos

    public ChatManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Carrega configurações de chat do config.yml.
     */
    private void loadConfig() {
        var config = plugin.getConfig();

        this.localRadius = config.getInt("chat.local_radius", 200);
        this.chatFormat = config.getString("chat.format", "{kingdom_tag} {rank} {player}: {message}");

        this.rankIconKing = config.getString("chat.rank_icons.king", "Rei");
        this.rankIconVice = config.getString("chat.rank_icons.vice", "Vice");
        this.rankIconNoble = config.getString("chat.rank_icons.noble", "Nobre");
        this.rankIconResident = config.getString("chat.rank_icons.resident", "Sudito");

        this.colorGlobal = config.getString("chat.channel_colors.global", "§f");
        this.colorKingdom = config.getString("chat.channel_colors.kingdom", "§b");
        this.colorAlliance = config.getString("chat.channel_colors.alliance", "§a");
        this.colorLocal = config.getString("chat.channel_colors.local", "§e");
        this.colorTrade = config.getString("chat.channel_colors.trade", "§6");
        this.colorNation = config.getString("chat.channel_colors.nation", "§d");  // B19

        // B9 — Rate limiter
        this.rateLimitMessages = config.getInt("chat.rate_limit.messages", 3);
        this.rateLimitSeconds = config.getInt("chat.rate_limit.seconds", 5);

        // B9 — Filtro de palavras
        this.filterAction = config.getString("chat.filter.action", "censor");
        this.filterMuteDuration = config.getInt("chat.filter.mute_duration", 300);
        blockedPatterns.clear();
        List<String> patterns = config.getStringList("chat.blocked_words");
        for (String pattern : patterns) {
            try {
                blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                plugin.getLogger().warning("[Chat] Padrão de filtro inválido: " + pattern);
            }
        }
    }

    /**
     * Recarrega configurações. Chamado por /gorvax reload.
     */
    public void reload() {
        loadConfig();
    }

    // ========================================================================
    // Canal ativo por jogador
    // ========================================================================

    /**
     * Retorna o canal ativo do jogador (GLOBAL se não definido).
     */
    public ChatChannel getChannel(UUID uuid) {
        return activeChannels.getOrDefault(uuid, ChatChannel.GLOBAL);
    }

    /**
     * Define o canal ativo do jogador.
     */
    public void setChannel(UUID uuid, ChatChannel channel) {
        if (channel == ChatChannel.GLOBAL) {
            activeChannels.remove(uuid); // Economiza memória, GLOBAL é default
        } else {
            activeChannels.put(uuid, channel);
        }
    }

    /**
     * Remove o jogador do cache (ao sair do servidor).
     */
    public void removePlayer(UUID uuid) {
        activeChannels.remove(uuid);
        messageTimestamps.remove(uuid);
        lastMessages.remove(uuid);
        mutedPlayers.remove(uuid);
    }

    // ========================================================================
    // B9 — Rate Limiter
    // ========================================================================

    /**
     * Resultado da verificação anti-spam.
     */
    public enum SpamCheckResult {
        OK,
        RATE_LIMITED,
        DUPLICATE_MESSAGE,
        MUTED
    }

    /**
     * Verifica se o jogador pode enviar uma mensagem (rate limit + duplicata + mute).
     * Se sim, registra o timestamp. Deve ser chamado ANTES de processar a mensagem.
     */
    public SpamCheckResult checkSpam(UUID uuid, String message) {
        // Verificar mute temporário
        Long muteExpiry = mutedPlayers.get(uuid);
        if (muteExpiry != null) {
            if (System.currentTimeMillis() < muteExpiry) {
                return SpamCheckResult.MUTED;
            } else {
                mutedPlayers.remove(uuid);
            }
        }

        // Verificar mensagem duplicada
        String lastMsg = lastMessages.get(uuid);
        if (lastMsg != null && lastMsg.equalsIgnoreCase(message.trim())) {
            return SpamCheckResult.DUPLICATE_MESSAGE;
        }

        // Verificar rate limit
        long now = System.currentTimeMillis();
        long windowMs = rateLimitSeconds * 1000L;
        Deque<Long> timestamps = messageTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());

        // Remover timestamps antigos fora da janela
        while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) > windowMs) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= rateLimitMessages) {
            return SpamCheckResult.RATE_LIMITED;
        }

        // Registrar mensagem
        timestamps.addLast(now);
        lastMessages.put(uuid, message.trim());

        return SpamCheckResult.OK;
    }

    // ========================================================================
    // B9 — Filtro de Palavras
    // ========================================================================

    /**
     * Resultado do filtro de palavras.
     */
    public enum FilterResult {
        CLEAN,
        BLOCKED,
        CENSORED,
        MUTED
    }

    /**
     * Aplica o filtro de palavras na mensagem.
     * @return par (FilterResult, mensagemFiltrada)
     */
    public FilterResultData applyFilter(UUID uuid, String message) {
        if (blockedPatterns.isEmpty()) {
            return new FilterResultData(FilterResult.CLEAN, message);
        }

        boolean matched = false;
        String filtered = message;

        for (Pattern pattern : blockedPatterns) {
            var matcher = pattern.matcher(filtered);
            if (matcher.find()) {
                matched = true;
                if ("censor".equalsIgnoreCase(filterAction)) {
                    filtered = matcher.replaceAll(mr -> "*".repeat(mr.group().length()));
                }
            }
        }

        if (!matched) {
            return new FilterResultData(FilterResult.CLEAN, message);
        }

        return switch (filterAction.toLowerCase()) {
            case "block" -> new FilterResultData(FilterResult.BLOCKED, message);
            case "mute" -> {
                mutedPlayers.put(uuid, System.currentTimeMillis() + (filterMuteDuration * 1000L));
                yield new FilterResultData(FilterResult.MUTED, message);
            }
            default -> new FilterResultData(FilterResult.CENSORED, filtered); // censor
        };
    }

    /**
     * Dados do resultado do filtro.
     */
    public record FilterResultData(FilterResult result, String filteredMessage) {}

    /**
     * Retorna o tempo restante de mute em segundos, ou 0 se não estiver mutado.
     */
    public int getMuteRemaining(UUID uuid) {
        Long expiry = mutedPlayers.get(uuid);
        if (expiry == null) return 0;
        int remaining = (int) ((expiry - System.currentTimeMillis()) / 1000);
        return Math.max(remaining, 0);
    }

    // ========================================================================
    // B9 — Verificação de /ignore
    // ========================================================================

    /**
     * Verifica se o receptor está ignorando o remetente.
     */
    public boolean isIgnoring(UUID receiver, UUID sender) {
        PlayerData pd = plugin.getPlayerDataManager().getData(receiver);
        return pd.getIgnoredPlayers().contains(sender);
    }

    // ========================================================================
    // Formatação de mensagem
    // ========================================================================

    /**
     * Retorna o ícone de rank do jogador no seu reino.
     */
    public String getRankIcon(Player player) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
        if (kingdom == null) return "";

        String kingdomId = kingdom.getId();

        // Rei
        if (plugin.getKingdomManager().isRei(kingdomId, player.getUniqueId())) {
            return rankIconKing;
        }

        // Vice (trust VICE no claim)
        if (kingdom.hasPermission(player.getUniqueId(), Claim.TrustType.VICE)) {
            return rankIconVice;
        }

        // Nobre (trust GERAL no claim)
        if (kingdom.hasPermission(player.getUniqueId(), Claim.TrustType.GERAL)) {
            return rankIconNoble;
        }

        // Súdito comum
        if (plugin.getKingdomManager().isSudito(kingdomId, player.getUniqueId())) {
            return rankIconResident;
        }

        return "";
    }

    /**
     * Formata a mensagem de chat usando o template configurável.
     * Aplica tag do reino, ícone de rank e cor do canal.
     *
     * @param player  Jogador que enviou
     * @param message Texto da mensagem
     * @param channel Canal de destino
     * @return String formatada com § color codes
     */
    public String formatMessage(Player player, String message, ChatChannel channel) {
        Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());

        // Montar tag do reino
        String kingdomTag = "";
        if (kingdom != null) {
            String tag = kingdom.getTag();
            if (tag != null && !tag.isEmpty()) {
                String color = kingdom.getTagColor();
                kingdomTag = "§f[" + color + tag + "§f]";
            }
        }

        // Ícone de rank
        String rankIcon = getRankIcon(player);

        // Cor do canal
        String channelColor = getChannelColor(channel);

        // Montar a mensagem formatada usando o template
        // B13 — Tag cosmética
        String cosmeticTag = "";
        if (plugin.getCosmeticManager() != null) {
            cosmeticTag = plugin.getCosmeticManager().getActiveChatTag(player);
        }

        String formatted = chatFormat
                .replace("{kingdom_tag}", kingdomTag)
                .replace("{rank}", rankIcon)
                .replace("{player}", player.getName())
                .replace("{message}", channelColor + message);

        // Inserir tag cosmética após o rank icon e antes do nome
        if (!cosmeticTag.isEmpty()) {
            formatted = formatted.replace(rankIcon + " " + player.getName(),
                    rankIcon + " " + cosmeticTag + player.getName());
        }

        // Limpar espaços duplos se não houver tag/rank
        formatted = formatted.replaceAll("  +", " ").trim();

        return formatted;
    }

    /**
     * Retorna o prefixo de exibição do canal.
     */
    public String getChannelPrefix(ChatChannel channel) {
        var msg = plugin.getMessageManager();
        return switch (channel) {
            case GLOBAL -> msg.get("chat.global_prefix");
            case KINGDOM -> msg.get("chat.kingdom_prefix");
            case ALLIANCE -> msg.get("chat.alliance_prefix");
            case LOCAL -> msg.get("chat.local_prefix");
            case TRADE -> msg.get("chat.trade_prefix");
            case NATION -> msg.get("chat.nation_prefix");  // B19
        };
    }

    /**
     * Retorna a cor configurada para o canal.
     */
    public String getChannelColor(ChatChannel channel) {
        return switch (channel) {
            case GLOBAL -> colorGlobal;
            case KINGDOM -> colorKingdom;
            case ALLIANCE -> colorAlliance;
            case LOCAL -> colorLocal;
            case TRADE -> colorTrade;
            case NATION -> colorNation;  // B19
        };
    }

    // ========================================================================
    // Getters de configuração
    // ========================================================================

    public int getLocalRadius() {
        return localRadius;
    }
}
