package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B22 — Gerenciador de Estruturas (reinos pré-construídos no mundo).
 * Permite mapear locais de builds como pontos de interesse com nome, tema,
 * centro e raio. Prepara infraestrutura para NPCs, quests e mercados futuros.
 */
public class StructureManager {

    private final GorvaxCore plugin;
    private final Map<String, StructureData> structures = new ConcurrentHashMap<>();
    private File file;
    private FileConfiguration config;

    public StructureManager(GorvaxCore plugin) {
        this.plugin = plugin;
        load();
    }

    // ========== Record de dados ==========

    /** Dados de uma estrutura mapeada no mundo. */
    public record StructureData(
            String id,
            String nome,
            String tema,
            String mundo,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            int raio,
            String criadoPor,
            String criadoEm) {

        /** Retorna a Location do centro da estrutura. */
        public Location toLocation() {
            World w = Bukkit.getWorld(mundo);
            if (w == null)
                return null;
            Location loc = new Location(w, x, y, z);
            loc.setYaw(yaw);
            loc.setPitch(pitch);
            return loc;
        }

        /** Verifica se uma Location está dentro do raio da estrutura. */
        public boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null)
                return false;
            if (!loc.getWorld().getName().equals(mundo))
                return false;
            double distSq = Math.pow(loc.getX() - x, 2) + Math.pow(loc.getZ() - z, 2);
            return distSq <= (double) raio * raio;
        }

        /** Distância horizontal do centro ao jogador. */
        public double distanceTo(Location loc) {
            if (loc == null || loc.getWorld() == null)
                return Double.MAX_VALUE;
            if (!loc.getWorld().getName().equals(mundo))
                return Double.MAX_VALUE;
            return Math.sqrt(Math.pow(loc.getX() - x, 2) + Math.pow(loc.getZ() - z, 2));
        }
    }

    // ========== Carregamento e salvamento ==========

    public void load() {
        structures.clear();
        file = new File(plugin.getDataFolder(), "structures.yml");
        if (!file.exists()) {
            plugin.saveResource("structures.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection sec = config.getConfigurationSection("estruturas");
        if (sec == null) {
            plugin.getLogger().info("[Estruturas] Nenhuma estrutura encontrada em structures.yml.");
            return;
        }

        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null)
                continue;
            try {
                StructureData data = new StructureData(
                        id,
                        s.getString("nome", "§7Estrutura Desconhecida"),
                        s.getString("tema", "generico"),
                        s.getString("mundo", "world"),
                        s.getDouble("centro.x", 0),
                        s.getDouble("centro.y", 64),
                        s.getDouble("centro.z", 0),
                        (float) s.getDouble("centro.yaw", 0),
                        (float) s.getDouble("centro.pitch", 0),
                        s.getInt("raio", 100),
                        s.getString("criado_por", "Console"),
                        s.getString("criado_em", LocalDate.now().toString()));
                structures.put(id, data);
            } catch (Exception e) {
                plugin.getLogger().warning("[Estruturas] Erro ao carregar estrutura '" + id + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("[Estruturas] " + structures.size() + " estrutura(s) carregada(s).");
    }

    public void save() {
        config.set("estruturas", null);
        for (Map.Entry<String, StructureData> entry : structures.entrySet()) {
            String path = "estruturas." + entry.getKey();
            StructureData d = entry.getValue();
            config.set(path + ".nome", d.nome());
            config.set(path + ".tema", d.tema());
            config.set(path + ".mundo", d.mundo());
            config.set(path + ".centro.x", d.x());
            config.set(path + ".centro.y", d.y());
            config.set(path + ".centro.z", d.z());
            config.set(path + ".centro.yaw", d.yaw());
            config.set(path + ".centro.pitch", d.pitch());
            config.set(path + ".raio", d.raio());
            config.set(path + ".criado_por", d.criadoPor());
            config.set(path + ".criado_em", d.criadoEm());
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("[Estruturas] Erro ao salvar structures.yml: " + e.getMessage());
            }
        });
    }

    public void saveSync() {
        config.set("estruturas", null);
        for (Map.Entry<String, StructureData> entry : structures.entrySet()) {
            String path = "estruturas." + entry.getKey();
            StructureData d = entry.getValue();
            config.set(path + ".nome", d.nome());
            config.set(path + ".tema", d.tema());
            config.set(path + ".mundo", d.mundo());
            config.set(path + ".centro.x", d.x());
            config.set(path + ".centro.y", d.y());
            config.set(path + ".centro.z", d.z());
            config.set(path + ".centro.yaw", d.yaw());
            config.set(path + ".centro.pitch", d.pitch());
            config.set(path + ".raio", d.raio());
            config.set(path + ".criado_por", d.criadoPor());
            config.set(path + ".criado_em", d.criadoEm());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Estruturas] Erro ao salvar structures.yml: " + e.getMessage());
        }
    }

    // ========== Operações CRUD ==========

    /** Cria uma nova estrutura na posição do jogador. */
    public boolean create(String id, String nome, String tema, int raio, Player creator) {
        if (structures.containsKey(id))
            return false;

        Location loc = creator.getLocation();
        StructureData data = new StructureData(
                id, nome, tema,
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(),
                raio,
                creator.getName(),
                LocalDate.now().toString());

        structures.put(id, data);
        save();
        return true;
    }

    /** Remove uma estrutura pelo ID. */
    public boolean delete(String id) {
        if (!structures.containsKey(id))
            return false;
        structures.remove(id);
        save();
        return true;
    }

    /** Retorna uma estrutura pelo ID. */
    public StructureData get(String id) {
        return structures.get(id);
    }

    /** Lista todos os IDs de estruturas registradas. */
    public List<String> getAllIds() {
        return new ArrayList<>(structures.keySet());
    }

    /** Retorna todas as estruturas. */
    public Collection<StructureData> getAll() {
        return structures.values();
    }

    /** Retorna a estrutura que contém a Location, ou null. */
    public StructureData getStructureAt(Location loc) {
        for (StructureData s : structures.values()) {
            if (s.contains(loc))
                return s;
        }
        return null;
    }

    /** Retorna a estrutura mais próxima de uma Location. */
    public StructureData getNearest(Location loc) {
        StructureData nearest = null;
        double minDist = Double.MAX_VALUE;
        for (StructureData s : structures.values()) {
            double dist = s.distanceTo(loc);
            if (dist < minDist) {
                minDist = dist;
                nearest = s;
            }
        }
        return nearest;
    }

    /** Recarrega os dados (para /gorvax reload). */
    public void reload() {
        load();
    }
}
