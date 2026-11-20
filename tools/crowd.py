#!/usr/bin/env python3
"""More people for Retromon (John's 15:14 notes): 100 townspeople for busy places, more trainer looks, Team Rocket
grunts in different kit, JESSIE and JAMES in red, and a few more nurses, officers and clerks (men and women).

Paints their skins with tools/skins2.py into the mod's npc folder and writes the Person Viewer's list of them
(tools/personviewer/more.js).

    python3 tools/crowd.py
"""
import json, os, random, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import skins2
from skins2 import PEOPLE, person

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "textures", "entity", "npc")

COLS = [("red", "D8343C"), ("coral", "F07A5A"), ("orange", "F28A2A"), ("mustard", "D9A22E"), ("yellow", "F2C12E"), ("lime", "9ACD3A"), ("green", "3F9A4A"),
        ("forest", "2E5E3A"), ("mint", "7FD8B0"), ("teal", "2A9A92"), ("sky blue", "6AB8E8"), ("blue", "3A6FD0"), ("navy", "243A6A"), ("purple", "7A4AB8"),
        ("lilac", "B89AE0"), ("pink", "F28AB2"), ("magenta", "C83A8A"), ("maroon", "7A2A3A"), ("brown", "7A5030"), ("tan", "C8A87A"), ("cream", "F2E8D0"),
        ("white", "F4F4F2"), ("grey", "8A8E96"), ("charcoal", "3A3C44"), ("black", "1E1E24"), ("denim", "4A5F8A"), ("khaki", "A8946A"), ("olive", "6E7A3A")]
NAME = {h: n for n, h in COLS}
HAIRC = ["1A1A1A", "3A2A20", "6A4428", "8A4A2A", "A85A2A", "D87A3A", "E8C870", "E8E4D0", "A8A8A8", "3A6FD0", "E86AA8", "7A4AB8", "2A9A92"]
TONES = ["pale", "fair", "light", "tan", "medium", "brown", "deep", "dark"]
EYES = ["brown", "dark", "blue", "green", "grey", "hazel", "amber"]
MOTIF_NAME = {"heart": "a heart", "star": "a star", "ball": "a ball", "bolt": "a lightning bolt", "leaf": "a leaf", "flame": "a flame", "drop": "a drop",
              "moon": "a moon", "note": "a music note", "seven": "a 7", "chevron": "a chevron", "crown": "a crown", "paw": "a paw print", "flower": "a flower",
              "smile": "a smiley", "wave": "a wave"}


def cname(h): return NAME.get(h, "")


def art(w): return ("an " if w[:1].lower() in "aeiou" else "a ") + w


def describe(sp):
    """A short line of what they're wearing, from their spec."""
    bits = []
    top = sp.get("top", "tee"); tc = cname(sp.get("topc", ""))
    topw = {"tee": "tee", "hoodie": "hoodie", "jumper": "jumper", "shirt": "shirt", "blouse": "blouse", "tank": "vest top", "dress": "dress"}.get(top, top)
    pat = {"check": "checked ", "dots": "spotty ", "vstripe": "striped ", "yoke": "two-tone "}.get(sp.get("pattern"), "")
    t = f"{tc} {pat}{topw}".strip()
    if sp.get("motif"): t += f" with {MOTIF_NAME[sp['motif']]}"
    if sp.get("sleevec"): t += f", {cname(sp['sleevec'])} sleeves"
    if sp.get("coat"):
        cc = cname(sp.get("coatc", ""))
        kind = "puffer jacket" if sp.get("puffer") and sp.get("coatsleeve", 12) > 1 else "gilet" if sp.get("puffer") else {"jacket": "jacket", "leather": "leather jacket", "blazer": "blazer",
                "cardigan": "cardigan", "suit": "suit jacket", "vest": "waistcoat", "lab": "white coat"}.get(sp["coat"], "jacket")
        if sp.get("coatsleevec"): kind = f"varsity jacket ({cname(sp['coatsleevec'])} sleeves)"
        long = " long" if sp.get("coatlen", 12) > 13 else ""
        bits.append(art(f"{long.strip()} {cc} {kind}".strip()) + (" done up" if sp.get("closed") else "") + " over " + art(t))
    else:
        bits.append(t[0].upper() + t[1:] if t else "")
    b = sp.get("bottom", "trousers"); bc = cname(sp.get("botc", ""))
    if b != "dress":
        bw = {"jeans": "jeans", "trousers": "joggers" if sp.get("jog") else "trousers", "shorts": "shorts", "skirt": "skirt", "combat": "combat trousers", "overalls": "dungarees"}.get(b, b)
        bits.append(f"{bc} {bw}".strip())
    hw = sp.get("hw")
    hc = cname(sp.get("hwc", ""))
    if hw: bits.append("the hood up" if hw == "hood" else art(f"{hc} " + {"cap": "cap", "capback": "cap on backwards", "bucket": "bucket hat", "beret": "beret", "headband": "headband", "bobble": "bobble hat"}[hw]))
    if sp.get("hat") in ("beanie", "sunhat", "visor"): bits.append(art(f"{cname(sp.get('hatc', ''))} {sp['hat'].replace('sunhat', 'sun hat')}".strip()))
    if sp.get("phones"): bits.append("headphones")
    if sp.get("shades"): bits.append("shades")
    elif sp.get("glasses"): bits.append("glasses")
    if sp.get("scarf"): bits.append(art(f"{cname(sp['scarf'])} scarf".strip()))
    if sp.get("bag") == "backpack": bits.append("a backpack")
    elif sp.get("bag"): bits.append("a satchel")
    s = ", ".join(x for x in bits if x)
    i = s.rfind(", ")
    if i > 0: s = s[:i] + " and " + s[i + 2:]
    s = s.replace("an ", "an ").strip()
    return (s[0].upper() + s[1:] + ".") if s else ""


