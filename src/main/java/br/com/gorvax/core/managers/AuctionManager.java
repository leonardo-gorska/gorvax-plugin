package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * B14.1 — Sistema de Leilão Global.
 * Gerencia criação, lances, finalização e coleta de leilões.
 */
public class AuctionManager {

    private final GorvaxCore plugin;
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final Map<UUID, List<PendingCollection>> pendingCollections = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private int auctionCounter = 0;

    // Configurações (carregadas do config.yml)
    private boolean enabled;
    private int defaultDuration;
    private int maxDuration;
    private double minPrice;
    private double bidIncrement;
    private double taxPercent;
    private int antiSnipeSeconds;
    private int broadcastLastSeconds;
    private int maxActivePerPlayer;

    public AuctionManager(GorvaxCore plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        startTickTask();
    }

    // =============================
    // CONFIGURAÇÃO
    // =============================

    private void loadConfig() {
        var config = plugin.getConfig();
        this.enabled = config.getBoolean("auction.enabled", true);
        this.defaultDuration = config.getInt("auction.default_duration", 300);
        this.maxDuration = config.getInt("auction.max_duration", 3600);
        this.minPrice = config.getDouble("auction.min_price", 10.0);
        this.bidIncrement = config.getDouble("auction.bid_increment", 1.0);
        this.taxPercent = config.getDouble("auction.tax_percent", 5.0);
        this.antiSnipeSeconds = config.getInt("auction.anti_snipe_seconds", 15);
        this.broadcastLastSeconds = config.getInt("auction.broadcast_last_seconds", 30);
        this.maxActivePerPlayer = config.getInt("auction.max_active_per_player", 3);
    }

    // =============================
    // LEILÃO — CRIAR
    // =============================

