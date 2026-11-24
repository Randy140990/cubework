"""
Retromon's own creatures, part nine: Johto's last, the three beasts, the stone line, the two great birds and the
moth of time (John, 2026-11-24 12:00: "build out our Retromon please for the additional Johto"). Our own designs drawn
from real animals, plants and things.

Run: python3 tools/mons9.py [names]   (all if none; refreshes mons/names.json either way)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, cell, face_blob, hash01, mix, rgb, track, wave, keys, write, flame  # noqa: E402
from monanims import four_legs, bird, floater, insect, biped, cocoon  # noqa: E402
from mons import fx_for, open_mouth, shag_mat, whisker_mat, horn_mat, leaf_mat  # noqa: E402
from mons2 import fur, brush, n  # noqa: E402
from mons3 import moves, bone_of, tail_bones, ear_mat, spike, taper_mat, glow_patch, comb_mat  # noqa: E402
from mons5 import quad, avian, bug, S_, first, solid, plain, antlers, back_row, flame_row, spot_coat, stripe_coat, both  # noqa: E402
from mons6 import upright  # noqa: E402


# ======================================================================================================
# The clouded leopard: NIMBUSPARD (in place of Raikou)
# ======================================================================================================

def nimbuspard():
    """A clouded leopard that runs with the thunder: a pale tawny-grey coat marked all over with great dark-rimmed
    blotches like storm clouds, a long thick ringed tail crackling at its tip, the longest fangs of any cat, and along
    its back a mane of dark cloud with lightning flickering through it."""
    FUR, FUR_L, FUR_D = "#C8B48A", "#ECDCB8", "#8A7650"

    def coat(at, base):
        if at.face == "bottom":
            return None
        cx, cy, cz = int(at.x / 3.2), int(at.y / 3.2), int(at.z / 3.2)
        k = hash01("cloud", cx, cy, cz)
        if k < 0.55:
            fx, fy, fz = (at.x / 3.2) % 1, (at.y / 3.2) % 1, (at.z / 3.2) % 1
            d = max(abs(fx - 0.5), abs(fz - 0.5) if at.face in ("top",) else abs(fy - 0.5), abs(fz - 0.5) if at.face in ("nx", "px") else 0)
            if 0.28 < d < 0.42:
                return rgb("#2A2420")
            if d <= 0.28:
                return mix(FUR_D, "#7A6A52", 0.5)
        return None
    cloud = Mat("#5A5E70", lambda a: mix("#3A3E50", "#9A9EB0", cell(a.x, a.y, a.z, 1.2)), glow=lambda a: rgb("#FFF07A") if hash01("bolt", round(a.x), round(a.y)) < 0.05 else None,
                noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        # A mane of storm cloud along its back: soft dark wisps streaming back, lightning flickering in them.
        wisp = Mat("#6A6E82", lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (1 - a.ly ** 1.4) + 0.04 else mix("#3A3E52", "#B8BCCC", a.ly),
                   glow=lambda a: rgb("#FFF07A") if hash01("bolt", a.col, a.row) < 0.06 else None, noise=0.01, edge=0, flat=True)
        back_row(p, wisp, 7, 3.4, w=3.4, lean=-48, z_from=0.5, z_to=-0.95, grow=0.4)
        nb = bone_of(m, "neck")
        nb.cubes += [Cube(S([-1.7, p["hy"] + 1, p["hz"] + 0.4 + k]), [n(3.4 * s), n(3.4 * s), 0], wisp, [-50, 0, 0], S([0, p["hy"] + 1, p["hz"] + 0.4 + k])) for k in (0, 1.4)]
        hd = bone_of(m, "head")
        hd.cubes += [Cube(S([x - 0.3, p["hy"] - 1.4, p["hz"] - p["hl"] - p["snout"] + 0.4]), [1, n(1.8 * s), 1], plain("#F8F4E8")) for x in (-0.9, 0.9)]
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        tb.cubes.append(Cube([-1.2 * s, tb.pivot[1] - 1.2 * s, z - 0.4], [n(2.4 * s), n(2.4 * s), n(2 * s)], Mat("#FFF4A0", glow=lambda a: rgb("#FFF07A"), noise=0, edge=0)))
    tailm = Mat(FUR, fur(FUR, FUR_L, FUR_D), lambda a: rgb("#2A2420") if (a.z % 2.6) < 0.9 else None, noise=0.02)
    quad("raikou", "NIMBUSPARD", 1.9, dict(
        s=2.0, body=(5.6, 5, 11), top=4.8, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#F4ECD8", coat=coat, FACE=FUR, head=(4.8, 4, 4.4), snout=1.8, snout_w=1,
        snout_h=2, MUZZLE="#F0E4CC", NOSE="#C87A7A", neck=2.4, neck_up=0.6, neck_tilt=-10, ears=("round", 1.8, 1.8), EAR=FUR_D, EAR_IN="#E8C8B0",
        IRIS="#E8A020", IRIS_L="#FFE07A", eye_grid=["LL", "IP"], eye_y=0.55, leg_w=2.2, toes=3, sharp=True,
        tail={"segs": [(2, 2, 3), (2, 2, 3), (1.9, 1.9, 3), (1.8, 1.8, 3), (1.8, 1.8, 2.6)], "rests": [-6, -4, 2, 6, 10], "mat": tailm, "y": 0.7}, extra=extra,
        moves=("BITE", "THUNDER"), walk=0.6, stride=32, k=0.6))


# ======================================================================================================
# The volcano bison: MAGMABISON (in place of Entei)
# ======================================================================================================

def magmabison():
    """A bison born of a volcano: a vast shaggy dark brown hump and mane over massive shoulders, a heavy bearded head
    held low, short black horns, slim hindquarters; and its hump crusted over with black rock split by glowing seams of
    lava, smoke and flame streaming from its top like a vent."""
    FUR, FUR_L, FUR_D = "#4A3022", "#7A5038", "#26160E"
    shag = Mat(FUR_D, brush(FUR_D, FUR, "#140A06", tip="#8A5A3A", tip_from=0.7), noise=0.0, edge=0)
    crust = Mat("#2A2424", lambda a: mix("#1E1818", "#5A4E4A", cell(a.x, a.y, a.z, 1.2)), lambda a: rgb("#FF7A20") if abs((a.x * 0.7 + a.z * 0.5) % 3.2 - 1.6) < 0.2 else None,
                glow=lambda a: rgb("#FFA040") if abs((a.x * 0.7 + a.z * 0.5) % 3.2 - 1.6) < 0.2 else None, noise=0.02)
    hornm = Mat("#1A1414", lambda a: rgb("#5A4A40") if a.ly < 0.3 else None, noise=0.01)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        top, bl, bw = p["top"], p["bl"], p["bw"]
        b.cubes += blob(S([-bw / 2 - 0.6, top + 1, -bl / 2 - 0.6]), S([bw + 1.2, p["bh"] + 2.4, bl * 0.55]), Mat(FUR, fur(FUR, FUR_L, FUR_D, speck=0.6), noise=0.03))
        b.cubes += blob(S([-bw / 2 + 0.2, top + p["bh"] + 2.2, -bl / 2 + 0.2]), S([bw - 0.4, 2.4, bl * 0.42]), crust)
        b.cubes += flame(S([-1.4, top + p["bh"] + 4.2, -bl / 2 + 1.4]), n(2.8 * s), n(5 * s), heat=1.2, smoke=True)
        for x in (-bw / 2 - 0.4, bw / 2 + 0.4):
            b.cubes.append(Cube(S([x, top - 0.6, -bl / 2]), [0, n((p["bh"] + 1.4) * s), n(bl * 0.5 * s)], shag))
        hd = bone_of(m, "head")
        hy, hz, hl, hh = p["hy"], p["hz"], p["hl"], p["hh"]
        hd.cubes += blob(S([-p["hw"] / 2 - 0.4, hy + hh * 0.5, hz - hl * 0.6]), S([p["hw"] + 0.8, hh * 0.7, hl * 0.8]), Mat(FUR_D, fur(FUR_D, FUR, "#140A06", speck=0.6), noise=0.03))
        hd.cubes.append(Cube(S([-1.2, hy - 2.6, hz - hl * 0.6]), [n(2.4 * s), n(2.6 * s), 0], shag))
        for x in (-1, 1):
            hd.cubes += [Cube(S([x * p["hw"] / 2 - 0.6 + (0 if x < 0 else -0), hy + hh * 0.7, hz - hl * 0.5]), [n(1.2 * s), n(1.2 * s), n(1.2 * s)], hornm),
                         Cube(S([x * (p["hw"] / 2 + 1) - 0.5, hy + hh * 0.7 + 0.6, hz - hl * 0.5]), [1, n(1.8 * s), 1], hornm, [0, 0, -x * 25],
                              S([x * (p["hw"] / 2 + 1), hy + hh * 0.7 + 0.6, hz - hl * 0.5]))]
    quad("entei", "MAGMABISON", 2.1, dict(
        s=2.1, body=(6.4, 6, 11), top=4.4, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=FUR_D, FACE=FUR_D, head=(4.6, 4.2, 4.2), snout=1.8, snout_w=1.1, snout_h=2.2,
        MUZZLE="#2A1A12", NOSE="#0A0606", neck=1.6, neck_up=-1.6, neck_tilt=4, head_tilt=24, ears=("round", 1.2, 1.2), EAR=FUR_D, EAR_IN="#5A3A2A",
        IRIS="#E8501A", IRIS_L="#FFA040", eye_grid=["LL", "IP"], eye_y=0.55, leg_w=2.2, toes=2, foot=Mat("#141010", noise=0.01),
        tail={"segs": [(1, 1, 3), (1, 1, 2)], "rests": [-50, -10], "y": 0.85, "tip": FUR_D}, extra=extra, moves=("STOMP", "FIRE BLAST"), walk=0.75, stride=24, k=0.6,
        slow=1.2))


# ======================================================================================================
# The crystal stag: RIPPLESTAG (in place of Suicune)
# ======================================================================================================

def ripplestag():
    """A slender stag of the north wind and clear water: a coat of pale aquamarine dappled white like light through
    water, legs fine as reeds, a mane of mist streaming back from its neck in two long ribbons, and antlers clear as ice
    that glow faintly blue; wherever it treads, water runs clean."""
    FUR, FUR_L, FUR_D = "#7AC8D0", "#B8ECF0", "#3A8A9A"
    mist = Mat("#E8F8FF", lambda a: CLEAR if (a.lz > 0.85 and a.col % 2) or abs(a.ly - 0.5) > 0.5 - a.lz * 0.25 else mix("#B8E8F8", "#FFFFFF", a.lz), noise=0, edge=0, flat=True)
    ice = Mat("#C8F4FF", lambda a: mix("#8ADCF0", "#F4FFFF", a.ly), glow=lambda a: rgb("#A8F0FF") if a.ly > 0.6 else None, noise=0.01, edge=0)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        antlers(p, ice, 4, spread=1.0, h=6, tip_mat=ice)
        nb = bone_of(m, "neck")
        for x in (-0.8, 0.8):
            nb.cubes.append(Cube(S([x, p["hy"] + 0.6, p["hz"] + 0.6]), [0, n(3 * s), n(12 * s)], mist, [-6, x * 8, 0], S([x, p["hy"] + 1.2, p["hz"] + 0.6])))
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        tb.cubes.append(Cube([0, tb.pivot[1] - 1.6 * s, z - 0.6], [0, n(3 * s), n(6 * s)], mist))
    quad("suicune", "RIPPLESTAG", 2.0, dict(
        s=1.8, body=(5, 5, 10), top=7, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#F0FCFF", coat=spot_coat("#F0FCFF", 1.4, 0.16, faces=("top", "nx", "px")), FACE=FUR,
        head=(3.8, 3.6, 4.4), snout=2, snout_w=0.8, snout_h=1.8, MUZZLE="#E8F8FC", NOSE="#2A4A5A", neck=3.8, neck_up=3.4, neck_tilt=-10, head_tilt=10,
        ears=("rabbit", 1.8, 3), EAR=FUR, EAR_IN="#E8F8FC", IRIS="#1A4A8A", IRIS_L="#5AA0E8", eye_grid=["LL", "IP"], eye_y=0.58, leg_w=1.4, toes=2,
        foot=Mat("#2A4A5A", noise=0.01), tail={"segs": [(1, 1, 2)], "rests": [-20], "y": 0.85}, extra=extra, moves=("WATER GUN", "AURORA BEAM"),
        walk=0.5, stride=32, k=0.6))


# ======================================================================================================
# The thorny lizards: THORNLET > CHRYSALITH > CRAGMOLOCH (in place of Larvitar, Pupitar, Tyranitar)
# ======================================================================================================

def thornlet():
    """A horned lizard hatchling: flat and round as a pebble, mossy grey-green stone-coloured and speckled so it
    vanishes among rocks, a crown of short horns round the back of its head and a fringe of little spines down its
    sides. It eats its way through soil and stone."""
    FUR, FUR_L, FUR_D = "#8A9A6A", "#B8C894", "#4E5A3A"

    def coat(at, base):
        k = hash01("peb", round(at.x * 1.4), round(at.y * 1.4), round(at.z * 1.4))
        if k < 0.1: return rgb("#5A4A3A")
        if k > 0.9: return rgb("#D8D0B0")
        return None
    hornm = taper_mat("#6A7450", "#E8E0C0")

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        y, z = p["hy"] + p["hh"] - 0.4, p["hz"] - p["hl"] * 0.2
        for i, a in enumerate((-60, -30, 0, 30, 60)):
            hd.cubes += spike(math.sin(math.radians(a)) * 1.8 * s, y * s, (z + 0.6) * s, (2.2 if a else 1.6) * s, [50, a, 0], hornm, 1.2)
        b = bone_of(m, "body")
        for side in (-1, 1):
            for z2 in (-2.4, 0, 2.4):
                b.cubes += spike(side * (p["bw"] / 2) * s, (p["top"] + 1.2) * s, z2 * s, 1.6 * s, [-15, -side * 80, 0], hornm, 1)
    quad("larvitar", "THORNLET", 0.6, dict(
        s=1.1, body=(7, 3.4, 7.6), top=1.8, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#D8D0B0", coat=coat, FACE=FUR, head=(4.4, 3, 3.6), snout=1, snout_w=0.9,
        snout_h=1.4, MUZZLE=FUR, NOSE="#3A3A2A", neck=1.2, neck_up=-1, neck_tilt=0, ears=None, IRIS="#C83A2A", IRIS_L="#FF8A5A", eye_grid=["LL", "IP"], eye_y=0.55,
        leg_w=1.6, toes=3, sharp=True, tail={"segs": [(2.4, 1.6, 2), (1.6, 1.2, 2), (1, 1, 2)], "rests": [-4, 0, 4], "y": 0.35}, extra=extra,
        moves=("BITE", "SANDSTORM"), walk=0.5, stride=30, k=0.5))


def chrysalith():
    """The horned lizard sealed in a chrysalis of stone while it grows: a hard grey-blue shell of fused rock plates,
    the outline of folded limbs pressed into it, two slits for eyes, and vents round its foot that hiss steam and jet it
    about in sudden bursts."""
    m = Model()
    s = 1.4
    S = S_(s)
    shell = Mat("#6A7A92", lambda a: mix("#3E4A5E", "#9AAAC0", 0.3 + 0.6 * a.ly), lambda a: rgb("#2A3444") if a.col % 4 == 0 and a.face != "top" else None, noise=0.025)
    glowm = Mat("#FF9A4A", glow=lambda a: rgb("#FFB060"), noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 5, 0]), blob(S([-3.4, 1.4, -3.2]), S([6.8, 8, 6.4]), shell, b=2) + blob(S([-2.4, 0, -2.2]), S([4.8, 2, 4.4]), shell))
    m.bone("head", "body", S([0, 9, 0]), blob(S([-2.8, 8.8, -2.6]), S([5.6, 3.6, 5.2]), shell) + spike(0, 12.2 * s, -0.4 * s, 2.4 * s, [-10, 0, 0], taper_mat("#5A6A82", "#C8D4E4"), 1.6))
    m.bone("jaw", "head", S([0, 9.4, -2.6]), [])
    hd = bone_of(m, "head")
    hd.cubes += [Cube(S([x - 1, 10.2, -2.65]), [n(2 * s), 1, 0], glowm) for x in (-1.3, 1.3)]
    m.bone("eyelid_right", "head", S([0, 10.2, -2.6]), [])
    bone_of(m, "body").cubes += [Cube(S([x - 0.6, 0.2, z - 0.6]), [n(1.2 * s), 1, n(1.2 * s)], glowm) for x, z in ((-2, -1.6), (2, -1.6), (-2, 1.6), (2, 1.6))]
    fx_for(m, 0.6 * s)
    moves(m, "THRASH", "SCREECH")
    A, info = cocoon("chrysalith", m, k=0.6 * s, extras={"walk": {"body": track(position=[0, wave(0.35, 1.4 * s, 0, 1.4 * s), 0])}})
    write("pupitar", "CHRYSALITH", m, A, info, 1.2)


def cragmoloch():
    """A thorny devil grown into a mountain: it stands upright on legs like pillars, its hide a craggy olive and ochre
    stone covered all over in conical thorns, a great thorned hump on its nape (a false head, to fool anything that
    bites), a heavy jaw, a crown of horns and a thick spiked tail. When it walks, the ground shifts its shape."""
    FUR, FUR_L, FUR_D = "#8A8A4A", "#C8B878", "#4A4A26"

    def coat(at, base):
        k = hash01("crag", round(at.x * 0.9), round(at.y * 0.9), round(at.z * 0.9))
        if at.face in ("nx", "px", "back") and (at.y % 4) < 1.2: return mix("#A8702A", FUR_L, k)
        if k < 0.12: return rgb(FUR_D)
        return None
    hornm = taper_mat("#5A5A30", "#F0E4C0")

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        for i in range(22):
            a = hash01("th", i) * 2 * math.pi
            y = p["hip"] + 1 + (i % 4) * p["bh"] / 4.2
            x, z = math.cos(a) * p["bw"] / 2, math.sin(a) * p["bd"] / 2
            if z < -p["bd"] / 2 * 0.6:
                continue
            b.cubes += spike(x * s, y * s, z * s, 3.2 * s, [-25, 80 if x < 0 else -80, 0] if abs(z) < abs(x) else [-30, 0, 0], hornm, 2)
        top = p["hip"] + p["bh"]
        b.cubes += blob(S([-2.6, top - 1.6, 0.4]), S([5.2, 3.6, 3.4]), p["furm"])
        for a in (-50, -20, 20, 50):
            b.cubes += spike(math.sin(math.radians(a)) * 2 * s, (top + 1.8) * s, 1.8 * s, 2.2 * s, [20, a, 0], hornm, 1.6)
        hd = bone_of(m, "head")
        for a in (-55, -25, 25, 55):
            hd.cubes += spike(math.sin(math.radians(a)) * p["hw"] * 0.45 * s, (p["hy"] + p["hh"] - 0.4) * s, (p["hz"] + 0.4) * s, 2.6 * s, [40, a, 0], hornm, 1.6)
        for nm in p["tail"]:
            tb = bone_of(m, nm)
            c = tb.cubes[0]
            tb.cubes += spike(0, c.origin[1] + c.size[1], c.origin[2] + c.size[2] / 2, 1.8 * s, [60, 0, 0], hornm, 1.4)
    upright("tyranitar", "CRAGMOLOCH", 2.0, dict(
        s=2.1, hip=4.6, body=(8.4, 9, 7.4), FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#D8C898", coat=coat, belly_size=(5.4, 7), FACE=None, head=(5.6, 4.4, 5.4),
        head_y=-0.6, head_z=-1.4, snout=(2.4, 4, 2.4), NOSE="#2A2A16", MUZZLE=FUR, MOUTH="#8A2A2A", ears=None, IRIS="#C83A1A", IRIS_L="#FF8A4A", EYE_SKIN=FUR,
        eye_in=1.2, eye_y=0.55, eye_grid=["LL", "IP"], arms=(3, 3, 2.6), HAND=FUR_D, claws=3, leg_w=3.4, foot=4.6, FOOT=FUR_D, toes=3, sharp=True, thigh=4.6,
        tail={"segs": [(4, 3.6, 3), (3.4, 3, 3), (2.8, 2.4, 3), (2.2, 2, 3)], "rests": [-30, -6, 2, 4], "y": 1}, extra=extra, moves=("CRUNCH", "ROCK SLIDE"),
        walk=0.9, stride=20, slow=1.4, guard=40, k=1.0))


# ======================================================================================================
# The albatross: ABYSSALBA (in place of Lugia)
# ======================================================================================================

def abyssalba():
    """A wandering albatross that sleeps on the deepest sea: snow-white, its long narrow wings slate-blue above and
    edged black, a great pale hooked bill, a dark brow over its eye like a frown of thought, and along its back a row of
    slate fins it cuts through storms with. It can sail around the world without a beat of its wings."""
    def coat(at, base):
        if at.face == "top" and at.z > -1: return rgb("#8A9AB8")
        return None

    def face_coat(a):
        if a.face in ("nx", "px") and 0.5 < a.ly < 0.65: return rgb("#4A5A7A")
        return None
    finm = Mat("#5A6A8A", lambda a: CLEAR if a.ly > 1 - a.lz * 0.8 else mix("#3A4A6A", "#8A9AB8", a.ly), noise=0.01, edge=0, flat=True)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        for i, z in enumerate((-1.6, 0.6, 2.8)):
            b.cubes.append(Cube(S([0, p["leg"] + p["bh"] - 0.4, z]), [0, n((2.6 - i * 0.5) * s), n(2.4 * s)], finm))
        for nm in ("wing_tip_right", "wing_tip_left"):
            try:
                wb = bone_of(m, nm)
            except StopIteration:
                continue
            for c in wb.cubes:
                if c.mat.flat:
                    c.mat = Mat("#4A5A7A", lambda a: rgb("#141820") if a.lz > 0.7 else mix("#3A4A6A", "#8A9AB8", a.ly), noise=0.01, edge=0, flat=True)
    avian("lugia", "ABYSSALBA", 5.2, dict(
        s=3.0, body=(6, 6.4, 10), leg=3, B="#F4F6FA", B_L="#FFFFFF", B_D="#C8D0DC", BELLY="#FFFFFF", coat=coat, head=(4.6, 4.4, 5), FACE="#F4F6FA", face_coat=face_coat,
        neck_up=1.6, beak=4, beak_w=1.6, beak_h=1.8, hooked=True, BEAK="#F0E0C0", BEAK_D="#C8A878", eye_w=2, eye_h=1, IRIS="#1A2A4A", eye_grid=["IP"], eye_in=2.3,
        eye_back=1.4, wing=16, WING="#6A7A9A", WING_D="#3A4A6A", WING_L="#9AAAC8", tail=4, TAIL="#C8D0DC", LEG="#E8C8C0", pitch=-6, extra=extra,
        moves=("AEROBLAST", "AEROBLAST"), walk=0.6, k=0.6, flap=0.6))


# ======================================================================================================
# The rainbow pheasant: SUNPLUME (in place of Ho-Oh)
# ======================================================================================================

def sunplume():
    """A great monal pheasant, every feather a different burning colour: a head of shining emerald with a crest of
    spoon-tipped plumes, a neck of molten copper-red, a back of royal violet-blue shot with gold, wings flashing blue and
    green, a white rump and a broad tail of cinnamon-orange, its feather edges glowing like embers in the sun."""
    def coat(at, base):
        if at.z < -1.2 and at.face != "bottom": return mix("#C83A1A", "#F08A2A", cell(at.x, at.y, at.z, 1.1))
        if at.z > 3 and at.face in ("top", "back"): return rgb("#F4F0E8")
        if at.face == "top": return mix("#4A3AA8", "#C8A030", 0.15 if hash01("gl", round(at.x), round(at.z)) > 0.2 else 0.8)
        return None

    def face_coat(a):
        return mix("#1A8A5A", "#4AE0A0", a.ly) if a.face != "bottom" else None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        stalk = Mat("#1A6A4A", noise=0, edge=0)
        spoon = Mat("#2AC890", glow=lambda a: rgb("#6AF0C0"), noise=0, edge=0)
        y0, z0 = p["hy"] + p["hh"] - 0.4, p["hz"] - p["hl"] * 0.5
        for a in (-30, -12, 6, 24):
            piv = S([0, y0, z0])
            hd.cubes += [Cube(S([-0.5, y0, z0 - 0.5]), [1, n(4 * s), 1], stalk, [a - 10, 0, 0], piv),
                         Cube(S([-0.8, y0 + 3.6, z0 - 0.8]), [n(1.6 * s), n(1.6 * s), n(1.6 * s)], spoon, [a - 10, 0, 0], piv)]
        tb = bone_of(m, "tail")
        fan = Mat("#E8822A", lambda a: CLEAR if a.lz > 0.95 or (a.lz > 0.85 and a.col % 2) else (rgb("#FFE07A") if a.lz > 0.8 else mix("#B8501A", "#F8A04A", a.lz)),
                  glow=lambda a: rgb("#FFE07A") if 0.8 < a.lz < 0.95 else None, noise=0.01, edge=0, flat=True)
        y, z = p["leg"] + p["bh"] * 0.55, p["bl"] / 2 - 0.6
        for a in (-40, -20, 0, 20, 40):
            tb.cubes.append(Cube(S([-1.4, y, z]), [n(2.8 * s), 0, n(10 * s)], fan, [14, a, 0], S([0, y, z])))
        for nm in ("wing_tip_right", "wing_tip_left", "wing_mid_right", "wing_mid_left"):
            try:
                wb = bone_of(m, nm)
            except StopIteration:
                continue
            for c in wb.cubes:
                if c.mat.flat:
                    c.mat = Mat("#2A6AC8", lambda a: rgb("#FFC040") if a.lz > 0.86 else mix("#1A4A9A", "#3ACA9A", a.ly), glow=lambda a: rgb("#FFD060") if a.lz > 0.86 else None,
                                noise=0.01, edge=0, flat=True)
    avian("ho_oh", "SUNPLUME", 3.8, dict(
        s=2.8, body=(6.4, 6.4, 9), leg=4, B="#3A3AA0", B_L="#6A6AD8", B_D="#1A1A5A", BELLY="#1A1A3A", coat=coat, head=(4.4, 4.2, 4.6), FACE="#1A8A5A", face_coat=face_coat,
        neck_up=1.6, beak=2.4, beak_w=1.4, beak_h=1.6, hooked=True, BEAK="#E8C860", BEAK_D="#B8902A", eye_w=2, eye_h=2, IRIS="#C83A1A", IRIS_L="#FF8A4A", eye_in=2.2,
        eye_back=1.2, wing=13, WING="#2A6AC8", WING_D="#1A3A8A", WING_L="#3ACA9A", tail=3, TAIL="#E8822A", LEG="#8A7A6A", pitch=-10, extra=extra,
        moves=("SACRED FIRE", "SUNNY DAY"), walk=0.6, k=0.6, flap=0.55))


# ======================================================================================================
# The moth of time: CHRONOMOTH (in place of Celebi)
# ======================================================================================================

def chronomoth():
    """A luna moth from some other summer: wings of pale spring green edged in rose, each hindwing trailing a long
    curling tail, and on all four a round eye-spot ringed like the face of a clock, its hands at different hours; a
    soft white furry body, feathery antennae, and big dark gentle eyes. Where it flutters, forests remember being young."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        ant = comb_mat("#C8A060", "#F4E0B0")
        hb = p["y"] + p["hh"] / 2
        z = -p["tl"] / 2 - p["hl"] + 0.5
        m.pair("antenna_right", "head", S([-0.8, hb, z]), [Cube(S([-0.8, hb, z]), [0, n(4 * s), n(2.4 * s)], ant, [-30, -20, -14], S([-0.8, hb, z]))])
        clock = Mat("#F4F0E0", lambda a: (rgb("#6A3A5A") if math.hypot(a.lx - 0.5, a.lz - 0.5) > 0.38 or (abs(a.lx - 0.5) < 0.06 and a.lz < 0.5) or
                                          (abs(a.lz - 0.5) < 0.06 and a.lx > 0.5) else None) if math.hypot(a.lx - 0.5, a.lz - 0.5) < 0.5 else CLEAR,
                    glow=lambda a: rgb("#FFF8C0") if math.hypot(a.lx - 0.5, a.lz - 0.5) < 0.3 else None, noise=0, edge=0, flat=True)
        for nm in ("wing_right", "wing_left", "hindwing_right", "hindwing_left"):
            wb = bone_of(m, nm)
            c = wb.cubes[0]
            sg = -1 if nm.endswith("right") else 1
            x = c.origin[0] + c.size[0] * 0.55
            wb.cubes.append(Cube([x - 1.2 * s, c.origin[1] + 0.05, c.origin[2] + c.size[2] * 0.3], [n(2.4 * s), 0, n(2.4 * s)], clock, c.rotation, c.pivot))
            if nm.startswith("hind"):
                tl = Mat("#A8E0A0", lambda a: CLEAR if abs(a.lx - 0.5) > 0.4 - a.lz * 0.2 else mix("#C8F0B0", "#E87A9A", a.lz ** 2), noise=0, edge=0, flat=True)
                wb.cubes.append(Cube([c.origin[0] + c.size[0] * (0.2 if sg < 0 else 0.6), c.origin[1], c.origin[2] + c.size[2] * 0.7], [n(1.6 * s), 0, n(7 * s)], tl, c.rotation, c.pivot))
    bug("celebi", "CHRONOMOTH", 0.6, dict(
        s=1.1, y=8, B="#F4F4EC", B_D="#C8C8B8", B_L="#FFFFFF", HEAD="#F4F4EC", thorax=(4, 3.6, 4), head=(3.6, 3.2, 2.6), abdomen=(3.6, 3.2), segs=3, seg_l=2.6,
        ABD="#F4F4EC", IRIS="#2A1A3A", IRIS_L="#6A5A8A", leg_len=3, LEGC="#E8C8A8", wings=10, WING="#B8F0A0", VEIN="#7AC07A", WING_EDGE="#E87A9A", WING_TINT="#E8FFD8",
        wing_rest=30, extra=extra, moves=("HEAL BELL", "ANCIENTPOWER"), flap=0.32, amp=40))


def main(names):
    for f in ALL9:
        if not names or f.__name__ in names:
            f()
    import mons4
    mons4.refresh_names()


ALL9 = (nimbuspard, magmabison, ripplestag, thornlet, chrysalith, cragmoloch, abyssalba, sunplume, chronomoth)

if __name__ == "__main__":
    main(set(sys.argv[1:]))
