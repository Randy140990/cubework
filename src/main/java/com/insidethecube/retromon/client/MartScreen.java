package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;

import com.insidethecube.retromon.engine.Item;
import com.insidethecube.retromon.engine.Places;
import com.insidethecube.retromon.game.Net;
import com.insidethecube.retromon.game.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;

/** The Poké Mart counter: BUY (this Mart's own list, as in Yellow) or SELL (half price), with how many. */
public class MartScreen extends GameScreen {
	private enum Step {
		MENU, BUY, SELL, HOW_MANY, BYE
	}

	private Step step = Step.MENU, from = Step.BUY;
	private final Choice menu = new Choice(List.of("BUY", "SELL", "QUIT"));
	private final List<String> stock;
	private int at, count = 1;
	private String picked;
	private final TextBox box = new TextBox(List.of("Hi there! May I help you?"));

	public MartScreen(String mart) {
		super("Mart");
		Places.Mart m = Places.martFor(mart);
		this.stock = m == null ? List.of("poke_ball", "potion") : m.items();
	}

	private List<String> sellable() {
		List<String> out = new ArrayList<>();
		for (String id : ClientState.data.items.keySet()) {
			Item it = Item.find(id);
			if (it != null && !it.keyItem() && it.price() > 0) out.add(id);
		}
		return out;
	}

	private List<String> list() {
		return step == Step.SELL || (step == Step.HOW_MANY && from == Step.SELL) ? sellable() : stock;
	}

	private int price(String id) {
		Item it = Item.find(id);
		if (it == null) return 0;
		return from == Step.SELL ? it.price() / 2 : it.price();
	}

	private int maxCount() {
		PlayerData d = ClientState.data;
		if (from == Step.SELL) return Math.max(1, d.count(picked));
		int p = Math.max(1, price(picked));
		return Math.max(1, Math.min(99, d.money / p));
	}

	@Override
	public void tick() {
		super.tick();
		box.tick();
	}

	private void open(Step s) {
		step = s;
		from = s;
		at = 0;
		box.set(List.of(s == Step.BUY ? "Take your time." : "What would you like to sell?"));
	}

	private void pick(int i) {
		List<String> l = list();
		if (i >= l.size()) {
			step = Step.MENU;
			box.set(List.of("Is there anything else I can do?"));
			return;
		}
		picked = l.get(i);
		Item it = Item.find(picked);
		if (from == Step.BUY && ClientState.data.money < price(picked)) {
			box.set(List.of("You don't have enough money."));
			return;
		}
		count = 1;
		step = Step.HOW_MANY;
		box.set(List.of(it.name() + "? How many?"));
	}

	private void confirm() {
		PlayerData d = ClientState.data;
		CompoundTag t = new CompoundTag();
		t.putString("item", picked);
		t.putInt("n", count);
		ClientPlayNetworking.send(new Net.Act(from == Step.SELL ? "sell" : "buy", t));
		// Shown straight away; the game's count follows.
		if (from == Step.SELL) {
			d.give(picked, -count);
			d.money += price(picked) * count;
		} else {
			d.give(picked, count);
			d.money -= price(picked) * count;
		}
		Sounds.jingle("get_item");
		step = from;
		box.set(List.of(from == Step.SELL ? "Thank you! Here's your money." : "Here you are! Thank you!"));
	}

	private void bye() {
		step = Step.BYE;
		box.set(List.of("Thank you! Please come again!"));
	}

	@Override
	protected boolean key(KeyEvent e) {
		switch (step) {
			case BYE -> {
				if ((Ui.isA(e) || Ui.isB(e)) && box.press()) onClose();
			}
			case MENU -> {
				if (menu.key(e)) return true;
				if (Ui.isB(e)) bye();
				else if (Ui.isA(e)) menuPick(menu.at);
			}
			case BUY, SELL -> {
				int n = list().size() + 1;
				if (Ui.isUp(e)) at = Math.floorMod(at - 1, n);
				else if (Ui.isDown(e)) at = Math.floorMod(at + 1, n);
				else if (Ui.isA(e)) pick(at);
				else if (Ui.isB(e)) {
					step = Step.MENU;
					box.set(List.of("Is there anything else I can do?"));
				}
			}
			case HOW_MANY -> {
				if (Ui.isUp(e) || Ui.isRight(e)) count = Math.min(maxCount(), count + (Ui.isRight(e) ? 10 : 1));
				else if (Ui.isDown(e) || Ui.isLeft(e)) count = Math.max(1, count - (Ui.isLeft(e) ? 10 : 1));
				else if (Ui.isA(e)) confirm();
				else if (Ui.isB(e)) step = from;
			}
		}
		return true;
	}

