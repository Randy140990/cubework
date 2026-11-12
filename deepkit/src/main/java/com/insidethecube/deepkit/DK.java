package com.insidethecube.deepkit;

import java.util.function.Function;
import com.insidethecube.deepkit.flare.FlareColour;
import com.insidethecube.deepkit.flare.FlareEntity;
import com.insidethecube.deepkit.flare.FlareItem;
import com.insidethecube.deepkit.flare.LitFlareItem;
import com.insidethecube.deepkit.lamp.BeamLightBlock;
import com.insidethecube.deepkit.lamp.LampData;
import com.insidethecube.deepkit.lamp.LampItem;
import com.insidethecube.deepkit.lamp.LampKind;
import com.insidethecube.deepkit.lamp.LampRecipe;
import com.insidethecube.deepkit.paint.GlowMarkBlock;
import com.insidethecube.deepkit.paint.GlowPaintItem;
import com.insidethecube.deepkit.paint.PaintColour;
import com.insidethecube.deepkit.rope.GrapplingHookItem;
import com.insidethecube.deepkit.rope.HookEntity;
import com.insidethecube.deepkit.rope.RopeBlock;
import com.insidethecube.deepkit.rope.RopeCoilItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Everything Deepkit adds to the game's registries. */
public final class DK {
	private DK() {}

	public static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath(Deepkit.ID, name);
	}

	// Data kept on items.
	public static DataComponentType<LampData> LAMP;
	/** When a lit flare burns out (game time). */
	public static DataComponentType<Long> BURN_UNTIL;
	/** The mark a can of glow paint paints. */
	public static DataComponentType<Integer> MARK;

	// Blocks.
	public static BeamLightBlock BEAM_LIGHT;
	public static RopeBlock ROPE;
	public static final GlowMarkBlock[] MARKS = new GlowMarkBlock[PaintColour.values().length];

	// Items.
	public static LampItem HAND_TORCH, HEADLAMP, CHEST_LAMP;
	public static final FlareItem[] FLARES = new FlareItem[FlareColour.values().length];
	public static final LitFlareItem[] LIT_FLARES = new LitFlareItem[FlareColour.values().length];
	public static final GlowPaintItem[] PAINTS = new GlowPaintItem[PaintColour.values().length];
	public static RopeCoilItem ROPE_COIL;
	public static GrapplingHookItem GRAPPLING_HOOK;

	// Entities.
	public static EntityType<FlareEntity> FLARE;
	public static EntityType<HookEntity> HOOK;

	public static RecipeSerializer<LampRecipe> LAMP_RECIPE;

	public static void register() {
		LAMP = component("lamp", LampData.CODEC, LampData.STREAM_CODEC);
		BURN_UNTIL = component("burn_until", Codec.LONG, ByteBufCodecs.VAR_LONG);
		MARK = component("mark", Codec.INT, ByteBufCodecs.VAR_INT);

		BEAM_LIGHT = block("beam_light", BlockBehaviour.Properties.of().replaceable().noCollision().noOcclusion().noLootTable().instabreak()
				.randomTicks().pushReaction(PushReaction.DESTROY).lightLevel(s -> s.getValue(BeamLightBlock.LEVEL)), BeamLightBlock::new);
		ROPE = block("rope", BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollision().noOcclusion().noLootTable().strength(0.4f)
				.sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY), RopeBlock::new);
		for (PaintColour c : PaintColour.values()) {
			MARKS[c.ordinal()] = block(c.name + "_glow_mark", BlockBehaviour.Properties.of().replaceable().noCollision().noOcclusion().noLootTable()
					.instabreak().sound(SoundType.SLIME_BLOCK).pushReaction(PushReaction.DESTROY).lightLevel(s -> 3), p -> new GlowMarkBlock(c, p));
		}

		HAND_TORCH = item("hand_torch", new Item.Properties(), p -> new LampItem(LampKind.TORCH, p));
		HEADLAMP = item("headlamp", new Item.Properties(), p -> new LampItem(LampKind.HEAD, p));
		CHEST_LAMP = item("chest_lamp", new Item.Properties(), p -> new LampItem(LampKind.CHEST, p));
		for (FlareColour c : FlareColour.values()) {
			FLARES[c.ordinal()] = item(c.item, new Item.Properties().stacksTo(16), p -> new FlareItem(c, p));
			LIT_FLARES[c.ordinal()] = item("lit_" + c.item, new Item.Properties().stacksTo(1), p -> new LitFlareItem(c, p));
		}
		for (PaintColour c : PaintColour.values()) {
			PAINTS[c.ordinal()] = item(c.name + "_glow_paint", new Item.Properties().durability(64), p -> new GlowPaintItem(c, p));
		}
		ROPE_COIL = item("rope_coil", new Item.Properties().stacksTo(16), RopeCoilItem::new);
		GRAPPLING_HOOK = item("grappling_hook", new Item.Properties().stacksTo(1), GrapplingHookItem::new);

		FLARE = entity("flare", EntityType.Builder.<FlareEntity>of(FlareEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(8)
				.updateInterval(5));
		HOOK = entity("grappling_hook", EntityType.Builder.<HookEntity>of(HookEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(8)
				.updateInterval(2));

		LAMP_RECIPE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("lamp"), new CustomRecipe.Serializer<>(LampRecipe::new));
	}

	private static <T> DataComponentType<T> component(String name, Codec<T> codec,
			net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, T> stream) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(name),
				DataComponentType.<T>builder().persistent(codec).networkSynchronized(stream).build());
	}

	private static <B extends Block> B block(String name, BlockBehaviour.Properties props, Function<BlockBehaviour.Properties, B> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
	}

	private static <I extends Item> I item(String name, Item.Properties props, Function<Item.Properties, I> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
