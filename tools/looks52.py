"""
0.52.0's looks: Saffron City in Let's Go Pikachu's colours (John's 09:13 and 09:26), the pictures for
tools/arealooks.py's round52() and the look pack.

Let's Go's Saffron (John's reference): yellow walls with cream bands, green roofs, the red and white Center; the
pavement John laid in four colours (white, yellow, ochre and orange blocks in pairs) as clean bevelled pavers; the
roads tarmac; the street lamps no longer black (warm white posts, green caps, warm frosted glass); the planters and
shelves each a colour of their own; SILPH CO. in white panels and blue glass; the FIGHTING DOJO's roof in slate tiles
with dark timber and white plaster; the rooftops' units and vents as metal.

Run: called from tools/lookpack.py (looks48.EXTRA).
"""

import os
import random

from PIL import Image

import lookarea as L
import looks46 as K
import looks48b as B
import looks49 as V
import looks50 as F

hexc, mix, save = L.hexc, L.mix, L.save
shade = B.shade


def _img(size=16):
    return Image.new("RGBA", (size, size))


def _put(im, x, y, c):
    im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))


def paver(base, seed):
    """John's paving (laid @2): long pavers a block deep and two blocks long, each one's joint in the middle of the
    picture (his pairs start on the odd blocks), a soft bevel, a fine light grout."""
    r = random.Random(seed)
    im = _img(32)
    grout = shade(base, 0.28)
    for y in range(32):
        for x in range(32):
            ly = y % 16
            k = (r.random() - 0.5) * 0.03
            if x == 15 or ly == 15:
                c = grout
            else:
                if ly == 0 or x == 16: k += 0.1
                elif ly == 14 or x == 14: k -= 0.1
                elif ly == 1 or x == 17: k += 0.04
                c = shade(base, k)
            _put(im, x, y, c)
    return im


def wall(base, seed, joints=False):
    """A painted wall (Let's Go's smooth render): one colour, the faintest grain, a soft lit top; (joints) a fine
    join every half block, as panels."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.018
            if joints and y in (7, 15): k -= 0.07
            if joints and y in (0, 8): k += 0.03
            _put(im, x, y, shade(base, k))
    return im


def band(base, seed):
    """A cream band or pilaster: lit along its left and top, a shade along its right and foot."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.02
            if x == 0 or y == 0: k += 0.08
            if x == 15 or y == 15: k -= 0.1
            _put(im, x, y, shade(base, k))
    return im


def roof(base, seed):
    """Let's Go's green flat roof: smooth roofing sheets, a fine seam every half block, lit on their upper side."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.025
            if y % 8 == 7: k -= 0.1
            elif y % 8 == 0: k += 0.06
            if x == 0: k -= 0.04
            _put(im, x, y, shade(base, k))
    return im


def rim(green, cream):
    """The roof's edge (the slabs' sides): a cream coping along the top of each half, green below it."""
    im = _img()
    for y in range(16):
        ly = y % 8
        for x in range(16):
            if ly == 0: c = shade(cream, 0.08)
            elif ly == 1: c = cream
            elif ly == 2: c = shade(cream, -0.12)
            else: c = shade(green, -0.04 * (ly - 3) / 4)
            _put(im, x, y, c)
    return im


def deck(base, grout):
    """A roof terrace (the middle of the flat roofs): square tiles, four a block, a light grout."""
    return B.tiles(base, grout, 2, "deck")


def unit():
    """A rooftop unit's casing (the iron blocks): pale metal, a row of vent slats across its middle."""
    im = L.metal(hexc("#C9CDD1"), seam=False)
    for y in range(5, 12):
        if y % 2 == 1:
            for x in range(2, 14):
                _put(im, x, y, hexc("#8E959C"))
    for x in range(16):
        _put(im, x, 15, hexc("#9EA4AA"))
    return im


def vent():
    """A vent grille on the roofs (the smooth sandstone slabs): a pale grey frame, dark slats."""
    im = _img()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): c = hexc("#D4D7D8")
            elif x in (1, 14) or y in (1, 14): c = hexc("#B4B9BC")
            else: c = hexc("#5F676E") if y % 3 == 0 else hexc("#A3AAAF")
            _put(im, x, y, c)
    return im


