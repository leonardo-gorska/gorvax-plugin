package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B10 — Gerenciador de itens customizados lendários.
 * Carrega definições de custom_items.yml e cria ItemStacks com
 * PersistentDataContainer, enchants, atributos, efeitos on-hit e passivos.
 */
public class CustomItemManager {

    private final GorvaxCore plugin;
    private final NamespacedKey customItemKey;
    private final Map<String, ItemDefinition> itemDefinitions = new ConcurrentHashMap<>();

    public CustomItemManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");
        loadItems();
    }

    // ========== Records internos ==========

    /** Efeito aplicado ao acertar um hit com a arma. */
    public record OnHitEffect(PotionEffectType type, int duration, int amplifier, int chance) {
    }

    /** Efeito passivo aplicado enquanto a armadura está equipada. */
    public record PassiveEffect(PotionEffectType type, int amplifier) {
    }

    /** Efeito visual de partícula exibido enquanto o item está na mão/equipado. */
    public record ParticleEffect(Particle particle, int count, double speed,
            double offsetX, double offsetY, double offsetZ) {
    }

    /**
     * Som customizado tocado em ações específicas do item (equip, swing, block).
     */
    public record SoundEffect(String sound, float volume, float pitch) {
    }

    /** Definição completa de um custom item (apenas dados, sem ItemStack). */
    private record ItemDefinition(
            String id,
            Material base,
            String name,
            List<String> lore,
            Map<Enchantment, Integer> enchants,
            Map<Attribute, Double> attributes,
            List<OnHitEffect> onHitEffects,
            List<PassiveEffect> passiveEffects,
            List<ParticleEffect> particleEffects,
            List<SoundEffect> equipSounds,
            List<SoundEffect> swingSounds,
            List<SoundEffect> blockSounds,
            String source,
            int customModelData,
            String itemModel,
            String trimPattern,
            String trimMaterial) {
    }

    // ========== Carregamento ==========

    /**
     * Carrega (ou recarrega) todas as definições de itens do custom_items.yml.
     */
    public void loadItems() {
        itemDefinitions.clear();

        // Salvar o arquivo padrão se não existir
        File file = new File(plugin.getDataFolder(), "custom_items.yml");
        if (!file.exists()) {
            plugin.saveResource("custom_items.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("[CustomItems] Nenhum item encontrado em custom_items.yml!");
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection sec = itemsSection.getConfigurationSection(id);
            if (sec == null)
                continue;

            try {
                ItemDefinition def = parseDefinition(id, sec);
                itemDefinitions.put(id, def);
            } catch (Exception e) {
                plugin.getLogger().warning("[CustomItems] Erro ao carregar item '" + id + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("[CustomItems] " + itemDefinitions.size() + " itens lendários carregados.");
    }

    private ItemDefinition parseDefinition(String id, ConfigurationSection sec) {
        // Material base
        Material base = Material.matchMaterial(sec.getString("base", "STONE"));
        if (base == null)
            throw new IllegalArgumentException("Material inválido: " + sec.getString("base"));

        // Nome e lore
        String name = sec.getString("name", "§cItem Desconhecido");
        List<String> lore = sec.getStringList("lore");

        // Enchants
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>();
        ConfigurationSection enchSec = sec.getConfigurationSection("enchants");
        if (enchSec != null) {
            for (String key : enchSec.getKeys(false)) {
                Enchantment ench = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase()));
                if (ench != null) {
                    enchants.put(ench, enchSec.getInt(key));
                } else {
                    plugin.getLogger().warning("[CustomItems] Encantamento desconhecido: " + key + " no item " + id);
                }
            }
        }

        // Atributos
        Map<Attribute, Double> attributes = new LinkedHashMap<>();
        ConfigurationSection attrSec = sec.getConfigurationSection("attributes");
        if (attrSec != null) {
            for (String key : attrSec.getKeys(false)) {
                try {
                    Attribute attr = Attribute.valueOf(key.toUpperCase());
                    attributes.put(attr, attrSec.getDouble(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[CustomItems] Atributo desconhecido: " + key + " no item " + id);
                }
            }
        }

        // Efeitos on-hit
        List<OnHitEffect> onHitEffects = new ArrayList<>();
        List<Map<?, ?>> onHitList = sec.getMapList("on_hit_effects");
        for (Map<?, ?> entry : onHitList) {
            PotionEffectType type = parsePotionType(String.valueOf(entry.get("type")));
            if (type == null) {
                plugin.getLogger().warning(
                        "[CustomItems] PotionEffectType desconhecido: " + entry.get("type") + " no item " + id);
                continue;
            }
            int duration = entry.get("duration") instanceof Number n ? n.intValue() : 60;
            int amplifier = entry.get("amplifier") instanceof Number n ? n.intValue() : 0;
            int chance = entry.get("chance") instanceof Number n ? n.intValue() : 100;
            onHitEffects.add(new OnHitEffect(type, duration, amplifier, chance));
        }

        // Efeitos passivos
        List<PassiveEffect> passiveEffects = new ArrayList<>();
        List<Map<?, ?>> passiveList = sec.getMapList("passive_effects");
        for (Map<?, ?> entry : passiveList) {
            PotionEffectType type = parsePotionType(String.valueOf(entry.get("type")));
            if (type == null) {
                plugin.getLogger().warning(
                        "[CustomItems] PotionEffectType passivo desconhecido: " + entry.get("type") + " no item " + id);
                continue;
            }
            int amplifier = entry.get("amplifier") instanceof Number n ? n.intValue() : 0;
            passiveEffects.add(new PassiveEffect(type, amplifier));
        }

        // Efeitos visuais de partícula
        List<ParticleEffect> particleEffects = new ArrayList<>();
        List<Map<?, ?>> particleList = sec.getMapList("particle_effects");
        for (Map<?, ?> entry : particleList) {
            String particleStr = String.valueOf(entry.get("particle"));
            Particle particle;
            try {
                particle = Particle.valueOf(particleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(
                        "[CustomItems] Partícula desconhecida: " + particleStr + " no item " + id);
                continue;
            }
            int count = entry.get("count") instanceof Number n ? n.intValue() : 3;
            double speed = entry.get("speed") instanceof Number n ? n.doubleValue() : 0.02;
            double offsetX = entry.get("offset_x") instanceof Number n ? n.doubleValue() : 0.2;
            double offsetY = entry.get("offset_y") instanceof Number n ? n.doubleValue() : 0.3;
            double offsetZ = entry.get("offset_z") instanceof Number n ? n.doubleValue() : 0.2;
            particleEffects.add(new ParticleEffect(particle, count, speed, offsetX, offsetY, offsetZ));
        }

        // Sons customizados (equip, swing, block)
        List<SoundEffect> equipSounds = parseSoundList(sec, "sound_effects.equip", id);
        List<SoundEffect> swingSounds = parseSoundList(sec, "sound_effects.swing", id);
        List<SoundEffect> blockSounds = parseSoundList(sec, "sound_effects.block", id);

        String source = sec.getString("source", "Desconhecido");
        int customModelData = sec.getInt("custom_model_data", 0);
        String itemModel = sec.getString("item_model", null);
        String trimPattern = sec.getString("trim_pattern", null);
        String trimMaterial = sec.getString("trim_material", null);

        return new ItemDefinition(id, base, name, lore, enchants, attributes, onHitEffects, passiveEffects,
                particleEffects, equipSounds, swingSounds, blockSounds,
                source, customModelData, itemModel, trimPattern, trimMaterial);
    }

    private List<SoundEffect> parseSoundList(ConfigurationSection sec, String path, String itemId) {
        List<SoundEffect> sounds = new ArrayList<>();
        List<Map<?, ?>> list = sec.getMapList(path);
        for (Map<?, ?> entry : list) {
            String sound = String.valueOf(entry.get("sound"));
            if (sound.equals("null") || sound.isEmpty()) {
                plugin.getLogger().warning("[CustomItems] Som inválido na seção " + path + " do item " + itemId);
                continue;
            }
            float volume = entry.get("volume") instanceof Number n ? n.floatValue() : 0.8f;
            float pitch = entry.get("pitch") instanceof Number n ? n.floatValue() : 1.0f;
            sounds.add(new SoundEffect(sound, volume, pitch));
        }
        return sounds;
    }

    // Mapa de nomes legados pré-1.21 para os novos nomes de PotionEffectType
    private static final Map<String, String> LEGACY_POTION_NAMES = Map.ofEntries(
            Map.entry("HARM", "instant_damage"),
            Map.entry("INSTANT_DAMAGE", "instant_damage"),
            Map.entry("HEAL", "instant_health"),
            Map.entry("INSTANT_HEALTH", "instant_health"),
            Map.entry("SLOW", "slowness"),
            Map.entry("DAMAGE_RESISTANCE", "resistance"),
            Map.entry("JUMP", "jump_boost"),
            Map.entry("CONFUSION", "nausea"),
            Map.entry("INCREASE_DAMAGE", "strength"));

    private PotionEffectType parsePotionType(String name) {
        if (name == null || name.equalsIgnoreCase("null"))
            return null;
        // Verificar aliases legados primeiro
        String resolved = LEGACY_POTION_NAMES.getOrDefault(name.toUpperCase(), name.toLowerCase());
        PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.minecraft(resolved));
        return type;
    }

    // ========== Criação de ItemStack ==========

    /**
     * Cria uma instância de ItemStack de um custom item pelo ID.
     * 
     * @param id ID do item (ex: "lamina_gorvax")
     * @return ItemStack configurado, ou null se o ID não existir
     */
    public ItemStack getItem(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        if (def == null)
            return null;
        return buildItem(def);
    }

    private ItemStack buildItem(ItemDefinition def) {
        ItemStack item = new ItemStack(def.base);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // Nome
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(def.name));

        // Lore
        meta.lore(def.lore.stream()
                .map(s -> (Component) LegacyComponentSerializer.legacySection().deserialize(s))
                .toList());

        // PDC — marca o item como custom
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(customItemKey, PersistentDataType.STRING, def.id);

        // Enchants (permite acima do nível vanilla)
        for (Map.Entry<Enchantment, Integer> e : def.enchants.entrySet()) {
            meta.addEnchant(e.getKey(), e.getValue(), true);
        }

        // Item Model (1.21.4+) — vincula ao item definition do resource pack
        // Usa reflexão porque compilamos contra API 1.21 mas o servidor roda 1.21.11
        if (def.itemModel != null && !def.itemModel.isEmpty()) {
            try {
                String[] parts = def.itemModel.split(":", 2);
                NamespacedKey modelKey = parts.length == 2
                        ? new NamespacedKey(parts[0], parts[1])
                        : new NamespacedKey("gorvax", def.itemModel);
                java.lang.reflect.Method setItemModel = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
                setItemModel.setAccessible(true);
                setItemModel.invoke(meta, modelKey);
            } catch (NoSuchMethodException e) {
                // Servidor pré-1.21.4 — fallback para CMD
                if (def.customModelData > 0) {
                    meta.setCustomModelData(def.customModelData);
                }
            } catch (Exception e) {
                plugin.getLogger()
                        .warning("[CustomItems] Erro ao setar item_model para " + def.id + ": " + e.getMessage());
                if (def.customModelData > 0) {
                    meta.setCustomModelData(def.customModelData);
                }
            }
        } else if (def.customModelData > 0) {
            // Fallback legacy para itens sem item_model definido
            meta.setCustomModelData(def.customModelData);
        }

        // Flags visuais (esconde atributos vanilla para mostrar apenas os da lore)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(false);

        // Armor Trim — aplica visual custom via sistema de trims (1.20+)
        if (def.trimPattern != null && def.trimMaterial != null && meta instanceof ArmorMeta armorMeta) {
            try {
                TrimPattern pattern = Registry.TRIM_PATTERN.getOrThrow(
                        NamespacedKey.fromString(def.trimPattern));
                TrimMaterial material = Registry.TRIM_MATERIAL.getOrThrow(
                        NamespacedKey.fromString(def.trimMaterial));
                armorMeta.setTrim(new ArmorTrim(material, pattern));
                armorMeta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
            } catch (Exception e) {
                // Trim customizado (namespace gorvax:) não existe no registro vanilla —
                // esperado.
                plugin.getLogger().fine(
                        "[CustomItems] Trim customizado ignorado para " + def.id + ": " + e.getMessage());
            }
        }

        item.setItemMeta(meta);

        // Atributos (feito após setItemMeta para evitar reset)
        ItemMeta meta2 = item.getItemMeta();
        if (meta2 != null) {
            EquipmentSlotGroup slotGroup = getSlotGroup(def.base);
            for (Map.Entry<Attribute, Double> e : def.attributes.entrySet()) {
                AttributeModifier mod = new AttributeModifier(
                        new NamespacedKey(plugin, def.id + "_" + e.getKey().name().toLowerCase()),
                        e.getValue(),
                        AttributeModifier.Operation.ADD_NUMBER,
                        slotGroup);
                meta2.addAttributeModifier(e.getKey(), mod);
            }
            item.setItemMeta(meta2);
        }

        return item;
    }

    /**
     * Determina o EquipmentSlotGroup apropriado baseado no material.
     */
    private EquipmentSlotGroup getSlotGroup(Material mat) {
        String name = mat.name();
        if (name.endsWith("_HELMET") || name.endsWith("_CAP"))
            return EquipmentSlotGroup.HEAD;
        if (name.endsWith("_CHESTPLATE") || name.endsWith("_TUNIC"))
            return EquipmentSlotGroup.CHEST;
        if (name.endsWith("_LEGGINGS") || name.endsWith("_PANTS"))
            return EquipmentSlotGroup.LEGS;
        if (name.endsWith("_BOOTS"))
            return EquipmentSlotGroup.FEET;
        if (name.equals("SHIELD"))
            return EquipmentSlotGroup.OFFHAND;
        return EquipmentSlotGroup.HAND;
    }

    // ========== Consultas ==========

    /**
     * Verifica se um ItemStack é um custom item do GorvaxCore.
     */
    public boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(customItemKey, PersistentDataType.STRING);
    }

    /**
     * Retorna o ID do custom item, ou null se não for um custom item.
     */
    public String getCustomItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(customItemKey, PersistentDataType.STRING);
    }

    /**
     * Retorna os efeitos on-hit de um custom item pelo ID.
     */
    public List<OnHitEffect> getOnHitEffects(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        if (def == null)
            return Collections.emptyList();
        return def.onHitEffects;
    }

    /**
     * Retorna os efeitos passivos de um custom item pelo ID.
     */
    public List<PassiveEffect> getPassiveEffects(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        if (def == null)
            return Collections.emptyList();
        return def.passiveEffects;
    }

    /**
     * Retorna os efeitos visuais de partícula de um custom item pelo ID.
     */
    public List<ParticleEffect> getParticleEffects(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        if (def == null)
            return Collections.emptyList();
        return def.particleEffects;
    }

    /**
     * Retorna os sons de equip de um custom item pelo ID.
     */
    public List<SoundEffect> getEquipSounds(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        if (def == null)
            return Collections.emptyList();
        return def.equipSounds;
    }

    /**
     * Retorna os sons de swing/ataque de um custom item pelo ID.
     */
    public List<SoundEffect> getSwingSounds(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        if (def == null)
            return Collections.emptyList();
        return def.swingSounds;
    }

    /**
     * Retorna os sons de block/defesa de um custom item pelo ID.
     */
    public List<SoundEffect> getBlockSounds(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        if (def == null)
            return Collections.emptyList();
        return def.blockSounds;
    }

    /**
     * Retorna todos os IDs de itens registrados.
     */
    public List<String> getAllItemIds() {
        return new ArrayList<>(itemDefinitions.keySet());
    }

    /**
     * Retorna todos os IDs de itens que pertencem a um set (por sufixo).
     * Ex: getItemsBySet("gorvax") retorna todos os IDs que terminam com "_gorvax".
     */
    public List<String> getItemsBySet(String setName) {
        String suffix = "_" + setName.toLowerCase();
        return itemDefinitions.keySet().stream()
                .filter(id -> id.toLowerCase().endsWith(suffix))
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Retorna todos os nomes de sets disponíveis (extraídos dos sufixos dos IDs).
     * Ex: se existem itens "lamina_gorvax" e "coroa_gorvax", retorna ["gorvax"].
     */
    public List<String> getAllSetNames() {
        Set<String> sets = new java.util.TreeSet<>();
        for (String id : itemDefinitions.keySet()) {
            int lastUnderscore = id.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String suffix = id.substring(lastUnderscore + 1);
                // Só conta como set se tiver 2+ itens com esse sufixo
                long count = itemDefinitions.keySet().stream()
                        .filter(i -> i.endsWith("_" + suffix))
                        .count();
                if (count >= 2) {
                    sets.add(suffix);
                }
            }
        }
        return new ArrayList<>(sets);
    }

    /**
     * Retorna o nome formatado de um item pelo ID.
     */
    public String getItemName(String id) {
        ItemDefinition def = itemDefinitions.get(id);
        return def != null ? def.name : id;
    }

    /**
     * Recarrega os itens (para /gorvax reload).
     */
    public void reload() {
        loadItems();
    }

    /**
     * Retorna a NamespacedKey usada para identificar custom items no PDC.
     */
    public NamespacedKey getCustomItemKey() {
        return customItemKey;
    }
}
