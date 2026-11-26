#!/usr/bin/env python3
"""
A picture of a piece of John's world as the game draws it with our looks, to check a place's outside without going
in game (26 Nov 2026, John's 09:20: "a pass across all of Johto and touch up any external materials").

An isometric view from one corner (se, sw, ne or nw), every block as a cube (slabs, carpets and snow lower, plants as
a small sprite), each face in the picture the game would give it:
- Minecraft's own (from its models), or the look pack's in its place;
- then our looks (area_looks.tsv) over it: the swaps family (swap, panel, glass, hedge, cube, keep, hide; the one in
  the smallest area wins, as in the game), laid over one block or several (@N) as the game lays them, a "top|side"
  pair, a cube's own pictures; a roof's tiles on its stairs and slabs.
Grass, leaves and water take a plain biome green or blue. The overlays (the grass's and moss's edges), props and
signs' boards aren't drawn: it's for seeing materials, not for a likeness.

    python3 tools/lookview.py <world> <assets/minecraft> x0 x1 z0 z1 out.png [view=se] [px=12] [y0=14] [y1=140]
"""
import collections
import functools
import json
import math
import os
import re
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import spotworld  # noqa: E402
import shiplooks  # noqa: E402

ROOT = os.path.join(HERE, "..", "src", "main", "resources")
PACK = os.path.join(ROOT, "resourcepacks", "retromon_look", "assets", "minecraft", "textures")
OURS = os.path.join(ROOT, "assets", "retromon", "textures", "block", "area")
TSV = os.path.join(ROOT, "retromon", "data", "area_looks.tsv")
FAMILY = {"swap", "panel", "glass", "hedge", "leafy", "leafy_hedge", "sign", "sign_o", "sign_b", "keep", "hide", "cube"}
AIR = {"air", "cave_air", "void_air", "barrier", "light", "structure_void"}
PLANT = ("short_grass", "tall_grass", "fern", "large_fern", "dandelion", "poppy", "tulip", "daisy", "bluet", "orchid", "allium", "cornflower", "lily_of_the_valley",
         "_sapling", "dead_bush", "sweet_berry_bush", "wildflowers", "pink_petals", "torchflower", "sugar_cane", "seagrass", "kelp", "vine", "glow_lichen", "lilac",
         "rose_bush", "peony", "sunflower", "bush", "leaf_litter", "firefly_bush", "short_dry_grass", "tall_dry_grass", "pitcher", "spore_blossom", "hanging_roots",
         "torch", "lantern", "_button", "lever", "tripwire", "rail", "redstone_wire", "_sign", "_banner", "_head", "_skull", "flower_pot", "potted_", "candle", "chain",
         "_bars", "_pane", "_rod", "ladder", "scaffolding", "cobweb", "_coral", "small_dripleaf", "big_dripleaf", "mushroom", "fungus", "roots", "sprouts", "lily_pad")
TINT_GRASS = (124, 189, 89)
TINT_LEAF = (96, 161, 52)
TINT_BIRCH = (128, 167, 85)
TINT_SPRUCE = (97, 153, 97)
TINT_WATER = (63, 118, 228)
TINTED = re.compile(r"^(grass_block_top|grass_block_side_overlay|short_grass|tall_grass_(top|bottom)|fern|large_fern_(top|bottom)|vine|lily_pad|sugar_cane|"
                    r"(oak|jungle|acacia|dark_oak|mangrove|birch|spruce)_leaves|water_still|water_flow)$")


def bname(n):
    return n.replace("minecraft:", "").split("[")[0]


def props(n):
    if "[" not in n: return {}
    return dict(kv.split("=") for kv in n.split("[", 1)[1].rstrip("]").split(","))


MISSING = collections.Counter()