# ---------- 100 townspeople, by the kind of person they are ----------
# Each kind: name, where, and how it dresses (a function of a random source and whether they're a woman).
def pick(r, xs): return r.choice(xs)


def col(r, names): return pick(r, [h for n, h in COLS if n in names])


BRIGHT = ["red", "coral", "orange", "mustard", "yellow", "lime", "green", "mint", "teal", "sky blue", "blue", "purple", "lilac", "pink", "magenta"]
CALM = ["navy", "forest", "maroon", "brown", "tan", "cream", "white", "grey", "charcoal", "black", "denim", "khaki", "olive", "teal"]
DARK = ["navy", "charcoal", "black", "denim", "brown", "forest", "maroon", "grey", "olive", "khaki"]
MOTIFS = list(MOTIF_NAME)


def base(r, fem, i):
    hs = ["long", "bob", "ponytail", "bun", "pigtails", "afro", "braids", "short"] if fem else ["short", "spiky", "swept", "buzz", "afro", "short", "swept", "bald"]
    return dict(tone=TONES[i % 8], fem=fem, hair=pick(r, hs), hairc=pick(r, HAIRC[:9] if r.random() < 0.85 else HAIRC), eyes=pick(r, EYES),
                shoec=col(r, ["white", "black", "charcoal", "brown", "red", "blue", "tan"]), freckles=r.random() < 0.12)


def k_shopper(r, f):
    return dict(top=pick(r, ["jumper", "tee", "blouse" if f else "shirt"]), topc=col(r, BRIGHT + CALM), pattern=pick(r, [None, None, "dots", "vstripe"]), patc=col(r, ["white", "cream", "navy"]),
                bottom=pick(r, ["jeans", "skirt" if f else "trousers", "trousers"]), botc=col(r, DARK), bag="satchel", bagc=col(r, ["brown", "tan", "black", "red", "cream"]))
def k_commuter(r, f):
    return dict(top="shirt", topc=col(r, ["white", "sky blue", "cream", "lilac"]), coat="jacket", closed=True, coatc=col(r, ["navy", "charcoal", "khaki", "olive", "black", "maroon"]),
                bottom="trousers", botc=col(r, DARK), bag="satchel", bagc=col(r, ["black", "brown"]), phones=col(r, ["black", "white", "grey"]) if r.random() < 0.5 else None)
def k_student(r, f):
    return dict(top="hoodie", topc=col(r, BRIGHT + ["navy", "grey", "maroon"]), motif=pick(r, MOTIFS), motifc=col(r, ["white", "yellow", "cream"]), bottom=pick(r, ["jeans", "trousers"]), jog=r.random() < 0.4,
                botc=col(r, DARK), bag="backpack", bagc=col(r, BRIGHT + DARK), glasses=r.random() < 0.3)
def k_skater(r, f):
    return dict(top="hoodie", topc=col(r, ["charcoal", "black", "maroon", "olive", "teal", "purple", "orange"]), motif=pick(r, ["flame", "star", "chevron", "wave", "seven"]), motifc=col(r, ["white", "yellow", "lime", "pink"]),
                bottom="trousers", jog=True, stripec=col(r, ["white", "red", "yellow"]), botc=col(r, ["black", "charcoal", "khaki", "olive"]), hw=pick(r, ["capback", "bobble", "capback"]), hwc=col(r, BRIGHT + DARK),
                bobc=col(r, ["white", "yellow"]), shoec=col(r, ["white", "black"]), soles=col(r, ["white", "red", "black"]))
def k_gamer(r, f):
    return dict(top=pick(r, ["hoodie", "tee"]), topc=col(r, ["purple", "black", "navy", "teal", "magenta", "lime"]), motif=pick(r, ["bolt", "star", "crown", "smile", "ball"]), motifc=col(r, ["lime", "pink", "yellow", "white", "sky blue"]),
                motif2=col(r, ["white", "red"]), bottom=pick(r, ["jeans", "trousers"]), jog=True, botc=col(r, DARK), phones=col(r, ["black", "white", "red", "lime", "pink"]))
def k_artist(r, f):
    return dict(top=pick(r, ["shirt", "jumper"]), topc=col(r, ["cream", "white", "mustard", "teal", "coral"]), pattern=pick(r, ["check", "vstripe", None]), patc=col(r, ["red", "navy", "forest"]),
                bottom=pick(r, ["overalls", "jeans", "skirt" if f else "trousers"]), botc=col(r, ["denim", "khaki", "black", "olive"]), hw="beret", hwc=col(r, ["red", "black", "navy", "mustard", "purple"]), apron=None)
