package br.com.gorvax.core.towns.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.VoteManager;
import br.com.gorvax.core.towns.managers.KingdomManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Subcomando de votação do reino: criar, sim, nao, resultado, cancelar.
 * Extraído do KingdomCommand no Batch B20.
 */
public class KingdomVoteSubcommand {

    private final GorvaxCore plugin;

    public KingdomVoteSubcommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Despacha o subcomando de votação.
     */
    public boolean handle(Player p, String[] args) {
        var msg = plugin.getMessageManager();
        var km = plugin.getKingdomManager();
        VoteManager vm = plugin.getVoteManager();

        if (vm == null) {
            msg.send(p, "vote.not_available");
            return true;
        }

        Claim kingdom = km.getKingdom(p.getUniqueId());
        if (kingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return true;
        }

        String kingdomId = kingdom.getId();
        boolean isKing = km.isRei(kingdomId, p.getUniqueId());

        if (args.length < 2) {
            msg.send(p, "vote.usage");
            return true;
        }

        String voteSub = args[1].toLowerCase();

        switch (voteSub) {
            case "criar", "create" -> {
                if (!isKing) {
                    msg.send(p, "vote.only_king");
                    return true;
                }
                if (args.length < 3) {
                    msg.send(p, "vote.usage_create");
                    return true;
                }
                // Juntar a pergunta (args[2] em diante)
                StringBuilder questionBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2)
                        questionBuilder.append(" ");
                    questionBuilder.append(args[i]);
                }
                String question = questionBuilder.toString();

                boolean created = vm.createVote(kingdomId, question, p.getUniqueId());
                if (created) {
                    msg.send(p, "vote.created", question);
                    // Notificar membros online
                    for (String memberStr : km.getSuditosList(kingdomId)) {
                        try {
                            UUID memberUUID = UUID.fromString(memberStr);
                            Player member = Bukkit.getPlayer(memberUUID);
                            if (member != null && member.isOnline() && !member.equals(p)) {
                                msg.send(member, "vote.new_vote", question);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                } else {
                    msg.send(p, "vote.already_active");
                }
            }
            case "sim", "yes" -> {
                int result = vm.castVote(kingdomId, p.getUniqueId(), true);
                switch (result) {
                    case 0 -> msg.send(p, "vote.voted_yes");
                    case 1 -> msg.send(p, "vote.no_active");
                    case 2 -> msg.send(p, "vote.already_voted");
                    case 3 -> msg.send(p, "vote.expired");
                }
            }
            case "nao", "no" -> {
                int result = vm.castVote(kingdomId, p.getUniqueId(), false);
                switch (result) {
                    case 0 -> msg.send(p, "vote.voted_no");
                    case 1 -> msg.send(p, "vote.no_active");
                    case 2 -> msg.send(p, "vote.already_voted");
                    case 3 -> msg.send(p, "vote.expired");
                }
            }
            case "resultado", "result" -> {
                VoteManager.KingdomVote vote = vm.getActiveVote(kingdomId);
                if (vote == null) {
                    msg.send(p, "vote.no_active");
                    return true;
                }
                msg.send(p, "vote.result_header");
                msg.send(p, "vote.result_question", vote.question);
                msg.send(p, "vote.result_yes", vote.getYesCount());
                msg.send(p, "vote.result_no", vote.getNoCount());
                msg.send(p, "vote.result_total", vote.getTotalVotes());
                msg.send(p, "vote.result_remaining", vote.getRemainingTime());
                msg.send(p, "vote.result_footer");
            }
            case "cancelar", "cancel" -> {
                if (!isKing) {
                    msg.send(p, "vote.only_king");
                    return true;
                }
                boolean cancelled = vm.cancelVote(kingdomId);
                if (cancelled) {
                    msg.send(p, "vote.cancelled");
                } else {
                    msg.send(p, "vote.no_active");
                }
            }
            default -> msg.send(p, "vote.usage");
        }
        return true;
    }

    /**
     * Tab completion para subcomandos de votação.
     */
    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>(List.of("criar", "sim", "nao", "resultado", "cancelar"));
            return filterCompletions(completions, args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
