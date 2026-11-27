#!/usr/bin/env python3
"""
(0.73, John's 27 Nov 20:11: "if we were to place people, are the people placed in the right places? Do they have the
right proximity ... the right level of stuff ... people like Fisher, NPC, Teacher") Where everyone and everything
FireRed has outside stands on John's own map, worked out by lining each of FireRed's outside maps up against his
world's blocks, and the data for the placement map page (RankedBoost's kind of map, of his Kanto).

His map is FireRed's at 2:1 (each of FireRed's steps two blocks by two), so each FireRed map sits on it at some block
offset. For each map that offset is found by trying every offset near where FireRed's own map layout puts it and
scoring how well the map's tiles agree with the blocks there:

  - FireRed's walls (trees, rocks, buildings) against his leaves and solid blocks;
  - FireRed's water against his water;
  - FireRed's tall grass against his grass (not path);
  - FireRed's ledges against a drop in his ground's height the way the ledge jumps;
  - FireRed's signs against his signs (shelves).

The best offset wins; one whose next best is about as good is marked "check" (the routes whose trees repeat along
them can't tell one step from the next).

    python3 tools/kanto_outdoor_map.py <pokefirered> <world bake folder (tools/mapbake.py's)> <rooms.json> <out folder>

Writes <out>/fit.json (each map's offset and how sure), <out>/data.json (every trainer, person, item ball and hidden
item, with FireRed's step, the world block it's on, which way it faces, how far it sees, and its team, words or item)
and <out>/kanto.png (his Kanto from above, one pixel a block).
"""
import glob
import json
import os
import re
import sys
from collections import deque

import numpy as np

FR, BAKE, ROOMS, OUT = sys.argv[1:5]
HERE = os.path.dirname(os.path.abspath(__file__))
sys.argv = [sys.argv[0], FR]
sys.path.insert(0, HERE)
import fr_placeables as frp  # noqa: E402  (FireRed's scripts, words, trainers, items, and the mod's places)

os.makedirs(OUT, exist_ok=True)
X0, Z0, N = -1024, 0, 1024
# Kanto on his map (blocks).
KX0, KX1, KZ0, KZ1 = -944, -96, 136, 956

# ---------- his world, from above ----------
top = np.full((N, N), "", dtype=object)
hy = np.zeros((N, N), np.int32)
dep = np.zeros((N, N), np.int32)
for rx in (-2, -1):
    for rz in (0, 1):
        z = np.load(os.path.join(BAKE, "%d.%d.npz" % (rx, rz)))
        names = np.array([str(n).replace("minecraft:", "") for n in z["names"]] + [""], dtype=object)
        ids = z["id"].copy()
        ids[ids < 0] = len(names) - 1
        x0, z0 = rx * 512 - X0, rz * 512 - Z0
        top[z0:z0 + 512, x0:x0 + 512] = names[ids]
        hy[z0:z0 + 512, x0:x0 + 512] = z["y"]
        dep[z0:z0 + 512, x0:x0 + 512] = z["d"]

GROUND = ("grass_block", "dirt", "moss", "short_grass", "tall_grass", "fern", "sand", "gravel", "podzol", "poppy", "dandelion", "tulip",
          "orchid", "allium", "bluet", "daisy", "cornflower", "lily_of", "flower", "farmland", "path", "mud", "snow", "rose", "peony", "lilac",
          "sunflower", "pink_petals", "leaf_litter", "bush")
vtop = np.vectorize(lambda t: 1 if "water" in t else 2 if ("leaves" in t or "_log" in t) else 3 if any(k in t for k in GROUND) else 4 if t else 0)
cls = vtop(top).astype(np.int8)
cls[dep > 0] = 1
path = np.isin(top, ["dirt_path", "moss_block", "mud_brick_slab", "cobblestone_slab", "mossy_cobblestone_slab"])
grassb = top == "grass_block"
shelf0 = np.vectorize(lambda t: t.endswith("_shelf") and not t.startswith("spruce"))(top)
shelf = shelf0.copy()
for ddx in (-1, 0, 1):
    for ddz in (-1, 0, 1): shelf |= np.roll(np.roll(shelf0, ddz, 0), ddx, 1)
H = hy.copy()
H[(cls == 1) | (cls == 2)] = -999

