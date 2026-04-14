package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gerencia o comando /confirmar (/c).
 * Confirma criação ou redimensionamento de claims e subplots.
 * Extraído do antigo ClaimCommand no Batch B9.
 */
public class ConfirmCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public ConfirmCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;
        handleConfirmation(p);
        return true;
    }

    private void handleConfirmation(Player p) {
        SelectionManager.Selection subSel = plugin.getSelectionManager().getSelection(p.getUniqueId(),
                SelectionManager.SelectionType.SUB);
        SelectionManager.Selection mainSel = plugin.getSelectionManager().getSelection(p.getUniqueId(),
                SelectionManager.SelectionType.MAIN);

        if (subSel != null && subSel.isComplete()) {
            if (subSel.isResize) {
                executeSubPlotResize(p, subSel);
            } else {
                executeSubPlotCreation(p, subSel);
            }
            subSel.point1 = null;
            subSel.point2 = null;
            return;
        }

        if (mainSel != null && mainSel.isComplete()) {
            if (mainSel.isResize) {
                executeMainClaimResize(p, mainSel);
            } else {
                executeMainClaimCreation(p, mainSel);
            }
            mainSel.point1 = null;
            mainSel.point2 = null;
            return;
        }

        var msg = plugin.getMessageManager();
        msg.send(p, "confirm.no_pending");
    }

    private void executeMainClaimCreation(Player p, SelectionManager.Selection sel) {
        var msg = plugin.getMessageManager();
        int area = sel.getArea();
        int width = sel.getWidth();
        int length = sel.getLength();

        if (width < 10 || length < 10) {
            msg.send(p, "confirm.error_too_small");
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        // --- SEC-03: Max Size Enforcement ---
        int maxArea = plugin.getConfig().getInt("claims.max_area", 30000);
        if (area > maxArea) {
            msg.send(p, "confirm.error_max_area", maxArea, area);
            return;
        }

        PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());

        // B13 — Verificar se é criação de Outpost
        boolean isOutpost = pd.isNextClaimIsOutpost();
        String parentKingdomId = pd.getOutpostParentKingdomId();

        if (isOutpost && parentKingdomId != null) {
            // Fluxo de criação de Outpost
            pd.setNextClaimIsOutpost(false);
            pd.setOutpostParentKingdomId(null);

            // Aplicar multiplicador de custo
            double costMultiplier = plugin.getConfig().getDouble("outposts.cost_multiplier", 3.0);
            int requiredBlocks = (int) Math.ceil(area * costMultiplier);

            if (pd.getClaimBlocks() < requiredBlocks) {
                msg.send(p, "confirm.error_insufficient_blocks", requiredBlocks, pd.getClaimBlocks());
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                return;
            }

            String id = UUID.randomUUID().toString();
            Claim newClaim = new Claim(id, p.getUniqueId(), sel.point1, sel.point2);
            newClaim.setType(Claim.Type.OUTPOST);
            newClaim.setParentKingdomId(parentKingdomId);

            // Herdar nome do reino
            Claim parentClaim = plugin.getClaimManager().getClaimById(parentKingdomId);
            if (parentClaim != null && parentClaim.getKingdomName() != null) {
                newClaim.setKingdomName(parentClaim.getKingdomName());
            }

            if (plugin.getClaimManager().createClaim(newClaim)) {
                pd.removeClaimBlocks(requiredBlocks);
                plugin.getPlayerDataManager().saveData(p.getUniqueId());

                msg.send(p, "outpost.create_success");
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                msg.sendTitle(p, "outpost.create_title", "outpost.create_subtitle", 10, 70, 20);
                p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 20, 0.5, 1, 0.5, 0.1);
            } else {
                msg.send(p, "confirm.error_overlap");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            return;
        }

        // Fluxo normal de criação de claim/reino
        if (plugin.getKingdomManager().hasActiveKingdom(p.getUniqueId())) {
            msg.send(p, "confirm.error_already_has_kingdom");
            return;
        }

        if (pd.getClaimBlocks() < area) {
            msg.send(p, "confirm.error_insufficient_blocks", area, pd.getClaimBlocks());
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }

        String id = UUID.randomUUID().toString();
        Claim newClaim = new Claim(id, p.getUniqueId(), sel.point1, sel.point2);

        if (plugin.getClaimManager().createClaim(newClaim)) {
            pd.removeClaimBlocks(area);
            plugin.getPlayerDataManager().saveData(p.getUniqueId());

            // Registra no KingdomManager como "semi" reino (sem nome ainda)
            plugin.getKingdomManager().setRei(id, p.getUniqueId());

            // B19 — Evento customizado: KingdomCreateEvent
            org.bukkit.Bukkit.getPluginManager().callEvent(
                    new br.com.gorvax.core.events.KingdomCreateEvent(p, id, ""));

            msg.send(p, "confirm.claim_success");
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            msg.sendTitle(p, "confirm.claim_success_title", "confirm.claim_success_subtitle", 10, 70, 20);
            p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 20, 0.5, 1, 0.5, 0.1);
        } else {
            msg.send(p, "confirm.error_overlap");
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void executeMainClaimResize(Player p, SelectionManager.Selection sel) {
        var msg = plugin.getMessageManager();
        Claim claim = plugin.getClaimManager().getClaimById(sel.resizeClaimId);
        if (claim == null) {
            msg.send(p, "confirm.error_claim_not_found");
            return;
        }

        int newArea = sel.getArea();
        int width = sel.getWidth();
        int length = sel.getLength();

        // 1. Min Size
        if (width < 10 || length < 10) {
            msg.send(p, "confirm.error_too_small");
            return;
        }

        // --- SEC-03: Max Size Enforcement (Resize) ---
        int maxAreaLimit = plugin.getConfig().getInt("claims.max_area", 30000);
        if (newArea > maxAreaLimit) {
            msg.send(p, "confirm.error_max_area", maxAreaLimit, newArea);
            return;
        }

        // 2. Cost Calculation
        int diff = newArea - sel.originalArea;
        PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());

        if (diff > 0) {
            if (pd.getClaimBlocks() < diff) {
                msg.send(p, "confirm.error_insufficient_blocks", diff, pd.getClaimBlocks());
                return;
            }
            pd.removeClaimBlocks(diff);
            msg.send(p, "confirm.resize_cost", diff, pd.getClaimBlocks());
        } else if (diff < 0) {
            int refund = Math.abs(diff);
            pd.addClaimBlocks(refund);
            msg.send(p, "confirm.resize_refund", refund);
        }
        plugin.getPlayerDataManager().saveData(p.getUniqueId());

        // 3. Update Claim
        int minX = Math.min(sel.point1.getBlockX(), sel.point2.getBlockX());
        int maxX = Math.max(sel.point1.getBlockX(), sel.point2.getBlockX());
        int minZ = Math.min(sel.point1.getBlockZ(), sel.point2.getBlockZ());
        int maxZ = Math.max(sel.point1.getBlockZ(), sel.point2.getBlockZ());

        // Check Overlap & Protections
        if (plugin.getClaimManager().isRestrictedWorld(p.getWorld().getName()) ||
                plugin.getClaimManager().isSpawnProtected(sel.point1) ||
                plugin.getClaimManager().isRegionProtected(p.getWorld().getName(), minX, minZ, maxX, maxZ)) {

            msg.send(p, "confirm.error_protected_area");
            if (diff > 0)
                pd.addClaimBlocks(diff);
            else if (diff < 0)
                pd.removeClaimBlocks(Math.abs(diff));
            return;
        }

        // Check Overlap (Ignoring itself)
        if (plugin.getClaimManager().isOverlapping(claim.getWorldName(), minX, minZ, maxX, maxZ, claim.getId())) {
            msg.send(p, "confirm.error_overlap");
            if (diff > 0)
                pd.addClaimBlocks(diff);
            else if (diff < 0)
                pd.removeClaimBlocks(Math.abs(diff));
            plugin.getPlayerDataManager().saveData(p.getUniqueId());
            return;
        }

        claim.setMinX(minX);
        claim.setMinZ(minZ);
        claim.setMaxX(maxX);
        claim.setMaxZ(maxZ);

        plugin.getClaimManager().saveClaims();
        msg.send(p, "confirm.resize_success");
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        p.spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 20, 0.5, 1, 0.5, 0.1);
    }

    private void executeSubPlotResize(Player p, SelectionManager.Selection sel) {
        var msg = plugin.getMessageManager();
        String[] parts = sel.resizeClaimId.split(":");
        if (parts.length != 2)
            return;

        String kingdomId = parts[0];
        String plotId = parts[1];

        Claim parent = plugin.getClaimManager().getClaimById(kingdomId);
        if (parent == null)
            return;

        SubPlot plot = null;
        for (SubPlot s : parent.getSubPlots()) {
            if (s.getId().equals(plotId)) {
                plot = s;
                break;
            }
        }

        if (plot == null) {
            msg.send(p, "confirm.error_subplot_not_found");
            return;
        }

        int minX = Math.min(sel.point1.getBlockX(), sel.point2.getBlockX());
        int maxX = Math.max(sel.point1.getBlockX(), sel.point2.getBlockX());
        int minZ = Math.min(sel.point1.getBlockZ(), sel.point2.getBlockZ());
        int maxZ = Math.max(sel.point1.getBlockZ(), sel.point2.getBlockZ());

        // 1. Verify if inside Kingdom
        Location t1 = new Location(p.getWorld(), minX, 64, minZ);
        Location t2 = new Location(p.getWorld(), maxX, 64, maxZ);

        if (!parent.contains(t1) || !parent.contains(t2)) {
            msg.send(p, "confirm.error_not_in_kingdom");
            return;
        }

        // 2. Verify Overlap with other Plots
        for (SubPlot other : parent.getSubPlots()) {
            if (other.getId().equals(plot.getId()))
                continue;
            if (maxX >= other.getMinX() && minX <= other.getMaxX() &&
                    maxZ >= other.getMinZ() && minZ <= other.getMaxZ()) {
                msg.send(p, "confirm.error_overlap");
                return;
            }
        }

        plot.setMinX(minX);
        plot.setMinZ(minZ);
        plot.setMaxX(maxX);
        plot.setMaxZ(maxZ);

        plugin.getClaimManager().saveClaims();
        msg.send(p, "confirm.subplot_resize_success");
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1f);
    }

    private void executeSubPlotCreation(Player p, SelectionManager.Selection sel) {
        var msg = plugin.getMessageManager();
        Claim parent = plugin.getClaimManager().getClaimAt(p.getLocation());
        if (parent == null) {
            msg.send(p, "confirm.error_not_in_kingdom");
            return;
        }

        if (!parent.getOwner().equals(p.getUniqueId())
                && !parent.hasPermission(p.getUniqueId(), Claim.TrustType.VICE)) {
            msg.send(p, "confirm.error_no_permission_subplot");
            return;
        }

        if (!plugin.getPlayerDataManager().getData(p.getUniqueId()).hasKingRank()) {
            msg.send(p, "confirm.error_needs_king_rank");
            return;
        }

        if (parent.getKingdomName() == null || !parent.isKingdom()) {
            msg.send(p, "confirm.error_needs_kingdom_name");
            return;
        }

        if (!parent.contains(sel.point1) || !parent.contains(sel.point2)) {
            msg.send(p, "confirm.error_not_in_kingdom");
            return;
        }

        String id = UUID.randomUUID().toString();
        String nomeLote = "Feudo " + (parent.getSubPlots().size() + 1);
        SubPlot plot = new SubPlot(id, nomeLote, sel.point1, sel.point2);

        parent.addSubPlot(plot);
        plugin.getClaimManager().saveClaims();

        // Vincula no KingdomManager para aparecer em menus e ter ID curto
        plugin.getKingdomManager().vincularLoteAoReino(parent.getId(), id);
        int num = plugin.getKingdomManager().atribuirNumeroLote(parent.getId(), id);

        msg.send(p, "confirm.subplot_success", num);
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // /confirmar não possui tab completion
        return new ArrayList<>();
    }
}
