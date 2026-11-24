"""
Retromon's own creatures, part seven: Johto's, Espeon to Magcargo (John, 2026-11-24 12:00: "build out our Retromon
please for the additional Johto"). Our own designs drawn from real animals, plants and things.

Run: python3 tools/mons7.py [names]   (all if none; refreshes mons/names.json either way)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, cell, face_blob, hash01, mix, rgb, track, wave, keys, write, flame  # noqa: E402
from monanims import four_legs, bird, floater, insect, biped, serpent, flyer, hopper, crawler, cocoon  # noqa: E402
from mons import fx_for, open_mouth, shag_mat, stout_legs, whisker_mat, horn_mat, leaf_mat, membrane_mat, dragon_wing  # noqa: E402
from mons2 import fur, brush, n  # noqa: E402
from mons3 import (moves, bone_of, tail_bones, ear_mat, spike, taper_mat, legs_pair, glow_patch, upright_legs, upright_arms, conch_mat,  # noqa: E402
                   hex_drum, basalt_mat, compound_grid, wing_paint, comb_mat)
from mons5 import quad, avian, bug, S_, first, solid, plain, antlers, back_row, flame_row, spot_coat, stripe_coat, both  # noqa: E402
from mons6 import upright, fish, fin_mat, face_petal, flower_face  # noqa: E402


def ringed(base, ring, every=2.2, width=0.9, glow=None):
    """A tail in rings: bands of a second colour round it (glow: the bands glow)."""
    m = Mat(base, fur(base, mix(base, "#FFFFFF", 0.2), mix(base, "#000000", 0.3), speck=0.2), lambda a: rgb(ring) if (a.z % every) < width else None,
            glow=(lambda a: rgb(glow) if (a.z % every) < width else None) if glow else None, noise=0.02)
    return m


# ======================================================================================================
# The genets: SUNGENET > DUSKGENET (in place of Espeon, Umbreon)
# ======================================================================================================

def genet(sid, name, stands, dusk):
    """A genet, slender as a cat and long as a weasel, spotted along its flanks, a long tail in rings, big rounded ears
    and a pale mask under its eyes. The sun's is honey-gold with rust spots, a glowing gold mark on its brow and violet
    eyes that see what's coming. The dusk's is charcoal, its spots and rings glowing moon-silver in the dark, and its
    eyes amber."""
    if not dusk:
        FUR, FUR_L, FUR_D, SPOT, MARK, IRIS, IRIS_L = "#E8C48A", "#FFF0C8", "#B08848", "#B0602A", "#FFC040", "#8A3AC8", "#D8A0FF"
    else:
        FUR, FUR_L, FUR_D, SPOT, MARK, IRIS, IRIS_L = "#2E2E38", "#4E4E5C", "#17171E", "#B8D8FF", "#C8E4FF", "#E89A20", "#FFD070"
    glowing = (lambda a: rgb(SPOT) if a.face in ("top", "nx", "px", "back") and hash01("spot", int(a.x / 1.4), int(a.y / 1.4), int(a.z / 1.4)) < 0.2 else None) if dusk else None

    def face_coat(a):
        if a.face == "front" and 0.3 < a.ly < 0.5 and abs(a.lx - 0.5) > 0.12: return rgb("#FFF4E0" if not dusk else "#8A8A9A")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        bone_of(m, "head").cubes.append(Cube(S([-0.8, p["hy"] + p["hh"] * 0.7, p["hz"] - p["hl"] - 0.03]), [n(1.6 * s), n(1.4 * s), 0],
                                             glow_patch(MARK, "diamond" if not dusk else "crescent")))
        m.pair("whiskers_right", "head", S([-1, p["hy"] + 0.8, p["hz"] - p["hl"] - p["snout"] + 0.6]),
               [Cube(S([-4.4, p["hy"] + 0.8, p["hz"] - p["hl"] - p["snout"]]), [n(3.4 * s), 0, n(1.6 * s)], whisker_mat("#F4F0E8" if not dusk else "#9A9AAA"))])
    tail = ringed(FUR, "#3A2416" if not dusk else SPOT, glow=SPOT if dusk else None)
    quad(sid, name, stands, dict(
        s=1.3, body=(4.4, 4, 11), top=4.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#FFF4E0" if not dusk else "#3E3E4A",
        coat=spot_coat(SPOT, 1.4, 0.2, faces=("top", "nx", "px", "back")), glow=glowing, FACE=FUR, face_coat=face_coat, head=(3.8, 3.4, 4),
        snout=2.2, snout_w=0.7, snout_h=1.6, MUZZLE=FUR_L if not dusk else FUR, NOSE="#2A1A14" if not dusk else "#0A0A10", neck=3, neck_up=0.6, neck_tilt=-14,
        ears=("rabbit", 2.4, 3.2), EAR=FUR, EAR_IN="#F0B0A0" if not dusk else "#5A4A5A", EAR_TIP=None if not dusk else SPOT,
        IRIS=IRIS, IRIS_L=IRIS_L, eye_grid=["LL", "IP", "II"], eye_h=3, eye_y=0.42, leg_w=1.4, toes=2, sharp=False,
        tail={"segs": [(1.6, 1.6, 3), (1.6, 1.6, 3), (1.5, 1.5, 3), (1.4, 1.4, 3), (1.3, 1.3, 2.6)], "rests": [8, 6, 2, -4, -6], "mat": tail, "y": 0.6},
        extra=extra, moves=(("QUICK ATTACK", "PSYCHIC") if not dusk else ("FAINT ATTACK", "CONFUSE RAY")), walk=0.5, stride=34, k=0.7))


def sungenet():
    genet("espeon", "SUNGENET", 0.9, False)


def duskgenet():
    genet("umbreon", "DUSKGENET", 1.0, True)


# ======================================================================================================
# The magpie: PILFERPIE (in place of Murkrow)
# ======================================================================================================

def pilferpie():
    """A magpie, a thief of anything that shines: black and white, its wings and long tail shot through with blue and
    green when the light catches them, a heavy black bill with a gold ring in it it's found somewhere."""
    def coat(at, base):
        if at.face in ("nx", "px") and -0.5 < at.z < 2.5 and at.y > 5: return rgb("#F4F4F0")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        ring = Mat("#F0C040", lambda a: rgb("#FFE88A") if a.face == "top" else None, glow=None, noise=0.01, edge=0)
        b = bone_of(m, "beak_upper")
        y, z = p["hy"] + p["hh"] * 0.32, p["hz"] - p["hl"] - 1.6
        b.cubes += [Cube(S([-0.3, y - 2.2, z - 0.8]), [1, n(2.4 * s), 0], ring), Cube(S([-0.3, y - 2.2, z + 0.8]), [1, n(2.4 * s), 0], ring),
                    Cube(S([-0.3, y - 2.4, z - 0.8]), [1, 0, n(1.6 * s)], ring)]
    avian("murkrow", "PILFERPIE", 0.5, dict(
        s=1.0, body=(5.4, 5.6, 8), leg=3, B="#16161C", B_L="#2E3038", B_D="#08080C", BELLY="#F4F4F0", coat=coat, head=(4.4, 4.2, 4.4), FACE="#16161C",
        neck_up=0.6, beak=2.6, beak_w=1.4, beak_h=1.4, BEAK="#141418", BEAK_D="#08080A", eye_w=2, eye_h=2, IRIS="#1A1210", IRIS_L="#4A4040", eye_in=2.2, eye_back=1.2,
        wing=9, WING="#1E3A5A", WING_D="#0A1420", WING_L="#2A8A8A", tail=9, TAIL="#1A4A40", TAIL_D="#0A1410", tail_w=2.4, tail_tilt=10, LEG="#1A1A1E",
        extra=extra, moves=("PECK", "FAINT ATTACK"), walk=0.45, k=0.7))


# ======================================================================================================
# The sage salamander: SAGEMANDER (in place of Slowking; from SLOWMANDER)
# ======================================================================================================

