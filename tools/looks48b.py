"""
0.48.0's town looks (John's 17:42 to 17:53 rounds), drawn for client/AreaLooks (textures/block/area), called from
looks48.build() (EXTRA):

  - the double lamps (Cerulean's off-white, Vermilion's off-grey): their carpet tops a metal cap, the black glass
    warm frosted glass that runs on seamless, the sea lanterns lit globes,
  - Vermilion City in HeartGold's colours: orange tiled roofs, cream walls, warm brick, white trim, the checker path
    as olive-grey paving, its terracotta border peach clay with a trim where it meets the paving, the ground round it
    lighter clay, the smooth stone edge a cream kerb, the canal walls dressed stone, the barrels, the planters, the Gym
    (a brown tiled roof, its walls),
  - the small trees: a proper little trunk (thicker, its bark, a root flare and a stub of a branch) for the fence post,
  - Fuchsia: the dry grass drawn, the pink petals in five patterns, the slab steps the path border's red,
  - flowering azalea hedges, the shore steps (jungle stairs at the water) as wet stones,
  - Bill's door slabs and pillars, the Bike Shop's oak posts and the awning's underside and lettering,
  - Route 4's cobblestone steps as the boarded steps, the mud steps more detailed, Pewter's lamps' chiselled feet,
  - LT. SURGE's electric door (inside).
"""
import math
import os
import random

from PIL import Image, ImageDraw

import lookarea as L
import looks46 as K
import looks47 as K7

V = lambda n: os.path.join(L.VANILLA, "textures", n + ".png")
hexc, mix, save = L.hexc, L.mix, L.save
MODELS = os.path.join(L.HERE, "..", "src", "main", "resources", "assets", "retromon", "models", "block", "area")


def shade(c, k):
    return tuple(int(max(0, min(255, v + (255 - v) * k if k >= 0 else v * (1 + k)))) for v in c[:3]) + (255,)


def grain(base, seed, amt=0.04, rim=0.0):
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * amt
            if rim and (x in (0, 15) or y in (0, 15)): k += rim
            im.putpixel((x, y), shade(base, k))
    return im


# ---------- the double lamps ----------
def cap_top(base):
    """A lamp's cap seen from above: a lit rim, a shallow dish, a centre boss."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            k = 0.16 if d > 6.6 else (-0.06 if d > 5.6 else 0.03 - 0.02 * (d / 6))
            if d < 1.6: k = 0.1
            im.putpixel((x, y), shade(base, k))
    return im


def globe(base):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 6.5) / 9.0
            im.putpixel((x, y), shade(base, 0.35 - 0.4 * d))
    return im


def lamps():
    for nm, post, cap in (("cer", "#E9E5DC", "#F3EFE7"), ("ver", "#BDC1C4", "#CDD1D4")):
        save("lamp_%s_top" % nm, cap_top(hexc(cap)))
        if nm == "ver":
            save("lamp_ver_post", L.metal(hexc(post), seam=False))
            save("lamp_ver_cap", L.metal(hexc(cap), seam=False))
    save("lamp_globe_warm", globe(hexc("#FFE7A8")))
    K.GLASS["glass_lamp"] = (hexc("#FFF0CC"), 120, hexc("#F6F1E6"), hexc("#FFF8E6"))
    for m in range(16):
        save("glass_lamp_%d" % m, K.glass_tile("glass_lamp", m))


# ---------- Vermilion City (HeartGold's colours) ----------
def clay(base, seed, dots=14):
    """HeartGold's clay ground: warm, a few darker and lighter flecks."""
    r = random.Random(seed)
    im = grain(base, seed, 0.03)
    for _ in range(dots):
        x, y = r.randrange(16), r.randrange(16)
        im.putpixel((x, y), shade(base, r.choice((-0.12, -0.08, 0.1))))
    return im


