#!/usr/bin/env python3
"""The pictures taken from John's asset pack (textures.zip, Shards of Arcadia's) for the rooms' plants and pots and the
Look pack's cobblestone and leaf litter. Writes tools/art/shards_props/*.png, which the Interior and UI viewers build
in (tools/interior/tex.js packs them as PACKTEX) and tools/lookground.py recolours.

    python3 tools/propsart.py <textures.zip>
"""
import io, os, sys, zipfile
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "art", "shards_props")


def shrink(im, size, cut=False):
    """Down to size (alpha weighted, so no dark fringes); cut: every pixel either there or not."""
    im = im.convert("RGBa").resize((size, size), Image.LANCZOS).convert("RGBA")
    if cut:
        px = im.load()
        for y in range(size):
            for x in range(size):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 255 if a >= 128 else 0)
    return im


def over(im, bg):
    """im laid over a solid colour (a leafy cube's inside, so no gaps show through)."""
    base = Image.new("RGBA", im.size, bg + (255,))
    base.alpha_composite(im)
    return base


def tint(im, c):
    """A grey picture (tinted in game) in a colour: its brightness times c."""
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            l = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            px[x, y] = (min(255, int(c[0] * l * 1.25)), min(255, int(c[1] * l * 1.25)), min(255, int(c[2] * l * 1.25)), a)
    return im


def ramp(im, cols):
    """A picture's brightness put on a ramp of colours (dark to light)."""
    px = im.load()
    lum = [0.3 * px[x, y][0] + 0.59 * px[x, y][1] + 0.11 * px[x, y][2] for y in range(im.height) for x in range(im.width) if px[x, y][3]]
    lo, hi = min(lum), max(lum)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a: continue
            t = (0.3 * r + 0.59 * g + 0.11 * b - lo) / max(1.0, hi - lo)
            s = t * (len(cols) - 1); i = min(len(cols) - 2, int(s)); f = s - i
            c = [cols[i][k] + (cols[i + 1][k] - cols[i][k]) * f for k in range(3)]
            px[x, y] = (int(c[0]), int(c[1]), int(c[2]), a)
    return im


def main():
    z = zipfile.ZipFile(sys.argv[1])
    get = lambda n: Image.open(io.BytesIO(z.read("textures/block/" + n + ".png"))).convert("RGBA")
    os.makedirs(OUT, exist_ok=True)
    save = lambda n, im: im.save(os.path.join(OUT, n + ".png"))
    # Leafy bushes: the azalea's leaves (sprigs, gaps between), and a solid copy over a deep green for their insides.
    save("azalea_leaves", shrink(get("azalea_leaves"), 64, cut=True))
    save("azalea_leaves_solid", over(shrink(get("azalea_leaves"), 64, cut=True), (34, 74, 30)))
    save("flowering_azalea_leaves", shrink(get("flowering_azalea_leaves"), 64, cut=True))
    save("flowering_azalea_leaves_solid", over(shrink(get("flowering_azalea_leaves"), 64, cut=True), (34, 74, 30)))
    # A potted azalea's crossed sprigs, a fern (grey in the pack, the leaves' green here) and a big leaf.
    save("azalea_plant", shrink(get("azalea_plant"), 64, cut=True))
    save("fern", tint(shrink(get("fern"), 64, cut=True), (92, 168, 60)))
    save("large_fern_top", tint(shrink(get("large_fern_top"), 64, cut=True), (92, 168, 60)))
    save("big_leaf", shrink(get("big_dripleaf_top"), 64))
    # The flower pot's side (its smooth terracotta), and a white glazed one in the same shading.
    pot = get("flower_pot").crop((88, 0, 128, 128)).resize((20, 64), Image.LANCZOS)
    save("pot", pot)
    save("pot_white", ramp(pot.copy(), [(176, 186, 192), (214, 222, 226), (236, 241, 243), (250, 252, 252)]))
    # The Look pack's: the cobbles and the leaf litter, kept at the pack's size (recoloured in lookground.py).
    for n in ("cobblestone", "mossy_cobblestone", "leaf_litter"):
        save(n, get(n))
    print("wrote", sorted(os.listdir(OUT)))


if __name__ == "__main__":
    main()
