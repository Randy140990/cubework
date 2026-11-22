"""
0.47.0's looks (John's 16:17 and 16:58 rounds), drawn for client/AreaLooks (textures/block/area):

  - hedges: leaves standing on the ground drawn as a trimmed hedge, one solid green running on along a row, its edges
    rounded where it ends (16 pictures each, by which sides run on; client/AreaLooks "hedge"),
  - Cerulean City: its houses' roofs (a new style in their teal), the checkered paving as grey-blue paving stones, the
    smooth stone border as a kerb, the brick base, the pillars, the white window trim, the door steps, its lamps in
    off-white metal, the Bike Shop's awning,
  - Nugget Bridge in gold (HeartGold's golden bridge),
  - Bill's Sea Cottage (its bamboo, its white trim, the chimney, its decking, the cream path),
  - the Power Plant (its iron very metallic, its yellow, red and white painted metal, its roof, its fronts).

    (called from lookarea.build())
"""
import math
import os
import random

from PIL import Image

import lookarea as L
import looks46 as K

V = lambda n: os.path.join(L.VANILLA, "textures", n + ".png")
hexc, mix, save = L.hexc, L.mix, L.save


def shade(c, k):
    return tuple(int(max(0, min(255, v + (255 - v) * k if k >= 0 else v * (1 + k)))) for v in c[:3]) + (255,)


# ---------- hedges ----------
HEDGES = {
    "hedge_az": ("#4F9A35", "#7CC24E", "#2F6A22"),   # azalea: fresh green
    "hedge_ac": ("#5E9E38", "#8AC758", "#3A6C24"),   # acacia: a touch yellower
    "hedge_jg": ("#3F8A2C", "#66AE45", "#24581A"),   # jungle: deeper
    "hedge_do": ("#2F6E2A", "#4E9142", "#1B4718"),   # dark oak: darkest
    "hedge_bi": ("#6EA548", "#97C86A", "#46762E"),   # birch: light
    "hedge_ok": ("#4A8F33", "#73B74B", "#2C6020"),   # oak
}


def hedge_tile(name, mask):
    base, light, dark = (hexc(c) for c in HEDGES[name])
    r = random.Random(name)
    im = Image.new("RGBA", (16, 16))
    # Small leaves, close-clipped: the base green, clusters lit and gaps in shade (the same every tile, so it runs on).
    px = [[0.0] * 16 for _ in range(16)]
    for _ in range(46):
        x, y, s = r.randrange(16), r.randrange(16), r.choice((1, 1, 2))
        for dy in range(s):
            for dx in range(s):
                px[(y + dy) % 16][(x + dx) % 16] += 0.55
    for _ in range(30):
        x, y = r.randrange(16), r.randrange(16)
        px[y][x] -= 0.6
    top, right, bottom, left = mask & 1, mask & 2, mask & 4, mask & 8
    for y in range(16):
        for x in range(16):
            k = px[y][x]
            c = mix(base, light, min(1.0, k)) if k > 0 else mix(base, dark, min(1.0, -k))
            # Where the hedge ends: rounded off, a lit rim inside a darker edge (the clipped top catching the light).
            d = 99
            if top: d = min(d, y)
            if bottom: d = min(d, 15 - y)
            if left: d = min(d, x)
            if right: d = min(d, 15 - x)
            if d == 0:
                c = mix(c, dark, 0.45)
            elif d == 1:
                c = mix(c, light, 0.30)
            elif d == 2:
                c = mix(c, light, 0.12)
            # The corners where two ends meet: cut round.
            for cx, cy, a, b in ((0, 0, top, left), (15, 0, top, right), (0, 15, bottom, left), (15, 15, bottom, right)):
                if a and b and abs(x - cx) + abs(y - cy) <= 1:
                    c = mix(dark, base, 0.25)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    return im


def hedges():
    for name in HEDGES:
        for m in range(16):
            save("%s_%d" % (name, m), hedge_tile(name, m))


def hedge_spec():
    return ",".join("block/%s=%s" % (b, n) for b, n in (("azalea_leaves", "hedge_az"), ("acacia_leaves", "hedge_ac"), ("jungle_leaves", "hedge_jg"),
                                                         ("dark_oak_leaves", "hedge_do"), ("birch_leaves", "hedge_bi"), ("oak_leaves", "hedge_ok")))


HEDGE_BLOCKS = "azalea_leaves,acacia_leaves,jungle_leaves,dark_oak_leaves,birch_leaves,oak_leaves"


# ---------- Cerulean City ----------
def pillar(base):
    """A smooth stone pillar: lit down its left, shadowed down its right, a fine groove a little in from each side."""
    im = Image.new("RGBA", (16, 16))
    r = random.Random("pil")
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.03
            if x == 0: k += 0.12
            if x == 15: k -= 0.14
            if x in (3, 12): k -= 0.07
            if x in (4, 13): k += 0.04
            im.putpixel((x, y), shade(base, k))
    return im


def trim_boards(base):
    """White painted trim: wide boards across, a soft shadow under each."""
    im = Image.new("RGBA", (16, 16))
    r = random.Random("trim")
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.02
            if y % 8 == 0: k += 0.06
            if y % 8 == 7: k -= 0.12
            im.putpixel((x, y), shade(base, k))
    return im


def step_stone(base):
    """A door step: a lighter nosing along its front edge (the top of the picture), its face plain."""
    im = Image.new("RGBA", (16, 16))
    r = random.Random("step")
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.04
            if y <= 1: k += 0.16
            if y == 2: k -= 0.08
            if y == 15: k -= 0.1
            im.putpixel((x, y), shade(base, k))
    return im


