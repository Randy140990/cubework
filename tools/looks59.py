"""
0.59.0's looks (John's review, 18:14 to 18:40): the pictures for tools/arealooks59.py's round59().

Cinnabar's volcano in its own rock (no deepslate, tuff or basalt left); Routes 13 to 15's lamps and lanterns white;
Blackthorn's lamps brown; the INDIGO PLATEAU's copper trapdoors flat; the park gate's roof and walls; the plant pots in
four colours; Goldenrod's alleys, its DEPT. STORE's blue and red seamless, its walls; Johto's house walls; the
Slowpoke Well; the towers' spires; Violet's and Ecruteak's roofs in shades of their colour.

Run: called from tools/lookpack.py (looks48.EXTRA, after looks57).
"""

import json
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


# ---------- the Game Corner's and the Slowpoke Well's outsides (19:08: "I want all exteriors squared away") ----------
def outsides():
    # The Game Corner: marquee bulbs along its front, the gold lights in its doorway's floor.
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#3A2A4A")
            d = math.hypot((x % 8) - 3.5, (y % 8) - 3.5)
            if d < 2.6: c = mix(hexc("#FFF6D8"), hexc("#FFC94A"), d / 2.6)
            _put(im, x, y, c)
    save("gc_bulbs", im)
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#E9B640")
            if x in (0, 15) or y in (0, 15): c = shade(c, -0.25)
            elif (x // 4 + y // 4) % 2 == 0: c = mix(c, hexc("#FFF1C2"), 0.45)
            _put(im, x, y, c)
    save("gc_floor_lights", im)
    # The well: its windlass beam dark wood, its bucket staves with iron bands.
    save("well_beam", S.dojo_wood(hexc("#4A3326"), "wellb", vertical=False))
    im = _img()
    r = random.Random("bucket59")
    for y in range(16):
        for x in range(16):
            c = shade(hexc("#8A6242"), (r.random() - 0.5) * 0.06 - (0.12 if x % 4 == 0 else 0))
            if y in (2, 3, 12, 13): c = shade(hexc("#5E6266"), 0.05 if y in (2, 12) else -0.08)
            _put(im, x, y, c)
    save("well_bucket", im)


# ---------- all Johto's outsides (19:17: "anywhere there's an old texture, we've retextured in our style") ----------
def johto_sweep():
    # The plain white and grey of the Centers, Marts, houses and gates: a clean render, the faintest grain.
    save("jw_white", J.render_wall(hexc("#F3F2EE"), "jww"))
    save("jw_grey", J.render_wall(hexc("#C9CBCB"), "jwg"))
    # The lights in the doorways' floors: a warm cream panel that glows.
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#FFF3D6")
            if x in (0, 15) or y in (0, 15): c = hexc("#D9C9A6")
            elif x in (1, 14) or y in (1, 14): c = hexc("#F5E3BC")
            _put(im, x, y, c)
    save("door_light", im)
    # The S.S. AQUA: white painted steel with its seams, a navy hull, grey steel decks, teak boards, a yellow funnel
    # with its red band, dark steel, portholes lit warm, its teal stripe.
    def plates(base, seed, seam=0.1, rivets=True):
        r = random.Random(seed)
        im = _img()
        for y in range(16):
            for x in range(16):
                k = (r.random() - 0.5) * 0.025
                if y == 0 or x == 0: k -= seam
                if rivets and (x in (2, 13)) and (y in (2, 13)): k -= seam * 0.6
                _put(im, x, y, shade(base, k))
        return im
    save("aqua_white", plates(hexc("#F1F3F4"), "aqw", 0.06))
    save("aqua_blue", plates(hexc("#264A7E"), "aqb", 0.12))
    save("aqua_deck", plates(hexc("#8E9499"), "aqd", 0.1))
    save("aqua_boards", K.boards(hexc("#A87A4E"), "aqbo"))
    save("aqua_yellow", plates(hexc("#E8B93A"), "aqy", 0.08, rivets=False))
    save("aqua_red", plates(hexc("#C23A30"), "aqr", 0.08, rivets=False))
    save("aqua_dark", plates(hexc("#2B2E33"), "aqk", 0.06, rivets=False))
    save("aqua_steel", plates(hexc("#B7BCC1"), "aqs", 0.08))
    save("aqua_teal", plates(hexc("#2E8D97"), "aqt", 0.08, rivets=False))
    im = _img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = hexc("#F1F3F4")
            if d < 6.2: c = hexc("#9AA2AA")
            if d < 5.0: c = mix(hexc("#FFF0C8"), hexc("#F2C66A"), d / 5.0)
            _put(im, x, y, c)
    save("aqua_porthole", im)


# ---------- 0.59.3 (20:35, 20:47) ----------
def seamless_flags(base, seed, joint, size=32):
    """Square-cut flagstones that tile (the glazed terracotta paths, 20:47: "it's not tiling properly"): slabs of 8 and
    16 pixels on an 8-pixel grid, so they meet across every block edge; each its own shade, a fine joint."""
    r = random.Random(seed)
    im = _img(size)
    cells = {}
    grid = size // 8
    used = [[False] * grid for _ in range(grid)]
    slabs = []
    for gy in range(grid):
        for gx in range(grid):
            if used[gy][gx]: continue
            w = 2 if gx + 1 < grid and not used[gy][gx + 1] and r.random() < 0.35 else 1
            h = 2 if gy + 1 < grid and r.random() < 0.3 and all(not used[gy + 1][gx + i] for i in range(w)) else 1
            for yy in range(h):
                for xx in range(w): used[gy + yy][gx + xx] = True
            slabs.append((gx * 8, gy * 8, w * 8, h * 8, (r.random() - 0.5) * 0.12))
    for x0, y0, w, h, k0 in slabs:
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if x == x0 or y == y0: c = joint
                else:
                    k = k0 + (r.random() - 0.5) * 0.035 + (0.05 if y == y0 + 1 else 0) - (0.05 if y == y0 + h - 1 else 0)
                    c = shade(base, k)
                _put(im, x, y, c)
    return im


def boardwalk(base, seed, size=64):
    """Olivine's boardwalk (20:35: "the planks ... the walkway ... hasn't been done"): long boards running along it,
    each its own shade, staggered butt joints, a dark gap between boards, nail heads; laid over four blocks."""
    r = random.Random(seed)
    im = _img(size)
    bh = 8
    for row in range(size // bh):
        cut = r.randrange(0, size)
        k0s = {}
        for y in range(row * bh, row * bh + bh):
            for x in range(size):
                seg = ((x - cut) % size) // 32
                if seg not in k0s: k0s[seg] = (r.random() - 0.5) * 0.12
                gx = (x - cut) % 32
                if y == row * bh: c = shade(base, -0.42)
                elif gx == 0: c = shade(base, -0.3)
                else:
                    k = k0s[seg] + 0.025 * math.sin((x + row * 13) * 0.45) + (r.random() - 0.5) * 0.03 + (0.06 if y == row * bh + 1 else 0)
                    if gx in (2, 29) and y == row * bh + 4: k -= 0.35
                    c = shade(base, k)
                _put(im, x, y, c)
    return im


def metal_tiles(base, seed, rows=4):
    """A tower's roof as metal (20:47: "more like a metallic colour"): standing-seam sheet in rows, each row lit along its
    top and shaded at its foot, a sheen across it, a fine seam."""
    r = random.Random(seed)
    im = _img()
    h = 16 // rows
    for y in range(16):
        for x in range(16):
            yy = y % h
            k = 0.16 - 0.3 * (yy / (h - 1)) + 0.07 * math.sin((x + y) * 0.5) + (r.random() - 0.5) * 0.02
            if yy == h - 1: k -= 0.18
            if x % 8 == 7: k -= 0.1
            if (x + 2 * y) % 16 in (3, 4): k += 0.12
            _put(im, x, y, shade(base, k))
    return im


def gold_column(base, seed):
    """Polished gold that runs on from block to block (20:47: "anything that's gold needs doing"): no frame at its ends,
    upright highlights, a soft sheen."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = 0.32 * math.cos((x - 4.5) / 16 * math.pi * 2) * 0.5 + (r.random() - 0.5) * 0.02
            if x in (4, 5): k += 0.22
            if x in (12, 13): k -= 0.12
            _put(im, x, y, shade(base, k))
    return im


def whirlpool():
    """The WHIRL ISLANDS' whirlpools (20:35: "add those whirlpools ... in their locations"): a spiral of white foam over
    darker water, turning (16 frames), over five blocks; see-through between the arms."""
    size, frames = 80, 16
    im = Image.new("RGBA", (size, size * frames))
    for f in range(frames):
        rot = f / frames * math.pi * 2
        for y in range(size):
            for x in range(size):
                dx, dy = (x + 0.5 - size / 2) / (size / 2), (y + 0.5 - size / 2) / (size / 2)
                rr = math.hypot(dx, dy)
                if rr > 1: continue
                a = math.atan2(dy, dx)
                arm = math.cos(3 * (a + rot) + 10.0 * rr)
                edge = min(1.0, (1 - rr) * 3)
                foam = max(0.0, arm) ** 6 * edge
                if foam > 0.3:
                    c = mix(hexc("#CFEAF8"), hexc("#FFFFFF"), min(1, foam))
                    al = int(170 + 85 * min(1, foam))
                else:
                    c = mix(hexc("#2C78B6"), hexc("#123A66"), 1 - rr)
                    al = int(min(1.0, (1 - rr) * 2.2) * 190)
                if rr < 0.08: c, al = hexc("#0E2A48"), 220
                im.putpixel((x, y + f * size), c[:3] + (max(0, min(255, al)),))
    save("whirlpool", im)
    with open(os.path.join(P.AREA_TEX, "whirlpool.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh)


def signs59():
    """(20:35: "each of the signs ... does some squiggles") Every sign's back has writing too (some face the way you
    come), and the blue sign on the beaches (20:35: "the top of a normal sign, but just in the blue colour")."""
    for part, w, c0 in (("l", 32, 16), ("r", 32, 0), ("m", 48, 16), ("s", 16, 0)):
        save("sign_back_" + part, L.board(w).crop((c0, 0, c0 + 16, 16)))
        save("sign_short_back_" + part, L.short_board(w).crop((c0, 0, c0 + 16, 16)))
    def tint(src, out, a, b):
        im = Image.open(os.path.join(P.AREA_TEX, src + ".png")).convert("RGBA")
        p = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r_, g_, b_, al = p[x, y]
                if al and abs(r_ - g_) < 18 and abs(g_ - b_) < 22 and 120 < r_ < 215:
                    t = (r_ - 120) / 95.0
                    p[x, y] = tuple(int(v) for v in mix(hexc(a), hexc(b), t)[:3]) + (al,)
        save(out, im)
    for part in ("s", "l", "r", "m"):
        tint("sign_back_" + part, "sign_back_%s_o" % part, "#D9651E", "#F59A4A")
        tint("sign_front_" + part, "sign_front_%s_b" % part, "#2F6DB5", "#5C95D6")
        tint("sign_back_" + part, "sign_back_%s_b" % part, "#2F6DB5", "#5C95D6")
    models = L.MODELS
    for f in sorted(os.listdir(models)):
        if f.startswith("osign_board_"):
            m = json.load(open(os.path.join(models, f)))
            for k_, v in list(m["textures"].items()):
                if v.startswith("retromon:block/area/sign_back_") and not v.endswith("_o"): m["textures"][k_] = v + "_o"
            json.dump(m, open(os.path.join(models, f), "w"))
        if f.startswith("sign_board_") or f.startswith("sign_stand_"):
            m = json.load(open(os.path.join(models, f)))
            if f.startswith("sign_board_"):
                for k_, v in list(m["textures"].items()):
                    if v.startswith("retromon:block/area/sign_front_") or v.startswith("retromon:block/area/sign_back_"): m["textures"][k_] = v + "_b"
            json.dump(m, open(os.path.join(models, "b" + f), "w"))


def beach59():
    """Route 40's beach and the seafronts (20:35)."""
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#F4F1EA") if (x // 3) % 2 == 0 else hexc("#3E86C8")
            if x in (0, 15): c = shade(hexc("#E9E4D8"), -0.05)
            _put(im, x, y, shade(c, (random.Random(x * 31 + y).random() - 0.5) * 0.03))
    save("beach_towel", im)
    save("umbrella_blue", flat(hexc("#2F78C8"), "umbb", 0.02))
    save("umbrella_white", flat(hexc("#F4F3EE"), "umbw", 0.02))
    save("umbrella_pole", L.metal(hexc("#E6E8EA"), seam=False))
    save("ol_boardwalk", boardwalk(hexc("#C6A57A"), "olbw"))
    save("ol_pave", seamless_flags(hexc("#B9B7B1"), "olpave", hexc("#8E8C87")))
    save("ol_kerb", board(hexc("#D9D7D1"), "olkerb", 0.05))
    save("ol_planter", K.ashlar(hexc("#C8C6C0"), "olpl"))
    save("ol_wall_cap", board(hexc("#E3E1DB"), "olcap", 0.06))
    save("ol_post", lamp_plain(hexc("#ECECE8"), False))
    save("ol_step", board(hexc("#BDBAB3"), "olstep", 0.1))
    save("planter_wood", K.boards(hexc("#9A6A44"), "plwood"))
    save("cliff_root", recolour_png(os.path.join(P.AREA_TEX, "drip_rock.png"), -0.12, "croot"))
    for k_, c in (("green2", "#4F8A3C"), ("green3", "#2F6B4A")):
        for part in ("tiles", "edge", "under"):
            N.tint_of("roof_green_" + part, "roof_%s_%s" % (k_, part), hexc(c))


def recolour_png(path, k, seed):
    """A texture a shade darker, with a few fine root lines through it (the cliffs' timbers, 20:47: "blend in ... a unique
    pattern that sticks out a little")."""
    im = Image.open(path).convert("RGBA")
    p = im.load()
    r = random.Random(seed)
    for y in range(im.height):
        for x in range(im.width):
            c = p[x, y]
            if c[3] == 0: continue
            p[x, y] = shade(c, k)[:3] + (c[3],)
    for _ in range(im.width // 8):
        x, y = r.randrange(im.width), r.randrange(im.height)
        for i in range(r.randrange(6, 14)):
            c = p[x % im.width, y % im.height]
            p[x % im.width, y % im.height] = shade(c, -0.18)[:3] + (255,)
            x += r.choice((-1, 0, 1)); y += 1
    return im


def ecruteak59():
    save("sprout_roof", metal_tiles(hexc("#6A5578"), "sprm"))
    save("sprout_ridge", metal_tiles(hexc("#4A3E55"), "sprg"))
    save("burned_teal", metal_tiles(hexc("#4E8A82"), "brm"))
    save("tin_roof", metal_tiles(hexc("#9A5236"), "tinm"))
    save("tin_ridge", metal_tiles(hexc("#B48A3A"), "tinr"))
    save("spire_gold", gold_column(hexc("#D9AE45"), "spg"))
    save("redb_front", F.panel(hexc("#A9372C"), "redbf", 0.05))
    save("redb_roof", metal_tiles(hexc("#9E3A30"), "redbr"))
    save("ecru_flag", seamless_flags(hexc("#B4AEA6"), "ecfl", hexc("#8E8780")))


def cinnabar59():
    save("lab_cin_wall", flat(hexc("#F2EFE6"), "labw", 0.015))
    save("lab_sill", board(hexc("#F7F5EE"), "labsill", 0.07))
    cells = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#D9DDE1") if x in (0, 15) or y in (0, 15) else hexc("#BAC2CA") if x % 5 == 0 or y % 5 == 0 else hexc("#3F74C2")
            if c == hexc("#3F74C2") and (x + y) % 5 == 0: c = hexc("#6A9BE0")
            _put(cells, x, y, c)
    save("solar_cells", cells)


def round59c():
    seamless = None
    whirlpool()
    signs59()
    beach59()
    ecruteak59()
    cinnabar59()


def build():
    round59c()
    johto_sweep()
    outsides()
    volcano()
    lamps()
    plateau()
    johto()
    goldenrod()
