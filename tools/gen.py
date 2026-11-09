#!/usr/bin/env python3
"""
Writes every Cubework resource: blockstates, block and item models, item definitions, loot tables,
stonecutter recipes, tags and names. Run from the repo root after changing the material or shape lists
(they must match Material.java and Shape.java):  python3 tools/gen.py

Models point straight at the vanilla textures (minecraft:block/red_wool and so on), so a texture pack
that changes a block changes all of its shapes too.
"""
import json
import os
import shutil

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "cubework")
DATA = os.path.join(ROOT, "data")
TEMPLATES = os.path.join(os.path.dirname(__file__), "templates")
NS = "cubework"

COLOURS = ["white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan",
           "light_blue", "blue", "purple", "magenta", "pink"]

# (id, kind), in the same order as Material.java
MATERIALS = [(c + "_wool", "wool") for c in COLOURS] + [("terracotta", "terracotta")] + \
            [(c + "_terracotta", "terracotta") for c in COLOURS] + [(c + "_concrete", "concrete") for c in COLOURS] + \
            [(c + "_concrete_powder", "powder") for c in COLOURS] + [(c + "_glazed_terracotta", "glazed") for c in COLOURS] + \
            [("glass", "glass"), ("tinted_glass", "glass")] + [(c + "_stained_glass", "glass") for c in COLOURS]

# (suffix, English name, how many one block makes in the stonecutter), in the order of Shape.java
SHAPES = [("stairs", "Stairs", 1), ("slab", "Slab", 2), ("vertical_slab", "Vertical Slab", 2), ("slope", "Slope", 2),
          ("layer", "Layer", 8), ("vertical_layer", "Vertical Layer", 8), ("carpet", "Carpet", 4), ("panel", "Panel", 4),
          ("post", "Post", 4), ("wall", "Wall", 1), ("fence", "Fence", 1), ("fence_gate", "Fence Gate", 1),
          ("trapdoor", "Trapdoor", 1), ("button", "Button", 4), ("pressure_plate", "Pressure Plate", 2)]

TABS = [("wool", "Wool"), ("terracotta", "Terracotta"), ("concrete", "Concrete"), ("concrete_powder", "Concrete Powder"),
        ("glazed_terracotta", "Glazed Terracotta"), ("glass", "Glass")]

HORIZONTAL = ["north", "east", "south", "west"]
# Rotation (y) that turns a model made facing north to face each way.
Y_FROM_NORTH = {"north": 0, "east": 90, "south": 180, "west": 270}


def applies(mat, kind, suffix):
    return not (suffix == "carpet" and kind == "wool")


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


def tex(mat):
    return "minecraft:block/" + mat


def m(name):
    return NS + ":block/" + name


def model(name, parent, textures):
    write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": parent, "textures": textures})


def blockstate(name, obj):
    write(os.path.join(ASSETS, "blockstates", name + ".json"), obj)


def item(name, model_id):
    write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": model_id}})


def from_template(template, values):
    with open(os.path.join(TEMPLATES, template + ".json")) as f:
        s = f.read()
    for k, v in values.items():
        s = s.replace('"{' + k + '}"', json.dumps(v))
    return json.loads(s)


# ---------- our own template models (all use one texture, #all) ----------

def face(cull=None):
    f = {"texture": "#all"}
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, culls):
    """An element; culls lists the faces that sit on the block's edge (and so can be hidden)."""
    faces = {}
    for d in ["down", "up", "north", "south", "west", "east"]:
        faces[d] = face(d if d in culls else None)
    return {"from": frm, "to": to, "faces": faces}


def edge_faces(frm, to):
    """The faces of a box that touch the block's edge."""
    out = []
    if frm[1] == 0: out.append("down")
    if to[1] == 16: out.append("up")
    if frm[2] == 0: out.append("north")
    if to[2] == 16: out.append("south")
    if frm[0] == 0: out.append("west")
    if to[0] == 16: out.append("east")
    return out


def template(name, boxes, display=None):
    obj = {"parent": "minecraft:block/block", "textures": {"particle": "#all"},
           "elements": [box(f, t, edge_faces(f, t)) for f, t in boxes]}
    if display:
        obj["display"] = display
    write(os.path.join(ASSETS, "models", "block", name + ".json"), obj)