def k_runner(r, f):
    return dict(top="tank", topc=col(r, BRIGHT), bottom=pick(r, ["shorts", "trousers"]), jog=True, botc=col(r, ["black", "charcoal", "navy"]), stripec=col(r, BRIGHT),
                hw="headband", hwc=col(r, BRIGHT + ["white"]), wristband=col(r, ["white", "black"] + BRIGHT[:3]), shoec="F4F4F2", soles=col(r, BRIGHT))
def k_cyclist(r, f):
    return dict(top="tee", topc=col(r, BRIGHT), sleevec=col(r, ["black", "white", "navy"]), coat="jacket", closed=True, coatc=col(r, ["yellow", "lime", "orange", "sky blue", "red"]), coatsleeve=12,
                bottom="shorts", botc=col(r, ["black", "charcoal"]), hw="cap", hwc=col(r, ["white", "black"] + BRIGHT[:4]), gloves=col(r, ["black", "charcoal"]), socks=col(r, ["white", "black"]))
def k_tourist(r, f):
    return dict(top=pick(r, ["tee", "shirt"]), topc=col(r, BRIGHT + ["white"]), motif=pick(r, ["heart", "star", "flower", "wave", "leaf"]), motifc=col(r, ["white", "red", "yellow", "navy"]),
                bottom="shorts", botc=col(r, ["khaki", "tan", "denim", "cream"]), hw="bucket", hwc=col(r, ["cream", "khaki", "white", "sky blue", "olive"]), bag="backpack", bagc=col(r, BRIGHT + DARK), socks="F4F4F4", shades=r.random() < 0.5)
def k_musicfan(r, f):
    return dict(top="tee", topc=col(r, ["black", "charcoal", "white", "maroon"]), motif=pick(r, ["note", "star", "flame", "crown"]), motifc=col(r, ["red", "white", "yellow", "pink"]),
                coat=pick(r, ["leather", "jacket"]), coatc=col(r, ["black", "denim", "brown"]), bottom="jeans", botc=col(r, ["black", "denim", "charcoal"]), phones=col(r, ["black", "red", "white"]) if r.random() < 0.6 else None)
def k_bookworm(r, f):
    return dict(top=pick(r, ["shirt", "blouse" if f else "jumper"]), topc=col(r, ["cream", "white", "sky blue", "lilac", "mint"]), coat="cardigan", coatc=col(r, ["mustard", "forest", "maroon", "navy", "brown", "lilac"]),
                bottom=pick(r, ["skirt" if f else "trousers", "trousers"]), botc=col(r, ["brown", "navy", "khaki", "charcoal"]), glasses=True, bag="satchel", bagc=col(r, ["brown", "tan"]))
def k_cafe(r, f):
    return dict(top="jumper", topc=col(r, CALM + ["mustard", "coral", "mint"]), pattern=pick(r, ["yoke", "check", "dots", None]), patc=col(r, ["cream", "white", "navy", "red"]),
                bottom=pick(r, ["jeans", "trousers", "skirt" if f else "jeans"]), botc=col(r, DARK), scarf=col(r, ["red", "mustard", "forest", "lilac", "cream"]) if r.random() < 0.6 else None)
def k_office(r, f):
    return dict(top=pick(r, ["shirt", "blouse" if f else "shirt"]), topc=col(r, ["white", "sky blue", "lilac", "cream"]), coat="blazer", coatc=col(r, ["navy", "charcoal", "grey", "black", "maroon", "tan"]),
                tie=col(r, ["red", "navy", "teal", "purple", "mustard"]) if not f and r.random() < 0.7 else None, bottom=pick(r, ["trousers", "skirt" if f else "trousers"]), botc=col(r, ["navy", "charcoal", "grey", "black"]),
                shoec=col(r, ["black", "brown"]), bag="satchel", bagc=col(r, ["black", "brown"]))
def k_park(r, f):
    return dict(top=pick(r, ["jumper", "shirt"]), topc=col(r, BRIGHT + CALM), coat="vest", puffer=True, closed=True, coatsleeve=1, coatc=col(r, ["navy", "olive", "red", "black", "forest", "mustard"]),
                bottom="jeans", botc=col(r, DARK), hw="bobble", hwc=col(r, BRIGHT + ["navy", "grey", "cream"]), bobc=col(r, ["white", "cream", "yellow", "pink"]))
def k_gymfan(r, f):
    c = col(r, ["red", "blue", "green", "purple", "orange", "navy", "maroon"])
    return dict(top="tee", topc=col(r, ["white", "cream", "grey"]), coat="jacket", closed=True, coatc=c, coatsleevec=col(r, ["white", "cream"]), motif=pick(r, ["seven", "star", "bolt", "crown"]),
                motifc=col(r, ["white", "yellow", "cream"]), bottom=pick(r, ["jeans", "trousers"]), jog=r.random() < 0.5, botc=col(r, DARK), hw="cap", hwc=c, hwlogo=col(r, ["white", "yellow"]))
