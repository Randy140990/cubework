#!/usr/bin/env python3
"""
(0.73) Every block John changed between two saves of his world: the chunks whose blocks differ, and each changed
block's old and new state. For finding what a new map save brings (his tunnels, a station copied over, a tree).

    python3 -I tools/worlddiff.py <old world folder> <new world folder> <out.json>

Writes {"chunks": [[cx, cz, changed], ...], "blocks": [[x, y, z, old, new], ...]} (blocks capped per chunk).
Stdlib and numpy; chunks whose bytes are the same in both are skipped unread.
"""
import gzip, json, os, struct, sys, zlib
from multiprocessing import Pool

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mapbake  # noqa: E402
from spotworld import state_name  # noqa: E402

CAP = 4096


def chunks_of(path):
    if not os.path.exists(path): return {}
    data = open(path, "rb").read()
    out = {}
    for i in range(1024):
        off = (data[i * 4] << 16 | data[i * 4 + 1] << 8 | data[i * 4 + 2]) * 4096
        if off == 0: continue
        n = struct.unpack_from(">i", data, off)[0]
        out[i] = (data[off + 4], data[off + 5:off + 4 + n])
    return out


def parse(comp, raw):
    b = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
    return mapbake.nbt(b)


def volumes(c):
    """{section Y: 4096 state names (y, z, x order)} for a chunk."""
    out = {}
    for s in c.get("sections") or []:
        bs = s.get("block_states")
        if not bs: continue
        pal = [state_name(p) for p in bs.get("palette") or []]
        if not pal: continue
        if len(pal) == 1:
            out[s.get("Y", 0)] = np.array([pal[0]] * 4096, dtype=object)
            continue
        data = bs.get("data")
        if data is None: continue
        bits = max(4, int(np.ceil(np.log2(len(pal)))))
        idx = np.clip(mapbake.unpack(data, bits, 4096), 0, len(pal) - 1)
        out[s.get("Y", 0)] = np.array(pal, dtype=object)[idx]
    return out


def job(args):
    old_path, new_path, rx, rz = args
    a, b = chunks_of(old_path), chunks_of(new_path)
    chunks, blocks = [], []
    for i in sorted(set(a) | set(b)):
        if a.get(i) == b.get(i): continue
        cx, cz = rx * 32 + (i & 31), rz * 32 + (i >> 5)
        va = volumes(parse(*a[i])) if i in a else {}
        vb = volumes(parse(*b[i])) if i in b else {}
        n = 0
        for sy in sorted(set(va) | set(vb)):
            x = va.get(sy)
            y = vb.get(sy)
            if x is None: x = np.array(["minecraft:air"] * 4096, dtype=object)
            if y is None: y = np.array(["minecraft:air"] * 4096, dtype=object)
            diff = np.nonzero(x != y)[0]
            for k in diff:
                n += 1
                if n <= CAP:
                    ly, lz, lx = k >> 8, (k >> 4) & 15, k & 15
                    blocks.append([cx * 16 + int(lx), sy * 16 + int(ly), cz * 16 + int(lz), x[k], y[k]])
        if n: chunks.append([cx, cz, n])
    return chunks, blocks


def main():
    old, new, out = sys.argv[1:4]
    names = set(os.listdir(os.path.join(old, "region"))) | set(os.listdir(os.path.join(new, "region")))
    jobs = []
    for nm in sorted(names):
        if not nm.endswith(".mca"): continue
        _, rx, rz, _ = nm.split(".")
        jobs.append((os.path.join(old, "region", nm), os.path.join(new, "region", nm), int(rx), int(rz)))
    chunks, blocks = [], []
    with Pool(2) as pool:
        for c, b in pool.imap_unordered(job, jobs):
            chunks += c
            blocks += b
    json.dump({"chunks": chunks, "blocks": blocks}, open(out, "w"))
    print(len(chunks), "chunks changed,", sum(c[2] for c in chunks), "blocks")


if __name__ == "__main__":
    main()
