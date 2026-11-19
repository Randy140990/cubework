"""
Retromon's own creatures, part three: the Kanto creatures beyond the first batches, built the same way as mons.py and
mons2.py (cubes painted in 3D, our own designs, the same species underneath).

Run: python3 tools/mons3.py [names]   (all if none)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, face_blob, hash01, mix, ramp, rgb, track, wave, keys, write, underside  # noqa: E402
from monanims import crawler, cocoon, insect, serpent, four_legs, hopper, flyer, burrower, biped, floater, ooze, bird  # noqa: E402
from mons import leaf_mat, sharp_claw_mat, fx_for, open_mouth, stout_legs, whisker_mat, dragon_wing, membrane_mat, shag_mat  # noqa: E402
from monlib import flame, flame_shape, eye_mat, mirror_cube, bounce  # noqa: E402
from mons import feather_plane, jointed_wing  # noqa: E402
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
    """A jade chrysalis, hard as armour, standing tall and curving forward like a crescent: broad in the middle, its top
    drawn forward into a hooked point, plates edged in gold, a silk thread holding it up from the back, and sleepy eyes
    under heavy lids."""
    m = Model()
    J, JL, JD, SEAM = "#5FB56A", "#B8EAB0", "#2E7040", "#D8B04A"
    shell = Mat(J, shell_paint(J, JL, JD, SEAM), noise=0.015)
    silk = Mat("#F4F2EC", lambda a: CLEAR if a.col % 2 else None, noise=0.0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    # From the bottom up: a rounded foot, the swelling middle, the shoulders leaning back a little.
    m.bone("body", "root", [0, 0, 0], blob([-3, 0, -2.6], [6, 3, 5.4], shell) + blob([-4.4, 3, -3.4], [8.8, 4, 7.2], shell, b=2) +
           blob([-4.6, 7, -3.2], [9.2, 4, 7.4], shell, b=2) + blob([-4.2, 11, -2.6], [8.4, 3, 6.8], shell))
    # The head: the face leaning forward, then the hooked crown curving over it to a point.
    m.bone("head", "body", [0, 14, 0], blob([-3.8, 14, -3.8], [7.6, 3.4, 6.6], shell) +
           [Cube([-3, 17.2, -3.4], [6, 2, 5.6], shell), Cube([-2, 18.8, -3.6], [4, 2, 4.4], shell, [-18, 0, 0], [0, 18.8, -1]),
            Cube([-1.2, 20.2, -4.6], [2.4, 1.6, 3], shell, [-38, 0, 0], [0, 20.2, -2]), Cube([-0.6, 20.6, -6.4], [1.2, 1.2, 2], shell, [-55, 0, 0], [0, 20.6, -4.4])])
    m.eyes("head", x_inner=0.6, y=14.8, z=-3.8, w=2, h=2, iris="#2A6A2A", pupil="#0A1A0A", skin=J, iris_light="#7ACB6A", grid=["LL", "IP"])
    m.brows("head", [Cube([-2.9, 16.7, -4.1], [2.6, 1, 0.6], Mat(JD, noise=0.01, edge=0), [0, 0, 10], [-0.6, 17.1, -4])])
    m.bone("silk", "head", [0, 20, 1.5], [Cube([-0.5, 20.5, 1.5], [0, 7, 1], silk, [0, 0, 8], [0, 20.5, 1.5]),
                                         Cube([0.5, 20.5, 1.8], [0, 8, 1], silk, [0, 0, -6], [0, 20.5, 1.5])])
    fx_for(m, 0.9)
    moves(m, "TACKLE", "HARDEN")
    A, info = cocoon("huskalis", m, k=0.9, extras={"idle": {"silk": track(rotation=[wave(2, 4), 0, wave(3, 5)])}})
    write("metapod", "HUSKALIS", m, A, info, 1.05)


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
    m.pair("wing_right", "body", [-3, 14.2, -1.5], [Cube([-20, 14.2, -7.5], [17, 0, 11], fore, [0, -12, 0], [-3, 14.2, -1.5])], rotation=[0, 0, 40])
    m.pair("hindwing_right", "body", [-3, 12.6, 2.5], [Cube([-15, 12.6, 2.2], [12, 0, 9.5], hind, [0, 14, 0], [-3, 12.6, 2.5])], rotation=[0, 0, 18])
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
    """A hard golden cocoon hanging like a lantern: a rounded hood over its face, dark armour bands round it, sharp
    pointed barbs along its ridges, two little arms folded tight, and narrow eyes glaring out under hard brows."""
    m = Model()
    GO, GOL, GOD, PLATE = "#E8B030", "#FFDC70", "#9A6A14", "#5E3E14"
    shell = Mat(GO, shell_paint(GO, GOL, GOD, PLATE), noise=0.015)
    plate = Mat(PLATE, shell_paint(PLATE, "#8A6430", "#3A2408", "#2A1A06", facets=False), noise=0.015)
    barb = taper_mat("#3A2610", "#F0E4C0")
    brow = Mat(PLATE, noise=0.01, edge=0)
    m.bone("root", None, [0, 0, 0])
    body = blob([-2.6, 0, -2.4], [5.2, 2.5, 4.8], shell) + blob([-4, 2.5, -3.4], [8, 3, 6.8], plate) + blob([-4.6, 5.5, -3.6], [9.2, 4, 7.4], shell, b=2) + \
        blob([-4.4, 9.5, -3.4], [8.8, 2.5, 7], plate)
    # Barbs: sharp points sticking out along the bands, sideways and back.
    for y in (4, 10.5):
        for sx in (-1, 1):
            body += spike(sx * 4.2, y, 0, 3.5, [0, -sx * 90, 0], barb, 2.2)
            body += spike(sx * 3.2, y, 2.4, 3, [0, -sx * 40, 0], barb, 2)
        body += spike(0, y, 3.2, 3.5, [0, 0, 0], barb, 2.2)
    body += [Cube([-4.6, 6.2, -4.1], [1.8, 2.6, 1.4], shell, [0, 0, -12], [-3.6, 7.6, -3.4]), Cube([2.8, 6.2, -4.1], [1.8, 2.6, 1.4], shell, [0, 0, 12], [3.6, 7.6, -3.4])]
    m.bone("body", "root", [0, 0, 0], body)
    # The hood: a rounded dome over the face, its rim jutting out over the eyes, a barb on top.
    hd = blob([-3.8, 12, -3.4], [7.6, 3.4, 6.6], shell) + blob([-4, 15.2, -3.6], [8, 2.6, 7], plate) + [Cube([-2.6, 17.6, -2.4], [5.2, 1.6, 4.8], plate)]
    hd += spike(0, 19, -0.4, 3, [-80, 0, 0], barb, 1.4) + [Cube([-3.6, 15.2, -4.3], [7.2, 1, 1.2], plate)]
    m.bone("head", "body", [0, 12, 0], hd)
    m.eyes("head", x_inner=0.5, y=13.4, z=-3.4, w=2, h=1, iris="#E8E040", pupil="#2A1A04", skin=GO, iris_light="#FFF8A0", grid=["IP"])
    m.brows("head", [Cube([-3.0, 14.4, -3.9], [2.8, 1, 1], brow, [0, 0, 16], [-0.5, 14.9, -3.6])])
    fx_for(m, 0.9)
    moves(m, "POISON STING", "HARDEN")
    A, info = cocoon("barbcase", m, k=0.9)
    write("kakuna", "BARBCASE", m, A, info, 1.0)


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


def snake_paint(base, light, dark, belly, mark, below, kind="chevron", mark_edge=None):
    """A snake's skin: small overlapping scales in staggered rows all over (each catching the light along its front
    edge, shadowed at its back), broad pale plates across the belly, and down the back a pattern: chevrons, or a chain
    of diamonds edged in dark."""
    B, Lt, Dk, Be, Mk = rgb(base), rgb(light), rgb(dark), rgb(belly), rgb(mark)
    Me = rgb(mark_edge) if mark_edge else Dk

    def scaled(at, c):
        if at.face in ("top", "bottom"):
            u, v = at.z, at.x
        elif at.face in ("nx", "px"):
            u, v = at.z, at.y
        else:
            u, v = at.x, at.y
        rv = v * 1.3
        r = math.floor(rv)
        fu = (u * 1.3 + (r % 2) * 0.5) % 1.0
        fv = rv % 1.0
        if fu > 0.8 or fv < 0.16:
            return c * 0.78
        if fu < 0.3:
            return mix(c, Lt, 0.2)
        return c

    def f(at):
        if at.face == "bottom" or at.y < below:
            return Be * (0.8 if (at.z * 1.5) % 1.0 < 0.2 else 1.0)
        if at.face == "top" or (at.side and at.ly > 0.7):
            zz = at.z
            if kind == "chevron":
                q = (zz % 3.2) / 3.2
                d = abs(abs(at.x) - (q - 0.2) * 2.2)
                if d < 0.42:
                    return scaled(at, Mk)
            else:
                q = ((zz % 4.4) / 4.4 - 0.5) * 4.4
                r = abs(at.x) * 1.15 + abs(q) * 0.7
                if r < 1.05:
                    return scaled(at, Mk if r < 0.75 else Me)
            return scaled(at, mix(B, Lt, 0.25))
        return scaled(at, mix(Dk, B, 0.35 + 0.65 * at.ly))
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
    """A viper. The small one slim and lilac with copper chevrons down its back, a broad viper's head with brow ridges
    and slit-pupilled eyes, a forked tongue, and a little copper spade at the end of its tail; it lies coiled, its head
    raised, and rattles the tip of its tail. Grown, long and deep violet with a chain of gold diamonds down its back, a
    heavy brow, long fangs, and a great ribbed frill of teal and gold round its neck that flares when it hisses. Both
    are covered in fine scales, with broad plates down the belly, and slither in waves running down their length."""
    m = Model()
    s = 1.0 if not big else 1.45
    S = lambda v: [x * s for x in v]
    if not big:
        BASE, LT, DK, BELLY, MARK, MEDGE = "#9A7AC0", "#C8B0E4", "#5E4A80", "#F0E0B0", "#C87A3A", None
    else:
        BASE, LT, DK, BELLY, MARK, MEDGE = "#5A3A7A", "#8A6AAA", "#2E1E44", "#F0DCA0", "#E8B030", "#1A1024"
    skin = Mat(BASE, snake_paint(BASE, LT, DK, BELLY, MARK, 0.5 * s, "chevron" if not big else "diamond", MEDGE), noise=0.015)
    headp = snake_paint(BASE, LT, DK, BELLY, MARK, 0.4 * s, "none")
    headm = Mat(BASE, lambda a: rgb(BELLY) if a.face == "bottom" else headp(a), noise=0.015)
    ridge = Mat(DK, lambda a: mix(DK, BASE, 0.5) if a.face == "top" else None, noise=0.01, edge=0)
    jaw_m = Mat(BELLY, open_mouth("#8A2A3A", "#D8C090"), noise=0.02)
    fang = Mat("#FFFFF4", noise=0.0, edge=0)
    tongue = Mat("#D8304A", lambda a: CLEAR if (a.lz < 0.3 and abs(a.lx - 0.5) < 0.2) else None, noise=0, edge=0, flat=True)
    spade = Mat(MARK, lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (1 - abs(a.lz - 0.5) * 2) + 0.05 else None, noise=0.01, edge=0, flat=True)
    nost = Mat("#140C18", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 1.4, -1]), [Cube(S([-1.6, 0, -2.4]), [n(3.2 * s), n(2.8 * s), n(4.2 * s)], skin)])
    # The body: many short segments tapering to the tail, lying straight at rest (its curves come from how it moves).
    nseg = 10 if not big else 12
    segs, prev, z = [], "body", 1.6
    L = 2.6
    for i in range(nseg):
        t = i / (nseg - 1)
        w, h = (3.0 - 2.0 * t ** 1.3) * s, (2.7 - 1.7 * t ** 1.3) * s
        nm = f"seg{i + 1}"
        cubes = [Cube([-w / 2, 0, (z - 0.2) * s], [n(w), n(h), n((L + 0.4) * s)], skin)]
        if i == nseg - 1:
            cubes.append(Cube([-1.5 * s, 0.3 * s, (z + L) * s], [n(3 * s), 0, n(3 * s)], spade if not big else Mat(DK, lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (1 - a.lz) else None, noise=0, edge=0, flat=True)))
        m.bone(nm, prev, [0, 1.2 * s, z * s], cubes)
        segs.append(nm)
        prev, z = nm, z + L
    r1, r2 = (-55, -22) if not big else (-68, -18)
    m.bone("neck", "body", S([0, 1.4, -2.4]), [Cube(S([-1.4, 0.2, -6.4]), [n(2.8 * s), n(2.8 * s), n(4.4 * s)], skin)], rotation=[r1, 0, 0])
    m.bone("neck2", "neck", S([0, 1.6, -6.2]), [Cube(S([-1.3, 0.3, -9.8]), [n(2.6 * s), n(2.6 * s), n(3.9 * s)], skin)], rotation=[r2, 0, 0])
    # A viper's head: broad at the back over the jaws, narrowing to a blunt snout, a ridge over each eye.
    hz = -9.6
    m.bone("head", "neck2", S([0, 1.8, hz]), face_blob(S([-2.3, 0.6, hz - 3.6]), S([4.6, 2.8, 3.8]), headm) +
           [Cube(S([-1.6, 0.6, hz - 5.4]), [n(3.2 * s), n(2.2 * s), n(1.9 * s)], headm),
            Cube(S([-2.5, 3.2, hz - 3.4]), [n(1.6 * s), 1, n(2.6 * s)], ridge, [0, 0, 10], S([-1, 3.4, hz - 2])),
            Cube(S([0.9, 3.2, hz - 3.4]), [n(1.6 * s), 1, n(2.6 * s)], ridge, [0, 0, -10], S([1, 3.4, hz - 2])),
            Cube(S([-1.0, 2.2, hz - 5.45]), [1, 1, 0], nost), Cube(S([0.4, 2.2, hz - 5.45]), [1, 1, 0], nost)],
           rotation=[-(r1 + r2) - 5, 0, 0])
    hd = bone_of(m, "head")
    hd.cubes += [Cube(S([-1.1, 0.0, hz - 5.2]), [1, n(1.4 * s), 1], fang), Cube(S([0.3, 0.0, hz - 5.2]), [1, n(1.4 * s), 1], fang)]
    m.bone("jaw", "head", S([0, 0.9, hz - 1.5]), [Cube(S([-1.9, 0.0, hz - 5.2]), [n(3.8 * s), 1, n(4 * s)], jaw_m)])
    m.bone("tongue", "jaw", S([0, 0.8, hz - 5]), [Cube(S([-0.5, 0.8, hz - 8]), [n(s), 0, n(3 * s)], tongue)])
    m.eyes("head", 2.3 * s, 2.0 * s, (hz - 3.6) * s, h=2, corner=(1, 1), half=2.3 * s, iris="#F0C030" if not big else "#E84030", pupil="#140C04",
           skin=BASE, iris_light="#FFE890", grid=["DLLD", "JIPI", "IIPJ", "SIIS"])
    if big:
        m.bone("hood", "neck2", S([0, 3.0, -6.4]), [Cube(S([-6, 3.0, -11.6]), [n(12 * s), 0, n(7.5 * s)], frill_mat("#2AA89A", "#E0B040", "#1A2A40", "#F0D070"))])
    fx_for(m, 0.7 * s)
    moves(m, "WRAP" if not big else "BITE", "ACID" if not big else "GLARE")
    if not big:
        # Coiled at rest: each joint curling the same way, the tail tip rattling.
        coil = [30, 34, 36, 36, 36, 36, 34, 30, 20, 10]
        rattle = {segs[-1]: track(rotation=[0, wave(0.12, 14), 0]), segs[-2]: track(rotation=[0, wave(0.12, 6, 40), 0])}
    else:
        coil = [22, 22, 18, -22, -26, -26, -20, 22, 24, 20, -14, -10]
        rattle = {}
    A, info = serpent(name.lower(), m, segs, k=0.6 * s, coil=coil, extras={"idle": rattle, "battle": rattle})
    write(sid, name, m, A, info, stands)


def coilet():
    viper("ekans", "COILET", 0.55, False)


def frillasp():
    viper("arbok", "FRILLASP", 1.15, True)


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
    rim round each inner ear, an ember mark on its brow, big glossy eyes, and a bushy tail in joints ending in a flame.
    Grown, tall and golden, slender-legged, a ruff of flame-tipped fur at its neck, and five long plumed tails fanned out
    behind it like the rays of the sun, each in joints and each tipped with flame."""
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
    tipm = Mat(BELLY, fur(BELLY, "#FFFFFF", FUR_L), noise=0.03)
    m.bone("root", None, [0, 0, 0])
    top = 5 if not big else 7.5
    m.bone("body", "root", S([0, top + 2.5, 0]), blob(S([-3, top, -5]), S([6, 5, 10]), furm) +
           [Cube(S([-2.4, top + 0.4, -5.4]), [n(4.8 * s), n(4 * s), 1], Mat(BELLY, noise=0.02))])
    hy = top + 4
    m.bone("neck", "body", S([0, hy, -4.5]), blob(S([-2.2, hy - 1.5, -6.5]), S([4.4, 4, 3.5]), furm), rotation=[-12, 0, 0])
    m.bone("head", "neck", S([0, hy + 1.5, -6]), face_blob(S([-3, hy, -10.5]), S([6, 4.6, 5]), face) +
           [Cube(S([-1.4, hy, -12.6]), [n(2.8 * s), n(2.2 * s), n(2.2 * s)], muzzle)], rotation=[12, 0, 0])
    m.bone("jaw", "head", S([0, hy + 0.6, -9.5]), [Cube(S([-1.2, hy - 0.4, -12.2]), [n(2.4 * s), 1, n(2.6 * s)], jaw_m)])
    # Big glossy eyes: a dark lash line over them, a bright catch-light, the iris paler below the pupil.
    m.eyes("head", x_inner=0.7 * s, y=(hy + 1.6) * s, z=-10.5 * s, w=2, h=3, iris="#B85A14" if not big else "#C8301A", pupil="#1A0A04", skin=FUR,
           iris_light="#FFB050" if not big else "#FF7A4A", grid=["LL", "PW", "JI"])
    m.bone("mark", "head", S([0, hy + 4, -10.52]), [Cube(S([-0.8, hy + 3.4, -10.53]), [n(1.6 * s), n(1.4 * s), 0], glow_patch("#FF9A3A", "flame"))])
    m.pair("ear_right", "head", S([-1.8, hy + 4.4, -8]), [Cube(S([-4.2, hy + 4.2, -8]), [n(4.4 * s), n(7.5 * s), 0], ear)], rotation=[-6, 10, -16])
    stout_legs(m, 2.0 * s, (-3.6 * s, 3.6 * s), round(top * s), round(2 * s), legm, claw, 2, sharp=True, claw_len=1)
    if not big:
        segs = [((3.2 - i * 0.3) * s, (3.2 - i * 0.3) * s, 3 * s) for i in range(4)]
        names, zt = tail_bones(m, "body", (top + 3.4) * s, 4.6 * s, segs, tailm, (12, 6, -2, -8))
        tip = bone_of(m, names[-1])
        tip.cubes += flame([0, tip.pivot[1] + 0.5 * s, zt - 1.2 * s], round(4.5 * s), round(7 * s))
        tails = []
    else:
        neck = bone_of(m, "neck")
        for i, a in enumerate((-60, -25, 25, 60)):
            neck.cubes += flame([math.sin(math.radians(a)) * 2.6 * s, (hy + 1.6) * s, (-5 + math.cos(math.radians(a)) * 0.8) * s], round(2.6 * s), round(4 * s))
        # Five tails fanned out like the sun's rays: the middle one up the back, two either side spread wide.
        tails = []
        y0, z0 = (top + 3.6) * s, 4.4 * s
        for j, (yaw, lift) in enumerate(((0, 22), (-30, 14), (30, 14), (-58, 4), (58, 4))):
            prev, z = "body", z0
            names = []
            for i in range(4):
                w = (2.6 - i * 0.35) * s
                nm = f"tail_{j}_{i}"
                cubes = [Cube([-w / 2, y0 - w / 2, z - 0.3], [n(w), n(w), n(2.8 * s + 0.3)], tailm if i < 3 else tipm)]
                rot = [lift if i == 0 else (6 if i == 1 else -6), yaw if i == 0 else 0, 0]
                m.bone(nm, prev, [0, y0, z], cubes, rotation=rot)
                names.append(nm)
                prev, z = nm, z + 2.8 * s
            bone_of(m, names[-1]).cubes += flame([0, y0 + 0.4 * s, z - 0.8 * s], round(3.4 * s), round(5.6 * s))
            tails.append(names)
    fx_for(m, 0.8 * s)
    moves(m, "QUICK ATTACK", "EMBER" if not big else "FIRE SPIN")
    sway = {}
    for j, names in enumerate(tails):
        for i, nm in enumerate(names):
            sway[nm] = track(rotation=[wave(2.2, 4 + i * 2, i * 50 + j * 40), wave(1.6, 6 + i * 3, i * 45 + j * 70), 0])
    A, info = four_legs(name.lower(), m, walk=0.55 if not big else 0.7, stride=32, k=0.7 * s,
                        extras={"idle": sway, "battle": sway, "walk": sway} if tails else None)
    write(sid, name, m, A, info, stands)


def fennember():
    fennec("vulpix", "FENNEMBER", 0.75, False)


def solfennec():
    fennec("ninetales", "SOLFENNEC", 1.4, True)


# ======================================================================================================
# The singing chinchillas: PUFFCHIN > CHORUSCHIN (in place of Jigglypuff, Wigglytuff)
# ======================================================================================================