def pad(base):
    """A pale concrete pad on the roofs: cast, lit along its top edge."""
    return L.solid_shaded(base, 0.06, -0.06, 0.03, "pad")


def blind():
    """The blinds behind the windows (the dark walls John set behind the glass): pale slats, so the windows read as
    Let's Go's light windows, not black holes."""
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#C9D6E0") if y % 4 != 3 else hexc("#A9B9C6")
            if y % 4 == 0: c = shade(c, 0.1)
            _put(im, x, y, c)
    return im


def stripes(a, b, w=4):
    """An awning: stripes running down it."""
    im = _img()
    for y in range(16):
        for x in range(16):
            c = a if (x // w) % 2 == 0 else b
            if y == 15: c = shade(c, -0.15)
            elif y == 0: c = shade(c, 0.1)
            _put(im, x, y, c)
    return im


def post(base, top=False):
    """A white column (the bone blocks at the doors): fluted down its sides, its ends a plain disc."""
    im = _img()
    for y in range(16):
        for x in range(16):
            if top:
                d = max(abs(x - 7.5), abs(y - 7.5))
                c = shade(base, 0.1 if d > 6.5 else -0.03)
            else:
                c = shade(base, -0.1 if x % 4 == 0 else 0.07 if x % 4 == 1 else 0.0)
            _put(im, x, y, c)
    return im


def planter(rim_c, body):
    """A planter's side (John's shelves round the beds): a lit lip along the top, the body below, a shadow at the
    foot. Laid over a shelf's whole picture, so it reads the same whichever part of it shows."""
    im = _img()
    for y in range(16):
        for x in range(16):
            if y < 3: c = shade(rim_c, 0.1 if y == 0 else 0.0)
            elif y == 3: c = shade(rim_c, -0.2)
            elif y == 15: c = shade(body, -0.18)
            else: c = shade(body, 0.04 if x % 8 == 0 else 0.0)
            _put(im, x, y, c)
    return im


def board():
    """A shop's name board over a door (the shelves high on the fronts): cream, an orange rim."""
    im = _img()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): c = hexc("#D97A36")
            elif x in (1, 14) or y in (1, 14): c = hexc("#F2A65E")
            else: c = hexc("#FBF3DE")
            _put(im, x, y, c)
    return im


def rail():
    """A low railing (the nether brick fences by the beds): green painted metal, lit along its top."""
    return L.metal(hexc("#3F8A55"), seam=False)


# ---------- the lamps ----------
def lamps():
    save("lamp_saf_post", L.metal(hexc("#ECE8DC"), seam=False))
    save("lamp_saf_chisel", V.chisel(hexc("#ECE8DC"), hexc("#FBF8EF"), hexc("#4F9E58")))
    save("lamp_saf_chisel_top", B.cap_top(hexc("#E3DFD2")))
    save("lamp_saf_cap", L.metal(hexc("#4F9E58"), seam=False))
    save("lamp_saf_cap_top", B.cap_top(hexc("#4F9E58")))


# ---------- SILPH CO. ----------
def silph_panel():
    """SILPH CO.'s walls: big white cladding panels, a fine grey seam round each (laid @2)."""
    r = random.Random("sp")
    im = _img(32)
    base = hexc("#F2F3F2")
    for y in range(32):
        for x in range(32):
            k = (r.random() - 0.5) * 0.015
            if x % 16 == 15 or y % 16 == 15: c = hexc("#C9CDD0")
            else:
                if x % 16 == 0 or y % 16 == 0: k += 0.05
                c = shade(base, k)
            _put(im, x, y, c)
    return im


def silph_blind():
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#AFC4DA") if y % 4 != 3 else hexc("#8EA6C0")
            if y % 4 == 0: c = shade(c, 0.12)
            _put(im, x, y, c)
    return im


def silph_plaza():
    """The plaza in front of SILPH CO.: big pale stone slabs, a fine joint (laid @2)."""
    return B.slab_pave(hexc("#E6E4DE"), "spz")


