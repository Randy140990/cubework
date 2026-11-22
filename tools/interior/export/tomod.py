#!/usr/bin/env python3
"""Turns the Interior Viewer's rooms (export.json, from export.js) into the mod's interiors.

Every face in a room is cut at block edges (and where its texture repeats), and each block's pieces become one
block model (retromon:prop, one state per model and collision shape). Collision comes from the room's solids
(furniture and walls, 1.5 high), its floor and its stairs. Writes:
  assets/retromon/textures/block/interior/*.png   (the viewer's pixel textures, padded to powers of two)
  assets/retromon/models/block/prop/*.json, assets/retromon/blockstates/prop.json
  retromon/interiors/rooms.json                    (each room's blocks, people, mats, stairs, things to look at)

    python3 tools/interior/export/tomod.py export.json src/main/resources
"""
import base64, io, json, math, os, re, sys
from PIL import Image

EPS = 1e-6
SKIP = {"aoN", "aoW", "aoE", "deep", "shadowBlob", "poolWaterView", "poolBottomView", "poolQuartzView", "surgeBarrierView"}
# Rooms with real blocks of their own laid after their props (Cerulean Gym's pool: tools/cerulean_pool.py), and from how far down.
SCHEM = {"cerulean_gym": ("cerulean_gym_pool.bin", -6)}
ORIGIN = {"house1": (0, 64, 0), "house2": (64, 64, 0), "rival1": (128, 64, 0), "rival2": (192, 64, 0), "lab": (256, 64, 0), "center": (320, 64, 0),
          "mart": (384, 64, 0), "school": (448, 64, 0), "trainer2": (512, 64, 0), "gate_s": (576, 64, 0), "nickhouse": (640, 64, 0), "gate_n": (704, 64, 0),
          "gate_league": (768, 64, 0), "gate_johto": (832, 64, 0), "route2_gate": (896, 64, 0), "route2_house": (960, 64, 0),
          "pewter_gym": (1024, 64, 0), "museum_1f": (1088, 64, 0), "museum_2f": (1152, 64, 0), "pewter_house1": (1216, 64, 0), "pewter_house2": (1280, 64, 0),
          # Fuchsia City and the SAFARI ZONE's houses (15:29 to 15:33).
          "fuchsia_gym": (1344, 64, 0), "safari_gate": (1408, 64, 0), "fuchsia_office": (1472, 64, 0), "fuchsia_house1": (1536, 64, 0),
          "fuchsia_house2": (1600, 64, 0), "fuchsia_house3": (1664, 64, 0), "fuchsia_house4": (1728, 64, 0), "safari_rest_center": (1792, 64, 0),
          "safari_rest_east": (1856, 64, 0), "safari_rest_north": (1920, 64, 0), "safari_rest_west": (1984, 64, 0), "safari_secret_house": (2048, 64, 0),
          # The Underground Paths, Cerulean City's insides, the Day Care and Bill's Sea Cottage (16:17 and 16:20).
          "ug_r5": (2112, 64, 0), "ug_r6": (2176, 64, 0), "ug_r7": (2240, 64, 0), "ug_r8": (2304, 64, 0), "ug_ns": (2368, 64, 0), "ug_ew": (2432, 64, 0),
          "cerulean_gym": (2560, 64, 0), "bike_shop": (2624, 64, 0), "cerulean_house1": (2688, 64, 0), "cerulean_house2": (2752, 64, 0),
          "cerulean_house3": (2816, 64, 0), "cerulean_house4": (2880, 64, 0), "daycare": (2944, 64, 0), "bills_house": (3008, 64, 0),
          "power_plant": (3072, 64, 0),
          # Vermilion City (17:51).
          "vermilion_gym": (3200, 64, 0), "pokemon_fan_club": (3264, 64, 0), "vermilion_old_rod_house": (3328, 64, 0),
          "vermilion_trade_house": (3392, 64, 0), "vermilion_pidgey_house": (3456, 64, 0)}
PLACE = {"house1": "pallet_town", "house2": "pallet_town", "rival1": "pallet_town", "rival2": "pallet_town", "lab": "pallet_town", "center": "viridian_city",
         "mart": "viridian_city", "school": "viridian_city", "trainer2": "viridian_city", "nickhouse": "viridian_city", "gate_s": "route_2", "gate_n": "route_2",
         "gate_league": "route_22", "gate_johto": "route_22", "route2_gate": "route_2_gate", "route2_house": "route_2_trade_house",
         "pewter_gym": "pewter_gym", "museum_1f": "museum_1f", "museum_2f": "museum_2f", "pewter_house1": "pewter_nidoran_house", "pewter_house2": "pewter_speech_house",
         "fuchsia_gym": "fuchsia_gym", "safari_gate": "safari_zone_gate", "fuchsia_office": "fuchsia_meeting_room", "fuchsia_house1": "fuchsia_bills_grandpas_house",
         "fuchsia_house2": "fuchsia_move_deleter_house", "fuchsia_house3": "wardens_house", "fuchsia_house4": "fuchsia_good_rod_house",
         "safari_rest_center": "safari_zone_center_rest_house", "safari_rest_east": "safari_zone_east_rest_house", "safari_rest_north": "safari_zone_north_rest_house",
         "safari_rest_west": "safari_zone_west_rest_house", "safari_secret_house": "safari_zone_secret_house",
         "ug_r5": "underground_path_route_5", "ug_r6": "underground_path_route_6", "ug_r7": "underground_path_route_7", "ug_r8": "underground_path_route_8",
         "ug_ns": "underground_path_ns", "ug_ew": "underground_path_we", "cerulean_gym": "cerulean_gym", "bike_shop": "bike_shop",
         "cerulean_house1": "cerulean_badge_house", "cerulean_house2": "cerulean_trashed_house", "cerulean_house3": "cerulean_trade_house",
         "cerulean_house4": "cerulean_berry_house", "daycare": "daycare", "bills_house": "bills_house", "power_plant": "power_plant",
         "vermilion_gym": "vermilion_gym", "pokemon_fan_club": "pokemon_fan_club", "vermilion_old_rod_house": "vermilion_old_rod_house",
         "vermilion_trade_house": "vermilion_trade_house", "vermilion_pidgey_house": "vermilion_pidgey_house"}
