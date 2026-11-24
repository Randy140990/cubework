"""
Retromon's own creatures, part five: Johto's (John, 2026-11-24 12:00: "build out our Retromon please for the additional
Johto"). Built the same way as Kanto's (cubes painted in 3D, our own designs drawn from real animals, plants and things,
the same species underneath), many of them on a few shared bodies here (a four-legged one, a bird, a swimmer, an
insect, a floater, an upright one) given each its own shape, coat, parts and colours.

Run: python3 tools/mons5.py [names]   (all if none; refreshes mons/names.json either way)
"""

import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, blob, cell, face_blob, hash01, mix, rgb, track, wave, keys, write  # noqa: E402
from monanims import four_legs, bird, floater, insect, biped, serpent, flyer  # noqa: E402
from mons import fx_for, open_mouth, shag_mat, stout_legs, folded_wing, quail_feet, whisker_mat, horn_mat, leaf_mat, broad_leaf  # noqa: E402
from mons2 import fur, brush, n  # noqa: E402
from mons3 import moves, bone_of, tail_bones, ear_mat, spike, taper_mat, scale_mat, petal_mat  # noqa: E402
from monlib import flame  # noqa: E402


def S_(s):
    return lambda v: [x * s for x in v]


def first(c, then, at):
    """A painter's colour if it has one, or what the next one gives."""
    return c if c is not None else then(at)


def solid(c, noise=0.02, edge=0.07):
    return Mat(c, noise=noise, edge=edge)


def plain(c):
    return Mat(c, noise=0.0, edge=0)


# ======================================================================================================
# A four-legged body, shaped and dressed per species
# ======================================================================================================

def quad(sid, name, stands, o):
    """A four-legged creature from its description o (a dict): its size (s), its body (w, h, l), how high it stands
    (top), its coat (FUR, FUR_L, FUR_D, BELLY, and a painter `coat` for its markings), its head (hw, hh, hl, snout),
    its ears, eyes and tail, and `extra` (a function given the model and its parts to add what's its own: antlers,
    flames, leaves, a fin)."""
    m = Model()
    s = o.get("s", 1.0)
    S = S_(s)
    FUR, FUR_L, FUR_D = o["FUR"], o.get("FUR_L", o["FUR"]), o.get("FUR_D", o["FUR"])
    BELLY = o.get("BELLY", FUR_L)
    bw, bh, bl = o.get("body", (6, 5, 10))
    top = o.get("top", 5)
    below = o.get("below", top + 1.6)
    base = fur(FUR, FUR_L, FUR_D, belly=BELLY, below=below * s, speck=o.get("speck", 0.35))
    coat = o.get("coat")
    furm = Mat(FUR, (lambda a: first(coat(a, base), base, a)) if coat else base, glow=o.get("glow"), noise=0.02)
    hw, hh, hl = o.get("head", (5, 4.4, 4.6))
    snout = o.get("snout", 1.6)
    FACE = o.get("FACE", BELLY)
    face_c = o.get("face_coat")
    face = Mat(FUR, (lambda a: first(face_c(a), lambda b: rgb(FACE) if (b.face == "front" and b.ly < 0.45) or b.face == "bottom" else None, a)) if face_c else
               (lambda a: rgb(FACE) if (a.face == "front" and a.ly < 0.45) or a.face == "bottom" else None), noise=0.02)
    NOSE = o.get("NOSE", "#3A2A22")
    muzzle = Mat(o.get("MUZZLE", FACE), lambda a: rgb(NOSE) if a.face == "front" and a.ly > 0.55 and abs(a.x) < 0.8 * s else None, noise=0.02)
    jaw_m = Mat(o.get("MUZZLE", FACE), open_mouth(o.get("MOUTH", "#A83A3A"), FACE), noise=0.02)
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, top + bh / 2, 0]), blob(S([-bw / 2, top, -bl / 2]), S([bw, bh, bl]), furm))
    hy = top + bh - 1 + o.get("neck_up", 0)
    nz = -bl / 2 + 0.5
    nl = o.get("neck", 3.5)
    ny0 = min(hy - 1.6, top + bh - 2.5)
    m.bone("neck", "body", S([0, hy, nz]), blob(S([-hw * 0.4, ny0, nz - nl]), S([hw * 0.8, hy + 2 - ny0, nl + 0.6]), furm),
           rotation=[o.get("neck_tilt", -10), 0, 0])
    hz = nz - nl + 0.5
    m.bone("head", "neck", S([0, hy + 1, hz]), face_blob(S([-hw / 2, hy, hz - hl]), S([hw, hh, hl]), face) +
           ([Cube(S([-1.4 * o.get("snout_w", 1), hy, hz - hl - snout]), [n(2.8 * o.get("snout_w", 1) * s), n(o.get("snout_h", 2.2) * s), n(snout * s)], muzzle)]
            if snout > 0 else []), rotation=[o.get("head_tilt", 10), 0, 0])
    m.bone("jaw", "head", S([0, hy + 0.4, hz - hl + 0.5]), [Cube(S([-1.2 * o.get("snout_w", 1), hy - 0.6, hz - hl - snout + 0.2]),
                                                               [n(2.4 * o.get("snout_w", 1) * s), 1, n((snout + 0.8) * s)], jaw_m)])
    ey = hy + hh * o.get("eye_y", 0.52)
    m.eyes("head", x_inner=o.get("eye_in", 0.8) * s, y=ey * s, z=(hz - hl) * s, w=o.get("eye_w", 2), h=o.get("eye_h", 2), iris=o.get("IRIS", "#3A2416"),
           pupil=o.get("PUPIL", "#0A0604"), skin=FACE if o.get("eye_on_face", True) else FUR, iris_light=o.get("IRIS_L"), grid=o.get("eye_grid", ["IW", "PI"]))
    ears = o.get("ears", ("round", 2.6, 2.6))
    if ears:
        shape, ew, eh = ears
        ear = ear_mat(o.get("EAR", FUR), o.get("EAR_IN", "#E8B8A8"), shape, tip=o.get("EAR_TIP"))
        m.pair("ear_right", "head", S([-hw * 0.32, hy + hh - 0.2, hz - hl * 0.45]),
               [Cube(S([-hw * 0.32 - ew, hy + hh - 0.6, hz - hl * 0.45]), [n(ew * s), n(eh * s), 0], ear)], rotation=o.get("ear_rot", [-6, 10, -18]))
    claw = Mat(o.get("CLAW", "#2A2018"), noise=0.0, edge=0)
    legm = o.get("legm") or Mat(FUR, fur(o.get("LEG", FUR), FUR_L, FUR_D, speck=0.2), noise=0.02)
    stout_legs(m, o.get("leg_x", bw / 2 - 0.4) * s, (-bl / 2 * 0.62 * s, bl / 2 * 0.62 * s), round(top * s), round(o.get("leg_w", 2) * s), legm, claw,
               o.get("toes", 2), sharp=o.get("sharp", False), foot=o.get("foot"))
    t = o.get("tail")
    names = []
    if t:
        segs, rests = t["segs"], t["rests"]
        tm = t.get("mat") or furm
        names, zt = tail_bones(m, "body", (top + bh * t.get("y", 0.7)) * s, bl / 2 * s - 0.5, [tuple(v * s for v in seg) for seg in segs], tm, rests)
        if t.get("tip"):
            tb = bone_of(m, names[-1])
            w0 = segs[-1][0] * s
            tb.cubes.append(Cube([-w0 / 2 - 0.2, tb.pivot[1] - w0 / 2 - 0.2, zt - 0.5], [n(w0 + 0.4), n(w0 + 0.4), n(1.6 * s)], Mat(t["tip"], noise=0.02)))
    parts = {"m": m, "s": s, "S": S, "furm": furm, "face": face, "hy": hy, "hz": hz, "hl": hl, "hw": hw, "hh": hh, "top": top, "bw": bw, "bh": bh,
             "bl": bl, "tail": names, "snout": snout}
    if o.get("extra"):
        o["extra"](parts)
    fx_for(m, o.get("fx", 0.8) * s)
    moves(m, *o.get("moves", ("TACKLE", "TACKLE")))
    A, info = four_legs(name.lower(), m, walk=o.get("walk", 0.6), stride=o.get("stride", 28), k=o.get("k", 0.8) * s, slow=o.get("slow", 1.0),
                        extras=o.get("anim_extras"))
    write(sid, name, m, A, info, stands)


