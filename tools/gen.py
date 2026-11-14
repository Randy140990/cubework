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
import math
import os
import shutil

HERE = os.path.dirname(__file__)
import argparse

_args = argparse.ArgumentParser(description="Generates Cubework's resources for one Minecraft version.")
_args.add_argument("--out", default=os.path.join(HERE, "..", "build", "generated"), help="resources folder to write into")
_args.add_argument("--mc", default="1.21.11", help="Minecraft version the resources are for")
_args.add_argument("--blocks", help="file listing the vanilla block ids in that version (blocks missing from it are left out)")
ARGS = _args.parse_args()
MC = tuple(int(p) for p in ARGS.mc.split("."))
VANILLA_BLOCKS = set(open(ARGS.blocks).read().split()) if ARGS.blocks else None


def exists(block):
    """Whether a vanilla block is in this Minecraft version (pale oak and resin came in 1.21.4, bushes in 1.21.5)."""
    return VANILLA_BLOCKS is None or block in VANILLA_BLOCKS


ROOT = ARGS.out
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
    if mat in PATTERN_OF:
        p, src = PATTERN_OF[mat]
        return p + "_" + STEMS.get(src, src)
    return STEMS.get(mat, mat)


def is_building(mat):
    return mat in BUILDING_SET


def full_model(mat):
    """The vanilla full block's model (waxed copper uses unwaxed copper's; patterns their own)."""
    if mat in PATTERN_OF:
        return m(mat)
    return "minecraft:block/" + (mat[6:] if mat.startswith("waxed_") else mat)


def vanilla_of(mat):
    """The vanilla block a material is made from (a pattern's is the block it's drawn on)."""
    return PATTERN_OF[mat][1] if mat in PATTERN_OF else mat


# Patterns laid over a block's own texture (made in the game, see client/Patterns.java), as Material.java.
PATTERNS = [("tiled", "Tiled"), ("checkered", "Checkered"), ("herringbone", "Herringbone"), ("bordered", "Bordered")]
PATTERN_SOURCES = ["stone", "smooth_stone", "cobblestone", "stone_bricks", "mossy_stone_bricks", "granite", "polished_granite", "diorite",
                   "polished_diorite", "andesite", "polished_andesite", "cobbled_deepslate", "polished_deepslate", "deepslate_bricks",
                   "deepslate_tiles", "tuff", "polished_tuff", "tuff_bricks", "calcite", "dripstone_block", "bricks", "mud_bricks", "packed_mud",
                   "resin_bricks", "smooth_sandstone", "smooth_red_sandstone", "prismarine", "prismarine_bricks", "dark_prismarine",
                   "nether_bricks", "red_nether_bricks", "smooth_basalt", "polished_blackstone", "polished_blackstone_bricks", "end_stone",
                   "end_stone_bricks", "purpur_block", "quartz_bricks", "smooth_quartz", "obsidian", "iron_block", "gold_block", "emerald_block",
                   "lapis_block", "diamond_block", "netherite_block", "amethyst_block", "terracotta"] + \
                  ["%s_terracotta" % c for c in COLOURS] + ["%s_concrete" % c for c in COLOURS] + ["%s_wool" % c for c in COLOURS]
PATTERN_OF = {p + "_" + src: (p, src) for src in PATTERN_SOURCES for p, _ in PATTERNS}


# (id, kind), in the same order as Material.java
MATERIALS = [(c + "_wool", "wool") for c in COLOURS] + [("terracotta", "terracotta")] + \
            [(c + "_terracotta", "terracotta") for c in COLOURS] + [(c + "_concrete", "concrete") for c in COLOURS] + \
            [(c + "_concrete_powder", "powder") for c in COLOURS] + [(c + "_glazed_terracotta", "glazed") for c in COLOURS] + \
            [("glass", "glass"), ("tinted_glass", "glass")] + [(c + "_stained_glass", "glass") for c in COLOURS] + \
            [(n, "nature") for n in NATURE] + [(b, "building") for b in BUILDING if b not in NATURE] + \
            [(p + "_" + src, "pattern") for src in PATTERN_SOURCES for p, _ in PATTERNS]

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
SHAPES = [("stairs", "Stairs", 1), ("steps_4", "Stairs (4 Steps)", 1), ("steps_6", "Stairs (6 Steps)", 1), ("steps_8", "Stairs (8 Steps)", 1), ("stairs_2", "Stairs (1 in 2)", 2), ("stairs_3", "Stairs (1 in 3)", 2), ("stairs_4", "Stairs (1 in 4)", 2), ("stair_curve", "Curved Stairs", 1),
          ("slab", "Slab", 2), ("quarter_slab", "Quarter Slab", 4), ("vertical_slab", "Vertical Slab", 2),
          ("slope", "Slope", 2), ("slope_2", "Slope (1 in 2)", 2), ("slope_3", "Slope (1 in 3)", 2), ("gentle_slope", "Slope (1 in 4)", 2), ("layer", "Layer", 8), ("vertical_layer", "Vertical Layer", 8),
          ("carpet", "Carpet", 4), ("panel", "Panel", 4), ("post", "Post", 4), ("pillar_12", "(12 px)", 1), ("pillar_8", "(8 px)", 2),
          ("pillar_4", "(4 px)", 4), ("round_column_16", "Round Column (16 px)", 1), ("round_column_12", "Round Column (12 px)", 2),
          ("round_column_8", "Round Column (8 px)", 4), ("round_column_4", "Round Column (4 px)", 8), ("corner_post", "Corner Post", 4), ("arch", "Arch", 1), ("arch_corner", "Arch Corner", 1),
          ("round_corner", "Round Corner", 1), ("round_corner_inner", "Inner Round Corner", 4),
          ("wall", "Wall", 1), ("fence", "Fence", 1), ("fence_gate", "Fence Gate", 1),
          ("trapdoor", "Trapdoor", 1), ("button", "Button", 4), ("pressure_plate", "Pressure Plate", 2),
          ("trim_2", "Trim (2 px)", 16), ("trim_4", "Trim (4 px)", 8), ("trim_8", "Trim (8 px)", 4)] + \
         [("window_frame_%d" % d, "Window Frame (%d px)" % d, c) for d, c in ((4, 4), (8, 2), (12, 1), (16, 1))] + \
         [("diamond_window_frame_%d" % d, "Diamond Window Frame (%d px)" % d, c) for d, c in ((4, 4), (8, 2), (12, 1), (16, 1))] + \
         [("round_window_frame_%d" % d, "Round Window Frame (%d px)" % d, c) for d, c in ((4, 4), (8, 2), (12, 1), (16, 1))]
