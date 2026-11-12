package com.insidethecube.deepkit.client;

import java.util.ArrayList;
import java.util.List;
import com.insidethecube.deepkit.DK;
import com.insidethecube.deepkit.lamp.LampData;
import com.insidethecube.deepkit.lamp.LampItem;
import com.insidethecube.deepkit.lamp.LampKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lamp beams, drawn by each player's game for every lamp nearby that's on: a soft round pool of light
 * thrown onto whatever the lamp points at, bright in the middle and fading at the edge and with distance,
 * with a little spill round you. Nothing is placed in the world, so the beam follows the lamp smoothly
 * every frame and never disturbs blocks (gravel and sand stay put).
 *
 * How: every tick, rays fanned out across each beam's cone find the blocks it reaches, and their faces
 * towards it are gathered with their textures. Every frame those faces are drawn again over the world,
 * fully lit, see-through by how much light reaches each point (from where the lamp is that very frame),
 * so the lit pool slides and fades smoothly. Works the same with and without shaders.
 */
public final class Beams {
	private Beams() {}

	/** A lamp shining: where from, which way (unit length), half its cone (radians), reach, and strength (0 to 1). */
	private record Lamp(Vec3 from, Vec3 axis, double half, double range, double strength) {
		/** Past the cone, a faint ring of spill. */
		double spill() {
			return half * 1.7;
		}
	}

	/** One textured side (of a block, or of one box of a block's shape) a beam reaches. */
	private record Face(int x, int y, int z, Direction dir, float a0, float a1, float b0, float b1, float c, TextureAtlasSprite sprite, int rgb,
			float lift) {}

	private record Layer(TextureAtlasSprite sprite, int rgb) {}

	/** Rays from the beam's middle to its edge, each way across (so about 350 rays a lamp, each tick). */
	private static final int RAYS = 10;
	/** Each side of a block is lit in a 4 by 4 grid, so the pool's edge is round and soft. */
	private static final int CELLS = 4;
	/** A warm white, like an LED torch. */
	private static final float WARM_R = 1f, WARM_G = 0.95f, WARM_B = 0.84f;
	/** How far away other players' lamps are drawn. */
	private static final double SEEN = 96;

