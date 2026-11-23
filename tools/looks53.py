"""
0.53.0's looks (John's 11:16 round): the pictures for tools/arealooks.py's round53() and the look pack.

- The INDIGO PLATEAU: its roof's stone walls (diorite) in its cream stone, its orange corner pillars (resin bricks) a
  deeper red brick; the copper golem statues on its plaza no longer golems ("change those models to something else"):
  a POKé BALL on a gold stand on each pedestal, the golem itself not drawn.
- ROUTE 23's statues: John's own blocks again (his statues are as Let's Go's), in statue stone with a polished gold
  trim on top, in place of the model laid over them (it didn't read: its top was dark).
- The cliffs: the stripped jungle wood in them (Route 22's "brown") as the cliffs' rock and earthy tops.
- Saffron and Celadon: the oak stairs under the windows as stone sills; the doors new glass doors in a pale metal
  frame; the GAME CORNER's sign glossy enamel in its colour; Celadon's lamps one seamless post; its fountain in
  pale dressed stone; its benches proper benches (wooden slats, dark green iron ends); its trims flat; the pond by
  the house quartz all through, no blue and white tiles.

Run: called from tools/lookpack.py (looks48.EXTRA).
"""

import json
import os
import random

from PIL import Image

import lookarea as L
import looks46 as K
import looks48b as B
import looks49 as V
import looks52 as S

hexc, mix, save = L.hexc, L.mix, L.save
shade = B.shade


def _img(size=16):
    return Image.new("RGBA", (size, size))


def _put(im, x, y, c, a=255):
    im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (a,))


def sill(base, seed):
    """A stone sill under a window: lit along its top, a soft face, a fine shadow line under its lip."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.02
            if y <= 1: k += 0.12
            elif y == 2: k += 0.05
            elif y == 4: k -= 0.16
            elif y == 15: k -= 0.08
            if x == 0: k -= 0.04
            _put(im, x, y, shade(base, k))
    return im


def door_glass(top):
    """A new glass door (Let's Go's shops and houses): a pale metal frame, a tall pane of soft blue glass with a
    light streak across it; the top half its frame's top rail, the bottom half a push bar and a kick plate."""
    frame, edge, glass, glass_hi = hexc("#DADFE4"), hexc("#A9B2BB"), hexc("#8EBFDC"), hexc("#D3EBF7")
    im = _img()
    for y in range(16):
        for x in range(16):
            c = glass
            k = 0.08 * (1 - y / 15) if top else -0.06 * (y / 15)
            c = shade(c, k)
            d = (x + (y if top else y + 16)) % 13
            if d in (5, 6): c = mix(c, glass_hi, 0.55)
            if x <= 1 or x >= 14: c = frame if x in (0, 15) else edge
            if top and y <= 1: c = frame if y == 0 else edge
            if not top:
                if 4 <= y <= 5 and 2 <= x <= 13: c = hexc("#C9D0D7") if y == 4 else hexc("#8E98A2")
                if y >= 12: c = frame if y == 12 else shade(hexc("#C3CAD1"), -0.04 * (y - 12))
            _put(im, x, y, c)
    return im


def bench_seat(base, seed):
    """A bench's seat: warm wooden slats along it, dark gaps between, a fine grain."""
    r = random.Random(seed)
    im = _img()
    gap = shade(base, -0.45)
    for y in range(16):
        for x in range(16):
            if y % 4 == 3:
                _put(im, x, y, gap)
                continue
            k = (r.random() - 0.5) * 0.05 + (0.08 if y % 4 == 0 else 0)
            if r.random() < 0.06: k -= 0.08
            _put(im, x, y, shade(base, k))
    return im


def bench_end(base):
    """A bench's end: dark green iron, its frame lit, a scroll of lighter iron in the middle."""
    im = _img()
    for y in range(16):
        for x in range(16):
            k = -0.06
            if x <= 1 or x >= 14 or y <= 1 or y >= 14: k = 0.08 if (x == 0 or y == 0) else 0.0
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 10 and (x - 7.5) ** 2 + (y - 7.5) ** 2 > 4: k = 0.16
            _put(im, x, y, shade(base, k))
    return im


