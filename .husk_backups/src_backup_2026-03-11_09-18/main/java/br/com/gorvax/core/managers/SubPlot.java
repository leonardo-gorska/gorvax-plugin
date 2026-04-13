package br.com.gorvax.core.managers;

import org.bukkit.Location;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import br.com.gorvax.core.managers.Claim.TrustType;

public class SubPlot {
    private String id;
    private String name;
    private int minX, minZ, maxX, maxZ;
    private double price; // Purchase price
    private double rentPrice; // Daily rent
    private boolean forSale;
    private boolean forRent;
    private UUID owner; // Null if belongs to city/main claim owner
    private UUID renter;
    private long rentExpire;
    private final Map<UUID, java.util.Set<TrustType>> trustedPlayers;

    public SubPlot(String id, String name, Location loc1, Location loc2) {
        this.id = id;
        this.name = name;
        this.minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        this.forSale = false;
        this.forRent = false;
        this.trustedPlayers = new HashMap<>();
    }

    // Load Constructor
    public SubPlot(String id, String name, int minX, int minZ, int maxX, int maxZ,
            double price, double rentPrice, boolean forSale, boolean forRent,
            UUID owner, UUID renter, long rentExpire) {
        this.id = id;
        this.name = name;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.price = price;
        this.rentPrice = rentPrice;
        this.forSale = forSale;
        this.forRent = forRent;
        this.owner = owner;
        this.renter = renter;
        this.rentExpire = rentExpire;
        this.trustedPlayers = new HashMap<>(); // Loaded manually later or empty
    }

    public boolean contains(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public void addTrust(UUID player, TrustType type) {
        trustedPlayers.computeIfAbsent(player, k -> new java.util.HashSet<>()).add(type);
    }

    public void removeTrust(UUID player) {
        trustedPlayers.remove(player);
    }

    public void removeTrust(UUID player, TrustType type) {
        if (trustedPlayers.containsKey(player)) {
            trustedPlayers.get(player).remove(type);
            if (trustedPlayers.get(player).isEmpty()) {
                trustedPlayers.remove(player);
            }
        }
    }

    public boolean hasPermission(UUID player, TrustType required) {
        // 1. Dono do Lote (Prioridade máxima local)
        if (owner != null && player.equals(owner))
            return true;

        // 2. Inquilino (Se estiver alugado, assume como dono temporário)
        if (renter != null && player.equals(renter))
            return true;

        // 3. Lista de Confiança (Local do Lote)
        if (trustedPlayers.containsKey(player)) {
            java.util.Set<TrustType> perms = trustedPlayers.get(player);

            if (perms.contains(TrustType.VICE))
                return true;

            if (required == TrustType.VICE)
                return false;

            if (perms.contains(TrustType.GERAL))
                return true;

            return perms.contains(required);
        }

        return false;
    }

    /**
     * Verifica se o SubPlot tem um "Senhor" definido (Dono ou Inquilino).
     * Se tiver, ele é autônomo e bloqueia regras do Reino.
     * Se não tiver (Lote à venda ou da prefeitura), segue regras do Reino.
     */
    public boolean hasEffectiveOwner() {
        return owner != null || renter != null;
    }

    public Map<UUID, java.util.Set<TrustType>> getTrustedPlayers() {
        return trustedPlayers;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getRentPrice() {
        return rentPrice;
    }

    public void setRentPrice(double rentPrice) {
        this.rentPrice = rentPrice;
    }

    public boolean isForSale() {
        return forSale;
    }

    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }

    public boolean isForRent() {
        return forRent;
    }

    public void setForRent(boolean forRent) {
        this.forRent = forRent;
    }

    public UUID getRenter() {
        return renter;
    }

    public void setRenter(UUID renter) {
        this.renter = renter;
    }

    public long getRentExpire() {
        return rentExpire;
    }

    public void setRentExpire(long rentExpire) {
        this.rentExpire = rentExpire;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }
}
