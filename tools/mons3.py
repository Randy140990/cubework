"""
Retromon's own creatures, part three: the Kanto creatures beyond the first batches, built the same way as mons.py and
mons2.py (cubes painted in 3D, our own designs, the same species underneath).

Run: python3 tools/mons3.py [names]   (all if none)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, face_blob, hash01, mix, rgb, track, wave, keys, write  # noqa: E402
from monanims import crawler, cocoon, insect, serpent, four_legs, hopper, flyer, burrower, biped  # noqa: E402
from mons import leaf_mat, sharp_claw_mat, fx_for, open_mouth, stout_legs, whisker_mat, dragon_wing, membrane_mat, shag_mat  # noqa: E402
from monlib import flame  # noqa: E402
from mons2 import fur, brush, n  # noqa: E402


# ---------- shared painting ----------

def grub_paint(base, light, dark, belly, ring, below=1.2, spot=None):
    """A larva's skin: lighter along its back, a pale belly, a dark ring at the back of each segment, and (spot) a round
    spot on each side of each segment."""
    B, Lt, Dk, Be, R = rgb(base), rgb(light), rgb(dark), rgb(belly), rgb(ring)

    def f(at):
        if at.face == "bottom" or at.y < below:
            return Be
        c = at.cube
        if at.face in ("px", "nx", "top") and (c.origin[2] + c.size[2]) - at.z < 0.6:
            return R
        if spot and at.side and abs(at.lz - 0.45) < 0.22 and abs(at.ly - 0.55) < 0.22:
            return rgb(spot) if abs(at.lz - 0.45) < 0.14 and abs(at.ly - 0.55) < 0.14 else mix(spot, base, 0.5)
        if at.face == "top":
            return mix(B, Lt, 0.55)
        return mix(Dk, B, 0.35 + 0.65 * at.ly)
    return f


def shell_paint(base, light, dark, seam, facets=True):
    """A hard shell (a chrysalis, a cocoon): glossy facets, a pale line along the top edge of each plate, dark seams."""
    B, Lt, Dk, Sm = rgb(base), rgb(light), rgb(dark), rgb(seam)

    def f(at):
        c = at.cube
        if at.face != "top" and (c.origin[1] + c.size[1]) - at.y < 0.6:
            return Lt
        if at.face != "bottom" and at.y - c.origin[1] < 0.5:
            return Sm
        k = 0.45 + 0.55 * at.ly
        col = mix(Dk, B, k)
        if facets and at.face in ("front", "back") and int(at.x + 50) % 3 == 0:
            col = mix(col, Lt, 0.2)
        return col
    return f


def compound_grid(w, h):
    """A big insect's eye drawn as facets: alternating shades, a glint at the top inner corner."""
    rows = []
    for r in range(h):
        row = "".join("J" if (r + c) % 2 == 0 else "I" for c in range(w))
        rows.append(row)
    rows[0] = "W" + rows[0][1:]
    return rows


def wing_paint(base, vein, edge, root_x, span, spot=None, spot_core=None, hind=False, tint=None, lace=False):
    """An insect's wing on a flat part lying out from the body: broad at the root, rounding to the tip, veins fanning out
    from the root, a dark border, and (spot) an eye-spot near the tip. tint: a colour washing in from the root."""
    B, V, E = rgb(base), rgb(vein), rgb(edge)

    def f(at):
        s = (abs(at.x) - root_x) / span
        cc = at.lz
        if hind:
            k = ((s - 0.42) / 0.62) ** 2 + ((cc - 0.45) / 0.52) ** 2
        else:
            k = ((s - 0.46) / 0.58) ** 2 + ((cc - 0.42) / 0.5) ** 2
        if k > 1.0 and not (s < 0.22 and 0.08 < cc < 0.7):
            return CLEAR
        if k > 0.8:
            return E
        if lace and (abs(((s * 11) % 1.0) - 0.5) < 0.09 or abs(((cc * 8 + s * 2) % 1.0) - 0.5) < 0.09):
            return V
        ang = math.atan2(cc - 0.42, s + 0.08)
        if abs(((ang * 6 / math.pi) % 1.0) - 0.5) < 0.07 and s > 0.08:
            return V
        if spot and (s - 0.62) ** 2 + (cc - 0.5) ** 2 < 0.012:
            return rgb(spot_core or edge)
        if spot and (s - 0.62) ** 2 + (cc - 0.5) ** 2 < 0.03:
            return rgb(spot)
        col = mix(B, "#FFFFFF", 0.25 * (1 - s))
        if tint:
            col = mix(col, tint, max(0.0, 0.5 - s) * 1.2)
        return col
    return Mat(base, f, noise=0.01, edge=0, flat=True)


def comb_mat(shaft, barb):
    """A feathery antenna on a flat part: a dark shaft up its front with fine barbs sweeping back from it."""
    S_, Bb = rgb(shaft), rgb(barb)

    def f(at):
        if at.lz < 0.22:
            return S_
        if (at.row % 2 == 0) and at.lz < 0.22 + 0.75 * (1 - at.ly):
            return Bb
        return CLEAR
    return Mat(shaft, f, noise=0.0, edge=0, flat=True)


def legs_pair(m, name, parent, x, y, z, length, mat, claw, ang=0.0):
    """A thin insect leg hanging from the body, with a hooked foot."""
    m.pair(name, parent, [-x, y, z], [Cube([-x - 0.5, y - length, z - 0.5], [1, length, 1], mat),
                                      Cube([-x - 0.5, y - length - 0.6, z - 1.4], [1, 0.8, 1.6], claw)], rotation=[ang, 0, 0])


def moves(m, attack, special):
    """The moves it uses on its buttons (their effects come from the move library)."""
    m.attack_name, m.attack_extras = attack, {}
    m.move_name, m.move_extras = special, {}


# ======================================================================================================
# The leaf grubs: LEAFGRUB > HUSKALIS > VEILMOTH (in place of Caterpie, Metapod, Butterfree)
# ======================================================================================================

def leafgrub():
    """A plump green grub with a leaf for a hood: a broad leaf curling back over its crown, two short antennae with
    sunny knobs, button eyes, a yellow spot on each side of each segment, stubby clawed legs at the front and soft pads
    behind, and a forked pair of leaflets at its tail."""
    m = Model()
    G, GL, GD, BELLY, RING, SPOT = "#7CC650", "#B4E67C", "#3E8A38", "#F2E8B0", "#2E6A2C", "#FFE24A"
    skin = Mat(G, grub_paint(G, GL, GD, BELLY, RING, spot=SPOT), noise=0.02)
    face = Mat(G, lambda a: rgb("#E8F0B8") if a.face == "bottom" or (a.face == "front" and a.y < 3.2) else
               (mix(G, GL, 0.5) if a.face == "top" else None), noise=0.02)
    jaw_m = Mat("#E8F0B8", open_mouth("#8A3A3A", "#C8D898"), noise=0.02)
    mand = Mat("#4A3A22", noise=0.01, edge=0)
    knob = Mat("#FFD23A", lambda a: rgb("#FFF0A0") if a.face == "top" else None, noise=0.01, edge=0)
    stalk = Mat(GD, noise=0.01, edge=0)
    legm = Mat(GD, noise=0.02, edge=0)
    pad = Mat(BELLY, lambda a: rgb("#D8CC90") if a.face == "bottom" else None, noise=0.02, edge=0)
    claw = Mat("#3A2A1A", noise=0.0, edge=0)
    hood = leaf_mat("#9AD860", "#3E8A3A", "#E0F4B0", "z")
    tleaf = leaf_mat("#9AD860", "#3E8A3A", "#E0F4B0", "y")

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 3, -3], blob([-3, 0.4, -5.2], [6, 5, 4.4], skin) +
           [Cube([-2.7, 0, -4.6], [1, 1, 1], legm), Cube([1.7, 0, -4.6], [1, 1, 1], legm), Cube([-2.7, 0, -2.6], [1, 1, 1], legm),
            Cube([1.7, 0, -2.6], [1, 1, 1], legm), Cube([-2.7, 0, -5.3], [1, 0.5, 0.7], claw), Cube([1.7, 0, -5.3], [1, 0.5, 0.7], claw),
            Cube([-2.7, 0, -3.3], [1, 0.5, 0.7], claw), Cube([1.7, 0, -3.3], [1, 0.5, 0.7], claw)])
    m.bone("head", "body", [0, 4, -5], face_blob([-3.25, 1, -10], [6.5, 6, 5], face))
    m.bone("jaw", "head", [0, 2.2, -8], [Cube([-1.5, 1.2, -10.4], [3, 1, 3], jaw_m), Cube([-2.4, 1.0, -10.7], [1, 1, 1], mand),
                                       Cube([1.4, 1.0, -10.7], [1, 1, 1], mand)])
    m.eyes("head", x_inner=0.9, y=4.4, z=-10, w=2, h=2, iris="#1A1A14", pupil="#0A0A06", skin=G, grid=["PW", "PP"])
    m.bone("hood", "head", [0, 7, -8], [Cube([-3.5, 7, -9.5], [7, 0, 10], hood, [-34, 0, 0], [0, 7, -8.5]),
                                       Cube([-3.6, 6.6, -8], [0, 4, 5], tleaf, [0, 0, 20], [-3.4, 6.6, -6]),
                                       Cube([3.6, 6.6, -8], [0, 4, 5], tleaf, [0, 0, -20], [3.4, 6.6, -6])])
    m.pair("antenna_right", "head", [-1.4, 6.8, -9], [Cube([-1.9, 6.8, -9.4], [1, 3, 1], stalk), Cube([-2.4, 9.6, -9.9], [2, 2, 2], knob)],
           rotation=[-12, 0, -14])
    segs = []
    prev, z, y, w, h = "body", -1.0, 2.5, 5.8, 4.8
    for i in range(4):
        name = f"seg{i + 1}"
        cubes = blob([-w / 2, 0.4, z - 0.2], [w, h, 3.9], skin)
        if i in (1, 2):
            cubes += [Cube([-w / 2 + 0.3, 0, z + 0.8], [1.2, 0.8, 1.6], pad), Cube([w / 2 - 1.5, 0, z + 0.8], [1.2, 0.8, 1.6], pad)]
        m.bone(name, prev, [0, y, z], cubes)
        segs.append(name)
        prev, z, y, w, h = name, z + 3.6, y - 0.1, w - 0.5, h - 0.4
    m.bone("tail", prev, [0, 2.4, z], [Cube([0, 2.6, z - 1], [0, 4.5, 3], tleaf, [28, 0, 22], [0, 2.6, z]),
                                     Cube([0, 2.6, z - 1], [0, 4.5, 3], tleaf, [28, 0, -22], [0, 2.6, z])])
    fx_for(m, 0.6)
    moves(m, "TACKLE", "STRING SHOT")
    A, info = crawler("leafgrub", m, segs + ["tail"], walk=0.8, k=0.6, extras={"idle": {"hood": track(rotation=[wave(1.6, 4), 0, 0])},
                                                                               "walk": {"hood": track(rotation=[wave(0.8, 6), 0, 0])}})
    write("caterpie", "LEAFGRUB", m, A, info, 0.42)


def huskalis():
    """A jade chrysalis standing up like a curled bud, wrapped round the back in a big dried leaf held on with silk; gold
    seams between its plates, and sleepy eyes peering out from under heavy lids."""
    m = Model()
    J, JL, JD, SEAM = "#5FB56A", "#B8EAB0", "#2E7040", "#D8B04A"
    shell = Mat(J, shell_paint(J, JL, JD, SEAM), noise=0.015)
    dried = leaf_mat("#C8A25A", "#7A5A2A", "#E8D08A", "y")
    silk = Mat("#F4F2EC", lambda a: CLEAR if a.col % 2 else None, noise=0.0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 0, 0], blob([-3, 0, -2.6], [6, 3, 5.2], shell) + blob([-3.8, 3, -3.1], [7.6, 5, 6.2], shell) +
           blob([-3.6, 8, -3.3], [7.2, 4, 6.2], shell))
    m.bone("head", "body", [0, 12, 0], blob([-3.1, 12, -3.5], [6.2, 3, 5.8], shell) +
           [Cube([-2.2, 15, -2.9], [4.4, 2, 4.6], shell), Cube([-1.2, 16.6, -2.6], [2.4, 1.6, 3], shell, [-14, 0, 0], [0, 16.6, -1])])
    m.eyes("head", x_inner=0.8, y=12.6, z=-3.5, w=3, h=2, iris="#2A5A2A", pupil="#0A1A0A", skin=J, iris_light="#7ACB6A", grid=["LLL", "IPW"])
    m.bone("wrap", "body", [0, 2, 3], [Cube([-4.5, 1.5, 3.3], [9, 15, 0], dried),
                                      Cube([-4.4, 2.5, -2.5], [0, 11, 6], dried, [0, -8, 0], [-4.4, 2.5, 3]),
                                      Cube([4.4, 2.5, -2.5], [0, 11, 6], dried, [0, 8, 0], [4.4, 2.5, 3])])
    m.bone("silk", "head", [0, 17, -1], [Cube([-0.5, 17.5, -1.5], [0, 5, 1], silk, [0, 0, 10], [0, 17.5, -1]),
                                        Cube([0.5, 17.5, -1.2], [0, 6, 1], silk, [0, 0, -8], [0, 17.5, -1])])
    fx_for(m, 0.8)
    moves(m, "TACKLE", "HARDEN")
    A, info = cocoon("huskalis", m, k=0.8, extras={"idle": {"silk": track(rotation=[wave(2, 4), 0, wave(3, 5)])}})
    write("metapod", "HUSKALIS", m, A, info, 0.72)


def veilmoth():
    """A soft, fuzzy moth: a lavender-grey body with a cream ruff, great faceted red eyes, feathery antennae, and four
    pale wings veined in violet with an eye-spot on each forewing and a teal edge to each hind wing. Its legs hang
    beneath it as it hovers, little hooked feet curled."""
    m = Model()
    FZ, FZL, FZD, CREAM = "#B8A8CC", "#DCD0EA", "#7A6A90", "#F4ECD8"

    def fuzz(at):
        if at.face == "bottom":
            return mix(FZ, FZD, 0.3)
        c = mix(FZD, FZ, 0.4 + 0.6 * at.ly)
        if hash01("fz", int(at.x * 2), int(at.y * 2), int(at.z * 2)) > 0.7:
            c = mix(c, FZL, 0.5)
        return c
    fur = Mat(FZ, fuzz, noise=0.03)
    ruff = Mat(CREAM, lambda a: mix(CREAM, "#D8CCB0", a.ly * 0.4), noise=0.03, edge=0)
    stripe = Mat(FZ, lambda a: rgb(CREAM) if int(a.z * 1.2) % 2 == 0 and a.face != "bottom" else None, fuzz, noise=0.03)
    legm = Mat("#5A4A6A", noise=0.01, edge=0)
    claw = Mat("#2A2030", noise=0.0, edge=0)
    jaw_m = Mat(FZD, open_mouth("#6A2A4A", FZ), noise=0.02)
    fore = wing_paint("#EEE8F8", "#5A4A7A", "#3A2E50", 3, 17, spot="#8A5AD8", spot_core="#1A1020", tint="#C8B8E8")
    hind = wing_paint("#E8F0F4", "#4A6A7A", "#3AA8A0", 3, 12, hind=True, tint="#C8E4E8")
    comb = comb_mat("#3A2E48", "#8A7AA8")

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 12, 0], blob([-3, 9, -3], [6, 6, 6], fur) + [Cube([-3.6, 9.5, -3.8], [7.2, 5.5, 1.2], ruff)])
    m.bone("head", "body", [0, 13, -3], blob([-3, 10.8, -7.6], [6, 5.6, 4.8], fur))
    m.bone("jaw", "head", [0, 11.4, -6.5], [Cube([-1, 10.6, -7.9], [2, 1, 2], jaw_m)])
    m.eyes("head", 3, 12.2, -7.6, h=4, corner=(2, 2), half=3, iris="#C0303A", pupil="#4A0A14", skin=FZ, iris_light="#F07A80",
           grid=compound_grid(4, 4))
    m.pair("antenna_right", "head", [-1.2, 16, -7], [Cube([-1.2, 16, -7.5], [0, 7, 3], comb)], rotation=[-28, 0, -18])
    m.bone("abdomen", "body", [0, 11, 3], blob([-2.5, 8.6, 2.6], [5, 5, 4], stripe), rotation=[-10, 0, 0])
    m.bone("abdomen2", "abdomen", [0, 10.8, 6.5], [Cube([-2, 8.8, 6.3], [4, 4, 3.6], stripe)], rotation=[-8, 0, 0])
    m.bone("abdomen3", "abdomen2", [0, 10.6, 9.8], [Cube([-1.5, 9.2, 9.7], [3, 3, 3], stripe)], rotation=[-8, 0, 0])
    legs_pair(m, "leg_1_right", "body", 2.2, 9.5, -1.5, 5, legm, claw, ang=-10)
    legs_pair(m, "leg_2_right", "body", 2.2, 9.5, 1.5, 5.5, legm, claw, ang=10)
    m.pair("wing_right", "body", [-3, 14.2, -1], [Cube([-20, 14.2, -4.5], [17, 0, 13], fore)], rotation=[0, 0, 38])
    m.pair("hindwing_right", "body", [-3, 13.4, 2], [Cube([-15, 13.4, 1], [12, 0, 10], hind)], rotation=[0, 0, 26])
    fx_for(m, 0.9)
    moves(m, "GUST", "CONFUSION")
    A, info = insect("veilmoth", m, flap=0.55, amp=48, k=0.9, legs=("leg_1_right", "leg_1_left", "leg_2_right", "leg_2_left"))
    write("butterfree", "VEILMOTH", m, A, info, 1.15)


# ======================================================================================================
# The lance wasps: STINGLET > BARBCASE > LANCEWASP (in place of Weedle, Kakuna, Beedrill)
# ======================================================================================================

