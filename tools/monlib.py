"""
The workshop for Retromon's own creatures (tools/mons.py builds them with it): models made of cubes, textures
painted in 3D, and animations written in Molang, all in the Bedrock format BedrockModel.java reads.

Conventions (Bedrock's): y up, units of 1/16 of a block, feet at y = 0, the face towards -z, the creature's right
at -x. A texel's colour is worked out from where it sits on the model, so patterns run across cubes and eyes land
where they're placed.
"""

import json, math, os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon")
OUT_MODELS = os.path.join(ROOT, "mons")
OUT_TEX = os.path.join(ROOT, "textures", "mons")

CLEAR = "clear"


# ---------- colours ----------

def rgb(h):
    if not isinstance(h, str):
        return np.asarray(h, float)
    h = h.lstrip("#")
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], float)


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return a + (b - a) * max(0.0, min(1.0, t))


def ramp(cols, t):
    """A colour along a run of colours (t from 0 to 1)."""
    cols = [rgb(c) for c in cols]
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(len(cols) - 2, int(t))
    return mix(cols[i], cols[i + 1], t - i)


def hash01(*k):
    """A steady pseudo-random number in [0, 1) for these keys (the same every run)."""
    h = 2166136261
    for v in k:
        for ch in str(v):
            h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return (h & 0xFFFFFF) / float(0x1000000)


def cell(x, y, z, size=1.0):
    """A steady random number for the block of space a point's in (for clumps, spots and moss)."""
    return hash01("c", math.floor(x / size), math.floor(y / size), math.floor(z / size))


# ---------- the model ----------

class Mat:
    """
    A cube's paint: a base colour and layers drawn over it in turn, each a function of the texel's spot (At) giving
    a colour, CLEAR (no texel: see-through) or None (leave it). glow: the same for the glowing layer.
    edge: how much darker the texels round each face's edge are (an outline, as hand-painted models have).
    """

    def __init__(self, base, *layers, glow=None, shade=1.0, noise=0.03, flat=False, edge=0.07, frames=False):
        self.base = rgb(base)
        self.layers = layers
        self.glow = glow
        self.shade = shade
        self.noise = noise
        self.flat = flat
        self.edge = edge
        # frames: glow is called with the frame too, glow(at, frame), and gives (colour, alpha) or None: something
        # whose shape changes from frame to frame (a flame's tongues), drawn only in the glowing layer.
        self.frames = frames

    def colour(self, at):
        c = self.base
        for f in self.layers:
            r = f(at)
            if r is None:
                continue
            if isinstance(r, str) and r == CLEAR:
                return None
            c = rgb(r)
        return c


class At:
    """Where a texel is: its point (x, y, z), the face, the cube, its spot on the cube (lx, ly, lz from 0 to 1), and its
    column and row on the face (fw by fh)."""

    __slots__ = ("x", "y", "z", "face", "cube", "lx", "ly", "lz", "col", "row", "fw", "fh")

    def __init__(self, x, y, z, face, cube, col, row, fw, fh):
        self.x, self.y, self.z, self.face, self.cube = x, y, z, face, cube
        o, s = cube.origin, cube.size
        self.lx = (x - o[0]) / s[0] if s[0] else 0.5
        self.ly = (y - o[1]) / s[1] if s[1] else 0.5
        self.lz = (z - o[2]) / s[2] if s[2] else 0.5
        self.col, self.row, self.fw, self.fh = col, row, fw, fh

    @property
    def side(self):
        return self.face in ("px", "nx")

    @property
    def edge(self):
        return self.col == 0 or self.row == 0 or self.col == self.fw - 1 or self.row == self.fh - 1


class Cube:
    def __init__(self, origin, size, mat, rotation=None, pivot=None, seams=(), effect=False):
        self.origin = [float(v) for v in origin]
        self.size = [int(round(v)) for v in size]
        self.mat = mat
        self.rotation = rotation
        self.pivot = pivot
        # Edges that join on to another box of the same part, so they get no outline: (face, "r0" first row, "rl"
        # last row, "c0" first column or "cl" last column).
        self.seams = set(seams)
        # An effect (smoke, stars, a move's leaves): not part of the creature's body, so it doesn't count to its height.
        self.effect = effect
        self.uv = None


class Bone:
    def __init__(self, name, parent, pivot, cubes=(), rotation=None):
        self.name, self.parent, self.pivot, self.cubes, self.rotation = name, parent, [float(v) for v in pivot], list(cubes), rotation


def mirror_cube(c):
    o, s = c.origin, c.size
    piv = None if c.pivot is None else [-c.pivot[0], c.pivot[1], c.pivot[2]]
    rot = None if c.rotation is None else [c.rotation[0], -c.rotation[1], -c.rotation[2]]
    return Cube([-(o[0] + s[0]), o[1], o[2]], s, c.mat, rot, piv, c.seams, c.effect)


def blob(origin, size, mat, b=1, flat_bottom=False):
    """A rounded block: three overlapping boxes, so its edges and corners are cut back b (as modelled creatures' bodies
    are). flat_bottom keeps the underside square (for things that sit on the ground)."""
    x, y, z = origin
    w, h, d = [int(round(v)) for v in size]
    if w <= 2 * b or h <= 2 * b or d <= 2 * b:
        return [Cube(origin, size, mat)]
    yb = 0 if flat_bottom else b
    return [Cube([x, y + yb, z + b], [w, h - b - yb, d - 2 * b], mat),
            Cube([x + b, y, z + b], [w - 2 * b, h, d - 2 * b], mat),
            Cube([x + b, y + yb, z], [w - 2 * b, h - b - yb, d], mat)]


