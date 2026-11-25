#!/usr/bin/env python3
"""
(0.67, John's 16:28) The S.S. ANNE from outside, for the Interior Viewer to show in our looks, for review only: "I
want you to present me in the viewer ... this will be an exterior kind of, the ship updated ... for me to pan around
it ... whatever colours exist now, it's just focusing on those and making them within our textural changes. It's
purely for review first." (The game's ship stays as John built it: arealooks59c's "keep" rows.)

Reads the ship out of John's Kanto world, keeps what can be seen from outside it (a flood of the open air and water
round it, from the edges of its box in), and writes tools/interior/ship_data.js: its blocks (where, which state, and
for a whole block which of its faces show), each kind's colour (its own picture's, from Minecraft's textures) and the
family of our looks it's drawn in (rooms70.js draws them).

    python3 tools/shipview.py <John's world folder> <Minecraft's assets folder (assets/minecraft)>
"""
import base64
import collections
import json
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import spotworld  # noqa: E402

BOX = (-452, -262, 18, 72, 616, 666)  # x0 x1 y0 y1 z0 z1
OUT = os.path.join(HERE, "interior", "ship_data.js")
# What isn't the ship: the sea, the sea bed and the bits of land in its box.
NOT_SHIP = {"air", "cave_air", "water", "dripstone_block", "dirt", "grass_block", "podzol", "short_grass", "tall_grass", "large_fern", "fern", "seagrass", "tall_seagrass", "kelp", "kelp_plant"}
# Drawn not at all (too small to read from outside, or nothing our looks could show: writing, faces, wiring).
SKIP_SUFFIX = ("_sign", "_banner", "_coral_fan", "_coral_wall_fan", "_coral", "torch", "_bed")
SKIP = {"tripwire", "tripwire_hook", "redstone_wire", "redstone_wall_torch", "comparator", "repeater", "cobweb", "lever", "potted_dead_bush", "campfire", "brewing_stand",
        "bamboo", "rail", "activator_rail", "powered_rail", "detector_rail", "flower_pot", "potted_fern", "fire"}
# Whole blocks (all six faces a full square; what shows of them is worked out here).
PARTIAL = ("_slab", "_stairs", "_trapdoor", "_fence", "_fence_gate", "_wall", "_pane", "_carpet", "_button", "_door", "_head", "_wall_head")
PARTIAL_NAMES = {"iron_bars", "end_rod", "iron_chain", "chain", "ladder", "lantern", "anvil", "grindstone", "cauldron", "daylight_detector", "player_head", "player_wall_head", "snow"}
# What the air outside passes through (railings, rods, posts): the rest, even half blocks, closes a room off.
OPEN_SUFFIX = ("_fence", "_fence_gate", "_wall", "_carpet", "_button", "_head", "_wall_head")
OPEN_NAMES = {"iron_bars", "end_rod", "iron_chain", "chain", "ladder", "lantern"}


def bname(n):
    return n.replace("minecraft:", "").split("[")[0]


def props(n):
    if "[" not in n: return {}
    return dict(kv.split("=") for kv in n.split("[", 1)[1].rstrip("]").split(","))


def skipped(b):
    return b in NOT_SHIP or b in SKIP or b.endswith(SKIP_SUFFIX)


def partial(b, p):
    if b.endswith("_slab"): return p.get("type") != "double"
    return b in PARTIAL_NAMES or b.endswith(PARTIAL)


def passes(b, p):
    return b in NOT_SHIP or skipped(b) or b in OPEN_NAMES or b.endswith(OPEN_SUFFIX)