NATURE_SHAPES = {"stairs", "slab", "quarter_slab", "vertical_slab", "slope", "slope_2", "slope_3", "gentle_slope", "layer", "vertical_layer", "carpet"}
# Natural blocks that are building blocks too (stone, sandstone...) keep the building versions of these.
NEW_NATURE = {"stairs", "quarter_slab", "slope_2", "slope_3", "gentle_slope"}
PATTERN_SHAPES = {"stairs", "slab", "quarter_slab", "vertical_slab", "slope", "layer", "vertical_layer", "carpet", "panel"}
COLUMN_WIDTHS = [16, 12, 8, 4]
STEP_COUNTS = [4, 6, 8]
# Gentle slopes, quarter slabs, arches and window frames: wool, terracotta and concrete for now.
CASTLE_SHAPES = {"quarter_slab", "slope_2", "slope_3", "gentle_slope", "arch", "arch_corner"} | {"%swindow_frame_%d" % (p, d) for p in ("", "diamond_") for d in (4, 8, 12, 16)}
WINDOW_DEPTHS = [4, 8, 12, 16]
STAIRS_PITCHES = [2, 3, 4]
TRIM_WIDTHS = [2, 4, 8]

LOGS = ["oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log",
        "pale_oak_log", "crimson_stem", "warped_stem", "mushroom_stem"]
LOG_WIDTHS = [(16, 1), (12, 1), (8, 2), (4, 4)]  # width, how many one log makes
PILLAR_WIDTHS = [12, 8, 4]

LOW_PLANTS = ["short_grass", "fern", "short_dry_grass", "tall_dry_grass", "bush"]
LOW_SIZES = [50, 75, 125, 150, 175]
PLANTS = ["red_mushroom", "brown_mushroom", "crimson_fungus", "warped_fungus", "dandelion", "poppy", "blue_orchid", "allium",
          "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower",
          "lily_of_the_valley", "torchflower", "wither_rose", "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
          "acacia_sapling", "dark_oak_sapling", "cherry_sapling", "pale_oak_sapling", "dead_bush", "cactus_flower", "closed_eyeblossom",
          "open_eyeblossom", "firefly_bush", "crimson_roots", "warped_roots"]
# Two-block plants, drawn whole inside one block (half size fits; three-quarter size reaches half a block above).
TALL_PLANTS = ["tall_grass", "large_fern", "sunflower", "lilac", "rose_bush", "peony"]
TALL_SIZES = [50, 75]
TALL_GRASSY = {"tall_grass", "large_fern"}
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

TABS = [("colours", "Colours"), ("building", "Building"), ("nature", "Nature"), ("plants", "Plants"), ("doors", "Doors"), ("tables", "Tables"),
        ("patterns", "Patterns"), ("carpets", "Carpets")]

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
    if kind == "pattern":
        return suffix in PATTERN_SHAPES
    if suffix.startswith("pillar_"):
        return is_building(mat)
    if is_building(mat):
        return True
    if kind == "nature":
        return suffix in NATURE_SHAPES
    return not (suffix == "carpet" and kind == "wool")


def textures_of(mat):
    if mat in PATTERN_OF:
        p, src = PATTERN_OF[mat]
        t = TEXTURES.get(src, (src, src, src, None))
        at = NS + ":block/pattern/" + p + "/"
        return {"top": at + t[0], "side": at + t[1], "bottom": at + t[2], "overlay": None}
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


# Vanilla textures renamed in later versions.
TEXTURE_RENAMES = {"minecraft:block/quartz_pillar": "minecraft:block/quartz_pillar_side",
                   "minecraft:block/purpur_pillar": "minecraft:block/purpur_pillar_side"} if MC >= (26, 2) else {}


def model(name, parent, textures):
    textures = {k: TEXTURE_RENAMES.get(v, v) for k, v in textures.items()}
    write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": parent, "textures": textures})


def blockstate(name, obj):
    write(os.path.join(ASSETS, "blockstates", name + ".json"), obj)


def item(name, model_id, tinted=False):
    """The item's look: an item model definition from 1.21.4, before that an item model (tints are set in code)."""
    if MC >= (1, 21, 4):
        mdl = {"type": "minecraft:model", "model": model_id}
        if tinted:
            mdl["tints"] = GRASS_TINT
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": mdl})
    elif model_id != NS + ":item/" + name:
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": model_id})


def ing(x):
    """A recipe ingredient (an item id; 1.21.1 wants it as an object)."""
    return x if MC >= (1, 21, 2) else {"item": x}


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
    # Stairs (rising east, like vanilla's), in boxes whose side textures hang from their own tops, so a
    # grass edge runs along each step.
    template("template_stairs" + sfx, [([0, 0, 0], [8, half, 16]), ([8, 0, 0], [16, top, 16])], top, anchored, grass, STAIRS_DISPLAY)
    template("template_inner_stairs" + sfx, [([8, 0, 0], [16, top, 16]), ([0, 0, 8], [8, top, 16]), ([0, 0, 0], [8, half, 8])], top, anchored, grass)
    template("template_outer_stairs" + sfx, [([8, 0, 8], [16, top, 16]), ([0, 0, 0], [16, half, 8]), ([0, 0, 8], [8, half, 16])], top, anchored, grass)
    template("template_quarter_slab" + sfx, [([0, 0, 0], [16, 4, 16])], 4, anchored, grass)
    template("template_quarter_slab_top" + sfx, [([0, top - 4, 0], [16, top, 16])], top, anchored, grass)
    for n in (2, 3, 4):
        step = top / n
        template("template_slope_%d_item%s" % (n, sfx), [([2 * k, 0, 0], [2 * k + 2, round(step + (k + 1) * step / 8, 4), 16]) for k in range(8)],
                 round(2 * step, 4), anchored, grass, STAIRS_DISPLAY)
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
    # Two-block plants at a size: the bottom half's cross under the top half's, in one block.
    for size in TALL_SIZES:
        s = size / 100
        lo, hi, half = round(8 - 7.2 * s, 3), round(8 + 7.2 * s, 3), round(16 * s, 3)
        rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
        for tinted in (False, True):
            for head in (False, True):
                def face(tex):
                    f = {"uv": [0, 0, 16, 16], "texture": tex}
                    if tinted: f["tintindex"] = 0
                    return f
                elements = []
                for tex, y0, y1 in (("#bottom", 0, half), ("#top", half, round(2 * half, 3))):
                    elements.append({"from": [lo, y0, 8], "to": [hi, y1, 8], "rotation": rot, "shade": False, "faces": {"north": face(tex), "south": face(tex)}})
                    elements.append({"from": [8, y0, lo], "to": [8, y1, hi], "rotation": rot, "shade": False, "faces": {"west": face(tex), "east": face(tex)}})
                if head:
                    # The sunflower's head, tilted towards the east as vanilla's.
                    hx, hz0, hz1 = round(8 + 1.6 * s, 3), round(8 - 7 * s, 3), round(8 + 7 * s, 3)
                    hy0, hy1 = round(half - s, 3), round(half + 15 * s, 3)
                    elements.append({"from": [hx, hy0, hz0], "to": [hx, hy1, hz1], "rotation": {"origin": [8, round(half + 8 * s, 3), 8], "axis": "z", "angle": 22.5, "rescale": True},
                                     "shade": True, "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#back"}, "east": {"uv": [0, 0, 16, 16], "texture": "#front"}}})
                name = "template_%sdouble_cross_%d%s" % ("tinted_" if tinted else "", size, "_head" if head else "")
                write(os.path.join(ASSETS, "models", "block", name + ".json"),
                      {"ambientocclusion": False, "textures": {"particle": "#bottom"}, "elements": elements})


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
    # Slopes over 2, 3 and 4 blocks in the hand: their second step, rising east like stairs.
    for n in (2, 3, 4):
        step = 16 / n
        template("template_slope_%d_item" % n, [([2 * k, 0, 0], [2 * k + 2, round(step + (k + 1) * step / 8, 4), 16]) for k in range(8)],
                 round(2 * step, 4), display=STAIRS_DISPLAY)
    for d in WINDOW_DEPTHS:
        for piece in WINDOW_PIECES:
            template("template_window_%d_%s" % (d, piece), mask_boxes(window_piece_mask(piece), 0, d))
        for diamond in (False, True):
            pieces = list(WINDOW_RECTS) + (["tri_bl", "tri_br", "tri_tl", "tri_tr"] if diamond else [])
            boxes = [b for p in pieces for b in mask_boxes(window_piece_mask(p), 0, d)]
            template("template_%swindow_%d_inventory" % ("diamond_" if diamond else "", d), boxes)


