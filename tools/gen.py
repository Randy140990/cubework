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

# Building blocks, in John's order (as Material.BUILDING): every shape, plus pillars. Some are natural blocks too.
BUILDING = [
    "stone", "cobblestone", "mossy_cobblestone", "smooth_stone", "stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks",
    "mossy_stone_bricks", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite",
    "deepslate", "cobbled_deepslate", "chiseled_deepslate", "polished_deepslate", "deepslate_bricks", "cracked_deepslate_bricks",
    "deepslate_tiles", "cracked_deepslate_tiles", "tuff", "chiseled_tuff", "polished_tuff", "tuff_bricks",
    "chiseled_tuff_bricks", "bricks", "packed_mud", "mud_bricks", "resin_bricks", "chiseled_resin_bricks", "sandstone",
    "chiseled_sandstone", "smooth_sandstone", "cut_sandstone", "red_sandstone", "chiseled_red_sandstone", "smooth_red_sandstone",
    "cut_red_sandstone", "prismarine", "prismarine_bricks", "dark_prismarine", "netherrack", "nether_bricks",
    "cracked_nether_bricks", "chiseled_nether_bricks", "red_nether_bricks", "basalt", "smooth_basalt", "polished_basalt",
    "blackstone", "gilded_blackstone", "chiseled_polished_blackstone", "polished_blackstone", "polished_blackstone_bricks",
    "cracked_polished_blackstone_bricks", "end_stone_bricks", "purpur_block", "purpur_pillar", "coal_block", "iron_block",
    "gold_block", "redstone_block", "emerald_block", "lapis_block", "diamond_block", "netherite_block", "quartz_block",
    "chiseled_quartz_block", "quartz_bricks", "quartz_pillar", "smooth_quartz", "amethyst_block", "copper_block",
    "chiseled_copper", "cut_copper", "exposed_copper", "exposed_chiseled_copper", "exposed_cut_copper", "weathered_copper",
    "weathered_chiseled_copper", "weathered_cut_copper", "oxidized_copper", "oxidized_chiseled_copper", "oxidized_cut_copper",
    "waxed_copper_block", "waxed_chiseled_copper", "waxed_cut_copper", "waxed_exposed_copper", "waxed_exposed_chiseled_copper",
    "waxed_exposed_cut_copper", "waxed_weathered_copper", "waxed_weathered_chiseled_copper", "waxed_weathered_cut_copper",
    "waxed_oxidized_copper", "waxed_oxidized_chiseled_copper", "waxed_oxidized_cut_copper"]
BUILDING_SET = set(BUILDING)
STEMS = {"stone_bricks": "stone_brick", "cracked_stone_bricks": "cracked_stone_brick", "chiseled_stone_bricks": "chiseled_stone_brick",
         "mossy_stone_bricks": "mossy_stone_brick", "deepslate_bricks": "deepslate_brick", "cracked_deepslate_bricks": "cracked_deepslate_brick",
         "deepslate_tiles": "deepslate_tile", "cracked_deepslate_tiles": "cracked_deepslate_tile", "tuff_bricks": "tuff_brick",
         "chiseled_tuff_bricks": "chiseled_tuff_brick", "bricks": "brick", "mud_bricks": "mud_brick", "resin_bricks": "resin_brick",
         "chiseled_resin_bricks": "chiseled_resin_brick", "prismarine_bricks": "prismarine_brick", "nether_bricks": "nether_brick",
         "cracked_nether_bricks": "cracked_nether_brick", "chiseled_nether_bricks": "chiseled_nether_brick", "red_nether_bricks": "red_nether_brick",
         "polished_blackstone_bricks": "polished_blackstone_brick", "cracked_polished_blackstone_bricks": "cracked_polished_blackstone_brick",
         "end_stone_bricks": "end_stone_brick", "purpur_block": "purpur", "coal_block": "coal", "iron_block": "iron", "gold_block": "gold",
         "redstone_block": "redstone", "emerald_block": "emerald", "lapis_block": "lapis", "diamond_block": "diamond",
         "netherite_block": "netherite", "quartz_block": "quartz", "chiseled_quartz_block": "chiseled_quartz", "quartz_bricks": "quartz_brick",
         "copper_block": "copper", "waxed_copper_block": "waxed_copper"}


def stem(mat):
    return STEMS.get(mat, mat)


def is_building(mat):
    return mat in BUILDING_SET


