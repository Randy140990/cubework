#!/usr/bin/env python3
"""
(0.67.1, John's 17:13) The two ships retextured in the game: the S.S. ANNE (Vermilion) and the S.S. AQUA (Olivine,
the same build). "both ships obviously get the retouch ... retexturize the shell ... The doors, the outside, the
whites, the blacks, the large areas, the metallics, etc. Glass, lifeboats, all of it. So when I'm in my camera and I'm
getting some screenshots, it'll be retextured ... Even if that means you retexture and touch up the inside, I do not
care."

Every kind of block in each ship keeps its own colour (its own picture's), drawn in the family of our looks it
belongs to:
- painted steel and stone: one clean colour, laid four blocks across, a soft unevenness and no block lines;
- timber: clean boards, four to a block;
- metal: brushed, with a soft sheen, laid two blocks across;
- glass: tinted, with soft streaks of light (the panes see-through as Minecraft's are; plain glass solid tinted);
- lamps: lit, clean;
- anything with holes in its picture (doors, bars, trapdoors, ladders, rails): the same shape, its colour cleaned.

Writes, from John's world and Minecraft's own textures and models:
- src/main/resources/assets/retromon/textures/block/area/ship_<texture>.png (the pictures);
- tools/area_ships67.json (the swaps for arealooks67: which blocks, where, old picture = new);
- tools/interior/ship_data.js (both ships for the Interior Viewer, drawn in the same pictures: rooms70.js).

    python3 tools/shiplooks.py <John's world folder> <Minecraft's assets folder (assets/minecraft)>
"""
import base64
import collections
import io
import json
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import spotworld  # noqa: E402
import shipview as SV  # noqa: E402  (the blocks' kinds and shapes: what's a whole block, what isn't the ship)

TEX_OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "textures", "block", "area")
SHIPS = [
    # id, name, where it's moored, the box it's read from (x0 x1 y0 y1 z0 z1), how the swaps are sliced
    ("anne", "S.S. ANNE", "Vermilion's harbour", (-460, -262, 18, 72, 616, 666), "x"),
    ("aqua", "S.S. AQUA", "Olivine's harbour", (-2310, -2258, 5, 70, 650, 870), "z"),
]
# What isn't the ship though it's a block the ship has too: under the S.S. AQUA, the sea bed's stone (its top at y 13),
# which runs under it and would join on.
SEABED = {"aqua": (14, {"stone"})}


def ship_mask(sid, bn, ids, y0):
    """The ship's blocks in its box: the biggest piece of touching blocks, less the sea and the sea bed."""
    from scipy import ndimage  # noqa
    skip = np.array([SV.skipped(b) for b in bn])
    S = ~skip[ids]
    if sid in SEABED:
        below, blocks = SEABED[sid]
        bad = np.array([b in blocks for b in bn])[ids]
        k = max(0, below - y0)
        S[:k] &= ~bad[:k]
    lab, n = ndimage.label(S, structure=np.ones((3, 3, 3)))
    sizes = ndimage.sum(S, lab, range(1, n + 1))
    return lab == (1 + int(np.argmax(sizes)))
# Drawn by the game as things, not blocks (their pictures aren't the blocks' atlas): left as they are.
ENTITY = ("_sign", "_banner", "_head", "_skull", "_bed", "chest", "shulker_box", "conduit", "bell")