def sagemander():
    """The dozy salamander grown wise: standing tall and pink, feathery red gills framing its face like a ruff, heavy-
    lidded eyes that have seen everything, and on its crown, like a sage's cap, the conch snail clamped there, its
    spiralled shell banded violet and knobbed."""
    P, P_L, P_D, BELLY = "#EE9AAE", "#FFC4D0", "#C06A80", "#FFF0E2"
    gill = Mat("#E8506A", lambda a: CLEAR if (a.col % 2 == 1 and a.ly > 0.25) else mix("#B83A52", "#FF8AA0", a.ly), noise=0.0, edge=0, flat=True)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        hy, hh, hw, fz = p["hy"], p["hh"], p["hw"], p["fz"]
        for side in (-1, 1):
            for k, (dy, a) in enumerate(((hh - 1, -40), (hh * 0.6, -10), (hh * 0.2, 20))):
                x = side * hw / 2
                hd.cubes.append(Cube(S([x - (2.4 if side < 0 else 0), hy + dy - 0.8, fz + 2.4]), [n(2.4 * s), n(3.6 * s), 0], gill, [0, side * 30, side * a],
                                     S([x, hy + dy, fz + 2.4])))
        shell = conch_mat("#F2E4D6", "#9A6AB8", "#7A5A6A")
        ty = hy + hh - 0.4
        hd.cubes += blob(S([-3, ty, fz + 1]), S([6, 4.4, 5]), shell) + blob(S([-2, ty + 4, fz + 1.6]), S([4, 2.6, 3.6]), shell) + [Cube(S([-1, ty + 6.4, fz + 2.2]), [n(2 * s), n(2 * s), n(2 * s)], shell)]
        hd.cubes += spike(-3, ty + 2, fz + 3, 1.6, [0, -90, 0], taper_mat("#E8D8C8", "#FFFFFF"), 1) + spike(3, ty + 2, fz + 3, 1.6, [0, 90, 0], taper_mat("#E8D8C8", "#FFFFFF"), 1)
    upright("slowking", "SAGEMANDER", 2.0, dict(
        s=1.15, hip=6, body=(9, 10, 7), FUR=P, FUR_L=P_L, FUR_D=P_D, BELLY=BELLY, belly_size=(7, 8), FACE=None, head=(9, 4.6, 7), head_y=-0.4, nose=False,
        mouth_y=0.18, mouth_w=7, MUZZLE=BELLY, ears=None, IRIS="#3A2024", PUPIL="#120808", eye_in=1.6, eye_y=0.5, eye_grid=["DD", "PW"],
        arms=(3.5, 3.5, 2), HAND=P, fingers=3, leg_w=3, foot=5, FOOT=P, toes=4, thigh=4,
        tail={"segs": [(3.6, 3.2, 3), (3.1, 2.8, 3), (2.6, 2.4, 3), (2.1, 2, 3)], "rests": [-24, -6, 4, 6], "y": 0.6, "tip": BELLY},
        extra=extra, moves=("HEADBUTT", "PSYCHIC"), walk=0.9, stride=20, slow=1.4, guard=16, k=0.9))


# ======================================================================================================
# The ghostly bellflower: KEENBELL (in place of Misdreavus)
# ======================================================================================================

def keenbell():
    """A bellflower's ghost: a violet bell of a flower hanging from a curled stem, a pale face peering out of it, the
    bell's five lobes flared, a glowing pistil swinging below like a clapper, and wisps of tattered petal trailing after
    it as it drifts. When it rings, you hear it in your sleep."""
    m = Model()
    s = 1.0
    S = S_(s)
    V, V_L, V_D = "#7A5AC8", "#B49AF0", "#46307A"
    petal = Mat(V, lambda a: mix(V_D, V_L, 0.2 + 0.7 * a.ly) if (a.col + a.row) % 7 else mix(V_D, V, 0.5), noise=0.02)
    face = Mat(V, lambda a: rgb("#E8E0F8") if a.face == "front" and 0.12 < a.lx < 0.88 and a.ly < 0.8 else None, noise=0.02)
    lobe = Mat(V_L, lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * math.sin(math.pi * min(1, a.ly * 1.05)) ** 0.7 else mix(V, V_L, a.ly), noise=0.01, edge=0, flat=True)
    wisp = Mat("#C8B8F0", lambda a: CLEAR if abs(a.lx - 0.5) > 0.42 * (1 - a.ly) + 0.06 or (a.ly > 0.7 and a.col % 2) else mix("#9A84E0", "#E8E0FF", a.ly), noise=0, edge=0, flat=True)
    pist = Mat("#E8FFB0", glow=lambda a: rgb("#E8FFB0"), noise=0, edge=0)
    stem = plain("#4A7A3A")
    m.bone("root", None, [0, 0, 0])
    y0 = 9
    m.bone("body", "root", S([0, y0, 0]), blob(S([-3, y0 + 2, -3]), S([6, 3, 6]), petal) + [Cube(S([-0.5, y0 + 5, -0.5]), [1, n(3 * s), 1], stem),
                                                                                         Cube(S([-0.5, y0 + 7.4, -0.5]), [n(2.4 * s), 1, 1], stem)])
    m.bone("head", "body", S([0, y0, 0]), face_blob(S([-4, y0 - 3, -4]), S([8, 5.4, 8]), face))
    hd = bone_of(m, "head")
    for i in range(5):
        a = i * 72 + 36
        hd.cubes.append(Cube(S([-1.6, y0 - 6, 3.6]), [n(3.2 * s), n(3.4 * s), 0], lobe, [-200 + 160, a, 0], S([0, y0 - 3, 0])))
    m.bone("jaw", "head", S([0, y0 - 1.4, -4]), [Cube(S([-0.8, y0 - 2.0, -4.3]), [n(1.6 * s), 1, 1], Mat("#E8E0F8", open_mouth("#4A2A6A", "#E8E0F8"), noise=0.02))])
    m.eyes("head", x_inner=0.8 * s, y=(y0 - 0.6) * s, z=-4 * s, w=2, h=2, iris="#E03A5A", pupil="#1A0410", skin="#E8E0F8", iris_light="#FF8AA0", grid=["LL", "IP"])
    m.bone("clapper", "head", S([0, y0 - 3, 0]), [Cube(S([-0.4, y0 - 5.6, -0.4]), [1, n(2.6 * s), 1], plain("#C8E890")),
                                                  Cube(S([-0.9, y0 - 7, -0.9]), [n(1.8 * s), n(1.8 * s), n(1.8 * s)], pist)])
    tents = []
    for j, (x, z) in enumerate(((-2, 1.6), (2, 1.6), (0, 3))):
        prev, y = "head", y0 - 3
        names = []
        for i in range(3):
            nm = f"wisp{j}_{i}"
            m.bone(nm, prev, S([x, y, z]), [Cube(S([x - 1.2, y - 3, z]), [n(2.4 * s), n(3.2 * s), 0], wisp)], rotation=[18 if i == 0 else 10, 0, 0])
            names.append(nm)
            prev, y = nm, y - 3
        tents.append(names)
    fx_for(m, 0.7)
    moves(m, "ASTONISH", "CONFUSE RAY")
    A, info = floater("keenbell", m, tentacles=tents, k=0.8, bob=1.2, extras={"idle": {"clapper": track(rotation=[wave(1.4, 10), 0, wave(2.0, 8)])},
                                                                           "battle": {"clapper": track(rotation=[wave(0.6, 18), 0, wave(0.9, 10)])}})
    write("misdreavus", "KEENBELL", m, A, info, 0.7)


# ======================================================================================================
# The rune pebble: RUNELET (in place of Unown)
# ======================================================================================================

