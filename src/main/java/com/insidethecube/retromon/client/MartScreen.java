package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/**
 * The Poké Mart counter, as the UI Viewer's "Shop": the clerk's welcome with BUY, SELL and SEE YOU; BUY opens the
 * shelf as Let's Go's (a white page of tiles, two across, each thing's picture in its colour, its name and price; the
 * one you're on in sunny yellow with a red arrow), your money at the top left and how many you have at the foot; then
 * how many, the clerk's "That'll be... OK?", and thank you. SELL lists what you could sell, one at a time at half price.
 */
public class MartScreen extends GameScreen {
	private enum Step {
		TALK, SHELF, HOW_MANY, SELL
	}

	private Step step = Step.TALK;
	private final List<String> stock;
	private int at, count = 1, shelfTop, sellAt;
	private String picked;
	private boolean first = true, buying;
	private final TextBox box = new TextBox(List.of(""));
	/** The question at the end of the clerk's words (its answers), and what to do with the answer (-1: no answer, B). */
	private Choice ask;
	private IntConsumer then;

	public MartScreen(String mart) {
		super("Mart");
		Places.Mart m = Places.martFor(mart);
		this.stock = m == null ? List.of("poke_ball", "potion") : m.items();
		box.speaker = "CLERK";
		welcome();
	}

	/** The clerk says something; with answers, they come up once it's said. */
	private void say(String line, List<String> answers, IntConsumer next) {
		step = Step.TALK;
		box.set(List.of(line));
		ask = null;
		pending = answers;
		then = next;
	}

	private List<String> pending;

	private void welcome() {
		say(first ? "Hi there! Welcome to the POKé MART. How can I help?" : "Is there anything else I can do for you?", List.of("BUY", "SELL", "SEE YOU"), i -> {
			first = false;
			if (i == 0) {
				buying = true;
				step = Step.SHELF;
			} else if (i == 1) {
				if (sellable().isEmpty()) say("You don't have anything I can buy, I'm afraid.", null, j -> welcome());
				else {
					sellAt = 0;
					step = Step.SELL;
				}
			} else say("Thanks! Please come again!", null, j -> onClose());
		});
	}

	private List<String> sellable() {
		List<String> out = new ArrayList<>();
		for (String id : ClientState.data.items.keySet()) {
			Item it = Item.find(id);
			if (it != null && !it.keyItem() && it.price() > 0 && ClientState.data.count(id) > 0) out.add(id);
		}
		return out;
	}

	private int maxCount() {
		Item it = Item.find(picked);
		int p = it == null ? 1 : Math.max(1, it.price());
		return Math.max(1, Math.min(99, ClientState.data.money / p));
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
		if (step == Step.TALK && ask == null && pending != null && box.atEnd()) {
			ask = new Choice(pending);
			pending = null;
		}
	}

	private void answer(int i) {
		IntConsumer t = then;
		ask = null;
		pending = null;
		then = null;
		if (t != null) t.accept(i);
	}

	private void pickShelf(int i) {
		if (i < 0 || i >= stock.size()) return;
		picked = stock.get(i);
		at = i;
		count = 1;
		step = Step.HOW_MANY;
		Sounds.press();
	}

	private void howMany() {
		Item it = Item.find(picked);
		if (it == null) return;
		int cost = it.price() * count;
		say(it.name() + ", and you want " + count + ". That'll be " + Ui.money(cost) + ". OK?", List.of("YES", "NO"), i -> {
			if (i != 0) {
				step = Step.SHELF;
				return;
			}
			PlayerData d = ClientState.data;
			if (d.money < cost) {
				say("Sorry, you don't have enough money.", null, j -> step = Step.SHELF);
				return;
			}
			CompoundTag t = new CompoundTag();
			t.putString("item", picked);
			t.putInt("n", count);
			ClientPlayNetworking.send(new Net.Act("buy", t));
			// Shown straight away; the game's count follows.
			d.give(picked, count);
			d.money -= cost;
			Sounds.jingle("get_item");
			say("Here you are! Thank you!", null, j -> step = Step.SHELF);
		});
	}

