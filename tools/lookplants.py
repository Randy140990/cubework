"""
The Retromon Look pack's plants: short grass, tall grass, fern and large fern, and the red flowers (poppies, red
tulips, rose bushes), from John's Shards of Arcadia set
(tools/art/shards_plants: its models, its pictures and its OptiFine random variants), recoloured for Retromon.

His pictures are greys with a lilac cast, made to take the biome's green; here they're put on clean greys (a warm
light at the blade tips, so the tint reads as Let's Go's sunny yellow-green), the grass a touch lighter than the
ferns. His models (several crossed planes at angles, for a full clump) and his variants (picked at random per block,
the tall ones top and bottom together) come over as they are, so no two clumps look alike. They sway in the wind and
part round you as you walk through them: Retromon Light does that (lib/wave.glsl).
"""
import json
import os
import shutil

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "art", "shards_plants")
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft")
CTM = os.path.join(ASSETS, "optifine", "ctm", "retromon", "plants")

GRASS = [(112, 116, 104), (176, 180, 164), (226, 230, 208), (252, 252, 232)]
FERN = [(92, 98, 88), (150, 158, 140), (206, 214, 190), (238, 244, 222)]


def ramp(colours, t):
    seg = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(len(colours) - 2, int(seg))
    k = seg - i
    return tuple(int(round(colours[i][c] + (colours[i + 1][c] - colours[i][c]) * k)) for c in range(3))


