#!/usr/bin/env python3
"""
Johto's insides John has passed over (16:15, 16:23), laid out from Crystal's own maps (pret/pokecrystal: maps/*.blk,
data/tilesets; read by tools/crystal_render.py), one of its steps (a 16 x 16 square) a block, for the Interior Viewer
(tools/interior/johto_data.js, built by tools/interior/rooms62.js) and so the mod:

  sprout_tower_1f..3f            VIOLET CITY's SPROUT TOWER: its wooden floors and log rails, the swaying pillar up
                                 the middle, the ladders between floors, the statues; the SAGEs (Crystal's teams), ELDER
                                 LI at the top with the painting behind him
  earls_pokemon_academy          EARL's POKéMON ACADEMY: the blackboard, the bookshelves, the rows of desks
  route_35_national_park_gate    the NATIONAL PARK's two gates, each its counter, its officer and its two ways
  route_36_national_park_gate

Each step is sorted by Crystal's collision for it (wall, floor, ladder, counter, the way out) and its block (the rails
from the dark walls, the pillar, the statues, the desks). Its warps, signs and people are Crystal's, with its words.

    python3 -I tools/johto_rooms.py
"""
import collections, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import crystal_render as C  # noqa: E402
import silph  # noqa: E402

DATA = silph.DATA
OUT_JS = os.path.join(HERE, "interior", "johto_data.js")

MAPS = [("SproutTower1F", "sprout_tower_1f", "Sprout Tower", "1F"), ("SproutTower2F", "sprout_tower_2f", "Sprout Tower", "2F"),
        ("SproutTower3F", "sprout_tower_3f", "Sprout Tower", "3F"), ("EarlsPokemonAcademy", "earls_pokemon_academy", "Pokémon Academy", ""),
        ("Route35NationalParkGate", "route_35_national_park_gate", "National Park Gate", "Route 35"),
        ("Route36NationalParkGate", "route_36_national_park_gate", "National Park Gate", "Route 36")]
ROOM = {"SPROUT_TOWER_1F": "sprout_tower_1f", "SPROUT_TOWER_2F": "sprout_tower_2f", "SPROUT_TOWER_3F": "sprout_tower_3f"}
FIRST = {m: i for i, (m, _, _, _) in enumerate(MAPS)}

# The SPROUT TOWER's blocks: the log rails (their posts, walls you can't cross but see over), the dark walls (2F's round
# the pillar, 3F's outer brick), the outside (the tower's dark sides).
RAILS = {0x04, 0x05, 0x06, 0x08, 0x0a, 0x0c, 0x0d, 0x0e}
DARK = {0x07, 0x0f, 0x0b, 0x2a}
PILLAR = {0x10, 0x11, 0x14, 0x15}
STATUE = {0x30, 0x31, 0x2e, 0x2f}

SPRITE_SKIN = {"SAGE": "sage", "GRANNY": "granny", "TEACHER": "teacher", "FISHER": "fishing_guru", "YOUNGSTER": "youngster", "GAMEBOY_KID": "gameboy_kid",
               "OFFICER": "officer_johto", "ROCKER": "rocker", "POKEFAN_M": "pokefan_m", "LASS": "lass", "RIVAL": "silver"}
NAME = {"SAGE": "Sage", "GRANNY": "Granny", "TEACHER": "Teacher", "FISHER": "Earl", "YOUNGSTER": "Youngster", "GAMEBOY_KID": "Boy", "OFFICER": "Officer"}
FACE = {"STANDING_DOWN": "s", "STANDING_UP": "n", "STANDING_LEFT": "w", "STANDING_RIGHT": "e", "SPINRANDOM_FAST": "s", "WANDER": "s", "STILL": "s"}
# Crystal's SAGEs (data/trainers/parties.asm), the money for one (the class's base, 8, times the top level), and who.
SAGES = {"CHOW": [(3, "bellsprout")] * 3, "NICO": [(3, "bellsprout")] * 3, "EDMOND": [(3, "bellsprout")] * 3, "JIN": [(6, "bellsprout")],
         "TROY": [(7, "bellsprout"), (7, "hoothoot")], "NEAL": [(6, "bellsprout")], "LI": [(7, "bellsprout"), (7, "bellsprout"), (10, "hoothoot")]}
# The people who are only there on contest days (Crystal's Bug-Catching Contest: Tuesdays, Thursdays and Saturdays)
# aren't put in: the gates' contest officers and the contestants waiting in Route 36's.
CONTEST = ("Contest", "BugCatchingContestant")


