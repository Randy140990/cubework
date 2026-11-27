#!/usr/bin/env python3
"""
The Town Map's data for the game, from mapbake.py's columns:

    python3 tools/mapexport.py <raw folder>

Writes src/main/resources/assets/retromon/townmap/r.<x>.<z>.bin for every region with land in it (gzip; big-endian:
'RMAP', version 1, region x and z, the block names, then 512x512 block indices, heights and water depths, row by
row from the north-west), and index.json: those regions, and the part of the world the map opens on (the land,
leaving out the picture built off the west coast).
"""
import gzip, json, os, struct, sys

import numpy as np

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon", "townmap")
# West of here was the HeartGold & SoulSilver picture made of blocks: drawn on the map, but not where it opens.
# ((0.70) John's 27 Nov save has no picture: Johto's built out to his Route 48 and cliffs at x -2760, all of it in view.)
VIEW_WEST = -2800


def main():
    raw = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):
        if f.startswith("r.") and f.endswith(".bin"):
            os.remove(os.path.join(OUT, f))
    regions = []
    bx0 = bz0 = 10 ** 9
    bx1 = bz1 = -10 ** 9
    ax0 = az0 = 10 ** 9
    ax1 = az1 = -10 ** 9
    total = 0
    for fn in sorted(os.listdir(raw)):
        if not fn.endswith(".npz"):
            continue
        rx, rz = map(int, fn[:-4].split("."))
        z = np.load(os.path.join(raw, fn))
        names = [str(n) for n in z["names"]]
        ids = z["id"]
        water = names.index("minecraft:water") if "minecraft:water" in names else -1
        land = (ids != 0) & (ids != water)
        if not land.any():
            continue
        zz, xx = np.nonzero(land)
        wx, wz = xx + rx * 512, zz + rz * 512
        keep = wx >= VIEW_WEST
        # (A stray block or two out at sea doesn't count.)
        if land.sum() > 1000:
            ax0, ax1 = min(ax0, int(wx.min())), max(ax1, int(wx.max()))
            az0, az1 = min(az0, int(wz.min())), max(az1, int(wz.max()))
        if keep.sum() > 1000:
            bx0, bx1 = min(bx0, int(wx[keep].min())), max(bx1, int(wx[keep].max()))
            bz0, bz1 = min(bz0, int(wz[keep].min())), max(bz1, int(wz[keep].max()))
        b = bytearray()
        b += b"RMAP" + struct.pack(">iii", 1, rx, rz)
        b += struct.pack(">h", len(names))
        for n in names:
            e = n.encode("utf-8")
            b += struct.pack(">H", len(e)) + e
        b += ids.astype(">i2").tobytes()
        b += np.clip(z["y"], -2048, 2047).astype(">i2").tobytes()
        b += np.clip(z["d"], 0, 127).astype(np.int8).tobytes()
        path = os.path.join(OUT, "r.%d.%d.bin" % (rx, rz))
        with gzip.open(path, "wb", compresslevel=9) as f:
            f.write(bytes(b))
        total += os.path.getsize(path)
        regions.append("%d.%d" % (rx, rz))
    pad = 48
    index = {"regions": regions, "view": [bx0 - pad, bz0 - pad, bx1 + pad, bz1 + pad], "all": [ax0 - pad, az0 - pad, ax1 + pad, az1 + pad]}
    with open(os.path.join(OUT, "index.json"), "w") as f:
        json.dump(index, f)
    print(len(regions), "regions,", total // 1024, "KB; view", index["view"])


if __name__ == "__main__":
    main()
