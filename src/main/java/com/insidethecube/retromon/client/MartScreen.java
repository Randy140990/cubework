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
		// The UI Viewer's shop: the clerk's words in the text box, your money at the top left, the list at the top right
		// with prices, how many you have of the one you're on, and how many to buy in its own panel.
		if (!box.empty()) {
			box.speaker = "CLERK";
			box.renderV(g, font, 140, 26, 1000, ticks, partial, step == Step.BYE);
		}
		String money = Ui.money(d.money);
		float mw = Math.max(V.width(money, 26, true), V.labelWidth("Money")) + 44;
		V.panel(g, 30, 30, mw, 80, 22, false);
		V.label(g, "Money", 52, 46);
		V.text(g, money, 52, 66, 26, V.TEXT, true);
		if (step == Step.BYE) return;
		if (step == Step.MENU) {
			vchoice(g, menu, V.W - 140 - 300, V.H - 26 - 150 - V.choiceHeight(menu.options.size()), 300, this::menuPick);
			return;
		}
		List<String> l = list();
		float lx = 760, ly = 30, lw = 480;
		int rows = Math.min(l.size() + 1, 9);
		V.panel(g, lx, ly, lw, V.choiceHeight(rows));
		int top = Math.max(0, Math.min(at - rows + 1, l.size() + 1 - rows));
		for (int k = 0; k < rows; k++) {
			int i = top + k, idx = i;
			float ty = ly + 12 + k * 54;
			boolean on = i == at && step != Step.HOW_MANY || (step == Step.HOW_MANY && i < l.size() && l.get(i).equals(picked));
			if (vover(lx + 12, ty, lw - 24, 46) && step != Step.HOW_MANY) at = i;
			if (i == l.size()) V.item(g, "CANCEL", null, lx + 12, ty, lw - 24, 46, on, false, 21);
			else {
				Item it = Item.find(l.get(i));
				V.item(g, it == null ? l.get(i) : it.name(), Ui.money(price(l.get(i))), lx + 12, ty, lw - 24, 46, on, false, 21);
			}
			if (step != Step.HOW_MANY) vhit(lx + 12, ty, lw - 24, 46, () -> pick(idx));
		}
		// How many of it you have.
		if (at < l.size()) {
			V.panel(g, 30, 600, 300, 52, 24, true);
			V.label(g, "In your bag", 52, 619);
			V.text(g, String.valueOf(d.count(l.get(at))), 52 + V.labelWidth("In your bag") + 16, 613, 22, V.TEXT, true);
		}
		if (step == Step.HOW_MANY) {
			float qx = 440, qy = 300, qw = 300, qh = 128;
			V.panel(g, qx, qy, qw, qh);
			V.label(g, "How many?", qx + 22, qy + 18);
			V.text(g, "x" + String.format("%02d", count), qx + 22, qy + 40, 38, V.TEXT, true);
			V.right(g, Ui.money(price(picked) * count), qx + qw - 22, qy + 48, 26, V.TEXT, true);
			float bw = 54;
			for (int b = 0; b < 3; b++) {
				String t = b == 0 ? "-" : b == 1 ? "+" : "OK";
				float bx = qx + 22 + b * (bw + 10), by = qy + 88;
				boolean over = vover(bx, by, b == 2 ? qw - 44 - 2 * (bw + 10) : bw, 30);
				float w = b == 2 ? qw - 44 - 2 * (bw + 10) : bw;
				V.pill(g, bx, by, w, 30, b == 2 ? (over ? V.GREEN : 0xCC41BF84) : over ? V.CREAM : 0x33FFF4D8);
				V.centred(g, t, bx + w / 2, by + 6, 17, b == 2 ? 0xFFFFFFFF : over ? V.INK : V.TEXT, true);
				Runnable act = b == 0 ? () -> count = Math.max(1, count - 1) : b == 1 ? () -> count = Math.min(maxCount(), count + 1) : this::confirm;
				vhit(bx, by, w, 30, act);
			}
		}
	}
}