def fabric(base, seed):
    im = Image.new("RGBA", (16, 16))
    r = random.Random(seed)
    for y in range(16):
        for x in range(16):
            k = ((x + y) % 2) * 0.03 + (r.random() - 0.5) * 0.04
            if y == 15: k -= 0.12
            im.putpixel((x, y), shade(base, k))
    return im


def cerulean():
    pal = K.roof_pal("#3FB4B8", "#2B8589")
    save("roof_cer_tiles", L.roof_tiles(pal))
    save("roof_cer_edge", L.roof_edge(pal))
    save("roof_cer_under", L.solid(pal["under"]))
    # The checker (white and light blue concrete, light blue and white terracotta) as paving stones in grey-blue tones.
    for i, c in enumerate(("#D4DBE3", "#A3B5C8", "#8597AC", "#BCC6D2"), 1):
        save("pave_cer_%d" % i, L.pave(hexc(c), "pc%d" % i))
    save("kerb_cer", step_stone(hexc("#C7CBCF")))
    save("base_cer", L.bricks(hexc("#A04B3C"), hexc("#D8CDBE")))
    save("pillar_cer", pillar(hexc("#C9CDD2")))
    save("trim_cer", trim_boards(hexc("#F2F1EC")))
    save("step_cer", step_stone(hexc("#7C858E")))
    save("lamp_white_post", L.metal(hexc("#E9E5DC"), seam=False))
    save("lamp_white_cap", L.metal(hexc("#F3EFE7"), seam=False))
    save("awning_orange", fabric(hexc("#E0702F"), "ao"))
    save("awning_white", fabric(hexc("#F4F1EA"), "aw"))
    # Nugget Bridge, gold (HeartGold's).
    save("bridge_gold_boards", K.boards(hexc("#E6B63F"), "gbd", vertical=True))
    save("bridge_gold_beam", L.metal(hexc("#C99A2A"), seam=False))
    save("bridge_gold_rail", L.recolour_flat(V("block/oak_shelf"), hexc("#E2B23C")))
    save("gold_bolt", tint(K.bolt(), hexc("#E2B23C")))


def tint(im, c):
    out = Image.new("RGBA", im.size)
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            p = im.getpixel((x, y))
            l = (p[0] + p[1] + p[2]) / 765.0
            out.putpixel((x, y), tuple(int(v * (0.35 + 0.9 * l)) for v in c[:3]) + (p[3],))
    return out


# ---------- Bill's Sea Cottage ----------
def bills():
    save("bill_roof", K.boards(hexc("#D8B648"), "brf", vertical=False))
    save("trim_white", trim_boards(hexc("#F4F2EC")))
    save("chimney_stone", L.bricks(hexc("#7E939B"), hexc("#B9C3C7")))
    save("bill_shelf", L.recolour_flat(V("block/bamboo_shelf"), hexc("#D8B648")))
    save("deck_boards", K.boards(hexc("#8C6239"), "dkb", vertical=True))
    save("deck_shelf", L.recolour_flat(V("block/spruce_shelf"), hexc("#8C6239")))
    save("path_cream", L.pave(hexc("#E8E1C2"), "pcr"))


# ---------- the Power Plant ----------
def steel(base, seed, rivets=True):
    """Very metallic: brushed lines across, a strong sheen high on it and a bright streak, rivets at the corners."""
    im = Image.new("RGBA", (16, 16))
    r = random.Random(seed)
    rows = [(r.random() - 0.5) * 0.07 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            k = 0.18 - 0.32 * (y / 15) + rows[y] + (r.random() - 0.5) * 0.025
            if 4 <= (x + y) % 16 <= 5: k += 0.16
            if y == 0 or x == 0: k += 0.12
            if y == 15 or x == 15: k -= 0.18
            if rivets and (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
                k = 0.35
            if rivets and (x, y) in ((3, 3), (14, 3), (3, 14), (14, 14)):
                k = -0.3
            im.putpixel((x, y), shade(base, k))
    return im


def grate(base):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.08 if (x % 4 in (0, 1)) else -0.35
            if y in (0, 15) or x in (0, 15): k = 0.15
            im.putpixel((x, y), shade(base, k))
    return im


def corrugated(base):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.14 * math.cos(x / 4.0 * 2 * math.pi) - 0.08 * (y / 15)
            im.putpixel((x, y), shade(base, k))
    return im


def frame(base):
    """Painted steel, a lattice of darker struts across it (the red towers at the sides)."""
    im = steel(base, "frm", rivets=False)
    for i in range(16):
        for x, y in ((i, i), (15 - i, i)):
            im.putpixel((x, y), shade(base, -0.3))
    return im


def power_plant():
    save("pp_iron", steel(hexc("#C5CCD3"), "ppi"))
    save("pp_grate", grate(hexc("#6E767F")))
    save("pp_yellow", L.metal(hexc("#E7B437"), seam=True))
    save("pp_red", L.metal(hexc("#B8443A"), seam=True))
    save("pp_white", L.metal(hexc("#F1F0EC"), seam=True))
    save("pp_roof", corrugated(hexc("#8C3A44")))
    save("pp_red_frame", frame(hexc("#B8443A")))
    save("pp_front", L.solid_shaded(hexc("#E9E7E1"), 0.06, -0.08, 0.01, "ppf"))
    save("pp_wall_grey", steel(hexc("#AEB5BC"), "ppw", rivets=False))
    save("pp_steel", steel(hexc("#7F8790"), "pps"))
    save("pp_post", L.metal(hexc("#4A5058"), seam=False))


def build():
    hedges()
    cerulean()
    bills()
    power_plant()
