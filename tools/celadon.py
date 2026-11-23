#!/usr/bin/env python3
"""
CELADON CITY's GAME CORNER (and its PRIZE ROOM) and the CELADON DEPT. STORE (1F to 5F, its ROOFTOP SQUARE and its
elevator), laid out from FireRed's own maps (pret/pokefirered: data/layouts, data/maps), a FireRed tile a block, for
the Interior Viewer (tools/interior/celadon_data.js, built by tools/interior/rooms58.js) and so the mod.

Each floor's tiles are sorted by FireRed's tile (its picture, and whether you can walk on it): the walls, the floor
(and which floor: the DEPT. STORE's lilac hall, its orange tiles, the yellow carpet round its counter, the GAME
CORNER's gold carpet, the roof's tiles and rug), the counters, plants, sofas, shelves of goods, glass cases, the TV
GAME SHOP's counters, the slot machines and their stools, the prize shelves, the vending machines, the roof's fence,
the stairs, the elevator's door, the way out. Its warps, signs and people are FireRed's: the people matched to the
mod's (people.tsv, written from the first games) room by room, by what they say; any FireRed has that the mod hasn't
are added to people.tsv with FireRed's words. The DEPT. STORE's clerks sell the first games' stock (marts.tsv).

    python3 -I tools/celadon.py
"""
import collections, json, os, re, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import silph  # noqa: E402
import silph_render as render  # noqa: E402

P = render.P
DATA = silph.DATA
OUT_JS = os.path.join(HERE, "interior", "celadon_data.js")

DS = "CeladonCity_DepartmentStore_"
MAPS = [("CeladonCity_GameCorner", "game_corner", "GAME CORNER"), ("CeladonCity_GameCorner_PrizeRoom", "game_corner_prize_room", "PRIZE ROOM"),
        (DS + "1F", "celadon_mart_1f", "1F"), (DS + "2F", "celadon_mart_2f", "2F"), (DS + "3F", "celadon_mart_3f", "3F"), (DS + "4F", "celadon_mart_4f", "4F"),
        (DS + "5F", "celadon_mart_5f", "5F"), (DS + "Roof", "celadon_mart_roof", "ROOF"), (DS + "Elevator", "celadon_mart_elevator", "Elevator")]
ROOM = {m: r for m, r, _ in MAPS}
FLOOR = {m: f for m, _, f in MAPS}
MAP_OF = {"MAP_CELADON_CITY_GAME_CORNER": "CeladonCity_GameCorner", "MAP_CELADON_CITY_GAME_CORNER_PRIZE_ROOM": "CeladonCity_GameCorner_PrizeRoom",
          "MAP_CELADON_CITY_DEPARTMENT_STORE_ELEVATOR": DS + "Elevator", "MAP_CELADON_CITY_DEPARTMENT_STORE_ROOF": DS + "Roof"}
for f in ("1F", "2F", "3F", "4F", "5F"): MAP_OF["MAP_CELADON_CITY_DEPARTMENT_STORE_" + f] = DS + f


def ids(*xs):
    out = set()
    for x in xs:
        if isinstance(x, tuple): out.update(range(x[0], x[1] + 1))
        else: out.add(x)
    return out


# ----- The GAME CORNER's tiles (gTileset_GameCorner) -----
GC = {}
for k, s in {
    "v": ids(0x8), "X": ids(0x1a, 0x1b, 0x1c),
    "w": ids((0x281, 0x287), 0x289, (0x28a, 0x28e), 0x2b7, 0x2bf),
    "p": ids(0x2ce, 0x2d6),
    # The counter (its green and white rim, the glass case in its middle).
    "k": ids(0x297, 0x29b, (0x2a0, 0x2a4), 0x28f, 0x2bc, 0x2bd, 0x2be),
    # The violet partition at the back by the hidden stairs, its marquee of bulbs (the outside's roof).
    "P": ids(0x2a5, 0x2a6, 0x2a7, 0x2ad, 0x2ae, 0x2af, 0x2cf, 0x2d7),
    "S": ids(0x2b8, 0x2b9, 0x2c0, 0x2c1),
    "o": ids(0x2aa, 0x2ab, 0x2b2, 0x2b3, 0x2ba, 0x2bb),
    "q": ids(0x2cc, 0x2cd),
    "m": ids(0x2df),
}.items():
    for i in s: GC[i] = k

