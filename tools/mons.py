"""
Retromon's own creatures: models, painted textures and animations, made here from scratch (no one else's
models, textures or animations in them). Each stands in for one species when OPTION's POKéMON LOOK is set to
RETROMON: same species underneath (stats, moves, levelling), its own look and name.

Writes, for each:
  assets/retromon/mons/<species>/model.geo.json   Bedrock geometry (as BedrockModel.java reads it)
  assets/retromon/mons/<species>/anim.json        its animations (idle, battle idle, walk, cry, recoil; fly and hover for birds)
  assets/retromon/mons/<species>/info.json        scale, which animations are which, its texture
  assets/retromon/textures/mons/<species>.png     its texture (and <species>_glow_<n>.png, the glowing parts, if it has any)
  assets/retromon/mons/names.json                 the names they go by

Conventions (Bedrock's): y up, units of 1/16 of a block, feet at y = 0, the face towards -z, the creature's
right at -x. Textures are painted in 3D: every texel is worked out from where it sits on the model, so
patterns run across cubes and eyes land where they're placed.

Run: python3 tools/mons.py
"""

import json, math, os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon")
OUT_MODELS = os.path.join(ROOT, "mons")
OUT_TEX = os.path.join(ROOT, "textures", "mons")


# ---------- colours ----------

def rgb(h):
    h = h.lstrip("#")
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], float)


def mix(a, b, t):
    return a + (b - a) * max(0.0, min(1.0, t))


def hash01(*k):
    """A steady pseudo-random number in [0, 1) for these keys (the same every run)."""
    h = 2166136261
    for v in k:
        for ch in str(v):
            h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return (h & 0xFFFFFF) / float(0x1000000)


# ---------- the model ----------

class Mat:
    """
    A cube's paint: a base colour and layers drawn over it, each a function of the texel's spot (its point on the
    model, the face it's on, and the cube) giving a colour or None. glow: the same for the glowing layer (or None).
    """

    def __init__(self, base, *layers, glow=None, shade=1.0, noise=0.035, flat=False):
        self.base = rgb(base) if isinstance(base, str) else base
        self.layers = layers
        self.glow = glow
        self.shade = shade
        self.noise = noise
        self.flat = flat

    def colour(self, at):
        c = self.base
        for f in self.layers:
            r = f(at)
            if r is not None:
                c = rgb(r) if isinstance(r, str) else r
        return c


class At:
    """Where a texel is: its point (x, y, z), the face, the cube, and its spot on the cube (0 to 1 each way)."""

    __slots__ = ("x", "y", "z", "face", "cube", "lx", "ly", "lz", "col", "row")

    def __init__(self, x, y, z, face, cube, col, row):
        self.x, self.y, self.z, self.face, self.cube = x, y, z, face, cube
        o, s = cube.origin, cube.size
        self.lx = (x - o[0]) / s[0] if s[0] else 0.5
        self.ly = (y - o[1]) / s[1] if s[1] else 0.5
        self.lz = (z - o[2]) / s[2] if s[2] else 0.5
        self.col, self.row = col, row


class Cube:
    def __init__(self, origin, size, mat, rotation=None, pivot=None):
        self.origin = [float(v) for v in origin]
        self.size = [int(v) for v in size]
        self.mat = mat
        self.rotation = rotation
        self.pivot = pivot
        self.uv = None


class Bone:
    def __init__(self, name, parent, pivot, cubes=(), rotation=None):
        self.name, self.parent, self.pivot, self.cubes, self.rotation = name, parent, [float(v) for v in pivot], list(cubes), rotation


def mirror_cube(c):
    o, s = c.origin, c.size
    piv = None if c.pivot is None else [-c.pivot[0], c.pivot[1], c.pivot[2]]
    rot = None if c.rotation is None else [c.rotation[0], -c.rotation[1], -c.rotation[2]]
    return Cube([-(o[0] + s[0]), o[1], o[2]], s, c.mat, rot, piv)


def pair(right, parent, pivot, cubes, rotation=None, left=None):
    """A part on the right (-x) and its mirror on the left."""
    left = left or right.replace("right", "left")
    rot_l = None if rotation is None else [rotation[0], -rotation[1], -rotation[2]]
    return [Bone(right, parent, pivot, cubes, rotation),
            Bone(left, parent.replace("right", "left") if parent else parent, [-pivot[0], pivot[1], pivot[2]], [mirror_cube(c) for c in cubes], rot_l)]


# ---------- painting ----------

FACES = ("top", "bottom", "px", "front", "nx", "back")


def face_rect(c):
    """Each face's texels: (face, u, v, width, height) for Minecraft's box UV."""
    w, h, d = c.size
    u, v = c.uv
    return [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("px", u, v + d, d, h), ("front", u + d, v + d, w, h),
            ("nx", u + d + w, v + d, d, h), ("back", u + 2 * d + w, v + d, w, h)]


def texel_point(c, face, col, row):
    x0, y0, z0 = c.origin
    w, h, d = c.size
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    if face == "top":
        return x1 - (col + .5), y1, z1 - (row + .5)
    if face == "bottom":
        return x1 - (col + .5), y0, z1 - (row + .5)
    if face == "px":
        return x1, y1 - (row + .5), z1 - (col + .5)
    if face == "front":
        return x1 - (col + .5), y1 - (row + .5), z0
    if face == "nx":
        return x0, y1 - (row + .5), z0 + (col + .5)
    return x0 + (col + .5), y1 - (row + .5), z1


def shade_for(face, at, c):
    if c.mat.flat:
        return 1.0
    if face == "top":
        k = 1.08
    elif face == "bottom":
        k = 0.8
    else:
        k = 0.9 + 0.14 * at.ly
        if at.row == 0:
            k *= 1.04
        if c.size[1] > 2 and at.row == c.size[1] - 1:
            k *= 0.93
    return 1 + (k - 1) * c.mat.shade


def pack(cubes):
    """Places each cube's texels on the texture (shelves, tallest first). Returns the texture's size."""
    W = 64
    while True:
        x = y = shelf = 0
        ok = True
        for c in sorted(cubes, key=lambda c: -(c.size[2] + c.size[1])):
            w, h, d = c.size
            cw, ch = max(1, 2 * (w + d)), max(1, d + h)
            if cw > W:
                ok = False
                break
            if x + cw > W:
                x, y = 0, y + shelf
                shelf = 0
            c.uv = [x, y]
            x += cw
            shelf = max(shelf, ch)
        H = y + shelf
        if ok and H <= W * 2:
            break
        W *= 2
    Hp = 32
    while Hp < H:
        Hp *= 2
    return W, Hp


def paint(cubes, tex_w, tex_h, frames=4):
    """The texture, and the glowing layer's frames (or None)."""
    img = np.zeros((tex_h, tex_w, 4), np.uint8)
    glows = [np.zeros((tex_h, tex_w, 4), np.uint8) for _ in range(frames)]
    any_glow = False
    for ci, c in enumerate(cubes):
        for face, u, v, fw, fh in face_rect(c):
            for row in range(fh):
                for col in range(fw):
                    x, y, z = texel_point(c, face, col, row)
                    at = At(x, y, z, face, c, col, row)
                    base = c.mat.colour(at)
                    if base is None:
                        continue
                    k = shade_for(face, at, c)
                    n = (hash01(ci, face, col, row) * 2 - 1) * c.mat.noise * 255
                    col_rgb = np.clip(base * k + n, 0, 255)
                    img[v + row, u + col, :3] = col_rgb.astype(np.uint8)
                    img[v + row, u + col, 3] = 255
                    if c.mat.glow is not None:
                        g = c.mat.glow(at)
                        if g is not None:
                            any_glow = True
                            g = rgb(g) if isinstance(g, str) else g
                            for fi in range(frames):
                                flick = 0.78 + 0.22 * math.sin(fi / frames * 2 * math.pi + hash01(ci, col, row) * 6.3)
                                gg = np.clip(g * (0.9 + 0.1 * flick), 0, 255)
                                glows[fi][v + row, u + col, :3] = gg.astype(np.uint8)
                                glows[fi][v + row, u + col, 3] = int(255 * min(1, 0.55 + 0.45 * flick))
    return img, (glows if any_glow else None)