# ---------- FireRed's outside maps, laid out as their connections join them ----------
L = json.load(open(os.path.join(FR, "data", "layouts", "layouts.json")))
lay = {l["id"]: l for l in L["layouts"] if "id" in l}
maps = {}
for n in os.listdir(os.path.join(FR, "data", "maps")):
    p = os.path.join(FR, "data", "maps", n, "map.json")
    if os.path.exists(p):
        m = json.load(open(p))
        maps[m["id"]] = (n, m)


def size(mid):
    l = lay[maps[mid][1]["layout"]]
    return l["width"], l["height"]


pos = {"MAP_PALLET_TOWN": (0, 0)}
q = deque(["MAP_PALLET_TOWN"])
while q:
    a = q.popleft()
    ax, ay = pos[a]
    aw, ah = size(a)
    for c in maps[a][1].get("connections") or []:
        b = c["map"]
        if b not in maps or b in pos: continue
        bw, bh = size(b)
        o, d = c["offset"], c["direction"]
        p = {"up": (ax + o, ay - bh), "down": (ax + o, ay + ah), "left": (ax - bw, ay + o), "right": (ax + aw, ay + o)}.get(d)
        if p is None: continue
        pos[b] = p
        q.append(b)

TS = {os.path.basename(os.path.dirname(f)).replace("_", ""): np.fromfile(f, dtype="<u4")
      for f in glob.glob(os.path.join(FR, "data", "tilesets", "*", "*", "metatile_attributes.bin"))}


def tileset(name):
    return TS.get(name.replace("gTileset_", "").lower(), np.zeros(1, np.uint32))


# Pallet Town's doors put FireRed's layout on his map here (block of step 0, 0); each map is then found near it.
AX, AZ = -814, 666
# (Where the doors agree better than the blocks: Fuchsia's next best fits its doors.)
PREFER = {"MAP_FUCHSIA_CITY": (4, 1)}

fit = {}
grids = {}
for mid in sorted(pos):
    n, m = maps[mid]
    l = lay[m["layout"]]
    w, h = l["width"], l["height"]
    a = np.fromfile(os.path.join(FR, l["blockdata_filepath"]), dtype="<u2").reshape(h, w)
    mt = (a & 0x3FF).astype(int)
    col, el = (a >> 10) & 3, a >> 12
    P, Q = tileset(l["primary_tileset"]), tileset(l["secondary_tileset"])
    att = np.where(mt < 640, P[np.clip(mt, 0, len(P) - 1)], Q[np.clip(mt - 640, 0, max(len(Q) - 1, 0))])
    beh = att & 0x1FF
    frgr = ((att >> 24) & 7) == 1
    js, je, jw = beh == 0x3B, beh == 0x38, beh == 0x39
    frw, frs, frg = (col == 0) & (el == 1), col != 0, (col == 0) & (el != 1)
    signs = [(b["x"], b["y"]) for b in m.get("bg_events", []) if b.get("type") == "sign"]
    px, py = pos[mid]
    bx, bz = AX + 2 * px, AZ + 2 * py
    cand = []
    for dz in range(-48, 49):
        for dx in range(-48, 49):
            ox, oz = bx + dx - X0, bz + dz - Z0
            if ox < 2 or oz < 2 or ox + 2 * w + 4 > N or oz + 2 * h + 4 > N: continue
            s = 0.0
            for sx, sz in ((0, 0), (1, 1)):
                W = cls[oz + sz:oz + sz + 2 * h:2, ox + sx:ox + sx + 2 * w:2]
                s += (np.where(W == 1, 1, -1) * frw).sum() + (np.where((W == 2) | (W == 4), 1, np.where(W == 1, -1, -0.5)) * frs).sum() \
                    + (np.where(W == 3, 1, np.where(W == 1, -1, np.where(W == 4, 0, -0.5))) * frg).sum()
            Pm, Gm = path[oz:oz + 2 * h:2, ox:ox + 2 * w:2], grassb[oz:oz + 2 * h:2, ox:ox + 2 * w:2]
            s += (np.where(Gm, 1, np.where(Pm, -1, 0)) * frgr).sum()
            h0 = H[oz:oz + 2 * h:2, ox:ox + 2 * w:2]
            if js.any():
                h1 = H[oz + 2:oz + 2 * h + 2:2, ox:ox + 2 * w:2]
                ok = (h0 > -999) & (h1 > -999)
                s += 3 * (np.where(ok & (h0 > h1), 1, np.where(ok, -1, 0)) * js).sum()
            if je.any() or jw.any():
                hE, hW = H[oz:oz + 2 * h:2, ox + 2:ox + 2 * w + 2:2], H[oz:oz + 2 * h:2, ox - 2:ox + 2 * w - 2:2]
                s += 3 * (np.where((h0 > -999) & (hE > -999) & (h0 > hE), 1, -1) * je).sum() + 3 * (np.where((h0 > -999) & (hW > -999) & (h0 > hW), 1, -1) * jw).sum()
            for sx_, sy_ in signs:
                s += 40 if shelf[oz + 2 * sy_:oz + 2 * sy_ + 2, ox + 2 * sx_:ox + 2 * sx_ + 2].any() else -10
            cand.append((s / (2 * w * h), dx, dz))
    cand.sort(reverse=True)
    peaks = []
    for c in cand:
        if all(abs(c[1] - p[1]) > 1 or abs(c[2] - p[2]) > 1 for p in peaks): peaks.append(c)
        if len(peaks) >= 4: break
    best = peaks[0]
    if mid in PREFER:
        best = next((c for c in cand if (c[1], c[2]) == PREFER[mid]), best)
    second = next(p for p in peaks if (p[1], p[2]) != (best[1], best[2]))
    sure = best[0] >= 0.55 and best[0] - second[0] >= 0.03
    fit[mid] = {"map": n, "ox": bx + best[1], "oz": bz + best[2], "w": w, "h": h, "score": round(float(best[0]), 3),
                "next": round(float(second[0]), 3), "sure": bool(sure)}
    grids[mid] = (frs, frw)
