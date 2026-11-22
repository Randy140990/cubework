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
import os

from PIL import Image

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


def roof_tiles():
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


def roof_edge():
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


def build():
    save("white_fence", fence())
    save("roof_tiles", roof_tiles())
    save("roof_edge", roof_edge())
    save("roof_under", solid(ROOF["under"]))
    sign_textures()
    sign_models()
    mailbox_textures()
    mailbox_model()


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
