"""
Retromon's own creatures, part six: more of Johto's (John, 2026-11-24 12:00: "build out our Retromon please for the
additional Johto"). Built as part five's were: our own designs drawn from real animals, plants and things, the same
species underneath. Two more shared bodies here (an upright one and a swimmer) and the creatures built on them.

Run: python3 tools/mons6.py [names]   (all if none; refreshes mons/names.json either way)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, cell, face_blob, hash01, mix, rgb, track, wave, keys, write, flame  # noqa: E402
from monanims import four_legs, bird, floater, insect, biped, serpent, flyer, hopper  # noqa: E402
from mons import fx_for, open_mouth, shag_mat, stout_legs, folded_wing, quail_feet, whisker_mat, horn_mat, leaf_mat, broad_leaf, membrane_mat, dragon_wing  # noqa: E402
from mons2 import fur, brush, n  # noqa: E402
from mons3 import moves, bone_of, tail_bones, ear_mat, spike, taper_mat, scale_mat, petal_mat, legs_pair, glow_patch, upright_legs, upright_arms  # noqa: E402
from mons5 import quad, avian, bug, S_, first, solid, plain, antlers, back_row, spot_coat, stripe_coat, both  # noqa: E402


# ======================================================================================================
# An upright body: standing on two legs, arms at its sides
# ======================================================================================================

def upright(sid, name, stands, o):
    """A creature that stands on two legs, from its description o: its size (s), its hips' height (hip), its body (w,
    h, d) and coat (FUR, FUR_L, FUR_D, BELLY, a painter `coat`, a belly patch), its head (hw, hh, hd, how far forward
    and up), a snout, ears, eyes, arms (upper, fore, w, its hands), legs (w, feet, toes), a tail, and `extra` for what's
    its own. anim: biped (it walks) or hopper (it hops)."""
    m = Model()
    s = o.get("s", 1.0)
    S = S_(s)
    FUR, FUR_L, FUR_D = o["FUR"], o.get("FUR_L", o["FUR"]), o.get("FUR_D", o["FUR"])
    BELLY = o.get("BELLY", FUR_L)
    hip = o.get("hip", 4)
    bw, bh, bd = o.get("body", (7, 7, 6))
    base = fur(FUR, FUR_L, FUR_D, speck=o.get("speck", 0.35))
    coat = o.get("coat")
    furm = o.get("furm") or Mat(FUR, (lambda a: first(coat(a, base), base, a)) if coat else base, noise=0.02)
    m.bone("root", None, [0, 0, 0])
    cubes = blob(S([-bw / 2, hip - 0.5, -bd / 2]), S([bw, bh, bd]), furm, b=o.get("round", 1))
    if o.get("belly_patch", True):
        bp = Mat(BELLY, *([o["belly_paint"]] if o.get("belly_paint") else []), noise=0.02)
        bpw, bph = o.get("belly_size", (bw - 2.4, bh - 1.8))
        cubes.append(Cube(S([-bpw / 2, hip + o.get("belly_y", 0.4), -bd / 2 - 0.4]), [n(bpw * s), n(bph * s), 1], bp))
    m.bone("body", "root", S([0, hip + bh / 2, 0]), cubes, rotation=[o.get("lean", 0), 0, 0])
    hw, hh, hd = o.get("head", (6, 5.5, 5.5))
    hy = hip + bh - 0.8 + o.get("head_y", 0)
    hz = o.get("head_z", -0.4)
    fz = hz - hd / 2
    FACE = o.get("FACE", BELLY)
    line = o.get("face_line", 0.42)
    face_c = o.get("face_coat")
    default_face = (lambda a: rgb(FACE) if FACE and ((a.face == "front" and a.ly < line) or a.face == "bottom") else None)
    headm = o.get("headm") or Mat(FUR, base, (lambda a: first(face_c(a), default_face, a)) if face_c else default_face, noise=0.02)
    hcubes = face_blob(S([-hw / 2, hy, fz]), S([hw, hh, hd]), headm)
    sn = o.get("snout")
    NOSE = o.get("NOSE", "#3A2A22")
    MUZ = o.get("MUZZLE", FACE or FUR_L)
    if sn:
        sl, sw, sh_ = sn
        muz = Mat(MUZ, lambda a: rgb(NOSE) if a.face == "front" and a.ly > 0.5 and abs(a.lx - 0.5) < 0.3 else None, noise=0.02)
        hcubes.append(Cube(S([-sw / 2, hy + o.get("snout_y", 0.3), fz - sl]), [n(sw * s), n(sh_ * s), n(sl * s)], muz))
    elif o.get("nose", True):
        hcubes.append(Cube(S([-0.6, hy + hh * 0.36, fz - 0.4]), [n(1.2 * s), 1, 1], plain(NOSE)))
    m.bone("head", "body", S([0, hy, hz]), hcubes, rotation=[o.get("head_tilt", 0), 0, 0])
    jaw_m = Mat(MUZ, open_mouth(o.get("MOUTH", "#A83A4A"), MUZ), noise=0.02)
    if sn:
        sl, sw, sh_ = sn
        m.bone("jaw", "head", S([0, hy + 0.2, fz - 0.5]), [Cube(S([-(sw - 0.4) / 2, hy - 0.6, fz - sl + 0.2]), [n((sw - 0.4) * s), 1, n((sl + 0.4) * s)], jaw_m)])
    else:
        my = hy + hh * o.get("mouth_y", 0.2)
        mw = o.get("mouth_w", 2.4)
        m.bone("jaw", "head", S([0, my + 0.6, fz + 0.6]), [Cube(S([-mw / 2, my, fz - 0.5]), [n(mw * s), 1, n(1.4 * s)], jaw_m)])
    m.eyes("head", x_inner=o.get("eye_in", 0.8) * s, y=(hy + hh * o.get("eye_y", 0.5)) * s, z=fz * s, w=o.get("eye_w", 2), h=o.get("eye_h", 2),
           iris=o.get("IRIS", "#2A1A10"), pupil=o.get("PUPIL", "#0A0604"), skin=o.get("EYE_SKIN", FUR), iris_light=o.get("IRIS_L"),
           grid=o.get("eye_grid", ["PW", "PP"]))
    ears = o.get("ears", ("round", 2.4, 2.4))
    if ears:
        shape, ew, eh = ears
        ear = ear_mat(o.get("EAR", FUR), o.get("EAR_IN", "#E8B8A8"), shape, tip=o.get("EAR_TIP"))
        ex = o.get("ear_x", hw * 0.5 - 0.6)
        ez = o.get("ear_z", hz)
        m.pair("ear_right", "head", S([-ex, hy + hh - 0.4, ez]), [Cube(S([-ex - ew * 0.6, hy + hh - 0.8, ez]), [n(ew * s), n(eh * s), 0], ear)],
               rotation=o.get("ear_rot", [-6, 8, -16]))
    au, af, aw = o.get("arms", (3, 3, 2))
    armm = o.get("armm") or furm
    handm = o.get("handm") or Mat(o.get("HAND", FUR), noise=0.02)
    if au:
        upright_arms(m, (bw / 2 + aw / 2 - 0.3) * s, (hip + bh - 1.4 + o.get("arm_y", 0)) * s, o.get("arm_z", -0.6) * s, au * s, af * s, n(aw * s), armm, handm,
                     hw=n(o.get("hand_w", aw + 0.6) * s), hh=n(o.get("hand_h", 1.4) * s), claws=o.get("claws", 0), fingers=o.get("fingers", 0),
                     rest=o.get("arm_rest", (0, 0, 10)), fore_rest=o.get("fore_rest", (-14, 0, 0)))
    lw = o.get("leg_w", 2.4)
    legm = o.get("legm") or furm
    footm = o.get("footm") or Mat(o.get("FOOT", FUR_D), noise=0.02)
    upright_legs(m, o.get("leg_x", bw / 2 - lw / 2 - 0.4) * s, hip * s, o.get("leg_z", 0.3) * s, n(lw * s), legm, footm, n(o.get("foot", 3) * s),
                 toe_m=o.get("toem"), ntoes=o.get("toes", 3), sharp=o.get("sharp", False), thigh_w=n(o.get("thigh", lw + 0.8) * s), knee_at=o.get("knee", 0.5))
    t = o.get("tail")
    names = []
    if t:
        tm = t.get("mat") or furm
        names, zt = tail_bones(m, "body", (hip + t.get("y", 1.6)) * s, (bd / 2 - 0.6) * s, [tuple(v * s for v in seg) for seg in t["segs"]], tm, t["rests"])
        if t.get("tip"):
            tb = bone_of(m, names[-1])
            w0 = t["segs"][-1][0] * s
            tb.cubes.append(Cube([-w0 / 2 - 0.3, tb.pivot[1] - w0 / 2 - 0.3, zt - 0.6], [n(w0 + 0.6), n(w0 + 0.6), n(1.8 * s)], Mat(t["tip"], noise=0.02)))
    p = {"m": m, "s": s, "S": S, "furm": furm, "headm": headm, "hip": hip, "bw": bw, "bh": bh, "bd": bd, "hy": hy, "hz": hz, "fz": fz, "hw": hw, "hh": hh, "hd": hd,
         "tail": names}
    if o.get("extra"):
        o["extra"](p)
    fx_for(m, o.get("fx", 0.8) * s)
    moves(m, *o.get("moves", ("POUND", "POUND")))
    if o.get("anim") == "hopper":
        A, info = hopper(name.lower(), m, hop=o.get("hop", 0.45), height=o.get("height", 1.6) * s, k=o.get("k", 0.7) * s, extras=o.get("anim_extras"))
    else:
        A, info = biped(name.lower(), m, walk=o.get("walk", 0.6), stride=o.get("stride", 28), k=o.get("k", 0.7) * s, slow=o.get("slow", 1.0),
                        extras=o.get("anim_extras"), guard=o.get("guard", 30))
    write(sid, name, m, A, info, stands)


# ======================================================================================================
# A swimmer's body: a fish, a ray
# ======================================================================================================

def fin_mat(c, light, dark, shape="round", rays=True, glow=None):
    """A fin on a flat part, its root at the start (lz 0 or ly 0 depending on how it lies): round, a fork (a tail),
    a sail (tall at the front, low behind), or a spray (long ragged rays); soft rays running out along it."""
    C, Lt, D = rgb(c), rgb(light), rgb(dark)

    def f(at):
        sx = at.cube.size
        # u along the fin's length, v across it (from its root).
        if sx[0] == 0:
            u, v = at.lz, at.ly
        elif sx[1] == 0:
            u, v = at.lz, at.lx
        else:
            u, v = at.lx, at.ly
        if shape == "round":
            if (u - 0.0) ** 2 / 1.0 + (v - 0.5) ** 2 / 0.25 > 1.0:
                return CLEAR
        elif shape == "fork":
            notch = 0.55 + 0.4 * (1 - abs(v - 0.5) * 2)
            if u > 1 - 0.45 * (1 - abs(v - 0.5) * 2) ** 0.7 and u > 0.5:
                return CLEAR
            if notch < 0:
                return CLEAR
        elif shape == "sail":
            if v > 1.0 - 0.75 * u:
                return CLEAR
        elif shape == "spray":
            k = (u * 7) % 1.0
            if v > 0.95 - 0.5 * u or (v > 0.4 and k > 0.6):
                return CLEAR
        col = mix(D, Lt, 0.3 + 0.6 * u)
        if rays and abs(((v * 6) % 1.0) - 0.5) < 0.12:
            col = mix(col, D, 0.45)
        return col
    return Mat(c, f, glow=glow, noise=0.01, edge=0, flat=True)


def fish(sid, name, stands, o):
    """A swimmer from its description o: its body (w, h, l) at height y, its skin (B, B_L, B_D, BELLY, a painter
    `paint`), its head (hw, hh, hl), eyes on the sides of its head, its tail in joints with a tail fin (tail_fin: shape,
    height, length), a fin along its back (dorsal: height, length, shape), fins at its sides (pecs), and extra."""
    m = Model()
    s = o.get("s", 1.0)
    S = S_(s)
    B, B_L, B_D = o["B"], o.get("B_L", o["B"]), o.get("B_D", o["B"])
    BELLY = o.get("BELLY", B_L)
    y0 = o.get("y", 6)
    bw, bh, bl = o.get("body", (4, 5, 8))
    under = y0 - bh * 0.15

    def paint0(at):
        if at.face == "bottom" or (at.y < under * s and at.face != "top"):
            return mix(BELLY, B_L, 0.2)
        return mix(B_D, B, 0.35 + 0.65 * at.ly)
    pf = o.get("paint")
    skin = Mat(B, (lambda a: first(pf(a), paint0, a)) if pf else paint0, noise=0.015)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, y0, 0]), blob(S([-bw / 2, y0 - bh / 2, -bl / 2]), S([bw, bh, bl]), skin, b=o.get("round", 1)))
    hw, hh, hl = o.get("head", (bw - 0.4, bh - 0.8, 3))
    hdy = o.get("head_dy", 0)
    hf = -bl / 2 - hl + 0.6
    headm = o.get("headm") or skin
    m.bone("head", "body", S([0, y0, -bl / 2 + 0.5]), face_blob(S([-hw / 2, y0 - hh / 2 + hdy, hf]), S([hw, hh, hl]), headm) +
           ([Cube(S([-o["lip"][0] / 2, y0 - hh / 2 + hdy + o["lip"][2], hf - o["lip"][1]]), [n(o["lip"][0] * s), n(1.4 * s), n(o["lip"][1] * s)], headm)] if o.get("lip") else []))
    MOUTH = o.get("MOUTH_C", B_D)
    m.bone("jaw", "head", S([0, y0 - hh / 2 + hdy + 1, hf + 0.6]), [Cube(S([-(hw - 1.6) / 2, y0 - hh / 2 + hdy + 0.3, hf - 0.3]), [n((hw - 1.6) * s), 1, n(1.6 * s)],
                                                                         Mat(MOUTH, open_mouth(o.get("MOUTH", "#8A2A3A"), MOUTH), noise=0.02))])
    if o.get("eyes_top"):
        m.eyes("head", o.get("eye_in", 0.6) * s, (y0 + hh / 2 + hdy - 0.6) * s, hf * s, w=o.get("eye_w", 2), h=o.get("eye_h", 2), iris=o.get("IRIS", "#1A1010"),
               pupil=o.get("PUPIL", "#000000"), skin=o.get("EYE_SKIN", B), iris_light=o.get("IRIS_L"), grid=o.get("eye_grid", ["WP", "PP"]))
    else:
        m.eyes("head", hw / 2 * s, (y0 + hdy + o.get("eye_dy", 0.2)) * s, hf * s, h=o.get("eye_h", 2), corner=(1, 1), half=hw / 2 * s, iris=o.get("IRIS", "#1A1010"),
               pupil=o.get("PUPIL", "#000000"), skin=o.get("EYE_SKIN", B), iris_light=o.get("IRIS_L"), grid=o.get("eye_grid", ["WP", "PP"]))
    FIN, FIN_L, FIN_D = o.get("FIN", B_L), o.get("FIN_L", B_L), o.get("FIN_D", B_D)
    segs = o.get("tail_segs", [(bw * 0.7, bh * 0.7, 2.2), (bw * 0.45, bh * 0.5, 2)])
    names, zt = tail_bones(m, "body", y0 * s, (bl / 2 - 0.5) * s, [tuple(v * s for v in sg) for sg in segs], skin, o.get("tail_rests", [0] * len(segs)))
    tf = o.get("tail_fin", ("fork", bh * 1.3, 4))
    if tf:
        shape, th_, tl_ = tf
        tb = bone_of(m, names[-1])
        tb.cubes.append(Cube([0, (y0 - th_ / 2) * s, zt - 0.6], [0, n(th_ * s), n(tl_ * s)], fin_mat(FIN, FIN_L, FIN_D, shape)))
    d = o.get("dorsal", (2.4, bl * 0.6, "sail"))
    if d:
        dh, dl, dshape = d
        bone_of(m, "body").cubes.append(Cube([0, (y0 + bh / 2 - 0.4) * s, (-dl / 2 + o.get("dorsal_z", 0.5)) * s], [0, n(dh * s), n(dl * s)], fin_mat(FIN, FIN_L, FIN_D, dshape)))
    pe = o.get("pecs", (2.6, 3, 30))
    if pe:
        ph, pl, prot = pe
        m.pair("wing_right", "body", S([-bw / 2 + 0.2, y0 - 0.8, -bl / 2 + 2]), [Cube(S([-bw / 2 + 0.1, y0 - 0.8 - ph, -bl / 2 + 2]), [0, n(ph * s), n(pl * s)],
                                                                                fin_mat(FIN, FIN_L, FIN_D, "round"))], rotation=[0, 0, prot])
    p = {"m": m, "s": s, "S": S, "skin": skin, "y0": y0, "bw": bw, "bh": bh, "bl": bl, "hw": hw, "hh": hh, "hl": hl, "hf": hf, "tail": names, "zt": zt,
         "FIN": FIN, "FIN_L": FIN_L, "FIN_D": FIN_D}
    if o.get("extra"):
        o["extra"](p)
    fx_for(m, o.get("fx", 0.7) * s)
    moves(m, *o.get("moves", ("TACKLE", "WATER GUN")))
    fl = o.get("flap", 0.8)
    fa = o.get("flap_amp", 16)
    ex = {"idle": {"wing_right": track(rotation=[0, 0, wave(fl, fa, 0, pe[2] if pe else 0)]), "wing_left": track(rotation=[0, 0, wave(fl, -fa, 0, -(pe[2] if pe else 0))])},
          "walk": {"wing_right": track(rotation=[0, 0, wave(fl * 0.6, fa * 1.4, 0, pe[2] if pe else 0)]),
                   "wing_left": track(rotation=[0, 0, wave(fl * 0.6, -fa * 1.4, 0, -(pe[2] if pe else 0))])}}
    for k_, v_ in (o.get("anim_extras") or {}).items():
        ex.setdefault(k_, {}).update(v_)
    A, info = floater(name.lower(), m, k=o.get("k", 0.7) * s, bob=o.get("bob", 0.6), tentacles=[names], extras=ex)
    write(sid, name, m, A, info, stands)


def face_petal(base, light, vein):
    """A petal on a flat part standing up (x-y), its root at the bottom: rounded, a pale tip, a vein up its middle."""
    B, Lt, V = rgb(base), rgb(light), rgb(vein)

    def f(at):
        u, t = at.lx, at.ly
        hw = 0.5 * math.sin(math.pi * min(1.0, t * 0.85 + 0.12)) ** 0.5
        d = abs(u - 0.5)
        if d > hw or t > 0.98:
            return CLEAR
        if d < 0.06 and t < 0.8:
            return V
        return mix(B, Lt, 0.15 + 0.6 * t)
    return Mat(base, f, noise=0.015, edge=0, flat=True)


def flower_face(cx, cy, cz, r, n_, petal, heart, s, tilt=0):
    """A flower facing forward at (cx, cy, cz): n_ petals of length r round a heart (r * 0.6 across)."""
    out = []
    for i in range(n_):
        a = i * 360 / n_
        out.append(Cube([(cx - r * 0.3) * s, cy * s, (cz + 0.05 * (i % 2)) * s], [n(r * 0.6 * s), n(r * s), 0], petal, [tilt, 0, a], [cx * s, cy * s, cz * s]))
    hr = r * 0.62
    out.append(Cube([(cx - hr / 2) * s, (cy - hr / 2) * s, (cz - 0.6) * s], [n(hr * s), n(hr * s), n(0.8 * s)], heart))
    return out


# ======================================================================================================
# The golden-mantled flying fox: NOCTIVANE (in place of Crobat; after DUSKBAT and NIGHTMAW)
# ======================================================================================================

def noctivane():
    """A great flying fox, sleek and fast: a fox's long face, tall pointed ears, a dusky violet body with a mantle of
    golden fur over its shoulders and nape, amber eyes, and vast swept-back wings of deep violet membrane edged in gold,
    a hooked thumb at each wrist. It never lands."""
    m = Model()
    s = 1.8
    S = S_(s)
    FUR, FUR_L, FUR_D = "#3E2E6E", "#6A58A0", "#1E1640"
    MEM, EDGE, VEIN = "#3A1E5A", "#E8B848", "#1A0C2E"
    furm = Mat(FUR, fur(FUR, FUR_L, FUR_D, speck=0.5), noise=0.03)
    gold = shag_mat("#E8B040", "#FFE08A", "#A87420")
    face = Mat(FUR, lambda a: rgb(FUR_L) if a.face == "front" and a.ly < 0.4 else None, noise=0.02)
    muz = Mat("#2A2040", lambda a: rgb("#0E0A18") if a.face == "front" and a.ly > 0.5 and abs(a.lx - 0.5) < 0.3 else None, noise=0.02)
    jaw_m = Mat(FUR_D, open_mouth("#A82A4A", FUR), noise=0.02)
    fang = plain("#FFFFF4")
    bonem = Mat(FUR_D, noise=0.02, edge=0)
    claw = plain("#F0E0B8")
    mems = [membrane_mat(MEM, EDGE, VEIN, seed=i * 3.1) for i in range(5)]
    ear = ear_mat(FUR, "#C86A9A", "fennec")
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 10, 0]), blob(S([-2.5, 7.4, -2.4]), S([5, 5.2, 6]), furm) + blob(S([-3, 10.6, -3]), S([6, 2.6, 4]), gold), rotation=[-6, 0, 0])
    m.bone("head", "body", S([0, 11.4, -2.6]), face_blob(S([-2.2, 10.2, -6]), S([4.4, 4, 3.6]), face) + [Cube(S([-1.2, 10.4, -7.8]), [n(2.4 * s), n(2 * s), n(2 * s)], muz)] +
           blob(S([-2.6, 12.6, -4.6]), S([5.2, 2, 3]), gold))
    m.bone("jaw", "head", S([0, 10.6, -6]), [Cube(S([-1, 9.8, -7.6]), [n(2 * s), 1, n(2 * s)], jaw_m), Cube(S([-0.9, 10.4, -7.7]), [1, 1, 1], fang),
                                             Cube(S([0.4, 10.4, -7.7]), [1, 1, 1], fang)])
    m.eyes("head", x_inner=0.5 * s, y=11.8 * s, z=-6 * s, w=2, h=2, iris="#F0A020", pupil="#1A0A04", skin=FUR, iris_light="#FFE07A", grid=["LL", "IP"])
    m.pair("ear_right", "head", S([-1.4, 13.6, -4]), [Cube(S([-3, 13.4, -4]), [n(3 * s), n(5 * s), 0], ear)], rotation=[-12, 6, -10])
    dragon_wing(m, 2.4 * s, 11.6 * s, -2 * s, 7 * s, (3.4 * s, 4.6 * s, 1.8 * s, 9 * s), bonem, mems, thick=1.4, claw=claw, under="#6A3A7A")
    legs_pair(m, "leg_right", "body", 1.2 * s, 7.8 * s, 1.8 * s, round(2 * s), bonem, claw)
    fx_for(m, 0.7 * s)
    moves(m, "WING ATTACK", "CONFUSE RAY")
    A, info = flyer("noctivane", m, flap=0.42, amp=36, rest=60, k=0.7 * s, legs=("leg_right", "leg_left"))
    write("crobat", "NOCTIVANE", m, A, info, 1.8)


# ======================================================================================================
# The torpedo rays: STATICRAY > STORMRAY (in place of Chinchou, Lanturn)
# ======================================================================================================

def torpedo_ray(sid, name, stands, big):
    """A torpedo ray of the deep: a round flat disc of a body, a short thick tail with two little fins and a tail fin,
    eyes up on its brow, and in its back two kidney-shaped electric organs that glow. Grown, a great midnight ray, its
    organs blazing, a ring of tiny cyan lights round the rim of its disc like stars."""
    m = Model()
    s = 1.0 if not big else 1.6
    S = S_(s)
    B, B_L, B_D = ("#3A6AB0", "#7AA8E0", "#1E3A70") if not big else ("#2A3A7A", "#5A78C0", "#141E48")
    BELLY = "#E8F0F4"
    ORG = "#FFF07A" if not big else "#FFE84A"

    def disc(a, under=True):
        if under and a.face == "bottom":
            return rgb(BELLY)
        c = mix(B_D, B, 0.5 + 0.5 * cell(a.x, a.y, a.z, 1.4))
        if hash01("rayspot", round(a.x * 1.5), round(a.z * 1.5)) < 0.08:
            c = mix(c, "#C8E4FF", 0.6)
        return c
    skin = Mat(B, disc, noise=0.015)

    def wing_shape(a):
        # The disc's rounded edge: u out from the body, v front to back.
        u, v = (abs(a.x) - 3.6 * s) / (5.4 * s), a.lz
        if u ** 2 + ((v - 0.5) / 0.5) ** 2 > 1.0:
            return CLEAR
        return disc(a, False)
    wingm = Mat(B, wing_shape, noise=0.015, edge=0, flat=True)
    organ = Mat(ORG, lambda a: rgb(ORG) if ((a.lx - 0.5) / 0.5) ** 2 + ((a.lz - 0.5) / 0.5) ** 2 < 1 and not (a.lx < 0.3 and 0.35 < a.lz < 0.65) else CLEAR,
                glow=lambda a: rgb(ORG) if ((a.lx - 0.5) / 0.5) ** 2 + ((a.lz - 0.5) / 0.5) ** 2 < 1 and not (a.lx < 0.3 and 0.35 < a.lz < 0.65) else None,
                noise=0, edge=0, flat=True)
    y0 = 4
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, y0, 0]), blob(S([-4, y0 - 1.4, -4.5]), S([8, 2.8, 9]), skin) +
           [Cube(S([-3.6, y0 + 1.45, -2.4]), [n(2.6 * s), 0, n(3.6 * s)], organ), Cube(S([1.0, y0 + 1.45, -2.4]), [n(2.6 * s), 0, n(3.6 * s)], organ)])
    m.bone("head", "body", S([0, y0 + 1, -4.4]), face_blob(S([-2.6, y0 + 0.4, -5.6]), S([5.2, 2.4, 3]), skin))
    m.bone("jaw", "head", S([0, y0 - 0.6, -5]), [Cube(S([-1.2, y0 - 1.2, -5.0]), [n(2.4 * s), 1, n(1.2 * s)], Mat(BELLY, open_mouth("#B84A5A", BELLY), noise=0.02))])
    m.eyes("head", 0.7 * s, (y0 + 1.0) * s, -5.6 * s, w=2, h=2 if big else 2, iris="#1A1A2A" if not big else "#E8B830", pupil="#05050A", skin=B, iris_light="#7A8AB0",
           grid=["WP", "PP"] if not big else ["LL", "IP"])
    # The disc's rims: broad, rounded, rippling as it swims.
    m.pair("wing_right", "body", S([-3.6, y0 + 0.2, 0]), [Cube(S([-9, y0 + 0.2, -4.4]), [n(5.4 * s), 0, n(8.8 * s)], wingm)], rotation=[0, 0, 4])
    if big:
        lit = Mat("#8AF0FF", glow=lambda a: rgb("#B8FAFF"), noise=0, edge=0)
        for side in ("wing_right", "wing_left"):
            b = bone_of(m, side)
            sg = -1 if side.endswith("right") else 1
            for i in range(5):
                ang = math.pi * (0.15 + 0.7 * i / 4)
                b.cubes.append(Cube([sg * (4 + 4.4 * math.sin(ang)) * s - 0.5, (y0 + 0.2) * s, (-4.2 * math.cos(ang)) * s - 0.5], [1, 1, 1], lit))
    finm = fin_mat(B_L, "#A8C8F0", B_D, "round")
    names, zt = tail_bones(m, "body", y0 * s, 4.2 * s, [(2.2 * s, 1.8 * s, 2 * s), (1.6 * s, 1.4 * s, 2 * s), (1.2 * s, 1 * s, 1.6 * s)], skin, (4, 0, -2))
    bone_of(m, names[0]).cubes.append(Cube([0, (y0 + 0.7) * s, 4.4 * s], [0, n(1.8 * s), n(1.8 * s)], fin_mat(B_L, "#A8C8F0", B_D, "sail")))
    bone_of(m, names[1]).cubes.append(Cube([0, (y0 + 0.5) * s, 6.4 * s], [0, n(1.4 * s), n(1.6 * s)], fin_mat(B_L, "#A8C8F0", B_D, "sail")))
    bone_of(m, names[-1]).cubes.append(Cube([0, (y0 - 1.6) * s, zt - 0.6], [0, n(3.2 * s), n(2.6 * s)], finm))
    fx_for(m, 0.7 * s)
    moves(m, "SPARK" if not big else "THUNDERBOLT", "THUNDER WAVE" if not big else "CONFUSE RAY")
    ripple = {"wing_right": track(rotation=[wave(1.6, 6, 90), 0, wave(1.6, 12, 0, 4)]), "wing_left": track(rotation=[wave(1.6, 6, 90), 0, wave(1.6, -12, 0, -4)])}
    A, info = floater(name.lower(), m, k=0.6 * s, bob=0.5, tentacles=[names], extras={"idle": ripple, "walk": {"wing_right": track(rotation=[0, 0, wave(0.8, 22, 0, 4)]),
                                                                                                                "wing_left": track(rotation=[0, 0, wave(0.8, -22, 0, -4)])}})
    write(sid, name, m, A, info, stands)


def staticray():
    torpedo_ray("chinchou", "STATICRAY", 0.5, False)


def stormray():
    torpedo_ray("lanturn", "STORMRAY", 1.2, True)


# ======================================================================================================
# The moon rabbit's kit: LUNAKIT (in place of Cleffa; before LUNABUN)
# ======================================================================================================

def lunakit():
    """The moon rabbit's kit: a little round ball of lilac-pink fluff with a big round head, a tiny gold star of a
    crescent just coming in on its brow, short soft ears tipped violet, button eyes, stubby paws and a cotton tail."""
    FUR, FUR_L, FUR_D, BELLY = "#E8C4E4", "#F6E0F2", "#B88AB4", "#FFF4EC"
    cotton = Mat("#FFFFFF", lambda a: rgb("#ECE4F0") if a.face == "bottom" else None, noise=0.03, edge=0)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        bone_of(m, "head").cubes.append(Cube(S([-1, p["hy"] + p["hh"] * 0.78, p["fz"] - 0.03]), [n(2 * s), n(1.4 * s), 0], glow_patch("#FFD860")))
        m.bone("tail", "body", S([0, p["hip"] + 1.6, p["bd"] / 2 - 0.4]), [Cube(S([-1.3, p["hip"] + 0.6, p["bd"] / 2 - 0.4]), [n(2.6 * s), n(2.6 * s), n(2 * s)], cotton)])
        m.pair("whiskers_right", "head", S([-1, p["hy"] + 1.6, p["fz"]]), [Cube(S([-4, p["hy"] + 1.6, p["fz"] - 1]), [n(3 * s), 0, n(1.6 * s)], whisker_mat("#C8A8C8"))])
    upright("cleffa", "LUNAKIT", 0.3, dict(
        s=1.0, hip=1.6, body=(5.6, 4.4, 5), FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=BELLY, FACE=None, head=(7, 6.4, 6.2), head_y=-0.4, head_z=-0.2,
        NOSE="#E87AA0", mouth_y=0.18, ears=("rabbit", 2.6, 4.4), EAR_IN="#FFC8DC", EAR_TIP="#8A5AB0", ear_rot=[-10, 0, -20],
        IRIS="#4A2A6A", PUPIL="#140A1C", eye_in=1.2, eye_y=0.42, eye_grid=["PW", "PP"], arms=(1.4, 1.2, 1.4), HAND=BELLY, leg_w=1.8, foot=2.6, toes=2, FOOT=BELLY,
        thigh=2.4, extra=extra, moves=("POUND", "SWEET KISS"), anim="hopper", hop=0.42, height=1.4, round=2))


# ======================================================================================================
# The singing chinchilla's kit: HUMCHIN (in place of Igglybuff; before PUFFCHIN)
# ======================================================================================================

def humchin():
    """The singing chinchilla's kit, too young to sing, only hum: one round ball of silvery pink fluff, two great round
    ears pink inside, a curl of a forelock, long whiskers, a stubby plume of a tail and two tiny feet."""
    FUR, FUR_L, FUR_D, BELLY = "#ECD0DC", "#FFF2F8", "#B096A6", "#FFF6F2"
    plume = Mat(FUR_L, brush(FUR, FUR_L, FUR_D, tip="#FFFFFF", tip_from=0.7), noise=0.0, edge=0)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hy, fz = p["hy"], p["fz"]
        m.bone("curl", "head", S([0, hy + p["hh"], fz + 1.4]), [Cube(S([-1, hy + p["hh"] - 0.6, fz + 0.6]), [n(2 * s), n(2 * s), n(2 * s)], p["furm"]),
                                                               Cube(S([-0.6, hy + p["hh"] + 0.6, fz - 0.2]), [n(1.4 * s), n(1.4 * s), n(1.4 * s)], p["furm"], [-30, 0, 0],
                                                                    S([0, hy + p["hh"] + 1, fz]))])
        m.pair("whiskers_right", "head", S([-1.2, hy + 1.8, fz]), [Cube(S([-6, hy + 1.8, fz - 1.2]), [n(4.8 * s), 0, n(2 * s)], whisker_mat("#F8F0F4"))])
        tb = bone_of(m, p["tail"][-1])
        tb.cubes += [Cube([-2 * s, tb.pivot[1], tb.pivot[2] - 0.5], [n(4 * s), 0, n(2.6 * s)], plume), Cube([0, tb.pivot[1] - 2 * s, tb.pivot[2] - 0.5], [0, n(4 * s), n(2.6 * s)], plume)]
    upright("igglybuff", "HUMCHIN", 0.3, dict(
        s=1.0, hip=1.4, body=(6, 3, 5.6), FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=BELLY, belly_patch=False, FACE=None, head=(8, 7.4, 7.4), head_y=-1.2, head_z=0,
        speck=0.6, NOSE="#E8789A", mouth_y=0.24, ears=("round", 5, 5.4), EAR_IN="#F8A8C0", ear_x=3.0, ear_rot=[-4, 14, -26],
        IRIS="#2A1A24", PUPIL="#0A0608", IRIS_L="#6A4A5A", eye_in=1.1, eye_y=0.44, eye_grid=["PW", "KP"], arms=(1.2, 1, 1.4), HAND=BELLY, leg_w=1.6, foot=2.4,
        toes=2, FOOT="#F2B8C8", thigh=2, tail={"segs": [(1.4, 1.4, 1.6), (1.2, 1.2, 1.6)], "rests": [60, 40], "y": 1.2}, extra=extra, moves=("POUND", "SING"),
        anim="hopper", hop=0.4, height=1.4, round=2,
        anim_extras={"cry": {"head": track(scale=keys({0: [1, 1, 1], 0.25: [1.12, 1.1, 1.12], 0.8: [1.12, 1.1, 1.12], 1.1: [1, 1, 1]}))}}))


# ======================================================================================================
# The doves: DOWNLET > GRACEDOVE (in place of Togepi, Togetic)
# ======================================================================================================

def downlet():
    """A dove's chick, just out of the nest: a round puff of cream down, rosy cheeks, a soft pink beak, a tuft sticking
    up on its crown, stubby wings it can't fly with yet and two little pink feet."""
    def coat(at, base):
        k = hash01("down", round(at.x * 1.5), round(at.y * 1.5), round(at.z * 1.5))
        if k < 0.12: return rgb("#FFFFFF")
        if k > 0.9: return rgb("#E8D4A8")
        return None

    def face_coat(a):
        if a.face == "front" and 0.25 < a.ly < 0.5 and abs(a.lx - 0.5) > 0.32: return rgb("#F6B8B8")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        tuft = Mat("#FFF4DC", lambda a: CLEAR if abs(a.lx - 0.5) > 0.5 * (1 - a.ly) + 0.08 else None, noise=0.01, edge=0, flat=True)
        bone_of(m, "head").cubes += [Cube(S([-0.8, p["hy"] + p["hh"] - 0.2, p["hz"] - p["hl"] * 0.5]), [n(1.6 * s), n(2.4 * s), 0], tuft, [-10, a, 0],
                                          S([0, p["hy"] + p["hh"], p["hz"] - p["hl"] * 0.5])) for a in (-30, 0, 30)]
    avian("togepi", "DOWNLET", 0.3, dict(
        s=1.0, body=(6.4, 6.4, 6.4), leg=1.4, B="#FFF0D0", B_L="#FFFFFF", B_D="#E0CCA0", BELLY="#FFF8E8", coat=coat, head=(5.6, 5, 5), FACE="#FFF0D0", face_coat=face_coat,
        neck_up=-1.4, beak=1.0, beak_w=1.4, BEAK="#F2A0A8", BEAK_D="#C87078", eye_w=2, eye_h=2, side_eyes=False, eye_in=0.7, eye_back=0, IRIS="#2A1A12", eye_grid=["PW", "PP"],
        wing=4, WING="#FFF0D0", WING_D="#E0CCA0", WING_L="#FFFFFF", tail=1.6, TAIL="#FFF0D0", LEG="#F2A0A8", pitch=0, extra=extra,
        moves=("POUND", "SWEET KISS"), walk=0.4, k=0.6))