def pitch_stairs_heights(n, step):
    """Stairs climbing one block over n: the low (front) and high (back) step of each block, in pixels (as PitchStairsBlock)."""
    base = (step - 1) * 16 / n
    return round(base + 8 / n, 4), round(base + 16 / n, 4)


def new_shape_templates():
    """Stairs over 2, 3 and 4 blocks (rising east, like vanilla's), trims, and round window frames in the hand."""
    for n in STAIRS_PITCHES:
        for step in range(1, n + 1):
            lo, hi = pitch_stairs_heights(n, step)
            template("template_stairs_%d_%d" % (n, step), [([0, 0, 0], [8, lo, 16]), ([8, 0, 0], [16, hi, 16])], display=STAIRS_DISPLAY)
            template("template_stairs_%d_%d_top" % (n, step), [([0, round(16 - lo, 4), 0], [8, 16, 16]), ([8, round(16 - hi, 4), 0], [16, 16, 16])])
    for n in STEP_COUNTS:
        # Stairs with n smaller steps, rising east like vanilla's: straight, inner corner (high but for the
        # north-west quarter) and outer corner (high in the south-east quarter), turned by vanilla's blockstate.
        w = 16 / n
        r = lambda v: round(v, 4)
        template("template_steps_%d" % n, [([r(k * w), 0, 0], [r((k + 1) * w), r((k + 1) * w), 16]) for k in range(n)], display=STAIRS_DISPLAY)
        inner = []
        for k in range(n):
            inner.append(([r(k * w), r(k * w), 0], [16, r((k + 1) * w), 16]))
            if k > 0:
                inner.append(([0, r(k * w), r(k * w)], [r(k * w), r((k + 1) * w), 16]))
        template("template_steps_%d_inner" % n, inner)
        template("template_steps_%d_outer" % n, [([r(k * w), r(k * w), r(k * w)], [16, r((k + 1) * w), 16]) for k in range(n)])
    for w in TRIM_WIDTHS:
        # Along the bottom north edge, and up the north-west corner; turned in the blockstate to the other ten.
        template("template_trim_%d" % w, [([0, 0, 0], [16, w, w])])
        template("template_trim_%d_upright" % w, [([0, 0, 0], [w, 16, w])])
        template("template_trim_%d_item" % w, [([0, 8 - w / 2, 8 - w / 2], [16, 8 + w / 2, 8 + w / 2])])
    # Curved Stairs in the hand: a stair (rising east) with the S-curve cut under its back, as the middle
    # stair of a three-stair flight (as StairCurveGeometry.curve).
    def stair_curve_at(w, X):
        if X <= 0: return 0.0
        thin, flare = 0.25, 0.35
        length = w - thin
        waves = max(1, round(length / 3.5))
        period = length / waves
        swing = min(0.45, 0.8 * period / (2 * math.pi))
        s = max(0.0, X - thin) / period
        raw = X - (thin + swing - swing * math.cos(2 * math.pi * s))
        hh = max(flare - abs(raw), 0.0) / flare
        return max(0.0, min(max(raw, 0.0) + hh * hh * flare * 0.25, X - 0.2))
    def under(x):
        return max(0.0, stair_curve_at(3, 1 + x) - 1.0)
    curve = [[(y + 1) / 16 > max(under(x / 16), under((x + 1) / 16)) and y / 16 < (0.5 if x < 8 else 1.0) for y in range(16)] for x in range(16)]
    template("template_stair_curve", mask_boxes(curve, 0, 16), display=STAIRS_DISPLAY)
    for d in WINDOW_DEPTHS:
        # A porthole: the placed ones are drawn smooth in code (RoundWindowModel).
        mask = [[((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) >= 16 for y in range(16)] for x in range(16)]
        template("template_round_window_%d_inventory" % d, mask_boxes(mask, 0, d))


def round_templates():
    """Item models for the round shapes (placed ones are drawn smooth in code), and the corner post."""
    for d in COLUMN_WIDTHS:
        h = d / 2
        a, b = round(h * 0.414, 3), round(h * 0.707, 3)
        boxes = [([8 - h, 0, 8 - a], [8 + h, 16, 8 + a]), ([8 - a, 0, 8 - h], [8 + a, 16, 8 + h]), ([8 - b, 0, 8 - b], [8 + b, 16, 8 + b])]
        template("template_round_column_%d" % d, [([round(f[0], 3), f[1], round(f[2], 3)], [round(t[0], 3), t[1], round(t[2], 3)]) for f, t in boxes])
    # Facing north: the north-east corner (as Corners.java).
    for inner in (False, True):
        boxes = []
        for z in range(16):
            start = None
            for x in range(17):
                if x < 16:
                    px, pz = (x + 0.5) / 16, (z + 0.5) / 16
                    solid = ((px - 0) ** 2 + (pz - 1) ** 2 >= 1) if inner else ((px - 1) ** 2 + pz ** 2 <= 1)
                else:
                    solid = False
                if solid and start is None: start = x
                if not solid and start is not None:
                    boxes.append(([start, 0, z], [x, 16, z + 1]))
                    start = None
        template("template_round_corner%s" % ("_inner" if inner else ""), boxes)
    template("template_corner_post", [([8, 0, 0], [16, 16, 8])])


def templates():
    round_templates()
    castle_templates()
    new_shape_templates()
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
    if kind == "nature":
        t, sfx = shape_textures(mat, kind)
        model(name, m("template_stairs" + sfx), t)
        model(name + "_inner", m("template_inner_stairs" + sfx), t)
        model(name + "_outer", m("template_outer_stairs" + sfx), t)
        blockstate(name, from_template("stairs", {"straight": m(name), "inner": m(name + "_inner"), "outer": m(name + "_outer")}))
        item(name, m(name), mat == "grass_block")
        return
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
    # Laid facing the player and turned by an empty hand, as glazed terracotta is turned.
    blockstate(name, {"variants": {"facing=" + d: {"model": m(name), "y": {"south": 0, "west": 90, "north": 180, "east": 270}[d]} for d in HORIZONTAL}})
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
    t, sfx = shape_textures(mat, kind)
    model(name, m("template_quarter_slab" + sfx), t)
    model(name + "_top", m("template_quarter_slab_top" + sfx), t)
    blockstate(name, {"variants": {"half=bottom": {"model": m(name)}, "half=top": {"model": m(name + "_top")}}})
    item(name, m(name), mat == "grass_block")


def gentle_slope(mat, kind, name, n=4):
    # Placed slopes are built in code (SlopeModel); this one is for the item.
    t, sfx = shape_textures(mat, kind)
    model(name + "_item", m("template_slope_%d_item%s" % (n, sfx)), t)
    item(name, m(name + "_item"), mat == "grass_block")


def step_stairs(mat, kind, name, n):
    t = faces(mat)
    model(name, m("template_steps_%d" % n), t)
    model(name + "_inner", m("template_steps_%d_inner" % n), t)
    model(name + "_outer", m("template_steps_%d_outer" % n), t)
    blockstate(name, from_template("stairs", {"straight": m(name), "inner": m(name + "_inner"), "outer": m(name + "_outer")}))
    item(name, m(name))


def round_column(mat, kind, name, d):
    # Placed columns are drawn round in code (RoundModel); this one is for the item.
    model(name + "_item", m("template_round_column_%d" % d), faces(mat))
    item(name, m(name + "_item"))


def round_corner(mat, kind, name, inner=False):
    # Placed corners are drawn round in code (RoundModel); this stepped one is for the item.
    model(name + "_item", m("template_round_corner" + ("_inner" if inner else "")), faces(mat))
    item(name, m(name + "_item"))


def corner_post(mat, kind, name):
    model(name, m("template_corner_post"), faces(mat))
    blockstate(name, {"variants": {"facing=%s,waterlogged=%s" % (d, w): dict({"model": m(name)}, **({"y": Y_FROM_NORTH[d]} if Y_FROM_NORTH[d] else {}))
                                   for d in HORIZONTAL for w in ("false", "true")}})
    item(name, m(name))


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
    # Upright on a wall, or tipped flat onto the floor (x 90: its top towards the north) or against the
    # ceiling (x 270), as WindowFrameBlock's hit box.
    for face, x in (("wall", 0), ("floor", 90), ("ceiling", 270)):
        for f in HORIZONTAL:
            def apply(p, f=f, x=x):
                a = {"model": m("%s_%s" % (name, p)), "y": Y_FROM_NORTH[f], "uvlock": True}
                if x:
                    a["x"] = x
                return a
            for p in pieces:
                if p.startswith("bar_"):
                    key = {"bar_left": "left", "bar_right": "right", "bar_bottom": "down", "bar_top": "up"}[p]
                    parts.append({"when": {"face": face, "facing": f, key: "false"}, "apply": apply(p)})
                elif p.startswith("corner_"):
                    v, h = side[p[-2]], side[p[-1]]
                    parts.append({"when": {"OR": [{"face": face, "facing": f, v: "false"}, {"face": face, "facing": f, h: "false"}]}, "apply": apply(p)})
                else:
                    v, h = side[p[-2]], side[p[-1]]
                    parts.append({"when": {"face": face, "facing": f, v: "false", h: "false"}, "apply": apply(p)})
    blockstate(name, {"multipart": parts})
    item(name, m(name + "_inventory"))


def pitch_stairs(mat, kind, name, n):
    t, _ = shape_textures(mat, kind)
    for step in range(1, n + 1):
        model("%s_s%d" % (name, step), m("template_stairs_%d_%d" % (n, step)), t)
        model("%s_s%d_top" % (name, step), m("template_stairs_%d_%d_top" % (n, step)), t)
    y = {"east": 0, "south": 90, "west": 180, "north": 270}
    v = {}
    for f in HORIZONTAL:
        for half in ("bottom", "top"):
            for step in range(1, n + 1):
                mdl = {"model": m("%s_s%d%s" % (name, step, "_top" if half == "top" else ""))}
                if y[f]:
                    mdl["y"] = y[f]
                mdl["uvlock"] = True
                v["facing=%s,half=%s,step=%d" % (f, half, step)] = mdl
    blockstate(name, {"variants": v})
    item(name, m("%s_s%d" % (name, min(2, n))))


# Each edge: (model, x, y) from the strip along the bottom north edge or up the north-west corner.
TRIM_EDGES = {"down_north": ("", 0, 0), "down_east": ("", 0, 90), "down_south": ("", 0, 180), "down_west": ("", 0, 270),
              "up_south": ("", 180, 0), "up_west": ("", 180, 90), "up_north": ("", 180, 180), "up_east": ("", 180, 270),
              "north_west": ("_upright", 0, 0), "north_east": ("_upright", 0, 90), "south_east": ("_upright", 0, 180),
              "south_west": ("_upright", 0, 270)}


def trim(mat, kind, name, w):
    t, _ = shape_textures(mat, kind)
    model(name, m("template_trim_%d" % w), t)
    model(name + "_upright", m("template_trim_%d_upright" % w), t)
    model(name + "_item", m("template_trim_%d_item" % w), t)
    v = {}
    for edge, (sfx, x, y) in TRIM_EDGES.items():
        mdl = {"model": m(name + sfx)}
        if x: mdl["x"] = x
        if y: mdl["y"] = y
        mdl["uvlock"] = True
        v["edge=" + edge] = mdl
    blockstate(name, {"variants": v})
    item(name, m(name + "_item"))


def stair_curve(mat, kind, name):
    # Placed pieces are drawn smooth in code (StairCurveModel); this stepped one is for the item.
    t, _ = shape_textures(mat, kind)
    model(name + "_item", m("template_stair_curve"), t)
    item(name, m(name + "_item"))


def round_window(mat, kind, name, depth):
    # Placed frames are drawn smooth in code (RoundWindowModel); this porthole is for the item.
    t, _ = shape_textures(mat, kind)
    model(name + "_inventory", m("template_round_window_%d_inventory" % depth), t)
    item(name, m(name + "_inventory"))


for _d in WINDOW_DEPTHS:
    for _diamond in (False, True):
        globals()["window_%s%d" % ("d" if _diamond else "s", _d)] = (lambda d, dm: lambda mat, kind, name: window_frame(mat, kind, name, d, dm))(_d, _diamond)


BUILDERS = {"stairs": stairs, "slab": slab, "vertical_slab": vertical_slab, "slope": slope, "layer": layer,
            "vertical_layer": vertical_layer, "carpet": carpet, "panel": panel, "post": post, "wall": wall,
            "fence": fence, "fence_gate": fence_gate, "trapdoor": trapdoor, "button": button, "pressure_plate": pressure_plate,
            "quarter_slab": quarter_slab, "gentle_slope": gentle_slope, "arch": arch, "arch_corner": arch_corner,
            "slope_2": lambda mat, kind, name: gentle_slope(mat, kind, name, 2), "slope_3": lambda mat, kind, name: gentle_slope(mat, kind, name, 3),
            "corner_post": corner_post, "round_corner": round_corner,
            "round_corner_inner": lambda mat, kind, name: round_corner(mat, kind, name, True)}
for _d in COLUMN_WIDTHS:
    BUILDERS["round_column_%d" % _d] = (lambda d: lambda mat, kind, name: round_column(mat, kind, name, d))(_d)
for _w in PILLAR_WIDTHS:
    BUILDERS["pillar_%d" % _w] = (lambda w: lambda mat, kind, name: pillar(mat, kind, name, w))(_w)
BUILDERS["stair_curve"] = stair_curve
for _n in STAIRS_PITCHES:
    BUILDERS["stairs_%d" % _n] = (lambda n: lambda mat, kind, name: pitch_stairs(mat, kind, name, n))(_n)
for _n in STEP_COUNTS:
    BUILDERS["steps_%d" % _n] = (lambda n: lambda mat, kind, name: step_stairs(mat, kind, name, n))(_n)
for _w in TRIM_WIDTHS:
    BUILDERS["trim_%d" % _w] = (lambda w: lambda mat, kind, name: trim(mat, kind, name, w))(_w)
for _d in WINDOW_DEPTHS:
    BUILDERS["round_window_frame_%d" % _d] = (lambda d: lambda mat, kind, name: round_window(mat, kind, name, d))(_d)
for _d in WINDOW_DEPTHS:
    BUILDERS["window_frame_%d" % _d] = globals()["window_s%d" % _d]
    BUILDERS["diamond_window_frame_%d" % _d] = globals()["window_d%d" % _d]


# ---------- loot and recipes ----------

SILK = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]