# The doors in John's world (Pallet Town: the lower halves of each double door; outside is to the south).
DOORS = {
    "home": {"room": "house1", "door": [[-803, 19, 679], [-802, 19, 679]], "out": [-802.0, 19.0, 680.6]},
    "rival": {"room": "rival1", "door": [[-785, 19, 679], [-784, 19, 679]], "out": [-784.0, 19.0, 680.6]},
    "lab": {"room": "lab", "door": [[-783, 19, 694], [-782, 19, 694]], "out": [-782.0, 19.0, 695.6]},
    # Viridian City: the Pokémon Center (red roof) and the Poké Mart (blue roof), their open doorways 3 wide.
    "viridian_center": {"room": "center", "door": [[-787, 26, 557], [-786, 26, 557], [-785, 26, 557]], "out": [-785.5, 26.0, 558.6]},
    "viridian_mart": {"room": "mart", "door": [[-767, 26, 543], [-766, 26, 543], [-765, 26, 543]], "out": [-765.5, 26.0, 544.6]},
    # Viridian's two green-roofed buildings (warped doors, 2 wide, set back under a porch): the Trainer School just
    # above the Center (the Trainer House's battle room under it), and the house above that.
    "viridian_school": {"room": "school", "door": [[-788, 26, 542], [-787, 26, 542]], "out": [-787.0, 26.0, 543.8]},
    "viridian_house": {"room": "nickhouse", "door": [[-788, 26, 528], [-787, 26, 528]], "out": [-787.0, 26.0, 529.8]},
    # Route 2's gates into Viridian Forest: the south one's way in (its south side), the north one's (its north side,
    # so you come out facing north).
    # (The dark backs of the forest gatehouses' doorways, at the top of their steps: south at z 443, north at z 377.)
    "forest_south": {"room": "gate_s", "door": [[x, 29, 443] for x in range(-804, -800)], "out": [-802.5, 27.0, 449.8]},
    "forest_north": {"room": "gate_n", "door": [[x, 32, 377] for x in range(-804, -800)], "out": [-802.5, 30.0, 370.2], "face": "n", "yaw": 180.0},
    # Route 22's POKéMON LEAGUE gate (its glass doors, 4 wide): in from ROUTE 22 at the south, on to ROUTE 23 at
    # the north (the guard lets only BOULDERBADGE holders through), coming back from there into the north nook.
    "league_s": {"room": "gate_league", "door": [[x, 27, 534] for x in range(-919, -915)], "out": [-917.5, 27.0, 536.6]},
    "league_n": {"room": "gate_league", "door": [[x, 27, 514] for x in range(-919, -915)], "out": [-917.5, 27.0, 512.2], "face": "n", "yaw": 180.0,
                 "in": {"x": 7.0, "z": 1.4, "yaw": 0.0}},
    # The Route 28 Gate: in at its east door off ROUTE 22 (a sideways gate); its far side comes out of the gate house
    # on the far side of the mountain, at ROUTE 28 (facing west), or (for the CHAMPION) at ROUTE 26's gate (facing
    # south). Coming back in either of those, you're at its west side.
    "johto_e": {"room": "gate_johto", "door": [[-951, 28, z] for z in range(540, 544)], "out": [-946.6, 26.0, 541.5], "face": "e", "yaw": -90.0},
    "route28": {"room": "gate_johto", "door": [[-1039, 28, z] for z in range(540, 544)], "out": [-1042.6, 26.0, 541.5], "face": "w", "yaw": 90.0,
                "in": {"x": 1.6, "z": 5.0, "yaw": -90.0}},
    "route26": {"room": "gate_johto", "door": [[x, 27, 586] for x in range(-993, -987)], "out": [-990.5, 26.0, 589.6], "yaw": 0.0,
                "in": {"x": 1.6, "z": 5.0, "yaw": -90.0}},
    # Route 2's east lane (past the CUT trees): its gate (in at its south door, the dark back of its doorway at z 434;
    # out at the north, back of that doorway at z 431), and the trade house north of it (a bamboo door, 2 wide).
    "route2_gate_s": {"room": "route2_gate", "door": [[-777, 30, 434], [-776, 30, 434]], "out": [-776.5, 29.0, 439.4]},
    "route2_gate_n": {"room": "route2_gate", "door": [[-777, 30, 431], [-776, 30, 431]], "out": [-776.5, 29.0, 426.6], "face": "n", "yaw": 180.0,
                      "in": {"x": 7.0, "z": 1.4, "yaw": 0.0}},
    "route2_house": {"room": "route2_house", "door": [[-783, 31, 387], [-782, 31, 387]], "out": [-782.0, 30.0, 390.4]},
    # Pewter City: the Gym (its glass doors, 3 wide), the Museum (its main doorway between the spruce shelves, into the
    # fossils' side, and its side door, a mangrove door, into the OLD AMBER's side), and its two houses (bamboo doors).
    "pewter_gym": {"room": "pewter_gym", "door": [[x, 31, 295] for x in range(-809, -806)], "out": [-807.5, 31.0, 298.0]},
    "pewter_museum": {"room": "museum_1f", "door": [[-803, 31, 278], [-802, 31, 278]], "out": [-802.0, 31.0, 280.4], "in": {"x": 14.5, "z": 9.4, "yaw": 180.0}},
    "pewter_museum_side": {"room": "museum_1f", "door": [[-785, 32, 273], [-784, 32, 273]], "out": [-784.0, 32.0, 275.4], "in": {"x": 22.5, "z": 9.4, "yaw": 180.0}},
    "pewter_house1": {"room": "pewter_house1", "door": [[-769, 31, 286], [-768, 31, 286]], "out": [-768.0, 31.0, 288.4]},
    "pewter_house2": {"room": "pewter_house2", "door": [[-818, 31, 324], [-817, 31, 324]], "out": [-817.0, 31.0, 326.4]},
    # Fuchsia City (15:29 to 15:33): the Gym (its two glass doors either side of a post), the SAFARI ZONE's gate (its
    # glass doors, 4 wide) and office (a warped door), and its four houses (dark oak doors).
    "fuchsia_gym": {"room": "fuchsia_gym", "door": [[x, 32, 809] for x in range(-531, -528)], "out": [-528.5, 31.0, 812.6]},
    "fuchsia_safari": {"room": "safari_gate", "door": [[x, 31, 754] for x in range(-488, -484)], "out": [-486.0, 31.0, 757.4]},
    "fuchsia_office": {"room": "fuchsia_office", "door": [[-463, 31, 761], [-462, 31, 761]], "out": [-462.0, 31.0, 763.4]},
    "fuchsia_house1": {"room": "fuchsia_house1", "door": [[-519, 32, 809], [-518, 32, 809]], "out": [-518.0, 32.0, 811.4]},
    "fuchsia_house2": {"room": "fuchsia_house2", "door": [[-508, 32, 809], [-507, 32, 809]], "out": [-507.0, 32.0, 811.4]},
    "fuchsia_house3": {"room": "fuchsia_house3", "door": [[-478, 31, 808], [-477, 31, 808]], "out": [-477.0, 31.0, 810.4]},
    "fuchsia_house4": {"room": "fuchsia_house4", "door": [[-468, 31, 808], [-467, 31, 808]], "out": [-467.0, 31.0, 810.4]},
    # The Underground Paths (16:20): John's four huts, each a dark doorway 4 wide facing south (the black at its front).
    "ug_route5": {"room": "ug_r5", "door": [[x, 33, 366] for x in range(-339, -335)], "out": [-337.5, 33.0, 368.4]},
    "ug_route6": {"room": "ug_r6", "door": [[x, 31, 490] for x in range(-334, -330)], "out": [-332.5, 31.0, 492.4]},
    "ug_route7": {"room": "ug_r7", "door": [[x, 32, 430] for x in range(-434, -430)], "out": [-432.5, 32.0, 432.4]},
    "ug_route8": {"room": "ug_r8", "door": [[x, 33, 411] for x in range(-274, -270)], "out": [-272.5, 33.0, 413.4]},
    # Cerulean City (16:17): the Gym (its glass doors either side of a post, up its quartz steps), the Bike Shop (a
    # warped door), its four houses (copper doors), and Route 5's Day Care (mangrove) and Route 25's Sea Cottage (birch).
    "cerulean_gym": {"room": "cerulean_gym", "door": [[x, 35, 265] for x in range(-338, -335)], "out": [-338.5, 35.0, 267.5]},
    "bike_shop": {"room": "bike_shop", "door": [[-374, 35, 279], [-373, 35, 279]], "out": [-373.0, 35.0, 281.4]},
    "cerulean_house1": {"room": "cerulean_house1", "door": [[-381, 35, 246], [-380, 35, 246]], "out": [-380.0, 35.0, 248.4]},
    "cerulean_house2": {"room": "cerulean_house2", "door": [[-338, 35, 246], [-337, 35, 246]], "out": [-337.0, 35.0, 248.4]},
    # (17:42) The two top houses' back ways (FireRed's): in from the path behind (walking south into the back wall),
    # out onto it (facing north).
    "cerulean_house1_back": {"room": "cerulean_house1", "door": [[-381, 34, 242], [-380, 34, 242]], "out": [-380.0, 34.0, 240.2], "face": "n", "yaw": 180.0,
                             "in": {"x": 10.3, "z": 1.6, "yaw": 0.0}},
    "cerulean_house2_back": {"room": "cerulean_house2", "door": [[-334, 34, 242], [-333, 34, 242]], "out": [-333.0, 34.0, 240.2], "face": "n", "yaw": 180.0,
                             "in": {"x": 10.3, "z": 1.6, "yaw": 0.0}},
    "cerulean_house3": {"room": "cerulean_house3", "door": [[-375, 35, 261], [-374, 35, 261]], "out": [-374.0, 35.0, 263.4]},
    "cerulean_house4": {"room": "cerulean_house4", "door": [[-358, 35, 279], [-357, 35, 279]], "out": [-357.0, 35.0, 281.4]},
    "daycare": {"room": "daycare", "door": [[-354, 38, 331], [-353, 38, 331]], "out": [-353.0, 38.0, 333.4]},
    "bills_house": {"room": "bills_house", "door": [[-218, 36, 151], [-217, 36, 151]], "out": [-217.0, 36.0, 153.4]},
    # The POWER PLANT on Route 10 (16:58): its dark doorway, 3 wide, at the top of its smooth stone steps.
    "power_plant": {"room": "power_plant", "door": [[-142, 35, 325], [-141, 35, 325], [-140, 35, 325]], "out": [-140.5, 35.0, 328.5]},
    # Vermilion City (17:51): the Gym (its glass doors either side of a post), the FAN CLUB (a mangrove door), and its
    # three houses (warped doors): the FISHING GURU's top left, the trade house, the PIDGEY letter house by the harbor.
    "vermilion_gym": {"room": "vermilion_gym", "door": [[x, 32, 593] for x in range(-372, -369)], "out": [-370.5, 32.0, 595.6]},
    "pokemon_fan_club": {"room": "pokemon_fan_club", "door": [[-375, 32, 578], [-374, 32, 578]], "out": [-374.0, 32.0, 580.4]},
    "vermilion_old_rod_house": {"room": "vermilion_old_rod_house", "door": [[-382, 32, 556], [-381, 32, 556]], "out": [-381.0, 32.0, 558.4]},
    "vermilion_trade_house": {"room": "vermilion_trade_house", "door": [[-361, 32, 578], [-360, 32, 578]], "out": [-360.0, 32.0, 580.4]},
    "vermilion_pidgey_house": {"room": "vermilion_pidgey_house", "door": [[-344, 32, 590], [-343, 32, 590]], "out": [-343.0, 32.0, 592.4]},
}
# Every other Pokémon Center and Poké Mart on the map (shops.tsv): the same insides, each its own way back out. A shared
# door's "group" says which: going out of the room, you come out of the door you went in by (Interiors.doorOut).
DOORS["viridian_center"].update({"place": "viridian_city", "group": "center"})
DOORS["viridian_mart"].update({"place": "viridian_city", "group": "mart"})
for _line in open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "shops.tsv")):
    if _line.startswith("#") or not _line.strip(): continue
    _kind, _place, _bx, _by, _bz = _line.rstrip("\n").split("\t")
    _bx, _by, _bz = int(_bx), int(_by), int(_bz)
    if _place == "viridian_city": continue
    DOORS["%s_%s" % (_place, _kind)] = {"room": _kind, "door": [[_bx + i, _by - 5, _bz - 3] for i in (-1, 0, 1)], "out": [_bx + 0.5, float(_by - 5), _bz - 1.4],
                                        "place": _place, "group": _kind}
