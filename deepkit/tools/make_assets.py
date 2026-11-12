"""Makes Deepkit's textures, models, block states, recipes, tags and names (run from deepkit/)."""
import json, os, math
from PIL import Image, ImageDraw

R = "src/main/resources"
A = R + "/assets/deepkit"
D = R + "/data"

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=1)

def png(path, im):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)

def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + ((c[3],) if len(c) == 4 else (255,))

def pixels(rows, pal):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                im.putpixel((x, y), pal[ch])
    return im

FLARES = [("flare", 0xFF3A2A, "red_dye", "Flare"), ("green_flare", 0x46E35A, "green_dye", "Green Flare"),
          ("blue_flare", 0x3D8BFF, "blue_dye", "Blue Flare"), ("white_flare", 0xF6F2E8, "white_dye", "White Flare")]
PAINTS = [("white", 0xF4F4EE, "white_dye", "White"), ("red", 0xFF4A3D, "red_dye", "Red"), ("orange", 0xFF9A2E, "orange_dye", "Orange"),
          ("yellow", 0xFFE14A, "yellow_dye", "Yellow"), ("lime", 0x8CF04A, "lime_dye", "Lime"), ("cyan", 0x3EE6E6, "cyan_dye", "Cyan"),
          ("blue", 0x4F86FF, "blue_dye", "Blue"), ("pink", 0xFF7BC0, "pink_dye", "Pink")]
MARKS = ["arrow", "cross", "dot", "circle", "exclamation", "question", "tick", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]

COPPER, COPPER_L, COPPER_D = (196, 108, 72, 255), (231, 148, 104, 255), (128, 66, 44, 255)
LENS, LENS_L = (255, 226, 120, 255), (255, 250, 210, 255)
LEATHER, LEATHER_D = (122, 78, 46, 255), (84, 52, 30, 255)
IRON, IRON_L, IRON_D = (170, 176, 186, 255), (214, 220, 228, 255), (96, 102, 112, 255)
ROPE, ROPE_L, ROPE_D = (178, 146, 98, 255), (214, 186, 132, 255), (118, 92, 58, 255)

