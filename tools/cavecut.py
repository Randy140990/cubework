"""
(0.52, John's 09:05) The caves cut away on the camera's side, as the rooms are: "at the bottom edge of the screen, the
wall should be open, as in interiors, or at least only one block high, not many".

The camera looks north from the south at 52 degrees, so rock standing south of a floor hides the floor behind it. Any
rock within REACH blocks south of a floor lower than it (by 3 or more: a step of 2 between B2F's levels isn't a wall)
is cut down to one block over that floor. Rock north of a floor (the cave's back walls) stays as high as it is. A
column with a ladder on it (the ladders up face south, on the walls north of their floors) is left whole.

Used by tools/caves.py, tools/mtmoon.py and tools/caves48.py on the blocks they write (names in y, z, x order).
"""

REACH = 8
SOFT = {"air", "cave_air", "light", "ladder", "glow_lichen", "vine"}


def _base(n):
    return n.replace("minecraft:", "").split("[")[0]


def cut(names, W, H, L, reach=REACH):
    """The blocks with the rock south of every floor cut down to a block over it (a new list)."""
    out = list(names)
    base = [_base(n) for n in names]
    at = lambda x, y, z: (y * L + z) * W + x
    top = [[-1] * W for _ in range(L)]
    ladder = [[False] * W for _ in range(L)]
    for z in range(L):
        for x in range(W):
            for y in range(H - 1, -1, -1):
                b = base[at(x, y, z)]
                if b == "ladder": ladder[z][x] = True
                if b not in SOFT and top[z][x] < 0: top[z][x] = y
    n_cut = 0
    for x in range(W):
        for z in range(L):
            h = top[z][x]
            if h < 0: continue
            # A ladder hung on this rock (from the block south of it), or on this column: leave it whole.
            if ladder[z][x] or (z + 1 < L and ladder[z + 1][x]): continue
            cap = None
            for d in range(1, reach + 1):
                if z - d < 0: break
                f = top[z - d][x]
                if 0 <= f and h - f >= 3:
                    cap = f + 1 if cap is None else min(cap, f + 1)
            if cap is None or cap >= h: continue
            for y in range(cap + 1, H):
                i = at(x, y, z)
                if base[i] not in SOFT:
                    out[i] = "minecraft:air"
                    n_cut += 1
            # (The daylight John's doorways have at their backs: not left as a white block at the foot.)
            for y in range(0, cap + 1):
                i = at(x, y, z)
                if base[i] == "white_concrete" and y >= cap:
                    out[i] = out[at(x, y - 1, z)] if y > 0 and base[at(x, y - 1, z)] not in SOFT else "minecraft:air"
    return out, n_cut
