package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * B17.1 — Comando /carta com subcomandos.
 * Subcomandos: enviar, ler, deletar, limpar
 */
public class MailCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public MailCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                              String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.getMessageManager().get("general.player_only"));
            return true;
        }

        var msg = plugin.getMessageManager();
        MailManager mailManager = plugin.getMailManager();

        if (mailManager == null) {
            msg.send(p, "mail.not_available");
            return true;
        }

        if (args.length == 0) {
            showHelp(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "enviar", "send" -> handleSend(p, args);
            case "ler", "read" -> handleRead(p, args);
            case "deletar", "delete" -> handleDelete(p, args);
            case "limpar", "clear" -> handleClear(p);
            case "ajuda", "help" -> showHelp(p);
            default -> {
                msg.send(p, "mail.unknown_command");
                showHelp(p);
            }
        }

        return true;
    }

    private void handleSend(Player p, String[] args) {
        var msg = plugin.getMessageManager();

        if (args.length < 3) {
            msg.send(p, "mail.usage_send");
            return;
        }

        String targetName = args[1];

        // Buscar jogador (online ou offline)
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            msg.send(p, "mail.player_not_found");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        // Não pode enviar para si mesmo
        if (targetUUID.equals(p.getUniqueId())) {
            msg.send(p, "mail.cannot_self");
            return;
        }

        // Juntar a mensagem (args[2] em diante)
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Verificar tamanho
        if (message.length() > plugin.getMailManager().getMaxMessageLength()) {
            msg.send(p, "mail.too_long", plugin.getMailManager().getMaxMessageLength());
            return;
        }

        boolean success = plugin.getMailManager().sendMail(
                p.getUniqueId(), p.getName(), targetUUID, message);

        if (success) {
            msg.send(p, "mail.sent", targetName);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

            // Se o alvo estiver online, notificar imediatamente
            Player onlineTarget = Bukkit.getPlayer(targetUUID);
            if (onlineTarget != null && onlineTarget.isOnline()) {
                msg.send(onlineTarget, "mail.received_notification", p.getName());
                onlineTarget.playSound(onlineTarget.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
            }
        } else {
            msg.send(p, "mail.target_full");
        }
    }

    private void handleRead(Player p, String[] args) {
        var msg = plugin.getMessageManager();
        MailManager mailManager = plugin.getMailManager();

        List<MailManager.MailEntry> mails = mailManager.getMail(p.getUniqueId());

        if (mails.isEmpty()) {
            msg.send(p, "mail.empty");
            return;
        }

        // Paginação
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }

        int perPage = 5;
        int totalPages = (int) Math.ceil((double) mails.size() / perPage);
        page = Math.max(1, Math.min(page, totalPages));

        int startIndex = (page - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, mails.size());

        p.sendMessage(msg.get("mail.header", page, totalPages));
        p.sendMessage(msg.get("mail.separator"));

        for (int i = startIndex; i < endIndex; i++) {
            MailManager.MailEntry mail = mails.get(i);
            String readIcon = mail.read ? "§7✉" : "§a✉";
            p.sendMessage(msg.get("mail.entry",
                    readIcon, i + 1, mail.senderName, mail.getFormattedDate()));
            p.sendMessage(msg.get("mail.entry_message", mail.message));
        }

        p.sendMessage(msg.get("mail.separator"));

        if (page < totalPages) {
            p.sendMessage(msg.get("mail.next_page", page + 1));
        }

        // Marcar como lidas
        mailManager.markAllRead(p.getUniqueId());
    }

    private void handleDelete(Player p, String[] args) {
        var msg = plugin.getMessageManager();

        if (args.length < 2) {
            msg.send(p, "mail.usage_delete");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[1]) - 1; // 1-indexed para o jogador
        } catch (NumberFormatException e) {
            msg.send(p, "mail.invalid_index");
            return;
        }

        boolean success = plugin.getMailManager().deleteMail(p.getUniqueId(), index);
        if (success) {
            msg.send(p, "mail.deleted");
        } else {
            msg.send(p, "mail.invalid_index");
        }
    }

    private void handleClear(Player p) {
        var msg = plugin.getMessageManager();
        int removed = plugin.getMailManager().clearRead(p.getUniqueId());
        msg.send(p, "mail.cleared", removed);
    }

    private void showHelp(Player p) {
        var msg = plugin.getMessageManager();
        p.sendMessage(msg.get("mail.help_header"));
        p.sendMessage(msg.get("mail.help_send"));
        p.sendMessage(msg.get("mail.help_read"));
        p.sendMessage(msg.get("mail.help_delete"));
        p.sendMessage(msg.get("mail.help_clear"));
        p.sendMessage(msg.get("mail.help_footer"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                       @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("enviar");
            completions.add("ler");
            completions.add("deletar");
            completions.add("limpar");
            completions.add("ajuda");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("enviar") || sub.equals("send")) {
                // Sugerir jogadores online
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("ler") || sub.equals("read")) {
                completions.add("<pagina>");
            }
            if (sub.equals("deletar") || sub.equals("delete")) {
                completions.add("<numero>");
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("enviar") || sub.equals("send")) {
                completions.add("<mensagem>");
            }
        }

        return completions;
    }
}
