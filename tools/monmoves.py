"""
Retromon's move effects: one effect for each of the 251 moves, made once and played by whichever creature uses the move,
from its own mouth, back or body. They're Bedrock models and animations, as the creatures are, so the game can play them
the same way in battles.

Each effect's root sits at the user's mouth, facing the target (-z). Anchor bones mark the other places an effect can
come from or land on; whoever plays it moves them to suit the creature (its back, its body, the ground under it, the
target D pixels in front). Every effect has three animations, stage1 to stage3: moves grow as creatures evolve (Razor
Leaf fires 3 leaves, then 7, then 10). Special moves wrap the user in an aura of its type (the aura_<type> effects).

Run: python3 tools/monmoves.py
"""

import json, math, os, sys

import numpy as np
from PIL import Image

from monlib import Model, Cube, Mat, CLEAR, FRAMES, ROOT, rgb, mix, hash01, pack, paint, keys, blade_mat

OUT = os.path.join(ROOT, "moves")
OUT_TEX = os.path.join(ROOT, "textures", "moves")
DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "retromon", "data")

D = 56.0                      # from the user's mouth to where a move lands on the target (3.5 blocks)
F = FRAMES
H_ = [0.001, 0.001, 0.001]   # tucked away
L0 = 0.3                      # when a move leaves the user (after its wind-up)
K = {1: 1.0, 2: 1.22, 3: 1.45}

# The anchors, with where they sit for a middling creature (the player moves them to fit): parent, pivot.
ANCH = {"at_back": ("root", [0, 6, 12]), "at_body": ("root", [0, -3, 12]), "at_ground": ("root", [0, -10, 4]),
        "at_target": ("root", [0, 0, -D]), "at_tground": ("root", [0, -10, -D]), "at_sky": ("at_tground", [0, 38, -D]),
        "at_mid": ("root", [0, 4, -D / 2])}

# ---------- colours: white, core, mid, edge, dark ----------
PAL = {
    "NORMAL": ("#FFFFFF", "#FFF4D8", "#E8DCB8", "#A89C80", "#5A5040"),
    "FIRE": ("#FFF8D2", "#FFD640", "#FF9A24", "#E8501C", "#7A1E12"),
    "WATER": ("#F2FCFF", "#B8ECFF", "#5EC0F4", "#2A72D8", "#123C78"),
    "ELECTRIC": ("#FFFFF0", "#FFF27A", "#FFD21E", "#E09A10", "#6A4A08"),
    "GRASS": ("#F4FFE0", "#B8F070", "#6CC840", "#2E8A2A", "#164A18"),
    "ICE": ("#FFFFFF", "#DFF8FF", "#9EE4F8", "#4EA8E0", "#1E5A8A"),
    "FIGHTING": ("#FFF4E8", "#FFC07A", "#FF7A3A", "#C8341E", "#6A140C"),
    "POISON": ("#FBEFFF", "#E2A8FF", "#B060E0", "#7A2AA8", "#3A1050"),
    "GROUND": ("#FFF4DA", "#EED096", "#CFA060", "#8A6030", "#4A3018"),
    "FLYING": ("#FFFFFF", "#EAF0FF", "#BCCAFF", "#7A8CE0", "#3A4888"),
    "PSYCHIC": ("#FFF0F8", "#FFB8DE", "#FF6EB4", "#C8307E", "#6A1040"),
    "BUG": ("#FAFFE0", "#DCF070", "#A8CC30", "#6A8A14", "#344808"),
    "ROCK": ("#F6EEDC", "#D8C49A", "#B09A6A", "#7A6440", "#3E3020"),
    "GHOST": ("#F4EEFF", "#C8B4FF", "#8A6AE8", "#5038A8", "#24184E"),
    "DRAGON": ("#EEF0FF", "#B4B4FF", "#7A6AFF", "#4630C8", "#201060"),
    "DARK": ("#E8E0F0", "#9A88B0", "#5E4A72", "#34243E", "#160E1C"),
    "STEEL": ("#FFFFFF", "#E8F0F8", "#B8C8D8", "#7A8C9E", "#3A4652"),
    "CURSE": ("#E8FFF4", "#9AE6C4", "#5AB890", "#2A7058", "#10382A"),
    # Not types: the looks of particular things.
    "HEAL": ("#FFFFFF", "#E2FFE4", "#8CF0A0", "#38B45E", "#145A2A"),
    "HEART": ("#FFFFFF", "#FFC4DA", "#FF6E9E", "#D0306A", "#6A1030"),
    "ANGER": ("#FFFFFF", "#FFB0A0", "#FF4A30", "#B81C10", "#5A0A06"),
    "SLEEP": ("#FFFFFF", "#E4ECFF", "#A8BCF8", "#5A6EC8", "#2A3470"),
    "GOLD": ("#FFFBE0", "#FFE070", "#E8B020", "#A06A10", "#4A3008"),
    "INK": ("#E8E8F0", "#8A8AA0", "#4A4A5E", "#24242E", "#101016"),
    "SMOKE": ("#F4F2F0", "#D8D2CC", "#A8A09A", "#6E6862", "#3A3632"),
    "AURORA": ("#FFFFFF", "#FFC8EC", "#A8F0FF", "#C8FFB0", "#5A6A8A"),
    "STAR": ("#FFFBE0", "#FFE45A", "#FFD83A", "#E08A1A", "#6A3A08"),
    "SKIN": ("#FFF4EC", "#FFD8C0", "#F2B898", "#B87A5A", "#5A3020"),
    "BONE": ("#FFFFFF", "#F6F0E0", "#E2D6BA", "#A89A78", "#5A5040"),
    "PETAL": ("#FFFFFF", "#FFD8EC", "#FF9ACC", "#E0609E", "#7A2050"),
    "WOOD": ("#F8E8D0", "#D8B080", "#A87A48", "#6A4A28", "#342410"),
    "TRI_F": ("#FFF8D2", "#FFD640", "#FF7A24", "#D8401C", "#7A1E12"),
    "TRI_I": ("#FFFFFF", "#DFF8FF", "#8ADCF8", "#3E98D8", "#1E5A8A"),
    "TRI_E": ("#FFFFF0", "#FFF27A", "#FFD21E", "#D89010", "#6A4A08"),
}


def pal(name):
    w, c, m, e, d = PAL[name]
    return {"W": w, "C": c, "M": m, "E": e, "D": d}


# ---------- where a texel sits on a flat part ----------

def uv_bill(at):
    """A part standing up, facing along z: across (left to right as seen) and up."""
    return (at.col + .5) / at.fw, 1 - (at.row + .5) / at.fh


def uv_long(at):
    """A part lying along z (a beam, a jet): along it from the mouth (0) to the far end (1), and across it."""
    c = at.cube
    return 1 - at.lz, (at.ly if c.size[0] == 0 else at.lx)


def uv_flat(at):
    """A part lying flat on the ground: across, and forward (away from the user)."""
    return at.lx, 1 - at.lz


def uv_vert(at):
    """A standing part read top to bottom (a bolt from the sky): down it, and across."""
    u, v = uv_bill(at)
    return 1 - v, u


UV = {"bill": uv_bill, "long": uv_long, "flat": uv_flat, "vert": uv_vert}


# ---------- the shapes (u, v from 0 to 1; frame fi): a colour key (W C M E D) and an alpha, or None ----------

def _polar(u, v):
    dx, dy = u - .5, v - .5
    return math.hypot(dx, dy) * 2, math.atan2(dy, dx)


def sh_orb(u, v, fi):
    r, th = _polar(u, v)
    r /= 1 + 0.07 * math.sin(th * 5 + fi * 1.05)
    if r > 1:
        return None
    return ("W", 255) if r < .32 else ("C", 255) if r < .58 else ("M", 235) if r < .82 else ("E", 185)


def sh_glow(u, v, fi):
    r, th = _polar(u, v)
    r /= 1 + 0.05 * math.sin(th * 4 + fi)
    if r > 1:
        return None
    return ("W" if r < .25 else "C" if r < .5 else "M"), int(30 + 200 * (1 - r) ** 0.9)


def sh_spark(u, v, fi):
    r, th = _polar(u, v)
    th += fi * 0.1
    R = max(0.16, 0.95 * abs(math.cos(2 * th)) ** 12, 0.55 * abs(math.sin(2 * th)) ** 16)
    if r > R:
        return None
    k = r / R
    return ("W", 255) if k < .4 else ("C", 255) if k < .75 else ("M", 230)


def sh_pow(u, v, fi):
    dx, dy = u - .5, v - .5
    r = math.hypot(dx, dy)
    th = math.atan2(dy, dx) + fi * 0.05
    i = int(math.floor((th + math.pi) / (2 * math.pi) * 10))
    f = ((th + math.pi) / (2 * math.pi) * 10) % 1.0
    tip = 0.47 if i % 2 == 0 else 0.33
    R = 0.17 + (tip - 0.17) * (1 - abs(f - 0.5) * 2) ** 1.6
    if r <= R:
        k = r / R
        return ("W", 255) if k < .38 else ("C", 255) if k < .66 else ("M", 255) if k < .86 else ("E", 255)
    if abs(r - (0.3 + 0.03 * fi)) < 0.025 and r < 0.5:
        return "W", 170
    return None


def sh_ring(u, v, fi, r0=.82, w=.12, dash=12):
    r, th = _polar(u, v)
    if abs(r - r0) > w:
        return None
    if dash and int((th + math.pi) / (2 * math.pi) * dash + fi * 0.3) % 3 == 2:
        return None
    k = abs(r - r0) / w
    return ("W", 240) if k < .35 else ("C", 220) if k < .7 else ("M", 160)


def sh_rings(u, v, fi):
    """Sound: three rings spreading out."""
    r, th = _polar(u, v)
    for i, r0 in enumerate((.3, .58, .86)):
        rr = r0 + 0.04 * fi / F
        if abs(r - rr) < .06:
            return ("C" if i % 2 == 0 else "M"), 230 - 40 * i
    return None


def sh_jag(u, v, fi):
    """A screech: a jagged ring."""
    r, th = _polar(u, v)
    r0 = .72 + (0.12 if math.sin(th * 14 + fi * 1.2) > 0 else -0.06)
    if abs(r - r0) < .07:
        return ("W", 240) if abs(r - r0) < .03 else ("C", 220)
    return None


def sh_dots(u, v, fi):
    """Supersonic: a ring of dots."""
    r, th = _polar(u, v)
    if abs(r - .75) > .1:
        return None
    if ((th + math.pi) / (2 * math.pi) * 16 + fi * .15) % 1 < .45:
        return ("W", 240) if abs(r - .75) < .05 else ("C", 220)
    return None


def sh_slash(u, v, fi):
    """A blade's arc: a crescent, white along its heart."""
    x, y = u - .5, v - .5
    if math.hypot(x, y) > .48:
        return None
    d = math.hypot(x - .14, y + .14) - .43
    if d < 0:
        return None
    return ("W", 255) if d < .045 else ("C", 250) if d < .09 else ("M", 230)


def sh_claws(u, v, fi):
    if not 0.08 < v < 0.92:
        return None
    for c in (0.18, 0.4, 0.62):
        d = abs((u - v * 0.45) - c + 0.1)
        w = 0.05 * math.sin(math.pi * v)
        if d < w:
            return "W", 255
        if d < w + 0.04:
            return "C", 200
    return None


def sh_fang_d(u, v, fi):
    for c in (0.3, 0.7):
        hw = 0.17 * v + 0.02
        if abs(u - c) < hw:
            return ("W", 255) if abs(u - c) < hw * .6 else ("C", 255)
    return None


def sh_fang_u(u, v, fi):
    return sh_fang_d(u, 1 - v, fi)


def sh_beam(a, c, fi):
    d = abs(c - .5) * 2
    d /= 1 + 0.1 * math.sin(a * 40 - fi * 2.1)
    if d > 1:
        return None
    stripe = ((a * 9 - fi / F) % 1) < .18
    if d < .22 or (stripe and d < .42):
        return "W", 255
    return ("C", 250) if d < .45 else ("M", 230) if d < .72 else ("E", 150)


def sh_aurora(a, c, fi):
    d = abs(c - .5) * 2
    if d > 1:
        return None
    if d < .2:
        return "W", 250
    band = int(a * 14 - fi / 2) % 3
    return ("C", "M", "E")[band], int(235 - 80 * d)


def _jag(a, seed, fi, amp=.3, n=9):
    i = int(min(a, .999) * n)
    f = a * n - i

    def j(k):
        return .5 if k == 0 else .5 + (hash01("bolt", seed, k, fi) - .5) * 2 * amp
    return j(i) + (j(i + 1) - j(i)) * f


def sh_bolt(a, c, fi, seed=0, w=.08):
    p = _jag(a, seed, fi)
    d = abs(c - p)
    if d < w * .45:
        return "W", 255
    if d < w:
        return "C", 255
    if d < w * 1.9:
        return "M", 150
    if .3 < a < .8:
        side = 1 if hash01("fork", seed, fi) > .5 else -1
        q = _jag(.3, seed, fi) + (a - .3) * .8 * side
        if abs(c - q) < w * .55:
            return "C", 235
    return None


def sh_flame(u, v, fi, seed=0.0):
    ph = fi / F * 2 * math.pi
    centre = 0.5 + 0.08 * math.sin(ph + v * 3.0 + seed) * v
    widest = 0.28
    hw = 0.3 + 0.2 * math.sin(v / widest * math.pi / 2) if v < widest else 0.5 * (1 - ((v - widest) / (1 - widest)) ** 0.9)
    if v > 0.45:
        lick = abs(math.sin((u - centre) * math.pi * 2.0 + ph + seed))
        hw *= 0.55 + 0.45 * lick
        if v > 0.72 and lick < 0.45 + (v - 0.72):
            return None
    d = abs(u - centre)
    if d > hw or hw <= 0.04:
        return None
    heat = (1 - d / hw) * 0.75 + (1 - v) * 0.55
    return ("W", 255) if heat > .98 else ("C", 255) if heat > .72 else ("M", 255) if heat > .45 else ("E", 235)


def sh_cone(a, c, fi):
    """A stream of flame (Flamethrower): narrow at the mouth, roaring out wider, its edges licking."""
    ph = fi / F * 2 * math.pi
    hw = .1 + .4 * a ** .75
    edge = hw * (0.78 + 0.22 * math.sin(a * 30 - ph * 2 + c * 7))
    d = abs(c - .5 - 0.04 * math.sin(a * 12 - ph))
    if d > edge:
        return None
    if a > .8 and hash01("cone", int(a * 30), int(c * 12), fi) < (a - .8) * 4:
        return None
    heat = (1 - d / edge) * .8 + (1 - a) * .5
    return ("W", 255) if heat > 1.0 else ("C", 255) if heat > .72 else ("M", 245) if heat > .45 else ("E", 220)


def sh_torrent(a, c, fi):
    """Hydro Pump: a great rope of water, streaming, foaming white, breaking into spray at its end."""
    hw = .24 + .26 * a ** .6
    d = abs(c - .5)
    if d > hw:
        return None
    k = d / hw
    lane = round(c * 14)
    flow = (a * 3 - fi / F + hash01("tl", lane) * .7) % 1
    if k > .55 and flow < .22:
        return None
    if a > .86 and hash01("td", lane, int(a * 20), fi) < (a - .86) * 4:
        return None
    return ("W", 245) if k < .28 or flow < .1 else ("C", 235) if k < .6 else ("M", 215) if k < .85 else ("E", 170)


def sh_jet(a, c, fi):
    """Water Gun: a narrow jet, widening a little, its streaks flowing out and breaking into drops at the end."""
    hw = 0.14 + 0.3 * a ** 0.8
    d = abs(c - 0.5)
    if d > hw:
        return None
    k = d / hw
    lane = round(c * 9)
    flow = (a * 2.2 - fi / F + hash01("lane", lane) * 0.7) % 1.0
    if a > 0.3 and flow < 0.12 + 0.3 * a * k:
        return None
    if a > 0.72 and hash01("drop", lane, round(a * 12), fi) < (a - 0.72) * 2.4:
        return None
    return ("W", 235) if k < .4 else ("C", 225) if k < .75 else ("M", 190)


def sh_puff(u, v, fi):
    r, th = _polar(u, v)
    R = .78 + .12 * math.sin(th * 5 + fi * .9) + .06 * math.sin(th * 9 - fi * 1.3)
    if r > R:
        return None
    if (u - .38) ** 2 + (v - .62) ** 2 < .02:
        return "W", 200
    k = r / R
    return ("C", 215) if k < .5 else ("M", 200) if k < .8 else ("E", 160)


