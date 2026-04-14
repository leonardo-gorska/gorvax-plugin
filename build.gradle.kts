plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "br.com.gorvax"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://mvn.wesjd.net/repository/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.mikeprimm.com/")  // B16 — Dynmap API
    maven("https://repo.bluecolored.de/releases")  // B16 — BlueMap API
}

dependencies {
    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")
    
    // Dependencies
    implementation("net.wesjd:anvilgui:1.10.11-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:5.1.0") // B18 — MySQL connection pooling
    implementation("org.bstats:bstats-bukkit:3.0.2") // B32 — Métricas anônimas
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly("me.clip:placeholderapi:2.11.6") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly("org.geysermc.geyser:api:2.4.2-SNAPSHOT") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly("net.luckperms:api:5.4") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    // B16 — Web Map Integration (Dynmap usa reflexão pura, sem dependência compile-time)
    compileOnly("de.bluecolored.bluemap:BlueMapAPI:2.7.2")

    // Testes unitários
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.mockito:mockito-core:5.14.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    test {
        useJUnitPlatform()
    }
    // B34 — Task separada para rodar testes de integração (classes *IT.java)
    register<Test>("integrationTest") {
        useJUnitPlatform {
            includeTags("integration")
        }
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        description = "Roda testes de integração end-to-end (tag: integration)"
        group = "verification"
    }
    compileJava {
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
    assemble {
        dependsOn(shadowJar)
    }
    
    shadowJar {
        relocate("net.wesjd.anvilgui", "br.com.gorvax.libs.anvilgui")
        relocate("com.zaxxer.hikari", "br.com.gorvax.libs.hikari") // B18
        relocate("org.bstats", "br.com.gorvax.libs.bstats") // B32
        // Minimizar o jar final removendo classes não usadas das dependencias sombreadas (opcional, mas recomendado)
        // minimize() - Removido para evitar que o AnvilGUI quebra (NoClassDefFoundError) 
    }
}