def statue_crown():
    """The top of a statue on ROUTE 23 (John's middle top block): its stone, a polished gold band round it and a
    gold edge on top."""
    stone, gold = hexc("#C9CCCC"), hexc("#E0B040")
    r = random.Random("scr")
    im = _img()
    for y in range(16):
        for x in range(16):
            if y <= 1 or 7 <= y <= 9:
                k = 0.3 if y in (0, 7) else 0.1 if y in (1, 8) else -0.12
                if (x + y) % 9 in (2, 3): k += 0.2
                _put(im, x, y, shade(gold, k))
            else:
                _put(im, x, y, shade(stone, (r.random() - 0.5) * 0.03 + (0.06 if y in (2, 10) else -0.06 if y == 15 else 0)))
    return im


def trophy_model():
    """A POKé BALL on a gold stand (10:41, 11:16: the plateau's golem statues "something else"), laid on each one's
    pedestal and standing in the statue's own block: red over white, polished, a dark band and a gold button."""
    els = []

    def box(a, b, tex, top=None):
        faces = {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "down")}
        faces["up"] = {"texture": "#" + (top or tex)}
        els.append({"from": a, "to": b, "faces": faces})

    box([4, 16, 4], [12, 17.5, 12], "gold")
    box([6, 17.5, 6], [10, 18.5, 10], "gold")
    # The ball (three boxes crossing, so it's round), white below its band and red above.
    for lo, hi, tex in ((18.5, 23.5, "white"), (24.5, 29.5, "red")):
        box([3.5, lo + 1, 4.5], [12.5, hi - 1, 11.5], tex)
        box([4.5, lo + 1, 3.5], [11.5, hi - 1, 12.5], tex)
        box([4.5, lo, 4.5], [11.5, hi, 11.5], tex)
    box([3.3, 23.5, 3.3], [12.7, 24.5, 12.7], "band")
    for a, b in (([6.5, 22.6, 12.4], [9.5, 25.4, 13.2]), ([6.5, 22.6, 2.8], [9.5, 25.4, 3.6]), ([12.4, 22.6, 6.5], [13.2, 25.4, 9.5]),
                 ([2.8, 22.6, 6.5], [3.6, 25.4, 9.5])):
        box(a, b, "gold")
    model = {"ambientocclusion": False,
             "textures": {"gold": "retromon:block/area/statue_gold", "white": "retromon:block/area/trophy_white",
                          "red": "retromon:block/area/trophy_red", "band": "retromon:block/area/statue_band",
                          "particle": "retromon:block/area/statue_gold"},
             "elements": els}
    L.write_json(os.path.join(L.MODELS, "prop_trophy.json"), model)


def golems_away():
    """The copper golems themselves not drawn (their pedestals carry the POKé BALLs): the look pack's weathered
    copper golem left clear."""
    out = os.path.join(L.HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft", "textures", "entity",
                       "copper_golem")
    src = os.path.join(L.VANILLA, "textures", "entity", "copper_golem", "weathered_copper_golem.png")
    os.makedirs(out, exist_ok=True)
    size = Image.open(src).size if os.path.exists(src) else (64, 64)
    Image.new("RGBA", size, (0, 0, 0, 0)).save(os.path.join(out, "weathered_copper_golem.png"))


def cel_trim():
    """Celadon's green trims (John's shelves under the windows, 11:16: "don't make it shelving, make it more flat"):
    one flat green, lit along its top edge only."""
    base = hexc("#5EA14C")
    im = _img()
    for y in range(16):
        for x in range(16):
            _put(im, x, y, shade(base, 0.14 if y == 0 else -0.08 if y == 15 else 0.0))
    return im