def full_model(mat):
    """The vanilla full block's model (waxed copper uses unwaxed copper's)."""
    return "minecraft:block/" + (mat[6:] if mat.startswith("waxed_") else mat)


# (id, kind), in the same order as Material.java
MATERIALS = [(c + "_wool", "wool") for c in COLOURS] + [("terracotta", "terracotta")] + \
            [(c + "_terracotta", "terracotta") for c in COLOURS] + [(c + "_concrete", "concrete") for c in COLOURS] + \
            [(c + "_concrete_powder", "powder") for c in COLOURS] + [(c + "_glazed_terracotta", "glazed") for c in COLOURS] + \
            [("glass", "glass"), ("tinted_glass", "glass")] + [(c + "_stained_glass", "glass") for c in COLOURS] + \
            [(n, "nature") for n in NATURE] + [(b, "building") for b in BUILDING if b not in NATURE]

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
    "chiseled_tuff": ("chiseled_tuff_top", "chiseled_tuff", "chiseled_tuff_top", None),
    "chiseled_tuff_bricks": ("chiseled_tuff_bricks_top", "chiseled_tuff_bricks", "chiseled_tuff_bricks_top", None),
    "chiseled_sandstone": ("sandstone_top", "chiseled_sandstone", "sandstone_top", None),
    "smooth_sandstone": ("sandstone_top", "sandstone_top", "sandstone_top", None),
    "cut_sandstone": ("sandstone_top", "cut_sandstone", "sandstone_top", None),
    "chiseled_red_sandstone": ("red_sandstone_top", "chiseled_red_sandstone", "red_sandstone_top", None),
    "smooth_red_sandstone": ("red_sandstone_top", "red_sandstone_top", "red_sandstone_top", None),
    "cut_red_sandstone": ("red_sandstone_top", "cut_red_sandstone", "red_sandstone_top", None),
    "polished_basalt": ("polished_basalt_top", "polished_basalt_side", "polished_basalt_top", None),
    "purpur_pillar": ("purpur_pillar_top", "purpur_pillar", "purpur_pillar_top", None),
    "quartz_block": ("quartz_block_top", "quartz_block_side", "quartz_block_top", None),
    "chiseled_quartz_block": ("chiseled_quartz_block_top", "chiseled_quartz_block", "chiseled_quartz_block_top", None),
    "quartz_pillar": ("quartz_pillar_top", "quartz_pillar", "quartz_pillar_top", None),
    "smooth_quartz": ("quartz_block_bottom", "quartz_block_bottom", "quartz_block_bottom", None),
}

# (suffix, English name, how many one block makes in the stonecutter), in the order of Shape.java
SHAPES = [("stairs", "Stairs", 1), ("slab", "Slab", 2), ("quarter_slab", "Quarter Slab", 4), ("vertical_slab", "Vertical Slab", 2),
          ("slope", "Slope", 2), ("gentle_slope", "Gentle Slope", 2), ("layer", "Layer", 8), ("vertical_layer", "Vertical Layer", 8),
          ("carpet", "Carpet", 4), ("panel", "Panel", 4), ("post", "Post", 4), ("pillar_12", "(12 px)", 1), ("pillar_8", "(8 px)", 2),
          ("pillar_4", "(4 px)", 4), ("arch", "Arch", 1), ("arch_corner", "Arch Corner", 1),
          ("wall", "Wall", 1), ("fence", "Fence", 1), ("fence_gate", "Fence Gate", 1),
          ("trapdoor", "Trapdoor", 1), ("button", "Button", 4), ("pressure_plate", "Pressure Plate", 2)] + \
         [("window_frame_%d" % d, "Window Frame (%d px)" % d, c) for d, c in ((4, 4), (8, 2), (12, 1), (16, 1))] + \
         [("diamond_window_frame_%d" % d, "Diamond Window Frame (%d px)" % d, c) for d, c in ((4, 4), (8, 2), (12, 1), (16, 1))]
NATURE_SHAPES = {"slab", "vertical_slab", "slope", "layer", "vertical_layer", "carpet"}
# Gentle slopes, quarter slabs, arches and window frames: wool, terracotta and concrete for now.
CASTLE_SHAPES = {"quarter_slab", "gentle_slope", "arch", "arch_corner"} | {"%swindow_frame_%d" % (p, d) for p in ("", "diamond_") for d in (4, 8, 12, 16)}
WINDOW_DEPTHS = [4, 8, 12, 16]

