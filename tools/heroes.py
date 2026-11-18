#!/usr/bin/env python3
"""
The main characters as proper 64x64 player skins, using both layers (the outer layer stands proud of the body,
so hair can spike out, a coat can hang, a cap can have a brim): Professor Oak, the boy and the girl you play,
and your rival. Drawn in the spirit of the later games' designs (not copies of them), shaded so they read
well in 3D. Writes textures/entity/npc/oak.png and blue.png, textures/entity/player/boy.png and girl.png,
and a flat preview of each (front and back) to tools/art/heroes_preview.png.
"""
import os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon", "textures", "entity")

# part: texture offset (u, v) and box size (w, h, d)
PARTS = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4),
}
SIDES = ("right", "front", "left", "back")


def C(hexs, a=255):
    hexs = hexs.lstrip("#")
    return (int(hexs[0:2], 16), int(hexs[2:4], 16), int(hexs[4:6], 16), a)


def mul(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3])


class Skin:
    def __init__(self, seed):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.p = self.im.load()
        self.rng = random.Random(seed)

    def face(self, part, face):
        u, v, w, h, d = PARTS[part]
        return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}[face]

    def set(self, part, face, x, y, c):
        fx, fy, fw, fh = self.face(part, face)
        if 0 <= x < fw and 0 <= y < fh:
            self.p[fx + x, fy + y] = c

    def get(self, part, face, x, y):
        fx, fy, fw, fh = self.face(part, face)
        return self.p[fx + x, fy + y]

    def fill(self, part, face, c, x0=0, y0=0, x1=None, y1=None, grain=0.0, light=0.0):
        """A face (or a box of it) in a colour, with a little cloth grain and lit a touch from the top."""
        fx, fy, fw, fh = self.face(part, face)
        x1 = fw if x1 is None else x1
        y1 = fh if y1 is None else y1
        for y in range(y0, y1):
            for x in range(x0, x1):
                k = 1 + (self.rng.random() - 0.5) * grain - light * (y / max(1, fh - 1) - 0.4)
                self.set(part, face, x, y, mul(c, k) if c[3] else c)

    def box(self, part, c, grain=0.06, light=0.12, top=None, bottom=None):
        for f in SIDES:
            # Sides a little darker than the front, the back darker still.
            k = {"front": 1.0, "right": 0.9, "left": 0.9, "back": 0.82}[f]
            self.fill(part, f, mul(c, k), grain=grain, light=light)
        self.fill(part, "top", mul(top or c, 1.06), grain=grain)
        self.fill(part, "bottom", mul(bottom or c, 0.75), grain=grain)

    def ring(self, part, y0, y1, c, grain=0.06):
        """Rows y0..y1 all the way round the part."""
        for f in SIDES:
            k = {"front": 1.0, "right": 0.9, "left": 0.9, "back": 0.82}[f]
            self.fill(part, f, mul(c, k), 0, y0, None, y1, grain=grain)

    def clear(self, part):
        for f in SIDES + ("top", "bottom"):
            fx, fy, fw, fh = self.face(part, f)
            for y in range(fh):
                for x in range(fw):
                    self.p[fx + x, fy + y] = (0, 0, 0, 0)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.im.save(path)


# ---------- shared bits ----------

def face_features(s, eye, skin, brow, mouth=None, lash=False, blush=None, eye_y=4):
    """Eyes (white and iris, two tall), brows, a mouth and a nose shadow on the head's front."""
    W = C("f6f7fb")
    for x in (1, 6):
        s.set("head", "front", x, eye_y, W)
        s.set("head", "front", x, eye_y + 1, W)
    for x in (2, 5):
        s.set("head", "front", x, eye_y, mul(eye, 1.25))
        s.set("head", "front", x, eye_y + 1, eye)
    for x in (1, 2):
        s.set("head", "front", x, eye_y - 1, brow)
    for x in (5, 6):
        s.set("head", "front", x, eye_y - 1, brow)
    if lash:
        s.set("head", "front", 0, eye_y, brow)
        s.set("head", "front", 7, eye_y, brow)
    s.set("head", "front", 3, eye_y + 2, mul(skin, 0.86))
    s.set("head", "front", 4, eye_y + 2, mul(skin, 0.9))
    m = mouth or mul(skin, 0.7)
    s.set("head", "front", 3, eye_y + 3, m)
    s.set("head", "front", 4, eye_y + 3, m)
    if blush:
        s.set("head", "front", 1, eye_y + 2, blush)
        s.set("head", "front", 6, eye_y + 2, blush)