# ----- The DEPT. STORE's tiles (gTileset_DepartmentStore) -----
DSK = {}
for k, s in {
    "v": ids(0x8, 0x288, (0x2f4, 0x2f6), (0x2fc, 0x2fe), 0x343, 0x33b, 0x357),
    "X": ids(0x2b2, 0x2b3, 0x2b4),
    "e": ids(0x28d),
    "u": ids(0x360, 0x361, 0x368, 0x369, 0x370, 0x371, 0x36c, 0x36d, 0x374, 0x375, 0x299, 0x29a, 0x2a1, 0x2a2, 0x29d, 0x29e, 0x2a5, 0x297, 0x29b, 0x29c,
             0x34d, 0x355),
    "k": ids(0x2d3, 0x2d4, 0x2d5, 0x2d6, 0x2db, 0x2dd, 0x2e3, 0x2f3, 0x2eb, 0x2fb, 0x2ec, 0x31c, 0x324, 0x2e6, 0x2ee, 0x2e2),
    "m": ids(0x2ef, 0x2f7, 0x2d7, 0x2df),
    "s": ids(0x316, 0x31e, 0x326, 0x312, 0x31a, 0x322),
    "p": ids(0x2c6, 0x2c7, 0x323),
    "h": ids(0x320, 0x321, 0x328, 0x329, 0x330, 0x331, 0x31f, 0x327, 0x306, 0x307, 0x30d, 0x30e, 0x30f, 0x2e0),
    "g": ids(0x300, 0x301, 0x302),
    "n": ids(0x313, 0x314),
    # The ROOFTOP SQUARE: its fence, the stairs' little house, the vending machines, the bin, the table.
    "F": ids(0x338, 0x339, 0x340, 0x341, 0x348, 0x350, 0x358, 0x35a, 0x352, 0x359, 0x351, 0x35d),
    "w": ids(0x32e, 0x32f, 0x34f, 0x336, 0x337, 0x33f, 0x33d, 0x33c, 0x33e, 0x345, 0x344, 0x346, 0x347, 0x349, 0x34a),
    "V": ids(0x32d, 0x325),
    "b": ids(0x363),
    "d": ids(0x34b, 0x34c, 0x353, 0x354),
    "o": ids(0x334, 0x335),
}.items():
    for i in s: DSK[i] = k
# Which floor: the hall's lilac carpet, the yellow carpet round the counters, the roof's tiles and its lilac rug; else
# the shop floor's orange tiles.
DSF = {}
for k, s in {"l": ids(0x281, 0x289, 0x28a, (0x290, 0x296), 0x36a, 0x36b, 0x35e, 0x35f, 0x372, 0x373, 0x2a3, 0x2a4),
             "y": ids((0x2c8, 0x2ce), (0x2d0, 0x2d2), 0x2d8, 0x2d9, 0x2da, 0x2dc, 0x2e4, 0x2e5),
             "R": ids(0x2b8, 0x2b9, 0x2bb, 0x342, 0x33a, 0x34d, 0x355, 0x34e, 0x356),
             "r": ids(0x32a, 0x32b, 0x32c, 0x332, 0x333, 0x35b, 0x35c, 0x334, 0x335, 0x34b, 0x34c, 0x353, 0x354)}.items():
    for i in s: DSF[i] = k


def grid(m):
    L = silph.layouts()
    mp = json.load(open(os.path.join(P, "data/maps", m, "map.json"))); l = L[mp["layout"]]
    w, h = l["width"], l["height"]
    v = list(struct.unpack("<%dH" % (w * h), open(os.path.join(P, l["blockdata_filepath"]), "rb").read()[:w * h * 2]))
    return mp, l, w, h, v


