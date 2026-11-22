"""
Writes the games' data for the looks only some places have (src/main/resources/retromon/data/area_looks.tsv, read by
client/AreaLooks): which look, on which blocks, over which part of the map, with which textures swapped (made by
tools/lookarea.py). The Pokemon Centers' reds and the Marts' blues are put on every Center and Mart on the map (from
tools/interior/export/shops.tsv: the button over each one's door, the buildings all John's one design).
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data", "area_looks.tsv")

PALLET_R1 = (-845, 590, -745, 735, "Pallet Town and Route 1")
PALLET = (-845, 655, -745, 735, "Pallet Town")
VIRIDIAN = (-830, 495, -745, 589, "Viridian City")
ROUTE2 = (-830, 340, -745, 494, "Route 2")
ROUTE22 = (-1000, 495, -831, 600, "Route 22")
ROUTE3 = (-740, 285, -580, 332, "Route 3")
ROUTE4 = (-628, 238, -410, 284, "Route 4")
PEWTER = (-850, 255, -741, 339, "Pewter City")
# Pewter's two houses and Route 2's trade house (13:18): John's one design.
PEWTER_HOUSES = [(-778, 278, -763, 291, "Pewter's house by the Museum"), (-827, 316, -812, 329, "Pewter's house by the Center"),
                 (-791, 379, -777, 392, "Route 2's trade house")]
HOUSE_P = ("block/deepslate_tiles=pewter_roof,block/pale_oak_planks=pewter_wall")
MUSEUM = (-820, 265, -777, 282, "Pewter Museum of Science")
# (14:15) All of Kanto (John's map east of the Route 28 Gate): the fences' log posts, the lanterns, the hidden glowstone,
# the lamps' sun catchers.
KANTO = (-1010, 100, 60, 1060, "Kanto")
VIRIDIAN_HOUSES = (-792, 521, -779, 546, "Viridian's green houses")
FENCE_WOOD = "block/spruce_log=fence_post,block/spruce_log_top=fence_post_top,block/spruce_planks=fence_rail"
PLAMP = ("block/tuff=plamp_post,block/polished_tuff=plamp_post,block/chiseled_tuff_bricks=plamp_chiseled_tuff_bricks,"
         "block/chiseled_tuff_bricks_top=plamp_chiseled_tuff_bricks_top,block/gray_wool=plamp_cap,block/redstone_lamp=plamp_globe,"
         "block/redstone_lamp_on=plamp_globe")
TRIM = "block/polished_diorite=house_trim"
STEPS = "block/polished_andesite=house_stone,block/cobblestone=house_stone,block/andesite=house_stone"
PAVE = ("block/white_concrete=pewter_pave_white,block/quartz_block_bottom=pave_quartz,block/white_terracotta=pave_terracotta,"
        "block/chiseled_quartz_block_top=pave_chiseled,block/chiseled_quartz_block=pave_chiseled_side,block/light_gray_concrete=pave_gray").replace("pewter_pave_white", "pave_white")

FENCE = "block/pale_oak_planks=white_fence"
PATH = "block/calcite=path_calcite,block/diorite=path_diorite,block/polished_diorite=path_polished_diorite"
LAMP = ("block/tuff=lamp_post,block/polished_tuff=lamp_post,block/chiseled_tuff_bricks=lamp_chiseled_tuff_bricks,"
        "block/chiseled_tuff_bricks_top=lamp_chiseled_tuff_bricks_top,block/gray_wool=lamp_cap")
HOUSES = [(-807, 671, -794, 684, "your house"), (-789, 671, -776, 684, "Blue's house")]
LAB = (-791, 687, -774, 698, "Prof. Oak's lab")
GATE_BLOCKS = ("polished_deepslate,polished_deepslate_slab,polished_deepslate_stairs,deepslate_tile_wall,deepslate_tiles,deepslate_tile_slab,"
               "deepslate_tile_stairs,andesite,polished_andesite,polished_andesite_stairs,polished_andesite_slab,birch_stairs,birch_planks,birch_slab,"
               "stripped_oak_log,stripped_oak_wood,stripped_jungle_log,stripped_jungle_wood")
GATE = ("block/polished_deepslate=gate_roof,block/deepslate_tiles=gate_roof_rim,block/andesite=gate_pillar,block/polished_andesite=gate_stone,"
        "block/birch_planks=gate_wall_birch,block/stripped_oak_log=gate_wall_oak,block/stripped_oak_log_top=gate_wall_oak_top,"
        "block/stripped_jungle_log=gate_beam,block/stripped_jungle_log_top=gate_beam_top")
CENTER = ("block/stripped_mangrove_log=center_stripped_mangrove_log,block/stripped_mangrove_log_top=center_stripped_mangrove_log_top,"
          "block/mangrove_planks=center_mangrove_planks")
MART = "block/blue_concrete=mart_blue_concrete,block/blue_wool=mart_blue_wool"
# The POKéMON LEAGUE gate (Route 22's west end): the gates' looks, but its roof lime green metal, its deepslate brick
# pillars metal, the oak over its doors lime green, its resin brick walls Let's Go's mixed brick.
LEAGUE_BLOCKS = GATE_BLOCKS + (",deepslate_brick_wall,deepslate_bricks,deepslate_brick_stairs,deepslate_brick_slab,oak_planks,oak_stairs,oak_slab,"
                               "resin_bricks,resin_brick_stairs,resin_brick_slab,resin_brick_wall")
LEAGUE = (GATE.replace("block/polished_deepslate=gate_roof", "block/polished_deepslate=league_roof")
          .replace("block/deepslate_tiles=gate_roof_rim", "block/deepslate_tiles=league_roof_rim")
          + ",block/deepslate_bricks=league_pillar,block/oak_planks=league_arch,block/resin_bricks=league_brick")
# Its four little trees out front: spruce fence trunks as wood, acacia leaves as a round bush (! : without the
# biome's tint, the bush's own green).
LEAGUE_TREES = "block/spruce_planks=league_trunk,block/acacia_leaves=league_bush!"
GYM = ("block/jungle_planks=gym_flat,block/oak_planks=gym_flat_light,block/stripped_jungle_log=gym_beam,"
       "block/stripped_jungle_log_top=gym_beam_top")


SHOPS, GYMS = [], []


def main():
    rows = []
    add = lambda look, blocks, area, swaps="-", ys=("-", "-"): rows.append((look, blocks, area[0], area[1], area[2], area[3], ys[0], ys[1], swaps, area[4]))
    for a in (PALLET_R1, VIRIDIAN, ROUTE22, PEWTER):
        add("swap", "pale_oak_fence", a, FENCE)
    add("roof", "acacia_stairs", (-806, 673, -795, 682, "your house"))
    add("roof", "acacia_stairs", (-788, 673, -777, 682, "Blue's house"))
    for a in (PALLET_R1, VIRIDIAN, ROUTE2, ROUTE22, ROUTE3, ROUTE4):
        add("sign", "pale_oak_shelf,pale_oak_sign", a)
    for a in (PALLET_R1, VIRIDIAN, ROUTE2, ROUTE22, PEWTER):
        add("wood_sign", "spruce_sign", a)
    add("grass_edge", "dripstone_block", PALLET)
    # Viridian City's two green houses: their roofs (warped stairs) in green tiles, their windows' frames (pale oak
    # stairs and slabs) solid white.
    add("roof_green", "warped_stairs", VIRIDIAN)
    add("swap", "pale_oak_stairs,pale_oak_slab", VIRIDIAN, "block/pale_oak_planks=window_frame")
    add("swap", "calcite,diorite,polished_diorite", VIRIDIAN, PATH, ("24", "25"))
    add("kerb", "calcite,diorite,polished_diorite", VIRIDIAN, "-", ("24", "25"))
    for a in (VIRIDIAN, ROUTE22, ROUTE2):
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet", a, LAMP)
    # Pewter's own lamps (14:15): grey metal, a frosted globe.
    add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet,redstone_lamp", PEWTER, PLAMP)
    # All Kanto (14:15, 14:18): the lamps' sun catchers solar panels; the fences' log posts and rails solid wood (a fence,
    # not trees); John's lanterns on the fences and over the paths garden lights; the glowstone he hid under carpets as
    # the carpet, so it doesn't show through the windows.
    add("swap", "daylight_detector", KANTO, "block/daylight_detector_top=lamp_solar,block/daylight_detector_side=lamp_solar_side")
    add("swap", "spruce_log,spruce_fence", KANTO, FENCE_WOOD)
    add("swap", "lantern", KANTO, "block/lantern=garden_light")
    add("swap", "glowstone", KANTO, "block/glowstone=glow_hidden")
    # Pallet's little wooden sign by the flower garden (an oak sign) as the wooden sign (14:18).
    add("wood_sign", "oak_sign", PALLET)
    # Viridian's green houses (14:15): the chimney painted brick, the warped shelf on the ridge and the chimney's top solid
    # green, the birch shelves round the foot one solid wood (their shelf kept), the steps in as Pallet's, the trim too.
    add("swap", "mud_bricks,mud_brick_stairs,mud_brick_slab", VIRIDIAN_HOUSES, "block/mud_bricks=chimney_brick")
    add("swap", "warped_shelf", VIRIDIAN_HOUSES, "block/warped_shelf=green_shelf")
    add("swap", "stripped_warped_hyphae,stripped_warped_stem", VIRIDIAN_HOUSES, "block/stripped_warped_stem=green_solid,block/stripped_warped_stem_top=green_solid_top")
    add("swap", "birch_shelf", VIRIDIAN_HOUSES, "block/birch_shelf=wood_shelf")
    add("swap", "polished_andesite_slab,polished_andesite_stairs,polished_andesite", VIRIDIAN_HOUSES, STEPS)
    add("swap", "polished_diorite_stairs,polished_diorite_slab", VIRIDIAN_HOUSES, TRIM)
    add("swap", "polished_deepslate_wall", VIRIDIAN_HOUSES, "block/polished_deepslate=drainpipe")
    # Pewter City (13:18). Its town sign (the standing pale oak shelves: not the Museum's, higher up) as HeartGold's
    # sign; its oak shelf sign (TRAINER TIPS) as our wooden sign; the flower beds' sides (pale oak trapdoors, y 31) a
    # white planter; the paving (y 29) each block a paving stone of its own colour, as laid.
    add("sign", "pale_oak_shelf,pale_oak_sign", PEWTER, "-", ("29", "32"))
    add("wood_board", "oak_shelf", PEWTER, "-", ("29", "32"))
    add("swap", "pale_oak_trapdoor", PEWTER, "block/pale_oak_trapdoor=planter_white", ("31", "31"))
    add("swap", "white_concrete,smooth_quartz,white_terracotta,chiseled_quartz_block,light_gray_concrete", PEWTER, PAVE, ("29", "29"))
    # The houses: the roof metal, the walls solid, the stairs at the windows white frames, the trapdoors (y 33)
    # painted panels, the chimney solid.
    for h in PEWTER_HOUSES:
        # (14:15) The steps in and the stone round the foot as Viridian's (Pallet's), the trim, the drainpipe.
        add("swap", "cobblestone,polished_andesite_slab", h, STEPS, ("29", "31"))
        add("swap", "polished_diorite_slab,polished_diorite_stairs", h, TRIM, ("30", "38"))
        add("swap", "polished_deepslate_wall", h, "block/polished_deepslate=drainpipe", ("29", "38"))
        add("swap", "deepslate_tile_slab,pale_oak_planks", h, HOUSE_P, ("30", "38"))
        add("swap", "pale_oak_stairs,pale_oak_slab", h, "block/pale_oak_planks=window_frame", ("30", "38"))
        add("swap", "pale_oak_trapdoor", h, "block/pale_oak_trapdoor=pewter_shutter", ("33", "38"))
        add("swap", "mud_bricks,mud_brick_stairs,mud_brick_slab", h, "block/mud_bricks=pewter_chimney", ("33", "38"))
    # The Museum, as FireRed's: the purpur stairs its purple tiled roof, the purpur slabs purple, the crimson slabs the
    # darker bands, the white terracotta walls cream.
    add("roof_museum", "purpur_stairs", MUSEUM, "-", ("31", "47"))
    add("swap", "purpur_slab", MUSEUM, "block/purpur_block=museum_roof_flat", ("31", "47"))
    add("swap", "crimson_slab", MUSEUM, "block/crimson_planks=museum_roof_dark", ("31", "47"))
    add("swap", "white_terracotta", MUSEUM, "block/white_terracotta=museum_wall", ("31", "47"))
    # (14:15) Its stone and diorite slabs and its trim a solid light grey; the six little flowering trees out front kept
    # flowery, as the mod's own.
    add("swap", "polished_andesite_slab,smooth_stone,polished_diorite_slab,polished_diorite_stairs", MUSEUM,
        "block/polished_andesite=museum_trim,block/smooth_stone=museum_trim,block/polished_diorite=museum_trim", ("31", "47"))
    add("swap", "flowering_azalea_leaves", (-818, 277, -787, 281, "the Museum's six little trees"), "block/flowering_azalea_leaves=flower_bush!", ("31", "34"))
    # Pallet Town's houses: pillars and steps, the dark parts round the underside, the trim across the front, the
    # drainpipe.
    for h in HOUSES:
        add("swap", "polished_andesite,polished_andesite_slab", h, "block/polished_andesite=house_stone", ("17", "26"))
        add("swap", "polished_deepslate", h, "block/polished_deepslate=house_dark", ("17", "26"))
        # (15:21) The dark slabs under the windows: dark glass panels in a slim frame.
        add("swap", "polished_deepslate_slab", h, "block/polished_deepslate=window_low", ("17", "26"))
        add("swap", "polished_deepslate_wall", h, "block/polished_deepslate=drainpipe", ("17", "26"))
        add("swap", "polished_diorite_stairs", h, "block/polished_diorite=house_trim", ("17", "26"))
    # Prof. Oak's lab: the base and the steps in (y 18), the band under the roof (y 24), the walls, the pillars, the
    # awnings, the chimney. (The roof stays as it is.)
    add("swap", "polished_andesite,polished_andesite_slab", LAB, "block/polished_andesite=lab_stone", ("17", "18"))
    add("swap", "polished_andesite_slab", LAB, "block/polished_andesite=lab_trim", ("24", "24"))
    add("swap", "end_stone_bricks,end_stone_brick_stairs", LAB, "block/end_stone_bricks=lab_brick")
    add("swap", "brown_mushroom_block,mud_brick_stairs", LAB, "block/brown_mushroom_block=lab_pillar,block/mud_bricks=lab_pillar")
    add("swap", "waxed_cut_copper_slab,cut_copper_slab", LAB, "block/cut_copper=lab_awning_red")
    add("swap", "oxidized_cut_copper_slab,waxed_oxidized_cut_copper_slab", LAB, "block/oxidized_cut_copper=lab_awning_door")
    add("swap", "bricks,brick_wall,brick_stairs", LAB, "block/bricks=lab_chimney")
    # Its roof (14:15): light grey metal panels, the rim along its edge darker.
    add("swap", "polished_andesite_slab,polished_andesite_stairs", LAB, "block/polished_andesite=lab_roof", ("25", "26"))
    add("swap", "polished_blackstone_wall", LAB, "block/polished_blackstone=lab_roof_rim", ("25", "26"))
    # Every route gate on the map (the buildings with polished deepslate roofs, tools/area_gates.tsv).
    for line in open(os.path.join(HERE, "area_gates.tsv")):
        if not line.strip() or line.startswith("#"):
            continue
        x0, x1, z0, z1, ry, where = line.rstrip("\n").split("\t")
        ry = int(ry)
        league = where == "League gate"
        add("swap", LEAGUE_BLOCKS if league else GATE_BLOCKS, (int(x0) - 2, int(z0) - 4, int(x1) + 2, int(z1) + 4, where), LEAGUE if league else GATE,
            (str(ry - 18), str(ry + 2)))
        if league:
            add("swap", "spruce_fence,acacia_leaves", (int(x0) - 4, int(z0) - 6, int(x1) + 4, int(z1) + 6, "the League gate's trees"), LEAGUE_TREES,
                (str(ry - 12), str(ry - 1)))
    add("swap", "mud_bricks,mud_brick_stairs,mud_brick_slab", ROUTE22, "block/mud_bricks=route_path@4")
    add("swap", "brown_concrete_powder", ROUTE22, "block/brown_concrete_powder=route_path_edge@4")
    for line in open(os.path.join(HERE, "interior", "export", "shops.tsv")):
        if not line.strip() or line.startswith("#"):
            continue
        kind, place, x, y, z = line.rstrip("\n").split("\t")[:5]
        x, y, z = int(x), int(y), int(z)
        if kind == "center":
            add("swap", "stripped_mangrove_wood,stripped_mangrove_log,mangrove_stairs,mangrove_planks,mangrove_slab",
                (x - 6, z - 10, x + 6, z, place + " Pokemon Center"), CENTER, (str(y - 1), str(y + 2)))
        else:
            add("swap", "blue_concrete,blue_wool,blue_carpet", (x - 6, z - 10, x + 6, z - 2, place + " Mart"), MART, (str(y - 5), str(y + 2)))
        # (14:15) Its trim (diorite stairs and slabs) as Pallet's houses', its foot (polished diorite) solid, its own shade.
        box = (x - 7, z - 11, x + 7, z + 1, place + (" Pokemon Center" if kind == "center" else " Mart"))
        add("swap", "polished_diorite_stairs,polished_diorite_slab", box, TRIM, (str(y - 5), str(y + 3)))
        add("swap", "polished_diorite", box, "block/polished_diorite=" + ("center_base" if kind == "center" else "mart_base"), (str(y - 5), str(y + 3)))
        SHOPS.append((kind, place, x, y, z))
    # Every Gym's roof: its stairs in tan tiles, its planks and beams solid tan shades.
    for line in open(os.path.join(HERE, "area_gyms.tsv")):
        if not line.strip() or line.startswith("#"):
            continue
        place, x0, x1, y0, y1, z0, z1 = line.rstrip("\n").split("\t")
        box = (int(x0) - 1, int(z0) - 1, int(x1) + 1, int(z1) + 1, place + " Gym")
        add("roof_gym", "jungle_stairs,oak_stairs", box, "-", (y0, str(int(y1) + 1)))
        add("swap", "jungle_planks,oak_planks,stripped_jungle_log,jungle_slab,oak_slab", box, GYM, (y0, str(int(y1) + 1)))
        # (14:15) Its diorite trim as the houses'.
        add("swap", "polished_diorite_stairs,polished_diorite_slab,polished_diorite", (box[0] - 1, box[1] - 1, box[2] + 1, box[3] + 1, box[4]), TRIM)
        GYMS.append((box[0] - 1, box[1] - 1, box[2] + 1, box[3] + 1, box[4]))
    # Diglett's Cave's sign by its mouth on Route 11 (east of Vermilion City), as HeartGold's (Route 2's is in Route 2's).
    add("sign", "pale_oak_shelf,pale_oak_sign", (-300, 578, -293, 584, "Diglett's Cave's sign on Route 11"))
    # Viridian Forest (John's, built in the interiors at x 4096, the floor at y 64: tools/forest.py): its shelf signs
    # as HeartGold's, and its two gatehouses (Route 2's gate, outside) in the gates' colours.
    fx, fy = 4096, 64
    forest = lambda x0, z0, x1, z1, note: (fx + x0, z0, fx + x1, z1, note)
    room = lambda look, blocks, area, swaps="-", ys=("-", "-"): rows.append((look, blocks, area[0], area[1], area[2], area[3], ys[0], ys[1], swaps, area[4], "interiors"))
    room("sign", "pale_oak_shelf,pale_oak_sign", forest(0, 0, 119, 171, "Viridian Forest"))
    # Diglett's Cave (John's three rooms, built in the interiors: tools/caves.py), as FireRed's (13:07): its rock (the
    # dripstone, and the brown mushroom blocks over it) olive scales on top and darker olive brown faces; its floor
    # (coarse dirt, packed mud for the paler patches, dirt) FireRed's brown, patchy, laid over four blocks so it doesn't
    # repeat; the entrances' sand (and the sandstone at their doorways) a pale sandy floor; the rocks (tuff,
    # cobblestone, their slabs, andesite slabs) round boulders.
    CAVE = ("block/dripstone_block=cave_rock_top|cave_rock@4,block/brown_mushroom_block=cave_rock_top|cave_rock@4,"
            "block/mushroom_block_inside=cave_rock_top|cave_rock@4")
    CAVE_FLOOR = ("block/coarse_dirt=cave_floor@4,block/packed_mud=cave_floor@4,block/dirt=cave_floor@4,block/sand=cave_sand@4,"
                  "block/smooth_sandstone=cave_mat@2,block/smooth_sandstone_top=cave_mat@2,block/sandstone_top=cave_mat@2,block/mud=cave_floor@4")
    UNDER = "coarse_dirt=cave_floor@4,packed_mud=cave_floor@4,dirt=cave_floor@4,mud=cave_floor@4,sand=cave_sand@4,smooth_sandstone=cave_sand@4"
    for ox, w, d, note in ((4352, 20, 16, "Diglett's Cave north entrance"), (4608, 170, 160, "Diglett's Cave B1F"), (4864, 20, 16, "Diglett's Cave south entrance")):
        box = (ox - 1, -1, ox + w, d, note)
        room("swap", "dripstone_block,brown_mushroom_block", box, CAVE)
        room("swap", "coarse_dirt,packed_mud,dirt,sand,smooth_sandstone,mud", box, CAVE_FLOOR)
        room("sand_edge", "coarse_dirt,packed_mud,dirt,mud", box)
        room("swap", "white_concrete", box, "block/white_concrete=cave_daylight!")
        # (15:21) The rocks are copper grates in the room now (tools/caves.py), hiding nothing round them: no floor drawn under.
        room("boulder", "waxed_copper_grate,waxed_exposed_copper_grate", box, "prefix:boulder")
    # Mt. Moon (13:56, tools/mtmoon.py), as FireRed's: grey brown rock (the dripstone, its tops the white terracotta John
    # capped it with), a pale grey floor (andesite; tuff and gravel its darker patches), the sand soft at its edges, its
    # rocks (granite) grey boulders, a mat at the way out with daylight beyond, the sign as HeartGold's.
    MOON = "block/dripstone_block=moon_rock_top|moon_rock@4,block/white_terracotta=moon_rock_top|moon_rock@4"
    # (15:21) Its darker patches (tuff, gravel) the same floor, so no square shows where they are.
    MOON_FLOOR = ("block/andesite=moon_floor@4,block/tuff=moon_floor@4,block/gravel=moon_floor@4,block/sand=cave_sand@4,"
                  "block/smooth_sandstone=cave_mat@2,block/smooth_sandstone_top=cave_mat@2,block/sandstone_top=cave_mat@2")
    for ox, w, d, note in ((5120, 104, 90, "Mt. Moon 1F"), (5376, 106, 86, "Mt. Moon B1F"), (5632, 106, 90, "Mt. Moon B2F")):
        box = (ox - 1, -1, ox + w, d, note)
        room("swap", "dripstone_block,white_terracotta", box, MOON)
        room("swap", "andesite,tuff,gravel,sand,smooth_sandstone", box, MOON_FLOOR)
        room("sand_edge", "andesite,tuff,gravel", box)
        room("swap", "white_concrete", box, "block/white_concrete=cave_daylight!")
        room("boulder", "waxed_copper_grate,waxed_exposed_copper_grate", box, "prefix:mboulder")
        room("sign", "pale_oak_shelf", box)
    for x0, z0, x1, z1, note in ((22, 23, 41, 42, "Viridian Forest's north gatehouse"), (54, 138, 73, 157, "Viridian Forest's south gatehouse")):
        room("swap", GATE_BLOCKS, forest(x0, z0, x1, z1, note), GATE, (str(fy - 2), str(fy + 16)))
    round46(add, room)
    with open(OUT, "w") as f:
        f.write("# Looks the mod gives some places only (client/AreaLooks), written by tools/arealooks.py: which look, on which blocks,\n"
                "# over which part of the map (x0 z0 to x1 z1; y0 y1 the heights, - for every height), the textures swapped.\n"
                "# swap: textures for others (old=new, the new ones the mod's own, in textures/block/area; @4: laid from where the face\n"
                "# is, a picture four blocks across). roof, roof_green, roof_gym: rows of tiles along the eaves (HeartGold's red, the\n# green houses' green, the Gyms' tan). sign: shelves and standing pale oak\n"
                "# signs as HeartGold's signs. wood_sign: standing spruce signs as its wooden sign, taller. wood_board: shelves as\n# the sign in wood (our wooden sign). roof_museum: the Museum's purple tiles. grass_edge: the grass's tufts\n"
                "# onto the dripstone round the water. kerb: a grey kerb with the grass rounded onto it, on a path's blocks.\n"
                "# rooms: interiors for a look in the interiors (the forest), not John's world.\n"
                "#look\tblocks\tx0\tz0\tx1\tz1\ty0\ty1\tswaps\twhere\trooms\n")
        for r in rows:
            f.write("\t".join(str(v) for v in r) + "\n")
    print(len(rows), "rows")



FUCHSIA = (-560, 733, -441, 860, "Fuchsia City")
FU_HOUSES = [(-522, 803, -513, 812, "Fuchsia's house (BILL's grandpa)", 1), (-511, 803, -502, 812, "Fuchsia's house (the MOVE DELETER)", 2),
             (-481, 803, -472, 812, "Fuchsia's house (the SAFARI ZONE WARDEN)", 3), (-471, 803, -462, 812, "Fuchsia's house (the FISHING GURU's brother)", 4)]
SAFARI_HOUSES = [(-494, 741, -477, 759, "the SAFARI ZONE's gate"), (-472, 750, -454, 767, "the SAFARI ZONE's office")]
# Stone heaped as rocks (15:21, 15:29): Pewter's by the Museum's west side and by the way east; Fuchsia's.
BOULDERS = [(-829, 267, -816, 282, "Pewter's rocks by the Museum"), (-769, 297, -763, 303, "Pewter's rocks by the way east"), FUCHSIA]
BOULDER_BLOCKS = ("cobblestone,cobblestone_stairs,cobblestone_slab,cobblestone_wall,andesite,andesite_stairs,andesite_slab,andesite_wall,diorite,"
                  "diorite_slab,diorite_stairs,diorite_wall,tuff,tuff_slab,tuff_stairs,tuff_wall,stone,mossy_cobblestone")
BOULDER = ",".join("block/%s=boulder_stone_top|boulder_stone@2" % t for t in ("cobblestone", "andesite", "diorite", "tuff", "stone", "mossy_cobblestone"))
TUNNEL = ("block/stone_bricks=tunnel_stone,block/smooth_stone_slab_side=tunnel_cap_side,block/smooth_stone=tunnel_cap,"
          "block/black_concrete=tunnel_dark")


def round46(add, room):
    """0.46.0's looks (15:21, 15:29): see tools/looks46.py for the pictures."""
    import looks46
    K = KANTO
    # All of Kanto (the most particular area's swap wins where two cover a block): the doors, the doorbells, the glass
    # (one pane), the ladders (metal), the lamps (their tops, ends, glass), the white fences, the cliffs and ledges
    # (dripstone, and the jungle stairs John makes them with) as rock, the steps (mud brick stairs) as boards, the mud
    # brick slabs round the ponds as rock in their own colour, the low mud brick walls dressed stone, the chains hung
    # from the posts, the sand not repeating, the grass soft onto the sand and the moss.
    spec, blocks = looks46.door_swaps()
    add("swap", blocks, K, spec)
    add("swap", "stone_button", K, "block/stone=doorbell")
    add("glass", looks46.GLASS_BLOCKS, K, looks46.glass_spec())
    add("swap", "ladder", K, "block/ladder=metal_ladder")
    add("swap", "daylight_detector", K, "block/daylight_detector_top=lamp_solar,block/daylight_detector_inverted_top=lamp_solar,"
                                        "block/daylight_detector_side=lamp_solar_side")
    add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet", K, LAMP46)
    add("swap", "pale_oak_fence", K, FENCE)
    add("swap", "dripstone_block", K, "block/dripstone_block=drip_rock_top|drip_rock@4")
    add("swap", "jungle_stairs,jungle_slab", K, "block/jungle_planks=drip_rock_top|drip_rock@4")
    add("swap", "mud_brick_stairs", K, "block/mud_bricks=step_boards")
    add("swap", "mud_brick_slab", K, "block/mud_bricks=mud_rock@4")
    add("swap", "mud_brick_wall", K, "block/mud_bricks=wall_tan")
    add("chain", "iron_chain", K)
    add("swap", "sand", K, "block/sand=beach_sand@4")
    add("grass_onto", "sand", K)
    add("grass_onto", "moss_block", K, "grass")
    # The old per-place lamp rows keep their own (Pewter's grey); Viridian's etc. get the new chiselled ends.
    for a in (VIRIDIAN, ROUTE22, ROUTE2):
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet", a, LAMP46)
    # The dark window panels (Pallet's lab, Viridian's green houses and the rest of the towns' dark prismarine).
    add("swap", "polished_deepslate_slab", LAB, "block/polished_deepslate=window_low", ("17", "26"))
    for a in (PALLET_R1, VIRIDIAN, PEWTER, FUCHSIA):
        add("swap", "dark_prismarine,dark_prismarine_slab,dark_prismarine_stairs", a, "block/dark_prismarine=window_low")
    # Every cave and tunnel mouth (tools/area_mouths.tsv): Let's Go's grey portal, its slab cap, the dark inside.
    for line in open(os.path.join(HERE, "area_mouths.tsv")):
        if not line.strip() or line.startswith("#"):
            continue
        x0, z0, x1, z1, y0, y1 = map(int, line.split("\t")[:6])
        add("swap", "stone_bricks,stone_brick_slab,stone_brick_stairs,stone_brick_wall,smooth_stone_slab,smooth_stone,black_concrete",
            (x0, z0, x1, z1, "a cave's mouth"), TUNNEL, (str(y0 - 1), str(y1 + 1)))
    # The Centers' and Marts' fronts (quartz) clean white stone, their feet (calcite: John's "diorite") marble, the
    # Gyms' feet too.
    for kind, place, x, y, z in SHOPS:
        front = (x - 7, z - 11, x + 7, z - 3, place + (" Pokemon Center" if kind == "center" else " Mart"))
        add("swap", "quartz_block,quartz_stairs,quartz_slab,quartz_pillar,chiseled_quartz_block,quartz_bricks", front, QUARTZ, (str(y - 6), str(y + 3)))
        mk = "marble_" + kind
        add("swap", "calcite,diorite,polished_diorite", front, "block/calcite=%s@2,block/diorite=%s@2,block/polished_diorite=%s@2" % (mk, mk, mk),
            (str(y - 6), str(y - 5)))
    for g in GYMS:
        add("swap", "quartz_block,quartz_stairs,quartz_slab,quartz_pillar,chiseled_quartz_block", g, QUARTZ)
        add("swap", "calcite,diorite", g, "block/calcite=marble_gym@2,block/diorite=marble_gym@2")
    # Pewter: the planters' steps (quartz stairs) white stone; the Museum's window shelves cream, the spruce shelves by
    # its door clean dark wood; its rocks and those by the way east.
    add("swap", "quartz_stairs,quartz_slab", PEWTER, "block/quartz_block_top=white_step,block/quartz_block_side=white_step,block/quartz_block_bottom=white_step", ("29", "32"))
    add("swap", "pale_oak_shelf", MUSEUM, "block/pale_oak_shelf=window_shelf", ("33", "47"))
    add("swap", "spruce_shelf", MUSEUM, "block/spruce_shelf=wood_shelf_dark", ("29", "47"))
    for b in BOULDERS:
        add("swap", BOULDER_BLOCKS, b, BOULDER)
    # Fuchsia City (15:29).
    F = FUCHSIA
    add("sign", "pale_oak_shelf,pale_oak_sign", F)
    add("swap", "nether_brick_fence,nether_brick_wall,chiseled_nether_bricks,black_carpet", F,
        "block/nether_bricks=lamp_plum_post,block/chiseled_nether_bricks=lamp_plum_chisel,block/black_wool=lamp_plum_cap")
    add("swap", "red_glazed_terracotta", F, "block/red_glazed_terracotta=red_pave@2")
    add("swap", "stripped_mangrove_wood,stripped_mangrove_log", F, "block/stripped_mangrove_log=red_kerb,block/stripped_mangrove_log_top=red_kerb")
    add("grass_onto", "red_glazed_terracotta,stripped_mangrove_wood,stripped_mangrove_log", F)
    add("swap", "polished_tuff_wall", F, "block/polished_tuff=wall_grey")
    add("swap", "dark_oak_shelf", F, "block/dark_oak_shelf=shelf_wood")
    add("swap", "coarse_dirt", F, "block/coarse_dirt=planter_soil", ("29", "34"))
    add("swap", "sandstone_slab,sandstone_stairs,sandstone", F, "block/sandstone_top=sand_step,block/sandstone=sand_step,block/sandstone_bottom=sand_step")
    add("swap", "oak_slab,oak_planks,oak_stairs,stripped_oak_wood,stripped_oak_log,oak_shelf,stone_button", (-526, 757, -519, 767, "Fuchsia's bridge"),
        "block/oak_planks=bridge_boards,block/stripped_oak_log=bridge_beam,block/stripped_oak_log_top=bridge_beam,block/oak_shelf=bridge_beam,block/stone=bolt", ("28", "33"))
    for x0, z0, x1, z1, note, n in FU_HOUSES:
        h = (x0, z0, x1, z1, note)
        add("roof_fu%d" % n, "deepslate_tile_stairs", h, "-", ("33", "39"))
        add("swap", "deepslate_tile_slab,deepslate_tiles", h, "block/deepslate_tiles=roof_fu%d_tiles" % n, ("33", "39"))
        add("swap", "white_terracotta", h, "block/white_terracotta=fu_wall_%d" % n, ("30", "39"))
        add("swap", "stripped_dark_oak_wood,stripped_dark_oak_log,dark_oak_slab,dark_oak_planks", h,
            "block/stripped_dark_oak_log=fu_beam,block/stripped_dark_oak_log_top=fu_beam_top,block/dark_oak_planks=fu_beam", ("30", "39"))
        add("swap", "black_concrete", h, "block/black_concrete=fu_plinth", ("29", "32"))
        add("swap", "smooth_stone_slab,smooth_stone", h, "block/smooth_stone_slab_side=house_trim,block/smooth_stone=house_stone", ("29", "33"))
    for x0, z0, x1, z1, note in SAFARI_HOUSES:
        h = (x0, z0, x1, z1, note)
        add("swap", "polished_deepslate,polished_deepslate_slab,polished_deepslate_stairs", h, "block/polished_deepslate=sz_roof", ("33", "45"))
        add("swap", "deepslate_tile_wall", h, "block/deepslate_tiles=sz_roof_rim", ("33", "45"))
        add("swap", "pale_oak_planks,pale_oak_stairs,pale_oak_slab", h, "block/pale_oak_planks=sz_wall", ("30", "45"))
        add("swap", "green_concrete", h, "block/green_concrete=sz_green", ("29", "45"))
        add("swap", "warped_shelf", h, "block/warped_shelf=sz_green_shelf", ("29", "45"))
        add("swap", "warped_slab,warped_planks,warped_stairs", h, "block/warped_planks=sz_green", ("29", "45"))
        add("swap", "oak_planks,oak_stairs,oak_slab", h, "block/oak_planks=sz_wood", ("29", "45"))
        add("swap", "deepslate_brick_wall", h, "block/deepslate_bricks=sz_post", ("29", "45"))
        add("swap", "polished_diorite,polished_diorite_slab", h, "block/polished_diorite=sz_floor", ("29", "33"))
    # The caves (the interiors): metal ladders.
    for ox, w, d, note in ((4352, 20, 16, "Diglett's Cave north entrance"), (4608, 170, 160, "Diglett's Cave B1F"), (4864, 20, 16, "Diglett's Cave south entrance"),
                           (5120, 104, 90, "Mt. Moon 1F"), (5376, 106, 86, "Mt. Moon B1F"), (5632, 106, 90, "Mt. Moon B2F")):
        room("swap", "ladder", (ox - 1, -1, ox + w, d, note), "block/ladder=metal_ladder")
    # Viridian Forest's gatehouses seen from inside the forest (15:21): their doorways' daylight at the end of the dark.
    fx = 4096
    for x0, z0, x1, z1, note in ((22, 23, 41, 42, "Viridian Forest's north gatehouse"), (54, 138, 73, 157, "Viridian Forest's south gatehouse")):
        room("swap", "white_concrete", (fx + x0, z0, fx + x1, z1, note), "block/white_concrete=cave_daylight!", ("60", "80"))


LAMP46 = ("block/tuff=lamp_post,block/polished_tuff=lamp_post,block/chiseled_tuff_bricks=lamp_chisel,"
          "block/chiseled_tuff_bricks_top=lamp_chisel_top,block/gray_wool=lamp_cap")
QUARTZ = ("block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean,"
          "block/quartz_pillar=quartz_clean_pillar,block/quartz_pillar_top=quartz_clean,block/chiseled_quartz_block=quartz_clean,"
          "block/chiseled_quartz_block_top=quartz_clean,block/quartz_bricks=quartz_clean")


if __name__ == "__main__":
    main()
