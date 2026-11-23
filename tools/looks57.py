"""
0.57.0's looks (John's review, 15:17 to 16:35): the pictures for tools/arealooks57.py's round57() and the look pack.

Across the map: every door redrawn crisp (each wood and metal its own colour, panels, a window, a brass handle); the
shelves John trims with (chimney caps, bands, planters) as plain boards; the moss's soft edge onto the cobblestone; the
lamps' caps and feet plain; the Gyms' dark middle column; the volcano's steam and flowing lava; the dripstone hanging in
the caves' mouths; the copper lanterns as the garden lights. Then each place: Cinnabar, Violet and its SPROUT TOWER,
Mr. Berry's house, Mahogany and the Lake of Rage, Ecruteak (its houses in red, the BURNED TOWER, the TIN TOWER, the
DANCE THEATRE, its arch), the NATIONAL PARK, the MOOMOO FARM, Olivine and its lighthouse, Cianwood, Azalea, the Day Care.

Run: called from tools/lookpack.py (looks48.EXTRA).
"""

import json
import math
import os
import random

from PIL import Image

import lookarea as L
import lookground
import looks46 as K
import looks48b as B
import looks49 as V
import looks50 as F
import looks52 as S
import looks54 as J
import looks56 as N

hexc, mix, save = L.hexc, L.mix, L.save
shade = B.shade
AREA_TEX = J.AREA_TEX
LOOK = os.path.join(L.HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft", "textures")
ENTITY = os.path.join(L.HERE, "..", "src", "main", "resources", "assets", "retromon", "textures", "entity")


def _img(w=16, h=None):
    return Image.new("RGBA", (w, h or w))


def _put(im, x, y, c, a=255):
    im.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in c[:3]) + (a,))


def area(name):
    return Image.open(os.path.join(AREA_TEX, name + ".png")).convert("RGBA")