def classify(m, steps, signs, warps):
    H, W = len(steps), len(steps[0])
    rows = [["."] * W for _ in range(H)]
    fl = [["w"] * W for _ in range(H)]
    sign_at = {(s["x"], s["z"]) for s in signs}
    for z in range(H):
        for x in range(W):
            s = steps[z][x]
            c, b = s["c"], s["b"]
            if c == "FLOOR": k = "."
            elif c == "LADDER": k = "u"
            elif c == "COUNTER": k = "k"
            elif c.startswith("WARP") or c == "DOOR": k = "X"
            elif c == "BOOKSHELF": k = "h"
            elif c == "PC": k = "m"
            else: k = "w"
            if m.startswith("SproutTower") and k == "w":
                if b == 0x01: k = "v"
                elif b in RAILS: k = "r"
                elif b in DARK: k = "w"
                elif b in PILLAR: k = "P"
                elif b in STATUE: k = "S"
            if m == "EarlsPokemonAcademy" and k == "w":
                if b == 0x16: k = "k"      # the pupils' desks
                elif b == 0x15: k = "d"    # EARL's desk at the front
                elif b in (0x11, 0x12): k = "B"  # the blackboard
            if m.endswith("NationalParkGate") and k == "w" and b in (0x2c, 0x2d) and z == 0: k = "m"
            rows[z][x] = k
            fl[z][x] = "t" if m.startswith("SproutTower") else ("a" if m == "EarlsPokemonAcademy" else "g")
    # The SPROUT TOWER's statues and its painting, where Crystal's signs are (and the step above each statue's).
    if m.startswith("SproutTower"):
        for s in signs:
            if "Painting" in s["script"]: rows[s["z"]][s["x"]] = "A"
            elif "Statue" in s["script"]:
                rows[s["z"]][s["x"]] = "S"
                if s["z"] > 0 and rows[s["z"] - 1][s["x"]] in "wS": rows[s["z"] - 1][s["x"]] = "S"
    for w in warps:
        if rows[w["z"]][w["x"]] not in "uX": rows[w["z"]][w["x"]] = "X"
    return rows, fl