# ---------- each kind's own colour (its picture's average, from Minecraft's textures) ----------
TEXNAME = {
    "smooth_quartz": "quartz_block_bottom", "smooth_quartz_stairs": "quartz_block_bottom", "smooth_quartz_slab": "quartz_block_bottom",
    "quartz_block": "quartz_block_side", "quartz_stairs": "quartz_block_side", "quartz_slab": "quartz_block_side", "quartz_pillar": "quartz_pillar",
    "bone_block": "bone_block_side", "polished_basalt": "polished_basalt_side", "smooth_stone_slab": "smooth_stone", "stone_slab": "stone",
    "smooth_red_sandstone_slab": "red_sandstone_top", "smooth_red_sandstone_stairs": "red_sandstone_top", "sandstone": "sandstone",
    "stripped_dark_oak_wood": "stripped_dark_oak_log", "stripped_birch_wood": "stripped_birch_log", "stripped_oak_wood": "stripped_oak_log",
    "stripped_spruce_wood": "stripped_spruce_log", "oak_wood": "oak_log", "acacia_wood": "acacia_log", "birch_wood": "birch_log",
    "light_gray_shulker_box": "light_gray_shulker_box", "white_shulker_box": "white_shulker_box", "anvil": "anvil", "grindstone": "grindstone_side",
    "cauldron": "cauldron_side", "barrel": "barrel_side", "smoker": "smoker_front", "blast_furnace": "blast_furnace_front", "bookshelf": "bookshelf",
    "player_head": "white_concrete", "player_wall_head": "white_concrete", "iron_chain": "iron_chain", "lantern": "lantern", "daylight_detector": "daylight_detector_top",
}


def texture_for(b, tex):
    cands = []
    if b in TEXNAME: cands.append(TEXNAME[b])
    cands.append(b)
    stem = b
    for suf in ("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_button", "_pane", "_carpet", "_door", "_trapdoor"):
        if stem.endswith(suf) and suf != "_trapdoor":
            stem = stem[: -len(suf)]
            break
    for c in (stem, stem + "s", stem + "_planks", stem + "_block", stem + "_side", stem + "_log", stem.replace("_brick", "_bricks"), stem.replace("wood", "planks")):
        cands.append(c)
    if b.endswith("_carpet"): cands.append(b.replace("_carpet", "_wool"))
    if b.endswith("_pane"): cands.append(b.replace("_pane", ""))
    if b.endswith("_door"): cands.append(b + "_bottom")
    if b == "chain": cands.append("iron_chain")
    for c in cands:
        f = os.path.join(tex, c + ".png")
        if os.path.exists(f): return f
    return None


def colour_of(b, tex):
    f = texture_for(b, tex)
    if not f: return (200, 200, 200)
    im = np.array(Image.open(f).convert("RGBA"))[:16, :16].reshape(-1, 4)
    im = im[im[:, 3] > 20]
    if not len(im): return (200, 200, 200)
    return tuple(int(v) for v in im[:, :3].mean(0))


def family(b):
    if "glass" in b: return "glass"
    if b in ("sea_lantern", "end_rod", "glowstone", "shroomlight", "redstone_lamp", "lantern", "ochre_froglight", "pearlescent_froglight", "verdant_froglight"): return "glow"
    if b.endswith("_leaves"): return "leaves"
    if b.startswith("iron_") or b in ("anvil", "chain", "cauldron", "hopper", "grindstone", "polished_basalt", "lightning_rod"): return "metal"
    woods = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped")
    if any(b.startswith(w + "_") or b.startswith("stripped_" + w) for w in woods) or b in ("bookshelf", "barrel"): return "wood"
    return "paint"


