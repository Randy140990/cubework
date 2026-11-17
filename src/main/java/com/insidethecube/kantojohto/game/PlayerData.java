package com.insidethecube.kantojohto.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.insidethecube.kantojohto.engine.Mon;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

/** Your adventure so far: your Pokémon, money, bag, badges, the trainers you've beaten and your Pokédex. */
public class PlayerData {
	public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::load, PlayerData::save);

	public final List<Mon> party = new ArrayList<>();
	public final List<Mon> box = new ArrayList<>();
	public int money = 3000;
	public int balls, potions;
	public final Set<String> badges = new LinkedHashSet<>(), beaten = new LinkedHashSet<>(), seen = new LinkedHashSet<>(), caught = new LinkedHashSet<>();
	/** Whether you've had your first Pokémon yet. */
	public boolean started;
	/** Where you last healed (you go back there if you black out). */
	public boolean hasHeal;
	public double healX, healY, healZ;

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
		t.putInt("balls", balls);
		t.putInt("potions", potions);
		t.putString("badges", join(badges));
		t.putString("beaten", join(beaten));
		t.putString("seen", join(seen));
		t.putString("caught", join(caught));
		t.putBoolean("started", started);
		t.putBoolean("hasHeal", hasHeal);
		t.putDouble("hx", healX);
		t.putDouble("hy", healY);
		t.putDouble("hz", healZ);
		return t;
	}

	public static PlayerData load(CompoundTag t) {
		PlayerData d = new PlayerData();
		d.party.addAll(MonTag.loadAll(t.getListOrEmpty("party")));
		d.box.addAll(MonTag.loadAll(t.getListOrEmpty("box")));
		d.money = t.getIntOr("money", 3000);
		d.balls = t.getIntOr("balls", 0);
		d.potions = t.getIntOr("potions", 0);
		split(t.getStringOr("badges", ""), d.badges);
		split(t.getStringOr("beaten", ""), d.beaten);
		split(t.getStringOr("seen", ""), d.seen);
		split(t.getStringOr("caught", ""), d.caught);
		d.started = t.getBooleanOr("started", false);
		d.hasHeal = t.getBooleanOr("hasHeal", false);
		d.healX = t.getDoubleOr("hx", 0);
		d.healY = t.getDoubleOr("hy", 0);
		d.healZ = t.getDoubleOr("hz", 0);
		return d;
	}
}
