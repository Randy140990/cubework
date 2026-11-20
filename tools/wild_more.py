#!/usr/bin/env python3
"""Makes sure every one of the 251 can be found in the wild, for John's note: "all 151 and whatever's in Johto are
catchable in an area". Adds to wild.tsv (Kanto is Yellow's, with Red's and Blue's gaps; Johto was Silver's grass and
water only):

- Gold's own Johto grass and water slots, beside Silver's, at the same odds (Spinarak, Mankey, Teddiursa, Gligar...).
- Johto fishing (old, good and super rod) by place, from Gold/Silver's fishing groups, day and night.
- Headbutting trees and smashing rocks, by place (Heracross, Pineco, Aipom, Shuckle...).
- The Bug-Catching Contest in the National Park, and the swarms (Yanma, Snubbull, Dunsparce, Marill, Qwilfish, Remoraid).
- Our own additions, for the ones the old games only gave you (the starters) or hatched from eggs (the babies), each
  rare, in a place that suits it. Marked "ours" so they're easy to find and change.

Also adds Celebi in Ilex Forest's shrine to statics.tsv (the old games' event).

    python3 tools/wild_more.py [pret folder]   (writes src/main/resources/retromon/data/wild.tsv and statics.tsv)
"""
import os, re, sys

PRET = sys.argv[1] if len(sys.argv) > 1 else "/home/claude/pret"
GOLD = os.path.join(PRET, "pokegold")
DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "retromon", "data")


def ident(c):
    c = c.strip()
    special = {"NIDORAN_M": "nidoran_m", "NIDORAN_F": "nidoran_f", "MR__MIME": "mr_mime", "FARFETCH_D": "farfetch_d", "HO_OH": "ho_oh"}
    return special.get(c, c.lower())


def camel(name):
    """Route29 -> route_29, IlexForest -> ilex_forest, MountMortar1FOutside -> mount_mortar_1f_outside."""
    s = re.sub(r"(?<=[a-z])(?=[A-Z0-9])|(?<=[0-9])(?=[A-Z][a-z])", "_", name)
    s = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", "_", s)
    return s.lower().replace("__", "_")


def strip(l): return l.split(";")[0].strip()


def read(p): return open(p, encoding="utf-8").read()


def rows(f): return [l.rstrip("\n").split("\t") for l in open(os.path.join(DATA, f), encoding="utf-8") if l.strip() and not l.startswith("#")]


# ---------- what's there already ----------
HEAD = "# place\tmethod\ttime\trate_per_256\tslots species:level:weight\n"
existing = [r for r in rows("wild.tsv") if r[1] in ("grass", "water", "super_rod", "good_rod", "old_rod") and not (len(r) > 5 and r[5] == "ours")]
# Drop anything this tool added before, so it can run again.
existing = [r for r in existing if r[1] in ("grass", "water", "super_rod", "good_rod", "old_rod")]
places = {r[0] for r in rows("places.tsv")}
region = {r[0]: r[1] for r in rows("places.tsv")}
# Kanto stays Yellow's: Gold and Silver's fishing and trees only go where Yellow has nothing (Johto, and the routes
# Yellow doesn't have, 26 to 28).
YELLOW_PLACES = {r[0] for r in existing if region.get(r[0]) == "kanto"}
gs_ok = lambda pid: pid in places and pid not in YELLOW_PLACES
table = {}
order = []
for r in existing:
    k = (r[0], r[1], r[2]); table[k] = [r[3], [tuple(s.split(":")) for s in r[4].split(",") if s]]; order.append(k)


def add_rows(place, method, tod, rate, slots):
    k = (place, method, tod)
    if k not in table: table[k] = [str(rate), []]; order.append(k)
    have = {(sp, lv) for sp, lv, _ in table[k][1]}
    for sp, lv, w in slots:
        if (sp, str(lv)) not in have: table[k][1].append((sp, str(lv), str(w))); have.add((sp, str(lv)))