def stinglet():
    """A striped amber grub, a hornet's larva: dark umber rings, a pale belly, a curved black stinger rising from its
    crown and a pair of barbs at its tail, black button eyes and small working mandibles."""
    m = Model()
    AM, AML, AMD, BELLY, RING = "#E2A23A", "#F6CE78", "#A0641E", "#FFF0C8", "#4E2E14"
    skin = Mat(AM, grub_paint(AM, AML, AMD, BELLY, RING), noise=0.02)
    face = Mat(AM, lambda a: rgb("#F6D8A0") if a.face == "bottom" or (a.face == "front" and a.y < 3.2) else
               (mix(AM, AML, 0.5) if a.face == "top" else None), noise=0.02)
    jaw_m = Mat("#F6D8A0", open_mouth("#8A2A1A", "#D8B070"), noise=0.02)
    mand = Mat("#3A2210", noise=0.01, edge=0)
    spike = Mat("#2A1E1A", lambda a: rgb("#E8DCC8") if a.ly > 0.7 else None, noise=0.01, edge=0)
    legm = Mat(AMD, noise=0.02, edge=0)
    pad = Mat(BELLY, lambda a: rgb("#E0C890") if a.face == "bottom" else None, noise=0.02, edge=0)
    claw = Mat("#2A1A10", noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 3, -3], blob([-3, 0.4, -5.2], [6, 5, 4.4], skin) +
           [Cube([-2.7, 0, -4.6], [1, 1, 1], legm), Cube([1.7, 0, -4.6], [1, 1, 1], legm), Cube([-2.7, 0, -2.6], [1, 1, 1], legm),
            Cube([1.7, 0, -2.6], [1, 1, 1], legm), Cube([-2.7, 0, -5.3], [1, 0.5, 0.7], claw), Cube([1.7, 0, -5.3], [1, 0.5, 0.7], claw),
            Cube([-2.7, 0, -3.3], [1, 0.5, 0.7], claw), Cube([1.7, 0, -3.3], [1, 0.5, 0.7], claw)])
    m.bone("head", "body", [0, 4, -5], face_blob([-3.25, 1, -10], [6.5, 6, 5], face))
    m.bone("jaw", "head", [0, 2.2, -8], [Cube([-1.5, 1.2, -10.4], [3, 1, 3], jaw_m), Cube([-2.5, 0.9, -10.9], [1, 1, 1.4], mand, [0, 20, 0], [-2, 1.4, -10.2]),
                                       Cube([1.5, 0.9, -10.9], [1, 1, 1.4], mand, [0, -20, 0], [2, 1.4, -10.2])])
    m.eyes("head", x_inner=0.9, y=4.3, z=-10, w=2, h=2, iris="#14100C", pupil="#060404", skin=AM, grid=["PW", "PP"])
    m.bone("horn", "head", [0, 7, -8], [Cube([-0.75, 6.8, -8.6], [1.5, 2.2, 1.5], spike, [-12, 0, 0], [0, 7, -8]),
                                       Cube([-0.5, 8.8, -9.0], [1, 2.2, 1], spike, [-30, 0, 0], [0, 8.8, -8.5]),
                                       Cube([-0.25, 10.6, -10.2], [0.5, 1.6, 0.5], spike, [-48, 0, 0], [0, 10.6, -9.8])])
    segs = []
    prev, z, y, w, h = "body", -1.0, 2.5, 5.8, 4.8
    for i in range(4):
        name = f"seg{i + 1}"
        cubes = blob([-w / 2, 0.4, z - 0.2], [w, h, 3.9], skin)
        if i in (1, 2):
            cubes += [Cube([-w / 2 + 0.3, 0, z + 0.8], [1.2, 0.8, 1.6], pad), Cube([w / 2 - 1.5, 0, z + 0.8], [1.2, 0.8, 1.6], pad)]
        m.bone(name, prev, [0, y, z], cubes)
        segs.append(name)
        prev, z, y, w, h = name, z + 3.6, y - 0.1, w - 0.5, h - 0.4
    m.bone("tail", prev, [0, 2.4, z], [Cube([-1.6, 2.6, z - 0.5], [1, 3, 1], spike, [35, 0, 18], [-1.1, 2.6, z]),
                                     Cube([0.6, 2.6, z - 0.5], [1, 3, 1], spike, [35, 0, -18], [1.1, 2.6, z])])
    fx_for(m, 0.6)
    moves(m, "POISON STING", "STRING SHOT")
    A, info = crawler("stinglet", m, segs + ["tail"], walk=0.75, k=0.6, extras={"idle": {"horn": track(rotation=[wave(1.6, 5), 0, 0])}})
    write("weedle", "STINGLET", m, A, info, 0.4)


def barbcase():
    """A hard golden cocoon like a lantern, banded with dark armour plates, short spikes along its ridges, its little
    arms folded tight, and narrow eyes glaring out under hard brows."""
    m = Model()
    GO, GOL, GOD, PLATE = "#E8B030", "#FFDC70", "#9A6A14", "#5E3E14"
    shell = Mat(GO, shell_paint(GO, GOL, GOD, PLATE), noise=0.015)
    plate = Mat(PLATE, shell_paint(PLATE, "#8A6430", "#3A2408", "#2A1A06", facets=False), noise=0.015)
    spike = Mat("#3A2610", lambda a: rgb("#E8D8B0") if a.ly > 0.65 else None, noise=0.01, edge=0)
    brow = Mat(PLATE, noise=0.01, edge=0)
    m.bone("root", None, [0, 0, 0])
    body = blob([-3, 0, -2.6], [6, 3, 5.2], shell) + blob([-4, 3, -3.2], [8, 3, 6.4], plate) + blob([-3.8, 6, -3.1], [7.6, 3, 6.2], shell) + \
        blob([-3.6, 9, -3], [7.2, 2.5, 6], plate)
    for y in (4.5, 10):
        for sx in (-1, 1):
            body.append(Cube([sx * 4.2 - 0.5, y, -0.5], [1, 2.2, 1], spike, [0, 0, 55 * sx], [sx * 4.2, y, 0]))
        body.append(Cube([-0.5, y - 0.2, 3.2], [1, 2.2, 1], spike, [60, 0, 0], [0, y, 3.4]))
    body += [Cube([-3.9, 5.2, -3.7], [1.6, 2.4, 1.4], shell, [0, 0, -12], [-3.2, 6.4, -3]), Cube([2.3, 5.2, -3.7], [1.6, 2.4, 1.4], shell, [0, 0, 12], [3.2, 6.4, -3])]
    m.bone("body", "root", [0, 0, 0], body)
    m.bone("head", "body", [0, 11.5, 0], blob([-3.2, 11.5, -3.3], [6.4, 3.5, 5.8], shell) +
           [Cube([-2.4, 15, -2.6], [4.8, 2, 4.4], plate), Cube([-0.6, 17, -1], [1.2, 2.4, 1.2], spike, [-15, 0, 0], [0, 17, -0.4])])
    m.eyes("head", x_inner=0.6, y=12.6, z=-3.3, w=3, h=2, iris="#E8E040", pupil="#2A1A04", skin=GO, iris_light="#FFF8A0", grid=["LLL", "IPW"])
    m.brows("head", [Cube([-3.6, 14.6, -3.8], [3.4, 1, 1], brow, [0, 0, 18], [-0.6, 15.1, -3.4])])
    fx_for(m, 0.8)
    moves(m, "POISON STING", "HARDEN")
    A, info = cocoon("barbcase", m, k=0.8)
    write("kakuna", "BARBCASE", m, A, info, 0.75)


def lancewasp():
    """A hunting wasp: an amber-and-umber banded body, a long drooping abdomen ending in a black stinger, great faceted
    red eyes, hooked mandibles, bent antennae, two forelegs ending in long lance blades, thin hind legs with hooked feet,
    and two pairs of narrow, amber-veined wings beating in a blur."""
    m = Model()
    AM, AML, AMD, BAND = "#E0A030", "#F8D070", "#9A6418", "#2A1A10"
    banded = Mat(AM, lambda a: rgb(BAND) if int(a.z * 0.7 + 50) % 2 == 0 and a.face != "bottom" else
                 (mix(AM, AML, 0.5) if a.face == "top" else mix(AMD, AM, 0.4 + 0.6 * a.ly)), noise=0.02)
    thor = Mat(AM, lambda a: rgb(BAND) if a.face == "top" and abs(a.x) < 1.2 else (mix(AM, AML, 0.4) if a.face == "top" else mix(AMD, AM, 0.4 + 0.6 * a.ly)),
               noise=0.02)
    headm = Mat(AM, lambda a: rgb("#F6D890") if a.face == "front" and a.y < 15.8 else None, noise=0.02)
    lance = Mat("#E8E4D8", lambda a: mix("#8A8478", "#FFFFFF", a.ly) if a.side else None, noise=0.01, edge=0.1)
    legm = Mat("#2A1A10", noise=0.01, edge=0)
    claw = Mat("#14100C", noise=0.0, edge=0)
    jaw_m = Mat(AMD, open_mouth("#6A1A10", AM), noise=0.02)
    mand = Mat("#2A1A10", noise=0.01, edge=0)
    stinger = Mat("#1A1410", lambda a: rgb("#D8D0C0") if a.lz < 0.3 else None, noise=0.0, edge=0)
    fore = wing_paint("#F0F0EC", "#9A7A40", "#7A5A2A", 3, 15, tint="#E8C878")
    hind = wing_paint("#F0F0EC", "#9A7A40", "#7A5A2A", 3, 11, hind=True, tint="#E8C878")
    ant = Mat("#2A1A10", noise=0.0, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 16, 0], blob([-3, 13, -3], [6, 6, 6], thor))
    m.bone("head", "body", [0, 17, -3], blob([-2.8, 14.6, -7.6], [5.6, 5, 4.8], headm))
    m.bone("jaw", "head", [0, 15.4, -6.5], [Cube([-1, 14.6, -8], [2, 1, 2], jaw_m), Cube([-2.4, 14.2, -8.6], [1, 1.2, 1.8], mand, [0, 25, 0], [-1.9, 14.8, -7.6]),
                                           Cube([1.4, 14.2, -8.6], [1, 1.2, 1.8], mand, [0, -25, 0], [1.9, 14.8, -7.6])])
    m.eyes("head", 2.8, 16, -7.6, h=3, corner=(2, 2), half=2.8, iris="#C02A1A", pupil="#4A0A06", skin=AM, iris_light="#F06A40",
           grid=compound_grid(4, 3))
    m.pair("antenna_right", "head", [-1.2, 19.4, -6.8], [Cube([-1.7, 19.4, -7.3], [1, 3, 1], ant), Cube([-1.7, 21.8, -9.6], [1, 1, 3], ant)],
           rotation=[-20, 0, -15])
    m.bone("abdomen", "body", [0, 14.6, 3], blob([-2.8, 11.2, 2.6], [5.6, 6, 5], banded), rotation=[-18, 0, 0])
    m.bone("abdomen2", "abdomen", [0, 14, 7.4], blob([-2.4, 10.4, 7.2], [4.8, 5.4, 4.6], banded), rotation=[-10, 0, 0])
    m.bone("abdomen3", "abdomen2", [0, 13.2, 11.6], [Cube([-1.7, 10.6, 11.4], [3.4, 3.8, 3.6], banded),
                                                     Cube([-0.5, 11.8, 14.8], [1, 1, 4], stinger)], rotation=[-8, 0, 0])
    m.pair("arm_right", "body", [-3, 14.5, -2], [Cube([-4.2, 10.5, -2.6], [1.4, 4.5, 1.4], legm),
                                                Cube([-4.5, 3.5, -3], [2, 7, 2], lance), Cube([-4.1, 1.2, -2.6], [1.2, 2.5, 1.2], lance)],
           rotation=[-35, 0, 8])
    legs_pair(m, "leg_1_right", "body", 2.5, 13.5, 0.5, 6, legm, claw, ang=12)
    legs_pair(m, "leg_2_right", "body", 2.5, 13.5, 2.5, 7, legm, claw, ang=25)
    m.pair("wing_right", "body", [-3, 19, -1.5], [Cube([-18, 19, -2.5], [15, 0, 5], fore)])
    m.pair("hindwing_right", "body", [-3, 18.6, 1.5], [Cube([-14, 18.6, 1.0], [11, 0, 4], hind)])
    fx_for(m, 1.2)
    moves(m, "FURY ATTACK", "TWINEEDLE")
    A, info = insect("lancewasp", m, flap=0.09, amp=40, k=1.0, legs=("leg_1_right", "leg_1_left", "leg_2_right", "leg_2_left"),
                     extras={"idle": {"arm_right": track(rotation=[wave(1, 6), 0, 0]), "arm_left": track(rotation=[wave(1, 6, 50), 0, 0])},
                             "battle": {"arm_right": track(rotation=[wave(0.5, 10, 0, -10), 0, 0]), "arm_left": track(rotation=[wave(0.5, 10, 90, -10), 0, 0])}})
    write("beedrill", "LANCEWASP", m, A, info, 1.6)


# ---------- more shared parts ----------

def taper_mat(base, tip, glow=None):
    """A spike or spire on crossed flat parts lying along z from its root: wide at the root, tapering to a sharp pale point."""
    def f(at):
        c = at.cube
        u = at.lx if c.size[0] else at.ly
        t = at.lz
        if abs(u - 0.5) > 0.5 * (1 - t) + 0.02:
            return CLEAR
        return mix(base, tip, min(1.0, t * 1.3))
    return Mat(base, f, noise=0.01, edge=0, flat=True, glow=(lambda a: rgb(glow) if a.lz > 0.75 else None) if glow else None)


def spike(x, y, z, L, rot, mat, qw=2):
    return [Cube([x - qw / 2, y, z], [qw, 0, L], mat, rot, [x, y, z]), Cube([x, y - qw / 2, z], [0, qw, L], mat, rot, [x, y, z])]


def scale_mat(base, light, dark):
    """An armour scale on a flat part lying along z (its root at the front): rounded at its tip, a pale ridge down its
    middle, darker at its rim."""
    B, Lt, Dk = rgb(base), rgb(light), rgb(dark)

    def f(at):
        u, t = at.lx if at.cube.size[0] else at.ly, at.lz
        hw = 0.5 if t < 0.55 else 0.5 * math.sqrt(max(0.0, 1 - ((t - 0.55) / 0.45) ** 2))
        d = abs(u - 0.5)
        if d > hw:
            return CLEAR
        if d > hw - 0.1 or t > 0.9:
            return Dk
        return Lt if d < 0.08 else mix(B, Lt, 0.3 * (1 - t))
    return Mat(base, f, noise=0.02, edge=0, flat=True)


def tail_bones(m, parent, y, z, segs, mat, rests):
    """A tail in joints: segs [(w, h, length)], each a box running back (+z) from the one before; rests: each joint's
    resting tilt (a positive tilt lifts the tail). Gives the bone names and where the tip ends (z)."""
    prev, names = parent, []
    for i, ((w, h, L), r) in enumerate(zip(segs, rests)):
        name = "tail" if i == 0 else f"tail{i + 1}"
        m.bone(name, prev, [0, y, z], [Cube([-w / 2, y - h / 2, z - 0.3], [w, h, L + 0.3], mat)], rotation=[r, 0, 0])
        prev, z = name, z + L
        names.append(name)
    return names, z


def ear_mat(fur_c, inner, shape="rabbit", tip=None, rim=None):
    """An ear on a flat part standing up (facing forward): its shape (rabbit: long and rounded; fennec: a great
    triangle; round), the fur on its back, the pale inner ear on its front, a coloured tip, and (rim) a glowing rim
    round the inner ear."""
    F, I = rgb(fur_c), rgb(inner)

    def hw(v):
        if shape == "fennec":
            return 0.5 * (1 - v) ** 0.75
        if shape == "round":
            return math.sqrt(max(0.0, 0.25 - (v - 0.5) ** 2))
        return 0.5 * math.sin(math.pi * min(1.0, v * 0.92 + 0.06)) ** 0.55

    def f(at):
        u, v = at.lx, at.ly
        h = hw(v)
        d = abs(u - 0.5)
        if d > h:
            return CLEAR
        if tip is not None and v > 0.82:
            return rgb(tip)
        if at.face == "front" and d < h * 0.58 and 0.08 < v < 0.86:
            return I
        if at.face == "front" and rim is not None and d < h * 0.78 and 0.05 < v < 0.9:
            return rgb(rim)
        return F

    def g(at):
        if rim is None or at.face != "front":
            return None
        u, v = at.lx, at.ly
        h = hw(v)
        d = abs(u - 0.5)
        return rgb(rim) if h * 0.58 <= d < h * 0.78 and 0.05 < v < 0.9 else None
    return Mat(fur_c, f, glow=g if rim else None, noise=0.015, edge=0, flat=True)


def glow_patch(colour, shape="crescent"):
    """A glowing mark on a flat part (a crescent on the brow, an ember on the forehead)."""
    def inside(at):
        x, y = at.lx - 0.5, at.ly - 0.5
        if shape == "crescent":
            return x * x + y * y < 0.2 and (x - 0.18) ** 2 + (y - 0.08) ** 2 > 0.12
        if shape == "diamond":
            return abs(x) + abs(y) < 0.48
        return abs(x) < 0.32 * (1 - (y + 0.5)) + 0.08 and y > -0.5
    return Mat(colour, lambda a: rgb(colour) if inside(a) else CLEAR, glow=lambda a: rgb(colour) if inside(a) else None, noise=0, edge=0, flat=True)


def snake_paint(base, light, dark, belly, chevron, below):
    """A snake's scales: a pale belly, the back marked with a chain of chevrons, sides shading down."""
    B, Lt, Dk, Be, Ch = rgb(base), rgb(light), rgb(dark), rgb(belly), rgb(chevron)

    def f(at):
        if at.face == "bottom" or at.y < below:
            return Be
        if at.face == "top":
            zz = (at.z % 4.0) / 4.0
            if abs(at.x) < 0.45 + 0.9 * abs(zz - 0.5) * 2 and abs(at.x) > 0.9 * abs(zz - 0.5) * 2 - 0.4:
                return Ch
            return mix(B, Lt, 0.3)
        return mix(Dk, B, 0.4 + 0.6 * at.ly)
    return f


def frill_mat(web, rib, edge, spot):
    """A great neck frill on a flat part facing forward: ribs fanning up from its root at the bottom middle, webbing
    between them, a dark rim, and a pair of eye-spots."""
    W, R, E, SP = rgb(web), rgb(rib), rgb(edge), rgb(spot)

    def f(at):
        x, y = (at.lx - 0.5) * 2, (1 - at.lz if at.cube.size[1] == 0 else at.ly)
        r = math.hypot(x * 0.9, y)
        if r > 1 or y < 0.05:
            return CLEAR
        if r > 0.88:
            return E
        ang = math.atan2(y, x)
        if abs(((ang * 7 / math.pi) % 1.0) - 0.5) < 0.09:
            return R
        for sx in (-0.45, 0.45):
            dd = (x - sx) ** 2 + (y - 0.55) ** 2
            if dd < 0.012:
                return rgb("#140C10")
            if dd < 0.035:
                return SP
        return mix(W, R, 0.25 * r)
    return Mat(web, f, noise=0.015, edge=0, flat=True)


# ======================================================================================================
# The vipers: COILET > FRILLASP (in place of Ekans, Arbok)
# ======================================================================================================

