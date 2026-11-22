"""
Looks the mod gives some places only (client/AreaLooks draws them, by where a block is), and Pallet Town's mailboxes.

- The pale oak fences round Pallet Town and on Route 1: solid white with a slight gradient.
- The roofs of your house and Blue's (acacia stairs): HeartGold's roof red, in rows of tiles, not planks.
- The signs made of pale oak shelves (two by two): HeartGold's sign, a white board with a light grey outline and a
  grey panel with lines of white writing, on a grey stand.
- The mailbox (a block of the mod's own, put by the houses): a red box on a silver post.

The grass's tufts on the dripstone round the water are drawn by lookground.py (they come from the grass's own).
Writes the mod's textures, models and blockstates under src/main/resources/assets/retromon.
"""
import json
import math
import os
import random

from PIL import Image

import lookground

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon")
TEX = os.path.join(ASSETS, "textures", "block", "area")
MODELS = os.path.join(ASSETS, "models", "block", "area")


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def save(name, im):
    os.makedirs(TEX, exist_ok=True)
    im.save(os.path.join(TEX, name + ".png"))


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=1)


def slotted(model):
    """A model whose faces name their textures outright, with the textures moved into its texture slots (faces
    take "#slot"), as Minecraft's models must have them."""
    slots = dict(model.get("textures", {}))
    names = {}
    for el in model["elements"]:
        for face in el["faces"].values():
            t = face["texture"]
            if t.startswith("#"):
                continue
            if t not in names:
                names[t] = "t%d" % len(names)
                slots[names[t]] = t
            face["texture"] = "#" + names[t]
    model["textures"] = slots
    return model


# ---------- the white fences ----------
def fence():
    """Solid white, a touch greyer towards the bottom (the posts run the texture's full height)."""
    top, bottom = hexc("#F6F7F9"), hexc("#D8DDE4")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        c = mix(top, bottom, (y / 15) ** 1.2)
        for x in range(16):
            im.putpixel((x, y), c)
    return im


# ---------- the roofs (HeartGold's Pallet roofs) ----------
ROOF = {"hi": hexc("#FF9F87"), "base": hexc("#FF8B73"), "mid": hexc("#F68362"), "low": hexc("#EE7B5A"),
        "shadow": hexc("#D56241"), "seam": hexc("#E67352"), "edge_top": hexc("#E0704F"), "edge_bottom": hexc("#C95A3A"),
        "under": hexc("#AC4A2E")}


# The green houses' roofs (Viridian City's: Let's Go's and HeartGold's greens) and the Gyms' (their jungle and oak
# turned to a warm tan roof, as FireRed's and HeartGold's Gyms), in the same rows of tiles.
ROOF_GREEN = {"hi": hexc("#86C46A"), "base": hexc("#6AAE55"), "mid": hexc("#5EA14C"), "low": hexc("#559546"), "shadow": hexc("#3F7836"),
              "seam": hexc("#4E8C42"), "edge_top": hexc("#4F8C43"), "edge_bottom": hexc("#3D7033"), "under": hexc("#33602C")}
ROOF_GYM = {"hi": hexc("#F0D592"), "base": hexc("#DDBB72"), "mid": hexc("#D2AE66"), "low": hexc("#C7A25C"), "shadow": hexc("#A47F40"),
            "seam": hexc("#BC9754"), "edge_top": hexc("#B48C4A"), "edge_bottom": hexc("#93703A"), "under": hexc("#7C5E31")}


def roof_tiles(ROOF=ROOF):
    """Two rows of tiles a block, running across the texture (the mod lays the rows along the eaves, the top of
    each row uphill): a light lip, the tile, its shadow on the row below; the joins between tiles staggered."""
    im = Image.new("RGBA", (16, 16))
    rows = ["hi", "base", "base", "base", "mid", "mid", "low", "shadow"]
    for y in range(16):
        course, k = divmod(y, 8)
        for x in range(16):
            c = ROOF[rows[k]]
            seams = (3, 11) if course == 0 else (7, 15)
            if 1 <= k <= 6 and x in seams:
                c = ROOF["seam"]
            elif 1 <= k <= 5 and (x - 1) % 16 in seams:
                c = mix(c, ROOF["hi"], 0.5)
            im.putpixel((x, y), c)
    return im


def roof_edge(ROOF=ROOF):
    """The roof's ends (where the rows of tiles are cut): a darker trim, a touch darker towards the bottom."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        c = mix(ROOF["edge_top"], ROOF["edge_bottom"], y / 15)
        for x in range(16):
            im.putpixel((x, y), c)
    return im


def solid(c):
    im = Image.new("RGBA", (16, 16), c)
    return im


# ---------- the signs (HeartGold's town sign) ----------
SIGN = {"outline": hexc("#B5BBC3"), "white": hexc("#F7F8FA"), "bright": hexc("#FFFFFF"), "panel_rim": hexc("#C5CDCD"),
        "panel": hexc("#A4ACB4"), "text": hexc("#EEF0F2"), "back_panel": hexc("#C9CED4"),
        "stand_top": hexc("#AEB5BC"), "stand_bottom": hexc("#8E959D"), "foot": hexc("#7E858D"), "foot_top": hexc("#959CA4")}


def board(width, back=False):
    """A board `width` pixels wide and 16 high, as seen from the front (or the back): a light grey outline, a white
    frame, a grey panel with a lighter rim and, on the front, two lines of white writing."""
    im = Image.new("RGBA", (width, 16))
    for y in range(16):
        for x in range(width):
            if x == 0 or y == 0 or x == width - 1 or y == 15:
                c = SIGN["outline"]
            elif x <= 2 or y <= 2 or x >= width - 3 or y >= 13:
                c = SIGN["bright"] if y == 1 else SIGN["white"]
            elif x == 3 or y == 3 or x == width - 4 or y == 12:
                c = SIGN["panel_rim"]
            else:
                c = SIGN["back_panel"] if back else SIGN["panel"]
            im.putpixel((x, y), c)
    if not back:
        # Two lines of writing (like "PALLET TOWN" and a line under it): words of different lengths, two pixels
        # apart, as many as fit in the panel, each line centred.
        room = width - 12
        for row, pattern in ((5, (9, 7, 8, 6)), (9, (6, 5, 5, 7, 4))):
            words, used = [], 0
            for w in pattern * 4:
                need = w + (2 if words else 0)
                if used + need > room:
                    w = room - used - (2 if words else 0)
                    if w >= 3:
                        words.append(w)
                    break
                words.append(w)
                used += need
            x = 6 + (room - (sum(words) + 2 * (len(words) - 1))) // 2
            for w in words:
                scribble(im, x, row, w, SIGN["text"], row)
                x += w + 2
    return im



def scribble(im, x, y, w, c, seed=0):
    """A word of handwriting w pixels wide on rows y and y+1 (14:15: a little squiggle on the signs, not a bar): its
    letters a pixel or two wide, some reaching up a row, some down, joined along the bottom, a gap between letters."""
    r = random.Random("%d,%d,%d" % (x, y, seed))
    xx, end = x, x + w
    while xx < end:
        lw = min(end - xx, r.choice((2, 2, 3)))
        kind = r.random()
        for i in range(lw):
            px = xx + i
            if kind < 0.3:                      # a tall letter: up a row on its first stroke
                rows = (y - 1, y, y + 1) if i == 0 else (y + 1,) if i == lw - 1 else (y, y + 1)
            elif kind < 0.55:                   # a round letter: top and bottom, open in the middle
                rows = (y, y + 1) if i in (0, lw - 1) else (y,)
            elif kind < 0.75:                   # a low one with a tail
                rows = (y + 1,) if i < lw - 1 else (y, y + 1)
            else:
                rows = (y, y + 1) if i % 2 == 0 else (y + 1,)
            for py in rows:
                if 0 <= px < im.width and 0 <= py < im.height:
                    im.putpixel((px, py), c)
        xx += lw
        if xx < end and r.random() < 0.6 and xx + 1 < end:
            im.putpixel((xx, y + 1), c)         # joined on to the next letter
        xx += 1

def edge(lines_on_rows):
    """The board's top, bottom and ends: white, with the outline along its front and back (rows or columns 5 and
    10, where the board's faces are)."""
    im = Image.new("RGBA", (16, 16), SIGN["white"])
    for i in range(16):
        for k in (5, 10):
            im.putpixel((i, k) if lines_on_rows else (k, i), SIGN["outline"])
    return im


def stand():
    """The stand: grey, a little darker towards the bottom."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        c = mix(SIGN["stand_top"], SIGN["stand_bottom"], y / 15)
        for x in range(16):
            im.putpixel((x, y), c)
    return im


def foot():
    im = Image.new("RGBA", (16, 16), SIGN["foot"])
    for x in range(16):
        im.putpixel((x, 13), SIGN["foot_top"])
    return im


FACES = ("north", "east", "south", "west", "up", "down")


def rot_point(x, z, turns):
    """(x, z) turned clockwise (seen from above) a quarter `turns` times about the block's middle."""
    for _ in range(turns):
        x, z = 16 - z, x
    return x, z