def sh_powder(u, v, fi):
    r, th = _polar(u, v)
    if r > 1:
        return None
    h = hash01("pw", int(u * 12), int(v * 12), fi // 2)
    if h < .3:
        return ("W", 255) if h < .06 else ("C", 255) if h < .16 else ("M", 230)
    return None


def sh_bubble(u, v, fi):
    r, th = _polar(u, v)
    r /= 1 + .04 * math.sin(th * 3 + fi)
    if r > .92:
        return None
    if (u - .36) ** 2 + (v - .66) ** 2 < .008:
        return "W", 255
    if r > .74:
        return "M", 235
    return "C", 70


def sh_drop(u, v, fi):
    hw = .36 * math.sin(math.pi * min(1, (1 - v)) ** .7)
    d = abs(u - .5)
    if v > .97 or d > hw:
        return None
    if (u - .42) ** 2 + (v - .35) ** 2 < .006:
        return "W", 255
    k = d / max(.01, hw)
    return ("C", 240) if k < .5 else ("M", 235) if k < .85 else ("E", 220)


def sh_flake(u, v, fi):
    r, th = _polar(u, v)
    if r > .95:
        return None
    a6 = (th + fi * 0.05) % (math.pi / 3)
    a6 = min(a6, math.pi / 3 - a6)
    dist = r * math.sin(a6)
    if dist < .07 or r < .18:
        return "W", 255
    if abs(r - .58) < .09 and dist < .22:
        return "C", 240
    return None


def sh_crystal(u, v, fi):
    dx, dy = u - .5, v - .5
    s = abs(dx) / .28 + abs(dy) / .48
    if s > 1:
        return None
    if s > .82:
        return "E", 235
    if dx < -.05 and dy > .05:
        return "W", 255
    return ("C", 245) if dx > 0 else ("M", 240)


def sh_wind(u, v, fi):
    r, th = _polar(u, v)
    if not .44 < r < .96:
        return None
    if (th + fi * .5) % (2 * math.pi / 3) > 1.6:
        return None
    k = abs(r - .7) / .26
    return ("W", 230) if k < .35 else ("C", 200) if k < .7 else ("M", 140)


def sh_tornado(u, v, fi):
    w = .12 + .36 * v
    d = abs(u - .5 - .05 * math.sin(v * 8 + fi))
    if d > w:
        return None
    if d > w * .82:
        return "E", 190
    return ("C" if ((v * 7 - fi / F * 2 + (u - .5) * 3) % 1) < .45 else "M"), 215


def sh_spiral(u, v, fi):
    r, th = _polar(u, v)
    if r > .95:
        return None
    s = (th / (2 * math.pi) + r * 2.2 - fi * 0.08) % 1
    if s < .22:
        return ("W", 245) if r < .22 else ("C", 240)
    return None


def sh_crack(u, v, fi):
    """A crack in the ground (lying flat, running away from the user)."""
    p = _jag(v, 7, 0, amp=.28)
    d = abs(u - p)
    if d < .05:
        return "D", 255
    if d < .1:
        return "E", 200
    for k, at in ((1, .35), (-1, .62)):
        if at < v < at + .25:
            q = _jag(at, 7, 0, amp=.28) + (v - at) * 1.3 * k
            if abs(u - q) < .045:
                return "D", 255
    return None


def sh_wave(u, v, fi):
    """Surf: a breaking wave seen face on, foaming white along its crest."""
    yt = .72 + .1 * math.sin(u * math.pi * 4 + fi * .9) + .06 * math.sin(u * math.pi * 9 - fi * 1.3)
    if v > yt:
        return None
    if v > yt - .1:
        return "W", 245
    if v < .1:
        return "E", 200
    return ("C" if ((v * 6 + u * 1.5 - fi / F) % 1) < .3 else "M"), 228


def sh_streak(a, c, fi):
    d = abs(c - .5) * 2
    if d > .35:
        return None
    return ("W" if a > .7 else "C"), int(60 + 190 * a)


def sh_needle(a, c, fi):
    d = abs(c - .5) * 2
    if a > .72:
        hw = .55 * (1 - a) / .28
        return ("W", 255) if d < hw else None
    if a < .12:
        return ("E", 230) if d < .9 else None
    return ("C", 255) if d < .4 else ("M", 200) if d < .6 else None


def sh_drill(a, c, fi):
    """A drill of wind (Drill Peck, Horn Drill): a cone tapering to its point at the target, its spiral turning."""
    hw = .48 * (1 - a) + .02
    d = abs(c - .5)
    if d > hw:
        return None
    if d > hw * .85:
        return "E", 220
    return ("W" if ((a * 6 + (c - .5) * 3 - fi / F) % 1) < .3 else "C"), 235


def sh_disc(u, v, fi):
    """A shield bubble's face: a bright rim, a faint hex pattern, see-through."""
    r, th = _polar(u, v)
    if r > .95:
        return None
    if r > .84:
        return "W", 230
    if ((u * 10) % 1 < .08) or (((u * 5 + v * 8.66) % 1) < .08) or (((u * 5 - v * 8.66) % 1) < .08):
        return "C", 120
    return "M", 55


def sh_wall(u, v, fi):
    if u < .05 or u > .95 or v < .05 or v > .95:
        return "W", 235
    if abs(v - (fi + .5) / F) < .04:
        return "W", 150
    if ((u * 8) % 1 < .07) or (((u * 4 + v * 6.9) % 1) < .07) or (((u * 4 - v * 6.9) % 1) < .07):
        return "C", 130
    return "M", 50


def sh_coin(u, v, fi):
    r, th = _polar(u, v)
    if r > .92:
        return None
    if r > .78:
        return "E", 255
    if (u - .38) ** 2 + (v - .64) ** 2 < .01:
        return "W", 255
    if .46 < r < .56:
        return "M", 255
    return "C", 255


def sh_sun(u, v, fi):
    r, th = _polar(u, v)
    if r < .5:
        return ("W", 255) if r < .3 else ("C", 255)
    if r < .96 and math.cos(12 * th + fi * .3) > .55:
        return "M", int(230 * (1.2 - r))
    return None


def sh_moon(u, v, fi):
    r, th = _polar(u, v)
    if r > .85:
        return None
    if math.hypot(u - .5 - .2, v - .5 - .12) * 2 < .72:
        return None
    return ("W", 250) if r < .6 else ("C", 240)


def sh_web(u, v, fi):
    r, th = _polar(u, v)
    if r > .95:
        return None
    a8 = th % (math.pi / 4)
    if r * math.sin(min(a8, math.pi / 4 - a8)) < .035:
        return "W", 235
    for r0 in (.32, .58, .84):
        if abs(r - r0 - .03 * math.sin(th * 8)) < .03:
            return "C", 225
    return None


def sh_coil(u, v, fi):
    r, th = _polar(u, v)
    if abs(r - .7) > .2:
        return None
    if abs(r - .7) > .15:
        return "E", 240
    return ("C" if int((th / math.pi * 6) + fi * .5) % 2 else "M"), 245


def sh_thread(a, c, fi):
    p = .5 + .25 * math.sin(a * 18 - fi)
    return ("W", 230) if abs(c - p) < .1 else None


def sh_vine(a, c, fi):
    d = abs(c - .5) * 2
    if d < .28:
        return ("M", 255) if d < .16 else ("E", 255)
    if (a * 7) % 1 < .3 and d < .7 + .2 * math.sin((a * 7) % 1 / .3 * math.pi) and c > .5:
        return "C", 245
    return None


def sh_tongue(a, c, fi):
    d = abs(c - .5) * 2
    hw = .7 if a < .85 else .7 * math.sqrt(max(0, 1 - ((a - .85) / .15) ** 2))
    if d > hw:
        return None
    return ("C", 255) if d < hw * .5 else ("M", 255) if d < hw * .85 else ("E", 255)


def sh_wisp(u, v, fi):
    ph = fi / F * 2 * math.pi
    centre = .5 + .14 * math.sin(v * 6 + ph) * v
    hw = .38 * math.sin(math.pi * min(1, v * 1.4)) ** .6 * (1 - v * .6)
    d = abs(u - centre)
    if d > hw or v > .95:
        return None
    k = d / max(.01, hw)
    return ("W", 220) if k < .3 and v < .5 else ("C", 210) if k < .6 else ("M", 180)


def sh_rain(u, v, fi):
    if abs(u - .5) > .18:
        return None
    return ("C", 220) if ((v * 2 + fi / F) % 1) < .55 else None


def sh_sand(u, v, fi):
    h = hash01("sand", int(v * 10), int((u + fi / F) * 14) % 14)
    if h < .45 and abs(((v * 10) % 1) - .5) < .3:
        return ("C" if h < .2 else "M"), 210
    return None


def sh_rays(u, v, fi):
    r, th = _polar(u, v)
    if r > .98 or r < .1:
        return None
    if math.cos(10 * th + fi * .2) > .7:
        return "C", int(170 * (1 - r))
    return None


def sh_cross(u, v, fi):
    """Lock-On's crosshair."""
    r, th = _polar(u, v)
    if r > .95:
        return None
    if abs(r - .7) < .07:
        return "W", 240
    if (abs(u - .5) < .035 or abs(v - .5) < .035) and r > .25:
        return "C", 235
    return None


def sh_petal(u, v, fi):
    if ((u - .5) / .3) ** 2 + ((v - .5) / .48) ** 2 > 1:
        return None
    return ("W", 255) if abs(u - .5) < .05 else ("C", 255) if abs(u - .5) < .17 else ("M", 255)


# ---------- little pictures, drawn pixel by pixel (O outline, then W C M D) ----------

GLYPHS = {
    "FIST": ["..OOOOOO..", ".OMCMCMCMO", ".OMCMCMCMO", "OOMMMMMMMO", "OCOMMMMMMO", "OCCOMMMMMO", "OMMMMMMMMO", ".OMMMMMMO.", "..OOOOOO.."],
    "FOOT": [".OOOO.....", ".OMMO.....", ".OMMO.....", ".OMMO.....", ".OMMMOOOO.", ".OMMMMMMMO", ".OMMMMMMMO", ".OCCCCCCCO", "..OOOOOOO."],
    "HAND": [".O.O.O.....", "OMOMOMO....", "OMOMOMO.OO.", "OMOMOMOOMO.", "OMMMMMMMO..", "OMMMMMMO...", "OMMMMMMO...", ".OMMMMO....", "..OOOO....."],
    "HEART": [".OO...OO.", "OCCO.OMMO", "OCWMOMMMO", "OCMMMMMMO", ".OMMMMMO.", "..OMMMO..", "...OMO...", "....O...."],
    "NOTE": ["....OOOOO", "....OMMMO", "....OMOOO", "....OMO..", "....OMO..", "..OOOMO..", ".OMMMMO..", ".OMMMMO..", "..OOOO..."],
    "Z": ["OOOOOOO", "OWWWWWO", "OOOOWO.", "...OWO.", "..OWO..", ".OWO...", "OWOOOOO", "OWWWWWO", "OOOOOOO"],
    "Q": ["..OOOOO..", ".OWWWWWO.", ".OWOOOWO.", "..O..OWO.", "....OWO..", "...OWO...", "...OOO...", "...OWO...", "...OOO..."],
    "X": ["OO.....OO", "OWO...OWO", ".OWO.OWO.", "..OWOWO..", "...OWO...", "..OWOWO..", ".OWO.OWO.", "OWO...OWO", "OO.....OO"],
    "EYE": ["...OOOOO...", ".OOWWWWWOO.", "OWWWOOOWWWO", "OWWOCCCOWWO", "OWWOCDCOWWO", "OWWWOOOWWWO", ".OOWWWWWOO.", "...OOOOO..."],
    "UP": ["...O...", "..OWO..", ".OWWWO.", "OWWWWWO", "OOOWOOO", "..OWO..", "..OWO..", "..OWO..", "..OOO.."],
    "GIFT": ["..OO.OO..", ".OCCOCCO.", "OOOOOOOOO", "OMMMCMMMO", "OCCCCCCCO", "OMMMCMMMO", "OMMMCMMMO", "OMMMCMMMO", "OOOOOOOOO"],
    "SWORD": ["....O....", "...OWO...", "...OWO...", "...OWO...", "...OWO...", "...OWO...", "...OWO...", ".OOOOOOO.", ".OCCCCCO.", "...OMO...",
              "...OMO...", "...OOO..."],
    "BONE": [".OO.......OO.", "OWWO.....OWWO", "OWWWOOOOOWWWO", ".OWWWWWWWWWO.", "OWWWOOOOOWWWO", "OWWO.....OWWO", ".OO.......OO."],
    "SPOON": ["..OOO..", ".OWWWO.", ".OWCWO.", ".OWWWO.", "..OWO..", "..OWO..", "..OWO..", "..OWO..", "..OOO.."],
    "BELL": ["....O....", "...OCO...", "..OCCCO..", "..OCWCO..", ".OCCCCCO.", ".OCCCCCO.", "OCCCCCCCO", "OOOOOOOOO", "....OO..."],
    "DOLL": [".OO...OO.", "OMMOOOMMO", ".OMMMMMO.", ".OMDMDMO.", ".OMMMMMO.", "..OOOOO..", ".OMMMMMO.", "OMMMMMMMO", ".OMOOOMO.", "..O...O.."],
    "NAIL": ["OOOOOOO", "OWWWWWO", "OOOWOOO", "..OWO..", "..OWO..", "..OWO..", "..OWO..", "...O..."],
    "EGG": ["..OOO..", ".OWWWO.", "OWWCWWO", "OWWWWCO", "OWCWWWO", "OWWWWWO", ".OWWWO.", "..OOO.."],
    "ANGER": [".OO...OO.", "OMMO.OMMO", "OMO...OMO", ".O.....O.", ".........", ".O.....O.", "OMO...OMO", "OMMO.OMMO", ".OO...OO."],
    "CROSS": ["...OOO...", "...OWO...", "...OWO...", "OOOOWOOOO", "OWWWWWWWO", "OOOOWOOOO", "...OWO...", "...OWO...", "...OOO..."],
    "BATON": [".OO......", "OWWO.....", "OWCWO....", ".OWCWO...", "..OWCWO..", "...OWCWO.", "....OWCWO", ".....OWWO", "......OO."],
}


def glyph_shape(name, flip=False):
    rows = GLYPHS[name][::-1] if flip else GLYPHS[name]
    H, W = len(rows), max(len(r) for r in rows)
    key = {"O": "E", "W": "W", "C": "C", "M": "M", "D": "D"}

    def f(u, v, fi):
        col, row = min(W - 1, int(u * W)), min(H - 1, int((1 - v) * H))
        ch = rows[row][col] if col < len(rows[row]) else "."
        return None if ch == "." else (key[ch], 255)
    f.__name__ = f"glyph_{name}_{flip}"
    return f, W, H


# ---------- paints ----------

_mats = {}


def G(shape, palname, mode="bill", **o):
    """A glowing, see-through paint drawn by shape (in the glowing layer, frame by frame)."""
    k = ("G", shape.__name__, palname, mode, tuple(sorted(o.items())))
    if k in _mats:
        return _mats[k]
    P = {kk: rgb(v) for kk, v in pal(palname).items()}
    to_uv = UV[mode]

    def g(at, fi):
        u, v = to_uv(at)
        r = shape(u, v, fi, **o)
        return None if r is None else (P[r[0]], r[1])
    m = Mat(PAL[palname][2], lambda a: CLEAR, glow=g, frames=True, noise=0, edge=0)
    _mats[k] = m
    return m


def S(shape, palname, mode="bill", **o):
    """A solid paint drawn by shape (a coin, a bone): lit like the creatures, not glowing."""
    k = ("S", shape.__name__, palname, mode, tuple(sorted(o.items())))
    if k in _mats:
        return _mats[k]
    P = pal(palname)
    to_uv = UV[mode]

    def f(at):
        u, v = to_uv(at)
        r = shape(u, v, 0, **o)
        return CLEAR if r is None else P[r[0]]
    m = Mat(PAL[palname][2], f, noise=0.02, edge=0, flat=True)
    _mats[k] = m
    return m


def rock_mat(palname="ROCK"):
    k = ("rock", palname)
    if k not in _mats:
        P = pal(palname)
        _mats[k] = Mat(P["M"], lambda at: P["E"] if hash01("rk", int(at.x), int(at.y), int(at.z)) < .3 else
                       (P["C"] if hash01("rk2", int(at.x), int(at.z)) < .2 else None), noise=0.05, edge=0.12)
    return _mats[k]


def leaf_mat(palname="GRASS"):
    k = ("leaf", palname)
    if k not in _mats:
        P = pal(palname)
        _mats[k] = blade_mat(P["M"], P["E"], P["C"])
    return _mats[k]


# ---------- an effect being built ----------

class FX:
    def __init__(self, key):
        self.id = key
        self.m = Model()
        self.m.bone("root", None, [0, 0, 0])
        self.pv = {"root": [0, 0, 0]}
        for name, (parent, pos) in ANCH.items():
            self.m.bone(name, parent, pos)
            self.pv[name] = list(pos)
        self.ev = {1: {}, 2: {}, 3: {}}
        self.n = 0

    def spr(self, s, slot, mat, w, h, at="root", off=(0, 0, 0), kind="bill", planes=2, rot=None):
        """A part: crossed flat parts standing up (bill), one lying flat (flat), crossed ones lying along z from its
        point out to h in front (long), one standing up from its point (vert), or a solid box (box)."""
        name = f"s{s}_{slot}"
        assert name not in self.pv, (self.id, name)
        P = [a + b for a, b in zip(self.pv[at], off)]
        x, y, z = P
        if kind == "bill":
            cubes = [Cube([x - w / 2, y - h / 2, z], [w, h, 0], mat, [0, 90, 0] if i else None, P) for i in range(planes)]
        elif kind == "vert":
            cubes = [Cube([x - w / 2, y, z], [w, h, 0], mat, [0, 90, 0] if i else None, P) for i in range(planes)]
        elif kind == "flat":
            cubes = [Cube([x - w / 2, y, z - h / 2], [w, 0, h], mat)]
        elif kind == "long":
            cubes = [Cube([x, y - w / 2, z - h], [0, w, h], mat), Cube([x - w / 2, y, z - h], [w, 0, h], mat)]
        elif kind == "leaf":
            cubes = [Cube([x - w / 2, y, z - h / 2], [w, 0, h], mat), Cube([x - w / 2, y, z - h / 2], [w, 0, h], mat, [0, 0, 90], P)]
        elif kind == "box":
            cubes = [Cube([x - w / 2, y - h / 2, z - w / 2], [w, h, w], mat)]
        else:
            raise ValueError(kind)
        self.m.bone(name, at, P, cubes, rotation=rot)
        self.pv[name] = P
        return name

    def group(self, s, slot, at="root", off=(0, 0, 0)):
        name = f"s{s}_{slot}"
        assert name not in self.pv, (self.id, name)
        P = [a + b for a, b in zip(self.pv[at], off)]
        self.m.bone(name, at, P, [])
        self.pv[name] = P
        return name

    def key(self, s, bone, ch, pts):
        self.ev[s].setdefault(bone, {}).setdefault(ch, {}).update({round(t, 3): v for t, v in pts.items()})

    def show(self, s, b, t0, t1, k=1.0, k1=None, pop=0.06):
        """Shown from t0 to t1 (popping up from small), sized k (growing to k1 by the end), tucked away otherwise."""
        k1 = k if k1 is None else k1
        v = lambda q: list(q) if isinstance(q, (list, tuple)) else [q, q, q]
        pop = min(pop, max(0.02, (t1 - t0) / 3))
        pts = {}
        if t0 > 0.015:
            pts[0] = H_
            pts[t0 - 0.01] = H_
        pts[t0] = [c * 0.5 for c in v(k)]
        pts[t0 + pop] = v(k)
        pts[t1] = v(k1)
        pts[t1 + 0.01] = H_
        self.key(s, b, "scale", pts)

    def pos(self, s, b, pts):
        self.key(s, b, "position", pts)

    def rot(self, s, b, pts):
        self.key(s, b, "rotation", pts)

    def nominal(self, a, b):
        """How far anchor b is from anchor a, as they sit by default."""
        return [y - x for x, y in zip(self.pv[a], self.pv[b])]

    def anims(self):
        out, lens = {}, {}
        for s in (1, 2, 3):
            bones, L = {}, 0.0
            for b in self.m.bones:
                tr = self.ev[s].get(b.name)
                if b.name == "root" or b.name in ANCH:
                    if tr:
                        bones[b.name] = {ch: keys(p) for ch, p in tr.items()}
                        L = max(L, max(max(p) for p in tr.values()))
                    continue
                mine = b.name.startswith(f"s{s}_")
                if not mine:
                    if b.parent == "root" or b.parent in ANCH:
                        bones[b.name] = {"scale": H_}
                    continue
                if not tr:
                    continue
                bones[b.name] = {ch: keys(p) for ch, p in tr.items()}
                L = max(L, max(max(p) for p in tr.values()))
            L = round(L + 0.05, 2)
            out[f"animation.move.{self.id}.stage{s}"] = {"loop": False, "animation_length": L, "bones": bones}
            lens[s] = L
        return out, lens


def rnd(*k):
    return hash01("fx", *k) * 2 - 1


# ---------- the parts moves are made of: each adds itself for stage s (size k, count n) and gives its times ----------

def p_hit(fx, s, k, n, P, glyph=None, fx_shape="pow", big=1.0, dt=0.16, extra=(), gpal=None, t=None, slot="h", at="at_target", spin=25):
    """Blows landing on the target: a burst (or claw marks, a slash, a spark...), a fist or foot coming in, extras."""
    t = L0 if t is None else t
    hits = []
    for i in range(n):
        ti = t + i * dt
        dx, dy = (0, 0) if n == 1 else (rnd(s, slot, i) * 5 * k, rnd(s, slot, i, 1) * 4 * k)
        if fx_shape:
            shp = {"pow": sh_pow, "spark": sh_spark, "claws": sh_claws, "slash": sh_slash, "ring": sh_ring}[fx_shape]
            sz = {"pow": 14, "spark": 12, "claws": 12, "slash": 14, "ring": 14}[fx_shape]
            b = fx.spr(s, f"{slot}{i}", G(shp, P), sz, sz, at, (dx, dy, 3))
            fx.show(s, b, ti, ti + 0.22, k * big, k * big * (1.5 if fx_shape in ("pow", "ring", "spark") else 1.1))
            r0 = rnd(s, slot, i, 2) * 40
            fx.rot(s, b, {ti: [0, 0, r0], ti + 0.22: [0, 0, r0 + (spin if fx_shape != "slash" else 0)]})
            if fx_shape in ("slash", "claws"):
                fx.pos(s, b, {ti: [3 * k, 3 * k, 0], ti + 0.14: [-2 * k, -2 * k, 0]})
        if glyph:
            g, W, Hh = glyph_shape(glyph)
            b = fx.spr(s, f"{slot}g{i}", G(g, gpal or P), W, Hh, at, (dx, dy, 6))
            fx.show(s, b, ti - 0.1, ti + 0.16, k * big * 1.1)
            fx.pos(s, b, {ti - 0.1: [0, 2, 10], ti: [0, 0, 0], ti + 0.16: [0, 0, -1]})
        hits.append(ti)
    T = hits[-1]
    for e in extra:
        EXTRA[e](fx, s, k, P, T, slot + e)
    return {"hit": hits[0], "end": T + 0.3}


def x_ring(fx, s, k, P, t, slot):
    b = fx.spr(s, slot, G(sh_ring, "NORMAL" if P in ("NORMAL",) else P), 18, 18, "at_target", (0, 0, 4))
    fx.show(s, b, t, t + 0.3, k * 0.8, k * 1.9)


def x_flames(fx, s, k, P, t, slot):
    for i in range(3):
        b = fx.spr(s, f"{slot}{i}", G(sh_flame, "FIRE", seed=i * 1.7), 6, 9, "at_target", (rnd(s, slot, i) * 6 * k, -4, 2))
        fx.show(s, b, t + i * 0.04, t + 0.45 + i * 0.04, k, k * 1.2)
        fx.pos(s, b, {t: [0, 0, 0], t + 0.45: [0, 6 * k, 0]})


def x_ice(fx, s, k, P, t, slot):
    for i in range(4):
        b = fx.spr(s, f"{slot}{i}", G(sh_crystal, "ICE"), 5, 8, "at_target", (0, 0, 2))
        a = i * 90 + 45
        fx.show(s, b, t, t + 0.4, k)
        fx.pos(s, b, {t: [0, 0, 0], t + 0.4: [math.cos(math.radians(a)) * 9 * k, math.sin(math.radians(a)) * 9 * k, 0]})
        fx.rot(s, b, {t: [0, 0, a - 90], t + 0.4: [0, 0, a - 90]})


def x_elec(fx, s, k, P, t, slot):
    for i in range(3):
        b = fx.spr(s, f"{slot}{i}", G(sh_bolt, "ELECTRIC", "vert", seed=i), 6, 14, "at_target", (rnd(s, slot, i) * 6, -7, 2))
        fx.show(s, b, t + i * 0.07, t + 0.14 + i * 0.07, k)


def x_stars(fx, s, k, P, t, slot):
    g = fx.group(s, slot, "at_target", (0, 8, 0))
    for i in range(3):
        a = math.radians(i * 120)
        fx.spr(s, f"{slot}{i}", G(sh_spark, "STAR"), 7, 7, slot_parent(s, slot), (math.sin(a) * 7, 0, math.cos(a) * 7))
    fx.show(s, g, t, t + 0.8, k)
    fx.rot(s, g, {t: [0, 0, 0], t + 0.8: [0, 400, 0]})


def slot_parent(s, slot):
    return f"s{s}_{slot}"


def x_dust(fx, s, k, P, t, slot):
    for i in range(4):
        b = fx.spr(s, f"{slot}{i}", G(sh_puff, "SMOKE"), 9, 9, "at_tground", ((i - 1.5) * 6 * k, 3, 2))
        fx.show(s, b, t + i * 0.03, t + 0.5, k * 0.7, k * 1.3)
        fx.pos(s, b, {t: [0, 0, 0], t + 0.5: [(i - 1.5) * 4 * k, 4 * k, 0]})


def x_rocks(fx, s, k, P, t, slot):
    for i in range(4):
        b = fx.spr(s, f"{slot}{i}", rock_mat(), 3, 3, "at_target", (0, -2, 0), kind="box")
        fx.show(s, b, t, t + 0.5, k)
        fx.pos(s, b, {t: [0, 0, 0], t + 0.25: [rnd(s, slot, i) * 10 * k, 8 * k, rnd(s, slot, i, 1) * 6], t + 0.5: [rnd(s, slot, i) * 14 * k, -2, rnd(s, slot, i, 1) * 8]})
        fx.rot(s, b, {t: [0, 0, 0], t + 0.5: [300, 200, 0]})


def x_speed(fx, s, k, P, t, slot):
    for i in range(4):
        b = fx.spr(s, f"{slot}{i}", G(sh_streak, "NORMAL", "long"), 2, 18, "at_body", (rnd(s, slot, i) * 4, rnd(s, slot, i, 1) * 4, 22), kind="long")
        fx.show(s, b, t - 0.25, t, k)


def x_recoil(fx, s, k, P, t, slot):
    b = fx.spr(s, slot, G(sh_pow, "NORMAL"), 10, 10, "root", (0, 0, 2))
    fx.show(s, b, t + 0.05, t + 0.22, k * 0.8, k * 1.1)


def x_hearts(fx, s, k, P, t, slot):
    g, W, Hh = glyph_shape("HEART")
    for i in range(3):
        b = fx.spr(s, f"{slot}{i}", G(g, "HEART"), W, Hh, "at_target", ((i - 1) * 7, 6, 2))
        fx.show(s, b, t + i * 0.08, t + 0.7, k * 0.8)
        fx.pos(s, b, {t: [0, 0, 0], t + 0.7: [0, 8, 0]})


def x_anger(fx, s, k, P, t, slot):
    g, W, Hh = glyph_shape("ANGER")
    b = fx.spr(s, slot, G(g, "ANGER"), W, Hh, "root", (5, 9, 6))
    fx.show(s, b, t - 0.25, t + 0.2, k)


def x_splash(fx, s, k, P, t, slot):
    for i in range(5):
        b = fx.spr(s, f"{slot}{i}", G(sh_drop, "WATER"), 4, 6, "at_target", (0, 0, 3))
        a = math.radians(-60 + i * 30)
        fx.show(s, b, t, t + 0.45, k)
        fx.pos(s, b, {t: [0, 0, 0], t + 0.2: [math.sin(a) * 9 * k, 8 * k, 2], t + 0.45: [math.sin(a) * 13 * k, -4, 3]})


EXTRA = {"ring": x_ring, "flames": x_flames, "ice": x_ice, "elec": x_elec, "stars": x_stars, "dust": x_dust, "rocks": x_rocks,
         "speed": x_speed, "recoil": x_recoil, "hearts": x_hearts, "anger": x_anger, "splash": x_splash}


def p_fangs(fx, s, k, n, P, big=1.0, burst=True, t=None, gpal="NORMAL"):
    """Great fangs flashing in on the target and clamping shut."""
    t = L0 if t is None else t
    for i in range(n):
        ti = t + i * 0.22
        up = fx.spr(s, f"fu{i}", G(sh_fang_d, gpal), 12, 9, "at_target", (0, 6, 5))
        lo = fx.spr(s, f"fl{i}", G(sh_fang_u, gpal), 12, 9, "at_target", (0, -6, 5))
        for b, d in ((up, 1), (lo, -1)):
            fx.show(s, b, ti - 0.12, ti + 0.14, k * big)
            fx.pos(s, b, {ti - 0.12: [0, 4 * d * k, 0], ti: [0, -2 * d * k, 0], ti + 0.14: [0, -2.5 * d * k, 0]})
    if burst:
        p_hit(fx, s, k, n, P, fx_shape="pow", big=0.7 * big, dt=0.22, t=t + 0.02, slot="fb")
    return {"hit": t, "end": t + n * 0.22 + 0.2}


def p_pincer(fx, s, k, n, P, big=1.0, t=None):
    t = L0 if t is None else t
    for d in (1, -1):
        b = fx.spr(s, f"pin{d}", G(sh_slash, P), 12, 14, "at_target", (8 * d, 0, 4), rot=[0, 0, 135 if d > 0 else -45])
        fx.show(s, b, t - 0.12, t + 0.16, k * big)
        fx.pos(s, b, {t - 0.12: [6 * d * k, 0, 0], t: [-4 * d * k, 0, 0]})
    p_hit(fx, s, k, 1, P, big=big * 0.8, t=t + 0.02, slot="pb")
    return {"hit": t, "end": t + 0.4}


def p_drill(fx, s, k, n, P, big=1.0, t=None):
    """A spinning drill of wind boring into the target."""
    t = L0 if t is None else t
    b = fx.spr(s, "drill", G(sh_drill, P, "long"), 10, 22, "at_target", (0, 0, 24), kind="long")
    fx.show(s, b, t - 0.18, t + 0.3, k * big)
    fx.pos(s, b, {t - 0.18: [0, 0, 6], t: [0, 0, -2], t + 0.3: [0, 0, -4]})
    fx.rot(s, b, {t - 0.18: [0, 0, 0], t + 0.3: [0, 0, 900]})
    p_hit(fx, s, k, max(1, n), P, fx_shape="spark", big=big, dt=0.1, t=t + 0.02, slot="dh")
    return {"hit": t, "end": t + 0.5}


def p_coil(fx, s, k, n, P, t=None, palname=None):
    """Bind, Wrap: coils wrapping round the target and squeezing."""
    t = L0 if t is None else t
    for i in range(3):
        b = fx.spr(s, f"coil{i}", G(sh_coil, palname or P), 18, 18, "at_target", (0, (i - 1) * 5, 0), rot=[90, 0, 0])
        fx.show(s, b, t + i * 0.06, t + 0.8, [k * 1.4, k * 1.4, k * 1.4], [k * 0.8, k * 0.8, k * 0.8])
        fx.rot(s, b, {t: [0, 0, 0], t + 0.8: [0, 0, 120 * (1 if i % 2 else -1)]})
    p_hit(fx, s, k, n, P, fx_shape="pow", big=0.6, dt=0.2, t=t + 0.3, slot="ch")
    return {"hit": t + 0.3, "end": t + 0.9}


def p_shoot(fx, s, k, n, P, shape="orb", size=8, frm="root", travel=0.42, gap=0.1, arc=0.0, spin=(0, 0, 0), burst="pow",
            spread=4.0, rise=0.0, ret=False, kind="bill", grow=1.0, t=None, slot="sh", solid=False, orbit=0.0, glyph=None, gpal=None):
    """Things flying from the user to the target (embers, leaves, needles, rocks...), each landing with a burst."""
    t = L0 if t is None else t
    mats = {"orb": lambda: G(sh_orb, P), "flame": lambda: G(sh_flame, P), "glow": lambda: G(sh_glow, P), "spark": lambda: G(sh_spark, P),
            "needle": lambda: G(sh_needle, P, "long"), "leaf": lambda: leaf_mat(P), "rock": lambda: rock_mat(P),
            "bubble": lambda: G(sh_bubble, P), "drop": lambda: G(sh_drop, P), "coin": lambda: S(sh_coin, "GOLD"),
            "star": lambda: G(sh_spark, "STAR"), "puff": lambda: G(sh_puff, P), "petal": lambda: S(sh_petal, "PETAL"),
            "flake": lambda: G(sh_flake, P), "ring": lambda: G(sh_ring, P), "wind": lambda: G(sh_wind, P),
            "slash": lambda: G(sh_slash, P), "dots": lambda: G(sh_dots, P), "rings": lambda: G(sh_rings, P),
            "jag": lambda: G(sh_jag, P), "crystal": lambda: G(sh_crystal, P), "spiral": lambda: G(sh_spiral, P)}
    if glyph:
        g, W, Hh = glyph_shape(glyph)
        mat, w, h = (S if solid else G)(g, gpal or P), W, Hh
    else:
        mat = mats[shape]()
        w = h = size
        if kind == "long":
            w, h = max(2, size // 3), size
    if shape == "leaf":
        kind, w, h = "leaf", max(3, round(size * 0.5)), size
    if shape == "rock":
        kind, w, h = "box", size, size
    start = fx.pv[frm]
    to = [0 - start[0], 0 - start[1], -D - start[2] + 2]
    last = t
    for i in range(n):
        ta = t + gap * i
        dx, dy = rnd(s, slot, i) * spread * k, rnd(s, slot, i, 1) * spread * 0.7 * k
        b = fx.spr(s, f"{slot}{i}", mat, w, h, frm, (0, 0, 0), kind=kind)
        pts = {ta: [0, 0, 0]}
        t1 = ta
        if orbit:
            ang = i / n * 2 * math.pi
            pts[ta + 0.12] = [math.sin(ang) * orbit * k, 4 + math.cos(ang) * 3, math.cos(ang) * orbit * k]
            pts[ta + 0.36] = [math.sin(ang + 2) * orbit * k, 6, math.cos(ang + 2) * orbit * k]
            t1 = ta + 0.36
        if rise:
            pts[ta + 0.14] = [dx * 0.5, rise * k, 2]
            t1 = ta + 0.14
        tb = t1 + travel
        if arc:
            pts[(t1 + tb) / 2] = [to[0] / 2 + dx / 2, to[1] / 2 + arc * k + dy / 2, to[2] / 2]
        pts[tb] = [to[0] + dx, to[1] + dy, to[2]]
        end = tb
        if ret:
            pts[tb + travel] = [0, 0, 0]
            end = tb + travel
        fx.show(s, b, ta, end, k, k * grow)
        fx.pos(s, b, pts)
        if any(spin):
            fx.rot(s, b, {ta: [0, 0, 0], end: list(spin)})
        if burst:
            p_hit(fx, s, k, 1, P, fx_shape=burst if burst in ("pow", "spark", "ring") else "pow", big=0.6 if n > 3 else 0.8,
                  t=tb, slot=f"{slot}b{i}", extra=("splash",) if burst == "splash" else ())
        last = max(last, tb)
    return {"hit": t + travel + (0.5 if orbit else 0) + (0.14 if rise else 0), "end": last + 0.3}


def p_beam(fx, s, k, n, P, shape="beam", w=8, hold=0.5, sweep=0.0, t=None, hits="pow", slot="bm", charge=False, width_k=None):
    """A beam (or a jet, a stream of flame, a bolt) reaching from the mouth to the target and held there."""
    t = L0 if t is None else t
    if charge:
        p_charge(fx, s, k, P, t, slot + "c")
        t += 0.55
    shp = {"beam": sh_beam, "aurora": sh_aurora, "cone": sh_cone, "torrent": sh_torrent, "jet": sh_jet, "bolt": sh_bolt,
           "thread": sh_thread, "vine": sh_vine, "tongue": sh_tongue, "streak": sh_streak}[shape]
    t1 = t + hold
    wk = k if width_k is None else width_k
    for i in range(n):
        o = {"seed": i} if shape == "bolt" else {}
        b = fx.spr(s, f"{slot}{i}", G(shp, P, "long", **o), w, D + 2, "root", (0, 0, 0), kind="long")
        if shape == "bolt":
            # Bolts crackle (their frames flicker), a little apart.
            fx.show(s, b, t + i * 0.05, t1, [wk, wk, 1])
            fx.rot(s, b, {t: [rnd(s, i) * 4, rnd(s, i, 1) * 4, i * 60], t1: [rnd(s, i, 2) * 4, rnd(s, i, 3) * 4, i * 60 + 30]})
        else:
            fx.key(s, b, "scale", {0: H_, t - 0.01: H_, t: [wk * 0.6, wk * 0.6, 0.05], t + 0.12: [wk, wk, 1], t1: [wk, wk, 1],
                                   t1 + 0.1: [wk * 0.2, wk * 0.2, 1], t1 + 0.11: H_})
            if sweep:
                fx.rot(s, b, {t: [0, -sweep, 0], t1: [0, sweep, 0]})
            elif n > 1:
                fx.rot(s, b, {t: [0, 0, i * 90 / n], t1: [0, 0, i * 90 / n + 90]})
    if hits:
        m = max(1, int(hold / 0.22))
        for j in range(m):
            tj = t + 0.12 + j * 0.22
            if hits == "flames":
                x_flames(fx, s, k, P, tj, f"{slot}f{j}")
            elif hits == "splash":
                x_splash(fx, s, k, P, tj, f"{slot}s{j}")
            elif hits == "ice":
                x_ice(fx, s, k, P, tj, f"{slot}i{j}")
            else:
                p_hit(fx, s, k, 1, P, fx_shape=hits, big=0.7, t=tj, slot=f"{slot}h{j}")
    return {"hit": t + 0.12, "end": t1 + 0.3, "hold": max(0.0, hold - 0.5)}


def p_charge(fx, s, k, P, t, slot="cg"):
    """Gathering power: motes of light drawn in to the mouth."""
    for i in range(6):
        a = math.radians(i * 60)
        b = fx.spr(s, f"{slot}{i}", G(sh_glow, P), 5, 5, "root", (math.sin(a) * 12, math.cos(a) * 12, -2))
        fx.show(s, b, t + i * 0.04, t + 0.5, k * 0.8, k * 0.3)
        fx.pos(s, b, {t: [0, 0, 0], t + 0.5: [-math.sin(a) * 12, -math.cos(a) * 12, 0]})
    g = fx.spr(s, f"{slot}g", G(sh_glow, P), 10, 10, "root", (0, 0, -2))
    fx.show(s, g, t + 0.1, t + 0.55, k * 0.4, k * 1.3)


def p_wave(fx, s, k, n, P, t=None):
    """Surf: a great wave rising up in front of the user and rolling over the target."""
    t = L0 if t is None else t
    b = fx.spr(s, "wave", G(sh_wave, P), 40, 22, "at_ground", (0, 11, -4), planes=1)
    fx.show(s, b, t, t + 0.9, [k * 0.8, k * 0.3, k], [k * 1.2, k * 1.15, k])
    fx.pos(s, b, {t: [0, 0, 0], t + 0.3: [0, 2, -16], t + 0.9: [0, 0, -D - 8]})
    x_splash(fx, s, k, P, t + 0.6, "wvs")
    x_splash(fx, s, k, P, t + 0.75, "wvs2")
    return {"hit": t + 0.6, "end": t + 1.1, "hold": 0.3}


def p_cloud(fx, s, k, n, P, shape="powder", size=12, t=None, linger=0.7, glyph=None, gpal=None, slot="cl"):
    """Powder, gas or smoke drifting from the user to settle over the target."""
    t = L0 if t is None else t
    shp = {"powder": sh_powder, "puff": sh_puff, "sand": sh_powder}[shape]
    for i in range(n):
        ta = t + i * 0.07
        b = fx.spr(s, f"{slot}{i}", G(shp, P), size, size, "root", (0, 0, -2))
        end = [rnd(s, slot, i) * 8 * k, rnd(s, slot, i, 1) * 5 * k + 2, -D + 2 + rnd(s, slot, i, 2) * 4]
        fx.show(s, b, ta, ta + 0.45 + linger, k * 0.5, k * 1.3)
        fx.pos(s, b, {ta: [0, 0, 0], ta + 0.45: end, ta + 0.45 + linger: [end[0] * 1.3, end[1] - 3, end[2]]})
        fx.rot(s, b, {ta: [0, 0, 0], ta + 0.45 + linger: [0, 0, 90 * (1 if i % 2 else -1)]})
    if glyph:
        p_glyph(fx, s, k, glyph, gpal or P, "at_target", t + 0.5, 0.7, (0, 10, 0))
    return {"hit": t + 0.45, "end": t + 0.5 + linger + n * 0.07}


def p_sound(fx, s, k, n, P, shape="rings", t=None, glyph=None, size=14):
    """Sound rolling out towards the target: rings (Growl), jagged (Screech), dots (Supersonic) or notes (Sing)."""
    t = L0 if t is None else t
    for i in range(n):
        ta = t + i * 0.13
        if shape == "notes":
            g, W, Hh = glyph_shape("NOTE")
            b = fx.spr(s, f"snd{i}", G(g, P), W, Hh, "root", (0, 2, -2))
            fx.show(s, b, ta, ta + 0.7, k)
            fx.pos(s, b, {ta: [0, 0, 0], ta + 0.35: [rnd(s, i) * 6, 5 + 3 * math.sin(i), -D / 2], ta + 0.7: [rnd(s, i, 1) * 8, 3, -D + 4]})
        else:
            shp = {"rings": sh_rings, "jag": sh_jag, "dots": sh_dots}[shape]
            b = fx.spr(s, f"snd{i}", G(shp, P), size, size, "root", (0, 0, -2), planes=1)
            fx.show(s, b, ta, ta + 0.55, k * 0.5, k * 1.6)
            fx.pos(s, b, {ta: [0, 0, 0], ta + 0.55: [0, 0, -D + 6]})
    if glyph:
        p_glyph(fx, s, k, glyph, "SLEEP" if glyph == "Z" else P, "at_target", t + 0.6, 0.6, (0, 10, 0))
    return {"hit": t + 0.55, "end": t + n * 0.13 + 0.7}


def p_glyph(fx, s, k, name, palname, at, t, dur, off=(0, 8, 0), flip=False, bob=3.0, slot=None, size=1.0):
    g, W, Hh = glyph_shape("UP" if name == "DOWN" else name, flip=(name == "DOWN") or flip)
    slot = slot or f"gl{name}{at}{int(t * 100)}"
    b = fx.spr(s, slot, G(g, palname), W, Hh, at, off)
    fx.show(s, b, t, t + dur, k * size)
    fx.pos(s, b, {t: [0, 0, 0], t + dur: [0, bob * (-1 if name == "DOWN" else 1), 0]})
    return b


def p_arrows(fx, s, k, n, P, up=True, at=None, t=None, palname=None):
    """A stat going up (arrows rising round the user) or down (sinking round the target)."""
    t = L0 if t is None else t
    at = at or ("at_body" if up else "at_target")
    for i in range(3):
        g, W, Hh = glyph_shape("UP", flip=not up)
        b = fx.spr(s, f"ar{int(up)}{i}{at}", G(g, palname or P), W, Hh, at, ((i - 1) * 7, (-4 if up else 6) + (i % 2) * 3, 0))
        ti = t + i * 0.1
        fx.show(s, b, ti, ti + 0.6, k * 0.9)
        fx.pos(s, b, {ti: [0, 0, 0], ti + 0.6: [0, 9 if up else -8, 0]})
    return {"hit": t, "end": t + 0.9}


def p_at(fx, s, k, n, P, kind="rings", t=None, dur=0.9, size=1.0):
    """Something happening at the target."""
    t = L0 if t is None else t
    T1 = t + dur
    if kind == "rings":          # Confusion: rings pulsing round it
        for i in range(3):
            b = fx.spr(s, f"pr{i}", G(sh_ring, P, dash=0), 16, 16, "at_target", (0, 0, 0), rot=[0, i * 60, 0])
            fx.show(s, b, t + i * 0.12, T1, k * 0.6 * size, k * 1.6 * size)
    elif kind == "waves":        # Psychic: waves of power squeezing it
        for i in range(4):
            b = fx.spr(s, f"pw{i}", G(sh_rings, P), 22, 22, "at_target", (0, 0, 0), rot=[0, i * 45, 0])
            fx.show(s, b, t + i * 0.1, T1, k * 1.4, k * 0.6)
            fx.rot(s, b, {t: [0, i * 45, 0], T1: [0, i * 45, 180]})
    elif kind == "spark_ring":   # Thunder Wave: a ring of sparks closing in
        g = fx.group(s, "twg", "at_target")
        for i in range(6):
            a = math.radians(i * 60)
            fx.spr(s, f"tw{i}", G(sh_bolt, "ELECTRIC", "vert", seed=i), 5, 12, "s%d_twg" % s, (math.sin(a) * 10, -6, math.cos(a) * 10))
        fx.show(s, g, t, T1, k * 1.3, k * 0.7)
        fx.rot(s, g, {t: [0, 0, 0], T1: [0, 240, 0]})
    elif kind == "vortex":       # Fire Spin, Whirlpool, Dragon Rage: a ring of fire (or water) spinning round it
        g = fx.group(s, "vxg", "at_tground", (0, 0, 0))
        mat = G(sh_flame, P) if P in ("FIRE", "DRAGON") else G(sh_tornado, P)
        for i in range(8):
            a = math.radians(i * 45)
            fx.spr(s, f"vx{i}", mat, 7, 14, "s%d_vxg" % s, (math.sin(a) * 10, 7, math.cos(a) * 10), rot=[0, i * 45, 0])
        fx.show(s, g, t, T1, k * 0.6, k * 1.2)
        fx.rot(s, g, {t: [0, 0, 0], T1: [0, 540, 0]})
    elif kind == "storm":        # Blizzard: driving snow and wind round it
        for i in range(8):
            b = fx.spr(s, f"bz{i}", G(sh_flake, "ICE"), 6, 6, "at_target", (rnd(s, i) * 14, 14 + rnd(s, i, 1) * 6, rnd(s, i, 2) * 8 + 12))
            ti = t + i * 0.08
            fx.show(s, b, ti, ti + 0.6, k)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.6: [rnd(s, i, 3) * 4, -22, -18]})
            fx.rot(s, b, {ti: [0, 0, 0], ti + 0.6: [0, 0, 240]})
        for i in range(2):
            b = fx.spr(s, f"bzw{i}", G(sh_wind, "ICE"), 22, 22, "at_target", (0, 0, 0), rot=[0, 90 * i, 0])
            fx.show(s, b, t + 0.2 + i * 0.15, T1, k, k * 1.5)
            fx.rot(s, b, {t: [0, 90 * i, 0], T1: [0, 90 * i, 360]})
    elif kind == "sky_bolt":     # Thunder: a great bolt down out of the sky
        for i in range(2):
            b = fx.spr(s, f"sb{i}", G(sh_bolt, "ELECTRIC", "vert", seed=i + 5, w=.12), 14, 48, "at_tground", (rnd(s, i) * 3, 0, 0))
            fx.show(s, b, t + i * 0.1, t + 0.35 + i * 0.1, [k, 1, k])
        p_hit(fx, s, k, 2, "ELECTRIC", fx_shape="pow", big=1.2, dt=0.15, t=t + 0.05, slot="sbh")
        b = fx.spr(s, "sbg", G(sh_glow, "ELECTRIC"), 30, 30, "at_target", (0, 4, 0))
        fx.show(s, b, t, t + 0.4, k, k * 1.4)
    elif kind == "fire_star":    # Fire Blast: the great five-armed flame
        for i in range(5):
            a = math.radians(i * 72)
            b = fx.spr(s, f"fs{i}", G(sh_flame, "FIRE", seed=i), 9, 16, "at_target", (0, 0, 2), rot=[0, 0, -i * 72])
            fx.show(s, b, t, T1, k * 0.5, k * 1.2)
            fx.pos(s, b, {t: [0, 0, 0], t + 0.25: [math.sin(a) * 8 * k, math.cos(a) * 8 * k, 0], T1: [math.sin(a) * 10 * k, math.cos(a) * 10 * k, 0]})
        x_flames(fx, s, k, P, t + 0.2, "fsf")
    elif kind == "rocks_fall":   # Rock Slide: rocks tumbling down on it
        for i in range(n):
            b = fx.spr(s, f"rf{i}", rock_mat(), 5 + i % 3, 5 + i % 3, "at_tground", (rnd(s, i) * 10, 36, rnd(s, i, 1) * 8), kind="box")
            ti = t + i * 0.1
            fx.show(s, b, ti, ti + 0.45, k)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.38: [0, -32, 0], ti + 0.45: [rnd(s, i, 2) * 4, -30, 0]})
            fx.rot(s, b, {ti: [0, 0, 0], ti + 0.45: [200, 90, 0]})
            x_dust(fx, s, k * 0.7, P, ti + 0.38, f"rfd{i}")
    elif kind == "cracks":       # Earthquake, Magnitude: the ground cracking open, dust thrown up
        for i in range(3):
            b = fx.spr(s, f"ck{i}", G(sh_crack, "GROUND", "flat"), 12, 34, "at_ground", ((i - 1) * 10, 0.2, -D / 2), rot=[0, (i - 1) * 25, 0])
            fx.show(s, b, t + i * 0.08, T1, [k, 1, 0.3], [k, 1, 1])
        for j in range(3):
            x_dust(fx, s, k, P, t + 0.2 + j * 0.2, f"ckd{j}")
    elif kind == "fissure":      # Fissure: one great crack racing to the target
        b = fx.spr(s, "fiss", G(sh_crack, "GROUND", "flat"), 16, D + 6, "at_ground", (0, 0.2, -D / 2 - 2))
        fx.show(s, b, t, T1 + 0.3, [k * 1.4, 1, 0.1], [k * 1.6, 1, 1])
        x_dust(fx, s, k * 1.3, P, t + 0.35, "fd1")
        x_dust(fx, s, k * 1.5, P, t + 0.55, "fd2")
    elif kind == "spikes":       # Spikes: spikes scattered on the ground round it
        for i in range(n):
            b = fx.spr(s, f"sp{i}", G(sh_crystal, "STEEL"), 5, 7, "at_tground", (rnd(s, i) * 14, 3, rnd(s, i, 1) * 10 + 10))
            ti = t + i * 0.06
            fx.show(s, b, ti, T1 + 0.4, k)
            fx.pos(s, b, {ti: [0, 20, 18], ti + 0.3: [0, 0, 0]})
    elif kind == "web":
        b = fx.spr(s, "web", G(sh_web, "NORMAL"), 22, 22, "at_target", (0, 0, 6), planes=1)
        fx.show(s, b, t, T1, k * 0.3, k * 1.2)
    elif kind == "bubbles":      # Toxic: poison bubbling up round it
        for i in range(n):
            b = fx.spr(s, f"tx{i}", G(sh_bubble, P), 5, 5, "at_tground", (rnd(s, i) * 9, 2, rnd(s, i, 1) * 6))
            ti = t + i * 0.08
            fx.show(s, b, ti, ti + 0.6, k * 0.7, k * 1.2)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.6: [rnd(s, i, 2) * 3, 16, 0]})
    elif kind == "wisps":        # Nightmare, Spite, Destiny Bond: ghostly wisps
        for i in range(n):
            b = fx.spr(s, f"ws{i}", G(sh_wisp, P), 7, 12, "at_target", (rnd(s, i) * 9, rnd(s, i, 1) * 4, rnd(s, i, 2) * 6))
            ti = t + i * 0.1
            fx.show(s, b, ti, ti + 0.7, k)
            fx.pos(s, b, {ti: [0, -4, 0], ti + 0.7: [rnd(s, i, 3) * 4, 8, 0]})
    elif kind == "dig_out":      # Dig: bursting up out of the ground under it
        x_dust(fx, s, k * 1.3, P, t, "dgd")
        x_rocks(fx, s, k, P, t, "dgr")
        p_hit(fx, s, k, 1, P, big=1.1, t=t + 0.05, slot="dgh")
    elif kind == "lift":         # its power lifting the target (Psychic, Seismic Toss)
        for i in range(2):
            b = fx.spr(s, f"lf{i}", G(sh_ring, P, dash=0), 20, 20, "at_tground", (0, 2 + i * 6, 0), kind="flat")
            fx.show(s, b, t + i * 0.1, T1, k * 0.5, k * 1.4)
    return {"hit": t + 0.1, "end": T1 + 0.2}


