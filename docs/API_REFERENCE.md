# 🔗 Referência de APIs — GorvaxCore

Guia rápido das APIs utilizadas no projeto com exemplos de uso.

---

## Paper API (1.21+)

**Javadocs**: https://jd.papermc.io/paper/1.21/

### Classes Mais Usadas no Projeto

| Classe | Uso no GorvaxCore |
|---|---|
| `JavaPlugin` | Base de `GorvaxCore.java` |
| `BukkitRunnable` | Tasks assíncronas e periódicas |
| `Listener` + `@EventHandler` | Todos os listeners |
| `CommandExecutor` | Todos os commands |
| `InventoryHolder` | GUIs customizadas (menus) |
| `PersistentDataContainer` | Metadados em itens |
| `NamespacedKey` | Chaves para PersistentData |
| `BossBar` | Barras de HP dos bosses |
| `FileConfiguration` | Leitura/escrita YAML |
| `Particle` | Efeitos visuais (bosses, seleção) |
| `Attribute` | Stats dos mobs (HP, dano, speed) |

### Padrão de Task Assíncrona
```java
// Save assíncrono — padrão do projeto
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // I/O de disco aqui (salvar YAML)
});

// Task periódica
new BukkitRunnable() {
    @Override
    public void run() {
        // Lógica periódica
    }
}.runTaskTimer(plugin, 0L, 20L * 60); // a cada 60 segundos
```

---

## Vault API (Economia)

**Repositório**: https://github.com/MilkBowl/VaultAPI

### Uso no Projeto
```java
// Setup (no onEnable)
RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
    .getRegistration(Economy.class);
Economy economy = rsp.getProvider();

// Operações
economy.getBalance(player);              // Consultar saldo
economy.withdrawPlayer(player, amount);   // Debitar
economy.depositPlayer(player, amount);    // Creditar
economy.has(player, amount);             // Verificar se tem saldo
```

---

## WorldGuard API

**Docs**: https://worldguard.enginehub.org/en/latest/developer/

### Uso no Projeto (WorldGuardHook)
```java
// Verificar se uma região tem proteção WorldGuard
RegionContainer container = WorldGuard.getInstance()
    .getPlatform().getRegionContainer();
RegionManager regions = container.get(BukkitAdapter.adapt(world));

// Verificar overlap com regiões existentes
ApplicableRegionSet set = regions.getApplicableRegions(region);
```

---

## PlaceholderAPI

**Wiki**: https://github.com/PlaceholderAPI/PlaceholderAPI/wiki

### Uso no Projeto (GorvaxExpansion)
```java
public class GorvaxExpansion extends PlaceholderExpansion {
    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        switch (identifier) {
            case "localizacao_label": return getLocationLabel(p);
            case "reino_nome": return getKingdomName(p);
            // ...
        }
    }
}
```

**Placeholders registrados**: 50+ (ver [Placeholders.md](../Placeholders.md))

---

## LuckPerms API

**Wiki**: https://luckperms.net/wiki/Developer-API

### Uso no Projeto (PermissionManager)
```java
LuckPerms api = LuckPermsProvider.get();

// Dar permissão
User user = api.getUserManager().getUser(uuid);
user.data().add(Node.builder("gorvax.mayor").build());
api.getUserManager().saveUser(user);

// Adicionar a grupo
InheritanceNode node = InheritanceNode.builder("rei").build();
user.data().add(node);
```

---

## GeyserMC / Floodgate

**Wiki**: https://wiki.geysermc.org/

### Detectar Jogador Bedrock
```java
// Verificação usada no InputManager
public boolean isBedrockPlayer(Player player) {
    if (Bukkit.getPluginManager().getPlugin("Floodgate") == null) return false;
    return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
}
```

**Regra**: Todo input via AnvilGUI deve ter fallback chat para Bedrock.

---

## AnvilGUI

**Repositório**: https://github.com/WesJD/AnvilGUI

### Uso no Projeto (InputManager)
```java
new AnvilGUI.Builder()
    .plugin(plugin)
    .title("Título do Input")
    .text("Texto inicial")
    .onClick((slot, snapshot) -> {
        if (slot == AnvilGUI.Slot.OUTPUT) {
            String input = snapshot.getText();
            // Processar input
            return List.of(AnvilGUI.ResponseAction.close());
        }
        return Collections.emptyList();
    })
    .open(player);
```

**Nota**: Relocado via Shadow para `br.com.gorvax.libs.anvilgui`

---

## Gradle + Paperweight

**Compilação**: `./gradlew shadowJar`

O Paperweight Userdev mapeia automaticamente as classes do Paper para nomes legíveis durante o desenvolvimento. O Shadow plugin embarca o AnvilGUI no JAR final com relocação de pacote.

---

## bStats (Métricas)

**Site**: https://bstats.org

### Uso no Projeto
```java
// Setup (no onEnable)
Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);

// Custom chart
metrics.addCustomChart(new SimplePie("storage_backend", () -> {
    return config.getString("storage.type", "yaml");
}));
```

**Versão**: 3.0.2 (embarcado via Shadow)

---

## HikariCP (Connection Pooling)

**Repositório**: https://github.com/brettwooldridge/HikariCP

### Uso no Projeto (MySQL Backend)
```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
config.setUsername(username);
config.setPassword(password);
config.setMaximumPoolSize(10);

HikariDataSource dataSource = new HikariDataSource(config);
```

**Versão**: 5.1.0 (embarcado via Shadow)

---

## Adventure API (Componentes de Texto)

**Docs**: https://docs.advntr.dev/

### Uso no Projeto
```java
// Componente de texto rico
Component message = Component.text("Bem-vindo ao ")
    .append(Component.text("GorvaxCore").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
    .append(Component.text("!"));

player.sendMessage(message);

// MiniMessage
MiniMessage mm = MiniMessage.miniMessage();
Component parsed = mm.deserialize("<gold><bold>GorvaxCore</bold></gold> - Mensagem formatada");
```

**Nota**: Paper 1.21+ inclui Adventure nativamente. Usado extensivamente para mensagens, títulos, boss bars e action bars.
