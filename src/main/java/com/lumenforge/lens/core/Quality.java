package com.lumenforge.lens.core;

/** How much picture detail the video keeps, against how big the file gets. */
public enum Quality {
	GOOD("Good", "Smaller files, fine for uploads", 0.6, 24),
	HIGH("High", "Sharp, the one to use", 1.0, 20),
	MAX("Max", "Biggest files, for editing later", 1.6, 17);

	public final String label, hint;
	/** Bitrate against High. */
	public final double factor;
	/** The constant-quality level for encoders that use one (lower is better). */
	public final int cq;

	Quality(String label, String hint, double factor, int cq) {
		this.label = label;
		this.hint = hint;
		this.factor = factor;
		this.cq = cq;
	}
}