# ---------- item textures ----------
def items():
    t = "%s/textures/item/" % A
    png(t + "hand_torch.png", pixels([
        "................",
        "............ll..",
        "...........lLLl.",
        "..........lLLLl.",
        ".........CcLLl..",
        "........CccCl...",
        ".......CccC.....",
        "......CccC......",
        ".....CccC.......",
        "....CccC........",
        "...CccC.........",
        "..dccC..........",
        "..ddC...........",
        "...d............",
        "................",
        "................"], {"C": COPPER_L, "c": COPPER, "d": COPPER_D, "l": LENS, "L": LENS_L}))
    png(t + "headlamp.png", pixels([
        "................",
        "................",
        "................",
        "......CCCC......",
        ".....CllllC.....",
        "....bClLLlCb....",
        "..bbbClLLlCbbb..",
        ".bBBBCllllCBBBb.",
        ".bB..dCCCCd..Bb.",
        ".bB..........Bb.",
        "..bB........Bb..",
        "...bBB....BBb...",
        ".....bBBBBb.....",
        "................",
        "................",
        "................"], {"C": COPPER, "d": COPPER_D, "l": LENS, "L": LENS_L, "b": LEATHER_D, "B": LEATHER}))
    png(t + "chest_lamp.png", pixels([
        "................",
        "..bB........Bb..",
        "..bB........Bb..",
        "..bB........Bb..",
        "..bBB......BBb..",
        "...bBBCCCCBBb...",
        "....BClllCB.....",
        "....bClLlCb.....",
        "....BClllCB.....",
        "...bBdCCCdBBb...",
        "..bBB......BBb..",
        "..bB........Bb..",
        "..bB........Bb..",
        "................",
        "................",
        "................"], {"C": COPPER, "d": COPPER_D, "l": LENS, "L": LENS_L, "b": LEATHER_D, "B": LEATHER}))
    for name, col, _, _ in FLARES:
        c = rgb(col) + (255,)
        rows = [
            "................",
            "................",
            "..........kk....",
            ".........kKKk...",
            "........fFFk....",
            ".......fFFf.....",
            "......fFFf......",
            ".....fFFf.......",
            "....fFFf........",
            "...fFFf.........",
            "...wFf..........",
            "..wwf...........",
            "...w............",
            "................",
            "................",
            "................"]
        pal = {"f": shade(c, 0.7), "F": c, "k": (60, 56, 52, 255), "K": (96, 90, 84, 255), "w": (230, 222, 205, 255)}
        png(t + name + ".png", pixels(rows, pal))
        lit = [
            "..........y.Y...",
            ".......y.YyYy...",
            "........YoOoY.y.",
            ".......yoOWOoY..",
            "........fOOo....",
            ".......fFFf.....",
            "......fFFf......",
            ".....fFFf.......",
            "....fFFf........",
            "...fFFf.........",
            "...wFf..........",
            "..wwf...........",
            "...w............",
            "................",
            "................",
            "................"]
        pal2 = dict(pal)
        pal2.update({"y": (255, 220, 120, 255), "Y": (255, 250, 200, 255), "o": shade(c, 1.0), "O": (255, 236, 170, 255), "W": (255, 255, 255, 255)})
        png(t + "lit_" + name + ".png", pixels(lit, pal2))
    for name, col, _, _ in PAINTS:
        c = rgb(col) + (255,)
        rows = [
            "................",
            "......kk........",
            ".....kKKk.......",
            "......nn........",
            ".....mmmm.......",
            "....mPPPPm......",
            "....mPpPPm......",
            "....mPpPPm......",
            "....mPpPPm......",
            "....mPPPPm......",
            "....mPPPPm......",
            "....mPPPPm......",
            "....mmmmmm......",
            ".....mmmm.......",
            "................",
            "................"]
        pal = {"k": (60, 60, 66, 255), "K": (110, 110, 118, 255), "n": (150, 150, 158, 255), "m": IRON_D, "P": c, "p": shade(c, 1.35)}
        png(t + name + "_glow_paint.png", pixels(rows, pal))
    png(t + "rope_coil.png", pixels([
        "................",
        "....rrRRRRrr....",
        "..rRRrrrrrrRRr..",
        ".rRrr......rrRr.",
        ".Rr..rRRRRr..rR.",
        "rRr.rRrrrrRr.rRr",
        "rR.rRr....rRr.Rr",
        "rR.rR..dd..Rr.Rr",
        "rR.rR..dd..Rr.Rr",
        "rR.rRr....rRr.Rr",
        "rRr.rRrrrrRr.rRr",
        ".Rr..rRRRRr..rR.",
        ".rRrr......rrRr.",
        "..rRRrrrrrrRRr..",
        "....rrRRRRrr....",
        "................"], {"r": ROPE_D, "R": ROPE, "d": ROPE_L}))
    png(t + "grappling_hook.png", pixels([
        "................",
        ".I.....I.....I..",
        ".II....I....II..",
        "..II...I...II...",
        "...IIdIIIdII....",
        "....dIIIIId.....",
        ".....dIIId......",
        "......dId.......",
        ".......R........",
        "......R.........",
        ".....R..........",
        "....R...........",
        "...rR...........",
        "..rR............",
        "..r.............",
        "................"], {"I": IRON_L, "d": IRON_D, "R": ROPE, "r": ROPE_D}))

# ---------- block textures ----------
FONT = {
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
    "exclamation": ["00100", "00100", "00100", "00100", "00100", "00000", "00100"],
    "question": ["01110", "10001", "00001", "00010", "00100", "00000", "00100"],
}