def p_self(fx, s, k, n, P, kind="sparkles", t=None, dur=0.9, pre=""):
    """Something happening at the user: sparkles, a shield, a wall, swords, Zs..."""
    t = L0 if t is None else t
    T1 = t + dur
    if kind in ("sparkles", "heal"):
        palname = "HEAL" if kind == "heal" else P
        for i in range(n):
            a = math.radians(i * 360 / n)
            b = fx.spr(s, f"{pre}sk{i}", G(sh_spark, palname), 6, 6, "at_body", (math.sin(a) * 10, -6 + (i % 3) * 3, math.cos(a) * 8))
            ti = t + i * 0.07
            fx.show(s, b, ti, ti + 0.6, k * 0.8, k * 0.3)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.6: [0, 12, 0]})
        if kind == "heal":
            g, W, Hh = glyph_shape("CROSS")
            b = fx.spr(s, f"{pre}hcross", G(g, "HEAL"), W, Hh, "at_body", (0, 12, 0))
            fx.show(s, b, t + 0.2, T1, k)
            fx.pos(s, b, {t + 0.2: [0, 0, 0], T1: [0, 4, 0]})
    elif kind == "shield":       # Protect, Detect, Safeguard: a bubble round it
        for i, r in enumerate(([0, 0, 0], [0, 90, 0], [90, 0, 0])):
            b = fx.spr(s, f"sh{i}", G(sh_disc, P), 30, 30, "at_body", (0, 2, 0), planes=1, rot=r)
            fx.show(s, b, t, T1, k * 0.6, k)
    elif kind == "wall":         # Reflect, Light Screen, Barrier: a wall of light in front of it
        b = fx.spr(s, "wall", G(sh_wall, P), 26, 20, "root", (0, 2, -8), planes=1)
        fx.show(s, b, t, T1, [k, 0.05, k], [k, k, k])
        fx.key(s, b, "scale", {t + 0.15: [k, k, k]})
    elif kind == "swords":       # Swords Dance: swords whirling round it
        g = fx.group(s, "swg", "at_body", (0, 6, 0))
        gs, W, Hh = glyph_shape("SWORD")
        for i in range(n):
            a = math.radians(i * 360 / n)
            fx.spr(s, f"sw{i}", G(gs, "STEEL"), W, Hh, "s%d_swg" % s, (math.sin(a) * 11, 0, math.cos(a) * 11), rot=[0, i * 360 / n + 90, 0])
        fx.show(s, g, t, T1, k)
        fx.rot(s, g, {t: [0, 0, 0], T1: [0, 720, 0]})
    elif kind == "zs":           # Rest, Sleep Talk: Zs drifting up
        for i in range(3):
            p_glyph(fx, s, k * (0.7 + i * 0.2), "Z", "SLEEP", "root", t + i * 0.25, 0.7, (4 + i * 3, 6 + i * 3, 4), slot=f"z{i}")
    elif kind == "glints":       # Harden, Sharpen, Withdraw: hard glints flashing over it
        for i in range(n):
            b = fx.spr(s, f"gt{i}", G(sh_spark, P), 9, 9, "at_body", (rnd(s, i) * 9, rnd(s, i, 1) * 6 + 2, rnd(s, i, 2) * 8))
            ti = t + i * 0.12
            fx.show(s, b, ti, ti + 0.2, k, k * 1.4)
    elif kind == "aura_rings":   # Meditate, Psych Up: rings of calm rising round it
        for i in range(3):
            b = fx.spr(s, f"mr{i}", G(sh_ring, P, dash=0), 22, 22, "at_ground", (0, 1, 8), kind="flat")
            ti = t + i * 0.2
            fx.show(s, b, ti, ti + 0.7, k * 0.8, k * 1.1)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.7: [0, 16, 0]})
    elif kind == "speed":        # Agility: speed lines streaming past
        for i in range(n):
            b = fx.spr(s, f"ag{i}", G(sh_streak, "NORMAL", "long"), 2, 20, "at_body", (rnd(s, i) * 9, rnd(s, i, 1) * 6, 18), kind="long")
            ti = t + i * 0.06
            fx.show(s, b, ti, ti + 0.25, k)
            fx.pos(s, b, {ti: [0, 0, -10], ti + 0.25: [0, 0, 6]})
    elif kind == "flames_rise":  # Focus Energy: flames of effort rising off it
        for i in range(n):
            b = fx.spr(s, f"fe{i}", G(sh_flame, P, seed=i), 6, 10, "at_ground", (rnd(s, i) * 9, 0, rnd(s, i, 1) * 7 + 8))
            ti = t + i * 0.09
            fx.show(s, b, ti, ti + 0.5, k, k * 0.6)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.5: [0, 14, 0]})
    elif kind == "burst":        # Selfdestruct, Explosion: a great blast at the user
        for i in range(n):
            b = fx.spr(s, f"ex{i}", G(sh_pow, "FIRE" if i % 2 == 0 else "NORMAL"), 26, 26, "at_body",
                       (rnd(s, i) * 10, rnd(s, i, 1) * 7 + 2, rnd(s, i, 2) * 8))
            ti = t + i * 0.07
            fx.show(s, b, ti, ti + 0.35, k * 0.8, k * 1.8)
        b = fx.spr(s, "exr", G(sh_ring, "NORMAL"), 30, 30, "at_ground", (0, 1, 8), kind="flat")
        fx.show(s, b, t + 0.1, T1, k, k * 3.0)
        x_dust(fx, s, k * 1.5, P, t + 0.2, "exd")
    elif kind == "flash":        # Flash: a blinding burst of light
        b = fx.spr(s, "fl", G(sh_glow, "NORMAL"), 34, 34, "root", (0, 0, -4))
        fx.show(s, b, t, t + 0.5, k * 0.3, k * 2.2)
        b2 = fx.spr(s, "fl2", G(sh_rays, "STAR"), 40, 40, "root", (0, 0, -4))
        fx.show(s, b2, t + 0.05, t + 0.5, k * 0.6, k * 1.8)
    elif kind == "drops":        # Splash: water flying up as it flops about
        for i in range(n):
            b = fx.spr(s, f"dr{i}", G(sh_drop, "WATER"), 4, 6, "at_ground", (rnd(s, i) * 6, 2, rnd(s, i, 1) * 5 + 8))
            ti = t + (i % 3) * 0.2
            fx.show(s, b, ti, ti + 0.5, k)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.25: [rnd(s, i, 2) * 8, 12, 0], ti + 0.5: [rnd(s, i, 2) * 12, 0, 0]})
    elif kind == "doll":         # Substitute: a decoy doll popping up in front of it
        g, W, Hh = glyph_shape("DOLL")
        b = fx.spr(s, "doll", S(g, "WOOD"), W, Hh, "at_ground", (0, 6, -10))
        fx.show(s, b, t, T1 + 0.5, k * 1.3)
        fx.pos(s, b, {t: [0, 8, 0], t + 0.25: [0, 0, 0]})
        x_dust(fx, s, k * 0.6, P, t, "dld")
    elif kind == "shimmer":      # Double Team, Transform, Conversion: a shimmer of light over it
        for i in range(n):
            b = fx.spr(s, f"shm{i}", G(sh_glow, P), 10, 10, "at_body", (rnd(s, i) * 12, rnd(s, i, 1) * 6, rnd(s, i, 2) * 9))
            ti = t + i * 0.08
            fx.show(s, b, ti, ti + 0.35, k * 0.5, k * 1.2)
    elif kind == "sun":          # Morning Sun, Synthesis: the sun over it, its light pouring down
        b = fx.spr(s, "sun", G(sh_sun, "FIRE"), 16, 16, "at_body", (0, 22, 0))
        fx.show(s, b, t, T1, k * 0.5, k)
        b2 = fx.spr(s, "sunr", G(sh_rays, "STAR"), 34, 34, "at_body", (0, 22, 0))
        fx.show(s, b2, t + 0.15, T1, k, k * 1.3)
        p_self(fx, s, k, 8, P, "heal", t + 0.3, dur - 0.3, pre="sun")
    elif kind == "moon":
        b = fx.spr(s, "moon", G(sh_moon, "STAR"), 16, 16, "at_body", (0, 22, 0))
        fx.show(s, b, t, T1, k * 0.5, k)
        p_self(fx, s, k, 8, "SLEEP", "sparkles", t + 0.3, dur - 0.3, pre="mn")
        p_self(fx, s, k, 6, P, "heal", t + 0.4, dur - 0.4, pre="mh")
    elif kind == "bell":         # Heal Bell: a bell ringing, notes ringing out
        g, W, Hh = glyph_shape("BELL")
        b = fx.spr(s, "bell", G(g, "GOLD"), W, Hh, "at_body", (0, 16, 0))
        fx.show(s, b, t, T1, k * 1.2)
        fx.rot(s, b, {t: [0, 0, -20], t + 0.2: [0, 0, 20], t + 0.4: [0, 0, -20], t + 0.6: [0, 0, 20], T1: [0, 0, 0]})
        for i in range(3):
            p_glyph(fx, s, k, "NOTE", "HEAL", "at_body", t + 0.2 + i * 0.15, 0.5, ((i - 1) * 9, 14, 0), slot=f"bn{i}")
    elif kind == "drum":         # Belly Drum: drum beats round it
        for i in range(n):
            b = fx.spr(s, f"bd{i}", G(sh_ring, "ANGER", dash=0), 18, 18, "at_body", (0, 0, 0), planes=1)
            ti = t + i * 0.15
            fx.show(s, b, ti, ti + 0.2, k * 0.6, k * 1.5)
    return {"hit": None, "end": T1 + 0.2}


