package com.insidethecube.cubework.block;

import com.insidethecube.cubework.Behaviour;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A full block drawn differently from its material (the raised frame, wallpaper): it behaves like its material's block. */
public class PlainBlock extends CwBlock {
	public PlainBlock(Behaviour behaviour, BlockBehaviour.Properties settings) {
		super(behaviour, settings);
	}
}