def glyph(name):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    W = (255, 255, 255, 255)
    if name in FONT:
        rows = FONT[name]
        s = 6
        ox, oy = (64 - 5 * s) // 2, (64 - 7 * s) // 2
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == "1":
                    d.rectangle([ox + x * s, oy + y * s, ox + x * s + s - 1, oy + y * s + s - 1], fill=W)
    elif name == "arrow":
        d.polygon([(32, 6), (54, 30), (40, 30), (40, 58), (24, 58), (24, 30), (10, 30)], fill=W)
    elif name == "cross":
        d.line([(12, 12), (52, 52)], fill=W, width=10)
        d.line([(52, 12), (12, 52)], fill=W, width=10)
    elif name == "dot":
        d.ellipse([20, 20, 44, 44], fill=W)
    elif name == "circle":
        d.ellipse([10, 10, 54, 54], outline=W, width=8)
    elif name == "tick":
        d.line([(10, 34), (26, 50), (54, 14)], fill=W, width=10, joint="curve")
    # Paint is a little uneven at the edges.
    im = im.resize((16, 16), Image.LANCZOS)
    px = im.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            a = 0 if a < 70 else 255
            v = 255 if (x * 7 + y * 13) % 5 else 225
            px[x, y] = (v, v, v, a)
    return im

def blocks():
    t = "%s/textures/block/" % A
    png(t + "beam_light.png", Image.new("RGBA", (16, 16), (0, 0, 0, 0)))
    for m in MARKS:
        png(t + "mark/" + m + ".png", glyph(m))
    # Rope: a twisted strand.
    im = Image.new("RGBA", (16, 16), ROPE)
    for y in range(16):
        for x in range(16):
            k = (x + y) % 4
            im.putpixel((x, y), ROPE_L if k == 0 else ROPE_D if k == 2 else ROPE)
    png(t + "rope.png", im)
    im = Image.new("RGBA", (16, 16), IRON)
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), IRON_L if (x + 2 * y) % 7 == 0 else IRON_D if (2 * x + y) % 9 == 0 else IRON)
    png(t + "hook_metal.png", im)

# ---------- models and block states ----------
Y = {"north": 0, "east": 90, "south": 180, "west": 270}

def box(f, t, tex, faces=("north", "south", "east", "west", "up", "down")):
    out = {"from": f, "to": t, "faces": {}}
    for face in faces:
        out["faces"][face] = {"texture": tex}
    return out

