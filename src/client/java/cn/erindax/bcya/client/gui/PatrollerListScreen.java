package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.entity.net.PatrollerListActionPayload;
import cn.erindax.bcya.entity.net.PatrollerListPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PatrollerListScreen extends Screen {

	private static final int ROW_HEIGHT = 36;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 56;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int TEXT_COLOR = 0xFFFFFF;
	private static final int SUB_COLOR = 0xA0A0A0;

	private final PatrollerListPayload data;
	private PatrollerList list;

	public PatrollerListScreen(PatrollerListPayload data) {
		super(Component.translatable("screen.bcya.patrol_list.title"));
		this.data = data;
	}

	@Override
	protected void init() {
		list = addRenderableWidget(new PatrollerList());
		for (PatrollerListPayload.Entry entry : data.entries()) {
			list.add(entry);
		}

		int buttonWidth = 100;
		int x = (width - buttonWidth * 2 - GAP) / 2;
		int y = height - FOOTER_HEIGHT / 2 - BUTTON_HEIGHT / 2;
		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.patrol_list.refresh"),
				b -> send(PatrollerListActionPayload.Action.REFRESH, ""))
			.bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
			.bounds(x + buttonWidth + GAP, y, buttonWidth, BUTTON_HEIGHT).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 10, TEXT_COLOR);
		if (data.entries().isEmpty()) {
			graphics.drawCenteredString(font, Component.translatable("screen.bcya.patrol_list.empty"),
				width / 2, height / 2 - 20, SUB_COLOR);
		}
	}

	private static void send(PatrollerListActionPayload.Action action, String name) {
		ClientPlayNetworking.send(new PatrollerListActionPayload(action, name));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private class PatrollerList extends ContainerObjectSelectionList<Row> {

		PatrollerList() {
			super(Minecraft.getInstance(), PatrollerListScreen.this.width,
				PatrollerListScreen.this.height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP, ROW_HEIGHT);
		}

		void add(PatrollerListPayload.Entry entry) {
			addEntry(new Row(entry));
		}

		@Override
		public int getRowWidth() {
			return Math.min(340, PatrollerListScreen.this.width - 40);
		}

		@Override
		protected int getScrollbarPosition() {
			return (PatrollerListScreen.this.width + getRowWidth()) / 2 + 6;
		}
	}

	private class Row extends ContainerObjectSelectionList.Entry<Row> {

		private static final int ACTION_WIDTH = 48;

		private final PatrollerListPayload.Entry entry;
		private final Button edit;
		private final Button teleport;
		private final Button remove;
		private final List<Button> buttons;

		Row(PatrollerListPayload.Entry entry) {
			this.entry = entry;
			edit = Button.builder(Component.translatable("screen.bcya.patrol_list.edit"),
				b -> send(PatrollerListActionPayload.Action.EDIT, entry.name())).size(ACTION_WIDTH, BUTTON_HEIGHT).build();
			teleport = Button.builder(Component.translatable("screen.bcya.patrol_list.teleport"),
				b -> send(PatrollerListActionPayload.Action.TELEPORT, entry.name())).size(ACTION_WIDTH, BUTTON_HEIGHT).build();
			remove = Button.builder(Component.translatable("screen.bcya.patrol_list.remove"),
				b -> send(PatrollerListActionPayload.Action.REMOVE, entry.name())).size(ACTION_WIDTH, BUTTON_HEIGHT).build();
			buttons = List.of(edit, teleport, remove);
		}

		@Override
		public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
				int mouseX, int mouseY, boolean hovering, float partialTick) {
			int buttonY = top + (rowHeight - BUTTON_HEIGHT) / 2;
			int right = left + rowWidth;
			remove.setPosition(right - ACTION_WIDTH, buttonY);
			teleport.setPosition(right - ACTION_WIDTH * 2 - GAP, buttonY);
			edit.setPosition(right - ACTION_WIDTH * 3 - GAP * 2, buttonY);
			for (Button button : buttons) {
				button.render(graphics, mouseX, mouseY, partialTick);
			}

			graphics.drawString(font, Component.literal(entry.name()), left + 4, top + 6, TEXT_COLOR);
			Component detail = Component.translatable("screen.bcya.patrol_list.detail",
				entry.x(), entry.y(), entry.z(), entry.waypoints(),
				entry.skin().isEmpty() ? Component.translatable("screen.bcya.patroller.skin_none") : Component.literal(entry.skin()));
			graphics.drawString(font, detail, left + 4, top + 19, SUB_COLOR);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return buttons;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return buttons;
		}
	}
}
