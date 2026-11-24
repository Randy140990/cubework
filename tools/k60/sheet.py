"""Contact sheet of every metatile a set of FireRed maps uses (one secondary tileset's), each with its id, collision
and behaviour, for sorting them by eye. python3 -I tools/k60/sheet.py OUT.png MAP [MAP...]"""
import os, sys, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import render
from PIL import Image, ImageDraw

def sheet(out, maps):
    seen = collections.OrderedDict()
    l0 = None
    for m in maps:
        mp, l, w, h, blocks = render.layout(m)
        l0 = l0 or l
        att = render.attrs(l)
        for b in blocks:
            mid = b & 0x3FF
            k = (mid, (b >> 10) & 3)
            if k not in seen: seen[k] = att(mid)
    ids = list(seen.keys())
    # Draw each as a 1-wide map row.
    S = 3
    cols = 12
    rows = (len(ids) + cols - 1) // cols
    im = Image.new("RGB", (cols * (16 * S + 8), rows * (16 * S + 22)), (40, 40, 40))
    d = ImageDraw.Draw(im)
    # Render the tiles via a fake layout: reuse render.draw on the first map with an override of its blocks.
    import struct, json
    mp, l, w, h, blocks = render.layout(maps[0])
    for n, (mid, col) in enumerate(ids):
        one = render_one(maps[0], mid)
        one = one.resize((16 * S, 16 * S), Image.NEAREST)
        x, y = (n % cols) * (16 * S + 8), (n // cols) * (16 * S + 22)
        im.paste(one, (x, y))
        d.text((x, y + 16 * S + 2), "%x c%d b%x" % (mid, col, seen[(mid, col)]), fill=(255, 255, 0) if col else (255, 255, 255))
    im.save(out)
    print(len(ids), "metatiles")

_cache = {}
def render_one(mapname, mid):
    # Draw a 1x1 map of just this metatile.
    m, l, w, h, blocks = render.layout(mapname)
    orig = render.layout
    render.layout = lambda name: (m, l, 1, 1, [mid])
    try: return render.draw(mapname, 1)
    finally: render.layout = orig

if __name__ == "__main__":
    sheet(sys.argv[1], sys.argv[2:])