def loot(name, suffix=None, silk=False):
    block_id = NS + ":" + name
    if MC >= (26, 3):
        return loot_26_3(name, block_id, suffix, silk)
    entry = {"type": "minecraft:item", "name": NS + ":" + name, "functions": []}
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


def loot_26_3(name, block_id, suffix, silk):
    """26.3's loot tables: modifiers and conditions name their type, and silk touch is a predicate of its own."""
    mods = []
    if suffix in ("slab", "vertical_slab"):
        state = {"type": "double"} if suffix == "slab" else {"double": "true"}
        mods.append({"type": "minecraft:set_count", "condition": {"type": "minecraft:match_block", "blocks": block_id, "state": state}, "count": 2})
    if suffix in ("layer", "vertical_layer"):
        for n in range(2, 8):
            mods.append({"type": "minecraft:set_count", "condition": {"type": "minecraft:match_block", "blocks": block_id,
                                                                      "state": {"layers": str(n)}}, "count": n})
    mods.append({"type": "minecraft:explosion_decay"})
    pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": block_id, "modifier": mods}]}
    if silk:
        pool["condition"] = "minecraft:tool/can_silk_touch"
    write(os.path.join(DATA, NS, "loot_table", "blocks", name + ".json"),
          {"type": "minecraft:block", "pools": [pool], "random_sequence": NS + ":blocks/" + name})


