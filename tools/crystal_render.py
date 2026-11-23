#!/usr/bin/env python3
"""
Reads a pokecrystal map (pret/pokecrystal: maps/*.blk, data/tilesets, gfx/tilesets) and draws it, and gives each of its
steps (a 16x16 square, the size a person walks) its collision and the tiles on it, for tools/johto_rooms.py.

    python3 -I tools/crystal_render.py MAP OUT.png [scale]
"""
import os, re, sys
from PIL import Image

P = "/home/claude/pret/pokecrystal"
PALS = ["GRAY", "RED", "GREEN", "WATER", "YELLOW", "BROWN", "ROOF", "TEXT"]


def snake(s):
    return re.sub(r"(?<!^)(?=[A-Z])", "_", s).lower()


def header(m):
    """The map's tileset and size (in blocks)."""
    ts = None
    for line in open(os.path.join(P, "data/maps/maps.asm")):
        mm = re.match(r"\s*map (\w+), (TILESET_\w+)", line)
        if mm and mm.group(1) == m: ts = mm.group(2).replace("TILESET_", "").lower(); break
    const = snake(m).upper()
    # (the constants spell numbers apart: SPROUT_TOWER_1F, ROUTE_35_NATIONAL_PARK_GATE.)
    const = re.sub(r"(\d+)_F\b", r"\1F", re.sub(r"([A-Z])(\d)", r"\1_\2", const))
    w = h = None
    for line in open(os.path.join(P, "constants/map_constants.asm")):
        mm = re.match(r"\s*map_const (\w+),\s*(\d+),\s*(\d+)", line)
        if mm and mm.group(1) == const: w, h = int(mm.group(2)), int(mm.group(3)); break
    return ts, w, h


def palettes():
    """The indoor palettes (bg_tiles.pal), 0..1 RGB, a list of 4 per palette."""
    lines = open(os.path.join(P, "gfx/tilesets/bg_tiles.pal")).read().split("; indoor")[1].strip().split("\n\n")[0].split("\n")
    out = []
    for ln in lines:
        if "RGB" not in ln: continue
        nums = [int(n) for n in re.findall(r"\d+", ln.split(";")[0])]
        out.append([tuple(int(c * 255 / 31) for c in nums[i:i + 3]) for i in range(0, 12, 3)])
    return out


def palette_map(ts):
    vals = []
    for line in open(os.path.join(P, "gfx/tilesets", ts + "_palette_map.asm")):
        mm = re.match(r"\s*tilepal (\d), (.*)", line)
        if mm:
            bank = int(mm.group(1))
            vals += [(PALS.index(p.strip()), bank) for p in mm.group(2).split(",")]
            if len(vals) == 96: vals += [(7, 0)] * 32
    return vals


def collision(ts):
    out = []
    for line in open(os.path.join(P, "data/tilesets", ts + "_collision.asm")):
        mm = re.match(r"\s*tilecoll (.*?)\s*;", line)
        if mm: out.append([c.strip() for c in mm.group(1).split(",")])
    return out


def load(m):
    ts, w, h = header(m)
    blk = open(os.path.join(P, "maps", m + ".blk"), "rb").read()[:w * h]
    meta = open(os.path.join(P, "data/tilesets", ts + "_metatiles.bin"), "rb").read()
    return ts, w, h, list(blk), meta


