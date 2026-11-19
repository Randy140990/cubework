"""
Retromon's own creatures, made here from scratch (no one else's models, textures or animations in them). Each
stands in for one species when OPTION's POKéMON LOOK is set to RETROMON: the same species underneath (stats, moves,
levelling, evolution), with its own look and name.

  FERNADILLO > BRAMBADILLO > GROVADON      (Bulbasaur, Ivysaur, Venusaur)
  CINDERKID  > EMBERAM     > PYRIBEX       (Charmander, Charmeleon, Charizard)
  TIDAPIR    > SURFAPIR    > TSUNAPIR      (Squirtle, Wartortle, Blastoise)
  QUAILET    > PLUMQUAIL   > REGALQUAIL    (Pidgey, Pidgeotto, Pidgeot)
  JERBIT     > JERBOLT                     (Rattata, Raticate)

Writes, for each, assets/retromon/mons/<species>/ (model.geo.json, anim.json, info.json), its texture in
textures/mons/ (and the glowing parts' frames, if it has any), and mons/names.json.

Run: python3 tools/mons.py
"""

import json, math, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from monlib import (CLEAR, Cube, Mat, Model, OUT_MODELS, blob, cell, face_blob, flame, hash01, jet, merge, away, blade_mat, wind_mat, fang_mat, mix, ramp, rgb, track, wave, bounce, keys, pulse,  # noqa: E402
                    write)
from monanims import bird, four_legs, hopper, wings  # noqa: E402


# ---------- shared painters ----------

def frond(light, dark, rib, along="z", base_end=0, leaflets=5, width=1.0):
    """A fern frond on a flat part: a midrib, leaflets either side with gaps between them, narrowing to the tip.
    along: the axis it runs along; base_end 0 if its base is at the low end of that axis, 1 at the high end."""
    def f(at):
        c = at.cube
        if along == "z":
            t = at.lz if base_end == 0 else 1 - at.lz
            s = (at.lx - 0.5) * 2 if c.size[0] else (at.ly - 0.5) * 2
        elif along == "y":
            t = at.ly if base_end == 0 else 1 - at.ly
            s = (at.lx - 0.5) * 2 if c.size[0] else (at.lz - 0.5) * 2
        else:
            t = at.lx if base_end == 0 else 1 - at.lx
            s = (at.lz - 0.5) * 2 if c.size[2] else (at.ly - 0.5) * 2
        half = width * (1 - t * 0.75)
        if abs(s) > half + 0.05:
            return CLEAR
        if abs(s) < 0.18:
            return rgb(rib)
        if (t * leaflets) % 1.0 < 0.22 and abs(s) > 0.3:
            return CLEAR
        return mix(dark, light, 0.35 + 0.65 * abs(s))
    return f


def smile(dark, grin=True):
    """A mouth on a muzzle's face: a line along its bottom, the corners turned up (grin) or straight."""
    def f(at):
        if at.face != "front" or at.fh < 2:
            return None
        if at.row == at.fh - 1 and 0 < at.col < at.fw - 1:
            return rgb(dark)
        if grin and at.row == at.fh - 2 and at.col in (0, at.fw - 1):
            return rgb(dark)
        return None
    return f


def nostrils(dark, top=None):
    """Nostrils on a nose's face (its outer two texels on the top row), and a lit top."""
    def f(at):
        if at.face == "front" and at.row == 0 and at.col in (0, at.fw - 1):
            return rgb(dark)
        if top is not None and at.face == "top":
            return rgb(top)
        return None
    return f


def open_mouth(inside, under=None):
    """A jaw: the inside of the mouth on its top (seen when it opens), its underside a shade darker."""
    def f(at):
        if at.face == "top":
            return rgb(inside)
        if under is not None and at.face == "bottom":
            return rgb(under)
        return None
    return f


def muzzle_patch(colour, zf, top, half, edge=None):
    """A paler muzzle on a flat face (its front at z zf): below top, within half of the middle, fading at its rim."""
    def f(at):
        if at.face != "front" or at.z > zf + 0.02 or at.y > top or abs(at.x) > half:
            return None
        rim = abs(at.x) > half - 1 or at.y > top - 1
        return mix(colour, edge, 0.5) if rim and edge is not None else rgb(colour)
    return f


def tuck(length, down=24, back=2.0):
    """An armadillo pulling its head in under its shell while it's hit, then peeking back out."""
    L = length
    return {"head": track(rotation=keys({0: [0, 0, 0], L * 0.1: [down, 0, 0], L * 0.55: [down, 0, 0], L * 0.8: [0, 0, 0]}),
                          position=keys({0: [0, 0, 0], L * 0.1: [0, -back * 0.5, back], L * 0.55: [0, -back * 0.5, back], L * 0.8: [0, 0, 0]}))}


def goat_nose(nose, nostril, lip, front_z, top_y, mouth_y, nost_dx, tooth=None, tooth_z=None, line_to=None, top=None):
    """A goat's muzzle: a soft leather nose pad across its top with a nostril each side (seen from the front and from
    above), a split lip down the middle where there's a middle column, the mouth's line along the bottom and back along
    each side, and (tooth) a tooth showing at the side of the mouth. All by where the texel is (z front_z the muzzle's
    front, top_y its top, mouth_y its bottom), so it works across several boxes."""
    def at_nostril(x):
        return abs(abs(x) - nost_dx) < 0.3

    def f(at):
        if at.face == "front" and at.z <= front_z + 0.02:
            if at.y >= top_y - 1:
                return mix(nostril, nose, 0.15) if at_nostril(at.x) else rgb(nose)
            if at.y >= top_y - 2 and at_nostril(at.x):
                return mix(nose, nostril, 0.35)
            if at.y < mouth_y + 1:
                return rgb(lip) if 0.12 < at.lx < 0.88 else None
            if abs(at.x) < 0.3:
                return mix(nose, lip, 0.45)
            return None
        if at.face == "top" and at.z < front_z + 1.02:
            return mix(nostril, nose, 0.3) if at_nostril(at.x) else rgb(nose)
        if at.face == "top" and top is not None:
            return rgb(top)
        if at.face == "bottom":
            return mix(lip, "#C05060", 0.6)
        if at.face in ("nx", "px") and mouth_y <= at.y < mouth_y + 1:
            if tooth is not None and abs(at.z - tooth_z) < 0.5:
                return rgb(tooth)
            if at.z < (line_to if line_to is not None else front_z + 99):
                return rgb(lip)
        return None
    return f


def jaw_teeth(inside, teeth, front_z, under=None):
    """A lower jaw: its little front teeth along the front of its top, the inside of the mouth behind them (both seen
    when it opens), its underside a shade darker."""
    def f(at):
        if at.face == "top":
            return rgb(teeth) if at.z < front_z + 1.02 else rgb(inside)
        if under is not None and at.face == "bottom":
            return rgb(under)
        return None
    return f


def part_top(m, bone):
    b = next(x for x in m.bones if x.name == bone)
    return max(c.origin[1] + c.size[1] for c in b.cubes if not c.effect)


def fx_for(m, size=1.0):
    """Its stars-when-confused and critical-hit burst, placed from its head. Gives the head's top, front and mouth."""
    head = next(b for b in m.bones if b.name == "head")
    cs = [c for c in head.cubes if not c.effect and not c.rotation]
    top = max(c.origin[1] + c.size[1] for c in cs)
    front = min(c.origin[2] for c in cs)
    back = max(c.origin[2] + c.size[2] for c in cs)
    jaw = next((b for b in m.bones if b.name in ("jaw", "beak_lower")), None)
    mouth = jaw.pivot[1] if jaw else min(c.origin[1] for c in cs) + 1
    m.battle_fx("head", top, (front + back) / 2, mouth, front, size)
    return top, front, mouth


def fly_out(bone, t0, dur, to, spin=(0, 0, 0), grow=1.0):
    """A move's part flying: appearing at t0 (brought back from nothing), travelling to `to` over dur, spinning, gone."""
    g = 1000 * grow
    return {bone: track(scale=keys({0: [1, 1, 1], max(0.0, t0 - 0.02): [1, 1, 1], t0: [1000, 1000, 1000], t0 + dur * 0.9: [g, g, g],
                                    t0 + dur: [1, 1, 1]}),
                        position=keys({0: [0, 0, 0], t0: [0, 0, 0], t0 + dur: list(to)}),
                        rotation=keys({0: [0, 0, 0], t0: [0, 0, 0], t0 + dur: list(spin)}))}


def as_effect(cubes):
    for c in cubes:
        c.effect = True
    return cubes


def razor_leaf(m, S, size, light, dark, rib, shell="shell"):
    """RAZOR LEAF: three sharp leaves whirl up off its back and spin away like blades."""
    y, bw, bl = part_top(m, shell) + 1, max(2, round(3 * size)), max(4, round(6 * size))
    leaf, names, sp = blade_mat(light, dark, rib), [], {}
    for i, dx in enumerate((-3 * size, 0, 3 * size)):
        n = f"fx_leaf{i + 1}"
        m.bone(n, "root", [dx, y, 0], [Cube([dx - bw / 2, y, -bl / 2], [bw, 0, bl], leaf, effect=True),
                                       Cube([dx - bw / 2, y, -bl / 2], [bw, 0, bl], leaf, [0, 0, 30], [dx, y, 0], effect=True)])
        names.append(n)
        sp.update(fly_out(n, S * (0.44 + 0.07 * i), S * 0.32, [(-6 + 6 * i) * size, 2 * size, -60 * size], spin=(0, 1080, 0)))
    m.move_name, m.move_extras = "RAZOR LEAF", {"all": away(*names), "special": sp}


def embers(m, S, size, n=3, name="EMBER"):
    """EMBER (and FLAMETHROWER): little flames shot from its open mouth, one after another."""
    top, front, mouth = fx_for(m, size)
    names, sp = [], {}
    w, h = max(3, round(4 * size)), max(4, round(6 * size))
    for i in range(n):
        nm = f"fx_ember{i + 1}"
        py = mouth - h * 0.35
        m.bone(nm, "root", [0, py, front - 1], as_effect(flame([0, py, front - 1], w, h)))
        names.append(nm)
        gap = 0.4 / n
        sp.update(fly_out(nm, S * (0.45 + gap * i), S * 0.3, [(-2 + 4 * (i % 2)) * size, (-1.5 + (i % 3)) * size, -55 * size], grow=1.2))
    m.move_name, m.move_extras = name, {"all": away(*names), "special": sp}


def water_gun(m, S, power=1.0, name="WATER GUN"):
    """WATER GUN (HYDRO PUMP): the trunk swings up to aim and a long jet of water blasts out of it."""
    q = lambda f: S * f
    P = 1000 * power
    sp = {"spray": track(scale=keys({0: [1, 1, 1], q(.42): [1, 1, 1], q(.46): [P, P, 800], q(.55): [P, P, 2600 * power],
                                     q(.85): [P, P, 2600 * power], q(.92): [1, 1, 1]}))}
    for bone, ang in (("trunk", -12), ("trunk2", -10), ("trunk3", -8)):
        sp[bone] = track(rotation=keys({0: [0, 0, 0], q(.35): [ang, 0, 0], q(.88): [ang, 0, 0], S: [0, 0, 0]}))
    m.move_name, m.move_extras = name, {"special": sp}


def gust(m, S, size):
    """GUST: its wings beat hard and rings of wind roll out in front of it."""
    body = next(b for b in m.bones if b.name == "body")
    cs = [c for c in body.cubes if not c.effect]
    front = min(c.origin[2] for c in cs)
    y = (min(c.origin[1] for c in cs) + max(c.origin[1] + c.size[1] for c in cs)) / 2
    r, wm, names, sp = max(8, round(12 * size)), wind_mat(), [], {}
    for i in range(2):
        nm = f"fx_gust{i + 1}"
        pz = front - 3 * size
        m.bone(nm, "root", [0, y, pz], [Cube([-r / 2, y - r / 2, pz], [r, r, 0], wm, effect=True),
                                       Cube([-r / 2, y - r / 2, pz], [r, r, 0], wm, [0, 90, 0], [0, y, pz], effect=True)])
        names.append(nm)
        sp.update(fly_out(nm, S * (0.45 + 0.13 * i), S * 0.36, [0, size, -45 * size], spin=(0, 0, 360), grow=1.7))
    m.move_name, m.move_extras = "GUST", {"all": away(*names), "special": sp}


def hyper_fang(m, S, size, k=1.0):
    """HYPER FANG: it lunges and a pair of great fangs flash and clamp shut in front of its mouth."""
    top, front, mouth = fx_for(m, size)
    w, h = max(5, round(6 * size)), max(3, round(4 * size))
    pz = front - 2 * size
    m.bone("fx_fang_up", "head", [0, mouth + 0.5, pz], [Cube([-w / 2, mouth + 0.5, pz], [w, h, 0], fang_mat(True), effect=True)])
    m.bone("fx_fang_down", "head", [0, mouth - 0.5, pz], [Cube([-w / 2, mouth - 0.5 - h, pz], [w, h, 0], fang_mat(False), effect=True)])
    q = lambda f: S * f
    show = keys({0: [1, 1, 1], q(.36): [1, 1, 1], q(.38): [1000, 1000, 1000], q(.75): [1000, 1000, 1000], q(.78): [1, 1, 1]})
    sp = {"fx_fang_up": track(scale=show, position=keys({0: [0, 0, 0], q(.38): [0, 3 * size, 0], q(.5): [0, 0, 0]})),
          "fx_fang_down": track(scale=dict(show), position=keys({0: [0, 0, 0], q(.38): [0, -3 * size, 0], q(.5): [0, 0, 0]})),
          "body": track(position=keys({0: [0, 0, 0], q(.3): [0, -k, 2 * k], q(.48): [0, 0.6 * k, -5 * k], q(.75): [0, 0, -3 * k], S: [0, 0, 0]}),
                        rotation=keys({0: [0, 0, 0], q(.3): [8, 0, 0], q(.48): [-6, 0, 0], S: [0, 0, 0]})),
          "jaw": track(rotation=keys({0: [0, 0, 0], q(.38): [35, 0, 0], q(.5): [0, 0, 0], S: [0, 0, 0]}))}
    m.move_name, m.move_extras = "HYPER FANG", {"all": away("fx_fang_up", "fx_fang_down"), "special": sp}


def hidden(bone):
    """Tracks keeping a part tucked away (shrunk to nothing) while it isn't wanted: an effect shown only when calling."""
    return {bone: track(scale=[0.001, 0.001, 0.001])}


def burst(bone, length, rise=4.0, spin=90):
    """An effect's part bursting out while it calls (scaled back up from nothing), rising and turning."""
    L = length
    return {bone: track(scale=keys({0: [1, 1, 1], L * 0.12: [1000, 1000, 1000], L * 0.75: [1000, 1000, 1000], L * 0.95: [1, 1, 1]}),
                        position=keys({0: [0, 0, 0], L * 0.12: [0, rise * 0.3, 0], L * 0.75: [0, rise, 0]}),
                        rotation=keys({0: [0, 0, 0], L * 0.75: [0, spin, 0]}))}


def spray(bone, length):
    """Water spraying out while it calls: the jet grows out from the trunk's tip, then falls away."""
    L = length
    return {bone: track(scale=keys({0: [1, 1, 1], L * 0.18: [1000, 1000, 400], L * 0.3: [1000, 1000, 1000], L * 0.8: [1000, 1000, 1000],
                                    L * 0.95: [1, 1, 1]}))}


def leaf_spray(light, dark, rib):
    """Small leaves scattering out (each a flat part)."""
    return Mat(light, frond(light, dark, rib, "z", 0, 3, width=0.9), noise=0.02, edge=0)


