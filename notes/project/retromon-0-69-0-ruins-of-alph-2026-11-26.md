# Retromon 0.69.0: the Ruins of Alph, between its two gates (26 Nov 2026)

## What John asked (15:58, with Ruins_of_Alph.zip)
"we've got the ruins of Alf to incorporate within the 2 gates ... The schematic isn't built into the world ... you
would go into them. Those gates obviously need to be open. I think people can go in and just walk around and explore
it, but I would say none of those caves or anything will have any interaction until I build that."

## What was done
- **The place.** John's schematic (148 x 16 x 192, data version 4671) is a block room in the interiors, the way Ilex
  Forest is.
  - Origin (18944, 64, 0), past Mt. Silver's rooms. FLOOR 4 (the file's y 3 is the ground), base -4. Not dark (lit).
  - Files: `ruins_of_alph.bin` and `.json`, written by `tools/alph.py` (`tools/alph/ruins_of_alph.schem`).
  - Place `ruins_of_alph_outside`. No grass rects, items or people.
- **The ways in** (the gate rooms' ruins sides, `rooms63.js`'s INTO_RUINS):
  - `jgate_alph`: the south mat leads to the ruins at (58, 43.4), facing south.
  - `jgate_r32`: the west passage leads to the ruins at (104.6, 110), facing west.
  - `tomod.py`'s side warps can now lead to a room ("to" and "spawn").
- **The ways out** (the ruins' warps):
  - a `north` warp at x 56.6-59.4, z 36.5-38.2 leads to `jgate_alph` at (7, 12.5), facing north;
  - a `box` warp at x 108.8-110.2, z 108.6-111.4 leads to `jgate_r32` at (1.6, 5), facing east.
- **The doorways.**
  - The two gatehouses' doorway backs (the north one at file z 36, the east one at x 110) are now white concrete
    with lights in front. Our looks draw them as daylight (`cave_daylight`).
  - The 16 chamber doorways stay black, with no interaction.
- **The signs.** Three, as HeartGold's msg 72:
  - (51, 55): "Visitors who are interested in archaeology are welcome.";
  - (52, 97): "Feel free to explore the ruins and become a fossil professor!";
  - (72, 97): "RUINS RESEARCH CENTER".
- **Looks** (`tools/arealooks69.py`: 106 interiors rows, run after the post filters):
  - every generic Johto row (the JOHTO and JM boxes), with heights +42;
  - BOULDER over the rock;
  - his Route 36 gate's rows over each gatehouse, with the white concrete as `cave_daylight`;
  - the cave mouth rows round each chamber doorway (+-2);
  - `sign` on the three shelves.
  - Still in Minecraft's own pictures: the stone bricks family, polished andesite, crying obsidian, the blue and light
    blue concrete roof, packed mud, coarse and rooted dirt, and the spruce trapdoors.
- **Route 32's gate** (`jgate_r32`) is renamed "Ruins of Alph gate". Its floor is now "Route 32 to the Ruins of Alph"
  and its view "ROUTE 32 one way, the RUINS OF ALPH the other" (it used to say Violet City).
- **The viewer** (Interior Viewer v57):
  - `ruins_of_alph` in orbit view, from `alph_data.js` (`tools/alphview.py`): every block with a face showing (106k),
    in the game's pictures, with the signs as the game's sign models;
  - `rooms71.js`;
  - `rooms70.js` now exposes `blockShape` and `blockParse`, and has the plants' crosses and the vines;
  - the "Johto: the Ruins of Alph" group lists `jgate_alph`, `ruins_of_alph` and `jgate_r32`.
- **rooms.json** was regenerated (viewer, then `export.js`, then `tomod.py`). The only changes are the two gates'
  warps and Route 32's gate's name and words.

## Rebuilding
```
python3 -I tools/alph.py
cd tools && python3 arealooks.py && python3 checklooks.py && cd ..
python3 -I tools/alphview.py <assets/minecraft>
python3 tools/interiorviewer.py <creature-viewer.html> <interior-viewer.html>
```