def mean_of(name):
    p = os.path.join(AREA_TEX, name + ".png")
    if not os.path.exists(p): return None
    im = Image.open(p).convert("RGBA")
    px = [c for c in im.getdata() if c[3] > 0]
    if not px: return None
    return tuple(sum(c[i] for c in px) // len(px) for i in range(3)) + (255,)


def board(base, seed, edge=0.08):
    """A plain board (the shelves as trims, 0.57): one clean colour, the faintest grain along it, lit along its top,
    shaded along its foot, so a band of them reads as one solid trim."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.02 + 0.015 * math.sin(x * 0.9 + y * 0.15)
            if y == 0: k += edge
            elif y == 15: k -= edge * 1.2
            _put(im, x, y, shade(base, k))
    return im


# ---------- doors (15:46: "the bamboo doors definitely need updating, top and bottom ... probably the same on all doors") ----------
WOODS = {"oak": "#B0824F", "spruce": "#76523A", "birch": "#DCCB96", "jungle": "#A9704E", "acacia": "#BE6236", "dark_oak": "#56392A",
         "mangrove": "#7E302D", "cherry": "#E3A8A2", "pale_oak": "#DCD5C8", "bamboo": "#CFB75C", "crimson": "#7E3552", "warped": "#2E7C77"}
METALS = {"iron": "#C8CDD3", "copper": "#C7744F", "exposed_copper": "#AE866E", "weathered_copper": "#62A080", "oxidized_copper": "#53A894"}


def crisp_door(base, half, metal=False):
    """A door half, crisp (0.57): a darker frame, two inset panels below, a window up top (a small one on a metal door,
    its rivets), a brass handle; one even colour, lit along its left, shaded along its right."""
    frame, inset, glass = shade(base, -0.3), shade(base, -0.12), hexc("#BFE3EE")
    brass = hexc("#E2B84A") if not metal else hexc("#3B3F45")
    im = _img()
    top = half == "top"
    for y in range(16):
        for x in range(16):
            c = base
            if x in (0, 15) or (top and y == 0) or (not top and y == 15): c = frame
            elif x == 1 or (top and y == 1): c = shade(base, 0.12)
            elif x == 14: c = shade(base, -0.08)
            elif top and not metal and 4 <= x <= 11 and 3 <= y <= 10:
                c = frame if x in (4, 11) or y in (3, 10) else (shade(glass, 0.12) if x + y < 14 else glass)
            elif top and metal and 5 <= x <= 10 and 3 <= y <= 7:
                c = frame if x in (5, 10) or y in (3, 7) else glass
            elif (top and 4 <= x <= 11 and 12 <= y <= 15) or (not top and 4 <= x <= 11 and 0 <= y <= 12):
                c = inset
                if x == 4 or (top and y == 12): c = shade(inset, 0.1)
                elif x == 11 or (not top and y == 12): c = shade(inset, -0.1)
            if metal and (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): c = shade(base, -0.22)
            if not top and x in (12, 13) and 1 <= y <= 3: c = brass if y < 3 else shade(brass, -0.25)
            _put(im, x, y, c)
    return im


def doors():
    for d, col in WOODS.items():
        for half in ("top", "bottom"): save("door_%s_%s" % (d, half), crisp_door(hexc(col), half))
    for d, col in METALS.items():
        for half in ("top", "bottom"): save("door_%s_%s" % (d, half), crisp_door(hexc(col), half, metal=True))


# ---------- shelves as boards (15:46) ----------
SHELF_BOARDS = {"green_shelf": "#5EA24C", "wood_shelf": "#B98A55", "sz_green_shelf": "#4E8E3A", "shelf_wood": "#7A5232", "cap_shelf": "#8E8F8C",
                "band_shelf": "#A87652", "wood_shelf_dark": "#5A3B24"}


def shelves():
    for n, c in SHELF_BOARDS.items(): save(n, board(hexc(c), n))
    for n, c in (("panel_planter_dark", "#4E3426"), ("panel_cap_dark", "#5A5D61"), ("panel_white", "#E8E6E0"), ("panel_teal", "#2F8A86"),
                 ("panel_red", "#A8383A"), ("panel_gold", "#D9A93A"), ("panel_cream", "#E5D8B8"), ("panel_blue", "#3E6FB4"), ("panel_wood_red", "#8C3E2C")):
        save(n, board(hexc(c), n))


# ---------- the moss onto the cobblestone (15:46: "the cobblestone and mossy cobblestone ... blend together") ----------
def moss_edges():
    moss = Image.open(os.path.join(LOOK, "block", "mossy_cobblestone.png")).convert("RGBA")
    r = random.Random("moss57")
    g = _img()
    base = hexc("#6CAA46")
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.14
            _put(g, x, y, shade(base, k))
    for t in range(1, 18):
        save("moss_on_cobble_%d" % t, lookground.fringe(t, g))


# ---------- the volcano (15:57) ----------
def steam():
    """Cinnabar's steam (15:57: "opacity 30% and kind of make them a mist"): soft white haze, a little patchy, laid over
    four blocks, about a third see-through."""
    size = 64
    im = Image.new("RGBA", (size, size))
    r = random.Random("steam57")
    pts = [(r.random() * size, r.random() * size, 6 + r.random() * 10) for _ in range(26)]
    vals = {}
    for y in range(size):
        for x in range(size):
            v = 0.0
            for (px, py, rad) in pts:
                dx = min(abs(x - px), size - abs(x - px)); dy = min(abs(y - py), size - abs(y - py))
                v += math.exp(-(dx * dx + dy * dy) / (2 * rad * rad))
            vals[x, y] = v
    lo, hi = min(vals.values()), max(vals.values())
    for y in range(size):
        for x in range(size):
            v = (vals[x, y] - lo) / max(1e-6, hi - lo)
            a = int(40 + 58 * v)
            c = mix(hexc("#E8ECEF"), hexc("#FFFFFF"), v)
            im.putpixel((x, y), tuple(int(q) for q in c[:3]) + (a,))
    save("steam", im)


def lava_frames(size=32, frames=16, seed="lava57"):
    """Flowing lava (15:57: "retexturize the lava ... look like they're flowing"): bright molten orange running down,
    darker crust streaks drifting with it, a hot yellow core; the frames one above the other (an animation)."""
    r = random.Random(seed)
    streaks = [(r.random() * size, r.random() * size, 1.5 + r.random() * 2.5, r.random() * 6.28) for _ in range(14)]
    sheet = Image.new("RGBA", (size, size * frames))
    for f in range(frames):
        shift = f * size / frames
        for y in range(size):
            for x in range(size):
                yy = (y - shift) % size
                v = 0.5 + 0.25 * math.sin((x / size) * 6.28 * 2 + (yy / size) * 6.28) + 0.25 * math.sin((yy / size) * 6.28 * 3 + x * 0.3)
                crust = 0.0
                for (sx, sy, w, ph) in streaks:
                    dx = min(abs(x - sx), size - abs(x - sx))
                    dy = min(abs(yy - sy), size - abs(yy - sy))
                    if dy < 7: crust = max(crust, math.exp(-dx * dx / (2 * w * w)) * (1 - dy / 7))
                hot = mix(hexc("#FF7A1E"), hexc("#FFD24A"), max(0, min(1, v)))
                c = mix(hot, hexc("#B8361C"), min(0.8, crust * 0.9))
                sheet.putpixel((x, y + f * size), tuple(int(q) for q in c[:3]) + (255,))
    return sheet


def lava():
    sheet = lava_frames()
    save("lava_flow57", sheet)
    with open(os.path.join(AREA_TEX, "lava_flow57.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, fh)
    # The look pack's lava itself (John's lava pools on the volcano): the same molten flow, a block's worth.
    blk = os.path.join(LOOK, "block")
    os.makedirs(blk, exist_ok=True)
    still = lava_frames(16, 20, "lava57s")
    still.save(os.path.join(blk, "lava_still.png"))
    with open(os.path.join(blk, "lava_still.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh)
    flow = lava_frames(32, 16, "lava57f")
    flow.save(os.path.join(blk, "lava_flow.png"))
    with open(os.path.join(blk, "lava_flow.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh)


# ---------- lamps (15:57: "more of a solid colour instead of pattern"; "the bottom of the lights so they don't have like weird patterns") ----------
LAMP_CAPS = ("lamp_cap", "lamp_gray_wool", "lamp_ver_cap", "lamp_ver_top", "lamp_white_cap", "lamp_saf_cap", "lamp_saf_cap_top", "lamp_cel_cap_top",
             "lamp_cg_cap", "lamp_plum_cap", "lamp_r17_cap", "plamp_cap", "lamp_lav_top", "lamp_gold_cap_top", "lamp_cer_top")
LAMP_FEET = ("lamp_chisel", "lamp_chisel_top", "lamp_cel_chisel", "lamp_cel_chisel_top", "lamp_lav_chisel", "lamp_lav_chisel_top", "lamp_plum_chisel",
             "lamp_plum_chisel_top", "lamp_saf_chisel", "lamp_saf_chisel_top", "lamp_r17_chisel", "lamp_cg_chisel", "lamp_chiseled_tuff_bricks",
             "lamp_chiseled_tuff_bricks_top", "plamp_chiseled_tuff_bricks", "plamp_chiseled_tuff_bricks_top", "plamp_foot")


def lamp_plain(base, foot):
    """A lamp's cap (or its foot) as one clean colour: lit along its top; a foot a single darker band round it."""
    im = _img()
    for y in range(16):
        for x in range(16):
            k = 0.0
            if y == 0 or x == 0: k += 0.08
            if y == 15 or x == 15: k -= 0.1
            if foot and y in (3, 12): k -= 0.12
            _put(im, x, y, shade(base, k))
    return im


def lamps():
    for n in LAMP_CAPS:
        c = mean_of(n)
        if c: save(n, lamp_plain(c, False))
    for n in LAMP_FEET:
        c = mean_of(n)
        if c: save(n, lamp_plain(c, True))
    # Johto's other lamps: the white ones by the sea (Olivine, Cianwood, the coast), the NATIONAL PARK's (16:35:
    # "prefer not to be the red ... something different to complement the swan": white marble posts, a pale gold cap).
    for n, c in (("lamp_sea_post", "#E9ECEE"), ("lamp_sea_cap", "#4F8FC9"), ("lamp_sea_foot", "#C9D0D6"), ("lamp_park_post", "#EEEDE8"),
                 ("lamp_park_cap", "#D8BE72"), ("lamp_park_foot", "#D9D6CE"), ("lamp_north_post", "#3C4248"), ("lamp_north_cap", "#2F3439"),
                 ("lamp_north_foot", "#4A5056")):
        save(n, lamp_plain(hexc(c), n.endswith("foot")))


# ---------- the Gyms, the Underground huts (15:46, 15:57) ----------
def plain_bits():
    save("gym_divider", F.panel(hexc("#3C4148"), "gymd", 0.05))
    save("hut_dark", F.panel(hexc("#1E2126"), "hutd", 0.0))
    save("rod_clean", L.metal(hexc("#E8EAEC"), seam=False))
    # Planters' cherry saplings (15:46: "updating to something, even if you want to create a model"): a round
    # flowering shrub, pink blossom on a dark green bush (a plant's cross picture).
    im = _img()
    r = random.Random("shrub57")
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, (y - 8) * 1.15)
            if d < 7.2 and y < 15:
                k = (r.random() - 0.5) * 0.2 - (0.15 if y > 10 else 0)
                c = shade(hexc("#3F7A3A"), k)
                if r.random() < 0.32 and y < 12: c = shade(hexc("#F2A9C4"), (r.random() - 0.5) * 0.2)
                _put(im, x, y, c)
            elif 7 <= x <= 8 and y >= 13:
                _put(im, x, y, hexc("#5A4030"))
    save("shrub_pink", im)