MAT_DOOR = {"house1": "home", "rival1": "rival", "lab": "lab", "center": "viridian_center", "mart": "viridian_mart", "school": "viridian_school", "nickhouse": "viridian_house", "gate_s": "forest_south",
            "gate_league": "league_s", "route2_gate": "route2_gate_s", "route2_house": "route2_house", "pewter_gym": "pewter_gym",
            "pewter_house1": "pewter_house1", "pewter_house2": "pewter_house2", "fuchsia_gym": "fuchsia_gym", "safari_gate": "fuchsia_safari",
            "fuchsia_office": "fuchsia_office", "fuchsia_house1": "fuchsia_house1", "fuchsia_house2": "fuchsia_house2", "fuchsia_house3": "fuchsia_house3",
            "fuchsia_house4": "fuchsia_house4", "ug_r5": "ug_route5", "ug_r6": "ug_route6", "ug_r7": "ug_route7", "ug_r8": "ug_route8",
            "cerulean_gym": "cerulean_gym", "bike_shop": "bike_shop", "cerulean_house1": "cerulean_house1", "cerulean_house2": "cerulean_house2",
            "cerulean_house3": "cerulean_house3", "cerulean_house4": "cerulean_house4", "daycare": "daycare", "bills_house": "bills_house", "power_plant": "power_plant",
            "vermilion_gym": "vermilion_gym", "pokemon_fan_club": "pokemon_fan_club", "vermilion_old_rod_house": "vermilion_old_rod_house",
            "vermilion_trade_house": "vermilion_trade_house", "vermilion_pidgey_house": "vermilion_pidgey_house"}
