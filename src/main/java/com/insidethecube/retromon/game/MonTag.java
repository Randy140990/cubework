package com.insidethecube.retromon.game;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Mon;
import com.insidethecube.retromon.engine.Species;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** A Pokémon to and from a tag (for saving, and to show on your screen). */
public final class MonTag {
	private MonTag() {}

	public static CompoundTag save(Mon m) {
		CompoundTag t = new CompoundTag();
		t.putString("sp", m.species.id());
		if (m.nickname != null) t.putString("nick", m.nickname);
		t.putInt("lv", m.level);
		t.putInt("exp", m.exp);
		t.putInt("hp", m.hp);
		t.putIntArray("dv", new int[] {m.dvAtk, m.dvDef, m.dvSpe, m.dvSpc});
		t.putIntArray("se", m.statExp.clone());
		t.putString("st", m.status.name());
		t.putInt("slp", m.sleepTurns);
		t.putString("mv", String.join(",", m.moves));
		int[] pp = new int[m.pp.size()];
		for (int i = 0; i < pp.length; i++) pp[i] = m.pp.get(i);
		t.putIntArray("pp", pp);
		// For the screen (worked out here so the screen needn't).
		t.putInt("max", m.maxHp());
		t.putInt("fr", m.friendship);
		t.putInt("steps", m.steps);
		return t;
	}

	public static Mon load(CompoundTag t) {
		Species s = Species.ALL.get(t.getStringOr("sp", "pidgey"));
		if (s == null) s = Species.get("pidgey");
		Mon m = new Mon(s);
		String nick = t.getStringOr("nick", "");
		m.nickname = nick.isEmpty() ? null : nick;
		m.level = Math.max(1, t.getIntOr("lv", 5));
		m.exp = t.getIntOr("exp", s.growth().expAt(m.level));
		int[] dv = t.getIntArray("dv").orElse(new int[] {8, 8, 8, 8});
		if (dv.length == 4) {
			m.dvAtk = dv[0];
			m.dvDef = dv[1];
			m.dvSpe = dv[2];
			m.dvSpc = dv[3];
		}
		int[] se = t.getIntArray("se").orElse(new int[5]);
		if (se.length == 5) m.statExp = se;
		try {
			m.status = Mon.Status.valueOf(t.getStringOr("st", "NONE"));
		} catch (IllegalArgumentException e) {
			m.status = Mon.Status.NONE;
		}
		m.sleepTurns = t.getIntOr("slp", 0);
		String mv = t.getStringOr("mv", "");
		int[] pp = t.getIntArray("pp").orElse(new int[0]);
		int i = 0;
		for (String id : mv.split(",")) {
			if (id.isEmpty() || !com.insidethecube.retromon.engine.Move.ALL.containsKey(id) || m.moves.contains(id)) continue;
			m.moves.add(id);
			m.pp.add(i < pp.length ? pp[i] : com.insidethecube.retromon.engine.Move.get(id).pp());
			i++;
		}
		if (m.moves.isEmpty()) {
			for (String id : s.movesAt(m.level)) {
				m.moves.add(id);
				m.pp.add(com.insidethecube.retromon.engine.Move.get(id).pp());
			}
		}
		m.hp = Math.max(0, Math.min(m.maxHp(), t.getIntOr("hp", m.maxHp())));
		m.friendship = t.getIntOr("fr", 70);
		m.steps = t.getIntOr("steps", 0);
		return m;
	}

	public static ListTag saveAll(List<Mon> mons) {
		ListTag l = new ListTag();
		for (Mon m : mons) l.add(save(m));
		return l;
	}

	public static List<Mon> loadAll(ListTag l) {
		List<Mon> out = new ArrayList<>();
		for (Tag t : l) if (t instanceof CompoundTag c) out.add(load(c));
		return out;
	}
}