def viper(sid, name, stands, big):
    """A viper lying in curves, its front reared up. The small one slim and lilac with copper chevrons and a little copper
    spade at its tail; grown, long and deep violet with gold chevrons, a heavy brow, long fangs, and a great ribbed frill
    of teal and gold round its neck that flares when it hisses."""
    m = Model()
    s = 1.0 if not big else 1.7
    S = lambda v: [x * s for x in v]
    if not big:
        BASE, LT, DK, BELLY, CHEV = "#9A7AC0", "#C8B0E4", "#5E4A80", "#F0E0B0", "#C87A3A"
    else:
        BASE, LT, DK, BELLY, CHEV = "#5A3A7A", "#8A6AAA", "#2E1E44", "#F0DCA0", "#E0A030"
    skin = Mat(BASE, snake_paint(BASE, LT, DK, BELLY, CHEV, 0.6 * s), noise=0.02)
    headm = Mat(BASE, lambda a: rgb(BELLY) if a.face == "bottom" else (mix(BASE, LT, 0.35) if a.face == "top" else None), noise=0.02)
    jaw_m = Mat(BELLY, open_mouth("#8A2A3A", "#D8C090"), noise=0.02)
    fang = Mat("#FFFFF4", noise=0.0, edge=0)
    tongue = Mat("#D8304A", lambda a: CLEAR if (a.lz < 0.3 and abs(a.lx - 0.5) < 0.2) else None, noise=0, edge=0, flat=True)
    spade = Mat(CHEV, lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (1 - abs(a.lz - 0.5) * 2) + 0.05 else None, noise=0.01, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 1.5, -2]), [Cube(S([-1.6, 0, -2]), [round(3.2 * s), round(3 * s), round(4 * s)], skin)])
    segs, prev, z = [], "body", 2.0
    rests = (38, -52, -48, 42, 46, -36, -30) if big else (35, -50, -45, 40, 45, -35)
    for i, r in enumerate(rests):
        w, h = max(1.2, 3.0 - i * 0.28) * s, max(1.2, 2.8 - i * 0.24) * s
        nm = f"seg{i + 1}"
        cubes = [Cube([-w / 2, 0, z * s], [w, h, 4 * s], skin)]
        if i == len(rests) - 1:
            cubes.append(Cube([-1.5 * s, 0.4 * s, (z + 3.6) * s], [3 * s, 0, 3 * s], spade))
        m.bone(nm, prev, [0, 1.4 * s, z * s], cubes, rotation=[0, r, 0])
        segs.append(nm)
        prev, z = nm, z + 4
    r1, r2 = (-55, -22) if not big else (-70, -16)
    m.bone("neck", "body", S([0, 1.5, -2]), [Cube(S([-1.4, 0.2, -6]), [round(2.8 * s), round(2.8 * s), round(4.2 * s)], skin)], rotation=[r1, 0, 0])
    m.bone("neck2", "neck", S([0, 1.6, -6]), [Cube(S([-1.3, 0.3, -9.6]), [round(2.6 * s), round(2.6 * s), round(3.8 * s)], skin)], rotation=[r2, 0, 0])
    m.bone("head", "neck2", S([0, 1.8, -9.5]), face_blob(S([-2, 0.6, -14]), S([4, 3, 4.6]), headm), rotation=[-(r1 + r2) - 5, 0, 0])
    hd = m.bones[-1]
    hd.cubes += [Cube(S([-1.1, 0.5, -14.6]), [max(1, round(0.8 * s)), max(1, round(1.2 * s)), 1], fang), Cube(S([0.3, 0.5, -14.6]), [max(1, round(0.8 * s)), max(1, round(1.2 * s)), 1], fang)]
    if big:
        hd.cubes += [Cube(S([-2.3, 3.3, -13.6]), [round(1.8 * s), 1, round(2.4 * s)], Mat(DK, noise=0.01, edge=0), [0, 0, 14], S([-0.6, 3.6, -12.4])),
                     Cube(S([0.5, 3.3, -13.6]), [round(1.8 * s), 1, round(2.4 * s)], Mat(DK, noise=0.01, edge=0), [0, 0, -14], S([0.6, 3.6, -12.4]))]
    m.bone("jaw", "head", S([0, 1.0, -11]), [Cube(S([-1.6, 0.1, -13.8]), [round(3.2 * s), 1, round(3.4 * s)], jaw_m)])
    m.bone("tongue", "jaw", S([0, 0.9, -13.6]), [Cube(S([-0.5, 0.9, -16.6]), [max(1, round(s)), 0, round(3 * s)], tongue)])
    m.eyes("head", 2 * s, 2.2 * s, -14 * s, h=2, corner=(1, 1), half=2 * s, iris="#F0C030" if not big else "#E84030", pupil="#140C04",
           skin=BASE, iris_light="#FFE890", grid=["WP", "IP"])
    if big:
        # The frill lies along the reared neck, behind it, so it fans out round the head seen from the front.
        m.bone("hood", "neck2", S([0, 3.0, -6]), [Cube(S([-7, 3.0, -11.3]), [24, 0, 9], frill_mat("#2AA89A", "#E0B040", "#1A2A40", "#F0D070"))])
    fx_for(m, 0.7 * s)
    moves(m, "WRAP" if not big else "BITE", "ACID" if not big else "GLARE")
    A, info = serpent(name.lower(), m, segs, k=0.6 * s)
    write(sid, name, m, A, info, stands)


def coilet():
    viper("ekans", "COILET", 0.5, False)


def frillasp():
    viper("arbok", "FRILLASP", 1.5, True)


# ======================================================================================================
# The pangolins: PANGRIT > SPIREPANG (in place of Sandshrew, Sandslash)
# ======================================================================================================

def pangolin(sid, name, stands, big):
    """A desert pangolin, armoured in overlapping scales from its brow to the end of its long tail, a pale soft face and
    belly, small bright eyes, and great curved digging claws. Grown, it's dark ochre and its back scales have risen into
    tall spires with pale points, its claws longer still."""
    m = Model()
    s = 1.0 if not big else 1.35
    S = lambda v: [x * s for x in v]
    if not big:
        BASE, LT, DK, BELLY = "#D8B070", "#F4D8A0", "#9A7440", "#F4E4C0"
    else:
        BASE, LT, DK, BELLY = "#A8743A", "#D8A868", "#5E3E1E", "#F0DCB0"
    skin = Mat(BELLY, lambda a: mix(BELLY, "#D8C0A0", 0.3 * (1 - a.ly)), noise=0.02)
    back = Mat(BASE, lambda a: rgb(BELLY) if a.face == "bottom" or a.ly < 0.3 else mix(DK, BASE, 0.4 + 0.6 * a.ly), noise=0.02)
    sc = scale_mat(BASE, LT, DK)
    face = Mat(BELLY, lambda a: rgb("#3A2A1A") if a.face == "front" and abs(a.x) < 0.8 * s and a.ly < 0.5 else None, noise=0.02)
    claw = Mat("#3A2E24", noise=0.0, edge=0)
    spire = taper_mat(BASE, "#F6ECD0")
    jaw_m = Mat(BELLY, open_mouth("#9A4A4A", "#D8C0A0"), noise=0.02)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 6, 0]), blob(S([-4, 3, -6]), S([8, 6, 12]), back))
    body = m.bones[-1]
    for row, zz in enumerate((-6.2, -3.6, -1.0, 1.6, 4.2)):
        xs = (-3, 0, 3) if row % 2 == 0 else (-1.6, 1.6)
        for x in xs:
            body.cubes.append(Cube(S([x - 1.6, 9.05, zz]), [round(3.2 * s), 0, round(3.4 * s)], sc, [16, 0, 0], S([x, 9.05, zz])))
        for sx in (-1, 1):
            body.cubes.append(Cube(S([sx * 4.05, 4.5, zz]), [0, round(4 * s), round(3.2 * s)], sc, [0, 0, 0], S([sx * 4.05, 8.5, zz])))
    if big:
        for row, zz in enumerate((-5, -2.4, 0.2, 2.8)):
            for x in ((-2.6, 0, 2.6) if row % 2 == 0 else (-1.4, 1.4)):
                L = (7 if x == 0 else 5.5) * s
                body.cubes += spike(x * s, 9.4 * s, zz * s, L, [55 - row * 4, 0, x * 6], spire, qw=2)
    m.bone("head", "body", S([0, 7, -6]), face_blob(S([-2.8, 4.5, -10]), S([5.6, 4.5, 4.5]), skin) +
           [Cube(S([-1.6, 4.6, -12]), [round(3.2 * s), round(2.6 * s), round(2 * s)], face)] +
           [Cube(S([x - 1.4, 9.0, -9.6]), [round(2.8 * s), 0, round(3 * s)], sc, [20, 0, 0], S([x, 9.0, -9.6])) for x in (-1.3, 1.3)])
    m.bone("jaw", "head", S([0, 5.2, -9]), [Cube(S([-1.2, 4.4, -11.8]), [round(2.4 * s), 1, round(2.4 * s)], jaw_m)])
    m.pair("ear_right", "head", S([-2.6, 8, -7]), [Cube(S([-3.4, 7.6, -7.4]), [round(1.6 * s), round(1.6 * s), 1], back)])
    m.eyes("head", x_inner=1.0 * s, y=7.0 * s, z=-10 * s, w=2, h=2, iris="#1A120C", pupil="#080604", skin=BELLY, grid=["PW", "PP"])
    stout_legs(m, 3.2 * s, (-4 * s, 4 * s), round(4 * s), round(2.6 * s), back, claw, 3, sharp=True, claw_len=2 if not big else 3)
    names, zt = tail_bones(m, "body", 6 * s, 6 * s, [(5 * s, 4 * s, 4 * s), (4 * s, 3.2 * s, 4 * s), (3 * s, 2.4 * s, 4 * s), (2 * s, 1.8 * s, 3.5 * s)],
                           back, (-12, -8, -6, -4))
    for i, nm in enumerate(names):
        b = next(x for x in m.bones if x.name == nm)
        w = (5, 4, 3, 2)[i] * s
        z0 = b.pivot[2]
        b.cubes.append(Cube([-w / 2 - 0.4, b.pivot[1] + (2 - i * 0.4) * s, z0], [round(w + 0.8), 0, round(3.6 * s)], sc, [14, 0, 0], [0, b.pivot[1] + (2 - i * 0.4) * s, z0]))
        if big and i < 3:
            b.cubes += spike(0, b.pivot[1] + (2 - i * 0.4) * s, z0 + 1, 4.5 * s, [55, 0, 0], spire, qw=2)
    fx_for(m, s)
    moves(m, "SCRATCH" if not big else "FURY SWIPES", "SAND-ATTACK" if not big else "SANDSTORM")
    A, info = four_legs(name.lower(), m, walk=0.7 if not big else 0.8, stride=26, k=0.8 * s)
    write(sid, name, m, A, info, stands)


def pangrit():
    pangolin("sandshrew", "PANGRIT", 0.75, False)


def spirepang():
    pangolin("sandslash", "SPIREPANG", 1.1, True)


# ======================================================================================================
# The moon rabbits: LUNABUN > LUNAHARE (in place of Clefairy, Clefable)
# ======================================================================================================

def moon_rabbit(sid, name, stands, big):
    """A rabbit of the moon: soft lilac-pink fur, a cream belly, a glowing gold crescent on its brow, long ears tipped
    with violet, button eyes, a white cotton tail. Grown, a tall slender hare: longer ears and legs, a cream ruff at
    its throat, a crescent plume of a tail."""
    m = Model()
    s = 1.0 if not big else 1.45
    S = lambda v: [x * s for x in v]
    FUR, FUR_L, FUR_D, BELLY = "#E8C4E4", "#F6E0F2", "#B88AB4", "#FFF4EC"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=BELLY, below=None), noise=0.02)
    bellym = Mat(BELLY, noise=0.02)
    face = Mat(FUR, lambda a: rgb(BELLY) if a.face == "front" and a.y < 11.5 * s else None, noise=0.02)
    nose = Mat("#E87AA0", noise=0.0, edge=0)
    jaw_m = Mat(BELLY, open_mouth("#B84A6A", "#E8D0D8"), noise=0.02)
    ear = ear_mat(FUR, "#FFC8DC", "rabbit", tip="#8A5AB0")
    cotton = Mat("#FFFFFF", lambda a: rgb("#ECE4F0") if a.face == "bottom" else None, noise=0.03, edge=0)
    paw = Mat(BELLY, lambda a: rgb("#F2C0D4") if a.face == "bottom" else None, noise=0.02)
    claw = Mat("#8A6A8A", noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 6, 0]), blob(S([-3.5, 3, -3]), S([7, 7 if not big else 8, 6]), furm) +
           [Cube(S([-2.4, 4, -3.4]), [round(4.8 * s), round(5 * s), 1], bellym)])
    top = (10 if not big else 11) * s
    m.bone("head", "body", [0, top, -1 * s], face_blob([-3.5 * s, top - 0.5 * s, -5.5 * s], S([7, 6, 6]), face) +
           [Cube([-0.75 * s, top + 1.2 * s, -6.2 * s], [max(1, round(1.5 * s)), 1, 1], nose)])
    m.bone("snout", "head", [0, top + 1.2 * s, -5.5 * s], [])
    m.bone("jaw", "head", [0, top + 0.4 * s, -4 * s], [Cube([-1.2 * s, top - 0.2 * s, -5.9 * s], [round(2.4 * s), 1, round(2 * s)], jaw_m)])
    m.pair("whiskers_right", "head", [-1.5 * s, top + 1.2 * s, -5.5 * s], [Cube([-5.5 * s, top + 1.2 * s, -6.5 * s], [round(4 * s), 0, round(2 * s)], whisker_mat("#B898B8"))])
    m.eyes("head", x_inner=1.0 * s, y=top + 2.2 * s, z=-5.5 * s, w=2, h=2, iris="#4A2A6A", pupil="#140A1C", skin=FUR, grid=["PW", "PP"])
    m.bone("crescent", "head", [0, top + 4.6 * s, -5.6 * s], [Cube([-1.5 * s, top + 4.0 * s, -5.62 * s], [round(3 * s), round(2 * s), 0], glow_patch("#FFD860"))])
    eh = (9 if not big else 12) * s
    m.pair("ear_right", "head", [-2 * s, top + 5.5 * s, -2.5 * s], [Cube([-3.6 * s, top + 5.5 * s, -2.5 * s], [round(3.2 * s), round(eh), 0], ear)],
           rotation=[-8, 0, -10 if not big else -16])
    m.pair("arm_right", "body", S([-3.5, 8.5, -2]), [Cube(S([-4.6, 5.5, -2.6]), [round(1.6 * s), round(3.4 * s), round(1.6 * s)], furm),
                                                    Cube(S([-4.7, 4.9, -2.8]), [round(1.8 * s), 1, round(1.8 * s)], paw)])
    leg_h = (3 if not big else 4.5) * s
    m.pair("leg_right", "body", S([-2.5, 4.5, 1]), blob(S([-4, 1.4, -0.6]), [round(2.6 * s), round(leg_h + 1.2 * s), round(3.2 * s)], furm))
    m.pair("foot_right", "leg_right", S([-2.75, 1.2, 0]), [Cube(S([-3.9, 0, -3.4]), [round(2.3 * s), round(1.2 * s), round(4.4 * s)], paw)] +
           [Cube(S([-3.9 + i * 0.8, 0, -3.9]), [1, 1, 1], claw) for i in range(3)])
    if not big:
        m.bone("tail", "body", S([0, 5, 3]), [Cube(S([-1.6, 4, 2.8]), [round(3.2 * s), round(3.2 * s), round(2.6 * s)], cotton)])
    else:
        names, zt = tail_bones(m, "body", 5.5 * s, 3 * s, [(2.4 * s, 2.4 * s, 2 * s), (2 * s, 2 * s, 2 * s), (1.6 * s, 1.6 * s, 2 * s)], cotton, (40, 25, 20))
        m.bones[-1].cubes.append(Cube([-2 * s, 5.5 * s, zt - 1], [round(4 * s), round(4 * s), 0], glow_patch("#FFE890")))
        m.bones[next(i for i, b in enumerate(m.bones) if b.name == "body")].cubes.append(
            Cube(S([-3.6, 9.5, -3.4]), [round(7.2 * s), round(2.4 * s), round(2 * s)], Mat("#FFF8F0", noise=0.03, edge=0)))
    fx_for(m, s * 0.8)
    moves(m, "POUND" if not big else "DOUBLESLAP", "SING" if not big else "MOONLIGHT")
    A, info = hopper(name.lower(), m, hop=0.5, height=2.0 * s, k=0.8 * s, extras={"idle": {"crescent": track(scale=[wave(2, 0.08, 0, 1)] * 3)}})
    write(sid, name, m, A, info, stands)


def lunabun():
    moon_rabbit("clefairy", "LUNABUN", 0.75, False)


def lunahare():
    moon_rabbit("clefable", "LUNAHARE", 1.4, True)


# ======================================================================================================
# The fire fennecs: FENNEMBER > SOLFENNEC (in place of Vulpix, Ninetales)
# ======================================================================================================

