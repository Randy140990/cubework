R = "/home/claude/route-one/tools/"


def sub(path, old, new):
    s = open(path).read()
    assert s.count(old) == 1, (path, old[:70])
    open(path, "w").write(s.replace(old, new))


# The S.S. ANNE: its kept blocks only those not retextured now.
sub(R + "arealooks59c.py", '''    anne = json.load(open(os.path.join(HERE, "area_anne59.json")))
    for x in range(-450, -265, 16):
        b = (x, 619, min(-266, x + 15), 662, "the S.S. ANNE, as John built it")
        add("keep", ",".join(anne["low"]), b, "-", ys(21, 37))
        add("keep", ",".join(anne["high"]), b, "-", ys(38, 70))''', '''    # ((0.67.1, John's 17:13: "both ships obviously get the retouch") Retextured now, block for block (arealooks67 and
    # tools/shiplooks.py); kept as John built them only the things the game draws apart from the blocks (signs, banners,
    # beds, heads) and the leaves, webs and fans.)
    done = set(b for r in json.load(open(os.path.join(HERE, "area_ships67.json")))["rows"] for b in r["blocks"].split(","))
    anne = json.load(open(os.path.join(HERE, "area_anne59.json")))
    for x in range(-450, -265, 16):
        b = (x, 619, min(-266, x + 15), 662, "the S.S. ANNE, as John built it")
        add("keep", ",".join(k for k in anne["low"] if k not in done), b, "-", ys(21, 37))
        add("keep", ",".join(k for k in anne["high"] if k not in done), b, "-", ys(38, 70))''')
sub(R + "arealooks59b.py", '''    ship = ",".join(json.load(open(os.path.join(HERE, "area_ship59.json"))))''', '''    # ((0.67.1, 17:13) Retextured now, as the S.S. ANNE (arealooks67): kept only what isn't.)
    done = set(b for r in json.load(open(os.path.join(HERE, "area_ships67.json")))["rows"] for b in r["blocks"].split(","))
    ship = ",".join(k for k in json.load(open(os.path.join(HERE, "area_ship59.json"))) if k not in done)''')
sub(R + "arealooks67.py", '''    # The buttons on the stones''', '''    # (0.67.1, John's 17:13) Both ships retextured: the S.S. ANNE and the S.S. AQUA, every kind of block in its own
    # colour in our looks (tools/shiplooks.py made the pictures and these rows), in slices along each ship small
    # enough to win over the places round them.
    for r in json.load(open(os.path.join(HERE, "area_ships67.json")))["rows"]:
        x0, z0, x1, z1 = r["box"]
        add("swap", r["blocks"], (x0, z0, x1, z1, r["note"]), r["spec"], ys(*r["ys"]))

    # The buttons on the stones''')
print("ok")
