"""(0.61) John's 18:10 ("Any ladders going down places or up places, I want you to fix so they all are textures") and
18:19 (MT. SILVER): every cave's ladders our metal ones (Diglett's and Mt. Moon's were; now Seafoam's, Rock Tunnel's,
Cerulean Cave's and Victory Road's too), and MT. SILVER textured as Crystal's SILVER CAVE (tools/looks61.py)."""

SILVER = [(17920, 60, 94, "Room 1"), (18176, 84, 92, "Room 2"), (18432, 48, 80, "Room 3"), (18688, 56, 56, "Item Rooms")]
MAT = "block/smooth_sandstone=cave_mat@2,block/smooth_sandstone_top=cave_mat@2,block/sandstone_top=cave_mat@2"


def round61(add, room, G):
    caves = [(6656 + 128 * k, 84, 64, "Seafoam Islands " + fl) for k, fl in enumerate(("1F", "B1F", "B2F", "B3F", "B4F"))]
    caves += [(7296, 112, 94, "Rock Tunnel 1F"), (7424, 110, 96, "Rock Tunnel B1F")]
    caves += [(16384, 104, 72, "Cerulean Cave 1F"), (16640, 92, 68, "Cerulean Cave 2F"), (16896, 104, 70, "Cerulean Cave B1F")]
    caves += [(17152, 72, 70, "Victory Road 1F"), (17408, 126, 68, "Victory Road 2F"), (17664, 112, 68, "Victory Road 3F")]
    caves += [(ox, w, d, "Mt. Silver " + fl) for ox, w, d, fl in SILVER]
    for ox, w, d, note in caves:
        room("swap", "ladder", (ox - 1, -1, ox + w, d, note), "block/ladder=metal_ladder")
    # MT. SILVER: its floor grey-blue speckled stone (its darker patches, its paler raised ground), its walls blue-grey
    # rock in ledges with pale frosted tops, its ledges and steps pale stone, the rock by the water dark and wet, Room
    # 3's snow and frosted rock, its boulders grey-blue.
    MS = ("block/deepslate=ms_floor|ms_cliff@4,block/deepslate_top=ms_floor@4,block/cobbled_deepslate=ms_patch@4,block/tuff=ms_ground|ms_cliff@4,"
          "block/calcite=ms_top|ms_cliff@4,block/andesite=ms_step,block/polished_tuff=ms_step,block/dripstone_block=ms_edge@2,block/jungle_planks=ms_edge,"
          "block/stone=ms_top|ms_cliff_ice@4,block/snow=sf_snow_top|sf_snow@2," + MAT)
    for ox, w, d, fl in SILVER:
        box = (ox - 1, -1, ox + w, d, "Mt. Silver " + fl)
        room("swap", "deepslate,cobbled_deepslate,tuff,calcite,andesite,andesite_slab,polished_tuff,polished_tuff_stairs,dripstone_block,jungle_stairs,stone,stone_stairs,snow_block,smooth_sandstone", box, MS)
        room("boulder", "waxed_copper_grate,waxed_exposed_copper_grate", box, "prefix:xboulder")
