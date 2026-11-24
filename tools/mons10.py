"""
Retromon's own creatures, part ten: Kanto's last 28 (John, 2026-11-24 13:08: "Do the missing Kanto and any Johto
creatures in our design as well"). Our own designs drawn from real animals, plants and things, the same species
underneath; several join families Johto's started (the genets, the ermine, the numbat, the fire salamander, the pincer
beetle, the paper crane).

Run: python3 tools/mons10.py [names]   (all if none; refreshes mons/names.json either way)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, cell, face_blob, hash01, mix, rgb, track, wave, keys, write, flame  # noqa: E402
from monanims import four_legs, bird, floater, insect, biped, serpent, flyer, hopper, crawler, cocoon, ooze  # noqa: E402
from mons import fx_for, open_mouth, shag_mat, whisker_mat, horn_mat, leaf_mat, membrane_mat, dragon_wing  # noqa: E402
from mons2 import fur, brush, n  # noqa: E402
from mons3 import (moves, bone_of, tail_bones, ear_mat, spike, taper_mat, glow_patch, legs_pair, upright_legs, upright_arms, comb_mat,  # noqa: E402
                   compound_grid)
from mons5 import quad, avian, bug, S_, first, solid, plain, antlers, back_row, flame_row, spot_coat, stripe_coat, both  # noqa: E402
from mons6 import upright, fish, fin_mat  # noqa: E402
from mons7 import ringed  # noqa: E402


def sickle(S, s, y, fz, x0, length, w, mat, spine_mat, out=1.0, rise=0.0, steps=10):
    """A pair of jaws curving forward from the face and hooking in until their tips nearly meet, built as a chain of
    small blocks along the curve (no joints to come apart), spines along their inner edges. x0: how far out each starts;
    length: how far forward they reach; w: thickness at the root; out: how far they bow out; rise: how far the tips lift."""
    cubes = []
    for sg in (-1, 1):
        for i in range(steps + 1):
            t = i / steps
            x = sg * (x0 + out * math.sin(math.pi * t * 0.9) - (x0 - 0.35) * t * t)
            z = fz - length * t
            yy = y + rise * t * t
            ww = w * (1 - 0.6 * t)
            cubes.append(Cube(S([x - ww / 2, yy - ww / 2, z - ww / 2]), [n(ww * s), n(ww * s), n(ww * s)], mat))
            if 0.25 < t < 0.8 and i % 2 == 0:
                cubes.append(Cube(S([x - sg * ww / 2 - (0.7 if sg > 0 else 0), yy - 0.3, z - 0.3]), [n(0.7 * s), 1, 1], spine_mat))
    return cubes


# ======================================================================================================
# The genets, the rest of the family: GENKIT > TIDEGENET, VOLTGENET, BLAZEGENET (Eevee, Vaporeon, Jolteon, Flareon)
# ======================================================================================================

def genet_kin(sid, name, stands, kind):
    """The genet family (the sun and dusk genets' kin). The kit is a soft grey-buff, its spots and tail rings only just
    showing, big ears and a pale bib, and can grow into anything. The tide genet is slick slate with pale spots, webbed
    feet and a broad paddle at the end of its ringed tail; the volt genet is ash-gold spotted black, a crest of bristles
    down its spine that glow and crackle; the blaze genet is soot-dark, its spots and rings glowing like embers."""
    P = {
        "kit": ("#A89A86", "#D8CCB8", "#706452", "#F4ECDC", "#6A5A48", "#3A2414", "#8A6A4A", 1.0),
        "tide": ("#4E6878", "#8AA4B4", "#2A3E4C", "#DCE8EE", "#C8E0EC", "#1A3A6A", "#5A8AD0", 1.3),
        "volt": ("#C8B07A", "#E8D8A8", "#8A7448", "#F4ECD4", "#2A2418", "#2A3A1A", "#6A8A3A", 1.2),
        "blaze": ("#3A2E2A", "#5A4A42", "#1E1614", "#6A5A50", "#F08A2A", "#3A1A0A", "#F0A040", 1.25),
    }[kind]
    FUR, FUR_L, FUR_D, CREAM, SPOT, IRIS, IRIS_L, s = P

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        nb = bone_of(m, "neck")
        hb = bone_of(m, "head")
        m.pair("whiskers_right", "head", S([-1, p["hy"] + 0.8, p["hz"] - p["hl"] - p["snout"] + 0.6]),
               [Cube(S([-4.4, p["hy"] + 0.8, p["hz"] - p["hl"] - p["snout"]]), [n(3.4 * s_), 0, n(1.6 * s_)], whisker_mat("#F4F0E8" if kind != "blaze" else "#8A7A70"))])
        if kind == "tide":
            # A broad flat paddle at the end of its tail, for swimming (an otter's rudder).
            tb = bone_of(m, p["tail"][-1])
            pad = Mat("#4E6878", lambda a: mix("#2A3E4C", "#8AA4B4", a.lz) if a.lz < 0.85 else rgb("#C8E0EC"), noise=0.01, edge=0, flat=True)
            tb.cubes.append(Cube([-1.6 * s_, tb.pivot[1], tb.pivot[2]], [n(3.2 * s_), 0, n(3.6 * s_)], pad))
        if kind == "volt":
            # A crest of bristles down its spine, glowing at their tips and crackling (pointing up and back).
            b = bone_of(m, "body")
            for i in range(9):
                z = -p["bl"] / 2 + 1.2 + i * (p["bl"] - 2.4) / 8
                y = p["top"] + p["bh"] - 0.4
                b.cubes += spike(0, y * s_, z * s_, (2.4 - 0.6 * abs(i - 4) / 4) * s_, [60 - (i % 3) * 10, 0, 0], taper_mat("#8A7448", "#FFF4A0"), 1.2)
            nb.cubes += spike(0, (p["hy"] + 0.4) * s_, (p["hz"] + 1.2) * s_, 2 * s_, [55, 0, 0], taper_mat("#8A7448", "#FFF4A0"), 1.2)
        if kind == "blaze":
            # Its spots and rings glow like embers.
            for nm in ["body", "neck", "head"] + list(p["tail"]):
                for c in bone_of(m, nm).cubes:
                    if not c.mat.flat:
                        c.mat.glow = lambda a: rgb("#FFB050") if hash01("spot", int(a.x / 1.4), int(a.y / 1.4), int(a.z / 1.4)) < 0.2 and a.face in ("top", "nx", "px", "back") else None
    coat = spot_coat(SPOT, 1.4, 0.2 if kind != "kit" else 0.1, faces=("top", "nx", "px", "back"))
    tailm = ringed(FUR, SPOT if kind != "tide" else "#2A3E4C", glow="#FFB050" if kind == "blaze" else None)
    quad(sid, name, stands, dict(
        s=s, body=(4.4, 4, 11) if kind != "kit" else (4.2, 3.8, 8.4), top=4.4 if kind != "kit" else 3.6, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=CREAM, coat=coat,
        FACE=FUR, face_coat=lambda a: rgb(CREAM) if a.face == "front" and 0.3 < a.ly < 0.5 and abs(a.lx - 0.5) > 0.12 else None,
        head=(3.8, 3.4, 4) if kind != "kit" else (4.2, 3.8, 4), snout=2.2 if kind != "kit" else 1.8, snout_w=0.75, snout_h=1.6, MUZZLE=CREAM, NOSE="#2A1A14",
        neck=2.8, neck_up=0.6, neck_tilt=-12, ears=("rabbit", 2.4 if kind == "kit" else 2.2, 3.2 if kind == "kit" else 3.0), EAR=FUR,
        EAR_IN="#F0B0A0" if kind != "blaze" else "#5A4A42", EAR_TIP=None, IRIS=IRIS, IRIS_L=IRIS_L, eye_grid=["LL", "IP", "II"],
        eye_h=3, eye_y=0.42, leg_w=1.4, toes=2, webbed=kind == "tide",
        tail={"segs": [(1.6, 1.6, 3), (1.6, 1.6, 3), (1.5, 1.5, 3), (1.4, 1.4, 3), (1.3, 1.3, 2.6)] if kind != "kit" else [(1.4, 1.4, 2.6), (1.4, 1.4, 2.6), (1.3, 1.3, 2.6), (1.2, 1.2, 2.2)],
              "rests": [8, 6, 2, -4, -6] if kind != "kit" else [14, 8, 2, -4], "mat": tailm, "y": 0.6},
        extra=extra, moves={"kit": ("TACKLE", "SAND-ATTACK"), "tide": ("BITE", "WATER GUN"), "volt": ("DOUBLE KICK", "THUNDERSHOCK"), "blaze": ("BITE", "EMBER")}[kind],
        walk=0.5, stride=32, k=0.7))


def genkit():
    genet_kin("eevee", "GENKIT", 0.3, "kit")


def tidegenet():
    genet_kin("vaporeon", "TIDEGENET", 1.0, "tide")


def voltgenet():
    genet_kin("jolteon", "VOLTGENET", 0.8, "volt")


def blazegenet():
    genet_kin("flareon", "BLAZEGENET", 0.9, "blaze")


# ======================================================================================================
# The grown ermine: ERMINELLE (in place of Jynx; from FROSTLET)
# ======================================================================================================

def erminelle():
    """The ermine kit grown into a lady of the snows: long and elegant, white as new snow, the jet tip of her tail, a
    deep ruff of frosted fur falling about her like a gown trimmed in ermine, violet eyes, a rosy pout, and she sings a
    song that sends you to sleep."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        # (A long fall of frosted fur, flaring out to a scalloped hem, silvered with frost: no black dashes.)
        gown = Mat("#F4F8FF", lambda a: CLEAR if 0.12 * (1 + math.cos(a.lx * math.pi * 8)) > a.ly else
                   (rgb("#E8F4FF") if hash01("frost", a.col, a.row) < 0.06 else mix("#B8C8E4", "#FFFFFF", min(1, a.ly * 1.3))),
                   glow=lambda a: rgb("#F0F8FF") if hash01("frost", a.col, a.row) < 0.06 else None, noise=0.01, edge=0, flat=True)
        hip, bw, bd = p["hip"], p["bw"], p["bd"]
        top = hip + 2
        for sg in (-1, 1):
            x = sg * (bw / 2 + 0.4)
            b.cubes.append(Cube(S([x, top - 5.6, -bd / 2 - 0.8]), [0, n(5.6 * s), n((bd + 1.6) * s)], gown, [0, 0, sg * -14], S([x, top, 0])))
            z = sg * (bd / 2 + 0.4)
            b.cubes.append(Cube(S([-bw / 2 - 0.8, top - 5.6, z]), [n((bw + 1.6) * s), n(5.6 * s), 0], gown, [sg * 14, 0, 0], S([0, top, z])))
        b.cubes.append(Cube(S([-bw / 2 - 0.4, hip + p["bh"] - 1.8, -bd / 2 - 0.5]), [n((bw + 0.8) * s), n(1.6 * s), n((bd + 1) * s)],
                            Mat("#E8F4FF", lambda a: rgb("#C8E4FF") if a.row % 2 else None, glow=lambda a: rgb("#E8F8FF") if a.col % 3 == 0 else None, noise=0.02)))
    upright("jynx", "ERMINELLE", 1.4, dict(
        s=1.5, hip=4.4, body=(5, 7, 4.4), FUR="#F4F6FA", FUR_L="#FFFFFF", FUR_D="#C8D0DC", BELLY="#FFFFFF", FACE=None, head=(5, 4.4, 4.6), head_y=-0.2,
        snout=(1.6, 2, 1.4), NOSE="#F08AA0", MUZZLE="#FFFFFF", MOUTH="#E86A8A", ears=("round", 2, 2), EAR_IN="#F0C8D0", IRIS="#8A4AC8", IRIS_L="#C89AF0",
        eye_in=0.7, eye_y=0.56, eye_grid=["LL", "IP"], arms=(2.6, 2.4, 1.4), leg_w=1.6, foot=2.8, toes=2, thigh=2.4, FOOT="#E8ECF4",
        tail={"segs": [(1.4, 1.4, 2.6), (1.4, 1.4, 2.6), (1.4, 1.4, 2.4)], "rests": [-40, 20, 20], "y": 0.6, "tip": "#141418"}, extra=extra,
        moves=("LOVELY KISS", "ICE PUNCH"), walk=0.55, guard=20, k=0.8))


# ======================================================================================================
# The grown numbat: STRIPEVOLT (in place of Electabuzz; from NUMBOLT)
# ======================================================================================================

def stripevolt():
    """The numbat joey grown up and fizzing: tall on its hind legs, rust-red shading to near black over its rump, banded
    across its back with white, a cream chest, a long pointed snout with a dark stripe through each eye, strong digging
    claws, and a great bottlebrush of a tail that crackles blue-white with static when it's cross."""
    def coat(at, base):
        if at.face in ("top", "back", "nx", "px") and at.z > -0.6 and (at.y % 2.4) < 0.9 and at.y > 7: return rgb("#F4ECDC")
        if at.face in ("top", "back", "nx", "px") and at.y < 9: return mix("#2A1E18", "#8A3A20", min(1, max(0, (at.y - 4) / 5)))
        return None

    def face_coat(a):
        if a.face == "front" and 0.5 < a.ly < 0.62: return rgb("#1A1410")
        return None
    tailm = Mat("#5A3A2A", brush("#3A2A20", "#8A6A5A", "#1A1410", tip="#E8F4FF"), glow=lambda a: rgb("#E8F4FF") if a.lz > 0.8 else None, noise=0, edge=0)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        m.pair("whiskers_right", "head", S([-1, p["hy"] + 0.8, p["fz"] - 2.4]), [Cube(S([-4, p["hy"] + 0.8, p["fz"] - 2.8]), [n(3 * s), 0, n(1.4 * s)], whisker_mat("#E8F4FF"))])
        for c in bone_of(m, "whiskers_right").cubes + bone_of(m, "whiskers_left").cubes:
            c.mat.glow = lambda a: rgb("#E8F4FF")
        for nm in p["tail"]:
            tb = bone_of(m, nm)
            tb.cubes += [Cube([-2.4 * s, tb.pivot[1], tb.pivot[2] - 0.4], [n(4.8 * s), 0, n(2.6 * s)], tailm), Cube([0, tb.pivot[1] - 2.4 * s, tb.pivot[2] - 0.4], [0, n(4.8 * s), n(2.6 * s)], tailm)]
    upright("electabuzz", "STRIPEVOLT", 1.1, dict(
        s=1.5, hip=4.4, body=(6, 7.4, 5), FUR="#B04A2A", FUR_L="#D8784A", FUR_D="#6A2A18", BELLY="#F4E4C8", coat=coat, belly_size=(3.6, 5), FACE="#C86A40",
        face_coat=face_coat, face_line=0.4, head=(4.6, 3.8, 4.4), head_y=-0.4, snout=(3.2, 1.8, 1.6), NOSE="#1A1410", MUZZLE="#F4E4C8", ears=("round", 1.8, 2),
        EAR_IN="#E8A080", IRIS="#2A1A0A", IRIS_L="#7A5A2A", eye_in=0.6, eye_y=0.58, eye_grid=["LL", "IP"], arms=(3, 3, 2), HAND="#5A2A18", claws=3,
        leg_w=2.2, foot=3.6, toes=3, FOOT="#5A2A18", thigh=3.2, tail={"segs": [(1.4, 1.4, 2.6), (1.4, 1.4, 2.6), (1.4, 1.4, 2.4)], "rests": [-20, 20, 20], "y": 0.8},
        extra=extra, moves=("THUNDERPUNCH", "THUNDERBOLT"), guard=50, k=0.9))


# ======================================================================================================
# The grown fire salamander: PYRONEWT (in place of Magmar; from EMBERNEWT)
# ======================================================================================================

def pyronewt():
    """The fire salamander eft grown into its fire: a big glossy salamander standing on its hind legs, jet black
    splashed with blotches of orange and gold that glow like coals, a crest of small flames down its back and along its
    heavy tail, a broad head with a wide mouth and bright gold eyes."""
    lit = lambda a: hash01("blot", int(a.x / 1.8), int(a.y / 1.8), int(a.z / 1.8)) < 0.3 and a.face != "bottom"

    def coat(at, base):
        if lit(at): return rgb("#F89A20")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        for c in bone_of(m, "body").cubes + bone_of(m, "head").cubes:
            if not c.mat.flat:
                c.mat.glow = lambda a: rgb("#FFB040") if lit(a) else None
        b = bone_of(m, "body")
        top = p["hip"] + p["bh"]
        for i, z in enumerate((-1.6, 0, 1.6)):
            b.cubes += flame(S([-1, top - 0.8, z - 1 + 1.4]), n(2 * s), n((3 - abs(i - 1)) * s), heat=1.1)
        for nm in p["tail"][1:]:
            tb = bone_of(m, nm)
            c = tb.cubes[0]
            tb.cubes += flame([-1 * s, c.origin[1] + c.size[1] - 0.4, c.origin[2] + 0.4], n(2 * s), n(2.4 * s), heat=1.0)
    upright("magmar", "PYRONEWT", 1.3, dict(
        s=1.5, hip=4, body=(7, 7.6, 5.6), FUR="#1E1A1E", FUR_L="#3E363E", FUR_D="#0A080A", BELLY="#2A2228", coat=coat, belly_patch=False, FACE=None,
        face_coat=lambda a: rgb("#F89A20") if lit(a) and a.face in ("top", "nx", "px") else None, head=(6, 4, 5.4), head_y=-0.6, nose=False, mouth_y=0.2, mouth_w=4.4,
        MUZZLE="#2A2228", MOUTH="#E8501A", ears=None, IRIS="#E8A020", IRIS_L="#FFE07A", EYE_SKIN="#1E1A1E", eye_in=1.2, eye_y=0.6, eye_grid=["LL", "IP"],
        arms=(2.8, 2.8, 2), HAND="#2A2228", fingers=4, leg_w=2.4, foot=3.6, toes=4, FOOT="#2A2228", thigh=3.4,
        tail={"segs": [(3, 2.6, 2.6), (2.4, 2.2, 2.6), (1.8, 1.6, 2.4), (1.4, 1.2, 2.2)], "rests": [-30, -6, 2, 4], "y": 0.8}, extra=extra,
        moves=("FIRE PUNCH", "FLAMETHROWER"), guard=40, k=0.9))


# ======================================================================================================
# The tiger beetle: JADEJAW (in place of Scyther; before VERMICLAW)
# ======================================================================================================

def jadejaw():
    """A tiger beetle standing tall, the fastest hunter of the sand: a shell of metallic emerald with cream spots on its
    wing cases, long slender legs, huge bulging eyes, and great curved sickle jaws edged with teeth; it runs its prey
    down so fast it has to stop to see again."""
    G, G_L, G_D = "#2A9A5A", "#7AE0A0", "#0E4A2A"
    shellm = Mat(G, lambda a: mix(G_D, G_L, 0.2 + 0.7 * a.ly) if a.face != "top" else mix(G, G_L, 0.6),
                 lambda a: rgb("#B8F0FF") if a.face in ("front", "nx", "px") and a.ly > 0.85 and a.col % 3 == 0 else None, noise=0.015)
    jaw = Mat("#F0E0B0", lambda a: mix("#8A6A2A", "#FFF4D0", a.lz), noise=0.01, edge=0)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        hd.cubes += sickle(S, s, p["hy"] + 0.6, p["fz"], 1.6, 4.4, 1.1, jaw, plain("#FFF4D0"), out=0.9)
        bd = bone_of(m, "body")
        top = p["hip"] + p["bh"]
        cas = Mat(G, lambda a: rgb("#F4ECC8") if hash01("tb", a.col // 2, a.row // 2) < 0.12 else mix(G_D, G_L, 0.4 + 0.5 * a.ly),
                  lambda a: rgb("#0A2A18") if a.col == 0 else None, noise=0.015)
        bd.cubes += [Cube(S([-p["bw"] / 2 - 0.2, p["hip"] + 0.6, p["bd"] / 2 - 0.4]), [n((p["bw"] / 2 + 0.1) * s), n((p["bh"] - 0.8) * s), 1], cas, [8, -5, 0], S([0, top, p["bd"] / 2])),
                     Cube(S([0.1, p["hip"] + 0.6, p["bd"] / 2 - 0.4]), [n((p["bw"] / 2 + 0.1) * s), n((p["bh"] - 0.8) * s), 1], cas, [8, 5, 0], S([0, top, p["bd"] / 2]))]
        hd.cubes += [Cube(S([x - 0.5, p["hy"] + p["hh"] - 0.4, p["fz"] + 0.6]), [1, n(3.6 * s), 1], plain("#0E3A20"), [-40, 0, 24 if x > 0 else -24],
                          S([x, p["hy"] + p["hh"], p["fz"] + 0.6])) for x in (-1, 1)]
    upright("scyther", "JADEJAW", 1.5, dict(
        s=1.4, hip=7, body=(5.4, 6.4, 4.4), FUR=G, FUR_L=G_L, FUR_D=G_D, furm=shellm, BELLY="#1A3A2A", belly_size=(2.4, 4.6), FACE=None, head=(5.4, 3.6, 3.6),
        head_y=0.2, headm=shellm, nose=False, mouth_y=0.14, mouth_w=2, MUZZLE="#1A3A2A", ears=None, IRIS="#C8E040", IRIS_L="#F0FF9A", PUPIL="#1A2A04", EYE_SKIN=G,
        eye_in=1.4, eye_w=2, eye_h=3, eye_y=0.36, eye_grid=compound_grid(2, 3), arms=(3.4, 3.6, 1.2), armm=shellm, HAND=G_D, claws=2, leg_w=1.2, foot=3, legm=shellm,
        FOOT=G_D, toes=2, sharp=True, thigh=1.8, knee=0.55, extra=extra, moves=("SLASH", "SWORDS DANCE"), walk=0.45, guard=50, k=0.8))


# ======================================================================================================
# The antlion: PITJAW (in place of Pinsir)
# ======================================================================================================

def pitjaw():
    """An antlion, the terror at the bottom of the sand pit, standing up out of it: a squat bristly body the colour of
    sand, stumpy legs, a small flat head and on it a huge pair of hooked jaws lined with spines that it crushes things
    in. It flicks sand at whatever's above it."""
    SA, SA_L, SA_D = "#B89A6A", "#E0C898", "#7A6038"
    body = Mat(SA, fur(SA, SA_L, SA_D, speck=0.7), lambda a: rgb("#5A4628") if (a.row + a.col) % 4 == 0 and a.face != "bottom" else None, noise=0.03)
    jaw = Mat("#6A4A2A", lambda a: mix("#3A2614", "#C8A070", a.lz), noise=0.01, edge=0)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        # (Forward, out of the front of its face, and hooking in at the tips, spines along their inner edges: an
        # antlion's, not horns.)
        hd.cubes += sickle(S, s, p["hy"] + 0.8, p["fz"] + 0.4, 2.0, 7.0, 1.4, jaw, plain("#F0E0B0"), out=1.4, rise=0.8)
    upright("pinsir", "PITJAW", 1.5, dict(
        s=1.5, hip=3.6, body=(8, 7.6, 6.4), FUR=SA, FUR_L=SA_L, FUR_D=SA_D, furm=body, BELLY="#D8C098", belly_size=(5, 5), FACE=None, head=(5, 3, 4), head_y=-0.6,
        nose=False, mouth_y=0.2, mouth_w=3, MUZZLE="#7A6038", ears=None, IRIS="#1A1410", IRIS_L="#5A4A3A", eye_in=0.8, eye_w=2, eye_h=1, eye_y=0.55, eye_grid=["IP"],
        arms=(2.6, 2.6, 2), HAND=SA_D, claws=3, leg_w=2.4, foot=3, FOOT=SA_D, toes=3, sharp=True, thigh=3, extra=extra, moves=("VICEGRIP", "SEISMIC TOSS"),
        walk=0.7, guard=50, k=0.9))


# ======================================================================================================
# The lyre-horned bull: LYREHORN (in place of Tauros)
# ======================================================================================================

def lyrehorn():
    """An Ankole bull, the cattle of the long horns: a deep chestnut-red coat, a hump over the shoulders, a dewlap swinging
    at its throat, and enormous horns, thick at the root and sweeping up and out in a great lyre twice as wide as it is
    tall. It charges anything that moves."""
    FUR, FUR_L, FUR_D = "#9A3A22", "#C8643A", "#5A1E10"
    hornm = Mat("#E8DCC0", lambda a: mix("#B8A47A", "#F8F0DC", a.lz) if a.lz < 0.85 else rgb("#3A2A1E"), noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        y, z = p["hy"] + p["hh"] - 0.6, p["hz"] - p["hl"] * 0.5
        horn = Mat("#E8DCC0", lambda a: mix("#B8A47A", "#F8F0DC", a.ly), noise=0.02)
        tip = Mat("#3A2A1E", noise=0.01)
        steps = 14
        for side in (-1, 1):
            x0 = side * (p["hw"] / 2 - 0.6)
            for i in range(steps + 1):
                t = i / steps
                # Out along the side first, then sweeping up, the tips turning in a little: a lyre.
                x = x0 + side * (6.0 * math.sin(math.pi / 2 * min(1, t * 1.25)) - 1.4 * max(0, t - 0.7) / 0.3)
                yy = y + 0.4 + 9.0 * t ** 1.5
                zz = z + 1.2 * t
                w = 2.2 * (1 - 0.65 * t)
                hd.cubes.append(Cube(S([x - w / 2, yy - w / 2, zz - w / 2]), [n(w * s), n(w * s), n(w * s)], tip if t > 0.88 else horn))
        b = bone_of(m, "body")
        b.cubes += blob(S([-p["bw"] / 2 + 0.6, p["top"] + p["bh"] - 1.4, -p["bl"] / 2 + 0.6]), S([p["bw"] - 1.2, 2.4, 3.6]), p["furm"])
        nb = bone_of(m, "neck")
        nb.cubes.append(Cube(S([-0.6, p["hy"] - 3.6, p["hz"] - 0.6]), [n(1.2 * s), n(2.6 * s), n(3.4 * s)], p["furm"]))
    quad("tauros", "LYREHORN", 1.4, dict(
        s=1.6, body=(6.6, 6, 11), top=4.4, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#7A2A16", coat=spot_coat("#F0E0C8", 2.4, 0.08, faces=("nx", "px")), FACE=FUR,
        head=(4.6, 4.2, 4.6), snout=2.2, snout_w=1.1, snout_h=2.4, MUZZLE="#C8A48A", NOSE="#2A1A14", neck=2, neck_up=-0.6, neck_tilt=-4, head_tilt=14,
        ears=("round", 1.8, 1.4), EAR=FUR, EAR_IN="#E8A890", ear_rot=[-4, 30, -70], IRIS="#2A1A10", IRIS_L="#6A3A2A", eye_grid=["LL", "IP"], eye_y=0.55, leg_w=2,
        toes=2, foot=Mat("#2A1A14", noise=0.01), tail={"segs": [(0.8, 0.8, 3), (0.8, 0.8, 3)], "rests": [-60, -10], "y": 0.85, "tip": "#2A1A14"}, extra=extra,
        moves=("HORN ATTACK", "TAKE DOWN"), walk=0.6, stride=26, k=0.6))


# ======================================================================================================
# The oarfish: SPLASHLING > OARWYRM (in place of Magikarp, Gyarados)
# ======================================================================================================

def splashling():
    """A young oarfish, all silver and gangly: a long flat ribbon of a body much too thin for it, big round startled
    eyes, a gaping little mouth, and a crest of long red filaments streaming from its head that it can't help tangling.
    Out of the water it can only flop."""
    def paint(a):
        if a.face in ("nx", "px") and abs((a.z * 0.9) % 2 - 1) < 0.12 and a.y > 5: return rgb("#4A5A7A")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        crest = Mat("#E83A3A", lambda a: CLEAR if a.col % 2 else mix("#B81A1A", "#FF7A6A", a.lz), noise=0, edge=0, flat=True)
        for k, a in enumerate((-20, 0, 20)):
            hd.cubes.append(Cube(S([0, p["y0"] + p["hh"] / 2 - 0.4, p["hf"] + 1.4]), [0, n(2 * s), n((7 - k) * s)], crest, [-50 + a, 0, 0],
                                 S([0, p["y0"] + p["hh"] / 2 - 0.4, p["hf"] + 1.4])))
    fish("magikarp", "SPLASHLING", 0.9, dict(
        s=1.0, y=6, body=(2, 5.4, 9), B="#C8D0DC", B_L="#F4F8FF", B_D="#7A8698", BELLY="#F4F8FF", paint=paint, head=(2, 4.4, 3), IRIS="#1A1A1A", IRIS_L="#5A5A5A",
        eye_h=2, eye_grid=["WP", "PP"], FIN="#E83A3A", FIN_L="#FF8A7A", FIN_D="#A81A1A", tail_segs=[(1.6, 4, 2.6), (1.2, 3, 2.4), (1, 2, 2.2)], tail_fin=("round", 3, 2),
        dorsal=(2, 9, "spray"), dorsal_z=0, pecs=(1.6, 2, 40), extra=extra, moves=("SPLASH", "SPLASH"), k=0.8, bob=1.0))


def oarwyrm():
    """The oarfish grown into the sea serpent sailors tell of: a vast silver ribbon of a body with a blue sheen, longer
    than a ship, a blazing crimson crest running its whole length, a crown of long red rays streaming back from its
    head, and a great jaw full of teeth. When it's angry, it doesn't stop until everything's gone."""
    m = Model()
    s = 2.4
    S = S_(s)
    B, B_L, B_D = "#B8BCC8", "#EEF0F6", "#5A606E"

    def skin_f(a):
        if a.face == "bottom":
            return rgb("#E8EEF8")
        c = mix(B_D, B, 0.35 + 0.65 * a.ly)
        if a.face in ("nx", "px") and abs((a.z * 0.6) % 2 - 1) < 0.1:
            return mix(c, "#2A2E3A", 0.5)
        if hash01("sheen", round(a.x), round(a.y), round(a.z)) < 0.08:
            return mix(c, "#E8F0FF", 0.5)
        return c
    skin = Mat(B, skin_f, noise=0.012)
    crest = Mat("#E8301A", lambda a: CLEAR if a.ly > 0.95 or (a.ly > 0.7 and a.col % 2) else mix("#A81A10", "#FF6A3A", a.ly), noise=0, edge=0, flat=True)
    tooth = Mat("#F4F0E4", noise=0.01, edge=0)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 2, -1]), [Cube(S([-1.6, 0, -2.6]), [n(3.2 * s), n(4.6 * s), n(4.6 * s)], skin), Cube([0, 4.6 * s, -2.6 * s], [0, n(2.6 * s), n(4.6 * s)], crest)])
    segs, prev, z = [], "body", 2
    nseg = 12
    for i in range(nseg):
        t = i / (nseg - 1)
        w, h = (3.2 - 1.4 * t) * s, (4.6 - 2.4 * t) * s
        nm = f"seg{i + 1}"
        cubes = [Cube([-w / 2, 0, (z - 0.2) * s], [n(w), n(h), n(3.4 * s)], skin), Cube([0, h, (z - 0.2) * s], [0, n((2.6 - t) * s), n(3.4 * s)], crest)]
        if i == nseg - 1:
            cubes.append(Cube([0, -0.5 * s, (z + 3) * s], [0, n(4 * s), n(3 * s)], fin_mat("#E8301A", "#FF8A5A", "#A81A10", "round")))
        m.bone(nm, prev, [0, 1.2 * s, z * s], cubes)
        segs.append(nm)
        prev, z = nm, z + 3.2
    m.bone("neck", "body", S([0, 2.2, -2.6]), [Cube(S([-1.6, 0.2, -6.6]), [n(3.2 * s), n(4.4 * s), n(4.4 * s)], skin), Cube([0, 4.6 * s, -6.6 * s], [0, n(2.4 * s), n(4.4 * s)], crest)],
           rotation=[-60, 0, 0])
    m.bone("neck2", "neck", S([0, 2.4, -6.4]), [Cube(S([-1.6, 0.4, -10.4]), [n(3.2 * s), n(4.2 * s), n(4.2 * s)], skin), Cube([0, 4.6 * s, -10.4 * s], [0, n(2.4 * s), n(4.2 * s)], crest)],
           rotation=[-18, 0, 0])
    hz = -10.2
    m.bone("head", "neck2", S([0, 2.6, hz]), face_blob(S([-2.2, 0.6, hz - 5]), S([4.4, 4.4, 5.2]), skin), rotation=[74, 0, 0])
    hd = bone_of(m, "head")
    for k, a in enumerate((-10, 0, 10, 20)):
        hd.cubes.append(Cube(S([0, 4.6, hz - 3 + k * 0.6]), [0, n(2.2 * s), n((9 - k) * s)], crest, [-30 + a, 0, 0], S([0, 4.6, hz - 3 + k * 0.6])))
    hd.cubes += [Cube(S([x - 0.4, 0.2, hz - 4.8]), [1, n(1.2 * s), 1], tooth) for x in (-1.4, -0.4, 0.6, 1.4)]
    m.bone("jaw", "head", S([0, 1.2, hz - 1.4]), [Cube(S([-2, 0.0, hz - 4.8]), [n(4 * s), n(1.2 * s), n(4.4 * s)], Mat("#C8D4E4", open_mouth("#8A1A2A", "#C8D4E4"), noise=0.02))] +
           [Cube(S([x - 0.4, 1.2, hz - 4.6]), [1, n(1 * s), 1], tooth) for x in (-1.2, 0, 1.2)])
    m.eyes("head", 2.2 * s, 3.2 * s, (hz - 5) * s, h=1, corner=(1, 1), half=2.2 * s, iris="#E83A1A", pupil="#1A0404", skin=B, iris_light="#FF8A5A", grid=["IP"])
    fx_for(m, 0.9 * s)
    moves(m, "BITE", "HYDRO PUMP")
    coil = [30, 32, 32, 30, 26, 22, 18, 14, 10, 8, 6, 4]
    A, info = serpent("oarwyrm", m, segs, k=1.0 * s, slow=1.3, coil=coil)
    write("gyarados", "OARWYRM", m, A, info, 3.0)


