"""
The Retromon Look pack's ground, as Let's Go's (its Victory Road shot, 130520): grass in greens with lighter and
darker patches and a leaf here and there, dirt paths a warm brown gradient, moss as bushy leaves in darker greens.
Where they meet, the edges are rounded with connected textures (CTM, read by Continuity): the moss's leaves spill
onto the paths in rounded bushes, the grass creeps onto the moss and the paths in soft curves, so no block's square
edge shows. The same method as Shards of Arcadia's (OptiFine's overlay method, its 17 tiles; PixelSculpt's CTM pack
shows the method), with our own pictures.

Grass is drawn in greys, as Minecraft's is, so the biome's green (brightened by Retromon) colours it and its
fringes (tinted the same way). Large-scale variation comes from 64 x 64 pictures cut into 16 tiles (CTM's repeat
method), so the patches don't repeat every block and there are no lines between blocks.
"""
import math
import os
import random

from PIL import Image

PACK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "resourcepacks", "retromon_look")
TEX = os.path.join(PACK, "assets", "minecraft", "textures", "block")
CTM = os.path.join(PACK, "assets", "minecraft", "optifine", "ctm", "retromon")


# ---------- noise that wraps (so pictures tile with no seams) ----------
class Noise:
    """Smooth value noise over a square of side `period`, wrapping at its edges, as octaves of `cells` cells."""

    def __init__(self, period, cells, seed):
        rnd = random.Random(seed)
        self.p = period
        self.layers = [(c, [[rnd.random() for _ in range(c)] for _ in range(c)]) for c in cells]

    def __call__(self, x, y):
        tot = w = 0.0
        amp = 1.0
        for c, g in self.layers:
            u, v = x / self.p * c, y / self.p * c
            i, j = int(math.floor(u)), int(math.floor(v))
            fu, fv = u - i, v - j
            fu, fv = fu * fu * (3 - 2 * fu), fv * fv * (3 - 2 * fv)
            a = g[j % c][i % c]; b = g[j % c][(i + 1) % c]; cc = g[(j + 1) % c][i % c]; d = g[(j + 1) % c][(i + 1) % c]
            tot += amp * (a + (b - a) * fu + (cc - a) * fv + (a - b - cc + d) * fu * fv)
            w += amp
            amp *= 0.5
        return tot / w


def clamp(v):
    return max(0, min(255, int(round(v))))


def rgb(*c):
    return tuple(clamp(v) for v in c) + (255,)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# ---------- the ground's own pictures ----------