LOGS = ["oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log",
        "pale_oak_log", "crimson_stem", "warped_stem", "mushroom_stem"]
LOG_WIDTHS = [(16, 1), (12, 1), (8, 2), (4, 4)]  # width, how many one log makes
PILLAR_WIDTHS = [12, 8, 4]

LOW_PLANTS = ["short_grass", "fern", "short_dry_grass", "bush"]
LOW_SIZES = [50, 75, 125, 150, 175]
PLANTS = ["red_mushroom", "brown_mushroom", "crimson_fungus", "warped_fungus", "dandelion", "poppy", "blue_orchid", "allium",
          "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower",
          "lily_of_the_valley", "torchflower", "wither_rose"]
PLANT_SIZES = [50, 75, 125, 150]
GRASSY = {"short_grass", "fern", "bush"}

CORALS = ["tube", "brain", "bubble", "fire", "horn"]
CORAL_SIZES = [50, 75, 125, 150]

# The copper door's pattern in metals and gems: (name, block whose colours it takes, what crafts it with a copper door)
METAL_DOORS = [("iron", "iron_block", "iron_ingot"), ("gold", "gold_block", "gold_ingot"), ("redstone", "redstone_block", "redstone"),
               ("emerald", "emerald_block", "emerald"), ("lapis", "lapis_block", "lapis_lazuli"), ("diamond", "diamond_block", "diamond"),
               ("netherite", "netherite_block", "netherite_ingot")]

DOOR_WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson",
              "warped"]

TABS = [("colours", "Colours"), ("building", "Building"), ("nature", "Nature"), ("plants", "Plants"), ("doors", "Doors")]

HORIZONTAL = ["north", "east", "south", "west"]
Y_FROM_NORTH = {"north": 0, "east": 90, "south": 180, "west": 270}
GRASS_TINT = [{"type": "minecraft:grass", "downfall": 1.0, "temperature": 0.5}]


def load_list(name):
    with open(os.path.join(TEMPLATES, name)) as f:
        return set(json.load(f)["values"])


VANILLA_PICKAXE = load_list("vanilla_pickaxe.json")
VANILLA_SHOVEL = load_list("vanilla_shovel.json")
VANILLA_NEEDS = {tier: load_list("vanilla_needs_%s.json" % tier) for tier in ("stone", "iron", "diamond")}


def applies(mat, kind, suffix):
    if suffix.startswith("pillar_"):
        return is_building(mat)
    if is_building(mat):
        return True
    if kind == "nature":
        return suffix in NATURE_SHAPES
    if suffix in CASTLE_SHAPES and kind not in ("wool", "terracotta", "concrete"):
        return False
    return not (suffix == "carpet" and kind == "wool")


def textures_of(mat):
    if mat.startswith("waxed_"):
        mat = mat[6:]
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


def faces(mat):
    """Top, side and bottom textures of any block (all the same for coloured blocks)."""
    t = textures_of(mat)
    return {"top": t["top"], "side": t["side"], "bottom": t["bottom"]}


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
    """Logs (and chiseled, smooth and pillar blocks) with their whole texture squeezed onto each face."""
    for w, _ in LOG_WIDTHS:
        a, b = (16 - w) / 2, 16 - (16 - w) / 2
        faces = {"down": {"texture": "#bottom", "uv": [0, 0, 16, 16], "cullface": "down"},
                 "up": {"texture": "#top", "uv": [0, 0, 16, 16], "cullface": "up"}}
        for d in ["north", "south", "west", "east"]:
            faces[d] = {"texture": "#side", "uv": [0, 0, 16, 16]}
            if w == 16:
                faces[d]["cullface"] = d
        write(os.path.join(ASSETS, "models", "block", "template_thin_log_%d.json" % w),
              {"parent": "minecraft:block/block", "textures": {"particle": "#side"}, "elements": [{"from": [a, 0, a], "to": [b, 16, b], "faces": faces}]})
    # Other pillars: cut from the middle of the texture, so bricks stay brick-sized.
    for w in PILLAR_WIDTHS:
        a, b = (16 - w) / 2, 16 - (16 - w) / 2
        template("template_pillar_%d" % w, [([a, 0, a], [b, 16, b])])


