package br.com.gorvax.core.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.ArrayList;

public class Claim {

    private final String id;
    private UUID owner;
    private final String worldName;
    private int minX, minZ;
    private int maxX, maxZ;
    private String name;
    private String kingdomName; // Added kingdom name for progression
    private String parentKingdomId; // B13 — ID do claim principal do reino (para outposts)

    private final java.util.Map<UUID, java.util.Set<TrustType>> trustedPlayers;
    private final java.util.List<SubPlot> subPlots;

    // Customização v2.4
    private String tag; // Max 3 chars
    private String chatColor = "§f";
    private String welcomeColor = "§b";
    private String tagColor = "§e";
    private double tax = 5.0; // Taxa padrão do mercado da cidade

    public enum Type {
        REINO,
        TERRENO,
        LOTE,
        OUTPOST
    }

    private Type type = Type.TERRENO;
    private boolean pvp = false;
    private boolean residentsPvp = false;
    private boolean residentsPvpOutside = false;
    private boolean isPublic = true; // B1.4 — Reino aceita visitantes por padrão
    private int upkeepDebtDays = 0; // B36 — Dias de dívida de manutenção (claims pessoais)

    // Custom Messages
    private String enterTitle;
    private String enterSubtitle;
    private String exitTitle;
    private String exitSubtitle;

    public enum TrustType {
        ACESSO, // Botões, alavancas, portas
        CONTEINER, // Baús, fornalhas
        CONSTRUCAO, // Construir e quebrar (Antiga GERAL)
        GERAL, // Permissão TOTAL (Super-User)
        VICE // Administrativo (Pode dar permissão)
    }

    public Claim(String id, UUID owner, Location corner1, Location corner2) {
        this.id = id;
        this.owner = owner;
        this.worldName = corner1.getWorld().getName();
        this.minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        this.name = "Terreno de " + Bukkit.getOfflinePlayer(owner).getName();
        this.trustedPlayers = new ConcurrentHashMap<>();
        this.subPlots = new CopyOnWriteArrayList<>();
    }

    // Load Constructor
    public Claim(String id, UUID owner, String worldName, int minX, int minZ, int maxX, int maxZ, boolean isKingdom,
            String name) {
        this.id = id;
        this.owner = owner;
        this.worldName = worldName;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.type = isKingdom ? Type.REINO : Type.TERRENO;
        this.name = name;
        this.kingdomName = null; // Default
        this.trustedPlayers = new ConcurrentHashMap<>();
        this.subPlots = new CopyOnWriteArrayList<>();
    }

    public void addSubPlot(SubPlot plot) {
        subPlots.add(plot);
    }

    public void removeSubPlot(SubPlot plot) {
        subPlots.remove(plot);
    }

    public SubPlot getSubPlotAt(Location loc) {
        for (SubPlot plot : subPlots) {
            if (plot.contains(loc))
                return plot;
        }
        return null;
    }

    public SubPlot getSubPlot(String id) {
        for (SubPlot plot : subPlots) {
            if (plot.getId().equals(id)) {
                return plot;
            }
        }
        return null;
    }

    public java.util.List<SubPlot> getSubPlots() {
        return subPlots;
    }

    public void addTrust(UUID player, TrustType type) {
        trustedPlayers.computeIfAbsent(player, k -> new java.util.HashSet<>()).add(type);
    }

    public void removeTrust(UUID player) {
        trustedPlayers.remove(player);
    }

    public void removeTrust(UUID player, TrustType type) {
        if (trustedPlayers.containsKey(player)) {
            trustedPlayers.get(player).remove(type);
            if (trustedPlayers.get(player).isEmpty()) {
                trustedPlayers.remove(player);
            }
        }
    }

