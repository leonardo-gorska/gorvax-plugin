"""
GorvaxCore -- HD Texture Upgrade (64x32 -> 128x64)
Upscale armor textures using Nearest Neighbor and update geometry UV coords.
"""

import json
import os
from PIL import Image

# === Paths ===
BEDROCK_ROOT = os.path.join(os.path.dirname(__file__), "bedrock")
ARMOR_TEX_DIR = os.path.join(BEDROCK_ROOT, "textures", "models", "armor")
GEOMETRY_FILE = os.path.join(BEDROCK_ROOT, "models", "entity", "gorvax_armor.geo.json")

# === Target textures (armor only, shield excluded) ===
ARMOR_TEXTURES = [
    "indrax_crown.png",
    "skulkor_helm.png",
    "speed_boots.png",
    "vulgathor_mantle.png",
    "gorvax_leggings.png",
]

SCALE_FACTOR = 2  # 64x32 -> 128x64


def upscale_textures():
    """Upscale all armor textures from 64x32 to 128x64 using Nearest Neighbor."""
    print("=== Upscaling Armor Textures ===")
    for tex_name in ARMOR_TEXTURES:
        path = os.path.join(ARMOR_TEX_DIR, tex_name)
        img = Image.open(path)
        old_size = img.size
        new_size = (old_size[0] * SCALE_FACTOR, old_size[1] * SCALE_FACTOR)
        img_hd = img.resize(new_size, Image.NEAREST)
        img_hd.save(path)
        print(f"  [OK] {tex_name}: {old_size} -> {new_size}")


def update_geometry_uvs():
    """
    Update gorvax_armor.geo.json:
    - texture_width: 64 -> 128
    - texture_height: 32 -> 64
    - All UV values (positions and sizes) multiplied by 2
    """
    print("\n=== Updating Geometry UV Coords ===")
    with open(GEOMETRY_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    geometries = data.get("minecraft:geometry", [])
    updated_count = 0

    for geo in geometries:
        desc = geo.get("description", {})
        tw = desc.get("texture_width")
        th = desc.get("texture_height")

        # Only upgrade 64x32 geometries (skip shield at 64x64)
        if tw == 64 and th == 32:
            desc["texture_width"] = tw * SCALE_FACTOR
            desc["texture_height"] = th * SCALE_FACTOR
            identifier = desc.get("identifier", "unknown")

            # Recursively double all UV values in all bones/cubes
            bone_count = 0
            cube_count = 0
            for bone in geo.get("bones", []):
                bone_count += 1
                for cube in bone.get("cubes", []):
                    cube_count += 1
                    uv_data = cube.get("uv")
                    if isinstance(uv_data, dict):
                        # Per-face UV format: {"north": {"uv": [x,y], "uv_size": [w,h]}, ...}
                        for face_name, face_uv in uv_data.items():
                            if isinstance(face_uv, dict):
                                if "uv" in face_uv:
                                    face_uv["uv"] = [v * SCALE_FACTOR for v in face_uv["uv"]]
                                if "uv_size" in face_uv:
                                    face_uv["uv_size"] = [v * SCALE_FACTOR for v in face_uv["uv_size"]]
                    elif isinstance(uv_data, list):
                        # Simple UV format: [x, y]
                        cube["uv"] = [v * SCALE_FACTOR for v in uv_data]

            updated_count += 1
            print(f"  [OK] {identifier}: {tw}x{th} -> {tw*SCALE_FACTOR}x{th*SCALE_FACTOR} ({bone_count} bones, {cube_count} cubes)")

    # Write back with same formatting style
    with open(GEOMETRY_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"\n  Total geometries updated: {updated_count}")


def verify():
    """Post-upgrade verification."""
    print("\n=== Verification ===")
    errors = []

    # Check texture dimensions
    for tex_name in ARMOR_TEXTURES:
        path = os.path.join(ARMOR_TEX_DIR, tex_name)
        img = Image.open(path)
        if img.size != (128, 64):
            errors.append(f"FAIL {tex_name}: expected (128, 64), got {img.size}")
        else:
            print(f"  [OK] {tex_name}: {img.size}")

    # Check geometry headers
    with open(GEOMETRY_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)
    for geo in data.get("minecraft:geometry", []):
        desc = geo.get("description", {})
        identifier = desc.get("identifier", "unknown")
        tw = desc.get("texture_width")
        th = desc.get("texture_height")

        # Shield is 64x64 -> should stay unchanged
        if "guardian_shield" in identifier:
            continue

        if tw != 128 or th != 64:
            errors.append(f"FAIL {identifier}: texture_width={tw}, texture_height={th}")
        else:
            print(f"  [OK] {identifier}: {tw}x{th}")

    if errors:
        print("\nERRORS FOUND:")
        for e in errors:
            print(f"  {e}")
    else:
        print("\nAll checks passed!")


if __name__ == "__main__":
    upscale_textures()
    update_geometry_uvs()
    verify()
