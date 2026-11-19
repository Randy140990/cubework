"""
Retromon's own creatures, second set: the Route 22 families and the Viridian Forest gliders.

  QUILLETTE, QUILLENNE, QUILLDAME   (in place of Nidoran F, Nidorina, Nidoqueen): teal porcupines, quills tipped blue.
  QUILLBUCK, QUILLHORN, QUILLTUSK   (in place of Nidoran M, Nidorino, Nidoking): violet porcupines with a horn.
  SCRAPKIT, RAGEMONG                (in place of Mankey, Primeape): quick-tempered masked mongooses.
  SHRIKET, GALESHRIKE               (in place of Spearow, Fearow): fierce shrikes with a bandit mask and a hooked beak.
  ZIPSY, ZAPGLIDER, STORMGLIDER     (in place of Pichu, Pikachu, Raichu): golden sugar gliders lit with electric stripes.

Each family is one design grown stage by stage. Only the look and the name are Retromon's: underneath each is the same
species, so stats, moves, levelling and evolution are exactly the counterpart's.

Run: python3 tools/mons2.py [function names...]   (writes models, animations and textures, and refreshes names.json)
"""

import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import (CLEAR, Cube, Mat, Model, OUT_MODELS, FRAMES, blob, cell, face_blob, hash01, mix, ramp, rgb, track, wave, keys,  # noqa: E402
                    merge, away, write, wind_mat, burst_mat)
from monanims import four_legs, bird  # noqa: E402
from mons import (stout_legs, fx_for, fly_out, talon_mat, trousers, quail_feet, folded_wing, feather_plane,  # noqa: E402
                  jaw_teeth, attack_move)


# ---------- shared painting ----------

def fur(base, light, dark, belly=None, below=None, speck=0.35):
    """Fur: lighter along the top, shading down the sides, a paler belly, flecked."""
    def f(at):
        if belly is not None and (at.face == "bottom" or (below is not None and at.y < below and at.face != "top")):
            return rgb(belly)
        if at.face == "top":
            return mix(base, light, 0.55 + 0.25 * cell(at.x, at.y, at.z, 1.3))
        c = mix(dark, base, 0.45 + 0.55 * at.ly)
        if cell(at.x * 1.1, at.y, at.z * 1.1, 1.0) > 1 - speck * 0.3:
            c = mix(c, light, 0.35)
        return c
    return f


def brush(base, light, dark, tip=None, tip_from=0.7, band=None):
    """A brush of long fur on crossed flat parts along z (a bushy tail): thin at its root, full in the middle, drawn to a
    soft point, its edges ragged with hairs; a coloured tip, and a band across it if asked."""
    def f(at):
        c = at.cube
        u = at.lx if c.size[1] == 0 else at.ly
        t = at.lz
        hw = 0.5 * math.sin(math.pi * min(1.0, max(0.0, t)) ** 0.7)
        hair = hash01("hair", round(u * 12), round(t * 10))
        if abs(u - 0.5) > hw * (0.75 + 0.25 * hair):
            return CLEAR
        if tip is not None and t > tip_from:
            return mix(tip, light, hair * 0.3)
        if band is not None and band[0] < t < band[1]:
            return mix(dark, base, hair * 0.4)
        return mix(base, light if hair > 0.6 else dark, abs(hair - 0.5))
    return f


def bolt_shape(at, fi, frames=FRAMES):
    """Lightning along a flat part, frame by frame: a thick jagged bolt with sharp corners that jumps about, a fork
    branching off it, white-hot at the core, yellow round it, a pale blue glow at its edges."""
    c = at.cube
    if c.size[1] == 0:
        u, v = at.lz, at.lx
    elif c.size[0] == 0:
        u, v = (at.lz, at.ly) if c.size[2] else (at.ly, at.lz)
    else:
        u, v = at.lx, at.ly

    def line(u, seed, amp, seg):
        p = u * seg
        i = int(math.floor(p))
        k = p - i
        e0 = hash01(seed, i, fi) * 2 - 1
        e1 = hash01(seed, i + 1, fi) * 2 - 1
        return 0.5 + amp * (e0 + (e1 - e0) * k)

    d = abs(v - line(u, "bolt", 0.34, 4))
    if u > 0.45:
        fork = line(u, "fork", 0.2, 3) + (u - 0.45) * (0.5 if hash01("fd", fi) > 0.5 else -0.5)
        d = min(d, abs(v - fork) + 0.03)
    if d < 0.08:
        return rgb("#FFFFFF"), 255
    if d < 0.16:
        return rgb("#FFF06A"), 255
    if d < 0.27:
        return rgb("#86EEFF"), 140
    return None


def bolt_mat():
    return Mat("#FFF27A", lambda a: CLEAR, glow=lambda a, fi: bolt_shape(a, fi), frames=True, noise=0, edge=0)


def slash_shape(at, fi):
    """A chop's slash: a bright crescent sweeping through the air."""
    c = at.cube
    u, v = (at.lz, at.ly) if c.size[0] == 0 else (at.lx, at.ly)
    r1 = math.hypot(u - 0.5, v - 0.5)
    r2 = math.hypot(u - 0.66, v - 0.5)
    if r1 > 0.48 or r2 < 0.42:
        return None
    k = (r2 - 0.42) / 0.2
    return (rgb("#FFFFFF") if k < 0.35 else mix("#FFF6C0", "#FFD060", k)), max(120, 255 - 120 * k)


def slash_mat():
    return Mat("#FFFFFF", lambda a: CLEAR, glow=lambda a, fi: slash_shape(a, fi), frames=True, noise=0, edge=0)


def lift(m, dy):
    """Raises everything built so far by dy (to stand it on longer legs added after)."""
    for b in m.bones:
        b.pivot = [b.pivot[0], b.pivot[1] + dy, b.pivot[2]]
        for c in b.cubes:
            c.origin = [c.origin[0], c.origin[1] + dy, c.origin[2]]
            if c.pivot:
                c.pivot = [c.pivot[0], c.pivot[1] + dy, c.pivot[2]]


def ear_shape(u, v, pointed):
    """Where an ear is, on its flat part: rounded (a broad oval on a narrower base) or long and pointed."""
    if pointed:
        hw = 0.5 * (1 - v) ** 0.7 if v > 0.25 else 0.32 + 0.18 * v / 0.25
    elif v < 0.5:
        hw = 0.3 + 0.2 * v / 0.5
    else:
        hw = 0.5 * math.sqrt(max(0.0, 1 - ((v - 0.5) / 0.5) ** 2))
    return abs(u - 0.5) <= hw, hw


def S_(s):
    return lambda v: [x * s for x in v]