def models():
    b = "%s/models/block/" % A
    write(b + "beam_light.json", {"textures": {"particle": "deepkit:block/beam_light"}})
    write("%s/blockstates/beam_light.json" % A, {"variants": {"": {"model": "deepkit:block/beam_light"}}})
    strand = box([7, 0, 2], [9, 16, 4], "#rope")
    write(b + "rope.json", {"ambientocclusion": False, "textures": {"rope": "deepkit:block/rope", "particle": "deepkit:block/rope"},
                            "elements": [strand]})
    write(b + "rope_knot.json", {"ambientocclusion": False, "textures": {"rope": "deepkit:block/rope", "particle": "deepkit:block/rope"},
                                 "elements": [box([7, 0, 2], [9, 13, 4], "#rope"), box([6.5, 13, 1], [9.5, 16.5, 4.5], "#rope"),
                                              box([6.5, 16, -3], [9.5, 17, 1], "#rope")]})
    write(b + "rope_hook.json", {"ambientocclusion": False, "textures": {"rope": "deepkit:block/rope", "metal": "deepkit:block/hook_metal",
                                                                          "particle": "deepkit:block/rope"},
                                 "elements": [box([7, 0, 2], [9, 14, 4], "#rope"), box([6, 14, 1.5], [10, 17.5, 4], "#metal"),
                                              box([6, 16.5, -3.5], [10, 17.5, 1.5], "#metal"), box([6, 14, -3.5], [10, 16.5, -2.5], "#metal"),
                                              box([4.5, 16.5, -3], [5.5, 17.5, 1], "#metal"), box([10.5, 16.5, -3], [11.5, 17.5, 1], "#metal")]})
    variants = {}
    for f in Y:
        for knot in ("false", "true"):
            for hook in ("false", "true"):
                model = "rope_hook" if hook == "true" else "rope_knot" if knot == "true" else "rope"
                v = {"model": "deepkit:block/" + model}
                if Y[f]:
                    v["y"] = Y[f]
                variants["facing=%s,hook=%s,knot=%s" % (f, hook, knot)] = v
    write("%s/blockstates/rope.json" % A, {"variants": variants})
    # Marks: one model a mark and turn, painted on the face of the block to the north.
    for i, m in enumerate(MARKS):
        for r in range(4):
            write(b + "mark_%s_%d.json" % (m, r), {"ambientocclusion": False,
                  "textures": {"mark": "deepkit:block/mark/" + m, "particle": "deepkit:block/mark/" + m},
                  "elements": [{"from": [0, 0, 0.1], "to": [16, 16, 0.1], "shade": False, "light_emission": 12,
                                "faces": {"south": {"uv": [0, 0, 16, 16], "texture": "#mark", "tintindex": 0, "rotation": r * 90}}}]})
    for name, _, _, _ in PAINTS:
        variants = {}
        for f in ("north", "east", "south", "west", "down", "up"):
            for r in range(4):
                for i, m in enumerate(MARKS):
                    if f in Y:
                        v = {"model": "deepkit:block/mark_%s_%d" % (m, r)}
                        if Y[f]:
                            v["y"] = Y[f]
                    elif f == "down":
                        v = {"model": "deepkit:block/mark_%s_0" % m, "x": 90}
                        if r:
                            v["y"] = r * 90
                    else:
                        v = {"model": "deepkit:block/mark_%s_0" % m, "x": 270, "y": (r * 90 + 180) % 360}
                    variants["facing=%s,rotation=%d,symbol=%d" % (f, r, i)] = v
        write("%s/blockstates/%s_glow_mark.json" % (A, name), {"variants": variants})
    # Items.
    def item(name, handheld=False):
        write("%s/models/item/%s.json" % (A, name), {"parent": "minecraft:item/" + ("handheld" if handheld else "generated"),
                                                      "textures": {"layer0": "deepkit:item/" + name}})
        write("%s/items/%s.json" % (A, name), {"model": {"type": "minecraft:model", "model": "deepkit:item/" + name}})
    item("hand_torch", True)
    item("headlamp")
    item("chest_lamp")
    for name, _, _, _ in FLARES:
        item(name, True)
        item("lit_" + name, True)
    for name, _, _, _ in PAINTS:
        item(name + "_glow_paint")
    item("rope_coil")
    item("grappling_hook", True)