def fennec(sid, name, stands, big):
    """A desert fox of embers: warm cream-orange fur, a pale muzzle and chest, a black nose, huge ears with a glowing ember
    rim round each inner ear, an ember mark on its brow, and a bushy tail in joints ending in a flame. Grown, tall and
    golden, slender-legged, a ruff of flame-tipped fur at its neck, a long plumed tail with a great flame at its tip."""
    m = Model()
    s = 1.0 if not big else 1.6
    S = lambda v: [x * s for x in v]
    if not big:
        FUR, FUR_L, FUR_D, BELLY = "#F0B060", "#FFD9A0", "#C07830", "#FFF2DC"
    else:
        FUR, FUR_L, FUR_D, BELLY = "#F2D48A", "#FFF0C8", "#C89A40", "#FFF8E8"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=BELLY, below=5.6 * s), noise=0.02)
    face = Mat(FUR, lambda a: rgb(BELLY) if (a.face == "front" and a.y < 8.4 * s) or a.face == "bottom" else None, noise=0.02)
    muzzle = Mat(BELLY, lambda a: rgb("#1A1210") if a.face == "front" and a.ly > 0.55 and abs(a.x) < 0.8 * s else None, noise=0.02)
    jaw_m = Mat(BELLY, open_mouth("#A83A2A", "#E8D0B0"), noise=0.02)
    ear = ear_mat(FUR, "#FFE8D0", "fennec", rim="#FF8A2A")
    legm = Mat(FUR, lambda a: mix(FUR_D, FUR, 0.5 + 0.5 * a.ly), noise=0.02)
    claw = Mat("#3A2A20", noise=0.0, edge=0)
    tailm = Mat(FUR, fur(FUR, FUR_L, FUR_D), noise=0.03)
    m.bone("root", None, [0, 0, 0])
    top = 5 if not big else 7.5
    m.bone("body", "root", S([0, top + 2.5, 0]), blob(S([-3, top, -5]), S([6, 5, 10]), furm) +
           [Cube(S([-2.4, top + 0.4, -5.4]), [round(4.8 * s), round(4 * s), 1], Mat(BELLY, noise=0.02))])
    hy = top + 4
    m.bone("neck", "body", S([0, hy, -4.5]), blob(S([-2.2, hy - 1.5, -6.5]), S([4.4, 4, 3.5]), furm), rotation=[-12, 0, 0])
    m.bone("head", "neck", S([0, hy + 1.5, -6]), face_blob(S([-3, hy, -10.5]), S([6, 4.6, 5]), face) +
           [Cube(S([-1.4, hy, -12.6]), [round(2.8 * s), round(2.2 * s), round(2.2 * s)], muzzle)], rotation=[12, 0, 0])
    m.bone("jaw", "head", S([0, hy + 0.6, -9.5]), [Cube(S([-1.2, hy - 0.4, -12.2]), [round(2.4 * s), 1, round(2.6 * s)], jaw_m)])
    m.eyes("head", x_inner=0.9 * s, y=(hy + 2.2) * s, z=-10.5 * s, w=2 if not big else 3, h=2,
           iris="#C86A1A" if not big else "#C8301A", pupil="#1A0A04", skin=FUR, iris_light="#FFB050", grid=["IW", "PI"] if not big else ["DLL", "WPI"])
    m.bone("mark", "head", S([0, hy + 4, -10.52]), [Cube(S([-0.8, hy + 3.2, -10.53]), [round(1.6 * s), round(1.8 * s), 0], glow_patch("#FF9A3A", "flame"))])
    m.pair("ear_right", "head", S([-1.8, hy + 4.4, -8]), [Cube(S([-4.2, hy + 4.2, -8]), [round(4.4 * s), round(7.5 * s), 0], ear)], rotation=[-6, 10, -16])
    stout_legs(m, 2.0 * s, (-3.6 * s, 3.6 * s), round(top * s), round(2 * s), legm, claw, 2, sharp=True, claw_len=1)
    n_seg = 4 if not big else 5
    segs = [((3.2 - i * 0.3) * s, (3.2 - i * 0.3) * s, 3 * s) for i in range(n_seg)]
    names, zt = tail_bones(m, "body", (top + 3.4) * s, 4.6 * s, segs, tailm, (12, 6, -2, -8, -8)[:n_seg])
    tip = next(b for b in m.bones if b.name == names[-1])
    tip.cubes += flame([0, tip.pivot[1] + 0.5 * s, zt - 1.2 * s], round(4.5 * s), round(7 * s), smoke=big)
    if big:
        neck = next(b for b in m.bones if b.name == "neck")
        for i, a in enumerate((-60, -25, 25, 60)):
            neck.cubes += flame([math.sin(math.radians(a)) * 2.6 * s, (hy + 1.6) * s, (-5 + math.cos(math.radians(a)) * 0.8) * s], round(2.6 * s), round(4 * s))
    fx_for(m, 0.8 * s)
    moves(m, "QUICK ATTACK", "EMBER" if not big else "FIRE SPIN")
    A, info = four_legs(name.lower(), m, walk=0.55 if not big else 0.7, stride=32, k=0.7 * s)
    write(sid, name, m, A, info, stands)


def fennember():
    fennec("vulpix", "FENNEMBER", 0.75, False)


def solfennec():
    fennec("ninetales", "SOLFENNEC", 1.4, True)


# ======================================================================================================
# The singing chinchillas: PUFFCHIN > CHORUSCHIN (in place of Jigglypuff, Wigglytuff)
# ======================================================================================================

def chinchilla(sid, name, stands, big):
    """A singing chinchilla: a round ball of the softest pale pink-cream fur, big round ears, a curl of a forelock,
    round blue eyes, tiny hands and feet, and a little curled tail. It puffs itself up to sing. Grown, it's pear-shaped
    and lush, a thick ruff at its throat, long soft ears drooping down its back, longer feet."""
    m = Model()
    s = 1.0 if not big else 1.7
    S = lambda v: [x * s for x in v]
    FUR, FUR_L, FUR_D, BELLY = "#F4D8E0", "#FFF0F4", "#D0A8B8", "#FFF8F4"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=BELLY, below=2.5 * s, speck=0.6), noise=0.03)
    ear = ear_mat(FUR, "#F8B8C8", "round")
    longear = ear_mat(FUR, "#F8B8C8", "rabbit")
    jaw_m = Mat(BELLY, open_mouth("#C84A6A", "#F0D0D8"), noise=0.02)
    nose = Mat("#E8889A", noise=0.0, edge=0)
    paw = Mat(BELLY, lambda a: rgb("#F2B8C8") if a.face == "bottom" else None, noise=0.02)
    claw = Mat("#B08898", noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    if not big:
        m.bone("body", "root", S([0, 4, 0]), blob(S([-4.5, 1.2, -4]), S([9, 5, 8]), furm))
        m.bone("head", "body", S([0, 6, 0]), blob(S([-4.5, 5.8, -4.5]), S([9, 4.6, 9]), furm) +
               [Cube(S([-0.6, 7.4, -4.8]), [max(1, round(1.2 * s)), 1, 1], nose)])
        hy, hz, eh = 7.8, -4.5, 5
    else:
        m.bone("body", "root", S([0, 5, 0]), blob(S([-5, 1.5, -4.5]), S([10, 7, 9]), furm))
        m.bone("head", "body", S([0, 9, -0.5]), blob(S([-4, 8.4, -4.5]), S([8, 5.6, 8]), furm) +
               [Cube(S([-0.6, 10.4, -4.8]), [max(1, round(1.2 * s)), 1, 1], nose)])
        m.bones[next(i for i, b in enumerate(m.bones) if b.name == "body")].cubes += [
            Cube(S([-4.6, 7.4, -4.8]), [round(9.2 * s), round(2.4 * s), round(2 * s)], Mat(FUR_L, noise=0.04, edge=0))]
        hy, hz, eh = 10.8, -4.5, 10
    m.bone("jaw", "head", S([0, hy - 1.4, hz + 1]), [Cube(S([-1, hy - 1.9, hz - 0.4]), [round(2 * s), 1, round(1.6 * s)], jaw_m)])
    m.bone("snout", "head", S([0, hy - 0.4, hz]), [])
    m.eyes("head", x_inner=1.0 * s, y=hy * s, z=hz * s, w=3, h=2, iris="#3A7AC8", pupil="#0A1424", skin=FUR, iris_light="#8AC0F0", grid=["WPI", "PJE"])
    m.bone("curl", "head", S([0, hy + 2.6, hz + 1]), [Cube(S([-1, hy + 2.4, hz + 0.2]), [round(2 * s), round(2 * s), round(2 * s)], furm),
                                                     Cube(S([-0.6, hy + 3.6, hz - 0.8]), [round(1.6 * s), round(1.6 * s), round(1.6 * s)], furm, [-30, 0, 0], S([0, hy + 4, hz])),
                                                     Cube(S([-0.4, hy + 3.4, hz - 1.6]), [1, 1, 1], furm)])
    if not big:
        m.pair("ear_right", "head", S([-3, hy + 2.6, -1]), [Cube(S([-5.6, hy + 2.2, -1]), [round(5 * s), round(eh * s), 0], ear)], rotation=[-5, 15, -20])
    else:
        m.pair("ear_right", "head", S([-3, hy + 2.6, 0]), [Cube(S([-4.6, hy + 2.6 - eh, 0]), [round(3.6 * s), round(eh * s), 0], longear)],
               rotation=[30, 20, -18])
    m.pair("arm_right", "body", S([-4.2, 4.6, -2.5]), [Cube(S([-5.2, 2.6, -3]), [round(1.6 * s), round(2.4 * s), round(1.6 * s)], furm)])
    m.pair("leg_right", "body", S([-2.4, 2, 0]), [Cube(S([-3.4, 0.8, -1]), [round(2 * s), round(1.6 * s), round(2 * s)], furm)])
    m.pair("foot_right", "leg_right", S([-2.4, 0.8, 0]), [Cube(S([-3.5, 0, -2.6 if big else -2]), [round(2.2 * s), 1, round((3.6 if big else 2.6) * s)], paw)] +
           [Cube(S([-3.4 + i * 0.8, 0, -2.6 - (0.8 if big else 0)]), [1, 1, 1], claw) for i in range(2)])
    names, zt = tail_bones(m, "body", 3 * s, 3.8 * s, [(2 * s, 2 * s, 1.6 * s), (1.6 * s, 1.6 * s, 1.6 * s), (1.2 * s, 1.2 * s, 1.4 * s)], furm, (50, 40, 40))
    fx_for(m, 0.8 * s)
    moves(m, "POUND" if not big else "DOUBLESLAP", "SING")
    A, info = hopper(name.lower(), m, hop=0.42, height=1.6 * s, k=0.7 * s, extras={
        "cry": {"body": track(scale=keys({0: [1, 1, 1], 0.25: [1.15, 1.12, 1.15], 0.8: [1.15, 1.12, 1.15], 1.1: [1, 1, 1]})),
                "head": track(scale=keys({0: [1, 1, 1], 0.25: [1.1, 1.1, 1.1], 0.8: [1.1, 1.1, 1.1], 1.1: [1, 1, 1]}))}})
    write(sid, name, m, A, info, stands)


def puffchin():
    chinchilla("jigglypuff", "PUFFCHIN", 0.7, False)


def choruschin():
    chinchilla("wigglytuff", "CHORUSCHIN", 1.5, True)


# ======================================================================================================
# The bats: DUSKBAT > NIGHTMAW (in place of Zubat, Golbat)
# ======================================================================================================

def bat(sid, name, stands, big):
    """A bat, always on the wing: a round furry body, great pointed ears, fangs, and jointed membrane wings stretched
    over finger spines (a thumb claw at each wrist). The small one blue-violet with violet wings and button eyes; grown,
    dusky, red-eyed, a wide fanged maw, a shaggy collar, wings torn and edged in red."""
    m = Model()
    s = 1.0 if not big else 1.7
    S = lambda v: [x * s for x in v]
    if not big:
        FUR, FUR_L, FUR_D, MEM, EDGE, VEIN = "#6A6AB8", "#9A9AD8", "#3A3A7A", "#7A4AA0", "#C890E8", "#4A2A70"
    else:
        FUR, FUR_L, FUR_D, MEM, EDGE, VEIN = "#4A3A6A", "#7A6A9A", "#24183A", "#4A1E3A", "#D84A6A", "#2A0E20"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, speck=0.5), noise=0.03)
    face = Mat(FUR, lambda a: rgb(FUR_L) if a.face == "front" and a.y < 11.4 * s else None, noise=0.02)
    jaw_m = Mat(FUR_D, open_mouth("#A82A4A", FUR), noise=0.02)
    fang = Mat("#FFFFF4", noise=0.0, edge=0)
    bonem = Mat(FUR_D, noise=0.02, edge=0)
    claw = Mat("#E8DCC8", noise=0.0, edge=0)
    mems = [membrane_mat(MEM, EDGE, VEIN, seed=i * 2.3) for i in range(5)]
    ear = ear_mat(FUR, "#E8A0C8" if not big else "#C86A8A", "fennec")
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 10, 0]), blob(S([-2.5, 7.5, -2.5]), S([5, 5, 5]), furm))
    m.bone("head", "body", S([0, 11, -2]), face_blob(S([-2.5, 9.4, -6]), S([5, 4.4, 4]), face))
    m.bone("jaw", "head", S([0, 10, -4.5]), [Cube(S([-1.6, 9.0, -6.3]), [round(3.2 * s), max(1, round(1.2 * s)), round(2.4 * s)], jaw_m),
                                             Cube(S([-1.4, 9.8, -6.4]), [1, max(1, round(1.2 * s)), 1], fang), Cube(S([0.6, 9.8, -6.4]), [1, max(1, round(1.2 * s)), 1], fang)])
    if big:
        m.bones[-1].cubes = [Cube(S([-2.2, 8.2, -6.5]), [round(4.4 * s), round(1.6 * s), round(2.8 * s)], jaw_m),
                             Cube(S([-1.8, 9.6, -6.7]), [1, 2, 1], fang), Cube(S([1.0, 9.6, -6.7]), [1, 2, 1], fang)]
        m.bones[next(i for i, b in enumerate(m.bones) if b.name == "body")].cubes += [
            Cube(S([-3, 9.5, -3]), [0, round(3 * s), round(4 * s)], shag_mat(FUR_L, "#B8A0C8", FUR_D)),
            Cube(S([3, 9.5, -3]), [0, round(3 * s), round(4 * s)], shag_mat(FUR_L, "#B8A0C8", FUR_D)),
            Cube(S([-3, 9.5, -3.1]), [round(6 * s), round(3 * s), 0], shag_mat(FUR_L, "#B8A0C8", FUR_D))]
    m.eyes("head", x_inner=0.7 * s, y=11.3 * s, z=-6 * s, w=2, h=2, iris="#1A1420" if not big else "#E0303A", pupil="#06040A", skin=FUR,
           iris_light="#FF8080", grid=["PW", "PP"] if not big else ["IW", "PI"])
    m.pair("ear_right", "head", S([-1.4, 13.4, -4]), [Cube(S([-3.4, 13.2, -4]), [round(3.6 * s), round(5.5 * s), 0], ear)], rotation=[-5, 10, -12])
    dragon_wing(m, 2.4 * s, 11.6 * s, -2 * s, 6 * s, (3 * s, 4 * s, 1.5 * s, 7.5 * s), bonem, mems, thick=1 if not big else 1.5, claw=claw)
    legs_pair(m, "leg_right", "body", 1.2 * s, 7.8 * s, 1.5 * s, round(2 * s), bonem, claw)
    fx_for(m, 0.7 * s)
    moves(m, "LEECH LIFE" if not big else "WING ATTACK", "SUPERSONIC" if not big else "CONFUSE RAY")
    A, info = flyer(name.lower(), m, flap=0.3 if not big else 0.42, amp=48, rest=12, k=0.7 * s, legs=("leg_right", "leg_left"))
    write(sid, name, m, A, info, stands)


def duskbat():
    bat("zubat", "DUSKBAT", 0.8, False)


def nightmaw():
    bat("golbat", "NIGHTMAW", 1.4, True)


# ======================================================================================================
# The tubers: TUBERLET > DROOPBUD > RAFFLORA (in place of Oddish, Gloom, Vileplume)
# ======================================================================================================

def petal_mat(base, light, vein, spot=None):
    """A broad petal on a flat part lying along z from its root: rounded, a pale edge, veins running out, (spot) pale spots."""
    B, Lt, V = rgb(base), rgb(light), rgb(vein)

    def f(at):
        u, t = at.lx if at.cube.size[0] else at.ly, at.lz
        hw = 0.5 * math.sin(math.pi * min(1.0, t * 0.85 + 0.12)) ** 0.5
        d = abs(u - 0.5)
        if d > hw or t > 0.98:
            return CLEAR
        if d > hw - 0.08 or t > 0.9:
            return Lt
        if abs(((d * 5 + t * 0.4) % 1.0) - 0.5) < 0.07:
            return V
        if spot and hash01("pspot", int(u * 6), int(t * 8)) < 0.16:
            return rgb(spot)
        return mix(B, Lt, 0.2 * t)
    return Mat(base, f, noise=0.015, edge=0, flat=True)


def tuber(sid, name, stands, stage):
    """A walking tuber: a round indigo root with a face, waddling on little root feet. The first wears a crown of five
    broad leaves; the next a drooping, half-open bud of dark petals dripping glowing nectar, its eyes sleepy; the last a
    great five-petalled flower spread over its head, violet with orange veins and pale spots round a dark central pit."""
    m = Model()
    s = (1.0, 1.15, 1.35)[stage]
    S = lambda v: [x * s for x in v]
    TB, TBL, TBD = "#5A6AC8", "#8A9AE8", "#2E3A8A"
    body = Mat(TB, lambda a: mix(TBD, TB, 0.4 + 0.6 * a.ly) if a.face != "top" else mix(TB, TBL, 0.5), noise=0.025)
    jaw_m = Mat(TBD, open_mouth("#6A2A4A", TB), noise=0.02)
    root = Mat("#C8B890", lambda a: rgb("#8A7A5A") if a.face == "bottom" else None, noise=0.02, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 4.5, 0]), blob(S([-4, 1.4, -4]), S([8, 7, 8]), body, b=2))
    m.bone("head", "body", S([0, 8, 0]), [Cube(S([-2, 8.2, -2]), [round(4 * s), round(1.5 * s), round(4 * s)], Mat("#3E8A3A", noise=0.02))])
    hd = m.bones[-1]
    if stage == 0:
        lf = leaf_mat("#7AD060", "#2E7A30", "#C8F0A0", "z")
        for a in (0, 72, 144, 216, 288):
            hd.cubes.append(Cube(S([-2.5, 9.4, 0]), [round(5 * s), 0, round(10 * s)], lf, [52, a, 0], S([0, 9.4, 0])))
    elif stage == 1:
        pm = petal_mat("#A8382A", "#E87A4A", "#6A1A14")
        for a in (0, 72, 144, 216, 288):
            hd.cubes.append(Cube(S([-2.5, 9.4, 0]), [round(5 * s), 0, round(7 * s)], pm, [68, a, 0], S([0, 9.4, 0])))
            hd.cubes.append(Cube(S([-2, 9.6, 0]), [round(4 * s), 0, round(5 * s)], leaf_mat("#5AA848", "#2A6A2A", "#A8E080", "z"), [10, a + 36, 0], S([0, 9.4, 0])))
        drop = Mat("#E8FF8A", lambda a: CLEAR if (a.lx - 0.5) ** 2 + (a.ly - 0.45) ** 2 > 0.2 else None,
                   glow=lambda a: rgb("#E8FF8A") if (a.lx - 0.5) ** 2 + (a.ly - 0.45) ** 2 < 0.2 else None, noise=0, edge=0, flat=True)
        # A drop of glowing nectar hanging from the tip of the bud.
        m.bone("drip", "head", S([0, 16, 0]), [Cube(S([-0.8, 15.6, 0]), [round(1.6 * s), round(2.2 * s), 0], drop),
                                               Cube(S([0, 15.6, -0.8]), [0, round(2.2 * s), round(1.6 * s)], drop)])
    else:
        pm = petal_mat("#8A3AC8", "#E8B8FF", "#F08A2A", spot="#F4E0FF")
        for a in (0, 72, 144, 216, 288):
            hd.cubes.append(Cube(S([-6, 10, 1.5]), [round(12 * s), 0, round(13 * s)], pm, [-8, a, 0], S([0, 10, 0])))
        hd.cubes += [Cube(S([-2.4, 9.8, -2.4]), [round(4.8 * s), round(1.6 * s), round(4.8 * s)], Mat("#2A1A2E", noise=0.02)),
                     Cube(S([-1.6, 11.2, -1.6]), [round(3.2 * s), 1, round(3.2 * s)], Mat("#F0A040", noise=0.02, edge=0))]
    m.bone("jaw", "head", S([0, 4.4, -3.5]), [Cube(S([-1.2, 3.8, -4.1]), [round(2.4 * s), 1, round(1.4 * s)], jaw_m)])
    m.bone("snout", "head", S([0, 5.5, -4]), [])
    m.eyes("head", x_inner=0.9 * s, y=5.6 * s, z=-4 * s, w=2, h=2, iris="#C83A3A" if stage < 2 else "#E8A030", pupil="#1A0606", skin=TB,
           grid=["PW", "PP"] if stage == 0 else (["LL", "PW"] if stage == 1 else ["IW", "PI"]))
    m.pair("leg_right", "body", S([-2, 1.6, 0]), [Cube(S([-3, 0.9, -1]), [round(2 * s), round(1.2 * s), round(2 * s)], root)])
    m.pair("foot_right", "leg_right", S([-2, 0.6, 0]), [Cube(S([-3.2, 0, -1.6]), [round(2.4 * s), 1, round(2.6 * s)], root)])
    fx_for(m, 0.8 * s)
    moves(m, ("ABSORB", "ACID", "ACID")[stage], ("POISONPOWDER", "SLEEP POWDER", "PETAL DANCE")[stage])
    A, info = hopper(name.lower(), m, hop=0.4, height=1.0 * s, k=0.6 * s, extras={"drip": {} if stage != 1 else {}})
    write(sid, name, m, A, info, stands)


