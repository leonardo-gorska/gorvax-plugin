package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.StructureManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B22 — Listener que detecta entrada/saída de jogadores em estruturas mapeadas.
 * Mostra Title ao entrar e ActionBar ao sair.
 */
public class StructureListener implements Listener {

    private final GorvaxCore plugin;
    /** Rastreia em qual estrutura cada jogador está (null se nenhuma). */
    private final Map<UUID, String> playerInStructure = new ConcurrentHashMap<>();

    public StructureListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Otimização: só processa se mudou de bloco
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        StructureManager sm = plugin.getStructureManager();

        StructureManager.StructureData current = sm.getStructureAt(event.getTo());
        String previousId = playerInStructure.get(uuid);

        String currentId = current != null ? current.id() : null;

        // Sem mudança
        if (java.util.Objects.equals(previousId, currentId))
            return;

        // Saiu de uma estrutura
        if (previousId != null && currentId == null) {
            playerInStructure.remove(uuid);
            StructureManager.StructureData prev = sm.get(previousId);
            if (prev != null) {
                player.sendActionBar(LegacyComponentSerializer.legacySection()
                        .deserialize("§7⬅ Saindo de " + prev.nome()));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            }
        }

        // Entrou em uma estrutura
        if (currentId != null && !currentId.equals(previousId)) {
            playerInStructure.put(uuid, currentId);
            // Title com nome do local
            player.showTitle(Title.title(
                    LegacyComponentSerializer.legacySection().deserialize(current.nome()),
                    LegacyComponentSerializer.legacySection().deserialize("§7" + getTemaLabel(current.tema())),
                    Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2500),
                            java.time.Duration.ofMillis(1000))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        }
    }

    private String getTemaLabel(String tema) {
        return switch (tema) {
            case "deserto" -> "🏜️ Região Desértica";
            case "gelo", "glacial", "neve" -> "❄️ Região Glacial";
            case "nether", "fogo" -> "🔥 Região Infernal";
            case "floresta", "selva" -> "🌿 Região Florestal";
            case "medieval", "castelo" -> "🏰 Região Medieval";
            case "porto", "oceano", "mar" -> "🌊 Região Costeira";
            case "montanha" -> "⛰️ Região Montanhosa";
            case "pantano", "swamp" -> "🍄 Região Pantanosa";
            default -> "⚔️ Região Especial";
        };
    }
}