def recolour(src, dst, colours):
    """His picture in his pattern, its colours on Retromon's greys by brightness (from its darkest to its lightest)."""
    im = Image.open(src).convert("RGBA")
    p = im.load()
    w, h = im.size
    lum = sorted(0.3 * p[x, y][0] + 0.59 * p[x, y][1] + 0.11 * p[x, y][2] for y in range(h) for x in range(w) if p[x, y][3] > 8)
    if not lum:
        im.save(dst)
        return
    lo, hi = lum[len(lum) // 50], lum[-1 - len(lum) // 50]
    for y in range(h):
        for x in range(w):
            r, g, b, a = p[x, y]
            if not a:
                continue
            t = (0.3 * r + 0.59 * g + 0.11 * b - lo) / max(1.0, hi - lo)
            p[x, y] = ramp(colours, t) + (a,)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    im.save(dst)


# The red flowers (Pallet Town's flower beds and the rose bushes by the houses): his poppies, red tulips and rose
# bushes, their petals put on a clean, bright Let's Go red and their leaves and stems on fresh greens.
PETAL = [(104, 18, 40), (186, 34, 52), (234, 64, 66), (255, 122, 110), (255, 196, 178)]
LEAF = [(28, 66, 36), (48, 102, 46), (78, 140, 58), (122, 180, 80), (170, 212, 112)]
EYE = [(30, 26, 40), (64, 46, 58)]
FLOWERS = ("poppy", "red_tulip", "rose_bush_bottom", "rose_bush_top")


def flower(src, dst):
    """His flower picture in his pattern: each pixel by what it is (a petal, a leaf or stem, a poppy's dark eye), on
    Retromon's colours for it by how light it is among its own kind."""
    import colorsys
    im = Image.open(src).convert("RGBA")
    p = im.load()
    w, h = im.size
    kind = {}
    for y in range(h):
        for x in range(w):
            r, g, b, a = p[x, y]
            if a <= 8:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if vv < 0.22 or (ss < 0.25 and vv < 0.4):
                kind[x, y] = "eye"
            elif (hh < 0.1 or hh > 0.88) and ss > 0.3:
                kind[x, y] = "petal"
            else:
                kind[x, y] = "leaf"
    for k, colours in (("petal", PETAL), ("leaf", LEAF), ("eye", EYE)):
        px = [xy for xy, kk in kind.items() if kk == k]
        if not px:
            continue
        lum = sorted(0.3 * p[xy][0] + 0.59 * p[xy][1] + 0.11 * p[xy][2] for xy in px)
        lo, hi = lum[len(lum) // 40], lum[-1 - len(lum) // 40]
        for xy in px:
            r, g, b, a = p[xy]
            t = (0.3 * r + 0.59 * g + 0.11 * b - lo) / max(1.0, hi - lo)
            p[xy] = ramp(colours, t) + (a,)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    im.save(dst)


# The other flowers on John's map (the pink tulips and blue orchids by Pallet's houses, and the rest along the routes):
# his pictures and random variants too, on the same crossed planes. Their leaves and stems go on the same fresh greens;
# their petals keep his colours, a touch cleaner and brighter. (By the name of their picture, and their variants' folder.)
OTHERS = {"pink_tulip": "tulip/pink", "orange_tulip": "tulip/orange", "white_tulip": "tulip/white", "blue_orchid": "blue_orchid", "allium": "allium",
          "azure_bluet": "azure_bluet", "oxeye_daisy": "oxeye_daisy", "cornflower": "cornflower", "dandelion": "dandelion",
          "lily_of_the_valley": "lily_of_the_valley"}
# The tall ones (two blocks, as the rose bush is).
DOUBLES = ("peony", "lilac")


def flower_kept(src, dst):
    """His flower picture: its leaves and stems on Retromon's greens by how light they are, its petals in his own
    colours, a little more saturated and brighter (as the red flowers are put on a clean bright red)."""
    import colorsys
    im = Image.open(src).convert("RGBA")
    p = im.load()
    w, h = im.size
    leaves = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = p[x, y]
            if a <= 8:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if 0.16 < hh < 0.47 and ss > 0.18 and g >= r:
                leaves.append((x, y))
            else:
                ss2, vv2 = min(1.0, ss * 1.12), min(1.0, vv * 1.1 + 0.03)
                rr, gg, bb = colorsys.hsv_to_rgb(hh, ss2, vv2)
                p[x, y] = (int(rr * 255), int(gg * 255), int(bb * 255), a)
    if leaves:
        lum = sorted(0.3 * p[xy][0] + 0.59 * p[xy][1] + 0.11 * p[xy][2] for xy in leaves)
        lo, hi = lum[len(lum) // 40], lum[-1 - len(lum) // 40]
        for xy in leaves:
            r, g, b, a = p[xy]
            t = (0.3 * r + 0.59 * g + 0.11 * b - lo) / max(1.0, hi - lo)
            p[xy] = ramp(LEAF, t) + (a,)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    im.save(dst)


def build():
    if os.path.isdir(CTM):
        shutil.rmtree(CTM)
    tex = os.path.join(ASSETS, "textures", "block")
    models = os.path.join(ASSETS, "models", "block")
    for name in FLOWERS:
        flower(os.path.join(SRC, "textures", "block", name + ".png"), os.path.join(tex, name + ".png"))
        shutil.copy(os.path.join(SRC, "models", "block", name + ".json"), os.path.join(models, name + ".json"))
    for name in OTHERS:
        flower_kept(os.path.join(SRC, "textures", "block", name + ".png"), os.path.join(tex, name + ".png"))
        json.dump({"parent": "minecraft:block/cross4", "textures": {"cross": "minecraft:block/" + name}}, open(os.path.join(models, name + ".json"), "w"), indent=1)
    for name in DOUBLES:
        for half, shape in (("bottom", "flower_bottom"), ("top", "flower_top")):
            flower_kept(os.path.join(SRC, "textures", "block", "%s_%s.png" % (name, half)), os.path.join(tex, "%s_%s.png" % (name, half)))
            json.dump({"parent": "minecraft:block/" + shape, "textures": {"cross": "minecraft:block/%s_%s" % (name, half)}},
                      open(os.path.join(models, "%s_%s.json" % (name, half)), "w"), indent=1)
    # The shapes his flower models are made on (several crossed planes for a fuller plant).
    for name in ("cross2", "cross4", "flower_bottom", "flower_top"):
        shutil.copy(os.path.join(SRC, "models", "block", name + ".json"), os.path.join(models, name + ".json"))
    root = os.path.join(SRC, "optifine", "ctm", "test", "flower")
    for dirpath, _dirs, files in os.walk(root):
        out = os.path.join(CTM, "flower", os.path.relpath(dirpath, root))
        os.makedirs(out, exist_ok=True)
        rel = os.path.relpath(dirpath, root).replace(os.sep, "/")
        red = rel.split("/")[0] in ("poppy", "rose_bush") or rel == "tulip/red"
        for f in files:
            if f.endswith(".png"):
                (flower if red else flower_kept)(os.path.join(dirpath, f), os.path.join(out, f))
            elif f.endswith(".properties"):
                shutil.copy(os.path.join(dirpath, f), os.path.join(out, f))
    grasses()
    petals()


def petals():
    """Pink petals (Viridian City's, and wherever else they are) as little pink flowers standing up, swaying as the
    other flowers do, not petals lying flat: in each of the four corners of the block they fill (one to four, as
    Minecraft's), a small tuft of three blossoms on green stems on two crossed planes."""
    from PIL import Image
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    stem, leaf = (63, 138, 46, 255), (92, 168, 60, 255)
    petal, petal_dark, centre = (244, 158, 194, 255), (226, 122, 168, 255), (255, 231, 160, 255)
    for x, y0 in ((5, 12), (8, 11), (10, 13)):
        for y in range(y0, 16):
            im.putpixel((x, y), stem)
    for x, y in ((6, 14), (9, 14), (4, 15), (11, 15)):
        im.putpixel((x, y), leaf)
    for cx, cy in ((5, 11), (8, 10), (10, 12)):
        for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
            im.putpixel((cx + dx, cy + dy), petal)
        for dx, dy in ((-1, -1), (1, 1)):
            if 0 <= cx + dx < 16 and 0 <= cy + dy < 16 and im.getpixel((cx + dx, cy + dy))[3] == 0:
                im.putpixel((cx + dx, cy + dy), petal_dark)
        im.putpixel((cx, cy), centre)
    im.save(os.path.join(ASSETS, "textures", "block", "pink_petals.png"))
    models = os.path.join(ASSETS, "models", "block")
    for n, (x0, z0) in enumerate(((0, 0), (0, 8), (8, 8), (8, 0)), start=1):
        cx, cz = x0 + 4, z0 + 4
        face = {"uv": [4, 8, 12, 16], "texture": "#cross"}
        back = {"uv": [12, 8, 4, 16], "texture": "#cross"}
        rot = {"angle": 45, "axis": "y", "origin": [cx, 4, cz]}
        els = [{"from": [cx - 3.5, 0, cz], "to": [cx + 3.5, 7, cz], "shade": False, "rotation": rot, "faces": {"north": face, "south": back}},
               {"from": [cx, 0, cz - 3.5], "to": [cx, 7, cz + 3.5], "shade": False, "rotation": rot, "faces": {"east": face, "west": back}}]
        json.dump({"ambientocclusion": False, "textures": {"particle": "minecraft:block/pink_petals", "cross": "minecraft:block/pink_petals"}, "elements": els},
                  open(os.path.join(models, "pink_petals_%d.json" % n), "w"), indent=1)


def grasses():
    tex = os.path.join(ASSETS, "textures", "block")
    for name in ("short_grass", "tall_grass_bottom", "tall_grass_top", "fern", "large_fern_bottom", "large_fern_top"):
        recolour(os.path.join(SRC, "textures", "block", name + ".png"), os.path.join(tex, name + ".png"), FERN if "fern" in name else GRASS)
        os.makedirs(os.path.join(ASSETS, "models", "block"), exist_ok=True)
        shutil.copy(os.path.join(SRC, "models", "block", name + ".json"), os.path.join(ASSETS, "models", "block", name + ".json"))
    for name in ("large_fern_bottom_extra", "large_fern_top_extra"):
        recolour(os.path.join(SRC, "textures", "block", "extra", name + ".png"), os.path.join(tex, "extra", name + ".png"), FERN)
    # His random variants (with their properties as he wrote them), under our own folder.
    for sub, kind in (("grass/plant", GRASS), ("fern", FERN)):
        root = os.path.join(SRC, "optifine", "ctm", "test", sub)
        for dirpath, _dirs, files in os.walk(root):
            out = os.path.join(CTM, sub.split("/")[0], os.path.relpath(dirpath, root))
            os.makedirs(out, exist_ok=True)
            for f in files:
                if f.endswith(".png"):
                    recolour(os.path.join(dirpath, f), os.path.join(out, f), kind)
                elif f.endswith(".properties"):
                    shutil.copy(os.path.join(dirpath, f), os.path.join(out, f))


if __name__ == "__main__":
    build()
    print("plants written")
