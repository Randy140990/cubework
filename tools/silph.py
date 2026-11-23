#!/usr/bin/env python3
"""
SILPH CO. 1F to 11F and its elevator, laid out from FireRed's own maps (pret/pokefirered: data/layouts, data/maps,
data/scripts/silphco_doors.inc), a FireRed tile a block, for the Interior Viewer (tools/interior/silph_data.js, built
by tools/interior/rooms56.js) and so the mod.

Each floor's tiles are sorted by their pictures (the tileset drawn from pret, tools/silph_render.py) and whether you can
walk on them: the dark between rooms, the walls, the floor, plants, crates, tables, desks, machines, the warp pads, the
stairs, the elevator's door, the way out. Its warps are FireRed's: the stairs between floors, the warp pads (in the
floor, or to another floor), the elevator. Its card-key doors are FireRed's (where its scripts close them). Its
trainers, items and people are FireRed's, matched to the mod's (trainers.tsv, field_items.tsv, people.tsv, written
from the first games) floor by floor, kind by kind; any FireRed has that the mod hasn't are added to people.tsv and
field_items.tsv with FireRed's words.

    python3 -I tools/silph.py
"""
import collections, json, os, re, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import silph_render as render  # noqa: E402

P = render.P
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
OUT_JS = os.path.join(HERE, "interior", "silph_data.js")
FLOORS = ["SilphCo_%dF" % i for i in range(1, 12)]
ROOM = {m: "silph_co_%df" % (i + 1) for i, m in enumerate(FLOORS)}
ROOM["SilphCo_Elevator"] = "silph_co_elevator"
MAP_OF = {("MAP_SILPH_CO_%dF" % (i + 1)): FLOORS[i] for i in range(11)}
MAP_OF["MAP_SILPH_CO_ELEVATOR"] = "SilphCo_Elevator"

STAIRS = {0x26a, 0x26b, 0x26f, 0x272, 0x273, 0x276, 0x277, 0x27a, 0x27b, 0x27e, 0x27f, 0x350, 0x351, 0x352, 0x353, 0x354, 0x358, 0x359,
          0x35a, 0x35b, 0x35c, 0x360, 0x361}
PAD, ELEVATOR_DOOR = 0x390, 0x2e2


def layouts():
    return {l["id"]: l for l in json.load(open(os.path.join(P, "data/layouts/layouts.json")))["layouts"] if "id" in l}


def classify(ids):
    """Each metatile's kind, from its picture and collision."""
    im = render.draw("SilphCo_1F", 1, None, (len(ids), 1, [i for i, _ in ids])).convert("RGB")
    out = {}
    pal = {"cream": (213, 213, 180), "purple": (180, 115, 164), "pink": (238, 180, 222), "grey": (123, 123, 123), "lgrey": (180, 180, 164)}
    for n, (mid, coll) in enumerate(ids):
        px = [im.getpixel((n * 16 + x, y)) for y in range(16) for x in range(16)]
        frac = lambda f: sum(1 for p in px if f(p)) / 256.0
        black = frac(lambda p: max(p) < 30)
        white = frac(lambda p: min(p) > 240)
        wallish = frac(lambda p: p in pal.values())
        green = frac(lambda p: p[1] > p[0] + 30 and p[1] > p[2] + 20)
        yellow = frac(lambda p: p[0] > 180 and p[1] > 160 and p[2] < 120 and p[0] - p[2] > 80)
        orange = frac(lambda p: p[0] > 200 and 120 < p[1] < 185 and p[2] < 110)
        blue = frac(lambda p: p[2] > 200 and p[0] < 200 and p[2] - p[0] > 40)
        if mid == PAD: k = "x"
        elif 0x3c6 <= mid <= 0x3df: k = "f"
        # The sofas (1F's waiting room, the boardroom's), before the counters' range, and the boardroom's table.
        elif mid in (0x362, 0x363, 0x36a, 0x36b) or 0x39d <= mid <= 0x39f: k = "s"
        elif mid in (0x3a5, 0x3a6, 0x3a7, 0x3a8, 0x3ad, 0x3ae, 0x3af, 0x3b5, 0x3b6, 0x3b7, 0x3bd, 0x3be, 0x3bf): k = "t"
        # The pictures on the walls, and the way out's mat (1F's, in front of the doors).
        elif mid in (0x365, 0x366, 0x367): k = "w"
        elif mid in (0x3aa, 0x3ab, 0x3ac): k = "."
        elif mid in (0x36d, 0x36e, 0x36f): k = "d"
        elif 0x368 <= mid <= 0x374: k = "k"
        elif mid in (0x362, 0x363, 0x36a, 0x36b): k = "s"
        elif mid == ELEVATOR_DOOR: k = "e"
        elif mid in STAIRS: k = "u"
        elif mid in (0x1a, 0x1b, 0x1c): k = "X"
        elif black > 0.85: k = "v"
        elif not coll: k = "."
        elif black > 0.3 and white + black + wallish > 0.75: k = "w"
        elif green > 0.12: k = "p"
        elif wallish > 0.42: k = "w"
        elif yellow > 0.3: k = "c"
        elif orange > 0.3: k = "t"
        elif blue > 0.25: k = "s"
        elif white > 0.35: k = "d"
        else: k = "m"
        out[mid] = k
    return out