def main():
    trainers = silph.table("trainers.tsv")
    have_t = {r[0] for r in trainers}
    new_t, new_items = [], []
    items = silph.table("field_items.tsv")
    have_items = {r[0] for r in items}
    rooms = {}
    for m, rid, name, fname in MAPS:
        steps = C.steps(m)
        warps, signs, coords, objs = C.events(m)
        T = C.texts(m)
        rows, fl = classify(m, steps, signs, warps)
        H, W = len(rows), len(rows[0])
        ws = []
        for w in warps:
            to = ROOM.get(w["to"])
            wk = "stairs" if to else ("park" if w["to"] == "NATIONAL_PARK" else "exit")
            tx = tz = None
            if to:
                tw = C.events([mm for mm, r, _, _ in MAPS if r == to][0])[0][w["n"] - 1]
                tx, tz = tw["x"], tw["z"]
            # (A gate's two ways: by the side they're on.)
            side = "n" if w["z"] == 0 else "s" if w["z"] == H - 1 else "w" if w["x"] == 0 else "e" if w["x"] == W - 1 else None
            ws.append({"x": w["x"], "z": w["z"], "kind": wk, "to": to, "tx": tx, "tz": tz, "side": side})
        out = []
        for o in objs:
            if o["kind"] == "ITEMBALL":
                it = C.snake(o["script"].replace(m, "")).strip("_")
                it = {"parlyz_heal": "parlyz_heal", "x_accuracy": "x_accuracy", "potion": "potion", "escape_rope": "escape_rope"}.get(it, it)
                key = "%s_%s" % (rid, it)
                if key not in have_items: new_items.append([key, rid, it, "-1", "-1"]); have_items.add(key)
                out.append({"kind": "item", "key": key, "x": o["x"], "z": o["z"]}); continue
            if any(c in o["script"] for c in CONTEST): continue
            if o["sprite"] in ("RIVAL", "POKEDEX"):
                if o["sprite"] == "POKEDEX":
                    out.append({"kind": "notebook", "x": o["x"], "z": o["z"], "lines": T.get("AcademyNotebookText", ["It's this kid's notebook..."])})
                continue
            face = FACE.get(o["move"], "s")
            if o["kind"] == "TRAINER" or o["script"] == "SageLiScript":
                who = o["script"].replace("TrainerSage", "").replace("Sage", "").replace("Script", "").upper()
                key = "%s_sage_%s" % (rid, who.lower())
                seen = T.get("Sage%sSeenText" % who.title(), ["..."]) or ["..."]
                beat = T.get("Sage%sBeatenText" % who.title(), ["..."])
                after = T.get("Sage%sAfterBattleText" % who.title(), ["..."])
                party = SAGES[who]
                if key not in have_t:
                    new_t.append([key, rid, "SAGE " + who if who != "LI" else "ELDER LI", "sage", {"s": "down", "n": "up", "w": "left", "e": "right"}[face],
                                  str(max(o["sight"], 1) if who != "LI" else 0), str(8), ",".join("%s:%d" % (sp, lv) for lv, sp in party),
                                  "-", " | ".join(seen), " | ".join(beat), " | ".join(after), "-1", "-1"])
                    have_t.add(key)
                out.append({"kind": "trainer", "key": key, "name": ("Sage " + who.title()) if who != "LI" else "Elder Li", "skin": "sage", "x": o["x"], "z": o["z"],
                            "face": face, "lines": seen})
                continue
            sc = o["script"]
            lines = T.get(sc.replace("Script", "Text"), None) or T.get(sc + "Text", None)
            if not lines:
                # (a script that only shows a text: its label's words)
                body = open(os.path.join(C.P, "maps", m + ".asm")).read().split(sc + ":", 1)[-1].split("\n\n")[0]
                import re
                lab = re.findall(r"(?:jumptextfaceplayer|jumptext|writetext)\s+(\w+)", body)
                lines = T.get(lab[0], ["..."]) if lab else ["..."]
            skin = SPRITE_SKIN.get(o["sprite"], "youngster")
            key = "%s_%s" % (rid, C.snake(sc.replace(m, "").replace("Script", "")).strip("_") or "p%d" % len(out))
            out.append({"kind": "person", "key": key, "skin": skin, "x": o["x"], "z": o["z"], "face": face, "lines": lines,
                        "name": NAME.get(o["sprite"], skin.replace("_", " ").title()), "walk": o["move"].startswith("WANDER")})
        sg = []
        for s in signs:
            sc = s["script"]
            lab = sc + "Text"
            lines = T.get(lab)
            if not lines:
                body = open(os.path.join(C.P, "maps", m + ".asm")).read()
                import re
                mm = re.search(sc + r":\s*\n\s*jumptext (\w+)", body)
                lines = T.get(mm.group(1)) if mm else None
            if not lines:
                lines = {"BugCatchingContestExplanationSign": ["The BUG-CATCHING CONTEST is held on Tuesday, Thursday and Saturday.",
                                                               "Not only do you earn a prize just for participating, you also get to keep the bug POKéMON you may have at the end of the contest."],
                         "AcademyBookshelf": ["The bookshelves are full of books on POKéMON."], "AcademyBlackboard": ["The blackboard describes POKéMON status changes during battle."]}.get(sc, ["..."])
            sg.append({"x": s["x"], "z": s["z"], "lines": lines, "kind": sc})
        rooms[rid] = {"W": W, "D": H, "name": name, "floor": fname, "map": m, "rows": ["".join(r) for r in rows], "fl": ["".join(r) for r in fl],
                      "warps": ws, "objs": out, "signs": sg}
    js = "// Written by tools/johto_rooms.py from pret/pokecrystal: the SPROUT TOWER, EARL's ACADEMY and the NATIONAL PARK's gates, a step a block.\nconst JOHTO = " + \
         json.dumps(rooms, separators=(",", ":")) + ";\n"
    open(OUT_JS, "w").write(js)
    for name, rows in (("trainers.tsv", new_t), ("field_items.tsv", new_items)):
        if rows:
            with open(os.path.join(DATA, name), "a", encoding="utf-8") as f:
                for r in rows: f.write("\t".join(r) + "\n")
    print(len(rooms), "rooms;", len(new_t), "trainers and", len(new_items), "items added")
    for rid, f in rooms.items():
        print(rid, f["W"], f["D"], collections.Counter("".join(f["rows"])).most_common(), [(w["kind"], w["x"], w["z"], w["to"], w["side"]) for w in f["warps"]],
              [(o["kind"], o.get("key"), o.get("lines", [""])[0][:40]) for o in f["objs"]], [(s["kind"], s["lines"][0][:30]) for s in f["signs"]])


if __name__ == "__main__":
    main()
