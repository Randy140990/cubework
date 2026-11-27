"""(0.70, John's 27 Nov 14:30: "it's the latest map ... all you're doing is basically keeping everything we've already
applied on top of it") His new save's new parts, in the looks the rest of the map has.

- Johto's own looks now reach out to his new west end (arealooks.py's JOHTO, the cliffside's box and the sea rocks'
  "Routes 47 and 48" box: x -2770 now, Route 48's field and the cliffs to its west).
- The rocks standing in its water (his tuff, cobble and diorite rocks, in the river up on the cliffs and along the
  shore): the sea rocks' look, as every rock of his in the water.
- Its two plank bridges over the river: a bridge's own look, as his other bridges (arealooks67's two by Cianwood).
(tools/area_west70.json: the scan of the save that found them.)
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


def round70(add, room, G):
    W = json.load(open(os.path.join(HERE, "area_west70.json")))
    SEA_ROCK, SEA_ROCK_BLOCKS = G["SEA_ROCK"], G["SEA_ROCK_BLOCKS"]
    for r in W["rocks"]:
        add("swap", SEA_ROCK_BLOCKS, (r["x0"] - 1, r["z0"] - 1, r["x1"] + 1, r["z1"] + 1, "a rock in the water"), SEA_ROCK, ys(r["y0"] - 1, r["y1"] + 1))
    BR = ("oak_slab,oak_planks,oak_stairs,stripped_oak_wood,stripped_oak_log,oak_shelf,stone_button,spruce_shelf,spruce_stairs,spruce_slab,spruce_planks,"
          "stripped_spruce_wood,stripped_spruce_log")
    BRS = ("block/oak_planks=bridge_boards,block/spruce_planks=bridge_boards,block/stripped_oak_log=bridge_beam,block/stripped_oak_log_top=bridge_beam,"
           "block/stripped_spruce_log=bridge_beam,block/stripped_spruce_log_top=bridge_beam,block/oak_shelf=bridge_beam,block/spruce_shelf=bridge_beam,block/stone=bolt")
    for r in W["bridges"]:
        b = (r["x0"] - 1, r["z0"] - 1, r["x1"] + 1, r["z1"] + 1, "a bridge")
        add("panel", BR, b, BRS, ys(r["y0"] - 1, r["y1"] + 1))
        add("swap", "andesite_slab,andesite,andesite_stairs", b, "block/andesite=bridge_boards", ys(r["y0"] - 1, r["y1"] + 1))


# ---------- Ilex Forest (John's 14:44: "Plants and stuff all look good. Make sure they're all universal.") ----------
# John's forest (tools/ilex.py: a place of its own in the interiors at x 10240, its floor the file's y 6, at y 64) had
# none of our looks: its trees, its grass and its two gatehouses all Minecraft's own. Now as his Johto has them, as the
# Ruins of Alph are (arealooks69.round69): every look all Johto has over it, his Johto gates' looks on its two
# gatehouses (their doorways daylight, as Viridian Forest's), its shelf sign HeartGold's.
ILEX = dict(ox=10240, y0=58, W=152, D=224, floor=6,
            gates=[(34, 34, 49, 49, "its north gatehouse"), (32, 179, 47, 194, "its south gatehouse")], signs=[(44, 96)])


def round70ilex(room, G, rows):
    import arealooks69 as A
    I = ILEX
    ox, y0, W, D = I["ox"], I["y0"], I["W"], I["D"]
    shift = lambda ya, yb: ("-", "-") if ya == "-" else (str(int(ya) + A.DY), str(int(yb) + A.DY))
    whole = (ox - 1, -1, ox + W, D, "Ilex Forest")
    inner = (ox, 0, ox + W - 1, D - 1, "Ilex Forest")
    for box, key in ((whole, A.JOHTO_BOX), (inner, A.JM_BOX)):
        for r in rows:
            if len(r) != 10 or tuple(r[2:6]) != key: continue
            if r[0] == "panel" and r[1] == "pale_oak_shelf": continue
            room(r[0], r[1], box, r[8], shift(r[6], r[7]))
    gate = [r for r in rows if len(r) == 10 and tuple(r[2:6]) == A.R36_GATE_BOX]
    assert len(gate) >= 5, len(gate)
    for x0, z0, x1, z1, name in I["gates"]:
        box = (ox + x0 - 2, z0 - 4, ox + x1 + 2, z1 + 4, "Ilex Forest's gatehouse (" + name + ")")
        for r in gate:
            room(r[0], r[1], box, r[8], (str(y0), str(y0 + 26)))
        room("swap", "white_concrete", box, "block/white_concrete=cave_daylight!", (str(y0 + I["floor"] + 2), str(y0 + I["floor"] + 5)))
    for x, z in I["signs"]:
        room("sign", "pale_oak_shelf,pale_oak_sign", (ox + x, z, ox + x + 1, z, "Ilex Forest's sign"), "-", (str(y0 + I["floor"]), str(y0 + I["floor"] + 1)))
    # Every place in the interiors with flowers growing (the forests, the SAFARI ZONE, the caves): the map's flowers
    # (Kanto's and Johto's are the same), so they're the same everywhere.
    every = (4000, -10, 20000, 1100, "the flowers in every place inside")
    for r in rows:
        if len(r) == 10 and r[9] == "Kanto" and r[1] in ("wildflowers", "dandelion,oxeye_daisy,azure_bluet,poppy,red_tulip"):
            room(r[0], r[1], every, r[8], (r[6], r[7]))
