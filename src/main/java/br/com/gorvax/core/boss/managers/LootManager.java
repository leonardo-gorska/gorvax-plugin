package br.com.gorvax.core.boss.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LootManager {

    private final GorvaxCore plugin;
    private final Map<UUID, Inventory> personalLoot = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lootExpiry = new ConcurrentHashMap<>();
    private static final long LOOT_DURATION_MS = 5 * 60 * 1000L;
    private static final long LOOT_DURATION_TICKS = 5 * 60 * 20L;
    private static final long ABSOLUTE_TTL_MS = 7 * 24 * 60 * 60 * 1000L;
    private final Random random = new Random();
    private final File lootFile;
    private final org.bukkit.configuration.file.YamlConfiguration lootData;
    private final Map<UUID, Long> lootCreationTime = new ConcurrentHashMap<>();

    public LootManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.lootFile = new java.io.File(plugin.getDataFolder(), "loot_storage.yml");
        this.lootData = new org.bukkit.configuration.file.YamlConfiguration();
        loadLoot();
    }

    private void loadLoot() {
        if (!lootFile.exists())
            return;
        try {
            lootData.load(lootFile);
            long now = System.currentTimeMillis();
            boolean cleaned = false;

            for (String key : lootData.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long created = lootData.getLong(key + ".created", 0);
                    if (created == 0 && !lootData.isConfigurationSection(key))
                        created = now;

                    if (created != 0 && (now - created) > ABSOLUTE_TTL_MS) {
                        lootData.set(key, null);
                        cleaned = true;
                        continue;
                    }

                    List<?> items = lootData.isConfigurationSection(key) ? lootData.getList(key + ".items")
                            : lootData.getList(key);

                    if (items != null) {
                        Inventory inv = Bukkit.createInventory(null, 27,
                                LegacyComponentSerializer.legacySection().deserialize("§8Recompensa Real"));
                        int slot = 0;
                        for (Object obj : items) {
                            if (obj instanceof ItemStack is && slot < 27)
                                inv.setItem(slot++, is);
                        }
                        personalLoot.put(uuid, inv);
                        lootCreationTime.put(uuid, created == 0 ? now : created);
                        scheduleLootExpiry(uuid);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("Erro ao carregar loot de " + key + ": " + ex.getMessage());
                }
            }
            if (cleaned)
                lootData.save(lootFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao carregar loot_storage.yml: " + e.getMessage());
        }
    }

    public void saveLoot() {
        try {
            for (String key : lootData.getKeys(false))
                lootData.set(key, null);
            for (Map.Entry<UUID, Inventory> entry : personalLoot.entrySet()) {
                UUID uuid = entry.getKey();
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack is : entry.getValue().getContents()) {
                    if (is != null && is.getType() != Material.AIR)
                        items.add(is);
                }
                if (!items.isEmpty()) {
                    lootData.set(uuid.toString() + ".items", items);
                    lootData.set(uuid.toString() + ".created",
                            lootCreationTime.getOrDefault(uuid, System.currentTimeMillis()));
                }
            }
            lootData.save(lootFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao salvar loot_storage.yml: " + e.getMessage());
        }
    }

    public void generateLoot(Player player, int rank, String bossId) {
        generateLoot(player, rank, bossId, 1.0, null);
    }

    /**
     * B11 — Gera loot com multiplicador (raids) e lore extra (sazonal).
     * 
     * @param multiplier Multiplicador de quantidade (1.0 = normal, 2.0 = dobro)
     * @param extraLore  Linha de lore adicional (ex: "⚔ Recompensa de Raid"), null
     *                   para nenhum
     */
    public void generateLoot(Player player, int rank, String bossId, double multiplier, String extraLore) {
        if (rank <= 0)
            return;
        UUID uuid = player.getUniqueId();
        personalLoot.remove(uuid);
        lootCreationTime.put(uuid, System.currentTimeMillis());

        Inventory inv = Bukkit.createInventory(null, 27,
                LegacyComponentSerializer.legacySection().deserialize("§8Recompensa: " + rank + "º Lugar"));
        ConfigurationSection config = plugin.getBossManager().getConfigManager().getRewards()
                .getConfigurationSection(bossId);
        if (config == null)
            return;

        int expAmount = switch (rank) {
            case 1 -> 64;
            case 2 -> 50;
            case 3 -> 40;
            case 4 -> 30;
            case 5 -> 20;
            default -> random.nextInt(15) + 1;
        };
        inv.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, expAmount));

        if (rank == 1) {
            addRandomItems(inv, config.getConfigurationSection("armas_lendarias"), 1);
            addRandomItems(inv, config.getConfigurationSection("set_lendario"), 2);
            addRandomItems(inv, config.getConfigurationSection("ferramentas_lendarias"), 2);
        } else if (rank == 2) {
            addRandomItems(inv, config.getConfigurationSection("set_lendario"), 2);
            addRandomItems(inv, config.getConfigurationSection("ferramentas_lendarias"), 1);
        } else if (rank == 3) {
            addRandomItems(inv, config.getConfigurationSection("set_lendario"), 1);
            addRandomItems(inv, config.getConfigurationSection("ferramentas_lendarias"), 1);
        } else if (rank == 4) {
            addRandomItems(inv, config.getConfigurationSection("set_lendario"), 1);
        } else if (rank == 5) {
            addRandomItems(inv, config.getConfigurationSection("ferramentas_lendarias"), 1);
        } else {
            if (random.nextDouble() <= 0.02)
                addRandomItems(inv, config.getConfigurationSection("set_lendario"), 1);
            if (random.nextDouble() <= 0.02)
                addRandomItems(inv, config.getConfigurationSection("ferramentas_lendarias"), 1);
        }

        List<String> comuns = config.getStringList("itens_comuns");
        for (String s : comuns) {
            try {
                String[] split = s.split(":");
                Material mat = Material.valueOf(split[0].toUpperCase());
                int amt = Integer.parseInt(split[1]);
                if (random.nextDouble() <= 0.6)
                    inv.addItem(new ItemStack(mat, amt));
            } catch (Exception ignored) {
                plugin.getLogger().fine("Item comum inválido no loot: " + s + " - " + ignored.getMessage());
            }
        }

        // B11 — Aplicar multiplicador de raid nos itens stackáveis
        if (multiplier > 1.0) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR && item.getMaxStackSize() > 1) {
                    int newAmount = (int) Math.ceil(item.getAmount() * multiplier);
                    item.setAmount(Math.min(newAmount, item.getMaxStackSize()));
                }
            }
        }

        // B11 — Adicionar lore extra (sazonal/raid) em todos os itens com meta
        if (extraLore != null && !extraLore.isEmpty()) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore()
                                ? meta.lore().stream().map(c -> LegacyComponentSerializer.legacySection().serialize(c))
                                        .collect(Collectors.toCollection(ArrayList::new))
                                : null;
                        if (lore == null)
                            lore = new ArrayList<>();
                        lore.add("");
                        lore.add(extraLore);
                        meta.lore(lore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                                .collect(Collectors.toList()));
                        item.setItemMeta(meta);
                    }
                }
            }
        }

        personalLoot.put(uuid, inv);
        scheduleLootExpiry(uuid);
    }

    private void addRandomItems(Inventory inv, ConfigurationSection section, int amount) {
        if (section == null)
            return;
        List<String> keys = new ArrayList<>(section.getKeys(false));
        if (keys.isEmpty())
            return;
        Collections.shuffle(keys);
        for (int i = 0; i < Math.min(amount, keys.size()); i++) {
            ConfigurationSection itemSection = section.getConfigurationSection(keys.get(i));
            if (itemSection != null)
                inv.addItem(loadCustomItem(itemSection));
        }
    }

    private ItemStack loadCustomItem(ConfigurationSection section) {
        Material mat = Material.valueOf(section.getString("material", "NETHERITE_SWORD").toUpperCase());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.displayName(LegacyComponentSerializer.legacySection()
                .deserialize(section.getString("nome", "Item Lendário").replace("&", "§")));

        // Aplicar item_model para texturas customizadas do resource pack
        String itemModel = section.getString("item_model", null);
        if (itemModel != null && !itemModel.isEmpty()) {
            try {
                NamespacedKey modelKey = NamespacedKey.fromString(itemModel);
                java.lang.reflect.Method setItemModel = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
                setItemModel.invoke(meta, modelKey);
            } catch (NoSuchMethodException nsm) {
                // Fallback: tentar custom_model_data se API não suporta item_model
                int cmd = section.getInt("custom_model_data", 0);
                if (cmd > 0)
                    meta.setCustomModelData(cmd);
            } catch (Exception e) {
                plugin.getLogger().warning("[LootManager] item_model inválido: " + itemModel);
            }
        }

        List<String> finalLore = new ArrayList<>();
        List<String> configLore = section.getStringList("lore");
        configLore.forEach(l -> finalLore.add(l.replace("&", "§")));

        // --- ENCANTAMENTOS CUSTOMIZADOS (Java Fix para lvl > 10) ---
        List<String> encantosLines = section.getStringList("encantos");
        List<String> enchLore = new ArrayList<>();
        for (String enchStr : encantosLines) {
            try {
                String[] parts = enchStr.split(":");
                NamespacedKey key = NamespacedKey.minecraft(parts[0].toLowerCase());
                Enchantment ench = org.bukkit.Registry.ENCHANTMENT.get(key);
                if (ench != null) {
                    int level = Integer.parseInt(parts[1]);
                    meta.addEnchant(ench, level, true);
                    enchLore.add("§7" + getFriendlyEnchantName(ench) + " " + toRoman(level));
                }
            } catch (Exception ignored) {
                plugin.getLogger().fine("Encantamento inválido no loot: " + enchStr + " - " + ignored.getMessage());
            }
        }
        if (!enchLore.isEmpty()) {
            finalLore.add("");
            finalLore.addAll(enchLore);
        }

        List<String> atributos = section.getStringList("atributos");
        List<String> attrLore = new ArrayList<>();
        double baseDamage = getBaseDamage(mat);
        double baseSpeed = getBaseAttackSpeed(mat);
        double baseArmor = getBaseArmor(mat);
        double baseTough = getBaseToughness(mat);
        double baseKBRes = getBaseKBRes(mat);

        boolean hasCustomDamage = false;
        boolean hasCustomSpeed = false;
        boolean hasCustomArmor = false;
        boolean hasCustomTough = false;
        boolean hasCustomKBRes = false;

        for (String attrStr : atributos) {
            try {
                String[] parts = attrStr.split(":");
                Attribute attr = Attribute.valueOf(parts[0].toUpperCase());
                double bonusValue = Double.parseDouble(parts[1]);
                AttributeModifier.Operation op = parts.length > 2
                        ? AttributeModifier.Operation.valueOf(parts[2].toUpperCase())
                        : AttributeModifier.Operation.ADD_NUMBER;

                EquipmentSlot slot = parts.length > 3 ? EquipmentSlot.valueOf(parts[3].toUpperCase()) : null;
                EquipmentSlotGroup group = slot == null
                        ? (isArmor(mat) ? getArmorSlotGroup(mat) : EquipmentSlotGroup.MAINHAND)
                        : getSlotGroup(slot);

                double finalValue = bonusValue;
                String label = "", color = "§7";

                if (attr == Attribute.GENERIC_ATTACK_DAMAGE && op == AttributeModifier.Operation.ADD_NUMBER) {
                    finalValue += baseDamage;
                    hasCustomDamage = true;
                    label = "Dano de Ataque";
                    color = "§c";
                } else if (attr == Attribute.GENERIC_ATTACK_SPEED) {
                    if (op == AttributeModifier.Operation.ADD_NUMBER) {
                        finalValue += baseSpeed;
                    } else if (op == AttributeModifier.Operation.ADD_SCALAR) {
                        finalValue = ((4.0 + baseSpeed) * (1.0 + bonusValue)) - 4.0;
                        op = AttributeModifier.Operation.ADD_NUMBER;
                    }
                    hasCustomSpeed = true;
                    label = "Velocidade de Ataque";
                    color = "§e";
                } else if (attr == Attribute.GENERIC_ARMOR && op == AttributeModifier.Operation.ADD_NUMBER) {
                    finalValue += baseArmor;
                    hasCustomArmor = true;
                    label = "Armadura";
                    color = "§9";
                } else if (attr == Attribute.GENERIC_ARMOR_TOUGHNESS && op == AttributeModifier.Operation.ADD_NUMBER) {
                    finalValue += baseTough;
                    hasCustomTough = true;
                    label = "Resistência de Armadura";
                    color = "§9";
                } else if (attr == Attribute.GENERIC_KNOCKBACK_RESISTANCE
                        && op == AttributeModifier.Operation.ADD_NUMBER) {
                    finalValue += baseKBRes;
                    hasCustomKBRes = true;
                    label = "Resistência a Repulsão";
                    color = "§f";
                } else if (attr == Attribute.GENERIC_MAX_HEALTH) {
                    label = "Vida Máxima";
                    color = "§c";
                } else if (attr == Attribute.GENERIC_MOVEMENT_SPEED) {
                    label = "Velocidade de Movimento";
                    color = "§f";
                } else if (attr == Attribute.GENERIC_LUCK) {
                    label = "Sorte";
                    color = "§a";
                }

                meta.addAttributeModifier(attr, new AttributeModifier(
                        NamespacedKey.minecraft("gorvax_" + attr.name().toLowerCase()), finalValue, op, group));

                if (!label.isEmpty()) {
                    String sign = bonusValue >= 0 ? "+" : "";
                    String val = op == AttributeModifier.Operation.ADD_NUMBER ? String.format("%.1f", bonusValue)
                            : (int) (bonusValue * 100) + "%";
                    attrLore.add(" " + color + sign + val + " " + label);
                }
            } catch (Exception e) {
                plugin.getLogger().fine("Atributo inválido no loot: " + attrStr + " - " + e.getMessage());
            }
        }

        // --- BASE RESTORATION ---
        if (!hasCustomDamage && baseDamage > 1.0) {
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                    NamespacedKey.minecraft("gorvax_base_damage"), baseDamage,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        if (!hasCustomSpeed && baseSpeed != 0) {
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                    NamespacedKey.minecraft("gorvax_base_speed"), baseSpeed,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        if (!hasCustomArmor && baseArmor > 0) {
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                    NamespacedKey.minecraft("gorvax_base_armor"), baseArmor,
                    AttributeModifier.Operation.ADD_NUMBER, getArmorSlotGroup(mat)));
        }
        if (!hasCustomTough && baseTough > 0) {
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                    NamespacedKey.minecraft("gorvax_base_toughness"), baseTough,
                    AttributeModifier.Operation.ADD_NUMBER, getArmorSlotGroup(mat)));
        }
        if (!hasCustomKBRes && baseKBRes > 0) {
            meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                    NamespacedKey.minecraft("gorvax_base_kbres"), baseKBRes,
                    AttributeModifier.Operation.ADD_NUMBER, getArmorSlotGroup(mat)));
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (!attrLore.isEmpty()) {
            finalLore.add("");
            finalLore.add("§9Ao ser usado:");
            finalLore.addAll(attrLore);
        }

        meta.lore(finalLore.stream().map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                .collect(Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    private String getFriendlyEnchantName(Enchantment ench) {
        String key = ench.getKey().getKey().toLowerCase();
        return switch (key) {
            case "sharpness" -> "Afiada";
            case "fire_aspect" -> "Aspecto Flamejante";
            case "unbreaking" -> "Inquebrável";
            case "looting" -> "Pilhagem";
            case "sweeping_edge" -> "Alcance da Lâmina";
            case "efficiency" -> "Eficiência";
            case "power" -> "Força";
            case "flame" -> "Chama";
            case "infinity" -> "Infinito";
            case "punch" -> "Impacto";
            case "protection" -> "Proteção";
            case "respiration" -> "Respiração";
            case "aqua_affinity" -> "Afinidade Aquática";
            case "thorns" -> "Espinhos";
            case "soul_speed" -> "Velocidade das Almas";
            case "depth_strider" -> "Passos Profundos";
            case "feather_falling" -> "Peso Pena";
            case "mending" -> "Remendo";
            case "fortune" -> "Fortuna";
            case "knockback" -> "Repulsão";
            default -> key.substring(0, 1).toUpperCase() + key.substring(1).replace("_", " ");
        };
    }

    private String toRoman(int n) {
        if (n <= 0)
            return String.valueOf(n);
        String[] roman = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                n -= values[i];
                sb.append(roman[i]);
            }
        }
        return sb.toString();
    }

    private double getBaseDamage(Material mat) {
        String n = mat.name();
        if (n.contains("NETHERITE_SWORD"))
            return 8;
        if (n.contains("DIAMOND_SWORD"))
            return 7;
        if (n.contains("IRON_SWORD"))
            return 6;
        if (n.contains("STONE_SWORD"))
            return 5;
        if (n.contains("NETHERITE_AXE"))
            return 10;
        if (n.contains("DIAMOND_AXE") || n.contains("IRON_AXE") || n.contains("STONE_AXE"))
            return 9;
        if (n.contains("NETHERITE_PICKAXE"))
            return 6;
        if (n.contains("DIAMOND_PICKAXE"))
            return 5;
        if (n.contains("NETHERITE_SHOVEL"))
            return 6.5;
        return 1.0;
    }

    private double getBaseAttackSpeed(Material mat) {
        String n = mat.name();
        if (n.contains("SWORD"))
            return -2.4;
        if (n.contains("AXE"))
            return -3.0;
        if (n.contains("PICKAXE"))
            return -2.8;
        if (n.contains("SHOVEL"))
            return -3.0;
        if (n.contains("HOE"))
            return 0.0;
        return -2.4;
    }

    private double getBaseArmor(Material mat) {
        String n = mat.name();
        if (n.contains("NETHERITE") || n.contains("DIAMOND")) {
            if (n.contains("HELMET"))
                return 3;
            if (n.contains("CHESTPLATE"))
                return 8;
            if (n.contains("LEGGINGS"))
                return 6;
            if (n.contains("BOOTS"))
                return 3;
        }
        if (n.contains("IRON")) {
            if (n.contains("HELMET"))
                return 2;
            if (n.contains("CHESTPLATE"))
                return 6;
            if (n.contains("LEGGINGS"))
                return 5;
            if (n.contains("BOOTS"))
                return 2;
        }
        return 0;
    }

    private double getBaseToughness(Material mat) {
        String n = mat.name();
        if (n.contains("NETHERITE"))
            return 3;
        if (n.contains("DIAMOND"))
            return 2;
        return 0;
    }

    private double getBaseKBRes(Material mat) {
        String n = mat.name();
        if (n.contains("NETHERITE"))
            return 1.0;
        return 0;
    }

    private boolean isArmor(Material mat) {
        String n = mat.name();
        return n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("BOOTS");
    }

    private EquipmentSlotGroup getArmorSlotGroup(Material mat) {
        String n = mat.name();
        if (n.contains("HELMET"))
            return EquipmentSlotGroup.HEAD;
        if (n.contains("CHESTPLATE"))
            return EquipmentSlotGroup.CHEST;
        if (n.contains("LEGGINGS"))
            return EquipmentSlotGroup.LEGS;
        if (n.contains("BOOTS"))
            return EquipmentSlotGroup.FEET;
        return EquipmentSlotGroup.MAINHAND;
    }

    private EquipmentSlotGroup getSlotGroup(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
            default -> EquipmentSlotGroup.MAINHAND;
        };
    }

    public boolean hasLoot(Player p) {
        return personalLoot.containsKey(p.getUniqueId());
    }

    public void openPersonalLoot(Player p) {
        if (hasLoot(p))
            p.openInventory(personalLoot.get(p.getUniqueId()));
    }

    public void removeLoot(UUID uuid) {
        personalLoot.remove(uuid);
        lootExpiry.remove(uuid);
    }

    public void clearLootCache() {
        personalLoot.clear();
        lootExpiry.clear();
    }

    public void clearLoot() {
        clearLootCache();
    }

    private void scheduleLootExpiry(UUID uuid) {
        lootExpiry.put(uuid, System.currentTimeMillis() + LOOT_DURATION_MS);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Inventory inv = personalLoot.get(uuid);
            if (inv == null)
                return;
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline())
                return;
            org.bukkit.Location dropLoc = p.getLocation();
            p.sendMessage(GorvaxCore.getInstance().getMessageManager().get("boss_listener.treasure_expired"));
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR)
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item);
            }
            removeLoot(uuid);
        }, LOOT_DURATION_TICKS);
    }

    public long getRemainingSeconds(UUID uuid) {
        Long expiry = lootExpiry.get(uuid);
        return expiry == null ? 0 : Math.max(0, (expiry - System.currentTimeMillis()) / 1000);
    }
}