def ember_ramp(t):
    return ramp(["#7A1E12", "#C9361A", "#F06A22", "#FFA43A", "#FFD960", "#FFF4C0"], t)


# ======================================================================================================
# The leaf armadillos: FERNADILLO > BRAMBADILLO > GROVADON
# ======================================================================================================

ARM = dict(skin="#D8B183", skin_d="#B98D60", cream="#F1E1BC", pink="#E79A98", claw="#4B3326",
           leaf="#4FA64A", leaf_l="#8AD06A", leaf_d="#2E6F35", vein="#B8E68C")


def leaf_band(leaf, leaf_l, leaf_d, vein, thorn=None, moss=None):
    """A band of the armour, a row of overlapping leaves: a dark seam at its front edge, a pale midrib down each leaf,
    the leaf tips scalloped along its lower edge. thorn: the colour of thorn-tips along its top; moss: moss over it."""
    L, LL, LD, V = rgb(leaf), rgb(leaf_l), rgb(leaf_d), rgb(vein)

    def f(at):
        c = at.cube
        if moss is not None and at.face == "top" and cell(at.x, at.y, at.z, 1.5) > 0.35:
            return mix(moss, "#9BC46A", cell(at.x + 7, at.y, at.z, 1) * 0.5)
        if at.face in ("px", "nx", "top") and at.z < c.origin[2] + 0.6:
            return LD
        if at.side:
            if at.row == at.fh - 1:
                return LL if int(math.floor(at.z)) % 2 == 0 else LD
            col = mix(L, LL, (1 - at.ly) * 0.8)
            if abs((at.z - c.origin[2]) - c.size[2] / 2) < 0.6:
                col = mix(col, V, 0.55)
            return col
        if at.face == "top":
            col = mix(L, LL, 0.2 + 0.4 * at.lz)
            if abs(((at.x + 0.5) / 3.0) % 1.0 - 0.5) < 0.17:
                col = mix(col, V, 0.5)
            if thorn is not None and abs(at.x) < 0.6:
                return rgb(thorn)
            return col
        if at.face in ("front", "back"):
            k = int(math.floor(at.x + 0.5)) % 2
            col = mix(L, LL, (1 - at.ly) * 0.7) * (1.04 if k else 0.96)
            if at.row == at.fh - 1:
                return LL if k else LD
            return col
        return LD
    return f


def armadillo_belly(skin, cream, below):
    def f(at):
        if at.face == "bottom" or at.y < below:
            return rgb(cream)
        return None
    return f


def fernadillo():
    c = ARM
    m = Model()
    skin = Mat(c["skin"], armadillo_belly(c["skin"], c["cream"], 4.2))
    leaf = Mat(c["leaf"], leaf_band(c["leaf"], c["leaf_l"], c["leaf_d"], c["vein"]), noise=0.025)
    tip = Mat(c["leaf_l"], lambda a: mix(c["leaf_l"], c["leaf"], a.ly), noise=0.02, edge=0)
    sprout = Mat("#7CCB5A", lambda a: mix("#4FA64A", "#B8E68C", a.ly), noise=0.02, edge=0)
    face = Mat(c["skin"], lambda a: rgb(c["cream"]) if a.face == "bottom" or (a.face == "front" and a.y < 5.0) else
               (rgb("#E9AE98") if a.face == "front" and a.y < 6.0 and abs(a.x) > 2.4 else None))
    muzzle = Mat("#E6C69A", smile("#5A3A2C"), lambda a: rgb("#B8606A") if a.face == "bottom" else None, noise=0.02)
    nose = Mat("#5A3A2C", nostrils("#24140E", "#8A6A5A"), noise=0.01, edge=0)
    jaw_m = Mat("#E6C69A", open_mouth("#C8606A", "#CFAE84"), noise=0.02)
    cheek = Mat(c["cream"], lambda a: rgb("#FFF4DA") if a.face == "top" else None, noise=0.02, edge=0)
    ear = Mat(c["skin"], lambda a: rgb(c["pink"]) if a.face == "front" and a.row > 0 else None)
    leg = Mat(c["skin"], lambda a: rgb(c["skin_d"]) if a.face == "bottom" or a.ly < 0.25 else None)
    claw = Mat(c["claw"], noise=0.01, edge=0)
    fern = Mat(c["leaf"], frond(c["leaf_l"], c["leaf_d"], c["vein"], "z", 0, 4), noise=0.02, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 6, 0], blob([-4.5, 2, -6], [9, 7, 12], skin))
    m.bone("shell", "body", [0, 8, 0])
    bands = [(-7.0, 6, 11.5), (-4.25, 6.5, 12), (-1.5, 6.5, 12.5), (1.25, 6.5, 12), (4.0, 6, 11.5)]
    for i, (z, y0, top) in enumerate(bands):
        h = int(round(top - y0 + 1))
        m.bones[-1].cubes += blob([-5.5, y0 - 1, z], [11, h, 3], leaf)
        # Leaf tips hanging over the band's lower edge at each side.
        m.bones[-1].cubes += [Cube([-6, y0 - 2, z + 1], [1, 2, 1], tip), Cube([5, y0 - 2, z + 1], [1, 2, 1], tip)]
    m.bones[-1].cubes.append(Cube([-4.5, 4.5, 6.75], [9, 4, 1], leaf))
    m.bone("crest", "shell", [0, 12, 0], [Cube([-1, 12, -4], [2, 2, 2], sprout, [-20, 0, 0], [0, 12, -3]),
                                          Cube([-1, 12.5, -1.25], [2, 2, 2], sprout, [-10, 0, 0], [0, 12.5, -0.25]),
                                          Cube([-1, 12, 1.5], [2, 2, 2], sprout, [10, 0, 0], [0, 12, 2.5])])
    m.bone("head", "body", [0, 6.5, -6], face_blob([-4, 3, -11.5], [8, 6, 6], face))
    m.bones[-1].cubes += [Cube([-2, 3, -13.5], [4, 3, 2], muzzle), Cube([-1, 5.25, -14.25], [2, 1, 1], nose),
                          Cube([-4, 8.5, -11], [8, 1, 5], leaf), Cube([-1.5, 8.5, -12], [3, 1, 1], tip),
                          Cube([-4.75, 3.25, -11], [1, 2, 3], cheek), Cube([3.75, 3.25, -11], [1, 2, 3], cheek)]
    m.bone("jaw", "head", [0, 3.5, -8.5], [Cube([-1.5, 2.25, -13], [3, 1, 5], jaw_m)])
    m.pair("ear_right", "head", [-3.5, 8.5, -8], [Cube([-4.5, 8.5, -8.5], [2, 3, 1], ear)], rotation=[0, 0, -20])
    m.eyes("head", 4, 5, -11.5, h=3, corner=(1, 2), half=4, iris="#7A4A22", pupil="#1A100A", skin=c["skin"], iris_light="#D0923A",
           grid=["LLL", "WPI", ".JJ"])
    leafy = leaf_spray(c["leaf_l"], c["leaf_d"], c["vein"])
    m.bone("burst", "shell", [0, 12, 0], [Cube([-1.5, 12, 0], [3, 0, 6], leafy, [-35, a, 0], [0, 12, 0]) for a in (0, 60, 120, 180, 240, 300)])
    stout_legs(m, 3, (-3.5, 3.5), 4, 3, leg, claw, 2)
    m.bone("tail", "body", [0, 5, 6], [Cube([-1, 4, 6], [2, 2, 3], skin)], rotation=[18, 0, 0])
    m.bone("tail2", "tail", [0, 5, 9], [Cube([-0.5, 4.5, 8.5], [1, 1, 2], skin)], rotation=[12, 0, 0])
    m.bone("tail3", "tail2", [0, 5, 10.5], [Cube([-2, 5, 10], [4, 0, 5], fern)], rotation=[-35, 0, 0])

    fx_for(m, 1.0)
    razor_leaf(m, 1.6, 1.0, c["leaf_l"], c["leaf_d"], c["vein"])
    A, info = four_legs("fernadillo", m, walk=0.8, stride=30, extras={
        "idle": {"shell": track(rotation=[wave(2, 1.2), 0, 0]), "crest": track(rotation=[wave(1, 4), 0, 0])},
        "battle": {"shell": track(rotation=[wave(1, 2), 0, 0]), "crest": track(rotation=[wave(0.5, 6), 0, 0])},
        "recoil": tuck(0.75, 24, 2.0),
        "all": hidden("burst"),
        "cry": merge({"shell": track(scale=keys({0: [1, 1, 1], 0.25: [1.06, 1.1, 1.06], 0.75: [1.06, 1.1, 1.06], 1.2: [1, 1, 1]}))}, burst("burst", 1.2))})
    write("bulbasaur", "FERNADILLO", m, A, info, 0.72)


def brambadillo():
    """Bigger and sterner: its leaves have darkened and grown thorns tipped violet, a fern crest sweeps back from its
    brow, and its claws are violet."""
    m = Model()
    SKIN, SKIN_D, CREAM = "#C9A272", "#A57E52", "#ECDDB6"
    LEAF, LEAF_L, LEAF_D, VEIN, THORN = "#3E8C43", "#6DBA5C", "#235A2C", "#A6D884", "#9A5CC0"
    skin = Mat(SKIN, armadillo_belly(SKIN, CREAM, 5.5))
    leaf = Mat(LEAF, leaf_band(LEAF, LEAF_L, LEAF_D, VEIN, thorn=THORN), noise=0.025)
    thorn = Mat(THORN, lambda a: mix("#5E2E86", "#D7A8F0", a.ly), noise=0.015, edge=0)
    tip = Mat(LEAF_L, lambda a: mix(LEAF_L, LEAF_D, 1 - a.ly), noise=0.02, edge=0)
    face = Mat(SKIN, lambda a: rgb(CREAM) if a.face == "bottom" or (a.face == "front" and a.y < 7.0) else None)
    muzzle = Mat("#E2C192", smile("#4A2E24", grin=False), lambda a: rgb("#A8505A") if a.face == "bottom" else None, noise=0.02)
    nose = Mat("#4A2E24", nostrils("#1A0E0A", "#7A5A4A"), noise=0.01, edge=0)
    jaw_m = Mat("#E2C192", open_mouth("#B8505C", "#C6A57A"), noise=0.02)
    ear = Mat(SKIN, lambda a: rgb("#D88A92") if a.face == "front" and a.row > 0 else None)
    leg = Mat(SKIN, lambda a: rgb(SKIN_D) if a.face == "bottom" or a.ly < 0.25 else None)
    claw = Mat("#6E3A8E", lambda a: rgb("#B07AD0") if a.face == "top" else None, noise=0.01, edge=0)
    fern = Mat(LEAF, frond(LEAF_L, LEAF_D, VEIN, "z", 0, 5), noise=0.02, edge=0)
    brow = Mat(SKIN_D, noise=0.02, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 9, 0], blob([-6, 3.5, -8], [12, 9, 16], skin))
    m.bone("shell", "body", [0, 11, 0])
    sh = m.bones[-1]
    zs = [-9.5, -6, -2.5, 1, 4.5]
    tops = [15, 16, 16.5, 16, 15]
    for z, top in zip(zs, tops):
        y0 = 7
        sh.cubes += blob([-7.5, y0, z], [15, int(top - y0), 4], leaf)
        sh.cubes += [Cube([-8, y0 - 1.5, z + 1], [1, 3, 2], tip), Cube([7, y0 - 1.5, z + 1], [1, 3, 2], tip)]
        # Thorns along the band's ridge, leaning back.
        for x in (-4.5, -0.5, 3.5):
            sh.cubes.append(Cube([x, top, z + 1], [1, 2, 1], thorn, [-25, 0, 0], [x + 0.5, top, z + 1.5]))
    sh.cubes.append(Cube([-6, 6, 8.5], [12, 6, 1], leaf))
    m.bone("head", "body", [0, 9, -8], face_blob([-5, 4.5, -15], [10, 8, 8], face))
    hd = m.bones[-1]
    hd.cubes += blob([-2.5, 4.5, -18], [5, 4, 4], muzzle)
    hd.cubes += [Cube([-1.5, 7.75, -18.75], [3, 1, 1], nose), Cube([-5, 12, -14.5], [10, 1, 6], leaf),
                 Cube([-7, 6, -13], [2, 1, 1], thorn, [0, 0, -15], [-5, 6.5, -12.5]), Cube([5, 6, -13], [2, 1, 1], thorn, [0, 0, 15], [5, 6.5, -12.5])]
    m.bone("crest", "head", [0, 13, -12], [Cube([-2.5, 13, -12], [5, 0, 7], fern, [-20, 0, 0], [0, 13, -12])])
    m.bone("jaw", "head", [0, 5, -12], [Cube([-2, 3.5, -17.5], [4, 1, 6], jaw_m)])
    m.pair("ear_right", "head", [-4.5, 12, -10], [Cube([-5.5, 12, -10.5], [2, 3, 1], ear)], rotation=[10, 0, -28])
    m.eyes("head", x_inner=1.5, y=9, z=-15, w=3, h=2, iris="#7A40A0", pupil="#160A18", skin=SKIN, iris_light="#C08CE0", grid=["DLL", "JPW"])
    m.brows("head", [Cube([-4.75, 10.9, -15.4], [4, 1, 1], brow, [0, 0, 14], [-0.75, 11.4, -15])])
    leafy = leaf_spray(LEAF_L, LEAF_D, VEIN)
    m.bone("burst", "shell", [0, 16, 0], [Cube([-2, 16, 0], [4, 0, 7], leafy, [-35, a, 0], [0, 16, 0]) for a in (0, 60, 120, 180, 240, 300)])
    stout_legs(m, 4, (-4.5, 4.5), 6, 4, leg, claw, 3)
    m.bone("tail", "body", [0, 7, 8], [Cube([-1.5, 5.5, 8], [3, 3, 4], skin)], rotation=[15, 0, 0])
    m.bone("tail2", "tail", [0, 7, 12], [Cube([-1, 6, 11.5], [2, 2, 3], skin)], rotation=[10, 0, 0])
    m.bone("tail3", "tail2", [0, 7, 14.5], [Cube([-2.5, 7, 14], [5, 0, 6], fern)], rotation=[-35, 0, 0])

    fx_for(m, 1.3)
    razor_leaf(m, 1.6, 1.3, LEAF_L, LEAF_D, VEIN)
    A, info = four_legs("brambadillo", m, walk=0.9, stride=26, k=1.2, extras={
        "idle": {"shell": track(rotation=[wave(2, 1), 0, 0]), "crest": track(rotation=[wave(2, 5), 0, 0])},
        "battle": {"crest": track(rotation=[wave(0.5, 8), 0, 0])},
        "walk": {"crest": track(rotation=[wave(0.45, 6), 0, 0])},
        "recoil": tuck(0.75 * 1.0, 22, 2.5),
        "all": hidden("burst"),
        "cry": merge({"crest": track(rotation=keys({0: [0, 0, 0], 0.25: [-25, 0, 0], 0.9: [-25, 0, 0], 1.2: [0, 0, 0]})),
                      "shell": track(scale=keys({0: [1, 1, 1], 0.25: [1.05, 1.08, 1.05], 0.75: [1.05, 1.08, 1.05], 1.2: [1, 1, 1]}))}, burst("burst", 1.2, 5))})
    write("ivysaur", "BRAMBADILLO", m, A, info, 1.05)


