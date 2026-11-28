#!/usr/bin/env python3
"""
(0.78) John's 28 Nov 14:54 round: the DARK CAVE's and the ICE PATH's floors (tools/caves78.py; the interiors, x 19456
on, 256 apart) in the caves' own looks, and the looks he asked for outside.

Called from tools/arealooks.py after the 0.76 rows (so these win where they overlap).
"""

DARK = [(19456, 138, 140, "Violet side"), (19712, 146, 142, "Blackthorn side")]
ICE = [(19968, 124, 140, "1F"), (20224, 80, 120, "B1F"), (20480, 76, 66, "B2F Mahogany side"), (20736, 62, 76, "B2F Blackthorn side"),
       (20992, 76, 52, "B3F")]
MAT = "block/smooth_sandstone=cave_mat@2,block/smooth_sandstone_top=cave_mat@2,block/sandstone_top=cave_mat@2"


def caves(room):
    # The DARK CAVE (HeartGold's): its floor slate blue-grey with darker patches, its cliffs brown-grey rock in ledges
    # with pale rock on their tops, its steps cut stone, its mats the caves' sandstone.
    DC = ("block/cyan_terracotta=ms_floor@4,block/tuff=ms_ground@4,block/polished_deepslate=ms_patch@2,block/dripstone_block=rt_rock_top|rt_rock@2,"
          "block/brown_mushroom_block=rt_rock_top@2,block/mushroom_stem=rt_rock_top@2,block/mushroom_block_inside=rt_rock_top@2,"
          "block/mud_bricks=ms_step,block/mud_brick_stairs=ms_step," + MAT)
    for ox, w, d, fl in DARK:
        box = (ox - 1, -1, ox + w, d, "Dark Cave, " + fl)
        room("swap", "cyan_terracotta,tuff,polished_deepslate,dripstone_block,brown_mushroom_block,mushroom_stem,mud_bricks,mud_brick_stairs,smooth_sandstone", box, DC)
    # The ICE PATH (HeartGold's): its walls packed snow and ice, its floor frosted stone with blue ice patches, the
    # slide-on ice glassy and pale (you slide on it), its steps snow, its cracked rock (ROCK SMASH) as Rock Tunnel's.
    IP = ("block/packed_ice=sf_ice_top|sf_ice@2,block/snow_block=sf_snow_top|sf_snow@2,block/light_blue_concrete=sf_floor@4,block/lapis_block=sf_ice_top@2,"
          "block/blue_ice=sf_step@2,block/polished_diorite=sf_snow_top@2,block/polished_diorite_stairs=sf_snow_top@2," + MAT)
    for ox, w, d, fl in ICE:
        box = (ox - 1, -1, ox + w, d, "Ice Path " + fl)
        room("swap", "packed_ice,snow_block,light_blue_concrete,lapis_block,blue_ice,polished_diorite,polished_diorite_stairs,smooth_sandstone", box, IP)
        room("boulder", "waxed_weathered_copper_grate", box, "prefix:sboulder")


def ys(a, b):
    return (str(a), str(b))


