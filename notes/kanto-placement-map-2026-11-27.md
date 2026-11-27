# Kanto Placement Map, and the UI Viewer's admin pages (27 Nov, after 20:11)

John's 20:11: "if we were to place people, are the people placed in the right places? Do they have the right
proximity ... the right level of stuff ... people like Fisher, NPC, Teacher ... and locations" (with RankedBoost's
Gold/Silver interactive map as the example), and "I do want you to put the places and event pickers, etc. in the UI
viewer".

## The Kanto Placement Map (artifact)

https://claude.ai/artifact/VFBdGNf4Vyb8pWKLhULcUp

- John's Kanto from above (his world's blocks, one pixel a block), with every FireRed outside trainer, person, item
  ball, hidden item and CUT tree or rock on it at FireRed's own spot.
- A click shows the team and levels, which way they face and how far they see (drawn as the sight box: their sight in
  steps, two blocks a step, a step wide), the block they stand on (x, y, z, with a copy-/tp button), what's under them,
  and their key in the mod.
- Each route is marked LINED UP or CHECK (dashed edge on the map).

## How the spots were found (tools/kanto_outdoor_map.py)

- His terrain was built from FireRed's map at 2:1, so each FireRed map sits on his world at some block offset.
- FireRed's maps are laid out by their connections (pret pokefirered's map.json), Pallet's doors fixing the start
  (block x = -814 + 2 * step, z = 666 + 2 * step).
- Each map's offset is then searched within 48 blocks, scoring its tiles against his world's top blocks (mapbake.py's
  bake of the 1.0.1 map world):
  - walls (trees, rocks, buildings) against leaves and solid blocks;
  - water against water;
  - tall grass against grass_block, not path (his tall grass is grass_block on top);
  - ledges against a drop in ground height the way they jump;
  - signs against his sign shelves.
- A route whose next-best offset scores within 0.03, or whose best is under 0.55, is CHECK: those are mostly routes
  whose trees repeat along them, so the fit can't tell one step from the next (a possible 2-block shift).
- Fuchsia takes its second-best fit, which its doors agree with.
- Saffron is placed from its own connections (FireRed joins it to the routes through a connection map that isn't in
  the checkout).

## What's in it

- 165 trainers, 86 people, 15 item balls, 51 hidden items, 35 CUT trees and rocks.
- People and items are FireRed's (its townsfolk with their first line), keyed to the mod where they match.
- Nothing has been put into the game from it yet: waiting on John's look.

## UI Viewer (version 30)

- OPTION: PHOTO MODE, PARTY PICKER, EVENT PICKER and PLACER rows (as the mod's).
- New screens, as the mod draws them: PARTY PICKER, EVENT PICKER (the 87 steps), PLACER (the 470) and its WALK AND
  PLACE card, and the GAME CORNER's slot machine (its reels and payouts).
- The PLACER now names items as the game does (ULTRA BALL, TM28 DIG), in the mod's placeables.tsv too (next jar).