# ---------- the FIGHTING DOJO ----------
def dojo_wall():
    """White plaster between dark timbers (laid @2: a timber frame every two blocks)."""
    r = random.Random("dw")
    im = _img(32)
    plaster, wood = hexc("#F1ECE0"), hexc("#4E3424")
    for y in range(32):
        for x in range(32):
            if x in (0, 1) or y in (30, 31) or y == 15: c = shade(wood, (r.random() - 0.5) * 0.06)
            else: c = shade(plaster, (r.random() - 0.5) * 0.02 + (0.04 if y in (0, 16) else 0))
            _put(im, x, y, c)
    return im


def dojo_wood(base, seed, vertical=True):
    """Dark wood (the Dojo's posts and beams): a fine grain running along it."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            a = x if vertical else y
            k = (r.random() - 0.5) * 0.05 + (0.05 if a % 5 == 0 else -0.04 if a % 5 == 3 else 0)
            _put(im, x, y, shade(base, k))
    return im


def shoji():
    """The Dojo's sliding door: a dark wood frame, white paper in a grid of laths."""
    im = _img()
    wood, paper = hexc("#5A3B27"), hexc("#F6F1E3")
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): c = wood
            elif x in (5, 10) or y in (5, 10): c = shade(wood, 0.15)
            else: c = shade(paper, -0.04 if (x + y) % 7 == 0 else 0.0)
            _put(im, x, y, c)
    return im


def build():
    # The paving, in John's four colours as he laid them.
    for name, col in (("pave_saf_w", "#F1ECDD"), ("pave_saf_y", "#F1CA58"), ("pave_saf_o2", "#E2B266"), ("pave_saf_o", "#DD8C52")):
        save(name, paver(hexc(col), name))
    save("saf_kerb", B.slab_pave(hexc("#CECAC1"), "sk"))
    save("saf_plaza", B.slab_pave(hexc("#EFE6D3"), "spl"))
    # The walls: yellow, white, the cream bands, the stone and cream buildings.
    save("saf_wall", wall(hexc("#F2C552"), "sw"))
    save("saf_white", wall(hexc("#F6F2E8"), "swh"))
    save("saf_band", band(hexc("#F2E4C0"), "sb"))
    save("saf_stone", wall(hexc("#D9D3C6"), "sst", joints=True))
    save("saf_grey", wall(hexc("#DEDCD6"), "sgr"))
    save("saf_cream", wall(hexc("#EFE5CF"), "scr"))
    save("saf_floor", B.tiles(hexc("#E9DFC9"), hexc("#CFC2A6"), 2, "sfl"))
    # The roofs: green, a cream coping round their edges, the terraces in the middle, the rooftop units.
    green, cream = hexc("#5DA758"), hexc("#F3EBD2")
    save("saf_roof", roof(green, "sr"))
    save("saf_rim", rim(green, cream))
    save("saf_deck", deck(hexc("#8DC688"), hexc("#B9DDB2")))
    save("saf_unit", unit())
    save("saf_vent", vent())
    save("saf_pad", pad(hexc("#DCDAD3")))
    # The windows' blinds, the awnings and canopies, the door columns, the steps.
    save("saf_blind", blind())
    save("saf_canopy", band(hexc("#4FAE9A"), "sca"))
    save("saf_awning_red", stripes(hexc("#D9473C"), hexc("#F7F1E6")))
    save("saf_awning_green", stripes(hexc("#4F9E58"), hexc("#F7F1E6")))
    save("saf_post", post(hexc("#F4F1E8")))
    save("saf_post_top", post(hexc("#F4F1E8"), True))
    # The planters (John's shelves round the beds), each its own colour; the name boards; the railings.
    save("planter_saf_cream", planter(hexc("#F1E6CC"), hexc("#E3D3AE")))
    save("planter_saf_terra", planter(hexc("#D88A5E"), hexc("#B86A44")))
    save("planter_saf_wood", planter(hexc("#B98656"), hexc("#94663E")))
    save("saf_board", board())
    save("saf_rail", rail())
    lamps()
    # SILPH CO.
    save("silph_panel", silph_panel())
    save("silph_metal", L.metal(hexc("#B9C0C7"), seam=False))
    save("silph_blind", silph_blind())
    save("silph_band", band(hexc("#D6DCE2"), "sib"))
    save("silph_floor", B.tiles(hexc("#DCE3EA"), hexc("#BCC6D0"), 2, "sif"))
    save("silph_plaza", silph_plaza())
    # The FIGHTING DOJO: slate tiles, dark timber, white plaster, its sliding door.
    pal = K.roof_pal("#5C6773", "#3C444E")
    save("roof_dojo_tiles", L.roof_tiles(pal))
    save("roof_dojo_edge", L.roof_edge(pal))
    save("roof_dojo_under", L.solid(pal["under"]))
    save("dojo_ridge", L.flat(hexc("#47505A")))
    save("dojo_wall", dojo_wall())
    save("dojo_post", dojo_wood(hexc("#5B3B26"), "dp"))
    save("dojo_beam", dojo_wood(hexc("#4A3020"), "db", vertical=False))
    save("dojo_floor", K.boards(hexc("#B88A58"), "df"))
    save("dojo_door", shoji())
    others()
    celadon()


