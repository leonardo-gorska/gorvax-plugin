#!/bin/bash
# ============================================================
# GorvaxCore — Script de empacotamento de Resource Packs
# Gera os .zip prontos para deploy (Java + Bedrock)
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "[GorvaxCore] Empacotando resource packs..."

# --- Java Resource Pack ---
echo "[Java] Criando GorvaxCore-ResourcePack-Java.zip ..."
cd "$SCRIPT_DIR/java"
rm -f "$SCRIPT_DIR/GorvaxCore-ResourcePack-Java.zip"
zip -r "$SCRIPT_DIR/GorvaxCore-ResourcePack-Java.zip" . -x "*.DS_Store"
echo "[Java] Concluido!"

# --- Bedrock Resource Pack ---
echo "[Bedrock] Criando GorvaxCore-ResourcePack-Bedrock.zip ..."
cd "$SCRIPT_DIR/bedrock"
rm -f "$SCRIPT_DIR/GorvaxCore-ResourcePack-Bedrock.zip"
zip -r "$SCRIPT_DIR/GorvaxCore-ResourcePack-Bedrock.zip" . -x "*.DS_Store"
echo "[Bedrock] Concluido!"

echo ""
echo "[GorvaxCore] Resource packs gerados com sucesso!"
echo "  - GorvaxCore-ResourcePack-Java.zip"
echo "  - GorvaxCore-ResourcePack-Bedrock.zip"
echo ""
echo "Proximo passo: suba os .zip para GitHub e configure server.properties"