def chinchilla(sid, name, stands, big):
    """A singing chinchilla: a round ball of the softest silvery pink fur, enormous round ears pink inside, long
    whiskers, big glossy dark eyes, a curl of a forelock, tiny hands held to its chest, and a great bushy tail curled
    up its back. It puffs itself up to sing. Grown, pear-shaped and lush, a thick ruff at its throat, long soft ears
    drooping down its back, longer feet, and a still bigger plume of a tail."""
    m = Model()
    s = 1.0 if not big else 1.7
    S = lambda v: [x * s for x in v]
    FUR, FUR_L, FUR_D, BELLY = "#E8CCD8", "#FFF0F6", "#A88A9C", "#FFF6F2"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=BELLY, below=2.5 * s, speck=0.6), noise=0.03)
    ear = ear_mat(FUR, "#F8A8C0", "round")
    longear = ear_mat(FUR, "#F8A8C0", "rabbit")
    jaw_m = Mat(BELLY, open_mouth("#C84A6A", "#F0D0D8"), noise=0.02)
    nose = Mat("#E8789A", noise=0.0, edge=0)
    paw = Mat(BELLY, lambda a: rgb("#F2B8C8") if a.face == "bottom" else None, noise=0.02)
    claw = Mat("#B08898", noise=0.0, edge=0)
    plume = Mat(FUR_L, brush(FUR, FUR_L, FUR_D, tip="#FFFFFF", tip_from=0.75), noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    if not big:
        m.bone("body", "root", S([0, 4, 0]), blob(S([-4.5, 1.2, -4]), S([9, 5, 8]), furm, b=2) + [Cube(S([-3, 1.6, -4.3]), [n(6 * s), n(4 * s), 1], Mat(BELLY, noise=0.03))])
        m.bone("head", "body", S([0, 6, 0]), blob(S([-4.5, 5.8, -4.5]), S([9, 5, 9]), furm, b=2) +
               [Cube(S([-0.6, 7.4, -4.8]), [n(1.2 * s), 1, 1], nose)])
        hy, hz, eh = 7.8, -4.5, 7
    else:
        m.bone("body", "root", S([0, 5, 0]), blob(S([-5, 1.5, -4.5]), S([10, 7, 9]), furm, b=2) + [Cube(S([-3.4, 2, -4.8]), [n(6.8 * s), n(5 * s), 1], Mat(BELLY, noise=0.03))])
        m.bone("head", "body", S([0, 9, -0.5]), blob(S([-4, 8.4, -4.5]), S([8, 5.6, 8]), furm, b=2) +
               [Cube(S([-0.6, 10.4, -4.8]), [n(1.2 * s), 1, 1], nose)])
        bone_of(m, "body").cubes += [Cube(S([-4.6, 7.4, -4.8]), [n(9.2 * s), n(2.4 * s), n(2 * s)], Mat(FUR_L, noise=0.04, edge=0))]
        hy, hz, eh = 10.8, -4.5, 10
    m.bone("jaw", "head", S([0, hy - 1.4, hz + 1]), [Cube(S([-1, hy - 1.9, hz - 0.4]), [n(2 * s), 1, n(1.6 * s)], jaw_m)])
    m.bone("snout", "head", S([0, hy - 0.4, hz]), [])
    m.pair("whiskers_right", "head", S([-1.2, hy - 0.6, hz]), [Cube(S([-6.6, hy - 0.6, hz - 1.2]), [n(5.4 * s), 0, n(2.4 * s)], whisker_mat("#F8F0F4"))])
    m.eyes("head", x_inner=1.0 * s, y=hy * s, z=hz * s, w=2, h=2, iris="#2A1A24", pupil="#0A0608", skin=FUR, iris_light="#6A4A5A", grid=["PW", "KP"])
    m.bone("curl", "head", S([0, hy + 2.6, hz + 1]), [Cube(S([-1, hy + 2.4, hz + 0.2]), [n(2 * s), n(2 * s), n(2 * s)], furm),
                                                     Cube(S([-0.6, hy + 3.6, hz - 0.8]), [n(1.6 * s), n(1.6 * s), n(1.6 * s)], furm, [-30, 0, 0], S([0, hy + 4, hz])),
                                                     Cube(S([-0.4, hy + 3.4, hz - 1.6]), [1, 1, 1], furm)])
    if not big:
        m.pair("ear_right", "head", S([-3, hy + 2.6, -1]), [Cube(S([-7.4, hy + 1.8, -1]), [n(7 * s), n(eh * s), 0], ear)], rotation=[-5, 15, -22])
    else:
        m.pair("ear_right", "head", S([-3, hy + 2.6, 0]), [Cube(S([-4.6, hy + 2.6 - eh, 0]), [n(3.6 * s), n(eh * s), 0], longear)],
               rotation=[30, 20, -18])
    m.pair("arm_right", "body", S([-3.6, 4.6, -3]), [Cube(S([-4.4, 2.8, -4.2]), [n(1.6 * s), n(2.2 * s), n(1.6 * s)], furm),
                                                    Cube(S([-4.2, 2.6, -4.6]), [n(1.4 * s), 1, n(1.2 * s)], paw)], rotation=[-30, 0, -20])
    m.pair("leg_right", "body", S([-2.4, 2, 0]), [Cube(S([-3.4, 0.8, -1]), [n(2 * s), n(1.6 * s), n(2 * s)], furm)])
    m.pair("foot_right", "leg_right", S([-2.4, 0.8, 0]), [Cube(S([-3.5, 0, -2.6 if big else -2]), [n(2.2 * s), 1, n((3.6 if big else 2.6) * s)], paw)] +
           [Cube(S([-3.4 + i * 0.8, 0, -2.6 - (0.8 if big else 0)]), [1, 1, 1], claw) for i in range(2)])
    # The bushy tail: a plume of long fur in three joints, curled up the back.
    pl = (5 if not big else 6) * s
    names, zt = tail_bones(m, "body", 3 * s, 3.8 * s, [(1.6 * s, 1.6 * s, 2 * s), (1.4 * s, 1.4 * s, 2 * s), (1.2 * s, 1.2 * s, 2 * s)], furm, (55, 40, 40))
    for i, nm in enumerate(names):
        b = bone_of(m, nm)
        y, z = b.pivot[1], b.pivot[2]
        w = n(pl * (0.8 + i * 0.25))
        b.cubes += [Cube([-w / 2, y, z - 0.5], [w, 0, n(2.6 * s)], plume), Cube([0, y - w / 2, z - 0.5], [0, w, n(2.6 * s)], plume)]
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
    dragon_wing(m, 2.4 * s, 11.6 * s, -2 * s, 6 * s, (3 * s, 4 * s, 1.5 * s, 7.5 * s), bonem, mems, thick=1 if not big else 1.5, claw=claw,
                under="#B89AD8" if not big else "#A85A78")
    legs_pair(m, "leg_right", "body", 1.2 * s, 7.8 * s, 1.5 * s, round(2 * s), bonem, claw)
    fx_for(m, 0.7 * s)
    moves(m, "LEECH LIFE" if not big else "WING ATTACK", "SUPERSONIC" if not big else "CONFUSE RAY")
    A, info = flyer(name.lower(), m, flap=0.36 if not big else 0.62, amp=32, rest=66 if not big else 70, k=0.7 * s, legs=("leg_right", "leg_left"))
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
    """A walking tuber: a round indigo root with a face, walking on two short root legs. The first wears a crown of five
    broad leaves; the next a drooping, half-open bud of dark petals dripping glowing nectar, its eyes sleepy, little
    leafy arms at its sides; the last a great five-petalled flower spread over its head, violet with orange veins and pale
    spots round a dark central pit, and leafy arms it holds out."""
    m = Model()
    s = (1.0, 1.35, 1.55)[stage]
    S = lambda v: [x * s for x in v]
    TB, TBL, TBD = "#5A6AC8", "#8A9AE8", "#2E3A8A"
    body = Mat(TB, fur(TB, TBL, TBD, speck=0.3), noise=0.02)
    jaw_m = Mat(TBD, open_mouth("#6A2A4A", TB), noise=0.02)
    rootm = Mat("#C8B890", lambda a: rgb("#8A7A5A") if a.face == "bottom" else (rgb("#A8987A") if a.row % 2 else None), noise=0.02)
    toe = Mat("#8A7A5A", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    hip = 3
    m.bone("body", "root", S([0, hip + 3, 0]), blob(S([-4, hip - 0.6, -4]), S([8, 7, 8]), body, b=2))
    m.bone("head", "body", S([0, hip + 6.5, 0]), [Cube(S([-2, hip + 6.2, -2]), [n(4 * s), n(1.5 * s), n(4 * s)], Mat("#3E8A3A", noise=0.02))])
    hd = bone_of(m, "head")
    ty = hip + 7.4
    if stage == 0:
        lf = leaf_mat("#7AD060", "#2E7A30", "#C8F0A0", "z")
        for a in (0, 72, 144, 216, 288):
            hd.cubes.append(Cube(S([-2.5, ty, 0]), [n(5 * s), 0, n(10 * s)], lf, [52, a, 0], S([0, ty, 0])))
    elif stage == 1:
        pm = petal_mat("#A8382A", "#E87A4A", "#6A1A14")
        for a in (0, 72, 144, 216, 288):
            hd.cubes.append(Cube(S([-2.5, ty, 0]), [n(5 * s), 0, n(7 * s)], pm, [68, a, 0], S([0, ty, 0])))
            hd.cubes.append(Cube(S([-2, ty + 0.2, 0]), [n(4 * s), 0, n(5 * s)], leaf_mat("#5AA848", "#2A6A2A", "#A8E080", "z"), [10, a + 36, 0], S([0, ty, 0])))
        drop = Mat("#E8FF8A", lambda a: CLEAR if (a.lx - 0.5) ** 2 + (a.ly - 0.45) ** 2 > 0.2 else None,
                   glow=lambda a: rgb("#E8FF8A") if (a.lx - 0.5) ** 2 + (a.ly - 0.45) ** 2 < 0.2 else None, noise=0, edge=0, flat=True)
        m.bone("drip", "head", S([0, ty + 6.2, 0]), [Cube(S([-0.8, ty + 5.8, 0]), [n(1.6 * s), n(2.2 * s), 0], drop),
                                                     Cube(S([0, ty + 5.8, -0.8]), [0, n(2.2 * s), n(1.6 * s)], drop)])
    else:
        pm = petal_mat("#8A3AC8", "#E8B8FF", "#F08A2A", spot="#F4E0FF")
        for a in (0, 72, 144, 216, 288):
            hd.cubes.append(Cube(S([-6, ty + 0.6, 1.5]), [n(12 * s), 0, n(13 * s)], pm, [-8, a, 0], S([0, ty + 0.6, 0])))
        hd.cubes += [Cube(S([-2.4, ty + 0.4, -2.4]), [n(4.8 * s), n(1.6 * s), n(4.8 * s)], Mat("#2A1A2E", noise=0.02)),
                     Cube(S([-1.6, ty + 1.8, -1.6]), [n(3.2 * s), 1, n(3.2 * s)], Mat("#F0A040", noise=0.02, edge=0))]
    m.bone("jaw", "body", S([0, hip + 1.2, -3.6]), [Cube(S([-1.2, hip + 0.6, -4.2]), [n(2.4 * s), 1, n(1.4 * s)], jaw_m)])
    m.bone("face", "body", S([0, hip + 3, -4]), [])
    # Eyes: tall glossy ovals, set well apart, a catch-light in each.
    m.eyes("body", x_inner=1.0 * s, y=(hip + 2.0) * s, z=-4 * s, w=2, h=3, iris="#E04A4A" if stage < 2 else "#E8A030", pupil="#1A0606", skin=TB,
           iris_light="#FF9A8A", grid=["PW", "PP", "KI"] if stage == 0 else (["LL", "PW", "KI"] if stage == 1 else ["LL", "PW", "II"]))
    if stage:
        lf2 = leaf_mat("#6AC050", "#2A6A2A", "#B8E890", base_end=1)
        m.pair("arm_right", "body", S([-3.8, hip + 3.6, -0.5]), [Cube(S([-4.8, hip + 1.6, -1]), [n(1.4 * s), n(2.4 * s), n(1.4 * s)], body)],
               rotation=[0, 0, 24])
        m.pair("forearm_right", "arm_right", S([-4.2, hip + 1.8, -0.5]), [Cube(S([-6, hip - 2.4, -0.5]), [n(3.2 * s), n(4.4 * s), 0], lf2)], rotation=[-10, 0, 10])
    upright_legs(m, 2.0 * s, hip * s, 0.2 * s, n(2 * s), rootm, rootm, n(3 * s), toe_m=toe, ntoes=3, thigh_w=n(2.4 * s))
    fx_for(m, 0.8 * s)
    moves(m, ("ABSORB", "ACID", "ACID")[stage], ("POISONPOWDER", "SLEEP POWDER", "PETAL DANCE")[stage])
    A, info = biped(name.lower(), m, walk=0.5 + 0.08 * stage, stride=34, k=0.6 * s, guard=10 if stage else 0,
                    extras={"idle": {"head": track(rotation=[wave(2.4, 4), wave(3.2, 6), wave(2.4, 4, 90)])}})
    write(sid, name, m, A, info, stands)


def tuberlet():
    tuber("oddish", "TUBERLET", 0.8, 0)


def droopbud():
    tuber("gloom", "DROOPBUD", 1.25, 1)


def rafflora():
    tuber("vileplume", "RAFFLORA", 1.6, 2)


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
    m.eyes("head", x_inner=0.6 * s, y=3.4 * s, z=-4.4 * s, w=2, h=2, iris="#F4F0E8" if not big else "#E8F4E0", pupil="#C8C0B8" if not big else "#A8C8A0",
           skin=CR, grid=["WI", "II"])
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
        def bracket_p(a):
            # A shelf of bracket fungus: velvety growth bands round from its root (dark) to its pale rim, pores beneath.
            if a.face == "bottom":
                return rgb("#E8D8B8") if hash01("pore", round(a.x * 2), round(a.z * 2)) > 0.18 else rgb("#B8A07A")
            c = a.cube
            if a.face != "top" and (c.origin[1] + c.size[1]) - a.y < 0.5:
                return rgb("#F8E8C0")
            r = math.hypot(a.x / s, (a.z / s) - 1) if a.face == "top" else (c.origin[1] + c.size[1] - a.y) / s * 2
            band = ramp(["#5A3A14", "#8A5A1E", "#C8903A", "#E0B060", "#B87A2A", "#F0D8A0"], (r * 0.22) % 1.0)
            return mix(band, "#000000", 0.12 * hash01("vel", round(a.x * 2), round(a.y * 2), round(a.z * 2)))
        bracket = Mat("#C8903A", bracket_p, noise=0.02)
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
    """A star nose on a flat part facing forward: twenty-two pink fleshy rays round a small middle."""
    def inside(at):
        x, y = at.lx - 0.5, at.ly - 0.5
        r, th = math.hypot(x, y) * 2, math.atan2(y, x)
        R = 0.42 + 0.56 * abs(math.cos(th * 5.5)) ** 1.5
        return r < R
    return Mat("#F07A9A", lambda a: (rgb("#FFD0DC") if math.hypot(a.lx - 0.5, a.ly - 0.5) < 0.16 else
                                     (rgb("#C84A6A") if math.hypot(a.lx - 0.5, a.ly - 0.5) > 0.4 else None)) if inside(a) else CLEAR,
               noise=0.01, edge=0, flat=True)


def mole(m, suffix, x, z, s, velvet, face, nose, claw_m, paw, main):
    """A star-nosed mole poking up out of the mound: its velvet body, a round head, the big pink star of a nose on the
    end of a short snout, tiny eyes, and its great pink shovel paws turned out over the mound's rim, long claws on each."""
    b, h = "body" + suffix, "head" + suffix
    m.bone(b, "mound", [x, 3 * s, z], blob([x - 2.6 * s, 1.5 * s, z - 2.6 * s], [n(5.2 * s), n(6.5 * s), n(5.2 * s)], velvet, b=2))
    m.bone(h, b, [x, 7 * s, z - 1 * s], face_blob([x - 2.5 * s, 5.4 * s, z - 3.4 * s], [n(5 * s), n(3.6 * s), n(2.4 * s)], face) +
           [Cube([x - 1.2 * s, 5.8 * s, z - 4.6 * s], [n(2.4 * s), n(2 * s), n(1.4 * s)], face),
            Cube([x - 2.4 * s, 4.6 * s, z - 4.68 * s], [n(4.8 * s), n(4.8 * s), 0], nose)])
    if not main:
        bone_of(m, h).cubes += [Cube([x - 1.8 * s, 8.0 * s, z - 3.45 * s], [1, 1, 0], Mat("#0A0A0A", noise=0, edge=0)),
                                Cube([x + 0.8 * s, 8.0 * s, z - 3.45 * s], [1, 1, 0], Mat("#0A0A0A", noise=0, edge=0))]
    bb = bone_of(m, b)
    for sx in (-1, 1):
        px = x + sx * 3.0 * s
        bb.cubes += [Cube([px - 1.5 * s, 2.2 * s, z - 3.6 * s], [n(3 * s), n(1.2 * s), n(2.6 * s)], paw, [0, sx * -20, sx * 30], [px, 2.6 * s, z - 2.4 * s])]
        bb.cubes += [Cube([px - 1.3 * s + i * 0.9 * s, 2.0 * s, z - 5.6 * s], [0, n(1.6 * s), n(2.2 * s)], claw_m, [0, sx * -20, 0], [px, 2.6 * s, z - 2.4 * s])
                     for i in range(3)]


def burrow(sid, name, stands, trio):
    """A star-nosed mole poking up out of a lumpy mound of earth: dark brown velvet fur with a soft sheen, a big pink star
    of a nose, tiny eyes, broad pink shovel paws with long pale claws turned out over the mound's rim, pebbles and tufts
    of grass in the soil. Three of them together in one big mound when grown."""
    m = Model()
    s = 1.0 if not trio else 1.15
    V, V_L, V_D = "#4A3E3A", "#7A6A62", "#241C1A"
    velvet = Mat(V, fur(V, V_L, V_D, speck=0.25), noise=0.02)
    face = Mat(V, fur(V, V_L, V_D, speck=0.1), noise=0.02)
    soil = Mat("#8A6238", lambda a: rgb("#A88458") if a.face == "top" and hash01("dt", round(a.x * 2), round(a.z * 2)) < 0.25 else
               (rgb("#5A3E22") if hash01("dp", round(a.x * 2), round(a.y * 2), round(a.z * 2)) < 0.14 else
                (rgb("#B8B0A0") if hash01("pb", round(a.x), round(a.y), round(a.z)) < 0.04 else None)), noise=0.04)
    grass = leaf_mat("#7AC050", "#3A7A28", "#B8E890")
    paw = Mat("#F2A0B4", lambda a: rgb("#E07A94") if a.face == "bottom" else None, noise=0.02, edge=0)
    claw_m = sharp_claw_mat("#E8E0D0", "#FFFFFF")
    m.bone("root", None, [0, 0, 0])
    w = 12 if not trio else 19
    mound = blob([-w / 2, 0, -w / 2 + 1], [w, 2.5, w - 2], soil, b=2) + blob([-w / 2 + 1.5, 2, -w / 2 + 2.5], [w - 3, 1.6, w - 5], soil) + \
        [Cube([-w / 2 + 2.5, 3.4, -w / 2 + 3.5], [w - 5, 0.8, w - 7], soil)]
    for i in range(5 if not trio else 8):
        a = i * 2.4 + 0.6
        gx, gz = math.cos(a) * (w / 2 - 1.2), math.sin(a) * (w / 2 - 2) + 1
        mound += [Cube([gx - 1, 1.5, gz], [2, 3, 0], grass), Cube([gx, 1.5, gz - 1], [0, 3, 2], grass)]
    m.bone("mound", "root", [0, 0, 0], mound)
    if not trio:
        mole(m, "", 0, 0, s, velvet, face, star_nose(), claw_m, paw, True)
        bodies = ("body",)
    else:
        mole(m, "", 0, -1.5, s, velvet, face, star_nose(), claw_m, paw, True)
        mole(m, "2", -5.4, 2.0, s * 0.88, velvet, face, star_nose(), claw_m, paw, False)
        mole(m, "3", 5.4, 2.0, s * 0.88, velvet, face, star_nose(), claw_m, paw, False)
        bodies = ("body", "body2", "body3")
    m.bone("jaw", "head", [0, 5.8 * s, -3.4 * s], [Cube([-1 * s, 5.0 * s, -4.4 * s], [n(2 * s), 1, 1], Mat("#2A2020", noise=0.01))])
    m.eyes("head", x_inner=1.0 * s, y=8.0 * s, z=(-3.4 - (1.5 if trio else 0)) * s, w=1, h=1, iris="#0A0A0A", pupil="#000000", skin=V,
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


# ======================================================================================================
# The flytraps: SNAPSPROUT > SNAPBULB > GULPTRAP (in place of Bellsprout, Weepinbell, Victreebel)
# ======================================================================================================

def bristle_mat(base, tip):
    """Stiff bristles along a trap's rim on a flat part lying along z (their roots at the back): thin spines side by side,
    running pale towards their points."""
    B, T = rgb(base), rgb(tip)
    return Mat(base, lambda a: CLEAR if a.col % 2 else mix(B, T, 1 - a.lz), noise=0, edge=0, flat=True)


def spotted(base, light, dark, spot, inner=None, inner_face="bottom", seed="sp", rate=0.06):
    """A plant's skin: lighter on top, flecked with poison spots; (inner) the trap's red lining on one face."""
    F = fur(base, light, dark, speck=0.2)

    def f(at):
        if inner is not None and at.face == inner_face:
            return mix(inner, "#FFFFFF", 0.15 * hash01("in", at.col, at.row))
        if hash01(seed, round(at.x), round(at.y), round(at.z)) < rate and at.face != "bottom":
            return rgb(spot)
        return F(at)
    return f


def trap_lobes(m, parent, w, y, z_front, depth, h_up, h_low, lid, mouth, bristle, b_len, hinge_z, open_deg=16, side_bristles=True, jaw_b=1):
    """A flytrap's two lobes hinged at the back: the lid (the head) and the lower lobe (the jaw), each with a row of
    bristles along its rim, red inside."""
    m.bone("head", parent, [0, y, hinge_z], blob([-w / 2, y, z_front], [w, h_up, depth], lid) +
           [Cube([-w / 2, y + 0.3, z_front - b_len], [n(w), 0, n(b_len)], bristle)] +
           ([Cube([-w / 2 - b_len, y + 0.3, z_front + 1], [n(b_len), 0, n(depth - 2)], bristle, [0, 0, -12], [-w / 2, y + 0.3, 0]),
             Cube([w / 2, y + 0.3, z_front + 1], [n(b_len), 0, n(depth - 2)], bristle, [0, 0, 12], [w / 2, y + 0.3, 0])] if side_bristles else []),
           rotation=[-open_deg, 0, 0])
    m.bone("jaw", parent, [0, y, hinge_z], blob([-w / 2 + 0.5, y - h_low, z_front + 0.3], [w - 1, h_low, depth - 0.3], mouth, b=jaw_b) +
           [Cube([-w / 2 + 0.5, y - 0.3, z_front + 0.3 - b_len], [n(w - 1), 0, n(b_len)], bristle)], rotation=[open_deg * 0.4, 0, 0])


def leaf_arm(m, x, y, z, stalk, leaf_l, leaf_w, stalk_m, leaf, rest=(0, 0, 50), blade_rest=(0, 0, 20)):
    """A leaf on a stalk for an arm: the stalk (from the shoulder) and the leaf blade (bending at its base)."""
    m.pair("arm_right", "body", [-x, y, z], [Cube([-x - stalk, y - 0.5, z - 0.5], [n(stalk), 1, 1], stalk_m)], rotation=list(rest))
    m.pair("forearm_right", "arm_right", [-x - stalk, y, z], [Cube([-x - stalk - leaf_w / 2, y - leaf_l, z], [n(leaf_w), n(leaf_l), 0], leaf, [0, 0, 90], [-x - stalk, y, z])],
           rotation=list(blade_rest))


def flytrap(sid, name, stands, stage):
    """A walking flytrap. The sprout is a small two-lobed trap fringed with bristles on a thin stalk, two leaves for
    arms, walking on its roots, button eyes on its lid. The second is a fat spotted bulb that opens as a great trap,
    a leaf at each side and a vine curling from its back; it hops. The last is a huge gaping trap with long bristles,
    big leaves, a long jointed vine ending in a leaf, sitting on a knot of roots."""
    m = Model()
    G, G_L, G_D, RED, SPOT, ROOT = "#7AB83A", "#A8D860", "#4A7A24", "#E0485A", "#8A4AA8", "#8A6A3A"
    if stage == 2:
        G, G_L, G_D = "#9AC040", "#C8E070", "#5A7A20"
    lid = Mat(G, spotted(G, G_L, G_D, SPOT, RED, "bottom"), noise=0.02)
    mouth = Mat(G, spotted(G, G_L, G_D, SPOT, RED, "top", seed="sp2"), noise=0.02)
    stalk = Mat(G_D, fur(G_D, G, mix(G_D, "#000000", 0.2), speck=0.1), noise=0.02)
    bristle = bristle_mat(G_L, "#FFF4C0")
    leaf = leaf_mat(G_L, G_D, "#E8F4B0")
    rootm = Mat(ROOT, fur(ROOT, "#A88A5A", "#5A4426", speck=0.2), noise=0.02)
    m.bone("root", None, [0, 0, 0])
    if stage == 0:
        m.bone("body", "root", [0, 4, 0], [Cube([-1, 3, -1], [2, 8, 2], stalk), Cube([-1.5, 2.6, -1.5], [3, 1, 3], rootm)])
        trap_lobes(m, "body", 5, 12, -5, 6, 2.5, 2, lid, mouth, bristle, 1.5, 0.5, open_deg=18, side_bristles=False)
        m.eyes("head", x_inner=0.5, y=12.9, z=-5, w=1, h=1, iris="#141018", pupil="#141018", skin=G, grid=["P"])
        leaf_arm(m, 1, 7.5, -0.2, 1.5, 5, 3, stalk, leaf_mat(G_L, G_D, "#E8F4B0", base_end=1), rest=(0, 0, 30), blade_rest=(0, 0, 30))
        upright_legs(m, 1.0, 3.2, 0, 1, rootm, rootm, 3, ntoes=2, toe_m=Mat("#5A4426", noise=0, edge=0), thigh_w=1.4)
        moves(m, "VINE WHIP", "SLEEP POWDER")
        A, info = biped(name.lower(), m, walk=0.45, stride=30, k=0.6, guard=0,
                        extras={"idle": {"head": track(rotation=[wave(2, 5, 0, -2), wave(4, 10), 0]), "jaw": track(rotation=[wave(2, 4, 180), 0, 0])}})
        write(sid, name, m, A, info, stands)
        return
    big = stage == 2
    if not big:
        m.bone("body", "root", [0, 6, 0], [Cube([-2, 0, -2], [4, 1.5, 4], rootm)])
        trap_lobes(m, "body", 10, 9, -5, 10, 3, 7.5, lid, mouth, bristle, 2, 4.5, open_deg=20, jaw_b=2)
        m.eyes("head", x_inner=1.2, y=9.8, z=-5, w=2, h=2, iris="#E8C030", pupil="#141008", skin=G, iris_light="#FFE880", grid=["IW", "PP"])
        leaf_arm(m, 4.8, 5, 0, 1.5, 6, 4, stalk, leaf_mat(G_L, G_D, "#E8F4B0", base_end=1), rest=(0, 0, 30), blade_rest=(0, 0, 20))
        names, zt = tail_bones(m, "head", 11, 3.5, [(1, 1, 2.5)] * 4, stalk, (40, 30, 30, 30))
        tip = bone_of(m, names[-1])
        tip.cubes.append(Cube([-1.5, tip.pivot[1], zt - 0.5], [3, 0, 4], leaf_mat(G_L, G_D, "#E8F4B0", along="z")))
        moves(m, "WRAP", "STUN SPORE")
        k, hop, hh = 0.8, 0.55, 2.0
    else:
        m.bone("body", "root", [0, 8, 0], [Cube([-3, 0, -3], [6, 3, 6], rootm), Cube([-2, 2.5, -2], [4, 6, 4], stalk)] +
               [Cube([-1, 0, -1], [2, 1, 5], rootm, [0, a, 0], [0, 0, 0]) for a in (-150, -90, -30, 30, 90, 150)])
        trap_lobes(m, "body", 14, 14, -9, 14, 4, 5.5, lid, mouth, bristle, 3.5, 5, open_deg=30, jaw_b=2)
        m.eyes("head", x_inner=1.5, y=15, z=-9, w=3, h=2, iris="#E8A020", pupil="#141008", skin=G, iris_light="#FFE070", grid=["LLL", "IPW"])
        leaf_arm(m, 2, 6.5, 0, 2.5, 10, 6, stalk, leaf_mat(G_L, G_D, "#E8F4B0", base_end=1), rest=(0, 0, 40), blade_rest=(0, 0, 12))
        names, zt = tail_bones(m, "head", 17, 4.5, [(1.5, 1.5, 3)] * 5, stalk, (60, 30, 25, 25, 30))
        tip = bone_of(m, names[-1])
        tip.cubes.append(Cube([-2.5, tip.pivot[1], zt - 0.5], [5, 0, 7], leaf_mat(G_L, G_D, "#E8F4B0", along="z")))
        moves(m, "RAZOR LEAF", "SLEEP POWDER")
        k, hop, hh = 1.1, 0.7, 1.6
    fx_for(m, k)
    A, info = hopper(name.lower(), m, hop=hop, height=hh, k=k, extras={"idle": {"head": track(rotation=[wave(2.4, 5, 0, -3), 0, 0])},
                                                                          "cry": {"head": track(rotation=keys({0: [0, 0, 0], 0.2: [-30, 0, 0], 0.7: [-30, 0, 0], 1.0: [0, 0, 0]}))}})
    write(sid, name, m, A, info, stands)


def snapsprout():
    flytrap("bellsprout", "SNAPSPROUT", 0.8, 0)


def snapbulb():
    flytrap("weepinbell", "SNAPBULB", 1.1, 1)


def gulptrap():
    flytrap("victreebel", "GULPTRAP", 1.7, 2)


# ======================================================================================================
# The sail jellies: DRIFTSAIL > STORMSAIL (in place of Tentacool, Tentacruel)
# ======================================================================================================

def crest_mat(base, light, rib):
    """A floating jelly's crest on a flat part standing along its back: rising in a ruffled arc, pale ribs up it, its top
    edge rippling."""
    B, Lt, R = rgb(base), rgb(light), rgb(rib)

    def f(at):
        u, v = at.lz, at.ly
        top = math.sin(math.pi * u) ** 0.6 * (0.82 + 0.18 * math.cos(u * math.pi * 7))
        if v > top:
            return CLEAR
        col = mix(B, Lt, v / max(0.05, top))
        if abs(((u * 6) % 1.0) - 0.5) < 0.1:
            col = mix(col, R, 0.5)
        return col
    return Mat(base, f, noise=0.01, edge=0, flat=True)


def dangle(m, parent, x, y, z, segs, mat, rests, prefix, tip=None):
    """A tentacle hanging in joints: segs [(w, length)], each running down from the one before; rests: each joint's
    resting tilt. Its joints are named prefix_right, prefix + 'b'_right... (and mirrored on the left). Gives the names."""
    letters = "abcdefgh"
    prev, names = parent, []
    for i, ((w, L), r) in enumerate(zip(segs, rests)):
        nm = f"{prefix}{letters[i]}_right"
        cubes = [Cube([-x - w / 2, y - L - 0.2, z - w / 2], [n(w), n(L + 0.3), n(w)], mat)]
        if tip is not None and i == len(segs) - 1:
            cubes.append(Cube([-x - w / 2, y - L - 0.6, z - w / 2], [n(w), 1, n(w)], tip))
        m.pair(nm, prev, [-x, y, z], cubes, rotation=[r, 0, 0])
        prev, y = nm, y - L
        names.append(nm)
    return names


def man_o_war(sid, name, stands, big):
    """A floating sail jelly. The first is a small glossy blue float with a pink ruffled crest along its back, button
    eyes, glowing spots on its sides, and long tentacles in joints trailing below. Grown, a great storm-blue float
    with a tall ribbed crest, a scowl, rows of glowing spots, a beard of short stinging polyps, and eight jointed
    tentacles, the front pair long and heavy."""
    m = Model()
    s = 1.0 if not big else 1.5
    S = lambda v: [x * s for x in v]
    if not big:
        F, F_L, F_D, CREST, CREST_L, TENT, TIP = "#7A9CF0", "#BCD0FF", "#4A5CB8", "#E070C0", "#FFC0E8", "#4A5CB8", "#C8D8FF"
    else:
        F, F_L, F_D, CREST, CREST_L, TENT, TIP = "#4A6CD0", "#90B0FF", "#283A8A", "#C04AA8", "#FF9AD8", "#2A3A8A", "#A8C0FF"
    GLOW = "#7AF8FF"
    floatm = Mat(F, lambda a: rgb(F_D) if a.face == "bottom" else (rgb(mix(F_L, "#FFFFFF", 0.3)) if a.face == "top" and hash01("gl", a.col, a.row) < 0.1 else
                                                                   fur(F, F_L, F_D, speck=0.1)(a)), noise=0.015)
    tent = Mat(TENT, lambda a: rgb(mix(TENT, TIP, 0.5)) if a.row % 3 == 0 else None, noise=0.02)
    tipm = Mat(TIP, noise=0.0, edge=0)
    m.bone("root", None, [0, 0, 0])
    fy = 13 if not big else 15
    m.bone("body", "root", S([0, fy, 0]), blob(S([-3.5, fy - 2, -5]), S([7, 5, 10]), floatm) +
           [Cube(S([0, fy + 2.4, -4]), [0, n(4.5 * s), n(8 * s)], crest_mat(CREST, CREST_L, "#FFFFFF"))])
    if big:
        bone_of(m, "body").cubes += blob(S([-4, fy - 2.5, -3]), S([8, 4, 7]), floatm)
    m.bone("head", "body", S([0, fy, -5]), [Cube(S([-3, fy - 1.5, -5.4]), [n(6 * s), n(3.5 * s), 1], floatm)])
    m.eyes("head", x_inner=0.8 * s, y=(fy - 0.6) * s, z=-5.4 * s, w=2, h=2, iris="#141018" if not big else "#E03A4A", pupil="#141018",
           skin=F, iris_light="#FF8A8A", grid=["PW", "PP"] if not big else ["IW", "PI"])
    if big:
        m.brows("head", [Cube(S([-3, fy + 1.4, -5.8]), [n(2.4 * s), 1, 1], Mat(F_D, noise=0, edge=0), [0, 0, -20], S([-0.8, fy + 1.8, -5.6]))])
    spots = glow_patch(GLOW, "diamond")
    body = bone_of(m, "body")
    for i in range(2 if not big else 4):
        zz = (-2.5 + i * (4 if not big else 2.2)) * s
        body.cubes += [Cube([-3.52 * s, (fy - 0.6) * s, zz], [0, n(1.4 * s), n(1.4 * s)], spots), Cube([3.52 * s, (fy - 0.6) * s, zz], [0, n(1.4 * s), n(1.4 * s)], spots)]
    under = (fy - 2) * s
    tents = []
    if not big:
        tents.append(dangle(m, "body", 1.4, under, -2.5, [(1, 2)] * 5, tent, (8, 6, 6, 8, 10), "tlong", tipm))
        tents.append(dangle(m, "body", 2.2, under, 1, [(1, 1.8)] * 3, tent, (14, 8, 8), "tshort", tipm))
        tents.append(dangle(m, "body", 0.8, under, 3.5, [(1, 1.6)] * 3, tent, (20, 10, 10), "tback", tipm))
    else:
        tents.append(dangle(m, "body", 2 * s, under, -3 * s, [(2, 2.6)] + [(1.5, 2.6)] * 5, tent, (6, 6, 6, 8, 8, 10), "tlong", tipm))
        for j, (tx, tz) in enumerate(((3, -0.5), (2.4, 2.5), (1, 4.6))):
            tents.append(dangle(m, "body", tx * s, under, tz * s, [(1.2, 2.4)] * 4, tent, (14 + j * 4, 8, 8, 10), "tm" + "xyz"[j], tipm))
        pol = Mat("#8AA0F0", lambda a: rgb("#C8D8FF") if a.ly < 0.3 else None, noise=0.02)
        body.cubes += [Cube([px * s - 0.5, under - 2, pz * s - 0.5], [1, 2, 1], pol) for px, pz in ((-1.2, -1), (1.2, -1), (0, 0.5), (-0.6, 1.8), (0.6, 1.8), (0, -2.2))]
    fx_for(m, 0.7 * s)
    moves(m, "POISON STING" if not big else "CONSTRICT", "BUBBLEBEAM" if not big else "HYDRO PUMP")
    all_t = [nm for ch in tents for nm in ch] + [nm.replace("right", "left") for ch in tents for nm in ch]
    chains = [ch for ch in tents] + [[nm.replace("right", "left") for nm in ch] for ch in tents]
    A, info = floater(name.lower(), m, tentacles=chains, k=0.8 * s, bob=0.8, pulse_bell=0.04)
    write(sid, name, m, A, info, stands)


def driftsail():
    man_o_war("tentacool", "DRIFTSAIL", 0.95, False)


def stormsail():
    man_o_war("tentacruel", "STORMSAIL", 2.0, True)


# ======================================================================================================
# The geodes: GEODINK > GEOBRUTE > GEOTITAN (in place of Geodude, Graveler, Golem)
# ======================================================================================================

def rock_mat(base, light, dark, crack=None, seed="rk"):
    """Rough stone: faceted patches of lighter and darker grey-brown, paler on top, (crack) dark cracks running across."""
    B, L, D = rgb(base), rgb(light), rgb(dark)

    def f(at):
        h = hash01(seed, math.floor(at.x / 2), math.floor(at.y / 2), math.floor(at.z / 2))
        c = mix(D, L, 0.25 + h * 0.6)
        if at.face == "top":
            c = mix(c, L, 0.3)
        if at.face == "bottom":
            c = mix(c, D, 0.4)
        if crack and hash01(seed + "c", round(at.x), round(at.y), round(at.z)) < 0.05:
            return rgb(crack)
        return c
    return Mat(base, f, noise=0.03)


def crystal_mat(base, tip):
    return taper_mat(base, tip, glow=None)


def crystals(bone, pts, mat, qw=1.4):
    """A cluster of crystals bursting out (pts: (x, y, z, length, rotation))."""
    for x, y, z, L, rot in pts:
        bone.cubes += spike(x, y, z, L, rot, mat, qw)


def geode(sid, name, stands, stage):
    """A living geode. The first is a round grey-brown stone that floats, a crack across its front glinting with violet
    crystals, a stony brow over small eyes, and two arms ending in rocky fists. The second is a big lumpy boulder on
    stubby legs, four arms, crystals bursting from the cracks along its back. The last is a great boulder with a
    shell's dome and a ridge of amethyst spines, a stony head jutting out the front with a heavy jaw, and thick
    clawed limbs."""
    m = Model()
    R, R_L, R_D, CRK = "#8A8070", "#B0A690", "#5A5246", "#2A2620"
    CRY, CRY_L = "#8A4AE8", "#F0E0FF"
    rock = rock_mat(R, R_L, R_D, CRK)
    skin = Mat("#9A8E7A", lambda a: rgb("#B8AC96") if a.face == "front" and a.ly > 0.5 else None, noise=0.025)
    cry = crystal_mat(CRY, CRY_L)
    jaw_m = Mat(R_D, open_mouth("#6A3A3A", "#3A3430"), noise=0.02)
    brow = Mat(R_D, noise=0.02, edge=0)
    m.bone("root", None, [0, 0, 0])
    if stage == 0:
        y0 = 4
        m.bone("body", "root", [0, y0 + 3.5, 0], blob([-4, y0, -3.5], [8, 7, 7], rock, b=2) + blob([-3.5, y0 + 0.5, -3], [7, 6.5, 6.5], rock))
        crystals(bone_of(m, "body"), [(-1.5, y0 + 6.6, -0.5, 3.5, [-20, 0, 15]), (0.8, y0 + 6.6, 0.5, 4.5, [-35, 0, -10]), (2.6, y0 + 5.5, 1.5, 3, [-50, 0, -40]),
                                      (-2.4, y0 + 5.2, 2.4, 3, [-70, 0, 40])], cry, 1.8)
        m.bone("head", "body", [0, y0 + 3, -3.5], [Cube([-3, y0 + 1, -3.9], [6, 4.5, 1], rock)])
        m.eyes("head", x_inner=0.5, y=y0 + 3, z=-3.9, w=2, h=1, iris="#20180E", pupil="#20180E", skin=R, grid=["PW"])
        m.brows("head", [Cube([-3, y0 + 4, -4.4], [3, 1, 1], brow, [0, 0, 12], [-0.4, y0 + 4.4, -4.2])])
        m.bone("jaw", "head", [0, y0 + 1.8, -3.9], [Cube([-1.5, y0 + 1.2, -4.2], [3, 1, 1], jaw_m)])
        crystals(bone_of(m, "head"), [(-2.4, y0 + 2.4, -3.9, 2.5, [180 + 20, 0, 0]), (2.2, y0 + 1.8, -3.9, 2.5, [180 + 10, 20, 0])], cry, 1.4)
        upright_arms(m, 4.2, y0 + 4.5, -0.5, 2.5, 2, 2, rock, rock, hw=3, hh=2.5, rest=(-25, 0, 35), fore_rest=(-45, 0, -15))
        moves(m, "TACKLE", "ROCK THROW")
        fx_for(m, 0.7)
        A, info = floater(name.lower(), m, k=0.7, bob=0.6)
        write(sid, name, m, A, info, stands)
        return
    big = stage == 2
    if not big:
        hip = 4
        m.bone("body", "root", [0, hip + 4, 0], blob([-5.5, hip - 1, -5], [11, 10, 10], rock, b=2) + blob([-4.5, hip + 7, -4], [9, 4, 8], rock) +
               blob([-6.5, hip + 2, -2], [3, 5, 4], rock) + blob([3.5, hip + 2, -2], [3, 5, 4], rock))
        crystals(bone_of(m, "body"), [(-2, hip + 10.5, 1, 3, [-30, 0, 20]), (1, hip + 10.5, 0, 4, [-15, 0, -10]), (3, hip + 9, 2.5, 3, [-50, 0, -40]),
                                      (-3.5, hip + 8, 3.5, 2.5, [-70, 0, 30]), (0, hip + 7, 4.8, 3, [-80, 0, 0])], cry, 1.6)
        m.bone("head", "body", [0, hip + 5, -5], [Cube([-3.5, hip + 2.5, -5.4], [7, 5, 1], rock)])
        m.eyes("head", x_inner=0.6, y=hip + 5, z=-5.4, w=2, h=1, iris="#E07A2A", pupil="#20180E", skin=R, grid=["IP"])
        m.brows("head", [Cube([-3.4, hip + 6, -5.9], [3, 1, 1], brow, [0, 0, 18], [-0.5, hip + 6.4, -5.7])])
        m.bone("jaw", "head", [0, hip + 3.6, -5.4], [Cube([-2, hip + 2.8, -5.8], [4, 1, 1.5], jaw_m)])
        upright_arms(m, 6, hip + 8.5, -1, 3.5, 3, 2.5, rock, rock, hw=3.5, hh=2.5, thick=3, fore_w=3, rest=(-10, 0, 20), fore_rest=(-30, 0, 0))
        upright_arms(m, 5.6, hip + 4.5, -1, 3, 2.5, 2, rock, rock, hw=3, hh=2.5, name="arm2", rest=(-20, 0, 30), fore_rest=(-50, 0, 0))
        upright_legs(m, 2.8, hip, 0, 3, rock, rock, 4, ntoes=3, toe_m=Mat(R_D, noise=0, edge=0), thigh_w=4)
        moves(m, "ROCK THROW", "MAGNITUDE")
        k, guard, s = 1.0, 40, 1.0
    else:
        hip = 6
        m.bone("body", "root", [0, hip + 5, 0], blob([-7, hip - 1, -6], [14, 13, 13], rock, b=2) + blob([-6, hip + 11, -5], [12, 3, 11], rock))
        crystals(bone_of(m, "body"), [(0, hip + 13.5, -3 + i * 2.6, 5 + (i % 2) * 2, [-20 - i * 12, 0, 0]) for i in range(5)] +
                 [(sx * 5.2, hip + 10.5, -2 + i * 3.5, 4, [-40 - i * 15, 0, sx * -45]) for sx in (-1, 1) for i in range(3)], cry, 3)
        bone_of(m, "body").cubes.append(Cube([-5, hip, -6.4], [10, 9, 1], skin))
        hy = hip + 8
        m.bone("head", "body", [0, hy, -6], face_blob([-3, hy - 2, -10], [6, 5, 5], skin) + [Cube([-3.4, hy + 1.4, -10.4], [6.8, 1.2, 1.4], brow)])
        crystals(bone_of(m, "head"), [(0, hy + 3, -8, 2.5, [-60, 0, 0])], cry, 1.2)
        m.bone("jaw", "head", [0, hy - 1.4, -7], [Cube([-2.8, hy - 3, -10.6], [5.6, 2, 4], Mat("#9A8E7A", open_mouth("#6A3A3A", "#7A6E5E"), noise=0.02))])
        m.eyes("head", x_inner=0.7, y=hy - 0.4, z=-10, w=2, h=2, iris="#E8401E", pupil="#180604", skin="#9A8E7A", iris_light="#FF9050", grid=["LL", "IP"])
        upright_arms(m, 8, hip + 10, -1, 4, 4, 4, skin, skin, hw=5, hh=2, thick=5, fore_w=4, claws=3, rest=(-14, 0, 24), fore_rest=(-30, 0, 0))
        upright_legs(m, 4, hip, 0, 4, skin, skin, 5, ntoes=3, sharp=True, thigh_w=5, toe_len=2)
        moves(m, "ROLLOUT", "EARTHQUAKE")
        k, guard, s = 1.3, 30, 1.0
    fx_for(m, k)
    A, info = biped(name.lower(), m, walk=0.7 if not big else 0.85, stride=20, k=k, slow=1.1 if not big else 1.25, guard=guard,
                    extras={"battle": {"arm2_right": track(rotation=[wave(0.6, 6, 180, -20), 0, 6]), "arm2_left": track(rotation=[wave(0.6, 6, 0, -20), 0, -6])},
                            "walk": {"arm2_right": track(rotation=[wave(0.8, 14), 0, 0]), "arm2_left": track(rotation=[wave(0.8, -14), 0, 0])}} if not big else None)
    write(sid, name, m, A, info, stands)


def geodink():
    geode("geodude", "GEODINK", 0.8, 0)


def geobrute():
    geode("graveler", "GEOBRUTE", 1.3, 1)


def geotitan():
    geode("golem", "GEOTITAN", 1.8, 2)


# ======================================================================================================
# The fire deer: EMBERFAWN > BLAZEHART (in place of Ponyta, Rapidash)
# ======================================================================================================

def fire_deer(sid, name, stands, big):
    """A deer of fire. The fawn is warm tan with a dappling of pale spots, big ears, long slender jointed legs on cloven
    hooves, two glowing ember nubs where its antlers will grow, a flame mane down its neck and a little flame of a
    tail. Grown, a tall pale-gold stag with branching antlers tipped with flames, a blazing mane, a flame tail, and
    flames licking round its hooves."""
    m = Model()
    s = 1.0 if not big else 1.4
    S = lambda v: [x * s for x in v]
    if not big:
        FUR, FUR_L, FUR_D, BELLY, SPOT = "#C88A58", "#E8B482", "#8A5630", "#FFF0DC", "#FFF4E6"
    else:
        FUR, FUR_L, FUR_D, BELLY, SPOT = "#E4C48A", "#FFF0C6", "#A8844C", "#FFF8EA", None
    top = 7 if not big else 9
    base = fur(FUR, FUR_L, FUR_D, belly=BELLY, below=(top + 1.4) * s)

    def coat(at):
        if SPOT and at.face in ("top", "nx", "px") and at.y > (top + 2.6) * s and hash01("dap", round(at.x / 1.2), round(at.y / 1.2), round(at.z / 1.2)) < 0.2:
            return rgb(SPOT)
        return base(at)
    furm = Mat(FUR, coat, noise=0.02)
    face = Mat(FUR, lambda a: rgb(BELLY) if a.face == "bottom" or (a.face == "front" and a.ly < 0.4) else None, noise=0.02)
    muzzle = Mat(FUR_D, lambda a: rgb("#1A1210") if a.face == "front" and a.ly > 0.45 else None, noise=0.02)
    jaw_m = Mat(BELLY, open_mouth("#A83A3A", "#E8D0B0"), noise=0.02)
    ear = ear_mat(FUR, "#F8D8C8", "rabbit", rim=None)
    legm = Mat(FUR, lambda a: rgb(mix(FUR, FUR_D, 0.35)) if a.ly < 0.4 else None, noise=0.02)
    hoof = Mat("#2A1E18", lambda a: rgb("#4A3A30") if a.face == "top" else None, noise=0.01)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, top + 2.5, 0]), blob(S([-2.6, top, -5]), S([5.2, 5, 10]), furm))
    hy = top + 5
    m.bone("neck", "body", S([0, hy - 1, -4]), blob(S([-1.6, hy - 2, -6]), S([3.2, 6, 3]), furm), rotation=[-28, 0, 0])
    m.bone("head", "neck", S([0, hy + 3.5, -5.5]), face_blob(S([-2.2, hy + 2.5, -9]), S([4.4, 3.6, 4]), face) +
           [Cube(S([-1.3, hy + 2.5, -11.4]), [n(2.6 * s), n(2.4 * s), n(2.6 * s)], muzzle)], rotation=[28, 0, 0])
    m.bone("jaw", "head", S([0, hy + 2.8, -9]), [Cube(S([-1.1, hy + 2, -11.2]), [n(2.2 * s), 1, n(2.6 * s)], jaw_m)])
    m.eyes("head", x_inner=1.0 * s, y=(hy + 4) * s, z=-9 * s, w=2, h=2, iris="#3A1A0A", pupil="#100804", skin=FUR, iris_light="#8A4A20",
           grid=["PW", "PP"] if not big else ["LL", "IP"], corner=None)
    m.pair("ear_right", "head", S([-2, hy + 5.6, -6.8]), [Cube(S([-5.6, hy + 4.6, -6.8]), [n(3.6 * s), n(2.2 * s), 0], ear_mat(FUR, "#F8D8C8", "round"))],
           rotation=[0, 20, 20])
    head = bone_of(m, "head")
    if not big:
        head.cubes += [Cube(S([-1.6, hy + 6, -7.6]), [1, n(1.4), 1], Mat("#FF9A3A", glow=lambda a: rgb("#FFB050"), noise=0, edge=0)),
                       Cube(S([0.6, hy + 6, -7.6]), [1, n(1.4), 1], Mat("#FF9A3A", glow=lambda a: rgb("#FFB050"), noise=0, edge=0))]
    else:
        ant = Mat("#E8D8B0", lambda a: rgb("#B8A07A") if a.ly < 0.3 else None, noise=0.02)
        for sx in (-1, 1):
            bx, by_, bz = sx * 1.2 * s, (hy + 6) * s, -7.4 * s
            beam = [(0, 4, 0, [-15, 0, sx * -20]), ]
            head.cubes.append(Cube([bx - 0.5, by_ - 0.2, bz - 0.5], [1, n(5 * s), 1], ant, [-15, 0, sx * -22], [bx, by_, bz]))
            tx, ty, tz = bx + sx * 1.9 * s, by_ + 4.6 * s, bz + 1.3 * s
            head.cubes.append(Cube([tx - 0.5, ty - 0.2, tz - 0.5], [1, n(4 * s), 1], ant, [-35, 0, sx * -12], [tx, ty, tz]))
            head.cubes.append(Cube([tx - 0.5, ty - 0.2, tz - 0.5], [1, n(3.4 * s), 1], ant, [30, 0, sx * -30], [tx, ty, tz]))
            mx, my, mz = bx + sx * 0.9 * s, by_ + 2.2 * s, bz + 0.6 * s
            head.cubes.append(Cube([mx - 0.5, my - 0.2, mz - 0.5], [1, n(3 * s), 1], ant, [-70, 0, sx * -10], [mx, my, mz]))
            for fx_, fy_, fz_ in ((tx + sx * 0.6 * s, ty + 3.6 * s, tz + 1.2 * s), (tx + sx * 1.4 * s, ty + 2.6 * s, tz - 1.6 * s), (mx, my + 1 * s, mz - 2.8 * s)):
                head.cubes += flame([fx_, fy_, fz_], n(1.6 * s), n(2.6 * s))
    neck = bone_of(m, "neck")
    nf = 3 if not big else 5
    for i in range(nf):
        t = i / max(1, nf - 1)
        neck.cubes += flame([0, (hy - 1 + t * 5) * s, (-4.2 - t * 1.2) * s], n((2.2 if not big else 2.6) * s), n((3 + (1 - t) * 1.5) * s))
    stout_legs(m, 1.6 * s, (-3.6 * s, 3.6 * s), round(top * s), max(2, round(1.6 * s)), legm, hoof, 2, foot=hoof)
    tl = 2 if not big else 3
    names, zt = tail_bones(m, "body", (top + 4.4) * s, 4.8 * s, [(1.6 * s, 1.6 * s, 1.6 * s)] * tl, furm, (30, 10, 10)[:tl])
    tip = bone_of(m, names[-1])
    tip.cubes += flame([0, tip.pivot[1] - 0.4 * s, zt - 0.6 * s], n((2.4 if not big else 3) * s), n((3.4 if not big else 5) * s))
    if big:
        for b in m.bones:
            if b.name.startswith("shin_"):
                x = b.pivot[0]
                b.cubes += flame([x, 1.6 * s, b.pivot[2]], n(2 * s), n(2.4 * s))
    fx_for(m, 0.8 * s)
    moves(m, "STOMP" if not big else "FURY ATTACK", "EMBER" if not big else "FIRE SPIN")
    A, info = four_legs(name.lower(), m, walk=0.5 if not big else 0.6, stride=36, k=0.7 * s)
    write(sid, name, m, A, info, stands)


