package com.insidethecube.cubework.client;

//#if MC >= 1.21.5
import java.util.List;
import java.util.function.Predicate;

import com.insidethecube.cubework.block.Joining;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
//#if MC < 26.1
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//#endif

/**
 * The model of a block that joins its neighbours without keeping it in its block state (see Joining): it
 * holds the block's look for each way of joining (16), and as the chunk is drawn it asks the block which
 * sides join there and draws that look. With no world to ask (in the hand, falling, pushed by a piston) it
 * draws the block's lone look.
 */
final class JoinedModel implements BlockStateModel {
	private final Joining block;
	private final BlockStateModel[] looks;
	private final int lone;

	JoinedModel(Joining block, BlockStateModel[] looks, int lone) {
		this.block = block;
		this.looks = looks;
		this.lone = lone;
	}

	private BlockStateModel at(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		return looks[block.joins(world, pos, state) & 15];
	}

	@Override
	public void collectParts(RandomSource random, List<BlockModelPart> parts) {
		looks[lone].collectParts(random, parts);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
		at(world, pos, state).emitQuads(emitter, world, pos, state, random, cullTest);
	}

	@Override
	public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
		return at(world, pos, state).createGeometryKey(world, pos, state, random);
	}

	//#if MC >= 26.1
	@Override
	public net.minecraft.client.resources.model.Material.Baked particleMaterial() {
		return looks[lone].particleMaterial();
	}

	@Override
	public int materialFlags() {
		int flags = 0;
		for (BlockStateModel look : looks) flags |= look.materialFlags();
		return flags;
	}
	//#else
	@Override
	public TextureAtlasSprite particleIcon() {
		return looks[lone].particleIcon();
	}
	//#endif
}
//#else
/** Joined models need the block model system of 1.21.5 and later. */
final class JoinedModel {
	private JoinedModel() {}
}
//#endif