def n(v):
    """A size of at least one texel."""
    return max(1, round(v))


def done(sid, name, m, A, info, stands):
    write(sid, name, m, A, info, stands)


# ---------- the porcupines (the Nidoran lines) ----------

def porcupine(sid, name, stands, stage, male):
    """A porcupine: stout and low, its back bristling with quills, a blunt rodent's snout with orange incisors. The
    females teal with blue-tipped quills and round ears, the males violet with a horn and pointed ears. Grown bigger
    each stage, with more and longer quills; at the last, heavy plates of earth on the belly, the queen a crown of
    quills, the king tusks, and venom glowing at the quills' tips."""
    m = Model()
    s = (1.0, 1.4, 2.1)[stage]
    S = S_(s)
    if male:
        FUR, FUR_L, FUR_D, BELLY, Q_B, Q_T, IRIS = "#9C6BB4", "#C49AD6", "#6A4484", "#F0E2F4", "#F6EEF8", "#5A2A78", "#C03A5A"
    else:
        FUR, FUR_L, FUR_D, BELLY, Q_B, Q_T, IRIS = "#5E9DB3", "#8CC4D6", "#3B6B80", "#DCEAF0", "#EEF4F6", "#33589A", "#C04060"
    PLATE, PLATE_D, VENOM = "#B49A6E", "#8E7650", "#C070FF"
    body = Mat(FUR, fur(FUR, FUR_L, FUR_D, BELLY, below=3.2 * s), noise=0.025)
    head = Mat(FUR, fur(FUR, FUR_L, FUR_D, BELLY, below=3.6 * s), noise=0.02)
    snout = Mat(BELLY, lambda a: rgb(mix(BELLY, FUR_D, 0.25)) if a.face == "bottom" else None, noise=0.02)
    nose = Mat("#3A2630", lambda a: rgb("#6A4A54") if a.face == "top" else None, noise=0.01, edge=0)
    tooth = Mat("#F0A040", lambda a: rgb("#D07820") if a.face in ("bottom", "front") and a.ly < 0.5 else None, noise=0.01, edge=0)
    jaw = Mat(BELLY, jaw_teeth("#B84A5A", "#F0A040", -9.2 * s), noise=0.02)
    ear = Mat(FUR, lambda a: rgb("#E8A0B0") if a.face == "front" and 0 < a.col < a.fw - 1 and a.row > 0 else None, noise=0.02)
    claw = talon_mat("#3A2E30", "#B8A8A0")
    legm = Mat(FUR_D, lambda a: rgb(mix(FUR_D, "#2A2030", 0.4)) if a.face == "bottom" else mix(FUR_D, FUR, a.ly), noise=0.025)

    def quill_p(at):
        t = at.lz
        if t > 0.72:
            return rgb(Q_T)
        return mix("#FFFFFF", Q_B, 0.4 + 0.6 * t) if (at.col + at.row) % 3 else mix(Q_B, Q_T, 0.25)

    quill = Mat(Q_B, quill_p, glow=(lambda a: rgb(VENOM) if a.lz > 0.86 else None) if stage == 2 else None, noise=0.01, edge=0)

    def spike_p(at, tip_front=False):
        # A quill as a sharp spike: wide at its root, drawn to a needle point, banded pale to dark at the tip.
        c = at.cube
        u = at.lx if c.size[1] == 0 else at.ly
        t = 1 - at.lz if tip_front else at.lz
        hw = 0.5 * (1 - t) ** 0.85 + 0.04
        if abs(u - 0.5) > hw:
            return CLEAR
        if t > 0.55:
            return mix(Q_T, "#1A1430", (t - 0.55) * 1.2)
        if abs(u - 0.5) < 0.12:
            return mix("#FFFFFF", Q_B, 0.3)
        return mix(Q_B, mix(Q_B, Q_T, 0.35), t / 0.55)

    spike = Mat(Q_B, spike_p, glow=(lambda a: rgb(VENOM) if a.lz > 0.82 else None) if stage == 2 else None, noise=0.0, edge=0)
    qw = 3 if stage == 0 else 4

    def quill_cubes(x, y, z, L, rot, mat=spike):
        """A spike: two crossed flat parts along z, so it's sharp from every side."""
        return [Cube([x - qw / 2, y, z], [qw, 0, L], mat, rot, [x, y, z]), Cube([x, y - qw / 2, z], [0, qw, L], mat, rot, [x, y, z])]

    m.bone("root", None, [0, 0, 0])
    bb = max(1, round(s * 0.8))
    m.bone("body", "root", S([0, 5, 0]), blob(S([-3.5, 2.5, -4.5]), S([7, 5, 9]), body, b=bb))
    if stage == 2:
        plate = Mat(PLATE, lambda a: rgb(PLATE_D) if a.row == a.fh - 1 or a.face == "bottom" else mix(PLATE, "#D2BC8E", a.ly * 0.5), noise=0.03)
        m.bones[-1].cubes += [Cube([-3 * s, 2.5 * s - 1, (-3.6 + 2.6 * i) * s], [n(6 * s), 1, n(2.2 * s)], plate) for i in range(3)]

    # The head: a blunt snout, the nose on its tip, orange incisors below.
    m.bone("head", "body", S([0, 5.5, -4.5]), face_blob(S([-2.5, 3, -8]), S([5, 4.5, 4]), head, b=bb) +
           [Cube(S([-1.5, 3, -9.5]), [n(3 * s), n(2 * s), n(1.5 * s)], snout), Cube([-n(2 * s) / 2, 4.5 * s, -9.5 * s - 0.5], [n(2 * s), 1, 1], nose),
            Cube([-1, 3 * s - 1, -9.5 * s + 0.2], [2, 1, 1], tooth)])
    m.bone("jaw", "head", S([0, 3, -6]), [Cube([-1.25 * s, 3 * s - 1, -9.2 * s], [n(2.5 * s), 1, n(3.5 * s)], jaw)])
    if male:
        m.pair("ear_right", "head", S([-2.2, 7.2, -6]), [Cube(S([-3.2, 7, -6.2]), [n(2 * s), n(2.5 * s), 1], ear)], rotation=[0, -10, -25])
    else:
        m.pair("ear_right", "head", S([-2.2, 7, -6]), [Cube(S([-3, 6.8, -6.2]), [n(1.6 * s), n(1.6 * s), 1], ear)], rotation=[0, -10, -15])
    if stage < 2:
        m.eyes("head", 2.5 * s, 4.6 * s, -8 * s, h=3, corner=(1, 2), half=2.5 * s, iris=IRIS, pupil="#140810", skin=FUR, iris_light="#F08098",
               grid=["LLL", "WPI", ".JJ"] if stage == 0 else ["DLL", "WPI", ".JJ"])
    else:
        m.eyes("head", x_inner=0.9 * s, y=5.3 * s, z=-8 * s, w=3, h=2, iris=IRIS, pupil="#140810", skin=FUR, iris_light="#F08098", grid=["DLL", "JPW"])
        m.brows("head", [Cube([-0.9 * s - 3.4, 5.3 * s + 1.7, -8 * s - 0.5], [4, 1, 1], Mat(FUR_D, noise=0.02, edge=0), [0, 0, 14], [-0.9 * s, 5.3 * s + 2.2, -8 * s])])

    # Horn (the males): a short spike, a curved horn, then a great swept-back horn.
    if male:
        hm = Mat("#EDE2C8", lambda a: mix("#EDE2C8", "#B8A888", 1 - a.ly), noise=0.02, edge=0)
        segs = [(1, 2)] if stage == 0 else [(2, 2), (1, 2)] if stage == 1 else [(3, 3), (2, 3), (1, 3)]
        parent, y, z = "head", 7.4 * s, -6.6 * s
        for i, (w, h) in enumerate(segs):
            nm = f"horn{i + 1}"
            m.bone(nm, parent, [0, y, z], [Cube([-w / 2, y, z - w / 2], [w, h, w], hm)], rotation=[-18 if i else -8, 0, 0])
            parent, y = nm, y + h - 0.5
        if stage == 2:
            tusk = Mat("#F2EAD6", lambda a: rgb("#C8B898") if a.face == "bottom" else None, noise=0.02, edge=0)
            m.pair("tusk_right", "jaw", [-1.6 * s, 3 * s, -9 * s], [Cube([-1.6 * s - 1, 3 * s - 0.5, -9.2 * s], [2, 4, 2], tusk)], rotation=[-25, 0, 20])
    elif stage == 2:
        crown = [Cube([-0.5 + 1.6 * math.sin(math.radians(a)), 7.2 * s, -5.5 * s + 1.6 * math.cos(math.radians(a))], [1, 1, n(2.4 * s)], quill,
                      [55, a * 0.4, 0], [1.6 * math.sin(math.radians(a)), 7.2 * s + 0.5, -5.5 * s + 1.6 * math.cos(math.radians(a))]) for a in (-60, -30, 0, 30, 60)]
        m.bone("crown", "head", [0, 7.2 * s, -5.5 * s], crown)

    # The quills: rows down its back, longer and raised higher towards the rump, the outer ones leaning out.
    nr, nc = (3, 4) if stage == 0 else (4, 5) if stage == 1 else (5, 6)
    qs = []
    for r in range(nr):
        fr = r / (nr - 1)
        z = (-3.2 + 7.2 * fr) * s
        for c in range(nc):
            xf = -1 + 2 * c / (nc - 1)
            x = xf * 2.6 * s
            y = 7.5 * s - abs(xf) ** 2 * 1.4 * s
            L = n((2.4 + 1.6 * fr + 0.8 * hash01("q", r, c)) * s) + (1 if stage == 2 else 0)
            qs += quill_cubes(x, y, z, L + 1, [38 + 24 * fr, xf * 18, xf * 30])
    m.bone("quills", "body", S([0, 7, 0]), qs)
    tl = 4.5 if stage == 2 else 2.5
    tq = [quill_cubes(dx, 5 * s, (4.3 + tl * 0.6) * s, n(2.8 * s), [25, dx * 10, 0]) for dx in ((-s, 0, s) if stage < 2 else (-1.5 * s, -0.5 * s, 0.5 * s, 1.5 * s))]
    m.bone("tail", "body", S([0, 4.5, 4.5]), [Cube(S([-1.5, 3.5, 4.3]), [n(3 * s), n(2 * s), n(tl * s)], body)] + [c for q in tq for c in q], rotation=[-10, 0, 0])
    stout_legs(m, 2.4 * s, (-2.8 * s, 2.8 * s), round(3 * s), max(2, round(2.2 * s)), legm, claw, 3, claw_len=2, sharp=True)

    # POISON STING: barbed quills shot from its back, their tips glowing with venom.
    fx_for(m, s)
    barb = Mat(Q_B, lambda a: (rgb(VENOM) if a.lz < 0.3 else mix("#FFFFFF", Q_B, a.lz)) if spike_p(a, True) is not CLEAR else CLEAR,
               glow=lambda a: rgb(VENOM) if a.lz < 0.3 and spike_p(a, True) is not CLEAR else None, noise=0.0, edge=0)
    St = 1.6 * (1.3 if stage == 2 else 1.0)
    names, sp = [], {}
    for i in range(10):
        # A volley of ten barbs, fired one after another from all over its back.
        nm = f"fx_barb{i + 1}"
        dx, dy = (hash01("bx", i) - 0.5) * 4 * s, (hash01("by", i) - 0.5) * 2 * s
        y, z = 8.5 * s + dy, -1 * s + (i % 3) * s
        L = n(4 * s)
        m.bone(nm, "root", [dx, y, z], [Cube([dx - qw / 2, y, z], [qw, 0, L], barb, effect=True), Cube([dx, y - qw / 2, z], [0, qw, L], barb, effect=True)])
        names.append(nm)
        sp.update(fly_out(nm, St * (0.40 + 0.045 * i), St * 0.26, [dx * 0.4, -2.5 * s - dy, -55 * s], spin=(0, 0, 720)))
    sp["quills"] = track(rotation=keys({0: [0, 0, 0], St * 0.3: [-14, 0, 0], St * 0.5: [8, 0, 0], St: [0, 0, 0]}))
    m.move_name, m.move_extras = "POISON STING", {"all": away(*names), "special": sp}

    bristle = {"quills": track(rotation=keys({0: [0, 0, 0], 0.25: [-16, 0, 0], 0.9: [-16, 0, 0], 1.2: [0, 0, 0]}),
                               scale=keys({0: [1, 1, 1], 0.25: [1.08, 1.12, 1.1], 0.9: [1.08, 1.12, 1.1], 1.2: [1, 1, 1]}))}
    attack_move(m, "HORN ATTACK" if male else "SCRATCH", 1.0 + 0.15 * stage, s)
    A, info = four_legs(sid.replace("_", ""), m, walk=0.6 + 0.15 * stage, stride=34 - 6 * stage, k=s, slow=1.0 + 0.15 * stage, idle="battle", extras={
        "idle": {"quills": track(scale=[1, wave(3, 0.02, 0, 1), 1])},
        "cry": bristle, "recoil": {"quills": track(rotation=keys({0: [0, 0, 0], 0.1: [-20, 0, 0], 0.5: [0, 0, 0]}))}})
    done(sid, name, m, A, info, stands)


