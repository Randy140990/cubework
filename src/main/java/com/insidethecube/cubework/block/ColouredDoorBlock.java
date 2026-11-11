package com.insidethecube.cubework.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** A door's pattern in a colour or a metal; it opens, closes and sounds like its set type (its wood, iron or copper). */
public class ColouredDoorBlock extends DoorBlock {
	public ColouredDoorBlock(BlockSetType type, BlockBehaviour.Properties settings) {
		super(type, settings);
	}
}