# ---------- little shared parts ----------

def antlers(p, mat, tines, spread=1.0, h=5.0, base_x=1.2, tip_mat=None, buds=None):
    """A pair of branching antlers on the head: a beam rising and leaning out, tines off it (buds: flowers at the tips)."""
    m, s, S, hy, hz, hl, hh = p["m"], p["s"], p["S"], p["hy"], p["hz"], p["hl"], p["hh"]
    y0, z0 = hy + hh - 0.4, hz - hl * 0.5
    cubes = [Cube(S([-base_x - 0.5, y0, z0 - 0.5]), [1, n(h * s), 1], mat, [-10, 0, 22 * spread], S([-base_x, y0, z0]))]
    for i in range(tines):
        ty = y0 + h * (0.35 + 0.55 * i / max(1, tines - 1))
        L = h * (0.55 - 0.1 * i)
        cubes.append(Cube(S([-base_x - 0.5 - 0.4 * i * spread, ty, z0 - 0.5]), [1, n(L * s), 1], tip_mat or mat, [-30 + 18 * i, 0, 55 * spread - 12 * i],
                          S([-base_x - 0.4 * i * spread, ty, z0])))
        if buds:
            cubes.append(Cube(S([-base_x - 1.2 - 0.4 * i * spread - L * 0.6 * spread, ty + L * 0.7, z0 - 1]), [n(2 * s), n(2 * s), n(2 * s)], buds))
    m.pair("antler_right", "head", S([-base_x, y0, z0]), cubes)


def back_row(p, mat, n_, h, w=1.6, lean=-25, y_off=0.0, z_from=0.75, z_to=-0.65, bone="body", grow=0.0, xs=(0,)):
    """A row of upright flat parts along the back (spines, leaves, flames, a mane): n_ of them from tail to head."""
    m, s, S = p["m"], p["s"], p["S"]
    b = bone_of(m, bone)
    yb = p["top"] + p["bh"] + y_off
    for i in range(n_):
        f = i / max(1, n_ - 1)
        z = p["bl"] / 2 * (z_from + (z_to - z_from) * f)
        hh = h * (1 + grow * math.sin(math.pi * f))
        for x in xs:
            b.cubes.append(Cube(S([x - w / 2, yb - 0.4, z]), [n(w * s), n(hh * s), 0], mat, [lean, 0, 0], S([x, yb - 0.4, z])))


def flame_row(p, n_, h, heat=1.0, bone="body", y_off=0.0, z_from=0.6, z_to=-0.6):
    """Flames standing along the back (Minecraft draws them animated: our flame cubes)."""
    m, s, S = p["m"], p["s"], p["S"]
    b = bone_of(m, bone)
    yb = p["top"] + p["bh"] + y_off
    for i in range(n_):
        f = i / max(1, n_ - 1)
        z = p["bl"] / 2 * (z_from + (z_to - z_from) * f)
        hh = h * (0.7 + 0.5 * math.sin(math.pi * f))
        b.cubes += flame(S([-1.2, yb - 0.6, z - 1.2]), n(2.4 * s), n(hh * s), heat=heat)


def spot_coat(colour, size=1.6, chance=0.16, faces=("top", "nx", "px", "back")):
    """Spots across the coat."""
    def f(at, base):
        if at.face in faces and hash01("spot", int(at.x / size), int(at.y / size), int(at.z / size)) < chance:
            return rgb(colour)
        return None
    return f


def stripe_coat(colour, every=2.5, width=0.9, along="z", top_only=False):
    def f(at, base):
        if at.face == "bottom": return None
        if top_only and at.face not in ("top", "nx", "px"): return None
        v = at.z if along == "z" else at.y
        if (v % every) < width and at.face != "front":
            return rgb(colour)
        return None
    return f


def both(*fs):
    def f(at, base):
        for g in fs:
            c = g(at, base)
            if c is not None: return c
        return None
    return f


# ======================================================================================================
# The mouse-deer: SPRIGLET > CLOVERBUCK > BLOOMSTAG (in place of Chikorita, Bayleef, Meganium)
# ======================================================================================================