def runelet():
    """A flat slate pebble that drifts in the ruins on its own, a rune cut deep into its face and glowing faintly, two
    small carved slits of eyes above it. No two are cut alike."""
    m = Model()
    s = 1.0
    S = S_(s)
    SL, SL_L, SL_D = "#4A5A6A", "#7A8A9A", "#2A3440"

    def rune(a):
        # A glyph of strokes on the face: a stem, two arms and a hook.
        u, v = a.lx, a.ly
        stem = abs(u - 0.5) < 0.09 and 0.12 < v < 0.78
        arm1 = abs(v - (0.62 - (u - 0.5) * 0.8)) < 0.07 and 0.22 < u < 0.5
        arm2 = abs(v - (0.42 + (u - 0.5) * 0.8)) < 0.07 and 0.5 < u < 0.8
        hook = abs(v - 0.16) < 0.07 and 0.5 < u < 0.74
        return stem or arm1 or arm2 or hook
    slate = Mat(SL, lambda a: mix(SL_D, SL_L, 0.3 + 0.5 * cell(a.x, a.y, a.z, 1.6)), lambda a: rgb("#8AE8FF") if a.face == "front" and rune(a) else None,
                glow=lambda a: rgb("#8AE8FF") if a.face == "front" and rune(a) else None, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    y0 = 8
    m.bone("body", "root", S([0, y0, 0]), [])
    m.bone("head", "body", S([0, y0, 0]), blob(S([-3.4, y0 - 4.4, -1.4]), S([6.8, 8.8, 2.8]), slate))
    m.bone("jaw", "head", S([0, y0 - 3.6, -1.4]), [])
    m.eyes("head", x_inner=0.8 * s, y=(y0 + 3.0) * s, z=-1.4 * s, w=2, h=1, iris="#1A2028", pupil="#05070A", skin=SL, grid=["PP"])
    bone_of(m, "body").cubes.append(Cube(S([-0.5, y0 - 5.6, -0.5]), [1, 1, 1], Mat("#8AE8FF", glow=lambda a: rgb("#8AE8FF"), noise=0, edge=0)))
    fx_for(m, 0.6)
    moves(m, "HIDDEN POWER", "HIDDEN POWER")
    A, info = floater("runelet", m, k=0.6, bob=1.0, extras={"idle": {"head": track(rotation=[0, wave(4, 25), wave(3, 6)])}})
    write("unown", "RUNELET", m, A, info, 0.5)


# ======================================================================================================
# The blobfish: GLUMFISH (in place of Wobbuffet)
# ======================================================================================================

def glumfish():
    """A blobfish come up out of the deep and none too pleased about it: a great soft sagging sack of pale pink, a droopy
    nose hanging over a mouth turned down at the corners, small sorrowful eyes, two tiny fins it pats itself with and a
    little tail fin. Whatever you do to it, it gives straight back."""
    m = Model()
    s = 1.6
    S = S_(s)
    P, P_L, P_D = "#E8B4BE", "#FFD8DE", "#B8808C"
    skin = Mat(P, fur(P, P_L, P_D, speck=0.15), lambda a: mix(P_D, P, 0.6) if hash01("sag", round(a.x), round(a.y * 0.5)) < 0.08 else None, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 5, 0]), blob(S([-4.6, 0, -3.8]), S([9.2, 9, 7.6]), skin, b=2, flat_bottom=True) + blob(S([-5.2, 0, -4.2]), S([10.4, 3.4, 8.4]), skin, flat_bottom=True))
    m.bone("head", "body", S([0, 8, -3.8]), [Cube(S([-1.8, 3.4, -5.2]), [n(3.6 * s), n(2.8 * s), n(1.6 * s)], skin), Cube(S([-1.4, 2.6, -5.0]), [n(2.8 * s), 1, n(1.2 * s)], skin)])
    hd = bone_of(m, "head")
    hd.cubes.append(Cube(S([-2.4, 2.0, -3.95]), [n(1 * s), 1, 0], plain("#6A3A44")))
    hd.cubes.append(Cube(S([1.4, 2.0, -3.95]), [n(1 * s), 1, 0], plain("#6A3A44")))
    m.bone("jaw", "head", S([0, 2.2, -3.8]), [Cube(S([-1.6, 1.4, -4.1]), [n(3.2 * s), 1, 1], Mat("#C8909A", open_mouth("#7A2A3A", "#C8909A"), noise=0.02))])
    m.eyes("head", x_inner=1.6 * s, y=6.0 * s, z=-3.8 * s, w=2, h=2, iris="#1A0E10", pupil="#0A0405", skin=P, iris_light="#5A3A44", grid=["LL", "PI"])
    finm = fin_mat("#E8A8B4", "#FFD0D8", "#B8808C", "round")
    m.pair("arm_right", "body", S([-4.6, 4, -1]), [Cube(S([-4.7, 1.4, -1.6]), [0, n(2.6 * s), n(3 * s)], finm)], rotation=[0, 0, 30])
    m.bone("tail", "body", S([0, 3, 3.8]), [Cube(S([0, 1, 3.6]), [0, n(4 * s), n(3 * s)], finm)])
    fx_for(m, 0.8 * s)
    moves(m, "COUNTER", "MIRROR COAT")
    A, info = cocoon("glumfish", m, k=0.6 * s, extras={"idle": {"arm_right": track(rotation=[0, 0, wave(2.4, 10, 0, 30)]), "arm_left": track(rotation=[0, 0, wave(2.4, -10, 0, -30)]),
                                                                "head": track(rotation=[wave(3, 3), 0, 0])}})
    write("wobbuffet", "GLUMFISH", m, A, info, 1.3)


# ======================================================================================================
# The okapi: PSYKAPI (in place of Girafarig)
# ======================================================================================================

def psykapi():
    """An okapi of the deep forest: a velvet chocolate coat, legs and haunches striped boldly in cream and black like
    a zebra's, a pale face, two short furred horns, great soft ears, a long neck, violet eyes; and at the tip of its tail
    a tuft marked with two dark eye-spots, so it always seems to be watching behind it."""
    FUR, FUR_L, FUR_D = "#5A3424", "#7E4E36", "#36200F"

    def coat(at, base):
        if at.z > 1.4 and at.face in ("nx", "px", "back") and at.y < 13:
            return rgb("#F4ECDC") if int(at.y * 0.9) % 2 == 0 else rgb("#1A1210")
        return None
    legm = Mat(FUR, lambda a: (rgb("#F4ECDC") if int(a.y * 0.9) % 2 == 0 else rgb("#1A1210")) if a.y < 9 else mix(FUR_D, FUR, 0.6), noise=0.02)

    def face_coat(a):
        if a.face in ("front", "nx", "px") and a.ly < 0.7: return rgb("#E8D8C0")
        return None
    horn = Mat(FUR_D, lambda a: rgb("#1A1210") if a.ly > 0.7 else None, noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        for x in (-1.2, 1.2):
            hd.cubes.append(Cube(S([x - 0.5, p["hy"] + p["hh"] - 0.4, p["hz"] - p["hl"] * 0.4]), [1, n(2.4 * s), 1], horn, [-10, 0, 10 if x > 0 else -10],
                                 S([x, p["hy"] + p["hh"], p["hz"] - p["hl"] * 0.4])))
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        tuft = Mat("#2A1A12", lambda a: rgb("#8A3AC8") if a.face in ("back",) and abs(a.lx - 0.5) > 0.15 and 0.4 < a.ly < 0.75 else None,
                   glow=lambda a: rgb("#B070FF") if a.face == "back" and abs(a.lx - 0.5) > 0.15 and 0.5 < a.ly < 0.65 and abs(a.lx - 0.5) < 0.35 else None, noise=0.02)
        tb.cubes.append(Cube([-1.4 * s, tb.pivot[1] - 2.2 * s, z - 0.6], [n(2.8 * s), n(2.8 * s), n(1.6 * s)], tuft))
    quad("girafarig", "PSYKAPI", 1.5, dict(
        s=1.5, body=(5.6, 5.6, 10), top=7, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#4A2A1C", coat=coat, legm=legm, FACE="#E8D8C0", face_coat=face_coat,
        head=(3.8, 3.8, 4.4), snout=2.4, snout_w=0.8, snout_h=2.2, MUZZLE="#E8D8C0", NOSE="#2A1A14", neck=4.4, neck_up=5, neck_tilt=-6, head_tilt=14,
        ears=("rabbit", 2.4, 3.6), EAR=FUR, EAR_IN="#E8C8B0", IRIS="#8A3AC8", IRIS_L="#C89AF0", eye_grid=["LL", "IP"], eye_y=0.55, leg_w=1.6, toes=2,
        foot=Mat("#1A1210", noise=0.01), tail={"segs": [(1, 1, 2.6), (1, 1, 2.6)], "rests": [-40, -10], "y": 0.85}, extra=extra,
        moves=("STOMP", "PSYBEAM"), walk=0.55, stride=26, k=0.6))


# ======================================================================================================
# The pill bugs: BURRBALL > IRONBURR (in place of Pineco, Forretress)
# ======================================================================================================

def pill_bug(sid, name, stands, big):
    """A pill bug: a domed back of overlapping plates over many short legs, two long jointed feelers, a little face
    tucked under the front plate. The young one's plates are bark-brown and lichen-flecked, like the cone scales it hides
    among. Grown, plated in riveted iron edged with rust, and always rolled up tight into an armoured ball, its legs and
    face peeping out from the join."""
    m = Model()
    s = 1.0 if not big else 1.7
    S = S_(s)
    if not big:
        P, P_L, P_D, EDGE = "#7A5A3A", "#A88458", "#4A3420", "#C8B080"
        plate = Mat(P, lambda a: mix(P_D, P_L, 0.3 + 0.6 * a.ly) if a.face != "bottom" else rgb(P_D),
                    lambda a: rgb("#9AB87A") if hash01("lich", round(a.x), round(a.y), round(a.z)) < 0.08 and a.face == "top" else None,
                    lambda a: rgb(EDGE) if a.face in ("back",) else None, noise=0.02)
    else:
        P, P_L, P_D, EDGE = "#6A727E", "#A8B0BA", "#3A4048", "#B8642A"
        plate = Mat(P, lambda a: mix(P_D, P_L, 0.3 + 0.6 * a.ly) if a.face != "bottom" else rgb(P_D),
                    lambda a: rgb("#D8DEE6") if a.face == "top" and (a.col % 4 == 1) and (a.row % 3 == 1) else None,
                    lambda a: rgb(EDGE) if a.face in ("back",) or (a.face in ("nx", "px") and a.ly < 0.2) else None, noise=0.02)
    face = Mat("#5A4A40", noise=0.02)
    legm = plain("#3A2E26" if not big else "#2A2E34")
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 2.6, 0]), blob(S([-3.4, 0.8, -2.4]), S([6.8, 3.6, 3.4]), plate))
    m.bone("head", "body", S([0, 2, -2.4]), face_blob(S([-2.2, 0.6, -4.2]), S([4.4, 2.6, 2.2]), face))
    m.bone("jaw", "head", S([0, 1, -4.2]), [Cube(S([-0.8, 0.6, -4.4]), [n(1.6 * s), 1, 1], plain("#2A2020"))])
    m.eyes("head", x_inner=0.6 * s, y=1.8 * s, z=-4.2 * s, w=1, h=1, iris="#0A0A0A", pupil="#000000", skin="#5A4A40", grid=["P"])
    ant = Mat("#4A3A30" if not big else "#3A3E46", noise=0, edge=0)
    m.pair("antenna_right", "head", S([-1.4, 2, -4]), [Cube(S([-1.9, 2, -8.4]), [1, 1, n(4.4 * s)], ant, [-20, 25, 0], S([-1.4, 2, -4])),
                                                      Cube(S([-1.9, 2.6, -11]), [1, 1, n(2.6 * s)], ant, [-40, 35, 0], S([-1.4, 2, -4]))])
    segs, prev, z = [], "body", 1.0
    nseg = 6
    coil = ([0] * nseg) if not big else [44, 46, 46, 46, 44, 40]
    for i in range(nseg):
        w = 6.8 - (0.4 * max(0, i - 2))
        nm = f"seg{i + 1}"
        cubes = blob(S([-w / 2, 0.6, z - 0.2]), S([w, 3.8, 2.8]), plate) if w > 2 else [Cube(S([-w / 2, 0.6, z]), [n(w * s), n(3.4 * s), n(2.6 * s)], plate)]
        for x in (-w / 2 + 0.6, w / 2 - 1.6):
            cubes.append(Cube(S([x, 0, z + 0.6]), [1, 1, 1], legm))
        m.bone(nm, prev, S([0, 2.2, z]), cubes, rotation=[coil[i], 0, 0])
        segs.append(nm)
        prev, z = nm, z + 2.4 if not big else 2.2
    # The tail's two little prongs.
    bone_of(m, segs[-1]).cubes += [Cube(S([x - 0.5, 1, z + 0.6]), [1, 1, n(1.6 * s)], plate) for x in (-1.2, 1.2)]
    fx_for(m, 0.6 * s)
    moves(m, "TACKLE" if not big else "RAPID SPIN", "SELFDESTRUCT" if not big else "SPIKES")
    A, info = crawler(name.lower(), m, segs, walk=0.8, k=0.4 * s, slow=1.0 + 0.4 * big)
    write(sid, name, m, A, info, stands)