# ======================================================================================================
# The sea cow: TIDESIREN (in place of Lapras)
# ======================================================================================================

def tidesiren():
    """A sea cow of the cold northern seas, as big and gentle as a boat: a great smooth grey-blue body crusted on the back
    with pale barnacles and rime, a small round head with soft whiskered lips and kind dark eyes, broad flippers and a
    wide whale's fluke of a tail. It carries people across the sea on its back, singing."""
    def paint(a):
        if a.face == "top" and hash01("barn", round(a.x * 0.8), round(a.z * 0.8)) < 0.25: return mix("#E8F0F4", "#B8C8D4", hash01("b2", round(a.x), round(a.z)))
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        hd.cubes.append(Cube(S([-1.8, p["y0"] - p["hh"] / 2 + 0.2, p["hf"] - 1]), [n(3.6 * s), n(2.4 * s), n(1.4 * s)], Mat("#9AA8B8", noise=0.02)))
        m.pair("whiskers_right", "head", S([-1.4, p["y0"] - 0.6, p["hf"] - 1]), [Cube(S([-4.4, p["y0"] - 0.6, p["hf"] - 2]), [n(3 * s), 0, n(1.6 * s)], whisker_mat("#E8EEF4"))])
        tb = bone_of(m, p["tail"][-1])
        fluke = Mat("#6A7A92", lambda a: CLEAR if a.lz > 0.6 + 0.4 * abs(a.lx - 0.5) * 2 or (a.lz > 0.75 and abs(a.lx - 0.5) < 0.1) else mix("#4A5870", "#8A9AB0", a.lz),
                    noise=0.01, edge=0, flat=True)
        tb.cubes.append(Cube([-5 * s, tb.pivot[1], p["zt"] - 0.6], [n(10 * s), 0, n(4 * s)], fluke))
    fish("lapras", "TIDESIREN", 2.5, dict(
        s=2.0, y=5, body=(8, 6, 12), B="#6A7A92", B_L="#9AAAC0", B_D="#3E4A60", BELLY="#C8D0DC", paint=paint, round=2, head=(4.4, 4, 3.6), head_dy=0.8,
        IRIS="#1A1A2A", IRIS_L="#5A5A7A", eye_grid=["WP", "PP"], eye_dy=1.0, FIN="#5A6A82", FIN_L="#8A9AB0", FIN_D="#3A4660",
        tail_segs=[(5, 4, 3), (3.4, 2.6, 3), (2, 1.6, 2.4)], tail_fin=None, dorsal=None, pecs=(5, 4, 60), flap=2.0, flap_amp=12, extra=extra,
        moves=("BODY SLAM", "ICE BEAM"), k=0.5, bob=0.4))