def quillette():
    porcupine("nidoran_f", "QUILLETTE", 0.58, 0, False)


def quillenne():
    porcupine("nidorina", "QUILLENNE", 0.72, 1, False)


def quilldame():
    porcupine("nidoqueen", "QUILLDAME", 1.6, 2, False)


def quillbuck():
    porcupine("nidoran_m", "QUILLBUCK", 0.6, 0, True)


def quillhorn():
    porcupine("nidorino", "QUILLHORN", 1.02, 1, True)


def quilltusk():
    porcupine("nidoking", "QUILLTUSK", 1.8, 2, True)


# ---------- the mongooses (Mankey, Primeape) ----------

def mongoose(sid, name, stands, stage):
    """A mongoose: long and low, quick, its eyes in a dark bandit's mask and narrowed in a scowl, a long tail ringed with
    dark bands ending in a bushy brush. Grown, it's heavier at the shoulder, a pale mane bristling round its neck, tufts
    at its elbows, eyes burning red."""
    m = Model()
    s = (1.0, 1.45)[stage]
    S = S_(s)
    if stage == 0:
        FUR, FUR_L, FUR_D, BELLY, MASK, IRIS = "#C49A68", "#DDB98A", "#8A6642", "#EEDCC0", "#3E2C20", "#B8781E"
    else:
        FUR, FUR_L, FUR_D, BELLY, MASK, IRIS = "#B0784E", "#CC9668", "#7A4E30", "#F0DDBE", "#2E1E16", "#D0301E"
    MANE = "#E8D6B4"
    body = Mat(FUR, fur(FUR, FUR_L, FUR_D, BELLY, below=5.0 * s), noise=0.025)

    def head_p(at):
        eye_y = 8.6 * s
        if at.face in ("front", "nx", "px") and abs(at.y - eye_y) < 1.1 * s and (at.face != "front" or abs(at.x) > 0.4):
            return rgb(MASK)
        if at.face == "bottom" or (at.face == "front" and at.y < 8 * s):
            return rgb(BELLY)
        return fur(FUR, FUR_L, FUR_D)(at)

    head = Mat(FUR, head_p, noise=0.02)
    snout = Mat(BELLY, lambda a: rgb(mix(MASK, FUR_D, 0.4)) if a.face == "top" else None, noise=0.02)
    nose = Mat("#2A1E18", lambda a: rgb("#4A3A30") if a.face == "top" else None, noise=0.01, edge=0)
    jaw = Mat(BELLY, jaw_teeth("#B04850", "#FFFBF0", -12.5 * s), noise=0.02)
    ear = Mat(FUR, lambda a: rgb(MASK) if a.face == "front" and a.row > 0 else None, noise=0.02)
    claw = talon_mat("#2E2420", "#A89A90")
    legm = Mat(FUR_D, lambda a: rgb(MASK) if a.ly < 0.3 and a.face != "top" else mix(FUR_D, FUR, a.ly), noise=0.025)

    def ringed(at):
        if int(math.floor(at.z / (1.6 * s))) % 2 == 0 and at.face != "bottom":
            return mix(MASK, FUR_D, 0.4)
        return fur(FUR, FUR_L, FUR_D)(at)

    tailm = Mat(FUR, ringed, noise=0.02)
    tuft = Mat(FUR, brush(FUR, FUR_L, MASK, tip=MASK, tip_from=0.6), noise=0.0, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 6, 0]), blob(S([-2.5, 4, -5.5]), S([5, 4.5, 11]), body, b=1))
    if stage == 1:
        m.bones[-1].cubes += blob(S([-3, 4, -6]), S([6, 5.5, 5]), body, b=2)
    m.bone("neck", "body", S([0, 7.5, -5]), [Cube(S([-1.75, 6, -7.5]), [n(3.5 * s), n(3.5 * s), n(3 * s)], body)], rotation=[-20, 0, 0])
    m.bone("head", "neck", S([0, 9, -7.5]), face_blob(S([-2.25, 7.5, -10.5]), S([4.5, 3.5, 3.5]), head) +
           [Cube(S([-1.25, 7.5, -12.5]), [n(2.5 * s), n(2 * s), n(2 * s)], snout), Cube([-0.5 * n(1.5 * s), 9.5 * s - 1, -12.5 * s - 0.6], [n(1.5 * s), 1, 1], nose)])
    m.bone("jaw", "head", S([0, 7.5, -9]), [Cube(S([-1, 6.9, -12.5]), [n(2 * s), 1, n(4 * s)], jaw)])
    m.pair("ear_right", "head", S([-2, 10.6, -8.5]), [Cube(S([-2.6, 10.4, -8.8]), [n(1.4 * s), n(1.2 * s), 1], ear)], rotation=[0, -20, -20])
    w = 2 if stage == 0 else 3
    m.eyes("head", x_inner=0.35 * s if stage == 0 else 0.3 * s, y=8.3 * s, z=-10.5 * s, w=w, h=2, iris=IRIS, pupil="#120A06", skin=MASK,
           iris_light="#FFB040" if stage == 0 else "#FF6A3A", grid=["DL", "PW"] if stage == 0 else ["DLL", "JPW"])
    brow = Mat(FUR_D, noise=0.02, edge=0)
    m.brows("head", [Cube([-0.35 * s - w - 0.4, 8.3 * s + 1.7, -10.5 * s - 0.5], [w + 1, 1, 1], brow, [0, 0, 20], [-0.35 * s, 8.3 * s + 2.2, -10.5 * s])])
    if stage == 1:
        # The mane: bristling pale fur round the neck, and tufts at the elbows.
        mane = Mat(MANE, brush(MANE, "#FFFFFF", FUR_L), noise=0.0, edge=0)
        m.bone("mane", "neck", S([0, 8, -6]), [Cube([-3 * s, 7.5 * s, -6 * s], [n(6 * s), 0, n(4 * s)], mane, [rx, 0, rz], S([0, 8, -6]))
                                                for rx, rz in ((-40, 0), (-40, 60), (-40, -60), (-60, 120), (-60, -120))])
    m.bone("tail", "body", S([0, 6.5, 5.5]), [Cube(S([-1.25, 5.5, 5]), [n(2.5 * s), n(2.5 * s), n(4 * s)], tailm)], rotation=[-15, 0, 0])
    m.bone("tail2", "tail", S([0, 6.5, 9]), [Cube(S([-1, 5.75, 8.8]), [n(2 * s), n(2 * s), n(4 * s)], tailm)], rotation=[10, 0, 0])
    bw, bl = n(5 * s), n(7 * s)
    m.bone("tail3", "tail2", S([0, 6.75, 12.5]), [Cube([-bw / 2, 6.75 * s, 12.3 * s], [bw, 0, bl], tuft, [0, 0, a], [0, 6.75 * s, 12.3 * s])
                                                  for a in (0, 45, 90, 135)], rotation=[8, 0, 0])
    stout_legs(m, 1.9 * s, (-4 * s, 4 * s), round(4.5 * s), max(2, round(1.8 * s)), legm, claw, 4, sharp=True, claw_len=2 if stage == 2 else 1,
               extra=(lambda z: [Cube([-1.9 * s - 2, 2.6 * s, z - 1], [1, 2, 2], Mat(MANE, noise=0.02, edge=0))]) if stage == 1 else None)

    # KARATE CHOP: it rears up and chops down with a forepaw, a bright slash sweeping down in front of it.
    fx_for(m, s)
    St = 1.6 * (1.15 if stage else 1.0)
    q = lambda f: St * f
    hs = n(10 * s)
    fy, fz = 8 * s, -15 * s
    m.bone("fx_slash", "root", [0, fy, fz], [Cube([0, fy - hs / 2, fz - hs / 2], [0, hs, hs], slash_mat(), effect=True),
                                             Cube([-hs / 2, fy - hs / 2, fz], [hs, hs, 0], slash_mat(), effect=True)])
    sp = {"body": track(rotation=keys({0: [0, 0, 0], q(.3): [-32, 0, 0], q(.42): [-32, 0, 0], q(.52): [12, 0, 0], q(.8): [4, 0, 0], St: [0, 0, 0]}),
                        position=keys({0: [0, 0, 0], q(.3): [0, 2 * s, 1 * s], q(.52): [0, 0, -3 * s], St: [0, 0, 0]})),
          "leg_front_right": track(rotation=keys({0: [0, 0, 0], q(.3): [-120, 0, 0], q(.42): [-125, 0, 0], q(.52): [-15, 0, 0], q(.8): [-10, 0, 0],
                                                  St: [0, 0, 0]})),
          "fx_slash": track(scale=keys({0: [1, 1, 1], q(.42): [1, 1, 1], q(.44): [1000, 1000, 1000], q(.6): [1000, 1000, 1000], q(.62): [1, 1, 1]}),
                            rotation=keys({0: [0, 0, 0], q(.44): [-70, 0, 0], q(.6): [60, 0, 0]}))}
    m.move_name, m.move_extras = "KARATE CHOP", {"all": away("fx_slash"), "special": sp}
    stamp = {"leg_front_right": track(rotation=keys({0: [0, 0, 0], 0.2: [-40, 0, 0], 0.35: [5, 0, 0], 0.55: [-40, 0, 0], 0.7: [5, 0, 0], 1.2: [0, 0, 0]}))}
    attack_move(m, "SCRATCH", 1.0 + 0.1 * stage, s)
    A, info = four_legs(name.lower(), m, walk=0.5 + 0.1 * stage, stride=36, k=s, slow=1.0 + 0.1 * stage, idle="battle",
                        extras={"cry": stamp, "battle": {"tail3": track(rotation=[wave(0.6, 10), wave(1.2, 14), 0])}})
    done(sid, name, m, A, info, stands)


