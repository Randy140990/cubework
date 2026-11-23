package com.insidethecube.retromon.client;

import java.lang.reflect.Field;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * (0.59, 18:14: "I always get a text come up saying Axiom tip ... which if it's disabled I shouldn't see") In a Retromon
 * save, where Axiom is turned off (its editor and its keys), its tips aren't shown either: its lines in the chat (see
 * mixin/ChatQuietMixin) and its toasts, taken out of the toast queue as they arrive.
 */
public final class AxiomQuiet {
	private AxiomQuiet() {
	}

	/** Whether Axiom is turned off just now: in a Retromon save, playing (not editing the map). */
	public static boolean on() {
		return Adventure.inSave || Adventure.active();
	}

	public static boolean axiomText(Component c) {
		return c != null && c.getString().toLowerCase(java.util.Locale.ROOT).contains("axiom");
	}

	public static void tick(Minecraft mc) {
		if (!on()) return;
		try {
			Object tm = mc.getToastManager();
			for (Field f : tm.getClass().getDeclaredFields()) {
				if (!Collection.class.isAssignableFrom(f.getType())) continue;
				f.setAccessible(true);
				Object v = f.get(tm);
				if (v instanceof Collection<?> c && !c.isEmpty()) c.removeIf(AxiomQuiet::axiomToast);
			}
		} catch (Throwable ignored) {
			// (Minecraft's toasts as another version has them: left alone.)
		}
	}

	/** A toast of Axiom's own, or one whose words name it (looking one level in: a toast held in its slot). */
	private static boolean axiomToast(Object o) {
		return axiomToast(o, 0);
	}

	private static boolean axiomToast(Object o, int depth) {
		if (o == null) return false;
		if (o.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("axiom")) return true;
		if (depth > 1) return false;
		for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass())
			for (Field f : k.getDeclaredFields()) {
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
				try {
					f.setAccessible(true);
					Object v = f.get(o);
					if (v instanceof Component c && axiomText(c)) return true;
					if (v instanceof net.minecraft.client.gui.components.toasts.Toast t && axiomToast(t, depth + 1)) return true;
				} catch (Throwable ignored) {
					// Not one we can read.
				}
			}
		return false;
	}
}
