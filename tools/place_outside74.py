#!/usr/bin/env python3
"""
(0.74) John's 28 Nov 10:28: "based on your best guess, you placed everybody in the world ... You place everybody in
all the items, and then the idea is we can then review to ensure that the placements are correct." Everyone and
everything FireRed has outside (its trainers, its people, its item balls and hidden items) put down at the best guess
there is of their spot on John's map, as BAKED rows of placed.tsv (the PLACER and the Kanto Placement Map still move
any of them; John's own go in config/retromon_placed.tsv and win).

Where each goes, the first there is of:
  - a drag of John's on the Kanto Placement Map (its "moves", saved in tools/placemap/moves.json);
  - a spot the game had for them already (Game.java's SPOTS and TRAINER_SPOTS: John's, or fitted by hand before);
  - the page's spot for them (FireRed's spot on his map, from the fit of FireRed's map over his terrain), or where
    John has them in his own game where the page has that;
  - for the mod's people (Red and Blue's catalogue, people.tsv): the FireRed person they are (the same words, or for a
    POKéMON the same one; else the same kind of person nearest where Red and Blue had them), at that one's spot; one
    FireRed hasn't, where Red and Blue had them on the map scaled to FireRed's;
  - FireRed's people the mod hadn't (with words of their own, not the story's own scenes): added to people.tsv with
    FireRed's words, at their spot.
Heights aren't baked: the game finds the ground there as it loads (the street, not a roof; off a tree), and the sea
for a SWIMMER, so a row still holds when his map changes under it.

    python3 tools/place_outside74.py        (runs tools/fr_placeables.py in the middle; then the checklist's rows)
"""
import difflib
import json
import math
import os
import re
import subprocess
import sys
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DATA = os.path.join(ROOT, "src", "main", "resources", "retromon", "data")
JAVA = open(os.path.join(ROOT, "src", "main", "java", "com", "insidethecube", "retromon", "game", "Game.java"), encoding="utf-8").read()


def table(name):
    out = []
    for l in open(os.path.join(DATA, name), encoding="utf-8"):
        if l.startswith("#") or not l.strip(): continue
        out.append(l.rstrip("\n").split("\t"))
    return out


def outdoors(pl):
    return pl.endswith("_city") or pl.endswith("_town") or pl.endswith("_island") or (pl.startswith("route_") and "gate" not in pl and "house" not in pl)


OUTSIDE_NOW = {"pallet_town", "route_1", "viridian_city"}
SKIP = {"pallet_town_p0", "cerulean_city_p0", "cerulean_city_p8", "route_2_i1"}
PAGE = json.load(open(os.path.join(HERE, "placemap", "placement.json")))
MAPS = {m["id"]: m for m in PAGE["maps"]}
PLACE_MAPS = defaultdict(list)
for m in PAGE["maps"]: PLACE_MAPS[m["place"]].append(m)
MOVES_FILE = os.path.join(HERE, "placemap", "moves.json")
MOVES = {}
if os.path.exists(MOVES_FILE):
    for d in json.load(open(MOVES_FILE)):
        if d.get("key"): MOVES[d["key"]] = (float(d["x"]), float(d["z"]))
SPECIES = {r[2].upper(): r[1] for r in table("species.tsv") if len(r) > 2}
NAME_OF = {v: k for k, v in SPECIES.items()}
SIZES = {r[0]: (int(r[1]), int(r[2])) for r in table("sizes.tsv") if len(r) > 2}


def spots(name, cols):
    body = JAVA[JAVA.index("static final Object[][] %s = {" % name):]
    body = body[:body.index("};")]
    out = {}
    for m in re.finditer(r'\{"([a-z0-9_]+)",\s*' + r",\s*".join([r"(-?[\d.]+)"] * cols) + r"\}", body):
        v = [float(x) for x in m.groups()[1:]]
        out[m.group(1)] = (v[0], v[-1])
    return out


SPOTS = spots("SPOTS", 2)
TRAINER_SPOTS = spots("TRAINER_SPOTS", 2)
ITEM_SPOTS = set(spots("ITEM_SPOTS", 3))

