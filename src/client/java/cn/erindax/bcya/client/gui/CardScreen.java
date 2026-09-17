package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.card.CardData;
import cn.erindax.bcya.card.net.SaveCardPayload;
import cn.erindax.bcya.client.render.CardSkinRenderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CardScreen extends Screen {

	private static final String P = "screen.bcya.card.";

	private final CompoundTag card;
	private final boolean readOnly;
	private final boolean mainHand;
	private final UUID ownerId;
	private final String ownerName;
	private final String skinValue;
	private final String skinSig;

	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int skinX;
	private int skinY;
	private int skinW;
	private int skinH;
	private int formX0;
	private int formY0;
	private int formX1;
	private int formY1;
	private int contentHeight;
	private int scroll;

	private final List<AbstractWidget> formWidgets = new ArrayList<>();
	private final List<Integer> formBaseY = new ArrayList<>();
	private final List<Line> lines = new ArrayList<>();
	private final List<AbstractWidget> fixedWidgets = new ArrayList<>();

	private EditBox investigatorBox;
	private Button awakenButton;
	private Button doneButton;

	public CardScreen(CompoundTag card, boolean readOnly, boolean mainHand,
			UUID ownerId, String ownerName, String skinValue, String skinSig) {
		super(Component.translatable(P + "title"));
		this.card = CardData.normalize(card);
		this.readOnly = readOnly;
		this.mainHand = mainHand;
		this.ownerId = ownerId;
		this.ownerName = ownerName == null ? "" : ownerName;
		this.skinValue = skinValue == null ? "" : skinValue;
		this.skinSig = skinSig == null ? "" : skinSig;
	}

	@Override
	protected void init() {
		super.init();
		formWidgets.clear();
		formBaseY.clear();
		lines.clear();
		fixedWidgets.clear();
		scroll = 0;

		panelW = Math.min(width - 16, 440);
		panelH = Math.min(height - 16, 260);
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;

		skinX = panelX + 10;
		skinY = panelY + 22;
		skinW = 60;
		skinH = 74;
		formX0 = panelX + 10;
		formX1 = panelX + panelW - 10;
		formY0 = panelY + 104;
		formY1 = panelY + panelH - 32;

		int cursor = formY0 + 4;
		int labelW = 92;
		int fieldX = formX0 + labelW;
		int fieldW = Math.min(220, formX1 - fieldX - 4);

		line(formX0, cursor + 5, 0xFFE0E0E0, () -> Component.translatable(P + "investigator"));
		investigatorBox = new EditBox(font, fieldX, cursor, fieldW, 18, Component.empty());
		investigatorBox.setMaxLength(48);
		investigatorBox.setValue(card.getString(CardData.INVESTIGATOR));
		investigatorBox.setEditable(!readOnly);
		investigatorBox.setResponder(value -> {
			card.putString(CardData.INVESTIGATOR, value);
			updateDoneState();
		});
		addForm(investigatorBox, cursor);
		cursor += 24;

		line(formX0, cursor + 5, 0xFFE0E0E0, () -> Component.translatable(P + "emotion"));
		EditBox emotionBox = new EditBox(font, fieldX, cursor, 60, 18, Component.empty());
		emotionBox.setMaxLength(3);
		emotionBox.setFilter(value -> value.isEmpty()
			|| (value.matches("\\d{0,3}") && Integer.parseInt(value) <= CardData.EMOTION_MAX));
		emotionBox.setValue(String.valueOf(CardData.emotion(card)));
		emotionBox.setEditable(!readOnly);
		emotionBox.setResponder(value -> {
			int emotion = value.isEmpty() ? 0 : Mth.clamp(Integer.parseInt(value), 0, CardData.EMOTION_MAX);
			card.putInt(CardData.EMOTION, emotion);
		});
		addForm(emotionBox, cursor);
		cursor += 24;

		line(formX0, cursor + 5, 0xFFE0E0E0, () -> Component.translatable(P + "emotion_deep"));
		EditBox deepBox = new EditBox(font, fieldX, cursor, fieldW, 18, Component.empty());
		deepBox.setMaxLength(64);
		deepBox.setValue(card.getString(CardData.EMOTION_DEEP));
		deepBox.setEditable(!readOnly);
		deepBox.setResponder(value -> card.putString(CardData.EMOTION_DEEP, value));
		addForm(deepBox, cursor);
		cursor += 26;

		line(formX0, cursor + 3, 0xFFBA68C8, () -> Component.translatable(P + "awakened"));
		awakenButton = Button.builder(awakenLabel(), button -> {
			card.putBoolean(CardData.AWAKENED, !card.getBoolean(CardData.AWAKENED));
			button.setMessage(awakenLabel());
		}).pos(formX0 + labelW, cursor).size(70, 18).build();
		awakenButton.active = !readOnly;
		addForm(awakenButton, cursor);
		if (!readOnly) {
			Button check = Button.builder(Component.translatable(P + "awaken_check"), button -> {
				boolean ok = CardData.tryAwaken(card);
				awakenButton.setMessage(awakenLabel());
				if (minecraft != null && minecraft.player != null) {
					minecraft.player.displayClientMessage(
						Component.translatable(P + (ok ? "awaken_success" : "awaken_fail")), true);
				}
			}).pos(formX0 + labelW + 76, cursor).width(92).build();
			addForm(check, cursor);
		}
		cursor += 22;

		line(formX0, cursor + 5, 0xFFE0E0E0, () -> Component.translatable(P + "ability_name"));
		EditBox abilityBox = new EditBox(font, fieldX, cursor, fieldW, 18, Component.empty());
		abilityBox.setMaxLength(48);
		abilityBox.setValue(card.getString(CardData.ABILITY_NAME));
		abilityBox.setEditable(!readOnly);
		abilityBox.setResponder(value -> card.putString(CardData.ABILITY_NAME, value));
		addForm(abilityBox, cursor);
		cursor += 28;

		line(formX0, cursor + 2, 0xFFFFD700,
			() -> Component.translatable(P + "abilities", CardData.abilitySpent(card)));
		cursor += 16;
		for (String name : CardData.ABILITY_NAMES) {
			line(formX0 + 4, cursor + 4, 0xFFFFFFFF, () -> Component.literal(name));
			line(formX0 + 66, cursor + 4, 0xFFFFD700,
				() -> Component.literal(String.valueOf(CardData.ability(card, name))));
			if (!readOnly) {
				addForm(smallButton(formX0 + 42, cursor, "-", () -> decAbility(name)), cursor);
				addForm(smallButton(formX0 + 86, cursor, "+", () -> incAbility(name)), cursor);
			}
			cursor += 18;
		}
		cursor += 6;

		line(formX0, cursor + 2, 0xFFFFD700,
			() -> Component.translatable(P + "skills", CardData.skillSpent(card)));
		cursor += 16;
		for (Map.Entry<String, List<String>> entry : CardData.SKILL_CATEGORIES.entrySet()) {
			line(formX0, cursor + 2, 0xFF9ECBFF, () -> Component.literal(entry.getKey()));
			cursor += 14;
			for (String name : entry.getValue()) {
				boolean base = CardData.BASE_SKILLS.contains(name);
				line(formX0 + 10, cursor + 4, 0xFFDDDDDD,
					() -> base ? Component.translatable(P + "skill_base", name) : Component.literal(name));
				line(formX0 + 152, cursor + 4, 0xFFFFD700,
					() -> Component.literal(String.valueOf(CardData.skill(card, name))));
				if (!readOnly) {
					addForm(smallButton(formX0 + 128, cursor, "-", () -> decSkill(name)), cursor);
					addForm(smallButton(formX0 + 172, cursor, "+", () -> incSkill(name)), cursor);
				}
				cursor += 18;
			}
			cursor += 4;
		}
		contentHeight = cursor - formY0 + 6;

		int footerY = panelY + panelH - 26;
		Button close = Button.builder(Component.translatable(P + "close"), button -> onClose())
			.pos(panelX + panelW - 90, footerY).width(80).build();
		fixedWidgets.add(close);
		addRenderableWidget(close);
		if (!readOnly) {
			doneButton = Button.builder(Component.translatable(P + "done"), button -> onDone())
				.pos(panelX + panelW - 180, footerY).width(80).build();
			fixedWidgets.add(doneButton);
			addRenderableWidget(doneButton);
			updateDoneState();
		}

		clampScroll();
	}

	private Button smallButton(int x, int y, String text, Runnable action) {
		return Button.builder(Component.literal(text), button -> action.run()).pos(x, y).size(16, 16).build();
	}

	private Component awakenLabel() {
		return Component.translatable(P + (card.getBoolean(CardData.AWAKENED) ? "awakened_yes" : "awakened_no"));
	}

	private void line(int x, int baseY, int color, Supplier<Component> text) {
		lines.add(new Line(x, baseY, color, text));
	}

	private void addForm(AbstractWidget widget, int baseY) {
		widget.setY(baseY + scroll);
		formWidgets.add(widget);
		formBaseY.add(baseY);
		addRenderableWidget(widget);
	}

	private void updateDoneState() {
		if (doneButton != null) {
			doneButton.active = CardData.requiredComplete(card);
		}
	}

	private void incAbility(String name) {
		int value = CardData.ability(card, name);
		if (value >= CardData.ABILITY_MAX || CardData.abilitySpent(card) >= CardData.ABILITY_POINTS_TOTAL) {
			return;
		}
		CardData.setAbility(card, name, value + 1);
	}

	private void decAbility(String name) {
		CardData.setAbility(card, name, CardData.ability(card, name) - 1);
	}

	private void incSkill(String name) {
		if (CardData.skillSpent(card) >= CardData.SKILL_POINTS_TOTAL) {
			return;
		}
		CardData.setSkill(card, name, CardData.skill(card, name) + 1);
	}

	private void decSkill(String name) {
		CardData.setSkill(card, name, CardData.skill(card, name) - 1);
	}

	private void onDone() {
		card.putString(CardData.INVESTIGATOR, investigatorBox.getValue().trim());
		if (!CardData.requiredComplete(card)) {
			return;
		}
		ClientPlayNetworking.send(new SaveCardPayload(card, mainHand));
		if (doneButton != null) {
			doneButton.active = false;
		}
	}

	public void onSaveFailed() {
		if (doneButton != null) {
			doneButton.active = CardData.requiredComplete(card);
		}
	}

	private void clampScroll() {
		int viewH = formY1 - formY0;
		int min = Math.min(0, viewH - contentHeight);
		scroll = Mth.clamp(scroll, min, 0);
		for (int i = 0; i < formWidgets.size(); i++) {
			formWidgets.get(i).setY(formBaseY.get(i) + scroll);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (mouseX >= formX0 && mouseX <= formX1 && mouseY >= formY0 && mouseY <= formY1
			&& contentHeight > formY1 - formY0) {
			scroll += (int) (vertical * 18);
			clampScroll();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE6101010);
		graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF808080);
		graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 8, 0xFFFFFFFF);

		CardSkinRenderer.render(graphics, skinX, skinY, skinX + skinW, skinY + skinH,
			ownerId, ownerName, skinValue, skinSig);
		graphics.renderOutline(skinX - 1, skinY - 1, skinW + 2, skinH + 2, 0xFF404040);

		int infoX = skinX + skinW + 12;
		int infoY = panelY + 24;
		graphics.drawString(font, Component.translatable(P + "hp", CardData.hp(card)), infoX, infoY, 0xFFEF5350, false);
		graphics.drawString(font, Component.translatable(P + "mp", CardData.mp(card)), infoX, infoY + 12, 0xFF42A5F5, false);
		graphics.drawString(font, Component.translatable(P + "ability_points",
			CardData.abilitySpent(card), CardData.ABILITY_POINTS_TOTAL - CardData.abilitySpent(card)),
			infoX, infoY + 24, 0xFFFFD700, false);
		graphics.drawString(font, Component.translatable(P + "skill_points",
			CardData.skillSpent(card), CardData.SKILL_POINTS_TOTAL - CardData.skillSpent(card)),
			infoX, infoY + 36, 0xFFFFD700, false);
		if (card.getBoolean(CardData.AWAKENED) && !card.getString(CardData.ABILITY_NAME).isBlank()) {
			graphics.drawString(font, Component.translatable(P + "awakened_show", card.getString(CardData.ABILITY_NAME)),
				infoX, infoY + 50, 0xFFBA68C8, false);
		}
		if (readOnly) {
			graphics.drawString(font, Component.translatable(P + "readonly"), infoX, infoY + 62, 0xFFAAAAAA, false);
		}

		graphics.fill(formX0, formY0 - 3, formX1, formY0 - 2, 0xFF444444);
		graphics.enableScissor(formX0, formY0, formX1, formY1);
		for (Line line : lines) {
			int y = line.baseY + scroll;
			if (y + 10 < formY0 || y > formY1) {
				continue;
			}
			graphics.drawString(font, line.text.get(), line.x, y, line.color, false);
		}
		for (AbstractWidget widget : formWidgets) {
			if (widget.getY() + widget.getHeight() < formY0 || widget.getY() > formY1) {
				continue;
			}
			widget.render(graphics, mouseX, mouseY, partialTick);
		}
		graphics.disableScissor();
		drawScrollbar(graphics);
		for (AbstractWidget widget : fixedWidgets) {
			widget.render(graphics, mouseX, mouseY, partialTick);
		}
	}

	private void drawScrollbar(GuiGraphics graphics) {
		int viewH = formY1 - formY0;
		if (contentHeight <= viewH) {
			return;
		}
		int barX = formX1 + 2;
		int thumbH = Math.max(16, viewH * viewH / contentHeight);
		int maxScroll = contentHeight - viewH;
		int thumbY = formY0 + (int) ((float) (-scroll) / maxScroll * (viewH - thumbH));
		graphics.fill(barX, formY0, barX + 3, formY1, 0xFF202020);
		graphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFFAAAAAA);
	}

	private record Line(int x, int baseY, int color, Supplier<Component> text) {
	}
}
