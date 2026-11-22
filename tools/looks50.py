"""
0.50.0's looks (John's 20:07, 20:12 and 20:20 rounds), drawn for client/AreaLooks (textures/block/area) and the
Retromon Look pack, called from looks48.build() (EXTRA):

  - the trees leafy and rounded: Shards of Arcadia's extra leaves (his planes of leaves standing out of a block),
    on Retromon's greens, as two models a kind (client/AreaLooks "leafy"),
  - the hedges in Let's Go's style: bright, clumped leaves, their open rims clipped soft (not a solid edge), plain
    and flowering (pink blossoms), one picture for all a hedge's leaves (so no checker),
  - the cliffs' brown mushroom tops: rough flagstones of the ledges' earth, laid over four blocks so no two match,
  - the Power Plant: its yellow band, its roof, its red frames and its iron in solid colours,
  - doors: Let's Go's green (cross-braced) and red ones, and a plain wooden one (the SAFARI ZONE's),
  - Vermilion: its door steps, the Fan Club's bow (pink, a POKe BALL in its middle) and its arch, the S.S. ANNE's
    plinth, legs, gangway towers and doorway,
  - Route 17's lamps (orange metal), the benches under their arches (yellow slats, dark metal, a white pergola),
    its tarmac (dark, a couple of scuffs),
  - Route 12's piers as Let's Go's (yellow-tan boards in darker frames, steel bolts),
  - the rocks in the water (wet grey rock),
  - Routes 12 and 16's house (one look for both), Bill's timber, the Diglett's Cave mouths.

Also the pack's plants from John's Shards set: the dry grass, the torchflower, the azalea bushes and the bush.
"""
import json
import math
import os
import random
import shutil

from PIL import Image

import lookarea as L
import looks46 as K
import looks47 as K7
import looks48b as B
import looks49 as N9