# ---------- the caves' dripstone (16:04: "re-texturize the dripstone") and the copper lanterns ----------
def stalactites():
    """The pointed dripstone in the caves' mouths: clean grey stone points, lit along one side (the look pack's)."""
    blk = os.path.join(LOOK, "block")
    van = os.path.join(L.VANILLA, "textures", "block")
    for part in ("tip", "frustum", "middle", "base", "tip_merge"):
        for d in ("down", "up"):
            n = "pointed_dripstone_%s_%s.png" % (d, part)
            src = os.path.join(van, n)
            if not os.path.exists(src): continue
            im = Image.open(src).convert("RGBA")
            p = im.load()
            for y in range(im.height):
                for x in range(im.width):
                    a = p[x, y][3]
                    if a == 0: continue
                    k = 0.1 if x < im.width * 0.45 else -0.06
                    k += 0.04 * math.sin(y * 0.8)
                    p[x, y] = shade(hexc("#8E9193"), k)[:3] + (a,)
            im.save(os.path.join(blk, n))


# ---------- Cinnabar (15:51, 15:57) ----------
def cinnabar():
    # The piers, as Let's Go's: pale grey boards, a white edge with an orange line, dark posts, a rope.
    save("cin_deck", J.panel(hexc("#B9BEC2"), "cind"))
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#F2F2EF") if y < 6 else hexc("#E8752E") if y < 8 else hexc("#ECEBE6")
            if y in (0, 15): c = shade(c, -0.1)
            _put(im, x, y, c)
    save("cin_edge", im)
    save("cin_post", S.post(hexc("#3A3F46")))
    save("cin_post_top", S.post(hexc("#3A3F46"), top=True))
    N.shelf_of("iron_chain", hexc("#C9A66B"), "rope_tan", keep=0.45)
    # The lamps: white posts, an orange cap, a warm globe.
    save("lamp_cin_post", lamp_plain(hexc("#EDEDEA"), False))
    save("lamp_cin_cap", lamp_plain(hexc("#E8752E"), False))
    save("lamp_cin_foot", lamp_plain(hexc("#D5D6D2"), True))
    # The POKeMON MANSION: cream render, grey stone base, dark brown roof tiles, shutters, a white trim.
    save("mansion_wall", J.render_wall(hexc("#E9E1CF"), "manw"))
    save("mansion_base", K.ashlar(hexc("#9EA3A5"), "manb"))
    for a, b in (("roof_lav_tiles", "roof_mansion_tiles"), ("roof_lav_edge", "roof_mansion_edge"), ("roof_lav_under", "roof_mansion_under")):
        N.tint_of(a, b, hexc("#5B4436"))
    save("mansion_shutter", L.shutter(hexc("#D9D2C2")))
    save("mansion_trim", board(hexc("#F1EEE6"), "mant"))
    save("mansion_dark", F.panel(hexc("#2A2C30"), "mand", 0.03))
    # The LAB: white panels, a dark grey roof, copper-red trim, posts.
    save("lab_cin_wall", J.panel(hexc("#F1F2F2"), "labw"))
    save("lab_cin_roof", J.panel(hexc("#4D5359"), "labr"))
    save("lab_cin_trim", board(hexc("#B5553A"), "labt"))
    save("lab_cin_post", S.post(hexc("#6B7178")))
    # The solar panels on the LAB's roof (15:51: "I obviously didn't add like solar panels or anything").
    solar_prop()
    # Its paths: the brown edge, the brown powder as the path.
    save("cin_path_edge", J.panel(hexc("#8A6A4C"), "cinpe"))


