# Cubework

Cubework by Inside the Cube. Vanilla blocks in every shape: stairs, slabs, vertical slabs, snow-style layers, slopes, walls, fences and trapdoors that behave just like the originals.

- Fabric, Minecraft 1.21.11, needs Fabric API.
- The block list is in `Material.java` and `Shape.java`. After changing either, run `python3 tools/gen.py` (its lists must match) to rewrite the resources.
- Shapes use the vanilla textures directly, so texture packs carry over.