def coral_fan_templates():
    """The vanilla coral fan, scaled about the middle of the block's floor."""
    def sc(v, s):
        return [round(8 + (v[0] - 8) * s, 4), round(v[1] * s, 4), round(8 + (v[2] - 8) * s, 4)]
    base = [([8, 0, 0], [24, 0, 16], [8, 0, 0], "z", 22.5, {"up": 90, "down": 270}),
            ([-8, 0, 0], [8, 0, 16], [8, 0, 0], "z", -22.5, {"up": 270, "down": 90}),
            ([0, 0, 8], [16, 0, 24], [0, 0, 8], "x", -22.5, None),
            ([0, 0, -8], [16, 0, 8], [0, 0, 8], "x", 22.5, None)]
    for size in CORAL_SIZES:
        s = size / 100
        elements = []
        for i, (f, t, o, axis, angle, rot) in enumerate(base):
            up = {"uv": [0, 0, 16, 16] if i != 2 else [16, 16, 0, 0], "texture": "#fan"}
            down = {"uv": [0, 16, 16, 0] if i != 2 else [16, 0, 0, 16], "texture": "#fan"}
            if rot:
                up["rotation"], down["rotation"] = rot["up"], rot["down"]
            elements.append({"from": sc(f, s), "to": sc(t, s), "rotation": {"origin": sc(o, s), "axis": axis, "angle": angle, "rescale": False},
                             "shade": False, "faces": {"up": up, "down": down}})
        write(os.path.join(ASSETS, "models", "block", "template_coral_fan_%d.json" % size),
              {"ambientocclusion": False, "textures": {"particle": "#fan"}, "elements": elements})


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


# ---------- castle shapes: outlines (16 x 16, x across from the viewer's left, y up), as Profiles.java ----------

def arch_mask():
    return [[((x + 0.5 - 8) / 8) ** 2 + ((y + 0.5) / 12) ** 2 >= 1 for y in range(16)] for x in range(16)]


def arch_corner_mask():
    return [[((x + 0.5 - 16) / 16) ** 2 + ((y + 0.5) / 12) ** 2 >= 1 for y in range(16)] for x in range(16)]


def mask_boxes(mask, z0, z1):
    """Rows of solid pixels as boxes."""
    out = []
    for y in range(16):
        start = None
        for x in range(17):
            solid = x < 16 and mask[x][y]
            if solid and start is None: start = x
            if not solid and start is not None:
                out.append(([start, y, z0], [x, y + 1, z1]))
                start = None
    return out


# Window frame pieces (facing north: against the north side, seen from the south).
WINDOW_RECTS = {"bar_left": ((0, 4), (4, 12)), "bar_right": ((12, 16), (4, 12)), "bar_bottom": ((4, 12), (0, 4)), "bar_top": ((4, 12), (12, 16)),
                "corner_bl": ((0, 4), (0, 4)), "corner_br": ((12, 16), (0, 4)), "corner_tl": ((0, 4), (12, 16)), "corner_tr": ((12, 16), (12, 16))}


def window_piece_mask(piece):
    m = [[False] * 16 for _ in range(16)]
    if piece in WINDOW_RECTS:
        (x0, x1), (y0, y1) = WINDOW_RECTS[piece]
        for x in range(x0, x1):
            for y in range(y0, y1): m[x][y] = True
        return m
    for x in range(4, 12):
        for y in range(4, 12):
            left, bottom = x < 8, y < 8
            i = x - 4 if left else 11 - x
            j = y - 4 if bottom else 11 - y
            want = ("b" if bottom else "t") + ("l" if left else "r")
            if piece == "tri_" + want and i + j <= 2: m[x][y] = True
    return m


WINDOW_PIECES = list(WINDOW_RECTS) + ["tri_bl", "tri_br", "tri_tl", "tri_tr"]


def castle_templates():
    template("template_quarter_slab", [([0, 0, 0], [16, 4, 16])], 4)
    template("template_quarter_slab_top", [([0, 12, 0], [16, 16, 16])], 16)
    template("template_arch", mask_boxes(arch_mask(), 0, 16))
    template("template_arch_corner", mask_boxes(arch_corner_mask(), 0, 16))
    # The gentle slope in the hand: one step (4 to 8 pixels), rising east like stairs.
    template("template_gentle_slope_item", [([2 * k, 0, 0], [2 * k + 2, 4 + (k + 1) * 0.5, 16]) for k in range(8)], 8, display=STAIRS_DISPLAY)
    for d in WINDOW_DEPTHS:
        for piece in WINDOW_PIECES:
            template("template_window_%d_%s" % (d, piece), mask_boxes(window_piece_mask(piece), 0, d))
        for diamond in (False, True):
            pieces = list(WINDOW_RECTS) + (["tri_bl", "tri_br", "tri_tl", "tri_tr"] if diamond else [])
            boxes = [b for p in pieces for b in mask_boxes(window_piece_mask(p), 0, d)]
            template("template_%swindow_%d_inventory" % ("diamond_" if diamond else "", d), boxes)


