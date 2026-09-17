package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.entity.net.PatrollerListActionPayload;
import cn.erindax.bcya.entity.net.PatrollerSettingsPayload;
import cn.erindax.bcya.entity.net.PatrollerUpdatePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PatrollerSettingsScreen extends Screen {

	private record Label(Component text, int x, int y) {
	}

	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int LABEL_HEIGHT = 10;
	private static final int HEADER_HEIGHT = 26;
	private static final int LABEL_COLOR = 0xA0A0A0;

	private final PatrollerSettingsPayload data;
	private final List<Label> labels = new ArrayList<>();

	private EditBox nameBox;
	private CycleButton<String> skinButton;
	private CycleButton<Boolean> slimButton;
	private EditBox pauseBox;
	private EditBox detectBox;
	private EditBox stareBox;
	private CycleButton<Boolean> nameTagButton;
	private CycleButton<Boolean> heldItemsButton;
	private CycleButton<Boolean> patrollingButton;
	private CycleButton<Boolean> aggressiveButton;
	private int top;

	public PatrollerSettingsScreen(PatrollerSettingsPayload data) {
		super(Component.translatable("screen.bcya.patroller.title"));
		this.data = data;
	}

	@Override
	protected void init() {
		labels.clear();
		int contentHeight = HEADER_HEIGHT
			+ (LABEL_HEIGHT + FIELD_HEIGHT + GAP)
			+ (FIELD_HEIGHT + GAP) * 2
			+ (LABEL_HEIGHT + FIELD_HEIGHT + GAP)
			+ (FIELD_HEIGHT + GAP) * 2
			+ GAP
			+ (FIELD_HEIGHT + GAP) * 2;
		top = Math.max(8, (height - contentHeight) / 2);
		int x = (width - FIELD_WIDTH) / 2;
		int half = (FIELD_WIDTH - GAP) / 2;
		int y = top + HEADER_HEIGHT;

		labels.add(new Label(Component.translatable("screen.bcya.patroller.name"), x, y));
		y += LABEL_HEIGHT;
		nameBox = new EditBox(font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.translatable("screen.bcya.patroller.name"));
		nameBox.setMaxLength(32);
		nameBox.setValue(data.name());
		addRenderableWidget(nameBox);
		y += FIELD_HEIGHT + GAP;

		List<String> skins = new ArrayList<>();
		skins.add("");
		skins.addAll(data.skins());
		if (!data.skin().isEmpty() && !skins.contains(data.skin())) {
			skins.add(data.skin());
		}
		skinButton = addRenderableWidget(CycleButton.<String>builder(s ->
				s.isEmpty() ? Component.translatable("screen.bcya.patroller.skin_none") : Component.literal(s))
			.withValues(skins)
			.withInitialValue(data.skin())
			.create(x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.translatable("screen.bcya.patroller.skin"), (b, v) -> {}));
		y += FIELD_HEIGHT + GAP;

		slimButton = addRenderableWidget(CycleButton.<Boolean>builder(slim -> Component.translatable(
				slim ? "screen.bcya.patroller.arms_slim" : "screen.bcya.patroller.arms_wide"))
			.withValues(List.of(false, true))
			.withInitialValue(data.slim())
			.create(x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.translatable("screen.bcya.patroller.arms"), (b, v) -> {}));
		y += FIELD_HEIGHT + GAP;

		int third = (FIELD_WIDTH - GAP * 2) / 3;
		labels.add(new Label(Component.translatable("screen.bcya.patroller.pause"), x, y));
		labels.add(new Label(Component.translatable("screen.bcya.patroller.detect"), x + third + GAP, y));
		labels.add(new Label(Component.translatable("screen.bcya.patroller.stare"), x + (third + GAP) * 2, y));
		y += LABEL_HEIGHT;
		pauseBox = numberBox(x, y, third, data.pauseSeconds(), "screen.bcya.patroller.pause");
		detectBox = numberBox(x + third + GAP, y, third, data.detectDiameter(), "screen.bcya.patroller.detect");
		stareBox = numberBox(x + (third + GAP) * 2, y, third, data.stareSeconds(), "screen.bcya.patroller.stare");
		y += FIELD_HEIGHT + GAP;

		nameTagButton = addRenderableWidget(CycleButton.onOffBuilder(data.nameVisible())
			.create(x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.translatable("screen.bcya.patroller.nametag"), (b, v) -> {}));
		y += FIELD_HEIGHT + GAP;

		heldItemsButton = addRenderableWidget(CycleButton.<Boolean>builder(show -> Component.translatable(
				show ? "screen.bcya.patroller.held_shown" : "screen.bcya.patroller.held_hidden"))
			.withValues(List.of(true, false))
			.withInitialValue(data.showHeldItems())
			.create(x, y, third, FIELD_HEIGHT, Component.translatable("screen.bcya.patroller.held"), (b, v) -> {}));
		patrollingButton = addRenderableWidget(CycleButton.<Boolean>builder(on -> Component.translatable(
				on ? "screen.bcya.patroller.patrol_on" : "screen.bcya.patroller.patrol_off"))
			.withValues(List.of(true, false))
			.withInitialValue(data.patrolling())
			.create(x + third + GAP, y, third, FIELD_HEIGHT, Component.translatable("screen.bcya.patroller.patrol"), (b, v) -> {}));
		aggressiveButton = addRenderableWidget(CycleButton.onOffBuilder(data.aggressive())
			.create(x + (third + GAP) * 2, y, third, FIELD_HEIGHT, Component.translatable("screen.bcya.patroller.aggressive"), (b, v) -> {}));
		y += FIELD_HEIGHT + GAP + GAP;

		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.patroller.record"), b -> record())
			.bounds(x, y, half, FIELD_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.patroller.remove"), b -> remove())
			.bounds(x + half + GAP, y, half, FIELD_HEIGHT).build());
		y += FIELD_HEIGHT + GAP;

		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.patroller.save"), b -> save())
			.bounds(x, y, half, FIELD_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable(data.fromList() ? "gui.back" : "gui.cancel"), b -> cancel())
			.bounds(x + half + GAP, y, half, FIELD_HEIGHT).build());

		setInitialFocus(nameBox);
	}

	private EditBox numberBox(int x, int y, int width, int value, String key) {
		EditBox box = new EditBox(font, x, y, width, FIELD_HEIGHT, Component.translatable(key));
		box.setMaxLength(4);
		box.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
		box.setValue(Integer.toString(value));
		addRenderableWidget(box);
		return box;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, top, 0xFFFFFF);
		graphics.drawCenteredString(font, Component.translatable("screen.bcya.patroller.waypoints", data.waypointCount()),
			width / 2, top + 12, LABEL_COLOR);
		for (Label label : labels) {
			graphics.drawString(font, label.text(), label.x(), label.y(), LABEL_COLOR);
		}
	}

	private PatrollerUpdatePayload collect(boolean returnToList) {
		return new PatrollerUpdatePayload(data.name(), nameBox.getValue().trim(), skinButton.getValue(),
			slimButton.getValue(), parse(pauseBox, data.pauseSeconds()),
			parse(detectBox, data.detectDiameter()), parse(stareBox, data.stareSeconds()),
			nameTagButton.getValue(), heldItemsButton.getValue(), patrollingButton.getValue(),
			aggressiveButton.getValue(), returnToList);
	}

	private static int parse(EditBox box, int fallback) {
		try {
			return Integer.parseInt(box.getValue().trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private void save() {
		ClientPlayNetworking.send(collect(data.fromList()));
		onClose();
	}

	private void record() {
		ClientPlayNetworking.send(collect(false));
		ClientPlayNetworking.send(new PatrollerListActionPayload(PatrollerListActionPayload.Action.RECORD,
			nameBox.getValue().trim()));
		onClose();
	}

	private void remove() {
		ClientPlayNetworking.send(new PatrollerListActionPayload(PatrollerListActionPayload.Action.REMOVE, data.name()));
		if (!data.fromList()) {
			onClose();
		}
	}

	private void cancel() {
		if (data.fromList()) {
			ClientPlayNetworking.send(new PatrollerListActionPayload(PatrollerListActionPayload.Action.REFRESH, ""));
		} else {
			onClose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
