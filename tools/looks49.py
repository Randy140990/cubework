"""
0.49.0's looks (John's 19:06 round): Lavender Town at night, drawn for client/AreaLooks (textures/block/area),
called from looks48.build() (EXTRA):

  - its houses: purple brick, deep plum tiled roofs, dark timber, pale stone feet, dark planters and sills, warm lit
    windows (the town's dark: their glass glows),
  - the POKéMON TOWER in solid colours: deep lavender stone walls with fine panel joins, a pale lilac band, dark plum
    ledges, slate columns, a deep purple metal roof, a silver spire, violet glass,
  - its streets: purple-grey flagstones (laid over two blocks so they don't repeat), darker ones, a lilac kerb,
    the Tower's pale forecourt,
  - its lamps: plum metal posts, ornate violet chiselled feet and heads, a lilac cap, a violet globe and glass,
  - the low mist over the town (a soft lilac haze, laid over four blocks).
"""
import math
import random

from PIL import Image

import lookarea as L
import looks46 as K
import looks47 as K7
import looks48b as B

hexc, mix, save = L.hexc, L.mix, L.save
shade, grain = B.shade, B.grain


def flagstones(size, seed, base, var, joint):
    """Irregular flagstones (Let's Go's Lavender streets): slabs of a few sizes in rows, each its own shade, a dark
    joint round each, lit along its top edge."""
    r = random.Random(seed)
    im = Image.new("RGBA", (size, size))
    row_h = [5, 6, 5, 6, 5, 5]
    y = 0
    rows = []
    while y < size:
        h = row_h[len(rows) % len(row_h)]
        if y + h > size: h = size - y
        x = r.randrange(0, 6)
        cuts = [0]
        while True:
            x += r.choice((5, 6, 7, 8))
            if x >= size: break
            cuts.append(x)
        rows.append((y, h, cuts))
        y += h
    for y0, h, cuts in rows:
        bounds = cuts + [size]
        for i in range(len(bounds) - 1):
            k0 = (r.random() - 0.5) * var
            for yy in range(y0, y0 + h):
                for xx in range(bounds[i], bounds[i + 1]):
                    k = k0 + (r.random() - 0.5) * 0.04
                    if yy == y0 or xx == bounds[i]: c = joint
                    else:
                        if yy == y0 + 1: k += 0.1
                        if yy == y0 + h - 1 or xx == bounds[i + 1] - 1: k -= 0.08
                        c = shade(base, k)
                    im.putpixel((xx % size, yy), c[:3] + (255,))
    return im


def panels(base, seed, joint=True):
    """Solid stone panels (the Tower): a flat colour, the faintest grain, a fine join every half block."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.025
            if joint and (y in (0, 8)): k -= 0.12
            if joint and y in (1, 9): k += 0.05
            if joint and x == 0 and y < 8: k -= 0.08
            if joint and x == 8 and y >= 8: k -= 0.08
            im.putpixel((x, y), shade(base, k))
    return im


def chisel(base, light, dark):
    """An ornate lamp foot or head: a framed panel with a diamond in it."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = base
            if x in (0, 15) or y in (0, 15): c = dark
            elif x in (1, 14) or y in (1, 14): c = light
            elif abs(x - 7.5) + abs(y - 7.5) <= 4: c = light if (x + y) % 2 else shade(light, -0.08)
            elif abs(x - 7.5) + abs(y - 7.5) <= 5: c = dark
            im.putpixel((x, y), c[:3] + (255,))
    return im


def mist():
    """The low mist: soft lilac wisps, mostly clear, laid over four blocks (64 by 64)."""
    size = 64
    a = B.__dict__.get("noise")
    r = random.Random("mist")
    g = [[r.random() for _ in range(8)] for _ in range(8)]
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            fx, fy = x / size * 8, y / size * 8
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            p = g[y0 % 8][x0 % 8] * (1 - tx) * (1 - ty) + g[y0 % 8][(x0 + 1) % 8] * tx * (1 - ty) + g[(y0 + 1) % 8][x0 % 8] * (1 - tx) * ty + g[(y0 + 1) % 8][(x0 + 1) % 8] * tx * ty
            alpha = int(max(0, (p - 0.35) / 0.65) ** 1.6 * 95)
            im.putpixel((x, y), (214, 204, 236, alpha))
    return im


def build():
    # The houses.
    save("lav_brick", L.bricks(hexc("#7A4F8E"), hexc("#B9A2C8")))
    pal = K.roof_pal("#5B3A78", "#3A2350")
    save("roof_lav_tiles", L.roof_tiles(pal))
    save("roof_lav_edge", L.roof_edge(pal))
    save("roof_lav_under", L.solid(pal["under"]))
    save("lav_timber", K.boards(hexc("#3E2C3A"), "ltm", vertical=True))
    save("lav_base", K7.step_stone(hexc("#C9C1D3")))
    save("lav_base_dark", K7.step_stone(hexc("#8D8399")))
    save("lav_planter", K.boards(hexc("#4A3448"), "lpl", vertical=False))
    save("lav_sill", grain(hexc("#E6DDEB"), "lsl", 0.03, -0.05))
    save("lav_glow", B.globe(hexc("#D9C2FF")))
    # The Tower, in solid colours.
    save("tower_wall", panels(hexc("#6E5A96"), "tw"))
    save("tower_trim", panels(hexc("#B9A6D8"), "tt", joint=False))
    save("tower_ledge", panels(hexc("#3E2F52"), "tl"))
    save("tower_column", L.metal(hexc("#4A4458"), seam=False))
    save("tower_roof", K7.corrugated(hexc("#4B3570")))
    save("tower_plinth", L.bricks(hexc("#4E4658"), hexc("#2E2936")))
    save("tower_spire", L.metal(hexc("#D6D0E2"), seam=False))
    K.GLASS["glass_tower"] = (hexc("#B49BE6"), 150, hexc("#E7DDF7"), hexc("#CDB9F2"))
    for m in range(16):
        save("glass_tower_%d" % m, K.glass_tile("glass_tower", m))
    # The streets.
    save("lav_flag", flagstones(32, "lf", hexc("#7C7488"), 0.14, hexc("#433C4E")))
    save("lav_flag_dark", flagstones(32, "lfd", hexc("#5E5668"), 0.12, hexc("#352F3E")))
    save("lav_kerb", K7.step_stone(hexc("#B3A9C2")))
    save("lav_court", flagstones(32, "lc", hexc("#ABA4B8"), 0.1, hexc("#6E6680")))
    save("lav_wall", L.bricks(hexc("#8F869C"), hexc("#5F5869")))
    # The lamps.
    save("lamp_lav_post", L.metal(hexc("#4C3A5E"), seam=False))
    save("lamp_lav_chisel", chisel(hexc("#5E477A"), hexc("#9C84C2"), hexc("#33263F")))
    save("lamp_lav_chisel_top", chisel(hexc("#5E477A"), hexc("#9C84C2"), hexc("#33263F")))
    save("lamp_lav_top", B.cap_top(hexc("#8E7AAE")))
    save("lamp_globe_violet", B.globe(hexc("#E2CCFF")))
    K.GLASS["glass_lav_lamp"] = (hexc("#D8C4F5"), 120, hexc("#EDE4F8"), hexc("#F3ECFB"))
    for m in range(16):
        save("glass_lav_lamp_%d" % m, K.glass_tile("glass_lav_lamp", m))
    # The shops' white walls (clean panels) and the mist.
    save("shop_wall", panels(hexc("#F1EFF2"), "sw"))
    save("mist_lav", mist())
