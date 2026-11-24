"""
Retromon's own creatures, part eight: Johto's, Swinub to Blissey (John, 2026-11-24 12:00: "build out our Retromon
please for the additional Johto"). Our own designs drawn from real animals, plants and things.

Run: python3 tools/mons8.py [names]   (all if none; refreshes mons/names.json either way)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, cell, face_blob, hash01, mix, rgb, track, wave, keys, write, flame  # noqa: E402
from monanims import four_legs, bird, floater, insect, biped, serpent, flyer, hopper, crawler, cocoon  # noqa: E402
from mons import fx_for, open_mouth, shag_mat, stout_legs, whisker_mat, horn_mat, leaf_mat, feather_plane, sharp_claw_mat  # noqa: E402
from mons2 import fur, brush, n  # noqa: E402
from mons3 import (moves, bone_of, tail_bones, ear_mat, spike, taper_mat, legs_pair, glow_patch, upright_legs, upright_arms, bird_legs, crab_leg)  # noqa: E402
from mons5 import quad, avian, bug, S_, first, solid, plain, antlers, back_row, flame_row, spot_coat, stripe_coat, both  # noqa: E402
from mons6 import upright, fish, fin_mat  # noqa: E402


def curtain(base, light, dark):
    """Long hair hanging in a curtain on a flat part hanging from its top: strands, a ragged bottom edge."""
    def f(at):
        u, v = at.lz if at.cube.size[0] == 0 else at.lx, at.ly
        k = hash01("strand", int(u * at.fw)) * 0.3
        if v < k:
            return CLEAR
        return mix(dark, light, 0.25 + 0.6 * v) if int(u * at.fw) % 2 else mix(dark, base, 0.4 + 0.5 * v)
    return Mat(base, f, noise=0.01, edge=0, flat=True)


# ======================================================================================================
# The musk oxen: MUSKLET > TUNDRAMUSK (in place of Swinub, Piloswine)
# ======================================================================================================

def musk_ox(sid, name, stands, big):
    """A musk ox of the tundra: a dense coat of dark brown wool with a pale saddle over its back, its long outer hair
    hanging in a curtain almost to the ground. The calf is a woolly little barrel with a pale nose. Grown, a great
    shaggy hump of a beast, its eyes lost behind its fringe, its horns meeting in a heavy boss on its brow, sweeping
    down past its cheeks and hooking up at the tips; frost hangs in its fur."""
    s = 1.0 if not big else 1.9
    FUR, FUR_L, FUR_D = "#5A4030", "#8A6A4A", "#2E2016"

    def coat(at, base):
        if at.face == "top" and -1.5 < at.z < 2.5: return mix("#C8B490", "#E8DCC0", cell(at.x, at.y, at.z, 1.2))
        if big and at.face != "bottom" and hash01("frost", round(at.x), round(at.y), round(at.z)) < 0.05: return rgb("#E8F4FF")
        return None
    hang = curtain(FUR, FUR_L, FUR_D)
    hornm = Mat("#D8CCB0", lambda a: rgb("#4A3A2A") if a.lz > 0.75 or a.ly > 0.8 else None, noise=0.02)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        top, bl, bw = p["top"], p["bl"], p["bw"]
        dy = p["bh"] + (1.4 if big else 0.8)
        for x in (-bw / 2 - 0.2, bw / 2 + 0.2):
            b.cubes.append(Cube(S([x, top - 1.6, -bl / 2 + 0.4]), [0, n(dy * s_), n((bl - 0.8) * s_)], hang))
        b.cubes.append(Cube(S([-bw / 2, top - 1.6, bl / 2 + 0.2]), [n(bw * s_), n(dy * s_), 0], hang))
        hd = bone_of(m, "head")
        hy, hz, hl, hh = p["hy"], p["hz"], p["hl"], p["hh"]
        hd.cubes.append(Cube(S([-p["hw"] / 2, hy + hh * 0.2, hz - hl - 0.1]), [n(p["hw"] * s_), n(hh * 0.8 * s_), 0], curtain(FUR_D, FUR, FUR_D)))
        if big:
            hd.cubes += [Cube(S([-3, hy + hh - 0.8, hz - hl * 0.6]), [n(6 * s_), n(1.4 * s_), n(2 * s_)], hornm)]
            for x in (-1, 1):
                piv = S([x * 3, hy + hh - 0.2, hz - hl * 0.5])
                hd.cubes += [Cube(S([x * 3 - 0.6, hy + hh - 4, hz - hl * 0.5 - 0.6]), [n(1.2 * s_), n(3.8 * s_), n(1.2 * s_)], hornm, [0, 0, x * 18], piv),
                             Cube(S([x * 4.1 - 0.5, hy + hh - 5, hz - hl * 0.5 - 2.4]), [1, 1, n(2.6 * s_)], hornm, [-30, x * -30, 0], S([x * 4.1, hy + hh - 4.4, hz - hl * 0.5]))]
        else:
            hd.cubes += [Cube(S([x - 0.5, hy + hh - 0.4, hz - hl * 0.5]), [1, 1, 1], hornm) for x in (-1.4, 1.4)]
    quad(sid, name, stands, dict(
        s=s, body=(6.6, 5.6, 9) if not big else (7.6, 6.4, 10), top=3 if not big else 3.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=FUR_D, coat=coat, speck=0.6,
        FACE=FUR, head=(4.6, 4, 4) if not big else (5.4, 4.4, 4.6), snout=1.6, snout_w=1.1, snout_h=2, MUZZLE="#C8B898" if not big else "#8A7A64", NOSE="#2A2018",
        neck=1.6, neck_up=-1.2 if big else -0.6, neck_tilt=-2, head_tilt=20, ears=None, IRIS="#1A120C", eye_grid=["PW", "PP"] if not big else ["P"],
        eye_h=2 if not big else 1, eye_y=0.5, leg_w=1.8 if not big else 2, toes=2, foot=Mat("#2A2018", noise=0.01), tail={"segs": [(1.2, 1.2, 1.2)], "rests": [-30]},
        extra=extra, moves=(("POWDER SNOW", "MUD-SLAP") if not big else ("TAKE DOWN", "BLIZZARD")), walk=0.5 if not big else 0.75, stride=24, k=0.6, slow=1.0 + 0.3 * big))


def musklet():
    musk_ox("swinub", "MUSKLET", 0.4, False)


def tundramusk():
    musk_ox("piloswine", "TUNDRAMUSK", 1.1, True)


# ======================================================================================================
# The decorator crab: REEFLET (in place of Corsola)
# ======================================================================================================

def reeflet():
    """A decorator crab that dresses itself in living coral: a small round rose-pink crab, little pincers held up, eyes
    on short stalks, and its back grown over with branching pink coral, each tip a pale polyp, that it trims and tends."""
    m = Model()
    s = 1.1
    S = S_(s)
    CR, CRL, CRD = "#E87A8A", "#FFB0B8", "#A84050"
    shell = Mat(CR, lambda a: mix(CRD, CR, 0.4 + 0.6 * a.ly) if a.face != "top" else mix(CR, CRL, 0.4), noise=0.02)
    legm = Mat(CR, lambda a: mix(CRD, CR, a.ly), noise=0.02, edge=0)
    tip = sharp_claw_mat("#6A2030", "#FFE0E0")
    coral = Mat("#F08A9A", lambda a: rgb("#FFF0F0") if a.ly > 0.8 else mix("#D85A70", "#FFB8C0", a.ly), noise=0.02)
    eyeb = Mat("#141014", lambda a: rgb("#FFFFFF") if a.face == "front" and a.lx > 0.5 and a.ly > 0.5 else None, noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 3.4, 0]), blob(S([-3.6, 2, -3]), S([7.2, 3, 6]), shell))
    b = bone_of(m, "body")
    for i, (x, z, h, a) in enumerate(((-1.6, -0.6, 4, -14), (1.2, 0.4, 5, 12), (0, 1.8, 3.4, 0), (-2.4, 1.6, 2.6, -30), (2.6, -1, 2.8, 30))):
        piv = S([x, 5, z])
        b.cubes.append(Cube(S([x - 0.6, 5, z - 0.6]), [n(1.2 * s), n(h * s), n(1.2 * s)], coral, [0, 0, a], piv))
        for k in range(2):
            bb = h * (0.45 + 0.25 * k)
            b.cubes.append(Cube(S([x - 0.5, 5 + bb, z - 0.5]), [1, n(1.8 * s), 1], coral, [0, 0, a + (40 if k else -40)], S([x, 5 + bb, z])))
    m.bone("head", "body", S([0, 4, -3]), [Cube(S([-2, 2.6, -3.6]), [n(4 * s), n(2 * s), 1], shell)])
    m.bone("jaw", "head", S([0, 2.8, -3.6]), [Cube(S([-0.8, 2.4, -4.1]), [n(1.6 * s), 1, 1], Mat(CRD, noise=0.02))])
    for sx, side in ((-1, "right"), (1, "left")):
        m.bone(f"antenna_{side}", "head", S([sx * 1.2, 4.4, -3.2]), [Cube(S([sx * 1.2 - 0.4, 4.4, -3.6]), [1, n(1.8 * s), 1], legm),
                                                                      Cube(S([sx * 1.2 - 0.6, 6, -3.8]), [n(1.2 * s), n(1.2 * s), n(1.2 * s)], eyeb)])
    for sx, side in ((-1, "right"), (1, "left")):
        m.bone(f"arm_{side}", "body", S([sx * 3, 3.2, -2.6]), [Cube(S([sx * 3 - 0.6 + (-1.2 if sx < 0 else 0), 2.6, -4.4]), [n(1.8 * s), n(1.4 * s), n(2 * s)], shell)],
               rotation=[0, sx * -20, 0])
        m.bone(f"forearm_{side}", f"arm_{side}", S([sx * 4, 3.2, -4.2]), [Cube(S([sx * 4 - 1, 2.6, -6.6]), [n(2 * s), n(1.8 * s), n(2.4 * s)], shell),
                                                                       Cube(S([sx * 4 - 0.8, 3.6, -7.6]), [n(1.6 * s), 1, n(1.2 * s)], tip)], rotation=[-20, 0, 0])
    for nm, zz in (("leg_front_right", -1.4), ("leg_mid_right", 0.4), ("leg_back_right", 2)):
        crab_leg(m, nm, 3.4 * s, 3 * s, zz * s, 2.6 * s, 3.4 * s, legm, tip)
    fx_for(m, 0.6 * s)
    moves(m, "TACKLE", "RECOVER")
    W = 0.45
    A, info = four_legs("reeflet", m, walk=W, stride=22, k=0.5 * s, extras={
        "walk": {"leg_mid_right": track(rotation=[wave(W, 22, 90), 0, 0]), "leg_mid_left": track(rotation=[wave(W, -22, 90), 0, 0]), "body": track(rotation=[0, 20, 0])},
        "idle": {"forearm_right": track(rotation=[wave(1.6, 10, 0, -20), 0, 0]), "forearm_left": track(rotation=[wave(1.6, 10, 90, -20), 0, 0])}})
    write("corsola", "REEFLET", m, A, info, 0.6)


# ======================================================================================================
# The archerfish: ARCHERFIN (in place of Remoraid)
# ======================================================================================================

def archerfin():
    """An archerfish: a deep silver body with four black bands down its sides, big eyes set forward to aim, and a
    pointed mouth it shoots jets of water from to knock insects off the leaves."""
    def paint(a):
        if a.face in ("nx", "px", "top") and a.y > 5.4 and int((a.z + 4) * 0.55) % 2 == 0 and abs(a.z) < 3.6: return rgb("#1A1A20")
        return None
    fish("remoraid", "ARCHERFIN", 0.6, dict(
        s=1.0, y=6, body=(3.2, 5, 7.4), B="#B8C4D0", B_L="#E8EEF4", B_D="#6A7888", BELLY="#F4F6F8", paint=paint, head=(3, 4, 3), lip=(1.6, 1.4, 1.2),
        IRIS="#E8C840", IRIS_L="#FFF0A0", eye_grid=["WP", "IP"], eye_dy=0.6, FIN="#C8D4E0", FIN_L="#E8EEF4", FIN_D="#5A6878", tail_fin=("round", 5, 3.4),
        dorsal=(2.2, 3.4, "sail"), dorsal_z=2.4, pecs=(2.2, 2.6, 30), moves=("WATER GUN", "LOCK-ON"), k=0.7))


# ======================================================================================================
# The cuttlefish: INKCUTTLE (in place of Octillery)
# ======================================================================================================

def inkcuttle():
    """A cuttlefish: a broad shield of a mantle edged all round by a rippling fin, its skin rippling with maroon and
    cream zebra bands that flash and change, great eyes with W-shaped pupils, eight short arms gathered in front and two
    long hunting tentacles; it squirts clouds of ink."""
    m = Model()
    s = 1.5
    S = S_(s)
    C, C_L, C_D = "#A8384A", "#F0C8B0", "#5A1424"

    def zebra(a):
        if a.face == "bottom":
            return rgb("#F0D8C8")
        k = math.sin(a.z * 1.3 + math.sin(a.x * 0.8) * 1.5)
        return mix(C_D, C, 0.6 + 0.4 * a.ly) if k > 0.1 else mix(C, C_L, 0.5)
    skin = Mat(C, zebra, noise=0.015)
    finm = Mat(C_L, lambda a: CLEAR if a.lx > 0.55 + 0.35 * math.sin(a.lz * math.pi * 7) ** 2 else mix(C, C_L, 0.6), noise=0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    y0 = 7
    m.bone("body", "root", S([0, y0, 0]), blob(S([-3.4, y0 - 2, -3]), S([6.8, 4, 10]), skin) +
           [Cube(S([-5.4, y0 - 0.6, -2.6]), [n(2.2 * s), 0, n(9.4 * s)], finm), Cube(S([3.2, y0 - 0.6, -2.6]), [n(2.2 * s), 0, n(9.4 * s)], finm)])
    m.bone("head", "body", S([0, y0, -3]), face_blob(S([-3, y0 - 2, -6.4]), S([6, 4, 3.6]), skin))
    m.bone("jaw", "head", S([0, y0 - 1.4, -6.4]), [Cube(S([-0.8, y0 - 2, -6.7]), [n(1.6 * s), 1, 1], plain("#3A0A14"))])
    m.eyes("head", 3 * s, (y0 + 0.2) * s, -6.4 * s, h=2, corner=(1, 1), half=3 * s, iris="#E8C040", pupil="#140A04", skin=C, iris_light="#FFF0A0", grid=["IJ", "PJ"])
    armm = Mat(C, lambda a: mix(C_D, C_L, 0.3 + 0.5 * a.ly), noise=0.02)
    tents = []
    for j, (x, y) in enumerate(((-2, y0 - 1.4), (-0.7, y0 - 1.6), (0.7, y0 - 1.6), (2, y0 - 1.4), (-1.4, y0 - 0.2), (1.4, y0 - 0.2))):
        prev, z = "head", -6.4
        names = []
        for i in range(2):
            nm = f"arm{j}_{i}"
            m.bone(nm, prev, S([x, y, z]), [Cube(S([x - 0.5, y - 0.5, z - 2.4]), [1, 1, n(2.6 * s)], armm)], rotation=[10 if i == 0 else 6, 0, 0])
            names.append(nm)
            prev, z = nm, z - 2.4
        tents.append(names)
    fx_for(m, 0.7 * s)
    moves(m, "CONSTRICT", "OCTAZOOKA")
    ripple = {"body": track(scale=[1, wave(1.2, 0.02, 0, 1), 1])}
    A, info = floater("inkcuttle", m, tentacles=tents, k=0.6 * s, bob=0.6, extras={"idle": ripple})
    write("octillery", "INKCUTTLE", m, A, info, 0.9)


# ======================================================================================================
# The puffin: PARCELPUFF (in place of Delibird)
# ======================================================================================================

def parcelpuff():
    """A puffin that brings everyone a share of its catch: black above and white below, a white face with a grey
    eye-patch, a great striped bill of red, gold and slate, bright orange feet, and a row of silver fish held crossways
    in its bill to hand round."""
    def face_coat(a):
        if a.face in ("front", "nx", "px") and a.ly < 0.75: return rgb("#F4F4F0") if not (a.face != "front" and 0.4 < a.ly < 0.55 and a.lz < 0.5) else rgb("#9A9AA4")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "beak_upper")
        y, z0 = p["hy"] + p["hh"] * 0.32, p["hz"] - p["hl"]
        b.cubes += [Cube(S([-0.81, y - 0.2, z0 - 1.2]), [n(1.62 * s), n(1.4 * s), 0], plain("#E8B030"))]
        fishm = Mat("#C8D0D8", lambda a: rgb("#5A6A7A") if a.face == "top" else None, noise=0.01, edge=0)
        for i, x in enumerate((-3.4, -2.4, 2.4, 3.4)):
            b.cubes.append(Cube(S([x - 0.5, y - 0.6 - (i % 2) * 0.4, z0 - 1.6]), [1, 1, n(1.2 * s)], fishm, [0, 0, 70 if x < 0 else -70], S([x, y, z0 - 1])))
        b.cubes.append(Cube(S([-3.8, y - 0.4, z0 - 1.6]), [n(7.6 * s), 1, 1], fishm))
    avian("delibird", "PARCELPUFF", 0.9, dict(
        s=1.4, body=(5.6, 6.4, 6.4), leg=2, B="#1A1A20", B_L="#34343E", B_D="#0A0A0E", BELLY="#F4F4F0", head=(4.4, 4.2, 4.2), FACE="#1A1A20", face_coat=face_coat,
        neck_up=0.4, beak=2.4, beak_w=1.6, beak_h=2.4, beak_y=0.18, BEAK="#E84A2A", BEAK_D="#5A6070", eye_w=2, eye_h=2, IRIS="#1A1A1A", eye_in=2.2, eye_back=1.2,
        wing=6, WING="#1A1A20", WING_D="#0A0A0E", WING_L="#34343E", tail=2, TAIL="#1A1A20", LEG="#F08A2A", pitch=-18, extra=extra,
        moves=("PRESENT", "PRESENT"), walk=0.4, k=0.8))


# ======================================================================================================
# The flying fish: GLIDEFIN (in place of Mantine)
# ======================================================================================================

def glidefin():
    """A great flying fish: a sleek torpedo of a body, deep blue above and silver beneath, and pectoral fins so huge they
    are wings, clear blue with dark rays, that it spreads to sail out of the waves and glide far over the sea; a long
    lower lobe to its tail it sculls the water with to take off."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        b.cubes += [Cube(S([x - (0 if x < 0 else 0), p["y0"] - 2, 2]), [0, n(3 * s), n(3.6 * s)], fin_mat("#5A8AD0", "#A8D0FF", "#1A3A7A", "round"), [0, 0, 60 if x < 0 else -60],
                         S([x, p["y0"] - 2, 2])) for x in (-1.4, 1.4)]
    fish("mantine", "GLIDEFIN", 2.1, dict(
        s=2.0, y=6, body=(3.6, 3.6, 11), B="#2A4A8A", B_L="#C8D4E8", B_D="#14244A", BELLY="#E8EEF4", head=(3.2, 3, 3), IRIS="#1A1A2A", IRIS_L="#5A6A9A",
        FIN="#5A8AD0", FIN_L="#A8D0FF", FIN_D="#1A3A7A", tail_segs=[(2.4, 2.4, 2.4), (1.6, 1.6, 2)], tail_fin=("fork", 6, 4), dorsal=(1.6, 2.4, "sail"),
        dorsal_z=1.6, pecs=(11, 9, 84), flap=1.4, flap_amp=8, extra=extra, moves=("WING ATTACK", "BUBBLEBEAM"), k=0.6, bob=0.8))