def tuberlet():
    tuber("oddish", "TUBERLET", 0.8, 0)


def droopbud():
    tuber("gloom", "DROOPBUD", 1.0, 1)


def rafflora():
    tuber("vileplume", "RAFFLORA", 1.35, 2)


# ======================================================================================================
# The mushroom crabs: SHROOMSHELL > FUNGUARD (in place of Paras, Parasect)
# ======================================================================================================

def crab_leg(m, name, x, y, z, L1, L2, mat, tip):
    m.pair(name, "body", [-x, y, z], [Cube([-x - L1, y - 0.5, z - 0.5], [L1, 1, 1], mat)], rotation=[0, 0, 30])
    m.pair(name.replace("leg_", "shin_"), name, [-x - L1, y, z], [Cube([-x - L1 - 0.5, y - L2, z - 0.5], [1, L2, 1], mat),
                                                                  Cube([-x - L1 - 0.5, y - L2 - 1.2, z - 0.5], [0, 1.6, 1.2], tip)], rotation=[0, 0, -32])


def crab(sid, name, stands, big):
    """A hermit crab living under a mushroom: an orange-red carapace, stalked black eyes, two pincers and six jointed
    legs with sharp tips, and on its back a domed tan cap with pale spots and gills beneath. Grown, the fungus has taken
    it over: a towering three-tiered shelf of ochre bracket fungus, the crab's eyes glowing pale."""
    m = Model()
    s = 1.0 if not big else 1.8
    S = lambda v: [x * s for x in v]
    CR, CRL, CRD = ("#E06A3A", "#FF9A6A", "#9A3A1A") if not big else ("#B8502A", "#E08050", "#6A2A12")
    shell = Mat(CR, lambda a: mix(CRD, CR, 0.4 + 0.6 * a.ly) if a.face != "top" else mix(CR, CRL, 0.5), noise=0.025)
    legm = Mat(CR, lambda a: mix(CRD, CR, a.ly), noise=0.02, edge=0)
    tip = sharp_claw_mat("#3A1A10", "#E8D0B8")
    jaw_m = Mat(CRD, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 3.5, 0]), blob(S([-3, 2, -3]), S([6, 3, 6]), shell))
    m.bone("head", "body", S([0, 3.8, -3]), [Cube(S([-2, 2.4, -4.4]), [round(4 * s), round(2.4 * s), round(1.6 * s)], shell)])
    m.bone("jaw", "head", S([0, 2.6, -4]), [Cube(S([-1, 2.0, -4.8]), [round(2 * s), 1, 1], jaw_m)])
    stalk = Mat(CRD, noise=0.0, edge=0)
    m.pair("antenna_right", "head", S([-1.2, 4.8, -4]), [Cube(S([-1.6, 4.8, -4.4]), [1, round(1.8 * s), 1], stalk)], rotation=[-15, 0, -10])
    m.eyes("head", x_inner=0.6 * s, y=3.4 * s, z=-4.4 * s, w=2, h=2, iris="#0A0A0A" if not big else "#E8F4E0", pupil="#000000" if not big else "#A8C8A0",
           skin=CR, grid=["PW", "PP"] if not big else ["WI", "II"])
    m.pair("arm_right", "body", S([-2.6, 3.4, -3]), [Cube(S([-4.4, 2.8, -4.6]), [round(1.6 * s), round(1.4 * s), round(2.2 * s)], shell),
                                                    Cube(S([-4.8, 2.2, -7.2]), [round(2.2 * s), round(1.6 * s), round(2.8 * s)], shell),
                                                    Cube(S([-4.6, 3.8, -7.0]), [round(1.6 * s), 1, round(2.4 * s)], shell, [-14, 0, 0], S([-4, 3.8, -4.8]))],
           rotation=[0, 20, 0])
    for nm, zz in (("leg_front_right", -1.6), ("leg_mid_right", 0.2), ("leg_back_right", 2.0)):
        crab_leg(m, nm, 2.8 * s, 3 * s, zz * s, 2.6 * s, 3.6 * s, legm, tip)
    if not big:
        cap = Mat("#C89A5A", lambda a: rgb("#F4E4C0") if a.face == "top" and hash01("cs", int(a.x), int(a.z)) < 0.18 else
                  (rgb("#E8D8B0") if a.face == "bottom" and int(a.x * 2) % 2 == 0 else None), noise=0.02)
        # A domed cap: a broad rim, then narrower tiers rounding over the top.
        m.bone("shell", "body", S([0, 5, 1]), blob(S([-4.5, 5, -3]), S([9, 2, 9]), cap, b=2) + blob(S([-3.6, 7, -2.1]), S([7.2, 1.6, 7.2]), cap) +
               [Cube(S([-2.4, 8.6, -0.9]), [round(4.8 * s), 1, round(4.8 * s)], cap), Cube(S([-1.2, 4.6, 0.3]), [round(2.4 * s), 1, round(2.4 * s)], Mat("#F0E4C8", noise=0.02))])
    else:
        bracket = Mat("#C8903A", lambda a: rgb("#F4E0B0") if a.face != "bottom" and (a.cube.origin[1] + a.cube.size[1]) - a.y < 0.6 else
                      (rgb("#E8D8B8") if a.face == "bottom" else mix("#8A5A1E", "#C8903A", a.ly)), noise=0.025)
        m.bone("shell", "body", S([0, 5, 1]), blob(S([-6, 5, -3.5]), S([12, 2, 10]), bracket) + blob(S([-5, 7, -2.5]), S([10, 2, 8]), bracket) +
               blob(S([-3.5, 9, -1.5]), S([7, 2, 6]), bracket) + [Cube(S([-1, 11, 0]), [round(2 * s), round(1.2 * s), round(2 * s)], bracket)])
    fx_for(m, 0.6 * s)
    moves(m, "SCRATCH" if not big else "SLASH", "STUN SPORE" if not big else "SPORE")
    W = 0.5 if not big else 0.6
    A, info = four_legs(name.lower(), m, walk=W, stride=22, k=0.5 * s, extras={
        "walk": {"leg_mid_right": track(rotation=[wave(W, 22, 90), 0, 0]), "leg_mid_left": track(rotation=[wave(W, -22, 90), 0, 0])},
        "idle": {"arm_right": track(rotation=[wave(2, 4), wave(2, 6), 0]), "arm_left": track(rotation=[wave(2, 4, 60), wave(2, -6, 60), 0])}})
    write(sid, name, m, A, info, stands)


def shroomshell():
    crab("paras", "SHROOMSHELL", 0.5, False)


def funguard():
    crab("parasect", "FUNGUARD", 1.1, True)


# ======================================================================================================
# FUZZGNAT > TOXILACE (in place of Venonat, Venomoth)
# ======================================================================================================

def fuzzgnat():
    """A fuzzy violet gnat the size of a ball: thick soft fur, enormous faceted red eyes, knobbed antennae, small
    mandibles, and four short clawed legs."""
    m = Model()
    FZ, FZL, FZD = "#8A5AB0", "#B890D8", "#4A2A6A"

    def fuzz(at):
        c = mix(FZD, FZ, 0.4 + 0.6 * at.ly)
        if hash01("fg", int(at.x * 2), int(at.y * 2), int(at.z * 2)) > 0.68:
            c = mix(c, FZL, 0.55)
        return c
    furm = Mat(FZ, fuzz, noise=0.03)
    jaw_m = Mat(FZD, open_mouth("#6A1A3A", FZ), noise=0.02)
    mand = Mat("#E8D8F0", noise=0.0, edge=0)
    knob = Mat("#E8D8F8", noise=0.0, edge=0)
    claw = Mat("#2A1A30", noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 7, 0], blob([-4.5, 3, -4], [9, 8.5, 9], furm, b=2))
    m.bone("head", "body", [0, 8, -4], blob([-4, 4.5, -6.5], [8, 6, 3], furm))
    m.bone("jaw", "head", [0, 5, -6], [Cube([-1.5, 4.2, -7.2], [3, 1, 2], jaw_m), Cube([-1.8, 3.6, -7.6], [1, 1, 1], mand), Cube([0.8, 3.6, -7.6], [1, 1, 1], mand)])
    m.eyes("head", 4, 6.4, -6.5, h=4, corner=(3, 2), half=4, iris="#D02A30", pupil="#5A0A10", skin=FZ, iris_light="#FF7A70", grid=compound_grid(5, 4))
    m.pair("antenna_right", "head", [-1.6, 10.4, -5], [Cube([-2.1, 10.4, -5.5], [1, 3.5, 1], Mat(FZD, noise=0, edge=0)), Cube([-2.6, 13.6, -6], [2, 2, 2], knob)],
           rotation=[-12, 0, -18])
    stout_legs(m, 3.0, (-2.5, 2.5), 3, 2, furm, claw, 2, sharp=True)
    fx_for(m, 1.0)
    moves(m, "TACKLE", "CONFUSION")
    A, info = four_legs("fuzzgnat", m, walk=0.45, stride=26, k=0.7)
    write("venonat", "FUZZGNAT", m, A, info, 1.0)


def toxilace():
    """A poison lacewing: a slender pale-green body, big golden faceted eyes, long fine antennae, two pairs of lacy wings
    netted with dark veins (the hind pair drawn out into long trailing tails), and thin hooked legs."""
    m = Model()
    G, GL, GD = "#9AD8A0", "#D8F4D0", "#3A7A4A"
    bodym = Mat(G, lambda a: rgb("#C8A8E8") if int(a.z * 1.3 + 50) % 2 == 0 and a.face != "bottom" else mix(GD, G, 0.4 + 0.6 * a.ly), noise=0.02)
    legm = Mat(GD, noise=0.01, edge=0)
    claw = Mat("#1A2A1A", noise=0.0, edge=0)
    jaw_m = Mat(GD, noise=0.02)
    ant = Mat(GD, noise=0.0, edge=0)
    fore = wing_paint("#E8F8E8", "#2A5A3A", "#3A7A4A", 3, 16, tint="#C8F0C0", lace=True)
    hind = wing_paint("#E8F8E8", "#2A5A3A", "#3A7A4A", 3, 13, hind=True, tint="#E0C8F0", lace=True)
    streamer = Mat("#C8A8E8", lambda a: CLEAR if abs(a.lx - 0.5) > 0.3 * (1 - a.lz) + 0.08 else None, noise=0.0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 13, 0], blob([-2.2, 11, -2.5], [4.4, 4, 5], bodym))
    m.bone("head", "body", [0, 13.5, -2.5], blob([-2.4, 11.6, -6], [4.8, 4, 3.6], Mat(G, noise=0.02)))
    m.bone("jaw", "head", [0, 12, -5.5], [Cube([-0.8, 11.4, -6.4], [1.6, 1, 1.4], jaw_m)])
    m.eyes("head", 2.4, 12.6, -6, h=3, corner=(2, 2), half=2.4, iris="#E8B020", pupil="#6A4A08", skin=G, iris_light="#FFE070", grid=compound_grid(4, 3))
    m.pair("antenna_right", "head", [-1, 15.4, -5.6], [Cube([-1.3, 15.4, -6], [0.6 if False else 1, 7, 1], ant)], rotation=[-40, 0, -18])
    m.bone("abdomen", "body", [0, 12.6, 2.5], [Cube([-1.6, 11.4, 2.3], [3.2, 2.8, 4], bodym)], rotation=[-6, 0, 0])
    m.bone("abdomen2", "abdomen", [0, 12.6, 6.3], [Cube([-1.3, 11.6, 6.1], [2.6, 2.2, 4], bodym)], rotation=[-6, 0, 0])
    m.bone("abdomen3", "abdomen2", [0, 12.6, 10.1], [Cube([-1, 11.8, 9.9], [2, 1.8, 3.4], bodym),
                                                     Cube([-1.6, 12.4, 13], [1, 0, 6], streamer, [0, -10, 0], [-1.1, 12.4, 13]),
                                                     Cube([0.6, 12.4, 13], [1, 0, 6], streamer, [0, 10, 0], [1.1, 12.4, 13])], rotation=[-6, 0, 0])
    legs_pair(m, "leg_1_right", "body", 2, 11.4, -1.5, 5, legm, claw, ang=-12)
    legs_pair(m, "leg_2_right", "body", 2, 11.4, 1.2, 6, legm, claw, ang=14)
    m.pair("wing_right", "body", [-2.2, 15, -1], [Cube([-18.2, 15, -3.5], [16, 0, 9], fore)], rotation=[0, 0, 22])
    m.pair("hindwing_right", "body", [-2.2, 14.6, 1.5], [Cube([-15.2, 14.6, 0.8], [13, 0, 12], hind)], rotation=[0, 0, 14])
    fx_for(m, 1.0)
    moves(m, "TACKLE", "PSYBEAM")
    A, info = insect("toxilace", m, flap=0.32, amp=46, k=0.9, legs=("leg_1_right", "leg_1_left", "leg_2_right", "leg_2_left"))
    write("venomoth", "TOXILACE", m, A, info, 1.2)


# ======================================================================================================
# The star-nosed moles: STARSNOUT > TRIOSNOUT (in place of Diglett, Dugtrio)
# ======================================================================================================

def star_nose():
    """A star nose on a flat part facing forward: pink fleshy rays round a small middle."""
    def inside(at):
        x, y = at.lx - 0.5, at.ly - 0.5
        r, th = math.hypot(x, y) * 2, math.atan2(y, x)
        R = 0.35 + 0.6 * abs(math.cos(th * 5.5)) ** 2
        return r < R
    return Mat("#F08AA8", lambda a: (rgb("#FFC8D8") if math.hypot(a.lx - 0.5, a.ly - 0.5) < 0.18 else None) if inside(a) else CLEAR,
               noise=0.01, edge=0, flat=True)


def mole(m, suffix, x, z, s, velvet, face, nose, claw_m, paw, main):
    b, h = "body" + suffix, "head" + suffix
    m.bone(b, "mound", [x, 3 * s, z], blob([x - 2.4 * s, 1.5 * s, z - 2.4 * s], [round(4.8 * s), round(6 * s), round(4.8 * s)], velvet))
    m.bone(h, b, [x, 6.5 * s, z - 1 * s], face_blob([x - 2.3 * s, 5.2 * s, z - 3.2 * s], [round(4.6 * s), round(3.4 * s), round(2 * s)], face) +
           [Cube([x - 1.6 * s, 5.6 * s, z - 3.3 * s], [round(3.2 * s), round(3.2 * s), 0], nose)])
    if not main:
        m.bones[-1].cubes += [Cube([x - 1.6 * s, 7.6 * s, z - 3.25 * s], [1, 1, 0], Mat("#0A0A0A", noise=0, edge=0)),
                              Cube([x + 0.6 * s, 7.6 * s, z - 3.25 * s], [1, 1, 0], Mat("#0A0A0A", noise=0, edge=0))]
    for sx in (-1, 1):
        m.bones[next(i for i, bb in enumerate(m.bones) if bb.name == b)].cubes += [
            Cube([x + sx * 2.6 * s - 1, 2 * s, z - 2.8 * s], [2, 1.2, 2], paw)] + [
            Cube([x + sx * 2.6 * s - 1 + i * 0.7, 2 * s, z - 3.4 * s], [0, 1.4, 1.6], claw_m) for i in range(3)]


def burrow(sid, name, stands, trio):
    """A star-nosed mole poking up out of its mound: dark velvet fur, a pink star of a nose, tiny eyes, broad pink
    digging paws with long claws at the mound's edge. Three of them together in one big mound when grown."""
    m = Model()
    s = 1.0 if not trio else 1.15
    velvet = Mat("#4A4450", lambda a: mix("#2A2630", "#6A6474", a.ly) if a.face != "top" else rgb("#6A6474"), noise=0.03)
    face = Mat("#4A4450", noise=0.03)
    dirt = Mat("#8A6238", lambda a: rgb("#B08A5A") if a.face == "top" and hash01("dt", int(a.x), int(a.z)) < 0.3 else
               (rgb("#5A3E22") if hash01("dp", int(a.x * 2), int(a.y * 2), int(a.z * 2)) < 0.15 else None), noise=0.04)
    paw = Mat("#F2A8B8", noise=0.02, edge=0)
    claw_m = sharp_claw_mat("#E8E0D0", "#FFFFFF")
    m.bone("root", None, [0, 0, 0])
    w = 10 if not trio else 17
    m.bone("mound", "root", [0, 0, 0], blob([-w / 2, 0, -w / 2 + 1], [w, 3, w - 2], dirt, b=2) +
           [Cube([-w / 2 + 1, 2.5, -w / 2 + 2], [w - 2, 1.2, w - 4], dirt)])
    if not trio:
        mole(m, "", 0, 0, s, velvet, face, star_nose(), claw_m, paw, True)
        bodies = ("body",)
    else:
        mole(m, "", 0, -1.5, s, velvet, face, star_nose(), claw_m, paw, True)
        mole(m, "2", -4.6, 1.8, s * 0.9, velvet, face, star_nose(), claw_m, paw, False)
        mole(m, "3", 4.6, 1.8, s * 0.9, velvet, face, star_nose(), claw_m, paw, False)
        bodies = ("body", "body2", "body3")
    m.bone("jaw", "head", [0, 5.6 * s, -2.8 * s], [Cube([-1 * s, 5.0 * s, -3.4 * s], [round(2 * s), 1, 1], Mat("#2A2630", noise=0.01))])
    m.eyes("head", x_inner=0.9 * s, y=7.6 * s, z=(-3.2 - (1.5 if trio else 0)) * s, w=1, h=1, iris="#0A0A0A", pupil="#000000", skin="#4A4450",
           grid=["P"], squint=False)
    fx_for(m, 0.6 * s)
    moves(m, "SCRATCH" if not trio else "SLASH", "MAGNITUDE" if not trio else "EARTHQUAKE")
    A, info = burrower(name.lower(), m, bodies, k=0.6 * s)
    write(sid, name, m, A, info, stands)


