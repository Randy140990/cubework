"""
(0.52, John's 09:05) The caves cut away on the camera's side, as the rooms are: "at the bottom edge of the screen, the
wall should be open, as in interiors, or at least only one block high, not many".

The camera looks north from the south at 52 degrees, so rock standing south of a floor hides the floor behind it. Any
rock within REACH blocks south of a floor lower than it (by 3 or more: a step of 2 between B2F's levels isn't a wall)
is cut down to three blocks over that floor (0.61; one block before). Rock north of a floor (the cave's back walls) stays as high as it is. A
column with a ladder on it (the ladders up face south, on the walls north of their floors) is left whole.

Used by tools/caves.py, tools/mtmoon.py and tools/caves48.py on the blocks they write (names in y, z, x order).
"""

REACH = 8
KEEP = 3
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
    pit = [[False] * W for _ in range(L)]
    for z in range(L):
        for x in range(W):
            for y in range(H - 1, -1, -1):
                b = base[at(x, y, z)]
                if b == "ladder": ladder[z][x] = True
                if b == "black_concrete": pit[z][x] = True
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
                # (0.61, John's 18:16: stuck "near ladder areas") Not a hole's bottom or a ladder's well: the floor south
                # of a hole is floor, not a wall to cut down (cut, it left a trench a block deep you couldn't climb out of).
                if pit[z - d][x] or ladder[z - d][x]: continue
                # (0.61, John's 18:16: "only lower them down to the 3 ... we're 2 blocks tall, lower them down to the 3
                # blocks, so we'll still be able to see beyond it and still see ourselves") Three blocks over the
                # floor, everywhere, not one.
                if 0 <= f and h - f > KEEP:
                    cap = f + KEEP if cap is None else min(cap, f + KEEP)
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


# (0.61, John's 18:16: "offshoots where ... I've just got stuck") The spots tools/cavewalk.py finds you can get into but
# not out of (no jumping in the mod: up half a block at a time), each given a way out: Mt. Moon B1F's trench by the
# hole (its floor's top layer missing: filled), a pocket on Cerulean Cave 1F (its floor raised to the bank's), a step
# out of Cerulean Cave B1F's low ground by MEWTWO up onto the shelf (a half block), and a ramp of half blocks up out of
# Victory Road 1F's basin onto the ledge west of it. (bin x, y, z, block)
SLAB = "[type=bottom,waterlogged=false]"
FILLS = {
    "mt_moon_b1f": [(x, 2, z, "minecraft:andesite") for x in (82, 83) for z in range(16, 22)],
    "cerulean_cave_1f": [(x, 7, z, "minecraft:stone") for x in (40, 41) for z in (54, 55)] + [(x, 8, z, "minecraft:stone_slab" + SLAB) for x in (40, 41) for z in (54, 55)],
    "cerulean_cave_b1f": [(31, 7, z, "minecraft:stone_slab" + SLAB) for z in (36, 37)] + [b for x in (16, 17) for b in (
        # Steps out of the water (by the ladder's ledge, west of where you drop in): up out of the water onto a block
        # (as anyone can, swimming), then half a block at a time up onto the ledge.
        [(x, y, 35, "minecraft:mossy_cobblestone") for y in range(2, 8)] +
        [(x, y, 34, "minecraft:mossy_cobblestone") for y in range(2, 8)] + [(x, 8, 34, "minecraft:mossy_cobblestone_slab" + SLAB)] +
        [(x, y, 33, "minecraft:mossy_cobblestone") for y in range(2, 8)] + [(x, 8, 33, "minecraft:moss_block")] +
        [(x, y, 32, "minecraft:mossy_cobblestone") for y in range(2, 9)] + [(x, 9, 32, "minecraft:mossy_cobblestone_slab" + SLAB)])],
    "victory_road_1f": [b for z in (24, 25) for b in (
        (39, 8, z, "minecraft:andesite_slab" + SLAB),
        (38, 8, z, "minecraft:dripstone_block"),
        (37, 8, z, "minecraft:dripstone_block"), (37, 9, z, "minecraft:andesite_slab" + SLAB),
        (36, 8, z, "minecraft:dripstone_block"), (36, 9, z, "minecraft:andesite"))] +
    # The boulder (FireRed's rock barrier, that a boulder on the switch opens) between the low ground behind it and the
    # way on: open, as FireRed's is once it's solved; you can drop in from the ledges round it, so it mustn't shut you in.
    [(x, y, z, "minecraft:air") for x in (44, 45) for z in (18, 19) for y in (7, 8)],
}


def fill(names, W, H, L, rid):
    out = list(names)
    for x, y, z, n in FILLS.get(rid, ()):
        if 0 <= x < W and 0 <= y < H and 0 <= z < L: out[(y * L + z) * W + x] = n
    return out
