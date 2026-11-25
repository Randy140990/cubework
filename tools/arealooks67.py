"""(0.67) John's 16:28 walk round Kanto: the rows for it (the pictures are tools/looks67.py's)."""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


def round67(add, room, G):
    # Route 12 by the TM27 gate: "one of my stones ... which hasn't been texturized, and probably due to its
    # proximity". The rock in the water against the gate's east side (tuff, a diorite slab, cobble and andesite bits):
    # the map scan left it out, it being half inside the gate's own box. Now the sea rocks' look, as the rest.
    add("swap", G["SEA_ROCK_BLOCKS"], (-119, 483, -114, 488, "a rock in the water"), G["SEA_ROCK"], ys(24, 31))

    # The Power Plant's top: its yellow band (two blocks high) and the white cap along it, seamless ("Instead of
    # things looking like blocks, I think they should be seamless"): one clean colour each, laid four blocks across.
    P = (-160, 305, -120, 333, "the Power Plant's top trim")
    add("swap", "yellow_terracotta", P, "block/yellow_terracotta=pp_yellow_s@4", ys(43, 44))
    add("swap", "smooth_quartz_slab,smooth_quartz,quartz_block", P,
        "block/quartz_block_bottom=pp_white_s@4,block/quartz_block_top=pp_white_s@4,block/quartz_block_side=pp_white_s@4", ys(43, 45))

    # The strip of packed mud right of Nugget Bridge, along the water's edge (x -340, z 199 to 225): "turned into the
    # same as the dripstone changes for the cliff". The cliff's own rock on it, and the grass's tufts onto it.
    S = (-340, 199, -340, 225, "the strip by Nugget Bridge")
    add("swap", "packed_mud", S, "block/packed_mud=drip_rock_top|drip_rock@4", ys(34, 34))
    add("grass_edge", "packed_mud", S, "-", ys(34, 34))

    # Victory Road's buildings: their band along the top (andesite, y 32), the long bars on it and the caps at their
    # ends (polished andesite slabs and stairs, y 33) dark gold with a shine (tools/looks67.dark_gold), in place of
    # the light brick (arealooks594's row for the slabs is this one's now).
    V = (-935, 150, -890, 505, "Victory Road's buildings' tops")
    add("swap", "andesite", V, "block/andesite=vr_gold@4", ys(32, 32))
    add("swap", "polished_andesite_slab,polished_andesite_stairs", V, "block/polished_andesite=vr_gold@4", ys(33, 33))

    # The buttons on the stones (John's touches of shape on the rocks by Victory Road): each the stone it's on, the
    # rock's picture run on over it (found by a sweep of every button on the map and what it's on).
    for x, y, z, spec in json.load(open(os.path.join(HERE, "area_rock_buttons67.json"))):
        add("swap", "jungle_button,stone_button,oak_button,spruce_button,birch_button,dark_oak_button,acacia_button,mangrove_button,cherry_button,bamboo_button,"
            "crimson_button,warped_button,pale_oak_button,polished_blackstone_button", (x, z, x, z, "a button on a rock"), spec, ys(y, y))