# ======================================================================================================
# The mimic octopus: COPYPUS (in place of Ditto)
# ======================================================================================================

def copypus():
    """A little mimic octopus that can be anything: a soft round body banded cream and brown that shifts and blotches,
    two small dark knowing eyes, a little smile, and eight soft arms it tucks and spreads to pass itself off as whatever
    is in front of it."""
    m = Model()
    s = 1.0
    S = S_(s)

    def skin_f(a):
        k = math.sin(a.y * 1.4 + math.sin(a.x * 0.9) * 1.2)
        return mix("#8A5A3A", "#B88A62", 0.5 + 0.5 * a.ly) if k > 0.2 else mix("#E8D8C0", "#FFF4E4", a.ly)
    skin = Mat("#B88A62", skin_f, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 4, 0]), blob(S([-3.4, 2.4, -3.2]), S([6.8, 5.6, 6.4]), skin, b=2))
    m.bone("head", "body", S([0, 5, -3.2]), [Cube(S([-2, 4.6, -3.4]), [n(4 * s), n(1.4 * s), 1], skin)])
    m.bone("jaw", "head", S([0, 3.8, -3.2]), [Cube(S([-0.8, 3.4, -3.5]), [n(1.6 * s), 1, 1], Mat("#5A3A2A", open_mouth("#8A3A4A", "#5A3A2A"), noise=0.02))])
    m.eyes("head", x_inner=0.9 * s, y=5.6 * s, z=-3.2 * s, w=1, h=1, iris="#0A0A0A", pupil="#000000", skin="#E8D8C0", grid=["P"])
    armm = Mat("#B88A62", lambda a: rgb("#E8D8C0") if (a.z * 1.2 + a.x) % 2.4 < 1 else None, noise=0.02)
    tents = []
    for j in range(8):
        a = j * 45 + 22
        x, z = math.sin(math.radians(a)) * 2.6, math.cos(math.radians(a)) * 2.6
        prev, names = "body", []
        for i in range(2):
            nm = f"arm{j}_{i}"
            L = 2.6 - i * 0.6
            cubes = [Cube(S([x - 0.6, 2.4 - L * (i + 1), z - 0.6]), [n(1.2 * s), n(L * s), n(1.2 * s)], armm)]
            m.bone(nm, prev, S([x, 2.6 - L * i, z]), cubes, rotation=[math.cos(math.radians(a)) * 40, 0, -math.sin(math.radians(a)) * 40] if i == 0 else [0, 0, 0])
            names.append(nm)
            prev = nm
        tents.append(names)
    fx_for(m, 0.6)
    moves(m, "TRANSFORM", "TRANSFORM")
    A, info = floater("copypus", m, tentacles=tents, k=0.5, bob=0.5)
    write("ditto", "COPYPUS", m, A, info, 0.3)