def scrapkit():
    mongoose("mankey", "SCRAPKIT", 0.72, 0)


def ragemong():
    mongoose("primeape", "RAGEMONG", 1.2, 1)


# ---------- the shrikes (Spearow, Fearow) ----------

def shrike(sid, name, stands, stage):
    """A shrike: a small fierce bird, grey-brown above and cream below, its eyes in a black bandit's mask, a hooked beak,
    rust-red wings with dark tips, and a ruffled crest. Grown, a long neck, a long hooked beak, a tall swept crest of rust
    feathers, and long tail streamers."""
    m = Model()
    s = (1.3, 2.9)[stage]  # built finely, to Cobblemon's level of detail; its size in the world is set by `stands`
    S = S_(s)
    BACK, BACK_L, BACK_D, BREAST, RUST, RUST_L, MASK = "#7E7068", "#A0928A", "#54484A", "#EFE2CE", "#B4522E", "#D27448", "#1E1818"
    hy, hz = (0, 0) if stage == 0 else (4.0, -2.0)

    def body_p(at):
        if at.face == "front" or (at.face in ("nx", "px", "bottom") and at.y < 5.2 * s):
            return mix(BREAST, "#D8C8AE", 0.4 * hash01("br", round(at.x), round(at.y), round(at.z)))
        return fur(BACK, BACK_L, BACK_D)(at)

    def head_p(at):
        eye_y = (9 + hy) * s
        if at.face in ("front", "nx", "px") and abs(at.y - eye_y) < 0.9 * s:
            return rgb(MASK)
        if at.face == "top":
            return mix(RUST, RUST_L, 0.4)
        if at.y < (8.2 + hy) * s:
            return rgb(BREAST)
        return mix(BACK, BACK_L, 0.4)

    body = Mat(BACK, body_p, noise=0.025)
    head = Mat(BACK, head_p, noise=0.02)
    beak = Mat("#3A3434", lambda a: rgb("#C8B8A0") if a.lz < 0.3 else (rgb("#5A5254") if a.face == "top" else None), noise=0.02, edge=0)
    crest = Mat(RUST, feather_plane(RUST, RUST_L, "#7A2A18", edge="#3A1A10"), noise=0.015, edge=0)
    cov = Mat(RUST, lambda a: mix(RUST, RUST_L, a.ly), noise=0.02)
    sec = Mat(RUST, feather_plane(RUST, RUST_L, "#3A2420", bars=BACK_D), noise=0.015, edge=0)
    pri = Mat(BACK_D, feather_plane(BACK_D, "#2A2224", "#1A1416", edge=BREAST), noise=0.015, edge=0)
    tfea = Mat(BACK, feather_plane(BACK, BACK_D, MASK, edge=RUST), noise=0.015, edge=0)
    legm = Mat("#4A4246", lambda a: rgb("#2E282C") if a.face == "bottom" else (mix("#4A4246", "#6A6266", 0.4) if a.row % 2 else None),
               noise=0.02, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 5, 0]), blob(S([-3, 3, -4]), S([6, 5, 8]), body, b=max(1, round(s * 0.8))))
    parent = "body"
    if stage == 1:
        m.bone("neck", "body", S([0, 7.5, -3.5]), [Cube(S([-1.5, 7, -5]), [n(3 * s), n(5 * s), n(3 * s)], head)], rotation=[-12, 0, 0])
        parent = "neck"
    m.bone("head", parent, S([0, 8 + hy, -3 + hz]), face_blob(S([-2.25, 7.5 + hy, -6 + hz]), S([4.5, 3.5, 4]), head))
    bl = 2 if stage == 0 else 3
    m.bone("beak", "head", S([0, 9 + hy, -6 + hz]), [Cube(S([-0.6, 8.6 + hy, -6 - bl + hz]), [n(1.2 * s), n(1.1 * s), n(bl * s)], beak),
                                                     Cube(S([-0.4, 7.8 + hy, -6 - bl + hz]), [n(0.8 * s), n(0.8 * s), n(0.8 * s)], beak, [30, 0, 0],
                                                          S([0, 8.6 + hy, -6 - bl + hz]))])
    m.bone("beak_lower", "head", S([0, 8.4 + hy, -6 + hz]), [Cube(S([-0.5, 8 + hy, -5.5 - bl * 0.7 + hz]), [n(s), 1, n(bl * 0.7 * s)],
                                                                  Mat("#4A4446", noise=0.01, edge=0))])
    nf = 3 if stage == 0 else 5
    m.bone("plume", "head", S([0, 11 + hy, -4 + hz]), [Cube([0, 11 * s + hy * s, (-4.5 + i * 1.2 + hz) * s], [0, n((2 + (stage * 2.5)) * s * 0.6), n(2.5 * s)], crest,
                                                            [-25 - i * 6, 0, 0], [0, (11 + hy) * s, (-4.5 + i * 1.2 + hz) * s]) for i in range(nf)])
    m.eyes("head", 2.25 * s, (8.3 + hy) * s, (-6 + hz) * s, h=3 if stage == 0 else 3, corner=(1, 2) if stage == 0 else (1, 3), half=2.25 * s,
           iris="#B8401E", pupil="#100808", skin=MASK, iris_light="#F09040", grid=["DLL", "WPI", ".JJ"] if stage == 0 else ["SDLL", "LPPW", ".JJI"])
    nsec, npri = (3, 2) if stage == 0 else (5, 4)
    folded_wing(m, 3 * s, 7.6 * s, -3.4 * s, n(6.5 * s), n(2.5 * s), 1,
                [(i * 1.2 * s, n(2 * s), n(3 * s)) for i in range(nsec)],
                [(i * 1.1 * s, n(2 * s), n((4.5 + 0.5 * i) * s)) for i in range(npri)], cov, sec, pri)
    if stage == 1:
        # A breast of scalloped feathers.
        breast = Mat(BREAST, lambda a: mix(BREAST, "#D8C8AE", 0.5) if (a.col + 2 * a.row) % 4 == 0 else None, noise=0.02, edge=0)
        m.bones[[b.name for b in m.bones].index("body")].cubes += [Cube(S([-2.5, 3.5, -4.6]), [n(5 * s), n(4 * s), 1], breast)]
    tl = n((4 if stage == 0 else 6) * s)
    tail = [Cube([-0.75 * s, 5 * s, 3.5 * s], [n(1.5 * s), 0, tl], tfea, [0, ry, 0], [0, 5 * s, 3.5 * s]) for ry in (-30, -15, 0, 15, 30)]
    if stage == 1:
        tail += [Cube([-0.5 * s + d, 5 * s, 3.5 * s], [n(s), 0, n(11 * s)], tfea, [0, d * 4, 0], [d, 5 * s, 3.5 * s]) for d in (-0.8 * s, 0.8 * s)]
        tail += [Cube([-0.75 * s, 5 * s + 0.2, 3.5 * s], [n(1.5 * s), 0, n(7 * s)], tfea, [0, ry, 0], [0, 5 * s, 3.5 * s]) for ry in (-45, -22, 22, 45)]
    m.bone("tail", "body", S([0, 5, 4]), tail, rotation=[-20, 0, 0])
    dy = 0 if stage == 0 else 4.0 * s  # Galeshrike stands tall on long legs
    if dy:
        lift(m, dy)
    quail_feet(m, 1.4 * s, 3 * s + dy, (1.8 if stage == 0 else 2.6) * s + dy, legm, max(1.0, s * 0.8), talon=talon_mat(),
               trouser=trousers(BACK, BACK_L, BACK_D) if stage == 1 else None)

    fx_for(m, s)
    St = 1.6 * (1.1 if stage else 1.0)
    q = lambda f: St * f
    by, bz = (8.6 + hy) * s + dy, (-6 - bl + hz) * s - 1
    if stage == 0:
        # FURY ATTACK: three quick pecks, a spark where each lands.
        m.bone("fx_peck", "head", [0, by, bz], [Cube([-3, by - 3, bz], [6, 6, 0], burst_mat(), effect=True)])
        pk = {0: [0, 0, 0]}
        sc = {0: [1, 1, 1]}
        for f in (.42, .56, .7):
            pk.update({q(f - .05): [-12, 0, 0], q(f): [28, 0, 0], q(f + .05): [0, 0, 0]})
            sc.update({q(f - .01): [1, 1, 1], q(f): [900, 900, 900], q(f + .04): [700, 700, 700], q(f + .05): [1, 1, 1]})
        pk[St] = [0, 0, 0]
        sp = {"head": track(rotation=keys(pk)), "fx_peck": track(scale=keys(sc))}
        m.move_name, m.move_extras = "FURY ATTACK", {"all": away("fx_peck"), "special": sp}
    else:
        # DRILL PECK: it dives in beak first, a spinning drill of wind round its beak.
        wm = wind_mat()
        r = n(7 * s * 0.6)
        rings = [Cube([-r / 2, by - r / 2, bz + i * 1.5], [r - i * 1, r - i * 1, 0], wm, effect=True) for i in range(3)]
        m.bone("fx_drill", "head", [0, by, bz], rings)
        sp = {"fx_drill": track(scale=keys({0: [1, 1, 1], q(.38): [1, 1, 1], q(.4): [1000, 1000, 1000], q(.78): [1000, 1000, 1000], q(.8): [1, 1, 1]}),
                                rotation=keys({0: [0, 0, 0], q(.4): [0, 0, 0], q(.78): [0, 0, 1440]})),
              "body": track(position=keys({0: [0, 0, 0], q(.3): [0, 1 * s, 2 * s], q(.55): [0, 0, -7 * s], q(.8): [0, 0, -5 * s], St: [0, 0, 0]}),
                            rotation=keys({0: [0, 0, 0], q(.3): [-6, 0, 0], q(.55): [12, 0, 0], St: [0, 0, 0]}))}
        m.move_name, m.move_extras = "DRILL PECK", {"all": away("fx_drill"), "special": sp}
    attack_move(m, "PECK", 1.0 + 0.15 * stage, s, "bird")
    A, info = bird(name.lower(), m, walk=0.5 + 0.1 * stage, stride=36 - 6 * stage, k=s, slow=1.0 + 0.15 * stage, flap=0.34 + 0.12 * stage)
    done(sid, name, m, A, info, stands)


