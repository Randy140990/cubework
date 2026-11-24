"""(0.60) John's 16:36: Cerulean Cave and Victory Road (tools/caves48.py, their floors at x 16384 and 17152 on, 256
apart), textured as FireRed's (tools/looks60.py draws the textures and the boulders)."""

CERULEAN = [(16384, 104, 72, "1F"), (16640, 92, 68, "2F"), (16896, 104, 70, "B1F")]
VICTORY = [(17152, 72, 70, "1F"), (17408, 126, 68, "2F"), (17664, 112, 68, "3F")]
MAT = "block/smooth_sandstone=cave_mat@2,block/smooth_sandstone_top=cave_mat@2,block/sandstone_top=cave_mat@2"


def round60(add, room, G):
    # Cerulean Cave: its floor FireRed's pale grey speckled stone (both levels); its cliffs green and mossy (the mossy
    # cobblestone, the moss on top); its steps pale stone; the rock round and under the water red-brown, as FireRed's
    # rim; its rocks (grates: tools/caves48.py's rockify) grey boulders, the cracked ones as Rock Tunnel's.
    CC = ("block/stone=cc_floor@4,block/andesite=cc_floor@4,block/mossy_cobblestone=cc_moss_top|cc_cliff@4,block/moss_block=cc_moss_top|cc_cliff@4,"
          "block/polished_andesite=cc_step,block/jungle_planks=cc_edge,block/dripstone_block=cc_edge@2," + MAT)
    for ox, w, d, fl in CERULEAN:
        box = (ox - 1, -1, ox + w, d, "Cerulean Cave " + fl)
        room("swap", "stone,andesite,mossy_cobblestone,moss_block,polished_andesite,polished_andesite_stairs,jungle_stairs,dripstone_block,smooth_sandstone", box, CC)
        room("boulder", "waxed_copper_grate,waxed_exposed_copper_grate", box, "prefix:cboulder")
        room("boulder", "waxed_weathered_copper_grate", box, "prefix:sboulder")
    # Victory Road: its floor FireRed's olive-grey earth (the raised ground's edge the rock below it), its sand yellow,
    # its walls red-brown rock in ledges with the mountain's pale rock on top, its steps cut in the rock, the switches
    # round plates in the floor, its rocks and STRENGTH boulders warm granite boulders.
    VR = ("block/andesite=vr_floor|vr_cliff@4,block/sand=vr_sand@2,block/dripstone_block=vr_top|vr_cliff@4,block/white_terracotta=vr_top@2,"
          "block/spruce_planks=vr_step,block/quartz_block_bottom=vr_switch@2," + MAT)
    for ox, w, d, fl in VICTORY:
        box = (ox - 1, -1, ox + w, d, "Victory Road " + fl)
        room("swap", "andesite,sand,dripstone_block,white_terracotta,spruce_planks,spruce_stairs,smooth_quartz,smooth_sandstone", box, VR)
        room("boulder", "waxed_copper_grate,waxed_exposed_copper_grate", box, "prefix:vboulder")
