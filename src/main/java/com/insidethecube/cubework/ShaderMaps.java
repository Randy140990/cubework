package com.insidethecube.cubework;

import net.minecraft.resources.Identifier;

/**
 * Shader packs give blocks shine and depth from normal and specular maps named after each texture
 * (oak_door_top_n.png, oak_door_top_s.png). Cubework's coloured and metal doors are painted while the game
 * loads, so no pack has maps for them: when one is asked for, this names the map of the door it was painted
 * from (a red oak door uses the oak door's; the metal doors use the copper door's), so the pattern keeps
 * its shine and depth.
 */
public final class ShaderMaps {
	private ShaderMaps() {}

	/** Makes a patterned map (PNG bytes, or null) from the vanilla map; set by the client. */
	public interface Painter {
		byte[] make(java.io.InputStream in, String pattern, boolean normal);
	}

	public static Painter painter;

	/** A patterned block's map that has to be made: the vanilla map, the pattern, and whether it's a normal map. */
	public record PatternMap(Identifier source, String pattern, boolean normal) {}

	private static final String PATTERNS = "textures/block/pattern/";

	/**
	 * A patterned block's map (cubework:textures/block/pattern/{pattern}/{texture}_n.png). Patterns that keep
	 * every pixel in place use the block's own map; herringbone's is made by laying the block's map out the
	 * same way, so the shine and depth follow the bricks.
	 */
	public static PatternMap pattern(Identifier id) {
		if (!id.getNamespace().equals(Cubework.ID) || !id.getPath().startsWith(PATTERNS) || !id.getPath().endsWith(".png")) return null;
		String rest = id.getPath().substring(PATTERNS.length(), id.getPath().length() - 4);
		int slash = rest.indexOf('/');
		if (slash < 0 || !(rest.endsWith("_n") || rest.endsWith("_s"))) return null;
		String pattern = rest.substring(0, slash), texture = rest.substring(slash + 1);
		return new PatternMap(Identifier.withDefaultNamespace("textures/block/" + texture + ".png"), pattern, texture.endsWith("_n"));
	}

	/** Whether a pattern moves pixels (herringbone), so its maps are made rather than borrowed. */
	public static boolean moves(String pattern) {
		return pattern.equals("herringbone");
	}

	/** The vanilla map to use instead, or null for anything that isn't a Cubework door's (or still pattern's) map. */
	public static Identifier source(Identifier id) {
		if (!id.getNamespace().equals(Cubework.ID)) return null;
		PatternMap p = pattern(id);
		if (p != null) return moves(p.pattern()) ? null : p.source();
		String path = id.getPath();
		if (!path.startsWith("textures/") || !path.contains("_door")) return null;
		String ext = path.endsWith(".png") ? ".png" : path.endsWith(".png.mcmeta") ? ".png.mcmeta" : null;
		if (ext == null) return null;
		String base = path.substring(0, path.length() - ext.length());
		if (!base.endsWith("_n") && !base.endsWith("_s")) return null;
		String map = base.substring(base.length() - 2);
		base = base.substring(0, base.length() - 2);
		int slash = base.lastIndexOf('/');
		String door = door(base.substring(slash + 1));
		return door == null ? null : Identifier.withDefaultNamespace(base.substring(0, slash + 1) + door + map + ext);
	}

	/** red_oak_door_top to oak_door_top, red_iron_door to iron_door, gold_door_bottom to copper_door_bottom. */
	private static String door(String name) {
		int at = name.indexOf("_door");
		if (at < 0) return null;
		String head = name.substring(0, at), tail = name.substring(at);
		for (String c : Material.COLOURS) if (head.startsWith(c + "_")) return head.substring(c.length() + 1) + tail;
		for (String metal : Extras.METAL_DOORS) if (head.equals(metal)) return "copper" + tail;
		return null;
	}
}
