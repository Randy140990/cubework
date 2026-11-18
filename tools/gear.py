#!/usr/bin/env python3
"""
The hero's gear, as model textures (client/GearLayer.java builds the boxes these wrap):

  textures/gear/cap_<pattern>.png   64x32: the cap's crown, dome, brim and button, and the pattern's extras
                                    (ears, a bulb, a flame) laid out where GearLayer's boxes read them.
  textures/gear/bag_base.png        64x32: the bags (backpack and satchel) in greys, tinted to your colour in game.
  textures/gear/bag_detail.png      64x32: what isn't tinted: buckles, zips, the Poké Ball on the flap, stitching.
  textures/gear/device.png          32x16: the Pokédex in your hands (red, a screen, the blue lens).
  tools/art/gear_preview.png        every cap's front, top and back, and the bags, side by side.

Each box's faces are laid out as Minecraft does: for a box w x h x d at (u, v), the top is at (u+d, v) and the
sides run along the row below it: right (u, v+d), front (u+d, v+d), left (u+d+w, v+d), back (u+2d+w, v+d).
"""
import os, random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon", "textures", "gear")
ART = os.path.join(os.path.dirname(os.path.abspath(__file__)), "art")

# The cap's boxes: name -> (u, v, w, h, d)
CAP = {"crown": (0, 0, 9, 4, 9), "dome": (36, 0, 7, 1, 7), "brim": (0, 14, 9, 1, 5), "button": (28, 14, 1, 1, 1), "ear": (32, 16, 2, 4, 1),
       "bulb": (40, 16, 4, 3, 4), "flame": (32, 24, 3, 4, 1)}
BAG = {"main": (0, 0, 7, 9, 4), "flap": (0, 13, 7, 4, 1), "side": (22, 0, 1, 4, 3), "pocket": (16, 13, 5, 3, 1), "strap": (32, 0, 1, 8, 1),
       "shoulder": (36, 0, 1, 1, 5), "satchel": (0, 20, 5, 5, 2), "sling": (48, 0, 1, 14, 1)}


def C(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mul(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3])


class Tex:
    def __init__(self, w, h, boxes, seed=1):
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.p = self.im.load()
        self.boxes = boxes
        self.rng = random.Random(seed)

    def face(self, box, f):
        u, v, w, h, d = self.boxes[box]
        return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}[f]

    def fill(self, box, f, c, x0=0, y0=0, x1=None, y1=None, grain=0.05, light=0.0):
        fx, fy, fw, fh = self.face(box, f)
        x1 = fw if x1 is None else x1
        y1 = fh if y1 is None else y1
        for y in range(y0, y1):
            for x in range(x0, x1):
                if 0 <= x < fw and 0 <= y < fh:
                    k = 1 + (self.rng.random() - 0.5) * 2 * grain + light * (1 - y / max(1, fh - 1) * 2) * 0.5
                    self.p[fx + x, fy + y] = mul(c, k)

    def set(self, box, f, x, y, c):
        fx, fy, fw, fh = self.face(box, f)
        if 0 <= x < fw and 0 <= y < fh:
            self.p[fx + x, fy + y] = c

    def box(self, box, c, grain=0.05, shade=True):
        for f, k in (("top", 1.08), ("bottom", 0.7), ("right", 0.9), ("left", 0.9), ("front", 1.0), ("back", 0.86)):
            self.fill(box, f, mul(c, k if shade else 1), grain=grain)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.im.save(path)


# ---------- caps ----------

def ball_logo(t, box, f, x0, y0, top, bottom, line=C("25262b")):
    """A 4x4 Poké Ball on a face, its top-left at (x0, y0)."""
    for x in range(4):
        for y in range(4):
            if (x in (0, 3)) and (y in (0, 3)):
                continue
            t.set(box, f, x0 + x, y0 + y, top if y < 2 else bottom)
    t.set(box, f, x0 + 1, y0 + 2, line)
    t.set(box, f, x0 + 2, y0 + 2, line)
    t.set(box, f, x0 + 0, y0 + 2, line)
    t.set(box, f, x0 + 3, y0 + 2, line)


