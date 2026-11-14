Cubework by Inside the Cube.

0.8.4: Curved Stairs with a wavy underside, as in the curved-stair builds (1.21.11 only, cubework-0.8.4-mc1.21.11.jar).
- Curved Stairs: the underside is now a wave that rises the whole way and never dips: it flares off the floor, tucks in close under the first stair, swells out under the middle stairs, and tucks back in under the top stair. Flights of five or six stairs get two waves. The thinnest point is a quarter of a block under the steps' inner corners. Same pieces and building as before; flights you built with 0.8.3 take the new shape when you use any piece with an empty hand.
- Jar files are now named cubework-<version>-mc<Minecraft version>.jar (the plus sign was lost on download).

0.8.3: a smoother S under Curved Stairs (1.21.11 only, cubework-0.8.3+1.21.11.jar).
- Curved Stairs: the underside is now one even S that rises all the way: it bends up off the floor under the bottom stair, follows the underside of the stairs half a block below them, and bends level under the top stair. No more sag under the second stair. Same pieces and building as 0.8.2: lay the flight with Curved Stairs, then fill under it with the same item. Flights you built with 0.8.2 take the new shape when you use any piece with an empty hand.

0.8.2: Curved Stairs (1.21.11 only, cubework-0.8.2+1.21.11.jar).
- Curved Stairs: a flight of stairs with a smooth S-shaped underside, as in the curved-stair builds: under the first stair the underside springs up from the floor (in), under the middle stairs it sags into a belly (out), and it rises close under the top stair again (back in). Lay the flight with Curved Stairs like ordinary stairs (it climbs away from you), then fill the blocks under it with the same item: a block with the flight right above it becomes the fill. Every piece works out how long the flight is and where it sits, and the whole flight re-fits as you add pieces; fill that would sit in the hollow under the curve simply isn't placed, so you can fill the whole space under the flight. Flights of 2 to 6 stairs (longer ones curve under their top six); if you change a flight, use any piece with an empty hand to re-fit it. Ordinary stairs on top work too (the fill still curves, the steps stay square). On the shape wheel with stairs and slopes; 1 from a block in the stonecutter. For every coloured and building block.

0.8.1: Stair Curve, a single sweep under ordinary stairs (replaced by Curved Stairs in 0.8.2).

0.8.0: curving stairs, round windows and trims (1.21.11 only, cubework-0.8.0+1.21.11.jar). About 15,000 items, up from about 13,000.
- Stairs (1 in 2), (1 in 3) and (1 in 4): stairs that climb one block over two, three or four. Each block has two steps half a block deep, like ordinary stairs, but lower. Lay 1 in 4 stairs, then 1 in 3, 1 in 2 and ordinary stairs, and the flight curves up from the floor; run them the other way at the top and it curves out onto a landing. Placed in a row they take the next step by themselves, and an empty hand moves one to its next step. Upside down on a ceiling or the top half of a face, like stairs. Under a flight, slopes of the same pitch placed upside down, facing back down the flight, give it a smooth underside.
- Round window frames (4, 8, 12 and 16 pixels deep): one on its own is a round porthole. They join like the square ones and keep the curve: two side by side make an oval, a square of four one big round window, and a stack a tall window with round ends. Drawn smooth, with the sill, sides and head in the block's own textures. They join square and diamond frames too.
- Trim (2, 4 and 8 pixels): a square strip along one edge of the block, for skirting, cornices, sills, edging and wall corners. It lies against the face you click, along the edge of that face nearest where you clicked: click a wall near the top for a cornice, near the floor for a skirting board, near a side for an upright.
- All of these come in every coloured block (wool, terracotta, concrete, concrete powder, glazed terracotta, glass) and every building block (bricks, stone bricks, the stones and their polished forms, deepslate, tuff, mud and resin bricks, sandstone, prismarine, nether bricks, blackstone, purpur, quartz, the metal and gem blocks, copper). On the shape wheel, the new stairs are with stairs and slopes, round frames with the window frames, and trims with the small parts. Stonecutter: 2 of any of the new stairs from a block; 16, 8 or 4 trims; round frames as the square ones.

0.7.1: fixes from the first test of 0.7.0 (1.21.11 only, cubework-0.7.1+1.21.11.jar).
- Shape wheel: sized to fit the screen with its name plate showing in full; see-through slices with clear gaps between them, like slices of a pizza; the slice you point at is outlined and lit inside its own edges, never past them; the plate says what letting go will do (Let go to take it, In your hand now); the hint at the top names your wheel key; no blur. Pressing G with a block that has no shapes now says so (Iron Bars has no Cubework shapes).
- Builder's guide: before you place it, its outline is white and follows where you look; right-click a block and it turns gold where you clicked, with a chime and a line saying what it is. Taking the guide or the terrain brush in hand shows how to use it above the hotbar.
- Texture packs: patterned blocks and coloured doors are painted at up to 256 x 256 from a pack's textures (an HD pack's 512 or 1024 textures are scaled down for these), patterned blocks' shader maps are always one frame at the same size, and a texture that can't be read only leaves that one block plain instead of stopping the pack loading.