def k_beach(r, f):
    return dict(top="tank", topc=col(r, BRIGHT + ["white"]), pattern=pick(r, [None, "vstripe", "dots"]), patc=col(r, ["white", "navy", "yellow"]), bottom="shorts", botc=col(r, BRIGHT + ["denim", "white"]),
                hw=pick(r, ["bucket", None]), hwc=col(r, ["white", "cream", "sky blue", "yellow"]), hat=None, shades=True, shoec=col(r, ["tan", "white", "sky blue"]))
def k_walker(r, f):
    return dict(top="jumper", topc=col(r, BRIGHT + CALM), coat="jacket", puffer=True, closed=True, coatc=col(r, ["red", "navy", "teal", "orange", "olive", "black", "purple"]),
                bottom=pick(r, ["trousers", "jeans"]), botc=col(r, ["khaki", "olive", "charcoal", "brown"]), hat="beanie", hatc=col(r, BRIGHT + ["grey", "navy"]), bag="backpack", bagc=col(r, DARK + BRIGHT[:5]), shoec=col(r, ["brown", "charcoal"]))
def k_kid(r, f):
    return dict(top=pick(r, ["tee", "tee", "hoodie"]), topc=col(r, BRIGHT), motif=pick(r, ["heart", "star", "ball", "bolt", "paw", "smile", "flower"]), motifc=col(r, ["white", "yellow", "pink", "sky blue"]), motif2="F4F4F2",
                bottom=pick(r, ["shorts", "skirt" if f else "shorts", "jeans"]), botc=col(r, ["denim", "navy", "red", "khaki", "pink", "purple"]), hw=pick(r, ["cap", "capback", None, "bucket"]), hwc=col(r, BRIGHT),
                socks="F4F4F4", blush=True, shoec=col(r, ["red", "blue", "white", "pink", "yellow"]))
def k_teen(r, f):
    return dict(top="hoodie", topc=col(r, ["charcoal", "black", "maroon", "navy", "forest", "lilac", "pink", "mint", "mustard"]), hw="hood", motif=pick(r, [None, "moon", "star", "wave", "drop"]), motifc=col(r, ["white", "cream", "pink", "lime"]),
                bottom="trousers", jog=True, stripec=col(r, ["white", "black", "red"]) if r.random() < 0.5 else None, botc=col(r, ["black", "charcoal", "grey", "navy"]), phones=col(r, ["white", "black"]) if r.random() < 0.3 else None)
def k_grand(r, f):
    return dict(hair=pick(r, ["bun", "bob", "short"] if f else ["bald", "short", "swept"]), hairc=pick(r, ["E8E8E8", "C8C8C8", "A8A8A8", "D8D8D4"]), top=pick(r, ["shirt", "blouse" if f else "shirt"]),
                topc=col(r, ["cream", "white", "sky blue", "lilac", "mint"]), coat="cardigan", coatc=col(r, ["maroon", "forest", "brown", "mustard", "navy", "lilac", "teal"]), glasses=r.random() < 0.7,
                bottom=pick(r, ["skirt" if f else "trousers", "trousers"]), botc=col(r, ["brown", "grey", "navy", "khaki"]), hw=None if f else pick(r, ["cap", None]), hwc=col(r, ["grey", "brown", "navy", "tan"]),
                moustache=(not f and r.random() < 0.5), beardc="E8E8E8")
def k_parent(r, f):
    return dict(top=pick(r, ["tee", "jumper", "shirt"]), topc=col(r, BRIGHT + CALM), pattern=pick(r, [None, "check", None, "yoke"]), patc=col(r, ["navy", "red", "white"]), coat=pick(r, [None, "jacket"]),
                coatc=col(r, ["denim", "khaki", "navy", "olive"]), bottom=pick(r, ["jeans", "jeans", "skirt" if f else "trousers"]), botc=col(r, DARK), bag=pick(r, ["satchel", "backpack", None]), bagc=col(r, BRIGHT + DARK),
                beard=(not f and r.random() < 0.35))
def k_fashion(r, f):
    return dict(top=pick(r, ["blouse" if f else "shirt", "tee", "jumper"]), topc=col(r, ["black", "white", "cream", "pink", "lilac", "mint"]), coat=pick(r, ["blazer", "jacket"]), coatlen=15, coatc=col(r, ["tan", "cream", "black", "maroon", "lilac", "pink", "mustard"]),
                bottom=pick(r, ["trousers", "skirt" if f else "trousers", "jeans"]), botc=col(r, ["black", "cream", "charcoal", "white"]), hw=pick(r, ["beret", None, "bucket"]), hwc=col(r, ["black", "cream", "pink", "maroon"]),
                shades=r.random() < 0.4, earrings=col(r, ["yellow", "white"]) if f else None)
def k_trader(r, f):
    return dict(top="shirt", topc=col(r, ["red", "blue", "green", "mustard", "teal"]), pattern="check", patc=col(r, ["white", "cream", "black"]), apron=col(r, ["cream", "white", "forest", "navy", "maroon"]),
                bottom="trousers", botc=col(r, ["brown", "charcoal", "navy", "khaki"]), hw=pick(r, ["cap", "bucket"]), hwc=col(r, ["navy", "forest", "maroon", "khaki"]))