def emberfawn():
    fire_deer("ponyta", "EMBERFAWN", 1.2, False)


def blazehart():
    fire_deer("rapidash", "BLAZEHART", 2.0, True)


# ======================================================================================================
# The salamanders: SLOWMANDER > SHELLMANDER (in place of Slowpoke, Slowbro)
# ======================================================================================================

def conch_mat(base, band, knob):
    """A conch snail's shell: cream, a violet band spiralling round it, darker knobs."""
    B, Bd, K = rgb(base), rgb(band), rgb(knob)

    def f(at):
        a = math.atan2(at.x, at.z - at.cube.origin[2] - at.cube.size[2] / 2)
        if ((at.y * 0.9 + a * 1.2) % 3.0) < 0.9:
            return Bd
        if hash01("kn", round(at.x), round(at.y), round(at.z)) < 0.06:
            return K
        return mix(B, "#FFFFFF", 0.2 if at.face == "top" else 0.0)
    return Mat(base, f, noise=0.02)


def salamander(sid, name, stands, big):
    """A dozy salamander. The first is long, low and pink, a broad flat head with a wide easy smile, small heavy-lidded
    eyes, frilled folds of skin along its sides, short legs with round toes, and a long thick tail in joints with a pale
    tip it likes to dangle in the water. Grown, it stands up on its hind legs, and a conch snail has clamped itself on
    the end of its tail, its spiralled shell knobbed and banded violet."""
    m = Model()
    P, P_L, P_D, BELLY, FRILL = "#EE9AAE", "#FFC4D0", "#C06A80", "#FFF0E2", "#D8809A"
    skin = Mat(P, fur(P, P_L, P_D, belly=BELLY, below=None, speck=0.15), noise=0.015)
    bellym = Mat(BELLY, noise=0.02)
    face = Mat(P, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.015)
    jaw_m = Mat(BELLY, open_mouth("#C04A5A", "#F0D0D0"), noise=0.02)
    toe = Mat(P_L, noise=0, edge=0)
    frill = Mat(FRILL, lambda a: CLEAR if a.ly < 0.35 * (0.5 + 0.5 * math.cos(a.lz * math.pi * 6)) else mix(FRILL, P_L, a.ly * 0.4), noise=0.01, edge=0, flat=True)
    tipm = Mat(BELLY, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    if not big:
        top = 3
        m.bone("body", "root", [0, top + 2, 0], blob([-3.5, top, -6], [7, 4, 12], skin) + [Cube([-3, top - 0.1, -5], [6, 1, 10], bellym)] +
               [Cube([-3.6, top + 0.5, -5], [0, 2, 10], frill, [0, 0, -25], [-3.5, top + 2.4, 0]), Cube([3.6, top + 0.5, -5], [0, 2, 10], frill, [0, 0, 25], [3.5, top + 2.4, 0])])
        hy = top + 1
        m.bone("head", "body", [0, hy + 1.5, -6], face_blob([-4.5, hy, -11], [9, 4, 5.5], face))
        m.bone("jaw", "head", [0, hy + 0.6, -7], [Cube([-4, hy - 0.4, -11.2], [8, 1, 4.5], jaw_m)])
        m.eyes("head", x_inner=1.6, y=hy + 2.2, z=-11, w=2, h=2, iris="#3A2024", pupil="#120808", skin=P, grid=["DD", "PW"])
        stout_legs(m, 2.6, (-3.5, 3.5), top, 2, skin, toe, 4, claw_len=1)
        segs = [((3.6 - i * 0.5), (3.2 - i * 0.4), 3) for i in range(5)]
        names, zt = tail_bones(m, "body", top + 2, 5.5, segs, skin, (-10, -4, 2, 4, 6))
        tip = bone_of(m, names[-1])
        tip.cubes.append(Cube([-0.8, tip.pivot[1] - 0.8, zt - 0.5], [2, 2, 1.5], tipm))
        moves(m, "WATER GUN", "CONFUSION")
        fx_for(m, 0.8)
        A, info = four_legs(name.lower(), m, walk=0.9, stride=26, k=0.7, slow=1.4)
        write(sid, name, m, A, info, stands)
        return
    m.bone("body", "root", [0, 7, 0], blob([-4.5, 5, -3.5], [9, 11, 7], skin) + [Cube([-3.5, 6, -3.9], [7, 8, 1], bellym)] +
           [Cube([-4.6, 6, -2.5], [0, 2, 6], frill, [0, 0, -20], [-4.5, 8, 0]), Cube([4.6, 6, -2.5], [0, 2, 6], frill, [0, 0, 20], [4.5, 8, 0])])
    hy = 16
    m.bone("head", "body", [0, hy, -1], face_blob([-4.5, hy, -5.5], [9, 4.5, 7], face))
    m.bone("jaw", "head", [0, hy + 0.6, -2.5], [Cube([-4, hy - 0.4, -5.8], [8, 1, 4], jaw_m)])
    m.eyes("head", x_inner=1.6, y=hy + 2.4, z=-5.5, w=2, h=2, iris="#3A2024", pupil="#120808", skin=P, grid=["DD", "PW"])
    upright_arms(m, 5.2, 14, -0.5, 3.5, 3.5, 2, skin, skin, hw=3, hh=1, fingers=3, finger_m=toe, finger_len=1.5, rest=(-6, 0, 16), fore_rest=(-20, 0, 0))
    upright_legs(m, 2.6, 6, 0.5, 3, skin, skin, 5, ntoes=4, toe_m=toe, thigh_w=4)
    segs = [((3.6 - i * 0.5), (3.2 - i * 0.4), 3) for i in range(4)]
    names, zt = tail_bones(m, "body", 6.5, 3, segs, skin, (-24, -6, 4, 6))
    tip = bone_of(m, names[-1])
    ty = tip.pivot[1]
    shell = conch_mat("#F2E4D6", "#9A6AB8", "#7A5A6A")
    tip.cubes += blob([-3, ty - 3, zt - 1.5], [6, 6, 6], shell) + blob([-2, ty + 2.5, zt - 0.5], [4, 3, 4], shell) + [Cube([-1, ty + 5, zt + 0.5], [2, 2, 2], shell)]
    tip.cubes += spike(-2.8, ty + 1, zt + 1.5, 2, [0, -90, 0], taper_mat("#E8D8C8", "#FFFFFF"), 1) + spike(2.8, ty + 1, zt + 1.5, 2, [0, 90, 0], taper_mat("#E8D8C8", "#FFFFFF"), 1)
    tip.cubes += [Cube([-0.6, ty - 3.4, zt - 2], [0, 2, 1], Mat("#C8A0B0", noise=0, edge=0)), Cube([0.6, ty - 3.4, zt - 2], [0, 2, 1], Mat("#C8A0B0", noise=0, edge=0))]
    moves(m, "HEADBUTT", "PSYCHIC")
    fx_for(m, 1.0)
    A, info = biped(name.lower(), m, walk=0.9, stride=20, k=1.0, slow=1.4, guard=16)
    write(sid, name, m, A, info, stands)


def slowmander():
    salamander("slowpoke", "SLOWMANDER", 1.0, False)


def shellmander():
    salamander("slowbro", "SHELLMANDER", 1.6, True)


def single_eye(m, bone, x0, y, z, w, h, iris, pupil, skin, grid, iris_light=None):
    """One eye in the middle of a face (with its lid, so it blinks like a pair does)."""
    em = eye_mat(w, h, iris, pupil, "#1A1414", "#FFFFFF", iris_light=iris_light, grid=grid, skin=skin)
    m.bone("eye_right", bone, [x0 + w / 2, y + h, z], [Cube([x0, y, z - 0.01], [w, h, 0], em)])
    lm = Mat(skin, noise=0.01, shade=0.2, edge=0)
    m.bone("eyelid_right", "eye_right", [x0 + w / 2, y + h, z], [Cube([x0 - 0.02, y, z - 0.03], [w + 0.04, h, 0], lm)])


def ghost_flame_mat():
    """A ghost's flame: a flame's shape, burning blue and violet."""
    def g(at, fi):
        r = flame_shape(at, fi)
        if r is None:
            return None
        c, a = r
        return rgb([c[2], c[1] * 0.85, c[0]]), a
    return Mat("#5A6AFF", lambda a: CLEAR, glow=g, frames=True, noise=0, edge=0)


def bird_legs(m, x, hip, z, thigh, shank, w, thighm, legm, talon, toe_len=3, dagger=False):
    """A bird's leg in joints: the feathered thigh to the knee, the bare scaly shank from there (bending back at the
    ankle as it steps), and the foot: three toes splayed forward and one back, each with a hooked claw; (dagger) a long
    straight claw on the inner toe."""
    knee = hip - thigh
    m.pair("leg_right", "body", [-x, hip, z], [Cube([-x - (w + 1) / 2, knee - 0.3, z - (w + 1) / 2], [n(w + 1), n(thigh + 0.6), n(w + 1)], thighm)])
    sw = max(1, n(w * 0.6))
    m.pair("shin_right", "leg_right", [-x, knee, z], [Cube([-x - sw / 2, 0.8, z - sw / 2], [sw, n(knee - 0.6), sw], legm)])
    toes = []
    for a in (-30, 0, 30):
        sa, ca = math.sin(math.radians(a)), math.cos(math.radians(a))
        tx, tz = -x + toe_len * sa, z - toe_len * ca
        toes += [Cube([-x - 0.5, 0, z - toe_len], [1, 1, n(toe_len)], legm, [0, a, 0], [-x, 0.5, z]),
                 Cube([tx - 0.5, 0, tz - 1.5], [1, 1, 2], talon, [30, a, 0], [tx, 1, tz])]
    toes.append(Cube([-x - 0.5, 0, z], [1, 1, n(toe_len * 0.5)], legm))
    if dagger:
        a = -30
        sa, ca = math.sin(math.radians(a)), math.cos(math.radians(a))
        tx, tz = -x + toe_len * sa, z - toe_len * ca
        toes.append(Cube([tx, 0, tz - 3.5], [0, 2, 3.5], sharp_claw_mat(), [0, a, 0], [tx, 0.5, tz]))
    m.pair("foot_right", "shin_right", [-x, 1, z], toes)


# ======================================================================================================
# The bolt nuts: BOLTNUT > BOLTRIO (in place of Magnemite, Magneton)
# ======================================================================================================


def steel_mat(base, light, dark, rivet=None):
    """Brushed steel: streaks of light along it, darker at the bottom, (rivet) rivets at the corners."""
    B, Lt, Dk = rgb(base), rgb(light), rgb(dark)

    def f(at):
        k = 0.45 + 0.55 * at.ly if at.face not in ("top", "bottom") else (1.0 if at.face == "top" else 0.3)
        c = mix(Dk, B, k)
        if hash01("brush", round(at.y * 2), round((at.x + at.z) * 0.5)) < 0.18:
            c = mix(c, Lt, 0.35)
        if rivet and at.face in ("front", "back") and min(at.lx, 1 - at.lx) < 0.16 and min(at.ly, 1 - at.ly) < 0.16:
            return rgb(rivet)
        return c
    return f


def nut(m, bone, parent, cx, cy, cz, s, steel, ring, coil_m, tip_r, tip_b, eye_bone_name=None, main=True):
    """One bolt nut: a six-sided steel nut facing forward, its hole the socket of one big eye, copper coils for ears
    with a red and a blue tip crackling between them, a bolt through it."""
    cubes = [Cube([cx - 4 * s, cy - 2.3 * s, cz - 2 * s], [n(8 * s), n(4.6 * s), n(4 * s)], steel, [0, 0, a], [cx, cy, cz]) for a in (0, 60, 120)]
    cubes += [Cube([cx - 2.4 * s, cy - 2.4 * s, cz - 2.25 * s], [n(4.8 * s), n(4.8 * s), 0], ring)]
    m.bone(bone, parent, [cx, cy, cz], cubes)
    return bone


def bolt_nut(sid, name, stands, trio):
    """A living bolt nut that floats: a six-sided nut of brushed steel facing forward, one big eye looking out of its
    hole, copper coil ears with a red tip and a blue tip crackling between them, a bolt through it with a screw thread
    below and a hex head on top. Three together, linked by copper rods and arcs of light, when grown."""
    m = Model()
    s = 1.0
    STEEL, STEEL_L, STEEL_D = "#9AA6B4", "#DDE6EE", "#5A6674"
    steel = Mat(STEEL, steel_mat(STEEL, STEEL_L, STEEL_D), noise=0.02)
    ring = Mat("#2A3038", lambda a: rgb("#14181E") if math.hypot(a.lx - 0.5, a.ly - 0.5) < 0.42 else CLEAR, noise=0, edge=0, flat=True)
    coil = Mat("#C8743A", lambda a: rgb("#8A4A20") if a.row % 2 else rgb("#E8A060"), noise=0.01)
    thread = Mat("#8A96A4", lambda a: rgb("#5A6674") if int(a.y * 2) % 2 else None, noise=0.01)
    red = Mat("#E8402A", glow=lambda a: rgb("#FF6A4A"), noise=0, edge=0)
    blue = Mat("#3A6AE8", glow=lambda a: rgb("#6A9AFF"), noise=0, edge=0)
    arc = Mat("#FFF27A", lambda a: CLEAR, glow=lambda a: rgb("#FFF8A0") if abs(a.ly - 0.5 - 0.3 * math.sin(a.lx * 9)) < 0.18 else None, noise=0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    y0 = 9
    m.bone("body", "root", [0, y0, 0], [Cube([-1, y0 - 1, -1], [2, 2, 2], steel)])
    units = [("head", 0.0, y0 + (4.5 if trio else 0), 0.0)] + ([("nut2_right", -5.0, y0 - 3, 0.5)] if trio else [])
    for nm, cx, cy, cz in units:
        par = "body"
        nut(m, nm, par, cx, cy, cz, s, steel, ring, coil, red, blue)
        b = bone_of(m, nm)
        # The bolt: a hex head on top, a screw thread out the bottom.
        b.cubes += [Cube([cx - 1.2, cy + 3.6, cz - 1.2], [3, 1.4, 3], steel), Cube([cx - 0.6, cy + 2.2, cz - 0.6], [1, 1.6, 1], thread),
                    Cube([cx - 0.8, cy - 6.2, cz - 0.8], [2, 4, 2], thread)]
        # Coil ears, crackling between their tips.
        b.cubes += [Cube([cx - 6.4, cy - 1, cz - 1], [2.6, 2, 2], coil), Cube([cx - 7.2, cy - 1.2, cz - 1.2], [1, 2.4, 2.4], red),
                    Cube([cx + 3.8, cy - 1, cz - 1], [2.6, 2, 2], coil), Cube([cx + 6.2, cy - 1.2, cz - 1.2], [1, 2.4, 2.4], blue)]
    if trio:
        b = bone_of(m, "nut2_right")
        m.bone("nut2_left", "body", [5.0, y0 - 3, 0.5], [mirror_cube(c) for c in b.cubes])
        # Copper rods between them, and arcs crackling along them.
        rod = Mat("#B8642A", lambda a: rgb("#E8A060") if a.face == "top" else None, noise=0.01)
        bone_of(m, "body").cubes += [Cube([-0.5, y0 - 2, 0], [1, 1, 1], rod)]
        for sx in (-1, 1):
            bone_of(m, "body").cubes += [Cube([sx * 2.5 - 0.5, y0 - 1.5, -0.5], [1, 6, 1], rod, [0, 0, sx * 40], [sx * 2.5, y0 + 1.5, 0]),
                                          Cube([sx * 2.5 - 1.5, y0 - 1.5, -2.6], [3, 6, 0], arc, [0, 0, sx * 40], [sx * 2.5, y0 + 1.5, -2.6])]
        bone_of(m, "body").cubes += [Cube([-4, y0 - 4, -0.5], [8, 1, 1], rod), Cube([-4, y0 - 5, -2.4], [8, 2.4, 0], arc)]
        single_eye(m, "head", -1.5, y0 + 4.5 - 1.5, -2.27, 3, 3, "#E83A3A", "#1A0606", "#2A3038", None, "#FF8A6A")
        for side, cx in (("2", -5.0), ("3", 5.0)):
            em = eye_mat(3, 3, "#E83A3A", "#1A0606", "#1A1414", "#FFFFFF", iris_light="#FF8A6A", skin="#2A3038")
            bone_of(m, "nut2_right" if cx < 0 else "nut2_left").cubes.append(Cube([cx - 1.5, y0 - 4.5, 0.5 - 2.27], [3, 3, 0], em))
    else:
        single_eye(m, "head", -1.5, y0 - 1.5, -2.27, 3, 3, "#3A3A44", "#0A0A10", "#2A3038", None, "#8A8A9A")
    fx_for(m, 0.8 if not trio else 1.1)
    moves(m, "TACKLE" if not trio else "SONICBOOM", "THUNDERSHOCK" if not trio else "ZAP CANNON")
    ex = {"idle": {"head": track(rotation=[0, 0, wave(3, 14)])}, "battle": {"head": track(rotation=[0, 0, wave(1.2, 18)])}}
    if trio:
        for nm, ph in (("nut2_right", 120), ("nut2_left", 240)):
            ex["idle"][nm] = track(rotation=[0, 0, wave(3, 14, ph)], position=[0, wave(2.4, 0.5, ph), 0])
            ex["battle"][nm] = track(rotation=[0, 0, wave(1.2, 18, ph)], position=[0, wave(1.2, 0.6, ph), 0])
    A, info = floater(name.lower(), m, k=0.7 if not trio else 1.0, bob=0.8, extras=ex)
    write(sid, name, m, A, info, stands)


def boltnut():
    bolt_nut("magnemite", "BOLTNUT", 0.75, False)


def boltrio():
    bolt_nut("magneton", "BOLTRIO", 1.25, True)


# ======================================================================================================
# REEDFISHER (in place of Farfetch'd)
# ======================================================================================================


def reedfisher():
    """A kingfisher that carries a bulrush like a sword: a big head with a long dagger of a beak, a shaggy crest at the
    back of its head, teal-blue back and wings spotted with pale blue, a rust-orange breast, a white throat, short red
    legs, a short tail, and a tall bulrush (a brown velvet head on a green stem) held under one wing."""
    m = Model()
    s = 1.3
    S = lambda v: [x * s for x in v]
    BACK, BACK_L, BACK_D, BREAST, THROAT = "#2A86B8", "#62C0E8", "#174E78", "#E8832A", "#FFF6E8"

    def body_p(at):
        if at.face == "front" or (at.face in ("nx", "px", "bottom") and at.y < 5.4 * s):
            return mix(BREAST, "#F0A050", 0.4 * hash01("br", round(at.x * 2), round(at.y * 2)))
        c = fur(BACK, BACK_L, BACK_D)(at)
        if hash01("spot", round(at.x), round(at.y), round(at.z)) < 0.08:
            return mix(c, "#A8E8FF", 0.7)
        return c

    def head_p(at):
        if at.face in ("front", "nx", "px") and at.y < 8.4 * s:
            return rgb(THROAT)
        if at.face in ("nx", "px") and abs(at.y - 9.2 * s) < 0.5 * s:
            return rgb("#E8832A")
        c = fur(BACK, BACK_L, BACK_D)(at)
        return mix(c, "#A8E8FF", 0.6) if hash01("hs", round(at.x * 2), round(at.z * 2)) < 0.12 and at.face == "top" else c
    body = Mat(BACK, body_p, noise=0.02)
    head = Mat(BACK, head_p, noise=0.02)
    beak = Mat("#1E1C20", lambda a: rgb("#3A363C") if a.face == "top" else None, noise=0.01, edge=0)
    crest = Mat(BACK, feather_plane(BACK_D, BACK, BACK_L, edge="#A8E8FF"), noise=0.015, edge=0)
    sec = Mat(BACK, feather_plane(BACK_D, BACK, BACK_L, edge="#A8E8FF", spot="#A8E8FF"), noise=0.015, edge=0)
    pri = Mat(BACK_D, feather_plane("#123A5A", BACK_D, BACK, edge="#62C0E8"), noise=0.015, edge=0)
    cov = Mat(BACK, lambda a: mix(BACK, BACK_L, a.ly), noise=0.02)
    tfea = Mat(BACK_D, feather_plane(BACK_D, BACK, "#123A5A", edge=BACK_L), noise=0.015, edge=0)
    legm = Mat("#D84A3A", lambda a: rgb("#A83020") if a.face == "bottom" else None, noise=0.02, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 5, 0]), blob(S([-2.8, 3, -3.5]), S([5.6, 5, 7.5]), body, b=1))
    m.bone("head", "body", S([0, 8, -2.8]), face_blob(S([-2.6, 7.4, -6]), S([5.2, 4, 4.6]), head))
    m.bone("beak", "head", S([0, 9, -6]), [Cube(S([-0.6, 8.4, -10.5]), [n(1.2 * s), n(1.2 * s), n(4.5 * s)], beak)])
    m.bone("beak_lower", "head", S([0, 8.4, -6]), [Cube(S([-0.5, 7.9, -9.6]), [n(s), 1, n(3.6 * s)], beak)])
    m.bone("plume", "head", S([0, 11, -2.6]), [Cube([0, 10.8 * s, (-3.2 + i * 1.1) * s], [0, n(3 * s), n(2.6 * s)], crest, [-50 - i * 10, 0, 0],
                                                     [0, 10.8 * s, (-3.2 + i * 1.1) * s]) for i in range(4)])
    m.eyes("head", 2.6 * s, 8.8 * s, -6 * s, h=2, corner=(1, 2), half=2.6 * s, iris="#1A1014", pupil="#06040A", skin=BACK, iris_light="#5A4A50",
           grid=["DLL", "PPW"])
    jointed_wing(m, 2.8 * s, 7.8 * s, -3 * s, n(6.4 * s), (2.4 * s, 2.4 * s, 2 * s, 3.4 * s), sec, sec, pri, arm=cov)
    m.bone("tail", "body", S([0, 5, 4]), [Cube([-0.75 * s, 5 * s, 3.8 * s], [n(1.5 * s), 0, n(3.4 * s)], tfea, [0, ry, 0], [0, 5 * s, 3.8 * s]) for ry in (-20, -7, 7, 20)],
           rotation=[-12, 0, 0])
    bird_legs(m, 1.3 * s, 3.2 * s, 0.3 * s, 1.2 * s, 2 * s, 1, body, legm, Mat("#2A1E1E", noise=0, edge=0), toe_len=2)
    # The bulrush, tucked under the right wing like a sword at its side.
    stem = Mat("#5A9A3A", lambda a: rgb("#3A6A28") if a.col % 2 else None, noise=0.01, edge=0)
    velvet = Mat("#7A4A2A", fur("#7A4A2A", "#A06A40", "#4A2A18", speck=0.4), noise=0.03)
    bone_of(m, "wing_right").cubes += [Cube(S([-3.6, 2, -9]), [1, 1, n(17 * s)], stem, [-38, 0, 0], S([-3.4, 7, -1])),
                                       Cube(S([-4.1, 1.5, -10.5]), [n(2 * s), n(2 * s), n(5 * s)], velvet, [-38, 0, 0], S([-3.4, 7, -1]))]
    fx_for(m, s)
    moves(m, "FURY ATTACK", "SLASH")
    A, info = bird("reedfisher", m, walk=0.45, stride=34, k=s, flap=0.4,
                   extras={"walk": {"shin_right": track(rotation=[bounce(0.45, 24), 0, 0]), "shin_left": track(rotation=[bounce(0.45, 24, 90), 0, 0])}})
    write("farfetch_d", "REEDFISHER", m, A, info, 1.05)