# ======================================================================================================
# The blade heron: BLADEHERON (in place of Skarmory)
# ======================================================================================================

def bladeheron():
    """A heron forged of steel: tall, on long dark legs, a long neck and a dagger of a bill, its plumage burnished
    gunmetal, its long flight feathers edged sharp and bright as blades, two dark streamers trailing from its crown and
    fierce orange-red eyes."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        st = plain("#2A2E38")
        hd.cubes += [Cube(S([x - 0.5, p["hy"] + p["hh"] - 0.6, p["hz"] - p["hl"] * 0.3]), [1, 0, n(6 * s)], st, [-10, 0, 0], S([x, p["hy"] + p["hh"] - 0.6, p["hz"] - p["hl"] * 0.3]))
                     for x in (-0.6, 0.6)]
        for nm in ("wing_tip_right", "wing_tip_left", "wing_mid_right", "wing_mid_left"):
            try:
                b = bone_of(m, nm)
            except StopIteration:
                continue
            for c in b.cubes:
                if c.mat.flat:
                    c.mat = Mat("#8A94A4", lambda a: rgb("#F0F4F8") if a.lz > 0.82 or a.ly < 0.08 else mix("#4A5260", "#C8D0DC", a.ly), noise=0.01, edge=0, flat=True)
    avian("skarmory", "BLADEHERON", 1.7, dict(
        s=1.6, body=(5, 5.4, 9), leg=8, B="#7A8494", B_L="#B8C2D0", B_D="#3E4652", BELLY="#A8B2C0", head=(3.4, 3, 4), FACE="#7A8494", neck_up=4.6, beak=5.6,
        beak_w=1, beak_h=1, beak_tilt=4, BEAK="#C8A040", BEAK_D="#8A6A20", eye_w=2, eye_h=1, IRIS="#E8501A", IRIS_L="#FF9A5A", eye_grid=["IP"], eye_in=1.7, eye_back=0.8,
        wing=11, WING="#5A6474", WING_D="#2E3440", WING_L="#C8D0DC", tail=5, TAIL="#5A6474", LEG="#2A2E38", pitch=-16, extra=extra,
        moves=("STEEL WING", "SWIFT"), walk=0.6, stride=30, k=0.7, flap=0.4))


# ======================================================================================================
# The ember hyenas: ASHCUB > SCORCHYENA (in place of Houndour, Houndoom)
# ======================================================================================================

def hyena(sid, name, stands, big):
    """A striped hyena of the ash fields: a coat of smoky charcoal barred across its flanks and legs with ember-orange
    stripes, great pointed ears, a heavy dark muzzle, sloping back, and along its spine a crest of long hair it raises
    when angry. The cub's crest is a soft fuzz; grown, the crest smoulders, glowing embers along it, its eyes burning."""
    s = 1.0 if not big else 1.6
    FUR, FUR_L, FUR_D = "#4A4248", "#6E646C", "#28222A"
    ST = "#E8642A"

    def coat(at, base):
        if at.face in ("nx", "px") and (at.z % 2.4) < 0.9 and at.y > 2: return rgb(ST)
        return None
    legm = Mat(FUR, lambda a: rgb(ST) if a.face in ("nx", "px", "front") and (a.y % 2) < 0.7 else mix(FUR_D, FUR, 0.6), noise=0.02)
    crest = Mat(FUR_D, brush(FUR_D, "#8A7A7A", "#141014", tip=ST if big else None, tip_from=0.6), glow=(lambda a: rgb("#FF9A3A") if a.ly > 0.75 else None) if big else None,
                noise=0.0, edge=0)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        back_row(p, crest, 7, 2.4 if big else 1.4, w=1.4, lean=-35, z_from=0.5, z_to=-0.9, grow=0.3)
        nb = bone_of(m, "neck")
        nb.cubes.append(Cube(S([0, p["hy"] + 1.4, p["hz"] + 0.2]), [0, n((3 if big else 1.6) * s_), n(3.4 * s_)], crest, [-30, 0, 0], S([0, p["hy"] + 1.4, p["hz"]])))
        if big:
            flame_row(p, 3, 2.6, heat=1.0, y_off=1.4, z_from=0.0, z_to=-0.6)
    quad(sid, name, stands, dict(
        s=s, body=(5, 5, 9), top=4.4, below=5.4, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#5A5058", coat=coat, legm=legm, FACE=FUR, head=(4.4, 4, 4.2), snout=2.4,
        snout_w=1, snout_h=2.2, MUZZLE="#1E1A20", NOSE="#0A080A", neck=2.4, neck_up=0.4, neck_tilt=-12, head_tilt=6, ears=("fennec", 2.6, 3.6), EAR=FUR_D,
        EAR_IN="#8A5A4A", ear_rot=[-4, 12, -14], IRIS="#E8501A" if big else "#E8A020", IRIS_L="#FFB060", eye_grid=["LL", "IP"], eye_y=0.58, leg_w=1.8, toes=2,
        sharp=True, tail={"segs": [(1.6, 1.6, 2.4), (2, 2, 2.4)], "rests": [-30, -10], "y": 0.85, "mat": crest}, extra=extra,
        moves=(("BITE", "EMBER") if not big else ("CRUNCH", "FLAMETHROWER")), walk=0.5, stride=28, k=0.7))