# ---------- the mod's people against FireRed's ----------
# (FireRed's that are the story's own scenes, or things: not people to stand about.)
EVENT = {"RIVAL", "PROF. OAK", "BILL", "BOAT", "SNORLAX"}
SPRITE = {"BOY": "boy", "LITTLE BOY": "little_boy", "LITTLE GIRL": "little_girl", "GIRL": "girl", "WOMAN": "middle_aged_woman", "MAN": "middle_aged_man",
          "OLD MAN": "gramps", "OLD WOMAN": "old_woman", "FAT MAN": "fishing_guru", "SCIENTIST": "scientist", "BUG CATCHER": "bug_catcher", "LASS": "lass",
          "CAMPER": "camper", "PICNICKER": "picnicker", "BEAUTY": "beauty", "CRUSH GIRL": "crush_girl", "YOUNGSTER": "youngster", "GENTLEMAN": "gentleman",
          "WORKER": "silph_worker_m", "OFFICER": "guard", "BLACK BELT": "black_belt", "COOLTRAINER": "cooltrainer_m", "TEAM ROCKET": "rocket",
          "SAILOR": "sailor", "FISHERMAN": "fisher", "HIKER": "hiker", "SUPER NERD": "super_nerd", "GAMBLER": "gambler", "NURSE": "nurse"}
MARK = "# (0.74) FireRed's own people outside the mod hadn't (tools/place_outside74.py): its words, at its spots"
_ppl = open(os.path.join(DATA, "people.tsv"), encoding="utf-8").read()
PEOPLE = {r[0]: r for r in (l.split("\t") for l in _ppl[:_ppl.index(MARK)].splitlines() if MARK in _ppl) if len(r) > 2 and not r[0].startswith("#")} \
    if MARK in _ppl else {r[0]: r for r in table("people.tsv")}


def species_of(r):
    m = re.match(r"^([A-Z][A-Z0-9 .'♀♂-]*):", r[5] if len(r) > 5 else "")
    if m and m.group(1).strip() in SPECIES: return SPECIES[m.group(1).strip()]
    return r[2] if r[2] in NAME_OF else None


def norm(s):
    return re.sub(r"\s+", " ", re.sub(r"[^a-z ]", " ", s.lower().replace("é", "e"))).strip()


page_keyed = {t["key"]: t for t in PAGE["things"] if t.get("key")}
fr_people = defaultdict(list)
for i, t in enumerate(PAGE["things"]):
    if t["k"] != "person" or t.get("key") or t["l"] in EVENT: continue
    fr_people[MAPS[t["m"]]["place"]].append(i)
mod_people = defaultdict(list)
for k, r in PEOPLE.items():
    if len(r) < 6 or not outdoors(r[1]) or r[1] in OUTSIDE_NOW or k in SKIP or k in page_keyed: continue
    if r[2] == "monster" and not species_of(r): continue
    mod_people[r[1]].append(k)

MATCH, FR_TAKEN = {}, set()
# (FireRed's that the page tied to the wrong one of the mod's: Route 4's two BLACK BELTs, both at the game's spots.)
MINE_ALREADY = {("route_4", 50, 3)}
for place, frs in fr_people.items():
    for i in frs:
        t = PAGE["things"][i]
        if (place, t["gx"], t["gy"]) in MINE_ALREADY: FR_TAKEN.add(i)
for place in sorted(set(fr_people) | set(mod_people)):
    frs, mods = fr_people.get(place, []), mod_people.get(place, [])
    pairs = []
    for k in mods:
        r = PEOPLE[k]
        sp = species_of(r)
        for i in frs:
            if i in FR_TAKEN: continue
            t = PAGE["things"][i]
            fsp = SPECIES.get(t["l"])
            if sp or fsp:
                s = 1.0 if sp and fsp == sp else 0.0
            else:
                a, b = norm(" ".join(r[5].split(" | ")))[:140], norm(t.get("d", ""))[:140]
                s = difflib.SequenceMatcher(None, a, b).ratio() if a and b else 0.0
            pairs.append((s, k, i))
    used_k = set()
    for s, k, i in sorted(pairs, reverse=True):
        if s < 0.45 or k in used_k or i in FR_TAKEN: continue
        MATCH[k] = i
        used_k.add(k)
        FR_TAKEN.add(i)
    # Those left: the same kind of person nearest where Red and Blue had them (their map scaled to FireRed's).
    left = []
    for k in mods:
        if k in used_k: continue
        r = PEOPLE[k]
        if species_of(r) or not PLACE_MAPS.get(place): continue
        try:
            rx, ry = int(r[6]), int(r[7])
        except (ValueError, IndexError):
            continue
        if rx < 0 or ry < 0 or place not in SIZES: continue
        m = PLACE_MAPS[place][0]
        W, H = SIZES[place]
        fw, fh = (m["x1"] - m["x0"]) / 2, (m["z1"] - m["z0"]) / 2
        px, py = (rx + 0.5) / W * fw, (ry + 0.5) / H * fh
        for i in frs:
            if i in FR_TAKEN: continue
            t = PAGE["things"][i]
            if SPECIES.get(t["l"]): continue
            if (t["l"] == "TEAM ROCKET") != (r[2] == "rocket"): continue
            d = math.hypot(t["gx"] - px, t["gy"] - py)
            if d <= 20: left.append((d, k, i))
    for d, k, i in sorted(left):
        if k in used_k or i in FR_TAKEN: continue
        MATCH[k] = i
        used_k.add(k)
        FR_TAKEN.add(i)


