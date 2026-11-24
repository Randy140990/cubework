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
               "stripped_oak_log,stripped_oak_wood,stripped_jungle_log,stripped_jungle_wood,black_concrete,"
               # ((0.54, 12:24: "just under the top near the door ... still some old textures") the walls by the door, the
               # stone at its foot, the spruce corners, the dark wool and the beams of the Johto Gate's doorway.)
               "andesite_wall,stone,stripped_spruce_wood,stripped_spruce_log,black_wool,stripped_warped_stem")
GATE = ("block/polished_deepslate=gate_roof,block/deepslate_tiles=gate_roof_rim,block/andesite=gate_pillar,block/polished_andesite=gate_stone,"
        "block/birch_planks=gate_wall_birch,block/stripped_oak_log=gate_wall_oak,block/stripped_oak_log_top=gate_wall_oak_top,"
        "block/stripped_jungle_log=gate_beam,block/stripped_jungle_log_top=gate_beam_top,block/black_concrete=gate_dark,"
        "block/stone=gate_stone,block/stripped_spruce_log=gate_wall_oak,block/stripped_spruce_log_top=gate_wall_oak_top,block/black_wool=gate_dark,"
        "block/stripped_warped_stem=gate_beam,block/stripped_warped_stem_top=gate_beam_top")
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
# (0.54, 12:19 and 12:26) Every Gym's coloured band (the second row up), each its own colour (tools/looks54.py), and
# the height and blocks of each one's (a scan of the map).
GYM_BANDS = {"viridian_city": (27, "terracotta"), "pewter_city": (31, "gray_concrete"), "cerulean_city": (35, "yellow_concrete"),
             "vermilion_city": (32, "pink_terracotta"), "celadon_city": (34, "magenta_concrete"), "fuchsia_city": (32, "magenta_terracotta"),
             "saffron_city": (34, "blue_terracotta"), "cinnabar_island": (19, "red_concrete_powder,stripped_mangrove_log"), "azalea_town": (21, "stripped_mangrove_log"),
             "cianwood_city": (19, "blue_terracotta"), "blackthorn_city": (32, "stripped_mangrove_log"), "ecruteak_city": (22, "blue_terracotta"),
             "mahogany_town": (23, "blue_terracotta"), "olivine_city": (20, "stripped_mangrove_log,blue_terracotta"),
             "goldenrod_city": (21, "stripped_mangrove_log,blue_terracotta"), "violet_city": (23, "stripped_mangrove_log")}
GYM_GREY = ("block/jungle_planks=gym_flat_grey,block/oak_planks=gym_flat_light_grey,block/stripped_jungle_log=gym_beam_grey,"
            "block/stripped_jungle_log_top=gym_beam_top_grey")
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
    # (17:42: every route, all of Kanto, not only Pallet.)
    add("grass_edge", "dripstone_block", KANTO)
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
    # (0.52, 09:01: "all of it should align") Its stairs face two ways and John set stone among them: laid from where
    # each block is, so every one's roof runs on from the next.
    add("swap", "polished_andesite_slab,polished_andesite_stairs,stone", LAB, "block/polished_andesite=lab_roof@1,block/stone=lab_roof@1", ("25", "26"))
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
        # ((0.54, 12:26) Johto's Gyms: their roofs and beams grey, Kanto's tan as they are.)
        johto = int(x0) < -1010
        add("roof_gymg" if johto else "roof_gym", "jungle_stairs,oak_stairs", box, "-", (y0, str(int(y1) + 1)))
        add("swap", "jungle_planks,oak_planks,stripped_jungle_log,jungle_slab,oak_slab", box, GYM_GREY if johto else GYM, (y0, str(int(y1) + 1)))
        if place in GYM_BANDS:
            by, blocks = GYM_BANDS[place]
            tex = ",".join("block/%s=gym_band_%s" % (b, place) + (",block/%s_top=gym_band_%s" % (b, place) if b.endswith("_log") else "") for b in blocks.split(","))
            add("swap", blocks, (int(x0) - 3, int(z0) - 3, int(x1) + 3, int(z1) + 3, place + " Gym's band"), tex, (str(by), str(by)))
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
    round49(add, room)
    round50(add, room)
    # (0.52) Every look all Kanto has, on Route 28 and SILVER CAVE's outside too (west of the Johto Gate, 09:01).
    kanto_rows = [r for r in rows if len(r) == 10 and tuple(r[2:6]) == KANTO[:4]]
    for r in kanto_rows:
        rows.append((r[0], r[1]) + ROUTE28[:4] + (r[6], r[7], r[8], ROUTE28[4]))
    # (0.53) GOLDENROD CITY (Johto, 11:16: "we use the exact same buildings"): every look all Kanto has; and Celadon's
    # own, its buildings being Celadon's, thirteen blocks lower (its ground's 19, Celadon's 32), but for its RADIO
    # TOWER (its own, for later; the MAGNET TRAIN station west over the water isn't in the boxes either).
    for r in kanto_rows:
        rows.append((r[0], r[1]) + GOLDENROD[:4] + (r[6], r[7], r[8], GOLDENROD[4]))
    # (0.54, 12:24) All JOHTO: every look all Kanto has (its cliffs, the grass round its water, its trees' bark and
    # leaves, its glass, its fences, its lamps).
    for r in kanto_rows:
        # ((0.56, 13:53) Johto's mud brick slabs are John's steps, not Kanto's ponds' rocks: round56's steps.)
        if r[1] == "mud_brick_slab": continue
        # ((0.59.3, 20:35: "we've got moss around the end, and it's being impacted and turning into grass") Johto's
        # moss rings round the grass stay moss: the grass's tufts aren't laid on them.)
        if r[0] == "grass_onto" and r[1] == "moss_block": continue
        rows.append((r[0], r[1]) + JOHTO[:4] + (r[6], r[7], r[8], JOHTO[4]))
    # Johto's towns built as Kanto's (12:24, 12:26): each Kanto town's own looks over the Johto town, the heights moved
    # to its ground (the lamps', signs' and other one-offs' left out).
    def inside(r, a): return a[0] <= r[2] and r[4] <= a[2] and a[1] <= r[3] and r[5] <= a[3]
    one_off = ("lamp", "sign", "bike", "statue", "mailbox", "boulder", "cut tree", "rock", "Snorlax", "mouth", "bench", "pond", "fountain", "bridge")
    town_rows = [r for r in rows if len(r) == 10]
    for src, dsts in TOWN_COPIES:
        for r in town_rows:
            if not inside(r, src) or any(w in r[9] for w in one_off) or r[0] in ("sign", "wood_sign", "wood_board", "prop", "prop_only", "hide"): continue
            for dst, dy, note in dsts:
                # ((0.56, 13:53) A building's own looks (a Mart's pillars, the lab's, Viridian's chimneys) stay on their
                # buildings: copied over a whole Johto town they went on everything there of the same block. The Johto
                # buildings have their own (round56); Route 26's two houses are Pallet's two.)
                if dst is R26_HOUSES:
                    # (Pallet's houses' own looks only: the lab's pillars went on Route 26's cliffs.)
                    if not any(inside(r, h) for h in HOUSES): continue
                elif tuple(r[2:6]) != src[:4]: continue
                mv = lambda y: y if y in ("-", None) else str(int(y) + dy)
                rows.append((r[0], r[1]) + dst[:4] + (mv(r[6]), mv(r[7]), r[8], note))
    lower = lambda y: y if y in ("-", None) else str(int(y) - 13)
    for r in [r for r in rows if len(r) == 10 and tuple(r[2:6]) == CELADON[:4]]:
        blocks = ",".join(b for b in r[1].split(",") if b != "quartz_pillar")
        for b in GOLD_BOXES:
            rows.append((r[0], blocks) + b + (lower(r[6]), lower(r[7]), r[8], "Goldenrod City"))
    import arealooks57
    rows[:] = arealooks57.finish(rows, globals())
    import arealooks596
    rows[:] = arealooks596.post(rows, globals())
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
    add("swap", "jungle_stairs,jungle_slab", K, "block/jungle_planks=drip_rock_top|drip_rock@4,block/dripstone_block=drip_rock_top|drip_rock@4")
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
        pass  # (0.50: all the hedges are found on the map now, round50.)
    # The signs out front (16:20): each Underground Path hut's, and the Rock Tunnel Center's (only these: pale oak
    # shelves are planters elsewhere, Saffron's).
    for x0, z0, x1, z1, y0, y1, note in ((-336, 370, -333, 372, 33, 34, "Route 5's Underground Path sign"), (-332, 493, -329, 495, 31, 32, "Route 6's Underground Path sign"),
                                         (-431, 434, -428, 436, 32, 33, "Route 7's Underground Path sign"), (-268, 414, -265, 416, 33, 34, "Route 8's Underground Path sign"),
                                         (-141, 287, -139, 289, 33, 34, "the Rock Tunnel Center's sign")):
        add("sign", "pale_oak_shelf", (x0, z0, x1, z1, note), "-", (str(y0), str(y1)))
    # Route 5's Day Care (16:17): HeartGold's red tiles on its mangrove roof, its brick foot as Cerulean's.
    # (0.50.3: its rows are round50's CARE_HOUSES, shared with Routes 12 and 16's house, the same build.)
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
    # The hedges (17:42 to 17:53): Pewter Gym's bushes, Fuchsia's flowering azalea round its ponds.
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
    # The four Underground Path huts, as the bigger buildings: a slate blue tiled roof, clean walls, dressed stone at
    # the foot, white posts, dark metal rails, the steps.
    for x, y, z, note in UG_HUTS:
        # (21:58) As Let's Go's entrances and in line with the gatehouses: the gatehouses' grey slab roof and its darker
        # rim, a grey stone frame round the dark doorway, warm cream brick either side, stone steps.
        h = (x - 8, z - 10, x + 8, z + 4, note + " Underground Path hut")
        add("swap", "polished_deepslate,polished_deepslate_stairs", h, "block/polished_deepslate=gate_roof", (str(y), str(y + 10)))
        add("swap", "deepslate_tile_wall", h, "block/deepslate_tiles=gate_roof_rim", (str(y), str(y + 10)))
        add("swap", "polished_deepslate_slab", h, "block/polished_deepslate=step_cer", (str(y - 1), str(y + 1)))
        add("swap", "polished_deepslate_slab", h, "block/polished_deepslate=gate_roof_rim", (str(y + 2), str(y + 10)))
        add("swap", "birch_stairs,birch_slab,birch_planks", h, "block/birch_planks=gate_pillar", (str(y), str(y + 10)))
        add("swap", "stripped_oak_log,stripped_jungle_log", h,
            "block/stripped_oak_log=gate_pillar,block/stripped_oak_log_top=gate_pillar,block/stripped_jungle_log=gate_pillar,block/stripped_jungle_log_top=gate_pillar",
            (str(y), str(y + 10)))
        add("swap", "andesite", h, "block/andesite=ug_brick", (str(y), str(y + 10)))
        add("swap", "andesite", h, "block/andesite=gate_stone", (str(y - 1), str(y - 1)))
        add("swap", "light_gray_concrete,polished_andesite_stairs,polished_andesite_slab,polished_andesite", h,
            "block/light_gray_concrete=gate_stone,block/polished_andesite=gate_stone", (str(y - 1), str(y + 10)))
        add("swap", "black_concrete", h, "block/black_concrete=cave_dark", (str(y), str(y + 6)))
    # Inside: Cerulean Gym's pool in tiles (white walkways, blue floor, a blue-edged rim); LT. SURGE's electric door.
    room("swap", "smooth_quartz,quartz_block,smooth_quartz_slab,smooth_quartz_stairs,quartz_slab,quartz_stairs", (2559, -1, 2582, 26, "Cerulean Gym's pool"),
         "block/quartz_block_bottom=pool_tile,block/quartz_block_top=pool_edge_tile,block/quartz_block_side=pool_tile")
    room("swap", "dark_prismarine", (2559, -1, 2582, 26, "Cerulean Gym's pool"), "block/dark_prismarine=pool_floor")
    room("swap", "light_blue_stained_glass", (3199, -1, 3215, 23, "LT. SURGE's electric door"), "block/light_blue_stained_glass=surge_barrier!")