def head_skin(s, skin):
    s.box("head", skin, grain=0.03, light=0.1)
    # The ears: a shade on the sides.
    for f in ("right", "left"):
        s.set("head", f, 3 if f == "right" else 4, 4, mul(skin, 0.8))
        s.set("head", f, 3 if f == "right" else 4, 5, mul(skin, 0.8))


def hair_cap(s, hair, depth_front=1, depth_side=3, depth_back=6):
    """Hair on the head itself: the top, a fringe, down the sides and the back."""
    s.fill("head", "top", hair, grain=0.12)
    s.fill("head", "front", hair, 0, 0, 8, depth_front, grain=0.12)
    for f in ("right", "left"):
        s.fill("head", f, mul(hair, 0.92), 0, 0, 8, depth_side, grain=0.12)
    s.fill("head", "back", mul(hair, 0.85), 0, 0, 8, depth_back, grain=0.12)


def spikes(s, hair, front_rows, side_rows, back_rows, seed=0, top=True, tint=1.0):
    """Hair on the outer layer standing out round the head, ragged at the edges, so it reads as spikes in 3D."""
    rng = random.Random(seed)
    if top:
        s.fill("hat", "top", mul(hair, 1.05 * tint), grain=0.18)
    for f, rows in (("front", front_rows), ("right", side_rows), ("left", side_rows), ("back", back_rows)):
        k = {"front": 1.08, "right": 0.95, "left": 0.95, "back": 0.86}[f] * tint
        for x in range(8):
            # Each column hangs to its own length, so the edge is jagged.
            n = rows[x] if isinstance(rows, (list, tuple)) else max(0, rows + rng.choice((-1, 0, 0, 1)))
            for y in range(n):
                hl = 1.12 if y == 0 else 1.0
                s.set("hat", f, x, y, mul(hair, k * hl * (1 + (rng.random() - 0.5) * 0.16)))


def hands(s, skin, rows=2):
    for arm in ("rarm", "larm"):
        s.ring(arm, 12 - rows, 12, skin, grain=0.03)
        s.fill(arm, "bottom", mul(skin, 0.8))


def shoes(s, c, sole, rows=2):
    for leg in ("rleg", "lleg"):
        s.ring(leg, 12 - rows, 12, c, grain=0.05)
        s.ring(leg, 11, 12, sole, grain=0.02)
        s.fill(leg, "bottom", sole)


# ---------- Professor Oak ----------

def oak():
    s = Skin(1)
    SKIN, HAIR, BROW = C("e8b48e"), C("b9bcc4"), C("8e9098")
    head_skin(s, SKIN)
    hair_cap(s, HAIR, depth_front=1, depth_side=3, depth_back=7)
    face_features(s, C("4a3426"), SKIN, BROW, mouth=C("a8634e"))
    # A few lines: he's a professor of some years.
    s.set("head", "front", 0, 6, mul(SKIN, 0.9))
    s.set("head", "front", 7, 6, mul(SKIN, 0.9))
    # Swept-back silver hair, spiky at the back and sides, a peak at the front.
    spikes(s, HAIR, [2, 1, 1, 2, 2, 1, 1, 2], [3, 3, 4, 4, 5, 5, 6, 6], [6, 7, 7, 8, 8, 7, 7, 6], seed=3)
    for x in (2, 5):
        s.set("hat", "front", x, 1, mul(HAIR, 1.18))
    # The shirt: a deep red, with a collar at the neck.
    SHIRT, COAT, TROUSERS = C("a8332f"), C("f3f3ee"), C("8a6a48")
    s.box("body", SHIRT)
    s.box("rarm", COAT)
    s.box("larm", COAT)
    hands(s, SKIN, 2)
    s.box("rleg", TROUSERS)
    s.box("lleg", TROUSERS)
    shoes(s, C("5a3a24"), C("2c2018"))
    # The lab coat on the outer layer: open down the front over the red shirt, lapels, pockets, a pen.
    s.box("jacket", COAT, grain=0.04)
    for y in range(12):
        for x in (3, 4):
            s.set("jacket", "front", x, y, (0, 0, 0, 0))
    for y in range(0, 4):
        s.set("jacket", "front", 2, y, mul(COAT, 0.86))
        s.set("jacket", "front", 5, y, mul(COAT, 0.86))
    for y in range(4, 12):
        s.set("jacket", "front", 2, y, mul(COAT, 0.93))
        s.set("jacket", "front", 5, y, mul(COAT, 0.93))
    s.fill("body", "front", C("f1e6d8"), 3, 0, 5, 2)
    s.set("body", "front", 3, 2, mul(SHIRT, 0.8))
    s.set("body", "front", 4, 2, mul(SHIRT, 0.8))
    for x in (0, 1):
        s.set("jacket", "front", x, 8, mul(COAT, 0.8))
    for x in (6, 7):
        s.set("jacket", "front", x, 8, mul(COAT, 0.8))
    s.set("jacket", "front", 6, 5, C("3060c0"))
    s.set("jacket", "front", 6, 4, C("d0d0d8"))
    for arm in ("rsleeve", "lsleeve"):
        s.box(arm, COAT, grain=0.04)
        s.ring(arm, 10, 12, (0, 0, 0, 0))
        s.ring(arm, 9, 10, mul(COAT, 0.88))
    # The coat's tails hang to his knees.
    for leg in ("rpants", "lpants"):
        s.ring(leg, 0, 5, COAT, grain=0.04)
        s.ring(leg, 5, 6, mul(COAT, 0.85))
    for y in range(6):
        s.set("rpants", "front", 3, y, (0, 0, 0, 0))
        s.set("lpants", "front", 0, y, (0, 0, 0, 0))
    s.save(os.path.join(ROOT, "npc", "oak.png"))
    return s