def mouse_deer(sid, name, stands, stage):
    """A mouse-deer of the meadow (a chevrotain): a slender little deer on twig-thin legs, a moss-green coat dappled
    with cream, clover-leaf ears and a pale throat. Its buck grows a garland of leaves round its neck and twig antlers in
    bud; the great stag's antlers are in flower, pink blossom on every tine, its mane a mass of leaves."""
    s = (1.0, 1.4, 1.9)[stage]
    FUR, FUR_L, FUR_D = ("#7FAE52", "#A8D07A", "#4E7A30") if stage < 2 else ("#5E9A44", "#8CC266", "#36602A")
    leaf = leaf_mat("#8ED060", "#4E8A30", "#D8F0A8")
    blossom = Mat("#F2A0C0", lambda a: rgb("#FFE0EC") if a.face == "top" else (rgb("#F6D24A") if a.col == a.fw // 2 and a.row == a.fh // 2 else None),
                  noise=0.01, edge=0)
    twig = Mat("#8A6A44", lambda a: rgb("#B08A5A") if a.face == "top" else None, noise=0.02)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        # Clover ears: three round leaflets each (the ear and two more beside it).
        head = bone_of(m, "head")
        for side in (-1, 1):
            for k, (dx, dy) in enumerate(((0.6, 0.2), (-0.4, 1.0))):
                head.cubes.append(Cube(S([side * (p["hw"] * 0.32 + 1.6) + dx - 1.1, p["hy"] + p["hh"] + dy, p["hz"] - p["hl"] * 0.45]), [n(2.2 * s_), n(2.2 * s_), 0],
                                       leaf, [0, 0, side * (20 + 15 * k)], S([side * (p["hw"] * 0.32 + 1.6) + dx, p["hy"] + p["hh"] + dy, p["hz"] - p["hl"] * 0.45])))
        # Tiny tusks (a chevrotain's fangs).
        bone_of(m, "head").cubes += [Cube(S([x - 0.3, p["hy"] - 0.8, p["hz"] - p["hl"] - p["snout"] + 0.4]), [1, n(1.2 * s_), 1], plain("#F6F0DC")) for x in (-0.9, 0.9)]
        if stage >= 1:
            # A garland of leaves round its neck.
            nb = bone_of(m, "neck")
            for i in range(7):
                a = i / 7 * 2 * math.pi
                nb.cubes.append(Cube(S([math.cos(a) * 2.4 - 1, p["hy"] - 1.8, p["hz"] - 1.2 + math.sin(a) * 1.2]), [n(2 * s_), 0, n(2.4 * s_)], leaf,
                                     [0, -math.degrees(a), 30], S([math.cos(a) * 2.4, p["hy"] - 1.8, p["hz"] - 1.2 + math.sin(a) * 1.2])))
            antlers(p, twig, 2 + stage, spread=0.9, h=3 + stage * 2.2, buds=blossom if stage == 2 else Mat("#B6E08A", noise=0.01))
        if stage == 2:
            back_row(p, leaf, 6, 3.2, w=2.6, lean=-35, z_from=-0.2, z_to=-0.85, grow=0.3, xs=(-1.2, 1.2))

    quad(sid, name, stands, dict(
        s=s, body=(5, 4.6, 9), top=6 + stage, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#F4ECCC", FACE="#EAE2BE",
        coat=spot_coat("#E8F0C0", 1.4, 0.13, faces=("top", "nx", "px")), head=(4.4, 4, 4.4), snout=1.8, snout_w=0.85, neck=3.2 + stage * 0.8, neck_up=1 + stage,
        ears=("round", 2.4, 2.4), EAR="#8ED060", EAR_IN="#C8EAA0", IRIS="#6A3A1A", IRIS_L="#C08040", eye_grid=["IW", "PI"],
        leg_w=1.6, toes=2, foot=Mat("#3A2A1E", noise=0.01), tail={"segs": [(1.4, 1.4, 1.6)], "rests": [30], "tip": "#F4ECCC"},
        extra=extra, moves=(("TACKLE", "RAZOR LEAF"), ("RAZOR LEAF", "SYNTHESIS"), ("BODY SLAM", "SOLARBEAM"))[stage],
        walk=0.5 + 0.1 * stage, stride=30, k=0.7))


def spriglet():
    mouse_deer("chikorita", "SPRIGLET", 0.9, 0)


def cloverbuck():
    mouse_deer("bayleef", "CLOVERBUCK", 1.2, 1)


def bloomstag():
    mouse_deer("meganium", "BLOOMSTAG", 1.8, 2)


# ======================================================================================================
# The ember badgers: EMBERSETT > BLAZESETT > PYROBROCK (in place of Cyndaquil, Quilava, Typhlosion)
# ======================================================================================================

def badger(sid, name, stands, stage):
    """A badger of the hearth: a stocky grey body, black legs and belly, and down its face the badger's broad stripe,
    here glowing ember-orange; along its back, embers smoulder in its fur and catch as it grows, until the great boar
    wears a mane of fire from crown to tail and its eyes burn gold."""
    s = (1.0, 1.35, 1.8)[stage]
    GREY, GREY_L, GREY_D = ("#8E8A8A", "#B8B4B2", "#5E5A5C") if stage < 2 else ("#6E6A70", "#9A96A0", "#3E3A42")
    BLACK = "#2A2426"
    EMBER, EMBER_L = "#FF8A2A", "#FFD27A"

    def coat(at, base):
        if at.face == "bottom" or (at.face in ("nx", "px") and at.ly < 0.35):
            return rgb(BLACK)
        if stage >= 1 and at.face == "top" and hash01("ember", round(at.x * 2), round(at.z * 2)) < 0.12:
            return rgb(EMBER)
        return None

    def face_coat(at):
        if at.face in ("front", "top") and abs(at.x) < 0.9 * s:
            return rgb(EMBER_L if at.face == "front" else EMBER)
        if at.face in ("nx", "px") and at.ly > 0.3 and at.ly < 0.7:
            return rgb(BLACK)
        if at.face == "front":
            return rgb("#F2EEE8")
        return None

    def extra(p):
        if stage == 0:
            flame_row(p, 2, 2.4, heat=0.6, z_from=0.3, z_to=-0.2)
        elif stage == 1:
            flame_row(p, 4, 3.4, heat=0.85, z_from=0.6, z_to=-0.6)
        else:
            flame_row(p, 6, 4.4, heat=1.0, z_from=0.8, z_to=-0.8)
            flame_row(p, 2, 3.6, heat=1.0, bone="head", y_off=-p["bh"] + p["hh"] + 1, z_from=-0.9, z_to=-1.1)

    quad(sid, name, stands, dict(
        s=s, body=(7, 5, 10), top=3 + stage, FUR=GREY, FUR_L=GREY_L, FUR_D=GREY_D, BELLY=BLACK, FACE="#F2EEE8", coat=coat, face_coat=face_coat,
        head=(5, 4, 5), snout=2.2, snout_w=0.9, NOSE="#1A1414", MUZZLE="#F2EEE8", neck=2, ears=("round", 1.8, 1.6), EAR=BLACK, EAR_IN="#F2EEE8",
        IRIS="#2A1A10" if stage < 2 else "#F0B020", IRIS_L=None if stage < 2 else "#FFE080", legm=Mat(BLACK, fur(BLACK, "#4A4446", "#1A1416"), noise=0.02),
        leg_w=2.4, toes=3, sharp=True, tail={"segs": [(2.4, 2, 2.4)], "rests": [20]},
        extra=extra, moves=(("TACKLE", "EMBER"), ("QUICK ATTACK", "FLAME WHEEL"), ("SWIFT", "FLAMETHROWER"))[stage],
        walk=0.6, stride=26, k=0.9))


def embersett():
    badger("cyndaquil", "EMBERSETT", 0.6, 0)


def blazesett():
    badger("quilava", "BLAZESETT", 0.95, 1)


def pyrobrock():
    badger("typhlosion", "PYROBROCK", 1.7, 2)


# ======================================================================================================
# The river otters: RIPPLOT > SURGOTTER > MAELSTROT (in place of Totodile, Croconaw, Feraligatr)
# ======================================================================================================

def otter(sid, name, stands, stage):
    """A river otter: a long sleek body in wet blue-teal fur, a cream face and chest, whiskers, small round ears, webbed
    paws and a thick rudder of a tail ending in a fin. As it grows, a fin rises along its back like a breaking wave and
    its tail's fin spreads; the grown otter is deep sea-blue, its crest of white-capped fins running from brow to tail."""
    s = (1.0, 1.4, 1.95)[stage]
    B, B_L, B_D = ("#3E9AC0", "#7EC6E0", "#22607E") if stage < 2 else ("#2A6AA8", "#5E9ED6", "#163E6A")
    FOAM = "#F2FAFF"

    def coat(at, base):
        if at.face == "top" and abs(at.x) < 0.5 and stage >= 1:
            return rgb(B_D)
        return None

    fin = Mat(B_L, lambda a: rgb(FOAM) if a.ly > 0.78 else mix(B_D, B_L, a.ly), noise=0.01, edge=0, flat=True)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        tb = bone_of(m, p["tail"][-1])
        z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
        L = 3 + stage * 1.5
        tb.cubes.append(Cube([-L * s_ / 2, tb.pivot[1] - 0.2, z - 1], [n(L * s_), 0, n(3 * s_)], fin))
        if stage >= 1:
            back_row(p, fin, 3 + stage * 2, 2 + stage, w=0.1, lean=-35, z_from=0.7, z_to=-0.6, grow=0.4)
        if stage == 2:
            bone_of(m, "head").cubes.append(Cube(S([0, p["hy"] + p["hh"] - 0.5, p["hz"] - p["hl"] * 0.6]), [0, n(3 * s_), n(3.6 * s_)], fin, [-25, 0, 0],
                                                 S([0, p["hy"] + p["hh"], p["hz"] - p["hl"] * 0.6])))
        m.pair("whiskers_right", "head", S([-1.2, p["hy"] + 1, p["hz"] - p["hl"] - p["snout"]]),
               [Cube(S([-4.4, p["hy"] + 1, p["hz"] - p["hl"] - p["snout"] - 0.6]), [n(3.4 * s_), 0, n(1.6 * s_)], whisker_mat("#F8FCFF"))])

    quad(sid, name, stands, dict(
        s=s, body=(5, 4.6, 12), top=3 + stage, FUR=B, FUR_L=B_L, FUR_D=B_D, BELLY="#F4ECD8", FACE="#F4ECD8", coat=coat,
        head=(5, 4, 4.6), snout=1.8, NOSE="#1A2430", neck=2.6, ears=("round", 1.6, 1.6), EAR=B_D, EAR_IN="#C8E0EA",
        IRIS="#1A2A3A", eye_grid=["WP", "PP"], leg_w=2, toes=3, foot=Mat(B_D, noise=0.01),
        tail={"segs": [(3, 2.6, 3), (2.6, 2.2, 3), (2, 1.6, 2.6)], "rests": [10, -4, -6], "y": 0.5},
        extra=extra, moves=(("SCRATCH", "WATER GUN"), ("BITE", "WATER GUN"), ("SLASH", "HYDRO PUMP"))[stage],
        walk=0.55, stride=26, k=0.8))


def ripplot():
    otter("totodile", "RIPPLOT", 0.6, 0)


def surgotter():
    otter("croconaw", "SURGOTTER", 1.1, 1)


def maelstrot():
    otter("feraligatr", "MAELSTROT", 2.3, 2)


# ======================================================================================================
# The marmots: PEEKMOT > LONGMOT (in place of Sentret, Furret)
# ======================================================================================================

def marmot(sid, name, stands, big):
    """A marmot on lookout: a stout grizzled body, honey-brown with a frosted back, a dark mask across its eyes, a pale
    nose, little round ears and a short bushy tail. Grown, long and low, darker, with a pale saddle and a long plume of
    a tail it waves to warn the rest."""
    s = 1.0 if not big else 1.4
    FUR, FUR_L, FUR_D = ("#B88A56", "#E0BC88", "#7A5632") if not big else ("#8A6440", "#C49A6A", "#54381E")

    def coat(at, base):
        if at.face == "top" and hash01("frost", round(at.x * 2), round(at.z * 2)) < 0.3:
            return rgb("#F0E2C8")
        if big and at.face in ("nx", "px", "top") and abs(at.z) < 2.5 * s and at.ly > 0.5:
            return rgb("#E8D4B0")
        return None

    def face_coat(at):
        if at.face == "front" and 0.45 < at.ly < 0.72:
            return rgb("#3A2816")
        if at.face == "front" and at.ly <= 0.45:
            return rgb("#F2E6D0")
        return None

    tail_m = Mat(FUR, brush(FUR, FUR_L, FUR_D, tip="#3A2816"), noise=0.02, edge=0, flat=True)

    def extra(p):
        if big:
            tb = bone_of(p["m"], p["tail"][-1])
            z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
            tb.cubes += [Cube([0, tb.pivot[1] - 2 * p["s"], z - 1], [0, n(4 * p["s"]), n(7 * p["s"])], tail_m),
                         Cube([-2 * p["s"], tb.pivot[1], z - 1], [n(4 * p["s"]), 0, n(7 * p["s"])], tail_m)]

    quad(sid, name, stands, dict(
        s=s, body=(7, 6, 9) if not big else (5, 4.4, 15), top=3 if not big else 3, FUR=FUR, FUR_L=FUR_L, FUR_D=FUR_D, BELLY="#E8D2A8", coat=coat,
        face_coat=face_coat, FACE="#F2E6D0", head=(5.2, 4.4, 4.2), snout=1.2, NOSE="#2A1A10", neck=1.6, ears=("round", 1.6, 1.4), EAR=FUR_D,
        IRIS="#1A120A", eye_grid=["WP", "PP"], leg_w=2.2, toes=3, sharp=True,
        tail={"segs": [(2.4, 2.4, 2.2)] if not big else [(2.4, 2.2, 2.6), (2, 2, 2.6)], "rests": [30] if not big else [20, 0], "tip": None if big else "#3A2816"},
        extra=extra, moves=(("SCRATCH", "DEFENSE CURL"), ("FURY SWIPES", "QUICK ATTACK"))[1 if big else 0], walk=0.45, stride=24, k=0.9))


def peekmot():
    marmot("sentret", "PEEKMOT", 0.8, False)


def longmot():
    marmot("furret", "LONGMOT", 1.8, True)



# ======================================================================================================
# A bird's body, shaped and dressed per species
# ======================================================================================================

def avian(sid, name, stands, o):
    """A bird from its description o: size (s), body (w, h, l), how high it stands on its legs (leg), its plumage
    (B, B_L, B_D, BELLY and a painter `coat`), head (hw, hh, hl), beak (beak: length, BEAK colour, hooked), eyes, wings
    (WING, WING_D, wing length), tail (tail: length, TAIL), and extra (crests, streamers, glows)."""
    m = Model()
    s = o.get("s", 1.0)
    S = S_(s)
    B, B_L, B_D = o["B"], o.get("B_L", o["B"]), o.get("B_D", o["B"])
    BELLY = o.get("BELLY", B_L)
    bw, bh, bl = o.get("body", (6, 6, 8))
    leg = o.get("leg", 4)
    base = fur(B, B_L, B_D, belly=BELLY, below=(leg + bh * 0.4) * s, speck=0.3)
    coat = o.get("coat")
    plume = Mat(B, (lambda a: first(coat(a, base), base, a)) if coat else base, noise=0.02)
    hw, hh, hl = o.get("head", (5, 5, 5))
    FACE = o.get("FACE", B)
    face_c = o.get("face_coat")
    face = Mat(FACE, face_c if face_c else (lambda a: None), noise=0.02)
    beak_l = o.get("beak", 2.4)
    BEAK = o.get("BEAK", "#E8B040")
    beak = Mat(BEAK, lambda a: rgb(o.get("BEAK_D", "#A87820")) if a.face == "bottom" or a.lz < 0.2 else None, noise=0.01, edge=0)
    m.bone("root", None, [0, 0, 0])
    y0 = leg + bh / 2
    m.bone("body", "root", S([0, y0, 0]), blob(S([-bw / 2, leg, -bl / 2]), S([bw, bh, bl]), plume), rotation=[o.get("pitch", -8), 0, 0])
    hy = leg + bh - 1 + o.get("neck_up", 0.5)
    # (The neck runs from the body's top up to the head, however long it is.)
    m.bone("neck", "body", S([0, hy, -bl / 2 + 1]), [Cube(S([-hw * 0.35, min(hy - 1.4, leg + bh - 2.4), -bl / 2 - 0.4]),
                                                          [n(hw * 0.7 * s), n((hy + 1.2 - min(hy - 1.4, leg + bh - 2.4)) * s), n(2.4 * s)], plume)])
    hz = -bl / 2 - 0.6
    m.bone("head", "neck", S([0, hy + 1, hz]), face_blob(S([-hw / 2, hy, hz - hl]), S([hw, hh, hl]), face))
    by = hy + hh * o.get("beak_y", 0.32)
    bwid = o.get("beak_w", 1.6)
    m.bone("beak_upper", "head", S([0, by, hz - hl]), [Cube(S([-bwid / 2, by, hz - hl - beak_l]), [n(bwid * s), n(o.get("beak_h", 1.2) * s), n(beak_l * s)], beak,
                                                       [o.get("beak_tilt", 8), 0, 0], S([0, by, hz - hl]))] +
           ([Cube(S([-0.5, by - 0.6, hz - hl - beak_l - 0.2]), [1, 1, 1], beak)] if o.get("hooked") else []))
    m.bone("jaw", "head", S([0, by, hz - hl]), [Cube(S([-bwid / 2 + 0.2, by - 0.8, hz - hl - beak_l * 0.8]), [n((bwid - 0.4) * s), 1, n(beak_l * 0.8 * s)], beak)])
    m.bones[-1].name = "beak_lower"
    m.bone("jaw", "head", S([0, by, hz - hl]), [])
    ey = hy + hh * o.get("eye_y", 0.58)
    m.eyes("head", o.get("eye_in", hw / 2) * s, ey * s, (hz - hl + o.get("eye_back", 1.4)) * s, w=o.get("eye_w", 2), h=o.get("eye_h", 2), side=o.get("side_eyes", True),
           iris=o.get("IRIS", "#2A1A10"), pupil=o.get("PUPIL", "#080404"), skin=FACE, iris_light=o.get("IRIS_L"), grid=o.get("eye_grid", ["IP", "PI"]))
    WING, WING_D, WING_L = o.get("WING", B_D), o.get("WING_D", B_D), o.get("WING_L", B)
    cov = Mat(WING, lambda a: mix(WING_D, WING, 0.4 + 0.6 * a.ly), noise=0.02)
    feat = Mat(WING_L, lambda a: CLEAR if (a.lz > 0.86 and (a.col % 2 == 1)) else mix(WING_D, WING_L, a.ly), noise=0.02, edge=0, flat=True)
    wl = o.get("wing", bl * 0.9)
    if o.get("jointed"):
        # ((0.59.6, John's 13:18: "about wingspans") The great birds' wings in five joints, shoulder to fingertips, as
        # the quails' and Pyribex's: neat along the flank at rest, opening out huge to fly.)
        from mons import jointed_wing, feather_plane
        sec = Mat(WING, feather_plane(WING_D, WING, WING_L, bars=o.get("WING_BARS")), noise=0.015, edge=0)
        pri = Mat(WING_D, feather_plane(WING_D, WING, WING_L, edge=o.get("WING_EDGE", WING_L)), noise=0.015, edge=0)
        jointed_wing(m, bw / 2 * s, (leg + bh - 0.4) * s, -bl / 2 * 0.7 * s, n(bl * 0.8 * s), tuple(n(wl * f * s) for f in (0.2, 0.22, 0.2, 0.34)), sec, sec, pri,
                     arm=cov, thick=max(1, round(s)), under=o.get("WING_UNDER", "#EEE8DC"))
    else:
        folded_wing(m, bw / 2 * s, (leg + bh - 0.6) * s, -bl / 2 * 0.6 * s, n(wl * 0.7 * s), n(bh * 0.55 * s), 1,
                    [(0, n(wl * 0.5 * s), n(bh * 0.5 * s))], [(n(wl * 0.3 * s), n(wl * 0.6 * s), n(bh * 0.5 * s))], cov, feat, feat)
    tl = o.get("tail", 4)
    TAIL = o.get("TAIL", WING)
    tailm = Mat(TAIL, lambda a: CLEAR if a.lz > 0.9 and a.col % 2 else mix(o.get("TAIL_D", WING_D), TAIL, a.lz), noise=0.02, edge=0, flat=True)
    m.bone("tail", "body", S([0, leg + bh * 0.55, bl / 2 - 0.6]), [Cube(S([-o.get("tail_w", 3) / 2, leg + bh * 0.55, bl / 2 - 0.6]), [n(o.get("tail_w", 3) * s), 0, n(tl * s)], tailm,
                                                                      [o.get("tail_tilt", 18), 0, 0], S([0, leg + bh * 0.55, bl / 2 - 0.6]))])
    legm = Mat(o.get("LEG", "#C89A50"), noise=0.01, edge=0)
    quail_feet(m, bw * 0.22 * s, (leg + 0.6) * s, leg * s, legm, scale=s, trouser=plume if o.get("trousers") else None)
    p = {"m": m, "s": s, "S": S, "plume": plume, "face": face, "hy": hy, "hz": hz, "hl": hl, "hw": hw, "hh": hh, "leg": leg, "bw": bw, "bh": bh, "bl": bl}
    if o.get("extra"):
        o["extra"](p)
    fx_for(m, o.get("fx", 0.8) * s)
    moves(m, *o.get("moves", ("PECK", "GUST")))
    A, info = bird(name.lower(), m, walk=o.get("walk", 0.5), stride=o.get("stride", 36), k=o.get("k", 0.8) * s, flap=o.get("flap", 0.36), extras=o.get("anim_extras"))
    write(sid, name, m, A, info, stands)


# ======================================================================================================
# The nightjars: DUSKJAR > GLOAMJAR (in place of Hoothoot, Noctowl)
# ======================================================================================================

def nightjar(sid, name, stands, big):
    """A nightjar: plumage mottled bark-brown and grey like a dead leaf, a broad flat head with huge dark eyes, a tiny
    bill over a wide gape fringed with bristles, long pointed wings and tail. Grown, a moth-grey night hunter with
    moon-white patches on its wings and two long streamers trailing from its tail."""
    s = 1.0 if not big else 1.6
    B, B_L, B_D = ("#8A7458", "#B8A07C", "#54442E") if not big else ("#7A7468", "#A8A296", "#4A463E")

    def coat(at, base):
        k = hash01("bark", round(at.x * 2), round(at.y * 2), round(at.z * 2))
        if k < 0.18: return rgb("#2E2418")
        if k > 0.86: return rgb("#E8DCC0")
        return None

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        # Bristles round the gape.
        bone_of(m, "head").cubes += [Cube(S([x - 0.1, p["hy"] + 0.6, p["hz"] - p["hl"] - 0.6]), [0, n(1.4 * s_), n(1.6 * s_)], whisker_mat("#2A2016"), [0, 0, 0], None) for x in (-1.6, 1.6)]
        if big:
            wm = bone_of(m, "wing_mid_right")
            wm.cubes.append(Cube([wm.pivot[0] - 0.05, wm.pivot[1] - 3 * s_, wm.pivot[2] + 2 * s_], [0, n(2 * s_), n(2.4 * s_)], plain("#F4F0E6")))
            wl = bone_of(m, "wing_mid_left")
            wl.cubes.append(Cube([wl.pivot[0] + 0.05, wl.pivot[1] - 3 * s_, wl.pivot[2] + 2 * s_], [0, n(2 * s_), n(2.4 * s_)], plain("#F4F0E6")))
            tb = bone_of(m, "tail")
            tb.cubes += [Cube(S([x - 0.5, p["leg"] + p["bh"] * 0.55, p["bl"] / 2 + 3]), [1, 0, n(9 * s_)], Mat("#4A463E", noise=0.01, edge=0), [14, 0, 0],
                              S([x, p["leg"] + p["bh"] * 0.55, p["bl"] / 2])) for x in (-0.8, 0.8)]

    avian(sid, name, stands, dict(
        s=s, body=(6, 5, 8), leg=2.4, B=B, B_L=B_L, B_D=B_D, BELLY="#C8B494", coat=coat, head=(6, 4.2, 4.4), FACE=B, beak=0.8, beak_w=1.4, BEAK="#4A3A2A",
        BEAK_D="#2A2018", eye_w=2, eye_h=3, IRIS="#1A1210", side_eyes=False, eye_in=0.8, eye_back=0, eye_grid=["IPP", "IPP", "III"][:3] if False else ["IP", "PP", "PI"],
        wing=10, WING=B, WING_D=B_D, WING_L=B_L, tail=5, TAIL=B, tail_w=3, LEG="#8A6A4A", extra=extra,
        moves=(("PECK", "HYPNOSIS") if not big else ("TAKE DOWN", "HYPNOSIS")), walk=0.5, k=0.7))


def duskjar():
    nightjar("hoothoot", "DUSKJAR", 0.7, False)


def gloamjar():
    nightjar("noctowl", "GLOAMJAR", 1.6, True)


# ======================================================================================================
# An insect's body
# ======================================================================================================

def bug(sid, name, stands, o):
    """An insect from its description o: a thorax and head, an abdomen in segments, six legs, wings (none, wing cases,
    or flying wings) and its own extra (horns, lanterns, a net)."""
    from mons3 import legs_pair, wing_paint, compound_grid
    m = Model()
    s = o.get("s", 1.0)
    S = S_(s)
    B, B_D, B_L = o["B"], o.get("B_D", o["B"]), o.get("B_L", o["B"])
    shell = Mat(B, o.get("paint") or (lambda a: mix(B_D, B_L, 0.3 + 0.6 * a.ly)), noise=0.02)
    belly = Mat(o.get("BELLY", B_D), noise=0.02)
    y = o.get("y", 6)
    tw, th, tl = o.get("thorax", (5, 4, 5))
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, y, 0]), blob(S([-tw / 2, y - th / 2, -tl / 2]), S([tw, th, tl]), shell))
    hw, hh, hl = o.get("head", (4, 3.4, 3))
    HEAD = o.get("HEAD", B_D)
    headm = Mat(HEAD, o.get("head_paint") or (lambda a: None), noise=0.02)
    m.bone("head", "body", S([0, y, -tl / 2]), blob(S([-hw / 2, y - hh / 2, -tl / 2 - hl]), S([hw, hh, hl]), headm))
    m.bone("jaw", "head", S([0, y - hh / 2 + 0.5, -tl / 2 - hl]), [Cube(S([-0.8, y - hh / 2, -tl / 2 - hl - 0.6]), [n(1.6 * s), 1, 1], plain(o.get("JAW", "#2A2020")))])
    m.eyes("head", hw / 2 * s, (y - hh * 0.1) * s, (-tl / 2 - hl) * s, h=o.get("eye_h", 2), corner=(1, 1), half=hw / 2 * s, iris=o.get("IRIS", "#1A1A1A"),
           pupil=o.get("PUPIL", "#000000"), skin=HEAD, iris_light=o.get("IRIS_L", "#6A6A6A"), grid=compound_grid(2, o.get("eye_h", 2)))
    aw, ah = o.get("abdomen", (4.6, 4))
    segs = o.get("segs", 3)
    am = Mat(o.get("ABD", B), o.get("abd_paint") or (lambda a: None), glow=o.get("abd_glow"), noise=0.02)
    prev, z = "body", tl / 2 - 0.4
    for i in range(segs):
        nm = "abdomen" if i == 0 else f"abdomen{i + 1}"
        w = aw * (1 - 0.18 * i)
        L = o.get("seg_l", 3.2)
        m.bone(nm, prev, S([0, y, z]), [Cube(S([-w / 2, y - w * 0.45, z - 0.2]), [n(w * s), n(w * 0.9 * s), n(L * s)], am)], rotation=[o.get("abd_tilt", -6), 0, 0])
        prev, z = nm, z + L
    legm = Mat(o.get("LEGC", B_D), noise=0.01, edge=0)
    claw = plain("#1A1414")
    names = []
    for i, z_ in enumerate((-tl / 2 + 1, 0, tl / 2 - 1)):
        legs_pair(m, f"leg_{i + 1}_right", "body", tw / 2 * s, (y - th / 2 + 0.6) * s, z_ * s, o.get("leg_len", 4) * s, legm, claw, ang=(-14, 0, 14)[i])
        names += [f"leg_{i + 1}_right", f"leg_{i + 1}_left"]
    wing = o.get("wings")
    if wing:
        fore = wing_paint(o.get("WING", "#E8F0F8"), o.get("VEIN", "#5A6A7A"), o.get("WING_EDGE", "#3A4A5A"), 2, wing, tint=o.get("WING_TINT"))
        m.pair("wing_right", "body", S([-tw / 2, y + th / 2 - 0.4, -tl / 2 + 1]),
               [Cube(S([-tw / 2 - wing, y + th / 2 - 0.4, -tl / 2]), [n(wing * s), 0, n(wing * 0.45 * s)], fore, [0, -10, 0], S([-tw / 2, y + th / 2 - 0.4, -tl / 2 + 1]))],
               rotation=[0, 0, o.get("wing_rest", 30)])
        if o.get("hind", True):
            hind = wing_paint(o.get("WING", "#E8F0F8"), o.get("VEIN", "#5A6A7A"), o.get("WING_EDGE", "#3A4A5A"), 2, wing * 0.85, hind=True, tint=o.get("WING_TINT"))
            m.pair("hindwing_right", "body", S([-tw / 2, y + th / 2 - 0.8, tl / 2 - 1]),
                   [Cube(S([-tw / 2 - wing * 0.85, y + th / 2 - 0.8, tl / 2 - 1.5]), [n(wing * 0.85 * s), 0, n(wing * 0.4 * s)], hind, [0, 14, 0], S([-tw / 2, y + th / 2 - 0.8, tl / 2 - 1]))],
                   rotation=[0, 0, o.get("wing_rest", 30) - 10])
    if o.get("elytra"):
        EL = o["elytra"]
        el = Mat(EL[0], EL[1] if len(EL) > 1 else (lambda a: None), noise=0.02)
        m.pair("case_right", "body", S([-0.1, y + th / 2, -tl / 2 + 0.5]), [Cube(S([-tw / 2 - 0.3, y + th / 2 - 1.2, -tl / 2 + 0.5]), [n((tw / 2 + 0.2) * s), n(1.4 * s),
                                                                                                                                   n((tl + aw * 1.6) * s)], el)])
    p = {"m": m, "s": s, "S": S, "shell": shell, "y": y, "tw": tw, "th": th, "tl": tl, "hw": hw, "hh": hh, "hl": hl, "headm": headm}
    if o.get("extra"):
        o["extra"](p)
    fx_for(m, o.get("fx", 0.8) * s)
    moves(m, *o.get("moves", ("TACKLE", "STRING SHOT")))
    A, info = insect(name.lower(), m, flap=o.get("flap", 0.2), amp=o.get("amp", 40), k=o.get("k", 0.8) * s, legs=tuple(names), extras=o.get("anim_extras"))
    write(sid, name, m, A, info, stands)