def steps(m):
    """Each step's (block id, quadrant, collision, its four tile ids): rows of a (2w x 2h) grid."""
    ts, w, h, blk, meta = load(m)
    coll = collision(ts)
    out = []
    for sy in range(h * 2):
        row = []
        for sx in range(w * 2):
            b = blk[(sy // 2) * w + sx // 2]
            q = (sy % 2) * 2 + sx % 2
            t = meta[b * 16:b * 16 + 16]
            tl = (sy % 2) * 8 + (sx % 2) * 2
            row.append({"b": b, "q": q, "c": coll[b][q] if b < len(coll) else "WALL", "t": (t[tl], t[tl + 1], t[tl + 4], t[tl + 5])})
        out.append(row)
    return out


def draw(m, scale=1):
    ts, w, h, blk, meta = load(m)
    img = Image.open(os.path.join(P, "gfx/tilesets", ts + ".png")).convert("L")
    pm, pals = palette_map(ts), palettes()
    out = Image.new("RGB", (w * 32, h * 32))
    px = out.load(); src = img.load()
    tw = img.width // 8
    for i, b in enumerate(blk):
        bx, by = (i % w) * 32, (i // w) * 32
        for k in range(16):
            t = meta[b * 16 + k]
            n = t if t < 0x80 else t - 0x20
            if n >= tw * (img.height // 8): continue
            pal = pals[pm[t][0]] if t < len(pm) else pals[0]
            tx, ty = (n % tw) * 8, (n // tw) * 8
            for yy in range(8):
                for xx in range(8):
                    v = src[tx + xx, ty + yy]
                    shade = 3 - round(v / 85)
                    px[bx + (k % 4) * 8 + xx, by + (k // 4) * 8 + yy] = pal[shade]
    if scale != 1: out = out.resize((out.width * scale, out.height * scale), Image.NEAREST)
    return out


def events(m):
    """The map's warps, signs and people (pokecrystal's .asm), in steps."""
    txt = open(os.path.join(P, "maps", m + ".asm")).read()
    warps = [dict(x=int(a), z=int(b), to=c, n=int(d)) for a, b, c, d in re.findall(r"warp_event\s+(\d+),\s*(\d+),\s*(\w+),\s*(\d+)", txt)]
    signs = [dict(x=int(a), z=int(b), script=c) for a, b, c in re.findall(r"bg_event\s+(\d+),\s*(\d+),\s*\w+,\s*(\w+)", txt)]
    coords = [dict(x=int(a), z=int(b), script=c) for a, b, c in re.findall(r"coord_event\s+(\d+),\s*(\d+),\s*\w+,\s*(\w+)", txt)]
    objs = []
    for mm in re.finditer(r"object_event\s+(\d+),\s*(\d+),\s*(\w+),\s*(\w+),[^,]*,[^,]*,[^,]*,[^,]*,\s*(\w+),\s*(\w+),\s*(\d+),\s*(\w+),\s*(-?\w+)", txt):
        x, z, spr, mv, pal, kind, sight, script, ev = mm.groups()
        objs.append(dict(x=int(x), z=int(z), sprite=spr.replace("SPRITE_", ""), move=mv.replace("SPRITEMOVEDATA_", ""), kind=kind.replace("OBJECTTYPE_", ""),
                         sight=int(sight), script=script, event=ev))
    return warps, signs, coords, objs


def texts(m):
    """Every text label's words (pokecrystal's text/line/cont/para), cleaned."""
    txt = open(os.path.join(P, "maps", m + ".asm")).read()
    out, cur = {}, None
    for line in txt.split("\n"):
        mm = re.match(r"(\w+):", line)
        if mm: cur = mm.group(1); out[cur] = []; continue
        mm = re.match(r'\s*(text|line|cont|para|next)\s+"(.*)"', line)
        if mm and cur is not None:
            kind, s = mm.groups()
            if kind == "para" and out[cur]: out[cur].append("|")
            out[cur].append(s)
    clean = {}
    for k, v in out.items():
        if not v: continue
        s = " ".join(v).replace("#", "POKé").replace("<PLAYER>", "{player}").replace("<RIVAL>", "{rival}").replace("@", "")
        s = re.sub(r"\s+", " ", s).strip().replace("…", "...")
        # (Crystal splits a long word over two lines with a hyphen: keep the hyphen, lose the gap.)
        s = re.sub(r"(\w)- (\w)", r"\1-\2", s)
        clean[k] = [p.strip() for p in s.split("|") if p.strip()]
    return clean


if __name__ == "__main__":
    draw(sys.argv[1], int(sys.argv[3]) if len(sys.argv) > 3 else 1).save(sys.argv[2])