def starsnout():
    burrow("diglett", "STARSNOUT", 0.55, False)


def triosnout():
    burrow("dugtrio", "TRIOSNOUT", 0.85, True)


# ======================================================================================================
# The lynxes: LYNXKIT > SABRELYNX (in place of Meowth, Persian)
# ======================================================================================================

def lynx(sid, name, stands, big):
    """A lynx: tawny and spotted, black-tufted ears, a cream muzzle and chest, whiskers, green eyes, a short tail in
    joints with a black tip, and claws. Grown, a sleek grey-gold hunter on long legs, striped, with cheek ruffs, long
    saber fangs and a longer tail."""
    m = Model()
    s = 1.0 if not big else 1.5
    S = lambda v: [x * s for x in v]
    if not big:
        FUR, FUR_L, FUR_D, BELLY, MARK = "#D8A060", "#F0C890", "#9A6A30", "#FFF0DC", "#6A4A2A"
    else:
        FUR, FUR_L, FUR_D, BELLY, MARK = "#B8A07A", "#E0D0A8", "#7A6440", "#F6EEDC", "#4A3A22"
    base = fur(FUR, FUR_L, FUR_D, belly=BELLY, below=5.6 * s)

    def coat(at):
        if at.face != "bottom" and at.side:
            if not big and hash01("ros", int(at.x / 1.5), int(at.y / 1.5), int(at.z / 1.5)) < 0.16:
                return rgb(MARK)
            if big and int(at.z * 0.8 + 50) % 3 == 0 and at.ly > 0.4:
                return rgb(MARK)
        return base(at)
    furm = Mat(FUR, coat, noise=0.02)
    face = Mat(FUR, lambda a: rgb(BELLY) if (a.face == "front" and a.y < 8.6 * s) or a.face == "bottom" else None, noise=0.02)
    muzzle = Mat(BELLY, lambda a: rgb("#A86A5A") if a.face == "front" and a.ly > 0.6 and abs(a.x) < 0.7 * s else None, noise=0.02)
    jaw_m = Mat(BELLY, open_mouth("#A83A3A", "#E8D0B0"), noise=0.02)
    ear = ear_mat(FUR, "#F0D0B8", "fennec", tip="#1A1210")
    claw = Mat("#2A2018", noise=0.0, edge=0)
    fang = Mat("#FFFFF0", noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    top = 5 if not big else 8
    m.bone("body", "root", S([0, top + 2.5, 0]), blob(S([-3, top, -5]), S([6, 5, 10]), furm))
    hy = top + 4
    m.bone("neck", "body", S([0, hy, -4.5]), blob(S([-2.4, hy - 1.5, -6.5]), S([4.8, 4, 3.5]), furm), rotation=[-10, 0, 0])
    m.bone("head", "neck", S([0, hy + 1.5, -6]), face_blob(S([-3, hy, -10.2]), S([6, 4.8, 4.8]), face) +
           [Cube(S([-1.5, hy, -11.6]), [round(3 * s), round(2.2 * s), round(1.6 * s)], muzzle)], rotation=[10, 0, 0])
    m.bone("jaw", "head", S([0, hy + 0.6, -9.5]), [Cube(S([-1.3, hy - 0.4, -11.4]), [round(2.6 * s), 1, round(2.2 * s)], jaw_m)])
    if big:
        m.bones[-2].cubes += [Cube(S([-1.4, hy - 1.6, -11.2]), [1, round(2 * s), 1], fang), Cube(S([0.6, hy - 1.6, -11.2]), [1, round(2 * s), 1], fang)]
        m.bones[-2].cubes += [Cube(S([-3.4, hy, -9]), [0, round(3 * s), round(3 * s)], shag_mat(FUR_L, BELLY, FUR_D), [0, -20, 0], S([-3.2, hy + 3, -9])),
                              Cube(S([3.4, hy, -9]), [0, round(3 * s), round(3 * s)], shag_mat(FUR_L, BELLY, FUR_D), [0, 20, 0], S([3.2, hy + 3, -9]))]
    m.pair("whiskers_right", "head", S([-1.5, hy + 1.2, -11]), [Cube(S([-5.5, hy + 1.2, -12]), [round(4 * s), 0, round(2 * s)], whisker_mat("#F8F0E0"))])
    m.eyes("head", x_inner=0.9 * s, y=(hy + 2.4) * s, z=-10.2 * s, w=2 if not big else 3, h=2, iris="#4AA83A" if not big else "#E0A020",
           pupil="#0A1A04", skin=FUR, iris_light="#A8E070", grid=["IW", "PI"] if not big else ["DLL", "WPI"])
    m.pair("ear_right", "head", S([-1.8, hy + 4.4, -8]), [Cube(S([-3.6, hy + 4.2, -8]), [round(3.2 * s), round(4.6 * s), 0], ear)], rotation=[-6, 8, -12])
    stout_legs(m, 2.2 * s, (-3.6 * s, 3.6 * s), round(top * s), round(2 * s), furm, claw, 2, sharp=True)
    n_seg = 3 if not big else 4
    segs = [((2.4 - i * 0.3) * s, (2.4 - i * 0.3) * s, (1.8 if not big else 3) * s) for i in range(n_seg)]
    names, zt = tail_bones(m, "body", (top + 3.6) * s, 4.8 * s, segs, furm, (20, 8, -4, -8)[:n_seg])
    tipb = next(b for b in m.bones if b.name == names[-1])
    tipb.cubes.append(Cube([-1 * s, tipb.pivot[1] - 1 * s, zt - 0.6], [round(2 * s), round(2 * s), round(1.4 * s)], Mat("#1A1210", noise=0.02)))
    fx_for(m, 0.8 * s)
    moves(m, "SCRATCH" if not big else "SLASH", "PAY DAY" if not big else "FAINT ATTACK")
    A, info = four_legs(name.lower(), m, walk=0.55 if not big else 0.7, stride=30, k=0.7 * s)
    write(sid, name, m, A, info, stands)


def lynxkit():
    lynx("meowth", "LYNXKIT", 0.8, False)


def sabrelynx():
    lynx("persian", "SABRELYNX", 1.3, True)


# ======================================================================================================
# Upright bodies: jointed legs and arms for the creatures that stand and walk on two feet
# ======================================================================================================

def bone_of(m, name):
    return next(b for b in m.bones if b.name == name)


def web_mat(web, toe):
    """Webbing on a flat part lying flat in front of the toes (its front at the low z): a fan spreading out to a
    scalloped front edge, the toes running through it as darker lines."""
    W, T = rgb(web), rgb(toe)

    def f(at):
        u, t = at.lx - 0.5, 1 - at.lz
        hw = 0.32 + 0.18 * t
        if abs(u) > hw:
            return CLEAR
        if t > 0.8 and math.cos(abs(u) * 3 * math.pi * 2) < -0.2:
            return CLEAR
        if min(abs(u - k * hw * 0.66) for k in (-1, 0, 1)) < 0.06:
            return T
        return mix(W, "#FFFFFF", 0.12 * t)
    return Mat(web, f, noise=0.01, edge=0, flat=True)


def gill_mat(stalk, plume, tip):
    """A feathery gill frill on a flat part lying along z from its root: a stalk along its middle, fine filaments up and
    down from it, shorter towards the tip."""
    St, Pl, Tp = rgb(stalk), rgb(plume), rgb(tip)

    def f(at):
        u, t = at.ly - 0.5, at.lz
        hw = 0.5 * (1 - 0.55 * t)
        d = abs(u)
        if d < 0.09:
            return St
        if d < hw and at.col % 2 == 0:
            return mix(Pl, Tp, t)
        return CLEAR
    return Mat(stalk, f, noise=0.0, edge=0, flat=True)


def closed_eye_mat(line):
    """A shut, sleeping eye on a flat part facing forward (3 by 2): a soft curve dipping down in the middle."""
    L = rgb(line)

    def f(at):
        mid = 0.3 < at.lx < 0.7
        if at.ly < 0.5:
            return L
        return CLEAR if mid else L
    return Mat(line, f, noise=0, edge=0, flat=True)


def orb_mat(core, rim):
    """A glowing stone held in the hand: bright at its heart, a deeper rim."""
    C, R = rgb(core), rgb(rim)
    ring = lambda a: max(abs(a.lx - 0.5), abs(a.ly - 0.5) if a.cube.size[1] else 0, abs(a.lz - 0.5) if a.cube.size[2] else 0)
    return Mat(core, lambda a: mix(C, R, ring(a) * 1.6), glow=lambda a: mix("#FFFFFF", C, ring(a) * 2.2), noise=0, edge=0)


def upright_legs(m, x, hip, z, w, legm, footm, flen, toe_m=None, ntoes=3, sharp=False, web=None, knee_at=0.5, thigh_w=None,
                 rest=None, shin_rest=None, toe_len=2, foot_rest=None):
    """Two jointed legs for a creature that stands up: a thigh from the hip to the knee, a shin from the knee down to
    the ankle (bending back there), a foot bending at the ankle, and its toes, sharp claws or webbing along the front."""
    fh = 1 if hip < 9 else 2
    knee = fh + (hip - fh) * knee_at
    tw = thigh_w or w
    up = (blob([-x - tw / 2, knee - 0.5, z - tw / 2], [tw, hip - knee + 1.5, tw], legm) if tw >= 4 else
          [Cube([-x - tw / 2, knee - 0.5, z - tw / 2], [n(tw), n(hip - knee + 1.5), n(tw)], legm)])
    m.pair("leg_right", "body", [-x, hip, z], up, rotation=rest)
    wl = max(1, w - 1)
    m.pair("shin_right", "leg_right", [-x, knee, z], [Cube([-x - wl / 2, fh - 0.2, z - wl / 2], [n(wl), n(knee - fh + 0.7), n(wl)], legm)],
           rotation=shin_rest)
    fw = n(w + 1)
    z0 = z + wl / 2 + 0.5 - flen
    cubes = [Cube([-x - fw / 2, 0, z0], [fw, fh, n(flen)], footm)]
    toe_m = toe_m or footm
    if web is not None:
        cubes.append(Cube([-x - fw / 2 - 1, 0.04, z0 - toe_len], [fw + 2, 0, n(toe_len)], web))
    for i in range(ntoes):
        xc = -x - fw / 2 + (i + 0.5) * fw / ntoes
        if sharp:
            L = toe_len
            cubes += [Cube([xc - 0.5, 0, z0 - 1], [1, 1, 1], toe_m), Cube([xc, 0, z0 - 1 - L], [0, 2, n(L)], sharp_claw_mat())]
        elif web is not None:
            cubes.append(Cube([xc - 0.5, 0, z0 - toe_len - 0.6], [1, 1, 1], toe_m))
        else:
            cubes.append(Cube([xc - 0.5, 0, z0 - 1], [1, 1, 1], toe_m))
    m.pair("foot_right", "shin_right", [-x, fh, z], cubes, rotation=foot_rest)


def upright_arms(m, x, sh, z, upper, fore, w, armm, handm, hw=None, hh=2, fingers=0, finger_m=None, finger_len=2, claws=0, web=None,
                 name="arm", rest=(0, 0, 8), fore_rest=(-12, 0, 0), thick=None, fore_w=None, parent="body"):
    """Two jointed arms: the upper arm from the shoulder to the elbow, the forearm (bending at the elbow) to the hand, and
    its fingers, webbing or claws. Gives the hand's bottom (y)."""
    elbow = sh - upper
    wrist = elbow - fore
    tw = thick or w
    up = (blob([-x - tw / 2, elbow - 0.5, z - tw / 2], [tw, upper + 1, tw], armm) if tw >= 4 else
          [Cube([-x - tw / 2, elbow - 0.5, z - tw / 2], [n(tw), n(upper + 1), n(tw)], armm)])
    m.pair(f"{name}_right", parent, [-x, sh, z], up, rotation=list(rest) if rest else None)
    fw = fore_w or (w if w <= 2 else w - 1)
    hw = hw or fw + 1
    cubes = (blob([-x - fw / 2, wrist - 0.2, z - fw / 2], [fw, fore + 0.7, fw], armm) if fw >= 4 else
             [Cube([-x - fw / 2, wrist - 0.2, z - fw / 2], [n(fw), n(fore + 0.7), n(fw)], armm)])
    cubes.append(Cube([-x - hw / 2, wrist - hh, z - hw / 2], [n(hw), n(hh), n(hw)], handm))
    fm = finger_m or handm
    for i in range(fingers):
        xc = -x - hw / 2 + (i + 0.5) * hw / fingers
        cubes.append(Cube([xc - 0.5, wrist - hh - finger_len, z - hw / 2], [1, n(finger_len), 1], fm))
    if web is not None and fingers:
        cubes.append(Cube([-x - hw / 2, wrist - hh - finger_len + 0.5, z - hw / 2 + 0.5], [n(hw), n(finger_len - 0.5), 0], web))
    for i in range(claws):
        xc = -x - hw / 2 + (i + 0.5) * hw / claws
        zr = z - hw / 2
        cubes.append(Cube([xc, wrist - hh - 0.5, zr - 2.5], [0, 2, 3], sharp_claw_mat(), [72, 0, 0], [xc, wrist - hh + 0.5, zr]))
    m.pair(f"fore{name}_right", f"{name}_right", [-x, elbow, z], cubes, rotation=list(fore_rest) if fore_rest else None)
    return wrist - hh


# ======================================================================================================
# The platypuses: ACHEBILL > TIDEBILL (in place of Psyduck, Golduck)
# ======================================================================================================

def bill_mat(base, light, nostril):
    """A platypus's soft flat bill: paler on top, two nostrils near its tip, a darker rim."""
    B, Lt, N = rgb(base), rgb(light), rgb(nostril)

    def f(at):
        if at.face == "top":
            u = abs(at.lx - 0.5)
            if 0.08 < u < 0.22 and at.lz < 0.3 and at.lz > 0.1:
                return N
            return mix(Lt, B, at.lz * 0.6)
        return None
    return Mat(base, f, noise=0.015, edge=0.25)


def platypus(sid, name, stands, big):
    """A platypus. The first is a squat ochre one with a big round head and a soft slate bill, button eyes, a pained
    brow, forever holding its head in its webbed hands; a flat tail in joints. Grown, it's tall and sleek, deep blue,
    long-limbed, webbed hands with hooked claws, a glowing teal mark on its brow, a long flat swimmer's tail."""
    m = Model()
    if not big:
        FUR, FUR_L, FUR_D, BELLY, BILL, BILL_L = "#E6B440", "#F8D676", "#B0841E", "#FFF0C4", "#5E6874", "#8692A0"
    else:
        FUR, FUR_L, FUR_D, BELLY, BILL, BILL_L = "#3A74C4", "#6CA2E6", "#1E4A8A", "#BCD8F2", "#3A4450", "#5E6C7C"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D), noise=0.02)
    bellym = Mat(BELLY, noise=0.02, edge=0.1)
    face = Mat(FUR, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.02)
    bill = bill_mat(BILL, BILL_L, "#20262C")
    jaw_m = Mat(BILL, open_mouth("#8A3A44", mix(BILL, "#000000", 0.25)), noise=0.015)
    footm = Mat(BILL, lambda a: rgb(BILL_L) if a.face == "top" else None, noise=0.015)
    web = web_mat(BILL_L, BILL)
    toe = Mat("#2A2A30", noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    if not big:
        m.bone("body", "root", [0, 4, 0], blob([-4, 2.5, -3.5], [8, 7, 7], furm) + [Cube([-3, 3, -3.9], [6, 5, 1], bellym)])
        hy = 9
        m.bone("head", "body", [0, hy + 0.5, -0.5], face_blob([-4.5, hy, -4.5], [9, 7, 8], face) +
               [Cube([-3, hy + 1.5, -8.5], [6, 2, 4.5], bill)])
        m.bone("jaw", "head", [0, hy + 1.5, -4.5], [Cube([-2.5, hy + 0.7, -8], [5, 1, 4], jaw_m)])
        m.eyes("head", x_inner=1.0, y=hy + 3.6, z=-4.5, w=2, h=2, iris="#141018", pupil="#141018", skin=FUR, grid=["PW", "PP"])
        brow = Mat(FUR_D, noise=0.02, edge=0)
        m.brows("head", [Cube([-3.4, hy + 5.8, -4.8], [2, 1, 1], brow, [0, 0, -22], [-1.2, hy + 6.2, -4.6])])
        m.bone("tuft", "head", [0, hy + 7, -1], [Cube([-1.5, hy + 6.6, -1.5], [3, 3, 0], Mat(FUR_L, brush(FUR_L, "#FFFFFF", FUR), noise=0.0, edge=0), [-70, 0, 0], [0, hy + 6.8, -1.5]),
                                                 Cube([0, hy + 6.6, -1.5], [0, 3, 3], Mat(FUR_L, brush(FUR_L, "#FFFFFF", FUR), noise=0.0, edge=0), [-70, 0, 0], [0, hy + 6.8, -1.5])])
        # Forever holding its aching head: the arms rest raised, the hands at its temples.
        upright_arms(m, 4.4, 8.4, -0.5, 2.5, 2.5, 2, furm, footm, hw=3, hh=2, fingers=3, finger_m=footm, finger_len=1.5, web=web,
                     rest=(-10, 0, 150), fore_rest=(0, 0, 48))
        upright_legs(m, 2.4, 3.5, 0.5, 2.5, furm, footm, 4, toe_m=toe, web=web)
        tailm = Mat(FUR_D, lambda a: rgb(mix(FUR_D, "#000000", 0.15)) if a.face == "bottom" else None, noise=0.03)
        tail_bones(m, "body", 3.6, 3.2, [(5, 1.5, 2.5), (4.5, 1.5, 2.5), (3.5, 1.5, 2)], tailm, (-28, 8, 8))
        k, walk, guard = 0.7, 0.5, 8
    else:
        m.bone("body", "root", [0, 10, 0], blob([-3.5, 9, -3], [7, 10, 6], furm) + [Cube([-2.5, 10, -3.4], [5, 7, 1], bellym)])
        m.bone("neck", "body", [0, 18.5, -0.5], [Cube([-2, 18, -2.5], [4, 3, 4], furm)], rotation=[-6, 0, 0])
        hy = 20.5
        m.bone("head", "neck", [0, hy + 0.5, -1], face_blob([-3, hy, -4.5], [6, 5, 6], face) + [Cube([-2, hy + 0.9, -9.8], [4, 1, 5.5], bill)],
               rotation=[6, 0, 0])
        m.bone("jaw", "head", [0, hy + 0.9, -4.5], [Cube([-1.7, hy + 0.1, -9.2], [3.4, 1, 4.8], jaw_m)])
        m.eyes("head", x_inner=0.7, y=hy + 2.4, z=-4.5, w=2, h=2, iris="#D23A3A", pupil="#1A0606", skin=FUR, iris_light="#FF8A6A", grid=["IW", "PI"])
        m.bone("mark", "head", [0, hy + 4.5, -4.55], [Cube([-1, hy + 3.6, -4.56], [2, 2, 0], glow_patch("#5AF0E0", "diamond"))])
        fin = Mat(FUR_D, brush(FUR, FUR_L, FUR_D), noise=0.0, edge=0)
        m.bone("crest", "head", [0, hy + 4.6, -1], [Cube([0, hy + 3.6, -1.5], [0, 3, 6], fin, [-20, 0, 0], [0, hy + 4.6, -1.5])])
        upright_arms(m, 4.4, 18, -0.5, 5, 5, 2, furm, footm, hw=3, hh=2, fingers=3, finger_m=footm, finger_len=2, web=web, claws=3,
                     rest=(-6, 0, 10), fore_rest=(-16, 0, 0))
        upright_legs(m, 2.3, 9.5, 0.5, 3, furm, footm, 5, toe_m=toe, web=web, knee_at=0.48, sharp=False)
        tailm = Mat(FUR_D, lambda a: rgb(mix(FUR_D, "#000000", 0.15)) if a.face == "bottom" else None, noise=0.03)
        tail_bones(m, "body", 9.6, 2.8, [(4, 1.5, 3), (3.6, 1.5, 3), (3, 1.5, 3), (2, 1.5, 3)], tailm, (-38, 10, 10, 8))
        k, walk, guard = 1.0, 0.7, 36
    fx_for(m, 0.7 if not big else 1.0)
    moves(m, "SCRATCH" if not big else "FURY SWIPES", "CONFUSION" if not big else "HYDRO PUMP")
    ex = {"idle": {"head": track(rotation=[0, 0, wave(1.6, 6)])}} if not big else {"battle": {"mark": track(scale=[wave(0.8, 0.15, 0, 1)] * 3)}}
    A, info = biped(name.lower(), m, walk=walk, stride=24 if not big else 30, k=k, extras=ex, guard=guard)
    write(sid, name, m, A, info, stands)


def achebill():
    platypus("psyduck", "ACHEBILL", 0.95, False)


def tidebill():
    platypus("golduck", "TIDEBILL", 1.8, True)


# ======================================================================================================
# The maned wolves: KINDLEPUP > PYREWOLF (in place of Growlithe, Arcanine)
# ======================================================================================================

def maned_wolf(sid, name, stands, big):
    """A maned wolf. The pup is russet with black socks, a cream ruff round its neck flecked with embers, big pointed
    ears, a cream muzzle and a bushy tail in joints with a pale tip. Grown, it stands tall on long black legs, a black
    mane down the back of its neck, a collar of flame round its throat, a great plumed tail ending in a flame."""
    m = Model()
    s = 1.0 if not big else 1.55
    S = lambda v: [x * s for x in v]
    FUR, FUR_L, FUR_D, BELLY, SOCK = "#D2682A", "#F09650", "#94421A", "#FFF0D6", "#241612"
    CREAM, CREAM_L = "#F4E2C0", "#FFF8E8"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=BELLY, below=(5.4 if not big else 9.4) * s), noise=0.02)
    face = Mat(FUR, lambda a: rgb(BELLY) if (a.face == "front" and a.y < (8.6 if not big else 13.2) * s) or a.face == "bottom" else None, noise=0.02)
    muzzle = Mat(CREAM, lambda a: rgb("#1A1210") if a.face == "front" and a.ly > 0.55 and abs(a.x) < 0.8 * s else None, noise=0.02)
    jaw_m = Mat(CREAM, open_mouth("#A8302A", "#E8D0B0"), noise=0.02)
    ear = ear_mat(FUR, "#F8E0C8", "fennec", tip=SOCK)
    top = 5 if not big else 9
    legm = Mat(FUR, lambda a: rgb(SOCK) if a.y < top * s * (0.55 if not big else 0.75) else None, noise=0.02)
    claw = Mat("#1A1210", noise=0.0, edge=0)
    tailm = Mat(FUR, fur(FUR, FUR_L, FUR_D), noise=0.03)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, top + 2.5, 0]), blob(S([-3, top, -5]), S([6, 5, 10]), furm))
    hy = top + 4
    m.bone("neck", "body", S([0, hy, -4.5]), blob(S([-2.2, hy - 1.5, -6.5]), S([4.4, 4.4 if big else 4, 3.5]), furm), rotation=[-16 if big else -10, 0, 0])
    hz = -6 if not big else -6.5
    hb = 1.0 if big else 1.2
    m.bone("head", "neck", S([0, hy + 1.5, hz]), face_blob(S([-3 * hb, hy - 0.3, hz - 4.5]), S([6 * hb, 4.8 * hb, 5]), face) +
           [Cube(S([-1.4, hy - 0.3, hz - 7]), [n(2.8 * s), n(2.4 * s), n(2.6 * s)], muzzle)], rotation=[16 if big else 10, 0, 0])
    m.bone("jaw", "head", S([0, hy + 0.6, hz - 3.5]), [Cube(S([-1.2, hy - 0.4, hz - 6.6]), [n(2.4 * s), 1, n(3 * s)], jaw_m)])
    m.eyes("head", x_inner=0.9 * s, y=(hy + 2.3 + (0 if big else 0.4)) * s, z=(hz - 4.5) * s, w=2, h=2, iris="#7A3A14" if not big else "#E0901A",
           pupil="#140804", skin=FUR, iris_light="#C07030" if not big else "#FFD060", grid=["PW", "PP"] if not big else ["IW", "PI"])
    m.pair("ear_right", "head", S([-1.8 * hb, hy + 4.4, hz - 2]), [Cube(S([-1.8 * hb - 2.2, hy + 4.2, hz - 2]), [n(4.2 * s), n(6 * s if not big else 5.4 * s), 0], ear)],
           rotation=[-6, 10, -14])
    neck = bone_of(m, "neck")
    ruff = shag_mat(CREAM, CREAM_L, "#D8C098")
    for a in (-70, -35, 0, 35, 70):
        r = math.radians(a)
        cx, cz = math.sin(r) * 2.4 * s, (-6.4 + (1 - math.cos(r)) * 1.6) * s
        neck.cubes.append(Cube([cx - 1.6 * s, (hy - 3) * s, cz], [n(3.2 * s), n(4 * s), 0], ruff, [12, a, 0], [cx, (hy + 1) * s, cz]))
    if not big:
        for i, (ex, ey, ez) in enumerate(((-1.6, hy - 1.4, -6.7), (1.2, hy - 2.2, -6.7), (0, hy - 0.6, -6.9))):
            neck.cubes.append(Cube(S([ex - 0.5, ey, ez]), [1, 1, 0], glow_patch("#FFA040", "flame")))
    else:
        for a in (-80, -45, -15, 15, 45, 80):
            r = math.radians(a)
            neck.cubes += flame([math.sin(r) * 2.6 * s, (hy - 1.6) * s, (-6.2 + (1 - math.cos(r)) * 1.8) * s], n(2.6 * s), n(4.6 * s))
        neck.cubes += blob(S([-2.8, hy - 3.2, -7.6]), S([5.6, 4.6, 3.6]), Mat(CREAM, fur(CREAM, CREAM_L, "#D8C098"), noise=0.03))
        neck.cubes += [Cube(S([-2.6, hy - 6, -7.7]), [n(5.2 * s), n(3.4 * s), 0], ruff)]
        mane = Mat(SOCK, brush(SOCK, "#3A2A22", SOCK), noise=0.0, edge=0)
        neck.cubes += [Cube([0, (hy + 2) * s, -6 * s], [0, n(3 * s), n(5 * s)], mane, [-30, 0, 0], [0, (hy + 2.5) * s, -6 * s])]
        bone_of(m, "body").cubes += [Cube([0, (top + 4.4) * s, (-5 + i * 2.2) * s], [0, n(2 * s), n(3 * s)], mane, [-14, 0, 0],
                                          [0, (top + 4.6) * s, (-5 + i * 2.2) * s]) for i in range(3)]
    stout_legs(m, 2.0 * s, (-3.6 * s, 3.6 * s), round(top * s), round(2 * s), legm, claw, 3, sharp=True, claw_len=1)
    n_seg = 4 if not big else 5
    segs = [((3.4 - i * 0.25) * s, (3.4 - i * 0.25) * s, (2.6 if not big else 2.8) * s) for i in range(n_seg)]
    names, zt = tail_bones(m, "body", (top + 3.4) * s, 4.6 * s, segs, tailm, (18, 2, -8, -10, -8)[:n_seg])
    tip = bone_of(m, names[-1])
    tip.cubes.append(Cube([-1.6 * s, tip.pivot[1] - 1.6 * s, zt - 0.6 * s], [n(3.2 * s), n(3.2 * s), n(1.6 * s)], Mat(CREAM, noise=0.03)))
    if big:
        tip.cubes += flame([0, tip.pivot[1] + 1.2 * s, zt + 0.2 * s], n(3.6 * s), n(5.4 * s))
    fx_for(m, 0.8 * s)
    moves(m, "BITE" if not big else "EXTREMESPEED", "EMBER" if not big else "FLAME WHEEL")
    A, info = four_legs(name.lower(), m, walk=0.5 if not big else 0.62, stride=32 if not big else 38, k=0.7 * s)
    write(sid, name, m, A, info, stands)


