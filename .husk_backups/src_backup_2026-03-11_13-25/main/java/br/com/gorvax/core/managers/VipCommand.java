package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.VipManager.VipTier;
import br.com.gorvax.core.managers.VipManager.TierBenefits;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * B14 — Comando /vip com subcomandos para informações, status e administração de VIP.
 *
 * Subcomandos para jogadores:
 *   /vip           — mostra tabela de benefícios
 *   /vip info      — mostra tabela de benefícios
 *   /vip status    — mostra tier atual e benefícios ativos
 *
 * Subcomandos admin (gorvax.admin):
 *   /vip set <jogador> <tier>    — define tier VIP
 *   /vip remove <jogador>        — remove VIP
 *   /vip keys                    — distribui keys mensais manualmente
 *   /vip reload                  — recarrega configurações VIP
 */
public class VipCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public VipCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        var msg = plugin.getMessageManager();
        VipManager vipManager = plugin.getVipManager();

        if (vipManager == null || !vipManager.isEnabled()) {
            msg.send(sender, "vip.disabled");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            showInfo(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "status" -> {
                if (!(sender instanceof Player p)) {
                    msg.send(sender, "general.player_only");
                    return true;
                }
                showStatus(p);
            }

            case "set" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 3) {
                    msg.send(sender, "vip.set_usage");
                    return true;
                }
                handleSet(sender, args[1], args[2]);
            }

            case "remove", "remover" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(sender, "vip.remove_usage");
                    return true;
                }
                handleRemove(sender, args[1]);
            }

            case "keys" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                vipManager.distributeMonthlyKeys();
                msg.send(sender, "vip.keys_distributed");
            }

            case "reload" -> {
                if (!sender.hasPermission("gorvax.admin")) {
                    msg.send(sender, "general.no_permission");
                    return true;
                }
                vipManager.reload();
                msg.send(sender, "vip.reload_success");
            }

            default -> msg.send(sender, "vip.unknown_command");
        }

        return true;
    }

    /**
     * Mostra tabela comparativa de benefícios VIP.
     */
    private void showInfo(CommandSender sender) {
        var msg = plugin.getMessageManager();
        sender.sendMessage(msg.get("vip.info_header"));
        sender.sendMessage("");

        VipManager vm = plugin.getVipManager();

        // Exibir cada tier
        for (VipTier tier : new VipTier[]{VipTier.VIP, VipTier.VIP_PLUS, VipTier.ELITE, VipTier.LENDARIO}) {
            TierBenefits b = vm.getBenefits(tier);
            if (b == null) continue;

            sender.sendMessage("  " + tier.getDisplayName());
            sender.sendMessage(msg.get("vip.info_tier_blocks", b.extraClaimBlocks()));
            sender.sendMessage(msg.get("vip.info_tier_homes", b.extraHomes()));

            if (b.marketDiscountPercent() > 0) {
                sender.sendMessage(msg.get("vip.info_tier_discount", (int) b.marketDiscountPercent()));
            }

            if (!b.monthlyKeys().isEmpty()) {
                StringBuilder keysStr = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String, Integer> entry : b.monthlyKeys().entrySet()) {
                    if (!first) keysStr.append(" §7| ");
                    keysStr.append(msg.get("vip.info_tier_key_entry", entry.getValue(), entry.getKey()));
                    first = false;
                }
                sender.sendMessage(msg.get("vip.info_tier_keys", keysStr.toString()));
            }

            sender.sendMessage("");
        }

        sender.sendMessage(msg.get("vip.info_footer"));
    }

    /**
     * Mostra o status VIP atual do jogador.
     */
    private void showStatus(Player player) {
        var msg = plugin.getMessageManager();
        VipManager vm = plugin.getVipManager();
        VipTier tier = vm.getVipTier(player);

        player.sendMessage(msg.get("vip.status_header"));
        player.sendMessage("");

        if (tier == VipTier.NONE) {
            player.sendMessage(msg.get("vip.status_none_rank"));
            player.sendMessage("");
            player.sendMessage(msg.get("vip.status_none_hint_shop"));
            player.sendMessage(msg.get("vip.status_none_hint_info"));
        } else {
            TierBenefits b = vm.getBenefits(tier);

            player.sendMessage(msg.get("vip.status_rank", tier.getDisplayName()));
            player.sendMessage("");
            player.sendMessage(msg.get("vip.status_benefits_header"));
            player.sendMessage(msg.get("vip.status_blocks", b.extraClaimBlocks()));
            player.sendMessage(msg.get("vip.status_homes", b.extraHomes()));

            if (b.marketDiscountPercent() > 0) {
                player.sendMessage(msg.get("vip.status_discount", (int) b.marketDiscountPercent()));
            }

            if (!b.monthlyKeys().isEmpty()) {
                player.sendMessage(msg.get("vip.status_keys_header"));
                for (Map.Entry<String, Integer> entry : b.monthlyKeys().entrySet()) {
                    player.sendMessage(msg.get("vip.status_key_entry", entry.getValue(), entry.getKey()));
                }
            }
        }

        player.sendMessage("");
        player.sendMessage(msg.get("vip.status_footer"));
    }

    /**
     * Define o tier VIP de um jogador (admin).
     */
    private void handleSet(CommandSender sender, String targetName, String tierName) {
        var msg = plugin.getMessageManager();
        VipManager vm = plugin.getVipManager();

        VipTier tier = vm.parseTier(tierName);
        if (tier == null) {
            msg.send(sender, "vip.invalid_tier", tierName);
            return;
        }

        // Buscar jogador (online ou offline)
        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            var offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (!offlinePlayer.hasPlayedBefore()) {
                msg.send(sender, "general.player_never_played");
                return;
            }
            targetUuid = offlinePlayer.getUniqueId();
        }

        boolean success = vm.setVipTier(targetUuid, tier);
        if (success) {
            msg.send(sender, "vip.set_success", targetName, tier.getDisplayName());

            // Se online, aplicar benefícios imediatamente
            if (target != null) {
                vm.applyVipBenefits(target);
                msg.send(target, "vip.tier_changed", tier.getDisplayName());
            }
        } else {
            msg.send(sender, "vip.set_error");
        }
    }

    /**
     * Remove VIP de um jogador (admin).
     */
    private void handleRemove(CommandSender sender, String targetName) {
        var msg = plugin.getMessageManager();
        VipManager vm = plugin.getVipManager();

        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;

        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            var offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (!offlinePlayer.hasPlayedBefore()) {
                msg.send(sender, "general.player_never_played");
                return;
            }
            targetUuid = offlinePlayer.getUniqueId();
        }

        boolean success = vm.setVipTier(targetUuid, VipTier.NONE);
        if (success) {
            msg.send(sender, "vip.remove_success", targetName);
            if (target != null) {
                msg.send(target, "vip.tier_removed");
            }
        } else {
            msg.send(sender, "vip.set_error");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("info", "status"));
            if (sender.hasPermission("gorvax.admin")) {
                subs.addAll(Arrays.asList("set", "remove", "keys", "reload"));
            }
            return filterCompletions(subs, args[0]);
        }

        if (args.length == 2 && sender.hasPermission("gorvax.admin")) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) {
                return null; // Retorna lista de jogadores online
            }
        }

        if (args.length == 3 && sender.hasPermission("gorvax.admin") && args[0].equalsIgnoreCase("set")) {
            return filterCompletions(
                    Arrays.asList("vip", "vip_plus", "elite", "lendario", "none"),
                    args[2]
            );
        }

        return List.of();
    }

    private List<String> filterCompletions(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