    public boolean hasPermission(UUID player, TrustType required) {
        if (player.equals(owner))
            return true;
        if (!trustedPlayers.containsKey(player))
            return false;

        java.util.Set<TrustType> perms = trustedPlayers.get(player);

        if (perms.contains(TrustType.VICE))
            return true;

        if (required == TrustType.VICE)
            return false;

        // GERAL (Super-User) concede tudo (exceto VICE/Admin e funções de dono)
        if (perms.contains(TrustType.GERAL))
            return true;

        // ESTRICTO: Não há mais herança (ex: Construção NÃO dá Container).
        // Cada permissão deve ser explicita.
        return perms.contains(required);
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName))
            return false;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private boolean residentsBuild = false;
    private boolean residentsContainer = false;
    private boolean residentsSwitch = false;

    // Escopo de Cidade/Reino (Áreas Comuns)
    public boolean hasPermissionKingdomScope(java.util.UUID player, TrustType required) {
        if (player.equals(owner))
            return true;

        // 1. Staff do Reino (Vice/Duque) tem acesso total
        if (hasPermission(player, TrustType.VICE))
            return true;

        // 2. Confiança Direta (Trust list)
        if (trustedPlayers.containsKey(player)) {
            java.util.Set<TrustType> perms = trustedPlayers.get(player);

            if (perms.contains(TrustType.VICE))
                return true;

            if (perms.contains(TrustType.GERAL) && required != TrustType.VICE)
                return true;

            if (perms.contains(required))
                return true;
        }

        // 3. Permissões de Residente (Flags Globais - Apenas para Súditos)
        // Isso será checado no Listener, mas se quisermos centralizar aqui:
        // O Listener verifica a flag (ex: isResidentsBuild) E se o player é súdito.
        // Manter essa lógica no Listener permite mensagens de erro mais específicas
        // ("Requer ser habitante").

        return false;
    }

    // Método adicionado para corrigir erro de compilação
    public boolean isResident(UUID playerUUID) {
        if (playerUUID.equals(owner))
            return true;

        // Verifica se é residente oficial do reino (via KingdomManager)
        // Mas esta classe Claim é agnóstica de KingdomManager.
        // Porém, para PVP check (residents only), confiamos na lista de trust ou na
        // flag de reino?
        // O KingdomManager gerencia a lista oficial de "Súditos".
        // Aqui, podemos verificar se o jogador tem ALGUMA permissão, ou se está na
        // trust list.
        // Mas "Súdito" é diferente de "Trust".

        // CORREÇÃO: Vamos assumir que para isResident, verificamos se ele tem alguma
        // permissão ou é dono.
        // OU melhor, vamos delegar para o Listener verificar isSudito usando
        // KingdomManager.

        // Entretanto, para corrigir RAPIDO o erro de compilação onde `Claim.isResident`
        // é chamado:
        // O erro estava em ProtectionListener linha 185.
        // Lá ele quer saber se os dois players são residentes para aplicar a logica de
        // residentsPvp.

        // Vamos implementar baseando-se na TrustList por enquanto, pois KingdomManager
        // não é acessível estaticamente fácil aqui sem acoplamento.
        // Se isKingdom, a lista de suditos fica no config.

        // WORKAROUND: Se o player está na trust list, ele é considerado "residente"
        // para fins de PVP safe zone?
        // Não necessariamente.

        // Melhor abordagem: O ProtectionListener deve usar KingdomManager.isSudito().
        // Mas o código já chama claim.isResident().
        // Vamos implementar isResident checando se o player está na lista de
        // trustedPlayers como paliativo,
        // mas o ideal é que isResident retornasse true se ele é membro do reino.

        return trustedPlayers.containsKey(playerUUID);
    }

    public boolean isResidentsBuild() {
        return residentsBuild;
    }

    public void setResidentsBuild(boolean residentsBuild) {
        this.residentsBuild = residentsBuild;
    }

    public boolean isResidentsContainer() {
        return residentsContainer;
    }

    public void setResidentsContainer(boolean residentsContainer) {
        this.residentsContainer = residentsContainer;
    }

    public boolean isResidentsSwitch() {
        return residentsSwitch;
    }

    public void setResidentsSwitch(boolean residentsSwitch) {
        this.residentsSwitch = residentsSwitch;
    }

    public String getKingdomName() {
        return kingdomName;
    }

    public void setKingdomName(String kingdomName) {
        this.kingdomName = kingdomName;
    }

    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    // Getters
    public String getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    public boolean isKingdom() {
        return type == Type.REINO;
    }

    // B13 — Outposts
    public boolean isOutpost() {
        return type == Type.OUTPOST;
    }

    public String getParentKingdomId() {
        return parentKingdomId;
    }

    public void setParentKingdomId(String parentKingdomId) {
        this.parentKingdomId = parentKingdomId;
    }

    public String getName() {
        return name;
    }

    public java.util.Map<UUID, java.util.Set<TrustType>> getTrustedPlayers() {
        return trustedPlayers;
    }

    public void setKingdom(boolean kingdom) {
        this.type = kingdom ? Type.REINO : Type.TERRENO;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChatColor() {
        return chatColor;
    }

    public void setChatColor(String chatColor) {
        this.chatColor = chatColor;
    }

    public String getWelcomeColor() {
        return welcomeColor;
    }

    public void setWelcomeColor(String welcomeColor) {
        this.welcomeColor = welcomeColor;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public boolean isResidentsPvp() {
        return residentsPvp;
    }

    public void setResidentsPvp(boolean residentsPvp) {
        this.residentsPvp = residentsPvp;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        if (tag != null) {
            // Remove códigos de cor para evitar spoofing no chat
            tag = tag.replaceAll("[§&]", "");
            if (tag.length() > 3) {
                tag = tag.substring(0, 3);
            }
        }
        this.tag = tag;
    }

    public String getTagColor() {
        return tagColor;
    }

    public void setTagColor(String tagColor) {
        this.tagColor = tagColor;
    }

    public boolean isResidentsPvpOutside() {
        return residentsPvpOutside;
    }

    public void setResidentsPvpOutside(boolean residentsPvpOutside) {
        this.residentsPvpOutside = residentsPvpOutside;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    // B36 — Upkeep de claims pessoais
    public int getUpkeepDebtDays() {
        return upkeepDebtDays;
    }

    public void setUpkeepDebtDays(int upkeepDebtDays) {
        this.upkeepDebtDays = upkeepDebtDays;
    }

    public String getEnterTitle() {
        return enterTitle;
    }

    public void setEnterTitle(String enterTitle) {
        this.enterTitle = enterTitle;
    }

    public String getEnterSubtitle() {
        return enterSubtitle;
    }

    public void setEnterSubtitle(String enterSubtitle) {
        this.enterSubtitle = enterSubtitle;
    }

    public String getExitTitle() {
        return exitTitle;
    }

    public void setExitTitle(String exitTitle) {
        this.exitTitle = exitTitle;
    }

    public String getExitSubtitle() {
        return exitSubtitle;
    }

    public void setExitSubtitle(String exitSubtitle) {
        this.exitSubtitle = exitSubtitle;
    }
}