def k_rocker(r, f):
    return dict(hair=pick(r, ["spiky", "long", "swept"] if not f else ["long", "bob", "ponytail"]), hairc=pick(r, ["E86AA8", "3A6FD0", "7A4AB8", "D8343C", "1A1A1A", "E8E4D0"]), top="tank", topc=col(r, ["black", "charcoal", "maroon", "white"]),
                motif=pick(r, ["flame", "star", "crown", "moon"]), motifc=col(r, ["red", "white", "yellow", "pink"]), coat="leather", coatc=col(r, ["black", "brown", "maroon"]), bottom="jeans", botc=col(r, ["black", "charcoal"]),
                wristband=col(r, ["grey", "red", "black"]), earrings=col(r, ["white", "yellow"]) if f else None, shoec="1E1E24")
def k_sportsfan(r, f):
    c = col(r, ["red", "blue", "green", "yellow", "orange", "purple", "navy", "maroon", "teal"])
    return dict(top="tee", topc=col(r, ["white", "cream", "grey"]), sleevec=c, motif=pick(r, ["seven", "chevron", "star", "ball"]), motifc=c, motif2=col(r, ["white", "red"]), bottom=pick(r, ["jeans", "trousers"]), jog=True,
                stripec=c, botc=col(r, ["navy", "black", "charcoal", "grey"]), hw=pick(r, ["cap", "capback"]), hwc=c, scarf=c if r.random() < 0.5 else None)

KINDS = [("SHOPPER", "Shops and markets", k_shopper), ("COMMUTER", "Station and streets", k_commuter), ("STUDENT", "Near the school and the library", k_student), ("SKATER", "The park, the square", k_skater),
         ("GAMER", "The game shop, the GAME CORNER", k_gamer), ("ARTIST", "The square, the museum steps", k_artist), ("RUNNER", "Laps of the park", k_runner), ("CYCLIST", "Roads and CYCLING ROAD", k_cyclist),
         ("TOURIST", "Sights, the harbour, the museum", k_tourist), ("MUSIC FAN", "Outside the theatre", k_musicfan), ("BOOKWORM", "The library, benches", k_bookworm), ("CAFÉ REGULAR", "Cafés and corners", k_cafe),
         ("OFFICE WORKER", "SAFFRON and CELADON", k_office), ("PARK VISITOR", "The park", k_park), ("GYM FAN", "Outside the gym", k_gymfan), ("BEACHGOER", "Beaches and the harbour", k_beach),
         ("WALKER", "Routes and paths", k_walker), ("KID", "Playgrounds and streets", k_kid), ("TEEN", "Hanging round the centre", k_teen), ("GRANDPARENT", "Benches and the park", k_grand),
         ("PARENT", "Streets and the park", k_parent), ("FASHION FAN", "The DEPT. STORE, the square", k_fashion), ("MARKET TRADER", "The market", k_trader), ("ROCKER", "Outside the theatre, the square", k_rocker),
         ("SPORTS FAN", "On the way to the match", k_sportsfan)]


def crowd():
    out = []
    n = 0
    for ki, (name, where, fn) in enumerate(KINDS):
        for j in range(4):
            n += 1
            r = random.Random(1000 + n * 7919)
            fem = j % 2 == 1
            sp = base(r, fem, n)
            sp.update({k: v for k, v in fn(r, fem).items() if v is not None})
            if sp.get("hw") == "hood" and sp.get("top") != "hoodie": sp.pop("hw")
            if sp.get("hw") and sp.get("hair") in ("afro",) and sp["hw"] in ("cap", "capback", "bucket"): sp["hair"] = "short" if not fem else "ponytail"
            if sp.get("jog"): sp["bottom"] = "trousers"
            if fem and sp.get("bottom") == "skirt": sp.setdefault("socks", None)
            sp = {k: v for k, v in sp.items() if v is not None}
            key = f"crowd_{n:03d}"
            out.append((key, sp, name, describe(sp), where))
    return out