def texts(mapname):
    """FireRed's words on a floor: each script's first message, by script name."""
    d = os.path.join(P, "data/maps", mapname)
    tx = {}
    if os.path.exists(os.path.join(d, "text.inc")):
        cur = None
        for line in open(os.path.join(d, "text.inc"), encoding="utf-8"):
            m = re.match(r"(\w+)::", line)
            if m: cur = m.group(1); tx[cur] = ""; continue
            m = re.search(r'\.string "(.*)"', line)
            if m and cur: tx[cur] += m.group(1)
    first = {}
    if os.path.exists(os.path.join(d, "scripts.inc")):
        cur = None
        for line in open(os.path.join(d, "scripts.inc"), encoding="utf-8"):
            m = re.match(r"(\w+)::", line)
            if m: cur = m.group(1); continue
            m = re.search(r"msgbox (\w+)", line) or re.search(r"trainerbattle_\w+ \w+, (\w+)", line)
            if m and cur and cur not in first: first[cur] = m.group(1)
    def clean(s):
        s = s.replace("\\p", " | ").replace("\\n", " ").replace("\\l", " ").replace("$", "")
        s = s.replace("{PLAYER}", "{player}").replace("{RIVAL}", "{rival}")
        return re.sub(r"\s+", " ", s).strip()
    return {k: clean(tx.get(v, "")) for k, v in first.items()}


def doors():
    """FireRed's card-key doors: (floor, the barrier's tiles x0, z0, x1, z1 (inclusive), which way it's crossed)."""
    out = collections.defaultdict(list)
    cur, tiles = None, []
    for line in open(os.path.join(P, "data/scripts/silphco_doors.inc"), encoding="utf-8"):
        m = re.match(r"EventScript_Close(\d+)FDoor\d*::", line)
        if m:
            cur, tiles = int(m.group(1)), []
            continue
        m = re.search(r"setmetatile (\d+), (\d+), METATILE_SilphCo_(Horizontal|Vertical)Barrier", line)
        if m and cur:
            tiles.append((int(m.group(1)), int(m.group(2)), m.group(3)))
        if line.strip() == "return" and cur and tiles:
            xs, zs = [t[0] for t in tiles], [t[1] for t in tiles]
            out["SilphCo_%dF" % cur].append({"x0": min(xs), "z0": min(zs), "x1": max(xs), "z1": max(zs), "way": "ns" if tiles[0][2] == "Horizontal" else "ew"})
            cur, tiles = None, []
    return out


def table(name):
    return [l.rstrip("\n").split("\t") for l in open(os.path.join(DATA, name), encoding="utf-8") if l.strip() and not l.startswith("#")]


GFX_TITLE = {"ROCKET_M": "ROCKET", "ROCKET_F": "ROCKET", "SCIENTIST": "SCIENTIST", "ROCKER": "JUGGLER", "GIOVANNI": "ROCKET BOSS GIOVANNI"}
GFX_SKIN = {"WORKER_M": "silph_worker_m", "WORKER_F": "silph_worker_f", "WOMAN_2": "lady", "OLD_MAN_2": "silph_president", "SCIENTIST": "scientist",
            "BLUE": "blue", "ROCKET_M": "rocket", "ROCKER": "rocker", "GIOVANNI": "leader_giovanni"}
