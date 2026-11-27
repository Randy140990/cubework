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
    add("swap", "yellow_terracotta", P, "block/yellow_terracotta=pp_yellow_seam@4", ys(43, 44))
    add("swap", "smooth_quartz_slab,smooth_quartz,quartz_block", P,
        "block/quartz_block_bottom=pp_white_seam@4,block/quartz_block_top=pp_white_seam@4,block/quartz_block_side=pp_white_seam@4", ys(43, 45))

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

    # (0.67.1, John's 17:13) Both ships retextured: the S.S. ANNE and the S.S. AQUA, every kind of block in its own
    # colour in our looks (tools/shiplooks.py made the pictures and these rows), in slices along each ship small
    # enough to win over the places round them.
    for r in json.load(open(os.path.join(HERE, "area_ships67.json")))["rows"]:
        x0, z0, x1, z1 = r["box"]
        add("swap", r["blocks"], (x0, z0, x1, z1, r["note"]), r["spec"], ys(*r["ys"]))

    # The buttons on the stones (John's touches of shape on the rocks by Victory Road): each the stone it's on, the
    # rock's picture run on over it (found by a sweep of every button on the map and what it's on).
    for x, y, z, spec in json.load(open(os.path.join(HERE, "area_rock_buttons67.json"))):
        add("swap", "jungle_button,stone_button,oak_button,spruce_button,birch_button,dark_oak_button,acacia_button,mangrove_button,cherry_button,bamboo_button,"
            "crimson_button,warped_button,pale_oak_button,polished_blackstone_button", (x, z, x, z, "a button on a rock"), spec, ys(y, y))


