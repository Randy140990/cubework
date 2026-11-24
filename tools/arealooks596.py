"""
0.59.6's looks (John's 13:17 round, his Kanto test of 0.59.5), for tools/arealooks.py: round596() adds the rows (after
round594's) and post() is run on all the rows at the end (after arealooks57.finish).

- Every cut-through (the route gates and the Underground Path's huts): the "rock" rows the map scans laid over their
  andesite are gone, so the gates' own look (and the huts' cream brick) is drawn on them; the blocks at their ways in
  that still had their old pictures have the gates' look.
- Kanto's cliffs: the stripped spruce, birch and oak set in the rock with no look (tools/area_cliffwood596.json, from
  a scan of the map) are the cliffs' rock.
- The Bike Shop's back posts as its front ones; the bikes on the paving's slabs (the bikes' rows asked for smooth
  stone, the paving there is smooth stone slabs: they were never drawn).
- post(): the stripped logs and woods and the mushroom blocks drawn as our own whole block ("cube") wherever a swap
  gave them a picture, so nothing of theirs (a resource pack's connected textures, a mushroom's inside) can show.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


def gates():
    """The route gates (tools/area_gates.tsv): (x0, z0, x1, z1, roof height, where)."""
    out = []
    for line in open(os.path.join(HERE, "area_gates.tsv")):
        if not line.strip() or line.startswith("#"): continue
        x0, x1, z0, z1, ry, where = line.rstrip("\n").split("\t")
        out.append((int(x0), int(z0), int(x1), int(z1), int(ry), where))
    return out


def cut_throughs(G):
    """Every cut-through's box (x0, z0, x1, z1, y0, y1): the gates' own footprints and the huts'."""
    out = [(x0, z0, x1, z1, ry - 18, ry + 2) for x0, z0, x1, z1, ry, where in gates()]
    out += [(x - 8, z - 10, x + 8, z + 4, y - 2, y + 10) for x, y, z, note in G["UG_HUTS"]]
    return out


# The gates' blocks at their ways in that had no look (13:17: "it also has the old textures at the entrance"): the
# grey inner walls and floor the gates' dressed stone, the blue mat the stone too, the cyan band and the deepslate
# brick walls under the roof its dark rim, the warped trapdoors its beams, the white the gates' cream walls.
GATE_MORE = "light_gray_concrete,polished_tuff,smooth_stone_slab,smooth_stone,light_blue_wool,cyan_terracotta,warped_trapdoor,white_concrete,deepslate_brick_wall"
GATE_MORE_TO = ("block/light_gray_concrete=gate_stone,block/polished_tuff=gate_stone,block/smooth_stone=gate_stone,block/smooth_stone_slab_side=gate_stone,"
                "block/light_blue_wool=gate_stone,block/cyan_terracotta=gate_roof_rim,block/warped_trapdoor=gate_beam,block/white_concrete=gate_wall_birch,"
                "block/deepslate_bricks=gate_roof_rim")
# The huts' (21:58's look: a grey stone frame round the dark doorway, cream brick either side, white posts): the blue
# and warped frame round the doorway the grey stone, the slabs the stone, any stripped wood the white posts.
HUT_MORE = "light_blue_wool,warped_trapdoor,stripped_warped_stem,stripped_warped_hyphae,smooth_stone_slab,smooth_stone,stripped_spruce_wood,stripped_spruce_log,stripped_oak_wood,stripped_jungle_wood,white_concrete"
HUT_MORE_TO = ("block/light_blue_wool=gate_stone,block/warped_trapdoor=gate_stone,block/stripped_warped_stem=gate_stone,block/stripped_warped_stem_top=gate_stone,"
               "block/smooth_stone=gate_stone,block/smooth_stone_slab_side=gate_stone,block/stripped_spruce_log=gate_pillar,block/stripped_spruce_log_top=gate_pillar,"
               "block/stripped_oak_log=gate_pillar,block/stripped_oak_log_top=gate_pillar,block/stripped_jungle_log=gate_pillar,block/white_concrete=gate_pillar")


def round596(add, room, G):
    # ---------- every cut-through (13:17: "for all the cut throughs ... update the textures across the entire map") ----------
    for x0, z0, x1, z1, ry, where in gates():
        b = (x0 - 2, z0 - 4, x1 + 2, z1 + 4, where)
        if where == "League gate":
            add("swap", GATE_MORE.replace(",deepslate_brick_wall", ""), b, GATE_MORE_TO.replace(",block/deepslate_bricks=gate_roof_rim", ""), ys(ry - 18, ry + 2))
        else:
            add("swap", GATE_MORE, b, GATE_MORE_TO, ys(ry - 18, ry + 2))
    for x, y, z, note in G["UG_HUTS"]:
        add("swap", HUT_MORE, (x - 8, z - 10, x + 8, z + 4, note + " Underground Path hut"), HUT_MORE_TO, ys(y - 1, y + 10))

    # ---------- Kanto's cliffs (13:17, Diglett's Cave: "stripped spruce, potentially, on the sides ... retexturizing all") ----------
    for x0, z0, x1, z1, y0, y1, blocks in json.load(open(os.path.join(HERE, "area_cliffwood596.json"))):
        add("cube", blocks, (x0, z0, x1, z1, "the cliff's rock"), "hill_top|drip_rock@4", ys(y0, y1))

    # ---------- the Bike Shop (13:17: "the back pillars") ----------
    add("swap", "stripped_oak_wood,stripped_oak_log", (-377, 273, -366, 275, "the Bike Shop's back posts"),
        "block/stripped_oak_log=bike_post,block/stripped_oak_log_top=bike_post", ys(34, 37))


