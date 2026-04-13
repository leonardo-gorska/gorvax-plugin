package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.TutorialManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

/**
 * B4 — Listener de primeiro login e progressão do tutorial.
 * Detecta primeiro join, dá welcome kit, inicia tutorial e
 * monitora ações do jogador para avançar passos automaticamente.
 */
public class WelcomeListener implements Listener {

    private final GorvaxCore plugin;

    public WelcomeListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Detecta primeiro login para iniciar tutorial e dar welcome kit.
     * Priority NORMAL para rodar antes de StatisticsListener (MONITOR) que seta firstJoin.
     * Usamos hasPlayedBefore() do Bukkit como verificação primária.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TutorialManager tutorialManager = plugin.getTutorialManager();
        if (tutorialManager == null || !tutorialManager.isEnabled()) return;

        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        // Primeiro login: detectar via firstJoin == 0 (antes de StatisticsListener setar)
        if (pd.getFirstJoin() == 0L && !pd.isTutorialCompleted()) {
            // Agendar início do tutorial com leve delay para garantir que o jogador veja
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    tutorialManager.startTutorial(player);
                }
            }, 40L); // 2 segundos após login
        } else if (!pd.isTutorialCompleted() && pd.getTutorialStep() >= 1) {
            // Reconexão com tutorial em progresso: restaurar BossBar
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    tutorialManager.restoreBossBar(player);
                }
            }, 40L);
        }
    }

    /**
     * Limpa BossBar quando o jogador sai do servidor.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        TutorialManager tutorialManager = plugin.getTutorialManager();
        if (tutorialManager != null) {
            tutorialManager.removeBossBar(event.getPlayer());
        }
    }

    /**
     * Detecta comandos para avançar passos do tutorial.
     * Passo 3 → 4: ao usar /rtp (fuga do spawn)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        TutorialManager tutorialManager = plugin.getTutorialManager();
        if (tutorialManager == null || !tutorialManager.isInTutorial(player)) return;

        int step = tutorialManager.getCurrentStep(player);
        String cmd = event.getMessage().toLowerCase().trim();

        // Passo 3: jogador usa /rtp para fugir do spawn
        if (step == 3 && cmd.startsWith("/rtp")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && tutorialManager.isInTutorial(player)) {
                    tutorialManager.advanceStep(player);
                }
            }, 60L); // 3s delay para dar tempo ao RTP
        }

        // Passo 5: jogador usa /confirmar ou /c para criar claim
        if (step == 5 && (cmd.startsWith("/confirmar") || cmd.equals("/c"))) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && tutorialManager.isInTutorial(player)) {
                    tutorialManager.advanceStep(player);
                }
            }, 20L); // 1s delay
        }
    }

    /**
     * Detecta interação com Pá de Ouro para avançar passo 4→5 do tutorial.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ignorar off-hand para evitar dupla detecção
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        TutorialManager tutorialManager = plugin.getTutorialManager();
        if (tutorialManager == null || !tutorialManager.isInTutorial(player)) return;

        int step = tutorialManager.getCurrentStep(player);

        // Passo 4: jogador interage com Pá de Ouro (seleção de terreno)
        if (step == 4) {
            if (player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_SHOVEL) {
                // Avançar após clicar com a pá (presume que é a segunda seleção)
                // A lógica real de claim aceita 2 cliques; avançamos otimisticamente
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && tutorialManager.isInTutorial(player)
                            && tutorialManager.getCurrentStep(player) == 4) {
                        tutorialManager.advanceStep(player);
                    }
                }, 5L);
            }
        }
    }
}