# The mod's own people a FireRed sprite may be (the 1F receptionist, the 2F woman, the 11F secretary).
ALSO = {"WORKER_F": ["link_receptionist", "beauty"], "WOMAN_2": ["silph_worker_f"]}
ITEM_IDS = {r[0]: r[0] for r in table("items.tsv")}
# Words for the few people whose FireRed lines come from shared scripts.
FALLBACK = {"SilphCo_5F_EventScript_Scientist": "Those thugs that took over our building... | Their BOSS said he was looking for strong POKéMON. | I hope our PRESIDENT managed to avoid trouble...",
            "SilphCo_8F_EventScript_Scientist": "TEAM ROCKET's BOSS is terribly cruel! | To him, POKéMON are just tools to be used. | What will happen if that tyrant takes over our company..."}


def snake(s):
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", s).lower()


def main():
    L = layouts()
    trainers, people, items = table("trainers.tsv"), table("people.tsv"), table("field_items.tsv")
    have_people = {r[0] for r in people}
    have_items = {r[0] for r in items}
    # The tiles used, and whether each is ever walked through.
    used = {}
    grids = {}
    for m in FLOORS + ["SilphCo_Elevator"]:
        mp = json.load(open(os.path.join(P, "data/maps", m, "map.json"))); l = L[mp["layout"]]
        w, h = l["width"], l["height"]
        v = struct.unpack("<%dH" % (w * h), open(os.path.join(P, l["blockdata_filepath"]), "rb").read()[:w * h * 2])
        grids[m] = (mp, w, h, v)
        for b in v: used[b & 0x3FF] = used.get(b & 0x3FF, False) or bool((b >> 10) & 3)
    kinds = classify(sorted(used.items()))
    DOORS = doors()
    floors, new_people, new_items = {}, [], []
    for m, (mp, w, h, v) in grids.items():
        rid = ROOM[m]
        rows = []
        for z in range(h):
            rows.append("".join(kinds[v[z * w + x] & 0x3FF] for x in range(w)))
        rows = [list(r) for r in rows]
        for dr in DOORS.get(m, []):
            for z in range(dr["z0"], dr["z1"] + 1):
                for x in range(dr["x0"], dr["x1"] + 1):
                    rows[z][x] = "b"
        warps = []
        for i, wv in enumerate(mp["warp_events"]):
            x, z, dest = wv["x"], wv["y"], wv["dest_map"]
            t = v[z * w + x] & 0x3FF
            if dest == "MAP_SAFFRON_CITY": kind = "exit"
            elif dest == "MAP_SILPH_CO_ELEVATOR": kind = "elevator"
            elif dest == "MAP_DYNAMIC": kind = "back"
            elif t == PAD: kind = "pad"
            else: kind = "stairs"
            to = MAP_OF.get(dest)
            tx = tz = None
            if to and kind in ("pad", "stairs"):
                d = json.load(open(os.path.join(P, "data/maps", to, "map.json")))["warp_events"][int(wv["dest_warp_id"])]
                tx, tz = d["x"], d["y"]
            warps.append({"x": x, "z": z, "kind": kind, "to": ROOM.get(to) if to else None, "tx": tx, "tz": tz})
            if kind in ("stairs",): rows[z][x] = "u"
        # The people, trainers and items, FireRed's, matched to the mod's.
        T = TX = texts(m)
        tl = [r for r in trainers if r[1] == rid]
        pl = [r for r in people if r[1] == rid]
        il = [r for r in items if r[1] == rid]
        objs = []
        for o in mp["object_events"]:
            g = o["graphics_id"].replace("OBJ_EVENT_GFX_", "")
            sc = o.get("script") or ""
            x, z = o["x"], o["y"]
            if g == "ITEM_BALL":
                nm = snake(sc.split("EventScript_Item")[-1]) if "EventScript_Item" in sc else ""
                pick = next((r for r in il if r[2] == nm), None)
                if pick is None and nm.startswith("tm"): pick = next((r for r in il if r[2].startswith("tm_")), None)
                if pick: il.remove(pick); key = pick[0]
                elif nm in ITEM_IDS:
                    key = "%s_fr_%s" % (rid, nm)
                    if key not in have_items: new_items.append([key, rid, nm, "-1", "-1"])
                else: continue
                objs.append({"kind": "item", "key": key, "x": x, "z": z})
            elif o.get("trainer_type", "TRAINER_TYPE_NONE") != "TRAINER_TYPE_NONE" or g == "GIOVANNI":
                title = GFX_TITLE.get(g, "ROCKET")
                pick = next((r for r in tl if r[2] == title), None) or (tl[0] if tl else None)
                if not pick: continue
                tl.remove(pick)
                objs.append({"kind": "trainer", "key": pick[0], "name": pick[2].title(), "skin": pick[3], "x": x, "z": z,
                             "face": (o.get("movement_type") or ""), "dir": pick[4]})
            else:
                skin = GFX_SKIN.get(g, "silph_worker_m")
                pick = next((r for r in pl if r[2] in [skin] + ALSO.get(g, [])), None)
                if pick: pl.remove(pick); key, line = pick[0], pick[5]
                else:
                    key = "%s_fr_%s" % (rid, snake(sc.split("EventScript_")[-1]) or "p%d" % len(objs))
                    line = T.get(sc, "") or FALLBACK.get(sc, "TEAM ROCKET took over the building! We're stuck in here!")
                    if key not in have_people:
                        new_people.append([key, rid, skin, "down", "stay", line, "-1", "-1"])
                objs.append({"kind": "person", "key": key, "skin": skin, "x": x, "z": z, "lines": line.split(" | "), "face": o.get("movement_type") or "",
                             "name": {"WORKER_M": "Silph worker", "WORKER_F": "Silph worker", "OLD_MAN_2": "President", "BLUE": "{rival}",
                                      "WOMAN_2": "Woman", "SCIENTIST": "Scientist"}.get(g, "Silph worker"), "role": sc.split("EventScript_")[-1]})
        signs = []
        for b in mp.get("bg_events", []):
            sc = b.get("script") or ""
            if sc.endswith("FloorSign"): signs.append({"x": b["x"], "z": b["y"], "lines": [T.get(sc, "") or ("SILPH CO. HEAD OFFICE %s" % m.split("_")[1])]})
            elif sc.endswith("FloorSelect"): signs.append({"x": b["x"], "z": b["y"], "lines": ["The elevator's floor buttons."], "elevator": True})
            elif "Report" in sc and T.get(sc): signs.append({"x": b["x"], "z": b["y"], "lines": T[sc].split(" | ")})
        # FireRed's hidden items (one on most floors): found by looking at the spot.
        hidden = []
        for b in mp.get("bg_events", []):
            if b.get("type") == "hidden_item":
                it = b["item"].replace("ITEM_", "").lower()
                it = {"elixir": "elixer"}.get(it, it)
                if it not in ITEM_IDS: continue  # (ZINC: not in the first games)
                hidden.append({"x": b["x"], "z": b["y"], "item": it, "key": "%s_hidden_%s" % (rid, it)})
        floors[rid] = {"W": w, "D": h, "name": m.split("_")[1].replace("Elevator", "Elevator"), "rows": ["".join(r) for r in rows], "warps": warps,
                       "doors": DOORS.get(m, []), "objs": objs, "signs": signs, "hidden": hidden}
    js = "// Written by tools/silph.py from pret/pokefirered: SILPH CO.'s floors, a FireRed tile a block.\nconst SILPH = " + json.dumps(floors, separators=(",", ":")) + ";\n"
    open(OUT_JS, "w").write(js)
    if new_people:
        with open(os.path.join(DATA, "people.tsv"), "a", encoding="utf-8") as f:
            for r in new_people: f.write("\t".join(r) + "\n")
    if new_items:
        with open(os.path.join(DATA, "field_items.tsv"), "a", encoding="utf-8") as f:
            for r in new_items: f.write("\t".join(r) + "\n")
    print(len(floors), "rooms;", len(new_people), "people and", len(new_items), "items added")
    for rid, f in floors.items():
        print(rid, f["W"], f["D"], collections.Counter("".join(f["rows"])).most_common(), len(f["warps"]), "warps", len(f["doors"]), "doors", len(f["objs"]), "objs")


if __name__ == "__main__":
    main()
