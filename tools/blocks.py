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
# Joined up like glass panes: each side of a strip knows whether another strip of its colour is there. A
# face gets the coloured line (the outermost pixel in the colour, the next one between) only along its
# edges where nothing joins, so the line runs round the outside of a whole run or panel of strips.
# Which world direction each edge of each face's texture looks towards (Minecraft's own face UVs):
EDGES = {  # face: (top, bottom, left, right)
    "north": ("up", "down", "east", "west"),
    "south": ("up", "down", "west", "east"),
    "west": ("up", "down", "north", "south"),
    "east": ("up", "down", "south", "north"),
    "up": ("north", "south", "west", "east"),
    "down": ("south", "north", "west", "east"),
}
DIRS = ["down", "east", "north", "south", "up", "west"]
LIGHTS = {
    "green": (40, 255, 96),
    "red": (255, 40, 56),
    "white": (150, 205, 255),
}
import itertools, glob
for name, c in LIGHTS.items():
    block = f"light_strip_{name}"
    for old in glob.glob(f"{A}/textures/block/{block}*.png") + glob.glob(f"{A}/models/block/{block}*.json"):
        os.remove(old)
    core = mix((255, 255, 255), c, 0.07)
    # A texture for each set of edges with a line (top 1, bottom 2, left 4, right 8).
    for mask in range(16):
        img = Image.new("RGB", (16, 16))
        for x in range(16):
            for y in range(16):
                d = 99
                if mask & 1: d = min(d, y)
                if mask & 2: d = min(d, 15 - y)
                if mask & 4: d = min(d, x)
                if mask & 8: d = min(d, 15 - x)
                img.putpixel((x, y), c if d == 0 else mix(c, (255, 255, 255), 0.5) if d == 1 else core)
        img.save(f"{A}/textures/block/{block}_{mask}.png")
    variants = {}
    for joins in itertools.product([False, True], repeat=6):
        j = dict(zip(DIRS, joins))
        tex = {}
        for face, (t, bo, l, r) in EDGES.items():
            mask = (0 if j[t] else 1) | (0 if j[bo] else 2) | (0 if j[l] else 4) | (0 if j[r] else 8)
            tex[face] = f"orbitarena:block/{block}_{mask}"
        tex["particle"] = f"orbitarena:block/{block}_15"
        key = "".join("1" if j[d] else "0" for d in DIRS)
        write(f"{A}/models/block/{block}_{key}.json", {"parent": "minecraft:block/cube", "textures": tex})
        variants[",".join(f"{d}={'true' if j[d] else 'false'}" for d in DIRS)] = {"model": f"orbitarena:block/{block}_{key}"}
    write(f"{A}/blockstates/{block}.json", {"variants": variants})
    write(f"{A}/items/{block}.json", {"model": {"type": "minecraft:model", "model": f"orbitarena:block/{block}_000000"}})

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
# A clean glowing wash (no lines in it), with only a faint drifting shimmer, so a wall of them reads as one
# sheet. Fade 0 is the full field; fades 1 to 12 each run from the strength at their foot to a little less at
# their top, so a column 0 (or 1), 2, ..., 12 fades smoothly out to nothing at its top. The wall in front of
# the stands is much fainter than the goals' fields, so you can see the crowd through it.
import math
FIELDS = {"blue": ((70, 140, 255), 0.42), "red": ((255, 64, 76), 0.42), "wall": ((150, 215, 255), 0.13)}
FADES = 13
STEPS = FADES - 1


def strength(k):
    return max(0.0, 1 - k / STEPS) ** 1.3


for name, (c, base) in FIELDS.items():
    block = f"force_field_{name}"
    for old in glob.glob(f"{A}/textures/block/{block}*.png") + glob.glob(f"{A}/models/block/{block}*.json"):
        os.remove(old)
    variants = {}
    for k in range(FADES):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            up = (15 - y + 0.5) / 16
            f = strength(0) if k == 0 else strength(k - 1) + (strength(k) - strength(k - 1)) * up
            for x in range(16):
                shimmer = 0.5 + 0.5 * math.sin((x + y * 0.5) * math.pi / 8) * math.sin((x * 0.5 - y) * math.pi / 8)
                col = mix(c, (255, 255, 255), 0.18 + 0.12 * shimmer)
                alpha = 255 * base * (0.9 + 0.1 * shimmer) * f
                img.putpixel((x, y), col + (max(0, min(255, int(round(alpha)))),))
        img.save(f"{A}/textures/block/{block}_{k}.png")
        write(f"{A}/models/block/{block}_{k}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"orbitarena:block/{block}_{k}"}})
        variants[f"fade={k}"] = {"model": f"orbitarena:block/{block}_{k}"}
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