json.dump(fit, open(os.path.join(OUT, "fit.json"), "w"), indent=1)

# ---------- everyone and everything on them ----------
CAT = {}
for r in frp.table("placeables.tsv"):
    if len(r) >= 10: CAT[(r[1], r[2], int(r[6]), int(r[7]))] = r
PEOPLE_MOD = {r[0]: r for r in frp.table("people.tsv") if len(r) > 5}

GFX = {"OLD_MAN_1": "OLD MAN", "OLD_MAN_2": "OLD MAN", "OLD_MAN_LYING_DOWN": "OLD MAN", "WOMAN_1": "WOMAN", "WOMAN_2": "WOMAN", "WOMAN_3": "WOMAN",
       "MAN": "MAN", "FAT_MAN": "FAT MAN", "BALDING_MAN": "GENTLEMAN", "LITTLE_GIRL": "LITTLE GIRL", "LITTLE_BOY": "LITTLE BOY", "WORKER_M": "WORKER",
       "WORKER_F": "WORKER", "FISHER": "FISHERMAN", "SWIMMER_M_LAND": "SWIMMER", "SWIMMER_F_LAND": "SWIMMER", "SWIMMER_M_WATER": "SWIMMER",
       "SWIMMER_F_WATER": "SWIMMER", "COOLTRAINER_M": "COOLTRAINER", "COOLTRAINER_F": "COOLTRAINER", "ROCKET_M": "TEAM ROCKET", "ROCKET_F": "TEAM ROCKET",
       "SCIENTIST": "SCIENTIST", "POLICEMAN": "OFFICER", "CLERK": "CLERK", "NURSE": "NURSE", "BLUE": "RIVAL", "PROF_OAK": "PROF. OAK", "GIRL": "GIRL",
       "BOY": "BOY", "GBA_KID": "KID", "BEAUTY": "BEAUTY", "TEACHER": "TEACHER", "SAILOR": "SAILOR", "GENTLEMAN": "GENTLEMAN", "HIKER": "HIKER",
       "LASS": "LASS", "YOUNGSTER": "YOUNGSTER", "SUPER_NERD": "SUPER NERD", "BIKER": "BIKER", "CAMPER": "CAMPER", "PICNICKER": "PICNICKER",
       "BUG_CATCHER": "BUG CATCHER", "ROCKER": "ROCKER", "CRUSH_GIRL": "CRUSH GIRL", "BLACK_BELT": "BLACK BELT", "TUBER_M_WATER": "TUBER",
       "TUBER_F": "TUBER", "SNORLAX": "SNORLAX", "CUT_TREE": "CUT TREE", "ROCK_SMASH_ROCK": "ROCK", "PUSHABLE_BOULDER": "BOULDER", "ITEM_BALL": "ITEM",
       "CHANNELER": "CHANNELER", "GAMBLER": "GAMBLER", "BURGLAR": "BURGLAR", "MG_DELIVERYMAN": "DELIVERYMAN", "CHEF": "CHEF", "SEAGALLOP": "BOAT",
       "SS_ANNE": "S.S. ANNE", "TOWN_MAP": "SIGN", "DOG": "DOG", "MEOWTH": "MEOWTH", "CLEFAIRY": "CLEFAIRY", "PIDGEOT": "PIDGEOT", "MACHOP": "MACHOP",
       "POKE_BALL": "POKé BALL", "SPEAR_OW": "SPEAROW", "CUBONE": "CUBONE", "SLOWPOKE": "SLOWPOKE"}