def door_loot(name):
    if MC >= (26, 3):
        block_id = NS + ":" + name
        write(os.path.join(DATA, NS, "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block", "random_sequence": NS + ":blocks/" + name,
            "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                       "entries": [{"type": "minecraft:item", "name": block_id,
                                    "condition": {"type": "minecraft:match_block", "blocks": block_id, "state": {"half": "lower"}}}]}]})
        return
    write(os.path.join(DATA, NS, "loot_table", "blocks", name + ".json"), {
        "type": "minecraft:block", "random_sequence": NS + ":blocks/" + name,
        "pools": [{"bonus_rolls": 0.0, "rolls": 1.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
                   "entries": [{"type": "minecraft:item", "name": NS + ":" + name, "conditions": [
                       {"condition": "minecraft:block_state_property", "block": NS + ":" + name, "properties": {"half": "lower"}}]}]}]})


def stonecutting(source, name, count, result_ns=NS):
    """A stonecutter recipe; patterns (and their shapes) are cut from Cubework's patterned blocks."""
    src_id = (NS if source in PATTERN_OF else "minecraft") + ":" + source
    write(os.path.join(DATA, NS, "recipe", name + "_from_" + source + "_stonecutting.json"),
          {"type": "minecraft:stonecutting", "ingredient": ing(src_id), "result": {"id": result_ns + ":" + name, "count": count}})


def pattern_blocks(add_name, tags):
    """The patterned full blocks: cut from the block in the stonecutter (and back), behaving like it."""
    for src in PATTERN_SOURCES:
        if not exists(src):
            continue
        for p, english in PATTERNS:
            name = p + "_" + src
            t = textures_of(name)
            model(name, "minecraft:block/cube", {"particle": t["side"], "down": t["bottom"], "up": t["top"], "north": t["side"],
                                                 "south": t["side"], "east": t["side"], "west": t["side"]})
            blockstate(name, {"variants": {"": {"model": m(name)}}})
            item(name, m(name))
            loot(name)
            stonecutting(src, name, 1)
            stonecutting(name, src, 1, "minecraft")
            vid = "minecraft:" + src
            if vid in VANILLA_PICKAXE or src.endswith("_terracotta") or src.endswith("_concrete") or src == "terracotta":
                tags["mineable/pickaxe"].append(NS + ":" + name)
            for tier, blocks in VANILLA_NEEDS.items():
                if vid in blocks:
                    tags["needs_%s_tool" % tier].append(NS + ":" + name)
            add_name(name, lambda g, e=english, s=src: e + " " + title(s, g))


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


