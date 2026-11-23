"""
0.51.0's looks (John's 09:01 to 09:30 rounds), the pictures for tools/arealooks.py's round51() and the look pack:

- Route 1's cobblestone and mossy cobblestone (the pack's, so everywhere they aren't styled otherwise) in Retromon's
  own flat style: rounded stones, each lit along its top and left with a soft shadow below, dark gaps; moss in the gaps.
- Pewter's yellow doors (John's bamboo doors), a style of their own in the doors' family.
- Lily pads, Let's Go's: a round leaf with its notch, a lit rim, veins, a little pink bloom.
- The route gates' doorways (their black panels) one solid dark, no faded outline.
- The benches' frame (the swing's hanging sides) the bench's own yellow.

Run: called from tools/lookpack.py (looks48.EXTRA).
"""

import math
import os
import random

from PIL import Image

import lookarea as L
import looks46 as K
import looks48b as B
import looks50 as F

hexc, mix, save = L.hexc, L.mix, L.save
shade = B.shade
PACK_TEX = os.path.join(L.HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft", "textures", "block")


def _cells(n, seed, size=16):
    r = random.Random(seed)
    pts = []
    for i in range(n):
        pts.append((r.uniform(0, size), r.uniform(0, size), r.random()))
    return pts


def cobbles(base, gap, seed, moss=None, size=16, n=9):
    """Rounded cobbles (a Voronoi of n stones, tiling): each stone its own shade, lit along its upper left and shadowed
    along its lower right, a dark gap between; moss (if given) in the gaps and creeping a little onto the stones."""
    pts = _cells(n, seed, size)
    r = random.Random(seed + "g")
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            best, second, k, bx, by = 1e9, 1e9, 0.5, 0, 0
            for (px, py, kk) in pts:
                for ox in (-size, 0, size):
                    for oy in (-size, 0, size):
                        d = (x + 0.5 - px - ox) ** 2 + (y + 0.5 - py - oy) ** 2
                        if d < best:
                            second, best, k, bx, by = best, d, kk, px + ox, py + oy
                        elif d < second:
                            second = d
            edge = second ** 0.5 - best ** 0.5
            c = shade(base, (k - 0.5) * 0.12)
            # Lit towards the upper left of each stone, shadowed towards its lower right.
            lit = ((bx - x - 0.5) + (by - y - 0.5)) / 6.0
            c = shade(c, max(-0.16, min(0.14, lit * 0.12)))
            if edge < 0.9:
                c = gap
                if moss is not None and r.random() < 0.85:
                    c = shade(moss, (r.random() - 0.5) * 0.12)
            elif edge < 1.6:
                c = shade(c, -0.1)
                if moss is not None and r.random() < 0.35:
                    c = mix(c, moss, 0.6)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    return im


def lily(bloom=False):
    """A lily pad (16 by 16, full colour): a round leaf with its notch, a lit rim, faint veins from the middle, and (bloom)
    a little pink flower in its middle."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    leaf, lit, dark, vein = hexc("#3FAE4A"), hexc("#7FD36A"), hexc("#23823A"), hexc("#5CC45A")
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dy)
            if d > 7.2:
                continue
            a = math.atan2(dy, dx)
            # The notch: a narrow wedge to the lower right.
            if d > 1.2 and abs(((a - 0.7) + math.pi) % (2 * math.pi) - math.pi) < 0.22:
                continue
            c = leaf
            if d > 6.2:
                c = dark if dy > 0 else lit
            elif any(abs(((a - va) + math.pi) % (2 * math.pi) - math.pi) < 0.06 for va in [i * math.pi / 4 for i in range(8)]) and d > 1.5:
                c = vein
            else:
                c = shade(leaf, -0.06 * (d / 7.2) + (0.05 if dy < 0 else 0))
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    if bloom:
        for (x, y, col) in ((7, 7, "#FFC9E2"), (8, 7, "#F59AC4"), (7, 8, "#F59AC4"), (8, 8, "#FFE070"), (6, 7, "#F59AC4"), (9, 8, "#FFC9E2"),
                            (7, 6, "#FFC9E2"), (8, 9, "#F59AC4"), (6, 8, "#FFC9E2"), (9, 7, "#F59AC4")):
            im.putpixel((x, y), hexc(col)[:3] + (255,))
    return im


def solid(c):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    return im


def build():
    os.makedirs(PACK_TEX, exist_ok=True)
    # Route 1's cobblestone (the pack's own: everywhere it's not styled otherwise), in the ledges' warm stone, and the
    # mossy one with green in its gaps.
    cobbles(hexc("#B9AE9A"), hexc("#6F6656"), "cob").save(os.path.join(PACK_TEX, "cobblestone.png"))
    cobbles(hexc("#B3AC94"), hexc("#6F6656"), "mcob", moss=hexc("#6FB24A")).save(os.path.join(PACK_TEX, "mossy_cobblestone.png"))
    # Pewter's yellow doors (John's bamboo doors): the doors' family, Pewter's yellow, a cream window.
    for half in (True, False):
        save("door_yellow_" + ("top" if half else "bottom"), F.door(half, hexc("#E9B83A"), hexc("#9C7420"), hexc("#FFF3CF"), False))
    # Lily pads.
    save("lily_lg", lily(True))
    # The route gates' doorways: one solid dark.
    save("gate_dark", solid(hexc("#1E2330")))
    # The benches' frame (the hanging sides): the bench's yellow, a shade deeper.
    save("bench_frame", K.boards(hexc("#D7A23E"), "bf", lines=False, vertical=True))


if __name__ == "__main__":
    build()