class Pictures:
    def __init__(self, assets):
        self.van = os.path.join(assets, "textures")
        self.cache = {}

    def get(self, ref):
        """A picture by its name: "block/x" (the look pack's, else Minecraft's) or "area:x" (ours)."""
        if ref in self.cache: return self.cache[ref]
        im = None
        if ref.startswith("area:"):
            n = ref[5:]
            for f in (os.path.join(OURS, n + ".png"), os.path.join(OURS, n + "_0.png")):
                if os.path.exists(f): im = Image.open(f).convert("RGBA"); break
        else:
            for f in (os.path.join(PACK, ref + ".png"), os.path.join(self.van, ref + ".png")):
                if os.path.exists(f): im = Image.open(f).convert("RGBA"); break
            if im is not None:
                w = im.size[0]
                if im.size[1] > w: im = im.crop((0, 0, w, w))  # (an animated strip: its first frame)
                name = ref.split("/")[-1]
                if TINTED.match(name):
                    tint = (TINT_WATER if "water" in name else TINT_BIRCH if "birch" in name else TINT_SPRUCE if "spruce" in name
                            else TINT_LEAF if "leaves" in name else TINT_GRASS)
                    a = np.array(im).astype(float)
                    a[:, :, :3] *= np.array(tint) / 255.0
                    im = Image.fromarray(a.clip(0, 255).astype(np.uint8))
        if im is None:
            MISSING[ref] += 1
            im = Image.new("RGBA", (16, 16), (255, 0, 255, 255))
        self.cache[ref] = im
        return im


class Looks:
    """Our looks over a box: for a block at a place, the swaps family's winner and the roofs."""
    def __init__(self, box):
        x0, x1, z0, z1 = box
        self.by = collections.defaultdict(list)
        for line in open(TSV):
            if line.startswith("#") or not line.strip(): continue
            r = line.rstrip("\n").split("\t")
            if len(r) < 9 or (len(r) > 10 and r[10].strip() == "interiors"): continue
            a0, b0, a1, b1 = int(r[2]), int(r[3]), int(r[4]), int(r[5])
            if a1 < x0 or a0 > x1 or b1 < z0 or b0 > z1: continue
            ya, yb = (-999, 999) if r[6] == "-" else (int(r[6]), int(r[7]))
            size = (a1 - a0 + 1) * (b1 - b0 + 1) * (400 if r[6] == "-" else yb - ya + 1)
            for b in r[1].split(","):
                self.by[b].append((a0, b0, a1, b1, ya, yb, r[0], r[8], size))

    @functools.lru_cache(maxsize=None)
    def rules(self, b):
        return self.by.get(b, [])

    def at(self, b, x, y, z):
        win, roof = None, None
        for (a0, b0, a1, b1, ya, yb, kind, spec, size) in self.rules(b):
            if not (a0 <= x <= a1 and b0 <= z <= b1 and ya <= y <= yb): continue
            if kind in FAMILY:
                if win is None or (size, spec) < (win[2], win[1]): win = (kind, spec, size)
            elif kind.startswith("roof"):
                roof = kind
            else:
                OTHER.add((b, kind))
        return win, roof


@functools.lru_cache(maxsize=None)
def parse_swaps(spec):
    out = {}
    for one in spec.split(","):
        if "=" not in one: continue
        a, b = one.split("=", 1)
        per = max([int(m) for m in re.findall(r"@(\d+)", b)] or [0])
        b = re.sub(r"@\d+", "", b).replace("!", "").strip()
        top, side = (b.split("|", 1) + [None])[:2]
        out[a.strip().replace("minecraft:", "")] = (top.strip(), side.strip() if side else None, per)
    return out


# (HILITE=1: a face still in Minecraft's own picture, on something built (not the ground, trees, plants or water),
# washed red, to find what our looks have missed.)
HILITE = os.environ.get("HILITE") == "1"
NODRAW = os.environ.get("NODRAW") == "1"  # (with HILITE: only the list of what's missed, no picture)
NATURAL = ("grass_block", "dirt", "_leaves", "oak_log", "oak_wood", "birch_log", "birch_wood", "spruce_log", "spruce_wood", "dripstone", "water", "sand", "moss",
           "mud", "coarse_dirt", "rooted_dirt", "podzol", "dirt_path", "snow", "ice", "clay", "gravel", "lily_pad", "mycelium", "farmland", "azalea")
LAST = {}
HIT = collections.defaultdict(list)
OTHER = set()   # (a block, a look of ours that isn't a picture swap: a prop, a chain, an overlay) seen


