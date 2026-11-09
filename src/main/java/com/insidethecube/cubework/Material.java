package com.insidethecube.cubework;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * A vanilla block that Cubework makes every shape of. Its shapes copy its properties (sound, hardness,
 * tool, colour on maps) and use its texture, so a texture pack that changes the block changes them too.
 */
public record Material(String id, Kind kind, String colour) {

	public enum Kind {
		WOOL("wool"), TERRACOTTA("terracotta"), CONCRETE("concrete"), POWDER("concrete_powder"), GLAZED("glazed_terracotta"), GLASS("glass");

		/** Name of its creative tab. */
		public final String tab;

		Kind(String tab) {
			this.tab = tab;
		}
	}

	/** The 16 dye colours, in the order the game's creative menu uses. */
	public static final String[] COLOURS = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan",
			"light_blue", "blue", "purple", "magenta", "pink"};

	public static final List<Material> ALL = build();

	private static List<Material> build() {
		List<Material> out = new ArrayList<>();
		for (String c : COLOURS) out.add(new Material(c + "_wool", Kind.WOOL, c));
		out.add(new Material("terracotta", Kind.TERRACOTTA, null));
		for (String c : COLOURS) out.add(new Material(c + "_terracotta", Kind.TERRACOTTA, c));
		for (String c : COLOURS) out.add(new Material(c + "_concrete", Kind.CONCRETE, c));
		for (String c : COLOURS) out.add(new Material(c + "_concrete_powder", Kind.POWDER, c));
		for (String c : COLOURS) out.add(new Material(c + "_glazed_terracotta", Kind.GLAZED, c));
		out.add(new Material("glass", Kind.GLASS, null));
		out.add(new Material("tinted_glass", Kind.GLASS, null));
		for (String c : COLOURS) out.add(new Material(c + "_stained_glass", Kind.GLASS, c));
		return List.copyOf(out);
	}

	/** The vanilla block. */
	public Block base() {
		return Registries.BLOCK.get(Identifier.ofVanilla(id));
	}

	public boolean glass() {
		return kind == Kind.GLASS;
	}

	/** Coloured glass is drawn see-through; clear and tinted glass have hard edges. */
	public boolean translucent() {
		return kind == Kind.GLASS && !id.equals("glass");
	}

	/** For concrete powder: the concrete of the same colour, which its shapes turn into when wet. */
	public Material hardened() {
		if (kind != Kind.POWDER) return null;
		for (Material m : ALL) if (m.kind == Kind.CONCRETE && m.colour.equals(colour)) return m;
		return null;
	}

	/** The block's id for one of this material's shapes, for example red_wool_vertical_slab. */
	public String idOf(Shape shape) {
		return id + "_" + shape.suffix;
	}
}
