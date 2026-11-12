package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A full block in a pattern (tiled, checkered, herringbone or bordered), behaving like the block it's made from. */
public class PatternBlock extends Block implements CwShape {
	private final Material material;

	public PatternBlock(Material material, BlockBehaviour.Properties settings) {
		super(settings);
		this.material = material;
	}

	@Override
	public Material material() {
		return material;
	}
}
