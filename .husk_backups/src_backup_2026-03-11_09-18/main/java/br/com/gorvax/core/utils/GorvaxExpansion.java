package br.com.gorvax.core.utils;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import br.com.gorvax.core.managers.LeaderboardManager;
import java.util.List;
import java.util.UUID;

public class GorvaxExpansion extends PlaceholderExpansion {

    private final GorvaxCore plugin;

    public GorvaxExpansion(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gorvax";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GorvaxTeam";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.2";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null)
            return "";

        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());

        // --- PLACEHOLDERS DE STATUS E LOCALIZAÇÃO ---
        if (identifier.equals("localizacao_label")) {
            if (claim == null)
                return "§cTerras Barbaras";

            String kingdomName = plugin.getKingdomManager().getNome(claim.getId());
            if (kingdomName != null) {
                boolean isSudito = plugin.getKingdomManager().isSudito(claim.getId(), player.getUniqueId());
                return (isSudito ? "§a" : "§f") + kingdomName + (isSudito ? " §8(§b✔§8)" : "");
            }
            return "§fTerras de: §7" + plugin.getPlayerName(claim.getOwner());
        }

        if (identifier.equals("status_protecao")) {
            return (claim != null) ? "§a§lPROTEGIDO" : "§c§lDESPROTEGIDO";
        }

        if (identifier.equals("localizacao_tab")) {
            if (claim == null)
                return "§7Mundo Aberto";

            String kingdomName = plugin.getKingdomManager().getNome(claim.getId());
            if (kingdomName != null)
                return "§b" + kingdomName + " §8| §eSede Real";

            return "§6Casa de " + plugin.getPlayerName(claim.getOwner());
        }

        // --- PLACEHOLDERS DE TERRENO (BLOCOS) ---
        if (pd != null) {
            if (identifier.equals("blocos_disponiveis"))
                return String.valueOf(pd.getClaimBlocks());

            if (identifier.equals("blocos_usados")) {
                int usados = plugin.getClaimManager().getClaims().stream()
                        .filter(c -> c.getOwner().equals(player.getUniqueId()))
                        .mapToInt(Claim::getArea)
                        .sum();
                return String.valueOf(usados);
            }

            if (identifier.equals("blocos_total")) {
                int usados = plugin.getClaimManager().getClaims().stream()
                        .filter(c -> c.getOwner().equals(player.getUniqueId()))
                        .mapToInt(Claim::getArea)
                        .sum();
                return String.valueOf(pd.getClaimBlocks() + usados);
            }

            if (identifier.equals("territorio_desc")) {
                return "§fDisp: §a" + pd.getClaimBlocks();
            }
        }

        // --- PLACEHOLDERS DE REINO ---
        if (identifier.equals("reino_nome") || identifier.equals("cidade_nome")) {
            if (claim == null)
                return "§7Terras Bárbaras";
            String name = plugin.getKingdomManager().getNome(claim.getId());
            return (name != null) ? name : "§7Terras Bárbaras";
        }

        if (identifier.equals("reino_rei") || identifier.equals("cidade_prefeito")) {
            if (claim == null)
                return "Ninguém";
            UUID king = plugin.getKingdomManager().getRei(claim.getId());
            return (king != null) ? plugin.getPlayerName(king) : plugin.getPlayerName(claim.getOwner());
        }

        if (identifier.equals("reino_suditos") || identifier.equals("cidade_moradores")) {
            if (claim == null)
                return "0";
            return String.valueOf(plugin.getKingdomManager().getSuditosCount(claim.getId()));
        }

        if (identifier.equals("reino_rank") || identifier.equals("cidade_rank")) {
            if (claim == null)
                return "§7Sem Rank";
            return plugin.getKingdomManager().getKingdomRank(claim.getId());
        }

        if (identifier.equals("reino_tag")) {
            if (claim == null)
                return "";
            return (claim.getTag() != null) ? claim.getTag() : "";
        }

        if (identifier.equals("reino_tag_color")) {
            if (claim == null)
                return "§f";
            return (claim.getTagColor() != null) ? claim.getTagColor() : "§f";
        }

        // --- PLACEHOLDERS DE BOSS ---
        if (identifier.equals("next_boss")) {
            long nextSpawn = plugin.getBossManager().getNextSpawnTime();
            long now = System.currentTimeMillis();
            if (nextSpawn <= now)
                return "§aA qualquer momento";
            long diff = nextSpawn - now;
            long minutes = (diff / 1000) / 60;
            long seconds = (diff / 1000) % 60;
            return String.format("%02dm %02ds", minutes, seconds);
        }

