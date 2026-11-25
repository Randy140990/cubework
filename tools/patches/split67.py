"""0.67: the rooms' block (retromon:prop, 37 thousand states) shared out over many blocks of 256 (John's 16:07)."""
import os, re, subprocess
R = "/home/claude/route-one"
J = R + "/src/main/java/com/insidethecube/retromon/"

def sub(path, old, new, count=1):
    s = open(path).read()
    assert s.count(old) == count, (path, old[:80], s.count(old))
    open(path, "w").write(s.replace(old, new))

# Prop.java whole.
open(J + "game/Prop.java", "w").write('''package com.insidethecube.retromon.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One block of an inside: its look is one of the models made from the Interior Viewer's rooms, its collision what's
 * solid there (furniture and walls to 1.5 high, the floor, the stairs). It can't be broken, lets light through and
 * hides nothing behind it.
 * <p>
 * (0.67, John's 16:07: "the mod is getting really slow to start") The rooms' looks, some 37 thousand, are shared out
 * over many blocks of {@link #SIZE} states each (retromon:prop, retromon:prop_1, ...). The time and memory Minecraft
 * takes to make a block's states grow with the square of how many it has: all of them in one block took about a
 * minute and a half and five gigabytes at every start (measured in the CI: 16 thousand took 16 seconds and a
 * gigabyte); in blocks of 256 it's under a second.
 */
public class Prop extends Block {
	/** States per block. */
	public static final int SIZE = 256;
	/**
	 * How many blocks: enough for the looks, and never fewer than this, so the blocks' names stay the same from one
	 * release to the next (the shaders list them: shaderpack's block.properties).
	 */
	public static final int BLOCKS = Math.max(160, (Interiors.STATES + SIZE - 1) / SIZE);
	public static final IntegerProperty M = IntegerProperty.create("m", 0, SIZE - 1);
	/** The blocks, in order. */
	public static final Prop[] ALL = new Prop[BLOCKS];
	/** The look this block's m=0 is (its m=k being look first + k). */
	public final int first;

	public Prop(BlockBehaviour.Properties p, int first) {
		super(p);
		this.first = first;
		registerDefaultState(stateDefinition.any().setValue(M, 0));
	}

	/** The block state for one of the looks. */
	public static BlockState state(int look) {
		int g = Math.max(0, Math.min(Interiors.STATES - 1, look));
		return ALL[g / SIZE].defaultBlockState().setValue(M, g % SIZE);
	}

	/** Which look a state is (-1 for any other block's). */
	public static int look(BlockState s) {
		return s.getBlock() instanceof Prop p ? p.first + s.getValue(M) : -1;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(M);
	}

	@Override
	protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return Interiors.shape(first + s.getValue(M));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) {
		return Interiors.shape(first + s.getValue(M));
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState s) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState s) {
		return true;
	}

	@Override
	protected float getShadeBrightness(BlockState s, BlockGetter level, BlockPos pos) {
		return 1F;
	}
}
''')

sub(J + "Retromon.java", '''		PROP = Registry.register(BuiltInRegistries.BLOCK, PROP_KEY, new com.insidethecube.retromon.game.Prop(
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(PROP_KEY).noOcclusion().strength(-1F, 3600000F).noLootTable()
						.isViewBlocking((st, g, pos) -> false).isSuffocating((st, g, pos) -> false)
						.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).sound(net.minecraft.world.level.block.SoundType.WOOD)));''',
'''		// (0.67) The rooms' blocks: their looks shared out over many blocks of Prop.SIZE states each (game/Prop).
		for (int k = 0; k < com.insidethecube.retromon.game.Prop.BLOCKS; k++) {
			ResourceKey<net.minecraft.world.level.block.Block> key = k == 0 ? PROP_KEY : ResourceKey.create(Registries.BLOCK, id("prop_" + k));
			com.insidethecube.retromon.game.Prop.ALL[k] = Registry.register(BuiltInRegistries.BLOCK, key, new com.insidethecube.retromon.game.Prop(
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(key).noOcclusion().strength(-1F, 3600000F).noLootTable()
							.isViewBlocking((st, g, pos) -> false).isSuffocating((st, g, pos) -> false)
							.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).sound(net.minecraft.world.level.block.SoundType.WOOD),
					k * com.insidethecube.retromon.game.Prop.SIZE));
		}
		PROP = com.insidethecube.retromon.game.Prop.ALL[0];''')

I = J + "game/Interiors.java"
sub(I, "if (!level.getBlockState(at).is(Retromon.PROP) && (r.schem()", "if (!(level.getBlockState(at).getBlock() instanceof Prop) && (r.schem()")
sub(I, '''		BlockState prop = Retromon.PROP.defaultBlockState();
		int[] c = r.cells();
		for (int i = 0; i + 3 < c.length; i += 4)
			level.setBlock(at.set(r.ox() + c[i], r.oy() + c[i + 1], r.oz() + c[i + 2]), prop.setValue(Prop.M, Math.min(STATES - 1, c[i + 3])), Block.UPDATE_CLIENTS);''',
'''		int[] c = r.cells();
		for (int i = 0; i + 3 < c.length; i += 4)
			level.setBlock(at.set(r.ox() + c[i], r.oy() + c[i + 1], r.oz() + c[i + 2]), Prop.state(c[i + 3]), Block.UPDATE_CLIENTS);''')
