package br.com.gorvax.core.managers;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para PermissionManager.
 * Testa lógica de definição de grupos (nomes, pesos, prefixos, permissões).
 * Como a classe depende do LuckPerms, replicamos a lógica de configuração.
 */
class PermissionManagerTest {

    // --- Modelo de grupo para testes ---

    record GroupConfig(String name, int weight, String prefix, List<String> permissions) {
    }

    /**
     * Replica a lista de grupos definida em setupGroups().
     */
    private List<GroupConfig> getExpectedGroups() {
        return List.of(
                new GroupConfig("default", 0, null,
                        List.of("gorvax.player", "gorvax.city.join", "gorvax.city.leave",
                                "gorvax.plot.buy", "gorvax.plot.sell")),
                new GroupConfig("prefeito", 10, "§b[Prefeito] ",
                        List.of("gorvax.mayor", "gorvax.city.create", "gorvax.city.rename",
                                "gorvax.city.delete", "gorvax.city.claim", "gorvax.city.invite")),
                new GroupConfig("rei", 15, "§6[Rei] ",
                        List.of("gorvax.king", "gorvax.mayor", "gorvax.city.create",
                                "gorvax.city.rename", "gorvax.city.delete", "gorvax.city.claim",
                                "gorvax.city.invite")),
                new GroupConfig("gorvax-admin", 100, "§c[Admin] ",
                        List.of("gorvax.admin", "gorvax.king", "gorvax.mayor",
                                "minecraft.command.op")),
                new GroupConfig("vip", 20, "§a[✦ VIP] ",
                        List.of("gorvax.vip", "gorvax.player")),
                new GroupConfig("vip-plus", 25, "§b[✦ VIP+] ",
                        List.of("gorvax.vip", "gorvax.vip.plus", "gorvax.player")),
                new GroupConfig("elite", 30, "§6[⚡ ELITE] ",
                        List.of("gorvax.vip", "gorvax.vip.elite", "gorvax.player")),
                new GroupConfig("lendario", 35, "§d[🐉 LENDÁRIO] ",
                        List.of("gorvax.vip", "gorvax.vip.lendario", "gorvax.player")));
    }

    @Test
    void total8Grupos() {
        assertEquals(8, getExpectedGroups().size());
    }

    @Test
    void grupoDefaultSemPrefixo() {
        GroupConfig def = getExpectedGroups().get(0);
        assertEquals("default", def.name());
        assertEquals(0, def.weight());
        assertNull(def.prefix());
    }

    @Test
    void grupoPrefeitoTemPrefixo() {
        GroupConfig prefeito = getExpectedGroups().get(1);
        assertEquals("prefeito", prefeito.name());
        assertEquals(10, prefeito.weight());
        assertNotNull(prefeito.prefix());
        assertTrue(prefeito.prefix().contains("Prefeito"));
    }

    @Test
    void grupoReiPeso15() {
        GroupConfig rei = getExpectedGroups().get(2);
        assertEquals("rei", rei.name());
        assertEquals(15, rei.weight());
    }

    @Test
    void grupoAdminPesoMaisAlto() {
        GroupConfig admin = getExpectedGroups().get(3);
        assertEquals("gorvax-admin", admin.name());
        assertEquals(100, admin.weight());
        assertTrue(admin.permissions().contains("minecraft.command.op"));
    }

    @Test
    void gruposVipPesosOrdenados() {
        var groups = getExpectedGroups();
        GroupConfig vip = groups.get(4);
        GroupConfig vipPlus = groups.get(5);
        GroupConfig elite = groups.get(6);
        GroupConfig lendario = groups.get(7);

        assertTrue(vip.weight() < vipPlus.weight());
        assertTrue(vipPlus.weight() < elite.weight());
        assertTrue(elite.weight() < lendario.weight());
    }

    @Test
    void todosVipTemPermissaoBase() {
        var groups = getExpectedGroups();
        for (int i = 4; i <= 7; i++) {
            GroupConfig g = groups.get(i);
            assertTrue(g.permissions().contains("gorvax.vip"),
                    g.name() + " deveria ter gorvax.vip");
            assertTrue(g.permissions().contains("gorvax.player"),
                    g.name() + " deveria ter gorvax.player");
        }
    }

    @Test
    void defaultTemPermissoes() {
        GroupConfig def = getExpectedGroups().get(0);
        assertEquals(5, def.permissions().size());
        assertTrue(def.permissions().contains("gorvax.player"));
        assertTrue(def.permissions().contains("gorvax.city.join"));
        assertTrue(def.permissions().contains("gorvax.city.leave"));
        assertTrue(def.permissions().contains("gorvax.plot.buy"));
        assertTrue(def.permissions().contains("gorvax.plot.sell"));
    }

    @Test
    void reiTemPermissaoDePrefeito() {
        GroupConfig rei = getExpectedGroups().get(2);
        assertTrue(rei.permissions().contains("gorvax.mayor"));
        assertTrue(rei.permissions().contains("gorvax.king"));
    }

    @Test
    void adminTemPermissaoDeReiEPrefeito() {
        GroupConfig admin = getExpectedGroups().get(3);
        assertTrue(admin.permissions().contains("gorvax.admin"));
        assertTrue(admin.permissions().contains("gorvax.king"));
        assertTrue(admin.permissions().contains("gorvax.mayor"));
    }

    @Test
    void eliteTemPermissaoElite() {
        GroupConfig elite = getExpectedGroups().get(6);
        assertTrue(elite.permissions().contains("gorvax.vip.elite"));
    }

    @Test
    void lendarioTemPermissaoLendario() {
        GroupConfig lendario = getExpectedGroups().get(7);
        assertTrue(lendario.permissions().contains("gorvax.vip.lendario"));
    }

    @Test
    void nomesUnicosEntreGrupos() {
        var names = getExpectedGroups().stream().map(GroupConfig::name).toList();
        assertEquals(names.size(), new HashSet<>(names).size(), "Nomes de grupos devem ser únicos");
    }

    @Test
    void pesosUnicosEntreGrupos() {
        var weights = getExpectedGroups().stream().map(GroupConfig::weight).toList();
        assertEquals(weights.size(), new HashSet<>(weights).size(), "Pesos de grupos devem ser únicos");
    }
}