def grovadon():
    """Old and huge: its armour has become a mossy grove, ferns and violet-capped toadstools growing on it and thorns
    along its ridge. A mossy beard hangs from its jaw, its claws are great and violet, and its eyes are wise."""
    m = Model()
    SKIN, SKIN_D, CREAM = "#B8946A", "#8E6E4A", "#E2D2AC"
    LEAF, LEAF_L, LEAF_D, VEIN, THORN, MOSS = "#35793B", "#5FA552", "#1F4E27", "#93C878", "#8B4FB3", "#6E9E46"
    skin = Mat(SKIN, armadillo_belly(SKIN, CREAM, 9), lambda a: mix(SKIN, SKIN_D, 0.5) if a.side and cell(a.x, a.y, a.z, 2) > 0.8 else None)
    leaf = Mat(LEAF, leaf_band(LEAF, LEAF_L, LEAF_D, VEIN, thorn=None, moss=MOSS), noise=0.025)
    thorn = Mat(THORN, lambda a: mix("#4E2272", "#CFA0EE", a.ly), noise=0.015, edge=0)
    tip = Mat(LEAF_L, lambda a: mix(LEAF_L, LEAF_D, 1 - a.ly), noise=0.02, edge=0)
    moss = Mat(MOSS, lambda a: mix("#4E7A34", "#A6CC6E", cell(a.x, a.y, a.z, 1) * 0.8 + (0.2 if a.face == "top" else 0)), noise=0.03, edge=0)
    face = Mat(SKIN, lambda a: rgb(CREAM) if a.face == "bottom" or (a.face == "front" and a.y < 12.0) else None)
    muzzle = Mat("#D7B68A", smile("#3E2620", grin=False), lambda a: rgb("#9A4A50") if a.face == "bottom" else None, noise=0.02)
    nose = Mat("#3E2620", nostrils("#140A08", "#6A4A3A"), noise=0.01, edge=0)
    jaw_m = Mat("#D7B68A", open_mouth("#A84A54", "#B8966C"), noise=0.02)
    ear = Mat(SKIN, lambda a: rgb("#C98088") if a.face == "front" and a.row > 0 else None)
    leg = Mat(SKIN, lambda a: rgb(SKIN_D) if a.face == "bottom" or a.ly < 0.2 else (mix(MOSS, SKIN, 0.4) if a.side and a.ly > 0.75 and cell(a.x, a.y, a.z, 1.2) > 0.5 else None))
    claw = Mat("#5E2E80", lambda a: rgb("#A877C8") if a.face == "top" else None, noise=0.01, edge=0)
    stalk = Mat("#EDE3CC", lambda a: rgb("#CFC3A8") if a.ly < 0.3 else None, noise=0.02, edge=0)
    cap = Mat("#8B4FB3", lambda a: rgb("#F2EAF8") if (a.face == "top" or a.side) and cell(a.x, a.y, a.z, 1) > 0.72 else (rgb("#6A3790") if a.face == "bottom" else None),
              noise=0.02, edge=0.05)
    fern = Mat(LEAF, frond(LEAF_L, LEAF_D, VEIN, "y", 0, 6), noise=0.02, edge=0)
    beard = Mat(MOSS, lambda a: mix("#4E7A34", "#8CBA5C", a.ly + cell(a.x, a.y, a.z, 1) * 0.3), noise=0.03, edge=0)
    brow = Mat(SKIN_D, noise=0.02, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 15, 0], blob([-10, 7, -13], [20, 14, 26], skin, b=2))
    m.bone("shell", "body", [0, 18, 0])
    sh = m.bones[-1]
    zs = [-15, -10, -5, 0, 5, 10]
    tops = [26, 28, 29, 29, 28, 26]
    for z, top in zip(zs, tops):
        y0 = 12
        sh.cubes += blob([-12.5, y0, z], [25, int(top - y0), 6], leaf, b=2)
        sh.cubes += [Cube([-13.5, y0 - 3, z + 2], [1, 4, 2], tip), Cube([12.5, y0 - 3, z + 2], [1, 4, 2], tip)]
        sh.cubes.append(Cube([-1, top, z + 2], [2, 3, 2], thorn, [-25, 0, 0], [0, top, z + 3]))
    sh.cubes += blob([-10, 10, 15], [20, 11, 2], leaf)
    # Moss lumps over the top of the armour.
    for i, (x, z, w, d) in enumerate([(-9, -12, 6, 5), (3, -8, 7, 6), (-6, -1, 5, 6), (5, 4, 6, 5), (-10, 7, 6, 5), (-3, 10, 7, 4)]):
        top = max(t for zz, t in zip(zs, tops) if zz - 1 <= z + d / 2 <= zz + 7)
        sh.cubes.append(Cube([x, top - 0.5, z], [w, 2, d], moss))
    # Ferns standing up from the grove, and toadstools.
    m.bone("ferns", "shell", [0, 29, 0])
    fb = m.bones[-1]
    for i, (x, z, ry, rz) in enumerate([(-6, -6, 20, -18), (6, -3, -30, 16), (-3, 5, 60, -10), (7, 8, -60, 20), (0, -11, 5, 4)]):
        fb.cubes.append(Cube([x - 2.5, 28, z], [5, 9, 0], fern, [rz * 0.4, ry, rz], [x, 28, z]))
    m.bone("toadstools", "shell", [0, 29, 0])
    tb = m.bones[-1]
    for x, z, h, r in [(-8, 1, 4, 3), (8, -10, 3, 2), (2, 9, 5, 3), (-1, -4, 3, 2)]:
        top = 29.5
        tb.cubes += [Cube([x - 0.5, top - 1, z - 0.5], [1, h, 1], stalk), Cube([x - r, top - 1 + h, z - r], [2 * r + 1, 2, 2 * r + 1], cap),
                     Cube([x - r + 1, top + h + 1, z - r + 1], [2 * r - 1, 1, 2 * r - 1], cap)]
    m.bone("head", "body", [0, 15, -13], face_blob([-8, 8, -24], [16, 11, 12], face, b=2))
    hd = m.bones[-1]
    hd.cubes += blob([-4, 8, -29], [8, 6, 6], muzzle)
    hd.cubes += [Cube([-2.5, 13, -29.75], [5, 1, 1], nose), Cube([-8, 18, -23.5], [16, 2, 9], leaf), Cube([-7, 19.5, -22], [14, 1, 6], moss)]
    m.bone("jaw", "head", [0, 9, -19.5], [Cube([-3.5, 6.5, -28], [7, 2, 9], jaw_m)])
    m.bone("beard", "jaw", [0, 6.5, -24], [Cube([-3, 3, -27], [6, 4, 4], beard), Cube([-2, 1.5, -26.5], [4, 2, 3], beard)])
    m.pair("ear_right", "head", [-6, 18, -16], [Cube([-7.5, 18, -16.5], [3, 4, 1], ear)], rotation=[15, 0, -35])
    m.eyes("head", x_inner=2.5, y=14.5, z=-24, w=3, h=2, iris="#8A5A22", pupil="#160E08", skin=SKIN, iris_light="#E0B058", grid=["DLL", "JPW"])
    mossy_brow = Mat(MOSS, lambda a: mix("#4E7A34", "#8CBA5C", a.ly + cell(a.x, a.y, a.z, 1) * 0.3), noise=0.03, edge=0)
    m.brows("head", [Cube([-7.5, 16.4, -24.6], [6, 2, 1], mossy_brow, [0, 0, 10], [-1.5, 17.4, -24])])
    leafy = leaf_spray(LEAF_L, LEAF_D, VEIN)
    m.bone("burst", "shell", [0, 30, 0], [Cube([-3, 30, 0], [6, 0, 10], leafy, [-35, a, 0], [0, 30, 0]) for a in (0, 45, 90, 135, 180, 225, 270, 315)])
    stout_legs(m, 7.5, (-8, 8), 11, 7, leg, claw, 3, extra=lambda z: [Cube([-11.5, 9, z - 3], [2, 2, 6], moss)])
    m.bone("tail", "body", [0, 12, 13], [Cube([-2.5, 9.5, 13], [5, 5, 6], skin)], rotation=[14, 0, 0])
    m.bone("tail2", "tail", [0, 12, 19], [Cube([-1.5, 10.5, 18.5], [3, 3, 5], skin), Cube([-2, 13, 19], [4, 1, 4], moss)], rotation=[10, 0, 0])
    m.bone("tail3", "tail2", [0, 12, 23], [Cube([-4, 12, 22.5], [8, 0, 10], Mat(LEAF, frond(LEAF_L, LEAF_D, VEIN, "z", 0, 6), noise=0.02, edge=0))],
           rotation=[-30, 0, 0])

    fx_for(m, 2.0)
    razor_leaf(m, 2.24, 2.0, LEAF_L, LEAF_D, VEIN)
    A, info = four_legs("grovadon", m, walk=1.2, stride=18, k=1.8, slow=1.4, extras={
        "idle": {"ferns": track(rotation=[wave(2.8, 3), 0, wave(2.8, 3, 90)]), "toadstools": track(scale=pulse(5.6, 0.02)),
                 "beard": track(rotation=[wave(2.8, 4), 0, 0])},
        "battle": {"ferns": track(rotation=[wave(1.4, 5), 0, 0]), "beard": track(rotation=[wave(1.4, 6), 0, 0])},
        "walk": {"ferns": track(rotation=[wave(0.6, 5), 0, wave(1.2, 4)]), "beard": track(rotation=[wave(0.6, 8), 0, 0])},
        "recoil": tuck(0.75 * 1.4, 18, 3.0),
        # A stamp and a roar: the front up, then down hard.
        "all": hidden("burst"),
        "cry": {**burst("burst", 1.68, 8, 120),
                "body": track(rotation=keys({0: [0, 0, 0], 0.4: [-12, 0, 0], 0.9: [-12, 0, 0], 1.1: [3, 0, 0], 1.68: [0, 0, 0]}),
                              position=keys({0: [0, 0, 0], 0.4: [0, 2, 0], 0.9: [0, 2, 0], 1.1: [0, -0.5, 0], 1.68: [0, 0, 0]})),
                "ferns": track(rotation=keys({0: [0, 0, 0], 1.1: [0, 0, 0], 1.2: [10, 0, 6], 1.4: [-6, 0, -4], 1.68: [0, 0, 0]}))}})
    write("venusaur", "GROVADON", m, A, info, 2.63)


# ======================================================================================================
# The ember goats: CINDERKID > EMBERAM > PYRIBEX
# ======================================================================================================

def wool_paint(base, light, dark, belly=None, below=None):
    W, WL, WD = rgb(base), rgb(light), rgb(dark)

    def f(at):
        h = cell(at.x, at.y, at.z, 1.0)
        col = WL if h > 0.74 else WD if h < 0.16 else W
        if belly is not None and (at.face == "bottom" or (below is not None and at.y < below)):
            col = mix(col, belly, 0.5)
        return col
    return f


def goat_legs(m, xs, zs, top, knee, hoof_h, w, mat, shin, hoof, cuff=None, parent="body"):
    """Four legs, each an upper leg, a shin (that bends at the knee) and a hoof."""
    for end, z in (("front", zs[0]), ("back", zs[1])):
        x = xs[0] if end == "front" else xs[1]
        # The upper leg thick with muscle at the top (the haunch, on a back leg, bulging back), then a knee cap in front
        # (front legs) or the hock jutting back (back legs) where it bends.
        cut = knee + round((top - knee) * 0.4)
        back = end == "back"
        jw = max(1, w - 1)
        joint = Cube([-x - jw / 2, knee - 1, z - w / 2 - 1], [jw, 2, 1], mat) if not back else Cube([-x - jw / 2, knee - 1, z + w / 2], [jw, 2, 1], mat)
        m.pair(f"leg_{end}_right", parent, [-x, top, z], [Cube([-x - w / 2, knee, z - w / 2], [w, top - knee + 1, w], mat),
                                                          Cube([-x - (w + 1) / 2, cut, z - (w + 1) / 2], [w + 1, top - cut + 1, w + 1 + (1 if back else 0)], mat),
                                                          joint])
        H, x0, z0 = w + 1, -x - w / 2 - 0.5, z - w / 2 - 0.5
        tw = H // 2 if H % 2 == 0 else (H - 1) // 2
        th = max(1, round(hoof_h) - 1)
        # A cloven hoof: its heel, two toes in front with the cleft between them, and a dewclaw behind.
        sw = max(2, w - 1)
        cubes = [Cube([-x - sw / 2, hoof_h, z - sw / 2], [sw, knee - hoof_h, sw], shin), Cube([x0, 0, z0 + 1], [H, hoof_h, H - 1], hoof),
                 Cube([x0, 0, z0], [tw, th, 1], hoof), Cube([x0 + H - tw, 0, z0], [tw, th, 1], hoof),
                 Cube([x0 + (H - 1) / 2, hoof_h, z0 + H], [1, 1, 1], hoof)]
        if cuff is not None:
            cubes.append(Cube([-x - w / 2 - 0.5, hoof_h, z - w / 2 - 0.5], [w + 1, 1, w + 1], cuff))
        m.pair(f"shin_{end}_right", f"leg_{end}_right", [-x, knee, z], cubes)


def hoof_mat(dark="#241C20"):
    """Hard dark hoof, a sheen down its front, the cleft dark, smouldering along its bottom edge."""
    def f(at):
        if at.face == "bottom" or (at.face != "top" and at.row == at.fh - 1 and at.fh > 1):
            return rgb("#FF7A2A")
        if at.face == "front" and at.fw >= 3 and at.col == at.fw // 2 and at.fw % 2 == 0:
            return rgb("#0E0A0C")
        if at.face == "top":
            return mix(dark, "#4A3E44", 0.35)
        return mix(dark, "#5A4C54", 0.3 * (1 - at.ly)) if at.face == "front" else rgb(dark)
    return Mat(dark, f, glow=lambda a: rgb("#FF8A2A") if a.face == "bottom" or (a.face != "top" and a.row == a.fh - 1 and a.fh > 1) else None,
               noise=0.02)


def horn_mat(lo, hi, ridges=False):
    def col(at):
        t = lo + (hi - lo) * at.ly
        c = ember_ramp(t)
        if ridges and at.side and at.row % 2 == 0:
            c = mix(c, "#3A1A12", 0.35)
        return c
    return Mat("#8E2414", col, glow=lambda a: ember_ramp(min(1, lo + 0.2 + (hi - lo) * a.ly)) if (not ridges or a.row % 2 == 1) else None,
               noise=0.015, shade=0.4, edge=0)


def horn(m, side_name, parent, start, seg_sizes, rots, lo=0.0, hi=1.0, ridges=False):
    """A horn of segments, each turning on from the last (Bedrock turns, degrees)."""
    x, y, z = start
    name_prev = parent
    n = len(seg_sizes)
    for i, ((w, h), rot) in enumerate(zip(seg_sizes, rots)):
        nm = f"{side_name}{'' if i == 0 else '_' + str(i)}_right"
        mat = horn_mat(lo + (hi - lo) * i / n, lo + (hi - lo) * (i + 1) / n, ridges)
        m.pair(nm, name_prev, [x, y, z], [Cube([x - w / 2, y, z - w / 2], [w, h, w], mat)], rotation=rot)
        name_prev = nm
        y += h