def border(base, trim, light, mask):
    """The path's clay border: a trim (a dark line, a lit one inside it) where it meets something else."""
    im = clay(base, "vb")
    top, right, bottom, left = mask & 1, mask & 2, mask & 4, mask & 8
    for y in range(16):
        for x in range(16):
            if (top and y == 0) or (bottom and y == 15) or (left and x == 0) or (right and x == 15):
                im.putpixel((x, y), trim[:3] + (255,))
            elif (top and y == 1) or (bottom and y == 14) or (left and x == 1) or (right and x == 14):
                im.putpixel((x, y), light[:3] + (255,))
    return im


def slab_pave(base, seed):
    """HeartGold's road: big flat slabs, a fine dark seam round each, lit along the top."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.035
            if y == 0 or x == 0: k += 0.07
            if y == 15 or x == 15: k -= 0.16
            if y == 8 and x > 0: k -= 0.07
            im.putpixel((x, y), shade(base, k))
    return im


def stone_wall(base):
    """Dressed stone blocks, two courses a block, the joins staggered."""
    r = random.Random("vsw")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            row = y // 8
            k = (r.random() - 0.5) * 0.05
            xo = (x + (4 if row else 0)) % 8
            if y % 8 == 7 or xo == 7: k -= 0.2
            elif y % 8 == 0 or xo == 0: k += 0.08
            im.putpixel((x, y), shade(base, k))
    return im


def vermilion():
    pal = K.roof_pal("#F08A30", "#B45A22")
    save("roof_ver_tiles", L.roof_tiles(pal))
    save("roof_ver_edge", L.roof_edge(pal))
    save("roof_ver_under", L.solid(pal["under"]))
    gp = K.roof_pal("#9B6A40", "#6A4426")
    save("roof_vgym_tiles", L.roof_tiles(gp))
    save("roof_vgym_edge", L.roof_edge(gp))
    save("roof_vgym_under", L.solid(gp["under"]))
    save("wall_ver", grain(hexc("#F3E8D2"), "vw", 0.03))
    save("base_ver", L.bricks(hexc("#B45E46"), hexc("#E0D2BE")))
    save("trim_ver", K7.trim_boards(hexc("#F5F3EE")))
    for i, c in enumerate(("#A9A88E", "#BDBBA2", "#9A987E", "#B3B198"), 1):
        save("pave_ver_%d" % i, slab_pave(hexc(c), "pv%d" % i))
    for m in range(16):
        save("border_ver_%d" % m, border(hexc("#E9A97C"), hexc("#B06E48"), hexc("#F6C9A2"), m))
    save("ground_ver", clay(hexc("#EDB98E"), "vg"))
    save("kerb_ver", K7.step_stone(hexc("#E8DDC4")))
    save("canal_ver", stone_wall(hexc("#B9B4A8")))
    save("barrel_ver_side", L.recolour_flat(V("block/barrel_side"), hexc("#B07A44")))
    save("barrel_ver_top", L.recolour_flat(V("block/barrel_top"), hexc("#C08A52")))
    save("planter_ver", K.boards(hexc("#9C6A40"), "pvw", vertical=True))
    save("planter_rim", K7.step_stone(hexc("#E6D9BC")))
    save("vgym_wall", grain(hexc("#E7A35A"), "vgw", 0.03, 0.05))
    save("dock_post", L.metal(hexc("#5B5550"), seam=False))


# ---------- the small trees ----------
def bark():
    r = random.Random("bark")
    base = hexc("#7A5534")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.06 + (0.08 if x % 4 == 0 else -0.08 if x % 4 == 2 else 0)
            if r.random() < 0.06: k -= 0.18
            im.putpixel((x, y), shade(base, k))
    return im


def trunk_models():
    save("tree_bark", bark())
    tex = {"bark": "retromon:block/area/tree_bark", "particle": "retromon:block/area/tree_bark"}
    f = lambda a, b, c, d, e, g: {"from": [a, b, c], "to": [d, e, g], "faces": {k: {"texture": "#bark"} for k in ("north", "south", "east", "west", "up", "down")}}
    # At the foot: a trunk 5 across, a flare of roots spreading at the ground; higher up, 4 across and a stub of branch.
    base = [f(5.5, 0, 5.5, 10.5, 16, 10.5), f(4, 0, 6, 12, 2, 10), f(6, 0, 4, 10, 2.5, 12), f(4.5, 0, 4.5, 11.5, 1, 11.5)]
    upper = [f(6, 0, 6, 10, 16, 10), f(10, 9, 7, 14, 11, 9), f(2.5, 4, 7, 6, 6, 9)]
    L.write_json(os.path.join(MODELS, "ttrunk_a.json"), {"parent": "block/block", "ambientocclusion": True, "textures": tex, "elements": base})
    L.write_json(os.path.join(MODELS, "ttrunk_b.json"), {"parent": "block/block", "ambientocclusion": True, "textures": tex, "elements": upper})
    L.write_json(os.path.join(MODELS, "ttrunk_small.json"), {"parent": "block/block", "ambientocclusion": True, "textures": tex, "elements": upper})


# ---------- Fuchsia, the hedges, the water's edge ----------
def petals(seed):
    """Pink petals lying about, a different scatter each (the patches look different side by side)."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cols = [hexc(c) for c in ("#F6B6CE", "#EE93B6", "#FBD3E2", "#E57AA3")]
    for _ in range(r.randint(9, 15)):
        x, y = r.randrange(1, 15), r.randrange(1, 15)
        c = r.choice(cols)
        for dx, dy in ((0, 0), (1, 0), (0, 1)) if r.random() < 0.6 else ((0, 0), (1, 0), (0, 1), (1, 1)):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16: im.putpixel((x + dx, y + dy), c[:3] + (255,))
        if r.random() < 0.5: im.putpixel((x, y), shade(c, 0.2)[:3] + (255,))
    for _ in range(3):
        x, y = r.randrange(16), r.randrange(16)
        im.putpixel((x, y), hexc("#6E9A3E")[:3] + (255,))
    return im


