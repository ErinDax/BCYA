package cn.erindax.bcya.client.mask;

import cn.erindax.bcya.mask.slot.MaskSlotActions;
import cn.erindax.bcya.mask.slot.MaskSlots;
import cn.erindax.bcya.mask.slot.net.MaskSlotClickPayload;
import cn.erindax.bcya.mask.slot.net.MaskSlotCreativePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class MaskSlotWidget {

	private static final int FRAME_U = 76;
	private static final int FRAME_V = 61;
	private static final int FRAME_SIZE = 18;

	private MaskSlotWidget() {
	}

	public static void render(GuiGraphics graphics, Font font, LocalPlayer player, int x, int y, int mouseX, int mouseY) {
		graphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, x - 1, y - 1, FRAME_U, FRAME_V, FRAME_SIZE, FRAME_SIZE);
		ItemStack stack = MaskSlots.get(player);
		if (!stack.isEmpty()) {
			graphics.renderItem(stack, x, y);
			graphics.renderItemDecorations(font, stack, x, y);
		}
		if (isHovering(x, y, mouseX, mouseY)) {
			AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
		}
	}

	public static void renderTooltip(GuiGraphics graphics, Font font, LocalPlayer player, AbstractContainerMenu menu,
			int x, int y, int mouseX, int mouseY) {
		ItemStack stack = MaskSlots.get(player);
		if (!stack.isEmpty() && menu.getCarried().isEmpty() && isHovering(x, y, mouseX, mouseY)) {
			graphics.renderTooltip(font, stack, mouseX, mouseY);
		}
	}

	public static boolean isHovering(int x, int y, double mouseX, double mouseY) {
		return mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17;
	}

	public static boolean isClickButton(int button) {
		return button == 0 || button == 1;
	}

	public static void clickSurvival() {
		ClientPlayNetworking.send(new MaskSlotClickPayload(Screen.hasShiftDown()));
	}

	public static void clickCreative(LocalPlayer player, AbstractContainerMenu menu) {
		boolean changed = Screen.hasShiftDown() ? MaskSlotActions.quickMove(player) : MaskSlotActions.click(player, menu);
		if (changed) {
			player.inventoryMenu.broadcastChanges();
			sendCreative(player);
		}
	}

	public static void sendCreative(LocalPlayer player) {
		ClientPlayNetworking.send(new MaskSlotCreativePayload(MaskSlots.get(player).copy()));
	}
}
