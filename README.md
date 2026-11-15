# Cubework

Cubework by Inside the Cube. Vanilla blocks in every shape: stairs, slabs, vertical slabs, snow-style layers, slopes, walls, fences and trapdoors that behave just like the originals.

- Fabric, Minecraft 1.21.11, needs Fabric API.
- The block list is in `Material.java` and `Shape.java`. After changing either, run `python3 tools/gen.py` (its lists must match) to rewrite the resources.
- Shapes use the vanilla textures directly, so texture packs carry over.
- Each shape is one block for each behaviour (`Behaviour.java`: solid, glass, wool, copper...), 437 in all, and each placed shape keeps its material in a block entity (`block/ShapeEntity.java`), as each stack keeps it in the `cubework:material` component. The client draws a shape from its material's family's look (`assets/cubework/looks/`, written in stand-in textures) in the material's own textures (`client/MaterialModels.java`). Worlds from before 0.12, when every material's shapes were blocks of their own, are converted as they load (`world/Convert.java`).

## Included mod
The 1.21.11 jar carries FerriteCore 8.2.0 (https://modrinth.com/mod/ferrite-core, by malte0811, MIT licence; its licence is inside its own jar under META-INF/jars). It makes the game's block states much lighter, which a mod with this many blocks needs. Fabric loads whichever copy is newer if a player also has FerriteCore installed. ModernFix (by embeddedt) is suggested but not included.
