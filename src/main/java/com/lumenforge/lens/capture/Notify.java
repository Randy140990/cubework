package com.lumenforge.lens.capture;

import com.lumenforge.lens.Lens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.nio.file.Path;

/** Little messages: a toast in the corner, and a line in chat you can click to open the folder. */
public final class Notify {
	private static final SystemToast.SystemToastId ID = new SystemToast.SystemToastId(6000L);

	private Notify() {}

	public static void toast(String title, String text) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> SystemToast.addOrUpdate(mc.getToastManager(), ID, Component.literal(title), text == null ? null : Component.literal(text)));
	}

	/** Something went wrong: a toast, and the reason in chat (red), so it can't be missed. */
	public static void failed(String title, String reason) {
		Minecraft mc = Minecraft.getInstance();
		toast(title, reason);
		Lens.LOG.warn("{}: {}", title, reason);
		mc.execute(() -> {
			if (mc.player == null) return;
			mc.player.displayClientMessage(Component.literal("[" + Lens.NAME + "] ").withStyle(ChatFormatting.DARK_AQUA)
					.append(Component.literal(title + ": " + reason).withStyle(ChatFormatting.RED)), false);
		});
	}

	/** A finished file: a toast, and in chat its name and a link to the folder. */
	public static void saved(String title, Path file, String detail) {
		Minecraft mc = Minecraft.getInstance();
		toast(title, file.getFileName() + " · " + detail);
		mc.execute(() -> {
			if (mc.player == null) return;
			Component open = Component.literal("[Open folder]").withStyle(Style.EMPTY.withColor(0x8B5CF6).withUnderlined(true)
					.withClickEvent(new ClickEvent.OpenFile(file.getParent().toString()))
					.withHoverEvent(new HoverEvent.ShowText(Component.literal(file.getParent().toString()))));
			Component line = Component.literal("[" + Lens.NAME + "] ").withStyle(ChatFormatting.DARK_AQUA)
					.append(Component.literal(title + ": " + file.getFileName() + " (" + detail + ") ").withStyle(ChatFormatting.GRAY)).append(open);
			mc.player.displayClientMessage(line, false);
		});
	}
}