def kindlepup():
    maned_wolf("growlithe", "KINDLEPUP", 1.1, False)


def pyrewolf():
    maned_wolf("arcanine", "PYREWOLF", 2.2, True)


# ======================================================================================================
# The frogs: DROPTAD > FRILLFROG > BRAWLTOAD (in place of Poliwag, Poliwhirl, Poliwrath)
# ======================================================================================================

def drop_belly(base, mark, shine):
    """A pale belly with a raindrop drawn on it: round at the bottom, drawn up to a point, a glint on one side."""
    B, M, Sh = rgb(base), rgb(mark), rgb(shine)

    def inside(u, v):
        if v < 0.5:
            return u * u + ((v - 0.34) / 1.0) ** 2 < 0.06
        return abs(u) < 0.245 * (1 - (v - 0.5) / 0.36) and v < 0.86

    def f(at):
        if at.face != "front":
            return None
        u, v = at.lx - 0.5, at.ly
        if inside(u, v):
            return Sh if (-0.16 < u < -0.04 and 0.3 < v < 0.5) else M
        return None
    return Mat(base, f, noise=0.015, edge=0.15)


def frog_skin(base, light, dark, belly=None, below=None, warts=None):
    F = fur(base, light, dark, belly=belly, below=below, speck=0.15)

    def f(at):
        if warts and at.face in ("top", "nx", "px", "back") and hash01("wart", round(at.x), round(at.y), round(at.z)) < 0.1:
            return rgb(warts)
        return F(at)
    return f


def gills(m, bone, x, y, z, s, mat, n_=3, length=3.0, spread=24):
    """Feathery gill frills fanning back from behind the head, one above another."""
    b = bone_of(m, bone)
    for i in range(n_):
        yy = y + (i - (n_ - 1) / 2) * 1.6 * s
        b.cubes.append(Cube([-x, yy - 0.9 * s, z], [0, n(1.8 * s), n(length * s)], mat, [(i - (n_ - 1) / 2) * -spread, 58, 0], [-x, yy, z]))
        b.cubes.append(Cube([x, yy - 0.9 * s, z], [0, n(1.8 * s), n(length * s)], mat, [(i - (n_ - 1) / 2) * -spread, -58, 0], [x, yy, z]))


def droptad():
    """A teardrop of a tadpole: round and glossy teal, a pale ripple-marked belly, button eyes, a little smile, three
    pink feathery gill frills each side of its head, two short legs with webbed feet, and a long tail fin in joints."""
    m = Model()
    SK, SK_L, SK_D, BELLY = "#3AA8B4", "#6AD0D8", "#20707C", "#E8F8F4"
    skin = Mat(SK, frog_skin(SK, SK_L, SK_D), noise=0.015)
    belly = drop_belly(BELLY, "#3AA8B4", "#C8F4F4")
    jaw_m = Mat(SK, open_mouth("#A84A5A", SK_D), noise=0.015)
    gill = gill_mat("#E0607E", "#F8A8BC", "#FFD8E2")
    fin = Mat(SK_L, lambda a: rgb(mix(SK_L, "#FFFFFF", 0.4)) if a.ly > 0.75 or a.ly < 0.2 else None, noise=0.02, edge=0.1)
    footm = Mat(SK_D, noise=0.015)
    web = web_mat("#7ADCE0", SK_D)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 3, 0], [Cube([-3, 1.5, -2.5], [6, 3, 5], skin)])
    m.bone("head", "body", [0, 4, 0], blob([-4.5, 1.5, -4.5], [9, 5, 8], skin, b=2) + blob([-3.5, 5.5, -4], [7, 3, 7], skin) +
           blob([-2, 7.5, -2.5], [4, 2, 4], skin) + [Cube([-2.5, 1.8, -4.9], [5, 3, 1], belly)])
    m.bone("jaw", "head", [0, 5.4, -4.4], [Cube([-1.5, 4.8, -4.9], [3, 1, 1], jaw_m)])
    m.eyes("head", x_inner=0.7, y=5.8, z=-4, w=2, h=2, iris="#10141A", pupil="#10141A", skin=SK, grid=["PW", "PP"])
    gills(m, "head", 3.8, 5.6, -1.5, 1.0, gill, 3, 3.5, 26)
    upright_legs(m, 2.0, 2.5, 0.5, 2, skin, footm, 3, web=web, toe_m=Mat("#1A3A40", noise=0, edge=0), toe_len=1.5)
    tail_bones(m, "body", 4.6, 3, [(1, 4, 2.5), (1, 3.5, 2.5), (1, 3, 2.5), (1, 2, 2)], fin, (12, 0, -6, -6))
    fx_for(m, 0.6)
    moves(m, "BUBBLE", "HYPNOSIS")
    A, info = biped("droptad", m, walk=0.4, stride=30, k=0.6, guard=0,
                    extras={"idle": {"head": track(rotation=[wave(1.5, 4), 0, wave(3, 5)])}})
    write("poliwag", "DROPTAD", m, A, info, 0.6)