def round_logs(add_name, tags):
    """Round logs and stems: drawn in code when placed (RoundModel.buildLog); item models from the round column templates."""
    for log in LOGS:
        for kind in ([log, "stripped_" + log] if log != "mushroom_stem" else [log]):
            side = "minecraft:block/" + kind
            end = "minecraft:block/mushroom_block_inside" if kind == "mushroom_stem" else "minecraft:block/" + kind + "_top"
            for w, cut in LOG_WIDTHS:
                name = "round_%s_%d" % (kind, w)
                model(name + "_item", m("template_round_column_%d" % w), {"side": side, "top": end, "bottom": end})
                item(name, m(name + "_item"))
                loot(name)
                stonecutting(kind, name, cut)
                tags["mineable/axe"].append(NS + ":" + name)
                add_name(name, lambda g, k=kind, w=w: "Round %s (%d px)" % (title(k, g), w))


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
    for plant in TALL_PLANTS:
        tinted = plant in TALL_GRASSY
        head = plant == "sunflower"
        for size in TALL_SIZES:
            name = "%s_%d" % (plant, size)
            t = {"bottom": "minecraft:block/%s_bottom" % plant, "top": "minecraft:block/%s_top" % plant}
            if head:
                t.update(front="minecraft:block/sunflower_front", back="minecraft:block/sunflower_back")
            model(name, m("template_%sdouble_cross_%d%s" % ("tinted_" if tinted else "", size, "_head" if head else "")), t)
            blockstate(name, {"variants": {"": {"model": m(name)}}})
            write(os.path.join(ASSETS, "models", "item", name + ".json"),
                  {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:block/" + ("sunflower_front" if head else plant + "_top")}})
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
                   "ingredients": [ing("minecraft:%s_door" % wood), ing("minecraft:%s_dye" % colour)], "result": {"id": NS + ":" + name, "count": 1}})
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
              {"type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": [ing(r) for r in recipe], "result": {"id": NS + ":" + name, "count": 1}})
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

    # Trapdoors: each wood's trapdoor and the iron one in the 16 colours (keeping their patterns), and the
    # copper trapdoor's pattern in metals and gems. Laid like vanilla's wooden trapdoors (the pattern turns
    # with the way they face).
    def trapdoor_models(name, english, recipe, wooden, metal):
        t = {"texture": NS + ":block/" + name}
        for part in ("bottom", "top", "open"):
            model(name + "_" + part, "minecraft:block/template_orientable_trapdoor_" + part, t)
        variants = {}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            variants["facing=%s,half=bottom,open=false" % f] = {"model": m(name + "_bottom"), "y": y} if y else {"model": m(name + "_bottom")}
            variants["facing=%s,half=top,open=false" % f] = {"model": m(name + "_top"), "y": y} if y else {"model": m(name + "_top")}
            variants["facing=%s,half=bottom,open=true" % f] = {"model": m(name + "_open"), "y": y} if y else {"model": m(name + "_open")}
            variants["facing=%s,half=top,open=true" % f] = {"model": m(name + "_open"), "x": 180, "y": (y + 180) % 360}
        full = {}
        for k, v in variants.items():
            for powered in ("false", "true"):
                for water in ("false", "true"):
                    full[k + ",powered=%s,waterlogged=%s" % (powered, water)] = v
        blockstate(name, {"variants": full})
        item(name, m(name + "_bottom"))
        loot(name)
        write(os.path.join(DATA, NS, "recipe", name + ".json"),
              {"type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": [ing(r) for r in recipe], "result": {"id": NS + ":" + name, "count": 1}})
        tags["trapdoors"].append(NS + ":" + name)
        if wooden:
            tags["wooden_trapdoors"].append(NS + ":" + name)
        tags["mineable/pickaxe" if metal else "mineable/axe"].append(NS + ":" + name)
        add_name(name, english)

    for wood in DOOR_WOODS:
        for colour in COLOURS:
            trapdoor_models("%s_%s_trapdoor" % (colour, wood), lambda g, c=colour, w=wood: "%s %s Trapdoor" % (title(c, g), title(w, g)),
                            ["minecraft:%s_trapdoor" % wood, "minecraft:%s_dye" % colour], True, False)
    for colour in COLOURS:
        trapdoor_models("%s_iron_trapdoor" % colour, lambda g, c=colour: "%s Iron Trapdoor" % title(c, g),
                        ["minecraft:iron_trapdoor", "minecraft:%s_dye" % colour], False, True)
    for metal, block, ingredient in METAL_DOORS:
        trapdoor_models("%s_trapdoor" % metal, lambda g, mt=metal: "Iron Trapdoor (Copper Style)" if mt == "iron" else "%s Trapdoor" % title(mt, g),
                        ["minecraft:copper_trapdoor", "minecraft:" + ingredient], False, True)

    # The coloured textures are made in the game from the door textures in use: wood and iron recoloured with
    # dyes, the copper door repainted with each metal block's own colours.
    block_sources = [
        {"type": NS + ":door_colours", "prefix": "block/",
         "textures": ["minecraft:block/%s_door_%s" % (w, p) for w in DOOR_WOODS for p in ("top", "bottom")] + ["minecraft:block/%s_trapdoor" % w for w in DOOR_WOODS]},
        {"type": NS + ":door_colours", "prefix": "block/", "metal": True,
         "textures": ["minecraft:block/iron_door_top", "minecraft:block/iron_door_bottom", "minecraft:block/iron_trapdoor"]},
        {"type": NS + ":metal_doors", "prefix": "block/", "textures": ["minecraft:block/copper_door_top", "minecraft:block/copper_door_bottom", "minecraft:block/copper_trapdoor"],
         "metals": ["minecraft:block/" + b for _, b, _ in METAL_DOORS]}]
    # The patterned blocks' textures, made from the textures in use.
    pattern_textures = sorted({textures_of(src)[f] for src in PATTERN_SOURCES if exists(src) for f in ("top", "side", "bottom")})
    block_sources.append({"type": NS + ":patterns", "textures": pattern_textures, "patterns": [p for p, _ in PATTERNS]})
    item_sources = [
        {"type": NS + ":door_colours", "prefix": "item/", "textures": ["minecraft:item/%s_door" % w for w in DOOR_WOODS]},
        {"type": NS + ":door_colours", "prefix": "item/", "metal": True, "textures": ["minecraft:item/iron_door"]},
        {"type": NS + ":metal_doors", "prefix": "item/", "textures": ["minecraft:item/copper_door"],
         "metals": ["minecraft:block/" + b for _, b, _ in METAL_DOORS]}]
    # Items got their own atlas in 1.21.11; before that their textures are in the blocks atlas.
    if MC >= (1, 21, 11):
        write(os.path.join(ROOT, "assets", "minecraft", "atlases", "blocks.json"), {"sources": block_sources})
        write(os.path.join(ROOT, "assets", "minecraft", "atlases", "items.json"), {"sources": item_sources})
    else:
        write(os.path.join(ROOT, "assets", "minecraft", "atlases", "blocks.json"), {"sources": block_sources + item_sources})


