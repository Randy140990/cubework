package com.insidethecube.kantojohto.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The trainers and gym leaders placed so far: who they are, their team, what they say and what they pay. */
public final class Trainers {
	private Trainers() {}

	public record Member(String species, int level) {}

	/**
	 * title: as the battle shows it. prizeBase: money per level of their last Pokémon. badge: what a gym
	 * leader gives (or null). sight: how many blocks ahead they watch (0: they only battle if you talk to them).
	 */
	public record Trainer(String key, String title, int skin, int prizeBase, List<Member> team, List<String> before, List<String> after, String badge,
			int sight) {}

	public static final Map<String, Trainer> ALL = new LinkedHashMap<>();

	private static void add(Trainer t) {
		ALL.put(t.key(), t);
	}

	static {
		add(new Trainer("youngster_joey", "YOUNGSTER JOEY", 2, 16, List.of(new Member("rattata", 4)),
				List.of("Hey! You have POKéMON!", "Come on! Let's battle 'em!"), List.of("I lost... My RATTATA's in the top percentage, though!"), null, 6));
		add(new Trainer("leader_brock", "LEADER BROCK", 3, 99, List.of(new Member("geodude", 12), new Member("onix", 14)),
				List.of("I'm BROCK!", "I'm PEWTER's GYM LEADER!", "I believe in rock hard defense and determination!",
						"That's why my POKéMON are all the rock-type!", "Do you still want to challenge me?", "Fine then! Show me your best!"),
				List.of("There are all kinds of trainers in the world!", "You appear to be very gifted as a POKéMON trainer!", "Go to the GYM in CERULEAN and test your abilities!"),
				"BOULDERBADGE", 0));
	}

	public static Trainer get(String key) {
		return ALL.get(key);
	}
}
