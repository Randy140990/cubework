#!/usr/bin/env python3
"""Paints Retromon's people as 64x64 player skins (textures/entity/npc). Writes the ones not there yet;
--all repaints everyone."""
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
            if isinstance(e, tuple):
                n, c = e[0], e[1]
                if n == "headband": band(p, 0, 0, 8, 8, 8, 1, 2, c)
                elif n == "badge": front(p, 16, 16, 8, 12, 4, 1, 2, 3, 4, c)
                elif n == "collar": front(p, 16, 16, 8, 12, 4, 2, 0, 6, 1, c)
                elif n == "overalls":
                    front(p, 16, 16, 8, 12, 4, 2, 4, 6, 12, c); front(p, 16, 16, 8, 12, 4, 2, 0, 3, 4, c); front(p, 16, 16, 8, 12, 4, 5, 0, 6, 4, c)
                    band(p, 16, 16, 8, 12, 4, 7, 12, c)
                elif n == "gloves": band(p, 40, 16, 4, 12, 4, 9, 12, c); band(p, 32, 48, 4, 12, 4, 9, 12, c)
                elif n == "stripe": band(p, 16, 16, 8, 12, 4, e[2], e[2] + 1, c); band(p, 40, 16, 4, 12, 4, e[2], e[2] + 1, c); band(p, 32, 48, 4, 12, 4, e[2], e[2] + 1, c)
                elif n == "sash":
                    for i in range(8): front(p, 16, 16, 8, 12, 4, i, i, i + 1, i + 2, c)
                elif n == "tie": front(p, 16, 16, 8, 12, 4, 3, 0, 5, 7, c)
                elif n == "cape": rect(p, 32, 20, 40, 32, c)
                elif n == "belt": band(p, 16, 16, 8, 12, 4, 10, 12, c)
                elif n == "socks": band(p, 0, 16, 4, 12, 4, 7, 10, c); band(p, 16, 48, 4, 12, 4, 7, 10, c)
                elif n == "bag": rect(p, 32, 21, 40, 30, c)
                elif n == "scarf": band(p, 16, 16, 8, 12, 4, 0, 2, c)
                elif n == "apron": front(p, 16, 16, 8, 12, 4, 1, 4, 7, 12, c)
                elif n == "trim": front(p, 16, 16, 8, 12, 4, 3, 0, 5, 12, c)
                elif n == "hem": band(p, 16, 16, 8, 12, 4, 11, 12, c)
                continue
            if e == "freckles":
                p[10, 13] = p[13, 13] = tuple(max(0, v - 45) for v in skin_c[:3]) + (255,)
            elif e == "blush":
                p[9, 13] = p[14, 13] = S(240, 140, 150)
            elif e == "lipstick":
                p[11, 14] = p[12, 14] = S(200, 60, 80)
            elif e == "glasses":
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
    "scientist_f": person(FAIR, S(70, 50, 40), "ponytail", S(90, 150, 170), S(60, 60, 72), "skirt", shoes=S(60, 60, 72), extras=("coat", "glasses")),
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