# ---------- 0.52.0: the other places (09:01, 09:05, 09:11) ----------
def step(base, side=False):
    """A stone step as Let's Go's: a lit nosing along its front edge, the tread a shade darker behind it; (side) a
    riser, its nosing at the top of each half block (a slab shows one half)."""
    r = random.Random("st" + str(side))
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.03
            ly = y % 8 if side else y
            if side:
                if ly == 0: k += 0.16
                elif ly == 1: k += 0.06
                elif ly == 7: k -= 0.14
            else:
                if y >= 14: k += 0.12
                elif y == 13: k -= 0.06
                if x in (0, 15): k -= 0.05
            _put(im, x, y, shade(base, k))
    return im


def museum_entry():
    """The Museum's doorway (John's spruce shelves in its porch): dark walnut frames round warm-lit glass."""
    im = _img()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): c = hexc("#3E2A1C")
            elif x in (1, 14) or y in (1, 14): c = hexc("#6A4A30")
            elif x in (7, 8): c = hexc("#4E3424")
            else: c = mix(hexc("#F6E3AE"), hexc("#E2B866"), y / 15)
            _put(im, x, y, c)
    return im


def bench_yellow():
    """The benches' chains and hanging boards (09:11): the bench's own yellow, their pattern kept."""
    ramp = [hexc("#8E6420"), hexc("#C58F2E"), hexc("#E3B04C"), hexc("#F6D489")]
    save("bench_chain", L.recolour_soft(os.path.join(L.VANILLA, "textures", "block", "iron_chain.png"), ramp, 90.0))
    hang = os.path.join(L.VANILLA, "textures", "entity", "signs", "hanging", "spruce.png")
    out = os.path.join(L.HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft", "textures", "entity",
                       "signs", "hanging")
    if os.path.exists(hang):
        os.makedirs(out, exist_ok=True)
        L.recolour_soft(hang, ramp, 90.0).save(os.path.join(out, "spruce.png"))


def others():
    save("lav_step", step(hexc("#B7A9C6")))
    save("lav_step_side", step(hexc("#A595B6"), True))
    save("league_wall", V.panels(hexc("#E8CFA9"), "lw"))
    save("museum_entry", museum_entry())
    bench_yellow()


# ---------- Celadon City (09:30: "retexture and redo all builds... the lights something unique") ----------
# Let's Go's Celadon, the City of Rainbow Dreams: soft greens and white, teal and green roofs, the DEPT. STORE's cream
# columns and blue window strips, the GAME CORNER bright under its marquee, warm basket-weave pavements, tarmac, a
# blue-tiled fountain basin; its lamps dark green and gold, each globe a colour of the rainbow.
RAINBOW = [("red", "#FF8A7A"), ("orange", "#FFB86A"), ("yellow", "#FFE77A"), ("green", "#9FEA8A"), ("blue", "#8FC8FF"), ("violet", "#D6A6FF")]