# Rooms with two doors out (the Museum): which door each mat is, by the mat's west edge.
MAT_DOOR_X = {("museum_1f", 13): "pewter_museum", ("museum_1f", 21): "pewter_museum_side"}
# Rooms laid out step for step on the games' (the Pewter Gym): how many blocks one of its steps is, across and down,
# so its trainers see straight ahead as many steps as theirs.
TILE = {"pewter_gym": [1.0, 1.0], "fuchsia_gym": [1.0, 1.0], "cerulean_gym": [1.0, 1.0]}
# Item balls in a room of the viewer's (the WARDEN's RARE CANDY, behind his boulders: 15:33).
BALLS = {"fuchsia_house3": [{"key": "wardens_house_rare_candy", "x": 0.8, "z": 8.4, "y": 0}]}
# The Underground Paths' items (16:20), as John listed them: north to south (the first four come back each day), and
# west to east, each by a wall.
_NS = ["antidote_again", "parlyz_heal_again", "awakening_again", "potion_again", "ether", "ice_heal", "burn_heal"]
_WE = ["potion", "parlyz_heal", "awakening", "burn_heal", "ether", "antidote", "ice_heal"]
BALLS["ug_ns"] = [{"key": "underground_path_ns_" + k, "x": 0.9 if i % 2 == 0 else 6.1, "z": 7.0 + 7.0 * i, "y": 0} for i, k in enumerate(_NS)]
# The Power Plant's items (Red and Blue's, a step a block), and FireRed's THUNDER STONE and ELIXIR.
BALLS["power_plant"] = [{"key": k, "x": x, "z": z, "y": 0} for k, x, z in (
    ("power_plant_i8", 7.5, 25.5), ("power_plant_i9", 28.5, 3.5), ("power_plant_i10", 34.5, 3.5), ("power_plant_i11", 26.5, 32.5),
    ("power_plant_i12", 20.5, 32.5), ("power_plant_thunderstone", 12.5, 3.5), ("power_plant_elixer", 36.5, 11.5))]
# POKéMON standing in rooms (statics.tsv's): ZAPDOS by the big machine; VOLTORB and ELECTRODE posing as item balls.
MONS_DAYCARE = [{"key": "pet:daycare_clefairy", "species": "clefairy", "x": 8.0, "z": 4.0, "yaw": 210.0},
                {"key": "pet:daycare_psyduck", "species": "psyduck", "x": 11.6, "z": 4.6, "yaw": 150.0}]
MONS = {"power_plant": [{"key": "zapdos", "species": "zapdos", "x": 5.6, "z": 4.2, "yaw": 0.0}] + [
    {"key": k, "species": "poke_ball", "x": x, "z": z, "yaw": 0.0} for k, x, z in (
        ("power_plant_voltorb_1", 9.5, 20.5), ("power_plant_voltorb_2", 32.5, 18.5), ("power_plant_voltorb_3", 21.5, 25.5),
        ("electrode_power_plant_1", 25.5, 18.5), ("power_plant_voltorb_4", 23.5, 32.5), ("power_plant_voltorb_5", 26.5, 26.0),
        ("electrode_power_plant_2", 21.5, 12.0), ("power_plant_voltorb_6", 37.5, 32.5))]}
MONS["daycare"] = MONS_DAYCARE
MONS["pokemon_fan_club"] = [{"key": "pet:pokemon_fan_club_p2", "species": "clefairy", "x": 10.4, "z": 8.6, "yaw": 90.0},
                            {"key": "pet:pokemon_fan_club_p3", "species": "seel", "x": 3.6, "z": 8.6, "yaw": 270.0}]
MONS["vermilion_pidgey_house"] = [{"key": "pet:vermilion_pidgey_house_p1", "species": "pidgey", "x": 8.2, "z": 5.6, "yaw": 270.0}]
BALLS["ug_ew"] = [{"key": "underground_path_we_" + k, "x": 9.0 + 7.0 * i, "z": 6.1 if i % 2 == 0 else 0.9, "y": 0} for i, k in enumerate(_WE)]
# Ways out through the north wall to the world (the north gate's).
NORTH_DOOR = {"gate_n": "forest_north", "gate_league": "league_n", "route2_gate": "route2_gate_n", "cerulean_house1": "cerulean_house1_back",
              "cerulean_house2": "cerulean_house2_back"}
