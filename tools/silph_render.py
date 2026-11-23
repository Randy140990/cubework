"""Draws a FireRed map from pret (its layout and tilesets) to a picture: python3 silph_render.py MAP OUT [scale] (tools/silph.py uses it to sort the tiles)."""
import json, struct, sys, os
from PIL import Image
P = "/home/claude/pret/pokefirered"
def pal(path):
    lines = open(path).read().split("\n")
    cols = [tuple(map(int, l.split())) for l in lines[3:19] if l.strip()]
    return cols
def tileset(name, kind):
    d = os.path.join(P, "data/tilesets", kind, name)
    img = Image.open(os.path.join(d, "tiles.png"))
    pals = [pal(os.path.join(d, "palettes", "%02d.pal" % i)) for i in range(16)]
    mt = open(os.path.join(d, "metatiles.bin"), "rb").read()
    return img, pals, mt
def draw(mapname, scale=1, layout_override=None, blocks_override=None):
    L = {l["id"]: l for l in json.load(open(os.path.join(P, "data/layouts/layouts.json")))["layouts"] if "id" in l}
    m = json.load(open(os.path.join(P, "data/maps", mapname, "map.json")))
    l = L[layout_override or m["layout"]]
    w, h = l["width"], l["height"]
    blocks = struct.unpack("<%dH" % (w * h), open(os.path.join(P, l["blockdata_filepath"]), "rb").read()[:w * h * 2])
    if blocks_override is not None:
        w, h, blocks = blocks_override
    pname = l["primary_tileset"].replace("gTileset_", ""); sname = l["secondary_tileset"].replace("gTileset_", "")
    snake = lambda s: "".join(("_" + c.lower() if c.isupper() and i else c.lower()) for i, c in enumerate(s))
    prim = tileset(snake(pname), "primary")
    if sname == "SilphCo":
        c = tileset("condominiums", "secondary")
        sec = (c[0], c[1], open(os.path.join(P, "data/tilesets/secondary/silph_co/metatiles.bin"), "rb").read())
    else: sec = tileset(snake(sname), "secondary")
    pimg, ppal, pmt = prim; simg, spal, smt = sec
    pals = ppal[:7] + spal[7:13] + ppal[13:16]
    out = Image.new("RGB", (w * 16, h * 16))
    def tile_px(t):
        img = pimg if t < 640 else simg
        t2 = t if t < 640 else t - 640
        tw = img.width // 8
        tx, ty = (t2 % tw) * 8, (t2 // tw) * 8
        return img, tx, ty
    for i, b in enumerate(blocks):
        mid = b & 0x3FF
        data = pmt if mid < 640 else smt
        off = (mid if mid < 640 else mid - 640) * 16
        ents = struct.unpack("<8H", data[off:off + 16]) if off + 16 <= len(data) else (0,) * 8
        bx, by = (i % w) * 16, (i // w) * 16
        for layer in range(2):
            for q in range(4):
                e = ents[layer * 4 + q]
                t, hf, vf, pn = e & 0x3FF, (e >> 10) & 1, (e >> 11) & 1, e >> 12
                img, tx, ty = tile_px(t)
                if tx + 8 > img.width or ty + 8 > img.height: continue
                for yy in range(8):
                    for xx in range(8):
                        sx, sy = (7 - xx if hf else xx), (7 - yy if vf else yy)
                        ci = img.getpixel((tx + sx, ty + sy))
                        if isinstance(ci, tuple): ci = ci[0]
                        if ci == 0 and layer: continue
                        if ci == 0 and not layer:
                            c = pals[pn][0] if pn < len(pals) else (0, 0, 0)
                        else:
                            c = pals[pn][ci] if pn < len(pals) and ci < len(pals[pn]) else (255, 0, 255)
                        out.putpixel((bx + (q % 2) * 8 + xx, by + (q // 2) * 8 + yy), c)
    if scale != 1: out = out.resize((out.width * scale, out.height * scale), Image.NEAREST)
    return out
if __name__ == "__main__":
    draw(sys.argv[1], int(sys.argv[3]) if len(sys.argv) > 3 else 1).save(sys.argv[2])