def cinderkid():
    m = Model()
    WOOL, WOOL_L, WOOL_D, CREAM = "#4A4248", "#6A5F66", "#2F2A2E", "#EBDDC6"
    wool = Mat(WOOL, wool_paint(WOOL, WOOL_L, WOOL_D, belly="#5E555A", below=8.5), noise=0.02)
    tuft = Mat(WOOL_L, wool_paint(WOOL_L, "#857980", WOOL), noise=0.02, edge=0)
    bib = Mat(CREAM, lambda a: rgb("#D9C8AE") if hash01("b", a.col, a.row, a.face) > 0.7 else None, noise=0.02)
    muzzle = Mat(CREAM, goat_nose("#B89A92", "#3A2A2A", "#5A3A34", -13.5, 17, 14, 1.5, line_to=-12.5, top="#F6EAD6"), noise=0.02)
    jaw = Mat("#E0D0B6", jaw_teeth("#B8505A", "#FFFBF0", -13.25), noise=0.02)
    ear = Mat(WOOL, lambda a: rgb("#E8662C") if a.face == "bottom" else None, glow=lambda a: rgb("#B83A18") if a.face == "bottom" else None, noise=0.02)
    leg = Mat(WOOL, lambda a: mix(WOOL, WOOL_D, 1 - a.ly) if a.face != "top" else None, noise=0.02)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 10, 0], blob([-3.5, 7.5, -5.5], [7, 6, 11], wool) +
           [Cube([-2.5, 13.5, -4], [5, 1, 3], tuft), Cube([-2, 13.5, 0], [4, 1, 3], tuft), Cube([-3, 8, -6.5], [6, 5, 2], bib)])
    m.bone("neck", "body", [0, 12.5, -5], [Cube([-1.5, 11, -7.5], [3, 4, 3], wool)], rotation=[-12, 0, 0])
    m.bone("head", "neck", [0, 15, -7], face_blob([-4, 14, -11.5], [8, 7, 5], wool) +
           [Cube([-2, 14, -13.5], [4, 3, 2], muzzle), Cube([-0.5, 12, -12], [1, 2, 1], bib)])
    m.bone("jaw", "head", [0, 14.5, -9.5], [Cube([-1.5, 13.25, -13.25], [3, 1, 4], jaw)])
    m.bone("fringe", "head", [0, 20.5, -9.5], flame([0, 20.5, -9.5], 4, 6))
    horn(m, "horn", "head", [-2.5, 20.5, -8.5], [(1, 3), (1, 2), (1, 2)], [[-30, 0, -14], [-25, 0, 0], [-30, 0, 0]])
    m.pair("ear_right", "head", [-4, 18.5, -9], [Cube([-7, 18, -9.75], [3, 1, 2], ear)], rotation=[0, -15, 42])
    m.eyes("head", x_inner=1.5, y=16.5, z=-11.5, w=2, h=3, iris="#E88A20", pupil="#1A0A06", skin=WOOL, iris_light="#FFD060", grid=["LL", "WP", "JI"])
    m.brows("head", [Cube([-4, 19.6, -11.9], [3, 1, 1], tuft, [0, 0, 12], [-1, 20.1, -11.5])])
    goat_legs(m, (2.25, 2.25), (-3.5, 3.5), 9, 5, 2, 2, leg, leg, hoof_mat(), cuff=tuft)
    m.bone("tail", "body", [0, 12.5, 5.5], [Cube([-1, 12, 5], [2, 2, 2], wool)], rotation=[-40, 0, 0])
    # Kept upright on its tail (turned back against the tail's lift), the way a flame burns.
    m.bone("tail2", "tail", [0, 13.5, 6], flame([0, 13.5, 6], 5, 8), rotation=[40, 0, 0])
    flick = {"fringe": track(scale=[1, wave(0.5, 0.12, 0, 1), 1]), "tail2": track(scale=[wave(0.4, 0.08, 0, 1), wave(0.3, 0.16, 0, 1), 1])}
    embers(m, 1.6, 1.0)
    A, info = four_legs("cinderkid", m, walk=0.7, stride=30, extras={
        "all": flick,
        "cry": {"tail2": track(scale=keys({0: [1, 1, 1], 0.25: [1.3, 1.6, 1.3], 0.8: [1.3, 1.6, 1.3], 1.2: [1, 1, 1]})),
                "fringe": track(scale=keys({0: [1, 1, 1], 0.25: [1.2, 1.5, 1.2], 0.8: [1.2, 1.5, 1.2], 1.2: [1, 1, 1]})),
                "leg_front_right": track(rotation=keys({0: [0, 0, 0], 0.25: [-30, 0, 0], 0.8: [-30, 0, 0], 1.2: [0, 0, 0]})),
                "leg_front_left": track(rotation=keys({0: [0, 0, 0], 0.25: [-22, 0, 0], 0.8: [-22, 0, 0], 1.2: [0, 0, 0]})),
                "body": track(rotation=keys({0: [0, 0, 0], 0.25: [-14, 0, 0], 0.8: [-14, 0, 0], 1.2: [0, 0, 0]}),
                              position=keys({0: [0, 0, 0], 0.25: [0, 1.2, 0], 0.8: [0, 1.0, 0], 1.2: [0, 0, 0]}))}})
    write("charmander", "CINDERKID", m, A, info, 1.0)


def emberam():
    """A young ram now: longer-legged, its horns curling back round its ears, a mane of embers down its neck, and
    glowing cracks down its legs like cooling lava."""
    m = Model()
    WOOL, WOOL_L, WOOL_D, CREAM = "#3E363C", "#5E5258", "#262124", "#E3D2B8"
    wool = Mat(WOOL, wool_paint(WOOL, WOOL_L, WOOL_D, belly="#544A50", below=13), noise=0.02)
    tuft = Mat(WOOL_L, wool_paint(WOOL_L, "#7A6E74", WOOL), noise=0.02, edge=0)
    bib = Mat(CREAM, lambda a: rgb("#CDB99C") if hash01("b", a.col, a.row, a.face) > 0.7 else None, noise=0.02)
    muzzle = Mat(CREAM, goat_nose("#8E7A74", "#2A1E1E", "#4A2E2A", -17.5, 24, 20, 1, tooth="#FFF8EA", tooth_z=-16, line_to=-15,
                                   top="#F2E4CE"), noise=0.02)
    jaw = Mat("#D8C6AA", jaw_teeth("#B04850", "#FFF8EA", -17.25), noise=0.02)
    ear = Mat(WOOL, lambda a: rgb("#E8662C") if a.face == "bottom" else None, glow=lambda a: rgb("#B83A18") if a.face == "bottom" else None, noise=0.02)

    def lava(at):
        if at.face != "top" and at.side and abs(math.sin(at.y * 1.3 + at.z * 2.1)) < 0.18 and at.ly < 0.85:
            return rgb("#FF7A2A")
        return mix(WOOL, WOOL_D, 1 - at.ly) if at.face != "top" else None

    leg = Mat(WOOL, lava, glow=lambda a: rgb("#FF8A2A") if a.side and abs(math.sin(a.y * 1.3 + a.z * 2.1)) < 0.18 and a.ly < 0.85 else None, noise=0.02)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 15, 0], blob([-4.5, 11, -7.5], [9, 8, 15], wool) +
           [Cube([-3.5, 19, -6], [7, 1, 4], tuft), Cube([-3, 19, -1], [6, 1, 4], tuft), Cube([-2.5, 19, 3.5], [5, 1, 3], tuft),
            Cube([-4, 12, -8.5], [8, 6, 2], bib)])
    m.bone("neck", "body", [0, 17, -7], [Cube([-2, 15.5, -10], [4, 6, 4], wool)], rotation=[-18, 0, 0])
    m.bone("mane", "neck", [0, 21, -8], flame([0, 20.5, -7.5], 5, 8) + flame([0, 18.5, -4.5], 5, 7))
    m.bone("head", "neck", [0, 21, -9], face_blob([-4.5, 20, -15], [9, 8, 6], wool) +
           [Cube([-2.5, 20, -17.5], [5, 4, 3], muzzle), Cube([-1, 17.5, -15.5], [2, 3, 1], bib)])
    m.bone("jaw", "head", [0, 20.5, -12.5], [Cube([-2, 19, -17.25], [4, 1, 5], jaw)])
    m.bone("fringe", "head", [0, 27.5, -12.5], flame([0, 27.5, -12.5], 5, 7))
    # The horns curl back and down round the ears.
    horn(m, "horn", "head", [-3, 27, -11], [(2, 3), (2, 3), (1, 3), (1, 2), (1, 2)],
         [[-45, 0, -20], [-45, 0, 0], [-45, 0, 0], [-40, 0, 0], [-35, 0, 0]], lo=0.1, hi=0.95)
    m.pair("ear_right", "head", [-4.5, 24.5, -12], [Cube([-8, 24, -12.75], [4, 1, 2], ear)], rotation=[0, -15, 40])
    m.eyes("head", x_inner=1.25, y=24.5, z=-15, w=3, h=2, iris="#E07818", pupil="#1A0A06", skin=WOOL, iris_light="#FFC850", grid=["DLL", "JPW"])
    m.brows("head", [Cube([-4.5, 26.4, -15.4], [3, 1, 1], tuft, [0, 0, 16], [-1.5, 26.9, -15])])
    goat_legs(m, (3, 3), (-5, 5), 13, 7, 2.5, 3, leg, leg, hoof_mat(), cuff=tuft)
    m.bone("tail", "body", [0, 18, 7.5], [Cube([-1, 17, 7], [2, 3, 2], wool)], rotation=[-45, 0, 0])
    m.bone("tail2", "tail", [0, 19.5, 8], flame([0, 19, 8], 6, 11, smoke=True), rotation=[45, 0, 0])
    flick = {"fringe": track(scale=[1, wave(0.5, 0.12, 0, 1), 1]), "mane": track(scale=[1, wave(0.4, 0.1, 30, 1), wave(0.6, 0.06, 0, 1)]),
             "tail2": track(scale=[wave(0.4, 0.08, 0, 1), wave(0.3, 0.16, 0, 1), 1])}
    embers(m, 1.6, 1.3)
    A, info = four_legs("emberam", m, walk=0.75, stride=28, k=1.2, extras={
        "all": flick,
        "cry": {"mane": track(scale=keys({0: [1, 1, 1], 0.25: [1.3, 1.5, 1.3], 0.8: [1.3, 1.5, 1.3], 1.2: [1, 1, 1]})),
                "leg_front_right": track(rotation=keys({0: [0, 0, 0], 0.25: [-35, 0, 0], 0.8: [-35, 0, 0], 1.2: [0, 0, 0]})),
                "leg_front_left": track(rotation=keys({0: [0, 0, 0], 0.25: [-25, 0, 0], 0.8: [-25, 0, 0], 1.2: [0, 0, 0]})),
                "body": track(rotation=keys({0: [0, 0, 0], 0.25: [-16, 0, 0], 0.8: [-16, 0, 0], 1.2: [0, 0, 0]}),
                              position=keys({0: [0, 0, 0], 0.25: [0, 1.6, 0], 0.8: [0, 1.4, 0], 1.2: [0, 0, 0]}))}})
    write("charmeleon", "EMBERAM", m, A, info, 1.54)


def pyribex():
    """A great winged ibex: huge ridged horns sweeping back, wings of ember feathers, a blazing mane and tail, heavy
    chest wool, and glowing seams down its legs."""
    m = Model()
    WOOL, WOOL_L, WOOL_D, CREAM = "#352E34", "#57494F", "#1E1A1D", "#E0CDAE"
    wool = Mat(WOOL, wool_paint(WOOL, WOOL_L, WOOL_D, belly="#4A4046", below=21), noise=0.02)
    tuft = Mat(WOOL_L, wool_paint(WOOL_L, "#75676D", WOOL), noise=0.02, edge=0)
    bib = Mat(CREAM, lambda a: rgb("#C8B394") if cell(a.x, a.y, a.z, 1) > 0.65 else None, noise=0.02)
    muzzle = Mat(CREAM, goat_nose("#6E5E5A", "#1E1416", "#3A2424", -26, 40, 36, 1, tooth="#FFF8EA", tooth_z=-24.5, line_to=-22,
                                   top="#F0E2CA"), noise=0.02)
    jaw = Mat("#D2BFA2", jaw_teeth("#A84048", "#FFF8EA", -25.5), noise=0.02)
    ear = Mat(WOOL, lambda a: rgb("#E8662C") if a.face == "bottom" else None, glow=lambda a: rgb("#B83A18") if a.face == "bottom" else None, noise=0.02)

    def lava(at):
        if at.side and abs(math.sin(at.y * 1.1 + at.z * 1.7)) < 0.15 and at.ly < 0.85:
            return rgb("#FF7A2A")
        return mix(WOOL, WOOL_D, 1 - at.ly) if at.face != "top" else None

    leg = Mat(WOOL, lava, glow=lambda a: rgb("#FF8A2A") if a.side and abs(math.sin(a.y * 1.1 + a.z * 1.7)) < 0.15 and a.ly < 0.85 else None, noise=0.02)

    def wing_bone_paint(at):
        return mix(WOOL_D, WOOL, at.ly)

    arm = Mat(WOOL, wing_bone_paint, noise=0.02)

    def feather(at):
        # Ember feathers: dark at the root, red to orange to a glowing gold edge.
        t = at.ly if at.cube.size[1] > 0 else 0.5
        c = ramp(["#2A1A18", "#7A1E12", "#C9361A", "#F06A22", "#FFB44A"], 1 - t)
        if at.edge and (1 - t) > 0.6:
            c = mix(c, "#FFE08A", 0.6)
        if at.face in ("front", "back") and at.col % 3 == 0:
            c = c * 0.85
        return c

    fea = Mat("#C9361A", feather, glow=lambda a: ember_ramp(0.55 + 0.45 * (1 - a.ly)) if (1 - a.ly) > 0.55 else None, noise=0.015, shade=0.35, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 26, 0], blob([-7, 19, -12], [14, 13, 24], wool, b=2) +
           [Cube([-6, 32, -10], [12, 1, 6], tuft), Cube([-5, 32, -3], [10, 1, 6], tuft), Cube([-4.5, 32, 4], [9, 1, 5], tuft)] +
           blob([-6, 19, -14], [12, 11, 4], bib))
    m.bone("neck", "body", [0, 29, -11], blob([-3.5, 27, -16], [7, 10, 7], wool), rotation=[-20, 0, 0])
    m.bone("mane", "neck", [0, 34, -12], flame([0, 36, -11], 7, 11) + flame([0, 33, -7.5], 7, 10) + flame([0, 31, -4], 6, 8))
    m.bone("head", "neck", [0, 36, -14], face_blob([-6, 35, -22], [12, 11, 9], wool) +
           blob([-3.5, 35, -26], [7, 6, 5], muzzle) + [Cube([-1.5, 31, -22.5], [3, 5, 2], bib), Cube([-1, 29, -22], [2, 2, 1], bib)])
    m.bone("jaw", "head", [0, 36, -20], [Cube([-3, 33.5, -25.5], [6, 2, 6], jaw)])
    m.bone("fringe", "head", [0, 45.5, -18.5], flame([0, 45.5, -18.5], 7, 9))
    horn(m, "horn", "head", [-3.5, 45, -16], [(3, 5), (3, 5), (2, 5), (2, 4), (2, 4), (1, 3)],
         [[-35, 0, -16], [-25, 0, -4], [-22, 0, 0], [-20, 0, 0], [-18, 0, 0], [-15, 0, 0]], lo=0.05, hi=1.0, ridges=True)
    m.pair("ear_right", "head", [-6, 41, -18], [Cube([-11, 40.5, -19], [5, 2, 3], ear)], rotation=[0, -15, 35])
    m.eyes("head", x_inner=1.75, y=42, z=-22, w=3, h=2, iris="#F07818", pupil="#160806", skin=WOOL, iris_light="#FFD050", grid=["DLL", "JPW"])
    m.brows("head", [Cube([-5.75, 43.9, -22.5], [5, 1, 1], tuft, [0, 0, 16], [-1.25, 44.4, -22])])
    # Wings, folded at rest along its sides: the coverts on an arm along the shoulder, the ember feathers hanging from it.
    efea = Mat("#C9361A", feather_plane("#2A1A18", "#C9361A", "#FFB44A", edge="#FFE08A"), noise=0.015, edge=0,
               glow=lambda a: ember_ramp(0.5 + 0.5 * (1 - a.ly)) if a.ly < 0.4 else None)
    folded_wing(m, 7, 33, -8, 18, 4, 2, [(0, 3, 10), (3, 3, 10), (6, 3, 10), (9, 3, 10), (12, 3, 10)], [(0, 3, 15), (3, 3, 14), (6, 3, 13), (9, 3, 12)],
                arm, efea, efea)
    goat_legs(m, (4.5, 4.5), (-8, 8), 22, 11, 3, 4, leg, leg, hoof_mat(), cuff=tuft)
    m.bone("tail", "body", [0, 29, 12], [Cube([-1.5, 28, 11.5], [3, 4, 3], wool)], rotation=[-40, 0, 0])
    m.bone("tail2", "tail", [0, 31.5, 13], flame([0, 30.5, 13.5], 10, 18, smoke=True), rotation=[40, 0, 0])
    flick = {"fringe": track(scale=[1, wave(0.5, 0.12, 0, 1), 1]), "mane": track(scale=[1, wave(0.4, 0.1, 30, 1), wave(0.6, 0.06, 0, 1)]),
             "tail2": track(scale=[wave(0.4, 0.08, 0, 1), wave(0.3, 0.16, 0, 1), 1])}
    fold = {}
    fly = {"period": 0.7, "hover_period": 0.9,
           "fly": merge(flick, {"body": track(rotation=[12, 0, 0], position=[0, wave(0.7, 1.5, 90), 0]),
                                "leg_front_right": track(rotation=[-50, 0, 0]), "leg_front_left": track(rotation=[-50, 0, 0]),
                                "leg_back_right": track(rotation=[55, 0, 0]), "leg_back_left": track(rotation=[55, 0, 0]),
                                "neck": track(rotation=[10, 0, 0]), "head": track(rotation=[-6, 0, 0])}, wings(10, 40, 85, 0.7)),
           "hover": merge(flick, {"body": track(rotation=[-8, 0, 0], position=[0, wave(0.9, 2, 90), 0]),
                                  "leg_front_right": track(rotation=[-25, 0, 0]), "leg_front_left": track(rotation=[-20, 0, 0]),
                                  "leg_back_right": track(rotation=[20, 0, 0]), "leg_back_left": track(rotation=[20, 0, 0])}, wings(14, 35, 80, 0.9))}
    embers(m, 2.0, 1.8, n=6, name="FLAMETHROWER")
    A, info = four_legs("pyribex", m, walk=1.0, stride=24, k=2.0, slow=1.25, fly=fly, extras={
        "all": flick, "idle": fold, "walk": fold,
        "battle": {"wing_right": track(rotation=[0, 0, wave(1.25, 5, 0, 22)]), "wing_left": track(rotation=[0, 0, wave(1.25, -5, 0, -22)])},
        "cry": {"wing_right": track(rotation=keys({0: [0, 0, 0], 0.3: [0, 0, 95], 1.1: [0, 0, 95], 1.5: [0, 0, 0]})),
                "wing_left": track(rotation=keys({0: [0, 0, 0], 0.3: [0, 0, -95], 1.1: [0, 0, -95], 1.5: [0, 0, 0]})),
                "mane": track(scale=keys({0: [1, 1, 1], 0.3: [1.3, 1.5, 1.3], 1.1: [1.3, 1.5, 1.3], 1.5: [1, 1, 1]})),
                "body": track(rotation=keys({0: [0, 0, 0], 0.3: [-18, 0, 0], 1.1: [-18, 0, 0], 1.5: [0, 0, 0]}),
                              position=keys({0: [0, 0, 0], 0.3: [0, 3, 0], 1.1: [0, 2.6, 0], 1.5: [0, 0, 0]}))}})
    write("charizard", "PYRIBEX", m, A, info, 4.15)


