@echo off
REM ============================================================
REM GorvaxCore — Script de empacotamento de Resource Packs
REM Gera os .zip prontos para deploy (Java + Bedrock)
REM ============================================================

echo [GorvaxCore] Empacotando resource packs...

REM --- Java Resource Pack ---
echo [Java] Criando GorvaxCore-ResourcePack-Java.zip ...
cd /d "%~dp0java"
if exist "..\GorvaxCore-ResourcePack-Java.zip" del "..\GorvaxCore-ResourcePack-Java.zip"
powershell -Command "Compress-Archive -Path '.\*' -DestinationPath '..\GorvaxCore-ResourcePack-Java.zip' -Force"
cd /d "%~dp0"
echo [Java] Concluido!

REM --- Bedrock Resource Pack ---
echo [Bedrock] Criando GorvaxCore-ResourcePack-Bedrock.zip ...
cd /d "%~dp0bedrock"
if exist "..\GorvaxCore-ResourcePack-Bedrock.zip" del "..\GorvaxCore-ResourcePack-Bedrock.zip"
powershell -Command "Compress-Archive -Path '.\*' -DestinationPath '..\GorvaxCore-ResourcePack-Bedrock.zip' -Force"
cd /d "%~dp0"
echo [Bedrock] Concluido!

echo.
echo [GorvaxCore] Resource packs gerados com sucesso!
echo   - GorvaxCore-ResourcePack-Java.zip
echo   - GorvaxCore-ResourcePack-Bedrock.zip
echo.
echo Proximo passo: suba os .zip para GitHub e configure server.properties
pause