# ---------- 1. Gold's grass and water, beside Silver's ----------
GRASS_W = [30, 30, 20, 10, 5, 4, 1]
place, rates, times, cur, mode = None, None, None, None, "both"
for raw in read(os.path.join(GOLD, "data/wild/johto_grass.asm")).splitlines():
    l = strip(raw)
    if l.startswith("IF DEF(_GOLD)"): mode = "gold"; continue
    if l.startswith("IF DEF(_SILVER)") or l.startswith("ELIF DEF(_SILVER)") or l.startswith("ELSE"): mode = "silver"; continue
    if l.startswith("ENDC"): mode = "both"; continue
    m = re.match(r"def_grass_wildmons (\w+)", l)
    if m: place, times, cur = ident(m.group(1)), {"morn": [], "day": [], "nite": []}, None; continue
    m = re.match(r"db (\d+) percent, (\d+) percent, (\d+) percent", l)
    if m: rates = [int(x) for x in m.groups()]; continue
    c = raw.strip().lstrip(";").strip()
    if c in ("morn", "day", "nite"): cur = c; continue
    m = re.match(r"db (\d+), (\w+)", l)
    if m and cur and place and mode == "gold": times[cur].append((len([1 for _ in times[cur]]), int(m.group(1)), ident(m.group(2))))
    if l.startswith("end_grass_wildmons") and place:
        for i, tod in enumerate(("morn", "day", "nite")):
            # Gold's slots, at the odds of the slot they stand in (in the order they come).
            slots = [(sp, lv, GRASS_W[min(j, 6)] * 256 // 100) for j, (_, lv, sp) in enumerate(times[tod][:7])]
            if slots: add_rows(place, "grass", tod, round(rates[i] * 256 / 100), slots)
        place = None
WATER_W = [60, 30, 10]
place, mode, slots = None, "both", []
for raw in read(os.path.join(GOLD, "data/wild/johto_water.asm")).splitlines():
    l = strip(raw)
    if l.startswith("IF DEF(_GOLD)"): mode = "gold"; continue
    if l.startswith("IF DEF(_SILVER)") or l.startswith("ELIF DEF(_SILVER)") or l.startswith("ELSE"): mode = "silver"; continue
    if l.startswith("ENDC"): mode = "both"; continue
    m = re.match(r"def_water_wildmons (\w+)", l)
    if m: place, slots, rate = ident(m.group(1)), [], 0; continue
    m = re.match(r"db (\d+) percent$", l)
    if m and place: rate = int(m.group(1)); continue
    m = re.match(r"db (\d+), (\w+)", l)
    if m and place and mode == "gold": slots.append((int(m.group(1)), ident(m.group(2))))
    if l.startswith("end_water_wildmons") and place:
        if slots: add_rows(place, "water", "any", round(rate * 256 / 100), [(sp, lv, WATER_W[min(j, 2)] * 256 // 100) for j, (lv, sp) in enumerate(slots[:3])])
        place = None

# ---------- 2. Johto fishing ----------
fish = read(os.path.join(GOLD, "data/wild/fish.asm"))
groups = re.findall(r"fishgroup [^,]+, \.(\w+)_Old,\s+\.\w+_Good,\s+\.\w+_Super", fish)
gname = {}
for g in groups: gname[g] = g
tgroups = []
m = re.search(r"TimeFishGroups:(.*)", fish, re.S)
for line in m.group(1).splitlines():
    mm = re.match(r"\s*db (\w+),\s*(\d+),\s*(\w+),\s*(\d+)", line)
    if mm: tgroups.append(((ident(mm.group(1)), int(mm.group(2))), (ident(mm.group(3)), int(mm.group(4)))))
rods = {}
for g in groups:
    for rod in ("Old", "Good", "Super"):
        blk = re.search(r"\." + g + "_" + rod + r":\n(.*?)(?=\n\.|\nTimeFishGroups)", fish, re.S)
        if not blk: continue
        prev, out = 0, {}
        for line in blk.group(1).splitlines():
            mm = re.match(r"\s*db\s+(\d+) percent( \+ 1)?,\s+(time_group (\d+)|(\w+),\s+(\d+))", line)
            if not mm: continue
            cum = int(mm.group(1)); w = max(1, round((cum - prev) * 256 / 100)); prev = cum
            if mm.group(4) is not None:
                (d, dl), (n, nl) = tgroups[int(mm.group(4))]
                out[(d, dl)] = out.get((d, dl), 0) + w // 2 or 1; out[(n, nl)] = out.get((n, nl), 0) + w // 2 or 1
            else:
                k = (ident(mm.group(5)), int(mm.group(6))); out[k] = out.get(k, 0) + w
        rods[(g, rod)] = out
GROUP_OF = {}
for mm in re.finditer(r"map (\w+), TILESET_\w+, (\w+), LANDMARK_\w+, MUSIC_\w+, \w+, PALETTE_\w+, FISHGROUP_(\w+)", read(os.path.join(GOLD, "data/maps/maps.asm"))):
    pid, env, grp = camel(mm.group(1)), mm.group(2), mm.group(3)
    if gs_ok(pid) and env not in ("INDOOR",):
        g = "".join(w.capitalize() for w in grp.lower().split("_")).replace("Noswarm", "NoSwarm").replace("Whirlislands", "WhirlIslands").replace("2", "_2")
        GROUP_OF[pid] = g
fished = 0
for pid, g in GROUP_OF.items():
    for rod, meth in (("Old", "old_rod"), ("Good", "good_rod"), ("Super", "super_rod")):
        sl = rods.get((g, rod))
        if not sl: continue
        add_rows(pid, meth, "any", 0, [(sp, lv, w) for (sp, lv), w in sl.items()]); fished += 1

# ---------- 3. Headbutt trees and smashed rocks ----------
tm = read(os.path.join(GOLD, "data/wild/treemons.asm"))
def treeset(name):
    body = re.search(r"TreeMonSet_" + name + r":\n(.*?)(?=\nTreeMonSet_|\Z)", tm, re.S).group(1)
    # Gold and Silver both: take every table in the set (common and rare, both versions).
    out = {}
    for line in body.splitlines():
        mm = re.match(r"\s*db\s+(\d+), (\w+),\s+(\d+)", line)
        if mm: k = (ident(mm.group(2)), int(mm.group(3))); out[k] = out.get(k, 0) + int(mm.group(1))
    return out
sets = {s: treeset(s) for s in ("Forest", "Canyon", "Rock", "City")}
for mm in re.finditer(r"treemon_map (\w+),\s+TREEMON_SET_(\w+)", read(os.path.join(GOLD, "data/wild/treemon_maps.asm"))):
    pid, s = ident(mm.group(1)), mm.group(2).capitalize()
    if s not in sets or not gs_ok(pid): continue
    meth = "rock_smash" if s == "Rock" else "headbutt"
    tot = sum(sets[s].values()); add_rows(pid, meth, "any", 0, [(sp, lv, max(1, w * 256 // tot)) for (sp, lv), w in sets[s].items()])

# ---------- 4. The Bug-Catching Contest, and swarms ----------
bc = read(os.path.join(GOLD, "data/wild/bug_contest_mons.asm"))
add_rows("national_park_bug_contest", "contest", "any", 64, [(ident(s), (int(a) + int(b)) // 2, max(1, int(p) * 256 // 100)) for p, s, a, b in re.findall(r"db\s+(\d+), (\w+),\s+(\d+),\s+(\d+)", bc)])
for f, meth in (("swarm_grass.asm", "swarm"), ("swarm_water.asm", "swarm")):
    txt = read(os.path.join(GOLD, "data/wild/" + f)); place = None; seen = []
    for raw in txt.splitlines():
        l = strip(raw)
        m = re.match(r"def_(grass|water)_wildmons (\w+)", l)
        if m: place = ident(m.group(2)); seen = []; continue
        m = re.match(r"db (\d+), (\w+)", l)
        if m and place: seen.append((ident(m.group(2)), int(m.group(1))))
        if l.startswith("end_") and place:
            add_rows(place, meth, "any", 26, [(sp, lv, 36) for sp, lv in dict.fromkeys(seen)]); place = None

# ---------- 5. Ours: the ones the old games only gave you or hatched ----------
OURS = [
    # Kanto's starters, near where Yellow hands them out, rare.
    ("route_25", "grass", "any", "bulbasaur", 12, 6, "By Cerulean, where the girl by the Gym gives one away"),
    ("pokemon_mansion_1f", "grass", "any", "charmander", 32, 6, "In the burnt-out mansion on Cinnabar"),
    ("route_6", "water", "any", "squirtle", 15, 8, "In the water by Vermilion, Officer Jenny's town, and the grass beside it"),
    ("route_6", "grass", "any", "squirtle", 15, 6, ""),
    # Johto's starters.
    ("route_46", "grass", "day", "chikorita", 6, 8, "In the long grass on the way down to New Bark"),
    ("route_46", "grass", "morn", "chikorita", 6, 8, ""),
    ("route_37", "grass", "nite", "cyndaquil", 15, 8, "By Ecruteak's burnt tower, at night"),
    ("route_37", "grass", "day", "cyndaquil", 15, 4, ""),
    ("route_32", "water", "any", "totodile", 15, 8, "In the water along Route 32, and the grass by it"),
    ("route_32", "grass", "day", "totodile", 12, 6, ""), ("route_32", "grass", "morn", "totodile", 12, 6, ""),
    # The babies, as eggs would give them (no breeding in the mod).
    ("viridian_forest", "grass", "any", "pichu", 4, 6, "With the PIKACHU in Viridian Forest"),
    ("route_4", "grass", "any", "cleffa", 8, 6, "Outside Mt. Moon, where CLEFAIRY live"),
    ("route_3", "grass", "any", "igglybuff", 6, 6, "On the way to Mt. Moon, with JIGGLYPUFF"),
    ("ice_path_b1f", "grass", "any", "smoochum", 22, 10, "In the Ice Path"),
    ("power_plant", "grass", "any", "elekid", 22, 10, "In the Power Plant, with the ELECTABUZZ"),
    ("burned_tower_1f", "grass", "any", "magby", 14, 10, "In the Burned Tower"),
    ("mount_mortar_1f_inside", "grass", "any", "tyrogue", 14, 10, "In Mt. Mortar, where the Karate King trains"),
    ("national_park", "grass", "day", "togepi", 12, 4, "In the National Park (the egg from ELM's aide still hatches one)"),
    # Gold and Silver had these in Kanto's grass, which here is Yellow's: put back where those games had them.
    ("route_7", "grass", "any", "houndour", 16, 8, "Route 7, as in Gold"),
    ("route_7", "grass", "any", "murkrow", 16, 8, "Route 7 and 16, as in Silver"), ("route_16", "grass", "any", "murkrow", 16, 8, ""),
    ("route_16", "grass", "any", "slugma", 18, 10, "Routes 16 to 18, as in Gold and Silver"), ("route_17", "grass", "any", "slugma", 20, 10, ""), ("route_18", "grass", "any", "slugma", 20, 10, ""),
]
notes = []
for place, meth, tod, sp, lv, w, note in OURS:
    if place not in places: print("no place", place); continue
    tods = ("morn", "day", "nite") if tod == "any" and meth == "grass" and any(k[0] == place and k[1] == "grass" and k[2] != "any" for k in table) else (tod,)
    for t in tods:
        k = (place, meth, t)
        if k not in table: table[k] = ["20" if meth == "grass" else "10", []]; order.append(k)
        table[k][1].append((sp, str(lv), str(w)))
    if note: notes.append(f"{sp}: {place} ({meth}). {note}")

# ---------- write ----------
with open(os.path.join(DATA, "wild.tsv"), "w", encoding="utf-8") as f:
    f.write(HEAD)
    for k in order:
        rate, sl = table[k]
        f.write("\t".join([k[0], k[1], k[2], rate, ",".join(":".join(s) for s in sl)]) + "\n")
# Celebi in Ilex Forest's shrine.
st = open(os.path.join(DATA, "statics.tsv"), encoding="utf-8").read()
if "celebi_ilex" not in st:
    st = st.rstrip("\n") + "\ncelebi_ilex\tcelebi\t30\tilex_forest\tstatic\tAt the shrine in Ilex Forest (the old games' event, here for everyone)\n"
    open(os.path.join(DATA, "statics.tsv"), "w", encoding="utf-8").write(st)
print(len(order), "tables,", fished, "fishing tables")
print("\n".join(notes))
