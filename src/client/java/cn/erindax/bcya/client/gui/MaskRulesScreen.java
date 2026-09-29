package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.mask.net.MaskRulesPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class MaskRulesScreen extends Screen {

	private static final int ROW_HEIGHT = 26;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 40;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int ROW_WIDTH = 340;
	private static final int TOGGLE_WIDTH = 90;

	private final Set<ResourceLocation> voiceDisabled;
	private final Set<ResourceLocation> swapBlacklist;
	private final List<Row> rows = new ArrayList<>();

	public MaskRulesScreen(MaskRulesPayload data) {
		super(Component.translatable("screen.bcya.mask_rules.title"));
		this.voiceDisabled = Set.copyOf(data.voiceDisabled());
		this.swapBlacklist = Set.copyOf(data.swapBlacklist());
	}

	@Override
	protected void init() {
		rows.clear();
		RuleList list = addRenderableWidget(new RuleList());
		for (MaskItem mask : ModItems.MASKS) {
			Row row = new Row(mask);
			rows.add(row);
			list.add(row);
		}

		int buttonWidth = 100;
		int x = (width - buttonWidth * 2 - GAP) / 2;
		int y = height - FOOTER_HEIGHT / 2 - BUTTON_HEIGHT / 2;
		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.patroller.save"), b -> save())
			.bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
			.bounds(x + buttonWidth + GAP, y, buttonWidth, BUTTON_HEIGHT).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
	}

	private void save() {
		List<ResourceLocation> voice = new ArrayList<>();
		List<ResourceLocation> swap = new ArrayList<>();
		for (Row row : rows) {
			if (!row.voiceButton.getValue()) {
				voice.add(row.id);
			}
			if (!row.swapButton.getValue()) {
				swap.add(row.id);
			}
		}
		ClientPlayNetworking.send(new MaskRulesPayload(voice, swap));
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int rowWidth() {
		return Math.min(ROW_WIDTH, width - 40);
	}

	private class RuleList extends ContainerObjectSelectionList<Row> {

		RuleList() {
			super(Minecraft.getInstance(), MaskRulesScreen.this.width,
				MaskRulesScreen.this.height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP, ROW_HEIGHT);
		}

		void add(Row row) {
			addEntry(row);
		}

		@Override
		public int getRowWidth() {
			return rowWidth();
		}

		@Override
		protected int getScrollbarPosition() {
			return (MaskRulesScreen.this.width + rowWidth()) / 2 + 6;
		}
	}

	private class Row extends ContainerObjectSelectionList.Entry<Row> {

		private final ResourceLocation id;
		private final Component label;
		private final CycleButton<Boolean> voiceButton;
		private final CycleButton<Boolean> swapButton;
		private final List<GuiEventListener> children;

		Row(MaskItem mask) {
			this.id = BuiltInRegistries.ITEM.getKey(mask);
			this.label = new ItemStack(mask).getHoverName();
			voiceButton = CycleButton.onOffBuilder(!voiceDisabled.contains(id))
				.create(0, 0, TOGGLE_WIDTH, BUTTON_HEIGHT, Component.translatable("screen.bcya.mask_rules.voice"));
			swapButton = CycleButton.onOffBuilder(!swapBlacklist.contains(id))
				.create(0, 0, TOGGLE_WIDTH, BUTTON_HEIGHT, Component.translatable("screen.bcya.mask_rules.swap"));
			children = List.of(voiceButton, swapButton);
		}

		@Override
		public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
				int mouseX, int mouseY, boolean hovering, float partialTick) {
			int buttonY = top + (rowHeight - BUTTON_HEIGHT) / 2;
			int right = left + rowWidth;
			swapButton.setPosition(right - TOGGLE_WIDTH, buttonY);
			voiceButton.setPosition(right - TOGGLE_WIDTH - GAP - TOGGLE_WIDTH, buttonY);
			voiceButton.render(graphics, mouseX, mouseY, partialTick);
			swapButton.render(graphics, mouseX, mouseY, partialTick);
			graphics.drawString(font, label, left + 4, top + (rowHeight - 8) / 2, 0xFFFFFF);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return children;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(voiceButton, swapButton);
		}
	}
}
