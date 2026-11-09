package com.insidethecube.cubework.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.DoorBlock;

/** A wooden door's pattern in one of the 16 colours; opens, closes and sounds like its wood. */
public class ColouredDoorBlock extends DoorBlock {
	public ColouredDoorBlock(BlockSetType type, AbstractBlock.Settings settings) {
		super(type, settings);
	}
}
