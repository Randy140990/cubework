"""
Retromon's own creatures, part eleven: the farm and its pets (John, 2026-11-25 13:27). Not Kanto's or Johto's: the
animals of a farm and a home, for his own maps. Not caught: raised. The young grow up into a female or a male (the
calf into a cow or a bull, the lamb into a ewe or a ram...); the dog and the cat stay as they are.

  "a dog, a cat, a pig, a cow, a sheep, a horse ... chicken as well ... like the ones that you see in like Harvest Moon
   ... Let them have an evolution, because you'd have a male and female version. You know, you'd have a bull ... For
   the dog and cat type ones, probably not."

Their look: round and soft, big heads and big bright eyes, short legs (a farm game's animals), each its own.
They're written to tools/farm/ (not into the game: they're for the viewer until John says they go in).

Run: python3 tools/mons11.py [names]   (all if none)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import monlib  # noqa: E402
from monlib import CLEAR, Cube, Mat, Model, blob, face_blob, hash01, mix, rgb, write  # noqa: E402
from mons2 import fur, n  # noqa: E402
from mons3 import bone_of, spike, taper_mat, ear_mat  # noqa: E402
from mons5 import quad, avian, spot_coat, stripe_coat, both, plain, solid, back_row  # noqa: E402
from mons8 import curtain  # noqa: E402

FARM = os.path.join(os.path.dirname(os.path.abspath(__file__)), "farm")
monlib.OUT_MODELS = os.path.join(FARM, "mons")
monlib.OUT_TEX = os.path.join(FARM, "textures")

# Big bright eyes, drawn in fine texels (two to each unit): dark ovals with a shine, the young ones' taller.
EYE_YOUNG = [".PP.", "PPWP", "PPPP", ".PP."]   # a round eye two by two
EYE_ADULT = [".PP.", ".PW.", ".PP.", "...."]   # a smaller oval, one by one and a half
EYE_LASH = [".PP.", ".PW.", ".PP.", "...."]    # (hers the same: her bell, bow or flower says she is)


def patches(colour, size=3.2, chance=0.34, faces=("top", "nx", "px", "back", "front")):
    """Big soft patches (a dairy cow's): blobs a few blocks across."""
    def f(at, base):
        if at.face not in faces: return None
        k = hash01("patch", int(at.x / size + 0.5), int(at.y / size + 0.3), int(at.z / size + 0.5))
        if k < chance:
            return rgb(colour)
        return None
    return f


def bell(p, y_off=-1.8, colour="#E8B83A", strap="#C83A3A"):
    """A bell on a collar round the neck."""
    m, S, s = p["m"], p["S"], p["s"]
    nb = bone_of(m, "neck")
    bm = Mat(colour, lambda a: rgb("#FFE69A") if a.face == "top" or a.col == 0 else (rgb("#8A6420") if a.ly < 0.2 else None), noise=0.01)
    nb.cubes += [Cube(S([-p["hw"] * 0.42, p["hy"] + y_off + 0.2, p["hz"] - 1.2]), [n(p["hw"] * 0.84 * s), 1, n(2.4 * s)], plain(strap)),
                 Cube(S([-0.9, p["hy"] + y_off - 1.6, p["hz"] - 1.8]), [n(1.8 * s), n(1.8 * s), n(1.8 * s)], bm)]


def flower(p, x, colour="#FFFFFF", centre="#F6C83A"):
    """A flower tucked behind an ear (a daisy)."""
    m, S, s = p["m"], p["S"], p["s"]
    hd = bone_of(m, "head")
    pet = Mat(colour, lambda a: rgb(centre) if a.col == a.fw // 2 and a.row == a.fh // 2 else (CLEAR if (a.col in (0, a.fw - 1) and a.row in (0, a.fh - 1)) else None),
              noise=0.0, edge=0)
    hd.cubes.append(Cube(S([x - 1.1, p["hy"] + p["hh"] - 0.4, p["hz"] - p["hl"] * 0.55]), [n(2.2 * s), n(2.2 * s), 0], pet))


def nub_horns(p, mat, h=1.4, spread=2.0):
    m, S, s = p["m"], p["S"], p["s"]
    hd = bone_of(m, "head")
    for x in (-1, 1):
        hd.cubes.append(Cube(S([x * spread - 0.5, p["hy"] + p["hh"] - 0.4, p["hz"] - p["hl"] * 0.5 - 0.5]), [1, n(h * s), 1], mat, [0, 0, -x * 20],
                             S([x * spread, p["hy"] + p["hh"] - 0.4, p["hz"] - p["hl"] * 0.5])))


def chain(points, w, mat):
    """Little blocks along a curve (no joints to come apart): points [(x, y, z)], w their size."""
    return [Cube([x - w / 2, y - w / 2, z - w / 2], [n(w), n(w), n(w)], mat) for x, y, z in points]


def udder(p, colour="#F4A8B8"):
    m, S, s = p["m"], p["S"], p["s"]
    b = bone_of(m, "body")
    um = Mat(colour, lambda a: rgb("#E87A94") if a.face == "bottom" and (a.col + a.row) % 3 == 0 else None, noise=0.01)
    b.cubes += blob(S([-1.6, p["top"] - 1.0, p["bl"] / 2 - 3.8]), S([3.2, 1.6, 2.6]), um)


# ======================================================================================================
# The farm cattle: MOOPIP > MOOBELLE (cow) or BRAWNOX (bull)
# ======================================================================================================

def cattle(sid, name, stands, kind):
    """A dairy calf, a cow, a bull, as a farm game draws them: round and solid on short legs, big heads, big gentle eyes.
    The calf is white with big soft black patches, a pink nose and ears too big for it. The cow (she grows from it) is
    the same, plump and kind: pink nose, an udder, two little cream horns, a daisy behind her ear and a gold bell on a
    red collar. The bull (he grows from it) is black with white patches, broad and deep in the chest, a curly forelock
    between big curving horns, and a brass ring in his nose."""
    if kind == "calf":
        s, body, top, head, FUR, PATCH = 0.95, (5.4, 4.6, 7.0), 2.6, (5.4, 5.0, 4.4), "#F6F2EA", "#2E2A2C"
    elif kind == "cow":
        s, body, top, head, FUR, PATCH = 1.3, (7.4, 6.4, 10.0), 3.0, (5.8, 5.2, 4.8), "#F6F2EA", "#2E2A2C"
    else:
        s, body, top, head, FUR, PATCH = 1.45, (8.4, 7.4, 11.0), 3.4, (6.4, 5.6, 5.0), "#34302E", "#F2EEE6"
    FUR_L, FUR_D = mix(FUR, "#FFFFFF", 0.4), mix(FUR, "#000000", 0.18)
    horn = Mat("#F0E4C8", lambda a: rgb("#8A7A5A") if a.ly > 0.75 else None, noise=0.02)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        if kind == "cow":
            nub_horns(p, horn, 1.4, 2.0)
            bell(p)
            flower(p, -p["hw"] * 0.5 - 1.2)
            udder(p)
        if kind == "bull":
            # Big horns curving out and up, a curly forelock between them, a brass ring through his nose.
            hd = bone_of(m, "head")
            y0, z0 = p["hy"] + p["hh"] - 1.0, p["hz"] - p["hl"] * 0.5
            for sg in (-1, 1):
                pts = []
                for i in range(9):
                    t = i / 8
                    pts.append((sg * (p["hw"] / 2 + 0.4 + 3.2 * t), y0 + 2.6 * t * t, z0 - 0.6 * t))
                hd.cubes += chain([(x * s_, y * s_, z * s_) for x, y, z in pts], 1.4 * s_ * 0.9, horn)
            curl = Mat("#4A4440", lambda a: rgb("#6A625C") if (a.col + a.row) % 2 else None, noise=0.02)
            hd.cubes += blob(S([-1.4, p["hy"] + p["hh"] - 0.6, p["hz"] - p["hl"] - 0.3]), S([2.8, 1.6, 1.6]), curl)
            ring = Mat("#D8A840", lambda a: rgb("#FFE08A") if a.face == "top" else None, noise=0.0, edge=0)
            hd.cubes += [Cube(S([-1.1, p["hy"] - 0.9, p["hz"] - p["hl"] - p["snout"] - 0.4]), [n(2.2 * s_), 1, 0], ring),
                         Cube(S([-1.1, p["hy"] - 0.9, p["hz"] - p["hl"] - p["snout"] - 0.4]), [0, n(1.4 * s_), 1], ring),
                         Cube(S([1.1, p["hy"] - 0.9, p["hz"] - p["hl"] - p["snout"] - 0.4]), [0, n(1.4 * s_), 1], ring)]
            # A deep chest and a hump over his shoulders.
            bone_of(m, "body").cubes += blob(S([-p["bw"] / 2 + 0.4, p["top"] + p["bh"] - 2.2, -p["bl"] / 2 + 0.6]), S([p["bw"] - 0.8, 3.0, 4.2]), p["furm"])
        if kind == "calf":
            nub_horns(p, horn, 0.6, 1.8)

    quad(sid, name, stands, dict(
        s=s, body=body, top=top, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=FUR_L if kind != "bull" else "#F2EEE6",
        coat=patches(PATCH, 3.0 if kind != "bull" else 2.6, 0.34 if kind != "bull" else 0.2), speck=0.1,
        FACE=FUR if kind != "bull" else "#34302E", face_coat=(lambda a: rgb("#F2EEE6") if a.face == "front" and abs(a.lx - 0.5) < 0.18 and a.ly > 0.45 else None) if kind == "bull" else None,
        head=head, snout=1.8 if kind != "bull" else 2.2, snout_w=1.3, snout_h=2.4, MUZZLE="#F6B8C4" if kind != "bull" else "#C89A8A", NOSE="#E07A92" if kind != "bull" else "#5A3A3A",
        neck=1.6, neck_up=-0.2, neck_tilt=-4, head_tilt=8, ears=("round", 2.6 if kind == "calf" else 2.2, 1.8), EAR=FUR, EAR_IN="#F6C0C8", ear_rot=[-4, 30, -75],
        IRIS="#2A1A14", IRIS_L="#7A5A44", eye_w=2, eye_h=2, eye_grid=EYE_YOUNG if kind == "calf" else (EYE_LASH if kind == "cow" else EYE_ADULT),
        eye_y=0.5, eye_in=1.0, PUPIL="#16100C",
        leg_w=2.2 if kind != "bull" else 2.6, toes=2, foot=Mat("#4A3A30", noise=0.01), LEG=FUR,
        tail={"segs": [(0.8, 0.8, 2.4), (0.8, 0.8, 2.4)], "rests": [-70, -5], "y": 0.85, "tip": PATCH}, extra=extra,
        moves=("TACKLE", "MILK DRINK") if kind == "cow" else (("HORN ATTACK", "TAKE DOWN") if kind == "bull" else ("TACKLE", "GROWL")),
        walk=0.6 if kind != "calf" else 0.8, stride=24, k=0.6 if kind != "calf" else 0.9))


def moopip(): cattle("moopip", "MOOPIP", 0.9, "calf")
def moobelle(): cattle("moobelle", "MOOBELLE", 1.45, "cow")
def brawnox(): cattle("brawnox", "BRAWNOX", 1.75, "bull")


# ======================================================================================================
# The sheep: LAMBLET > FLEECELLE (ewe) or CURLRAM (ram)
# ======================================================================================================

def wool_mat(base, light, dark):
    """Thick curly wool: little round curls, lighter on top."""
    def f(at):
        k = hash01("curl", int(at.x * 1.6), int(at.y * 1.6), int(at.z * 1.6))
        c = mix(dark, light, 0.35 + 0.5 * at.ly) if at.face != "bottom" else rgb(dark)
        if k < 0.22: return mix(c, dark, 0.35)
        if k > 0.8: return mix(c, "#FFFFFF", 0.3)
        return c
    return Mat(base, f, noise=0.03)


def sheep(sid, name, stands, kind):
    """A sheep in a cloud of wool, on little dark legs. The lamb is a small soft ball of cream curls, its face and ears
    peeping out. The ewe (she grows from it) is a great round cloud of white wool, a tuft on her brow, a pink ribbon tied
    in it. The ram (he grows from it) has a darker face, a fleece a shade greyer, and big horns curling round his ears."""
    if kind == "lamb":
        s, bw, bh, bl, top, WOOL, FACE = 0.85, 5.6, 5.0, 6.4, 2.6, "#F4EEDC", "#F0E0D0"
    elif kind == "ewe":
        s, bw, bh, bl, top, WOOL, FACE = 1.2, 8.4, 7.4, 9.0, 2.8, "#FBF8F2", "#F2E2D4"
    else:
        s, bw, bh, bl, top, WOOL, FACE = 1.3, 8.2, 7.0, 9.4, 3.2, "#E6E2DA", "#4A4440"
    wool = wool_mat(WOOL, mix(WOOL, "#FFFFFF", 0.5), mix(WOOL, "#8A8070", 0.35))
    legm = Mat(FACE if kind != "ram" else "#3A3430", noise=0.02)
    horn = Mat("#D8C8A8", lambda a: rgb("#A8987A") if (a.col + a.row) % 2 else None, noise=0.02)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        # The fleece: a big round cloud round the body (bigger than it), curls standing out of it here and there.
        top_, bl_, bw_, bh_ = p["top"], p["bl"], p["bw"], p["bh"]
        b.cubes += blob(S([-bw_ / 2 - 0.8, top_ - 0.6, -bl_ / 2 - 0.6]), S([bw_ + 1.6, bh_ + 1.6, bl_ + 1.2]), wool, b=2)
        for i in range(10):
            x = (hash01("w", i, 1, 0) - 0.5) * (bw_ - 1)
            z = (hash01("w", i, 2, 0) - 0.5) * (bl_ - 1)
            b.cubes += blob(S([x - 1.2, top_ + bh_ + 0.6, z - 1.2]), S([2.4, 1.4, 2.4]), wool)
        hd = bone_of(m, "head")
        hy, hz, hl, hh, hw = p["hy"], p["hz"], p["hl"], p["hh"], p["hw"]
        hd.cubes += blob(S([-hw / 2 + 0.4, hy + hh - 0.6, hz - hl + 0.4]), S([hw - 0.8, 1.8, hl - 0.4]), wool)
        if kind == "ewe":
            rib = Mat("#F28AB0", lambda a: rgb("#FFC0D8") if a.face == "top" else None, noise=0.0, edge=0)
            hd.cubes += [Cube(S([-2.2, hy + hh + 0.8, hz - hl * 0.4]), [n(1.8 * s_), n(1.6 * s_), 1], rib), Cube(S([0.4, hy + hh + 0.8, hz - hl * 0.4]), [n(1.8 * s_), n(1.6 * s_), 1], rib),
                         Cube(S([-0.5, hy + hh + 1.1, hz - hl * 0.4 - 0.1]), [1, 1, n(1.2 * s_)], rib)]
        if kind == "ram":
            # Spiral horns curling back and round each ear.
            for sg in (-1, 1):
                pts = []
                for i in range(14):
                    a = i / 13 * 1.6 * math.pi
                    r = 2.2 - 0.9 * i / 13
                    pts.append((sg * (hw / 2 + 0.6 + 0.6 * i / 13), hy + hh - 1.2 + r * math.cos(a), hz - hl * 0.4 + r * math.sin(a)))
                hd.cubes += chain([(x * s_, y * s_, z * s_) for x, y, z in pts], 1.5 * s_ * 0.9, horn)

    quad(sid, name, stands, dict(
        s=s, body=(bw, bh, bl), top=top, FUR=WOOL, FUR_L=mix(WOOL, "#FFFFFF", 0.4), FUR_D=mix(WOOL, "#8A8070", 0.3), BELLY=WOOL, speck=0.0,
        FACE=FACE, head=(4.4, 4.2, 4.0), snout=1.4, snout_w=1.1, snout_h=2.0, MUZZLE=FACE, NOSE="#C87A8A" if kind != "ram" else "#2A2422",
        neck=1.4, neck_up=0.2, neck_tilt=-4, head_tilt=8, ears=("round", 2.4, 1.4), EAR=FACE, EAR_IN="#F4B8C4", ear_rot=[0, 20, -85],
        IRIS="#2A1E16", IRIS_L="#6A5040", eye_w=2, eye_h=2, eye_grid=EYE_YOUNG if kind == "lamb" else (EYE_LASH if kind == "ewe" else EYE_ADULT),
        eye_y=0.46, eye_in=0.7, PUPIL="#16100C",
        legm=legm, leg_w=1.6, toes=2, foot=Mat("#3A302A", noise=0.01),
        tail={"segs": [(1.6, 1.4, 1.6)], "rests": [-40], "y": 0.7}, extra=extra,
        moves=("TACKLE", "COTTON SPORE") if kind != "ram" else ("HEADBUTT", "TAKE DOWN"), walk=0.7, stride=24, k=0.7))


def lamblet(): sheep("lamblet", "LAMBLET", 0.75, "lamb")
def fleecelle(): sheep("fleecelle", "FLEECELLE", 1.2, "ewe")
def curlram(): sheep("curlram", "CURLRAM", 1.3, "ram")


# ======================================================================================================
# The pigs: OINKLET > TRUFFLESOW (sow) or TUSKHOG (boar)
# ======================================================================================================

def pig(sid, name, stands, kind):
    """A pig, round as a barrel on neat little trotters, a flat round snout, a curly tail. The piglet is shell pink,
    big-eared and bright-eyed. The sow (she grows from it) is a plump pink with soft brown spots, floppy ears and a
    snout dusty from rooting (she finds truffles). The boar (he grows from it) is rosy grey, a bristly dark crest down
    his back and two little tusks curling up from his jaw."""
    if kind == "piglet":
        s, body, top, head, SKIN = 0.8, (5.0, 4.4, 6.6), 2.0, (4.8, 4.4, 4.0), "#F6B8C0"
    elif kind == "sow":
        s, body, top, head, SKIN = 1.25, (7.6, 6.4, 10.0), 2.4, (5.6, 5.0, 4.6), "#F4AAB6"
    else:
        s, body, top, head, SKIN = 1.35, (7.8, 6.8, 10.4), 2.8, (5.8, 5.2, 4.8), "#B89A9A"
    SKIN_L, SKIN_D = mix(SKIN, "#FFFFFF", 0.35), mix(SKIN, "#5A3A3A", 0.25)
    snout_m = Mat(mix(SKIN, "#E07A8A", 0.4), lambda a: rgb("#8A3A4A") if a.face == "front" and abs(a.lx - 0.5) > 0.15 and abs(a.lx - 0.5) < 0.32 and 0.3 < a.ly < 0.7 else None,
                  noise=0.01)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        hy, hz, hl = p["hy"], p["hz"], p["hl"]
        # The round flat snout disc on the front of the face (over the muzzle).
        hd.cubes.append(Cube(S([-1.6, hy + 0.2, hz - hl - p["snout"] - 0.3]), [n(3.2 * s_), n(2.4 * s_), 1], snout_m))
        # The curly tail: a little spiral of blocks.
        b = bone_of(m, "body")
        pts = [(0.8 * math.cos(t), p["top"] + p["bh"] * 0.75 + 0.8 * math.sin(t), p["bl"] / 2 + 0.4 + 0.25 * t) for t in [i * 0.7 for i in range(9)]]
        b.cubes += chain([(x * s_, y * s_, z * s_) for x, y, z in pts], 0.9 * s_, Mat(SKIN, noise=0.02))
        if kind == "boar":
            br = taper_mat("#5A4444", "#2A2222")
            for i in range(9):
                z = -p["bl"] / 2 + 1 + i * (p["bl"] - 2) / 8
                b.cubes += spike(0, (p["top"] + p["bh"] - 0.3) * s_, z * s_, (1.8 - 0.5 * abs(i - 4) / 4) * s_, [70, 0, 0], br, 1)
            tusk = Mat("#F6F0E0", lambda a: rgb("#D8CCB0") if a.ly < 0.3 else None, noise=0.01)
            for sg in (-1, 1):
                hd.cubes.append(Cube(S([sg * 1.5 - 0.4, hy - 0.4, hz - hl - p["snout"] + 0.2]), [1, n(1.6 * s_), 1], tusk, [-10, 0, -sg * 25],
                                     S([sg * 1.5, hy - 0.4, hz - hl - p["snout"] + 0.6])))
        if kind == "sow":
            # Dusty from rooting: a little earth on her snout and her trotters.
            hd.cubes.append(Cube(S([-1.4, hy + 0.1, hz - hl - p["snout"] - 0.35]), [n(2.8 * s_), n(0.6 * s_), 0], Mat("#9A7458", noise=0.04, edge=0)))

    quad(sid, name, stands, dict(
        s=s, body=body, top=top, FUR=SKIN, FUR_L=SKIN_L, FUR_D=SKIN_D, BELLY=SKIN_L, speck=0.05,
        coat=spot_coat("#A87A5A", 2.0, 0.12) if kind == "sow" else None,
        FACE=SKIN, head=head, snout=1.4, snout_w=1.3, snout_h=2.2, MUZZLE=mix(SKIN, "#E07A8A", 0.3), NOSE=mix(SKIN, "#E07A8A", 0.4),
        neck=1.0, neck_up=-0.6, neck_tilt=0, head_tilt=4, ears=("fennec" if kind != "sow" else "round", 2.4, 2.4 if kind != "sow" else 2.2), EAR=SKIN, EAR_IN="#F08A9A",
        ear_rot=[30, 10, -30] if kind == "sow" else [-6, 10, -24],
        IRIS="#2A1A16", IRIS_L="#6A4A40", eye_w=2, eye_h=2, eye_grid=EYE_YOUNG if kind == "piglet" else (EYE_LASH if kind == "sow" else EYE_ADULT),
        eye_y=0.56, eye_in=0.9, PUPIL="#16100C",
        leg_w=1.8, toes=2, foot=Mat("#8A5A5A", noise=0.01) if kind != "sow" else Mat("#8A6448", noise=0.02), extra=extra,
        moves=("TACKLE", "DIG") if kind == "sow" else (("TAKE DOWN", "HORN ATTACK") if kind == "boar" else ("TACKLE", "TAIL WHIP")), walk=0.8, stride=26, k=0.8))


def oinklet(): pig("oinklet", "OINKLET", 0.55, "piglet")
def trufflesow(): pig("trufflesow", "TRUFFLESOW", 1.05, "sow")
def tuskhog(): pig("tuskhog", "TUSKHOG", 1.15, "boar")


# ======================================================================================================
# The chickens: PEEPLING > CLUCKMAID (hen) or CROWCOMB (rooster)
# ======================================================================================================

def comb(p, h=1.6, w=3.2, colour="#E03A3A", wattle=True):
    m, S, s = p["m"], p["S"], p["s"]
    hd = bone_of(m, "head")
    cm = Mat(colour, lambda a: CLEAR if a.ly > 0.55 and a.col % 3 == 1 else (rgb("#FF6A5A") if a.face == "top" else None), noise=0.01, edge=0, flat=True)
    hd.cubes.append(Cube(S([-0.1, p["hy"] + p["hh"] - 0.4, p["hz"] - p["hl"] + 0.2]), [0, n(h * s), n(w * s)], cm))
    if wattle:
        wm = Mat(colour, noise=0.01, edge=0)
        hd.cubes.append(Cube(S([-0.6, p["hy"] - 0.6, p["hz"] - p["hl"] - 0.8]), [n(1.2 * s), n(1.4 * s), n(0.8 * s)], wm))


def chicken(sid, name, stands, kind):
    """A chicken. The chick is a fluffy yellow ball on two little legs, a tiny beak, wing nubs and big eyes. The hen
    (she grows from it) is round and white, a small red comb and wattle, her wings tipped cream. The rooster (he grows
    from it) stands tall: a big red comb and wattles, a cape of glossy copper feathers on his neck, a golden-brown body
    and a great arching tail of dark green-black sickle feathers."""
    if kind == "chick":
        o = dict(s=0.6, body=(6, 6, 6), leg=1.6, B="#F8D84A", B_L="#FFF0A0", B_D="#E0B020", BELLY="#FFF0A0", head=(5, 5, 4.6), beak=1.0, BEAK="#F0A030", WING="#F0C83A",
                 WING_D="#D8A820", WING_L="#FFE680", wing=3, tail=1.2, TAIL="#F0C83A", LEG="#F0A030", eye_w=2, eye_h=2, eye_grid=EYE_YOUNG, side_eyes=False, eye_in=0.7,
                 eye_back=0, neck_up=-1.4, pitch=0, moves=("PECK", "GROWL"))
    elif kind == "hen":
        o = dict(s=1.0, body=(6.4, 6.4, 8.0), leg=2.4, B="#FAF7F0", B_L="#FFFFFF", B_D="#D8D0C2", BELLY="#FFFFFF", head=(4.0, 4.4, 4.0), beak=1.4, BEAK="#F2B43A",
                 WING="#F4EEE2", WING_D="#D8CCB4", WING_L="#FFFFFF", wing=6, tail=3, TAIL="#F4EEE2", tail_tilt=40, LEG="#F2B43A", eye_w=2, eye_h=2, eye_grid=["PW", "PP"],
                 pitch=-4, neck_up=0.4, moves=("PECK", "SAND ATTACK"))
    else:
        o = dict(s=1.15, body=(6.0, 6.4, 8.2), leg=3.4, B="#C8782E", B_L="#E8A24A", B_D="#8A4A1E", BELLY="#3A2A22", head=(3.8, 4.4, 4.0), beak=1.5, BEAK="#E8B040",
                 WING="#8A4A1E", WING_D="#2A3A2E", WING_L="#C8782E", wing=6.5, tail=1, TAIL="#1E2A24", LEG="#E8B040", eye_w=2, eye_h=2, eye_grid=["LL", "IP"],
                 IRIS="#C87A2A", pitch=-10, neck_up=1.4, moves=("PECK", "GROWL"))

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        if kind == "hen":
            comb(p, 1.2, 2.6)
        if kind == "rooster":
            comb(p, 2.2, 3.6)
            # A cape of glossy copper hackles down the neck.
            nb = bone_of(m, "neck")
            cape = curtain("#E89A3A", "#FFC46A", "#A85A1E")
            for sg in (-1, 1):
                nb.cubes.append(Cube(S([sg * p["hw"] * 0.42, p["hy"] - 2.6, p["hz"] + 0.2]), [0, n(3.4 * s_), n(2.6 * s_)], cape))
            nb.cubes.append(Cube(S([-p["hw"] * 0.42, p["hy"] - 2.6, p["hz"] + 2.8]), [n(p["hw"] * 0.84 * s_), n(3.4 * s_), 0], cape))
            # The sickle feathers: long curves arching up and back over the tail, glossed green.
            tb = bone_of(m, "tail")
            sick = Mat("#1E2A24", lambda a: rgb("#2E5A3E") if (a.col + a.row) % 3 == 0 else None, noise=0.01, edge=0)
            for k, (sp, hgt) in enumerate(((-1.0, 6.0), (0.0, 7.0), (1.0, 6.0), (-0.5, 5.0), (0.5, 5.0))):
                pts = []
                for i in range(10):
                    t = i / 9
                    pts.append((sp * 0.8, p["leg"] + p["bh"] * 0.6 + hgt * math.sin(math.pi * 0.75 * t), p["bl"] / 2 + 0.2 + 5.2 * t))
                tb.cubes += chain([(x * s_, y * s_, z * s_) for x, y, z in pts], 0.9 * s_, sick)
        if kind == "chick":
            # Fluff: a few soft tufts on its head.
            hd = bone_of(m, "head")
            for x in (-0.6, 0.4):
                hd.cubes.append(Cube(S([x, p["hy"] + p["hh"] - 0.2, p["hz"] - p["hl"] * 0.5]), [1, n(1.2 * s_), 0], Mat("#FFE680", noise=0.0, edge=0)))

    avian(sid, name, stands, dict(o, extra=extra, walk=0.6, stride=34, k=0.8, flap=0.3))


def peepling(): chicken("peepling", "PEEPLING", 0.35, "chick")
def cluckmaid(): chicken("cluckmaid", "CLUCKMAID", 0.7, "hen")
def crowcomb(): chicken("crowcomb", "CROWCOMB", 0.95, "rooster")


# ======================================================================================================
# The horses: FOALET > MAREBRAID (mare) or STORMSTEED (stallion)
# ======================================================================================================

def mane(p, n_, colour, light, dark, length=3.0, width=0.0, braided=False):
    """A mane down the back of the neck: hair hanging to one side in a curtain, or braided into little knots."""
    m, S, s = p["m"], p["S"], p["s"]
    nb = bone_of(m, "neck")
    hy, hz = p["hy"], p["hz"]
    if braided:
        knot = Mat(colour, lambda a: rgb(light) if (a.col + a.row) % 2 else rgb(dark), noise=0.01)
        for i in range(n_):
            t = i / max(1, n_ - 1)
            y = hy + 1.4 - t * 4.4
            z = hz + 1.2 + t * 2.6
            nb.cubes.append(Cube(S([-0.7, y - 0.7, z - 0.7]), [n(1.4 * s), n(1.4 * s), n(1.4 * s)], knot))
        return
    hang = curtain(colour, light, dark)
    nb.cubes.append(Cube(S([-0.9, hy - 3.6, hz + 0.8]), [0, n((length + 2.2) * s), n(3.6 * s)], hang))
    nb.cubes.append(Cube(S([-0.6, hy + 1.0, hz + 0.6]), [n(1.2 * s), n(1.2 * s), n(4.2 * s)], Mat(colour, noise=0.02)))


def horse(sid, name, stands, kind):
    """A horse. The foal is all legs: long and knobbly-kneed under a short body, a big head with big eyes, a fuzzy little
    brush of a mane and a tuft of a tail; chestnut, a white blaze and white socks. The mare (she grows from it) is a
    sleek chestnut, her cream mane braided in knots down her neck, a forelock, her long cream tail flowing. The stallion
    (he grows from it) is a dark bay, tall and strong, a long black mane falling down his neck, white feathering round
    his hooves, a flowing black tail. A horse can be ridden."""
    from mons import stout_legs
    from mons3 import tail_bones
    from monanims import four_legs
    from mons import fx_for
    from mons3 import moves
    m = Model()
    if kind == "foal":
        s, top, W, H, L, FUR, MANE, nw, nh = 1.0, 7.4, 4.2, 4.0, 7.6, "#B8703A", "#E8C890", 2.8, 5.6
    elif kind == "mare":
        s, top, W, H, L, FUR, MANE, nw, nh = 1.4, 8.0, 5.0, 5.4, 11.0, "#B8703A", "#F0DCA8", 3.2, 7.0
    else:
        s, top, W, H, L, FUR, MANE, nw, nh = 1.55, 8.4, 5.6, 6.0, 12.0, "#5A3220", "#1E1614", 3.8, 7.4
    S = lambda v: [x * s for x in v]
    FUR_L, FUR_D = mix(FUR, "#FFFFFF", 0.25), mix(FUR, "#000000", 0.25)
    base = fur(FUR, FUR_L, FUR_D, belly=FUR_L, below=(top + 1.2) * s, speck=0.05)
    furm = Mat(FUR, base, noise=0.02)
    WHITE = "#F8F4EC"
    face = Mat(FUR, lambda a: rgb(WHITE) if a.face == "front" and abs(a.lx - 0.5) < 0.16 else (rgb(FUR_L) if a.face == "bottom" else None), noise=0.02)
    muzzle = Mat(mix(FUR, "#3A2A22", 0.4), lambda a: rgb("#1A1210") if a.face == "front" and a.ly > 0.5 and abs(a.lx - 0.5) > 0.2 else
                 (rgb(WHITE) if a.face in ("top", "front") and abs(a.lx - 0.5) < 0.16 and a.ly > 0.3 else None), noise=0.02)
    jaw_m = Mat(mix(FUR, "#3A2A22", 0.4), noise=0.02)
    legm = Mat(FUR, lambda a: rgb(WHITE) if a.ly < 0.32 else None, noise=0.02)
    hoof = Mat("#3A2A22", lambda a: rgb("#5A4A3A") if a.face == "top" else None, noise=0.01)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, top + H / 2, 0]), blob(S([-W / 2, top, -L / 2]), S([W, H, L]), furm))
    hy = top + H
    nz = -L / 2 + 1.0
    m.bone("neck", "body", S([0, hy - 1, nz]), blob(S([-nw / 2, hy - 2.4, nz - 2.6]), S([nw, nh, 3.2]), furm), rotation=[-30, 0, 0])
    hz = nz - 2.0
    hhy = hy + nh - 2.6
    hl = 4.0 if kind == "foal" else 4.6
    ml = 2.2 if kind == "foal" else 3.0
    m.bone("head", "neck", S([0, hhy + 1, hz]), face_blob(S([-1.9, hhy, hz - hl]), S([3.8, 3.4, hl]), face) +
           [Cube(S([-1.3, hhy, hz - hl - ml]), [n(2.6 * s), n(2.4 * s), n(ml * s)], muzzle)], rotation=[34, 0, 0])
    m.bone("jaw", "head", S([0, hhy + 0.4, hz - hl]), [Cube(S([-1.1, hhy - 0.6, hz - hl - ml + 0.2]), [n(2.2 * s), 1, n(ml * s)], jaw_m)])
    grid = EYE_YOUNG if kind == "foal" else (EYE_LASH if kind == "mare" else EYE_ADULT)
    m.eyes("head", x_inner=1.0 * s, y=(hhy + 1.4) * s, z=(hz - hl) * s, w=2, h=2, iris="#2A1A12", pupil="#120A08", skin=FUR, grid=grid)
    m.pair("ear_right", "head", S([-1.2, hhy + 3.2, hz - 1.2]), [Cube(S([-2.2, hhy + 3.0, hz - 1.2]), [n(1.4 * s), n(2.2 * s), 0], ear_mat(FUR, "#C89A8A", "fennec"))],
           rotation=[-8, 10, -10])
    stout_legs(m, (W / 2 - 0.9) * s, (-(L / 2 - 1.6) * s, (L / 2 - 1.6) * s), round(top * s), max(2, round((1.6 if kind == "foal" else 1.8) * s)), legm, hoof, 1, foot=hoof)
    nb, hd, b = bone_of(m, "neck"), bone_of(m, "head"), bone_of(m, "body")
    # The mane.
    if kind == "foal":
        nb.cubes.append(Cube(S([-0.6, hy - 1.0, nz + 0.2]), [n(1.2 * s), n(nh * s), n(1.4 * s)], Mat(MANE, lambda a: rgb(mix(MANE, "#8A6A4A", 0.3)) if (a.row % 2) else None, noise=0.03)))
    elif kind == "mare":
        knot = Mat(MANE, lambda a: rgb(mix(MANE, "#FFFFFF", 0.35)) if (a.col + a.row) % 2 else rgb(mix(MANE, "#8A6A3A", 0.25)), noise=0.01)
        for i in range(7):
            t = i / 6
            nb.cubes.append(Cube(S([-0.75, hy - 1.6 + t * (nh - 0.8) - 0.75, nz + 0.6 - 0.75]), [n(1.5 * s), n(1.5 * s), n(1.5 * s)], knot))
        hd.cubes.append(Cube(S([-0.9, hhy + 1.6, hz - hl - 0.05]), [n(1.8 * s), n(1.8 * s), 0], curtain(MANE, "#FFFFFF", "#C8A870")))
    else:
        nb.cubes.append(Cube(S([-0.6, hy - 2.2, nz + 0.4]), [n(1.2 * s), n((nh + 0.8) * s), n(1.4 * s)], Mat(MANE, noise=0.02)))
        nb.cubes.append(Cube(S([-0.7, hy - 3.6, nz + 1.0]), [0, n((nh + 1.4) * s), n(2.6 * s)], curtain(MANE, "#4A3A30", "#0E0A08")))
        hd.cubes.append(Cube(S([-0.9, hhy + 1.4, hz - hl - 0.05]), [n(1.8 * s), n(2.2 * s), 0], curtain(MANE, "#4A3A30", "#0E0A08")))
        feather = Mat(WHITE, lambda a: CLEAR if a.ly > 0.7 and a.col % 2 else None, noise=0.03, edge=0)
        for bb in m.bones:
            if bb.name.startswith("shin_") and bb.cubes:
                c = bb.cubes[0]
                bb.cubes.append(Cube([c.origin[0] - 0.5, c.origin[1], c.origin[2] - 0.5], [n(c.size[0] + 1), n(2.2 * s), n(c.size[2] + 1)], feather))
    # The tail: hair falling from the dock.
    th = 2.6 if kind == "foal" else (7.0 if kind == "mare" else 7.4)
    tail_c = MANE if kind != "foal" else mix(MANE, FUR, 0.4)
    m.bone("tail", "body", S([0, hy - 0.8, L / 2]), [Cube(S([-0.9, hy - 0.8 - th, L / 2 - 0.4]), [n(1.8 * s), n(th * s), n(1.8 * s)],
                                                     curtain(tail_c, mix(tail_c, "#FFFFFF", 0.25), mix(tail_c, "#000000", 0.25)), [-16, 0, 0], S([0, hy - 0.8, L / 2]))])
    fx_for(m, 0.8 * s)
    moves(m, *(("TACKLE", "TAIL WHIP") if kind == "foal" else ("STOMP", "DOUBLE KICK")))
    A, info = four_legs(name.lower(), m, walk=0.6, stride=34, k=0.6 * s)
    write(sid, name, m, A, info, stands)