# ---------- the boy ----------

def boy():
    s = Skin(2)
    SKIN, HAIR, BROW = C("f0c39e"), C("2b2422"), C("2b2422")
    head_skin(s, SKIN)
    hair_cap(s, HAIR, depth_front=2, depth_side=3, depth_back=6)
    face_features(s, C("2f5fa8"), SKIN, BROW, mouth=C("b56a58"))
    # A cap: red crown, a white front panel with a little ball mark, a dark brim (outer layer).
    RED, WHITE, BRIM = C("d23a3a"), C("f4f4f2"), C("23252b")
    s.fill("hat", "top", RED, grain=0.08)
    for f in ("right", "left", "back"):
        s.fill("hat", f, mul(RED, 0.9 if f != "back" else 0.82), 0, 0, 8, 2, grain=0.08)
    s.fill("hat", "front", WHITE, 0, 0, 8, 2, grain=0.04)
    s.set("hat", "front", 3, 0, RED)
    s.set("hat", "front", 4, 0, RED)
    s.set("hat", "front", 3, 1, C("23252b"))
    s.set("hat", "front", 4, 1, C("23252b"))
    s.fill("hat", "front", BRIM, 0, 2, 8, 3)
    for x in (0, 7):
        s.set("hat", "front", x, 2, mul(BRIM, 1.3))
    # Dark hair spiking out under the cap at the sides and the back.
    spikes(s, HAIR, [0] * 8, [3, 3, 4, 4, 5, 4, 4, 3], [3, 4, 5, 6, 6, 5, 4, 3], seed=5, top=False)
    for f in ("right", "left", "back"):
        for x in range(8):
            for y in range(2):
                pass
    for f in ("right", "left"):
        s.fill("hat", f, mul(RED, 0.9), 0, 0, 8, 2, grain=0.08)
    s.fill("hat", "back", mul(RED, 0.82), 0, 0, 8, 2, grain=0.08)
    s.fill("hat", "back", C("e8e8e6"), 3, 2, 5, 3)
    # A black tee under a short-sleeved jacket: navy with white panels and a teal trim.
    TEE, JACKET, PANEL, TRIM = C("1e1f24"), C("27407a"), C("eef0f2"), C("2bb3a3")
    s.box("body", TEE)
    s.box("jacket", JACKET, grain=0.05)
    for y in range(12):
        for x in (3, 4):
            s.set("jacket", "front", x, y, (0, 0, 0, 0))
    for y in range(0, 12):
        s.set("jacket", "front", 2, y, TRIM)
        s.set("jacket", "front", 5, y, TRIM)
    for f in ("right", "left"):
        s.fill("jacket", f, mul(PANEL, 0.92), 0, 0, 4, 5)
    s.fill("jacket", "back", mul(PANEL, 0.86), 0, 0, 8, 3)
    s.ring("jacket", 11, 12, mul(JACKET, 0.8))
    # Backpack straps over the shoulders, and the pack itself on the back (yellow).
    STRAP, PACK = C("3a2c22"), C("f2c230")
    for y in range(0, 8):
        s.set("jacket", "front", 1, y, STRAP)
        s.set("jacket", "front", 6, y, STRAP)
    s.fill("jacket", "back", PACK, 1, 2, 7, 10, grain=0.08)
    s.fill("jacket", "back", mul(PACK, 0.8), 1, 6, 7, 7)
    s.set("jacket", "back", 4, 4, C("3a2c22"))
    # Arms: the jacket's short sleeves (white with a teal cuff), bare forearms, a green wristband.
    for arm, sl in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        s.box(arm, SKIN, grain=0.03)
        s.ring(arm, 0, 4, PANEL)
        s.box(sl, PANEL, grain=0.04)
        s.ring(sl, 4, 12, (0, 0, 0, 0))
        s.fill(sl, "bottom", (0, 0, 0, 0))
        s.ring(sl, 3, 4, TRIM)
        s.ring(arm, 9, 10, C("3fa34d"))
    hands(s, SKIN, 2)
    # Charcoal jeans, rolled at the cuff, and red and white trainers.
    JEANS = C("4a4f5c")
    for leg in ("rleg", "lleg"):
        s.box(leg, JEANS, grain=0.08)
        s.ring(leg, 8, 9, mul(JEANS, 1.25))
    shoes(s, C("e04444"), C("f2f2f2"), 3)
    for leg in ("rleg", "lleg"):
        s.set(leg, "front", 1, 9, C("f2f2f2"))
        s.set(leg, "front", 2, 9, C("f2f2f2"))
    s.save(os.path.join(ROOT, "player", "boy.png"))
    return s


