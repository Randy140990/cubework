"""
The Retromon Look pack (built into the mod, switched on in Resource Packs): a first set of Pokémon-style
16x16 textures for the outdoors. Grass, tall grass and leaves are drawn in greys, as Minecraft's are, so
the biome's green (made brighter by Retromon) colours them; flowers, paths and sand keep their own colours.
"""
import json
import os
import random

import lookground
import lookplants

from PIL import Image

PACK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "resourcepacks", "retromon_look")
TEX = os.path.join(PACK, "assets", "minecraft", "textures", "block")


def grey(v, a=255):
    v = max(0, min(255, int(v)))
    return (v, v, v, a)


def save(name, im):
    im.save(os.path.join(TEX, name + ".png"))


def grass_top():
    """Soft ground with small regular tufts, as the games' grass tiles."""
    rnd = random.Random(1)
    im = Image.new("RGBA", (16, 16))
    p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = grey(196 + rnd.randint(-6, 6))
    for ox, oy in ((2, 1), (10, 1), (6, 5), (14, 5), (1, 8), (9, 8), (5, 11), (13, 11), (2, 14), (10, 14)):
        for dx, dy, v in ((0, 0, 238), (-1, 1, 228), (1, 1, 228), (0, 1, 160)):
            x, y = (ox + dx) % 16, (oy + dy) % 16
            p[x, y] = grey(v)
    return im


def tuft(tall_top=False, tall_bottom=False):
    """The wild grass you meet Pokémon in: a row of pointed blades, outlined, with light streaks."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    rnd = random.Random(2 + tall_top * 3 + tall_bottom * 7)
    # Height of the grass at each column: points every 4 pixels.
    top = []
    for x in range(16):
        k = x % 4
        h = [3, 1, 3, 5][k]
        top.append(16 if tall_bottom else h + (6 if not tall_top else 2))
    if tall_bottom:
        top = [0] * 16
    for x in range(16):
        for y in range(top[x], 16):
            v = 200 + rnd.randint(-8, 8)
            if x % 4 == 1 and (y - top[x]) < 6:
                v = 240
            p[x, y] = grey(v)
    # Dark outline along the tips and between the blades.
    for x in range(16):
        y = top[x]
        if y < 16:
            p[x, y] = grey(110)
        if x % 4 == 3 and y + 1 < 16:
            for yy in range(y, min(16, y + 4)):
                p[x, yy] = grey(130)
    return im


def leaves():
    """Rounded puffs of leaves with a light top and a dark rim, as the games' trees."""
    im = Image.new("RGBA", (16, 16))
    p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = grey(150)
    for cx, cy in ((4, 4), (12, 4), (8, 10), (0, 10), (16, 10), (4, 16), (12, 16), (4, 0), (12, 0)):
        for y in range(16):
            for x in range(16):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d < 3.6:
                    v = 205
                    if (x - cx) + (y - cy) < -2.2:
                        v = 240
                    elif (x - cx) + (y - cy) > 2.4:
                        v = 165
                    p[x, y] = grey(v)
                elif d < 4.3 and (y - cy) > 0:
                    p[x, y] = grey(110)
    return im


def flower(petal, centre, stem=(70, 150, 60, 255)):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    for y in range(9, 16):
        p[7, y] = stem
    p[6, 12] = p[5, 11] = stem
    p[8, 13] = p[9, 12] = stem
    for dx, dy in ((0, -2), (-2, 0), (2, 0), (0, 2), (-1, -1), (1, -1), (-1, 1), (1, 1), (0, -1), (-1, 0), (1, 0), (0, 1)):
        p[7 + dx, 6 + dy] = petal
    p[7, 6] = centre
    edge = tuple(max(0, c - 60) for c in petal[:3]) + (255,)
    for dx, dy in ((0, -3), (-3, 0), (3, 0), (0, 3)):
        p[7 + dx, 6 + dy] = edge
    return im


def speckled(base, dark, light, seed):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = tuple(max(0, min(255, c + rnd.randint(-5, 5))) for c in base) + (255,)
    for ox, oy in ((2, 2), (9, 3), (13, 9), (5, 8), (1, 13), (10, 13), (6, 15)):
        p[ox, oy] = dark + (255,)
        p[(ox + 1) % 16, oy] = light + (255,)
    return im


def side_overlay():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    for x in range(16):
        h = 3 + (1 if x % 4 in (1, 2) else 0)
        for y in range(h):
            p[x, y] = grey(200 if y < h - 1 else 150)
    return im


def main():
    os.makedirs(TEX, exist_ok=True)
    json.dump({"pack": {"description": "Retromon Look: Pokémon-style grass, moss, leaves, flowers and paths", "pack_format": 75, "min_format": 75, "max_format": 75}},
              open(os.path.join(PACK, "pack.mcmeta"), "w"), indent=1, ensure_ascii=False)
    # The ground (grass, dirt paths, moss, dirt) and its rounded edges (CTM, for Continuity): lookground.py.
    lookground.build(save)
    # The plants (short and tall grass, fern, large fern, the red flowers): John's Shards of Arcadia set, recoloured:
    # lookplants.py.
    lookplants.build()
    # (The trees keep Minecraft's leaves, as the map has them; only Kanto's colours are brighter: lookground.py and
    # client/Colours.java. Earlier versions' own leaves are gone.)
    for wood in ("oak", "jungle", "acacia", "dark_oak", "mangrove"):
        f = os.path.join(TEX, wood + "_leaves.png")
        if os.path.exists(f):
            os.remove(f)
    # (The poppy is John's now, with the other red flowers: lookplants.py.)
    save("dandelion", flower((250, 215, 60, 255), (240, 140, 40, 255)))
    save("azure_bluet", flower((245, 245, 250, 255), (250, 220, 90, 255)))
    save("sand", speckled((240, 226, 172), (214, 196, 140), (250, 242, 205), 4))
    print("look pack written")


if __name__ == "__main__":
    main()