# ======================================================================================================
# The fireflies: GLIMMET > LANTERNWING (in place of Ledyba, Ledian)
# ======================================================================================================

def firefly(sid, name, stands, big):
    """A firefly: a soft dark beetle, its shield orange with a black heart, wing cases edged in cream, and the last
    segments of its body a lantern that glows yellow-green. Grown, it flies upright on long wings, a lantern glowing at
    the tip of each of its antennae too."""
    s = 1.0 if not big else 1.5
    GLOW = "#E8FF7A"
    lantern = lambda a: rgb("#F4FFB0")
    glow = lambda a: rgb(GLOW)

    def head_paint(a):
        if a.face in ("top", "front") and a.ly > 0.4: return rgb("#F08A2A")
        return None

    def abd(a):
        return None

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        last = "abdomen3"
        b = bone_of(m, last)
        c = b.cubes[0]
        b.cubes.append(Cube([c.origin[0] - 0.2, c.origin[1] - 0.2, c.origin[2] + c.size[2] - 0.6], [c.size[0] + 0.4, c.size[1] + 0.4, n(2.4 * s_)],
                            Mat(GLOW, lantern, glow=glow, noise=0.0, edge=0)))
        ant = Mat("#2A2420", noise=0.0, edge=0)
        bulb = Mat(GLOW, lantern, glow=glow, noise=0.0, edge=0)
        hb = p["y"] + p["hh"] / 2
        z = -p["tl"] / 2 - p["hl"] + 0.5
        cubes = [Cube(S([-1.2, hb, z]), [1, n(4 * s_), 1], ant, [-20, 0, -18], S([-0.8, hb, z]))]
        if big:
            cubes.append(Cube(S([-2.8, hb + 3.4, z - 1.8]), [n(1.6 * s_), n(1.6 * s_), n(1.6 * s_)], bulb))
        m.pair("antenna_right", "head", S([-0.8, hb, z]), cubes)

    bug(sid, name, stands, dict(
        s=s, y=5 if not big else 8, B="#3A2A2E", B_D="#1E1418", B_L="#5A464A", HEAD="#2A2024", head_paint=head_paint, thorax=(5, 3.4, 4.4), head=(3.6, 3, 2.6),
        abdomen=(4.4, 3.8), segs=3, ABD="#4A3A3A", IRIS="#2A2A2A", IRIS_L="#7A7A7A", leg_len=3 if not big else 6, LEGC="#2A2024",
        elytra=("#2E2226", lambda a: rgb("#F2E6C8") if a.col in (0, a.fw - 1) else None), wings=9 if big else 6, WING="#E0E8F0", VEIN="#6A7488",
        wing_rest=40 if big else 20, extra=extra, moves=(("TACKLE", "SUPERSONIC") if not big else ("COMET PUNCH", "LIGHT SCREEN")), flap=0.16, amp=44))