def dry_grass(tall):
    """Dry grass (Fuchsia's): pale straw blades, a few bent, darker at the root."""
    r = random.Random("dg%d" % tall)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    top = 2 if tall else 6
    for i in range(9 if tall else 7):
        x = 1 + i * 14 // (8 if tall else 6) + r.randint(-1, 1)
        h0 = r.randint(top, top + 4)
        lean = r.choice((-1, 0, 0, 1))
        for y in range(15, h0, -1):
            xx = x + (lean if y < h0 + 4 else 0)
            if 0 <= xx < 16:
                t = (15 - y) / max(1, 15 - h0)
                im.putpixel((xx, y), mix(hexc("#9C8A4E"), hexc("#E6D49A"), t)[:3] + (255,))
    return im


def wet_stones():
    """The steps at the water as the shore's wet stones: rounded pebbles, darker and glossy, foam light at the top."""
    r = random.Random("ws")
    base = hexc("#8C8579")
    im = Image.new("RGBA", (16, 16))
    pts = [(r.random() * 16, r.random() * 16, r.uniform(2.2, 3.6)) for _ in range(14)]
    for y in range(16):
        for x in range(16):
            best, k = 99, -0.25
            for px, py, s in pts:
                d = math.hypot(((x - px + 8) % 16) - 8, ((y - py + 8) % 16) - 8) / s
                if d < best: best = d
            if best < 1: k = 0.12 * (1 - best) - 0.04 + (0.16 if best < 0.3 else 0)
            if y <= 1: k += 0.18
            im.putpixel((x, y), shade(base, k))
    return im


def fuchsia_and_more():
    for i in range(5):
        save("petals_%d" % i, petals("pp%d" % i))
    save("dry_grass_short", dry_grass(False))
    save("dry_grass_tall_bottom", dry_grass(True))
    save("dry_grass_tall_top", dry_grass(False))
    save("shore_step", wet_stones())
    K7.HEDGES["hedge_fa"] = ("#4E9334", "#79BD4E", "#2D6421")
    for m in range(16):
        im = K7.hedge_tile("hedge_fa", m)
        rr = random.Random("fa%d" % m)
        for _ in range(9):
            x, y = rr.randrange(16), rr.randrange(16)
            im.putpixel((x, y), hexc(rr.choice(("#E58BC2", "#F2A9D3", "#D86FB0")))[:3] + (255,))
        save("hedge_fa_%d" % m, im)
    save("red_step", K7.step_stone(hexc("#C9473E")))


