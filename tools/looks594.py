"""
0.59.4's looks (John's Kanto fly-round, 07:31 and 07:36 on 2026-11-24): the pictures for tools/arealooks594.py.

The fires (their logs, embers and flames, and the smoke over them) in our style; iron bars a proper iron fence; the end
rods that are lights a glowing tube in a metal cap; the volcano's lava one seamless flow, its pools and its rock alike;
Lavender's floor as flagstones that tile.

Run: called from tools/lookpack.py (looks48.EXTRA, after looks59).
"""

import json
import math
import os
import random

from PIL import Image

import lookarea as L
import looks48b as B
import looks57 as P
import looks59 as Q

hexc, mix, save = L.hexc, L.mix, L.save
shade = B.shade
_img, _put = P._img, P._put
VAN = os.path.join(L.VANILLA, "textures", "block")
LOOK = P.LOOK


def _rgba(c, a=255):
    return tuple(int(max(0, min(255, q))) for q in c[:3]) + (a,)


# ---------- the fires (07:31: "fires ... the wood and the flames and stuff ... stylizing it to our style") ----------
def fire_log(lit_frame=None, frames=4):
    """A campfire's logs as Minecraft lays them out (bark along the top rows, the cut end at the left of the next, the
    bed of ash below), in our clean wood: warm bark with a soft grain, a pale cut end with its rings, a charcoal bed.
    Lit (lit_frame 0 to frames-1): glowing cracks along the bark and embers in the bed, breathing from frame to frame."""
    van = Image.open(os.path.join(VAN, "campfire_log.png")).convert("RGBA")
    vp = van.load()
    r = random.Random("firelog%s" % lit_frame)
    im = _img()
    bark, bark_d, end, end_d, ash, ash_d = hexc("#8A5A36"), hexc("#5E3B22"), hexc("#D8B27C"), hexc("#A97F4C"), hexc("#3E3A3A"), hexc("#2A2727")
    glow = 0.0 if lit_frame is None else 0.55 + 0.45 * math.sin(lit_frame / frames * 2 * math.pi)
    for y in range(16):
        for x in range(16):
            if vp[x, y][3] == 0: continue
            if y < 4 or (y < 8 and x >= 4):
                # The bark (a long log's sides and top): grain running along it, a darker line between staves.
                k = 0.06 * math.sin(x * 0.9 + y * 2.1) + (r.random() - 0.5) * 0.05
                c = shade(bark_d if y in (0, 3) else bark, k)
                if lit_frame is not None and y in (1, 2) and (x + 3 * y) % 7 == 0:
                    c = mix(hexc("#FF9A2E"), hexc("#FFD45A"), glow)
            elif y < 8:
                # The cut end: rings round the heart.
                d = math.hypot(x - 1.5, y - 5.5)
                c = shade(end if int(d * 1.6) % 2 == 0 else end_d, (r.random() - 0.5) * 0.04)
                if x in (0, 3) or y in (4, 7): c = shade(bark_d, 0)
            else:
                # The bed: charcoal and ash, speckled; lit, clusters of embers glowing in it, breathing.
                rb = random.Random("bed%d,%d" % (x, y))
                k = (rb.random() - 0.5) * 0.14
                c = shade(ash_d if rb.random() < 0.3 else ash, k)
                if lit_frame is not None:
                    e = 0.0
                    for (ex, ey, er) in ((3, 10, 2.2), (9, 9, 2.6), (13, 12, 2.0), (6, 13, 2.4), (11, 14, 1.6), (1, 14, 1.5)):
                        e = max(e, 1 - math.hypot(x - ex, y - ey) / er)
                    e *= 0.75 + 0.25 * math.sin(lit_frame / frames * 2 * math.pi + x * 0.9 + y * 1.3)
                    if e > 0.15:
                        c = mix(hexc("#C9441A"), hexc("#FFC247"), min(1.0, (e - 0.15) * 1.8))
            _put(im, x, y, c)
    return im


