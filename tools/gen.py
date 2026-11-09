#!/usr/bin/env python3
"""
Writes every Cubework resource: blockstates, block and item models, item definitions, loot tables,
recipes, tags, names, and the atlas lists for the coloured doors. Run from the repo root after changing
any list (they must match Material.java, Shape.java and Extras.java):  python3 tools/gen.py

Models point straight at the vanilla textures (minecraft:block/red_wool and so on), so a texture pack
that changes a block changes all of its shapes too. The coloured doors' textures are made in the game
from the door textures in use (DoorColoursSource).
"""
import json
import os
import shutil

HERE = os.path.dirname(__file__)
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "cubework")
DATA = os.path.join(ROOT, "data")
TEMPLATES = os.path.join(HERE, "templates")
NS = "cubework"

COLOURS = ["white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan",
           "light_blue", "blue", "purple", "magenta", "pink"]

NATURE = ["grass_block", "podzol", "mycelium", "dirt_path", "dirt", "coarse_dirt", "rooted_dirt", "farmland", "mud", "clay",
          "gravel", "sand", "sandstone", "red_sand", "red_sandstone", "ice", "packed_ice", "blue_ice", "snow_block", "moss_block",
          "pale_moss_block", "stone", "deepslate", "granite", "diorite", "andesite", "calcite", "tuff", "dripstone_block", "prismarine",
          "magma_block", "obsidian", "crying_obsidian", "netherrack", "crimson_nylium", "warped_nylium", "soul_sand", "soul_soil",
          "bone_block", "blackstone", "basalt", "smooth_basalt", "end_stone", "coal_ore", "deepslate_coal_ore", "iron_ore",
          "deepslate_iron_ore", "copper_ore", "deepslate_copper_ore", "gold_ore", "deepslate_gold_ore", "redstone_ore",
          "deepslate_redstone_ore", "emerald_ore", "deepslate_emerald_ore", "lapis_ore", "deepslate_lapis_ore", "diamond_ore",
          "deepslate_diamond_ore", "nether_gold_ore", "nether_quartz_ore", "ancient_debris", "raw_iron_block", "raw_copper_block",
          "raw_gold_block", "glowstone", "amethyst_block", "budding_amethyst"]

# (id, kind), in the same order as Material.java
MATERIALS = [(c + "_wool", "wool") for c in COLOURS] + [("terracotta", "terracotta")] + \
            [(c + "_terracotta", "terracotta") for c in COLOURS] + [(c + "_concrete", "concrete") for c in COLOURS] + \
            [(c + "_concrete_powder", "powder") for c in COLOURS] + [(c + "_glazed_terracotta", "glazed") for c in COLOURS] + \
            [("glass", "glass"), ("tinted_glass", "glass")] + [(c + "_stained_glass", "glass") for c in COLOURS] + \
            [(n, "nature") for n in NATURE]

# top, side, bottom, overlay (as Material.java)
TEXTURES = {
    "grass_block": ("grass_block_top", "grass_block_side", "dirt", "grass_block_side_overlay"),
    "podzol": ("podzol_top", "podzol_side", "dirt", None),
    "mycelium": ("mycelium_top", "mycelium_side", "dirt", None),
    "dirt_path": ("dirt_path_top", "dirt_path_side", "dirt", None),
    "farmland": ("farmland", "dirt", "dirt", None),
    "sandstone": ("sandstone_top", "sandstone", "sandstone_bottom", None),
    "red_sandstone": ("red_sandstone_top", "red_sandstone", "red_sandstone_bottom", None),
    "crimson_nylium": ("crimson_nylium", "crimson_nylium_side", "netherrack", None),
    "warped_nylium": ("warped_nylium", "warped_nylium_side", "netherrack", None),
    "basalt": ("basalt_top", "basalt_side", "basalt_top", None),
    "bone_block": ("bone_block_top", "bone_block_side", "bone_block_top", None),
    "deepslate": ("deepslate_top", "deepslate", "deepslate_top", None),
    "blackstone": ("blackstone_top", "blackstone", "blackstone_top", None),
    "ancient_debris": ("ancient_debris_top", "ancient_debris_side", "ancient_debris_top", None),
    "snow_block": ("snow", "snow", "snow", None),
    "magma_block": ("magma", "magma", "magma", None),
}