# ---------- Minecraft's models: which pictures each block uses, and which is its top, bottom and side ----------
class Models:
    def __init__(self, assets):
        self.a = assets
        self.cache = {}

    def model(self, name):
        name = name.replace("minecraft:", "")
        if name in self.cache: return self.cache[name]
        f = os.path.join(self.a, "models", name + ".json")
        if not os.path.exists(f):
            self.cache[name] = ({}, [])
            return self.cache[name]
        m = json.load(open(f))
        tex, els = {}, None
        if "parent" in m and not m["parent"].startswith("builtin/"):
            ptex, pels = self.model(m["parent"])
            tex.update(ptex)
            els = pels
        tex.update(m.get("textures", {}))
        if "elements" in m: els = m["elements"]
        self.cache[name] = (tex, els or [])
        return self.cache[name]

    @staticmethod
    def resolve(tex, ref, depth=0):
        while ref and ref.startswith("#") and depth < 10:
            ref = tex.get(ref[1:])
            depth += 1
        return ref.replace("minecraft:", "") if ref and not ref.startswith("#") else None

    def block_models(self, b):
        f = os.path.join(self.a, "blockstates", b + ".json")
        if not os.path.exists(f): return []
        s = json.load(open(f))
        out = []
        def add(v):
            for x in (v if isinstance(v, list) else [v]): out.append(x["model"])
        for v in s.get("variants", {}).values(): add(v)
        for part in s.get("multipart", []): add(part["apply"])
        return list(dict.fromkeys(out))

    def textures(self, b):
        """All the pictures a block's models use, and its top, bottom and side (of its biggest piece)."""
        used, faces = [], {}
        for mn in self.block_models(b):
            tex, els = self.model(mn)
            best, vol = None, -1
            for e in els:
                fr, to = e["from"], e["to"]
                v = (to[0] - fr[0] + .01) * (to[1] - fr[1] + .01) * (to[2] - fr[2] + .01)
                for d, fc in e.get("faces", {}).items():
                    t = self.resolve(tex, fc.get("texture"))
                    if t and t not in used: used.append(t)
                if v > vol: best, vol = e, v
            if best and not faces:
                for d, fc in best.get("faces", {}).items():
                    t = self.resolve(tex, fc.get("texture"))
                    if t: faces[d] = t
        side = faces.get("north") or faces.get("south") or faces.get("east") or faces.get("west") or (used[0] if used else None)
        return used, {"top": faces.get("up") or side, "bottom": faces.get("down") or side, "side": side}


# ---------- our pictures ----------
def clean(c):
    """A kind's own colour made clean: near-whites whiter and even, the rest a touch richer (as rooms70.js had it)."""
    l = 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]
    mx, mn = max(c), min(c)
    sat = (mx - mn) / mx if mx else 0
    if l > 178 and sat < 0.16:
        return tuple(int(round(c[i] + (t - c[i]) * 0.55)) for i, t in enumerate((238, 238, 233)))
    return tuple(max(0, min(255, int(round(l + (c[i] - l) * 1.12 + 4)))) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, int(round(v + (255 - v) * k if k >= 0 else v * (1 + k))))) for v in c)


