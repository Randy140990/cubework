"""
0.54.0's looks (John's 12:19, 12:24 and 12:26): the pictures for tools/arealooks.py's round54() and the look pack.

Kanto: Saffron's yellow walls as a finished render (no more stripes of concrete); every Gym's coloured band (the second
row up) in a colour of its own; the Johto Gyms' roofs and beams grey; every Mart's door pillars, step and sign; the
little flowers, crisper. Johto: a darker painted fence, an orange board for the coloured signs, Cherrygrove's paving
and lamps, the snow, clean. Goldenrod: the alleys' setts, the barrels and bins, the copper signs as glossy enamel, the
station's panels and lights, the Radio Tower's dark metal and glass, the fence round the city, the Magnet Train's
track, the railings; the paintings everywhere are our own pictures.

Run: called from tools/lookpack.py (looks48.EXTRA).
"""

import json
import math
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
AREA_TEX = os.path.join(L.ASSETS, "textures", "block", "area")


def _img(w=16, h=None):
    return Image.new("RGBA", (w, h or w))


def _put(im, x, y, c, a=255):
    im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (a,))


# The Gyms' bands: each Gym its own colour (John's own for Kanto's; Johto's eight by their types).
GYM_COLOURS = {"viridian_city": "#9A5A3C", "pewter_city": "#6E747C", "cerulean_city": "#E8B83A", "vermilion_city": "#D8848C",
               "celadon_city": "#B83C9E", "fuchsia_city": "#9A4F7A", "saffron_city": "#4B4F8F", "cinnabar_island": "#D2433A",
               "violet_city": "#5FA3DA", "azalea_town": "#6DAA3C", "goldenrod_city": "#E784A8", "ecruteak_city": "#5B4A9A",
               "cianwood_city": "#C7652E", "olivine_city": "#6F8CA6", "mahogany_town": "#7FD3E0", "blackthorn_city": "#A3283C"}