def solar_prop():
    """A row of solar panels on a frame, tilted to the sun (a prop over the block it's on): blue cells in a silver frame."""
    cells = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#C9CED3") if x in (0, 15) or y in (0, 15) else hexc("#9AA3AD") if x % 5 == 0 or y % 5 == 0 else hexc("#2D4F8A")
            if c == hexc("#2D4F8A") and (x + y) % 7 == 0: c = hexc("#4A6FB0")
            _put(cells, x, y, c)
    save("solar_cells", cells)
    save("solar_frame", L.metal(hexc("#A9B0B7"), seam=False))
    els = []
    def box(a, b, tex, rot=None):
        e = {"from": a, "to": b, "faces": {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}}
        if rot: e["rotation"] = rot
        els.append(e)
    box([1, 16, 2], [15, 17, 14], "cells", {"origin": [8, 16, 8], "axis": "x", "angle": 22.5})
    for x in (2, 13):
        box([x, 16, 3], [x + 1, 19, 4], "frame")
        box([x, 16, 12], [x + 1, 17, 13], "frame")
    L.write_json(os.path.join(L.MODELS, "prop_solar.json"),
                 {"ambientocclusion": False, "textures": {"cells": "retromon:block/area/solar_cells", "frame": "retromon:block/area/solar_frame",
                                                          "particle": "retromon:block/area/solar_frame"}, "elements": els})


