"""
0.73's pictures (John's 28 Nov 09:22 save: the MAGNET TRAIN's station copied into Saffron's north-west corner, "just
probably wants its own style to feel Kanto versus Johto ... a different roof colour"):

- saf_dome: the Saffron station's curved roof in Saffron's green (its flat roofs' green, saf_roof), laid over four
  blocks (@4) so it reads as one smooth shell: long panels with fine light seams, a soft sheen across them.
- saf_emblem: the station's badge over its doorway (Goldenrod's copper one, gold_emblem) in Saffron's green, its
  shine kept.

Writes textures/block/area/saf_dome.png and saf_emblem.png.
"""
import colorsys
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookarea as L  # noqa: E402

GREEN = (98, 168, 93)


def shade(c, k):
    return tuple(max(0, min(255, round(v * (1 + k)))) for v in c) + (255,)


def dome(size=64):
    rnd = random.Random(73)
    im = Image.new("RGBA", (size, size))
    px = im.load()
    for y in range(size):
        for x in range(size):
            k = (rnd.random() - 0.5) * 0.035
            # A soft sheen running across the panels (wraps, so the four-block picture joins its neighbours).
            k += 0.035 * math.cos((x + y) / size * 2 * math.pi)
            # The panels' seams: a fine light line every two blocks one way, a fainter one every block the other.
            if x % 32 == 0: k += 0.16
            elif x % 32 == 1: k -= 0.06
            if y % 16 == 0: k += 0.06
            px[x, y] = shade(GREEN, k)
    return im


def emblem():
    src = Image.open(os.path.join(L.TEX, "gold_emblem.png")).convert("RGBA")
    out = Image.new("RGBA", src.size)
    sp, op = src.load(), out.load()
    # Goldenrod's copper orange (hue about 20 degrees) turned to Saffron's green (about 115), its light and dark kept.
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = sp[x, y]
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            h = (h + 95 / 360) % 1.0
            s = min(1.0, s * 0.8)
            nr, ng, nb = colorsys.hls_to_rgb(h, l, s)
            op[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def main():
    L.save("saf_dome", dome())
    L.save("saf_emblem", emblem())


if __name__ == "__main__":
    main()