# ======================================================================================================
# The sea tapirs: TIDAPIR > SURFAPIR > TSUNAPIR
# ======================================================================================================

def tapir_paint(blue, blue_l, blue_d, foam, belly, waves, belly_below, head=False):
    """A tapir's coat: deep on its back, a bold wave of foam along each side, the belly pale below a wavy line."""
    B, BL, BD, F, BE = rgb(blue), rgb(blue_l), rgb(blue_d), rgb(foam), rgb(belly)

    def f(at):
        if at.face == "bottom":
            return BE
        c = mix(BD, B, 0.35 + 0.65 * (1 - at.ly)) if at.face != "top" else BD
        if at.side:
            line = belly_below + 0.6 * math.sin(at.z * 0.55)
            if at.y < line:
                return mix(B, BE, 0.75)
            for base, amp, freq in waves[:1]:
                yy = base + amp * math.sin(at.z * freq * 0.6 + 1.3)
                if -0.9 < at.y - yy < 0.6:
                    return F if at.y - yy > -0.4 else mix(F, BL, 0.5)
        if at.face == "front":
            if head and at.ly < 0.45:
                return mix(B, BL, 0.7)
            if not head and at.y < belly_below + 1:
                return mix(B, BE, 0.6)
        if at.face == "top" and not head and abs(at.x) > 2 and cell(at.x, at.y, at.z, 1) > 0.85:
            return mix(BD, F, 0.35)
        return c
    return f


def stout_legs(m, cx, zs, top, w, leg, toe, ntoes, extra=None, foot=None):
    """Four sturdy jointed legs: a thick upper leg, a knee cap (front) or a hock jutting back (back) at the joint, a
    slimmer lower leg that bends there, and a broad foot with its toes or claws along the front."""
    body = next(b for b in m.bones if b.name == "body")
    hw = max(abs(v) for c in body.cubes if not c.effect for v in (c.origin[0], c.origin[0] + c.size[0]))
    cx = max(cx, hw - w * 0.15)  # set wide, half out from under the body
    knee = max(2, round(top * 0.5))
    wl = max(2, w - 1) if w <= 4 else w - 2
    fh = 1 if top <= 6 else 2
    tw = 1 if w < 6 else 2
    foot = foot or leg
    for end, z in (("front", zs[0]), ("back", zs[1])):
        x0, z0 = -cx - w / 2, z - w / 2
        upper = blob([x0, knee, z0], [w, top - knee + 1, w], leg) if w >= 6 else [Cube([x0, knee, z0], [w, top - knee + 1, w], leg)]
        jw = max(1, w - 2)
        joint = Cube([-cx - jw / 2, knee - 1, z0 - 1], [jw, 2, 1], leg) if end == "front" else Cube([-cx - jw / 2, knee - 1, z0 + w], [jw, 2, 1], leg)
        m.pair(f"leg_{end}_right", "body", [-cx, top, z], upper + [joint] + (extra(z) if extra else []))
        lower = [Cube([-cx - wl / 2, fh, z - wl / 2], [wl, knee - fh + 1, wl], leg), Cube([x0, 0, z0 - 1], [w, fh, w + 1], foot)]
        toes = [Cube([x0 + (i + 0.5) * w / ntoes - tw / 2, 0, z0 - 2], [tw, 1 if tw == 1 else fh, 1], toe) for i in range(ntoes)]
        m.pair(f"shin_{end}_right", f"leg_{end}_right", [-cx, knee, z], lower + toes)


def tapir_toes(x, z, n, w, mat):
    return [Cube([x + i * (w / n), 0, z - 1], [1, 1, 1], mat) for i in range(n)]


def tidapir():
    m = Model()
    BLUE, BLUE_L, BLUE_D, FOAM, BELLY, AQUA = "#2F74BA", "#4E95D6", "#1F5590", "#DDF6FF", "#A6DDEE", "#6FD3EE"
    body = Mat(BLUE, tapir_paint(BLUE, BLUE_L, BLUE_D, FOAM, BELLY, [(8.5, 0.6, 0.7), (6.3, 0.6, 0.7)], 4.8))
    head = Mat(BLUE, tapir_paint(BLUE, BLUE_L, BLUE_D, FOAM, BELLY, [(10, 0.4, 1.2)], 8.5, head=True), muzzle_patch(mix(BLUE_L, BELLY, 0.45), -11, 9.5, 2.5, BLUE_L))
    trunk = Mat(BLUE_L, lambda a: rgb("#123A66") if a.face == "front" and a.row == a.fh // 2 and 0 < a.col < a.fw - 1 else
                (rgb(BELLY) if a.face == "bottom" else mix(BLUE, BLUE_L, 0.5 + 0.3 * a.ly)), noise=0.02)
    jaw = Mat(BELLY, noise=0.02)
    fin = Mat(AQUA, lambda a: mix(AQUA, FOAM, a.ly * 0.8), noise=0.02, shade=0.6)
    ear = Mat(BLUE, lambda a: rgb(FOAM) if a.row == 0 and a.face != "bottom" else (rgb(AQUA) if a.face == "front" else None), noise=0.02)
    leg = Mat(BLUE, lambda a: rgb("#16406E") if a.face == "bottom" else mix(BLUE_D, BLUE, a.ly), noise=0.025)
    toe = Mat(FOAM, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.01, edge=0)
    cheek = Mat(BLUE_L, noise=0.02)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 7, 0], blob([-4, 3.5, -6], [8, 7, 12], body))
    m.bone("back_fin", "body", [0, 10.5, 0], [Cube([-0.5, 10.5, -4], [1, 1, 8], fin), Cube([-0.5, 11.5, -2.5], [1, 1, 5], fin), Cube([-0.5, 12.5, -1], [1, 1, 2], fin)])
    m.bone("head", "body", [0, 9, -6], face_blob([-3.5, 6.5, -11], [7, 6, 5], head) +
           [Cube([-0.5, 12.5, -10], [1, 1, 3], fin), Cube([-0.5, 13.5, -9], [1, 1, 1], fin), Cube([-4.25, 6.75, -10.5], [1, 2, 2], cheek),
            Cube([3.25, 6.75, -10.5], [1, 2, 2], cheek)])
    m.bone("jaw", "head", [0, 7, -8.5], [Cube([-1.5, 5.75, -12], [3, 1, 4], jaw)])
    m.bone("trunk", "head", [0, 8.5, -11], [Cube([-1.5, 6.5, -13.5], [3, 3, 3], trunk)])
    m.bone("trunk2", "trunk", [0, 7.5, -13.5], [Cube([-1, 6, -15], [2, 2, 2], trunk)], rotation=[15, 0, 0])
    m.bone("trunk3", "trunk2", [0, 7, -15], [Cube([-1, 5, -16], [2, 2, 1], trunk)], rotation=[20, 0, 0])
    m.bone("spray", "trunk3", [0, 6, -16], jet([0, 6, -16], 10, 6))
    m.pair("ear_right", "head", [-2.5, 12, -8], [Cube([-4, 11.5, -8.5], [2, 2, 1], ear)], rotation=[0, 0, -20])
    m.eyes("head", 3.5, 8.5, -11, h=3, corner=(1, 2), half=3.5, iris="#1E5A8A", pupil="#0A1426", skin=BLUE, iris_light="#8AE4F4",
           grid=["LLL", "WPI", ".JJ"])
    stout_legs(m, 2.5, (-3.75, 3.75), 4, 3, leg, toe, 2)
    m.bone("tail", "body", [0, 9, 6], [Cube([-0.5, 8, 6], [1, 2, 2], fin), Cube([-1.5, 8.5, 7.5], [3, 1, 1], fin)], rotation=[-20, 0, 0])
    fx_for(m, 1.0)
    water_gun(m, 1.6)
    A, info = four_legs("tidapir", m, walk=0.8, stride=28, idle="ground", extras={
        "all": hidden("spray"),
        "idle": {"trunk": track(rotation=[wave(1.33, 6), wave(2, 5), 0]), "trunk2": track(rotation=[wave(1, 8, 60), 0, 0]),
                 "trunk3": track(rotation=[wave(1, 10, 120), 0, 0])},
        "battle": {"trunk": track(rotation=[wave(0.5, 8, 0, -6), 0, 0]), "trunk2": track(rotation=[wave(0.5, 10, 60), 0, 0]),
                   "back_fin": track(scale=[1, wave(1, 0.1, 0, 1), 1])},
        "walk": {"trunk": track(rotation=[wave(0.4, 7), 0, 0])},
        "cry": {"trunk": track(rotation=keys({0: [0, 0, 0], 0.25: [-55, 0, 0], 0.5: [-48, 0, 0], 0.8: [-55, 0, 0], 1.2: [0, 0, 0]})),
                "trunk2": track(rotation=keys({0: [0, 0, 0], 0.25: [-30, 0, 0], 0.8: [-30, 0, 0], 1.2: [0, 0, 0]})),
                "trunk3": track(rotation=keys({0: [0, 0, 0], 0.25: [-25, 0, 0], 0.8: [-25, 0, 0], 1.2: [0, 0, 0]})), **spray("spray", 1.2)}})
    write("squirtle", "TIDAPIR", m, A, info, 0.72)