def weave(base, light, dark, grout, seed):
    """A basket-weave pavement (laid @2): pairs of pavers turning each way, a light grout."""
    r = random.Random(seed)
    im = _img(32)
    for y in range(32):
        for x in range(32):
            cx, cy, lx, ly = x // 8, y // 8, x % 8, y % 8
            a, b = (lx, ly) if (cx + cy) % 2 == 0 else (ly, lx)
            if b in (3, 7) or a == 7: c = grout
            else:
                pick = (cx * 7 + cy * 13 + (b // 4) * 5) % 5
                c = light if pick == 0 else dark if pick == 1 else base
                c = shade(c, (0.06 if b in (0, 4) else -0.05 if b in (2, 6) else 0.0) + (r.random() - 0.5) * 0.03)
            _put(im, x, y, c)
    return im


def store_glass():
    """The DEPT. STORE's window strips: blue glass, a white frame, a mullion every half block."""
    im = _img()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y % 8 == 0: c = hexc("#F4F6F6")
            elif x in (1, 14) or y % 8 == 1: c = hexc("#D5DEE3")
            else: c = mix(hexc("#9ED3F2"), hexc("#5FA8DA"), (y % 8) / 7)
            if 3 <= x <= 5 and 2 <= y % 8 <= 4: c = hexc("#DDF1FC")
            _put(im, x, y, c)
    return im


def marquee():
    """The GAME CORNER's marquee: a dark band studded with warm bulbs."""
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#3A2450")
            if (x % 4 == 1 and y % 4 == 1) or (x % 4 == 2 and y % 4 == 1) or (x % 4 == 1 and y % 4 == 2) or (x % 4 == 2 and y % 4 == 2):
                c = [hexc("#FFE27A"), hexc("#FF9AD5"), hexc("#8FE3FF")][((x // 4) + (y // 4)) % 3]
            _put(im, x, y, c)
    return im


def gc_roof():
    """The GAME CORNER's roof: deep violet, gold seams."""
    im = roof(hexc("#5E3A86"), "gcr")
    for x in range(16):
        _put(im, x, 7, hexc("#E7B94C"))
    return im


def celadon():
    save("pave_cel", weave(hexc("#D9B98E"), hexc("#E8CFA8"), hexc("#C29A6C"), hexc("#F0E2C8"), "pcel"))
    save("cel_kerb", B.slab_pave(hexc("#ECECE6"), "ck"))
    save("cel_green", wall(hexc("#7FBF7A"), "cg"))
    save("cel_green_dark", band(hexc("#4E9A55"), "cgd"))
    save("cel_lime", band(hexc("#B7D86E"), "cl"))
    save("cel_lime_bright", band(hexc("#9CD25A"), "clb"))
    save("cel_white", wall(hexc("#F5F5F0"), "cw"))
    save("cel_cream", band(hexc("#F1E4C2"), "cc"))
    save("cel_floor", B.tiles(hexc("#E6E1D2"), hexc("#C9C2AE"), 2, "cfl"))
    save("cel_deck", deck(hexc("#9ACB8E"), hexc("#C2E2B8")))
    save("cel_tile_band", B.tiles(hexc("#7CCBC6"), hexc("#E9F6F4"), 4, "ctb"))
    save("cel_roof", roof(hexc("#3E9E86"), "cro"))
    save("cel_roof_teal", roof(hexc("#3FA7B3"), "crt"))
    save("cel_roof_dark", roof(hexc("#4A5866"), "crd"))
    save("cel_gc_roof", gc_roof())
    save("cel_gc_lights", marquee())
    save("cel_pilaster", post(hexc("#F1E4C6")))
    save("cel_store_glass", store_glass())
    save("cel_base", K.ashlar(hexc("#CFC8BA"), "cb"))
    save("lamp_cel_post", L.metal(hexc("#2F6A4A"), seam=False))
    save("lamp_cel_chisel", V.chisel(hexc("#2F6A4A"), hexc("#E7C35A"), hexc("#1F4A33")))
    save("lamp_cel_chisel_top", B.cap_top(hexc("#2F6A4A")))
    save("lamp_cel_cap_top", B.cap_top(hexc("#E7C35A")))
    for name, col in RAINBOW:
        save("lamp_globe_" + name, B.globe(hexc(col)))


if __name__ == "__main__":
    build()
