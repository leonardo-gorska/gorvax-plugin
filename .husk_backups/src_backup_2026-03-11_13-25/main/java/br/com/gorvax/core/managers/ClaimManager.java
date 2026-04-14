package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Location;
// Unused imports removed
import br.com.gorvax.core.utils.WorldGuardHook;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClaimManager {

    private final GorvaxCore plugin;
    private final List<Claim> claims = new CopyOnWriteArrayList<>();
    private final Map<String, Map<Long, List<Claim>>> chunkClaims = new ConcurrentHashMap<>(); // Cache espacial
    private final Map<String, Claim> idMap = new ConcurrentHashMap<>(); // Cache de ID O(1)
    private final AtomicBoolean saving = new AtomicBoolean(false);

    public ClaimManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadClaims();
    }

    public void reload() {
        claims.clear();
        chunkClaims.clear();
        idMap.clear();
        loadClaims();
    }

    // --- STORAGE ---
    private void loadClaims() {
        // Simple YAML Storage for now (can migrate to SQL later)
        File file = new File(plugin.getDataFolder(), "claims.yml");
        if (!file.exists())
            return;

        YamlConfiguration config = YamlConfiguration
                .loadConfiguration(file);
        if (config.contains("claims")) {
            for (String id : config.getConfigurationSection("claims").getKeys(false)) {
                try {
                    String ownerStr = config.getString("claims." + id + ".owner");
                    if (ownerStr == null || ownerStr.isEmpty()) {
                        plugin.getLogger().warning("Claim " + id + " tem owner null/vazio. Pulando...");
                        continue;
                    }

                    UUID owner;
                    try {
                        owner = UUID.fromString(ownerStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().severe("UUID inválido no claim " + id + ": " + ownerStr + ". Pulando...");
                        continue;
                    }

                    String world = config.getString("claims." + id + ".world");
                    int minX = config.getInt("claims." + id + ".minX");
                    int minZ = config.getInt("claims." + id + ".minZ");
                    int maxX = config.getInt("claims." + id + ".maxX");
                    int maxZ = config.getInt("claims." + id + ".maxZ");
                    boolean isCity = config.getBoolean("claims." + id + ".isCity");
                    String name = config.getString("claims." + id + ".name");

                    Claim claim = new Claim(id, owner, world, minX, minZ, maxX, maxZ, isCity, name);
                    updateChunkCache(claim, true);
                    idMap.put(id, claim); // Cache ID

                    if (config.contains("claims." + id + ".cityName")) {
                        claim.setKingdomName(config.getString("claims." + id + ".cityName"));
                    }
                    claim.setWelcomeColor(config.getString("claims." + id + ".welcomeColor", "§b"));
                    claim.setChatColor(config.getString("claims." + id + ".chatColor", "§f"));
                    claim.setTagColor(config.getString("claims." + id + ".tagColor", "§e"));
                    claim.setTag(config.getString("claims." + id + ".tag", null));
                    claim.setTax(config.getDouble("claims." + id + ".tax", 5.0));

                    // Load Messages
                    claim.setEnterTitle(config.getString("claims." + id + ".msg.enterTitle", null));
                    claim.setEnterSubtitle(config.getString("claims." + id + ".msg.enterSubtitle", null));
                    claim.setExitTitle(config.getString("claims." + id + ".msg.exitTitle", null));
                    claim.setExitSubtitle(config.getString("claims." + id + ".msg.exitSubtitle", null));

                    // Load New Fields
                    if (config.contains("claims." + id + ".type")) {
                        try {
                            claim.setType(Claim.Type.valueOf(config.getString("claims." + id + ".type")));
                        } catch (Exception e) {
                        }
                    }
                    claim.setPvp(config.getBoolean("claims." + id + ".pvp", false));
                    claim.setResidentsPvp(config.getBoolean("claims." + id + ".residentsPvp", false));
                    claim.setResidentsPvpOutside(config.getBoolean("claims." + id + ".residentsPvpOutside", false));
                    claim.setPublic(config.getBoolean("claims." + id + ".isPublic", true));

                    // B13 — Outpost parent link
                    if (config.contains("claims." + id + ".parentKingdomId")) {
                        claim.setParentKingdomId(config.getString("claims." + id + ".parentKingdomId"));
                    }

                    // B36 — Upkeep debt days (claims pessoais)
                    claim.setUpkeepDebtDays(config.getInt("claims." + id + ".upkeepDebtDays", 0));

                    // Residents Permissions
                    if (config.contains("claims." + id + ".residentsBuild"))
                        claim.setResidentsBuild(config.getBoolean("claims." + id + ".residentsBuild"));
                    if (config.contains("claims." + id + ".residentsContainer"))
                        claim.setResidentsContainer(config.getBoolean("claims." + id + ".residentsContainer"));
                    if (config.contains("claims." + id + ".residentsSwitch"))
                        claim.setResidentsSwitch(config.getBoolean("claims." + id + ".residentsSwitch"));

                    // Load Trusts
                    if (config.contains("claims." + id + ".trusts")) {
                        for (String puuid : config.getConfigurationSection("claims." + id + ".trusts").getKeys(false)) {
                            Object trustData = config.get("claims." + id + ".trusts." + puuid);
                            UUID uuid;
                            try {
                                uuid = UUID.fromString(puuid);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning(
                                        "UUID inválido em trusts do claim " + id + ": " + puuid + ". Pulando...");
                                continue;
                            }

                            if (trustData instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> types = (List<String>) trustData;
                                for (String t : types) {
                                    try {
                                        claim.addTrust(uuid, Claim.TrustType.valueOf(t));
                                    } catch (IllegalArgumentException e) {
                                        // Migration for LIST format
                                        if (t.equals("GERAL"))
                                            claim.addTrust(uuid, Claim.TrustType.CONSTRUCAO);
                                    } catch (Exception e) {
                                    }
                                }
                            } else if (trustData instanceof String) {
                                // MIGRATION logic
                                String typeStr = (String) trustData;
                                Claim.TrustType type = null;
                                try {
                                    type = Claim.TrustType.valueOf(typeStr);
                                } catch (IllegalArgumentException ex) {
                                    if (typeStr.equals("BUILD") || typeStr.equals("GERAL"))
                                        type = Claim.TrustType.CONSTRUCAO;
                                    else if (typeStr.equals("ACCESS"))
                                        type = Claim.TrustType.ACESSO;
                                    else if (typeStr.equals("CONTAINER"))
                                        type = Claim.TrustType.CONTEINER;
                                    else if (typeStr.equals("MANAGER"))
                                        type = Claim.TrustType.VICE;
                                }

                                if (type != null) {
                                    if (type == Claim.TrustType.VICE) {
                                        claim.addTrust(uuid, Claim.TrustType.VICE);
                                    } else if (type == Claim.TrustType.GERAL) {
                                        claim.addTrust(uuid, Claim.TrustType.GERAL);
                                    } else if (type == Claim.TrustType.CONSTRUCAO) {
                                        claim.addTrust(uuid, Claim.TrustType.CONSTRUCAO);
                                        claim.addTrust(uuid, Claim.TrustType.CONTEINER);
                                        claim.addTrust(uuid, Claim.TrustType.ACESSO);
                                    } else if (type == Claim.TrustType.CONTEINER) {
                                        claim.addTrust(uuid, Claim.TrustType.CONTEINER);
                                        claim.addTrust(uuid, Claim.TrustType.ACESSO);
                                    } else if (type == Claim.TrustType.ACESSO) {
                                        claim.addTrust(uuid, Claim.TrustType.ACESSO);
                                    }
                                }
                            }
                        }
                    }

                    // Load Sub-plots
                    if (config.contains("claims." + id + ".subplots")) {
                        for (String sid : config.getConfigurationSection("claims." + id + ".subplots").getKeys(false)) {
                            String p = "claims." + id + ".subplots." + sid;
                            String sname = config.getString(p + ".name");
                            int sminX = config.getInt(p + ".minX");
                            int sminZ = config.getInt(p + ".minZ");
                            int smaxX = config.getInt(p + ".maxX");
                            int smaxZ = config.getInt(p + ".maxZ");
                            double sprice = config.getDouble(p + ".price");
                            double srentPrice = config.getDouble(p + ".rentPrice");
                            boolean sforSale = config.getBoolean(p + ".forSale");
                            boolean sforRent = config.getBoolean(p + ".forRent");

                            UUID sowner = null;
                            String subplotOwnerStr = config.getString(p + ".owner");
                            if (subplotOwnerStr != null && !subplotOwnerStr.isEmpty()) {
                                try {
                                    sowner = UUID.fromString(subplotOwnerStr);
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("UUID inválido em subplot owner: " + subplotOwnerStr);
                                }
                            }

                            UUID srenter = null;
                            String renterStr = config.getString(p + ".renter");
                            if (renterStr != null && !renterStr.isEmpty()) {
                                try {
                                    srenter = UUID.fromString(renterStr);
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("UUID inválido em subplot renter: " + renterStr);
                                }
                            }

                            long srentExpire = config.getLong(p + ".rentExpire");

                            SubPlot plot = new SubPlot(sid, sname, sminX, sminZ, smaxX, smaxZ, sprice, srentPrice,
                                    sforSale, sforRent, sowner, srenter, srentExpire);

                            // Load SubPlot Trusts
                            if (config.contains(p + ".trusts")) {
                                for (String tUuid : config.getConfigurationSection(p + ".trusts").getKeys(false)) {
                                    Object tData = config.get(p + ".trusts." + tUuid);
                                    UUID uuid;
                                    try {
                                        uuid = UUID.fromString(tUuid);
                                    } catch (IllegalArgumentException e) {
                                        plugin.getLogger().warning("UUID inválido em subplot trusts: " + tUuid);
                                        continue;
                                    }
                                    if (tData instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<String> types = (List<String>) tData;
                                        for (String t : types) {
                                            try {
                                                plot.addTrust(uuid, Claim.TrustType.valueOf(t));
                                            } catch (IllegalArgumentException e) {
                                                if (t.equals("GERAL"))
                                                    plot.addTrust(uuid, Claim.TrustType.CONSTRUCAO);
                                            } catch (Exception e) {
                                            }
                                        }
                                    } else if (tData instanceof String) {
                                        // Migration
                                        try {
                                            Claim.TrustType type = Claim.TrustType.valueOf((String) tData);
                                            if (type == Claim.TrustType.VICE) {
                                                plot.addTrust(uuid, Claim.TrustType.VICE);
                                            } else if (type == Claim.TrustType.GERAL) {
                                                plot.addTrust(uuid, Claim.TrustType.GERAL);
                                            } else if (type == Claim.TrustType.CONSTRUCAO) {
                                                plot.addTrust(uuid, Claim.TrustType.CONSTRUCAO);
                                                plot.addTrust(uuid, Claim.TrustType.CONTEINER);
                                                plot.addTrust(uuid, Claim.TrustType.ACESSO);
                                            } else if (type == Claim.TrustType.CONTEINER) {
                                                plot.addTrust(uuid, Claim.TrustType.CONTEINER);
                                                plot.addTrust(uuid, Claim.TrustType.ACESSO);
                                            } else if (type == Claim.TrustType.ACESSO) {
                                                plot.addTrust(uuid, Claim.TrustType.ACESSO);
                                            }
                                        } catch (Exception e) {
                                        }
                                    }
                                }
                            }

                            claim.addSubPlot(plot);
                        }
                    }

                    claims.add(claim);
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao carregar claim " + id + ": " + e.getMessage());
                }
            }
        }
    }

    public void saveClaims() {
        saveClaimsAsync();
    }

    public void saveClaimsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    public synchronized void saveSync() {
        if (saving.get())
            return;
        saving.set(true);

        try {
            File file = new File(plugin.getDataFolder(), "claims.yml");
            File tmpFile = new File(plugin.getDataFolder(), "claims.yml.tmp");

            YamlConfiguration config = new YamlConfiguration();

            List<Claim> snapshot;
            synchronized (claims) {
                snapshot = new ArrayList<>(claims);
            }

            for (Claim c : snapshot) {
                String path = "claims." + c.getId();
                config.set(path + ".owner", c.getOwner().toString());
                config.set(path + ".world", c.getWorldName());
                config.set(path + ".minX", c.getMinX());
                config.set(path + ".minZ", c.getMinZ());
                config.set(path + ".maxX", c.getMaxX());
                config.set(path + ".maxZ", c.getMaxZ());
                config.set(path + ".isCity", c.isKingdom());
                config.set(path + ".name", c.getName());
                if (c.isKingdom()) {
                    if (c.getKingdomName() != null) {
                        config.set(path + ".cityName", c.getKingdomName());
                    }
                }

                config.set(path + ".type", c.getType().name());
                config.set(path + ".pvp", c.isPvp());
                config.set(path + ".residentsPvp", c.isResidentsPvp());
                config.set(path + ".residentsPvpOutside", c.isResidentsPvpOutside());
                config.set(path + ".isPublic", c.isPublic());
                config.set(path + ".residentsBuild", c.isResidentsBuild());
                config.set(path + ".residentsContainer", c.isResidentsContainer());
                config.set(path + ".residentsSwitch", c.isResidentsSwitch());

                // B36 — Upkeep debt days (claims pessoais)
                if (c.getUpkeepDebtDays() > 0) {
                    config.set(path + ".upkeepDebtDays", c.getUpkeepDebtDays());
                }

                // B13 — Outpost parent link
                if (c.isOutpost() && c.getParentKingdomId() != null) {
                    config.set(path + ".parentKingdomId", c.getParentKingdomId());
                }

                config.set(path + ".welcomeColor", c.getWelcomeColor());
                config.set(path + ".chatColor", c.getChatColor());
                config.set(path + ".tagColor", c.getTagColor());
                config.set(path + ".tag", c.getTag());
                config.set(path + ".tax", c.getTax());

                config.set(path + ".msg.enterTitle", c.getEnterTitle());
                config.set(path + ".msg.enterSubtitle", c.getEnterSubtitle());
                config.set(path + ".msg.exitTitle", c.getExitTitle());
                config.set(path + ".msg.exitSubtitle", c.getExitSubtitle());

                for (Map.Entry<UUID, Set<Claim.TrustType>> entry : c.getTrustedPlayers().entrySet()) {
                    List<String> perms = new ArrayList<>();
                    for (Claim.TrustType t : entry.getValue())
                        perms.add(t.name());
                    config.set(path + ".trusts." + entry.getKey().toString(), perms);
                }

                for (SubPlot plot : c.getSubPlots()) {
                    String sp = path + ".subplots." + plot.getId();
                    config.set(sp + ".name", plot.getName());
                    config.set(sp + ".minX", plot.getMinX());
                    config.set(sp + ".minZ", plot.getMinZ());
                    config.set(sp + ".maxX", plot.getMaxX());
                    config.set(sp + ".maxZ", plot.getMaxZ());
                    config.set(sp + ".price", plot.getPrice());
                    config.set(sp + ".rentPrice", plot.getRentPrice());
                    config.set(sp + ".forSale", plot.isForSale());
                    config.set(sp + ".forRent", plot.isForRent());
                    config.set(sp + ".owner", plot.getOwner() != null ? plot.getOwner().toString() : null);
                    config.set(sp + ".renter", plot.getRenter() != null ? plot.getRenter().toString() : null);
                    config.set(sp + ".rentExpire", plot.getRentExpire());

                    if (!plot.getTrustedPlayers().isEmpty()) {
                        for (Map.Entry<UUID, Set<Claim.TrustType>> entry : plot.getTrustedPlayers().entrySet()) {
                            List<String> perms = new ArrayList<>();
                            for (Claim.TrustType t : entry.getValue())
                                perms.add(t.name());
                            config.set(sp + ".trusts." + entry.getKey().toString(), perms);
                        }
                    }
                }
            }

            config.save(tmpFile);

            // Atomic Move
            try {
                Files.move(tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erro critico ao salvar claims.yml", e);
        } finally {
            saving.set(false);
        }
    }

    public boolean isOverlapping(Claim newClaim) {
        return isOverlapping(newClaim.getWorldName(), newClaim.getMinX(), newClaim.getMinZ(), newClaim.getMaxX(),
                newClaim.getMaxZ());
    }

    public boolean isOverlapping(String world, int minX, int minZ, int maxX, int maxZ) {
        return isOverlapping(world, minX, minZ, maxX, maxZ, null);
    }

    public boolean isOverlapping(String world, int minX, int minZ, int maxX, int maxZ, String ignoreClaimId) {
        Map<Long, List<Claim>> worldMap = chunkClaims.get(world);
        if (worldMap == null)
            return false;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                List<Claim> inChunk = worldMap.get(chunkKey);
                if (inChunk == null)
                    continue;

                for (Claim existing : inChunk) {
                    if (ignoreClaimId != null && existing.getId().equals(ignoreClaimId))
                        continue;

                    // AABB Collision Detection
                    if (maxX >= existing.getMinX() && minX <= existing.getMaxX() &&
                            maxZ >= existing.getMinZ() && minZ <= existing.getMaxZ()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isRegionProtected(Claim claim) {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null)
            return false;

        return WorldGuardHook.isRegionProtected(claim);
    }

    public boolean createClaim(Claim claim) {
        if (isOverlapping(claim))
            return false;

        if (isRegionProtected(claim)) {
            return false;
        }

        // B19 — Evento customizado: ClaimCreateEvent
        br.com.gorvax.core.events.ClaimCreateEvent createEvent = new br.com.gorvax.core.events.ClaimCreateEvent(
                claim.getOwner(), claim);
        org.bukkit.Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled())
            return false;

        claims.add(claim);
        updateChunkCache(claim, true);
        idMap.put(claim.getId(), claim); // Update Map
        saveClaimsAsync();
        notifyMapUpdate(); // B16

        // B10 — Log de auditoria
        if (plugin.getAuditManager() != null) {
            String details = String.format("%s em %s [%d,%d -> %d,%d]",
                    claim.isKingdom() ? "Reino '" + claim.getKingdomName() + "'" : "Claim",
                    claim.getWorldName(), claim.getMinX(), claim.getMinZ(),
                    claim.getMaxX(), claim.getMaxZ());
            plugin.getAuditManager().log(
                    AuditManager.AuditAction.CLAIM_CREATE,
                    claim.getOwner(),
                    plugin.getServer().getOfflinePlayer(claim.getOwner()).getName(),
                    details);
        }
        return true;
    }

    /**
     * B16 — Notifica o WebMapManager para atualizar markers.
     */
    private void notifyMapUpdate() {
        if (plugin.getWebMapManager() != null) {
            plugin.getWebMapManager().forceUpdate();
        }
    }

    private void updateChunkCache(Claim claim, boolean add) {
        int minChunkX = claim.getMinX() >> 4;
        int maxChunkX = claim.getMaxX() >> 4;
        int minChunkZ = claim.getMinZ() >> 4;
        int maxChunkZ = claim.getMaxZ() >> 4;

        Map<Long, List<Claim>> worldMap = chunkClaims.computeIfAbsent(claim.getWorldName(), k -> new HashMap<>());

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                List<Claim> list = worldMap.computeIfAbsent(chunkKey, k -> new ArrayList<>());
                if (add) {
                    if (!list.contains(claim))
                        list.add(claim);
                } else {
                    list.remove(claim);
                    if (list.isEmpty())
                        worldMap.remove(chunkKey);
                }
            }
        }
    }

    public Claim getClaimAt(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return null;

        Map<Long, List<Claim>> worldMap = chunkClaims.get(loc.getWorld().getName());
        if (worldMap == null)
            return null;

        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);

        List<Claim> inChunk = worldMap.get(chunkKey);
        if (inChunk == null)
            return null;

        for (Claim claim : inChunk) {
            if (claim.contains(loc)) {
                return claim;
            }
        }
        return null;
    }

    public Claim getClaimById(String id) {
        return idMap.get(id); // O(1) Lookup
    }

    public List<Claim> getClaims() {
        return claims;
    }

    // B13 — Retorna todos os outposts vinculados a um reino
    public List<Claim> getOutpostsForKingdom(String kingdomId) {
        List<Claim> outposts = new ArrayList<>();
        for (Claim c : claims) {
            if (c.isOutpost() && kingdomId.equals(c.getParentKingdomId())) {
                outposts.add(c);
            }
        }
        return outposts;
    }

    public Set<Claim> getClaimsNearby(Location loc, int radius) {
        Set<Claim> nearby = new HashSet<>();
        if (loc == null || loc.getWorld() == null)
            return nearby;

        Map<Long, List<Claim>> worldMap = chunkClaims.get(loc.getWorld().getName());
        if (worldMap == null)
            return nearby;

        int centerCX = loc.getBlockX() >> 4;
        int centerCZ = loc.getBlockZ() >> 4;
        int chunkRadius = (radius >> 4) + 1;

        for (int cx = centerCX - chunkRadius; cx <= centerCX + chunkRadius; cx++) {
            for (int cz = centerCZ - chunkRadius; cz <= centerCZ + chunkRadius; cz++) {
                long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                List<Claim> inChunk = worldMap.get(chunkKey);
                if (inChunk != null) {
                    nearby.addAll(inChunk);
                }
            }
        }
        return nearby;
    }

    // --- PROTECTIONS ---

    // --- PROTECTION CHECKS (Moved to ProtectionListener) ---
    // Métodos antigos removidos para garantir que a lógica nova seja a única ativa.
    // Lembre-se de registrar ProtectionListener no main Class.

    public void removeClaim(Claim claim) {
        // B10 — Log de auditoria
        if (plugin.getAuditManager() != null) {
            String details = String.format("%s em %s",
                    claim.isKingdom() ? "Reino '" + claim.getKingdomName() + "'" : "Claim '" + claim.getId() + "'",
                    claim.getWorldName());
            plugin.getAuditManager().log(
                    AuditManager.AuditAction.CLAIM_DELETE,
                    claim.getOwner(),
                    plugin.getServer().getOfflinePlayer(claim.getOwner()).getName(),
                    details);
        }
        claims.remove(claim);
        updateChunkCache(claim, false);
        idMap.remove(claim.getId()); // Update Map
        saveClaimsAsync();
        notifyMapUpdate(); // B16
    }

    public String getOwnerName(UUID uuid) {
        if (uuid == null)
            return "Ninguém";
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
    // --- PROTECTION CHECKS (Missing methods) ---

    public boolean isRestrictedWorld(String worldName) {
        List<String> blocked = plugin.getConfig().getStringList("protection.blocked_worlds");
        return blocked != null && blocked.contains(worldName);
    }

    public boolean isSpawnProtected(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return false;
        int radius = plugin.getConfig().getInt("protection.spawn_radius", 100);

        // Obtém o mundo principal (primeiro mundo carregado, geralmente "world")
        World spawnWorld = plugin.getServer().getWorlds().get(0);
        if (spawnWorld == null)
            return false;

        // Verifica se a localização está no mundo principal
        if (!loc.getWorld().equals(spawnWorld))
            return false;

        // Calcula distância ao spawn usando distance squared para performance
        Location spawnLoc = spawnWorld.getSpawnLocation();
        return loc.distanceSquared(spawnLoc) <= (radius * radius);
    }

    public boolean isRegionProtected(String worldName, int minX, int minZ, int maxX, int maxZ) {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null)
            return false;

        // Using temporary claim object to reuse hook logic
        Claim temp = new Claim("temp", UUID.randomUUID(), worldName, minX, minZ, maxX, maxZ, false, "temp");
        return WorldGuardHook.isRegionProtected(temp);
    }
}