def fire_flame(frames=8):
    """A campfire's flame, our way: a clean tongue of fire, orange outside, gold, then a pale core, its tips flickering
    from frame to frame (the frames one above the other)."""
    sheet = Image.new("RGBA", (16, 16 * frames))
    outer, mid, inner, core = hexc("#F2762A"), hexc("#FFA733"), hexc("#FFD45C"), hexc("#FFF4C9")
    for f in range(frames):
        t = f / frames * 2 * math.pi
        for y in range(16):
            for x in range(16):
                # Height above the logs (0 at the bottom, 1 at the top): a rounded body low down, three licking tongues
                # above it (the middle one tallest), each swaying and stretching in turn.
                h = (15.5 - y) / 15.0
                v = 0.0
                # The body: a soft round blob sitting on the logs.
                bd = math.hypot((x - 7.5) / 5.6, (h - 0.24) / 0.3)
                v = max(v, 1 - bd)
                for ox, top, ph, wid in ((-3.0, 0.66, 0.0, 2.3), (0.0, 0.97, 2.1, 2.8), (3.1, 0.74, 4.2, 2.2)):
                    th = top + 0.07 * math.sin(t + ph)
                    if h > th: continue
                    tx = 7.5 + ox * (1 - h * 0.5) + 0.8 * math.sin(t + ph + h * 4) * h
                    ww = wid * (1 - h / th) ** 0.7 + 0.35
                    v = max(v, 1 - abs(x - tx) / ww)
                if v <= 0.04: continue
                c = outer if v < 0.3 else mid if v < 0.55 else inner if v < 0.8 else core
                if h > 0.55 and c == core: c = inner
                a = 255 if v > 0.1 else 160
                _put(sheet, x, y + 16 * f, c, a)
    return sheet


def smoke(n, of=12):
    """The smoke rising off a fire (Minecraft's big_smoke_0 to 11, each older than the last): soft round puffs, pale
    warm grey with a shaded underside, thinning as they go."""
    r = random.Random("smoke%d" % n)
    im = _img()
    age = n / (of - 1)
    puffs = [(7.5 + (r.random() - 0.5) * 2, 8 + (r.random() - 0.5) * 2, 5.6 - age * 1.6)]
    for _ in range(3):
        puffs.append((7.5 + (r.random() - 0.5) * 7, 8 + (r.random() - 0.5) * 6, 2.6 + r.random() * 1.6 - age * 0.8))
    light, dark = hexc("#ECE8E2"), hexc("#B9B2A8")
    for y in range(16):
        for x in range(16):
            best = 0.0
            for (px, py, pr) in puffs:
                d = math.hypot(x - px, y - py) / max(pr, 0.8)
                if d < 1: best = max(best, 1 - d * d)
            if best <= 0: continue
            shadeK = max(0.0, min(1.0, (y - 6) / 9))
            c = mix(light, dark, shadeK * 0.8)
            a = int(255 * min(1.0, best * 1.8) * (1 - age * 0.55))
            if a > 8: _put(im, x, y, c, a)
    return im


# ---------- iron bars and end rods (07:36) ----------
def railing():
    """Iron bars as a proper iron fence (07:36: "just going up without bits across ... a proper sort of iron fence"):
    round bars every four pixels (one through the middle, for the post), a rail at the top and the bottom, a point on
    each bar."""
    im = _img()
    iron, hi, lo = hexc("#2C3238"), hexc("#55606A"), hexc("#1B1F23")
    bars = (3, 7, 11, 15)
    for x in range(16):
        for y in range(16):
            on_bar = any(x == b or x == (b + 1) % 16 for b in bars)
            rail = y in (3, 13)
            if on_bar and y >= 1 or rail:
                c = hi if (on_bar and x in (3, 7, 11, 15)) else iron
                if rail and not on_bar: c = iron if y == 3 else lo
                _put(im, x, y, c)
            elif y == 0 and x in (4, 8, 12, 0):
                _put(im, x, y, hi)
    return im


def rod_light():
    """An end rod that's a light (07:36: "the end rods for the light source ... think about the style"): a glowing warm
    tube (the rod's two-pixel sides and its end, as Minecraft lays them out) in a brushed metal cap (the base)."""
    im = Image.new("RGBA", (16, 16))
    for y in range(15):
        for x in range(2):
            _put(im, x, y, hexc("#FFF6DE") if x == 0 else hexc("#F6DFA6"))
    for y in range(2):
        for x in range(2, 4): _put(im, x, y, hexc("#FFFBEF"))
    for y in range(2, 6):
        for x in range(2, 6):
            edge = x in (2, 5) or y in (2, 5)
            _put(im, x, y, hexc("#5D646B") if edge else hexc("#8B939A"))
    for x in range(2, 6): _put(im, x, 6, hexc("#6C737A"))
    return im