# ---------- the girl ----------

def girl():
    s = Skin(3)
    SKIN, HAIR, BROW = C("f3c9a8"), C("7a4a2a"), C("5a3420")
    head_skin(s, SKIN)
    hair_cap(s, HAIR, depth_front=2, depth_side=6, depth_back=8)
    face_features(s, C("3a7a5a"), SKIN, BROW, mouth=C("d0706a"), lash=True, blush=C("f2a9a0"))
    # A white sun hat with a red band (outer layer), long hair under it.
    HAT, BAND = C("f6f6f2"), C("d83a4a")
    s.fill("hat", "top", HAT, grain=0.05)
    for f in SIDES:
        s.fill("hat", f, mul(HAT, {"front": 1.0, "right": 0.92, "left": 0.92, "back": 0.86}[f]), 0, 0, 8, 1, grain=0.05)
        s.fill("hat", f, BAND if f != "back" else mul(BAND, 0.85), 0, 1, 8, 2)
    # Long hair down the sides and the back, past the shoulders (sides of the hat layer and the jacket's back).
    for f in ("right", "left"):
        for x in range(8):
            for y in range(2, 8):
                s.set("hat", f, x, y, mul(HAIR, 0.92 * (1.06 if y == 2 else 1)))
    for x in range(8):
        for y in range(2, 8):
            s.set("hat", "back", x, y, mul(HAIR, 0.84))
    for x in (0, 1, 6, 7):
        s.set("hat", "front", x, 2, mul(HAIR, 1.05))
    # A fresh blue sleeveless top with a white collar, a yellow bag strap across.
    TOP, COLLAR, SKIRT = C("3fa7d6"), C("f4f4f2"), C("d83a4a")
    s.box("body", TOP)
    s.fill("body", "front", COLLAR, 2, 0, 6, 1)
    s.fill("body", "front", mul(TOP, 0.85), 3, 1, 5, 2)
    for i in range(8):
        s.set("body", "front", 7 - i, i + 1, C("f2c230"))
    s.fill("jacket", "back", mul(HAIR, 0.84), 1, 0, 7, 6, grain=0.1)
    for x in (1, 3, 5):
        s.set("jacket", "back", x, 6, mul(HAIR, 0.8))
    # A red skirt flaring over the hips (on the outer layer, below the waist).
    s.ring("body", 9, 12, SKIRT)
    s.ring("jacket", 9, 12, mul(SKIRT, 1.05))
    s.ring("jacket", 11, 12, mul(SKIRT, 0.8))
    for leg in ("rpants", "lpants"):
        s.ring(leg, 0, 2, SKIRT)
        s.ring(leg, 1, 2, mul(SKIRT, 0.82))
    for x in (1, 4, 7):
        s.set("jacket", "front", x, 10, mul(SKIRT, 0.8))
    # Bare arms with a white wristband; legs in long white socks, red shoes.
    for arm in ("rarm", "larm"):
        s.box(arm, SKIN, grain=0.03)
        s.ring(arm, 0, 1, TOP)
        s.ring(arm, 9, 10, C("f4f4f2"))
    hands(s, SKIN, 2)
    for leg in ("rleg", "lleg"):
        s.box(leg, SKIN, grain=0.03)
        s.ring(leg, 0, 2, SKIRT)
        s.ring(leg, 5, 10, C("f4f4f2"))
    shoes(s, C("d83a4a"), C("f2f2f2"), 2)
    s.save(os.path.join(ROOT, "player", "girl.png"))
    return s


