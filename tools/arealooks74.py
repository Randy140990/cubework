#!/usr/bin/env python3
"""
(0.74) John's 28 Nov 10:42 and 10:56: the area looks for

  - GOLDENROD's floor: "take that pattern and extend it all the way through goldenrod". The MAGNET TRAIN plaza's
    (tools/arealooks70b.py, the left-hand side he liked) all through the city: its terracotta the plaza's big cream
    flagstones (gold_plaza, four blocks to a picture), its glazed terracotta paths the same flagstones in the paths'
    yellow (gold_plaza_path, tools/looks74.py); the plaza itself keeps its gold square.
  - GOLDENROD's MAGNET TRAIN station roof, white till now: "something different": Goldenrod's teal (gold_dome), its
    rim and posts white, as Saffron's green one.
  - SAFFRON's line run on west to its new tunnel (his 10:56 save): the line's white sides and its rails all the way, the
    tunnel's new dark back, the cliff's timbers round its mouth as cliff rock.

Called from tools/arealooks.py after the 0.73 rows (so these win where they overlap).
"""


def ys(a, b):
    return (str(a), str(b))


GOLD_STATION = (-2130, 678, -2112, 694, "Goldenrod's MAGNET TRAIN station")
QUARTZ = "block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean"


def round74(add, G):
    # Goldenrod's station roof: Goldenrod's teal.
    add("swap", "polished_diorite_slab", GOLD_STATION, "block/polished_diorite=gold_dome@4", ys(27, 35))
    add("swap", "polished_diorite,polished_diorite_stairs", GOLD_STATION, "block/polished_diorite=saf_white", ys(20, 35))
    # Saffron's line on west to the new tunnel (x -433 to -410, z 388 to 394).
    LINE = (-436, 383, -409, 399, "Saffron's MAGNET TRAIN line, on to its tunnel")
    add("swap", "smooth_quartz,smooth_quartz_stairs,smooth_quartz_slab,quartz_slab", LINE, QUARTZ, ys(33, 44))
    for z in range(389, 394):
        add("swap", "gray_concrete", (-433, z, -409, z, "the MAGNET TRAIN's line, Saffron"), "block/gray_concrete=" + ("track_rail" if z in (390, 392) else "track_bed"),
            ys(40, 40))
    add("swap", "black_concrete", (-436, 385, -433, 397, "Saffron's MAGNET TRAIN tunnel"), "block/black_concrete=tunnel_dark", ys(36, 45))
    add("swap", "stripped_spruce_wood,stripped_spruce_log", (-452, 360, -400, 406, "the cliff round Saffron's MAGNET TRAIN tunnel"),
        "block/stripped_spruce_log=hill_top@4|cliff_root@4,block/stripped_spruce_log_top=hill_top@4|cliff_root@4")
