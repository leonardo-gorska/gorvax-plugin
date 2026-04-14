---
description: como compilar e gerar o .jar do plugin GorvaxCore
---

# Build do GorvaxCore

## Pré-requisitos
- Java 21 instalado e configurado no PATH
- Gradle (wrapper incluído no projeto)

## Passos

// turbo-all

1. Limpar builds anteriores:
```
./gradlew clean
```

2. Compilar e gerar o Shadow JAR (inclui dependências relocadas):
```
./gradlew shadowJar
```

3. O arquivo `.jar` gerado estará em:
```
build/libs/GorvaxCore-2.2-all.jar
```

4. Copie o JAR para a pasta `plugins/` do seu servidor Paper 1.21+.

## Notas
- O Shadow JAR embarca automaticamente o AnvilGUI (relocado para `br.com.gorvax.libs.anvilgui`)
- Para desenvolvimento, use `./gradlew build` para compilação rápida sem Shadow
- Em caso de erro, execute `./gradlew clean build --stacktrace` para debug