def templates():
    castle_templates()
    shape_templates(False, False, 16)
    for grass in (False, True):
        for top in (16, 15):
            shape_templates(True, grass, top)
    glass_fence_side()
    thin_log_templates()
    plant_templates()
    coral_fan_templates()


# ---------- shapes of materials ----------

def shape_textures(mat, kind):
    if kind == "nature":
        t = textures_of(mat)
        out = {"top": t["top"], "side": t["side"], "bottom": t["bottom"]}
        if t["overlay"]: out["overlay"] = t["overlay"]
        return out, variant_suffix(True, bool(t["overlay"]), height_of(mat))
    return faces(mat), ""


def stairs(mat, kind, name):
    t = faces(mat)
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
        t = faces(mat)
        model(name, "minecraft:block/slab", t)
        model(name + "_top", "minecraft:block/slab_top", t)
    blockstate(name, {"variants": {"type=bottom": {"model": m(name)}, "type=top": {"model": m(name + "_top")},
                                   "type=double": {"model": full_model(mat)}}})
    item(name, m(name), mat == "grass_block")


def vertical_slab(mat, kind, name):
    t, sfx = shape_textures(mat, kind)
    model(name, m("template_vertical_slab" + sfx), t)
    v = {}
    for d in HORIZONTAL:
        v["double=false,facing=" + d] = {"model": m(name), "y": Y_FROM_NORTH[d], "uvlock": kind != "nature"}
        v["double=true,facing=" + d] = {"model": full_model(mat)}
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
    v["layers=8"] = {"model": full_model(mat)}
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
        v["facing=%s,layers=8" % d] = {"model": full_model(mat)}
    blockstate(name, {"variants": v})
    item(name, m(name + "_depth4"), mat == "grass_block")


def carpet(mat, kind, name):
    if kind == "nature":
        t, sfx = shape_textures(mat, kind)
        model(name, m("template_carpet" + sfx), t)
    else:
        model(name, "minecraft:block/carpet", {"wool": faces(mat)["top"]})
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
    t = {"wall": faces(mat)["side"]}
    model(name + "_post", "minecraft:block/template_wall_post", t)
    model(name + "_side", "minecraft:block/template_wall_side", t)
    model(name + "_side_tall", "minecraft:block/template_wall_side_tall", t)
    model(name + "_inventory", "minecraft:block/wall_inventory", t)
    blockstate(name, from_template("wall", {"post": m(name + "_post"), "side": m(name + "_side"), "side_tall": m(name + "_side_tall")}))
    item(name, m(name + "_inventory"))


def fence(mat, kind, name):
    t = {"texture": faces(mat)["side"]}
    model(name + "_post", "minecraft:block/fence_post", t)
    model(name + "_side", m("template_glass_fence_side") if kind == "glass" else "minecraft:block/fence_side", t)
    model(name + "_inventory", "minecraft:block/fence_inventory", t)
    blockstate(name, from_template("fence", {"post": m(name + "_post"), "side": m(name + "_side")}))
    item(name, m(name + "_inventory"))


def fence_gate(mat, kind, name):
    t = {"texture": faces(mat)["side"]}
    model(name, "minecraft:block/template_fence_gate", t)
    model(name + "_open", "minecraft:block/template_fence_gate_open", t)
    model(name + "_wall", "minecraft:block/template_fence_gate_wall", t)
    model(name + "_wall_open", "minecraft:block/template_fence_gate_wall_open", t)
    blockstate(name, from_template("fence_gate", {"closed": m(name), "open": m(name + "_open"), "wall": m(name + "_wall"),
                                                  "wall_open": m(name + "_wall_open")}))
    item(name, m(name))


def trapdoor(mat, kind, name):
    t = {"texture": faces(mat)["top"]}
    model(name + "_bottom", "minecraft:block/template_trapdoor_bottom", t)
    model(name + "_top", "minecraft:block/template_trapdoor_top", t)
    model(name + "_open", "minecraft:block/template_trapdoor_open", t)
    blockstate(name, from_template("trapdoor", {"bottom": m(name + "_bottom"), "top": m(name + "_top"), "open": m(name + "_open")}))
    item(name, m(name + "_bottom"))


