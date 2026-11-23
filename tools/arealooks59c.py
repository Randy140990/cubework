"""
0.59.3's looks, for tools/arealooks.py: round59c() adds the rows (after round59b). John's 20:35 and 20:47 rounds
(notes/feedback-2026-11-23-2035.md, -2047.md); the pictures are tools/looks59.py's round59c().
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


# The WHIRL ISLANDS' four islands (their mounds above the water) and their whirlpools (20:35: "1 per entrance"): the
# rock each is drawn by, and where its middle is from that rock.
WHIRL_ISLANDS = [(-2490, 712, -2438, 750, "the WHIRL ISLANDS' north-west island"), (-2406, 710, -2356, 754, "the WHIRL ISLANDS' north-east island"),
                 (-2480, 765, -2428, 802, "the WHIRL ISLANDS' south-west island"), (-2400, 770, -2352, 802, "the WHIRL ISLANDS' south-east island")]
WHIRLPOOLS = [("tuff", (-2452, 17, 716), (-1, -4)), ("tuff", (-2358, 17, 734), (-3, -1)), ("andesite", (-2399, 17, 793), (-4, 3)),
              ("diorite_slab", (-2472, 17, 778), (2, -5))]
# The blue signs (a warped shelf on a pale oak one): the top's x0, x1, y, z.
BLUE_SIGNS = [(-2360, -2360, 19, 597), (-2503, -2503, 19, 767), (-2075, -2074, 21, 835)]


def round59c(add, room, G):
    BOULDER, BOULDER_BLOCKS = G["BOULDER"], G["BOULDER_BLOCKS"]

    # ---------- Cinnabar (20:47) ----------
    # Its two lamps one lamp ("all the lights on the island need to follow one universal"): the left one's post stood
    # outside its box.
    for x0, x1 in ((-801, -795), (-771, -765)):
        add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern", (x0, 931, x1, 935, "a Cinnabar lamp"),
            "block/red_sandstone=lamp_cin_post,block/chiseled_red_sandstone=lamp_cin_foot,block/red_sandstone_top=lamp_cin_cap,block/orange_wool=lamp_cin_cap,"
            "block/sea_lantern=lamp_globe_warm!", ys(16, 26))
        add("glass", "black_stained_glass_pane", (x0, 931, x1, 935, "a Cinnabar lamp"), "block/black_stained_glass=glass_lamp", ys(16, 26))
    # The LAB: its window sills white; the floor seen through its door its walls' eggshell (its walls one seamless
    # eggshell now: looks59).
    add("swap", "mangrove_slab", (-810, 923, -799, 925, "the LAB's window sills"), "block/mangrove_planks=lab_sill", ys(19, 19))
    add("swap", "white_concrete,white_carpet", (-810, 917, -799, 924, "the LAB's floor"), "block/white_concrete=lab_cin_wall,block/white_wool=lab_cin_wall", ys(18, 19))

    # ---------- the S.S. ANNE (20:47: "do not texture those"): the ship as John built it, its stand, legs and gangway
    # (0.4x) still ours ----------
    anne = json.load(open(os.path.join(HERE, "area_anne59.json")))
    for x in range(-450, -265, 16):
        b = (x, 619, min(-266, x + 15), 662, "the S.S. ANNE, as John built it")
        add("keep", ",".join(anne["low"]), b, "-", ys(21, 37))
        add("keep", ",".join(anne["high"]), b, "-", ys(38, 70))

    # ---------- Vermilion's window sills (20:47: "they're still using the old slabs") ----------
    add("swap", "acacia_slab", (-402, 546, -300, 614, "Vermilion's window sills"), "block/acacia_planks=trim_ver", ys(31, 34))
    # ...and anywhere else John put slabs in windows (a sweep of every window on the maps): Saffron's quartz sills
    # its clean white stone, the LEAGUE gate's its pale step, the lighthouse's lantern room's white, the MOOMOO FARM's
    # shutter its timber.
    add("swap", "smooth_quartz_slab,smooth_quartz,quartz_block,quartz_slab,quartz_stairs", (-395, 389, -305, 459, "Saffron's window sills"),
        "block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean", ys(33, 66))
    add("swap", "polished_diorite_slab", (-930, 505, -905, 542, "the POKeMON LEAGUE gate's windows"), "block/polished_diorite=step_cer", ys(29, 34))
    add("swap", "pale_oak_stairs", (-2228, 574, -2216, 586, "the lighthouse's lantern room"), "block/pale_oak_planks=lh_white", ys(50, 52))
    add("swap", "dark_oak_trapdoor", (-2336, 371, -2299, 396, "the MOOMOO FARM's shutters"), "block/dark_oak_trapdoor=barn_timber", ys(22, 31))

    # ---------- the WHIRL ISLANDS (20:35) ----------
    for b in WHIRL_ISLANDS:
        add("swap", BOULDER_BLOCKS + ",cobblestone_stairs,andesite_wall,andesite_stairs,diorite_slab,cobblestone_wall,stone_bricks", b, BOULDER, ys(18, 26))
    for block, (x, y, z), (dx, dz) in WHIRLPOOLS:
        add("whirlpool", block, (x, z, x, z, "a whirlpool"), "%d,%d,5" % (dx, dz), ys(y, y))

    # ---------- the signs (20:35) ----------
    for x0, x1, y, z in BLUE_SIGNS:
        add("sign_b", "warped_shelf", (x0, z, x1, z, "a blue sign"), "-", ys(y, y))
        add("sign", "pale_oak_shelf", (x0, z, x1, z, "a blue sign's stand"), "-", ys(y - 1, y - 1))

    # ---------- Route 40's beach (20:35: "the benches, the sun towels, and the umbrella") ----------
    BE = (-2376, 584, -2328, 609, "Route 40's beach")
    add("swap", "spruce_slab", BE, "block/spruce_planks=bench_slats", ys(17, 19))
    add("swap", "spruce_trapdoor", BE, "block/spruce_trapdoor=bench_frame", ys(17, 19))
    add("swap", "white_carpet", BE, "block/white_wool=beach_towel", ys(18, 18))
    add("swap", "blue_carpet,cyan_carpet,light_blue_carpet", BE,
        "block/blue_wool=umbrella_blue,block/cyan_wool=umbrella_blue,block/light_blue_wool=umbrella_white", ys(22, 24))
    add("swap", "iron_bars", BE, "block/iron_bars=umbrella_pole", ys(18, 23))

    # ---------- Olivine's seafront (20:35) ----------
    SF = (-2330, 581, -2234, 600, "Olivine's seafront")
    # Its sea wall: a pale stone cap, white posts, the white rails.
    add("swap", "diorite", (-2330, 598, -2234, 600, "Olivine's sea wall"), "block/diorite=ol_wall_cap", ys(18, 18))
    add("swap", "diorite_wall", (-2330, 598, -2234, 600, "Olivine's sea wall"), "block/diorite=ol_post", ys(19, 20))
    # Its tree planters: pale stone, their soil, the trunks bark.
    add("swap", "polished_diorite_slab,polished_diorite", (-2330, 592, -2234, 599, "Olivine's tree planters"), "block/polished_diorite=ol_planter", ys(19, 19))
    add("swap", "coarse_dirt", SF, "block/coarse_dirt=planter_soil", ys(18, 18))
    add("swap", "spruce_fence", (-2330, 592, -2234, 599, "Olivine's seafront trees"), "block/spruce_planks=tree_bark", ys(19, 24))
    # Its benches: wooden slats on dark green ends.
    add("swap", "polished_diorite_slab", (-2330, 589, -2234, 591, "Olivine's seafront benches"), "block/polished_diorite=bench_seat_cel", ys(19, 19))
    add("swap", "pale_oak_trapdoor", (-2330, 589, -2234, 591, "Olivine's seafront benches"), "block/pale_oak_trapdoor=bench_end_cel", ys(19, 19))
    # The white line along the boardwalk's edge.
    add("swap", "white_carpet", (-2330, 581, -2234, 583, "Olivine's boardwalk edge"), "block/white_wool=ol_kerb", ys(19, 19))
    # Its steps down to the beach and its trims (the polished granite) a pale stone step.
    add("swap", "polished_granite,polished_granite_slab,polished_granite_stairs", (-2340, 495, -2230, 625, "Olivine's steps and trims"),
        "block/polished_granite=ol_step", ys(17, 19))
    # The shelves round the avenue's greenery (20:35: "supposed to obviously be the stand") wooden planter boards.
    for z0, z1 in ((517, 536), (556, 574)):
        add("panel", "pale_oak_shelf", (-2284, z0, -2277, z1, "the avenue's planters"), "block/pale_oak_shelf=planter_wood", ys(18, 21))

    # ---------- Ecruteak (20:47) ----------
    # The BURNED TOWER's way in: its pale oak, its buttons, the tower's charred timber.
    BW = (-2068, 362, -2054, 372, "the BURNED TOWER's way in")
    add("swap", "pale_oak_trapdoor,pale_oak_stairs,pale_oak_slab,pale_oak_planks", BW,
        "block/pale_oak_trapdoor=burned_timber,block/pale_oak_planks=burned_timber", ys(20, 32))
    add("swap", "dark_oak_button", BW, "block/dark_oak_planks=burned_timber", ys(20, 32))