# ---------- Bill's, the Bike Shop, steps, Pewter's lamp feet ----------
def letters(base, ink, text, scale=1):
    im = Image.new("RGBA", (16 * len(text), 16), base[:3] + (255,))
    d = ImageDraw.Draw(im)
    for i, ch in enumerate(text):
        d.text((i * 16 + 4, 2), ch, fill=ink[:3] + (255,))
    return im


def bits():
    save("bill_pillar", K7.pillar(hexc("#E6E2D8")))
    save("bill_plinth", L.bricks(hexc("#5E6670"), hexc("#3A4048")))
    save("bill_step", K7.step_stone(hexc("#D9D4C8")))
    save("bike_post", K.boards(hexc("#C79A62"), "bkp", vertical=True))
    save("awning_under", grain(hexc("#C85E26"), "aun", 0.03))
    # BIKE SHOP across the awning's front: a letter a block, white on orange. (The shelves' street side is the top
    # right quarter of their picture, so the letter's drawn there, a pixel font 5 by 7 in its 8 by 8.)
    FONT = {"B": ["1110", "1001", "1110", "1001", "1001", "1110"], "I": ["111", "010", "010", "010", "010", "111"],
            "K": ["1001", "1010", "1100", "1010", "1001", "1001"], "E": ["1111", "1000", "1110", "1000", "1000", "1111"],
            "S": ["0111", "1000", "0110", "0001", "0001", "1110"], "H": ["1001", "1001", "1111", "1001", "1001", "1001"],
            "O": ["0110", "1001", "1001", "1001", "1001", "0110"], "P": ["1110", "1001", "1110", "1000", "1000", "1000"]}
    base = hexc("#E0702F")
    for i, ch in enumerate("BIKESHOP"):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                im.putpixel((x, y), shade(base, -0.1 if y in (7, 15) else 0))
        g = FONT[ch]
        ox = 8 + (8 - len(g[0])) // 2
        for gy, row in enumerate(g):
            for gx, c in enumerate(row):
                if c == "1": im.putpixel((ox + gx, 1 + gy), (252, 249, 240, 255))
        save("awning_l%d" % i, im)
    # The awning's bottom row (its stairs), a scalloped valance: orange and white in turn, a shadowed hem.
    for nm, c in (("o", "#E0702F"), ("w", "#F4F1EA")):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                hem = 12 + int(2 * abs(math.sin((x + 0.5) / 16 * math.pi * 2)))
                k = -0.16 if y >= hem else (0.05 if y < 2 else 0.0)
                im.putpixel((x, y), shade(hexc(c), k))
        save("awning_val_" + nm, im)
    save("step_cobble", K.boards(hexc("#9C7A55"), "stc"))
    save("mud_step", mud_step())
    save("plamp_foot", plamp_foot())
    # LT. SURGE's electric door (inside): bolts of yellow on a dark blue glow, see-through between.
    im = Image.new("RGBA", (16, 16), (40, 90, 160, 110))
    zz = [3, 6, 4, 8, 6, 10, 8, 12, 10, 13, 11, 12]
    for i, x in enumerate(zz):
        for dx in (-1, 0, 1):
            if 0 <= x + dx < 16: im.putpixel((x + dx, i + 2), (255, 228, 106, 255 if dx == 0 else 200))
    for y in (0, 15):
        for x in range(16): im.putpixel((x, y), (120, 220, 255, 230))
    save("surge_barrier", im)


