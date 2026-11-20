"""The world map on the walls inside (the lab's board, the Center's map, Daisy's table): John's Kanto from above, painted
as the Town Map paints it (TownMap.paint: grass, paths, sand, trees, buildings, shallow-to-deep sea, cliffs in earth
browns, a shadow to the south-east), then shrunk to a picture for a wall. Reads the mod's own Town Map data
(assets/retromon/townmap), so it's the same world the Town Map shows.

    python3 tools/worldmap.py [out.png] [width]
"""
import gzip
import json
import os
import struct
import sys

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
TM = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "townmap")
R = 512
WATER, FOLIAGE, GROUND, PATH, SAND, BUILT, NOTHING = range(7)
SEA = (0x34, 0x8C, 0xCE)


def kind(name):
    n = name[10:] if name.startswith("minecraft:") else name
    n = n.split("[")[0]
    if n == "air": return NOTHING
    if n in ("water", "bubble_column") or n.startswith("kelp") or n.endswith("seagrass"): return WATER
    if "leaves" in n or n in ("vine", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem", "azalea", "flowering_azalea", "moss_carpet"): return FOLIAGE
    if n in ("dirt_path", "gravel", "coarse_dirt", "rooted_dirt", "packed_mud", "mud"): return PATH
    if n in ("sand", "sandstone", "smooth_sandstone", "suspicious_sand", "red_sand"): return SAND
    if n.startswith("grass_block") or n in ("dirt", "podzol", "mycelium", "moss_block", "clay", "snow", "snow_block", "farmland", "lily_pad", "poppy", "dandelion", "bush"):
        return GROUND
    return BUILT


def read(rx, rz, palette):
    raw = gzip.open(os.path.join(TM, f"r.{rx}.{rz}.bin"), "rb").read()
    o = 16
    n = struct.unpack_from(">h", raw, o)[0]; o += 2
    names = []
    for _ in range(n):
        ln = struct.unpack_from(">H", raw, o)[0]; o += 2
        names.append(raw[o:o + ln].decode("utf-8", "replace")); o += ln
    ids = np.frombuffer(raw, ">i2", R * R, o).astype(np.int32).clip(0, n - 1); o += R * R * 2
    ys = np.frombuffer(raw, ">i2", R * R, o).astype(np.int32); o += R * R * 2
    depth = np.frombuffer(raw, np.uint8, R * R, o).astype(np.int32)
    kinds = np.array([kind(s) for s in names], np.int32)
    def col(s):
        c = palette.get(s) or palette.get(s.split("[")[0]) or "8c8c8c"
        return [int(c[i:i + 2], 16) for i in (0, 2, 4)]
    cols = np.array([col(s) for s in names], np.float32)
    return kinds[ids].reshape(R, R), cols[ids].reshape(R, R, 3), ys.reshape(R, R), depth.reshape(R, R)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "interior", "worldmap.png")
    width = int(sys.argv[2]) if len(sys.argv) > 2 else 320
    idx = json.load(open(os.path.join(TM, "index.json")))
    x0, z0, x1, z1 = idx["view"]
    palette = json.load(open(os.path.join(TM, "palette.json")))
    W, H = x1 - x0, z1 - z0
    K = np.full((H, W), NOTHING, np.int32); C = np.zeros((H, W, 3), np.float32); Y = np.full((H, W), -64, np.int32); D = np.zeros((H, W), np.int32)
    for key in idx["regions"]:
        rx, rz = (int(v) for v in key.split("."))
        bx, bz = rx * R, rz * R
        ax0, az0, ax1, az1 = max(bx, x0), max(bz, z0), min(bx + R, x1), min(bz + R, z1)
        if ax0 >= ax1 or az0 >= az1: continue
        k, c, y, d = read(rx, rz, palette)
        K[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = k[az0 - bz:az1 - bz, ax0 - bx:ax1 - bx]
        C[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = c[az0 - bz:az1 - bz, ax0 - bx:ax1 - bx]
        Y[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = y[az0 - bz:az1 - bz, ax0 - bx:ax1 - bx]
        D[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = d[az0 - bz:az1 - bz, ax0 - bx:ax1 - bx]
    # The island north of Cerulean's cape stays off, as on the Town Map.
    gx, gz = np.meshgrid(np.arange(x0, x1), np.arange(z0, z1))
    K[(gx >= -675) & (gx <= -515) & (gz >= 35) & (gz <= 119)] = NOTHING
    lum = (C[..., 0] * 0.3 + C[..., 1] * 0.59 + C[..., 2] * 0.11) / 255
    out_c = np.zeros((H, W, 3), np.float32)
    out_c[:] = SEA
    def put(mask, rgb): out_c[mask] = rgb[mask] if rgb.ndim == 3 else rgb
    f = np.clip(1 + 0.21 * (lum / 0.42 - 1), 0.8, 1.15)[..., None]
    put(K == GROUND, np.array([128, 196, 96], np.float32) * f)
    f = np.clip(0.9 + 0.3 * (lum - 0.45), 0.85, 1.05)[..., None]
    put(K == PATH, np.array([226, 205, 150], np.float32) * f)
    put(K == SAND, np.broadcast_to(np.array([240, 226, 172], np.float32), (H, W, 3)).copy())
    f = (np.clip(0.85 + 0.5 * (lum - 0.3), 0.75, 1.1) * 0.6 + 0.4)[..., None]
    put(K == FOLIAGE, np.array([62, 140, 74], np.float32) * f)
    m = C.mean(axis=2, keepdims=True)
    put(K == BUILT, np.clip((m + (C - m) * 1.25) * 1.08 + 12, 0, 255))
    t = np.clip((D - 1) / 6, 0, 1)[..., None]
    put(K == WATER, np.array([130, 214, 232], np.float32) + (np.array([52, 140, 206], np.float32) - [130, 214, 232]) * t)
    land = (K != WATER) & (K != NOTHING)
    nw = np.roll(np.roll(Y, 1, 0), 1, 1)
    shade = np.clip(1 + 0.07 * (Y - nw), 0.7, 1.25)[..., None]
    out_c = np.where(land[..., None], out_c * shade, out_c)
    # Coasts: light where water meets land.
    nb = np.zeros_like(land)
    for dz, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)): nb |= np.roll(np.roll(land, dz, 0), dx, 1)
    coast = (K == WATER) & nb
    out_c[coast] = out_c[coast] * 0.35 + np.array([235, 250, 248]) * 0.65
    img = Image.fromarray(np.clip(out_c, 0, 255).astype(np.uint8), "RGB")
    h = round(width * H / W)
    small = img.resize((width, h), Image.BOX).filter(ImageFilter.UnsharpMask(1.2, 60, 2))
    small.save(out)
    print("wrote", out, small.size)


if __name__ == "__main__":
    main()