LAVENDER = (-158, 398, -104, 447, "Lavender Town")
LAV_TOWER = (-128, 401, -108, 420, "the Pokemon Tower")
LAV_HOUSES = [(-147, 420, -132, 431, "Lavender's Volunteer House"), (-155, 431, -128, 442, "Lavender's two houses")]
LAV_SHOPS = [(-150, 405, -134, 420, "Lavender's Pokemon Center"), (-125, 429, -110, 442, "Lavender's Mart")]
LAV_LAMPS = [(-151, 421), (-135, 404), (-127, 425), (-127, 443)]


def round49(add, room):
    """0.49.0's looks (19:06): Lavender Town at night (tools/looks49.py)."""
    LV = LAVENDER
    # Its streets: purple-grey flagstones, darker ones where John laid deepslate, a lilac kerb, the grass running
    # onto them all; a low lilac mist over the ground.
    add("swap", "gray_concrete", LV, "block/gray_concrete=lav_flag@2", ("32", "32"))
    add("swap", "deepslate_bricks,cracked_deepslate_bricks,deepslate_tiles", LV,
        "block/deepslate_bricks=lav_flag_dark@2,block/cracked_deepslate_bricks=lav_flag_dark@2,block/deepslate_tiles=lav_flag_dark@2", ("32", "32"))
    add("swap", "stone", LV, "block/stone=lav_kerb", ("33", "33"))
    add("grass_onto", "gray_concrete,deepslate_bricks,cracked_deepslate_bricks,deepslate_tiles,stone,smooth_stone", LV, "-", ("32", "33"))
    # The houses: purple brick, deep plum tiled roofs, dark timber, pale stone feet, dark planters and sills, their
    # windows lit warm (the town's at night).
    add("swap", "purple_terracotta", LV, "block/purple_terracotta=lav_brick")
    add("roof_lav", "crimson_stairs", LV, "-", ("35", "42"))
    add("swap", "crimson_slab,crimson_planks", LV, "block/crimson_planks=roof_lav_tiles", ("35", "42"))
    for b in LAV_HOUSES + LAV_SHOPS + [LAV_TOWER]:
        add("swap", "stripped_dark_oak_wood,stripped_dark_oak_log,stripped_jungle_wood,stripped_jungle_log,stripped_spruce_wood,stripped_spruce_log,dark_oak_slab", b,
            "block/stripped_dark_oak_log=lav_timber,block/stripped_dark_oak_log_top=lav_timber,block/stripped_jungle_log=lav_timber,"
            "block/stripped_jungle_log_top=lav_timber,block/stripped_spruce_log=lav_timber,block/stripped_spruce_log_top=lav_timber,block/dark_oak_planks=lav_timber",
            ("33", "45"))
    add("swap", "white_concrete,light_gray_concrete", LV, "block/white_concrete=lav_base,block/light_gray_concrete=lav_base_dark", ("33", "33"))
    add("swap", "dark_oak_shelf,pale_oak_shelf", LV, "block/dark_oak_shelf=lav_planter,block/pale_oak_shelf=lav_planter")
    add("swap", "white_carpet", LV, "block/white_wool=lav_sill")
    add("swap", "dirt,coarse_dirt", LV, "block/dirt=planter_soil,block/coarse_dirt=planter_soil", ("33", "34"))
    add("swap", "glowstone", LV, "block/glowstone=lav_glow!")
    for h in LAV_HOUSES:
        add("glass", "light_blue_stained_glass,light_blue_stained_glass_pane", h, "block/light_blue_stained_glass=glass_lamp")
    # The Center and the Mart: their white walls clean panels, the grey pillars smooth stone.
    for b in LAV_SHOPS:
        add("swap", "white_concrete,light_gray_concrete", b, "block/white_concrete=shop_wall,block/light_gray_concrete=pillar_cer", ("34", "41"))
    # Its lamps, its own: plum metal posts, ornate violet chiselled feet and heads, a lilac cap, a violet globe and glass.
    for x, z in LAV_LAMPS:
        b = (x - 2, z - 2, x + 2, z + 2, "a Lavender lamp")
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet,sea_lantern", b,
            "block/tuff=lamp_lav_post,block/polished_tuff=lamp_lav_post,block/chiseled_tuff_bricks=lamp_lav_chisel,"
            "block/chiseled_tuff_bricks_top=lamp_lav_chisel_top,block/gray_wool=lamp_lav_top,block/sea_lantern=lamp_globe_violet!", ("32", "42"))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lav_lamp", ("32", "42"))
    # The POKeMON TOWER in solid colours: deep lavender stone walls, a pale lilac band, dark plum ledges and core,
    # slate columns, a deep purple metal roof, a silver spire, violet glass; its forecourt pale flagstones.
    T = LAV_TOWER
    add("swap", "blue_terracotta,light_blue_terracotta", T, "block/blue_terracotta=tower_wall,block/light_blue_terracotta=tower_trim", ("34", "72"))
    add("swap", "nether_brick_slab,nether_bricks,nether_brick_stairs", T, "block/nether_bricks=tower_ledge", ("34", "72"))
    add("swap", "deepslate_brick_wall", T, "block/deepslate_bricks=tower_column", ("34", "72"))
    add("swap", "iron_block", T, "block/iron_block=tower_roof", ("55", "72"))
    add("swap", "stone", T, "block/stone=tower_ledge", ("34", "72"))
    add("swap", "tuff_bricks,tuff_brick_stairs,tuff_brick_slab", T, "block/tuff_bricks=tower_plinth", ("32", "36"))
    add("swap", "polished_diorite,diorite_wall,diorite", T, "block/polished_diorite=tower_spire,block/diorite=tower_spire", ("58", "75"))
    add("glass", "light_blue_stained_glass,light_blue_stained_glass_pane", T, "block/light_blue_stained_glass=glass_tower")
    add("swap", "smooth_stone,smooth_stone_slab,stone_bricks,cracked_stone_bricks", T,
        "block/smooth_stone=lav_court@2,block/smooth_stone_slab_side=lav_kerb,block/stone_bricks=lav_court@2,block/cracked_stone_bricks=lav_court@2", ("32", "34"))
    add("swap", "stone_brick_wall", T, "block/stone_bricks=lav_wall", ("33", "36"))



# ---------- 0.50.0 (20:07, 20:12, 20:20) ----------
LEAVES_ALL = "oak_leaves,acacia_leaves,birch_leaves,azalea_leaves,flowering_azalea_leaves,dark_oak_leaves,jungle_leaves,mangrove_leaves,cherry_leaves,spruce_leaves"
HEDGE_LG = ",".join("block/%s=hedge_lg" % b for b in LEAVES_ALL.split(","))
HEDGE_LGF = ",".join("block/%s=hedge_lgf" % b for b in LEAVES_ALL.split(","))
ROUTE11 = (-299, 556, -142, 612, "Route 11")
ROUTE12 = (-142, 448, -100, 720, "Route 12")
ROUTE13 = (-300, 688, -130, 728, "Route 13")
ROUTE14 = (-305, 729, -250, 805, "Route 14")
ROUTE15 = (-440, 758, -306, 805, "Route 15")
ROUTE16 = (-680, 388, -560, 444, "Route 16")
ROUTE17 = (-665, 445, -615, 757, "Route 17")
ROUTE18 = (-665, 758, -561, 805, "Route 18")
SOUTH_ROUTES = [ROUTE11, ROUTE12, ROUTE13, ROUTE14, ROUTE15, ROUTE16, ROUTE17, ROUTE18]
BENCHES = [(-261, 758), (-621, 483), (-638, 544), (-638, 642), (-658, 753)]
R17_LAMPS = [(x, z) for z in range(468, 739, 30) for x in (-658, -621)] + [(-558, 429)]
ROUTE_SIGNS = [(-351, -350, 789, 30, 31), (-117, -116, 473, 31, 32), (-341, -340, 560, 31, 32), (-123, -122, 585, 31, 32), (-265, -264, 711, 31, 32),
               (-209, -208, 715, 31, 32), (-630, -629, 496, 33, 34), (-633, -632, 547, 33, 33), (-633, -632, 639, 33, 33), (-650, -649, 690, 33, 34)]
# The Day Care's build (Route 5), which John also put on Routes 12 and 16: its box and its floor's height. All three
# take the Day Care's look (07:38: "copy that over").
CARE_HOUSES = [(-361, 317, -342, 337, 37, "the Day Care"), (-648, 408, -635, 419, 33, "Route 16's house"), (-133, 611, -120, 622, 31, "Route 12's house")]
SS_ANNE = (-450, 600, -230, 670, "the S.S. ANNE")
SEA_ROCK = ",".join("block/%s=sea_rock_top|sea_rock@2" % b for b in ("tuff", "andesite", "cobblestone", "diorite", "granite", "polished_andesite", "mossy_cobblestone", "stone"))
SEA_ROCK_BLOCKS = ("tuff,tuff_slab,tuff_stairs,tuff_wall,andesite,andesite_slab,andesite_stairs,andesite_wall,cobblestone,cobblestone_slab,cobblestone_stairs,"
                   "cobblestone_wall,diorite,diorite_slab,diorite_stairs,diorite_wall,granite,polished_andesite,mossy_cobblestone,stone")


