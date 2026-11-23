"""
0.59.0's looks (John's review, 18:14 to 18:40): the pictures for tools/arealooks59.py's round59().

Cinnabar's volcano in its own rock (no deepslate, tuff or basalt left); Routes 13 to 15's lamps and lanterns white;
Blackthorn's lamps brown; the INDIGO PLATEAU's copper trapdoors flat; the park gate's roof and walls; the plant pots in
four colours; Goldenrod's alleys, its DEPT. STORE's blue and red seamless, its walls; Johto's house walls; the
Slowpoke Well; the towers' spires; Violet's and Ecruteak's roofs in shades of their colour.

Run: called from tools/lookpack.py (looks48.EXTRA, after looks57).
"""

import math
import os
import random

from PIL import Image

import lookarea as L
import looks46 as K
import looks48b as B
import looks50 as F
import looks52 as S
import looks54 as J
import looks56 as N
import looks57 as P

hexc, mix, save = L.hexc, L.mix, L.save
shade = B.shade
_img, _put, board, lamp_plain = P._img, P._put, P.board, P.lamp_plain
VAN = os.path.join(L.VANILLA, "textures", "block")


def flat(base, seed, k=0.012):
    """One colour, seamless (18:36: "make the blues and the reds seamless, so they connect properly ... no lines
    between them"): the faintest grain, no edge, no joint, so blocks side by side read as one surface."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            _put(im, x, y, shade(base, (r.random() - 0.5) * k + 0.006 * math.sin((x + y) * 0.7)))
    return im


def recolour(src, base, seed, keep=0.55):
    """A vanilla texture in another colour, its shading kept (the pots, the lantern's frame)."""
    im = Image.open(os.path.join(VAN, src + ".png")).convert("RGBA")
    p = im.load()
    r = random.Random(seed)
    for y in range(im.height):
        for x in range(im.width):
            c = p[x, y]
            if c[3] == 0: continue
            lum = (c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11) / 255.0
            k = (lum - 0.5) * keep + (r.random() - 0.5) * 0.02
            p[x, y] = shade(base, k)[:3] + (c[3],)
    return im


# ---------- Cinnabar's volcano (18:14: "you haven't retextured it fully ... the entire volcano") ----------
def volcano():
    # Warm grey-brown volcanic rock, darker and redder towards the crater, its black basalt crust round the top.
    save("volc_rock", L.rock_face(hexc("#6E5E55"), hexc("#86736A"), hexc("#4A3D38"), "vrock", cells=22))
    save("volc_rock_dark", L.rock_face(hexc("#5A4A44"), hexc("#705C55"), hexc("#3B302C"), "vdark", cells=18, strata=0.35))
    save("volc_rock_dark_top", L.rock_face(hexc("#615048"), hexc("#7A655D"), hexc("#41352F"), "vdarkt", cells=24))
    save("volc_basalt", L.rock_face(hexc("#3E3634"), hexc("#554945"), hexc("#272120"), "vbas", cells=14, strata=0.5))
    save("volc_basalt_top", L.rock_face(hexc("#433A37"), hexc("#5A4D49"), hexc("#2B2423"), "vbast", cells=20))
    save("volc_tuff", L.rock_face(hexc("#8A7868"), hexc("#A08C7A"), hexc("#655649"), "vtuff", cells=24))


# ---------- the lamps (18:19: "not orange ... maybe like a white"; 18:25 Blackthorn "a brown or something") ----------
def lamps():
    for n, c in (("lamp_route_post", "#EEEEEA"), ("lamp_route_cap", "#DCDFE2"), ("lamp_route_foot", "#CDD1D5"),
                 ("lamp_bt_post", "#6E4C36"), ("lamp_bt_cap", "#4C3527"), ("lamp_bt_foot", "#5E412F")):
        save(n, lamp_plain(hexc(c), n.endswith("foot")))
    save("lamp_globe_white", B.globe(hexc("#F4F8FF")))
    # Route 13's lanterns: a white metal frame, a cool white light.
    im = Image.open(os.path.join(VAN, "lantern.png")).convert("RGBA")
    p = im.load()
    for y in range(im.height):
        for x in range(im.width):
            c = p[x, y]
            if c[3] == 0: continue
            warm = c[0] > 150 and c[1] > 100 and c[2] < 120
            if warm: p[x, y] = mix(hexc("#FFFFFF"), hexc("#DDE8F5"), (255 - c[1]) / 155.0)[:3] + (c[3],)
            else:
                lum = (c[0] + c[1] + c[2]) / 765.0
                p[x, y] = shade(hexc("#E6E8EA"), (lum - 0.35) * 0.6)[:3] + (c[3],)
    save("lantern_white", im)
    # (The lantern flickers: its three frames, as the game's.)
    import shutil
    shutil.copy(os.path.join(VAN, "lantern.png.mcmeta"), os.path.join(P.AREA_TEX, "lantern_white.png.mcmeta"))


# ---------- the INDIGO PLATEAU (18:25: "the oxidized copper trapdoors probably need replacing something flat") ----------
def plateau():
    save("metal_teal_flat", F.panel(hexc("#3E9C8E"), "mtflat", 0.06))
    save("plateau_sill", board(hexc("#E8DFC8"), "plsill"))


# ---------- Ecruteak, the NATIONAL PARK, Violet (18:25 to 18:36) ----------
def johto():
    save("ecru_trim", board(hexc("#BDB7AD"), "ectrim", 0.06))
    save("dance_trim", board(hexc("#F2EFE8"), "dtrim"))
    save("ecru_wall_stone", K.ashlar(hexc("#A29D94"), "ecws"))
    save("banner_foot", F.panel(hexc("#B9B5AE"), "bfoot", 0.06))
    save("spire_metal", L.metal(hexc("#3A3C42"), seam=False))
    save("spire_gold", S.shiny(hexc("#E0B040"), "spg"))
    # The park's gate (18:34: "doesn't look done ... I don't want everything to look like the dripstone"): a warm red
    # tiled roof with darker ridges, cream render, dark timbers, dressed grey stone, a teal trim.
    save("npg_roof", B.tiles(hexc("#B4553A"), hexc("#7A3324"), 4, "npgr"))
    save("npg_ridge", F.panel(hexc("#7A3324"), "npgg", 0.06))
    save("npg_wall", J.render_wall(hexc("#EFE8DA"), "npgw"))
    save("npg_siding", K.boards(hexc("#C8A97C"), "npgs", vertical=True))
    save("npg_timber", S.dojo_wood(hexc("#4A3326"), "npgt"))
    save("npg_stone", K.ashlar(hexc("#A7A49D"), "npgst"))
    save("npg_trim", board(hexc("#2E8D97"), "npgtr"))
    save("npg_dark", F.panel(hexc("#2A2C30"), "npgd", 0.03))
    # Johto's house walls (18:25: "they've all still got the terracotta wall"): a clean white render, as Pallet's.
    save("jhouse_wall", J.render_wall(hexc("#F0ECE4"), "jhw"))
    # The Slowpoke Well (18:39): shingled wood roof, the well's grey stone.
    save("well_roof", K.boards(hexc("#8A6242"), "wellr"))
    save("well_stone", K.ashlar(hexc("#9C9A94"), "wells"))
    save("well_cover", K.boards(hexc("#6E4E36"), "wellc", vertical=True))
    # The plant pots (18:36: "maybe you can have different colours per plant pot").
    for n, c in (("pot_terracotta", "#B8613E"), ("pot_cream", "#E6DCC4"), ("pot_sage", "#7F9E78"), ("pot_slate", "#5F7A96")):
        save(n, recolour("flower_pot", hexc(c), n))
    # Violet's purples and Ecruteak's reds (18:40: "variations of the purple ... and the red ... a tiny bit of character").
    for k, c in (("vio2", "#7B4596"), ("vio3", "#5E3474"), ("vio4", "#8A4F8C"), ("ecru2", "#A4372E"), ("ecru3", "#8A2B28"), ("ecru4", "#B4473A")):
        for part in ("tiles", "edge", "under"):
            N.tint_of("roof_lav_" + part, "roof_%s_%s" % (k, part), hexc(c))


# ---------- Goldenrod (18:36) ----------
def goldenrod():
    # The DEPT. STORE's blue facing, its paler band and its red: seamless.
    save("ds_blue", flat(hexc("#3E78C8"), "dsb"))
    save("ds_blue_light", flat(hexc("#7FB0E6"), "dsbl"))
    save("ds_red", flat(hexc("#C8443C"), "dsr"))
    # Its walls (the pink terracotta): a warm clean render.
    save("gold_wall", J.render_wall(hexc("#EFE6DA"), "gwall"))
    save("band_wood", board(hexc("#9C6A42"), "gband"))
    # Its alleys (18:36: "the rods, the fences and barrels and flooring and everything else"): worn dark tarmac, blue
    # steel drums, green bins, grey pipes, dark green railings.
    r = random.Random("alley59")
    im = _img(32)
    for y in range(32):
        for x in range(32):
            k = (r.random() - 0.5) * 0.08
            if r.random() < 0.03: k -= 0.12
            if (x * 3 + y * 5) % 29 == 0: k += 0.05
            _put(im, x, y, shade(hexc("#5C5F63"), k))
    for _ in range(3):
        x, y = r.randrange(32), r.randrange(32)
        for i in range(r.randrange(5, 11)):
            _put(im, x % 32, y % 32, shade(hexc("#45484C"), 0))
            x += r.choice((0, 1)); y += r.choice((0, 1, 1))
    save("alley_floor", im)
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#3D6FA8")
            if y in (2, 3, 12, 13): c = shade(c, -0.18)
            if y in (0, 15): c = shade(c, -0.25)
            k = 0.08 * math.cos((x - 5) / 16 * 3.14)
            _put(im, x, y, shade(c, k))
    save("alley_drum", im)
    im = _img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = hexc("#2F5C93") if d < 6.5 else hexc("#3D6FA8")
            if 5.5 < d < 6.5: c = hexc("#264A77")
            _put(im, x, y, c)
    save("alley_drum_top", im)
    save("alley_bin", F.panel(hexc("#3E7A44"), "abin", 0.08))
    save("alley_bin_top", F.panel(hexc("#335F38"), "abint", 0.05))
    save("alley_pipe", L.metal(hexc("#8E9399"), seam=False))
    save("alley_fence", L.metal(hexc("#2F4A3A"), seam=False))


def build():
    volcano()
    lamps()
    plateau()
    johto()
    goldenrod()