# ---------- the rival ----------

def rival():
    s = Skin(4)
    SKIN, HAIR, BROW = C("f0c09a"), C("b0662c"), C("7a3f1a")
    head_skin(s, SKIN)
    hair_cap(s, HAIR, depth_front=2, depth_side=4, depth_back=7)
    face_features(s, C("3a2e2a"), SKIN, BROW, mouth=C("a8584a"))
    # A smirk.
    s.set("head", "front", 5, 7, mul(SKIN, 0.72))
    # Big spiky hair: tall at the top and swept up at the front.
    spikes(s, HAIR, [3, 1, 2, 3, 2, 1, 2, 3], [4, 5, 4, 6, 5, 6, 5, 6], [7, 8, 6, 8, 8, 6, 8, 7], seed=9)
    for x in (1, 3, 5, 7):
        s.set("hat", "top", x, 0, (0, 0, 0, 0))
    for x in (0, 2, 4, 6):
        s.set("hat", "top", x, 7, mul(HAIR, 1.2))
    # A purple shirt with a pendant, dark trousers.
    SHIRT, TROUSERS = C("6a4aa8"), C("2e3140")
    s.box("body", SHIRT)
    s.box("jacket", mul(SHIRT, 1.05), grain=0.05)
    s.fill("jacket", "front", (0, 0, 0, 0), 2, 0, 6, 3)
    s.fill("body", "front", SKIN, 3, 0, 5, 1)
    s.set("body", "front", 3, 2, C("e0c050"))
    s.set("body", "front", 4, 2, C("e0c050"))
    s.set("body", "front", 2, 1, C("c8c8c8"))
    s.set("body", "front", 5, 1, C("c8c8c8"))
    s.ring("jacket", 11, 12, mul(SHIRT, 0.75))
    for arm, sl in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        s.box(arm, SHIRT)
        s.box(sl, mul(SHIRT, 1.05), grain=0.05)
        s.ring(sl, 10, 12, (0, 0, 0, 0))
    hands(s, SKIN, 2)
    for leg in ("rleg", "lleg"):
        s.box(leg, TROUSERS, grain=0.07)
    shoes(s, C("6a4a30"), C("2a2018"))
    s.save(os.path.join(ROOT, "npc", "blue.png"))
    return s


def preview(skins):
    """Front and back of each, flat, at 8x, for checking."""
    def doll(s, back):
        im = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
        side = "back" if back else "front"
        def paste(part, x, y):
            fx, fy, fw, fh = s.face(part, side)
            tile = s.im.crop((fx, fy, fx + fw, fy + fh))
            if back:
                tile = tile.transpose(Image.FLIP_LEFT_RIGHT)
            im.alpha_composite(tile, (x, y))
        arms = (("larm", "lsleeve", 0), ("rarm", "rsleeve", 12)) if back else (("rarm", "rsleeve", 0), ("larm", "lsleeve", 12))
        legs = (("lleg", "lpants", 4), ("rleg", "rpants", 8)) if back else (("rleg", "rpants", 4), ("lleg", "lpants", 8))
        for base, over, x in legs:
            paste(base, x, 20)
            paste(over, x, 20)
        paste("body", 4, 8)
        paste("jacket", 4, 8)
        for base, over, x in arms:
            paste(base, x, 8)
            paste(over, x, 8)
        paste("head", 4, 0)
        paste("hat", 4, 0)
        return im
    out = Image.new("RGBA", (len(skins) * 40, 36), (40, 48, 70, 255))
    for i, s in enumerate(skins):
        out.alpha_composite(doll(s, False), (i * 40 + 2, 2))
        out.alpha_composite(doll(s, True), (i * 40 + 20, 2))
    out = out.resize((out.width * 8, out.height * 8), Image.NEAREST)
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "art", "heroes_preview.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)
    return path


if __name__ == "__main__":
    print(preview([oak(), boy(), girl(), rival()]))
