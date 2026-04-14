package br.com.gorvax.core.utils;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.managers.KingdomManager;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * B16 — Hook isolado para Dynmap usando reflexão pura.
 * Evita dependência compile-time no Dynmap para não gerar ClassNotFound.
 * Toda interação com a Dynmap API ocorre via reflection.
 */
public class DynmapHook {

    private final Logger logger;
    private boolean available = false;
    private Object markerApi;
    private Object markerSet;

    public DynmapHook(Logger logger) {
        this.logger = logger;
        try {
            Object dynmapPlugin = Bukkit.getPluginManager().getPlugin("dynmap");
            if (dynmapPlugin == null) return;

            // Obter MarkerAPI via reflexão: dynmapPlugin.getMarkerAPI()
            Method getMarkerAPI = dynmapPlugin.getClass().getMethod("getMarkerAPI");
            this.markerApi = getMarkerAPI.invoke(dynmapPlugin);

            if (this.markerApi == null) return;

            // Criar ou obter MarkerSet: markerApi.createMarkerSet(id, label, null, false)
            Method createMarkerSet = this.markerApi.getClass().getMethod("createMarkerSet",
                    String.class, String.class, Set.class, boolean.class);
            this.markerSet = createMarkerSet.invoke(this.markerApi,
                    "gorvax.reinos", "GorvaxCore — Reinos", null, false);

            // Se já existe, buscar
            if (this.markerSet == null) {
                Method getMarkerSet = this.markerApi.getClass().getMethod("getMarkerSet", String.class);
                this.markerSet = getMarkerSet.invoke(this.markerApi, "gorvax.reinos");
            }

            this.available = (this.markerSet != null);

        } catch (Throwable e) {
            logger.warning("[GorvaxCore] Falha ao inicializar DynmapHook: " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Atualiza todos os markers no Dynmap com base nos claims atuais.
     */
    public void updateMarkers(List<Claim> claims, KingdomManager km) {
        if (!available || markerSet == null) return;
        try {
            boolean showPersonal = GorvaxCore.getInstance().getConfig().getBoolean("webmap.show_personal_claims", false);
            String colorKingdom = GorvaxCore.getInstance().getConfig().getString("webmap.colors.kingdom", "#00AA00");
            String colorOutpost = GorvaxCore.getInstance().getConfig().getString("webmap.colors.outpost", "#FF8800");
            String colorPersonal = GorvaxCore.getInstance().getConfig().getString("webmap.colors.personal", "#5555FF");

            Set<String> activeIds = new HashSet<>();

            for (Claim claim : claims) {
                if (claim.getType() == Claim.Type.LOTE) continue;
                if (claim.getType() == Claim.Type.TERRENO && !showPersonal) continue;

                String markerId = "gorvax_" + claim.getId();
                activeIds.add(markerId);

                String hexColor;
                String label;
                String description;

                if (claim.isKingdom()) {
                    hexColor = colorKingdom;
                    String kingdomName = claim.getKingdomName() != null ? claim.getKingdomName() : claim.getName();
                    String ownerName = GorvaxCore.getInstance().getPlayerName(claim.getOwner());
                    int memberCount = km != null ? km.getSuditosCount(claim.getId()) + 1 : 1;
                    int level = km != null ? km.getKingdomLevel(claim.getId()) : 1;
                    label = kingdomName;
                    description = buildDescription(kingdomName, ownerName, memberCount, level, "Reino");
                } else if (claim.isOutpost()) {
                    hexColor = colorOutpost;
                    label = (claim.getName() != null ? claim.getName() : "Outpost") + " (Outpost)";
                    String ownerName = GorvaxCore.getInstance().getPlayerName(claim.getOwner());
                    description = buildDescription(label, ownerName, 0, 0, "Outpost");
                } else {
                    hexColor = colorPersonal;
                    String ownerName = GorvaxCore.getInstance().getPlayerName(claim.getOwner());
                    label = claim.getName() != null ? claim.getName() : "Terreno de " + ownerName;
                    description = buildDescription(label, ownerName, 0, 0, "Terreno");
                }

                int colorInt = parseHexColor(hexColor);

                // Coordenadas do claim (polígono retangular)
                double[] xCorners = new double[]{claim.getMinX(), claim.getMaxX() + 1, claim.getMaxX() + 1, claim.getMinX()};
                double[] zCorners = new double[]{claim.getMinZ(), claim.getMinZ(), claim.getMaxZ() + 1, claim.getMaxZ() + 1};

                setOrCreateAreaMarker(markerId, label, description, claim.getWorldName(),
                        xCorners, zCorners, colorInt);
            }

            // Remover markers antigos que não estão mais ativos
            removeStaleMarkers(activeIds);

        } catch (Throwable e) {
            logger.warning("[GorvaxCore] Erro ao atualizar markers Dynmap: " + e.getMessage());
        }
    }

    /**
     * Cria ou atualiza um AreaMarker via reflexão.
     */
    private void setOrCreateAreaMarker(String id, String label, String description,
                                        String world, double[] x, double[] z, int color) {
        try {
            // Buscar marker existente: markerSet.findAreaMarker(id)
            Method findAreaMarker = markerSet.getClass().getMethod("findAreaMarker", String.class);
            Object area = findAreaMarker.invoke(markerSet, id);

            if (area == null) {
                // Criar novo: markerSet.createAreaMarker(id, label, false, world, x, z, false)
                Method createAreaMarker = markerSet.getClass().getMethod("createAreaMarker",
                        String.class, String.class, boolean.class, String.class,
                        double[].class, double[].class, boolean.class);
                area = createAreaMarker.invoke(markerSet, id, label, false, world, x, z, false);
            } else {
                // Atualizar coordenadas: area.setCornerLocations(x, z)
                Method setCornerLocations = area.getClass().getMethod("setCornerLocations",
                        double[].class, double[].class);
                setCornerLocations.invoke(area, x, z);

                // Atualizar label
                Method setLabel = area.getClass().getMethod("setLabel", String.class);
                setLabel.invoke(area, label);
            }

            if (area != null) {
                // Setar descrição (popup HTML)
                Method setDescription = area.getClass().getMethod("setDescription", String.class);
                setDescription.invoke(area, description);

                // Cor de preenchimento: area.setFillStyle(0.35, color)
                Method setFillStyle = area.getClass().getMethod("setFillStyle", double.class, int.class);
                setFillStyle.invoke(area, 0.35, color);

                // Cor da borda: area.setLineStyle(2, 1.0, color)
                Method setLineStyle = area.getClass().getMethod("setLineStyle", int.class, double.class, int.class);
                setLineStyle.invoke(area, 2, 1.0, color);
            }
        } catch (Throwable e) {
            logger.warning("[GorvaxCore] Erro ao criar marker Dynmap '" + id + "': " + e.getMessage());
        }
    }

    /**
     * Remove markers que não estão mais na lista ativa.
     */
    private void removeStaleMarkers(Set<String> activeIds) {
        try {
            // markerSet.getAreaMarkers() retorna Set<AreaMarker>
            Method getAreaMarkers = markerSet.getClass().getMethod("getAreaMarkers");
            @SuppressWarnings("unchecked")
            Set<?> existing = (Set<?>) getAreaMarkers.invoke(markerSet);

            if (existing == null) return;

            for (Object area : existing) {
                Method getMarkerId = area.getClass().getMethod("getMarkerID");
                String mid = (String) getMarkerId.invoke(area);
                if (mid != null && mid.startsWith("gorvax_") && !activeIds.contains(mid)) {
                    Method deleteMarker = area.getClass().getMethod("deleteMarker");
                    deleteMarker.invoke(area);
                }
            }
        } catch (Throwable e) {
            logger.warning("[GorvaxCore] Erro ao limpar markers antigos Dynmap: " + e.getMessage());
        }
    }

    /**
     * Remove todos os markers do GorvaxCore no Dynmap.
     */
    public void clearMarkers() {
        if (!available || markerSet == null) return;
        try {
            Method getAreaMarkers = markerSet.getClass().getMethod("getAreaMarkers");
            @SuppressWarnings("unchecked")
            Set<?> existing = (Set<?>) getAreaMarkers.invoke(markerSet);

            if (existing == null) return;

            for (Object area : existing) {
                Method getMarkerId = area.getClass().getMethod("getMarkerID");
                String mid = (String) getMarkerId.invoke(area);
                if (mid != null && mid.startsWith("gorvax_")) {
                    Method deleteMarker = area.getClass().getMethod("deleteMarker");
                    deleteMarker.invoke(area);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private String buildDescription(String name, String owner, int members, int level, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:sans-serif;'>");
        sb.append("<b>").append(name).append("</b><br>");
        sb.append("<i>").append(type).append("</i><br>");
        sb.append("Dono: ").append(owner).append("<br>");
        if (members > 0) sb.append("Membros: ").append(members).append("<br>");
        if (level > 0) sb.append("Nível: ").append(level);
        sb.append("</div>");
        return sb.toString();
    }

    private int parseHexColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0x00AA00; // Verde padrão
        }
    }
}
