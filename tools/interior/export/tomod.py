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
SKIP = {"aoN", "aoW", "aoE", "deep", "sunPool", "shadowBlob"}
ORIGIN = {"house1": (0, 64, 0), "house2": (64, 64, 0), "rival1": (128, 64, 0), "rival2": (192, 64, 0), "lab": (256, 64, 0), "center": (320, 64, 0),
          "mart": (384, 64, 0)}
PLACE = {"house1": "pallet_town", "house2": "pallet_town", "rival1": "pallet_town", "rival2": "pallet_town", "lab": "pallet_town", "center": "viridian_city",
         "mart": "viridian_city"}
# The doors in John's world (Pallet Town: the lower halves of each double door; outside is to the south).
DOORS = {
    "home": {"room": "house1", "door": [[-803, 19, 679], [-802, 19, 679]], "out": [-802.0, 19.0, 680.6]},
    "rival": {"room": "rival1", "door": [[-785, 19, 679], [-784, 19, 679]], "out": [-784.0, 19.0, 680.6]},
    "lab": {"room": "lab", "door": [[-783, 19, 694], [-782, 19, 694]], "out": [-782.0, 19.0, 695.6]},
    # Viridian City: the Pokémon Center (red roof) and the Poké Mart (blue roof), their open doorways 3 wide.
    "viridian_center": {"room": "center", "door": [[-787, 26, 557], [-786, 26, 557], [-785, 26, 557]], "out": [-785.5, 26.0, 558.6]},
    "viridian_mart": {"room": "mart", "door": [[-767, 26, 543], [-766, 26, 543], [-765, 26, 543]], "out": [-765.5, 26.0, 544.6]},
}
MAT_DOOR = {"house1": "home", "rival1": "rival", "lab": "lab", "center": "viridian_center", "mart": "viridian_mart"}
# Where you come in from outside, and where a new game starts (your room, by the bed).
ENTRY = {"house1": {"x": 6.0, "z": 12.9, "yaw": 180.0}, "rival1": {"x": 6.0, "z": 12.9, "yaw": 180.0}, "lab": {"x": 9.0, "z": 18.9, "yaw": 180.0},
         "center": {"x": 10.0, "z": 14.9, "yaw": 180.0}, "mart": {"x": 8.0, "z": 12.9, "yaw": 180.0}}
START = {"room": "house2", "x": 4.2, "z": 9.5, "yaw": 0.0}
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
                us, ue, vs, ve = a0 / w, a1 / w, b0 / h, b1 / h
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
    for (x0, z0, x1, z1) in rects:
        x0, z0, x1, z1 = max(x0, -1), max(z0, -1), min(x1, W + 1), min(z1, D + 1)
        if x1 <= x0 or z1 <= z0: continue
        for ix in range(math.floor(x0), math.ceil(x1)):
            for iz in range(math.floor(z0), math.ceil(z1)):
                a0, a1 = max(x0, ix) - ix, min(x1, ix + 1) - ix
                b0, b1 = max(z0, iz) - iz, min(z1, iz + 1) - iz
                if a1 - a0 < 1e-3 or b1 - b0 < 1e-3: continue
                add((ix, 0, iz), [a0 * 16, 0, b0 * 16, a1 * 16, 24, b1 * 16])
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


def main():
    src, res = sys.argv[1], sys.argv[2]
    data = json.load(open(src))
    tex = write_textures(data["mats"], res)
    models, shapes_t, states = {}, {}, {}
    model_list, shape_list, state_list = [], [], []
    rooms_out = {}
    for rid, room in data["rooms"].items():
        cells = {}
        for f in room["faces"]:
            if f["k"] in SKIP or f["k"] not in tex: continue
            slice_face(f, tex, cells)
        col = collision(room)
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
        o = ORIGIN[rid]
        warps = []
        for m in room["mats"]:
            warps.append({"kind": "mat", "x0": m["x0"], "x1": m["x1"], "z": room["D"] - 0.42, "door": MAT_DOOR.get(rid), "to": None})
        for s in room["stairs"]:
            to = s["to"]; sp = s["spawn"]
            warps.append({"kind": s["dir"], "x0": s["x0"], "z0": s["z0"], "z1": s["z0"] + s["depth"], "to": to,
                          "spawn": {"x": sp["x"], "z": sp["z"], "yaw": (180 - math.degrees(sp["yaw"])) % 360}})
        people, aides = [], list(AIDES_FROM_GAMES)
        for i, n in enumerate(room["npcs"]):
            if n["name"] == "Blue": continue  # your rival comes in himself when it's time
            key = n.get("key") or PEOPLE.get((rid, n["name"]))
            if not key and n["name"] == "Aide" and aides and not n["seated"]: key = aides.pop(0)
            if not key: key = "in_%s_%d" % (rid, i)
            x, z, yaw = n["x"], n["z"], n["yaw"]
            # Someone seated sits on their chair, as the Interior Viewer has them.
            people.append({"key": key, "name": n["name"].upper(), "skin": n["skin"] or "youngster", "x": round(x, 3), "z": round(z, 3),
                           "yaw": round((180 - math.degrees(yaw)) % 360, 1), "lines": n["lines"], "role": n.get("role"), "seated": bool(n["seated"])})
        acts = [{"rect": a["rect"], "label": a["label"], "lines": a["lines"]} for a in room["acts"] if a["lines"]]
        sp = room["spawn"]
        rooms_out[rid] = {"origin": list(o), "W": room["W"], "D": room["D"], "H": room["H"], "name": room["name"], "floor": room["floor"],
                          "place": PLACE[rid], "spawn": {"x": sp["x"], "z": sp["z"], "yaw": (180 - math.degrees(sp["yaw"])) % 360},
                          "entry": ENTRY.get(rid), "cam": fit_camera(room["W"], room["D"], room["H"]), "cells": placed, "warps": warps, "people": people, "acts": acts}
    # Models and the block's states.
    mdir = os.path.join(res, "assets/retromon/models/block/prop")
    os.makedirs(mdir, exist_ok=True)
    for f in os.listdir(mdir): os.remove(os.path.join(mdir, f))
    for i, m in enumerate(model_list): json.dump(m, open(os.path.join(mdir, "%d.json" % i), "w"), separators=(",", ":"))
    os.makedirs(os.path.join(res, "assets/retromon/blockstates"), exist_ok=True)
    json.dump({"variants": {"m=%d" % s: {"model": "retromon:block/prop/%d" % st[0]} for s, st in enumerate(state_list)}},
              open(os.path.join(res, "assets/retromon/blockstates/prop.json"), "w"), separators=(",", ":"))
    os.makedirs(os.path.join(res, "retromon/interiors"), exist_ok=True)
    out = {"version": 1, "states": len(state_list), "shapes": [shape_list[st[1]] for st in state_list], "rooms": rooms_out, "doors": DOORS, "start": START}
    json.dump(out, open(os.path.join(res, "retromon/interiors/rooms.json"), "w"), separators=(",", ":"))
    print("textures", len(tex), "models", len(model_list), "shapes", len(shape_list), "states", len(state_list))
    for rid, r in rooms_out.items(): print(rid, len(r["cells"]) // 4, "blocks,", len(r["people"]), "people,", len(r["acts"]), "acts,", len(r["warps"]), "warps")


main()