def boxes(text):
    """FireRed's words as the mod's text boxes (a sentence or two each)."""
    parts = re.findall(r"[^.!?…]+(?:[.!?…]+[\"']?|$)", text)
    out, cur = [], ""
    for p in (x.strip() for x in parts):
        if not p: continue
        if cur and len(cur) + 1 + len(p) > 80:
            out.append(cur)
            cur = p
        else:
            cur = (cur + " " + p).strip()
    if cur: out.append(cur)
    return " | ".join(out)


# FireRed's people the mod hadn't: into people.tsv (after a marker line, made again each time).
NEW, used = [], Counter()
for place, frs in sorted(fr_people.items()):
    for i in frs:
        if i in FR_TAKEN: continue
        t = PAGE["things"][i]
        sp = SPECIES.get(t["l"])
        text = t.get("d", "").strip()
        if not sp and (not text or t["l"] not in SPRITE): continue
        base = "%s_fr_%s" % (place, (sp or t["l"].lower().replace(" ", "_").replace(".", "")))
        used[base] += 1
        key = base if used[base] == 1 else "%s_%d" % (base, used[base])
        f = t.get("f") or ["down"]
        if len(f) == 1: facing, move = f[0], "stay"
        elif set(f) == {"left", "right"}: facing, move = "left_right", "walk"
        elif set(f) == {"up", "down"}: facing, move = "up_down", "walk"
        else: facing, move = "any_dir", "walk"
        if sp:
            lines = text if text.upper().startswith(t["l"] + ":") else "%s: !" % t["l"]
            sprite = "monster"
        else:
            lines, sprite = boxes(text), SPRITE[t["l"]]
        NEW.append([key, place, sprite, facing, move, lines, "-1", "-1"])
        MATCH[key] = i
        FR_TAKEN.add(i)
src = open(os.path.join(DATA, "people.tsv"), encoding="utf-8").read()
if MARK in src: src = src[:src.index(MARK)]
# (Fuchsia's zoo: Red and Blue's CHANSEY and SEEL as the POKéMON they are.)
src = src.replace("fuchsia_city_p4\tfuchsia_city\tchansey\tdown\tstay\t!\t", "fuchsia_city_p4\tfuchsia_city\tchansey\tdown\tstay\tCHANSEY: !\t")
src = src.replace("fuchsia_city_p7\tfuchsia_city\tseel\tany_dir\twalk\t!\t", "fuchsia_city_p7\tfuchsia_city\tseel\tany_dir\twalk\tSEEL: !\t")
src = src.rstrip("\n") + "\n" + MARK + "\n" + "".join("\t".join(r) + "\n" for r in NEW)
open(os.path.join(DATA, "people.tsv"), "w", encoding="utf-8").write(src)
PEOPLE = {r[0]: r for r in table("people.tsv")}

# The PLACER's list again (it reads people.tsv's outside people), and the caves' baked rows.
subprocess.run([sys.executable, os.path.join(HERE, "fr_placeables.py")], check=True, stdout=subprocess.DEVNULL)

# (The ROCKET out the back of Cerulean's burgled house, who gives back TM28: FireRed's grunt there, into the PLACER's
# list too.)
EXTRA = {("cerulean_city", "TEAM ROCKET", 33, 6): "cerulean_city_0"}
TR = {r[0]: r for r in table("trainers.tsv")}
with open(os.path.join(DATA, "placeables.tsv"), "a", encoding="utf-8") as f:
    for i, t in enumerate(PAGE["things"]):
        k = EXTRA.get((MAPS[t["m"]]["place"], t["l"], t["gx"], t["gy"]))
        if not k or k not in TR: continue
        team = ", ".join("%s %s" % (NAME_OF.get(m.split(":")[0], m.split(":")[0].upper()), m.split(":")[1]) for m in TR[k][7].split(",") if ":" in m)
        f.write("\t".join([k, TR[k][1], "trainer", k, TR[k][2], team, str(t["gx"]), str(t["gy"]), (t.get("f") or ["down"])[0], TR[k][5]]) + "\n")
        page_keyed[k] = t