def round78(add, room, G):
    caves(room)
    # GOLDENROD's street, the whole way through: the plaza's own look ("to the left, the middle is correct. And then to
    # the right, underneath the sign, it starts to tile. You need to stop it being a tile. It needs to be the same as the
    # left."): the glazed terracotta of every street the plaza's seamless gold flags, not the yellow tiles; the
    # white-edged squares in it stay the pavements' stony cream (0.76).
    add("swap", "brown_glazed_terracotta", G["GOLD_IN"], "block/brown_glazed_terracotta=gold_plaza_gold@4", ys(18, 19))
    import arealooks76
    arealooks76.round76(add, G)
    # The foam at the foot of ROUTE 47's two waterfalls, for the cobwebs there ("make it seem a bit bubbly").
    add("swap", "cobweb", (-2664, 774, -2615, 784, "the foot of Route 47's waterfalls"), "block/cobweb=wf_foam", ys(14, 30))
    # Barrels everywhere (Cianwood's coast, behind Olivine's GYM, ...: "barrels need doing across the board"): the
    # honey-wood barrels with dark hoops already on Goldenrod's and Saffron's, all over both regions; Vermilion's harbour
    # keeps its own.
    BARREL = "block/barrel_side=crate_barrel_side,block/barrel_top=crate_barrel_top,block/barrel_top_open=crate_barrel_top,block/barrel_bottom=crate_barrel_top"
    add("swap", "barrel", G["JOHTO"], BARREL)
    add("swap", "barrel", G["KANTO"], BARREL)
    add("swap", "barrel", G["VERMILION"], "block/barrel_side=barrel_ver_side,block/barrel_top=barrel_ver_top,block/barrel_top_open=barrel_ver_top,block/barrel_bottom=barrel_ver_top")
    # OLIVINE's pier: its checkered first stretch (pale oak boards and stripped pale oak by turns) all the boards, the
    # same as the boardwalk ("just the boards the exact same"); past the line, the steps' studs (stone buttons) the
    # piers' steel bolts.
    add("swap", "stripped_pale_oak_log,stripped_pale_oak_wood", (-2281, 599, -2274, 604, "the first stretch of Olivine's pier"),
        "block/stripped_pale_oak_log=ol_boardwalk@4,block/stripped_pale_oak_log_top=ol_boardwalk@4", ys(17, 18))
    add("swap", "stone_button", (-2281, 605, -2274, 620, "the steps of Olivine's pier"), "block/stone=pier_bolt", ys(18, 20))
    # Its gate on the sea: the floor through it Olivine's paving ("not retextured on the floor"), its chains white, as
    # the pier's.
    add("swap", "white_terracotta,yellow_terracotta,white_concrete,light_gray_concrete,terracotta", (-2284, 619, -2270, 625, "the floor through Olivine's sea gate"),
        "block/white_terracotta=ol_pave@2,block/yellow_terracotta=ol_pave@2,block/white_concrete=ol_pave@2,block/light_gray_concrete=ol_pave@2,block/terracotta=ol_pave@2",
        ys(18, 18))
    add("swap", "iron_chain", (-2283, 598, -2272, 622, "the chains of Olivine's pier and sea gate"), "block/iron_chain=chain_white", ys(18, 22))
    # Olivine at night: the chains hung between its lamps white ("I'm more than happy that they are probably a white"),
    # and its two hedge planters' rims (pale oak shelves, brown on one side and white on the other) white all round
    # ("I believe they should be white, and same with the one further up as well").
    OLIVINE = (-2335, 495, -2170, 625, "Olivine City")
    CHAINS = ",".join("block/%scopper_chain=chain_white" % c for c in ("", "exposed_", "weathered_", "oxidized_"))
    add("swap", "waxed_oxidized_copper_chain,oxidized_copper_chain,waxed_copper_chain,copper_chain,waxed_weathered_copper_chain,waxed_exposed_copper_chain",
        OLIVINE, CHAINS, ys(20, 32))
    for z0, z1 in ((517, 536), (556, 575)):
        add("swap", "pale_oak_shelf", (-2279, z0, -2274, z1, "an Olivine hedge planter's rim"), "block/pale_oak_shelf=trim_white", ys(17, 22))
    # MAHOGANY TOWN's floor: one paving, its grey flags, all over ("I'm not too keen on this flooring where it's
    # checkered"): the white concrete between them too.
    MAH = (-1800, 370, -1725, 435, "Mahogany Town")
    add("swap", "white_concrete,light_gray_concrete,chiseled_quartz_block,smooth_quartz,white_terracotta", MAH,
        "block/white_concrete=mah_pave@2,block/light_gray_concrete=mah_pave@2,block/chiseled_quartz_block_top=mah_pave@2,block/chiseled_quartz_block=mah_pave@2,"
        "block/quartz_block_bottom=mah_pave@2,block/white_terracotta=mah_pave@2", ys(21, 21))
    add("grass_onto", "white_concrete,light_gray_concrete,chiseled_quartz_block,smooth_quartz,white_terracotta", MAH, "-", ys(21, 21))