# ---------- the INDIGO PLATEAU's statues (15:46: "go with the statues like inside the Victory Road house ... the Pokeball") ----------
def ball_statue():
    """A stone POKe BALL on a stone pedestal's top (laid over John's pedestal, the copper golem gone)."""
    els = []
    def box(a, b, tex, top=None):
        faces = {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "down")}
        faces["up"] = {"texture": "#" + (top or tex)}
        els.append({"from": a, "to": b, "faces": faces})
    box([1.5, 16, 1.5], [14.5, 17.5, 14.5], "gold")
    box([3, 17.5, 3], [13, 22.5, 13], "ball")
    box([2.6, 22.5, 2.6], [13.4, 23.6, 13.4], "band")
    box([3, 23.6, 3], [13, 28.6, 13], "ball", "ball_top")
    for a, b in (([6.5, 21.6, 13.2], [9.5, 24.6, 13.8]), ([6.5, 21.6, 2.2], [9.5, 24.6, 2.8]), ([13.2, 21.6, 6.5], [13.8, 24.6, 9.5]), ([2.2, 21.6, 6.5], [2.8, 24.6, 9.5])):
        box(a, b, "gold")
    L.write_json(os.path.join(L.MODELS, "prop_ball_statue.json"),
                 {"ambientocclusion": False,
                  "textures": {"ball": "retromon:block/area/statue_ball", "ball_top": "retromon:block/area/statue_ball_top", "band": "retromon:block/area/statue_band",
                               "gold": "retromon:block/area/statue_gold", "particle": "retromon:block/area/statue_stone"}, "elements": els})
    # The copper golems hidden (the look pack's statue picture clear), the ball statues in their place.
    p = os.path.join(LOOK, "entity", "copper_golem")
    os.makedirs(p, exist_ok=True)
    for n in ("weathered_copper_golem.png",):
        Image.new("RGBA", (64, 64), (0, 0, 0, 0)).save(os.path.join(p, n))


# ---------- roofs: one tiled look recoloured for each place ----------
ROOFS = {"vio": "#6D3C86", "ecru": "#A8382F", "black": "#34383E", "mah": "#8A3A2E", "white": "#ECEFF1", "mangrove": "#9A3530", "daycare": "#9E4A3A",
         "dance": "#3B67B8", "azalea1": "#6E8A4A", "azalea2": "#4E6E9A", "farm": "#B03A30", "lake": "#7E3A30", "r26": "#C9653A"}


def roofs():
    for k, c in ROOFS.items():
        for part in ("tiles", "edge", "under"):
            N.tint_of("roof_lav_" + part, "roof_%s_%s" % (k, part), hexc(c))


