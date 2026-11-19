"""Renders the mod's baked Town Map (assets/retromon/townmap) to a PNG, as TownMap.paint draws it in game.

python3 tools/townmap_render.py OUT.png [scale]   (scale: blocks per pixel, default 1; the 'view' box of index.json)
"""
import gzip, json, struct, sys, os
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/retromon/townmap")
R = 512
WATER, FOLIAGE, GROUND, PATH, SAND, BUILT, NOTHING = range(7)


def kind(name):
    n = name[10:] if name.startswith("minecraft:") else name
    n = n.split("[")[0]
    if n == "air": return NOTHING
    if n in ("water", "bubble_column") or n.startswith("kelp") or n.endswith("seagrass"): return WATER
    if "leaves" in n or n in ("vine", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem", "azalea", "flowering_azalea", "moss_carpet"): return FOLIAGE
    if n in ("dirt_path", "gravel", "coarse_dirt", "rooted_dirt", "packed_mud", "mud"): return PATH
    if n in ("sand", "sandstone", "smooth_sandstone", "suspicious_sand", "red_sand"): return SAND
    if n.startswith("grass_block") or n in ("dirt", "podzol", "mycelium", "moss_block", "clay", "snow", "snow_block", "farmland", "lily_pad", "poppy", "dandelion", "bush"): return GROUND
    return BUILT


def read(rx, rz, pal):
    with gzip.open(os.path.join(ROOT, f"r.{rx}.{rz}.bin"), "rb") as f: b = f.read()
    o = 4 + 12
    n = struct.unpack_from(">h", b, o)[0]; o += 2
    names = []
    for _ in range(n):
        L = struct.unpack_from(">H", b, o)[0]; o += 2
        names.append(b[o:o + L].decode("utf-8", "replace")); o += L
    ids = np.frombuffer(b, ">i2", R * R, o).astype(np.int32).clip(0, n - 1); o += R * R * 2
    ys = np.frombuffer(b, ">i2", R * R, o).astype(np.int32); o += R * R * 2
    depth = np.frombuffer(b, np.uint8, R * R, o).astype(np.int32)
    kinds = np.array([kind(s) for s in names], np.int32)
    def col(s):
        c = pal.get(s) or pal.get(s.split("[")[0])
        return int(c, 16) if c else 0x8C8C8C
    cols = np.array([col(s) for s in names], np.int64)
    return kinds[ids].reshape(R, R), cols[ids].reshape(R, R), ys.reshape(R, R), depth.reshape(R, R)


def render(box=None, scale=1):
    """The map of box (x0, z0, x1, z1 in blocks; index.json's view if None), as a PIL image, scale blocks to a pixel."""
    idx = json.load(open(os.path.join(ROOT, "index.json"))); pal = json.load(open(os.path.join(ROOT, "palette.json")))
    x0, z0, x1, z1 = box or idx["view"]
    rxs = range(x0 // R, x1 // R + 1); rzs = range(z0 // R, z1 // R + 1)
    gx0, gz0 = rxs[0] * R, rzs[0] * R
    GW, GH = len(rxs) * R, len(rzs) * R
    K = np.full((GH, GW), NOTHING, np.int32); C = np.zeros((GH, GW), np.int64); Y = np.full((GH, GW), -64, np.int32); Dp = np.zeros((GH, GW), np.int32)
    have = set(idx["regions"])
    for rx in rxs:
        for rz in rzs:
            if f"{rx}.{rz}" not in have: continue
            k, c, y, d = read(rx, rz, pal)
            sx, sz = (rx - rxs[0]) * R, (rz - rzs[0]) * R
            K[sz:sz + R, sx:sx + R] = k; C[sz:sz + R, sx:sx + R] = c; Y[sz:sz + R, sx:sx + R] = y; Dp[sz:sz + R, sx:sx + R] = d
    cr = ((C >> 16) & 255).astype(np.float32); cg = ((C >> 8) & 255).astype(np.float32); cb = (C & 255).astype(np.float32)
    lum = (cr * 0.3 + cg * 0.59 + cb * 0.11) / 255
    o = np.zeros((GH, GW, 3), np.float32)
    def put(mask, r, g, b): o[mask, 0] = r[mask] if hasattr(r, "shape") else r; o[mask, 1] = g[mask] if hasattr(g, "shape") else g; o[mask, 2] = b[mask] if hasattr(b, "shape") else b
    f = np.clip(1 + 0.21 * (lum / 0.42 - 1), 0.8, 1.15); put(K == GROUND, 128 * f, 196 * f, 96 * f)
    f = np.clip(0.9 + 0.3 * (lum - 0.45), 0.85, 1.05); put(K == PATH, 226 * f, 205 * f, 150 * f)
    put(K == SAND, 240, 226, 172)
    f = np.clip(0.85 + 0.5 * (lum - 0.3), 0.75, 1.1) * 0.6 + 0.4; put(K == FOLIAGE, 62 * f, 140 * f, 74 * f)
    m = (cr + cg + cb) / 3
    put(K == BUILT, np.clip((m + (cr - m) * 1.25) * 1.08 + 12, 0, 255), np.clip((m + (cg - m) * 1.25) * 1.08 + 12, 0, 255), np.clip((m + (cb - m) * 1.25) * 1.08 + 12, 0, 255))
    t = np.clip((Dp - 1) / 6, 0, 1); put(K == WATER, 130 + (52 - 130) * t, 214 + (140 - 214) * t, 232 + (206 - 232) * t)
    def sh(A, dx, dz, fill):
        B = np.full_like(A, fill)
        ys = slice(max(0, dz), GH + min(0, dz)); yd = slice(max(0, -dz), GH + min(0, -dz))
        xs = slice(max(0, dx), GW + min(0, dx)); xd = slice(max(0, -dx), GW + min(0, -dx))
        B[ys, xs] = A[yd, xd]; return B   # B[z, x] = A[z - dz, x - dx]
    land = (K != WATER) & (K != NOTHING)
    hNW = sh(Y, 1, 1, -64)
    shade = np.clip(1 + 0.07 * (Y - hNW), 0.7, 1.25); o[land] *= shade[land][:, None]
    shadow = np.zeros((GH, GW), bool)
    for s in range(1, 5): shadow |= sh(Y, s, s, -64) > Y + s * 1.1
    o[shadow & land] *= 0.74; o[shadow & ~land] *= 0.85
    Kn = [sh(K, 1, 0, NOTHING), sh(K, -1, 0, NOTHING), sh(K, 0, 1, NOTHING), sh(K, 0, -1, NOTHING)]
    landN = np.zeros((GH, GW), bool)
    for kk in Kn: landN |= (kk != WATER) & (kk != NOTHING)
    shore = (K == WATER) & landN
    o[shore] = o[shore] * 0.35 + np.array([235, 250, 248], np.float32) * 0.65
    Yn = [sh(Y, 1, 0, -64), sh(Y, -1, 0, -64), sh(Y, 0, 1, -64), sh(Y, 0, -1, -64)]
    edge = np.zeros((GH, GW), bool)
    for yy in Yn: edge |= (Y - yy) >= 2
    o[(K == BUILT) & edge] *= 0.7
    o[K == NOTHING] = [0x34, 0x8C, 0xCE]
    img = np.clip(o, 0, 255).astype(np.uint8)
    img = img[z0 - gz0:z1 - gz0, x0 - gx0:x1 - gx0]
    im = Image.fromarray(img, "RGB")
    if scale > 1: im = im.resize((im.width // scale, im.height // scale), Image.BOX)
    return im


if __name__ == "__main__":
    out = sys.argv[1]; scale = int(sys.argv[2]) if len(sys.argv) > 2 else 1
    im = render(None, scale); im.save(out, optimize=True)
    print(out, im.size, "blocks per pixel", scale)