def glimmet():
    firefly("ledyba", "GLIMMET", 0.8, False)


def lanternwing():
    firefly("ledian", "LANTERNWING", 1.3, True)


# ======================================================================================================
# The net-casting spiders: GAZEWEB > SNARECAST (in place of Spinarak, Ariados)
# ======================================================================================================

def net_spider(sid, name, stands, big):
    """A net-casting spider: a slender body on eight long jointed legs, banded violet and cream, and two great dark
    eyes glinting at the front of its face (it hunts by night). Grown, deep plum and gold, it carries its square net
    stretched between its front legs, ready to throw."""
    s = 1.0 if not big else 1.6
    B, B_L, B_D = ("#8A6A9A", "#C0A8CC", "#4E3A5A") if not big else ("#6A2A4A", "#C89A3A", "#3A1428")

    def abd(a):
        if a.face != "bottom" and int(a.z * 1.1) % 2 == 0: return rgb(B_L)
        return None

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        legm = Mat(B_D, lambda a: rgb(B_L) if a.row % 3 == 0 else None, noise=0.01, edge=0)
        # Its other two pairs of legs, long and bent.
        for k, (z, a) in enumerate(((-1.6, -38), (1.6, 38))):
            m.pair(f"leg_x{k + 1}_right", "body", S([-p["tw"] / 2, p["y"], z]), [Cube(S([-p["tw"] / 2 - 6, p["y"] - 0.5, z - 0.5]), [n(6 * s_), 1, 1], legm),
                                                                           Cube(S([-p["tw"] / 2 - 6.5, p["y"] - 5, z - 0.5]), [1, n(5 * s_), 1], legm)],
                   rotation=[0, a, 0])
        if big:
            net = Mat("#F4F0FF", lambda a: None if (a.col % 3 == 0 or a.row % 3 == 0) else CLEAR, noise=0.0, edge=0, flat=True)
            bone_of(m, "head").cubes.append(Cube(S([-4, p["y"] - 1, -p["tl"] / 2 - p["hl"] - 2]), [n(8 * s_), n(7 * s_), 0], net))

    bug(sid, name, stands, dict(
        s=s, y=5, B=B, B_D=B_D, B_L=B_L, HEAD=B, thorax=(4.4, 3, 4), head=(3.8, 3, 2.4), abdomen=(5.6, 5), segs=2, seg_l=4, ABD=B, abd_paint=abd,
        IRIS="#0A0A10", IRIS_L="#5A5A7A", eye_h=3, leg_len=5, LEGC=B_D, extra=extra,
        moves=(("POISON STING", "STRING SHOT") if not big else ("NIGHT SHADE", "SPIDER WEB")), flap=0.3, amp=4))