def rot_face(f, turns):
    order = ["north", "east", "south", "west"]
    if f in order:
        return order[(order.index(f) + turns) % 4]
    return f


def element(box, faces, turns):
    """A box (x0, y0, z0, x1, y1, z1, as for facing north) with {face: texture}, turned to face the other way."""
    x0, y0, z0, x1, y1, z1 = box
    a, b = rot_point(x0, z0, turns), rot_point(x1, z1, turns)
    fr = [min(a[0], b[0]), y0, min(a[1], b[1])]
    to = [max(a[0], b[0]), y1, max(a[1], b[1])]
    return {"from": fr, "to": to, "faces": {rot_face(f, turns): {"texture": t} for f, t in faces.items()}}


def sign_models(prefix="sign"):
    """The sign's parts, for each way it can face, as models the mod puts where the shelves are: the board (the top
    row of shelves) and the stand (the row under it), each as its left end (as you look at its front), right end,
    a middle block or a sign one block wide. Facing north: you look south at it, so its left is east (x = 16).
    prefix "wsign": the same in wood (the oak shelf signs, as our wooden sign)."""
    turns_of = {"north": 0, "east": 1, "south": 2, "west": 3}
    for facing, turns in turns_of.items():
        for part in ("l", "r", "m", "s"):
            outer_left = part in ("l", "s")    # the end at x = 16 (facing north) is the sign's end
            outer_right = part in ("r", "s")   # the end at x = 0
            tex = lambda n: "retromon:block/area/" + n
            # The board: the whole block across, 6 thick.
            faces = {"north": tex(prefix + "_front_" + part), "south": tex(prefix + "_back_" + part),
                     "up": tex(prefix + "_edge_h"), "down": tex(prefix + "_edge_h")}
            if outer_left:
                faces["east"] = tex(prefix + "_edge_v")
            if outer_right:
                faces["west"] = tex(prefix + "_edge_v")
            board_el = [element((0, 0, 5, 16, 16, 11), faces, turns)]
            # The stand: set in from the sign's ends, a little thinner than the board, on a foot.
            sx0 = 2 if outer_right else 0
            sx1 = 14 if outer_left else 16
            sfaces = {"north": tex(prefix + "_stand"), "south": tex(prefix + "_stand")}
            if outer_left:
                sfaces["east"] = tex(prefix + "_stand")
            if outer_right:
                sfaces["west"] = tex(prefix + "_stand")
            fx0 = 1 if outer_right else 0
            fx1 = 15 if outer_left else 16
            ffaces = {"north": tex(prefix + "_foot"), "south": tex(prefix + "_foot"), "up": tex(prefix + "_foot"), "down": tex(prefix + "_foot")}
            if outer_left:
                ffaces["east"] = tex(prefix + "_foot")
            if outer_right:
                ffaces["west"] = tex(prefix + "_foot")
            stand_el = [element((sx0, 2, 6, sx1, 16, 10), sfaces, turns), element((fx0, 0, 5, fx1, 2, 11), ffaces, turns)]
            for row, els in (("board", board_el), ("stand", stand_el)):
                write_json(os.path.join(MODELS, "%s_%s_%s_%s.json" % (prefix, row, part, facing)), slotted({
                    "parent": "minecraft:block/block", "ambientocclusion": False,
                    "textures": {"particle": tex(prefix + "_edge_h")}, "elements": els}))


def sign_textures():
    whole = board(32)
    back = board(32, back=True)
    # Facing north, the left end (as you look at the front) is the east block, whose front is the board's left half;
    # from the back, the same block is on your right.
    save("sign_front_l", whole.crop((0, 0, 16, 16)))
    save("sign_front_r", whole.crop((16, 0, 32, 16)))
    save("sign_back_l", back.crop((16, 0, 32, 16)))
    save("sign_back_r", back.crop((0, 0, 16, 16)))
    wide = board(48)
    save("sign_front_m", wide.crop((16, 0, 32, 16)))
    save("sign_back_m", board(48, back=True).crop((16, 0, 32, 16)))
    save("sign_front_s", board(16))
    save("sign_back_s", board(16, back=True))
    save("sign_edge_h", edge(True))
    save("sign_edge_v", edge(False))
    save("sign_stand", stand())
    save("sign_foot", foot())


# ---------- the mailbox ----------
MAIL = {"top": hexc("#FF834A"), "top_hi": hexc("#FFA070"), "light": hexc("#F66239"), "red": hexc("#DE3910"),
        "dark": hexc("#B42000"), "slot": hexc("#2C2C48"), "post_hi": hexc("#DCE2E6"), "post": hexc("#B4BCC2"), "post_lo": hexc("#99A1A8"),
        "plate": hexc("#8A9298")}


def mailbox_textures():
    """The box's front (its slot), its sides, its top, the post. The box is 7 wide (x 4.5 to 11.5) from 8 to 14 up,
    so its front takes rows 2 to 8 of its texture, columns 4 to 12."""
    def body(slot):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            c = MAIL["light"] if y <= 3 else MAIL["red"] if y <= 6 else MAIL["dark"]
            for x in range(16):
                im.putpixel((x, y), c)
        if slot:
            for x in range(6, 10):
                im.putpixel((x, 5), MAIL["slot"])
            for x in range(6, 10):
                im.putpixel((x, 6), mix(MAIL["red"], MAIL["dark"], 0.5))
        return im
    save("mailbox_front", body(True))
    save("mailbox_side", body(False))
    top = Image.new("RGBA", (16, 16), MAIL["top"])
    for y in range(16):
        top.putpixel((7, y), MAIL["top_hi"])
        top.putpixel((8, y), MAIL["top_hi"])
    save("mailbox_top", top)
    post = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            post.putpixel((x, y), MAIL["post_hi"] if x % 2 == 1 else MAIL["post"])
    save("mailbox_post", post)
    save("mailbox_plate", solid(MAIL["plate"]))
    save("mailbox_under", solid(MAIL["post_lo"]))


def mailbox_model():
    """Facing north (the slot towards you as you stand north of it): a small plate, the silver post, the red box
    and its rounded top. The blockstate turns it."""
    t = lambda n: "retromon:block/area/" + n
    els = [
        {"from": [6, 0, 6], "to": [10, 0.5, 10], "faces": {f: {"texture": t("mailbox_plate")} for f in ("north", "east", "south", "west", "up")}},
        {"from": [7, 0.5, 7], "to": [9, 8, 9], "faces": {f: {"texture": t("mailbox_post")} for f in ("north", "east", "south", "west")}},
        {"from": [4.5, 8, 2], "to": [11.5, 14, 14], "faces": {"north": {"texture": t("mailbox_front")}, "south": {"texture": t("mailbox_side")},
                                                          "east": {"texture": t("mailbox_side")}, "west": {"texture": t("mailbox_side")},
                                                          "down": {"texture": t("mailbox_under")}, "up": {"texture": t("mailbox_top")}}},
        {"from": [5.5, 14, 2], "to": [10.5, 15.5, 14], "faces": {f: {"texture": t("mailbox_top")} for f in ("north", "east", "south", "west", "up")}},
    ]
    write_json(os.path.join(ASSETS, "models", "block", "mailbox.json"), slotted({"parent": "minecraft:block/block", "ambientocclusion": False,
                                                                                "textures": {"particle": t("mailbox_side")}, "elements": els}))
    write_json(os.path.join(ASSETS, "blockstates", "mailbox.json"), {"variants": {
        "facing=north": {"model": "retromon:block/mailbox"}, "facing=east": {"model": "retromon:block/mailbox", "y": 90},
        "facing=south": {"model": "retromon:block/mailbox", "y": 180}, "facing=west": {"model": "retromon:block/mailbox", "y": 270}}})


# ---------- the short sign (HeartGold's) over a standing pale oak sign ----------
def short_board(width, back=False):
    """The board of a sign a block high (10 pixels): the same outline, frame and panel, one line of writing."""
    im = Image.new("RGBA", (width, 16), (0, 0, 0, 0))
    h = 10
    for y in range(h):
        for x in range(width):
            if x == 0 or y == 0 or x == width - 1 or y == h - 1:
                c = SIGN["outline"]
            elif x <= 1 or y <= 1 or x >= width - 2 or y >= h - 2:
                c = SIGN["bright"] if y == 1 else SIGN["white"]
            elif x == 2 or y == 2 or x == width - 3 or y == h - 3:
                c = SIGN["panel_rim"]
            else:
                c = SIGN["back_panel"] if back else SIGN["panel"]
            im.putpixel((x, y), c)
    if not back:
        room = width - 8
        words, used = [], 0
        for w in (7, 5, 6, 4, 7) * 3:
            need = w + (2 if words else 0)
            if used + need > room:
                w = room - used - (2 if words else 0)
                if w >= 3:
                    words.append(w)
                break
            words.append(w)
            used += need
        x = 4 + (room - (sum(words) + 2 * (len(words) - 1))) // 2
        for w in words:
            scribble(im, x, 4, w, SIGN["text"], 4)
            x += w + 2
    return im