def burrball():
    pill_bug("pineco", "BURRBALL", 0.6, False)


def ironburr():
    pill_bug("forretress", "IRONBURR", 1.2, True)


# ======================================================================================================
# The slow worm: SLOWWYRM (in place of Dunsparce)
# ======================================================================================================

def slowwyrm():
    """A slow worm, the lizard with no legs: glossy bronze-gold and smooth as polished wood, a thin dark line down its
    back and bright sky-blue spots along it, a dark belly, a small blunt head with bright little eyes and a flickering
    tongue. In no hurry at all."""
    m = Model()
    s = 1.3
    S = S_(s)
    B, B_L, B_D, BELLY = "#B8862E", "#E8B85A", "#6A4818", "#3A3028"

    def skin_f(a):
        if a.face == "bottom":
            return rgb(BELLY)
        c = mix(B_D, B, 0.35 + 0.65 * a.ly)
        if a.face == "top":
            c = mix(B, B_L, 0.4)
            if abs(a.x) < 0.5:
                return rgb("#3A2410")
        if hash01("blue", round(a.x * 1.2), round(a.z * 1.2)) < 0.07 and a.face in ("top", "nx", "px"):
            return rgb("#4AA0F0")
        if (a.z * 2) % 1.0 < 0.12:
            c = mix(c, B_D, 0.25)
        return c
    skin = Mat(B, skin_f, noise=0.012)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 1.4, -1]), [Cube(S([-1.6, 0, -2.4]), [n(3.2 * s), n(2.8 * s), n(4.2 * s)], skin)])
    segs, prev, z = [], "body", 1.6
    nseg = 8
    for i in range(nseg):
        t = i / (nseg - 1)
        w, h = (3.0 - 1.6 * t ** 1.5) * s, (2.7 - 1.4 * t ** 1.5) * s
        nm = f"seg{i + 1}"
        m.bone(nm, prev, [0, 1.2 * s, z * s], [Cube([-w / 2, 0, (z - 0.2) * s], [n(w), n(h), n((2.6 + 0.4) * s)], skin)])
        segs.append(nm)
        prev, z = nm, z + 2.6
    m.bone("neck", "body", S([0, 1.4, -2.4]), [Cube(S([-1.5, 0.1, -5.6]), [n(3 * s), n(2.8 * s), n(3.6 * s)], skin)], rotation=[-30, 0, 0])
    m.bone("neck2", "neck", S([0, 1.6, -5.4]), [Cube(S([-1.4, 0.2, -8]), [n(2.8 * s), n(2.6 * s), n(2.8 * s)], skin)], rotation=[-10, 0, 0])
    hz = -7.8
    m.bone("head", "neck2", S([0, 1.8, hz]), face_blob(S([-1.7, 0.4, hz - 3]), S([3.4, 2.6, 3.2]), skin), rotation=[36, 0, 0])
    m.bone("jaw", "head", S([0, 0.8, hz - 1]), [Cube(S([-1.4, 0.0, hz - 2.9]), [n(2.8 * s), 1, n(2.8 * s)], Mat("#C8A070", open_mouth("#B84A4A", "#C8A070"), noise=0.02))])
    tongue = Mat("#D8304A", lambda a: CLEAR if (a.lz < 0.3 and abs(a.lx - 0.5) < 0.2) else None, noise=0, edge=0, flat=True)
    m.bone("tongue", "jaw", S([0, 0.8, hz - 2.8]), [Cube(S([-0.5, 0.8, hz - 5.2]), [n(s), 0, n(2.4 * s)], tongue)])
    m.eyes("head", 1.7 * s, 1.8 * s, (hz - 3) * s, h=1, corner=(1, 1), half=1.7 * s, iris="#E8A030", pupil="#140C04", skin=B, iris_light="#FFE07A", grid=["IPI"[:2]])
    fx_for(m, 0.7 * s)
    moves(m, "GLARE", "DEFENSE CURL")
    coil = [26, 28, 28, 26, 24, 20, 16, 10]
    A, info = serpent("slowwyrm", m, segs, k=0.6 * s, slow=1.4, coil=coil)
    write("dunsparce", "SLOWWYRM", m, A, info, 1.5)


# ======================================================================================================
# The flying lizard: DUNEGLIDE (in place of Gligar)
# ======================================================================================================

