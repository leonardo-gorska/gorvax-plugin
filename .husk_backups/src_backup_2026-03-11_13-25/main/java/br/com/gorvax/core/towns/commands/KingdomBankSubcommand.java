package br.com.gorvax.core.towns.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.MessageManager;
import br.com.gorvax.core.towns.managers.KingdomManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subcomando de banco do reino: depositar, sacar, banco.
 * Extraído do KingdomCommand no Batch B20.
 */
public class KingdomBankSubcommand {

    private final GorvaxCore plugin;

    public KingdomBankSubcommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Despacha o subcomando de banco.
     * 
     * @param p    jogador
     * @param sub  subcomando (depositar/deposit, sacar/withdraw, banco/bank)
     * @param args argumentos completos do comando /reino
     * @return true se o subcomando foi tratado
     */
    public boolean handle(Player p, String sub, String[] args) {
        switch (sub) {
            case "depositar":
            case "deposit":
                if (args.length < 2) {
                    plugin.getMessageManager().send(p, "kingdom.bank_deposit_usage");
                    return true;
                }
                handleBankDeposit(p, args[1]);
                return true;
            case "sacar":
            case "withdraw":
                if (args.length < 2) {
                    plugin.getMessageManager().send(p, "kingdom.bank_withdraw_usage");
                    return true;
                }
                handleBankWithdraw(p, args[1]);
                return true;
            case "banco":
            case "bank":
                handleBankInfo(p);
                return true;
            default:
                return false;
        }
    }

    private void handleBankDeposit(Player p, String amountStr) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            msg.send(p, "general.invalid_value");
            return;
        }

        if (amount <= 0) {
            msg.send(p, "general.invalid_value");
            return;
        }

        if (GorvaxCore.getEconomy() == null || !GorvaxCore.getEconomy().has(p, amount)) {
            msg.send(p, "kingdom.bank_insufficient_personal");
            return;
        }

        GorvaxCore.getEconomy().withdrawPlayer(p, amount);
        plugin.getKingdomManager().depositToBank(kingdom.getId(), amount);

        double newBalance = plugin.getKingdomManager().getBankBalance(kingdom.getId());
        msg.send(p, "kingdom.bank_deposit_success", String.format("%.2f", amount), String.format("%.2f", newBalance));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    private void handleBankWithdraw(Player p, String amountStr) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }

        if (!plugin.getKingdomManager().isRei(kingdom.getId(), p.getUniqueId())) {
            msg.send(p, "kingdom.bank_withdraw_only_king");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            msg.send(p, "general.invalid_value");
            return;
        }

        if (amount <= 0) {
            msg.send(p, "general.invalid_value");
            return;
        }

        if (!plugin.getKingdomManager().withdrawFromBank(kingdom.getId(), amount)) {
            msg.send(p, "kingdom.bank_insufficient");
            return;
        }

        GorvaxCore.getEconomy().depositPlayer(p, amount);
        double newBalance = plugin.getKingdomManager().getBankBalance(kingdom.getId());
        msg.send(p, "kingdom.bank_withdraw_success", String.format("%.2f", amount), String.format("%.2f", newBalance));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
    }

    private void handleBankInfo(Player p) {
        var msg = plugin.getMessageManager();
        Claim kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return;
        }

        String kingdomId = kingdom.getId();
        double balance = plugin.getKingdomManager().getBankBalance(kingdomId);
        double tax = plugin.getKingdomManager().getResidentTax(kingdomId);
        int suditos = plugin.getKingdomManager().getSuditosCount(kingdomId) - 1; // Exclui o Rei
        double dailyIncome = tax * suditos;

        // Calcular upkeep diário
        int width = kingdom.getMaxX() - kingdom.getMinX() + 1;
        int length = kingdom.getMaxZ() - kingdom.getMinZ() + 1;
        int chunks = Math.max(1, (width * length) / (16 * 16));
        double perChunk = plugin.getConfig().getDouble("kingdoms.upkeep.per_chunk", 10.0);
        double dailyUpkeep = perChunk * chunks;

        int level = plugin.getKingdomManager().getKingdomLevel(kingdomId);
        int maxMembers = plugin.getKingdomManager().getMaxMembers(kingdomId);
        int currentMembers = plugin.getKingdomManager().getSuditosCount(kingdomId);
        boolean decaying = plugin.getKingdomManager().isDecaying(kingdomId);

        msg.send(p, "kingdom.bank_header");
        msg.send(p, "kingdom.bank_balance", String.format("%.2f", balance));
        msg.send(p, "kingdom.bank_tax_info", String.format("%.2f", tax), suditos, String.format("%.2f", dailyIncome));
        msg.send(p, "kingdom.bank_upkeep_info", String.format("%.2f", dailyUpkeep), chunks);
        msg.send(p, "kingdom.bank_level_info", level, currentMembers, maxMembers);
        if (decaying) {
            msg.send(p, "kingdom.bank_decaying_warning");
        }
        msg.send(p, "kingdom.bank_footer");
    }

    /**
     * Tab completion do subcomando banco.
     */
    public List<String> tabComplete(String sub, String[] args) {
        // Sem tab completion adicional para banco
        return new ArrayList<>();
    }
}