# ((0.67) "p256": built again once, in the new blocks.)
sub(I, "b.append(STATES).append('|').append(java.util.Arrays.hashCode(r.cells()))",
    "b.append(STATES).append(\"|p\").append(Prop.SIZE).append('|').append(java.util.Arrays.hashCode(r.cells()))")

sub(J + "client/BattleScene.java", '''		net.minecraft.world.level.block.state.BlockState prop = Retromon.PROP.defaultBlockState();
		int[] c = a.cells();
		int states = com.insidethecube.retromon.game.Interiors.STATES;
		for (int i = 0; i + 3 < c.length; i += 4)
			set(ox + c[i], y + c[i + 1], oz + c[i + 2], prop.setValue(com.insidethecube.retromon.game.Prop.M, Math.min(states - 1, c[i + 3])));''',
'''		int[] c = a.cells();
		for (int i = 0; i + 3 < c.length; i += 4) set(ox + c[i], y + c[i + 1], oz + c[i + 2], com.insidethecube.retromon.game.Prop.state(c[i + 3]));''')

sub(J + "client/RetromonClient.java",
    "		net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(Retromon.PROP, net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);",
    '''		for (com.insidethecube.retromon.game.Prop p : com.insidethecube.retromon.game.Prop.ALL)
			net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(p, net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);''')

sub(J + "client/PropTiles.java", '''			if (Retromon.PROP != null) ctx.registerBlockStateResolver(Retromon.PROP, r -> {
				int[] models = propModels();
				Map<Integer, BlockStateModel.UnbakedRoot> roots = new HashMap<>();
				for (BlockState st : r.block().getStateDefinition().getPossibleStates()) {
					int m = st.getValue(com.insidethecube.retromon.game.Prop.M);
					int model = m >= 0 && m < models.length ? models[m] : 0;
					r.setModel(st, roots.computeIfAbsent(model, k -> new SingleVariant.Unbaked(new Variant(Retromon.id("block/prop/" + k))).asRoot()));
				}
			});
			ctx.modifyBlockModelAfterBake().register((model, c) -> {
				if (Retromon.PROP == null || c.state().getBlock() != Retromon.PROP || model == null) return model;''',
'''			// ((0.67) Over all the rooms' blocks, each state its look's model; looks past the last share the first's.)
			int[] models = propModels();
			Map<Integer, BlockStateModel.UnbakedRoot> roots = new HashMap<>();
			for (com.insidethecube.retromon.game.Prop p : com.insidethecube.retromon.game.Prop.ALL) {
				if (p == null) continue;
				ctx.registerBlockStateResolver(p, r -> {
					for (BlockState st : r.block().getStateDefinition().getPossibleStates()) {
						int g = com.insidethecube.retromon.game.Prop.look(st);
						int model = g >= 0 && g < models.length ? models[g] : models.length > 0 ? models[0] : 0;
						r.setModel(st, roots.computeIfAbsent(model, k -> new SingleVariant.Unbaked(new Variant(Retromon.id("block/prop/" + k))).asRoot()));
					}
				});
			}
			ctx.modifyBlockModelAfterBake().register((model, c) -> {
				if (!(c.state().getBlock() instanceof com.insidethecube.retromon.game.Prop) || model == null) return model;''')

# Shaders 1.2.10: every prop block under 10090.
SP = R + "/shaderpack/retromon-light/shaders/"
ids = ["retromon:prop"] + ["retromon:prop_%d" % k for k in range(1, 160)]
lines = []
for i in range(0, len(ids), 8):
    lines.append("  " + " ".join(ids[i:i + 8]))
block = "block.10090 = " + " \\\n".join(lines).lstrip()
sub(SP + "block.properties", '''# (1.2.9) Retromon's own blocks, the insides of buildings: lit already (each corner coloured with the room's light).
block.10090 = retromon:prop''', '''# (1.2.9) Retromon's own blocks, the insides of buildings: lit already (each corner coloured with the room's light).
# (1.2.10) All of them: the rooms' looks are shared out over 160 blocks now (retromon:prop, retromon:prop_1 ...).
''' + block)
sub(SP + "shaders.properties", "# Retromon Light 1.2.9 by", "# Retromon Light 1.2.10 by")
sub(SP + "lang/en_us.lang", "option.INFO=Retromon Light 1.2.9", "option.INFO=Retromon Light 1.2.10")
sub(J + "client/Shaders.java", '"Retromon-Light-1.2.9.zip"', '"Retromon-Light-1.2.10.zip"')
Z = R + "/src/main/resources/retromon/shaders/"
os.remove(Z + "Retromon-Light-1.2.9.zip")
subprocess.check_call(["zip", "-q", "-r", "-X", Z + "Retromon-Light-1.2.10.zip", "shaders"], cwd=R + "/shaderpack/retromon-light")
print("ok")
