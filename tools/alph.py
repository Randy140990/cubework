#!/usr/bin/env python3
"""
The RUINS OF ALPH as John has it (tools/alph/ruins_of_alph.schem, 148 x 16 x 192, x east and z south: HeartGold's
ruins, two blocks a tile, the chambers in his cave style, the research centre, the pond and the channel, his three
shelf signs, his two gatehouses copied from his world, his trees seven deep all round), made into a place of its own
in the interiors, as Ilex Forest is (tools/ilex.py).

John's 26 Nov 15:58: "we've got the ruins of Alf to incorporate within the 2 gates ... The schematic isn't built into
the world ... you can see in the world where my 2 gates are, so you would go into them. Those gates obviously need to
be open. I think people can go in and just walk around and explore it, but I would say none of those caves or
anything will have any interaction until I build that."

The ways in and out (both of John's gates, the rooms the mod already has for them, rooms63.js):
  - ROUTE 36's Ruins of Alph gate (jgate_alph): in from Route 36 at its north, out of its south side into the ruins,
    in front of the ruins' north gatehouse, facing south; back up its steps into the gate, at its south side;
  - ROUTE 32's gate (jgate_r32): in from Route 32 at its east, out of its west side into the ruins, in front of the
    ruins' east gatehouse, facing west; back up its steps into the gate, at its west side.
The two gatehouses' doorways from inside the ruins: a dark way in with daylight at its end, as Ilex Forest's and
Viridian Forest's. The chambers' doorways stay John's black (nothing in them yet: "none of those caves ... will have
any interaction until I build that"). No people, items or wild POKeMON: just to walk round. The signs read as
HeartGold's (its message file 72: the ruins' signs).

Writes:
  src/main/resources/retromon/interiors/ruins_of_alph.bin   its blocks (palette, then runs), gzipped
  src/main/resources/retromon/interiors/ruins_of_alph.json  the place
  tools/alph/alph_spots.json                                its gatehouses, doorways and signs, for our looks
                                                            (tools/arealooks69.py) and the viewer (tools/alphview.py)

    python3 -I tools/alph.py
"""
import gzip, io, json, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402

SRC = os.path.join(HERE, "alph", "ruins_of_alph.schem")
RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
SPOTS = os.path.join(HERE, "alph", "alph_spots.json")

# Where the ruins sit in the interiors: past every other place (Mt. Silver's last rooms end at x 18744).
ORIGIN = (18944, 64, 0)
# The schematic's y 3 is the ground (sand, John's paths, grass), on dirt at y 2: you stand at y 4, the place's floor.
FLOOR = 4
FREE = {"air", "light", "short_grass", "fern", "short_dry_grass", "tall_dry_grass", "dead_bush", "orange_tulip", "poppy", "moss_carpet", "vine",
        "hanging_roots", "pointed_dripstone"}

# The gatehouses, John's two from his world (the Route 36 one in the north, the Route 32 one in the east, the same build
# turned), as the file has them: the north one x 52 to 63, z 30 to 41, its doorway onto the ruins in its south face
# (steps z 39 to 41, the dark back at z 36, between low walls at x 56 and 59); the east one x 106 to 117, z 104 to 115,
# its doorway in its west face (steps x 106 to 108, the dark back at x 110, between low walls at z 108 and 111).
NORTH_GATE = dict(x0=52, z0=30, x1=63, z1=41, back=("z", 36), front=37, across=(56, 60))
EAST_GATE = dict(x0=106, z0=104, x1=117, z1=115, back=("x", 110), front=109, across=(108, 112))
# In front of each, coming out of the gate (on the path at the foot of the steps, facing out into the ruins).
FROM_NORTH = {"x": 58.0, "z": 43.4, "yaw": 0.0}
FROM_EAST = {"x": 104.6, "z": 110.0, "yaw": 90.0}
# Back into the gates (the mod's rooms for them, rooms63.js: where you stand coming in from that side).
INTO_R36_GATE = {"x": 7.0, "z": 12.5, "yaw": 180.0}   # jgate_alph, at its south side, facing north
INTO_R32_GATE = {"x": 1.6, "z": 5.0, "yaw": -90.0}    # jgate_r32, at its west side, facing east