# ---------- the lava (07:36: "if that could all look seamless in terms of like next to each other") ----------
def lava_seamless(size=16, frames=16, seed="lava594"):
    """Molten lava that tiles (no seam at a block's edge): every wave a whole number of times across the picture and the
    crust streaks wrapped round, drifting down frame by frame."""
    r = random.Random(seed)
    streaks = [(r.random() * size, r.random() * size, 0.8 + r.random() * 1.4) for _ in range(5)]
    sheet = Image.new("RGBA", (size, size * frames))
    tau = 2 * math.pi
    for f in range(frames):
        shift = f * size / frames
        for y in range(size):
            for x in range(size):
                yy = (y - shift) % size
                v = 0.5 + 0.25 * math.sin(tau * (x / size * 2 + yy / size)) + 0.25 * math.sin(tau * (yy / size * 3 + x / size))
                crust = 0.0
                for (sx, sy, w) in streaks:
                    dx = min(abs(x - sx), size - abs(x - sx))
                    dy = min(abs(yy - sy), size - abs(yy - sy))
                    if dy < 4: crust = max(crust, math.exp(-dx * dx / (2 * w * w)) * (1 - dy / 4))
                hot = mix(hexc("#FF7A1E"), hexc("#FFD24A"), max(0, min(1, v)))
                c = mix(hot, hexc("#B8361C"), min(0.75, crust * 0.85))
                sheet.putpixel((x, y + f * size), _rgba(c))
    return sheet


def tile2(sheet, size, frames):
    """A 16-pixel animation laid 2 x 2 (32 pixels a frame): the same lava at the same scale where it's laid over two blocks."""
    out = Image.new("RGBA", (size * 2, size * 2 * frames))
    for f in range(frames):
        fr = sheet.crop((0, f * size, size, (f + 1) * size))
        for oy in (0, size):
            for ox in (0, size): out.paste(fr, (ox, f * size * 2 + oy))
    return out


def _anim(path, frametime, interpolate=True):
    with open(path + ".mcmeta", "w") as fh:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, fh)


def build():
    area = P.AREA_TEX
    # The fires.
    save("fire_log", fire_log())
    lit = Image.new("RGBA", (16, 64))
    for f in range(4): lit.paste(fire_log(f, 4), (0, f * 16))
    lit.save(os.path.join(area, "fire_log_lit.png"))
    _anim(os.path.join(area, "fire_log_lit.png"), 20)
    flame = fire_flame()
    flame.save(os.path.join(area, "fire_flame.png"))
    _anim(os.path.join(area, "fire_flame.png"), 3, False)
    # Their smoke (the look pack's particles, so every fire's smoke).
    part = os.path.join(LOOK, "particle")
    os.makedirs(part, exist_ok=True)
    for n in range(12): smoke(n).save(os.path.join(part, "big_smoke_%d.png" % n))
    # Iron bars, every railing_dark too; the lights' rods.
    save("railing_dark", railing())
    save("rod_light", rod_light())
    # The lava: the volcano's rock-lava (laid over two blocks) and its pools (a block's worth) one seamless flow.
    still = lava_seamless()
    lay2 = tile2(still, 16, 16)
    lay2.save(os.path.join(area, "lava_flow57.png"))
    _anim(os.path.join(area, "lava_flow57.png"), 3)
    blk = os.path.join(LOOK, "block")
    os.makedirs(blk, exist_ok=True)
    still.save(os.path.join(blk, "lava_still.png"))
    _anim(os.path.join(blk, "lava_still.png"), 3)
    lay2.save(os.path.join(blk, "lava_flow.png"))
    _anim(os.path.join(blk, "lava_flow.png"), 3)
    # Lavender's floor: flagstones that tile, its grey and its darker grey.
    save("lav_flag2", Q.seamless_flags(hexc("#6C6476"), "lavf2", hexc("#4E4757")))
    save("lav_flag2_dark", Q.seamless_flags(hexc("#524B5D"), "lavf2d", hexc("#3A3443")))