# ---------- where each goes ----------
YAW = {"up": 180, "down": 0, "left": 90, "right": 270}
rows, how, report = [], Counter(), []


def fit(place, gx, gy):
    ms = PLACE_MAPS.get(place)
    if not ms: return None
    for m in ms:
        if 0 <= gx < (m["x1"] - m["x0"]) / 2 and 0 <= gy < (m["z1"] - m["z0"]) / 2: return (m["x0"] + 2 * gx + 1, m["z0"] + 2 * gy + 1)
    m = ms[0]
    return (m["x0"] + 2 * gx + 1, m["z0"] + 2 * gy + 1)


for r in table("placeables.tsv"):
    if len(r) < 10: continue
    key, place, kind = r[3], r[1], r[2]
    if kind not in ("trainer", "person", "item", "hidden") or not outdoors(place) or key in SKIP: continue
    if kind in ("trainer", "person") and place in OUTSIDE_NOW: continue
    if kind == "item" and (key in ITEM_SPOTS or "Red and Blue's" in r[5]): continue
    at, why = None, ""
    t = page_keyed.get(key)
    if key in MOVES: at, why = MOVES[key], "your drag on the page"
    elif kind == "trainer" and key in TRAINER_SPOTS: at, why = (TRAINER_SPOTS[key][0] + 0.5, TRAINER_SPOTS[key][1] + 0.5), "the game's spot"
    elif kind == "person" and key in SPOTS: at, why = (SPOTS[key][0] + 0.5, SPOTS[key][1] + 0.5), "the game's spot"
    elif t is not None and "mx" in t: at, why = (t["mx"], t["mz"]), "your own spot (the page)"
    elif t is not None: at, why = (t["x"], t["z"]), "FireRed's spot (the page)"
    elif kind == "person" and key in MATCH:
        f = PAGE["things"][MATCH[key]]
        at, why = (f["x"], f["z"]), "FireRed's spot for them (%s)" % f["l"]
    elif kind == "person":
        p = PEOPLE.get(key)
        try:
            rx, ry = int(p[6]), int(p[7])
        except (TypeError, ValueError, IndexError):
            rx = ry = -1
        ms = PLACE_MAPS.get(place)
        if ms and rx >= 0 and place in SIZES:
            m, (W, H) = ms[0], SIZES[place]
            gx = int((rx + 0.5) / W * (m["x1"] - m["x0"]) / 2)
            gy = int((ry + 0.5) / H * (m["z1"] - m["z0"]) / 2)
            at, why = (m["x0"] + 2 * gx + 1, m["z0"] + 2 * gy + 1), "Red and Blue's spot, scaled (FireRed hasn't them)"
    else:
        try:
            at, why = fit(place, int(r[6]), int(r[7])), "FireRed's spot (fitted)"
        except ValueError:
            at = None
    if at is None:
        report.append(("not placed", kind, key, place))
        continue
    how[why.split(" (")[0] if "for them" not in why else "FireRed's spot for them"] += 1
    rows.append([key, kind, place, "-", "%.2f" % at[0], "NaN", "%.2f" % at[1], str(YAW.get(r[8], 0)), "placed"])
    report.append((why, kind, key, place))

with open(os.path.join(DATA, "placed.tsv"), "a", encoding="utf-8") as f:
    f.write("# (0.74) Outside, at a best guess (tools/place_outside74.py); heights found in the game\n")
    for row in rows: f.write("\t".join(row) + "\n")
json.dump(report, open(os.path.join(HERE, "placemap", "outside74.json"), "w"), indent=0)
print(len(rows), "placed outside:", dict(Counter(r[1] for r in rows)))
print("how:", dict(how))
print("new people from FireRed:", len(NEW), [n[0] for n in NEW])
print("not placed:", [x for x in report if x[0] == "not placed"])
print("matched:", {k: PAGE["things"][i]["l"] for k, i in MATCH.items() if not k.startswith(tuple(n[0] for n in NEW))})