def frog(sid, name, stands, big):
    """An upright frog. The first is a round teal tree frog with a ripple-marked belly, bulging eyes, a wide mouth, pink
    gill frills behind its head, long webbed fingers tipped with sticky pale pads, and long folded frog legs. Grown, a
    broad-shouldered toad of a boxer: darker and warty, a great orange throat that puffs out, bulging arms, wrapped
    fists, and a scowl."""
    m = Model()
    if not big:
        SK, SK_L, SK_D, BELLY, LINE = "#3AA0B0", "#6ACAD4", "#1E6A78", "#EAF8F2", "#58A6AE"
    else:
        SK, SK_L, SK_D, BELLY, LINE = "#2A7A92", "#4EA6BC", "#164E62", "#DCEEE6", "#4A8C98"
    skin = Mat(SK, frog_skin(SK, SK_L, SK_D, warts=None if not big else "#3E92A8"), noise=0.015)
    belly = drop_belly(BELLY, SK, mix(SK_L, "#FFFFFF", 0.5))
    jaw_m = Mat(SK, open_mouth("#A84A5A", SK_D), noise=0.015)
    gill = gill_mat("#D85A78", "#F49AB0", "#FFD2DC")
    footm = Mat(SK_D, noise=0.015)
    web = web_mat(SK_L, SK_D)
    pad = Mat("#F6EEC0", noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    if not big:
        m.bone("body", "root", [0, 5, 0], blob([-5, 4, -4], [10, 9, 8], skin) + [Cube([-4, 5, -4.4], [8, 7, 1], belly)])
        hy = 12.5
        m.bone("head", "body", [0, hy, -0.5], face_blob([-4.5, hy, -4], [9, 4, 7], skin) +
               [Cube([-4.2, hy + 3, -3.5], [3, 2, 3], skin), Cube([1.2, hy + 3, -3.5], [3, 2, 3], skin)])
        m.bone("jaw", "head", [0, hy + 0.8, -3.6], [Cube([-3.5, hy, -4.4], [7, 1, 3.5], jaw_m)])
        m.eyes("head", x_inner=1.7, y=hy + 3.1, z=-3.5, w=2, h=2, iris="#E8C030", pupil="#10100A", skin=SK, iris_light="#FFE880",
               grid=["IW", "PP"])
        gills(m, "head", 4.6, hy + 2, -0.5, 1.0, gill, 3, 4.5, 22)
        upright_arms(m, 5.2, 11, -0.5, 3, 3, 2, skin, footm, hw=3, hh=1, fingers=3, finger_m=Mat(SK_D, lambda a: rgb("#F6EEC0") if a.ly < 0.4 else None,
                     noise=0, edge=0), finger_len=2.5, web=web, rest=(0, 0, 14), fore_rest=(-18, 0, 0))
        upright_legs(m, 3.2, 5, 0.5, 3, skin, footm, 5, web=web, toe_m=pad, thigh_w=4, knee_at=0.5, toe_len=2)
        moves(m, "DOUBLESLAP", "WATER GUN")
        k, walk, guard = 0.8, 0.55, 30
    else:
        m.bone("body", "root", [0, 9, 0], blob([-6, 8, -4.5], [12, 11, 9], skin) + [Cube([-4.5, 9, -4.9], [9, 8, 1], belly)] +
               blob([-6.5, 15, -4], [13, 4, 7], skin))
        m.bone("throat", "body", [0, 18, -4.5], blob([-3, 16, -6.2], [6, 3, 3], Mat("#F08A38", lambda a: rgb("#FFB468") if a.face == "front" else None,
                                                                                    noise=0.02)))
        hy = 19
        m.bone("head", "body", [0, hy, -0.5], face_blob([-5, hy, -4.5], [10, 4, 7], skin) +
               [Cube([-4.6, hy + 3, -4], [3, 2, 3], skin), Cube([1.6, hy + 3, -4], [3, 2, 3], skin)])
        m.bone("jaw", "head", [0, hy + 0.8, -4], [Cube([-4, hy, -4.9], [8, 1, 3.8], jaw_m)])
        m.eyes("head", x_inner=2.0, y=hy + 3.1, z=-4, w=2, h=2, iris="#E85A20", pupil="#10100A", skin=SK, iris_light="#FFA050", grid=["IW", "PP"])
        brow = Mat(SK_D, noise=0.02, edge=0)
        m.brows("head", [Cube([-4.4, hy + 4.7, -4.4], [3, 1, 1], brow, [0, 0, 20], [-2, hy + 5.2, -4.2])])
        gills(m, "head", 5.2, hy + 2, -0.5, 1.2, gill, 3, 4.5, 22)
        wrap = Mat("#F2EEE2", lambda a: rgb("#C8C0B0") if a.row % 2 == 0 and a.face != "bottom" else None, noise=0.02)
        upright_arms(m, 7, 17.5, -0.5, 5, 4, 3, skin, wrap, hw=5, hh=3, thick=4, fore_w=4, rest=(0, 0, 14), fore_rest=(-20, 0, 0))
        upright_legs(m, 3.8, 8.5, 0.5, 4, skin, footm, 6, web=web, toe_m=Mat(SK_D, noise=0, edge=0), thigh_w=5, knee_at=0.5, toe_len=2)
        moves(m, "SUBMISSION", "WATER GUN")
        k, walk, guard = 1.0, 0.7, 50
    fx_for(m, 0.8 if not big else 1.1)
    ex = {"cry": {"throat": track(scale=keys({0: [1, 1, 1], 0.25: [1.5, 1.6, 1.8], 0.8: [1.5, 1.6, 1.8], 1.2: [1, 1, 1]}))}} if big else None
    A, info = biped(name.lower(), m, walk=walk, stride=30, k=k, extras=ex, guard=guard)
    write(sid, name, m, A, info, stands)


def frillfrog():
    frog("poliwhirl", "FRILLFROG", 1.1, False)


def brawltoad():
    frog("poliwrath", "BRAWLTOAD", 1.8, True)


# ======================================================================================================
# The tarsiers: SLUMBERSIER > MINDSIER > SAGESIER (in place of Abra, Kadabra, Alakazam)
# ======================================================================================================

def tarsier(sid, name, stands, stage):
    """A tarsier of the mind: soft sand-gold fur, a dusk-violet mantle over its shoulders, great round ears, long thin
    fingers, long feet, and a long thin tail in joints ending in a tuft. The first sits curled up, fast asleep, its eyes
    shut. The second stands, its huge amber eyes wide open, a violet mark on its brow, a glowing stone in one hand.
    The last is tall and wise, a long white beard, a broad mantle, a glowing stone in each hand."""
    m = Model()
    FUR, FUR_L, FUR_D, FACE = "#D6AE6C", "#F0D498", "#9A7440", "#F4E4C2"
    MANT, MANT_L, MANT_D = ("#6A4A8E", "#8A6AB0", "#40285E")
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=FACE, below=None), noise=0.02)
    face = Mat(FUR, lambda a: rgb(FACE) if a.face in ("front", "bottom") else None, noise=0.02)
    mant = Mat(MANT, fur(MANT, MANT_L, MANT_D), noise=0.02)
    ear = ear_mat(FUR, "#F4B8B0", "round")
    jaw_m = Mat(FACE, open_mouth("#8A3A4A", "#D8C0A0"), noise=0.02)
    nose = Mat("#6A4A3A", noise=0, edge=0)
    handm = Mat(FUR_D, noise=0.02)
    pad = Mat(FUR_D, lambda a: rgb("#E8B8A0") if a.ly < 0.35 else None, noise=0, edge=0)
    tuft = Mat(FUR_L, brush(FUR_L, "#FFF4E0", FUR), noise=0.0, edge=0)
    orb = orb_mat("#C88AFF", "#6A3AB0")
    m.bone("root", None, [0, 0, 0])
    if stage == 0:
        # Curled up asleep: knees drawn up, the head sunk on its chest, the tail wrapped forward.
        m.bone("body", "root", [0, 3, 0], blob([-3.5, 2, -3], [7, 7, 6], furm) + blob([-4, 7, -3.4], [8, 3, 6.8], mant))
        hy = 8.5
        m.bone("head", "body", [0, hy, -0.5], face_blob([-4, hy, -4], [8, 7, 7], face) + [Cube([-0.5, hy + 2, -4.5], [1, 1, 1], nose)], rotation=[10, 0, 0])
        m.bone("jaw", "head", [0, hy + 1.2, -4], [Cube([-1, hy + 0.8, -4.3], [2, 1, 1], jaw_m)])
        shut = closed_eye_mat("#3A2418")
        m.bone("eyes_shut", "head", [0, hy + 3, -4], [Cube([-3.6, hy + 2.8, -4.03], [3, 2, 0], shut), Cube([0.6, hy + 2.8, -4.03], [3, 2, 0], shut)])
        m.pair("ear_right", "head", [-3.4, hy + 5.6, -1], [Cube([-6.6, hy + 4, -1], [4, 5, 0], ear)], rotation=[0, 30, 18])
        upright_arms(m, 3.8, 7.5, -1.5, 3, 2.5, 1.5, furm, handm, hw=2, hh=1, fingers=2, finger_m=pad, finger_len=2.5, rest=(-50, 0, -10),
                     fore_rest=(-30, 0, 0))
        # Sitting: the thighs run forward from the hips, the shins drop from the knees, long feet flat out in front.
        m.pair("leg_right", "body", [-2.2, 3, 0], [Cube([-3.2, 2, -4], [2, 2, 4.5], furm)], rotation=[-8, 0, 6])
        m.pair("shin_right", "leg_right", [-2.2, 3.5, -3.5], [Cube([-3, 1, -4.4], [n(1.6), 3, n(1.6)], furm)])
        m.pair("foot_right", "shin_right", [-2.2, 1, -3.6], [Cube([-3.2, 0, -8], [2, 1, 5], handm)] +
               [Cube([-3.2 + i * 0.7, 0, -9.2], [1, 1, 1.2], pad) for i in range(3)])
        names, zt = tail_bones(m, "body", 2.5, 2.6, [(1.2, 1.2, 2.5)] * 4, furm, (-24, 22, 14, 12))
        tip = bone_of(m, names[-1])
        tip.cubes += [Cube([0, tip.pivot[1] - 1.5, zt - 0.5], [0, 3, 4], tuft), Cube([-1.5, tip.pivot[1], zt - 0.5], [3, 0, 4], tuft)]
        moves(m, "HEADBUTT", "TELEPORT")
        k, walk, guard = 0.6, 0.45, 0
        ex = {"idle": {"body": track(scale=[wave(3, 0.03, 0, 1), wave(3, 0.04, 0, 1), wave(3, 0.03, 0, 1)]),
                       "head": track(rotation=[wave(3, 3, 0, 2), 0, wave(6, 4)])}}
        eyes = False
    else:
        big = stage == 2
        bh, hy = (9, 19) if not big else (11, 23)
        m.bone("body", "root", [0, 10, 0], blob([-3, 9, -2.5], [6, bh, 5], furm) + blob([-4.2, 9 + bh - 4, -3.2], [8.4, 4, 6.4], mant))
        if big:
            m.bones[-1].cubes += [Cube([-5.4, 9 + bh - 2, -3], [n(10.8), 2, 6], mant)]
        m.bone("head", "body", [0, hy, -0.5], face_blob([-4, hy, -4], [8, 7, 7], face) + [Cube([-0.5, hy + 2, -4.5], [1, 1, 1], nose)])
        m.bone("jaw", "head", [0, hy + 1.2, -4], [Cube([-1, hy + 0.8, -4.3], [2, 1, 1], jaw_m)])
        m.eyes("head", x_inner=0.4, y=hy + 2.6, z=-4, w=3, h=3, iris="#E89A1E", pupil="#100804", skin=FACE, iris_light="#FFD460",
               grid=["SJS", "JPW", "SIS"])
        m.bone("mark", "head", [0, hy + 6.2, -4.05], [Cube([-1, hy + 5.6, -4.06], [2, 1, 0], glow_patch("#C88AFF", "diamond"))])
        m.pair("ear_right", "head", [-3.4, hy + 5.6, -1], [Cube([-7.4, hy + 4, -1], [5, 6, 0], ear)], rotation=[0, 26, 14])
        if big:
            beard = shag_mat("#F6F2EC", "#FFFFFF", "#C8C0B8")
            bone_of(m, "jaw").cubes += [Cube([-2.5, hy - 6, -4.4], [5, 7, 0], beard), Cube([-2, hy - 5, -4.2], [0, 6, 3], beard, [0, -20, 0], [-2, hy, -4.2]),
                                        Cube([2, hy - 5, -4.2], [0, 6, 3], beard, [0, 20, 0], [2, hy, -4.2])]
        sh = 9 + bh - 1
        bottom = upright_arms(m, 4.2, sh, -0.5, 4 if not big else 5, 4 if not big else 5, 1.5, furm, handm, hw=2, hh=1, fingers=2, finger_m=pad,
                              finger_len=2.5, rest=(-30, 0, 12), fore_rest=(-40, 0, 0))
        for side in (("forearm_right",) if not big else ("forearm_right", "forearm_left")):
            fb = bone_of(m, side)
            hx = -4.2 if side.endswith("right") else 4.2
            fb.cubes.append(Cube([hx - 1.5, bottom - 2.6, -2.2], [3, 3, 3], orb))
        upright_legs(m, 2, 10, 0.5, 2, furm, handm, 6 if not big else 7, toe_m=pad, ntoes=3, knee_at=0.45)
        names, zt = tail_bones(m, "body", 10, 2.4, [(1.2, 1.2, 3.5)] * (4 if not big else 5), furm, (-50, 12, 10, 10, 10))
        tip = bone_of(m, names[-1])
        tip.cubes += [Cube([0, tip.pivot[1] - 1.5, zt - 0.5], [0, 3, 5], tuft), Cube([-1.5, tip.pivot[1], zt - 0.5], [3, 0, 5], tuft)]
        moves(m, "KINESIS" if not big else "CONFUSION", "PSYBEAM" if not big else "PSYCHIC")
        k, walk, guard = 0.9 if not big else 1.1, 0.65 if not big else 0.75, 20
        ex = {"idle": {"mark": track(scale=[wave(1.4, 0.15, 0, 1)] * 3)}, "battle": {"mark": track(scale=[wave(0.6, 0.2, 0, 1)] * 3)}}
        eyes = True
    fx_for(m, 0.6 if stage == 0 else (0.9 if stage == 1 else 1.1))
    A, info = biped(name.lower(), m, walk=walk, stride=26, k=k, extras=ex, guard=guard)
    write(sid, name, m, A, info, stands)


def slumbersier():
    tarsier("abra", "SLUMBERSIER", 1.0, 0)


def mindsier():
    tarsier("kadabra", "MINDSIER", 1.6, 1)


def sagesier():
    tarsier("alakazam", "SAGESIER", 2.0, 2)


# ======================================================================================================
# The gorillas: KNUCKLET > BRAWLAPE > QUADRILLA (in place of Machop, Machoke, Machamp)
# ======================================================================================================

def gorilla(sid, name, stands, stage):
    """A fighting gorilla: slate blue-grey fur, a bare charcoal face and chest, a crest of fur on its crown, red-orange
    eyes under a heavy brow, long thick arms with taped fists, short strong legs. The first is young and small. The
    second is big and heavy-shouldered, silvering across its back, a gold champion's belt. The last is a full
    silverback with four arms, the belt, and red wraps on all four fists."""
    m = Model()
    s = (1.0, 1.35, 1.55)[stage]
    S = lambda v: [x * s for x in v]
    FUR, FUR_L, FUR_D, SKIN, SKIN_L, SILVER = "#5E6C86", "#8090AA", "#3C4660", "#3C3A48", "#5E5A6E", "#C4CAD8"
    base = fur(FUR, FUR_L, FUR_D)

    def coat(at):
        if stage and at.face in ("back", "top", "nx", "px") and at.z > (0.5 if stage == 1 else -1) * s and at.y > 7 * s:
            return mix(SILVER, FUR_L, hash01("sil", round(at.x), round(at.y)) * 0.4)
        return base(at)
    furm = Mat(FUR, coat, noise=0.02)
    skin = Mat(SKIN, lambda a: rgb(SKIN_L) if a.face == "front" and a.ly > 0.6 else None, noise=0.02)
    face = Mat(FUR, lambda a: rgb(SKIN) if a.face == "front" else None, noise=0.02)
    muzzle = Mat(SKIN_L, lambda a: rgb("#1E1C24") if a.face == "front" and a.ly > 0.5 and 0.2 < a.lx < 0.8 and abs(a.lx - 0.5) > 0.08 else None, noise=0.02)
    jaw_m = Mat(SKIN_L, open_mouth("#8A3040", "#3A3640"), noise=0.02)
    wrap_c, wrap_d = ("#F0ECE0", "#C8C0B0") if stage < 2 else ("#D83A3A", "#A02424")
    wrap = Mat(wrap_c, lambda a: rgb(wrap_d) if a.row % 2 == 0 and a.face != "bottom" else None, noise=0.02)
    footm = Mat(SKIN, noise=0.02)
    toe = Mat("#26242C", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    hip = 5.5
    bh = 8 if stage == 0 else 9
    bw = 7 if stage == 0 else 8
    m.bone("body", "root", S([0, hip, 0]), blob(S([-bw / 2, hip - 0.5, -3]), S([bw, bh, 6]), furm) +
           [Cube(S([-bw / 2 + 1, hip + 2, -3.4]), [n((bw - 2) * s), n((bh - 3) * s), 1], skin)] +
           (blob(S([-bw / 2 - 1, hip + bh - 3.5, -3.2]), S([bw + 2, 4, 6.4]), furm) if stage else []))
    if stage:
        belt = Mat("#2A2630", lambda a: rgb("#E8C040") if (a.face == "front" and abs(a.lx - 0.5) < 0.22) else (rgb("#B8902A") if a.face == "front" and abs(a.lx - 0.5) < 0.3 else None),
                   glow=lambda a: rgb("#FFE070") if a.face == "front" and abs(a.lx - 0.5) < 0.12 and 0.3 < a.ly < 0.7 else None, noise=0.02)
        m.bones[-1].cubes.append(Cube(S([-bw / 2 - 0.3, hip - 0.2, -3.3]), [n((bw + 0.6) * s), n(2 * s), n(6.6 * s)], belt))
    hy = hip + bh - 1
    m.bone("head", "body", S([0, hy, -1.5]), face_blob(S([-3, hy, -4.5]), S([6, 5.5, 5.5]), face) +
           [Cube(S([-2, hy, -5.6]), [n(4 * s), n(2.5 * s), n(1.4 * s)], muzzle),
            Cube(S([-3.2, hy + 3.4, -4.9]), [n(6.4 * s), n(1.1 * s), n(1.2 * s)], Mat(FUR_D, noise=0.02, edge=0))])
    crest = Mat(FUR_D, brush(FUR_D, FUR, FUR_D), noise=0.0, edge=0)
    bone_of(m, "head").cubes.append(Cube(S([0, hy + 4.4, -3.5]), [0, n((3 + stage) * s), n(6 * s)], crest, [-40, 0, 0], S([0, hy + 5, -3.5])))
    m.bone("jaw", "head", S([0, hy + 0.6, -4.5]), [Cube(S([-1.6, hy - 0.4, -5.6]), [n(3.2 * s), 1, n(2 * s)], jaw_m)])
    m.eyes("head", x_inner=0.6 * s, y=(hy + 2.3) * s, z=-4.5 * s, w=2, h=1 if stage == 0 else 2, iris="#E2541E", pupil="#180604",
           skin=SKIN, iris_light="#FF9450", grid=["IP"] if stage == 0 else ["LL", "IP"])
    m.pair("ear_right", "head", S([-3, hy + 2.6, -2]), [Cube(S([-3.6, hy + 1.8, -2.6]), [1, n(1.6 * s), n(1.4 * s)], skin)])
    sh = hip + bh - 1.5
    ua, fa = (4, 4) if stage == 0 else (5, 5)
    aw = 2 if stage == 0 else 3
    upright_arms(m, (bw / 2 + 1.2) * s, sh * s, -0.5 * s, ua * s, fa * s, n(aw * s) if stage else 2, furm, wrap, hw=n((aw + 1.4) * s), hh=n(2 * s),
                 thick=n((aw + 1) * s), fore_w=n((aw + 1) * s), rest=(0, 0, 14), fore_rest=(-16, 0, 0))
    if stage == 2:
        upright_arms(m, (bw / 2 + 0.8) * s, (sh - 3.5) * s, -0.5 * s, 4 * s, 4 * s, n(2.4 * s), furm, wrap, hw=n(3.6 * s), hh=n(2 * s),
                     thick=n(3.2 * s), fore_w=n(3.2 * s), name="arm2", rest=(-20, 0, 24), fore_rest=(-40, 0, 0))
    upright_legs(m, 2.2 * s, hip * s, 0.5 * s, n(2.6 * s), furm, footm, n(4 * s), toe_m=toe, ntoes=4, thigh_w=n(3.4 * s), knee_at=0.5)
    fx_for(m, s)
    moves(m, ("LOW KICK", "KARATE CHOP", "CROSS CHOP")[stage], ("KARATE CHOP", "SEISMIC TOSS", "SUBMISSION")[stage])
    ex = None
    if stage == 2:
        ex = {"idle": {"arm2_right": track(rotation=[wave(3, 4, 90), 0, wave(3, 3)]), "arm2_left": track(rotation=[wave(3, 4, 270), 0, wave(3, -3)])},
              "battle": {"arm2_right": track(rotation=[wave(0.6, 5, 180, -30), 0, 8]), "arm2_left": track(rotation=[wave(0.6, 5, 0, -30), 0, -8]),
                         "forearm2_right": track(rotation=[wave(0.6, 6, 60, -40), 0, 0]), "forearm2_left": track(rotation=[wave(0.6, 6, 240, -40), 0, 0])},
              "walk": {"arm2_right": track(rotation=[wave(0.75, 18), 0, 0]), "arm2_left": track(rotation=[wave(0.75, -18), 0, 0])},
              "cry": {"arm2_right": track(rotation=keys({0: [0, 0, 0], 0.25: [-60, 0, 40], 0.85: [-60, 0, 40], 1.4: [0, 0, 0]})),
                      "arm2_left": track(rotation=keys({0: [0, 0, 0], 0.25: [-60, 0, -40], 0.85: [-60, 0, -40], 1.4: [0, 0, 0]}))}}
    A, info = biped(name.lower(), m, walk=0.55 + 0.1 * stage, stride=26, k=s, slow=1.0 + 0.1 * stage, extras=ex, guard=48)
    write(sid, name, m, A, info, stands)


def knucklet():
    gorilla("machop", "KNUCKLET", 1.1, 0)


def brawlape():
    gorilla("machoke", "BRAWLAPE", 1.8, 1)


def quadrilla():
    gorilla("machamp", "QUADRILLA", 2.2, 2)


ALL3 = (leafgrub, huskalis, veilmoth, stinglet, barbcase, lancewasp, coilet, frillasp, pangrit, spirepang, lunabun, lunahare,
        fennember, solfennec, puffchin, choruschin,
        duskbat, nightmaw, tuberlet, droopbud, rafflora, shroomshell, funguard, fuzzgnat, toxilace, starsnout, triosnout, lynxkit, sabrelynx,
        achebill, tidebill, kindlepup, pyrewolf, droptad, frillfrog, brawltoad, slumbersier, mindsier, sagesier, knucklet, brawlape,
        quadrilla)


def main(names):
    for f in ALL3:
        if not names or f.__name__ in names:
            f()


if __name__ == "__main__":
    main(set(sys.argv[1:]))
