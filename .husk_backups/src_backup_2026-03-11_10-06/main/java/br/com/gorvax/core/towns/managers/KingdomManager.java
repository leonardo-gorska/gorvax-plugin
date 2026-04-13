package br.com.gorvax.core.towns.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.Relation;
import br.com.gorvax.core.managers.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class KingdomManager {

    private final GorvaxCore plugin;
    private File configFile;
    private FileConfiguration data;

    // Cache de performance
    private final Map<UUID, Claim> playerKingdomCache = new ConcurrentHashMap<>();
    private final ReentrantLock saveLock = new ReentrantLock();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private final Map<UUID, String> reverseMemberIndex = new ConcurrentHashMap<>(); // UUID -> KingdomID

    // B1.1 — Sistema de Convites
    public record Invite(String kingdomId, UUID inviter, long timestamp) {
    }

    private final Map<UUID, Invite> pendingInvites = new ConcurrentHashMap<>();

    // B7 — Diplomacia: Propostas de aliança pendentes (reinoAlvo -> proposta)
    public record AllianceProposal(String fromKingdomId, String toKingdomId, long timestamp) {
    }

    private final Map<String, AllianceProposal> pendingAlliances = new ConcurrentHashMap<>();

    public KingdomManager(GorvaxCore plugin) {
        this.plugin = plugin;
        setup();
        startSaveTask();
    }

    private void startSaveTask() {
        // Run SYNC to safely access 'data'
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (dirty.getAndSet(false)) {
                performAsyncSave();
            }
        }, 1200L, 1200L); // 60 segundos
    }

    private void performAsyncSave() {
        try {
            // Snapshot on Main Thread (Fast)
            final String dataString = data.saveToString();

            // Write to Disk Async (Slow IO)
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                saveToDisk(dataString);
            });
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao serializar dados do towns.yml: " + e.getMessage());
        }
    }

    private void saveToDisk(String content) {
        saveLock.lock();
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar towns.yml (Async): " + e.getMessage());
        } finally {
            saveLock.unlock();
        }
    }

    private void setup() {
        if (!plugin.getDataFolder().exists())
            plugin.getDataFolder().mkdirs();
        configFile = new File(plugin.getDataFolder(), "towns.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar arquivo towns.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(configFile);

        // MIGRATION CHECK: City to Kingdom
        if (data.contains("cidade")) {
            plugin.getLogger().info("[MIGRATION] Migrando dados de 'cidade' para 'reino'...");
            ConfigurationSection oldCity = data.getConfigurationSection("cidade");
            data.set("reino", oldCity);
            data.set("cidade", null);
            save(); // Mark dirty
            saveInternal(); // Force save
            plugin.getLogger().info("[MIGRATION] Migração concluída com sucesso.");
        }

        // MIGRATION CHECK: nivel_X to upgrades.X
        ConfigurationSection reinos = data.getConfigurationSection("reino");
        if (reinos != null) {
            boolean changed = false;
            boolean prefeitoChanged = false;
            boolean syncClaimsNeeded = false;

            for (String key : reinos.getKeys(false)) {
                ConfigurationSection kingdom = reinos.getConfigurationSection(key);
                if (kingdom == null)
                    continue;

                String[] types = { "xp", "speed", "extension" };
                for (String t : types) {
                    if (kingdom.contains("nivel_" + t)) {
                        int v = kingdom.getInt("nivel_" + t);
                        data.set("reino." + key + ".upgrades." + t, v);
                        data.set("reino." + key + ".nivel_" + t, null);
                        changed = true;
                    }
                }
            }
            if (changed) {
                plugin.getLogger().info("[MIGRATION] Upgrades do reino migrados para novo formato (upgrades.X).");
                saveInternal(); // Use internal to save immediately during setup
            }

            // MIGRATION: prefeito -> rei
            prefeitoChanged = false;
            for (String key : reinos.getKeys(false)) {
                if (data.contains("reino." + key + ".prefeito")) {
                    String prefeito = data.getString("reino." + key + ".prefeito");
                    if (prefeito != null && !data.contains("reino." + key + ".rei")) {
                        data.set("reino." + key + ".rei", prefeito);
                    }
                    data.set("reino." + key + ".prefeito", null);
                    prefeitoChanged = true;
                }
            }
            if (prefeitoChanged) {
                plugin.getLogger().info("[MIGRATION] Campo 'prefeito' migrado para 'rei'.");
                saveInternal();
            }

            // --- SYNC CHECK: Ensure Claims are marked as REINO ---
            for (String kingdomId : reinos.getKeys(false)) {
                Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
                if (claim != null && !claim.isKingdom()) {
                    plugin.getLogger().warning("[SYNC] Reparando Reino " + kingdomId + " (Tipo TERRENO -> REINO)");
                    claim.setType(Claim.Type.REINO);
                    String savedName = data.getString("reino." + kingdomId + ".nome");
                    if (savedName != null && claim.getKingdomName() == null) {
                        claim.setKingdomName(savedName);
                    }
                    syncClaimsNeeded = true; // Mark that claims.yml needs saving
                }
            }
            if (syncClaimsNeeded) {
                plugin.getLogger().info("[SYNC] Sincronização de reinos/claims concluída.");
                plugin.getClaimManager().saveSync(); // Sincroniza claims.yml IMEDIATAMENTE no setup
            }
            plugin.getLogger()
                    .info("[GorvaxKingdoms] " + reinos.getKeys(false).size() + " reinos carregados com sucesso.");
        }
        buildReverseIndex();
    }

    private void buildReverseIndex() {
        reverseMemberIndex.clear();
        ConfigurationSection reinos = data.getConfigurationSection("reino");
        if (reinos == null)
            return;
        for (String kingdomId : reinos.getKeys(false)) {
            String rei = data.getString("reino." + kingdomId + ".rei");
            if (rei != null)
                reverseMemberIndex.put(UUID.fromString(rei), kingdomId);
            List<String> suditos = data.getStringList("reino." + kingdomId + ".suditos");
            if (suditos.isEmpty())
                suditos = data.getStringList("reino." + kingdomId + ".moradores");
            for (String s : suditos) {
                reverseMemberIndex.put(UUID.fromString(s), kingdomId);
            }
        }
    }

    public void reload() {
        setup();
        playerKingdomCache.clear();
    }

    // Synchronous immediate save (use sparingly, e.g. on shutdown)
    public void saveInternal() {
        saveLock.lock();
        try {
            data.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar o towns.yml!");
        } finally {
            saveLock.unlock();
        }
    }

    public void save() {
        dirty.set(true);
    }

    public void saveSync() {
        saveInternal();
    }

    // --- ACESSO AOS DADOS ---
    public FileConfiguration getData() {
        return data;
    }

    public void saveData() {
        save();
    }

    // --- SISTEMA DE ATIVIDADE E LOGIN ---

    public void atualizarUltimoLogin(String kingdomId) {
        long agora = System.currentTimeMillis();
        data.set("reino." + kingdomId + ".ultimo_login", agora);
        save();
    }

    public void registrarAtividade(UUID uuid, long timestamp) {
        ConfigurationSection reinosSection = data.getConfigurationSection("reino");
        if (reinosSection == null)
            return;

        for (String kingdomIdStr : reinosSection.getKeys(false)) {
            String reiUUID = data.getString("reino." + kingdomIdStr + ".rei");
            if (reiUUID != null && reiUUID.equals(uuid.toString())) {
                data.set("reino." + kingdomIdStr + ".ultimo_login_rei", timestamp);
                data.set("reino." + kingdomIdStr + ".ultimo_login", timestamp);
            }

            List<String> suditos = data.getStringList("reino." + kingdomIdStr + ".suditos");
            if (suditos.contains(uuid.toString())) {
                data.set("reino." + kingdomIdStr + ".suditos_atividade." + uuid.toString(), timestamp);
            }
        }
        save();
    }

    // --- SISTEMA DE RANK POR SÚDITOS ---
    public String getKingdomRank(String kingdomId) {
        int suditos = getSuditosCount(kingdomId);

        // RPG PROGRESSION: Acampamento -> Vila -> Reino -> Império
        if (suditos >= 50)
            return "§6§lImpério";
        if (suditos >= 20)
            return "§e§lReino";
        if (suditos >= 10)
            return "§b§lVila";

        return "§7Acampamento";
    }

    public Claim getKingdom(UUID playerUUID) {
        String kId = reverseMemberIndex.get(playerUUID);
        if (kId != null) {
            return plugin.getClaimManager().getClaimById(kId);
        }

        if (data.getConfigurationSection("reino") == null)
            return null;

        // Synchronize on data or this because FileConfiguration is not thread-safe
        synchronized (this) {
            // Re-check cache inside sync block
            kId = reverseMemberIndex.get(playerUUID);
            if (kId != null)
                return plugin.getClaimManager().getClaimById(kId);

            for (String kingdomId : data.getConfigurationSection("reino").getKeys(false)) {
                String path = "reino." + kingdomId;
                String king = data.getString(path + ".rei");
                if (king != null && king.equals(playerUUID.toString())) {
                    reverseMemberIndex.put(playerUUID, kingdomId);
                    return plugin.getClaimManager().getClaimById(kingdomId);
                }
                List<String> subjects = data.getStringList(path + ".suditos");
                if (subjects.contains(playerUUID.toString())) {
                    reverseMemberIndex.put(playerUUID, kingdomId);
                    return plugin.getClaimManager().getClaimById(kingdomId);
                }
            }
        }

        return null;
    }

    public void invalidateCache(UUID playerUUID) {
        playerKingdomCache.remove(playerUUID);
    }

    public double getPassiveXpBonus(String kingdomId) {
        int suditos = getSuditosCount(kingdomId);
        return (suditos / 5) * 0.02;
    }

    // --- NOME E NÍVEIS ---
    public String getNome(String id) {
        return data.getString("reino." + id + ".nome");
    }

    public void setNome(String id, String nome) {
        data.set("reino." + id + ".nome", nome);
        save();

        // Sync with Claim object
        Claim claim = plugin.getClaimManager().getClaimById(id);
        if (claim != null) {
            claim.setKingdomName(nome);
            claim.setType(Claim.Type.REINO);
            plugin.getClaimManager().saveClaims(); // Persist change in claims.yml
        }
    }

    public String tryFindKingdomIdByName(String name) {
        if (data.getConfigurationSection("reino") == null)
            return null;
        for (String id : data.getConfigurationSection("reino").getKeys(false)) {
            String existingName = data.getString("reino." + id + ".nome");
            if (existingName != null && existingName.equalsIgnoreCase(name)) {
                return id;
            }
        }
        return null;
    }

    public int getNivel(String id, String tipo) {
        return data.getInt("reino." + id + ".upgrades." + tipo, 0);
    }

    public void setNivel(String id, String tipo, int nivel) {
        data.set("reino." + id + ".upgrades." + tipo, nivel);
        save();
    }

    public int getNivelPreservacao(String kingdomId) {
        return getNivel(kingdomId, "preservacao");
    }

    public void setNivelPreservacao(String kingdomId, int nivel) {
        setNivel(kingdomId, "preservacao", nivel);
    }

    public int getNivelSorte(String kingdomId) {
        return getNivel(kingdomId, "sorte");
    }

    public void setNivelSorte(String kingdomId, int nivel) {
        setNivel(kingdomId, "sorte", nivel);
    }

    // --- GESTÃO DE REI (LÍDER) ---
    public void setRei(String kingdomId, UUID uuid) {
        // Enforce single kingdom: Remove from old kingdom if subject
        removeFromOldKingdom(uuid);

        // Demote previous king if exists
        UUID oldKing = getRei(kingdomId);
        if (oldKing != null && !oldKing.equals(uuid)) {
            demoteKing(oldKing);
        }

        data.set("reino." + kingdomId + ".rei", uuid.toString());
        // Clean legacy
        data.set("reino." + kingdomId + ".prefeito", null);

        data.set("reino." + kingdomId + ".ultimo_login_rei", System.currentTimeMillis());
        reverseMemberIndex.put(uuid, kingdomId);
        save();

        // Sync with Claim object
        Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
        if (claim != null) {
            claim.setOwner(uuid);
            claim.setType(Claim.Type.REINO);
            plugin.getClaimManager().saveClaims(); // Persist change in claims.yml
        }

        // Promote new King
        promoteToKing(uuid);
    }

    /**
     * Verifica se um jogador já possui um reino ativo.
     */
    public boolean hasActiveKingdom(UUID uuid) {
        return reverseMemberIndex.containsKey(uuid);
    }

    public UUID getRei(String kingdomId) {
        String uuidStr = data.getString("reino." + kingdomId + ".rei");

        if (uuidStr == null)
            return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isRei(String kingdomId, UUID playerUUID) {
        String dono = data.getString("reino." + kingdomId + ".rei");

        if (dono == null) {
            Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
            return claim != null && playerUUID.equals(claim.getOwner());
        }
        return playerUUID.toString().equals(dono);
    }

    // Helper to prevent multi-kingdom states
    private void removeFromOldKingdom(UUID uuid) {
        String oldKingdomId = reverseMemberIndex.get(uuid);
        if (oldKingdomId != null) {
            // Only auto-remove if they are a subject. Kings should explicitly
            // delete/transfer.
            if (isSudito(oldKingdomId, uuid) && !isRei(oldKingdomId, uuid)) {
                removeSudito(oldKingdomId, uuid, false); // No auto-save here, wait for next op
            }
        }
    }

    // --- INTEGRAÇÃO LUCKPERMS (CARGOS) ---
    public void promoteToKing(UUID uuid) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        if (pd != null) {
            pd.setKingRank(true);
            plugin.getPlayerDataManager().saveData(uuid);
        }
    }

    public void demoteKing(UUID uuid) {
        // Persistência de Rei: Não removemos o cargo ao desfazer o reino.
        // O título é perpétuo, conforme solicitado.
    }

    /**
     * Verifica se o jogador possui o título PERMANENTE de Rei,
     * independente de ter um reino ativo.
     */
    public boolean isPlayerKing(UUID uuid) {
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);
        return pd != null && pd.hasKingRank();
    }

    // --- SISTEMA DE SÚDITOS E BLOCOS ---
    public void devolverBlocos(UUID playerUUID, int quantidade) {
        PlayerData pd = plugin.getPlayerDataManager().getData(playerUUID);
        if (pd != null) {
            pd.addClaimBlocks(quantidade);
            plugin.getPlayerDataManager().saveData(playerUUID);
        }
    }

    public boolean addSudito(String kingdomId, UUID uuid) {
        // B5.4 — Verificar limite de membros
        if (!canAddMember(kingdomId)) {
            return false;
        }

        // Enforce single kingdom
        removeFromOldKingdom(uuid);

        List<String> suditos = getSuditosList(kingdomId); // Handles legacy 'moradores'

        if (!suditos.contains(uuid.toString())) {
            suditos.add(uuid.toString());
            data.set("reino." + kingdomId + ".suditos", suditos);
            data.set("reino." + kingdomId + ".moradores", null); // Clean legacy

            data.set("reino." + kingdomId + ".suditos_atividade." + uuid.toString(), System.currentTimeMillis());
            reverseMemberIndex.put(uuid, kingdomId);
            save();

            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                plugin.refreshPlayerName(p);
        }
        return true;
    }

    public void removeSudito(String kingdomId, UUID uuid) {
        removeSudito(kingdomId, uuid, true);
    }

    public void removeSudito(String kingdomId, UUID uuid, boolean shouldSave) {
        List<String> suditos = getSuditosList(kingdomId);
        if (suditos.remove(uuid.toString())) {
            data.set("reino." + kingdomId + ".suditos", suditos);
            data.set("reino." + kingdomId + ".moradores", null); // Clean legacy

            data.set("reino." + kingdomId + ".suditos_atividade." + uuid.toString(), null);
            reverseMemberIndex.remove(uuid);
            if (shouldSave)
                save();

            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                plugin.refreshPlayerName(p);
        }
    }

    public List<String> getSuditosList(String kingdomId) {
        List<String> list = data.getStringList("reino." + kingdomId + ".suditos");
        if (list == null || list.isEmpty()) {
            list = data.getStringList("reino." + kingdomId + ".moradores"); // Fallback
        }
        // Retorna cópia para evitar modificação direta do cache da config
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public int getSuditosCount(String kingdomId) {
        return getSuditosList(kingdomId).size() + 1;
    }

    public boolean isSudito(String kingdomId, UUID uuid) {
        Claim mainClaim = plugin.getClaimManager().getClaimById(kingdomId);
        if (mainClaim != null && mainClaim.getOwner() != null && mainClaim.getOwner().equals(uuid))
            return true;
        return getSuditosList(kingdomId).contains(uuid.toString());
    }

    public boolean hasBuffAccess(String kingdomId, UUID uuid) {
        // Verifica se o reino existe
        if (getNome(kingdomId) == null)
            return false;

        // Verifica se é Rei
        if (isRei(kingdomId, uuid))
            return true;

        // Verifica se é Súdito
        if (isSudito(kingdomId, uuid))
            return true;

        return false;
    }

    // --- GESTÃO DE LOTES ---

    public List<String> getLotesDoReino(String kingdomId) {
        List<String> lotes = new ArrayList<>();
        ConfigurationSection vinculados = data.getConfigurationSection("lotes_vinculados");
        if (vinculados != null) {
            for (String key : vinculados.getKeys(false)) {
                if (data.getString("lotes_vinculados." + key).equals(kingdomId)) {
                    lotes.add(key);
                }
            }
        }
        return lotes;
    }

    public double getPrecoLote(String kingdomId, String loteId) {
        return data.getDouble("reino." + kingdomId + ".lotes." + loteId + ".preco", 0.0);
    }

    public boolean isLoteAVenda(String loteId) {
        String kingdomId = getReinoDoLote(loteId);
        if (kingdomId == null)
            return false;
        return getPrecoLote(kingdomId, loteId) > 0;
    }

    public void setPrecoLote(String kingdomId, String loteId, double preco) {
        vincularLoteAoReino(kingdomId, loteId);
        data.set("reino." + kingdomId + ".lotes." + loteId + ".preco", preco);

        if (preco > 0) {
            atribuirNumeroLote(kingdomId, loteId);
        }
        save();
    }

    public void vincularLoteAoReino(String kingdomId, String loteId) {
        data.set("lotes_vinculados." + loteId, kingdomId);
        save();
    }

    public String getReinoDoLote(String loteId) {
        return data.getString("lotes_vinculados." + loteId);
    }

    public void removerLoteDoReino(String loteId) {
        String kingdomId = getReinoDoLote(loteId);
        if (kingdomId != null) {
            data.set("reino." + kingdomId + ".lotes." + loteId, null);
        }
        data.set("lotes_vinculados." + loteId, null);
        save();
    }

    // --- SISTEMA DE NUMERAÇÃO AMIGÁVEL ---
    public int getNumeroReino(String kingdomId) {
        int num = data.getInt("reino." + kingdomId + ".numero_fundacao", 0);
        if (num == 0) {
            int contReinos = data.getInt("global.contador_reinos", 0);
            int contCidades = data.getInt("global.contador_cidades", 0);
            int proximo = Math.max(contReinos, contCidades) + 1;

            data.set("global.contador_reinos", proximo);
            data.set("reino." + kingdomId + ".numero_fundacao", proximo);
            save();
            return proximo;
        }
        return num;
    }

    public int atribuirNumeroLote(String kingdomId, String loteId) {
        int numExistente = data.getInt("reino." + kingdomId + ".lotes." + loteId + ".numero_display", 0);
        if (numExistente != 0)
            return numExistente;

        int proximo = data.getInt("reino." + kingdomId + ".contador_lotes", 0) + 1;
        data.set("reino." + kingdomId + ".contador_lotes", proximo);
        data.set("reino." + kingdomId + ".lotes." + loteId + ".numero_display", proximo);
        save();
        return proximo;
    }

    public int getNumeroLote(String kingdomId, String loteId) {
        return data.getInt("reino." + kingdomId + ".lotes." + loteId + ".numero_display", 0);
    }

    // --- SISTEMA DE SPAWN ---
    public void setSpawn(String kingdomId, Location loc) {
        String path = "reino." + kingdomId + ".spawn";
        data.set(path + ".world", loc.getWorld().getName());
        data.set(path + ".x", loc.getX());
        data.set(path + ".y", loc.getY());
        data.set(path + ".z", loc.getZ());
        data.set(path + ".yaw", (float) loc.getYaw());
        data.set(path + ".pitch", (float) loc.getPitch());
        save();
    }

    public Location getSpawn(String kingdomId) {
        String path = "reino." + kingdomId + ".spawn";
        if (!data.contains(path))
            return null;

        World world = Bukkit.getWorld(data.getString(path + ".world"));
        if (world == null)
            return null;

        return new Location(
                world,
                data.getDouble(path + ".x"),
                data.getDouble(path + ".y"),
                data.getDouble(path + ".z"),
                (float) data.getDouble(path + ".yaw"),
                (float) data.getDouble(path + ".pitch"));
    }

    // --- DELEÇÃO E LIMPEZA ---
    public void deleteKingdom(String kingdomId) {
        deleteKingdom(kingdomId, true);
    }

    public void deleteKingdom(String kingdomId, boolean shouldSave) {
        // B19 — Evento customizado: KingdomDeleteEvent
        String kingdomName = getNome(kingdomId);
        UUID kingUUID = getRei(kingdomId);
        org.bukkit.Bukkit.getPluginManager().callEvent(
                new br.com.gorvax.core.events.KingdomDeleteEvent(kingdomId, kingdomName, kingUUID));

        // Persistência de Rei: demoteKing é no-op, título permanece.
        if (kingUUID != null) {
            demoteKing(kingUUID);
            invalidateCache(kingUUID);
            reverseMemberIndex.remove(kingUUID);
        }

        Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
        if (claim != null) {
            // Reembolsar blocos ao Rei (centralizado aqui)
            if (kingUUID != null) {
                int width = claim.getMaxX() - claim.getMinX() + 1;
                int length = claim.getMaxZ() - claim.getMinZ() + 1;
                int area = width * length;
                devolverBlocos(kingUUID, area);
            }

            // FIX: Use removeClaim to update chunk cache
            plugin.getClaimManager().removeClaim(claim);

            // B13 — Remover todos os outposts vinculados
            List<Claim> outposts = plugin.getClaimManager().getOutpostsForKingdom(kingdomId);
            for (Claim outpost : outposts) {
                if (kingUUID != null) {
                    int ow = outpost.getMaxX() - outpost.getMinX() + 1;
                    int ol = outpost.getMaxZ() - outpost.getMinZ() + 1;
                    devolverBlocos(kingUUID, ow * ol);
                }
                plugin.getClaimManager().removeClaim(outpost);
            }
            // Limpar spawns de outposts
            data.set("reino." + kingdomId + ".outpost_spawns", null);

            // Limpa o cache de todos os súditos do reino deletado
            List<String> suditos = getSuditosList(kingdomId);
            suditos.forEach(s -> {
                UUID sUuid = UUID.fromString(s);
                invalidateCache(sUuid);
                reverseMemberIndex.remove(sUuid);
            });
            if (claim.getOwner() != null)
                invalidateCache(claim.getOwner());
        }

        // B7 — Limpar relações diplomáticas do reino deletado
        clearAllRelations(kingdomId);

        apagarDadosReino(kingdomId, shouldSave);

        // Refresh online players (they might have been subjects)
        for (Player online : Bukkit.getOnlinePlayers()) {
            plugin.refreshPlayerName(online);
        }
    }

    public void apagarDadosReino(String kingdomId, boolean shouldSave) {
        data.set("reino." + kingdomId, null);
        ConfigurationSection vinculados = data.getConfigurationSection("lotes_vinculados");
        if (vinculados != null) {
            for (String key : vinculados.getKeys(false)) {
                if (data.getString("lotes_vinculados." + key).equals(kingdomId)) {
                    data.set("lotes_vinculados." + key, null);
                }
            }
        }
        if (shouldSave) {
            saveInternal(); // Força salvamento síncrono para garantir exclusão física imediata
        }
    }

    // --- B1.1: SISTEMA DE CONVITES ---

    /**
     * Envia convite para um jogador entrar no reino.
     * Expiração configurável via config.yml (kingdoms.invite_expire, default 60s).
     */
    public void invitePlayer(String kingdomId, UUID inviter, UUID invited) {
        pendingInvites.put(invited, new Invite(kingdomId, inviter, System.currentTimeMillis()));
    }

    /**
     * Aceita convite pendente. Adiciona jogador como súdito.
     * 
     * @return true se aceito com sucesso, false se convite inválido/expirado.
     */
    public boolean acceptInvite(UUID invited) {
        Invite invite = getPendingInvite(invited);
        if (invite == null)
            return false;
        pendingInvites.remove(invited);
        return addSudito(invite.kingdomId(), invited);
    }

    /**
     * Recusa convite pendente.
     * 
     * @return true se havia convite para recusar.
     */
    public boolean denyInvite(UUID invited) {
        return pendingInvites.remove(invited) != null;
    }

    /**
     * Retorna o convite pendente do jogador, ou null se não existir ou estiver
     * expirado.
     */
    public Invite getPendingInvite(UUID invited) {
        Invite invite = pendingInvites.get(invited);
        if (invite == null)
            return null;
        long expireSeconds = plugin.getConfig().getLong("kingdoms.invite_expire", 60);
        if (System.currentTimeMillis() - invite.timestamp() > expireSeconds * 1000L) {
            pendingInvites.remove(invited);
            return null;
        }
        return invite;
    }

    // --- B1.4: LISTAR NOMES DE REINOS ---

    /**
     * Retorna lista com os nomes de todos os reinos registrados.
     */
    public List<String> getAllKingdomNames() {
        List<String> names = new ArrayList<>();
        ConfigurationSection reinos = data.getConfigurationSection("reino");
        if (reinos == null)
            return names;
        for (String id : reinos.getKeys(false)) {
            String nome = data.getString("reino." + id + ".nome");
            if (nome != null)
                names.add(nome);
        }
        return names;
    }

    /**
     * B7 — Retorna todos os IDs de reinos existentes.
     */
    public List<String> getAllKingdomIds() {
        List<String> ids = new ArrayList<>();
        ConfigurationSection reinos = data.getConfigurationSection("reino");
        if (reinos == null)
            return ids;
        for (String id : reinos.getKeys(false)) {
            // Filtrar apenas reinos com nome (exclui dados órfãos)
            if (data.getString("reino." + id + ".nome") != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    // --- B5.3: BANCO DO REINO ---

    public double getBankBalance(String kingdomId) {
        return data.getDouble("reino." + kingdomId + ".bank_balance", 0.0);
    }

    public void depositToBank(String kingdomId, double amount) {
        double current = getBankBalance(kingdomId);
        data.set("reino." + kingdomId + ".bank_balance", current + amount);
        save();
    }

    /**
     * Saca do banco do reino. Retorna true se houve saldo suficiente.
     */
    public boolean withdrawFromBank(String kingdomId, double amount) {
        double current = getBankBalance(kingdomId);
        if (current < amount)
            return false;
        data.set("reino." + kingdomId + ".bank_balance", current - amount);
        save();
        return true;
    }

    // --- B5.4: NÍVEL DO REINO E LIMITE DE MEMBROS ---

    /**
     * Calcula nível do reino baseado em: chunks claimados + total de upgrades +
     * tempo ativo.
     * Cada 5 chunks = +1 nível, cada upgrade comprado = +1 nível, cada 30 dias = +1
     * nível.
     */
    public int getKingdomLevel(String kingdomId) {
        // Chunks do claim principal
        Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
        int chunks = 0;
        if (claim != null) {
            int width = claim.getMaxX() - claim.getMinX() + 1;
            int length = claim.getMaxZ() - claim.getMinZ() + 1;
            chunks = (width * length) / (16 * 16); // Aproximação por chunks
            if (chunks < 1)
                chunks = 1;
        }

        // B13 — Somar chunks de outposts
        for (Claim outpost : plugin.getClaimManager().getOutpostsForKingdom(kingdomId)) {
            int ow = outpost.getMaxX() - outpost.getMinX() + 1;
            int ol = outpost.getMaxZ() - outpost.getMinZ() + 1;
            chunks += Math.max(1, (ow * ol) / (16 * 16));
        }

        // Upgrades
        int totalUpgrades = getNivel(kingdomId, "preservacao")
                + getNivel(kingdomId, "xp")
                + getNivel(kingdomId, "sorte")
                + getNivel(kingdomId, "speed")
                + getNivel(kingdomId, "extension");

        // Tempo ativo (dias desde fundação, usando ultimo_login como proxy)
        long createdAt = data.getLong("reino." + kingdomId + ".created_at", 0);
        int diasAtivo = 0;
        if (createdAt > 0) {
            diasAtivo = (int) ((System.currentTimeMillis() - createdAt) / (1000L * 60 * 60 * 24));
        }

        return Math.max(1, (chunks / 5) + totalUpgrades + (diasAtivo / 30));
    }

    /**
     * Retorna o número máximo de membros permitido (incluindo Rei).
     */
    public int getMaxMembers(String kingdomId) {
        int base = plugin.getConfig().getInt("kingdoms.max_members.base", 5);
        int perLevel = plugin.getConfig().getInt("kingdoms.max_members.per_level", 3);
        int extraSlots = getExtraSlots(kingdomId);
        int level = getKingdomLevel(kingdomId);
        return base + (level * perLevel) + extraSlots;
    }

    public int getExtraSlots(String kingdomId) {
        return data.getInt("reino." + kingdomId + ".extra_member_slots", 0);
    }

    /**
     * Compra +1 slot de membro usando o banco do reino.
     * 
     * @return true se a compra foi bem-sucedida.
     */
    public boolean buyMemberSlot(String kingdomId) {
        double cost = plugin.getConfig().getDouble("kingdoms.max_members.slot_cost", 10000.0);
        if (!withdrawFromBank(kingdomId, cost))
            return false;
        int current = getExtraSlots(kingdomId);
        data.set("reino." + kingdomId + ".extra_member_slots", current + 1);
        save();
        return true;
    }

    /**
     * Verifica se o reino pode aceitar mais membros.
     */
    public boolean canAddMember(String kingdomId) {
        int current = getSuditosCount(kingdomId); // Já inclui o Rei
        int max = getMaxMembers(kingdomId);
        return current < max;
    }

    // --- B5.1/B5.2: DÍVIDA DE IMPOSTOS E UPKEEP ---

    public int getTaxDebtDays(String kingdomId, UUID uuid) {
        return data.getInt("reino." + kingdomId + ".tax_debt." + uuid.toString(), 0);
    }

    public void setTaxDebtDays(String kingdomId, UUID uuid, int days) {
        data.set("reino." + kingdomId + ".tax_debt." + uuid.toString(), days);
        save();
    }

    public void clearTaxDebt(String kingdomId, UUID uuid) {
        data.set("reino." + kingdomId + ".tax_debt." + uuid.toString(), null);
        save();
    }

    public int getUpkeepDebtDays(String kingdomId) {
        return data.getInt("reino." + kingdomId + ".upkeep_debt_days", 0);
    }

    public void setUpkeepDebtDays(String kingdomId, int days) {
        data.set("reino." + kingdomId + ".upkeep_debt_days", days);
        save();
    }

    /**
     * Retorna true se o reino está em estado de "Decadência" (buffs desativados).
     */
    public boolean isDecaying(String kingdomId) {
        int warningDays = plugin.getConfig().getInt("kingdoms.upkeep.warning_days", 7);
        return getUpkeepDebtDays(kingdomId) >= warningDays;
    }

    /**
     * Retorna a taxa diária customizada pelo Rei, ou o padrão do config.
     */
    public double getResidentTax(String kingdomId) {
        double customTax = data.getDouble("reino." + kingdomId + ".custom_tax", -1);
        if (customTax >= 0)
            return customTax;
        return plugin.getConfig().getDouble("kingdoms.tax.resident_daily", 50.0);
    }

    public void setResidentTax(String kingdomId, double tax) {
        double maxRate = plugin.getConfig().getDouble("kingdoms.tax.max_rate", 500.0);
        data.set("reino." + kingdomId + ".custom_tax", Math.min(Math.max(0, tax), maxRate));
        save();
    }

    // ========================================================================
    // B7 — SISTEMA DE DIPLOMACIA: ALIANÇAS E RIVALIDADES
    // ========================================================================

    /**
     * Define a relação entre dois reinos (bidirecional).
     * Salvar em ambos os lados garante consistência na leitura.
     */
    public void setRelation(String kingdomA, String kingdomB, Relation relation) {
        if (kingdomA.equals(kingdomB))
            return;
        if (relation == Relation.NEUTRAL) {
            // Remover entradas — NEUTRAL é o default implícito
            data.set("reino." + kingdomA + ".relations." + kingdomB, null);
            data.set("reino." + kingdomB + ".relations." + kingdomA, null);
        } else {
            data.set("reino." + kingdomA + ".relations." + kingdomB, relation.name());
            data.set("reino." + kingdomB + ".relations." + kingdomA, relation.name());
        }
        save();
    }

    /**
     * Retorna a relação entre dois reinos. Default: NEUTRAL.
     */
    public Relation getRelation(String kingdomA, String kingdomB) {
        if (kingdomA.equals(kingdomB))
            return Relation.NEUTRAL;
        String val = data.getString("reino." + kingdomA + ".relations." + kingdomB);
        if (val == null)
            return Relation.NEUTRAL;
        try {
            return Relation.valueOf(val);
        } catch (IllegalArgumentException e) {
            return Relation.NEUTRAL;
        }
    }

    /** Atalho: verifica se dois reinos são aliados. */
    public boolean areAllied(String kA, String kB) {
        return getRelation(kA, kB) == Relation.ALLY;
    }

    /** Atalho: verifica se dois reinos são inimigos. */
    public boolean areEnemies(String kA, String kB) {
        return getRelation(kA, kB) == Relation.ENEMY;
    }

    /**
     * Retorna lista de IDs de reinos com a relação especificada.
     */
    public List<String> getKingdomsByRelation(String kingdomId, Relation relation) {
        List<String> result = new ArrayList<>();
        ConfigurationSection relations = data.getConfigurationSection("reino." + kingdomId + ".relations");
        if (relations == null)
            return result;
        for (String otherId : relations.getKeys(false)) {
            String val = relations.getString(otherId);
            if (val != null) {
                try {
                    if (Relation.valueOf(val) == relation) {
                        result.add(otherId);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    /** Retorna IDs de todos os reinos aliados. */
    public List<String> getAlliances(String kingdomId) {
        return getKingdomsByRelation(kingdomId, Relation.ALLY);
    }

    /** Retorna IDs de todos os reinos inimigos. */
    public List<String> getEnemies(String kingdomId) {
        return getKingdomsByRelation(kingdomId, Relation.ENEMY);
    }

    /**
     * Limpa todas as relações diplomáticas de um reino (chamado ao deletar).
     */
    public void clearAllRelations(String kingdomId) {
        ConfigurationSection relations = data.getConfigurationSection("reino." + kingdomId + ".relations");
        if (relations == null)
            return;
        for (String otherId : relations.getKeys(false)) {
            // Remover referência no outro reino
            data.set("reino." + otherId + ".relations." + kingdomId, null);
        }
        data.set("reino." + kingdomId + ".relations", null);
        // Limpar propostas pendentes
        pendingAlliances.remove(kingdomId);
        pendingAlliances.entrySet().removeIf(e -> e.getValue().fromKingdomId().equals(kingdomId));
        save();
    }

    // --- B7: Propostas de Aliança ---

    /**
     * Envia proposta de aliança para outro reino.
     */
    public void proposeAlliance(String fromKingdomId, String toKingdomId) {
        pendingAlliances.put(toKingdomId, new AllianceProposal(fromKingdomId, toKingdomId, System.currentTimeMillis()));
    }

    /**
     * Retorna a proposta de aliança pendente para o reino, ou null se não
     * existir/expirada.
     */
    public AllianceProposal getPendingAlliance(String kingdomId) {
        AllianceProposal proposal = pendingAlliances.get(kingdomId);
        if (proposal == null)
            return null;
        long expireSeconds = plugin.getConfig().getLong("diplomacy.alliance_expire", 60);
        if (System.currentTimeMillis() - proposal.timestamp() > expireSeconds * 1000L) {
            pendingAlliances.remove(kingdomId);
            return null;
        }
        return proposal;
    }

    /**
     * Aceita proposta de aliança pendente. Define relação ALLY para ambos.
     * 
     * @return true se aceita com sucesso.
     */
    public boolean acceptAlliance(String kingdomId) {
        AllianceProposal proposal = getPendingAlliance(kingdomId);
        if (proposal == null)
            return false;
        pendingAlliances.remove(kingdomId);
        setRelation(proposal.fromKingdomId(), proposal.toKingdomId(), Relation.ALLY);
        return true;
    }

    /**
     * Recusa proposta de aliança.
     * 
     * @return true se havia proposta para recusar.
     */
    public boolean denyAlliance(String kingdomId) {
        return pendingAlliances.remove(kingdomId) != null;
    }

    /**
     * Retorna o número de alianças ativas de um reino.
     */
    public int getAllianceCount(String kingdomId) {
        return getAlliances(kingdomId).size();
    }

    /**
     * Retorna o número de inimizades ativas de um reino.
     */
    public int getEnemyCount(String kingdomId) {
        return getEnemies(kingdomId).size();
    }

    // ========================================================================
    // B13 — SISTEMA DE OUTPOSTS (POSTOS AVANÇADOS)
    // ========================================================================

    /**
     * Retorna o número máximo de outposts que um reino pode ter.
     * Leitura da config: outposts.max_per_level (mapa nível → max).
     */
    public int getMaxOutposts(String kingdomId) {
        int level = getKingdomLevel(kingdomId);
        org.bukkit.configuration.ConfigurationSection sec = plugin.getConfig()
                .getConfigurationSection("outposts.max_per_level");
        if (sec == null)
            return 0;

        int max = 0;
        for (String key : sec.getKeys(false)) {
            try {
                int requiredLevel = Integer.parseInt(key);
                if (level >= requiredLevel) {
                    max = Math.max(max, sec.getInt(key));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    /**
     * Retorna a quantidade atual de outposts de um reino.
     */
    public int getOutpostCount(String kingdomId) {
        return plugin.getClaimManager().getOutpostsForKingdom(kingdomId).size();
    }

    /**
     * Define o spawn de um outpost.
     */
    public void setOutpostSpawn(String kingdomId, String outpostClaimId, Location loc) {
        String path = "reino." + kingdomId + ".outpost_spawns." + outpostClaimId;
        data.set(path + ".world", loc.getWorld().getName());
        data.set(path + ".x", loc.getX());
        data.set(path + ".y", loc.getY());
        data.set(path + ".z", loc.getZ());
        data.set(path + ".yaw", (float) loc.getYaw());
        data.set(path + ".pitch", (float) loc.getPitch());
        save();
    }

    /**
     * Retorna o spawn de um outpost, ou null se não definido.
     */
    public Location getOutpostSpawn(String kingdomId, String outpostClaimId) {
        String path = "reino." + kingdomId + ".outpost_spawns." + outpostClaimId;
        if (!data.contains(path))
            return null;

        World world = Bukkit.getWorld(data.getString(path + ".world"));
        if (world == null)
            return null;

        return new Location(
                world,
                data.getDouble(path + ".x"),
                data.getDouble(path + ".y"),
                data.getDouble(path + ".z"),
                (float) data.getDouble(path + ".yaw"),
                (float) data.getDouble(path + ".pitch"));
    }

    /**
     * Remove o spawn de um outpost.
     */
    public void removeOutpostSpawn(String kingdomId, String outpostClaimId) {
        data.set("reino." + kingdomId + ".outpost_spawns." + outpostClaimId, null);
        save();
    }
}