def shriket():
    shrike("spearow", "SHRIKET", 0.66, 0)


def galeshrike():
    shrike("fearow", "GALESHRIKE", 2.7, 1)


# ---------- the gliders (Pichu, Pikachu, Raichu) ----------

def glider(sid, name, stands, stage):
    """A sugar glider: golden fur, a pale belly, huge dark eyes, big round ears, a gliding membrane along each side, and
    a bushy tail. Electricity runs in it: glowing pale-blue stripes down its back and along its membranes' edges, the
    tip of its tail alight, its whiskers fizzing. The baby round and big-eared; grown, sleeker, then (the last) deep
    amber, more stripes, long tufted ears, a long tail, a scowl."""
    m = Model()
    s = (1.0, 1.35, 1.75)[stage]
    S = S_(s)
    if stage < 2:
        FUR, FUR_L, FUR_D, BELLY = "#E3AE3C", "#F4CC6E", "#A87A22", "#FFF2D4"
    else:
        FUR, FUR_L, FUR_D, BELLY = "#D9852C", "#EEA650", "#9A5418", "#FFEBCB"
    GLOW, GLOW_L = "#7FE8FF", "#E8FCFF"

    def stripes(at):
        """Where it glows: a stripe down its spine (and, grown, one along each flank)."""
        if at.face == "top" and abs(at.x) < 0.55:
            return True
        if stage >= 1 and at.face in ("nx", "px") and abs(at.y - (5.2 * s + 0.6 * math.sin(at.z * 0.9))) < 0.45:
            return True
        return False

    def body_p(at):
        if stripes(at):
            return rgb(GLOW_L)
        return fur(FUR, FUR_L, FUR_D, BELLY, below=3.4 * s)(at)

    body = Mat(FUR, body_p, glow=lambda a: rgb(GLOW) if stripes(a) else None, noise=0.025)
    head = Mat(FUR, fur(FUR, FUR_L, FUR_D, BELLY, below=4.4 * s), noise=0.02)
    snout = Mat(BELLY, noise=0.02)
    nose = Mat("#5A3A2A", lambda a: rgb("#8A5A4A") if a.face == "top" else None, noise=0.01, edge=0)
    jaw = Mat(BELLY, jaw_teeth("#C0606A", "#FFFBF0", -8.4 * s), noise=0.02)
    ear = Mat(FUR, lambda a: (mix("#F2C6B4", "#E8A898", a.ly) if a.face == "front" and 0 < a.col < a.fw - 1 and 0 < a.row < a.fh - 1 else
                              (rgb(FUR_D) if a.row == 0 and stage == 2 else None)), noise=0.02)
    toe = Mat("#F6E6CC", noise=0.01, edge=0)
    legm = Mat(FUR, lambda a: rgb(BELLY) if a.ly < 0.25 and a.face != "top" else mix(FUR_D, FUR, a.ly), noise=0.025)

    def mem_p(at):
        if at.row == at.fh - 1:
            return rgb(GLOW_L)
        return mix(FUR, FUR_D, 0.3 + 0.4 * at.row / max(1, at.fh - 1))

    membrane = Mat(FUR, mem_p, glow=lambda a: rgb(GLOW) if a.row == a.fh - 1 else None, noise=0.02, edge=0)
    whisk = Mat("#FFF6C0", lambda a: None if (a.col + a.row) % 2 == 0 else CLEAR, glow=lambda a: rgb("#FFF27A") if (a.col + a.row) % 2 == 0 else None,
                noise=0.0, edge=0)

    big = 1.15 if stage == 0 else 1.0  # the baby's head is big for its body
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 4, 0]), blob(S([-2.5, 2.5, -3.5]), S([5, 4, 7]), body, b=1))
    hx, hh, hd = 2.75 * big, 4.5 * big, 4 * big
    m.bone("head", "body", S([0, 5.5, -3.5]), face_blob(S([-hx, 3.6, -7.5]), S([2 * hx, hh, hd]), head) +
           [Cube(S([-1.25, 3.6, -8.6]), [n(2.5 * s), n(1.6 * s), n(1.1 * s)], snout), Cube([-0.5 * n(1.2 * s), 4.6 * s + 0.4, -8.6 * s - 0.5], [n(1.2 * s), 1, 1], nose)])
    m.bone("jaw", "head", S([0, 3.6, -6]), [Cube(S([-1, 3.1, -8.4]), [n(2 * s), 1, n(3 * s)], jaw)])
    ew, eh = (3, 4) if stage == 0 else (3, 4) if stage == 1 else (2.5, 5.5)
    pointed = stage == 2

    def ear_front(at):
        if at.face != "front":
            return CLEAR
        inside, hw = ear_shape(at.lx, at.ly, pointed)
        if not inside:
            return CLEAR
        if abs(at.lx - 0.5) < hw - 0.17 and 0.12 < at.ly < 0.86:
            return mix("#F6CDBE", "#E8A090", at.ly)
        if pointed and at.ly > 0.82:
            return rgb(FUR_D)
        return mix(FUR, FUR_L, at.ly * 0.5)

    def ear_back(at):
        if at.face != "back":
            return CLEAR
        inside, hw = ear_shape(at.lx, at.ly, pointed)
        if not inside:
            return CLEAR
        return rgb(FUR_D) if pointed and at.ly > 0.82 else mix(FUR_D, FUR, 0.4 + 0.6 * at.ly)

    ewn, ehn = n(ew * s * 1.2), n(eh * s * 1.15)
    ex, ey, ez = -hx * 0.7 * s - ewn * 0.5, (3.6 + hh) * s - 1, -5.8 * s
    m.pair("ear_right", "head", S([-hx * 0.7, 3.6 + hh, -5.5]),
           [Cube([ex, ey, ez], [ewn, ehn, 0], Mat(FUR, ear_front, noise=0.015, edge=0)),
            Cube([ex, ey, ez + 0.06], [ewn, ehn, 0], Mat(FUR, ear_back, noise=0.015, edge=0))],
           rotation=[-8, -15, -25 if stage < 2 else -12])
    if stage < 2:
        m.eyes("head", x_inner=0.45 * s, y=5.4 * s * (1.04 if stage == 0 else 1.0), z=-7.5 * s, w=2 if stage == 1 else 3, h=3, iris="#2A1A12",
               pupil="#0A0604", skin=FUR, iris_light="#6A4A30", grid=["LL", "PW", "PP"] if stage == 1 else ["SLS", "PPW", "SPS"])
    else:
        m.eyes("head", x_inner=0.5 * s, y=5.6 * s, z=-7.5 * s, w=3, h=2, iris="#2A1A12", pupil="#0A0604", skin=FUR, iris_light="#7A5030",
               grid=["DLL", "PPW"])
        m.brows("head", [Cube([-0.5 * s - 3.4, 5.6 * s + 1.7, -7.5 * s - 0.5], [4, 1, 1], Mat(FUR_D, noise=0.02, edge=0), [0, 0, 16],
                              [-0.5 * s, 5.6 * s + 2.2, -7.5 * s])])
    m.pair("whiskers_right", "head", S([-1.25, 4.4, -8.4]), [Cube(S([-4.2, 4.4, -8.8]), [n(3 * s), 0, 2], whisk)], rotation=[0, -12, 0])
    # The gliding membranes, folded along each side between the legs.
    m.pair("membrane_right", "body", S([-2.5, 4, 0]), [Cube([-2.5 * s - 0.02, 2.2 * s, -3 * s], [0, n(2.2 * s), n(6 * s)], membrane)])
    tl = n((4 if stage == 0 else 7 if stage == 1 else 9) * s)
    tw = n((3.5 if stage == 0 else 4) * s)
    tailf = Mat(FUR, brush(FUR, FUR_L, FUR_D, tip=GLOW_L, tip_from=0.78, band=(0.45, 0.55) if stage == 2 else None),
                glow=lambda a: rgb(GLOW) if a.lz > 0.78 else None, noise=0.0, edge=0)
    m.bone("tail", "body", S([0, 4.5, 3.5]), [Cube(S([-1, 3.6, 3.3]), [n(2 * s), n(2 * s), n(2 * s)], body)], rotation=[-25 if stage < 2 else -15, 0, 0])
    m.bone("tail2", "tail", S([0, 4.5, 5]), [Cube([-tw / 2, 4.6 * s, 5 * s], [tw, 0, tl], tailf, [0, 0, a], [0, 4.6 * s, 5 * s]) for a in (0, 45, 90, 135)],
           rotation=[10, 0, 0])
    stout_legs(m, 1.8 * s, (-2.4 * s, 2.4 * s), round(3 * s), max(2, round(1.5 * s)), legm, toe, 3)

    # THUNDERSHOCK (THUNDERBOLT): sparks crackle round it as it charges, then a bolt leaps out in front.
    fx_for(m, s)
    bm = bolt_mat()
    St = 1.6
    q = lambda f: St * f
    sparks = []
    for i, (x, y, z, ry) in enumerate(((-3, 6, -2, 30), (3, 6, 1, -40), (0, 8, 2, 90))):
        nm = f"fx_spark{i + 1}"
        L = n(4 * s)
        m.bone(nm, "body", S([x, y, z]), [Cube([x * s - L / 2, y * s - L / 4, z * s], [L, n(L / 2), 0], bm, [0, ry, 0], S([x, y, z]), effect=True)])
        sparks.append(nm)
    # The aura: crackling bolts all round its body while it charges and fires (it glows with it).
    aura = []
    for i, ry in enumerate((0, 60, 120)):
        L = n(9 * s)
        aura.append(Cube([-L / 2, 2 * s, 0], [L, n(6 * s), 0], bm, [0, ry, 90 if i == 1 else 0], [0, 4.5 * s, 0], effect=True))
    m.bone("fx_aura", "body", [0, 4.5 * s, 0], aura)
    sparks.append("fx_aura")
    bl, bw = n((26 if stage < 2 else 36) * s), n((5 if stage < 2 else 7) * s)
    yb, zb = 5 * s, -9 * s
    m.bone("fx_bolt", "root", [0, yb, zb], [Cube([-bw / 2, yb, zb - bl], [bw, 0, bl], bm, effect=True),
                                            Cube([0, yb - bw / 2, zb - bl], [0, bw, bl], bm, effect=True)])
    flick = {0: [1, 1, 1]}
    for j, f in enumerate((.2, .26, .32, .38, .44, .5)):
        flick[q(f)] = [1000, 1000, 1000] if j % 2 == 0 else [600, 1200, 600]
    flick[q(.56)] = [1, 1, 1]
    sp = {nm: track(scale=keys(dict(flick))) for nm in sparks}
    sp["fx_bolt"] = track(scale=keys({0: [1, 1, 1], q(.48): [1, 1, 1], q(.5): [1000, 1000, 200], q(.56): [1000, 1000, 1000], q(.8): [1000, 1000, 1000],
                                      q(.84): [1, 1, 1]}))
    sp["tail2"] = track(rotation=keys({0: [0, 0, 0], q(.2): [-30, 0, 0], q(.8): [-30, 0, 0], St: [0, 0, 0]}))
    m.move_name = "THUNDERSHOCK" if stage < 2 else "THUNDERBOLT"
    m.move_extras = {"all": away(*sparks, "fx_bolt"), "special": sp}
    attack_move(m, "QUICK ATTACK", 1.0, s)
    A, info = four_legs(name.lower(), m, walk=0.45 + 0.05 * stage, stride=34, k=s, slow=1.0, idle="battle",
                        extras={"idle": {"ear_right": track(rotation=[0, 0, wave(3, 4)]), "ear_left": track(rotation=[0, 0, wave(3, -4)])},
                                "cry": {k: track(scale=keys({0: [1, 1, 1], 0.2: [1000, 1000, 1000], 0.3: [700, 1200, 700], 0.4: [1000, 1000, 1000],
                                                             0.5: [1, 1, 1]})) for k in sparks}})
    done(sid, name, m, A, info, stands)


def zipsy():
    glider("pichu", "ZIPSY", 0.6, 0)


def zapglider():
    glider("pikachu", "ZAPGLIDER", 0.78, 1)


def stormglider():
    glider("raichu", "STORMGLIDER", 1.6, 2)


ALL2 = (quillette, quillenne, quilldame, quillbuck, quillhorn, quilltusk, scrapkit, ragemong, shriket, galeshrike, zipsy, zapglider, stormglider)


def main():
    only = set(sys.argv[1:])
    for f in ALL2:
        if not only or f.__name__ in only:
            f()
    names = {}
    for sid in sorted(os.listdir(OUT_MODELS)):
        p = os.path.join(OUT_MODELS, sid, "info.json")
        if os.path.exists(p):
            names[sid] = json.load(open(p))["name"]
    json.dump(names, open(os.path.join(OUT_MODELS, "names.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