def button(mat, kind, name):
    t = {"texture": faces(mat)["side"]}
    model(name, "minecraft:block/button", t)
    model(name + "_pressed", "minecraft:block/button_pressed", t)
    model(name + "_inventory", "minecraft:block/button_inventory", t)
    blockstate(name, from_template("button", {"button": m(name), "pressed": m(name + "_pressed")}))
    item(name, m(name + "_inventory"))


def pressure_plate(mat, kind, name):
    t = {"texture": faces(mat)["top"]}
    model(name, "minecraft:block/pressure_plate_up", t)
    model(name + "_down", "minecraft:block/pressure_plate_down", t)
    blockstate(name, from_template("pressure_plate", {"up": m(name), "down": m(name + "_down")}))
    item(name, m(name))


def quarter_slab(mat, kind, name):
    t, _ = shape_textures(mat, kind)
    model(name, m("template_quarter_slab"), t)
    model(name + "_top", m("template_quarter_slab_top"), t)
    blockstate(name, {"variants": {"half=bottom": {"model": m(name)}, "half=top": {"model": m(name + "_top")}}})
    item(name, m(name))


def gentle_slope(mat, kind, name):
    # Placed gentle slopes are built in code (SlopeModel); this one is for the item.
    t, _ = shape_textures(mat, kind)
    model(name + "_item", m("template_gentle_slope_item"), t)
    item(name, m(name + "_item"))


def arch(mat, kind, name):
    # Placed arches are drawn smooth in code (ArchModel); this stepped one is for the item.
    t, _ = shape_textures(mat, kind)
    model(name, m("template_arch"), t)
    item(name, m(name))


def arch_corner(mat, kind, name):
    # Placed arch corners are drawn smooth in code (ArchModel); this stepped one (rising east) is for the item.
    t, _ = shape_textures(mat, kind)
    model(name, m("template_arch_corner"), t)
    item(name, m(name))


def pillar(mat, kind, name, w):
    """Chiseled, smooth and pillar blocks squeeze their whole texture onto the pillar; the rest are cut from the middle."""
    f = faces(mat)
    squeeze = "chiseled" in mat or "smooth" in mat or mat.endswith("pillar")
    model(name, m(("template_thin_log_%d" if squeeze else "template_pillar_%d") % w), f)
    lock = not squeeze
    blockstate(name, {"variants": {"axis=y": {"model": m(name)}, "axis=z": {"model": m(name), "x": 90, "uvlock": lock},
                                   "axis=x": {"model": m(name), "x": 90, "y": 90, "uvlock": lock}}})
    item(name, m(name))


def window_frame(mat, kind, name, depth, diamond):
    t, _ = shape_textures(mat, kind)
    pieces = list(WINDOW_RECTS) + (["tri_bl", "tri_br", "tri_tl", "tri_tr"] if diamond else [])
    for p in pieces:
        model("%s_%s" % (name, p), m("template_window_%d_%s" % (depth, p)), t)
    model(name + "_inventory", m("template_%swindow_%d_inventory" % ("diamond_" if diamond else "", depth)), t)
    side = {"l": "left", "r": "right", "b": "down", "t": "up"}
    parts = []
    for f in HORIZONTAL:
        apply = lambda p: {"model": m("%s_%s" % (name, p)), "y": Y_FROM_NORTH[f], "uvlock": True}
        for p in pieces:
            if p.startswith("bar_"):
                key = {"bar_left": "left", "bar_right": "right", "bar_bottom": "down", "bar_top": "up"}[p]
                parts.append({"when": {"facing": f, key: "false"}, "apply": apply(p)})
            elif p.startswith("corner_"):
                v, h = side[p[-2]], side[p[-1]]
                parts.append({"when": {"OR": [{"facing": f, v: "false"}, {"facing": f, h: "false"}]}, "apply": apply(p)})
            else:
                v, h = side[p[-2]], side[p[-1]]
                parts.append({"when": {"facing": f, v: "false", h: "false"}, "apply": apply(p)})
    blockstate(name, {"multipart": parts})
    item(name, m(name + "_inventory"))


for _d in WINDOW_DEPTHS:
    for _diamond in (False, True):
        globals()["window_%s%d" % ("d" if _diamond else "s", _d)] = (lambda d, dm: lambda mat, kind, name: window_frame(mat, kind, name, d, dm))(_d, _diamond)


