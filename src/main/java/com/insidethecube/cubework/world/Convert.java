package com.insidethecube.cubework.world;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Reg;
import com.insidethecube.cubework.block.ShapeEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

/**
 * Converts worlds from before 0.12. Then every material's shapes were blocks and items of their own
 * (cubework:red_wool_slab); now a shape is one block for all the materials of its behaviour
 * (cubework:slab_wool), each placed block keeping its material in a block entity and each stack on the
 * stack. As a chunk loads, its old shape blocks are renamed, keeping how they were placed, and given a
 * block entity with their material; old shape items, wherever they are (chests, item frames, dropped,
 * players' inventories, shulker boxes inside those), are renamed and given their material. The world saves
 * in the new form as it's played; nothing old is lost if it never is.
 */
public final class Convert {
	private Convert() {}

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String SHAPE_ENTITY = "cubework:shape";
	private static final String MATERIAL_COMPONENT = "cubework:material";

	/** A chunk's tag, before the game reads it. */
	public static void chunk(CompoundTag chunk) {
		if (CwBlocks.OLD.isEmpty()) return;
		int cx = chunk.getIntOr("xPos", 0), cz = chunk.getIntOr("zPos", 0);
		ListTag entities = chunk.getList("block_entities").orElse(null);
		Set<Long> taken = new HashSet<>();
		if (entities != null) {
			for (Tag t : entities) {
				if (t instanceof CompoundTag be) taken.add(key(be.getIntOr("x", 0), be.getIntOr("y", 0), be.getIntOr("z", 0)));
			}
		}
		int added = 0;
		for (Tag t : chunk.getListOrEmpty("sections")) {
			if (!(t instanceof CompoundTag section)) continue;
			CompoundTag states = section.getCompound("block_states").orElse(null);
			if (states == null) continue;
			ListTag palette = states.getListOrEmpty("palette");
			Material[] materials = null;
			for (int i = 0; i < palette.size(); i++) {
				CompoundTag entry = palette.getCompound(i).orElse(null);
				if (entry == null) continue;
				CwBlocks.Old old = CwBlocks.OLD.get(entry.getStringOr("Name", ""));
				if (old == null) continue;
				entry.putString("Name", Reg.idOf(old.block()).toString());
				if (materials == null) materials = new Material[palette.size()];
				materials[i] = old.material();
			}
			if (materials == null) continue;
			if (entities == null) {
				entities = new ListTag();
				chunk.put("block_entities", entities);
			}
			int sy = section.getByteOr("Y", (byte) 0);
			long[] data = states.getLongArray("data").orElse(null);
			if (palette.size() == 1 || data == null) {
				if (materials[0] == null) continue;
				for (int i = 0; i < 4096; i++) added += add(entities, taken, cx, sy, cz, i, materials[0]);
				continue;
			}
			// Packed palette indices, as the game writes them: at least 4 bits each, none split between longs.
			int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
			int perLong = 64 / bits;
			if (data.length != (4096 + perLong - 1) / perLong) {
				LOGGER.warn("Cubework: couldn't read a section of chunk {}, {} to convert its shapes", cx, cz);
				continue;
			}
			long mask = (1L << bits) - 1;
			for (int i = 0; i < 4096; i++) {
				int v = (int) (data[i / perLong] >>> (i % perLong * bits) & mask);
				if (v < materials.length && materials[v] != null) added += add(entities, taken, cx, sy, cz, i, materials[v]);
			}
		}
		if (entities != null) items(entities);
		items(chunk.getListOrEmpty("entities"));
		for (Tag t : chunk.getListOrEmpty("block_ticks")) {
			if (t instanceof CompoundTag tick) {
				CwBlocks.Old old = CwBlocks.OLD.get(tick.getStringOr("i", ""));
				if (old != null) tick.putString("i", Reg.idOf(old.block()).toString());
			}
		}
		if (added > 0) LOGGER.debug("Cubework: converted {} shapes in chunk {}, {}", added, cx, cz);
	}

	private static long key(int x, int y, int z) {
		return (long) x & 0x3FFFFFFL | ((long) z & 0x3FFFFFFL) << 26 | ((long) y & 0xFFFL) << 52;
	}

	/** A block entity with the material for the block at index i of a section (y, z, x order). */
	private static int add(ListTag entities, Set<Long> taken, int cx, int sy, int cz, int i, Material m) {
		int x = cx * 16 + (i & 15), z = cz * 16 + (i >> 4 & 15), y = sy * 16 + (i >> 8);
		if (!taken.add(key(x, y, z))) return 0;
		CompoundTag be = new CompoundTag();
		be.putString("id", SHAPE_ENTITY);
		be.putInt("x", x);
		be.putInt("y", y);
		be.putInt("z", z);
		be.putString(ShapeEntity.KEY, m.id());
		entities.add(be);
		return 1;
	}

	/** Old shape items anywhere in a tag (and old shape blocks held by entities, without their material). */
	public static void items(Tag tag) {
		if (tag instanceof CompoundTag c) {
			CwBlocks.Old old = CwBlocks.OLD.get(c.getStringOr("id", ""));
			if (old != null) {
				c.putString("id", Reg.idOf(old.block()).toString());
				CompoundTag components = c.getCompound("components").orElse(null);
				if (components == null) {
					components = new CompoundTag();
					c.put("components", components);
				}
				components.putString(MATERIAL_COMPONENT, old.material().id());
			}
			CwBlocks.Old block = CwBlocks.OLD.get(c.getStringOr("Name", ""));
			if (block != null) c.putString("Name", Reg.idOf(block.block()).toString());
			for (String k : List.copyOf(c.keySet())) {
				Tag child = c.get(k);
				if (child instanceof CompoundTag || child instanceof ListTag) items(child);
			}
		} else if (tag instanceof ListTag list) {
			for (Tag t : list) if (t instanceof CompoundTag || t instanceof ListTag) items(t);
		}
	}
}
