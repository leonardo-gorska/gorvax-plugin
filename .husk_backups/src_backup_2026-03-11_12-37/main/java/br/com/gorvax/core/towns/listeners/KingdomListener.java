package br.com.gorvax.core.towns.listeners;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.managers.PlayerData;
import br.com.gorvax.core.managers.SubPlot;
import br.com.gorvax.core.towns.Relation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KingdomListener implements Listener {

    private final GorvaxCore plugin;
    private final Map<UUID, String> lastClaimId = new ConcurrentHashMap<>();

    public KingdomListener(GorvaxCore plugin) {
        this.plugin = plugin;
    }

    // --- SISTEMA DE LOGIN E ATIVIDADE ---

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        long agora = System.currentTimeMillis();

        // O método registrarAtividade no KingdomManager já cuida de atualizar
        // tanto o tempo do Rei quanto dos Súditos de forma otimizada.
        plugin.getKingdomManager().registrarAtividade(uuid, agora);
        plugin.refreshPlayerName(p);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();

        // Limpeza de cache para evitar vazamento de memória (Diagnóstico item 3)
        plugin.getPlayerDataManager().unloadData(uuid);
        // Nota: Loot de boss NÃO é removido aqui. Expira automaticamente com o baú (5
        // min).
        lastClaimId.remove(uuid);
    }

    // --- GESTÃO DE CLAIMS E REINOS (UI) ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null)
            return; // Proteção contra NPE

        // Ignora movimentos apenas de olhar/cabeça
        if (event.getFrom().getBlockX() == to.getBlockX() &&
                event.getFrom().getBlockZ() == to.getBlockZ())
            return;

        Player player = event.getPlayer();
        // Usa 'to' (destino) em vez de player.getLocation() para evitar lag de 1 tick
        Claim claim = plugin.getClaimManager().getClaimAt(to);

        String currentId = (claim != null) ? claim.getId() : null;
        String previousId = lastClaimId.getOrDefault(player.getUniqueId(), null);

        // --- LÓGICA DE TÍTULOS (ENTRADA E SAÍDA) ---
        if (currentId != null && !currentId.equals(previousId)) {
            // B19 — Evento customizado: ClaimEnterEvent (cancelável)
            br.com.gorvax.core.events.ClaimEnterEvent enterEvent = new br.com.gorvax.core.events.ClaimEnterEvent(player,
                    claim);
            org.bukkit.Bukkit.getPluginManager().callEvent(enterEvent);
            if (enterEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            lastClaimId.put(player.getUniqueId(), currentId);

            String title = "";
            String subtitle = "";

            if (plugin.getKingdomManager().getNome(currentId) != null) {
                // REINO OFICIAL
                String nome = plugin.getKingdomManager().getNome(currentId);
                String rank = plugin.getKingdomManager().getKingdomRank(currentId);

                // Custom Messages Logic
                if (claim.getEnterTitle() != null && !claim.getEnterTitle().isEmpty()) {
                    title = claim.getEnterTitle().replace("&", "§"); // Support color codes
                    // Se não tiver cor definida na mensagem, aplica a WelcomeColor
                    if (!title.contains("§"))
                        title = claim.getWelcomeColor() + title;
                } else {
                    title = claim.getWelcomeColor() + "§l" + rank + " " + nome;
                }

                if (claim.getEnterSubtitle() != null && !claim.getEnterSubtitle().isEmpty()) {
                    subtitle = claim.getEnterSubtitle().replace("&", "§");
                } else {
                    int nivelXp = plugin.getKingdomManager().getNivel(currentId, "xp");
                    double bonusPassivo = plugin.getKingdomManager().getPassiveXpBonus(currentId) * 100;
                    int totalBonus = (int) (20 + (nivelXp * 5) + bonusPassivo);
                    subtitle = "§fBônus de XP: §a+" + totalBonus + "%";
                }

                player.showTitle(Title.title(
                        LegacyComponentSerializer.legacySection().deserialize(title),
                        LegacyComponentSerializer.legacySection().deserialize(subtitle),
                        Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000),
                                java.time.Duration.ofMillis(500))));

                // B9 — Som ao entrar em claim
                PlayerData pd = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (pd.isBorderSound()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                }

                // B7 — Aviso de território inimigo/aliado
                Claim playerKingdom = plugin.getKingdomManager().getKingdom(player.getUniqueId());
                if (playerKingdom != null && !playerKingdom.getId().equals(currentId)) {
                    Relation rel = plugin.getKingdomManager()
                            .getRelation(playerKingdom.getId(), currentId);
                    if (rel == Relation.ENEMY) {
                        plugin.getMessageManager().send(player, "diplomacy.enter_enemy_territory", nome);
                    } else if (rel == Relation.ALLY) {
                        plugin.getMessageManager().send(player, "diplomacy.enter_ally_territory", nome);
                    }
                }
            } else {
                // Claim Privado
                String ownerName = plugin.getClaimManager().getOwnerName(claim.getOwner());
                player.showTitle(Title.title(
                        LegacyComponentSerializer.legacySection().deserialize(
                                plugin.getMessageManager().get("kingdom.enter_private_title")),
                        LegacyComponentSerializer.legacySection().deserialize(
                                plugin.getMessageManager().get("kingdom.enter_private_subtitle", ownerName)),
                        Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000),
                                java.time.Duration.ofMillis(500))));
            }
        } else if (currentId == null && previousId != null) {
            // SAINDO DO REINO (Mundo Aberto)
            Claim prevClaim = plugin.getClaimManager().getClaimById(previousId);

            String exitTitle = plugin.getMessageManager().get("kingdom.exit_wild_title");
            String exitSubtitle = plugin.getMessageManager().get("kingdom.exit_wild_subtitle");

            if (prevClaim != null) {
                if (prevClaim.getExitTitle() != null && !prevClaim.getExitTitle().isEmpty()) {
                    exitTitle = prevClaim.getExitTitle().replace("&", "§");
                }
                if (prevClaim.getExitSubtitle() != null && !prevClaim.getExitSubtitle().isEmpty()) {
                    exitSubtitle = prevClaim.getExitSubtitle().replace("&", "§");
                }
            }

            // B19 — Evento customizado: ClaimLeaveEvent
            if (prevClaim != null) {
                org.bukkit.Bukkit.getPluginManager().callEvent(
                        new br.com.gorvax.core.events.ClaimLeaveEvent(player, prevClaim));
            }

            lastClaimId.remove(player.getUniqueId());
            player.showTitle(Title.title(
                    LegacyComponentSerializer.legacySection().deserialize(exitTitle),
                    LegacyComponentSerializer.legacySection().deserialize(exitSubtitle),
                    Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000),
                            java.time.Duration.ofMillis(500))));

            // B9 — ActionBar + Som ao sair
            String exitName = "";
            if (prevClaim != null) {
                String nomeReino = plugin.getKingdomManager().getNome(previousId);
                exitName = nomeReino != null ? nomeReino : plugin.getClaimManager().getOwnerName(prevClaim.getOwner());
            }
            player.sendActionBar(LegacyComponentSerializer.legacySection()
                    .deserialize(plugin.getMessageManager().get("visualization.leaving", exitName)));

            PlayerData pdExit = plugin.getPlayerDataManager().getData(player.getUniqueId());
            if (pdExit.isBorderSound()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            }
        }

        // --- LÓGICA DA ACTION BAR INTELIGENTE ---
        // Exibida APENAS na transição de claim (não a cada passo)
        boolean transitioned = (currentId != null && !currentId.equals(previousId))
                || (currentId == null && previousId != null);

        if (transitioned) {
            String barMessage;

            if (claim == null) {
                barMessage = plugin.getMessageManager().get("kingdom.actionbar_wilderness");
            } else {
                String cid = claim.getId();
                String nomeReino = plugin.getKingdomManager().getNome(cid);

                if (nomeReino == null) {
                    // Claim privado comum
                    barMessage = plugin.getMessageManager().get("kingdom.actionbar_private",
                            plugin.getClaimManager().getOwnerName(claim.getOwner()));
                } else {
                    // É um reino
                    // Checkmark (✔) se for súdito ou rei
                    String check = (plugin.getKingdomManager().isSudito(cid, player.getUniqueId()) ||
                            plugin.getKingdomManager().isRei(cid, player.getUniqueId()))
                                    ? plugin.getMessageManager().get("kingdom.actionbar_check")
                                    : "";

                    String status;
                    // Verifica sub-lote (Feudo)
                    if (claim.getSubPlotAt(to) != null) {
                        var sub = claim.getSubPlotAt(to);
                        if (sub.isForSale()) {
                            status = plugin.getMessageManager().get("kingdom.actionbar_for_sale", (int) sub.getPrice());
                        } else if (sub.isForRent() && sub.getRenter() == null) {
                            status = plugin.getMessageManager().get("kingdom.actionbar_for_rent",
                                    (int) sub.getRentPrice());
                        } else if (sub.getRenter() != null) {
                            status = plugin.getMessageManager().get("kingdom.actionbar_rented",
                                    plugin.getClaimManager().getOwnerName(sub.getRenter()));
                        } else if (sub.getOwner() != null) {
                            status = plugin.getMessageManager().get("kingdom.actionbar_owner",
                                    plugin.getClaimManager().getOwnerName(sub.getOwner()));
                        } else {
                            status = plugin.getMessageManager().get("kingdom.actionbar_vacant");
                        }
                    } else {
                        status = plugin.getMessageManager().get("kingdom.actionbar_headquarters");
                    }

                    barMessage = plugin.getMessageManager().get("kingdom.actionbar_kingdom", nomeReino, check, status);
                }
            }
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(barMessage));
        }
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player))
            return;

        // Suporte a dano direto E projéteis (flechas, tridentes, etc.)
        Player attacker;
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) {
            attacker = p;
        } else {
            return;
        }

        Player victim = (Player) e.getEntity();

        // --- NEW: PvP Outside Kingdom (Same Residents) ---
        Claim vKingdom = plugin.getKingdomManager().getKingdom(victim.getUniqueId());
        Claim aKingdom = plugin.getKingdomManager().getKingdom(attacker.getUniqueId());

        if (vKingdom != null && aKingdom != null && vKingdom.getId().equals(aKingdom.getId())) {
            Claim currentLoc = plugin.getClaimManager().getClaimAt(victim.getLocation());
            boolean insideOwnKingdom = (currentLoc != null && currentLoc.getId().equals(vKingdom.getId()));

            if (!insideOwnKingdom) {
                if (!vKingdom.isResidentsPvpOutside()) {
                    e.setCancelled(true);
                    attacker.sendMessage(plugin.getMessageManager().get("pvp.residents_outside_disabled"));
                    return;
                }
            }
        }

        Claim claim = plugin.getClaimManager().getClaimAt(victim.getLocation());
        if (claim == null)
            return; // Wilderness PvP allowed usually

        // 1. Global PvP Check
        if (!claim.isPvp()) {
            e.setCancelled(true);
            attacker.sendMessage(plugin.getMessageManager().get("pvp.pvp_disabled"));
            return;
        }

        // 2. Residents PvP Check (Friendly Fire - Duelos Internos)
        if (!claim.isResidentsPvp()) {
            boolean vRes = false;
            boolean aRes = false;

            if (claim.isKingdom()) {
                vRes = plugin.getKingdomManager().isSudito(claim.getId(), victim.getUniqueId()) ||
                        plugin.getKingdomManager().isRei(claim.getId(), victim.getUniqueId());
                aRes = plugin.getKingdomManager().isSudito(claim.getId(), attacker.getUniqueId()) ||
                        plugin.getKingdomManager().isRei(claim.getId(), attacker.getUniqueId());
            } else {
                // For private claims, owner and trusted are "residents"
                vRes = (claim.getOwner() != null && claim.getOwner().equals(victim.getUniqueId()))
                        || claim.getTrustedPlayers().containsKey(victim.getUniqueId());
                aRes = (claim.getOwner() != null && claim.getOwner().equals(attacker.getUniqueId()))
                        || claim.getTrustedPlayers().containsKey(attacker.getUniqueId());
            }

            if (vRes && aRes) {
                e.setCancelled(true);
                attacker.sendMessage(plugin.getMessageManager().get("pvp.residents_disabled"));
            }
        }
    }

    // --- PROTEÇÃO DE TELEPORTE ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent event) {
        // Ignora administradores e bypass
        if (event.getPlayer().hasPermission("gorvax.admin")) {
            return;
        }

        // Se o destino for o mesmo mundo (maioria dos casos de tp/pérola locais)
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            return; // Mudança de mundo geralmente é segura/controlada por spawns
        }

        Player p = event.getPlayer();
        Location to = event.getTo();
        Claim claim = plugin.getClaimManager().getClaimAt(to);

        // Se não tem claim no destino, está livre
        if (claim == null) {
            return;
        }

        boolean allowed = false;
        SubPlot plot = claim.getSubPlotAt(to);

        // 1. LÓGICA DE SUB-LOTE (Prioridade Máxima)
        if (plot != null) {
            // Se tem Inquilino, só ele (ou quem ele confiar futuramente) entra
            if (plot.getRenter() != null) {
                if (p.getUniqueId().equals(plot.getRenter()))
                    allowed = true;
            }
            // Se tem Dono
            else if (plot.getOwner() != null) {
                if (p.getUniqueId().equals(plot.getOwner()))
                    allowed = true;
                if (plot.hasPermission(p.getUniqueId(), Claim.TrustType.ACESSO))
                    allowed = true;
            }
            // Se não tem dono nem inquilino (Lote à venda/Livre)
            // Segue regras do reino (Area Comum) -> Lógica abaixo
            else {
                // Fallback para lógica do reino
                plot = null; // Trata como se fosse area comum para checagem abaixo
            }
        }

        // 2. LÓGICA DO REINO / AREA COMUM (Se não foi permitido por SubPlot ou não é
        // SubPlot)
        if (!allowed && plot == null) {
            // A. Rei / Dono
            if (claim.getOwner().equals(p.getUniqueId()))
                allowed = true;

            // B. Duque / Vice
            if (claim.hasPermission(p.getUniqueId(), Claim.TrustType.VICE))
                allowed = true;

            // C. Súditos (Se for reino) -> Permite andar nas areas comuns?
            // "Teleport" é entrar. Geralmente súditos podem entrar no reino.
            if (claim.isKingdom()) {
                if (plugin.getKingdomManager().isSudito(claim.getId(), p.getUniqueId()))
                    allowed = true;
            }

            // D. Trusted (Permissões gerais de entrada/acesso)
            if (claim.hasPermission(p.getUniqueId(), Claim.TrustType.ACESSO) ||
                    claim.hasPermission(p.getUniqueId(), Claim.TrustType.GERAL)) {
                allowed = true;
            }
        }

        if (allowed)
            return;

        // Se chegou aqui, o teleporte é INVÁLIDO (Terreno ocupado e sem permissão)
        // Lógica de redirecionamento para a borda

        Location safeLoc = calculateSafeBorder(claim, to);
        if (safeLoc != null) {
            event.setTo(safeLoc);
            // Mensagem e efeito visual (opcional mas bom para feedback)
            plugin.getMessageManager().send(p, "protection.teleport_redirected");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        } else {
            // Se falhar o cálculo (ex: mundo inválido), cancela
            event.setCancelled(true);
            plugin.getMessageManager().send(p, "protection.teleport_cancelled");
        }
    }

    private Location calculateSafeBorder(Claim claim, Location target) {
        int minX = claim.getMinX();
        int minZ = claim.getMinZ();
        int maxX = claim.getMaxX();
        int maxZ = claim.getMaxZ();

        double tx = target.getX();
        double tz = target.getZ();

        // Distâncias para cada borda
        double dLeft = Math.abs(tx - minX);
        double dRight = Math.abs(tx - maxX);
        double dTop = Math.abs(tz - minZ);
        double dBottom = Math.abs(tz - maxZ);

        // Encontrar a menor distância
        double min = Math.min(Math.min(dLeft, dRight), Math.min(dTop, dBottom));

        double newX = tx;
        double newZ = tz;

        // Empurrar 1 bloco para fora da borda mais próxima
        if (min == dLeft) {
            newX = minX - 1.5; // .5 para centralizar no bloco
        } else if (min == dRight) {
            newX = maxX + 1.5;
        } else if (min == dTop) {
            newZ = minZ - 1.5;
        } else { // Bottom
            newZ = maxZ + 1.5;
        }

        // Cria a location base
        Location borderLoc = new Location(target.getWorld(), newX, target.getY(), newZ);

        // Ajusta a altura (Y) para o chão seguro
        // Pega o bloco mais alto no mundo naquela coordenada X, Z
        int highestY = target.getWorld().getHighestBlockYAt((int) newX, (int) newZ);

        // Se o teleporte original era muito alto (ex: elytra), talvez manter?
        // Mas o pedido é "teleportar para fora/borda". Geralmente implica chão.
        // Vamos colocar no chão seguro + 1.
        borderLoc.setY(highestY + 1);

        // Mantém a rotação original do player para ele entender onde está
        borderLoc.setYaw(target.getYaw());
        borderLoc.setPitch(target.getPitch());

        return borderLoc;
    }
}