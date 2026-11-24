"""Finds doors in a box of the Kanto map with no room behind them (not near a door in rooms.json): each door block's
lower half, which way it faces (the side open to the sky), and the ground out there."""
import sys, json
sys.path.insert(0, '/home/claude/scan51'); import k
import numpy as np
M = k.Map(sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].lstrip('-').isdigit() else 'kanto')
R = json.load(open("/home/claude/route-one/src/main/resources/retromon/interiors/rooms.json"))
dp = [tuple(b) for d in R["doors"].values() for b in d["door"]]
def b(x, y, z):
    try: return M.base[M.A[y - M.Y0, z - M.Z0, x - M.X0]]
    except Exception: return '?'
AIR = {'air', 'cave_air', 'light', 'short_grass', 'tall_grass'}
def sky(x, y, z):
    return all(b(x, yy, z) in AIR for yy in range(y + 2, y + 10))
def doors(x0, x1, z0, z1, y0, y1, near=3):
    idx = [i for i, n in enumerate(M.base) if n.endswith('_door') and 'trap' not in n]
    sub = M.A[y0 - M.Y0:y1 - M.Y0 + 1, z0 - M.Z0:z1 - M.Z0 + 1, x0 - M.X0:x1 - M.X0 + 1]
    ys, zs, xs = np.nonzero(np.isin(sub, idx))
    pts = sorted(set((int(x + x0), int(y + y0), int(z + z0)) for x, z, y in zip(xs, zs, ys)))
    low = [p for p in pts if (p[0], p[1] - 1, p[2]) not in pts]
    out = []
    for x, y, z in low:
        if any(abs(x - a) <= near and abs(z - c) <= near and abs(y - yy) <= 4 for a, yy, c in dp): continue
        face = [f for f, (dx, dz) in (('n', (0, -1)), ('s', (0, 1)), ('w', (-1, 0)), ('e', (1, 0))) if b(x + dx, y, z + dz) in AIR and sky(x + 2 * dx, y, z + 2 * dz)]
        out.append((x, y, z, b(x, y, z), face))
    return out
if __name__ == "__main__":
    a = [int(v) for v in sys.argv[-6:]]
    for d in doors(*a): print(d)
