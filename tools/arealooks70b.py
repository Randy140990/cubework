"""(0.70, John's 27 Nov 15:00, his Johto pass on 0.69) The looks for it, after round70 (tools/looks70b.py draws the new
pictures; tools/scan70b.py found the blocks, tools/area_70b.json):

- The bridges' posts ("one rogue block which is the same texture as the planks ... wherever there's a pillar on Route 45
  ... just need updating to be the same as the rest of the pillars wherever you find it"): each post's top block in the
  deck its beams' wood, as the rest of the post, not the deck's boards.
- Kanto's steps up (arealooks.py: Johto's boards now, "change all of Kanto to align to the Johto ones"): the mud brick
  slabs round Fuchsia's ponds stay rock; Celadon's two slab steps in its paving stay its paving.
- Goldenrod's bars in its alleys ("wherever you've got the iron bars, just make sure they're correct. The ones that are
  specifically like in the alleyways"): his copper bars at the alleys' ends the alleys' dark railing, as his iron bars.
  Johto's iron chains the steel chain, hung as Kanto's (the chain along Olivine's pier, Goldenrod's way south).
- Goldenrod's MAGNET TRAIN plaza and the way over to it ("not too keen on the floor being like this continued brick ...
  where it would have been a glazed terracotta ... It doesn't look as seamless as it probably could"): big cream
  flags, the gold square a seamless gold terrazzo (the fountains' green in arealooks.py, its rims in arealooks59b).
- The NATIONAL PARK's swan's eyes ("a gem, like a blue and a shine to it"): a sapphire each side.
- The TIN TOWER's roofs ("Tiling I just think could probably be a little bit better"): rounded tiles in channels down
  each side, the red and the gold alike, each side's tiles running down away from the tower.
- Olivine's harbour wall where it turns up by the cliff ("it's gone from white to like a grey"): the sea wall's white.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def ys(a, b):
    return (str(a), str(b))


FUCHSIA = (-560, 733, -441, 860)
PLAZA = (-2214, 668, -2147, 720, "Goldenrod's MAGNET TRAIN plaza")
TIN = (-1992, 337, -1964, 366)
TIN_BLOCKS = "crimson_planks,crimson_slab,crimson_stairs,nether_bricks,nether_brick_slab,nether_brick_stairs"
COPPER_BARS = ",".join(w + c + "copper_bars" for w in ("", "waxed_") for c in ("", "exposed_", "weathered_", "oxidized_"))


def round70b(add, G):
    D = json.load(open(os.path.join(HERE, "area_70b.json")))
    # The bridges' post tops in their decks: the beams' wood all round.
    for x, y, z in D["piertops"]:
        add("cube", "stripped_spruce_wood,stripped_spruce_log,stripped_oak_wood,stripped_oak_log", (x, z, x, z, "a bridge's post"), "bridge_beam", ys(y, y))
    # Fuchsia's ponds' rims: rock, in their own colour, as before.
    for x0, z0, x1, z1, y0, y1 in D["rims"]:
        if not (FUCHSIA[0] <= x0 <= FUCHSIA[2] and FUCHSIA[1] <= z0 <= FUCHSIA[3]): continue
        add("swap", "mud_brick_slab", (x0, z0, x1, z1, "a pond's rim in Fuchsia"), "block/mud_bricks=mud_rock@4", ys(y0, y1))
    # Celadon's two slab steps at the end of its path: its paving, as the kerb they sit in.
    for x in (-547, -541):
        add("swap", "mud_brick_slab", (x, 452, x, 452, "a step in Celadon's paving"), "block/mud_bricks=pave_cel@2", ys(32, 32))
    # Goldenrod's copper bars (the alleys' ends): the alleys' dark railing.
    GOLD = G["GOLD_IN"]
    add("swap", COPPER_BARS, GOLD, ",".join("block/%scopper_bars=railing_dark" % c for c in ("", "exposed_", "weathered_", "oxidized_")))
    # Johto's iron chains: the steel chain, hung as Kanto's.
    J = G["JOHTO"]
    add("chain", "iron_chain", J)
    add("swap", "iron_chain", J, "block/iron_chain=chain_steel")
    # The MAGNET TRAIN plaza and its way over: cream flags; the gold square seamless.
    add("swap", "terracotta,brown_glazed_terracotta", PLAZA, "block/terracotta=gold_plaza@4,block/brown_glazed_terracotta=gold_plaza_gold@4", ys(19, 19))
    # The swan's eyes.
    add("swap", "spruce_button", (-2126, 480, -2126, 482, "the NATIONAL PARK swan's eyes"), "block/spruce_planks=swan_gem", ys(35, 35))
    # The TIN TOWER's roofs, side by side: each row of the tower (north to south) in three, its west, its middle (the
    # north side or the south) and its east, as the four sides meet on the diagonals.
    x0, z0, x1, z1 = TIN
    # (Its middle: the middle of its roofs and of its posts, x -1978, z 351.)
    cx, cz = -1978, 351
    spec = lambda side: ("block/crimson_planks=tin_roof_%s@4|tin_roof_south@4,block/nether_bricks=tin_ridge_%s@4|tin_ridge_south@4" % (side, side))
    for z in range(z0, z1 + 1):
        dz = z - cz
        half = int(abs(dz))
        mid = ("north" if dz < 0 else "south")
        a, b = int(cx - half), int(cx + half)
        parts = [(x0, a - 1, "west"), (max(x0, a), min(x1, b), mid), (b + 1, x1, "east")]
        for xa, xb, side in parts:
            if xa > xb: continue
            add("swap", TIN_BLOCKS, (xa, z, xb, z, "the TIN TOWER's roofs (%s)" % side), spec(side), ys(20, 84))
    # Olivine's sea wall where it turns up by the cliff: its white cap and posts, not the sea's rocks.
    b = (-2324, 594, -2320, 598, "Olivine's sea wall by the cliff")
    add("swap", "diorite", b, "block/diorite=ol_wall_cap", ys(18, 18))
    add("swap", "diorite_wall", b, "block/diorite=ol_post", ys(19, 20))