# ======================================================================================================
# The paper bird: FOLDLING (in place of Porygon; before CREASECRANE)
# ======================================================================================================

def foldling():
    """The simplest thing ever folded from a square of paper, come to life: the flapping bird, in red washi paper white
    on its back, every part a sharp folded facet: a narrow keel of a body, a long thin neck rising forward to a little
    folded beak, a thin tail rising behind, and two great triangle wings. It moves by turning itself to face a new way."""
    m = Model()
    s = 1.3
    S = S_(s)
    RED, WHITE = "#D8443A", "#F4ECD8"

    def paper(c_out, c_in):
        def f(a):
            u, v = a.lx, (a.ly if a.cube.size[1] else a.lz)
            c = c_out if a.face in ("top", "nx", "px", "front") else c_in
            if abs(u - v) < 0.05 or abs(u + v - 1) < 0.05:
                return mix(c, "#000000", 0.22)
            return mix(c, "#FFFFFF", 0.12 * v)
        return Mat(c_out, f, noise=0.006, edge=0.06)
    pa = paper(RED, WHITE)

    def tri(c_top, c_bot):
        # A right triangle of paper: its long edge along the body, its point out at the tip.
        return Mat(c_top, lambda a: CLEAR if a.lz > 1 - a.lx * 0.95 else (mix(c_top, "#000000", 0.2) if abs(a.lz - (1 - a.lx) * 0.5) < 0.03 else
                                                                          rgb(c_top if a.face == "top" else c_bot)), noise=0.005, edge=0, flat=True)
    wing = tri(RED, WHITE)
    m.bone("root", None, [0, 0, 0])
    y0 = 6
    # The keel: tall and thin, pointed below.
    m.bone("body", "root", S([0, y0, 0]), [Cube(S([-0.9, y0 - 1.6, -3]), [n(1.8 * s), n(3.2 * s), n(6 * s)], pa), Cube(S([-0.5, y0 - 2.8, -1.6]), [1, n(1.4 * s), n(3.2 * s)], pa)])
    # The neck, thin and rising forward; the head a folded point at its end.
    m.bone("neck", "body", S([0, y0 + 0.8, -2.6]), [Cube(S([-0.5, y0 + 0.3, -8.6]), [1, 1, n(6 * s)], pa)], rotation=[-38, 0, 0])
    m.bone("head", "neck", S([0, y0 + 0.8, -8.4]), [Cube(S([-0.6, y0 + 0.2, -9.6]), [n(1.2 * s), n(1.4 * s), n(1.4 * s)], pa),
                                                   Cube(S([-0.4, y0 - 0.2, -11]), [1, 1, n(1.6 * s)], pa, [30, 0, 0], S([0, y0 + 0.3, -9.6]))])
    m.bone("jaw", "head", S([0, y0 + 0.2, -9.6]), [])
    m.eyes("head", x_inner=0.6 * s, y=(y0 + 0.9) * s, z=-9.4 * s, w=1, h=1, iris="#1A1A1A", pupil="#0A0A0A", skin=RED, grid=["P"])
    m.bone("tail", "body", S([0, y0 + 0.8, 3]), [Cube(S([-0.5, y0 + 0.3, 3]), [1, 1, n(6 * s)], pa)], rotation=[34, 0, 0])
    m.pair("wing_right", "body", S([-0.9, y0 + 1.4, -1.6]), [Cube(S([-11.9, y0 + 1.4, -1.6]), [n(11 * s), 0, n(5.6 * s)], wing)], rotation=[0, 0, 28])
    m.bone("foot", "body", S([0, y0 - 2.8, 0]), [])
    fx_for(m, 0.6 * s)
    moves(m, "TRI ATTACK", "CONVERSION")
    flap = {"wing_right": track(rotation=[0, 0, wave(1.2, 24, 0, 28)]), "wing_left": track(rotation=[0, 0, wave(1.2, -24, 0, -28)]),
            "tail": track(rotation=[wave(1.2, 6, 90, 34), 0, 0]), "neck": track(rotation=[wave(1.2, 4, 90, -38), 0, 0])}
    A, info = floater("foldling", m, k=0.6 * s, bob=0.8, extras={"idle": flap, "walk": flap, "battle": flap})
    write("porygon", "FOLDLING", m, A, info, 0.8)


