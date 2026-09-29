package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.mask.MaskRulesState;
import cn.erindax.bcya.voice.VoicePreset;
import cn.erindax.bcya.voice.VoicePresetsPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class VoicePresetScreen extends Screen {

	private static final int PANEL_WIDTH = 320;
	private static final int ROW = 24;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int ARROW_WIDTH = 20;
	private static final int LABEL_WIDTH = 40;
	private static final int TOP = 30;
	private static final int FOOTER_HEIGHT = 40;
	private static final int MUTED = 0xA0A0A0;

	private final List<Draft> drafts = new ArrayList<>();
	private int selected;
	private int left;
	private int nameY;
	private int hintY;

	private static final class Draft {
		private final String id;
		private String name;
		private float pitch;
		private float robot;
		private float radio;
		private float grit;
		private float echo;
		private float volume;

		private Draft(VoicePreset preset) {
			this.id = preset.id();
			this.name = preset.name();
			this.pitch = preset.pitch();
			this.robot = preset.robot();
			this.radio = preset.radio();
			this.grit = preset.grit();
			this.echo = preset.echo();
			this.volume = preset.volume();
		}

		private VoicePreset toPreset() {
			return new VoicePreset(id, name, pitch, robot, radio, grit, echo, volume);
		}
	}

	public VoicePresetScreen(VoicePresetsPayload data) {
		super(Component.translatable("screen.bcya.voice.title"));
		for (VoicePreset preset : data.presets()) {
			drafts.add(new Draft(preset));
		}
	}

	@Override
	protected void init() {
		int panel = Math.min(PANEL_WIDTH, width - 20);
		int half = (panel - GAP) / 2;
		left = (width - panel) / 2;
		int y = TOP;
		if (!drafts.isEmpty()) {
			selected = Mth.clamp(selected, 0, drafts.size() - 1);
			addRenderableWidget(Button.builder(Component.literal("<"), b -> select(selected - 1))
				.bounds(left, y, ARROW_WIDTH, BUTTON_HEIGHT).build()).active = drafts.size() > 1;
			addRenderableWidget(Button.builder(Component.literal(">"), b -> select(selected + 1))
				.bounds(left + panel - ARROW_WIDTH, y, ARROW_WIDTH, BUTTON_HEIGHT).build()).active = drafts.size() > 1;
		}
		y += ROW;
		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.voice.new"), b -> create())
			.bounds(left, y, half, BUTTON_HEIGHT).build()).active = drafts.size() < MaskRulesState.MAX_PRESETS;
		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.voice.delete"), b -> delete())
			.bounds(left + half + GAP, y, half, BUTTON_HEIGHT).build()).active = !drafts.isEmpty();
		y += ROW;
		nameY = y;
		if (!drafts.isEmpty()) {
			Draft draft = drafts.get(selected);
			EditBox name = new EditBox(font, left + LABEL_WIDTH, y, panel - LABEL_WIDTH, BUTTON_HEIGHT,
				Component.translatable("screen.bcya.voice.name"));
			name.setMaxLength(VoicePreset.MAX_NAME);
			name.setValue(draft.name);
			name.setResponder(value -> draft.name = value);
			addRenderableWidget(name);
			y += ROW;
			int right = left + half + GAP;
			addRenderableWidget(new ParamSlider(left, y, half, VoicePreset.MIN_PITCH, VoicePreset.MAX_PITCH, 0.05F,
				draft.pitch, value -> draft.pitch = value,
				value -> Component.translatable("screen.bcya.voice.pitch", String.format(Locale.ROOT, "%.2f", value))));
			addRenderableWidget(new ParamSlider(right, y, half, VoicePreset.MIN_VOLUME, VoicePreset.MAX_VOLUME, 0.05F,
				draft.volume, value -> draft.volume = value,
				value -> Component.translatable("screen.bcya.voice.volume", Math.round(value * 100.0F))));
			y += ROW;
			addRenderableWidget(new ParamSlider(left, y, half, 0.0F, 1.0F, 0.05F, draft.robot,
				value -> draft.robot = value, value -> percent("screen.bcya.voice.robot", value)));
			addRenderableWidget(new ParamSlider(right, y, half, 0.0F, 1.0F, 0.05F, draft.radio,
				value -> draft.radio = value, value -> percent("screen.bcya.voice.radio", value)));
			y += ROW;
			addRenderableWidget(new ParamSlider(left, y, half, 0.0F, 1.0F, 0.05F, draft.grit,
				value -> draft.grit = value, value -> percent("screen.bcya.voice.grit", value)));
			addRenderableWidget(new ParamSlider(right, y, half, 0.0F, 1.0F, 0.05F, draft.echo,
				value -> draft.echo = value, value -> percent("screen.bcya.voice.echo", value)));
			y += ROW;
		}
		hintY = y + 4;

		int buttonWidth = 100;
		int x = (width - buttonWidth * 2 - GAP) / 2;
		int footer = height - FOOTER_HEIGHT / 2 - BUTTON_HEIGHT / 2;
		addRenderableWidget(Button.builder(Component.translatable("screen.bcya.patroller.save"), b -> save())
			.bounds(x, footer, buttonWidth, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
			.bounds(x + buttonWidth + GAP, footer, buttonWidth, BUTTON_HEIGHT).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
		if (drafts.isEmpty()) {
			graphics.drawCenteredString(font, Component.translatable("screen.bcya.voice.empty"), width / 2, nameY + 6,
				MUTED);
			return;
		}
		Draft draft = drafts.get(selected);
		String shown = draft.name.isBlank() ? "-" : draft.name;
		graphics.drawCenteredString(font, Component.translatable("screen.bcya.voice.preset", shown, selected + 1,
			drafts.size()), width / 2, TOP + 6, 0xFFFFFF);
		graphics.drawString(font, Component.translatable("screen.bcya.voice.name"), left, nameY + 6, 0xFFFFFF);
		graphics.drawCenteredString(font, Component.translatable("screen.bcya.voice.hint"), width / 2, hintY, MUTED);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static Component percent(String key, float value) {
		return Component.translatable(key, Math.round(value * 100.0F));
	}

	private void select(int index) {
		if (drafts.isEmpty()) {
			return;
		}
		selected = Math.floorMod(index, drafts.size());
		rebuildWidgets();
	}

	private void create() {
		if (drafts.size() >= MaskRulesState.MAX_PRESETS) {
			return;
		}
		String name = Component.translatable("screen.bcya.voice.new_name").getString() + " " + (drafts.size() + 1);
		drafts.add(new Draft(VoicePreset.blank(name)));
		selected = drafts.size() - 1;
		rebuildWidgets();
	}

	private void delete() {
		if (drafts.isEmpty()) {
			return;
		}
		drafts.remove(selected);
		selected = Math.max(0, Math.min(selected, drafts.size() - 1));
		rebuildWidgets();
	}

	private void save() {
		List<VoicePreset> presets = new ArrayList<>();
		for (Draft draft : drafts) {
			presets.add(draft.toPreset());
		}
		ClientPlayNetworking.send(new VoicePresetsPayload(presets));
		onClose();
	}

	private static final class ParamSlider extends AbstractSliderButton {

		private final float min;
		private final float max;
		private final float step;
		private final Consumer<Float> setter;
		private final Function<Float, Component> label;

		private ParamSlider(int x, int y, int width, float min, float max, float step, float current,
				Consumer<Float> setter, Function<Float, Component> label) {
			super(x, y, width, BUTTON_HEIGHT, Component.empty(), Mth.clamp((current - min) / (max - min), 0.0F, 1.0F));
			this.min = min;
			this.max = max;
			this.step = step;
			this.setter = setter;
			this.label = label;
			updateMessage();
		}

		private float current() {
			float raw = min + (float) value * (max - min);
			return Mth.clamp(Math.round(raw / step) * step, min, max);
		}

		@Override
		protected void updateMessage() {
			if (label != null) {
				setMessage(label.apply(current()));
			}
		}

		@Override
		protected void applyValue() {
			setter.accept(current());
		}
	}
}