    public boolean createAuction(Player seller, ItemStack item, double startPrice, int durationSeconds) {
        var msg = plugin.getMessageManager();

        if (!enabled) {
            msg.send(seller, "auction.disabled");
            return false;
        }

        if (startPrice < minPrice) {
            msg.send(seller, "auction.price_too_low", String.format("%.2f", minPrice));
            return false;
        }

        if (durationSeconds <= 0)
            durationSeconds = defaultDuration;
        if (durationSeconds > maxDuration)
            durationSeconds = maxDuration;

        // Verificar limite por jogador
        long activeCount = activeAuctions.values().stream()
                .filter(a -> a.sellerUUID.equals(seller.getUniqueId()))
                .count();
        if (activeCount >= maxActivePerPlayer) {
            msg.send(seller, "auction.max_active", String.valueOf(maxActivePerPlayer));
            return false;
        }

        // Remover item do inventário
        ItemStack auctionItem = item.clone();
        item.setAmount(0);

        auctionCounter++;
        String id = "AUC-" + auctionCounter;
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        Auction auction = new Auction(id, seller.getUniqueId(), seller.getName(),
                auctionItem, startPrice, endTime);
        activeAuctions.put(id, auction);
        dirty.set(true);

        // Broadcast de início
        String itemName = auctionItem.hasItemMeta() && auctionItem.getItemMeta().hasDisplayName()
                ? PlainTextComponentSerializer.plainText().serialize(auctionItem.getItemMeta().displayName())
                : formatMaterialName(auctionItem.getType().name());
        int qty = auctionItem.getAmount();

        for (Player p : Bukkit.getOnlinePlayers()) {
            msg.send(p, "auction.broadcast_start", seller.getName(),
                    qty + "x " + itemName, String.format("%.2f", startPrice),
                    formatTime(durationSeconds));
        }

        msg.send(seller, "auction.created", id, formatTime(durationSeconds));
        seller.playSound(seller.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        // Registrar no audit (se disponível)
        if (plugin.getAuditManager() != null) {
            plugin.getAuditManager().log(
                    AuditManager.AuditAction.MARKET_SELL,
                    seller.getUniqueId(), seller.getName(),
                    "Leilão criado: " + qty + "x " + itemName + " por $" + String.format("%.2f", startPrice),
                    startPrice);
        }

        return true;
    }

    // =============================
    // LEILÃO — LANCE
    // =============================

    public boolean placeBid(Player bidder, String auctionId, double bidAmount) {
        var msg = plugin.getMessageManager();

        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            msg.send(bidder, "auction.not_found");
            return false;
        }

        if (auction.sellerUUID.equals(bidder.getUniqueId())) {
            msg.send(bidder, "auction.cannot_bid_own");
            return false;
        }

        if (auction.isExpired()) {
            msg.send(bidder, "auction.expired");
            return false;
        }

        double minimumBid = auction.currentBid > 0
                ? auction.currentBid + bidIncrement
                : auction.minPrice;

        if (bidAmount < minimumBid) {
            msg.send(bidder, "auction.bid_too_low", String.format("%.2f", minimumBid));
            return false;
        }

        // Verificar saldo
        if (!GorvaxCore.getEconomy().has(bidder, bidAmount)) {
            msg.send(bidder, "auction.insufficient_funds", String.format("%.2f", bidAmount));
            return false;
        }

        // Devolver dinheiro ao licitante anterior
        if (auction.currentBidder != null) {
            GorvaxCore.getEconomy().depositPlayer(
                    Bukkit.getOfflinePlayer(auction.currentBidder), auction.currentBid);
            Player prevBidder = Bukkit.getPlayer(auction.currentBidder);
            if (prevBidder != null && prevBidder.isOnline()) {
                msg.send(prevBidder, "auction.outbid", bidder.getName(),
                        String.format("%.2f", bidAmount), auction.id);
                prevBidder.playSound(prevBidder.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            }
        }

        // Debitar do novo licitante
        GorvaxCore.getEconomy().withdrawPlayer(bidder, bidAmount);

        auction.currentBid = bidAmount;
        auction.currentBidder = bidder.getUniqueId();
        auction.currentBidderName = bidder.getName();
        auction.bidCount++;
        dirty.set(true);

        // Anti-snipe: se faltam menos de 15s, estender
        long remaining = auction.endTime - System.currentTimeMillis();
        if (remaining < antiSnipeSeconds * 1000L) {
            auction.endTime = System.currentTimeMillis() + (antiSnipeSeconds * 1000L);
        }

        msg.send(bidder, "auction.bid_placed", String.format("%.2f", bidAmount), auction.id);
        bidder.playSound(bidder.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Broadcast do lance
        String itemName = getAuctionItemName(auction);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(bidder)) {
                msg.send(p, "auction.broadcast_bid", bidder.getName(),
                        String.format("%.2f", bidAmount), itemName);
            }
        }

        return true;
    }

    // =============================
    // LEILÃO — LANCE POR GUI (sem ID, usa o selecionado)
    // =============================

    /**
     * Dá lance no leilão com menor tempo restante (facilita uso pela GUI).
     * Retorna o ID do leilão se bem-sucedido, null caso contrário.
     */
    public String placeBidOnLatest(Player bidder, double bidAmount) {
        // Encontra o leilão com menos tempo restante que não seja do próprio jogador
        Auction target = activeAuctions.values().stream()
                .filter(a -> !a.sellerUUID.equals(bidder.getUniqueId()) && !a.isExpired())
                .min(Comparator.comparingLong(a -> a.endTime))
                .orElse(null);

        if (target == null) {
            plugin.getMessageManager().send(bidder, "auction.none_active");
            return null;
        }

        return placeBid(bidder, target.id, bidAmount) ? target.id : null;
    }

    // =============================
    // LEILÃO — CANCELAR
    // =============================

    public boolean cancelAuction(Player player, String auctionId) {
        var msg = plugin.getMessageManager();

        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            msg.send(player, "auction.not_found");
            return false;
        }