# ---------- Violet City and the SPROUT TOWER (16:15) ----------
def violet():
    save("violet_pave", J.setts(hexc("#A9A6AE"), "vpave"))
    save("violet_kerb", F.panel(hexc("#8D8893"), "vkerb", 0.06))
    save("vio_bridge", K.step_boards(hexc("#8C7A92")))
    save("vio_bridge_beam", S.dojo_wood(hexc("#5C4A62"), "vbb"))
    save("vio_isle", V.flagstones(32, "visle", hexc("#8F8A96"), 0.12, hexc("#6E6876")))
    # Its stone lanterns either side of the bridge (HeartGold's, 16:15): grey stone, a dark window lit warm, a cap.
    save("lantern_stone", K.ashlar(hexc("#A3A6A8"), "lnst"))
    save("lantern_stone_top", B.cap_top(hexc("#B3B6B8")))
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#A3A6A8")
            if 3 <= x <= 12 and 4 <= y <= 11: c = hexc("#2B2622") if (x in (3, 12) or y in (4, 11)) else mix(hexc("#F6C36A"), hexc("#E39A3B"), (y - 4) / 8)
            if y in (0, 15) or x in (0, 15): c = shade(hexc("#A3A6A8"), -0.12)
            _put(im, x, y, c)
    save("lantern_stone_window", im)
    # The houses: purple tiles (ROOFS), dark timber, the chimney's dark brick with a gold trim.
    save("vio_timber", S.dojo_wood(hexc("#3E2A25"), "vtim"))
    save("chimney_dark", L.bricks(hexc("#5A3436"), hexc("#3A2426")))
    save("gold_trim", S.shiny(hexc("#E0B040"), "gtr"))
    # The SPROUT TOWER: dark timber, white plaster, purple roof tiers, wood floors, a stone plinth, paper screens.
    tower("sprout", "#3D2A24", "#6D3C86", "#4B2A55")


def tower(k, timber, roof, ridge):
    save(k + "_timber", S.dojo_wood(hexc(timber), k + "t"))
    save(k + "_pillar", S.dojo_wood(shade(hexc(timber), 0.06), k + "p", vertical=True))
    save(k + "_plaster", J.render_wall(hexc("#EEEAE0"), k + "w"))
    save(k + "_roof", B.tiles(hexc(roof), shade(hexc(roof), -0.3), 4, k + "r"))
    save(k + "_ridge", F.panel(hexc(ridge), k + "g", 0.08))
    save(k + "_floor", K.boards(hexc("#9A6E45"), k + "f"))
    save(k + "_stone", K.ashlar(hexc("#A6A39C"), k + "s"))
    save(k + "_trim", board(hexc("#F0EEE8"), k + "m"))
    save(k + "_rail", board(shade(hexc(timber), 0.1), k + "l"))
    save(k + "_beam", board(hexc("#6A4630"), k + "b"))
    save(k + "_screen", S.shoji())