BUILDERS = {"stairs": stairs, "slab": slab, "vertical_slab": vertical_slab, "slope": slope, "layer": layer,
            "vertical_layer": vertical_layer, "carpet": carpet, "panel": panel, "post": post, "wall": wall,
            "fence": fence, "fence_gate": fence_gate, "trapdoor": trapdoor, "button": button, "pressure_plate": pressure_plate,
            "quarter_slab": quarter_slab, "gentle_slope": gentle_slope, "arch": arch, "arch_corner": arch_corner}
for _w in PILLAR_WIDTHS:
    BUILDERS["pillar_%d" % _w] = (lambda w: lambda mat, kind, name: pillar(mat, kind, name, w))(_w)
for _d in WINDOW_DEPTHS:
    BUILDERS["window_frame_%d" % _d] = globals()["window_s%d" % _d]
    BUILDERS["diamond_window_frame_%d" % _d] = globals()["window_d%d" % _d]


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
                # An end with another log against it shows bark, so stacked logs look like one tapering trunk.
                variants = {}
                for top in (False, True):
                    for bottom in (False, True):
                        mname = name + {(False, False): "", (True, False): "_bark_top", (False, True): "_bark_bottom", (True, True): "_bark_ends"}[(top, bottom)]
                        model(mname, m("template_thin_log_%d" % w), {"side": side, "top": side if top else end, "bottom": side if bottom else end})
                        props = "bark_bottom=%s,bark_top=%s" % (str(bottom).lower(), str(top).lower())
                        variants["axis=y," + props] = {"model": m(mname)}
                        variants["axis=z," + props] = {"model": m(mname), "x": 90}
                        variants["axis=x," + props] = {"model": m(mname), "x": 90, "y": 90}
                blockstate(name, {"variants": variants})
                item(name, m(name))
                loot(name)
                stonecutting(kind, name, cut)
                tags["mineable/axe"].append(NS + ":" + name)
                add_name(name, lambda g, k=kind, w=w: "%s (%d px)" % (title(k, g), w))


