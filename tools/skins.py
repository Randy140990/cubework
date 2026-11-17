#!/usr/bin/env python3
"""Paints Retromon's first people as 64x64 player skins: the nurse, the Mart clerk, a youngster and a gym leader."""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "retromon", "textures", "entity", "npc")

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


# ---------- everyone else, from a short description each ----------

def person(skin_c, hair, style, top, legs, legstyle="pants", shoes=S(40, 35, 35), sleeve=None, hat=None, extras=()):
    sleeve = sleeve or top
    def extra(p):
        # Hair or hat.
        if style == "bald":
            rect(p, 8, 0, 16, 8, skin_c); band(p, 0, 0, 8, 8, 8, 0, 2, skin_c)
            rect(p, 0, 10, 8, 13, hair); rect(p, 16, 10, 24, 13, hair); rect(p, 24, 10, 32, 14, hair)
        elif style in ("long", "ponytail", "bun", "pigtails"):
            rect(p, 24, 8, 32, 16, hair); rect(p, 0, 8, 8, 16, hair); rect(p, 16, 8, 24, 16, hair)
            front(p, 0, 0, 8, 8, 8, 0, 0, 8, 2, hair)
            if style == "long":
                rect(p, 32, 20, 40, 26, hair)  # down the back
            if style == "bun":
                rect(p, 34, 0, 38, 4, hair)
        elif style == "spiky":
            for x in (8, 10, 12, 14): p[x, 0] = (0, 0, 0, 0)
            rect(p, 40, 0, 48, 8, hair); rect(p, 32, 8, 64, 9, hair)
        if hat:
            rect(p, 40, 0, 48, 8, hat); rect(p, 32, 8, 64, 10, hat)
            rect(p, 40, 10, 48, 11, tuple(max(0, c - 40) for c in hat[:3]) + (255,))
        # Legs.
        if legstyle == "shorts":
            band(p, 0, 16, 4, 12, 4, 6, 10, skin_c); band(p, 16, 48, 4, 12, 4, 6, 10, skin_c)
        elif legstyle in ("skirt", "dress"):
            band(p, 0, 16, 4, 12, 4, 0, 10, skin_c); band(p, 16, 48, 4, 12, 4, 0, 10, skin_c)
            band(p, 0, 16, 4, 12, 4, 0, 4 if legstyle == "skirt" else 7, legs); band(p, 16, 48, 4, 12, 4, 0, 4 if legstyle == "skirt" else 7, legs)
            if legstyle == "dress":
                box(p, 16, 16, 8, 12, 4, top)
        for e in extras:
            if e == "glasses":
                for x in (9, 10, 13, 14): p[x, 12] = S(30, 30, 40)
                p[11, 11] = p[12, 11] = S(30, 30, 40)
            elif e == "shades":
                rect(p, 9, 11, 15, 13, S(20, 20, 25))
            elif e == "beard":
                rect(p, 9, 13, 15, 16, hair)
            elif e == "moustache":
                rect(p, 10, 14, 14, 15, hair)
            elif e.startswith("coat"):
                c = S(240, 240, 240)
                box(p, 16, 16, 8, 12, 4, c); box(p, 40, 16, 4, 12, 4, c); box(p, 32, 48, 4, 12, 4, c)
                front(p, 16, 16, 8, 12, 4, 3, 0, 5, 12, top)
                band(p, 40, 16, 4, 12, 4, 10, 12, skin_c); band(p, 32, 48, 4, 12, 4, 10, 12, skin_c)
            elif e.startswith("letter"):
                c = S(220, 30, 40)
                front(p, 16, 16, 8, 12, 4, 2, 2, 3, 8, c); front(p, 16, 16, 8, 12, 4, 3, 2, 5, 3, c); front(p, 16, 16, 8, 12, 4, 5, 3, 6, 5, c)
                front(p, 16, 16, 8, 12, 4, 3, 5, 5, 6, c); front(p, 16, 16, 8, 12, 4, 5, 6, 6, 8, c)
            elif e.startswith("tie"):
                front(p, 16, 16, 8, 12, 4, 3, 0, 5, 7, S(190, 30, 40))
            elif e == "vest":
                front(p, 16, 16, 8, 12, 4, 0, 0, 2, 12, S(30, 30, 35)); front(p, 16, 16, 8, 12, 4, 6, 0, 8, 12, S(30, 30, 35))
            elif e == "shirtless":
                box(p, 16, 16, 8, 12, 4, skin_c); box(p, 40, 16, 4, 12, 4, skin_c); box(p, 32, 48, 4, 12, 4, skin_c)
            elif e == "belt":
                band(p, 16, 16, 8, 12, 4, 10, 12, S(50, 35, 25))
            elif e == "backpack":
                rect(p, 32, 21, 40, 30, S(120, 80, 40))
            elif e == "stripes":
                for y in (1, 4, 7, 10): front(p, 16, 16, 8, 12, 4, 0, y, 8, y + 1, S(40, 70, 170))
            elif e == "apron":
                front(p, 16, 16, 8, 12, 4, 1, 4, 7, 12, S(245, 245, 245))
            elif e == "scarf":
                band(p, 16, 16, 8, 12, 4, 0, 2, S(120, 50, 150))
    return skin(skin_c, hair, top, sleeve, skin_c, legs, shoes, extra)

