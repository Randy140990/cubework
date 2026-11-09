package com.insidethecube.cubework.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.DoorBlock;

/** A door's pattern in a colour or a metal; it opens, closes and sounds like its set type (its wood, iron or copper). */
public class ColouredDoorBlock extends DoorBlock {
	public ColouredDoorBlock(BlockSetType type, AbstractBlock.Settings settings) {
		super(type, settings);
	}
}