def gracedove():
    """A fantail dove: pure white, plump-breasted, its head held high, its great fan of a tail raised upright behind it,
    each feather edged in soft sky-blue and rose, a gold ring round each dark eye, long wings for gliding."""
    def tail_extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        fan = Mat("#FFFFFF", lambda a: CLEAR if a.ly > 0.96 or (a.ly > 0.82 and a.col % 2) else (rgb("#A8D0F0") if a.ly > 0.74 else (rgb("#F4B8CC") if a.ly > 0.62 else None)),
                  noise=0.01, edge=0, flat=True)
        tb = bone_of(m, "tail")
        y, z = p["leg"] + p["bh"] * 0.6, p["bl"] / 2 - 0.8
        for a in (-70, -45, -22, 0, 22, 45, 70):
            tb.cubes.append(Cube(S([-1.2, y, z]), [n(2.4 * s), n(9 * s), 0], fan, [-12, 0, a], S([0, y, z])))
        ring = plain("#F0C850")
        hd = bone_of(m, "head")
        for side in (-1, 1):
            hd.cubes.append(Cube(S([side * (p["hw"] / 2) - (0.04 if side > 0 else -0.04) - 0, p["hy"] + p["hh"] * 0.5, p["hz"] - p["hl"] + 1.0]), [0, n(2.6 * s), n(2.6 * s)], ring))
    avian("togetic", "GRACEDOVE", 0.6, dict(
        s=1.3, body=(6, 6.6, 8), leg=3, B="#FAFAF4", B_L="#FFFFFF", B_D="#D8DCE4", BELLY="#FFFFFF", head=(4.4, 4.4, 4.6), FACE="#FAFAF4", neck_up=1.6, beak=1.4, beak_w=1.2,
        BEAK="#E8A0A8", BEAK_D="#B87078", eye_w=2, eye_h=2, IRIS="#1A1420", eye_in=2.2, eye_back=1.2, wing=11, WING="#F4F6FA", WING_D="#C8D0DC", WING_L="#FFFFFF",
        tail=2, TAIL="#FFFFFF", LEG="#E89AA4", pitch=-14, extra=tail_extra, moves=("PECK", "SWEET KISS"), walk=0.45, k=0.7, flap=0.32))


