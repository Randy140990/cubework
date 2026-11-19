package com.insidethecube.retromon.client;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import com.insidethecube.retromon.Retromon;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Your clothes: skin tone, hair (style and colour), hat, top, bottoms and shoes, each in a colour. The skin
 * is made from Retromon's wardrobe layers (textures/outfit), each tinted and stacked, and kept as a texture
 * of its own (retromon:dynamic/hero) that your player wears in a Retromon save.
 */
public final class Outfit {
	/** Skin tones: the first four as ever, then four more (the maker's new ones). */
	public static final String[] SKINS = {"FAIR", "LIGHT", "TAN", "DEEP", "PALE", "MEDIUM", "BROWN", "DARK"};
	public static final int[] SKIN_RGB = {0xF2CBAE, 0xE6B48E, 0xC08A60, 0x86563A, 0xF8DCC8, 0xD69E72, 0xA2683F, 0x5C3824};
	/** Eye colours: the face layer's irises (45, 50, 80) take this colour. */
	public static final String[] EYES = {"NAVY", "BROWN", "HAZEL", "GREEN", "BLUE", "GREY", "AMBER", "VIOLET"};
	public static final int[] EYE_RGB = {0x2D3250, 0x5A3A22, 0x7A6A34, 0x3A7A44, 0x3A6FD0, 0x6E7A88, 0xB87A22, 0x6A4AA8};
	public static final String[] HAIRS = {"short", "spiky", "long", "ponytail", "pigtails", "bun"};
	public static final String[] HATS = {"none", "cap", "beanie", "sunhat"};
	public static final String[] TOPS = {"jacket", "tee", "hoodie", "vest"};
	public static final String[] BOTTOMS = {"jeans", "shorts", "skirt"};
	/** The cap's patterns (textures/gear/cap_*.png): PLAIN takes your hat colour. */
	public static final String[] CAPS = {"pokeball", "pikachu", "bulbasaur", "charmander", "squirtle", "eevee", "gengar", "snorlax", "greatball", "ultraball",
			"plain"};
	public static final String[] CAP_NAMES = {"POKé BALL", "PIKACHU", "BULBASAUR", "CHARMANDER", "SQUIRTLE", "EEVEE", "GENGAR", "SNORLAX", "GREAT BALL",
			"ULTRA BALL", "PLAIN"};
	public static final String[] CAP_WAYS = {"FORWARDS", "BACKWARDS"};
	public static final String[] BAGS = {"backpack", "satchel", "none"};
	public static final String[] COLOUR_NAMES = {"RED", "BLUE", "GREEN", "YELLOW", "BLACK", "WHITE", "PINK", "ORANGE", "PURPLE", "BROWN", "TEAL", "GREY", "NAVY",
			"CREAM"};
	public static final int[] COLOURS = {0xD8343C, 0x3A6FD0, 0x3FA34D, 0xF2C12E, 0x2A2A30, 0xF2F2F2, 0xF08FB6, 0xF07D2A, 0x8A52C8, 0x7A5030, 0x2AA5A0, 0x8C8F99,
			0x283A6E, 0xF0E2BE};

	/** What's being picked: the parts in order, and their values (style or colour index). */
	public static final String[] PARTS = {"skin", "eyes", "hair", "hairc", "hat", "capp", "capb", "hatc", "top", "topc", "bot", "botc", "shoec", "bag", "bagc"};
	public final Map<String, Integer> v = new LinkedHashMap<>();

	public static Outfit defaults(boolean girl) {
		Outfit o = new Outfit();
		o.v.put("skin", 0);
		o.v.put("eyes", 0);
		o.v.put("hair", girl ? 4 : 0);
		o.v.put("hairc", girl ? 9 : 4);
		o.v.put("hat", girl ? 3 : 1);
		o.v.put("hatc", girl ? 5 : 0);
		o.v.put("top", girl ? 1 : 0);
		o.v.put("topc", girl ? 10 : 0);
		o.v.put("bot", girl ? 2 : 0);
		o.v.put("botc", girl ? 0 : 12);
		o.v.put("shoec", girl ? 6 : 0);
		o.v.put("capp", girl ? 1 : 0);
		o.v.put("capb", 0);
		o.v.put("bag", girl ? 1 : 0);
		o.v.put("bagc", 3);
		return o;
	}

	public static Outfit parse(String s, boolean girl) {
		Outfit o = defaults(girl);
		if (s == null) return o;
		for (String kv : s.split(";")) {
			String[] p = kv.split("=");
			if (p.length != 2) continue;
			try {
				o.v.put(p[0], Integer.parseInt(p[1].trim()));
			} catch (NumberFormatException ignored) {
				// An old or broken setting: the default stays.
			}
		}
		return o;
	}

