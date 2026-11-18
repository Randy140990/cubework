#!/usr/bin/env python3
"""
Reads a Minecraft world's region files (Anvil, 1.18+ chunk format) and writes what the Town Map is drawn from:
for every column, the top block (an index into a list of block names), its height, and how deep the water is.

    python3 tools/mapbake.py <world folder> <out folder> [--regions x0 x1 z0 z1]

Writes <out>/<rx>.<rz>.npz (top block index, height, water depth: 512x512 each) and <out>/blocks.json (the names,
in index order), plus <out>/counts.json (how often each block is on top, for choosing the map's colours).
Stdlib, numpy only: the NBT is read by hand.
"""
import json, os, struct, sys, zlib
from multiprocessing import Pool

import numpy as np

# ---------- NBT ----------


class R:
    __slots__ = ("b", "i")

    def __init__(self, b):
        self.b, self.i = b, 0

    def take(self, n):
        v = self.b[self.i:self.i + n]
        self.i += n
        return v


def payload(r, t):
    b = r.b
    if t == 1:
        r.i += 1
        return b[r.i - 1] - 256 if b[r.i - 1] > 127 else b[r.i - 1]
    if t == 2:
        r.i += 2
        return struct.unpack_from(">h", b, r.i - 2)[0]
    if t == 3:
        r.i += 4
        return struct.unpack_from(">i", b, r.i - 4)[0]
    if t == 4:
        r.i += 8
        return struct.unpack_from(">q", b, r.i - 8)[0]
    if t == 5:
        r.i += 4
        return struct.unpack_from(">f", b, r.i - 4)[0]
    if t == 6:
        r.i += 8
        return struct.unpack_from(">d", b, r.i - 8)[0]
    if t == 7:
        n = struct.unpack_from(">i", b, r.i)[0]
        r.i += 4 + n
        return None  # byte arrays aren't needed
    if t == 8:
        n = struct.unpack_from(">H", b, r.i)[0]
        r.i += 2 + n
        return b[r.i - n:r.i].decode("utf-8", "replace")
    if t == 9:
        et = b[r.i]
        n = struct.unpack_from(">i", b, r.i + 1)[0]
        r.i += 5
        return [payload(r, et) for _ in range(max(0, n))]
    if t == 10:
        out = {}
        while True:
            tt = b[r.i]
            r.i += 1
            if tt == 0:
                return out
            n = struct.unpack_from(">H", b, r.i)[0]
            name = b[r.i + 2:r.i + 2 + n].decode("utf-8", "replace")
            r.i += 2 + n
            out[name] = payload(r, tt)
    if t == 11:
        n = struct.unpack_from(">i", b, r.i)[0]
        r.i += 4
        a = np.frombuffer(b, ">i4", n, r.i)
        r.i += 4 * n
        return a
    if t == 12:
        n = struct.unpack_from(">i", b, r.i)[0]
        r.i += 4
        a = np.frombuffer(b, ">i8", n, r.i)
        r.i += 8 * n
        return a
    raise ValueError("tag %d" % t)


def nbt(b):
    r = R(b)
    t = b[0]
    n = struct.unpack_from(">H", b, 1)[0]
    r.i = 3 + n
    return payload(r, t)


# ---------- unpacking ----------


def unpack(longs, bits, count):
    """Values packed bits wide, not spanning longs (1.16+)."""
    per = 64 // bits
    a = np.asarray(longs).astype(np.uint64)
    shifts = (np.arange(per, dtype=np.uint64) * np.uint64(bits))
    vals = (a[:, None] >> shifts[None, :]) & np.uint64((1 << bits) - 1)
    return vals.reshape(-1)[:count].astype(np.int64)


def block_name(p):
    n = p.get("Name", "minecraft:air")
    props = p.get("Properties") or {}
    # A few properties change how a block looks from above.
    extra = ""
    if "snowy" in props and props["snowy"] == "true":
        extra = "[snowy]"
    if n.endswith("_slab") and props.get("type") == "double":
        extra = ""
    return n + extra


AIRS = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:light", "minecraft:barrier", "minecraft:structure_void"}
# Things too small or thin to see on a map: the block under them shows instead.
SEE_THROUGH = ("minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern", "minecraft:dead_bush", "_button",
               "minecraft:torch", "minecraft:wall_torch", "minecraft:lantern", "_pressure_plate", "minecraft:tripwire", "minecraft:redstone_wire",
               "minecraft:rail", "_rail", "_sign", "_banner", "minecraft:lever", "minecraft:ladder", "_carpet_never", "minecraft:glass_pane",
               "minecraft:string", "minecraft:cobweb", "minecraft:snow_never", "minecraft:vine", "_head", "_skull", "minecraft:flower_pot",
               "potted_", "minecraft:end_rod", "minecraft:chain", "minecraft:iron_bars", "minecraft:candle", "_candle", "minecraft:scaffolding")


