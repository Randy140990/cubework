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
ROUTE22 = (-960, 495, -831, 600, "Route 22")

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
GYM = ("block/jungle_planks=gym_flat,block/oak_planks=gym_flat_light,block/stripped_jungle_log=gym_beam,"
       "block/stripped_jungle_log_top=gym_beam_top")


def main():
    rows = []
    add = lambda look, blocks, area, swaps="-", ys=("-", "-"): rows.append((look, blocks, area[0], area[1], area[2], area[3], ys[0], ys[1], swaps, area[4]))
    for a in (PALLET_R1, VIRIDIAN, ROUTE22):
        add("swap", "pale_oak_fence", a, FENCE)
    add("roof", "acacia_stairs", (-806, 673, -795, 682, "your house"))
    add("roof", "acacia_stairs", (-788, 673, -777, 682, "Blue's house"))
    for a in (PALLET_R1, VIRIDIAN, ROUTE2, ROUTE22):
        add("sign", "pale_oak_shelf,pale_oak_sign", a)
        add("wood_sign", "spruce_sign", a)
    add("grass_edge", "dripstone_block", PALLET)
    # Viridian City's two green houses: their roofs (warped stairs) in green tiles, their windows' frames (pale oak
    # stairs and slabs) solid white.
    add("roof_green", "warped_stairs", VIRIDIAN)
    add("swap", "pale_oak_stairs,pale_oak_slab", VIRIDIAN, "block/pale_oak_planks=window_frame")
    add("swap", "calcite,diorite,polished_diorite", VIRIDIAN, PATH, ("24", "25"))
    add("kerb", "calcite,diorite,polished_diorite", VIRIDIAN, "-", ("24", "25"))
    for a in (VIRIDIAN, ROUTE22):
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet", a, LAMP)
    # Pallet Town's houses: pillars and steps, the dark parts round the underside, the trim across the front, the
    # drainpipe.
    for h in HOUSES:
        add("swap", "polished_andesite,polished_andesite_slab", h, "block/polished_andesite=house_stone", ("17", "26"))
        add("swap", "polished_deepslate,polished_deepslate_slab", h, "block/polished_deepslate=house_dark", ("17", "26"))
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
    # Every route gate on the map (the buildings with polished deepslate roofs, tools/area_gates.tsv).
    for line in open(os.path.join(HERE, "area_gates.tsv")):
        if not line.strip() or line.startswith("#"):
            continue
        x0, x1, z0, z1, ry, where = line.rstrip("\n").split("\t")
        ry = int(ry)
        add("swap", GATE_BLOCKS, (int(x0) - 2, int(z0) - 4, int(x1) + 2, int(z1) + 4, where), GATE, (str(ry - 18), str(ry + 2)))
    add("swap", "mud_bricks", ROUTE22, "block/mud_bricks=route_path@4")
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
    # Every Gym's roof: its stairs in tan tiles, its planks and beams solid tan shades.
    for line in open(os.path.join(HERE, "area_gyms.tsv")):
        if not line.strip() or line.startswith("#"):
            continue
        place, x0, x1, y0, y1, z0, z1 = line.rstrip("\n").split("\t")
        box = (int(x0) - 1, int(z0) - 1, int(x1) + 1, int(z1) + 1, place + " Gym")
        add("roof_gym", "jungle_stairs,oak_stairs", box, "-", (y0, str(int(y1) + 1)))
        add("swap", "jungle_planks,oak_planks,stripped_jungle_log,jungle_slab,oak_slab", box, GYM, (y0, str(int(y1) + 1)))
    with open(OUT, "w") as f:
        f.write("# Looks the mod gives some places only (client/AreaLooks), written by tools/arealooks.py: which look, on which blocks,\n"
                "# over which part of the map (x0 z0 to x1 z1; y0 y1 the heights, - for every height), the textures swapped.\n"
                "# swap: textures for others (old=new, the new ones the mod's own, in textures/block/area; @4: laid from where the face\n"
                "# is, a picture four blocks across). roof, roof_green, roof_gym: rows of tiles along the eaves (HeartGold's red, the\n# green houses' green, the Gyms' tan). sign: shelves and standing pale oak\n"
                "# signs as HeartGold's signs. wood_sign: standing spruce signs as its wooden sign, taller. grass_edge: the grass's tufts\n"
                "# onto the dripstone round the water. kerb: a grey kerb with the grass rounded onto it, on a path's blocks.\n"
                "#look\tblocks\tx0\tz0\tx1\tz1\ty0\ty1\tswaps\twhere\n")
        for r in rows:
            f.write("\t".join(str(v) for v in r) + "\n")
    print(len(rows), "rows")


if __name__ == "__main__":
    main()