def grass(size, seed):
    """Greys for the biome's green: a soft base, lighter (warmer) and darker (cooler) patches, small blades, and a
    leaf here and there. size 16 (the plain texture) or 64 (cut into the 16 repeat tiles)."""
    rnd = random.Random(seed)
    big = Noise(size, [2, 4] if size >= 64 else [1, 2], seed)
    fine = Noise(size, [size // 2], seed + 1)
    im = Image.new("RGBA", (size, size))
    p = im.load()
    for y in range(size):
        for x in range(size):
            n = big(x, y) - 0.5
            f = fine(x, y) - 0.5
            v = 214 + n * (30 if size >= 64 else 18) + f * 8
            # Lighter patches a touch warmer (yellower once tinted), darker ones a touch cooler.
            p[x, y] = rgb(v + n * 10, v + n * 3, v - n * 14)
    # Small blades: a light tip over a darker root, scattered (not on a grid).
    for _ in range(size * size // 26):
        x, y = rnd.randrange(size), rnd.randrange(size)
        r, g, b, _a = p[x, y]
        p[x, y] = rgb(r + 22, g + 22, b + 16)
        p[x, (y + 1) % size] = rgb(r - 30, g - 26, b - 22)
    # A leaf here and there: a little rounded leaf, lighter and warm, with a dark underside.
    for _ in range(max(1, size * size // 1400)):
        cx, cy = rnd.randrange(size), rnd.randrange(size)
        for dx, dy, k in ((0, 0, 40), (1, 0, 44), (-1, 0, 30), (0, -1, 34), (1, -1, 26), (0, 1, -34), (1, 1, -40), (-1, 1, -24)):
            x, y = (cx + dx) % size, (cy + dy) % size
            r, g, b, _a = p[x, y]
            p[x, y] = rgb(r + k + (8 if k > 0 else 0), g + k, b + k - (18 if k > 0 else 0))
    return im


PATH_LIGHT, PATH_DARK = hexc("#DDB57C"), hexc("#C48D55")


def path(size, seed):
    """The dirt path: a warm brown, lighter and darker in soft gradients, a few tiny pebbles; never a line."""
    rnd = random.Random(seed)
    big = Noise(size, [2, 4] if size >= 64 else [1, 2], seed)
    fine = Noise(size, [size // 2], seed + 3)
    im = Image.new("RGBA", (size, size))
    p = im.load()
    for y in range(size):
        for x in range(size):
            t = max(0.0, min(1.0, (big(x, y) - 0.5) * 1.9 + 0.5))
            c = mix(PATH_LIGHT, PATH_DARK, t)
            f = (fine(x, y) - 0.5) * 10
            p[x, y] = rgb(c[0] + f, c[1] + f, c[2] + f * 0.7)
    for _ in range(size * size // 40):
        x, y = rnd.randrange(size), rnd.randrange(size)
        r, g, b, _a = p[x, y]
        if rnd.random() < 0.5:
            p[x, y] = rgb(r + 18, g + 16, b + 14)
        else:
            p[x, y] = rgb(r - 24, g - 22, b - 18)
            p[(x + 1) % size, y] = rgb(r + 10, g + 9, b + 8)
    return im


def dirt(seed=11):
    """Dirt (under the grass, and its sides): the path's warm brown, darker and earthier."""
    rnd = random.Random(seed)
    n = Noise(16, [2, 4], seed)
    im = Image.new("RGBA", (16, 16))
    p = im.load()
    a, b = hexc("#A8774A"), hexc("#8C5E37")
    for y in range(16):
        for x in range(16):
            c = mix(a, b, max(0.0, min(1.0, (n(x, y) - 0.5) * 2 + 0.5)))
            p[x, y] = rgb(*c)
    for _ in range(14):
        x, y = rnd.randrange(16), rnd.randrange(16)
        r, g, bb, _a = p[x, y]
        p[x, y] = rgb(r - 26, g - 22, bb - 18) if rnd.random() < 0.6 else rgb(r + 20, g + 16, bb + 12)
    return im


MOSS = [hexc(c) for c in ("#1E4721", "#2C6230", "#3B7C38", "#4E9640", "#69AE4E")]


def moss():
    """Bushy leaves in darker greens: round clumps overlapping like a hedge seen from above, each lit from the top
    left with a dark rim below, so the blocks read as one bush."""
    rnd = random.Random(21)
    im = Image.new("RGBA", (16, 16))
    p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = rgb(*MOSS[0])
    clumps = [(2, 2), (10, 1), (6, 6), (14, 6), (1, 10), (9, 10), (5, 14), (13, 14)]
    rnd.shuffle(clumps)
    # Drawn top to bottom so the lower clumps sit over the upper ones (a bush's look).
    for cx, cy in sorted(clumps, key=lambda c: c[1]):
        r = 3.4 + rnd.random() * 0.5
        for y in range(16):
            for x in range(16):
                dx = (x - cx + 8) % 16 - 8
                dy = (y - cy + 8) % 16 - 8
                d = math.hypot(dx + 0.2, dy + 0.2)
                if d < r:
                    s = (-(dx + dy) / (2 * r)) + 0.15 * (rnd.random() - 0.5)
                    k = 2 if s < -0.15 else 3 if s < 0.3 else 4
                    if d > r - 1.0 and dy > 0:
                        k = 1
                    p[x, y] = rgb(*MOSS[k])
                elif d < r + 0.9 and dy > 0:
                    p[x, y] = rgb(*MOSS[0])
    # Little leaf tips catching the light.
    for _ in range(7):
        x, y = rnd.randrange(16), rnd.randrange(16)
        if p[x, y][:3] in (MOSS[3], MOSS[4]):
            p[x, y] = rgb(*mix(MOSS[4], (220, 240, 160), 0.35))
    return im


# ---------- the rounded edges (CTM overlays, OptiFine's 17 tiles) ----------
# Which neighbours each tile is for: its edges (the block on that side matches) and corners (only the diagonal).
TILES = {
    1: ([], ["br"]), 2: (["bottom"], []), 3: ([], ["bl"]), 4: (["right", "bottom"], []), 5: (["bottom", "left"], []),
    6: (["left", "bottom", "right"], []), 7: (["bottom", "left", "top"], []), 8: (["right"], []), 9: (["top", "right", "bottom", "left"], []),
    10: (["left"], []), 11: (["top", "right"], []), 12: (["left", "top"], []), 13: (["top", "right", "bottom"], []),
    14: (["left", "top", "right"], []), 15: ([], ["tr"]), 16: (["top"], []), 17: ([], ["tl"]),
}


def smin(a, b, k):
    if k <= 0:
        return min(a, b)
    h = max(k - abs(a - b), 0.0) / k
    return min(a, b) - h * h * k / 4


def field(edges, corners, x, y, wave, amp):
    """How far a pixel (its centre) is from the next block's edge, less the edge's wave: under `depth` is covered."""
    ds = []
    for e in edges:
        t, d = {"top": (x, y), "bottom": (x, 16 - y), "left": (y, x), "right": (y, 16 - x)}[e]
        ds.append(d - amp * wave(t))
    for c in corners:
        cx, cy = {"tl": (0, 0), "tr": (16, 0), "bl": (0, 16), "br": (16, 16)}[c]
        ds.append(math.hypot(x - cx, y - cy))
    return ds


def fringe(tile, depth, amp, wave, fill, rim, shadow=None, round_k=9.0):
    """One overlay tile: covered pixels from `fill(x, y)`, a 1 px rim of `rim` at the edge (rounded where two edges
    meet: their distances are blended), and optionally a 1 px `shadow` just past it."""
    edges, corners = TILES[tile]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    for y in range(16):
        for x in range(16):
            ds = field(edges, corners, x + 0.5, y + 0.5, wave, amp)
            f = ds[0]
            for d in ds[1:]:
                f = smin(f, d, round_k)
            if f < depth - 1:
                p[x, y] = fill(x, y)
            elif f < depth:
                p[x, y] = rim(x, y)
            elif shadow and f < depth + 1:
                p[x, y] = shadow(x, y)
    return im


def scallop(t):
    """Two bushes along each block edge (none where blocks meet, so neighbours line up)."""
    return math.sin(math.pi * t / 8) ** 2


def soft(t):
    return math.sin(math.pi * t / 16) ** 2


def tufty(t):
    return 0.6 * math.sin(math.pi * t / 16) ** 2 + 0.4 * math.sin(math.pi * t / 4) ** 2


def write_overlay(name, match, connect, make, tint=False):
    d = os.path.join(CTM, name)
    os.makedirs(d, exist_ok=True)
    for t in range(1, 18):
        make(t).save(os.path.join(d, "%d.png" % t))
    props = ["method=overlay", "tiles=1-17", "matchBlocks=" + match, "connectBlocks=" + connect, "faces=top", "layer=cutout_mipped"]
    if tint:
        props += ["tintIndex=0", "tintBlock=minecraft:grass_block"]
    open(os.path.join(d, name + ".properties"), "w").write("\n".join(props) + "\n")


def write_repeat(name, match, big, tile0=None):
    d = os.path.join(CTM, name)
    os.makedirs(d, exist_ok=True)
    for j in range(4):
        for i in range(4):
            big.crop((i * 16, j * 16, i * 16 + 16, j * 16 + 16)).save(os.path.join(d, "%d.png" % (j * 4 + i)))
    props = ["method=repeat", "matchBlocks=" + match, "faces=top", "width=4", "height=4", "tiles=0-15"]
    open(os.path.join(d, name + ".properties"), "w").write("\n".join(props) + "\n")


def build(save):
    """Writes the ground's textures (through `save(name, image)`) and its CTM, and returns the pictures for previews."""
    g16, g64 = grass(16, 31), grass(64, 32)
    p16, p64 = path(16, 41), path(64, 42)
    m, dt = moss(), dirt()
    save("grass_block_top", g16)
    save("dirt_path_top", p16)
    save("moss_block", m)
    save("dirt", dt)
    # The grass's side: dirt (the green lip is its overlay, tinted).
    save("grass_block_side", dt)
    # The path's side: its lip in the path's brown, dirt below.
    side = dt.copy()
    sp = side.load()
    pp = p16.load()
    for x in range(16):
        for y in range(3):
            sp[x, y] = pp[x, (y + 5) % 16]
    save("dirt_path_side", side)
    write_repeat("grass_repeat", "grass_block", g64)
    write_repeat("path_repeat", "dirt_path", p64)
    # The grass fringes take the plain grass's detail at the large picture's average tone, so where a fringe meets
    # the next block's grass there's no step in brightness.
    def mean(im):
        px = im.load(); n = im.size[0] * im.size[1]
        return [sum(px[x, y][i] for y in range(im.size[1]) for x in range(im.size[0])) / n for i in range(3)]
    off = [a - b for a, b in zip(mean(g64), mean(g16))]
    gf = g16.copy()
    gfp = gf.load()
    for y in range(16):
        for x in range(16):
            gfp[x, y] = rgb(*(gfp[x, y][i] + off[i] for i in range(3)))
    mp, gp = m.load(), gfp
    moss_fill = lambda x, y: mp[x, y]
    moss_rim = lambda x, y: rgb(*MOSS[0])
    path_shadow = lambda x, y: rgb(*mix(PATH_DARK, (90, 55, 30), 0.35))
    grass_fill = lambda x, y: gp[x, y]
    grass_rim = lambda x, y: rgb(*(v * 0.74 for v in gp[x, y][:3]))
    # The moss's leaves spill onto the paths in rounded bushes, a soft shadow past them.
    write_overlay("moss_on_path", "dirt_path coarse_dirt rooted_dirt", "moss_block",
                  lambda t: fringe(t, 3.0, 2.2, scallop, moss_fill, moss_rim, path_shadow, round_k=14.0))
    # The grass creeps onto the moss (rounding the moss's corners) and onto the paths, in soft curves.
    write_overlay("grass_on_moss", "moss_block", "grass_block",
                  lambda t: fringe(t, 2.6, 1.2, soft, grass_fill, grass_rim), tint=True)
    write_overlay("grass_on_path", "dirt_path coarse_dirt rooted_dirt", "grass_block",
                  lambda t: fringe(t, 3.2, 1.6, tufty, grass_fill, grass_rim), tint=True)
    return {"g64": g64, "p64": p64, "moss": m}


# ---------- an offline preview: a stretch of path with its moss border in the grass, composed as Continuity would ----------
TINT = (126, 200, 82)


def preview(out, scale=6):
    pics = {}

    def keep(name, im):
        pics[name] = im

    built = build(keep)
    lay = [
        "GGGGGGGGGGGGGGGGGGGG",
        "GGGGGGGGGGGGGGGGGGGG",
        "GGMMMMMMMMMMGGGGGGGG",
        "GGMPPPPPPPPMGGGGGGGG",
        "GGMPPPPPPPPMMMMMMGGG",
        "GGMPPMMMMPPPPPPPMGGG",
        "GGMPPMGGMPPPPPPPMGGG",
        "GGMPPMGGMMMMMPPPMGGG",
        "GGMPPMGGGGGGMPPPMGGG",
        "GGMPPMMMGGGGMPPPMGGG",
        "GGMPPPPMGGGGMPPPMGGG",
        "GGMMMPPMGGGGMMMPPPGG",
        "GGGGMPPMGGGGGGMPPPGG",
        "GGGGMMMMGGGGGGMMMMGG",
        "GGGGGGGGGGGGGGGGGGGG",
    ]
    H, W = len(lay), len(lay[0])
    img = Image.new("RGBA", (W * 16, H * 16))
    at = lambda x, z: lay[z][x] if 0 <= z < H and 0 <= x < W else "G"

    def tint(im):
        im = im.copy()
        px = im.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = px[x, y]
                if a:
                    px[x, y] = (r * TINT[0] // 255, g * TINT[1] // 255, b * TINT[2] // 255, a)
        return im

    def tiles_for(x, z, conn):
        c = lambda dx, dz: at(x + dx, z + dz) in conn
        e = {"top": c(0, -1), "bottom": c(0, 1), "left": c(-1, 0), "right": c(1, 0)}
        k = {"tl": c(-1, -1), "tr": c(1, -1), "bl": c(-1, 1), "br": c(1, 1)}
        E = frozenset(n for n, v in e.items() if v)
        out = []
        three = {frozenset(["left", "bottom", "right"]): 6, frozenset(["bottom", "left", "top"]): 7,
                 frozenset(["top", "right", "bottom"]): 13, frozenset(["left", "top", "right"]): 14}
        two = {frozenset(["right", "bottom"]): 4, frozenset(["bottom", "left"]): 5, frozenset(["top", "right"]): 11, frozenset(["left", "top"]): 12}
        one = {"top": 16, "bottom": 2, "left": 10, "right": 8}
        if len(E) == 4:
            out.append(9)
        elif E in three:
            out.append(three[E])
        elif E in two:
            out.append(two[E])
        else:
            out += [one[n] for n in E]
        near = {"tl": ("top", "left"), "tr": ("top", "right"), "bl": ("bottom", "left"), "br": ("bottom", "right")}
        cn = {"tl": 17, "tr": 15, "bl": 3, "br": 1}
        out += [cn[q] for q, v in k.items() if v and not (set(near[q]) & E)]
        return out

    ov = {}
    for name in ("moss_on_path", "grass_on_moss", "grass_on_path"):
        ov[name] = {t: Image.open(os.path.join(CTM, name, "%d.png" % t)).convert("RGBA") for t in range(1, 18)}
    for z in range(H):
        for x in range(W):
            b = at(x, z)
            if b == "G":
                base = tint(built["g64"].crop(((x % 4) * 16, (z % 4) * 16, (x % 4) * 16 + 16, (z % 4) * 16 + 16)))
            elif b == "P":
                base = built["p64"].crop(((x % 4) * 16, (z % 4) * 16, (x % 4) * 16 + 16, (z % 4) * 16 + 16))
            else:
                base = built["moss"]
            img.paste(base, (x * 16, z * 16))
            layers = []
            if b == "P":
                layers += [("moss_on_path", "M", False), ("grass_on_path", "G", True)]
            if b == "M":
                layers += [("grass_on_moss", "G", True)]
            for name, conn, tn in layers:
                for t in tiles_for(x, z, conn):
                    o = ov[name][t]
                    img.alpha_composite(tint(o) if tn else o, (x * 16, z * 16))
    img.resize((W * 16 * scale // 2, H * 16 * scale // 2), Image.NEAREST).save(out)


if __name__ == "__main__":
    import sys
    preview(sys.argv[1] if len(sys.argv) > 1 else "ground_preview.png")
