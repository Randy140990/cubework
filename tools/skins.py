#!/usr/bin/env python3
"""Paints Route One's first people as 64x64 player skins: the nurse, the Mart clerk, a youngster and a gym leader."""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "kantojohto", "textures", "entity", "npc")

def rect(p, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            p[x, y] = c

def box(p, ox, oy, w, h, d, c):
    """All six faces of a box part in the skin layout (w wide, h tall, d deep) at texture offset (ox, oy)."""
    rect(p, ox + d, oy, ox + d + w, oy + d, c)                    # top
    rect(p, ox + d + w, oy, ox + d + 2 * w, oy + d, c)            # bottom
    rect(p, ox, oy + d, ox + 2 * d + 2 * w, oy + d + h, c)        # sides, front, back

def band(p, ox, oy, w, h, d, y0, y1, c):
    """Rows y0..y1 (from the top of the part) all the way round."""
    rect(p, ox, oy + d + y0, ox + 2 * d + 2 * w, oy + d + y1, c)

def front(p, ox, oy, w, h, d, x0, y0, x1, y1, c):
    rect(p, ox + d + x0, oy + d + y0, ox + d + x1, oy + d + y1, c)

def skin(skin, hair, shirt, sleeve, hand, pants, shoes, extra=None):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    p = im.load()
    # Head: skin, hair over the top and down the back and sides, eyes and a mouth.
    box(p, 0, 0, 8, 8, 8, skin)
    rect(p, 8, 0, 16, 8, hair)
    band(p, 0, 0, 8, 8, 8, 0, 2, hair)
    rect(p, 24, 8, 32, 14, hair)          # back of the head
    rect(p, 0, 8, 8, 12, hair)            # right side
    rect(p, 16, 8, 24, 12, hair)          # left side
    p[10, 12] = (255, 255, 255, 255); p[11, 12] = (40, 40, 60, 255)
    p[13, 12] = (40, 40, 60, 255); p[12, 12] = skin
    p[14, 12] = (255, 255, 255, 255)
    p[11, 14] = p[12, 14] = (150, 80, 70, 255)
    # Body, arms and legs.
    box(p, 16, 16, 8, 12, 4, shirt)
    box(p, 40, 16, 4, 12, 4, sleeve)
    box(p, 32, 48, 4, 12, 4, sleeve)
    band(p, 40, 16, 4, 12, 4, 8, 12, hand)
    band(p, 32, 48, 4, 12, 4, 8, 12, hand)
    box(p, 0, 16, 4, 12, 4, pants)
    box(p, 16, 48, 4, 12, 4, pants)
    band(p, 0, 16, 4, 12, 4, 10, 12, shoes)
    band(p, 16, 48, 4, 12, 4, 10, 12, shoes)
    if extra: extra(p)
    return im

S = lambda r, g, b: (r, g, b, 255)
FAIR, TAN = S(240, 200, 170), S(196, 146, 104)

def nurse_extra(p):
    # White cap with a red cross, a white apron, pink hair loops at the sides.
    rect(p, 40, 0, 48, 8, S(250, 250, 250)); rect(p, 32, 8, 64, 10, S(250, 250, 250))
    p[43, 9] = p[44, 9] = S(220, 40, 60); p[43, 8] = p[44, 8] = S(220, 40, 60)
    front(p, 16, 16, 8, 12, 4, 1, 3, 7, 12, S(250, 250, 250))
    rect(p, 0, 12, 2, 16, S(240, 130, 170)); rect(p, 22, 12, 24, 16, S(240, 130, 170))

def clerk_extra(p):
    # A white apron with a blue stripe across the chest.
    front(p, 16, 16, 8, 12, 4, 1, 5, 7, 12, S(245, 245, 245))
    front(p, 16, 16, 8, 12, 4, 0, 4, 8, 5, S(40, 80, 180))

def youngster_extra(p):
    # A blue cap with a brim, white socks.
    rect(p, 40, 0, 48, 8, S(40, 90, 200)); rect(p, 32, 8, 64, 10, S(40, 90, 200))
    rect(p, 40, 10, 48, 11, S(30, 70, 170))
    band(p, 0, 16, 4, 12, 4, 8, 10, S(250, 250, 250)); band(p, 16, 48, 4, 12, 4, 8, 10, S(250, 250, 250))
    band(p, 0, 16, 4, 12, 4, 4, 8, FAIR); band(p, 16, 48, 4, 12, 4, 4, 8, FAIR)

def leader_extra(p):
    # A green vest open over the shirt, spiky hair, narrow eyes.
    front(p, 16, 16, 8, 12, 4, 0, 0, 2, 12, S(80, 120, 60)); front(p, 16, 16, 8, 12, 4, 6, 0, 8, 12, S(80, 120, 60))
    rect(p, 32, 20, 40, 32, S(80, 120, 60))
    for x in (8, 10, 12, 14): p[x, 0] = (0, 0, 0, 0)
    rect(p, 40, 0, 48, 8, S(100, 66, 34)); rect(p, 32, 8, 64, 9, S(100, 66, 34))
    p[10, 12] = p[14, 12] = TAN; p[11, 12] = p[13, 12] = S(50, 40, 30)

SKINS = {
    "nurse": skin(FAIR, S(240, 130, 170), S(248, 176, 200), S(248, 176, 200), FAIR, FAIR, S(245, 245, 245), nurse_extra),
    "clerk": skin(FAIR, S(110, 70, 40), S(60, 110, 210), S(60, 110, 210), FAIR, S(40, 50, 90), S(30, 30, 35), clerk_extra),
    "youngster": skin(FAIR, S(60, 40, 25), S(245, 190, 60), S(245, 190, 60), FAIR, S(50, 90, 190), S(200, 40, 40), youngster_extra),
    "leader": skin(TAN, S(100, 66, 34), S(220, 120, 50), S(220, 120, 50), TAN, S(110, 80, 50), S(50, 40, 35), leader_extra),
}
os.makedirs(OUT, exist_ok=True)
for name, im in SKINS.items():
    im.save(os.path.join(OUT, name + ".png"))
    print(name)
