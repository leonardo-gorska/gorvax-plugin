package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.boss.model.WorldBoss;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class EndResetManager {

    private final GorvaxCore plugin;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public EndResetManager(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // BUG-16 / MELHORIA-01: Método extraído para evitar duplicação
    private World getEndWorld() {
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld == null) {
            for (World w : Bukkit.getWorlds()) {
                if (w.getEnvironment() == World.Environment.THE_END) {
                    return w;
                }
            }
        }
        return endWorld;
    }

    public void startScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkSchedules();
            }
        }.runTaskTimer(plugin, 60L, 600L); // Delay inicial: 3s (60 ticks); Intervalo: 30s (600 ticks)
    }

    private void checkSchedules() {
        if (!plugin.getConfig().getBoolean("end_reset.enabled", true))
            return;

        LocalDateTime now = LocalDateTime.now();
        String currentTimeStr = now.format(timeFormatter);

        // --- DRAGON RESET CHECK ---
        String dragonResetTimeStr = plugin.getConfig().getString("end_reset.dragon_reset_time", "03:00");
        try {
            LocalTime dragonResetTime = LocalTime.parse(dragonResetTimeStr, timeFormatter);
            LocalTime nowTime = now.toLocalTime();

            // Checar reset exato
            if (currentTimeStr.equals(dragonResetTimeStr)) {
                // BUG-10 Fix: Ensure we only run once per day
                if (plugin.getConfig().getLong("end_reset.last_dragon_reset_day", 0) != now.getDayOfYear()) {
                    plugin.getConfig().set("end_reset.last_dragon_reset_day", now.getDayOfYear());
                    plugin.saveConfig();
                    resetDragonBattle();
                }
            } else {
                // Checar avisos (Warnings)
                checkAndSendWarning(nowTime, dragonResetTime, "dragon");
            }

            // --- END DIMENSION RESET CHECK ---
            String endResetTimeStr = plugin.getConfig().getString("end_reset.dimension_reset_time", "04:00");
            String endResetDayStr = plugin.getConfig().getString("end_reset.dimension_reset_day", "MONDAY");
            LocalTime endResetTime = LocalTime.parse(endResetTimeStr, timeFormatter);

            // Verificar o dia e a hora
            try {
                DayOfWeek day = DayOfWeek.valueOf(endResetDayStr.toUpperCase());
                if (now.getDayOfWeek() == day) {

                    // Checar reset exato
                    if (currentTimeStr.equals(endResetTimeStr)) {
                        resetEndDimension();
                    } else {
                        // Checar avisos (Warnings)
                        checkAndSendWarning(nowTime, endResetTime, "end");

                        // Checar aviso de Kick (2 minutos antes) - ESPECÍFICO PARA KICK
                        long minutesUntil = Duration.between(nowTime, endResetTime).toMinutes();
                        if (minutesUntil < 0)
                            minutesUntil += 24 * 60;

                        if (minutesUntil == 2) {
                            String kickMsg = plugin.getConfig().getString("messages.warning_kick",
                                    "§4§l[Expulsão] §cA dimensão está colapsando! Fujam agora!");
                            // BUG-16 FIX: Usar getEndWorld() extraído
                            World endWorld = getEndWorld();
                            if (endWorld != null) {
                                for (Player p : endWorld.getPlayers()) {
                                    p.sendMessage(kickMsg);
                                    plugin.getMessageManager().sendTitle(p, "end_reset.title_flee",
                                            "end_reset.subtitle_flee", 10, 100, 20);
                                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
                                }
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Dia inválido no config end_reset.dimension_reset_day: " + endResetDayStr);
            }

        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Erro no formato de hora do reset: " + e.getMessage());
        }
    }

    private void checkAndSendWarning(LocalTime now, LocalTime target, String type) {
        long minutesUntil = ChronoUnit.MINUTES.between(now, target);
        if (minutesUntil < 0) {
            minutesUntil += 24 * 60;
        }

        List<Integer> intervals = plugin.getConfig().getIntegerList("end_reset.warning_intervals");
        if (intervals.isEmpty()) {
            intervals = Arrays.asList(60, 30, 10, 5, 2);
        }

        if (intervals.contains((int) minutesUntil)) {
            String msgKey = "messages.warning_" + type;
            String msg = plugin.getConfig().getString(msgKey);
            if (msg != null && !msg.isEmpty()) {
                msg = msg.replace("{tempo}", minutesUntil + " minutos");
                Bukkit.broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .deserialize(msg));

                // Sound Effect for Global Warnings
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.0f);
                }
            }
        }
    }

    public void resetDragonBattle() {
        World endWorld = getEndWorld();

        if (endWorld == null || endWorld.getEnvironment() != World.Environment.THE_END) {
            plugin.getLogger().warning("Mundo do The End não encontrado.");
            return;
        }

        // ============================================================
        // CARREGAR CHUNKS: O dragão voa pela ilha do End.
        // Precisamos carregar vários chunks para encontrá-lo.
        // A ilha central tem ~60 blocos de raio ≈ 4 chunks.
        // ============================================================
        int chunkRadius = 5;
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                endWorld.getChunkAt(cx, cz).load(true);
            }
        }
        // Manter o chunk central force-loaded para o respawn funcionar sem jogadores
        endWorld.getChunkAt(0, 0).setForceLoaded(true);

        DragonBattle battle = endWorld.getEnderDragonBattle();
        if (battle == null) {
            plugin.getLogger().severe("DragonBattle é NULL. Verifique bukkit.yml/spigot.yml.");
            return;
        }

        plugin.getLogger().info("Iniciando reset do Ender Dragon (método robusto v4).");

        // ============================================================
        // ETAPA 0: Remover o dragão (se vivo)
        //
        // PROBLEMA: setHealth(0) precisa de entity ticking (animação de
        // morte ~200 ticks) para o NMS chamar setDragonKilled() e limpar
        // o dragonUUID. Sem jogadores no End, entities NÃO tickam.
        //
        // SOLUÇÃO: remove() imediato + reflection para limpar dragonUUID
        // diretamente no NMS EndDragonFight.
        // ============================================================
        boolean dragonWasAlive = false;

        // Nível 1: API oficial
        EnderDragon trackedDragon = battle.getEnderDragon();
        if (trackedDragon != null) {
            plugin.getLogger().info("Dragão detectado via API (HP=" + trackedDragon.getHealth()
                    + ", Loc=" + trackedDragon.getLocation().toVector() + "). Removendo imediatamente...");
            trackedDragon.remove();
            dragonWasAlive = true;
        }

        // Nível 2: Varrer entidades (pega dragões órfãos não rastreados pelo battle)
        for (Entity e : endWorld.getEntities()) {
            if (e instanceof EnderDragon dragon) {
                plugin.getLogger().info("Dragão extra encontrado (HP=" + dragon.getHealth() + "). Removendo...");
                dragon.remove();
                dragonWasAlive = true;
            }
        }

        // Limpar o dragonUUID no NMS via reflection
        // Isso é NECESSÁRIO porque remove() não chama setDragonKilled()
        // e o NMS continua achando que o dragão existe (bloqueia respawn)
        clearNMSDragonUUID(battle);

        // Garantir estado correto
        battle.setPreviouslyKilled(true);

        // Limpar cristais existentes do portal (não das torres)
        Location portalCenter = new Location(endWorld, 0, 64, 0);
        for (Entity e : endWorld.getNearbyEntities(portalCenter, 6, 20, 6)) {
            if (e instanceof EnderCrystal) {
                e.remove();
            }
        }

        // Delay curto (2s) — não precisamos mais esperar a animação de morte
        // pois limpamos o dragonUUID via reflection
        long initialDelay = 40L; // 2s
        plugin.getLogger().info("Aguardando 2s antes de iniciar respawn..."
                + (dragonWasAlive ? " (dragão foi removido)" : " (dragão já estava morto)"));

        new BukkitRunnable() {
            @Override
            public void run() {
                // ============================================================
                // ETAPA 1: Preparar estado do DragonBattle
                // ============================================================
                plugin.getLogger().info("Etapa 1: Configurando estado do DragonBattle...");

                // Garantir que o NMS sabe que o dragão já foi morto antes
                // Isso é necessário para que initiateRespawn() não trate como "first fight"
                battle.setPreviouslyKilled(true);

                // Fechar portal de saída (remover blocos END_PORTAL)
                closeExitPortal(endWorld);

                // Regenerar a estrutura de bedrock do portal (sem portals abertos)
                battle.generateEndPortal(false);

                // Resetar cristais das torres de obsidiana
                battle.resetCrystals();

                plugin.getLogger().info("Estado do DragonBattle preparado.");

                // ============================================================
                // ETAPA 2: Iniciar respawn via API Paper (bypass NMS crystal scan)
                // ============================================================
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getLogger().info("Etapa 2: Chamando initiateRespawn(null) — modo bypass...");

                    // initiateRespawn(null) bypassa o scanForCrystals() interno do NMS
                    // e torna o respawn "uncancellable" (cristais não podem ser destruídos para
                    // cancelar)
                    boolean success = battle.initiateRespawn(null);

                    if (success) {
                        onRespawnSuccess(battle);
                    } else {
                        plugin.getLogger()
                                .warning("initiateRespawn(null) retornou false. Tentando fallback com cristais...");

                        // ============================================================
                        // ETAPA 3 (Fallback): Spawnar cristais e tentar com lista explícita
                        // ============================================================
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            List<EnderCrystal> crystals = spawnPortalCrystals(endWorld);
                            plugin.getLogger().info("Etapa 3: Chamando initiateRespawn(crystalList) com "
                                    + crystals.size() + " cristais...");

                            boolean fallbackSuccess = battle.initiateRespawn(crystals);

                            if (fallbackSuccess) {
                                onRespawnSuccess(battle);
                            } else {
                                plugin.getLogger().warning(
                                        "Fallback com cristais também retornou false. Tentando último recurso...");

                                // ============================================================
                                // ETAPA 4 (Último recurso): initiateRespawn() vanilla
                                // ============================================================
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    // Limpar tudo e tentar o método vanilla (sem args)
                                    battle.setPreviouslyKilled(true);
                                    battle.generateEndPortal(false);
                                    List<EnderCrystal> freshCrystals = spawnPortalCrystals(endWorld);

                                    battle.initiateRespawn();

                                    // Monitorar o resultado
                                    new BukkitRunnable() {
                                        int checks = 0;

                                        @Override
                                        public void run() {
                                            checks++;
                                            DragonBattle.RespawnPhase phase = battle.getRespawnPhase();
                                            if (phase != null && phase != DragonBattle.RespawnPhase.NONE) {
                                                onRespawnSuccess(battle);
                                                this.cancel();
                                            } else if (checks >= 10) {
                                                plugin.getLogger().severe(
                                                        "FALHA: Todas as tentativas falharam. O dragão será resetado no próximo ciclo agendado.");
                                                // Limpar cristais que sobraram
                                                freshCrystals.forEach(Entity::remove);
                                                // Liberar chunk forçado
                                                World end = getEndWorld();
                                                if (end != null)
                                                    end.getChunkAt(0, 0).setForceLoaded(false);
                                                this.cancel();
                                            }
                                        }
                                    }.runTaskTimer(plugin, 20L, 20L);
                                }, 40L);
                            }
                        }, 40L);
                    }
                }, 40L); // 2s depois de preparar o estado
            }
        }.runTaskLater(plugin, initialDelay);
    }

    /**
     * Limpa o dragonUUID no NMS EndDragonFight via reflection.
     * Isso é necessário porque remove() na entidade NÃO limpa o UUID
     * interno que o NMS usa para bloquear initiateRespawn().
     *
     * Funciona com Paper 1.21.x (Mojang mappings).
     */
    private void clearNMSDragonUUID(DragonBattle battle) {
        try {
            // CraftDragonBattle tem um campo "handle" que é o EndDragonFight do NMS
            Field handleField = battle.getClass().getDeclaredField("handle");
            handleField.setAccessible(true);
            Object endDragonFight = handleField.get(battle);

            // Procurar o campo UUID no EndDragonFight (dragonUUID em Mojang mappings)
            // Procuramos por tipo (UUID) para ser resiliente a mudanças de nome
            boolean clearedUUID = false;
            for (Field f : endDragonFight.getClass().getDeclaredFields()) {
                if (f.getType() == UUID.class) {
                    f.setAccessible(true);
                    Object value = f.get(endDragonFight);
                    if (value != null) {
                        f.set(endDragonFight, null);
                        plugin.getLogger().info("NMS dragonUUID limpo via reflection (campo: " + f.getName() + ")");
                        clearedUUID = true;
                    }
                }
            }

            // Também setar dragonKilled = true no NMS
            // Procurar booleanos que possam ser "dragonKilled" ou "previouslyKilled"
            for (Field f : endDragonFight.getClass().getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    String name = f.getName().toLowerCase();
                    if (name.contains("killed") || name.contains("dead") || name.contains("dragon")) {
                        f.setAccessible(true);
                        if (!f.getBoolean(endDragonFight)) {
                            f.set(endDragonFight, true);
                            plugin.getLogger().info("NMS flag '" + f.getName() + "' setado para true via reflection");
                        }
                    }
                }
            }

            if (!clearedUUID) {
                plugin.getLogger()
                        .info("Nenhum campo UUID encontrado para limpar (dragão pode já estar morto no NMS).");
            }
        } catch (NoSuchFieldException e) {
            plugin.getLogger().warning("Campo 'handle' não encontrado em CraftDragonBattle. "
                    + "Isso pode indicar uma versão incompatível do Paper. Erro: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().warning("Falha ao limpar dragonUUID via reflection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Chamado quando o respawn do dragão é iniciado com sucesso.
     */
    private void onRespawnSuccess(DragonBattle battle) {
        DragonBattle.RespawnPhase phase = battle.getRespawnPhase();
        plugin.getLogger().info("Sucesso! Respawn iniciado. Fase: " + phase);

        // Liberar chunk forçado (o NMS cuida do restante do respawn sozinho)
        World endWorld = getEndWorld();
        if (endWorld != null)
            endWorld.getChunkAt(0, 0).setForceLoaded(false);

        String successMsg = plugin.getConfig().getString("messages.success_dragon",
                "§5§l[Renascimento] §dO Ender Dragon retornou ao seu trono!");
        Bukkit.broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .deserialize(successMsg));

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getMessageManager().sendTitle(p, "end_reset.title_dragon_return",
                    "end_reset.subtitle_dragon_return", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
    }

    /**
     * Spawna 4 End Crystals nas posições corretas do portal de saída do End.
     * Retorna a lista de cristais criados para uso com initiateRespawn(Collection).
     *
     * As posições vanilla são nas 4 bordas da cruz de bedrock do portal central:
     * Norte (0, 65, -3), Sul (0, 65, 4), Leste (3, 65, 0), Oeste (-3, 65, 0)
     * sobre blocos de bedrock em Y=64.
     */
    private List<EnderCrystal> spawnPortalCrystals(World endWorld) {
        Location portalCenter = new Location(endWorld, 0, 64, 0);
        List<EnderCrystal> crystals = new ArrayList<>();

        // Posições vanilla dos cristais de respawn (bordas da cruz de bedrock)
        int[][] blockPositions = {
                { 0, -3 }, // Norte
                { 0, 4 }, // Sul
                { 3, 0 }, // Leste
                { -3, 0 } // Oeste
        };

        // Limpar cristais existentes perto do portal antes de spawnar novos
        for (Entity e : endWorld.getNearbyEntities(portalCenter, 6, 20, 6)) {
            if (e instanceof EnderCrystal) {
                e.remove();
            }
        }

        int spawned = 0;
        for (int[] pos : blockPositions) {
            int bx = pos[0];
            int bz = pos[1];

            // Garantir bedrock abaixo do cristal (pré-requisito NMS)
            Block bedrockBlock = endWorld.getBlockAt(bx, 64, bz);
            if (bedrockBlock.getType() != Material.BEDROCK) {
                bedrockBlock.setType(Material.BEDROCK);
            }

            // Entidade centralizada no bloco, em Y=65 (sobre o bedrock em Y=64)
            Location spawnLoc = new Location(endWorld, bx + 0.5, 65.0, bz + 0.5);
            spawnLoc.getChunk().load(true);

            EnderCrystal crystal = endWorld.spawn(spawnLoc, EnderCrystal.class);
            crystal.setShowingBottom(false);
            crystal.setBeamTarget(null);
            crystals.add(crystal);
            spawned++;

            plugin.getLogger().info("Cristal #" + spawned + " spawnado em ("
                    + bx + ", 65, " + bz + ") — bloco bedrock em (" + bx + ", 64, " + bz + ")");
        }

        plugin.getLogger().info(spawned + " cristais posicionados nos cantos do portal de saída.");
        return crystals;
    }

    public void resetEndDimension() {
        // BUG-16 FIX: Usar getEndWorld() extraído
        World endWorld = getEndWorld();

        File worldFolder = null;
        if (endWorld != null) {
            worldFolder = endWorld.getWorldFolder();

            // BUG-08 FIX: Coletar antes para evitar ConcurrentModificationException
            final World finalEnd = endWorld;
            List<WorldBoss> toRemove = new ArrayList<>();
            for (var boss : plugin.getBossManager().getActiveBosses().values()) {
                if (boss.getEntity() != null && boss.getEntity().getWorld().equals(finalEnd)) {
                    toRemove.add(boss);
                }
            }
            toRemove.forEach(boss -> plugin.getBossManager().removeBoss(boss));

            // RPG Title for Everyone before Kick
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.getMessageManager().sendTitle(p, "end_reset.title_reset", "end_reset.subtitle_reset", 10, 100,
                        20);
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
            }

            // 1. Teleport & Kick
            World mainWorld = Bukkit.getWorlds().get(0);
            Location spawn = mainWorld.getSpawnLocation();

            for (Player player : endWorld.getPlayers()) {
                player.teleport(spawn);
                player.sendMessage(plugin.getMessageManager().get("end_reset.kick_message"));
            }

            // 2. Unload
            plugin.getLogger().info("Descarregando mundo: " + endWorld.getName());
            if (!Bukkit.unloadWorld(endWorld, false)) {
                plugin.getLogger().severe("Falha ao descarregar world_the_end!");
            }
        }

        // 3. Delete Region Files
        File regionFolder = null;
        if (worldFolder != null) {
            File f1 = new File(worldFolder, "region");
            File f2 = new File(worldFolder, "DIM1/region");
            if (f1.exists())
                regionFolder = f1;
            else if (f2.exists())
                regionFolder = f2;
        } else {
            File container = Bukkit.getWorldContainer();
            File f1 = new File(container, "world_the_end/region");
            File f2 = new File(container, "world_the_end/DIM1/region");
            if (f1.exists())
                regionFolder = f1;
            else if (f2.exists())
                regionFolder = f2;
        }

        if (regionFolder != null && regionFolder.exists()) {
            plugin.getLogger().info("Deletando arquivos de região em: " + regionFolder.getAbsolutePath());
            if (deleteDirectory(regionFolder)) {
                plugin.getLogger().info("Arquivos de região do End deletados.");
            } else {
                plugin.getLogger().severe("Falha ao deletar arquivos de região!");
            }
        } else {
            plugin.getLogger().warning("Pasta region do End não encontrada.");
        }

        // 4. Recreate World
        plugin.getLogger().info("Carregando world_the_end novamente...");
        String worldName = (worldFolder != null) ? worldFolder.getName() : "world_the_end";
        World newEnd = Bukkit.createWorld(new WorldCreator(worldName).environment(World.Environment.THE_END));

        if (newEnd != null) {
            newEnd.setKeepSpawnInMemory(false);
        }

        String successMsg = plugin.getConfig().getString("messages.success_end",
                "§a§l[Gênesis] §2O The End foi purificado e renasceu das cinzas!");
        Bukkit.broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .deserialize(successMsg));

        // Final Success Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    private void closeExitPortal(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null)
            return;

        Location loc = battle.getEndPortalLocation();
        if (loc == null)
            loc = new Location(world, 0, 64, 0);

        // Limpar blocos de portal e ovo em um raio seguro ao redor do centro
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -2; y <= 4; y++) {
                    Block b = world.getBlockAt(loc.clone().add(x, y, z));
                    if (b.getType() == Material.END_PORTAL || b.getType() == Material.DRAGON_EGG) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private boolean deleteDirectory(File path) {
        if (!path.exists())
            return true;
        try {
            Files.walkFileTree(path.toPath(),
                    EnumSet.noneOf(FileVisitOption.class),
                    Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao deletar diretório: " + path.getAbsolutePath(), e);
            return false;
        }
    }
}