def round50(add, room):
    """0.50.0's looks (20:07 to 20:20): see tools/looks50.py for the pictures."""
    import json as _json
    K_ = KANTO
    # Trees leafy and rounded (Shards' extra leaves on the leaves that show), in the world and in the forest and the
    # SAFARI ZONE (their own rooms).
    add("leafy", LEAVES_ALL, K_)
    room("leafy", LEAVES_ALL, (4095, -1, 4500, 400, "Viridian Forest"))
    room("leafy", LEAVES_ALL, (6143, -1, 6144 + 352, 214, "the SAFARI ZONE"))
    # Every hedge and bush on the map (found: leaves standing on the ground, 1 to 4 high, nothing leafy over them), as
    # the little trees are (07:38: John liked theirs): the azalea's leaves with Shards' extra leaves standing out of
    # them, one kind for all a hedge's leaves (no checker), the flowering azalea's where it has flowers.
    for x0, z0, x1, z1, y0, y1, fl in _json.load(open(os.path.join(HERE, "area_hedges50.json"))):
        add("leafy_hedge", LEAVES_ALL, (x0, z0, x1, z1, "a hedge"), "flowering_azalea" if fl else "azalea", (str(y0), str(y1)))
    # The benches under their flowering arches (Routes 14 and 17): the arch all hedge, its frame a white pergola, the
    # swing's seat yellow slats, its bars dark metal, its lanterns garden lights.
    for x, z in BENCHES:
        b = (x - 4, z - 4, x + 4, z + 4, "a bench under its arch")
        add("leafy_hedge", LEAVES_ALL, b, "flowering_azalea", ("32", "40"))
        add("swap", "oak_log", b, "block/oak_log=pergola,block/oak_log_top=pergola_top", ("32", "40"))
        add("swap", "spruce_trapdoor", b, "block/spruce_trapdoor=bench_slat", ("32", "40"))
        add("swap", "spruce_fence,spruce_fence_gate", b, "block/spruce_planks=bench_frame", ("32", "40"))
        add("swap", "waxed_copper_lantern,copper_lantern", b, "block/copper_lantern=garden_light", ("32", "40"))
    # The cliffs' brown mushroom tops, all over the map: rough flagstones of the ledges' earth (laid over four blocks),
    # their sides the cliffs' rock.
    add("swap", "brown_mushroom_block", K_, "block/brown_mushroom_block=hill_top@4|drip_rock@4,block/mushroom_block_inside=hill_top@4|drip_rock@4")
    # The rocks in the water (the blockers): wet grey rock, only those standing in the water (found on the map).
    for x0, z0, x1, z1, y0, y1 in _json.load(open(os.path.join(HERE, "area_rocks50.json"))):
        add("swap", SEA_ROCK_BLOCKS, (x0, z0, x1, z1, "a rock in the water"), SEA_ROCK, (str(y0), str(y1)))
    # Routes 11 to 18: the trees' trunks bark, the white fences, the signs as HeartGold's.
    for r in SOUTH_ROUTES:
        add("swap", "oak_wood,oak_log", r, "block/oak_log=tree_bark,block/oak_log_top=tree_bark")
        add("swap", "pale_oak_fence", r, FENCE)
    for x0, x1, z, y0, y1 in ROUTE_SIGNS:
        add("sign", "pale_oak_shelf,pale_oak_sign", (x0 - 1, z - 1, x1 + 1, z + 1, "a route sign"), "-", (str(y0), str(y1)))
    # Route 17 (the CYCLING ROAD), the full treatment: its lamps in orange metal (their glass warm frosted, their lights
    # lit globes), its tarmac dark with a scuff or two (Routes 16 and 18's too).
    for x, z in R17_LAMPS:
        b = (x - 3, z - 2, x + 2, z + 2, "a Cycling Road lamp")
        add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern", b,
            "block/red_sandstone=lamp_r17_post,block/chiseled_red_sandstone=lamp_r17_chisel,block/red_sandstone_top=lamp_r17_cap,"
            "block/orange_wool=lamp_r17_cap,block/sea_lantern=lamp_globe_warm!", ("32", "42"))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ("32", "42"))
    for r in (ROUTE16, ROUTE17, ROUTE18):
        add("swap", "gray_concrete", r, "block/gray_concrete=tarmac@4")
    round51(add, room)
    round52(add, room)
    round52b(add, room)
    round52c(add, room)
    round52d(add, room)
    round53(add, room)
    round54(add, room)
    round56(add, room)
    # (0.57) John's review, 15:17 to 16:35.
    import arealooks57
    arealooks57.round57(add, room, globals())
    import arealooks58
    arealooks58.round58(add, room, globals())
    import arealooks59
    arealooks59.round59(add, room, globals())
    import arealooks59b
    arealooks59b.round59b(add, room, globals())
    import arealooks59c
    arealooks59c.round59c(add, room, globals())
    # (0.59.4) John's Kanto fly-round, 07:31 and 07:36.
    import arealooks594
    arealooks594.round594(add, room, globals())
    # (0.59.6) John's 13:17 round: every cut-through, Kanto's cliffs, the Bike Shop's back posts.
    import arealooks596
    arealooks596.round596(add, room, globals())
    # Route 12's piers as Let's Go's: yellow-tan boards in darker frames, steel bolts.
    add("swap", "oak_stairs,oak_planks,oak_slab,oak_fence,spruce_shelf,stripped_oak_wood,stripped_oak_log,stone_button", ROUTE12,
        "block/oak_planks=pier_boards,block/spruce_shelf=pier_frame,block/stripped_oak_log=pier_frame,block/stripped_oak_log_top=pier_frame,block/stone=pier_bolt")
    # Route 13's end of the same pier (07:38: "the bit where you enter Route 13").
    add("swap", "oak_stairs,oak_planks,oak_slab,oak_fence,spruce_shelf,stripped_oak_wood,stripped_oak_log,stone_button", (-160, 708, -143, 715, "Route 13's end of the pier"),
        "block/oak_planks=pier_boards,block/spruce_shelf=pier_frame,block/stripped_oak_log=pier_frame,block/stripped_oak_log_top=pier_frame,block/stone=pier_bolt")
    # The Day Care and Routes 12 and 16's house (the same build, 07:38): HeartGold's red tiles on the mangrove roof,
    # the brick foot as Cerulean's, the corner posts Cerulean's pale stone pillars (no dark timber), stone steps, the
    # chimney's stone.
    for x0, z0, x1, z1, f, note in CARE_HOUSES:
        h = (x0, z0, x1, z1, note)
        add("roof", "mangrove_stairs", h, "-", (str(f + 2), str(f + 8)))
        add("swap", "mangrove_slab,mangrove_planks", h, "block/mangrove_planks=roof_tiles", (str(f + 1), str(f + 8)))
        add("swap", "bricks", h, "block/bricks=base_cer", (str(f - 1), str(f + 1)))
        add("swap", "stripped_spruce_wood,stripped_spruce_log", h, "block/stripped_spruce_log=pillar_cer,block/stripped_spruce_log_top=pillar_cer", (str(f), str(f + 3)))
        add("swap", "polished_andesite_slab,polished_andesite_stairs,polished_andesite,stone_bricks", h, "block/polished_andesite=step_cer,block/stone_bricks=step_cer", (str(f), str(f)))
        add("swap", "prismarine_brick_slab,prismarine_brick_stairs", h, "block/prismarine_bricks=chimney_stone", (str(f + 2), str(f + 10)))
    # The Day Care's ledge: the timber set in its sides is the cliff's rock (07:38: "stripped oak beams on the sides").
    add("swap", "stripped_spruce_wood,stripped_jungle_wood", (-362, 316, -341, 339, "the Day Care's ledge"),
        "block/stripped_spruce_log=drip_rock@4,block/stripped_jungle_log=drip_rock@4", ("33", "36"))
    # The Power Plant (20:20): the gold band, the roof, the red frames, the iron in solid colours; its grey stair
    # pillars steel, its grates, its doors.
    P = POWER_PLANT
    add("swap", "polished_tuff_stairs,polished_tuff", P, "block/polished_tuff=pp_steel")
    add("swap", "dark_oak_trapdoor", P, "block/dark_oak_trapdoor=pp_grate")
    add("swap", "black_concrete", P, "block/black_concrete=pp_dark", ("35", "39"))
    # Vermilion City (20:20): the doors as Let's Go's (green, the Fan Club's red), the steps up to them, their stone
    # frames cream; the Fan Club's bow (pink, a POKe BALL in its middle) and its arch board.
    VE = VERMILION
    add("swap", "warped_door", VE, "block/warped_door_top=door_green_top,block/warped_door_bottom=door_green_bottom")
    add("swap", "mangrove_door", VE, "block/mangrove_door_top=door_red_top,block/mangrove_door_bottom=door_red_bottom")
    add("swap", "smooth_stone_slab", VE, "block/smooth_stone_slab_side=step_ver,block/smooth_stone=step_ver", ("31", "31"))
    add("swap", "light_gray_concrete", VE, "block/light_gray_concrete=trim_ver", ("31", "33"))
    add("swap", "birch_shelf", (-378, 578, -371, 580, "the Fan Club's arch"), "block/birch_shelf=fan_arch", ("35", "40"))
    add("swap", "cherry_shelf", (-378, 579, -371, 581, "the Fan Club's bow"), "block/cherry_shelf=fan_bow", ("34", "38"))
    add("swap", "cherry_shelf", (-375, 580, -374, 580, "the Fan Club's bow's ball"), "block/cherry_shelf=fan_bow_ball", ("36", "36"))
    # The S.S. ANNE: the stone stand under it a smooth slate plinth, the legs under its gangway navy steel, the
    # gangway's towers white and navy, its doorway dark.
    add("swap", "stone_brick_slab,stone_brick_stairs,stone_bricks,polished_andesite,polished_andesite_stairs,andesite_wall,andesite", SS_ANNE,
        "block/stone_bricks=ss_plinth,block/polished_andesite=ss_plinth,block/andesite=ss_plinth", ("20", "37"))
    add("swap", "andesite", (-410, 605, -335, 645, "the S.S. ANNE's legs"), "block/andesite=ss_leg", ("20", "30"))
    gang = (-364, 612, -341, 617, "the S.S. ANNE's gangway")
    add("swap", "resin_bricks,resin_brick_stairs,polished_andesite_stairs,birch_planks", gang,
        "block/resin_bricks=ss_tower_dark,block/polished_andesite=ss_tower,block/birch_planks=ss_tower", ("24", "36"))
    add("swap", "black_concrete", (-360, 617, -347, 617, "the S.S. ANNE's doorway"), "block/black_concrete=cave_dark", ("29", "36"))
    # Bill's Sea Cottage (20:12): its stripped oak and oak timbers too, "nothing left unturned".
    add("swap", "stripped_oak_log,stripped_oak_wood,oak_wood,oak_log", (-226, 141, -209, 158, "Bill's Sea Cottage's timbers"),
        "block/stripped_oak_log=bill_timber,block/stripped_oak_log_top=bill_timber,block/oak_log=bill_timber,block/oak_log_top=bill_timber", ("33", "41"))
    # The Day Care's front steps (20:12) as Cerulean's.
    add("swap", "polished_andesite_slab,stone_bricks", (-361, 317, -342, 340, "the Day Care's steps"), "block/polished_andesite=step_cer,block/stone_bricks=step_cer", ("37", "37"))
    # Nugget Bridge's south end (its steps and the crossing), gold as the bridge.
    add("swap", "andesite_slab,stripped_oak_wood,oak_shelf,pale_oak_fence,light_gray_concrete", (-360, 232, -346, 242, "Nugget Bridge's south end"),
        "block/andesite=step_cer,block/stripped_oak_log=bridge_gold_beam,block/oak_shelf=bridge_gold_rail,block/pale_oak_planks=white_fence,block/light_gray_concrete=pillar_cer", ("33", "36"))
    # Diglett's Cave's two mouths (20:20: "redo the doors"): a hole in the cliff, its frame the cliff's rock, dark within.
    for x0, z0, x1, z1, y0, y1 in ((-781, 365, -778, 369, 30, 34), (-292, 579, -289, 583, 31, 35)):
        add("swap", "stone_bricks,stone_brick_slab,stone_brick_stairs,stone_brick_wall,black_concrete", (x0, z0, x1, z1, "Diglett's Cave's mouth"),
            "block/stone_bricks=tunnel_stone,block/black_concrete=cave_dark", (str(y0 - 1), str(y1 + 1)))
    # The bikes outside the Bike Shop (20:12; 12:19: "stacking a couple at the side"): three side by side on the pavement
    # by the shop's west side, by its sign.
    # ((0.59, 18:19: "the bikes in the road ... move them to the right 3 blocks") On the paving three blocks east.)
    # ((0.59.6, 13:17: "the bikes ... aren't there") The paving there is smooth stone SLABS, which the rows didn't name:
    # the bikes were never drawn. On the slabs, half a block lower.)
    for z in (278, 279, 280):
        add("prop", "light_blue_terracotta,white_concrete,light_blue_concrete,white_terracotta,smooth_stone", (-378, z, -378, z, "a bike by the Bike Shop"), "bike", ("33", "33"))
        add("prop", "smooth_stone_slab", (-378, z, -378, z, "a bike by the Bike Shop"), "bike_low", ("33", "33"))
    # Lavender's sign (20:12) as every other town's.
    add("sign", "pale_oak_shelf", (-126, 425, -123, 427, "Lavender Town's sign"), "-", ("33", "35"))
    # The SAFARI ZONE (its room): its warped doors plain wood, its fences as the routes'.
    SZ = (6143, -1, 6144 + 352, 214, "the SAFARI ZONE")
    room("swap", "warped_door", SZ, "block/warped_door_top=door_wood_top,block/warped_door_bottom=door_wood_bottom")
    room("swap", "spruce_log,spruce_fence", SZ, FENCE_WOOD)

LAMP46 = ("block/tuff=lamp_post,block/polished_tuff=lamp_post,block/chiseled_tuff_bricks=lamp_chisel,"
          "block/chiseled_tuff_bricks_top=lamp_chisel_top,block/gray_wool=lamp_cap")
QUARTZ = ("block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean,"
          "block/quartz_pillar=quartz_clean_pillar,block/quartz_pillar_top=quartz_clean,block/chiseled_quartz_block=quartz_clean,"
          "block/chiseled_quartz_block_top=quartz_clean,block/quartz_bricks=quartz_clean")



