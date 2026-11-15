package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A full block in a pattern (tiled, checkered, herringbone or bordered) or a wallpaper, behaving like the block it's made from. */
public class PatternBlock extends Block {
	private final Material material;

	public PatternBlock(Material material, BlockBehaviour.Properties settings) {
		super(settings);
		this.material = material;
	}

	public Material material() {
		return material;
	}
}