# ======================================================================================================
# The fossils: NAUTILET > SPIREMONITE (Omanyte, Omastar); TRILOBLET > EURYPTOR (Kabuto, Kabutops)
# ======================================================================================================

def nautilus(sid, name, stands, big):
    """A chambered nautilus, older than the dinosaurs: a coiled shell of cream barred with tiger stripes of rust, a
    pinhole eye on each side of a hooded face, and a cluster of many fine tentacles at its mouth. Grown into one of the
    great uncoiled ammonites: its shell grown out into a long spiked horn curving up behind it, knobbed and ridged, its
    tentacles thicker and hooked."""
    m = Model()
    s = 1.0 if not big else 1.6
    S = S_(s)

    def shell_f(a):
        k = math.atan2(a.y - 6 * s, a.z) * 2 + math.hypot(a.y - 6 * s, a.z) * 0.6
        if (k % 2.0) < 0.7 and a.lz > 0.2:
            return mix("#A8481E", "#C8642A", a.ly)
        return mix("#E8DCC4", "#FFF8E8", a.ly)
    shell = Mat("#E8DCC4", shell_f, noise=0.02)
    hood = Mat("#C89A7A", lambda a: rgb("#8A5A3A") if hash01("hood", a.col, a.row) < 0.2 else None, noise=0.02)
    tent = Mat("#E8B8A0", lambda a: mix("#C88A6A", "#F8D8C8", a.ly), noise=0.01, edge=0)
    m.bone("root", None, [0, 0, 0])
    y0 = 6
    cubes = blob(S([-2.6, y0 - 4.4, -2]), S([5.2, 8.8, 7]), shell, b=2)
    if not big:
        cubes += blob(S([-2.2, y0 + 3.4, 0]), S([4.4, 2.6, 4.4]), shell)
    else:
        knob = taper_mat("#C8642A", "#FFF4E0")
        for i in range(5):
            y, z = y0 + 3 + i * 1.8, 2 + i * 1.4 - (i * i) * 0.2
            w = 4.2 - i * 0.6
            cubes += blob(S([-w / 2, y, z - w / 2]), S([w, 2.4, w]), shell)
            cubes += spike(0, (y + 2) * s, z * s, 1.6 * s, [60, 0, 0], knob, 1.2)
    m.bone("body", "root", S([0, y0, 0]), cubes)
    m.bone("head", "body", S([0, y0, -2]), face_blob(S([-2.2, y0 - 2.6, -4.4]), S([4.4, 3.8, 2.8]), hood) +
           [Cube(S([-2.4, y0 + 1.2, -4.6]), [n(4.8 * s), n(1.6 * s), n(3.4 * s)], hood)])
    m.bone("jaw", "head", S([0, y0 - 2.4, -4.4]), [])
    m.eyes("head", 2.2 * s, (y0 - 0.4) * s, -4.4 * s, h=1, corner=(1, 1), half=2.2 * s, iris="#0A0A0A", pupil="#000000", skin="#C89A7A", iris_light="#5A5A5A", grid=["WP"])
    tents = []
    for j, (x, y) in enumerate(((-1.4, y0 - 2.4), (0, y0 - 2.6), (1.4, y0 - 2.4), (-0.7, y0 - 1.4), (0.7, y0 - 1.4))):
        prev, z = "head", -4.4
        names = []
        for i in range(2):
            nm = f"tent{j}_{i}"
            m.bone(nm, prev, S([x, y, z]), [Cube(S([x - 0.4, y - 0.4, z - 2.2]), [1, 1, n(2.4 * s)], tent)], rotation=[24 if i == 0 else 10, 0, 0])
            names.append(nm)
            prev, z = nm, z - 2.2
        tents.append(names)
    fx_for(m, 0.6 * s)
    moves(m, "WITHDRAW" if not big else "SPIKE CANNON", "WATER GUN" if not big else "HYDRO PUMP")
    A, info = floater(name.lower(), m, tentacles=tents, k=0.5 * s, bob=0.6)
    write(sid, name, m, A, info, stands)


def nautilet():
    nautilus("omanyte", "NAUTILET", 0.4, False)


def spiremonite():
    nautilus("omastar", "SPIREMONITE", 1.0, True)


def trilobite():
    """A trilobite, one of the first creatures with eyes: a flat oval shield in three lobes, its middle lobe raised, a
    round head shield with a swollen brow, cheek spines sweeping back along its sides, crescent eyes of many lenses, its
    body ribbed across in segments that narrow to a rounded tail shield, and rows of feathery legs beneath."""
    m = Model()
    s = 1.1
    S = S_(s)
    SL, SL_L, SL_D = "#6E6458", "#A89C88", "#3A342C"
    shell = Mat(SL, lambda a: mix(SL_D, SL_L, 0.25 + 0.6 * a.ly) if a.face != "top" else (rgb(SL_D) if a.lz < 0.12 else mix(SL, SL_L, 0.3 + 0.4 * abs(a.lx - 0.5) * 2)),
                noise=0.025)
    ridge = Mat(SL_L, lambda a: mix(SL, "#D8CCB4", a.ly) if a.face != "top" else (rgb(SL_D) if a.lz < 0.15 else rgb("#C8BCA4")), noise=0.02)
    legm = Mat("#C8A07A", noise=0.02, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 1.4, 0]), [Cube(S([-3.6, 0.6, -2]), [n(7.2 * s), n(1.4 * s), n(3 * s)], shell), Cube(S([-1.2, 2.0, -2]), [n(2.4 * s), n(1 * s), n(3 * s)], ridge)])
    # The head shield: round in front (narrower blocks stepping in), its brow (the glabella) swollen in the middle.
    hc = [Cube(S([-4, 0.6, -4]), [n(8 * s), n(1.4 * s), n(2 * s)], shell), Cube(S([-3.2, 0.6, -5.4]), [n(6.4 * s), n(1.4 * s), n(1.4 * s)], shell),
          Cube(S([-2, 0.6, -6.4]), [n(4 * s), n(1.4 * s), n(1 * s)], shell), Cube(S([-1.3, 2.0, -6]), [n(2.6 * s), n(1.4 * s), n(4 * s)], ridge)]
    for side in (-1, 1):
        # Cheek spines from the back corners of the head shield, sweeping back along its sides.
        hc += spike(side * 3.9 * s, 1.3 * s, -2.2 * s, 5 * s, [0, side * -8, 0], taper_mat(SL, "#E8DCC4"), 1)
        # A crescent eye on each cheek, raised.
        hc.append(Cube(S([side * 2.2 - 0.6, 2.0, -4.6]), [n(1.2 * s), n(0.8 * s), n(1.6 * s)], Mat("#2A2A24", lambda a: rgb("#8A8A6A") if (a.col + a.row) % 2 else None, noise=0)))
    m.bone("head", "body", S([0, 1.4, -2]), hc)
    m.bone("jaw", "head", S([0, 0.8, -5]), [])
    m.eyes("head", 2.8 * s, 2.6 * s, -4.4 * s, w=1, h=1, iris="#1A1A14", pupil="#000000", skin=SL, iris_light="#6A6A5A", grid=["P"])
    segs, prev, z = [], "body", 1
    for i in range(5):
        w = 7.0 - i * 1.0
        nm = f"seg{i + 1}"
        cubes = [Cube(S([-w / 2, 0.6, z - 0.1]), [n(w * s), n(1.2 * s), n(1.8 * s)], shell), Cube(S([-1.1, 1.8, z - 0.1]), [n(2.2 * s), n(0.8 * s), n(1.8 * s)], ridge)]
        cubes += [Cube(S([x, 0.2, z + 0.2]), [n(1.8 * s), 0, n(1.2 * s)], legm, [0, 0, 10 if x < 0 else -10], S([x + (1.8 if x < 0 else 0), 0.2, z + 0.6]))
                  for x in (-w / 2 - 1.2, w / 2 - 0.6)]
        m.bone(nm, prev, S([0, 1.4, z]), cubes)
        segs.append(nm)
        prev, z = nm, z + 1.8
    # The tail shield, round.
    bone_of(m, segs[-1]).cubes += [Cube(S([-2, 0.6, z]), [n(4 * s), n(1.2 * s), n(1.4 * s)], shell), Cube(S([-1.2, 0.6, z + 1.4]), [n(2.4 * s), n(1 * s), n(1 * s)], shell)]
    m.pair("antenna_right", "head", S([-1, 1.4, -6]), [Cube(S([-1.5, 1.4, -9]), [1, 0, n(3 * s)], legm, [0, 20, 0], S([-1, 1.4, -6]))])
    fx_for(m, 0.6 * s)
    moves(m, "SCRATCH", "HARDEN")
    A, info = crawler("triloblet", m, segs, walk=0.8, k=0.3, slow=1.2)
    write("kabuto", "TRILOBLET", m, A, info, 0.5)