STAIRS_DISPLAY = {"gui": {"rotation": [30, 135, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                  "head": {"rotation": [0, -90, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
                  "thirdperson_lefthand": {"rotation": [75, -135, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]}}
THIN_DISPLAY = {"thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 2], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4.2, 0], "scale": [0.4, 0.4, 0.4]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 4.2, 0], "scale": [0.4, 0.4, 0.4]}}


def templates():
    template("template_vertical_slab", [([0, 0, 0], [16, 16, 8])])
    for n in range(1, 8):
        template("template_layer_%d" % n, [([0, 0, 0], [16, 2 * n, 16])], THIN_DISPLAY if n <= 2 else None)
        # Grows south, from the north wall.
        template("template_vertical_layer_%d" % n, [([0, 0, 0], [16, 16, 2 * n])])
    template("template_panel", [([0, 0, 0], [16, 16, 1])])
    template("template_post", [([5, 0, 5], [11, 16, 11])])
    # The slope in the hand and the inventory: eight small steps (the placed slope is smooth).
    template("template_slope_item", [([0, 0, 2 * k], [16, 16 - 2 * k, 2 * k + 2]) for k in range(8)], STAIRS_DISPLAY)


# ---------- per shape ----------

def stairs(mat, name):
    t = {"bottom": tex(mat), "top": tex(mat), "side": tex(mat)}
    model(name, "minecraft:block/stairs", t)
    model(name + "_inner", "minecraft:block/inner_stairs", t)
    model(name + "_outer", "minecraft:block/outer_stairs", t)
    blockstate(name, from_template("stairs", {"straight": m(name), "inner": m(name + "_inner"), "outer": m(name + "_outer")}))
    item(name, m(name))


def slab(mat, name):
    t = {"bottom": tex(mat), "top": tex(mat), "side": tex(mat)}
    model(name, "minecraft:block/slab", t)
    model(name + "_top", "minecraft:block/slab_top", t)
    blockstate(name, {"variants": {"type=bottom": {"model": m(name)}, "type=top": {"model": m(name + "_top")},
                                   "type=double": {"model": "minecraft:block/" + mat}}})
    item(name, m(name))


def vertical_slab(mat, name):
    model(name, m("template_vertical_slab"), {"all": tex(mat)})
    v = {}
    for d in HORIZONTAL:
        v["double=false,facing=" + d] = {"model": m(name), "y": Y_FROM_NORTH[d], "uvlock": True}
        v["double=true,facing=" + d] = {"model": "minecraft:block/" + mat}
    blockstate(name, {"variants": v})
    item(name, m(name))


def slope(mat, name):
    # The placed slope's model is built in code (SlopeModel); this one is for the item.
    model(name + "_item", m("template_slope_item"), {"all": tex(mat)})
    item(name, m(name + "_item"))


def layer(mat, name):
    v = {}
    for n in range(1, 8):
        model(name + "_height%d" % (2 * n), m("template_layer_%d" % n), {"all": tex(mat)})
        v["layers=%d" % n] = {"model": m(name + "_height%d" % (2 * n))}
    v["layers=8"] = {"model": "minecraft:block/" + mat}
    blockstate(name, {"variants": v})
    item(name, m(name + "_height2"))


def vertical_layer(mat, name):
    v = {}
    grows_y = {"south": 0, "west": 90, "north": 180, "east": 270}
    for n in range(1, 8):
        model(name + "_depth%d" % (2 * n), m("template_vertical_layer_%d" % n), {"all": tex(mat)})
    for d in HORIZONTAL:
        for n in range(1, 8):
            v["facing=%s,layers=%d" % (d, n)] = {"model": m(name + "_depth%d" % (2 * n)), "y": grows_y[d], "uvlock": True}
        v["facing=%s,layers=8" % d] = {"model": "minecraft:block/" + mat}
    blockstate(name, {"variants": v})
    item(name, m(name + "_depth4"))


def carpet(mat, name):
    model(name, "minecraft:block/carpet", {"wool": tex(mat)})
    blockstate(name, {"variants": {"": {"model": m(name)}}})
    item(name, m(name))


def panel(mat, name):
    model(name, m("template_panel"), {"all": tex(mat)})
    blockstate(name, {"variants": {"facing=" + d: {"model": m(name), "y": Y_FROM_NORTH[d], "uvlock": True} for d in HORIZONTAL}})
    item(name, m(name))


def post(mat, name):
    model(name, m("template_post"), {"all": tex(mat)})
    blockstate(name, {"variants": {"axis=y": {"model": m(name)}, "axis=z": {"model": m(name), "x": 90, "uvlock": True},
                                   "axis=x": {"model": m(name), "x": 90, "y": 90, "uvlock": True}}})
    item(name, m(name))


def wall(mat, name):
    t = {"wall": tex(mat)}
    model(name + "_post", "minecraft:block/template_wall_post", t)
    model(name + "_side", "minecraft:block/template_wall_side", t)
    model(name + "_side_tall", "minecraft:block/template_wall_side_tall", t)
    model(name + "_inventory", "minecraft:block/wall_inventory", t)
    blockstate(name, from_template("wall", {"post": m(name + "_post"), "side": m(name + "_side"), "side_tall": m(name + "_side_tall")}))
    item(name, m(name + "_inventory"))


def fence(mat, name):
    t = {"texture": tex(mat)}
    model(name + "_post", "minecraft:block/fence_post", t)
    model(name + "_side", "minecraft:block/fence_side", t)
    model(name + "_inventory", "minecraft:block/fence_inventory", t)
    blockstate(name, from_template("fence", {"post": m(name + "_post"), "side": m(name + "_side")}))
    item(name, m(name + "_inventory"))


def fence_gate(mat, name):
    t = {"texture": tex(mat)}
    model(name, "minecraft:block/template_fence_gate", t)
    model(name + "_open", "minecraft:block/template_fence_gate_open", t)
    model(name + "_wall", "minecraft:block/template_fence_gate_wall", t)
    model(name + "_wall_open", "minecraft:block/template_fence_gate_wall_open", t)
    blockstate(name, from_template("fence_gate", {"closed": m(name), "open": m(name + "_open"), "wall": m(name + "_wall"),
                                                  "wall_open": m(name + "_wall_open")}))
    item(name, m(name))


def trapdoor(mat, name):
    t = {"texture": tex(mat)}
    model(name + "_bottom", "minecraft:block/template_trapdoor_bottom", t)
    model(name + "_top", "minecraft:block/template_trapdoor_top", t)
    model(name + "_open", "minecraft:block/template_trapdoor_open", t)
    blockstate(name, from_template("trapdoor", {"bottom": m(name + "_bottom"), "top": m(name + "_top"), "open": m(name + "_open")}))
    item(name, m(name + "_bottom"))


def button(mat, name):
    t = {"texture": tex(mat)}
    model(name, "minecraft:block/button", t)
    model(name + "_pressed", "minecraft:block/button_pressed", t)
    model(name + "_inventory", "minecraft:block/button_inventory", t)
    blockstate(name, from_template("button", {"button": m(name), "pressed": m(name + "_pressed")}))
    item(name, m(name + "_inventory"))


def pressure_plate(mat, name):
    t = {"texture": tex(mat)}
    model(name, "minecraft:block/pressure_plate_up", t)
    model(name + "_down", "minecraft:block/pressure_plate_down", t)
    blockstate(name, from_template("pressure_plate", {"up": m(name), "down": m(name + "_down")}))
    item(name, m(name))


BUILDERS = {"stairs": stairs, "slab": slab, "vertical_slab": vertical_slab, "slope": slope, "layer": layer,
            "vertical_layer": vertical_layer, "carpet": carpet, "panel": panel, "post": post, "wall": wall,
            "fence": fence, "fence_gate": fence_gate, "trapdoor": trapdoor, "button": button, "pressure_plate": pressure_plate}


# ---------- loot ----------

def loot(name, kind, suffix):
    entry = {"type": "minecraft:item", "name": NS + ":" + name, "functions": []}
    block_id = NS + ":" + name
    if suffix in ("slab", "vertical_slab"):
        prop = {"type": "double"} if suffix == "slab" else {"double": "true"}
        entry["functions"].append({"function": "minecraft:set_count", "add": False, "count": 2.0,
                                   "conditions": [{"condition": "minecraft:block_state_property", "block": block_id, "properties": prop}]})
    if suffix in ("layer", "vertical_layer"):
        for n in range(2, 8):
            entry["functions"].append({"function": "minecraft:set_count", "add": False, "count": float(n),
                                       "conditions": [{"condition": "minecraft:block_state_property", "block": block_id,
                                                       "properties": {"layers": str(n)}}]})
    entry["functions"].append({"function": "minecraft:explosion_decay"})
    pool = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry]}
    if kind == "glass":
        # Like glass: it only drops with Silk Touch.
        pool["conditions"] = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]
    write(os.path.join(DATA, NS, "loot_table", "blocks", name + ".json"),
          {"type": "minecraft:block", "pools": [pool], "random_sequence": NS + ":blocks/" + name})


def recipe(mat, name, count):
    write(os.path.join(DATA, NS, "recipe", name + "_from_" + mat + "_stonecutting.json"),
          {"type": "minecraft:stonecutting", "ingredient": "minecraft:" + mat, "result": {"id": NS + ":" + name, "count": count}})


# ---------- names ----------

def title(words, grey):
    s = " ".join(w.capitalize() for w in words.split("_"))
    return s.replace("Gray", "Grey") if grey else s


def main():
    for sub in ["blockstates", "models", "items"]:
        shutil.rmtree(os.path.join(ASSETS, sub), ignore_errors=True)
    for sub in ["loot_table", "recipe"]:
        shutil.rmtree(os.path.join(DATA, NS, sub), ignore_errors=True)
    templates()

    tags = {k: [] for k in ["stairs", "slabs", "walls", "fences", "fence_gates", "trapdoors", "buttons", "pressure_plates",
                            "mineable/pickaxe", "mineable/shovel"]}
    tag_of = {"stairs": "stairs", "slab": "slabs", "wall": "walls", "fence": "fences", "fence_gate": "fence_gates",
              "trapdoor": "trapdoors", "button": "buttons", "pressure_plate": "pressure_plates"}
    us, gb = {}, {}
    count = 0
    for mat, kind in MATERIALS:
        for suffix, shape_name, cut in SHAPES:
            if not applies(mat, kind, suffix):
                continue
            name = mat + "_" + suffix
            BUILDERS[suffix](mat, name)
            loot(name, kind, suffix)
            recipe(mat, name, cut)
            if suffix in tag_of:
                tags[tag_of[suffix]].append(NS + ":" + name)
            if kind in ("terracotta", "concrete", "glazed"):
                tags["mineable/pickaxe"].append(NS + ":" + name)
            if kind == "powder":
                tags["mineable/shovel"].append(NS + ":" + name)
            us["block.%s.%s" % (NS, name)] = title(mat, False) + " " + shape_name
            gb["block.%s.%s" % (NS, name)] = title(mat, True) + " " + shape_name
            count += 1

    for tag, values in tags.items():
        write(os.path.join(DATA, "minecraft", "tags", "block", tag + ".json"), {"replace": False, "values": values})
    for tag in ["stairs", "slabs", "walls", "fences", "fence_gates", "trapdoors", "buttons"]:
        write(os.path.join(DATA, "minecraft", "tags", "item", tag + ".json"), {"replace": False, "values": tags[tag]})

    for tab, name in TABS:
        us["itemGroup.%s.%s" % (NS, tab)] = "Cubework: " + name
        gb["itemGroup.%s.%s" % (NS, tab)] = "Cubework: " + name
    us["item.%s.cube" % NS] = gb["item.%s.cube" % NS] = "Cubework"
    write(os.path.join(ASSETS, "lang", "en_us.json"), us)
    write(os.path.join(ASSETS, "lang", "en_gb.json"), gb)

    write(os.path.join(ASSETS, "items", "cube.json"), {"model": {"type": "minecraft:model", "model": NS + ":item/cube"}})
    write(os.path.join(ASSETS, "models", "item", "cube.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/cube"}})
    print("blocks:", count)


if __name__ == "__main__":
    main()
