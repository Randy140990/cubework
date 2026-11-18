"""The signs of Kanto, as Yellow has them: where each stands on its map (x, y in steps) and what it says
(PALLET TOWN / Shades of your journey await!, the TRAINER TIPS, the Mart and Pokémon Center signs...).
Writes src/main/resources/retromon/data/signs.tsv (key, place, x, y, lines with | between pages)."""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pret_import as P

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "retromon", "data", "signs.tsv")


def main():
    texts = P.text_blocks(P.YELLOW)
    rows = []
    objdir = os.path.join(P.YELLOW, "data/maps/objects")
    for f in sorted(os.listdir(objdir)):
        mapname = f[:-4]
        obj = P.read(os.path.join(objdir, f))
        m = re.search(r"def_warps_to (\w+)", obj)
        place = P.ident(m.group(1) if m else "")
        if not place:
            continue
        scripts = P.script_texts(P.YELLOW, mapname)
        sp = os.path.join(P.YELLOW, "scripts", mapname + ".asm")
        sc = P.read(sp) if os.path.exists(sp) else ""
        tptr = dict((c, l) for l, c in re.findall(r"dw_const (\w+),\s*(TEXT_\w+)", sc))
        n = 0
        for bm in re.finditer(r"bg_event\s+(\d+),\s*(\d+),\s*(TEXT_\w+)", obj):
            x, y, tconst = bm.groups()
            lines = P.resolve(tptr.get(tconst, ""), scripts, texts)
            if not lines:
                continue
            lines = [re.sub(r" -(\w)", r" - \1", l) for l in lines]
            rows.append((f"{place}_s{n}", place, x, y, " | ".join(lines)))
            n += 1
    with open(OUT, "w", encoding="utf-8") as out:
        out.write("# key\tplace\tx\ty\tlines\n")
        for r in rows:
            out.write("\t".join(r) + "\n")
    print(len(rows), "signs")


if __name__ == "__main__":
    main()
