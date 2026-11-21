package com.lumenforge.lens.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * FREX "Flawless Frames": renderers such as Sodium call this entrypoint and hand us a switch.
 * While it's on, every frame waits until all visible chunks are built, so photos have no holes.
 */
public final class Flawless implements Consumer<Function<String, Consumer<Boolean>>> {
	private static final List<Consumer<Boolean>> SWITCHES = new ArrayList<>();

	@Override
	public void accept(Function<String, Consumer<Boolean>> provider) {
		synchronized (SWITCHES) {
			SWITCHES.add(provider.apply("lightlens"));
		}
	}

	public static void set(boolean on) {
		synchronized (SWITCHES) {
			for (Consumer<Boolean> s : SWITCHES) {
				try {
					s.accept(on);
				} catch (Throwable ignored) {
				}
			}
		}
	}
}
