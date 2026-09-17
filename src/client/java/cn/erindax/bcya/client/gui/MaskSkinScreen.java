package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.mask.MaskSkinOverride;
import cn.erindax.bcya.mask.net.MaskSkinEditorPayload;
import cn.erindax.bcya.mask.net.MaskSkinUpdatePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class MaskSkinScreen extends Screen {

	private static final int ROW_HEIGHT = 26;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 40;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int ROW_WIDTH = 360;
	private static final int SKIN_WIDTH = 130;
	private static final int ARMS_WIDTH = 70;

	private final List<String> skins;
	private final Map<ResourceLocation, MaskSkinOverride> current = new HashMap<>();
	private final List<Row> rows = new ArrayList<>();

	public MaskSkinScreen(MaskSkinEditorPayload data) {
		super(Component.translatable("screen.bcya.mask_skins.title"));
		List<String> list = new ArrayList<>();
		list.add("");
		list.addAll(data.skins());
		for (MaskSkinOverride override : data.overrides()) {
			current.put(override.mask(), override);
			if (!list.contains(override.skin())) {
				list.add(override.skin());
			}
		}
		skins = list;
	}

	@Override
	protected void init() {
		rows.clear();
		MaskList list = addRenderableWidget(new MaskList());
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
		List<MaskSkinOverride> overrides = new ArrayList<>();
		for (Row row : rows) {
			String skin = row.skinButton.getValue();
			if (!skin.isEmpty()) {
				overrides.add(new MaskSkinOverride(BuiltInRegistries.ITEM.getKey(row.mask), skin, row.armsButton.getValue()));
			}
		}
		ClientPlayNetworking.send(new MaskSkinUpdatePayload(overrides));
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int rowWidth() {
		return Math.min(ROW_WIDTH, width - 40);
	}

	private class MaskList extends ContainerObjectSelectionList<Row> {

		MaskList() {
			super(Minecraft.getInstance(), MaskSkinScreen.this.width,
				MaskSkinScreen.this.height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP, ROW_HEIGHT);
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
			return (MaskSkinScreen.this.width + rowWidth()) / 2 + 6;
		}
	}

	private class Row extends ContainerObjectSelectionList.Entry<Row> {

		private final MaskItem mask;
		private final Component label;
		private final CycleButton<String> skinButton;
		private final CycleButton<Boolean> armsButton;
		private final List<GuiEventListener> children;

		Row(MaskItem mask) {
			this.mask = mask;
			this.label = new ItemStack(mask).getHoverName();
			MaskSkinOverride override = current.get(BuiltInRegistries.ITEM.getKey(mask));
			skinButton = CycleButton.<String>builder(s -> s.isEmpty()
					? Component.translatable("screen.bcya.mask_skins.builtin") : Component.literal(s))
				.withValues(skins)
				.withInitialValue(override == null ? "" : override.skin())
				.displayOnlyValue()
				.create(0, 0, SKIN_WIDTH, BUTTON_HEIGHT, Component.empty(), (b, v) -> {});
			armsButton = CycleButton.<Boolean>builder(slim -> Component.translatable(
					slim ? "screen.bcya.patroller.arms_slim" : "screen.bcya.patroller.arms_wide"))
				.withValues(List.of(false, true))
				.withInitialValue(override == null ? mask.isSlimModel() : override.slim())
				.displayOnlyValue()
				.create(0, 0, ARMS_WIDTH, BUTTON_HEIGHT, Component.empty(), (b, v) -> {});
			children = List.of(skinButton, armsButton);
		}

		@Override
		public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
				int mouseX, int mouseY, boolean hovering, float partialTick) {
			int buttonY = top + (rowHeight - BUTTON_HEIGHT) / 2;
			int right = left + rowWidth;
			armsButton.setPosition(right - ARMS_WIDTH, buttonY);
			skinButton.setPosition(right - ARMS_WIDTH - GAP - SKIN_WIDTH, buttonY);
			skinButton.render(graphics, mouseX, mouseY, partialTick);
			armsButton.render(graphics, mouseX, mouseY, partialTick);
			graphics.drawString(font, label, left + 4, top + (rowHeight - 8) / 2, 0xFFFFFF);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return children;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(skinButton, armsButton);
		}
	}
}
