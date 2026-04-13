"""
GorvaxCore — Deploy Script (rebuild_and_deploy.py)
Empacota e deploya os resource packs Java e Bedrock.

USO: python rebuild_and_deploy.py

Automaticamente:
  1. Zipa resourcepack/bedrock/ -> GorvaxCore.mcpack (com manifest.json na raiz!)
  2. Zipa resourcepack/java/   -> GorvaxCore-ResourcePack-Java.zip
  3. Calcula SHA1 do ZIP Java
  4. Copia .mcpack -> plugins/Geyser-Spigot/packs/
  5. Copia geyser_mappings.json -> plugins/Geyser-Spigot/custom_mappings/
  6. Atualiza resource-pack-sha1 no server.properties
"""
import zipfile
import hashlib
import os
import shutil
import re

# === PATHS ===
ROOT = os.path.dirname(os.path.abspath(__file__))
BEDROCK_SRC = os.path.join(ROOT, 'resourcepack', 'bedrock')
JAVA_SRC = os.path.join(ROOT, 'resourcepack', 'java')

SERVER_DIR = r'C:\Users\Gorska\Desktop\Gorvax'
GEYSER_PACKS = os.path.join(SERVER_DIR, 'plugins', 'Geyser-Spigot', 'packs')
GEYSER_MAPPINGS = os.path.join(SERVER_DIR, 'plugins', 'Geyser-Spigot', 'custom_mappings')
SERVER_PROPS = os.path.join(SERVER_DIR, 'server.properties')

BEDROCK_OUT = os.path.join(ROOT, 'GorvaxCore.mcpack')
JAVA_OUT = os.path.join(ROOT, 'GorvaxCore-ResourcePack-Java.zip')

# Files/extensions to exclude from zips
EXCLUDE_EXT = {'.bak', '.tmp', '.py', '.log'}
EXCLUDE_DIRS = {'__pycache__', '.git', 'backup'}


def zip_directory(src_dir, output_path, label):
    """Zip a directory using Python zipfile (reliable, includes all files)."""
    count = 0
    has_manifest = False
    
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        for dirpath, dirnames, filenames in os.walk(src_dir):
            # Skip excluded dirs
            dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
            
            for filename in sorted(filenames):
                ext = os.path.splitext(filename)[1].lower()
                if ext in EXCLUDE_EXT:
                    continue
                
                full_path = os.path.join(dirpath, filename)
                arc_name = os.path.relpath(full_path, src_dir)
                zf.write(full_path, arc_name)
                count += 1
                
                if filename == 'manifest.json' and os.path.dirname(arc_name) == '':
                    has_manifest = True
    
    size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"  [{label}] {count} arquivos -> {os.path.basename(output_path)} ({size_mb:.2f} MB)")
    
    if label == 'Bedrock' and not has_manifest:
        print(f"  [AVISO] manifest.json NAO encontrado na raiz de {src_dir}!")
        print(f"          O pack Bedrock NAO vai funcionar sem manifest.json!")
        return False
    
    return True


def sha1_file(filepath):
    """Calculate SHA1 hash of a file."""
    h = hashlib.sha1()
    with open(filepath, 'rb') as f:
        while True:
            chunk = f.read(8192)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest().upper()


def update_server_properties(sha1):
    """Update resource-pack-sha1 in server.properties."""
    if not os.path.exists(SERVER_PROPS):
        print(f"  [AVISO] server.properties nao encontrado em {SERVER_PROPS}")
        return False
    
    with open(SERVER_PROPS, 'r', encoding='utf-8') as f:
        content = f.read()
    
    old_sha1 = re.search(r'resource-pack-sha1=(\S+)', content)
    if old_sha1:
        old = old_sha1.group(1)
        if old == sha1:
            print(f"  SHA1 ja esta atualizado: {sha1}")
            return True
        content = content.replace(f'resource-pack-sha1={old}', f'resource-pack-sha1={sha1}')
        with open(SERVER_PROPS, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  SHA1 atualizado: {old} -> {sha1}")
        return True
    else:
        print(f"  [AVISO] resource-pack-sha1 nao encontrado no server.properties")
        return False


def main():
    print("=" * 60)
    print("GorvaxCore — Deploy de Resource Packs")
    print("=" * 60)
    
    # 1. Build Bedrock pack
    print("\n[1/5] Empacotando Bedrock...")
    if not os.path.isdir(BEDROCK_SRC):
        print(f"  [ERRO] Diretorio nao encontrado: {BEDROCK_SRC}")
        return
    ok = zip_directory(BEDROCK_SRC, BEDROCK_OUT, 'Bedrock')
    if not ok:
        return
    
    # 2. Build Java pack
    print("\n[2/5] Empacotando Java...")
    if not os.path.isdir(JAVA_SRC):
        print(f"  [ERRO] Diretorio nao encontrado: {JAVA_SRC}")
        return
    zip_directory(JAVA_SRC, JAVA_OUT, 'Java')
    
    # 3. Calculate SHA1
    print("\n[3/5] Calculando SHA1 do Java pack...")
    sha1 = sha1_file(JAVA_OUT)
    print(f"  SHA1: {sha1}")
    
    # 4. Deploy Bedrock to Geyser
    print("\n[4/5] Deploy Bedrock -> Geyser...")
    os.makedirs(GEYSER_PACKS, exist_ok=True)
    dest = os.path.join(GEYSER_PACKS, 'GorvaxCore.mcpack')
    shutil.copy2(BEDROCK_OUT, dest)
    print(f"  Copiado -> {dest}")
    
    # Deploy geyser_mappings if exists
    mappings_src = os.path.join(ROOT, 'geyser_mappings.json')
    if os.path.exists(mappings_src):
        os.makedirs(GEYSER_MAPPINGS, exist_ok=True)
        mappings_dest = os.path.join(GEYSER_MAPPINGS, 'geyser_mappings.json')
        shutil.copy2(mappings_src, mappings_dest)
        print(f"  Mappings -> {mappings_dest}")
    
    # 5. Update server.properties SHA1
    print("\n[5/5] Atualizando server.properties...")
    update_server_properties(sha1)
    
    # Copy Java ZIP to Desktop for GitHub upload
    desktop_zip = os.path.join(os.path.expanduser('~'), 'Desktop', 'GorvaxCore-ResourcePack-Java.zip')
    shutil.copy2(JAVA_OUT, desktop_zip)
    print(f"\n  Java ZIP -> {desktop_zip}")
    
    print("\n" + "=" * 60)
    print("Deploy concluido!")
    print("=" * 60)
    print("\nProximos passos:")
    print("  1. Reiniciar o servidor (ou /gorvax reload + geyser reload)")
    print("  2. Subir Java ZIP no GitHub Releases")
    print(f"     SHA1: {sha1}")
    print("  3. Testar no Bedrock e Java")


if __name__ == '__main__':
    main()