def face_blob(origin, size, mat, b=1):
    """A head that's rounded at the top and back but has a flat face the full width across (for eyes on the front)."""
    x, y, z = origin
    w, h, d = [int(round(v)) for v in size]
    # Three boxes that don't overlap (where two faces lie in the same place, they fight over which is drawn): the
    # face and its sides, a band over the top, and a band round the back. The joins between them get no outline.
    return [Cube([x, y, z], [w, h - b, d - b], mat, seams={("front", "r0"), ("top", "r0"), ("bottom", "r0")}),
            Cube([x + b, y + h - b, z], [w - 2 * b, b, d - b], mat),
            Cube([x + b, y, z + d - b], [w - 2 * b, h - b, b], mat, seams={("top", "rl"), ("bottom", "rl")})]


class Model:
    """A creature's bones (and their cubes), with helpers for parts in pairs and for eyes."""

    def __init__(self):
        self.bones = []
        self.names = set()

    def bone(self, name, parent, pivot, cubes=(), rotation=None):
        b = Bone(name, parent, pivot, cubes, rotation)
        self.bones.append(b)
        self.names.add(name)
        return b

    def pair(self, right, parent, pivot, cubes, rotation=None):
        """A part on the right (-x), and its mirror on the left. A parent named ..._right gets ..._left on the left."""
        left = right.replace("right", "left")
        rot_l = None if rotation is None else [rotation[0], -rotation[1], -rotation[2]]
        pl = parent.replace("right", "left") if parent else parent
        self.bone(right, parent, pivot, cubes, rotation)
        self.bone(left, pl, [-pivot[0], pivot[1], pivot[2]], [mirror_cube(c) for c in cubes], rot_l)

    def eyes(self, head, x_inner, y, z, w=2, h=3, iris="#3A2A20", pupil="#120D0A", skin="#888888", lash="#1A1414", shine="#FFFFFF",
             iris_light=None, white=None, lid_line=None, side=False, corner=None, grid=None, half=None, squint=True):
        """Eyes, each with an eyelid that drops shut for a blink (the lid's height, its scale in y, goes from 0 to 1).
        Front (the default): on the face at z, the right eye's inner edge x_inner from the middle.
        side=True: on the sides of the head, x_inner being the head's half-width and z where the eye starts at the front.
        corner=(wf, ws): round the corner of a flat face: wf columns on the face (at its outer edge, half being the head's
        half-width), ws on the side; z is the face. grid: the eye's own drawing (rows of letters, inner side first)."""
        if corner:
            wf, ws = corner
            hw = half if half is not None else x_inner
            em_f = eye_mat(wf + ws, h, iris, pupil, lash, shine, iris_light, white, grid=grid, part=(0, wf), face="front", skin=skin)
            em_s = eye_mat(wf + ws, h, iris, pupil, lash, shine, iris_light, white, grid=grid, part=(wf, ws), face="side", skin=skin)
            lm = Mat(skin, lambda a: CLEAR if not outward(a) else (rgb(lid_line or lash) if a.row == a.fh - 1 else None), noise=0.01, shade=0.2, edge=0)
            self.pair("eye_right", head, [-hw, y + h, z], [Cube([-hw, y, z - 0.01], [wf, h, 0], em_f), Cube([-hw - 0.01, y, z], [0, h, ws], em_s)])
            self.pair("eyelid_right", "eye_right", [-hw, y + h, z], [Cube([-hw - 0.02, y, z - 0.03], [wf + 0.04, h, 0], lm),
                                                                     Cube([-hw - 0.03, y, z - 0.02], [0, h, ws + 0.02], lm)])
            if squint:
                for name, gridf, off in COVERS:
                    g = gridf(wf + ws, h)
                    c_f = eye_mat(wf + ws, h, iris, pupil, lash, shine, grid=g, part=(0, wf), face="front", skin=skin)
                    c_s = eye_mat(wf + ws, h, iris, pupil, lash, shine, grid=g, part=(wf, ws), face="side", skin=skin)
                    self.pair(name + "_right", "eye_right", [-hw, y + h / 2, z], [Cube([-hw - 0.02, y, z - off], [wf, h, 0], c_f),
                                                                                Cube([-hw - off, y, z - 0.02], [0, h, ws], c_s)])
            return
        em = eye_mat(w, h, iris, pupil, lash, shine, iris_light, white, side=side, grid=grid, skin=skin)
        lid_face = ("nx", "px") if side else ("front",)
        lm = Mat(skin, lambda a: CLEAR if not outward(a) else (rgb(lid_line or lash) if a.face in lid_face and a.row == a.fh - 1 else None),
                 noise=0.01, shade=0.2, edge=0)
        if side:
            xs = -x_inner
            self.pair("eye_right", head, [xs, y + h, z + w / 2], [Cube([xs - 0.01, y, z], [0, h, w], em)])
            self.pair("eyelid_right", "eye_right", [xs, y + h, z + w / 2], [Cube([xs - 0.03, y, z], [0, h, w], lm)])
            return
        xr = -(x_inner + w)
        self.pair("eye_right", head, [xr + w / 2, y + h, z], [Cube([xr, y, z - 0.01], [w, h, 0], em)])
        self.pair("eyelid_right", "eye_right", [xr + w / 2, y + h, z], [Cube([xr, y, z - 0.03], [w, h, 0], lm)])
        if squint:
            for name, gridf, off in COVERS:
                # Dazed and fainted eyes need room for their mark: at least three across and three high, centred on the eye.
                cw, ch = (w, h) if name == "squint" else (max(3, w), max(3, h))
                cx0, cy0 = xr - (cw - w) / 2, y - (ch - h) / 2
                cm = eye_mat(cw, ch, iris, pupil, lash, shine, grid=gridf(cw, ch), skin=skin)
                self.pair(name + "_right", "eye_right", [xr + w / 2, y + h / 2, z], [Cube([cx0, cy0, z - off], [cw, ch, 0], cm)])

    def battle_fx(self, head, top, cz, mouth, front, size=1.0):
        """The battle effects every creature has: stars that circle above its head when it's confused (top: the head's
        top, cz: its middle front to back), and a burst where a critical hit lands (in front of its mouth)."""
        # The stars are sized when the creature is written, once its size in the world is known: the same size above
        # every creature, big or small, and well clear of its head.
        hw = max((abs(v) for b in self.bones if b.name == head for c in b.cubes if not c.effect for v in (c.origin[0], c.origin[0] + c.size[0])),
                 default=3.0)
        self.stars_spec = (head, top, cz, hw)
        self.bone("fx_stars", head, [0, top, cz], [])
        bm = burst_mat()
        m = max(8, round(12 * size))
        bz = front - 4 * size
        self.bone("fx_impact", "root" if "root" in self.names else None, [0, mouth, bz],
                  [Cube([-m / 2, mouth - m / 2, bz], [m, m, 0], bm, effect=True),
                   Cube([-m / 2, mouth - m / 2, bz], [m, m, 0], bm, [0, 90, 0], [0, mouth, bz], effect=True)])

    def brows(self, head, cubes):
        """Brows over the eyes (cubes for the right one; the left is mirrored)."""
        self.bone("brows", head, [0, 0, 0], list(cubes) + [mirror_cube(c) for c in cubes])

    def cubes(self):
        return [c for b in self.bones for c in b.cubes]


