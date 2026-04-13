package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class SelectionManager implements Listener {

    private final GorvaxCore plugin;
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();
    private final Map<UUID, Selection> subSelections = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public enum SelectionType {
        MAIN, SUB
    }

    public SelectionManager(GorvaxCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public Selection getSelection(UUID uuid, SelectionType type) {
        if (type == SelectionType.SUB) {
            return subSelections.computeIfAbsent(uuid, k -> new Selection());
        }
        return selections.computeIfAbsent(uuid, k -> new Selection());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stopVisualization(e.getPlayer().getUniqueId());
        selections.remove(e.getPlayer().getUniqueId());
        subSelections.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        cancelSelectionIfActive(e.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        // Only cancel if the player drops the selection tool
        Material dropped = e.getItemDrop().getItemStack().getType();
        if (dropped == Material.GOLDEN_SHOVEL || dropped == Material.IRON_SHOVEL) {
            cancelSelectionIfActive(e.getPlayer());
        }
    }

    private void cancelSelectionIfActive(Player p) {
        if (selections.containsKey(p.getUniqueId()) && selections.get(p.getUniqueId()).point1 != null) {
            Selection sel = selections.get(p.getUniqueId());
            sel.point1 = null;
            sel.point2 = null;
            stopVisualization(p.getUniqueId());
            plugin.getMessageManager().send(p, "selection.cancelled_tool");
            p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
        }
        // Check sub selection too? Usually users only do one.
        if (subSelections.containsKey(p.getUniqueId()) && subSelections.get(p.getUniqueId()).point1 != null) {
            Selection sel = subSelections.get(p.getUniqueId());
            sel.point1 = null;
            sel.point2 = null;
            stopVisualization(p.getUniqueId());
            p.sendMessage(plugin.getMessageManager().get("selection.cancelled_lot"));
            p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null)
            return;

        SelectionType type;
        if (item.getType() == Material.GOLDEN_SHOVEL) {
            type = SelectionType.MAIN;
        } else if (item.getType() == Material.IRON_SHOVEL) {
            type = SelectionType.SUB;
        } else {
            return; // Not a selection tool
        }

        Player p = e.getPlayer();
        Action action = e.getAction();

        // ... existing code ...
        if (action == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            Location loc = e.getClickedBlock().getLocation();
            Selection sel = getSelection(p.getUniqueId(), type);

            // CORNER DETECTION FOR RESIZE
            if (sel.point1 == null) {
                // Check if clicked near a corner of an owned claim
                Claim existing = plugin.getClaimManager().getClaimAt(loc);
                if (existing != null && existing.getOwner().equals(p.getUniqueId()) && type == SelectionType.MAIN) {
                    if (isCorner(existing, loc)) {
                        setupResize(p, sel, existing, loc);
                        return;
                    }
                }
                // Also check subplots if tool is Iron Shovel
                if (type == SelectionType.SUB) {
                    Claim parent = plugin.getClaimManager().getClaimAt(loc);
                    if (parent != null) {
                        SubPlot sub = parent.getSubPlotAt(loc);
                        if (sub != null) {
                            boolean isKingdomAuthority = parent.getOwner().equals(p.getUniqueId())
                                    || parent.hasPermission(p.getUniqueId(), Claim.TrustType.VICE);

                            if (isKingdomAuthority) {
                                if (isCorner(sub, loc)) {
                                    setupResize(p, sel, parent.getId(), sub, loc);
                                    return;
                                }
                            }
                        }
                    }
                }

                // START SELECTION (Normal)
                sel.point1 = loc;
                sel.isResize = false; // Reset
                sel.resizeClaimId = null;
                plugin.getMessageManager().send(p, "selection.point_a_set");
                p.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .deserialize(plugin.getMessageManager().get("selection.point_a_actionbar")));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

                // Start Dynamic Visualization (Stretching)
                startDynamicVisualization(p, sel, type);

            } else {
                // FINISH SELECTION
                sel.point2 = loc;

                ValidationResult result = validateSelection(p, sel, type, sel.point1, sel.point2);

                if (result == ValidationResult.VALID) {
                    int width = Math.abs(sel.point1.getBlockX() - sel.point2.getBlockX()) + 1;
                    int length = Math.abs(sel.point1.getBlockZ() - sel.point2.getBlockZ()) + 1;
                    int area = width * length;

                    p.sendMessage(plugin.getMessageManager().get("selection.area_selected", width, length, area));

                    if (sel.isResize) {
                        int diff = area - sel.originalArea;
                        String costMsg = diff > 0 ? "§cCusto: " + diff + " blocos"
                                : "§aReembolso: " + Math.abs(diff) + " blocos";
                        p.sendMessage(plugin.getMessageManager().get("selection.resize_cost", costMsg));
                    }

                    plugin.getMessageManager().send(p, "selection.confirm_hint");
                    plugin.getMessageManager().sendTitle(p, "selection.selection_valid_title",
                            "selection.selection_valid_subtitle", 5, 40, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                    // Only start static visualization if valid
                    startStaticVisualization(p, sel, type, true);
                } else {
                    // Invalid: Send Error Message
                    String errorMsg = "";
                    switch (result) {
                        case INVALID_SIZE:
                            int w = Math.abs(sel.point1.getBlockX() - sel.point2.getBlockX()) + 1;
                            int l = Math.abs(sel.point1.getBlockZ() - sel.point2.getBlockZ()) + 1;
                            errorMsg = plugin.getMessageManager().get("selection.error_size", w, l);
                            break;
                        case INVALID_FUNDS:
                            int area = (Math.abs(sel.point1.getBlockX() - sel.point2.getBlockX()) + 1) *
                                    (Math.abs(sel.point1.getBlockZ() - sel.point2.getBlockZ()) + 1);
                            int cost = area - (sel.isResize ? sel.originalArea : 0);
                            errorMsg = plugin.getMessageManager().get("selection.error_funds", cost);
                            break;
                        case OVERLAP:
                            errorMsg = plugin.getMessageManager().get("selection.error_overlap");
                            break;
                        case NOT_IN_CITY:
                            errorMsg = plugin.getMessageManager().get("selection.error_not_in_city");
                            break;
                    }

                    p.sendMessage(plugin.getMessageManager().get("general.error") + " " + errorMsg);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);

                    // Keep point2 null so selection continues (Dynamic)
                    sel.point2 = null;
                    // Do NOT stop dynamic visualization
                }
            }
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            if (e.getClickedBlock() != null) {
                Selection sel = getSelection(p.getUniqueId(), type);
                if (sel.point1 != null) {
                    cancelSelectionIfActive(p);
                }
            }
        }
    }

    private boolean isCorner(Claim claim, Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        // Check 4 corners
        return (x == claim.getMinX() && z == claim.getMinZ()) ||
                (x == claim.getMaxX() && z == claim.getMaxZ()) ||
                (x == claim.getMinX() && z == claim.getMaxZ()) ||
                (x == claim.getMaxX() && z == claim.getMinZ());
    }

    private boolean isCorner(SubPlot plot, Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return (x == plot.getMinX() && z == plot.getMinZ()) ||
                (x == plot.getMaxX() && z == plot.getMaxZ()) ||
                (x == plot.getMinX() && z == plot.getMaxZ()) ||
                (x == plot.getMaxX() && z == plot.getMinZ());
    }

    private void setupResize(Player p, Selection sel, Claim claim, Location cornerClicked) {
        sel.isResize = true;
        sel.resizeClaimId = claim.getId();
        sel.originalArea = claim.getArea();

        // Determine opposite corner (Fixed Point)
        int opX = (cornerClicked.getBlockX() == claim.getMinX()) ? claim.getMaxX() : claim.getMinX();
        int opZ = (cornerClicked.getBlockZ() == claim.getMinZ()) ? claim.getMaxZ() : claim.getMinZ();

        sel.point1 = new Location(p.getWorld(), opX, cornerClicked.getY(), opZ);

        plugin.getMessageManager().send(p, "selection.resize_mode");
        plugin.getMessageManager().send(p, "selection.resize_hint");
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        startDynamicVisualization(p, sel, SelectionType.MAIN);
    }

    private void setupResize(Player p, Selection sel, String parentId, SubPlot plot,
            Location cornerClicked) {
        sel.isResize = true;
        sel.resizeClaimId = parentId + ":" + plot.getId(); // Format CityID:PlotID
        sel.originalArea = (plot.getMaxX() - plot.getMinX() + 1) * (plot.getMaxZ() - plot.getMinZ() + 1);

        int opX = (cornerClicked.getBlockX() == plot.getMinX()) ? plot.getMaxX() : plot.getMinX();
        int opZ = (cornerClicked.getBlockZ() == plot.getMinZ()) ? plot.getMaxZ() : plot.getMinZ();

        sel.point1 = new Location(p.getWorld(), opX, cornerClicked.getY(), opZ);

        plugin.getMessageManager().send(p, "selection.resizing_lot");
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        startDynamicVisualization(p, sel, SelectionType.SUB);
    }

    // --- VISUALIZATION SYSTEMS (GHOST BLOCKS) ---

    private void stopVisualization(UUID uuid) {
        if (activeTasks.containsKey(uuid)) {
            activeTasks.get(uuid).cancel();
            activeTasks.remove(uuid);
        }
        // Clear ghost blocks immediately
        if (selections.containsKey(uuid)) {
            selections.get(uuid).clearGhostBlocks(plugin.getServer().getPlayer(uuid));
        }
        if (subSelections.containsKey(uuid)) {
            subSelections.get(uuid).clearGhostBlocks(plugin.getServer().getPlayer(uuid));
        }
    }

    private void startDynamicVisualization(Player p, Selection sel, SelectionType type) {
        stopVisualization(p.getUniqueId());

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || sel.point1 == null) {
                    this.cancel();
                    return;
                }

                // Target is player's current looking block or feet
                Location target = p.getTargetBlockExact(20) != null
                        ? p.getTargetBlockExact(20).getLocation()
                        : p.getLocation().getBlock().getLocation();

                // Validate PREEMPTIVELY
                ValidationResult result = validateSelection(p, sel, type, sel.point1, target);

                Material edgeMat;
                Material cornerMat;

                if (result != ValidationResult.VALID) {
                    edgeMat = Material.RED_WOOL;
                    cornerMat = Material.REDSTONE_BLOCK;
                } else {
                    edgeMat = (type == SelectionType.MAIN) ? Material.ORANGE_WOOL : Material.LIGHT_BLUE_WOOL;
                    cornerMat = (type == SelectionType.MAIN) ? Material.SHROOMLIGHT : Material.SEA_LANTERN;
                }

                sendGhostBlocks(p, sel, sel.point1, target, edgeMat, cornerMat);
            }
        }.runTaskTimer(plugin, 0L, 5L); // Every 5 ticks (0.25s)

        activeTasks.put(p.getUniqueId(), task);
    }

    private void startStaticVisualization(Player p, Selection sel, SelectionType type, boolean valid) {
        stopVisualization(p.getUniqueId());

        long duration = 15 * 20L; // 15 seconds

        // Colors based on validity and type
        Material edgeMat;
        Material cornerMat;

        if (valid) {
            // MAIN: Laranja (Orange) + Shroomlight
            // SUB: Azul Claro (Light Blue) + Sea Lantern
            edgeMat = (type == SelectionType.MAIN) ? Material.ORANGE_WOOL : Material.LIGHT_BLUE_WOOL;
            cornerMat = (type == SelectionType.MAIN) ? Material.SHROOMLIGHT : Material.SEA_LANTERN;
        } else {
            edgeMat = Material.RED_WOOL;
            cornerMat = Material.REDSTONE_BLOCK; // Não brilha muito, mas representa erro bem
        }

        BukkitTask task = new BukkitRunnable() {
            int time = 0;

            @Override
            public void run() {
                if (!p.isOnline() || sel.point1 == null || sel.point2 == null) {
                    this.cancel();
                    // Clear on cancel/finish
                    sel.clearGhostBlocks(p);
                    return;
                }
                if (time >= duration) {
                    this.cancel();
                    sel.clearGhostBlocks(p);
                    return;
                }

                // Re-send blocks occasionally to prevent flicker or block updates overriding
                // them
                if (time % 20 == 0) {
                    sendGhostBlocks(p, sel, sel.point1, sel.point2, edgeMat, cornerMat);
                }

                time += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every 1 second

        activeTasks.put(p.getUniqueId(), task);
    }

    private void sendGhostBlocks(Player p, Selection sel, Location p1, Location p2, Material edgeMat,
            Material cornerMat) {
        // 1. Clear previous blocks first to avoid trails
        sel.clearGhostBlocks(p);

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        World world = p.getWorld();
        BlockData edgeData = edgeMat.createBlockData();
        BlockData cornerData = cornerMat.createBlockData();

        // Helper to add block
        BiConsumer<Integer, Integer> addBlock = (x, z) -> {
            int y = world.getHighestBlockYAt(x, z);
            // Ensure we don't bury it if player is in a cave?
            // The requirement says "colado ao chão", "no bloco mais alto".
            // getHighestBlockYAt returns the Y of the highest non-air block.
            // We want to replace that block or place above it?
            // "Ghost block pode substituir o lugar dos pisos mais altos" -> replace the top
            // block.

            Location loc = new Location(world, x, y, z);

            // Determine if corner
            boolean isCorner = (x == minX && z == minZ) || (x == maxX && z == maxZ) ||
                    (x == minX && z == maxZ) || (x == maxX && z == minZ);

            p.sendBlockChange(loc, isCorner ? cornerData : edgeData);
            sel.activeGhostBlocks.add(loc);
        };

        // Density based on size
        int step = (maxX - minX + maxZ - minZ) > 200 ? 2 : 1;

        // Draw edges
        for (int x = minX; x <= maxX; x += step) {
            addBlock.accept(x, minZ);
            addBlock.accept(x, maxZ);
        }
        for (int z = minZ; z <= maxZ; z += step) {
            addBlock.accept(minX, z);
            addBlock.accept(maxX, z);
        }

        // Ensure corners are always drawn even with step
        addBlock.accept(minX, minZ);
        addBlock.accept(maxX, maxZ);
        addBlock.accept(minX, maxZ);
        addBlock.accept(maxX, minZ);
    }

    public static class Selection {
        public Location point1;
        public Location point2;

        // Resize Fields
        public boolean isResize = false;
        public String resizeClaimId = null;
        public int originalArea = 0;

        // Ghost Blocks Tracking
        public List<Location> activeGhostBlocks = new ArrayList<>();

        public int getArea() {
            return getWidth() * getLength();
        }

        public int getWidth() {
            if (point1 == null || point2 == null)
                return 0;
            return Math.abs(point1.getBlockX() - point2.getBlockX()) + 1;
        }

        public int getLength() {
            if (point1 == null || point2 == null)
                return 0;
            return Math.abs(point1.getBlockZ() - point2.getBlockZ()) + 1;
        }

        public boolean isComplete() {
            return point1 != null && point2 != null;
        }

        public void clearGhostBlocks(Player p) {
            if (p == null || !p.isOnline())
                return;

            for (Location loc : activeGhostBlocks) {
                // Revert to original block
                p.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
            activeGhostBlocks.clear();
        }
    }

    private boolean isSubPlotOverlapping(Claim parent, int minX, int minZ, int maxX, int maxZ, String ignoreDetails) {
        String ignoreId = null;
        if (ignoreDetails != null && ignoreDetails.contains(":")) {
            ignoreId = ignoreDetails.split(":")[1];
        }

        for (SubPlot plot : parent.getSubPlots()) {
            if (ignoreId != null && plot.getId().equals(ignoreId))
                continue;

            // AABB Collision
            if (maxX >= plot.getMinX() && minX <= plot.getMaxX() &&
                    maxZ >= plot.getMinZ() && minZ <= plot.getMaxZ()) {
                return true;
            }
        }
        return false;
    }

    public enum ValidationResult {
        VALID,
        INVALID_SIZE,
        INVALID_FUNDS,
        OVERLAP,
        NOT_IN_CITY
    }

    private ValidationResult validateSelection(Player p, Selection sel, SelectionType type, Location p1, Location p2) {
        if (p1 == null || p2 == null)
            return ValidationResult.VALID;

        if (type == SelectionType.MAIN) {
            int width = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
            int length = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;

            if (width < 10 || length < 10) {
                return ValidationResult.INVALID_SIZE;
            }

            int area = width * length;
            if (!sel.isResize || area > sel.originalArea) {
                int cost = area - (sel.isResize ? sel.originalArea : 0);
                if (cost > 0) {
                    PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());
                    if (pd.getClaimBlocks() < cost) {
                        return ValidationResult.INVALID_FUNDS;
                    }
                }
            }
        }

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        if (type == SelectionType.MAIN) {
            if (plugin.getClaimManager().isOverlapping(p.getWorld().getName(), minX, minZ, maxX, maxZ,
                    sel.resizeClaimId)) {
                return ValidationResult.OVERLAP;
            }
        } else if (type == SelectionType.SUB) {
            // Busca o pai baseado no ponto 1 (que deve estar dentro da cidade)
            Claim parent = plugin.getClaimManager().getClaimAt(p1);
            if (parent != null) {
                // Durante expansão (isResize), o ignoreClaimId contém o ID do subplot
                if (isSubPlotOverlapping(parent, minX, minZ, maxX, maxZ, sel.resizeClaimId)) {
                    return ValidationResult.OVERLAP;
                }

                // Verifica se os novos limites estão totalmente dentro do Reino pai
                if (!parent.contains(new Location(p.getWorld(), minX, p1.getY(), minZ)) ||
                        !parent.contains(new Location(p.getWorld(), maxX, p1.getY(), maxZ))) {
                    return ValidationResult.NOT_IN_CITY;
                }
            } else {
                return ValidationResult.NOT_IN_CITY;
            }
        }

        return ValidationResult.VALID;
    }
}
