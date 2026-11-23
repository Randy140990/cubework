package com.insidethecube.retromon.game;

import com.insidethecube.retromon.Retromon;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Small changes John asked for to his map itself, made once in a Retromon save when that part of the map is loaded,
 * and only while it's still as he built it (each checks a few of his blocks first). What's done is noted in the world's
 * folder (retromon_map_fixes.txt), so it's never done twice.
 *
 * (0.57, 16:15) The DARK CAVE's way in on Route 46: "I accidentally built it one block too low ... it should be 3 high,
 * not 2", as its Blackthorn way in (Route 45's) is: the doorway, its pillars, the lintel and the stone cap on it all a
 * block higher, the dark inside three high, the dripstone hanging in it moved up with it, the rock over it one higher.
 */
public final class MapFixes {
	private MapFixes() {}

	private static final String FILE = "retromon_map_fixes.txt";
	private static Set<String> done;
	private static MinecraftServer forServer;

	public static void tick(MinecraftServer s) {
		ServerLevel level = s.overworld();
		if (done == null || forServer != s) load(s);
		if (!done.contains("dark_cave_route_46_mouth") && level.hasChunkAt(new BlockPos(-1565, 22, 654)) && level.hasChunkAt(new BlockPos(-1565, 22, 652))) {
			if (darkCaveRoute46(level)) Retromon.LOG.info("Retromon: raised the DARK CAVE's way in on Route 46 to three high");
			mark(s, "dark_cave_route_46_mouth");
		}
	}

	private static void load(MinecraftServer s) {
		forServer = s;
		done = new HashSet<>();
		try {
			Path f = s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve(FILE);
			if (Files.exists(f)) for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) if (!line.isBlank()) done.add(line.trim());
		} catch (IOException e) {
			Retromon.LOG.warn("Couldn't read the map fixes", e);
		}
	}

	private static void mark(MinecraftServer s, String id) {
		done.add(id);
		try {
			Path f = s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve(FILE);
			Files.writeString(f, id + "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException e) {
			Retromon.LOG.warn("Couldn't note the map fix", e);
		}
	}

	private static boolean is(ServerLevel l, int x, int y, int z, Block b) {
		return l.getBlockState(new BlockPos(x, y, z)).is(b);
	}

	/** Sets a block without waking its neighbours (so the hanging dripstone stays where it's put). */
	private static void set(ServerLevel l, int x, int y, int z, BlockState st) {
		l.setBlock(new BlockPos(x, y, z), st, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}

	private static BlockState at(ServerLevel l, int x, int y, int z) {
		return l.getBlockState(new BlockPos(x, y, z));
	}

	/**
	 * The way in: pillars at x -1566 and -1563 (z 653 to 655), the doorway x -1565 and -1564, the dark inside at z 652,
	 * the ground at y 20; the lintel (stone bricks) at y 23 and the stone slab on it at y 24, which go to 24 and 25.
	 */
	private static boolean darkCaveRoute46(ServerLevel l) {
		final int x0 = -1566, x1 = -1565, x2 = -1564, x3 = -1563;
		if (!is(l, x1, 24, 654, Blocks.SMOOTH_STONE_SLAB) || !is(l, x1, 23, 654, Blocks.STONE_BRICKS) || !is(l, x1, 22, 652, Blocks.BLACK_CONCRETE)
				|| !is(l, x1, 23, 652, Blocks.DRIPSTONE_BLOCK) || !l.getBlockState(new BlockPos(x1, 25, 654)).isAir()) return false;
		BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState(), rock = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
		// The stone cap up a block; the rock over the doorway's front and the hill above it a block higher.
		for (int x = x0; x <= x3; x++) {
			for (int z = 654; z <= 655; z++) {
				set(l, x, 25, z, at(l, x, 24, z));
				set(l, x, 24, z, z == 654 ? bricks : air);
			}
			if (at(l, x, 25, 653).isAir()) set(l, x, 25, 653, rock);
			set(l, x, 24, 653, bricks);
		}
		for (int x = x0 - 1; x <= x3 + 1; x++) if (at(l, x, 25, 652).isAir()) set(l, x, 25, 652, rock);
		// The doorway three high: the old lintel gone from it, the pillars a block taller at the front.
		for (int z = 653; z <= 654; z++) {
			set(l, x0, 23, z, bricks);
			set(l, x3, 23, z, bricks);
		}
		set(l, x1, 23, 654, air);
		set(l, x2, 23, 654, air);
		set(l, x0, 22, 655, bricks);
		set(l, x3, 22, 655, bricks);
		// The dark inside up to the lintel.
		set(l, x1, 23, 652, Blocks.BLACK_CONCRETE.defaultBlockState());
		set(l, x2, 23, 652, Blocks.BLACK_CONCRETE.defaultBlockState());
		// The dripstone hanging in the doorway's top, up a block with it (as the Blackthorn side's).
		BlockState a1 = at(l, x1, 21, 653), b1 = at(l, x1, 22, 653), b2 = at(l, x2, 22, 653);
		set(l, x1, 23, 653, b1.is(Blocks.POINTED_DRIPSTONE) ? b1 : air);
		set(l, x2, 23, 653, b2.is(Blocks.POINTED_DRIPSTONE) ? b2 : air);
		set(l, x1, 22, 653, a1.is(Blocks.POINTED_DRIPSTONE) ? a1 : air);
		set(l, x2, 22, 653, air);
		set(l, x1, 21, 653, air);
		return true;
	}
}
