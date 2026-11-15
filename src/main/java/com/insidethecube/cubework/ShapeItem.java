package com.insidethecube.cubework;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The item of a shape's block: one item for every material of the shape and behaviour, the material kept
 * on the stack (CwBlocks.MATERIAL), so "Red Wool Vertical Slab" and "Blue Wool Vertical Slab" are one item
 * with two materials. Its name is the shape's name in that material, as it always was.
 */
public class ShapeItem extends BlockItem {
	private final Shape shape;

	public ShapeItem(Block block, Shape shape, Item.Properties settings) {
		super(block, settings);
		this.shape = shape;
	}

	public Shape shape() {
		return shape;
	}

	/** The stack's material (the block's first one when the stack has none). */
	public Material material(ItemStack stack) {
		Material m = stack.get(CwBlocks.MATERIAL);
		return m != null ? m : CwBlocks.defaultMaterial(getBlock());
	}

	@Override
	public Component getName(ItemStack stack) {
		Material m = material(stack);
		return m == null ? super.getName(stack) : Component.translatable("block." + Cubework.ID + "." + m.idOf(shape));
	}
}