def round67j(add, room, G):
    """(0.68, John's 26 Nov 09:20: "a pass across all of Johto and touch up any external materials") What the pass found
    still in Minecraft's own pictures on Johto's outside (tools/lookview.py, every face of every built block, both ways
    round), and the rocks a scan had missed."""
    import arealooks59b
    BOULDER, BOULDER_BLOCKS = G["BOULDER"], G["BOULDER_BLOCKS"]
    # The cliffside west of Cianwood (Route 47's way to the SAFARI ZONE's gate): left out of Johto's own looks while
    # John was building it (19:17); built now, so the same as the rest of Johto: its grey paths along the cliff tops,
    # its posts, its shelf trims, its white and grey (arealooks59b's fallbacks, over just that part).
    JW = (-2770, 60, -2576, 1000, "the cliffside west of Cianwood")   # ((0.70) out to his new west end)
    for look, blocks, swaps in arealooks59b.FALLBACK:
        if blocks == "light_gray_terracotta": continue   # (its paths, below)
        add(look, blocks, JW, swaps)
    # Its paths of light grey stone along the cliff tops: Kanto's soft, seamless path in that grey (looks67.grey_path).
    add("swap", "light_gray_terracotta", JW, "block/light_gray_terracotta=cliff_path_grey@4")
    add("swap", "black_concrete", JW, "block/black_concrete=cin_post")
    # Its two plank bridges (built after the bridges' scan): a bridge's own look, as the third one there has.
    BR = ("oak_slab,oak_planks,oak_stairs,stripped_oak_wood,stripped_oak_log,oak_shelf,stone_button,spruce_shelf,spruce_stairs,spruce_slab,spruce_planks,"
          "stripped_spruce_wood,stripped_spruce_log")
    BRS = ("block/oak_planks=bridge_boards,block/spruce_planks=bridge_boards,block/stripped_oak_log=bridge_beam,block/stripped_oak_log_top=bridge_beam,"
           "block/stripped_spruce_log=bridge_beam,block/stripped_spruce_log_top=bridge_beam,block/oak_shelf=bridge_beam,block/spruce_shelf=bridge_beam,block/stone=bolt")
    for b, y0, y1 in (((-2664, 785, -2641, 792, "a bridge"), 33, 36), ((-2628, 782, -2616, 789, "a bridge"), 29, 32)):
        add("panel", BR, b, BRS, ys(y0, y1))
        add("swap", "andesite_slab,andesite,andesite_stairs", b, "block/andesite=bridge_boards", ys(y0, y1))
    # Its signboard on the cliff (a frame of birch, its pattern in iron and violet glass, sea lanterns lighting it).
    SB = (-2648, 752, -2642, 761, "the cliffside's signboard")
    add("swap", "iron_block", SB, "block/iron_block=ds_steel", ys(40, 52))
    add("swap", "purple_stained_glass,purple_stained_glass_pane", SB, "block/purple_stained_glass=lamp_globe_violet!", ys(40, 52))
    add("swap", "sea_lantern", SB, "block/sea_lantern=lamp_globe_warm!", ys(40, 52))
    add("cube", "stripped_birch_wood,stripped_birch_log", SB, "wood_shelf", ys(40, 52))
    # The TIN TOWER's spire, its top above where the tower's looks stopped (y 84): its gold, its dark metal.
    TT = (-1992, 337, -1964, 366, "the TIN TOWER's spire")
    add("swap", "gold_block", TT, "block/gold_block=spire_gold", ys(85, 96))
    add("swap", "polished_deepslate_wall,oxidized_lightning_rod,lightning_rod,weathered_lightning_rod,exposed_lightning_rod", TT,
        "block/polished_deepslate=spire_metal,block/lightning_rod=spire_metal,block/oxidized_lightning_rod=spire_metal,block/weathered_lightning_rod=spire_metal,"
        "block/exposed_lightning_rod=spire_metal", ys(85, 96))
    # The house by the BURNED TOWER: its two whole nether brick blocks on the roof, as its roof's slabs and stairs.
    add("swap", "nether_bricks", (-2048, 356, -2022, 374, "the house by the BURNED TOWER"), "block/nether_bricks=redb_roof", ys(25, 36))
    # Its cyan mat out front (two by seven): a woven mat, clean (looks67.mat).
    add("swap", "cyan_carpet", (-2039, 364, -2031, 367, "the mat by the house by the BURNED TOWER"), "block/cyan_wool=mat_cyan", ys(22, 22))
    # The lighthouse: its pale stairs up the lantern room's sides white as its walls, its dark studs as bolts.
    LH = (-2233, 570, -2205, 590, "the lighthouse")
    add("swap", "pale_oak_stairs,pale_oak_slab,pale_oak_planks", LH, "block/pale_oak_planks=lh_white", ys(20, 62))
    add("swap", "polished_blackstone_button", LH, "block/polished_blackstone=bolt", ys(20, 62))
    # Olivine: the harbour fence's gate at its west end.
    add("swap", "pale_oak_fence_gate,pale_oak_fence", (-2323, 594, -2321, 599, "Olivine's harbour fences"), "block/pale_oak_planks=white_fence", ys(17, 22))
    add("swap", "pale_oak_trapdoor", (-2230, 593, -2223, 595, "Olivine's shutters"), "block/pale_oak_trapdoor=pewter_shutter", ys(24, 28))
    # The NATIONAL PARK: the swan's last bits in its marble, the gate's little plaques in the park's stone.
    SW = (-2128, 476, -2114, 487, "the NATIONAL PARK's swan")
    add("swap", "pale_oak_trapdoor,iron_trapdoor,oak_stairs", SW,
        "block/pale_oak_trapdoor=swan_marble,block/iron_trapdoor=swan_marble,block/oak_planks=swan_marble", ys(19, 40))
    add("swap", "pale_oak_trapdoor", (-2125, 468, -2118, 496, "the NATIONAL PARK's gate"), "block/pale_oak_trapdoor=park_stone@2", ys(19, 22))
    # Mahogany: the pale trapdoors by its west gate as Pewter's shutters.
    add("swap", "pale_oak_trapdoor", (-1777, 386, -1775, 393, "Mahogany's shutters"), "block/pale_oak_trapdoor=pewter_shutter", ys(20, 24))
    # Goldenrod: the carved stone bricks over a doorway in the trim's look.
    add("swap", "chiseled_stone_bricks", (-2124, 691, -2118, 693, "Goldenrod City"), "block/chiseled_stone_bricks=house_trim", ys(29, 33))
    # The little docks and benches by the water (spruce slabs and trapdoors): a bench's slats.
    for b in ((-2457, 742, -2451, 745, "a bench by the water"), (-2388, 600, -2382, 603, "a bench by the water")):
        add("swap", "spruce_trapdoor,spruce_slab,spruce_stairs", b, "block/spruce_trapdoor=bench_slat,block/spruce_planks=bench_slat", ys(16, 20))
    # The rocks a scan had missed (on the ground, by trees: a boulder's look), and a cobble block left out of the Lake of
    # Rage's rocks.
    # ((Named "a boulder": the rocks' check drops "a rock" rows over a gate's footprint, and these two stand just by gates.))
    for b, y0, y1 in (((-2257, 554, -2252, 559, "a boulder"), 22, 28), ((-1806, 269, -1799, 275, "a boulder"), 22, 29), ((-1827, 251, -1821, 256, "a boulder"), 22, 29)):
        add("swap", BOULDER_BLOCKS, b, BOULDER, ys(y0, y1))
    # Cianwood: two tuff blocks in its cliff, the cliff's rock.
    add("swap", "tuff", (-2569, 740, -2566, 745, "the cliff's rock"), "block/tuff=drip_rock_top|drip_rock@4", ys(26, 28))
