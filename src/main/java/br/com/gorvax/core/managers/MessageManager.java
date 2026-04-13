package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Gerencia todas as mensagens configuráveis do plugin.
 * Carrega e fornece acesso a mensagens do arquivo messages.yml.
 */
public class MessageManager {

    private final GorvaxCore plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessageManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Merge defaults from JAR to ensure new keys are available after updates
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }
    }

    /**
     * Recarrega o arquivo messages.yml do disco.
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Obtém uma mensagem pelo caminho (key), substituindo placeholders posicionais.
     * Placeholders: {0}, {1}, {2}, ...
     *
     * @param key  Caminho da mensagem no YAML (ex: "kingdom.error.no_permission")
     * @param args Argumentos para substituir {0}, {1}, etc.
     * @return A mensagem formatada, ou a própria key se não encontrada.
     */
    public String get(String key, Object... args) {
        String msg = messages.getString(key);
        if (msg == null) {
            plugin.getLogger().warning("[MessageManager] Chave não encontrada: " + key);
            return "§c[Mensagem ausente: " + key + "]";
        }

        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return msg;
    }

    /**
     * B4.3 — Obtém uma mensagem adaptada à plataforma do jogador.
     * Se o jogador é Bedrock e existe a chave "key.bedrock", usa a variante
     * Bedrock.
     * Caso contrário, usa a chave padrão.
     *
     * @param player Jogador para detectar plataforma
     * @param key    Chave da mensagem
     * @param args   Argumentos para placeholders
     * @return A mensagem formatada
     */
    public String getForPlayer(Player player, String key, Object... args) {
        if (InputManager.isBedrockPlayer(player)) {
            String bedrockKey = key + ".bedrock";
            String bedrockMsg = messages.getString(bedrockKey);
            if (bedrockMsg != null) {
                for (int i = 0; i < args.length; i++) {
                    bedrockMsg = bedrockMsg.replace("{" + i + "}", String.valueOf(args[i]));
                }
                return bedrockMsg;
            }
        }
        return get(key, args);
    }

    /**
     * B4.3 — Envia uma mensagem adaptada à plataforma do jogador.
     */
    public void sendForPlayer(Player player, String key, Object... args) {
        player.sendMessage(getForPlayer(player, key, args));
    }

    /**
     * Envia uma mensagem a um CommandSender (Player ou Console).
     */
    public void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(get(key, args));
    }

    /**
     * Envia uma mensagem ao jogador.
     */
    public void send(Player player, String key, Object... args) {
        player.sendMessage(get(key, args));
    }

    /**
     * Envia um título ao jogador.
     *
     * @param player      Jogador destinatário.
     * @param titleKey    Chave da mensagem do título. Pode ser null para título
     *                    vazio.
     * @param subtitleKey Chave da mensagem do subtítulo. Pode ser null para
     *                    subtítulo vazio.
     * @param fadeIn      Ticks de fade in.
     * @param stay        Ticks de permanência.
     * @param fadeOut     Ticks de fade out.
     * @param args        Argumentos para placeholders (aplicados em ambos título e
     *                    subtítulo).
     */
    public void sendTitle(Player player, String titleKey, String subtitleKey,
            int fadeIn, int stay, int fadeOut, Object... args) {
        String title = titleKey != null ? get(titleKey, args) : "";
        String subtitle = subtitleKey != null ? get(subtitleKey, args) : "";
        LegacyComponentSerializer lcs = LegacyComponentSerializer.legacySection();
        player.showTitle(Title.title(
                lcs.deserialize(title),
                lcs.deserialize(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L))));
    }

    /**
     * Envia uma ActionBar ao jogador.
     */
    public void sendActionBar(Player player, String key, Object... args) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(get(key, args)));
    }

    /**
     * Envia uma mensagem broadcast para todos os jogadores online.
     */
    public void broadcast(String key, Object... args) {
        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(get(key, args)));
    }
}
