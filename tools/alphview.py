#!/usr/bin/env python3
"""
(0.69, John's 26 Nov 15:58) The RUINS OF ALPH for the Interior Viewer (rooms71.js), block for block as the game puts
them in the interiors (tools/alph.py's ruins_of_alph.bin: the gatehouses' doorways lit), each face in the picture the
game gives it: Minecraft's own (from its models) or the look pack's in its place, then our looks over it
(area_looks.tsv's rows for the ruins, tools/arealooks69.py: the swaps family's winner, laid over one block or several
as the game lays them, a cube's own pictures), John's three shelf signs as the game's sign (its own models).

Writes tools/interior/alph_data.js: the place's blocks (where, which kind, which of a whole block's faces show), each
kind's pictures for its top, bottom and sides, and the pictures themselves.

    python3 -I tools/alphview.py <Minecraft's assets folder (assets/minecraft)>
"""
import base64
import collections
import gzip
import io
import json
import os
import struct
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookview  # noqa: E402
import shiplooks  # noqa: E402

ROOT = os.path.join(HERE, "..", "src", "main", "resources")
BIN = os.path.join(ROOT, "retromon", "interiors", "ruins_of_alph.bin")
MODELS = os.path.join(ROOT, "assets", "retromon", "models", "block", "area")
TEX = os.path.join(ROOT, "assets", "retromon", "textures")
SPOTS = os.path.join(HERE, "alph", "alph_spots.json")
OUT = os.path.join(HERE, "interior", "alph_data.js")
AIR = {"air", "cave_air", "light", "barrier", "structure_void"}
# Drawn in their shapes (rooms70.js's, and rooms71.js's plants): everything that isn't a whole block.
PARTIAL_END = ("_slab", "_stairs", "_trapdoor", "_fence", "_fence_gate", "_wall", "_pane", "_carpet", "_button", "_door", "_sign", "torch", "_rail", "_tulip")
PLANTS = {"short_grass", "fern", "short_dry_grass", "tall_dry_grass", "dead_bush", "poppy", "dandelion", "hanging_roots", "pointed_dripstone", "vine",
          "tall_grass", "large_fern", "azure_bluet", "oxeye_daisy", "cornflower", "wildflowers"}


def read_bin(path):
    raw = gzip.open(path).read()
    assert raw[:4] == b"RMF1"
    W, H, L, n = struct.unpack_from(">hhhh", raw, 4)
    i = 12
    pal = []
    for _ in range(n):
        k = struct.unpack_from(">h", raw, i)[0]; i += 2
        pal.append(raw[i:i + k].decode()); i += k
    seq = np.zeros(W * H * L, np.int32)
    p = 0
    while i < len(raw):
        vals = []
        for _ in range(2):
            v = s = 0
            while True:
                c = raw[i]; i += 1; v |= (c & 0x7F) << s; s += 7
                if not c & 0x80: break
            vals.append(v)
        seq[p:p + vals[0]] = vals[1]; p += vals[0]
    assert p == W * H * L
    return W, H, L, pal, seq.reshape(H, L, W)


class RoomLooks(lookview.Looks):
    """Our looks over a box of the interiors (the rows marked "interiors")."""
    def __init__(self, box):
        x0, x1, z0, z1 = box
        self.by = collections.defaultdict(list)
        for line in open(lookview.TSV):
            if line.startswith("#") or not line.strip(): continue
            r = line.rstrip("\n").split("\t")
            if len(r) < 11 or r[10].strip() != "interiors": continue
            a0, b0, a1, b1 = int(r[2]), int(r[3]), int(r[4]), int(r[5])
            if a1 < x0 or a0 > x1 or b1 < z0 or b0 > z1: continue
            ya, yb = (-999, 999) if r[6] == "-" else (int(r[6]), int(r[7]))
            size = (a1 - a0 + 1) * (b1 - b0 + 1) * (400 if r[6] == "-" else yb - ya + 1)
            for b in r[1].split(","):
                self.by[b].append((a0, b0, a1, b1, ya, yb, r[0], r[8], size))


