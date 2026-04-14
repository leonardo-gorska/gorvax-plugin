package br.com.gorvax.core.utils;

import br.com.gorvax.core.GorvaxCore;
import br.com.gorvax.core.managers.Claim;
import br.com.gorvax.core.towns.managers.KingdomManager;

import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * B16 — Hook isolado para BlueMap.
 * Usa a API v2 do BlueMap com callback pattern (BlueMapAPI.onEnable).
 * Evita NoClassDefFoundError se BlueMap não estiver instalado.
 */
public class BlueMapHook {

    private final Logger logger;
    private boolean available = false;
    private Object blueMapApiRef; // Guarda a referência para BlueMapAPI

    public BlueMapHook(Logger logger) {
        this.logger = logger;
        try {
            // Verifica se a classe BlueMapAPI existe no classpath
            Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");

            // Registra callback para quando o BlueMap ficar pronto
            de.bluecolored.bluemap.api.BlueMapAPI.onEnable(api -> {
                this.blueMapApiRef = api;
                this.available = true;
                logger.info("[GorvaxCore] BlueMap API conectada com sucesso!");
            });

            de.bluecolored.bluemap.api.BlueMapAPI.onDisable(api -> {
                this.blueMapApiRef = null;
                this.available = false;
            });

        } catch (Throwable ignored) {
            // BlueMap não disponível
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Atualiza todos os markers no BlueMap com base nos claims atuais.
     */
    public void updateMarkers(List<Claim> claims, KingdomManager km) {
        if (!available || blueMapApiRef == null) return;
        try {
            de.bluecolored.bluemap.api.BlueMapAPI api = (de.bluecolored.bluemap.api.BlueMapAPI) blueMapApiRef;
            boolean showPersonal = GorvaxCore.getInstance().getConfig().getBoolean("webmap.show_personal_claims", false);

            String colorKingdom = GorvaxCore.getInstance().getConfig().getString("webmap.colors.kingdom", "#00AA00");
            String colorOutpost = GorvaxCore.getInstance().getConfig().getString("webmap.colors.outpost", "#FF8800");
            String colorPersonal = GorvaxCore.getInstance().getConfig().getString("webmap.colors.personal", "#5555FF");
            String markerSetLabel = GorvaxCore.getInstance().getConfig().getString("webmap.marker_set_label", "GorvaxCore — Reinos");

            Set<String> activeIds = new HashSet<>();

            // Itera por todos os mapas de todos os mundos
            api.getWorlds().forEach(world -> {
                String worldName = world.getId().toString();

                world.getMaps().forEach(map -> {
                    // Cria ou obtém o marker set
                    de.bluecolored.bluemap.api.markers.MarkerSet markerSet =
                            map.getMarkerSets().computeIfAbsent("gorvax-reinos", id -> {
                                de.bluecolored.bluemap.api.markers.MarkerSet ms = new de.bluecolored.bluemap.api.markers.MarkerSet(markerSetLabel);
                                ms.setDefaultHidden(false);
                                return ms;
                            });

                    // Limpar markers antigos do GorvaxCore neste set
                    Set<String> toRemove = new HashSet<>();
                    markerSet.getMarkers().forEach((id, marker) -> {
                        if (id.startsWith("gorvax_")) {
                            toRemove.add(id);
                        }
                    });
                    toRemove.forEach(id -> markerSet.getMarkers().remove(id));

                    // Adicionar markers para claims neste mundo
                    for (Claim claim : claims) {
                        if (claim.getType() == Claim.Type.LOTE) continue;
                        if (claim.getType() == Claim.Type.TERRENO && !showPersonal) continue;

                        // Verificar se o claim pertence a este mundo
                        if (!worldName.endsWith(claim.getWorldName()) && !claim.getWorldName().equals(worldName)) {
                            continue;
                        }

                        String markerId = "gorvax_" + claim.getId();
                        activeIds.add(markerId);

                        // Criar shape com as coordenadas do claim
                        Shape shape = Shape.createRect(
                                claim.getMinX(), claim.getMinZ(),
                                claim.getMaxX() + 1, claim.getMaxZ() + 1
                        );

                        // Determinar cor e label
                        String hexColor;
                        String label;

                        if (claim.isKingdom()) {
                            hexColor = colorKingdom;
                            label = claim.getKingdomName() != null ? claim.getKingdomName() : claim.getName();
                        } else if (claim.isOutpost()) {
                            hexColor = colorOutpost;
                            label = (claim.getName() != null ? claim.getName() : "Outpost") + " (Outpost)";
                        } else {
                            hexColor = colorPersonal;
                            String ownerName = GorvaxCore.getInstance().getPlayerName(claim.getOwner());
                            label = claim.getName() != null ? claim.getName() : "Terreno de " + ownerName;
                        }

                        int rgb = parseHexColor(hexColor);
                        Color fillColor = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 90 / 255f);
                        Color lineColor = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 1f);

                        de.bluecolored.bluemap.api.markers.ShapeMarker shapeMarker =
                                new de.bluecolored.bluemap.api.markers.ShapeMarker(label, shape, 64);
                        shapeMarker.setFillColor(fillColor);
                        shapeMarker.setLineColor(lineColor);
                        shapeMarker.setLineWidth(2);

                        markerSet.getMarkers().put(markerId, shapeMarker);
                    }
                });
            });
        } catch (Throwable e) {
            logger.warning("[GorvaxCore] Erro ao atualizar markers BlueMap: " + e.getMessage());
        }
    }

    /**
     * Remove todos os markers do GorvaxCore no BlueMap.
     */
    public void clearMarkers() {
        if (!available || blueMapApiRef == null) return;
        try {
            de.bluecolored.bluemap.api.BlueMapAPI api = (de.bluecolored.bluemap.api.BlueMapAPI) blueMapApiRef;
            api.getWorlds().forEach(world -> {
                world.getMaps().forEach(map -> {
                    de.bluecolored.bluemap.api.markers.MarkerSet ms = map.getMarkerSets().get("gorvax-reinos");
                    if (ms != null) {
                        Set<String> toRemove = new HashSet<>();
                        ms.getMarkers().forEach((id, marker) -> {
                            if (id.startsWith("gorvax_")) {
                                toRemove.add(id);
                            }
                        });
                        toRemove.forEach(id -> ms.getMarkers().remove(id));
                    }
                });
            });
        } catch (Throwable ignored) {
        }
    }

    private int parseHexColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0x00AA00; // Verde padrão
        }
    }
}