0.7.0: the shape wheel, the terrain brush, builder's guides, patterns, round shapes and connected textures, for 1.21.11 only for now (cubework-0.7.0+1.21.11.jar). About 13,000 items, up from about 7,100. The other versions follow once this one is tested.
- Shape wheel: hold G (Controls, Cubework) while holding a block, or with an empty hand looking at one in creative. Groups sit on the inner ring (slabs and layers, stairs and slopes, pillars and columns, arches and curves, walls and fences, window frames, small parts, patterns); point at one and its shapes open on the outer ring; let go over a shape to take it, or over the middle for the full block. Tap G to leave it open and click instead; scroll for groups; 1 to 9 picks a shape. In creative the stack in hand becomes the shape. In survival it changes at stonecutter rates in whole blocks' worth (64 stone bricks make 128 slabs, and 128 slabs make 64 bricks back), and the wheel shows what your stack would make. Vanilla's own stairs, slabs and walls count as their block's shapes.
- Terrain brush: use it on the ground and every one-block step in its circle becomes a slope of the ground's own block, corners joined. Fill adds slopes on the low side; carve cuts the top edge (survival only carves). Steep (1 in 1), natural (terraces 2 to 4 wide get the long slopes) or gentle. Radius 3 to 16. Undo on the wheel. Its circle shows on the ground as you aim. Craft: brush, copper ingot, smooth stone slab.
- New land can be smoothed as it generates: set smoothNewTerrain to true in config/cubework.json (off to start with; smoothSteepness steep, natural or gentle).
- Builder's guide: see-through outlines to build along by hand, never placing anything. Circle, ellipse, sphere, dome, arch, cylinder, polygon (3 to 12 sides) and line. Use it on a block to set the middle (for a line: the start, and sneak-use for the end); before that it previews where you look. Shape, sizes and sides on the wheel, with the block count; Pin keeps it showing when it's in your inventory. Craft: paper, string, stick.
- Patterns tab: tiled, checkered, herringbone and bordered versions of 96 blocks (stone and stone bricks, polished stones, deepslate, tuff, bricks, mud and resin bricks, smooth sandstones, prismarine, nether bricks, blackstone, end stone, purpur, quartz, obsidian, the metal and gem blocks, amethyst, terracotta, concrete and wool), each as a full block plus stairs, slabs, quarter and vertical slabs, slope, layers, carpet and panel. They're painted from your texture pack's own textures, and with shaders they keep the block's shine (herringbone's maps are laid out like its bricks). Stonecutter: block to pattern and back.
- New shapes: round columns 16, 12 and 8 pixels across (any axis), round corners (a quarter of a round tower, and the curved fill for the inside of a corner, sitting in the corner you look towards) and corner posts (8 by 8, in the corner you click). For every building and coloured block.
- Natural blocks get stairs, quarter slabs and the 1 in 2, 3 and 4 slopes (grass keeps its green edges on each step). Arches, window frames, quarter slabs and the long slopes now come in concrete powder, glazed terracotta and glass too.
- Connected textures: to Continuity every shape looks like its full block, so a pack's connected textures, random variants and edge overlays for that block apply to its shapes, and shapes join up with the full blocks beside them. Overlays (a grass edge onto sand) follow the shape's own face, and shapes give overlays to their neighbours, so a grass slope edges the sand slope next to it.

0.6.1: adds 26.1, 26.2 and 26.3 (cubework-0.6.1+26.1.jar for 26.1 to 26.1.2, +26.2, +26.3), alongside every version from 1.21.1 to 1.21.11. Nothing changes in the game. From 26.1 the game picks see-through and cut-out drawing from the textures itself, and from 26.3 Cubework's thin logs strip themselves when you use an axe on them (26.3 moved stripping into data a mod can't add to).

0.6.0: Cubework for every Minecraft version from 1.21.1 to 1.21.11, one jar for each: pick the one named for your version (cubework-0.6.0+1.21.1.jar for 1.21 and 1.21.1, +1.21.3 for 1.21.2 and 1.21.3, +1.21.4, +1.21.5, +1.21.8 for 1.21.6 to 1.21.8, +1.21.10 for 1.21.9 and 1.21.10, +1.21.11). Everything from 0.5.0 is in each, except blocks the game didn't have yet: pale oak and resin arrived in 1.21.4, bushes and short dry grass in 1.21.5. 26.1, 26.2 and 26.3 are next.