def main(assets):
    S = json.load(open(SPOTS))
    ox, oy, oz = S["origin"]
    y0 = oy - S["floor"]
    W, H, L, pal, grid = read_bin(BIN)
    bn = [lookview.bname(n) for n in pal]
    pr = [lookview.props(n) for n in pal]
    M = shiplooks.Models(assets)
    P = lookview.Pictures(assets)
    LK = RoomLooks((ox - 2, ox + W + 2, oz - 2, oz + L + 2))

    def partial(i):
        b, p = bn[i], pr[i]
        if b.endswith("_slab"): return p.get("type") != "double"
        return b in PLANTS or b.endswith(PARTIAL_END) or b in ("pale_oak_shelf", "iron_bars", "lantern", "chain", "iron_chain", "ladder", "flower_pot")

    air = np.array([b in AIR for b in bn])[grid]
    part = np.array([partial(i) for i in range(len(pal))])[grid]
    water = np.array([b == "water" for b in bn])[grid]
    glass = np.array(["glass" in b for b in bn])[grid]
    full = ~air & ~part
    # What hides the faces against it: every whole block but glass and water (leaves too: the canopies drawn as their
    # outsides, the trunks inside them not drawn).
    hider = full & ~glass & ~water
    mask = np.zeros(grid.shape, np.uint8)
    for bit, ax, d in [(1, 0, 1), (2, 0, -1), (4, 1, 1), (8, 1, -1), (16, 2, 1), (32, 2, -1)]:
        nb_hide = np.roll(hider, -d, axis=ax)
        nb_same = np.roll(grid, -d, axis=ax) == grid
        hidden = nb_hide | ((glass | water) & nb_same)
        sl = [slice(None)] * 3
        sl[ax] = slice(-1, None) if d == 1 else slice(0, 1)
        hidden[tuple(sl)] = False
        if bit == 2: hidden[0] = True   # (nothing's seen from under the place)
        mask |= np.where(full & ~hidden, bit, 0).astype(np.uint8)
    mask[part & ~air] = 63

    # ---------- pictures ----------
    pics, pic_of = {}, {}

    def pic(ref, per=0, cut=False, alpha=0.0, opaque=None):
        key = (ref, per, cut, alpha, opaque)
        if key in pic_of: return pic_of[key]
        im = P.get(ref).copy()
        if opaque is not None:
            bg = Image.new("RGBA", im.size, opaque + (255,))
            bg.alpha_composite(im)
            im = bg
        buf = io.BytesIO(); im.save(buf, "PNG")
        k = "a%d" % len(pics)
        d = {"png": base64.b64encode(buf.getvalue()).decode(), "per": max(1, per)}
        if cut: d["cut"] = True
        if alpha: d["alpha"] = alpha
        pics[k] = d
        pic_of[key] = k
        return k

    def face_ref(b, base_ref, ft, look):
        """The picture on a face (ft: top, bottom or side) under our look: (ref, per)."""
        horiz = ft == "side"
        if look:
            kind, spec, _ = look
            if kind == "cube":
                per = max([int(m) for m in __import__("re").findall(r"@(\d+)", spec)] or [0])
                s = __import__("re").sub(r"@\d+", "", spec).replace("!", "").split("|")
                return "area:" + (s[1] if horiz and len(s) > 1 else s[0]).strip(), per
            if kind in ("swap", "panel", "glass", "hedge"):
                sw = lookview.parse_swaps(spec).get(base_ref)
                if sw:
                    top, side, per = sw
                    return "area:" + (side if side and horiz else top), per
        return base_ref, 0

    # ---------- the signs: the game's own models (client/AreaLooks' board) ----------
    def facing_of(i):
        return pr[i].get("facing")

    def same_sign(x, y, z, i):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < L): return False
        j = int(grid[y, z, x])
        return bn[j] == bn[i] and facing_of(j) == facing_of(i)

    CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
    STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    models = {}

    def sign_model(x, y, z, i):
        f = facing_of(i)
        left = CW[f]
        lx, lz = STEP[left]
        on_l, on_r = same_sign(x + lx, y, z + lz, i), same_sign(x - lx, y, z - lz, i)
        part_ = "m" if on_l and on_r else "l" if on_r else "r" if on_l else "s"
        row = "stand" if same_sign(x, y + 1, z, i) else "board"
        name = "sign_%s_%s_%s" % (row, part_, f)
        if name not in models:
            m = json.load(open(os.path.join(MODELS, name + ".json")))
            tex = m.get("textures", {})
            boxes = []
            for e in m["elements"]:
                fc = {}
                for d, v in e["faces"].items():
                    t = v["texture"]
                    while t.startswith("#"): t = tex[t[1:]]
                    t = t.replace("retromon:block/area/", "area:")
                    fc[{"up": "top", "down": "bottom", "north": "n", "south": "s", "east": "e", "west": "w"}[d]] = pic(t, 0, True)
                boxes.append([v / 16 for v in e["from"]] + [v / 16 for v in e["to"]] + [fc])
            models[name] = boxes
        return name

    # ---------- every block ----------
    kinds, kind_of, faces = [], {}, {}
    looks_named = {}
    cells = bytearray()
    counts = collections.Counter()
    ys, zs, xs = np.nonzero(mask)
    for y, z, x in zip(ys.tolist(), zs.tolist(), xs.tolist()):
        i = int(grid[y, z, x])
        b = bn[i]
        look, roof = LK.at(b, ox + x, y0 + y, oz + z)
        if look and look[0] == "hide": continue
        if look and look[0] in ("sign", "sign_o", "sign_b") and facing_of(i):
            name = sign_model(x, y, z, i)
            state = name
        else:
            lk = "" if not look else "%s:%s" % (look[0], look[1])
            if lk and lk not in looks_named: looks_named[lk] = len(looks_named)
            synth = b if not lk else "%s~%d" % (b, looks_named[lk])
            if synth not in faces:
                _, fc = M.textures(b)
                if not fc.get("side"): fc = {"top": "block/" + b, "side": "block/" + b, "bottom": "block/" + b}
                if b == "water": fc = {"top": "block/water_still", "side": "block/water_still", "bottom": "block/water_still"}
                one = {}
                for ft in ("top", "bottom", "side"):
                    ref, per = face_ref(b, fc[ft], ft, look)
                    if b == "water": one[ft] = pic(ref, 0, False, 0.78)
                    elif "glass" in b: one[ft] = pic(ref, per, False, 0.62)
                    elif b.endswith("_leaves"): one[ft] = pic(ref, per, False, 0.0, (44, 82, 40))
                    elif b in PLANTS or b.endswith("_tulip"): one[ft] = pic(ref, per, True)
                    else:
                        im = P.get(ref)
                        holes = im.mode == "RGBA" and np.array(im)[:, :, 3].min() < 128
                        one[ft] = pic(ref, per, bool(holes))
                faces[synth] = one
            state = pal[i].replace("minecraft:", "")
            state = synth + (state[len(b):] if state.startswith(b) else "")
        if state not in kind_of:
            kind_of[state] = len(kinds); kinds.append(state)
        k = kind_of[state]
        cells += bytes([x, y, z, k & 255, k >> 8, int(mask[y, z, x])])
        counts[b] += 1
    missing = dict(lookview.MISSING.most_common(20))
    # (What's still in Minecraft's own pictures: not ours, not the look pack's.)
    vanilla = collections.Counter()
    for synth, one in faces.items():
        b = synth.split("~")[0]
        for ft, k in one.items():
            ref = next(r for (r, *_), kk in pic_of.items() if kk == k)
            if not ref.startswith("area:") and ref.split("/")[-1] not in lookview.PACK_NAMES: vanilla[(b, ref)] += 1
    drawn_of = collections.Counter()
    for y, z, x in zip(ys.tolist(), zs.tolist(), xs.tolist()): drawn_of[bn[int(grid[y, z, x])]] += 1
    print("in Minecraft's own pictures:", sorted({b for b, _ in vanilla}, key=lambda b: -drawn_of[b]))
    out = {"name": "RUINS OF ALPH", "W": W, "H": H, "D": L, "floor": S["floor"], "palette": kinds, "faces": faces, "models": models, "pics": pics,
           "cells": base64.b64encode(bytes(cells)).decode(), "count": len(cells) // 6, "fromNorth": S["fromNorth"], "fromEast": S["fromEast"],
           "signs": S["signs"], "warps": S["warps"]}
    with open(OUT, "w") as f:
        f.write("// Written by tools/alphview.py: the RUINS OF ALPH as the game has it in the interiors (tools/alph.py), every block\n")
        f.write("// with a face showing (x, y, z, its kind in two bytes, which of a whole block's faces show), each kind's pictures\n")
        f.write("// as the game draws them (our looks over it: tools/arealooks69.py), the signs as the game's sign.\n")
        f.write("const ALPH = " + json.dumps(out, separators=(",", ":")) + ";\n")
    print("drawn", len(cells) // 6, "kinds", len(kinds), "pictures", len(pics), "sign models", len(models), "bytes", os.path.getsize(OUT))
    print("most drawn:", counts.most_common(12))
    if missing: print("pictures not found:", missing)


if __name__ == "__main__":
    main(sys.argv[1])