def face_pic(P, base_ref, face, look, roof, wx, wy, wz):
    im = _face_pic(P, base_ref, face, look, roof, wx, wy, wz)
    LAST["ours"] = (im is not P.get(base_ref) or base_ref.split("/")[-1] in PACK_NAMES
                    or (look is not None and look[0] in ("sign", "sign_o", "sign_b", "keep", "leafy", "leafy_hedge")))
    return im


PACK_NAMES = set(f[:-4] for f in os.listdir(os.path.join(PACK, "block")) if f.endswith(".png")) if os.path.isdir(os.path.join(PACK, "block")) else set()


def _face_pic(P, base_ref, face, look, roof, wx, wy, wz):
    """The picture (a PIL image, 16 by 16 or so) on one face of a block: face is 'top', 'south' or 'east' etc."""
    horiz = face not in ("top", "bottom")
    if look:
        kind, spec, _ = look
        if kind == "cube":
            per = max([int(m) for m in re.findall(r"@(\d+)", spec)] or [0])
            s = re.sub(r"@\d+", "", spec).replace("!", "").split("|")
            ref = "area:" + (s[1] if horiz and len(s) > 1 else s[0]).strip()
            return sub(P.get(ref), per, face, wx, wy, wz)
        if kind in ("swap", "panel", "glass", "hedge"):
            sw = parse_swaps(spec).get(base_ref)
            if sw:
                top, side, per = sw
                ref = "area:" + (side if side and horiz else top)
                return sub(P.get(ref), per, face, wx, wy, wz)
    if roof:
        return sub(P.get("area:" + roof + "_tiles"), 0, face, wx, wy, wz)
    return P.get(base_ref)


def sub(im, per, face, wx, wy, wz):
    """A picture laid over per blocks (as the game lays an @N one): this block's part of it."""
    if per <= 0: return im
    w = im.size[0] // per
    if face in ("top", "bottom"): s, t = wx % per, wz % per
    elif face in ("south", "north"): s, t = wx % per, (-wy - 1) % per
    else: s, t = wz % per, (-wy - 1) % per
    return im.crop((s * w, t * w, s * w + w, t * w + w))


def LOCAL(L, b, x, y, z):
    """Our looks over this block here that aren't picture swaps (a prop, a chain, an overlay, a board)."""
    return [k for (a0, b0, a1, b1, ya, yb, k, spec, size) in L.rules(b) if a0 <= x <= a1 and b0 <= z <= b1 and ya <= y <= yb and k not in FAMILY and not k.startswith("roof")
            and k not in ("grass_edge", "grass_onto", "moss_onto", "sand_edge", "kerb")]


_RED = {}


def red(im):
    if id(im) not in _RED:
        a = np.array(im).astype(float)
        a[:, :, 0] = a[:, :, 0] * 0.4 + 255 * 0.6
        a[:, :, 1] *= 0.35
        a[:, :, 2] *= 0.35
        _RED[id(im)] = (Image.fromarray(a.clip(0, 255).astype(np.uint8)), im)
    return _RED[id(im)][0]


