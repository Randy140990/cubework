"""Draws Orbit Arena's entity textures (64 x 64, the humanoid layout with mirrored left limbs).

Fans: 8 original space-crowd looks (astronauts, a robot, critters, supporters) x 10 (no sign, 6 signs, and the three
big boards that spell INSID3 TH3 CUB3 when three fans hold them side by side).
Fighters: 8 masked arena fighters. Run from the project root: python3 tools/textures.py
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "orbitarena", "textures", "entity")


def box_faces(u, v, w, h, d):
    """The texture rectangles of a box at texOffs(u, v) with size w x h x d: name -> (x0, y0, x1, y1)."""
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


HEAD = box_faces(0, 0, 8, 8, 8)
HAT = box_faces(32, 0, 8, 8, 8)
BODY = box_faces(16, 16, 8, 12, 4)
ARM = box_faces(40, 16, 4, 12, 4)
LEG = box_faces(0, 16, 4, 12, 4)
SIGN = box_faces(0, 32, 14, 8, 1)
BIG = box_faces(0, 48, 26, 10, 1)


class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def rect(self, r, colour, noise=0.06):
        x0, y0, x1, y1 = r
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.px(x, y, colour, noise)

    def px(self, x, y, colour, noise=0.0):
        k = 1 + self.rnd.uniform(-noise, noise)
        c = tuple(max(0, min(255, int(ch * k))) for ch in colour[:3])
        a = colour[3] if len(colour) > 3 else 255
        self.img.putpixel((x, y), c + (a,))

    def part(self, faces, colour, noise=0.06):
        for r in faces.values():
            self.rect(r, colour, noise)

    def band(self, faces, rows, colour, which=("front", "back", "left", "right")):
        """Rows (from the top of each side face) in a colour."""
        for name in which:
            x0, y0, x1, y1 = faces[name]
            for y in rows:
                if y0 + y < y1:
                    for x in range(x0, x1):
                        self.px(x, y0 + y, colour, 0.04)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


def shade(c, k):
    return tuple(max(0, min(255, int(ch * k))) for ch in c[:3])


# A tiny 3 x 5 pixel font for the signs.
FONT = {
    "G": ["111", "100", "101", "101", "111"], "O": ["111", "101", "101", "101", "111"], "!": ["010", "010", "010", "000", "010"],
    "W": ["101", "101", "101", "111", "101"], "I": ["111", "010", "010", "010", "111"], "N": ["101", "111", "111", "111", "101"],
    "S": ["111", "100", "111", "001", "111"], "D": ["110", "101", "101", "101", "110"], "3": ["111", "001", "111", "001", "111"],
    "T": ["111", "010", "010", "010", "010"], "H": ["101", "101", "111", "101", "101"], "C": ["111", "100", "100", "100", "111"],
    "U": ["101", "101", "101", "101", "111"], "B": ["110", "101", "110", "101", "110"],
}
HEART = ["0101000", "1111100", "1111100", "0111000", "0010000"]
STAR = ["0010000", "1111100", "0111000", "0101000", "1000100"]
SIGNS = [("GO!", (255, 255, 255)), ("WOW", (255, 230, 80)), ("GG!", (120, 255, 200)), ("WIN", (255, 140, 60)), (HEART, (255, 70, 110)),
         (STAR, (255, 220, 60))]
# The brand, across three big boards held side by side.
BRAND = [("INSID3", (255, 150, 40)), ("TH3", (255, 255, 255)), ("CUB3", (255, 150, 40))]


def draw_sign(s, sign, board, faces=SIGN):
    """The board, with the text on its back face turned half round (it's raised overhead, so it reads from the front)."""
    s.part(faces, board, 0.03)
    text, colour = sign
    glyph = text if isinstance(text, list) else None
    x0, y0, x1, y1 = faces["back"]
    w, h = x1 - x0, y1 - y0
    pixels = []
    if glyph:
        gw = len(glyph[0])
        ox, oy = (w - gw + 2) // 2, (h - 5) // 2
        for gy, row in enumerate(glyph):
            for gx, ch in enumerate(row):
                if ch == "1":
                    pixels.append((ox + gx, oy + gy))
    else:
        tw = len(text) * 4 - 1
        ox, oy = (w - tw) // 2, (h - 5) // 2
        for i, ch in enumerate(text):
            for gy, row in enumerate(FONT[ch]):
                for gx, bit in enumerate(row):
                    if bit == "1":
                        pixels.append((ox + i * 4 + gx, oy + gy))
    for (px, py) in pixels:
        # Turned half round: flipped across and up and down.
        s.px(x0 + (w - 1 - px), y0 + (h - 1 - py), colour)
    # A frame round the board's face.
    for x in range(x0, x1):
        s.px(x, y0, shade(board, 0.6))
        s.px(x, y1 - 1, shade(board, 0.6))


def face(s, faces, eyes, eye_row=3, mouth=None, mouth_row=6):
    x0, y0, _, _ = faces["front"]
    for ex in (1, 2, 5, 6):
        s.px(x0 + ex, y0 + eye_row, eyes)
    if mouth:
        for mx in range(3, 5):
            s.px(x0 + mx, y0 + mouth_row, mouth)


def fan(look):
    s = Skin(1000 + look)
    if look in (0, 1):
        # Astronauts: a white suit with a coloured trim, a dark visor.
        suit, trim = (235, 235, 240), [(255, 140, 40), (60, 120, 255)][look]
        s.part(HEAD, (225, 225, 232))
        s.part(BODY, suit)
        s.part(ARM, suit)
        s.part(LEG, suit)
        s.band(BODY, [5, 6], trim)
        s.band(ARM, [2], trim)
        s.band(LEG, [10, 11], shade(suit, 0.7))
        x0, y0, _, _ = HEAD["front"]
        s.rect((x0 + 1, y0 + 2, x0 + 7, y0 + 6), (25, 35, 70))
        s.px(x0 + 2, y0 + 2, (140, 180, 255))
        s.px(x0 + 3, y0 + 2, (110, 150, 230))
        bx, by, _, _ = BODY["front"]
        s.rect((bx + 2, by + 1, bx + 6, by + 4), shade(trim, 0.9))
    elif look == 2:
        # A robot: brushed steel, glowing cyan eyes.
        steel = (150, 158, 170)
        for f in (HEAD, BODY, ARM, LEG):
            s.part(f, steel, 0.1)
        x0, y0, _, _ = HEAD["front"]
        s.rect((x0 + 1, y0 + 3, x0 + 7, y0 + 5), (30, 35, 45))
        for ex in (2, 5):
            s.px(x0 + ex, y0 + 3, (80, 255, 255))
            s.px(x0 + ex, y0 + 4, (40, 200, 230))
        s.band(BODY, [3], (80, 255, 255), ("front",))
        s.band(LEG, [0, 1], (90, 95, 105))
    elif look in (3, 7):
        # Critters: a cyclops in purple, a three-eyed one in coral.
        skin = [(150, 90, 200), (240, 120, 110)][look == 7]
        shirt = [(40, 40, 60), (40, 110, 120)][look == 7]
        s.part(HEAD, skin)
        s.part(ARM, skin)
        s.part(BODY, shirt)
        s.part(LEG, shade(shirt, 0.8))
        x0, y0, _, _ = HEAD["front"]
        if look == 3:
            s.rect((x0 + 2, y0 + 2, x0 + 6, y0 + 5), (250, 250, 250))
            s.rect((x0 + 3, y0 + 3, x0 + 5, y0 + 5), (30, 20, 50))
        else:
            for ex in (1, 3, 5):
                s.rect((x0 + ex, y0 + 3, x0 + ex + 2, y0 + 4), (250, 250, 250))
                s.px(x0 + ex + 1, y0 + 3, (20, 20, 30))
        s.px(x0 + 3, y0 + 6, shade(skin, 0.5))
        s.px(x0 + 4, y0 + 6, shade(skin, 0.5))
    else:
        # Supporters: a hoodie with a star, a "10" shirt, a striped scarf.
        tones = [(240, 200, 170), (198, 140, 100), (140, 95, 65), (95, 65, 45)]
        tone = tones[look % 4]
        shirt = {4: (30, 170, 160), 5: (210, 40, 50), 6: (250, 205, 40)}[look]
        hair = [(60, 40, 25), (25, 20, 20), (200, 150, 70)][look % 3]
        s.part(HEAD, tone)
        s.part(ARM, shirt)
        s.part(BODY, shirt)
        s.part(LEG, (50, 60, 90))
        s.band(ARM, [10, 11], tone)
        s.rect(HEAD["top"], hair)
        for n in ("front", "left", "right", "back"):
            x0, y0, x1, _ = HEAD[n]
            for x in range(x0, x1):
                s.px(x, y0, hair)
                if n != "front":
                    s.px(x, y0 + 1, hair)
        face(s, HEAD, (30, 30, 40), 4, shade(tone, 0.7), 6)
        bx, by, _, _ = BODY["front"]
        if look == 4:
            for (sx, sy) in [(4, 2), (3, 3), (4, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (3, 5), (5, 5)]:
                s.px(bx + sx - 1, by + sy, (255, 230, 90))
        elif look == 5:
            for (sx, sy) in [(1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (3, 2), (4, 2), (5, 2), (3, 3), (5, 3), (3, 4), (5, 4), (3, 5), (5, 5), (3, 6), (4, 6), (5, 6)]:
                s.px(bx + sx, by + sy, (255, 255, 255))
        else:
            s.band(BODY, [0, 1], (40, 40, 40))
            s.band(BODY, [2, 3], (255, 255, 255))
    return s


def fighter(look):
    """Arena fighters: masked, in coloured suits (mostly under armour; the face shows through helmets)."""
    s = Skin(2000 + look)
    tones = [(236, 196, 164), (196, 140, 100), (150, 100, 70), (100, 70, 50)]
    tone = tones[look % 4]
    suit = [(170, 30, 40), (30, 70, 170), (20, 130, 80), (30, 30, 36), (220, 220, 225), (110, 50, 160), (230, 110, 20), (90, 98, 110)][look]
    mask = [(20, 20, 24), (240, 240, 240), (20, 20, 24), (200, 30, 40), (20, 60, 120), (20, 20, 24), (30, 30, 30), (250, 200, 40)][look]
    glow = [(255, 80, 80), (80, 180, 255), (90, 255, 140), (255, 210, 60), (60, 230, 255), (220, 120, 255), (255, 160, 60), (255, 255, 255)][look]
    hair = [(40, 28, 20), (20, 18, 18), (150, 90, 40), (230, 200, 120), (30, 25, 22), (120, 30, 30), (20, 20, 20), (60, 45, 35)][look]
    s.part(HEAD, tone)
    s.part(BODY, suit)
    s.part(ARM, suit)
    s.part(LEG, shade(suit, 0.75))
    s.band(ARM, [10, 11], tone)
    s.band(BODY, [11], (30, 30, 30))
    s.rect(HEAD["top"], hair)
    for n in ("left", "right", "back"):
        x0, y0, x1, _ = HEAD[n]
        for y in range(y0, y0 + 3):
            for x in range(x0, x1):
                s.px(x, y, hair)
    x0, y0, x1, _ = HEAD["front"]
    for x in range(x0, x1):
        s.px(x, y0, hair)
    # A mask across the eyes with a glowing slit.
    s.rect((x0, y0 + 3, x1, y0 + 5), mask, 0.03)
    for ex in (1, 2, 5, 6):
        s.px(x0 + ex, y0 + 3, glow)
    for n in ("left", "right"):
        fx0, fy0, fx1, _ = HEAD[n]
        s.rect((fx0, fy0 + 3, fx1, fy0 + 5), mask, 0.03)
    s.px(x0 + 3, y0 + 6, shade(tone, 0.7))
    s.px(x0 + 4, y0 + 6, shade(tone, 0.7))
    bx, by, _, _ = BODY["front"]
    s.rect((bx + 3, by + 2, bx + 5, by + 6), glow)
    return s


def main():
    boards = [(232, 200, 140), (250, 250, 250), (60, 60, 70), (40, 80, 160), (160, 40, 60), (40, 130, 90)]
    for look in range(8):
        for sign in range(10):
            s = fan(look)
            if 0 < sign <= len(SIGNS):
                draw_sign(s, SIGNS[sign - 1], boards[(look + sign) % len(boards)])
            elif sign > len(SIGNS):
                draw_sign(s, BRAND[sign - len(SIGNS) - 1], (28, 30, 40), BIG)
            s.save(os.path.join(ROOT, "fan", "fan_%d_%d.png" % (look, sign)))
        fighter(look).save(os.path.join(ROOT, "fighter", "fighter_%d.png" % look))


if __name__ == "__main__":
    main()