def dirs_of(mv):
    """Which ways one looks, from FireRed's movement: one way, two, or all four."""
    mv = mv.replace("MOVEMENT_TYPE_", "")
    if mv in ("LOOK_AROUND", "WANDER_AROUND", "ROTATE_CLOCKWISE", "ROTATE_COUNTERCLOCKWISE", "WANDER_UP_AND_DOWN", "WANDER_LEFT_AND_RIGHT") \
            or mv.startswith("WALK_SEQUENCE") or mv.startswith("COPY_PLAYER"):
        if "UP_AND_DOWN" in mv: return ["up", "down"]
        if "LEFT_AND_RIGHT" in mv: return ["left", "right"]
        return ["up", "down", "left", "right"]
    if mv.startswith("FACE_"):
        out = []
        for d in ("UP", "DOWN", "LEFT", "RIGHT"):
            if re.search(r"(^|_)%s(_|$)" % d, mv[5:]): out.append(d.lower())
        return out or ["down"]
    if mv.startswith("WALK_UP_AND_DOWN"): return ["up", "down"]
    if mv.startswith("WALK_LEFT_AND_RIGHT"): return ["left", "right"]
    return ["down"]


def ground_y(X, Z):
    """The height to stand at on his map at a block corner (the middle of one of FireRed's steps): on its ground."""
    xs, zs = int(X) - X0, int(Z) - Z0
    ys = [hy[zz, xx] for zz in (zs - 1, zs) for xx in (xs - 1, xs) if cls[zz, xx] in (1, 3, 4)]
    return int(max(ys)) + 1 if ys else int(hy[zs, xs]) + 1


def first_text(script):
    for l in frp.SCRIPTS.get(script, []):
        mm = re.match(r"msgbox (\w+)", l)
        if mm:
            t = frp.say(mm.group(1))
            if t: return " ".join(t)
    return ""


things = []
for mid, f in fit.items():
    n, m = maps[mid]
    place = frp.MAPS.get(n) or re.sub(r"route(\d+)", r"route_\1", re.sub(r"(?<!^)([A-Z])", r"_\1", n).lower())
    frs, frw = grids[mid]
    for o in m.get("object_events", []):
        g = o.get("graphics_id", "").replace("OBJ_EVENT_GFX_", "")
        gx, gy = o.get("x", 0), o.get("y", 0)
        X, Z = f["ox"] + 2 * gx + 1, f["oz"] + 2 * gy + 1
        t = {"map": n, "place": place, "gx": gx, "gy": gy, "x": X, "z": Z, "y": ground_y(X, Z), "sure": f["sure"]}
        script = o.get("script", "")
        on = int(cls[int(Z) - Z0 - 1, int(X) - X0 - 1])
        t["on"] = {1: "water", 2: "tree", 3: "ground", 4: "built", 0: "?"}[on]
        if g == "ITEM_BALL":
            r = CAT.get((place, "item", gx, gy))
            it, _ = frp.item_of(script)
            t.update(kind="item", key=r[3] if r else None, label=(frp.our_item(it) or (it or "?")[5:].lower()).replace("_", " ").upper(), detail="")
        elif o.get("trainer_type", "TRAINER_TYPE_NONE") != "TRAINER_TYPE_NONE" or (place == "route_24" and script == "Route24_EventScript_Rocket"):
            r = CAT.get((place, "trainer", gx, gy))
            sight = int(o.get("trainer_sight_or_berry_tree_id", "0") or 0)
            t.update(kind="trainer", key=r[3] if r else None, label=r[4] if r else GFX.get(g, g), detail=r[5] if r else "", sight=sight,
                     dirs=dirs_of(o.get("movement_type", "")))
            if not r: t["note"] = "the other half of a pair (one battle)"
        elif g in ("CUT_TREE", "ROCK_SMASH_ROCK", "PUSHABLE_BOULDER"):
            t.update(kind="obstacle", key=None, label=GFX.get(g, g), detail="")
        else:
            t.update(kind="person", key=None, label=GFX.get(g, g.replace("_", " ")), detail=first_text(script), dirs=dirs_of(o.get("movement_type", "")))
        things.append(t)
    for b in m.get("bg_events", []):
        if b.get("type") != "hidden_item": continue
        gx, gy = b["x"], b["y"]
        X, Z = f["ox"] + 2 * gx + 1, f["oz"] + 2 * gy + 1
        r = CAT.get((place, "hidden", gx, gy))
        ours = frp.our_item(b.get("item", "")) or b.get("item", "")[5:].lower()
        things.append({"map": n, "place": place, "gx": gx, "gy": gy, "x": X, "z": Z, "y": ground_y(X, Z), "sure": f["sure"], "kind": "hidden",
                       "key": r[3] if r else None, "label": ours.replace("_", " ").upper(), "detail": "", "on": "ground"})