def main(world, assets):
    W = spotworld.World(world)
    x0, x1, y0, y1, z0, z1 = BOX
    names, ids = W.box(x0, x1, y0, y1, z0, z1)
    bn = [bname(n) for n in names]
    pr = [props(n) for n in names]
    Y, Z, X = ids.shape
    skip = np.array([skipped(b) for b in bn])
    part = np.array([partial(b, p) for b, p in zip(bn, pr)])
    open_ = np.array([passes(b, p) for b, p in zip(bn, pr)])
    S = ~skip[ids]           # the ship's blocks (drawn ones)
    O = open_[ids]           # where the outside's air can go
    # The ship itself: the biggest piece of touching blocks (the dock's posts and the harbour wall left out).
    from scipy import ndimage  # noqa
    lab, n = ndimage.label(S, structure=np.ones((3, 3, 3)))
    sizes = ndimage.sum(S, lab, range(1, n + 1))
    ship = lab == (1 + int(np.argmax(sizes)))
    # The outside: the open cells joined to the box's edges (the sky and the sea round the ship).
    olab, on = ndimage.label(O | ~ship & ~S, structure=ndimage.generate_binary_structure(3, 1))
    edge = set(np.unique(np.concatenate([olab[0].ravel(), olab[-1].ravel(), olab[:, 0].ravel(), olab[:, -1].ravel(), olab[:, :, 0].ravel(), olab[:, :, -1].ravel()]))) - {0}
    outside = np.isin(olab, list(edge))
    full = ship & ~part[ids]
    shown = ship & (np.zeros_like(ship))
    # A block shows if it's in the outside's air (a railing) or has a face on it.
    near = np.zeros_like(ship)
    for ax in range(3):
        for d in (1, -1):
            near |= np.roll(outside, d, axis=ax) & True
    shown = ship & (outside | near)
    # Each whole block's faces that show: those onto the outside's air or onto a part block that's drawn.
    # Bits: 1 top (+y), 2 bottom, 4 south (+z), 8 north (-z), 16 east (+x), 32 west (-x).
    dirs = [(1, 0, 0, 1), (2, 0, 0, -1), (4, 1, 0, 1), (8, 1, 0, -1), (16, 2, 0, 1), (32, 2, 0, -1)]
    glassy = np.array(["glass" in b for b in bn])[ids]
    mask = np.zeros(ids.shape, np.uint8)
    for bit, ax, _, d in dirs:
        nb_out = np.roll(outside, -d, axis=ax)
        nb_part = np.roll(shown & ~full, -d, axis=ax)
        nb_glass = np.roll(shown & glassy, -d, axis=ax) & ~glassy
        f = nb_out | nb_part | nb_glass
        # (Not round the box's own edges, where the roll wraps.)
        sl = [slice(None)] * 3
        sl[ax] = slice(-1, None) if d == 1 else slice(0, 1)
        f[tuple(sl)] = True
        mask |= np.where(full & shown & f, bit, 0).astype(np.uint8)
    keep = shown & (~full | (mask > 0))
    zz = np.nonzero(keep)
    used = sorted(set(ids[zz].tolist()))
    remap = {o: i for i, o in enumerate(used)}
    tex = os.path.join(assets, "textures", "block")
    kinds = {}
    pal = []
    for o in used:
        b = bn[o]
        if b not in kinds: kinds[b] = {"rgb": colour_of(b, tex), "fam": family(b)}
        pal.append(names[o].replace("minecraft:", ""))
    ys, zs, xs = zz
    buf = bytearray()
    for y, z, x in zip(ys.tolist(), zs.tolist(), xs.tolist()):
        p = remap[int(ids[y, z, x])]
        buf += bytes([x - 0 if x < 256 else 255, y, z, p & 255, p >> 8, int(mask[y, z, x])])
    # The sea's top (for the water round it in the viewer).
    water = np.array([b == "water" for b in bn])[ids]
    tops = collections.Counter()
    for z in range(0, Z, 3):
        for x in range(0, X, 3):
            col = np.nonzero(water[:, z, x])[0]
            if len(col): tops[int(col[-1])] += 1
    sea = tops.most_common(1)[0][0] + 1 if tops else 11
    data = {"W": X, "H": Y, "D": Z, "sea": sea, "palette": pal, "kinds": kinds, "cells": base64.b64encode(bytes(buf)).decode(),
            "origin": [x0, y0, z0], "count": len(ys)}
    with open(OUT, "w") as f:
        f.write("// Written by tools/shipview.py from John's Kanto world: the S.S. ANNE as can be seen from outside it, for the\n")
        f.write("// Interior Viewer to show in our looks (rooms70.js), for review. x, y, z, its state (two bytes), its faces that show.\n")
        f.write("const SHIP = " + json.dumps(data, separators=(",", ":")) + ";\n")
    print("ship blocks shown:", len(ys), "kinds:", len(kinds), "states:", len(pal), "sea at", sea, "bytes", os.path.getsize(OUT))
    fams = collections.Counter(kinds[bn[o]]["fam"] for o in used)
    print(fams)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