def euryptor():
    """A sea scorpion of the ancient seas, risen up on its tail: a long segmented body in plates of bronze-brown, a broad
    head shield with crescent eyes, two great swimming paddles, and in front a pair of long spiny claws held up like
    scythes, raking through the water. Its tail ends in a flat spiked fan."""
    BR, BR_L, BR_D = "#8A5A3A", "#C88A5A", "#4A2E1A"
    shell = Mat(BR, lambda a: mix(BR_D, BR_L, 0.3 + 0.6 * a.ly), lambda a: rgb(BR_D) if a.row % 3 == 0 and a.face != "top" else None, noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        for side in ("forearm_right", "forearm_left"):
            b = bone_of(m, side)
            hand = b.cubes[-1]
            sg = -1 if side.endswith("right") else 1
            b.cubes[-1] = Cube([hand.origin[0], hand.origin[1] - 4 * s, hand.origin[2]], [hand.size[0], n(5.6 * s), hand.size[2]], shell)
            # Spines along the claw's front edge, pointing forward.
            for k in range(4):
                b.cubes += spike(hand.origin[0] + hand.size[0] / 2, hand.origin[1] + (0.6 - k * 1.3) * s, hand.origin[2], 2.6 * s, [0, 180, 0], taper_mat(BR_D, "#F0D8B0"), 1.4)
        b = bone_of(m, "body")
        pad = fin_mat("#A8703A", "#D8A070", "#5A3A20", "round")
        for x in (-p["bw"] / 2, p["bw"] / 2):
            b.cubes.append(Cube(S([x, p["hip"] + 1, -0.4]), [0, n(3 * s), n(4.4 * s)], pad, [0, 0, 50 if x < 0 else -50], S([x, p["hip"] + 2.4, 0])))
        tb = bone_of(m, p["tail"][-1])
        tb.cubes.append(Cube([-2 * s, tb.pivot[1] - 0.2, p.get("zt", tb.pivot[2]) + 2 * s], [n(4 * s), 0, n(3 * s)], fin_mat("#A8703A", "#D8A070", "#5A3A20", "fork")))
    upright("kabutops", "EURYPTOR", 1.3, dict(
        s=1.5, hip=4.6, body=(5, 7.6, 4.4), FUR=BR, FUR_L=BR_L, FUR_D=BR_D, furm=shell, BELLY="#C8A07A", belly_size=(3, 5.4), FACE=None, head=(5.6, 3, 4.6),
        head_y=0.2, headm=shell, nose=False, mouth_y=0.14, mouth_w=2.4, MUZZLE=BR_D, ears=None, IRIS="#1A1A14", IRIS_L="#8A8A6A", EYE_SKIN=BR, eye_in=1.6, eye_w=2,
        eye_h=1, eye_y=0.6, eye_grid=["IJ"], arms=(3.2, 2.4, 1.4), armm=shell, HAND=BR_D, leg_w=1.6, foot=2.6, legm=shell, FOOT=BR_D, toes=2, sharp=True, thigh=2.2,
        tail={"segs": [(3, 2, 2.6), (2.4, 1.6, 2.6), (1.8, 1.2, 2.4), (1.4, 1, 2.2)], "rests": [-40, -10, 6, 8], "y": 0.6}, extra=extra,
        moves=("SLASH", "HYDRO PUMP"), walk=0.6, guard=50, k=0.9))


# ======================================================================================================
# The sail-crested pterosaur: SAILCREST (in place of Aerodactyl)
# ======================================================================================================

def sailcrest():
    """A pterosaur of the old cliffs: a great tall sail of a crest on its head, banded red and gold, a long toothless beak
    like a pick, a lean body in short grey fuzz, and leathery wings stretched from its long fourth finger to its ankles,
    clawed hands at each wrist. It shrieks as it dives."""
    m = Model()
    s = 1.7
    S = S_(s)
    FUR, FUR_L, FUR_D = "#8A8A9A", "#B8B8C8", "#4A4A5A"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, speck=0.5), noise=0.03)
    beak = Mat("#D8C8A0", lambda a: rgb("#8A7A5A") if a.face == "bottom" else None, noise=0.01, edge=0)
    sail = Mat("#C83A2A", lambda a: CLEAR if a.ly > 1 - abs(a.lz - 0.35) * 1.3 else (rgb("#F0C040") if int(a.lz * 6) % 2 == 0 else mix("#A82A1A", "#E85A3A", a.ly)),
               noise=0.01, edge=0, flat=True)
    mems = [membrane_mat("#6A5A7A", "#C8A0B0", "#3A2A4A", seed=i * 2.7) for i in range(5)]
    bonem = Mat(FUR_D, noise=0.02, edge=0)
    claw = plain("#E8DCC8")
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 9, 0]), blob(S([-2.2, 7, -3]), S([4.4, 4, 6.4]), furm), rotation=[-8, 0, 0])
    m.bone("neck", "body", S([0, 10.4, -2.6]), [Cube(S([-1.1, 9.8, -5.4]), [n(2.2 * s), n(2.2 * s), n(3.2 * s)], furm)], rotation=[-30, 0, 0])
    m.bone("head", "neck", S([0, 11, -5.2]), face_blob(S([-1.6, 10, -8]), S([3.2, 2.8, 3.2]), furm) + [Cube(S([-0.8, 10.4, -12.6]), [n(1.6 * s), n(1.4 * s), n(4.6 * s)], beak)] +
           [Cube(S([0, 12.6, -8.4]), [0, n(6 * s), n(6.4 * s)], sail, [-24, 0, 0], S([0, 12.6, -6]))], rotation=[34, 0, 0])
    m.bone("jaw", "head", S([0, 10.2, -8]), [Cube(S([-0.6, 9.8, -11.8]), [n(1.2 * s), 1, n(3.8 * s)], beak)])
    m.eyes("head", 1.6 * s, 11.2 * s, -8 * s, h=1, corner=(1, 1), half=1.6 * s, iris="#E8A020", pupil="#1A0A04", skin=FUR, iris_light="#FFE07A", grid=["IP"])
    dragon_wing(m, 2 * s, 10.6 * s, -2 * s, 6 * s, (3.4 * s, 4.4 * s, 1.6 * s, 8.6 * s), bonem, mems, thick=1.2, claw=claw, under="#8A7A9A")
    legs_pair(m, "leg_right", "body", 1.2 * s, 7.4 * s, 1.8 * s, round(2.4 * s), bonem, claw)
    m.bone("tail", "body", S([0, 8.6, 3]), [Cube(S([-0.5, 8.4, 3]), [1, 1, n(3 * s)], bonem)])
    fx_for(m, 0.7 * s)
    moves(m, "WING ATTACK", "SUPERSONIC")
    A, info = flyer("sailcrest", m, flap=0.5, amp=34, rest=60, k=0.7 * s, legs=("leg_right", "leg_left"))
    write("aerodactyl", "SAILCREST", m, A, info, 1.8)


# ======================================================================================================
# The three great birds: RIMEFALCON, VOLTEAGLE, CINDERKITE (in place of Articuno, Zapdos, Moltres)
# ======================================================================================================

WING_BONES = ("wing_right", "wing_left", "wing_mid_right", "wing_mid_left", "wing_tip_right", "wing_tip_left", "wing_fan_right", "wing_fan_left",
              "wing_fan2_right", "wing_fan2_left")


def tip_glow(m, colour, names=WING_BONES):
    """The flight feathers' tips lit (each feather hangs from its top, its tip at the bottom)."""
    for nm in names:
        try:
            b = bone_of(m, nm)
        except StopIteration:
            continue
        for c in b.cubes:
            if c.mat.flat:
                c.mat.glow = lambda a, col=colour: rgb(col) if a.ly < 0.16 else None