json.dump({"bounds": [KX0, KZ0, KX1, KZ1], "maps": {k: dict(v, place=(frp.MAPS.get(v["map"]) or v["map"])) for k, v in fit.items()},
           "things": things}, open(os.path.join(OUT, "data.json"), "w"))

# ---------- his Kanto from above, one pixel a block ----------
COL = [("water", (64, 132, 214)), ("birch_leaves", (74, 150, 70)), ("azalea_leaves", (52, 118, 56)), ("leaves", (60, 128, 60)),
       ("grass_block", (128, 196, 98)), ("dirt_path", (214, 186, 128)), ("moss", (150, 170, 80)), ("dripstone", (150, 112, 82)),
       ("mushroom_block", (196, 160, 120)), ("sand", (232, 216, 160)), ("mud_brick", (172, 140, 104)), ("cobblestone", (150, 150, 150)),
       ("shelf", (120, 84, 50)), ("fence", (190, 170, 130)), ("concrete", (180, 184, 196)), ("terracotta", (196, 120, 96)), ("glass", (180, 220, 236)),
       ("quartz", (236, 232, 224)), ("deepslate", (90, 92, 104)), ("andesite", (140, 144, 146)), ("stairs", (176, 140, 96)), ("slab", (176, 150, 112)),
       ("planks", (180, 140, 90)), ("tulip", (230, 110, 120)), ("poppy", (220, 60, 60)), ("flower", (240, 200, 90)), ("dandelion", (250, 220, 80)),
       ("clay", (160, 170, 180))]
sub = top[KZ0 - Z0:KZ1 - Z0, KX0 - X0:KX1 - X0]
img = np.zeros(sub.shape + (3,), np.float32)
fill = np.zeros(sub.shape, bool)
for k, c in COL:
    msk = np.vectorize(lambda t, k=k: k in t)(sub) & ~fill
    img[msk] = c
    fill |= msk
img[~fill] = (170, 160, 150)
hs = hy[KZ0 - Z0:KZ1 - Z0, KX0 - X0:KX1 - X0].astype(np.float32)
shade = np.clip(1.0 + (hs - np.roll(hs, 1, 0)) * 0.06 + (hs - np.roll(hs, 1, 1)) * 0.03, 0.75, 1.2)
d = dep[KZ0 - Z0:KZ1 - Z0, KX0 - X0:KX1 - X0]
wat = sub == "water"
shade[wat] = np.clip(1.05 - d[wat] * 0.06, 0.7, 1.05)
img = np.clip(img * shade[..., None], 0, 255).astype(np.uint8)
from PIL import Image  # noqa: E402
Image.fromarray(img).save(os.path.join(OUT, "kanto.png"), optimize=True)
from collections import Counter  # noqa: E402
print("maps", len(fit), "sure", sum(1 for v in fit.values() if v["sure"]), "things", Counter(t["kind"] for t in things),
      "on", Counter((t["kind"], t["on"]) for t in things if t["kind"] in ("trainer", "person", "item")))