def cap_base(seed, crown, panel=None, brim=None, button=None):
    t = Tex(64, 32, CAP, seed)
    t.box("crown", crown)
    t.box("dome", crown)
    t.box("button", button or mul(crown, 0.85))
    t.box("brim", brim or mul(crown, 0.8))
    if panel:
        t.fill("crown", "front", panel, 1, 0, 8, 4, grain=0.03)
        t.fill("dome", "front", panel, 1, 0, 6, 1, grain=0.03)
    # Seams: a slightly darker line down the middle of the top and the sides.
    for y in range(9):
        t.set("crown", "top", 4, y, mul(crown, 0.9))
    for y in range(7):
        t.set("dome", "top", 3, y, mul(crown, 0.9))
    # The brim's underside darker, its edge lighter.
    t.fill("brim", "bottom", mul(brim or crown, 0.55), grain=0.03)
    t.fill("brim", "front", mul(brim or crown, 1.1), grain=0.02)
    # The back strap: a little gap and a buckle.
    t.fill("crown", "back", mul(crown, 0.55), 3, 2, 6, 4)
    t.set("crown", "back", 4, 3, C("d8c070"))
    return t


def cap_pokeball():
    RED, WHITE = C("d6383a"), C("f3f3f0")
    t = cap_base(11, RED, panel=WHITE, brim=C("26282e"), button=RED)
    ball_logo(t, "crown", "front", 3, 0, RED, WHITE)
    return t


def cap_pikachu():
    Y, BR, RED, BLK = C("f6d23c"), C("8a5a2a"), C("e2463c"), C("2a2a2a")
    t = cap_base(12, Y, brim=C("e4b52a"), button=Y)
    # Brown stripes across the back, red cheeks on the sides, a little face on the front.
    for x in range(9):
        t.set("crown", "back", x, 0, BR)
        t.set("crown", "back", x, 1, mul(Y, 0.9))
    for x in range(1, 8):
        t.set("dome", "back", x, 0, BR)
    for f in ("right", "left"):
        t.fill("crown", f, RED, 3, 1, 6, 3, grain=0.02)
    t.set("crown", "front", 2, 1, BLK)
    t.set("crown", "front", 6, 1, BLK)
    t.set("crown", "front", 2, 0, C("ffffff"))
    t.set("crown", "front", 6, 0, C("ffffff"))
    t.set("crown", "front", 4, 2, C("7a4a2a"))
    # Ears: yellow with black tips.
    t.box("ear", Y)
    for f in ("front", "back", "right", "left"):
        t.fill("ear", f, BLK, 0, 0, 2, 1, grain=0.02)
    t.fill("ear", "top", BLK, grain=0.02)
    return t


def cap_bulbasaur():
    TEAL, SPOT, BULB, LEAF = C("6cc6a8"), C("3c8a72"), C("5aa84a"), C("2f7a3c")
    t = cap_base(13, TEAL, brim=C("4aa88a"), button=BULB)
    rng = random.Random(3)
    for f, (w, h) in (("top", (9, 9)), ("right", (9, 4)), ("left", (9, 4)), ("back", (9, 4))):
        for _ in range(4 if f == "top" else 2):
            x, y = rng.randrange(w - 1), rng.randrange(h - 1)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                t.set("crown", f, x + dx, y + dy, SPOT)
    # Red eyes and a smile on the front.
    t.set("crown", "front", 2, 1, C("d03a3a"))
    t.set("crown", "front", 6, 1, C("d03a3a"))
    t.box("bulb", BULB, grain=0.08)
    for f in ("front", "back", "right", "left"):
        for x in range(4):
            t.set("bulb", f, x, 0, LEAF)
        t.set("bulb", f, 1, 1, LEAF)
        t.set("bulb", f, 2, 2, mul(BULB, 1.15))
    t.fill("bulb", "top", LEAF, grain=0.1)
    t.set("bulb", "top", 1, 1, mul(BULB, 1.2))
    t.set("bulb", "top", 2, 2, mul(BULB, 1.2))
    return t


def cap_charmander():
    OR, CREAM, FL, FL2 = C("f2883a"), C("f6dc98"), C("f6c43a"), C("e2462e")
    t = cap_base(14, OR, panel=None, brim=C("f6dc98"), button=OR)
    t.set("crown", "front", 2, 1, C("2a3a6a"))
    t.set("crown", "front", 6, 1, C("2a3a6a"))
    t.fill("crown", "front", CREAM, 3, 3, 6, 4)
    t.box("flame", FL)
    for f in ("front", "back", "right", "left"):
        for y in range(4):
            t.set("flame", f, 0, y, FL2)
            t.set("flame", f, 2 if f in ("front", "back") else 0, y, FL2)
        t.set("flame", f, 1, 0, C("fff2a0"))
    return t