def short_sign_models():
    """A standing pale oak sign (a block high) as HeartGold's sign, built round Minecraft's own (its board, 16 by 8
    from 9.3 to 17.3 up and 1.3 thick in the middle, and its stick, are inside it, so they never show): the board
    from 8.5 to 18.5 up, the stand under it and its foot. Its ends, middles and one-block signs as the shelves'."""
    tex = lambda n: "retromon:block/area/" + n
    full = [0, 0, 16, 16]
    for facing, turns in {"north": 0, "east": 1, "south": 2, "west": 3}.items():
        for part in ("l", "r", "m", "s"):
            outer_left, outer_right = part in ("l", "s"), part in ("r", "s")
            bx0, bx1 = (-0.25 if outer_right else 0), (16.25 if outer_left else 16)
            uvx = lambda a, b: [16 - b, 0, 16 - a, 0]
            faces = {"north": {"texture": tex("sign_short_front_" + part), "uv": [0, 0, 16, 10]},
                     "south": {"texture": tex("sign_short_back_" + part), "uv": [0, 0, 16, 10]},
                     "up": {"texture": tex("sign_edge_h"), "uv": [0, 5, 16, 11]}, "down": {"texture": tex("sign_edge_h"), "uv": [0, 5, 16, 11]}}
            if outer_left:
                faces["east"] = {"texture": tex("sign_edge_v"), "uv": [5, 3, 11, 13]}
            if outer_right:
                faces["west"] = {"texture": tex("sign_edge_v"), "uv": [5, 3, 11, 13]}
            els = [element_uv((bx0, 8.5, 5, bx1, 18.5, 11), faces, turns)]
            sx0, sx1 = (2 if outer_right else 0), (14 if outer_left else 16)
            sf = {"north": {"texture": tex("sign_stand"), "uv": [16 - sx1, 7.5, 16 - sx0, 14]}, "south": {"texture": tex("sign_stand"), "uv": [sx0, 7.5, sx1, 14]}}
            if outer_left:
                sf["east"] = {"texture": tex("sign_stand"), "uv": [6, 7.5, 10, 14]}
            if outer_right:
                sf["west"] = {"texture": tex("sign_stand"), "uv": [6, 7.5, 10, 14]}
            els.append(element_uv((sx0, 2, 6, sx1, 8.5, 10), sf, turns))
            fx0, fx1 = (1 if outer_right else 0), (15 if outer_left else 16)
            ff = {f: {"texture": tex("sign_foot")} for f in ("north", "south", "up", "down")}
            if outer_left:
                ff["east"] = {"texture": tex("sign_foot")}
            if outer_right:
                ff["west"] = {"texture": tex("sign_foot")}
            els.append(element_uv((fx0, 0, 5, fx1, 2, 11), ff, turns))
            write_json(os.path.join(MODELS, "sign_short_%s_%s.json" % (part, facing)), slotted({
                "parent": "minecraft:block/block", "ambientocclusion": False, "textures": {"particle": tex("sign_edge_h")}, "elements": els}))


def element_uv(box, faces, turns):
    """As element(), for faces that carry their own uv."""
    x0, y0, z0, x1, y1, z1 = box
    a, b = rot_point(x0, z0, turns), rot_point(x1, z1, turns)
    fr = [min(a[0], b[0]), y0, min(a[1], b[1])]
    to = [max(a[0], b[0]), y1, max(a[1], b[1])]
    return {"from": fr, "to": to, "faces": {rot_face(f, turns): dict(v) for f, v in faces.items()}}


# ---------- the wooden sign (HeartGold's) over a standing spruce sign ----------
WOOD = {"face": hexc("#C8945A"), "face_hi": hexc("#D9AA70"), "grain": hexc("#B07C46"), "frame": hexc("#8A5A32"), "frame_hi": hexc("#A06C3E"),
        "dark": hexc("#6E4526"), "post": hexc("#7A4D2A"), "post_hi": hexc("#946039")}


def wood_face(back=False):
    """A wooden board, a frame of darker wood round planks running across, two lines of carved writing (front)."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x == 0 or y == 0 or x == 15 or y == 15:
                c = WOOD["dark"]
            elif x == 1 or y == 1 or x == 14 or y == 14:
                c = WOOD["frame_hi"] if (x == 1 or y == 1) else WOOD["frame"]
            else:
                c = WOOD["face_hi"] if y in (2, 7, 12) else WOOD["grain"] if y in (6, 11) else WOOD["face"]
            im.putpixel((x, y), c)
    if not back:
        for row, words in ((4, (4, 3, 3)), (9, (3, 5))):
            x = 3
            for w in words:
                scribble(im, x, row, min(13, x + w) - x, WOOD["dark"], row)
                x += w + 1
    return im


def wood_sign_models():
    """A standing spruce sign as HeartGold's wooden sign, taller: a board (17 wide, 16 high, from 9 up) on a thick
    post, round Minecraft's own sign (its board and stick inside, so they never show)."""
    tex = lambda n: "retromon:block/area/" + n
    for facing, turns in {"north": 0, "east": 1, "south": 2, "west": 3}.items():
        bf = {"north": {"texture": tex("wood_sign_front"), "uv": [0, 0, 16, 16]}, "south": {"texture": tex("wood_sign_back"), "uv": [0, 0, 16, 16]},
              "east": {"texture": tex("wood_sign_edge"), "uv": [6, 0, 9, 16]}, "west": {"texture": tex("wood_sign_edge"), "uv": [6, 0, 9, 16]},
              "up": {"texture": tex("wood_sign_edge"), "uv": [0, 6, 16, 9]}, "down": {"texture": tex("wood_sign_edge"), "uv": [0, 6, 16, 9]}}
        pf = {f: {"texture": tex("wood_post"), "uv": [6, 7, 9, 16]} for f in ("north", "south", "east", "west")}
        pf["down"] = {"texture": tex("wood_post"), "uv": [6, 6, 9, 9]}
        cap = {f: {"texture": tex("wood_post"), "uv": [5, 0, 11, 1]} for f in ("north", "south", "east", "west")}
        cap["up"] = {"texture": tex("wood_post"), "uv": [5, 6, 11, 10]}
        els = [element_uv((-0.5, 9, 6.5, 16.5, 25, 9.5), bf, turns), element_uv((6.5, 0, 6.5, 9.5, 9, 9.5), pf, turns),
               element_uv((5, 25, 6, 11, 26, 10), cap, turns)]
        write_json(os.path.join(MODELS, "wood_sign_%s.json" % facing), slotted({
            "parent": "minecraft:block/block", "textures": {"particle": tex("wood_sign_edge")}, "elements": els}))


def wood_textures():
    save("wood_sign_front", wood_face())
    save("wood_sign_back", wood_face(back=True))
    edge = Image.new("RGBA", (16, 16), WOOD["frame"])
    for i in range(16):
        edge.putpixel((i, 6), WOOD["frame_hi"])
    save("wood_sign_edge", edge)
    post = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            post.putpixel((x, y), WOOD["post_hi"] if x in (6, 7) else WOOD["post"])
    save("wood_post", post)


# ---------- Viridian City's path: three patterns of the same white, a grey kerb, the grass rounded onto it ----------
PATH = {"base": hexc("#DDDED9"), "lo": hexc("#D2D3CD"), "bump": hexc("#C6C8C1"), "bump_hi": hexc("#EFF0EC"), "bump_lo": hexc("#ACAFA8")}
# Where each block's bumps are (a block of each pattern): calcite four, diorite five, polished diorite three bigger.
BUMPS = {"calcite": [(3, 3, 2), (10, 4, 2), (5, 10, 2), (12, 11, 2)],
         "diorite": [(2, 6, 2), (7, 2, 2), (12, 5, 2), (5, 12, 2), (11, 11, 2)],
         "polished_diorite": [(4, 4, 3), (10, 9, 3), (3, 11, 2)]}


def path_tile(name):
    """The path's white (one colour for all three, a little soft variation), with this block's bumps: each lit on
    its top left, shaded on its bottom right, so the three patterns differ while the path reads as one."""
    r = random.Random("path" + name)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = mix(PATH["base"], PATH["lo"], r.random() * 0.45)
            im.putpixel((x, y), c)
    for bx, by, size in BUMPS[name]:
        # A round pebble: its body, lit at its top left, its shadow just past its bottom right.
        for j in range(size):
            for i in range(size):
                if size == 3 and (i, j) in ((0, 0), (2, 0), (0, 2), (2, 2)):
                    continue
                im.putpixel((bx + i, by + j), PATH["bump"])
        im.putpixel((bx + (1 if size == 3 else 0), by), PATH["bump_hi"])
        im.putpixel((bx, by + (1 if size == 3 else 0)), PATH["bump_hi"])
        im.putpixel((bx + size, by + size - 1), PATH["bump_lo"])
        im.putpixel((bx + size - 1, by + size), PATH["bump_lo"])
    return im