TABLE_WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"]
TABLE_TOPS = (["terracotta"] + ["%s_terracotta" % c for c in COLOURS] + ["%s_glazed_terracotta" % c for c in COLOURS] +
              ["stripped_%s_wood" % w for w in TABLE_WOODS] +
              ["stripped_crimson_hyphae", "stripped_warped_hyphae", "stripped_bamboo_block", "smooth_stone", "polished_granite",
               "polished_diorite", "polished_andesite", "chiseled_deepslate", "tuff", "polished_tuff", "chiseled_tuff", "chiseled_resin_bricks",
               "quartz_block", "chiseled_quartz_block", "quartz_bricks", "smooth_quartz", "glass"] +
              ["%s_stained_glass" % c for c in COLOURS])
TABLE_LEGS = ["oak"] + COLOURS
LEG_BOXES = {"nw": ([0, 0, 0], [3, 3, 3]), "ne": ([13, 0, 0], [16, 3, 3]), "sw": ([0, 0, 13], [3, 3, 16]), "se": ([13, 0, 13], [16, 3, 16])}


def table_top_textures(top):
    """Top, side and bottom of a table's top: stripped wood shows its grain all round."""
    if top.startswith("stripped_"):
        if top == "stripped_bamboo_block":
            t = "minecraft:block/stripped_bamboo_block"
        elif "hyphae" in top:
            t = "minecraft:block/" + top.replace("hyphae", "stem")
        else:
            t = "minecraft:block/" + top.replace("_wood", "_log")
        return {"top": t, "side": t, "bottom": t}
    return faces(top)


def tables(add_name, tags):
    def face(tex, cull=None, uv=None):
        f = {"texture": tex}
        if cull:
            f["cullface"] = cull
        if uv:
            f["uv"] = uv
        return f

    def leg(box):
        f, t = box
        return {"from": f, "to": t, "faces": {d: face("#legs", "down" if d == "down" else None) for d in ["down", "up", "north", "south", "west", "east"]}}

    def leg_boxes(h):
        return {c: (f, [t[0], h, t[2]]) for c, (f, t) in LEG_BOXES.items()}

    # (tall, glass): the top's lowest and highest pixel. Low tables' tops are at 9, tall ones' at 16 (level
    # with a full block); stone, terracotta and wood tops are 6 pixels thick, glass ones 2. Legs reach the top.
    kinds = {}
    for tall in (False, True):
        for glass in (False, True):
            y1 = 16 if tall else 9
            y0 = y1 - (2 if glass else 6)
            kinds[(tall, glass)] = (y0, y1)
            # The top, built facing north (the blockstate turns it, and its texture, to the way it was placed).
            sides = {d: face("#side", d) for d in HORIZONTAL}
            write(os.path.join(ASSETS, "models", "block", "template_table_top_%d_%d.json" % (y0, y1)),
                  {"parent": "minecraft:block/block", "textures": {"particle": "#side"},
                   "elements": [{"from": [0, y0, 0], "to": [16, y1, 16], "faces": dict(sides, up=face("#top", "up" if y1 == 16 else None), down=face("#bottom"))}]})
            # Legs, 3 pixels square, one model per corner and one with all four.
            for corner, box in leg_boxes(y0).items():
                write(os.path.join(ASSETS, "models", "block", "template_table_leg_%s_%d.json" % (corner, y0)),
                      {"parent": "minecraft:block/block", "textures": {"particle": "#legs"}, "elements": [leg(box)]})
            write(os.path.join(ASSETS, "models", "block", "template_table_legs_%d.json" % y0),
                  {"parent": "minecraft:block/block", "textures": {"particle": "#legs"}, "elements": [leg(b) for b in leg_boxes(y0).values()]})
            write(os.path.join(ASSETS, "models", "block", "template_table_item_%d_%d.json" % (y0, y1)),
                  {"parent": "minecraft:block/block", "textures": {"particle": "#side"},
                   "elements": [{"from": [0, y0, 0], "to": [16, y1, 16], "faces": dict({d: face("#side") for d in HORIZONTAL}, up=face("#top"), down=face("#bottom"))}]
                   + [leg(b) for b in leg_boxes(y0).values()]})
    t_legs = {}
    for legs in TABLE_LEGS:
        t = {"legs": "minecraft:block/oak_planks" if legs == "oak" else "minecraft:block/%s_concrete" % legs}
        t_legs[legs] = t["legs"]
        for y0 in sorted({k[0] for k in kinds.values()}):
            for corner in LEG_BOXES:
                model("table_legs_%s_%s_%d" % (legs, corner, y0), m("template_table_leg_%s_%d" % (corner, y0)), t)
            model("table_legs_%s_%d" % (legs, y0), m("template_table_legs_%d" % y0), t)
    for tall in (False, True):
        for top in TABLE_TOPS:
            glass = top.endswith("glass")
            y0, y1 = kinds[(tall, glass)]
            top_model = "table_top_%s%s" % (top, "_tall" if tall else "")
            model(top_model, m("template_table_top_%d_%d" % (y0, y1)), table_top_textures(top))
            for legs in TABLE_LEGS:
                name = top + ("_tall" if tall else "") + "_table" + ("" if legs == "oak" else "_%s_legs" % legs)
                parts = [{"when": {"facing": f}, "apply": {"model": m(top_model), "y": Y_FROM_NORTH[f]}} for f in HORIZONTAL]
                for corner in LEG_BOXES:
                    ns, ew = {"n": "north", "s": "south"}[corner[0]], {"w": "west", "e": "east"}[corner[1]]
                    parts.append({"when": {ns: "false", ew: "false"}, "apply": {"model": m("table_legs_%s_%s_%d" % (legs, corner, y0))}})
                blockstate(name, {"multipart": parts})
                if MC >= (1, 21, 4):
                    write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:composite", "models": [
                        {"type": "minecraft:model", "model": m(top_model)}, {"type": "minecraft:model", "model": m("table_legs_%s_%d" % (legs, y0))}]}})
                else:
                    # Before 1.21.4 an item has one model: the top and legs together.
                    write(os.path.join(ASSETS, "models", "item", name + ".json"),
                          {"parent": m("template_table_item_%d_%d" % (y0, y1)), "textures": dict(table_top_textures(top), legs=t_legs[legs])})
                loot(name)
                leg_item = "minecraft:stick" if legs == "oak" else "minecraft:%s_concrete" % legs
                pattern = ["TTT", "L L", "L L"] if tall else ["TTT", "L L"]
                write(os.path.join(DATA, NS, "recipe", name + ".json"),
                      {"type": "minecraft:crafting_shaped", "category": "decorations", "pattern": pattern,
                       "key": {"T": ing("minecraft:" + top), "L": ing(leg_item)}, "result": {"id": NS + ":" + name, "count": 3}})
                if not glass:
                    tags["mineable/axe" if top.startswith("stripped_") else "mineable/pickaxe"].append(NS + ":" + name)
                add_name(name, lambda g, tp=top, lg=legs, tl=tall: "%s %sTable" % (title(tp, g), "Tall " if tl else "")
                         + ("" if lg == "oak" else " (%s Legs)" % title(lg, g)))