def gazeweb():
    net_spider("spinarak", "GAZEWEB", 0.6, False)


def snarecast():
    net_spider("ariados", "SNARECAST", 1.2, True)


# ======================================================================================================
# The storm alpacas: ZAPPACA > VOLTICUNA > TEMPESTLAMA (in place of Mareep, Flaaffy, Ampharos)
# ======================================================================================================

def alpaca(sid, name, stands, stage):
    """An alpaca whose fleece holds a storm: a woolly body, a long neck, a sweet flat face with long lashes, banana
    ears, and wool that crackles pale gold with static. The young one is all fleece; the next is shorn to its blue-grey
    skin but for a crackling ruff and leggings; the grown one is a tall llama, its mane a thundercloud, a glowing bead
    of lightning at the end of its tail."""
    s = (1.0, 1.35, 1.75)[stage]
    WOOL, WOOL_L, WOOL_D = "#F4E8B8", "#FFF8DC", "#C8B478"
    SKIN, SKIN_L, SKIN_D = ("#F4E8B8", "#FFF8DC", "#C8B478") if stage == 0 else ("#8AA0B8", "#B4C6D8", "#5A6E86")
    SPARK = "#FFE85A"

    def coat(at, base):
        if hash01("crackle", round(at.x * 2), round(at.y * 2), round(at.z * 2)) < 0.05: return rgb(SPARK)
        return None

    wool = Mat(WOOL, lambda a: mix(WOOL_D, WOOL_L, 0.4 + 0.6 * cell(a.x, a.y, a.z, 1.0)), glow=lambda a: rgb(SPARK) if hash01("sp", round(a.x), round(a.y), round(a.z)) < 0.06 else None,
               noise=0.02)
    cloud = Mat("#7A7E92", lambda a: mix("#4A4E62", "#B8BCCC", cell(a.x, a.y, a.z, 1.2)), glow=lambda a: rgb(SPARK) if hash01("bolt", round(a.x), round(a.y)) < 0.05 else None, noise=0.02)

    def extra(p):
        m, S, s_ = p["m"], p["S"], p["s"]
        if stage >= 1:
            # The ruff of fleece round its neck and puffs on its legs.
            nb = bone_of(m, "neck")
            nb.cubes += blob(S([-2.8, p["hy"] - 2.6, p["hz"] - 1.4]), S([5.6, 4.4, 4.4]), wool if stage == 1 else cloud)
            for leg in ("leg_front_right", "leg_front_left", "leg_back_right", "leg_back_left"):
                b = bone_of(m, leg)
                c = b.cubes[0]
                b.cubes.append(Cube([c.origin[0] - 0.6, c.origin[1] + c.size[1] * 0.4, c.origin[2] - 0.6], [c.size[0] + 1.2, n(2.4 * s_), c.size[2] + 1.2], wool))
        if stage == 2 and p["tail"]:
            tb = bone_of(m, p["tail"][-1])
            z = tb.cubes[0].origin[2] + tb.cubes[0].size[2]
            tb.cubes.append(Cube([-1.2 * s_, tb.pivot[1] - 1.2 * s_, z - 0.4], [n(2.4 * s_), n(2.4 * s_), n(2.4 * s_)],
                                 Mat("#FFF4A0", glow=lambda a: rgb(SPARK), noise=0.0, edge=0)))
        if stage == 0:
            bone_of(m, "body").cubes += blob(S([-3.6, p["top"] - 0.4, -p["bl"] / 2 - 0.4]), S([7.2, p["bh"] + 1.6, p["bl"] + 0.8]), wool)

    quad(sid, name, stands, dict(
        s=s, body=(6, 5, 8), top=5 + stage, FUR=SKIN, FUR_L=SKIN_L, FUR_D=SKIN_D, BELLY=SKIN_L, coat=coat if stage == 0 else None, FACE="#F6ECD0",
        head=(4, 4, 4), snout=1.8, snout_w=0.9, NOSE="#5A4A3A", neck=2.2 + stage * 0.6, neck_up=2 + stage * 2.4, neck_tilt=-8,
        ears=("rabbit", 1.4, 3.2), EAR=SKIN_D if stage else WOOL_D, EAR_IN="#F2C8B0", IRIS="#2A1A10", eye_grid=["LW", "PP"],
        leg_w=1.8, toes=2, foot=Mat("#4A3A2A", noise=0.01), tail={"segs": [(1.6, 1.6, 2)] if stage < 2 else [(1.6, 1.6, 2.4), (1.4, 1.4, 2.4)],
                                                                  "rests": [40] if stage < 2 else [10, -10]},
        extra=extra, moves=(("TACKLE", "THUNDERSHOCK"), ("THUNDERSHOCK", "COTTON SPORE"), ("THUNDERPUNCH", "THUNDER"))[stage], walk=0.55, k=0.7))


def zappaca():
    alpaca("mareep", "ZAPPACA", 0.6, 0)


def volticuna():
    alpaca("flaaffy", "VOLTICUNA", 0.8, 1)


def tempestlama():
    alpaca("ampharos", "TEMPESTLAMA", 1.4, 2)


NAMES5 = {}


def main(names):
    for f in ALL5:
        if not names or f.__name__ in names:
            f()
    import mons4
    mons4.refresh_names()


ALL5 = (spriglet, cloverbuck, bloomstag, embersett, blazesett, pyrobrock, ripplot, surgotter, maelstrot, peekmot, longmot, duskjar, gloamjar,
        glimmet, lanternwing, gazeweb, snarecast, zappaca, volticuna, tempestlama)

if __name__ == "__main__":
    main(set(sys.argv[1:]))