# ======================================================================================================
# The hoopoes: HOOPLET > OMENHOOP (in place of Natu, Xatu)
# ======================================================================================================

def hoopoe(sid, name, stands, big):
    """A hoopoe: soft cinnamon-pink plumage, wings and back boldly barred black and white, a long thin down-curved bill,
    and a great fan of a crest, each feather tipped black, raised when it's startled. Its eyes are a strange violet.
    Grown, tall and still as a post, its barred wings drawn round it like a cloak, its crest always up, a long tail."""
    s = 1.0 if not big else 1.9
    B, B_L, B_D = "#E0A07A", "#F4C8A8", "#A86A48"

    def bars(a):
        return rgb("#141414") if int((a.z + a.y * 0.3) * 1.1) % 2 == 0 else rgb("#F4F0E8")

    def coat(at, base):
        if at.face in ("top", "back") and at.z > 0.5: return bars(at)
        return None

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        crest = Mat(B, lambda a: CLEAR if abs(a.lx - 0.5) > 0.42 * (1 - a.ly * 0.4) else (rgb("#141414") if a.ly > 0.84 else (rgb("#F4F0E8") if a.ly > 0.76 else mix(B_D, B_L, a.ly))),
                    noise=0.01, edge=0, flat=True)
        hd = bone_of(m, "head")
        y0, z0 = p["hy"] + p["hh"] - 0.6, p["hz"] - p["hl"] * 0.5
        ang = (-50, -25, 0, 25, 50, 75) if big else (-40, -10, 20, 50)
        for a in ang:
            hd.cubes.append(Cube(S([-1, y0, z0]), [n(2 * s_), n((4.2 if big else 3) * s_), 0], crest, [a - 10, 0, 0], S([0, y0, z0])))
        # The wings barred black and white.
        for nm in ("wing_right", "wing_left", "wing_mid_right", "wing_mid_left", "wing_tip_right", "wing_tip_left"):
            try:
                b = bone_of(m, nm)
            except StopIteration:
                continue
            for c in b.cubes:
                c.mat = Mat("#141414", lambda a: rgb("#F4F0E8") if int(a.lz * 6) % 2 == 0 else rgb("#141414"), noise=0.01, edge=0, flat=c.mat.flat)
        if big:
            tb = bone_of(m, "tail")
            tb.cubes.append(Cube(S([-1.4, p["leg"] + p["bh"] * 0.4, p["bl"] / 2]), [n(2.8 * s_), 0, n(7 * s_)], Mat("#141414", lambda a: rgb("#F4F0E8") if 0.5 < a.lz < 0.62 else None,
                                                                                                                      noise=0.01, edge=0, flat=True), [40, 0, 0],
                                 S([0, p["leg"] + p["bh"] * 0.4, p["bl"] / 2])))
    avian(sid, name, stands, dict(
        s=s, body=(5.6, 6, 7) if not big else (5.6, 8, 6), leg=2.4 if not big else 3.4, B=B, B_L=B_L, B_D=B_D, BELLY="#F8D8C0", coat=coat, head=(4.4, 4, 4.2),
        FACE=B, neck_up=0.6 if not big else 1.4, beak=3.6 if not big else 4.4, beak_w=1, beak_h=1, beak_tilt=16, BEAK="#2A2420", BEAK_D="#141210",
        eye_w=2, eye_h=2, IRIS="#8A4AC8", IRIS_L="#C89AF0", eye_in=2.2, eye_back=1.0, wing=8 if not big else 10, WING="#141414", WING_D="#141414", WING_L="#F4F0E8",
        tail=3 if not big else 5, TAIL="#141414", tail_w=3, LEG="#6A5A50", pitch=-6 if not big else -38, extra=extra,
        moves=(("PECK", "NIGHT SHADE") if not big else ("PECK", "FUTURE SIGHT")), walk=0.45, k=0.7))


