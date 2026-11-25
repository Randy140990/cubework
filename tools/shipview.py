#!/usr/bin/env python3
"""
(0.67, John's 16:28) The S.S. ANNE from outside, for the Interior Viewer to show in our looks, for review only: "I
want you to present me in the viewer ... this will be an exterior kind of, the ship updated ... for me to pan around
it ... whatever colours exist now, it's just focusing on those and making them within our textural changes. It's
purely for review first." (The game's ship stays as John built it: arealooks59c's "keep" rows.)

Reads the ship out of John's Kanto world, every block of it (17:11: "you've not like removed anything ... You've just
basically updated the textures only, correct?": its insides too, so its windows look into its rooms, and every
detail, the beds, signs, banners and the rest, in its shape), and writes tools/interior/ship_data.js: its blocks
(where, which state, and for a whole block which of its faces show), each kind's colour (its own picture's, from
Minecraft's textures) and the family of our looks it's drawn in (rooms70.js draws them). Left out is only what isn't
the ship: the sea, the sea bed, and what doesn't touch the ship (the harbour's glass wall, the pier's legs, a tree).

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

OUT = os.path.join(HERE, "interior", "ship_data.js")
# What isn't the ship: the sea, the sea bed and the bits of land in its box.
NOT_SHIP = {"air", "cave_air", "water", "dripstone_block", "dirt", "grass_block", "podzol", "short_grass", "tall_grass", "large_fern", "fern", "seagrass", "tall_seagrass", "kelp", "kelp_plant"}
# (Nothing of the ship is left out: what isn't a whole block is drawn in its shape, rooms70.js.)
SKIP_SUFFIX = ()
SKIP = set()
# Whole blocks (all six faces a full square; which of their faces show is worked out here).
PARTIAL = ("_slab", "_stairs", "_trapdoor", "_fence", "_fence_gate", "_wall", "_pane", "_carpet", "_button", "_door", "_head", "_wall_head", "_bed", "_sign",
           "_banner", "_fan", "torch", "_rail", "_skull")
PARTIAL_NAMES = {"iron_bars", "end_rod", "iron_chain", "chain", "ladder", "lantern", "anvil", "grindstone", "daylight_detector", "player_head", "player_wall_head", "snow",
                 "tripwire_hook", "tripwire", "rail", "cobweb", "lever", "comparator", "repeater", "campfire", "bamboo", "potted_dead_bush", "flower_pot", "brewing_stand",
                 "redstone_wire", "fire", "dead_bush", "short_grass", "fern"}


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
    "player_head": "light_gray_concrete", "player_wall_head": "light_gray_concrete", "iron_chain": "iron_chain", "lantern": "lantern", "daylight_detector": "daylight_detector_top",
    "redstone_wall_torch": "redstone_torch", "campfire": "campfire_log", "bamboo": "bamboo_stalk", "potted_dead_bush": "flower_pot", "dead_horn_coral_wall_fan": "dead_horn_coral_fan",
    "dead_fire_coral_wall_fan": "dead_fire_coral_fan", "brewing_stand": "brewing_stand_base",
}
WOOL = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")


def texture_for(b, tex):
    cands = []
    if b in TEXNAME: cands.append(TEXNAME[b])
    # Beds and banners in their cloth's colour, signs in their wood's.
    for w in WOOL:
        if b in (w + "_bed", w + "_banner", w + "_wall_banner"): cands.append(w + "_wool")
    for suf in ("_wall_hanging_sign", "_hanging_sign", "_wall_sign", "_sign"):
        if b.endswith(suf): cands.append(b[: -len(suf)] + "_planks"); break
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
    if b == "cobweb": return "web"
    if b in ("sea_lantern", "end_rod", "glowstone", "shroomlight", "redstone_lamp", "lantern", "ochre_froglight", "pearlescent_froglight", "verdant_froglight"): return "glow"
    if b.endswith("_leaves"): return "leaves"
    if b.startswith("iron_") or b in ("anvil", "chain", "cauldron", "hopper", "grindstone", "polished_basalt", "lightning_rod"): return "metal"
    woods = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped")
    if any(b.startswith(w + "_") or b.startswith("stripped_" + w) for w in woods) or b in ("bookshelf", "barrel"): return "wood"
    return "paint"


def ship_cells(W, sid, box, assets):
    """One ship's blocks for the viewer: where, which state, which of a whole block's faces show."""
    import shiplooks
    x0, x1, y0, y1, z0, z1 = box
    names, ids = W.box(x0, x1, y0, y1, z0, z1)
    bn = [bname(n) for n in names]
    pr = [props(n) for n in names]
    Y, Z, X = ids.shape
    part = np.array([partial(b, p) for b, p in zip(bn, pr)])
    ship = shiplooks.ship_mask(sid, bn, ids, y0)
    full = ship & ~part[ids]
    # Each whole block's faces that show: all but those against another whole block of the ship that hides them (glass
    # and leaves hide only their own kind). Bits: 1 top (+y), 2 bottom, 4 south (+z), 8 north (-z), 16 east (+x), 32 west (-x).
    seethru = np.array(["glass" in b or b.endswith("_leaves") for b in bn])
    opaque = full & ~seethru[ids]
    mask = np.zeros(ids.shape, np.uint8)
    for bit, ax, d in [(1, 0, 1), (2, 0, -1), (4, 1, 1), (8, 1, -1), (16, 2, 1), (32, 2, -1)]:
        nb_opaque = np.roll(opaque, -d, axis=ax)
        nb_same = np.roll(full & seethru[ids], -d, axis=ax) & (np.roll(ids, -d, axis=ax) == ids)
        hidden = nb_opaque | (nb_same & seethru[ids])
        sl = [slice(None)] * 3
        sl[ax] = slice(-1, None) if d == 1 else slice(0, 1)
        hidden[tuple(sl)] = False
        mask |= np.where(full & ~hidden, bit, 0).astype(np.uint8)
    # (A detail's shape is drawn whole; 63 in case its shape isn't one rooms70.js knows: then a whole block.)
    mask[ship & ~full] = 63
    keep = ship & (mask > 0)
    zz = np.nonzero(keep)
    used = sorted(set(ids[zz].tolist()))
    remap = {o: i for i, o in enumerate(used)}
    tex = os.path.join(assets, "textures", "block")
    kinds, pal = {}, []
    for o in used:
        b = bn[o]
        if b not in kinds: kinds[b] = {"rgb": colour_of(b, tex), "fam": family(b)}
        pal.append(names[o].replace("minecraft:", ""))
    ys, zs, xs = zz
    buf = bytearray()
    for y, z, x in zip(ys.tolist(), zs.tolist(), xs.tolist()):
        p = remap[int(ids[y, z, x])]
        buf += bytes([x, y, z, p & 255, p >> 8, int(mask[y, z, x])])
    water = np.array([b == "water" for b in bn])[ids]
    tops = collections.Counter()
    for z in range(0, Z, 3):
        for x in range(0, X, 3):
            col = np.nonzero(water[:, z, x])[0]
            if len(col): tops[int(col[-1])] += 1
    sea = tops.most_common(1)[0][0] + 1 if tops else 11
    print(sid, "ship blocks:", int(ship.sum()), "drawn (some face showing):", len(ys), "kinds:", len(kinds), "states:", len(pal), "sea at", sea)
    return {"W": X, "H": Y, "D": Z, "sea": sea, "palette": pal, "kinds": kinds, "cells": base64.b64encode(bytes(buf)).decode(), "origin": [x0, y0, z0], "count": len(ys)}


def main(world, assets):
    """Both ships (tools/shiplooks.py's), in the pictures the game draws them in (its ship_looks.json, written first)."""
    import shiplooks
    W = spotworld.World(world)
    looks = json.load(open(os.path.join(HERE, "interior", "ship_looks.json")))
    out = {"pics": looks["pics"], "ships": {}}
    for sid, name, moored, box, _ in shiplooks.SHIPS:
        d = ship_cells(W, sid, box, assets)
        d.update({"name": name, "moored": moored, "faces": looks["ships"][sid]["faces"]})
        out["ships"][sid] = d
    with open(OUT, "w") as f:
        f.write("// Written by tools/shipview.py from John's world: the S.S. ANNE and the S.S. AQUA, every block of them, for the\n")
        f.write("// Interior Viewer (rooms70.js) in the very pictures the game draws them in (tools/shiplooks.py). Each block: x, y, z,\n")
        f.write("// its state (two bytes), its faces that show.\n")
        f.write("const SHIPS = " + json.dumps(out, separators=(",", ":")) + ";\n")
    print("bytes", os.path.getsize(OUT))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