# ---------- recipes, tags, names ----------
def data():
    r = "%s/deepkit/recipe/" % D
    def shapeless(name, ingredients, result, count=1, category="equipment"):
        write(r + name + ".json", {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                                   "result": {"id": "deepkit:" + result, "count": count}})
    write(r + "hand_torch.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["CPC", "RGC"],
                                  "key": {"C": "minecraft:copper_ingot", "P": "minecraft:glass_pane", "R": "minecraft:redstone",
                                          "G": "minecraft:glowstone_dust"}, "result": {"id": "deepkit:hand_torch", "count": 1}})
    shapeless("headlamp", ["deepkit:hand_torch", "minecraft:leather", "minecraft:string"], "headlamp")
    shapeless("chest_lamp", ["deepkit:hand_torch", "minecraft:leather", "minecraft:iron_nugget"], "chest_lamp")
    write(r + "lamp.json", {"type": "deepkit:lamp", "category": "equipment"})
    for name, _, dye, _ in FLARES:
        shapeless(name, ["minecraft:paper", "minecraft:gunpowder", "minecraft:redstone", "minecraft:" + dye], name, 4)
    for name, _, dye, _ in PAINTS:
        shapeless(name + "_glow_paint", ["minecraft:glass_bottle", "minecraft:glow_ink_sac", "minecraft:" + dye], name + "_glow_paint")
    write(r + "rope_coil.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["SSS", "S/S", "SSS"],
                                 "key": {"S": "minecraft:string", "/": "minecraft:stick"}, "result": {"id": "deepkit:rope_coil", "count": 1}})
    shapeless("grappling_hook", ["minecraft:crossbow", "deepkit:rope_coil", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"],
              "grappling_hook")
    write("%s/minecraft/tags/block/climbable.json" % D, {"replace": False, "values": ["deepkit:rope"]})
    names = {"itemGroup.deepkit": "Deepkit", "key.deepkit.lamps": "Lamps on/off", "key.category.deepkit.deepkit": "Deepkit",
             "item.deepkit.hand_torch": "Hand Torch", "item.deepkit.headlamp": "Headlamp", "item.deepkit.chest_lamp": "Chest Lamp",
             "item.deepkit.rope_coil": "Rope Coil", "item.deepkit.grappling_hook": "Grappling Hook", "block.deepkit.rope": "Rope",
             "block.deepkit.beam_light": "Lamp Light", "entity.deepkit.flare": "Flare", "entity.deepkit.grappling_hook": "Grappling Hook"}
    for name, _, _, title in FLARES:
        names["item.deepkit." + name] = title
        names["item.deepkit.lit_" + name] = "Lit " + title
    for name, _, _, title in PAINTS:
        names["item.deepkit.%s_glow_paint" % name] = "%s Glow Paint" % title
        names["block.deepkit.%s_glow_mark" % name] = "%s Glow Mark" % title
    write("%s/lang/en_us.json" % A, names)
    write("%s/lang/en_gb.json" % A, names)

# ---------- worn lamps (drawn like armour) ----------
def equipment():
    """The headlamp and chest lamp on a player: 64 x 32 armour-layer textures (the game's helmet and
    chestplate layout), a strap round the head with the lamp on the forehead, and a harness with the lamp
    on the chest."""
    STRAP, STRAP_D, BUCKLE = LEATHER, LEATHER_D, IRON
    def lamp(im, x, y):
        # A 4 x 4 copper housing with a 2 x 2 lens.
        for i in range(4):
            for j in range(4):
                edge = i in (0, 3) or j in (0, 3)
                im.putpixel((x + i, y + j), COPPER_D if edge else COPPER)
        im.putpixel((x + 1, y + 1), LENS_L); im.putpixel((x + 2, y + 1), LENS)
        im.putpixel((x + 1, y + 2), LENS); im.putpixel((x + 2, y + 2), LENS)
        im.putpixel((x + 1, y), COPPER_L); im.putpixel((x + 2, y), COPPER_L)

    head = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # Head: right side x0-7, front x8-15, left side x16-23, back x24-31 (rows 8-15); top x8-15 (rows 0-7).
    for x in range(0, 32):
        head.putpixel((x, 9), STRAP)
        head.putpixel((x, 10), STRAP_D)
    for y in range(0, 8):
        head.putpixel((11, y), STRAP)
        head.putpixel((12, y), STRAP_D)
    head.putpixel((27, 9), BUCKLE); head.putpixel((28, 9), BUCKLE); head.putpixel((27, 10), BUCKLE); head.putpixel((28, 10), BUCKLE)
    lamp(head, 10, 8)
    png(A + "/textures/entity/equipment/humanoid/headlamp.png", head)
    write(A + "/equipment/headlamp.json", {"layers": {"humanoid": [{"texture": "deepkit:headlamp"}]}})

    chest = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # Body: top x20-27 (rows 16-19), right side x16-19, front x20-27, left side x28-31, back x32-39 (rows 20-31).
    for y in range(16, 32):
        for x in (21, 26):
            chest.putpixel((x, y), STRAP)
        if y >= 20:
            for x in (33, 38):
                chest.putpixel((x, y), STRAP)
    for x in range(16, 40):
        chest.putpixel((x, 27), STRAP_D)
    chest.putpixel((23, 27), BUCKLE); chest.putpixel((24, 27), BUCKLE)
    lamp(chest, 22, 21)
    png(A + "/textures/entity/equipment/humanoid/chest_lamp.png", chest)
    write(A + "/equipment/chest_lamp.json", {"layers": {"humanoid": [{"texture": "deepkit:chest_lamp"}]}})

items()
equipment()
blocks()
models()
data()
print("done")