def hooplet():
    hoopoe("natu", "HOOPLET", 0.2, False)


def omenhoop():
    hoopoe("xatu", "OMENHOOP", 1.5, True)


# ======================================================================================================
# The tuber in bloom: FLORETTA (in place of Bellossom; from DROOPBUD)
# ======================================================================================================

def floretta():
    """The walking tuber in full bloom and dancing: its root gone a sky blue at dawn, a skirt of broad glossy leaves
    round its middle that twirls as it spins, two marigold-orange flowers on its crown, and little leafy arms held up."""
    m = Model()
    s = 1.0
    S = S_(s)
    TB, TBL, TBD = "#7A9AE0", "#AAC4F4", "#4A62AA"
    body = Mat(TB, fur(TB, TBL, TBD, speck=0.3), noise=0.02)
    jaw_m = Mat(TBD, open_mouth("#6A2A4A", TB), noise=0.02)
    rootm = Mat("#C8B890", lambda a: rgb("#8A7A5A") if a.face == "bottom" else None, noise=0.02)
    leaf = leaf_mat("#6ACC50", "#2A7A30", "#B8F090", "z")
    m.bone("root", None, [0, 0, 0])
    hip = 3.4
    m.bone("body", "root", S([0, hip + 3, 0]), blob(S([-3.5, hip - 0.4, -3.5]), S([7, 6.6, 7]), body, b=2))
    bd = bone_of(m, "body")
    for a in range(0, 360, 45):
        bd.cubes.append(Cube(S([-2.2, hip + 0.6, 2.4]), [n(4.4 * s), 0, n(5 * s)], leaf, [-62, a, 0], S([0, hip + 1.2, 0])))
    m.bone("head", "body", S([0, hip + 6, 0]), [Cube(S([-1.6, hip + 5.8, -1.6]), [n(3.2 * s), n(1.2 * s), n(3.2 * s)], Mat("#3E8A3A", noise=0.02))])
    hd = bone_of(m, "head")
    pet = face_petal("#FF9A1E", "#FFE27A", "#E06A10")
    heart = Mat("#8A3A14", lambda a: rgb("#C86A1E") if (a.row + a.col) % 2 else None, noise=0.02)
    for cx, cy in ((-2.2, hip + 9.4), (2.2, hip + 9.0)):
        hd.cubes += flower_face(cx, cy, -0.4, 2.8, 8, pet, heart, s, tilt=-15)
        hd.cubes.append(Cube(S([cx * 0.5 - 0.4, hip + 6.6, -0.4]), [1, n(2.4 * s), 1], plain("#3E8A3A"), [0, 0, -cx * 8], S([0, hip + 6.6, 0])))
    m.bone("jaw", "body", S([0, hip + 2, -3.4]), [Cube(S([-1, hip + 1.4, -3.9]), [n(2 * s), 1, n(1.2 * s)], jaw_m)])
    m.eyes("body", x_inner=0.8 * s, y=(hip + 3.0) * s, z=-3.5 * s, w=2, h=2, iris="#2A1A3A", pupil="#0A0610", skin=TB, iris_light="#6A5A8A", grid=["PW", "PP"])
    lf2 = leaf_mat("#6AC050", "#2A6A2A", "#B8E890", base_end=1)
    m.pair("arm_right", "body", S([-3.2, hip + 4, -0.5]), [Cube(S([-4.2, hip + 2.2, -1]), [n(1.4 * s), n(2.2 * s), n(1.4 * s)], body)], rotation=[0, 0, -60])
    m.pair("forearm_right", "arm_right", S([-3.8, hip + 2.4, -0.5]), [Cube(S([-5.4, hip - 1.4, -0.5]), [n(3 * s), n(4 * s), 0], lf2)], rotation=[-10, 0, 10])
    upright_legs(m, 1.8 * s, hip * s, 0.2 * s, n(1.6 * s), rootm, rootm, n(2.6 * s), toe_m=plain("#8A7A5A"), ntoes=2, thigh_w=n(2 * s))
    fx_for(m, 0.7)
    moves(m, "PETAL DANCE", "SWEET SCENT")
    spin = {"body": track(rotation=[0, wave(1.6, 18), 0])}
    A, info = biped("floretta", m, walk=0.5, stride=30, k=0.6, guard=0, extras={"idle": {"head": track(rotation=[wave(2, 6), wave(3, 8), wave(2, 6, 90)])}, "battle": spin})
    write("bellossom", "FLORETTA", m, A, info, 0.4)


