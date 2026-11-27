"""
0.59.2's looks, for tools/arealooks.py: round59b() adds the rows (after round59). John's 19:17: "go across all of Johto
now, specifically on the externals ... anywhere there's an old texture, we've retextured in our style for all
buildings ... shelf trims ... flag poles". Found by a sweep of the Johto map for every block no row covered.

Two layers:
  - Johto's fallbacks: one row a block over all Johto (but the cliffside west of Cianwood John's still building, and
    the far south), so whatever wasn't given its own look somewhere gets ours. The rows are as big as Johto, so every
    place's own row (smaller) is drawn over them.
  - Each place's own leftovers: the S.S. AQUA, Olivine's pier, Ecruteak, the BURNED TOWER, Goldenrod, the rocks.
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


# Johto but the cliffside John's building west of Cianwood (19:17: "you don't need to worry about where I'm going on
# the cliffside yet ... maybe Route 48 ... up to the beach") and the shore south of it.
JM = (-2575, 60, -1000, 1000, "Johto")

# (look, blocks, swaps): the fallbacks.
FALLBACK = [
    ("swap", "white_concrete,white_carpet,white_wool", "block/white_concrete=jw_white,block/white_wool=jw_white"),
    ("swap", "light_gray_concrete,light_gray_carpet,light_gray_wool", "block/light_gray_concrete=jw_grey,block/light_gray_wool=jw_grey"),
    ("swap", "quartz_block,quartz_slab,quartz_stairs,smooth_quartz,smooth_quartz_slab,smooth_quartz_stairs,quartz_bricks,chiseled_quartz_block",
     "block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean,block/quartz_bricks=quartz_clean,"
     "block/chiseled_quartz_block=quartz_clean,block/chiseled_quartz_block_top=quartz_clean"),
    ("swap", "white_terracotta", "block/white_terracotta=jhouse_wall"),
    ("swap", "light_gray_terracotta", "block/light_gray_terracotta=jw_grey"),
    ("swap", "stripped_mangrove_log,stripped_mangrove_wood", "block/stripped_mangrove_log=mah_post,block/stripped_mangrove_log_top=post_bt_top"),
    # The gatehouses (Routes 29, 31, 32, 36, 42, 43, 28 and the rest): their teal band, their dark stone foot, their
    # signs, their mullions; their windows' pale blue.
    ("swap", "cyan_terracotta", "block/cyan_terracotta=npg_trim"),
    ("swap", "polished_tuff", "block/polished_tuff=npg_stone"),
    ("swap", "warped_trapdoor", "block/warped_trapdoor=npg_trim"),
    ("swap", "deepslate_brick_wall", "block/deepslate_bricks=station_mullion"),
    ("swap", "light_blue_wool", "block/light_blue_wool=ds_blue_light"),
    # The lights in the Centers' and Marts' doorway floors.
    ("swap", "ochre_froglight,sea_lantern", "block/ochre_froglight_side=door_light!,block/ochre_froglight_top=door_light!,block/sea_lantern=door_light!"),
    # (20:47: "on the cliffside you'll see there's still stripped logs ... blend in a little bit better") The timbers set
    # in the cliffs: the cliffs' rock, a shade darker with roots through it, the ledges' earth on top.
    ("swap", "stripped_spruce_wood,stripped_spruce_log", "block/stripped_spruce_log=hill_top@4|cliff_root@4,block/stripped_spruce_log_top=hill_top@4|cliff_root@4"),
    ("swap", "polished_diorite_stairs,polished_diorite_slab,polished_diorite", "block/polished_diorite=house_trim"),
    ("swap", "iron_bars", "block/iron_bars=railing_dark"),
    ("swap", "barrel", "block/barrel_side=crate_barrel_side,block/barrel_top=crate_barrel_top,block/barrel_bottom=crate_barrel_top"),
    ("swap", "composter", "block/composter_side=bin_side,block/composter_top=bin_top,block/composter_bottom=bin_top"),
    ("swap", "end_stone_brick_wall", "block/end_stone_bricks=banner_foot"),
    ("swap", "redstone_lamp", "block/redstone_lamp=lamp_globe_warm!,block/redstone_lamp_on=lamp_globe_warm!"),
    # (19:17: "if you've accidentally missed any shelf trims") Every shelf left a flat board in its own wood's colour
    # (the signs, in their own boxes, stay signs).
    ("panel", "spruce_shelf", "block/spruce_shelf=wood_shelf_dark"),
    ("panel", "oak_shelf,jungle_shelf", "block/oak_shelf=band_shelf,block/jungle_shelf=band_shelf"),
    ("panel", "pale_oak_shelf", "block/pale_oak_shelf=panel_white"),
    ("panel", "birch_shelf", "block/birch_shelf=wood_shelf"),
    ("panel", "dark_oak_shelf", "block/dark_oak_shelf=panel_planter_dark"),
    ("panel", "mangrove_shelf", "block/mangrove_shelf=panel_wood_red"),
    ("panel", "warped_shelf", "block/warped_shelf=green_shelf"),
    ("panel", "cherry_shelf", "block/cherry_shelf=panel_cream"),
    ("panel", "bamboo_shelf", "block/bamboo_shelf=panel_gold"),
    ("panel", "crimson_shelf", "block/crimson_shelf=vio_bridge_beam"),
]

# (0.59.3, 20:47: "do not texture those, there's no need right now") The S.S. AQUA as John built it: its blocks keep
# their own look, in slices small enough to win over every place's look round it.
SHIP = (-2306, 662, -2262, 856)


def round59b(add, room, G):
    SEA_ROCK, SEA_ROCK_BLOCKS = G["SEA_ROCK"], G["SEA_ROCK_BLOCKS"]
    for look, blocks, swaps in FALLBACK:
        add(look, blocks, JM, swaps)
    # The cliffs' timbers on Johto's routes on Kanto's map too (Routes 26 and 27, Tohjo Falls; Kanto's own wait for
    # the Kanto pass: John, 20:47, "in Johto specifically").
    for b in ((-1310, 600, -940, 800, "Routes 26 and 27's cliffs"), (-1040, 540, -995, 770, "Routes 26 and 27's cliffs")):
        add("swap", "stripped_spruce_wood,stripped_spruce_log", b,
            "block/stripped_spruce_log=hill_top@4|cliff_root@4,block/stripped_spruce_log_top=hill_top@4|cliff_root@4")
    # The S.S. AQUA, John's own.
    # ((0.67.1, 17:13) Retextured now, as the S.S. ANNE (arealooks67): kept only what isn't.)
    done = set(b for r in json.load(open(os.path.join(HERE, "area_ships67.json")))["rows"] for b in r["blocks"].split(","))
    ship = ",".join(k for k in json.load(open(os.path.join(HERE, "area_ship59.json"))) if k not in done)
    for z in range(SHIP[1], SHIP[3] + 1, 16):
        add("keep", ship, (SHIP[0], z, SHIP[2], min(SHIP[3], z + 15), "the S.S. AQUA, as John built it"), "-", ys(12, 60))
    # Olivine's pier: its stone head, its posts and bollards, its rails, its deck's edges.
    OP = (-2290, 598, -2263, 625, "Olivine's pier")
    # ((0.70, John's 27 Nov 15:00: "the textures at the back haven't been done? The bit which basically blocking you from
    # going to the boat ... it's gone from white to like a grey") Its stone head (the gate at its end) the sea wall's
    # white in big dressed stones; the steps down onto it and their posts the sea wall's white cap and posts.)
    add("swap", "andesite,polished_andesite,polished_andesite_slab,polished_andesite_stairs,diorite,diorite_slab", OP,
        "block/andesite=ol_gate_stone@4,block/polished_andesite=ol_gate_stone@4,block/diorite=ol_wall_cap", ys(14, 26))
    add("swap", "diorite_wall", OP, "block/diorite=ol_post", ys(14, 26))
    add("swap", "polished_blackstone_wall,stripped_pale_oak_wood,stripped_pale_oak_log", OP,
        "block/polished_blackstone=cin_post,block/stripped_pale_oak_log=pier_ol,block/stripped_pale_oak_log_top=pier_ol", ys(12, 22))
    add("swap", "resin_bricks,resin_brick_stairs,birch_planks,birch_stairs,terracotta,yellow_terracotta", OP,
        "block/resin_bricks=ol_gate_stone@4,block/birch_planks=pier_ol,block/terracotta=ol_gate_stone@4,block/yellow_terracotta=lamp_park_cap", ys(14, 26))
    add("swap", "pale_oak_fence_gate,pale_oak_fence", (-2320, 560, -2235, 625, "Olivine's harbour fences"), "block/pale_oak_planks=white_fence", ys(17, 22))
    # The planters' soil (Azalea's, New Bark's, Olivine's); the stepping stones by the Ilex gate and the Day Care.
    for b in ((-2019, 869, -2009, 871, "Azalea's planters"), (-1429, 758, -1423, 760, "New Bark's planter"), (-2313, 595, -2256, 597, "Olivine's planters")):
        add("swap", "coarse_dirt", b, "block/coarse_dirt=planter_soil", ys(17, 21))
    for b in ((-2099, 865, -2089, 873, "the stepping stones by the Ilex gate"), (-2080, 821, -2063, 836, "the stepping stones by the Day Care")):
        add("swap", "packed_mud,mossy_cobblestone_slab", b, "block/packed_mud=park_stone,block/mossy_cobblestone=park_stone", ys(18, 20))
    # Route 27's stone steps by the falls; the DANCE THEATRE's floor; Goldenrod's DEPT. STORE's forecourt; the
    # NATIONAL PARK's swan's step and rods.
    add("swap", "stone_brick_stairs", (-1175, 745, -1098, 770, "Route 27's steps"), "block/stone_bricks=step_cer", ys(18, 26))
    add("swap", "calcite", (-2031, 393, -2014, 405, "the DANCE THEATRE's floor"), "block/calcite=tin_floor", ys(21, 21))
    add("swap", "light_blue_terracotta,light_gray_terracotta", (-2098, 712, -2068, 726, "the DEPT. STORE's forecourt"),
        "block/light_blue_terracotta=ds_blue_light,block/light_gray_terracotta=gold_kerb", ys(19, 19))
    add("swap", "polished_andesite", (-2127, 490, -2120, 494, "the swan's step"), "block/polished_andesite=park_stone", ys(19, 19))
    add("swap", "end_rod", (-2086, 501, -2082, 510, "the park gate's rods"), "block/end_rod=rod_clean", ys(22, 26))
    # Ecruteak: the timber of its houses' eaves and the house by the BURNED TOWER (stone foot, wood floor, trim,
    # paper screens); the stone lantern there; the dark of the BURNED TOWER, its spire.
    EC = (-2090, 336, -1966, 432, "Ecruteak City")
    add("swap", "dark_oak_slab,dark_oak_stairs,dark_oak_planks,dark_oak_trapdoor,dark_oak_fence,dark_oak_fence_gate,dark_oak_button", EC,
        "block/dark_oak_planks=ecru_timber,block/dark_oak_trapdoor=ecru_timber")
    RB = (-2048, 356, -2022, 374, "the house by the BURNED TOWER")
    add("swap", "stone,stone_stairs,stone_slab,andesite,stone_bricks,stone_brick_stairs,cobblestone_stairs", RB, "block/stone=ecru_base,block/andesite=ecru_base,"
        "block/stone_bricks=ecru_base,block/cobblestone=ecru_base", ys(20, 22))
    add("swap", "spruce_planks,spruce_slab,spruce_button,acacia_stairs", RB, "block/spruce_planks=tin_floor,block/acacia_planks=tin_floor", ys(20, 34))
    add("swap", "polished_diorite,polished_diorite_slab,polished_diorite_stairs", RB, "block/polished_diorite=ecru_trim", ys(20, 34))
    add("swap", "deepslate_tiles", RB, "block/deepslate_tiles=lacquer_dark", ys(20, 34))
    add("swap", "jungle_trapdoor,mangrove_trapdoor,oak_trapdoor,pale_oak_trapdoor", EC,
        "block/jungle_trapdoor=sprout_screen,block/mangrove_trapdoor=sprout_screen,block/oak_trapdoor=sprout_screen,block/pale_oak_trapdoor=sprout_screen", ys(20, 34))
    add("swap", "anvil,chipped_anvil,damaged_anvil", (-2038, 367, -2032, 369, "a stone lantern by the BURNED TOWER"),
        "block/anvil=lantern_stone_top,block/anvil_top=lantern_stone_top", ys(22, 26))
    BT = (-2079, 344, -2051, 372, "the BURNED TOWER")
    add("swap", "black_concrete", BT, "block/black_concrete=burned_char", ys(20, 45))
    add("swap", "polished_deepslate_wall", BT, "block/polished_deepslate=spire_metal", ys(28, 45))
    add("swap", "dark_oak_shelf", BT, "block/dark_oak_shelf=burned_char", ys(20, 45))
    add("swap", "black_concrete", (-1992, 337, -1964, 366, "the TIN TOWER's way in"), "block/black_concrete=cave_dark", ys(20, 30))
    # Goldenrod: the station's dark roof, its aerials; the bands of the Celadon buildings; the fountains' lawn.
    GI = G["GOLD_IN"]
    add("swap", "polished_deepslate,polished_deepslate_stairs", (-2200, 678, -2185, 686, "the station's roof"), "block/polished_deepslate=lab_cin_roof", ys(40, 50))
    add("swap", "diorite_wall", (-2196, 680, -2138, 688, "the aerials over the line"), "block/diorite=rod_clean", ys(45, 66))
    add("swap", "prismarine_bricks", GI, "block/prismarine_bricks=cel_roof_teal", ys(20, 47))
    # ((0.70, 15:00) Seamless, as the green inside them.)
    add("swap", "lime_terracotta", (-2207, 697, -2178, 711, "the station's fountains"), "block/lime_terracotta=gold_plaza_lime@4", ys(19, 19))
    # The Lake of Rage: the GYARADOS's rock under the water, the rocks along the shore, the statue's last pieces (its
    # whiskers' copper, its eye's coal, its planks).
    add("swap", SEA_ROCK_BLOCKS + ",stone", (-1785, 201, -1766, 220, "the GYARADOS's rock"), SEA_ROCK, ys(12, 18))
    add("swap", "tuff,tuff_slab,tuff_stairs,tuff_wall,cobblestone_stairs,cobblestone_wall,andesite_stairs,andesite_wall,diorite_slab",
        (-1827, 227, -1776, 256, "the Lake of Rage's rocks"), G["BOULDER"], ys(22, 36))
    GY = (-1800, 195, -1750, 232, "the Lake of Rage's GYARADOS")
    add("swap", "exposed_copper,weathered_copper,waxed_exposed_copper,waxed_weathered_copper,brown_concrete", GY,
        "block/exposed_copper=gy_fin@4,block/weathered_copper=gy_fin@4,block/brown_concrete=gy_fin@4", ys(16, 50))
    add("swap", "coal_block", GY, "block/coal_block=gy_eye", ys(16, 50))
    add("swap", "jungle_planks", GY, "block/jungle_planks=gy_cream@4", ys(16, 50))
    add("swap", "mangrove_planks", GY, "block/mangrove_planks=gy_red_dark@4", ys(16, 50))
    # The rocks still in the water: Cherrygrove's line by the beach, the WHIRL ISLANDS' rocks, Route 41's.
    for x0, z0, x1, z1 in ((-1797, 786, -1758, 791), (-2477, 719, -2367, 792), (-1229, 791, -1225, 796)):
        add("swap", SEA_ROCK_BLOCKS, (x0, z0, x1, z1, "a rock in the water"), SEA_ROCK, ys(12, 26))