	private void menuPick(int i) {
		if (i == 0) open(Step.BUY);
		else if (i == 1) {
			if (sellable().isEmpty()) box.set(List.of("You don't have anything to sell."));
			else open(Step.SELL);
		} else bye();
	}

	@Override
	protected void clickAnywhere() {
		if (step == Step.BYE && box.press()) onClose();
	}

	@Override
	protected void back() {
		if (step == Step.MENU) bye();
		else if (step == Step.HOW_MANY) step = from;
		else if (step == Step.BYE) onClose();
		else step = Step.MENU;
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	protected void draw(GuiGraphics g, float partial) {
		PlayerData d = ClientState.data;
		int m = 6 * s, h = boxHeight();
		box.render(g, font, m, height - h - m, width - 2 * m, h, s, ticks, step == Step.BYE);
		// Money, top right.
		String money = Ui.money(d.money);
		int mw = Math.max(Ui.width(font, money, s), Ui.width(font, "MONEY", s)) + 20 * s;
		Ui.tagged(g, width - mw - m, m, mw, 26 * s, s, Ui.GOLD);
		Ui.text(g, font, "MONEY", width - mw - m + 8 * s, m + 4 * s, Ui.MUTED, s);
		Ui.right(g, font, money, width - m - 8 * s, m + 14 * s, Ui.TEXT, s);
		if (step == Step.BYE) return;
		if (step == Step.MENU) {
			int w = menu.width(font, s);
			choice(g, menu, m, m, w, this::menuPick);
			return;
		}
		// The list: what's for sale (or what you have), the price and how many you've got.
		List<String> l = list();
		int lw = Math.min(width - 2 * m, 220 * s), row = 14 * s, lh = (l.size() + 1) * row + 10 * s, lx = m, ly = m;
		Ui.tagged(g, lx, ly, lw, lh, s, from == Step.SELL ? Ui.GREEN : Ui.BLUE);
		for (int i = 0; i <= l.size(); i++) {
			int ty = ly + 6 * s + i * row;
			int idx = i;
			boolean on = i == at && step != Step.HOW_MANY || (step == Step.HOW_MANY && i < l.size() && l.get(i).equals(picked));
			if (over(lx, ty - 2 * s, lw, row) && step != Step.HOW_MANY) at = i;
			if (on) Ui.round(g, lx + 4 * s, ty - 2 * s, lw - 8 * s, row - s, 0x334A8FE0, s);
			if (i == l.size()) {
				Ui.text(g, font, "CANCEL", lx + 12 * s, ty + s, Ui.MUTED, s);
			} else {
				Item it = Item.find(l.get(i));
				Ui.text(g, font, it == null ? l.get(i) : it.name(), lx + 12 * s, ty + s, Ui.TEXT, s);
				Ui.right(g, font, Ui.money(price(l.get(i))), lx + lw - 46 * s, ty + s, Ui.GOLD, s);
				Ui.right(g, font, "x" + d.count(l.get(i)), lx + lw - 10 * s, ty + s, Ui.MUTED, s);
			}
			if (step != Step.HOW_MANY) hit(lx, ty - 2 * s, lw, row, () -> pick(idx));
		}
		if (step == Step.HOW_MANY) {
			int qw = 120 * s, qh = 46 * s, qx = width - qw - m, qy = height - h - m - qh - 6 * s;
			Ui.panel(g, qx, qy, qw, qh, s);
			Ui.text(g, font, "x" + count, qx + 10 * s, qy + 8 * s, Ui.TEXT, s);
			Ui.right(g, font, Ui.money(price(picked) * count), qx + qw - 10 * s, qy + 8 * s, Ui.GOLD, s);
			int bw = 22 * s, by = qy + 22 * s;
			Ui.button(g, font, qx + 8 * s, by, bw, 16 * s, "-", Ui.BLUE, over(qx + 8 * s, by, bw, 16 * s), s);
			hit(qx + 8 * s, by, bw, 16 * s, () -> count = Math.max(1, count - 1));
			Ui.button(g, font, qx + 12 * s + bw, by, bw, 16 * s, "+", Ui.BLUE, over(qx + 12 * s + bw, by, bw, 16 * s), s);
			hit(qx + 12 * s + bw, by, bw, 16 * s, () -> count = Math.min(maxCount(), count + 1));
			int ow = qw - 2 * bw - 28 * s;
			Ui.button(g, font, qx + qw - ow - 8 * s, by, ow, 16 * s, "OK", Ui.GREEN, over(qx + qw - ow - 8 * s, by, ow, 16 * s), s);
			hit(qx + qw - ow - 8 * s, by, ow, 16 * s, this::confirm);
		}
	}
}