# ---------- Ecruteak (16:23) ----------
def ecruteak():
    save("ecru_flag", V.flagstones(32, "ecfl", hexc("#B4AEA6"), 0.12, hexc("#8E8780")))
    save("ecru_plot", L.cave_ground(hexc("#A36A52"), hexc("#B57A60"), hexc("#8E5A46"), "ecplot", size=32, speck=0.03, patches=0.3))
    save("ecru_kerb", F.panel(hexc("#9D978F"), "eckerb", 0.06))
    save("ecru_plaster", J.render_wall(hexc("#F0EAE0"), "ecwall"))
    save("ecru_timber", S.dojo_wood(hexc("#4A2E24"), "ectim"))
    save("ecru_base", F.panel(hexc("#B9B5AE"), "ecbase", 0.06))
    save("lacquer_red", F.panel(hexc("#B8352C"), "lacr", 0.07))
    save("lacquer_dark", F.panel(hexc("#3A2421"), "lacd", 0.06))
    # The TIN TOWER: deep red lacquer pillars, dark timber, bronze-red roof tiers with gold ridges.
    tower("tin", "#3A2420", "#9C4630", "#C79A3A")
    save("tin_pillar", F.panel(hexc("#A8362C"), "tinp", 0.05))
    # The BURNED TOWER: charred timber, its teal roof scorched, soot on the plaster.
    tower("burned", "#2C2522", "#3E6E68", "#2E4A47")
    save("burned_plaster", J.render_wall(hexc("#CFC8BC"), "bwall"))
    save("burned_teal", B.tiles(hexc("#3E6E68"), hexc("#263D3A"), 4, "bteal"))
    save("burned_char", F.panel(hexc("#2E3335"), "bchar", 0.04))
    save("burned_rubble", L.rock_face(hexc("#6E6964"), hexc("#86807A"), hexc("#55504C"), "brub", size=32))
    # The DANCE THEATRE (16:23: "more blue, red and white ... I put like teal at the top, and I'll let you dictate"):
    # a blue tiled roof (ROOFS), red lacquered beams and posts, white walls, a gold ridge.
    save("dance_wall", J.render_wall(hexc("#F2F0EA"), "dwall"))
    save("dance_ridge", S.shiny(hexc("#E0B040"), "dridge"))
    # The red house by the BURNED TOWER: its striped roof one deep red, its front lacquer.
    save("redb_roof", B.tiles(hexc("#9C2E2A"), hexc("#5E1C1A"), 4, "redbr"))
    save("redb_front", F.panel(hexc("#C8532E"), "redbf", 0.07))
    # The banners' poles (16:35: "a universal way to maybe hold the banners"): a lacquered pole with gold ends.
    N.shelf_of("iron_chain", hexc("#5A3A2C"), "banner_cord", keep=0.4)
    save("banner_pole", F.panel(hexc("#3E2A22"), "bpole", 0.06))
    save("banner_pole_top", S.shiny(hexc("#E0B040"), "bpt"))


# ---------- the NATIONAL PARK (16:23, 16:35) ----------
def park():
    save("swan_marble", K.marble(hexc("#F3F2EE"), hexc("#C9CED4"), "swan", 32))
    save("park_stone", V.flagstones(32, "pkst", hexc("#B9B6AE"), 0.1, hexc("#97938B")))
    save("park_cobble", J.setts(hexc("#A8A49B"), "pkcb"))


# ---------- Mr. Berry's house (16:15), Mahogany, the Lake of Rage (16:04) ----------
def houses():
    save("berry_wall", J.render_wall(hexc("#B6744E"), "brwall"))
    save("berry_brick", L.bricks(hexc("#8E3E32"), hexc("#5C2A24")))
    save("berry_post", S.dojo_wood(hexc("#3A2A22"), "brpost"))
    save("mah_post", S.dojo_wood(hexc("#5A2E28"), "mhpost"))
    save("mah_pave", J.setts(hexc("#A7A29A"), "mhpave"))
    # The Lake of Rage's GYARADOS (16:04: "pick a primary colour ... solid and a bit gradient, and obviously flow"):
    # its red body flowing over four blocks, the darker red, the cream belly, white teeth, dark eyes, the mouth.
    save("gy_red", flow_gradient(hexc("#D23A30"), hexc("#E85A3C"), "gyr"))
    save("gy_red_dark", flow_gradient(hexc("#9A2622"), hexc("#B8302A"), "gyd"))
    save("gy_cream", flow_gradient(hexc("#F1E1B8"), hexc("#E6CF9A"), "gyc"))
    save("gy_teeth", F.panel(hexc("#F8F8F4"), "gyt", 0.05))
    save("gy_eye", F.panel(hexc("#1E2024"), "gye", 0.03))
    save("gy_mouth", flow_gradient(hexc("#C9506A"), hexc("#A8384E"), "gym"))
    save("gy_fin", flow_gradient(hexc("#F1E1B8"), hexc("#D9BE84"), "gyf"))
    save("sea_glow", F.panel(hexc("#CFEFF2"), "sglow", 0.04))
    save("coral_clean", F.panel(hexc("#D85F86"), "corl", 0.05))
    save("coral_clean_b", F.panel(hexc("#3E7FD8"), "corb", 0.05))


