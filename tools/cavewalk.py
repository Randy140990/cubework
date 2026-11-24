"""
(0.61, John's 18:16: "there are some of these offshoots where you can see I've just got stuck ... near ladder areas")
Walks the caves as the mod's blocks have them (src/main/resources/retromon/interiors/<cave>.json and its .bin
floors), block by block, as you can move in the game: up a step of 0.6 at most (a slab, a half block) or onto stairs,
down any drop, never through anything solid, never into water (unless the room is one you swim in); the ladders and
holes (the rooms' warps) take you to where they land you.

From each way in, it finds every spot you can reach, then every spot you can reach that you can't get back out of
(no way to a ladder, a hole, the way in or the way out from there): where you'd be stuck. And the spots a ladder or
hole lands you on, if they're stuck.

    python3 -I tools/cavewalk.py mt_moon [rock_tunnel ...]
"""
import gzip, json, os, struct, sys
from collections import defaultdict, deque

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
PASS = ("air", "cave_air", "void_air", "light", "ladder", "vine", "glow_lichen", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush",
        "torch", "wall_torch", "soul_torch", "redstone_torch", "sugar_cane", "seagrass", "tall_seagrass", "kelp", "kelp_plant", "structure_void",
        "rail", "lever", "button", "tripwire", "cobweb", "sweet_berry_bush", "hanging_roots", "spore_blossom", "small_dripleaf", "leaf_litter",
        "pink_petals", "wildflowers", "short_dry_grass", "tall_dry_grass", "firefly_bush", "bush")
FLOWERS = ("dandelion", "poppy", "orchid", "allium", "bluet", "tulip", "daisy", "cornflower", "lily_of_the_valley", "torchflower", "sapling", "mushroom",
           "lilac", "rose_bush", "peony", "sunflower", "pitcher")
TALL = ("fence", "_wall", "fence_gate", "iron_bars", "pane")


def load_bin(path):
    data = gzip.open(path).read()
    W, H, L, n = struct.unpack(">hhhh", data[4:12])
    pos, pal = 12, []
    for _ in range(n):
        ln = struct.unpack(">h", data[pos:pos + 2])[0]; pos += 2
        pal.append(data[pos:pos + ln].decode()); pos += ln
    seq = []

    def varint():
        nonlocal pos
        v = sh = 0
        while True:
            c = data[pos]; pos += 1
            v |= (c & 0x7F) << sh; sh += 7
            if not c & 0x80: return v
    while pos < len(data):
        run = varint(); pid = varint()
        seq.extend([pid] * run)
    return W, H, L, pal, seq


def kind(name):
    """(what it is to walk on, its top): 'pass', 'water', 'tall' (a fence: 1.5, never stepped onto), 'stair', 'solid'."""
    b = name.replace("minecraft:", "").split("[")[0]
    st = name[len(name.split("[")[0]):]
    if b in PASS or any(f in b for f in FLOWERS) or b.endswith("_button") or b.endswith("_sign") or b.endswith("_banner") or b.endswith("pressure_plate") or b.endswith("_carpet") or b == "carpet":
        return "pass", 0.0
    if b in ("water", "bubble_column") or "lava" in b: return "water", 0.0
    if "waterlogged=true" in st and not b.endswith("slab") and not b.endswith("stairs"): return "water", 0.0
    if any(b.endswith(t) for t in TALL): return "tall", 1.5
    if b.endswith("_slab"): return ("solid", 0.5) if "type=bottom" in st else ("solid", 1.0)
    if b.endswith("_stairs"): return ("stair", 1.0) if "half=bottom" in st else ("solid", 1.0)
    if b == "snow": return "pass", 0.0
    return "solid", 1.0