        boolean isAdmin = player.hasPermission("gorvax.admin");
        if (!auction.sellerUUID.equals(player.getUniqueId()) && !isAdmin) {
            msg.send(player, "auction.not_owner");
            return false;
        }

        if (auction.bidCount > 0 && !isAdmin) {
            msg.send(player, "auction.cannot_cancel_with_bids");
            return false;
        }

        // Devolver lance ao licitante atual (se houver, caso admin cancele)
        if (auction.currentBidder != null) {
            GorvaxCore.getEconomy().depositPlayer(
                    Bukkit.getOfflinePlayer(auction.currentBidder), auction.currentBid);
            Player bidder = Bukkit.getPlayer(auction.currentBidder);
            if (bidder != null && bidder.isOnline()) {
                msg.send(bidder, "auction.cancelled_refund",
                        String.format("%.2f", auction.currentBid), auctionId);
            }
        }

        // Devolver item ao vendedor
        addPendingCollection(auction.sellerUUID, auction.sellerName,
                auction.item, 0, "Leilão cancelado: " + auctionId);

        activeAuctions.remove(auctionId);
        dirty.set(true);

        msg.send(player, "auction.cancelled", auctionId);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);

        return true;
    }

    // =============================
    // LEILÃO — COLETAR PENDÊNCIAS
    // =============================

    public void collectPending(Player player) {
        var msg = plugin.getMessageManager();
        List<PendingCollection> pending = pendingCollections.get(player.getUniqueId());

        if (pending == null || pending.isEmpty()) {
            msg.send(player, "auction.nothing_to_collect");
            return;
        }

        List<PendingCollection> toRemove = new ArrayList<>();
        for (PendingCollection pc : pending) {
            if (pc.item != null) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(pc.item);
                if (!overflow.isEmpty()) {
                    msg.send(player, "auction.inventory_full_collect");
                    break;
                }
            }
            if (pc.money > 0) {
                GorvaxCore.getEconomy().depositPlayer(player, pc.money);
                msg.send(player, "auction.money_collected", String.format("%.2f", pc.money));
            }
            toRemove.add(pc);
        }

        pending.removeAll(toRemove);
        if (pending.isEmpty()) {
            pendingCollections.remove(player.getUniqueId());
        }
        dirty.set(true);

        if (!toRemove.isEmpty()) {
            msg.send(player, "auction.collected_success", String.valueOf(toRemove.size()));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        }
    }

    // =============================
    // TASK PERIÓDICA
    // =============================

    private void startTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled)
                    return;
                processExpiredAuctions();
                broadcastEndingSoon();
            }
        }.runTaskTimer(plugin, 20L, 20L); // A cada 1 segundo
    }

    private void processExpiredAuctions() {
        var msg = plugin.getMessageManager();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Auction> entry : activeAuctions.entrySet()) {
            Auction auction = entry.getValue();
            if (!auction.isExpired())
                continue;

            toRemove.add(entry.getKey());

            String itemName = getAuctionItemName(auction);

            if (auction.currentBidder != null) {
                // Leilão finalizado com vencedor
                double tax = auction.currentBid * (taxPercent / 100.0);
                double sellerReceives = auction.currentBid - tax;

                // Dinheiro para o vendedor (via pendingCollection)
                addPendingCollection(auction.sellerUUID, auction.sellerName,
                        null, sellerReceives,
                        "Leilão vendido: " + itemName + " por $" + String.format("%.2f", auction.currentBid));

                // Item para o comprador (via pendingCollection)
                addPendingCollection(auction.currentBidder, auction.currentBidderName,
                        auction.item, 0,
                        "Leilão arrematado: " + itemName);

                // Broadcast
                for (Player p : Bukkit.getOnlinePlayers()) {
                    msg.send(p, "auction.broadcast_end",
                            auction.currentBidderName, itemName,
                            String.format("%.2f", auction.currentBid));
                }

                // Notificar vendedor
                Player seller = Bukkit.getPlayer(auction.sellerUUID);
                if (seller != null && seller.isOnline()) {
                    msg.send(seller, "auction.sold",
                            itemName, String.format("%.2f", auction.currentBid),
                            String.format("%.2f", tax), String.format("%.2f", sellerReceives));
                    seller.playSound(seller.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
                }

                // Notificar comprador
                Player buyer = Bukkit.getPlayer(auction.currentBidder);
                if (buyer != null && buyer.isOnline()) {
                    msg.send(buyer, "auction.won",
                            itemName, String.format("%.2f", auction.currentBid));
                    buyer.playSound(buyer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                }

                // Audit
                if (plugin.getAuditManager() != null) {
                    plugin.getAuditManager().log(
                            AuditManager.AuditAction.MARKET_BUY,
                            auction.currentBidder, auction.currentBidderName,
                            "Leilão arrematado: " + itemName + " de " + auction.sellerName,
                            auction.currentBid);
                }
            } else {
                // Leilão expirou sem lances — devolver item
                addPendingCollection(auction.sellerUUID, auction.sellerName,
                        auction.item, 0,
                        "Leilão expirado sem lances: " + itemName);

                Player seller = Bukkit.getPlayer(auction.sellerUUID);
                if (seller != null && seller.isOnline()) {
                    msg.send(seller, "auction.expired_no_bids", itemName);
                }
            }
        }

        for (String id : toRemove) {
            activeAuctions.remove(id);
        }
        if (!toRemove.isEmpty()) {
            dirty.set(true);
        }
    }

    private final Set<String> broadcastedIds = ConcurrentHashMap.newKeySet();

    private void broadcastEndingSoon() {
        var msg = plugin.getMessageManager();
        for (Auction auction : activeAuctions.values()) {
            long remaining = (auction.endTime - System.currentTimeMillis()) / 1000;
            if (remaining <= broadcastLastSeconds && remaining > 0
                    && auction.currentBidder != null
                    && broadcastedIds.add(auction.id)) {
                String itemName = getAuctionItemName(auction);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    msg.send(p, "auction.broadcast_ending",
                            itemName, String.valueOf(remaining),
                            String.format("%.2f", auction.currentBid),
                            auction.currentBidderName);
                }
            }
        }
    }

    // =============================
    // UTILITÁRIOS
    // =============================

    private void addPendingCollection(UUID playerUUID, String playerName,
            ItemStack item, double money, String description) {
        pendingCollections.computeIfAbsent(playerUUID, k -> new ArrayList<>())
                .add(new PendingCollection(item, money, description));
        dirty.set(true);

        // Notificar se online
        Player p = Bukkit.getPlayer(playerUUID);
        if (p != null && p.isOnline()) {
            plugin.getMessageManager().send(p, "auction.pending_collect");
        }
    }

    public boolean hasPendingCollections(UUID uuid) {
        List<PendingCollection> list = pendingCollections.get(uuid);
        return list != null && !list.isEmpty();
    }

    public List<PendingCollection> getPendingCollections(UUID uuid) {
        return pendingCollections.getOrDefault(uuid, Collections.emptyList());
    }

    public Collection<Auction> getActiveAuctions() {
        return Collections.unmodifiableCollection(activeAuctions.values());
    }

    public Auction getAuction(String id) {
        return activeAuctions.get(id);
    }

    public String getAuctionItemName(Auction auction) {
        if (auction.item.hasItemMeta() && auction.item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(auction.item.getItemMeta().displayName());
        }
        return formatMaterialName(auction.item.getType().name());
    }

    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty())
                sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return sb.toString();
    }

    public String formatTime(long seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        } else if (seconds >= 60) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }

    public boolean isEnabled() {
        return enabled;
    }

    // =============================
    // PERSISTÊNCIA
    // =============================

    private File getDataFile() {
        return new File(plugin.getDataFolder(), "auction_data.yml");
    }

    public void loadData() {
        File file = getDataFile();
        if (!file.exists())
            return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        auctionCounter = yaml.getInt("counter", 0);

        // Carregar leilões ativos
        if (yaml.isConfigurationSection("auctions")) {
            for (String id : yaml.getConfigurationSection("auctions").getKeys(false)) {
                String path = "auctions." + id;
                try {
                    UUID sellerUUID = UUID.fromString(yaml.getString(path + ".seller_uuid"));
                    String sellerName = yaml.getString(path + ".seller_name", "?");
                    ItemStack item = yaml.getItemStack(path + ".item");
                    double minP = yaml.getDouble(path + ".min_price");
                    long endTime = yaml.getLong(path + ".end_time");
                    double currentBid = yaml.getDouble(path + ".current_bid", 0);
                    String bidderStr = yaml.getString(path + ".current_bidder", null);
                    UUID currentBidder = bidderStr != null ? UUID.fromString(bidderStr) : null;
                    String bidderName = yaml.getString(path + ".current_bidder_name", null);
                    int bidCount = yaml.getInt(path + ".bid_count", 0);

                    if (item == null)
                        continue;

                    Auction auction = new Auction(id, sellerUUID, sellerName, item, minP, endTime);
                    auction.currentBid = currentBid;
                    auction.currentBidder = currentBidder;
                    auction.currentBidderName = bidderName;
                    auction.bidCount = bidCount;
                    activeAuctions.put(id, auction);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Leilão] Erro ao carregar leilão " + id + ": " + e.getMessage());
                }
            }
        }

        // Carregar pendências
        if (yaml.isConfigurationSection("pending")) {
            for (String uuidStr : yaml.getConfigurationSection("pending").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<Map<?, ?>> list = yaml.getMapList("pending." + uuidStr);
                    List<PendingCollection> pending = new ArrayList<>();
                    for (Map<?, ?> map : list) {
                        ItemStack pItem = null;
                        if (map.containsKey("item_data")) {
                            String itemPath = "pending." + uuidStr + ".items";
                            // Item será carregado separadamente
                        }
                        double money = map.containsKey("money") ? ((Number) map.get("money")).doubleValue() : 0;
                        String desc = map.containsKey("description") ? (String) map.get("description") : "";
                        pending.add(new PendingCollection(pItem, money, desc));
                    }
                    // Carregar itens das pendências separadamente
                    if (yaml.isConfigurationSection("pending_items." + uuidStr)) {
                        var itemSection = yaml.getConfigurationSection("pending_items." + uuidStr);
                        int idx = 0;
                        for (String key : itemSection.getKeys(false)) {
                            ItemStack pItem = itemSection.getItemStack(key);
                            if (idx < pending.size()) {
                                pending.get(idx).item = pItem;
                            }
                            idx++;
                        }
                    }
                    if (!pending.isEmpty()) {
                        pendingCollections.put(uuid, pending);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[Leilão] Erro ao carregar pendências: " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("[Leilão] Carregados " + activeAuctions.size() + " leilões ativos e "
                + pendingCollections.size() + " pendências.");
    }

    public void saveSync() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("counter", auctionCounter);

        // Salvar leilões ativos
        for (Map.Entry<String, Auction> entry : activeAuctions.entrySet()) {
            String path = "auctions." + entry.getKey();
            Auction a = entry.getValue();
            yaml.set(path + ".seller_uuid", a.sellerUUID.toString());
            yaml.set(path + ".seller_name", a.sellerName);
            yaml.set(path + ".item", a.item);
            yaml.set(path + ".min_price", a.minPrice);
            yaml.set(path + ".end_time", a.endTime);
            yaml.set(path + ".current_bid", a.currentBid);
            yaml.set(path + ".current_bidder",
                    a.currentBidder != null ? a.currentBidder.toString() : null);
            yaml.set(path + ".current_bidder_name", a.currentBidderName);
            yaml.set(path + ".bid_count", a.bidCount);
        }

        // Salvar pendências
        for (Map.Entry<UUID, List<PendingCollection>> entry : pendingCollections.entrySet()) {
            String uuidStr = entry.getKey().toString();
            List<PendingCollection> list = entry.getValue();
            List<Map<String, Object>> serialized = new ArrayList<>();
            int idx = 0;
            for (PendingCollection pc : list) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("money", pc.money);
                map.put("description", pc.description);
                map.put("has_item", pc.item != null);
                serialized.add(map);

                if (pc.item != null) {
                    yaml.set("pending_items." + uuidStr + "." + idx, pc.item);
                }
                idx++;
            }
            yaml.set("pending." + uuidStr, serialized);
        }

        try {
            yaml.save(getDataFile());
            dirty.set(false);
        } catch (IOException e) {
            plugin.getLogger().severe("[Leilão] Erro ao salvar dados: " + e.getMessage());
        }
    }

    public void saveAsync() {
        if (!dirty.getAndSet(false))
            return;
        // Snapshot dos dados na main thread
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("counter", auctionCounter);

        for (Map.Entry<String, Auction> entry : activeAuctions.entrySet()) {
            String path = "auctions." + entry.getKey();
            Auction a = entry.getValue();
            yaml.set(path + ".seller_uuid", a.sellerUUID.toString());
            yaml.set(path + ".seller_name", a.sellerName);
            yaml.set(path + ".item", a.item);
            yaml.set(path + ".min_price", a.minPrice);
            yaml.set(path + ".end_time", a.endTime);
            yaml.set(path + ".current_bid", a.currentBid);
            yaml.set(path + ".current_bidder",
                    a.currentBidder != null ? a.currentBidder.toString() : null);
            yaml.set(path + ".current_bidder_name", a.currentBidderName);
            yaml.set(path + ".bid_count", a.bidCount);
        }

        for (Map.Entry<UUID, List<PendingCollection>> entry : pendingCollections.entrySet()) {
            String uuidStr = entry.getKey().toString();
            List<PendingCollection> list = entry.getValue();
            List<Map<String, Object>> serialized = new ArrayList<>();
            int idx = 0;
            for (PendingCollection pc : list) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("money", pc.money);
                map.put("description", pc.description);
                map.put("has_item", pc.item != null);
                serialized.add(map);
                if (pc.item != null) {
                    yaml.set("pending_items." + uuidStr + "." + idx, pc.item);
                }
                idx++;
            }
            yaml.set("pending." + uuidStr, serialized);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                yaml.save(getDataFile());
            } catch (IOException e) {
                plugin.getLogger().severe("[Leilão] Erro ao salvar dados (async): " + e.getMessage());
            }
        });
    }

    public void reload() {
        loadConfig();
        activeAuctions.clear();
        pendingCollections.clear();
        broadcastedIds.clear();
        loadData();
        plugin.getLogger().info("[Leilão] Dados recarregados com sucesso!");
    }

    // =============================
    // CLASSES INTERNAS
    // =============================

    public static class Auction {
        public final String id;
        public final UUID sellerUUID;
        public final String sellerName;
        public final ItemStack item;
        public final double minPrice;
        public long endTime;
        public double currentBid;
        public UUID currentBidder;
        public String currentBidderName;
        public int bidCount;

        public Auction(String id, UUID sellerUUID, String sellerName,
                ItemStack item, double minPrice, long endTime) {
            this.id = id;
            this.sellerUUID = sellerUUID;
            this.sellerName = sellerName;
            this.item = item;
            this.minPrice = minPrice;
            this.endTime = endTime;
            this.currentBid = 0;
            this.currentBidder = null;
            this.currentBidderName = null;
            this.bidCount = 0;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= endTime;
        }

        public long getRemainingSeconds() {
            return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static class PendingCollection {
        public ItemStack item;
        public double money;
        public String description;

        public PendingCollection(ItemStack item, double money, String description) {
            this.item = item;
            this.money = money;
            this.description = description;
        }
    }
}
