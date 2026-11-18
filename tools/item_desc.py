"""The bag's item descriptions, from Crystal's (pret/pokecrystal data/items/descriptions.asm), matched to the item
ids by the order of the item constants. Writes src/main/resources/retromon/data/item_desc.tsv (id, text)."""
import os, re

PRET = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "pret", "pokecrystal")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "retromon", "data", "item_desc.tsv")

consts = []
for line in open(os.path.join(PRET, "constants", "item_constants.asm")):
    m = re.match(r"\s*const (\w+)", line)
    if m:
        consts.append(m.group(1).lower())
    if "NUM_ITEMS" in line or "const_def" in line and consts:
        pass
consts = [c for c in consts if c != "no_item"]
src = open(os.path.join(PRET, "data", "items", "descriptions.asm")).read()
order = re.findall(r"dw (\w+Desc)", src)
texts = {}
for m in re.finditer(r"(\w+Desc):\s*\n((?:\s*(?:db|next)\s+\"[^\"]*\"\s*\n)+)", src):
    parts = re.findall(r"\"([^\"]*)\"", m.group(2))
    out = ""
    for p in parts:
        p = p.replace("@", "")
        if out.endswith("-"):
            out = out[:-1] + p
        else:
            out = (out + " " + p).strip()
    texts[m.group(1)] = out.replace("#MON", "POKéMON").replace("#", "POKé").replace("<PK><MN>", "POKéMON").replace("'", "'")
rows = []
for i, label in enumerate(order):
    if i < len(consts) and label in texts and texts[label] and texts[label] != "?":
        rows.append((consts[i], texts[label]))
with open(OUT, "w", encoding="utf-8") as f:
    f.write("# id\ttext\n")
    for k, v in rows:
        f.write(f"{k}\t{v}\n")
print(len(rows), "descriptions")