def coral(add_name):
    for c in CORALS:
        for fan in (False, True):
            for dead in (False, True):
                plant = ("dead_" if dead else "") + c + "_coral" + ("_fan" if fan else "")
                for size in CORAL_SIZES:
                    name = "%s_%d" % (plant, size)
                    if fan:
                        model(name, m("template_coral_fan_%d" % size), {"fan": "minecraft:block/" + plant})
                    else:
                        model(name, m("template_cross_%d" % size), {"cross": "minecraft:block/" + plant})
                    blockstate(name, {"variants": {"": {"model": m(name)}}})
                    write(os.path.join(ASSETS, "models", "item", name + ".json"),
                          {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:block/" + plant}})
                    item(name, NS + ":item/" + name)
                    loot(name, silk=True)
                    stonecutting(plant, name, 1)
                    add_name(name, lambda g, p=plant, s=size: "%s (%d%%)" % (title(p, g), s))


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
    parts_of = lambda name: {part: m(name + "_" + part) for part in parts}

    def metal_door(name, recipe, english):
        t = {"bottom": NS + ":block/" + name + "_bottom", "top": NS + ":block/" + name + "_top"}
        for part in parts:
            model(name + "_" + part, "minecraft:block/door_" + part, t)
        blockstate(name, from_template("door", parts_of(name)))
        write(os.path.join(ASSETS, "models", "item", name + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}})
        item(name, NS + ":item/" + name)
        door_loot(name)
        write(os.path.join(DATA, NS, "recipe", name + ".json"),
              {"type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": recipe, "result": {"id": NS + ":" + name, "count": 1}})
        tags["doors"].append(NS + ":" + name)
        tags["mineable/pickaxe"].append(NS + ":" + name)
        add_name(name, english)

    # The iron door in the 16 colours.
    for colour in COLOURS:
        metal_door("%s_iron_door" % colour, ["minecraft:iron_door", "minecraft:%s_dye" % colour], lambda g, c=colour: "%s Iron Door" % title(c, g))
    # The copper door's pattern in metals and gems.
    for metal, block, ingredient in METAL_DOORS:
        metal_door("%s_door" % metal, ["minecraft:copper_door", "minecraft:" + ingredient],
                   lambda g, mt=metal: "Iron Door (Copper Style)" if mt == "iron" else "%s Door" % title(mt, g))

    # The coloured textures are made in the game from the door textures in use: wood and iron recoloured with
    # dyes, the copper door repainted with each metal block's own colours.
    write(os.path.join(ROOT, "assets", "minecraft", "atlases", "blocks.json"), {"sources": [
        {"type": NS + ":door_colours", "prefix": "block/",
         "textures": ["minecraft:block/%s_door_%s" % (w, p) for w in DOOR_WOODS for p in ("top", "bottom")]},
        {"type": NS + ":door_colours", "prefix": "block/", "metal": True, "textures": ["minecraft:block/iron_door_top", "minecraft:block/iron_door_bottom"]},
        {"type": NS + ":metal_doors", "prefix": "block/", "textures": ["minecraft:block/copper_door_top", "minecraft:block/copper_door_bottom"],
         "metals": ["minecraft:block/" + b for _, b, _ in METAL_DOORS]}]})
    write(os.path.join(ROOT, "assets", "minecraft", "atlases", "items.json"), {"sources": [
        {"type": NS + ":door_colours", "prefix": "item/", "textures": ["minecraft:item/%s_door" % w for w in DOOR_WOODS]},
        {"type": NS + ":door_colours", "prefix": "item/", "metal": True, "textures": ["minecraft:item/iron_door"]},
        {"type": NS + ":metal_doors", "prefix": "item/", "textures": ["minecraft:item/copper_door"],
         "metals": ["minecraft:block/" + b for _, b, _ in METAL_DOORS]}]})


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
                            "wooden_doors", "doors", "mineable/pickaxe", "mineable/shovel", "mineable/axe", "mineable/hoe",
                            "needs_stone_tool", "needs_iron_tool", "needs_diamond_tool"]}
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
            name = "%s_%s" % (mat, suffix[7:]) if suffix.startswith("pillar_") else stem(mat) + "_" + suffix
            # A natural block's building shapes are made like any building block's.
            BUILDERS[suffix](mat, "building" if kind == "nature" and suffix not in NATURE_SHAPES else kind, name)
            loot(name, suffix, silk=kind == "glass" or mat == "ice" or mat == "packed_ice" or mat == "blue_ice")
            stonecutting(mat, name, cut)
            if suffix in tag_of:
                tags[tag_of[suffix]].append(NS + ":" + name)
            vid = "minecraft:" + mat
            if kind in ("terracotta", "concrete", "glazed") or (kind in ("nature", "building") and vid in VANILLA_PICKAXE):
                tags["mineable/pickaxe"].append(NS + ":" + name)
            for tier, blocks in VANILLA_NEEDS.items():
                if vid in blocks:
                    tags["needs_%s_tool" % tier].append(NS + ":" + name)
            if kind == "powder" or (kind == "nature" and vid in VANILLA_SHOVEL):
                tags["mineable/shovel"].append(NS + ":" + name)
            if mat in ("moss_block", "pale_moss_block"):
                tags["mineable/hoe"].append(NS + ":" + name)
            if suffix.startswith("pillar_"):
                add_name(name, lambda g, mt=mat, sn=shape_name: title(mt, g) + " " + sn)
            else:
                add_name(name, lambda g, mt=mat, sn=shape_name: title(stem(mt), g) + " " + sn)
            count += 1
    thin_logs(add_name, tags)
    plants(add_name)
    coral(add_name)
    doors(add_name, tags)
    cube_icon()

    for tag, values in tags.items():
        write(os.path.join(DATA, "minecraft", "tags", "block", tag + ".json"), {"replace": False, "values": values})
    for tag in ["stairs", "slabs", "walls", "fences", "fence_gates", "trapdoors", "buttons", "wooden_doors", "doors"]:
        write(os.path.join(DATA, "minecraft", "tags", "item", tag + ".json"), {"replace": False, "values": tags[tag]})

    for tab, name in TABS:
        us["itemGroup.%s.%s" % (NS, tab)] = gb["itemGroup.%s.%s" % (NS, tab)] = "Cubework: " + name
    us["item.%s.cube" % NS] = gb["item.%s.cube" % NS] = "Cubework"
    write(os.path.join(ASSETS, "lang", "en_us.json"), us)
    write(os.path.join(ASSETS, "lang", "en_gb.json"), gb)
    print("shapes:", count, "names:", len(us))


if __name__ == "__main__":
    main()