def coverage_at(tile, depth, amp, round_):
    """Distance in from the grass's side of a tile (lookground's 17), its corners rounded: (16 x 16 of distances)."""
    edges, corners = lookground.TILES[tile]
    out = [[99.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            ds = []
            for e in edges:
                t, d = {"top": (cx, cy), "bottom": (cx, 16 - cy), "left": (cy, cx), "right": (cy, 16 - cx)}[e]
                ds.append(d - amp * lookground.tufts(t))
            for c in corners:
                px, py = {"tl": (0, 0), "tr": (16, 0), "bl": (0, 16), "br": (16, 16)}[c]
                ds.append(math.hypot(cx - px, cy - py))
            if not ds:
                continue
            f = ds[0]
            for d in ds[1:]:
                f = lookground.smin(f, d, round_)
            out[y][x] = f
    return out


KERB = {"outer": hexc("#8F959D"), "kerb": hexc("#A2A8AF"), "lip": hexc("#BCC1C6"), "shadow": hexc("#B9BCB6")}
KERB_W = 4.6


def kerb_tile(tile):
    """The kerb along the grass: light grey, a lighter lip along the path, a soft line where it meets the white;
    its corners rounded (where the path turns the corner it's round, as the grass follows it)."""
    d = coverage_at(tile, KERB_W, 0.0, 10.0)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = d[y][x]
            if v < 1.4:
                c = KERB["outer"]
            elif v < KERB_W - 0.9:
                c = KERB["kerb"]
            elif v < KERB_W:
                c = KERB["lip"]
            elif v < KERB_W + 0.8:
                c = KERB["shadow"]
            else:
                continue
            im.putpixel((x, y), c)
    return im


def kerb_grass_tile(tile, g16):
    """The grass's own edge onto the kerb: a little way in, small tufts, following the kerb round its corners."""
    d = coverage_at(tile, 1.2, 0.9, 10.0)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    gp = g16.load()
    for y in range(16):
        for x in range(16):
            if d[y][x] < 1.2:
                r, g, b, _a = gp[x, y]
                im.putpixel((x, y), (r, g, b, 255))
    return im


# ---------- recoloured, in their own patterns: the lamps' posts, the Centers' reds, the Marts' blues ----------
VANILLA = lookground.VANILLA
LAMP = [hexc(c) for c in ("#111316", "#1A1D21", "#23272C", "#2F343A", "#424850")]
CENTER_RED = [hexc(c) for c in ("#7A1A1F", "#A6282F", "#C6363D", "#DC4F55", "#EE7A7E")]
MART_BLUE = [hexc(c) for c in ("#163468", "#234F98", "#3066B7", "#4882D1", "#72A5E5")]


def recolour_soft(src, ramp, spread=70.0):
    """A texture's own pattern on a new ramp, keeping how much it varies (a flat one, as concrete, stays flat):
    each pixel's brightness against the texture's middle, over `spread`, from the ramp's middle."""
    im = Image.open(src).convert("RGBA")
    p = im.load()
    w, h = im.size
    lum = [0.3 * p[x, y][0] + 0.59 * p[x, y][1] + 0.11 * p[x, y][2] for y in range(h) for x in range(w) if p[x, y][3]]
    mid = sum(lum) / len(lum)
    for y in range(h):
        for x in range(w):
            r, g, b, a = p[x, y]
            if not a:
                continue
            t = max(0.0, min(1.0, 0.5 + (0.3 * r + 0.59 * g + 0.11 * b - mid) / spread))
            seg = t * (len(ramp) - 1)
            i = min(len(ramp) - 2, int(seg))
            p[x, y] = mix(ramp[i], ramp[i + 1], seg - i)[:3] + (a,)
    return im


def metal(base, seam=True):
    """A solid colour as a metal sheet: lit along its top, a soft sheen across it, a touch darker towards the
    bottom, a fine line at its left edge where the sheets meet (none for a plain one)."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.10 - 0.16 * (y / 15)
            if y == 0:
                k = 0.2
            band = (x + y) % 16
            if 5 <= band <= 7:
                k += 0.05
            c = mix(base, (255, 255, 255), k) if k >= 0 else mix(base, (0, 0, 0), -k)
            if seam and x == 0 and y > 0:
                c = mix(c, (0, 0, 0), 0.12)
            im.putpixel((x, y), c)
    return im


def flat(base, lines=True):
    """A roof's flat (planks in John's builds): one solid colour, a fine line across every eight pixels (the joins of
    its sheets), lit on their upper side."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = base
            if lines and y % 8 == 7:
                c = mix(base, (0, 0, 0), 0.14)
            elif lines and y % 8 == 0:
                c = mix(base, (255, 255, 255), 0.1)
            im.putpixel((x, y), c)
    return im


def window_frame():
    """A window's frame (pale oak stairs and slabs by the green houses' windows): solid white, its edges a touch
    greyer, so it reads as a painted frame."""
    im = Image.new("RGBA", (16, 16), hexc("#F1F0EA"))
    for i in range(16):
        for (x, y) in ((i, 0), (0, i)):
            im.putpixel((x, y), hexc("#FBFBF8"))
        for (x, y) in ((i, 15), (15, i)):
            im.putpixel((x, y), hexc("#D9D8D1"))
    return im


def solid_shaded(base, top=0.06, bottom=-0.08, speckle=0.0, seed="s"):
    """One solid colour, a touch lighter at the top and darker at the bottom (and, for stone, the faintest speckle),
    so a face reads as one painted or cast surface, not a pattern."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        k = top + (bottom - top) * (y / 15)
        for x in range(16):
            kk = k + (r.random() - 0.5) * speckle
            c = mix(base, (255, 255, 255), kk) if kk >= 0 else mix(base, (0, 0, 0), -kk)
            im.putpixel((x, y), c)
    return im


def bricks(base, mortar, course=4, length=8, shade=0.05):
    """Clean, even bricks (HeartGold's lab and its chimney, painted): courses `course` high, `length` long,
    staggered, a fine mortar line under each course and at each joint, each brick lit a touch along its top."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        row, k = divmod(y, course)
        for x in range(16):
            off = (length // 2) * (row % 2)
            if k == course - 1 or (x + off) % length == 0:
                c = mortar
            elif k == 0:
                c = mix(base, (255, 255, 255), shade)
            else:
                c = base
            im.putpixel((x, y), c)
    return im


def mixed_bricks(base, light, dark, mortar, seed="lb", course=4, length=8):
    """Let's Go's League gate brick: courses of warm red-orange brick, staggered, with here and there a lighter or a
    darker one, a fine mortar line under each course and at each joint, each brick lit a touch along its top."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    pick = {}
    for row in range(16 // course):
        for b in range(-1, 16 // length + 1):
            pick[(row, b)] = base
    # (A lighter brick, a darker one and a half-light one in each block, in different places, so it reads as a mixed
    # wall rather than stripes.)
    for (row, b), c in (((0, 1), light), ((2, 0), dark), ((3, 1), mix(base, light, 0.4))):
        pick[(row, b)] = c
        if b == 0: pick[(row, 2)] = c
    for y in range(16):
        row, k = divmod(y, course)
        off = (length // 2) * (row % 2)
        for x in range(16):
            c = pick[(row, (x + off) // length)]
            if k == course - 1 or (x + off) % length == 0:
                c = mortar
            elif k == 0:
                c = mix(c, (255, 255, 255), 0.07)
            im.putpixel((x, y), c)
    return im


def bush(seed="bush"):
    """A round bush's leaves, as Let's Go's (the League gate's four little trees): soft clumps of a fresh green, lit
    on their tops and shaded under, with no holes (they're drawn without the biome's tint)."""
    r = random.Random(seed)
    base, lit, shade = hexc("#4E9E3C")[:3], hexc("#79C25A")[:3], hexc("#2F6E2C")[:3]
    im = Image.new("RGBA", (16, 16), base + (255,))
    for _ in range(9):
        cx, cy, rad = r.uniform(0, 16), r.uniform(0, 16), r.uniform(2.5, 4.2)
        for y in range(16):
            for x in range(16):
                for ox in (-16, 0, 16):
                    for oy in (-16, 0, 16):
                        dx, dy = x + 0.5 - cx - ox, y + 0.5 - cy - oy
                        d = (dx * dx + dy * dy) ** 0.5
                        if d < rad:
                            k = -dy / rad
                            c = mix(base, lit, max(0.0, k) * 0.8) if k > 0 else mix(base, shade, -k * 0.6)
                            if d > rad - 0.8: c = mix(c, shade, 0.45)
                            im.putpixel((x, y), tuple(c[:3]) + (255,))
    return im


def recolours():
    v = lambda n: os.path.join(VANILLA, "textures", "block", n + ".png")
    for n in ("tuff", "polished_tuff", "chiseled_tuff_bricks", "chiseled_tuff_bricks_top"):
        save("lamp_" + n, lookground.recolour(v(n), LAMP))
    save("lamp_gray_wool", recolour_soft(v("gray_wool"), LAMP))
    # The Pokémon Centers' reds and the Marts' blues: Let's Go's, as solid metal sheets, each of John's blocks its own
    # shade (the roof's sheets, the trim and the emblem, the wool and carpets that lift parts of it).
    save("center_stripped_mangrove_log", metal(hexc("#C8363D")))
    save("center_stripped_mangrove_log_top", metal(hexc("#C8363D"), seam=False))
    save("center_mangrove_planks", metal(hexc("#DB4A50"), seam=False))
    save("mart_blue_concrete", metal(hexc("#3268B8")))
    save("mart_blue_wool", metal(hexc("#2A5AA6"), seam=False))
    # The Gyms' roofs: the stairs get the tan tiles (the roof look), their flats and beams solid tan shades.
    save("gym_flat", flat(hexc("#D8B46A")))
    save("gym_flat_light", flat(hexc("#E6C783")))
    save("gym_beam", flat(hexc("#A57A40"), lines=False))
    save("gym_beam_top", flat(hexc("#B3884A"), lines=False))
    save("window_frame", window_frame())
    # The lamps (11:16): the posts a solid off-black, the cap metal, the chiselled blocks kept in their pattern.
    save("lamp_post", solid_shaded(hexc("#2B2D31"), 0.05, -0.06))
    save("lamp_cap", metal(hexc("#3E434A"), seam=False))
    # Pallet Town's houses: their pillars and steps a solid light grey, the dark parts round their underside solid,
    # the trim across the front a solid off-white, the drainpipe metal.
    save("house_stone", solid_shaded(hexc("#B9C0C7"), 0.05, -0.07, 0.015, "hs"))
    save("house_dark", solid_shaded(hexc("#59616C"), 0.04, -0.06, 0.0))
    save("house_trim", solid_shaded(hexc("#E8E7E1"), 0.03, -0.05))
    save("drainpipe", metal(hexc("#8C949C"), seam=False))
    # Prof. Oak's lab (as HeartGold's): its base and steps solid stone, the band under its roof lighter, its walls
    # clean pale yellow brick, its pillars a light brown, the awnings metal (red over the windows, teal-green over the
    # door), the chimney painted brick.
    save("lab_stone", solid_shaded(hexc("#A6ABB1"), 0.05, -0.07, 0.02, "ls"))
    save("lab_trim", solid_shaded(hexc("#CACED3"), 0.04, -0.05))
    save("lab_brick", bricks(hexc("#F2E6B2"), hexc("#D8C98C")))
    save("lab_pillar", solid_shaded(hexc("#C9A47A"), 0.06, -0.07))
    save("lab_awning_red", metal(hexc("#C9524F"), seam=False))
    save("lab_awning_door", metal(hexc("#1FA27A"), seam=False))
    save("lab_chimney", bricks(hexc("#B9473A"), hexc("#A23E33"), shade=0.06))
    # The route gates (every one on the map): the roof and the overhang over the way in gunmetal metal (the roof's
    # rim a shade darker), the pillars a solid grey, the steps solid stone, the walls each their own solid shade.
    save("gate_roof", metal(hexc("#5E656F")))
    save("gate_roof_rim", metal(hexc("#4D535C"), seam=False))
    save("gate_pillar", solid_shaded(hexc("#ADB2B8"), 0.05, -0.07))
    save("gate_stone", solid_shaded(hexc("#9CA1A7"), 0.04, -0.06, 0.02, "gs"))
    save("gate_wall_birch", solid_shaded(hexc("#E5D9AD"), 0.04, -0.05))
    save("gate_wall_oak", solid_shaded(hexc("#CBAF7F"), 0.04, -0.06))
    save("gate_wall_oak_top", solid_shaded(hexc("#D6BC8E"), 0.0, 0.0))
    save("gate_beam", solid_shaded(hexc("#B08757"), 0.04, -0.06))
    save("gate_beam_top", solid_shaded(hexc("#BC9563"), 0.0, 0.0))
    # The POKéMON LEAGUE's gate on Route 22 (12:44): its roof the gates' metal but a lime green (FireRed's is pale
    # green), the deepslate brick pillars at its doors metal, the oak over its doors a lime green as Let's Go's arch,
    # its walls Let's Go's warm brick with lighter and darker ones mixed in, its four little trees a wooden trunk and
    # a round bush of fresh green leaves.
    save("league_roof", metal(hexc("#9CBF5C")))
    save("league_roof_rim", metal(hexc("#86A84C"), seam=False))
    save("league_pillar", metal(hexc("#8A929B"), seam=False))
    save("league_arch", metal(hexc("#ADB664"), seam=False))
    save("league_brick", mixed_bricks(hexc("#C8673C"), hexc("#E6B06C"), hexc("#A54C2C"), hexc("#8D4A30")))
    save("league_trunk", solid_shaded(hexc("#72502F"), 0.05, -0.08, 0.04, "lt"))
    save("league_bush", bush())


# ---------- Diglett's Cave, as FireRed's (13:07) ----------

def _noise(w, h, seed, cells):
    """A smooth value noise over w by h that wraps at its edges (so a picture laid over blocks has no seams): cells
    across, bilinear between random corners, 0 to 1."""
    r = random.Random(seed)
    g = [[r.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            fx, fy = x / w * cells, y / h * cells
            ix, iy = int(fx), int(fy)
            tx, ty = fx - ix, fy - iy
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a, b = g[iy % cells][ix % cells], g[iy % cells][(ix + 1) % cells]
            c, d = g[(iy + 1) % cells][ix % cells], g[(iy + 1) % cells][(ix + 1) % cells]
            out[y][x] = (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty
    return out


def cave_wall_top():
    """The cave's rock seen from above (FireRed's olive cave top): an olive khaki in rows of little scales, each lit on
    its upper edge and shaded under, a few darker flecks."""
    base, light, dark = hexc("#B5AE6C"), hexc("#CBC488"), hexc("#958E54")
    im = Image.new("RGBA", (16, 16), base)
    for y in range(16):
        for x in range(16):
            row = y // 4
            cx = (x + (2 if row % 2 else 0)) % 4
            cy = y % 4
            c = base
            if cy == 0 and cx in (1, 2): c = light
            elif cy == 3 or (cy == 2 and cx in (0, 3)): c = dark
            im.putpixel((x, y), c)
    r = random.Random("cw")
    for _ in range(6):
        im.putpixel((r.randrange(16), r.randrange(16)), mix(dark, (0, 0, 0), 0.25))
    return im


def cave_wall_side():
    """The cave's rock faces (FireRed's cliffs inside the cave): a darker olive brown in rough upright strata, lit along
    the top edge and darker towards the foot."""
    base, light, dark = hexc("#8F7E52"), hexc("#A8966A"), hexc("#6F6040")
    r = random.Random("cs")
    im = Image.new("RGBA", (16, 16))
    cols = [r.choice((0, 0, 1, -1)) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            k = cols[x] * 0.5 + (r.random() - 0.5) * 0.35
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            c = mix(c, dark, y / 15 * 0.35)
            if y == 0: c = mix(light, (255, 255, 255), 0.1)
            if x % 5 == 4 and y > 2: c = mix(c, dark, 0.4)
            im.putpixel((x, y), c)
    return im


def cave_ground(base, light, dark, seed, size=64, speck=0.04, patches=0.45):
    """A cave floor laid over four blocks by four (no repeat you'd see): its colour with soft, patchy lighter and darker
    areas (FireRed's floor: brown with paler patches), a fine grain, and a few small stones."""
    n1, n2 = _noise(size, size, seed + "a", 4), _noise(size, size, seed + "b", 8)
    r = random.Random(seed)
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = n1[y][x] * 0.7 + n2[y][x] * 0.3
            k = (v - 0.5) * 2 * patches + (r.random() - 0.5) * speck * 2
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            im.putpixel((x, y), c)
    for _ in range(size // 3):
        x, y = r.randrange(size), r.randrange(size)
        im.putpixel((x, y), dark)
        if x + 1 < size: im.putpixel((x + 1, y), mix(dark, light, 0.5))
    return im


def cave_boulder():
    """A boulder's stone (FireRed's cave rocks, 13:19: rocks, never bricks): one smooth grey olive stone, softly mottled
    with no lines in it and no light or dark band (the light falling on the rounded mound shades it), so each mound reads
    as one rock."""
    base, light, dark = hexc("#938F72"), hexc("#B3AF90"), hexc("#6E6B54")
    n1, n2 = _noise(16, 16, "bd1", 2), _noise(16, 16, "bd2", 4)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (n1[y][x] - 0.5) * 0.9 + (n2[y][x] - 0.5) * 0.4
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            im.putpixel((x, y), c)
    return im


def cave_boulder_tone(base, seed):
    """As cave_boulder, in another stone (Mt. Moon's grey)."""
    light, dark = mix(base, (255, 255, 255), 0.22), mix(base, (0, 0, 0), 0.25)
    n1, n2 = _noise(16, 16, seed + "1", 2), _noise(16, 16, seed + "2", 4)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (n1[y][x] - 0.5) * 0.9 + (n2[y][x] - 0.5) * 0.4
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def boulder_models():
    """Boulders (the cave's rocks: John's tuff, cobblestone and slabs), round mounds of stone stepped in, two shapes
    for the full blocks and a low one for the slabs (the look picks one by where it is)."""
    # Each a round dome of stone: at each height a cross of two boxes (so its corners are cut, round from above), each
    # level smaller, low steps so its sides read as a curve, not stacked slabs.
    def dome(cx, cz, r, h, levels):
        out = []
        for i in range(levels):
            t0, t1 = i / levels, (i + 1) / levels
            rr = r * (1 - t0 * t0) ** 0.5 if i else r
            rr = max(1.0, r * (1 - (t0 + 0.5 / levels) ** 2) ** 0.5)
            cut = rr * 0.42
            y0, y1 = round(h * t0, 2), round(h * t1, 2)
            out.append([round(cx - rr, 2), y0, round(cz - rr + cut, 2), round(cx + rr, 2), y1, round(cz + rr - cut, 2)])
            out.append([round(cx - rr + cut, 2), y0, round(cz - rr, 2), round(cx + rr - cut, 2), y1, round(cz + rr, 2)])
        return out
    shapes = {
        "boulder_a": dome(8, 8, 7.2, 11, 6),
        "boulder_b": dome(7, 8.5, 6.2, 9, 5) + dome(12, 4, 3.4, 5, 3),
        "boulder_small": dome(8, 8, 6.5, 6, 4),
    }
    for prefix, rock in (("boulder", "cave_boulder"), ("mboulder", "moon_boulder")):
      for name, boxes in shapes.items():
        name = name.replace("boulder", prefix, 1)
        # (No floor of its own: the mod draws the floor under it, laid as the floor round it is, so no square shows; 14:15.)
        els = []
        for b in boxes:
            els.append({"from": b[:3], "to": b[3:], "faces": {f: {"texture": "#rock"} for f in ("north", "south", "east", "west", "up", "down")}})
        write_json(os.path.join(MODELS, name + ".json"), {"parent": "block/block", "ambientocclusion": True,
                                                           "textures": {"rock": "retromon:block/area/" + rock, "particle": "retromon:block/area/" + rock},
                                                           "elements": els})


def _cells(size, n, seed):
    """Points for a pattern of cells over size by size that wraps at its edges (so it runs on across blocks)."""
    r = random.Random(seed)
    return [(r.random() * size, r.random() * size, r.random()) for _ in range(n)]


def rock_face(base, light, dark, seed, size=64, cells=26, strata=0.0):
    """A rock face laid over four blocks by four (14:15: rock, not stripes): big angular facets of stone, each its own
    shade, lit on its upper left, a dark crack between them; fine grit; faint layers across if strata."""
    pts = _cells(size, cells, seed)
    n = _noise(size, size, seed + "n", 8)
    r = random.Random(seed + "g")
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            best, second, shade, bx, by = 1e9, 1e9, 0, 0, 0
            for (px, py, k) in pts:
                for ox in (-size, 0, size):
                    for oy in (-size, 0, size):
                        d = (x - px - ox) ** 2 + (y - py - oy) ** 2
                        if d < best:
                            second, best, shade, bx, by = best, d, k, px + ox, py + oy
                        elif d < second:
                            second = d
            edge = second ** 0.5 - best ** 0.5
            # Each facet lit from its upper left (a slope away from its middle), its own shade.
            k = (shade - 0.5) * 0.16 + ((bx - x) + (by - y)) / 40.0 * 0.5 + (n[y][x] - 0.5) * 0.08 + (r.random() - 0.5) * 0.05
            if strata:
                k += math_sin(y * 0.55 + n[y][x] * 3) * strata
            c = mix(base, light, min(1, k * 2.2)) if k > 0 else mix(base, dark, min(1, -k * 2.2))
            if edge < 1.1:
                c = mix(c, dark, 0.55)
            elif edge < 2.0:
                c = mix(c, light, 0.18)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def math_sin(v):
    return math.sin(v)


def cave_floor_one(base, light, dark, seed, size=64):
    """One cave floor over four blocks by four (14:15: no squares where one of John's blocks meets another): its
    colour with soft broad patches, grit and a few pebbles."""
    n1, n2 = _noise(size, size, seed + "a", 3), _noise(size, size, seed + "b", 9)
    r = random.Random(seed)
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            k = (n1[y][x] - 0.5) * 0.34 + (n2[y][x] - 0.5) * 0.14 + (r.random() - 0.5) * 0.06
            c = mix(base, light, k * 1.6) if k > 0 else mix(base, dark, -k * 1.6)
            im.putpixel((x, y), c[:3] + (255,))
    for _ in range(size // 3):
        px, py = r.randrange(size), r.randrange(size)
        im.putpixel((px, py), mix(base, dark, 0.55)[:3] + (255,))
        im.putpixel(((px + 1) % size, py), mix(base, light, 0.35)[:3] + (255,))
    return im


def cave_mat():
    """The way out of a cave (14:15: still a mat there): the cave's sand, a worn mat of it two blocks by two, its rim a
    shade darker, two chevrons pointing out (south), and the light from outside on it."""
    im = cave_floor_one(hexc("#E2CC98"), hexc("#F4E6C2"), hexc("#C2A872"), "cm", 32)
    rim, mark = hexc("#B79A63"), hexc("#FFF6E0")
    for i in range(32):
        for (x, y) in ((i, 0), (i, 1), (i, 30), (i, 31), (0, i), (1, i), (30, i), (31, i)):
            im.putpixel((x, y), rim[:3] + (255,))
    for y0 in (8, 16):
        for k in range(7):
            for (x, y) in ((8 + k, y0 + k), (22 - k, y0 + k)):
                for dx in (0, 1):
                    for dy in (0, 1):
                        im.putpixel((x + dx, y + dy), mark[:3] + (255,))
    return im


def cave_daylight():
    """Daylight in a cave's mouth: a warm white glow, brightest in the middle and low down, softly to the edges."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 1 - (abs(x - 7.5) / 9.0) ** 2 * 0.25 - (1 - y / 15) * 0.12
            c = mix(hexc("#E8DDC4"), hexc("#FFFDF6"), max(0, min(1, k)))
            im.putpixel((x, y), c[:3] + (255,))
    return im


def sand_edges():
    """The sand's soft edge onto the cave floor next to it (14:15: no squares): as the grass's onto the dripstone,
    from lookground's tiles, in the sand's colours."""
    sand16 = Image.open(os.path.join(TEX, "cave_sand.png")).crop((0, 0, 16, 16))
    for t in range(1, 18):
        save("sand_on_floor_%d" % t, lookground.fringe(t, sand16))


def cave_textures():
    save("cave_wall_top", cave_wall_top())
    save("cave_wall_side", cave_wall_side())
    # (14:15) One floor for all John's floor blocks, so there are no squares where they meet; the sand its own, its edges soft.
    save("cave_floor", cave_floor_one(hexc("#A2733F"), hexc("#C0915C"), hexc("#7E5632"), "cf"))
    save("cave_floor_patch", Image.open(os.path.join(TEX, "cave_floor.png")))
    save("cave_sand", cave_floor_one(hexc("#D8BE86"), hexc("#ECD9AC"), hexc("#B99E68"), "cs"))
    save("cave_rock", rock_face(hexc("#8E6A44"), hexc("#B08A5E"), hexc("#5A4128"), "dr", cells=15, strata=0.04))
    save("cave_rock_top", rock_face(hexc("#7A5E3C"), hexc("#97784F"), hexc("#52402A"), "dt", cells=34))
    save("cave_mat", cave_mat())
    save("cave_daylight", cave_daylight())
    # Mt. Moon (13:56), as FireRed's: grey brown rock, a pale grey floor, its rocks grey stone.
    save("moon_rock", rock_face(hexc("#86786A"), hexc("#A59686"), hexc("#554A40"), "mr", cells=15, strata=0.04))
    save("moon_rock_top", rock_face(hexc("#9A8C7C"), hexc("#B5A796"), hexc("#6C6052"), "mt", cells=34))
    save("moon_floor", cave_floor_one(hexc("#B9B0A0"), hexc("#D1C9BA"), hexc("#958C7C"), "mf"))
    save("moon_gravel", cave_floor_one(hexc("#8F887E"), hexc("#A8A196"), hexc("#6E675E"), "mg"))
    save("moon_boulder", cave_boulder_tone(hexc("#9A958C"), "mb"))
    save("moon_steps", solid_shaded(hexc("#9A7A52"), 0.06, -0.08, 0.03, "ms"))
    save("cave_boulder", cave_boulder())
    save("cave_floor_small", Image.open(os.path.join(TEX, "cave_floor.png")).crop((16, 16, 32, 32)))
    sand_edges()
    boulder_models()


def build():
    cave_textures()
    save("white_fence", fence())
    for prefix, pal in (("roof", ROOF), ("roof_green", ROOF_GREEN), ("roof_gym", ROOF_GYM)):
        save(prefix + "_tiles", roof_tiles(pal))
        save(prefix + "_edge", roof_edge(pal))
        save(prefix + "_under", solid(pal["under"]))
    sign_textures()
    sign_models()
    mailbox_textures()
    mailbox_model()
    for part, w0 in (("l", 0), ("r", 16)):
        save("sign_short_front_" + part, short_board(32).crop((w0, 0, w0 + 16, 16)))
        save("sign_short_back_" + part, short_board(32, back=True).crop((16 - w0, 0, 32 - w0, 16)))
    save("sign_short_front_m", short_board(48).crop((16, 0, 32, 16)))
    save("sign_short_back_m", short_board(48, back=True).crop((16, 0, 32, 16)))
    save("sign_short_front_s", short_board(16))
    save("sign_short_back_s", short_board(16, back=True))
    short_sign_models()
    wood_textures()
    wood_sign_models()
    wsign_textures()
    sign_models("wsign")
    pewter_textures()
    town_textures()
    for n in BUMPS:
        save("path_" + n, path_tile(n))
    g16 = lookground.grass(16, 31)
    for t in range(1, 18):
        save("kerb_%d" % t, kerb_tile(t))
        save("kerb_grass_%d" % t, kerb_grass_tile(t, g16))
    recolours()
    # Route 22's floor: the dirt paths' own picture (64 by 64, laid over four blocks by four), and its border a
    # shade deeper, so it reads as the path's worn edge.
    p64 = lookground.path(64, 42)
    save("route_path", p64)
    edge = p64.copy()
    ep = edge.load()
    for y in range(64):
        for x in range(64):
            r, g, b, a = ep[x, y]
            ep[x, y] = (int(r * 0.84), int(g * 0.8), int(b * 0.78), a)
    save("route_path_edge", edge)
    # 0.46.0's (15:21, 15:29): tools/looks46.py.
    import looks46
    looks46.build()


# ---------- Pewter City (13:18) ----------

def _vanilla_avg(name):
    im = Image.open(os.path.join(VANILLA, "textures", "block", name + ".png")).convert("RGBA")
    px = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width)]
    return tuple(sum(p[i] for p in px) // len(px) for i in range(3)) + (255,)


def _shade(c, k):
    return mix(c, (255, 255, 255), k) if k >= 0 else mix(c, (0, 0, 0), -k)


def pave(base, seed, kind="plain"):
    """A paving stone as Let's Go's, one a block (the size of John's), in his block's own colour: lit along its top
    and left, a shadow along its bottom and right (the joins between them), the faintest grain. "inset": a square
    set in a little (the chiselled quartz); "dome": rounded, lighter in its middle (the grey in the middle of his
    pattern)."""
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = (r.random() - 0.5) * 0.035
            if kind == "dome":
                k += 0.07 * (1 - min(1.0, math.hypot(x - 7.5, y - 7.5) / 8.5))
            if kind == "inset" and (x in (3, 12) and 3 <= y <= 12 or y in (3, 12) and 3 <= x <= 12):
                k -= 0.07 if (x == 12 or y == 12) else -0.06
            if x == 0 or y == 0:
                k += 0.09
            if x == 15 or y == 15:
                k -= 0.13
            im.putpixel((x, y), _shade(base, k))
    return im


def planter():
    """The flower beds' sides (pale oak trapdoors): a white stone planter, a bright lip along the top with a soft
    shadow under it, a band a shade greyer at its foot."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = hexc("#F1F0EB")
            if y <= 1:
                c = hexc("#FFFFFF")
            elif y == 2:
                c = hexc("#D9D8D2")
            elif y >= 13:
                c = hexc("#D3D2CB") if y > 13 else hexc("#C8C7C0")
            im.putpixel((x, y), c)
    return im


def shutter(base):
    """A painted wooden panel (the houses' pale oak trapdoors): solid, a fine frame round it, a rail across the
    middle."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.03 - 0.06 * (y / 15)
            if x in (0, 15) or y in (0, 15):
                k = -0.12
            elif x in (1, 14) or y in (1, 14):
                k = 0.06
            elif y in (7, 8):
                k = -0.07 if y == 8 else 0.04
            im.putpixel((x, y), _shade(base, k))
    return im


ROOF_MUSEUM = {"hi": hexc("#C88CC2"), "base": hexc("#A9679F"), "mid": hexc("#9E5E95"), "low": hexc("#93558A"), "shadow": hexc("#723F6B"),
               "seam": hexc("#874F7F"), "edge_top": hexc("#8E4E73"), "edge_bottom": hexc("#743C5D"), "under": hexc("#62334F")}


def wood_board(width, back=False):
    """The shelf sign's board in wood (our wooden sign's look): a dark rim, a darker frame lit along its top and left,
    planks running across, and on the front two lines of carved writing."""
    im = Image.new("RGBA", (width, 16))
    for y in range(16):
        for x in range(width):
            if x == 0 or y == 0 or x == width - 1 or y == 15:
                c = WOOD["dark"]
            elif x == 1 or y == 1 or x == width - 2 or y == 14:
                c = WOOD["frame_hi"] if (x == 1 or y == 1) else WOOD["frame"]
            else:
                c = WOOD["face_hi"] if y in (2, 7, 12) else WOOD["grain"] if y in (6, 11) else WOOD["face"]
            im.putpixel((x, y), c)
    if not back:
        for row, pattern in ((4, (6, 5, 7, 4)), (9, (4, 6, 5, 3))):
            x = 4
            for w in pattern * 4:
                if x + w > width - 4:
                    break
                scribble(im, x, row, w, WOOD["dark"], row)
                x += w + 2
    return im


def wsign_textures():
    whole, back = wood_board(32), wood_board(32, back=True)
    save("wsign_front_l", whole.crop((0, 0, 16, 16)))
    save("wsign_front_r", whole.crop((16, 0, 32, 16)))
    save("wsign_back_l", back.crop((16, 0, 32, 16)))
    save("wsign_back_r", back.crop((0, 0, 16, 16)))
    save("wsign_front_m", wood_board(48).crop((16, 0, 32, 16)))
    save("wsign_back_m", wood_board(48, back=True).crop((16, 0, 32, 16)))
    save("wsign_front_s", wood_board(16))
    save("wsign_back_s", wood_board(16, back=True))
    for n, rows in (("wsign_edge_h", True), ("wsign_edge_v", False)):
        im = Image.new("RGBA", (16, 16), WOOD["frame"])
        for i in range(16):
            for k in (5, 10):
                im.putpixel((i, k) if rows else (k, i), WOOD["dark"])
        save(n, im)
    post = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            post.putpixel((x, y), WOOD["post_hi"] if x in (6, 7) else mix(WOOD["post"], WOOD["dark"], y / 30))
    save("wsign_stand", post)
    foot = Image.new("RGBA", (16, 16), WOOD["dark"])
    for x in range(16):
        foot.putpixel((x, 13), WOOD["frame"])
    save("wsign_foot", foot)


def pewter_textures():
    # The houses (Pewter's two and Route 2's trade house): the deepslate tile roof metal, the pale oak walls solid,
    # the pale oak stairs at their windows the white frames, the trapdoors painted panels, the mud brick chimney solid.
    save("pewter_roof", metal(hexc("#59606C")))
    save("pewter_wall", solid_shaded(hexc("#ECE3DD"), 0.04, -0.05))
    save("pewter_shutter", shutter(hexc("#E6DCD6")))
    save("pewter_chimney", solid_shaded(hexc("#8E6C53"), 0.05, -0.08, 0.02, "pc"))
    save("planter_white", planter())
    # The paving: each of John's blocks a paving stone of its own colour, one a block, as he laid them.
    for name, block, kind in (("pave_white", "white_concrete", "plain"), ("pave_quartz", "quartz_block_bottom", "plain"),
                              ("pave_terracotta", "white_terracotta", "plain"), ("pave_chiseled", "chiseled_quartz_block_top", "inset"),
                              ("pave_chiseled_side", "chiseled_quartz_block", "plain"), ("pave_gray", "light_gray_concrete", "dome")):
        save(name, pave(_vanilla_avg(block), name, kind))
    # The Museum (as FireRed's): its purpur roof FireRed's purple tiles (the roof look), the purpur slabs the same
    # purple flat, the crimson slabs the darker crimson-purple bands, the white terracotta walls cream.
    for suffix, im in (("_tiles", roof_tiles(ROOF_MUSEUM)), ("_edge", roof_edge(ROOF_MUSEUM)), ("_under", solid(ROOF_MUSEUM["under"]))):
        save("roof_museum" + suffix, im)
    save("museum_roof_flat", flat(hexc("#A56A9F")))
    save("museum_roof_dark", flat(hexc("#7E3E62")))
    save("museum_wall", solid_shaded(hexc("#EEDFC3"), 0.04, -0.06))


# ---------- 14:15: the towns' second pass ----------

def solar_top():
    """A lamp's sun catcher (the daylight detector on top, 14:15: still solar): a solar panel, dark blue cells in a
    silver frame, a fine grid between them, a sheen across."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = hexc("#C3C8CE") if (x == 0 or y == 0) else hexc("#8E959C")
            elif x in (5, 10) or y in (5, 10):
                c = hexc("#9AA3AD")
            else:
                k = 0.18 if 3 <= (x + y) % 11 <= 4 else 0
                c = mix(hexc("#1D3A6B"), hexc("#5D86C2"), k + 0.08 * (1 - y / 15))
            im.putpixel((x, y), c[:3] + (255,))
    return im


def solar_side():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = hexc("#A9B0B7") if y <= 1 else mix(hexc("#5B6168"), hexc("#3E4349"), y / 15)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def fence_post(top=False):
    """The fence's posts (John's spruce logs between fence panels, 14:15: a fence, not trees): solid painted wood, a
    fine grain down it, a lighter edge; its top end a plain square with a bevel."""
    base = hexc("#8A5E3A")
    r = random.Random("fp" + str(top))
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if top:
                k = 0.10 if (x in (0, 1) or y in (0, 1)) else -0.14 if (x in (14, 15) or y in (14, 15)) else 0.02
            else:
                k = 0.07 if x in (0, 1) else -0.10 if x in (14, 15) else 0.0
                if x in (5, 10) and r.random() < 0.85:
                    k -= 0.06
                k += 0.04 - 0.08 * (y / 15)
            k += (r.random() - 0.5) * 0.025
            c = mix(base, (255, 255, 255), k) if k > 0 else mix(base, (0, 0, 0), -k)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def fence_rail():
    """The fence's rails (spruce fence: its planks' texture): the same painted wood, a line along each board."""
    base = hexc("#93653F")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.08 if y % 4 == 0 else -0.08 if y % 4 == 3 else 0.0
            c = mix(base, (255, 255, 255), k) if k > 0 else mix(base, (0, 0, 0), -k)
            im.putpixel((x, y), c[:3] + (255,))
    return im


def recolour_flat(src, base, keep=0.35):
    """A vanilla texture (a shelf's: its shape kept) in one solid colour: its own light and dark kept only a little
    (keep), so it reads as painted, transparent pixels left as they are."""
    im = Image.open(src).convert("RGBA")
    px = im.load()
    lum = [0.299 * px[x, y][0] + 0.587 * px[x, y][1] + 0.114 * px[x, y][2] for y in range(im.height) for x in range(im.width) if px[x, y][3] > 0]
    mean = sum(lum) / max(1, len(lum))
    for y in range(im.height):
        for x in range(im.width):
            r_, g_, b_, a = px[x, y]
            if a == 0: continue
            k = (0.299 * r_ + 0.587 * g_ + 0.114 * b_ - mean) / 255.0 * keep * 2
            c = mix(base, (255, 255, 255), k) if k > 0 else mix(base, (0, 0, 0), -k)
            px[x, y] = c[:3] + (a,)
    return im


def garden_light():
    """John's lanterns on the fences and over the paths (14:18), as a garden light: a dark metal frame round a warm
    white glass, a metal cap and ring (laid out as the lantern's own picture, one frame)."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    metal, metal_hi, glass, glass_hi = hexc("#33383E"), hexc("#5A6169"), hexc("#FFE9B8"), hexc("#FFF8E6")
    for y in range(16):
        for x in range(16):
            c = None
            if 0 <= x <= 5 and 2 <= y <= 8:      # the body's sides: frame round a glass
                c = metal if x in (0, 5) or y in (2, 8) else (glass_hi if x in (1, 2) and y in (3, 4) else glass)
            elif 0 <= x <= 5 and 9 <= y <= 14:   # its top and bottom
                c = metal_hi if x in (0, 5) or y in (9, 14) else metal
            elif 1 <= x <= 4 and 0 <= y <= 1:    # the cap's sides
                c = metal_hi if y == 0 else metal
            elif 11 <= x <= 13 and 1 <= y <= 2:  # the ring
                c = metal_hi
            if c is not None:
                im.putpixel((x, y), c[:3] + (255,))
    return im


def flower_bush():
    """The Museum's six little flowering trees (14:15: still flowery): round leaves in two greens, light on their tops,
    pink and white blossoms among them, a few gaps (as leaves are)."""
    r = random.Random("fb")
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    g1, g2, g3 = hexc("#4E9A3E"), hexc("#6DB850"), hexc("#356E2C")
    for y in range(16):
        for x in range(16):
            if r.random() < 0.06: continue
            k = r.random()
            c = g2 if k < 0.35 else g1 if k < 0.8 else g3
            im.putpixel((x, y), c[:3] + (255,))
    for (cx, cy, col) in ((3, 3, "#F6A8C8"), (11, 2, "#FFFFFF"), (7, 7, "#F48DB8"), (13, 9, "#F6A8C8"), (2, 11, "#FFFFFF"), (9, 13, "#F48DB8"), (5, 14, "#F6A8C8")):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            x, y = (cx + dx) % 16, (cy + dy) % 16
            im.putpixel((x, y), (hexc(col) if (dx, dy) != (0, 0) else hexc("#FFE27A"))[:3] + (255,))
    return im


def town_textures():
    v = lambda n: os.path.join(VANILLA, "textures", "block", n + ".png")
    save("lamp_solar", solar_top())
    save("lamp_solar_side", solar_side())
    # Pewter's own lamps (14:15): grey metal, not black; their globe frosted warm white.
    GREY = [hexc(c) for c in ("#4A5058", "#5E656E", "#737B85", "#8C949E", "#A8B0B9")]
    for n in ("chiseled_tuff_bricks", "chiseled_tuff_bricks_top"):
        save("plamp_" + n, lookground.recolour(v(n), GREY))
    save("plamp_post", solid_shaded(hexc("#7A828C"), 0.06, -0.07))
    save("plamp_cap", metal(hexc("#8C949E"), seam=False))
    save("plamp_globe", solid_shaded(hexc("#FFF3D6"), 0.04, -0.04))
    save("fence_post", fence_post())
    save("fence_post_top", fence_post(top=True))
    save("fence_rail", fence_rail())
    save("garden_light", garden_light())
    save("flower_bush", flower_bush())
    # Viridian's green houses: the chimney clean painted brick, the warped shelf on the ridge and the top of the chimney
    # a solid green, the birch shelves round the foot one solid wood (their shelf kept).
    save("chimney_brick", bricks(hexc("#B65A3E"), hexc("#94452F"), shade=0.06))
    save("green_shelf", recolour_flat(v("warped_shelf"), hexc("#5EA14C")))
    save("green_solid", solid_shaded(hexc("#5EA14C"), 0.05, -0.07))
    save("green_solid_top", solid_shaded(hexc("#6AAE55"), 0.0, 0.0))
    save("wood_shelf", recolour_flat(v("birch_shelf"), hexc("#B98A57")))
    # The Centers' and Marts' bottoms (diorite) solid, each its own; the Gyms' and houses' trim as Pallet's.
    save("center_base", solid_shaded(hexc("#E9E4DE"), 0.04, -0.06))
    save("mart_base", solid_shaded(hexc("#D9DEE6"), 0.04, -0.06))
    # Prof. Oak's lab roof (14:15): light grey metal panels, a darker rim.
    save("lab_roof", metal(hexc("#A2AAB3")))
    save("lab_roof_rim", metal(hexc("#6E7680"), seam=False))
    # Glowstone (John's hidden lights, under carpets inside, 14:15): as the white carpet it's under, so you can't see it.
    save("glow_hidden", solid_shaded(hexc("#E8EAEA"), 0.02, -0.03, 0.01, "gh"))
    # The Museum's slabs (stone, diorite): solid light grey trim.
    save("museum_trim", solid_shaded(hexc("#C9CCD0"), 0.04, -0.06))


def preview(out, scale=8):
    """The pieces side by side, enlarged (to look at offline)."""
    names = ["white_fence", "roof_tiles", "roof_edge", "sign_front_l", "sign_front_r", "sign_back_l", "sign_back_r", "sign_stand",
             "mailbox_front", "mailbox_side", "mailbox_top", "mailbox_post"]
    im = Image.new("RGBA", (len(names) * 17 * scale, 16 * scale), (60, 120, 70, 255))
    for i, n in enumerate(names):
        t = Image.open(os.path.join(TEX, n + ".png")).resize((16 * scale, 16 * scale), Image.NEAREST)
        im.paste(t, (i * 17 * scale, 0))
    im.save(out)


if __name__ == "__main__":
    build()
    import sys
    if len(sys.argv) > 1:
        preview(sys.argv[1])