# Ways out through the side walls (a sideways gate's), by room and wall.
SIDE_DOOR = {("gate_johto", "e"): "johto_e"}
# Where you come in from outside, and where a new game starts (your room, by the bed).
# (On the mat's inner half: the mats are two deep, and you're out once you're past their middle.)
ENTRY = {"house1": {"x": 7.0, "z": 11.5, "yaw": 180.0}, "rival1": {"x": 7.0, "z": 11.5, "yaw": 180.0}, "lab": {"x": 8.0, "z": 14.5, "yaw": 180.0},
         "center": {"x": 9.0, "z": 12.5, "yaw": 180.0}, "mart": {"x": 7.0, "z": 10.5, "yaw": 180.0}, "school": {"x": 6.0, "z": 9.5, "yaw": 180.0},
         "nickhouse": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "gate_s": {"x": 7.0, "z": 12.5, "yaw": 180.0}, "gate_n": {"x": 7.0, "z": 1.4, "yaw": 0.0},
         "gate_league": {"x": 7.0, "z": 14.5, "yaw": 180.0}, "gate_johto": {"x": 14.4, "z": 5.0, "yaw": 90.0},
         "route2_gate": {"x": 7.0, "z": 12.5, "yaw": 180.0}, "route2_house": {"x": 6.0, "z": 9.5, "yaw": 180.0},
         "pewter_gym": {"x": 7.0, "z": 16.5, "yaw": 180.0}, "museum_1f": {"x": 14.5, "z": 9.4, "yaw": 180.0}, "museum_2f": {"x": 12.5, "z": 9.9, "yaw": 0.0},
         "pewter_house1": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "pewter_house2": {"x": 6.0, "z": 9.5, "yaw": 180.0},
         "fuchsia_gym": {"x": 6.0, "z": 15.5, "yaw": 180.0}, "safari_gate": {"x": 7.0, "z": 12.5, "yaw": 180.0}, "fuchsia_office": {"x": 7.0, "z": 9.5, "yaw": 180.0},
         "fuchsia_house1": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "fuchsia_house2": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "fuchsia_house3": {"x": 6.0, "z": 9.5, "yaw": 180.0},
         "fuchsia_house4": {"x": 6.0, "z": 9.5, "yaw": 180.0},
         "ug_r5": {"x": 7.0, "z": 10.5, "yaw": 180.0}, "ug_r6": {"x": 7.0, "z": 10.5, "yaw": 180.0}, "ug_r7": {"x": 7.0, "z": 10.5, "yaw": 180.0},
         "ug_r8": {"x": 7.0, "z": 10.5, "yaw": 180.0}, "cerulean_gym": {"x": 7.0, "z": 17.5, "yaw": 180.0}, "bike_shop": {"x": 7.0, "z": 9.5, "yaw": 180.0},
         "cerulean_house1": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "cerulean_house2": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "cerulean_house3": {"x": 6.0, "z": 9.5, "yaw": 180.0},
         "cerulean_house4": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "daycare": {"x": 7.0, "z": 9.5, "yaw": 180.0}, "bills_house": {"x": 7.0, "z": 9.5, "yaw": 180.0},
         "power_plant": {"x": 5.0, "z": 34.5, "yaw": 180.0},
         "vermilion_gym": {"x": 7.0, "z": 20.5, "yaw": 180.0}, "pokemon_fan_club": {"x": 7.0, "z": 10.5, "yaw": 180.0},
         "vermilion_old_rod_house": {"x": 6.0, "z": 9.5, "yaw": 180.0}, "vermilion_trade_house": {"x": 6.0, "z": 9.5, "yaw": 180.0},
         "vermilion_pidgey_house": {"x": 6.0, "z": 9.5, "yaw": 180.0}}
START = {"room": "house2", "x": 3.6, "z": 6.4, "yaw": 0.0}
# People: who they are in the mod (a key from the games' people where there is one, else our own).
PEOPLE = {
    ("lab", "Prof. Oak"): "oaks_lab_p0",
}
AIDES_FROM_GAMES = ["oaks_lab_p3", "oaks_lab_p4"]


def snake(k):
    return re.sub(r"[^a-z0-9_]", "_", re.sub(r"(?<!^)(?=[A-Z])", "_", k).lower())


def pow2(n):
    p = 1
    while p < n: p *= 2
    return p


def img_of(uri):
    return Image.open(io.BytesIO(base64.b64decode(uri.split(",", 1)[1]))).convert("RGBA")


