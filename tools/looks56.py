"""
0.56.0's looks (John's 13:53 and 13:55): the pictures for tools/arealooks.py's round56() and the look pack.

Johto's dirt as Kanto's (the paths' own picture on the brown powder John edges them with); Blackthorn's houses (their
roofs in tiles of John's warm brown, the band between the floors and the trims clean wood, the posts either side of the
doors dark timber); the chimneys' caps; Cherrygrove's lamps' feet; Route 27's posts and chains (steel, one barrier);
every Mart's sign as the Centers' (a white disc, the Mart's symbol on it in blue); Goldenrod's Dept. Store (its blue and
red panels, its red roof, its steel); the station's planters.

Run: called from tools/lookpack.py (looks48.EXTRA).
"""

import os
import random

from PIL import Image

import lookarea as L
import looks46 as K
import looks47 as C
import looks48b as B
import looks52 as S
import looks54 as J

hexc, mix, save = L.hexc, L.mix, L.save
shade = B.shade
AREA_TEX = J.AREA_TEX
PATH_TILES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "resourcepacks", "retromon_look", "assets",
                          "minecraft", "optifine", "ctm", "retromon", "path_repeat")


def _img(w=16, h=None):
    return Image.new("RGBA", (w, h or w))


def _put(im, x, y, c, a=255):
    im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (a,))