def foalet(): horse("foalet", "FOALET", 1.2, "foal")
def marebraid(): horse("marebraid", "MAREBRAID", 1.8, "mare")
def stormsteed(): horse("stormsteed", "STORMSTEED", 2.0, "stallion")


# ======================================================================================================
# The pets: HEELHOUND (the farm's dog) and BARNPURR (its cat)
# ======================================================================================================

def heelhound():
    """The farm's dog, a herding collie: black and white with tan on its cheeks and brows, a deep white ruff on its
    chest, its ears up but tipped over at the ends, a long plumed tail with a white tip, bright clever eyes. It herds
    the flock, finds what's lost and keeps watch at night. It doesn't grow into anything else."""
    BLACK, WHITE, TAN = "#26221F", "#F6F2EA", "#C8823A"

    def coat(at, base):
        if at.face == "bottom": return rgb(WHITE)
        if at.y < 6.0 and at.z < -2: return rgb(WHITE)   # the ruff and chest
        return None

    def face_coat(a):
        if a.face == "front" and abs(a.lx - 0.5) < 0.12: return rgb(WHITE)
        if a.face == "front" and a.ly < 0.4: return rgb(WHITE)
        if a.face in ("front", "nx", "px") and 0.45 < a.ly < 0.6: return rgb(TAN)
        return None

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        nb = bone_of(m, "neck")
        ruff = Mat(WHITE, fur(WHITE, "#FFFFFF", "#D8D0C4", speck=0.6), noise=0.03)
        nb.cubes += blob(S([-p["hw"] * 0.5, p["hy"] - 3.2, p["hz"] - 0.8]), S([p["hw"], 3.2, 2.6]), ruff)
        # The plume of a tail, its white tip.
        t = bone_of(m, p["tail"][-1]) if p["tail"] else None
        if t:
            t.cubes.append(Cube([-1.2 * s_, t.pivot[1] - 1.2 * s_, t.pivot[2] + 1.6 * s_], [n(2.4 * s_), n(2.4 * s_), n(1.6 * s_)], plain(WHITE)))
        # Ears tipped over: a little fold at each tip.
        for side in ("ear_right", "ear_left"):
            for b in m.bones:
                if b.name == side and b.cubes:
                    c = b.cubes[0]
                    b.cubes.append(Cube([c.origin[0], c.origin[1] + c.size[1] - 0.2, c.origin[2] - 0.1], [c.size[0], 0, n(1.2 * s_)], Mat(BLACK, noise=0.01, edge=0)))

    quad("heelhound", "HEELHOUND", 0.85, dict(
        s=1.0, body=(4.6, 4.4, 9.0), top=4.4, FUR=BLACK, FUR_L="#3A3430", FUR_D="#141210", BELLY=WHITE, coat=coat, speck=0.1, FACE=BLACK, face_coat=face_coat,
        head=(4.4, 3.8, 3.8), snout=2.4, snout_w=0.9, snout_h=1.8, MUZZLE=WHITE, NOSE="#1A1614", neck=2.4, neck_up=1.0, neck_tilt=-14, head_tilt=12,
        ears=("fennec", 1.8, 2.4), EAR=BLACK, EAR_IN="#C8A090", ear_rot=[-6, 10, -12], IRIS="#7A4A1A", IRIS_L="#C88A3A", eye_w=2, eye_h=2, eye_grid=["LL", "IP"], eye_y=0.58,
        LEG=WHITE, leg_w=1.6, toes=2, foot=Mat(WHITE, noise=0.02),
        tail={"segs": [(1.4, 1.4, 2.6), (1.6, 1.6, 2.6), (1.8, 1.8, 2.4)], "rests": [-30, 10, 10], "y": 0.75}, extra=extra,
        moves=("BITE", "ROAR"), walk=0.9, stride=30, k=0.8))


