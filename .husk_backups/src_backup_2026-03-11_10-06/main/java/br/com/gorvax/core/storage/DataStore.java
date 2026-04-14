package br.com.gorvax.core.storage;

import br.com.gorvax.core.managers.AuditManager;
import br.com.gorvax.core.managers.AuctionManager;
import br.com.gorvax.core.managers.BountyManager;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.MailManager;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.PriceHistoryManager;
import br.com.gorvax.core.managers.VoteManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * B18 — Interface de abstração de persistência.
 * Define contratos de CRUD por domínio para diferentes backends
 * (YAML, SQLite, MySQL).
 */
public interface DataStore {

    /**
     * Tipo de backend de armazenamento.
     */
    enum StorageType {
        YAML, SQLITE, MYSQL
    }

    // === Ciclo de vida ===

    /** Inicializa o backend (cria tabelas, abre conexões, etc). */
    void init() throws Exception;

    /** Encerra o backend (fecha conexões, flush final). */
    void shutdown();

    /** Retorna o tipo de backend. */
    StorageType getType();

    // === Claims ===

    /** Carrega todas as claims do armazenamento. */
    List<Claim> loadClaims();

    /** Salva todas as claims (substituição completa). */
    void saveClaims(List<Claim> claims);

    // === Player Data ===

    /** Carrega os dados de um jogador. Retorna null se não existir. */
    PlayerData loadPlayerData(UUID uuid);

    /** Carrega todos os dados de jogadores (para migração). */
    Map<UUID, PlayerData> loadAllPlayerData();

    /** Salva os dados de um jogador. */
    void savePlayerData(UUID uuid, PlayerData data);

    /** Salva todos os dados de jogadores (batch). */
    void saveAllPlayerData(Map<UUID, PlayerData> dataMap);

    // === Audit Log ===

    /** Carrega todas as entradas de auditoria. */
    List<AuditManager.AuditEntry> loadAuditEntries();

    /** Salva todas as entradas de auditoria (substituição completa). */
    void saveAuditEntries(List<AuditManager.AuditEntry> entries);

    // === Mail ===

    /** Carrega todas as cartas agrupadas por destinatário. */
    Map<UUID, List<MailManager.MailEntry>> loadAllMail();

    /** Salva todas as cartas (substituição completa). */
    void saveAllMail(Map<UUID, List<MailManager.MailEntry>> mailMap);

    // === Bounties ===

    /** Carrega todas as bounties. */
    Map<UUID, BountyManager.Bounty> loadBounties();

    /** Salva todas as bounties (substituição completa). */
    void saveBounties(Map<UUID, BountyManager.Bounty> bounties);

    // === Votes ===

    /** Carrega todas as votações ativas. */
    Map<String, VoteManager.KingdomVote> loadVotes();

    /** Salva todas as votações (substituição completa). */
    void saveVotes(Map<String, VoteManager.KingdomVote> votes);

    // === Auctions ===

    /** Carrega todos os leilões ativos. */
    List<AuctionManager.Auction> loadAuctions();

    /** Salva todos os leilões (substituição completa). */
    void saveAuctions(List<AuctionManager.Auction> auctions);

    /** Carrega as coletas pendentes de todos os jogadores. */
    Map<UUID, List<Map<String, Object>>> loadPendingCollections();

    /** Salva as coletas pendentes de todos os jogadores. */
    void savePendingCollections(Map<UUID, List<Map<String, Object>>> pendingMap);

    // === Price History ===

    /** Carrega o histórico de preços por item. */
    Map<String, List<PriceHistoryManager.PriceSnapshot>> loadPriceHistory();

    /** Salva o histórico de preços (substituição completa). */
    void savePriceHistory(Map<String, List<PriceHistoryManager.PriceSnapshot>> history);
}
