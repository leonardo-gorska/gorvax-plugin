package br.com.gorvax.core.managers;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para CustomItemManager.
 * Testa lógica de getSlotGroup (replicada) e validação de records.
 * Records que usam PotionEffectType/Particle não podem ser testados
 * sem runtime Bukkit (registry initialization), então testamos apenas lógica
 * pura.
 */
class CustomItemManagerTest {

    // --- getSlotGroup (lógica privada replicada) ---

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

    @Test
    void slotGroupHelmet() {
        assertEquals(EquipmentSlotGroup.HEAD, getSlotGroup(Material.DIAMOND_HELMET));
    }

    @Test
    void slotGroupIronHelmet() {
        assertEquals(EquipmentSlotGroup.HEAD, getSlotGroup(Material.IRON_HELMET));
    }

    @Test
    void slotGroupGoldenHelmet() {
        assertEquals(EquipmentSlotGroup.HEAD, getSlotGroup(Material.GOLDEN_HELMET));
    }

    @Test
    void slotGroupChestplate() {
        assertEquals(EquipmentSlotGroup.CHEST, getSlotGroup(Material.IRON_CHESTPLATE));
    }

    @Test
    void slotGroupDiamondChestplate() {
        assertEquals(EquipmentSlotGroup.CHEST, getSlotGroup(Material.DIAMOND_CHESTPLATE));
    }

    @Test
    void slotGroupLeggings() {
        assertEquals(EquipmentSlotGroup.LEGS, getSlotGroup(Material.NETHERITE_LEGGINGS));
    }

    @Test
    void slotGroupBoots() {
        assertEquals(EquipmentSlotGroup.FEET, getSlotGroup(Material.LEATHER_BOOTS));
    }

    @Test
    void slotGroupDiamondBoots() {
        assertEquals(EquipmentSlotGroup.FEET, getSlotGroup(Material.DIAMOND_BOOTS));
    }

    @Test
    void slotGroupShield() {
        assertEquals(EquipmentSlotGroup.OFFHAND, getSlotGroup(Material.SHIELD));
    }

    @Test
    void slotGroupSword() {
        assertEquals(EquipmentSlotGroup.HAND, getSlotGroup(Material.DIAMOND_SWORD));
    }

    @Test
    void slotGroupBow() {
        assertEquals(EquipmentSlotGroup.HAND, getSlotGroup(Material.BOW));
    }

    @Test
    void slotGroupStick() {
        assertEquals(EquipmentSlotGroup.HAND, getSlotGroup(Material.STICK));
    }

    @Test
    void slotGroupTrident() {
        assertEquals(EquipmentSlotGroup.HAND, getSlotGroup(Material.TRIDENT));
    }

    @Test
    void slotGroupCrossbow() {
        assertEquals(EquipmentSlotGroup.HAND, getSlotGroup(Material.CROSSBOW));
    }

    // --- SoundEffect record (usa String, sem dependência Bukkit) ---

    @Test
    void soundEffectCampos() {
        var effect = new CustomItemManager.SoundEffect("entity.player.attack.sweep", 1.0f, 1.2f);
        assertEquals("entity.player.attack.sweep", effect.sound());
        assertEquals(1.0f, effect.volume(), 0.001);
        assertEquals(1.2f, effect.pitch(), 0.001);
    }

    @Test
    void soundEffectVolumeZero() {
        var effect = new CustomItemManager.SoundEffect("block.anvil.place", 0.0f, 0.5f);
        assertEquals(0.0f, effect.volume(), 0.001);
    }

    @Test
    void soundEffectPitchAlto() {
        var effect = new CustomItemManager.SoundEffect("entity.ender_dragon.growl", 0.8f, 2.0f);
        assertEquals(2.0f, effect.pitch(), 0.001);
    }
}