# ---------- animations (Molang) ----------

def num(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def wave(period, amp, phase=0.0, base=0.0):
    """A smooth back and forth: amp each way, once every period seconds, from phase (degrees)."""
    if amp == 0:
        return base
    e = f"math.sin(q.anim_time * {num(360 / period)} + {num(phase)}) * {num(amp)}"
    return e if base == 0 else f"{num(base)} + {e}"


def bounce(period, amp, phase=0.0, base=0.0):
    """Up and back down each half period (a hop or a step's lift)."""
    e = f"math.abs(math.sin(q.anim_time * {num(180 / period)} + {num(phase)})) * {num(amp)}"
    return e if base == 0 else f"{num(base)} + {e}"


def blink(at, length=0.14):
    """An eyelid's height: shut for a moment at `at` seconds into the loop, open otherwise."""
    return f"(q.anim_time > {num(at)}) * (q.anim_time < {num(at + length)})"


def keys(points):
    return {num(t): v for t, v in sorted(points.items())}


class Anims:
    def __init__(self, sid):
        self.sid = sid
        self.all = {}

    def add(self, name, length, bones, loop=True):
        self.all[f"animation.{self.sid}.{name}"] = {"loop": loop, "animation_length": length, "bones": bones}
        return f"animation.{self.sid}.{name}"


def track(**kw):
    out = {}
    for k in ("rotation", "position", "scale"):
        if k in kw and kw[k] is not None:
            out[k] = kw[k]
    return out


def merge(*dicts):
    out = {}
    for d in dicts:
        for bone, chans in d.items():
            out.setdefault(bone, {}).update(chans)
    return out


def eyelids(at, period_shift=0.0):
    return {"eyelid_right": track(scale=[1, blink(at + period_shift), 1]), "eyelid_left": track(scale=[1, blink(at + period_shift), 1])}


# ---------- writing ----------

def write(sid, name, bones, anims, info, stands):
    cubes = [c for b in bones for c in b.cubes]
    tw, th = pack(cubes)
    img, glows = paint(cubes, tw, th)
    os.makedirs(os.path.join(OUT_MODELS, sid), exist_ok=True)
    os.makedirs(OUT_TEX, exist_ok=True)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT_TEX, sid + ".png"))
    layers = []
    if glows:
        frames = []
        for i, gimg in enumerate(glows):
            Image.fromarray(gimg, "RGBA").save(os.path.join(OUT_TEX, f"{sid}_glow_{i}.png"))
            frames.append(f"retromon:textures/mons/{sid}_glow_{i}.png")
        layers.append({"frames": frames, "fps": 8, "emissive": True, "translucent": True})
    top = 0.0
    out_bones = []
    for b in bones:
        o = {"name": b.name, "pivot": b.pivot}
        if b.parent:
            o["parent"] = b.parent
        if b.rotation:
            o["rotation"] = b.rotation
        cs = []
        for c in b.cubes:
            co = {"origin": c.origin, "size": c.size, "uv": c.uv}
            if c.rotation:
                co["rotation"] = c.rotation
                co["pivot"] = c.pivot or b.pivot
            cs.append(co)
            top = max(top, c.origin[1] + c.size[1])
        if cs:
            o["cubes"] = cs
        out_bones.append(o)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.retromon.{sid}", "texture_width": tw, "texture_height": th, "visible_bounds_width": 3,
                        "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
        "bones": out_bones}]}
    json.dump(geo, open(os.path.join(OUT_MODELS, sid, "model.geo.json"), "w"), indent=1)
    json.dump({"format_version": "1.8.0", "animations": anims.all}, open(os.path.join(OUT_MODELS, sid, "anim.json"), "w"), indent=1)
    height = round(top / 16, 3)
    info = dict(info)
    # How tall it stands in the world, in blocks (about the size of the one it stands in for).
    info["scale"] = round(stands / height, 3)
    info["height"] = height
    info["texture"] = f"retromon:textures/mons/{sid}.png"
    info["layers"] = layers
    info["name"] = name
    info.setdefault("parts", [])
    json.dump(info, open(os.path.join(OUT_MODELS, sid, "info.json"), "w"), indent=1)
    print(f"{sid:11s} {name:11s} {len(cubes):3d} cubes  texture {tw}x{th}  height {height}  scale {info['scale']}")


# ---------- shared bits ----------

def eye_mat(iris, pupil="#151820", shine="#FFFFFF", rim=None):
    """A big glossy eye (2 wide, 2 or 3 tall): the pupil low and to the inside, a highlight at the top towards the
    middle of the face, the iris round them."""
    I, P, S = rgb(iris), rgb(pupil), rgb(shine)
    R = rgb(rim) if rim else None

    def f(at):
        if at.face != "front":
            return None
        w, h = at.cube.size[0], at.cube.size[1]
        cx, ry = at.col, at.row
        # Which column is nearer the middle of the face: the eye's inner side.
        inner = 0 if at.cube.origin[0] + w / 2 > 0 else w - 1
        if ry == 0 and cx == inner:
            return S
        if ry >= h - 2 and cx != inner:
            return P
        if R is not None and ry == h - 1:
            return R
        return I

    return Mat(iris, f, noise=0.0, shade=0.15)


def lid_mat(skin):
    return Mat(skin, noise=0.02, shade=0.3)


def eye_pair(head, x_inner, y, z, w=2, h=2, iris="#2A1E1A", pupil="#100C0A", skin="#888888", shine="#FFFFFF", rim=None):
    """Eyes on the face (the right one at -x), each with an eyelid that drops shut for a blink."""
    bones = []
    em = eye_mat(iris, pupil, shine, rim)
    lm = lid_mat(skin)
    xr = -(x_inner + w)
    bones += pair("eye_right", head, [xr + w / 2, y + h, z], [Cube([xr, y, z - 0.01], [w, h, 0], em)])
    # The lid hangs from the eye's top edge: its height (scale y) is 0 when open, 1 when shut.
    bones += pair("eyelid_right", "eye_right", [xr + w / 2, y + h, z], [Cube([xr, y, z - 0.04], [w, h, 0], lm)])
    for b in bones:
        if b.name == "eyelid_left":
            b.parent = "eye_left"
    return bones


# ======================================================================================================
# FERNADILLO (the grass starter's place): a little armadillo whose armour is overlapping leaves. It walks on
# four short legs, its tail ends in a curled fern frond, and when it's startled it tucks its head in.
# ======================================================================================================