def cap_squirtle():
    BL, SHELL, RIM = C("6ab8e8"), C("a8642c"), C("f2e6b0")
    t = cap_base(15, BL, brim=C("4a98d0"), button=SHELL)
    # A shell over the top: brown with a cream rim and its pattern.
    t.fill("crown", "top", SHELL, grain=0.07)
    t.fill("dome", "top", SHELL, grain=0.07)
    for i in range(9):
        t.set("crown", "top", i, 0, RIM)
        t.set("crown", "top", i, 8, RIM)
        t.set("crown", "top", 0, i, RIM)
        t.set("crown", "top", 8, i, RIM)
    for x, y in ((2, 3), (3, 2), (5, 2), (6, 3), (4, 5), (2, 6), (6, 6)):
        t.set("crown", "top", x, y, mul(SHELL, 0.7))
    for f in ("back", "right", "left"):
        t.fill("crown", f, SHELL, 0, 0, 9, 1)
        t.fill("crown", f, RIM, 0, 1, 9, 2)
    t.fill("dome", "back", SHELL)
    t.set("crown", "front", 2, 1, C("7a3a2a"))
    t.set("crown", "front", 6, 1, C("7a3a2a"))
    return t


def cap_eevee():
    BR, CREAM, DARK = C("a8743e"), C("f2e2b8"), C("6a4424")
    t = cap_base(16, BR, brim=CREAM, button=BR)
    # A fluffy cream band round the bottom.
    for f in ("front", "right", "left", "back"):
        t.fill("crown", f, CREAM, 0, 3, 9, 4, grain=0.12)
    t.set("crown", "front", 2, 1, C("3a2414"))
    t.set("crown", "front", 6, 1, C("3a2414"))
    t.box("ear", BR)
    for f in ("front",):
        t.fill("ear", f, DARK, 0, 1, 2, 4)
    t.fill("ear", "top", mul(BR, 0.9))
    return t


def cap_gengar():
    PU, RED, WHITE = C("6a4a9a"), C("e23a4a"), C("f6f6f6")
    t = cap_base(17, PU, brim=C("553a80"), button=PU)
    t.set("crown", "front", 2, 1, RED)
    t.set("crown", "front", 3, 1, RED)
    t.set("crown", "front", 5, 1, RED)
    t.set("crown", "front", 6, 1, RED)
    for x in range(2, 7):
        t.set("crown", "front", x, 3, WHITE)
    t.box("ear", PU)
    t.fill("ear", "top", mul(PU, 0.8))
    return t


def cap_snorlax():
    TE, CREAM = C("2c6a7a"), C("f2e4c2")
    t = cap_base(18, TE, panel=CREAM, brim=C("23525e"), button=TE)
    for x in (2, 3, 5, 6):
        t.set("crown", "front", x, 1, C("2a2a2a"))
    t.set("crown", "front", 4, 3, C("c08a8a"))
    return t


def cap_greatball():
    BL, RED, WHITE = C("3a72d0"), C("e2423c"), C("f3f3f0")
    t = cap_base(19, BL, panel=WHITE, brim=C("26282e"), button=BL)
    for f in ("right", "left"):
        t.fill("crown", f, RED, 2, 0, 4, 3)
        t.fill("crown", f, RED, 5, 0, 7, 3)
    for y in range(9):
        t.set("crown", "top", 2, y, RED)
        t.set("crown", "top", 6, y, RED)
    ball_logo(t, "crown", "front", 3, 0, BL, WHITE)
    return t


def cap_ultraball():
    BK, YEL, WHITE = C("2c2c32"), C("f2c63a"), C("f3f3f0")
    t = cap_base(20, BK, panel=WHITE, brim=C("1c1c20"), button=YEL)
    for f in ("right", "left"):
        t.fill("crown", f, YEL, 1, 0, 3, 3)
        t.fill("crown", f, YEL, 6, 0, 8, 3)
        t.fill("crown", f, YEL, 3, 1, 6, 2)
    for y in range(9):
        t.set("crown", "top", 1, y, YEL)
        t.set("crown", "top", 7, y, YEL)
    ball_logo(t, "crown", "front", 3, 0, BK, WHITE)
    return t


def cap_plain():
    # In greys: tinted to your hat colour in game (the white ball mark stays light).
    G, WHITE = C("e8e8e8"), C("ffffff")
    t = cap_base(21, G, panel=None, brim=C("c4c4c4"), button=C("d0d0d0"))
    ball_logo(t, "crown", "front", 3, 0, WHITE, C("bdbdbd"), line=C("6a6a6a"))
    return t


CAPS = [("pokeball", cap_pokeball), ("pikachu", cap_pikachu), ("bulbasaur", cap_bulbasaur), ("charmander", cap_charmander),
        ("squirtle", cap_squirtle), ("eevee", cap_eevee), ("gengar", cap_gengar), ("snorlax", cap_snorlax), ("greatball", cap_greatball),
        ("ultraball", cap_ultraball), ("plain", cap_plain)]


