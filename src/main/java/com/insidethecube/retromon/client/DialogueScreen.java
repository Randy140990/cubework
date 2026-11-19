package com.insidethecube.retromon.client;

import java.util.List;

import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.Trainers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * Someone talking to you: their lines in the text box, and a YES/NO at the end for the nurse and for gifts.
 * When it closes the game hears what you said (heal, open the Mart, start the battle, take the gift).
 */
public class DialogueScreen extends GameScreen {
	private final TextBox box;
	private final String mode, trainer;
	private final CompoundTag extra;
	private final int npc;
	private Choice yesNo;
	private boolean sent;
	/** Jingles on lines ("got OAK's PARCEL!"): line index to jingle, and the last line one played for. */
	private final java.util.Map<Integer, String> cues = new java.util.HashMap<>();
	private int cued = -1;

	/** The lines as they're said (the speakers taken off). */
	private final java.util.List<String> said;
	/** When the line showing came up (ticks), for the cards that pop up with it. */
	private int lineAt;
	/** The nurse's line while your POKéMON are in the machine (-1: none), and when it began (ticks; -1: not yet). */
	private final int healLine;
	private int healAt = -1;
	private static final int HEAL_TICKS = 52;
	private static final String[] BADGES = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};

	/** A line that starts with who's speaking: "OAK: ..." (capitals, before a colon). */
	private static final java.util.regex.Pattern SPEAKER = java.util.regex.Pattern.compile("^([A-Z][A-Z .'é&]{1,20}): (.+)$");

	public DialogueScreen(List<String> lines, String mode, int npc, CompoundTag extra) {
		super("Dialogue");
		// "OAK: Hey! Wait!": the speaker goes on the tab over the box (as the viewer shows it), not in the words.
		String who = null;
		java.util.List<String> said = new java.util.ArrayList<>();
		for (String l : lines) {
			java.util.regex.Matcher m = SPEAKER.matcher(l);
			if (m.matches() && (who == null || who.equals(m.group(1)))) {
				who = m.group(1);
				said.add(m.group(2));
			} else said.add(l);
		}
		this.box = new TextBox(said);
		this.said = said;
		if (who != null) box.speaker = who;
		this.mode = mode;
		this.npc = npc;
		this.extra = extra;
		String t = extra.getStringOr("trainer", "");
		this.trainer = t.isEmpty() ? null : t;
		Trainers.Trainer tr = Trainers.get(trainer);
		if (tr != null && box.speaker == null) box.speaker = tr.title();
		String whoTag = extra.getStringOr("who", "");
		if (box.speaker == null && !whoTag.isEmpty()) box.speaker = whoTag;
		for (net.minecraft.nbt.Tag c : extra.getListOrEmpty("cues")) c.asString().ifPresent(v -> {
			int at = v.indexOf(':');
			if (at > 0) {
				try {
					cues.put(Integer.parseInt(v.substring(0, at)), v.substring(at + 1));
				} catch (NumberFormatException ignored) {
					// Not a cue.
				}
			}
		});
		this.healLine = extra.getIntOr("healing", -1);
		String cry = extra.getStringOr("cry", "");
		if (!cry.isEmpty()) Sounds.cry(cry, 1F);
		// The camera closes in on the two of you, and the black bars come in, as in a cutscene.
		Adventure.focus(npc);
	}

	private boolean asks() {
		return mode.equals("heal") || mode.equals("gift") || mode.equals("trade");
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		int line = box.message();
		if (line >= 0 && line != cued) {
			cued = line;
			lineAt = ticks;
			String j = cues.get(line);
			if (j != null) Music.jingle(j);
		}
		if (asks() && yesNo == null && box.atEnd()) yesNo = new Choice(List.of("YES", "NO"));
		// The healing: once the nurse's line is out, the machine's notes play for a few seconds, then she goes on.
		if (healLine >= 0 && healAt < 0 && line == healLine && box.typedOut()) {
			healAt = ticks;
			Sounds.heal();
		}
		if (healing() && ticks - healAt >= HEAL_TICKS) {
			healAt = -2;
			if (box.press()) done(false);
		}
	}

	private boolean healing() {
		return healAt >= 0;
	}

	@Override
	protected boolean key(KeyEvent e) {
		if (healing()) return true;
		if (yesNo != null) {
			if (yesNo.key(e)) return true;
			if (Ui.isA(e)) {
				Sounds.select();
				done(yesNo.at == 0);
			} else if (Ui.isB(e)) done(false);
			return true;
		}
		if (Ui.isA(e) || Ui.isB(e)) {
			if (box.press()) done(false);
			return true;
		}
		return false;
	}

	@Override
	protected void clickAnywhere() {
		if (!healing() && yesNo == null && box.press()) done(false);
	}

	@Override
	protected void back() {
		if (yesNo != null) done(false);
		else clickAnywhere();
	}

	private void done(boolean yes) {
		if (sent) return;
		sent = true;
		CompoundTag t = extra.copy();
		t.putInt("npc", npc);
		t.putString("mode", mode);
		t.putBoolean("yes", yes);
		if (trainer != null) t.putString("trainer", trainer);
		ClientPlayNetworking.send(new Net.Act("dialogue_done", t));
		// A badge won: its notice on the HUD, where it sits among the eight.
		for (String l : said) {
			int b = badgeIn(l);
			if (b >= 0 && (l.contains("received") || l.contains("got"))) Hud.badge(b);
		}
		Adventure.focus(-1);
		if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
	}

	@Override
	public void onClose() {
		done(false);
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		// (The cinema frame fades in and out round this, from the HUD.) The box rises into place.
		// The UI Viewer's text box (teal glass, the speaker on a cream tab, names in gold), rising into place; a
		// question's answers in its list above the box's right end.
		float in = Math.min(1, (ticks + partial) / 5F);
		float rise = (1 - in * in * (3 - 2 * in)) * 90;
		g.pose().pushMatrix();
		g.pose().translate(0, V.s(rise));
		box.renderV(g, font, 140, 26, 1000, ticks, partial, yesNo == null && !healing());
		g.pose().popMatrix();
		if (healing()) {
			// The machine's notes (the viewer's), popping in and bobbing.
			float ht = (ticks - healAt + partial) / 20F, pop = Math.min(1, ht / 0.4F), bob = (float) Math.sin(ht * Math.PI * 2) * 4;
			int c = Ui.alpha(V.GOLD, pop);
			V.text(g, "♫ ♪", 560 + 4, 210 + 14 + 3 + bob, 46, Ui.alpha(0x59000000, pop), true);
			V.text(g, "♫ ♪", 560, 210 + 14 + bob, 46, c, true);
		}
		// Getting something: the UI Viewer's card held up over the world (a badge spins in instead).
		int line = box.message();
		if (line >= 0 && line < said.size()) {
			String l = said.get(line);
			int b = badgeIn(l);
			float t = (ticks - lineAt + partial) / 20F;
			if (b >= 0 && (l.contains("received") || l.contains("got"))) badgeCard(g, b, t);
			else if (cues.containsKey(line) || l.matches(".*\\b(got|received|obtained|found)\\b.*!.*")) itemCard(g, l, t);
		}
		if (yesNo != null) {
			float w = 300;
			vchoice(g, yesNo, V.W - 140 - w, V.H - 26 - 150 - V.choiceHeight(yesNo.options.size()), w, i -> done(i == 0));
		}
	}

	private static int badgeIn(String line) {
		String u = line.toUpperCase();
		for (int i = 0; i < BADGES.length; i++) if (u.contains(BADGES[i])) return i;
		return -1;
	}

	/** The picture for something you've got: a ball, medicine, a disc for a TM, the map, a key for the rest. */
	private static String itemPicture(String line) {
		String u = line.toUpperCase();
		if (u.contains("BALL")) return "icon_ball";
		if (u.contains("POTION") || u.contains("HEAL") || u.contains("ANTIDOTE") || u.contains("REVIVE")) return "icon_potion";
		if (u.contains(" TM") || u.contains(" HM")) return "icon_disc";
		if (u.contains("MAP")) return "icon_map";
		if (u.contains("POKéDEX") || u.contains("POKEDEX")) return "icon_dex";
		return "icon_key";
	}

	/** The item held up in a card (the viewer's): a soft glow bobbing behind it, a couple of notes. */
	private void itemCard(GuiGraphics g, String line, float t) {
		float pop = Math.min(1, t / 0.4F), k = pop < 1 ? (float) (0.85 + 0.15 * Math.sin(pop * Math.PI / 2) + Math.sin(pop * Math.PI) * 0.08) : 1;
		float w = 300 * k, x = 640 - w / 2, y = 120 + (300 - w) / 2;
		V.panel(g, x, y, w, w);
		float bob = (float) Math.sin(t * Math.PI / 0.8) * 2;
		V.glow(g, 640, 270 + bob, 126 * k, 126 * k, 0x2EFFF4D8);
		V.icon(g, itemPicture(line), 640 - 75 * k, 270 - 75 * k, 150 * k);
		V.text(g, "♫", x + 18, y + 14, 30, V.GOLD, true);
		V.text(g, "♪", x + w - 22 - 16, y + 40, 24, V.EDGE_LIGHT, true);
	}

	/** A badge won (the viewer's): a burst of light, the badge spinning in and shining. */
	private void badgeCard(GuiGraphics g, int b, float t) {
		g.fill(0, 0, width, height, 0x59061A1E);
		V.glow(g, 640, 220, 190, 190, 0x26FFF4D8);
		V.glow(g, 640, 220, 120, 120, 0x40FFF4D8);
		float in = Math.min(1, t / 1.1F), e = 1 - (1 - in) * (1 - in) * (1 - in);
		float spin = (float) Math.cos((1 - e) * Math.PI * 3), sc = 0.2F + 0.8F * e + (in < 1 ? (float) Math.sin(in * Math.PI) * 0.08F : 0);
		float size = 240 * sc, w = Math.max(4, size * Math.abs(spin));
		V.img(g, V.tex("badge_" + b), 640 - w / 2, 220 - size / 2, w, size, 128, 128, 0xFFFFFFFF);
	}
}