# Eyes, as their faces are drawn (column 0 is the eye's inner side, towards the middle of the face):
# L lash (the dark line along the top), W highlight, w a second small highlight, P pupil, I iris, J the iris's lit lower part,
# E the white of the eye.
EYES = {
    (2, 2): ["WP", "PI"],
    (2, 3): ["LL", "WP", "PJ"],
    (3, 2): ["WPI", "PJE"],
    (3, 3): ["LLL", "WPI", "EJJ"],
    (3, 4): ["LLL", "WPI", "PPI", "EJJ"],
    (4, 3): ["LLLL", "EWPI", "EJJE"],
    (4, 4): ["LLLL", "EWPI", "EPPI", "EEJE"],
    (5, 4): ["LLLLL", "EWPPI", "EPPPI", "EEJJE"],
    (5, 5): ["LLLLL", "EWWPI", "EWPPI", "EPPPJ", "EEJJE"],
}


def outward(at):
    """Whether this texel is on the face of a flat part (no depth, or no width) that looks out from the head, not the
    face behind it. Both faces of a flat part sit in the same place, so the one behind is left clear: otherwise the
    two fight over which is drawn."""
    s = at.cube.size
    if s[2] == 0:
        return at.face == "front"
    if s[0] == 0:
        return at.face == ("nx" if at.cube.origin[0] < 0 else "px")
    return True


def squint_grid(n, h):
    """A shut eye squeezed tight, n texels across (inner side first) and h high: skin, with a dark line like a ">" whose
    point is at the inner corner and whose arms open towards the outer side."""
    rows = [["S"] * n for _ in range(h)]
    mid = (h - 1) / 2
    for k in range(n):
        off = (k / max(1, n - 1)) * (h - 1) / 2
        for r in {int(math.floor(mid - off + 0.5)), int(math.floor(mid + off + 0.5))}:
            rows[max(0, min(h - 1, r))][k] = "L"
    return ["".join(r) for r in rows]


def ring_grid(n, h):
    """Dazed eyes: a dark ring (an "o") where they're big enough, a wavy line where they're small."""
    if n < 3 or h < 3:
        return ["".join("L" if (r + k) % 2 == 0 else "S" for k in range(n)) for r in range(h)]
    return ["".join("L" if r in (0, h - 1) or k in (0, n - 1) else "S" for k in range(n)) for r in range(h)]


def cross_grid(n, h):
    """Fainted eyes: an "x"."""
    rows = [["S"] * n for _ in range(h)]
    for r in range(h):
        f = r / max(1, h - 1)
        for k in {int(math.floor(f * (n - 1) + 0.5)), int(math.floor((1 - f) * (n - 1) + 0.5))}:
            rows[r][k] = "L"
    return ["".join(r) for r in rows]


# The overlays an eye can have, each kept away until wanted: squeezed shut (hit), dazed (confused), fainted. Each sits a
# hair further out than the last so none fights another.
COVERS = (("squint", squint_grid, 0.05), ("dizzy", ring_grid, 0.06), ("faint", cross_grid, 0.07))