def mud_step():
    """A step in the mud (Route 3 and 4's): packed earth, a worn lighter tread at the top edge, stones in it."""
    r = random.Random("ms")
    base = hexc("#7D5B3F")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.08
            if y <= 2: k += 0.18
            if y == 3: k -= 0.12
            im.putpixel((x, y), shade(base, k))
    for _ in range(6):
        x, y = r.randrange(16), r.randrange(5, 15)
        for dx, dy in ((0, 0), (1, 0)):
            if x + dx < 16: im.putpixel((x + dx, y + dy), shade(hexc("#9A928A"), r.uniform(-0.1, 0.1)))
    return im


def plamp_foot():
    """Pewter's lamps' foot (the chiselled tuff): a plain grey metal plinth, a lit band near the top."""
    base = hexc("#8E959C")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.0
            if y in (2, 3): k = 0.14
            if y == 4: k = -0.12
            if y == 15: k = -0.15
            if x == 0: k += 0.06
            if x == 15: k -= 0.08
            im.putpixel((x, y), shade(base, k))
    return im


def build():
    lamps()
    vermilion()
    trunk_models()
    fuchsia_and_more()
    bits()
    more()


# ---------- more (Underground huts, Cerulean Gym's pool, the Day Care's bushes, Vermilion's fence, big petals) ----------
def tiles(base, grout, n=4, seed="t"):
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    s = 16 // n
    for y in range(16):
        for x in range(16):
            if x % s == s - 1 or y % s == s - 1: c = grout
            else:
                k = (r.random() - 0.5) * 0.03 + (0.06 if x % s == 0 or y % s == 0 else 0)
                c = shade(base, k)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def shrub():
    """A round leafy shrub (the Day Care's bushes): clusters of leaves, lit on top, a darker heart, see-through corners."""
    r = random.Random("shrub")
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) / 7.5, (y - 8.5) / 7.0)
            if d > 1 or r.random() < 0.08 * d: continue
            k = 0.22 * (1 - y / 15) - 0.25 * d + (r.random() - 0.5) * 0.25
            im.putpixel((x, y), shade(hexc("#4C8A34"), k)[:3] + (255,))
    return im


def more():
    pal = K.roof_pal("#5C7FA8", "#3E5878")
    save("roof_ug_tiles", L.roof_tiles(pal))
    save("roof_ug_edge", L.roof_edge(pal))
    save("roof_ug_under", L.solid(pal["under"]))
    save("wall_ug", grain(hexc("#EDEAE2"), "wug", 0.025, 0.04))
    save("base_ug", stone_wall(hexc("#A7A398")))
    save("post_ug", K7.trim_boards(hexc("#F2F0EA")))
    save("rail_ug", L.metal(hexc("#4C535C"), seam=False))
    save("pool_tile", tiles(hexc("#F4F7F8"), hexc("#B9CCD6"), 4, "pt"))
    save("pool_floor", tiles(hexc("#3D86C6"), hexc("#2C6599"), 4, "pf"))
    save("pool_edge_tile", tiles(hexc("#E9EEF0"), hexc("#3D86C6"), 2, "pe"))
    save("bush_round", shrub())
    save("fence_grey", L.metal(hexc("#9AA0A6"), seam=False))
    save("step_boards_d", step_detail())
    big = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for i in range(16):
        big.paste(petals("pb%d" % i), ((i % 4) * 16, (i // 4) * 16))
    save("petals_big", big)


def step_detail():
    """The boarded steps (the mud brick stairs) more detailed: three boards with a lit nosing, a shadowed riser line,
    the grain, and the nails at their ends."""
    r = random.Random("sbd")
    base = hexc("#A8794A")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            b = y // 5
            k = (r.random() - 0.5) * 0.06 + (0.05 if (x * 3 + b * 7) % 9 == 0 else 0)
            if y % 5 == 0: k += 0.16
            if y % 5 == 4: k -= 0.2
            if x in (1, 14) and y % 5 == 2: k = -0.35
            if x in (7, 8) and y % 5 in (1, 2, 3) and b == 1: k -= 0.05
            im.putpixel((x, y), shade(base, k))
    return im