0.5.1: no changes in the game. The code now uses Mojang's own names for Minecraft's code (needed for 26.1 and later), the first step to versions from 1.21.1 to 26.3.

0.5.0: tables, steeper and gentler slopes, door handles and door shine.
- Tables tab: low tables shaped like a bed (a 6-pixel top on 3-pixel legs). 58 tops (terracotta, glazed terracotta, stripped woods, smooth stone, polished granite, diorite and andesite, chiseled deepslate, tuff, chiseled resin, quartz) with oak legs or legs in any concrete colour: 986 tables. Tables with the same legs placed side by side join: legs only at the outside corners, so two make a bed-sized table, four a square one, a ring one with a hole. The top's pattern turns with the way you face as you place it. Crafting: three of the top block over two sticks (or two concrete for coloured legs) makes three.
- Slopes over 2 and 3 blocks join the slope over 4 (now called Slope (1 in 4)): place them in a row and each takes the next step by itself; an empty hand moves one to its next step. For wool, terracotta, concrete and every building block.
- Recoloured doors keep their handles and hinges (cherry's silver ring, dark oak's gold handle, birch's dark hinges).
- With shaders, coloured and metal doors use the shader maps (shine and depth) of the door they're painted from: the wood or iron door, or the copper door for the metal ones.

0.4.0: building blocks, smooth arches, coral, tapering logs, and iron and metal doors.
- Building tab: 98 building blocks in every shape (stairs to window frames) plus pillars 12, 8 and 4 pixels wide: stone and stone bricks, granite, diorite, andesite, deepslate, tuff, bricks, mud, resin, sandstone and red sandstone, prismarine, nether bricks, basalt, blackstone, end stone bricks, purpur, the metal and gem blocks, quartz, amethyst and copper. 2,856 new pieces.
- Copper shapes weather like copper, take wax from honeycomb, and an axe scrapes off wax or weathering.
- Arches are drawn with a smooth curve. An arch corner's back goes against the block you click, so two facing each other across a gap make an arch.
- Window frames join whatever their depth (a shallower one leaves a ledge), square or diamond.
- Thin logs: a 16 px log, and any end with another log against it shows bark, so thinner logs on thicker ones look like one tapering trunk.
- Coral and coral fans at 50% to 150%, living and dead; living coral dies out of water like vanilla's.
- Doors: the iron door in the 16 colours (still redstone only), and the copper door's pattern in iron, gold, redstone, emerald, lapis, diamond and netherite (opens by hand), painted from those blocks' textures.
- The Plants tab has a fern for its icon.

0.3.0: gentle slopes, quarter slabs, arches and window frames, for wool, terracotta and concrete (49 blocks, 588 new pieces).
- Gentle slope: rises 4 pixels across the block, so four in a row climb one block. Placed in front of one facing the same way, it takes the next step by itself; use one with an empty hand to move it to the next step. Corners join like slopes.
- Quarter slab: 4 pixels thick, at the bottom or the top of the block.
- Arch: a round-headed opening in one block. Arch corner: half of a two-wide arch; it rises to your right, or to your left to meet another corner. Top quarter slabs between two corners make a three- or four-wide arch.
- Window frames, 4, 8, 12 or 16 pixels deep, square or diamond: a 4-pixel border round an 8-wide opening, facing you when you place it. Frames of the same kind join: stacked they make a tall slit with a sill and a lintel, side by side a long one.

0.2.0: nature, plants and coloured doors.
- Nature: 68 natural blocks (dirt and grass family, sand and sandstone, ices, snow, moss, stones, Nether blocks, end stone, every ore, raw ore blocks, glowstone, amethyst) as slab, vertical slab, slope, layer, vertical layer and carpet. Grass keeps the biome colour; dirt path and farmland pieces are 15 pixels tall.
- Thin logs: every log and stem (and stripped) at 12, 8 and 4 pixels wide; an axe strips them.
- Plants: short grass, fern, short dry grass and bush at 50% to 175%; mushrooms, fungi and flowers at 50% to 150%.
- Doors: the 12 wooden door patterns in all 16 colours (192 doors), coloured from your current door textures. Craft a door with a dye.
- Four tabs instead of six. Slope icons face like stairs. The cube icon is a proper block. Glass fences no longer show overlapping bars.

0.1.1: works with Fabric Loader 0.16 and later (0.1.0 wrongly asked for 0.19.5).

0.1.0: the first pass.
- 99 vanilla blocks: all wool, terracotta, concrete, concrete powder, glazed terracotta and glass (clear, tinted and stained).
- 15 shapes for each: stairs, slab, vertical slab, slope (with corners), layer, vertical layer, carpet, panel, post, wall, fence, fence gate, trapdoor, button and pressure plate. 1,469 new blocks.
- Six creative tabs. Every shape is made in the stonecutter.
