"""
0.59.4's looks, for tools/arealooks.py: round594() adds the rows (after round59c). John's Kanto fly-round, 07:31 and
07:36 on 2026-11-24 (notes/feedback-2026-11-24-0731.md, -0736.md); the pictures are tools/looks594.py's.
"""


def ys(a, b):
    return (str(a), str(b))


# The campfires (07:31: "fires ... the wood and the flames ... stylizing it to our style"): logs, embers and flames ours.
FIRE = "block/campfire_log=fire_log,block/campfire_log_lit=fire_log_lit,block/campfire_fire=fire_flame"


def round594(add, room, G):
    KANTO, JOHTO = G["KANTO"], G["JOHTO"]

    # ---------- every fire, Kanto and Johto (07:31) ----------
    # (The stands' campfire roofs keep their wooden slats: their own rows are smaller; the ships are as John built them.)
    for big in (KANTO, JOHTO):
        add("swap", "campfire", big[:4] + ("a fire",), FIRE)

    # ---------- the pillars that still showed as stripped logs (07:31, 07:36) ----------
    # Routes 12 and 16's house and the Day Care: their corner posts drawn whole in the pale stone pillar's texture
    # (their look was set as a swap, and still showed the logs in game).
    for x0, z0, x1, z1, f, note in G["CARE_HOUSES"]:
        add("cube", "stripped_spruce_wood,stripped_spruce_log", (x0 + 1, z0 + 1, x1 - 1, z1 - 1, note + "'s pillars"), "pillar_cer", ys(f, f + 3))
    # Viridian's green houses: their front posts the same pillars.
    add("cube", "stripped_spruce_wood,stripped_spruce_log", (-791, 522, -780, 545, "Viridian's green houses' pillars"), "pillar_cer", ys(24, 28))

    # ---------- Diglett's Cave's mound on Route 2 (07:31: "the stripped and probably the mushrooms on the side") ----------
    DIG = (-789, 355, -772, 368, "Diglett's Cave's mound")
    add("swap", "brown_mushroom_block,mushroom_stem,stripped_jungle_wood,stripped_jungle_log,stripped_spruce_wood,stripped_spruce_log", DIG,
        "block/brown_mushroom_block=drip_rock_top|drip_rock@4,block/mushroom_block_inside=drip_rock_top|drip_rock@4,"
        "block/mushroom_stem=drip_rock_top|drip_rock@4,block/stripped_jungle_log=drip_rock_top|drip_rock@4,"
        "block/stripped_jungle_log_top=drip_rock_top|drip_rock@4,block/stripped_spruce_log=drip_rock_top|drip_rock@4,"
        "block/stripped_spruce_log_top=drip_rock_top|drip_rock@4", ys(29, 38))

    # ---------- Pewter's MUSEUM (07:31) ----------
    MU = (-817, 267, -780, 277, "the MUSEUM's front")
    # Its bottom row (quartz bricks) the Museum's plain stone; the brown band (terracotta, and the wall signs on it) a
    # clean wooden band; the shelves in its windows flat.
    add("swap", "quartz_bricks,quartz_block", MU, "block/quartz_bricks=museum_trim,block/quartz_block_side=museum_trim,block/quartz_block_top=museum_trim", ys(29, 31))
    add("swap", "terracotta", MU, "block/terracotta=band_wood", ys(32, 32))
    add("wallboard", "spruce_wall_sign", (-818, 266, -779, 279, "the MUSEUM's band"), "band_wood", ys(31, 34))
    add("panel", "pale_oak_shelf", (-819, 266, -778, 281, "the MUSEUM's window shelves"), "block/pale_oak_shelf=trim_white", ys(33, 47))
    # (Pewter's Gym's door frame, the same mangrove, its beam.)
    add("swap", "stripped_mangrove_log,stripped_mangrove_wood", (-815, 289, -801, 296, "Pewter's Gym's door"),
        "block/stripped_mangrove_log=gym_beam,block/stripped_mangrove_log_top=gym_beam", ys(30, 33))

    # ---------- Cerulean's BIKE SHOP (07:31: "the trim at the bottom ... stone bricks") ----------
    add("swap", "stone_bricks", (-376, 276, -367, 280, "the Bike Shop's foot"), "block/stone_bricks=base_cer", ys(34, 34))
    add("swap", "stripped_oak_wood,stripped_oak_log", (-377, 280, -366, 282, "the Bike Shop's posts"),
        "block/stripped_oak_log=bike_post,block/stripped_oak_log_top=bike_post", ys(34, 37))

    # ---------- Nugget Bridge (07:31: "buttons on the far steps. Those buttons need removing") ----------
    for x in (-355, -350):
        add("hide", "stone_button", (x, 178, x, 178, "a button on Nugget Bridge's steps"), "-", ys(35, 35))

    # ---------- Saffron's FIGHTING DOJO (07:31: "the stripped beam across the second from bottom") ----------
    add("swap", "stripped_mangrove_log,stripped_mangrove_wood", (-343, 388, -330, 400, "the FIGHTING DOJO's beam"),
        "block/stripped_mangrove_log=dojo_beam,block/stripped_mangrove_log_top=dojo_beam", ys(33, 35))

    # ---------- Lavender's floor (07:31: "you can see the lines ... every two") ----------
    # Flagstones that tile (no line every two blocks), in Lavender's two greys.
    LAV = (-158, 398, -104, 447, "Lavender's floor")
    add("swap", "gray_concrete", LAV, "block/gray_concrete=lav_flag2@2", ys(32, 32))
    add("swap", "deepslate_bricks,cracked_deepslate_bricks,deepslate_tiles", LAV,
        "block/deepslate_bricks=lav_flag2_dark@2,block/cracked_deepslate_bricks=lav_flag2_dark@2,block/deepslate_tiles=lav_flag2_dark@2", ys(32, 32))

    # ---------- Route 14's swing bench (07:31: "the hanging signs have, but the rest of the bench hasn't") ----------
    # (Lower than the Cycling Road's: its arch, slats and frame from the ground up.)
    b = (-265, 754, -257, 762, "Route 14's bench under its arch")
    add("swap", "oak_log", b, "block/oak_log=pergola,block/oak_log_top=pergola_top", ys(29, 38))
    add("swap", "spruce_trapdoor", b, "block/spruce_trapdoor=bench_slat", ys(29, 38))
    add("swap", "spruce_fence,spruce_fence_gate", b, "block/spruce_planks=bench_frame", ys(29, 38))

    # ---------- Victory Road's buildings (07:36: "the tops are supposed to be the orange, not the white") ----------
    # (Their rock-looking tops were the scans' rocks, now not; 0.59.4 arealooks57.) The slabs filling their tops the
    # orange brick too.
    # ((0.67, 16:28) Dark gold now, with the band under them and the caps at their ends: arealooks67.)

    # ---------- iron bars and end rods, everywhere (07:36) ----------
    # Iron bars a proper iron fence: bars going up, a rail top and bottom, no bits across (railing_dark itself is drawn
    # so now, so Goldenrod's and Celadon's are too; the ships keep theirs).
    add("swap", "iron_bars", KANTO[:4] + ("iron bars as an iron fence",), "block/iron_bars=railing_dark")
    # The end rods that are lights (Celadon's alley's lamps): a lamp's glowing tube in a metal cap.
    add("swap", "end_rod", KANTO[:4] + ("end rods as lights",), "block/end_rod=rod_light")
    add("swap", "end_rod", (-532, 405, -530, 407, "Celadon's alley's lamps"), "block/end_rod=rod_light", ys(35, 37))