	private void pickSell(int i) {
		List<String> l = sellable();
		if (i < 0 || i >= l.size()) {
			welcome();
			return;
		}
		String id = l.get(i);
		Item it = Item.find(id);
		if (it == null) return;
		int pay = it.price() / 2;
		Sounds.press();
		say("I can pay " + Ui.money(pay) + " for the " + it.name() + ". Is that OK?", List.of("YES", "NO"), k -> {
			if (k != 0) {
				step = Step.SELL;
				return;
			}
			CompoundTag t = new CompoundTag();
			t.putString("item", id);
			t.putInt("n", 1);
			ClientPlayNetworking.send(new Net.Act("sell", t));
			ClientState.data.give(id, -1);
			ClientState.data.money += pay;
			Sounds.jingle("get_item");
			box.speaker = null;
			say("You handed over the **" + it.name() + "** and got " + Ui.money(pay) + ".", null, j -> {
				box.speaker = "CLERK";
				if (sellable().isEmpty()) welcome();
				else {
					sellAt = Math.min(sellAt, sellable().size());
					step = Step.SELL;
				}
			});
		});
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case TALK -> {
				if (ask != null) {
					if (ask.key(e)) return true;
					if (Ui.isA(e)) {
						Sounds.select();
						answer(ask.at);
					} else if (Ui.isB(e)) answer(ask.options.size() - 1);
					return true;
				}
				if (Ui.isA(e) || Ui.isB(e)) {
					boolean done = box.press();
					if (done && pending == null) answer(-1);
				}
			}
			case SHELF -> {
				int n = stock.size();
				if (Ui.isLeft(e) || Ui.isRight(e) || Ui.isUp(e) || Ui.isDown(e)) {
					int was = at;
					if (Ui.isLeft(e)) at = Math.floorMod(at - 1, n);
					else if (Ui.isRight(e)) at = Math.floorMod(at + 1, n);
					else if (Ui.isUp(e)) at = at - 2 >= 0 ? at - 2 : at;
					else at = at + 2 < n ? at + 2 : at;
					if (was != at) Sounds.select();
				} else if (Ui.isA(e)) pickShelf(at);
				else if (Ui.isB(e)) {
					buying = false;
					welcome();
				}
			}
			case HOW_MANY -> {
				if (Ui.isUp(e) || Ui.isDown(e)) count = Math.max(1, Math.min(maxCount(), count + (Ui.isUp(e) ? 1 : -1)));
				else if (Ui.isRight(e) || Ui.isLeft(e)) count = Math.max(1, Math.min(maxCount(), count + (Ui.isRight(e) ? 10 : -10)));
				else if (Ui.isA(e)) howMany();
				else if (Ui.isB(e)) step = Step.SHELF;
			}
			case SELL -> {
				int n = sellable().size() + 1;
				if (Ui.isUp(e)) sellAt = Math.floorMod(sellAt - 1, n);
				else if (Ui.isDown(e)) sellAt = Math.floorMod(sellAt + 1, n);
				else if (Ui.isA(e)) pickSell(sellAt);
				else if (Ui.isB(e)) welcome();
			}
		}
		return true;
	}

	@Override
	protected void clickAnywhere() {
		if (step == Step.TALK && ask == null && pending == null && box.press()) answer(-1);
	}

	@Override
	protected void back() {
		switch (step) {
			case TALK -> {
				if (ask != null) answer(ask.options.size() - 1);
				else clickAnywhere();
			}
			case SHELF, SELL -> welcome();
			case HOW_MANY -> step = Step.SHELF;
		}
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		boolean shop = buying || step == Step.SELL || step == Step.HOW_MANY;
		// Your money at the top left (the viewer's moneyBox), once you're buying or selling.
		if (shop) {
			float mh = 12 + 18 + 30 * 1.36F + 12;
			V.panel(g, 30, 30, 300, mh);
			V.label(g, Lang.t("Money"), 52, 42);
			V.text(g, Ui.money(d.money), 52, 42 + 18, 30, V.INK, true);
		}
		// How many of it you have, at the foot, while buying.
		if (buying && (step == Step.SHELF || step == Step.HOW_MANY || step == Step.TALK) && at < stock.size()) {
			String n = String.valueOf(d.count(stock.get(at)));
			V.panel(g, 30, 600, 300, 54, 22, true);
			V.label(g, Lang.t("In your bag"), 52, V.capsTop(627, 13));
			V.text(g, n, 52 + V.labelWidth(Lang.t("In your bag")) + 14, V.capsTop(627, 22), 22, V.INK, true);
		}
		switch (step) {
			case SHELF -> drawShelf(g);
			case HOW_MANY -> drawHowMany(g);
			case SELL -> drawSell(g, d);
			case TALK -> {
				box.renderV(g, font, 140, 26, 1000, ticks, partial, ask == null && pending == null);
				if (ask != null) vchoice(g, ask, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(ask.options.size()), 300, this::answer);
			}
		}
	}

	/** The shelf (the viewer's martPick): a white page, BUY in coral, the tiles two across. */
	private void drawShelf(GuiGraphics g) {
		int n = stock.size(), rows = (n + 1) / 2, shown = Math.min(rows, 7);
		if (at / 2 < shelfTop) shelfTop = at / 2;
		if (at / 2 >= shelfTop + shown) shelfTop = at / 2 - shown + 1;
		float px = 600, py = 30, pw = 650, th = 74, gap = 12;
		float ph = 22 + 22 * 1.2F + 12 + shown * th + (shown - 1) * gap + 18;
		V.panel(g, px, py, pw, ph, 30, false);
		V.title(g, Lang.t("BUY"), px + 22 + 8, py + 22, 22, 0xFFE5525C);
		float gx = px + 22, gy = py + 22 + 22 * 1.2F + 12, tw = (pw - 44 - gap) / 2;
		for (int k = shelfTop * 2; k < Math.min(n, (shelfTop + shown) * 2); k++) {
			int idx = k, r = k / 2 - shelfTop;
			float x = gx + (k % 2) * (tw + gap), y = gy + r * (th + gap);
			if (vover(x, y, tw, th)) at = k;
			boolean on = k == at;
			Item it = Item.find(stock.get(k));
			String name = it == null ? stock.get(k) : it.name();
			int[] kc = V.GOODS[V.goodsKind(name)];
			if (on) {
				V.softShadow(g, x, y + 8, tw, th, 16, 0x4DE8961E);
				V.rr(g, x - 6, y - 6, tw + 12, th + 12, 28, 0xFFFFC93C);
				V.rr(g, x - 3, y - 3, tw + 6, th + 6, 25, 0xFFFFFFFF);
				V.sel(g, x, y, tw, th, 22);
				V.text(g, "▶", x - 18 - 4, V.capsTop(y + th / 2, 20), 20, V.RED, true);
			} else {
				V.rr(g, x, y, tw, th, 22, 0xFFE8EDF6);
				V.rr(g, x + 2, y + 2, tw - 4, th - 4, 20, 0xFFF5F8FD);
			}
			float icx = x + 10 + 26, icy = y + th / 2;
			V.ellipse(g, icx, icy, 29, 29, kc[1]);
			V.ellipse(g, icx, icy, 26, 26, 0xFFFFFFFF);
			V.icon(g, V.itemIcon(name), icx - 17, icy - 17, 34);
			String price = Ui.money(it == null ? 0 : it.price());
			float prw = V.width(price, 18, true);
			V.text(g, price, x + tw - 16 - prw, V.capsTop(icy, 18), 18, V.INK, true);
			List<String> nl = V.wrap(name, tw - 10 - 52 - 12 - 16 - prw - 8, 17, true);
			float lh = 17 * 1.15F, ny = icy - Math.min(2, nl.size()) * lh / 2;
			for (int q = 0; q < Math.min(2, nl.size()); q++) V.text(g, nl.get(q), x + 10 + 52 + 12, ny + q * lh + 1, 17, V.INK, true);
			vhit(x, y, tw, th, () -> pickShelf(idx));
		}
		if (shelfTop > 0) V.tri(g, px + pw / 2, gy - 8, 14, 3, V.MUTED);
		if (shelfTop + shown < rows) V.tri(g, px + pw / 2, gy + shown * (th + gap) - 2, 14, 1, V.MUTED);
		V.hints(g, "A", "Choose", "B", "Back");
	}

	/** How many (the viewer's): x01, what it comes to, up and down to change. */
	private void drawHowMany(GuiGraphics g) {
		Item it = Item.find(picked);
		float qx = 440, qy = 300, qw = 300, qh = 16 + 18 + 6 + 38 * 1.36F + 14 * 1.36F + 16;
		V.panel(g, qx, qy, qw, qh);
		V.label(g, Lang.t("How many?"), qx + 22, qy + 16);
		float ry = qy + 16 + 18 + 6;
		V.text(g, "x" + String.format("%02d", count), qx + 22, ry, 38, V.INK, true);
		V.right(g, Ui.money((it == null ? 0 : it.price()) * count), qx + qw - 22, ry + (38 - 26) * 0.7F, 26, V.INK, true);
		V.text(g, Lang.t("Up and down to change"), qx + 22, ry + 38 * 1.36F, 14, V.MUTED, true);
		vhit(qx, qy, qw, qh, this::howMany);
		V.hints(g, "A", "Buy", "B", "Back");
	}

	/** What you could sell, each with how many you have (the viewer's choose list at the right), and CANCEL. */
	private void drawSell(GuiGraphics g, PlayerData d) {
		List<String> l = sellable();
		float lx = 760, ly = 30, lw = 480;
		int rows = Math.min(l.size() + 1, 10);
		V.panel(g, lx, ly, lw, V.choiceHeight(rows));
		int top = Math.max(0, Math.min(sellAt - rows + 1, l.size() + 1 - rows));
		for (int k = 0; k < rows; k++) {
			int i = top + k, idx = i;
			float ty = ly + 12 + k * 54;
			if (vover(lx + 12, ty, lw - 24, 46)) sellAt = i;
			boolean on = i == sellAt;
			if (i == l.size()) V.item(g, "CANCEL", null, lx + 12, ty, lw - 24, 46, on, false, 21);
			else {
				Item it = Item.find(l.get(i));
				V.goods(g, it == null ? l.get(i) : it.name(), "x" + d.count(l.get(i)), lx + 12, ty, lw - 24, 46, on, 21);
			}
			vhit(lx + 12, ty, lw - 24, 46, () -> pickSell(idx));
		}
		V.hints(g, "A", "Sell", "B", "Back");
	}
}
