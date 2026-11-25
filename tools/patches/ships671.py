"""0.67.1 (John's 17:13): both ships retextured in the game; the viewer from the same pictures."""
R = "/home/claude/route-one/tools/"


def sub(path, old, new):
    s = open(path).read()
    assert s.count(old) == 1, (path, old[:70])
    open(path, "w").write(s.replace(old, new))


sub(R + "shiplooks.py", '''    # (Above the sea bed's stone, y 13, which runs under it and would join on.)
    ("aqua", "S.S. AQUA", "Olivine's harbour", (-2310, -2258, 14, 70, 650, 870), "z"),
]''', '''    ("aqua", "S.S. AQUA", "Olivine's harbour", (-2310, -2258, 5, 70, 650, 870), "z"),
]
# What isn't the ship though it's a block the ship has too: under the S.S. AQUA, the sea bed's stone (its top at y 13),
# which runs under it and would join on.
SEABED = {"aqua": (14, {"stone"})}


def ship_mask(sid, bn, ids, y0):
    """The ship's blocks in its box: the biggest piece of touching blocks, less the sea and the sea bed."""
    from scipy import ndimage  # noqa
    skip = np.array([SV.skipped(b) for b in bn])
    S = ~skip[ids]
    if sid in SEABED:
        below, blocks = SEABED[sid]
        bad = np.array([b in blocks for b in bn])[ids]
        k = max(0, below - y0)
        S[:k] &= ~bad[:k]
    lab, n = ndimage.label(S, structure=np.ones((3, 3, 3)))
    sizes = ndimage.sum(S, lab, range(1, n + 1))
    return lab == (1 + int(np.argmax(sizes)))''')
sub(R + "shiplooks.py", '''        bn = [SV.bname(n) for n in names]
        skip = np.array([SV.skipped(b) for b in bn])
        S = ~skip[ids]
        from scipy import ndimage  # noqa
        lab, n = ndimage.label(S, structure=np.ones((3, 3, 3)))
        sizes = ndimage.sum(S, lab, range(1, n + 1))
        ship = lab == (1 + int(np.argmax(sizes)))
        zz = np.nonzero(ship)''', '''        bn = [SV.bname(n) for n in names]
        ship = ship_mask(sid, bn, ids, y0)
        zz = np.nonzero(ship)''')
sub(R + "shiplooks.py", '''        for a in range(lo, hi + 1, 16):
            b2 = min(hi, a + 15)
            area = (a, bx[4], b2, bx[5]) if slice_ax == "x" else (bx[0], a, bx[1], b2)
            rows.append({"blocks": ",".join(swapped), "box": list(area), "ys": [bx[2], bx[3]], "spec": spec, "note": "the %s, retextured" % name})''',
    '''        bands = [(bx[2], bx[3], swapped)]
        if sid in SEABED and bx[2] < SEABED[sid][0]:
            below, blocks = SEABED[sid]
            bands = [(bx[2], below - 1, [b for b in swapped if b not in blocks]), (below, bx[3], swapped)]
        for a in range(lo, hi + 1, 16):
            b2 = min(hi, a + 15)
            area = (a, bx[4], b2, bx[5]) if slice_ax == "x" else (bx[0], a, bx[1], b2)
            for ya, yb, bl in bands:
                rows.append({"blocks": ",".join(bl), "box": list(area), "ys": [ya, yb], "spec": spec, "note": "the %s, retextured" % name})''')

s = open(R + "shipview.py").read()
s = s.replace("BOX = (-476, -262, 18, 72, 616, 666)  # x0 x1 y0 y1 z0 z1 (the bow's tip is at x -465)\n", "")
i = s.index("def main(world, assets):")
s = s[:i] + open(R + "patches/shipview_main671.txt").read()
open(R + "shipview.py", "w").write(s)
print("ok")