	private static List<Face> faces = List.of();

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(Beams::tick);
		WorldRenderEvents.AFTER_ENTITIES.register(Beams::render);
	}

	// ---------- the lamps ----------

	private static List<Lamp> lamps(Minecraft mc, float pt) {
		List<Lamp> out = new ArrayList<>();
		ClientLevel level = mc.level;
		for (Player p : level.players()) {
			if (p.isSpectator() || p.distanceToSqr(mc.player) > SEEN * SEEN) continue;
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack s = p.getItemInHand(hand);
				if (s.getItem() instanceof LampItem) add(out, p, s.get(DK.LAMP), hand, pt, level);
			}
			add(out, p, p.getItemBySlot(EquipmentSlot.HEAD).get(DK.LAMP), null, pt, level);
			add(out, p, p.getItemBySlot(EquipmentSlot.CHEST).get(DK.LAMP), null, pt, level);
		}
		return out;
	}

	private static void add(List<Lamp> out, Player p, LampData d, InteractionHand hand, float pt, ClientLevel level) {
		if (d == null || !d.on() || d.charge() <= 0) return;
		LampKind kind = d.lamp();
		Vec3 eye = p.getEyePosition(pt), look = p.getViewVector(pt);
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
		Vec3 from, axis;
		switch (kind) {
			case HEAD -> {
				// On the forehead, pointing where you look.
				from = eye.add(0, 0.22, 0).add(look.scale(0.3));
				axis = look;
			}
			case CHEST -> {
				// On the chest, pointing where the body faces and a little down.
				float yaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
				axis = Vec3.directionFromRotation(10f, yaw);
				from = p.getPosition(pt).add(0, p.getBbHeight() * 0.68, 0).add(axis.scale(0.3));
			}
			default -> {
				// In the hand, beside you, pointing where you look.
				boolean rightSide = (hand == InteractionHand.MAIN_HAND) == (p.getMainArm() == HumanoidArm.RIGHT);
				from = eye.add(right.scale(rightSide ? 0.32 : -0.32)).add(0, -0.28, 0).add(look.scale(0.45));
				axis = look;
			}
		}
		double range = kind.range * (p.isEyeInFluid(FluidTags.WATER) ? 2 / 3.0 : 1);
		double strength = 1;
		if (d.percent() <= 10) {
			// A low battery flickers, changing every tick.
			long h = (level.getGameTime() * 31 + p.getId()) * 0x9E3779B97F4A7C15L;
			strength = 0.4 + 0.6 * ((h >>> 40) & 0xFF) / 255.0;
		}
		out.add(new Lamp(from, axis.normalize(), Math.toRadians(kind.cone) / 2, range, strength));
	}

	// ---------- each tick: what the beams reach ----------

	private static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			faces = List.of();
			return;
		}
		List<Lamp> lamps = lamps(mc, 1f);
		if (lamps.isEmpty()) {
			faces = List.of();
			return;
		}
		LongOpenHashSet hits = new LongOpenHashSet();
		for (Lamp l : lamps) cast(mc.level, mc.player, l, hits);
		faces = faces(mc, hits);
	}

	/** Fans rays across a beam (a little wider than it, so a moving lamp never outruns them). */
	private static void cast(ClientLevel level, Player viewer, Lamp l, LongOpenHashSet hits) {
		Vec3 a = l.axis();
		Vec3 u = a.cross(new Vec3(0, 1, 0));
		u = u.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : u.normalize();
		Vec3 v = u.cross(a).normalize();
		double t = Math.tan(Math.min(1.3, l.spill() * 1.2));
		for (int i = -RAYS; i <= RAYS; i++) {
			for (int j = -RAYS; j <= RAYS; j++) {
				double x = i / (double) RAYS, y = j / (double) RAYS;
				if (x * x + y * y > 1) continue;
				Vec3 dir = a.add(u.scale(x * t)).add(v.scale(y * t)).normalize();
				Vec3 end = l.from().add(dir.scale(l.range()));
				BlockHitResult hit = level.clip(new ClipContext(l.from(), end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
				if (hit.getType() != HitResult.Type.BLOCK) continue;
				BlockPos p = hit.getBlockPos();
				hits.add(p.asLong());
				// Far off the rays spread out, so take the blocks round each hit in the same surface too.
				if (hit.getLocation().distanceToSqr(l.from()) > 100) {
					for (Direction d : Direction.values()) if (d.getAxis() != hit.getDirection().getAxis()) hits.add(p.relative(d).asLong());
				}
			}
		}
	}

	private static List<Face> faces(Minecraft mc, LongOpenHashSet hits) {
		ClientLevel level = mc.level;
		List<Face> out = new ArrayList<>();
		BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
		LongIterator it = hits.iterator();
		while (it.hasNext()) {
			BlockPos pos = BlockPos.of(it.nextLong());
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
			// Plants, torches and the like (nothing to stand on) are left as they are.
			if (state.getCollisionShape(level, pos).isEmpty()) continue;
			VoxelShape shape = state.getShape(level, pos);
			if (shape.isEmpty()) continue;
			List<AABB> boxes = shape.toAabbs();
			// Complicated shapes too (they'd light as boxes that don't match what's drawn).
			if (boxes.size() > 6) continue;
			BlockStateModel model = mc.getBlockRenderer().getBlockModel(state);
			List<BlockModelPart> parts = model.collectParts(RandomSource.create(state.getSeed(pos)));
			for (Direction d : Direction.values()) {
				List<Layer> layers = null;
				for (AABB box : boxes) {
					boolean edge = switch (d) {
						case UP -> box.maxY >= 1 - 1e-4;
						case DOWN -> box.minY <= 1e-4;
						case NORTH -> box.minZ <= 1e-4;
						case SOUTH -> box.maxZ >= 1 - 1e-4;
						case WEST -> box.minX <= 1e-4;
						case EAST -> box.maxX >= 1 - 1e-4;
					};
					// A side against a solid block can't be seen.
					if (edge && level.getBlockState(next.setWithOffset(pos, d)).isSolidRender()) continue;
					float a0, a1, b0, b1, c;
					switch (d.getAxis()) {
						case Y -> {
							a0 = (float) box.minX; a1 = (float) box.maxX; b0 = (float) box.minZ; b1 = (float) box.maxZ;
							c = (float) (d == Direction.UP ? box.maxY : box.minY);
						}
						case Z -> {
							a0 = (float) box.minX; a1 = (float) box.maxX; b0 = (float) box.minY; b1 = (float) box.maxY;
							c = (float) (d == Direction.SOUTH ? box.maxZ : box.minZ);
						}
						default -> {
							a0 = (float) box.minZ; a1 = (float) box.maxZ; b0 = (float) box.minY; b1 = (float) box.maxY;
							c = (float) (d == Direction.EAST ? box.maxX : box.minX);
						}
					}
					if (layers == null) layers = layers(mc, level, state, pos, model, parts, d);
					int n = 0;
					for (Layer l : layers) out.add(new Face(pos.getX(), pos.getY(), pos.getZ(), d, a0, a1, b0, b1, c, l.sprite(), l.rgb(), 0.003f + 0.001f * n++));
				}
			}
		}
		return out;
	}

	/** The textures on one side of a block, tinted as the game tints them (grass and its green edge, leaves). */
	private static List<Layer> layers(Minecraft mc, ClientLevel level, BlockState state, BlockPos pos, BlockStateModel model, List<BlockModelPart> parts,
			Direction d) {
		List<Layer> out = new ArrayList<>(2);
		for (BlockModelPart part : parts) {
			for (BakedQuad q : part.getQuads(d)) layer(out, mc, level, state, pos, q);
			for (BakedQuad q : part.getQuads(null)) if (q.direction() == d) layer(out, mc, level, state, pos, q);
		}
		if (out.isEmpty()) out.add(new Layer(model.particleIcon(), 0xFFFFFF));
		return out;
	}

	private static void layer(List<Layer> out, Minecraft mc, ClientLevel level, BlockState state, BlockPos pos, BakedQuad q) {
		if (out.size() >= 2) return;
		for (Layer l : out) if (l.sprite() == q.sprite()) return;
		int rgb = q.tintIndex() >= 0 ? mc.getBlockColors().getColor(state, level, pos, q.tintIndex()) : -1;
		out.add(new Layer(q.sprite(), rgb == -1 ? 0xFFFFFF : rgb & 0xFFFFFF));
	}

	// ---------- each frame: the light ----------

	private static void render(WorldRenderContext ctx) {
		List<Face> list = faces;
		if (list.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		List<Lamp> lamps = lamps(mc, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
		if (lamps.isEmpty()) return;
		Vec3 cam = ctx.worldState().cameraRenderState.pos;
		PoseStack.Pose pose = ctx.matrices().last();
		VertexConsumer vc = ctx.consumers().getBuffer(RenderTypes.entityTranslucent(list.get(0).sprite().atlasLocation()));
		float[] grid = new float[(CELLS + 1) * (CELLS + 1)];
		for (Face f : list) draw(vc, pose, cam, f, lamps, grid);
	}

	private static void draw(VertexConsumer vc, PoseStack.Pose pose, Vec3 cam, Face f, List<Lamp> lamps, float[] grid) {
		Direction d = f.dir();
		float nx = d.getStepX(), ny = d.getStepY(), nz = d.getStepZ();
		boolean any = false;
		for (int j = 0; j <= CELLS; j++) {
			for (int i = 0; i <= CELLS; i++) {
				float a = f.a0() + (f.a1() - f.a0()) * i / CELLS, b = f.b0() + (f.b1() - f.b0()) * j / CELLS;
				float lit = (float) light(lamps, f.x() + lx(f, a, b), f.y() + ly(f, a, b), f.z() + lz(f, a, b), nx, ny, nz);
				grid[j * (CELLS + 1) + i] = lit;
				any |= lit > 0.004f;
			}
		}
		if (!any) return;
		int r = (int) (((f.rgb() >> 16) & 0xFF) * WARM_R), g = (int) (((f.rgb() >> 8) & 0xFF) * WARM_G), bl = (int) ((f.rgb() & 0xFF) * WARM_B);
		for (int j = 0; j < CELLS; j++) {
			for (int i = 0; i < CELLS; i++) {
				float l00 = grid[j * (CELLS + 1) + i], l10 = grid[j * (CELLS + 1) + i + 1];
				float l01 = grid[(j + 1) * (CELLS + 1) + i], l11 = grid[(j + 1) * (CELLS + 1) + i + 1];
				if (Math.max(Math.max(l00, l10), Math.max(l01, l11)) <= 0.004f) continue;
				float a0 = f.a0() + (f.a1() - f.a0()) * i / CELLS, a1 = f.a0() + (f.a1() - f.a0()) * (i + 1) / CELLS;
				float b0 = f.b0() + (f.b1() - f.b0()) * j / CELLS, b1 = f.b0() + (f.b1() - f.b0()) * (j + 1) / CELLS;
				vertex(vc, pose, cam, f, a0, b0, l00, r, g, bl, nx, ny, nz);
				vertex(vc, pose, cam, f, a1, b0, l10, r, g, bl, nx, ny, nz);
				vertex(vc, pose, cam, f, a1, b1, l11, r, g, bl, nx, ny, nz);
				vertex(vc, pose, cam, f, a0, b1, l01, r, g, bl, nx, ny, nz);
			}
		}
	}

	// A point on a face, from its two in-plane coordinates (a, b) and the plane (c), within its block.
	private static float lx(Face f, float a, float b) {
		return switch (f.dir().getAxis()) {
			case X -> f.c();
			default -> a;
		};
	}

	private static float ly(Face f, float a, float b) {
		return switch (f.dir().getAxis()) {
			case Y -> f.c();
			default -> b;
		};
	}

	private static float lz(Face f, float a, float b) {
		return switch (f.dir().getAxis()) {
			case Z -> f.c();
			case Y -> b;
			default -> a;
		};
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 cam, Face f, float a, float b, float lit, int r, int g, int bl, float nx, float ny,
			float nz) {
		float x = lx(f, a, b), y = ly(f, a, b), z = lz(f, a, b);
		// The game's own texture layout for each side of a block.
		float u, v;
		switch (f.dir()) {
			case UP -> { u = x; v = z; }
			case DOWN -> { u = x; v = 1 - z; }
			case NORTH -> { u = 1 - x; v = 1 - y; }
			case SOUTH -> { u = x; v = 1 - y; }
			case WEST -> { u = z; v = 1 - y; }
			default -> { u = 1 - z; v = 1 - y; }
		}
		TextureAtlasSprite s = f.sprite();
		float su = s.getU0() + (s.getU1() - s.getU0()) * u, sv = s.getV0() + (s.getV1() - s.getV0()) * v;
		int alpha = (int) (Math.min(1f, lit) * 235);
		vc.addVertex(pose, (float) (f.x() - cam.x + x + nx * f.lift()), (float) (f.y() - cam.y + y + ny * f.lift()), (float) (f.z() - cam.z + z + nz * f.lift()))
				.setColor(r, g, bl, alpha).setUv(su, sv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
	}

	/** How much light reaches a point on a surface facing (nx, ny, nz), from every lamp (0 to 1). */
	private static double light(List<Lamp> lamps, double px, double py, double pz, float nx, float ny, float nz) {
		double total = 0;
		for (Lamp l : lamps) {
			double dx = px - l.from().x, dy = py - l.from().y, dz = pz - l.from().z;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (dist < 1e-3 || dist > l.range()) continue;
			dx /= dist;
			dy /= dist;
			dz /= dist;
			double facing = -(dx * nx + dy * ny + dz * nz);
			if (facing <= 0) continue;
			facing = 0.4 + 0.6 * facing;
			// A little spill all round, close by.
			double near = dist < 3 ? 0.22 * (1 - dist / 3) : 0;
			double ang = Math.acos(Math.max(-1, Math.min(1, dx * l.axis().x + dy * l.axis().y + dz * l.axis().z)));
			double inner = l.half() * 0.5, spot;
			if (ang <= inner) spot = 1;
			else if (ang <= l.half()) spot = 1 - 0.65 * smooth((ang - inner) / (l.half() - inner));
			else if (ang < l.spill()) spot = 0.35 * (1 - smooth((ang - l.half()) / (l.spill() - l.half())));
			else spot = 0;
			double far = dist / l.range(), fall = (1 - far * far) * (1 - far * far);
			total += (spot * fall + near) * facing * l.strength();
		}
		return Math.min(1, total);
	}

	private static double smooth(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}
}