def p_drain(fx, s, k, n, P, t=None, palname=None):
    """Absorb, Giga Drain, Dream Eater: motes of life drawn from the target back to the user, who glows."""
    t = L0 if t is None else t
    palname = palname or P
    to = fx.nominal("at_target", "root")
    for i in range(n):
        ta = t + 0.15 + i * 0.07
        b = fx.spr(s, f"dn{i}", G(sh_orb, palname), 5, 5, "at_target", (rnd(s, i) * 5, rnd(s, i, 1) * 5, 0))
        fx.show(s, b, ta, ta + 0.5, k * 0.9, k * 0.6)
        fx.pos(s, b, {ta: [0, 0, 0], ta + 0.25: [to[0] / 2 + rnd(s, i, 2) * 8, to[1] / 2 + 6 + rnd(s, i, 3) * 4, to[2] / 2], ta + 0.5: to})
    p_hit(fx, s, k, 1, palname, fx_shape="spark", big=0.8, t=t, slot="dnh")
    p_self(fx, s, k, 6, palname, "heal", t + 0.5 + n * 0.07, 0.5, pre="dr")
    return {"hit": t, "end": t + 1.2 + n * 0.07}


def p_field(fx, s, k, n, P, kind="rain", t=None, dur=1.4):
    """Weather over the whole field: rain, sandstorm, sunshine, haze."""
    t = L0 if t is None else t
    T1 = t + dur
    if kind == "rain":
        for i in range(n):
            b = fx.spr(s, f"rn{i}", G(sh_rain, "WATER"), 3, 14, "at_mid", (rnd(s, i) * 26, 26, rnd(s, i, 1) * 34))
            ti = t + (i % 5) * 0.08
            fx.show(s, b, ti, T1, k)
            fx.pos(s, b, {ti: [0, 0, 0], ti + 0.35: [0, -30, 0], ti + 0.36: [0, 0, 0], ti + 0.71: [0, -30, 0], ti + 0.72: [0, 0, 0], T1: [0, -30, 0]})
    elif kind == "sand":
        for i in range(n):
            b = fx.spr(s, f"sn{i}", G(sh_sand, "GROUND"), 24, 10, "at_mid", (-30, rnd(s, i) * 10, rnd(s, i, 1) * 30), rot=[0, 90 if i % 2 else 0, 0])
            ti = t + (i % 4) * 0.1
            fx.show(s, b, ti, T1, k)
            fx.pos(s, b, {ti: [0, 0, 0], T1: [60, -4, 0]})
    elif kind == "sun":
        b = fx.spr(s, "fsun", G(sh_sun, "FIRE"), 22, 22, "at_mid", (0, 30, 0))
        fx.show(s, b, t, T1, k * 0.4, k * 1.1)
        b2 = fx.spr(s, "frays", G(sh_rays, "STAR"), 60, 60, "at_mid", (0, 30, 0))
        fx.show(s, b2, t + 0.2, T1, k * 0.6, k * 1.2)
        fx.rot(s, b2, {t: [0, 0, 0], T1: [0, 0, 40]})
    elif kind == "haze":
        for i in range(n):
            b = fx.spr(s, f"hz{i}", G(sh_puff, P), 20, 20, "at_mid", (rnd(s, i) * 24, rnd(s, i, 1) * 6, rnd(s, i, 2) * 26))
            ti = t + i * 0.06
            fx.show(s, b, ti, T1, k * 0.4, k * 1.4)
    return {"hit": None, "end": T1 + 0.2}