def eye_mat(w, h, iris, pupil, lash, shine, iris_light=None, white=None, side=False, grid=None, part=None, face=None, skin=None):
    """An eye's drawing on its flat part. part=(first column, columns) for an eye in two parts round a corner (the face
    part's columns run inner to outer; the side part's carry on from front to back)."""
    I, P, L, S = rgb(iris), rgb(pupil), rgb(lash), rgb(shine)
    J = rgb(iris_light) if iris_light else mix(I, "#FFFFFF", 0.35)
    E = rgb(white) if white else rgb("#F4F1EA")
    grid = grid or EYES.get((w, h))
    SK = rgb(skin) if skin else I
    # S skin (to shape the eye's outline), D the skin's shadow (a lid's crease), . nothing (the face behind shows).
    table = {"L": L, "W": S, "w": mix(S, I, 0.35), "P": P, "I": I, "J": J, "E": E, "K": mix(I, P, 0.5), "S": SK, "D": mix(SK, L, 0.4),
             ".": CLEAR}

    def f(at):
        if not outward(at):
            return CLEAR
        if part is not None:
            c0, n = part
            if face == "front":
                if at.face != "front":
                    return None
                right = at.cube.origin[0] < 0
                # The right eye's face part: its inner side is towards +x (column 0 of the face is +x).
                k = (n - 1 - at.col) if right else at.col
                k = n - 1 - k
            else:
                if at.face == "nx":
                    k = at.col
                elif at.face == "px":
                    k = at.fw - 1 - at.col
                else:
                    return None
            k += c0
        elif side:
            if at.face == "nx":
                k = at.col
            elif at.face == "px":
                k = w - 1 - at.col
            else:
                return None
        else:
            if at.face != "front":
                return None
            inner_right = at.cube.origin[0] + w / 2 < 0
            k = at.col if inner_right else (w - 1 - at.col)
        if grid is None or at.row >= len(grid) or k >= len(grid[at.row]):
            return I
        return table[grid[at.row][k]]

    return Mat(iris, f, noise=0.0, shade=0.1, edge=0)


# ---------- painting ----------

def face_rect(c):
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


def shade_for(at, c):
    m = c.mat
    if m.flat:
        k = 1.0
    elif at.face == "top":
        k = 1.07
    elif at.face == "bottom":
        k = 0.78
    else:
        k = 0.9 + 0.14 * at.ly
        if at.row == 0 and at.fh > 2 and (at.face, "r0") not in c.seams:
            k *= 1.05
    if m.edge and at.edge and at.fw > 2 and at.fh > 2:
        on = [e for e, hit in (("r0", at.row == 0), ("rl", at.row == at.fh - 1), ("c0", at.col == 0), ("cl", at.col == at.fw - 1)) if hit]
        if not all((at.face, e) in c.seams for e in on):
            k *= 1 - m.edge
    return 1 + (k - 1) * m.shade


def pack(cubes):
    """Places each cube's texels on the texture (shelves, tallest first). Returns the texture's size."""
    W = 64
    while True:
        x = y = shelf = 0
        ok = True
        for c in sorted(cubes, key=lambda c: (-(c.size[2] + c.size[1]), -(c.size[0] + c.size[2]))):
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
        if ok and H <= W:
            break
        W *= 2
    Hp = 32
    while Hp < H:
        Hp *= 2
    return W, Hp


FRAMES = 6


def paint(cubes, tex_w, tex_h, frames=FRAMES):
    """The texture, and the glowing layer's frames (or None)."""
    img = np.zeros((tex_h, tex_w, 4), np.uint8)
    glows = [np.zeros((tex_h, tex_w, 4), np.uint8) for _ in range(frames)]
    any_glow = False
    for ci, c in enumerate(cubes):
        for face, u, v, fw, fh in face_rect(c):
            for row in range(fh):
                for col in range(fw):
                    x, y, z = texel_point(c, face, col, row)
                    at = At(x, y, z, face, c, col, row, fw, fh)
                    if c.mat.frames:
                        for fi in range(frames):
                            g = c.mat.glow(at, fi)
                            if g is None:
                                continue
                            any_glow = True
                            col_rgb, alpha = g
                            glows[fi][v + row, u + col, :3] = np.clip(rgb(col_rgb), 0, 255).astype(np.uint8)
                            glows[fi][v + row, u + col, 3] = int(max(0, min(255, alpha)))
                        continue
                    base = c.mat.colour(at)
                    if base is None:
                        continue
                    k = shade_for(at, c)
                    n = (hash01(ci, face, col, row) * 2 - 1) * c.mat.noise * 255
                    img[v + row, u + col, :3] = np.clip(base * k + n, 0, 255).astype(np.uint8)
                    img[v + row, u + col, 3] = 255
                    if c.mat.glow is not None:
                        g = c.mat.glow(at)
                        if g is not None:
                            any_glow = True
                            g = rgb(g)
                            for fi in range(frames):
                                flick = 0.78 + 0.22 * math.sin(fi / frames * 2 * math.pi + hash01(ci, col, row) * 6.3)
                                glows[fi][v + row, u + col, :3] = np.clip(g * (0.9 + 0.1 * flick), 0, 255).astype(np.uint8)
                                glows[fi][v + row, u + col, 3] = int(255 * min(1, 0.55 + 0.45 * flick))
    return img, (glows if any_glow else None)


def flame_shape(at, fi, frames=FRAMES, heat=1.0):
    """A flame on a flat part, frame by frame, drawn as crisp pixel art: a tight teardrop, narrow at its base, swelling
    a little, then drawn up into two or three licking tongues. Solid bands of colour: a pale white-yellow heart, yellow,
    orange, and a red-orange rim, with no soft fading or dark edges. Gives (colour, alpha) or None (no flame there)."""
    c = at.cube
    if c.size[2] == 0:
        u = at.lx
    elif c.size[0] == 0:
        u = at.lz
    else:
        return None
    v = at.ly
    ph = fi / frames * 2 * math.pi
    seed = (c.origin[0] * 1.7 + c.origin[2] * 2.3) % 6.28
    centre = 0.5 + 0.08 * math.sin(ph + v * 3.0 + seed) * v
    widest = 0.28
    if v < widest:
        hw = 0.3 + 0.2 * math.sin(v / widest * math.pi / 2)
    else:
        hw = 0.5 * (1 - ((v - widest) / (1 - widest)) ** 0.9)
    if v > 0.45:
        # Tongues: the upper flame splits into licks that rise and fall from frame to frame.
        lick = abs(math.sin((u - centre) * math.pi * 2.0 + ph + seed))
        hw *= 0.55 + 0.45 * lick
        if v > 0.72 and lick < 0.45 + (v - 0.72):
            return None
    d = abs(u - centre)
    if d > hw or hw <= 0.04:
        return None
    k = d / hw
    heat_v = (1 - k) * 0.75 + (1 - v) * 0.55 * heat
    if heat_v > 0.98:
        return rgb("#FFF8D2"), 255
    if heat_v > 0.72:
        return rgb("#FFD640"), 255
    if heat_v > 0.45:
        return rgb("#FF9A24"), 255
    return rgb("#F2581E"), 235


