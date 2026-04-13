package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia o comando /reinonome (/cidadenome).
 * Também usado pelo KingdomCommand para /reino nome.
 * Extraído do antigo ClaimCommand no Batch B9.
 */
public class RenameCommand implements CommandExecutor, TabCompleter {

    private final GorvaxCore plugin;

    public RenameCommand(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (args.length < 1) {
            plugin.getMessageManager().send(p, "rename.usage");
            return true;
        }
        return handleKingdomRename(p, String.join(" ", args));
    }

    /**
     * Renomeia (ou funda) o reino do jogador.
     * Público para que KingdomCommand possa delegar /reino nome.
     */
    public boolean handleKingdomRename(Player p, String newName) {
        if (!newName.matches("[a-zA-Z0-9_]+")) {
            plugin.getMessageManager().send(p, "rename.invalid_chars");
            return true;
        }

        if (newName.length() < 3 || newName.length() > 16) {
            plugin.getMessageManager().send(p, "rename.invalid_length");
            return true;
        }

        if (plugin.getKingdomManager().tryFindKingdomIdByName(newName) != null) {
            plugin.getMessageManager().send(p, "rename.name_taken");
            return true;
        }

        // Localizar Reino: Priorizar onde o jogador está pisando
        Claim kingdom = plugin.getClaimManager().getClaimAt(p.getLocation());

        // Se não está no local ou não é dono do local, fallback para o reino do jogador
        if (kingdom == null || !kingdom.getOwner().equals(p.getUniqueId())) {
            kingdom = plugin.getKingdomManager().getKingdom(p.getUniqueId());
        }

        // Se ainda não achou reino/claim, o jogador não tem terras
        if (kingdom == null) {
            plugin.getMessageManager().send(p, "rename.no_territory");
            plugin.getMessageManager().send(p, "rename.no_territory_hint");
            return true;
        }

        // Verifica se é o dono do território
        if (!kingdom.getOwner().equals(p.getUniqueId())) {
            plugin.getMessageManager().send(p, "rename.not_owner");
            return true;
        }

        // Validação de Título (Precisa ter comprado o cargo de Rei/Prefeito)
        PlayerData pd = plugin.getPlayerDataManager().getData(p.getUniqueId());
        boolean hasKingRank = pd.hasKingRank();
        boolean hasPermission = p.hasPermission("gorvax.king");

        if (!hasKingRank && !hasPermission) {
            plugin.getMessageManager().send(p, "rename.needs_king_rank");
            plugin.getMessageManager().send(p, "rename.needs_king_rank_hint");
            return true;
        }

        String oldName = kingdom.getKingdomName();
        plugin.getKingdomManager().setNome(kingdom.getId(), newName);
        plugin.getKingdomManager().setRei(kingdom.getId(), p.getUniqueId());

        kingdom.setKingdomName(newName);
        // Default Configs on Creation
        if (oldName == null) {
            kingdom.setType(Claim.Type.REINO);
            kingdom.setPvp(false);
            kingdom.setResidentsPvp(false);
            kingdom.setTag(newName.substring(0, Math.min(3, newName.length())).toUpperCase());
        }

        plugin.getClaimManager().saveClaims();

        plugin.getMessageManager().send(p, "rename.success", newName);
        plugin.refreshPlayerName(p);
        plugin.getMessageManager().sendTitle(p, "rename.title", "rename.subtitle", 10, 80, 20, newName);
        plugin.getMessageManager().broadcast("rename.broadcast", newName);

        // Som de fundação/renomeação do reino
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // /reinonome não possui tab completion (nome é livre)
        return new ArrayList<>();
    }
}
