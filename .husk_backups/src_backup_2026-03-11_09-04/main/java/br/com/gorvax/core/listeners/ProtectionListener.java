package br.com.gorvax.core.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.SubPlot;
import br.com.gorvax.core.towns.Relation;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Villager;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtectionListener implements Listener {

    private final GorvaxCore plugin;

    // B1.3 — Mapa para rastrear origem de TNT (entidade UUID → localização de
    // spawn)
    private final Map<UUID, Location> tntOrigins = new ConcurrentHashMap<>();

    // B1.5 — Contadores de ticks de redstone por chunk (chunkKey → contador)
    private final Map<String, AtomicInteger> redstoneCounters = new ConcurrentHashMap<>();

    // MELHORIA-04: Cache via EnumSet para lookup O(1) em vez de String.contains()
    private static final Set<Material> CONTAINERS = EnumSet.noneOf(Material.class);
    private static final Set<Material> INTERACTABLES = EnumSet.noneOf(Material.class);
    private static final Set<Material> SPAWN_EGGS = EnumSet.noneOf(Material.class);

    static {
        for (Material m : Material.values()) {
            String name = m.name();
            // Containers
            if (name.contains("CHEST") || name.contains("SHULKER") || name.contains("BARREL")
                    || name.contains("HOPPER") || name.contains("DISPENSER") || name.contains("DROPPER")
                    || name.contains("FURNACE") || name.contains("SMOKER") || name.contains("BREWING")
                    || name.contains("CHISELED_BOOKSHELF") || name.contains("DECORATED_POT")
                    || name.contains("CRAFTER")) {
                CONTAINERS.add(m);
            }
            // Interactables
            if (name.contains("BUTTON") || name.contains("LEVER") || name.contains("DOOR")
                    || name.contains("GATE") || name.contains("TRAPDOOR")
                    || name.contains("TABLE") || name.contains("ANVIL") || name.contains("GRINDSTONE")
                    || name.contains("LOOM") || name.contains("STONECUTTER") || name.contains("BEACON")
                    || name.contains("JUKEBOX") || name.contains("NOTE_BLOCK") || name.contains("COMPARATOR")
                    || name.contains("REPEATER") || name.contains("DAYLIGHT") || name.contains("BED")
                    || name.contains("BELL") || name.contains("LECTERN") || name.contains("COMPOSTER")
                    || name.contains("CAULDRON") || name.contains("SIGN") || name.contains("CAKE")
                    || name.contains("CANDLE") || name.contains("POT") || name.contains("RESPAWN_ANCHOR")
                    || name.contains("COMMAND") || name.contains("STRUCTURE") || name.contains("COPPER_BULB")) {
                INTERACTABLES.add(m);
            }
            // BUG-13: Spawn Eggs como CONSTRUCAO
            if (name.contains("SPAWN_EGG")) {
                SPAWN_EGGS.add(m);
            }
        }
    }

    public ProtectionListener(GorvaxCore plugin) {
        this.plugin = plugin;

        // B1.5 — Task periódica para resetar contadores de redstone
        int resetInterval = plugin.getConfig().getInt("protection.redstone_reset_interval", 20);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> redstoneCounters.clear(), resetInterval, resetInterval);
    }

    // --- MAIN PROTECTION LOGIC ---

    private boolean canPerformAction(Player p, Location loc, Claim.TrustType type) {
        // 1. Admin/Op Bypass (Regra Suprema)
        if (p.isOp() || p.hasPermission("gorvax.admin"))
            return true;

        Claim claim = plugin.getClaimManager().getClaimAt(loc);
        if (claim == null)
            return true; // Wilderness (Permitido, a menos que haja regra global de mundo, mas foco aqui
                         // é Claim)

        SubPlot plot = claim.getSubPlotAt(loc);

        // 2. ISOLAMENTO DE FEUDO (SubPlot)
        if (plot != null) {
            // Se o feudo tem um senhor (Dono ou Inquilino)
            if (plot.hasEffectiveOwner()) {
                // A regra é ESTRITA do feudo. O Reino não apita nada aqui.
                return plot.hasPermission(p.getUniqueId(), type);
            }
            // Se o feudo NÃO tem dono (Lote à venda, ou lote público da cidade)
            // Cai para a regra do Reino (abaixo)
        }

        // 3. REGRA DO REINO / TERRENO (Claim Principal)
        // Se chegamos aqui, ou não estamos em SubPlot, ou o SubPlot é público/da coroa.

        // A. Staff do Reino (Rei/Duque) ou Dono do Terreno Privado
        if (claim.hasPermissionKingdomScope(p.getUniqueId(), type))
            return true;

        // B. Súditos/Residentes (Permissões Globais)
        // Só se aplica se for um REINO. Terrenos privados não tem "Súditos" dessa
        // forma, só Trust lista.
        if (claim.isKingdom()) {
            boolean isSubject = plugin.getKingdomManager().isSudito(claim.getId(), p.getUniqueId());
            if (isSubject) {
                if (type == Claim.TrustType.CONSTRUCAO && claim.isResidentsBuild())
                    return true;
                if (type == Claim.TrustType.CONTEINER && claim.isResidentsContainer())
                    return true;
                if (type == Claim.TrustType.ACESSO && claim.isResidentsSwitch())
                    return true;
            }

            // B7 — Acesso automático para aliados (apenas ACESSO trust)
            if (type == Claim.TrustType.ACESSO
                    && plugin.getConfig().getBoolean("diplomacy.ally_access_enabled", true)) {
                Claim playerKingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
                if (playerKingdom != null && !playerKingdom.getId().equals(claim.getId())) {
                    if (plugin.getKingdomManager().areAllied(playerKingdom.getId(), claim.getId())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!canPerformAction(e.getPlayer(), e.getBlock().getLocation(), Claim.TrustType.CONSTRUCAO)) {
            e.setCancelled(true);
            sendDenyMessage(e.getPlayer(), Claim.TrustType.CONSTRUCAO);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!canPerformAction(e.getPlayer(), e.getBlock().getLocation(), Claim.TrustType.CONSTRUCAO)) {
            e.setCancelled(true);
            sendDenyMessage(e.getPlayer(), Claim.TrustType.CONSTRUCAO);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!canPerformAction(e.getPlayer(), e.getBlock().getLocation(), Claim.TrustType.CONSTRUCAO)) {
            e.setCancelled(true);
            sendDenyMessage(e.getPlayer(), Claim.TrustType.CONSTRUCAO);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!canPerformAction(e.getPlayer(), e.getBlock().getLocation(), Claim.TrustType.CONSTRUCAO)) {
            e.setCancelled(true);
            sendDenyMessage(e.getPlayer(), Claim.TrustType.CONSTRUCAO);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null)
            return;

        Block b = e.getClickedBlock();
        Material mat = b.getType();
        Claim.TrustType required = null;

        // Proteção contra Physical (Placa de Pressão / Farm Land)
        if (e.getAction() == Action.PHYSICAL) {
            // Se for Farm Land, bloqueia para evitar trampling
            if (mat == Material.FARMLAND) {
                if (!canPerformAction(e.getPlayer(), b.getLocation(), Claim.TrustType.CONSTRUCAO)) {
                    e.setCancelled(true);
                }
                return;
            }
            // Placas de pressão: Acesso (Redstone)
            if (mat.name().contains("PRESSURE_PLATE")) {
                if (!canPerformAction(e.getPlayer(), b.getLocation(), Claim.TrustType.ACESSO)) {
                    e.setCancelled(true);
                }
                return;
            }
            return;
        }

        // Classificação Granular
        if (CONTAINERS.contains(mat)) {
            required = Claim.TrustType.CONTEINER;
        } else if (INTERACTABLES.contains(mat)) {
            required = Claim.TrustType.ACESSO;
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getItem() != null) {
            // B1.2 — Anti-Crystal: bloquear colocação de End Crystal em claims alheios
            if (e.getItem().getType() == Material.END_CRYSTAL
                    && plugin.getConfig().getBoolean("protection.block_crystal_placement", true)) {
                if (!canPerformAction(e.getPlayer(), b.getLocation(), Claim.TrustType.CONSTRUCAO)) {
                    e.setCancelled(true);
                    plugin.getMessageManager().send(e.getPlayer(), "protection.crystal_blocked");
                    return;
                }
            }
            // BUG-13: Spawn eggs são CONSTRUCAO (evitar spawn de mobs em terreno alheio)
            if (SPAWN_EGGS.contains(e.getItem().getType())) {
                required = Claim.TrustType.CONSTRUCAO;
            } else {
                // Uso genérico de item no bloco (balde, isqueiro, etc) = CONSTRUCAO
                required = Claim.TrustType.CONSTRUCAO;
            }
        }

        if (required != null) {
            if (!canPerformAction(e.getPlayer(), b.getLocation(), required)) {
                e.setCancelled(true);
                sendDenyMessage(e.getPlayer(), required);
            }
        }
    }

    // --- B1.1: ANTI-WITHER EM CLAIMS ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getEntityType() != EntityType.WITHER)
            return;
        if (!plugin.getConfig().getBoolean("protection.block_wither_spawn", true))
            return;

        Claim claim = plugin.getClaimManager().getClaimAt(e.getLocation());
        if (claim == null)
            return; // Wilderness — permitido

        // Withers spawnam por construção de jogador — não há como obter o builder
        // diretamente
        // do CreatureSpawnEvent. Por segurança, bloqueamos spawn de Wither dentro de
        // qualquer claim.
        // Admins podem usar /boss spawn se necessário.
        e.setCancelled(true);
        plugin.getLogger()
                .info("[Proteção] Spawn de Wither bloqueado em claim " + claim.getId() + " em " + e.getLocation());

        // Notificar jogadores próximos
        for (Player p : e.getLocation().getNearbyPlayers(16)) {
            plugin.getMessageManager().send(p, "protection.wither_blocked");
        }
    }

    // --- B1.3: ANTI-TNT CANNON (RASTREAMENTO DE ORIGEM) ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTntSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed))
            return;
        if (!plugin.getConfig().getBoolean("protection.block_tnt_from_outside", true))
            return;

        // Registrar localização de origem da TNT para verificação posterior
        tntOrigins.put(e.getEntity().getUniqueId(), e.getLocation().clone());

        // Limpar referência após 30 segundos (TNT max fuse = 80 ticks = 4s, mas margem
        // de segurança)
        Bukkit.getScheduler().runTaskLater(plugin, () -> tntOrigins.remove(e.getEntity().getUniqueId()), 600L);
    }

    // --- B3.1 + B1.2 + B1.3 + B1.4: PROTEÇÃO CONTRA EXPLOSÕES ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        // Proteção global: impede QUALQUER explosão de destruir blocos no mundo inteiro
        // Mantém dano, knockback e efeitos visuais — só remove a destruição de terreno
        if (plugin.getConfig().getBoolean("protection.block_all_explosion_grief", true)) {
            e.blockList().clear();
            return;
        }

        // B1.3 — Anti-TNT Cannon: se TNT foi spawnada fora de claim e explode dentro,
        // remove blocos protegidos
        if (e.getEntity() instanceof TNTPrimed
                && plugin.getConfig().getBoolean("protection.block_tnt_from_outside", true)) {
            Location origin = tntOrigins.remove(e.getEntity().getUniqueId());
            if (origin != null) {
                Claim originClaim = plugin.getClaimManager().getClaimAt(origin);
                // Se a TNT foi spawnada fora de qualquer claim (wilderness)
                // remove blocos que estejam dentro de claims
                if (originClaim == null) {
                    e.blockList().removeIf(block -> plugin.getClaimManager().getClaimAt(block.getLocation()) != null);
                    return;
                }
                // Se a TNT veio de outro claim diferente, protege também
                e.blockList().removeIf(block -> {
                    Claim blockClaim = plugin.getClaimManager().getClaimAt(block.getLocation());
                    return blockClaim != null && !isSameClaim(originClaim, blockClaim);
                });
                return;
            }
        }

        // B1.2 — Anti-Crystal: proteger blocos de claims contra explosão de End Crystal
        if (e.getEntity() instanceof EnderCrystal
                && plugin.getConfig().getBoolean("protection.block_crystal_damage", true)) {
            e.blockList().removeIf(block -> plugin.getClaimManager().getClaimAt(block.getLocation()) != null);
            return;
        }

        // B1.4 — Anti-Bed/Anchor Bomb: explosões de camas (Nether) e Respawn Anchors
        // (Overworld)
        // Essas explosões não têm entidade, mas são capturadas pelo bloco source
        // Para bed/anchor que geram EntityExplodeEvent, proteger blocos de claims
        if (plugin.getConfig().getBoolean("protection.block_bed_explosions", true)) {
            // Wither também gera explosão — já protegemos spawn, mas dupla proteção
            if (e.getEntity() instanceof Wither) {
                e.blockList().removeIf(block -> plugin.getClaimManager().getClaimAt(block.getLocation()) != null);
                return;
            }
        }

        // Proteção genérica: Remove da lista de blocos aqueles que estão dentro de
        // claims
        // Não cancela o evento inteiro — apenas protege os blocos claimados
        e.blockList().removeIf(block -> plugin.getClaimManager().getClaimAt(block.getLocation()) != null);
    }

    // --- B1.4: ANTI-BED/ANCHOR BOMB (BlockExplodeEvent) ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent e) {
        // Proteção global contra destruição de blocos por explosões de bloco (Bed,
        // Anchor)
        if (plugin.getConfig().getBoolean("protection.block_all_explosion_grief", true)) {
            e.blockList().clear();
            return;
        }

        if (!plugin.getConfig().getBoolean("protection.block_bed_explosions", true))
            return;

        // BlockExplodeEvent é usado por camas no Nether e Respawn Anchors no Overworld
        e.blockList().removeIf(block -> plugin.getClaimManager().getClaimAt(block.getLocation()) != null);
    }

    // --- B3.2: PROTEÇÃO CONTRA PISTONS CRUZANDO FRONTEIRAS ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (doesPistonCrossBoundary(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (doesPistonCrossBoundary(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    /**
     * Verifica se um piston está empurrando/puxando blocos através de fronteiras de
     * claims.
     * Cancela se algum bloco movido cruza de fora para dentro ou de dentro para
     * fora de um claim.
     */
    private boolean doesPistonCrossBoundary(Block pistonBlock, List<Block> movedBlocks, BlockFace direction) {
        Claim pistonClaim = plugin.getClaimManager().getClaimAt(pistonBlock.getLocation());

        for (Block block : movedBlocks) {
            // Verifica o bloco na posição atual
            Claim blockClaim = plugin.getClaimManager().getClaimAt(block.getLocation());
            // Verifica o bloco na posição destino (após ser movido)
            Block destination = block.getRelative(direction);
            Claim destClaim = plugin.getClaimManager().getClaimAt(destination.getLocation());

            // Se o bloco cruza fronteira de claim (entra ou sai de um claim diferente)
            if (!isSameClaim(blockClaim, destClaim) || !isSameClaim(pistonClaim, destClaim)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameClaim(Claim a, Claim b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.getId().equals(b.getId());
    }

    // --- B3.3: PROTEÇÃO CONTRA ENDERMAN PEGANDO BLOCOS ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        // B1.2 — Proteção contra pisoteio de crops por qualquer entidade (mobs,
        // jogadores)
        if (e.getBlock().getType() == Material.FARMLAND) {
            Claim claim = plugin.getClaimManager().getClaimAt(e.getBlock().getLocation());
            if (claim != null) {
                // Mobs e entidades não-jogador: always block
                if (!(e.getEntity() instanceof Player)) {
                    e.setCancelled(true);
                    return;
                }
                // Jogadores: verificar permissão
                Player p = (Player) e.getEntity();
                if (!canPerformAction(p, e.getBlock().getLocation(), Claim.TrustType.CONSTRUCAO)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // Enderman block pickup protection
        if (e.getEntity() instanceof Enderman) {
            Claim claim = plugin.getClaimManager().getClaimAt(e.getBlock().getLocation());
            if (claim != null) {
                e.setCancelled(true);
            }
        }
    }

    // --- B3.4: PROTEÇÃO CONTRA FIRE SPREAD ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        // Cancela queima de blocos dentro de claims
        Claim claim = plugin.getClaimManager().getClaimAt(e.getBlock().getLocation());
        if (claim != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        // Cancela apenas propagação de fogo para dentro de claims
        if (e.getNewState().getType() == Material.FIRE) {
            Claim claim = plugin.getClaimManager().getClaimAt(e.getBlock().getLocation());
            if (claim != null) {
                e.setCancelled(true);
            }
        }
    }

    // --- B3.5: PROTEÇÃO CONTRA FLUXO DE LAVA/ÁGUA ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        // Cancela fluxo de líquidos cruzando fronteiras de claims
        Claim sourceClaim = plugin.getClaimManager().getClaimAt(e.getBlock().getLocation());
        Claim destClaim = plugin.getClaimManager().getClaimAt(e.getToBlock().getLocation());

        // Se o bloco destino está em um claim e o bloco origem não está no MESMO claim
        if (destClaim != null && !isSameClaim(sourceClaim, destClaim)) {
            e.setCancelled(true);
        }
    }

    // --- ENTITY PROTECTION ---

    // B3.6: Suporte a projéteis — extrai o Player atacante (direto ou via projétil)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        // B3.6: Resolver o Player atacante — direto ou via projétil
        Player p = resolveAttacker(e.getDamager());
        if (p == null)
            return;

        // Proteção de PVE (Animais, Villagers, ArmorStands, ItemFrames)
        // Ignora monstros
        if (e.getEntity() instanceof Monster)
            return;

        // Se for Player, é PVP (Gerenciado pelo Claims PVP setting)
        if (e.getEntity() instanceof Player target) {
            // B7 — Diplomacia: PvP entre reinos aliados/inimigos
            Claim attackerKingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
            Claim targetKingdom = plugin.getKingdomManager().getKingdom(target.getUniqueId());

            if (attackerKingdom != null && targetKingdom != null
                    && !attackerKingdom.getId().equals(targetKingdom.getId())) {
                Relation rel = plugin.getKingdomManager()
                        .getRelation(attackerKingdom.getId(), targetKingdom.getId());

                if ((rel == Relation.ALLY || rel == Relation.TRUCE)
                        && plugin.getConfig().getBoolean("diplomacy.ally_pvp_disabled", true)) {
                    e.setCancelled(true);
                    plugin.getMessageManager().send(p, "diplomacy.pvp_ally_disabled");
                    return;
                }
                // B15 — Guerra: PvP SEMPRE liberado entre reinos em guerra
                if (rel == Relation.WAR) {
                    return;
                }
                if (rel == Relation.ENEMY
                        && plugin.getConfig().getBoolean("diplomacy.enemy_pvp_forced", true)) {
                    // PvP SEMPRE liberado entre inimigos — bypass do check de claim
                    return;
                }
            }

            // Check original de PvP por claim
            Claim claim = plugin.getClaimManager().getClaimAt(target.getLocation());
            if (claim != null && !claim.isPvp()) {
                // BUG-02 + BUG-05 FIX: Usar isSudito em vez de isResident,
                // e corrigir lógica invertida do PVP entre residentes
                boolean bothResidents;
                if (claim.isKingdom()) {
                    bothResidents = plugin.getKingdomManager().isSudito(claim.getId(), p.getUniqueId())
                            && plugin.getKingdomManager().isSudito(claim.getId(), target.getUniqueId());
                } else {
                    bothResidents = claim.isResident(p.getUniqueId())
                            && claim.isResident(target.getUniqueId());
                }

                if (bothResidents && claim.isResidentsPvp()) {
                    // PVP entre residentes está ATIVADO -> permitir
                    return;
                }

                // PVP global OFF -> bloqueia todos os demais casos
                e.setCancelled(true);
                p.sendMessage(bothResidents
                        ? "\u00a7cO PVP entre residentes est\u00e1 desativado aqui."
                        : "\u00a7cO PVP est\u00e1 desativado neste local.");
                return;
            }
            return; // Era Player -> sai do handler de Entity
        }

        // Entity Protection (Animals, Villagers, Hangings, ArmorStands)
        Claim.TrustType required = Claim.TrustType.CONSTRUCAO;

        // Se for ItemFrame ou ArmorStand, definitivamente é Construção
        if (e.getEntity() instanceof Hanging
                || e.getEntity() instanceof ArmorStand) {
            required = Claim.TrustType.CONSTRUCAO;
        } else if (e.getEntity() instanceof Animals
                || e.getEntity() instanceof Villager
                || e.getEntity() instanceof NPC) {
            // Matar animais/villagers -> Construção (Dano ao patrimônio)
            required = Claim.TrustType.CONSTRUCAO;
        } else {
            required = Claim.TrustType.CONSTRUCAO;
        }

        if (!canPerformAction(p, e.getEntity().getLocation(), required)) {
            e.setCancelled(true);
            sendDenyMessage(p, required);
        }
    }

    /**
     * B3.6: Resolve o Player atacante.
     * Se o damager for um Player, retorna diretamente.
     * Se for um Projectile cujo shooter é Player, retorna o shooter.
     * Caso contrário, retorna null.
     */
    private Player resolveAttacker(org.bukkit.entity.Entity damager) {
        // Nota: Usa FQN intencional para evitar conflito com Entity genérico
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (!canPerformAction(e.getPlayer(), e.getRightClicked().getLocation(), Claim.TrustType.CONSTRUCAO)) {
            e.setCancelled(true);
            sendDenyMessage(e.getPlayer(), Claim.TrustType.CONSTRUCAO);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        if (!(e.getRemover() instanceof Player))
            return; // Explosões tratadas em onEntityExplode

        Player p = (Player) e.getRemover();
        if (!canPerformAction(p, e.getEntity().getLocation(), Claim.TrustType.CONSTRUCAO)) {
            e.setCancelled(true);
            sendDenyMessage(p, Claim.TrustType.CONSTRUCAO);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        // Item Frames rotation, Animal breeding, Villager trading, Vehicle entry

        Claim.TrustType required = Claim.TrustType.ACESSO;

        if (e.getRightClicked() instanceof ItemFrame) {
            required = Claim.TrustType.CONSTRUCAO; // Rotacionar item frame
        } else if (e.getRightClicked() instanceof Villager) {
            required = Claim.TrustType.ACESSO; // Trade
        } else if (e.getRightClicked() instanceof Vehicle) {
            required = Claim.TrustType.ACESSO; // Entrar no barco/minecart
        } else {
            // Breeding animais, etc
            required = Claim.TrustType.ACESSO;
        }

        if (!canPerformAction(e.getPlayer(), e.getRightClicked().getLocation(), required)) {
            e.setCancelled(true);
            sendDenyMessage(e.getPlayer(), required);
        }
    }

    // --- B1.5: LIMITE DE REDSTONE POR CLAIM ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent e) {
        int limit = plugin.getConfig().getInt("protection.redstone_tick_limit", 100);
        if (limit <= 0)
            return; // Desativado

        Block block = e.getBlock();
        Claim claim = plugin.getClaimManager().getClaimAt(block.getLocation());
        if (claim == null)
            return; // Wilderness — sem limite

        // Gerar chave do chunk
        Chunk chunk = block.getChunk();
        String chunkKey = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();

        AtomicInteger counter = redstoneCounters.computeIfAbsent(chunkKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > limit) {
            // Desativar redstone neste tick — setar novo current para 0
            e.setNewCurrent(0);

            // Avisar dono do claim (apenas uma vez por ciclo)
            if (current == limit + 1) {
                UUID ownerId = claim.getOwner();
                Player owner = Bukkit.getPlayer(ownerId);
                if (owner != null && owner.isOnline()) {
                    plugin.getMessageManager().send(owner, "protection.redstone_limit");
                }
                plugin.getLogger().warning("[Proteção] Limite de redstone atingido no chunk " + chunkKey + " (claim "
                        + claim.getId() + ")");
            }
        }
    }

    // --- B1.6: ANTI-CHORUS FRUIT EM CLAIMS ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChorusTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT)
            return;
        if (!plugin.getConfig().getBoolean("protection.block_chorus_teleport", true))
            return;

        Location to = e.getTo();
        if (to == null)
            return;

        Player p = e.getPlayer();
        Claim claim = plugin.getClaimManager().getClaimAt(to);
        if (claim == null)
            return; // Wilderness — permitido

        // Se o jogador não tem permissão de acesso no claim destino, bloqueia
        if (!canPerformAction(p, to, Claim.TrustType.ACESSO)) {
            e.setCancelled(true);
            plugin.getMessageManager().send(p, "protection.chorus_blocked");
        }
    }

    // --- UTILS ---

    // Cooldown de mensagens de negação para evitar spam (2 segundos)
    private static final long DENY_COOLDOWN_MS = 2000L;
    private final Map<UUID, Long> denyCooldowns = new ConcurrentHashMap<>();

    private void sendDenyMessage(Player p, Claim.TrustType type) {
        long now = System.currentTimeMillis();
        Long lastDeny = denyCooldowns.get(p.getUniqueId());
        if (lastDeny != null && (now - lastDeny) < DENY_COOLDOWN_MS) {
            return; // Ainda em cooldown, não envia mensagem
        }
        denyCooldowns.put(p.getUniqueId(), now);

        var msg = plugin.getMessageManager();
        switch (type) {
            case CONSTRUCAO:
                msg.send(p, "protection.deny_build");
                break;
            case CONTEINER:
                msg.send(p, "protection.deny_container");
                break;
            case ACESSO:
                msg.send(p, "protection.deny_access");
                break;
            default:
                msg.send(p, "protection.deny_general");
                break;
        }
        msg.sendActionBar(p, "protection.deny_actionbar");
        // B1.3 — Feedback sonoro de negação
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }

}
