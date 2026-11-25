#!/usr/bin/env python3
"""(0.66) Checks that the game will find the light for every corner it draws: for each room, every piece in every
block's model (its corners, room-local, and which way it faces) against the room's light file, as client/RoomShade
looks them up (the nearest 64th, or one 64th off).

    python3 tools/interior/export/checkshade.py [room ...]
"""
import bisect, json, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "..", "src", "main", "resources")
Q, OFF = 64, 4096
DIRS = {"east": 0, "west": 1, "up": 2, "down": 3, "south": 4, "north": 5}


def key(x, y, z, d):
    q = lambda v: int((v * Q) + 0.5 if v * Q >= 0 else -int(-(v * Q) + 0.5)) if False else int(__import__("math").floor(v * Q + 0.5)) + OFF
    return ((d * 16384 + q(x)) * 16384 + q(y)) * 16384 + q(z)


def table(rid):
    p = os.path.join(RES, "retromon", "interiors", "light", rid + ".bin")
    if not os.path.exists(p): return None
    b = open(p, "rb").read()
    assert b[:4] == b"RLT1", rid
    n = struct.unpack(">i", b[5:9])[0]
    i, last, keys = 9, 0, []
    for _ in range(n):
        d, mul = 0, 1
        while True:
            c = b[i]; i += 1
            d += (c & 127) * mul
            if c < 128: break
            mul *= 128
        last += d; keys.append(last)
    return keys


def corners(el):
    f, t = [v / 16 for v in el["from"]], [v / 16 for v in el["to"]]
    (d, _), = el["faces"].items()
    a = {"east": 0, "west": 0, "up": 1, "down": 1, "south": 2, "north": 2}[d]
    other = [i for i in range(3) if i != a]
    out = []
    for u in (f[other[0]], t[other[0]]):
        for v in (f[other[1]], t[other[1]]):
            p = [0, 0, 0]; p[a] = f[a]; p[other[0]] = u; p[other[1]] = v
            out.append((p, DIRS[d]))
    return out


def main(ids):
    data = json.load(open(os.path.join(RES, "retromon", "interiors", "rooms.json")))
    states = data["states"] if isinstance(data["states"], list) else None
    st = json.load(open(os.path.join(RES, "assets", "retromon", "blockstates", "prop.json")))["variants"]
    cache = {}
    bad_rooms = 0
    for rid, room in data["rooms"].items():
        if ids and rid not in ids: continue
        keys = table(rid)
        if keys is None: print(rid, "no light"); bad_rooms += 1; continue
        ks = set(keys)
        cells = room["cells"]; miss = near = total = 0
        for i in range(0, len(cells), 4):
            x, y, z, s = cells[i:i + 4]
            mname = st["m=%d" % s]["model"].split("/")[-1]
            if mname not in cache: cache[mname] = json.load(open(os.path.join(RES, "assets", "retromon", "models", "block", "prop", mname + ".json"))).get("elements", [])
            for el in cache[mname]:
                for p, d in corners(el):
                    total += 1
                    k = key(x + p[0], y + p[1], z + p[2], d)
                    if k in ks: continue
                    found = False
                    for dx in (-1, 0, 1):
                        for dy in (-1, 0, 1):
                            for dz in (-1, 0, 1):
                                if key(x + p[0] + dx / Q, y + p[1] + dy / Q, z + p[2] + dz / Q, d) in ks: found = True
                    if found: near += 1
                    else: miss += 1
        if miss: bad_rooms += 1
        print("%-28s corners %7d  exact %7d  near %5d  missing %5d" % (rid, total, total - near - miss, near, miss))
    print("rooms with corners missing:", bad_rooms)


if __name__ == "__main__":
    main(set(sys.argv[1:]))
