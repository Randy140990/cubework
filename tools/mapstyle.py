#!/usr/bin/env python3
"""
A preview of the Town Map's look (the same steps as client/TownMap.java's painter), drawn from mapbake.py's
columns: python3 tools/mapstyle.py <raw folder> <out.png> x0 z0 x1 z1 [blocks per pixel]
"""
import json, os, sys

import numpy as np
from PIL import Image

PALETTE = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon", "townmap",
                                      "palette.json")))

WATER, FOLIAGE, GROUND, PATH, SAND, BUILT, NOTHING = 0, 1, 2, 3, 4, 5, 6


def kind(name):
    n = name.replace("minecraft:", "")
    if n in ("air",):
        return NOTHING
    if n in ("water", "bubble_column", "kelp", "kelp_plant", "seagrass", "tall_seagrass"):
        return WATER
    if "leaves" in n or n in ("vine", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem", "azalea", "flowering_azalea", "moss_carpet"):
        return FOLIAGE
    if n in ("dirt_path", "gravel", "coarse_dirt", "rooted_dirt", "packed_mud", "mud"):
        return PATH
    if n in ("sand", "sandstone", "smooth_sandstone", "suspicious_sand", "red_sand"):
        return SAND
    if n.startswith("grass_block") or n in ("dirt", "podzol", "mycelium", "moss_block", "clay", "snow", "snow_block", "farmland", "lily_pad",
                                             "short_grass", "tall_grass", "fern", "large_fern", "poppy", "dandelion", "bush"):
        return GROUND
    return BUILT


def rgb(hexs):
    return np.array([int(hexs[0:2], 16), int(hexs[2:4], 16), int(hexs[4:6], 16)], np.float64)


def load_area(raw, x0, z0, x1, z1):
    W, H = x1 - x0, z1 - z0
    col = np.zeros((H, W, 3))
    knd = np.full((H, W), NOTHING, np.int8)
    hy = np.full((H, W), -64.0)
    dep = np.zeros((H, W))
    for rz in range(z0 // 512, (z1 - 1) // 512 + 1):
        for rx in range(x0 // 512, (x1 - 1) // 512 + 1):
            p = os.path.join(raw, "%d.%d.npz" % (rx, rz))
            if not os.path.exists(p):
                continue
            z = np.load(p)
            names = [str(n) for n in z["names"]]
            pal = np.array([rgb(PALETTE.get(n, PALETTE.get(n.split("[")[0], "808080"))) for n in names])
            kd = np.array([kind(n) for n in names], np.int8)
            ax0, az0 = max(x0, rx * 512), max(z0, rz * 512)
            ax1, az1 = min(x1, rx * 512 + 512), min(z1, rz * 512 + 512)
            if ax0 >= ax1 or az0 >= az1:
                continue
            ids = z["id"][az0 - rz * 512:az1 - rz * 512, ax0 - rx * 512:ax1 - rx * 512]
            col[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = pal[ids]
            knd[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = kd[ids]
            hy[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = z["y"][az0 - rz * 512:az1 - rz * 512, ax0 - rx * 512:ax1 - rx * 512]
            dep[az0 - z0:az1 - z0, ax0 - x0:ax1 - x0] = z["d"][az0 - rz * 512:az1 - rz * 512, ax0 - rx * 512:ax1 - rx * 512]
    return col, knd, hy, dep


def lum(c):
    return (c[..., 0] * 0.3 + c[..., 1] * 0.59 + c[..., 2] * 0.11) / 255.0


def paint(col, knd, hy, dep):
    out = col.copy()
    L = lum(col)
    # Ground: one soft grass green, a little lighter or darker as the block was.
    g = knd == GROUND
    grass = np.array([128, 196, 96.0])
    out[g] = grass * np.clip(0.75 + 0.6 * (L[g] / 0.42 - 1) * 0.35 + 0.25, 0.8, 1.15)[:, None]
    # Paths and sand: warm creams.
    p = knd == PATH
    out[p] = np.array([226, 205, 150.0]) * np.clip(0.9 + 0.3 * (L[p] - 0.45), 0.85, 1.05)[:, None]
    sd = knd == SAND
    out[sd] = np.array([240, 226, 172.0])
    # Trees: deep green canopies.
    f = knd == FOLIAGE
    leaf = np.array([62, 140, 74.0])
    out[f] = leaf * np.clip(0.85 + 0.5 * (L[f] - 0.3), 0.75, 1.1)[:, None] * 0.6 + col[f] * 0.0 + leaf * 0.4
    # Buildings: their own colour, a touch brighter and cleaner.
    b = knd == BUILT
    c = col[b]
    m = c.mean(axis=1, keepdims=True)
    c = m + (c - m) * 1.25
    out[b] = np.clip(c * 1.08 + 12, 0, 255)
    # Water: lighter where it's shallow.
    w = knd == WATER
    t = np.clip((dep[w] - 1) / 6.0, 0, 1)[:, None]
    out[w] = np.array([130, 214, 232.0]) * (1 - t) + np.array([52, 140, 206.0]) * t
    # Light from the north-west: slopes facing it lighter, the others darker.
    H = hy.copy()
    H[w] = H[w] - dep[w]  # (water's surface is flat; this only matters for land)
    hy2 = np.where(knd == WATER, hy, hy)
    up = np.roll(np.roll(hy2, 1, 0), 1, 1)
    d = hy2 - up
    land = (knd != WATER) & (knd != NOTHING)
    shade = np.clip(1 + 0.07 * d, 0.7, 1.25)
    out[land] *= shade[land][:, None]
    # Shadows cast to the south-east by anything taller.
    sh = np.zeros(hy.shape, bool)
    for k in range(1, 5):
        s = np.roll(np.roll(hy2, k, 0), k, 1)
        sh |= s > hy2 + k * 1.1
    out[sh & land] *= 0.74
    out[sh & w] *= 0.85
    # Shores: a pale line where water meets land.
    nb = np.zeros(hy.shape, bool)
    for dz, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb |= np.roll(np.roll(land, dz, 0), dx, 1)
    shore = w & nb
    out[shore] = out[shore] * 0.35 + np.array([235, 250, 248.0]) * 0.65
    # Building edges: a dark line where it drops by two or more.
    edge = np.zeros(hy.shape, bool)
    for dz, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nh = np.roll(np.roll(hy2, dz, 0), dx, 1)
        edge |= (hy2 - nh >= 2) & (knd == BUILT)
    out[edge] *= 0.7
    out[knd == NOTHING] = np.array([40, 120, 190.0])
    return np.clip(out, 0, 255).astype(np.uint8)


def main():
    raw, outp = sys.argv[1], sys.argv[2]
    x0, z0, x1, z1 = map(int, sys.argv[3:7])
    step = int(sys.argv[7]) if len(sys.argv) > 7 else 1
    col, knd, hy, dep = load_area(raw, x0, z0, x1, z1)
    img = paint(col, knd, hy, dep)
    if step > 1:
        H, W = img.shape[:2]
        img = img[:H - H % step, :W - W % step].reshape(H // step, step, W // step, step, 3).mean(axis=(1, 3)).astype(np.uint8)
    Image.fromarray(img).save(outp)
    print(img.shape)


if __name__ == "__main__":
    main()
