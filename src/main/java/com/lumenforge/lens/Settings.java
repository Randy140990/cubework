package com.lumenforge.lens;

import com.lumenforge.lens.core.Quality;
import com.lumenforge.lens.core.Sizes;

/** What you've picked on the Lightlens screen. Saved to config/lightlens.json. */
public final class Settings {
	/** Where finished videos and photos go. Blank until you choose. */
	public String folder = "";
	public boolean folderChosen;

	public Sizes.Size videoSize = Sizes.Size.K1;
	public int fps = 60;
	public Quality quality = Quality.HIGH;
	/** H.265: about 60% of the size, if your graphics card can encode it. */
	public boolean hevc;
	public Sizes.Shape shape = Sizes.Shape.WIDE;
	/** Draws the game at the video's size while recording (sharper on a smaller screen, slower). */
	public boolean sharp;
	public boolean gameSound = true;
	public boolean mic;
	public float micVolume = 1.0F;

	public Sizes.Size photoSize = Sizes.Size.K4;
	public boolean jpg;
	public boolean photosInSubfolder = true;

	/** The red dot and the clock in the corner while recording (never in the video). */
	public boolean showDot = true;
	/** A path to your own ffmpeg, if you'd rather. */
	public String ffmpegPath = "";

	/** Puts anything missing or out of range right (an older or hand-edited file). */
	void fix() {
		if (videoSize == null) videoSize = Sizes.Size.K1;
		if (fps != 30 && fps != 60) fps = 60;
		if (quality == null) quality = Quality.HIGH;
		if (shape == null) shape = Sizes.Shape.WIDE;
		if (photoSize == null) photoSize = Sizes.Size.K4;
		if (folder == null) folder = "";
		if (ffmpegPath == null) ffmpegPath = "";
		if (!(micVolume >= 0.1F && micVolume <= 3F)) micVolume = 1F;
	}
}