        // --- PLACEHOLDERS DE ESTATÍSTICAS (B3) ---
        if (pd != null) {
            if (identifier.equals("playtime")) {
                long ms = pd.getTotalPlayTime();
                long totalSecs = ms / 1000;
                long days = totalSecs / 86400;
                long hours = (totalSecs % 86400) / 3600;
                long mins = (totalSecs % 3600) / 60;
                if (days > 0)
                    return days + "d " + hours + "h";
                if (hours > 0)
                    return hours + "h " + mins + "m";
                return mins + "m";
            }
            if (identifier.equals("kills"))
                return String.valueOf(pd.getTotalKills());
            if (identifier.equals("deaths"))
                return String.valueOf(pd.getTotalDeaths());
            if (identifier.equals("kdr")) {
                int deaths = pd.getTotalDeaths();
                if (deaths == 0)
                    return String.format("%.2f", (double) pd.getTotalKills());
                return String.format("%.2f", (double) pd.getTotalKills() / deaths);
            }
            if (identifier.equals("bosses_killed"))
                return String.valueOf(pd.getBossesKilled());
            if (identifier.equals("blocks_broken"))
                return String.valueOf(pd.getTotalBlocksBroken());
            if (identifier.equals("blocks_placed"))
                return String.valueOf(pd.getTotalBlocksPlaced());
            if (identifier.equals("title")) {
                String title = pd.getActiveTitle();
                return (title != null && !title.isEmpty()) ? title : "";
            }
            if (identifier.equals("money_earned"))
                return String.format("%.2f", pd.getTotalMoneyEarned());
            if (identifier.equals("money_spent"))
                return String.format("%.2f", pd.getTotalMoneySpent());

            // B12 — Conquistas
            if (identifier.equals("achievements")) {
                if (plugin.getAchievementManager() != null) {
                    return plugin.getAchievementManager().getUnlockedCount(player.getUniqueId())
                            + "/" + plugin.getAchievementManager().getTotalCount();
                }
                return "0/0";
            }
        }

        // --- PLACEHOLDERS B17: FEATURES SOCIAIS ---
        if (identifier.equals("bounty")) {
            if (plugin.getBountyManager() != null) {
                double value = plugin.getBountyManager().getBountyValue(player.getUniqueId());
                return value > 0 ? String.format("%.2f", value) : "0";
            }
            return "0";
        }

        if (identifier.equals("mail_unread")) {
            if (plugin.getMailManager() != null) {
                return String.valueOf(plugin.getMailManager().getUnreadCount(player.getUniqueId()));
            }
            return "0";
        }

        // --- PLACEHOLDERS B19: NAÇÕES ---
        if (identifier.startsWith("nation")) {
            var nm = plugin.getNationManager();
            Claim kingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
            if (nm != null && kingdom != null) {
                var nation = nm.getNationByKingdom(kingdom.getId());
                if (identifier.equals("nation")) {
                    return nation != null ? nation.getName() : "Nenhuma";
                }
                if (identifier.equals("nation_level")) {
                    return nation != null ? String.valueOf(nm.getNationLevel(nation)) : "0";
                }
                if (identifier.equals("nation_kingdoms")) {
                    return nation != null ? String.valueOf(nation.getKingdomCount()) : "0";
                }
                if (identifier.equals("nation_bank")) {
                    return nation != null ? String.format("%.2f", nation.getBankBalance()) : "0";
                }
            }
            return "---";
        }

        // --- PLACEHOLDERS B7: LEADERBOARD ---
        // Formato: %gorvax_top_<cat>_<pos>_<name|value>%
        if (identifier.startsWith("top_")) {
            LeaderboardManager lm = plugin.getLeaderboardManager();
            if (lm == null)
                return "---";

            // Remover prefixo "top_"
            String rest = identifier.substring(4);

            // Encontrar a última parte (_name ou _value)
            int lastUnderscore = rest.lastIndexOf('_');
            if (lastUnderscore <= 0)
                return "---";
            String type = rest.substring(lastUnderscore + 1); // name ou value
            rest = rest.substring(0, lastUnderscore);

            // Encontrar a posição (último número)
            int posUnderscore = rest.lastIndexOf('_');
            if (posUnderscore < 0)
                return "---";
            String posStr = rest.substring(posUnderscore + 1);
            String category = rest.substring(0, posUnderscore);

            int pos;
            try {
                pos = Integer.parseInt(posStr);
            } catch (NumberFormatException e) {
                return "---";
            }

            if (pos < 1 || !lm.isValidCategory(category))
                return "---";

            List<LeaderboardManager.LeaderboardEntry> entries = lm.getTop(category);
            if (pos > entries.size())
                return "---";

            LeaderboardManager.LeaderboardEntry entry = entries.get(pos - 1);
            if (type.equals("name")) {
                return entry.name();
            } else if (type.equals("value")) {
                return formatLeaderboardValue(category, entry.value());
            }
            return "---";
        }