class Iso:
    def __init__(self, px):
        self.B = px
        self.cache = {}

    def face(self, im, kind, shade, h=1.0):
        """A face's picture warped to the screen: kind 'top', 'left' (the south face from se) or 'right'."""
        key = (id(im), kind, shade, h)
        if key in self.cache: return self.cache[key][0]
        B = self.B
        src = im.resize((32, 32), Image.NEAREST)
        if shade != 1.0:
            a = np.array(src).astype(float); a[:, :, :3] *= shade; src = Image.fromarray(a.clip(0, 255).astype(np.uint8))
        if kind == "top":
            # (0,0)->(B,0) top corner; u along +x goes right-down, v along +z goes left-down
            W, H = 2 * B, B
            out = src.transform((W, H), Image.AFFINE, self._inv([(0, 0, B, 0), (32, 0, 2 * B, B / 2), (0, 32, 0, B / 2)]), Image.NEAREST)
        elif kind == "left":
            hh = B * h
            W, H = B, int(math.ceil(B / 2 + hh))
            out = src.crop((0, int(32 * (1 - h)), 32, 32)).resize((32, max(1, int(32 * h))), Image.NEAREST) if h < 1 else src
            out = out.transform((W, H), Image.AFFINE, self._inv([(0, 0, 0, 0), (32, 0, B, B / 2), (0, out.size[1], 0, hh)]), Image.NEAREST)
        else:
            hh = B * h
            W, H = B, int(math.ceil(B / 2 + hh))
            out = src.crop((0, int(32 * (1 - h)), 32, 32)).resize((32, max(1, int(32 * h))), Image.NEAREST) if h < 1 else src
            out = out.transform((W, H), Image.AFFINE, self._inv([(0, 0, 0, B / 2), (32, 0, B, 0), (0, out.size[1], 0, B / 2 + hh)]), Image.NEAREST)
        self.cache[key] = (out, im)
        return out

    @staticmethod
    def _inv(pts):
        """The inverse affine (screen -> picture) from three picture->screen point pairs."""
        (u0, v0, x0, y0), (u1, v1, x1, y1), (u2, v2, x2, y2) = pts
        A = np.array([[x0, y0, 1], [x1, y1, 1], [x2, y2, 1]], float)
        a = np.linalg.solve(A, np.array([u0, u1, u2], float))
        b = np.linalg.solve(A, np.array([v0, v1, v2], float))
        return (a[0], a[1], a[2], b[0], b[1], b[2])


VIEWS = {
    # view: (turn of the world so the camera's at +x +z, which world faces show as 'left' (screen south) and 'right')
    "se": (lambda x, z: (x, z), "south", "east"),
    "sw": (lambda x, z: (-z, x), "west", "south"),
    "nw": (lambda x, z: (-x, -z), "north", "west"),
    "ne": (lambda x, z: (z, -x), "east", "north"),
}
STEP = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}


