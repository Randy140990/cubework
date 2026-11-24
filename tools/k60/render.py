"""Draws a FireRed map from pret with each tile's metatile id (hex) and collision marked, for sorting its tiles by eye.
python3 -I tools/k60/render.py MAP OUT.png [scale]"""
import json, os, struct, sys
from PIL import Image, ImageDraw
P = "/home/claude/pret/pokefirered"
SNAKE = {"SSAnne": "ss_anne", "GenericBuilding1": "generic_building_1", "Building": "building", "General": "general"}


def snake(s):
    return SNAKE.get(s) or "".join(("_" + c.lower() if c.isupper() and i else c.lower()) for i, c in enumerate(s))


def pal(path):
    lines = open(path).read().split("\n")
    return [tuple(map(int, l.split())) for l in lines[3:19] if l.strip()]


def tileset(name, kind):
    d = os.path.join(P, "data/tilesets", kind, name)
    img = Image.open(os.path.join(d, "tiles.png"))
    pals = [pal(os.path.join(d, "palettes", "%02d.pal" % i)) for i in range(16)]
    return img, pals, open(os.path.join(d, "metatiles.bin"), "rb").read(), open(os.path.join(d, "metatile_attributes.bin"), "rb").read()


def layout(mapname):
    L = {l["id"]: l for l in json.load(open(os.path.join(P, "data/layouts/layouts.json")))["layouts"] if "id" in l}
    m = json.load(open(os.path.join(P, "data/maps", mapname, "map.json")))
    l = L[m["layout"]]
    w, h = l["width"], l["height"]
    blocks = struct.unpack("<%dH" % (w * h), open(os.path.join(P, l["blockdata_filepath"]), "rb").read()[:w * h * 2])
    return m, l, w, h, list(blocks)


def tilesets(l):
    prim = tileset(snake(l["primary_tileset"].replace("gTileset_", "")), "primary")
    sname = l["secondary_tileset"].replace("gTileset_", "")
    if sname == "SilphCo":
        c = tileset("condominiums", "secondary")
        d = os.path.join(P, "data/tilesets/secondary/silph_co")
        sec = (c[0], c[1], open(os.path.join(d, "metatiles.bin"), "rb").read(), open(os.path.join(d, "metatile_attributes.bin"), "rb").read())
    else: sec = tileset(snake(sname), "secondary")
    return prim, sec


def attrs(l):
    """Each metatile's behaviour (FireRed: 32-bit attributes, the low 9 bits)."""
    prim, sec = tilesets(l)
    def get(mid):
        a = prim[3] if mid < 640 else sec[3]
        i = mid if mid < 640 else mid - 640
        if 4 * i + 4 > len(a): return 0
        return struct.unpack("<I", a[4 * i:4 * i + 4])[0] & 0x1FF
    return get


def draw(mapname, scale=1, ids=False):
    m, l, w, h, blocks = layout(mapname)
    prim, sec = tilesets(l)
    pimg, ppal, pmt, _ = prim; simg, spal, smt, _ = sec
    pals = ppal[:7] + spal[7:13] + ppal[13:16]
    out = Image.new("RGB", (w * 16, h * 16))
    px = out.load()
    def tile_px(t):
        img = pimg if t < 640 else simg
        t2 = t if t < 640 else t - 640
        tw = img.width // 8
        return img, (t2 % tw) * 8, (t2 // tw) * 8
    cache = {}
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
                key = (id(img), tx, ty)
                if key not in cache: cache[key] = [[img.getpixel((tx + xx, ty + yy)) for xx in range(8)] for yy in range(8)]
                tp = cache[key]
                for yy in range(8):
                    for xx in range(8):
                        ci = tp[7 - yy if vf else yy][7 - xx if hf else xx]
                        if isinstance(ci, tuple): ci = ci[0]
                        if ci == 0 and layer: continue
                        c = pals[pn][ci] if pn < len(pals) and ci < len(pals[pn]) else (255, 0, 255)
                        px[bx + (q % 2) * 8 + xx, by + (q // 2) * 8 + yy] = c
    if scale != 1: out = out.resize((out.width * scale, out.height * scale), Image.NEAREST)
    if ids:
        d = ImageDraw.Draw(out)
        s = scale * 16
        for i, b in enumerate(blocks):
            x, y = (i % w) * s, (i // w) * s
            col = (b >> 10) & 3
            d.rectangle([x, y, x + s - 1, y + s - 1], outline=(255, 0, 0) if col else (0, 200, 255))
            d.text((x + 2, y + 2), "%x" % (b & 0x3FF), fill=(255, 255, 0) if col else (255, 255, 255))
    return out


if __name__ == "__main__":
    draw(sys.argv[1], int(sys.argv[3]) if len(sys.argv) > 3 else 1, ids=len(sys.argv) > 4).save(sys.argv[2])