def smoke_shape(at, fi, frames=FRAMES):
    """Smoke on a flat part, frame by frame: soft grey puffs drifting up off a flame, widening and thinning as they rise."""
    c = at.cube
    if c.size[2] == 0:
        u = at.lx
    elif c.size[0] == 0:
        u = at.lz
    else:
        return None
    v = at.ly
    seed = (c.origin[0] * 1.3 + c.origin[2] * 1.9) % 6.28
    p = (v * 2.5 - fi / frames) % 1.0
    puff = 1 - abs(p - 0.5) * 2
    cx = 0.5 + 0.18 * math.sin(v * 5 + seed + fi / frames * 2 * math.pi) * v
    r = (0.14 + 0.3 * v) * (0.55 + 0.45 * puff)
    d = abs(u - cx)
    if d > r or puff < 0.25:
        return None
    alpha = 80 * (1 - v) * (1 - d / r) * puff
    if alpha < 18:
        return None
    return mix("#5E5654", "#B4ACA8", v), alpha


STAR = ["....O....",
        "...OYO...",
        "...OYO...",
        "OOOOWYOOO",
        ".OYYWYYO.",
        "..OYYYO..",
        ".OYYOYYO.",
        ".OYO.OYO.",
        ".OO...OO."]


def star_shape(at, fi):
    """A five-pointed star drawn as pixel art: bright yellow, a pale streak of light down its middle, a deep gold rim."""
    col, row = int(at.lx * 9 - 1e-9), int((1 - at.ly) * 9 - 1e-9)
    ch = STAR[max(0, min(8, row))][max(0, min(8, col))]
    if ch == ".":
        return None
    if ch == "W":
        return rgb("#FFFBE0"), 255
    if ch == "O":
        return rgb("#E08A1A"), 255
    return rgb("#FFD83A"), 255


def star_mat():
    return Mat("#FFD83A", lambda a: CLEAR, glow=lambda a, fi: star_shape(a, fi), frames=True, noise=0, edge=0)


def burst_shape(at, fi):
    """A hit's impact, drawn like a comic's "pow": ten spikes, long and short by turns, round a white-hot heart, yellow,
    then orange, rimmed in red; a white shock ring spreading out behind it."""
    dx, dy = at.lx - 0.5, at.ly - 0.5
    r = math.hypot(dx, dy)
    th = math.atan2(dy, dx) + fi * 0.05
    i = int(math.floor((th + math.pi) / (2 * math.pi) * 10))
    f = ((th + math.pi) / (2 * math.pi) * 10) % 1.0
    tip = 0.47 if i % 2 == 0 else 0.33
    R = 0.17 + (tip - 0.17) * (1 - abs(f - 0.5) * 2) ** 1.6
    if r <= R:
        k = r / R
        if k < 0.38:
            return rgb("#FFFFFF"), 255
        if k < 0.66:
            return rgb("#FFE45A"), 255
        if k < 0.86:
            return rgb("#FF9A30"), 255
        return rgb("#C8301E"), 255
    ring = 0.3 + 0.03 * fi
    if abs(r - ring) < 0.025 and r < 0.5:
        return rgb("#FFFFFF"), 170
    return None


def burst_mat():
    return Mat("#FFE45A", lambda a: CLEAR, glow=lambda a, fi: burst_shape(a, fi), frames=True, noise=0, edge=0)


