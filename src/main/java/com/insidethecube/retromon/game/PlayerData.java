package com.insidethecube.retromon.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.insidethecube.retromon.engine.Mon;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

/** Your adventure so far: your Pokémon, money, bag, badges, the trainers you've beaten and your Pokédex. */
public class PlayerData {
	public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::load, PlayerData::save);

	public final List<Mon> party = new ArrayList<>();
	public final List<Mon> box = new ArrayList<>();
	public int money = 3000;
	/** The bag: item id to how many, in the order they went in. */
	public final Map<String, Integer> items = new LinkedHashMap<>();
	public final Set<String> badges = new LinkedHashSet<>(), beaten = new LinkedHashSet<>(), seen = new LinkedHashSet<>(), caught = new LinkedHashSet<>();
	/** One-off Pokémon you've caught, beaten or been given (they're gone from the world for you). */
	public final Set<String> statics = new LinkedHashSet<>();
	/** Whether you've had your first Pokémon yet. */
	public boolean started;
	/** Where you last healed (you go back there if you black out, or use an ESCAPE ROPE). */
	public boolean hasHeal;
	public double healX, healY, healZ;
	/** Steps of REPEL left. */
	public int repel;
	/** The place you're in (or were last in). */
	public String place = "";
	/** Text speed: 0 slow, 1 mid, 2 fast. */
	public int textSpeed = 2;
	/** Which of your party walks with you (its place in the party). */
	public int follow;
	/** From the new-game intro: your name, your rival's and whether you're a boy or a girl. */
	public String name = "", rival = "", gender = "boy";
	/** Your clothes (from CLOTHES in the menu), as the wardrobe writes them; empty for the hero's own. */
	public String outfit = "";
	/** The places you've been (for the Town Map, and where you can fly). */
	public final Set<String> visited = new LinkedHashSet<>();
	/** How far the story's come: "parcel" (carrying OAK's PARCEL), "pokedex" (Oak gave you the POKéDEX). */
	public final Set<String> story = new LinkedHashSet<>();

	public boolean hasDex() {
		return story.contains("pokedex");
	}

	public boolean anyHealthy() {
		for (Mon m : party) if (!m.fainted()) return true;
		return false;
	}

	public void healAll() {
		for (Mon m : party) m.heal();
	}

	public void add(Mon m) {
		if (party.size() < 6) party.add(m);
		else box.add(m);
		caught.add(m.species.id());
		seen.add(m.species.id());
	}

	public int count(String item) {
		return items.getOrDefault(item, 0);
	}

	public void give(String item, int n) {
		items.merge(item, n, Integer::sum);
		if (items.get(item) <= 0) items.remove(item);
	}

	/** Takes one; false if there wasn't one. */
	public boolean take(String item) {
		int n = count(item);
		if (n <= 0) return false;
		if (n == 1) items.remove(item);
		else items.put(item, n - 1);
		return true;
	}

	private static String join(Set<String> s) {
		return String.join(",", s);
	}

	private static void split(String v, Set<String> into) {
		into.clear();
		if (!v.isEmpty()) into.addAll(Arrays.asList(v.split(",")));
	}

	public CompoundTag save() {
		CompoundTag t = new CompoundTag();
		t.put("party", MonTag.saveAll(party));
		t.put("box", MonTag.saveAll(box));
		t.putInt("money", money);
		CompoundTag bag = new CompoundTag();
		for (Map.Entry<String, Integer> e : items.entrySet()) bag.putInt(e.getKey(), e.getValue());
		t.put("bag", bag);
		StringBuilder order = new StringBuilder();
		for (String k : items.keySet()) order.append(order.isEmpty() ? "" : ",").append(k);
		t.putString("bagOrder", order.toString());
		t.putString("badges", join(badges));
		t.putString("beaten", join(beaten));
		t.putString("seen", join(seen));
		t.putString("caught", join(caught));
		t.putString("statics", join(statics));
		t.putBoolean("started", started);
		t.putBoolean("hasHeal", hasHeal);
		t.putDouble("hx", healX);
		t.putDouble("hy", healY);
		t.putDouble("hz", healZ);
		t.putInt("repel", repel);
		t.putString("place", place);
		t.putInt("textSpeed", textSpeed);
		t.putInt("follow", follow);
		t.putString("name", name);
		t.putString("rival", rival);
		t.putString("gender", gender);
		t.putString("outfit", outfit);
		t.putString("visited", join(visited));
		t.putString("story", join(story));
		return t;
	}

	public static PlayerData load(CompoundTag t) {
		PlayerData d = new PlayerData();
		d.party.addAll(MonTag.loadAll(t.getListOrEmpty("party")));
		d.box.addAll(MonTag.loadAll(t.getListOrEmpty("box")));
		d.money = t.getIntOr("money", 3000);
		CompoundTag bag = t.getCompoundOrEmpty("bag");
		for (String k : t.getStringOr("bagOrder", "").split(",")) {
			if (k.isEmpty()) continue;
			int n = bag.getIntOr(k, 0);
			if (n > 0) d.items.put(k, n);
		}
		// Saves from before the bag.
		int balls = t.getIntOr("balls", 0), potions = t.getIntOr("potions", 0);
		if (balls > 0) d.give("poke_ball", balls);
		if (potions > 0) d.give("potion", potions);
		split(t.getStringOr("badges", ""), d.badges);
		split(t.getStringOr("beaten", ""), d.beaten);
		split(t.getStringOr("seen", ""), d.seen);
		split(t.getStringOr("caught", ""), d.caught);
		split(t.getStringOr("statics", ""), d.statics);
		d.started = t.getBooleanOr("started", false);
		d.hasHeal = t.getBooleanOr("hasHeal", false);
		d.healX = t.getDoubleOr("hx", 0);
		d.healY = t.getDoubleOr("hy", 0);
		d.healZ = t.getDoubleOr("hz", 0);
		d.repel = t.getIntOr("repel", 0);
		d.place = t.getStringOr("place", "");
		d.textSpeed = t.getIntOr("textSpeed", 2);
		d.follow = t.getIntOr("follow", 0);
		d.name = t.getStringOr("name", "");
		d.rival = t.getStringOr("rival", "");
		d.gender = t.getStringOr("gender", "boy");
		d.outfit = t.getStringOr("outfit", "");
		if (t.contains("story")) split(t.getStringOr("story", ""), d.story);
		// A save from before the errand (0.8 and older): you already had the POKéDEX.
		else if (d.started) d.story.add("pokedex");
		split(t.getStringOr("visited", ""), d.visited);
		return d;
	}
}
