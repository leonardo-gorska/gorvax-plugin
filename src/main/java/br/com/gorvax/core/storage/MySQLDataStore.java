package br.com.gorvax.core.storage;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.*;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * B18 — Implementação MySQL do DataStore.
 * Usa HikariCP para connection pooling.
 * Mesmo schema do SQLite com adaptações de tipos MySQL.
 */
public class MySQLDataStore implements DataStore {

    private final GorvaxCore plugin;
    private HikariDataSource dataSource;

    public MySQLDataStore(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() throws Exception {
        String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin.getConfig().getString("storage.mysql.database", "gorvax");
        String username = plugin.getConfig().getString("storage.mysql.username", "root");
        String password = plugin.getConfig().getString("storage.mysql.password", "");
        int poolSize = plugin.getConfig().getInt("storage.mysql.pool_size", 10);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8&useUnicode=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("GorvaxCore-MySQL");

        // Otimizações de performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
        createTables();
        plugin.getLogger().info("[Storage] Backend MySQL inicializado: " + host + ":" + port + "/" + database);
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public StorageType getType() {
        return StorageType.MYSQL;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ==========================================
    // SCHEMA (MySQL adaptado)
    // ==========================================

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claims (
                    id VARCHAR(64) PRIMARY KEY,
                    owner_uuid VARCHAR(36) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    min_x INT NOT NULL,
                    min_z INT NOT NULL,
                    max_x INT NOT NULL,
                    max_z INT NOT NULL,
                    is_kingdom TINYINT DEFAULT 0,
                    name VARCHAR(128),
                    kingdom_name VARCHAR(128),
                    type VARCHAR(32) DEFAULT 'NORMAL',
                    pvp TINYINT DEFAULT 0,
                    residents_pvp TINYINT DEFAULT 0,
                    residents_pvp_outside TINYINT DEFAULT 0,
                    is_public TINYINT DEFAULT 1,
                    residents_build TINYINT DEFAULT 0,
                    residents_container TINYINT DEFAULT 0,
                    residents_switch TINYINT DEFAULT 0,
                    parent_kingdom_id VARCHAR(64),
                    welcome_color VARCHAR(16) DEFAULT '§b',
                    chat_color VARCHAR(16) DEFAULT '§f',
                    tag_color VARCHAR(16) DEFAULT '§e',
                    tag VARCHAR(32),
                    tax DOUBLE DEFAULT 5.0,
                    enter_title VARCHAR(256),
                    enter_subtitle VARCHAR(256),
                    exit_title VARCHAR(256),
                    exit_subtitle VARCHAR(256),
                    INDEX idx_owner (owner_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claim_trusts (
                    claim_id VARCHAR(64) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    trust_type VARCHAR(32) NOT NULL,
                    PRIMARY KEY (claim_id, player_uuid, trust_type),
                    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subplots (
                    id VARCHAR(64) NOT NULL,
                    claim_id VARCHAR(64) NOT NULL,
                    name VARCHAR(128),
                    min_x INT NOT NULL,
                    min_z INT NOT NULL,
                    max_x INT NOT NULL,
                    max_z INT NOT NULL,
                    price DOUBLE DEFAULT 0,
                    rent_price DOUBLE DEFAULT 0,
                    for_sale TINYINT DEFAULT 0,
                    for_rent TINYINT DEFAULT 0,
                    owner_uuid VARCHAR(36),
                    renter_uuid VARCHAR(36),
                    rent_expire BIGINT DEFAULT 0,
                    PRIMARY KEY (id, claim_id),
                    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subplot_trusts (
                    subplot_id VARCHAR(64) NOT NULL,
                    claim_id VARCHAR(64) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    trust_type VARCHAR(32) NOT NULL,
                    PRIMARY KEY (subplot_id, claim_id, player_uuid, trust_type),
                    FOREIGN KEY (subplot_id, claim_id) REFERENCES subplots(id, claim_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    blocks INT DEFAULT 100,
                    king_rank TINYINT DEFAULT 0,
                    first_join BIGINT DEFAULT 0,
                    total_play_time BIGINT DEFAULT 0,
                    last_login BIGINT DEFAULT 0,
                    total_blocks_broken INT DEFAULT 0,
                    total_blocks_placed INT DEFAULT 0,
                    total_kills INT DEFAULT 0,
                    total_deaths INT DEFAULT 0,
                    bosses_killed INT DEFAULT 0,
                    boss_top_damage INT DEFAULT 0,
                    total_money_earned DOUBLE DEFAULT 0,
                    total_money_spent DOUBLE DEFAULT 0,
                    active_title VARCHAR(128) DEFAULT '',
                    border_sound TINYINT DEFAULT 1,
                    tutorial_step INT DEFAULT 0,
                    has_received_kit TINYINT DEFAULT 0,
                    tutorial_completed TINYINT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_titles (
                    player_uuid VARCHAR(36) NOT NULL,
                    title VARCHAR(128) NOT NULL,
                    PRIMARY KEY (player_uuid, title),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_achievements (
                    player_uuid VARCHAR(36) NOT NULL,
                    achievement_id VARCHAR(64) NOT NULL,
                    unlocked_at BIGINT DEFAULT 0,
                    PRIMARY KEY (player_uuid, achievement_id),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    timestamp BIGINT NOT NULL,
                    action VARCHAR(32) NOT NULL,
                    player_uuid VARCHAR(36),
                    player_name VARCHAR(64),
                    details TEXT,
                    value DOUBLE DEFAULT 0,
                    INDEX idx_player (player_uuid),
                    INDEX idx_timestamp (timestamp)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mail (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    target_uuid VARCHAR(36) NOT NULL,
                    sender_uuid VARCHAR(36) NOT NULL,
                    sender_name VARCHAR(64) NOT NULL,
                    message TEXT NOT NULL,
                    timestamp BIGINT NOT NULL,
                    is_read TINYINT DEFAULT 0,
                    INDEX idx_target (target_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bounties (
                    target_uuid VARCHAR(36) PRIMARY KEY,
                    target_name VARCHAR(64) NOT NULL,
                    total_value DOUBLE DEFAULT 0,
                    last_updated BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bounty_contributors (
                    target_uuid VARCHAR(36) NOT NULL,
                    contributor_uuid VARCHAR(36) NOT NULL,
                    amount DOUBLE DEFAULT 0,
                    PRIMARY KEY (target_uuid, contributor_uuid),
                    FOREIGN KEY (target_uuid) REFERENCES bounties(target_uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS votes (
                    kingdom_id VARCHAR(64) PRIMARY KEY,
                    question TEXT NOT NULL,
                    creator_uuid VARCHAR(36) NOT NULL,
                    created_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vote_entries (
                    kingdom_id VARCHAR(64) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    vote_value TINYINT NOT NULL,
                    PRIMARY KEY (kingdom_id, player_uuid),
                    FOREIGN KEY (kingdom_id) REFERENCES votes(kingdom_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id VARCHAR(64) PRIMARY KEY,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(64) NOT NULL,
                    item_data LONGTEXT NOT NULL,
                    min_price DOUBLE NOT NULL,
                    end_time BIGINT NOT NULL,
                    current_bid DOUBLE DEFAULT 0,
                    current_bidder VARCHAR(36),
                    current_bidder_name VARCHAR(64),
                    bid_count INT DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pending_collections (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    item_data LONGTEXT,
                    money DOUBLE DEFAULT 0,
                    description VARCHAR(256),
                    INDEX idx_player (player_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS price_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    item_id VARCHAR(128) NOT NULL,
                    timestamp BIGINT NOT NULL,
                    buy_price DOUBLE NOT NULL,
                    sell_price DOUBLE NOT NULL,
                    INDEX idx_item (item_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
        }
    }

    // ==========================================
    // CLAIMS — Delegam para métodos comuns usando getConnection()
    // ==========================================

    @Override
    public List<Claim> loadClaims() {
        List<Claim> result = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // 1. Carregar claims
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM claims");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Claim claim = readClaimFromResultSet(rs);
                    result.add(claim);
                }
            }

            // 2. Trusts
            Map<String, Claim> claimMap = new HashMap<>();
            for (Claim c : result) claimMap.put(c.getId(), c);

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM claim_trusts");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Claim claim = claimMap.get(rs.getString("claim_id"));
                    if (claim == null) continue;
                    claim.addTrust(UUID.fromString(rs.getString("player_uuid")),
                            Claim.TrustType.valueOf(rs.getString("trust_type")));
                }
            }

            // 3. SubPlots
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM subplots");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Claim claim = claimMap.get(rs.getString("claim_id"));
                    if (claim == null) continue;
                    claim.addSubPlot(readSubPlotFromResultSet(rs));
                }
            }

            // 4. SubPlot Trusts
            Map<String, Map<String, SubPlot>> plotMap = new HashMap<>();
            for (Claim c : result) {
                Map<String, SubPlot> subs = new HashMap<>();
                for (SubPlot sp : c.getSubPlots()) subs.put(sp.getId(), sp);
                plotMap.put(c.getId(), subs);
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM subplot_trusts");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, SubPlot> subs = plotMap.get(rs.getString("claim_id"));
                    if (subs == null) continue;
                    SubPlot plot = subs.get(rs.getString("subplot_id"));
                    if (plot == null) continue;
                    plot.addTrust(UUID.fromString(rs.getString("player_uuid")),
                            Claim.TrustType.valueOf(rs.getString("trust_type")));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar claims (MySQL)", e);
        }
        return result;
    }

    private Claim readClaimFromResultSet(ResultSet rs) throws SQLException {
        Claim claim = new Claim(
                rs.getString("id"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("world"),
                rs.getInt("min_x"), rs.getInt("min_z"),
                rs.getInt("max_x"), rs.getInt("max_z"),
                rs.getInt("is_kingdom") == 1,
                rs.getString("name"));
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
        try { claim.setType(Claim.Type.valueOf(rs.getString("type"))); } catch (Exception e) { plugin.getLogger().fine("Tipo de claim inválido (MySQL): " + e.getMessage()); }
        claim.setPvp(rs.getInt("pvp") == 1);
        claim.setResidentsPvp(rs.getInt("residents_pvp") == 1);
        claim.setResidentsPvpOutside(rs.getInt("residents_pvp_outside") == 1);
        claim.setPublic(rs.getInt("is_public") == 1);
        claim.setResidentsBuild(rs.getInt("residents_build") == 1);
        claim.setResidentsContainer(rs.getInt("residents_container") == 1);
        claim.setResidentsSwitch(rs.getInt("residents_switch") == 1);
        claim.setParentKingdomId(rs.getString("parent_kingdom_id"));
        return claim;
    }

    private SubPlot readSubPlotFromResultSet(ResultSet rs) throws SQLException {
        return new SubPlot(
                rs.getString("id"),
                rs.getString("name"),
                rs.getInt("min_x"), rs.getInt("min_z"),
                rs.getInt("max_x"), rs.getInt("max_z"),
                rs.getDouble("price"), rs.getDouble("rent_price"),
                rs.getInt("for_sale") == 1, rs.getInt("for_rent") == 1,
                parseUuid(rs.getString("owner_uuid")),
                parseUuid(rs.getString("renter_uuid")),
                rs.getLong("rent_expire"));
    }

    @Override
    public void saveClaims(List<Claim> claims) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM claims");
                }

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
                String stSql = "INSERT INTO subplot_trusts (subplot_id, claim_id, player_uuid, trust_type) VALUES (?, ?, ?, ?)";

                try (PreparedStatement cps = conn.prepareStatement(claimSql);
                     PreparedStatement tps = conn.prepareStatement(trustSql);
                     PreparedStatement sps = conn.prepareStatement(subplotSql);
                     PreparedStatement stps = conn.prepareStatement(stSql)) {

                    for (Claim c : claims) {
                        setClaimParams(cps, c);
                        cps.addBatch();

                        for (Map.Entry<UUID, Set<Claim.TrustType>> entry : c.getTrustedPlayers().entrySet()) {
                            for (Claim.TrustType t : entry.getValue()) {
                                tps.setString(1, c.getId());
                                tps.setString(2, entry.getKey().toString());
                                tps.setString(3, t.name());
                                tps.addBatch();
                            }
                        }

                        for (SubPlot sp : c.getSubPlots()) {
                            setSubPlotParams(sps, sp, c.getId());
                            sps.addBatch();

                            for (Map.Entry<UUID, Set<Claim.TrustType>> entry : sp.getTrustedPlayers().entrySet()) {
                                for (Claim.TrustType t : entry.getValue()) {
                                    stps.setString(1, sp.getId());
                                    stps.setString(2, c.getId());
                                    stps.setString(3, entry.getKey().toString());
                                    stps.setString(4, t.name());
                                    stps.addBatch();
                                }
                            }
                        }
                    }
                    cps.executeBatch();
                    tps.executeBatch();
                    sps.executeBatch();
                    stps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar claims (MySQL)", e);
        }
    }

    private void setClaimParams(PreparedStatement ps, Claim c) throws SQLException {
        ps.setString(1, c.getId());
        ps.setString(2, c.getOwner().toString());
        ps.setString(3, c.getWorldName());
        ps.setInt(4, c.getMinX());
        ps.setInt(5, c.getMinZ());
        ps.setInt(6, c.getMaxX());
        ps.setInt(7, c.getMaxZ());
        ps.setInt(8, c.isKingdom() ? 1 : 0);
        ps.setString(9, c.getName());
        ps.setString(10, c.getKingdomName());
        ps.setString(11, c.getType().name());
        ps.setInt(12, c.isPvp() ? 1 : 0);
        ps.setInt(13, c.isResidentsPvp() ? 1 : 0);
        ps.setInt(14, c.isResidentsPvpOutside() ? 1 : 0);
        ps.setInt(15, c.isPublic() ? 1 : 0);
        ps.setInt(16, c.isResidentsBuild() ? 1 : 0);
        ps.setInt(17, c.isResidentsContainer() ? 1 : 0);
        ps.setInt(18, c.isResidentsSwitch() ? 1 : 0);
        ps.setString(19, c.getParentKingdomId());
        ps.setString(20, c.getWelcomeColor());
        ps.setString(21, c.getChatColor());
        ps.setString(22, c.getTagColor());
        ps.setString(23, c.getTag());
        ps.setDouble(24, c.getTax());
        ps.setString(25, c.getEnterTitle());
        ps.setString(26, c.getEnterSubtitle());
        ps.setString(27, c.getExitTitle());
        ps.setString(28, c.getExitSubtitle());
    }

    private void setSubPlotParams(PreparedStatement ps, SubPlot sp, String claimId) throws SQLException {
        ps.setString(1, sp.getId());
        ps.setString(2, claimId);
        ps.setString(3, sp.getName());
        ps.setInt(4, sp.getMinX());
        ps.setInt(5, sp.getMinZ());
        ps.setInt(6, sp.getMaxX());
        ps.setInt(7, sp.getMaxZ());
        ps.setDouble(8, sp.getPrice());
        ps.setDouble(9, sp.getRentPrice());
        ps.setInt(10, sp.isForSale() ? 1 : 0);
        ps.setInt(11, sp.isForRent() ? 1 : 0);
        ps.setString(12, sp.getOwner() != null ? sp.getOwner().toString() : null);
        ps.setString(13, sp.getRenter() != null ? sp.getRenter().toString() : null);
        ps.setLong(14, sp.getRentExpire());
    }

    // ==========================================
    // PLAYER DATA
    // ==========================================

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                PlayerData pd = readPlayerFromResultSet(rs, uuid);
                loadPlayerExtras(conn, uuid, pd);
                return pd;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar player (MySQL): " + uuid, e);
            return null;
        }
    }

    @Override
    public Map<UUID, PlayerData> loadAllPlayerData() {
        Map<UUID, PlayerData> result = new LinkedHashMap<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM players");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    result.put(uuid, readPlayerFromResultSet(rs, uuid));
                }
            }
            // Batch load extras
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_titles");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    PlayerData pd = result.get(uuid);
                    if (pd != null) pd.getUnlockedTitles().add(rs.getString("title"));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_achievements");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    PlayerData pd = result.get(uuid);
                    if (pd != null) pd.getAchievements().put(rs.getString("achievement_id"), rs.getLong("unlocked_at"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar todos os players (MySQL)", e);
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

        // B4 — Tutorial Interativo + Welcome Kit
        pd.setTutorialStep(rs.getInt("tutorial_step"));
        pd.setHasReceivedKit(rs.getInt("has_received_kit") == 1);
        pd.setTutorialCompleted(rs.getInt("tutorial_completed") == 1);

        return pd;
    }

    private void loadPlayerExtras(Connection conn, UUID uuid, PlayerData pd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT title FROM player_titles WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> titles = new HashSet<>();
                while (rs.next()) titles.add(rs.getString("title"));
                pd.setUnlockedTitles(titles);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT achievement_id, unlocked_at FROM player_achievements WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Long> ach = new HashMap<>();
                while (rs.next()) ach.put(rs.getString("achievement_id"), rs.getLong("unlocked_at"));
                pd.setAchievements(ach);
            }
        }
    }

    @Override
    public void savePlayerData(UUID uuid, PlayerData pd) {
        try (Connection conn = getConnection()) {
            upsertPlayer(conn, uuid, pd);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar player (MySQL): " + uuid, e);
        }
    }

    @Override
    public void saveAllPlayerData(Map<UUID, PlayerData> dataMap) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Map.Entry<UUID, PlayerData> entry : dataMap.entrySet()) {
                    upsertPlayer(conn, entry.getKey(), entry.getValue());
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar todos os players (MySQL)", e);
        }
    }

    private void upsertPlayer(Connection conn, UUID uuid, PlayerData pd) throws SQLException {
        String sql = """
            INSERT INTO players (uuid, blocks, king_rank, first_join, total_play_time, last_login,
                total_blocks_broken, total_blocks_placed, total_kills, total_deaths,
                bosses_killed, boss_top_damage, total_money_earned, total_money_spent,
                active_title, border_sound, tutorial_step, has_received_kit, tutorial_completed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                blocks=VALUES(blocks), king_rank=VALUES(king_rank), first_join=VALUES(first_join),
                total_play_time=VALUES(total_play_time), last_login=VALUES(last_login),
                total_blocks_broken=VALUES(total_blocks_broken), total_blocks_placed=VALUES(total_blocks_placed),
                total_kills=VALUES(total_kills), total_deaths=VALUES(total_deaths),
                bosses_killed=VALUES(bosses_killed), boss_top_damage=VALUES(boss_top_damage),
                total_money_earned=VALUES(total_money_earned), total_money_spent=VALUES(total_money_spent),
                active_title=VALUES(active_title), border_sound=VALUES(border_sound),
                tutorial_step=VALUES(tutorial_step), has_received_kit=VALUES(has_received_kit),
                tutorial_completed=VALUES(tutorial_completed)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

        try (PreparedStatement del = conn.prepareStatement("DELETE FROM player_titles WHERE player_uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        if (!pd.getUnlockedTitles().isEmpty()) {
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO player_titles (player_uuid, title) VALUES (?, ?)")) {
                for (String title : pd.getUnlockedTitles()) {
                    ins.setString(1, uuid.toString());
                    ins.setString(2, title);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        }

        try (PreparedStatement del = conn.prepareStatement("DELETE FROM player_achievements WHERE player_uuid = ?")) {
            del.setString(1, uuid.toString());
            del.executeUpdate();
        }
        if (!pd.getAchievements().isEmpty()) {
            try (PreparedStatement ins = conn.prepareStatement(
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
    // AUDIT, MAIL, BOUNTIES, VOTES, AUCTIONS, PRICE HISTORY
    // Mesma lógica do SQLite, mas usando try-with-resources no Connection pool
    // ==========================================

    @Override
    public List<AuditManager.AuditEntry> loadAuditEntries() {
        List<AuditManager.AuditEntry> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM audit_log ORDER BY timestamp ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new AuditManager.AuditEntry(
                        rs.getLong("timestamp"),
                        AuditManager.AuditAction.valueOf(rs.getString("action")),
                        parseUuid(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("details"),
                        rs.getDouble("value")));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar audit log (MySQL)", e);
        }
        return result;
    }

    @Override
    public void saveAuditEntries(List<AuditManager.AuditEntry> entries) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM audit_log"); }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO audit_log (timestamp, action, player_uuid, player_name, details, value) VALUES (?, ?, ?, ?, ?, ?)")) {
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
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar audit log (MySQL)", e);
        }
    }

    @Override
    public Map<UUID, List<MailManager.MailEntry>> loadAllMail() {
        Map<UUID, List<MailManager.MailEntry>> result = new ConcurrentHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM mail ORDER BY timestamp ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID target = UUID.fromString(rs.getString("target_uuid"));
                result.computeIfAbsent(target, k -> new ArrayList<>())
                        .add(new MailManager.MailEntry(
                                UUID.fromString(rs.getString("sender_uuid")),
                                rs.getString("sender_name"),
                                rs.getString("message"),
                                rs.getLong("timestamp"),
                                rs.getInt("is_read") == 1));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar mail (MySQL)", e);
        }
        return result;
    }

    @Override
    public void saveAllMail(Map<UUID, List<MailManager.MailEntry>> mailMap) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM mail"); }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO mail (target_uuid, sender_uuid, sender_name, message, timestamp, is_read) VALUES (?, ?, ?, ?, ?, ?)")) {
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
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar mail (MySQL)", e);
        }
    }

    @Override
    public Map<UUID, BountyManager.Bounty> loadBounties() {
        Map<UUID, BountyManager.Bounty> result = new ConcurrentHashMap<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM bounties");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID target = UUID.fromString(rs.getString("target_uuid"));
                    BountyManager.Bounty b = new BountyManager.Bounty(target, rs.getString("target_name"));
                    b.totalValue = rs.getDouble("total_value");
                    b.lastUpdated = rs.getLong("last_updated");
                    result.put(target, b);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM bounty_contributors");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BountyManager.Bounty b = result.get(UUID.fromString(rs.getString("target_uuid")));
                    if (b != null) b.contributors.put(UUID.fromString(rs.getString("contributor_uuid")),
                            rs.getDouble("amount"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar bounties (MySQL)", e);
        }
        return result;
    }

    @Override
    public void saveBounties(Map<UUID, BountyManager.Bounty> bounties) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM bounties"); }
                try (PreparedStatement bps = conn.prepareStatement(
                        "INSERT INTO bounties (target_uuid, target_name, total_value, last_updated) VALUES (?, ?, ?, ?)");
                     PreparedStatement cps = conn.prepareStatement(
                             "INSERT INTO bounty_contributors (target_uuid, contributor_uuid, amount) VALUES (?, ?, ?)")) {
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
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar bounties (MySQL)", e);
        }
    }

    @Override
    public Map<String, VoteManager.KingdomVote> loadVotes() {
        Map<String, VoteManager.KingdomVote> result = new ConcurrentHashMap<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM votes");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String kid = rs.getString("kingdom_id");
                    result.put(kid, new VoteManager.KingdomVote(kid, rs.getString("question"),
                            UUID.fromString(rs.getString("creator_uuid")),
                            rs.getLong("created_at"), rs.getLong("expires_at")));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM vote_entries");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VoteManager.KingdomVote v = result.get(rs.getString("kingdom_id"));
                    if (v != null) v.votes.put(UUID.fromString(rs.getString("player_uuid")),
                            rs.getInt("vote_value") == 1);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar votes (MySQL)", e);
        }
        return result;
    }

    @Override
    public void saveVotes(Map<String, VoteManager.KingdomVote> votes) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM votes"); }
                try (PreparedStatement vps = conn.prepareStatement(
                        "INSERT INTO votes (kingdom_id, question, creator_uuid, created_at, expires_at) VALUES (?, ?, ?, ?, ?)");
                     PreparedStatement eps = conn.prepareStatement(
                             "INSERT INTO vote_entries (kingdom_id, player_uuid, vote_value) VALUES (?, ?, ?)")) {
                    for (VoteManager.KingdomVote v : votes.values()) {
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
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar votes (MySQL)", e);
        }
    }

    @Override
    public List<AuctionManager.Auction> loadAuctions() {
        List<AuctionManager.Auction> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM auctions");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemStack item = itemFromBase64(rs.getString("item_data"));
                if (item == null) continue;
                AuctionManager.Auction a = new AuctionManager.Auction(
                        rs.getString("id"), UUID.fromString(rs.getString("seller_uuid")),
                        rs.getString("seller_name"), item, rs.getDouble("min_price"), rs.getLong("end_time"));
                a.currentBid = rs.getDouble("current_bid");
                String bidder = rs.getString("current_bidder");
                a.currentBidder = bidder != null ? UUID.fromString(bidder) : null;
                a.currentBidderName = rs.getString("current_bidder_name");
                a.bidCount = rs.getInt("bid_count");
                result.add(a);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar auctions (MySQL)", e);
        }
        return result;
    }

    @Override
    public void saveAuctions(List<AuctionManager.Auction> auctions) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM auctions"); }
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO auctions (id, seller_uuid, seller_name, item_data, min_price,
                            end_time, current_bid, current_bidder, current_bidder_name, bid_count)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
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
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar auctions (MySQL)", e);
        }
    }

    @Override
    public Map<UUID, List<Map<String, Object>>> loadPendingCollections() {
        Map<UUID, List<Map<String, Object>>> result = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM pending_collections");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player_uuid"));
                Map<String, Object> entry = new HashMap<>();
                String itemData = rs.getString("item_data");
                if (itemData != null) entry.put("item", itemFromBase64(itemData));
                entry.put("money", rs.getDouble("money"));
                entry.put("description", rs.getString("description"));
                result.computeIfAbsent(player, k -> new ArrayList<>()).add(entry);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar pending collections (MySQL)", e);
        }
        return result;
    }

    @Override
    public void savePendingCollections(Map<UUID, List<Map<String, Object>>> pendingMap) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM pending_collections"); }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO pending_collections (player_uuid, item_data, money, description) VALUES (?, ?, ?, ?)")) {
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
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar pending collections (MySQL)", e);
        }
    }

    @Override
    public Map<String, List<PriceHistoryManager.PriceSnapshot>> loadPriceHistory() {
        Map<String, List<PriceHistoryManager.PriceSnapshot>> result = new ConcurrentHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM price_history ORDER BY timestamp ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("item_id"), k -> new ArrayList<>())
                        .add(new PriceHistoryManager.PriceSnapshot(
                                rs.getLong("timestamp"), rs.getDouble("buy_price"), rs.getDouble("sell_price")));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao carregar price history (MySQL)", e);
        }
        return result;
    }

    @Override
    public void savePriceHistory(Map<String, List<PriceHistoryManager.PriceSnapshot>> history) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM price_history"); }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO price_history (item_id, timestamp, buy_price, sell_price) VALUES (?, ?, ?, ?)")) {
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
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Storage] Erro ao salvar price history (MySQL)", e);
        }
    }

    // ==========================================
    // UTILITÁRIOS
    // ==========================================

    private UUID parseUuid(String str) {
        if (str == null || str.isEmpty()) return null;
        try { return UUID.fromString(str); } catch (Exception e) { return null; }
    }

    private String itemToBase64(ItemStack item) {
        if (item == null) return null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out);
            dataOut.writeObject(item);
            dataOut.close();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) { return null; }
    }

    private ItemStack itemFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            BukkitObjectInputStream in =
                    new BukkitObjectInputStream(new ByteArrayInputStream(bytes));
            ItemStack item = (ItemStack) in.readObject();
            in.close();
            return item;
        } catch (Exception e) { return null; }
    }
}