# The signs (John's double shelves, facing south), by where they stand: HeartGold's words (msg 72), in the first
# games' capitals.
SIGNS = [
    ((51, 55), "by the path down from the Route 36 gate", ["RUINS OF ALPH.", "Visitors who are interested in archaeology are welcome."]),
    ((52, 97), "below the middle chamber", ["RUINS OF ALPH.", "Feel free to explore the ruins and become a fossil professor!"]),
    ((72, 97), "beside the research centre's door", ["RUINS RESEARCH CENTER"]),
]


def main():
    root = schem.load(SRC)
    W, H, L, inv, ids, _bes, _r = schem.blocks(root)
    names = [inv[i] for i in ids]
    assert (W, H, L) == (148, 16, 192), (W, H, L)

    def idx(x, y, z):
        return (y * L + z) * W + x

    def base(x, y, z):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < L): return "air"
        return names[idx(x, y, z)].replace("minecraft:", "").split("[")[0]

    # The gatehouses' doorways from inside the ruins: the black at the back of each, lit (daylight beyond, as the
    # forests' gatehouses), a light in the way in.
    # (The north one's back faces south, the way out is +z; the east one's faces west, the way out is -x.)
    lit, lamps = 0, 0
    for g in (NORTH_GATE, EAST_GATE):
        axis, at = g["back"]
        cells = [(at, z) for z in range(g["z0"], g["z1"] + 1)] if axis == "x" else [(x, at) for x in range(g["x0"], g["x1"] + 1)]
        for (x, z) in cells:
            for y in range(FLOOR + 2, FLOOR + 6):
                if base(x, y, z) == "black_concrete":
                    names[idx(x, y, z)] = "minecraft:white_concrete"; lit += 1
        for (x, z) in cells:
            if base(x, FLOOR + 3, z) != "white_concrete": continue
            nx, nz = (x - 1, z) if axis == "x" else (x, z + 1)
            fx, fz = (x - 2, z) if axis == "x" else (x, z + 2)
            if base(nx, FLOOR + 2, nz) == "air":
                names[idx(nx, FLOOR + 2, nz)] = "minecraft:light[level=15,waterlogged=false]"; lamps += 1
                if base(fx, FLOOR + 3, fz) == "air": names[idx(fx, FLOOR + 3, fz)] = "minecraft:light[level=10,waterlogged=false]"; lamps += 1
    print("doorway daylight blocks:", lit, " lights:", lamps)

    def open_(x, z):
        return base(x, FLOOR - 1, z) not in FREE and base(x, FLOOR - 1, z) != "water" and base(x, FLOOR, z) in FREE and base(x, FLOOR + 1, z) in FREE

    for k, v in (("from the Route 36 gate", FROM_NORTH), ("from the Route 32 gate", FROM_EAST)):
        assert open_(int(v["x"]), int(v["z"])), k

    # The signs: read from in front (the south), as Ilex Forest's.
    acts, signs = [], []
    for (sx, sz), where, lines in SIGNS:
        assert base(sx, FLOOR, sz) == "pale_oak_shelf" and base(sx + 1, FLOOR, sz) == "pale_oak_shelf", where
        acts.append({"rect": [sx - 0.1, sz - 0.1, sx + 2.1, sz + 1.6], "label": "Read", "lines": lines})
        signs.append({"x": sx, "z": sz, "where": where})

    # The ways back into the gates: up the gatehouse's steps into its doorway. The north one heading north, in its
    # last block and a half before the light; the east one (a box: a way west through a wall has no far side) in the
    # doorway itself.
    warps = [
        {"kind": "north", "x0": 56.6, "x1": 59.4, "z0": 36.5, "z": 38.2, "door": None, "to": "jgate_alph", "spawn": INTO_R36_GATE},
        {"kind": "box", "x0": 108.8, "x1": 110.2, "z0": 108.6, "z1": 111.4, "door": None, "to": "jgate_r32", "spawn": INTO_R32_GATE},
    ]
    # (Where the doorways are open: the north one across x 57 and 58, the east one across z 109 and 110.)
    for x in (57, 58): assert base(x, FLOOR + 2, 37) in ("air", "light") and base(x, FLOOR + 1, 37) == "polished_andesite", x
    for z in (109, 110): assert base(109, FLOOR + 2, z) in ("air", "light") and base(109, FLOOR + 1, z) == "polished_andesite", z

    # The chambers' doorways (John's black, two or four wide, two or three high): for our looks (his cave mouths').
    seen, mouths = set(), []
    blacks = {(x, y, z) for y in range(H) for z in range(L) for x in range(W) if base(x, y, z) == "black_concrete"}
    for p in sorted(blacks):
        if p in seen: continue
        st, grp = [p], []
        seen.add(p)
        while st:
            q = st.pop(); grp.append(q)
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (q[0] + d[0], q[1] + d[1], q[2] + d[2])
                if n in blacks and n not in seen: seen.add(n); st.append(n)
        xs, ys, zs = [a[0] for a in grp], [a[1] for a in grp], [a[2] for a in grp]
        mouths.append([min(xs), min(ys), min(zs), max(xs), max(ys), max(zs)])
    # (The two gatehouses' own dark: their other doorways, inside them, out of sight.)
    gate_dark = [m for m in mouths if any(g["x0"] <= m[0] <= g["x1"] and g["z0"] <= m[2] <= g["z1"] for g in (NORTH_GATE, EAST_GATE))]
    mouths = [m for m in mouths if m not in gate_dark]
    print("chamber doorways:", len(mouths))

    # Things that would come loose or run if anything nudged them (the place is put down without updates, as the
    # forests are): just a count, to know.
    loose = sum(1 for y in range(1, H) for z in range(L) for x in range(W) if base(x, y, z) == "sand" and base(x, y - 1, z) in ("air", "water"))
    runs = sum(1 for y in range(H) for z in range(L) for x in range(W) if base(x, y, z) == "water"
               and any(base(x + dx, y + dy, z + dz) == "air" for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0))))
    print("sand over nothing:", loose, " water with open sides:", runs)

    room = {
        "origin": list(ORIGIN), "W": W, "D": L, "H": H - FLOOR, "name": "Ruins of Alph", "floor": "", "place": "ruins_of_alph_outside",
        "schem": "ruins_of_alph.bin", "base": -FLOOR, "dark": False, "spawn": FROM_NORTH, "entry": FROM_NORTH,
        "cam": {"x": W / 2, "z": L / 2, "dist": 30}, "cells": [], "warps": warps, "people": [], "acts": acts, "grass": [], "items": [],
        "lights": [], "pools": [], "panes": [], "clocks": [],
    }
    os.makedirs(RES, exist_ok=True)
    json.dump({"rooms": {"ruins_of_alph": room}, "doors": {}}, open(os.path.join(RES, "ruins_of_alph.json"), "w"), indent=1)

    # The blocks: the palette, then (count, palette index) runs over y, then z, then x.
    palette, pid, seq = [], {}, []
    for n in names:
        if n not in pid:
            pid[n] = len(palette); palette.append(n)
        seq.append(pid[n])
    out = io.BytesIO()
    out.write(b"RMF1")
    out.write(struct.pack(">hhhh", W, H, L, len(palette)))
    for n in palette:
        b = n.encode("utf-8"); out.write(struct.pack(">h", len(b))); out.write(b)

    def varint(v):
        while True:
            c = v & 0x7F; v >>= 7
            if v: out.write(bytes([c | 0x80]))
            else:
                out.write(bytes([c])); return
    i, runs_n = 0, 0
    while i < len(seq):
        j = i
        while j < len(seq) and seq[j] == seq[i]: j += 1
        varint(j - i); varint(seq[i]); runs_n += 1; i = j
    with gzip.open(os.path.join(RES, "ruins_of_alph.bin"), "wb") as f: f.write(out.getvalue())
    print("blocks", len(seq), "palette", len(palette), "runs", runs_n, "bin", os.path.getsize(os.path.join(RES, "ruins_of_alph.bin")), "bytes")

    spots = {"origin": list(ORIGIN), "floor": FLOOR, "W": W, "H": H, "D": L,
             "gates": [dict(NORTH_GATE, name="the Route 36 gatehouse"), dict(EAST_GATE, name="the Route 32 gatehouse")],
             "mouths": mouths, "signs": signs, "fromNorth": FROM_NORTH, "fromEast": FROM_EAST, "warps": warps}
    json.dump(spots, open(SPOTS, "w"), indent=1)
    print("spots: %d gatehouses, %d doorways, %d signs" % (len(spots["gates"]), len(mouths), len(signs)))


main()