# ======================================================================================================
# The water voles: DEWVOLE > BRIMVOLE (in place of Marill, Azumarill)
# ======================================================================================================

def water_vole(sid, name, stands, big):
    """A water vole, its fur slicked to a soft slate blue by the river: a round body, a blunt face, small round ears
    tucked in its fur, long whiskers, webbed hind feet and a flat paddle of a tail. Grown, plump and tall, a cream bib
    of fur, a sprig of watercress tucked behind one ear, and a broad strong tail it slaps on the water."""
    s = 1.0 if not big else 1.6
    FUR, FUR_L, FUR_D = "#6A8AB8", "#9AB8E0", "#3A5480"
    BELLY = "#F4ECD8"
    web = Mat("#5A6A8A", lambda a: CLEAR if (a.lx * 4) % 1.0 > 0.85 else None, noise=0, edge=0, flat=True)
    paddle = Mat("#4A5A7A", lambda a: rgb("#3A4864") if (a.row + a.col) % 3 == 0 else None, noise=0.02)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        m.pair("whiskers_right", "head", S([-1.4, p["hy"] + 1.6, p["fz"]]), [Cube(S([-5.6, p["hy"] + 1.6, p["fz"] - 1.2]), [n(4.4 * s_), 0, n(2 * s_)], whisker_mat("#E8EEF4"))])
        tb = bone_of(m, p["tail"][-1])
        tb.cubes.append(Cube([-1.6 * s_, tb.pivot[1] - 0.5 * s_, tb.pivot[2] - 0.4], [n(3.2 * s_), n(1 * s_), n(3.6 * s_)], paddle))
        for side in ("foot_right", "foot_left"):
            b = bone_of(m, side)
            c = b.cubes[0]
            b.cubes.append(Cube([c.origin[0] - 0.6, 0.05, c.origin[2] - 1.2], [c.size[0] + 1.2, 0, 1.6], web))
        if big:
            cress = leaf_mat("#5AB848", "#2A6A2A", "#B8E8A0")
            hd = bone_of(m, "head")
            for k, a in enumerate((-20, 15, 45)):
                hd.cubes.append(Cube(S([-p["hw"] / 2 - 0.4, p["hy"] + p["hh"] - 0.8, p["hz"] + 0.6 + k * 0.4]), [0, n(2.2 * s_), n(2 * s_)], cress, [a, 0, 12],
                                     S([-p["hw"] / 2, p["hy"] + p["hh"] - 0.8, p["hz"] + 0.6])))
    upright(sid, name, stands, dict(
        s=s, hip=2 if not big else 3, body=(7, 6.4, 6.4) if not big else (8, 8, 7), FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY=BELLY if big else "#D8E4F0",
        belly_size=(4, 4.4) if not big else (5.4, 6.4), FACE="#C8D6E8", head=(5.6, 4.8, 5) if not big else (6, 5.4, 5.4), head_y=-0.6, snout=(1.4, 2.8, 2),
        NOSE="#3A2A3A", MUZZLE="#C8D6E8", ears=("round", 1.8, 1.8), EAR_IN="#C89AAA", IRIS="#140C10", eye_in=1.0, eye_y=0.5, eye_grid=["PW", "PP"],
        arms=(1.4, 1.4, 1.6) if not big else (2.4, 2.4, 2.2), HAND="#C8B8C0", claws=0, leg_w=2, foot=3, FOOT="#8A7A8A", thigh=3 if not big else 3.6,
        tail={"segs": [(1.4, 1.2, 2), (1.2, 1, 2)], "rests": [-10, -6], "y": 0.8}, extra=extra,
        moves=(("TACKLE", "BUBBLE") if not big else ("ROLLOUT", "BUBBLEBEAM")), anim="hopper" if not big else None, hop=0.45, walk=0.6))


