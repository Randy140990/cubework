package com.lumenforge.lens.capture;

/** Added to Minecraft's Window by our mixin: the window's real picture size, whatever the game's drawing at. */
public interface LensWindow {
	int lightlens$realWidth();

	int lightlens$realHeight();
}