        // --- PLACEHOLDERS B14-VIP: RANKS VIP ---
        if (identifier.startsWith("vip_")) {
            var vm = plugin.getVipManager();
            if (vm != null && vm.isEnabled()) {
                var tier = vm.getVipTier(player);
                if (identifier.equals("vip_tier")) {
                    return tier.getLabel();
                }
                if (identifier.equals("vip_display")) {
                    return tier.getDisplayName();
                }
                if (identifier.equals("vip_blocks")) {
                    return String.valueOf(vm.getExtraClaimBlocks(tier));
                }
            }
            return "---";
        }

        // --- PLACEHOLDERS B15: BATTLE PASS ---
        if (identifier.startsWith("bp_")) {
            var bpm = plugin.getBattlePassManager();
            if (bpm != null && bpm.isEnabled() && pd != null) {
                if (identifier.equals("bp_level")) {
                    return String.valueOf(pd.getBattlePassLevel());
                }
                if (identifier.equals("bp_xp")) {
                    return String.valueOf(pd.getBattlePassXp());
                }
                if (identifier.equals("bp_season")) {
                    return String.valueOf(bpm.getSeasonNumber());
                }
                if (identifier.equals("bp_season_name")) {
                    return bpm.getSeasonName();
                }
                if (identifier.equals("bp_premium")) {
                    return pd.isBattlePassPremium() ? "Sim" : "Não";
                }
                if (identifier.equals("bp_days")) {
                    return String.valueOf(bpm.getDaysRemaining());
                }
            }
            return "---";
        }

        // --- PLACEHOLDERS B17: EVENTOS SAZONAIS ---
        if (identifier.startsWith("event_")) {
            var sem = plugin.getSeasonalEventManager();
            if (sem != null) {
                if (identifier.equals("event_active")) {
                    return sem.isEventActive() ? sem.getActiveEvent().name() : "Nenhum";
                }
                if (identifier.equals("event_days_remaining")) {
                    return String.valueOf(sem.getDaysRemaining());
                }
                if (identifier.equals("event_xp_multiplier")) {
                    return String.format("%.1fx", sem.getXpMultiplier());
                }
            }
            return "---";
        }

        // --- PLACEHOLDERS B18: KARMA / REPUTAÇÃO ---
        if (identifier.startsWith("karma")) {
            var rm = plugin.getReputationManager();
            if (rm != null && pd != null) {
                if (identifier.equals("karma")) {
                    return String.valueOf(pd.getKarma());
                }
                if (identifier.equals("karma_rank")) {
                    return rm.getKarmaColor(pd.getKarma()) + rm.getKarmaLabel(pd.getKarma());
                }
                if (identifier.equals("karma_label")) {
                    return rm.getKarmaLabel(pd.getKarma());
                }
            }
            return "---";
        }
        // --- PLACEHOLDERS B28: CÓDEX DE GORVAX ---
        if (identifier.startsWith("codex_")) {
            var cm = plugin.getCodexManager();
            if (cm != null && pd != null) {
                if (identifier.equals("codex_unlocked")) {
                    return String.valueOf(cm.getProgress(player.getUniqueId())[0]);
                }
                if (identifier.equals("codex_total")) {
                    return String.valueOf(cm.getTotalEntries());
                }
                if (identifier.equals("codex_percent")) {
                    int[] prog = cm.getProgress(player.getUniqueId());
                    return prog[1] > 0 ? String.valueOf(prog[0] * 100 / prog[1]) : "0";
                }
                // %gorvax_codex_category_<catId>% → "X/Y"
                if (identifier.startsWith("codex_category_")) {
                    String catId = identifier.substring("codex_category_".length());
                    int[] catProg = cm.getCategoryProgress(player.getUniqueId(), catId);
                    return catProg[0] + "/" + catProg[1];
                }
            }
            return "---";
        }

        return "---";
    }

    /**
     * Formata valores do leaderboard para placeholders PAPI.
     */
    private String formatLeaderboardValue(String category, double value) {
        return switch (category) {
            case "kdr" -> String.format("%.2f", value);
            case "riqueza" -> String.format("%.0f", value);
            case "playtime" -> {
                long totalSeconds = (long) value / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                yield hours + "h " + minutes + "m";
            }
            case "reinos" -> String.format("%.0f", value);
            default -> String.format("%.0f", value);
        };
    }
}