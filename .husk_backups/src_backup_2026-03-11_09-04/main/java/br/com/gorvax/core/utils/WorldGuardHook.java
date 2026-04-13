package br.com.gorvax.core.utils;

import br.com.gorvax.core.managers.Claim;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Hook isolado para WorldGuard para evitar NoClassDefFoundError no
 * ClaimManager.
 */
public class WorldGuardHook {

    public static boolean isRegionProtected(Claim claim) {
        try {
            World world = Bukkit.getWorld(claim.getWorldName());
            if (world == null)
                return false;

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            if (regions == null)
                return false;

            com.sk89q.worldedit.math.BlockVector3 min = com.sk89q.worldedit.math.BlockVector3.at(claim.getMinX(), 0,
                    claim.getMinZ());
            com.sk89q.worldedit.math.BlockVector3 max = com.sk89q.worldedit.math.BlockVector3.at(claim.getMaxX(), 255,
                    claim.getMaxZ());

            ProtectedRegion claimRegion = new ProtectedCuboidRegion("temp_claim_check", min, max);
            ApplicableRegionSet intersecting = regions.getApplicableRegions(claimRegion);

            for (ProtectedRegion existing : intersecting) {
                if (existing.getId().equals("__global__"))
                    continue;
                return true;
            }
        } catch (Throwable ignored) {
            // Se der erro de classe não encontrada ou qualquer outro, assumimos que não há
            // proteção
        }
        return false;
    }

    public static boolean isLocationProtected(Location loc) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));
            if (regions == null)
                return false;

            ApplicableRegionSet applicable = regions.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
            for (ProtectedRegion region : applicable) {
                if (region.getId().equals("__global__"))
                    continue;
                return true;
            }
            return false;
        } catch (Throwable ignored) {
        }
        return false;
    }
}
