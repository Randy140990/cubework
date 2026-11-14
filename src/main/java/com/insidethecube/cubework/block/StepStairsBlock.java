package com.insidethecube.cubework.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.insidethecube.cubework.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stairs with more, smaller steps: 4, 6 or 8 to the block, still climbing one block per block. Placed,
 * joined into corners and turned upside down exactly like vanilla stairs (it is vanilla's stairs block);
 * only the steps differ. The hit box follows the steps: it's worked out from where vanilla's stairs of the
 * same state are high (one, two or three quarters of the block), so corners always match the model.
 */
public class StepStairsBlock extends CwStairs {
	public final int steps;
	private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

	public StepStairsBlock(Material material, BlockState base, int steps, BlockBehaviour.Properties settings) {
		super(material, base, settings);
		this.steps = steps;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		VoxelShape s = shapes.get(state);
		if (s == null) {
			s = build(state, super.getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()));
			shapes.put(state, s);
		}
		return s;
	}

	/** Solid all over where the steps are, for hiding neighbours' faces. */
	@Override
	//#if MC >= 1.21.2
	protected VoxelShape getOcclusionShape(BlockState state) {
	//#else
	protected VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
	//#endif
		return getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
	}

	private VoxelShape build(BlockState state, VoxelShape vanilla) {
		boolean top = state.getValue(HALF) == Half.TOP;
		// Which quarters vanilla's stairs are high in: two for straight stairs, one for an outer corner, three for an inner one.
		double y = top ? 0.25 : 0.75;
		boolean[][] high = new boolean[2][2];
		int count = 0;
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				double px = 0.25 + 0.5 * i, pz = 0.25 + 0.5 * j;
				for (AABB b : vanilla.toAabbs()) {
					if (b.minX <= px && px < b.maxX && b.minY <= y && y < b.maxY && b.minZ <= pz && pz < b.maxZ) {
						high[i][j] = true;
						break;
					}
				}
				if (high[i][j]) count++;
			}
		}
		int n = steps;
		double w = 1.0 / n;
		VoxelShape out = Shapes.empty();
		for (int a = 0; a < n; a++) {
			for (int b = 0; b < n; b++) {
				double h = height(high, count, (a + 0.5) * w, (b + 0.5) * w);
				out = Shapes.or(out, top ? Shapes.box(a * w, 1 - h, b * w, (a + 1) * w, 1, (b + 1) * w) : Shapes.box(a * w, 0, b * w, (a + 1) * w, h, (b + 1) * w));
			}
		}
		return out.optimize();
	}

	/** Height of the steps at (x, z), 0 to 1. */
	private double height(boolean[][] high, int count, double x, double z) {
		if (count == 2) {
			// Straight: climbing towards the high half.
			if (high[1][0] && high[1][1]) return step(x);
			if (high[0][0] && high[0][1]) return step(1 - x);
			if (high[0][1] && high[1][1]) return step(z);
			return step(1 - z);
		}
		if (count == 1) {
			// Outer corner: climbing towards the high quarter both ways (the lower of the two).
			int i = high[1][0] || high[1][1] ? 1 : 0, j = high[0][1] || high[1][1] ? 1 : 0;
			return Math.min(step(i == 1 ? x : 1 - x), step(j == 1 ? z : 1 - z));
		}
		if (count == 3) {
			// Inner corner: climbing away from the low quarter both ways (the higher of the two).
			int i = 0, j = 0;
			for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) if (!high[a][b]) {
				i = a;
				j = b;
			}
			return Math.max(step(i == 1 ? 1 - x : x), step(j == 1 ? 1 - z : z));
		}
		return 1;
	}

	/** The height of the step t of the way along the flight. */
	private double step(double t) {
		int k = Math.min(steps - 1, Math.max(0, (int) Math.floor(t * steps)));
		return (k + 1) / (double) steps;
	}
}