def barnpurr():
    """The farm's cat, a barn tabby: ginger striped in darker orange, a white chest and white paws, a white muzzle,
    tall pointed ears, a long tail curling up at its end, big green eyes. It keeps the mice from the grain, naps in
    the sun and leaves you presents on the doorstep. It doesn't grow into anything else."""
    GINGER, WHITE = "#E89A4A", "#FBF6EE"

    def coat(at, base):
        if at.face == "bottom": return rgb(WHITE)
        if at.z < -2.6 and at.y < 4.2: return rgb(WHITE)
        return stripe_coat("#B8642A", every=1.6, width=0.6)(at, base)

    quad("barnpurr", "BARNPURR", 0.5, dict(
        s=0.75, body=(4.0, 3.8, 7.6), top=3.6, FUR=GINGER, FUR_L="#F6B870", FUR_D="#B8642A", BELLY=WHITE, coat=coat, speck=0.1, FACE=GINGER,
        face_coat=lambda a: rgb(WHITE) if a.face == "front" and a.ly < 0.42 else None,
        head=(4.6, 4.0, 3.6), snout=0.9, snout_w=1.1, snout_h=1.6, MUZZLE=WHITE, NOSE="#E07A8A", neck=1.2, neck_up=0.6, neck_tilt=-6, head_tilt=6,
        ears=("fennec", 2.0, 2.4), EAR=GINGER, EAR_IN="#F4B8B0", ear_rot=[-4, 6, -10], IRIS="#6AB84A", IRIS_L="#B8E88A", PUPIL="#0A0A08", eye_w=2, eye_h=2,
        eye_grid=["LLLL", "IPJW", "IPII", ".PI."], eye_y=0.52, eye_in=0.9, LEG=WHITE, leg_w=1.4, toes=3, foot=Mat(WHITE, noise=0.02),
        tail={"segs": [(1.0, 1.0, 3.0), (1.0, 1.0, 3.0), (1.0, 1.0, 2.6)], "rests": [-10, 30, 40], "y": 0.75, "tip": "#B8642A"},
        moves=("SCRATCH", "GROWL"), walk=0.9, stride=30, k=0.9))


ALL11 = (moopip, moobelle, brawnox, lamblet, fleecelle, curlram, oinklet, trufflesow, tuskhog, peepling, cluckmaid, crowcomb, foalet, marebraid, stormsteed,
         heelhound, barnpurr)


def main(names):
    os.makedirs(monlib.OUT_MODELS, exist_ok=True)
    os.makedirs(monlib.OUT_TEX, exist_ok=True)
    for f in ALL11:
        if not names or f.__name__ in names:
            f()


if __name__ == "__main__":
    main(sys.argv[1:])