# ======================================================================================================
# The cassowaries: TWINCASK > TRICASK (in place of Doduo, Dodrio)
# ======================================================================================================


def cassowary(sid, name, stands, heads):
    """A cassowary with more than one head: a round body of long shaggy black plumes hanging like hair, long strong
    scaly legs on three toes with a dagger of a claw on each inner toe, and on blue necks hung with red wattles, heads
    each crowned with a tall horn casque. Two heads when young; three when grown, the middle one tallest."""
    m = Model()
    s = 1.0 if heads == 2 else 1.25
    S = lambda v: [x * s for x in v]
    PL, PL_L, PL_D = "#24222A", "#4A4654", "#121016"
    plumes = shag_mat(PL_L, "#5A5664", PL_D)
    body = Mat(PL, fur(PL, PL_L, PL_D, speck=0.5), noise=0.03)
    neckm = Mat("#3A8AD8", lambda a: mix("#2A5AB8", "#5AB8F0", a.ly), noise=0.02)
    head = Mat("#3A8AD8", lambda a: rgb("#2A2A34") if a.face == "top" else None, noise=0.02)
    casque = Mat("#A87A4A", lambda a: mix("#6A4A28", "#D8A870", a.ly), noise=0.02)
    beak = Mat("#2A2630", lambda a: rgb("#4A4450") if a.face == "top" else None, noise=0.01, edge=0)
    wattle = Mat("#D8302A", lambda a: rgb("#A81E1A") if a.ly < 0.3 else None, noise=0.02)
    legm = Mat("#6A7A8A", lambda a: mix("#4A5462", "#8A96A4", a.ly) if a.row % 2 else None, noise=0.02)
    talon = Mat("#2A2420", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    by = 10
    m.bone("body", "root", S([0, by + 3, 0]), blob(S([-4.2, by, -5]), S([8.4, 7, 10]), body, b=2) +
           [Cube(S([sx * 4.3, by - 3, -4]), [0, n(6 * s), n(9 * s)], plumes) for sx in (-1, 1)] + [Cube(S([-4, by - 3, 5.1]), [n(8 * s), n(6 * s), 0], plumes)])
    # The necks: from the front of the body, each with its own head.
    spots = [(0, "head", "neck", 0, 9, -14)] if heads == 1 else []
    if heads == 2:
        spots = [(-1.8, "head", "neck", 12, 8, -10), (1.8, "head2", "neck2", -12, 8, -10)]
    elif heads == 3:
        spots = [(0, "head", "neck", 0, 10, -6), (-2.4, "head2", "neck2", 22, 8, -14), (2.4, "head3", "neck3", -22, 8, -14)]
    for x, hn, nn, yaw, L, tilt in spots:
        m.bone(nn, "body", S([x, by + 5, -4]), [Cube(S([x - 0.9, by + 5, -4.9]), [n(1.8 * s), n(L * s), n(1.8 * s)], neckm),
                                                Cube(S([x - 0.9, by + 5.5, -5.4]), [n(1.8 * s), n(2 * s), 0], wattle)], rotation=[tilt, yaw, 0])
        ty = by + 5 + L
        m.bone(hn, nn, S([x, ty, -4]), face_blob(S([x - 1.5, ty - 0.5, -6.6]), S([3, 2.6, 3.6]), head) +
               [Cube(S([x - 0.6, ty + 1.8, -6]), [n(1.2 * s), n(3 * s), n(3 * s)], casque),
                Cube(S([x - 0.8, ty, -9.4]), [n(1.6 * s), 1, n(2.8 * s)], beak)], rotation=[-tilt, 0, 0])
        bone_of(m, hn).cubes += [Cube(S([x - 0.8, ty - 0.8, -6.6]), [n(1.6 * s), 1, n(1.6 * s)], wattle)]
        em = eye_mat(1, 1, "#C8501A", "#100804", "#1A1414", "#FFFFFF", grid=["I"], skin="#3A8AD8")
        bone_of(m, hn).cubes += [Cube(S([x - 1.52, ty + 0.8, -6.2]), [0, 1, 1], em), Cube(S([x + 1.52, ty + 0.8, -6.2]), [0, 1, 1], em)]
    # Little wings of bare quills.
    quill = Mat("#1A1820", lambda a: rgb("#3A3644") if a.ly > 0.6 else None, noise=0, edge=0)
    m.pair("wing_right", "body", S([-4.3, by + 4.5, -1]), [Cube(S([-4.4, by + 1.5 - i * 0.6, -1 + i]), [1, n(3 * s), 1], quill, [-20 - i * 10, 0, 0], S([-4.4, by + 4.5, -1]))
                                                           for i in range(3)])
    bird_legs(m, 2.2 * s, (by + 1) * s, 0.5 * s, 4 * s, 7 * s, 2, body, legm, talon, toe_len=3, dagger=True)
    fx_for(m, s)
    moves(m, "PECK" if heads == 2 else "DRILL PECK", "FURY ATTACK" if heads == 2 else "TRI ATTACK")
    W = 0.5 if heads == 2 else 0.6
    ex = {"idle": {}, "walk": {"shin_right": track(rotation=[bounce(W, 30), 0, 0]), "shin_left": track(rotation=[bounce(W, 30, 90), 0, 0])}, "battle": {}}
    for i, (_, hn, nn, *_r) in enumerate(spots[1:], start=1):
        ex["idle"][hn] = track(rotation=[wave(2.2, 8, i * 100), wave(3, 18, i * 140), 0])
        ex["idle"][nn] = track(rotation=[wave(2.6, 5, i * 70), 0, 0])
        ex["walk"][hn] = track(position=[0, 0, wave(W / 2, 0.5, i * 90)])
    A, info = bird(name.lower(), m, walk=W, stride=34, k=s, flap=0.3, extras=ex)
    write(sid, name, m, A, info, stands)


def twincask():
    cassowary("doduo", "TWINCASK", 2.05, 2)


def tricask():
    cassowary("dodrio", "TRICASK", 3.2, 3)


# ======================================================================================================
# The ice seals: FROSTPUP > GLACISEAL (in place of Seel, Dewgong)
# ======================================================================================================


def seal(sid, name, stands, big):
    """A seal. The pup is a fluffy ball of cream-white fur with huge dark eyes, a small black nose, long frosty whiskers,
    flippers, and a glowing pale-blue snowflake on its brow. Grown, a long sleek silver-blue seal with a frosty mane of
    icicle spines down its back, two long tusks of ice, heavy brows, and a long tail in joints ending in broad flukes."""
    m = Model()
    s = 1.0 if not big else 1.5
    S = lambda v: [x * s for x in v]
    if not big:
        F, F_L, F_D, BELLY = "#F2EEE6", "#FFFFFF", "#C8C8D0", "#FFFFFF"
    else:
        F, F_L, F_D, BELLY = "#B8CCE0", "#E8F4FF", "#7A90AC", "#E8F0F8"
    furm = Mat(F, fur(F, F_L, F_D, belly=BELLY, below=1.4 * s, speck=0.6), noise=0.03)
    face = Mat(F, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.03)
    nose = Mat("#1A1A20", noise=0, edge=0)
    jaw_m = Mat(BELLY, open_mouth("#C85A6A", "#E8D8D8"), noise=0.02)
    flip = Mat(F_D, lambda a: rgb(mix(F_D, "#000000", 0.2)) if a.face == "bottom" else None, noise=0.02)
    fluke = Mat(F_D, lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (0.4 + 0.6 * a.lz) or (a.lz > 0.85 and abs(a.lx - 0.5) < 0.12) else None, noise=0.01, edge=0, flat=True)
    ice = taper_mat("#A8E0F8", "#FFFFFF")
    m.bone("root", None, [0, 0, 0])
    if not big:
        m.bone("body", "root", [0, 4, 0], blob([-4, 0.5, -5], [8, 6.5, 11], furm, b=2))
        m.bone("head", "body", [0, 6, -4.5], face_blob([-3.6, 3, -9.6], [7.2, 6, 5.4], face) + [Cube([-0.7, 5, -9.9], [1.4, 1, 1], nose)])
        m.bone("jaw", "head", [0, 4.2, -8], [Cube([-1.2, 3.4, -9.8], [2.4, 1, 2], jaw_m)])
        m.bone("snout", "head", [0, 5, -9.6], [])
        m.eyes("head", x_inner=1.0, y=5.8, z=-9.6, w=2, h=2, iris="#141018", pupil="#06040A", skin=F, iris_light="#4A4050",
               grid=["PPPW", "PPWP", "PPPP", "KPPK"])
        m.pair("whiskers_right", "head", [-1.2, 5.2, -9.6], [Cube([-5.6, 5.2, -10.6], [4.4, 0, 2], whisker_mat("#E8F4FF"))])
        m.bone("mark", "head", [0, 8.4, -9.62], [Cube([-1, 7.8, -9.63], [2, 2, 0], glow_patch("#A8E8FF", "diamond"))])
        m.pair("leg_front_right", "body", [-3.6, 2.5, -2.5], [Cube([-6, 0, -3.5], [3, 1, 3], flip, [0, 0, -20], [-3.6, 1, -2.5])])
        names, zt = tail_bones(m, "body", 2.5, 5.5, [(5, 3, 2.5), (3.5, 2, 2)], furm, (0, 6))
        bone_of(m, names[-1]).cubes.append(Cube([-3, 1.6, zt - 0.5], [6, 0, 4], fluke))
        moves(m, "HEADBUTT", "AURORA BEAM")
        k = 0.7
    else:
        m.bone("body", "root", S([0, 4, 0]), blob(S([-3.6, 0.5, -6]), S([7.2, 6, 13]), furm, b=2))
        bd = bone_of(m, "body")
        for i in range(6):
            z = (-5 + i * 2.2) * s
            bd.cubes += spike(0, 6.4 * s, z, (3.5 - abs(i - 2) * 0.4) * s, [48, 0, 0], ice, 2)
        m.bone("neck", "body", S([0, 5, -5]), blob(S([-2.8, 2.5, -8]), S([5.6, 5, 4]), furm), rotation=[-16, 0, 0])
        m.bone("head", "neck", S([0, 6.5, -7.5]), face_blob(S([-3, 4, -12]), S([6, 4.6, 5]), face) + [Cube(S([-0.7, 5.6, -12.3]), [n(1.4 * s), 1, 1], nose),
                                                                                                    Cube(S([-3.2, 8, -12.4]), [n(6.4 * s), 1, n(1.4 * s)], Mat(F_D, noise=0.02, edge=0))],
               rotation=[16, 0, 0])
        hd = bone_of(m, "head")
        hd.cubes += spike(-1.2 * s, 4.4 * s, -11.8 * s, 4 * s, [-100, 0, 0], ice, 1.6) + spike(1.2 * s, 4.4 * s, -11.8 * s, 4 * s, [-100, 0, 0], ice, 1.6)
        m.bone("jaw", "head", S([0, 4.8, -10]), [Cube(S([-1.6, 3.8, -12]), [n(3.2 * s), 1, n(2.4 * s)], jaw_m)])
        m.eyes("head", x_inner=0.9 * s, y=6.4 * s, z=-12 * s, w=2, h=2, iris="#1A2440", pupil="#06040A", skin=F, iris_light="#6A8AC8",
               grid=["LLLL", "PPWW", "PPPW", "KPPK"])
        m.pair("whiskers_right", "head", S([-1.2, 5.6, -12]), [Cube(S([-6, 5.6, -13]), [n(4.6 * s), 0, n(2 * s)], whisker_mat("#E8F4FF"))])
        m.pair("leg_front_right", "body", S([-3.2, 2.5, -3]), [Cube(S([-6.4, 0, -4]), [n(3.4 * s), 1, n(3.4 * s)], flip, [0, 0, -18], S([-3.2, 1, -3]))])
        names, zt = tail_bones(m, "body", 2.6 * s, 6.5 * s, [(5 * s, 3 * s, 3 * s), (3.6 * s, 2.4 * s, 3 * s), (2.4 * s, 1.6 * s, 3 * s)], furm, (0, 6, 8))
        bone_of(m, names[-1]).cubes.append(Cube([-3.6 * s, 2.2 * s, zt - 0.5], [n(7.2 * s), 0, n(5 * s)], fluke))
        moves(m, "TAKE DOWN", "ICE BEAM")
        k = 1.0
    fx_for(m, 0.8 * s)
    A, info = four_legs(name.lower(), m, walk=0.9, stride=28, k=k, slow=1.3, extras={"walk": {"body": track(position=[0, bounce(0.9, 0.8 * s), 0], rotation=[wave(0.9, 5), 0, 0]),
                                                                                         "tail": track(rotation=[wave(0.9, 12, 90), 0, 0])}})
    write(sid, name, m, A, info, stands)


def frostpup():
    seal("seel", "FROSTPUP", 0.85, False)


def glaciseal():
    seal("dewgong", "GLACISEAL", 1.4, True)


# ======================================================================================================
# The bog slimes: BOGBLOB > MIREMASS (in place of Grimer, Muk)
# ======================================================================================================


def ooze_mat(base, light, dark, bubble):
    """Glossy swamp ooze: dark in its folds, a wet shine along the top, little pale bubbles in it."""
    B, Lt, Dk, Bu = rgb(base), rgb(light), rgb(dark), rgb(bubble)

    def f(at):
        c = mix(Dk, B, 0.4 + 0.6 * at.ly) if at.face != "top" else mix(B, Lt, 0.4)
        h = hash01("bub", round(at.x * 2), round(at.y * 2), round(at.z * 2))
        if h < 0.05:
            return Bu
        if h < 0.09:
            return mix(c, Lt, 0.5)
        if at.face != "bottom" and hash01("sheen", round(at.x), round(at.z)) < 0.15 and at.ly > 0.7:
            return mix(c, "#FFFFFF", 0.3)
        return c
    return f


def bog_slime(sid, name, stands, big):
    """A slime of the bog: a heap of glossy green-brown ooze sagging over a puddle of itself, bubbles in it, a lily pad
    with a little flower stuck on top, small pale eyes, a drooling grin, and two drippy arms. Grown, a great mound of
    murk with a broken branch, reeds and lily pads caught in it, a wide toothy maw, long dripping arms, and moss."""
    m = Model()
    s = 1.0 if not big else 1.6
    S = lambda v: [x * s for x in v]
    O, O_L, O_D, BUB = ("#5A7A3A", "#8AAE5A", "#2E4420", "#C8E8A0") if not big else ("#4A5A3A", "#7A8A5A", "#22281A", "#B8D090")
    ooze_m = Mat(O, ooze_mat(O, O_L, O_D, BUB), noise=0.02)
    jaw_m = Mat(O_D, open_mouth("#3A1A1A", O_D), noise=0.02)
    tooth = Mat("#E8E0C0", noise=0, edge=0)
    pad = Mat("#4A9A3A", lambda a: CLEAR if math.hypot(a.lx - 0.5, a.lz - 0.5) > 0.5 or (abs(a.lx - 0.5) < 0.06 and a.lz < 0.5) else
              (rgb("#7AC860") if math.hypot(a.lx - 0.5, a.lz - 0.5) < 0.2 else None), noise=0.01, edge=0, flat=True)
    flower = Mat("#F8C8E0", lambda a: rgb("#FFF0A0") if a.face == "top" and abs(a.lx - 0.5) < 0.3 else None, noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("puddle", "root", [0, 0, 0], blob(S([-6.5, 0, -6]), S([13, 1.5, 12]), ooze_m))
    m.bone("body", "root", S([0, 2, 0]), blob(S([-4.8, 1, -4.4]), S([9.6, 6, 8.8]), ooze_m, b=2) + blob(S([-3.6, 6.5, -3.4]), S([7.2, 3, 6.6]), ooze_m) +
           [Cube(S([x - 0.5, 1, z]), [1, n(1.6 * s), 1], ooze_m) for x, z in ((-4.4, -2), (4.2, 1), (-1.6, -4.5), (2.4, -4.4))])
    top = 9.4
    m.bone("head", "body", S([0, 7, -1]), [Cube(S([-3.2, 6.2, -3.6]), [n(6.4 * s), n(2 * s), 1], ooze_m)])
    m.eyes("head", x_inner=0.8 * s, y=7.0 * s, z=-3.6 * s, w=2, h=2, iris="#F0F4D8" if not big else "#E8D860", pupil="#1A1A10", skin=O,
           grid=["SWWS", "WWWW", "WWPW", "SWWS"] if not big else ["DDDD", "JIIJ", "IIPI", "SIIS"])
    jw = 4 if not big else 6
    m.bone("jaw", "body", S([0, 5, -4]), [Cube(S([-jw / 2, 4.2, -4.7]), [n(jw * s), n(1.4 * s), n(2 * s)], jaw_m)] +
           ([Cube(S([-jw / 2 + 0.4 + i * 1.1, 5.2, -4.8]), [1, n(1.2 * s), 1], tooth) for i in range(5)] if big else []))
    upright_arms(m, 4.6 * s, 6 * s, -0.5 * s, 3 * s, 3 * s, n(1.8 * s), ooze_m, ooze_m, hw=n(2.4 * s), hh=n(1.6 * s), fingers=3, finger_len=n(1.4 * s),
                 rest=(10, 0, 40), fore_rest=(-20, 0, -10))
    hd = bone_of(m, "body")
    if not big:
        hd.cubes += [Cube(S([-2, top, -2]), [n(4.4 * s), 0, n(4.4 * s)], pad, [6, 20, 0], S([0, top, 0])),
                     Cube(S([0.4, top, -0.2]), [1, 1, 1], flower), Cube(S([0.2, top + 0.6, -0.4]), [1, 1, 1], Mat("#FFFFFF", noise=0, edge=0))]
    else:
        wood = Mat("#6A4A2A", lambda a: rgb("#C8A070") if a.face in ("front", "back") and min(a.lx, 1 - a.lx, a.ly, 1 - a.ly) > 0.2 else
                   (rgb("#4A3018") if a.row % 2 else None), noise=0.02)
        reed = leaf_mat("#8AB050", "#4A7028", "#C8E090")
        hd.cubes += [Cube(S([-6, 4.5, -1]), [n(5 * s), n(1.8 * s), n(1.8 * s)], wood, [0, 25, -15], S([-3, 5.4, 0])),
                     Cube(S([-2.4, top - 0.2, -2.2]), [n(4.6 * s), 0, n(4.6 * s)], pad, [4, -30, 0], S([0, top, 0])),
                     Cube(S([3, 6, 0]), [n(3.6 * s), 0, n(3.6 * s)], pad, [0, 40, 60], S([4, 6.5, 1]))]
        for x, z, a in ((-2, 1.5, -10), (2.4, 2, 12), (0.4, 2.8, 4)):
            hd.cubes += [Cube(S([x - 0.8, top - 1, z]), [n(1.6 * s), n(6 * s), 0], reed, [a, 0, a * 0.5], S([x, top - 1, z]))]
    fx_for(m, 0.8 * s)
    moves(m, "POUND" if not big else "SLUDGE", "SLUDGE" if not big else "SLUDGE BOMB")
    A, info = ooze(name.lower(), m, k=0.8 * s, slow=1.0 if not big else 1.2)
    write(sid, name, m, A, info, stands)


def bogblob():
    bog_slime("grimer", "BOGBLOB", 1.1, False)


def miremass():
    bog_slime("muk", "MIREMASS", 2.2, True)


# ======================================================================================================
# The shells: GLINTSHELL > THORNYSTER (in place of Shellder, Cloyster)
# ======================================================================================================


def ribbed(base, light, dark, rib_n=9):
    """A shell's valve: ribs fanning out from its hinge at the back, lighter along each rib, growth lines across them."""
    B, Lt, Dk = rgb(base), rgb(light), rgb(dark)

    def f(at):
        if at.face == "bottom":
            return mix(Dk, B, 0.5)
        x, z = at.x, at.z - at.cube.origin[2] - at.cube.size[2]
        ang = math.atan2(x, -z)
        rib = abs(((ang / math.pi * rib_n) % 1.0) - 0.5) * 2
        r = math.hypot(x, z)
        c = mix(Dk, Lt, 0.35 + 0.65 * (1 - rib))
        if abs((r % 1.6) - 0.8) < 0.1:
            c = mix(c, Dk, 0.35)
        return c
    return f


def shell_creature(sid, name, stands, big):
    """A shell that hops on its hinge. The first is a pink-and-cream scallop of two ribbed valves, a frilled orange mantle
    between them with a row of tiny glowing blue eyes all along its edge, and two big white eyes peering out. Grown, a
    thorny oyster: a heavy violet-red shell bristling all over with long pale spines, gaping open on a frosted pearl of a
    face with two eyes in it."""
    m = Model()
    s = 1.0 if not big else 1.5
    S = lambda v: [x * s for x in v]
    if not big:
        SH, SH_L, SH_D = "#E89AA8", "#FFE8E0", "#B05A6A"
    else:
        SH, SH_L, SH_D = "#9A3A5A", "#D888A0", "#5A1A30"
    valve = Mat(SH, ribbed(SH, SH_L, SH_D), noise=0.015)
    mantle = Mat("#F0904A", lambda a: rgb("#FFB878") if a.row % 2 else None, noise=0.02, edge=0)
    dot = Mat("#3AA8FF", glow=lambda a: rgb("#8AE0FF"), noise=0, edge=0)
    spine = taper_mat(SH_D, "#FFF0F4")
    m.bone("root", None, [0, 0, 0])
    w, d, hl = (9, 8, 3) if not big else (11, 10, 5)
    m.bone("body", "root", S([0, 2, 3]), blob(S([-w / 2, 0, -d / 2 - 1]), S([w, 3, d]), valve, b=1) +
           [Cube(S([-w / 2 + 0.5, 2.6, -d / 2 - 1.4]), [n((w - 1) * s), n(1.2 * s), n((d - 1) * s)], mantle)])
    # The top valve is the head: hinged at the back, opening up.
    m.bone("head", "body", S([0, 3, 2.6]), blob(S([-w / 2, 3, -d / 2 - 1]), S([w, hl, d]), valve, b=1) +
           [Cube(S([-1.5, 3.4, 3]), [n(3 * s), n(1.4 * s), n(1.6 * s)], valve), Cube(S([-w / 2, 3, 2.4]), [n(2 * s), n(1.4 * s), n(1.2 * s)], valve)],
           rotation=[-14 if not big else -20, 0, 0])
    m.bone("jaw", "head", S([0, 3, -d / 2 - 1]), [])
    bd = bone_of(m, "body")
    if not big:
        for i in range(8):
            x = (-w / 2 + 1 + i * (w - 2) / 7) * s
            bd.cubes.append(Cube([x - 0.4, 3.6 * s, (-d / 2 - 1.5) * s], [1, 1, 0], dot))
        m.bone("face", "body", S([0, 3, -4]), [])
        m.eyes("body", x_inner=0.6 * s, y=3.0 * s, z=(-d / 2 - 1.45) * s, w=2, h=2, iris="#F4F0E8", pupil="#1A1A20", skin="#F0904A",
               grid=["SWWS", "WWWW", "WPPW", "SWWS"])
        moves(m, "CLAMP", "AURORA BEAM")
    else:
        hd = bone_of(m, "head")
        for i in range(14):
            a = i * 2.4
            r = 0.6 + (i % 3) * 0.12
            x, z = math.sin(a) * w / 2 * r, math.cos(a) * d / 2 * r - 1
            L = (4 + (i % 4)) * s
            hd.cubes += spike(x * s, (3 + hl) * s - 0.5, z * s, L, [40 + (i % 3) * 12, -math.degrees(math.atan2(x, z)), 0], spine, 2)
        for i in range(8):
            a = i * 0.8 - 2.8
            bd.cubes += spike(math.sin(a) * w / 2 * s, 1.2 * s, (math.cos(a) * d / 2 - 1) * s, 3.5 * s, [8, -math.degrees(a), 0], spine, 2)
        pearl = Mat("#E8F4FF", lambda a: mix("#B8D8F0", "#FFFFFF", a.ly), glow=lambda a: rgb("#D8F0FF") if a.ly > 0.75 else None, noise=0.02)
        m.bone("pearl", "body", S([0, 4.5, -2]), blob(S([-2.8, 2.8, -5]), S([5.6, 5, 5]), pearl) +
               [Cube(S([-0.6, 7.6, -3.6]), [n(1.2 * s), n(2.2 * s), n(1.2 * s)], pearl, [-20, 0, 0], S([0, 7.6, -3]))])
        m.eyes("pearl", x_inner=0.4 * s, y=4.6 * s, z=-5 * s, w=2, h=2, iris="#2A3A5A", pupil="#0A0A14", skin="#E8F4FF", iris_light="#8AA8D8",
               grid=["LLLL", "IIPW", "IPPI", "SIIS"])
        moves(m, "SPIKE CANNON", "AURORA BEAM")
    fx_for(m, 0.8 * s)
    A, info = hopper(name.lower(), m, hop=0.5, height=1.4 * s, k=0.7 * s,
                     extras={"idle": {"head": track(rotation=[wave(2.4, 5, 0, -3), 0, 0])},
                             "walk": {"head": track(rotation=[wave(0.5, 14, 180, -6), 0, 0])},
                             "cry": {"head": track(rotation=keys({0: [0, 0, 0], 0.2: [-25, 0, 0], 0.7: [-25, 0, 0], 1.0: [0, 0, 0]}))}})
    write(sid, name, m, A, info, stands)


def glintshell():
    shell_creature("shellder", "GLINTSHELL", 0.45, False)


def thornyster():
    shell_creature("cloyster", "THORNYSTER", 1.4, True)


# ======================================================================================================
# The shadow lanterns: WISPLET > SHADEWICK > GLOOMWARDEN (in place of Gastly, Haunter, Gengar)
# ======================================================================================================


def smoke_mat(base, light, dark):
    """Living shadow: deep violet-black, wisps of paler smoke curling over it."""
    B, Lt, Dk = rgb(base), rgb(light), rgb(dark)

    def f(at):
        w = math.sin(at.x * 1.3 + at.y * 0.9) + math.sin(at.z * 1.1 - at.y * 1.4)
        c = mix(Dk, B, 0.4 + 0.6 * at.ly) if at.face != "top" else mix(B, Lt, 0.25)
        if abs(w) < 0.22:
            c = mix(c, Lt, 0.35)
        return c
    return f


def shadow(sid, name, stands, stage):
    """A shadow that carries a ghostly flame. The first is a ball of violet-black shadow, a ring of smoke drifting round it,
    glowing slanted eyes and a wide grin, a blue wick of flame burning on its crown. The second is a hooded shade with
    no body below its smoky tail, the wick flame on the tip of its hood, and two clawed hands floating free at its sides.
    The last is broad and stout, a lantern in its chest with the blue flame behind the bars, a ridge of smoke spines
    down its back, a huge grin, short clawed legs and arms, and the flame on its crown."""
    m = Model()
    SM, SM_L, SM_D = "#3E2E58", "#7A62A0", "#1C1428"
    shade = Mat(SM, smoke_mat(SM, SM_L, SM_D), noise=0.02)
    jaw_m = Mat(SM_D, open_mouth("#E8E0F8", "#140C1C"), noise=0.01)
    tooth = Mat("#F4F0FF", noise=0, edge=0)
    gf = ghost_flame_mat()
    claw = Mat("#E8E0F0", noise=0, edge=0)
    smoke = Mat("#8A72B8", lambda a: CLEAR if abs(a.ly - 0.5 - 0.25 * math.sin(a.lx * 12)) > 0.22 else None,
                glow=lambda a: rgb("#9A88D8") if abs(a.ly - 0.5 - 0.25 * math.sin(a.lx * 12)) < 0.1 else None, noise=0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    eye_grid = ["DD....", "JJJ..."]
    if stage == 0:
        y0 = 8
        m.bone("body", "root", [0, y0, 0], blob([-4.5, y0 - 4.5, -4.5], [9, 9, 9], shade, b=2))
        m.bone("head", "body", [0, y0, -4.5], [Cube([-3.5, y0 - 2.5, -4.6], [7, 5, 0], Mat(SM, lambda a: CLEAR, noise=0, edge=0, flat=True))])
        m.bone("jaw", "body", [0, y0 - 1.6, -4.5], [Cube([-2.8, y0 - 2.6, -4.7], [5.6, 1.2, 1.2], jaw_m)] + [Cube([-2.6 + i * 1.1, y0 - 1.8, -4.8], [1, 1, 0], tooth) for i in range(5)])
        m.bone("ring", "body", [0, y0, 0], [Cube([-7, y0 - 1, -7], [14, 0, 14], Mat("#8A72B8", lambda a: CLEAR if abs(math.hypot(a.lx - 0.5, a.lz - 0.5) - 0.42) > 0.06 else None,
                                                                                      glow=lambda a: rgb("#A890E8") if abs(math.hypot(a.lx - 0.5, a.lz - 0.5) - 0.42) < 0.04 else None,
                                                                                      noise=0, edge=0, flat=True))], rotation=[12, 0, 8])
        bone_of(m, "body").cubes += flame([0, y0 + 4.2, 0], 4, 6, mat=gf)
        eyes_at, ew = (y0 + 0.4, -4.5), 2
        moves(m, "LICK", "NIGHT SHADE")
    elif stage == 1:
        y0 = 11
        m.bone("body", "root", [0, y0, 0], blob([-4.5, y0 - 3, -4], [9, 7, 8], shade, b=2) + [Cube([-3.5, y0 + 3.5, -3], [7, 2.5, 6], shade),
                                                                                              Cube([-2, y0 + 5.6, -2], [4, 2, 4], shade, [-12, 0, 0], [0, y0 + 5.6, 0]),
                                                                                              Cube([-0.8, y0 + 7.2, -1], [1.6, 1.6, 2], shade, [-24, 0, 0], [0, y0 + 7.2, 0])])
        bone_of(m, "body").cubes += flame([0, y0 + 8.4, -1.4], 3.6, 5.6, mat=gf)
        m.bone("head", "body", [0, y0, -4], [Cube([-3.5, y0 - 1.5, -4.1], [7, 4, 0], Mat(SM, lambda a: CLEAR, noise=0, edge=0, flat=True))])
        m.bone("jaw", "body", [0, y0 - 1.4, -4], [Cube([-3.2, y0 - 2.6, -4.3], [6.4, 1.4, 1.2], jaw_m)] + [Cube([-3 + i * 1.2, y0 - 1.6, -4.4], [1, 1, 0], tooth) for i in range(6)])
        names, zt = tail_bones(m, "body", y0 - 3, 0, [(6, 3, 0.1), (4.5, 2.5, 0.1), (3, 2, 0.1)], shade, (0, 0, 0))
        for i, nm in enumerate(names):
            b = bone_of(m, nm)
            b.cubes = [Cube([-(3 - i) - 0.5, y0 - 3 - 2.5 * (i + 1), -2 + i * 0.8], [n((6 - i * 1.5)), n(2.6), n(4 - i)], shade)]
            b.pivot = [0, y0 - 3 - 2.5 * i, -1 + i * 0.8]
        # Floating hands: no arms, just the hands drifting at its sides, three long claws on each.
        m.pair("arm_right", "body", [-7.5, y0 + 1, -2], [])
        m.pair("forearm_right", "arm_right", [-7.5, y0 + 1, -2], [Cube([-9, y0 - 1, -3.2], [3, 3, 2.4], shade)] +
               [Cube([-9 + i, y0 - 3, -3.6], [1, 2.4, 1], claw, [-20, 0, 0], [-9 + i, y0 - 1, -3]) for i in range(3)] +
               [Cube([-6.6, y0, -3], [1.4, 1, 1.4], shade)])
        eyes_at, ew = (y0 + 0.8, -4), 2
        moves(m, "LICK", "CONFUSE RAY")
    else:
        hip = 4
        m.bone("body", "root", [0, hip + 6, 0], blob([-6.5, hip - 0.5, -5], [13, 12, 10], shade, b=2) + blob([-5.5, hip + 11, -4], [11, 2, 8], shade))
        bd = bone_of(m, "body")
        for i in range(5):
            bd.cubes += spike(0, hip + 12.5 - i * 1.4, 3 + i * 0.8, 4 - abs(i - 1) * 0.5, [-40 + i * 18, 0, 0], taper_mat(SM, "#B8A0E8"), 2.4)
        # The lantern in its chest: the blue flame behind dark bars.
        bars = Mat("#14101C", lambda a: CLEAR if (a.col % 3 != 0 and min(a.ly, 1 - a.ly) > 0.15) else None, noise=0, edge=0, flat=True)
        bd.cubes += [Cube([-2.6, hip + 1.6, -5.25], [5.2, 5, 0], Mat("#2A2A6A", glow=lambda a: rgb("#6A9AFF") if math.hypot(a.lx - 0.5, a.ly - 0.45) < 0.32 else None,
                                                                    noise=0, edge=0, flat=True)),
                     Cube([-2.8, hip + 1.4, -5.4], [5.6, 5.4, 0], bars)]
        bd.cubes += flame([0, hip + 1.6, -5.3], 3.2, 4.4, mat=gf, planes=1)
        bd.cubes += flame([0, hip + 13, -1], 4, 6.5, mat=gf)
        m.bone("head", "body", [0, hip + 9, -5], [Cube([-5, hip + 7.5, -5.1], [10, 4, 0], Mat(SM, lambda a: CLEAR, noise=0, edge=0, flat=True))])
        m.bone("jaw", "body", [0, hip + 8.2, -5], [Cube([-4.6, hip + 7.2, -5.3], [9.2, 1.6, 1.4], jaw_m)] +
               [Cube([-4.4 + i * 1.1, hip + 8.2, -5.4], [1, 1, 0], tooth) for i in range(9)])
        upright_arms(m, 6.8, hip + 9, -1, 3, 3, 3, shade, shade, hw=3.5, hh=2, claws=3, rest=(-14, 0, 26), fore_rest=(-30, 0, 0))
        upright_legs(m, 3.4, hip, 0, 3, shade, shade, 4, ntoes=3, sharp=True, thigh_w=4)
        eyes_at, ew = (hip + 9.6, -5), 3
        moves(m, "NIGHT SHADE", "SHADOW BALL")
    # Glowing slanted eyes on the dark: no whites, just light.
    def eye_glow(a, fi):
        # A slanted eye of light: its inner corner lower than its outer, a brighter middle.
        inner = (a.lx - 0.5) * (1 if a.x < 0 else -1)
        if a.ly < 0.3 - inner * 0.7 or a.ly > 0.95:
            return None
        return (rgb("#FFFFFF") if abs(a.ly - 0.6) < 0.18 and abs(a.lx - 0.5) < 0.3 else rgb("#B8F4FF")), 255
    eg = Mat("#C8F0FF", lambda a: CLEAR, glow=eye_glow, frames=True, noise=0, edge=0)
    hd = bone_of(m, "head")
    ey, ez = eyes_at
    hd.cubes += [Cube([-0.6 - ew - 0.4, ey, ez - 0.12], [ew + 0.4, 1.6 + 0.4 * stage, 0], eg), Cube([0.6, ey, ez - 0.12], [ew + 0.4, 1.6 + 0.4 * stage, 0], eg)]
    m.bone("eyelid_right", "head", [0, ey, ez], [])
    fx_for(m, (0.8, 1.0, 1.2)[stage])
    if stage == 0:
        A, info = floater(name.lower(), m, k=0.7, bob=1.0, extras={"idle": {"ring": track(rotation=[12, "q.anim_time * 90", 8])},
                                                                   "battle": {"ring": track(rotation=[12, "q.anim_time * 160", 8])}})
    elif stage == 1:
        A, info = floater(name.lower(), m, k=0.9, bob=1.2, tentacles=[["tail", "tail2", "tail3"]],
                          extras={"idle": {"forearm_right": track(position=[0, wave(2.4, 0.8, 0), wave(2.4, 0.6, 90)]),
                                           "forearm_left": track(position=[0, wave(2.4, 0.8, 120), wave(2.4, 0.6, 210)])}})
    else:
        A, info = biped(name.lower(), m, walk=0.62, stride=22, k=1.1, slow=1.1, guard=40)
    write(sid, name, m, A, info, stands)


def wisplet():
    shadow("gastly", "WISPLET", 1.0, 0)


def shadewick():
    shadow("haunter", "SHADEWICK", 1.65, 1)


def gloomwarden():
    shadow("gengar", "GLOOMWARDEN", 1.95, 2)


def basalt_mat(base, light, dark, seam):
    """Columnar basalt: dark stone in long facets, lighter on the top faces, fine cracks across, pale seams at the edges."""
    B, Lt, Dk, Sm = rgb(base), rgb(light), rgb(dark), rgb(seam)

    def f(at):
        c = mix(Dk, B, 0.45 + 0.55 * at.ly) if at.face != "top" else mix(B, Lt, 0.5)
        if hash01("crk", round(at.z * 1.5), round(at.x + at.y)) < 0.07:
            c = mix(c, Dk, 0.6)
        if at.fine_edge:
            c = mix(c, Sm, 0.5)
        return c
    return Mat(base, f, noise=0.025)


def hex_drum(x, y, z, w, L, mat):
    """A hexagonal column lying along z (three boxes turned 60 degrees apart about z)."""
    return [Cube([x - w / 2, y - w * 0.29, z], [n(w), n(w * 0.58), n(L)], mat, [0, 0, a], [x, y, z]) for a in (0, 60, 120)]


def basalwyrm():
    """A great wyrm of columnar basalt: its body a chain of hexagonal stone columns, a ridge spike on each, a heavy horned
    head with a jutting jaw of stone teeth and glowing amber eyes; it rears up high and slithers in waves."""
    m = Model()
    s = 2.0
    stone = basalt_mat("#4E4E58", "#7A7A86", "#2A2A32", "#8A8A96")
    spikem = taper_mat("#3A3A44", "#A8A8B4")
    tooth = Mat("#C8C4BC", noise=0.02, edge=0)
    glow_e = Mat("#FFA83A", glow=lambda a: rgb("#FFC860"), noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 3 * s, -2 * s], hex_drum(0, 3 * s, -3 * s, 6.4 * s, 4 * s, stone))
    segs, prev, z = [], "body", 1.0
    nseg = 10
    for i in range(nseg):
        t = i / (nseg - 1)
        w = (6.2 - 3.2 * t) * s
        nm = f"seg{i + 1}"
        cubes = hex_drum(0, w / 2 + 0.2, (z - 0.3) * s, w, 3.0 * s, stone) + spike(0, w * 0.95, (z + 1.4) * s, (2.4 - 1.2 * t) * s, [55, 0, 0], spikem, 2 * s)
        if i == nseg - 1:
            cubes += spike(0, w / 2, (z + 2.6) * s, 4 * s, [0, 0, 0], spikem, 2.4 * s)
        m.bone(nm, prev, [0, w / 2 + 0.2, z * s], cubes)
        segs.append(nm)
        prev, z = nm, z + 3.0
    m.bone("neck", "body", [0, 3 * s, -3 * s], hex_drum(0, 3 * s, -7 * s, 5.6 * s, 4.4 * s, stone), rotation=[-62, 0, 0])
    m.bone("neck2", "neck", [0, 3 * s, -7 * s], hex_drum(0, 3 * s, -11 * s, 5 * s, 4.4 * s, stone), rotation=[-14, 0, 0])
    hz = -11
    m.bone("head", "neck2", [0, 3 * s, hz * s], face_blob([-3.4 * s, 1.4 * s, (hz - 6) * s], [n(6.8 * s), n(5 * s), n(6.4 * s)], stone) +
           [Cube([-2.6 * s, 1.0 * s, (hz - 8.2) * s], [n(5.2 * s), n(3 * s), n(2.4 * s)], stone)] +
           spike(-2.6 * s, 5.6 * s, (hz - 1) * s, 5 * s, [30, 20, 0], spikem, 2 * s) + spike(2.6 * s, 5.6 * s, (hz - 1) * s, 5 * s, [30, -20, 0], spikem, 2 * s) +
           spike(0, 6.2 * s, (hz - 3) * s, 3 * s, [60, 0, 0], spikem, 2 * s), rotation=[76, 0, 0])
    m.bone("jaw", "head", [0, 1.6 * s, (hz - 2) * s], [Cube([-2.8 * s, 0.4 * s, (hz - 8) * s], [n(5.6 * s), n(1.4 * s), n(6 * s)], stone)] +
           [Cube([(-2.4 + i * 1.2) * s, 1.6 * s, (hz - 7.8) * s], [n(0.8 * s), n(1.2 * s), 1], tooth) for i in range(5)])
    hd = bone_of(m, "head")
    hd.cubes += [Cube([-2.8 * s, 4.0 * s, (hz - 6.05) * s], [n(1.8 * s), n(1 * s), 0], glow_e), Cube([1.0 * s, 4.0 * s, (hz - 6.05) * s], [n(1.8 * s), n(1 * s), 0], glow_e)]
    m.bone("eyelid_right", "head", [0, 4 * s, (hz - 6) * s], [])
    fx_for(m, 1.6)
    moves(m, "SLAM", "ROCK THROW")
    coil = [34, 36, 36, 36, 36, 34, 32, 30, 26, 20]
    A, info = serpent("basalwyrm", m, segs, k=1.4, slow=1.4, coil=coil)
    write("onix", "BASALWYRM", m, A, info, 2.3)


def koala(sid, name, stands, big):
    """A dream-eating koala. The first is plump and grey, great round fluffy ears, a big glossy black nose, heavy sleepy
    eyelids, short arms with curved claws. Grown, tall and long-armed, a thick white ruff round its neck, and in one hand
    a thread with a glowing crescent moon swinging on it."""
    m = Model()
    s = 1.0 if not big else 1.45
    S = lambda v: [x * s for x in v]
    FUR, FUR_L, FUR_D, BELLY = ("#9C9CA8", "#C8C8D4", "#6A6A78", "#E8E2D8") if not big else ("#8A8494", "#B8B2C4", "#5A5468", "#E8E0E0")
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, speck=0.6), noise=0.03)
    face = Mat(FUR, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.03)
    bellym = Mat(BELLY, fur(BELLY, "#FFFFFF", "#C8C0B4", speck=0.3), noise=0.03)
    ear = ear_mat(FUR, "#F2F0F0", "round", rim="#FFFFFF")
    nose = Mat("#1A181E", lambda a: rgb("#4A4652") if a.face == "top" or (a.face == "front" and a.ly > 0.7 and a.lx < 0.4) else None, noise=0.01, edge=0)
    jaw_m = Mat(BELLY, open_mouth("#8A3A4A", "#D8C8C8"), noise=0.02)
    claw = Mat("#2A2420", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    hip = 3.5 if not big else 5
    bh = 7 if not big else 9
    m.bone("body", "root", S([0, hip + 2, 0]), blob(S([-4, hip - 1, -3.5]), S([8, bh, 7]), furm, b=2) + [Cube(S([-3, hip, -3.9]), [n(6 * s), n((bh - 2) * s), 1], bellym)])
    hy = hip + bh - 1.5
    m.bone("head", "body", S([0, hy, -0.5]), face_blob(S([-4, hy, -4.5]), S([8, 6.5, 7]), face) +
           [Cube(S([-1.2, hy + 1, -5.6]), [n(2.4 * s), n(3 * s), n(1.6 * s)], nose)])
    m.bone("jaw", "head", S([0, hy + 0.8, -4]), [Cube(S([-1.2, hy, -4.8]), [n(2.4 * s), 1, n(1.4 * s)], jaw_m)])
    m.eyes("head", x_inner=1.5 * s, y=(hy + 3.6) * s, z=-4.5 * s, w=2, h=2, iris="#2A1A20", pupil="#08060A", skin=FUR, iris_light="#6A5A60",
           grid=["DDDD", "DDDD", "LLLL", "PPWP"] if not big else ["DDDD", "LLLL", "PPWP", "KPPK"])
    m.pair("ear_right", "head", S([-3.6, hy + 5.4, -1]), [Cube(S([-8.6, hy + 2.6, -1]), [n(6 * s), n(6 * s), 0], ear)], rotation=[0, 15, 10])
    ua, fa = (2.5, 2.5) if not big else (4.5, 4.5)
    upright_arms(m, 4.6 * s, (hip + bh - 2.5) * s, -0.5 * s, ua * s, fa * s, n(2 * s), furm, furm, hw=n(2.6 * s), hh=n(1.4 * s), claws=2,
                 rest=(-10, 0, 14), fore_rest=(-24, 0, 0))
    upright_legs(m, 2.2 * s, hip * s, 0.3 * s, n(2.4 * s), furm, furm, n(3 * s), toe_m=claw, ntoes=3, thigh_w=n(3.2 * s))
    if big:
        ruff = shag_mat("#FFFFFF", "#FFFFFF", "#D8D4DC")
        bd = bone_of(m, "body")
        for a in (-70, -35, 0, 35, 70):
            r = math.radians(a)
            cx, cz = math.sin(r) * 4.2 * s, (-3.6 + (1 - math.cos(r)) * 2.4) * s
            bd.cubes.append(Cube([cx - 2 * s, (hy - 4) * s, cz], [n(4 * s), n(4.4 * s), 0], ruff, [10, a, 0], [cx, hy * s, cz]))
        moon = Mat("#FFD860", lambda a: CLEAR if not (math.hypot(a.lx - 0.5, a.ly - 0.5) < 0.48 and math.hypot(a.lx - 0.68, a.ly - 0.58) > 0.36) else None,
                   glow=lambda a: rgb("#FFE890") if math.hypot(a.lx - 0.5, a.ly - 0.5) < 0.48 and math.hypot(a.lx - 0.68, a.ly - 0.58) > 0.36 else None,
                   noise=0, edge=0, flat=True)
        thread = Mat("#E8E0F0", noise=0, edge=0)
        hand_y = (hip + bh - 2.5 - ua - fa - 1.4) * s
        m.bone("pendulum", "forearm_right", [-4.6 * s, hand_y, -1.6 * s], [Cube([-4.6 * s, hand_y - 6 * s, -1.6 * s], [0, n(6 * s), 0.01], thread),
                                                                           Cube([-4.6 * s - 1.8 * s, hand_y - 9.4 * s, -1.6 * s], [n(3.6 * s), n(3.6 * s), 0], moon)])
        moves(m, "HEADBUTT", "PSYCHIC")
        ex = {"idle": {"pendulum": track(rotation=[0, 0, wave(1.6, 24)])}, "battle": {"pendulum": track(rotation=[0, 0, wave(1.2, 30)])}}
    else:
        moves(m, "POUND", "HYPNOSIS")
        ex = {"idle": {"head": track(rotation=[wave(3.2, 5, 0, 6), wave(6, 6), 0])}}
    fx_for(m, 0.8 * s)
    A, info = biped(name.lower(), m, walk=0.7 if not big else 0.8, stride=22, k=0.8 * s, slow=1.2, guard=20, extras=ex)
    write(sid, name, m, A, info, stands)


def dozala():
    koala("drowzee", "DOZALA", 1.15, False)


def hypnala():
    koala("hypno", "HYPNALA", 1.95, True)


def fiddler(sid, name, stands, big):
    """A fiddler crab with one enormous claw. The first small and red-orange, eyes up on tall stalks, a little claw on
    one side and a great cream-white claw on the other. Grown, deep red and armoured, spikes along the front of its
    shell like a crown, and its huge claw ridged with spikes and toothed along its grip."""
    m = Model()
    s = 1.0 if not big else 1.7
    S = lambda v: [x * s for x in v]
    CR, CRL, CRD = ("#E8642A", "#FF9A5A", "#A83A14") if not big else ("#C8341E", "#F0684A", "#7A1A0E")
    shell = Mat(CR, lambda a: mix(CRD, CR, 0.4 + 0.6 * a.ly) if a.face != "top" else (rgb(CRL) if hash01("sp", round(a.x * 2), round(a.z * 2)) < 0.12 else mix(CR, CRL, 0.4)), noise=0.02)
    legm = Mat(CR, lambda a: mix(CRD, CR, a.ly), noise=0.02, edge=0)
    claw_m = Mat("#F4E8D0", lambda a: mix("#C8B898", "#FFF8EC", a.ly) if a.face != "bottom" else rgb("#B8A888"), noise=0.02)
    tip = sharp_claw_mat("#5A2A14", "#F0E0C8")
    stalk = Mat(CRD, noise=0.01, edge=0)
    eyeb = Mat("#141014", lambda a: rgb("#FFFFFF") if a.face == "front" and a.lx > 0.5 and a.ly > 0.5 else None, noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 3.5, 0]), blob(S([-4.4, 2, -3.2]), S([8.8, 3, 6.4]), shell, b=1))
    if big:
        bone_of(m, "body").cubes += sum([spike(x * s, 4.6 * s, -3.2 * s, 2 * s, [40, 180, 0], taper_mat(CRD, "#FFD0C0"), 1.4) for x in (-3, -1.5, 0, 1.5, 3)], [])
    m.bone("head", "body", S([0, 4, -3]), [Cube(S([-2.4, 2.6, -3.8]), [n(4.8 * s), n(2 * s), 1], shell)])
    m.bone("jaw", "head", S([0, 2.8, -3.6]), [Cube(S([-1, 2.4, -4.1]), [n(2 * s), 1, 1], Mat(CRD, noise=0.02))])
    for sx, side in ((-1, "right"), (1, "left")):
        m.bone(f"antenna_{side}", "head", S([sx * 1.4, 4.4, -3.2]), [Cube(S([sx * 1.4 - 0.4, 4.4, -3.6]), [1, n(3.4 * s), 1], stalk),
                                                                      Cube(S([sx * 1.4 - 0.6, 7.6, -3.8]), [n(1.2 * s), n(1.2 * s), n(1.2 * s)], eyeb)], rotation=[-10, 0, sx * -8])
    # The great claw (on its right) and the little one: each an arm and a pincer that opens.
    L = (5 if not big else 6) * s
    m.bone("arm_right", "body", S([-3.8, 3.4, -2.6]), [Cube(S([-6, 2.8, -4.4]), [n(2.4 * s), n(1.6 * s), n(2.6 * s)], shell)], rotation=[0, 24, 0])
    # The palm, then the fixed finger running on from its top, the moving finger hinged below it, a gap between them,
    # teeth along the inside of the grip.
    P = L * 0.5
    z0 = -4.4 * s
    m.bone("forearm_right", "arm_right", S([-5.2, 3.4, -4.2]), [Cube([-8 * s, 1.4 * s, z0 - P], [n(4.4 * s), n(4 * s), n(P)], claw_m),
                                                                Cube([-7.6 * s, 3.4 * s, z0 - P - L * 0.75], [n(3.6 * s), n(1.8 * s), n(L * 0.75)], claw_m),
                                                                Cube([-7.4 * s, 3.4 * s, z0 - P - L * 0.95], [n(3.2 * s), n(1.4 * s), n(L * 0.25)], tip, [-12, 0, 0], [0, 3.4 * s, z0 - P - L * 0.7])] +
           [Cube([(-7.2 + i * 1.2) * s, 3.0 * s, z0 - P - L * 0.6], [1, 1, n(L * 0.5)], Mat("#F8F0E0", noise=0, edge=0)) for i in range(3)] +
           ([Cube([-8.2 * s, 5.4 * s, z0 - P + (i + 0.5) * P / 3], [n(1 * s), 1, 1], taper_mat(CRD, "#FFF0E0")) for i in range(3)] if big else []))
    m.bone("pincer_right", "forearm_right", S([-6, 2.0, -4.4 - P / s]), [Cube([-7.4 * s, 1.0 * s, z0 - P - L * 0.7], [n(3.2 * s), n(1.4 * s), n(L * 0.7)], claw_m),
                                                                        Cube([-7.2 * s, 1.0 * s, z0 - P - L * 0.9], [n(2.8 * s), n(1.2 * s), n(L * 0.22)], tip, [12, 0, 0], [0, 1.0 * s, z0 - P - L * 0.68])],
           rotation=[16, 0, 0])
    m.bone("arm_left", "body", S([3.6, 3.4, -2.6]), [Cube(S([3.4, 2.8, -4]), [n(1.4 * s), n(1.2 * s), n(1.8 * s)], shell)], rotation=[0, -20, 0])
    m.bone("forearm_left", "arm_left", S([4, 3.4, -3.8]), [Cube(S([3.6, 2.6, -6.2]), [n(1.6 * s), n(1.4 * s), n(2.4 * s)], shell),
                                                        Cube(S([3.8, 2.2, -6.4]), [n(1.2 * s), 1, n(2 * s)], shell)])
    for nm, zz in (("leg_front_right", -1.6), ("leg_mid_right", 0.4), ("leg_back_right", 2.2)):
        crab_leg(m, nm, 4 * s, 3 * s, zz * s, 3 * s, 3.8 * s, legm, tip)
    fx_for(m, 0.7 * s)
    moves(m, "VICEGRIP" if not big else "CRABHAMMER", "BUBBLE" if not big else "GUILLOTINE")
    W = 0.45 if not big else 0.6
    A, info = four_legs(name.lower(), m, walk=W, stride=22, k=0.5 * s, extras={
        "walk": {"leg_mid_right": track(rotation=[wave(W, 22, 90), 0, 0]), "leg_mid_left": track(rotation=[wave(W, -22, 90), 0, 0]),
                 "body": track(rotation=[0, 20, 0])},
        "idle": {"forearm_right": track(rotation=[wave(1.6, 10, 0, -6), 0, 0]), "pincer_right": track(rotation=[wave(0.8, 14, 0, 10), 0, 0])},
        "battle": {"forearm_right": track(rotation=[wave(0.8, 14, 0, -12), 0, 0]), "pincer_right": track(rotation=[wave(0.4, 18, 0, 12), 0, 0])}})
    write(sid, name, m, A, info, stands)