def fernadillo():
    sid = "bulbasaur"
    SKIN, SKIN_D, CREAM = "#D8B183", "#B98D60", "#F1E1BC"
    LEAF, LEAF_L, LEAF_D, VEIN = rgb("#4FA64A"), rgb("#8AD06A"), rgb("#2E6F35"), rgb("#B8E68C")
    PINK, CLAW = "#E79A98", "#4B3326"

    def leaf_plate(at):
        # Each band is a row of leaves: lighter towards the tip (the band's lower edge on the sides, its front on top),
        # a pale midrib down each leaf, and a dark notch between leaves.
        c = at.cube
        if at.face in ("px", "nx") and at.z < c.origin[2] + 0.6:
            return LEAF_D
        if at.face == "top" and at.z < c.origin[2] + 0.6:
            return LEAF_D
        if at.face in ("px", "nx", "front", "back"):
            across = at.z if at.face in ("px", "nx") else at.x
            t = 1 - at.ly
            col = mix(LEAF, LEAF_L, t * 0.8)
            k = (across * 0.5) % 1.0
            if abs(k - 0.5) < 0.12 and at.ly < 0.9:
                col = mix(col, VEIN, 0.7)
            if at.row == c.size[1] - 1:
                # The leaf tips: a scalloped edge, alternately light and dark.
                col = LEAF_L if int(math.floor(across)) % 2 == 0 else LEAF_D
            if at.row == 0:
                col = mix(col, LEAF_D, 0.5)
            return col
        if at.face == "top":
            t = at.lz
            col = mix(LEAF, LEAF_L, 0.25 + 0.4 * t)
            if abs(((at.x * 0.5) % 1.0) - 0.5) < 0.12:
                col = mix(col, VEIN, 0.6)
            if at.row == c.size[2] - 1:
                col = LEAF_L
            return col
        return LEAF_D

    leaf = Mat(LEAF, leaf_plate, noise=0.03)
    crest = Mat("#7CCB5A", lambda a: "#B8E68C" if a.face == "top" and a.col % 2 == 0 else None, noise=0.03)

    def belly(at):
        if at.face == "bottom" or at.y < 4.2:
            return CREAM
        return None

    skin = Mat(SKIN, belly, noise=0.03)

    def head_paint(at):
        if at.face == "bottom":
            return CREAM
        if at.face == "front" and at.y < 6.2:
            return "#E6C69A"
        return None

    head = Mat(SKIN, head_paint, noise=0.03)
    snout = Mat("#E6C69A", lambda a: "#5A3A2C" if a.face == "front" and a.row == 0 and 0 < a.col < a.cube.size[0] - 1 else None, noise=0.02)

    def paw(at):
        if at.face == "front" and at.row == at.cube.size[1] - 1 and at.col % 2 == 0:
            return CLAW
        if at.face == "bottom":
            return SKIN_D
        return None

    legm = Mat(SKIN, paw, noise=0.03)
    ear = Mat(SKIN, lambda a: PINK if a.face == "front" and a.row > 0 else None, noise=0.03)

    def frond(at):
        t = at.ly if at.face != "top" else 1
        col = mix(LEAF_D, LEAF_L, t)
        if at.face in ("px", "nx") and at.col % 2 == 1:
            col = mix(col, VEIN, 0.4)
        return col

    fern = Mat(LEAF, frond, noise=0.03)

    bones = [Bone("root", None, [0, 0, 0]), Bone("body", "root", [0, 6, 0], [
        Cube([-4, 2, -5], [8, 6, 10], skin),
    ])]
    bones.append(Bone("shell", "body", [0, 7, 0], [
        Cube([-5, 5, -5.5], [10, 5, 3], leaf),
        Cube([-5.5, 5.5, -3], [11, 5, 3], leaf),
        Cube([-5.5, 5.5, -0.25], [11, 5, 3], leaf),
        Cube([-5, 5, 2.5], [10, 5, 3], leaf),
        Cube([-4, 4.5, 5.25], [8, 4, 1], leaf),
    ]))
    bones.append(Bone("crest", "shell", [0, 10.5, 0], [
        Cube([-1, 10.5, -4.5], [2, 1, 3], crest), Cube([-1, 10.5, -1.5], [2, 1, 3], crest), Cube([-1, 10.5, 1.5], [2, 1, 3], crest),
    ]))
    bones.append(Bone("head", "body", [0, 6, -5], [
        Cube([-3, 3.5, -10], [6, 5, 5], head),
        Cube([-1.5, 3.5, -12], [3, 2, 2], snout),
        Cube([-3.5, 8, -9], [7, 1, 4], leaf),
    ]))
    bones += pair("ear_right", "head", [-2.5, 9, -6.5], [Cube([-3.5, 9, -7], [2, 3, 1], ear)], rotation=[0, 0, -18])
    bones += eye_pair("head", x_inner=1, y=5.5, z=-10, w=2, h=2, iris="#3A2A20", pupil="#120D0A", skin=SKIN)
    bones += pair("leg_front_right", "body", [-3, 3.5, -3], [Cube([-4.5, 0, -4.5], [3, 4, 3], legm)])
    bones += pair("leg_back_right", "body", [-3, 3.5, 3], [Cube([-4.5, 0, 1.5], [3, 4, 3], legm)])
    bones.append(Bone("tail", "body", [0, 5, 5], [Cube([-1, 3.5, 5], [2, 2, 3], skin)], rotation=[20, 0, 0]))
    bones.append(Bone("tail_tip", "tail", [0, 4.5, 8], [Cube([-1.5, 3.5, 7.5], [3, 3, 3], fern)], rotation=[25, 0, 0]))

    A = Anims("fernadillo")
    legs4 = lambda period, amp: {
        "leg_front_right": track(rotation=[wave(period, amp), 0, 0]), "leg_back_left": track(rotation=[wave(period, amp), 0, 0]),
        "leg_front_left": track(rotation=[wave(period, -amp), 0, 0]), "leg_back_right": track(rotation=[wave(period, -amp), 0, 0])}
    ground = A.add("ground_idle", 4.0, merge({
        "body": track(position=[0, wave(4, 0.18), 0], scale=[wave(2, 0.012, 0, 1), wave(2, 0.02, 0, 1), 1]),
        "shell": track(rotation=[wave(2, 1.2), 0, 0]),
        "head": track(rotation=[wave(4, 3, 90), wave(4, 9), 0]),
        "ear_right": track(rotation=[0, 0, wave(1, 4, 0)]), "ear_left": track(rotation=[0, 0, wave(1, -4, 30)]),
        "tail": track(rotation=[0, wave(2, 10), 0]), "tail_tip": track(rotation=[wave(2, 6, 60), 0, 0]),
    }, eyelids(3.1)))
    battle = A.add("battle_idle", 2.0, merge({
        "body": track(position=[0, bounce(1, 0.45), 0]),
        "shell": track(rotation=[wave(1, 2), 0, 0]),
        "head": track(rotation=[wave(1, 3, 90, -4), wave(2, 5), 0]),
        "ear_right": track(rotation=[wave(1, 3), 0, wave(0.5, 4)]), "ear_left": track(rotation=[wave(1, 3), 0, wave(0.5, -4)]),
        "tail": track(rotation=[0, wave(0.5, 14), 0]),
        "leg_front_right": track(rotation=[wave(1, 3), 0, 0]), "leg_front_left": track(rotation=[wave(1, 3), 0, 0]),
    }, eyelids(1.4)))
    walk = A.add("ground_walk", 0.8, merge(legs4(0.8, 32), {
        "body": track(position=[0, bounce(0.4, 0.5), 0], rotation=[0, 0, wave(0.8, 2.5)]),
        "head": track(rotation=[wave(0.4, 2.5, 90), wave(0.8, -3), 0]),
        "tail": track(rotation=[0, wave(0.8, 16), 0]), "tail_tip": track(rotation=[wave(0.4, 5), 0, 0]),
        "ear_right": track(rotation=[wave(0.4, 6), 0, 0]), "ear_left": track(rotation=[wave(0.4, 6), 0, 0]),
    }, eyelids(0.5)))
    cry = A.add("cry", 1.2, {
        "head": track(rotation=keys({0: [0, 0, 0], 0.25: [-26, 0, 0], 0.5: [-22, 0, 0], 0.75: [-26, 0, 0], 1.2: [0, 0, 0]})),
        "body": track(position=keys({0: [0, 0, 0], 0.25: [0, 1.2, 0], 0.5: [0, 0.6, 0], 0.75: [0, 1.0, 0], 1.2: [0, 0, 0]})),
        "shell": track(scale=keys({0: [1, 1, 1], 0.25: [1.06, 1.1, 1.06], 0.75: [1.06, 1.1, 1.06], 1.2: [1, 1, 1]})),
        "ear_right": track(rotation=keys({0: [0, 0, 0], 0.25: [-20, 0, -14], 1.0: [-20, 0, -14], 1.2: [0, 0, 0]})),
        "ear_left": track(rotation=keys({0: [0, 0, 0], 0.25: [-20, 0, 14], 1.0: [-20, 0, 14], 1.2: [0, 0, 0]})),
    }, loop=False)
    # Startled: it ducks its head under its leaves.
    recoil = A.add("recoil", 0.6, {
        "head": track(rotation=keys({0: [0, 0, 0], 0.12: [24, 0, 0], 0.6: [0, 0, 0]}), position=keys({0: [0, 0, 0], 0.12: [0, -1, 1.5], 0.6: [0, 0, 0]})),
        "body": track(rotation=keys({0: [0, 0, 0], 0.12: [-8, 0, 0], 0.6: [0, 0, 0]}), position=keys({0: [0, 0, 0], 0.12: [0, 0, 1], 0.6: [0, 0, 0]})),
    }, loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk, "parts": []}}}
    write(sid, "FERNADILLO", bones, A, info, 0.72)