def see_through(name):
    for s in SEE_THROUGH:
        if s in name:
            return True
    return False


def chunk_columns(c, names, index):
    """For one chunk: (16x16 top index, top y, water depth), or None."""
    status = c.get("Status", "")
    if status and "full" not in status and "minecraft:full" != status:
        pass
    secs = c.get("sections") or []
    if not secs:
        return None
    ymin = min(s.get("Y", 0) for s in secs)
    ymax = max(s.get("Y", 0) for s in secs)
    nsec = ymax - ymin + 1
    # Every section's blocks as global indices (palette mapped), 4096 each (y, z, x order).
    vol = np.zeros((nsec, 16, 16, 16), np.int32)  # global index; 0 = air
    for s in secs:
        bs = s.get("block_states")
        if not bs:
            continue
        pal = bs.get("palette") or []
        gid = []
        for p in pal:
            nm = block_name(p)
            if nm in AIRS or see_through(nm):
                gid.append(0)
                continue
            if nm not in index:
                index[nm] = len(names)
                names.append(nm)
            gid.append(index[nm])
        gid = np.array(gid, np.int32)
        sy = s.get("Y", 0) - ymin
        if len(pal) == 1:
            vol[sy] = gid[0]
            continue
        data = bs.get("data")
        if data is None:
            continue
        bits = max(4, int(np.ceil(np.log2(len(pal)))))
        idx = unpack(data, bits, 4096)
        idx = np.clip(idx, 0, len(gid) - 1)
        vol[sy] = gid[idx].reshape(16, 16, 16)
    # Flatten to (Y, z, x) from the bottom.
    col = vol.reshape(nsec * 16, 16, 16)
    solid = col != 0
    any_ = solid.any(axis=0)
    # Topmost solid: last True along y.
    top_from_top = np.argmax(solid[::-1], axis=0)
    top = (nsec * 16 - 1) - top_from_top
    top_id = np.take_along_axis(col, top[None], axis=0)[0]
    top_id[~any_] = 0
    top_y = np.where(any_, top + ymin * 16, -64)
    # Water depth: how many water blocks from the top down.
    water = index.get("minecraft:water")
    depth = np.zeros((16, 16), np.int16)
    if water is not None:
        is_w = col == water
        if is_w.any():
            # Walk down from the top while it's water (up to 24).
            for k in range(24):
                yy = top - k
                ok = yy >= 0
                ww = np.zeros((16, 16), bool)
                ww[ok] = np.take_along_axis(is_w, np.clip(yy, 0, None)[None], axis=0)[0][ok]
                still = (depth == k) & ww
                depth[still] = k + 1
    return top_id, top_y, depth


def read_region(path, names, index):
    out_id = np.zeros((512, 512), np.int32)
    out_y = np.full((512, 512), -64, np.int16)
    out_d = np.zeros((512, 512), np.int16)
    with open(path, "rb") as f:
        data = f.read()
    if len(data) < 8192:
        return out_id, out_y, out_d, 0
    n = 0
    for i in range(1024):
        off = int.from_bytes(data[i * 4:i * 4 + 3], "big")
        cnt = data[i * 4 + 3]
        if off == 0 or cnt == 0:
            continue
        p = off * 4096
        ln = int.from_bytes(data[p:p + 4], "big")
        comp = data[p + 4]
        raw = data[p + 5:p + 4 + ln]
        try:
            b = zlib.decompress(raw) if comp == 2 else (__import__("gzip").decompress(raw) if comp == 1 else raw)
            c = nbt(b)
        except Exception:
            continue
        if "Level" in c:
            continue  # pre-1.18 chunks aren't read
        r = chunk_columns(c, names, index)
        if r is None:
            continue
        cx, cz = i % 32, i // 32
        out_id[cz * 16:cz * 16 + 16, cx * 16:cx * 16 + 16] = r[0]
        out_y[cz * 16:cz * 16 + 16, cx * 16:cx * 16 + 16] = r[1]
        out_d[cz * 16:cz * 16 + 16, cx * 16:cx * 16 + 16] = r[2]
        n += 1
    return out_id, out_y, out_d, n


def job(args):
    path, out = args
    names, index = ["minecraft:air"], {"minecraft:air": 0}
    rid, ry, rd, n = read_region(path, names, index)
    base = os.path.basename(path)[2:-4]
    np.savez_compressed(os.path.join(out, base + ".npz"), id=rid, y=ry, d=rd, names=np.array(names))
    return base, n


def main():
    world, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    reg = os.path.join(world, "region")
    files = sorted(f for f in os.listdir(reg) if f.endswith(".mca"))
    jobs = [(os.path.join(reg, f), out) for f in files]
    total = 0
    with Pool(max(1, os.cpu_count() or 1)) as pool:
        for base, n in pool.imap_unordered(job, jobs):
            total += n
            print(base, n, flush=True)
    print("chunks", total)


if __name__ == "__main__":
    main()