def duneglide():
    """A flying lizard of the dunes: slim and sand-coloured, banded violet across its back, a long whip of a tail, a
    bright yellow dewlap it flicks out under its chin, and along each side a gliding wing of skin stretched over long
    ribs, sunset-orange barred with black, that it spreads to sail from rock to rock."""
    FUR, FUR_L, FUR_D = "#C8A878", "#E8D0A0", "#8A6A48"

    def coat(at, base):
        if at.face in ("top", "nx", "px") and int(at.z * 0.6) % 2 == 0 and at.y > 3.4: return rgb("#7A5A9A")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        def glide(a):
            u, v = (abs(a.x) - 2 * s) / (6 * s), a.lz
            if u < 0 or u > math.sin(math.pi * min(1, max(0, v))) ** 0.6:
                return CLEAR
            if int(v * 9) % 2 == 0:
                return rgb("#1A1210") if u > 0.3 else rgb("#C85A1A")
            return mix("#F08A2A", "#FFC060", u)
        gm = Mat("#F08A2A", glide, noise=0.01, edge=0, flat=True)
        m.pair("wing_right", "body", S([-2.1, p["top"] + p["bh"] - 0.6, 0]), [Cube(S([-8, p["top"] + p["bh"] - 0.6, -3.6]), [n(6 * s), 0, n(7.2 * s)], gm)], rotation=[0, 0, 10])
        bone_of(m, "head").cubes.append(Cube(S([-0.1, p["hy"] - 1.8, p["hz"] - p["hl"] + 0.4]), [0, n(2 * s), n(2.4 * s)],
                                             Mat("#F8D840", lambda a: CLEAR if (a.ly - 1) ** 2 + (a.lz - 0.5) ** 2 * 2 > 1 else None, noise=0, edge=0, flat=True)))
    flap = {"idle": {"wing_right": track(rotation=[0, 0, wave(3, 6, 0, 10)]), "wing_left": track(rotation=[0, 0, wave(3, -6, 0, -10)])},
            "walk": {"wing_right": track(rotation=[0, 0, wave(0.6, 14, 0, 4)]), "wing_left": track(rotation=[0, 0, wave(0.6, -14, 0, -4)])},
            "battle": {"wing_right": track(rotation=[0, 0, wave(0.8, 10, 0, -4)]), "wing_left": track(rotation=[0, 0, wave(0.8, -10, 0, 4)])}}
    quad("gligar", "DUNEGLIDE", 1.1, dict(
        s=1.3, body=(4, 3, 9), top=2.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#F0E0C0", coat=coat, FACE=FUR, head=(3.4, 2.8, 3.6), snout=1.8, snout_w=0.8,
        snout_h=1.6, MUZZLE=FUR, NOSE="#5A4030", neck=2, neck_up=0.4, neck_tilt=-16, head_tilt=8, ears=None, IRIS="#C8601A", IRIS_L="#FFA050",
        eye_grid=["IP"], eye_h=1, eye_y=0.62, leg_w=1.2, toes=3, sharp=True, foot=Mat(FUR_D, noise=0.01),
        tail={"segs": [(1.6, 1.4, 3), (1.3, 1.1, 3), (1, 1, 3), (1, 1, 3), (1, 1, 2.4)], "rests": [-4, 2, 4, 6, 8], "y": 0.4},
        extra=extra, moves=("POISON STING", "SAND-ATTACK"), walk=0.4, stride=36, k=0.5, anim_extras=flap))


# ======================================================================================================
# The iron wyrm: IRONWYRM (in place of Steelix; from BASALWYRM)
# ======================================================================================================

def ironwyrm():
    """The basalt wyrm grown old and pressed to iron deep under the mountains: a vast chain of hexagonal columns of dark
    burnished steel, a crest of steel blades along its back, a heavy jaw like an anvil, glowing coal-red eyes."""
    m = Model()
    s = 2.4
    steel = basalt_mat("#6A727E", "#B8C0CA", "#3A4048", "#D8E0E8")
    spikem = taper_mat("#4A505A", "#E8EEF4")
    tooth = Mat("#D8DCE0", noise=0.02, edge=0)
    glow_e = Mat("#FF5A2A", glow=lambda a: rgb("#FF8A4A"), noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 3 * s, -2 * s], hex_drum(0, 3 * s, -3 * s, 6.8 * s, 4 * s, steel))
    segs, prev, z = [], "body", 1.0
    nseg = 11
    for i in range(nseg):
        t = i / (nseg - 1)
        w = (6.6 - 3.0 * t) * s
        nm = f"seg{i + 1}"
        cubes = hex_drum(0, w / 2 + 0.2, (z - 0.3) * s, w, 3.0 * s, steel) + spike(0, w * 0.95, (z + 1.4) * s, (3.2 - 1.6 * t) * s, [50, 0, 0], spikem, 2 * s)
        if i % 2 == 0:
            cubes += spike(-w * 0.45, w * 0.75, (z + 1.4) * s, (2 - t) * s, [40, 0, 40], spikem, 1.6 * s) + spike(w * 0.45, w * 0.75, (z + 1.4) * s, (2 - t) * s, [40, 0, -40], spikem, 1.6 * s)
        if i == nseg - 1:
            cubes += spike(0, w / 2, (z + 2.6) * s, 5 * s, [0, 0, 0], spikem, 2.4 * s)
        m.bone(nm, prev, [0, w / 2 + 0.2, z * s], cubes)
        segs.append(nm)
        prev, z = nm, z + 3.0
    m.bone("neck", "body", [0, 3 * s, -3 * s], hex_drum(0, 3 * s, -7 * s, 6 * s, 4.4 * s, steel), rotation=[-62, 0, 0])
    m.bone("neck2", "neck", [0, 3 * s, -7 * s], hex_drum(0, 3 * s, -11 * s, 5.4 * s, 4.4 * s, steel), rotation=[-14, 0, 0])
    hz = -11
    m.bone("head", "neck2", [0, 3 * s, hz * s], face_blob([-3.8 * s, 1.4 * s, (hz - 6.4) * s], [n(7.6 * s), n(5.2 * s), n(6.8 * s)], steel) +
           [Cube([-3 * s, 1.0 * s, (hz - 9) * s], [n(6 * s), n(3.2 * s), n(2.8 * s)], steel)] +
           spike(-2.8 * s, 5.8 * s, (hz - 1) * s, 6 * s, [24, 24, 0], spikem, 2 * s) + spike(2.8 * s, 5.8 * s, (hz - 1) * s, 6 * s, [24, -24, 0], spikem, 2 * s) +
           spike(0, 6.4 * s, (hz - 3) * s, 3.4 * s, [60, 0, 0], spikem, 2 * s), rotation=[76, 0, 0])
    m.bone("jaw", "head", [0, 1.6 * s, (hz - 2) * s], [Cube([-3.2 * s, 0.2 * s, (hz - 8.8) * s], [n(6.4 * s), n(1.8 * s), n(6.8 * s)], steel)] +
           [Cube([(-2.8 + i * 1.1) * s, 1.8 * s, (hz - 8.6) * s], [n(0.8 * s), n(1.2 * s), 1], tooth) for i in range(6)])
    hd = bone_of(m, "head")
    hd.cubes += [Cube([-3.2 * s, 4.2 * s, (hz - 6.45) * s], [n(2 * s), n(1 * s), 0], glow_e), Cube([1.2 * s, 4.2 * s, (hz - 6.45) * s], [n(2 * s), n(1 * s), 0], glow_e)]
    m.bone("eyelid_right", "head", [0, 4 * s, (hz - 6) * s], [])
    fx_for(m, 1.8)
    moves(m, "IRON TAIL", "ROCK SLIDE")
    coil = [34, 36, 36, 36, 36, 34, 32, 30, 26, 22, 18]
    A, info = serpent("ironwyrm", m, segs, k=1.6, slow=1.5, coil=coil)
    write("steelix", "IRONWYRM", m, A, info, 3.0)


# ======================================================================================================
# The warthogs: SNORTLET > BRISTLEHOG (in place of Snubbull, Granbull)
# ======================================================================================================

def warthog(sid, name, stands, big):
    """A warthog: a barrel of a body on short legs, soft pink-grey skin, a long flat face with a broad pink disc of a
    nose, warty bumps along its cheeks, curving ivory tusks, little pointed ears, a mane of lilac bristles down its neck
    and back, and a thin tail it carries straight up like an aerial when it trots. The piglet's tusks are only nubs."""
    s = 1.0 if not big else 1.6
    FUR, FUR_L, FUR_D = "#C8A0AC", "#E8C8D0", "#8A6A76"
    mane = Mat("#8A6AA0", brush("#8A6AA0", "#C8A8E0", "#5A4070"), noise=0.0, edge=0)
    tusk = Mat("#F8F0DC", lambda a: rgb("#D8C8A8") if a.ly < 0.3 else None, noise=0.01, edge=0)
    wart = Mat("#B08894", noise=0.02)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        back_row(p, mane, 6 if big else 4, 2.6 if big else 1.8, w=1, lean=-30, z_from=0.2, z_to=-0.9, xs=(0,), grow=0.2)
        nb = bone_of(m, "neck")
        nb.cubes.append(Cube(S([0, p["hy"] + 1.2, p["hz"] - 0.4]), [0, n((3 if big else 2) * s_), n(4 * s_)], mane, [-30, 0, 0], S([0, p["hy"] + 1.2, p["hz"]])))
        hd = bone_of(m, "head")
        y, zf = p["hy"], p["hz"] - p["hl"]
        L = 3.6 if big else 1.0
        for x in (-1.8, 1.8):
            hd.cubes.append(Cube(S([x - 0.5, y + 0.6, zf - p["snout"] + 0.6]), [1, n(L * s_), 1], tusk, [-30, 0, 40 if x > 0 else -40], S([x, y + 0.6, zf - p["snout"] + 0.8])))
            hd.cubes += blob(S([x * 1.25 - 0.6, y + 1.6, zf + 0.6]), S([1.2, 1.2, 1.4]), wart) if big else []
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        tb.cubes.append(Cube([-0.6 * s_, tb.pivot[1] - 0.4, z - 0.4], [n(1.2 * s_), n(2.4 * s_), n(1.2 * s_)], mane))
    quad(sid, name, stands, dict(
        s=s, body=(6.4, 5.4, 9), top=3.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#D8B8C0", FACE=FUR, head=(5, 4.4, 4.4), snout=3, snout_w=1.1, snout_h=2.4,
        MUZZLE=FUR_L, NOSE="#E87A9A", neck=2, neck_up=-0.6, neck_tilt=-4, head_tilt=16, ears=("fennec", 1.6, 2.4), EAR=FUR, EAR_IN="#F0A8B8", ear_rot=[-6, 20, -30],
        IRIS="#3A1A24", IRIS_L="#7A4A5A", eye_grid=["LL", "IP"], eye_y=0.62, leg_w=1.8, toes=2, foot=Mat("#4A3A3E", noise=0.01),
        tail={"segs": [(0.8, 0.8, 2), (0.8, 0.8, 2)], "rests": [70, 10], "y": 0.85}, extra=extra,
        moves=(("TACKLE", "CHARM") if not big else ("BITE", "ROAR")), walk=0.45, stride=26, k=0.7))