def pinchlet():
    fiddler("krabby", "PINCHLET", 0.5, False)


def fiddleking():
    fiddler("kingler", "FIDDLEKING", 2.3, True)


def bulb_creature(sid, name, stands, big):
    """A living light bulb that rolls about: a ball of pale glass with a glowing coiled filament inside, a brass screw cap
    with its threads, and an angry face on the glass. Grown, bigger and brighter, two filaments blazing, a wide toothy
    grin, sparks jumping off it."""
    m = Model()
    s = 1.0 if not big else 1.5
    S = lambda v: [x * s for x in v]

    def glass(at):
        c = mix("#B8D8E8", "#F0FAFF", at.ly)
        if at.face in ("front", "nx", "px", "top") and abs(at.lx - 0.3) < 0.08 and at.ly > 0.55:
            c = mix(c, "#FFFFFF", 0.7)
        return c

    def filament(at):
        if at.face not in ("front", "back", "nx", "px"):
            return None
        u = at.lx if at.face in ("front", "back") else at.lz
        for k, off in ((0, 0.0),) if not big else ((0, -0.12), (1, 0.12)):
            if abs(at.ly - 0.42 - off - 0.08 * math.sin(u * 30)) < 0.04 and 0.3 < u < 0.7:
                return rgb("#FFF0A0")
        if abs(u - 0.5) < 0.03 and at.ly < 0.42:
            return rgb("#E8D070")
        return None
    glassm = Mat("#D8EEF8", glass, glow=filament, noise=0.01, edge=0.04)
    brass = Mat("#C8A040", lambda a: rgb("#8A6A20") if int(a.y * 2) % 2 else (rgb("#F0D070") if a.face == "top" else None), noise=0.02)
    jaw_m = Mat("#2A3A4A", open_mouth("#3A1A24", "#2A3A4A"), noise=0.01)
    tooth = Mat("#FFFFFF", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 6, 0]), blob(S([-4.5, 2.5, -4.5]), S([9, 9, 9]), glassm, b=2) +
           [Cube(S([-2.6, 0.4, -2.6]), [n(5.2 * s), n(2.6 * s), n(5.2 * s)], brass), Cube(S([-1.2, 0, -1.2]), [n(2.4 * s), 1, n(2.4 * s)], Mat("#5A4A2A", noise=0.01))])
    m.bone("head", "body", S([0, 8, -4.5]), [Cube(S([-3.5, 5.5, -4.6]), [n(7 * s), n(5 * s), 0], Mat("#D8EEF8", lambda a: CLEAR, noise=0, edge=0, flat=True))])
    m.eyes("head", x_inner=0.5 * s, y=7.4 * s, z=-4.6 * s, w=2, h=2, iris="#2A3A4A", pupil="#0A1018", skin="#C8E4F0", iris_light="#6A8AA8",
           grid=["LL..", "SLLL", "PPPW", "SPPS"] if not big else ["L...", "SLLL", "SPPW", "SPPS"])
    m.brows("head", [Cube(S([-3.4, 9.2, -4.8]), [n(3 * s), 1, 0.5], Mat("#2A3A4A", noise=0, edge=0), [0, 0, 22], S([-0.4, 9.6, -4.7]))])
    jw = 3 if not big else 5.4
    m.bone("jaw", "head", S([0, 6, -4.6]), [Cube(S([-jw / 2, 5.4, -4.8]), [n(jw * s), n((1 if not big else 1.6) * s), 1], jaw_m)] +
           ([Cube(S([-jw / 2 + 0.3 + i * 1.1, 6.0, -4.9]), [1, n(0.8 * s), 0], tooth) for i in range(5)] if big else []))
    if big:
        spark = Mat("#FFF27A", lambda a: CLEAR, glow=lambda a, fi: (rgb("#FFFFC0"), 255) if abs(a.ly - 0.5 - 0.3 * math.sin(a.lx * 9 + fi)) < 0.12 else None,
                    frames=True, noise=0, edge=0)
        bone_of(m, "body").cubes += [Cube(S([sx * 4.8, 6, -1]), [0, n(4 * s), n(3 * s)], spark) for sx in (-1, 1)] + [Cube(S([-1.5, 11.8, -1]), [n(3 * s), n(3 * s), 0], spark)]
    fx_for(m, 0.8 * s)
    moves(m, "TACKLE" if not big else "ROLLOUT", "SONICBOOM" if not big else "EXPLOSION")
    A, info = hopper(name.lower(), m, hop=0.45, height=1.0 * s, k=0.7 * s,
                     extras={"walk": {"body": track(rotation=["q.anim_time * -400", 0, 0])},
                             "idle": {"body": track(rotation=[0, 0, wave(2.4, 6)])}})
    write(sid, name, m, A, info, stands)