def tools(add_name):
    """The terrain brush and the builder's guide, the wheel key's name, and the world-gen feature for smoothing new land."""
    for name, english, recipe in (("terrain_brush", "Terrain Brush", ["minecraft:brush", "minecraft:copper_ingot", "minecraft:smooth_stone_slab"]),
                                  ("builders_guide", "Builder's Guide", ["minecraft:paper", "minecraft:string", "minecraft:stick"])):
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/handheld", "textures": {"layer0": NS + ":item/" + name}})
        item(name, NS + ":item/" + name)
        write(os.path.join(DATA, NS, "recipe", name + ".json"),
              {"type": "minecraft:crafting_shapeless", "category": "equipment", "ingredients": [ing(r) for r in recipe],
               "result": {"id": NS + ":" + name, "count": 1}})
        add_name(name, lambda g, e=english: e)
    write(os.path.join(DATA, NS, "worldgen", "configured_feature", "smooth_terrain.json"), {"type": NS + ":smooth_terrain", "config": {}})
    write(os.path.join(DATA, NS, "worldgen", "placed_feature", "smooth_terrain.json"), {"feature": NS + ":smooth_terrain", "placement": []})


def cube_icon():
    """The first tab's icon: a block in the brand colours, with gaps along its edges."""
    if MC >= (1, 21, 4):
        write(os.path.join(ASSETS, "items", "cube.json"), {"model": {"type": "minecraft:model", "model": NS + ":item/cube"}})
    write(os.path.join(ASSETS, "models", "item", "cube.json"), {"parent": "minecraft:block/cube", "textures": {
        "particle": NS + ":block/cube_top", "up": NS + ":block/cube_top", "down": NS + ":block/cube_top",
        "north": NS + ":block/cube_left", "south": NS + ":block/cube_left", "east": NS + ":block/cube_right", "west": NS + ":block/cube_right"}})


def main():
    # Leave out blocks this Minecraft version doesn't have.
    global MATERIALS, LOGS, LOW_PLANTS, PLANTS, TALL_PLANTS, DOOR_WOODS, TABLE_TOPS
    MATERIALS = [(mat, kind) for mat, kind in MATERIALS if exists(vanilla_of(mat))]
    LOGS = [log for log in LOGS if exists(log)]
    LOW_PLANTS = [p for p in LOW_PLANTS if exists(p)]
    PLANTS = [p for p in PLANTS if exists(p)]
    TALL_PLANTS = [p for p in TALL_PLANTS if exists(p)]
    DOOR_WOODS = [w for w in DOOR_WOODS if exists(w + "_door")]
    TABLE_TOPS = [t for t in TABLE_TOPS if exists(t)]
    for sub in ["blockstates", "models", "items"]:
        shutil.rmtree(os.path.join(ASSETS, sub), ignore_errors=True)
    for sub in ["loot_table", "recipe", "worldgen"]:
        shutil.rmtree(os.path.join(DATA, NS, sub), ignore_errors=True)
    templates()

    tags = {k: [] for k in ["stairs", "slabs", "walls", "fences", "fence_gates", "trapdoors", "buttons", "pressure_plates",
                            "wooden_doors", "doors", "wooden_trapdoors", "mineable/pickaxe", "mineable/shovel", "mineable/axe", "mineable/hoe",
                            "needs_stone_tool", "needs_iron_tool", "needs_diamond_tool"]}
    tag_of = {"stairs": "stairs", "steps_4": "stairs", "steps_6": "stairs", "steps_8": "stairs", "slab": "slabs", "wall": "walls", "fence": "fences", "fence_gate": "fence_gates",
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
            as_building = kind == "nature" and (suffix not in NATURE_SHAPES or (is_building(mat) and suffix in NEW_NATURE))
            BUILDERS[suffix](mat, "building" if as_building else kind, name)
            loot(name, suffix, silk=kind == "glass" or mat == "ice" or mat == "packed_ice" or mat == "blue_ice")
            stonecutting(mat, name, cut)
            if suffix in tag_of:
                tags[tag_of[suffix]].append(NS + ":" + name)
            vid = "minecraft:" + vanilla_of(mat)
            src_kind = next((k for mt, k in MATERIALS if mt == vanilla_of(mat)), kind) if kind == "pattern" else kind
            if src_kind in ("terracotta", "concrete", "glazed") or (src_kind in ("nature", "building", "pattern") and vid in VANILLA_PICKAXE):
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
    pattern_blocks(add_name, tags)
    thin_logs(add_name, tags)
    round_logs(add_name, tags)
    plants(add_name)
    coral(add_name)
    doors(add_name, tags)
    tables(add_name, tags)
    tools(add_name)
    cube_icon()

    for tag, values in tags.items():
        write(os.path.join(DATA, "minecraft", "tags", "block", tag + ".json"), {"replace": False, "values": values})
    for tag in ["stairs", "slabs", "walls", "fences", "fence_gates", "trapdoors", "buttons", "wooden_doors", "doors", "wooden_trapdoors"]:
        write(os.path.join(DATA, "minecraft", "tags", "item", tag + ".json"), {"replace": False, "values": tags[tag]})

    for tab, name in TABS:
        us["itemGroup.%s.%s" % (NS, tab)] = gb["itemGroup.%s.%s" % (NS, tab)] = "Cubework: " + name
    us["item.%s.cube" % NS] = gb["item.%s.cube" % NS] = "Cubework"
    for k in list(us):
        if k in ("block.%s.terrain_brush" % NS, "block.%s.builders_guide" % NS):
            us["item" + k[5:]] = gb["item" + k[5:]] = us.pop(k)
            gb.pop(k, None)
    us["key.%s.wheel" % NS] = gb["key.%s.wheel" % NS] = "Shape wheel (hold)"
    us["key.category.%s.%s" % (NS, NS)] = gb["key.category.%s.%s" % (NS, NS)] = "Cubework"
    write(os.path.join(ASSETS, "lang", "en_us.json"), us)
    write(os.path.join(ASSETS, "lang", "en_gb.json"), gb)
    print("shapes:", count, "names:", len(us))


if __name__ == "__main__":
    main()