def hash01(*a):
    h = 2166136261
    for v in a:
        h = ((h ^ (v & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return (h & 0xFFFF) / 65535.0


def pic_paint(c, seed):
    """Painted steel or stone: one clean colour, a soft unevenness over four blocks, no block lines (seamless)."""
    n = 64
    ph = [hash01(seed, i) * 2 * math.pi for i in range(3)]
    im = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            w = (math.sin(2 * math.pi * (x + y) / n + ph[0]) + math.sin(2 * math.pi * (2 * x - y) / n + ph[1]) * 0.6 + math.sin(2 * math.pi * (x - 2 * y) / n + ph[2]) * 0.4) / 2.0
            k = round(w * 0.016 / 0.004) * 0.004  # (in small steps, so it stays clean)
            im.putpixel((x, y), shade(c, k) + (255,))
    return im, 4


def pic_wood(c, seed):
    """Timber: clean boards, four to a block, each with its lit edge and a soft line under it."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.1 if y % 4 == 0 else -0.16 if y % 4 == 3 else (round((hash01(seed, y // 4, x // 8) - 0.5) * 0.04 / 0.01) * 0.01)
            im.putpixel((x, y), shade(c, k) + (255,))
    return im, 0


def pic_metal(c, seed):
    """Metal: brushed along it, a soft sheen across, laid two blocks across."""
    n = 32
    im = Image.new("RGBA", (n, n))
    for y in range(n):
        s = round((hash01(seed, y) - 0.5) * 0.05 / 0.01) * 0.01
        for x in range(n):
            b = 0.5 + 0.5 * math.sin((x + y) / n * 2 * math.pi)
            k = s + round(0.14 * b ** 8 / 0.02) * 0.02 - 0.03
            im.putpixel((x, y), shade(c, k) + (255,))
    return im, 2


def pic_glass(c, dark, alpha):
    """Glass: tinted (the light off the sea on it), soft streaks of light, laid two blocks across."""
    n = 32
    g = tuple(int(round(c[i] + (t - c[i]) * (0.55 if dark else 0.35))) for i, t in enumerate((24, 38, 58) if dark else (150, 190, 214)))
    im = Image.new("RGBA", (n, n))
    a = int(round(alpha * 255))
    for y in range(n):
        for x in range(n):
            d = (x + y) % n
            k = 0.26 if d in (0, 1) else 0.14 if d in (5,) else 0.0
            im.putpixel((x, y), shade(g, k) + (a,))
    return im, 2


def pic_glow(c, seed):
    g = tuple(int(round(c[i] + (t - c[i]) * 0.5)) for i, t in enumerate((255, 248, 230)))
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), shade(g, 0.05 if 3 <= x <= 12 and 3 <= y <= 12 else -0.04) + (255,))
    return im, 0


def pic_masked(src, c):
    """A picture with holes (a door's windows, bars, rails, a trapdoor's gaps): its own shape and its own light and
    dark, softened, on the clean colour."""
    im = src.convert("RGBA")
    w, h = im.size
    px = np.array(im).astype(float)
    a = px[:, :, 3]
    lum = 0.3 * px[:, :, 0] + 0.59 * px[:, :, 1] + 0.11 * px[:, :, 2]
    m = lum[a > 8].mean() if (a > 8).any() else 128
    out = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            if a[y, x] <= 8:
                out.putpixel((x, y), (0, 0, 0, 0))
                continue
            k = max(-0.35, min(0.35, 0.45 * (lum[y, x] - m) / max(40.0, m)))
            k = round(k / 0.05) * 0.05
            out.putpixel((x, y), shade(c, k) + (int(a[y, x]),))
    return out, 0


def avg_colour(im):
    a = np.array(im.convert("RGBA"))
    w = a.shape[1]
    a = a[:w, :w].reshape(-1, 4)  # (an animated strip: its first frame)
    a = a[a[:, 3] > 20]
    return tuple(int(v) for v in a[:, :3].mean(0)) if len(a) else (200, 200, 200)


def family_of(block, tex):
    if "glass" in tex or "glass" in block: return "glass"
    if block in ("sea_lantern", "end_rod", "glowstone", "shroomlight", "redstone_lamp", "lantern", "ochre_froglight", "pearlescent_froglight", "verdant_froglight"): return "glow"
    if block.endswith("_leaves") or block in ("cobweb",) or "coral" in block or block in ("bamboo", "fire", "campfire"): return None   # (left as they are)
    if block.startswith("iron_") or block in ("anvil", "chain", "cauldron", "hopper", "grindstone", "polished_basalt", "lightning_rod", "tripwire_hook", "rail", "activator_rail", "lever"): return "metal"
    woods = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped")
    if any(block.startswith(w + "_") or block.startswith("stripped_" + w) for w in woods) or block in ("bookshelf", "barrel"): return "wood"
    return "paint"


def main(world, assets):
    W = spotworld.World(world)
    M = Models(assets)
    texdir = os.path.join(assets, "textures")
    pics = {}        # new picture name -> (PIL image, period)
    mapping = {}     # old texture ("block/x") -> new picture name
    per = {}         # new picture -> period
    rows = []
    viewer = {"ships": {}, "pics": {}}
    for sid, name, moored, box, slice_ax in SHIPS:
        x0, x1, y0, y1, z0, z1 = box
        names, ids = W.box(x0, x1, y0, y1, z0, z1)
        bn = [SV.bname(n) for n in names]
        ship = ship_mask(sid, bn, ids, y0)
        zz = np.nonzero(ship)
        bx = (int(zz[2].min()) + x0, int(zz[2].max()) + x0, int(zz[0].min()) + y0, int(zz[0].max()) + y0, int(zz[1].min()) + z0, int(zz[1].max()) + z0)
        kinds = sorted(set(bn[i] for i in np.unique(ids[ship]).tolist()))
        swapped, faces = [], {}
        for b in kinds:
            if b.endswith(ENTITY) or b in ENTITY: continue
            used, fc = M.textures(b)
            if not used: continue
            fam = None
            for t in used:
                fam = family_of(b, t)
                if fam is None: break
                if t in mapping: continue
                f = os.path.join(texdir, t + ".png")
                if not os.path.exists(f): continue
                src = Image.open(f)
                c = clean(avg_colour(src))
                arr = np.array(src.convert("RGBA"))[: src.size[0]]
                holes = (arr[:, :, 3] < 250).any()
                key = "ship_" + t.split("/")[-1]
                seed = sum(ord(ch) for ch in key)
                if fam == "glass":
                    stained = "stained" in t
                    dark = any(t.split("/")[-1].startswith(p) for p in ("black_", "gray_", "blue_", "cyan_", "brown_", "tinted")) or t.endswith("/glass") or t.endswith("/glass_pane_top")
                    im, p = pic_glass(c, dark, (0.88 if dark else 0.62) if stained else 1.0)
                elif holes: im, p = pic_masked(src, c)
                elif fam == "wood": im, p = pic_wood(c, seed)
                elif fam == "metal": im, p = pic_metal(c, seed)
                elif fam == "glow": im, p = pic_glow(c, seed)
                else: im, p = pic_paint(c, seed)
                pics[key] = (im, p)
                mapping[t] = key
            if fam is None: continue
            if not all(t in mapping for t in used if os.path.exists(os.path.join(texdir, t + ".png"))): continue
            swapped.append(b)
            faces[b] = {k: mapping.get(v) for k, v in fc.items() if v}
        # The swaps: every swapped block in slices along the ship (small enough to win over the places round it).
        spec_tex = sorted(set(t for b in swapped for t in M.textures(b)[0] if t in mapping))
        spec = ",".join("%s=%s%s" % (t, mapping[t], "@%d" % pics[mapping[t]][1] if pics[mapping[t]][1] else "") for t in spec_tex)
        lo, hi = (bx[0], bx[1]) if slice_ax == "x" else (bx[4], bx[5])
        bands = [(bx[2], bx[3], swapped)]
        if sid in SEABED and bx[2] < SEABED[sid][0]:
            below, blocks = SEABED[sid]
            bands = [(bx[2], below - 1, [b for b in swapped if b not in blocks]), (below, bx[3], swapped)]
        for a in range(lo, hi + 1, 16):
            b2 = min(hi, a + 15)
            area = (a, bx[4], b2, bx[5]) if slice_ax == "x" else (bx[0], a, bx[1], b2)
            for ya, yb, bl in bands:
                rows.append({"blocks": ",".join(bl), "box": list(area), "ys": [ya, yb], "spec": spec, "note": "the %s, retextured" % name})
        kept = [b for b in kinds if b not in swapped]
        print(name, "at", bx, "kinds", len(kinds), "retextured", len(swapped), "left as they are", kept)
        viewer["ships"][sid] = {"name": name, "moored": moored, "box": bx, "faces": faces}
    # Write the pictures.
    for key, (im, p) in pics.items():
        im.save(os.path.join(TEX_OUT, key + ".png"))
        buf = io.BytesIO()
        im.save(buf, "PNG", optimize=True)
        viewer["pics"][key] = {"png": base64.b64encode(buf.getvalue()).decode(), "per": p or 1, "alpha": int(np.array(im)[:, :, 3].min()) < 255,
                               "cut": int(np.array(im)[:, :, 3].min()) == 0}
    json.dump({"rows": rows}, open(os.path.join(HERE, "area_ships67.json"), "w"), indent=0)
    json.dump(viewer, open(os.path.join(HERE, "interior", "ship_looks.json"), "w"))
    print("pictures", len(pics), "rows", len(rows), "viewer bytes", sum(len(v["png"]) for v in viewer["pics"].values()))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