def walk(cave):
    J = json.load(open(os.path.join(RES, cave + ".json")))
    rooms = J["rooms"]
    nodes = {}   # (rid, x, z) -> [surface heights]
    info = {}
    for rid, R in rooms.items():
        W, H, L, pal, seq = load_bin(os.path.join(RES, R["schem"]))
        K = [kind(n) for n in pal]
        swim = bool(R.get("swim"))
        at = (lambda K, seq, W, H, L: lambda x, y, z: K[seq[(y * L + z) * W + x]] if 0 <= y < H and 0 <= x < W and 0 <= z < L else ("solid", 1.0) if 0 <= y < H else ("pass", 0.0))(K, seq, W, H, L)
        cols = {}
        for z in range(L):
            for x in range(W):
                surf = []
                for y in range(H):
                    k, t = at(x, y, z)
                    if k in ("solid", "stair") or (k == "water" and swim):
                        s = y + (t if k != "water" else 0.9)
                        # Room for you over it: 1.8 blocks.
                        ok = True
                        for yy in range(y + 1, min(H, int(s + 1.8) + 1)):
                            kk, tt = at(x, yy, z)
                            if kk not in ("pass",) and not (kk == "water" and swim): ok = False; break
                        if ok:
                            # Water at your feet, in a room you don't swim in: not somewhere you can walk.
                            if not swim and y + 1 < H and at(x, y + 1, z)[0] == "water": continue
                            surf.append((s, k))
                if surf: cols[(x, z)] = surf
        info[rid] = (W, H, L, at, cols, R)
    # The graph.
    edges = defaultdict(set)
    def node_ids(rid, x, z):
        return [(rid, x, z, s) for s, _k in info[rid][4].get((x, z), [])]
    for rid, (W, H, L, at, cols, R) in info.items():
        for (x, z), surf in cols.items():
            for s, k in surf:
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    q = info[rid][4].get((x + dx, z + dz))
                    if not q: continue
                    best = None
                    for s2, k2 in q:
                        rise = s2 - s
                        # (Swimming, you climb out onto a bank a block over the water, as anyone can in Minecraft.)
                        if rise <= 0.6 + 1e-6 or (k2 == "stair" and rise <= 1.0 + 1e-6) or (k == "water" and rise <= 1.25):
                            if best is None or s2 > best[0]: best = (s2, k2)
                    if best is None: continue
                    s2 = best[0]
                    # Nothing in the way, from the lower of the two up to 1.8 over the higher.
                    top = max(s, s2) + 1.8
                    blocked = False
                    for yy in range(int(s2 + 0.001) + (1 if best[1] != "water" else 1), int(top - 0.001) + 1):
                        kk, tt = at(x + dx, yy, z + dz)
                        if kk in ("solid", "stair", "tall") and yy + 0.0 < top and yy + tt > s2 + 0.001: blocked = True; break
                    if not blocked: edges[(rid, x, z, s)].add((rid, x + dx, z + dz, s2))
    # Warps: ladders and holes.
    def landing(rid, sx, sz, sy=0):
        # (The mod puts you at the room's floor level, plus the spot's own height: Interiors.go.)
        want = -rooms[rid].get("base", 0) + sy
        x, z = int(sx), int(sz)
        for d in range(0, 3):
            best = None
            for dz in range(-d, d + 1):
                for dx in range(-d, d + 1):
                    for n in node_ids(rid, x + dx, z + dz):
                        if n[3] <= want + 0.6 and (best is None or abs(n[3] - want) < abs(best[3] - want)): best = n
            if best: return best
        return None
    exits = set()
    lands = []
    for rid, (W, H, L, at, cols, R) in info.items():
        for w in R.get("warps", []):
            if "z0" not in w:
                # A mat: the way out (walk south off it).
                if w.get("kind") == "mat":
                    for x in range(int(w["x0"]), int(w["x1"] + 0.999)):
                        for z in range(int(w["z"]) - 2, int(w["z"]) + 1):
                            for n in node_ids(rid, x, z): exits.add(n)
                continue
            tgt = landing(w["to"], w["spawn"]["x"], w["spawn"]["z"], w["spawn"].get("y", 0)) if w.get("to") in info and w.get("spawn") else None
            if tgt: lands.append((rid, w, tgt))
            # (A ladder up's box is on the ladder, in the wall: you take it from the block at its foot.)
            ex = 1 if w.get("kind") == "up" else 0
            for x in range(int(w["x0"]) - ex, int(w.get("x1", w["x0"] + 1) + 0.999) + ex):
                for z in range(int(w["z0"]), int(w["z1"] + 0.999) + ex):
                    for n in node_ids(rid, x, z):
                        if tgt: edges[n].add(tgt)
                        else: exits.add(n)
    entries = []
    for dk, d in J.get("doors", {}).items():
        if "in" not in d: d["in"] = rooms[d["room"]].get("entry") or rooms[d["room"]]["spawn"]
        n = landing(d["room"], d["in"]["x"], d["in"]["z"], d["in"].get("y", 0))
        if n:
            entries.append((dk, n))
            # Back the way you came in: the door.
            for dz in range(-2, 3):
                for dx in range(-2, 3):
                    for m in node_ids(d["room"], int(d["in"]["x"]) + dx, int(d["in"]["z"]) + dz): exits.add(m)
    # Reach from the ways in; and back to a way out.
    seen = set()
    todo = deque(n for _k, n in entries)
    seen.update(todo)
    while todo:
        a = todo.popleft()
        for b in edges.get(a, ()):
            if b not in seen: seen.add(b); todo.append(b)
    rev = defaultdict(set)
    for a, bs in edges.items():
        for b in bs: rev[b].add(a)
    good = set(exits)
    # A spot is fine if from it you can get to a warp (anything that moves you on) or a way out.
    for a in list(edges):
        for b in edges[a]:
            if a[0] != b[0]: good.add(a)
    todo = deque(good)
    while todo:
        b = todo.popleft()
        for a in rev.get(b, ()):
            if a not in good: good.add(a); todo.append(a)
    stuck = [n for n in seen if n not in good]
    # Group them.
    groups = []
    left = set(stuck)
    while left:
        n = left.pop(); g = [n]; st = [n]
        while st:
            a = st.pop()
            for b in list(edges.get(a, ())) + list(rev.get(a, ())):
                if b in left: left.discard(b); g.append(b); st.append(b)
        groups.append(g)
    print("%s: %d spots reachable from %s; %d stuck in %d places" % (cave, len(seen), [k for k, _ in entries], len(stuck), len(groups)))
    for g in sorted(groups, key=len, reverse=True)[:25]:
        rid = g[0][0]; xs = [n[1] for n in g]; zs = [n[2] for n in g]; ss = [n[3] for n in g]
        print("  %-22s %4d spots  x %d..%d  z %d..%d  at %.1f..%.1f" % (rid, len(g), min(xs), max(xs), min(zs), max(zs), min(ss), max(ss)))
    # The things to get to: items, people (trainers), POKéMON: within a step of somewhere you can reach.
    miss = []
    for rid, R in rooms.items():
        for t in R.get("items", []) + R.get("people", []) + R.get("mons", []):
            x, z = int(t["x"]), int(t["z"])
            ok = any((rid, x + dx, z + dz, s2) in seen for dx in (-1, 0, 1) for dz in (-1, 0, 1) for s2, _k in info[rid][4].get((x + dx, z + dz), []))
            if not ok: miss.append((rid, t.get("key"), x, z))
    print("  can't get to:", miss)
    bad_lands = [(r, w["kind"], w.get("to"), t) for r, w, t in lands if t in seen and t not in good]
    for r, k, to, t in bad_lands: print("  a %s in %s lands you stuck in %s at %d,%d" % (k, r, to, t[1], t[2]))
    return seen, good, groups, info, edges


if __name__ == "__main__":
    for c in sys.argv[1:]: walk(c)