def bulblet():
    bulb_creature("voltorb", "BULBLET", 0.65, False)


def surgebulb():
    bulb_creature("electrode", "SURGEBULB", 1.15, True)


def acorn_paint(nut, nut_l, nut_d):
    """An acorn: glossy, a pale streak down it, darker towards its point."""
    def f(at):
        c = mix(nut_d, nut, 0.3 + 0.7 * at.ly)
        if abs(at.lx - 0.3) < 0.08 and at.face in ("front", "nx", "px"):
            c = mix(c, nut_l, 0.6)
        return c
    return f


def cap_mat(base, dark):
    """An acorn's cap: a criss-cross of little scales."""
    return Mat(base, lambda a: rgb(dark) if (round(a.x * 2) + round(a.z * 2) + round(a.y * 2)) % 2 == 0 else mix(base, "#FFFFFF", 0.1), noise=0.02)


def acorn(m, bone, parent, x, y, z, s, nutm, capm, stem, face=True, mood="calm", rot=None, sprout=None):
    cubes = blob([x - 2 * s, y - 2.4 * s, z - 2 * s], [n(4 * s), n(4.4 * s), n(4 * s)], nutm, b=1) + [Cube([x - 0.5 * s, y - 3.1 * s, z - 0.5 * s], [1, n(0.8 * s), 1], nutm)]
    cubes += blob([x - 2.4 * s, y + 1.6 * s, z - 2.4 * s], [n(4.8 * s), n(1.8 * s), n(4.8 * s)], capm) + [Cube([x - 0.4 * s, y + 3.2 * s, z - 0.4 * s], [1, n(1.2 * s), 1], stem)]
    if sprout is not None:
        cubes += [Cube([x - 1.2 * s, y + 3.8 * s, z], [n(2.4 * s), n(2.6 * s), 0], sprout, [0, 30, -20], [x, y + 3.8 * s, z])]
    m.bone(bone, parent, [x, y, z], cubes, rotation=rot)
    if face:
        dark = Mat("#2A1A10", noise=0, edge=0)
        fz = z - 2 * s - 0.02
        eyes = [Cube([x - 1.4 * s, y - 0.2 * s, fz], [1, 1, 0], dark), Cube([x + 0.4 * s, y - 0.2 * s, fz], [1, 1, 0], dark)]
        if mood == "cross":
            eyes += [Cube([x - 1.6 * s, y + 0.9 * s, fz], [n(1.2 * s), 0.5, 0], dark, [0, 0, 20], [x - 1, y + 1, fz]),
                     Cube([x + 0.4 * s, y + 0.9 * s, fz], [n(1.2 * s), 0.5, 0], dark, [0, 0, -20], [x + 1, y + 1, fz])]
        mouth = Cube([x - 0.6 * s, y - 1.4 * s, fz], [n(1.2 * s), 0.5, 0], dark)
        bone_of(m, bone).cubes += eyes + [mouth]


def podlings():
    """Six little acorns that go everywhere together, bunched up: glossy tawny nuts in scaly caps, each with its own face
    (one cross, one sleepy, one with a sprout pushing up out of its cap), hopping along as one."""
    m = Model()
    nutm = Mat("#C8903A", acorn_paint("#C8903A", "#F0C878", "#7A5020"), noise=0.02)
    capm = cap_mat("#7A5A30", "#4A3418")
    stem = Mat("#5A4020", noise=0.01, edge=0)
    sprout = leaf_mat("#8AD060", "#3A8A2A", "#C8F0A0")
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 2.5, 0], [Cube([-1, 0, -1], [2, 1, 2], nutm)])
    acorn(m, "head", "body", 0, 7.4, -1, 1.0, nutm, capm, stem)
    spots = [(-4, 3, -1.5, "cross", None), (4, 3, -1.5, "calm", sprout), (-2.2, 3.2, 3, "calm", None), (2.4, 3.4, 3.2, "cross", None), (0, 3, -4.4, "calm", None)]
    names = []
    for i, (x, y, z, mood, sp) in enumerate(spots):
        nm = f"pod{i + 2}"
        acorn(m, nm, "body", x, y, z, 0.95, nutm, capm, stem, mood=mood, rot=[0, (i - 2) * 12, 0], sprout=sp)
        names.append(nm)
    m.bone("jaw", "head", [0, 6.2, -3], [])
    fx_for(m, 0.7)
    moves(m, "BARRAGE", "HYPNOSIS")
    ex = {"idle": {nm: track(position=[0, wave(1.2 + i * 0.13, 0.4, i * 60), 0], rotation=[0, wave(2.4, 8, i * 50), wave(1.8, 5, i * 70)]) for i, nm in enumerate(names)},
          "walk": {nm: track(position=[0, bounce(0.45, 1.2, i * 30), 0]) for i, nm in enumerate(names)}}
    A, info = hopper("podlings", m, hop=0.45, height=1.4, k=0.6, extras=ex)
    write("exeggcute", "PODLINGS", m, A, info, 0.45)


def oaktrio():
    """A walking oak: a thick trunk of furrowed bark on two root legs, two branch arms with leaves at their tips, a crown of
    oak leaves, and three acorn heads, each with its own face, peering out of the leaves."""
    m = Model()
    bark = Mat("#6A4A2E", lambda a: mix("#4A3018", "#8A6A48", 0.3 + 0.7 * abs(math.sin(a.x * 2.2 + a.z * 1.3))) if a.face != "top" else rgb("#A8865E"), noise=0.03)
    oak = leaf_mat("#6AB040", "#2E6A24", "#B8E090")
    nutm = Mat("#C8903A", acorn_paint("#C8903A", "#F0C878", "#7A5020"), noise=0.02)
    capm = cap_mat("#7A5A30", "#4A3418")
    stem = Mat("#5A4020", noise=0.01, edge=0)
    rootm = Mat("#5A3E24", noise=0.02)
    m.bone("root", None, [0, 0, 0])
    hip = 7
    m.bone("body", "root", [0, hip + 4, 0], blob([-4.5, hip - 1, -4], [9, 17, 8], bark, b=2))
    bd = bone_of(m, "body")
    lobed = Mat("#5AA03A", lambda a: CLEAR if (lambda u, v: abs(u) > 0.5 * math.sin(math.pi * min(1.0, v)) ** 0.6 * (0.78 + 0.22 * math.cos(v * math.pi * 7)))(a.lx - 0.5, a.lz)
                else (rgb("#C8E890") if abs(a.lx - 0.5) < 0.05 else mix("#3A7A28", "#7AC050", hash01("lf", round(a.x), round(a.z)))), noise=0.02, edge=0, flat=True)
    for ring, (rr, yy, tilt, cnt) in enumerate(((4, 21, 70, 6), (7.5, 18.5, 40, 10), (8.5, 15.5, 10, 12), (5, 23.5, 85, 5))):
        for i in range(cnt):
            a = (i + ring * 0.5) * 2 * math.pi / cnt
            x, z = math.sin(a) * rr, math.cos(a) * rr * 0.85
            bd.cubes.append(Cube([x - 3.5, hip + yy, z], [7, 0, 7.5], lobed, [tilt, -math.degrees(a), 0], [x, hip + yy, z]))
    acorn(m, "head", "body", 0, hip + 21, -3.5, 1.4, nutm, capm, stem)
    acorn(m, "head2", "body", -5, hip + 18.5, -2, 1.25, nutm, capm, stem, mood="cross", rot=[0, 0, 14])
    acorn(m, "head3", "body", 5, hip + 18.5, -2, 1.25, nutm, capm, stem, mood="calm", rot=[0, 0, -14])
    m.bone("jaw", "head", [0, hip + 19.4, -6], [])
    upright_arms(m, 5.2, hip + 12, -0.5, 5, 5, 2.4, bark, bark, hw=3, hh=1.4, fingers=3, finger_m=bark, finger_len=2.4, rest=(-10, 0, 30), fore_rest=(-30, 0, -10))
    for side in ("forearm_right", "forearm_left"):
        fb = bone_of(m, side)
        hx = -5.2 if side.endswith("right") else 5.2
        fb.cubes += [Cube([hx - 2.5, hip - 1.5, -0.5], [5, 0, 6], oak, [60, 0, 0], [hx, hip + 1, -0.5])]
    upright_legs(m, 2.6, hip, 0, 4, bark, rootm, 6, toe_m=rootm, ntoes=4, thigh_w=5, toe_len=3)
    fx_for(m, 1.4)
    moves(m, "STOMP", "EGG BOMB")
    ex = {"idle": {"head2": track(rotation=[wave(2.6, 6), wave(3.4, 12, 60), 0]), "head3": track(rotation=[wave(2.2, 6, 120), wave(3, 12, 200), 0])}}
    A, info = biped("oaktrio", m, walk=0.85, stride=20, k=1.4, slow=1.3, guard=20, extras=ex)
    write("exeggutor", "OAKTRIO", m, A, info, 3.4)


def jackal(sid, name, stands, big):
    """A desert jackal that wears a ram's bleached skull for a helmet, its own tall ears poking up through it, its eyes
    looking out of the skull's sockets, and that carries a bone for a club. Grown, lean and darker, the skull's horns
    curled in full spirals, a long thigh bone for a staff, a bushy black-tipped tail in joints."""
    m = Model()
    s = 1.0 if not big else 1.45
    S = lambda v: [x * s for x in v]
    FUR, FUR_L, FUR_D, BELLY = ("#C8A06A", "#E8C890", "#8A6A3A", "#F0E0C0") if not big else ("#A87A4A", "#C89A6A", "#5A3E22", "#E0C8A0")
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=BELLY, below=None), noise=0.02)
    bone_m = Mat("#EEE6D4", lambda a: mix("#C8BCA0", "#FFF8EA", a.ly) if a.face != "bottom" else rgb("#B8AC90"), noise=0.02)
    socket = Mat("#2A2018", noise=0, edge=0)
    ear = ear_mat(FUR, "#F0C8B0", "fennec", tip="#2A1A10")
    jaw_m = Mat(BELLY, open_mouth("#8A2A2A", "#E0D0B0"), noise=0.02)
    m.bone("root", None, [0, 0, 0])
    hip = 4 if not big else 5.5
    bh = 6 if not big else 8
    m.bone("body", "root", S([0, hip + 2, 0]), blob(S([-3, hip - 1, -2.6]), S([6, bh, 5.2]), furm) + [Cube(S([-2, hip, -3]), [n(4 * s), n((bh - 2) * s), 1], Mat(BELLY, noise=0.02))])
    hy = hip + bh - 1.5
    # The head: the jackal's snout out the front; over it the ram skull, horns curling round each side.
    m.bone("head", "body", S([0, hy, -0.5]), face_blob(S([-2.8, hy, -3.4]), S([5.6, 4.6, 5]), furm) +
           [Cube(S([-1.3, hy, -6]), [n(2.6 * s), n(2 * s), n(2.8 * s)], furm), Cube(S([-0.6, hy + 1.3, -6.3]), [n(1.2 * s), 1, 1], socket)] +
           blob(S([-3.2, hy + 2.6, -4.2]), S([6.4, 3.4, 6]), bone_m) + [Cube(S([-2.6, hy + 2.4, -4.4]), [n(5.2 * s), n(1 * s), n(1.4 * s)], bone_m)])
    hd = bone_of(m, "head")
    nh = 3 if not big else 4
    for sx in (-1, 1):
        prev = None
        for i in range(nh):
            a = i * 70
            r = 1.6 * s
            cx = sx * (3.2 * s + 0.6 * s * i)
            cy = (hy + 4.5) * s - math.sin(math.radians(a)) * r - i * 0.4 * s
            cz = -1 * s + math.cos(math.radians(a)) * r
            hd.cubes.append(Cube([cx - 0.9 * s, cy - 0.9 * s, cz - 1.2 * s], [n(1.8 * s), n(1.8 * s), n(2.4 * s)], bone_m, [a, 0, 0], [cx, cy, cz]))
    m.eyes("head", x_inner=0.7 * s, y=(hy + 2.9) * s, z=-4.2 * s, w=2, h=1, iris="#E8A030", pupil="#100804", skin="#2A2018", grid=["IPPI", "SIIS"])
    m.bone("jaw", "head", S([0, hy + 0.4, -3.4]), [Cube(S([-1.1, hy - 0.4, -5.8]), [n(2.2 * s), 1, n(2.4 * s)], jaw_m)])
    m.pair("ear_right", "head", S([-1.8, hy + 5.4, -1]), [Cube(S([-3.6, hy + 5.0, -1]), [n(3.2 * s), n(4.6 * s), 0], ear)], rotation=[-6, 10, -12])
    ua, fa = (2.6, 2.6) if not big else (3.4, 3.4)
    bottom = upright_arms(m, 3.6 * s, (hip + bh - 2) * s, -0.5 * s, ua * s, fa * s, n(1.6 * s), furm, furm, hw=n(2 * s), hh=n(1.4 * s), claws=0,
                          rest=(-30, 0, 10), fore_rest=(-40, 0, 0))
    fb = bone_of(m, "forearm_right")
    hx = -3.6 * s
    Lb = (7 if not big else 14) * s
    fb.cubes += [Cube([hx - 0.6 * s, bottom - Lb * 0.35, -1.6 * s], [n(1.2 * s), n(Lb), n(1.2 * s)], bone_m, [70, 0, 0], [hx, bottom, -1 * s]),
                 Cube([hx - 1.2 * s, bottom - Lb * 0.35 - 1.2 * s, -2.2 * s], [n(2.4 * s), n(1.8 * s), n(2.4 * s)], bone_m, [70, 0, 0], [hx, bottom, -1 * s]),
                 Cube([hx - 1.2 * s, bottom + Lb * 0.65 - 0.6 * s, -2.2 * s], [n(2.4 * s), n(1.8 * s), n(2.4 * s)], bone_m, [70, 0, 0], [hx, bottom, -1 * s])]
    upright_legs(m, 1.8 * s, hip * s, 0.3 * s, n(2 * s), furm, furm, n(3 * s), toe_m=Mat("#2A1A10", noise=0, edge=0), ntoes=3, thigh_w=n(2.6 * s), knee_at=0.45)
    segs = [((2 - i * 0.2) * s, (2 - i * 0.2) * s, 2.2 * s) for i in range(3 if not big else 4)]
    tailm = Mat(FUR, fur(FUR, FUR_L, FUR_D), noise=0.03)
    names, zt = tail_bones(m, "body", (hip + 1.5) * s, 2.4 * s, segs, tailm, (-30, 10, 8, 8)[:len(segs)])
    bone_of(m, names[-1]).cubes.append(Cube([-0.9 * s, bone_of(m, names[-1]).pivot[1] - 0.9 * s, zt - 0.6], [n(1.8 * s), n(1.8 * s), n(1.2 * s)], Mat("#2A1A10", noise=0.02)))
    fx_for(m, 0.8 * s)
    moves(m, "BONE CLUB" if not big else "BONEMERANG", "BONEMERANG" if not big else "BONE RUSH")
    A, info = biped(name.lower(), m, walk=0.55, stride=28, k=0.8 * s, guard=30)
    write(sid, name, m, A, info, stands)