def rimefalcon():
    """A white gyrfalcon of the far north, the largest of falcons: snow-white barred with frost-blue across its back and
    wings, a pale blue-grey hooked bill, dark fierce eyes in a white face, and a long tail edged with ice; frost forms in
    the air wherever it flies. Its wings fold neat along its flanks and open out huge, every feather barred."""
    def coat(at, base):
        if at.face in ("top", "nx", "px", "back") and int(at.z * 0.9 + at.y * 0.3) % 3 == 0: return rgb("#8AB8E0")
        return None

    def extra(p):
        tip_glow(p["m"], "#C8F0FF")
        tb = bone_of(p["m"], "tail")
        for c in tb.cubes:
            if c.mat.flat:
                c.mat = Mat("#E8F4FF", lambda a: rgb("#5A9AD8") if int(a.lz * 7) % 2 == 0 else mix("#C8E4FF", "#FFFFFF", a.ly),
                            glow=lambda a: rgb("#C8F0FF") if a.lz > 0.9 else None, noise=0.01, edge=0, flat=True)
    avian("articuno", "RIMEFALCON", 1.7, dict(
        s=2.0, body=(5.6, 6, 8.4), leg=3, B="#F4F8FF", B_L="#FFFFFF", B_D="#C8D8EC", BELLY="#FFFFFF", coat=coat, head=(3.6, 3.4, 3.6), FACE="#F4F8FF", neck_up=0.6,
        beak=1.5, beak_w=1.2, beak_h=1.4, hooked=True, BEAK="#8AA0B8", BEAK_D="#4A5A70", eye_w=2, eye_h=2, IRIS="#2A1A10", IRIS_L="#6A4A2A", eye_in=1.8, eye_back=1,
        wing=13, jointed=True, WING="#E8F4FF", WING_D="#8AB8E0", WING_L="#FFFFFF", WING_BARS="#5A9AD8", WING_EDGE="#C8F0FF", WING_UNDER="#F4FAFF",
        tail=7, TAIL="#E8F4FF", LEG="#C8D0DC", pitch=-14, trousers=True, extra=extra, moves=("PECK", "BLIZZARD"), walk=0.55, k=0.6, flap=0.5))


def volteagle():
    """A storm eagle: dark as a thundercloud with feathers of gold, the gold flashing along every edge like lightning, a
    ruff of stiff golden hackles at its neck that stand on end and crackle, a heavy yellow hooked beak and a fierce
    golden eye; thunder follows it. Its broad eagle's wings open wider than any bird's, every flight feather gold-tipped."""
    def coat(at, base):
        if at.face in ("nx", "px", "top") and hash01("bolt", round(at.x), round(at.y), round(at.z)) < 0.12: return rgb("#F8D030")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        nb = bone_of(m, "neck")
        hack = Mat("#F8D030", lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (1 - a.ly) + 0.06 else mix("#C89A10", "#FFF4A0", a.ly), glow=lambda a: rgb("#FFF4A0") if a.ly > 0.7 else None,
                   noise=0, edge=0, flat=True)
        for a in (-80, -50, -20, 20, 50, 80, 180):
            nb.cubes.append(Cube(S([-1, p["hy"] - 0.6, p["hz"] + 1.6]), [n(2 * s), n(2.8 * s), 0], hack, [-30, a, 0], S([0, p["hy"] - 0.6, p["hz"] + 1.6])))
        tip_glow(m, "#FFF07A")
        tb = bone_of(m, "tail")
        for c in tb.cubes:
            if c.mat.flat:
                c.mat = Mat("#2A2A34", lambda a: rgb("#F8D030") if a.lz > 0.82 else mix("#14141A", "#4A4A5A", a.ly), glow=lambda a: rgb("#FFF07A") if a.lz > 0.82 else None,
                            noise=0.01, edge=0, flat=True)
    avian("zapdos", "VOLTEAGLE", 1.6, dict(
        s=2.0, body=(6, 6.4, 8.4), leg=3, B="#2A2A34", B_L="#4A4A5A", B_D="#14141A", BELLY="#3A3A48", coat=coat, head=(3.8, 3.6, 3.8), FACE="#2A2A34", neck_up=0.8,
        beak=1.8, beak_w=1.4, beak_h=1.6, hooked=True, BEAK="#F8C830", BEAK_D="#B88A10", eye_w=2, eye_h=1, IRIS="#F8C830", IRIS_L="#FFF07A", eye_grid=["IP"], eye_in=1.9,
        eye_back=1, wing=14, jointed=True, WING="#2A2A34", WING_D="#14141A", WING_L="#4A4A5A", WING_EDGE="#F8D030", WING_UNDER="#5A5A6A",
        tail=6, TAIL="#2A2A34", LEG="#F8C830", pitch=-14, trousers=True, extra=extra, moves=("DRILL PECK", "THUNDER"), walk=0.55, k=0.6, flap=0.5))