# (suffix, English name, how many one block makes in the stonecutter), in the order of Shape.java
SHAPES = [("stairs", "Stairs", 1), ("slab", "Slab", 2), ("vertical_slab", "Vertical Slab", 2), ("slope", "Slope", 2),
          ("layer", "Layer", 8), ("vertical_layer", "Vertical Layer", 8), ("carpet", "Carpet", 4), ("panel", "Panel", 4),
          ("post", "Post", 4), ("wall", "Wall", 1), ("fence", "Fence", 1), ("fence_gate", "Fence Gate", 1),
          ("trapdoor", "Trapdoor", 1), ("button", "Button", 4), ("pressure_plate", "Pressure Plate", 2)]
NATURE_SHAPES = {"slab", "vertical_slab", "slope", "layer", "vertical_layer", "carpet"}

LOGS = ["oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log",
        "pale_oak_log", "crimson_stem", "warped_stem", "mushroom_stem"]
LOG_WIDTHS = [(12, 1), (8, 2), (4, 4)]  # width, how many one log makes

LOW_PLANTS = ["short_grass", "fern", "short_dry_grass", "bush"]
LOW_SIZES = [50, 75, 125, 150, 175]
PLANTS = ["red_mushroom", "brown_mushroom", "crimson_fungus", "warped_fungus", "dandelion", "poppy", "blue_orchid", "allium",
          "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower",
          "lily_of_the_valley", "torchflower", "wither_rose"]
PLANT_SIZES = [50, 75, 125, 150]
GRASSY = {"short_grass", "fern", "bush"}

DOOR_WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson",
              "warped"]

TABS = [("colours", "Colours"), ("nature", "Nature"), ("plants", "Plants"), ("doors", "Doors")]

HORIZONTAL = ["north", "east", "south", "west"]
Y_FROM_NORTH = {"north": 0, "east": 90, "south": 180, "west": 270}
GRASS_TINT = [{"type": "minecraft:grass", "downfall": 1.0, "temperature": 0.5}]


def load_list(name):
    with open(os.path.join(TEMPLATES, name)) as f:
        return set(json.load(f)["values"])


VANILLA_PICKAXE = load_list("vanilla_pickaxe.json")
VANILLA_SHOVEL = load_list("vanilla_shovel.json")


def applies(mat, kind, suffix):
    if kind == "nature":
        return suffix in NATURE_SHAPES
    return not (suffix == "carpet" and kind == "wool")


def textures_of(mat):
    t = TEXTURES.get(mat, (mat, mat, mat, None))
    return {"top": "minecraft:block/" + t[0], "side": "minecraft:block/" + t[1], "bottom": "minecraft:block/" + t[2],
            "overlay": "minecraft:block/" + t[3] if t[3] else None}


def height_of(mat):
    return 15 if mat in ("dirt_path", "farmland") else 16


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


def tex(mat):
    return "minecraft:block/" + mat


def m(name):
    return NS + ":block/" + name


def model(name, parent, textures):
    write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": parent, "textures": textures})


def blockstate(name, obj):
    write(os.path.join(ASSETS, "blockstates", name + ".json"), obj)


def item(name, model_id, tinted=False):
    mdl = {"type": "minecraft:model", "model": model_id}
    if tinted:
        mdl["tints"] = GRASS_TINT
    write(os.path.join(ASSETS, "items", name + ".json"), {"model": mdl})


def from_template(template, values):
    with open(os.path.join(TEMPLATES, template + ".json")) as f:
        s = f.read()
    for k, v in values.items():
        s = s.replace('"{' + k + '}"', json.dumps(v))
    return json.loads(s)


# ---------- template models ----------

