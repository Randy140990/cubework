"""Makes the arena's own blocks: textures (16 x 16), block states, models, item models, the pickaxe tag and
the names. Run from the repository's root: python3 tools/blocks.py"""
import json
import os
import random

from PIL import Image

ROOT = "src/main/resources"
A = ROOT + "/assets/orbitarena"
os.makedirs(A + "/textures/block", exist_ok=True)
os.makedirs(A + "/blockstates", exist_ok=True)
os.makedirs(A + "/models/block", exist_ok=True)
os.makedirs(A + "/items", exist_ok=True)
os.makedirs(ROOT + "/data/minecraft/tags/block/mineable", exist_ok=True)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def write(path, data):
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


# ---------------------------------------------------------------- light strips
# The side: a white core, the outermost line of pixels each side in the colour and the next one between.
# Laid along x or z (lying down), those lines run along the top and bottom; stood up, down the sides.
LIGHTS = {
    "green": (40, 255, 96),
    "red": (255, 40, 56),
    "white": (150, 205, 255),
}
for name, c in LIGHTS.items():
    core = mix((255, 255, 255), c, 0.07)
    side = Image.new("RGB", (16, 16))
    for x in range(16):
        edge = min(x, 15 - x)
        col = c if edge == 0 else mix(c, (255, 255, 255), 0.5) if edge == 1 else core
        for y in range(16):
            side.putpixel((x, y), col)
    side.save(f"{A}/textures/block/light_strip_{name}.png")
    end = Image.new("RGB", (16, 16))
    for x in range(16):
        for y in range(16):
            edge = min(x, 15 - x, y, 15 - y)
            end.putpixel((x, y), c if edge == 0 else mix(c, (255, 255, 255), 0.5) if edge == 1 else core)
    end.save(f"{A}/textures/block/light_strip_{name}_end.png")
    block = f"light_strip_{name}"
    tex = {"end": f"orbitarena:block/{block}_end", "side": f"orbitarena:block/{block}"}
    write(f"{A}/models/block/{block}.json", {"parent": "minecraft:block/cube_column", "textures": tex})
    write(f"{A}/models/block/{block}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": tex})
    write(f"{A}/blockstates/{block}.json", {"variants": {
        "axis=x": {"model": f"orbitarena:block/{block}_horizontal", "x": 90, "y": 90},
        "axis=y": {"model": f"orbitarena:block/{block}"},
        "axis=z": {"model": f"orbitarena:block/{block}_horizontal", "x": 90},
    }})
    write(f"{A}/items/{block}.json", {"model": {"type": "minecraft:model", "model": f"orbitarena:block/{block}"}})

# ---------------------------------------------------------------- bridge wool
# Knitted: rows of little V stitches, each a lighter side and a darker side, with a soft fleck.
WOOLS = {"blue": (46, 74, 206), "red": (200, 38, 42)}
for name, c in WOOLS.items():
    rnd = random.Random(name)
    img = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            sx, sy = x % 4, y % 4
            row = (y // 4) % 2
            # A stitch: two slanted halves meeting in the middle of the cell.
            left = sx < 2
            slant = (sy - (sx if left else 3 - sx)) % 4
            light = 1.0 + (0.10 if left else -0.06) - 0.07 * slant + (0.03 if row else 0)
            if sy == 3 and (sx == 0 or sx == 3):
                light -= 0.12
            light += rnd.uniform(-0.035, 0.035)
            img.putpixel((x, y), tuple(max(0, min(255, int(v * light))) for v in c))
    img.save(f"{A}/textures/block/bridge_wool_{name}.png")
    block = f"bridge_wool_{name}"
    write(f"{A}/models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"orbitarena:block/{block}"}})
    write(f"{A}/blockstates/{block}.json", {"variants": {"": {"model": f"orbitarena:block/{block}"}}})
    write(f"{A}/items/{block}.json", {"model": {"type": "minecraft:model", "model": f"orbitarena:block/{block}"}})

# ---------------------------------------------------------------- force fields
# A soft glowing wash with faint scan lines and a shimmer, all of it tiling, so a wall of them reads as
# one sheet (no lines round each block). Fade 0 is the full field; fades 1 to 5 each run from the one
# below's strength at their foot to their own at their top, so a column 0, 1, ..., 5 fades out smoothly.
import math
FIELDS = {"blue": (70, 140, 255), "red": (255, 64, 76), "wall": (150, 215, 255)}
FADES = 6


def strength(k):
    return (1 - k / (FADES - 1)) ** 1.25


for name, c in FIELDS.items():
    block = f"force_field_{name}"
    variants = {}
    for k in range(FADES):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            # How strong the field is at this row (y 0 is the top of the block).
            up = (15 - y + 0.5) / 16
            f = strength(0) if k == 0 else strength(k - 1) + (strength(k) - strength(k - 1)) * up
            for x in range(16):
                shimmer = 0.5 + 0.25 * math.sin((x + y * 0.5) * math.pi / 4) + 0.25 * math.sin((x * 0.5 - y) * math.pi / 8)
                scan = 1.0 if y % 4 == 0 else 0.0
                col = mix(c, (255, 255, 255), 0.12 + 0.3 * scan + 0.1 * shimmer)
                alpha = (70 + 70 * scan + 26 * shimmer) * f
                img.putpixel((x, y), col + (max(0, min(255, int(round(alpha)))),))
        img.save(f"{A}/textures/block/{block}_{k}.png")
        write(f"{A}/models/block/{block}_{k}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"orbitarena:block/{block}_{k}"}})
        variants[f"fade={k}"] = {"model": f"orbitarena:block/{block}_{k}"}
    if os.path.exists(f"{A}/textures/block/{block}.png"):
        os.remove(f"{A}/textures/block/{block}.png")
    if os.path.exists(f"{A}/models/block/{block}.json"):
        os.remove(f"{A}/models/block/{block}.json")
    write(f"{A}/blockstates/{block}.json", {"variants": variants})
    write(f"{A}/items/{block}.json", {"model": {"type": "minecraft:model", "model": f"orbitarena:block/{block}_0"}})

write(ROOT + "/data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [
    "orbitarena:bridge_wool_blue", "orbitarena:bridge_wool_red", "orbitarena:light_strip_green", "orbitarena:light_strip_red",
    "orbitarena:light_strip_white"]})

# The names.
lang_path = A + "/lang/en_us.json"
lang = json.load(open(lang_path))
lang.update({
    "block.orbitarena.light_strip_green": "Green Light Strip",
    "block.orbitarena.light_strip_red": "Red Light Strip",
    "block.orbitarena.light_strip_white": "White Light Strip",
    "block.orbitarena.bridge_wool_blue": "Blue Bridge Wool",
    "block.orbitarena.bridge_wool_red": "Red Bridge Wool",
    "block.orbitarena.force_field_blue": "Blue Force Field",
    "block.orbitarena.force_field_red": "Red Force Field",
    "block.orbitarena.force_field_wall": "Wall Force Field",
    "itemGroup.orbitarena.blocks": "Orbit Arena",
    "entity.orbitarena.rock": "Orbiting Rock",
})
write(lang_path, lang)
print("done")