def all_texts(m):
    """FireRed's words on a map, by text label."""
    d = os.path.join(P, "data/maps", m)
    tx, cur = {}, None
    if os.path.exists(os.path.join(d, "text.inc")):
        for line in open(os.path.join(d, "text.inc"), encoding="utf-8"):
            mm = re.match(r"(\w+)::", line)
            if mm: cur = mm.group(1); tx[cur] = ""; continue
            mm = re.search(r'\.string "(.*)"', line)
            if mm and cur: tx[cur] += mm.group(1)
    return {k: clean(v) for k, v in tx.items()}


def clean(s):
    s = s.replace("\\p", " | ").replace("\\n", " ").replace("\\l", " ").replace("$", "").replace("…", "...")
    s = re.sub(r"\{FONT_\w+\}|\{COLOR \w+\}|\{SHADOW \w+\}", "", s)
    s = s.replace("{PLAYER}", "{player}").replace("{RIVAL}", "{rival}")
    return re.sub(r"\s+", " ", s).strip().replace(" | |", " |")


def script_texts(m):
    """Each script's words: its messages in order (following a goto or call to the map's own scripts once)."""
    d = os.path.join(P, "data/maps", m)
    TX = all_texts(m)
    body, cur = collections.defaultdict(list), None
    if os.path.exists(os.path.join(d, "scripts.inc")):
        for line in open(os.path.join(d, "scripts.inc"), encoding="utf-8"):
            mm = re.match(r"(\w+)::", line)
            if mm: cur = mm.group(1); continue
            if cur: body[cur].append(line.strip())
    out = {}
    for name, lines in body.items():
        words = []
        for ln in lines:
            mm = re.search(r"(?:msgbox|message) (\w+)", ln) or re.search(r"trainerbattle_\w+ \w+, (\w+)", ln)
            if mm and mm.group(1) in TX: words.append(TX[mm.group(1)])
        out[name] = words
    return out, TX


def words(s):
    return set(re.findall(r"[a-z]{3,}", s.lower().replace("é", "e")))


def best(line, rows):
    """The mod's person (row) whose words are nearest FireRed's line."""
    w = words(line)
    scored = sorted(((len(w & words(r[5])) / max(1, len(w | words(r[5]))), r) for r in rows), key=lambda t: -t[0])
    return scored[0][1] if scored and scored[0][0] >= 0.18 else None


GFX_SKIN = {"WORKER_F": "link_receptionist", "WORKER_M": "clerk", "CLERK": "clerk", "BALDING_MAN": "balding_guy", "WOMAN_1": "lady", "WOMAN_2": "beauty",
            "FISHER": "fishing_guru", "GYM_GUY": "gym_guide", "OLD_MAN_2": "gramps", "SCIENTIST": "scientist", "GENTLEMAN": "gentleman",
            "ROCKET_M": "rocket", "LASS": "girl", "GBA_KID": "gameboy_kid", "LITTLE_GIRL": "little_girl", "MAN": "super_nerd", "YOUNGSTER": "youngster",
            "SAILOR": "sailor", "COOLTRAINER_M": "super_nerd"}
NAME = {"CLERK": "Clerk", "WORKER_M": "Clerk", "WORKER_F": "Receptionist"}
# The DEPT. STORE's clerks: which counter of the first games' each is (marts.tsv).
CLERK_MART = {"CeladonCity_DepartmentStore_2F_EventScript_ClerkItems": "celadon_city_2f_items",
              "CeladonCity_DepartmentStore_2F_EventScript_ClerkTMs": "celadon_city_2f_tms",
              "CeladonCity_DepartmentStore_4F_EventScript_Clerk": "celadon_city_4f",
              "CeladonCity_DepartmentStore_5F_EventScript_ClerkXItems": "celadon_city_5f_x",
              "CeladonCity_DepartmentStore_5F_EventScript_ClerkVitamins": "celadon_city_5f_vitamins"}
SLOT = ["A slot machine!", "Want to play?", "A COIN CASE is required..."]