def surfapir():
    """Its ears have become fins, a sail of a fin runs along its back, its tail ends in a fluke, a curl of foam sits
    on its brow like a breaking wave, and its trunk is longer."""
    m = Model()
    BLUE, BLUE_L, BLUE_D, FOAM, BELLY, AQUA = "#2A66AA", "#4A8ACB", "#1A4A80", "#E6FAFF", "#9ED6EA", "#62CBE8"
    body = Mat(BLUE, tapir_paint(BLUE, BLUE_L, BLUE_D, FOAM, BELLY, [(11.5, 0.8, 0.5), (9, 0.8, 0.5), (6.6, 0.6, 0.5)], 6.5))
    head = Mat(BLUE, tapir_paint(BLUE, BLUE_L, BLUE_D, FOAM, BELLY, [(13.5, 0.5, 0.9)], 11, head=True), muzzle_patch(mix(BLUE_L, BELLY, 0.45), -14.5, 12.5, 3, BLUE_L))
    trunk = Mat(BLUE_L, lambda a: rgb("#103462") if a.face == "front" and a.row == a.fh // 2 and 0 < a.col < a.fw - 1 else
                (rgb(BELLY) if a.face == "bottom" else mix(BLUE, BLUE_L, 0.5 + 0.3 * a.ly)), noise=0.02)
    jaw = Mat(BELLY, noise=0.02)
    fin = Mat(AQUA, lambda a: mix(AQUA, FOAM, a.ly * 0.8) if not (a.side and a.col % 2 == 0) else mix(AQUA, BLUE_L, 0.5), noise=0.02, shade=0.6)
    foam = Mat(FOAM, lambda a: rgb("#BDEBFA") if cell(a.x, a.y, a.z, 1) > 0.7 else None, noise=0.02, edge=0)
    leg = Mat(BLUE, lambda a: rgb("#123A6A") if a.face == "bottom" else mix(BLUE_D, BLUE, a.ly), noise=0.025)
    toe = Mat(FOAM, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.01, edge=0)
    cheek = Mat(BLUE_L, noise=0.02)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 10, 0], blob([-5, 5, -8], [10, 9, 16], body))
    m.bone("back_fin", "body", [0, 14, 0], [Cube([-0.5, 14, -6], [1, 1, 12], fin), Cube([-0.5, 15, -4.5], [1, 2, 9], fin), Cube([-0.5, 17, -3], [1, 2, 6], fin),
                                             Cube([-0.5, 19, -1.5], [1, 1, 3], fin)])
    m.bone("head", "body", [0, 12, -8], face_blob([-4.5, 9, -14.5], [9, 7, 7], head) +
           [Cube([-5.25, 9.5, -13.5], [1, 3, 3], cheek), Cube([4.25, 9.5, -13.5], [1, 3, 3], cheek)])
    m.bone("foam", "head", [0, 16, -12], [Cube([-2, 16, -13], [4, 2, 4], foam), Cube([-1.5, 18, -14.5], [3, 1, 3], foam), Cube([-1, 17, -15.5], [2, 1, 1], foam)])
    m.bone("jaw", "head", [0, 10, -11.5], [Cube([-2, 8.25, -16], [4, 1, 5], jaw)])
    m.bone("trunk", "head", [0, 11.5, -14.5], [Cube([-2, 9, -17.5], [4, 4, 3], trunk)])
    m.bone("trunk2", "trunk", [0, 10.5, -17.5], [Cube([-1.5, 8.5, -20], [3, 3, 3], trunk)], rotation=[15, 0, 0])
    m.bone("trunk3", "trunk2", [0, 10, -20], [Cube([-1, 7.5, -22], [2, 3, 2], trunk)], rotation=[22, 0, 0])
    m.bone("spray", "trunk3", [0, 9, -22], jet([0, 9, -22], 13, 8))
    m.pair("ear_right", "head", [-4, 15.5, -10.5], [Cube([-7, 15, -11], [3, 3, 0], fin), Cube([-5.5, 15, -11.5], [1, 2, 1], fin)], rotation=[0, 20, -30])
    m.eyes("head", 4.5, 11.5, -14.5, h=3, corner=(1, 2), half=4.5, iris="#1E5A8A", pupil="#081226", skin=BLUE, iris_light="#8AE4F4",
           grid=["DLL", "WPI", ".JJ"])
    stout_legs(m, 3.25, (-5, 5), 6, 4, leg, toe, 3)
    m.bone("tail", "body", [0, 12, 8], [Cube([-0.5, 11, 8], [1, 2, 3], fin)], rotation=[-15, 0, 0])
    m.bone("tail2", "tail", [0, 12, 11], [Cube([-3, 11.5, 10.5], [6, 0, 3], fin)], rotation=[10, 0, 0])
    fx_for(m, 1.2)
    water_gun(m, 1.6)
    A, info = four_legs("surfapir", m, walk=0.85, stride=26, k=1.2, idle="battle", extras={
        "all": hidden("spray"),
        "idle": {"trunk": track(rotation=[wave(1.33, 6), wave(2, 5), 0]), "trunk2": track(rotation=[wave(1, 8, 60), 0, 0]),
                 "trunk3": track(rotation=[wave(1, 10, 120), 0, 0]), "foam": track(scale=pulse(1, 0.05))},
        "battle": {"trunk": track(rotation=[wave(0.5, 8, 0, -6), 0, 0]), "trunk2": track(rotation=[wave(0.5, 10, 60), 0, 0]),
                   "back_fin": track(scale=[1, wave(1, 0.08, 0, 1), 1]), "foam": track(scale=pulse(0.5, 0.06))},
        "walk": {"trunk": track(rotation=[wave(0.42, 7), 0, 0]), "tail2": track(rotation=[wave(0.42, 12), 0, 0])},
        "cry": {"trunk": track(rotation=keys({0: [0, 0, 0], 0.25: [-60, 0, 0], 0.5: [-50, 0, 0], 0.8: [-60, 0, 0], 1.2: [0, 0, 0]})),
                "trunk2": track(rotation=keys({0: [0, 0, 0], 0.25: [-30, 0, 0], 0.8: [-30, 0, 0], 1.2: [0, 0, 0]})),
                "trunk3": track(rotation=keys({0: [0, 0, 0], 0.25: [-25, 0, 0], 0.8: [-25, 0, 0], 1.2: [0, 0, 0]})), **spray("spray", 1.2)}})
    write("wartortle", "SURFAPIR", m, A, info, 1.0)


def tsunapir():
    """Vast and heavy: its back is armoured in coral and barnacles, a great crest fin rises over it, its trunk is
    long and ends in a wide spout, a beard of foam hangs under its chin, and its feet are broad and pale-toed."""
    m = Model()
    BLUE, BLUE_L, BLUE_D, FOAM, BELLY, AQUA = "#1F5A9C", "#3F80C2", "#123E70", "#E6FAFF", "#94CDE4", "#58C2E2"
    CORAL, CORAL_L, CORAL_D = "#F0806A", "#FFB6A0", "#B8483C"
    body = Mat(BLUE, tapir_paint(BLUE, BLUE_L, BLUE_D, FOAM, BELLY, [(22, 1.2, 0.3), (17, 1.2, 0.3), (12.5, 1.0, 0.3)], 11))
    head = Mat(BLUE, tapir_paint(BLUE, BLUE_L, BLUE_D, FOAM, BELLY, [(26, 0.8, 0.5)], 21, head=True), muzzle_patch(mix(BLUE_L, BELLY, 0.45), -26, 21, 5, BLUE_L))
    trunk = Mat(BLUE_L, lambda a: (rgb(BELLY) if a.face == "bottom" else mix(BLUE, BLUE_L, 0.5 + 0.3 * a.ly)) if not (a.side and a.row % 3 == 0)
                else mix(BLUE, BLUE_D, 0.5), noise=0.02)
    spout = Mat(BLUE_L, lambda a: rgb("#0C2648") if a.face == "front" and 0 < a.row < a.fh - 1 and 0 < a.col < a.fw - 1 else None, noise=0.02)
    jaw = Mat(BELLY, noise=0.02)
    fin = Mat(AQUA, lambda a: mix(AQUA, FOAM, a.ly * 0.8) if not (a.side and a.col % 2 == 0) else mix(AQUA, BLUE_L, 0.5), noise=0.02, shade=0.6)
    foam = Mat(FOAM, lambda a: rgb("#BDEBFA") if cell(a.x, a.y, a.z, 1) > 0.65 else None, noise=0.02, edge=0)
    plate = Mat(BLUE_D, lambda a: mix(BLUE_D, BLUE, 0.3 + 0.4 * a.ly) if not a.edge else rgb("#0C2A50"), noise=0.025)
    coral = Mat(CORAL, lambda a: mix(CORAL_D, CORAL_L, a.ly) if not (a.face == "top") else rgb(CORAL_L), noise=0.02, edge=0)
    barn = Mat("#E8E2D2", lambda a: rgb("#6A6458") if a.face == "top" and not a.edge else None, noise=0.02, edge=0)
    leg = Mat(BLUE, lambda a: rgb("#0E2E58") if a.face == "bottom" else mix(BLUE_D, BLUE, a.ly), noise=0.025)
    toe = Mat(FOAM, lambda a: rgb(BELLY) if a.face == "bottom" else None, noise=0.01, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 18, 0], blob([-10, 9, -15], [20, 17, 30], body, b=2))
    m.bone("armour", "body", [0, 26, 0])
    ar = m.bones[-1]
    for i, z in enumerate([-13, -7, -1, 5, 10]):
        ar.cubes += blob([-11, 22, z], [22, 6, 6], plate)
    for x, z, h, r in [(-8, -10, 5, 0), (7, -6, 4, 20), (-6, 3, 6, -10), (8, 7, 5, 10), (-2, -12, 3, 0), (2, 11, 4, -15), (-9, 9, 3, 15)]:
        ar.cubes += [Cube([x - 1, 27.5, z - 1], [2, h, 2], coral, [0, 0, r], [x, 27.5, z]), Cube([x - 2.5, 27.5 + h - 2, z - 0.5], [2, 2, 1], coral, [0, 0, r], [x, 27.5, z]),
                     Cube([x + 0.5, 27.5 + h - 3, z - 0.5], [2, 2, 1], coral, [0, 0, r], [x, 27.5, z])]
    for x, z in [(-4, -4), (4, 0), (-1, 6), (6, -11), (-7, -1), (1, -8)]:
        ar.cubes.append(Cube([x - 1, 27.5, z - 1], [2, 1, 2], barn))
    m.bone("back_fin", "armour", [0, 28, 0], [Cube([-0.5, 28, -9], [1, 2, 18], fin), Cube([-0.5, 30, -7], [1, 3, 13], fin), Cube([-0.5, 33, -4], [1, 3, 8], fin),
                                               Cube([-0.5, 36, -2], [1, 2, 4], fin)])
    m.bone("head", "body", [0, 22, -15], face_blob([-8, 16, -26], [16, 12, 12], head, b=2))
    m.bone("foam", "head", [0, 16, -22], [Cube([-4, 12, -26], [8, 5, 5], foam), Cube([-3, 10, -25.5], [6, 2, 4], foam), Cube([-2, 8.5, -25], [4, 2, 3], foam)])
    m.bone("crest", "head", [0, 28, -20], [Cube([-0.5, 28, -24], [1, 2, 8], fin), Cube([-0.5, 30, -22], [1, 2, 5], fin)])
    m.bone("jaw", "head", [0, 17, -20.5], [Cube([-3, 15, -27], [6, 2, 7], jaw)])
    m.bone("trunk", "head", [0, 21, -26], [Cube([-3.5, 16.5, -31], [7, 7, 5], trunk)])
    m.bone("trunk2", "trunk", [0, 20, -31], [Cube([-3, 15.5, -35], [6, 6, 4], trunk)], rotation=[15, 0, 0])
    m.bone("trunk3", "trunk2", [0, 18.5, -35], [Cube([-2.5, 14, -38], [5, 5, 3], trunk), Cube([-3.5, 13, -40], [7, 7, 2], spout)], rotation=[20, 0, 0])
    m.bone("spray", "trunk3", [0, 16.5, -40], jet([0, 16.5, -40], 20, 12))
    m.pair("ear_right", "head", [-7, 26, -19], [Cube([-13, 25, -20], [6, 5, 0], fin), Cube([-10, 25, -20.5], [2, 3, 1], fin)], rotation=[0, 20, -25])
    m.eyes("head", x_inner=3.5, y=22.5, z=-26, w=3, h=2, iris="#1A4E80", pupil="#060E1C", skin=BLUE, iris_light="#8AE4F4", grid=["DLL", "PIE"])
    m.brows("head", [Cube([-8.5, 24.4, -26.6], [6, 1, 1], Mat(BLUE_D, noise=0.02, edge=0), [0, 0, 12], [-3, 24.9, -26])])
    stout_legs(m, 6.5, (-9, 9), 12, 8, leg, toe, 3)
    m.bone("tail", "body", [0, 20, 15], [Cube([-1.5, 18, 15], [3, 4, 5], fin)], rotation=[-15, 0, 0])
    m.bone("tail2", "tail", [0, 20, 20], [Cube([-6, 20, 19.5], [12, 0, 5], fin)], rotation=[10, 0, 0])
    fx_for(m, 2.0)
    water_gun(m, 2.24, 1.5, "HYDRO PUMP")
    A, info = four_legs("tsunapir", m, walk=1.25, stride=16, k=2.0, slow=1.4, idle="battle", extras={
        "all": hidden("spray"),
        "idle": {"trunk": track(rotation=[wave(1.87, 5), wave(2.8, 4), 0]), "trunk2": track(rotation=[wave(1.4, 6, 60), 0, 0]),
                 "trunk3": track(rotation=[wave(1.4, 8, 120), 0, 0]), "foam": track(scale=pulse(1.4, 0.04))},
        "battle": {"trunk": track(rotation=[wave(0.7, 6, 0, -8), 0, 0]), "trunk2": track(rotation=[wave(0.7, 8, 60), 0, 0]),
                   "back_fin": track(scale=[1, wave(1.4, 0.05, 0, 1), 1])},
        "walk": {"trunk": track(rotation=[wave(0.62, 6), 0, 0]), "tail2": track(rotation=[wave(0.62, 10), 0, 0])},
        "cry": {"trunk": track(rotation=keys({0: [0, 0, 0], 0.35: [-65, 0, 0], 0.7: [-55, 0, 0], 1.1: [-65, 0, 0], 1.68: [0, 0, 0]})),
                "trunk2": track(rotation=keys({0: [0, 0, 0], 0.35: [-30, 0, 0], 1.1: [-30, 0, 0], 1.68: [0, 0, 0]})),
                "trunk3": track(rotation=keys({0: [0, 0, 0], 0.35: [-28, 0, 0], 1.1: [-28, 0, 0], 1.68: [0, 0, 0]})), **spray("spray", 1.68)}})
    write("blastoise", "TSUNAPIR", m, A, info, 3.1)


# ======================================================================================================
# The quails: QUAILET > PLUMQUAIL > REGALQUAIL
# ======================================================================================================