def skullkit():
    jackal("cubone", "SKULLKIT", 0.7, False)


def bonejackal():
    jackal("marowak", "BONEJACKAL", 1.35, True)


def kickaroo():
    """A kickboxing kangaroo: lean and tawny, long ears, a red headband, short arms in wraps, a thick tail in joints to lean
    on, and great long springing hind legs, their long feet bound in wraps, made for kicking."""
    m = Model()
    FUR, FUR_L, FUR_D, BELLY = "#C88A52", "#E8B07A", "#8A5A30", "#F2DCC0"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, belly=None), noise=0.02)
    bellym = Mat(BELLY, noise=0.02)
    face = Mat(FUR, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.02)
    wrap = Mat("#F0ECE0", lambda a: rgb("#C8C0B0") if a.row % 2 == 0 and a.face != "bottom" else None, noise=0.02)
    band = Mat("#D8302A", lambda a: rgb("#A81E1A") if a.ly < 0.3 else None, noise=0.02, edge=0)
    ear = ear_mat(FUR, "#F0C0A8", "rabbit")
    jaw_m = Mat(BELLY, open_mouth("#8A2A2A", "#E0D0B0"), noise=0.02)
    m.bone("root", None, [0, 0, 0])
    hip = 10
    m.bone("body", "root", [0, hip + 3, 0], blob([-3.4, hip - 1.5, -3], [6.8, 11, 6], furm, b=2) + [Cube([-2.4, hip, -3.4], [4.8, 8, 1], bellym)])
    hy = hip + 9
    m.bone("neck", "body", [0, hy, -0.5], [Cube([-1.6, hy - 0.5, -2.2], [3.2, 3, 3], furm)], rotation=[-8, 0, 0])
    m.bone("head", "neck", [0, hy + 2.2, -1], face_blob([-2.4, hy + 1.6, -4.2], [4.8, 4, 4.6], face) + [Cube([-1.3, hy + 1.6, -6.6], [2.6, 2.4, 2.6], face),
                                                                                                    Cube([-0.6, hy + 3.2, -6.9], [1.2, 1, 1], Mat("#2A1A14", noise=0, edge=0)),
                                                                                                    Cube([-2.5, hy + 4.4, -4.4], [5, 1.2, 4.8], band)], rotation=[8, 0, 0])
    m.bone("jaw", "head", [0, hy + 2, -4], [Cube([-1.1, hy + 1, -6.4], [2.2, 1, 2.4], jaw_m)])
    m.eyes("head", x_inner=0.6, y=hy + 3, z=-4.2, w=2, h=2, iris="#3A2010", pupil="#0A0604", skin=FUR, iris_light="#8A5A30", grid=["DLLL", "PPWP", "PPPP", "SKKS"])
    m.pair("ear_right", "head", [-1.4, hy + 5.4, -2], [Cube([-3.2, hy + 5.2, -2], [3, 6, 0], ear)], rotation=[-10, 8, -14])
    upright_arms(m, 3.6, hy - 1, -1.4, 2.6, 2.6, 1.6, furm, wrap, hw=2.4, hh=2, rest=(-30, 0, 12), fore_rest=(-50, 0, 0))
    upright_legs(m, 2.6, hip, 0.5, 3, furm, wrap, 8, toe_m=Mat("#2A1A14", noise=0, edge=0), ntoes=3, thigh_w=4.4, knee_at=0.62, toe_len=1.5)
    tailm = Mat(FUR, fur(FUR, FUR_L, FUR_D), noise=0.03)
    tail_bones(m, "body", hip + 1, 2.4, [(3.6, 3.2, 3), (3, 2.6, 3), (2.4, 2, 3), (1.8, 1.6, 3)], tailm, (-34, -10, -6, 4))
    fx_for(m, 1.0)
    moves(m, "DOUBLE KICK", "HI JUMP KICK")
    A, info = biped("kickaroo", m, walk=0.5, stride=34, k=1.0, guard=46)
    write("hitmonlee", "KICKAROO", m, A, info, 1.7)


