package com.insidethecube.retromon.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.insidethecube.retromon.Retromon;
import com.insidethecube.retromon.engine.Species;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * OPTION's POKéMON LOOK: Cobblemon's models, or Retromon's own creatures (assets/retromon/mons) where there's one.
 * Retromon's own go by their own names on screen too; underneath they're the same species (stats, moves, levelling).
 */
public final class Look {
	/** Species → our creature's name (from mons/names.json). */
	private static Map<String, String> names;
	/** The names to swap on screen: the species' name (as written, and as a word in a sentence) → ours. */
	private static List<Pattern> from;
	private static List<String> to;

	private Look() {}

	/** Whether Retromon's own creatures are showing. */
	public static boolean ours() {
		return Settings.look() == 1;
	}

	private static Map<String, String> names() {
		if (names != null) return names;
		names = new HashMap<>();
		from = new ArrayList<>();
		to = new ArrayList<>();
		try {
			var r = Minecraft.getInstance().getResourceManager().getResource(Retromon.id("mons/names.json"));
			if (r.isPresent()) {
				try (Reader in = r.get().openAsReader()) {
					JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
					for (Map.Entry<String, JsonElement> e : o.entrySet()) names.put(e.getKey(), e.getValue().getAsString());
				}
			}
		} catch (Exception e) {
			Retromon.LOG.warn("Couldn't read Retromon's creature names: {}", e.toString());
		}
		for (Map.Entry<String, String> e : names.entrySet()) {
			Species sp = Species.get(e.getKey());
			if (sp == null) continue;
			String theirs = sp.name(), ours = e.getValue();
			from.add(Pattern.compile("\\b" + Pattern.quote(theirs) + "\\b"));
			to.add(ours);
			String title = theirs.charAt(0) + theirs.substring(1).toLowerCase();
			String oursTitle = ours.charAt(0) + ours.substring(1).toLowerCase();
			from.add(Pattern.compile("\\b" + Pattern.quote(title) + "\\b"));
			to.add(oursTitle);
		}
		return names;
	}

	/** Whether there's one of our own for this species. */
	public static boolean has(String species) {
		return species != null && names().containsKey(species);
	}

	/** Text as it's shown: with our creatures' names in place of the species they stand in for, when they're showing. */
	public static String text(String s) {
		if (s == null || s.isEmpty() || !ours()) return s;
		names();
		for (int i = 0; i < from.size(); i++) {
			Matcher m = from.get(i).matcher(s);
			if (m.find()) s = m.replaceAll(Matcher.quoteReplacement(to.get(i)));
		}
		return s;
	}

	/** A species' picture for the party, the Pokédex and the summaries. */
	public static Identifier portrait(String species) {
		if (ours() && has(species)) return Retromon.id("textures/gui/mon_r/" + species + ".png");
		return Retromon.id("textures/gui/mon/" + species + ".png");
	}

	/** Forgets the names (the resources were reloaded). */
	public static void reload() {
		names = null;
	}
}
