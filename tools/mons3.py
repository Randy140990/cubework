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
from monanims import crawler, cocoon, insect  # noqa: E402
from mons import leaf_mat, sharp_claw_mat, fx_for, open_mouth  # noqa: E402


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


def wing_paint(base, vein, edge, root_x, span, spot=None, spot_core=None, hind=False, tint=None):
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


ALL3 = (leafgrub, huskalis, veilmoth, stinglet, barbcase, lancewasp)


def main(names):
    for f in ALL3:
        if not names or f.__name__ in names:
            f()


if __name__ == "__main__":
    main(set(sys.argv[1:]))