# ---------- the bags ----------

def bags():
    base = Tex(64, 32, BAG, 30)
    G = C("e6e6e6")
    for b in BAG:
        base.box(b, G, grain=0.06)
    # The main pack: rounded at the top (darker corners), a seam round the edge.
    for f in ("back",):
        for x in range(7):
            base.set("main", f, x, 8, mul(G, 0.78))
        for y in range(9):
            base.set("main", f, 0, y, mul(G, 0.86))
            base.set("main", f, 6, y, mul(G, 0.86))
    base.fill("flap", "back", mul(G, 1.04))
    for x in range(7):
        base.set("flap", "back", x, 3, mul(G, 0.8))
    base.fill("pocket", "back", mul(G, 0.94))
    detail = Tex(64, 32, BAG, 31)
    BUCKLE, ZIP, RED, WHITE, LINE = C("d6c070"), C("4a4a50"), C("e2423c"), C("f6f6f2"), C("2a2a2e")
    # Buckles on the flap's straps, a Poké Ball badge in the middle.
    for x in (1, 5):
        detail.set("flap", "back", x, 2, BUCKLE)
        detail.set("flap", "back", x, 3, mul(BUCKLE, 0.8))
    detail.set("flap", "back", 3, 1, RED)
    detail.set("flap", "back", 2, 1, RED)
    detail.set("flap", "back", 4, 1, RED)
    detail.set("flap", "back", 3, 2, LINE)
    detail.set("flap", "back", 2, 2, WHITE)
    detail.set("flap", "back", 4, 2, WHITE)
    # A zip along the front pocket.
    for x in range(5):
        detail.set("pocket", "back", x, 0, ZIP)
    detail.set("pocket", "back", 4, 1, C("c8c8c8"))
    # The straps: darker webbing (left in greys on the base; a little buckle on each).
    for b in ("strap",):
        for f in ("front", "back"):
            detail.set(b, f, 0, 5, BUCKLE)
    # The satchel: a flap line and a clasp.
    for x in range(5):
        detail.set("satchel", "back", x, 2, mul(C("8a8a8a"), 1))
    detail.set("satchel", "back", 2, 3, BUCKLE)
    for x in range(5):
        detail.set("satchel", "front", x, 2, mul(C("8a8a8a"), 1))
    detail.set("satchel", "front", 2, 3, BUCKLE)
    return base, detail


# ---------- the Pokédex in your hands ----------

def device():
    t = Tex(32, 16, {"body": (0, 0, 6, 1, 4)}, 40)
    RED = C("dc3a34")
    t.box("body", RED, grain=0.04)
    # The face (the box's top, held towards you): a cream bezel, the dark screen, the blue lens top left.
    t.fill("body", "top", C("f2ece0"), grain=0.02)
    t.fill("body", "top", C("1c2a44"), 1, 1, 5, 4, grain=0.03)
    t.set("body", "top", 2, 2, C("5ad0f2"))
    t.set("body", "top", 3, 2, C("8ae0ff"))
    t.set("body", "top", 0, 0, C("4a9af2"))
    return t


def preview(caps, base, detail, dev):
    S = 6
    sheet = Image.new("RGBA", (64 * S * 4, 32 * S * ((len(caps) + 3) // 4 + 1)), (40, 44, 56, 255))
    for i, (name, t) in enumerate(caps):
        im = t.im.resize((64 * S, 32 * S), Image.NEAREST)
        sheet.alpha_composite(im, ((i % 4) * 64 * S, (i // 4) * 32 * S))
    row = (len(caps) + 3) // 4
    b = base.im.copy()
    b.alpha_composite(detail.im)
    sheet.alpha_composite(b.resize((64 * S, 32 * S), Image.NEAREST), (0, row * 32 * S))
    sheet.alpha_composite(dev.im.resize((32 * S, 16 * S), Image.NEAREST), (64 * S, row * 32 * S))
    os.makedirs(ART, exist_ok=True)
    sheet.save(os.path.join(ART, "gear_preview.png"))


def main():
    caps = []
    for name, fn in CAPS:
        t = fn()
        t.save(os.path.join(ROOT, "cap_" + name + ".png"))
        caps.append((name, t))
    base, detail = bags()
    base.save(os.path.join(ROOT, "bag_base.png"))
    detail.save(os.path.join(ROOT, "bag_detail.png"))
    dev = device()
    dev.save(os.path.join(ROOT, "device.png"))
    preview(caps, base, detail, dev)
    print("gear:", len(caps), "caps, bags, device")


if __name__ == "__main__":
    main()