PARTS = {"hit": p_hit, "fangs": p_fangs, "pincer": p_pincer, "drill": p_drill, "coil": p_coil, "shoot": p_shoot, "beam": p_beam,
         "wave": p_wave, "cloud": p_cloud, "sound": p_sound, "arrows": p_arrows, "at": p_at, "self": p_self, "drain": p_drain,
         "field": p_field}


def glyph_part(fx, s, k, n, P, name="Q", at="at_body", t=None, dur=0.8, off=(0, 14, 0), palname=None, size=1.2):
    t = L0 if t is None else t
    p_glyph(fx, s, k, name, palname or P, at, t, dur, off, size=size, slot=f"gp{name}{at}")
    return {"hit": None, "end": t + dur}


PARTS["glyph"] = glyph_part


# ---------- every move: how the user moves (its body), and the parts of its effect. n: counts at stages 1, 2, 3 ----------

def h(**o): return ("hit", o)
def sh(**o): return ("shoot", o)
def bm(**o): return ("beam", o)
def at(**o): return ("at", o)
def sf(**o): return ("self", o)
def ar(**o): return ("arrows", o)
def sd(**o): return ("sound", o)
def cl(**o): return ("cloud", o)
def gl(**o): return ("glyph", o)


N1, N2, N3, N5 = (1, 1, 1), (2, 2, 3), (2, 3, 4), (2, 3, 5)
A, SP, CR = "attack", "special", "cry"

