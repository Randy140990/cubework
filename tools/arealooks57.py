"""
0.57.0's looks (John's review, 15:17 to 16:35), for tools/arealooks.py: round57() adds the rows (called at the end of
round50's chain, so the rows for all Kanto are copied onto Route 28, Goldenrod and all Johto as the others are) and
finish() turns John's shelves used as trims into the plain boards (client/AreaLooks' "panel"). The pictures are
tools/looks57.py's; the places come from the scans in tools/area_scan57.json (every building on the Johto maps found by
its windows and doors, every wooden bridge, every rock cluster, the lamps, the copper lanterns).
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
SCAN = json.load(open(os.path.join(HERE, "area_scan57.json")))

# The shelves whose swapped picture is a trim, cap, band or planter: drawn as a plain board (the signs, the Fan Club's
# arch and the awnings keep their shelf; they're pictures laid over the shelves' parts).
PANEL_PICTURES = {"green_shelf", "wood_shelf", "sz_green_shelf", "shelf_wood", "cap_shelf", "band_shelf", "wood_shelf_dark", "bridge_beam",
                  "planter_rim", "planter_saf_cream", "planter_saf_wood", "planter_saf_terra", "lav_planter", "pier_frame", "panel_planter_dark",
                  "panel_cap_dark", "panel_white", "panel_teal", "panel_red", "panel_gold", "panel_cream", "panel_blue", "panel_wood_red", "rope_tan",
                  "cian_trim", "lh_rail", "vio_bridge_beam", "sprout_rail", "tin_rail", "burned_rail", "barn_trim", "mansion_trim", "lab_cin_trim", "fan_bow",
                  "fan_bow_ball", "stand_counter"}


def finish(rows, G):
    """Rows of swaps of shelves (only shelves, to a board's picture) become "panel" rows: the same swaps, the shelf drawn
    as a flat board. (The rivalry between areas is the same for both: the smaller area's is drawn.) And (15:46: "the
    end of the path and the fence and the last pillar and wall in the water ... needs extending") Vermilion's looks run
    on to its east edge, past where its box ended."""
    V = G["VERMILION"]
    rows = rows + [(r[0], r[1], -299, r[3], -284, r[5]) + tuple(r[6:]) for r in rows if len(r) == 10 and tuple(r[2:6]) == V[:4]]
    out = []
    for r in rows:
        if r[0] == "swap" and r[8] != "-":
            pairs = [p.split("=", 1) for p in r[8].split(",") if "=" in p]
            shelf_pairs = [(a, b) for a, b in pairs if a.endswith("_shelf")]
            if shelf_pairs and all(b.split("@")[0].split("|")[0].rstrip("!") in PANEL_PICTURES for a, b in shelf_pairs):
                r = ("panel",) + tuple(r[1:])
        out.append(r)
    return out


def box(b, note, pad=0):
    return (b[0] - pad, b[1] - pad, b[2] + pad, b[3] + pad, note)


def ys(a, b):
    return (str(a), str(b))


def round57(add, room, G):
    K, J = G["KANTO"], G["JOHTO"]
    SEA_ROCK, SEA_ROCK_BLOCKS, BOULDER, BOULDER_BLOCKS = G["SEA_ROCK"], G["SEA_ROCK_BLOCKS"], G["BOULDER"], G["BOULDER_BLOCKS"]
    TRIM, PALLET_KIT, CHIMNEY_CAP = G["TRIM"], G["PALLET_KIT"], G["CHIMNEY_CAP"]

    # ---------- everywhere (Kanto; copied to Johto, Route 28 and Goldenrod) ----------
    # The copper lanterns (16:04: "a slightly upgraded texture"): the garden lights.
    cu = ",".join(w + c + "copper_lantern" for w in ("", "waxed_") for c in ("", "exposed_", "weathered_", "oxidized_"))
    add("swap", cu, K, ",".join("block/%scopper_lantern=garden_light" % c for c in ("", "exposed_", "weathered_", "oxidized_")))
    # The planters' cherry saplings (15:46): a round flowering shrub.
    add("swap", "cherry_sapling,potted_cherry_sapling", K, "block/cherry_sapling=shrub_pink")
    # The moss's soft edge onto the cobblestone it borders (15:46, Route 1 and Route 2).
    add("moss_onto", "cobblestone,cobblestone_slab,cobblestone_stairs", K)
    # Every Gym's dark middle column at its door (15:46: "the black brick in the centre of the gym entrances").
    for g in G["GYMS"]:
        add("swap", "deepslate_tiles,deepslate_tile_wall,deepslate_tile_slab,deepslate_tile_stairs", g, "block/deepslate_tiles=gym_divider")
    # The stands with the plant pots (16:04: "tailor every one of those throughout to be the same"): their posts and
    # frame dark timber, their counters clean wood, their roofs (John's campfires) wooden slats, no fire showing.
    stands = [(-262, 785, -260, 791)] + [(a, b, c, d) for a, b, c, d in ((-2539, 736, -2537, 742), (-2319, 586, -2317, 592), (-2281, 546, -2275, 548),
                                                                     (-2278, 439, -2276, 445), (-2095, 513, -2093, 519), (-2046, 398, -2040, 400),
                                                                     (-2036, 875, -2034, 881), (-1690, 414, -1684, 416))]
    for x0, z0, x1, z1 in stands:
        b = (x0 - 2, z0 - 2, x1 + 2, z1 + 2, "a stand with plant pots")
        add("swap", "spruce_fence,spruce_fence_gate", b, "block/spruce_planks=stand_post")
        add("swap", "jungle_stairs,jungle_planks,jungle_slab", b, "block/jungle_planks=stand_counter")
        add("swap", "campfire", b, "block/campfire_log=stand_roof,block/campfire_log_lit=stand_roof,block/campfire_fire=clear")
    # Every route's rocks (15:46, 15:51: "the individual stones need updating ... across the board"; "rocks at route
    # transitions ... clear line where sections divide"): each rock the scans found, in the sea as the sea's rocks, on
    # land as the boulders.
    # ((0.59.4, 07:31) Not the lamps' posts, nor Victory Road's walls, which the scans took for rocks: Cerulean's lamps
    # "a stone design", Route 23's tops "this weird rock".)
    skip = {tuple(b) for b in json.load(open(os.path.join(HERE, "area_rocks_skip594.json")))}
    for mp in ("rocks_kanto", "rocks_johto"):
        for r in SCAN[mp]:
            x0, z0, x1, z1, y0, y1 = r["box"]
            if r["kind"] != "sea" and (x0, z0, x1, z1, y0, y1) in skip: continue
            b = (x0 - 1, z0 - 1, x1 + 1, z1 + 1, "a rock in the water" if r["kind"] == "sea" else "a rock")
            if r["kind"] == "sea": add("swap", SEA_ROCK_BLOCKS, b, SEA_ROCK, ys(y0 - 1, y1 + 1))
            else: add("swap", BOULDER_BLOCKS, b, BOULDER, ys(y0 - 1, y1 + 1))
    for kind, boxes in (("sea", json.load(open(os.path.join(HERE, "area_rocks57.json")))["sea"]),
                        ("land", json.load(open(os.path.join(HERE, "area_rocks57.json")))["land"])):
        for x0, z0, x1, z1, y0, y1 in boxes:
            if kind == "land" and (x0, z0, x1, z1, y0, y1) in skip: continue
            b = (x0 - 1, z0 - 1, x1 + 1, z1 + 1, "a rock in the water" if kind == "sea" else "a rock")
            add("swap", SEA_ROCK_BLOCKS if kind == "sea" else BOULDER_BLOCKS, b, SEA_ROCK if kind == "sea" else BOULDER, ys(y0 - 1, y1 + 1))
    # The open sea's rocks all over (the Whirl Islands' chains, Routes 40 and 41, Cherrygrove's bay, Olivine's harbour,
    # Cianwood's shore, Routes 47 and 48, Route 19 to 21): everything rocky standing in the sea.
    for b in ((-2490, 560, -2320, 910, "the Whirl Islands and Routes 40 and 41"), (-1800, 790, -1640, 860, "Cherrygrove's bay"),
              (-2335, 540, -2170, 680, "Olivine's harbour"), (-2600, 690, -2480, 820, "Cianwood's shore"), (-2770, 600, -2590, 820, "Routes 47 and 48"),
              (-900, 735, -460, 960, "the sea from Pallet to Fuchsia")):
        add("swap", SEA_ROCK_BLOCKS + ",end_stone_bricks", b, SEA_ROCK + ",block/end_stone_bricks=sea_rock_top|sea_rock@2", ys(10, 19))
    # The caves' dripstone hanging in their mouths: the look pack's (grey stone points, tools/looks57.py).

    # ---------- Pallet to Vermilion (15:46) ----------
    # The INDIGO PLATEAU's statues: a stone POKe BALL on each stone pedestal (the copper golems gone).
    for (x, y, z) in ((-920, 28, 220), (-920, 28, 226), (-919, 32, 204), (-919, 32, 207), (-919, 36, 194), (-919, 36, 197), (-919, 40, 184), (-919, 40, 187),
                      (-917, 28, 214), (-917, 28, 232), (-917, 36, 178), (-917, 36, 180), (-916, 32, 204), (-916, 32, 207), (-916, 36, 194), (-916, 36, 197),
                      (-916, 40, 184), (-916, 40, 187), (-909, 32, 204), (-909, 32, 207), (-909, 36, 178), (-909, 36, 180), (-909, 36, 194), (-909, 36, 197),
                      (-909, 40, 184), (-909, 40, 187), (-908, 28, 214), (-908, 28, 232), (-906, 32, 204), (-906, 32, 207), (-906, 36, 194), (-906, 36, 197),
                      (-906, 40, 184), (-906, 40, 187), (-905, 28, 220), (-905, 28, 226)):
        b = (x - 1, z - 1, x + 1, z + 1, "a statue's pedestal")
        add("swap", "waxed_cut_copper,waxed_cut_copper_stairs,waxed_weathered_cut_copper_stairs,waxed_weathered_cut_copper,waxed_copper_grate,waxed_weathered_copper_grate",
            b, "block/cut_copper=statue_stone,block/weathered_cut_copper=statue_stone,block/copper_grate=statue_stone,block/weathered_copper_grate=statue_stone",
            ys(y - 4, y - 1))
        add("prop", "waxed_cut_copper,waxed_cut_copper_stairs,waxed_weathered_cut_copper,waxed_weathered_cut_copper_stairs", (x, z, x, z, "a statue"), "ball_statue",
            ys(y - 1, y - 1))
    # The Museum's roof (15:46: "purple concrete/terracotta"): its tiles.
    add("swap", "purple_terracotta,purple_concrete", (-817, 267, -788, 278, "the Museum's roof"), "block/purple_terracotta=roof_museum_tiles,block/purple_concrete=roof_museum_tiles",
        ys(43, 46))
    # Celadon's alley (15:46: "iron bars and the rods for the lights"): dark railings, clean white rods.
    for b in ((-538, 439, -536, 441, "Celadon's alley"), (-533, 407, -530, 409, "Celadon's alley")):
        add("swap", "iron_bars", b, "block/iron_bars=railing_dark", ys(32, 37))
    add("swap", "end_rod", (-533, 405, -529, 407, "Celadon's alley's lamps"), "block/end_rod=rod_clean", ys(35, 37))
    # Vermilion's pier out to the S.S. ANNE (15:46: "hasn't been updated at all"): Let's Go's pier boards, its rails and
    # posts timber, its bolts steel, its legs the dock's.
    VP = (-362, 594, -318, 619, "the pier to the S.S. ANNE")
    add("swap", "acacia_stairs,acacia_planks,acacia_slab,stripped_acacia_wood,stripped_acacia_log", VP,
        "block/acacia_planks=pier_boards,block/stripped_acacia_log=pier_frame,block/stripped_acacia_log_top=pier_frame", ys(30, 30))
    add("swap", "acacia_shelf,stone_button", VP, "block/acacia_shelf=pier_frame,block/stone=pier_bolt", ys(30, 31))
    add("swap", "polished_blackstone_wall,andesite", VP, "block/polished_blackstone=dock_post,block/andesite=ss_leg", ys(24, 29))
    # The Fan Club's sign (15:46: "some sort of bow tie"): the bow's shelves plain pink boards round its POKe BALL knot.
    # Vermilion's east end (15:46: "the end of the path and the fence and the last pillar and wall in the water ...
    # needs extending"): the town's looks on to its east edge (finish() copies them).

    # ---------- Routes 12 to 21, Fuchsia, Cinnabar (15:51, 15:57) ----------
    # Fuchsia and the SAFARI ZONE (15:51: "The planter outside, which has like the green shelving"): their green
    # shelves the Safari Zone's green boards.
    add("swap", "warped_shelf", G["FUCHSIA"], "block/warped_shelf=sz_green_shelf")
    # The Rock Tunnel's sign (15:57), Route 28's lump in the water (buttons, granite, mud brick: one dripstone rock).
    add("sign", "pale_oak_shelf", (-141, 287, -138, 289, "the Rock Tunnel's sign"), "-", ys(32, 35))
    add("swap", "tuff,granite_stairs,mud_brick_slab,diorite_slab,cobblestone,cobblestone_stairs,andesite_wall,andesite_stairs,cobblestone_wall,jungle_trapdoor",
        (-1002, 786, -989, 795, "a rock in Route 28's water"),
        "block/tuff=drip_rock_top|drip_rock@4,block/granite=drip_rock_top|drip_rock@4,block/mud_bricks=drip_rock_top|drip_rock@4,block/diorite=drip_rock_top|drip_rock@4,"
        "block/cobblestone=drip_rock_top|drip_rock@4,block/andesite=drip_rock_top|drip_rock@4,block/jungle_trapdoor=drip_rock@4", ys(14, 20))
    add("hide", "jungle_button", (-1002, 786, -989, 795, "a rock in Route 28's water"), "-", ys(14, 20))
    # The Underground Path's huts (15:57: "just have it as one solid colour"): their dark doorways one dark.
    for x, y, z, note in G["UG_HUTS"]:
        add("swap", "black_concrete,black_wool", (x - 6, z - 6, x + 6, z + 6, note + " Underground Path hut"), "block/black_concrete=hut_dark,block/black_wool=hut_dark",
            ys(y - 2, y + 6))
    # CINNABAR ISLAND (15:51: "both piers, the lights, and all the buildings ... the mansion is probably the big one").
    C = (-895, 885, -756, 958, "Cinnabar Island")
    for b in ((-792, 892, -783, 900, "Cinnabar's north pier"), (-765, 914, -757, 923, "Cinnabar's east pier")):
        add("swap", "gray_concrete", b, "block/gray_concrete=cin_deck", ys(15, 18))
        add("swap", "spruce_trapdoor", b, "block/spruce_trapdoor=cin_edge", ys(15, 18))
        add("swap", "blackstone_wall", b, "block/blackstone=cin_post,block/blackstone_top=cin_post_top", ys(15, 20))
        add("swap", "iron_chain", b, "block/iron_chain=rope_tan", ys(15, 20))
    add("swap", "brown_concrete_powder", C, "block/brown_concrete_powder=path_kanto@4", ys(16, 18))
    for x0, z0, x1, z1 in ((-798, 932, -796, 934), (-769, 932, -767, 934)):
        add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern", (x0 - 1, z0 - 1, x1 + 1, z1 + 1, "a Cinnabar lamp"),
            "block/red_sandstone=lamp_cin_post,block/chiseled_red_sandstone=lamp_cin_foot,block/red_sandstone_top=lamp_cin_cap,block/orange_wool=lamp_cin_cap,"
            "block/sea_lantern=lamp_globe_warm!", ys(16, 26))
    for b in ((-784, 911, -781, 913, "a Cinnabar sign"), (-800, 928, -797, 930, "a Cinnabar sign")):
        add("sign", "pale_oak_shelf,mangrove_shelf", b, "-", ys(16, 21))
    # Its POKeMON MANSION: a cream render, a grey stone base, dark brown tiles, its shutters, a white trim.
    MN = (-811, 898, -790, 915, "the POKeMON MANSION")
    add("roof_mansion", "mangrove_stairs", MN, "-", ys(22, 31))
    add("swap", "mangrove_planks,mangrove_slab,pale_oak_slab,pale_oak_stairs", MN, "block/mangrove_planks=roof_mansion_tiles,block/pale_oak_planks=mansion_trim", ys(26, 31))
    add("swap", "white_terracotta", MN, "block/white_terracotta=mansion_wall", ys(18, 31))
    add("swap", "jungle_slab,smooth_stone,smooth_stone_slab", MN, "block/jungle_planks=mansion_trim,block/smooth_stone=mansion_trim,block/smooth_stone_slab_side=mansion_trim",
        ys(18, 31))
    add("swap", "stone_bricks,mossy_stone_bricks,stone_brick_wall,brick_wall,tuff_bricks,tuff_brick_stairs,light_gray_terracotta", MN,
        "block/stone_bricks=mansion_base,block/mossy_stone_bricks=mansion_base,block/bricks=mansion_base,block/tuff_bricks=mansion_base,block/light_gray_terracotta=mansion_base",
        ys(17, 31))
    add("swap", "pale_oak_trapdoor", MN, "block/pale_oak_trapdoor=mansion_shutter", ys(18, 31))
    add("swap", "black_concrete", MN, "block/black_concrete=mansion_dark", ys(18, 31))
    # Its LAB: white panels, a dark grey roof with solar panels on it, a copper-red trim, its posts.
    LB = (-812, 915, -796, 930, "the POKeMON LAB")
    add("swap", "smooth_quartz,smooth_quartz_stairs", LB, "block/quartz_block_bottom=lab_cin_wall", ys(18, 23))
    add("swap", "polished_deepslate", LB, "block/polished_deepslate=lab_cin_roof", ys(22, 25))
    add("swap", "waxed_cut_copper_stairs", LB, "block/cut_copper=lab_cin_trim", ys(21, 23))
    add("swap", "deepslate_tile_wall,stripped_mangrove_wood", LB, "block/deepslate_tiles=lab_cin_post,block/stripped_mangrove_log=lab_cin_trim", ys(23, 26))
    add("swap", "tuff_bricks,tuff_brick_stairs,stone_bricks,stone_brick_wall", LB, "block/tuff_bricks=mansion_base,block/stone_bricks=mansion_base", ys(17, 21))
    add("sign", "pale_oak_shelf", LB, "-", ys(17, 20))
    # ((0.59.6, 13:17: "make the solar panels flat on top ... a 3x2, maybe a 2 gap, a 3x2, a 2 gap, and then a 3x2")
    # Three flat arrays, each three blocks across and two deep, two blocks apart, down the roof (its ten blocks
    # deep exactly); each drawn from its front middle block.)
    # ((0.67, John's 16:28: "they should be left and right instead of the centre ... those sets to move left, and then
    # you duplicate it and have it on the right side") The three down the roof's left side, between its edge and the
    # dark rail (x -809 to -807), and the same three down its right side (x -802 to -800), each the mirror of the other
    # about the roof's middle.)
    for z in (917, 921, 925):
        for x in (-808, -801):
            add("prop", "polished_deepslate", (x, z, x, z, "a solar array on the LAB's roof"), "solar", ys(24, 24))
    # The volcano (15:57: "the bone, which is supposed to be steam ... opacity 30%"; "retexturize the lava ... flowing").
    V = (-870, 899, -826, 937, "Cinnabar's volcano")
    add("steam", "bone_block", V, "steam")
    add("swap", "magma_block", V, "block/magma=lava_flow57@2")

    # ---------- Johto ----------
    # The JOHTO box now runs north over the Lake of Rage (G["JOHTO"] changed in arealooks.py).
    # Every door crisp (16:04: "all the doors"), as all Kanto's (round46's, copied over; this is Johto's own north).
    # Every Johto lamp by its kind: the sea's white ones (Olivine, Cianwood, the coast), the NATIONAL PARK's (16:35:
    # "something different to complement the swan"), Mahogany's and the Lake's (16:04: "give a style to the lights").
    NP = (-2152, 448, -2090, 515, "the NATIONAL PARK")
    MAH = (-1800, 370, -1725, 435, "Mahogany Town")
    LAKE = (-1870, 60, -1700, 270, "the Lake of Rage")
    for L in SCAN["lamps_johto"]:
        x, y, z = L["at"]
        comp = [n for n, c in L["comp"]]
        b = (x - 1, z - 1, x + 1, z + 1, "a lamp")
        inp = NP[0] <= x <= NP[2] and NP[1] <= z <= NP[3]
        north = (MAH[0] <= x <= MAH[2] and MAH[1] <= z <= MAH[3]) or (LAKE[0] <= x <= LAKE[2] and LAKE[1] <= z <= LAKE[3]) or (-1980 <= x <= -1640 and 370 <= z <= 440)
        if "diorite_wall" in comp:
            post, cap, foot = ("lamp_north_post", "lamp_north_cap", "lamp_north_foot") if north else ("lamp_sea_post", "lamp_sea_cap", "lamp_sea_foot")
            add("swap", "diorite_wall,polished_diorite,light_gray_carpet,sea_lantern", b,
                "block/diorite=%s,block/polished_diorite=%s,block/light_gray_wool=%s,block/sea_lantern=lamp_globe_warm!" % (post, foot, cap), ys(y - 9, y + 1))
            add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ys(y - 9, y + 1))
        elif "red_sandstone_wall" in comp and inp:
            add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern", b,
                "block/red_sandstone=lamp_park_post,block/chiseled_red_sandstone=lamp_park_foot,block/red_sandstone_top=lamp_park_cap,block/orange_wool=lamp_park_cap,"
                "block/sea_lantern=lamp_globe_warm!", ys(y - 9, y + 1))
            add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ys(y - 9, y + 1))
        elif "tuff_wall" in comp and north:
            add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet", b,
                "block/tuff=lamp_north_post,block/polished_tuff=lamp_north_post,block/chiseled_tuff_bricks=lamp_north_foot,block/chiseled_tuff_bricks_top=lamp_north_foot,"
                "block/gray_wool=lamp_north_cap", ys(y - 9, y + 1))
    # The banners' cords between the posts (16:35: "a universal way to maybe hold the banners"): a dark cord, the
    # posts' tops gold. (Two banners one above the other are one long banner: client/LongBanners.)
    add("swap", "waxed_oxidized_copper_chain,waxed_copper_chain,oxidized_copper_chain,copper_chain", J,
        ",".join("block/%scopper_chain=banner_cord" % c for c in ("", "oxidized_", "exposed_", "weathered_")), ys(24, 40))
    add("swap", "bamboo_fence", J, "block/bamboo_fence=banner_pole_top,block/bamboo_fence_particle=banner_pole_top")
    # Every wooden bridge in Johto (16:04: "the old styles for the steps and the shelves which are supposed to be units";
    # 16:23 Route 32's; Route 44's; Routes 47 and 48): Route 45's treatment, one run of boards, beams, bolts.
    for br in SCAN["bridges57"]:
        x0, z0, x1, z1 = br["box"]
        if G["bridge45"](x0, z0): continue
        d = br["deck"]
        b = (x0 - 3, z0 - 3, x1 + 3, z1 + 3, "a bridge")
        add("swap", G["BRIDGE_DECK"], b, G["DECK"], ys(d, d))
        add("swap", "spruce_shelf,oak_shelf,stone_button", b, G["RAILS"], ys(d, d + 1))
        add("swap", "stripped_spruce_wood,stripped_spruce_log,stripped_oak_wood,stripped_oak_log,spruce_fence,oak_fence", b,
            G["PIERS"] + ",block/spruce_planks=bridge_beam,block/oak_planks=bridge_beam", ys(d - 14, d - 1))
    add("swap", "pale_oak_planks,pale_oak_stairs,pale_oak_slab", (-2283, 603, -2272, 622, "Olivine's pier by the lighthouse"), "block/pale_oak_planks=pier_ol", ys(17, 19))

    # Johto's houses (John's one design, as Pallet's: 16:15 "same styles as where we see everywhere else, but these
    # specific ones will need different colours"): Pallet's kit on each, its roof in its town's colour.
    ROOF_OF = {"acacia_stairs": ("roof", "acacia_slab,acacia_planks", "block/acacia_planks=roof_tiles"),
               "mangrove_stairs": ("roof_mangrove", "mangrove_slab,mangrove_planks", "block/mangrove_planks=roof_mangrove_tiles"),
               "warped_stairs": ("roof_green", "warped_slab,warped_planks", "block/warped_planks=roof_green_tiles"),
               "pale_oak_stairs": ("roof_white", "pale_oak_slab,pale_oak_planks", "block/pale_oak_planks=roof_white_tiles"),
               "prismarine_stairs": ("roof_cer", "prismarine_slab,prismarine", "block/prismarine=roof_cer_tiles"),
               "prismarine_brick_stairs": ("roof_cer", "prismarine_brick_slab,prismarine_bricks", "block/prismarine_bricks=roof_cer_tiles"),
               "crimson_stairs": ("roof_vio", "crimson_slab,crimson_planks", "block/crimson_planks=roof_vio_tiles"),
               "cobbled_deepslate_stairs": ("roof_black", "cobbled_deepslate_slab,cobbled_deepslate", "block/cobbled_deepslate=roof_black_tiles"),
               "red_nether_brick_stairs": ("roof_daycare", "red_nether_brick_slab,red_nether_bricks", "block/red_nether_bricks=roof_daycare_tiles"),
               "jungle_stairs": ("roof_azalea1", "jungle_slab,jungle_planks", "block/jungle_planks=roof_azalea1_tiles")}
    done = set()
    VC59 = (-1912, 500, -1760, 600)
    OLG59 = []
    NOT_HOUSES = [(-1861, 466, -1833, 494), (-1992, 337, -1964, 366), (-2079, 344, -2051, 372), (-2090, 336, -1966, 432), (-2215, 615, -2040, 775),
                  (-2233, 570, -2205, 590), (-2310, 675, -2260, 850)]
    for h in SCAN["houses_johto"] + SCAN["houses_north"]:
        comp = dict((n, c) for n, c in h["comp"])
        if "blue_wool" in comp or "cyan_stained_glass" in comp or "deepslate_tile_wall" in comp or "stripped_mangrove_wood" in comp and "quartz_block" in comp:
            continue
        x0, z0, x1, z1, y0, y1 = h["box"]
        if x1 - x0 > 26 or z1 - z0 > 26 or (x0, z0) in done: continue
        if any(a <= x0 <= c and b_ <= z0 <= d for a, b_, c, d in NOT_HOUSES): continue
        stairs = [n for n, c in sorted(comp.items(), key=lambda kv: -kv[1]) if n in ROOF_OF]
        if not stairs: continue
        if not ("polished_deepslate_slab" in comp or "jungle_shelf" in comp or "polished_andesite" in comp): continue
        done.add((x0, z0))
        b, yr = (x0 - 1, z0 - 1, x1 + 1, z1 + 1, "a Johto house"), ys(y0 - 1, y1 + 2)
        roof = stairs[0]
        # Azalea's two (16:23: "these specific ones will need different colours"): the one by the Center moss green,
        # Kurt's blue.
        if roof == "jungle_stairs" and x0 > -2000: look = ("roof_azalea2", "jungle_slab,jungle_planks", "block/jungle_planks=roof_azalea2_tiles")
        else: look = ROOF_OF[roof]
        # (0.59, 18:40: "variations of the purple in Violet City ... a tiny bit of character breakup") Violet's houses
        # one of four purples each.
        vio = VC59[0] <= x0 <= VC59[2] and VC59[1] <= z0 <= VC59[3]
        if look[0] == "roof_green" and -2335 <= x0 <= -2170 and 495 <= z0 <= 625:
            OLG59.append(1)
            g = ("green", "green2", "green3")[(len(OLG59) - 1) % 3]
            look = ("roof_" + g, look[1], "block/warped_planks=roof_%s_tiles" % g)
        if look[0] == "roof_vio":
            v = ("vio", "vio2", "vio3", "vio4")[len([1 for d_ in done if VC59[0] <= d_[0] <= VC59[2] and VC59[1] <= d_[1] <= VC59[3]]) % 4]
            look = ("roof_" + v, look[1], "block/crimson_planks=roof_%s_tiles" % v)
        for lk, blocks, swaps in PALLET_KIT:
            if lk == "swap" and blocks.startswith("jungle_stairs") and roof == "jungle_stairs": continue
            add(lk, blocks, b, swaps, yr)
        add(look[0], roof, b, "-", yr)
        # (16:04, 16:15) The shelves under and between the windows a wooden band (their caps on the chimneys stone).
        add("swap", "jungle_shelf,birch_shelf", b, "block/jungle_shelf=band_shelf,block/birch_shelf=band_shelf", ys(y0 - 1, y0 + 5))
        add("swap", look[1], b, look[2], ys(y0 + 3, y1 + 2))
        add("swap", "dark_oak_shelf", b, "block/dark_oak_shelf=panel_planter_dark", ys(y0 - 1, y0 + 1))
        # (Mahogany's and the Lake's planters are John's pale oak shelves; elsewhere a pale oak shelf is a sign.)
        if comp.get("pale_oak_shelf", 0) >= 8:
            add("swap", "pale_oak_shelf", (x0, z0, x1, z1, "a Johto house's planters"), "block/pale_oak_shelf=panel_white", ys(y0 - 1, y0 + 1))
        add("swap", "stripped_dark_oak_wood,stripped_spruce_wood,stripped_spruce_log,stripped_mangrove_wood", b,
            "block/stripped_dark_oak_log=post_bt,block/stripped_dark_oak_log_top=post_bt_top,block/stripped_spruce_log=post_bt,block/stripped_spruce_log_top=post_bt_top,"
            "block/stripped_mangrove_log=mah_post,block/stripped_mangrove_log_top=post_bt_top", yr)
        add("swap", "smooth_stone_slab", b, "block/smooth_stone=house_stone,block/smooth_stone_slab_side=house_stone", ys(y0 - 1, y0 + 1))
        # (0.59, 18:25: "they've all still got the terracotta wall") Their walls white render, as Pallet's; (Earl's
        # Academy: "the trim around the sides for the curb are like a brick") Violet's kerbs at their feet its kerb.
        add("swap", "white_terracotta", b, "block/white_terracotta=jhouse_wall", yr)
        if vio: add("swap", "mud_bricks", b, "block/mud_bricks=violet_kerb", ys(y0 - 2, y0 - 1))
        add("swap", "nether_bricks,nether_brick_stairs,nether_brick_slab", b, "block/nether_bricks=chimney_dark", ys(y0 + 3, y1 + 2))
        add("swap", "gold_block", b, "block/gold_block=gold_trim", ys(y0 + 3, y1 + 2))
        add("swap", "dark_oak_shelf", b, "block/dark_oak_shelf=panel_cap_dark", ys(y0 + 4, y1 + 2))
        # Cianwood's white roofs, the blue and white trim (16:35).
        if roof == "pale_oak_stairs":
            add("swap", "polished_diorite_stairs,polished_diorite_slab", b, "block/polished_diorite=cian_trim", yr)
    # AZALEA TOWN's two houses (16:23: "one with a chimney, one without ... these specific ones will need different
    # colours"): the one by the Center moss green, the one with the chimney (Kurt's) blue; Pallet's kit on both.
    for x0, z0, x1, z1, k in ((-2034, 848, -2020, 860, "roof_azalea1"), (-1998, 869, -1984, 881, "roof_azalea2")):
        b = (x0, z0, x1, z1, "an Azalea house")
        for lk, blocks, swaps in PALLET_KIT:
            if blocks.startswith("jungle_stairs"): continue
            add(lk, blocks, b, swaps, ys(19, 31))
        add(k, "jungle_stairs", b, "-", ys(21, 31))
        add("swap", "jungle_slab,jungle_planks", b, "block/jungle_planks=%s_tiles" % k, ys(21, 31))
        add("swap", "gray_terracotta", b, "block/gray_terracotta=farm_wall", ys(19, 28))
        add("swap", "white_terracotta", b, "block/white_terracotta=jhouse_wall", ys(19, 31))
    # Mr. Berry's house (16:15: "the black roof, darker chimneys and wood and bricks ... lean into that"): its
    # terracotta a warm render, its bricks clean, its dark timbers.
    BR = (-1691, 595, -1677, 608, "Mr. Berry's house")
    add("swap", "terracotta", BR, "block/terracotta=berry_wall", ys(20, 30))
    add("swap", "bricks,brick_stairs,brick_slab", BR, "block/bricks=berry_brick", ys(20, 30))
    add("swap", "stripped_spruce_wood,stripped_spruce_log", BR, "block/stripped_spruce_log=berry_post,block/stripped_spruce_log_top=post_bt_top", ys(20, 30))
    # The Route 28 house by SILVER CAVE (15:57: "the shelving situation around the chimney ... the pillars ... the
    # shelving trim at the base"): Viridian's green houses' kit.
    RH = (-1120, 531, -1101, 543, "the house on Route 28")
    for blocks, swaps in (("mud_bricks,mud_brick_stairs,mud_brick_slab", "block/mud_bricks=chimney_brick"), ("warped_shelf", "block/warped_shelf=green_shelf"),
                          ("stripped_warped_hyphae,stripped_warped_stem", "block/stripped_warped_stem=green_solid,block/stripped_warped_stem_top=green_solid_top"),
                          ("birch_shelf", "block/birch_shelf=wood_shelf"), ("polished_andesite_slab,polished_andesite_stairs,polished_andesite", G["STEPS"]),
                          ("polished_diorite_stairs,polished_diorite_slab", TRIM), ("dark_prismarine,dark_prismarine_slab,dark_prismarine_stairs", "block/dark_prismarine=window_low"),
                          ("stripped_spruce_log,stripped_spruce_wood", "block/stripped_spruce_log=post_bt,block/stripped_spruce_log_top=post_bt_top"),
                          ("pale_oak_fence", "block/pale_oak_planks=white_fence")):
        add("swap", blocks, RH, swaps, ys(26, 40))
    add("roof_green", "warped_stairs", RH, "-", ys(26, 40))

    # ---------- Violet City (16:15) ----------
    VC = (-1912, 500, -1760, 600, "Violet City")
    add("swap", "clay", VC, "block/clay=violet_pave@2", ys(20, 22))
    add("swap", "mud_bricks", VC, "block/mud_bricks=violet_kerb", ys(20, 22))
    add("grass_onto", "clay,mud_bricks", VC, "-", ys(20, 22))
    # The bridge over to the tower: one run of slate boards, its rails and posts dark beams, its bolts; the island's
    # stone flagstones.
    VB = (-1882, 493, -1820, 530, "Violet's bridge")
    add("swap", "crimson_stairs,crimson_planks,crimson_slab", VB, "block/crimson_planks=vio_bridge", ys(21, 23))
    add("swap", "crimson_shelf,stripped_crimson_hyphae,stripped_crimson_stem,polished_blackstone_button", VB,
        "block/crimson_shelf=vio_bridge_beam,block/stripped_crimson_stem=vio_bridge_beam,block/stripped_crimson_stem_top=vio_bridge_beam,block/polished_blackstone=bolt", ys(21, 24))
    add("swap", "purple_terracotta,magenta_terracotta", VB, "block/purple_terracotta=vio_isle@2,block/magenta_terracotta=violet_kerb", ys(15, 23))
    # Its two pairs of stone lanterns by the bridge (HeartGold's).
    for x0, z0, x1, z1 in ((-1878, 494, -1874, 496), (-1864, 494, -1860, 496), (-1852, 530, -1848, 532), (-1845, 530, -1841, 532)):
        b = (x0 - 1, z0 - 1, x1 + 1, z1 + 1, "a stone lantern")
        add("swap", "cauldron,hopper,blackstone_wall,anvil,chipped_anvil,damaged_anvil", b,
            "block/cauldron_side=lantern_stone,block/cauldron_top=lantern_stone_top,block/cauldron_bottom=lantern_stone,block/cauldron_inner=lantern_stone,"
            "block/hopper_outside=lantern_stone,block/hopper_inside=lantern_stone,block/hopper_top=lantern_stone_top,block/blackstone=lantern_stone,"
            "block/blackstone_top=lantern_stone_top,block/anvil=lantern_stone_top,block/anvil_top=lantern_stone_top", ys(21, 26))
        add("swap", "chiseled_nether_bricks", b, "block/chiseled_nether_bricks=lantern_stone_window", ys(21, 26))
    # The SPROUT TOWER and the TIN TOWER and the BURNED TOWER (16:15, 16:23).
    def tower(bx, k, ylo, yhi):
        add("swap", "dark_oak_slab,dark_oak_stairs,dark_oak_planks,dark_oak_trapdoor", bx, "block/dark_oak_planks=%s_timber,block/dark_oak_trapdoor=%s_timber" % (k, k), ys(ylo, yhi))
        add("swap", "spruce_log", bx, "block/spruce_log=%s_pillar,block/spruce_log_top=%s_pillar" % (k, k), ys(ylo, yhi))
        add("swap", "calcite", bx, "block/calcite=%s_plaster" % k, ys(ylo, yhi))
        add("swap", "crimson_planks,crimson_slab,crimson_stairs", bx, "block/crimson_planks=%s_roof" % k, ys(ylo, yhi))
        add("swap", "nether_bricks,nether_brick_slab,nether_brick_stairs", bx, "block/nether_bricks=%s_ridge" % k, ys(ylo, yhi))
        add("swap", "dark_oak_fence,dark_oak_fence_gate", bx, "block/dark_oak_planks=%s_rail" % k, ys(ylo, yhi))
        add("swap", "spruce_planks,spruce_slab", bx, "block/spruce_planks=%s_floor" % k, ys(ylo, yhi))
        add("swap", "smooth_stone,smooth_stone_slab,cobblestone,stone_bricks,stone_brick_stairs,stone_brick_slab,stone_stairs,polished_andesite_stairs,polished_andesite,"
            "polished_andesite_slab", bx,
            "block/smooth_stone=%s_stone,block/smooth_stone_slab_side=%s_stone,block/cobblestone=%s_stone,block/stone_bricks=%s_stone,block/stone=%s_stone,"
            "block/polished_andesite=%s_stone" % ((k,) * 6), ys(ylo - 3, yhi))
        add("swap", "polished_diorite_stairs,polished_diorite", bx, "block/polished_diorite=%s_trim" % k, ys(ylo, yhi))
        add("swap", "brown_terracotta", bx, "block/brown_terracotta=%s_beam" % k, ys(ylo, yhi))
        add("swap", "oak_trapdoor,bamboo_trapdoor", bx, "block/oak_trapdoor=%s_screen,block/bamboo_trapdoor=%s_screen" % (k, k), ys(ylo, yhi))
    tower((-1861, 466, -1833, 494, "the SPROUT TOWER"), "sprout", 22, 76)
    tower((-1992, 337, -1964, 366, "the TIN TOWER"), "tin", 20, 84)
    tower((-2079, 344, -2051, 372, "the BURNED TOWER"), "burned", 20, 50)
    BT = (-2079, 344, -2051, 372, "the BURNED TOWER")
    add("swap", "warped_planks,warped_slab,warped_stairs", BT, "block/warped_planks=burned_teal", ys(28, 50))
    add("swap", "dark_prismarine,dark_prismarine_slab,dark_prismarine_stairs", BT, "block/dark_prismarine=burned_char", ys(28, 50))
    add("swap", "stripped_mangrove_log,stripped_mangrove_wood", BT, "block/stripped_mangrove_log=burned_pillar,block/stripped_mangrove_log_top=burned_pillar", ys(20, 50))
    add("swap", "dripstone_block", BT, "block/dripstone_block=burned_rubble@2", ys(20, 24))

    # ---------- Ecruteak (16:23: "the same as Lavender Town ... leaning more into the sort of like red") ----------
    EC = (-2090, 336, -1966, 432, "Ecruteak City")
    add("swap", "gray_glazed_terracotta", EC, "block/gray_glazed_terracotta=ecru_flag@2", ys(19, 22))
    add("swap", "light_gray_terracotta", EC, "block/light_gray_terracotta=ecru_plot@2", ys(19, 21))
    add("grass_onto", "gray_glazed_terracotta,light_gray_terracotta", EC, "-", ys(19, 22))
    for i, h in enumerate(((-2069, 395, -2056, 405), (-2069, 380, -2056, 390), (-2053, 380, -2040, 390), (-2049, 410, -2036, 420), (-2008, 410, -1995, 420))):
        b = (h[0], h[1], h[2], h[3], "an Ecruteak house")
        # (0.59, 18:40: "variations ... the red in Ecruteak City") one of four reds each.
        k = ("ecru", "ecru2", "ecru3", "ecru4")[i % 4]
        add("roof_" + k, "mangrove_stairs", b, "-", ys(22, 30))
        add("swap", "mangrove_slab,mangrove_planks", b, "block/mangrove_planks=roof_%s_tiles" % k, ys(22, 30))
        add("swap", "white_concrete", b, "block/white_concrete=ecru_plaster", ys(22, 28))
        add("swap", "light_gray_concrete", b, "block/light_gray_concrete=ecru_base", ys(21, 23))
        add("swap", "stripped_dark_oak_wood,stripped_dark_oak_log", b, "block/stripped_dark_oak_log=ecru_timber,block/stripped_dark_oak_log_top=ecru_timber", ys(21, 30))
        add("swap", "dark_oak_shelf", b, "block/dark_oak_shelf=panel_planter_dark", ys(21, 30))
        add("swap", "smooth_stone_slab", b, "block/smooth_stone=house_stone,block/smooth_stone_slab_side=house_stone", ys(21, 23))
        add("swap", "composter", b, "block/composter_side=bin_side,block/composter_top=bin_top,block/composter_bottom=bin_top", ys(21, 24))
    # The DANCE THEATRE: a blue tiled roof, a gold ridge, red lacquered beams, white walls.
    DT = (-2031, 393, -2014, 405, "the DANCE THEATRE")
    add("roof_dance", "warped_stairs", DT, "-", ys(26, 32))
    add("swap", "warped_slab,warped_planks", DT, "block/warped_planks=roof_dance_tiles", ys(26, 32))
    add("swap", "birch_stairs,birch_slab,birch_planks,jungle_planks", DT, "block/birch_planks=dance_ridge,block/jungle_planks=dance_ridge", ys(29, 32))
    add("swap", "oak_planks,oak_stairs,stripped_mangrove_log", DT, "block/oak_planks=lacquer_red,block/stripped_mangrove_log=lacquer_red,block/stripped_mangrove_log_top=lacquer_red",
        ys(21, 29))
    add("swap", "light_gray_concrete", DT, "block/light_gray_concrete=dance_wall", ys(21, 30))
    add("swap", "deepslate_tiles", DT, "block/deepslate_tiles=lacquer_dark", ys(20, 25))
    # The red house by the BURNED TOWER: a deep red roof, lacquered front, dark timber, the white render.
    RB = (-2048, 356, -2022, 374, "the house by the BURNED TOWER")
    add("swap", "nether_brick_slab,nether_brick_stairs,red_nether_brick_slab,red_nether_brick_stairs,red_nether_bricks,red_nether_brick_wall", RB,
        "block/nether_bricks=redb_roof,block/red_nether_bricks=redb_roof", ys(25, 36))
    add("swap", "acacia_planks,acacia_fence,acacia_fence_gate,acacia_trapdoor", RB, "block/acacia_planks=redb_front,block/acacia_trapdoor=redb_front", ys(20, 26))
    add("swap", "stripped_dark_oak_log,stripped_dark_oak_wood,stripped_spruce_log,stripped_oak_log", RB,
        "block/stripped_dark_oak_log=ecru_timber,block/stripped_dark_oak_log_top=ecru_timber,block/stripped_spruce_log=ecru_timber,block/stripped_spruce_log_top=ecru_timber,"
        "block/stripped_oak_log=ecru_timber,block/stripped_oak_log_top=ecru_timber", ys(20, 36))
    add("swap", "white_concrete", RB, "block/white_concrete=ecru_plaster", ys(22, 32))
    add("swap", "end_rod", RB, "block/end_rod=rod_clean", ys(20, 36))
    # The arch into town from Route 37 (16:23: "the pillared and arch approach"): red lacquer, dark posts and band.
    AR = (-2039, 423, -2024, 433, "Ecruteak's arch")
    add("swap", "nether_brick_stairs,red_nether_brick_stairs,red_nether_bricks,red_nether_brick_wall", AR, "block/nether_bricks=lacquer_red,block/red_nether_bricks=lacquer_red",
        ys(28, 34))
    add("swap", "spruce_log,mangrove_fence_gate,dark_oak_slab,dark_oak_stairs,dark_oak_planks,dark_oak_trapdoor", AR,
        "block/spruce_log=lacquer_dark,block/spruce_log_top=lacquer_dark,block/mangrove_planks=lacquer_dark,block/dark_oak_planks=lacquer_dark,block/dark_oak_trapdoor=lacquer_dark",
        ys(21, 36))
    add("swap", "dark_oak_shelf", AR, "block/dark_oak_shelf=panel_planter_dark", ys(21, 32))

    # ---------- the NATIONAL PARK (16:23, 16:35) ----------
    add("swap", "quartz_slab,quartz_block,polished_diorite_slab,polished_diorite_stairs,diorite,diorite_stairs,diorite_slab,smooth_quartz,smooth_quartz_stairs,smooth_quartz_slab,calcite",
        (-2128, 476, -2114, 487, "the NATIONAL PARK's swan"),
        "block/quartz_block_top=swan_marble,block/quartz_block_side=swan_marble,block/quartz_block_bottom=swan_marble,block/polished_diorite=swan_marble,"
        "block/diorite=swan_marble,block/calcite=swan_marble", ys(19, 40))
    add("swap", "stone_bricks,cracked_stone_bricks", NP, "block/stone_bricks=park_stone@2,block/cracked_stone_bricks=park_stone@2", ys(18, 20))
    add("swap", "cobblestone", NP, "block/cobblestone=park_cobble@2", ys(18, 20))
    add("grass_onto", "stone_bricks,cracked_stone_bricks,cobblestone", NP, "-", ys(18, 20))

    # ---------- Mahogany and the Lake of Rage (16:04) ----------
    add("swap", "chiseled_quartz_block,smooth_quartz,white_terracotta", MAH, "block/chiseled_quartz_block_top=mah_pave@2,block/chiseled_quartz_block=mah_pave@2,block/quartz_block_bottom=mah_pave@2,block/white_terracotta=mah_pave@2",
        ys(21, 21))
    add("grass_onto", "chiseled_quartz_block,smooth_quartz,white_terracotta", MAH, "-", ys(21, 21))
    GY = (-1800, 195, -1750, 232, "the Lake of Rage's GYARADOS")
    for blocks, tex in (("redstone_block", "gy_red@4"), ("nether_wart_block,red_concrete,red_terracotta,netherrack", "gy_red_dark@4"),
                        ("end_stone,birch_planks,oak_planks,acacia_planks,white_terracotta,birch_slab,oak_slab,birch_stairs,oak_stairs", "gy_cream@4"),
                        ("calcite", "gy_teeth"), ("black_terracotta", "gy_eye"), ("pink_terracotta,magenta_terracotta,purple_terracotta", "gy_mouth@4"),
                        ("orange_terracotta,orange_concrete,brown_terracotta,dark_oak_planks,cut_copper,copper_block,waxed_copper_block,waxed_cut_copper", "gy_fin@4")):
        sw = ",".join("block/%s=%s" % ({"nether_wart_block": "nether_wart_block", "birch_slab": "birch_planks", "oak_slab": "oak_planks", "birch_stairs": "birch_planks",
                                       "oak_stairs": "oak_planks", "waxed_copper_block": "copper_block", "waxed_cut_copper": "cut_copper"}.get(b, b), tex) for b in blocks.split(","))
        add("swap", blocks, GY, sw, ys(16, 50))
    add("swap", "sea_lantern", LAKE, "block/sea_lantern=sea_glow!", ys(5, 20))
    add("swap", "brain_coral_block,bubble_coral_block", LAKE, "block/brain_coral_block=coral_clean,block/bubble_coral_block=coral_clean_b", ys(5, 20))
    add("sign", "pale_oak_shelf", LAKE, "-", ys(19, 30))

    # ---------- Olivine, its lighthouse, Cianwood, the Day Care, the farm, Azalea (16:23, 16:35) ----------
    OL = (-2335, 495, -2170, 625, "Olivine City")
    # ((0.59.3, 20:35: "you've replaced terracotta with like a brick ... the floor isn't even") Its paving square-cut
    # flagstones that tile across every block; its boardwalk long boards.)
    add("swap", "light_gray_glazed_terracotta", OL, "block/light_gray_glazed_terracotta=ol_pave@2", ys(17, 19))
    add("swap", "pale_oak_planks", OL, "block/pale_oak_planks=ol_boardwalk@4", ys(17, 19))
    add("grass_onto", "light_gray_glazed_terracotta,pale_oak_planks", OL, "-", ys(17, 19))
    LH = (-2233, 570, -2205, 590, "the lighthouse")
    add("swap", "white_concrete", LH, "block/white_concrete=lh_white", ys(20, 62))
    add("swap", "cyan_concrete,cyan_concrete_powder", LH, "block/cyan_concrete=lh_teal,block/cyan_concrete_powder=lh_teal", ys(20, 62))
    add("swap", "light_blue_concrete", LH, "block/light_blue_concrete=lh_teal_light", ys(20, 62))
    add("swap", "smooth_stone,smooth_stone_slab,stone", LH, "block/smooth_stone=lh_stone,block/smooth_stone_slab_side=lh_stone,block/stone=lh_stone", ys(20, 62))
    add("swap", "waxed_oxidized_copper_trapdoor", LH, "block/oxidized_copper_trapdoor=lh_rail", ys(20, 62))
    add("swap", "warped_shelf,dark_oak_shelf", LH, "block/warped_shelf=panel_teal,block/dark_oak_shelf=panel_cap_dark", ys(20, 62))
    add("swap", "end_rod", LH, "block/end_rod=rod_clean", ys(20, 62))
    add("swap", "smooth_quartz,smooth_quartz_stairs", LH, "block/quartz_block_bottom=quartz_clean", ys(20, 62))
    # The benches along the beach and the harbour (16:35): wooden slats on dark ends.
    for b in (OL, (-2600, 690, -2480, 820, "Cianwood")):
        add("swap", "spruce_slab,spruce_stairs", b, "block/spruce_planks=bench_slats", ys(17, 21))
    # Cianwood's beach edge and paths.
    CW = (-2600, 690, -2480, 820, "Cianwood City")
    add("swap", "smooth_sandstone,sandstone_slab", CW, "block/sandstone_top=beach_sand@4,block/sandstone=beach_sand@4", ys(16, 18))
    # The Day Care (Route 34): pale render, a green foot, its red roof, white boards.
    DC = (-2093, 775, -2070, 793, "the Day Care")
    add("roof_daycare", "red_nether_brick_stairs", DC, "-", ys(22, 29))
    add("swap", "white_concrete,light_gray_concrete", DC, "block/white_concrete=daycare_wall,block/light_gray_concrete=daycare_wall", ys(20, 27))
    add("swap", "green_terracotta", DC, "block/green_terracotta=daycare_foot", ys(19, 21))
    add("swap", "pale_oak_planks", DC, "block/pale_oak_planks=barn_trim", ys(20, 24))
    add("swap", "pale_oak_shelf", DC, "block/pale_oak_shelf=panel_white", ys(19, 23))
    add("swap", "black_concrete", DC, "block/black_concrete=mansion_dark", ys(19, 24))
    add("swap", "spruce_log,spruce_fence", DC, "block/spruce_log=fence_post,block/spruce_log_top=fence_post_top,block/spruce_planks=fence_rail", ys(19, 23))
    # The MOOMOO FARM (16:23: "you went with like the dripstone top on that ... just retexturize the entire thing").
    FM = (-2336, 371, -2299, 396, "the MOOMOO FARM")
    add("roof_farm", "mangrove_stairs", FM, "-", ys(26, 33))
    add("swap", "mangrove_planks,mangrove_slab", FM, "block/mangrove_planks=roof_farm_tiles", ys(26, 33))
    add("swap", "oak_planks", FM, "block/oak_planks=barn_red", ys(22, 31))
    add("swap", "dark_oak_wood,dark_oak_log,dark_oak_stairs", FM, "block/dark_oak_log=barn_timber,block/dark_oak_log_top=barn_timber,block/dark_oak_planks=barn_timber", ys(22, 31))
    add("swap", "jungle_slab,jungle_stairs,jungle_planks", FM, "block/jungle_planks=barn_slats", ys(24, 28))
    add("swap", "white_concrete", FM, "block/white_concrete=barn_trim", ys(21, 23))
    add("swap", "dark_oak_shelf", FM, "block/dark_oak_shelf=panel_planter_dark", ys(21, 31))
    add("swap", "black_concrete", FM, "block/black_concrete=mansion_dark", ys(21, 27))
    add("swap", "polished_andesite,polished_andesite_slab", FM, "block/polished_andesite=house_stone", ys(21, 23))