# ======================================================================================================
# CINDERKID (the fire starter's place): a young goat with charcoal wool, glowing ember horns and an ember fringe,
# hooves that glow underneath, and a flame for a tail-tuft. Its eyes are amber with a goat's bar of a pupil.
# ======================================================================================================

def cinderkid():
    sid = "charmander"
    WOOL, WOOL_L, WOOL_D = rgb("#4A4248"), rgb("#6A5F66"), rgb("#2F2A2E")
    CREAM = "#EBDDC6"
    EMBER = [rgb("#8E2414"), rgb("#E04A1C"), rgb("#FF8A2A"), rgb("#FFD45A"), rgb("#FFF2B0")]

    def ember(t):
        t = max(0.0, min(1.0, t)) * (len(EMBER) - 1)
        i = min(len(EMBER) - 2, int(t))
        return mix(EMBER[i], EMBER[i + 1], t - i)

    def woolly(at):
        # Curly wool: clumps of lighter and darker texels.
        h = hash01("w", math.floor(at.x * 1.0), math.floor(at.y * 1.0), math.floor(at.z * 1.0), at.face)
        c = WOOL_L if h > 0.72 else WOOL_D if h < 0.18 else WOOL
        if at.face == "bottom":
            c = mix(c, WOOL_D, 0.4)
        return c

    wool = Mat(WOOL, woolly, noise=0.02)
    bib = Mat(CREAM, lambda a: "#D9C8AE" if hash01("b", a.col, a.row, a.face) > 0.7 else None, noise=0.02)

    def head_paint(at):
        c = woolly(at)
        return c

    head = Mat(WOOL, head_paint, noise=0.02)

    def muzzle_paint(at):
        if at.face == "front":
            w = at.cube.size[0]
            if at.row == 0 and at.col in (0, w - 1):
                return "#3A2C2C"
            if at.row == at.cube.size[1] - 1 and 0 < at.col < w - 1:
                return "#C9A98E"
        if at.face == "top" and at.lz < 0.4:
            return "#F6EAD6"
        return None

    muzzle = Mat(CREAM, muzzle_paint, noise=0.02)

    def horn_colour(at):
        # Dark at the root, ember up the horn, white-hot at the tip.
        c = at.cube
        base_y = c.origin[1]
        return ember((at.y - base_y) / max(1, c.size[1]) * 0.75)

    def horn_tip_colour(at):
        return ember(0.7 + 0.3 * at.ly)

    horn = Mat("#8E2414", horn_colour, glow=lambda a: ember(0.35 + 0.6 * a.ly), noise=0.02, shade=0.4)
    horn_tip = Mat("#FFD45A", horn_tip_colour, glow=lambda a: ember(0.75 + 0.25 * a.ly), noise=0.02, shade=0.3)
    fringe = Mat("#FF8A2A", lambda a: ember(0.45 + 0.5 * a.ly + 0.15 * hash01("f", a.col, a.row)),
                 glow=lambda a: ember(0.5 + 0.5 * a.ly), noise=0.02, shade=0.3)
    flame = Mat("#FF8A2A", lambda a: ember(0.35 + 0.6 * a.ly + 0.1 * hash01("t", a.col, a.row, a.face)),
                glow=lambda a: ember(0.45 + 0.55 * a.ly), noise=0.02, shade=0.3)

    def ear_paint(at):
        if at.face == "bottom":
            return "#E8662C"
        return woolly(at)

    ear = Mat(WOOL, ear_paint, glow=lambda a: rgb("#B83A18") if a.face == "bottom" else None, noise=0.02)
    leg = Mat(WOOL, lambda a: mix(WOOL, WOOL_D, 1 - a.ly) if a.face != "top" else None, noise=0.02)

    def hoof_paint(at):
        if at.face == "bottom" or (at.face != "top" and at.row == at.cube.size[1] - 1):
            return "#FF7A2A"
        if at.face == "front" and at.col == at.cube.size[0] // 2:
            return "#120E10"
        return "#241C20"

    hoof = Mat("#241C20", hoof_paint, glow=lambda a: rgb("#FF8A2A") if a.face == "bottom" or (a.face != "top" and a.row == a.cube.size[1] - 1) else None,
               noise=0.02)

    bones = [Bone("root", None, [0, 0, 0]), Bone("body", "root", [0, 8, 0], [
        Cube([-3, 5.5, -4.5], [6, 5, 9], wool),
        Cube([-2.5, 6, -5.5], [5, 4, 1], bib),
    ])]
    bones.append(Bone("neck", "body", [0, 9.5, -4], [Cube([-1.5, 8.5, -5.5], [3, 3, 2], wool)], rotation=[-12, 0, 0]))
    bones.append(Bone("head", "neck", [0, 11.5, -5.5], [
        Cube([-2.5, 10, -9.5], [5, 5, 5], head),
        Cube([-1.5, 10, -11], [3, 2, 2], muzzle),
        Cube([-0.5, 8.5, -10.5], [1, 2, 1], bib),
    ]))
    bones.append(Bone("fringe", "head", [0, 15, -8], [Cube([-1.5, 14.5, -9], [3, 2, 3], fringe)]))
    bones += pair("horn_right", "head", [-1.5, 15, -7], [Cube([-2.25, 14.5, -7.75], [1, 3, 1], horn)], rotation=[-28, 0, -12])
    bones += pair("horn_tip_right", "horn_right", [-1.75, 17.5, -7.25], [Cube([-2.25, 17.5, -7.75], [1, 2, 1], horn_tip)], rotation=[-40, 0, 0])
    for b in bones:
        if b.name == "horn_tip_left":
            b.parent = "horn_left"
    bones += pair("ear_right", "head", [-2.5, 13, -7], [Cube([-5.5, 12.5, -7.75], [3, 1, 2], ear)], rotation=[0, -15, 42])
    bones += eye_pair("head", x_inner=0.5, y=12.5, z=-9.5, w=2, h=2, iris="#4A2412", pupil="#160A06", skin="#4A4248")
    for side, x in (("right", -1.75), ("left", 1.75)):
        for end, z in (("front", -3), ("back", 3)):
            name = f"leg_{end}_{side}"
            bones.append(Bone(name, "body", [x, 6, z], [Cube([x - 0.75, 2, z - 0.75], [2, 4, 2], leg)]))
            bones.append(Bone(f"hoof_{end}_{side}", name, [x, 2, z], [Cube([x - 1, 0, z - 1], [2, 2, 2], hoof)]))
    bones.append(Bone("tail", "body", [0, 10, 4.5], [Cube([-1, 9.5, 4], [2, 2, 2], wool)], rotation=[-35, 0, 0]))
    bones.append(Bone("tail_flame", "tail", [0, 11.5, 5], [Cube([-1.5, 11, 4], [3, 2, 2], flame), Cube([-1, 13, 4.5], [2, 1, 1], flame),
                                                             Cube([-0.5, 14, 4.5], [1, 1, 1], flame)], rotation=[-15, 0, 0]))

    A = Anims("cinderkid")
    legs = lambda period, amp: {
        "leg_front_right": track(rotation=[wave(period, amp), 0, 0]), "leg_back_left": track(rotation=[wave(period, amp), 0, 0]),
        "leg_front_left": track(rotation=[wave(period, -amp), 0, 0]), "leg_back_right": track(rotation=[wave(period, -amp), 0, 0]),
        "hoof_front_right": track(rotation=[bounce(period, -20, 0), 0, 0]), "hoof_front_left": track(rotation=[bounce(period, -20, 90), 0, 0])}
    flick = {"fringe": track(scale=[1, wave(0.5, 0.12, 0, 1), 1]), "tail_flame": track(scale=[wave(0.4, 0.08, 0, 1), wave(0.3, 0.16, 0, 1), 1]),
             "horn_tip_right": track(rotation=[wave(1, 2), 0, 0]), "horn_tip_left": track(rotation=[wave(1, 2), 0, 0])}
    ground = A.add("ground_idle", 4.0, merge(flick, {
        "body": track(position=[0, wave(4, 0.18), 0]),
        "neck": track(rotation=[wave(4, 2), 0, 0]),
        "head": track(rotation=[wave(4, 3, 90), wave(4, 10), wave(4, 3, 45)]),
        "ear_right": track(rotation=[0, 0, wave(2, 6)]), "ear_left": track(rotation=[0, 0, wave(2, -6, 40)]),
        "tail": track(rotation=[wave(1, 8), 0, 0]),
    }, eyelids(2.6)))
    battle = A.add("battle_idle", 2.0, merge(flick, {
        "body": track(position=[0, bounce(1, 0.5), 0], rotation=[wave(1, 1.5), 0, 0]),
        "neck": track(rotation=[wave(1, 3, 0, 8), 0, 0]),
        "head": track(rotation=[wave(1, 3, 90, 6), wave(2, 4), 0]),
        "ear_right": track(rotation=[0, 0, wave(0.5, 7)]), "ear_left": track(rotation=[0, 0, wave(0.5, -7)]),
        "tail": track(rotation=[wave(0.5, 14), 0, 0]),
        "leg_front_right": track(rotation=[wave(1, 4, 0, -6), 0, 0]), "leg_front_left": track(rotation=[wave(1, 4, 0, 6), 0, 0]),
    }, eyelids(1.2)))
    walk = A.add("ground_walk", 0.7, merge(legs(0.7, 30), flick, {
        "body": track(position=[0, bounce(0.35, 0.55), 0], rotation=[0, 0, wave(0.7, 2)]),
        "neck": track(rotation=[wave(0.35, 3), 0, 0]),
        "head": track(rotation=[wave(0.35, 2, 90), 0, 0]),
        "ear_right": track(rotation=[0, 0, wave(0.35, 8)]), "ear_left": track(rotation=[0, 0, wave(0.35, -8)]),
        "tail": track(rotation=[wave(0.35, 12), 0, 0]),
    }, eyelids(0.4)))
    # A bleat: head thrown up, a little rear on the back legs, the horns and tail flaring.
    cry = A.add("cry", 1.1, {
        "body": track(rotation=keys({0: [0, 0, 0], 0.25: [-14, 0, 0], 0.7: [-14, 0, 0], 1.1: [0, 0, 0]}),
                      position=keys({0: [0, 0, 0], 0.25: [0, 1.2, 0], 0.7: [0, 1.0, 0], 1.1: [0, 0, 0]})),
        "neck": track(rotation=keys({0: [0, 0, 0], 0.25: [-16, 0, 0], 0.7: [-16, 0, 0], 1.1: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], 0.25: [-18, 0, 0], 0.45: [-12, 0, 0], 0.6: [-18, 0, 0], 1.1: [0, 0, 0]})),
        "leg_front_right": track(rotation=keys({0: [0, 0, 0], 0.25: [-30, 0, 0], 0.7: [-30, 0, 0], 1.1: [0, 0, 0]})),
        "leg_front_left": track(rotation=keys({0: [0, 0, 0], 0.25: [-22, 0, 0], 0.7: [-22, 0, 0], 1.1: [0, 0, 0]})),
        "tail_flame": track(scale=keys({0: [1, 1, 1], 0.25: [1.3, 1.6, 1.3], 0.8: [1.3, 1.6, 1.3], 1.1: [1, 1, 1]})),
        "fringe": track(scale=keys({0: [1, 1, 1], 0.25: [1.2, 1.5, 1.2], 0.8: [1.2, 1.5, 1.2], 1.1: [1, 1, 1]})),
    }, loop=False)
    recoil = A.add("recoil", 0.5, {
        "body": track(rotation=keys({0: [0, 0, 0], 0.1: [-10, 0, 6], 0.5: [0, 0, 0]}), position=keys({0: [0, 0, 0], 0.1: [0, 0, 1.2], 0.5: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], 0.1: [14, 0, 0], 0.5: [0, 0, 0]})),
    }, loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk, "parts": []}}}
    write(sid, "CINDERKID", bones, A, info, 0.95)


