package br.com.gorvax.core.towns.commands;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.towns.managers.KingdomManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subcomando de outposts do reino: criar, setspawn, spawn, listar, deletar.
 * Extraído do KingdomCommand no Batch B20.
 */
public class KingdomOutpostSubcommand {

    private final GorvaxCore plugin;

    public KingdomOutpostSubcommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Despacha o subcomando de outpost.
     */
    public boolean handle(Player p, String[] args) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();
        Claim kingdom = km.getKingdom(p.getUniqueId());

        if (kingdom == null) {
            msg.send(p, "kingdom.error_no_kingdom");
            return true;
        }

        if (!km.isRei(kingdom.getId(), p.getUniqueId())) {
            msg.send(p, "kingdom.error_not_king");
            return true;
        }

        String sub = (args.length >= 2) ? args[1].toLowerCase() : "listar";

        switch (sub) {
            case "criar":
            case "create":
                handleOutpostCreate(p, kingdom);
                break;
            case "setspawn":
                handleOutpostSetSpawn(p, kingdom);
                break;
            case "spawn":
                handleOutpostSpawn(p, kingdom, args);
                break;
            case "listar":
            case "list":
                handleOutpostList(p, kingdom);
                break;
            case "deletar":
            case "delete":
                handleOutpostDelete(p, kingdom);
                break;
            default:
                msg.send(p, "outpost.usage");
                break;
        }
        return true;
    }

    private void handleOutpostCreate(Player p, Claim kingdom) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        int current = km.getOutpostCount(kingdom.getId());
        int max = km.getMaxOutposts(kingdom.getId());

        if (current >= max) {
            msg.send(p, "outpost.limit_reached", current, max);
            return;
        }

        // Verifica distância mínima do claim principal
        int minDist = plugin.getConfig().getInt("outposts.min_distance", 200);
        Location loc = p.getLocation();
        int centerX = (kingdom.getMinX() + kingdom.getMaxX()) / 2;
        int centerZ = (kingdom.getMinZ() + kingdom.getMaxZ()) / 2;
        double dist = Math.sqrt(Math.pow(loc.getBlockX() - centerX, 2) + Math.pow(loc.getBlockZ() - centerZ, 2));

        if (dist < minDist) {
            msg.send(p, "outpost.too_close", minDist);
            return;
        }

        // Hint de como criar: usar pá de ouro + /confirmar (mesmo fluxo de claim
        // normal)
        // Marcamos no PlayerData que o próximo claim será um outpost
        PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());
        if (pd != null) {
            pd.setNextClaimIsOutpost(true);
            pd.setOutpostParentKingdomId(kingdom.getId());
        }

        msg.send(p, "outpost.create_hint");
    }

    private void handleOutpostSetSpawn(Player p, Claim kingdom) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();
        Claim claimAt = plugin.getClaimManager().getClaimAt(p.getLocation());

        if (claimAt == null || !claimAt.isOutpost() || !kingdom.getId().equals(claimAt.getParentKingdomId())) {
            msg.send(p, "outpost.not_in_outpost");
            return;
        }

        km.setOutpostSpawn(kingdom.getId(), claimAt.getId(), p.getLocation());
        msg.send(p, "outpost.setspawn_success", claimAt.getName() != null ? claimAt.getName() : claimAt.getId());
    }

    private void handleOutpostSpawn(Player p, Claim kingdom, String[] args) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        List<Claim> outposts = plugin.getClaimManager().getOutpostsForKingdom(kingdom.getId());
        if (outposts.isEmpty()) {
            msg.send(p, "outpost.none");
            return;
        }

        // Se nome específico, procura; senão usa o primeiro
        Claim target = null;
        if (args.length >= 3) {
            String nameArg = args[2].toLowerCase();
            for (Claim o : outposts) {
                if (o.getId().equalsIgnoreCase(nameArg) ||
                        (o.getName() != null && o.getName().toLowerCase().contains(nameArg))) {
                    target = o;
                    break;
                }
            }
            if (target == null) {
                msg.send(p, "outpost.not_found");
                return;
            }
        } else {
            target = outposts.get(0);
        }

        Location spawn = km.getOutpostSpawn(kingdom.getId(), target.getId());
        if (spawn == null) {
            // Fallback: centro do outpost
            int cx = (target.getMinX() + target.getMaxX()) / 2;
            int cz = (target.getMinZ() + target.getMaxZ()) / 2;
            org.bukkit.World w = Bukkit.getWorld(target.getWorldName());
            if (w == null) {
                msg.send(p, "outpost.not_found");
                return;
            }
            spawn = new Location(w, cx + 0.5, w.getHighestBlockYAt(cx, cz) + 1, cz + 0.5);
        }

        p.teleport(spawn);
        String outpostName = target.getName() != null ? target.getName() : "Outpost";
        msg.send(p, "outpost.teleport_success", outpostName);
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    private void handleOutpostList(Player p, Claim kingdom) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();

        List<Claim> outposts = plugin.getClaimManager().getOutpostsForKingdom(kingdom.getId());
        int max = km.getMaxOutposts(kingdom.getId());

        msg.send(p, "outpost.list_header", outposts.size(), max);

        if (outposts.isEmpty()) {
            msg.send(p, "outpost.list_empty");
            return;
        }

        int i = 1;
        for (Claim o : outposts) {
            String name = o.getName() != null ? o.getName() : "Outpost";
            int area = (o.getMaxX() - o.getMinX() + 1) * (o.getMaxZ() - o.getMinZ() + 1);
            int cx = (o.getMinX() + o.getMaxX()) / 2;
            int cz = (o.getMinZ() + o.getMaxZ()) / 2;
            msg.send(p, "outpost.list_entry", i, name, area, cx, cz);
            i++;
        }
    }

    private void handleOutpostDelete(Player p, Claim kingdom) {
        var msg = plugin.getMessageManager();
        KingdomManager km = plugin.getKingdomManager();
        Claim claimAt = plugin.getClaimManager().getClaimAt(p.getLocation());

        if (claimAt == null || !claimAt.isOutpost() || !kingdom.getId().equals(claimAt.getParentKingdomId())) {
            msg.send(p, "outpost.not_in_outpost");
            return;
        }

        // Reembolsar blocos
        int area = (claimAt.getMaxX() - claimAt.getMinX() + 1) * (claimAt.getMaxZ() - claimAt.getMinZ() + 1);
        km.devolverBlocos(p.getUniqueId(), area);

        // Remover spawn
        km.removeOutpostSpawn(kingdom.getId(), claimAt.getId());

        // Remover claim
        plugin.getClaimManager().removeClaim(claimAt);

        String outpostName = claimAt.getName() != null ? claimAt.getName() : "Outpost";
        msg.send(p, "outpost.delete_success", outpostName);
    }

    /**
     * Tab completion para subcomandos de outpost.
     */
    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>(List.of("criar", "setspawn", "spawn", "listar", "deletar"));
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