def snortlet():
    warthog("snubbull", "SNORTLET", 0.6, False)


def bristlehog():
    warthog("granbull", "BRISTLEHOG", 1.4, True)


# ======================================================================================================
# The lionfish: LIONFRILL (in place of Qwilfish)
# ======================================================================================================

def lionfrill():
    """A lionfish: a body banded red and cream, fanned fins like a mane spread out round it, and along its back a spray
    of long venomous spines, each striped; feathery tassels over its eyes."""
    def paint(a):
        if a.face == "bottom": return None
        return rgb("#F4E8D8") if int(a.z * 0.9) % 2 == 0 else rgb("#C83A2A")

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        sp = Mat("#F4E8D8", lambda a: rgb("#C83A2A") if int(a.ly * 6) % 2 else None, noise=0, edge=0)
        b = bone_of(m, "body")
        for i, z in enumerate((-2.6, -1.4, -0.2, 1.0, 2.2)):
            h = 5.6 - abs(i - 1.4) * 0.6
            b.cubes.append(Cube(S([-0.5, p["y0"] + p["bh"] / 2 - 0.4, z]), [1, n(h * s), 1], sp, [-20 + i * 6, 0, 0], S([0, p["y0"] + p["bh"] / 2 - 0.4, z])))
        fan = Mat("#F4C8B0", lambda a: CLEAR if (a.lz > 0.9 and a.col % 2) or abs(a.ly - 0.5) > 0.5 * math.sin(math.pi * min(1, a.lz * 1.1 + 0.1)) ** 0.5
                  else (rgb("#C83A2A") if int(a.lz * 8) % 2 else rgb("#F4E8D8")), noise=0, edge=0, flat=True)
        for side in ("wing_right", "wing_left"):
            bb = bone_of(m, side)
            c = bb.cubes[0]
            bb.cubes[0] = Cube(c.origin, [0, n(6 * s), n(6.4 * s)], fan)
        hd = bone_of(m, "head")
        tass = Mat("#F4E8D8", lambda a: CLEAR if a.col % 2 else None, noise=0, edge=0, flat=True)
        hd.cubes += [Cube(S([x - 0.8, p["y0"] + p["hh"] / 2 - 0.2, p["hf"] + 0.8]), [n(1.6 * s), n(1.8 * s), 0], tass) for x in (-1.2, 1.2)]
    fish("qwilfish", "LIONFRILL", 0.5, dict(
        s=1.0, y=7, body=(3.6, 5, 8), B="#C83A2A", B_L="#F4E8D8", B_D="#8A1E14", BELLY="#F4E8D8", paint=paint, head=(3.4, 4.2, 3.4), IRIS="#E8C030", IRIS_L="#FFF0A0",
        eye_grid=["WI", "PI"], FIN="#F4C8B0", FIN_L="#FFE8D8", FIN_D="#C83A2A", tail_fin=("round", 6, 4), dorsal=(2.6, 3, "sail"), dorsal_z=2.4,
        pecs=(6, 6.4, 50), flap=1.2, flap_amp=12, extra=extra, moves=("POISON STING", "SPIKES"), k=0.7))


# ======================================================================================================
# The crimson pincer beetle: VERMICLAW (in place of Scizor)
# ======================================================================================================

def vermiclaw():
    """A beetle in crimson armour bright as polished metal, standing tall: a narrow waist, a smooth shell of wing cases
    over thin buzzing wings, golden compound eyes, and in place of hands two great pincers like a pseudoscorpion's, each
    with a dark spot on it like an eye to put off whatever it faces."""
    R, R_L, R_D = "#C8282A", "#FF7A6A", "#6A0A14"
    shellm = Mat(R, lambda a: mix(R_D, R_L, 0.25 + 0.7 * a.ly) if a.face != "top" else mix(R, R_L, 0.6),
                 lambda a: rgb("#FFC8B8") if a.face in ("front", "nx", "px") and a.ly > 0.82 and a.col % 3 == 0 else None, noise=0.015)
    joint = plain("#3A3A42")
    claw = Mat(R, lambda a: mix(R_D, R_L, 0.3 + 0.6 * a.ly), lambda a: rgb("#1A1A20") if a.face in ("nx", "px") and abs(a.lx - 0.5) < 0.2 and abs(a.ly - 0.5) < 0.22 else None,
               noise=0.015)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        for side in ("forearm_right", "forearm_left"):
            b = bone_of(m, side)
            hand = b.cubes[-1]
            x0, y0, z0 = hand.origin
            w = hand.size[0]
            b.cubes[-1] = Cube([x0 - 0.3, y0 - 0.4, z0 - 0.6], [w + 0.6, n(2.4 * s), n(2.4 * s)], claw)
            b.cubes.append(Cube([x0, y0 + 0.8 * s, z0 - 4.6 * s], [w, n(1.2 * s), n(4.6 * s)], claw, [-10, 0, 0], [x0, y0 + 1.4 * s, z0]))
            b.cubes.append(Cube([x0 + 0.2, y0 - 0.4, z0 - 3.8 * s], [max(1, w - 0.4), n(1 * s), n(3.8 * s)], claw, [26, 0, 0], [x0, y0, z0]))
        bd = bone_of(m, "body")
        top = p["hip"] + p["bh"]
        cas = Mat(R, lambda a: mix(R_D, R_L, 0.4 + 0.5 * a.ly), lambda a: rgb("#1A0A0E") if a.col == 0 else None, noise=0.015)
        bd.cubes += [Cube(S([-3.4, p["hip"] + 1, p["bd"] / 2 - 0.2]), [n(3.3 * s), n((p["bh"] - 1) * s), 1], cas, [8, -6, 0], S([-1.7, top, p["bd"] / 2])),
                     Cube(S([0.1, p["hip"] + 1, p["bd"] / 2 - 0.2]), [n(3.3 * s), n((p["bh"] - 1) * s), 1], cas, [8, 6, 0], S([1.7, top, p["bd"] / 2]))]
        hd = bone_of(m, "head")
        ant = plain("#3A1A1E")
        hd.cubes += [Cube(S([x - 0.5, p["hy"] + p["hh"] - 0.4, p["fz"] + 0.6]), [1, n(3.6 * s), 1], ant, [-30, 0, 20 if x > 0 else -20], S([x, p["hy"] + p["hh"], p["fz"] + 0.6]))
                     for x in (-1, 1)]
    upright("scizor", "VERMICLAW", 1.8, dict(
        s=1.5, hip=6, body=(6, 7, 4.6), FUR=R, FUR_L=R_L, FUR_D=R_D, furm=shellm, BELLY="#3A3A42", belly_size=(2.4, 5.4), FACE=None, head=(4.4, 3.6, 3.6),
        head_y=0.2, headm=shellm, nose=False, mouth_y=0.14, mouth_w=2, MUZZLE="#3A3A42", ears=None, IRIS="#E8B020", IRIS_L="#FFE890", PUPIL="#3A2A04",
        EYE_SKIN=R, eye_in=0.6, eye_w=2, eye_h=2, eye_y=0.5, eye_grid=["WI", "IJ"], arms=(3, 3.4, 1.6), armm=shellm, leg_w=1.4, foot=3, legm=shellm,
        FOOT="#3A3A42", toes=2, sharp=True, thigh=2.2, extra=extra, moves=("METAL CLAW", "AGILITY"), walk=0.6, guard=50, k=0.8))


# ======================================================================================================
# The caddis grub: JARBUG (in place of Shuckle)
# ======================================================================================================