def pale_tiles():
    """Celadon's bands of tiles (11:16: no blue and white checker): large warm white tiles, fine soft joints."""
    return B.tiles(hexc("#F1EEE6"), hexc("#DAD3C4"), 2, "ptl")


# ---------- Goldenrod City (Johto: 11:16, its buildings Celadon's) ----------
def gold_fence():
    """Goldenrod's fences (11:16: "a different bit of white"): crisp white painted rails, a gold cap on each post."""
    white, gold = hexc("#F6F5F0"), hexc("#E0B040")
    r = random.Random("gf")
    im = _img()
    for y in range(16):
        for x in range(16):
            if y <= 1:
                _put(im, x, y, shade(gold, 0.18 if y == 0 else -0.04))
            else:
                _put(im, x, y, shade(white, (r.random() - 0.5) * 0.02 + (-0.06 if x in (0, 15) else 0)))
    return im


def gold_pillar(top=False):
    """Goldenrod's white columns: fluted white stone, a gold ring at the head and foot (the top: white, a gold ring)."""
    white, gold, groove = hexc("#F4F2EC"), hexc("#E0B040"), hexc("#D6D0C2")
    im = _img()
    for y in range(16):
        for x in range(16):
            if top:
                d = max(abs(x - 7.5), abs(y - 7.5))
                c = shade(gold, 0.1) if 5.5 < d < 7 else white
            elif y <= 1 or y >= 14:
                c = shade(gold, 0.2 if y in (0, 14) else -0.05)
            else:
                c = groove if x % 4 == 0 else shade(white, 0.04 if x % 4 == 1 else 0.0)
            _put(im, x, y, c)
    return im


def build_goldenrod():
    save("gold_road", L.bricks(hexc("#A9573E"), hexc("#D8B49A")))
    save("gold_pave", B.tiles(hexc("#E8CF98"), hexc("#C7A15E"), 2, "gpv"))
    save("gold_kerb", B.slab_pave(hexc("#EDEAE2"), "gk"))
    save("gold_fence", gold_fence())
    save("gold_pillar", gold_pillar())
    save("gold_pillar_top", gold_pillar(True))
    save("gold_trim", S.shiny(hexc("#E0B040"), "gtr"))
    save("lamp_gold_post", L.metal(hexc("#5A4630"), seam=False))
    save("lamp_gold_cap_top", B.cap_top(hexc("#E0B040")))
    save("lamp_globe_amber", B.globe(hexc("#FFC46B")))
    rod = os.path.join(L.VANILLA, "textures", "block", "end_rod.png")
    if os.path.exists(rod):
        g = hexc("#E0B040")
        save("gold_rod", L.recolour_soft(rod, [shade(g, -0.45), shade(g, -0.15), g, shade(g, 0.35), (255, 246, 214)], 80.0))


def build():
    save("sill_saf", sill(hexc("#EFE3C6"), "ss"))
    save("sill_cel", sill(hexc("#F1F1EC"), "sc"))
    save("door_glass_top", door_glass(True))
    save("door_glass_bottom", door_glass(False))
    save("gc_symbol", S.shiny(hexc("#E5602F"), "gcs"))
    save("cel_fountain_stone", K.ashlar(hexc("#E6E1D6"), "cfs"))
    save("bench_seat_cel", bench_seat(hexc("#C98F55"), "bsc"))
    save("bench_end_cel", bench_end(hexc("#2F5D3F")))
    save("cel_trim", cel_trim())
    save("cel_tile_band", pale_tiles())
    save("plateau_brick_dark", L.bricks(hexc("#93402F"), hexc("#E2D0BA")))
    save("statue_crown", statue_crown())
    save("trophy_white", S.shiny(hexc("#EEF0F2"), "trw"))
    save("trophy_red", S.shiny(hexc("#D8393A"), "trr"))
    trophy_model()
    golems_away()
    build_goldenrod()


if __name__ == "__main__":
    build()