def path_kanto():
    """Kanto's dirt path as one picture four blocks across (laid @4): the look pack's sixteen path tiles where its
    repeat lays them (the block x across, z down), so the brown powder John edges Johto's paths with runs on from them."""
    im = _img(64)
    for i in range(16):
        p = os.path.join(PATH_TILES, "%d.png" % i)
        if not os.path.exists(p): continue
        t = Image.open(p).convert("RGBA").resize((16, 16))
        im.paste(t, ((i % 4) * 16, (i // 4) * 16))
    return im


def tint_of(name, out, base):
    """One of our textures (a Gym roof's) in another colour: its light and dark kept, on `base`."""
    src = os.path.join(AREA_TEX, name + ".png")
    if not os.path.exists(src): return
    im = Image.open(src).convert("RGBA")
    p = im.load()
    lum = [0.3 * p[x, y][0] + 0.59 * p[x, y][1] + 0.11 * p[x, y][2] for y in range(im.height) for x in range(im.width) if p[x, y][3] > 0]
    mean = sum(lum) / max(1, len(lum))
    for y in range(im.height):
        for x in range(im.width):
            r_, g_, b_, a = p[x, y]
            if a == 0: continue
            k = (0.3 * r_ + 0.59 * g_ + 0.11 * b_ - mean) / 255.0 * 1.25
            c = mix(base, (255, 255, 255), k) if k > 0 else mix(base, (0, 0, 0), -k)
            p[x, y] = c[:3] + (a,)
    save(out, im)


def shelf_of(src, base, out, keep=0.3):
    v = os.path.join(L.VANILLA, "textures", "block", src + ".png")
    if os.path.exists(v): save(out, L.recolour_flat(v, base, keep))


def post_top(base):
    """A post's end grain: a dark rim, its rings."""
    im = _img()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            k = -0.14 if d > 6.5 else (0.06 if int(d) % 3 == 0 else -0.03)
            _put(im, x, y, shade(base, k))
    return im


def steel_post():
    """Route 27's posts (John's oak fence posts, the chain hung between them): dark painted steel, a light cap band
    at the top and a darker foot."""
    base = hexc("#4E5864")
    im = _img()
    for y in range(16):
        for x in range(16):
            k = 0.04 if x in (6, 7) else -0.06 if x in (9,) else 0.0
            if y <= 1: k = 0.32 if y == 0 else 0.22
            elif y == 2: k = -0.12
            elif y >= 14: k -= 0.1
            _put(im, x, y, shade(base, k))
    return im


def mart_symbol():
    """A Mart's sign (13:53: "just like on the Center, you've got the white background and then the red symbol"):
    the round symbol John set in front of the white disc the Mart's blue, as the Center's is its red, each block a
    clean blue with a soft bevel (so it reads the same whichever way the blocks fall)."""
    blue = hexc("#3A6FC4")
    im = _img()
    for y in range(16):
        for x in range(16):
            k = 0.1 if (x == 0 or y == 0) else -0.12 if (x == 15 or y == 15) else 0.0
            _put(im, x, y, shade(blue, k))
    return im


def house_door(base, half):
    """Blackthorn's house doors (13:53: "the doors ... still incorrect"; John's warped doors): a crisp painted door in
    his teal, a darker frame, two inset panels, a small window up top, a brass handle."""
    frame, inset, glass, brass = shade(base, -0.3), shade(base, -0.12), hexc("#BFE3EE"), hexc("#E2B84A")
    im = _img()
    for y in range(16):
        for x in range(16):
            c = base
            if x in (0, 15) or (half == "top" and y == 0) or (half == "bottom" and y == 15): c = frame
            elif x == 1 or (half == "top" and y == 1): c = shade(base, 0.12)
            elif half == "top" and 4 <= x <= 11 and 3 <= y <= 9:
                c = frame if x in (4, 11) or y in (3, 9) else (shade(glass, 0.1) if x + y < 13 else glass)
            elif (half == "top" and 4 <= x <= 11 and 11 <= y <= 15) or (half == "bottom" and 4 <= x <= 11 and 0 <= y <= 12):
                edge_top = (half == "top" and y == 11) or (half == "bottom" and y == 0 and False)
                c = shade(inset, 0.1) if x == 4 or edge_top else shade(inset, -0.08) if x == 11 or (half == "bottom" and y == 12) else inset
            if half == "bottom" and x in (12, 13) and 1 <= y <= 3: c = brass if y < 3 else shade(brass, -0.25)
            _put(im, x, y, c)
    return im


def ds_panel(base, seed):
    """Goldenrod's Dept. Store's facing (John's blue and red): a crisp panel, a fine joint at its foot and side."""
    return J.panel(base, seed)


def build():
    # Johto's paths (13:53: "it needs to follow the same for Kanto").
    save("path_kanto", path_kanto())
    # Blackthorn's houses (13:53): the roof tiles in John's warm brown (the Gyms' tiles, recoloured), the band between
    # the floors and the trims under the windows clean wood, the posts either side of the doors dark timber.
    for a, b in (("roof_gym_tiles", "roof_bt_tiles"), ("roof_gym_edge", "roof_bt_edge"), ("roof_gym_under", "roof_bt_under")):
        tint_of(a, b, hexc("#93603F"))
    save("trim_jungle", C.trim_boards(hexc("#A87652")))
    shelf_of("jungle_shelf", hexc("#A87652"), "band_shelf")
    save("post_bt", S.dojo_wood(hexc("#4C3527"), "btp"))
    save("post_bt_top", post_top(hexc("#4C3527")))
    for half in ("top", "bottom"): save("door_bt_" + half, house_door(hexc("#2E7C77"), half))
    # The chimneys' caps (Cherrygrove's jungle shelves, New Bark's birch, Blackthorn's): grey stone, as their tops.
    shelf_of("jungle_shelf", hexc("#8E8F8C"), "cap_shelf")
    # Cherrygrove's lamps' feet (13:53: "the bottom of the lights is still in chisel"): the lamp's own dark red metal.
    save("lamp_cg_chisel", K.lamp_chisel(hexc("#8E2A3E")))
    # Route 27's posts and chains (13:53): steel, one barrier along the cliff.
    save("post_steel", steel_post())
    shelf_of("iron_chain", hexc("#5E6873"), "chain_steel", keep=0.45)
    # Every Mart's sign (13:53).
    save("mart_sign_face", mart_symbol())
    # Goldenrod's Dept. Store (13:55: "the old blue ... the old slabs").
    save("ds_blue", ds_panel(hexc("#3F7CC8"), "dsb"))
    save("ds_blue_light", ds_panel(hexc("#86B6E4"), "dsl"))
    save("ds_red", ds_panel(hexc("#C9453D"), "dsr"))
    save("ds_roof", ds_panel(hexc("#A5333A"), "dsq"))
    save("ds_steel", L.metal(hexc("#9AA3AD"), seam=False))