def dewvole():
    water_vole("marill", "DEWVOLE", 0.4, False)


def brimvole():
    water_vole("azumarill", "BRIMVOLE", 0.8, True)


# ======================================================================================================
# The stump tortoise: STUMPSHELL (in place of Sudowoodo)
# ======================================================================================================

def stumpshell():
    """A tortoise that passes itself off as an old tree stump: a tall domed shell ridged and grooved like bark, its top
    a cut stump's rings, moss creeping over it and two leafy shoots springing up from it; stone-grey scaly legs and head,
    and a solemn beak. It stands very still when anyone looks."""
    BARK, BARK_L, BARK_D = "#8A6A44", "#B8946A", "#5A4228"
    shellm = Mat(BARK, lambda a: (rgb(BARK_D) if (a.x * 1.2) % 2 < 0.5 else mix(BARK, BARK_L, cell(a.x, a.y, a.z, 1.5))) if a.face != "top" else None, noise=0.02)
    rings = Mat("#D8B884", lambda a: rgb("#A8865A") if (math.hypot(a.lx - 0.5, a.lz - 0.5) * 9) % 1.0 < 0.3 else None, noise=0.01)
    moss = Mat("#6A9A3A", lambda a: CLEAR if hash01("moss", round(a.x * 2), round(a.z * 2)) < 0.35 else mix("#4A7A2A", "#8ABA4A", cell(a.x, a.y, a.z, 1.2)), noise=0, edge=0, flat=True)
    leaf = leaf_mat("#7ACC50", "#2E7A30", "#C8F0A0")
    twig = plain("#6A4A2A")

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        top = p["top"] + p["bh"]
        b = bone_of(m, "body")
        b.cubes += blob(S([-4, top - 1, -4.5]), S([8, 4.4, 9]), shellm) + [Cube(S([-3, top + 3.4, -3.5]), [n(6 * s), 0, n(7 * s)], rings)]
        b.cubes += [Cube(S([-4.1, top - 0.6, -4]), [0, n(3 * s), n(8 * s)], moss), Cube(S([4.1, top - 0.6, -4]), [0, n(3 * s), n(8 * s)], moss)]
        for x, z, h in ((-1.4, -1, 4), (1.6, 1.4, 3)):
            b.cubes.append(Cube(S([x - 0.5, top + 3.4, z - 0.5]), [1, n(h * s), 1], twig))
            for a in (-40, 40, 0):
                b.cubes.append(Cube(S([x - 1.2, top + 3.4 + h - 0.6, z]), [n(2.4 * s), n(2.6 * s), 0], leaf, [0, a + 90, 25 if a else 0], S([x, top + 3.4 + h - 0.6, z])))
    quad("sudowoodo", "STUMPSHELL", 1.2, dict(
        s=1.15, body=(7, 3.6, 8), top=3.4, FUR="#8A8A80", FUR_L="#B0B0A6", FUR_D="#5A5A54", BELLY="#C8B890", coat=lambda a, b: rgb("#6A6A62") if (a.row + a.col) % 3 == 0 and a.face != "bottom" else None,
        FACE="#9A9A90", head=(3.6, 3.4, 3.6), snout=1.2, snout_w=0.8, snout_h=1.6, MUZZLE="#7A7A70", NOSE="#3A3A36", neck=3, neck_up=-1, neck_tilt=-4,
        ears=None, IRIS="#3A2A10", IRIS_L="#A88A3A", eye_grid=["LL", "IP"], eye_y=0.58, leg_w=2.4, toes=3, foot=Mat("#5A5A54", noise=0.01),
        tail={"segs": [(1.2, 1.2, 1.4)], "rests": [-20]}, extra=extra, moves=("ROCK THROW", "MIMIC"), walk=0.9, stride=18, k=0.5, slow=1.4))


# ======================================================================================================
# The chorus toad: CHORUSTOAD (in place of Politoed; from FRILLFROG)
# ======================================================================================================

def chorustoad():
    """A chorus toad, the frog grown into a singer: plump and upright, leaf-green flecked with gold, a butter-yellow
    belly, big golden eyes with dark bars across them, a great vocal sac under its chin that swells out pale as it
    calls, webbed hands held to its chest and long hind feet."""
    G, GL, GD = "#4E9A3A", "#7AC860", "#2A6022"

    def coat(at, base):
        if hash01("gold", round(at.x * 1.3), round(at.y * 1.3), round(at.z * 1.3)) < 0.07: return rgb("#E8C84A")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        sac = Mat("#F4E8B8", lambda a: rgb("#E8D49A") if a.face == "bottom" else None, noise=0.02)
        m.bone("sac", "head", S([0, p["hy"] + 0.2, p["fz"] + 1.2]), blob(S([-2.2, p["hy"] - 1.6, p["fz"] - 0.4]), S([4.4, 2.8, 3.4]), sac))
        web = Mat("#3A7A2E", lambda a: CLEAR if (a.lx * 3) % 1.0 > 0.82 else None, noise=0, edge=0, flat=True)
        for side in ("foot_right", "foot_left"):
            b = bone_of(m, side)
            c = b.cubes[0]
            b.cubes.append(Cube([c.origin[0] - 1, 0.05, c.origin[2] - 2], [c.size[0] + 2, 0, 2.4], web))
    upright("politoed", "CHORUSTOAD", 1.1, dict(
        s=1.3, hip=3, body=(8, 7.6, 7), FUR=G, FUR_L=GL, FUR_D=GD, BELLY="#F2E08A", coat=coat, belly_size=(5.6, 6), FACE=None, head=(7.4, 4.6, 6), head_y=-1.6,
        head_z=-0.6, nose=True, NOSE="#2A4A22", mouth_y=0.16, mouth_w=4.4, MUZZLE="#E8D88A", ears=None, IRIS="#E8B830", IRIS_L="#FFE07A", PUPIL="#141008",
        eye_w=3, eye_h=3, eye_in=1.2, eye_y=0.5, eye_grid=["WII", "PPP", "III"], arms=(2.4, 2.4, 2), HAND="#7AC860", fingers=3, leg_w=2.4, foot=4.4, FOOT=GD, toes=3,
        thigh=3.6, extra=extra, moves=("DOUBLESLAP", "HYDRO PUMP"),
        anim_extras={"idle": {"sac": track(scale=keys({0: [1, 1, 1], 2.2: [1, 1, 1], 2.5: [1.25, 1.3, 1.2], 2.9: [1, 1, 1], 3.0: [1, 1, 1]}))},
                     "cry": {"sac": track(scale=keys({0: [1, 1, 1], 0.2: [1.5, 1.7, 1.4], 0.8: [1.5, 1.7, 1.4], 1.0: [1, 1, 1]}))}}))


# ======================================================================================================
# The maple keys: WHIRLKEY > TWIRLKEY > GALEKEY (in place of Hoppip, Skiploom, Jumpluff)
# ======================================================================================================

