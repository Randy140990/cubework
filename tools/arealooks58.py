"""
0.58.0's looks, for tools/arealooks.py: round58() adds the rows (called after round57, at the end of round50's chain).

  - (17:09: "there'll be one in Cherry Grove City, and there will be one before you get to the beach near Olivine City
    as well, and there might be a couple of others throughout") Johto's swings, as Routes 14 and 17's (0.50): the arch
    all hedge with its flowers, the frame a white pergola, the seat yellow slats, the bars dark metal, the lanterns
    garden lights. Found on the Johto map by their spruce trapdoor seats under oak log frames and azalea arches.
"""

# (x, z of the swing's middle, the seat's y): six on the map.
JOHTO_SWINGS = [
    (-1743, 769, 19, "Cherrygrove City's swing"),
    (-2337, 571, 18, "the swing west of Olivine, before the beach"),
    (-1716, 628, 22, "Route 30's swing"),
    (-2096, 768, 20, "Route 34's swing"),
    (-2526, 716, 18, "the swing by Route 41"),
    (-1601, 421, 22, "the swing by Route 44"),
]


def round58(add, room, G):
    leaves = G["LEAVES_ALL"]
    for x, z, y, name in JOHTO_SWINGS:
        b = (x - 5, z - 5, x + 5, z + 5, name)
        ys = (str(y - 3), str(y + 9))
        add("leafy_hedge", leaves, b, "flowering_azalea", ys)
        add("swap", "oak_log", b, "block/oak_log=pergola,block/oak_log_top=pergola_top", ys)
        add("swap", "spruce_trapdoor", b, "block/spruce_trapdoor=bench_slat", ys)
        add("swap", "spruce_fence,spruce_fence_gate", b, "block/spruce_planks=bench_frame", ys)
        add("swap", "waxed_copper_lantern,copper_lantern", b, "block/copper_lantern=garden_light", ys)