def jarbug():
    """A caddis grub, a homebody: it lives in a tube it builds of pebbles cemented together with silk, studded with the
    red berries it squirrels away there to ferment; only its striped head and three pairs of legs ever come out."""
    m = Model()
    s = 1.2
    S = S_(s)
    peb = Mat("#8A8478", lambda a: mix("#5A564E", "#C8C0B0", hash01("peb", int(a.x / 1.3), int(a.y / 1.3), int(a.z / 1.3))), noise=0.02)
    berry = Mat("#D8303A", lambda a: rgb("#FF8A8A") if a.face == "top" and a.col == 0 else None, noise=0.01, edge=0)
    head = Mat("#D8B878", lambda a: rgb("#5A3A1A") if a.face == "top" and int(a.lx * 5) % 2 == 0 else None, noise=0.02)
    legm = plain("#B89858")
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 3, 0]), blob(S([-3, 0.6, -3]), S([6, 5, 6]), peb))
    m.bone("head", "body", S([0, 2.6, -3]), face_blob(S([-2.2, 1.2, -5.6]), S([4.4, 3.4, 3]), head))
    m.bone("jaw", "head", S([0, 1.6, -5.4]), [Cube(S([-1, 1, -5.9]), [n(2 * s), 1, 1], plain("#3A2410"))])
    m.eyes("head", x_inner=0.7 * s, y=2.8 * s, z=-5.6 * s, w=1, h=1, iris="#0A0A0A", pupil="#000000", skin="#D8B878", grid=["P"])
    for i, z in enumerate((-4.4, -3.6, -2.8)):
        legs_pair(m, f"leg_{i + 1}_right", "head", 1.8 * s, 1.6 * s, z * s, round(2 * s), legm, plain("#5A3A1A"), ang=-30 + i * 10)
    segs, prev, z = [], "body", 3
    for i in range(3):
        w = 6 - i * 0.8
        nm = f"seg{i + 1}"
        cubes = blob(S([-w / 2, 0.6 + i * 0.3, z - 0.3]), S([w, w - 1, 3.4]), peb)
        if i < 2:
            cubes += [Cube(S([x, 0.6 + w - 1.2, z + 0.8]), [n(1.6 * s), n(1.6 * s), n(1.6 * s)], berry) for x in ((-w / 2 + 0.6, w / 2 - 2.2) if i == 0 else (-0.8,))]
        m.bone(nm, prev, S([0, 3, z]), cubes)
        segs.append(nm)
        prev, z = nm, z + 3
    bone_of(m, "body").cubes += [Cube(S([1.2, 5.2, -1.6]), [n(1.6 * s), n(1.6 * s), n(1.6 * s)], berry), Cube(S([-2.6, 4.8, 0.6]), [n(1.6 * s), n(1.6 * s), n(1.6 * s)], berry)]
    fx_for(m, 0.6 * s)
    moves(m, "WRAP", "WITHDRAW")
    A, info = crawler("jarbug", m, segs, walk=1.2, k=0.3, slow=1.6)
    write("shuckle", "JARBUG", m, A, info, 0.6)


# ======================================================================================================
# The stag beetle: STAGBRAWL (in place of Heracross)
# ======================================================================================================

def stagbrawl():
    """A stag beetle reared up to fight: glossy chestnut wing cases over a black body, a broad head and on it great
    branching jaws like a stag's antlers, strong arms with hooked claws for grappling, and short sturdy legs."""
    BLK, BLK_L, BLK_D = "#2A2026", "#5A4A52", "#120C10"
    shellm = Mat(BLK, lambda a: mix(BLK_D, BLK_L, 0.2 + 0.7 * a.ly), noise=0.015)
    jaw = Mat("#8A3A1E", lambda a: mix("#4A1A0E", "#C8683A", a.ly), noise=0.015)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        y, fz = p["hy"] + 0.8, p["fz"]
        for x in (-1.8, 1.8):
            sg = 1 if x > 0 else -1
            piv = S([x, y, fz])
            # The beam: out and forward, rising a little; a tine standing up off it; its tip hooking in.
            hd.cubes += [Cube(S([x - 0.6, y - 0.6, fz - 7]), [n(1.2 * s), n(1.2 * s), n(7.2 * s)], jaw, [-8, sg * 18, 0], piv),
                         Cube(S([x + sg * 1.7 - 0.5, y, fz - 4.6]), [1, n(2.6 * s), 1], jaw, [-20, 0, -sg * 10], S([x + sg * 1.7, y, fz - 4.6])),
                         Cube(S([x + sg * 2.2 - 0.5, y + 0.6, fz - 7.8]), [1, 1, n(2.4 * s)], jaw, [0, -sg * 60, 0], S([x + sg * 2.2, y + 0.6, fz - 6.6]))]
        bd = bone_of(m, "body")
        cas = Mat("#8A3A1E", lambda a: mix("#4A1A0E", "#D07040", 0.3 + 0.6 * a.ly), lambda a: rgb("#2A0E08") if a.col == 0 else None, noise=0.015)
        top = p["hip"] + p["bh"]
        bd.cubes += [Cube(S([-p["bw"] / 2 - 0.2, p["hip"], p["bd"] / 2 - 0.6]), [n((p["bw"] / 2 + 0.1) * s), n((p["bh"] - 0.4) * s), n(1.4 * s)], cas, [10, -4, 0], S([0, top, p["bd"] / 2])),
                     Cube(S([0.1, p["hip"], p["bd"] / 2 - 0.6]), [n((p["bw"] / 2 + 0.1) * s), n((p["bh"] - 0.4) * s), n(1.4 * s)], cas, [10, 4, 0], S([0, top, p["bd"] / 2]))]
    upright("heracross", "STAGBRAWL", 1.5, dict(
        s=1.4, hip=5, body=(8, 8, 5), FUR=BLK, FUR_L=BLK_L, FUR_D=BLK_D, furm=shellm, BELLY="#3A3038", belly_size=(5, 6), FACE=None, head=(6.4, 3.6, 4.4),
        head_y=-0.4, headm=shellm, nose=False, mouth_y=0.1, mouth_w=3, MUZZLE="#3A3038", ears=None, IRIS="#E8A020", IRIS_L="#FFD070", EYE_SKIN=BLK,
        eye_in=1.6, eye_w=2, eye_h=2, eye_y=0.46, eye_grid=["WI", "IJ"], arms=(3.4, 3.4, 2.4), armm=shellm, HAND=BLK_D, claws=3, leg_w=2, foot=3, legm=shellm,
        FOOT=BLK_D, toes=2, sharp=True, thigh=3, extra=extra, moves=("HORN ATTACK", "REVERSAL"), walk=0.6, guard=50, k=0.9))


# ======================================================================================================
# The wolverine: RIMECLAW (in place of Sneasel)
# ======================================================================================================

def rimeclaw():
    """A young wolverine of the snowfields: dark chocolate fur, a frost-pale band running along each flank from shoulder
    to tail, a silvered brow, small round ears, a bushy tail, and long hooked claws gone blue as ice."""
    FUR, FUR_L, FUR_D = "#3A2A22", "#5E483A", "#1E1612"

    def coat(at, base):
        if at.face in ("nx", "px") and 5.6 < at.y < 7.2: return rgb("#D8E4F0")
        return None

    def face_coat(a):
        if a.face in ("front", "top") and a.ly > 0.55: return rgb("#A8B0B8")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        for leg in ("leg_front_right", "leg_front_left"):
            b = bone_of(m, leg)
            c = b.cubes[0]
            for i in range(3):
                b.cubes.append(Cube([c.origin[0] + i * 0.7, 0, c.origin[2] - 1.6 * s], [1, 1, n(1.8 * s)], Mat("#A8E0FF", glow=lambda a: rgb("#C8F0FF"), noise=0, edge=0)))
    quad("sneasel", "RIMECLAW", 0.9, dict(
        s=1.2, body=(5.6, 4.4, 9), top=3.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#2A1E18", coat=coat, FACE=FUR, face_coat=face_coat, head=(4.4, 3.8, 4),
        snout=1.6, snout_w=0.9, snout_h=1.8, MUZZLE="#2A1E18", NOSE="#0A0606", neck=2.4, neck_up=0, neck_tilt=-10, ears=("round", 1.6, 1.6), EAR=FUR,
        EAR_IN="#8A7A6A", IRIS="#C83A2A", IRIS_L="#FF8A5A", eye_grid=["LL", "IP"], eye_y=0.55, leg_w=2, toes=3, sharp=True,
        tail={"segs": [(2.4, 2.4, 2.4), (2.6, 2.6, 2.4), (2, 2, 2)], "rests": [20, 0, -10], "y": 0.75}, extra=extra,
        moves=("QUICK ATTACK", "ICY WIND"), walk=0.45, stride=30, k=0.7))


