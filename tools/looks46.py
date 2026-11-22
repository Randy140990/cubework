"""
0.46.0's looks (John's 15:21 and 15:29 rounds), drawn for client/AreaLooks (textures/block/area) and the Retromon Look
pack (its plants): every door cleaner in its own colours, the doorbell, glass that runs on as one pane, the tunnels'
mouths as Let's Go's, marble feet and clean quartz on the Centers, Marts and Gyms, the lamps' tops, the dark window
panels, the cliffs and ledges as rock, the steps as boards, metal ladders, boulders, and Fuchsia City (its houses,
its red path, its walls, planters, bridge, the Safari Zone's buildings, its beach).

    (called from lookarea.build())
"""
import math
import os
import random

from PIL import Image, ImageFilter

import lookarea as L
import lookground

V = lambda n: os.path.join(L.VANILLA, "textures", n + ".png")
LOOK = os.path.join(L.HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft", "textures")
hexc, mix, save = L.hexc, L.mix, L.save


def shade(c, k):
    return mix(c, (255, 255, 255), k) if k >= 0 else mix(c, (0, 0, 0), -k)


def avg(path):
    im = Image.open(path).convert("RGBA")
    px = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 0]
    return tuple(sum(p[i] for p in px) // len(px) for i in range(3)) + (255,)


def look_or_vanilla(name):
    p = os.path.join(LOOK, "block", name + ".png")
    return p if os.path.exists(p) else V("block/" + name)


def noise(size, seed, cells):
    return L._noise(size, size, seed, cells)


# ---------- doors (15:21): every door cleaner, its own colour, shape and handles kept ----------
DOORS = ("acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle", "mangrove", "oak", "pale_oak", "spruce", "warped",
         "iron", "copper", "exposed_copper", "weathered_copper", "oxidized_copper")


def door_clean(src):
    """A door's picture with its wood (or metal) made clean: the same shape, holes and windows (its see-through parts
    kept), its handle and hinges kept as they are, the rest smoothed and put on a few even tones of its own colour,
    an outline round its panels, so it reads as a painted door rather than grainy planks."""
    im = Image.open(src).convert("RGBA")
    w, h = im.size
    p = im.load()
    opaque = [(x, y) for y in range(h) for x in range(w) if p[x, y][3] > 0]
    cols = sorted(p[xy][:3] for xy in opaque)
    med = cols[len(cols) // 2]
    dist = lambda c: abs(c[0] - med[0]) + abs(c[1] - med[1]) + abs(c[2] - med[2])
    sat = lambda c: max(c) - min(c)
    # The handle and hinges: few pixels far from the door's own colour (greyer or much darker/lighter), in small clumps.
    odd = {xy for xy in opaque if dist(p[xy]) > 120 and (abs(sat(p[xy]) - sat(med)) > 40 or dist(p[xy]) > 190)}
    if len(odd) > len(opaque) * 0.14:
        odd = set()
    lum = {xy: 0.3 * p[xy][0] + 0.59 * p[xy][1] + 0.11 * p[xy][2] for xy in opaque}
    wood = [xy for xy in opaque if xy not in odd]
    if not wood:
        return im
    base = tuple(sorted(p[xy][c] for xy in wood)[len(wood) // 2] for c in range(3)) + (255,)
    # The tone round each pixel (a little blur), then five even steps by where it falls among the door's own tones
    # (percentiles, so a door of soft contrast keeps its panels as well as a bold one).
    blur = {}
    for (x, y) in wood:
        acc, n = 0.0, 0
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                xx, yy = x + dx, y + dy
                if (xx, yy) in lum and (xx, yy) not in odd:
                    wgt = 4 if dx == 0 and dy == 0 else 1
                    acc += lum[(xx, yy)] * wgt; n += wgt
        blur[(x, y)] = acc / n
    vals = sorted(blur.values())
    cut = [vals[int(len(vals) * f)] for f in (0.12, 0.36, 0.68, 0.9)]
    steps = (-0.3, -0.14, -0.02, 0.08, 0.17)
    out = im.copy()
    q = out.load()
    for (x, y) in wood:
        b = blur[(x, y)]
        k = steps[sum(1 for c in cut if b > c)]
        edge = any(not (0 <= x + dx < w and 0 <= y + dy < h) or p[x + dx, y + dy][3] == 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            k -= 0.08
        q[x, y] = shade(base, k)[:3] + (255,)
    for xy in odd:
        q[xy] = p[xy][:3] + (255,)
    return out


def doors():
    for d in DOORS:
        for half in ("top", "bottom"):
            save("door_%s_%s" % (d, half), door_clean(V("block/%s_door_%s" % (d, half))))


def door_swaps():
    """The swap spec for every door (old=new), and the blocks (waxed ones the same pictures)."""
    spec, blocks = [], []
    for d in DOORS:
        for half in ("top", "bottom"):
            spec.append("block/%s_door_%s=door_%s_%s" % (d, half, d, half))
        blocks.append(d + "_door")
        if "copper" in d:
            blocks.append("waxed_" + d + "_door")
    return ",".join(spec), ",".join(blocks)


def doorbell():
    """The stone button by a door (15:21, John's doorbell): a little silver plate, a round white push in it with a dark
    ring (on the part of the picture a button shows), its edge a darker silver."""
    im = Image.new("RGBA", (16, 16), hexc("#AEB4BA"))
    for y in range(16):
        for x in range(16):
            c = hexc("#B9BFC5") if y < 12 else hexc("#7E858C")
            if 5 <= x <= 10 and 6 <= y <= 9:
                d = math.hypot(x - 7.5, y - 7.5)
                c = hexc("#F7F7F2") if d < 1.3 else hexc("#4C535B") if d < 1.9 else hexc("#C9CED3") if (x == 5 or y == 6) else hexc("#9AA1A8")
            im.putpixel((x, y), c)
    return im


def bolt():
    """The bridge's stone buttons (its bolts): dark iron with a lit top."""
    im = Image.new("RGBA", (16, 16), hexc("#3D4247"))
    for y in range(16):
        for x in range(16):
            if 5 <= x <= 10 and 6 <= y <= 9:
                im.putpixel((x, y), hexc("#6C737A") if (x <= 7 and y <= 7) else hexc("#4A5056"))
    return im


# ---------- glass that runs on (15:21): tinted, framed round the outside only ----------
GLASS = {
    # name: (tint, how see-through inside, frame, the frame's lit inner line)
    "glass_clear": (hexc("#CFE8EE"), 70, hexc("#F4F7F8"), hexc("#DCEBF0")),
    "glass_blue": (hexc("#8FC4E6"), 105, hexc("#F2F6F8"), hexc("#C7E1F1")),
    "glass_cyan": (hexc("#7ACBD0"), 110, hexc("#EEF5F5"), hexc("#BFE5E7")),
    "glass_white": (hexc("#E9EEF0"), 150, hexc("#FFFFFF"), hexc("#F3F6F7")),
    "glass_deep": (hexc("#4E7FC4"), 130, hexc("#E8EEF5"), hexc("#9DB8DE")),
    "glass_smoke": (hexc("#2A3038"), 175, hexc("#4A525C"), hexc("#5F6873")),
}
GLASS_FROM = {"glass": "glass_clear", "light_blue_stained_glass": "glass_blue", "cyan_stained_glass": "glass_cyan",
              "white_stained_glass": "glass_white", "blue_stained_glass": "glass_deep", "black_stained_glass": "glass_smoke"}


def glass_tile(name, mask):
    tint, a_in, frame, hi = GLASS[name]
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            # The pane: its tint, a soft broad sheen low across it (no streaks), a touch lighter at the top.
            k = 0.12 * (1 - y / 15) + (0.10 if 9 <= x + y <= 14 else 0.0)
            c = shade(tint, k)[:3] + (a_in + (18 if 9 <= x + y <= 14 else 0),)
            top, right, bottom, left = mask & 1, mask & 2, mask & 4, mask & 8
            if (top and y == 0) or (bottom and y == 15) or (left and x == 0) or (right and x == 15):
                c = frame[:3] + (255,)
            elif (top and y == 1) or (bottom and y == 14) or (left and x == 1) or (right and x == 14):
                c = hi[:3] + (210,)
            im.putpixel((x, y), c)
    return im


def glass_textures():
    for name in GLASS:
        for m in range(16):
            save("%s_%d" % (name, m), glass_tile(name, m))


def glass_spec():
    return ",".join("block/%s=%s" % (k, v) for k, v in GLASS_FROM.items())


GLASS_BLOCKS = ("glass,glass_pane,light_blue_stained_glass,light_blue_stained_glass_pane,cyan_stained_glass,cyan_stained_glass_pane,white_stained_glass,"
                "white_stained_glass_pane,blue_stained_glass,blue_stained_glass_pane,black_stained_glass,black_stained_glass_pane")


# ---------- the tunnels' mouths, as Let's Go's (15:21) ----------
def tunnel_stone():
    """Stone bricks round a cave's mouth as Let's Go's concrete portal: big smooth pale grey blocks, two courses a
    block, staggered, each lit along its top and left, a fine dark join."""
    r = random.Random("ts")
    base = hexc("#A9ADB3")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        course, k = divmod(y, 8)
        off = 8 * (course % 2)
        for x in range(16):
            xx = (x + off) % 16
            if k == 7 or xx == 0:
                c = shade(base, -0.24)
            else:
                kk = (0.07 if k == 0 or xx == 1 else -0.06 if k == 6 or xx == 15 else 0.0) + (r.random() - 0.5) * 0.025
                c = shade(base, kk)
            im.putpixel((x, y), c)
    return im


def tunnel_cap(side=False):
    base = hexc("#BABDC2")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if side:
                k = 0.1 if y <= 1 else -0.22 if y >= 7 and y <= 8 else -0.05 if y > 8 else 0.0
            else:
                k = 0.08 if (x == 0 or y == 0) else -0.1 if (x == 15 or y == 15) else 0.0
            im.putpixel((x, y), shade(base, k))
    return im


def tunnel_dark():
    r = random.Random("td")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), shade(hexc("#17191E"), (r.random() - 0.5) * 0.06))
    return im


# ---------- marble feet, clean quartz (15:21) ----------
def marble(base, vein, seed, size=32):
    """Marble laid over two blocks: its colour with soft drifting veins of a grey, a faint cloud in it."""
    n1, n2 = noise(size, seed, 4), noise(size, seed + "b", 9)
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = abs(math.sin((x * 0.21 + y * 0.13) * math.pi + n1[y][x] * 7.0))
            k = (n2[y][x] - 0.5) * 0.06
            c = shade(base, k)
            if v < 0.07:
                c = mix(c, vein, 0.55)
            elif v < 0.16:
                c = mix(c, vein, 0.22)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def quartz_clean(kind="block"):
    """John's quartz on the Centers' and Marts' fronts: clean white stone, a soft bevel round each block (a pillar's
    a fine line down each side)."""
    base = hexc("#F1F0EB")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.0
            if kind == "pillar":
                k = 0.05 if x in (1, 2) else -0.08 if x in (13, 14) else -0.12 if x in (0, 15) else 0.0
            else:
                k = 0.05 if (x == 0 or y == 0) else -0.09 if (x == 15 or y == 15) else 0.0
            im.putpixel((x, y), shade(base, k))
    return im


def white_step():
    """Pewter's planters' steps (quartz stairs): white stone, its front edge a soft grey line."""
    base = hexc("#EEEDE7")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.06 if y == 0 else -0.12 if y in (7, 15) else -0.04 if y in (8,) else 0.0
            im.putpixel((x, y), shade(base, k))
    return im


# ---------- the lamps (15:21): the tops, the chiselled ends, the glass ----------
def lamp_chisel(base, top=False):
    """The lamp's chiselled ends (tuff, nether brick): one dark metal, a bevelled band and a ring set into it, so it
    still reads as chiselled, cleanly."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if top:
                d = math.hypot(x - 7.5, y - 7.5)
                k = 0.08 if (x == 0 or y == 0) else -0.12 if (x == 15 or y == 15) else (0.1 if 4.5 < d < 5.5 else -0.1 if 5.5 <= d < 6.4 else 0.0)
            else:
                k = 0.1 if y in (0, 1) else -0.16 if y in (14, 15) else (0.08 if y in (4, 11) else -0.12 if y in (5, 12) else 0.0)
                if 6 <= y <= 9 and x in (3, 12):
                    k = -0.14
            im.putpixel((x, y), shade(base, k))
    return im


# ---------- the dark window panels (15:21) ----------
def window_low():
    """The dark slabs under John's windows (polished deepslate slabs, dark prismarine): dark blue grey glass in a
    slim frame, a soft reflection across it."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = mix(hexc("#3E4C5C"), hexc("#26303B"), y / 15)
            if 6 <= x + y <= 11:
                c = shade(c, 0.12)
            if x in (0, 15) or y in (0, 15):
                c = hexc("#5B6670") if (x == 0 or y == 0) else hexc("#20272F")
            im.putpixel((x, y), c[:3] + (255,))
    return im


# ---------- the cliffs and ledges, the steps (15:21) ----------
def step_boards(base):
    """The mud brick steps up the ledges: long boards laid lengthways, a dark gap between each, a joint here and there,
    each board lit along its top edge."""
    r = random.Random("sb")
    im = Image.new("RGBA", (16, 16))
    joints = {0: 11, 1: 4, 2: 13, 3: 7}
    for y in range(16):
        board, k = divmod(y, 4)
        for x in range(16):
            if k == 3:
                c = shade(base, -0.3)
            elif x == joints[board]:
                c = shade(base, -0.22)
            else:
                kk = (0.09 if k == 0 else -0.04 if k == 2 else 0.0) + (0.03 if (x // 4 + board) % 2 else -0.02) + (r.random() - 0.5) * 0.03
                c = shade(base, kk)
            im.putpixel((x, y), c)
    return im


def ashlar(base, seed):
    """A wall's dressed stone (the low walls with chains): two courses of clean blocks a block, staggered, lit on top."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        course, k = divmod(y, 8)
        off = 6 * (course % 2)
        for x in range(16):
            xx = (x + off) % 16
            if k == 7 or xx in (0, 9):
                c = shade(base, -0.2)
            else:
                c = shade(base, (0.08 if k == 0 else -0.05 if k == 6 else 0.0) + (r.random() - 0.5) * 0.03)
            im.putpixel((x, y), c)
    return im


def metal_ladder():
    """Ladders as metal ones (15:21): two steel rails, round rungs between, lit on their left."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rail, rail_hi, rail_lo, rung, rung_lo = hexc("#8C959E"), hexc("#B9C1C9"), hexc("#5E666F"), hexc("#AEB6BE"), hexc("#737B84")
    for y in range(16):
        for x in (1, 2, 13, 14):
            im.putpixel((x, y), rail_hi if x in (1, 13) else rail)
        im.putpixel((3, y), rail_lo); im.putpixel((15, y), rail_lo)
    for y0 in (1, 5, 9, 13):
        for x in range(3, 13):
            im.putpixel((x, y0), rung)
            im.putpixel((x, y0 + 1), rung_lo)
    return im


# ---------- Fuchsia City (15:29) ----------
FU_ROOFS = {
    "roof_fu1": ("#3B4048", "#2C3036"),  # charcoal
    "roof_fu2": ("#323C52", "#262E40"),  # navy
    "roof_fu3": ("#45364A", "#352838"),  # plum
    "roof_fu4": ("#36433E", "#29332F"),  # slate green
}
FU_WALLS = {"fu_wall_1": "#EFE7D8", "fu_wall_2": "#DFE7EE", "fu_wall_3": "#E2E9DB", "fu_wall_4": "#EFE2DF"}


def roof_pal(a, b):
    a, b = hexc(a), hexc(b)
    return {"hi": shade(a, 0.16), "base": a, "mid": shade(a, -0.04), "low": shade(a, -0.08), "shadow": b, "seam": shade(a, -0.1),
            "edge_top": shade(b, 0.06), "edge_bottom": shade(b, -0.12), "under": shade(b, -0.2)}


def boards(base, seed, lines=True, vertical=False):
    """Clean painted boards (the Safari buildings' walls, a wooden bridge): even boards, a fine line between."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            a = x if vertical else y
            k = (0.06 if a % 4 == 0 else -0.1 if a % 4 == 3 else 0.0) if lines else 0.0
            im.putpixel((x, y), shade(base, k + (r.random() - 0.5) * 0.02))
    return im


def red_pave(size=32):
    """Fuchsia's red path (the patterned red glazed terracotta), as Let's Go's: bright red pavers in a basket weave,
    pairs of bricks turning each way, a light grout, laid over two blocks so the weave runs on."""
    r = random.Random("rp")
    base, light, dark, grout = hexc("#DE3F36"), hexc("#F2685A"), hexc("#B32A28"), hexc("#F3B0A0")
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            cx, cy = x // 8, y // 8
            lx, ly = x % 8, y % 8
            horiz = (cx + cy) % 2 == 0
            a, b = (lx, ly) if horiz else (ly, lx)
            if b in (3, 7) or a == 7:
                c = grout
            else:
                pick = (cx * 7 + cy * 13 + (b // 4) * 5) % 5
                c = light if pick == 0 else dark if pick == 1 else base
                c = shade(c, (0.06 if b in (0, 4) else -0.05 if b in (2, 6) else 0.0) + (r.random() - 0.5) * 0.03)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def beach_sand(size=64):
    """Sand laid over four blocks (15:29: not repeating every block): its warm colour, soft ripples and drifts, grains."""
    n1, n2 = noise(size, "bs", 3), noise(size, "bs2", 11)
    r = random.Random("bsg")
    base, light, dark = hexc("#EBD9A9"), hexc("#F6EBCB"), hexc("#D2BD86")
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            k = (n1[y][x] - 0.5) * 0.3 + math.sin((y + n2[y][x] * 9) * 0.5) * 0.05 + (r.random() - 0.5) * 0.05
            c = mix(base, light, k * 1.8) if k > 0 else mix(base, dark, -k * 1.8)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def soil():
    """A planter's earth (coarse dirt): rich dark soil, crumbs of it lighter and darker."""
    r = random.Random("so")
    base = hexc("#5B3D25")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.random()
            im.putpixel((x, y), shade(base, 0.14 if v < 0.12 else -0.18 if v > 0.86 else (v - 0.5) * 0.08))
    return im


def dry_grass(tall):
    """The beach's dry grass, as Let's Go's: a tuft of pale straw blades, lit at their tips (15:29)."""
    r = random.Random("dg" + str(tall))
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    straw, straw_hi, straw_lo = hexc("#D9C38A"), hexc("#F2E4B4"), hexc("#A88E57")
    top = 1 if tall else 6
    for i in range(9 if tall else 7):
        x0 = 2 + i * (12 / (8 if tall else 6))
        lean = (r.random() - 0.5) * 4
        h = top + r.randint(0, 3)
        for y in range(15, h - 1, -1):
            t = (15 - y) / max(1, 15 - h)
            x = int(round(x0 + lean * t))
            if 0 <= x < 16:
                im.putpixel((x, y), (straw_hi if t > 0.75 else straw if t > 0.25 else straw_lo))
    return im


def safari():
    """The Safari Zone's two buildings (15:29), in the FireRed gate's colours: a sandy yellow tiled roof, its rim a
    darker gold, cream boards, green planters, warm wood over the door, a slate post, a shiny pale floor."""
    pal = roof_pal("#E5C46E", "#B8913F")
    save("roof_safari_tiles", L.roof_tiles(pal)); save("roof_safari_edge", L.roof_edge(pal)); save("roof_safari_under", L.solid(pal["under"]))
    save("sz_roof", L.flat(hexc("#E2C06A")))
    save("sz_roof_rim", L.metal(hexc("#B08A44"), seam=False))
    save("sz_wall", boards(hexc("#F2EAD6"), "szw", vertical=True))
    save("sz_green", L.solid_shaded(hexc("#5FA24E"), 0.06, -0.08))
    save("sz_green_shelf", L.recolour_flat(V("block/warped_shelf"), hexc("#5FA24E")))
    save("sz_wood", boards(hexc("#C38D55"), "szwood"))
    save("sz_post", L.metal(hexc("#4C545E"), seam=False))
    save("sz_floor", floor_shiny(hexc("#DCDFE2")))


def floor_shiny(base):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.1 if 5 <= x + y <= 8 else 0.04 if 9 <= x + y <= 10 else 0.0
            k += 0.05 if (x == 0 or y == 0) else -0.08 if (x == 15 or y == 15) else 0.0
            im.putpixel((x, y), shade(base, k))
    return im


def fuchsia():
    for name, (a, b) in FU_ROOFS.items():
        pal = roof_pal(a, b)
        save(name + "_tiles", L.roof_tiles(pal))
        save(name + "_edge", L.roof_edge(pal))
        save(name + "_under", L.solid(pal["under"]))
    for name, c in FU_WALLS.items():
        save(name, L.solid_shaded(hexc(c), 0.05, -0.06, 0.01, name))
    save("fu_beam", boards(hexc("#4A3324"), "fb", vertical=True))
    save("fu_beam_top", L.solid_shaded(hexc("#5A3F2C"), 0.04, -0.06))
    save("fu_plinth", L.solid_shaded(hexc("#30353C"), 0.04, -0.08, 0.02, "fp"))
    save("red_pave", red_pave())
    save("red_kerb", L.solid_shaded(hexc("#9E2E2A"), 0.08, -0.1, 0.02, "rk"))
    save("wall_tan", ashlar(avg(look_or_vanilla("mud_bricks")), "wt"))
    save("wall_grey", ashlar(hexc("#8C9196"), "wg"))
    save("shelf_wood", L.recolour_flat(V("block/dark_oak_shelf"), hexc("#7E5232")))
    save("planter_soil", soil())
    save("bridge_boards", boards(hexc("#C79A62"), "bb", vertical=True))
    save("bridge_beam", L.solid_shaded(hexc("#9C7246"), 0.06, -0.08, 0.02, "bbm"))
    save("bolt", bolt())
    save("beach_sand", beach_sand())
    save("sand_step", L.solid_shaded(hexc("#E8D39E"), 0.08, -0.1, 0.02, "ss"))
    save("lamp_plum_post", L.metal(hexc("#5C3040"), seam=False))
    save("lamp_plum_chisel", lamp_chisel(hexc("#5C3040")))
    save("lamp_plum_chisel_top", lamp_chisel(hexc("#5C3040"), top=True))
    save("lamp_plum_cap", L.metal(hexc("#4A2634"), seam=False))
    safari()


def rock_textures():
    """The cliffs and ledges (dripstone, and the jungle stairs John uses for them, 15:21): rock, kept their colour; the
    mud brick slabs round the ponds the same rock in their own colour; the steps (mud brick stairs) boards."""
    d = avg(look_or_vanilla("dripstone_block"))
    save("drip_rock", L.rock_face(d, shade(d, 0.18), shade(d, -0.3), "kr", cells=16, strata=0.03))
    save("drip_rock_top", L.rock_face(shade(d, 0.04), shade(d, 0.2), shade(d, -0.22), "kt", cells=30))
    m = avg(look_or_vanilla("mud_bricks"))
    save("mud_rock", L.rock_face(m, shade(m, 0.18), shade(m, -0.3), "mrk", cells=16, strata=0.02))
    save("step_boards", step_boards(m))
    save("boulder_stone", L.rock_face(hexc("#9EA0A2"), hexc("#C4C6C8"), hexc("#626568"), "bst", size=32, cells=8))
    save("boulder_stone_top", L.rock_face(hexc("#AAACAE"), hexc("#CDCFD1"), hexc("#6E7174"), "bsu", size=32, cells=10))


def plants():
    """The look pack's torchflowers and sunflowers (15:29), as the other flowers: their leaves on Retromon's greens,
    their petals cleaner and brighter; the dry grass as straw tufts."""
    import lookplants
    tex = os.path.join(LOOK, "block")
    for n in ("torchflower", "sunflower_front", "sunflower_back", "sunflower_top", "sunflower_bottom"):
        lookplants.flower_kept(V("block/" + n), os.path.join(tex, n + ".png"))
    dry_grass(True).save(os.path.join(tex, "tall_dry_grass.png"))
    dry_grass(False).save(os.path.join(tex, "short_dry_grass.png"))
    # The spruce wall signs round the Museum (15:21): clean wood (their board and post).
    sign_dir = os.path.join(LOOK, "entity", "signs")
    os.makedirs(sign_dir, exist_ok=True)
    L.recolour_flat(V("entity/signs/spruce"), hexc("#8A5E3A"), keep=0.25).save(os.path.join(sign_dir, "spruce.png"))


def build():
    doors()
    save("doorbell", doorbell())
    glass_textures()
    save("tunnel_stone", tunnel_stone())
    save("tunnel_cap", tunnel_cap())
    save("tunnel_cap_side", tunnel_cap(True))
    save("tunnel_dark", tunnel_dark())
    save("marble_center", marble(hexc("#F4EFE9"), hexc("#B9B2AA"), "mc"))
    save("marble_mart", marble(hexc("#EEF2F6"), hexc("#A8B3C0"), "mm"))
    save("marble_gym", marble(hexc("#F0EEEA"), hexc("#ADA9A3"), "mg"))
    save("quartz_clean", quartz_clean())
    save("quartz_clean_pillar", quartz_clean("pillar"))
    save("white_step", white_step())
    save("lamp_chisel", lamp_chisel(hexc("#3A3F46")))
    save("lamp_chisel_top", lamp_chisel(hexc("#3A3F46"), top=True))
    save("window_low", window_low())
    save("metal_ladder", metal_ladder())
    save("window_shelf", L.recolour_flat(V("block/pale_oak_shelf"), hexc("#EDE6D6")))
    save("wood_shelf_dark", L.recolour_flat(V("block/spruce_shelf"), hexc("#7A5232")))
    rock_textures()
    fuchsia()
    plants()