	public String write() {
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, Integer> e : v.entrySet()) b.append(b.isEmpty() ? "" : ";").append(e.getKey()).append('=').append(e.getValue());
		return b.toString();
	}

	public int get(String part) {
		return v.getOrDefault(part, 0);
	}

	/** How many choices a part has (styles, or the colours). */
	public static int options(String part) {
		return switch (part) {
			case "skin" -> SKINS.length;
			case "eyes" -> EYES.length;
			case "hair" -> HAIRS.length;
			case "hat" -> HATS.length;
			case "top" -> TOPS.length;
			case "bot" -> BOTTOMS.length;
			case "capp" -> CAPS.length;
			case "capb" -> CAP_WAYS.length;
			case "bag" -> BAGS.length;
			default -> COLOURS.length;
		};
	}

	public static String label(String part, int i) {
		return switch (part) {
			case "skin" -> SKINS[i];
			case "eyes" -> EYES[i];
			case "hair" -> HAIRS[i].toUpperCase();
			case "hat" -> HATS[i].toUpperCase();
			case "top" -> TOPS[i].toUpperCase();
			case "bot" -> BOTTOMS[i].toUpperCase();
			case "capp" -> CAP_NAMES[i];
			case "capb" -> CAP_WAYS[i];
			case "bag" -> BAGS[i].toUpperCase();
			default -> COLOUR_NAMES[i];
		};
	}

	// ---------- the skin ----------

	public static final Identifier HERO = Retromon.id("dynamic/hero");
	private static String built;

	/** The skin for this outfit, made (or remade) if it's changed. Returns false if it couldn't be made. */
	private static boolean builtGirl;

	/** The outfit the hero skin was made from (for the cap and bag on figures), and whether it's the girl. */
	public static String heroOutfit() {
		return built == null ? "" : built;
	}

	public static boolean heroGirl() {
		return builtGirl;
	}

	public static boolean ensure(String outfit, boolean girl) {
		if (outfit.equals(built) && girl == builtGirl) return true;
		builtGirl = girl;
		try {
			NativeImage img = parse(outfit, girl).compose();
			Minecraft.getInstance().getTextureManager().register(HERO, new DynamicTexture(() -> "Retromon hero", img));
			built = outfit;
			return true;
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't make your outfit: {}", e.toString());
			built = outfit;
			return false;
		}
	}

	private NativeImage compose() throws Exception {
		NativeImage out = new NativeImage(64, 64, true);
		int skin = SKIN_RGB[Math.floorMod(get("skin"), SKIN_RGB.length)];
		layer(out, "body", skin);
		layer(out, "face", -1);
		// The eyes in their colour, and the brows a shade of the hair (the face layer marks them in two set colours).
		int eye = EYE_RGB[Math.floorMod(get("eyes"), EYE_RGB.length)], hairc = colour("hairc");
		for (int y = 0; y < out.getHeight(); y++)
			for (int x = 0; x < out.getWidth(); x++) {
				int c = out.getPixel(x, y);
				int r = (c >> 16) & 255, gg = (c >> 8) & 255, b = c & 255, a = c & 0xFF000000;
				if (r == 45 && gg == 50 && b == 80) out.setPixel(x, y, a | eye);
				else if (r == 70 && gg == 50 && b == 40)
					out.setPixel(x, y, a | (int) (((hairc >> 16) & 255) * 0.62F) << 16 | (int) (((hairc >> 8) & 255) * 0.62F) << 8 | (int) ((hairc & 255) * 0.62F));
			}
		layer(out, "bottom_" + BOTTOMS[Math.floorMod(get("bot"), BOTTOMS.length)], colour("botc"));
		layer(out, "shoes", colour("shoec"));
		String top = TOPS[Math.floorMod(get("top"), TOPS.length)];
		layer(out, "top_" + top, colour("topc"));
		if (top.equals("jacket")) layer(out, "top_jacket_trim", -1);
		layer(out, "hair_" + HAIRS[Math.floorMod(get("hair"), HAIRS.length)], colour("hairc"));
		// The cap is a model of its own (GearLayer); the beanie and the sun hat are on the skin.
		String hat = HATS[Math.floorMod(get("hat"), HATS.length)];
		if (!hat.equals("none") && !hat.equals("cap")) layer(out, "hat_" + hat, colour("hatc"));
		return out;
	}

	int colour(String part) {
		return COLOURS[Math.floorMod(get(part), COLOURS.length)];
	}

	/** One wardrobe layer stacked on (tinted with rgb, or in its own colours for -1). */
	static void layer(NativeImage out, String name, int rgb) throws Exception {
		layer(out, Retromon.id("textures/outfit/" + name + ".png"), rgb);
	}

	/** Any picture stacked on, tinted (the bag's, from textures/gear, too). Both are 64 wide; as tall as out. */
	static void layer(NativeImage out, Identifier id, int rgb) throws Exception {
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(id); NativeImage img = NativeImage.read(in)) {
			int tr = rgb < 0 ? 255 : (rgb >> 16) & 255, tg = rgb < 0 ? 255 : (rgb >> 8) & 255, tb = rgb < 0 ? 255 : rgb & 255;
			for (int y = 0; y < Math.min(out.getHeight(), img.getHeight()); y++) {
				for (int x = 0; x < Math.min(out.getWidth(), img.getWidth()); x++) {
					int c = img.getPixel(x, y);
					int a = (c >>> 24) & 255;
					if (a == 0) continue;
					int r = ((c >> 16) & 255) * tr / 255, g = ((c >> 8) & 255) * tg / 255, b = (c & 255) * tb / 255;
					if (a >= 250) {
						out.setPixel(x, y, 0xFF000000 | r << 16 | g << 8 | b);
						continue;
					}
					int o = out.getPixel(x, y), oa = (o >>> 24) & 255;
					int na = a + oa * (255 - a) / 255;
					int nr = (r * a + ((o >> 16) & 255) * (255 - a)) / 255, ng = (g * a + ((o >> 8) & 255) * (255 - a)) / 255, nb = (b * a + (o & 255) * (255 - a)) / 255;
					out.setPixel(x, y, na << 24 | nr << 16 | ng << 8 | nb);
				}
			}
		}
	}
}