MOVES = {
    "POUND": (A, [h(glyph="HAND", extra=("ring",))]),
    "KARATE CHOP": (A, [h(fx_shape="slash", big=1.1, extra=("ring",))]),
    "DOUBLESLAP": (A, [h(glyph="HAND", n=(2, 3, 4), gpal="SKIN")]),
    "COMET PUNCH": (A, [h(glyph="FIST", n=(2, 3, 5), big=0.8, gpal="SKIN")]),
    "MEGA PUNCH": (A, [h(glyph="FIST", big=1.4, gpal="SKIN", extra=("ring",))]),
    "PAY DAY": (SP, [sh(shape="coin", size=6, n=(3, 5, 7), arc=10, spin=(0, 720, 0), burst="spark", spread=6)]),
    "FIRE PUNCH": (A, [h(glyph="FIST", big=1.1, gpal="FIRE", extra=("flames",))]),
    "ICE PUNCH": (A, [h(glyph="FIST", big=1.1, gpal="ICE", extra=("ice",))]),
    "THUNDERPUNCH": (A, [h(glyph="FIST", big=1.1, gpal="ELECTRIC", extra=("elec",))]),
    "SCRATCH": (A, [h(fx_shape="claws", big=1.1)]),
    "VICEGRIP": (A, [("pincer", {})]),
    "GUILLOTINE": (A, [("pincer", {"big": 1.5}), h(fx_shape="pow", big=1.6, t=L0 + 0.05, slot="gx", extra=("ring",))]),
    "RAZOR WIND": (SP, [sf(kind="shimmer", n=6, dur=0.5), sh(shape="slash", size=14, n=(2, 3, 4), t=0.8, spin=(0, 0, 720), burst="pow")]),
    "SWORDS DANCE": (SP, [sf(kind="swords", n=(4, 5, 6), dur=1.1), ar(up=True, t=1.0)]),
    "CUT": (A, [h(fx_shape="slash", big=1.3)]),
    "GUST": (SP, [sh(shape="wind", size=16, n=(2, 3, 4), gap=0.14, travel=0.5, spin=(0, 0, 360), grow=1.6, burst=None)]),
    "WING ATTACK": (A, [h(fx_shape="slash", n=(2, 2, 2), big=1.2, dt=0.12)]),
    "WHIRLWIND": (SP, [sh(shape="wind", size=20, n=(3, 4, 5), gap=0.1, travel=0.5, spin=(0, 0, 720), grow=1.8, burst=None)]),
    "FLY": (A, [sf(kind="speed", n=6, dur=0.3, t=0.05), h(fx_shape="pow", big=1.3, extra=("ring", "speed"))]),
    "BIND": (A, [("coil", {})]),
    "SLAM": (A, [h(big=1.5, extra=("ring", "dust"))]),
    "VINE WHIP": (A, [bm(shape="vine", w=4, hold=0.3, n=(2, 2, 2), hits="pow")]),
    "STOMP": (A, [h(glyph="FOOT", big=1.2, gpal="SKIN", extra=("dust",))]),
    "DOUBLE KICK": (A, [h(glyph="FOOT", n=(2, 2, 2), gpal="SKIN")]),
    "MEGA KICK": (A, [h(glyph="FOOT", big=1.5, gpal="SKIN", extra=("ring",))]),
    "JUMP KICK": (A, [h(glyph="FOOT", big=1.2, gpal="SKIN", extra=("speed",))]),
    "ROLLING KICK": (A, [h(glyph="FOOT", big=1.1, gpal="SKIN", extra=("ring", "dust"))]),
    "SAND-ATTACK": (SP, [cl(shape="sand", n=(5, 6, 8), size=10, linger=0.5), ar(up=False, t=0.9)]),
    "HEADBUTT": (A, [h(big=1.2, extra=("stars",))]),
    "HORN ATTACK": (A, [h(fx_shape="spark", big=1.2, extra=("ring",))]),
    "FURY ATTACK": (A, [h(fx_shape="spark", n=(2, 3, 5), dt=0.12)]),
    "HORN DRILL": (A, [("drill", {"big": 1.6, "n": 3}), h(big=1.6, t=L0 + 0.25, slot="hdx", extra=("ring",))]),
    "TACKLE": (A, [h(extra=("ring",))]),
    "BODY SLAM": (A, [h(big=1.5, extra=("ring", "dust"))]),
    "WRAP": (A, [("coil", {})]),
    "TAKE DOWN": (A, [h(big=1.3, extra=("ring", "recoil"))]),
    "THRASH": (A, [h(n=(3, 3, 4), dt=0.14, big=1.1), ("glyph", {"name": "ANGER", "at": "root", "off": (5, 9, 6), "palname": "ANGER", "size": 1.0})]),
    "DOUBLE-EDGE": (A, [h(big=1.6, extra=("ring", "recoil", "dust"))]),
    "TAIL WHIP": (CR, [ar(up=False, t=0.4)]),
    "POISON STING": (SP, [sh(shape="needle", size=12, n=(5, 8, 10), frm="at_back", gap=0.05, rise=6, kind="long", burst="spark", spread=5)]),
    "TWINEEDLE": (SP, [sh(shape="needle", size=12, n=(2, 2, 2), gap=0.12, kind="long", burst="spark")]),
    "PIN MISSILE": (SP, [sh(shape="needle", size=16, n=(3, 4, 5), gap=0.1, kind="long", burst="pow")]),
    "LEER": (CR, [gl(name="EYE", at="root", off=(0, 6, 4), t=0.2, dur=0.5, palname="ANGER"), ar(up=False, t=0.6)]),
    "BITE": (A, [("fangs", {"gpal": "NORMAL"})]),
    "GROWL": (CR, [sd(shape="rings", n=(3, 3, 4)), ar(up=False, t=0.85)]),
    "ROAR": (CR, [sd(shape="rings", n=(4, 5, 6), size=20)]),
    "SING": (CR, [sd(shape="notes", n=(3, 4, 5), glyph="Z")]),
    "SUPERSONIC": (CR, [sd(shape="dots", n=(3, 4, 5))]),
    "SONICBOOM": (SP, [sh(shape="ring", size=16, n=(1, 1, 1), travel=0.35, grow=1.4, burst="pow")]),
    "DISABLE": (CR, [sd(shape="dots", n=(2, 2, 3)), gl(name="X", at="at_target", off=(0, 10, 0), t=0.8, dur=0.6, palname="ANGER")]),
    "ACID": (SP, [sh(shape="drop", size=6, n=(2, 3, 4), arc=12, gap=0.1, burst="spark")]),
    "EMBER": (SP, [sh(shape="flame", size=8, n=(2, 3, 4), gap=0.1, travel=0.36, burst="pow", spread=3, grow=1.2)]),
    "FLAMETHROWER": (SP, [bm(shape="cone", w=12, hold=1.9, sweep=16, hits="flames")]),
    "MIST": (SP, [sf(kind="shimmer", n=10, dur=1.0)]),
    "WATER GUN": (SP, [bm(shape="jet", w=6, hold=0.7, hits="splash")]),
    "HYDRO PUMP": (SP, [bm(shape="torrent", w=15, hold=1.5, hits="splash", n=(1, 1, 1)), bm(shape="jet", w=9, hold=1.5, hits=None, slot="hp2")]),
    "SURF": (SP, [("wave", {})]),
    "ICE BEAM": (SP, [bm(shape="beam", w=7, hold=0.9, hits="ice")]),
    "BLIZZARD": (SP, [sh(shape="flake", size=6, n=(4, 5, 6), gap=0.06, travel=0.4, spin=(0, 0, 360), burst=None, spread=8),
                      at(kind="storm", t=0.55, dur=1.0)]),
    "PSYBEAM": (SP, [sh(shape="ring", size=12, n=(5, 6, 8), gap=0.07, travel=0.45, burst=None, spread=1), at(kind="rings", t=0.75, dur=0.5)]),
    "BUBBLEBEAM": (SP, [sh(shape="bubble", size=6, n=(6, 9, 12), gap=0.06, travel=0.5, burst="splash", spread=5)]),
    "AURORA BEAM": (SP, [bm(shape="aurora", w=8, hold=0.9, hits="spark")]),
    "HYPER BEAM": (SP, [bm(shape="beam", w=14, hold=1.0, charge=True, hits="pow")]),
    "PECK": (A, [h(fx_shape="spark")]),
    "DRILL PECK": (A, [("drill", {"n": 2})]),
    "SUBMISSION": (A, [h(n=(2, 2, 3), big=1.2, dt=0.2, extra=("recoil", "dust"))]),
    "LOW KICK": (A, [h(glyph="FOOT", big=1.0, gpal="SKIN", extra=("dust",))]),
    "COUNTER": (A, [sf(kind="glints", n=2, dur=0.3, t=0.0), h(big=1.3, extra=("ring",))]),
    "SEISMIC TOSS": (A, [at(kind="lift", t=0.2, dur=0.6), h(big=1.3, t=0.8, extra=("dust", "ring"))]),
    "STRENGTH": (A, [h(big=1.5, extra=("rocks", "ring"))]),
    "ABSORB": (SP, [("drain", {"n": (2, 3, 4)})]),
    "MEGA DRAIN": (SP, [("drain", {"n": (3, 5, 6)})]),
    "LEECH SEED": (SP, [sh(shape="orb", size=4, n=(2, 3, 3), arc=14, gap=0.08, burst=None, spread=5),
                        ("drain", {"n": (2, 3, 4), "t": 0.9, "palname": "GRASS"})]),
    "GROWTH": (SP, [sf(kind="sparkles", n=8, dur=0.8), ar(up=True, t=0.5)]),
    "RAZOR LEAF": (SP, [sh(shape="leaf", size=8, n=(3, 7, 10), frm="at_back", gap=0.06, rise=8, travel=0.4, spin=(0, 1080, 0), burst="spark", spread=7)]),
    "SOLARBEAM": (SP, [sf(kind="sun", n=6, dur=0.6, t=0.0), bm(shape="beam", w=14, hold=0.9, charge=True, t=0.45, hits="pow")]),
    "POISONPOWDER": (SP, [cl(shape="powder", n=(4, 5, 6))]),
    "STUN SPORE": (SP, [cl(shape="powder", n=(4, 5, 6)), at(kind="spark_ring", t=1.0, dur=0.4)]),
    "SLEEP POWDER": (SP, [cl(shape="powder", n=(4, 5, 6), glyph="Z", gpal="SLEEP")]),
    "PETAL DANCE": (SP, [sh(shape="petal", size=5, n=(6, 8, 10), orbit=12, gap=0.05, travel=0.45, spin=(0, 720, 360), burst="spark", spread=8)]),
    "STRING SHOT": (SP, [bm(shape="thread", w=6, hold=0.6, n=(2, 2, 3), hits=None), ar(up=False, t=0.9)]),
    "DRAGON RAGE": (SP, [sh(shape="flame", size=14, n=(1, 1, 1), travel=0.5, burst=None, grow=1.3), at(kind="vortex", t=0.8, dur=0.8)]),
    "FIRE SPIN": (SP, [sh(shape="flame", size=8, n=(2, 3, 3), travel=0.35, burst=None), at(kind="vortex", t=0.65, dur=1.1)]),
    "THUNDERSHOCK": (SP, [bm(shape="bolt", w=10, hold=0.55, n=(1, 1, 2), hits="spark")]),
    "THUNDERBOLT": (SP, [bm(shape="bolt", w=16, hold=0.8, n=(2, 3, 3), hits="pow"), at(kind="spark_ring", t=0.6, dur=0.5)]),
    "THUNDER WAVE": (SP, [at(kind="spark_ring", t=0.35, dur=0.8)]),
    "THUNDER": (SP, [at(kind="sky_bolt", t=0.45, dur=0.5)]),
    "ROCK THROW": (SP, [sh(shape="rock", size=5, n=(1, 2, 3), arc=16, gap=0.12, travel=0.45, spin=(360, 180, 0), burst="pow")]),
    "EARTHQUAKE": (SP, [at(kind="cracks", t=0.3, dur=1.0)]),
    "FISSURE": (SP, [at(kind="fissure", t=0.3, dur=0.9)]),
    "DIG": (A, [h(fx_shape=None, n=(1, 1, 1), extra=("dust",), at="at_body", t=0.1, slot="dgi"), at(kind="dig_out", t=0.7, dur=0.4)]),
    "TOXIC": (SP, [sh(shape="drop", size=6, n=(1, 1, 2), arc=10, burst=None), at(kind="bubbles", n=(5, 7, 9), t=0.75, dur=0.7)]),
    "CONFUSION": (SP, [at(kind="rings", t=0.35, dur=0.7)]),
    "PSYCHIC": (SP, [at(kind="waves", t=0.35, dur=0.9), at(kind="lift", t=0.4, dur=0.8)]),
    "HYPNOSIS": (CR, [sh(shape="spiral", size=12, n=(2, 3, 3), gap=0.15, travel=0.6, spin=(0, 0, 720), burst=None, spread=1),
                      gl(name="Z", at="at_target", t=1.0, dur=0.6, palname="SLEEP", off=(0, 10, 0))]),
    "MEDITATE": (SP, [sf(kind="aura_rings", dur=1.0), ar(up=True, t=0.7)]),
    "AGILITY": (SP, [sf(kind="speed", n=8, dur=0.7), ar(up=True, t=0.6)]),
    "QUICK ATTACK": (A, [h(extra=("speed", "ring"))]),
    "RAGE": (A, [h(big=1.2, extra=("anger",))]),
    "TELEPORT": (SP, [sf(kind="shimmer", n=10, dur=0.7), sf(kind="sparkles", n=8, dur=0.8, t=0.4)]),
    "NIGHT SHADE": (SP, [bm(shape="beam", w=12, hold=0.8, hits="pow")]),
    "MIMIC": (CR, [at(kind="rings", t=0.3, dur=0.4, size=0.6), sf(kind="shimmer", n=6, dur=0.5, t=0.8)]),
    "SCREECH": (CR, [sd(shape="jag", n=(3, 4, 5)), ar(up=False, t=0.85)]),
    "DOUBLE TEAM": (SP, [sf(kind="shimmer", n=10, dur=0.8), ar(up=True, t=0.6)]),
    "RECOVER": (SP, [sf(kind="heal", n=10, dur=0.9)]),
    "HARDEN": (SP, [sf(kind="glints", n=4, dur=0.6), ar(up=True, t=0.6)]),
    "MINIMIZE": (SP, [sf(kind="shimmer", n=8, dur=0.6), ar(up=True, t=0.5)]),
    "SMOKESCREEN": (SP, [cl(shape="puff", n=(4, 5, 6), size=14), ar(up=False, t=0.9)]),
    "CONFUSE RAY": (SP, [sh(shape="glow", size=10, n=(1, 1, 1), travel=0.7, burst=None, arc=6), at(kind="rings", t=1.0, dur=0.4, size=0.7)]),
    "WITHDRAW": (SP, [sf(kind="glints", n=4, dur=0.6), ar(up=True, t=0.6)]),
    "DEFENSE CURL": (SP, [sf(kind="aura_rings", dur=0.6), ar(up=True, t=0.5)]),
    "BARRIER": (SP, [sf(kind="wall", dur=0.9), ar(up=True, t=0.8)]),
    "LIGHT SCREEN": (SP, [sf(kind="wall", dur=1.0)]),
    "HAZE": (SP, [("field", {"kind": "haze", "n": (6, 8, 10), "dur": 1.0})]),
    "REFLECT": (SP, [sf(kind="wall", dur=1.0)]),
    "FOCUS ENERGY": (SP, [sf(kind="flames_rise", n=8, dur=0.9)]),
    "BIDE": (A, [sf(kind="glints", n=3, dur=0.5, t=0.0), h(big=1.6, t=0.7, extra=("ring",))]),
    "METRONOME": (SP, [gl(name="Q", at="at_body", off=(0, 16, 0), dur=0.8), sf(kind="sparkles", n=6, dur=0.6, t=0.6)]),
    "MIRROR MOVE": (SP, [sf(kind="glints", n=6, dur=0.6), sf(kind="shimmer", n=4, dur=0.4, t=0.6)]),
    "SELFDESTRUCT": (SP, [sf(kind="burst", n=(4, 5, 6), dur=0.6)]),
    "EGG BOMB": (SP, [sh(glyph="EGG", n=(1, 1, 2), arc=14, travel=0.5, spin=(0, 0, 360), burst="pow", solid=True, gpal="NORMAL"), h(big=1.2, t=0.82, slot="ebx", extra=("ring",))]),
    "LICK": (A, [bm(shape="tongue", w=5, hold=0.3, hits="spark", n=(1, 1, 1))]),
    "SMOG": (SP, [cl(shape="puff", n=(4, 5, 6), size=13)]),
    "SLUDGE": (SP, [sh(shape="orb", size=8, n=(1, 1, 2), arc=10, burst="splash", grow=1.2)]),
    "BONE CLUB": (A, [h(glyph="BONE", gpal="BONE", big=1.1, extra=("ring",))]),
    "FIRE BLAST": (SP, [sh(shape="flame", size=12, n=(1, 1, 1), travel=0.4, burst=None), at(kind="fire_star", t=0.7, dur=0.8)]),
    "WATERFALL": (A, [bm(shape="torrent", w=10, hold=0.4, hits=None, t=0.05), h(big=1.2, extra=("splash",))]),
    "CLAMP": (A, [("pincer", {"big": 1.1}), h(fx_shape=None, extra=("splash",), slot="clx")]),
    "SWIFT": (SP, [sh(shape="star", size=8, n=(3, 5, 6), gap=0.07, travel=0.45, spin=(0, 0, 720), burst="spark", spread=7)]),
    "SKULL BASH": (A, [sf(kind="glints", n=3, dur=0.4, t=0.0), h(big=1.5, t=0.6, extra=("ring", "stars"))]),
    "SPIKE CANNON": (SP, [sh(shape="needle", size=14, n=(2, 3, 5), gap=0.1, kind="long", burst="pow")]),
    "CONSTRICT": (A, [("coil", {})]),
    "AMNESIA": (SP, [gl(name="Q", at="at_body", off=(0, 16, 0), dur=0.8, palname="PSYCHIC"), ar(up=True, t=0.7)]),
    "KINESIS": (CR, [gl(name="SPOON", at="root", off=(0, 10, 0), dur=0.8, palname="STEEL"), ar(up=False, t=0.8)]),
    "SOFTBOILED": (SP, [gl(name="EGG", at="at_body", off=(0, 14, 0), dur=0.5, palname="NORMAL"), sf(kind="heal", n=10, dur=0.9, t=0.6)]),
    "HI JUMP KICK": (A, [h(glyph="FOOT", big=1.6, gpal="SKIN", extra=("speed", "ring"))]),
    "GLARE": (CR, [gl(name="EYE", at="root", off=(0, 6, 4), t=0.2, dur=0.5, palname="ANGER"), at(kind="spark_ring", t=0.7, dur=0.5)]),
    "DREAM EATER": (SP, [("drain", {"n": (3, 5, 7), "palname": "PSYCHIC"})]),
    "POISON GAS": (SP, [cl(shape="puff", n=(4, 5, 6), size=12)]),
    "BARRAGE": (SP, [sh(shape="orb", size=7, n=(2, 3, 5), arc=10, gap=0.12, burst="pow")]),
    "LEECH LIFE": (A, [("drain", {"n": (2, 3, 4)})]),
    "LOVELY KISS": (CR, [sh(glyph="HEART", gpal="HEART", n=(1, 1, 1), travel=0.6, burst=None), gl(name="Z", at="at_target", t=1.0, dur=0.6, palname="SLEEP", off=(0, 10, 0))]),
    "SKY ATTACK": (A, [sf(kind="shimmer", n=8, dur=0.6, t=0.0), h(big=1.7, t=0.8, extra=("speed", "ring"))]),
    "TRANSFORM": (SP, [sf(kind="shimmer", n=12, dur=0.9), sf(kind="sparkles", n=8, dur=0.7, t=0.5)]),
    "BUBBLE": (SP, [sh(shape="bubble", size=7, n=(3, 4, 5), gap=0.14, travel=0.7, burst="splash", spread=6, arc=4)]),
    "DIZZY PUNCH": (A, [h(glyph="FIST", gpal="SKIN", big=1.1, extra=("stars",))]),
    "SPORE": (SP, [cl(shape="powder", n=(4, 5, 6), glyph="Z", gpal="SLEEP")]),
    "FLASH": (SP, [sf(kind="flash", dur=0.5)]),
    "PSYWAVE": (SP, [sh(shape="rings", size=14, n=(4, 5, 6), gap=0.1, travel=0.5, burst=None, spread=2), at(kind="rings", t=0.8, dur=0.4)]),
    "SPLASH": (SP, [sf(kind="drops", n=8, dur=0.8)]),
    "ACID ARMOR": (SP, [sf(kind="shimmer", n=10, dur=0.8), ar(up=True, t=0.6)]),
    "CRABHAMMER": (A, [h(fx_shape="slash", big=1.4, extra=("splash", "ring"))]),
    "EXPLOSION": (SP, [sf(kind="burst", n=(6, 8, 10), dur=0.8)]),
    "FURY SWIPES": (A, [h(fx_shape="claws", n=(2, 3, 5), dt=0.13)]),
    "BONEMERANG": (SP, [sh(glyph="BONE", n=(2, 2, 2), gap=0.25, travel=0.4, ret=True, spin=(0, 0, 1080), burst="pow", solid=True, gpal="BONE")]),
    "REST": (SP, [sf(kind="zs", dur=1.2), sf(kind="heal", n=8, dur=0.8, t=0.7)]),
    "ROCK SLIDE": (SP, [at(kind="rocks_fall", n=(3, 5, 7), t=0.35, dur=0.9)]),
    "HYPER FANG": (A, [("fangs", {"big": 1.5})]),
    "SHARPEN": (SP, [sf(kind="glints", n=5, dur=0.6), ar(up=True, t=0.6)]),
    "CONVERSION": (SP, [sf(kind="shimmer", n=10, dur=0.8)]),
    "TRI ATTACK": (SP, [("tri", {})]),
    "SUPER FANG": (A, [("fangs", {"big": 1.3}), h(big=1.3, t=L0 + 0.05, slot="sfx", extra=("ring",))]),
    "SLASH": (A, [h(fx_shape="slash", big=1.6)]),
    "SUBSTITUTE": (SP, [sf(kind="doll", dur=0.8)]),
    "STRUGGLE": (A, [h(big=0.9, extra=("recoil",))]),
    "SKETCH": (SP, [at(kind="rings", t=0.3, dur=0.4, size=0.6), sf(kind="sparkles", n=8, dur=0.6, t=0.8)]),
    "TRIPLE KICK": (A, [h(glyph="FOOT", n=(3, 3, 3), dt=0.16, gpal="SKIN")]),
    "THIEF": (A, [h(glyph="HAND", gpal="DARK"), h(glyph="HAND", gpal="DARK", t=0.6, slot="th2", fx_shape="spark")]),
    "SPIDER WEB": (CR, [bm(shape="thread", w=6, hold=0.4, n=(2, 2, 2), hits=None), at(kind="web", t=0.65, dur=0.8)]),
    "MIND READER": (CR, [gl(name="EYE", at="root", off=(0, 6, 4), t=0.2, dur=0.5, palname="PSYCHIC"), ("lock", {})]),
    "NIGHTMARE": (CR, [at(kind="wisps", n=(4, 5, 6), t=0.35, dur=0.8)]),
    "FLAME WHEEL": (A, [sf(kind="flames_rise", n=8, dur=0.4, t=0.0), h(big=1.2, extra=("flames", "ring"))]),
    "SNORE": (SP, [sf(kind="zs", dur=0.6, t=0.0), sd(shape="rings", n=(2, 3, 3), t=0.4), h(big=0.8, t=0.95, slot="snx")]),
    "CURSE": (SP, [gl(name="NAIL", at="at_target", off=(0, 12, 0), t=0.4, dur=0.7, palname="CURSE"), ar(up=True, t=0.3, palname="CURSE")]),
    "FLAIL": (A, [h(n=(3, 3, 4), dt=0.13, big=0.9)]),
    "CONVERSION2": (SP, [sf(kind="shimmer", n=10, dur=0.8)]),
    "AEROBLAST": (SP, [bm(shape="beam", w=12, hold=0.8, hits="pow"), sh(shape="wind", size=14, n=(3, 3, 4), gap=0.12, travel=0.4, burst=None, slot="aw")]),
    "COTTON SPORE": (SP, [cl(shape="puff", n=(4, 5, 6), size=9), ar(up=False, t=0.9)]),
    "REVERSAL": (A, [h(n=(2, 2, 3), dt=0.15, big=1.2, extra=("ring",))]),
    "SPITE": (CR, [at(kind="wisps", n=(3, 4, 5), t=0.35, dur=0.7)]),
    "POWDER SNOW": (SP, [sh(shape="flake", size=6, n=(4, 6, 8), gap=0.06, travel=0.5, spin=(0, 0, 360), burst=None, spread=8), at(kind="rings", t=0.8, dur=0.3, size=0.6)]),
    "PROTECT": (SP, [sf(kind="shield", dur=1.0)]),
    "MACH PUNCH": (A, [h(glyph="FIST", gpal="SKIN", extra=("speed",))]),
    "SCARY FACE": (CR, [gl(name="EYE", at="root", off=(0, 6, 4), t=0.2, dur=0.6, palname="DARK", size=1.6), ar(up=False, t=0.7)]),
    "FAINT ATTACK": (A, [sf(kind="shimmer", n=6, dur=0.3, t=0.0), h(fx_shape="slash", big=1.2)]),
    "SWEET KISS": (CR, [sh(glyph="HEART", gpal="HEART", n=(1, 1, 1), travel=0.6, burst=None), at(kind="rings", t=0.95, dur=0.4, size=0.6)]),
    "BELLY DRUM": (SP, [sf(kind="drum", n=4, dur=0.7), ar(up=True, t=0.8, palname="ANGER")]),
    "SLUDGE BOMB": (SP, [sh(shape="orb", size=12, n=(1, 1, 1), arc=12, burst="splash", grow=1.3), at(kind="bubbles", n=(3, 4, 5), t=0.8, dur=0.5)]),
    "MUD-SLAP": (SP, [sh(shape="orb", size=6, n=(2, 3, 4), arc=10, gap=0.08, burst="pow")]),
    "OCTAZOOKA": (SP, [sh(shape="orb", size=10, n=(1, 1, 1), travel=0.4, burst="pow"), cl(shape="puff", n=(3, 3, 4), size=12, t=0.7, linger=0.5)]),
    "SPIKES": (SP, [at(kind="spikes", n=(4, 6, 8), t=0.3, dur=0.6)]),
    "ZAP CANNON": (SP, [sh(shape="orb", size=16, n=(1, 1, 1), travel=0.8, burst="pow", grow=1.2), at(kind="spark_ring", t=1.05, dur=0.5)]),
    "FORESIGHT": (CR, [gl(name="EYE", at="root", off=(0, 6, 4), t=0.2, dur=0.5, palname="NORMAL"), ("lock", {})]),
    "DESTINY BOND": (CR, [at(kind="wisps", n=(4, 5, 6), t=0.3, dur=0.9)]),
    "PERISH SONG": (CR, [sd(shape="notes", n=(4, 5, 6))]),
    "ICY WIND": (SP, [sh(shape="wind", size=16, n=(2, 3, 3), gap=0.12, travel=0.5, spin=(0, 0, 360), grow=1.5, burst=None),
                      sh(shape="flake", size=5, n=(4, 5, 6), gap=0.06, travel=0.5, burst=None, spread=8, slot="iwf")]),
    "DETECT": (SP, [gl(name="EYE", at="root", off=(0, 8, 2), t=0.1, dur=0.4, palname="FIGHTING"), sf(kind="shield", dur=0.9, t=0.5)]),
    "BONE RUSH": (SP, [sh(glyph="BONE", n=(2, 3, 5), gap=0.14, travel=0.4, spin=(0, 0, 720), burst="pow", solid=True, gpal="BONE")]),
    "LOCK-ON": (CR, [("lock", {})]),
    "OUTRAGE": (A, [h(n=(3, 4, 4), dt=0.15, big=1.3, extra=("ring",)), ("glyph", {"name": "ANGER", "at": "root", "off": (5, 9, 6), "palname": "ANGER", "size": 1.0})]),
    "SANDSTORM": (SP, [("field", {"kind": "sand", "n": (6, 8, 10), "dur": 1.3})]),
    "GIGA DRAIN": (SP, [("drain", {"n": (5, 7, 9)})]),
    "ENDURE": (SP, [sf(kind="flames_rise", n=6, dur=0.7), ar(up=True, t=0.5, palname="ANGER")]),
    "CHARM": (CR, [("hearts", {}), ar(up=False, t=0.8)]),
    "ROLLOUT": (A, [h(big=1.1, extra=("dust", "rocks"))]),
    "FALSE SWIPE": (A, [h(fx_shape="slash", big=1.0)]),
    "SWAGGER": (CR, [gl(name="ANGER", at="at_target", off=(0, 12, 0), t=0.4, dur=0.6, palname="ANGER"), ar(up=True, at="at_target", t=0.5, palname="ANGER")]),
    "MILK DRINK": (SP, [sf(kind="heal", n=10, dur=0.9)]),
    "SPARK": (A, [sf(kind="glints", n=4, dur=0.3, t=0.0), h(big=1.1, extra=("elec", "ring"))]),
    "FURY CUTTER": (A, [h(fx_shape="slash", n=(1, 2, 3), dt=0.14, big=1.2)]),
    "STEEL WING": (A, [h(fx_shape="slash", n=(2, 2, 2), dt=0.12, big=1.2, extra=("ring",))]),
    "MEAN LOOK": (CR, [gl(name="EYE", at="at_target", off=(0, 12, 0), t=0.3, dur=0.8, palname="DARK", size=1.5)]),
    "ATTRACT": (CR, [("hearts", {})]),
    "SLEEP TALK": (SP, [sf(kind="zs", dur=0.8, t=0.0), gl(name="NOTE", at="at_body", t=0.5, dur=0.6)]),
    "HEAL BELL": (SP, [sf(kind="bell", dur=1.0)]),
    "RETURN": (A, [h(big=1.3, extra=("hearts", "ring"))]),
    "PRESENT": (SP, [sh(glyph="GIFT", n=(1, 1, 1), arc=16, travel=0.55, spin=(0, 360, 0), burst="pow", solid=True, gpal="HEART")]),
    "FRUSTRATION": (A, [h(big=1.3, extra=("anger", "ring"))]),
    "SAFEGUARD": (SP, [sf(kind="shield", dur=1.0)]),
    "PAIN SPLIT": (SP, [("swap", {})]),
    "SACRED FIRE": (SP, [sh(shape="flame", size=14, n=(1, 1, 2), travel=0.45, burst=None, grow=1.3), at(kind="fire_star", t=0.75, dur=0.9)]),
    "MAGNITUDE": (SP, [at(kind="cracks", t=0.3, dur=0.8)]),
    "DYNAMICPUNCH": (A, [h(glyph="FIST", big=1.7, gpal="SKIN", extra=("ring", "stars"))]),
    "MEGAHORN": (A, [h(fx_shape="spark", big=1.7, extra=("ring",)), h(big=1.5, t=L0 + 0.06, slot="mhx")]),
    "DRAGONBREATH": (SP, [bm(shape="cone", w=10, hold=0.8, sweep=6, hits="pow")]),
    "BATON PASS": (SP, [gl(name="BATON", at="at_body", off=(0, 14, 0), dur=0.6, palname="STAR"), sf(kind="sparkles", n=8, dur=0.6, t=0.5)]),
    "ENCORE": (CR, [("hearts", {"stars": True})]),
    "PURSUIT": (A, [h(fx_shape="slash", extra=("speed",))]),
    "RAPID SPIN": (A, [sf(kind="aura_rings", dur=0.5, t=0.0), h(big=1.0, extra=("ring",))]),
    "SWEET SCENT": (SP, [cl(shape="puff", n=(4, 5, 6), size=10, linger=0.6), ar(up=False, t=0.9)]),
    "IRON TAIL": (A, [h(big=1.4, extra=("ring",)), h(fx_shape="spark", big=1.1, t=L0 + 0.04, slot="itx")]),
    "METAL CLAW": (A, [h(fx_shape="claws", big=1.2), h(fx_shape="spark", big=0.8, t=L0 + 0.08, slot="mcx")]),
    "VITAL THROW": (A, [at(kind="lift", t=0.2, dur=0.4), h(big=1.3, t=0.6, extra=("ring", "dust"))]),
    "MORNING SUN": (SP, [sf(kind="sun", dur=1.1)]),
    "SYNTHESIS": (SP, [sf(kind="sun", dur=1.1)]),
    "MOONLIGHT": (SP, [sf(kind="moon", dur=1.1)]),
    "HIDDEN POWER": (SP, [sh(shape="orb", size=7, n=(4, 5, 6), orbit=12, gap=0.05, travel=0.45, burst="pow", spread=4)]),
    "CROSS CHOP": (A, [h(fx_shape="slash", big=1.4), h(fx_shape="slash", big=1.4, t=L0 + 0.08, slot="cc2"), h(big=1.2, t=L0 + 0.12, slot="cc3")]),
    "TWISTER": (SP, [sh(shape="wind", size=18, n=(2, 3, 4), gap=0.1, travel=0.55, spin=(0, 0, 720), grow=1.7, burst=None), at(kind="vortex", t=0.75, dur=0.6)]),
    "RAIN DANCE": (SP, [("field", {"kind": "rain", "n": (10, 12, 14), "dur": 1.3})]),
    "SUNNY DAY": (SP, [("field", {"kind": "sun", "dur": 1.3})]),
    "CRUNCH": (A, [("fangs", {"big": 1.4}), h(fx_shape="spark", big=1.1, t=L0 + 0.06, slot="cnx")]),
    "MIRROR COAT": (SP, [sf(kind="shield", dur=0.5, t=0.0), bm(shape="beam", w=8, hold=0.5, t=0.6, hits="pow")]),
    "PSYCH UP": (SP, [sf(kind="aura_rings", dur=0.9), ar(up=True, t=0.6)]),
    "EXTREMESPEED": (A, [sf(kind="speed", n=10, dur=0.3, t=0.0), h(big=1.4, extra=("speed", "ring"))]),
    "ANCIENTPOWER": (SP, [sh(shape="rock", size=4, n=(3, 4, 5), orbit=12, gap=0.06, travel=0.4, spin=(360, 180, 0), burst="pow")]),
    "SHADOW BALL": (SP, [sh(shape="orb", size=13, n=(1, 1, 1), travel=0.55, burst="pow", grow=1.2, spin=(0, 0, 360))]),
    "FUTURE SIGHT": (SP, [("future", {})]),
    "ROCK SMASH": (A, [h(big=1.2, extra=("rocks", "ring"))]),
    "WHIRLPOOL": (SP, [at(kind="vortex", t=0.35, dur=1.1)]),
    "BEAT UP": (A, [h(n=(2, 4, 6), dt=0.12, big=0.8)]),
}