# ---------- more looks for the trainer classes: changes on their usual kit ----------
TRAINER_MORE = [
    ("youngster", "YOUNGSTER", [dict(tone="dark", hair="buzz", hairc="1A1A1A", topc="3F9A4A", motif="bolt", motifc="F2C12E", botc="243A6A", hw="capback", hwc="D8343C", hat=None),
                                dict(tone="pale", hair="spiky", hairc="D87A3A", top="hoodie", topc="F28A2A", botc="4A5F8A", hw="bucket", hwc="F2E8D0", hat=None, freckles=True)]),
    ("lass", "LASS", [dict(tone="deep", hair="braids", hairc="1A1A1A", topc="7FD8B0", botc="7A4AB8", hw="headband", hwc="F28AB2"),
                      dict(tone="light", hair="ponytail", hairc="E8C870", top="jumper", topc="B89AE0", pattern="dots", patc="F4F4F2", botc="243A6A", tiec="F28AB2")]),
    ("bug_catcher", "BUG CATCHER", [dict(tone="brown", hair="short", hairc="1A1A1A", topc="9ACD3A", motif="leaf", motifc="2E5E3A", hatc="F2C12E"),
                                    dict(tone="fair", hair="swept", hairc="E8C870", top="shirt", topc="F2E8D0", pattern="check", patc="3F9A4A", hat=None, hw="bucket", hwc="6E7A3A")]),
    ("camper", "CAMPER", [dict(tone="medium", hair="spiky", hairc="3A2A20", topc="2A9A92", coat="vest", puffer=True, closed=True, coatsleeve=1, coatc="F28A2A", hat=None, hw="capback", hwc="2E5E3A"),
                          dict(tone="pale", hair="short", hairc="A85A2A", top="shirt", topc="D8343C", pattern="check", patc="1E1E24", hat="beanie", hatc="6E7A3A")]),
    ("picnicker", "PICNICKER", [dict(tone="dark", hair="afro", hairc="1A1A1A", topc="F2C12E", motif="flower", motifc="D8343C", hat=None, hw="headband", hwc="F28AB2"),
                                dict(tone="fair", hair="bob", hairc="6A4428", top="blouse", topc="F28AB2", pattern="dots", patc="F4F4F2", botc="F2E8D0", hatc="F2E8D0")]),
    ("hiker", "HIKER", [dict(tone="brown", hair="short", hairc="1A1A1A", topc="D8343C", coat="jacket", puffer=True, closed=True, coatc="F28A2A", hatc="243A6A"),
                        dict(tone="pale", hair="bald", hairc="A8A8A8", beard=True, beardc="D87A3A", topc="2E5E3A", pattern="check", patc="F2E8D0", hat=None, hw="bucket", hwc="A8946A")]),
    ("swimmer", "SWIMMER", [dict(tone="pale", hair="short", hairc="E8C870", botc="F2C12E"), dict(tone="dark", hair="buzz", hairc="1A1A1A", botc="D8343C", shades=True)]),
    ("fisher", "FISHER", [dict(tone="deep", hair="short", hairc="1A1A1A", topc="F2C12E", coatc="2E5E3A", hatc="243A6A", beard=False),
                          dict(tone="light", hair="swept", hairc="A8A8A8", topc="3A6FD0", pattern="check", patc="F4F4F2", coat="vest", puffer=True, closed=True, coatsleeve=1, coatc="D8343C", hat=None, hw="bucket", hwc="6E7A3A")]),
    ("bird_keeper", "BIRD KEEPER", [dict(tone="medium", hair="ponytail", hairc="3A2A20", fem=True, topc="6AB8E8", motif="wave", motifc="F4F4F2", hatc="243A6A"),
                                    dict(tone="fair", hair="short", hairc="8A4A2A", topc="F2E8D0", coat="jacket", coatc="6E7A3A", hat=None, hw="bucket", hwc="6E7A3A", glasses=True)]),
    ("black_belt", "BLACK BELT", [dict(tone="pale", hair="spiky", hairc="1A1A1A", hw="headband", hwc="D8343C"), dict(tone="tan", hair="bald", hairc="3A2A20", belt="7A5030")]),
    ("cooltrainer_m", "COOLTRAINER", [dict(tone="dark", hair="buzz", hairc="1A1A1A", coatc="7A4AB8", soles="7A4AB8", wristband="7A4AB8", closed=True),
                                      dict(tone="fair", hair="swept", hairc="E8E4D0", coatc="D8343C", soles="D8343C", wristband="D8343C", coatsleevec="F4F4F2", closed=True)]),
    ("cooltrainer_f", "COOLTRAINER", [dict(tone="deep", hair="braids", hairc="1A1A1A", coatc="7A4AB8", tiec="7A4AB8", closed=True),
                                      dict(tone="light", hair="bob", hairc="3A6FD0", coatc="F2C12E", tiec="F2C12E", closed=True, coatsleevec="1E1E24")]),
    ("super_nerd", "SUPER NERD", [dict(tone="brown", hair="afro", hairc="1A1A1A", topc="F2E8D0", pattern="vstripe", patc="3A6FD0", tie=None, sleevec=None),
                                  dict(tone="fair", hair="short", hairc="D87A3A", top="jumper", topc="2A9A92", pattern="yoke", patc="F2C12E", tie=None, freckles=True)]),
    ("pokemaniac", "POKéMANIAC", [dict(tone="tan", hair="spiky", hairc="3A2A20", topc="7A4AB8", motif="ball", motifc="D8343C", motif2="F4F4F2", hw="hood", hwc="7A4AB8"),
                                  dict(tone="pale", hair="swept", hairc="1A1A1A", topc="F28A2A", motif="star", motifc="F2C12E", phones="1E1E24")]),
]
NEW_TRAINERS = [
    ("school_kid", "SCHOOL KID", "Straight out of the TRAINER SCHOOL.", "Routes near VIRIDIAN", ["pidgey", "rattata"],
     dict(tone="light", hair="short", hairc="3A2A20", top="shirt", topc="F4F4F2", coat="blazer", coatc="243A6A", tie="D8343C", bottom="shorts", botc="3A3C44", socks="F4F4F2", shoec="1E1E24", bag="backpack", bagc="D8343C")),
    ("school_kid_f", "SCHOOL KID", "Straight out of the TRAINER SCHOOL.", "Routes near VIRIDIAN", ["nidoran_f", "pidgey"],
     dict(tone="deep", fem=True, hair="pigtails", hairc="1A1A1A", tiec="D8343C", top="shirt", topc="F4F4F2", coat="blazer", coatc="243A6A", tie="D8343C", bottom="skirt", botc="3A3C44", socks="F4F4F2", shoec="1E1E24", bag="backpack", bagc="F2C12E")),
    ("pokefan", "POKéFAN", "Loves every POKéMON, and dresses to show it.", "Cities, the FAN CLUB", ["clefairy", "jigglypuff"],
     dict(tone="fair", hair="short", hairc="6A4428", top="hoodie", topc="F2C12E", motif="ball", motifc="D8343C", motif2="F4F4F2", bottom="trousers", jog=True, botc="243A6A", hw="cap", hwc="D8343C", hwlogo="F4F4F2", bag="backpack", bagc="F2C12E")),
    ("pokefan_f", "POKéFAN", "Loves every POKéMON, and dresses to show it.", "Cities, the FAN CLUB", ["pikachu", "clefairy"],
     dict(tone="medium", fem=True, hair="bun", hairc="3A2A20", top="jumper", topc="F28AB2", motif="heart", motifc="D8343C", bottom="skirt", botc="7A4AB8", hw="headband", hwc="F2C12E", socks="F4F4F2")),
    ("guitarist", "GUITARIST", "Plays for anyone who'll listen. Battles to the beat.", "CELADON, ROUTE 12", ["voltorb", "electabuzz"],
     dict(tone="tan", hair="long", hairc="1A1A1A", top="tank", topc="1E1E24", motif="note", motifc="F2C12E", coat="leather", coatc="7A2A3A", bottom="jeans", botc="1E1E24", wristband="8A8E96", shoec="1E1E24")),
    ("sightseer", "SIGHTSEER", "Here for the view. Will battle you for the best spot.", "ROUTES 11, 20, VERMILION", ["psyduck", "pidgey"],
     dict(tone="pale", hair="short", hairc="D87A3A", top="shirt", topc="6AB8E8", pattern="dots", patc="F4F4F2", bottom="shorts", botc="C8A87A", hw="bucket", hwc="F2E8D0", bag="backpack", bagc="3F9A4A", socks="F4F4F4", shades=True)),
    ("sightseer_f", "SIGHTSEER", "Here for the view. Will battle you for the best spot.", "ROUTES 11, 20, VERMILION", ["oddish", "spearow"],
     dict(tone="brown", fem=True, hair="ponytail", hairc="1A1A1A", tiec="F2C12E", top="tee", topc="F2E8D0", motif="flower", motifc="F28AB2", bottom="shorts", botc="4A5F8A", hw="bucket", hwc="F2C12E", bag="satchel", bagc="7A5030")),
    ("athlete", "ATHLETE", "Trains every morning, and so do their POKéMON.", "ROUTES 1 to 3, the park", ["machop", "rattata"],
     dict(tone="dark", hair="buzz", hairc="1A1A1A", top="tank", topc="3A6FD0", bottom="shorts", botc="1E1E24", hw="headband", hwc="F4F4F2", wristband="F4F4F2", shoec="F4F4F2", soles="3A6FD0")),
    ("athlete_f", "ATHLETE", "Trains every morning, and so do their POKéMON.", "ROUTES 1 to 3, the park", ["nidoran_f", "pidgey"],
     dict(tone="fair", fem=True, hair="ponytail", hairc="A85A2A", tiec="F2C12E", top="tank", topc="F28A2A", bottom="trousers", jog=True, stripec="F4F4F2", botc="1E1E24", wristband="F4F4F2", shoec="F4F4F2", soles="F28A2A")),
]
# Team Rocket: grunts in different kit, all black with the red mark; JESSIE and JAMES the other way round (red, the mark in black).
RK = "1E1E24"; RR = "D8343C"
ROCKET_MORE = [
    dict(tone="light", hair="short", hairc="1A1A1A", top="tee", topc=RK, coat="jacket", closed=True, coatc=RK, bigmark=RR, bottom="trousers", botc=RK, hat="beanie", hatc=RK, gloves=RR, shoec=RK),
    dict(tone="medium", fem=True, hair="ponytail", hairc=RR, tiec=RK, top="tee", topc=RK, letter=RR, mark=skins2.EMBLEM, coat="vest", puffer=True, closed=False, coatsleeve=1, coatc="3A3C44", bottom="skirt", botc=RK, socks=RK, shoec=RK, gloves=RR),
    dict(tone="tan", hair="spiky", hairc="3A2A20", top="hoodie", topc=RK, bigmark=RR, hw="hood", hwc=RK, bottom="trousers", jog=True, stripec=RR, botc=RK, shoec=RK),
    dict(tone="pale", fem=True, hair="bob", hairc="7A4AB8", top="tee", topc=RK, coat="jacket", puffer=True, closed=True, coatc=RK, bigmark=RR, hw="beret", hwc=RK, bottom="trousers", botc=RK, shoec=RK, gloves=RR),
    dict(tone="deep", hair="buzz", hairc="1A1A1A", top="tee", topc=RK, letter=RR, mark=skins2.EMBLEM, coat="leather", coatc="3A3C44", bottom="trousers", botc=RK, hw="cap", hwc=RK, hwlogo=RR, shoec=RK),
    dict(tone="fair", fem=True, hair="bun", hairc="3A2A20", top="tee", topc=RK, letter=RR, mark=skins2.EMBLEM, coat="blazer", coatlen=15, coatc=RK, bottom="trousers", botc=RK, shades=True, shoec=RK, gloves=RR),
    dict(tone="brown", hair="bald", hairc="1A1A1A", top="tank", topc=RK, letter=RR, mark=skins2.EMBLEM, bottom="combat", botc="3A3C44", shades=True, wristband=RR, shoec=RK),
    dict(tone="light", hair="swept", hairc="D87A3A", top="tee", topc=RK, sleevec=RR, letter=RR, mark=skins2.EMBLEM, bottom="trousers", botc=RK, hw="bucket", hwc=RK, shoec=RK),
]
STAFF_MORE = [
    ("nurse_2", "NURSE", "A centre nurse, healing your team.", "Every POKéMON CENTER", dict(PEOPLE["nurse"], tone="deep", hair="ponytail", hairc="1A1A1A", eyes="dark", tiec="FFFFFF")),
    ("nurse_m2", "NURSE", "A centre nurse, healing your team.", "Bigger POKéMON CENTERs", dict(tone="light", hair="short", hairc="D87A3A", eyes="green", top="shirt", topc="9ED8C8", collarc="FFFFFF", bottom="trousers", botc="7AC0B0",
                                                                                          hat="nurse", hatc="FFFFFF", crossc="2AA58A", shoec="F8F8F8", badge="2AA58A", freckles=True)),
    ("officer_f2", "POLICE OFFICER", "On the beat, in the bigger cities.", "Cities", dict(PEOPLE["officer_m"], tone="light", fem=True, hair="ponytail", hairc="E8C870", tiec="243A6A", eyes="blue")),
    ("officer_m2", "POLICE OFFICER", "On the beat, in the bigger cities.", "Cities", dict(PEOPLE["officer_m"], tone="dark", hair="buzz", hairc="1A1A1A", eyes="dark", moustache=True, beardc="1A1A1A")),
    ("clerk_2", "CLERK", "Behind the counter in every POKé MART.", "Every POKé MART", dict(PEOPLE["clerk"], tone="deep", hair="spiky", hairc="1A1A1A", eyes="dark", glasses=True)),
    ("clerk_f2", "CLERK", "Behind the counter in every POKé MART.", "Every POKé MART, the DEPT. STORE", dict(PEOPLE["clerk_f"], tone="fair", hair="bun", hairc="A85A2A", eyes="green")),
]