def ring_shape(at, fi):
    """A blow landing: a bright ring of shock spreading out, broken into dashes."""
    dx, dy = at.lx - 0.5, at.ly - 0.5
    r = math.hypot(dx, dy)
    if not 0.34 < r < 0.48:
        return None
    if int(math.floor((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 12)) % 3 == 2:
        return None
    k = abs(r - 0.41) / 0.07
    return mix("#FFFFFF", "#FFE890", k), 235 - 100 * k


def ring_mat():
    return Mat("#FFFFFF", lambda a: CLEAR, glow=lambda a, fi: ring_shape(a, fi), frames=True, noise=0, edge=0)


def claw_shape(at, fi):
    """A scratch: three bright claw marks raked diagonally through the air."""
    u, v = (at.lz, at.ly) if at.cube.size[0] == 0 else (at.lx, at.ly)
    if not 0.08 < v < 0.92:
        return None
    for c in (0.18, 0.4, 0.62):
        d = abs((u - v * 0.45) - c + 0.1)
        w = 0.045 * math.sin(math.pi * v)
        if d < w:
            return (rgb("#FFFFFF"), 255)
        if d < w + 0.035:
            return (rgb("#FFE48A"), 190)
    return None


def claw_mat():
    return Mat("#FFFFFF", lambda a: CLEAR, glow=lambda a, fi: claw_shape(a, fi), frames=True, noise=0, edge=0)


def streak_shape(at, fi):
    """A speed line: a thin white streak, bright at its front, fading out behind."""
    t = at.lz
    if (fi + int(at.cube.origin[1] * 3)) % 3 == 0 and t > 0.8:
        return None
    return mix("#FFFFFF", "#CFE8FF", t), max(40, 230 * (1 - t))


def streak_mat():
    return Mat("#FFFFFF", lambda a: CLEAR, glow=lambda a, fi: streak_shape(a, fi), frames=True, noise=0, edge=0)


def wind_shape(at, fi):
    """A gust: a ring of wind broken into three sweeping arcs, white at its heart, pale blue at its edges, turning."""
    dx, dy = at.lx - 0.5, at.ly - 0.5
    r = math.hypot(dx, dy)
    if not 0.22 < r < 0.48:
        return None
    th = (math.atan2(dy, dx) + fi * 0.5) % (2 * math.pi / 3)
    if th > 1.6:
        return None
    k = abs(r - 0.35) / 0.13
    return mix("#FFFFFF", "#A8DAF6", k), 230 - 120 * k


def wind_mat():
    return Mat("#E8F6FF", lambda a: CLEAR, glow=lambda a, fi: wind_shape(a, fi), frames=True, noise=0, edge=0)


def fang_shape(at, down):
    """A pair of great fangs, pointing down (the upper pair) or up (the lower), glinting white."""
    u, v = at.lx, at.ly
    for c in (0.3, 0.7):
        hw = 0.17 * (v if down else 1 - v) + 0.02
        if abs(u - c) < hw:
            return (mix("#FFFFFF", "#CFE6FF", abs(u - c) / hw), 255)
    return None


def fang_mat(down):
    return Mat("#FFFFFF", lambda a: CLEAR, glow=lambda a, fi: fang_shape(a, down), frames=True, noise=0, edge=0)


def blade_mat(light, dark, rib):
    """A single leaf blade, sharp at both ends, a pale rib down its middle."""
    def f(at):
        c = at.cube
        u = at.lx if c.size[1] == 0 else at.ly
        t = at.lz
        hw = 0.5 * math.sin(math.pi * min(1.0, max(0.0, t))) ** 0.8
        d = abs(u - 0.5)
        if d > hw:
            return CLEAR
        if d < 0.1:
            return rgb(rib)
        return mix(light, dark, d / max(0.01, hw))
    return Mat(light, f, noise=0.02, edge=0)


def smoke_mat():
    return Mat("#8A8280", lambda a: CLEAR, glow=lambda a, fi: smoke_shape(a, fi), frames=True, noise=0, edge=0)


def flame_mat(heat=1.0):
    """A see-through flat part that only shows its flame, glowing, in the glowing layer."""
    return Mat("#FF8A2A", lambda a: CLEAR, glow=lambda a, fi: flame_shape(a, fi, heat=heat), frames=True, noise=0, edge=0)


def flame(origin, w, h, mat=None, planes=2, heat=1.0, smoke=False):
    """A flame standing up from origin (its base's middle): crossed flat parts (two, square to each other, so it looks
    full from every side), w wide and h tall. smoke: a wisp of smoke rising off its top."""
    mat = mat or flame_mat(heat)
    x, y, z = origin
    out = []
    for i in range(planes):
        out.append(Cube([x - w / 2, y, z], [w, h, 0], mat, [0, i * 180 / planes, 0], [x, y, z]))
    if smoke:
        sm = smoke_mat()
        sw, sh, sy = max(3, round(w * 0.55)), max(5, round(h * 0.6)), y + h * 0.8
        for i in range(2):
            out.append(Cube([x - sw / 2, sy, z], [sw, sh, 0], sm, [0, 45 + i * 90, 0], [x, sy, z], effect=True))
    return out


def water_shape(at, fi, frames=FRAMES):
    """A jet of water on a flat part lying along z (its trunk end at the part's back, +z), frame by frame: a cone that
    widens away from the trunk, white at its heart and pale blue at its edges, its streaks flowing outward and
    breaking into drops towards the end. Gives (colour, alpha) or None."""
    c = at.cube
    if c.size[1] == 0:
        u = at.lx
    elif c.size[0] == 0:
        u = at.ly
    else:
        return None
    t = 1 - at.lz  # 0 at the trunk, 1 at the far end
    hw = 0.14 + 0.36 * t ** 0.8
    d = abs(u - 0.5)
    if d > hw:
        return None
    k = d / hw
    lane = round(u * 9)
    flow = (t * 2.2 - fi / frames + hash01("lane", lane) * 0.7) % 1.0
    if t > 0.3 and flow < 0.12 + 0.3 * t * k:
        return None
    if t > 0.72 and hash01("drop", lane, round(t * 12), fi) < (t - 0.72) * 2.4:
        return None
    col = mix("#FFFFFF", "#6FCFF2", min(1.0, k * 0.9 + t * 0.35))
    alpha = (235 if k < 0.45 else 235 - (k - 0.45) * 220) * (1 - max(0.0, t - 0.8) * 2.5)
    return col, max(60, alpha)


def water_mat():
    """A see-through flat part that only shows its water, in the glowing layer (so it looks wet and bright)."""
    return Mat("#BDEBFA", lambda a: CLEAR, glow=lambda a, fi: water_shape(a, fi), frames=True, noise=0, edge=0)


def jet(tip, length, w, planes=3):
    """A jet of water shooting out from tip (a trunk's end) towards -z: crossed flat parts round its line, so it looks
    full from every side, length long and w across at its widest."""
    x, y, z = tip
    mat = water_mat()
    return [Cube([x - w / 2, y, z - length], [w, 0, length], mat, [0, 0, i * 180 / planes], [x, y, z]) for i in range(planes)]


# ---------- animations (Molang) ----------

def num(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def wave(period, amp, phase=0.0, base=0.0):
    """A smooth back and forth: amp each way, once every period seconds, from phase (degrees), about base."""
    if amp == 0:
        return base
    e = f"math.sin(q.anim_time * {num(360 / period)} + {num(phase)}) * {num(amp)}"
    return e if base == 0 else f"{num(base)} + {e}"


def bounce(period, amp, phase=0.0, base=0.0):
    """Up and back down each period (a hop, or a step's lift)."""
    e = f"math.abs(math.sin(q.anim_time * {num(180 / period)} + {num(phase)})) * {num(amp)}"
    return e if base == 0 else f"{num(base)} + {e}"


def blink(at, length=0.14):
    """An eyelid's height: shut for a moment at `at` seconds into the loop, open otherwise."""
    return f"(q.anim_time > {num(at)}) * (q.anim_time < {num(at + length)})"


def keys(points):
    return {num(t): v for t, v in sorted(points.items())}


def track(**kw):
    return {k: kw[k] for k in ("rotation", "position", "scale") if kw.get(k) is not None}


def merge(*dicts):
    out = {}
    for d in dicts:
        for bone, chans in (d or {}).items():
            out.setdefault(bone, {}).update(chans)
    return out


class Anims:
    def __init__(self, sid):
        self.sid = sid
        self.all = {}

    def add(self, name, length, bones, loop=True):
        self.all[f"animation.{self.sid}.{name}"] = {"loop": loop, "animation_length": length, "bones": bones}
        return f"animation.{self.sid}.{name}"


def only(m, d):
    """These tracks, for the bones the model has."""
    return {k: v for k, v in d.items() if k in m.names}


HIDE = [0.001, 0.001, 0.001]
FX = ("squint_right", "squint_left", "dizzy_right", "dizzy_left", "faint_right", "faint_left", "fx_stars", "fx_impact")


def away(*bones):
    """Tracks keeping these parts tucked away (shrunk to nothing)."""
    return {b: track(scale=list(HIDE)) for b in bones}


def eyelids(at):
    """The eyelids' blink, with the battle overlays (squeezed, dazed and fainted eyes, the stars, the impact) kept away:
    they only show when wanted."""
    return {"eyelid_right": track(scale=[1, blink(at), 1]), "eyelid_left": track(scale=[1, blink(at), 1]), **away(*FX)}


def squeeze(length, shut=0.04, open_at=0.55):
    """Eyes squeezed shut while hit: the squint parts brought back (from nothing) for a moment."""
    L = length
    k = keys({0: [1, 1, 1], L * shut: [1000, 1000, 1000], L * open_at: [1000, 1000, 1000], L * (open_at + 0.07): [1, 1, 1]})
    return {"squint_right": track(scale=k), "squint_left": track(scale=dict(k))}


def pulse(period, amp):
    return [wave(period, amp, 0, 1), wave(period, amp * 1.4, 90, 1), wave(period, amp, 0, 1)]


# ---------- writing ----------

def place_stars(model, stands, anims=None):
    """Builds the confused stars: three crisp nine-pixel stars circling a little above the head. They're drawn at full
    detail and the confused pose scales them, so they come out the same size above every creature, big or small
    (about a third of a block), whatever the creature's own scale."""
    spec = getattr(model, "stars_spec", None)
    if not spec:
        return
    head, top, cz, hw = spec
    body_top = max(c.origin[1] + c.size[1] for c in model.cubes() if not c.effect)
    scale = stands / (body_top / 16)
    n = 9
    f = (5.0 / scale) / n            # the pose's scale for the stars: 5 world pixels across, about a third of a block
    # Above whatever solid part is highest (a shell, horns, a crest), not just the head; flames don't count.
    solid_top = max(c.origin[1] + c.size[1] for c in model.cubes() if not c.effect and not c.mat.frames)
    py = max(top, min(solid_top, top + 6)) + 6.0 / scale
    r = max(5.0 / scale, hw * 0.8) / f
    st = star_mat()
    stars = []
    for i in range(3):
        a = i * 120
        px, pz = r * math.sin(math.radians(a)), cz + r * math.cos(math.radians(a))
        for extra in (0, 90):
            stars.append(Cube([px - n / 2, py - n / 2, pz], [n, n, 0], st, [0, a + extra, 0], [px, py, pz], effect=True))
    b = next(x for x in model.bones if x.name == "fx_stars")
    b.pivot, b.cubes = [0, py, cz], stars
    for name, an in (anims.all.items() if anims is not None else []):
        if name.endswith(".confused") and "fx_stars" in an.get("bones", {}):
            an["bones"]["fx_stars"]["scale"] = [round(f, 4)] * 3


KEEP_FX = ("fx_stars", "fx_impact")


def drop_move_fx(model, anims):
    """Moves' effects come from the move library now (tools/monmoves.py: one effect per move, whoever uses it), so the
    effects a creature was built with are taken out, with their tracks. Its own motions (the wind-up, the lunge, the
    trunk aiming) stay. The confused stars and the critical-hit burst are the creature's and stay too."""
    drop = {b.name for b in model.bones if b.name.startswith("fx_") and b.name not in KEEP_FX}
    grew = True
    while grew:
        more = {b.name for b in model.bones if b.parent in drop} - drop
        grew = bool(more)
        drop |= more
    model.bones = [b for b in model.bones if b.name not in drop]
    for name, an in anims.all.items():
        bones = an.get("bones", {})
        for b in [b for b in bones if b in drop]:
            del bones[b]
        if name.endswith(".special"):
            bones.pop("spray", None)


def anchors(model):
    """Where a move's effect comes from on this creature, in its own units: its mouth (the front of its head, at the
    jaw), the top of its back, the middle of its body, and its size (width, height, length)."""
    solid = [c for c in model.cubes() if not c.effect and not c.mat.frames]
    x0 = min(c.origin[0] for c in solid); x1 = max(c.origin[0] + c.size[0] for c in solid)
    y1 = max(c.origin[1] + c.size[1] for c in solid)
    z0 = min(c.origin[2] for c in solid); z1 = max(c.origin[2] + c.size[2] for c in solid)
    by = {b.name: b for b in model.bones}
    head = by.get("head")
    hc = [c for c in head.cubes if not c.effect and not c.rotation] if head else []
    jaw = by.get("jaw") or by.get("beak_lower")
    beak = by.get("beak")
    front = min(c.origin[2] for c in (beak.cubes if beak and beak.cubes else hc)) if hc else z0
    mouth_y = jaw.pivot[1] if jaw else (min(c.origin[1] for c in hc) + 1 if hc else y1 * 0.6)
    body = by.get("body")
    bc = [c for c in body.cubes if not c.effect] if body else solid
    bz = (min(c.origin[2] for c in bc) + max(c.origin[2] + c.size[2] for c in bc)) / 2
    btop = max(c.origin[1] + c.size[1] for c in model.cubes() if not c.effect and not c.mat.frames and bz - 8 < c.origin[2] + c.size[2] / 2 < bz + 8)
    bmid = (min(c.origin[1] for c in bc) + max(c.origin[1] + c.size[1] for c in bc)) / 2
    r = lambda v: [round(a, 2) for a in v]
    return {"mouth": r([0, mouth_y, front - 0.5]), "back": r([0, btop + 1, bz]), "body": r([0, bmid, bz]),
            "size": r([x1 - x0, y1, z1 - z0])}


_stages = None


def stage_of(sid):
    """How far along its line a species is, for moves that grow as creatures evolve: 1 for the first (or a baby), 3 for
    the last of a line, 2 between (and for a species that doesn't evolve)."""
    global _stages
    if _stages is None:
        data = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "retromon", "data", "species.tsv")
        into, frm = {}, {}
        for line in open(data, encoding="utf-8"):
            if line.startswith("#") or not line.strip():
                continue
            f = line.rstrip("\n").split("\t")
            for e in (f[15].split(";") if f[15] and f[15] != "-" else []):
                p = e.split(":")
                if len(p) >= 3:
                    into.setdefault(f[1], set()).add(p[-1])
                    frm[p[-1]] = f[1]
        _stages = {}
        for s in set(into) | set(frm):
            has_before, has_after = s in frm, s in into
            _stages[s] = 2 if (has_before and has_after) or not (has_before or has_after) else (3 if has_before else 1)
    return _stages.get(sid, 2)


def write(sid, name, model, anims, info, stands):
    """Writes a creature: model, animations, info and textures. stands sets its size in the world: the height, in blocks, its
    cubes would reach unturned (set so it comes out about as big overall as the Cobblemon model it stands in for)."""
    place_stars(model, stands, anims)
    drop_move_fx(model, anims)
    cubes = model.cubes()
    tw, th = pack(cubes)
    img, glows = paint(cubes, tw, th)
    os.makedirs(os.path.join(OUT_MODELS, sid), exist_ok=True)
    os.makedirs(OUT_TEX, exist_ok=True)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT_TEX, sid + ".png"))
    for old in os.listdir(OUT_TEX):
        if old.startswith(sid + "_glow_"):
            os.remove(os.path.join(OUT_TEX, old))
    layers = []
    if glows:
        frames = []
        for i, gimg in enumerate(glows):
            Image.fromarray(gimg, "RGBA").save(os.path.join(OUT_TEX, f"{sid}_glow_{i}.png"))
            frames.append(f"retromon:textures/mons/{sid}_glow_{i}.png")
        layers.append({"frames": frames, "fps": 10, "emissive": True, "translucent": True})
    top = 0.0
    out_bones = []
    for b in model.bones:
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
            if not c.effect:
                top = max(top, c.origin[1] + c.size[1])
        if cs:
            o["cubes"] = cs
        out_bones.append(o)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.retromon.{sid}", "texture_width": tw, "texture_height": th, "visible_bounds_width": 4,
                        "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": out_bones}]}
    json.dump(geo, open(os.path.join(OUT_MODELS, sid, "model.geo.json"), "w"), indent=1)
    json.dump({"format_version": "1.8.0", "animations": anims.all}, open(os.path.join(OUT_MODELS, sid, "anim.json"), "w"), indent=1)
    height = round(top / 16, 3)
    info = dict(info)
    info["scale"] = round(stands / height, 3)
    info["height"] = height
    info["texture"] = f"retromon:textures/mons/{sid}.png"
    info["layers"] = layers
    info["name"] = name
    info["anchors"] = anchors(model)
    info["stage"] = stage_of(sid)
    info.setdefault("parts", [])
    json.dump(info, open(os.path.join(OUT_MODELS, sid, "info.json"), "w"), indent=1)
    used = int((img[..., 3] > 0).sum())
    print(f"{sid:11s} {name:12s} {len(cubes):3d} cubes {len(model.bones):3d} bones  texture {tw}x{th} ({used} texels)  stands {stands} blocks")