def cinderkite():
    """A red kite made of fire: a rich russet body streaked darker, a pale grey head, long angled wings and a deeply
    forked tail, every flight feather tipped with flame and trailing sparks, and a smouldering amber eye. It glides on
    the heat of its own wings, which open out longer than it is."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        tip_glow(m, "#FFC040")
        tb = bone_of(m, "tail")
        fork = Mat("#C8501A", lambda a: CLEAR if (a.lz > 0.55 and abs(a.lx - 0.5) < (a.lz - 0.55) * 1.1) else (rgb("#FFD040") if a.lz > 0.85 else mix("#7A2A10", "#E8742A", a.lz)),
                   glow=lambda a: rgb("#FFC040") if a.lz > 0.85 else None, noise=0.01, edge=0, flat=True)
        y, z = p["leg"] + p["bh"] * 0.55, p["bl"] / 2 - 0.6
        tb.cubes.append(Cube(S([-2.4, y, z]), [n(4.8 * s), 0, n(8 * s)], fork, [14, 0, 0], S([0, y, z])))
        tb.cubes += flame(S([-1.2, y - 1, z + 6]), n(2.4 * s), n(3 * s), heat=1.1)
    avian("moltres", "CINDERKITE", 2.0, dict(
        s=2.1, body=(5.4, 5.6, 8.6), leg=3, B="#B8481A", B_L="#E8742A", B_D="#6A2410", BELLY="#C8642A", head=(3.4, 3.2, 3.6), FACE="#C8C8D0",
        face_coat=lambda a: mix("#9A9AA8", "#E8E8F0", a.ly) if a.face != "bottom" else None, neck_up=0.6, beak=1.5, beak_w=1.1, beak_h=1.3, hooked=True, BEAK="#F0C040",
        BEAK_D="#3A2A1A", eye_w=2, eye_h=1, IRIS="#F0A020", IRIS_L="#FFE07A", eye_grid=["IP"], eye_in=1.7, eye_back=1, wing=15, jointed=True, WING="#B8481A",
        WING_D="#6A2410", WING_L="#E8742A", WING_EDGE="#FFC040", WING_UNDER="#E8A060", tail=3, TAIL="#B8481A", LEG="#F0C040", pitch=-14, trousers=True, extra=extra,
        moves=("PECK", "FIRE SPIN"), walk=0.55, k=0.6, flap=0.5))


# ======================================================================================================
# The blue sea dragons: AZURLET > AZURIBBON > AZURDRAKE (in place of Dratini, Dragonair, Dragonite)
# ======================================================================================================

def azure_slug(sid, name, stands, stage):
    """The blue sea dragon, a sea slug that floats upside-down on the open ocean: a slender body banded silver and deep
    blue, and three pairs of feathery blue frills fanning from its sides like a dragon's limbs. The hatchling is small
    and shy; grown long and ribbon-like, its frills spread wide and gleaming, a glowing pale bead on its brow; the great
    one stands up on the waves, its frills grown into broad fanned wings it flies on, and it has carried more than one
    lost sailor home."""
    s = (1.0, 1.6, 2.0)[stage]
    B, B_L, B_D = "#3A6AD0", "#9AC4FF", "#1A3488"
    S = S_(s)

    def skin_f(a):
        if a.face == "bottom":
            return rgb("#D8E4F4")
        if a.face == "top" and abs(a.x) < 0.6 * s:
            return rgb("#E8F0FF")
        return mix(B_D, B, 0.35 + 0.65 * a.ly)
    skin = Mat(B, skin_f, noise=0.012)

    def frill_mat_():
        return Mat(B_L, lambda a: CLEAR if a.lz > 0.85 and a.col % 2 or abs(a.lx - 0.5) > 0.5 * math.sin(math.pi * min(1, a.lz * 1.05 + 0.08)) ** 0.5
                   else mix(B, "#E8F4FF", a.lz), glow=lambda a: rgb("#C8E8FF") if a.lz > 0.8 else None, noise=0, edge=0, flat=True)
    frill = frill_mat_()
    if stage < 2:
        m = Model()
        m.bone("root", None, [0, 0, 0])
        m.bone("body", "root", S([0, 1.4, -1]), [Cube(S([-1.4, 0, -2.4]), [n(2.8 * s), n(2.4 * s), n(4.2 * s)], skin)])
        segs, prev, z = [], "body", 1.6
        nseg = 7 if stage == 0 else 10
        for i in range(nseg):
            t = i / (nseg - 1)
            w, h = (2.8 - 1.6 * t) * s, (2.4 - 1.2 * t) * s
            nm = f"seg{i + 1}"
            cubes = [Cube([-w / 2, 0, (z - 0.2) * s], [n(w), n(h), n(2.8 * s)], skin)]
            if i in ((1, 3, 5) if stage == 0 else (1, 4, 7)):
                L = (3 if stage == 0 else 4.6) * s
                for side in (-1, 1):
                    cubes.append(Cube([side * w / 2 - (L if side < 0 else 0), h * 0.6, (z + 0.2) * s], [n(L), 0, n(2.4 * s)], frill, [0, side * 20, side * -10],
                                      [side * w / 2, h * 0.6, (z + 1) * s]))
            m.bone(nm, prev, [0, 1.2 * s, z * s], cubes)
            segs.append(nm)
            prev, z = nm, z + 2.6
        m.bone("neck", "body", S([0, 1.4, -2.4]), [Cube(S([-1.3, 0.1, -5.4]), [n(2.6 * s), n(2.4 * s), n(3.4 * s)], skin)], rotation=[-44, 0, 0])
        m.bone("neck2", "neck", S([0, 1.6, -5.2]), [Cube(S([-1.2, 0.2, -8]), [n(2.4 * s), n(2.4 * s), n(3 * s)], skin)], rotation=[-12, 0, 0])
        hz = -7.8
        m.bone("head", "neck2", S([0, 1.8, hz]), face_blob(S([-1.8, 0.4, hz - 3.2]), S([3.6, 3, 3.4]), skin), rotation=[54, 0, 0])
        hd = bone_of(m, "head")
        for side in (-1, 1):
            hd.cubes.append(Cube(S([side * 1.8 - (2.2 if side < 0 else 0), 2.4, hz - 1.4]), [n(2.2 * s), 0, n(2 * s)], frill, [0, side * 30, side * -30], S([side * 1.8, 2.4, hz - 1.4])))
        m.bone("jaw", "head", S([0, 0.8, hz - 1]), [Cube(S([-1.2, 0.0, hz - 3]), [n(2.4 * s), 1, n(2.8 * s)], Mat("#D8E4F4", open_mouth("#4A2A6A", "#D8E4F4"), noise=0.02))])
        m.eyes("head", 1.8 * s, 1.8 * s, (hz - 3.2) * s, h=2 if stage == 0 else 1, corner=(1, 1), half=1.8 * s, iris="#1A1A3A", pupil="#05050A", skin=B, iris_light="#5A6AB0",
               grid=["WP", "PP"] if stage == 0 else ["IP"])
        fx_for(m, 0.7 * s)
        moves(m, ("WRAP", "TWISTER", "WRAP")[stage], ("THUNDER WAVE", "DRAGON RAGE", "DRAGON RAGE")[stage])
        coil = ([30] * nseg)
        A, info = serpent(name.lower(), m, segs, k=0.6 * s, slow=1.0 + 0.2 * stage, coil=coil)
        write(sid, name, m, A, info, stands)
        return

    # The great one, standing up on the waves.
    def extra(p):
        m, S_s, s_ = p["m"], p["S"], p["s"]
        b = bone_of(m, "body")
        top = p["hip"] + p["bh"]
        wing = frill_mat_()
        # (13:18: "about wingspans") Its frills grown into great fanned wings, each longer than it is tall, held half open.
        m.pair("wing_right", "body", S([-p["bw"] / 2 + 0.6, top - 1, 1.6]), [Cube(S([-p["bw"] / 2 - 16, top - 1, -1]), [n(16.4 * s_), 0, n(8 * s_)], wing),
                                                                            Cube(S([-p["bw"] / 2 - 12, top - 1.2, 5]), [n(12 * s_), 0, n(5 * s_)], wing, [0, -14, 0],
                                                                                 S([-p["bw"] / 2, top - 1.2, 5]))], rotation=[0, -20, 34])
        hd = bone_of(m, "head")
        for side in (-1, 1):
            hd.cubes.append(Cube(S([side * p["hw"] / 2 - (3 if side < 0 else 0), p["hy"] + p["hh"] - 0.6, p["hz"]]), [n(3 * s_), 0, n(2.6 * s_)], frill, [0, side * 30, side * -40],
                                 S([side * p["hw"] / 2, p["hy"] + p["hh"] - 0.6, p["hz"]])))
        for z in (-0.6, 1.6):
            for side in (-1, 1):
                b.cubes.append(Cube(S([side * p["bw"] / 2 - (3 if side < 0 else 0), p["hip"] + 2, z]), [n(3 * s_), 0, n(2 * s_)], frill, [0, side * 20, side * 30],
                                    S([side * p["bw"] / 2, p["hip"] + 2, z])))
    flap = {"wing_right": track(rotation=[0, -20, wave(1.6, 18, 0, 34)]), "wing_left": track(rotation=[0, 20, wave(1.6, -18, 0, -34)])}
    upright("dragonite", "AZURDRAKE", 2.2, dict(
        s=s, hip=4, body=(8, 9, 7), FUR=B, FUR_L=B_L, FUR_D=B_D, furm=skin, BELLY="#D8E4F4", belly_size=(5.4, 7), FACE=None, head=(5.6, 4.6, 5), head_y=-0.6,
        snout=(1.6, 3.2, 2), NOSE="#1A2A5A", MUZZLE="#D8E4F4", ears=None, IRIS="#1A1A3A", IRIS_L="#5A6AB0", eye_in=1, eye_y=0.6, eye_grid=["LL", "IP"],
        arms=(2.6, 2.6, 2), HAND=B_D, claws=3, leg_w=2.6, foot=3.6, FOOT=B_D, toes=3, thigh=3.6,
        tail={"segs": [(3.4, 3, 3), (2.8, 2.4, 3), (2.2, 1.8, 3), (1.6, 1.4, 2.6)], "rests": [-34, -8, 4, 8], "y": 0.6}, extra=extra,
        moves=("WING ATTACK", "HYPER BEAM"), walk=0.7, guard=30, k=0.9, anim_extras={"idle": flap, "battle": flap, "walk": flap}))


def azurlet():
    azure_slug("dratini", "AZURLET", 0.5, 0)


def azuribbon():
    azure_slug("dragonair", "AZURIBBON", 1.2, 1)


def azurdrake():
    azure_slug("dragonite", "AZURDRAKE", 2.2, 2)


# ======================================================================================================
# The galagos: BUSHLET (Mew), PSYCLONE (Mewtwo)
# ======================================================================================================

def bushlet():
    """A little bushbaby no one has ever caught: huge round amber eyes for the night, great thin ears it can fold like
    fans, a soft silver-fawn coat paling to cream, long slender fingers and toes, and a long bushy tail it curls about
    itself as it floats along in the air, laughing."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        brushm = Mat("#C8B49A", brush("#A8947A", "#E8DCC8", "#7A6A58", tip="#F4ECDC"), noise=0, edge=0)
        for nm in p["tail"][2:]:
            b = bone_of(m, nm)
            b.cubes += [Cube([-1.4 * s, b.pivot[1], b.pivot[2] - 0.2], [n(2.8 * s), 0, n(2.8 * s)], brushm), Cube([0, b.pivot[1] - 1.4 * s, b.pivot[2] - 0.2], [0, n(2.8 * s), n(2.8 * s)], brushm)]
    floaty = {"body": track(position=[0, wave(2.4, 1.2, 0, 3.0), 0])}
    upright("mew", "BUSHLET", 0.4, dict(
        s=1.0, hip=2.6, body=(4.4, 4.6, 4), FUR="#B8A890", FUR_L="#E0D4C0", FUR_D="#7A6A58", BELLY="#F4ECDC", FACE=None, head=(5.4, 4.6, 4.6), head_y=-0.4,
        face_coat=lambda a: rgb("#F4ECDC") if a.face == "front" and abs(a.lx - 0.5) < 0.08 and a.ly > 0.3 else None,
        snout=(0.8, 1.8, 1.2), NOSE="#3A2A20", MUZZLE="#F4ECDC", ears=("round", 3.6, 4.4), EAR_IN="#E8C8B0", ear_x=2.4, ear_rot=[-6, 10, -30], IRIS="#7A4A10",
        IRIS_L="#D8962A", PUPIL="#1A0E04", EYE_SKIN="#3A2A20", eye_in=0.5, eye_w=2, eye_h=3, eye_y=0.38, eye_grid=["PW", "II", "KI"], arms=(1.8, 1.6, 1), HAND="#9A8A72",
        fingers=3, leg_w=1.4, foot=3, toes=3, FOOT="#9A8A72", thigh=2,
        tail={"segs": [(0.8, 0.8, 2.6), (0.8, 0.8, 2.6), (0.8, 0.8, 2.6), (0.8, 0.8, 2.6), (0.8, 0.8, 2.4)], "rests": [10, 20, 20, 30, 30], "y": 0.6}, extra=extra,
        moves=("POUND", "PSYCHIC"), anim="hopper", hop=0.5, height=1.6, anim_extras={"idle": floaty}))


def psyclone():
    """A bushbaby made, not born: grown tall and gaunt in a laboratory from the little one's hair, a coat of slate and
    soot, a pale grey chest, enormous eyes ringed with a cold blue light that see through walls, long thin arms with
    three long fingers, and a great heavy brush of a tail tipped with the same light. It floats when it fights, and it
    remembers everything."""
    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        brushm = Mat("#4A4E58", brush("#3A3E48", "#7A808C", "#1E2028", tip="#C8F0FF"), glow=lambda a: rgb("#A8E8FF") if a.lz > 0.85 else None, noise=0, edge=0)
        for nm in p["tail"][2:]:
            b = bone_of(m, nm)
            b.cubes += [Cube([-2 * s, b.pivot[1], b.pivot[2] - 0.2], [n(4 * s), 0, n(3.2 * s)], brushm), Cube([0, b.pivot[1] - 2 * s, b.pivot[2] - 0.2], [0, n(4 * s), n(3.2 * s)], brushm)]
    upright("mewtwo", "PSYCLONE", 2.0, dict(
        s=1.9, hip=5.4, body=(5.4, 7.4, 4.4), FUR="#5A5E68", FUR_L="#8A909C", FUR_D="#2A2C34", BELLY="#B8BCC4", belly_size=(3.2, 5.4), FACE=None, head=(4.8, 4.2, 4.4),
        head_y=-0.2, snout=(0.8, 1.8, 1.2), NOSE="#1A1C22", MUZZLE="#8A909C", ears=("round", 3.0, 3.8), EAR_IN="#3A3E48", ear_x=2.2, ear_rot=[-6, 10, -20],
        IRIS="#2A6A9A", IRIS_L="#A8E8FF", PUPIL="#05080C", EYE_SKIN="#1A1C22", eye_in=0.5, eye_w=2, eye_h=3, eye_y=0.38, eye_grid=["LL", "PI", "II"], arms=(3.6, 3.6, 1.4),
        HAND="#3A3E48", fingers=3, leg_w=1.8, foot=3.6, toes=3, FOOT="#3A3E48", thigh=2.6,
        tail={"segs": [(2.4, 2.4, 3), (2.2, 2.2, 3), (2, 2, 3), (1.8, 1.8, 3), (1.6, 1.6, 2.6)], "rests": [-40, -6, 10, 14, 18], "y": 0.6}, extra=extra,
        moves=("PSYCHIC", "BARRIER"), walk=0.8, guard=20, k=0.9))


def main(names):
    for f in ALL10:
        if not names or f.__name__ in names:
            f()
    import mons4
    mons4.refresh_names()


ALL10 = (genkit, tidegenet, voltgenet, blazegenet, erminelle, stripevolt, pyronewt, jadejaw, pitjaw, lyrehorn, splashling, oarwyrm, tidesiren, copypus,
         foldling, nautilet, spiremonite, trilobite, euryptor, sailcrest, rimefalcon, volteagle, cinderkite, azurlet, azuribbon, azurdrake, bushlet, psyclone)

if __name__ == "__main__":
    main(set(sys.argv[1:]))
