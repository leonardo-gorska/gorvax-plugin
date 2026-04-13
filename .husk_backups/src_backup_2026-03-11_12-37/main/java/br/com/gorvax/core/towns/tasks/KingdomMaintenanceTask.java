package br.com.gorvax.core.towns.tasks;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.SubPlot;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class KingdomMaintenanceTask extends BukkitRunnable {

    private final GorvaxCore plugin;

    // Configurações de tempo
    private final long SUDITO_INATIVO_MS = TimeUnit.DAYS.toMillis(7); // 7 dias
    private final long REI_INATIVO_MS = TimeUnit.DAYS.toMillis(15); // 15 dias
    private final long REINO_ABANDONADO_MS = TimeUnit.DAYS.toMillis(30); // 30 dias (deletar reino vazio)

    public KingdomMaintenanceTask(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        FileConfiguration config = plugin.getKingdomManager().getData();
        if (config.getConfigurationSection("reino") == null)
            return;

        plugin.getLogger().info("------------------------------------------");
        plugin.getLogger().info("[GorvaxKingdoms] Iniciando varredura de inativos e sucessão...");

        long agora = System.currentTimeMillis();

        boolean anyTownChanged = false;
        for (String key : config.getConfigurationSection("reino").getKeys(false)) {
            try {
                String path = "reino." + key;

                // 1. VERIFICAR INATIVIDADE GERAL DO REINO (Proteção de dados órfãos)
                long ultimoLoginReino = config.getLong(path + ".ultimo_login", 0);
                if (ultimoLoginReino != 0 && (agora - ultimoLoginReino > REINO_ABANDONADO_MS)) {
                    plugin.getLogger().warning("Reino " + key + " abandonado há mais de 30 dias. Deletando...");
                    plugin.getKingdomManager().deleteKingdom(key, false);
                    anyTownChanged = true;
                    continue;
                }

                // 2. VERIFICAR INATIVIDADE DO REI
                String reiUUIDStr = config.getString(path + ".rei");
                if (reiUUIDStr != null) {
                    long loginRei = config.getLong(path + ".ultimo_login_rei", 0);

                    if (loginRei != 0 && (agora - loginRei > REI_INATIVO_MS)) {
                        plugin.getLogger()
                                .warning("Rei inativo detectado no reino " + key + ". Iniciando sucessão...");
                        realizarSucessao(key, UUID.fromString(reiUUIDStr));
                        anyTownChanged = true;
                        continue; // Próxima cidade, pois esta já foi alterada
                    }
                }

                // 3. VERIFICAR INATIVIDADE DOS SÚDITOS
                List<String> suditos = plugin.getKingdomManager().getSuditosList(key);
                List<UUID> paraRemover = new ArrayList<>();

                for (String uuidStr : suditos) {
                    // Ignora o Rei na lista
                    if (uuidStr.equals(reiUUIDStr))
                        continue;

                    long loginSudito = config.getLong(path + ".suditos_atividade." + uuidStr, 0);

                    // Se o súdito não loga há 7 dias
                    if (loginSudito != 0 && (agora - loginSudito > SUDITO_INATIVO_MS)) {
                        paraRemover.add(UUID.fromString(uuidStr));
                    }
                }

                for (UUID suditoUUID : paraRemover) {
                    plugin.getKingdomManager().removeSudito(key, suditoUUID, false);
                    anyTownChanged = true;
                    plugin.getLogger()
                            .info("Súdito " + suditoUUID + " removido do reino " + key + " por inatividade.");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Erro na manutenção do reino " + key + ": " + e.getMessage());
            }
        }

        // 4. COBRANÇA DE ALUGUÉIS (FEUDOS/LOTES)
        plugin.getLogger().info("[GorvaxKingdoms] Verificando arrendamentos de feudos...");
        int alugueisProcessados = 0;
        boolean anyClaimChanged = false;

        for (Claim claim : plugin.getClaimManager().getClaims()) {
            if (!claim.isKingdom())
                continue;

            boolean claimChanged = false;
            UUID ownerUUID = claim.getOwner();
            if (ownerUUID == null)
                continue;

            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);

            for (SubPlot plot : claim.getSubPlots()) {
                if (plot.isForRent() && plot.getRenter() != null) {
                    if (agora > plot.getRentExpire()) {
                        UUID renterUUID = plot.getRenter();
                        OfflinePlayer renter = Bukkit.getOfflinePlayer(renterUUID);
                        double price = plot.getRentPrice();

                        if (GorvaxCore.getEconomy() != null) {
                            if (GorvaxCore.getEconomy().has(renter, price)) {
                                GorvaxCore.getEconomy().withdrawPlayer(renter, price);
                                GorvaxCore.getEconomy().depositPlayer(owner, price);

                                plot.setRentExpire(agora + TimeUnit.DAYS.toMillis(1));
                                claimChanged = true;
                                alugueisProcessados++;

                                if (renter.isOnline()) {
                                    renter.getPlayer().sendMessage(
                                            plugin.getMessageManager().get("maintenance.rent_paid", price, plot.getName()));
                                }
                            } else {
                                plot.setRenter(null);
                                plot.setRentExpire(0);
                                claimChanged = true;

                                plugin.getKingdomManager().removeSudito(claim.getId(), renterUUID, false);
                                anyTownChanged = true;

                                if (renter.isOnline()) {
                                    renter.getPlayer().sendMessage(plugin.getMessageManager().get(
                                            "maintenance.eviction", plot.getName()));
                                    renter.getPlayer().sendMessage(
                                            plugin.getMessageManager().get("maintenance.eviction_warning"));
                                }
                            }
                        }
                    }
                }
            }
            if (claimChanged) {
                anyClaimChanged = true;
            }
        }

        if (anyClaimChanged) {
            plugin.getClaimManager().saveClaims(); // Salva tudo de uma vez ao final (Diagnóstico item 1)
        }

        if (anyTownChanged) {
            plugin.getKingdomManager().saveData(); // Salva tudo de uma vez ao final (Otimização)
        }

        plugin.getLogger().info("[GorvaxKingdoms] Processados " + alugueisProcessados + " pagamentos de arrendamento.");

        // --- B5: SISTEMA DE IMPOSTOS E UPKEEP DE REINOS ---
        boolean kingdomUpkeepEnabled = plugin.getConfig().getBoolean("kingdoms.upkeep_enabled", false);
        if (kingdomUpkeepEnabled) {
            collectResidentTaxes(config);
            collectKingdomUpkeep(config);
        }

        // --- B36: UPKEEP DE CLAIMS PESSOAIS ---
        boolean claimUpkeepEnabled = plugin.getConfig().getBoolean("claims.upkeep_enabled", false);
        if (claimUpkeepEnabled) {
            collectClaimUpkeep();
        }

        plugin.getLogger().info("------------------------------------------");
    }

    // --- B5.1: COBRANÇA DE IMPOSTO DIÁRIO SOBRE SÚDITOS ---
    private void collectResidentTaxes(FileConfiguration config) {
        if (config.getConfigurationSection("reino") == null) return;

        int totalCollected = 0;
        int totalExpelled = 0;
        int debtDaysExpel = plugin.getConfig().getInt("kingdoms.tax.debt_days_expel", 3);

        for (String kingdomId : config.getConfigurationSection("reino").getKeys(false)) {
            try {
                double taxRate = plugin.getKingdomManager().getResidentTax(kingdomId);
                if (taxRate <= 0) continue;

                List<String> suditos = plugin.getKingdomManager().getSuditosList(kingdomId);
                String reiStr = config.getString("reino." + kingdomId + ".rei");
                List<UUID> toExpel = new ArrayList<>();

                for (String uuidStr : suditos) {
                    if (uuidStr.equals(reiStr)) continue; // Rei não paga imposto

                    UUID suditoUUID;
                    try {
                        suditoUUID = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    OfflinePlayer sudito = Bukkit.getOfflinePlayer(suditoUUID);

                    if (GorvaxCore.getEconomy() != null && GorvaxCore.getEconomy().has(sudito, taxRate)) {
                        GorvaxCore.getEconomy().withdrawPlayer(sudito, taxRate);
                        plugin.getKingdomManager().depositToBank(kingdomId, taxRate);
                        plugin.getKingdomManager().clearTaxDebt(kingdomId, suditoUUID);
                        totalCollected++;

                        if (sudito.isOnline()) {
                            sudito.getPlayer().sendMessage(
                                    plugin.getMessageManager().get("maintenance.tax_paid", String.format("%.2f", taxRate)));
                        }
                    } else {
                        // Sem saldo — incrementar dívida
                        int debtDays = plugin.getKingdomManager().getTaxDebtDays(kingdomId, suditoUUID) + 1;
                        plugin.getKingdomManager().setTaxDebtDays(kingdomId, suditoUUID, debtDays);

                        if (debtDays >= debtDaysExpel) {
                            toExpel.add(suditoUUID);
                        } else if (sudito.isOnline()) {
                            sudito.getPlayer().sendMessage(
                                    plugin.getMessageManager().get("maintenance.tax_warning", debtDays, debtDaysExpel));
                        }
                    }
                }

                // Expulsar devedores
                String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
                for (UUID expelled : toExpel) {
                    plugin.getKingdomManager().removeSudito(kingdomId, expelled, false);
                    plugin.getKingdomManager().clearTaxDebt(kingdomId, expelled);
                    totalExpelled++;

                    OfflinePlayer op = Bukkit.getOfflinePlayer(expelled);
                    if (op.isOnline()) {
                        op.getPlayer().sendMessage(
                                plugin.getMessageManager().get("maintenance.tax_expelled", nomeReino != null ? nomeReino : kingdomId));
                    }
                    plugin.getLogger().info("[B5] Súdito " + expelled + " expulso do reino " + kingdomId + " por dívida fiscal.");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("[B5] Erro na cobrança de impostos do reino " + kingdomId + ": " + e.getMessage());
            }
        }

        if (totalExpelled > 0) {
            plugin.getKingdomManager().saveData();
        }

        plugin.getLogger().info("[B5] Impostos: " + totalCollected + " cobranças, " + totalExpelled + " expulsões.");
    }

    // --- B5.2: COBRANÇA DE UPKEEP DO REINO ---
    private void collectKingdomUpkeep(FileConfiguration config) {
        if (config.getConfigurationSection("reino") == null) return;

        double perChunk = plugin.getConfig().getDouble("kingdoms.upkeep.per_chunk", 10.0);
        int warningDays = plugin.getConfig().getInt("kingdoms.upkeep.warning_days", 7);
        int unclaimDays = plugin.getConfig().getInt("kingdoms.upkeep.unclaim_days", 14);

        for (String kingdomId : config.getConfigurationSection("reino").getKeys(false)) {
            try {
                Claim claim = plugin.getClaimManager().getClaimById(kingdomId);
                if (claim == null || !claim.isKingdom()) continue;

                // Calcular custo de upkeep
                int width = claim.getMaxX() - claim.getMinX() + 1;
                int length = claim.getMaxZ() - claim.getMinZ() + 1;
                int chunks = Math.max(1, (width * length) / (16 * 16));
                double upkeepCost = perChunk * chunks;

                String nomeReino = plugin.getKingdomManager().getNome(kingdomId);

                if (plugin.getKingdomManager().withdrawFromBank(kingdomId, upkeepCost)) {
                    // Pagamento OK — resetar dívida
                    plugin.getKingdomManager().setUpkeepDebtDays(kingdomId, 0);
                    plugin.getLogger().info("[B5] Upkeep cobrado: $" + upkeepCost + " do reino " + kingdomId);
                } else {
                    // Sem saldo — incrementar dívida
                    int debtDays = plugin.getKingdomManager().getUpkeepDebtDays(kingdomId) + 1;
                    plugin.getKingdomManager().setUpkeepDebtDays(kingdomId, debtDays);

                    UUID reiUUID = plugin.getKingdomManager().getRei(kingdomId);

                    if (debtDays >= unclaimDays) {
                        // 14+ dias: liberar claim mais antigo (o próprio reino, por segurança deletamos o reino)
                        plugin.getLogger().warning("[B5] Reino " + kingdomId + " com " + debtDays + " dias de dívida. Dissolvendo...");
                        plugin.getKingdomManager().deleteKingdom(kingdomId, false);
                        plugin.getMessageManager().broadcast("maintenance.upkeep_dissolved",
                                nomeReino != null ? nomeReino : kingdomId);
                    } else if (debtDays >= warningDays) {
                        // 7+ dias: Decadência (buffs desativados via KingdomEffectsTask)
                        plugin.getLogger().warning("[B5] Reino " + kingdomId + " em decadência (" + debtDays + " dias de dívida).");
                        if (reiUUID != null) {
                            OfflinePlayer rei = Bukkit.getOfflinePlayer(reiUUID);
                            if (rei.isOnline()) {
                                rei.getPlayer().sendMessage(
                                        plugin.getMessageManager().get("maintenance.upkeep_decaying", debtDays));
                            }
                        }
                    } else {
                        // Aviso simples
                        if (reiUUID != null) {
                            OfflinePlayer rei = Bukkit.getOfflinePlayer(reiUUID);
                            if (rei.isOnline()) {
                                rei.getPlayer().sendMessage(
                                        plugin.getMessageManager().get("maintenance.upkeep_warning",
                                                String.format("%.2f", upkeepCost), debtDays, warningDays));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[B5] Erro no upkeep do reino " + kingdomId + ": " + e.getMessage());
            }
        }

        plugin.getKingdomManager().saveData();
    }

    // --- B36: COBRANÇA DE UPKEEP DE CLAIMS PESSOAIS ---
    private void collectClaimUpkeep() {
        double perChunk = plugin.getConfig().getDouble("claims.upkeep.per_chunk", 5.0);
        int warningDays = plugin.getConfig().getInt("claims.upkeep.warning_days", 10);
        int unclaimDays = plugin.getConfig().getInt("claims.upkeep.unclaim_days", 21);

        int totalPaid = 0;
        int totalWarned = 0;
        int totalRemoved = 0;
        boolean anyChanged = false;

        // Coletar snapshot para evitar ConcurrentModificationException ao remover
        List<Claim> snapshot = new ArrayList<>(plugin.getClaimManager().getClaims());

        for (Claim claim : snapshot) {
            // Pular reinos, outposts e claims sem dono
            if (claim.isKingdom() || claim.isOutpost()) continue;
            UUID ownerUUID = claim.getOwner();
            if (ownerUUID == null) continue;

            try {
                // Calcular custo de upkeep
                int width = claim.getMaxX() - claim.getMinX() + 1;
                int length = claim.getMaxZ() - claim.getMinZ() + 1;
                int chunks = Math.max(1, (width * length) / (16 * 16));
                double upkeepCost = perChunk * chunks;

                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);

                if (GorvaxCore.getEconomy() != null && GorvaxCore.getEconomy().has(owner, upkeepCost)) {
                    // Pagamento OK
                    GorvaxCore.getEconomy().withdrawPlayer(owner, upkeepCost);
                    claim.setUpkeepDebtDays(0);
                    anyChanged = true;
                    totalPaid++;

                    if (owner.isOnline()) {
                        owner.getPlayer().sendMessage(
                                plugin.getMessageManager().get("maintenance.claim_upkeep_paid",
                                        String.format("%.2f", upkeepCost)));
                    }
                } else {
                    // Sem saldo — incrementar dívida
                    int debtDays = claim.getUpkeepDebtDays() + 1;
                    claim.setUpkeepDebtDays(debtDays);
                    anyChanged = true;

                    if (debtDays >= unclaimDays) {
                        // Remover claim por falta de pagamento
                        String worldName = claim.getWorldName();
                        plugin.getLogger().warning("[B36] Claim " + claim.getId() + " de "
                                + plugin.getClaimManager().getOwnerName(ownerUUID)
                                + " removido por " + debtDays + " dias de dívida.");
                        plugin.getClaimManager().removeClaim(claim);
                        totalRemoved++;

                        if (owner.isOnline()) {
                            owner.getPlayer().sendMessage(
                                    plugin.getMessageManager().get("maintenance.claim_upkeep_removed", worldName));
                        }
                    } else if (debtDays >= warningDays) {
                        // Aviso de expiração iminente
                        int daysLeft = unclaimDays - debtDays;
                        totalWarned++;

                        if (owner.isOnline()) {
                            owner.getPlayer().sendMessage(
                                    plugin.getMessageManager().get("maintenance.claim_upkeep_expiring", daysLeft));
                        }
                    } else {
                        // Aviso simples de dívida
                        totalWarned++;

                        if (owner.isOnline()) {
                            owner.getPlayer().sendMessage(
                                    plugin.getMessageManager().get("maintenance.claim_upkeep_warning",
                                            String.format("%.2f", upkeepCost), debtDays, warningDays));
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[B36] Erro no upkeep do claim " + claim.getId() + ": " + e.getMessage());
            }
        }

        if (anyChanged) {
            plugin.getClaimManager().saveClaims();
        }

        plugin.getLogger().info("[B36] Claims pessoais: " + totalPaid + " pagos, " + totalWarned
                + " avisos, " + totalRemoved + " removidos.");
    }

    private void realizarSucessao(String kingdomId, UUID reiAntigo) {
        List<String> suditos = plugin.getKingdomManager().getSuditosList(kingdomId);
        FileConfiguration config = plugin.getKingdomManager().getData();
        String path = "reino." + kingdomId;

        // Tenta achar o súdito mais ativo para ser o novo Rei
        UUID sucessorUUID = null;
        long maiorAtividade = 0;

        for (String mUuid : suditos) {
            if (mUuid.equals(reiAntigo.toString()))
                continue;

            long atividade = config.getLong(path + ".suditos_atividade." + mUuid, 0);
            if (atividade > maiorAtividade) {
                maiorAtividade = atividade;
                sucessorUUID = UUID.fromString(mUuid);
            }
        }

        if (sucessorUUID == null) {
            // Caso 1: Não há ninguém para assumir -> Reino é deletado
            plugin.getLogger()
                    .warning("Reino " + kingdomId + " não possui súditos ativos para sucessão. Deletando...");
            plugin.getKingdomManager().deleteKingdom(kingdomId, false);
        } else {
            // Caso 2: O súdito mais ativo assume
            String novoReiNome = plugin.getPlayerName(sucessorUUID);
            String antigoReiNome = plugin.getPlayerName(reiAntigo);

            // Atualiza o Rei no Manager
            plugin.getKingdomManager().setRei(kingdomId, sucessorUUID);

            // Remove o Rei antigo da lista de súditos (Rei != Súdito na lista, mas ok,
            // lógica de remover prev)
            plugin.getKingdomManager().removeSudito(kingdomId, reiAntigo);

            // Integração com LuckPerms (Via Console)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + antigoReiNome + " parent remove rei");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + novoReiNome + " parent add rei");

            plugin.getLogger()
                    .info("SUCESSÃO: " + novoReiNome + " assumiu a coroa do reino " + kingdomId);

            // Notificação global
            String nomeReino = plugin.getKingdomManager().getNome(kingdomId);
            plugin.getMessageManager().broadcast("maintenance.succession_absent", nomeReino);
            plugin.getMessageManager().broadcast("maintenance.succession_new_king", novoReiNome);
        }
    }
}