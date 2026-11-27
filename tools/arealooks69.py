"""(0.69, John's 26 Nov 15:58) Our looks over the RUINS OF ALPH (tools/alph.py: a place of its own in the interiors,
between Route 36's and Route 32's gates): as John's own Johto has them, his blocks there in the same looks as his blocks
out on the map.

- Every look all Johto has (its sand, its cliffs' rock, its trees' bark and leaves, its glass, its grey, its quartz,
  its trims, its flowers, the grass's tufts onto the sand and the rock), over the whole place.
- His rock (the chambers' rough courses, the mounds, the rocks and rubble: andesite, tuff, cobble, stone, diorite):
  the boulders' stone, as his rocks and his Whirl Islands mounds on the map.
- The two gatehouses (his own, copied from his world): his Johto gates' looks (the dark roof, the pale stone, the
  timber walls, the beams), their doorways onto the ruins daylight, as the forests' gatehouses.
- The chambers' doorways (his cave door: black, stone brick jambs, a stone brick lintel, a smooth stone cap): his cave
  mouths' looks on the map.
- His three double shelf signs: HeartGold's sign, as every sign of his on the map.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
SPOTS = os.path.join(HERE, "alph", "alph_spots.json")
# John's Johto boxes (arealooks.py's JOHTO, arealooks59b's JM): the looks over all of it.
JOHTO_BOX = (-2770, 60, -1011, 1080)
JM_BOX = (-2575, 60, -1000, 1000)
# His Route 36 Ruins of Alph gate on the map: the box its looks are over (tools/arealooks59b.py's gatehouses).
R36_GATE_BOX = (-1962, 533, -1947, 556)
# On the map his ground is y 21; in the ruins it's the file's y 3, put at the interiors' y 63 (the floor, y 64, less
# one): his heights there 42 higher.
DY = 42
MOUTH = ("block/stone_bricks=tunnel_stone,block/smooth_stone_slab_side=tunnel_cap_side,block/smooth_stone=tunnel_cap,block/black_concrete=tunnel_dark")
MOUTH_BLOCKS = "stone_bricks,stone_brick_slab,stone_brick_stairs,stone_brick_wall,smooth_stone_slab,smooth_stone,black_concrete"


def round69(add, room, G, rows):
    S = json.load(open(SPOTS))
    ox, oy, _ = S["origin"]
    y0 = oy - S["floor"]   # the file's y 0
    W, D = S["W"], S["D"]
    shift = lambda ya, yb: ("-", "-") if ya == "-" else (str(int(ya) + DY), str(int(yb) + DY))
    # Every look all Johto has: Johto's own (JOHTO_BOX) over the place and a block in from its edges JM's, which win
    # over them as they do on the map (the smaller box).
    whole = (ox - 1, -1, ox + W, D, "the Ruins of Alph")
    inner = (ox, 0, ox + W - 1, D - 1, "the Ruins of Alph")
    johto = [r for r in rows if len(r) == 10 and tuple(r[2:6]) == JOHTO_BOX]
    jm = [r for r in rows if len(r) == 10 and tuple(r[2:6]) == JM_BOX]
    for r in johto:
        # (Its shelves are signs here, below.)
        if r[0] == "panel" and r[1] == "pale_oak_shelf": continue
        room(r[0], r[1], whole, r[8], shift(r[6], r[7]))
    for r in jm:
        if r[0] == "panel" and r[1] == "pale_oak_shelf": continue
        room(r[0], r[1], inner, r[8], shift(r[6], r[7]))
    # His rock (andesite, tuff, cobble, stone, diorite: the chambers' rough courses, the mounds, the rocks and rubble):
    # his rocks' and his Whirl Islands mounds' look on the map, the boulders' stone.
    room("swap", G["BOULDER_BLOCKS"], (ox + 1, 1, ox + W - 2, D - 2, "the Ruins of Alph's rock"), G["BOULDER"])
    # The gatehouses: his Route 36 gate's looks, over each (its box round the gatehouse, as on the map).
    gate = [r for r in rows if len(r) == 10 and tuple(r[2:6]) == R36_GATE_BOX]
    assert len(gate) >= 5, len(gate)
    for g in S["gates"]:
        box = (ox + g["x0"] - 2, g["z0"] - 4, ox + g["x1"] + 2, g["z1"] + 4, "the ruins' gatehouse (" + g["name"] + ")")
        for r in gate:
            room(r[0], r[1], box, r[8], (str(y0), str(y0 + S["H"] + 4)))
        # Its doorway onto the ruins: daylight beyond (alph.py lit its black white).
        room("swap", "white_concrete", box, "block/white_concrete=cave_daylight!", (str(y0 + S["floor"] + 2), str(y0 + S["floor"] + 5)))
    # The chambers' doorways: his cave mouths' looks (a box round the door, its jambs and its cap).
    for x0, ya, z0, x1, yb, z1 in S["mouths"]:
        room("swap", MOUTH_BLOCKS, (ox + x0 - 2, z0 - 2, ox + x1 + 2, z1 + 2, "a doorway in the ruins"), MOUTH, (str(y0 + ya - 1), str(y0 + yb + 2)))
    # The signs: HeartGold's sign (his double shelf, two wide and two high).
    for s in S["signs"]:
        room("sign", "pale_oak_shelf,pale_oak_sign", (ox + s["x"], s["z"], ox + s["x"] + 1, s["z"], "a sign in the ruins (" + s["where"] + ")"),
             "-", (str(y0 + S["floor"]), str(y0 + S["floor"] + 1)))
