"""
The Retromon Look pack's plants: short grass, tall grass, fern and large fern, from John's Shards of Arcadia set
(tools/art/shards_plants: its models, its pictures and its OptiFine random variants), recoloured for Retromon.

His pictures are greys with a lilac cast, made to take the biome's green; here they're put on clean greys (a warm
light at the blade tips, so the tint reads as Let's Go's sunny yellow-green), the grass a touch lighter than the
ferns. His models (several crossed planes at angles, for a full clump) and his variants (picked at random per block,
the tall ones top and bottom together) come over as they are, so no two clumps look alike. They sway in the wind and
part round you as you walk through them: Retromon Light does that (lib/wave.glsl).
"""
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


def build():
    if os.path.isdir(CTM):
        shutil.rmtree(CTM)
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