MED, DEEP = S(214, 158, 114), S(120, 78, 52)
AUBURN, GINGER, PINK_H, NAVY_H, TEAL_H, WHITE_H = S(140, 60, 30), S(220, 120, 50), S(240, 130, 170), S(40, 50, 100), S(40, 140, 140), S(235, 235, 235)
W, NAVY, KHAKI, DENIM, BLK = S(245, 245, 245), S(40, 50, 90), S(170, 150, 100), S(60, 90, 150), S(30, 30, 35)
MORE = {
    # Unique people still to make (story and town).
    "mum": person(FAIR2, BROWN, "bun", S(240, 150, 120), S(70, 90, 140), "skirt", extras=(("apron", S(250, 240, 220)), "blush")),
    "daisy": person(FAIR, BROWN, "long", S(120, 180, 110), S(245, 235, 210), "skirt", shoes=S(120, 80, 50), extras=("blush",)),
    "bill": person(FAIR, BROWN, "short", S(70, 130, 200), S(60, 60, 80), extras=(("collar", W),)),
    "copycat": person(FAIR, BROWN, "pigtails", S(220, 60, 60), S(60, 60, 90), "skirt", extras=("blush",)),
    "mr_psychic": person(FAIR, GREY, "bald", S(120, 70, 150), S(120, 70, 150), extras=(("sash", S(230, 190, 80)),)),
    "fan_club_chairman": person(FAIR2, GREY, "short", S(150, 100, 60), S(150, 100, 60), extras=("moustache", ("tie", S(200, 40, 50)))),
    "name_rater": person(FAIR, WHITE_H, "short", S(90, 90, 120), S(60, 60, 80), extras=("glasses", ("tie", S(60, 120, 200)))),
    "day_care_man": person(TAN, GREY, "short", S(110, 150, 90), S(120, 90, 60), hat=S(200, 170, 110), extras=("moustache",)),
    "old_man": person(FAIR2, GREY, "bald", S(170, 140, 100), S(110, 90, 70), extras=("beard",)),
    "museum_guide": person(FAIR, BLACK, "short", NAVY, NAVY, hat=NAVY, extras=(("badge", S(230, 190, 80)),)),
    "nurse_m": person(FAIR, PINK_H, "short", S(248, 176, 200), W, shoes=W, hat=W, extras=(("apron", W),)),
    "officer_m": person(MED, BLACK, "short", S(40, 60, 140), S(40, 60, 140), hat=S(40, 60, 140), extras=(("badge", S(230, 190, 80)), ("belt", BLK))),
    "rocket_f": person(FAIR, S(160, 60, 160), "long", BLK, BLK, "skirt", shoes=BLK, hat=BLK, extras=("letter",)),
    "rocket_scientist": person(FAIR, BLACK, "short", BLK, BLK, extras=("coat", "glasses", "letter")),
    "clerk_f": person(MED, BLACK, "ponytail", S(60, 110, 210), S(40, 50, 90), "skirt", extras=(("apron", W), ("stripe", S(40, 80, 180), 4))),
    "game_corner_clerk": person(FAIR, BROWN, "short", W, BLK, extras=("vest", ("tie", S(200, 40, 50)))),
    # Trainers still to make (generic classes).
    "lass": person(FAIR, BLACK, "long", S(230, 120, 160), S(60, 60, 110), "skirt", extras=("blush",)),
    "bug_catcher": person(FAIR, BROWN, "short", S(240, 240, 200), S(110, 140, 70), "shorts", hat=S(120, 160, 70), extras=(("socks", W),)),
    "camper": person(TAN, BROWN, "short", S(220, 140, 60), KHAKI, "shorts", hat=S(60, 110, 60), extras=(("bag", S(110, 80, 50)),)),
    "picnicker": person(FAIR, GINGER, "pigtails", S(120, 190, 120), DENIM, "shorts", hat=S(250, 220, 120)),
    "engineer": person(MED, BROWN, "short", S(240, 170, 50), DENIM, hat=S(250, 210, 40), extras=(("overalls", DENIM),)),
    "swimmer_f": person(FAIR2, BLACK, "ponytail", S(60, 160, 220), FAIR2, "shorts", shoes=FAIR2, extras=("shirtless", ("overalls", S(60, 160, 220)))),
    "cue_ball": person(TAN, BLACK, "bald", BLK, S(60, 60, 70), extras=("shades", ("belt", S(200, 40, 50)))),
    "psychic_m": person(FAIR, BLACK, "short", S(90, 60, 140), S(60, 40, 90), extras=(("sash", S(230, 190, 80)),)),
    "psychic_f": person(FAIR, S(110, 70, 170), "long", S(150, 60, 140), S(60, 40, 90), "skirt"),
    "juggler": person(FAIR, GINGER, "short", S(200, 60, 60), S(60, 60, 160), extras=(("stripe", S(240, 200, 60), 3), ("stripe", S(240, 200, 60), 7))),
    "tamer": person(TAN, BLACK, "short", S(160, 60, 40), KHAKI, hat=S(150, 110, 60), extras=("moustache", ("belt", S(90, 60, 40)))),
    "bird_keeper": person(FAIR, BROWN, "short", S(230, 210, 120), S(90, 110, 70), hat=S(90, 140, 70), extras=(("bag", S(110, 80, 50)),)),
    "black_belt": person(TAN, BLACK, "short", W, W, extras=(("belt", BLK), ("headband", W))),
    "burglar": person(FAIR, BLACK, "short", S(60, 60, 70), S(40, 40, 50), extras=("shades", ("stripe", W, 3), ("stripe", W, 6))),
    "pokemaniac": person(FAIR, BLACK, "short", S(90, 160, 80), S(110, 90, 60), extras=("glasses", ("bag", S(60, 100, 160)))),
    "crush_girl": person(MED, BLACK, "ponytail", S(220, 60, 60), S(40, 40, 60), "shorts", extras=(("headband", S(220, 60, 60)), ("gloves", W))),
    "tuber_m": person(FAIR, BROWN, "short", FAIR, S(60, 140, 220), "shorts", shoes=FAIR, extras=("shirtless", "freckles")),
    "tuber_f": person(FAIR, GINGER, "pigtails", S(250, 140, 170), S(250, 140, 170), "shorts", shoes=FAIR),
    "twins": person(FAIR, BROWN, "pigtails", S(250, 200, 80), S(200, 60, 80), "skirt", extras=("blush",)),
    "ruin_maniac": person(TAN, BROWN, "short", KHAKI, KHAKI, hat=KHAKI, extras=("beard", ("bag", S(110, 80, 50)))),
    "lady": person(FAIR, BLOND, "long", S(240, 200, 220), S(240, 200, 220), "dress", extras=("lipstick",)),
    "painter": person(FAIR, AUBURN, "bun", W, S(80, 80, 120), "skirt", hat=S(200, 60, 60), extras=(("apron", S(240, 200, 120)),)),
    "aroma_lady": person(MED, S(130, 80, 40), "long", S(170, 210, 140), S(120, 170, 110), "dress", extras=("blush",)),
    "breeder_m": person(TAN, BROWN, "short", S(240, 230, 200), S(110, 140, 90), extras=(("apron", S(120, 170, 110)),)),
    "breeder_f": person(FAIR, BROWN, "ponytail", S(240, 230, 200), S(110, 140, 90), "skirt", extras=(("apron", S(240, 160, 170)),)),
    "ranger_m": person(MED, BROWN, "short", S(80, 130, 80), KHAKI, hat=S(80, 130, 80), extras=(("bag", S(110, 80, 50)),)),
    "ranger_f": person(FAIR, AUBURN, "ponytail", S(80, 130, 80), KHAKI, "shorts", hat=S(80, 130, 80)),
    "rich_boy": person(FAIR, BLOND, "short", S(60, 60, 120), S(60, 60, 120), "shorts", extras=(("collar", W), ("tie", S(200, 40, 50)), ("socks", W))),
    "hiker_f": person(TAN, BROWN, "ponytail", S(200, 90, 60), KHAKI, "shorts", hat=S(160, 110, 60), extras=(("bag", S(110, 80, 50)),)),
    "fisher_f": person(FAIR, BLACK, "ponytail", S(240, 230, 120), DENIM, hat=S(240, 230, 120)),
    # People in town (generic), with all skin tones.
    "teen_boy": person(MED, BLACK, "spiky", S(60, 140, 200), DENIM),
    "teen_girl": person(DEEP, BLACK, "ponytail", S(240, 200, 60), DENIM, "shorts"),
    "businessman": person(FAIR, BROWN, "short", S(60, 60, 70), S(60, 60, 70), extras=(("collar", W), ("tie", S(40, 80, 160)))),
    "office_worker": person(DARK, BLACK, "bun", S(80, 80, 100), S(80, 80, 100), "skirt", extras=(("collar", W),)),
    "farmer": person(TAN, BROWN, "short", S(200, 80, 60), DENIM, hat=S(230, 200, 120), extras=(("overalls", DENIM),)),
    "farmer_f": person(MED, AUBURN, "pigtails", S(240, 230, 200), DENIM, hat=S(230, 200, 120), extras=(("overalls", DENIM), "freckles")),
    "gardener": person(DEEP, GREY, "short", S(110, 150, 90), S(90, 80, 60), hat=S(230, 200, 120), extras=(("gloves", S(110, 150, 90)),)),
    "student_m": person(FAIR, BLACK, "short", NAVY, S(60, 60, 70), extras=(("collar", W), ("tie", S(200, 40, 50)), ("bag", S(60, 60, 70)))),
    "student_f": person(MED, BROWN, "long", NAVY, S(60, 60, 110), "skirt", extras=(("collar", W), ("socks", W))),
    "jogger": person(DARK, BLACK, "short", S(240, 90, 60), S(50, 50, 60), "shorts", extras=(("headband", W), ("socks", W))),
    "jogger_f": person(FAIR, BLOND, "ponytail", S(80, 200, 160), S(50, 50, 60), "shorts", extras=(("headband", S(240, 90, 120)),)),
    "tourist": person(FAIR, GINGER, "short", S(240, 140, 60), KHAKI, "shorts", hat=S(250, 230, 160), extras=(("bag", S(60, 60, 70)),)),
    "musician": person(MED, BLACK, "long", S(110, 40, 60), BLK, extras=("shades",)),
    "dancer": person(DEEP, BLACK, "bun", S(240, 120, 180), S(240, 120, 180), "dress"),
    "delivery": person(FAIR, BROWN, "short", S(200, 140, 60), S(110, 80, 50), hat=S(200, 140, 60), extras=(("bag", S(110, 80, 50)),)),
    "builder": person(TAN, BROWN, "short", S(240, 140, 40), DENIM, hat=S(250, 210, 40), extras=(("stripe", S(220, 220, 220), 6), ("gloves", S(200, 160, 60)))),
    "doctor": person(DARK, BLACK, "short", S(90, 160, 200), S(60, 60, 80), extras=("coat", "glasses")),
    "doctor_f": person(FAIR, AUBURN, "bun", S(90, 160, 200), S(60, 60, 80), "skirt", extras=("coat",)),
    "librarian": person(FAIR, GREY, "bun", S(140, 110, 160), S(90, 70, 110), "skirt", extras=("glasses",)),
    "teacher": person(MED, BROWN, "short", S(170, 130, 90), S(80, 70, 60), extras=("glasses", ("tie", S(60, 120, 80)))),
    "postman": person(FAIR, BLACK, "short", S(200, 50, 50), NAVY, hat=S(200, 50, 50), extras=(("bag", S(110, 80, 50)),)),
    "baker": person(FAIR2, BLOND, "short", W, S(110, 90, 70), hat=W, extras=(("apron", W),)),
    "shopkeeper": person(DEEP, BLACK, "short", S(80, 150, 120), S(80, 70, 60), extras=(("apron", S(240, 230, 200)),)),
    "shopkeeper_f": person(FAIR, BROWN, "bun", S(220, 140, 160), S(80, 70, 60), "skirt", extras=(("apron", S(240, 230, 200)),)),
    "young_mum": person(MED, BROWN, "ponytail", S(170, 200, 230), DENIM, "skirt"),
    "young_dad": person(FAIR, BROWN, "short", S(110, 150, 110), DENIM, extras=("beard",)),
    "backpacker": person(TAN, BLOND, "long", S(200, 90, 60), KHAKI, "shorts", extras=(("bag", S(60, 110, 60)),)),
    "skater": person(FAIR, BROWN, "short", S(40, 40, 50), DENIM, hat=S(200, 60, 60)),
    "lifeguard": person(MED, BLOND, "short", S(220, 50, 50), S(220, 50, 50), "shorts", shoes=MED, extras=("shades",)),
    "photographer": person(FAIR, BLACK, "short", S(110, 100, 80), S(60, 60, 70), hat=S(60, 60, 70), extras=(("bag", BLK),)),
    "reporter": person(DARK, BLACK, "long", S(200, 60, 60), S(60, 60, 70), "skirt", extras=("lipstick",)),
    "waitress": person(FAIR, AUBURN, "ponytail", BLK, BLK, "skirt", extras=(("apron", W), ("collar", W))),
    "cook_f": person(MED, BLACK, "bun", W, S(60, 60, 70), "skirt", hat=W, extras=(("apron", W),)),
    "fisherman_old": person(FAIR2, WHITE_H, "short", S(240, 220, 60), S(240, 220, 60), hat=S(240, 220, 60), extras=("beard",)),
    "sailor_f": person(FAIR, BLACK, "ponytail", W, NAVY, "skirt", hat=W, extras=(("collar", NAVY),)),
    "grandad": person(DEEP, WHITE_H, "short", S(140, 120, 90), S(90, 80, 70), extras=("glasses", "moustache")),
    "nan": person(MED, WHITE_H, "bun", S(160, 90, 130), S(90, 70, 110), "dress", extras=("glasses",)),
    "boy": person(DARK, BLACK, "short", S(60, 160, 90), DENIM, "shorts"),
    "toddler_girl": person(FAIR, BLOND, "pigtails", S(250, 170, 200), S(250, 170, 200), "dress"),
}
SKINS = {
    "nurse": skin(FAIR, S(240, 130, 170), S(248, 176, 200), S(248, 176, 200), FAIR, FAIR, S(245, 245, 245), nurse_extra),
    "clerk": skin(FAIR, S(110, 70, 40), S(60, 110, 210), S(60, 110, 210), FAIR, S(40, 50, 90), S(30, 30, 35), clerk_extra),
    "youngster": skin(FAIR, S(60, 40, 25), S(245, 190, 60), S(245, 190, 60), FAIR, S(50, 90, 190), S(200, 40, 40), youngster_extra),
    "leader": skin(TAN, S(100, 66, 34), S(220, 120, 50), S(220, 120, 50), TAN, S(110, 80, 50), S(50, 40, 35), leader_extra),
}
SKINS.update(PEOPLE)
SKINS.update(MORE)
os.makedirs(OUT, exist_ok=True)
import sys
everyone = "--all" in sys.argv
for name, im in SKINS.items():
    f = os.path.join(OUT, name + ".png")
    if os.path.exists(f) and not everyone: continue
    im.save(f)
    print(name)
