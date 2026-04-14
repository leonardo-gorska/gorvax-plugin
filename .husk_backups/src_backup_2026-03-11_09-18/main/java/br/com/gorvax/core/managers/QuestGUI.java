package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * B16 — GUI de Quests Diárias e Semanais.
 * Inventário de 27 slots com indicadores visuais de progresso.
 */
public class QuestGUI implements Listener {

    private static final LegacyComponentSerializer LCS = LegacyComponentSerializer.legacySection();
    private static final String GUI_TITLE = "§6§lQuests Diárias & Semanais";
    private static final int GUI_SIZE = 27;

    private final GorvaxCore plugin;

    public QuestGUI(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre a GUI de quests para um jogador.
     */
    public void open(Player player) {
        QuestManager qm = plugin.getQuestManager();
        UUID uuid = player.getUniqueId();
        PlayerData pd = plugin.getPlayerDataManager().getData(uuid);

        // Verificar se precisa resetar
        qm.checkAndResetIfNeeded(pd);

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, LCS.deserialize(GUI_TITLE));

        // Preencher bordas com vidro cinza
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, border);
        }

        // Header: Info geral (slot 4)
        ItemStack header = createItem(Material.BOOK, "§e§lSuas Quests",
                List.of(
                        "§7Complete missões para ganhar",
                        "§7recompensas especiais!",
                        "",
                        "§fDiárias §8— §7Resetam todo dia",
                        "§6Semanal §8— §7Reseta toda semana",
                        "",
                        "§eClique §7nas quests completas",
                        "§7para resgatar recompensas!"));
        inv.setItem(4, header);

        // Quests Diárias (slots 10, 12, 14)
        int[] dailySlots = { 10, 12, 14 };
        List<String> dailyIds = qm.getActiveDailyIds();

        for (int i = 0; i < dailySlots.length && i < dailyIds.size(); i++) {
            String questId = dailyIds.get(i);
            QuestManager.QuestDefinition def = qm.getQuest(questId);
            if (def == null)
                continue;

            inv.setItem(dailySlots[i], buildQuestItem(uuid, questId, def, false));
        }

        // Label "Diárias" (slot 9)
        ItemStack dailyLabel = createItem(Material.SUNFLOWER, "§e§lQuests Diárias",
                List.of("§7Resetam §fà meia-noite§7."));
        inv.setItem(9, dailyLabel);

        // Quest Semanal (slot 22 centralizado)
        String weeklyId = qm.getActiveWeeklyId();
        if (weeklyId != null) {
            QuestManager.QuestDefinition def = qm.getQuest(weeklyId);
            if (def != null) {
                inv.setItem(22, buildQuestItem(uuid, weeklyId, def, true));
            }
        }

        // Label "Semanal" (slot 18)
        ItemStack weeklyLabel = createItem(Material.CLOCK, "§6§lQuest Semanal",
                List.of("§7Reseta toda §fSegunda-feira§7."));
        inv.setItem(18, weeklyLabel);

        player.openInventory(inv);
    }

    /**
     * Constrói o item visual de uma quest baseado no estado atual do jogador.
     */
    private ItemStack buildQuestItem(UUID uuid, String questId, QuestManager.QuestDefinition def, boolean isWeekly) {
        QuestManager qm = plugin.getQuestManager();
        int progress = qm.getProgress(uuid, questId);
        boolean completed = progress >= def.amount();
        boolean claimed = qm.isClaimed(uuid, questId);

        Material material;
        String displayName;
        List<String> lore = new ArrayList<>();

        if (claimed) {
            // Resgatada — vidro verde
            material = Material.LIME_STAINED_GLASS_PANE;
            displayName = "§a§l✔ " + def.name();
            lore.add("§a§lRESGATADA!");
            lore.add("");
            lore.add("§7Recompensa recebida:");
            addRewardLore(def, lore);
        } else if (completed) {
            // Completa — baú dourado, clicável
            material = Material.CHEST;
            displayName = "§a§l★ " + def.name();
            lore.add("§a§lQUEST COMPLETA!");
            lore.add("");
            lore.add("§eProgresso: §a" + progress + "/" + def.amount() + " §a§l✔");
            lore.add("");
            lore.add("§7Recompensa:");
            addRewardLore(def, lore);
            lore.add("");
            lore.add("§a§l▸ CLIQUE PARA RESGATAR ◂");
        } else {
            // Em progresso — ícone da quest com barra
            material = def.icon();
            displayName = (isWeekly ? "§6" : "§e") + def.name();
            lore.add("§7" + def.description());
            lore.add("");
            lore.add("§eProgresso: §f" + progress + "/" + def.amount()
                    + " " + buildProgressBar(progress, def.amount()));
            lore.add("");
            lore.add("§7Recompensa:");
            addRewardLore(def, lore);
        }

        lore.add("");
        lore.add(isWeekly ? "§8Tipo: §6Semanal" : "§8Tipo: §eForça");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LCS.deserialize(displayName));
            meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());

            // Enchantment glow para quests completas (não resgatadas)
            if (completed && !claimed) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private String buildProgressBar(int current, int max) {
        int bars = 10;
        int filled = max > 0 ? (int) ((double) current / max * bars) : 0;
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§a█");
            } else {
                sb.append("§7░");
            }
        }
        sb.append("§8]");
        return sb.toString();
    }

    private void addRewardLore(QuestManager.QuestDefinition def, List<String> lore) {
        if (def.rewardMoney() > 0)
            lore.add("  §6$" + String.format("%.0f", def.rewardMoney()));
        if (def.rewardClaimBlocks() > 0)
            lore.add("  §a+" + def.rewardClaimBlocks() + " blocos de proteção");
        if (def.rewardCrateKey() != null)
            lore.add("  §d1x Key " + def.rewardCrateKey());
        if (def.rewardTitle() != null)
            lore.add("  §bTítulo: " + def.rewardTitle());
    }

    // ===== Listener =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;
        if (!(e.getView().title() instanceof net.kyori.adventure.text.TextComponent))
            return;
        if (!e.getView().title().equals(LCS.deserialize(GUI_TITLE)))
            return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE)
            return;

        QuestManager qm = plugin.getQuestManager();
        UUID uuid = player.getUniqueId();

        // Mapear slot para quest ID
        String questId = getQuestIdBySlot(slot, qm);
        if (questId == null)
            return;

        // Tentar resgatar
        if (qm.isCompleted(uuid, questId) && !qm.isClaimed(uuid, questId)) {
            boolean success = qm.claimReward(player, questId);
            if (success) {
                // Reabrir GUI para atualizar
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
            }
        }
    }

    /**
     * Resolve qual questId está associado a um slot.
     */
    private String getQuestIdBySlot(int slot, QuestManager qm) {
        int[] dailySlots = { 10, 12, 14 };
        List<String> dailyIds = qm.getActiveDailyIds();

        for (int i = 0; i < dailySlots.length && i < dailyIds.size(); i++) {
            if (slot == dailySlots[i])
                return dailyIds.get(i);
        }

        if (slot == 22 && qm.getActiveWeeklyId() != null) {
            return qm.getActiveWeeklyId();
        }

        return null;
    }

    // ===== Utilitários =====

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LCS.deserialize(name));
            if (lore != null)
                meta.lore(lore.stream().map(LCS::deserialize).map(c -> (Component) c).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
