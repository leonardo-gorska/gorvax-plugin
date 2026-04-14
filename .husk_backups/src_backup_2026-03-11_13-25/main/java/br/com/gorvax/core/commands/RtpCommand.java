package br.com.gorvax.core.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.MessageManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B25 — Comando /rtp nativo para teleporte aleatório.
 * Substitui a dependência do BetterRTP com um sistema integrado ao GorvaxCore.
 *
 * Features:
 * - Cooldown configurável por jogador
 * - Raio min/max em torno do spawn
 * - Evita claims, regiões protegidas e blocos perigosos
 * - Bloqueado durante combat tag
 * - Somente no Overworld
 */
public class RtpCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;
    private final MessageManager msg;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // Configs
    private int cooldownSeconds;
    private int maxRadius;
    private int minRadius;
    private int maxAttempts;
    private Set<Material> unsafeBlocks;

    public RtpCommand(GorvaxCore plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
        loadConfig();
    }

    /**
     * Carrega/recarrega configurações do config.yml.
     */
    public void loadConfig() {
        var config = plugin.getConfig();
        this.cooldownSeconds = config.getInt("rtp.cooldown_seconds", 600);
        this.maxRadius = config.getInt("rtp.max_radius", 5000);
        this.minRadius = config.getInt("rtp.min_radius", 500);
        this.maxAttempts = config.getInt("rtp.max_attempts", 10);

        // Blocos inseguros para teleporte
        this.unsafeBlocks = EnumSet.noneOf(Material.class);
        List<String> unsafeList = config.getStringList("rtp.unsafe_blocks");
        if (unsafeList.isEmpty()) {
            // Padrão se não configurado
            unsafeBlocks.addAll(List.of(
                    Material.LAVA, Material.FIRE, Material.SOUL_FIRE,
                    Material.MAGMA_BLOCK, Material.CACTUS,
                    Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW,
                    Material.WITHER_ROSE));
        } else {
            for (String s : unsafeList) {
                try {
                    unsafeBlocks.add(Material.valueOf(s.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("[RTP] Bloco inseguro inválido no config: " + s);
                }
            }
        }
    }

    /**
     * Recarrega configurações.
     */
    public void reload() {
        loadConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "general.player_only");
            return true;
        }

        // Verificar combat tag
        if (plugin.getCombatManager() != null && plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            String message = msg.get("rtp.in_combat");
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            return true;
        }

        // Verificar cooldown
        if (!player.hasPermission("gorvax.admin")) {
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null) {
                long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
                if (elapsed < cooldownSeconds) {
                    long remaining = cooldownSeconds - elapsed;
                    String minutes = String.valueOf(remaining / 60);
                    String seconds = String.valueOf(remaining % 60);
                    String message = msg.get("rtp.cooldown", minutes, seconds);
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
                    return true;
                }
            }
        }

        // Verificar mundo (somente Overworld)
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            String message = msg.get("rtp.wrong_world");
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            return true;
        }

        // Enviar mensagem de busca
        String searchMsg = msg.get("rtp.searching");
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(searchMsg));

        // Buscar localização segura de forma assíncrona
        Location spawn = world.getSpawnLocation();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Location safeLoc = findSafeLocation(world, spawn);

            if (safeLoc == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    String noLoc = msg.get("rtp.no_safe_location");
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(noLoc));
                });
                return;
            }

            // Teleportar na main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline())
                    return;

                player.teleport(safeLoc);
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

                int x = safeLoc.getBlockX();
                int z = safeLoc.getBlockZ();
                String successMsg = msg.get("rtp.success", String.valueOf(x), String.valueOf(z));
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(successMsg));
            });
        });

        return true;
    }

    /**
     * Busca uma localização segura aleatória dentro do raio configurado.
     * Executado em thread assíncrona — usa apenas leitura de chunks já carregados
     * ou bloqueia até o chunk ser gerado.
     */
    private Location findSafeLocation(World world, Location spawn) {
        Random random = new Random();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Gerar coordenadas aleatórias dentro do raio min-max
            double angle = random.nextDouble() * 2 * Math.PI;
            int distance = minRadius + random.nextInt(maxRadius - minRadius + 1);
            int x = spawn.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = spawn.getBlockZ() + (int) (Math.sin(angle) * distance);

            // Buscar o bloco mais alto de forma segura
            // Nota: getHighestBlockYAt é thread-safe no Paper
            int y;
            try {
                y = world.getHighestBlockYAt(x, z);
            } catch (Exception e) {
                continue;
            }

            if (y <= 0 || y >= 320)
                continue;

            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);

            // Verificar se é seguro
            if (!isSafeLocation(world, x, y, z))
                continue;

            // Verificar se está dentro de um claim
            if (isInClaim(loc))
                continue;

            return loc;
        }

        return null;
    }

    /**
     * Verifica se a localização é segura para teleporte.
     */
    private boolean isSafeLocation(World world, int x, int y, int z) {
        try {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            // Chão deve ser sólido
            if (!ground.getType().isSolid())
                return false;

            // Chão não pode ser bloco inseguro
            if (unsafeBlocks.contains(ground.getType()))
                return false;

            // Espaço para pés e cabeça deve ser passável (ar, planta pequena, etc.)
            if (feet.getType().isSolid() || unsafeBlocks.contains(feet.getType()))
                return false;
            if (head.getType().isSolid() || unsafeBlocks.contains(head.getType()))
                return false;

            // Não teleportar em água profunda (mais de 1 bloco)
            if (feet.isLiquid() || head.isLiquid())
                return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se a localização está dentro de um claim.
     * Thread-safe pois ClaimManager usa ConcurrentHashMap.
     */
    private boolean isInClaim(Location loc) {
        try {
            return plugin.getClaimManager().getClaimAt(loc) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Limpa cooldown de um jogador (para uso no onQuit se necessário).
     */
    public void clearCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
            @NotNull String label, String[] args) {
        // /rtp não tem subcomandos — retorna lista vazia
        return Collections.emptyList();
    }
}
