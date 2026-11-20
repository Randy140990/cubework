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
	/** The machine's turn (the viewer's 5.2 s): the balls go on one by one, it works, they come back. */
	private static final int HEAL_TICKS = 104;
	private static final String[] BADGES = {"BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE"};

	/** Something got, for the card held up: its name, how many, a badge (0 to 7, -1 for none), and whether it's going into the bag now. */
	private record Get(String name, int n, int badge, boolean away) {}

	/** Line numbers (as shown) to what was got on them. */
	private final java.util.Map<Integer, Get> gets = new java.util.HashMap<>();
	private int healLineAt = -1;

	/** "got 5 POKé BALLs!", "received a POTION!", "found TM28!": what, and how many. */
	private static final java.util.regex.Pattern GOT = java.util.regex.Pattern.compile("\\b(?:got|received|obtained|found) (?:a |an |the )?(?:(\\d+) )?([^!]+?)(?: from [^!]+)?!");

	private static Get itemGet(String line) {
		java.util.regex.Matcher m = GOT.matcher(line.replace("**", ""));
		if (!m.find()) return null;
		int n = m.group(1) == null ? 1 : Math.max(1, Math.min(99, Integer.parseInt(m.group(1))));
		String name = m.group(2).trim();
		com.insidethecube.retromon.engine.Item it = findItem(name);
		if (it != null) return new Get(it.name(), n, -1, false);
		String u = name.toUpperCase(java.util.Locale.ROOT);
		if (u.contains("POKéDEX") || u.contains("POKEDEX") || u.contains("TOWN MAP")) return new Get(name, n, -1, false);
		return null;
	}

	/** An item by its name ("POKé BALLs" finds POKé BALL). */
	private static com.insidethecube.retromon.engine.Item findItem(String name) {
		String n = name.trim();
		for (com.insidethecube.retromon.engine.Item it : com.insidethecube.retromon.engine.Item.ALL.values())
			if (it.name().equalsIgnoreCase(n) || (n.endsWith("s") || n.endsWith("S")) && it.name().equalsIgnoreCase(n.substring(0, n.length() - 1))) return it;
		return null;
	}

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
		// The cues (jingles on lines) and the nurse's line, by the server's line numbers.
		java.util.Map<Integer, String> serverCues = new java.util.HashMap<>();
		for (net.minecraft.nbt.Tag c : extra.getListOrEmpty("cues")) c.asString().ifPresent(v -> {
			int at = v.indexOf(':');
			if (at > 0) {
				try {
					serverCues.put(Integer.parseInt(v.substring(0, at)), v.substring(at + 1));
				} catch (NumberFormatException ignored) {
					// Not a cue.
				}
			}
		});
		int heal = extra.getIntOr("healing", -1);
		// Getting something, as the viewer's: after "got the TOWN MAP!", where it went ("put the TOWN MAP in the KEY
		// ITEMS pocket."); after a badge, that it's on your card now. The lines after move along one.
		java.util.List<String> out = new java.util.ArrayList<>();
		String whoNow = who;
		for (int i = 0; i < said.size(); i++) {
			String l = said.get(i);
			int at = out.size();
			if (serverCues.containsKey(i)) cues.put(at, serverCues.get(i));
			if (i == heal) heal = -100 - at;
			out.add(l);
			int b = badgeIn(l);
			if (b >= 0 && (l.contains("received") || l.contains("got")) && !someoneElse(whoNow, l)) {
				gets.put(at, new Get(null, 1, b, false));
				out.add("It's pinned to your **TRAINER CARD** now. That's **" + (b + 1) + " of 8**.");
				gets.put(at + 1, new Get(null, 1, b, true));
				continue;
			}
			Get it = itemGet(l);
			if (it != null && !someoneElse(whoNow, l)) {
				gets.put(at, it);
				com.insidethecube.retromon.engine.Item item = findItem(it.name());
				if (item != null) {
					String pocket = item.teaches() ? "TM & HM" : item.keyItem() ? "KEY ITEMS" : item.ball() ? "BALLS" : "ITEMS";
					out.add("**{N}** put the **" + it.name() + "** in the **" + pocket + "** pocket.");
					gets.put(at + 1, new Get(it.name(), it.n(), -1, true));
				}
			}
		}
		said = out;
		this.healLineAt = heal <= -100 ? -100 - heal : heal;
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
		this.healLine = healLineAt;
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

		if (healing() && ticks - healAt >= HEAL_TICKS) {
			healAt = -2;
			if (box.press()) done(false);
		}
	}

	private boolean healing() {
		return healAt >= 0;
	}

	/** The nurse's "I'll take your POKéMON" line said and moved on: her machine's turn (the text box goes meanwhile). */
	private boolean startHeal() {
		if (healLine < 0 || healAt != -1 || box.message() != healLine || !box.typedOut()) return false;
		healAt = ticks;
		Sounds.heal();
		return true;
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
			if (startHeal()) return true;
			if (box.press()) done(false);
			return true;
		}
		return false;
	}

	@Override
	protected void clickAnywhere() {
		if (healing() || yesNo != null || startHeal()) return;
		if (box.press()) done(false);
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
		if (!healing()) {
			g.pose().pushMatrix();
			g.pose().translate(0, V.s(rise));
			box.renderV(g, font, 140, 26, 1000, ticks, partial, yesNo == null);
			g.pose().popMatrix();
		} else machine(g, (ticks - healAt + partial) / HEAL_TICKS, (ticks + partial) / 20F);
		// Getting something: the UI Viewer's medallion held up over the world (a badge spins in instead).
		int line = box.message();
		Get got = gets.get(line);
		if (got != null) {
			float t = (ticks - lineAt + partial) / 20F;
			if (got.badge() >= 0) {
				if (!got.away()) badgeCard(g, got.badge(), t, (ticks + partial) / 20F);
				else badgeCard(g, got.badge(), 99, (ticks + partial) / 20F);
			} else itemCard(g, got, t, (ticks + partial) / 20F);
		}
		if (yesNo != null) {
			float w = 300;
			vchoice(g, yesNo, V.W - 140 - w, V.H - 26 - 150 - V.choiceHeight(yesNo.options.size()), w, i -> done(i == 0));
		}
	}

	/**
	 * Whether a line is about the one talking getting something (BLUE received a TIDAPIR!), not you: no card is held
	 * up for that (it showed a key).
	 */
	private static boolean someoneElse(String speaker, String line) {
		String who = speaker == null ? "" : speaker.trim().toUpperCase(java.util.Locale.ROOT);
		if (who.isEmpty()) return false;
		String u = line.trim().toUpperCase(java.util.Locale.ROOT);
		return u.startsWith(who + " ") || u.startsWith(who + ":");
	}

	/**
	 * The nurse's machine at work, as the viewer's: your balls (up to six) go from her hands onto its six cups one by
	 * one, it glows and they flash, then they come back. Drawn where the machine is behind her counter (the Center's
	 * room has it 6.45 to 8.55 across, 0.35 and 0.65 back, 1.36 up; she stands at 10, 3.3).
	 */
	private void machine(GuiGraphics g, float h, float secs) {
		net.minecraft.world.entity.Entity nurse = minecraft == null || minecraft.level == null ? null : minecraft.level.getEntity(npc);
		if (nurse == null) return;
		net.minecraft.world.phys.Vec3 n = nurse.position();
		int w = g.guiWidth(), gh = g.guiHeight();
		// The machine's glow while it works.
		if (h > 0.4F && h < 0.78F) {
			float[] gp = Adventure.project(n.add(7.5 - 10, 1.9, 0.18 - 3.3), w, gh);
			float[] ge = Adventure.project(n.add(7.5 - 10 + 1.6, 1.9, 0.18 - 3.3), w, gh);
			if (gp != null && ge != null) {
				float rx = Math.abs(ge[0] - gp[0]);
				int a = (int) ((0.35 + 0.3 * Math.sin(secs * 16)) * 255);
				Ui.glow(g, Math.round(gp[0]), Math.round(gp[1]), Math.round(rx), Math.round(rx / 2), (a << 24) | 0xFFB8C8);
			}
		}
		int count = Math.min(6, ClientState.data.party.size());
		for (int i = 0; i < count; i++) {
			float on0 = i * 0.06F, a = ease((h - on0) / 0.06F), back = ease((h - 0.78F - i * 0.02F) / 0.12F);
			if (h <= on0 || back >= 1) continue;
			double sx = 6.45 + (i % 3) * 1.05 - 10, sy = 1.36, sz = 0.35 + (i / 3) * 0.3 - 3.3;
			double hx = 0, hy = 1.2, hz = 0.7;
			double px, py, pz;
			if (back > 0) {
				px = sx + (hx - sx) * back;
				py = sy + (hy - sy) * back + Math.sin(back * Math.PI) * 0.45;
				pz = sz + (hz - sz) * back;
			} else {
				px = hx + (sx - hx) * a;
				py = hy + (sy - hy) * a + Math.sin(a * Math.PI) * 0.45;
				pz = hz + (sz - hz) * a;
			}
			net.minecraft.world.phys.Vec3 at = n.add(px, py, pz);
			float[] c = Adventure.project(at, w, gh), top = Adventure.project(at.add(0, 0.15, 0), w, gh);
			if (c == null || top == null) continue;
			float r = Math.max(2, Math.abs(c[1] - top[1]));
			boolean lit = h > 0.4F && h < 0.78F && Math.sin(secs * 14 + i * 1.3) > 0;
			if (lit) Ui.glow(g, Math.round(c[0]), Math.round(c[1]), Math.round(r * 2.4F), Math.round(r * 2.4F), 0xB3FF8FA8);
			g.pose().pushMatrix();
			g.pose().translate(c[0] - r, c[1] - r);
			g.pose().scale(2 * r / 128F, 2 * r / 128F);
			g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, V.tex("ball_pok_ball"), 0, 0, 0, 0, 128, 128, 128, 128);
			g.pose().popMatrix();
		}
	}

	private static float ease(float x) {
		return x < 0 ? 0 : x > 1 ? 1 : x * x * (3 - 2 * x);
	}

	private static int badgeIn(String line) {
		String u = line.toUpperCase();
		for (int i = 0; i < BADGES.length; i++) if (u.contains(BADGES[i])) return i;
		return -1;
	}

	/** The picture for something you've got (the viewer's ITEM_ICON): its own, the POKéDEX's, the map's. */
	private static String itemPicture(String name) {
		String u = name.toUpperCase(java.util.Locale.ROOT);
		if (u.contains("POKéDEX") || u.contains("POKEDEX")) return "icon_dex";
		return V.itemIcon(name);
	}

	/**
	 * Getting something, as Let's Go does it (the viewer's): the thing itself pops up in a round pearly medallion,
	 * rays turning behind it and sparkles round it; more than one counts in, x1 up to x5; on the next line it drops
	 * into your bag.
	 */
	private void itemCard(GuiGraphics g, Get it, float t, float secs) {
		float mx0 = 520, my0 = 90, cx = mx0 + 120, cy = my0 + 120;
		float scale, a;
		float dx = 0, dy = 0;
		if (!it.away()) {
			float p = Math.min(1, t / 0.5F);
			if (p < 0.6F) {
				float q = V.bezier(0.2F, 1.4F, 0.4F, 1, p / 0.6F);
				scale = 0.2F + (1.12F - 0.2F) * q;
				a = Math.max(0, Math.min(1, q));
			} else {
				float q = V.bezier(0.2F, 1.4F, 0.4F, 1, (p - 0.6F) / 0.4F);
				scale = 1.12F + (1 - 1.12F) * q;
				a = 1;
			}
		} else {
			float p = Math.min(1, t / 0.6F), q = V.bezier(0.42F, 0, 1, 1, p);
			dx = 40 * q;
			dy = 300 * q;
			scale = 1 - 0.8F * q;
			a = 1 - q;
		}
		if (a <= 0.01F) return;
		g.pose().pushMatrix();
		g.pose().translate(V.x(cx + dx), V.y(cy + dy));
		g.pose().scale(scale, scale);
		g.pose().translate(-V.x(cx), -V.y(cy));
		// The rays, turning slowly behind it.
		V.rays(g, cx, cy, 79, 158, 8, 22, secs / 12 * Math.PI * 2, Ui.alpha(0xFFFFFFFF, 0.55F * a));
		// The medallion: a pearly rim, white inside, the thing in the middle.
		V.shadow(g, mx0, my0, 240, 240, 120, 1.3F * a);
		V.rrh(g, mx0, my0, 240, 240, 120, V.HOLO_AT, alphas(V.HOLO, a));
		V.ellipse(g, cx, cy, 114, 114, Ui.alpha(0xFFF3F6FD, a));
		V.glow(g, mx0 + 6 + 228 * 0.4F, my0 + 6 + 228 * 0.3F, 150, 150, Ui.alpha(0xFFFFFFFF, a));
		V.icon(g, itemPicture(it.name()), cx - 70, cy - 70, 140, Ui.alpha(0xFFFFFFFF, a));
		// The sparkles, twinkling.
		float[][] sp = {{-6, 20, 34, 0}, {210, 10, 26, 0.5F}, {222, 170, 22, 1}, {-14, 180, 18, 1.4F}};
		for (float[] k : sp) {
			float ph = (((secs - k[3]) % 1.6F) + 1.6F) % 1.6F / 1.6F, tw = (float) Math.sin(ph * Math.PI);
			float sz = k[2] * (0.4F + 0.6F * tw), scx = mx0 + k[0] + k[2] / 2, scy = my0 + k[1] + k[2] / 2;
			if (tw * a < 0.02F) continue;
			V.glow(g, scx, scy, sz * 0.8F, sz * 0.8F, Ui.alpha(0xFFFFE27A, 0.8F * tw * a));
			V.icon(g, "icon_sparkle", scx - sz / 2, scy - sz / 2, sz, Ui.alpha(0xFFFFFFFF, tw * a));
		}
		g.pose().popMatrix();
		// More than one: they count in, one at a time.
		if (!it.away() && it.n() > 1) {
			int k = Math.max(0, Math.min(it.n(), (int) Math.floor((t * 1000 - 350) / 260)));
			if (k >= 1) {
				String x = "x" + k;
				float w = 14 + k * 30 + (k - 1) * 6 + 6 + 4 + V.titleWidth(x, 24) + 14, h = 42, px = 640 - w / 2, py = 70 + 272;
				V.shadow(g, px, py, w, h, h / 2, 1);
				V.pill(g, px, py, w, h, 0xFFFFFFFF);
				float since = (t * 1000 - 350 - k * 260) / 300F;
				for (int q = 0; q < k; q++) {
					float ix = px + 14 + q * 36, iy = py + 6, ia = 1, isz = 30;
					if (q == k - 1 && since < 1) {
						float e = Math.max(0, since);
						iy -= 30 * (1 - e);
						isz = 30 * (0.4F + 0.6F * e);
						ia = e;
						ix += (30 - isz) / 2;
						iy += (30 - isz) / 2;
					}
					V.icon(g, itemPicture(it.name()), ix, iy, isz, Ui.alpha(0xFFFFFFFF, ia));
				}
				V.title(g, x, px + 14 + k * 30 + (k - 1) * 6 + 6 + 4, V.capsTop(py + h / 2, 24), 24, 0xFFE5525C);
			}
		}
	}

	private static int[] alphas(int[] cols, float a) {
		int[] out = new int[cols.length];
		for (int i = 0; i < cols.length; i++) out[i] = Ui.alpha(cols[i], a);
		return out;
	}

	/**
	 * A badge won (the viewer's): the world dimmed round a warm light, rays in the badge's colour turning behind it,
	 * the badge spinning in (once round and a half, a little past, back) and settling.
	 */
	private void badgeCard(GuiGraphics g, int b, float t, float secs) {
		int[] cols = {0xFF8E8A84, 0xFF3F86D0, 0xFFE2B320, 0xFF4FA84A, 0xFFD04A8E, 0xFFD6962A, 0xFFD8443C, 0xFF4E9A3A};
		float dimIn = Math.min(1, t / 0.4F);
		V.radialSoft(g, 640, 302, 459, new float[] {0, 1}, new int[] {0x00FFF4D8, Ui.alpha(0xFF0A181E, 0.55F * dimIn)});
		V.annulus(g, 640, 302, 459, 1600, Ui.alpha(0xFF0A181E, 0.55F * dimIn), Ui.alpha(0xFF0A181E, 0.55F * dimIn));
		float cx = 640, cy = 220;
		V.rays(g, cx, cy, 115, 260, 10, 20, secs / 14 * Math.PI * 2, Ui.alpha(cols[b], 0.33F));
		V.radialSoft(g, cx, cy, 165, new float[] {0, 1}, new int[] {0xE6FFF4D8, 0x00FFF4D8});
		// The spin: from small and turned away to a little big, then settled.
		float p = Math.min(1, t / 1.1F), q;
		float sc, turn, a;
		if (p < 0.7F) {
			q = V.bezier(0.2F, 0.8F, 0.3F, 1, p / 0.7F);
			sc = 0.2F + (1.08F - 0.2F) * q;
			turn = 540 + (-12 - 540) * q;
			a = q;
		} else {
			q = V.bezier(0.2F, 0.8F, 0.3F, 1, (p - 0.7F) / 0.3F);
			sc = 1.08F + (1 - 1.08F) * q;
			turn = -12 + 12 * q;
			a = 1;
		}
		float size = 240 * sc, w = Math.max(3, size * Math.abs((float) Math.cos(Math.toRadians(turn))));
		V.img(g, V.tex("badge_" + b), cx - w / 2, cy - size / 2 + 14, w, size, 128, 128, Ui.alpha(0xFF000000, 0.3F * a));
		V.img(g, V.tex("badge_" + b), cx - w / 2, cy - size / 2, w, size, 128, 128, Ui.alpha(0xFFFFFFFF, a));
	}
}