def quail_paints(LIL, LIL_L, LIL_D, CREAM, SCALE, FACE_D):
    def body(at):
        if at.face == "bottom":
            return rgb(CREAM)
        if at.face == "front":
            if at.row % 2 == 1 and (at.col + at.row // 2) % 2 == 0:
                return rgb(SCALE)
            return rgb(CREAM)
        if at.side and at.ly < 0.3:
            return mix(LIL, CREAM, 0.6)
        return mix(LIL_D, LIL, 0.5 + 0.5 * (1 - at.ly)) if at.face != "top" else rgb(LIL_L)

    def head(at):
        brow = at.cube.origin[1] + at.cube.size[1] * 0.78 if at.cube.size[1] else 0
        if at.face == "front":
            if at.ly < 0.42:
                return rgb("#FFFFFF") if at.ly < 0.12 or at.col in (0, at.fw - 1) else rgb(FACE_D)
            if abs(at.y - brow) < 0.5:
                return rgb("#FFFFFF")
            return rgb(LIL_D)
        if at.side:
            if abs(at.y - brow) < 0.5:
                return rgb("#FFFFFF")
            if at.ly < 0.35 and at.z < at.cube.origin[2] + at.cube.size[2] * 0.5:
                return rgb(FACE_D) if at.ly > 0.12 else rgb("#FFFFFF")
            return mix(LIL_D, LIL, 0.4)
        if at.face == "top":
            return rgb(LIL_D)
        return rgb(CREAM) if at.face == "bottom" else mix(LIL_D, LIL, 0.4)

    def feather(at):
        t = (at.z - at.cube.origin[2]) / max(1, at.cube.size[2])
        c = mix(LIL, LIL_D, max(0, t - 0.5) * 2)
        if at.side and int(math.floor(at.y + at.z * 0.5)) % 2 == 0 and t < 0.75:
            c = mix(c, LIL_L, 0.65)
        if at.side and at.row == at.fh - 1:
            c = mix(c, CREAM, 0.6)
        return c

    return body, head, feather


def feather_plane(root, mid, tip, edge=None, bars=None, spot=None):
    """A feather on a flat part, hanging from its top: root colour to tip colour, a pale shaft, a rounded tip,
    bars across it (every other row) if bars, an edge colour along its tip."""
    def f(at):
        c = at.cube
        if c.size[0] == 0:
            v, u = 1 - at.ly, at.lz
        elif c.size[1] == 0:
            v, u = at.lz, at.lx
        else:
            v, u = 1 - at.ly, at.lz
        a = abs(u - 0.5) * 2
        if v > 0.78 and a > max(0.0, (1 - v) / 0.22) + 0.15:
            return CLEAR
        col = ramp([root, mid, tip], v)
        if bars is not None and int(math.floor(v * (c.size[1] or c.size[2]) * 0.5)) % 2 == 0 and v < 0.85:
            col = mix(col, bars, 0.45)
        if a < 0.15 and v < 0.9:
            col = mix(col, "#FFFFFF", 0.25)
        if edge is not None and v > 0.86:
            col = mix(col, edge, 0.7)
        return col
    return f


def folded_wing(m, x, y_top, z0, length, cov_h, thick, secondaries, primaries, cov, sec, pri, parent="body"):
    """A wing at rest, folded against the side: the coverts (a block along the shoulder), then the secondaries and
    primaries, feathers hanging from it, the primaries reaching back past it. Turning wing_right about z by a positive
    angle (the left by a negative one) lifts it out and up into flight; wing_mid and wing_tip follow."""
    m.pair("wing_right", parent, [-x, y_top, z0 + 1], [Cube([-x - thick, y_top - cov_h, z0], [thick, cov_h, length], cov)])
    xs = -x - thick / 2
    yb = y_top - cov_h + 1
    m.pair("wing_mid_right", "wing_right", [xs, yb, z0 + 1],
           [Cube([xs - 0.03 * i, yb - h, z0 + zo], [0, h, w], sec) for i, (zo, w, h) in enumerate(secondaries)])
    zt = z0 + length * 0.55
    m.pair("wing_tip_right", "wing_mid_right", [xs, yb, zt],
           [Cube([xs - 0.03 * (i + 3), yb - h + 1, zt + zo], [0, h, w], pri, [-12 - 4 * i, 0, 0], [xs, yb, zt + zo]) for i, (zo, w, h) in enumerate(primaries)])


def trousers(base, light, dark):
    """Feathers down a bird's thigh: rows of small overlapping scallops, darker underneath, ragged at the bottom."""
    def f(at):
        if at.face == "top":
            return None
        if at.face == "bottom":
            return rgb(dark)
        k = (at.col + (at.row % 2)) % 2
        c = mix(light, base, 0.4 + 0.4 * (1 - at.ly)) if k == 0 else mix(base, dark, 0.3 + 0.3 * (1 - at.ly))
        return c
    return Mat(base, f, noise=0.02, edge=0)


def talon_mat(dark="#2E2426", tip="#8A7A70"):
    """A hooked claw: dark, paler at its point."""
    return Mat(dark, lambda a: rgb(tip) if a.face == "front" or (a.face == "bottom") else None, noise=0.01, edge=0)


def quail_feet(m, x, y_hip, leg_h, mat, scale=1.0, talon=None, trouser=None):
    """Legs feathered down to the ankle (trouser: the feathers' look), then bare and scaly, each with a bird's foot:
    three long toes splayed forward and one back, each ending in a hooked talon."""
    talon = talon or talon_mat()
    cubes = [Cube([-x - 0.5 * scale, y_hip - leg_h, -0.5 * scale], [1 * scale, leg_h, 1 * scale], mat)]
    if trouser is not None:
        th, tw = max(2, round(leg_h * 0.5)), max(2, round(scale)) + 1
        cubes += [Cube([-x - tw / 2, y_hip - th, -tw / 2], [tw, th + 1, tw], trouser),
                  Cube([-x - (tw - 1) / 2, y_hip - th - 1, -(tw - 1) / 2], [tw - 1, 1, tw - 1], trouser)]
    m.pair("leg_right", "body", [-x, y_hip, 0], cubes)
    L = max(2, round(2.2 * scale + 0.4))
    toes = []
    for a in (-30, 0, 30):
        toes += [Cube([-x - 0.5, 0, -L], [1, 1, L], mat, [0, a, 0], [-x, 0.5, 0]),
                 Cube([-x - 0.5, 0, -L - 1], [1, 1, 1], talon, [0, a, 0], [-x, 0.5, 0])]
    hb = max(1, round(L * 0.55))
    toes += [Cube([-x - 0.5, 0, 0], [1, 1, hb], mat), Cube([-x - 0.5, 0, hb], [1, 1, 1], talon)]
    m.pair("foot_right", "leg_right", [-x, y_hip - leg_h, 0], toes)


def quailet():
    m = Model()
    LIL, LIL_L, LIL_D, CREAM, SCALE, PLUME = "#9A8DB4", "#B8AECB", "#6C6088", "#F3E3C4", "#DCC29C", "#2C2638"
    body_p, head_p, feather_p = quail_paints(LIL, LIL_L, LIL_D, CREAM, SCALE, "#3E3550")
    body = Mat(LIL, body_p, noise=0.025)
    head = Mat(LIL, head_p, noise=0.02)
    beak = Mat("#3A3640", lambda a: rgb("#5A5662") if a.face == "top" else None, noise=0.02, edge=0)
    plume = Mat(PLUME, lambda a: mix(PLUME, "#4A4060", a.ly * 0.6), noise=0.02, edge=0)
    fea = Mat(LIL, feather_p, noise=0.02)
    tail = Mat(LIL_D, lambda a: mix(LIL_D, PLUME, a.lz), noise=0.02, edge=0)
    legm = Mat("#E58C4E", lambda a: rgb("#C9703A") if a.face == "bottom" else None, noise=0.02, edge=0)
    fluff = Mat(CREAM, lambda a: rgb(SCALE) if a.face == "front" and a.row == a.fh - 1 else None, noise=0.02, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 5, 0], blob([-3.5, 2, -4], [7, 6, 8], body) + [Cube([-2.5, 2.5, -4.5], [5, 4, 1], body),
                                                                            Cube([-2, 2, -5], [4, 1, 1], fluff), Cube([-2.5, 7.5, -2.5], [5, 1, 5], body)])
    m.bone("head", "body", [0, 8, -2], face_blob([-3, 7.5, -5], [6, 5, 5], head))
    m.bone("beak", "head", [0, 9.5, -5], [Cube([-0.5, 9, -6], [1, 1, 1], beak)])
    m.bone("beak_lower", "head", [0, 9, -5], [Cube([-0.5, 8.4, -5.75], [1, 1, 1], Mat("#4A4650", noise=0.01, edge=0))])
    m.bone("plume", "head", [0, 12.5, -3], [Cube([-0.5, 12.5, -3.5], [1, 2, 1], plume)], rotation=[-10, 0, 0])
    m.bone("plume_tip", "plume", [0, 14.5, -3], [Cube([-0.5, 13.5, -5], [1, 2, 2], plume)], rotation=[35, 0, 0])
    m.eyes("head", 3, 8.5, -5, h=3, corner=(1, 2), half=3, iris="#6A3A1A", pupil="#0C0A10", skin=LIL_D, iris_light="#D89A4A", grid=["LLL", "WPI", ".JJ"])
    sec = Mat(LIL, feather_plane(LIL, LIL_D, PLUME, bars=LIL_L), noise=0.015, edge=0)
    pri = Mat(LIL_D, feather_plane(LIL_D, "#4A4066", PLUME, edge=CREAM), noise=0.015, edge=0)
    folded_wing(m, 3.5, 7.5, -3.5, 6, 3, 1, [(0, 2, 3), (1.5, 2, 3), (3, 2, 3)], [(0, 2, 4), (1.5, 2, 4)], fea, sec, pri)
    tfea = Mat(LIL_D, feather_plane(LIL_D, "#4A4066", PLUME, bars=LIL), noise=0.015, edge=0)
    m.bone("tail", "body", [0, 5, 4], [Cube([-0.75, 5, 3.5], [1.5, 0, 4], tfea, [0, ry, 0], [0, 5, 3.5]) for ry in (-36, -18, 0, 18, 36)] +
           [Cube([-1.5, 4.5, 3], [3, 1, 2], tail)], rotation=[-25, 0, 0])
    m.bone("cheeks", "head", [0, 8.5, -4], [Cube([-3.25, 8, -4.75], [1, 1, 2], Mat("#FFFFFF", noise=0.01, edge=0)),
                                            Cube([2.25, 8, -4.75], [1, 1, 2], Mat("#FFFFFF", noise=0.01, edge=0)),
                                            Cube([-1.5, 12.5, -1], [3, 1, 2], plume)])
    quail_feet(m, 1.25, 2.5, 1.5, legm)
    fx_for(m, 0.9)
    gust(m, 1.6, 0.9)
    A, info = bird("quailet", m, walk=0.5, stride=40)
    write("pidgey", "QUAILET", m, A, info, 0.73)


def plumquail():
    """Taller and quicker: a longer neck, two plumes, a long barred tail, bigger wings, and a dark mask through
    its eyes edged in white."""
    m = Model()
    LIL, LIL_L, LIL_D, CREAM, SCALE, PLUME = "#8F82AC", "#B2A7C8", "#5F5380", "#F1DFBE", "#D6B992", "#262032"
    body_p, head_p, feather_p = quail_paints(LIL, LIL_L, LIL_D, CREAM, SCALE, "#2E2640")
    body = Mat(LIL, body_p, noise=0.025)
    head = Mat(LIL, head_p, noise=0.02)
    neck = Mat(LIL, lambda a: mix(LIL_D, LIL, a.ly) if a.face != "front" else rgb(CREAM), noise=0.02)
    beak = Mat("#3A3640", lambda a: rgb("#5A5662") if a.face == "top" else None, noise=0.02, edge=0)
    plume = Mat(PLUME, lambda a: mix(PLUME, "#4A4060", a.ly * 0.6), noise=0.02, edge=0)
    fea = Mat(LIL, feather_p, noise=0.02)

    def barred(at):
        t = at.lz
        c = mix(LIL_D, PLUME, t * 0.8)
        if int(math.floor(at.z)) % 2 == 0:
            c = mix(c, LIL_L, 0.45)
        return c

    tail = Mat(LIL_D, barred, noise=0.02, edge=0)
    legm = Mat("#E58C4E", lambda a: rgb("#C9703A") if a.face == "bottom" else None, noise=0.02, edge=0)
    fluff = Mat(CREAM, lambda a: rgb(SCALE) if a.face == "front" and a.row == a.fh - 1 else None, noise=0.02, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 9, 0], blob([-4.5, 5, -6], [9, 8, 11], body) + [Cube([-3.5, 5.5, -6.5], [7, 6, 1], body),
                                                                              Cube([-3, 5, -7], [6, 1, 1], fluff), Cube([-2, 4.5, -6], [4, 1, 2], fluff)])
    m.bone("neck", "body", [0, 12, -4], [Cube([-2, 11, -6], [4, 5, 4], neck)], rotation=[-10, 0, 0])
    m.bone("head", "neck", [0, 16, -4.5], face_blob([-3.5, 15, -8.5], [7, 6, 6], head))
    m.bone("beak", "head", [0, 17.5, -8.5], [Cube([-0.5, 17, -10], [1, 1, 2], beak)])
    m.bone("beak_lower", "head", [0, 17, -8.5], [Cube([-0.5, 16.4, -9.5], [1, 1, 1], Mat("#4A4650", noise=0.01, edge=0))])
    m.bone("plume", "head", [0, 21, -6], [Cube([-1.5, 21, -6.5], [1, 3, 1], plume, [0, 0, 10], [-1, 21, -6]), Cube([0.5, 21, -6.5], [1, 3, 1], plume, [0, 0, -10], [1, 21, -6])],
           rotation=[-10, 0, 0])
    m.bone("plume_tip", "plume", [0, 24, -6], [Cube([-2, 23, -8], [1, 2, 2], plume, [0, 0, 10], [-1.5, 24, -6]),
                                               Cube([1, 23, -8], [1, 2, 2], plume, [0, 0, -10], [1.5, 24, -6])], rotation=[35, 0, 0])
    m.eyes("head", 3.5, 16.5, -8.5, h=3, corner=(1, 2), half=3.5, iris="#6A3A1A", pupil="#0C0A10", skin=LIL_D, iris_light="#E0A24A",
           grid=["DLL", "WPI", ".JJ"])
    sec = Mat(LIL, feather_plane(LIL, LIL_D, PLUME, bars=LIL_L), noise=0.015, edge=0)
    pri = Mat(LIL_D, feather_plane(LIL_D, "#433A60", PLUME, edge=CREAM), noise=0.015, edge=0)
    folded_wing(m, 4.5, 12, -5, 9, 4, 1, [(0, 2, 4), (1.5, 2, 4), (3, 2, 4), (4.5, 2, 4)], [(0, 2, 6), (1.5, 2, 6), (3, 2, 5)], fea, sec, pri)
    tfea = Mat(LIL_D, feather_plane(LIL_D, LIL, PLUME, bars=LIL_L), noise=0.015, edge=0)
    m.bone("tail", "body", [0, 9, 5], [Cube([-1, 9, 4.5], [2, 0, 8 if ry == 0 else 7], tfea, [0, ry, 0], [0, 9, 4.5]) for ry in (-30, -20, -10, 0, 10, 20, 30)] +
           [Cube([-2, 8.5, 4], [4, 1, 3], tail)], rotation=[-15, 0, 0])
    m.bone("mask", "head", [0, 18, -8], [Cube([-4, 17.5, -8], [1, 1, 3], Mat("#FFFFFF", noise=0.01, edge=0)),
                                         Cube([3, 17.5, -8], [1, 1, 3], Mat("#FFFFFF", noise=0.01, edge=0)),
                                         Cube([-2, 20.5, -4], [4, 1, 3], plume), Cube([-2.5, 13, -6.5], [5, 2, 2], fluff)])
    quail_feet(m, 2, 5, 4, legm, trouser=trousers(LIL, LIL_L, LIL_D))
    fx_for(m, 1.2)
    gust(m, 1.6, 1.2)
    A, info = bird("plumquail", m, walk=0.55, stride=36, k=1.2)
    write("pidgeotto", "PLUMQUAIL", m, A, info, 1.48)