def ashcub():
    hyena("houndour", "ASHCUB", 0.6, False)


def scorchyena():
    hyena("houndoom", "SCORCHYENA", 1.4, True)


# ======================================================================================================
# The weedy sea dragon: FRONDKING (in place of Kingdra; after DRIFTLEAF and FRONDRAKE)
# ======================================================================================================

def frondking():
    """A weedy sea dragon of royal blue, the leafy sea dragon grown into its majesty: a slim ringed body spotted gold, a
    long tube of a snout, a tail curled in coils, deep crimson leafy fronds edged with gold sprouting all along it, a
    crown of them on its head, and a glowing blue fin down its back."""
    m = Model()
    s = 2.0
    S = S_(s)
    B, B_L, B_D = "#3A4A9A", "#7A9AE0", "#1E2660"

    def ringed_f(at):
        c = mix(B_D, B, 0.4 + 0.6 * at.ly)
        if (at.y * 1.4) % 1.0 < 0.18:
            c = mix(c, B_D, 0.5)
        if hash01("dot", round(at.x * 2), round(at.y * 2), round(at.z * 2)) < 0.09:
            c = rgb("#F0D040")
        return c
    body = Mat(B, ringed_f, noise=0.02)
    frond = leaf_mat("#C83A4A", "#6A1424", "#F8D060", tip="#F0C040")
    finm = Mat("#5AC8F0", lambda a: CLEAR if a.ly > 0.4 + 0.5 * math.sin(math.pi * a.lz) else None, glow=lambda a: rgb("#8AE8FF") if a.ly < 0.3 else None, noise=0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    y0 = 9
    m.bone("body", "root", S([0, y0, 0]), blob(S([-2, y0 - 3, -2]), S([4, 6, 4]), body) + [Cube(S([-1.8, y0 - 1, -2.6]), [n(3.6 * s), n(3 * s), 1], body)])
    m.bone("neck", "body", S([0, y0 + 3, -0.5]), [Cube(S([-1.4, y0 + 2.6, -1.8]), [n(2.8 * s), n(3 * s), n(2.8 * s)], body)], rotation=[-10, 0, 0])
    m.bone("head", "neck", S([0, y0 + 5.4, -0.5]), face_blob(S([-1.8, y0 + 5, -2.6]), S([3.6, 3, 3.4]), body) + [Cube(S([-0.6, y0 + 5.6, -7]), [n(1.2 * s), n(1.2 * s), n(4.6 * s)], body),
                                                                                                         Cube(S([-1, y0 + 5.2, -7.6]), [n(2 * s), n(1.8 * s), 1], body)], rotation=[10, 0, 0])
    m.bone("jaw", "head", S([0, y0 + 5.6, -7]), [])
    m.eyes("head", 1.8 * s, (y0 + 6.2) * s, -2.6 * s, h=1, corner=(1, 1), half=1.8 * s, iris="#E8301A", pupil="#100804", skin=B, iris_light="#FF8A5A", grid=["IP"])
    names, prev, y = [], "body", y0 - 3
    for i in range(5):
        nm = "tail" if i == 0 else f"tail{i + 1}"
        w = (2.6 - i * 0.35) * s
        m.bone(nm, prev, [0, y * s, 0.5 * s], [Cube([-w / 2, (y - 2.2) * s, 0.5 * s - w / 2], [n(w), n(2.4 * s), n(w)], body)], rotation=[34 if i else 10, 0, 0])
        names.append(nm)
        prev, y = nm, y - 2.2
    fr = [("body", 0, y0 + 2, 1.8, 0, 4), ("body", -1.8, y0, 0, -60, 3.5), ("body", 1.8, y0, 0, 60, 3.5), ("neck", 0, y0 + 4.6, 1, -20, 3), ("tail2", 0, y0 - 5, 1.6, 20, 3),
          ("tail3", -1, y0 - 7, 1, -50, 2.6), ("tail3", 1, y0 - 7, 1, 50, 2.6), ("tail4", 0, y0 - 9, 1, 10, 2.4)]
    for bn, x, y, z, a, L in fr:
        b = bone_of(m, bn)
        for k in range(3):
            b.cubes.append(Cube([x * s - 1.2 * s, y * s, z * s], [n(2.4 * s), n(L * s * (1 + k * 0.3)), 0], frond, [k * 30 - 20, a + k * 25, (a / 3)], [x * s, y * s, z * s]))
    hd = bone_of(m, "head")
    for k, a in enumerate((-40, -15, 15, 40)):
        hd.cubes.append(Cube(S([-1.2, y0 + 7.8, -1]), [n(2.4 * s), n((3.4 - abs(k - 1.5) * 0.4) * s), 0], frond, [-10, 0, a], S([0, y0 + 7.8, -1])))
    bone_of(m, "body").cubes.append(Cube([0, (y0 - 2) * s, 1.8 * s], [0, n(4 * s), n(3 * s)], finm))
    fx_for(m, 0.7 * s)
    moves(m, "WATERFALL", "DRAGONBREATH")
    A, info = floater("frondking", m, k=0.7 * s, bob=0.8, tentacles=[names], extras={"idle": {"head": track(rotation=[wave(2.6, 5), wave(4, 10), 0])}})
    write("kingdra", "FRONDKING", m, A, info, 1.8)


# ======================================================================================================
# The hippos: HIPPET > TERRAHIPPO (in place of Phanpy, Donphan)
# ======================================================================================================

def hippo(sid, name, stands, big):
    """A hippo. The calf is a pygmy hippo, glossy slate-blue and rosy underneath, round as a barrel, with a broad
    rounded muzzle, little ears, and eyes and nostrils set up high. Grown, a great hippo caked in sun-baked river mud
    that has set across its back like armour plates, two long ivory tusks curving up from its lower jaw."""
    s = 1.0 if not big else 1.9
    FUR, FUR_L, FUR_D = ("#7A8AA8", "#A8B8D0", "#4A5870") if not big else ("#5A6478", "#8A94A8", "#343C4C")

    def coat(at, base):
        if at.face == "bottom" or (at.face in ("nx", "px", "front") and at.y < (3.2 if not big else 5.8)): return rgb("#D8A8B0")
        return None
    plates = Mat("#8A7A62", lambda a: mix("#6A5A44", "#B8A888", cell(a.x, a.y, a.z, 1.1)),
                 lambda a: rgb("#4A3E30") if hash01("crack", round(a.x * 1.2), round(a.z * 1.2)) < 0.12 else None, noise=0.03)
    tusk = Mat("#F8F0DC", lambda a: rgb("#D8C8A8") if a.ly < 0.3 else None, noise=0.01, edge=0)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        hy, hz, hl, hh, hw = p["hy"], p["hz"], p["hl"], p["hh"], p["hw"]
        hd.cubes += [Cube(S([x - 0.5, hy + hh * 0.5, hz - hl - p["snout"] - 0.05]), [1, 1, 0], plain("#2A2430")) for x in (-0.9, 0.9)]
        if big:
            b = bone_of(m, "body")
            top = p["top"] + p["bh"]
            for i, (x, z, w, d) in enumerate(((-1.6, -3.2, 3.6, 3.2), (1.8, -2.4, 3, 3.6), (-1.2, 0.6, 4, 3.4), (2, 1.6, 2.6, 3), (-0.4, 3.6, 4.4, 2.6))):
                b.cubes += blob(S([x - w / 2, top - 0.8, z - d / 2]), S([w, 1.4 + (i % 2) * 0.4, d]), plates)
            for x in (-1.4, 1.4):
                hd.cubes.append(Cube(S([x - 0.6, hy - 0.4, hz - hl - p["snout"] + 0.6]), [n(1.2 * s_), n(4 * s_), n(1.2 * s_)], tusk, [-30, 0, 18 if x > 0 else -18],
                                     S([x, hy - 0.2, hz - hl - p["snout"] + 1])))
    quad(sid, name, stands, dict(
        s=s, body=(7, 6, 10), top=2.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#D8A8B0", coat=coat, speck=0.1, FACE=FUR, head=(5.4, 4.2, 4.6), snout=2.4, snout_w=2.0,
        snout_h=3, MUZZLE=FUR_L, NOSE="#4A4A5A", neck=1.6, neck_up=-0.6, neck_tilt=-2, head_tilt=4, ears=("round", 1.4, 1.4), EAR=FUR_D, EAR_IN="#D8A8B0",
        IRIS="#2A1A1A", IRIS_L="#6A4A4A", eye_grid=["LL", "IP"] if big else ["PW", "PP"], eye_y=0.7, eye_in=1.2, leg_w=2.4, toes=3, foot=Mat(FUR_D, noise=0.01),
        tail={"segs": [(1, 1, 1.4)], "rests": [-20]}, extra=extra, moves=(("TACKLE", "DEFENSE CURL") if not big else ("RAPID SPIN", "EARTHQUAKE")),
        walk=0.5 if not big else 0.8, stride=22, k=0.6, slow=1.0 + 0.3 * big))


def hippet():
    hippo("phanpy", "HIPPET", 0.5, False)


def terrahippo():
    hippo("donphan", "TERRAHIPPO", 1.1, True)


# ======================================================================================================
# The paper crane: CREASECRANE (in place of Porygon2)
# ======================================================================================================

def creasecrane():
    """A paper crane folded so perfectly it came alive: crisp flat facets of pastel paper, rose on one side and sky blue
    on the other, crease lines running across them, a long pointed neck and tail, its wide wings beating with a papery
    rustle as it hovers."""
    m = Model()
    s = 1.4
    S = S_(s)

    def paper(c1, c2):
        def f(a):
            u, v = a.lx, (a.ly if a.cube.size[1] else a.lz)
            c = c1 if a.face in ("top", "front", "nx") else c2
            if abs(u - v) < 0.04 or abs(u + v - 1) < 0.04:
                return mix(c, "#000000", 0.18)
            return mix(c, "#FFFFFF", 0.1 * v)
        return Mat(c1, f, noise=0.005, edge=0.04)
    pa = paper("#F4A8B8", "#9AC8F0")
    wing = Mat("#F4A8B8", lambda a: CLEAR if a.lz > 1 - (abs(a.x) / (9 * s)) * 0.9 else (mix("#9AC8F0", "#FFFFFF", 0.1) if a.face == "bottom" else
                                                                                       (mix("#F4A8B8", "#000000", 0.15) if abs(a.lz - 0.5) < 0.03 else rgb("#F4A8B8"))),
               noise=0.005, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    y0 = 7
    m.bone("body", "root", S([0, y0, 0]), [Cube(S([-1.6, y0 - 1.6, -2.4]), [n(3.2 * s), n(3.2 * s), n(4.8 * s)], pa, [0, 0, 45], S([0, y0, 0]))])
    m.bone("neck", "body", S([0, y0, -2]), [Cube(S([-0.5, y0, -2.6]), [1, n(5.4 * s), n(1.2 * s)], pa)], rotation=[-34, 0, 0])
    m.bone("head", "neck", S([0, y0 + 5.4, -2.2]), [Cube(S([-0.9, y0 + 5.0, -3.4]), [n(1.8 * s), n(1.6 * s), n(2.4 * s)], pa),
                                                   Cube(S([-0.5, y0 + 5.2, -5.2]), [1, 1, n(1.8 * s)], pa)], rotation=[34, 0, 0])
    m.bone("jaw", "head", S([0, y0 + 5.2, -3.4]), [])
    m.eyes("head", 0.9 * s, (y0 + 5.6) * s, -3.2 * s, w=1, h=1, side=True, iris="#2A2A4A", pupil="#0A0A1A", skin="#F4A8B8", grid=["P"])
    m.bone("tail", "body", S([0, y0, 2]), [Cube(S([-0.5, y0, 1.6]), [1, n(5.6 * s), n(1.2 * s)], pa)], rotation=[40, 0, 0])
    m.pair("wing_right", "body", S([-1.6, y0 + 1, 0]), [Cube(S([-10.6, y0 + 1, -2.4]), [n(9 * s), 0, n(5 * s)], wing)], rotation=[0, 0, 20])
    fx_for(m, 0.6 * s)
    moves(m, "TRI ATTACK", "CONVERSION2")
    flap = {"wing_right": track(rotation=[0, 0, wave(0.9, 26, 0, 20)]), "wing_left": track(rotation=[0, 0, wave(0.9, -26, 0, -20)])}
    A, info = floater("creasecrane", m, k=0.7 * s, bob=1.0, extras={"idle": flap, "walk": flap, "battle": flap})
    write("porygon2", "CREASECRANE", m, A, info, 0.6)


# ======================================================================================================
# The kudu: KUDUSPIRE (in place of Stantler)
# ======================================================================================================

def kuduspire():
    """A greater kudu: a grey-brown coat with thin white stripes down its sides, a white chevron between its eyes, great
    rounded ears, a fringe along its throat, and long spiralling horns that turn and turn again, their gleam enough to
    send whoever stares at them to sleep."""
    FUR, FUR_L, FUR_D = "#8A7A6A", "#B8A890", "#5A4C40"

    def coat(at, base):
        if at.face in ("nx", "px") and (at.z % 2.2) < 0.5 and at.y > 8: return rgb("#F4F0E8")
        return None

    def face_coat(a):
        if a.face == "front" and abs(abs(a.lx - 0.5) - (0.7 - a.ly) * 0.5) < 0.08 and a.ly > 0.4: return rgb("#F4F0E8")
        return None
    hornm = Mat("#5A4A3A", lambda a: rgb("#E8DCC8") if a.ly > 0.85 else mix("#3A2A1E", "#8A7A6A", a.ly), noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        cubes = []
        for side in (-1, 1):
            x, y, z = side * 1.2, p["hy"] + p["hh"] - 0.3, p["hz"] - p["hl"] * 0.4
            for i in range(12):
                ang = i * 48
                r = 0.6 + i * 0.07
                px, pz = x + side * (math.cos(math.radians(ang)) * r + i * 0.22), z + math.sin(math.radians(ang)) * r * 0.7 + i * 0.12
                cubes.append(Cube(S([px - 0.45, y + i * 0.8, pz - 0.45]), [1, n(1.2 * s), 1], hornm, [0, 0, -side * 14], S([px, y + i * 0.8, pz])))
        bone_of(m, "head").cubes += cubes
        nb = bone_of(m, "neck")
        nb.cubes.append(Cube(S([0, p["hy"] - 2.4, p["hz"] - 0.4]), [0, n(1.8 * s), n(3.4 * s)], Mat("#E8E0D0", brush("#E8E0D0", "#FFFFFF", "#B8A890"), noise=0, edge=0)))
    quad("stantler", "KUDUSPIRE", 1.4, dict(
        s=1.5, body=(5.4, 5.6, 10), top=6.4, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#E8E0D0", coat=coat, FACE=FUR, face_coat=face_coat, head=(3.8, 3.8, 4.4),
        snout=2.2, snout_w=0.8, snout_h=2, MUZZLE="#E8E0D0", NOSE="#2A1E18", neck=3.6, neck_up=3, neck_tilt=-8, head_tilt=12, ears=("round", 2.8, 2.8), EAR=FUR,
        EAR_IN="#E8C8B0", ear_rot=[-6, 20, -30], IRIS="#3A2410", IRIS_L="#8A6A3A", eye_grid=["LL", "IP"], eye_y=0.58, leg_w=1.4, toes=2, foot=Mat("#2A1E18", noise=0.01),
        tail={"segs": [(1, 1, 2.4)], "rests": [-30], "tip": "#F4F0E8"}, extra=extra, moves=("STOMP", "HYPNOSIS"), walk=0.55, stride=28, k=0.6))


# ======================================================================================================
# The painter coati: DABBLECOAT (in place of Smeargle)
# ======================================================================================================

def dabblecoat():
    """A coati, the inquisitive one: a russet coat, a long upturned snout, white marks round its eyes, a long ringed
    tail it carries straight up; each of its four paws dipped in a different colour of paint, and dabs and smudges of
    paint wherever it's rubbed against its own work."""
    FUR, FUR_L, FUR_D = "#A8784A", "#D8A870", "#6A4426"
    PAINTS = ("#E83A3A", "#3A7AE8", "#3AB84A", "#F0C830", "#A84AE0")

    def coat(at, base):
        k = hash01("dab", int(at.x / 1.3), int(at.y / 1.3), int(at.z / 1.3))
        if k < 0.05 and at.face != "bottom": return rgb(PAINTS[int(k * 100) % 5])
        return None

    def face_coat(a):
        if a.face == "front" and 0.45 < a.ly < 0.65: return rgb("#F4ECDC")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        for i, leg in enumerate(("leg_front_right", "leg_front_left", "leg_back_right", "leg_back_left")):
            b = bone_of(m, leg)
            for c in b.cubes:
                if c.origin[1] < 1.6 * s:
                    c.mat = Mat(PAINTS[i], lambda a, P=PAINTS[i]: mix(P, "#FFFFFF", 0.25) if a.face == "top" else None, noise=0.01)
    quad("smeargle", "DABBLECOAT", 1.2, dict(
        s=1.2, body=(4.6, 4.4, 9), top=3.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#E8C898", coat=coat, FACE=FUR, face_coat=face_coat, head=(3.8, 3.4, 3.8),
        snout=3, snout_w=0.7, snout_h=1.4, MUZZLE="#4A3020", NOSE="#1A1210", neck=2.2, neck_up=0.4, neck_tilt=-10, head_tilt=4, ears=("round", 1.6, 1.6), EAR=FUR_D,
        EAR_IN="#E8C898", IRIS="#2A1A10", IRIS_L="#6A4A2A", eye_grid=["PW", "PP"], eye_y=0.55, leg_w=1.6, toes=3, sharp=False,
        tail={"segs": [(1.4, 1.4, 2.6), (1.3, 1.3, 2.6), (1.2, 1.2, 2.6), (1.1, 1.1, 2.4), (1, 1, 2.2)], "rests": [70, 10, 4, 0, -10], "y": 0.8,
              "mat": Mat(FUR, lambda a: rgb("#3A2414") if (a.z % 2.2) < 0.8 or (a.y % 2.2) < 0.8 else None, noise=0.02), "tip": "#3A2414"},
        extra=extra, moves=("SKETCH", "SKETCH"), walk=0.45, stride=28, k=0.7))


# ======================================================================================================
# The wallabies: SCRAPJOEY > TWIRLROO (in place of Tyrogue, Hitmontop)
# ======================================================================================================

def scrapjoey():
    """A young wallaby who wants to fight everyone: sandy fur, a cream chest, long ears always up, long hind feet, a
    thick tail to balance on, and white wraps round its little fists."""
    wrap = Mat("#F4F0E8", lambda a: rgb("#C8C0B0") if a.row % 2 == 0 else None, noise=0.02)
    upright("tyrogue", "SCRAPJOEY", 0.7, dict(
        s=1.1, hip=3.6, body=(5, 6, 4.6), FUR="#B88A5A", FUR_L="#E0B888", FUR_D="#7A5430", BELLY="#F0DCB8", belly_size=(3.2, 4.4), FACE="#E8C898", face_line=0.4,
        head=(4.4, 4, 4.4), head_y=-0.4, snout=(1.6, 2.2, 1.8), NOSE="#2A1A14", MUZZLE="#E8C898", ears=("rabbit", 1.8, 4.2), EAR_IN="#F0B0A0", ear_x=1.4,
        ear_rot=[-8, 6, -10], IRIS="#2A1A10", IRIS_L="#6A4A2A", eye_in=0.7, eye_y=0.6, eye_grid=["LL", "IP"], arms=(2.2, 2.2, 1.4), handm=wrap, hand_w=2.2, hand_h=2,
        leg_w=1.8, foot=4.4, FOOT="#7A5430", toes=2, thigh=3, tail={"segs": [(2, 2, 2.6), (1.6, 1.6, 2.6), (1.2, 1.2, 2.4)], "rests": [-50, -20, -10], "y": 0.6},
        moves=("TACKLE", "FOCUS ENERGY"), anim="hopper", hop=0.42, height=1.6, guard=50))


def twirlroo():
    """A rock-wallaby who fights upside down: grey with a black-banded tail as thick as a pillar, its tip hardened to a
    smooth stone pad it spins on like a top, kicking all round with its long feet; white wraps on its wrists."""
    wrap = Mat("#F4F0E8", lambda a: rgb("#C8C0B0") if a.row % 2 == 0 else None, noise=0.02)
    tailm = Mat("#8A8278", lambda a: rgb("#1A1816") if (a.z % 2.4) < 0.9 else None, noise=0.02)
    spin = track(rotation=keys({0: [0, 0, 0], 0.15: [0, 90, 0], 0.3: [0, 180, 0], 0.45: [0, 270, 0], 0.6: [0, 360, 0]}))
    upright("hitmontop", "TWIRLROO", 1.4, dict(
        s=1.5, hip=4, body=(5.4, 6.4, 5), FUR="#8A8278", FUR_L="#B8B0A6", FUR_D="#5A544C", BELLY="#E8E0D4", belly_size=(3.4, 4.4), FACE="#D8D0C4", face_line=0.4,
        head=(4.4, 4, 4.4), head_y=-0.4, snout=(1.6, 2.2, 1.8), NOSE="#1A1816", MUZZLE="#D8D0C4", ears=("rabbit", 1.8, 3.4), EAR_IN="#E8B8A8", ear_x=1.4,
        ear_rot=[-8, 6, -14], IRIS="#3A2A1A", IRIS_L="#8A6A4A", eye_in=0.7, eye_y=0.6, eye_grid=["LL", "IP"], arms=(2.4, 2.4, 1.6), handm=wrap, hand_w=2.2, hand_h=2,
        leg_w=2, foot=5, FOOT="#3A3630", toes=2, thigh=3.2,
        tail={"segs": [(2.8, 2.8, 2.6), (2.6, 2.6, 2.6), (2.4, 2.4, 2.4)], "rests": [-56, -20, -14], "y": 0.6, "mat": tailm, "tip": "#C8C0B4"},
        moves=("ROLLING KICK", "TRIPLE KICK"), guard=40, anim_extras={"attack": {"body": spin}, "special": {"body": spin}}))


# ======================================================================================================
# The babies: FROSTLET, NUMBOLT, EMBERNEWT (in place of Smoochum, Elekid, Magby)
# ======================================================================================================

def frostlet():
    """An ermine kit in its winter coat: snow-white all over but for the jet-black tip of its tail, a rosy pink nose and
    a little pink pout it gives everyone, violet eyes, round ears, a frosty ruff at its throat."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        b.cubes.append(Cube(S([-p["bw"] / 2 - 0.3, p["hip"] + p["bh"] - 1.6, -p["bd"] / 2 - 0.4]), [n((p["bw"] + 0.6) * s), n(1.4 * s), n((p["bd"] + 0.8) * s)],
                            Mat("#E8F4FF", lambda a: rgb("#C8E4FF") if a.row % 2 else None, glow=lambda a: rgb("#E8F8FF") if a.col % 3 == 0 else None, noise=0.02)))
    upright("smoochum", "FROSTLET", 0.4, dict(
        s=1.0, hip=2.4, body=(4.4, 4.4, 4), FUR="#F4F6FA", FUR_L="#FFFFFF", FUR_D="#C8D0DC", BELLY="#FFFFFF", FACE=None, head=(5.4, 4.6, 4.6), head_y=-0.4,
        snout=(1.2, 2, 1.4), NOSE="#F08AA0", MUZZLE="#FFFFFF", MOUTH="#E86A8A", ears=("round", 2, 2), EAR_IN="#F0C8D0", IRIS="#8A4AC8", IRIS_L="#C89AF0",
        eye_in=0.7, eye_y=0.56, eye_grid=["LL", "IP"], arms=(1.4, 1.2, 1.2), leg_w=1.4, foot=2.4, toes=2, thigh=2, FOOT="#E8ECF4",
        tail={"segs": [(1.2, 1.2, 2), (1.2, 1.2, 2)], "rests": [20, 20], "y": 0.6, "tip": "#141418"}, extra=extra, moves=("POUND", "POWDER SNOW"),
        anim="hopper", hop=0.4, height=1.2))


def numbolt():
    """A numbat joey that crackles: sunny yellow-orange fur banded across its back and rump with black stripes, a cream
    chest, a long pointed snout, a dark stripe through each eye, and a bushy tail that bristles with static."""
    def coat(at, base):
        if at.face in ("top", "back", "nx", "px") and at.z > -0.6 and (at.y % 1.8) < 0.7 and at.y > 4: return rgb("#1A1410")
        return None

    def face_coat(a):
        if a.face == "front" and 0.5 < a.ly < 0.62: return rgb("#1A1410")
        return None
    tailm = Mat("#F0C040", brush("#F0C040", "#FFE888", "#B88A20", tip="#FFF8C0"), glow=lambda a: rgb("#FFF8A0") if a.lz > 0.85 else None, noise=0, edge=0)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        tb = bone_of(m, p["tail"][-1])
        tb.cubes += [Cube([-1.8 * s, tb.pivot[1], tb.pivot[2] - 0.4], [n(3.6 * s), 0, n(2.4 * s)], tailm), Cube([0, tb.pivot[1] - 1.8 * s, tb.pivot[2] - 0.4], [0, n(3.6 * s), n(2.4 * s)], tailm)]
    upright("elekid", "NUMBOLT", 0.6, dict(
        s=1.1, hip=3, body=(4.6, 5.4, 4.4), FUR="#F0B83A", FUR_L="#FFDC7A", FUR_D="#B8801A", BELLY="#FFF0C8", coat=coat, belly_size=(2.8, 3.6), FACE="#FFE8B0",
        face_coat=face_coat, face_line=0.4, head=(4.4, 3.8, 4), head_y=-0.4, snout=(2.2, 1.6, 1.4), NOSE="#1A1410", MUZZLE="#FFE8B0", ears=("round", 1.6, 1.8),
        EAR_IN="#E8A060", IRIS="#2A1A0A", IRIS_L="#7A5A2A", eye_in=0.6, eye_y=0.58, eye_grid=["PW", "PP"], arms=(1.8, 1.8, 1.4), HAND="#B8801A", claws=2,
        leg_w=1.6, foot=2.8, toes=3, FOOT="#B8801A", thigh=2.4, tail={"segs": [(1.2, 1.2, 2), (1.2, 1.2, 2)], "rests": [40, 30], "y": 0.8}, extra=extra,
        moves=("QUICK ATTACK", "THUNDERPUNCH"), guard=40))


def embernewt():
    """A fire salamander's eft: glossy jet-black skin splashed with bright orange-gold blotches that glow faintly warm,
    a round head with big dark eyes, little four-toed hands, and a short tail; it puffs embers when it sneezes."""
    def coat(at, base):
        if hash01("blot", int(at.x / 1.6), int(at.y / 1.6), int(at.z / 1.6)) < 0.3 and at.face != "bottom": return rgb("#F8A020")
        return None
    glowing = lambda a: rgb("#FFB840") if hash01("blot", int(a.x / 1.6), int(a.y / 1.6), int(a.z / 1.6)) < 0.3 and a.face != "bottom" else None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        for c in bone_of(m, "body").cubes + bone_of(m, "head").cubes:
            if not c.mat.flat:
                c.mat.glow = glowing
    upright("magby", "EMBERNEWT", 0.7, dict(
        s=1.1, hip=2.6, body=(5, 5.4, 4.6), FUR="#1E1A1E", FUR_L="#3E363E", FUR_D="#0A080A", BELLY="#2A2228", coat=coat, belly_patch=False, FACE=None,
        face_coat=lambda a: rgb("#F8A020") if hash01("blot", int(a.x / 1.6), int(a.y / 1.6), int(a.z / 1.6)) < 0.3 and a.face in ("top", "nx", "px") else None,
        head=(5.4, 4, 5), head_y=-0.6, nose=False, mouth_y=0.2, mouth_w=3.4, MUZZLE="#2A2228", MOUTH="#E8501A", ears=None, IRIS="#1A0E06", IRIS_L="#6A3A1A",
        EYE_SKIN="#1E1A1E", eye_in=1.0, eye_y=0.6, eye_grid=["PW", "PP"], arms=(1.6, 1.6, 1.4), HAND="#2A2228", fingers=3, leg_w=1.6, foot=2.6, toes=3, FOOT="#2A2228",
        thigh=2.2, tail={"segs": [(2, 1.8, 2), (1.6, 1.4, 2), (1.2, 1, 2)], "rests": [-30, -10, 0], "y": 0.6}, extra=extra, moves=("EMBER", "SMOG"), guard=30))


# ======================================================================================================
# The dairy yak: DAIRYAK (in place of Miltank)
# ======================================================================================================

def dairyak():
    """A dairy yak of the high pastures: a short sturdy body, cream fur patched with rosy brown, a shaggy skirt of
    longer hair, a fringe tumbling over its brow between two short curving horns, a pink nose and a big copper bell on
    a collar round its neck."""
    FUR, FUR_L, FUR_D = "#F0E0C8", "#FFF4E4", "#C8B498"
    hang = curtain(FUR, FUR_L, FUR_D)
    hornm = Mat("#E8DCC8", lambda a: rgb("#5A4A3A") if a.ly > 0.7 else None, noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        top, bl, bw = p["top"], p["bl"], p["bw"]
        for x in (-bw / 2 - 0.2, bw / 2 + 0.2):
            b.cubes.append(Cube(S([x, top - 1.2, -bl / 2 + 0.6]), [0, n(3 * s), n((bl - 1.2) * s)], hang))
        hd = bone_of(m, "head")
        hy, hz, hl, hh = p["hy"], p["hz"], p["hl"], p["hh"]
        hd.cubes.append(Cube(S([-1.8, hy + hh * 0.5, hz - hl - 0.1]), [n(3.6 * s), n(hh * 0.5 * s), 0], curtain("#A87A5A", "#D8A880", "#6A4A30")))
        for x in (-1, 1):
            hd.cubes += [Cube(S([x * 2.2 - 0.6, hy + hh - 0.6, hz - hl * 0.5]), [n(1.2 * s), n(1.2 * s), n(1.2 * s)], hornm),
                         Cube(S([x * 3.2 - 0.5, hy + hh, hz - hl * 0.5]), [1, n(2 * s), 1], hornm, [0, 0, -x * 30], S([x * 3.2, hy + hh, hz - hl * 0.5]))]
        nb = bone_of(m, "neck")
        bell = Mat("#C87A3A", lambda a: rgb("#F0B060") if a.face == "top" or a.col == 0 else None, noise=0.01)
        nb.cubes += [Cube(S([-p["hw"] * 0.42, p["hy"] - 1.8, p["hz"] - 1.2]), [n(p["hw"] * 0.84 * s), 1, n(2.4 * s)], plain("#8A3A2A")),
                     Cube(S([-1, p["hy"] - 4, p["hz"] - 1.6]), [n(2 * s), n(2.2 * s), n(2 * s)], bell)]
    quad("miltank", "DAIRYAK", 1.2, dict(
        s=1.5, body=(6.6, 5.8, 9.4), top=3.8, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#FFF4E4", coat=spot_coat("#A86A4A", 2.6, 0.24, faces=("top", "nx", "px", "back")),
        FACE=FUR, head=(4.6, 4.2, 4.4), snout=2.2, snout_w=1.1, snout_h=2.2, MUZZLE="#F4C8C8", NOSE="#E87A9A", neck=2, neck_up=-0.4, neck_tilt=-4, head_tilt=10,
        ears=("round", 1.8, 1.4), EAR=FUR, EAR_IN="#F0C0C0", ear_rot=[-4, 30, -70], IRIS="#2A1A10", IRIS_L="#6A4A3A", eye_grid=["LL", "IP"], eye_y=0.55, leg_w=2,
        toes=2, foot=Mat("#4A3A30", noise=0.01), tail={"segs": [(1, 1, 2.6), (1, 1, 2.4)], "rests": [-60, -10], "y": 0.85, "tip": "#A86A4A"}, extra=extra,
        moves=("ROLLOUT", "MILK DRINK"), walk=0.6, stride=24, k=0.6))


# ======================================================================================================
# The silkie hen: BLISSHEN (in place of Blissey; from NESTHEN)
# ======================================================================================================

def blisshen():
    """The mother hen grown into a silkie: a great soft cloud of a hen, her feathers fine as down in white blushed with
    rose, a pom-pom crest on her head, a dark blue face with turquoise ear patches, feathered feet, and in front of her a
    deep nest of straw holding three golden eggs."""
    from mons3 import bird_legs as _bl
    m = Model()
    s = 1.7
    S = S_(s)
    F, F_L, F_D = "#FBE4EA", "#FFFFFF", "#D8A8B8"
    fluff = Mat(F, fur(F, F_L, F_D, belly="#FFFFFF", below=4 * s, speck=0.9), noise=0.04)
    face = Mat(F, lambda a: rgb("#3A4A8A") if a.face == "front" and 0.25 < a.ly < 0.75 and 0.15 < a.lx < 0.85 else
               (rgb("#4AC8C0") if a.face in ("nx", "px") and 0.3 < a.ly < 0.6 and a.lz < 0.4 else None), noise=0.03)
    beak = Mat("#5A5A6A", noise=0.01, edge=0)
    straw = Mat("#D8B060", lambda a: rgb("#A8803A") if (a.col + a.row) % 3 == 0 else (rgb("#F0D088") if (a.col * 2 + a.row) % 5 == 0 else None), noise=0.02)
    egg = Mat("#F8E4A0", lambda a: rgb("#E8A040") if hash01("eg", round(a.x * 2), round(a.y * 2), round(a.z * 2)) < 0.1 else mix("#E8C870", "#FFF8E0", a.ly), noise=0.01)
    legm = Mat("#5A5A6A", noise=0.02, edge=0)
    sec = Mat(F, feather_plane(F_D, F, F_L, edge="#FFFFFF"), noise=0.015, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 5, 0]), blob(S([-5, 1.5, -4.6]), S([10, 9, 9.2]), fluff, b=2))
    m.bone("head", "body", S([0, 10, -1.5]), face_blob(S([-2.8, 9.6, -5.2]), S([5.6, 4.2, 5]), face) + blob(S([-3.2, 13.2, -4.6]), S([6.4, 3.6, 5.4]), fluff, b=1))
    m.bone("beak", "head", S([0, 11, -5.2]), [Cube(S([-0.7, 10.6, -6.4]), [n(1.4 * s), 1, n(1.4 * s)], beak)])
    m.bone("beak_lower", "head", S([0, 10.4, -5.2]), [Cube(S([-0.5, 10.0, -6]), [n(1 * s), 1, n(1 * s)], beak)])
    m.eyes("head", x_inner=0.6 * s, y=11.4 * s, z=-5.2 * s, w=2, h=2, iris="#1A1420", pupil="#08060A", skin="#3A4A8A", iris_light="#5A4A6A", grid=["PW", "PP"])
    m.pair("wing_right", "body", S([-5.1, 9, -1.5]), [Cube(S([-5.2, 4.4, -2]), [0, n(4.6 * s), n(5.4 * s)], sec), Cube(S([-5.4, 5, -1]), [0, n(3.6 * s), n(4.4 * s)], sec)],
           rotation=[0, 0, 6])
    bone_of(m, "body").cubes += blob(S([-4, 1.6, -7.4]), S([8, 3.6, 3.8]), straw) + sum([blob(S([x - 1.2, 3.8, -6.8]), S([2.4, 3, 2.2]), egg) for x in (-2.4, 0, 2.4)], [])
    m.bone("tail", "body", S([0, 8, 4.2]), [Cube(S([-1.8, 7, 4]), [n(3.6 * s), 0, n(3.4 * s)], sec, [-40 + i * 20, 0, 0], S([0, 8, 4.2])) for i in range(3)], rotation=[-20, 0, 0])
    _bl(m, 2 * s, 2.4 * s, 0.4 * s, 0.8 * s, 1.6 * s, 1, fluff, legm, Mat("#3A3A48", noise=0, edge=0), toe_len=2)
    fx_for(m, s)
    moves(m, "EGG BOMB", "SOFTBOILED")
    A, info = bird("blisshen", m, walk=0.6, stride=26, k=s, flap=0.3,
                   extras={"walk": {"shin_right": track(rotation=[wave(0.6, 20), 0, 0]), "shin_left": track(rotation=[wave(0.6, 20, 180), 0, 0])}})
    write("blissey", "BLISSHEN", m, A, info, 1.5)


def main(names):
    for f in ALL8:
        if not names or f.__name__ in names:
            f()
    import mons4
    mons4.refresh_names()


ALL8 = (musklet, tundramusk, reeflet, archerfin, inkcuttle, parcelpuff, glidefin, bladeheron, ashcub, scorchyena, frondking, hippet, terrahippo,
        creasecrane, kuduspire, dabblecoat, scrapjoey, twirlroo, frostlet, numbolt, embernewt, dairyak, blisshen)

if __name__ == "__main__":
    main(set(sys.argv[1:]))