STAIRS_DISPLAY = {"gui": {"rotation": [30, 135, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                  "head": {"rotation": [0, -90, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
                  "thirdperson_lefthand": {"rotation": [75, -135, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]}}
THIN_DISPLAY = {"thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 2], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4.2, 0], "scale": [0.4, 0.4, 0.4]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 4.2, 0], "scale": [0.4, 0.4, 0.4]}}

DIRS = ["down", "up", "north", "south", "west", "east"]


def edge_faces(frm, to, top):
    out = []
    if frm[1] == 0: out.append("down")
    if to[1] == 16 or (to[1] == top and top == 16): out.append("up")
    if frm[2] == 0: out.append("north")
    if to[2] == 16: out.append("south")
    if frm[0] == 0: out.append("west")
    if to[0] == 16: out.append("east")
    return out


def side_uv(d, frm, to, top):
    """Side texture hung from the top of the piece, so its top band (grass edge, sandstone rim) sits on top."""
    v0, v1 = top - to[1], top - frm[1]
    if d == "north": return [16 - to[0], v0, 16 - frm[0], v1]
    if d == "south": return [frm[0], v0, to[0], v1]
    if d == "west": return [frm[2], v0, to[2], v1]
    return [16 - to[2], v0, 16 - frm[2], v1]


def element(frm, to, top, anchored, tinted_top, overlay_pass=False):
    """One box. Faces: #top up, #bottom down, #side round; tinted_top tints the top (grass)."""
    culls = edge_faces(frm, to, top)
    faces = {}
    for d in DIRS:
        if overlay_pass and d in ("up", "down"):
            continue
        if overlay_pass:
            f = {"texture": "#overlay", "tintindex": 0}
        elif d == "up":
            f = {"texture": "#top"}
            if tinted_top: f["tintindex"] = 0
        elif d == "down":
            f = {"texture": "#bottom"}
        else:
            f = {"texture": "#side"}
        if d not in ("up", "down") and anchored:
            f["uv"] = side_uv(d, frm, to, top)
        if d in culls:
            f["cullface"] = d
        faces[d] = f
    return {"from": frm, "to": to, "faces": faces}


def template(name, boxes, top=16, anchored=False, grass=False, display=None):
    """A template model; anchored hangs side textures from the top, grass adds the tinted overlay."""
    elements = [element(f, t, top, anchored, grass) for f, t in boxes]
    if grass:
        elements += [element(f, t, top, anchored, grass, overlay_pass=True) for f, t in boxes]
    obj = {"parent": "minecraft:block/block", "textures": {"particle": "#side"}, "elements": elements}
    if display:
        obj["display"] = display
    write(os.path.join(ASSETS, "models", "block", name + ".json"), obj)


def variant_suffix(anchored, grass, top):
    return ("_a" if anchored else "") + ("_g" if grass else "") + ("_15" if top == 15 else "")


def shape_templates(anchored, grass, top):
    sfx = variant_suffix(anchored, grass, top)
    half = top / 2
    template("template_slab" + sfx, [([0, 0, 0], [16, half, 16])], half, anchored, grass)
    template("template_slab_top" + sfx, [([0, half, 0], [16, top, 16])], top, anchored, grass)
    template("template_vertical_slab" + sfx, [([0, 0, 0], [16, top, 8])], top, anchored, grass)
    for n in range(1, 8):
        template("template_layer_%d%s" % (n, sfx), [([0, 0, 0], [16, 2 * n, 16])], 2 * n, anchored, grass, THIN_DISPLAY if n <= 2 else None)
        template("template_vertical_layer_%d%s" % (n, sfx), [([0, 0, 0], [16, top, 2 * n])], top, anchored, grass)
    template("template_carpet" + sfx, [([0, 0, 0], [16, 1, 16])], 1, anchored, grass, THIN_DISPLAY)
    template("template_panel" + sfx, [([0, 0, 0], [16, top, 1])], top, anchored, grass)
    template("template_post" + sfx, [([5, 0, 5], [11, top, 11])], top, anchored, grass)
    # The slope in the hand and the inventory: eight steps rising east, so it faces like vanilla stairs.
    template("template_slope_item" + sfx, [([2 * k, 0, 0], [2 * k + 2, (2 * k + 2) * top / 16, 16]) for k in range(8)], top, anchored,
             grass, STAIRS_DISPLAY)


def glass_fence_side():
    """Fence bars that stop at the post, so see-through glass doesn't show them overlapping it."""
    def bar(y0, y1, v0, v1):
        return {"from": [7, y0, 0], "to": [9, y1, 6], "faces": {
            "down": {"uv": [7, 0, 9, 6], "texture": "#texture"}, "up": {"uv": [7, 0, 9, 6], "texture": "#texture"},
            "north": {"uv": [7, v0, 9, v1], "texture": "#texture", "cullface": "north"},
            "west": {"uv": [0, v0, 6, v1], "texture": "#texture"}, "east": {"uv": [0, v0, 6, v1], "texture": "#texture"}}}
    write(os.path.join(ASSETS, "models", "block", "template_glass_fence_side.json"),
          {"textures": {"particle": "#texture"}, "elements": [bar(12, 15, 1, 4), bar(6, 9, 7, 10)]})


def thin_log_templates():
    for w, _ in LOG_WIDTHS:
        a, b = (16 - w) / 2, 16 - (16 - w) / 2
        faces = {"down": {"texture": "#end", "uv": [0, 0, 16, 16], "cullface": "down"},
                 "up": {"texture": "#end", "uv": [0, 0, 16, 16], "cullface": "up"}}
        for d in ["north", "south", "west", "east"]:
            faces[d] = {"texture": "#side", "uv": [0, 0, 16, 16]}
        write(os.path.join(ASSETS, "models", "block", "template_thin_log_%d.json" % w),
              {"parent": "minecraft:block/block", "textures": {"particle": "#side"}, "elements": [{"from": [a, 0, a], "to": [b, 16, b], "faces": faces}]})


def plant_templates():
    for size in sorted(set(LOW_SIZES + PLANT_SIZES)):
        s = size / 100
        for tinted in (False, True):
            def face(d):
                f = {"uv": [0, 0, 16, 16], "texture": "#cross"}
                if tinted: f["tintindex"] = 0
                return f
            lo, hi, h = round(8 - 7.2 * s, 3), round(8 + 7.2 * s, 3), round(16 * s, 3)
            rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
            elements = [{"from": [lo, 0, 8], "to": [hi, h, 8], "rotation": rot, "shade": False, "faces": {"north": face("north"), "south": face("south")}},
                        {"from": [8, 0, lo], "to": [8, h, hi], "rotation": rot, "shade": False, "faces": {"west": face("west"), "east": face("east")}}]
            name = "template_%scross_%d" % ("tinted_" if tinted else "", size)
            write(os.path.join(ASSETS, "models", "block", name + ".json"),
                  {"ambientocclusion": False, "textures": {"particle": "#cross"}, "elements": elements})


def templates():
    shape_templates(False, False, 16)
    for grass in (False, True):
        for top in (16, 15):
            shape_templates(True, grass, top)
    glass_fence_side()
    thin_log_templates()
    plant_templates()


# ---------- shapes of materials ----------

def shape_textures(mat, kind):
    if kind == "nature":
        t = textures_of(mat)
        out = {"top": t["top"], "side": t["side"], "bottom": t["bottom"]}
        if t["overlay"]: out["overlay"] = t["overlay"]
        return out, variant_suffix(True, bool(t["overlay"]), height_of(mat))
    return {"top": tex(mat), "side": tex(mat), "bottom": tex(mat)}, ""


def stairs(mat, kind, name):
    t = {"bottom": tex(mat), "top": tex(mat), "side": tex(mat)}
    model(name, "minecraft:block/stairs", t)
    model(name + "_inner", "minecraft:block/inner_stairs", t)
    model(name + "_outer", "minecraft:block/outer_stairs", t)
    blockstate(name, from_template("stairs", {"straight": m(name), "inner": m(name + "_inner"), "outer": m(name + "_outer")}))
    item(name, m(name))


def slab(mat, kind, name):
    if kind == "nature":
        t, sfx = shape_textures(mat, kind)
        model(name, m("template_slab" + sfx), t)
        model(name + "_top", m("template_slab_top" + sfx), t)
    else:
        t = {"bottom": tex(mat), "top": tex(mat), "side": tex(mat)}
        model(name, "minecraft:block/slab", t)
        model(name + "_top", "minecraft:block/slab_top", t)
    blockstate(name, {"variants": {"type=bottom": {"model": m(name)}, "type=top": {"model": m(name + "_top")},
                                   "type=double": {"model": "minecraft:block/" + mat}}})
    item(name, m(name), mat == "grass_block")


def vertical_slab(mat, kind, name):
    t, sfx = shape_textures(mat, kind)
    model(name, m("template_vertical_slab" + sfx), t)
    v = {}
    for d in HORIZONTAL:
        v["double=false,facing=" + d] = {"model": m(name), "y": Y_FROM_NORTH[d], "uvlock": kind != "nature"}
        v["double=true,facing=" + d] = {"model": "minecraft:block/" + mat}
    blockstate(name, {"variants": v})
    item(name, m(name), mat == "grass_block")


def slope(mat, kind, name):
    # The placed slope's model is built in code (SlopeModel); this one is for the item.
    t, sfx = shape_textures(mat, kind)
    if kind != "nature":
        sfx = ""
    model(name + "_item", m("template_slope_item" + sfx), t)
    item(name, m(name + "_item"), mat == "grass_block")


def layer(mat, kind, name):
    t, sfx = shape_textures(mat, kind)
    v = {}
    for n in range(1, 8):
        model(name + "_height%d" % (2 * n), m("template_layer_%d%s" % (n, sfx)), t)
        v["layers=%d" % n] = {"model": m(name + "_height%d" % (2 * n))}
    v["layers=8"] = {"model": "minecraft:block/" + mat}
    blockstate(name, {"variants": v})
    item(name, m(name + "_height2"), mat == "grass_block")


def vertical_layer(mat, kind, name):
    t, sfx = shape_textures(mat, kind)
    v = {}
    grows_y = {"south": 0, "west": 90, "north": 180, "east": 270}
    for n in range(1, 8):
        model(name + "_depth%d" % (2 * n), m("template_vertical_layer_%d%s" % (n, sfx)), t)
    for d in HORIZONTAL:
        for n in range(1, 8):
            v["facing=%s,layers=%d" % (d, n)] = {"model": m(name + "_depth%d" % (2 * n)), "y": grows_y[d], "uvlock": kind != "nature"}
        v["facing=%s,layers=8" % d] = {"model": "minecraft:block/" + mat}
    blockstate(name, {"variants": v})
    item(name, m(name + "_depth4"), mat == "grass_block")


def carpet(mat, kind, name):
    if kind == "nature":
        t, sfx = shape_textures(mat, kind)
        model(name, m("template_carpet" + sfx), t)
    else:
        model(name, "minecraft:block/carpet", {"wool": tex(mat)})
    blockstate(name, {"variants": {"": {"model": m(name)}}})
    item(name, m(name), mat == "grass_block")


def panel(mat, kind, name):
    t, sfx = shape_textures(mat, kind)
    model(name, m("template_panel" + sfx), t)
    blockstate(name, {"variants": {"facing=" + d: {"model": m(name), "y": Y_FROM_NORTH[d], "uvlock": True} for d in HORIZONTAL}})
    item(name, m(name))


def post(mat, kind, name):
    t, sfx = shape_textures(mat, kind)
    model(name, m("template_post" + sfx), t)
    blockstate(name, {"variants": {"axis=y": {"model": m(name)}, "axis=z": {"model": m(name), "x": 90, "uvlock": True},
                                   "axis=x": {"model": m(name), "x": 90, "y": 90, "uvlock": True}}})
    item(name, m(name))


def wall(mat, kind, name):
    t = {"wall": tex(mat)}
    model(name + "_post", "minecraft:block/template_wall_post", t)
    model(name + "_side", "minecraft:block/template_wall_side", t)
    model(name + "_side_tall", "minecraft:block/template_wall_side_tall", t)
    model(name + "_inventory", "minecraft:block/wall_inventory", t)
    blockstate(name, from_template("wall", {"post": m(name + "_post"), "side": m(name + "_side"), "side_tall": m(name + "_side_tall")}))
    item(name, m(name + "_inventory"))


def fence(mat, kind, name):
    t = {"texture": tex(mat)}
    model(name + "_post", "minecraft:block/fence_post", t)
    model(name + "_side", m("template_glass_fence_side") if kind == "glass" else "minecraft:block/fence_side", t)
    model(name + "_inventory", "minecraft:block/fence_inventory", t)
    blockstate(name, from_template("fence", {"post": m(name + "_post"), "side": m(name + "_side")}))
    item(name, m(name + "_inventory"))


def fence_gate(mat, kind, name):
    t = {"texture": tex(mat)}
    model(name, "minecraft:block/template_fence_gate", t)
    model(name + "_open", "minecraft:block/template_fence_gate_open", t)
    model(name + "_wall", "minecraft:block/template_fence_gate_wall", t)
    model(name + "_wall_open", "minecraft:block/template_fence_gate_wall_open", t)
    blockstate(name, from_template("fence_gate", {"closed": m(name), "open": m(name + "_open"), "wall": m(name + "_wall"),
                                                  "wall_open": m(name + "_wall_open")}))
    item(name, m(name))


def trapdoor(mat, kind, name):
    t = {"texture": tex(mat)}
    model(name + "_bottom", "minecraft:block/template_trapdoor_bottom", t)
    model(name + "_top", "minecraft:block/template_trapdoor_top", t)
    model(name + "_open", "minecraft:block/template_trapdoor_open", t)
    blockstate(name, from_template("trapdoor", {"bottom": m(name + "_bottom"), "top": m(name + "_top"), "open": m(name + "_open")}))
    item(name, m(name + "_bottom"))


def button(mat, kind, name):
    t = {"texture": tex(mat)}
    model(name, "minecraft:block/button", t)
    model(name + "_pressed", "minecraft:block/button_pressed", t)
    model(name + "_inventory", "minecraft:block/button_inventory", t)
    blockstate(name, from_template("button", {"button": m(name), "pressed": m(name + "_pressed")}))
    item(name, m(name + "_inventory"))


def pressure_plate(mat, kind, name):
    t = {"texture": tex(mat)}
    model(name, "minecraft:block/pressure_plate_up", t)
    model(name + "_down", "minecraft:block/pressure_plate_down", t)
    blockstate(name, from_template("pressure_plate", {"up": m(name), "down": m(name + "_down")}))
    item(name, m(name))


BUILDERS = {"stairs": stairs, "slab": slab, "vertical_slab": vertical_slab, "slope": slope, "layer": layer,
            "vertical_layer": vertical_layer, "carpet": carpet, "panel": panel, "post": post, "wall": wall,
            "fence": fence, "fence_gate": fence_gate, "trapdoor": trapdoor, "button": button, "pressure_plate": pressure_plate}


# ---------- loot and recipes ----------

SILK = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]


def loot(name, suffix=None, silk=False):
    entry = {"type": "minecraft:item", "name": NS + ":" + name, "functions": []}
    block_id = NS + ":" + name
    if suffix in ("slab", "vertical_slab"):
        prop = {"type": "double"} if suffix == "slab" else {"double": "true"}
        entry["functions"].append({"function": "minecraft:set_count", "add": False, "count": 2.0,
                                   "conditions": [{"condition": "minecraft:block_state_property", "block": block_id, "properties": prop}]})
    if suffix in ("layer", "vertical_layer"):
        for n in range(2, 8):
            entry["functions"].append({"function": "minecraft:set_count", "add": False, "count": float(n),
                                       "conditions": [{"condition": "minecraft:block_state_property", "block": block_id,
                                                       "properties": {"layers": str(n)}}]})
    entry["functions"].append({"function": "minecraft:explosion_decay"})
    pool = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry]}
    if silk:
        pool["conditions"] = SILK
    write(os.path.join(DATA, NS, "loot_table", "blocks", name + ".json"),
          {"type": "minecraft:block", "pools": [pool], "random_sequence": NS + ":blocks/" + name})


def door_loot(name):
    write(os.path.join(DATA, NS, "loot_table", "blocks", name + ".json"), {
        "type": "minecraft:block", "random_sequence": NS + ":blocks/" + name,
        "pools": [{"bonus_rolls": 0.0, "rolls": 1.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
                   "entries": [{"type": "minecraft:item", "name": NS + ":" + name, "conditions": [
                       {"condition": "minecraft:block_state_property", "block": NS + ":" + name, "properties": {"half": "lower"}}]}]}]})


def stonecutting(source, name, count):
    write(os.path.join(DATA, NS, "recipe", name + "_from_" + source + "_stonecutting.json"),
          {"type": "minecraft:stonecutting", "ingredient": "minecraft:" + source, "result": {"id": NS + ":" + name, "count": count}})


def title(words, grey):
    s = " ".join(w.capitalize() for w in words.split("_"))
    return s.replace("Gray", "Grey") if grey else s


# ---------- the rest ----------

def thin_logs(add_name, tags):
    for log in LOGS:
        for kind in ([log, "stripped_" + log] if log != "mushroom_stem" else [log]):
            side = "minecraft:block/" + kind
            end = "minecraft:block/mushroom_block_inside" if kind == "mushroom_stem" else "minecraft:block/" + kind + "_top"
            for w, cut in LOG_WIDTHS:
                name = "%s_%d" % (kind, w)
                model(name, m("template_thin_log_%d" % w), {"side": side, "end": end})
                blockstate(name, {"variants": {"axis=y": {"model": m(name)}, "axis=z": {"model": m(name), "x": 90},
                                               "axis=x": {"model": m(name), "x": 90, "y": 90}}})
                item(name, m(name))
                loot(name)
                stonecutting(kind, name, cut)
                tags["mineable/axe"].append(NS + ":" + name)
                add_name(name, lambda g, k=kind, w=w: "%s (%d px)" % (title(k, g), w))


def plants(add_name):
    for plant, sizes in [(p, LOW_SIZES) for p in LOW_PLANTS] + [(p, PLANT_SIZES) for p in PLANTS]:
        tinted = plant in GRASSY
        for size in sizes:
            name = "%s_%d" % (plant, size)
            model(name, m("template_%scross_%d" % ("tinted_" if tinted else "", size)), {"cross": "minecraft:block/" + plant})
            blockstate(name, {"variants": {"": {"model": m(name)}}})
            write(os.path.join(ASSETS, "models", "item", name + ".json"),
                  {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:block/" + plant}})
            item(name, NS + ":item/" + name, tinted)
            loot(name)
            stonecutting(plant, name, 1)
            add_name(name, lambda g, p=plant, s=size: "%s (%d%%)" % (title(p, g), s))


def doors(add_name, tags):
    parts = ["bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open", "top_left", "top_left_open", "top_right",
             "top_right_open"]
    for wood in DOOR_WOODS:
        for colour in COLOURS:
            name = "%s_%s_door" % (colour, wood)
            t = {"bottom": NS + ":block/" + name + "_bottom", "top": NS + ":block/" + name + "_top"}
            for part in parts:
                model(name + "_" + part, "minecraft:block/door_" + part, t)
            blockstate(name, from_template("door", {part: m(name + "_" + part) for part in parts}))
            write(os.path.join(ASSETS, "models", "item", name + ".json"),
                  {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}})
            item(name, NS + ":item/" + name)
            door_loot(name)
            write(os.path.join(DATA, NS, "recipe", name + ".json"),
                  {"type": "minecraft:crafting_shapeless", "category": "redstone",
                   "ingredients": ["minecraft:%s_door" % wood, "minecraft:%s_dye" % colour], "result": {"id": NS + ":" + name, "count": 1}})
            tags["wooden_doors"].append(NS + ":" + name)
            tags["mineable/axe"].append(NS + ":" + name)
            add_name(name, lambda g, c=colour, w=wood: "%s %s Door" % (title(c, g), title(w, g)))
    # The coloured textures are made in the game from the door textures in use.
    write(os.path.join(ROOT, "assets", "minecraft", "atlases", "blocks.json"), {"sources": [
        {"type": NS + ":door_colours", "prefix": "block/",
         "textures": ["minecraft:block/%s_door_%s" % (w, p) for w in DOOR_WOODS for p in ("top", "bottom")]}]})
    write(os.path.join(ROOT, "assets", "minecraft", "atlases", "items.json"), {"sources": [
        {"type": NS + ":door_colours", "prefix": "item/", "textures": ["minecraft:item/%s_door" % w for w in DOOR_WOODS]}]})


def cube_icon():
    """The first tab's icon: a block in the brand colours, with gaps along its edges."""
    write(os.path.join(ASSETS, "items", "cube.json"), {"model": {"type": "minecraft:model", "model": NS + ":item/cube"}})
    write(os.path.join(ASSETS, "models", "item", "cube.json"), {"parent": "minecraft:block/cube", "textures": {
        "particle": NS + ":block/cube_top", "up": NS + ":block/cube_top", "down": NS + ":block/cube_top",
        "north": NS + ":block/cube_left", "south": NS + ":block/cube_left", "east": NS + ":block/cube_right", "west": NS + ":block/cube_right"}})


def main():
    for sub in ["blockstates", "models", "items"]:
        shutil.rmtree(os.path.join(ASSETS, sub), ignore_errors=True)
    for sub in ["loot_table", "recipe"]:
        shutil.rmtree(os.path.join(DATA, NS, sub), ignore_errors=True)
    templates()

    tags = {k: [] for k in ["stairs", "slabs", "walls", "fences", "fence_gates", "trapdoors", "buttons", "pressure_plates",
                            "wooden_doors", "mineable/pickaxe", "mineable/shovel", "mineable/axe", "mineable/hoe"]}
    tag_of = {"stairs": "stairs", "slab": "slabs", "wall": "walls", "fence": "fences", "fence_gate": "fence_gates",
              "trapdoor": "trapdoors", "button": "buttons", "pressure_plate": "pressure_plates"}
    us, gb = {}, {}

    def add_name(name, fn):
        us["block.%s.%s" % (NS, name)] = fn(False)
        gb["block.%s.%s" % (NS, name)] = fn(True)

    count = 0
    for mat, kind in MATERIALS:
        for suffix, shape_name, cut in SHAPES:
            if not applies(mat, kind, suffix):
                continue
            name = mat + "_" + suffix
            BUILDERS[suffix](mat, kind, name)
            loot(name, suffix, silk=kind == "glass" or mat == "ice" or mat == "packed_ice" or mat == "blue_ice")
            stonecutting(mat, name, cut)
            if suffix in tag_of:
                tags[tag_of[suffix]].append(NS + ":" + name)
            vid = "minecraft:" + mat
            if kind in ("terracotta", "concrete", "glazed") or (kind == "nature" and vid in VANILLA_PICKAXE):
                tags["mineable/pickaxe"].append(NS + ":" + name)
            if kind == "powder" or (kind == "nature" and vid in VANILLA_SHOVEL):
                tags["mineable/shovel"].append(NS + ":" + name)
            if mat in ("moss_block", "pale_moss_block"):
                tags["mineable/hoe"].append(NS + ":" + name)
            add_name(name, lambda g, mt=mat, sn=shape_name: title(mt, g) + " " + sn)
            count += 1
    thin_logs(add_name, tags)
    plants(add_name)
    doors(add_name, tags)
    cube_icon()

    for tag, values in tags.items():
        write(os.path.join(DATA, "minecraft", "tags", "block", tag + ".json"), {"replace": False, "values": values})
    for tag in ["stairs", "slabs", "walls", "fences", "fence_gates", "trapdoors", "buttons", "wooden_doors"]:
        write(os.path.join(DATA, "minecraft", "tags", "item", tag + ".json"), {"replace": False, "values": tags[tag]})

    for tab, name in TABS:
        us["itemGroup.%s.%s" % (NS, tab)] = gb["itemGroup.%s.%s" % (NS, tab)] = "Cubework: " + name
    us["item.%s.cube" % NS] = gb["item.%s.cube" % NS] = "Cubework"
    write(os.path.join(ASSETS, "lang", "en_us.json"), us)
    write(os.path.join(ASSETS, "lang", "en_gb.json"), gb)
    print("shapes:", count, "names:", len(us))


if __name__ == "__main__":
    main()