def glassy(im):
    """A see-through material as Minecraft's cutout draws it: the clear body gone, its streaks and edges kept."""
    px = im.load(); w, h = im.size
    cols = sorted(px[x, y][:3] for y in range(h) for x in range(w))
    base = cols[len(cols) // 2]
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            d = abs(r - base[0]) + abs(g - base[1]) + abs(b - base[2])
            px[x, y] = (r, g, b, 255 if d > 24 else 0)
    return im


def write_textures(mats, res):
    out = os.path.join(res, "assets/retromon/textures/block/interior")
    os.makedirs(out, exist_ok=True)
    for f in os.listdir(out): os.remove(os.path.join(out, f))
    info = {}
    for k, m in mats.items():
        if k in SKIP or m.get("overlay"): continue
        name = snake(k)
        if m.get("frames"):
            frames = [img_of(u) for u in m["frames"]]
            fw, fh = frames[0].size; W, H = pow2(fw), pow2(fh)
            strip = Image.new("RGBA", (W, H * len(frames)), (0, 0, 0, 0))
            for i, fr in enumerate(frames): strip.paste(fr, (0, i * H))
            strip.save(os.path.join(out, name + ".png"))
            json.dump({"animation": {"frametime": 3, "width": W, "height": H}}, open(os.path.join(out, name + ".png.mcmeta"), "w"))
            info[k] = {"name": name, "tw": fw, "th": fh, "W": W, "H": H, "fit": True, "per": [1, 1]}
            continue
        im = img_of(m["png"])
        if m.get("alpha"): im = glassy(im)
        tw, th = im.size; W, H = pow2(tw), pow2(th)
        pad = Image.new("RGBA", (W, H), (0, 0, 0, 0)); pad.paste(im, (0, 0))
        if not m.get("cut") and not m.get("alpha"):
            # Opaque materials: fully opaque, so cutout keeps every pixel.
            p = pad.load()
            for y in range(th):
                for x in range(tw):
                    r, g, b, a = p[x, y]; p[x, y] = (r, g, b, 255)
        pad.save(os.path.join(out, name + ".png"))
        per = m.get("per", 1)
        info[k] = {"name": name, "tw": tw, "th": th, "W": W, "H": H, "fit": m.get("fit", False), "per": per if isinstance(per, list) else [per, per]}
    return info


AX = {(1, 0, 0): "east", (-1, 0, 0): "west", (0, 1, 0): "up", (0, -1, 0): "down", (0, 0, 1): "south", (0, 0, -1): "north"}


def axis_of(v):
    for i in range(3):
        if abs(v[i]) > 0.5: return i, (1 if v[i] > 0 else -1)
    raise ValueError(v)


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def cuts(a, b, extra):
    pts = {a, b}
    for i in range(math.floor(a) + 1, math.ceil(b)):
        if a + EPS < i < b - EPS: pts.add(float(i))
    for e in extra:
        if a + EPS < e < b - EPS: pts.add(e)
    return sorted(pts)


def slice_face(f, tex, cells):
    """Cuts one face into pieces, each inside one block and one repeat of its texture, as model elements."""
    k = f["k"]; t = tex[k]
    bl, R, U, w, h = f["bl"], f["r"], f["u"], f["w"], f["h"]
    n = cross(R, U); an, sn = axis_of(n); ar, sr = axis_of(R); au, su = axis_of(U)
    plane = bl[an]
    cn = math.floor(plane + sn * 1e-4)
    pu, pv = t["per"]
    u0 = sum(bl[i] * R[i] for i in range(3)) / pu
    v0 = sum(bl[i] * U[i] for i in range(3)) / pv
    # Cut along R (t from 0 to w) at block edges and texture repeats; same along U.
    def block_cuts(start, sign, length):
        out = []
        for i in range(math.floor(min(start, start + sign * length)) + 1, math.ceil(max(start, start + sign * length))):
            out.append((i - start) * sign)
        return out
    ext_r = block_cuts(bl[ar], sr, w)
    ext_u = block_cuts(bl[au], su, h)
    if not t["fit"]:
        ext_r += [(j - u0) * pu for j in range(math.floor(u0) + 1, math.ceil(u0 + w / pu))]
        ext_u += [(j - v0) * pv for j in range(math.floor(v0) + 1, math.ceil(v0 + h / pv))]
    tr = cuts(0.0, w, ext_r); tu = cuts(0.0, h, ext_u)
    face = AX[tuple(int(round(x)) for x in n)]
    for i in range(len(tr) - 1):
        for j in range(len(tu) - 1):
            a0, a1, b0, b1 = tr[i], tr[i + 1], tu[j], tu[j + 1]
            if a1 - a0 < 1e-4 or b1 - b0 < 1e-4: continue
            # World extent of the piece.
            ra, rb = bl[ar] + sr * a0, bl[ar] + sr * a1
            ua, ub = bl[au] + su * b0, bl[au] + su * b1
            lo = [0.0, 0.0, 0.0]; hi = [0.0, 0.0, 0.0]
            lo[ar], hi[ar] = min(ra, rb), max(ra, rb)
            lo[au], hi[au] = min(ua, ub), max(ua, ub)
            lo[an] = hi[an] = plane
            cell = [0, 0, 0]
            cell[ar] = math.floor((lo[ar] + hi[ar]) / 2); cell[au] = math.floor((lo[au] + hi[au]) / 2); cell[an] = cn
            frm = [round((lo[q] - cell[q]) * 16, 3) for q in range(3)]
            to = [round((hi[q] - cell[q]) * 16, 3) for q in range(3)]
            # Texture: the piece's place in its repeat, as 0..16 over the padded picture (v down from the top).
            if t["fit"]:
                # (A piece of a face whose picture is stretched over it, the part of the picture it shows: "sub".)
                s0, t0, s1, t1 = f.get("sub") or (0.0, 0.0, 1.0, 1.0)
                us, ue = s0 + (s1 - s0) * a0 / w, s0 + (s1 - s0) * a1 / w
                vs, ve = t0 + (t1 - t0) * b0 / h, t0 + (t1 - t0) * b1 / h
            else:
                us, ue = u0 + a0 / pu, u0 + a1 / pu
                vs, ve = v0 + b0 / pv, v0 + b1 / pv
                ku, kv = math.floor(min(us, ue) + 1e-6), math.floor(min(vs, ve) + 1e-6)
                us, ue, vs, ve = us - ku, ue - ku, vs - kv, ve - kv
            sx, sy = t["tw"] / t["W"] * 16, t["th"] / t["H"] * 16
            uv = [round(us * sx, 4), round((1 - ve) * sy, 4), round(ue * sx, 4), round((1 - vs) * sy, 4)]
            cells.setdefault(tuple(cell), []).append({"from": frm, "to": to, "faces": {face: {"uv": uv, "texture": "#" + t["name"]}}})


def collision(room):
    """Each block's collision boxes (0..16, up to 24 high): the floor, the furniture and walls, the stairs."""
    W, D = room["W"], room["D"]
    shapes = {}
    def add(cell, box):
        shapes.setdefault(cell, []).append([round(v, 3) for v in box])
    ramps = room["ramps"]
    def in_ramp(x, z):
        for rp in ramps:
            x0, z0, x1, z1 = rp["rect"]
            if x0 - EPS <= x <= x1 + EPS and z0 - EPS <= z <= z1 + EPS: return True
        return False
    # The floor: a full block under every floor square (not the stairwell).
    for (x0, z0, x1, z1) in room["floors"]:
        for ix in range(math.floor(x0), math.ceil(x1)):
            for iz in range(math.floor(z0), math.ceil(z1)):
                if not in_ramp(ix + 0.5, iz + 0.5): add((ix, -1, iz), [0, 0, 0, 16, 16, 16])
    # Furniture and walls: their rects, clipped to just round the room, 1.5 blocks high on the floor.
    rects = [list(s) for s in room["solids"]]
    rects.append([-5, D, W + 5, D + 5])  # the open front: the door mat is the only way out
    for rect in rects:
        # (A solid with a height of its own, a banister by stairs: as high as that, so you can't get over it from the steps.)
        (x0, z0, x1, z1), hh = rect[:4], (rect[4] if len(rect) > 4 else 1.5)
        x0, z0, x1, z1 = max(x0, -1), max(z0, -1), min(x1, W + 1), min(z1, D + 1)
        if x1 <= x0 or z1 <= z0: continue
        for ix in range(math.floor(x0), math.ceil(x1)):
            for iz in range(math.floor(z0), math.ceil(z1)):
                a0, a1 = max(x0, ix) - ix, min(x1, ix + 1) - ix
                b0, b1 = max(z0, iz) - iz, min(z1, iz + 1) - iz
                if a1 - a0 < 1e-3 or b1 - b0 < 1e-3: continue
                if hh <= 1.5:
                    add((ix, 0, iz), [a0 * 16, 0, b0 * 16, a1 * 16, 24, b1 * 16])
                else:
                    for iy in range(0, math.ceil(hh - EPS)):
                        add((ix, iy, iz), [a0 * 16, 0, b0 * 16, a1 * 16, min(16, (hh - iy) * 16), b1 * 16])
    # Stairs: columns an eighth of a block wide, as high as the steps.
    for rp in ramps:
        x0, z0, x1, z1 = rp["rect"]
        hs = rp["h"]
        for q in range(len(hs) - 1):
            xa, xb = hs[q][0], hs[q + 1][0]
            hgt = max(hs[q][1], hs[q + 1][1])
            for iz in range(math.floor(z0), math.ceil(z1)):
                b0, b1 = max(z0, iz) - iz, min(z1, iz + 1) - iz
                ix = math.floor((xa + xb) / 2)
                a0, a1 = xa - ix, xb - ix
                if hgt >= 0:
                    add((ix, -1, iz), [a0 * 16, 0, b0 * 16, a1 * 16, 16, b1 * 16])
                    top = hgt
                    for iy in range(0, math.ceil(top - EPS)):
                        add((ix, iy, iz), [a0 * 16, 0, b0 * 16, a1 * 16, min(16, (top - iy) * 16), b1 * 16])
                else:
                    iy = math.floor(hgt - EPS)
                    # The step's top is at hgt: a solid block under it (whole block if it's a whole step).
                    add((ix, iy, iz), [a0 * 16, 0, b0 * 16, a1 * 16, (hgt - iy) * 16, b1 * 16])
    return shapes


def fit_camera(W, D, H, aspect=16 / 9, pitch=52.0, fov=34.0):
    """How far back the game's camera (pitch 52, FOV 34, north up) sits to show the whole room, and where it looks."""
    p = math.radians(pitch); t = math.tan(math.radians(fov) / 2)
    corners = [(x, y, z) for x in (-0.4, W + 0.4) for y in (0.0, H) for z in (-0.4, D + 0.3)]
    def fits(tz, dist, m=0.9):
        cx, cy = W / 2, 0.9
        cam = (cx, cy + math.sin(p) * dist, tz + math.cos(p) * dist)
        fwd = (0, -math.sin(p), -math.cos(p)); up = (0, math.cos(p), -math.sin(p))
        lo, hi = 1e9, -1e9
        for (x, y, z) in corners:
            d = (x - cam[0], y - cam[1], z - cam[2])
            depth = d[0] * fwd[0] + d[1] * fwd[1] + d[2] * fwd[2]
            sx = d[0] / (depth * t * aspect); sy = (d[1] * up[1] + d[2] * up[2]) / (depth * t)
            if abs(sx) > m: return None
            lo, hi = min(lo, sy), max(hi, sy)
        return (lo, hi) if lo > -m and hi < m else (lo, hi, False)
    best = None
    for tzi in range(0, 41):
        tz = D * (0.3 + 0.5 * tzi / 40)
        a, b = 8.0, 80.0
        for _ in range(40):
            mid = (a + b) / 2; r = fits(tz, mid)
            if r is None or len(r) == 3: a = mid
            else: b = mid
        if best is None or b < best[1]: best = (tz, b)
    return {"x": round(W / 2, 3), "z": round(best[0], 3), "dist": round(best[1], 2)}


LIGHT_TEX = {"windowDusk": "window_dusk", "windowNight": "window_night",
             "doorDusk": "door_dusk", "doorNight": "door_night"}


def write_light(light, res):
    """The light through the windows' pictures (the mod's RoomLight draws them)."""
    out = os.path.join(res, "assets/retromon/textures/light")
    os.makedirs(out, exist_ok=True)
    for k, name in LIGHT_TEX.items():
        if k in light: img_of(light[k]).save(os.path.join(out, name + ".png"))


def panes_of(room):
    """A room's windows' glass, facing in: [corner x y z, along it x y z, up it x y z, facing x y z] each (the mod lays
    the dusk and night skies over them)."""
    W, D = room["W"], room["D"]
    out = []
    for f in room["faces"]:
        if f["k"] not in ("window", "doorLight") or not f.get("n"): continue
        bl, r, u, n, w, h = f["bl"], f["r"], f["u"], f["n"], f["w"], f["h"]
        c = [bl[i] + r[i] * w / 2 + u[i] * h / 2 for i in range(3)]
        to = [W / 2 - c[0], 0, D / 2 - c[2]]
        l = math.hypot(to[0], to[2]) or 1
        if (n[0] * to[0] + n[2] * to[2]) / l < 0.3: continue  # its outside (a hall's ring of windows)
        # (Last, what it is: 0 a window's glass, 1 an open way out's daylight.)
        out.append([round(v, 4) for v in bl + [r[i] * w for i in range(3)] + [u[i] * h for i in range(3)] + n] + [1 if f["k"] == "doorLight" else 0])
    return out


def main():
    src, res = sys.argv[1], sys.argv[2]
    data = json.load(open(src))
    tex = write_textures(data["mats"], res)
    write_light(data.get("light", {}), res)
    models, shapes_t, states = {}, {}, {}
    model_list, shape_list, state_list = [], [], []
    rooms_out, arenas_out = {}, {}
    for rid, room in data["rooms"].items():
        cells = {}
        for f in room["faces"]:
            if f["k"] in SKIP or f["k"] not in tex: continue
            slice_face(f, tex, cells)
        # (An arena's only for looking at: it's put up on your screen alone, for the battle.)
        col = {} if rid.startswith("arena_") else collision(room)
        placed = []
        for cell in set(cells) | set(col):
            els = cells.get(cell, [])
            used = sorted({fc["texture"][1:] for e in els for fc in e["faces"].values()})
            mkey = json.dumps(els, sort_keys=True)
            if mkey not in models:
                models[mkey] = len(model_list)
                model = {"ambientocclusion": False, "textures": {"particle": "retromon:block/interior/" + (used[0] if used else "paint")}, "elements": els}
                for u in used: model["textures"][u] = "retromon:block/interior/" + u
                model_list.append(model)
            boxes = sorted(col.get(cell, []))
            skey = json.dumps(boxes)
            if skey not in shapes_t:
                shapes_t[skey] = len(shape_list); shape_list.append(boxes)
            st = (models[mkey], shapes_t[skey])
            if st not in states:
                states[st] = len(state_list); state_list.append(st)
            placed += [cell[0], cell[1], cell[2], states[st]]
        if rid.startswith("arena_"):
            # A battle arena: its blocks only, as they stand from its corner (the mod builds it over you).
            arenas_out[rid[len("arena_"):]] = {"W": room["W"], "D": room["D"], "H": room["H"], "cells": placed, "panes": panes_of(room)}
            continue
        o = ORIGIN[rid]
        warps = []
        def spawn_of(sp):
            return {"x": sp["x"], "z": sp["z"], "yaw": (180 - math.degrees(sp["yaw"])) % 360}
        for m in room["mats"]:
            # Out to the world (a mat by the front door), or through to another room (the forest and its gates).
            if m.get("to") in (None, "town"):
                warps.append({"kind": "mat", "x0": m["x0"], "x1": m["x1"], "z": room["D"] - 1.0, "door": MAT_DOOR_X.get((rid, round(m["x0"])), MAT_DOOR.get(rid)), "to": None})
            else:
                warps.append({"kind": "mat", "x0": m["x0"], "x1": m["x1"], "z": room["D"] - 1.0, "door": None, "to": m["to"], "spawn": spawn_of(m["spawn"])})
        for m in room.get("passes", []):
            if m.get("to") in (None, "town"):
                warps.append({"kind": "north", "x0": m["x0"], "x1": m["x1"], "z": 1.0, "door": NORTH_DOOR.get(rid), "to": None, "needs": m.get("needs")})
            else:
                warps.append({"kind": "north", "x0": m["x0"], "x1": m["x1"], "z": 1.0, "door": None, "to": m["to"], "spawn": spawn_of(m["spawn"]), "needs": m.get("needs")})
        for m in room.get("sides", []):
            # Through a side wall: out to the world (or asked where to: the Route 28 Gate's west way).
            east = m["wall"] == "e"
            warps.append({"kind": "east" if east else "west", "x": room["W"] - 1.0 if east else 1.0, "z0": m["z0"], "z1": m["z1"],
                          "door": SIDE_DOOR.get((rid, m["wall"])), "to": None, "choice": m.get("choice")})
        for s in room["stairs"]:
            to = s["to"]; sp = s["spawn"]
            warps.append({"kind": s["dir"], "x0": s["x0"], "x1": s["x0"] + 4, "z0": s["z0"], "z1": s["z0"] + s["depth"], "to": to,
                          "spawn": {"x": sp["x"], "z": sp["z"], "yaw": (180 - math.degrees(sp["yaw"])) % 360}})
        people, aides = [], list(AIDES_FROM_GAMES)
        for i, n in enumerate(room["npcs"]):
            if n["name"] == "Blue": continue  # your rival comes in himself when it's time
            key = n.get("key") or PEOPLE.get((rid, n["name"]))
            if not key and n["name"] == "Aide" and aides and not n["seated"]: key = aides.pop(0)
            if not key: key = "in_%s_%d" % (rid, i)
            x, z, yaw = n["x"], n["z"], n["yaw"]
            # Someone seated sits on their chair, as the Interior Viewer has them.
            people.append({"key": key, "name": n["name"].upper(), "skin": n["skin"] or "youngster", "x": round(x, 3), "z": round(z, 3), "y": n.get("y", 0) or 0,
                           "yaw": round((180 - math.degrees(yaw)) % 360, 1), "lines": n["lines"], "role": n.get("role"), "seated": bool(n["seated"]),
                           **({"group": n["group"]} if n.get("group") else {}), **({"hours": n["hours"]} if n.get("hours") else {})})
        acts = [dict({"rect": a["rect"], "label": a["label"], "lines": a["lines"]}, **({"map": True} if a.get("map") else {}), **({"pc": True} if a.get("pc") else {}),
                     **({"use": a["use"]} if a.get("use") else {}))
                for a in room["acts"] if a["lines"]]
        sp = room["spawn"]
        rooms_out[rid] = {"origin": list(o), "W": room["W"], "D": room["D"], "H": room["H"], "name": room["name"], "floor": room["floor"],
                          "place": PLACE[rid], "spawn": {"x": sp["x"], "z": sp["z"], "yaw": (180 - math.degrees(sp["yaw"])) % 360},
                          "entry": ENTRY.get(rid), "cam": fit_camera(room["W"], room["D"], room["H"]), "cells": placed, "warps": warps, "people": people, "acts": acts,
                          "grass": room.get("grass", []), "pools": room.get("pools", []), "panes": panes_of(room), "clocks": room.get("clocks", []),
                          **({"tile": TILE[rid]} if rid in TILE else {}), **({"items": BALLS[rid]} if rid in BALLS else {}),
                          **({"schem": SCHEM[rid][0], "base": SCHEM[rid][1]} if rid in SCHEM else {}),
                          **({"mons": MONS[rid]} if rid in MONS else {})}
    # Models and the block's states.
    mdir = os.path.join(res, "assets/retromon/models/block/prop")
    os.makedirs(mdir, exist_ok=True)
    for f in os.listdir(mdir): os.remove(os.path.join(mdir, f))
    for i, m in enumerate(model_list): json.dump(m, open(os.path.join(mdir, "%d.json" % i), "w"), separators=(",", ":"))
    os.makedirs(os.path.join(res, "assets/retromon/blockstates"), exist_ok=True)
    json.dump({"variants": {"m=%d" % s: {"model": "retromon:block/prop/%d" % st[0]} for s, st in enumerate(state_list)}},
              open(os.path.join(res, "assets/retromon/blockstates/prop.json"), "w"), separators=(",", ":"))
    os.makedirs(os.path.join(res, "retromon/interiors"), exist_ok=True)
    out = {"version": 1, "states": len(state_list), "shapes": [shape_list[st[1]] for st in state_list], "rooms": rooms_out, "arenas": arenas_out, "doors": DOORS, "start": START}
    json.dump(out, open(os.path.join(res, "retromon/interiors/rooms.json"), "w"), separators=(",", ":"))
    print("textures", len(tex), "models", len(model_list), "shapes", len(shape_list), "states", len(state_list))
    for rid, r in rooms_out.items(): print(rid, len(r["cells"]) // 4, "blocks,", len(r["people"]), "people,", len(r["acts"]), "acts,", len(r["warps"]), "warps")


main()
