package com.insidethecube.cubework.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.insidethecube.cubework.CwBlocks;
import com.insidethecube.cubework.Material;
import com.insidethecube.cubework.Reg;
import com.insidethecube.cubework.Shape;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GrassColor;
import org.joml.Vector3fc;

/**
 * A shape's item: the stack's material picks its family's look (as MaterialModels does for placed shapes),
 * drawn in the material's textures. Written in the item's definition as
 * {"type": "cubework:material", "looks": {"plain": {"models": [...], "tint": false}, ...}}.
 */
public final class MaterialItemModel implements ItemModel {
	public static final Identifier ID = Reg.id("material");

	/** One family's look: its models (drawn together, like a composite) and whether it takes grass's colour. */
	public record Look(List<Identifier> models, boolean tint) {
		public static final Codec<Look> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.listOf().fieldOf("models").forGetter(Look::models),
				Codec.BOOL.optionalFieldOf("tint", false).forGetter(Look::tint)).apply(i, Look::new));
	}

	public record Unbaked(Map<String, Look> looks) implements ItemModel.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.unboundedMap(Codec.STRING, Look.CODEC).fieldOf("looks").forGetter(Unbaked::looks)).apply(i, Unbaked::new));

		@Override
		public void resolveDependencies(ResolvableModel.Resolver resolver) {
			for (Look look : looks.values()) for (Identifier id : look.models()) resolver.markDependency(id);
		}

		@Override
		public ItemModel bake(ItemModel.BakingContext context) {
			ModelBaker baker = context.blockModelBaker();
			MaterialModels.Sprites sprites = MaterialModels.Sprites.current.load(baker);
			Map<MaterialModels.Family, Baked> baked = new EnumMap<>(MaterialModels.Family.class);
			Baked first = null;
			for (Map.Entry<String, Look> e : looks.entrySet()) {
				MaterialModels.Family family = MaterialModels.Family.byId(e.getKey());
				if (family == null) continue;
				List<Layer> layers = new ArrayList<>();
				for (Identifier id : e.getValue().models()) {
					ResolvedModel model = baker.getModel(id);
					TextureSlots slots = model.getTopTextureSlots();
					List<BakedQuad> quads = model.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY).getAll();
					layers.add(new Layer(quads, ModelRenderProperties.fromResolvedModel(baker, model, slots), Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads)),
							new ConcurrentHashMap<>()));
				}
				Baked b = new Baked(layers, e.getValue().tint());
				baked.put(family, b);
				if (first == null || family == MaterialModels.Family.PLAIN) first = b;
			}
			return new MaterialItemModel(baked, first, sprites);
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}

	private record Layer(List<BakedQuad> quads, ModelRenderProperties properties, Supplier<Vector3fc[]> extents, Map<Material, List<BakedQuad>> retextured) {}

	private record Baked(List<Layer> layers, boolean tint) {}

	private static final int GRASS = ARGB.opaque(GrassColor.get(0.5, 1.0));

	private final Map<MaterialModels.Family, Baked> looks;
	private final Baked fallback;
	private final MaterialModels.Sprites sprites;

	private MaterialItemModel(Map<MaterialModels.Family, Baked> looks, Baked fallback, MaterialModels.Sprites sprites) {
		this.looks = looks;
		this.fallback = fallback;
		this.sprites = sprites;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, ClientLevel level, ItemOwner owner,
			int seed) {
		if (fallback == null) return;
		Material m = CwBlocks.materialOf(stack);
		Shape shape = CwBlocks.shapeOf(stack);
		Baked look = m == null || shape == null ? fallback : looks.getOrDefault(MaterialModels.Family.of(m, shape), fallback);
		TextureAtlasSprite[] mat = m == null ? null : sprites.of(m);
		state.appendModelIdentityElement(this);
		state.appendModelIdentityElement(m == null ? "none" : m);
		RenderType type = renderType(stack);
		for (Layer layer : look.layers()) {
			ItemStackRenderState.LayerRenderState l = state.newLayer();
			if (stack.hasFoil()) {
				l.setFoilType(ItemStackRenderState.FoilType.STANDARD);
				state.setAnimated();
				state.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
			}
			if (look.tint()) l.prepareTintLayers(1)[0] = GRASS;
			l.setExtents(layer.extents());
			l.setRenderType(type);
			layer.properties().applyToLayer(l, context);
			List<BakedQuad> quads = mat == null ? layer.quads() : layer.retextured().computeIfAbsent(m, k -> MaterialModels.retexture(layer.quads(), sprites, mat));
			l.prepareQuadList().addAll(quads);
			if (mat != null) {
				l.setParticleIcon(mat[MaterialModels.SIDE]);
				for (TextureAtlasSprite s : mat) if (s != null && s.contents().isAnimated()) state.setAnimated();
			}
		}
	}

	/** See-through shapes (glass, ice) draw see-through in the hand too; the rest cut out, as vanilla's block items. */
	private static RenderType renderType(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem bi && ItemBlockRenderTypes.getChunkRenderType(bi.getBlock().defaultBlockState()) == ChunkSectionLayer.TRANSLUCENT) {
			return Sheets.translucentBlockItemSheet();
		}
		return Sheets.cutoutBlockSheet();
	}
}