def render_wall(base, seed):
    """Saffron's walls (12:19: "still the yellow concrete"): a finished render, a soft plaster grain, a fine course
    line at the foot of each block, lit along its top."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.05 + (0.03 if r.random() < 0.08 else 0)
            if y == 15: k -= 0.09
            elif y == 0: k += 0.05
            _put(im, x, y, shade(base, k))
    return im


def smooth(base, seed):
    """A smooth band or pilaster, the same both ways round (no rims, so a stack doesn't stripe)."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            _put(im, x, y, shade(base, (r.random() - 0.5) * 0.025))
    return im


def gym_band(base):
    """A Gym's band (Let's Go's): glossy colour, a fine white pinstripe near its top and foot, a lit edge."""
    im = _img()
    for y in range(16):
        for x in range(16):
            k = 0.08 - 0.14 * (y / 15)
            if y in (2, 13): c = hexc("#F7F5EF")
            elif y in (1, 14): c = shade(base, -0.2)
            elif y == 0: c = shade(base, 0.3)
            elif y == 15: c = shade(base, -0.3)
            else:
                d = (x + y) % 16
                c = shade(base, k + (0.16 if d in (5, 6) else 0))
            _put(im, x, y, c)
    return im


def grey_of(name, out, cool=0.04):
    """A Gym texture of Kanto's (tan) in grey for Johto's (12:26): its light and dark kept, a faint cool tint."""
    im = Image.open(os.path.join(AREA_TEX, name + ".png")).convert("RGBA")
    p = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r_, g_, b_, a = p[x, y]
            l = 0.3 * r_ + 0.59 * g_ + 0.11 * b_
            l = 60 + l * 0.72
            p[x, y] = (int(l * (1 - cool)), int(l * (1 - cool * 0.3)), int(min(255, l * (1 + cool))), a)
    save(out, im)


def mart_pillar():
    """A Mart's door pillar: clean white stone, fluted, a Mart-blue ring at its head and foot."""
    white, blue = hexc("#F4F6F8"), hexc("#3A6FC4")
    im = _img()
    for y in range(16):
        for x in range(16):
            if y <= 1 or y >= 14: c = shade(blue, 0.18 if y in (0, 14) else -0.05)
            else: c = hexc("#D9DEE4") if x % 4 == 0 else shade(white, 0.04 if x % 4 == 1 else 0)
            _put(im, x, y, c)
    return im


def mart_step():
    """The step into a Mart: white stone, a Mart-blue nosing along its front edge."""
    im = _img()
    for y in range(16):
        for x in range(16):
            c = hexc("#3A6FC4") if y >= 13 else hexc("#F1F3F6") if y > 0 else hexc("#FFFFFF")
            if y == 12: c = hexc("#9FB6DA")
            _put(im, x, y, c)
    return im


def mart_sign_face():
    """A Mart's sign (its round symbol over the door), laid over three blocks: a white face, a Mart-blue ring, and a
    blue shopping bag on it."""
    W = 48
    im = _img(W)
    blue, white, dark = hexc("#3A6FC4"), hexc("#FBFCFD"), hexc("#244E96")
    for y in range(W):
        for x in range(W):
            d = math.hypot(x + 0.5 - W / 2, y + 0.5 - W / 2)
            c = white if d < 17 else blue if d < 22 else dark
            # The bag: a body and two handles.
            if 15 <= x <= 32 and 21 <= y <= 35 and d < 17: c = blue
            if 17 <= x <= 30 and 23 <= y <= 33 and d < 17 and (x in (17, 30) or y == 33): c = dark
            if 13 <= y <= 20 and ((x in (19, 20) or x in (27, 28)) or (y in (13, 14) and 19 <= x <= 28)) and d < 17: c = blue
            _put(im, x, y, c)
    return im


def mart_sign_ring():
    """The sign's ring behind (John's diorite): Mart blue, a lighter edge."""
    im = _img()
    for y in range(16):
        for x in range(16):
            k = 0.12 if (x == 0 or y == 0) else -0.12 if (x == 15 or y == 15) else 0.0
            _put(im, x, y, shade(hexc("#3A6FC4"), k))
    return im


def flower(src, ramp, out):
    """A little flower crisper (12:19): its own shape, its colours on a cleaner, brighter ramp."""
    p = os.path.join(L.VANILLA, "textures", "block", src + ".png")
    if not os.path.exists(p): return
    im = Image.open(p).convert("RGBA").crop((0, 0, 16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            r_, g_, b_, a = px[x, y]
            if a < 40:
                px[x, y] = (0, 0, 0, 0)
                continue
            green = g_ > r_ + 10 and g_ > b_
            l = (0.3 * r_ + 0.59 * g_ + 0.11 * b_) / 255.0
            rmp = ramp["stem"] if green else ramp["petal"]
            i = max(0, min(len(rmp) - 1, int(l * len(rmp))))
            px[x, y] = tuple(rmp[i][:3]) + (255,)
    save(out, im)


FLOWERS = {
    "wildflowers": ("flower_wild", ["#E7B21E", "#F6D33C", "#FFEA7A", "#FFFFFF"]),
    "dandelion": ("flower_dandelion", ["#E09A12", "#F4C21C", "#FFE04A", "#FFF3A6"]),
    "oxeye_daisy": ("flower_daisy", ["#C9CBD1", "#EEF0F3", "#FFFFFF", "#FFE45A"]),
    "azure_bluet": ("flower_bluet", ["#BFC8E6", "#E4EAF8", "#FFFFFF", "#FFE45A"]),
    "poppy": ("flower_poppy", ["#9E1F24", "#D8343A", "#F25A57", "#FFE45A"]),
    "red_tulip": ("flower_tulip", ["#A1202A", "#D9383E", "#F2645F", "#FFB3A8"]),
}
STEM = ["#2F7A2C", "#46A23A", "#6CC64E", "#9BE06A"]


def fence_dark():
    """Johto's fences (12:19: "a little bit of a darker colour than the whites"): painted a warm stone grey-brown,
    lit along their tops."""
    base = hexc("#8C7B68")
    r = random.Random("fj")
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.04 + (0.12 if y % 4 == 0 else -0.06 if y % 4 == 3 else 0)
            _put(im, x, y, shade(base, k))
    return im


def orange_signs():
    """Kanto's coloured signs in Johto (12:24: "keeping those colours"): the board's grey panel orange."""
    for part in ("s", "l", "r", "m"):
        src = os.path.join(AREA_TEX, "sign_front_%s.png" % part)
        if not os.path.exists(src): continue
        im = Image.open(src).convert("RGBA")
        p = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r_, g_, b_, a = p[x, y]
                if a and abs(r_ - g_) < 18 and abs(g_ - b_) < 22 and 120 < r_ < 215:
                    t = (r_ - 120) / 95.0
                    p[x, y] = tuple(int(v) for v in mix(hexc("#D9651E"), hexc("#F59A4A"), t)[:3]) + (a,)
        save("sign_front_%s_o" % part, im)
    # Their models: the signs' own, the front orange.
    models = L.MODELS
    for f in os.listdir(models):
        if not f.startswith("sign_board_"): continue
        m = json.load(open(os.path.join(models, f)))
        for k, v in list(m["textures"].items()):
            if v.startswith("retromon:block/area/sign_front_"): m["textures"][k] = v + "_o"
        json.dump(m, open(os.path.join(models, "o" + f), "w"))
    for f in os.listdir(models):
        if f.startswith("sign_stand_"):
            json.dump(json.load(open(os.path.join(models, f))), open(os.path.join(models, "o" + f), "w"))


def setts(base, seed):
    """Goldenrod's alleys (12:19): small rounded setts, each its own shade, fine dark joints, laid over two blocks."""
    r = random.Random(seed)
    im = _img(32)
    for y in range(32):
        for x in range(32):
            row = y // 6
            off = 3 if row % 2 else 0
            cx = (x + off) % 6
            c = shade(base, -0.32) if cx == 5 or y % 6 == 5 else None
            if c is None:
                cell = ((x + off) // 6, row)
                rr = random.Random("%s|%d|%d" % (seed, cell[0], cell[1]))
                k = (rr.random() - 0.5) * 0.14 + (0.06 if cx == 0 or y % 6 == 0 else 0)
                c = shade(base, k + (r.random() - 0.5) * 0.02)
            _put(im, x, y, c)
    return im


def recolour(src, ramp, out, spread=80.0):
    p = os.path.join(L.VANILLA, "textures", "block", src + ".png")
    if os.path.exists(p): save(out, L.recolour_soft(p, ramp, spread))


def panel(base, seed, joint=True):
    """A clean panel (the station's, the Radio Tower's): a soft sheen, a fine joint at its foot and side."""
    r = random.Random(seed)
    im = _img()
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.02 + 0.06 - 0.1 * (y / 15)
            if joint and (y == 15 or x == 15): k -= 0.14
            if joint and (y == 0 or x == 0): k += 0.06
            _put(im, x, y, shade(base, k))
    return im


def glow_panel(base):
    """A lit panel (the station's froglights): bright, a soft frame, as a light."""
    im = _img()
    for y in range(16):
        for x in range(16):
            e = min(x, y, 15 - x, 15 - y)
            _put(im, x, y, shade(base, -0.15 if e == 0 else 0.06 if e == 1 else 0.14))
    return im


def glass_dark():
    """The Radio Tower's dark glass: deep blue-black, a pale streak across it."""
    im = _img()
    for y in range(16):
        for x in range(16):
            d = (x + y) % 16
            c = hexc("#24344A") if d not in (4, 5) else hexc("#5C7896")
            if x in (0, 15) or y in (0, 15): c = hexc("#151E2A")
            _put(im, x, y, c, 235)
    return im


def rail_base():
    """The fence round Goldenrod (John's sideways quartz pillars): white stone, a gold line along its top and foot (the
    texture's sides, as the pillar lies on its side)."""
    im = _img()
    for y in range(16):
        for x in range(16):
            if x in (0, 15): c = hexc("#E0B040")
            elif x in (1, 14): c = hexc("#D4CDBE")
            else: c = shade(hexc("#F6F4EE"), 0.03 if y % 8 == 0 else 0)
            _put(im, x, y, c)
    return im


def carpet_cap():
    """A cap of white stone with a gold rim (John's white carpets along the fence's top)."""
    im = _img()
    for y in range(16):
        for x in range(16):
            e = min(x, y, 15 - x, 15 - y)
            c = hexc("#E0B040") if e == 0 else hexc("#FFFFFF") if e == 1 else hexc("#F6F4EE")
            _put(im, x, y, c)
    return im


def track(rail):
    """The Magnet Train's line (12:19): dark ballast, a sleeper every half block; along the rail's rows a steel rail
    (running east to west)."""
    im = _img()
    r = random.Random("tr" + str(rail))
    for y in range(16):
        for x in range(16):
            c = shade(hexc("#4A4D52"), (r.random() - 0.5) * 0.12)
            if x % 8 in (0, 1): c = hexc("#6B5A4A")
            if rail and y in (7, 8): c = hexc("#C9CED4") if y == 7 else hexc("#8E959D")
            _put(im, x, y, c)
    return im


def bin_side():
    """A bin (John's composters): green-painted slats, a darker rim."""
    im = _img()
    for y in range(16):
        for x in range(16):
            c = shade(hexc("#3F7F4A"), 0.1 if x % 4 == 0 else -0.06 if x % 4 == 3 else 0)
            if y <= 1 or y >= 14: c = hexc("#2C5A35")
            _put(im, x, y, c)
    return im


def snow_clean(seed):
    """The snow at the ICE PATH (12:26: "more connected texture-y and a bit cleaner"): soft white, the faintest blue
    shading, laid over two blocks so no square shows."""
    r = random.Random(seed)
    im = _img(32)
    for y in range(32):
        for x in range(32):
            k = math.sin((x + y * 0.6) / 5.0) * 0.025 + (r.random() - 0.5) * 0.02
            _put(im, x, y, shade(hexc("#F4F8FC"), k))
    return im


def cg_pave(base, seed):
    """Cherrygrove's white path (12:24: "a unique"): pale stone flags, a fine blossom-pink joint, laid over two."""
    r = random.Random(seed)
    im = _img(32)
    for y in range(32):
        for x in range(32):
            c = hexc("#E9C9D2") if x % 16 == 15 or y % 16 == 15 else shade(base, (r.random() - 0.5) * 0.03 + (0.05 if x % 16 == 0 or y % 16 == 0 else 0))
            _put(im, x, y, c)
    return im


# ---------- the paintings (12:19: "this artwork is Minecraft"): our own pictures in their places ----------
PAINTINGS = {"kebab": (1, 1), "aztec": (1, 1), "alban": (1, 1), "aztec2": (1, 1), "bomb": (1, 1), "plant": (1, 1), "wasteland": (1, 1),
             "meditative": (1, 1), "pool": (2, 1), "courbet": (2, 1), "sea": (2, 1), "sunset": (2, 1), "creebet": (2, 1), "wanderer": (1, 2),
             "graham": (1, 2), "prairie_ride": (1, 2), "match": (2, 2), "bust": (2, 2), "stage": (2, 2), "void": (2, 2), "skull_and_roses": (2, 2),
             "wither": (2, 2), "baroque": (2, 2), "humble": (2, 2), "earth": (2, 2), "wind": (2, 2), "water": (2, 2), "fire": (2, 2),
             "fighters": (4, 2), "changing": (4, 2), "finding": (4, 2), "lowmist": (4, 2), "passage": (4, 2), "skeleton": (4, 3),
             "donkey_kong": (4, 3), "pointer": (4, 4), "pigscene": (4, 4), "burning_skull": (4, 4), "orb": (4, 4), "unpacked": (4, 4),
             "backyard": (3, 4), "pond": (3, 4), "bouquet": (3, 3), "cavebird": (3, 3), "cotan": (3, 3), "endboss": (3, 3), "fern": (3, 3),
             "owlemons": (3, 3), "sunflowers": (3, 3), "tides": (3, 3), "dennis": (3, 3)}
SKIES = [("#9FD4F2", "#E8F6FF"), ("#F7B48A", "#FDE7C9"), ("#B9A6E8", "#F2E6FF"), ("#8FC8E8", "#DFF3FB"), ("#F4C46B", "#FFF1C9")]
LANDS = [("#5FAF4E", "#3D8A3A"), ("#7DC46A", "#4E9C45"), ("#C9A86A", "#9C7C48"), ("#6BB6A6", "#3E8C7E")]


def painting(name, w, h):
    """An original picture a painting's size (16 a block): a sky, hills or sea, a sun, a frame. Seeded by its name."""
    r = random.Random(name)
    W, H = w * 16, h * 16
    im = Image.new("RGBA", (W, H))
    sky = SKIES[r.randrange(len(SKIES))]
    land = LANDS[r.randrange(len(LANDS))]
    sea = r.random() < 0.4
    sun = (r.uniform(0.2, 0.8) * W, r.uniform(0.15, 0.4) * H, r.uniform(0.08, 0.14) * min(W, H) + 2)
    hills = [(r.uniform(0, W), r.uniform(0.45, 0.7) * H, r.uniform(0.25, 0.6) * W) for _ in range(3)]
    abstract = r.random() < 0.25
    p = im.load()
    for y in range(H):
        for x in range(W):
            t = y / max(1, H - 1)
            c = mix(hexc(sky[0]), hexc(sky[1]), min(1, t * 1.4))
            if abstract:
                band = int((x * 0.6 + y) / max(4, H / 5)) % 4
                c = mix(hexc(sky[0]), hexc(land[band % 2]), 0.2 + 0.2 * band)
            else:
                if math.hypot(x - sun[0], y - sun[1]) < sun[2]: c = hexc("#FFF6D8")
                ground = min(H * 0.98 - (math.cos((x - hx) / max(1, hw) * math.pi) + 1) * 0.12 * H if abs(x - hx) < hw else H * 2 for hx, hy, hw in hills)
                ground = min(ground, H * (0.78 if sea else 0.7))
                if y > ground: c = mix(hexc(land[0]), hexc(land[1]), min(1, (y - ground) / max(1, H * 0.3)))
                if sea and y > H * 0.72 and y <= ground + 1: c = hexc("#4F9BD6") if (x + y) % 7 else hexc("#BFE6FF")
            if x in (0, W - 1) or y in (0, H - 1): c = hexc("#6B4A2A")
            elif x in (1, W - 2) or y in (1, H - 2): c = hexc("#C9A15A")
            p[x, y] = tuple(int(v) for v in c[:3]) + (255,)
    return im


def paintings():
    out = os.path.join(L.HERE, "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets", "minecraft", "textures", "painting")
    os.makedirs(out, exist_ok=True)
    for name, (w, h) in PAINTINGS.items():
        painting(name, w, h).save(os.path.join(out, name + ".png"))


def build():
    # Saffron.
    save("saf_wall", render_wall(hexc("#F2C552"), "sw2"))
    save("saf_band", smooth(hexc("#F2E4C0"), "sb2"))
    # The Gyms.
    for place, col in GYM_COLOURS.items():
        save("gym_band_" + place, gym_band(hexc(col)))
    for a, b in (("roof_gym_tiles", "roof_gymg_tiles"), ("roof_gym_edge", "roof_gymg_edge"), ("roof_gym_under", "roof_gymg_under"),
                 ("gym_flat", "gym_flat_grey"), ("gym_flat_light", "gym_flat_light_grey"), ("gym_beam", "gym_beam_grey"), ("gym_beam_top", "gym_beam_top_grey")):
        if os.path.exists(os.path.join(AREA_TEX, a + ".png")): grey_of(a, b)
    # The Marts.
    save("mart_pillar", mart_pillar())
    save("mart_step", mart_step())
    save("mart_sign_face", mart_sign_face())
    save("mart_sign_ring", mart_sign_ring())
    # The flowers.
    for src, (out, petal) in FLOWERS.items():
        flower(src, {"petal": [hexc(c) for c in petal], "stem": [hexc(c) for c in STEM]}, out)
    recolour("wildflowers_stem", [hexc(c) for c in STEM], "flower_wild_stem")
    # Johto.
    save("fence_johto", fence_dark())
    orange_signs()
    save("snow_clean", snow_clean("sc"))
    save("pave_cg_a", cg_pave(hexc("#F4F1EA"), "cga"))
    save("pave_cg_b", cg_pave(hexc("#E6E2DA"), "cgb"))
    save("lamp_cg_post", L.metal(hexc("#8E2A3E"), seam=False))
    save("lamp_cg_cap", B.cap_top(hexc("#F2D8DE")))
    # Goldenrod.
    save("gold_alley", setts(hexc("#A39C92"), "ga"))
    recolour("barrel_side", [hexc(c) for c in ("#5A3B22", "#8A5E36", "#B98350", "#D9A86E", "#F0CB94")], "crate_barrel_side")
    recolour("barrel_top", [hexc(c) for c in ("#5A3B22", "#8A5E36", "#B98350", "#D9A86E", "#F0CB94")], "crate_barrel_top")
    save("bin_side", bin_side())
    save("bin_top", B.cap_top(hexc("#2C5A35")))
    save("gold_emblem", S.shiny(hexc("#D9774A"), "gem"))
    save("station_teal", panel(hexc("#2FA8A0"), "st"))
    save("station_white", panel(hexc("#F3F5F6"), "sw"))
    save("station_light", glow_panel(hexc("#F4FBFF")))
    save("station_light_green", glow_panel(hexc("#B6F0A8")))
    save("station_light_amber", glow_panel(hexc("#FFD58A")))
    save("station_mullion", L.metal(hexc("#3A434C"), seam=False))
    save("glass_dark", glass_dark())
    save("radio_panel", panel(hexc("#2E3A4E"), "rp"))
    save("radio_white", panel(hexc("#F2F3F5"), "rw"))
    save("radio_grey", panel(hexc("#8D96A2"), "rg"))
    save("rail_base", rail_base())
    save("gold_carpet_cap", carpet_cap())
    save("track_rail", track(True))
    save("track_bed", track(False))
    save("gold_lawn", B.tiles(hexc("#8FCB72"), hexc("#6FAE58"), 2, "gl"))
    recolour("iron_bars", [hexc(c) for c in ("#151A1F", "#262D34", "#3A434C", "#5C6670", "#8C96A0")], "railing_dark", 60.0)
    paintings()


if __name__ == "__main__":
    build()
