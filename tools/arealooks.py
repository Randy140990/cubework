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
LAMP = ("block/tuff=lamp_tuff,block/polished_tuff=lamp_polished_tuff,block/chiseled_tuff_bricks=lamp_chiseled_tuff_bricks,"
        "block/chiseled_tuff_bricks_top=lamp_chiseled_tuff_bricks_top,block/gray_wool=lamp_gray_wool")
CENTER = ("block/stripped_mangrove_log=center_stripped_mangrove_log,block/stripped_mangrove_log_top=center_stripped_mangrove_log_top,"
          "block/mangrove_planks=center_mangrove_planks")
MART = "block/blue_concrete=mart_blue_concrete,block/blue_wool=mart_blue_wool"


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
    add("swap", "calcite,diorite,polished_diorite", VIRIDIAN, PATH, ("24", "25"))
    add("kerb", "calcite,diorite,polished_diorite", VIRIDIAN, "-", ("24", "25"))
    for a in (VIRIDIAN, ROUTE22):
        add("swap", "tuff_wall,polished_tuff_wall,chiseled_tuff_bricks,gray_carpet", a, LAMP)
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
    with open(OUT, "w") as f:
        f.write("# Looks the mod gives some places only (client/AreaLooks), written by tools/arealooks.py: which look, on which blocks,\n"
                "# over which part of the map (x0 z0 to x1 z1; y0 y1 the heights, - for every height), the textures swapped.\n"
                "# swap: textures for others (old=new, the new ones the mod's own, in textures/block/area; @4: laid from where the face\n"
                "# is, a picture four blocks across). roof: HeartGold's red tiles along the eaves. sign: shelves and standing pale oak\n"
                "# signs as HeartGold's signs. wood_sign: standing spruce signs as its wooden sign, taller. grass_edge: the grass's tufts\n"
                "# onto the dripstone round the water. kerb: a grey kerb with the grass rounded onto it, on a path's blocks.\n"
                "#look\tblocks\tx0\tz0\tx1\tz1\ty0\ty1\tswaps\twhere\n")
        for r in rows:
            f.write("\t".join(str(v) for v in r) + "\n")
    print(len(rows), "rows")


if __name__ == "__main__":
    main()
