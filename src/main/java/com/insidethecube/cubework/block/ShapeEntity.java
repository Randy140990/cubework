package com.insidethecube.cubework.block;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a placed shape is made of. Each shape has one block per behaviour (slab, slab_glass...), and the
 * material of each placed one (red wool, stone bricks) is kept here: saved with the chunk, sent to players,
 * copied onto the item when it's broken and from the item when it's placed. The client draws the shape
 * with the material's textures (see client.MaterialModels).
 */
public final class ShapeEntity extends BlockEntity {
	public static final String KEY = "material";

	private Material material;

	public ShapeEntity(BlockPos pos, BlockState state) {
		super(CwBlocks.SHAPE_ENTITY, pos, state);
	}

	/** The material, or null when none was ever set (a shape made with /setblock). */
	public Material material() {
		return material;
	}

	/** Changes the material (concrete powder setting, copper weathering), telling players to redraw it. */
	public void setMaterial(Material m) {
		if (m == material) return;
		material = m;
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** The material of the shape at pos (null if it isn't one, or has none). */
	public static Material at(BlockGetter world, BlockPos pos) {
		if (world == null || pos == null) return null;
		return world.getBlockEntity(pos) instanceof ShapeEntity e ? e.material : null;
	}

	/** The material at pos, or the shape block's first material when it has none. */
	public static Material atOrDefault(BlockGetter world, BlockPos pos, BlockState state) {
		Material m = at(world, pos);
		return m != null ? m : CwBlocks.defaultMaterial(state.getBlock());
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		Material before = material;
		material = in.getString(KEY).map(Material::byId).orElse(null);
		// On the client, a changed material means drawing the block again.
		if (before != material && level != null && level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (material != null) out.putString(KEY, material.id());
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		Material m = components.get(CwBlocks.MATERIAL);
		if (m != null) material = m;
		// Concrete powder placed against water sets straight away.
		if (level != null && !level.isClientSide()) Shared.settle(level, worldPosition, this);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (material != null) components.set(CwBlocks.MATERIAL, material);
	}

	@Override
	public void removeComponentsFromTag(ValueOutput out) {
		out.discard(KEY);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	/** What the chunk's drawing reads (see client.MaterialModels). */
	@Override
	public Object getRenderData() {
		return material;
	}

}
