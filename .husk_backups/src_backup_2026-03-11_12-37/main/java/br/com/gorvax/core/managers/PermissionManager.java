package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.WeightNode;

public class PermissionManager {

    private final GorvaxCore plugin;
    private final LuckPerms luckPerms;

    public PermissionManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.luckPerms = plugin.getLuckPerms();
    }

    public void setupGroups() {
        if (luckPerms == null) {
            plugin.getLogger().warning("LuckPerms não detectado. Pulando configuração de grupos.");
            return;
        }

        plugin.getLogger().info("§a[Permissões] Verificando grupos e permissões...");

        // 1. Grupo DEFAULT (Jogadores comuns)
        updateGroup("default", 0, null,
                "gorvax.player",
                "gorvax.city.join",
                "gorvax.city.leave",
                "gorvax.plot.buy",
                "gorvax.plot.sell");

        // 2. Grupo PREFEITO
        updateGroup("prefeito", 10, "§b[Prefeito] ",
                "gorvax.mayor",
                "gorvax.city.create",
                "gorvax.city.rename",
                "gorvax.city.delete", // Cuidado com este
                "gorvax.city.claim",
                "gorvax.city.invite");

        // 3. Grupo REI (Título permanente de Rei)
        updateGroup("rei", 15, "§6[Rei] ",
                "gorvax.king",
                "gorvax.mayor",
                "gorvax.city.create",
                "gorvax.city.rename",
                "gorvax.city.delete",
                "gorvax.city.claim",
                "gorvax.city.invite");

        // 4. Grupo GORVAX-ADMIN (Staff)
        updateGroup("gorvax-admin", 100, "§c[Admin] ",
                "gorvax.admin",
                "gorvax.king", // Admin também é rei
                "gorvax.mayor", // Admin também é prefeito
                "minecraft.command.op");

        // B14-VIP — Grupos VIP (Ranks Premium)
        updateGroup("vip", 20, "§a[VIP] ",
                "gorvax.vip", "gorvax.player");

        updateGroup("vip-plus", 25, "§b[VIP+] ",
                "gorvax.vip", "gorvax.vip.plus", "gorvax.player");

        updateGroup("elite", 30, "§6[ELITE] ",
                "gorvax.vip", "gorvax.vip.elite", "gorvax.player");

        updateGroup("lendario", 35, "§d[LENDARIO] ",
                "gorvax.vip", "gorvax.vip.lendario", "gorvax.player");

        // Grupos de Game Rank (progressão gratuita — prefixo rank_ para não colidir com VIP)
        updateGroup("rank_aventureiro", 1, null, "gorvax.player");
        updateGroup("rank_explorador", 5, null, "gorvax.player");
        updateGroup("rank_guerreiro", 8, null, "gorvax.player");
        updateGroup("rank_lendario", 12, null, "gorvax.player");
    }

    private void updateGroup(String groupName, int weight, String prefix, String... permissions) {
        // Tenta carregar ou criar
        luckPerms.getGroupManager().loadGroup(groupName).thenAcceptAsync(optionalGroup -> {
            Group group = optionalGroup.orElse(null);
            boolean created = false;

            if (group == null) {
                try {
                    group = luckPerms.getGroupManager().createAndLoadGroup(groupName).join();
                    created = true;
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao criar grupo " + groupName + ": " + e.getMessage());
                    return;
                }
            }

            // Define Peso (Weight)
            if (weight > 0) {
                Node weightNode = WeightNode.builder(weight).build();
                group.data().add(weightNode);
            }

            // Define Prefixo (Opcional)
            if (prefix != null) {
                Node prefixNode = PrefixNode.builder(prefix, weight).build();
                group.data().add(prefixNode);
            }

            // Adiciona Permissões
            for (String perm : permissions) {
                Node node = Node.builder(perm).value(true).build();
                group.data().add(node);
            }

            // Salva
            luckPerms.getGroupManager().saveGroup(group);

            if (created) {
                plugin.getLogger().info("§a[Permissões] Grupo '" + groupName + "' criado com sucesso!");
            }
        });
    }
}