def smashrimp():
    """A mantis shrimp that boxes: it stands up on its walking legs, its armoured body banded in vivid green and teal,
    orange limbs, eyes on stalks banded like gemstones, a fan of a tail in rainbow colours, and two great club fists
    folded at its chest, red and hard as stone."""
    m = Model()
    G, G_L, G_D = "#2AA878", "#6AE0B0", "#14684A"

    def banded(at):
        k = int(math.floor(at.y / 2.2)) % 2
        c = mix(G_D, G, 0.4 + 0.6 * at.ly) if k == 0 else mix("#1A7A9A", "#4AC0E0", 0.3 + 0.5 * at.ly)
        if (at.y % 2.2) < 0.35:
            c = mix(c, "#FFF0A0", 0.6)
        return c
    shell = Mat(G, banded, noise=0.02)
    limb = Mat("#E8742A", lambda a: mix("#A84A18", "#FFA860", a.ly), noise=0.02)
    club = Mat("#D8302A", lambda a: rgb("#FF8A5A") if a.face == "front" and a.ly > 0.5 else (rgb("#8A1A14") if a.face == "bottom" else None), noise=0.02)
    stalk = Mat("#E8742A", noise=0.01, edge=0)
    eyeb = Mat("#3A8AF0", lambda a: rgb("#F0E060") if abs(a.ly - 0.5) < 0.17 else (rgb("#E8402A") if a.ly < 0.33 else None), noise=0, edge=0)
    fan = Mat("#E8402A", lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (0.5 + 0.5 * a.lz) else
              ramp(["#2AA878", "#3A8AF0", "#E8402A", "#F0E060"], abs(a.lx - 0.5) * 2), noise=0.01, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    hip = 6
    m.bone("body", "root", [0, hip + 4, 0], blob([-3.2, hip - 1, -2.6], [6.4, 6, 5.2], shell) + blob([-3, hip + 4.5, -2.4], [6, 4.5, 4.8], shell))
    hy = hip + 8.5
    m.bone("head", "body", [0, hy, -1], [Cube([-2.4, hy, -3.6], [4.8, 3, 4], shell), Cube([-1.6, hy + 0.6, -5], [3.2, 1.8, 1.6], shell)])
    m.bone("jaw", "head", [0, hy + 0.4, -4], [Cube([-1, hy - 0.2, -4.6], [2, 1, 1], limb)])
    for sx, side in ((-1, "right"), (1, "left")):
        m.bone(f"antenna_{side}", "head", [sx * 1.2, hy + 3, -2.6], [Cube([sx * 1.2 - 0.5, hy + 3, -3.1], [1, 3, 1], stalk),
                                                                     Cube([sx * 1.2 - 1.2, hy + 5.6, -3.8], [2.4, 2.6, 2.4], eyeb)], rotation=[-8, 0, sx * -14])
    upright_arms(m, 3.8, hy - 1, -2, 2.6, 3, 1.6, limb, club, hw=3.4, hh=3, rest=(-44, 0, 6), fore_rest=(-80, 0, 0))
    upright_legs(m, 1.8, hip, 0.5, 1.6, limb, limb, 3, ntoes=2, toe_m=Mat("#A84A18", noise=0, edge=0), knee_at=0.5)
    names, zt = tail_bones(m, "body", hip + 0.5, 2.4, [(4.8, 2.6, 2.4), (4, 2.2, 2.4), (3.2, 1.8, 2)], shell, (-50, -16, -10))
    bone_of(m, names[-1]).cubes.append(Cube([-4, bone_of(m, names[-1]).pivot[1], zt - 0.6], [8, 0, 5], fan))
    fx_for(m, 0.9)
    moves(m, "MACH PUNCH", "THUNDERPUNCH")
    A, info = biped("smashrimp", m, walk=0.45, stride=24, k=0.9, guard=36,
                    extras={"idle": {"antenna_right": track(rotation=[wave(1.4, 10), 0, wave(2.2, 8)]), "antenna_left": track(rotation=[wave(1.7, 10, 90), 0, wave(2.6, -8)])}})
    write("hitmonchan", "SMASHRIMP", m, A, info, 2.0)


def lashleon():
    """A chameleon with a tongue as long as it is: a tall narrow body in lime green with turquoise bands and a pink stripe,
    a ridged helmet of a head, turret eyes on the sides of its head each turning its own way, gripping feet, a tail curled
    in a tight spiral, and a long pink tongue it keeps rolled up in its mouth."""
    m = Model()
    s = 1.3
    S = lambda v: [x * s for x in v]
    L_, L_L, L_D = "#7AC83A", "#B8F070", "#3A7A1E"

    def skin_p(at):
        c = mix(L_D, L_, 0.4 + 0.6 * at.ly)
        if int(math.floor(at.z / 2.4)) % 2 == 0 and at.face in ("nx", "px") and at.ly > 0.3:
            c = mix(c, "#3AC8B8", 0.55)
        if at.face in ("nx", "px") and abs(at.ly - 0.25) < 0.08:
            return rgb("#F08AB0")
        if hash01("tub", round(at.x * 2), round(at.y * 2), round(at.z * 2)) < 0.12:
            c = mix(c, L_L, 0.4)
        return c
    skin = Mat(L_, skin_p, noise=0.02)
    head_m = Mat(L_, lambda a: rgb("#F8D8E0") if a.face == "bottom" else skin_p(a), noise=0.02)
    turret = Mat(L_, lambda a: mix(L_D, L_L, a.ly), noise=0.02)
    pupil = Mat("#100A08", lambda a: rgb("#F0D040") if a.fine_edge else None, noise=0, edge=0)
    tongue = Mat("#F06A9A", lambda a: rgb("#C8406A") if a.face == "bottom" else None, noise=0.02, edge=0)
    claw = Mat("#2A2A18", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    top = 5
    m.bone("body", "root", S([0, top + 3, 0]), blob(S([-2.4, top, -5]), S([4.8, 6.5, 10]), skin) +
           [Cube(S([-0.5, top + 6.4, -4]), [n(1 * s), n(1.2 * s), n(8 * s)], skin)])
    hy = top + 3
    m.bone("head", "body", S([0, hy + 2, -5]), face_blob(S([-2.4, hy, -10]), S([4.8, 4.2, 5.4]), head_m) +
           [Cube(S([-1.6, hy + 3.6, -8.6]), [n(3.2 * s), n(2.2 * s), n(5 * s)], head_m, [-24, 0, 0], S([0, hy + 4, -6]))])
    for sx, side in ((-1, "right"), (1, "left")):
        m.bone(f"eye_{side}", "head", S([sx * 2.4, hy + 2.8, -8]), [Cube(S([sx * 2.4 - 1.2 + (0.4 if sx < 0 else -0.4), hy + 1.6, -9.2]), [n(2.4 * s), n(2.4 * s), n(2.4 * s)], turret),
                                                                  Cube(S([sx * 3.25 - 0.5 + (0.5 if sx > 0 else -0.5) * 0.0, hy + 2.3, -8.6]), [1, 1, 1], pupil)])
    m.bone("eyelid_right", "head", [0, 0, 0], [])
    m.bone("jaw", "head", S([0, hy + 0.6, -6.5]), [Cube(S([-2, hy - 0.2, -10]), [n(4 * s), 1, n(4 * s)], Mat("#F8D8E0", open_mouth("#C8406A", "#E8B8C8"), noise=0.02))])
    m.bone("tongue", "jaw", S([0, hy + 0.4, -9.8]), [Cube(S([-0.6, hy + 0.2, -16]), [n(1.2 * s), n(0.8 * s), n(6.4 * s)], tongue),
                                                    Cube(S([-1.1, hy, -17.2]), [n(2.2 * s), n(1.4 * s), n(1.6 * s)], tongue)])
    stout_legs(m, 2.0 * s, (-3.4 * s, 3.4 * s), round(top * s), max(2, round(1.6 * s)), skin, claw, 2, sharp=False)
    names, zt = tail_bones(m, "body", (top + 3.4) * s, 4.8 * s, [((2.4 - i * 0.3) * s, (2.4 - i * 0.3) * s, 2.2 * s) for i in range(6)], skin, (-20, -40, -50, -60, -60, -60))
    fx_for(m, 0.8 * s)
    moves(m, "LICK", "WRAP")
    tongue_hidden = {"tongue": track(scale=[1, 1, 0.05])}
    tongue_out = {"tongue": track(scale=keys({0: [1, 1, 0.05], 0.25: [1, 1, 1.4], 0.7: [1, 1, 1.4], 1.0: [1, 1, 0.05]}))}
    eyes = {"eye_right": track(rotation=[wave(2.2, 14), wave(3.1, 30), 0]), "eye_left": track(rotation=[wave(2.7, 14, 120), wave(2.4, -30, 60), 0])}
    A, info = four_legs("lashleon", m, walk=0.9, stride=24, k=0.8 * s, slow=1.3,
                        extras={"idle": {**tongue_hidden, **eyes}, "battle": {**tongue_hidden, **eyes}, "walk": dict(tongue_hidden), "cry": tongue_out})
    write("lickitung", "LASHLEON", m, A, info, 1.5)


def puffball_mat(base, light, dark, wart):
    """A puffball fungus's skin: pale and leathery, cracked into little scales with darker seams, warts dotted over it."""
    B, Lt, Dk, W = rgb(base), rgb(light), rgb(dark), rgb(wart)

    def f(at):
        c = mix(Dk, B, 0.4 + 0.6 * at.ly) if at.face != "top" else mix(B, Lt, 0.4)
        h = hash01("pz", math.floor(at.x * 0.8), math.floor(at.y * 0.8), math.floor(at.z * 0.8))
        if abs((at.x * 0.8) % 1 - 0.5) > 0.44 or abs((at.y * 0.8 + (0.5 if h > 0.5 else 0)) % 1 - 0.5) > 0.44:
            c = mix(c, Dk, 0.35)
        if hash01("wt", round(at.x * 2), round(at.y * 2), round(at.z * 2)) < 0.05:
            return W
        return c
    return Mat(base, f, noise=0.02)


def spore_puff(at, fi):
    """A cloud of spores on a flat part: a soft round puff, frame by frame, thinning at its edge."""
    r = math.hypot(at.lx - 0.5, at.ly - 0.5) * 2
    R = 0.8 + 0.12 * math.sin(math.atan2(at.ly - 0.5, at.lx - 0.5) * 5 + fi)
    if r > R:
        return None
    return (rgb("#E8C8F8") if r < 0.4 else rgb("#B88AD8")), int(220 * (1 - r / R * 0.6))


def puffball(sid, name, stands, twin):
    """A puffball fungus that floats: a round, cracked, warty cream-and-violet ball, vents round it that puff out clouds of
    purple spores, a grimacing face. Grown, two of them fused, a big one and a small one, both puffing."""
    m = Model()
    skin = puffball_mat("#D8C8DC", "#F4ECF4", "#8A6A9A", "#6A4A7A")
    vent = Mat("#4A2A5A", lambda a: rgb("#2A1438") if a.face == "front" else None, noise=0.01)
    cloud = Mat("#C8A8E8", lambda a: CLEAR, glow=spore_puff, frames=True, noise=0, edge=0)
    jaw_m = Mat("#4A2A5A", open_mouth("#2A1020", "#4A2A5A"), noise=0.01)
    tooth = Mat("#F4F0E8", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    y0 = 9
    units = [("head", 0, y0, 0, 1.0)] + ([("pod2_right", -6, y0 - 4, 1.5, 0.7)] if twin else [])
    m.bone("body", "root", [0, y0, 0], [Cube([-1, y0 - 1, -1], [2, 2, 2], skin)])
    for nm, cx, cy, cz, k in units:
        R = 4.5 * k
        cubes = blob([cx - R, cy - R, cz - R], [n(2 * R), n(2 * R), n(2 * R)], skin, b=2)
        for a in (0, 72, 144, 216, 288):
            r = math.radians(a)
            vx, vz = cx + math.sin(r) * R, cz + math.cos(r) * R * 0.95
            if math.cos(r) < -0.6:
                continue
            cubes += [Cube([vx - 0.8 * k, cy + 1.2 * k, vz - 0.8 * k], [n(1.6 * k), n(1.6 * k), n(1.6 * k)], vent)]
            cubes += [Cube([vx - 2 * k, cy + 2.4 * k, vz], [n(4 * k), n(4 * k), 0], cloud, [0, -a, 0], [vx, cy + 2.4 * k, vz])]
        m.bone(nm, "body", [cx, cy, cz], cubes)
        jaw = "jaw" if nm == "head" else "jaw2_right"
        m.bone(jaw, nm, [cx, cy - 1.4 * k, cz - R], [Cube([cx - 2 * k, cy - 2.4 * k, cz - R - 0.2], [n(4 * k), n(1.4 * k), 1], jaw_m)] +
               [Cube([cx - 1.8 * k + i * 1.2 * k, cy - 1.6 * k, cz - R - 0.3], [1, 1, 0], tooth) for i in range(int(3 + k))])
    m.eyes("head", x_inner=0.6, y=y0 + 0.4, z=-4.5, w=2, h=2, iris="#E8C030", pupil="#1A1008", skin="#D8C8DC", iris_light="#FFE880",
           grid=["LLL.", "SIPI", "IIPI", "SIIS"])
    m.brows("head", [Cube([-3.2, y0 + 2.6, -4.9], [3, 1, 0.6], Mat("#6A4A7A", noise=0, edge=0), [0, 0, 18], [-0.6, y0 + 3, -4.8])])
    if twin:
        em = eye_mat(2, 2, "#E8C030", "#1A1008", "#1A1414", "#FFFFFF", iris_light="#FFE880", grid=["SIPI", "SIIS"], skin="#D8C8DC")
        bone_of(m, "pod2_right").cubes += [Cube([-7.8, y0 - 3.6, 1.5 - 3.17], [1.6, 1.4, 0], em), Cube([-5.6, y0 - 3.6, 1.5 - 3.17], [1.6, 1.4, 0], em)]
        m.bone("pod2_left", "body", [0, y0, 0], [])
    fx_for(m, 0.8 if not twin else 1.0)
    moves(m, "TACKLE" if not twin else "SLUDGE", "SMOG" if not twin else "EXPLOSION")
    ex = {"idle": {"head": track(rotation=[wave(3, 6), wave(5, 10), wave(4, 4)])}}
    if twin:
        ex["idle"]["pod2_right"] = track(position=[0, wave(2.2, 0.8, 120), 0], rotation=[0, wave(3, 12, 90), 0])
    A, info = floater(name.lower(), m, k=0.8 if not twin else 1.1, bob=0.9, pulse_bell=0.03, extras=ex)
    write(sid, name, m, A, info, stands)


def puffspore():
    puffball("koffing", "PUFFSPORE", 0.95, False)


def twinpuff():
    puffball("weezing", "TWINPUFF", 1.55, True)


def armour_plates(base, light, dark, stud):
    """Armour of bony plates (osteoderms): rounded studs in rows, each lighter at its crown, dark between them."""
    B, Lt, Dk, St = rgb(base), rgb(light), rgb(dark), rgb(stud)

    def f(at):
        if at.face == "bottom":
            return mix(Dk, B, 0.5)
        if at.face == "top":
            u, v = at.x, at.z
        elif at.face in ("nx", "px"):
            u, v = at.z, at.y
        else:
            u, v = at.x, at.y
        S = 2.4
        r = math.floor(v / S)
        du, dv = ((u + (r % 2) * S / 2) % S) / S - 0.5, (v % S) / S - 0.5
        d = math.hypot(du, dv)
        if d < 0.18:
            return St
        if d < 0.42:
            return mix(Lt, B, d * 2)
        return mix(Dk, B, 0.35)
    return Mat(base, f, noise=0.02)


def ankylosaur(sid, name, stands, upright):
    """An armoured dinosaur. The first walks low on four stout legs, its back a shell of rounded bony studs in rows, a
    blunt horn on its nose, spikes along its flanks, and a heavy club on the end of its tail. Grown, it rears up to stand
    on its hind legs, its horn grown long and ridged, clawed hands, its plates thicker, its tail club spiked."""
    m = Model()
    s = 1.0 if not upright else 1.4
    S = lambda v: [x * s for x in v]
    B, B_L, B_D, STUD = "#8A8C94", "#B8BAC4", "#4A4C56", "#D8D4C8"
    plates = armour_plates(B, B_L, B_D, STUD)
    hide = Mat(B, fur(B, B_L, B_D, belly="#C8BEB0", below=None, speck=0.2), noise=0.02)
    belly = Mat("#C8BEB0", noise=0.02)
    hornm = Mat("#E8E0D0", lambda a: mix("#A89A80", "#FFF8EC", a.ly) if a.face != "bottom" else None, noise=0.02)
    spk = taper_mat("#6A6C74", "#F0ECE0")
    jaw_m = Mat(B_D, open_mouth("#8A3A3A", B_D), noise=0.02)
    claw = Mat("#2A2420", noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    if not upright:
        top = 4
        m.bone("body", "root", S([0, top + 3, 0]), blob(S([-4.5, top, -5.5]), S([9, 5.5, 11]), plates, b=2) + [Cube(S([-3.5, top - 0.2, -5]), [n(7 * s), 1, n(10 * s)], belly)])
        bd = bone_of(m, "body")
        for i in range(4):
            for sx in (-1, 1):
                bd.cubes += spike(sx * 4.6 * s, (top + 2) * s, (-4 + i * 2.8) * s, 2.2 * s, [0, -sx * 90, 0], spk, 1.8)
        hy = top + 3
        m.bone("head", "body", S([0, hy, -5.5]), face_blob(S([-3, hy - 2, -10]), S([6, 4.4, 5]), hide) +
               [Cube(S([-2.6, hy + 1.6, -9.6]), [n(5.2 * s), 1, n(4 * s)], plates)] + spike(0, (hy + 1.2) * s, -9.4 * s, 3.4 * s, [112, 0, 0], spk, 2.4))
        m.bone("jaw", "head", S([0, hy - 1.6, -8]), [Cube(S([-2.2, hy - 2.6, -10.2]), [n(4.4 * s), 1, n(3.6 * s)], jaw_m)])
        m.eyes("head", x_inner=1.0 * s, y=(hy + 0.4) * s, z=-10 * s, w=2, h=1, iris="#E8A030", pupil="#100804", skin=B, grid=["LLLL", "IPPI"])
        stout_legs(m, 3.6 * s, (-3.4 * s, 3.4 * s), round(top * s), round(2.6 * s), hide, claw, 3, sharp=True, claw_len=1)
        names, zt = tail_bones(m, "body", (top + 2.6) * s, 5.4 * s, [(3.2 * s, 2.6 * s, 3 * s), (2.6 * s, 2.2 * s, 3 * s), (2 * s, 1.8 * s, 3 * s)], plates, (-10, 4, 6))
        tipb = bone_of(m, names[-1])
        tipb.cubes += blob([-2.6 * s, tipb.pivot[1] - 2 * s, zt - 1 * s], [n(5.2 * s), n(4 * s), n(4 * s)], plates)
        moves(m, "HORN ATTACK", "STOMP")
        fx_for(m, 0.9 * s)
        A, info = four_legs(name.lower(), m, walk=0.8, stride=24, k=0.9 * s, slow=1.2, extras={"idle": {names[-1]: track(rotation=[0, wave(2.2, 14), 0])}})
    else:
        hip = 7
        m.bone("body", "root", S([0, hip + 5, 0]), blob(S([-4.5, hip - 1, -4]), S([9, 12, 8]), plates, b=2) + [Cube(S([-3.4, hip + 0.5, -4.4]), [n(6.8 * s), n(9 * s), 1], belly)])
        bd = bone_of(m, "body")
        for i in range(4):
            bd.cubes += spike(0, (hip + 11.5 - i * 2.6) * s, 4 * s, 2.6 * s, [30, 0, 0], spk, 2.2)
        hy = hip + 10
        m.bone("head", "body", S([0, hy, -2]), face_blob(S([-3, hy, -7.4]), S([6, 4.6, 5.6]), hide) + [Cube(S([-2.6, hy + 1.6, -9.8]), [n(5.2 * s), n(2.6 * s), n(2.6 * s)], hide),
                                                                                                    Cube(S([-3.2, hy + 4, -7]), [n(6.4 * s), 1, n(4 * s)], plates)])
        hd = bone_of(m, "head")
        hd.cubes += spike(0, (hy + 3.6) * s, -9.0 * s, 5.5 * s, [108, 0, 0], Mat("#E8E0D0", lambda a: CLEAR if abs((a.lx if a.cube.size[0] else a.ly) - 0.5) > 0.5 * (1 - a.lz) + 0.02 else
                                                                               (rgb("#A89A80") if int(a.lz * 8) % 2 else rgb("#F8F0E0")), noise=0, edge=0, flat=True), 2.6)
        m.bone("jaw", "head", S([0, hy + 0.8, -6.6]), [Cube(S([-2.4, hy, -9.6]), [n(4.8 * s), 1, n(3 * s)], jaw_m)])
        m.eyes("head", x_inner=1.0 * s, y=(hy + 2.6) * s, z=-7.4 * s, w=2, h=1, iris="#E85A20", pupil="#100804", skin=B, grid=["LLLL", "IPPI"])
        upright_arms(m, 5 * s, (hip + 9) * s, -0.5 * s, 3.5 * s, 3.5 * s, n(2.6 * s), hide, hide, hw=n(3.2 * s), hh=n(1.6 * s), claws=3, rest=(-20, 0, 16), fore_rest=(-30, 0, 0))
        upright_legs(m, 3 * s, hip * s, 0.6 * s, n(3.4 * s), hide, hide, n(5 * s), ntoes=3, sharp=True, thigh_w=n(4.6 * s), toe_len=2)
        names, zt = tail_bones(m, "body", (hip + 1) * s, 3.6 * s, [(3.4 * s, 3 * s, 3.4 * s), (2.8 * s, 2.4 * s, 3.4 * s), (2.2 * s, 1.8 * s, 3.4 * s)], plates, (-36, -8, 6))
        tipb = bone_of(m, names[-1])
        tipb.cubes += blob([-2.6 * s, tipb.pivot[1] - 2 * s, zt - 1 * s], [n(5.2 * s), n(4 * s), n(4 * s)], plates)
        for sx in (-1, 1):
            tipb.cubes += spike(sx * 2.6 * s, tipb.pivot[1], zt + 1 * s, 2.4 * s, [0, -sx * 90, 0], spk, 1.8)
        moves(m, "HORN DRILL", "EARTHQUAKE")
        fx_for(m, 0.9 * s)
        A, info = biped(name.lower(), m, walk=0.8, stride=20, k=1.2 * s, slow=1.25, guard=26)
    write(sid, name, m, A, info, stands)


def clubhorn():
    ankylosaur("rhyhorn", "CLUBHORN", 1.38, False)


def cragsaur():
    ankylosaur("rhydon", "CRAGSAUR", 2.4, True)


def nesthen():
    """A round, fluffy mother hen in soft pink and cream, a little red comb, a short bill, kind eyes, stubby wings, and a
    nest of woven straw tucked against her front holding a speckled golden egg."""
    m = Model()
    s = 1.3
    S = lambda v: [x * s for x in v]
    F, F_L, F_D = "#F4C8D0", "#FFF0F2", "#C8909C"
    fluff = Mat(F, fur(F, F_L, F_D, belly="#FFF4F0", below=4 * s, speck=0.7), noise=0.03)
    comb = Mat("#E8403A", lambda a: rgb("#FF7A6A") if a.ly > 0.6 else None, noise=0.02, edge=0)
    beak = Mat("#F0B040", lambda a: rgb("#C8882A") if a.face == "bottom" else None, noise=0.01, edge=0)
    straw = Mat("#D8B060", lambda a: rgb("#A8803A") if (a.col + a.row) % 3 == 0 else (rgb("#F0D088") if (a.col * 2 + a.row) % 5 == 0 else None), noise=0.02)
    egg = Mat("#F4E4B0", lambda a: rgb("#E8A040") if hash01("eg", round(a.x * 2), round(a.y * 2), round(a.z * 2)) < 0.12 else mix("#E8D090", "#FFF8E0", a.ly),
              noise=0.01)
    legm = Mat("#F0A030", noise=0.02, edge=0)
    sec = Mat(F, feather_plane(F_D, F, F_L, edge="#FFFFFF"), noise=0.015, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 5, 0]), blob(S([-4.5, 1.5, -4.2]), S([9, 8.5, 8.4]), fluff, b=2))
    m.bone("head", "body", S([0, 9.5, -1.5]), face_blob(S([-2.8, 9, -5]), S([5.6, 4.4, 5]), fluff) + [Cube(S([-0.5, 13.2, -3.8]), [1, n(2 * s), n(3 * s)], comb)])
    m.bone("beak", "head", S([0, 10.6, -5]), [Cube(S([-0.8, 10.2, -6.4]), [n(1.6 * s), 1, n(1.6 * s)], beak)])
    m.bone("beak_lower", "head", S([0, 10, -5]), [Cube(S([-0.6, 9.6, -6]), [n(1.2 * s), 1, n(1.2 * s)], beak)])
    m.eyes("head", x_inner=0.6 * s, y=11 * s, z=-5 * s, w=2, h=2, iris="#2A1A20", pupil="#08060A", skin=F, iris_light="#6A4A5A", grid=["DLLL", "PPWP", "PPPP", "SKKS"])
    m.pair("wing_right", "body", S([-4.6, 8.5, -1.5]), [Cube(S([-4.7, 4, -2]), [0, n(4.5 * s), n(5 * s)], sec), Cube(S([-4.9, 4.5, -1]), [0, n(3.5 * s), n(4 * s)], sec)],
           rotation=[0, 0, 6])
    bone_of(m, "body").cubes += blob(S([-3.2, 2, -6.6]), S([6.4, 3.4, 3.4]), straw) + blob(S([-1.6, 4, -6.2]), S([3.2, 3.8, 3]), egg)
    m.bone("tail", "body", S([0, 7, 4]), [Cube(S([-1.5, 6, 3.8]), [n(3 * s), 0, n(3 * s)], sec, [-40 + i * 20, 0, 0], S([0, 7, 4])) for i in range(3)], rotation=[-20, 0, 0])
    bird_legs(m, 1.8 * s, 2.4 * s, 0.4 * s, 0.8 * s, 1.6 * s, 1, fluff, legm, Mat("#A87020", noise=0, edge=0), toe_len=2)
    fx_for(m, s)
    moves(m, "DOUBLESLAP", "SOFTBOILED")
    A, info = bird("nesthen", m, walk=0.55, stride=28, k=s, flap=0.3,
                   extras={"walk": {"shin_right": track(rotation=[bounce(0.55, 20), 0, 0]), "shin_left": track(rotation=[bounce(0.55, 20, 90), 0, 0])}})
    write("chansey", "NESTHEN", m, A, info, 1.37)


def tangleroot():
    """A round tangle of dark green vines with little leaves all over it, big round eyes peering out from the dark inside,
    two stubby red-orange root feet, and a few loose vines it waves like arms."""
    m = Model()
    vine = Mat("#3A7A3A", lambda a: mix("#1E4A22", "#6AB050", 0.5 + 0.5 * math.sin(a.x * 3 + a.z * 2.4 + a.y * 1.7)), noise=0.03)
    leafm = leaf_mat("#7AC050", "#2E6A24", "#C8F0A0")
    inner = Mat("#0E1A10", noise=0.01, edge=0)
    footm = Mat("#D8502A", lambda a: rgb("#A83A1A") if a.face == "bottom" else None, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    y0 = 8
    cubes = blob([-3.6, y0 - 3.6, -3.8], [7.2, 7.2, 7.2], inner, b=2)
    for i in range(30):
        a, b = i * 2.4, i * 1.3
        L = 10 + (i % 3)
        cubes.append(Cube([-L / 2, y0 - 0.8, -0.8], [L, 1.6, 1.6], vine, [math.degrees(b) % 180, math.degrees(a) % 360, (i * 37) % 180], [0, y0, 0]))
    for i in range(16):
        a = i * 2.2
        x, y, z = math.sin(a) * 4.6, y0 + math.cos(a * 1.3) * 3.6, math.cos(a) * 4.6
        cubes.append(Cube([x - 1.2, y, z], [2.4, 0, 2.6], leafm, [30, -math.degrees(a), 0], [x, y, z]))
    m.bone("body", "root", [0, y0, 0], cubes)
    m.bone("head", "body", [0, y0, -4], [Cube([-3, y0 - 1, -4.6], [6, 3, 0], Mat("#0E1A10", lambda a: CLEAR, noise=0, edge=0, flat=True))])
    em = eye_mat(2, 2, "#F4F0E0", "#141018", "#141018", "#FFFFFF", grid=["SWWS", "WWPW", "WPPW", "SWWS"], skin="#0E1A10")
    bone_of(m, "head").cubes += [Cube([-3.0, y0, -5.9], [2, 2, 0], em), Cube([1.0, y0, -5.9], [2, 2, 0], em)]
    m.bone("eyelid_right", "head", [0, y0 + 2, -4.6], [])
    m.bone("jaw", "head", [0, y0 - 1.2, -4.4], [])
    for sx, side in ((-1, "right"), (1, "left")):
        m.bone(f"arm_{side}", "body", [sx * 4.6, y0 + 1, -1], [Cube([sx * 4.6 - 0.6, y0 - 4, -1.6], [1.2, 5, 1.2], vine)], rotation=[0, 0, sx * -30])
        m.bone(f"forearm_{side}", f"arm_{side}", [sx * 4.6, y0 - 4, -1], [Cube([sx * 4.6 - 0.5, y0 - 8, -1.5], [1, 4, 1], vine),
                                                                          Cube([sx * 4.6 - 1.2, y0 - 9.5, -1], [2.4, 2.6, 0], leafm)], rotation=[-20, 0, 0])
    upright_legs(m, 2.2, 3.5, 0.5, 2, vine, footm, 4, toe_m=footm, ntoes=2, thigh_w=1.6)
    fx_for(m, 0.9)
    moves(m, "CONSTRICT", "MEGA DRAIN")
    A, info = biped("tangleroot", m, walk=0.55, stride=28, k=0.9, guard=20, extras={"idle": {"body": track(rotation=[0, wave(4, 8), wave(3, 3)])}})
    write("tangela", "TANGLEROOT", m, A, info, 0.93)


def antmatron():
    """A mother anteater, a brawler who rears up to fight: a long tube of a snout, a thick coat of grey-brown with a bold black
    band edged in white running over her shoulders, a great bushy tail, huge curved claws on her front feet, and her baby
    riding on her back, a little copy of her clinging on."""
    m = Model()
    FUR, FUR_L, FUR_D = "#9A8A78", "#C8B8A4", "#5E5244"
    base = fur(FUR, FUR_L, FUR_D, speck=0.5)

    def coat(at):
        # The black band over the shoulders, edged with white, slanting back across the flanks.
        k = at.z + at.y * 0.6
        if at.face != "bottom" and -2.5 < k < 0.5:
            return rgb("#1E1A18")
        if at.face != "bottom" and (0.5 <= k < 1.4 or -3.4 < k <= -2.5):
            return rgb("#F0ECE0")
        return base(at)
    furm = Mat(FUR, coat, noise=0.02)
    plain = Mat(FUR, base, noise=0.02)
    snout = Mat(FUR_L, lambda a: rgb("#1E1A18") if a.lz < 0.12 else mix(FUR_D, FUR_L, a.ly), noise=0.02)
    tailm = Mat(FUR_D, brush(FUR_D, FUR, "#2A241E"), noise=0.0, edge=0)
    claw = sharp_claw_mat("#2A2420", "#C8C0B0")
    m.bone("root", None, [0, 0, 0])
    top = 7
    m.bone("body", "root", [0, top + 4, 0], blob([-4, top, -6.5], [8, 8, 14], furm, b=2))
    hy = top + 5
    m.bone("neck", "body", [0, hy, -6], blob([-2.6, hy - 2.5, -9], [5.2, 5, 4], furm), rotation=[10, 0, 0])
    m.bone("head", "neck", [0, hy, -9], face_blob([-2, hy - 1.6, -12], [4, 3.4, 3.4], plain) + [Cube([-1, hy - 1.4, -20], [2, 2, 8.4], snout)], rotation=[14, 0, 0])
    m.bone("jaw", "head", [0, hy - 0.8, -12], [Cube([-0.6, hy - 1.6, -19.6], [1.2, 0.6, 7], Mat("#F07A9A", noise=0, edge=0))])
    m.eyes("head", x_inner=1.0, y=hy + 0.2, z=-12, w=1, h=1, iris="#1A1410", pupil="#000000", skin=FUR, grid=["PW", "PP"])
    m.pair("ear_right", "head", [-1.6, hy + 1.6, -10], [Cube([-2.6, hy + 1.4, -10], [1.6, 1.6, 0], plain)])
    stout_legs(m, 3.0, (-4.5, 4.5), top, 3, plain, claw, 3, sharp=True, claw_len=3)
    names, zt = tail_bones(m, "body", top + 5, 7, [(3, 3, 3), (2.6, 2.6, 3), (2.2, 2.2, 3)], plain, (8, -6, -8))
    for i, nm in enumerate(names):
        b = bone_of(m, nm)
        b.cubes += [Cube([0, b.pivot[1] - 4, b.pivot[2]], [0, 9, 3.2], tailm)]
    # Her baby, riding on her back.
    m.bone("baby", "body", [0, top + 8, -1], blob([-2, top + 8, -4], [4, 3.4, 6], furm) + [Cube([-0.6, top + 8.6, -8.4], [1.2, 1.2, 4.6], snout),
                                                                                            Cube([-1.6, top + 9.2, -5], [3.2, 2.6, 2.6], plain)])
    em = eye_mat(1, 1, "#1A1410", "#000000", "#1A1410", "#FFFFFF", grid=["P"], skin=FUR)
    bone_of(m, "baby").cubes += [Cube([-1.62, top + 10.4, -4.6], [0, 1, 1], em), Cube([1.62, top + 10.4, -4.6], [0, 1, 1], em)]
    fx_for(m, 1.2)
    moves(m, "MEGA PUNCH", "DIZZY PUNCH")
    A, info = four_legs("antmatron", m, walk=0.85, stride=26, k=1.2, slow=1.2,
                        extras={"idle": {"baby": track(rotation=[wave(2.6, 5), wave(3.4, 12), 0]), "jaw": track(scale=[1, 1, wave(1.6, 0.5, 0, 0.5)])}})
    write("kangaskhan", "ANTMATRON", m, A, info, 3.0)


def seadragon(sid, name, stands, big):
    """A leafy sea dragon that drifts upright: a slim golden-green body in rings, a long tube of a snout, a curled tail in
    joints, and leafy fronds sprouting from its back, belly and tail like seaweed. Grown, longer and spinier, its fronds
    grown into great frilled sprays edged in red, a glowing blue fin down its back."""
    m = Model()
    s = 1.0 if not big else 1.6
    S = lambda v: [x * s for x in v]
    B, B_L, B_D = ("#C8B050", "#F0DC80", "#7A6A28") if not big else ("#5AA87A", "#9AE0A8", "#2A6A4A")

    def ringed(at):
        c = mix(B_D, B, 0.4 + 0.6 * at.ly)
        if (at.y * 1.4) % 1.0 < 0.18:
            c = mix(c, B_D, 0.5)
        if hash01("dot", round(at.x * 2), round(at.y * 2), round(at.z * 2)) < 0.06:
            c = mix(c, "#FFFFFF", 0.6)
        return c
    body = Mat(B, ringed, noise=0.02)
    frond = leaf_mat("#9AC850" if not big else "#6AC880", "#4A7A28" if not big else "#2A6A48", "#E8F0A0", tip="#E84A3A" if big else None)
    finm = Mat("#5AC8F0", lambda a: CLEAR if a.ly > 0.4 + 0.5 * math.sin(math.pi * a.lz) else None, glow=(lambda a: rgb("#8AE8FF") if a.ly < 0.3 else None) if big else None,
               noise=0, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    y0 = 9
    m.bone("body", "root", S([0, y0, 0]), blob(S([-2, y0 - 3, -2]), S([4, 6, 4]), body) + [Cube(S([-1.8, y0 - 1, -2.6]), [n(3.6 * s), n(3 * s), 1], body)])
    m.bone("neck", "body", S([0, y0 + 3, -0.5]), [Cube(S([-1.4, y0 + 2.6, -1.8]), [n(2.8 * s), n(3 * s), n(2.8 * s)], body)], rotation=[-10, 0, 0])
    m.bone("head", "neck", S([0, y0 + 5.4, -0.5]), face_blob(S([-1.8, y0 + 5, -2.6]), S([3.6, 3, 3.4]), body) + [Cube(S([-0.6, y0 + 5.6, -7]), [n(1.2 * s), n(1.2 * s), n(4.6 * s)], body),
                                                                                                         Cube(S([-1, y0 + 5.2, -7.6]), [n(2 * s), n(1.8 * s), 1], body)],
           rotation=[10, 0, 0])
    m.bone("jaw", "head", S([0, y0 + 5.6, -7]), [])
    m.eyes("head", 1.8 * s, (y0 + 6.2) * s, -2.6 * s, h=1, corner=(1, 1), half=1.8 * s, iris="#E85A20" if big else "#2A1A10", pupil="#100804", skin=B,
           iris_light="#FFA060", grid=["IP"])
    names, zt = [], 0
    prev, y = "body", y0 - 3
    for i in range(5):
        nm = "tail" if i == 0 else f"tail{i + 1}"
        w = (2.6 - i * 0.35) * s
        m.bone(nm, prev, [0, y * s, 0.5 * s], [Cube([-w / 2, (y - 2.2) * s, 0.5 * s - w / 2], [n(w), n(2.4 * s), n(w)], body)], rotation=[30 if i else 10, 0, 0])
        names.append(nm)
        prev, y = nm, y - 2.2
    fr = [("body", 0, y0 + 2, 1.8, 0, 4), ("body", -1.8, y0, 0, -60, 3.5), ("body", 1.8, y0, 0, 60, 3.5), ("neck", 0, y0 + 4.6, 1, -20, 3), ("head", 0, y0 + 7.6, -1, -30, 3),
          ("tail2", 0, y0 - 5, 1.6, 20, 3), ("tail3", -1, y0 - 7, 1, -50, 2.6), ("tail3", 1, y0 - 7, 1, 50, 2.6)]
    for bn, x, y, z, a, L in fr:
        b = bone_of(m, bn)
        for k in range(2 if not big else 3):
            b.cubes.append(Cube([x * s - 1.2 * s, y * s, z * s], [n(2.4 * s), n(L * s * (1 + k * 0.3)), 0], frond, [k * 30 - 20, a + k * 25, (a / 3)], [x * s, y * s, z * s]))
    if big:
        bone_of(m, "body").cubes.append(Cube([0, (y0 - 2) * s, 1.8 * s], [0, n(4 * s), n(3 * s)], finm))
    fx_for(m, 0.7 * s)
    moves(m, "BUBBLE" if not big else "WATER GUN", "SMOKESCREEN" if not big else "TWISTER")
    A, info = floater(name.lower(), m, k=0.7 * s, bob=0.8, tentacles=[names],
                      extras={"idle": {"head": track(rotation=[wave(2.6, 5), wave(4, 10), 0])}})
    write(sid, name, m, A, info, stands)


def driftleaf():
    seadragon("horsea", "DRIFTLEAF", 0.6, False)


def frondrake():
    seadragon("seadra", "FRONDRAKE", 1.27, True)


def betta(sid, name, stands, big):
    """A fighting fish of flowing fins: a body of shining scales in flame red and orange, great veils of fins that ripple
    behind it, a small horn on its brow. Grown, deep crimson and gold, its fins longer still, its horn grown into a long
    ridged lance."""
    m = Model()
    s = 1.0 if not big else 1.6
    S = lambda v: [x * s for x in v]
    B, B_L, B_D = ("#F05A2A", "#FFB070", "#A02A14") if not big else ("#C81E3A", "#F8C048", "#6A0A1E")

    def scales(at):
        c = mix(B_D, B, 0.4 + 0.6 * at.ly)
        r = math.floor(at.y * 1.3)
        fu = (at.z * 1.3 + (r % 2) * 0.5) % 1.0
        if fu > 0.82:
            c = mix(c, B_D, 0.4)
        elif fu < 0.3:
            c = mix(c, B_L, 0.35)
        return c
    body = Mat(B, scales, noise=0.015)
    veil = Mat(B_L, lambda a: CLEAR if a.ly < 0.08 + 0.5 * (1 - math.sin(math.pi * min(1, a.lz * 1.05))) * 0.4 or (a.lz > 0.85 and math.sin(a.ly * 30) > 0.3)
               else mix(B, B_L, a.lz), noise=0.01, edge=0, flat=True)
    horn = Mat("#F4E8C8", lambda a: rgb("#C8B080") if int(a.lz * 10) % 2 else None, noise=0.01, edge=0)
    m.bone("root", None, [0, 0, 0])
    y0 = 6
    m.bone("body", "root", S([0, y0, 0]), blob(S([-2, y0 - 2.4, -4]), S([4, 5, 8]), body, b=1))
    m.bone("head", "body", S([0, y0, -4]), face_blob(S([-1.8, y0 - 2, -6.6]), S([3.6, 4.2, 3]), body) + [Cube(S([-1, y0 - 1.6, -7.4]), [n(2 * s), n(1.6 * s), 1], body)])
    hl = 1.6 if not big else 6
    bone_of(m, "head").cubes += [Cube(S([-0.5, y0 + 1.4, -6.8 - hl]), [1, 1, n(hl * s)], horn, [-14, 0, 0], S([0, y0 + 1.6, -6.4]))]
    m.bone("jaw", "head", S([0, y0 - 0.8, -7]), [Cube(S([-0.8, y0 - 1.6, -7.6]), [n(1.6 * s), 1, 1], Mat(B_D, noise=0.01))])
    m.eyes("head", 1.8 * s, y0 * s, -6.6 * s, h=2, corner=(1, 1), half=1.8 * s, iris="#1A1010", pupil="#000000", skin=B, grid=["WP", "PP"])
    names, zt = tail_bones(m, "body", y0 * s, 4 * s, [(3 * s, 3.6 * s, 2 * s), (2 * s, 2.6 * s, 2 * s)], body, (0, 0))
    tb = bone_of(m, names[-1])
    L = (7 if not big else 8) * s
    tb.cubes += [Cube([0, (y0 - 4) * s, zt - 0.5], [0, n(8 * s), n(L)], veil), Cube([0.1, (y0 - 3) * s, zt - 0.5], [0, n(6 * s), n(L * 0.8)], veil, [0, 0, 0], None)]
    bd = bone_of(m, "body")
    bd.cubes += [Cube([0, (y0 + 2.4) * s, -2 * s], [0, n(4 * s), n(7 * s)], veil, [-20, 0, 0], S([0, y0 + 2.4, -2])),
                 Cube([0, (y0 - 6.4) * s, -1 * s], [0, n(4 * s), n(5 * s)], veil, [20, 0, 0], S([0, y0 - 2.4, -1]))]
    m.pair("wing_right", "body", S([-2, y0 - 1, -2.4]), [Cube(S([-2.1, y0 - 4.5, -2.4]), [0, n(4 * s), n(4 * s)], veil)], rotation=[0, 0, 30])
    fx_for(m, 0.7 * s)
    moves(m, "PECK" if not big else "HORN DRILL", "HORN ATTACK" if not big else "WATERFALL")
    A, info = floater(name.lower(), m, k=0.7 * s, bob=0.6, tentacles=[names],
                      extras={"idle": {"wing_right": track(rotation=[0, 0, wave(0.8, 16, 0, 30)]), "wing_left": track(rotation=[0, 0, wave(0.8, -16, 0, -30)])},
                              "walk": {"wing_right": track(rotation=[0, 0, wave(0.5, 22, 0, 30)]), "wing_left": track(rotation=[0, 0, wave(0.5, -22, 0, -30)])}})
    write(sid, name, m, A, info, stands)


def flaretail():
    betta("goldeen", "FLARETAIL", 0.62, False)


def lancetail():
    betta("seaking", "LANCETAIL", 1.17, True)


def urchin(sid, name, stands, big):
    """A sea urchin that spins: a round purple test covered in long spines, pale-tipped, and in the middle of its face a
    faceted gem glowing gold. Grown, two layers of spines, the outer ones long and violet, and a great prism of a gem
    flashing every colour."""
    m = Model()
    s = 1.0 if not big else 1.5
    S = lambda v: [x * s for x in v]
    test = Mat("#6A3A8A", lambda a: rgb("#8A5AAA") if hash01("tub", round(a.x * 2), round(a.y * 2), round(a.z * 2)) < 0.2 else mix("#3A1A54", "#6A3A8A", a.ly), noise=0.02)
    spine = taper_mat("#4A2A6A", "#F4E8FF")
    spine2 = taper_mat("#8A4AC8", "#FFFFFF")
    gemc = "#FFD040" if not big else "#E848C8"
    gem = Mat(gemc, lambda a: rgb("#FFFFFF") if a.lx > 0.6 and a.ly > 0.6 else None,
              glow=(lambda a: rgb(gemc)) if not big else (lambda a: ramp(["#FF5A5A", "#FFD040", "#5AE8A0", "#5A9AFF", "#C85AFF"], (a.lx + a.ly) / 2)), noise=0, edge=0)
    m.bone("root", None, [0, 0, 0])
    y0 = 6
    m.bone("body", "root", S([0, y0, 0]), blob(S([-4, y0 - 4, -4]), S([8, 8, 8]), test, b=2))
    bd = bone_of(m, "body")
    k = 0
    for ring, (lat, cnt) in enumerate(((50, 6), (15, 9), (-20, 9), (-55, 6))):
        for i in range(cnt):
            a = (i + ring * 0.5) * 360 / cnt
            la, lo = math.radians(lat), math.radians(a)
            x, y, z = math.cos(la) * math.sin(lo) * 4, y0 + math.sin(la) * 4, math.cos(la) * math.cos(lo) * 4
            if z < -3.2 and abs(lat) < 30:
                continue
            bd.cubes += spike(x * s, y * s, z * s, (4.5 if not big else 6) * s, [lat, -a, 0], spine2 if (big and k % 2) else spine, 1.6 * s)
            k += 1
    m.bone("head", "body", S([0, y0, -4]), [Cube(S([-1.5, y0 - 1.5, -4.4]), [n(3 * s), n(3 * s), 1], gem)])
    m.bone("jaw", "head", S([0, y0 - 1, -4.4]), [])
    m.bone("eyelid_right", "head", S([0, y0, -4.4]), [])
    fx_for(m, 0.7 * s)
    moves(m, "TACKLE" if not big else "SWIFT", "SWIFT" if not big else "BUBBLEBEAM")
    A, info = hopper(name.lower(), m, hop=0.5, height=1.2 * s, k=0.7 * s,
                     extras={"idle": {"body": track(rotation=[0, "q.anim_time * 30", 0])}, "walk": {"body": track(rotation=[0, "q.anim_time * 240", 0])},
                             })
    write(sid, name, m, A, info, stands)


def urchlet():
    urchin("staryu", "URCHLET", 0.71, False)


def prismurchin():
    urchin("starmie", "PRISMURCHIN", 1.0, True)


def pantolemur():
    """A ring-tailed lemur who is a mime: soft grey with a white face and a black mask round big amber eyes, a white ruff,
    over-large white-gloved hands it holds up flat against walls no one else can see, red-and-white striped leg warmers,
    and a long tail ringed in black and white."""
    m = Model()
    FUR, FUR_L, FUR_D = "#A8A4AC", "#D8D4DC", "#6A6670"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, speck=0.4), noise=0.02)
    face = Mat("#F4F2F0", lambda a: rgb("#1A1418") if a.face == "front" and 0.25 < a.ly < 0.75 and abs(a.lx - 0.5) > 0.12 else None, noise=0.02)
    glove = Mat("#FFFFFF", lambda a: rgb("#E0DCE4") if a.face == "bottom" else None, noise=0.02)
    warmer = Mat("#E8403A", lambda a: rgb("#FFFFFF") if int(a.y * 1.2) % 2 else None, noise=0.02)
    ringed = Mat(FUR, lambda a: rgb("#1A1418") if int(math.floor(a.z / 1.6)) % 2 == 0 else rgb("#F4F2F0"), noise=0.02)
    ruff = shag_mat("#FFFFFF", "#FFFFFF", "#D8D4DC")
    jaw_m = Mat("#F4F2F0", open_mouth("#8A3A4A", "#E0D0D0"), noise=0.02)
    ear = ear_mat(FUR, "#E8E0E8", "fennec")
    m.bone("root", None, [0, 0, 0])
    hip = 7
    m.bone("body", "root", [0, hip + 3, 0], blob([-3.2, hip - 1, -2.6], [6.4, 9, 5.2], furm, b=1) + [Cube([-2.4, hip, -3], [4.8, 7, 1], Mat(FUR_L, noise=0.02))])
    hy = hip + 7.5
    m.bone("head", "body", [0, hy, -0.5], face_blob([-3, hy, -3.6], [6, 5, 5], face) + [Cube([-1.2, hy, -5], [2.4, 2, 1.6], face), Cube([-0.5, hy + 1.4, -5.1], [1, 1, 0.2], Mat("#1A1418", noise=0, edge=0))])
    m.bone("jaw", "head", [0, hy + 0.4, -3.6], [Cube([-1, hy - 0.2, -4.8], [2, 1, 1.4], jaw_m)])
    m.eyes("head", x_inner=0.6, y=hy + 2.4, z=-3.6, w=2, h=2, iris="#E8A020", pupil="#100804", skin="#1A1418", iris_light="#FFD060", grid=["SIIS", "IPPW", "IPPI", "SIIS"])
    m.pair("ear_right", "head", [-2.4, hy + 4.4, -1], [Cube([-4.4, hy + 3.8, -1], [3, 3.6, 0], ear)], rotation=[0, 10, -24])
    bd = bone_of(m, "body")
    for a in (-60, -20, 20, 60):
        r = math.radians(a)
        cx, cz = math.sin(r) * 3, (-2.6 + (1 - math.cos(r)) * 1.6)
        bd.cubes.append(Cube([cx - 1.6, hy - 2.6, cz], [3.2, 3, 0], ruff, [20, a, 0], [cx, hy, cz]))
    upright_arms(m, 3.8, hy - 1, -0.5, 3.6, 3.6, 1.4, furm, glove, hw=3.6, hh=2.6, fingers=4, finger_m=glove, finger_len=1.6, rest=(-70, 0, 12), fore_rest=(-60, 0, 0))
    upright_legs(m, 2, hip, 0.5, 2, warmer, Mat(FUR_D, noise=0.02), 4, ntoes=3, toe_m=Mat("#1A1418", noise=0, edge=0), thigh_w=2.6, knee_at=0.45)
    tail_bones(m, "body", hip + 1, 2.4, [(1.6, 1.6, 3)] * 6, ringed, (-20, 30, 22, 18, 10, 6))
    fx_for(m, 1.0)
    moves(m, "DOUBLESLAP", "BARRIER")
    # Miming: its gloved hands pressed flat against a wall that isn't there, feeling along it.
    mime = {"forearm_right": track(rotation=[wave(2.4, 8, 0, 0), 0, wave(1.2, 6)]), "forearm_left": track(rotation=[wave(2.4, 8, 180, 0), 0, wave(1.2, -6, 90)]),
            "arm_right": track(rotation=[wave(2.4, 6, 90), 0, 0]), "arm_left": track(rotation=[wave(2.4, 6, 270), 0, 0])}
    A, info = biped("pantolemur", m, walk=0.55, stride=26, k=1.0, guard=0, extras={"idle": mime, "battle": mime})
    write("mr_mime", "PANTOLEMUR", m, A, info, 1.5)


ALL3 = (leafgrub, huskalis, veilmoth, stinglet, barbcase, lancewasp, coilet, frillasp, pangrit, spirepang, lunabun, lunahare,
        fennember, solfennec, puffchin, choruschin,
        duskbat, nightmaw, tuberlet, droopbud, rafflora, shroomshell, funguard, fuzzgnat, toxilace, starsnout, triosnout, lynxkit, sabrelynx,
        achebill, tidebill, kindlepup, pyrewolf, droptad, frillfrog, brawltoad, slumbersier, mindsier, sagesier, knucklet, brawlape,
        quadrilla, snapsprout, snapbulb, gulptrap, driftsail, stormsail, geodink, geobrute, geotitan, emberfawn, blazehart, slowmander,
        shellmander, boltnut, boltrio, reedfisher, twincask, tricask, frostpup, glaciseal, bogblob, miremass, glintshell, thornyster,
        wisplet, shadewick, gloomwarden, basalwyrm, dozala, hypnala, pinchlet, fiddleking, bulblet, surgebulb, podlings, oaktrio, skullkit,
        bonejackal, kickaroo, smashrimp, lashleon, puffspore, twinpuff, clubhorn, cragsaur, nesthen, tangleroot, antmatron, driftleaf, frondrake,
        flaretail, lancetail, urchlet, prismurchin, pantolemur)


def main(names):
    for f in ALL3:
        if not names or f.__name__ in names:
            f()


if __name__ == "__main__":
    main(set(sys.argv[1:]))