def round51(add, room):
    """0.51.0's looks (09:01 to 09:30): see tools/looks51.py for the pictures."""
    K_ = KANTO
    # The big trees' trunks bark everywhere (09:11: Route 14's "across the board"), in the forest and the Safari Zone too.
    add("swap", "oak_wood,oak_log", K_, "block/oak_log=tree_bark,block/oak_log_top=tree_bark")
    room("swap", "oak_wood,oak_log", (4095, -1, 4500, 400, "Viridian Forest"), "block/oak_log=tree_bark,block/oak_log_top=tree_bark")
    room("swap", "oak_wood,oak_log", (6143, -1, 6144 + 352, 214, "the SAFARI ZONE"), "block/oak_log=tree_bark,block/oak_log_top=tree_bark")
    # Route 15's lamps as the Cycling Road's (the same lamp: orange metal, warm frosted glass, lit globes).
    add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern", ROUTE15,
        "block/red_sandstone=lamp_r17_post,block/chiseled_red_sandstone=lamp_r17_chisel,block/red_sandstone_top=lamp_r17_cap,"
        "block/orange_wool=lamp_r17_cap,block/sea_lantern=lamp_globe_warm!", ("30", "42"))
    add("glass", "black_stained_glass_pane", ROUTE15, "block/black_stained_glass=glass_lamp", ("30", "42"))
    # Pewter's yellow doors (John's bamboo doors), a style of their own.
    add("swap", "bamboo_door", PEWTER, "block/bamboo_door_top=door_yellow_top,block/bamboo_door_bottom=door_yellow_bottom")
    # Lily pads, Let's Go's, everywhere.
    add("swap", "lily_pad", K_, "block/lily_pad=lily_lg!")

SAF_GROUND = (-405, 384, -298, 466, "Saffron City's streets")
SAFFRON = (-395, 389, -305, 459, "Saffron City")
SILPH = (-362, 417, -341, 432, "SILPH CO.")
DOJO = (-343, 388, -330, 400, "the FIGHTING DOJO")
SAF_GYM = (-329, 388, -313, 400, "Saffron Gym")
SAF_LAMPS = [(-357, 399), (-346, 399), (-314, 417), (-385, 421), (-385, 428), (-314, 432), (-353, 450), (-342, 450), (-318, 450)]


def round52(add, room):
    """0.52.0's looks: Saffron City in Let's Go's colours (09:13, 09:26; tools/looks52.py for the pictures)."""
    G, S = SAF_GROUND, SAFFRON
    g32 = ("32", "32")
    # The streets: the roads tarmac (as Route 17's), the kerbs pale stone, John's four-colour paving as pavers, the
    # plaza strip in front of the south building cream slabs.
    add("swap", "gray_concrete", G, "block/gray_concrete=tarmac@4", g32)
    add("swap", "light_gray_concrete", G, "block/light_gray_concrete=saf_kerb", g32)
    add("swap", "white_concrete,yellow_concrete,yellow_terracotta,orange_terracotta", G,
        "block/white_concrete=pave_saf_w@2,block/yellow_concrete=pave_saf_y@2,block/yellow_terracotta=pave_saf_o2@2,block/orange_terracotta=pave_saf_o@2", g32)
    add("swap", "white_terracotta", G, "block/white_terracotta=saf_plaza", g32)
    up = ("33", "50")
    # The buildings: yellow walls, white walls, cream bands, the stone and cream buildings, the pale grey ones.
    add("swap", "yellow_concrete", S, "block/yellow_concrete=saf_wall", up)
    add("swap", "white_concrete", S, "block/white_concrete=saf_white", up)
    add("swap", "yellow_terracotta", S, "block/yellow_terracotta=saf_band", up)
    add("swap", "end_stone_bricks,end_stone_brick_stairs", S, "block/end_stone_bricks=saf_band", up)
    add("swap", "light_gray_terracotta", S, "block/light_gray_terracotta=saf_stone", up)
    add("swap", "white_terracotta", S, "block/white_terracotta=saf_cream", ("33", "42"))
    add("swap", "light_gray_concrete", S, "block/light_gray_concrete=saf_grey", ("33", "46"))
    # The roofs green (Let's Go's), a cream coping round their edges, their terraces lighter; the floors inside tiled.
    add("swap", "cut_red_sandstone_slab", S, "block/red_sandstone_top=saf_roof,block/cut_red_sandstone=saf_rim", up)
    add("swap", "light_blue_terracotta", S, "block/light_blue_terracotta=saf_deck", ("42", "50"))
    add("swap", "light_blue_terracotta", S, "block/light_blue_terracotta=saf_floor", ("33", "41"))
    # On the roofs: the units' metal casings, the vent grilles, the pale pads.
    add("swap", "iron_block", S, "block/iron_block=saf_unit", up)
    add("swap", "smooth_sandstone_slab", S, "block/sandstone_top=saf_vent", up)
    add("swap", "pale_oak_slab", S, "block/pale_oak_planks=saf_pad", ("43", "50"))
    add("swap", "white_terracotta", S, "block/white_terracotta=saf_pad", ("43", "50"))
    # The windows' dark backs pale blinds; the awnings striped; the canopies teal; the door columns white; the steps.
    add("swap", "deepslate_brick_wall", S, "block/deepslate_bricks=saf_blind", up)
    add("swap", "prismarine_brick_stairs,prismarine_bricks", S, "block/prismarine_bricks=saf_canopy", up)
    add("swap", "crimson_stairs", S, "block/crimson_planks=saf_awning_red", up)
    add("swap", "warped_stairs", S, "block/warped_planks=saf_awning_green", up)
    add("swap", "bone_block", S, "block/bone_block_side=saf_post,block/bone_block_top=saf_post_top", up)
    add("swap", "smooth_stone_slab", S, "block/smooth_stone=step_cer,block/smooth_stone_slab_side=step_cer", ("33", "33"))
    # The planters (John's shelves round the beds) each a colour of their own; the boards over the doors; the rails.
    add("swap", "pale_oak_shelf", S, "block/pale_oak_shelf=planter_saf_cream", ("33", "33"))
    add("swap", "pale_oak_shelf", S, "block/pale_oak_shelf=saf_board", ("35", "40"))
    add("swap", "mangrove_shelf", S, "block/mangrove_shelf=planter_saf_terra", ("33", "33"))
    add("swap", "oak_shelf", S, "block/oak_shelf=planter_saf_wood", ("33", "33"))
    add("swap", "nether_brick_fence", S, "block/nether_bricks=saf_rail", ("33", "34"))
    # The street lamps (not black any more): warm white posts, green caps, warm frosted glass, lit globes.
    for x, z in SAF_LAMPS:
        b = (x - 1, z - 1, x + 1, z + 1, "a Saffron lamp")
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet,sea_lantern", b,
            "block/tuff=lamp_saf_post,block/polished_tuff=lamp_saf_post,block/chiseled_tuff_bricks=lamp_saf_chisel,"
            "block/chiseled_tuff_bricks_top=lamp_saf_chisel_top,block/gray_wool=lamp_saf_cap_top,block/sea_lantern=lamp_globe_warm!", ("33", "42"))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ("33", "42"))
    # SILPH CO.: white cladding panels, metal, blue-grey blinds behind its glass, pale bands, its plaza.
    add("swap", "white_concrete", SILPH, "block/white_concrete=silph_panel@2", ("33", "70"))
    add("swap", "quartz_block,quartz_stairs,quartz_slab,quartz_pillar,smooth_quartz_slab", SILPH, QUARTZ, ("33", "70"))
    add("swap", "smooth_quartz", SILPH, "block/quartz_block_bottom=silph_plaza@2", ("33", "33"))
    add("swap", "deepslate_brick_wall", SILPH, "block/deepslate_bricks=silph_blind", ("33", "70"))
    add("swap", "iron_block", SILPH, "block/iron_block=silph_metal", ("33", "70"))
    add("swap", "yellow_terracotta", SILPH, "block/yellow_terracotta=silph_band", ("33", "70"))
    add("swap", "light_blue_terracotta", SILPH, "block/light_blue_terracotta=silph_floor", ("33", "70"))
    # The FIGHTING DOJO (the shaped roof, left of the Gym): slate tiles, a dark ridge, dark timber, white plaster
    # between, wooden floors and posts, its sliding door.
    D = DOJO
    add("roof_dojo", "jungle_stairs,oak_stairs", D, "-", ("37", "44"))
    add("swap", "jungle_stairs", D, "block/jungle_planks=dojo_ridge", ("37", "44"))
    add("swap", "jungle_planks,oak_slab,jungle_slab", D, "block/jungle_planks=dojo_ridge,block/oak_planks=dojo_ridge", ("37", "44"))
    add("swap", "oak_planks", D, "block/oak_planks=dojo_beam", ("37", "44"))
    add("swap", "light_gray_concrete", D, "block/light_gray_concrete=dojo_wall@2", ("33", "42"))
    add("swap", "quartz_block,quartz_slab", D, "block/quartz_block_top=dojo_floor,block/quartz_block_side=dojo_beam,block/quartz_block_bottom=dojo_floor", ("33", "33"))
    add("swap", "quartz_block", D, "block/quartz_block_top=dojo_post,block/quartz_block_side=dojo_post,block/quartz_block_bottom=dojo_post", ("34", "38"))
    add("swap", "quartz_block", D, "block/quartz_block_top=dojo_beam,block/quartz_block_side=dojo_wall@2,block/quartz_block_bottom=dojo_beam", ("39", "44"))
    add("swap", "quartz_stairs", D, "block/quartz_block_top=dojo_beam,block/quartz_block_side=dojo_beam", ("33", "44"))
    add("swap", "calcite", D, "block/calcite=dojo_floor", ("33", "33"))
    add("swap", "polished_diorite_stairs,polished_diorite_slab", D, "block/polished_diorite=dojo_beam")
    add("swap", "deepslate_tiles", D, "block/deepslate_tiles=dojo_door", ("33", "37"))
    # The Saffron Gym's doorway (its dark tiles): one solid dark, as the gates'.
    add("swap", "deepslate_tiles", SAF_GYM, "block/deepslate_tiles=gate_dark", ("33", "37"))

ROUTE28 = (-1210, 505, -1011, 590, "Route 28")
LAV_STEPS = [(-141, 427, -138, 430, "the Volunteer House's step"), (-148, 437, -145, 440, "Lavender's house's step"),
             (-137, 437, -134, 440, "Lavender's house's step"), (-120, 415, -117, 420, "Lavender's Center's step")]
FU_LAMPS = [(-490, 768), (-539, 777), (-464, 781), (-542, 788), (-512, 788), (-454, 793), (-483, 811), (-502, 819)]