# ======================================================================================================
# The spectacled bears: SPECTACUB > SPECTABEAR (in place of Teddiursa, Ursaring)
# ======================================================================================================

def spectacled(sid, name, stands, big):
    """A spectacled bear of the cloud forest: shaggy black fur, and round its eyes, down its muzzle and across its chest
    cream markings like a pair of spectacles and a bib. The cub sits up licking honey off the honeycomb in its paw;
    grown, a great bear on its hind legs, its bib a broad pale crescent, its claws long."""
    s = 1.0 if not big else 2.0
    FUR, FUR_L, FUR_D = "#2A2422", "#4A403C", "#141010"
    CREAM = "#F0DCA8"

    def face_coat(a):
        if a.face != "front":
            return None
        u, v = a.lx, a.ly
        for cx in (0.3, 0.7):
            r = math.hypot((u - cx) * 1.2, v - 0.5)
            if 0.12 < r < 0.24:
                return rgb(CREAM)
        if v < 0.3 and abs(u - 0.5) < 0.22:
            return rgb(CREAM)
        return None

    def bib(a):
        u, v = a.lx, a.ly
        if abs(u - 0.5) > 0.45 * v + 0.1 or v < 0.35:
            return rgb(FUR)
        return None

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        if not big:
            comb = Mat("#F0B030", lambda a: rgb("#C88018") if (a.row + a.col * 2) % 3 == 0 else None, glow=None, noise=0.01)
            b = bone_of(m, "forearm_right")
            hand = b.cubes[-1]
            b.cubes.append(Cube([hand.origin[0] - 0.4, hand.origin[1] - 1.6, hand.origin[2] - 1.6], [n(2.6 * s_), n(2.2 * s_), n(1.4 * s_)], comb))
    upright(sid, name, stands, dict(
        s=s, hip=3 if not big else 4, body=(7, 7, 6) if not big else (9, 9.6, 7), FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=CREAM, belly_paint=bib,
        belly_size=(5, 4.4) if not big else (6.6, 6), belly_y=(2.4 if not big else 3.4), FACE=None, face_coat=face_coat, head=(6, 5.4, 5) if not big else (7, 6, 6),
        head_y=-0.4, snout=(1.8, 2.8, 2.2), NOSE="#141010", MUZZLE=CREAM, ears=("round", 2.2, 2.2), EAR=FUR, EAR_IN="#4A3A36", ear_x=2.6 if not big else 3,
        IRIS="#2A1A10", PUPIL="#050302", EYE_SKIN=FUR, eye_in=0.9, eye_y=0.5, eye_grid=["PW", "PP"] if not big else ["IP"], eye_h=2 if not big else 1,
        arms=(2.4, 2.4, 2.2) if not big else (3.4, 3.4, 3), HAND=FUR_D, claws=3 if big else 0, leg_w=2.4 if not big else 3.4, foot=3 if not big else 4.4,
        FOOT=FUR_D, thigh=3 if not big else 4.4, extra=extra, moves=(("SCRATCH", "SWEET SCENT") if not big else ("SLASH", "SCARY FACE")),
        walk=0.6 if not big else 0.8, guard=30 if not big else 50, slow=1.0 if not big else 1.3, k=0.7 if not big else 1.0))


def spectacub():
    spectacled("teddiursa", "SPECTACUB", 0.6, False)


def spectabear():
    spectacled("ursaring", "SPECTABEAR", 1.8, True)


# ======================================================================================================
# The fire sea slugs: EMBERSLUG > MAGMALIMPET (in place of Slugma, Magcargo)
# ======================================================================================================

def fire_slug(sid, name, stands, big):
    """A sea slug of the lava fields, a nudibranch made of fire: a soft glowing body of molten orange, its back crowded
    with flickering flames in place of a sea slug's frills, two glowing horns on its head. Grown, it has grown a limpet's
    cone of a shell from its own cooled lava, crusted dark and cracked through with glowing seams, the flames licking
    out from under its rim."""
    m = Model()
    s = 1.0 if not big else 1.6
    S = S_(s)
    hot = lambda a: mix("#B82A10", "#F07A20", 0.3 + 0.7 * a.ly)
    lit = lambda a: hash01("molten", round(a.x * 1.4), round(a.y * 1.4), round(a.z * 1.4)) < 0.22 and a.face != "bottom"
    body = Mat("#E04A18", hot, lambda a: rgb("#FFB040") if lit(a) else None, glow=lambda a: rgb("#FFC050") if lit(a) else None, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 2, 0]), blob(S([-3, 0, -3]), S([6, 3.4, 6]), body))
    m.bone("head", "body", S([0, 2, -3]), face_blob(S([-2.4, 0.4, -5.6]), S([4.8, 3.2, 3]), body))
    m.bone("jaw", "head", S([0, 1, -5.6]), [Cube(S([-1, 0.6, -5.9]), [n(2 * s), 1, 1], Mat("#8A1E0A", noise=0.01))])
    m.eyes("head", x_inner=0.7 * s, y=1.8 * s, z=-5.6 * s, w=1, h=1 if not big else 2, iris="#2A0A04", pupil="#0A0200", skin="#FF8A2A", grid=["P"] if not big else ["L", "P"])
    horn = Mat("#FFD060", glow=lambda a: rgb("#FFE890"), noise=0, edge=0)
    m.pair("antenna_right", "head", S([-1.2, 3.4, -4.6]), [Cube(S([-1.7, 3.4, -5.1]), [1, n(2.4 * s), 1], horn, [-20, 0, -10], S([-1.2, 3.4, -4.6]))])
    segs, prev, z = [], "body", 3
    for i in range(3):
        w = 5.6 - i * 1.2
        nm = f"seg{i + 1}"
        cubes = blob(S([-w / 2, 0, z - 0.3]), S([w, 3 - i * 0.4, 3.2]), body)
        if not big or i == 2:
            cubes += flame(S([-1.2, 2.6 - i * 0.4, z + 0.4]), n(2.4 * s), n((4 - i) * s), heat=1.1)
        m.bone(nm, prev, S([0, 2, z]), cubes)
        segs.append(nm)
        prev, z = nm, z + 3
    b = bone_of(m, "body")
    if not big:
        for x in (-1.6, 1.6):
            b.cubes += flame(S([x - 1.2, 3, -1.4]), n(2.4 * s), n(4.6 * s), heat=1.2)
    else:
        crust = Mat("#3A3434", lambda a: mix("#2A2424", "#6A6060", cell(a.x, a.y, a.z, 1.2)), lambda a: rgb("#FF8A2A") if (abs((a.x * 0.8 + a.z * 0.5) % 3.0 - 1.5) < 0.18) else None,
                    glow=lambda a: rgb("#FFB040") if (abs((a.x * 0.8 + a.z * 0.5) % 3.0 - 1.5) < 0.18) else None, noise=0.02)
        b.cubes += blob(S([-4.4, 2.6, -4]), S([8.8, 2.4, 9]), crust) + blob(S([-3.2, 4.8, -3]), S([6.4, 2.4, 6.6]), crust) + blob(S([-2, 7, -1.8]), S([4, 2, 4]), crust) + \
            [Cube(S([-0.8, 8.8, -0.6]), [n(1.6 * s), n(1.4 * s), n(1.6 * s)], crust)]
        for x, z in ((-4, -2), (4, -2), (-4, 2.4), (4, 2.4)):
            b.cubes += flame(S([x - 1, 1.6, z - 1]), n(2 * s), n(3 * s), heat=1.0)
    fx_for(m, 0.7 * s)
    moves(m, "EMBER" if not big else "FLAMETHROWER", "SMOG" if not big else "ROCK SLIDE")
    A, info = crawler(name.lower(), m, segs, walk=1.2, k=0.3 * s, slow=1.6)
    write(sid, name, m, A, info, stands)


def emberslug():
    fire_slug("slugma", "EMBERSLUG", 0.7, False)


def magmalimpet():
    fire_slug("magcargo", "MAGMALIMPET", 0.8, True)


def main(names):
    for f in ALL7:
        if not names or f.__name__ in names:
            f()
    import mons4
    mons4.refresh_names()


ALL7 = (sungenet, duskgenet, pilferpie, sagemander, keenbell, runelet, glumfish, psykapi, burrball, ironburr, slowwyrm, duneglide, ironwyrm,
        snortlet, bristlehog, lionfrill, vermiclaw, jarbug, stagbrawl, rimeclaw, spectacub, spectabear, emberslug, magmalimpet)

if __name__ == "__main__":
    main(set(sys.argv[1:]))
