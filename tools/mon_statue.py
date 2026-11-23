"""
A statue of one of our own creatures (the mod's Retromon models, assets/retromon/mons/<kind>/model.geo.json) as a block
model: every cube of its rest pose laid out as Minecraft model elements (each turned about the one axis it's turned
most about, to the nearest 22.5 degrees), in one material, facing south, standing on the block below's top
(16 pixels up), as tall as `height` pixels. John's 11:57: the INDIGO PLATEAU's copper golem statues "like one of our
retro ones... just a conversion".

    import mon_statue; mon_statue.model("rhydon", "retromon:block/area/statue_bronze", 15.5)
"""
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
MONS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "mons")
SKIP = ("fx_", "eyelid", "squint", "dizzy", "faint", "eye_")


def _rx(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0], [0, c, -s], [0, s, c]]


def _ry(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s], [0, 1, 0], [-s, 0, c]]


def _rz(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def _mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def _mv(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def _turn(rot):
    """A bone's (or cube's) turn in the model's own frame, as the mod draws it (client/BedrockModel: Minecraft turns it
    by x, y, -z in its frame, which is the model's turned half round about z)."""
    rx, ry, rz = (math.radians(v) for v in (rot or [0, 0, 0]))
    return _mm(_rz(-rz), _mm(_ry(-ry), _rx(-rx)))


def elements(kind, height):
    geo = json.load(open(os.path.join(MONS, kind, "model.geo.json")))["minecraft:geometry"][0]
    bones = {b["name"]: b for b in geo["bones"]}

    def chain(name):
        out = []
        while name:
            b = bones[name]
            out.append(b)
            name = b.get("parent")
        return out

    cubes = []
    for b in geo["bones"]:
        if b["name"].startswith(SKIP) or any(a["name"].startswith(SKIP) for a in chain(b["name"])):
            continue
        for c in b.get("cubes", []):
            if min(c["size"]) <= 0.01:
                continue
            cubes.append((b, c))
    # Every cube's corners in the model's frame, and its whole turn.
    placed = []
    for b, c in cubes:
        o, s = c["origin"], c["size"]
        pts = [[o[0] + dx * s[0], o[1] + dy * s[1], o[2] + dz * s[2]] for dx in (0, 1) for dy in (0, 1) for dz in (0, 1)]
        turns = []
        if c.get("rotation") and any(c["rotation"]):
            turns.append((c.get("pivot") or b.get("pivot") or [0, 0, 0], _turn(c["rotation"])))
        for a in chain(b["name"]):
            if a.get("rotation") and any(a["rotation"]):
                turns.append((a.get("pivot") or [0, 0, 0], _turn(a["rotation"])))
        whole = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
        for pv, m in turns:
            pts = [[pv[i] + v for i, v in enumerate(_mv(m, [p[0] - pv[0], p[1] - pv[1], p[2] - pv[2]]))] for p in pts]
            whole = _mm(m, whole)
        placed.append((s, pts, whole))
    ys = [p[1] for _, pts, _ in placed for p in pts]
    sc = height / (max(ys) - min(ys))
    y0 = min(ys)
    els = []
    for s, pts, whole in placed:
        # Into the block's frame: facing south (the model faces -z: turned half round about y), from the block below's
        # top. The half turn about y flips the turns about x and z.
        bp = [[8 - p[0] * sc, 16 + (p[1] - y0) * sc, 8 - p[2] * sc] for p in pts]
        F = [[-1, 0, 0], [0, 1, 0], [0, 0, -1]]
        m = _mm(F, _mm(whole, F))
        cx, cy, cz = (sum(p[i] for p in bp) / 8 for i in range(3))
        ax = math.degrees(math.atan2(m[2][1], m[2][2]))
        ay = math.degrees(math.atan2(-m[2][0], math.hypot(m[2][1], m[2][2])))
        az = math.degrees(math.atan2(m[1][0], m[0][0]))
        axis, ang = max((("x", ax), ("y", ay), ("z", az)), key=lambda t: abs(t[1]))
        snap = max(-45.0, min(45.0, round(ang / 22.5) * 22.5))
        hx, hy, hz = s[0] * sc / 2, s[1] * sc / 2, s[2] * sc / 2
        e = {"from": [round(cx - hx, 3), round(cy - hy, 3), round(cz - hz, 3)], "to": [round(cx + hx, 3), round(cy + hy, 3), round(cz + hz, 3)],
             "faces": {f: {"texture": "#s"} for f in ("north", "south", "east", "west", "up", "down")}}
        if snap:
            e["rotation"] = {"origin": [round(cx, 3), round(cy, 3), round(cz, 3)], "axis": axis, "angle": snap}
        els.append(e)
    return els


def model(kind, texture, height, path):
    m = {"ambientocclusion": True, "textures": {"s": texture, "particle": texture}, "elements": elements(kind, height)}
    json.dump(m, open(path, "w"), indent=None, separators=(",", ":"))
    return m


if __name__ == "__main__":
    import sys
    els = elements(sys.argv[1] if len(sys.argv) > 1 else "rhydon", 15.5)
    print(len(els), "elements")
    for e in els[:5]: print(e)