def round52b(add, room):
    """0.52.0's other looks (09:01 to 09:11, now the map's back): see tools/looks52.py's others()."""
    # Route 28 and Silver Cave's outside (09:01): all Kanto's looks too (main() copies them; west of the Johto Gate
    # they stopped short).
    add("swap", "oak_wood,oak_log", ROUTE28, "block/oak_log=tree_bark,block/oak_log_top=tree_bark")
    add("swap", "pale_oak_fence", ROUTE28, FENCE)
    # Its one house (Viridian's green house, the same build): the green tiled roof, white frames, the chimney, the
    # shelves, the steps and trim.
    h = (-1115, 531, -1100, 544, "Route 28's house")
    add("roof_green", "warped_stairs", h)
    add("swap", "pale_oak_stairs,pale_oak_slab", h, "block/pale_oak_planks=window_frame")
    add("swap", "mud_bricks,mud_brick_stairs,mud_brick_slab", h, "block/mud_bricks=chimney_brick")
    add("swap", "warped_shelf", h, "block/warped_shelf=green_shelf")
    add("swap", "stripped_warped_hyphae,stripped_warped_stem", h, "block/stripped_warped_stem=green_solid,block/stripped_warped_stem_top=green_solid_top")
    add("swap", "birch_shelf", h, "block/birch_shelf=wood_shelf")
    add("swap", "polished_andesite_slab,polished_andesite_stairs,polished_andesite", h, STEPS)
    add("swap", "polished_diorite_stairs,polished_diorite_slab", h, TRIM)
    add("swap", "warped_door", h, "block/warped_door_top=door_wood_top,block/warped_door_bottom=door_wood_bottom")
    # SILVER CAVE's mouth: a hole in the cliff, framed in its rock, dark within.
    add("swap", "stone_bricks,smooth_stone_slab,black_concrete", (-1176, 525, -1167, 535, "SILVER CAVE's mouth"),
        "block/stone_bricks=drip_rock@4,block/smooth_stone=drip_rock_top|drip_rock@4,block/smooth_stone_slab_side=drip_rock@4,block/black_concrete=cave_dark", ("25", "33"))
    # Lavender's houses' steps (09:01: "still slabs"): stone steps, a lit nosing, in Lavender's lilac grey.
    for b in LAV_STEPS:
        add("swap", "smooth_stone_slab", b, "block/smooth_stone=lav_step,block/smooth_stone_slab_side=lav_step_side", ("33", "35"))
    # The POKeMON LEAGUE gate (09:01): its wooden walls (acacia) cream panels, the polished diorite at its doors
    # stone steps, the paving out front as Pewter's.
    lg = (-930, 505, -905, 542, "the POKeMON LEAGUE gate")
    add("swap", "acacia_planks", lg, "block/acacia_planks=league_wall", ("24", "36"))
    add("swap", "polished_diorite_slab,polished_diorite", lg, "block/polished_diorite=step_cer", ("24", "28"))
    add("swap", "white_terracotta,smooth_quartz,chiseled_quartz_block", lg,
        "block/white_terracotta=pave_terracotta,block/quartz_block_bottom=pave_quartz,block/chiseled_quartz_block_top=pave_chiseled,block/chiseled_quartz_block=pave_chiseled_side", ("25", "25"))
    # The Museum's way in (09:05): John's spruce shelves in its porch a walnut and glass doorway, the steps upside
    # down over it its trim.
    me = (-805, 274, -800, 279, "the Museum's way in")
    add("swap", "spruce_shelf", me, "block/spruce_shelf=museum_entry", ("30", "34"))
    add("swap", "spruce_stairs", me, "block/spruce_planks=museum_trim", ("33", "35"))
    # Route 13's sign (09:11): the wooden sign, read as FireRed's.
    add("wood_sign", "acacia_sign", (-177, 698, -175, 700, "Route 13's sign"))
    # The benches' chains (09:11): the bench's yellow (the hanging boards are too, in the look pack).
    for x, z in BENCHES:
        add("swap", "iron_chain", (x - 4, z - 4, x + 4, z + 4, "a bench's chains"), "block/iron_chain=bench_chain", ("32", "40"))
    # Fuchsia's lamps (09:11: one's glass didn't match the next's): all the same warm frosted glass and lit globes.
    for x, z in FU_LAMPS:
        b = (x - 1, z - 1, x + 1, z + 1, "a Fuchsia lamp")
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ("33", "40"))
        add("swap", "sea_lantern", b, "block/sea_lantern=lamp_globe_warm!", ("33", "40"))
    # Route 2's two CUT trees (09:05): as the little trees John likes, a trunk and a round green crown, so they read
    # as Let's Go's trees to CUT.
    for x0, z0, x1, z1 in ((-786, 467, -780, 471), (-787, 482, -781, 487)):
        b = (x0, z0, x1, z1, "a tree to CUT")
        add("boulder", "dark_oak_fence", b, "prefix:ttrunk", ("25", "29"))
        add("leafy_hedge", "cherry_leaves", b, "azalea", ("25", "31"))

CELADON = (-565, 385, -452, 462, "Celadon City")
CEL_LAMPS = [(-524, 416), (-483, 417), (-558, 429), (-524, 432), (-483, 432), (-465, 432), (-519, 438), (-508, 438), (-508, 446)]


def round52c(add, room):
    """0.52.0's Celadon City (09:30), as Let's Go's: see tools/looks52.py's celadon()."""
    C = CELADON
    g = ("31", "32")
    # The streets: the roads tarmac, the pavements warm basket-weave, the kerbs white; the fountain's rim its greens,
    # its basin blue tiles.
    add("swap", "cyan_terracotta", C, "block/cyan_terracotta=tarmac@4", g)
    add("swap", "mud_bricks", C, "block/mud_bricks=pave_cel@2", g)
    add("swap", "polished_diorite", C, "block/polished_diorite=cel_kerb", g)
    add("swap", "lime_terracotta,green_glazed_terracotta", C, "block/lime_terracotta=cel_lime,block/green_glazed_terracotta=cel_tile_band", ("32", "32"))
    add("swap", "cobblestone", (-545, 421, -537, 429, "the fountain's basin"), "block/cobblestone=pool_floor", ("32", "32"))
    # (The pond's rim: 0.53's round53 lays it in clean quartz; its blue and white tiles taken out at 12:19.)
    up = ("33", "60")
    # The buildings: soft green walls, lime bands, deep green columns, white walls, cream corners; the floors inside
    # tiled, the roof terraces green; the bands of tiles.
    add("swap", "green_terracotta", C, "block/green_terracotta=cel_green", up)
    add("swap", "lime_terracotta", C, "block/lime_terracotta=cel_lime", up)
    add("swap", "green_concrete", C, "block/green_concrete=cel_green_dark", up)
    add("swap", "lime_concrete", C, "block/lime_concrete=cel_lime_bright", up)
    add("swap", "white_concrete", C, "block/white_concrete=cel_white", up)
    add("swap", "yellow_terracotta", C, "block/yellow_terracotta=cel_cream", up)
    add("swap", "light_blue_terracotta", C, "block/light_blue_terracotta=cel_floor", ("33", "41"))
    add("swap", "light_blue_terracotta", C, "block/light_blue_terracotta=cel_deck", ("42", "60"))
    add("swap", "light_blue_glazed_terracotta", C, "block/light_blue_glazed_terracotta=cel_tile_band", up)
    # The roofs: green and teal, the small one by the pond slate; the GAME CORNER's violet with gold seams under a
    # marquee of bulbs.
    add("swap", "warped_slab,warped_stairs", C, "block/warped_planks=cel_roof", up)
    add("swap", "prismarine_brick_slab,prismarine_brick_stairs", C, "block/prismarine_bricks=cel_roof_teal", up)
    add("swap", "polished_blackstone_stairs", C, "block/polished_blackstone=cel_roof_dark", up)
    add("swap", "stripped_warped_hyphae", C, "block/stripped_warped_stem=cel_gc_roof", up)
    add("swap", "cyan_carpet", C, "block/cyan_wool=cel_gc_lights", up)
    # The DEPT. STORE: cream columns, blue window strips between, its canopy; stone plinths; quartz clean.
    add("swap", "acacia_planks,acacia_stairs,acacia_slab", C, "block/acacia_planks=cel_pilaster", up)
    add("swap", "bamboo_shelf", C, "block/bamboo_shelf=cel_store_glass", up)
    add("swap", "stone_bricks,bricks", C, "block/stone_bricks=cel_base,block/bricks=cel_base", ("33", "33"))
    add("swap", "smooth_quartz,smooth_quartz_slab,smooth_quartz_stairs,quartz_block,quartz_slab,quartz_stairs,quartz_pillar", C, QUARTZ, up)
    # The roofs' units, the windows' blinds, the door columns, the trim, the steps, the doorways.
    add("swap", "iron_block", C, "block/iron_block=saf_unit", up)
    add("swap", "deepslate_brick_wall", C, "block/deepslate_bricks=saf_blind", up)
    add("swap", "bone_block", C, "block/bone_block_side=saf_post,block/bone_block_top=saf_post_top", up)
    add("swap", "polished_diorite_stairs,polished_diorite_slab,polished_diorite", C, TRIM, up)
    add("swap", "smooth_stone_slab", C, "block/smooth_stone=step_cer,block/smooth_stone_slab_side=step_cer", ("33", "33"))
    add("swap", "deepslate_tiles", C, "block/deepslate_tiles=gate_dark", ("33", "37"))
    # The planters (shelves round the beds), the green shelves, the benches by the fountain.
    add("swap", "pale_oak_shelf", C, "block/pale_oak_shelf=planter_saf_cream", ("33", "33"))
    add("swap", "oak_shelf", C, "block/oak_shelf=planter_saf_wood", ("33", "33"))
    add("swap", "warped_shelf", C, "block/warped_shelf=green_shelf", up)
    add("swap", "spruce_trapdoor", C, "block/spruce_trapdoor=bench_slat", ("33", "34"))
    # Its lamps, its own (09:30: "something unique"): dark green posts, gold, warm frosted glass, and each globe a
    # colour of the rainbow (the City of Rainbow Dreams).
    import looks52
    for i, (x, z) in enumerate(CEL_LAMPS):
        b = (x - 1, z - 1, x + 1, z + 1, "a Celadon lamp")
        globe = looks52.RAINBOW[i % len(looks52.RAINBOW)][0]
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet,sea_lantern", b,
            "block/tuff=lamp_cel_post,block/polished_tuff=lamp_cel_post,block/chiseled_tuff_bricks=lamp_cel_chisel,"
            "block/chiseled_tuff_bricks_top=lamp_cel_chisel_top,block/gray_wool=lamp_cel_cap_top,block/sea_lantern=lamp_globe_%s!" % globe, ("33", "42"))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ("33", "42"))

ROUTE23 = (-962, 172, -865, 507, "Route 23")
PLATEAU = (-927, 148, -898, 240, "the INDIGO PLATEAU")
R23_STATUES = [(-931, 238), (-926, 238), (-899, 238), (-894, 238), (-930, 265), (-921, 265), (-930, 273), (-921, 273), (-930, 281), (-921, 281),
               (-931, 308), (-920, 308), (-914, 327), (-905, 327), (-898, 352), (-901, 363), (-924, 392), (-912, 430), (-914, 452), (-905, 452),
               (-920, 468), (-911, 468), (-926, 488), (-917, 488), (-912, 504), (-903, 504)]


