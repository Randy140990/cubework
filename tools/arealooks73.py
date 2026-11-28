"""
(0.73, John's 28 Nov 09:22 save) The looks for what he's changed on the map, after round70b (tools/looks73.py draws
the new pictures; tools/worlddiff.py found what changed between the 1.0.1 save and this one):

- SAFFRON's MAGNET TRAIN station: his Goldenrod station copied into Saffron's north-west corner, flipped east to west
  (Goldenrod's block at x is Saffron's at -2507 - x, z - 295, y + 13). "the track and stuff, absolutely all that's
  the same ... I purely mean just probably wants its own style to feel Kanto versus Johto ... a different roof
  colour". The same looks as Goldenrod's in Saffron's colours: its curved roof Saffron's green (saf_dome), its posts
  and the roof's rim white, its band yellow (Goldenrod's teal), its walls Saffron's cream, its badge green; the line
  (its rails, its white sides) and the band under the top windows exactly Goldenrod's.
- The tunnels at both ends ("an area specifically to allow the trams to disappear"): the black at the back the dark of
  a cave's mouth; the cliffs round them already have the mountains' looks (all Kanto's and Johto's); Saffron's
  cliff's timbers set in it the cliffs' rock, as Johto's ("the mountains are stylized"). Goldenrod's line runs on into
  its tunnel two blocks further now.
- Saffron's new bench and tree ("Same style updates as what we've already done"): the bench as Celadon's benches; the
  tree's trunk (spruce fence) a tree's trunk, as the other towns' little trees, and every other little tree in Saffron
  the same (none had it: their trunks were fence posts).
- The hedges of the building John took out for the station (found on the 1.0.1 map) left out: no hedge there now.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


STATION = (-395, 383, -377, 399, "Saffron's MAGNET TRAIN station")
LINE = (-409, 383, -377, 399, "Saffron's MAGNET TRAIN station and its line")
CLIFF = (-440, 362, -400, 406, "the cliff round Saffron's MAGNET TRAIN tunnel")
# The building John took out (its hedges gone with it).
GONE = (-440, 360, -376, 410)
QUARTZ = ("block/quartz_block_side=quartz_clean,block/quartz_block_top=quartz_clean,block/quartz_block_bottom=quartz_clean")


def round73(add, G):
    S = STATION
    # The station in Saffron's colours.
    add("swap", "white_terracotta", S, "block/white_terracotta=saf_cream", ys(33, 46))
    add("swap", "polished_diorite_slab", S, "block/polished_diorite=saf_dome@4", ys(40, 48))
    add("swap", "polished_diorite,polished_diorite_stairs", S, "block/polished_diorite=saf_white", ys(33, 48))
    add("swap", "chiseled_stone_bricks", S, "block/chiseled_stone_bricks=saf_white", ys(40, 48))
    add("swap", "cyan_terracotta", S, "block/cyan_terracotta=saf_wall", ys(40, 46))
    add("swap", "quartz_pillar", S, "block/quartz_pillar=saf_post,block/quartz_pillar_top=saf_post_top", ys(33, 38))
    add("swap", "waxed_cut_copper,waxed_cut_copper_slab,waxed_cut_copper_stairs", S, "block/cut_copper=saf_emblem", ys(36, 40))
    add("swap", "light_gray_carpet", S, "block/light_gray_wool=saf_grey", ys(34, 34))
    add("swap", "end_rod", S, "block/end_rod=rod_light", ys(33, 46))
    # As Goldenrod's: the planters' wood, the band under the top windows, the brick foot, the blinds, the crate and bin.
    add("panel", "oak_shelf", S, "block/oak_shelf=planter_saf_wood", ys(33, 33))
    add("panel", "oak_shelf", S, "block/oak_shelf=band_shelf", ys(34, 41))
    add("swap", "bricks", S, "block/bricks=cel_base", ys(33, 33))
    add("swap", "deepslate_brick_wall", S, "block/deepslate_bricks=saf_blind", ys(33, 46))
    add("swap", "barrel", S, "block/barrel_side=crate_barrel_side,block/barrel_top=crate_barrel_top,block/barrel_bottom=crate_barrel_top", ys(33, 35))
    add("swap", "composter", S, "block/composter_side=bin_side,block/composter_top=bin_top,block/composter_bottom=bin_top", ys(33, 35))
    # The line and the station's white: Goldenrod's.
    add("swap", "smooth_quartz,smooth_quartz_stairs,smooth_quartz_slab,quartz_slab", LINE, QUARTZ, ys(33, 44))
    for z in range(389, 394):
        add("swap", "gray_concrete", (-409, z, -385, z, "the MAGNET TRAIN's line, Saffron"), "block/gray_concrete=" + ("track_rail" if z in (390, 392) else "track_bed"), ys(40, 40))
    # Goldenrod's line on into its tunnel.
    for z in range(684, 689):
        add("swap", "gray_concrete", (-2049, z, -2048, z, "the MAGNET TRAIN's line, Goldenrod's tunnel"), "block/gray_concrete=" + ("track_rail" if z in (685, 687) else "track_bed"), ys(27, 27))
    # The tunnels' backs: the dark of a cave's mouth.
    add("swap", "black_concrete", (-411, 385, -409, 397, "Saffron's MAGNET TRAIN tunnel"), "block/black_concrete=tunnel_dark", ys(36, 45))
    add("swap", "black_concrete", (-2048, 680, -2046, 692, "Goldenrod's MAGNET TRAIN tunnel"), "block/black_concrete=tunnel_dark", ys(23, 32))
    # Saffron's tunnel cliff's timbers: the cliffs' rock, a shade darker with roots through it, the ledges' earth on top.
    add("swap", "stripped_spruce_wood,stripped_spruce_log", CLIFF,
        "block/stripped_spruce_log=hill_top@4|cliff_root@4,block/stripped_spruce_log_top=hill_top@4|cliff_root@4")
    # The new bench, as Celadon's.
    B = (-393, 401, -391, 407, "Saffron's bench by the station")
    add("swap", "polished_deepslate_slab", B, "block/polished_deepslate=bench_seat_cel", ys(33, 34))
    add("swap", "spruce_trapdoor", B, "block/spruce_trapdoor=bench_end_cel", ys(33, 34))
    # Saffron's little trees' trunks (tools/area_saffron_trunks73.json: each trunk's fence blocks, found on the map).
    for x0, z0, x1, z1, y0, y1, _n in json.load(open(os.path.join(HERE, "area_saffron_trunks73.json"))):
        add("boulder", "spruce_fence,oak_fence,dark_oak_fence", (x0, z0, x1, z1, "Saffron's tree"), "prefix:ttrunk", ys(y0, y1))


def post(rows):
    """The hedges of the building taken out for the station: none there now."""
    def gone(r):
        if r[0] != "leafy_hedge": return False
        x0, z0, x1, z1 = int(r[2]), int(r[3]), int(r[4]), int(r[5])
        return GONE[0] <= x0 and x1 <= GONE[2] and GONE[1] <= z0 and z1 <= GONE[3]
    out = [r for r in rows if not gone(r)]
    print("hedges where the station is now left out:", len(rows) - len(out))
    return out
