package com.insidethecube.cubework.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A light fitting made from a light block (glowstone, sea lantern, a froglight), fixed to the face you
 * click: a light rod with a metal bracket, a panel light (a square of light in a border, a quarter to a
 * whole block deep), a tube light (a strip of light in a border, joining end to end), or a recessed light
 * (a round opening with a 2-pixel lip, the light set behind it). A dye on the border recolours it (white
 * concrete to start with). Models are built facing up (fixed to the floor) and turned like end rods.
 */
public class LightFixtureBlock extends Block {
	public enum Kind {
		ROD, PANEL, TUBE, RECESSED
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final EnumProperty<DyeColor> COLOUR = EnumProperty.create("colour", DyeColor.class);
	/** Tube lights: turned a quarter turn about their face (along the other way). */
	public static final BooleanProperty ACROSS = BooleanProperty.create("across");

	private final Kind kind;
	/** Panel: the light's width and the depth; tube: the strip's width; recessed: the opening's width (pixels). */
	private final int size, depth;
	private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
	private final Map<Direction, VoxelShape> acrossShapes = new EnumMap<>(Direction.class);

	public LightFixtureBlock(Kind kind, int size, int depth, BlockBehaviour.Properties settings) {
		super(settings);
		this.kind = kind;
		this.size = size;
		this.depth = depth;
		BlockState def = defaultBlockState().setValue(FACING, Direction.DOWN);
		if (kind != Kind.ROD) def = def.setValue(COLOUR, DyeColor.WHITE);
		if (kind == Kind.TUBE) def = def.setValue(ACROSS, false);
		registerDefaultState(def);
		for (Direction d : Direction.values()) {
			shapes.put(d, shape(boxes(false), d));
			if (kind == Kind.TUBE) acrossShapes.put(d, shape(boxes(true), d));
		}
	}

	/**
	 * The block's properties depend on its kind, but they're declared before the constructor runs; every
	 * fitting gets all three and only uses its own (the rest stay at their defaults and have no models).
	 */
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, COLOUR, ACROSS);
	}

	/** Its boxes facing up, in pixels {x0, y0, z0, x1, y1, z1}. */
	private List<double[]> boxes(boolean across) {
		List<double[]> out = new ArrayList<>();
		switch (kind) {
			case ROD -> {
				out.add(new double[] {6, 0, 6, 10, 2, 10});
				out.add(new double[] {7, 2, 7, 9, 16, 9});
			}
			case PANEL -> out.add(new double[] {0, 0, 0, 16, depth, 16});
			case TUBE -> {
				double a = (16 - size) / 2.0, b = 16 - a;
				out.add(across ? new double[] {a, 0, 0, b, 2, 16} : new double[] {0, 0, a, 16, 2, b});
			}
			case RECESSED -> out.add(new double[] {0, 0, 0, 16, 4, 16});
		}
		return out;
	}

	/** Turns boxes made facing up to face d (as the blockstate turns the model: x then y). */
	private static VoxelShape shape(List<double[]> boxes, Direction d) {
		VoxelShape out = Shapes.empty();
		for (double[] b : boxes) {
			double[] p = point(b[0] / 16, b[1] / 16, b[2] / 16, d), q = point(b[3] / 16, b[4] / 16, b[5] / 16, d);
			out = Shapes.or(out, Shapes.box(Math.min(p[0], q[0]), Math.min(p[1], q[1]), Math.min(p[2], q[2]),
					Math.max(p[0], q[0]), Math.max(p[1], q[1]), Math.max(p[2], q[2])));
		}
		return out.optimize();
	}

	private static double[] point(double x, double y, double z, Direction d) {
		if (d == Direction.UP) return new double[] {x, y, z};
		if (d == Direction.DOWN) return new double[] {x, 1 - y, 1 - z};
		// Tipped to face north (x 90: up becomes north), then turned clockwise to face the others.
		double px = x, py = z, pz = 1 - y;
		int turns = switch (d) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int t = 0; t < turns; t++) {
			double nx = 1 - pz;
			pz = px;
			px = nx;
		}
		return new double[] {px, py, pz};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		Direction d = state.getValue(FACING);
		return kind == Kind.TUBE && state.getValue(ACROSS) ? acrossShapes.get(d) : shapes.get(d);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction face = ctx.getClickedFace();
		BlockState state = defaultBlockState().setValue(FACING, face);
		if (kind == Kind.TUBE) {
			// On a floor or ceiling it runs the way you face; on a wall, along the wall (sneak for up and down).
			boolean across = face.getAxis().isVertical()
					? ctx.getHorizontalDirection().getAxis() == Direction.Axis.Z
					: ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown();
			state = state.setValue(ACROSS, across);
		}
		return state;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (kind == Kind.ROD || !(stack.getItem() instanceof DyeItem dye)) return super.useItemOn(stack, state, world, pos, player, hand, hit);
		DyeColor colour = dye.getDyeColor();
		if (state.getValue(COLOUR) == colour) return InteractionResult.PASS;
		world.setBlock(pos, state.setValue(COLOUR, colour), Block.UPDATE_ALL);
		world.playSound(player, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.6f, 1.2f);
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState out = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
		boolean quarter = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
		if (kind == Kind.TUBE && quarter && state.getValue(FACING).getAxis().isVertical()) out = out.setValue(ACROSS, !state.getValue(ACROSS));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