def round52d(add, room):
    """0.52.0's Route 23 and INDIGO PLATEAU (10:41): see tools/looks52.py's victory()."""
    R = ROUTE23
    up = ("26", "45")
    # The walls as Let's Go's: warm brown brick, darker brick posts (John's orange), wooden caps, the lighter wood.
    add("swap", "andesite", R, "block/andesite=vr_brick", up)
    add("swap", "polished_andesite_stairs,polished_andesite_slab,polished_andesite", R, "block/polished_andesite=vr_cap", up)
    add("swap", "resin_bricks,resin_brick_stairs,resin_brick_slab", R, "block/resin_bricks=vr_brick_dark", up)
    add("swap", "birch_planks,birch_stairs,birch_slab", R, "block/birch_planks=vr_wood", up)
    # The paving (John's copy of another town's), as Pewter's Let's Go paving.
    for b in (R, PLATEAU):
        add("swap", "white_concrete,smooth_quartz,white_terracotta,chiseled_quartz_block,light_gray_concrete", b, PAVE, ("25", "33"))
    # The mountain over VICTORY ROAD (John's pink terracotta on top of the cliffs): the cliffs' earthy tops.
    add("swap", "pink_terracotta", R, "block/pink_terracotta=hill_top@4|drip_rock@4", ("24", "30"))
    # VICTORY ROAD's two mouths: a hole in the rock, framed in it, dark within (the cave itself is to come).
    for b in ((-929, 258, -922, 265, "VICTORY ROAD's mouth"), (-905, 258, -897, 265, "VICTORY ROAD's other mouth")):
        add("swap", "stone_bricks,smooth_stone_slab,black_concrete", b,
            "block/stone_bricks=drip_rock@4,block/smooth_stone=drip_rock_top|drip_rock@4,block/smooth_stone_slab_side=drip_rock@4,block/black_concrete=cave_dark", ("25", "31"))
    # The statues (10:41): Let's Go's, a stone POKe BALL on a grey pedestal with a gold trim, in place of John's.
    # (0.53, 11:16) The statues: John's own blocks (his are as Let's Go's), in statue stone, a polished gold trim on
    # top; the model laid over them before didn't read.
    for x, z in R23_STATUES:
        b = (x - 1, z, x + 1, z, "a statue")
        add("swap", "cauldron,hopper,blackstone_wall", b,
            "block/cauldron_side=statue_stone,block/cauldron_top=statue_stone_top,block/cauldron_bottom=statue_stone,block/cauldron_inner=statue_stone,"
            "block/hopper_outside=statue_stone,block/hopper_inside=statue_stone,block/hopper_top=statue_stone_top,block/blackstone=statue_stone,"
            "block/blackstone_top=statue_stone_top", ("24", "32"))
        add("swap", "chiseled_nether_bricks", b, "block/chiseled_nether_bricks=statue_crown", ("24", "32"))
        add("swap", "anvil,chipped_anvil,damaged_anvil", b,
            "block/anvil=statue_gold,block/anvil_top=statue_gold,block/chipped_anvil_top=statue_gold,block/damaged_anvil_top=statue_gold", ("24", "32"))
    # The INDIGO PLATEAU: its brick clean red brick, its stone base and eaves cream, every copper part polished metal
    # (bronze, and the weathered teal as teal), shining; its doorway dark.
    P = PLATEAU
    add("swap", "bricks,brick_stairs,brick_slab", P, "block/bricks=plateau_brick", ("30", "46"))
    add("swap", "andesite,polished_andesite_stairs,polished_andesite_slab", (-926, 150, -899, 171, "the INDIGO PLATEAU's base"),
        "block/andesite=plateau_stone,block/polished_andesite=plateau_stone", ("30", "46"))
    add("swap", "waxed_cut_copper,waxed_cut_copper_stairs,waxed_cut_copper_slab,waxed_copper_block,cut_copper,cut_copper_stairs,copper_block", P,
        "block/cut_copper=metal_bronze,block/copper_block=metal_bronze", ("24", "46"))
    add("swap", "waxed_copper_trapdoor,copper_trapdoor", P, "block/copper_trapdoor=metal_bronze_trapdoor", ("24", "46"))
    add("swap", "waxed_weathered_copper,waxed_weathered_cut_copper,waxed_weathered_cut_copper_stairs,weathered_copper,weathered_cut_copper,weathered_cut_copper_stairs", P,
        "block/weathered_copper=metal_teal,block/weathered_cut_copper=metal_teal", ("24", "46"))
    add("swap", "waxed_weathered_copper_trapdoor,weathered_copper_trapdoor", P, "block/weathered_copper_trapdoor=metal_teal_trapdoor", ("24", "46"))
    add("swap", "smooth_quartz_slab,polished_diorite_slab", P, "block/quartz_block_bottom=white_step,block/polished_diorite=white_step", ("25", "35"))
    add("swap", "black_concrete", (-916, 163, -909, 166, "the INDIGO PLATEAU's doorway"), "block/black_concrete=gate_dark", ("33", "40"))


# (0.53) The INDIGO PLATEAU's copper golem statues (on their pedestals).
PLATEAU_GOLEMS = [(-919, 32, 204), (-919, 32, 207), (-919, 36, 194), (-919, 36, 197), (-919, 40, 184), (-919, 40, 187), (-917, 36, 178), (-917, 36, 180),
                  (-916, 32, 204), (-916, 32, 207), (-916, 36, 194), (-916, 36, 197), (-916, 40, 184), (-916, 40, 187), (-909, 32, 204), (-909, 32, 207),
                  (-909, 36, 178), (-909, 36, 180), (-909, 36, 194), (-909, 36, 197), (-909, 40, 184), (-909, 40, 187), (-906, 32, 204), (-906, 32, 207),
                  (-906, 36, 194), (-906, 36, 197), (-906, 40, 184), (-906, 40, 187)]
CEL_FOUNTAIN = (-546, 420, -536, 430, "Celadon's fountain")
CEL_POND = (-521, 425, -507, 432, "the pond by the house")
CEL_BENCHES = [(-548, 422, -545, 428, "a bench by the fountain"), (-537, 422, -534, 428, "a bench by the fountain")]


GOLDENROD = (-2215, 615, -2040, 775, "Goldenrod City")
GOLD_IN = (-2214, 616, -2041, 774, "Goldenrod City")
# Goldenrod's Celadon buildings: the city east of the water, but for its RADIO TOWER.
GOLD_BOXES = [(-2165, 630, -2045, 677), (-2165, 695, -2045, 760), (-2165, 678, -2149, 694), (-2129, 678, -2045, 694)]
GOLD_LAMPS = [(-2152, 695), (-2152, 703), (-2131, 673), (-2109, 670), (-2109, 696), (-2109, 727), (-2109, 733), (-2098, 670), (-2098, 696), (-2098, 727),
              (-2098, 733), (-2072, 699), (-2072, 730), (-2071, 673), (-2207, 712), (-2178, 712), (-2167, 695), (-2167, 703)]
GOLD_SIGNS = [(-2188, 694), (-2146, 696), (-2117, 696)]


def round53(add, room):
    """0.53.0 (John's 11:16): see tools/looks53.py."""
    # The INDIGO PLATEAU: the stone walls along its roof its cream stone; its orange corner pillars a deeper red brick;
    # (11:57) each golem a statue of one of our own creatures instead: our RHYDON in polished bronze, on its pedestal
    # (the golem itself isn't drawn: the look pack).
    add("swap", "diorite_wall", PLATEAU, "block/diorite=plateau_stone", ("40", "48"))
    add("swap", "resin_bricks,resin_brick_stairs,resin_brick_slab", (-926, 150, -899, 171, "the INDIGO PLATEAU's corner pillars"),
        "block/resin_bricks=plateau_brick_dark", ("26", "46"))
    # ((0.57, 15:46: "the statues look funny ... go with the statues like inside the Victory Road house ... the
    # Pokeball") Stone POKe BALLs on the pedestals now: tools/arealooks57.py.)
    # The cliffs' stripped jungle wood (Route 22's "brown"), all over the map: the cliffs' rock and earthy tops, as their
    # mushroom blocks. (Where a place has its own look for it, a gate's, Lavender's timber, that's drawn instead.)
    add("swap", "stripped_jungle_wood", KANTO, "block/stripped_jungle_log=hill_top@4|drip_rock@4")
    # Saffron and Celadon: the oak stairs under the windows stone sills; the doors new glass doors.
    add("swap", "oak_stairs", SAFFRON, "block/oak_planks=sill_saf", ("34", "34"))
    add("swap", "oak_stairs", CELADON, "block/oak_planks=sill_cel", ("34", "34"))
    for a in (SAFFRON, CELADON):
        add("swap", "dark_oak_door", a, "block/dark_oak_door_top=door_glass_top,block/dark_oak_door_bottom=door_glass_bottom", ("33", "40"))
    # Celadon: the GAME CORNER's sign glossy enamel (its colour kept); the lamps one seamless post; the fountain pale
    # dressed stone; the benches wooden slats on dark green iron ends; the trims flat; the pond quartz all through.
    C = CELADON
    add("swap", "smooth_red_sandstone,smooth_red_sandstone_stairs,smooth_red_sandstone_slab,red_sandstone,red_sandstone_stairs,red_sandstone_slab,"
        "cut_red_sandstone,cut_red_sandstone_slab", C, "block/red_sandstone_top=gc_symbol,block/red_sandstone=gc_symbol,block/red_sandstone_bottom=gc_symbol,"
        "block/cut_red_sandstone=gc_symbol", ("33", "60"))
    import looks52
    for i, (x, z) in enumerate(CEL_LAMPS):
        b = (x - 1, z - 1, x + 1, z + 1, "a Celadon lamp")
        add("swap", "chiseled_tuff_bricks", b, "block/chiseled_tuff_bricks=lamp_cel_post,block/chiseled_tuff_bricks_top=lamp_cel_post", ("33", "42"))
    add("swap", "stone_bricks,stone_brick_stairs,stone_brick_slab,stone_brick_wall", CEL_FOUNTAIN, "block/stone_bricks=cel_fountain_stone", ("33", "43"))
    for b in CEL_BENCHES:
        add("swap", "polished_deepslate_slab", b, "block/polished_deepslate=bench_seat_cel", ("33", "34"))
        add("swap", "spruce_trapdoor", b, "block/spruce_trapdoor=bench_end_cel", ("33", "34"))
    add("swap", "warped_shelf", C, "block/warped_shelf=cel_trim", ("33", "60"))
    add("swap", "quartz_bricks,quartz_stairs,dripstone_block", CEL_POND,
        "block/quartz_bricks=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_side=quartz_clean,block/dripstone_block=quartz_clean", ("27", "33"))
    # GOLDENROD CITY's own (11:16): its streets red-brown brick paving, its pavements golden tiles, its kerbs pale
    # stone; its lamps its own (dark bronze posts, seamless, gold caps, amber globes); its fences crisp white with gold
    # caps, its white columns fluted with gold rings, the gold on its arch polished; its signs as Kanto's; its benches
    # as Celadon's new ones. (Its buildings are Celadon's: main() gives them Celadon's looks.)
    G = GOLD_IN
    g = ("18", "19")
    add("swap", "terracotta", G, "block/terracotta=gold_road", g)
    add("swap", "brown_glazed_terracotta", G, "block/brown_glazed_terracotta=gold_pave", g)
    add("swap", "white_terracotta", G, "block/white_terracotta=gold_kerb", g)
    add("swap", "pale_oak_fence", G, "block/pale_oak_planks=gold_fence", ("19", "40"))
    add("swap", "quartz_pillar", G, "block/quartz_pillar=gold_pillar,block/quartz_pillar_top=gold_pillar_top", ("19", "40"))
    add("swap", "end_rod", G, "block/end_rod=gold_rod", ("19", "40"))
    add("swap", "gold_block", G, "block/gold_block=gold_trim", ("19", "40"))
    add("swap", "polished_deepslate_slab", G, "block/polished_deepslate=bench_seat_cel", ("20", "20"))
    add("swap", "spruce_trapdoor", G, "block/spruce_trapdoor=bench_end_cel", ("20", "21"))
    for x, z in GOLD_SIGNS:
        add("sign", "pale_oak_shelf", (x - 1, z - 1, x + 2, z + 1, "a Goldenrod sign"), "-", ("21", "21"))
    for x, z in GOLD_LAMPS:
        b = (x - 1, z - 1, x + 1, z + 1, "a Goldenrod lamp")
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet,sea_lantern", b,
            "block/tuff=lamp_gold_post,block/polished_tuff=lamp_gold_post,block/chiseled_tuff_bricks=lamp_gold_post,"
            "block/chiseled_tuff_bricks_top=lamp_gold_post,block/gray_wool=lamp_gold_cap_top,block/sea_lantern=lamp_globe_amber!", ("20", "29"))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", ("20", "29"))


# (0.54) Johto (12:24 and 12:26): the whole region, and the places done now.
JOHTO = (-2720, 60, -1011, 1080, "Johto")
R26_HOUSES = (-1000, 655, -960, 712, "Route 26's houses")
ROUTE2627 = (-1310, 600, -940, 800, "Routes 26 and 27, Tohjo Falls")
NEW_BARK = (-1510, 715, -1395, 830, "New Bark Town")
CHERRYGROVE = (-1770, 705, -1650, 835, "Cherrygrove City")
BLACKTHORN = (-1580, 300, -1405, 445, "Blackthorn City, the Ice Path's mouth, Dragon's Den")
ROUTE45 = (-1560, 440, -1440, 700, "Route 45")
# Which Kanto town's looks go on which Johto place, and how far its ground is above or below.
# ((0.56, 13:53) Blackthorn's houses are its own, not Viridian's or Cerulean's: Cerulean's grey pillars went on its grey
# walls, the Gyms' tan on its Gym. round56 dresses them.)
TOWN_COPIES = [(PALLET, [(R26_HOUSES, 1, "Route 26's houses (as Pallet's)"), (CHERRYGROVE, 1, "Cherrygrove City (as Pallet)")]),
               (VIRIDIAN, [(CHERRYGROVE, -6, "Cherrygrove City (as Viridian)")]),
               (CERULEAN, [(NEW_BARK, -15, "New Bark Town (as Cerulean)")])]