def main():
    os.makedirs(OUT, exist_ok=True)
    rows, groups = [], [["crowd", "Busy places", "Extra people for the parks, the museum and the big cities: every skin tone, all sorts of clothes."]]
    def paint(key, sp, seed): person(sp, seed).save(os.path.join(OUT, key + ".png"))
    for i, (key, sp, name, desc, where) in enumerate(crowd()):
        paint(key, sp, 5000 + i); rows.append(["crowd", "G", key, name, desc, where, []])
    for base_key, name, vs in TRAINER_MORE:
        b = PEOPLE[base_key]; meta = None
        for j, v in enumerate(vs):
            sp = dict(b); sp.update(v); sp = {k: x for k, x in sp.items() if x is not None}
            key = f"{base_key}_{j + 2}"; paint(key, sp, 7000 + len(rows))
            rows.append(["trainers", "G", key, name, "Another look: " + describe(sp)[0].lower() + describe(sp)[1:], "Wherever the others are", []])
    for key, name, who, where, team, sp in NEW_TRAINERS:
        paint(key, sp, 8000 + len(rows)); rows.append(["trainers", "G", key, name, who, where, team])
    for j, sp in enumerate(ROCKET_MORE):
        key = f"rocket_{j + 2}"; paint(key, sp, 9000 + j)
        rows.append(["rocket", "G", key, "ROCKET GRUNT", "In black with the red mark: " + describe(sp)[0].lower() + describe(sp)[1:], "MT. MOON, CELADON, LAVENDER, SAFFRON", ["rattata", "zubat", "koffing"][: 2 + j % 2]])
    for key, name, who, where, sp in STAFF_MORE:
        paint(key, sp, 9500 + len(rows)); rows.append(["helpers", "G", key, name, who, where, []])
    # JESSIE and JAMES: red jackets done up, the mark in black, so they stand out from the grunts.
    for key, seed in (("jessie", 41), ("james", 42)): paint(key, PEOPLE[key], seed)
    js = "// More people (tools/crowd.py): 100 townspeople for busy places, more trainer looks, Team Rocket grunts, and more staff.\n"
    js += "GROUPS.splice(GROUPS.findIndex(g => g[0] === \"folk\") + 1, 0, ..." + json.dumps(groups, ensure_ascii=False) + ");\n"
    js += "PEOPLE.push(\n" + ",\n".join("  " + json.dumps(r, ensure_ascii=False) for r in rows) + "\n);\n"
    js += 'for (const k of ["jessie", "james"]) { const p = PEOPLE.find(q => q[2] === k); if (p) p[4] = p[4] + " Red jacket, the mark in black: the grunts wear it the other way round."; }\n'
    open(os.path.join(HERE, "personviewer", "more.js"), "w").write(js)
    print(len(rows), "more people painted")


if __name__ == "__main__":
    main()
