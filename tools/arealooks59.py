"""
0.59.0's looks, for tools/arealooks.py: round59() adds the rows (called after round58, at the end of round50's chain).
John's review of 0.58.0 (18:14 to 18:40, notes/feedback-2026-11-23-18*.md); the pictures are tools/looks59.py's.
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


# Routes 14 and 15's lamps (18:19: "update all those so they're not orange ... maybe like a white"): x0, z0, x1, z1.
ROUTE_LAMPS = [(-260, 748, -255, 752), (-260, 763, -255, 767), (-260, 778, -255, 782), (-260, 793, -255, 797), (-409, 794, -405, 799), (-394, 794, -390, 799),
               (-379, 794, -375, 799), (-364, 794, -360, 799), (-349, 794, -345, 799), (-334, 794, -330, 799), (-319, 794, -315, 799), (-304, 794, -300, 799),
               (-289, 794, -285, 799), (-274, 794, -270, 799)]
# Route 13's lanterns (and the one on Route 14): x, y, z of each.
ROUTE_LANTERNS = [(-296, 31, 782), (-264, 32, 703), (-226, 32, 703), (-201, 32, 703), (-282, 32, 707), (-240, 32, 707), (-213, 32, 707), (-272, 32, 711),
                  (-251, 32, 711), (-234, 32, 711), (-185, 32, 711)]
# Victory Road's rocks outside (18:19: "the stones here, combination of where you've got granite and stuff").
VR_ROCKS = [(-923, 267, -918, 271), (-933, 275, -928, 280), (-919, 283, -909, 294), (-898, 284, -891, 290), (-905, 298, -894, 306)]
# The lamps up Route 45 to Blackthorn (18:25: "they're all black ... change them to the white, the same as Bark").
R45_LAMPS = [(-1518, 30, 662), (-1505, 34, 580), (-1506, 35, 514), (-1521, 39, 429)]
# Goldenrod's alleys (18:36: "the rods, the fences and barrels and flooring and everything else"), found by their floors.
ALLEYS = [(-2146, 655, -2113, 666), (-2096, 655, -2069, 658), (-2065, 690, -2055, 696), (-2065, 707, -2055, 715), (-2152, 712, -2110, 726), (-2071, 735, -2064, 745)]


def round59(add, room, G):
    SEA_ROCK, SEA_ROCK_BLOCKS, BOULDER, BOULDER_BLOCKS = G["SEA_ROCK"], G["SEA_ROCK_BLOCKS"], G["BOULDER"], G["BOULDER_BLOCKS"]
    D = json.load(open(os.path.join(HERE, "area_johto54.json")))

    # ---------- the rocks in the water (18:14 Routes 26/27; 18:19 Routes 12/13 and the islands; 18:25 New Bark and
    # Cherrygrove's beach; 18:39 south of Goldenrod): every rock of John's left over, found on the maps ----------
    SR = SEA_ROCK_BLOCKS + ",granite_slab,granite_stairs,granite_wall"
    for x0, z0, x1, z1, y0, y1 in json.load(open(os.path.join(HERE, "area_rocks59.json"))):
        add("swap", SR, (x0, z0, x1, z1, "a rock in the water"), SEA_ROCK, ys(y0, y1))

    # ---------- Cinnabar Island (18:14) ----------
    # The LAB's way in ("still showing the old steps walking in"): its steps the white step; its planters' fronts
    # white boards, not signs; its sign, the Gym's two, as Kanto's.
    add("swap", "polished_diorite_slab,polished_diorite_stairs,polished_diorite,calcite", (-810, 917, -799, 925, "the LAB's doorstep"),
        "block/polished_diorite=white_step,block/calcite=white_step", ys(18, 18))
    add("panel", "mangrove_shelf", (-810, 924, -799, 927, "the LAB's planters"), "block/mangrove_shelf=panel_white", ys(18, 19))
    for b in ((-787, 911, -784, 913, "the Gym's sign"), (-774, 911, -772, 913, "the Gym's sign"), (-802, 928, -799, 930, "the LAB's sign")):
        add("sign", "pale_oak_shelf,mangrove_shelf", b, "-", ys(17, 20))
    # Its paths' brown edge ("around the path where you've added the curb trim"): the path itself, no division.
    C = (-895, 885, -756, 958, "Cinnabar Island")
    add("swap", "brown_terracotta", C, "block/brown_terracotta=path_kanto@4", ys(16, 18))
    # The volcano ("I can still see bone ... the entire volcano"): its steam over all the bone; its rock its own warm
    # volcanic stone, darker to the crater, the basalt crust black-brown (the rocks in the water keep the sea's).
    VB = (-884, 888, -811, 953, "Cinnabar's volcano")
    add("steam", "bone_block", (-866, 893, -815, 955, "Cinnabar's volcano's steam"), "steam")
    add("swap", "cobbled_deepslate,cobbled_deepslate_slab,cobbled_deepslate_stairs,cobbled_deepslate_wall", VB, "block/cobbled_deepslate=volc_rock@2")
    add("swap", "deepslate", VB, "block/deepslate=volc_rock_dark@2,block/deepslate_top=volc_rock_dark_top@2")
    add("swap", "basalt,polished_basalt", VB, "block/basalt_side=volc_basalt@2,block/basalt_top=volc_basalt_top@2,block/polished_basalt_side=volc_basalt@2,"
                                              "block/polished_basalt_top=volc_basalt_top@2")
    add("swap", "tuff,tuff_slab,tuff_stairs,tuff_wall", VB, "block/tuff=volc_tuff@2")

    # ---------- Routes 13 to 15 (18:19): the lamps white, the lanterns white ----------
    for x0, z0, x1, z1 in ROUTE_LAMPS:
        b = (x0, z0, x1, z1, "a lamp on Routes 14 and 15")
        add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern", b,
            "block/red_sandstone=lamp_route_post,block/chiseled_red_sandstone=lamp_route_foot,block/red_sandstone_top=lamp_route_cap,"
            "block/orange_wool=lamp_route_cap,block/sea_lantern=lamp_globe_white!", ys(29, 38))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ys(29, 38))
    for x, y, z in ROUTE_LANTERNS:
        add("swap", "lantern", (x - 1, z - 1, x + 1, z + 1, "a lantern on Route 13"), "block/lantern=lantern_white", ys(y - 1, y + 1))

    # ---------- Routes 9 and 10's two signs left (18:19) ----------
    add("sign", "pale_oak_shelf", (-247, 259, -244, 261, "Route 9's sign"), "-", ys(33, 36))
    add("sign", "pale_oak_shelf", (-136, 361, -133, 363, "Route 10's sign"), "-", ys(34, 36))

    # ---------- Nugget Bridge's north end (18:19: "the end of the bridge heading towards Bill's house ... isn't
    # textured"): its steps and crossing as the south end's ----------
    add("swap", "andesite_slab,stripped_oak_wood,oak_shelf,pale_oak_fence", (-360, 174, -346, 179, "Nugget Bridge's north end"),
        "block/andesite=step_cer,block/stripped_oak_log=bridge_gold_beam,block/oak_shelf=bridge_gold_rail,block/pale_oak_planks=white_fence", ys(33, 36))

    # ---------- the Pewter Museum's way in (18:19: "just be a flat one, same as the stairs above it ... an off grey"):
    # its floor and steps, and the shelves either side of its door, all the trim's off grey, flat ----------
    add("swap", "quartz_bricks,calcite,diorite,polished_diorite,chiseled_quartz_block,white_concrete,white_terracotta,light_gray_concrete,smooth_quartz,"
        "quartz_block,quartz_slab,smooth_quartz_slab,smooth_quartz_stairs", (-806, 274, -797, 280, "the Museum's way in"),
        "block/quartz_bricks=museum_trim,block/calcite=museum_trim,block/diorite=museum_trim,block/polished_diorite=museum_trim,"
        "block/chiseled_quartz_block=museum_trim,block/chiseled_quartz_block_top=museum_trim,block/white_concrete=museum_trim,block/white_terracotta=museum_trim,"
        "block/light_gray_concrete=museum_trim,block/quartz_block_bottom=museum_trim,block/quartz_block_top=museum_trim,block/quartz_block_side=museum_trim", ys(29, 30))
    add("panel", "spruce_shelf", (-804, 276, -801, 278, "the Museum's porch"), "block/spruce_shelf=museum_trim", ys(31, 33))

    # ---------- Victory Road, outside (18:19) ----------
    VRB = BOULDER_BLOCKS + ",granite,granite_stairs,granite_slab,mud_brick_slab,dripstone_block,cobblestone_stairs,andesite_wall"
    VRS = BOULDER + "," + ",".join("block/%s=boulder_stone_top|boulder_stone@2" % t for t in ("granite", "mud_bricks", "dripstone_block"))
    for x0, z0, x1, z1 in VR_ROCKS:
        add("swap", VRB, (x0, z0, x1, z1, "a rock by Victory Road"), VRS, ys(25, 30))
    add("sign", "mangrove_shelf,pale_oak_shelf", (-931, 267, -928, 269, "Victory Road's sign"), "-", ys(25, 28))

    # ---------- the INDIGO PLATEAU (18:25: "the oxidized copper trapdoors probably need replacing something flat ...
    # the same on the actual build when you get to the windows") ----------
    add("swap", "waxed_weathered_copper_trapdoor,weathered_copper_trapdoor", (-920, 183, -905, 208, "the PLATEAU's stairs' pillars"),
        "block/weathered_copper_trapdoor=metal_teal_flat", ys(26, 35))
    add("swap", "waxed_copper_trapdoor,copper_trapdoor", (-922, 165, -903, 170, "the PLATEAU's window sills"), "block/copper_trapdoor=plateau_sill", ys(36, 41))

    # ---------- Johto ----------
    # Route 45's bridges (18:25: "the top block of the middle pillar on the bridge is still a stripped ... spruce"):
    # the posts' tops above the deck the beams' too.
    for x0, z0, x1, z1, y0, y1 in D["bridges"]:
        if not G["bridge45"](x0, z0): continue
        d = y0 + 1
        add("swap", "stripped_spruce_wood,stripped_spruce_log,stripped_oak_wood,stripped_oak_log", (x0 - 4, z0 - 4, x1 + 4, z1 + 4, "a bridge on Route 45"),
            G["PIERS"], ys(d + 1, d + 3))
    # The lamps up Route 45 white, New Bark's; Blackthorn's ("the off-red again") a warm brown; the NATIONAL PARK's
    # posts its own white too (18:34: "if you're going to do the lampshade, can you also do the stands").
    for x, y, z in R45_LAMPS:
        b = (x - 1, z - 1, x + 1, z + 1, "a lamp up Route 45")
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet,sea_lantern", b,
            "block/tuff=lamp_white_post,block/polished_tuff=lamp_white_post,block/chiseled_tuff_bricks=lamp_white_post,block/chiseled_tuff_bricks_top=lamp_white_post,"
            "block/gray_wool=lamp_cer_top,block/sea_lantern=lamp_globe_warm!", ys(y - 8, y + 2))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ys(y - 8, y + 2))
    NP = (-2152, 448, -2090, 515)
    for x, y0, z, y1 in D["r17_posts"]:
        b = (x - 2, z - 2, x + 2, z + 2, "a lamp")
        if NP[0] <= x <= NP[2] and NP[1] <= z <= NP[3]:
            post, foot, cap, glow = "lamp_park_post", "lamp_park_foot", "lamp_park_cap", "lamp_globe_warm"
        elif -1600 <= x <= -1400 and 290 <= z <= 450:
            post, foot, cap, glow = "lamp_bt_post", "lamp_bt_foot", "lamp_bt_cap", "lamp_globe_warm"
        else: continue
        add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern,redstone_lamp", b,
            "block/red_sandstone=%s,block/chiseled_red_sandstone=%s,block/red_sandstone_top=%s,block/orange_wool=%s,block/sea_lantern=%s!,"
            "block/redstone_lamp=%s!,block/redstone_lamp_on=%s!" % (post, foot, cap, cap, glow, glow, glow), ys(y0 - 2, y1 + 3))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ys(y0 - 2, y1 + 3))
    # The rocks by the Ice Path's mouth in Blackthorn (18:25).
    add("swap", BOULDER_BLOCKS + ",cobblestone_stairs,andesite_wall", (-1427, 338, -1420, 346, "the rocks by the Ice Path"), BOULDER, ys(28, 36))
    # Earl's Academy (18:25): its bamboo trapdoor doors paper screens, as the tower's.
    add("swap", "bamboo_trapdoor", (-1835, 540, -1832, 542, "Earl's Academy's doors"), "block/bamboo_trapdoor=sprout_screen", ys(22, 25))
    # The SPROUT TOWER (18:25): the stone lanterns across the bridge as the ones leading up; the spire (its rod, its
    # gold, the rod at its tip); the beams' buttons the timber.
    for x0, z0, x1, z1 in ((-1856, 494, -1852, 496), (-1842, 494, -1838, 496)):
        b = (x0, z0, x1, z1, "a stone lantern by the SPROUT TOWER")
        add("swap", "cauldron,hopper,blackstone_wall,anvil,chipped_anvil,damaged_anvil", b,
            "block/cauldron_side=lantern_stone,block/cauldron_top=lantern_stone_top,block/cauldron_bottom=lantern_stone,block/cauldron_inner=lantern_stone,"
            "block/hopper_outside=lantern_stone,block/hopper_inside=lantern_stone,block/hopper_top=lantern_stone_top,block/blackstone=lantern_stone,"
            "block/blackstone_top=lantern_stone_top,block/anvil=lantern_stone_top,block/anvil_top=lantern_stone_top", ys(21, 26))
        add("swap", "chiseled_nether_bricks", b, "block/chiseled_nether_bricks=lantern_stone_window", ys(21, 26))
    for bx, k, ylo, yhi in (((-1861, 466, -1833, 494, "the SPROUT TOWER"), "sprout", 22, 76), ((-1992, 337, -1964, 366, "the TIN TOWER"), "tin", 20, 84)):
        add("swap", "polished_deepslate_wall", bx, "block/polished_deepslate=spire_metal", ys(ylo + 8, yhi))
        add("swap", "gold_block", bx, "block/gold_block=spire_gold", ys(ylo + 30, yhi))
        add("swap", "lightning_rod,exposed_lightning_rod,weathered_lightning_rod,oxidized_lightning_rod,waxed_lightning_rod,waxed_oxidized_lightning_rod", bx,
            "block/lightning_rod=spire_gold,block/oxidized_lightning_rod=spire_gold,block/exposed_lightning_rod=spire_gold,block/weathered_lightning_rod=spire_gold",
            ys(ylo + 30, yhi))
        add("swap", "dark_oak_button", bx, "block/dark_oak_planks=%s_timber" % k, ys(ylo, yhi))
    # The Lake of Rage (18:28): the GYARADOS's tooth (a sea lantern left), its two lamps on the south shore.
    add("swap", "sea_lantern", (-1775, 228, -1773, 230, "the GYARADOS's tooth"), "block/sea_lantern=gy_teeth", ys(28, 30))
    for x, y, z in ((-1803, 31, 247), (-1748, 31, 250)):
        b = (x - 2, z - 2, x + 2, z + 2, "a lamp by the Lake of Rage")
        add("swap", "diorite_wall,polished_diorite,light_gray_carpet,sea_lantern", b,
            "block/diorite=lamp_north_post,block/polished_diorite=lamp_north_foot,block/light_gray_wool=lamp_north_cap,block/sea_lantern=lamp_globe_warm!", ys(y - 9, y + 1))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ys(y - 9, y + 1))
    # Ecruteak (18:28, 18:29): the kerbs round every plot its kerb stone; every diorite trim on the ground (the steps
    # either side of the way in, the long one along the road) a pale stone trim; the gateway's base and its walls
    # dressed stone; the DANCE THEATRE's trim white, its quartz its white walls.
    EC = (-2090, 336, -1966, 432, "Ecruteak City")
    add("swap", "light_gray_concrete", EC, "block/light_gray_concrete=ecru_kerb", ys(20, 20))
    add("swap", "polished_diorite_stairs,polished_diorite_slab,polished_diorite", EC, "block/polished_diorite=ecru_trim", ys(20, 23))
    GW = (-2046, 419, -2017, 440, "Ecruteak's way in from Route 37")
    add("swap", "stone,stone_stairs,stone_slab", GW, "block/stone=ecru_base", ys(21, 24))
    add("swap", "mossy_stone_brick_wall,mossy_stone_bricks,mossy_stone_brick_slab,stone_brick_wall", GW,
        "block/mossy_stone_bricks=ecru_wall_stone,block/stone_bricks=ecru_wall_stone", ys(21, 25))
    DT = (-2031, 393, -2014, 405, "the DANCE THEATRE")
    add("swap", "polished_diorite_stairs,polished_diorite_slab,polished_diorite", DT, "block/polished_diorite=dance_trim", ys(21, 30))
    add("swap", "quartz_block,quartz_stairs,quartz_slab", DT, "block/quartz_block_side=dance_wall,block/quartz_block_top=dance_trim,block/quartz_block_bottom=dance_trim",
        ys(21, 30))
    # The banners' posts, everywhere (18:28: "the posts holding the banners has not been done at all, and I'm going to
    # assume that's universal"): the pole dark lacquer, its black base the same, its foot pale stone.
    for x, z, bot in sorted(set((p[1], p[2], p[3]) for p in json.load(open(os.path.join(HERE, "area_poles59.json"))))):
        add("swap", "diorite_wall,polished_tuff_wall,polished_diorite", (x, z, x, z, "a banner's pole"),
            "block/diorite=banner_pole,block/polished_tuff=banner_pole,block/polished_diorite=banner_foot", ys(bot - 9, bot - 1))
    # The NATIONAL PARK's gate (18:34: "doesn't look done ... that needs retexturing"): a warm red tiled roof with
    # darker ridges, its walls cream render, its timbers dark, its stone grey, the teal trim, dark windows.
    NG = (-2130, 541, -2085, 557, "the NATIONAL PARK's gate")
    y = ys(19, 33)
    for blocks, swaps in (("resin_bricks,resin_brick_slab,resin_brick_stairs", "block/resin_bricks=npg_roof@2"),
                          ("resin_brick_wall", "block/resin_bricks=npg_ridge"),
                          ("light_gray_concrete", "block/light_gray_concrete=npg_wall"),
                          ("stripped_oak_log,stripped_oak_wood,stripped_spruce_wood,stripped_spruce_log,stripped_jungle_log,stripped_jungle_wood",
                           "block/stripped_oak_log=npg_timber,block/stripped_oak_log_top=npg_timber,block/stripped_spruce_log=npg_timber,block/stripped_spruce_log_top=npg_timber,"
                           "block/stripped_jungle_log=npg_timber,block/stripped_jungle_log_top=npg_timber"),
                          ("birch_stairs,birch_slab,birch_planks", "block/birch_planks=npg_siding"),
                          ("andesite,andesite_wall,andesite_slab,andesite_stairs,polished_andesite,polished_andesite_slab,polished_andesite_stairs,stone",
                           "block/andesite=npg_stone,block/polished_andesite=npg_stone,block/stone=npg_stone"),
                          ("black_wool", "block/black_wool=npg_dark"),
                          ("deepslate_brick_wall", "block/deepslate_bricks=station_mullion"),
                          ("cyan_terracotta,stripped_warped_stem,stripped_warped_hyphae", "block/cyan_terracotta=npg_trim,block/stripped_warped_stem=npg_trim,block/stripped_warped_stem_top=npg_trim"),
                          ("polished_deepslate_slab", "block/polished_deepslate=npg_ridge"),
                          ("end_rod", "block/end_rod=rod_clean")):
        add("swap", blocks, NG, swaps, y)
    # Every plant pot (18:36: "restylize the plant pots, and maybe you can have different colours per plant pot"):
    # terracotta, cream, sage and slate, one after another.
    POTS = ("pot_terracotta", "pot_cream", "pot_sage", "pot_slate")
    for x, y_, z, name in json.load(open(os.path.join(HERE, "area_pots59.json"))):
        add("swap", name, (x, z, x, z, "a plant pot"), "block/flower_pot=" + POTS[(x * 7 + z * 13 + y_ * 3) % 4], ys(y_, y_))
    # Goldenrod (18:36): its walls (the pink terracotta) a clean warm render; the trim under the station's top windows
    # a wooden band; its alleys their own (dark tarmac, blue drums, green bins, grey pipes, dark green railings); the
    # way out south its path all the way to the arch, no brick border under it.
    G_IN = G["GOLD_IN"]
    add("swap", "white_terracotta", G_IN, "block/white_terracotta=gold_wall", ys(20, 45))
    add("panel", "oak_shelf", (-2130, 677, -2112, 695, "the station's band under its top windows"), "block/oak_shelf=band_shelf", ys(22, 27))
    for x0, z0, x1, z1 in ALLEYS:
        b = (x0, z0, x1, z1, "a Goldenrod alley")
        add("swap", "cobblestone,cracked_stone_bricks,stone_bricks,stone,dirt_path,coarse_dirt,gravel,andesite,mossy_cobblestone", b,
            "block/cobblestone=alley_floor@2,block/cracked_stone_bricks=alley_floor@2,block/stone_bricks=alley_floor@2,block/stone=alley_floor@2,"
            "block/dirt_path_top=alley_floor@2,block/dirt_path_side=alley_floor@2,block/coarse_dirt=alley_floor@2,block/gravel=alley_floor@2,"
            "block/andesite=alley_floor@2,block/mossy_cobblestone=alley_floor@2", ys(19, 19))
        add("swap", "barrel", b, "block/barrel_side=alley_drum,block/barrel_top=alley_drum_top,block/barrel_top_open=alley_drum_top,block/barrel_bottom=alley_drum_top", ys(20, 26))
        add("swap", "composter", b, "block/composter_side=alley_bin,block/composter_top=alley_bin_top,block/composter_bottom=alley_bin_top", ys(20, 26))
        add("swap", "end_rod", b, "block/end_rod=alley_pipe", ys(20, 30))
        add("swap", "pale_oak_fence,oak_fence,spruce_fence,dark_oak_fence", b,
            "block/pale_oak_planks=alley_fence,block/oak_planks=alley_fence,block/spruce_planks=alley_fence,block/dark_oak_planks=alley_fence", ys(20, 26))
        add("swap", "iron_chain", b, "block/iron_chain=chain_steel", ys(20, 30))
        add("swap", "stone_button", b, "block/stone=bolt", ys(20, 26))
    SX = (-2108, 750, -2099, 765, "Goldenrod's way out south")
    add("swap", "cyan_terracotta,light_gray_concrete", SX, "block/cyan_terracotta=path_kanto@4,block/light_gray_concrete=path_kanto@4", ys(19, 19))
    add("grass_onto", "cyan_terracotta,light_gray_concrete", SX, "-", ys(19, 19))
    add("swap", "terracotta", (-2109, 746, -2098, 747, "under Goldenrod's south arch"), "block/terracotta=gold_pave", ys(19, 19))
    # The Day Care (18:39: "you can see the sea lanterns inside ... get rid of the lights"): its windows dark glass.
    add("swap", "sea_lantern", (-2093, 775, -2070, 793, "the Day Care's windows"), "block/sea_lantern=glass_dark", ys(21, 26))
    # The Slowpoke Well (18:39: "you haven't done the well"): its shingled roof, its grey stone, its cover.
    WB = (-1963, 849, -1951, 859, "the Slowpoke Well")
    add("swap", "oak_stairs,oak_slab,oak_planks", WB, "block/oak_planks=well_roof", ys(19, 25))
    add("swap", "andesite,andesite_stairs,andesite_slab,andesite_wall,stone_bricks,stone_brick_wall,stone_brick_slab,stone_brick_stairs,cobblestone,cobblestone_wall", WB,
        "block/andesite=well_stone,block/stone_bricks=well_stone,block/cobblestone=well_stone", ys(14, 25))
    add("swap", "spruce_trapdoor", WB, "block/spruce_trapdoor=well_cover", ys(14, 18))