def round54(add, room):
    """0.54.0 (John's 12:19, 12:24, 12:26): see tools/looks54.py."""
    import json as _json
    D = _json.load(open(os.path.join(HERE, "area_johto54.json")))
    # Every Mart's front (12:19: "a universal update"): its door pillars, its step, its sign over the door.
    for kind, place, x, y, z in SHOPS:
        # ((0.56, 13:53) Every Center's and Mart's door pillars and step "just pure white", and a Mart's sign as a
        # Center's: "the white background and then the red symbol", the Mart's in its blue.)
        name = place + (" Pokemon Center" if kind == "center" else " Mart")
        add("swap", "quartz_block,quartz_stairs,quartz_slab", (x - 2, z - 2, x + 2, z - 2, name + "'s pillars and step"), QUARTZ, (str(y - 6), str(y - 2)))
        if kind != "mart": continue
        add("swap", "polished_diorite,polished_diorite_stairs,polished_diorite_slab", (x - 2, z - 1, x + 2, z - 1, place + " Mart's sign"),
            "block/polished_diorite=quartz_clean", (str(y - 2), str(y + 2)))
        add("swap", "quartz_block,quartz_stairs,quartz_slab", (x - 1, z, x + 1, z, place + " Mart's sign"),
            "block/quartz_block_side=mart_sign_face,block/quartz_block_top=mart_sign_face,block/quartz_block_bottom=mart_sign_face", (str(y - 1), str(y + 1)))
    # The little flowers, crisper, everywhere (12:19).
    for a in (KANTO, JOHTO, ROUTE28):
        add("swap", "wildflowers", a, "block/wildflowers=flower_wild,block/wildflowers_stem=flower_wild_stem")
        add("swap", "dandelion,oxeye_daisy,azure_bluet,poppy,red_tulip", a,
            "block/dandelion=flower_dandelion,block/oxeye_daisy=flower_daisy,block/azure_bluet=flower_bluet,block/poppy=flower_poppy,block/red_tulip=flower_tulip")
    # ---------- Johto ----------
    J = JOHTO
    # Its rocks in the water (as Kanto's), its fences (12:19: darker than Kanto's whites), its paths (12:26: "the
    # ground and the paths and connected textures"), the snow at the ICE PATH (clean, laid over two).
    for x0, z0, x1, z1, y0, y1 in D["rocks"]:
        add("swap", SEA_ROCK_BLOCKS, (x0, z0, x1, z1, "a rock in the water"), SEA_ROCK, (str(y0), str(y1)))
    add("swap", "oak_fence,oak_fence_gate", J, "block/oak_planks=fence_johto")
    # ((0.56, 13:53: "the dirt path you've now created like squares ... it needs to follow the same for Kanto") Its dirt
    # paths the look pack's, as Kanto's (its path laid four blocks across, the grass and moss onto it); the brown
    # powder John edges them with the same path, laid where it is, so they run on.)
    add("swap", "brown_concrete_powder", J, "block/brown_concrete_powder=path_kanto@4")
    add("swap", "snow_block,snow", J, "block/snow=snow_clean@2")
    # Its lamps: the Cycling Road's (Routes 26 and 27, Blackthorn's), Cerulean's in New Bark, Cherrygrove's own.
    for x, y0, z, y1 in D["r17_posts"]:
        b = (x - 3, z - 3, x + 3, z + 3, "a Cycling Road lamp")
        add("swap", "red_sandstone_wall,red_sandstone,chiseled_red_sandstone,orange_carpet,sea_lantern", b,
            "block/red_sandstone=lamp_r17_post,block/chiseled_red_sandstone=lamp_r17_chisel,block/red_sandstone_top=lamp_r17_cap,"
            "block/orange_wool=lamp_r17_cap,block/sea_lantern=lamp_globe_warm!", (str(y0 - 2), str(y1 + 3)))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", (str(y0 - 2), str(y1 + 3)))
    inb = lambda p, a: a[0] <= p[0] <= a[2] and a[1] <= p[2] <= a[3]
    for x, y, z in D["lamps"].get("tuff", []):
        if not inb((x, y, z), NEW_BARK): continue
        b = (x - 1, z - 1, x + 1, z + 1, "a New Bark lamp")
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet,sea_lantern", b,
            "block/tuff=lamp_white_post,block/polished_tuff=lamp_white_post,block/chiseled_tuff_bricks=lamp_white_post,block/chiseled_tuff_bricks_top=lamp_white_post,"
            "block/gray_wool=lamp_cer_top,block/sea_lantern=lamp_globe_warm!", (str(y - 8), str(y + 2)))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", (str(y - 8), str(y + 2)))
    for x, y, z in D["lamps"].get("nether", []):
        b = (x - 1, z - 1, x + 1, z + 1, "a Cherrygrove lamp")
        add("swap", "nether_brick_fence,nether_brick_wall,chiseled_nether_bricks,black_carpet,sea_lantern", b,
            "block/nether_bricks=lamp_cg_post,block/chiseled_nether_bricks=lamp_cg_chisel,block/black_wool=lamp_cg_cap,block/sea_lantern=lamp_globe_warm!",
            (str(y - 8), str(y + 2)))
        add("glass", "black_stained_glass_pane", b, "block/black_stained_glass=glass_lamp", (str(y - 8), str(y + 2)))
    # Its bridges (Routes 26, 27, 45 and the rest), as Fuchsia's.
    BR = ("oak_slab,oak_planks,oak_stairs,stripped_oak_wood,stripped_oak_log,oak_shelf,stone_button,spruce_shelf,spruce_stairs,spruce_slab,"
          "spruce_planks,stripped_spruce_wood,stripped_spruce_log")
    for x0, z0, x1, z1, y0, y1 in D["bridges"]:
        if bridge45(x0, z0): continue
        add("swap", BR, (x0 - 1, z0 - 1, x1 + 1, z1 + 1, "a bridge"),
            "block/oak_planks=bridge_boards,block/spruce_planks=bridge_boards,block/stripped_oak_log=bridge_beam,block/stripped_oak_log_top=bridge_beam,"
            "block/stripped_spruce_log=bridge_beam,block/stripped_spruce_log_top=bridge_beam,block/oak_shelf=bridge_beam,block/spruce_shelf=bridge_beam,"
            "block/stone=bolt", (str(y0), str(y1)))
    # Its signs as Kanto's (the orange one with the white beside it on Route 27, one sign in both their colours).
    for x0, z0, x1, z1, y0, y1 in D["signs"]:
        add("sign", "pale_oak_shelf", (x0, z0, x1, z1, "a Johto sign"), "-", (str(y0), str(y1)))
        add("sign_o", "acacia_shelf", (x0, z0, x1, z1, "a Johto sign"), "-", (str(y0), str(y1)))
    # New Bark: its chimneys as Viridian's (the rest is Cerulean's, main()).
    add("swap", "mud_bricks,mud_brick_stairs,mud_brick_slab", NEW_BARK, "block/mud_bricks=chimney_brick", ("19", "40"))
    # Cherrygrove: its white path its own (pale flags, a blossom-pink joint), its bench as Celadon's new ones.
    add("swap", "calcite,diorite,polished_diorite", CHERRYGROVE, "block/calcite=pave_cg_a@2,block/diorite=pave_cg_b@2,block/polished_diorite=pave_cg_a@2", ("18", "18"))
    add("swap", "polished_deepslate_slab", CHERRYGROVE, "block/polished_deepslate=bench_seat_cel", ("19", "19"))
    add("swap", "spruce_trapdoor", CHERRYGROVE, "block/spruce_trapdoor=bench_end_cel", ("19", "20"))
    # ---------- Goldenrod (12:19) ----------
    G = GOLD_IN
    add("swap", "cobblestone,cracked_stone_bricks,stone_bricks,coarse_dirt", G,
        "block/cobblestone=gold_alley@2,block/cracked_stone_bricks=gold_alley@2,block/stone_bricks=gold_alley@2,block/coarse_dirt=gold_alley@2", ("19", "19"))
    add("swap", "barrel", G, "block/barrel_side=crate_barrel_side,block/barrel_top=crate_barrel_top,block/barrel_bottom=crate_barrel_top", ("19", "24"))
    add("swap", "composter", G, "block/composter_side=bin_side,block/composter_top=bin_top,block/composter_bottom=bin_top", ("19", "24"))
    add("swap", "waxed_cut_copper,waxed_cut_copper_stairs,waxed_cut_copper_slab", G, "block/cut_copper=gold_emblem", ("19", "40"))
    add("swap", "iron_bars", G, "block/iron_bars=railing_dark")
    add("swap", "quartz_pillar", G, "block/quartz_pillar=rail_base,block/quartz_pillar_top=gold_carpet_cap", ("20", "20"))
    add("swap", "white_carpet", G, "block/white_wool=gold_carpet_cap", ("21", "23"))
    add("swap", "smooth_quartz_slab,smooth_quartz,smooth_quartz_stairs", G, "block/quartz_block_bottom=quartz_clean", ("19", "23"))
    add("swap", "smooth_quartz_slab", (G[0], G[1], G[2], G[3], "the fence's caps"), "block/quartz_block_bottom=gold_carpet_cap", ("22", "22"))
    # The Magnet Train's line: its track (two steel rails down the middle of its five rows).
    for z in range(684, 689):
        add("swap", "gray_concrete", (-2146, z, -2050, z, "the Magnet Train's line"), "block/gray_concrete=" + ("track_rail" if z in (685, 687) else "track_bed"), ("27", "27"))
    # The station (west, over the water): its teal panels and white, its lights, its dark mullions and glass, its sign;
    # its fountains as Celadon's (12:19: "the way you've done the water fountain here").
    ST = (-2207, 672, -2178, 712, "the Magnet Train station")
    add("swap", "cyan_concrete", ST, "block/cyan_concrete=station_teal")
    add("swap", "white_concrete,light_blue_terracotta", ST, "block/white_concrete=station_white,block/light_blue_terracotta=cel_floor")
    add("swap", "pearlescent_froglight,verdant_froglight,ochre_froglight", ST,
        "block/pearlescent_froglight_side=station_light!,block/pearlescent_froglight_top=station_light!,block/verdant_froglight_side=station_light_green!,"
        "block/verdant_froglight_top=station_light_green!,block/ochre_froglight_side=station_light_amber!,block/ochre_froglight_top=station_light_amber!")
    add("swap", "deepslate_brick_wall", ST, "block/deepslate_bricks=station_mullion")
    add("swap", "black_stained_glass,black_stained_glass_pane", (-2215, 615, -2040, 775, "Goldenrod's dark glass"), "block/black_stained_glass=glass_dark")
    add("swap", "smooth_quartz,smooth_quartz_stairs,smooth_quartz_slab,quartz_slab,quartz_block", ST, "block/quartz_block_bottom=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_side=quartz_clean")
    FT = (-2206, 697, -2178, 711, "the station's fountains")
    add("swap", "stone_bricks,stone_brick_stairs,stone_brick_slab,stone_brick_wall", FT, "block/stone_bricks=cel_fountain_stone", ("19", "30"))
    add("swap", "cobblestone,gravel", FT, "block/cobblestone=pool_floor,block/gravel=pool_floor", ("15", "19"))
    add("swap", "green_glazed_terracotta", FT, "block/green_glazed_terracotta=gold_lawn", ("19", "19"))
    # The RADIO TOWER (12:19: "the radio building, which obviously is black"): dark navy metal panels, white panels,
    # steel grey, dark glass; the steps up into it stone; its aerials steel.
    RT = (-2148, 678, -2130, 694, "the Radio Tower")
    add("swap", "polished_deepslate,polished_deepslate_stairs,polished_deepslate_slab", RT, "block/polished_deepslate=radio_panel")
    add("swap", "white_terracotta", RT, "block/white_terracotta=radio_white", ("20", "70"))
    add("swap", "gray_concrete,smooth_stone,smooth_stone_slab", RT, "block/gray_concrete=radio_grey,block/smooth_stone=radio_white,block/smooth_stone_slab_side=radio_white")
    add("swap", "deepslate_tile_stairs", RT, "block/deepslate_tiles=step_cer", ("19", "21"))
    add("swap", "deepslate_tile_stairs", RT, "block/deepslate_tiles=radio_panel", ("22", "70"))
    add("swap", "waxed_exposed_copper_bars", (-2215, 615, -2040, 775, "Goldenrod's aerials"), "block/exposed_copper_bars=railing_dark", ("40", "80"))