# ======================================================================================================
# TIDAPIR (the water starter's place): a young tapir, sea blue, with pale wave stripes down its sides, a short
# trunk it squirts water from, a fin along its back, and round ears with pale tips.
# ======================================================================================================

def tidapir():
    sid = "squirtle"
    BLUE, BLUE_L, BLUE_D = rgb("#2F74BA"), rgb("#4E95D6"), rgb("#1F5590")
    FOAM, BELLY, AQUA = rgb("#DDF6FF"), rgb("#A6DDEE"), rgb("#6FD3EE")

    def body_paint(at):
        if at.face == "bottom":
            return BELLY
        c = mix(BLUE_D, BLUE, 0.4 + 0.6 * (1 - at.ly)) if at.face != "top" else BLUE_D
        if at.face in ("px", "nx"):
            # Wave stripes along its sides, and a row of spots under them (as a young tapir's markings, but waves).
            for base, amp in ((7.5, 0.6), (5.5, 0.6)):
                yy = base + amp * math.sin(at.z * 0.7 + base)
                if abs(at.y - yy) < 0.5:
                    return FOAM
            if at.y < 3.6:
                return mix(c, BELLY, 0.6)
        if at.face == "front" and at.y < 6:
            return mix(c, BELLY, 0.5)
        return c

    body = Mat(BLUE, body_paint, noise=0.025)

    def head_paint(at):
        if at.face == "bottom":
            return BELLY
        if at.face == "front":
            if at.y < 7.0:
                return mix(BLUE, BLUE_L, 0.6)
            return BLUE
        if at.face in ("px", "nx"):
            yy = 8.6 + 0.4 * math.sin(at.z * 1.2)
            if abs(at.y - yy) < 0.45:
                return FOAM
        return mix(BLUE_D, BLUE, 0.5 + 0.5 * (1 - at.ly)) if at.face != "top" else BLUE_D

    head = Mat(BLUE, head_paint, noise=0.025)

    def trunk_paint(at):
        if at.face == "front":
            w, h = at.cube.size[0], at.cube.size[1]
            if at.row == h // 2 and 0 < at.col < w - 1:
                return "#123A66"
            return BLUE_L
        if at.face == "bottom":
            return BELLY
        return mix(BLUE, BLUE_L, 0.5)

    trunk = Mat(BLUE_L, trunk_paint, noise=0.02)
    fin = Mat(AQUA, lambda a: mix(AQUA, FOAM, a.ly * 0.8), noise=0.02, shade=0.6)

    def ear_paint(at):
        if at.row == 0 and at.face != "bottom":
            return FOAM
        if at.face == "front":
            return AQUA
        return BLUE

    ear = Mat(BLUE, ear_paint, noise=0.02)

    def foot(at):
        if at.face == "bottom":
            return "#16406E"
        if at.face == "front" and at.row == at.cube.size[1] - 1:
            return FOAM if at.col % 2 == 0 else BELLY
        return mix(BLUE_D, BLUE, at.ly)

    leg = Mat(BLUE, foot, noise=0.025)

    bones = [Bone("root", None, [0, 0, 0]), Bone("body", "root", [0, 7, 0], [
        Cube([-3.5, 3.5, -5], [7, 6, 10], body),
        Cube([-3, 4, 5], [6, 5, 1], body),
    ])]
    bones.append(Bone("back_fin", "body", [0, 9.5, 0], [Cube([-0.5, 9.5, -3.5], [1, 1, 8], fin), Cube([-0.5, 10.5, -1.5], [1, 1, 4], fin)]))
    bones.append(Bone("head", "body", [0, 8.5, -5], [
        Cube([-3, 6, -9.5], [6, 6, 5], head),
        Cube([-0.5, 12, -8.5], [1, 1, 3], fin),
    ]))
    bones.append(Bone("trunk", "head", [0, 7.5, -9.5], [Cube([-1.5, 6, -12], [3, 3, 3], trunk)]))
    bones.append(Bone("trunk_tip", "trunk", [0, 6.5, -12], [Cube([-1, 5, -13.5], [2, 2, 2], trunk)], rotation=[18, 0, 0]))
    bones += pair("ear_right", "head", [-2.5, 11.5, -7], [Cube([-4, 11, -7.5], [2, 2, 1], ear)], rotation=[0, 0, -20])
    bones += eye_pair("head", x_inner=0.75, y=9.5, z=-9.5, w=2, h=2, iris="#123A66", pupil="#0A1426", skin="#2F74BA")
    for side, x in (("right", -2.25), ("left", 2.25)):
        for end, z in (("front", -3.25), ("back", 3.25)):
            bones.append(Bone(f"leg_{end}_{side}", "body", [x, 4, z], [Cube([x - 1.25, 0, z - 1.25], [3, 4, 3], leg)]))
    bones.append(Bone("tail", "body", [0, 8, 6], [Cube([-1.5, 7, 6], [3, 2, 1], fin)], rotation=[-20, 0, 0]))

    A = Anims("tidapir")
    legs = lambda period, amp: {
        "leg_front_right": track(rotation=[wave(period, amp), 0, 0]), "leg_back_left": track(rotation=[wave(period, amp), 0, 0]),
        "leg_front_left": track(rotation=[wave(period, -amp), 0, 0]), "leg_back_right": track(rotation=[wave(period, -amp), 0, 0])}
    ground = A.add("ground_idle", 4.0, merge({
        "body": track(position=[0, wave(4, 0.2), 0], scale=[wave(2, 0.012, 0, 1), wave(2, 0.02, 0, 1), 1]),
        "head": track(rotation=[wave(4, 3, 90), wave(4, 8), 0]),
        "trunk": track(rotation=[wave(1.33, 6), wave(2, 5), 0]), "trunk_tip": track(rotation=[wave(1, 8, 60), 0, 0]),
        "ear_right": track(rotation=[0, wave(2, 8), wave(4, 5)]), "ear_left": track(rotation=[0, wave(2, -8, 70), wave(4, -5)]),
        "tail": track(rotation=[0, wave(1, 14), 0]),
    }, eyelids(1.8)))
    battle = A.add("battle_idle", 2.0, merge({
        "body": track(position=[0, bounce(1, 0.45), 0]),
        "head": track(rotation=[wave(1, 3, 90, -3), wave(2, 5), 0]),
        "trunk": track(rotation=[wave(0.5, 8, 0, -6), 0, 0]), "trunk_tip": track(rotation=[wave(0.5, 10, 60), 0, 0]),
        "ear_right": track(rotation=[0, 0, wave(0.5, 6)]), "ear_left": track(rotation=[0, 0, wave(0.5, -6)]),
        "tail": track(rotation=[0, wave(0.5, 18), 0]),
        "back_fin": track(scale=[1, wave(1, 0.1, 0, 1), 1]),
    }, eyelids(0.9)))
    walk = A.add("ground_walk", 0.8, merge(legs(0.8, 28), {
        "body": track(position=[0, bounce(0.4, 0.45), 0], rotation=[0, 0, wave(0.8, 3)]),
        "head": track(rotation=[wave(0.4, 2, 90), wave(0.8, -3), 0]),
        "trunk": track(rotation=[wave(0.4, 7), 0, 0]),
        "ear_right": track(rotation=[wave(0.4, 6), 0, 0]), "ear_left": track(rotation=[wave(0.4, 6), 0, 0]),
        "tail": track(rotation=[0, wave(0.8, 18), 0]),
    }, eyelids(0.55)))
    # A trumpet: trunk up and out, head up, a stamp of the front feet.
    cry = A.add("cry", 1.2, {
        "head": track(rotation=keys({0: [0, 0, 0], 0.25: [-20, 0, 0], 0.8: [-20, 0, 0], 1.2: [0, 0, 0]})),
        "trunk": track(rotation=keys({0: [0, 0, 0], 0.25: [-55, 0, 0], 0.5: [-48, 0, 0], 0.8: [-55, 0, 0], 1.2: [0, 0, 0]})),
        "trunk_tip": track(rotation=keys({0: [0, 0, 0], 0.25: [-30, 0, 0], 0.8: [-30, 0, 0], 1.2: [0, 0, 0]})),
        "leg_front_right": track(rotation=keys({0: [0, 0, 0], 0.15: [-30, 0, 0], 0.3: [0, 0, 0]})),
        "leg_front_left": track(rotation=keys({0: [0, 0, 0], 0.35: [0, 0, 0], 0.5: [-30, 0, 0], 0.65: [0, 0, 0]})),
        "ear_right": track(rotation=keys({0: [0, 0, 0], 0.25: [-10, 0, -22], 0.9: [-10, 0, -22], 1.2: [0, 0, 0]})),
        "ear_left": track(rotation=keys({0: [0, 0, 0], 0.25: [-10, 0, 22], 0.9: [-10, 0, 22], 1.2: [0, 0, 0]})),
    }, loop=False)
    recoil = A.add("recoil", 0.5, {
        "body": track(rotation=keys({0: [0, 0, 0], 0.1: [-8, 0, -5], 0.5: [0, 0, 0]}), position=keys({0: [0, 0, 0], 0.1: [0, 0, 1.2], 0.5: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], 0.1: [12, 0, 0], 0.5: [0, 0, 0]})),
    }, loop=False)
    info = {"idle": ground, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk, "parts": []}}}
    write(sid, "TIDAPIR", bones, A, info, 0.8)


