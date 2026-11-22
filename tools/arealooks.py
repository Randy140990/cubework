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
PLAMP = ("block/tuff=plamp_post,block/polished_tuff=plamp_post,block/chiseled_tuff_bricks=plamp_foot,"
         "block/chiseled_tuff_bricks_top=plamp_foot,block/gray_wool=plamp_cap,block/redstone_lamp=plamp_globe,"
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
    round47(add, room)
    round48(add, room)
    round48b(add, room)
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
    add("swap", "mud_brick_stairs", K, "block/mud_bricks=step_boards_d")
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



CERULEAN = (-401, 220, -311, 305, "Cerulean City")
CERULEAN_LAMPS = [(-384, 270), (-367, 271), (-359, 289), (-353, 271), (-349, 244), (-323, 256), (-323, 289)]
POWER_PLANT = (-160, 305, -120, 333, "the Power Plant")


def round47(add, room):
    """0.47.0's looks (16:17, 16:58): see tools/looks47.py for the pictures."""
    import looks47
    C = CERULEAN
    # Cerulean City's houses: their roofs (prismarine brick) a new style in their teal, their brick feet, their grey
    # pillars, the white trim round their windows, the steps at their doors; the checkered paving as grey-blue
    # paving stones, its smooth stone border a kerb, the grass rounded onto it.
    add("roof_cer", "prismarine_brick_stairs", C, "-", ("36", "42"))
    add("swap", "prismarine_brick_slab,prismarine_bricks", C, "block/prismarine_bricks=roof_cer_tiles", ("34", "42"))
    add("swap", "bricks", C, "block/bricks=base_cer", ("34", "34"))
    add("swap", "light_gray_concrete", C, "block/light_gray_concrete=pillar_cer", ("34", "41"))
    add("swap", "polished_diorite_stairs,polished_diorite_slab", C, "block/polished_diorite=trim_cer", ("36", "41"))
    add("swap", "polished_andesite_slab", C, "block/polished_andesite=step_cer", ("34", "34"))
    add("swap", "white_concrete,light_blue_concrete,light_blue_terracotta,white_terracotta", C,
        "block/white_concrete=pave_cer_1,block/light_blue_concrete=pave_cer_2,block/light_blue_terracotta=pave_cer_3,block/white_terracotta=pave_cer_4", ("33", "33"))
    add("swap", "smooth_stone_slab", C, "block/smooth_stone=kerb_cer,block/smooth_stone_slab_side=kerb_cer", ("33", "33"))
    add("kerb", "smooth_stone_slab", C, "-", ("33", "33"))
    # Its double lamps: the diorite posts and polished diorite tops off-white metal.
    # (0.48: the whole lamp, its carpet top a cap, its glass, its lights; round48.)
    # The Bike Shop: its sign (the white shelves out front), its awning in fabric.
    bike = (-381, 279, -366, 284, "the Bike Shop's front")
    add("sign", "pale_oak_shelf", bike, "-", ("34", "35"))
    add("swap", "acacia_shelf,acacia_stairs,pale_oak_stairs", bike, "block/acacia_shelf=awning_orange,block/acacia_planks=awning_orange,block/pale_oak_planks=awning_white", ("38", "40"))
    # Nugget Bridge in gold (HeartGold's).
    add("swap", "stripped_oak_wood,oak_stairs,oak_shelf,stone_button,oak_slab,oak_planks", (-357, 176, -348, 235, "Nugget Bridge"),
        "block/stripped_oak_log=bridge_gold_beam,block/oak_planks=bridge_gold_boards,block/oak_shelf=bridge_gold_rail,block/stone=gold_bolt", ("33", "36"))
    # The hedges and bushes (Cerulean's, Route 24's and 25's, the maze to Bill's, the four by the Rock Tunnel Center).
    for line in open(os.path.join(HERE, "area_hedges.tsv")):
        if not line.strip() or line.startswith("#"):
            continue
        x0, z0, x1, z1, y0, y1, note = line.rstrip("\n").split("\t")
        add("hedge", looks47.HEDGE_BLOCKS, (int(x0), int(z0), int(x1), int(z1), note), looks47.hedge_spec(), (y0, y1))
    # The signs out front (16:20): each Underground Path hut's, and the Rock Tunnel Center's (only these: pale oak
    # shelves are planters elsewhere, Saffron's).
    for x0, z0, x1, z1, y0, y1, note in ((-336, 370, -333, 372, 33, 34, "Route 5's Underground Path sign"), (-332, 493, -329, 495, 31, 32, "Route 6's Underground Path sign"),
                                         (-431, 434, -428, 436, 32, 33, "Route 7's Underground Path sign"), (-268, 414, -265, 416, 33, 34, "Route 8's Underground Path sign"),
                                         (-141, 287, -139, 289, 33, 34, "the Rock Tunnel Center's sign")):
        add("sign", "pale_oak_shelf", (x0, z0, x1, z1, note), "-", (str(y0), str(y1)))
    # Route 5's Day Care (16:17): HeartGold's red tiles on its mangrove roof, its brick foot as Cerulean's.
    care = (-361, 317, -342, 337, "the Day Care")
    add("roof", "mangrove_stairs", care, "-", ("39", "45"))
    add("swap", "mangrove_slab,mangrove_planks", care, "block/mangrove_planks=roof_tiles", ("38", "45"))
    add("swap", "bricks", care, "block/bricks=base_cer", ("36", "38"))
    # The rock piles (by the Gym, and in the maze to Bill's): stones, as Pewter's and Fuchsia's.
    for b in ((-349, 259, -344, 263, "Cerulean's rocks by the Gym"), (-287, 146, -282, 150, "Route 25's rocks"), (-269, 159, -265, 163, "Route 25's rocks"),
              (-243, 164, -240, 167, "Route 25's rocks")):
        add("swap", BOULDER_BLOCKS + ",mossy_cobblestone_slab", b, BOULDER, ("34", "36"))
    # Bill's Sea Cottage: its bamboo, its white trim, the chimney and the shelves round it, the decking, the cream path.
    bill = (-226, 141, -209, 158, "Bill's Sea Cottage")
    add("swap", "bamboo_stairs,bamboo_planks,bamboo_slab", bill, "block/bamboo_planks=bill_roof", ("36", "45"))
    add("swap", "pale_oak_stairs,pale_oak_slab,polished_diorite_stairs", bill, "block/pale_oak_planks=trim_white,block/polished_diorite=trim_white", ("36", "45"))
    add("swap", "cyan_terracotta", bill, "block/cyan_terracotta=chimney_stone", ("40", "45"))
    add("swap", "bamboo_shelf", bill, "block/bamboo_shelf=bill_shelf", ("43", "45"))
    add("swap", "spruce_slab,spruce_shelf", bill, "block/spruce_planks=deck_boards,block/spruce_shelf=deck_shelf", ("33", "41"))
    add("swap", "end_stone_bricks", (-224, 152, -210, 178, "the path to Bill's"), "block/end_stone_bricks=path_cream", ("33", "35"))
    # The Power Plant (16:58): its iron very metallic, its yellow, red and white painted metal, its roof, its red towers,
    # its fronts and grey walls, its steel; the smooth slab pillars as the houses'; its sign.
    P = POWER_PLANT
    add("swap", "iron_block", P, "block/iron_block=pp_iron")
    add("swap", "iron_trapdoor", P, "block/iron_trapdoor=pp_grate")
    add("swap", "yellow_terracotta", P, "block/yellow_terracotta=pp_yellow")
    add("swap", "red_terracotta", P, "block/red_terracotta=pp_red")
    add("swap", "smooth_quartz_slab,smooth_quartz,quartz_block", P, "block/quartz_block_bottom=pp_white,block/quartz_block_top=pp_white,block/quartz_block_side=pp_white")
    add("swap", "crimson_stairs,crimson_planks,crimson_slab,mangrove_planks,mangrove_fence,mangrove_stairs", P, "block/crimson_planks=pp_roof,block/mangrove_planks=pp_roof")
    add("swap", "red_nether_brick_wall,red_nether_bricks", P, "block/red_nether_bricks=pp_red_frame")
    add("swap", "polished_diorite,polished_diorite_stairs,polished_diorite_slab", P, "block/polished_diorite=pp_front")
    add("swap", "light_gray_concrete", P, "block/light_gray_concrete=pp_wall_grey")
    add("swap", "polished_andesite", P, "block/polished_andesite=pp_steel")
    add("swap", "white_concrete", P, "block/white_concrete=pp_white", ("34", "50"))
    add("swap", "deepslate_brick_wall", P, "block/deepslate_bricks=pp_post")
    add("swap", "smooth_stone_slab,smooth_stone", P, "block/smooth_stone_slab_side=house_trim,block/smooth_stone=house_stone")
    add("sign", "pale_oak_shelf", (-146, 330, -143, 332, "the Power Plant's sign"), "-", ("34", "35"))



def round48(add, room):
    """0.48.0's looks (17:42 to 17:53): see tools/looks48.py for the pictures."""
    # Seafoam Islands (tools/caves48.py: its five floors at x 6656 on, 128 apart, 84 by 64): its rock ice, its walls
    # packed snow, its floor frosted stone, its sand, its steps glassy ice, its edges at the water icy rock, its boulders
    # ice shards, its signs as HeartGold's.
    SEA = ("block/packed_ice=sf_ice_top|sf_ice@2,block/snow=sf_snow_top|sf_snow@2,block/calcite=sf_floor@4,block/end_stone=sf_sand@4,"
           "block/polished_diorite=sf_step,block/jungle_planks=sf_edge,block/smooth_sandstone=cave_mat@2,block/smooth_sandstone_top=cave_mat@2,"
           "block/sandstone_top=cave_mat@2")
    for k, fl in enumerate(("1F", "B1F", "B2F", "B3F", "B4F")):
        ox = 6656 + 128 * k
        box = (ox - 1, -1, ox + 84, 64, "Seafoam Islands " + fl)
        room("swap", "packed_ice,snow_block,calcite,end_stone,polished_diorite,polished_diorite_stairs,jungle_stairs,smooth_sandstone", box, SEA)
        room("boulder", "waxed_copper_grate,waxed_exposed_copper_grate", box, "prefix:iboulder")
        room("sign", "pale_oak_shelf", box)
    # Rock Tunnel (two floors at x 7296 and 7424): its rock warm grey brown in rough slabs (the andesite, its tops the
    # diorite), its floor brown earth, its rocks craggy boulders, the cracked ones (ROCK SMASH) darker and split.
    RT = ("block/andesite=rt_rock_top|rt_rock@2,block/diorite=rt_rock_top|rt_rock@2,block/coarse_dirt=rt_floor@4,block/rooted_dirt=rt_floor@4,"
          "block/dirt=rt_floor@4,block/smooth_sandstone=cave_mat@2,block/smooth_sandstone_top=cave_mat@2,block/sandstone_top=cave_mat@2")
    for ox, w, d, fl in ((7296, 112, 94, "1F"), (7424, 110, 96, "B1F")):
        box = (ox - 1, -1, ox + w, d, "Rock Tunnel " + fl)
        room("swap", "andesite,diorite,coarse_dirt,rooted_dirt,dirt,smooth_sandstone,andesite_slab", box, RT)
        room("boulder", "waxed_copper_grate,waxed_exposed_copper_grate", box, "prefix:rboulder")
        room("boulder", "waxed_weathered_copper_grate", box, "prefix:sboulder")
        room("sign", "pale_oak_shelf", box)
        room("swap", "white_concrete", box, "block/white_concrete=cave_daylight!")


VERMILION = (-402, 546, -300, 614, "Vermilion City")
VERMILION_LAMPS = [(-386, 562), (-358, 569), (-357, 587), (-338, 565), (-330, 587)]
# The small trees (17:51): a fence post for a trunk, a few leaves (not the big 4x4 trees). Their boxes, from the map
# (tools: the fence posts with leaves on them, the clumps 4 by 4 or smaller).
SMALL_TREES = [
    (-823, 271, -819, 276, 33, 38, "Pewter's tree by the Museum"), (-818, 277, -787, 281, 31, 34, "Pewter's little trees along the north"),
    (-788, 285, -783, 289, 30, 35, "Pewter's tree by the Museum's side"), (-770, 294, -765, 298, 30, 35, "Pewter's tree by the house"),
    (-352, 257, -347, 261, 35, 40, "Cerulean's tree by the Gym"),
    (-318, 575, -311, 582, 33, 39, "Vermilion's trees by the field"), (-363, 590, -359, 594, 33, 38, "Vermilion's tree by the Gym"),
    (-496, 756, -492, 761, 32, 37, "Fuchsia's tree"), (-480, 756, -475, 761, 32, 37, "Fuchsia's tree"),
    (-519, 767, -467, 772, 30, 35, "Fuchsia's row of trees"), (-496, 783, -488, 790, 32, 38, "Fuchsia's trees by the pond"),
    (-481, 794, -477, 798, 31, 36, "Fuchsia's tree"), (-520, 815, -516, 819, 33, 38, "Fuchsia's tree"),
    (-489, 816, -485, 832, 32, 37, "Fuchsia's trees on the beach"), (-510, 823, -506, 827, 32, 37, "Fuchsia's tree"),
]
UG_HUTS = [(-337, 33, 366, "Route 5's"), (-332, 31, 490, "Route 6's"), (-432, 32, 430, "Route 7's"), (-272, 33, 411, "Route 8's")]


def round48b(add, room):
    """0.48.0's town looks (17:42 to 17:53): see tools/looks48b.py for the pictures."""
    import looks47
    hedge_spec = looks47.hedge_spec() + ",block/flowering_azalea_leaves=hedge_fa"
    hedge_blocks = looks47.HEDGE_BLOCKS + ",flowering_azalea_leaves"
    # The double lamps, whole (Cerulean's off-white, Vermilion's off-grey): the diorite posts and caps metal, the carpet
    # tops a cap, the black glass round the lights warm frosted glass running on seamless, the sea lanterns lit globes.
    for lamps, nm, post, cap in ((CERULEAN_LAMPS, "Cerulean", "lamp_white_post", "lamp_white_cap"), (VERMILION_LAMPS, "Vermilion", "lamp_ver_post", "lamp_ver_cap")):
        top = "lamp_cer_top" if nm == "Cerulean" else "lamp_ver_top"
        for x, z in lamps:
            b = (x - 3, z - 3, x + 3, z + 3, "a %s lamp" % nm)
            add("swap", "diorite_wall,polished_diorite,light_gray_carpet,sea_lantern", b,
                "block/diorite=%s,block/polished_diorite=%s,block/light_gray_wool=%s,block/sea_lantern=lamp_globe_warm!" % (post, cap, top), ("30", "42"))
            add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ("30", "42"))
    # Vermilion City in HeartGold's colours (17:51).
    VE = VERMILION
    add("roof_ver", "acacia_stairs", VE, "-", ("33", "50"))
    add("swap", "acacia_slab,acacia_planks", VE, "block/acacia_planks=roof_ver_tiles", ("33", "50"))
    add("swap", "white_terracotta", VE, "block/white_terracotta=wall_ver", ("31", "46"))
    add("swap", "bricks,brick_stairs,brick_slab", VE, "block/bricks=base_ver")
    add("swap", "polished_diorite_stairs", VE, "block/polished_diorite=trim_ver", ("31", "46"))
    add("swap", "white_concrete,yellow_terracotta,light_gray_concrete,white_terracotta", VE,
        "block/white_concrete=pave_ver_1,block/yellow_terracotta=pave_ver_2,block/light_gray_concrete=pave_ver_3,block/white_terracotta=pave_ver_4", ("30", "30"))
    add("glass", "terracotta", VE, "block/terracotta=border_ver", ("30", "30"))
    add("swap", "light_gray_terracotta", VE, "block/light_gray_terracotta=ground_ver", ("29", "30"))
    add("grass_onto", "terracotta,light_gray_terracotta", VE, "-", ("30", "30"))
    add("swap", "smooth_stone", VE, "block/smooth_stone=kerb_ver", ("30", "30"))
    add("swap", "stone", VE, "block/stone=canal_ver", ("22", "29"))
    add("swap", "barrel", VE, "block/barrel_side=barrel_ver_side,block/barrel_top=barrel_ver_top,block/barrel_top_open=barrel_ver_top,block/barrel_bottom=barrel_ver_top")
    add("swap", "oak_trapdoor,spruce_trapdoor", VE, "block/oak_trapdoor=planter_ver,block/spruce_trapdoor=planter_ver")
    add("swap", "oak_shelf,dark_oak_shelf", VE, "block/oak_shelf=planter_rim,block/dark_oak_shelf=planter_rim", ("31", "31"))
    add("swap", "blackstone_wall", VE, "block/blackstone=dock_post")
    add("swap", "pale_oak_fence", VE, "block/pale_oak_planks=fence_grey")
    add("hedge", hedge_blocks, (-340, 578, -316, 581, "Vermilion's hedge by the field"), hedge_spec, ("31", "32"))
    gym = (-382, 584, -360, 597, "Vermilion Gym")
    add("roof_vgym", "jungle_stairs", gym, "-", ("38", "42"))
    add("swap", "jungle_planks,jungle_slab,stripped_jungle_log", gym, "block/jungle_planks=roof_vgym_tiles,block/stripped_jungle_log=roof_vgym_edge", ("38", "42"))
    add("swap", "light_gray_concrete", gym, "block/light_gray_concrete=vgym_wall", ("32", "38"))
    add("swap", "oak_planks", gym, "block/oak_planks=planter_ver", ("35", "40"))
    add("swap", "quartz_block", gym, "block/quartz_block_side=kerb_ver,block/quartz_block_top=kerb_ver,block/quartz_block_bottom=kerb_ver", ("31", "34"))
    add("swap", "pink_terracotta", gym, "block/pink_terracotta=base_ver")
    # The small trees: their fence posts a little trunk, their leaves a rounded, tufted crown.
    for x0, z0, x1, z1, y0, y1, note in SMALL_TREES:
        add("boulder", "spruce_fence,oak_fence,dark_oak_fence", (x0, z0, x1, z1, note), "prefix:ttrunk", (str(y0), str(y1)))
        add("hedge", hedge_blocks, (x0, z0, x1, z1, note), hedge_spec, (str(y0), str(y1)))
    # The hedges (17:42 to 17:53): Pewter Gym's bushes, Fuchsia's flowering azalea round its ponds.
    add("hedge", hedge_blocks, (-819, 297, -797, 300, "Pewter Gym's bushes"), hedge_spec, ("30", "31"))
    add("hedge", "flowering_azalea_leaves", FUCHSIA, hedge_spec, ("28", "33"))
    # The steps at the water (the jungle stairs standing in it) as the shore's wet stones: Cerulean's, Fuchsia's.
    add("swap", "jungle_stairs,jungle_slab", (-401, 220, -311, 305, "Cerulean's water's edge"), "block/jungle_planks=shore_step", ("30", "34"))
    add("swap", "jungle_stairs,jungle_slab", FUCHSIA, "block/jungle_planks=shore_step", ("27", "31"))
    # Fuchsia: the dry grass, the pink petals (laid over four blocks, so each patch differs), the slab steps up from
    # the red path in its border's red.
    add("swap", "short_dry_grass,tall_dry_grass", FUCHSIA, "block/short_dry_grass=dry_grass_short!,block/tall_dry_grass=dry_grass_tall_bottom!")
    add("swap", "pink_petals", FUCHSIA, "block/pink_petals=petals_big@4!")
    add("swap", "mangrove_slab", FUCHSIA, "block/mangrove_planks=red_kerb", ("29", "31"))
    # Bill's Sea Cottage: the slabs to its door, its pillars (smooth stone) on their dark plinths (polished blackstone bricks).
    bill = (-226, 141, -209, 158, "Bill's Sea Cottage")
    add("swap", "smooth_stone,smooth_stone_slab,polished_blackstone_bricks,polished_blackstone_brick_slab,polished_blackstone_brick_stairs", bill,
        "block/smooth_stone=bill_pillar,block/smooth_stone_slab_side=bill_step,block/polished_blackstone_bricks=bill_plinth")
    # The Bike Shop: its oak posts, the awning's bottom row a scalloped valance, BIKE SHOP across its front.
    add("swap", "stripped_oak_wood,stripped_oak_log", (-377, 280, -366, 282, "the Bike Shop's posts"), "block/stripped_oak_log=bike_post,block/stripped_oak_log_top=bike_post", ("35", "37"))
    add("swap", "acacia_stairs,pale_oak_stairs", (-377, 282, -366, 282, "the Bike Shop's valance"), "block/acacia_planks=awning_val_o,block/pale_oak_planks=awning_val_w", ("38", "38"))
    for i in range(8):
        add("swap", "acacia_shelf", (-375 + i, 281, -375 + i, 281, "the Bike Shop's lettering"), "block/acacia_shelf=awning_l%d" % i, ("39", "39"))
    # Route 4's steps by the POKéMON CENTER (cobblestone and mossy cobblestone) as the boarded steps.
    add("swap", "cobblestone_stairs,mossy_cobblestone_stairs,cobblestone_slab,mossy_cobblestone_slab", (-625, 245, -590, 275, "Route 4's steps by the Center"),
        "block/cobblestone=step_boards_d,block/mossy_cobblestone=step_boards_d")
    # Route 5: the bushes by the Day Care.
    add("swap", "bush", (-372, 306, -330, 376, "Route 5's bushes"), "block/bush=bush_round!")
    # The four Underground Path huts, as the bigger buildings: a slate blue tiled roof, clean walls, dressed stone at
    # the foot, white posts, dark metal rails, the steps.
    for x, y, z, note in UG_HUTS:
        h = (x - 8, z - 10, x + 8, z + 4, note + " Underground Path hut")
        add("roof_ug", "birch_stairs", h, "-", (str(y + 2), str(y + 10)))
        add("swap", "birch_slab,birch_planks", h, "block/birch_planks=roof_ug_tiles", (str(y + 2), str(y + 10)))
        add("swap", "polished_deepslate,polished_deepslate_stairs", h, "block/polished_deepslate=wall_ug", (str(y), str(y + 8)))
        add("swap", "andesite,polished_andesite_stairs,polished_andesite_slab,polished_andesite", h,
            "block/andesite=base_ug,block/polished_andesite=pillar_cer", (str(y - 1), str(y + 8)))
        add("swap", "stripped_oak_log,stripped_jungle_log,light_gray_concrete", h,
            "block/stripped_oak_log=post_ug,block/stripped_oak_log_top=post_ug,block/stripped_jungle_log=post_ug,block/stripped_jungle_log_top=post_ug,"
            "block/light_gray_concrete=pillar_cer", (str(y), str(y + 8)))
        add("swap", "deepslate_tile_wall", h, "block/deepslate_tiles=rail_ug", (str(y), str(y + 8)))
        add("swap", "polished_deepslate_slab", h, "block/polished_deepslate=step_cer", (str(y - 1), str(y + 1)))
    # Inside: Cerulean Gym's pool in tiles (white walkways, blue floor, a blue-edged rim); LT. SURGE's electric door.
    room("swap", "smooth_quartz,quartz_block,smooth_quartz_slab,smooth_quartz_stairs,quartz_slab,quartz_stairs", (2559, -1, 2582, 26, "Cerulean Gym's pool"),
         "block/quartz_block_bottom=pool_tile,block/quartz_block_top=pool_edge_tile,block/quartz_block_side=pool_tile")
    room("swap", "black_concrete", (2559, -1, 2582, 26, "Cerulean Gym's pool"), "block/black_concrete=pool_floor")
    room("swap", "light_blue_stained_glass", (3199, -1, 3215, 23, "LT. SURGE's electric door"), "block/light_blue_stained_glass=surge_barrier!")


LAMP46 = ("block/tuff=lamp_post,block/polished_tuff=lamp_post,block/chiseled_tuff_bricks=lamp_chisel,"
          "block/chiseled_tuff_bricks_top=lamp_chisel_top,block/gray_wool=lamp_cap")
QUARTZ = ("block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean,"
          "block/quartz_pillar=quartz_clean_pillar,block/quartz_pillar_top=quartz_clean,block/chiseled_quartz_block=quartz_clean,"
          "block/chiseled_quartz_block_top=quartz_clean,block/quartz_bricks=quartz_clean")


if __name__ == "__main__":
    main()