def maple_key(sid, name, stands, stage):
    """A maple's winged seed come alive: a round seed with a face, a pair of papery wings joined on top of it that spin
    like a whirligig and carry it on the wind, and two little rootlets for feet. The first's wings spring-red; the next
    has four, gone gold; the last, an autumn seed, has a crown of five crimson-and-amber wings and a long trailing
    rootlet, and rides the gales between towns."""
    m = Model()
    s = (1.0, 1.25, 1.5)[stage]
    S = S_(s)
    SEED, SEED_L, SEED_D = ("#9AC060", "#C8E08A", "#5A7A30") if stage == 0 else (("#C8A050", "#E8C87A", "#8A6A2A") if stage == 1 else ("#A8683A", "#D89A5A", "#6A3A1E"))
    WING, WING_L, WING_D = (("#E86A6A", "#FFB0A0", "#A83A3A"), ("#F0C040", "#FFE88A", "#B88A20"), ("#D84A2A", "#F8B048", "#8A2A1A"))[stage]
    seed = Mat(SEED, fur(SEED, SEED_L, SEED_D, speck=0.2), noise=0.02)

    def samara(a):
        # A key's wing: narrow at the seed, broadening out to a rounded end, veins running out along it.
        u, v = a.ly, a.lx
        half = 0.18 + 0.32 * min(1.0, u * 1.4) * (1 - max(0.0, u - 0.85) * 3)
        if abs(v - 0.5) > half or u > 0.98:
            return CLEAR
        c = mix(WING_D, WING_L, 0.25 + 0.6 * u)
        if abs(((v * 7 + u * 2) % 1.0) - 0.5) < 0.08:
            c = mix(c, WING_D, 0.4)
        return c
    wingm = Mat(WING, samara, noise=0.01, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    y0 = 5
    m.bone("body", "root", S([0, y0, 0]), [])
    m.bone("head", "body", S([0, y0, 0]), blob(S([-3, y0 - 3, -3]), S([6, 6, 6]), seed, b=2))
    m.bone("jaw", "head", S([0, y0 - 0.6, -3]), [Cube(S([-0.8, y0 - 1, -3.4]), [n(1.6 * s), 1, 1], Mat(SEED_D, open_mouth("#8A3A3A", SEED_D), noise=0.02))])
    m.eyes("head", x_inner=0.6 * s, y=(y0 + 0.2) * s, z=-3 * s, w=2, h=2, iris="#1A1410", pupil="#050302", skin=SEED, grid=["PW", "PP"])
    nw = (2, 4, 5)[stage]
    L = (8, 9, 10)[stage]
    rot = []
    for i in range(nw):
        a = i * 360 / nw
        rot.append(Cube(S([-1.6, y0 + 2.6, 0]), [n(3.2 * s), n(L * s), 0], wingm, [0, a, 70], S([0, y0 + 2.6, 0])))
    m.bone("rotor", "head", S([0, y0 + 2.8, 0]), rot + [Cube(S([-0.6, y0 + 2.4, -0.6]), [n(1.2 * s), n(1.2 * s), n(1.2 * s)], Mat(WING_D, noise=0.01))])
    rootm = plain("#E8DCB8")
    feet = []
    for x in (-1.4, 1.4):
        feet.append(Cube(S([x - 0.4, y0 - 4.4, -0.4]), [1, n(1.6 * s), 1], rootm))
    if stage == 2:
        feet.append(Cube(S([-0.4, y0 - 7.4, 0.6]), [1, n(4.4 * s), 1], rootm, [20, 0, 0], S([0, y0 - 3, 0.6])))
    bone_of(m, "body").cubes += feet
    fx_for(m, 0.7 * s)
    moves(m, ("TACKLE", "SPLASH", "MEGA DRAIN")[stage], ("POISONPOWDER", "STUN SPORE", "SLEEP POWDER")[stage])
    T = (1.2, 1.0, 0.9)[stage]
    spin = track(rotation=keys({0: [0, 0, 0], T / 4: [0, 90, 0], T / 2: [0, 180, 0], 3 * T / 4: [0, 270, 0], T: [0, 360, 0]}))
    A, info = floater(name.lower(), m, k=0.8 * s, bob=1.2, extras={"idle": {"rotor": spin}, "walk": {"rotor": spin}, "battle": {"rotor": spin},
                                                                   "cry": {"rotor": spin}})
    write(sid, name, m, A, info, stands)


def whirlkey():
    maple_key("hoppip", "WHIRLKEY", 0.4, 0)


def twirlkey():
    maple_key("skiploom", "TWIRLKEY", 0.6, 1)


def galekey():
    maple_key("jumpluff", "GALEKEY", 0.8, 2)


# ======================================================================================================
# The kinkajou: KINKATAIL (in place of Aipom)
# ======================================================================================================

def kinkatail():
    """A kinkajou, the honey bear of the treetops: soft honey-gold fur, a round face with huge dark eyes made for the
    night, small round ears set low, a long tongue for nectar, and a long gripping tail it curls up over its back, a
    ripe fig held in the curl."""
    FUR, FUR_L, FUR_D = "#C8923A", "#E8BA6A", "#8A5A20"
    fig = Mat("#7A3A6A", lambda a: rgb("#A85A8A") if a.face == "top" else None, noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        tb.cubes += [Cube([-1.2 * s, tb.pivot[1] - 1.4 * s, z - 1.6 * s], [n(2.4 * s), n(2.4 * s), n(2.4 * s)], fig),
                     Cube([-0.3, tb.pivot[1] + 1.0 * s, z - 0.8 * s], [1, 1, 1], plain("#5A7A30"))]
    upright("aipom", "KINKATAIL", 0.8, dict(
        s=1.1, hip=3.4, body=(6, 6.4, 5.4), FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#F0D49A", belly_size=(3.6, 4.6), FACE="#E8C88A", face_line=0.4,
        head=(6, 5.2, 5), head_y=-0.4, snout=(1.2, 2.4, 1.8), NOSE="#2A1A10", MUZZLE="#E8C88A", ears=("round", 2, 2), EAR_IN="#8A5A2A", ear_x=2.6,
        ear_rot=[-4, 20, -40], IRIS="#1A0E08", PUPIL="#050302", IRIS_L="#5A3A1A", eye_w=2, eye_h=3, eye_in=0.7, eye_y=0.36, eye_grid=["PW", "PP", "KP"],
        arms=(2.4, 2.2, 1.6), HAND="#6A4420", claws=0, fingers=0, leg_w=2, foot=3, FOOT="#6A4420", thigh=2.8,
        tail={"segs": [(1.6, 1.6, 3), (1.5, 1.5, 3), (1.4, 1.4, 3), (1.3, 1.3, 2.6), (1.2, 1.2, 2.4)], "rests": [40, 40, 34, 40, 60], "y": 1.2}, extra=extra,
        moves=("SCRATCH", "BATON PASS"), walk=0.5, k=0.8))


# ======================================================================================================
# The sunchokes: SUNPIP > SOLBLOOM (in place of Sunkern, Sunflora)
# ======================================================================================================

def sunpip():
    """A sunchoke's knobbly tuber just sprouting: a round knobbled root, sandy-gold, with a face, two seed leaves
    unfolding on top and the first fine root hairs for feet."""
    m = Model()
    s = 1.0
    S = S_(s)
    TU, TU_L, TU_D = "#D8A860", "#F0CC88", "#9A7034"
    tub = Mat(TU, fur(TU, TU_L, TU_D, speck=0.4), lambda a: rgb(TU_D) if hash01("eye", round(a.x), round(a.y), round(a.z)) < 0.05 else None, noise=0.02)
    leaf = leaf_mat("#8AD060", "#3A8A30", "#D0F0A8")
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 3.4, 0]), blob(S([-3, 0.4, -2.8]), S([6, 5.4, 5.6]), tub, b=2) + blob(S([1.6, 3.6, -1]), S([2.4, 2.4, 2.4]), tub) +
           blob(S([-3.6, 1.6, 0]), S([2, 2, 2.4]), tub))
    m.bone("head", "body", S([0, 5.8, 0]), [Cube(S([-0.4, 5.6, -0.4]), [1, n(1.6 * s), 1], plain("#5A9A3A"))] +
           [Cube(S([-1.4, 7, 0]), [n(2.8 * s), 0, n(3.6 * s)], leaf, [a, 0, 0], S([0, 7, 0])) for a in (-150, -30)])
    m.bone("jaw", "body", S([0, 2.0, -2.8]), [Cube(S([-0.8, 1.6, -3.2]), [n(1.6 * s), 1, 1], Mat(TU_D, open_mouth("#8A3A2A", TU_D), noise=0.02))])
    m.eyes("body", x_inner=0.6 * s, y=2.8 * s, z=-2.8 * s, w=2, h=2, iris="#2A1A0A", pupil="#0A0604", skin=TU, grid=["PW", "PP"])
    m.bone("rootlets", "body", S([0, 0.6, 0]), [Cube(S([x - 0.4, -0.4, -0.4]), [1, n(1.2 * s), 1], plain("#E8DCB8")) for x in (-1.2, 1.2)])
    fx_for(m, 0.6)
    moves(m, "ABSORB", "GROWTH")
    A, info = hopper("sunpip", m, hop=0.5, height=1.2, k=0.5, extras={"idle": {"head": track(rotation=[wave(2.4, 6), 0, wave(3.2, 6)])}})
    write("sunkern", "SUNPIP", m, A, info, 0.3)


