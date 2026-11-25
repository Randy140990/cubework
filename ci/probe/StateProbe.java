import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * (0.67, John's 16:07: "the mod is getting really slow to start") How long Minecraft takes to make a block with one
 * number property of N values (the rooms' block has 37,644), and how much memory it holds: printed as numbers only.
 */
public final class StateProbe {
	public static void main(String[] args) {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Runtime rt = Runtime.getRuntime();
		System.out.println("max heap MB " + rt.maxMemory() / 1048576);
		for (int size : new int[] {1000, 2000, 4000, 8000, 12000, 16000}) {
			System.gc();
			long m0 = rt.totalMemory() - rt.freeMemory(), t0 = System.nanoTime();
			// (The states as a block makes them: Block's own StateDefinition, owned by stone, nothing registered.)
			StateDefinition<Block, BlockState> def = new StateDefinition.Builder<Block, BlockState>(Blocks.STONE).add(IntegerProperty.create("m", 0, size - 1))
					.create(Block::defaultBlockState, BlockState::new);
			long t1 = System.nanoTime();
			System.gc();
			long m1 = rt.totalMemory() - rt.freeMemory();
			System.out.println("states " + def.getPossibleStates().size() + "  ms " + (t1 - t0) / 1000000 + "  MB held " + (m1 - m0) / 1048576);
		}
	}
}