def render(world, assets, x0, x1, z0, z1, out, view="se", px=12, y0=14, y1=140):
    W = spotworld.World(world)
    M = shiplooks.Models(assets)
    P = Pictures(assets)
    L = Looks((x0, x1, z0, z1))
    iso = Iso(px)
    names, ids = W.box(x0, x1, y0, y1, z0, z1)
    bn = [bname(n) for n in names]
    pr = [props(n) for n in names]
    Y, Z, X = ids.shape
    air = np.array([b in AIR for b in bn])[ids]
    plant = np.array([b.endswith(PLANT) or b.startswith(PLANT) or any(p in b for p in ("tulip", "orchid")) for b in bn])
    water = np.array([b in ("water", "bubble_column") for b in bn])
    low = {}
    for i, (b, p) in enumerate(zip(bn, pr)):
        if b.endswith("_slab") and p.get("type") != "double": low[i] = 0.5
        elif b.endswith("_carpet") or b in ("snow",) and p.get("layers", "1") == "1": low[i] = 0.125
        elif b == "snow": low[i] = int(p.get("layers", "1")) / 8
        elif b.endswith(("_trapdoor", "_pressure_plate")) and p.get("open") != "true": low[i] = 0.19
        elif b == "dirt_path" or b == "farmland": low[i] = 0.94
    lowa = np.zeros(len(bn)); [lowa.__setitem__(k, v) for k, v in low.items()]
    seethru = np.array([("glass" in b or b.endswith("_leaves") or b in AIR or b in ("water", "ice")) for b in bn]) | plant | (lowa > 0)
    opaque = ~seethru[ids] & ~air
    turn, lf, rf = VIEWS[view]
    # The screen: each block's corner at (sx, sy).
    pts = [turn(x, z) for x, z in ((0, 0), (X - 1, 0), (0, Z - 1), (X - 1, Z - 1))]
    minx = min(a - b for a, b in pts) - 1; maxx = max(a - b for a, b in pts) + 1
    miny = min(a + b for a, b in pts) - 1; maxy = max(a + b for a, b in pts) + 1
    B = px
    Wd = int((maxx - minx + 2) * B)
    Ht = int((maxy - miny + 2) * B / 2 + (Y + 2) * B)
    canvas = Image.new("RGBA", (Wd, Ht), (0, 0, 0, 0))
    tex_of = {}

    def tex(i):
        if i not in tex_of:
            b = bn[i]
            used, fc = M.textures(b)
            tex_of[i] = fc if fc.get("side") else {"top": "block/" + b, "side": "block/" + b, "bottom": "block/" + b}
        return tex_of[i]
    order = []
    nz = np.nonzero(~air)
    for y, z, x in zip(*nz):
        tx, tz = turn(int(x), int(z))
        order.append((tx + tz, int(y), tx, int(x), int(y), int(z)))
    order.sort()
    shade = {"top": 1.0, "left": 0.82, "right": 0.66}
    for _, _, _, x, y, z in order:
        i = int(ids[y, z, x])
        b = bn[i]
        wx, wy, wz = x + x0, y + y0, z + z0
        tx, tz = turn(x, z)
        sx = (tx - tz - minx) * B
        sy = (tx + tz - miny) * B / 2 + (Y - y) * B
        h = lowa[i] or 1.0
        def nb(dx, dy, dz):
            a, bb, c = x + dx, y + dy, z + dz
            if not (0 <= a < X and 0 <= bb < Y and 0 <= c < Z): return False
            return bool(opaque[bb, c, a])
        if water[i]:
            if y + 1 < Y and bn[ids[y + 1, z, x]] in ("water",): continue
            im = P.get("block/water_still")
            f = iso.face(im, "top", 1.0)
            canvas.alpha_composite(Image.blend(Image.new("RGBA", f.size, (0, 0, 0, 0)), f, 0.85), (int(sx), int(sy + (1 - 0.9) * B)))
            continue
        if plant[i]:
            t = tex(i)
            im = P.get(t["side"])
            spr = im.resize((B, B), Image.NEAREST)
            canvas.alpha_composite(spr, (int(sx + B / 2), int(sy - B / 2 + B)))
            continue
        look, roof = L.at(b, wx, wy, wz)
        if look and look[0] == "hide": continue
        t = tex(i)
        top_h = (1 - h) * B
        if not nb(0, 1, 0) or h < 1:
            im = face_pic(P, t["top"], "top", look, roof, wx, wy, wz)
            if HILITE and not LAST["ours"] and not any(k in b for k in NATURAL) and not LOCAL(L, b, wx, wy, wz): im = red(im); HIT[(b, t["top"])].append((wx, wy, wz))
            if not NODRAW:
                f = iso.face(im, "top", shade["top"])
                canvas.alpha_composite(f, (int(sx), int(sy + top_h)))
        for kind, wf in (("left", lf), ("right", rf)):
            dx, dz = STEP[wf]
            if nb(dx, 0, dz) and h >= 1: continue
            im = face_pic(P, t["side"], wf, look, roof, wx, wy, wz)
            if HILITE and not LAST["ours"] and not any(k in b for k in NATURAL) and not LOCAL(L, b, wx, wy, wz): im = red(im); HIT[(b, t["side"])].append((wx, wy, wz))
            if not NODRAW:
                f = iso.face(im, kind, shade[kind], h)
                ox = sx if kind == "left" else sx + B
                canvas.alpha_composite(f, (int(ox), int(sy + B / 2 + top_h)))
    canvas = canvas.crop(canvas.getbbox() or (0, 0, Wd, Ht))
    bg = Image.new("RGBA", canvas.size, (168, 216, 244, 255))
    bg.alpha_composite(canvas)
    bg.convert("RGB").save(out)
    if MISSING: print("pictures not found:", dict(MISSING.most_common(12)))
    if HILITE:
        for (b, ref), l in sorted(HIT.items(), key=lambda kv: -len(kv[1])):
            print("vanilla face: %-32s %-34s %5d e.g. %s" % (b, ref, len(l), l[len(l) // 2]))
    return out


if __name__ == "__main__":
    a = sys.argv
    render(a[1], a[2], int(a[3]), int(a[4]), int(a[5]), int(a[6]), a[7], *(a[8:9] or ["se"]), *[int(v) for v in a[9:12]])