def solbloom():
    """A sunchoke grown tall: a sturdy green stem of a body with a face, rough heart-shaped leaves for arms, its knobbly
    gold tubers for feet, and a crown of small sunflowers, each gold-rayed round a brown heart, all turned to the sun."""
    STEM, STEM_L, STEM_D = "#5A9A3A", "#8ACA5A", "#2E6022"
    leaf = leaf_mat("#6AB848", "#2A6A2A", "#B8E890", base_end=1)
    ray = face_petal("#F8C428", "#FFEC8A", "#D8941A")
    heart = Mat("#5A3416", lambda a: rgb("#8A5424") if (a.row * 2 + a.col) % 3 == 0 else None, noise=0.02)
    tub = Mat("#D8A860", fur("#D8A860", "#F0CC88", "#9A7034", speck=0.4), noise=0.02)

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        hd = bone_of(m, "head")
        top = p["hy"] + p["hh"]
        for cx, cz, h, r in ((0, -0.2, 3.4, 3.2), (-3, 0.6, 1.8, 2.4), (3, 0.6, 2.2, 2.4)):
            hd.cubes.append(Cube(S([cx - 0.4, top, cz - 0.4]), [1, n(h * s), 1], plain(STEM_D)))
            hd.cubes += flower_face(cx, top + h + r * 0.4, cz - 0.6, r, 10, ray, heart, s, tilt=-20)
        for side in ("forearm_right", "forearm_left"):
            b = bone_of(m, side)
            hand = b.cubes[-1]
            sg = -1 if side.endswith("right") else 1
            b.cubes.append(Cube([hand.origin[0] + (0 if sg < 0 else 0), hand.origin[1] - 3 * s, hand.origin[2]], [n(3 * s), n(4 * s), 0], leaf))
        for side in ("foot_right", "foot_left"):
            b = bone_of(m, side)
            c = b.cubes[0]
            b.cubes[0] = Cube(c.origin, c.size, tub)
    upright("sunflora", "SOLBLOOM", 0.8, dict(
        s=1.2, hip=3, body=(4.6, 6, 4.2), FUR=STEM, FUR_L=STEM_L, FUR_D=STEM_D, BELLY="#A8D878", belly_patch=False, FACE=None, head=(5, 4.4, 4.4), head_y=-0.4,
        nose=False, mouth_y=0.2, MUZZLE=STEM_D, ears=None, IRIS="#3A2410", PUPIL="#0A0604", IRIS_L="#8A6A3A", eye_in=0.6, eye_y=0.48, eye_grid=["PW", "PP"],
        arms=(2.4, 2.4, 1.2), HAND=STEM, leg_w=1.6, foot=3, thigh=2, extra=extra, moves=("RAZOR LEAF", "SOLARBEAM"), walk=0.6, k=0.6))


# ======================================================================================================
# The emperor darner: SKIMWING (in place of Yanma)
# ======================================================================================================

def skimwing():
    """An emperor darner: an apple-green thorax, a long sky-blue body ringed in black with a black line down it, great
    green compound eyes meeting over its brow, and four glassy wings it holds out flat, beating them in a blur."""
    def abd(a):
        if a.face == "top" and abs(a.lx - 0.5) < 0.14: return rgb("#141E2A")
        if int(a.lz * 4) % 4 == 3: return rgb("#141E2A")
        return None

    def extra(p):
        m, S, s = p["m"], p["S"], p["s"]
        # Two more joints of its long body.
        prev, z = "abdomen3", p["tl"] / 2 - 0.4 + 3 * 3.0
        am = Mat("#3AA0E8", abd, noise=0.02)
        for i in range(2):
            nm = f"abdomen{4 + i}"
            m.bone(nm, prev, S([0, p["y"], z]), [Cube(S([-0.9, p["y"] - 0.9, z - 0.2]), [n(1.8 * s), n(1.8 * s), n(3.2 * s)], am)], rotation=[-2, 0, 0])
            prev, z = nm, z + 3
        bone_of(m, prev).cubes.append(Cube(S([-1.2, p["y"] - 0.6, z]), [n(2.4 * s), 0, n(1.6 * s)], plain("#141E2A")))
    bug("yanma", "SKIMWING", 1.2, dict(
        s=1.2, y=7, B="#6AC040", B_D="#3A7A22", B_L="#A8E07A", HEAD="#5AAA38", thorax=(4, 3.6, 4.4), head=(4.8, 3.6, 3), abdomen=(2.2, 2), segs=3, seg_l=3,
        ABD="#3AA0E8", abd_paint=abd, abd_tilt=-2, IRIS="#2A8A2A", IRIS_L="#9AF07A", eye_h=3, leg_len=3, LEGC="#1A2416", wings=11, WING="#E8F4FC", VEIN="#5A6A7A",
        WING_EDGE="#2A3A4A", WING_TINT="#D8C070", wing_rest=4, extra=extra, moves=("QUICK ATTACK", "SONICBOOM"), flap=0.08, amp=30))


# ======================================================================================================
# The mudskippers: MUDSKIP > BOGSKIPPER (in place of Wooper, Quagsire)
# ======================================================================================================

def mudskipper(sid, name, stands, big):
    """A mudskipper, at home on the mud flats: a long olive-brown body flecked with bright blue spots, bulging eyes
    perched right up on top of its head, a wide grinning mouth, two tall sail-like fins along its back edged in
    electric blue, and stout fins at its front it props itself up and skips along on. Grown, a big stocky one, its back
    fins great sails, a broad grin, its spots in rows."""
    m = Model()
    s = 1.0 if not big else 1.8
    S = S_(s)
    B, B_L, B_D = ("#8A8A5A", "#B8B88A", "#5A5A34") if not big else ("#6A7A6A", "#9AAA9A", "#3A4A3E")
    SPOT = "#4AC8FF"

    def skin_paint(a):
        if a.face == "bottom" or (a.y < 3 * s and a.face != "top"):
            return rgb("#E8DCC0")
        c = mix(B_D, B, 0.4 + 0.6 * a.ly)
        if hash01("mudspot", round(a.x * 1.5), round(a.y * 1.5), round(a.z * 1.5)) < (0.08 if not big else 0.11):
            return rgb(SPOT)
        return c
    skin = Mat(B, skin_paint, noise=0.02)
    sail = fin_mat("#6A6A4A", "#9A9A7A", "#3A3A24", "sail", glow=None)
    edge_sail = Mat("#6A6A4A", lambda a: CLEAR if a.ly > 1.0 - 0.6 * a.lz else (rgb(SPOT) if a.ly > 0.9 - 0.6 * a.lz else mix("#3A3A24", "#9A9A7A", a.ly)), noise=0.01, edge=0, flat=True)
    m.bone("root", None, [0, 0, 0])
    y0 = 3
    bl = 9 if not big else 9
    m.bone("body", "root", S([0, y0, 0]), blob(S([-2.6, y0 - 2, -3]), S([5.2, 4.4, 8]), skin) +
           [Cube([0, (y0 + 2.2) * s, -1 * s], [0, n(3.4 * s), n(3 * s)], edge_sail), Cube([0, (y0 + 2) * s, 2.6 * s], [0, n(2.6 * s), n(3.4 * s)], edge_sail)])
    m.bone("head", "body", S([0, y0 + 0.6, -3]), face_blob(S([-2.8, y0 - 1.6, -6.4]), S([5.6, 4.2, 4]), skin) +
           blob(S([-2.6, y0 + 2.2, -5.4]), S([2.2, 2, 2.2]), skin) + blob(S([0.4, y0 + 2.2, -5.4]), S([2.2, 2, 2.2]), skin))
    m.bone("jaw", "head", S([0, y0 - 0.6, -5]), [Cube(S([-2.2, y0 - 1.6, -6.7]), [n(4.4 * s), 1, n(2.4 * s)], Mat("#E8DCC0", open_mouth("#B84A4A", "#E8DCC0"), noise=0.02))])
    # The eyes on top, bulging up out of their sockets.
    m.eyes("head", 0.4 * s, (y0 + 2.4) * s, -5.42 * s, w=2, h=2, iris="#C8A030", pupil="#0A0804", skin=B, iris_light="#FFE07A", grid=["PW", "PP"])
    # A grin's line round the front of its face.
    bone_of(m, "head").cubes.append(Cube(S([-2.4, y0 - 0.9, -6.45]), [n(4.8 * s), 0 if False else 1, 0], plain("#2A2418")))
    pec = fin_mat("#7A7A50", "#A8A880", "#4A4A2A", "round")
    m.pair("arm_right", "body", S([-2.4, y0 - 0.6, -2]), [Cube(S([-2.8, y0 - 3, -2.8]), [n(1.4 * s), n(3 * s), n(1.6 * s)], skin),
                                                         Cube(S([-3.4, y0 - 3, -3.4]), [n(2.6 * s), 0, n(2.6 * s)], pec)], rotation=[0, 0, 0])
    m.pair("leg_right", "body", S([-1.4, y0 - 1.4, 2]), [Cube(S([-2, y0 - 3, 1.4]), [n(1.2 * s), n(1.4 * s), n(1.2 * s)], skin)])
    m.pair("foot_right", "leg_right", S([-1.4, y0 - 3, 2]), [Cube(S([-2.4, 0, 0.6]), [n(2 * s), 0, n(2 * s)], pec)])
    names, zt = tail_bones(m, "body", y0 * s, 4.6 * s, [(3.4 * s, 3 * s, 2.6 * s), (2.4 * s, 2.2 * s, 2.4 * s), (1.6 * s, 1.6 * s, 2 * s)], skin, (-6, -4, 8))
    bone_of(m, names[-1]).cubes.append(Cube([0, (y0 - 2) * s, zt - 0.6], [0, n(4 * s), n(3 * s)], fin_mat("#7A7A50", "#A8A880", "#4A4A2A", "round")))
    fx_for(m, 0.7 * s)
    moves(m, "TACKLE" if not big else "EARTHQUAKE", "WATER GUN" if not big else "SURF")
    A, info = hopper(name.lower(), m, hop=0.4 if not big else 0.55, height=(1.4 if not big else 1.0) * s, k=0.6 * s)
    write(sid, name, m, A, info, stands)


def mudskip():
    mudskipper("wooper", "MUDSKIP", 0.4, False)


def bogskipper():
    mudskipper("quagsire", "BOGSKIPPER", 1.4, True)


NAMES6 = {}


def main(names):
    for f in ALL6:
        if not names or f.__name__ in names:
            f()
    import mons4
    mons4.refresh_names()


ALL6 = (noctivane, staticray, stormray, lunakit, humchin, downlet, gracedove, hooplet, omenhoop, floretta, dewvole, brimvole, stumpshell, chorustoad,
        whirlkey, twirlkey, galekey, kinkatail, sunpip, solbloom, skimwing, mudskip, bogskipper)

if __name__ == "__main__":
    main(set(sys.argv[1:]))