# ---------- 0.56.0 (John's 13:53 and 13:55: "before we move on, we need to clean all those up") ----------
def bridge45(x0, z0):
    """Route 45's bridges and Blackthorn's (spruce, andesite slabs where the path steps on): round56's."""
    return -1530 <= x0 <= -1480 and 380 <= z0 <= 700


BRIDGE_DECK = ("spruce_stairs,spruce_slab,spruce_planks,stripped_spruce_wood,stripped_spruce_log,andesite_slab,andesite_stairs,andesite,"
               "oak_slab,oak_planks,oak_stairs,stripped_oak_wood,stripped_oak_log")
DECK = ("block/spruce_planks=bridge_boards,block/stripped_spruce_log=bridge_boards,block/stripped_spruce_log_top=bridge_boards,block/andesite=bridge_boards,"
        "block/oak_planks=bridge_boards,block/stripped_oak_log=bridge_boards,block/stripped_oak_log_top=bridge_boards")
RAILS = "block/spruce_shelf=bridge_beam,block/oak_shelf=bridge_beam,block/stone=bolt"
PIERS = ("block/stripped_spruce_log=bridge_beam,block/stripped_spruce_log_top=bridge_beam,block/stripped_oak_log=bridge_beam,"
         "block/stripped_oak_log_top=bridge_beam")
COPPER_CHAINS = ",".join(w + c + "copper_chain" for w in ("", "waxed_") for c in ("", "exposed_", "weathered_", "oxidized_"))
CHAIN_STEEL = ",".join("block/%scopper_chain=chain_steel" % c for c in ("", "exposed_", "weathered_", "oxidized_"))
CHIMNEY_CAP = ("block/stripped_oak_log=chimney_stone,block/stripped_oak_log_top=chimney_stone")
# John's Pallet houses in Johto (Cherrygrove's, New Bark's): Pallet's looks; his jungle wood round their windows a clean
# wooden trim (not the cliffs' rock); the chimneys Viridian's brick, their caps stone; New Bark's lab's shelving
# Viridian's green, its brick foot Cerulean's, its oak slab steps as Kanto's steps.
PALLET_KIT = [
    ("swap", "polished_andesite,polished_andesite_slab,polished_andesite_stairs", "block/polished_andesite=house_stone"),
    ("swap", "polished_deepslate", "block/polished_deepslate=house_dark"),
    ("swap", "polished_deepslate_slab", "block/polished_deepslate=window_low"),
    ("swap", "polished_deepslate_wall", "block/polished_deepslate=drainpipe"),
    ("swap", "polished_diorite_stairs,polished_diorite_slab", TRIM),
    ("swap", "jungle_stairs,jungle_slab,jungle_planks", "block/jungle_planks=trim_jungle"),
    ("swap", "mud_bricks,mud_brick_stairs,mud_brick_slab", "block/mud_bricks=chimney_brick"),
    ("swap", "stripped_oak_wood,stripped_oak_log", CHIMNEY_CAP),
    ("swap", "jungle_shelf,birch_shelf", "block/jungle_shelf=cap_shelf,block/birch_shelf=cap_shelf"),
    ("swap", "warped_shelf", "block/warped_shelf=green_shelf"),
    ("swap", "bricks", "block/bricks=base_cer"),
    ("swap", "oak_slab", "block/oak_planks=step_boards_d"),
]
# (look, blocks, swaps) for its roof: Pallet's red tiles on acacia, Cerulean's teal on prismarine.
ROOF_ACACIA = [("roof", "acacia_stairs", "-"), ("swap", "acacia_slab,acacia_planks", "block/acacia_planks=roof_tiles")]
ROOF_PRISMARINE = [("roof_cer", "prismarine_stairs", "-"), ("swap", "prismarine_slab,prismarine", "block/prismarine=roof_cer_tiles")]
# Each Johto house: (x0, z0, x1, z1, ground, top, name, roof).
JOHTO_HOUSES = [
    (-1739, 764, -1724, 777, 18, 29, "a Cherrygrove house", ROOF_ACACIA), (-1719, 767, -1704, 780, 18, 29, "a Cherrygrove house", ROOF_ACACIA),
    (-1698, 775, -1683, 788, 18, 29, "a Cherrygrove house", ROOF_ACACIA),
    (-1460, 741, -1440, 757, 18, 31, "Prof. Elm's lab", ROOF_PRISMARINE), (-1435, 750, -1420, 763, 18, 28, "a New Bark house", ROOF_PRISMARINE),
]
# Blackthorn's houses (13:53): "the roof has dripstone on the slabs ... The entrance, the doors, the trim under the
# windows and between the top and the bottom and the pillars either side". Their roofs in tiles of John's warm brown,
# the band between the floors (his jungle shelves and slabs) clean wood, the dark windows Pallet's, the posts either
# side dark timber, the planters at the foot clean wood, the step stone, the chimney Viridian's with a stone cap.
BT_HOUSES = [(-1497, 354, -1482, 366, 30, "a Blackthorn house"), (-1448, 368, -1433, 380, 30, "a Blackthorn house"),
             (-1511, 373, -1496, 385, 30, "a Blackthorn house")]


def round56(add, room):
    """0.56.0 (John's 13:53 and 13:55): see tools/looks56.py."""
    import json as _json
    D = _json.load(open(os.path.join(HERE, "area_johto54.json")))
    J = JOHTO
    # The steps (13:53: "The steps in nature ... do not follow the same premise as in Kanto, and should"; 13:55: "the
    # steps going up should be properly texturized"): John's mud brick slabs and stairs up Johto's ledges and paths
    # Kanto's steps (Route 26's too, on Kanto's side of the map).
    for a in (J, ROUTE2627):
        add("swap", "mud_brick_slab,mud_brick_stairs", a, "block/mud_bricks=step_boards_d")
    # Route 45's bridges and Blackthorn's (13:55: "the steps coming onto them, and also it should be the same colours
    # throughout ... the shelves on the end"): the deck one run of boards (its stairs, its slabs, its beams and the
    # andesite slabs stepping on at either end), the rails (shelves) to their ends and the posts under it beams, the
    # bolts.
    for x0, z0, x1, z1, y0, y1 in D["bridges"]:
        if not bridge45(x0, z0): continue
        box, d = (x0 - 4, z0 - 4, x1 + 4, z1 + 4, "a bridge on Route 45"), y0 + 1
        add("swap", BRIDGE_DECK, box, DECK, (str(d), str(d)))
        add("swap", "spruce_shelf,oak_shelf,stone_button", box, RAILS, (str(d), str(d + 1)))
        add("swap", "stripped_spruce_wood,stripped_spruce_log,stripped_oak_wood,stripped_oak_log", box, PIERS, (str(d - 14), str(d - 1)))
    # Route 26's signs (13:53: "I'm still seeing the double signs ... where the white shelves are"): as Kanto's.
    add("sign", "pale_oak_shelf", (-985, 663, -982, 665, "Route 26's sign"), "-", ("19", "20"))
    add("sign", "pale_oak_shelf", (-1000, 597, -997, 599, "Route 26's sign by Victory Road"), "-", ("26", "27"))
    # Route 27's posts and chains along the cliff top (13:53): one steel barrier, the chain hung from the posts.
    for a in (J, ROUTE2627):
        add("chain", COPPER_CHAINS, a)
        add("swap", COPPER_CHAINS, a, CHAIN_STEEL)
    add("swap", "oak_fence", (-1154, 748, -1111, 750, "Route 27's posts along the cliff"), "block/oak_planks=post_steel", ("26", "26"))
    # Johto's Pallet houses and Elm's lab.
    for x0, z0, x1, z1, g, top, name, roof in JOHTO_HOUSES:
        b, ys = (x0, z0, x1, z1, name), (str(g), str(top))
        for look, blocks, swaps in PALLET_KIT + roof:
            add(look, blocks, b, swaps, ys)
        # ((0.59, 18:25) Their pink terracotta walls white render, as Pallet's.)
        add("swap", "white_terracotta", b, "block/white_terracotta=jhouse_wall", ys)
    # Blackthorn's houses.
    for x0, z0, x1, z1, g, name in BT_HOUSES:
        b, band = (x0, z0, x1, z1, name), g + 4
        add("roof_bt", "jungle_stairs", b, "-", (str(band + 1), str(band + 6)))
        add("swap", "jungle_planks,jungle_slab", b, "block/jungle_planks=roof_bt_tiles", (str(band + 1), str(band + 6)))
        add("swap", "jungle_slab,jungle_planks", b, "block/jungle_planks=trim_jungle", (str(band), str(band)))
        add("swap", "jungle_shelf", b, "block/jungle_shelf=band_shelf", (str(band), str(band)))
        add("swap", "stripped_dark_oak_wood,stripped_dark_oak_log", b, "block/stripped_dark_oak_log=post_bt,block/stripped_dark_oak_log_top=post_bt_top",
            (str(g + 1), str(band)))
        add("swap", "dark_oak_shelf", b, "block/dark_oak_shelf=shelf_wood", (str(g + 1), str(g + 1)))
        add("swap", "warped_door", b, "block/warped_door_top=door_bt_top,block/warped_door_bottom=door_bt_bottom", (str(g + 1), str(g + 3)))
        add("swap", "polished_deepslate_slab", b, "block/polished_deepslate=window_low", (str(g + 1), str(band + 6)))
        add("swap", "smooth_stone_slab", b, "block/smooth_stone=house_stone,block/smooth_stone_slab_side=house_stone", (str(g + 1), str(g + 1)))
        add("swap", "mud_bricks,mud_brick_stairs,mud_brick_slab", b, "block/mud_bricks=chimney_brick", (str(band + 1), str(band + 7)))
        add("swap", "stripped_oak_wood,stripped_oak_log", b, CHIMNEY_CAP, (str(band + 1), str(band + 7)))
        add("swap", "birch_shelf", b, "block/birch_shelf=cap_shelf", (str(band + 1), str(band + 7)))
    # Goldenrod's DEPT. STORE (13:55: "That's definitely the old blue. You can see the old slabs"): its blue facing
    # and the paler blue band a crisp blue panel each, its red corners and roof crisp red, the ledges round every floor
    # (quartz stairs and blocks) clean white stone, the mullions the station's, the steel on its top clean.
    DS = (-2098, 713, -2073, 726, "the Goldenrod DEPT. STORE")
    up = ("20", "58")
    add("swap", "light_blue_concrete", DS, "block/light_blue_concrete=ds_blue", up)
    add("swap", "light_blue_terracotta", DS, "block/light_blue_terracotta=ds_blue_light", ("21", "58"))
    add("swap", "red_terracotta", DS, "block/red_terracotta=ds_red", up)
    add("swap", "red_nether_bricks,red_nether_brick_stairs,red_nether_brick_slab,red_nether_brick_wall", DS, "block/red_nether_bricks=ds_roof", up)
    add("swap", "quartz_stairs,quartz_block,quartz_slab,smooth_quartz_slab,smooth_quartz_stairs", DS,
        "block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean", ("21", "58"))
    add("swap", "deepslate_brick_wall", DS, "block/deepslate_bricks=station_mullion", up)
    add("swap", "iron_block", DS, "block/iron_block=ds_steel", up)
    add("swap", "birch_stairs,birch_slab", DS, "block/birch_planks=trim_white", up)
    # The station's planters (13:55: "the shelves around the plants at the base"): Saffron's cream planters; and the
    # sign beside them as Kanto's.
    add("sign", "pale_oak_shelf", (-2189, 694, -2186, 695, "the station's sign"), "-", ("20", "21"))
    add("swap", "pale_oak_shelf", (-2205, 690, -2180, 693, "the station's planters"), "block/pale_oak_shelf=planter_saf_cream", ("20", "20"))


if __name__ == "__main__":
    main()
