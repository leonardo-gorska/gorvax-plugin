@echo off
echo Iniciando Build Manual...
set GRADLE_PATH="c:\Users\Gorska\.gradle\wrapper\dists\gradle-8.11-bin\c4te04g51qsyw1bxcb929u7br\gradle-8.11\bin\gradle.bat"

if exist %GRADLE_PATH% (
    echo Usando Gradle em %GRADLE_PATH%
    call %GRADLE_PATH% assemble --no-daemon --refresh-dependencies
) else (
    echo Gradle fixo nao encontrado, tentando via PATH...
    call gradle assemble --no-daemon --refresh-dependencies
)

if %ERRORLEVEL% NEQ 0 (
    echo [ERRO] Falha na compilacao!
    pause
    exit /b %ERRORLEVEL%
)

echo Verificando resultado...
dir build\libs /s /b > build_output.txt
echo DONE >> build_output.txt
