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
                for xx in range(x, x + w):
                    for yy in (row, row + 1):
                        im.putpixel((xx, yy), SIGN["text"])
                x += w + 2
    return im


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


def sign_models():
    """The sign's parts, for each way it can face, as models the mod puts where the shelves are: the board (the top
    row of shelves) and the stand (the row under it), each as its left end (as you look at its front), right end,
    a middle block or a sign one block wide. Facing north: you look south at it, so its left is east (x = 16)."""
    turns_of = {"north": 0, "east": 1, "south": 2, "west": 3}
    for facing, turns in turns_of.items():
        for part in ("l", "r", "m", "s"):
            outer_left = part in ("l", "s")    # the end at x = 16 (facing north) is the sign's end
            outer_right = part in ("r", "s")   # the end at x = 0
            tex = lambda n: "retromon:block/area/" + n
            # The board: the whole block across, 6 thick.
            faces = {"north": tex("sign_front_" + part), "south": tex("sign_back_" + part),
                     "up": tex("sign_edge_h"), "down": tex("sign_edge_h")}
            if outer_left:
                faces["east"] = tex("sign_edge_v")
            if outer_right:
                faces["west"] = tex("sign_edge_v")
            board_el = [element((0, 0, 5, 16, 16, 11), faces, turns)]
            # The stand: set in from the sign's ends, a little thinner than the board, on a foot.
            sx0 = 2 if outer_right else 0
            sx1 = 14 if outer_left else 16
            sfaces = {"north": tex("sign_stand"), "south": tex("sign_stand")}
            if outer_left:
                sfaces["east"] = tex("sign_stand")
            if outer_right:
                sfaces["west"] = tex("sign_stand")
            fx0 = 1 if outer_right else 0
            fx1 = 15 if outer_left else 16
            ffaces = {"north": tex("sign_foot"), "south": tex("sign_foot"), "up": tex("sign_foot"), "down": tex("sign_foot")}
            if outer_left:
                ffaces["east"] = tex("sign_foot")
            if outer_right:
                ffaces["west"] = tex("sign_foot")
            stand_el = [element((sx0, 2, 6, sx1, 16, 10), sfaces, turns), element((fx0, 0, 5, fx1, 2, 11), ffaces, turns)]
            for row, els in (("board", board_el), ("stand", stand_el)):
                write_json(os.path.join(MODELS, "sign_%s_%s_%s.json" % (row, part, facing)), slotted({
                    "parent": "minecraft:block/block", "ambientocclusion": False,
                    "textures": {"particle": tex("sign_edge_h")}, "elements": els}))


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
            for xx in range(x, x + w):
                for yy in (4, 5):
                    im.putpixel((xx, yy), SIGN["text"])
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
                for xx in range(x, min(13, x + w)):
                    im.putpixel((xx, row), WOOD["dark"])
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


def build():
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
