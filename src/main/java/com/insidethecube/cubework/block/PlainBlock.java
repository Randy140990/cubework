package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Material;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A full block drawn differently from its material (the raised frame, wallpaper): it behaves like its material's block. */
public class PlainBlock extends CwBlock {
	public PlainBlock(Material material, BlockBehaviour.Properties settings) {
		super(material, settings);
	}
}
