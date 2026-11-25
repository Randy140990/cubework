#!/usr/bin/env python3
"""
(0.64) Blocks out of John's Kanto world (its region files) and out of the caves' block rooms (.bin), for the battles
fought on the spot (tools/spots.py). Stdlib and numpy; the NBT read as tools/mapbake.py reads it.

    import spotworld
    W = spotworld.World(<world folder>)
    box = W.box(x0, x1, y0, y1, z0, z1)      # names (list of block states), ids [y][z][x] (numpy int32)
    room = spotworld.binroom(<path to .bin>) # the same for a block room: (names, ids, W, H, L)
"""
import gzip, os, struct, sys, zlib

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mapbake  # noqa: E402  (its NBT reader and bit unpacking)


def state_name(p):
    """A block state as the game writes it: name[key=value,...], the keys in order."""
    n = p.get("Name", "minecraft:air")
    props = p.get("Properties") or {}
    if not props: return n
    return n + "[" + ",".join("%s=%s" % (k, props[k]) for k in sorted(props)) + "]"


class World:
    def __init__(self, folder):
        self.folder = folder
        self.cache = {}

    def _region(self, rx, rz):
        key = (rx, rz)
        if key in self.cache: return self.cache[key]
        path = os.path.join(self.folder, "region", "r.%d.%d.mca" % (rx, rz))
        data = open(path, "rb").read() if os.path.exists(path) else None
        self.cache[key] = data
        return data

    def chunk(self, cx, cz):
        data = self._region(cx >> 5, cz >> 5)
        if data is None: return None
        i = ((cx & 31) + (cz & 31) * 32) * 4
        off = (data[i] << 16 | data[i + 1] << 8 | data[i + 2]) * 4096
        if off == 0: return None
        n = struct.unpack_from(">i", data, off)[0]
        comp = data[off + 4]
        raw = data[off + 5:off + 4 + n]
        b = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
        return mapbake.nbt(b)

    def box(self, x0, x1, y0, y1, z0, z1):
        """Every block from (x0, y0, z0) to (x1, y1, z1) inclusive: (names, ids[y - y0][z - z0][x - x0])."""
        names, index = ["minecraft:air"], {"minecraft:air": 0}
        out = np.zeros((y1 - y0 + 1, z1 - z0 + 1, x1 - x0 + 1), np.int32)
        for cz in range(z0 >> 4, (z1 >> 4) + 1):
            for cx in range(x0 >> 4, (x1 >> 4) + 1):
                c = self.chunk(cx, cz)
                if c is None: continue
                for s in c.get("sections") or []:
                    sy = s.get("Y", 0)
                    if (sy + 1) * 16 <= y0 or sy * 16 > y1: continue
                    bs = s.get("block_states")
                    if not bs: continue
                    pal = bs.get("palette") or []
                    gid = []
                    for p in pal:
                        nm = state_name(p)
                        if nm not in index:
                            index[nm] = len(names); names.append(nm)
                        gid.append(index[nm])
                    gid = np.array(gid, np.int32)
                    if len(pal) == 1:
                        vol = np.full((16, 16, 16), gid[0], np.int32)
                    else:
                        data = bs.get("data")
                        if data is None: continue
                        bits = max(4, int(np.ceil(np.log2(len(pal)))))
                        idx = np.clip(mapbake.unpack(data, bits, 4096), 0, len(gid) - 1)
                        vol = gid[idx].reshape(16, 16, 16)
                    # The section's part inside the box.
                    for ly in range(16):
                        y = sy * 16 + ly
                        if y < y0 or y > y1: continue
                        zs, ze = max(z0, cz * 16), min(z1, cz * 16 + 15)
                        xs, xe = max(x0, cx * 16), min(x1, cx * 16 + 15)
                        if zs > ze or xs > xe: continue
                        out[y - y0, zs - z0:ze - z0 + 1, xs - x0:xe - x0 + 1] = vol[ly, zs - cz * 16:ze - cz * 16 + 1, xs - cx * 16:xe - cx * 16 + 1]
        return names, out


def binroom(path):
    """A block room's blocks (the .bin tools/caves.py writes): (names, ids[y][z][x], W, H, L)."""
    b = gzip.open(path, "rb").read()
    assert b[:4] == b"RMF1"
    W, H, L, n = struct.unpack_from(">hhhh", b, 4)
    i = 12
    names = []
    for _ in range(n):
        ln = struct.unpack_from(">h", b, i)[0]; i += 2
        names.append(b[i:i + ln].decode("utf-8")); i += ln
    seq = np.zeros(W * H * L, np.int32)
    k = 0

    def varint():
        nonlocal i
        v = s = 0
        while True:
            c = b[i]; i += 1
            v |= (c & 0x7F) << s; s += 7
            if not c & 0x80: return v
    while k < W * H * L and i < len(b):
        run = varint(); val = varint()
        seq[k:k + run] = val; k += run
    return names, seq.reshape(H, L, W), W, H, L