hexc, mix, save = L.hexc, L.mix, L.save
shade, grain = B.shade, B.grain
HERE = os.path.dirname(os.path.abspath(__file__))
SHARDS = os.path.join(HERE, "art", "shards_plants")
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon")
PACK = os.path.join(HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft")


# ---------- the leafy trees ----------
LEAFY = ["oak", "acacia", "birch", "azalea", "flowering_azalea", "dark_oak", "jungle", "mangrove", "cherry"]
# Tinted leaves' extra leaves on clean greys (the biome's green, made brighter, colours them as it does the leaves).
GREYS = [(96, 100, 92), (150, 156, 144), (204, 210, 194), (240, 244, 230)]
AZALEA = [hexc(c) for c in ("#2F6A24", "#4C9330", "#6FB63C", "#94D14C", "#BCE56C")]


def _ramp(cols, t):
    t = max(0.0, min(1.0, t))
    seg = t * (len(cols) - 1)
    i = min(len(cols) - 2, int(seg))
    return mix(cols[i], cols[i + 1], seg - i)


def leaf_extra(kind):
    """His extra leaves for a kind, brought down to 32 by 32 (Retromon's texel size on a plane a block and a half
    across), their colours on Retromon's: the tinted kinds on greys, the azaleas on Let's Go's tree greens (the
    flowering one's blossoms kept pink)."""
    im = Image.open(os.path.join(SHARDS, "textures", "block", "extra", kind + "_leaves_extra.png")).convert("RGBA")
    im = im.resize((32, 32), Image.BOX)
    p = im.load()
    lum = sorted(0.3 * p[x, y][0] + 0.59 * p[x, y][1] + 0.11 * p[x, y][2] for y in range(32) for x in range(32) if p[x, y][3] > 100)
    lo, hi = (lum[len(lum) // 40], lum[-1 - len(lum) // 40]) if lum else (0, 255)
    for y in range(32):
        for x in range(32):
            r, g, b, a = p[x, y]
            if a < 110:
                p[x, y] = (0, 0, 0, 0)
                continue
            t = (0.3 * r + 0.59 * g + 0.11 * b - lo) / max(1.0, hi - lo)
            if kind == "flowering_azalea" and r > g + 18:
                c = mix(hexc("#E97FB4"), hexc("#FFC9E2"), max(0, min(1, t)))
            elif kind in ("azalea", "flowering_azalea"):
                c = _ramp(AZALEA, t)
            elif kind == "cherry":
                c = mix(hexc("#E58DB8"), hexc("#FFD3E7"), max(0, min(1, t)))
            else:
                c = _ramp(GREYS, t)
            p[x, y] = tuple(int(v) for v in c[:3]) + (255,)
    return im


def leafy():
    """The extra leaves' pictures and their two models a kind: a (his first model's three planes, without its
    block) and b (his second's four)."""
    src1 = json.load(open(os.path.join(SHARDS, "models", "block", "leaves_extra1.json")))["elements"][1:]
    src3 = json.load(open(os.path.join(SHARDS, "models", "block", "leaves_extra3.json")))["elements"]
    models = os.path.join(ASSETS, "models", "block", "area")
    os.makedirs(models, exist_ok=True)
    for kind in LEAFY:
        save("leafy_" + kind, leaf_extra(kind))
        tinted = kind not in ("azalea", "flowering_azalea", "cherry")
        for v, els in (("a", src1), ("b", src3)):
            out = []
            for e in json.loads(json.dumps(els)):
                for f in e["faces"].values():
                    f["texture"] = "#extra"
                    if not tinted: f.pop("tintindex", None)
                out.append(e)
            json.dump({"ambientocclusion": False, "textures": {"extra": "retromon:block/area/leafy_" + kind, "particle": "retromon:block/area/leafy_" + kind},
                       "elements": out}, open(os.path.join(models, "leafy_%s_%s.json" % (kind, v)), "w"), indent=1)


# ---------- the hedges, as Let's Go's ----------
HEDGE = (hexc("#2C7426"), hexc("#4AA236"), hexc("#74C64A"), hexc("#A3DF68"))


def hedge_tile(mask, flowers=False):
    """A hedge face: rounded clumps of small leaves in staggered rows (lit on their upper left, shade in the gaps);
    where the hedge ends (mask: 1 top, 2 right, 4 bottom, 8 left), its rim lit and clipped soft, a leaf or a gap at
    a time (cut out), its corners rounded."""
    dark, base, light, high = HEDGE
    r = random.Random("hlg")
    centres = []
    for row in range(5):
        for col in range(5):
            centres.append((col * 4 + (2 if row % 2 else 0) + r.uniform(-0.6, 0.6), row * 4 - 1 + r.uniform(-0.5, 0.5), r.uniform(2.4, 3.0)))
    im = Image.new("RGBA", (16, 16))
    top, right, bottom, left = mask & 1, mask & 2, mask & 4, mask & 8
    blossoms = {(3, 4), (11, 2), (7, 9), (13, 11), (2, 13), (9, 14)} if flowers else set()
    for y in range(16):
        for x in range(16):
            best, by, bx = 9.0, 0.0, 0.0
            for cx, cy, rad in centres:
                for ox in (-16, 0, 16):
                    for oy in (-16, 0, 16):
                        d = math.hypot(x - cx - ox, y - cy - oy) / rad
                        if d < best: best, bx, by = d, cx + ox, cy + oy
            if best > 1.0:
                c = mix(dark, base, 0.05)
            else:
                lit = ((bx - x) + (by - y)) / 3.0
                k = 0.45 + lit * 0.65 - best * 0.45
                c = mix(base, light, max(0, min(1, k))) if k < 0.8 else mix(light, high, min(1, (k - 0.8) * 2.5))
                if best > 0.8: c = mix(c, dark, 0.55)
            a = 255
            d = 99
            if top: d = min(d, y)
            if bottom: d = min(d, 15 - y)
            if left: d = min(d, x)
            if right: d = min(d, 15 - x)
            if d == 0:
                # The clipped rim: a gap between clumps is cut out, a clump's edge is a lit leaf.
                if best > 0.95 or (x * 7 + y * 3) % 5 == 0: a = 0
                else: c = mix(c, light, 0.25)
            elif d == 1:
                c = mix(c, light, 0.18)
            for cx, cy, aa, bb in ((0, 0, top, left), (15, 0, top, right), (0, 15, bottom, left), (15, 15, bottom, right)):
                if aa and bb and abs(x - cx) + abs(y - cy) <= 2: a = 0
            if (x, y) in blossoms:
                c = hexc("#FFD9EA")
            elif any(abs(x - bx2) + abs(y - by2) == 1 for bx2, by2 in blossoms):
                c = hexc("#EE7FB3")
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (a,))
    return im


def hedges():
    for m in range(16):
        save("hedge_lg_%d" % m, hedge_tile(m))
        save("hedge_lgf_%d" % m, hedge_tile(m, flowers=True))


# ---------- the cliffs' mushroom tops ----------
def hill_top():
    d = K.avg(K.look_or_vanilla("dripstone_block"))
    # Big angular slabs of the ledges' earth, each its own shade, dark cracks between (tiled, no two the same).
    t = shade(d, -0.06)
    save("hill_top", L.rock_face(t, shade(t, 0.22), shade(t, -0.4), "htp", cells=11))


# ---------- the Power Plant, solid ----------
def panel(c, seed, edge=0.07):
    """A solid painted panel: one colour, the faintest grain, a soft lit top and a shaded foot."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.02
            if y == 0: k += edge
            if y == 15 or x == 15: k -= edge
            im.putpixel((x, y), shade(c, k)[:3] + (255,))
    return im


def power_plant():
    save("pp_yellow", panel(hexc("#F2BE2C"), "ppy"))
    save("pp_roof", panel(hexc("#C9423A"), "ppr"))
    save("pp_red_frame", panel(hexc("#C23A33"), "pprf"))
    save("pp_iron", panel(hexc("#C9CED4"), "ppi", 0.05))
    save("pp_dark", panel(hexc("#2A2F38"), "ppd", 0.03))


# ---------- doors ----------
def door(top, colour, frame, glass=None, cross=False, double=False):
    """A door half (16 by 16): a frame, panels; the top half a window (glass) or the cross brace; a knob below."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = colour
            if x in (0, 15) or (top and y == 0) or (not top and y == 15): c = frame
            elif x in (1, 14): c = shade(colour, 0.12)
            elif top and glass and 3 <= x <= 12 and 3 <= y <= 12:
                c = mix(glass, (255, 255, 255, 255), 0.35 if (x + y) % 9 < 2 else 0.0)
                if x in (3, 12) or y in (3, 12): c = shade(colour, -0.25)
            elif cross and 3 <= x <= 12 and (3 <= y <= 15 if top else 0 <= y <= 12):
                yy = y if top else y + 16
                if abs((x - 3) - (yy - 3) * 9 / 25) < 1.1 or abs((12 - x) - (yy - 3) * 9 / 25) < 1.1: c = shade(colour, 0.2)
                elif x in (3, 12) or (top and y == 3) or (not top and y == 12): c = shade(colour, -0.22)
            elif not top and 3 <= x <= 12 and 2 <= y <= 12 and (x in (3, 12) or y in (2, 12)):
                c = shade(colour, -0.22)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    if not top:
        for kx in ((11, 12) if not double else (13, 14)):
            for ky in (1, 2):
                im.putpixel((kx, ky), (222, 186, 96, 255))
    return im


def doors():
    for name, col, fr, gl, cr in (("door_green", hexc("#2F6B4A"), hexc("#1E3F2D"), None, True), ("door_red", hexc("#D9503F"), hexc("#8E2E25"), hexc("#BFE6F5"), False),
                                  ("door_wood", hexc("#8A5A33"), hexc("#553620"), hexc("#CFE8F2"), False)):
        save(name + "_top", door(True, col, fr, gl, cr))
        save(name + "_bottom", door(False, col, fr, gl, cr))


# ---------- Vermilion ----------
def ribbon(base, seed):
    """The Fan Club's pink bow: soft folds across it, a lit edge."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = math.sin((x + y * 0.6) * 0.55) * 0.12
            if y in (0, 15): k -= 0.2
            if y == 1: k += 0.15
            im.putpixel((x, y), shade(base, k)[:3] + (255,))
    return im


def poke_ball():
    im = Image.new("RGBA", (16, 16), hexc("#F08BB4")[:3] + (255,))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.2: continue
            c = hexc("#E8433A") if y < 7 else hexc("#F7F4F0")
            if 6.6 < d: c = hexc("#2A2A30")
            if 7 <= y <= 8: c = hexc("#2A2A30")
            if d < 2.6: c = hexc("#2A2A30")
            if d < 1.7: c = hexc("#FFFFFF")
            if y < 7 and 3 < x < 6 and 2 < y < 5: c = shade(c, 0.35)
            im.putpixel((x, y), tuple(c[:3]) + (255,))
    return im


def arch():
    """The Fan Club's arch board: cream, a pink line along each edge."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = hexc("#F4E7CB")
            if y in (0, 15): c = hexc("#D9668F")
            elif y in (2, 13): c = hexc("#F2A2C2")
            elif (x + y) % 7 == 0 and 4 < y < 11: c = shade(c, -0.05)
            im.putpixel((x, y), tuple(c[:3]) + (255,))
    return im


def doorway():
    """A dark doorway (the S.S. ANNE's, the Diglett's Cave mouths): deep dark, a touch lighter at its edges."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            e = min(x, 15 - x, y, 15 - y)
            v = 14 + max(0, 3 - e) * 5
            im.putpixel((x, y), (v, v + 1, v + 4, 255))
    return im


def vermilion():
    save("step_ver", K7.step_stone(hexc("#EADCC2")))
    save("fan_bow", ribbon(hexc("#F08BB4"), "fb"))
    save("fan_bow_ball", poke_ball())
    save("fan_arch", arch())
    save("ss_plinth", N9.panels(hexc("#56657A"), "ssp"))
    save("ss_leg", L.metal(hexc("#2C3A55"), seam=False))
    save("ss_tower", N9.panels(hexc("#EEF1F4"), "sst"))
    save("ss_tower_dark", N9.panels(hexc("#2F4C7E"), "sstd"))
    save("cave_dark", doorway())


# ---------- Route 17 ----------
def tarmac():
    """Route 17's road: dark tarmac, fine grit, a couple of scuffs and a patch (laid over four blocks)."""
    size = 64
    r = random.Random("tar")
    n = L._noise(size, size, "tarn", 6)
    im = Image.new("RGBA", (size, size))
    base = hexc("#3D4046")
    scuffs = [(10, 18, 22, 3), (38, 44, 18, 2), (50, 9, 9, 2)]
    for y in range(size):
        for x in range(size):
            k = (n[y][x] - 0.5) * 0.08 + (r.random() - 0.5) * 0.07
            if r.random() < 0.03: k += 0.12
            for sx, sy, ln, w in scuffs:
                t = (x - sx)
                if 0 <= t <= ln and abs((y - sy) - t * 0.12) < w * (1 - abs(t - ln / 2) / (ln / 2 + 1)): k += 0.06
            if 22 <= x <= 33 and 40 <= y <= 50: k -= 0.05
            im.putpixel((x, y), shade(base, k)[:3] + (255,))
    return im


def route17():
    save("lamp_r17_post", L.metal(hexc("#E06A2C"), seam=False))
    save("lamp_r17_cap", L.metal(hexc("#C9571F"), seam=False))
    save("lamp_r17_chisel", N9.chisel(hexc("#E06A2C"), hexc("#F59A5E"), hexc("#9C3F14")))
    save("bench_slat", K.boards(hexc("#E5B24A"), "bs", vertical=False))
    save("bench_frame", L.metal(hexc("#3B4047"), seam=False))
    save("pergola", K.boards(hexc("#F3EFE7"), "pg", lines=False, vertical=True))
    save("pergola_top", K7.step_stone(hexc("#F3EFE7")))
    save("tarmac", tarmac())


# ---------- Route 12's piers ----------
def pier_boards():
    """Let's Go's pier planks: yellow-tan boards across, each its own shade, in a darker frame."""
    r = random.Random("pier")
    im = Image.new("RGBA", (16, 16))
    base, frame = hexc("#DDB565"), hexc("#9E7334")
    shades = [r.uniform(-0.06, 0.06) for _ in range(4)]
    for y in range(16):
        for x in range(16):
            row = y // 4
            k = shades[row] + (r.random() - 0.5) * 0.04 + (0.06 if y % 4 == 0 else 0)
            c = shade(base, k)
            if y % 4 == 3: c = shade(base, -0.28)
            if x in (0, 15) or y in (0, 15): c = frame
            im.putpixel((x, y), tuple(c[:3]) + (255,))
    return im


def piers():
    save("pier_boards", pier_boards())
    save("pier_frame", K.boards(hexc("#A87B3A"), "pf", vertical=False))
    save("pier_bolt", L.metal(hexc("#9AA2AC"), seam=False))


# ---------- the rocks in the water ----------
def sea_rocks():
    g = hexc("#878C94")
    save("sea_rock", L.rock_face(g, shade(g, 0.2), shade(g, -0.38), "sr", cells=18))
    save("sea_rock_top", L.rock_face(shade(g, 0.08), shade(g, 0.28), shade(g, -0.3), "srt", cells=26))


# ---------- Routes 12 and 16's house, Bill's, the gates ----------
def houses():
    pal = K.roof_pal("#C4593C", "#8A3624")
    save("roof_h16_tiles", L.roof_tiles(pal))
    save("roof_h16_edge", L.roof_edge(pal))
    save("roof_h16_under", L.solid(pal["under"]))
    save("wall_h16", N9.panels(hexc("#F2E8D6"), "wh16", joint=False))
    save("post_h16", K.boards(hexc("#7A5236"), "ph16", lines=False, vertical=True))
    save("bill_timber", K.boards(hexc("#6E4A30"), "btm", vertical=True))
    # The Underground Path huts' warm cream brick (21:58, as Let's Go's entrances).
    save("ug_brick", L.bricks(hexc("#E4CF8E"), hexc("#BFA868")))


# ---------- the pack's plants (Shards' models) ----------
def plants():
    models = os.path.join(PACK, "models", "block")
    for name in ("short_dry_grass", "tall_dry_grass", "torchflower", "template_azalea", "bush"):
        shutil.copy(os.path.join(SHARDS, "models", "block", name + ".json"), os.path.join(models, name + ".json"))
    # (Minecraft's own textures for these: his models only, the dry grass as John's beach has it, the bush's leaves
    # tinted as Minecraft's are.)


def build():
    leafy()
    hedges()
    hill_top()
    power_plant()
    doors()
    vermilion()
    route17()
    piers()
    sea_rocks()
    houses()
    plants()
    bike()


if __name__ == "__main__":
    build()


# ---------- the bikes outside the Bike Shop (20:12) ----------
def bike():
    """A bike standing side on (along x), a block long, above the block it's put on: two round wheels (pixel rings,
    a hub), a red frame, a black seat and handlebars."""
    save("bike_red", panel(hexc("#D8382E"), "bkr", 0.1))
    save("bike_black", panel(hexc("#26282C"), "bkb", 0.06))
    save("bike_steel", L.metal(hexc("#B8C0C8"), seam=False))
    els = []
    def box(a, b, tex):
        els.append({"from": a, "to": b, "faces": {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}})
    for cx in (4.0, 12.0):
        for px in range(16):
            for py in range(16):
                x, y = px + 0.5, py + 0.5
                d = math.hypot(x - cx, y - 19.5 + 16)
                if 2.3 <= d <= 3.3:
                    box([px, 16 + py, 7.4], [px + 1, 17 + py, 8.6], "tyre")
        box([cx - 0.5, 19.0, 7.2], [cx + 0.5, 20.0, 8.8], "steel")
    box([4.0, 19.2, 7.6], [12.0, 20.0, 8.4], "frame")      # the chain stay to the front fork
    box([5.0, 20.0, 7.6], [10.8, 21.0, 8.4], "frame")      # the top tube
    box([5.0, 19.0, 7.6], [6.0, 23.5, 8.4], "frame")       # the seat post
    box([3.6, 23.5, 7.1], [7.0, 24.3, 8.9], "tyre")       # the seat
    box([11.0, 19.0, 7.6], [11.8, 24.5, 8.4], "frame")    # the head tube and fork
    box([10.6, 24.5, 5.4], [12.2, 25.2, 10.6], "tyre")    # the handlebars
    box([7.4, 18.6, 7.3], [8.6, 19.8, 8.7], "steel")       # the chain wheel
    models = os.path.join(ASSETS, "models", "block", "area")
    json.dump({"ambientocclusion": False, "textures": {"tyre": "retromon:block/area/bike_black", "frame": "retromon:block/area/bike_red",
                                                       "steel": "retromon:block/area/bike_steel", "particle": "retromon:block/area/bike_red"}, "elements": els},
              open(os.path.join(models, "prop_bike.json"), "w"), indent=1)