# Parts used by only a move or two.
def p_tri(fx, s, k, n, P):
    for i, pn in enumerate(("TRI_F", "TRI_I", "TRI_E")):
        a = math.radians(90 + i * 120)
        b = fx.spr(s, f"tri{i}", G(sh_orb, pn), 8, 8, "root", (math.cos(a) * 6, math.sin(a) * 6, -2))
        fx.show(s, b, L0, L0 + 0.5, k)
        fx.pos(s, b, {L0: [0, 0, 0], L0 + 0.5: [-math.cos(a) * 6, -math.sin(a) * 6, -D + 2]})
    p_hit(fx, s, k, 1, "NORMAL", big=1.2, t=L0 + 0.5, slot="trh", extra=("flames", "ice"))
    return {"hit": L0 + 0.5, "end": L0 + 1.0}


def p_lock(fx, s, k, n, P):
    b = fx.spr(s, "lock", G(sh_cross, "ANGER"), 18, 18, "at_target", (0, 0, 6), planes=1)
    fx.show(s, b, 0.5, 1.2, k * 1.8, k * 0.9)
    fx.rot(s, b, {0.5: [0, 0, 0], 0.8: [0, 0, 90], 1.2: [0, 0, 90]})
    return {"hit": None, "end": 1.3}


def p_hearts(fx, s, k, n, P, stars=False):
    x_hearts(fx, s, k, "HEART", L0 + 0.2, "ht1")
    x_hearts(fx, s, k, "HEART", L0 + 0.45, "ht2")
    if stars:
        x_stars(fx, s, k, "STAR", L0 + 0.3, "hst")
    return {"hit": None, "end": L0 + 1.3}


