# Plan for 0.58.0 (after 0.57.0, delivered 17:00)

Interiors John has passed over (15:52, 16:15, 16:23) that 0.57 didn't have:
- CINNABAR (FireRed, pret/pokefirered): the Gym (30x25, quiz machines and gates), the POKeMON LAB (Entrance 28x11, Lounge,
  Research Room, Experiment Room 15x11 each; fossil revival), the POKeMON MANSION (1F 38x35, 2F 38x38, 3F 38x35, B1F
  38x35: stairs, holes down from 3F to 1F, the statue switches and barred gates).
  Way: tools/cinnabar.py like tools/celadon.py (tiles sorted by picture and collision, warps, people, signs) writing
  tools/interior/cinnabar_data.js; tools/interior/rooms62.js building them (like rooms58.js); tomod.py ORIGIN, PLACE,
  DOORS (the world doors: Gym's at x -786..-768 z 900..911, Lab's at -811..-797 916..929, Mansion's -810..-791 899..914),
  MAT_DOOR, ENTRY, TILE.
- JOHTO (pokecrystal, hand-built from its maps): the SPROUT TOWER 1F/2F/3F (ladders, the swaying pillar, Sages, items),
  EARL's POKeMON ACADEMY (Violet, the house with the chimney at -1837..-1826 535..544), the NATIONAL PARK's two gates
  (Route 35's at -2129..-2086 541..557, Route 36's at -2086..-2071 500..511).
- Mechanics to follow if there's time: the Gym quiz (a question at each machine, a right answer opens its gate), the
  Mansion's switches (toggle the barred gates), fossils revived at the Lab.
- Also asked: Surf (so the sea's trainers can be reached); where's the swing (John to say).