# Full blocks whose swapped looks are drawn as our own whole block (see post()).
FRAGILE = ("stripped_", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem")


def _fragile(b):
    return (b.startswith("stripped_") and b.endswith(("_wood", "_log", "_stem", "_hyphae"))) or b in ("brown_mushroom_block", "red_mushroom_block", "mushroom_stem")


def _textures(b):
    """A full block's pictures: (side, end)."""
    if b in ("brown_mushroom_block", "red_mushroom_block"): return ("block/" + b, "block/" + b)
    if b == "mushroom_stem": return ("block/mushroom_stem", "block/mushroom_stem")
    base = b[:-5] if b.endswith("_wood") else b[:-7] if b.endswith("_hyphae") else b
    if b.endswith("_wood"): base += "_log"
    if b.endswith("_hyphae"): base += "_stem"
    return ("block/" + base, "block/" + base + ("" if b.endswith(("_wood", "_hyphae")) else "_top"))


def _parse(v):
    """A swap's picture: (top, side, period)."""
    import re
    per = max([int(n) for n in re.findall(r"@(\d+)", v)] or [0])
    v = re.sub(r"@\d+", "", v).replace("!", "")
    if "|" in v:
        a, b = v.split("|", 1)
        return a.strip(), b.strip(), per
    return v.strip(), v.strip(), per


def post(rows, G):
    # Rock rows laid over a building's stone (the map scans took them for rocks: 13:17's house bases on Route 2 and by
    # the MUSEUM, the gates' andesite) or over any cut-through.
    skip = {tuple(b) for b in json.load(open(os.path.join(HERE, "area_rocks_skip596.json")))}
    cut = cut_throughs(G)
    BOULDER = G["BOULDER"]

    def hits(r, c):
        x0, z0, x1, z1 = int(r[2]), int(r[3]), int(r[4]), int(r[5])
        y0, y1 = (-999, 999) if r[6] == "-" else (int(r[6]), int(r[7]))
        return x0 <= c[2] and c[0] <= x1 and z0 <= c[3] and c[1] <= z1 and y0 <= c[5] and c[4] <= y1

    out = []
    dropped = 0
    for r in rows:
        if r[0] == "swap" and len(r) >= 10 and r[9] == "a rock" and r[8] == BOULDER:
            inner = (int(r[2]) + 1, int(r[3]) + 1, int(r[4]) - 1, int(r[5]) - 1, int(r[6]) + 1, int(r[7]) - 1)
            if inner in skip or any(hits(r, c) for c in cut):
                dropped += 1
                continue
        out.append(r)
    print("rock rows on buildings and cut-throughs left out:", dropped)

    # The fragile blocks' swaps as cubes: each swap row's stripped logs, woods and mushroom blocks taken out and given a
    # cube row (the same place and heights) in the same pictures.
    rows, out = out, []
    made = 0
    for r in rows:
        if r[0] != "swap" or r[8] in ("-", None):
            out.append(r)
            continue
        blocks = [b for b in r[1].split(",") if b]
        frag = [b for b in blocks if _fragile(b)]
        if not frag:
            out.append(r)
            continue
        pairs = dict(p.split("=", 1) for p in r[8].split(",") if "=" in p)
        groups = {}
        keep = [b for b in blocks if not _fragile(b)]
        for b in frag:
            side_tex, end_tex = _textures(b)
            sv = pairs.get(side_tex)
            ev = pairs.get(end_tex)
            if b.endswith(("_mushroom_block",)) and sv is None: sv = pairs.get("block/mushroom_block_inside")
            if sv is None and ev is None:
                keep.append(b)
                continue
            st, ss, sp = _parse(sv if sv is not None else ev)
            et, es, ep = _parse(ev if ev is not None else sv)
            # The ends' picture on the tops and bottoms (a standing post's), the sides' on the sides.
            top, side, per = (et if ev is not None else st), ss, max(sp, ep)
            spec = (top if top == side else top + "|" + side) + ("@%d" % per if per else "")
            groups.setdefault(spec, []).append(b)
        if keep:
            out.append((r[0], ",".join(keep)) + tuple(r[2:]))
        for spec, bs in groups.items():
            out.append(("cube", ",".join(bs)) + tuple(r[2:8]) + (spec,) + tuple(r[9:]))
            made += 1
    print("stripped wood and mushroom swaps drawn as whole blocks:", made)
    return out
