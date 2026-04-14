package br.com.gorvax.core.storage;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.*;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * B18 — Implementação SQLite do DataStore.
 * Arquivo gorvax.db dentro do dataFolder do plugin.
 * Todas as operações usam PreparedStatement (anti-SQL injection).
 */
public class SQLiteDataStore implements DataStore {

    private final GorvaxCore plugin;
    private final String dbPath;
    private Connection connection;

    public SQLiteDataStore(GorvaxCore plugin) {
        this.plugin = plugin;
        String fileName = plugin.getConfig().getString("storage.sqlite.file", "gorvax.db");
        this.dbPath = new File(plugin.getDataFolder(), fileName).getAbsolutePath();
    }

    @Override
    public void init() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

        // Otimizações de performance para SQLite
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("PRAGMA foreign_keys=ON");
        }

        createTables();
        plugin.getLogger().info("[Storage] Backend SQLite inicializado: " + dbPath);
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao fechar conexão SQLite", e);
        }
    }

    @Override
    public StorageType getType() {
        return StorageType.SQLITE;
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        }
        return connection;
    }

    // ==========================================
    // SCHEMA
    // ==========================================

    private void createTables() throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            // Claims
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claims (
                    id TEXT PRIMARY KEY,
                    owner_uuid TEXT NOT NULL,
                    world TEXT NOT NULL,
                    min_x INTEGER NOT NULL,
                    min_z INTEGER NOT NULL,
                    max_x INTEGER NOT NULL,
                    max_z INTEGER NOT NULL,
                    is_kingdom INTEGER DEFAULT 0,
                    name TEXT,
                    kingdom_name TEXT,
                    type TEXT DEFAULT 'NORMAL',
                    pvp INTEGER DEFAULT 0,
                    residents_pvp INTEGER DEFAULT 0,
                    residents_pvp_outside INTEGER DEFAULT 0,
                    is_public INTEGER DEFAULT 1,
                    residents_build INTEGER DEFAULT 0,
                    residents_container INTEGER DEFAULT 0,
                    residents_switch INTEGER DEFAULT 0,
                    parent_kingdom_id TEXT,
                    welcome_color TEXT DEFAULT '§b',
                    chat_color TEXT DEFAULT '§f',
                    tag_color TEXT DEFAULT '§e',
                    tag TEXT,
                    tax REAL DEFAULT 5.0,
                    enter_title TEXT,
                    enter_subtitle TEXT,
                    exit_title TEXT,
                    exit_subtitle TEXT
                )""");

            // Trusts de Claims
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claim_trusts (
                    claim_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    trust_type TEXT NOT NULL,
                    PRIMARY KEY (claim_id, player_uuid, trust_type),
                    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
                )""");

            // SubPlots
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subplots (
                    id TEXT NOT NULL,
                    claim_id TEXT NOT NULL,
                    name TEXT,
                    min_x INTEGER NOT NULL,
                    min_z INTEGER NOT NULL,
                    max_x INTEGER NOT NULL,
                    max_z INTEGER NOT NULL,
                    price REAL DEFAULT 0,
                    rent_price REAL DEFAULT 0,
                    for_sale INTEGER DEFAULT 0,
                    for_rent INTEGER DEFAULT 0,
                    owner_uuid TEXT,
                    renter_uuid TEXT,
                    rent_expire INTEGER DEFAULT 0,
                    PRIMARY KEY (id, claim_id),
                    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
                )""");

            // Trusts de SubPlots
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subplot_trusts (
                    subplot_id TEXT NOT NULL,
                    claim_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    trust_type TEXT NOT NULL,
                    PRIMARY KEY (subplot_id, claim_id, player_uuid, trust_type),
                    FOREIGN KEY (subplot_id, claim_id) REFERENCES subplots(id, claim_id) ON DELETE CASCADE
                )""");

            // Player Data
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    blocks INTEGER DEFAULT 100,
                    king_rank INTEGER DEFAULT 0,
                    first_join INTEGER DEFAULT 0,
                    total_play_time INTEGER DEFAULT 0,
                    last_login INTEGER DEFAULT 0,
                    total_blocks_broken INTEGER DEFAULT 0,
                    total_blocks_placed INTEGER DEFAULT 0,
                    total_kills INTEGER DEFAULT 0,
                    total_deaths INTEGER DEFAULT 0,
                    bosses_killed INTEGER DEFAULT 0,
                    boss_top_damage INTEGER DEFAULT 0,
                    total_money_earned REAL DEFAULT 0,
                    total_money_spent REAL DEFAULT 0,
                    active_title TEXT DEFAULT '',
                    border_sound INTEGER DEFAULT 1,
                    tutorial_step INTEGER DEFAULT 0,
                    has_received_kit INTEGER DEFAULT 0,
                    tutorial_completed INTEGER DEFAULT 0
                )""");

            // Títulos desbloqueados
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_titles (
                    player_uuid TEXT NOT NULL,
                    title TEXT NOT NULL,
                    PRIMARY KEY (player_uuid, title),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )""");

            // Conquistas dos jogadores
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_achievements (
                    player_uuid TEXT NOT NULL,
                    achievement_id TEXT NOT NULL,
                    unlocked_at INTEGER DEFAULT 0,
                    PRIMARY KEY (player_uuid, achievement_id),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )""");

            // Audit Log
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    player_uuid TEXT,
                    player_name TEXT,
                    details TEXT,
                    value REAL DEFAULT 0
                )""");

            // Mail
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mail (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid TEXT NOT NULL,
                    sender_uuid TEXT NOT NULL,
                    sender_name TEXT NOT NULL,
                    message TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    is_read INTEGER DEFAULT 0
                )""");

            // Bounties
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bounties (
                    target_uuid TEXT PRIMARY KEY,
                    target_name TEXT NOT NULL,
                    total_value REAL DEFAULT 0,
                    last_updated INTEGER NOT NULL
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bounty_contributors (
                    target_uuid TEXT NOT NULL,
                    contributor_uuid TEXT NOT NULL,
                    amount REAL DEFAULT 0,
                    PRIMARY KEY (target_uuid, contributor_uuid),
                    FOREIGN KEY (target_uuid) REFERENCES bounties(target_uuid) ON DELETE CASCADE
                )""");

            // Votes
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS votes (
                    kingdom_id TEXT PRIMARY KEY,
                    question TEXT NOT NULL,
                    creator_uuid TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vote_entries (
                    kingdom_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    vote_value INTEGER NOT NULL,
                    PRIMARY KEY (kingdom_id, player_uuid),
                    FOREIGN KEY (kingdom_id) REFERENCES votes(kingdom_id) ON DELETE CASCADE
                )""");

            // Auctions
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id TEXT PRIMARY KEY,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    item_data TEXT NOT NULL,
                    min_price REAL NOT NULL,
                    end_time INTEGER NOT NULL,
                    current_bid REAL DEFAULT 0,
                    current_bidder TEXT,
                    current_bidder_name TEXT,
                    bid_count INTEGER DEFAULT 0
                )""");

            // Coleções pendentes (itens a serem coletados após leilão)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pending_collections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    item_data TEXT,
                    money REAL DEFAULT 0,
                    description TEXT
                )""");

            // Price History
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS price_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    buy_price REAL NOT NULL,
                    sell_price REAL NOT NULL
                )""");

            // Índices para performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_claims_owner ON claims(owner_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_player ON audit_log(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mail_target ON mail(target_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_price_item ON price_history(item_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pending_player ON pending_collections(player_uuid)");
        }
    }

    // ==========================================
    // CLAIMS
    // ==========================================

    @Override
    public List<Claim> loadClaims() {
        List<Claim> result = new ArrayList<>();
        try {
            // 1. Carregar claims
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM claims");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    String world = rs.getString("world");
                    int minX = rs.getInt("min_x");
                    int minZ = rs.getInt("min_z");
                    int maxX = rs.getInt("max_x");
                    int maxZ = rs.getInt("max_z");
                    boolean isKingdom = rs.getInt("is_kingdom") == 1;
                    String name = rs.getString("name");

                    Claim claim = new Claim(id, owner, world, minX, minZ, maxX, maxZ, isKingdom, name);
                    claim.setKingdomName(rs.getString("kingdom_name"));
                    claim.setWelcomeColor(rs.getString("welcome_color"));
                    claim.setChatColor(rs.getString("chat_color"));
                    claim.setTagColor(rs.getString("tag_color"));
                    claim.setTag(rs.getString("tag"));
                    claim.setTax(rs.getDouble("tax"));
                    claim.setEnterTitle(rs.getString("enter_title"));
                    claim.setEnterSubtitle(rs.getString("enter_subtitle"));
                    claim.setExitTitle(rs.getString("exit_title"));
                    claim.setExitSubtitle(rs.getString("exit_subtitle"));

                    String typeStr = rs.getString("type");
                    if (typeStr != null) {
                        try { claim.setType(Claim.Type.valueOf(typeStr)); } catch (Exception e) { plugin.getLogger().fine("Tipo de claim inválido (SQLite): " + e.getMessage()); }
                    }
                    claim.setPvp(rs.getInt("pvp") == 1);
                    claim.setResidentsPvp(rs.getInt("residents_pvp") == 1);
                    claim.setResidentsPvpOutside(rs.getInt("residents_pvp_outside") == 1);
                    claim.setPublic(rs.getInt("is_public") == 1);
                    claim.setResidentsBuild(rs.getInt("residents_build") == 1);
                    claim.setResidentsContainer(rs.getInt("residents_container") == 1);
                    claim.setResidentsSwitch(rs.getInt("residents_switch") == 1);
                    claim.setParentKingdomId(rs.getString("parent_kingdom_id"));

                    result.add(claim);
                }
            }

            // 2. Carregar trusts dos claims
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM claim_trusts");
                 ResultSet rs = ps.executeQuery()) {
                Map<String, Claim> claimMap = new HashMap<>();
                for (Claim c : result) claimMap.put(c.getId(), c);

                while (rs.next()) {
                    String claimId = rs.getString("claim_id");
                    Claim claim = claimMap.get(claimId);
                    if (claim == null) continue;
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    Claim.TrustType type = Claim.TrustType.valueOf(rs.getString("trust_type"));
                    claim.addTrust(uuid, type);
                }
            }

            // 3. Carregar subplots
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM subplots");
                 ResultSet rs = ps.executeQuery()) {
                Map<String, Claim> claimMap = new HashMap<>();
                for (Claim c : result) claimMap.put(c.getId(), c);

                while (rs.next()) {
                    String claimId = rs.getString("claim_id");
                    Claim claim = claimMap.get(claimId);
                    if (claim == null) continue;

                    String sid = rs.getString("id");
                    String sname = rs.getString("name");
                    int sminX = rs.getInt("min_x");
                    int sminZ = rs.getInt("min_z");
                    int smaxX = rs.getInt("max_x");
                    int smaxZ = rs.getInt("max_z");
                    double sprice = rs.getDouble("price");
                    double srentPrice = rs.getDouble("rent_price");
                    boolean sforSale = rs.getInt("for_sale") == 1;
                    boolean sforRent = rs.getInt("for_rent") == 1;
                    UUID sowner = parseUuid(rs.getString("owner_uuid"));
                    UUID srenter = parseUuid(rs.getString("renter_uuid"));
                    long srentExpire = rs.getLong("rent_expire");

                    SubPlot plot = new SubPlot(sid, sname, sminX, sminZ, smaxX, smaxZ,
                            sprice, srentPrice, sforSale, sforRent, sowner, srenter, srentExpire);
                    claim.addSubPlot(plot);
                }
            }

            // 4. Carregar trusts dos subplots
            try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM subplot_trusts");
                 ResultSet rs = ps.executeQuery()) {
                Map<String, Map<String, SubPlot>> plotMap = new HashMap<>();
                for (Claim c : result) {
                    Map<String, SubPlot> subs = new HashMap<>();
                    for (SubPlot sp : c.getSubPlots()) subs.put(sp.getId(), sp);
                    plotMap.put(c.getId(), subs);
                }

                while (rs.next()) {
                    String claimId = rs.getString("claim_id");
                    String subplotId = rs.getString("subplot_id");
                    Map<String, SubPlot> subs = plotMap.get(claimId);
                    if (subs == null) continue;
                    SubPlot plot = subs.get(subplotId);
                    if (plot == null) continue;
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    Claim.TrustType type = Claim.TrustType.valueOf(rs.getString("trust_type"));
                    plot.addTrust(uuid, type);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar claims (SQLite)", e);
        }
        return result;
    }

    @Override
    public void saveClaims(List<Claim> claims) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                // Limpar tabelas existentes (cascata cuida dos relacionamentos)
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM claims");
                }

                // Inserir claims
                String claimSql = """
                    INSERT INTO claims (id, owner_uuid, world, min_x, min_z, max_x, max_z,
                        is_kingdom, name, kingdom_name, type, pvp, residents_pvp, residents_pvp_outside,
                        is_public, residents_build, residents_container, residents_switch,
                        parent_kingdom_id, welcome_color, chat_color, tag_color, tag, tax,
                        enter_title, enter_subtitle, exit_title, exit_subtitle)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

                String trustSql = "INSERT INTO claim_trusts (claim_id, player_uuid, trust_type) VALUES (?, ?, ?)";
                String subplotSql = """
                    INSERT INTO subplots (id, claim_id, name, min_x, min_z, max_x, max_z,
                        price, rent_price, for_sale, for_rent, owner_uuid, renter_uuid, rent_expire)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
                String subplotTrustSql = "INSERT INTO subplot_trusts (subplot_id, claim_id, player_uuid, trust_type) VALUES (?, ?, ?, ?)";

                try (PreparedStatement claimPs = conn.prepareStatement(claimSql);
                     PreparedStatement trustPs = conn.prepareStatement(trustSql);
                     PreparedStatement subplotPs = conn.prepareStatement(subplotSql);
                     PreparedStatement subplotTrustPs = conn.prepareStatement(subplotTrustSql)) {

                    for (Claim c : claims) {
                        claimPs.setString(1, c.getId());
                        claimPs.setString(2, c.getOwner().toString());
                        claimPs.setString(3, c.getWorldName());
                        claimPs.setInt(4, c.getMinX());
                        claimPs.setInt(5, c.getMinZ());
                        claimPs.setInt(6, c.getMaxX());
                        claimPs.setInt(7, c.getMaxZ());
                        claimPs.setInt(8, c.isKingdom() ? 1 : 0);
                        claimPs.setString(9, c.getName());
                        claimPs.setString(10, c.getKingdomName());
                        claimPs.setString(11, c.getType().name());
                        claimPs.setInt(12, c.isPvp() ? 1 : 0);
                        claimPs.setInt(13, c.isResidentsPvp() ? 1 : 0);
                        claimPs.setInt(14, c.isResidentsPvpOutside() ? 1 : 0);
                        claimPs.setInt(15, c.isPublic() ? 1 : 0);
                        claimPs.setInt(16, c.isResidentsBuild() ? 1 : 0);
                        claimPs.setInt(17, c.isResidentsContainer() ? 1 : 0);
                        claimPs.setInt(18, c.isResidentsSwitch() ? 1 : 0);
                        claimPs.setString(19, c.getParentKingdomId());
                        claimPs.setString(20, c.getWelcomeColor());
                        claimPs.setString(21, c.getChatColor());
                        claimPs.setString(22, c.getTagColor());
                        claimPs.setString(23, c.getTag());
                        claimPs.setDouble(24, c.getTax());
                        claimPs.setString(25, c.getEnterTitle());
                        claimPs.setString(26, c.getEnterSubtitle());
                        claimPs.setString(27, c.getExitTitle());
                        claimPs.setString(28, c.getExitSubtitle());
                        claimPs.addBatch();

                        // Trusts
                        for (Map.Entry<UUID, Set<Claim.TrustType>> entry : c.getTrustedPlayers().entrySet()) {
                            for (Claim.TrustType t : entry.getValue()) {
                                trustPs.setString(1, c.getId());
                                trustPs.setString(2, entry.getKey().toString());
                                trustPs.setString(3, t.name());
                                trustPs.addBatch();
                            }
                        }

                        // SubPlots
                        for (SubPlot sp : c.getSubPlots()) {
                            subplotPs.setString(1, sp.getId());
                            subplotPs.setString(2, c.getId());
                            subplotPs.setString(3, sp.getName());
                            subplotPs.setInt(4, sp.getMinX());
                            subplotPs.setInt(5, sp.getMinZ());
                            subplotPs.setInt(6, sp.getMaxX());
                            subplotPs.setInt(7, sp.getMaxZ());
                            subplotPs.setDouble(8, sp.getPrice());
                            subplotPs.setDouble(9, sp.getRentPrice());
                            subplotPs.setInt(10, sp.isForSale() ? 1 : 0);
                            subplotPs.setInt(11, sp.isForRent() ? 1 : 0);
                            subplotPs.setString(12, sp.getOwner() != null ? sp.getOwner().toString() : null);
                            subplotPs.setString(13, sp.getRenter() != null ? sp.getRenter().toString() : null);
                            subplotPs.setLong(14, sp.getRentExpire());
                            subplotPs.addBatch();

                            // SubPlot Trusts
                            for (Map.Entry<UUID, Set<Claim.TrustType>> entry : sp.getTrustedPlayers().entrySet()) {
                                for (Claim.TrustType t : entry.getValue()) {
                                    subplotTrustPs.setString(1, sp.getId());
                                    subplotTrustPs.setString(2, c.getId());
                                    subplotTrustPs.setString(3, entry.getKey().toString());
                                    subplotTrustPs.setString(4, t.name());
                                    subplotTrustPs.addBatch();
                                }
                            }
                        }
                    }

                    claimPs.executeBatch();
                    trustPs.executeBatch();
                    subplotPs.executeBatch();
                    subplotTrustPs.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar claims (SQLite)", e);
        }
    }

    // ==========================================
    // PLAYER DATA
    // ==========================================

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return readPlayerFromResultSet(rs, uuid);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar player data (SQLite): " + uuid, e);
            return null;
        }
    }

    @Override
    public Map<UUID, PlayerData> loadAllPlayerData() {
        Map<UUID, PlayerData> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM players");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                result.put(uuid, readPlayerFromResultSet(rs, uuid));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar todos os players (SQLite)", e);
        }

        // Carregar títulos e conquistas
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_titles");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                PlayerData pd = result.get(uuid);
                if (pd != null) pd.getUnlockedTitles().add(rs.getString("title"));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar títulos (SQLite)", e);
        }

        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_achievements");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                PlayerData pd = result.get(uuid);
                if (pd != null) pd.getAchievements().put(rs.getString("achievement_id"), rs.getLong("unlocked_at"));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar conquistas (SQLite)", e);
        }

        return result;
    }

    private PlayerData readPlayerFromResultSet(ResultSet rs, UUID uuid) throws SQLException {
        PlayerData pd = new PlayerData(uuid, rs.getInt("blocks"));
        pd.setKingRank(rs.getInt("king_rank") == 1);
        pd.setFirstJoin(rs.getLong("first_join"));
        pd.setTotalPlayTime(rs.getLong("total_play_time"));
        pd.setLastLogin(rs.getLong("last_login"));
        pd.setTotalBlocksBroken(rs.getInt("total_blocks_broken"));
        pd.setTotalBlocksPlaced(rs.getInt("total_blocks_placed"));
        pd.setTotalKills(rs.getInt("total_kills"));
        pd.setTotalDeaths(rs.getInt("total_deaths"));
        pd.setBossesKilled(rs.getInt("bosses_killed"));
        pd.setBossTopDamage(rs.getInt("boss_top_damage"));
        pd.setTotalMoneyEarned(rs.getDouble("total_money_earned"));
        pd.setTotalMoneySpent(rs.getDouble("total_money_spent"));
        pd.setActiveTitle(rs.getString("active_title"));
        pd.setBorderSound(rs.getInt("border_sound") == 1);

        // Carregar títulos para este jogador
        try (PreparedStatement tps = getConnection().prepareStatement(
                "SELECT title FROM player_titles WHERE player_uuid = ?")) {
            tps.setString(1, uuid.toString());
            try (ResultSet trs = tps.executeQuery()) {
                Set<String> titles = new HashSet<>();
                while (trs.next()) titles.add(trs.getString("title"));
                pd.setUnlockedTitles(titles);
            }
        }

        // Carregar conquistas para este jogador
        try (PreparedStatement aps = getConnection().prepareStatement(
                "SELECT achievement_id, unlocked_at FROM player_achievements WHERE player_uuid = ?")) {
            aps.setString(1, uuid.toString());
            try (ResultSet ars = aps.executeQuery()) {
                Map<String, Long> achievements = new HashMap<>();
                while (ars.next()) achievements.put(ars.getString("achievement_id"), ars.getLong("unlocked_at"));
                pd.setAchievements(achievements);
            }
        }

        // B4 — Tutorial Interativo + Welcome Kit
        pd.setTutorialStep(rs.getInt("tutorial_step"));
        pd.setHasReceivedKit(rs.getInt("has_received_kit") == 1);
        pd.setTutorialCompleted(rs.getInt("tutorial_completed") == 1);

        return pd;
    }

    @Override
    public void savePlayerData(UUID uuid, PlayerData pd) {
        try {
            upsertPlayer(uuid, pd);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar player data (SQLite): " + uuid, e);
        }
    }

    @Override
    public void saveAllPlayerData(Map<UUID, PlayerData> dataMap) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                for (Map.Entry<UUID, PlayerData> entry : dataMap.entrySet()) {
                    upsertPlayer(entry.getKey(), entry.getValue());
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar todos os players (SQLite)", e);
        }
    }

    private void upsertPlayer(UUID uuid, PlayerData pd) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO players
            (uuid, blocks, king_rank, first_join, total_play_time, last_login,
             total_blocks_broken, total_blocks_placed, total_kills, total_deaths,
             bosses_killed, boss_top_damage, total_money_earned, total_money_spent,
             active_title, border_sound, tutorial_step, has_received_kit, tutorial_completed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, pd.getClaimBlocks());
            ps.setInt(3, pd.hasKingRank() ? 1 : 0);
            ps.setLong(4, pd.getFirstJoin());
            ps.setLong(5, pd.getTotalPlayTime());
            ps.setLong(6, pd.getLastLogin());
            ps.setInt(7, pd.getTotalBlocksBroken());
            ps.setInt(8, pd.getTotalBlocksPlaced());
            ps.setInt(9, pd.getTotalKills());
            ps.setInt(10, pd.getTotalDeaths());
            ps.setInt(11, pd.getBossesKilled());
            ps.setInt(12, pd.getBossTopDamage());
            ps.setDouble(13, pd.getTotalMoneyEarned());
            ps.setDouble(14, pd.getTotalMoneySpent());
            ps.setString(15, pd.getActiveTitle());
            ps.setInt(16, pd.isBorderSound() ? 1 : 0);
            ps.setInt(17, pd.getTutorialStep());
            ps.setInt(18, pd.hasReceivedKit() ? 1 : 0);
            ps.setInt(19, pd.isTutorialCompleted() ? 1 : 0);
            ps.executeUpdate();
        }

        // Atualizar títulos
        try (PreparedStatement del = getConnection().prepareStatement(
                "DELETE FROM player_titles WHERE player_uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        if (!pd.getUnlockedTitles().isEmpty()) {
            try (PreparedStatement ins = getConnection().prepareStatement(
                    "INSERT INTO player_titles (player_uuid, title) VALUES (?, ?)")) {
                for (String title : pd.getUnlockedTitles()) {
                    ins.setString(1, uuid.toString());
                    ins.setString(2, title);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        }

        // Atualizar conquistas
        try (PreparedStatement del = getConnection().prepareStatement(
                "DELETE FROM player_achievements WHERE player_uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        if (!pd.getAchievements().isEmpty()) {
            try (PreparedStatement ins = getConnection().prepareStatement(
                    "INSERT INTO player_achievements (player_uuid, achievement_id, unlocked_at) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Long> e : pd.getAchievements().entrySet()) {
                    ins.setString(1, uuid.toString());
                    ins.setString(2, e.getKey());
                    ins.setLong(3, e.getValue());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        }
    }

    // ==========================================
    // AUDIT LOG
    // ==========================================

    @Override
    public List<AuditManager.AuditEntry> loadAuditEntries() {
        List<AuditManager.AuditEntry> result = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM audit_log ORDER BY timestamp ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long timestamp = rs.getLong("timestamp");
                AuditManager.AuditAction action = AuditManager.AuditAction.valueOf(rs.getString("action"));
                UUID playerUUID = parseUuid(rs.getString("player_uuid"));
                String playerName = rs.getString("player_name");
                String details = rs.getString("details");
                double value = rs.getDouble("value");
                result.add(new AuditManager.AuditEntry(timestamp, action, playerUUID, playerName, details, value));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar audit log (SQLite)", e);
        }
        return result;
    }

    @Override
    public void saveAuditEntries(List<AuditManager.AuditEntry> entries) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM audit_log");
                }
                String sql = "INSERT INTO audit_log (timestamp, action, player_uuid, player_name, details, value) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (AuditManager.AuditEntry e : entries) {
                        ps.setLong(1, e.timestamp);
                        ps.setString(2, e.action.name());
                        ps.setString(3, e.playerUUID != null ? e.playerUUID.toString() : null);
                        ps.setString(4, e.playerName);
                        ps.setString(5, e.details);
                        ps.setDouble(6, e.value);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar audit log (SQLite)", e);
        }
    }

    // ==========================================
    // MAIL
    // ==========================================

    @Override
    public Map<UUID, List<MailManager.MailEntry>> loadAllMail() {
        Map<UUID, List<MailManager.MailEntry>> result = new ConcurrentHashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM mail ORDER BY timestamp ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID target = UUID.fromString(rs.getString("target_uuid"));
                UUID sender = UUID.fromString(rs.getString("sender_uuid"));
                String senderName = rs.getString("sender_name");
                String message = rs.getString("message");
                long timestamp = rs.getLong("timestamp");
                boolean read = rs.getInt("is_read") == 1;

                result.computeIfAbsent(target, k -> new ArrayList<>())
                        .add(new MailManager.MailEntry(sender, senderName, message, timestamp, read));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar mail (SQLite)", e);
        }
        return result;
    }

    @Override
    public void saveAllMail(Map<UUID, List<MailManager.MailEntry>> mailMap) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM mail");
                }
                String sql = "INSERT INTO mail (target_uuid, sender_uuid, sender_name, message, timestamp, is_read) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map.Entry<UUID, List<MailManager.MailEntry>> entry : mailMap.entrySet()) {
                        for (MailManager.MailEntry mail : entry.getValue()) {
                            ps.setString(1, entry.getKey().toString());
                            ps.setString(2, mail.senderUUID.toString());
                            ps.setString(3, mail.senderName);
                            ps.setString(4, mail.message);
                            ps.setLong(5, mail.timestamp);
                            ps.setInt(6, mail.read ? 1 : 0);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar mail (SQLite)", e);
        }
    }

    // ==========================================
    // BOUNTIES
    // ==========================================

    @Override
    public Map<UUID, BountyManager.Bounty> loadBounties() {
        Map<UUID, BountyManager.Bounty> result = new ConcurrentHashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM bounties");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID target = UUID.fromString(rs.getString("target_uuid"));
                String name = rs.getString("target_name");
                BountyManager.Bounty bounty = new BountyManager.Bounty(target, name);
                bounty.totalValue = rs.getDouble("total_value");
                bounty.lastUpdated = rs.getLong("last_updated");
                result.put(target, bounty);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar bounties (SQLite)", e);
        }

        // Contributors
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM bounty_contributors");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID target = UUID.fromString(rs.getString("target_uuid"));
                BountyManager.Bounty bounty = result.get(target);
                if (bounty != null) {
                    bounty.contributors.put(UUID.fromString(rs.getString("contributor_uuid")),
                            rs.getDouble("amount"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar bounty contributors (SQLite)", e);
        }

        return result;
    }

    @Override
    public void saveBounties(Map<UUID, BountyManager.Bounty> bounties) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM bounties");
                }
                String bSql = "INSERT INTO bounties (target_uuid, target_name, total_value, last_updated) VALUES (?, ?, ?, ?)";
                String cSql = "INSERT INTO bounty_contributors (target_uuid, contributor_uuid, amount) VALUES (?, ?, ?)";
                try (PreparedStatement bps = conn.prepareStatement(bSql);
                     PreparedStatement cps = conn.prepareStatement(cSql)) {
                    for (Map.Entry<UUID, BountyManager.Bounty> entry : bounties.entrySet()) {
                        BountyManager.Bounty b = entry.getValue();
                        bps.setString(1, entry.getKey().toString());
                        bps.setString(2, b.targetName);
                        bps.setDouble(3, b.totalValue);
                        bps.setLong(4, b.lastUpdated);
                        bps.addBatch();

                        for (Map.Entry<UUID, Double> c : b.contributors.entrySet()) {
                            cps.setString(1, entry.getKey().toString());
                            cps.setString(2, c.getKey().toString());
                            cps.setDouble(3, c.getValue());
                            cps.addBatch();
                        }
                    }
                    bps.executeBatch();
                    cps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar bounties (SQLite)", e);
        }
    }

    // ==========================================
    // VOTES
    // ==========================================

    @Override
    public Map<String, VoteManager.KingdomVote> loadVotes() {
        Map<String, VoteManager.KingdomVote> result = new ConcurrentHashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM votes");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String kingdomId = rs.getString("kingdom_id");
                String question = rs.getString("question");
                UUID creator = UUID.fromString(rs.getString("creator_uuid"));
                long createdAt = rs.getLong("created_at");
                long expiresAt = rs.getLong("expires_at");
                result.put(kingdomId, new VoteManager.KingdomVote(kingdomId, question, creator, createdAt, expiresAt));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar votes (SQLite)", e);
        }

        // Vote entries
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM vote_entries");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String kingdomId = rs.getString("kingdom_id");
                VoteManager.KingdomVote vote = result.get(kingdomId);
                if (vote != null) {
                    vote.votes.put(UUID.fromString(rs.getString("player_uuid")),
                            rs.getInt("vote_value") == 1);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar vote entries (SQLite)", e);
        }

        return result;
    }

    @Override
    public void saveVotes(Map<String, VoteManager.KingdomVote> votes) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM votes");
                }
                String vSql = "INSERT INTO votes (kingdom_id, question, creator_uuid, created_at, expires_at) VALUES (?, ?, ?, ?, ?)";
                String eSql = "INSERT INTO vote_entries (kingdom_id, player_uuid, vote_value) VALUES (?, ?, ?)";
                try (PreparedStatement vps = conn.prepareStatement(vSql);
                     PreparedStatement eps = conn.prepareStatement(eSql)) {
                    for (Map.Entry<String, VoteManager.KingdomVote> entry : votes.entrySet()) {
                        VoteManager.KingdomVote v = entry.getValue();
                        vps.setString(1, v.kingdomId);
                        vps.setString(2, v.question);
                        vps.setString(3, v.creatorUUID.toString());
                        vps.setLong(4, v.createdAt);
                        vps.setLong(5, v.expiresAt);
                        vps.addBatch();

                        for (Map.Entry<UUID, Boolean> ve : v.votes.entrySet()) {
                            eps.setString(1, v.kingdomId);
                            eps.setString(2, ve.getKey().toString());
                            eps.setInt(3, ve.getValue() ? 1 : 0);
                            eps.addBatch();
                        }
                    }
                    vps.executeBatch();
                    eps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar votes (SQLite)", e);
        }
    }

    // ==========================================
    // AUCTIONS (serialização de ItemStack via Base64)
    // ==========================================

    @Override
    public List<AuctionManager.Auction> loadAuctions() {
        List<AuctionManager.Auction> result = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM auctions");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                UUID seller = UUID.fromString(rs.getString("seller_uuid"));
                String sellerName = rs.getString("seller_name");
                ItemStack item = itemFromBase64(rs.getString("item_data"));
                if (item == null) continue;
                double minPrice = rs.getDouble("min_price");
                long endTime = rs.getLong("end_time");

                AuctionManager.Auction auction = new AuctionManager.Auction(
                        id, seller, sellerName, item, minPrice, endTime);
                auction.currentBid = rs.getDouble("current_bid");
                String bidder = rs.getString("current_bidder");
                auction.currentBidder = bidder != null ? UUID.fromString(bidder) : null;
                auction.currentBidderName = rs.getString("current_bidder_name");
                auction.bidCount = rs.getInt("bid_count");
                result.add(auction);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar auctions (SQLite)", e);
        }
        return result;
    }

    @Override
    public void saveAuctions(List<AuctionManager.Auction> auctions) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM auctions");
                }
                String sql = """
                    INSERT INTO auctions (id, seller_uuid, seller_name, item_data, min_price,
                        end_time, current_bid, current_bidder, current_bidder_name, bid_count)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (AuctionManager.Auction a : auctions) {
                        ps.setString(1, a.id);
                        ps.setString(2, a.sellerUUID.toString());
                        ps.setString(3, a.sellerName);
                        ps.setString(4, itemToBase64(a.item));
                        ps.setDouble(5, a.minPrice);
                        ps.setLong(6, a.endTime);
                        ps.setDouble(7, a.currentBid);
                        ps.setString(8, a.currentBidder != null ? a.currentBidder.toString() : null);
                        ps.setString(9, a.currentBidderName);
                        ps.setInt(10, a.bidCount);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar auctions (SQLite)", e);
        }
    }

    @Override
    public Map<UUID, List<Map<String, Object>>> loadPendingCollections() {
        Map<UUID, List<Map<String, Object>>> result = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM pending_collections");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player_uuid"));
                Map<String, Object> entry = new HashMap<>();
                String itemData = rs.getString("item_data");
                if (itemData != null) {
                    entry.put("item", itemFromBase64(itemData));
                }
                entry.put("money", rs.getDouble("money"));
                entry.put("description", rs.getString("description"));
                result.computeIfAbsent(player, k -> new ArrayList<>()).add(entry);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar pending collections (SQLite)", e);
        }
        return result;
    }

    @Override
    public void savePendingCollections(Map<UUID, List<Map<String, Object>>> pendingMap) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM pending_collections");
                }
                String sql = "INSERT INTO pending_collections (player_uuid, item_data, money, description) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map.Entry<UUID, List<Map<String, Object>>> entry : pendingMap.entrySet()) {
                        for (Map<String, Object> coll : entry.getValue()) {
                            ps.setString(1, entry.getKey().toString());
                            Object item = coll.get("item");
                            ps.setString(2, item instanceof ItemStack ? itemToBase64((ItemStack) item) : null);
                            ps.setDouble(3, coll.containsKey("money") ? ((Number) coll.get("money")).doubleValue() : 0);
                            ps.setString(4, (String) coll.get("description"));
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar pending collections (SQLite)", e);
        }
    }

    // ==========================================
    // PRICE HISTORY
    // ==========================================

    @Override
    public Map<String, List<PriceHistoryManager.PriceSnapshot>> loadPriceHistory() {
        Map<String, List<PriceHistoryManager.PriceSnapshot>> result = new ConcurrentHashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM price_history ORDER BY timestamp ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String itemId = rs.getString("item_id");
                long timestamp = rs.getLong("timestamp");
                double buy = rs.getDouble("buy_price");
                double sell = rs.getDouble("sell_price");
                result.computeIfAbsent(itemId, k -> new ArrayList<>())
                        .add(new PriceHistoryManager.PriceSnapshot(timestamp, buy, sell));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar price history (SQLite)", e);
        }
        return result;
    }

    @Override
    public void savePriceHistory(Map<String, List<PriceHistoryManager.PriceSnapshot>> history) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM price_history");
                }
                String sql = "INSERT INTO price_history (item_id, timestamp, buy_price, sell_price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, List<PriceHistoryManager.PriceSnapshot>> entry : history.entrySet()) {
                        for (PriceHistoryManager.PriceSnapshot snap : entry.getValue()) {
                            ps.setString(1, entry.getKey());
                            ps.setLong(2, snap.timestamp);
                            ps.setDouble(3, snap.buyPrice);
                            ps.setDouble(4, snap.sellPrice);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar price history (SQLite)", e);
        }
    }

    // ==========================================
    // UTILITÁRIOS
    // ==========================================

    private UUID parseUuid(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Serializa um ItemStack para Base64 usando a API do Bukkit.
     */
    private String itemToBase64(ItemStack item) {
        if (item == null) return null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput =
                    new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage] Erro ao serializar ItemStack: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserializa um ItemStack de Base64.
     */
    private ItemStack itemFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput =
                    new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage] Erro ao deserializar ItemStack: " + e.getMessage());
            return null;
        }
    }
}
