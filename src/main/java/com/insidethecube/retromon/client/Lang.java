package com.insidethecube.retromon.client;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

/**
 * The game's own words in Minecraft's language (Options, Language), as the UI Viewer's picker shows them: every
 * button and key word, the menus, OPTIONS, the title and saves, the battle's commands and the professor's opening.
 * The table is the viewer's (tools/uiviewer/lang.js, written out to assets/retromon/ui_words.json); anything not in it
 * stays English. {N} is your name and {R} your rival's.
 */
public final class Lang {
	private Lang() {}

	private static Map<String, Map<String, String>> table;

	private static Map<String, Map<String, String>> table() {
		if (table != null) return table;
		table = new HashMap<>();
		try (var in = Lang.class.getResourceAsStream("/assets/retromon/ui_words.json")) {
			if (in != null) {
				JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
				for (var e : o.entrySet()) {
					Map<String, String> m = new HashMap<>();
					for (var f : e.getValue().getAsJsonObject().entrySet()) if (!f.getValue().isJsonNull()) m.put(f.getKey(), f.getValue().getAsString());
					table.put(e.getKey(), m);
				}
			}
		} catch (Exception ignored) {}
		return table;
	}

	/** Which of the viewer's languages Minecraft is set to: en, es, fr, de, it, ja, ko, zhs or zht. */
	public static String code() {
		String c;
		try {
			c = Minecraft.getInstance().getLanguageManager().getSelected();
		} catch (Exception e) {
			return "en";
		}
		if (c == null) return "en";
		c = c.toLowerCase(java.util.Locale.ROOT);
		if (c.startsWith("es")) return "es";
		if (c.startsWith("fr")) return "fr";
		if (c.startsWith("de")) return "de";
		if (c.startsWith("it")) return "it";
		if (c.startsWith("ja")) return "ja";
		if (c.startsWith("ko")) return "ko";
		if (c.equals("zh_cn")) return "zhs";
		if (c.startsWith("zh")) return "zht";
		return "en";
	}

	/** The words in Minecraft's language, with {N} and {R} filled in. */
	public static String t(String en) {
		if (en == null) return "";
		String r = en, c = code();
		if (!c.equals("en")) {
			Map<String, String> m = table().get(en);
			if (m != null && m.get(c) != null) r = m.get(c);
		}
		if (r.contains("{N}") || r.contains("{R}")) {
			var d = ClientState.data;
			r = r.replace("{N}", d == null || d.name.isEmpty() ? "RED" : d.name).replace("{R}", d == null || d.rival.isEmpty() ? "BLUE" : d.rival);
		}
		return r;
	}

	/** As t, with {0}, {1}... filled in. */
	public static String t(String en, Object... args) {
		String r = t(en);
		for (int i = 0; i < args.length; i++) r = r.replace("{" + i + "}", String.valueOf(args[i]));
		return r;
	}
}