# ======================================================================================================
# QUAILET (in Route 1's grass, the common bird's place): a round little quail, soft lilac-grey with a cream
# scalloped breast, white face stripes, a dark teardrop plume nodding over its brow, and barred wings.
# ======================================================================================================

def quailet():
    sid = "pidgey"
    LIL, LIL_L, LIL_D = rgb("#9A8DB4"), rgb("#B8AECB"), rgb("#6C6088")
    CREAM, SCALE = rgb("#F3E3C4"), rgb("#DCC29C")
    PLUME = rgb("#2C2638")
    LEG = "#E58C4E"

    def body_paint(at):
        if at.face == "bottom":
            return CREAM
        if at.face == "front":
            # The scalloped breast: rows of little crescents.
            c = CREAM
            r, k = at.row, at.col
            if (r % 2 == 1) and ((k + (r // 2)) % 2 == 0):
                c = SCALE
            return c
        if at.face in ("px", "nx") and at.y < 3.5:
            return mix(LIL, CREAM, 0.6)
        return mix(LIL_D, LIL, 0.5 + 0.5 * (1 - at.ly)) if at.face != "top" else LIL_L

    body = Mat(LIL, body_paint, noise=0.025)

    def head_paint(at):
        if at.face == "front":
            # A white stripe from the brow round under the eyes, a white chin.
            if at.row == 0:
                return "#FFFFFF"
            if at.row >= at.cube.size[1] - 1:
                return "#FFFFFF"
            if at.row == at.cube.size[1] - 2:
                return "#3E3550"
            return LIL_D
        if at.face in ("px", "nx") and abs(at.y - 10.5) < 0.5:
            return "#FFFFFF"
        if at.face == "top":
            return LIL_D
        return mix(LIL_D, LIL, 0.4)

    head = Mat(LIL, head_paint, noise=0.02)
    beak = Mat("#3A3640", lambda a: "#5A5662" if a.face == "top" else None, noise=0.02)
    plume = Mat(PLUME, lambda a: mix(PLUME, rgb("#4A4060"), a.ly * 0.6), noise=0.02)

    def wing_paint(at):
        # Barred: pale bars across the wing, the tips darker.
        if at.face in ("px", "nx", "top", "bottom"):
            t = (at.z - at.cube.origin[2]) / max(1, at.cube.size[2])
            c = mix(LIL, LIL_D, max(0, t - 0.55) * 2.2)
            if at.face in ("px", "nx") and int(math.floor(at.y + at.z * 0.5)) % 2 == 0 and t < 0.7:
                c = mix(c, LIL_L, 0.7)
            if at.face in ("px", "nx") and at.row == at.cube.size[1] - 1:
                c = mix(c, CREAM, 0.6)
            return c
        return LIL_D

    wing = Mat(LIL, wing_paint, noise=0.02)
    tail = Mat(LIL_D, lambda a: mix(LIL_D, PLUME, a.lz), noise=0.02)
    legm = Mat(LEG, lambda a: "#C9703A" if a.face == "bottom" else None, noise=0.02)

    bones = [Bone("root", None, [0, 0, 0]), Bone("body", "root", [0, 4.5, 0], [
        Cube([-3, 2, -3.5], [6, 5, 7], body),
        Cube([-2.5, 7, -2.5], [5, 1, 5], body),
    ])]
    bones.append(Bone("head", "body", [0, 7.5, -1.5], [
        Cube([-2.5, 7.5, -4.5], [5, 4, 4], head),
    ]))
    bones.append(Bone("beak", "head", [0, 9, -4.5], [Cube([-0.5, 8.5, -5.5], [1, 1, 1], beak)], rotation=[10, 0, 0]))
    bones.append(Bone("plume", "head", [0, 11.5, -3], [Cube([-0.5, 11.5, -3.5], [1, 2, 1], plume)], rotation=[-10, 0, 0]))
    bones.append(Bone("plume_tip", "plume", [0, 13.5, -3], [Cube([-0.5, 12.5, -5], [1, 2, 2], plume)], rotation=[35, 0, 0]))
    bones += eye_pair("head", x_inner=0.5, y=9, z=-4.5, w=2, h=2, iris="#1B1820", pupil="#0C0A10", skin="#6C6088")
    bones += pair("wing_right", "body", [-3, 6.5, -2], [Cube([-4, 2.5, -2.5], [1, 4, 6], wing)])
    bones.append(Bone("tail", "body", [0, 5, 3.5], [Cube([-1.5, 4.5, 3.5], [3, 1, 3], tail)], rotation=[-25, 0, 0]))
    bones += pair("leg_right", "body", [-1.25, 2, 0], [Cube([-1.75, 1, -0.5], [1, 1, 1], legm), Cube([-2.25, 0, -1.5], [2, 1, 2], legm)])

    A = Anims("quailet")
    peck_head = keys({0: [0, 0, 0], 2.2: [0, 0, 0], 2.35: [0, 25, 0], 2.9: [0, 25, 0], 3.05: [0, -20, 0], 3.6: [0, -20, 0], 3.75: [0, 0, 0], 4: [0, 0, 0]})
    ground = A.add("ground_idle", 4.0, merge({
        "body": track(position=[0, wave(2, 0.15), 0], scale=[wave(1, 0.015, 0, 1), wave(1, 0.025, 0, 1), wave(1, 0.015, 0, 1)]),
        "head": track(rotation=peck_head),
        "plume": track(rotation=[wave(1, 6), 0, 0]), "plume_tip": track(rotation=[wave(1, 10, 60), 0, 0]),
        "wing_right": track(rotation=[0, 0, wave(2, 1.5)]), "wing_left": track(rotation=[0, 0, wave(2, -1.5)]),
    }, eyelids(1.5)))
    # (The tail flick, as keyframes on its own.)
    A.all[ground]["bones"]["tail"] = track(rotation=keys({0: [0, 0, 0], 1.0: [0, 0, 0], 1.08: [-18, 0, 0], 1.2: [0, 0, 0], 4: [0, 0, 0]}))
    walk = A.add("ground_walk", 0.5, merge({
        "leg_right": track(rotation=[wave(0.5, 40), 0, 0]), "leg_left": track(rotation=[wave(0.5, -40), 0, 0]),
        "body": track(position=[0, bounce(0.25, 0.5), 0], rotation=[0, 0, wave(0.5, 4)]),
        # The head bobs as birds' do: back as the foot goes down, forward after.
        "head": track(position=[0, 0, wave(0.25, 0.45)]),
        "plume": track(rotation=[wave(0.25, 10), 0, 0]),
        "tail": track(rotation=[wave(0.25, 6), 0, 0]),
    }, eyelids(0.3)))
    fly = A.add("air_fly", 0.36, merge({
        "body": track(rotation=[14, 0, 0], position=[0, wave(0.36, 0.5, 90), 0]),
        "wing_right": track(rotation=[0, 0, wave(0.36, 55, 0, -75)]), "wing_left": track(rotation=[0, 0, wave(0.36, -55, 0, 75)]),
        "leg_right": track(rotation=[55, 0, 0]), "leg_left": track(rotation=[55, 0, 0]),
        "head": track(rotation=[-12, 0, 0]),
        "plume": track(rotation=[22, 0, 0]),
        "tail": track(rotation=[wave(0.36, 8), 0, 0]),
    }, eyelids(0.2, 0)))
    hover = A.add("air_hover", 0.5, merge({
        "body": track(position=[0, wave(0.5, 0.8, 90), 0]),
        "wing_right": track(rotation=[0, 0, wave(0.5, 45, 0, -70)]), "wing_left": track(rotation=[0, 0, wave(0.5, -45, 0, 70)]),
        "leg_right": track(rotation=[25, 0, 0]), "leg_left": track(rotation=[25, 0, 0]),
        "plume": track(rotation=[wave(0.5, 10), 0, 0]),
    }, eyelids(0.3)))
    cry = A.add("cry", 1.0, {
        "head": track(rotation=keys({0: [0, 0, 0], 0.2: [-30, 0, 0], 0.4: [-24, 0, 0], 0.6: [-30, 0, 0], 1.0: [0, 0, 0]})),
        "plume": track(rotation=keys({0: [0, 0, 0], 0.2: [-25, 0, 0], 0.6: [10, 0, 0], 1.0: [0, 0, 0]})),
        "wing_right": track(rotation=keys({0: [0, 0, 0], 0.2: [0, 0, -50], 0.4: [0, 0, -30], 0.6: [0, 0, -50], 1.0: [0, 0, 0]})),
        "wing_left": track(rotation=keys({0: [0, 0, 0], 0.2: [0, 0, 50], 0.4: [0, 0, 30], 0.6: [0, 0, 50], 1.0: [0, 0, 0]})),
        "body": track(position=keys({0: [0, 0, 0], 0.2: [0, 1, 0], 0.6: [0, 1, 0], 1.0: [0, 0, 0]})),
    }, loop=False)
    recoil = A.add("recoil", 0.5, {
        "body": track(rotation=keys({0: [0, 0, 0], 0.1: [-14, 0, 0], 0.5: [0, 0, 0]}), position=keys({0: [0, 0, 0], 0.1: [0, 0.6, 1.2], 0.5: [0, 0, 0]})),
        "wing_right": track(rotation=keys({0: [0, 0, 0], 0.1: [0, 0, -35], 0.5: [0, 0, 0]})),
        "wing_left": track(rotation=keys({0: [0, 0, 0], 0.1: [0, 0, 35], 0.5: [0, 0, 0]})),
    }, loop=False)
    info = {"idle": ground, "ground": ground, "actions": {"cry": cry, "recoil": recoil},
            "poses": {"walk": {"anim": walk, "parts": []}, "fly": {"anim": fly, "parts": []}, "hover": {"anim": hover, "parts": []}}}
    write(sid, "QUAILET", bones, A, info, 0.56)


# ======================================================================================================
# JERBIT (in Route 1's grass, the common rodent's place): a jerboa, sandy with a cream belly, huge pink-lined
# ears, long hind feet it hops on, tiny front paws, and a long tail ending in a black and white tuft.
# ======================================================================================================

def jerbit():
    sid = "rattata"
    SAND, SAND_L, SAND_D = rgb("#D9A764"), rgb("#E8C083"), rgb("#B7864A")
    CREAM = rgb("#F7E8CB")
    PINK = "#EFA2AE"

    def body_paint(at):
        if at.face == "bottom":
            return CREAM
        if at.face == "front":
            return CREAM if at.y < 6.5 else SAND
        if at.face in ("px", "nx") and at.y < 4.5:
            return mix(SAND, CREAM, 0.7)
        return mix(SAND_D, SAND, 0.5 + 0.5 * at.ly) if at.face != "top" else SAND_D

    body = Mat(SAND, body_paint, noise=0.025)

    def head_paint(at):
        if at.face == "bottom":
            return CREAM
        if at.face == "front" and at.y < 7.2:
            return CREAM
        if at.face == "top":
            return SAND_D
        return mix(SAND_D, SAND, 0.4 + 0.6 * at.ly)

    head = Mat(SAND, head_paint, noise=0.025)

    def snout_paint(at):
        if at.face == "front":
            w = at.cube.size[0]
            if at.row == 0:
                return PINK
            return CREAM
        if at.face in ("px", "nx") and at.row == 1:
            return "#8A6A4A"
        return CREAM

    snout = Mat(CREAM, snout_paint, noise=0.02)

    def ear_paint(at):
        if at.face == "front":
            w, h = at.cube.size[0], at.cube.size[1]
            if at.row == 0 or at.col in (0, w - 1):
                return SAND
            return PINK
        return mix(SAND_D, SAND, at.ly)

    ear = Mat(SAND, ear_paint, noise=0.02)
    paw = Mat(CREAM, noise=0.02)
    thigh = Mat(SAND, lambda a: CREAM if a.face == "bottom" or (a.face == "front" and a.y < 3) else None, noise=0.025)
    foot = Mat(CREAM, lambda a: "#E7C9A6" if a.face == "bottom" else None, noise=0.02)
    tailm = Mat(SAND, lambda a: SAND_D if a.face == "top" else None, noise=0.02)

    def tuft_paint(at):
        t = (at.z - at.cube.origin[2]) / max(1, at.cube.size[2])
        if at.face == "back" or t > 0.66:
            return "#FFFFFF"
        return "#2E2420"

    tuft = Mat("#2E2420", tuft_paint, noise=0.02)

    bones = [Bone("root", None, [0, 0, 0]), Bone("body", "root", [0, 5, 0], [
        Cube([-2.5, 3, -3], [5, 5, 6], body),
    ])]
    bones.append(Bone("head", "body", [0, 7, -2.5], [
        Cube([-2.5, 6, -6.5], [5, 4, 4], head),
    ]))
    bones.append(Bone("snout", "head", [0, 7, -6.5], [Cube([-1, 6, -7.5], [2, 2, 1], snout)]))
    bones += pair("ear_right", "head", [-1.5, 9.5, -4.5], [Cube([-2.75, 9.5, -5], [3, 5, 1], ear)], rotation=[6, 0, -16])
    bones += eye_pair("head", x_inner=0.25, y=8, z=-6.5, w=2, h=2, iris="#1A1414", pupil="#080606", skin="#D9A764")
    bones += pair("arm_right", "body", [-1.5, 5, -3], [Cube([-2, 3.5, -3.75], [1, 2, 1], paw)], rotation=[-25, 0, 0])
    bones += pair("leg_right", "body", [-2, 4.5, 1], [Cube([-3.5, 2, -0.5], [2, 3, 3], thigh)])
    bones += pair("foot_right", "leg_right", [-2.5, 2, 0.5], [Cube([-3.25, 0, -3], [1, 2, 4], foot)])
    for b in bones:
        if b.name == "foot_left":
            b.parent = "leg_left"
    bones.append(Bone("tail", "body", [0, 4.5, 3], [Cube([-0.5, 4, 3], [1, 1, 6], tailm)], rotation=[-12, 0, 0]))
    bones.append(Bone("tail2", "tail", [0, 4.5, 9], [Cube([-0.5, 4, 9], [1, 1, 4], tailm)], rotation=[18, 0, 0]))
    bones.append(Bone("tail_tuft", "tail2", [0, 4.5, 13], [Cube([-1, 3.5, 12.5], [2, 2, 3], tuft)]))

    A = Anims("jerbit")
    ground = A.add("ground_idle", 4.0, merge({
        "body": track(position=[0, wave(2, 0.14), 0], scale=[1, wave(1, 0.025, 0, 1), 1]),
        "head": track(rotation=[wave(4, 3, 90), wave(4, 12), 0]),
        "snout": track(scale=[1, 1, wave(0.25, 0.12, 0, 1)]),
        "ear_right": track(rotation=[wave(2, 4), wave(1, 10), 0]), "ear_left": track(rotation=[wave(2, 4, 50), wave(1, -10, 50), 0]),
        "tail": track(rotation=[0, wave(2, 10), 0]), "tail2": track(rotation=[wave(2, 6, 90), 0, 0]),
        "arm_right": track(rotation=[wave(1, 6), 0, 0]), "arm_left": track(rotation=[wave(1, 6, 40), 0, 0]),
    }, eyelids(2.2)))
    walk = A.add("ground_walk", 0.45, merge({
        # It hops on its long feet: both legs together, the body sailing up and the tail balancing.
        "body": track(position=[0, bounce(0.45, 2.0), 0], rotation=[wave(0.45, 8, 90), 0, 0]),
        "leg_right": track(rotation=[wave(0.45, 30, 180), 0, 0]), "leg_left": track(rotation=[wave(0.45, 30, 180), 0, 0]),
        "foot_right": track(rotation=[wave(0.45, -20, 180), 0, 0]), "foot_left": track(rotation=[wave(0.45, -20, 180), 0, 0]),
        "tail": track(rotation=[wave(0.45, 12, 0), 0, 0]), "tail2": track(rotation=[wave(0.45, 10, 60), 0, 0]),
        "ear_right": track(rotation=[wave(0.45, 10, 30), 0, 0]), "ear_left": track(rotation=[wave(0.45, 10, 30), 0, 0]),
        "arm_right": track(rotation=[-20, 0, 0]), "arm_left": track(rotation=[-20, 0, 0]),
    }, eyelids(0.3)))
    cry = A.add("cry", 1.0, {
        "body": track(rotation=keys({0: [0, 0, 0], 0.2: [-22, 0, 0], 0.7: [-22, 0, 0], 1.0: [0, 0, 0]}),
                      position=keys({0: [0, 0, 0], 0.2: [0, 1.4, 0], 0.7: [0, 1.4, 0], 1.0: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], 0.2: [-10, 0, 0], 0.4: [-4, 0, 0], 0.6: [-10, 0, 0], 1.0: [0, 0, 0]})),
        "ear_right": track(rotation=keys({0: [0, 0, 0], 0.2: [-12, 0, 10], 0.8: [-12, 0, 10], 1.0: [0, 0, 0]})),
        "ear_left": track(rotation=keys({0: [0, 0, 0], 0.2: [-12, 0, -10], 0.8: [-12, 0, -10], 1.0: [0, 0, 0]})),
        "arm_right": track(rotation=keys({0: [0, 0, 0], 0.2: [-40, 0, 0], 0.7: [-40, 0, 0], 1.0: [0, 0, 0]})),
        "arm_left": track(rotation=keys({0: [0, 0, 0], 0.2: [-40, 0, 0], 0.7: [-40, 0, 0], 1.0: [0, 0, 0]})),
    }, loop=False)
    recoil = A.add("recoil", 0.5, {
        "body": track(rotation=keys({0: [0, 0, 0], 0.1: [-12, 0, 0], 0.5: [0, 0, 0]}), position=keys({0: [0, 0, 0], 0.1: [0, 0.4, 1.2], 0.5: [0, 0, 0]})),
        "ear_right": track(rotation=keys({0: [0, 0, 0], 0.1: [30, 0, 0], 0.5: [0, 0, 0]})),
        "ear_left": track(rotation=keys({0: [0, 0, 0], 0.1: [30, 0, 0], 0.5: [0, 0, 0]})),
    }, loop=False)
    info = {"idle": ground, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk, "parts": []}}}
    write(sid, "JERBIT", bones, A, info, 0.5)


def main():
    fernadillo()
    cinderkid()
    tidapir()
    quailet()
    jerbit()
    names = {}
    for sid in sorted(os.listdir(OUT_MODELS)):
        p = os.path.join(OUT_MODELS, sid, "info.json")
        if os.path.exists(p):
            names[sid] = json.load(open(p))["name"]
    json.dump(names, open(os.path.join(OUT_MODELS, "names.json"), "w"), indent=1)


if __name__ == "__main__":
    main()