def regalquail():
    """Grand: a long sweeping train of tail feathers with eye-spots, a crown of three teardrop plumes, silver-lilac
    feathers, wings tipped gold, and a proud long neck."""
    m = Model()
    LIL, LIL_L, LIL_D, CREAM, SCALE, PLUME, GOLD = "#A49AC0", "#C8C0DA", "#6E6292", "#F4E6CA", "#DCC6A0", "#2A2238", "#E8B848"
    body_p, head_p, feather_p = quail_paints(LIL, LIL_L, LIL_D, CREAM, SCALE, "#2E2640")
    body = Mat(LIL, body_p, noise=0.025)
    head = Mat(LIL, head_p, noise=0.02)
    neck = Mat(LIL, lambda a: mix(LIL_D, LIL, a.ly) if a.face != "front" else (rgb(CREAM) if a.row % 3 else rgb(SCALE)), noise=0.02)
    beak = Mat(GOLD, lambda a: rgb("#B88A28") if a.face == "bottom" else None, noise=0.02, edge=0)
    plume = Mat(PLUME, lambda a: mix(PLUME, "#5A4A80", a.ly * 0.7), noise=0.02, edge=0)

    def wingf(at):
        c = feather_p(at)
        t = (at.z - at.cube.origin[2]) / max(1, at.cube.size[2])
        if t > 0.7:
            c = mix(c, GOLD, (t - 0.7) * 2.5)
        return c

    fea = Mat(LIL, wingf, noise=0.02)

    def train(at):
        # A long feather on its edge: barred lilac, an eye-spot near its end (a gold ring round dark violet), the end rounded.
        c = at.cube
        t = (at.z - c.origin[2]) / max(1, c.size[2])
        v = 1 - at.ly
        col = mix(LIL_D, LIL, 0.4 + 0.5 * v)
        if int(math.floor(at.z * 0.5)) % 2 == 0:
            col = mix(col, LIL_L, 0.3)
        width = 0.35 + 0.65 * min(1, t * 2.2)
        if v > width + 0.02:
            return CLEAR
        d = math.hypot((t - 0.8) * c.size[2] / max(1, c.size[1]) * 1.1, (v - 0.5) * 2)
        if d < 0.95:
            col = rgb(GOLD) if d > 0.6 else rgb("#3A2A60") if d > 0.28 else rgb("#8A7AC0")
        if t > 0.97 or (t > 0.9 and abs(v - 0.5) * 2 > (1 - t) / 0.1):
            return CLEAR
        if abs(v - 0.5) < 0.06:
            col = mix(col, "#FFFFFF", 0.2)
        return col

    trainm = Mat(LIL, train, noise=0.015, edge=0)
    legm = Mat("#E8A050", lambda a: rgb("#C47A30") if a.face == "bottom" else None, noise=0.02, edge=0)
    fluff = Mat(CREAM, lambda a: rgb(SCALE) if a.face == "front" and a.row == a.fh - 1 else None, noise=0.02, edge=0)
    crown = Mat(GOLD, lambda a: mix("#B88A28", "#FFE08A", a.ly), noise=0.02, edge=0)

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, 15, 0], blob([-7, 9, -9], [14, 12, 16], body, b=2) + blob([-5.5, 10, -10.5], [11, 9, 3], body) +
           [Cube([-4, 9, -11], [8, 2, 2], fluff), Cube([-3, 8, -9], [6, 2, 3], fluff)])
    m.bone("neck", "body", [0, 19, -7], [Cube([-2.5, 18, -9.5], [5, 9, 5], neck)], rotation=[-12, 0, 0])
    m.bone("head", "neck", [0, 27, -7.5], face_blob([-4.5, 26, -12.5], [9, 8, 8], head))
    m.bone("beak", "head", [0, 29.5, -12.5], [Cube([-1, 28.5, -15], [2, 2, 3], beak), Cube([-0.5, 28, -16], [1, 1, 1], beak)])
    m.bone("beak_lower", "head", [0, 28.5, -12.5], [Cube([-1, 27.6, -14.5], [2, 1, 2], Mat("#C89A38", noise=0.01, edge=0))])
    m.bone("crown", "head", [0, 34, -9], [Cube([-2.5, 33.5, -11], [5, 1, 4], crown)])
    m.bone("plume", "crown", [0, 34.5, -9], [Cube([-0.5, 34.5, -9.5], [1, 5, 1], plume), Cube([-2.5, 34.5, -9], [1, 4, 1], plume, [0, 0, 20], [-2, 34.5, -8.5]),
                                              Cube([1.5, 34.5, -9], [1, 4, 1], plume, [0, 0, -20], [2, 34.5, -8.5])], rotation=[-10, 0, 0])
    m.bone("plume_tip", "plume", [0, 39.5, -9], [Cube([-1, 38.5, -12], [2, 3, 3], plume), Cube([-4, 37.5, -11], [1, 2, 2], plume, [0, 0, 20], [-2, 38.5, -9]),
                                                  Cube([3, 37.5, -11], [1, 2, 2], plume, [0, 0, -20], [2, 38.5, -9])], rotation=[30, 0, 0])
    m.eyes("head", 4.5, 29, -12.5, h=3, corner=(1, 3), half=4.5, iris="#7A4A1A", pupil="#0C0A10", skin=LIL_D, iris_light="#F0C050",
           grid=["DLLL", "WPPI", ".JIE"])
    sec = Mat(LIL, feather_plane(LIL, LIL_D, GOLD, bars=LIL_L), noise=0.015, edge=0)
    pri = Mat(LIL_D, feather_plane(LIL_D, LIL, GOLD, edge="#FFE08A"), noise=0.015, edge=0)
    folded_wing(m, 7, 20, -8, 15, 6, 2, [(0, 3, 6), (2.5, 3, 6), (5, 3, 6), (7.5, 3, 6), (10, 3, 6)], [(0, 3, 9), (2.5, 3, 9), (5, 3, 8), (7.5, 3, 7)],
                fea, sec, pri)
    covm = Mat(LIL, feather_plane(LIL, LIL_D, GOLD, bars=LIL_L), noise=0.015, edge=0)
    m.bone("tail", "body", [0, 18, 6], [Cube([-3, 15, 5], [6, 4, 5], body)] +
           [Cube([-1.25, 18, 6], [2.5, 0, 9], covm, [0, ry, 0], [0, 18, 6]) for ry in (-40, -20, 0, 20, 40)], rotation=[-20, 0, 0])
    # The train: long feathers on their edges, fanned a little, sweeping back and drooping to the ground.
    m.bone("tail2", "tail", [0, 17, 9], [Cube([x, 4, 9], [0, 13, 26 - abs(x) * 0.8], trainm, [0, x * 5, 0], [x, 17, 9]) for x in (-5, -2.5, 0, 2.5, 5)],
           rotation=[12, 0, 0])
    m.bone("tail3", "tail2", [0, 17, 9])
    m.bone("ruff", "neck", [0, 20, -7], [Cube([-3.5, 18, -10.5], [7, 2, 7], Mat(LIL_L, feather_plane(LIL_L, LIL, GOLD), noise=0.015, edge=0)),
                                         Cube([-3, 20, -10], [6, 1, 6], Mat(LIL_L, noise=0.02, edge=0))])
    quail_feet(m, 3, 9, 7, legm, 1.6, trouser=trousers(LIL, LIL_L, LIL_D))
    # Bushy feathered brows sweeping back over its eyes: a hunter's glare.
    bushy = Mat("#F6F2FC", lambda a: rgb("#D4CAE6") if (a.col + 2 * a.row) % 3 == 0 else (rgb("#B6AACC") if a.face == "bottom" else None),
                noise=0.02, edge=0)
    m.brows("head", [Cube([-5.5, 31.6, -13], [4, 2, 6], bushy, [10, 0, 16], [-1.5, 32.6, -13]),
                     Cube([-5.75, 32.6, -8], [2, 1, 3], bushy, [18, 0, 8], [-4.75, 33.1, -8])])
    fx_for(m, 1.7)
    gust(m, 1.92, 1.7)
    A, info = bird("regalquail", m, walk=0.65, stride=30, k=1.6, slow=1.2, flap=0.5, extras={
        "idle": {"tail2": track(rotation=[wave(4.8, 3), wave(2.4, 4), 0])},
        "fly": {"tail2": track(rotation=[wave(0.5, 4, 0, -5), 0, 0])}})
    write("pidgeot", "REGALQUAIL", m, A, info, 2.24)


# ======================================================================================================
# The jerboas: JERBIT > JERBOLT
# ======================================================================================================

def jerboa(sid, name, stands, big=False):
    m = Model()
    if big:
        SAND, SAND_L, SAND_D, CREAM, PINK, STRIPE = "#C9934E", "#E0B06C", "#A2733A", "#F4E2C2", "#EB98A8", "#6A4626"
    else:
        SAND, SAND_L, SAND_D, CREAM, PINK, STRIPE = "#D9A764", "#E8C083", "#B7864A", "#F7E8CB", "#EFA2AE", None
    s = 1.45 if big else 1.0

    def body_p(at):
        if at.face == "bottom":
            return rgb(CREAM)
        if at.face == "front":
            return rgb(CREAM) if at.ly < 0.6 else rgb(SAND)
        if at.side and at.ly < 0.3:
            return mix(SAND, CREAM, 0.7)
        if STRIPE and at.face == "top" and abs(at.x) < 0.8:
            return rgb(STRIPE)
        return mix(SAND_D, SAND, 0.5 + 0.5 * at.ly) if at.face != "top" else rgb(SAND_D)

    def head_p(at):
        if at.face == "bottom":
            return rgb(CREAM)
        if at.face == "front" and at.ly < 0.4:
            return rgb(CREAM)
        if STRIPE and at.face == "top" and abs(at.x) < 0.8:
            return rgb(STRIPE)
        if at.face == "top":
            return rgb(SAND_D)
        return mix(SAND_D, SAND, 0.4 + 0.6 * at.ly)

    body = Mat(SAND, body_p, noise=0.025)
    head = Mat(SAND, head_p, noise=0.025)
    snout = Mat(CREAM, lambda a: rgb("#8A6A4A") if a.side and a.row == 1 else
                (rgb("#6A4A3A") if a.face == "front" and a.row == a.fh - 1 and a.col == a.fw // 2 else None), noise=0.02)
    tooth = Mat("#FFFDF4", lambda a: rgb("#E8E0CC") if a.face == "bottom" else None, noise=0.01, edge=0)
    nose = Mat(PINK, noise=0.01, edge=0)

    def ear_p(at):
        if at.face == "front":
            if at.row == 0 or at.col in (0, at.fw - 1):
                return rgb(SAND)
            return mix(PINK, "#F8C8D0", 1 - at.ly)
        return mix(SAND_D, SAND, at.ly)

    ear = Mat(SAND, ear_p, noise=0.02)
    paw = Mat(CREAM, noise=0.02, edge=0)
    thigh = Mat(SAND, lambda a: rgb(CREAM) if a.face == "bottom" or (a.face == "front" and a.ly < 0.4) else None, noise=0.025)
    foot = Mat(CREAM, lambda a: rgb("#E7C9A6") if a.face == "bottom" else (rgb("#C9A988") if a.face == "front" and a.col % 2 == 1 else None), noise=0.02)
    tailm = Mat(SAND, lambda a: rgb(SAND_D) if a.face == "top" else None, noise=0.02, edge=0)

    def tuft_p(at):
        t = at.lz
        if at.face == "back" or t > 0.62:
            return rgb("#FFFFFF")
        if big and 0.3 < t < 0.45:
            return rgb("#FFFFFF")
        return rgb("#2E2420")

    tuft = Mat("#2E2420", tuft_p, noise=0.02, edge=0)

    def brush_p(at):
        # A brush of long fur: thin at the tail, full in the middle, drawn in to a soft point, its edges ragged with hairs.
        c = at.cube
        u = at.lx if c.size[1] == 0 else at.ly
        t = at.lz
        hw = 0.5 * math.sin(math.pi * min(1.0, max(0.0, t)) ** 0.75)
        hair = hash01("hair", round(u * 12), round(t * 10))
        if abs(u - 0.5) > hw * (0.75 + 0.25 * hair):
            return CLEAR
        if t > 0.62 or (big and 0.3 < t < 0.45):
            return mix("#FFFFFF", "#E4DCD4", hair * 0.6)
        return mix("#2E2420", "#4A3C34", hair * 0.7)

    brush = Mat("#2E2420", brush_p, noise=0.0, edge=0)
    whisk = Mat("#3A2E28", lambda a: None if (a.col + a.row) % 2 == 0 else CLEAR, noise=0.0, edge=0)
    cheek = Mat(CREAM, lambda a: rgb("#FFF4E0") if a.face == "top" else None, noise=0.02, edge=0)

    def S(v):
        return [x * s for x in v]

    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", S([0, 5, 0]), blob(S([-3, 3, -3.5]), S([6, 6, 7]), body))
    m.bone("head", "body", S([0, 8.5, -3]), face_blob(S([-3, 7, -7.5]), S([6, 5, 5]), head) + [Cube([-0.5 * s, 7 * s - 1, -9 * s], [1, 1, 1], tooth)] +
           [Cube(S([-1.5, 7, -9]), S([3, 2, 2]), snout), Cube(S([-0.5, 8.5, -9.5]), [1, 1, 1], nose),
            Cube(S([-3.75, 7.25, -7]), S([1, 2, 2]), cheek), Cube(S([2.75, 7.25, -7]), S([1, 2, 2]), cheek)])
    m.bone("snout", "head", S([0, 8, -9]))
    m.bone("jaw", "head", S([0, 7.5, -6]), [Cube(S([-1, 6.4, -8.75]), [2 * s, 1, 3 * s], snout)])
    m.pair("whiskers_right", "head", S([-1.5, 8, -8.5]), [Cube(S([-4.5, 8, -9]), [3 * s, 0, 2], whisk)], rotation=[0, -15, 0])
    m.eyes("head", 3 * s, 8.5 * s, -7.5 * s, h=3, corner=(1, 2 if not big else 3), half=3 * s, iris="#3A2A20", pupil="#080606", skin=SAND,
           iris_light="#9A7A5A" if not big else "#B0402A", grid=["LLL", "WPP", ".PJ"] if not big else ["SDLL", "LPPW", ".PPJ"])
    ear_rot = [8, 0, -14] if not big else [35, 0, -20]
    m.pair("ear_right", "head", S([-1.75, 12, -5]), [Cube(S([-3.25, 12, -5.5]), [3 * s, 6 * s, 1], ear)], rotation=ear_rot)
    m.pair("arm_right", "body", S([-2, 6.5, -3.5]), [Cube(S([-2.5, 4.5, -4.25]), [1, 2 * s, 1], paw)], rotation=[-25, 0, 0])
    m.pair("leg_right", "body", S([-2.5, 6, 0.5]), [Cube(S([-4, 2.5, -1.5]), [2 * s, 4 * s, 4 * s], thigh)])
    # Long hopping feet: the sole, then three long toes splayed a little, each tipped with a claw.
    claw = talon_mat("#4A3426" if big else "#B89A80", "#D8C4AE" if big else "#F4E6D4")
    cx = -3.25 * s
    tl = round(1.5 * s) + 1
    toes = []
    for d in (-1, 0, 1):
        toes += [Cube([cx - 0.5 + d, 0, -3 * s - tl], [1, 1, tl], foot, [0, d * 12, 0], [cx + d, 0.5, -3 * s]),
                 Cube([cx - 0.5 + d, 0, -3 * s - tl - 1], [1, 1, 1], claw, [0, d * 12, 0], [cx + d, 0.5, -3 * s])]
    m.pair("foot_right", "leg_right", S([-3, 2.5, 1]), [Cube(S([-3.75, 0, -3.5]), [1 * s, 2.5 * s, 2 * s], thigh),
                                                       Cube([cx - 1, 0, -3 * s], [2, 1, round(3.5 * s)], foot)] + toes)
    m.bone("tail", "body", S([0, 5.5, 3.5]), [Cube(S([-0.5, 5, 3.5]), [1, 1, 4 * s], tailm)], rotation=[-12, 0, 0])
    m.bone("tail2", "tail", S([0, 5.5, 7.5]), [Cube(S([-0.5, 5, 7.5]), [1, 1, 4 * s], tailm)], rotation=[15, 0, 0])
    bw, bl = round(5 * s), round(7 * s)
    m.bone("tail3", "tail2", S([0, 5.5, 11.5]), [Cube(S([-0.5, 5, 11.5]), [1, 1, 3 * s], tailm), Cube(S([-1, 4.5, 14]), S([2, 2, 3]), tuft)] +
           [Cube([-bw / 2, 5.5 * s, 13.5 * s], [bw, 0, bl], brush, [0, 0, a], [0, 5.5 * s, 13.5 * s]) for a in (0, 45, 90, 135)],
           rotation=[10, 0, 0])
    if big:
        # Grown: a ruff of pale fur at the neck, a dark stripe down its back, claws on its long feet, and a second white band in its tuft.
        ruff = Mat(CREAM, lambda a: rgb("#FFF6E6") if a.face == "top" else (rgb("#E6CFA8") if a.row == a.fh - 1 else None), noise=0.02, edge=0)
        m.bone("ruff", "body", S([0, 8, -3]), [Cube(S([-3.5, 7.5, -4.5]), S([7, 2, 3]), ruff), Cube(S([-4, 8, -3.5]), S([1, 2, 2]), ruff),
                                               Cube(S([3, 8, -3.5]), S([1, 2, 2]), ruff)])
        m.brows("head", [Cube([-3 * s - 0.5, 11.2 * s - 0.6, -7.5 * s - 0.4], [round(2 * s) + 1, 1, 2], Mat(STRIPE, noise=0.02, edge=0), [8, 0, 24],
                              [-0.5 * s, 11.2 * s, -7.5 * s])])
    hyper_fang(m, 1.6, s, k=s)
    A, info = hopper(sid.replace("rattata", "jerbit").replace("raticate", "jerbolt"), m, hop=0.45 if not big else 0.5, height=2.0, k=s)
    write(sid, name, m, A, info, stands)


def main():
    only = set(sys.argv[1:])
    for f in (fernadillo, brambadillo, grovadon, cinderkid, emberam, pyribex, tidapir, surfapir, tsunapir, quailet, plumquail, regalquail):
        if not only or f.__name__ in only:
            f()
    if not only or "jerbit" in only:
        jerboa("rattata", "JERBIT", 0.6)
    if not only or "jerbolt" in only:
        jerboa("raticate", "JERBOLT", 1.2, big=True)
    names = {}
    for sid in sorted(os.listdir(OUT_MODELS)):
        p = os.path.join(OUT_MODELS, sid, "info.json")
        if os.path.exists(p):
            names[sid] = json.load(open(p))["name"]
    json.dump(names, open(os.path.join(OUT_MODELS, "names.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