def main():
    people, trainers = silph.table("people.tsv"), silph.table("trainers.tsv")
    have_people = {r[0] for r in people}
    rooms, new_people = {}, []
    elev_kinds = None
    for m, rid, fname in MAPS:
        mp, l, w, h, v = grid(m)
        sec = l["secondary_tileset"]
        if m == "CeladonCity_GameCorner":
            # The way down to the ROCKET HIDEOUT stays behind the wall till its poster's switch (FireRed's).
            for (x, z), (t, c) in {(15, 2): (0x292, 0), (16, 2): (0x292, 0), (17, 2): (0x292, 0), (16, 3): (0x2cf, 1), (17, 3): (0x2cf, 1)}.items():
                v[z * w + x] = t | (c << 10)
        if sec == "gTileset_SilphCo":
            if elev_kinds is None:
                elev_kinds = silph.classify(sorted({(b & 0x3FF, bool((b >> 10) & 3)) for b in v}))
            kind = lambda b: elev_kinds[b & 0x3FF]
            flr = lambda b: "e"
        else:
            K = GC if sec == "gTileset_GameCorner" else DSK
            kind = lambda b, K=K: K.get(b & 0x3FF, "w" if (b >> 10) & 3 else ".")
            flr = (lambda b: "g") if sec == "gTileset_GameCorner" else (lambda b: DSF.get(b & 0x3FF, "o"))
        rows = [[kind(v[z * w + x]) for x in range(w)] for z in range(h)]
        fl = ["".join(flr(v[z * w + x]) for x in range(w)) for z in range(h)]
        # The TV GAME SHOP's counters: the counter cells beside its TVs and consoles are theirs.
        for z in range(h):
            for x in range(w):
                if rows[z][x] == "n":
                    for dx in (-1, 1):
                        xx = x + dx
                        while 0 <= xx < w and rows[z][xx] == "k": rows[z][xx] = "n"; xx += dx
        warps = []
        for wv in mp["warp_events"]:
            x, z, dest = wv["x"], wv["y"], wv["dest_map"]
            if dest == "MAP_CELADON_CITY": wk = "exit"
            elif dest == "MAP_CELADON_CITY_DEPARTMENT_STORE_ELEVATOR": wk = "elevator"
            elif dest == "MAP_DYNAMIC": wk = "back"
            elif dest.startswith("MAP_ROCKET_HIDEOUT"): continue  # (the hideout: not built yet)
            else: wk = "stairs"
            to = MAP_OF.get(dest)
            tx = tz = None
            if to and wk == "stairs":
                d = json.load(open(os.path.join(P, "data/maps", to, "map.json")))["warp_events"][int(wv["dest_warp_id"])]
                tx, tz = d["x"], d["y"]
            warps.append({"x": x, "z": z, "kind": wk, "to": ROOM.get(to) if to else None, "tx": tx, "tz": tz})
            if wk == "stairs": rows[z][x] = "u"
        ST, TX = script_texts(m)
        pl = [r for r in people if r[1] == rid]
        tl = [r for r in trainers if r[1] == rid]
        objs = []
        for o in mp["object_events"]:
            g = o["graphics_id"].replace("OBJ_EVENT_GFX_", "")
            sc = o.get("script") or ""
            x, z = o["x"], o["y"]
            face = (o.get("movement_type") or "").replace("MOVEMENT_TYPE_", "")
            fr = " | ".join(ST.get(sc, [])[:1]) if ST.get(sc) else ""
            if o.get("trainer_type", "TRAINER_TYPE_NONE") != "TRAINER_TYPE_NONE":
                if not tl: continue
                pick = tl.pop(0)
                objs.append({"kind": "trainer", "key": pick[0], "name": pick[2].title(), "skin": pick[3], "x": x, "z": z, "face": face, "dir": pick[4]})
                continue
            if sc in CLERK_MART:
                objs.append({"kind": "clerk", "key": CLERK_MART[sc], "skin": "clerk", "x": x, "z": z, "face": face, "name": "Clerk"})
                continue
            skin = GFX_SKIN.get(g, "youngster")
            if "PrizeClerk" in sc:
                fr = "We exchange your COINS for prizes. | " + TX.get("CeladonCity_GameCorner_PrizeRoom_Text_CoinCaseRequired", "A COIN CASE is required...")
            if sc.endswith("CounterTutor"):
                fr = " | ".join(TX[k] for k in ("Text_CounterTeach",) if k in TX)
            # FireRed's person as the first games' one in the room: the one who says the same, else one who looks
            # the same, else the next of them (the clerks and the tutor are FireRed's own).
            own = "PrizeClerk" in sc or sc.endswith("CounterTutor")
            pick = None if own else (best(fr, pl) if fr else None) or next((r for r in pl if r[2] == skin), None) or (pl[0] if pl else None)
            if pick is not None:
                pl.remove(pick)
                key, line, skin = pick[0], pick[5], pick[2]
            else:
                key = "%s_fr_%s" % (rid, silph.snake(sc.split("EventScript_")[-1]) or "p%d" % len(objs))
                line = fr or "..."
                if key not in have_people: new_people.append([key, rid, skin, "down", "stay", line, "-1", "-1"])
            objs.append({"kind": "person", "key": key, "skin": skin, "x": x, "z": z, "lines": line.split(" | "), "face": face,
                         "name": NAME.get(g, skin.replace("_", " ").title()), "role": sc.split("EventScript_")[-1]})
        signs = []
        for b in mp.get("bg_events", []):
            sc = b.get("script") or ""
            if b.get("type") == "hidden_item" or not sc: continue
            name = sc.split("EventScript_")[-1]
            lines = ST.get(sc) or []
            if name.startswith("SlotMachine"): lines = SLOT
            elif name.startswith("UnusableSlotMachine"): lines = ST.get(sc) or ["OUT OF ORDER", "This is broken."]
            elif name == "FloorSelect": signs.append({"x": b["x"], "z": b["y"], "lines": ["The elevator's floor buttons."], "elevator": True}); continue
            elif name == "VendingMachine":
                signs.append({"x": b["x"], "z": b["y"], "lines": [TX.get("CeladonCity_DepartmentStore_Roof_Text_VendingMachineWhatDoesItHave", "A vending machine!")],
                              "use": "vending"}); continue
            elif name == "PhotoPrinter": lines = ["It's a photo printer. Its tray is empty."]
            elif name == "Poster" and m == "CeladonCity_GameCorner":
                lines = [l for l in (ST.get(sc) or []) if "switch" in l.lower()] or ["Hey! | A switch behind the poster!? | Let's push it!"]
            if not lines: continue
            lines = [s for l in lines for s in l.split(" | ")]
            signs.append({"x": b["x"], "z": b["y"], "lines": lines, "kind": name})
        rooms[rid] = {"W": w, "D": h, "name": "Game Corner" if rid.startswith("game_corner") else "Celadon Dept. Store", "floor": fname, "map": m,
                      "rows": ["".join(r) for r in rows], "fl": fl, "ids": [[v[z * w + x] & 0x3FF for x in range(w)] for z in range(h)],
                      "warps": warps, "objs": objs, "signs": signs}
    js = "// Written by tools/celadon.py from pret/pokefirered: the GAME CORNER and the DEPT. STORE, a FireRed tile a block.\nconst CELADON = " + \
         json.dumps(rooms, separators=(",", ":")) + ";\n"
    open(OUT_JS, "w").write(js)
    if new_people:
        with open(os.path.join(DATA, "people.tsv"), "a", encoding="utf-8") as f:
            for r in new_people: f.write("\t".join(r) + "\n")
    print(len(rooms), "rooms;", len(new_people), "people added")
    for rid, f in rooms.items():
        print(rid, f["W"], f["D"], collections.Counter("".join(f["rows"])).most_common(), len(f["warps"]), "warps", [(o["kind"], o["key"]) for o in f["objs"]],
              len(f["signs"]), "signs")


if __name__ == "__main__":
    main()