def p_swap(fx, s, k, n, P):
    to = fx.nominal("at_target", "root")
    b1 = fx.spr(s, "sw1", G(sh_orb, "HEAL"), 7, 7, "root", (0, 0, -2))
    b2 = fx.spr(s, "sw2", G(sh_orb, "HEART"), 7, 7, "at_target", (0, 0, 0))
    fx.show(s, b1, L0, L0 + 0.6, k)
    fx.show(s, b2, L0, L0 + 0.6, k)
    fx.pos(s, b1, {L0: [0, 0, 0], L0 + 0.3: [0, 8, -D / 2], L0 + 0.6: [0, 0, -D]})
    fx.pos(s, b2, {L0: [0, 0, 0], L0 + 0.3: [0, -6, to[2] / 2], L0 + 0.6: to})
    return {"hit": None, "end": L0 + 0.9}


def p_future(fx, s, k, n, P):
    """Future Sight: orbs of power rising off the user, then (later) crashing down on the target."""
    for i in range(3):
        b = fx.spr(s, f"fu{i}", G(sh_glow, "PSYCHIC"), 8, 8, "at_body", ((i - 1) * 6, 6, 0))
        fx.show(s, b, L0 + i * 0.08, L0 + 0.6, k)
        fx.pos(s, b, {L0: [0, 0, 0], L0 + 0.6: [0, 30, 0]})
        b2 = fx.spr(s, f"fd{i}", G(sh_glow, "PSYCHIC"), 10, 10, "at_sky", ((i - 1) * 5, 0, 0))
        fx.show(s, b2, 1.1 + i * 0.08, 1.45 + i * 0.08, k)
        fx.pos(s, b2, {1.1 + i * 0.08: [0, 0, 0], 1.45 + i * 0.08: [0, -34, 0]})
    p_hit(fx, s, k, 2, "PSYCHIC", big=1.2, t=1.45, dt=0.1, slot="fuh")
    return {"hit": 1.45, "end": 1.9, "hold": 0.4}


PARTS.update({"tri": p_tri, "lock": p_lock, "hearts": p_hearts, "swap": p_swap, "future": p_future})


# ---------- the auras: the user's type glowing round it while it uses a special move ----------

AURA_STYLE = {"FIRE": "flame", "DRAGON": "flame", "ELECTRIC": "bolt", "GHOST": "wisp", "DARK": "wisp", "POISON": "wisp", "PSYCHIC": "wisp"}


def aura_fx(typ):
    """Flickering wisps of the type's colour standing round a body 16 pixels wide, long and tall (the player scales it
    to the creature), turning slowly."""
    fx = FX(f"aura_{typ.lower()}")
    style = AURA_STYLE.get(typ, "glow")
    g = fx.group(1, "ring", "root")
    for i in range(10):
        a = math.radians(i * 36)
        if style == "flame":
            mat, w, hh = G(sh_flame, typ, seed=i * .7), 6, 10
        elif style == "bolt":
            mat, w, hh = G(sh_bolt, typ, "vert", seed=i), 5, 12
        elif style == "wisp":
            mat, w, hh = G(sh_wisp, typ), 6, 11
        else:
            mat, w, hh = G(sh_spark, typ), 7, 7
        fx.spr(1, f"a{i}", mat, w, hh, "s1_ring", (math.sin(a) * 9, (-6 if style != "glow" else -4 + (i % 3) * 5), math.cos(a) * 9),
               kind="vert" if style != "glow" else "bill", planes=1, rot=[0, i * 36, 0])
    anim = {"loop": True, "animation_length": 2.0, "bones": {"s1_ring": {"rotation": [0, "q.anim_time * 90", 0]}}}
    return fx, {f"animation.move.{fx.id}.loop": anim}


# ---------- writing ----------

def textures(fx):
    """One spot on the texture for each look (parts drawn alike share it), painted once."""
    cubes = [c for b in fx.m.bones for c in b.cubes]
    groups = {}
    for c in cubes:
        groups.setdefault((id(c.mat), tuple(c.size)), []).append(c)
    reps = [g[0] for g in groups.values()]
    tw, th = pack(reps)
    for g in groups.values():
        for c in g[1:]:
            c.uv = list(g[0].uv)
    img, glows = paint(reps, tw, th)
    return tw, th, img, glows


def write_fx(fx, anims, info):
    key = fx.id
    tw, th, img, glows = textures(fx)
    os.makedirs(os.path.join(OUT, key), exist_ok=True)
    os.makedirs(OUT_TEX, exist_ok=True)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT_TEX, key + ".png"))
    layers = []
    if glows:
        frames = []
        for i, gimg in enumerate(glows):
            Image.fromarray(gimg, "RGBA").save(os.path.join(OUT_TEX, f"{key}_glow_{i}.png"))
            frames.append(f"retromon:textures/moves/{key}_glow_{i}.png")
        layers.append({"frames": frames, "fps": 10, "emissive": True, "translucent": True})
    bones = []
    for b in fx.m.bones:
        o = {"name": b.name, "pivot": b.pivot}
        if b.parent:
            o["parent"] = b.parent
        if b.rotation:
            o["rotation"] = b.rotation
        cs = []
        for c in b.cubes:
            co = {"origin": c.origin, "size": c.size, "uv": c.uv}
            if c.rotation:
                co["rotation"] = c.rotation
                co["pivot"] = c.pivot or b.pivot
            cs.append(co)
        if cs:
            o["cubes"] = cs
        bones.append(o)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.retromon.move.{key}", "texture_width": tw, "texture_height": th, "visible_bounds_width": 8,
                        "visible_bounds_height": 6, "visible_bounds_offset": [0, 1, -2]}, "bones": bones}]}
    json.dump(geo, open(os.path.join(OUT, key, "model.geo.json"), "w"), separators=(",", ":"))
    json.dump({"format_version": "1.8.0", "animations": anims}, open(os.path.join(OUT, key, "anim.json"), "w"), separators=(",", ":"))
    info = dict(info)
    info["texture"] = f"retromon:textures/moves/{key}.png"
    info["layers"] = layers
    json.dump(info, open(os.path.join(OUT, key, "info.json"), "w"), indent=1)
    return tw, th


def read_moves():
    rows = []
    for line in open(os.path.join(DATA, "moves.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        r = line.rstrip("\n").split("\t")
        typ = "CURSE" if r[4] == "CURSE_TYPE" else r[4]
        power = int(r[3])
        status = power == 0 and r[2] not in ("OHKO", "BIDE")
        special = typ in ("FIRE", "WATER", "GRASS", "ELECTRIC", "ICE", "PSYCHIC", "DRAGON", "DARK")
        rows.append({"id": r[0], "name": r[1], "type": typ, "power": power, "accuracy": int(r[5]), "pp": int(r[6]),
                     "category": "Status" if status else ("Special" if special else "Physical"), "effect": r[2]})
    return rows


def build(only=None):
    moves = read_moves()
    names = {m["name"] for m in moves}
    missing = names - set(MOVES)
    extra = set(MOVES) - names
    assert not missing and not extra, (sorted(missing), sorted(extra))
    os.makedirs(OUT, exist_ok=True)
    index = {}
    for i, mv in enumerate(moves):
        body, parts = MOVES[mv["name"]]
        key = mv["id"]
        index[key] = {"n": i + 1, "name": mv["name"], "type": mv["type"], "category": mv["category"], "power": mv["power"],
                      "accuracy": mv["accuracy"], "pp": mv["pp"], "body": body}
        if only and mv["name"] not in only and key not in only:
            continue
        fx = FX(key)
        hits, ends, holds = [], [], []
        for s in (3, 2, 1):
            k = K[s]
            for pname, o in parts:
                o = dict(o)
                n = o.pop("n", (1, 1, 1))
                nn = n[s - 1] if isinstance(n, tuple) else n
                P = o.pop("pal", None) or mv["type"]
                r = PARTS[pname](fx, s, k, nn, P, **o)
                if s == 3:
                    if r.get("hit") is not None:
                        hits.append(r["hit"])
                    ends.append(r["end"])
                    holds.append(r.get("hold", 0.0))
        anims, lens = fx.anims()
        aura = mv["type"] if (mv["category"] == "Special" or mv["name"] in ("SOLARBEAM", "SKY ATTACK", "SKULL BASH", "FOCUS ENERGY", "OUTRAGE")) else None
        info = {"move": mv["name"], "type": mv["type"], "category": mv["category"], "body": body, "release": L0,
                "hit": round(min(hits), 3) if hits and mv["category"] != "Status" else None, "hold": round(max(holds), 3),
                "length": lens, "aura": aura, "anchors": {k2: v[1] for k2, v in ANCH.items()}, "reach": D}
        index[key].update({"aura": aura, "hit": info["hit"], "hold": info["hold"], "length": lens})
        tw, th = write_fx(fx, anims, info)
        print(f"{key:14s} {mv['name']:13s} {len(fx.m.bones):4d} bones  {tw}x{th}  {lens[3]}s")
    if not only:
        for typ in list(PAL)[:18]:
            fx, anims = aura_fx(typ)
            write_fx(fx, anims, {"aura": typ, "length": {1: 2.0}, "loop": True})
        json.dump(index, open(os.path.join(OUT, "index.json"), "w"), indent=1)
        print("auras", len(list(PAL)[:18]), "index", len(index))


if __name__ == "__main__":
    build(set(sys.argv[1:]) or None)