FAIR2, DARK = S(232, 186, 150), S(150, 100, 70)
BROWN, BLACK, GREY, BLOND, RED_H = S(100, 66, 34), S(35, 30, 30), S(170, 170, 170), S(230, 200, 110), S(200, 60, 40)
PEOPLE = {
    "red": person(FAIR, BLACK, "short", S(200, 40, 40), S(50, 80, 170), hat=S(220, 40, 40), extras=("vest",)),
    "blue": person(FAIR, BROWN, "spiky", S(120, 70, 170), S(40, 40, 50)),
    "oak": person(FAIR, GREY, "short", S(170, 80, 60), S(120, 90, 60), extras=("coat",)),
    "cooltrainer_f": person(FAIR, S(200, 110, 60), "ponytail", S(40, 120, 130), S(30, 30, 40), "skirt"),
    "cooltrainer_m": person(FAIR, S(60, 140, 70), "spiky", S(40, 120, 130), S(30, 30, 40)),
    "super_nerd": person(FAIR, BLACK, "short", S(235, 235, 235), S(120, 90, 60), extras=("glasses",)),
    "rocket": person(FAIR, BLACK, "short", S(30, 30, 35), S(30, 30, 35), hat=S(30, 30, 35), extras=("letter",)),
    "scientist": person(FAIR, GREY, "short", S(90, 90, 110), S(70, 70, 80), extras=("coat", "glasses")),
    "hiker": person(TAN, BROWN, "short", S(150, 100, 50), S(80, 110, 60), "shorts", hat=S(130, 90, 50), extras=("beard", "backpack")),
    "swimmer": person(TAN, BLACK, "short", S(0, 0, 0), S(40, 90, 200), "shorts", hat=S(240, 240, 240), extras=("shirtless",)),
    "gentleman": person(FAIR, GREY, "short", S(30, 30, 35), S(30, 30, 35), extras=("tie", "moustache")),
    "biker": person(FAIR, BLACK, "bald", S(30, 30, 35), S(50, 50, 60), extras=("shades",)),
    "channeler": person(FAIR, BLACK, "long", S(235, 235, 240), S(235, 235, 240), "dress"),
    "gambler": person(FAIR, BROWN, "short", S(170, 40, 50), S(40, 40, 50), hat=S(120, 80, 40)),
    "fisher": person(TAN, BROWN, "short", S(50, 90, 170), S(120, 90, 60), hat=S(200, 180, 120)),
    "sailor": person(TAN, BLACK, "short", S(245, 245, 245), S(245, 245, 245), hat=S(245, 245, 245), extras=("stripes",)),
    "girl": person(FAIR, BROWN, "long", S(240, 130, 170), S(240, 130, 170), "dress"),
    "little_girl": person(FAIR, S(160, 90, 40), "pigtails", S(250, 160, 190), S(250, 160, 190), "dress"),
    "brunette_girl": person(FAIR, BROWN, "long", S(245, 200, 80), S(60, 90, 180), "skirt"),
    "beauty": person(FAIR, BLOND, "long", S(200, 40, 60), S(200, 40, 60), "dress"),
    "rocker": person(FAIR, S(120, 60, 160), "spiky", S(120, 60, 160), S(30, 30, 35), extras=("shades",)),
    "fishing_guru": person(FAIR, S(230, 230, 230), "bald", S(120, 90, 60), S(120, 90, 60), extras=("beard",)),
    "middle_aged_man": person(FAIR, BROWN, "short", S(90, 140, 80), S(110, 80, 50)),
    "middle_aged_woman": person(FAIR, BROWN, "bun", S(140, 80, 160), S(140, 80, 160), "dress"),
    "little_boy": person(FAIR, BROWN, "short", S(210, 50, 50), S(50, 90, 190), "shorts"),
    "gameboy_kid": person(FAIR, BLACK, "short", S(60, 160, 80), S(50, 90, 190), "shorts", hat=S(60, 160, 80)),
    "gym_guide": person(FAIR, BLACK, "short", S(50, 100, 200), S(30, 30, 40), hat=S(220, 40, 40)),
    "cook": person(FAIR, BLACK, "short", S(245, 245, 245), S(30, 30, 35), hat=S(250, 250, 250), extras=("apron",)),
    "guard": person(FAIR, BLACK, "short", S(40, 60, 140), S(30, 40, 90), hat=S(40, 60, 140), extras=("belt",)),
    "gramps": person(FAIR, GREY, "bald", S(150, 110, 70), S(110, 90, 70), extras=("beard",)),
    "granny": person(FAIR, GREY, "bun", S(130, 80, 150), S(130, 80, 150), "dress"),
    "safari_zone_worker": person(TAN, BROWN, "short", S(190, 170, 110), S(190, 170, 110), "shorts", hat=S(90, 130, 60)),
    "link_receptionist": person(FAIR, BROWN, "bun", S(60, 90, 180), S(60, 90, 180), "dress"),
    "balding_guy": person(FAIR, BROWN, "bald", S(240, 240, 240), S(120, 90, 60)),
    "silph_worker_m": person(FAIR, BLACK, "short", S(110, 120, 140), S(60, 60, 70), extras=("tie",)),
    "silph_worker_f": person(FAIR, BROWN, "bun", S(110, 120, 140), S(60, 60, 70), "skirt"),
    "silph_president": person(FAIR, GREY, "short", S(40, 40, 50), S(40, 40, 50), extras=("tie", "moustache")),
    "giovanni": person(FAIR, BLACK, "short", S(200, 110, 50), S(40, 30, 30)),
    "waiter": person(FAIR, BLACK, "short", S(245, 245, 245), S(30, 30, 35), extras=("vest",)),
    "officer_jenny": person(FAIR, S(50, 90, 170), "long", S(40, 60, 140), S(40, 60, 140), "skirt", hat=S(40, 60, 140)),
    "mr_fuji": person(FAIR, GREY, "bald", S(200, 180, 130), S(140, 120, 90), extras=("glasses", "beard")),
    "bike_shop_clerk": person(FAIR, BROWN, "short", S(60, 100, 190), S(60, 100, 190), hat=S(220, 220, 220)),
    "warden": person(FAIR, GREY, "bald", S(130, 100, 70), S(130, 100, 70), extras=("beard",)),
    "captain": person(FAIR, GREY, "short", S(245, 245, 245), S(245, 245, 245), hat=S(245, 245, 245), extras=("beard",)),
    "lorelei": person(FAIR, RED_H, "long", S(130, 70, 160), S(40, 40, 60), "skirt", extras=("glasses",)),
    "lance": person(FAIR, RED_H, "spiky", S(30, 30, 40), S(50, 70, 140)),
    "bruno": person(TAN, BLACK, "short", S(0, 0, 0), S(240, 240, 240), extras=("shirtless", "belt")),
    "agatha": person(FAIR, GREY, "bun", S(110, 60, 140), S(110, 60, 140), "dress"),
    "jessie": person(FAIR, RED_H, "long", S(245, 245, 245), S(245, 245, 245), "skirt", extras=("letter",)),
    "james": person(FAIR, S(110, 70, 170), "short", S(245, 245, 245), S(245, 245, 245), extras=("letter",)),
    "leader_brock": person(TAN, BROWN, "spiky", S(220, 120, 50), S(110, 80, 50), extras=("vest",)),
    "leader_misty": person(FAIR, S(240, 120, 50), "ponytail", S(245, 210, 80), S(200, 40, 50), "shorts"),
    "leader_lt_surge": person(FAIR, BLOND, "spiky", S(90, 110, 60), S(70, 90, 50)),
    "leader_erika": person(FAIR, BLACK, "short", S(110, 160, 90), S(230, 190, 80), "dress"),
    "leader_koga": person(FAIR, BLACK, "short", S(40, 40, 50), S(40, 40, 50), extras=("scarf",)),
    "leader_sabrina": person(FAIR, S(40, 80, 60), "long", S(150, 30, 50), S(30, 30, 35)),
    "leader_blaine": person(FAIR, S(240, 240, 240), "bald", S(245, 245, 245), S(90, 90, 100), extras=("shades", "moustache", "coat")),
    "leader_giovanni": person(FAIR, BLACK, "short", S(200, 110, 50), S(40, 30, 30)),
}

SKINS = {
    "nurse": skin(FAIR, S(240, 130, 170), S(248, 176, 200), S(248, 176, 200), FAIR, FAIR, S(245, 245, 245), nurse_extra),
    "clerk": skin(FAIR, S(110, 70, 40), S(60, 110, 210), S(60, 110, 210), FAIR, S(40, 50, 90), S(30, 30, 35), clerk_extra),
    "youngster": skin(FAIR, S(60, 40, 25), S(245, 190, 60), S(245, 190, 60), FAIR, S(50, 90, 190), S(200, 40, 40), youngster_extra),
    "leader": skin(TAN, S(100, 66, 34), S(220, 120, 50), S(220, 120, 50), TAN, S(110, 80, 50), S(50, 40, 35), leader_extra),
}
SKINS.update(PEOPLE)
os.makedirs(OUT, exist_ok=True)
for name, im in SKINS.items():
    im.save(os.path.join(OUT, name + ".png"))
    print(name)