def flow_gradient(a, b, seed, size=64):
    """A colour laid over four blocks that runs from one shade to another in long soft waves (the statue's scales flow)."""
    r = random.Random(seed)
    im = _img(size)
    for y in range(size):
        for x in range(size):
            t = 0.5 + 0.35 * math.sin((x + y * 0.6) / size * 6.28) + 0.15 * math.sin(y / size * 6.28 * 2)
            c = mix(a, b, max(0, min(1, t)))
            k = (r.random() - 0.5) * 0.03 - (0.06 if (y % 8 == 7 and (x // 8 + y // 8) % 2 == 0) else 0)
            _put(im, x, y, shade(c, k))
    return im


# ---------- Olivine, its lighthouse, Cianwood, the Day Care, the farm (16:23, 16:35) ----------
def coast():
    save("lh_white", J.render_wall(hexc("#F3F4F2"), "lhw"))
    save("lh_teal", F.panel(hexc("#2E8D97"), "lht", 0.07))
    save("lh_teal_light", F.panel(hexc("#78C4CC"), "lhtl", 0.06))
    save("lh_stone", K.ashlar(hexc("#B2B4B3"), "lhs"))
    save("lh_rail", L.metal(hexc("#4E8D8A"), seam=False))
    save("pier_ol", K.step_boards(hexc("#B9A27A")))
    save("pier_ol_stone", V.flagstones(32, "pols", hexc("#B6B2A8"), 0.1, hexc("#908C84")))
    save("bench_slats", K.boards(hexc("#9C6A3E"), "bench57", vertical=False))
    save("umbrella_a", F.panel(hexc("#3E8CD8"), "umba", 0.04))
    save("umbrella_b", F.panel(hexc("#F2F2EC"), "umbb", 0.04))
    # Cianwood (16:35: "white roofs ... the blue and the white sort of trim").
    save("cian_trim", board(hexc("#3E78C8"), "cntr"))
    # The Day Care (Route 34): its red roof (ROOFS), pale walls, green foot.
    save("daycare_wall", J.render_wall(hexc("#EDE6D6"), "dcw"))
    save("daycare_foot", F.panel(hexc("#4C8A4A"), "dcf", 0.06))
    # The MOOMOO FARM (16:23: "where you've got on the barn ... the dripstone top on that"): red barn boards, white
    # trim, dark timbers, the roof red tiles (ROOFS).
    save("barn_red", K.boards(hexc("#A8392F"), "barnr", vertical=True))
    save("barn_trim", board(hexc("#F0EDE6"), "barnt"))
    save("barn_timber", S.dojo_wood(hexc("#4A3326"), "barnw"))
    save("barn_slats", K.boards(hexc("#C9A46A"), "barns"))
    save("farm_wall", J.render_wall(hexc("#EFE9DC"), "farmw"))


# ---------- the bikers' motorbike (15:51) ----------
def bike_texture():
    im = Image.new("RGBA", (64, 64))
    cols = [hexc("#2B2E34"), hexc("#C9302C"), hexc("#1C1D21"), hexc("#B7BEC7")]
    r = random.Random("bike57")
    for q, c in enumerate(cols):
        x0, y0 = (q % 2) * 32, (q // 2) * 32
        for y in range(32):
            for x in range(32):
                k = (r.random() - 0.5) * 0.04 + (0.1 if y % 8 == 0 and q in (1, 3) else 0)
                _put(im, x0 + x, y0 + y, shade(c, k))
    im.save(os.path.join(ENTITY, "npc_bike.png"))


def stands():
    """The stands with plant pots (16:04: "tailor every one of those throughout to be the same"): dark timber posts and
    frame, a clean wooden counter, a slatted wooden roof (over John's campfires, their fire not shown)."""
    save("stand_post", S.dojo_wood(hexc("#4A3326"), "stp"))
    save("stand_counter", K.boards(hexc("#B07A48"), "stc"))
    save("stand_roof", K.boards(hexc("#8A5A36"), "str", vertical=False))
    save("clear", Image.new("RGBA", (16, 16), (0, 0, 0, 0)))
    # The Fan Club's bow (15:46: "some sort of bow tie"): its lobes plain glossy pink boards.
    save("fan_bow", S.shiny(hexc("#F07FB0"), "fbow"))


def build():
    doors()
    shelves()
    moss_edges()
    steam()
    lava()
    lamps()
    plain_bits()
    stalactites()
    roofs()
    cinnabar()
    ball_statue()
    violet()
    ecruteak()
    park()
    houses()
    coast()
    bike_texture()
    stands()
