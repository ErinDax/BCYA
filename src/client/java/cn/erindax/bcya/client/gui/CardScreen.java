package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.card.CardData;
import cn.erindax.bcya.card.net.SaveCardPayload;
import cn.erindax.bcya.client.render.CardSkinRenderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CardScreen extends Screen {

	private static final String P = "screen.bcya.card.";
	private static final int PANEL = 0xD8F0F2F4;
	private static final int FORM = 0x55FFFFFF;
	private static final int FIELD = 0xFFE8EAEC;
	private static final int OUTLINE = 0xFFB4B8BC;
	private static final int HIGHLIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF8A9096;
	private static final int ACCENT = 0xFF166534;
	private static final int TEXT = 0xFF000000;
	private static final int AWAKE = 0xFF7E22CE;
	private static final int NAME = 0xFF000000;
	private static final int CATEGORY = 0xFF1E6E8A;
	private static final int BASE = 0xFF9A6A1A;
	private static final int VALUE = 0xFF000000;
	private static final int HP = 0xFFC45C5C;
	private static final int MP = 0xFF5C7CB4;
	private static final int ABILITY = 0xFFC49A54;
	private static final int SKILL = 0xFF6A9A64;
	private static final int TRACK = 0x66404040;
	private static final int BAR_TEXT = 0xFFFFFFFF;
	private static final int SCROLL_WELL = 0x66A0A4A8;
	private static final int SCROLL_END = 0xE6F0F2F4;
	private static final int SCROLL_THUMB = 0xE6D0D4D8;
	private static final int SCROLL_W = 8;
	private static final int SCROLL_CAP = 7;

	private final CompoundTag card;
	private final boolean readOnly;
	private final boolean mainHand;
	private final boolean operator;
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
	private boolean draggingScroll;

	private final List<AbstractWidget> formWidgets = new ArrayList<>();
	private final List<Integer> formBaseY = new ArrayList<>();
	private final List<Boolean> formProfile = new ArrayList<>();
	private final List<Line> lines = new ArrayList<>();
	private final List<AbstractWidget> fixedWidgets = new ArrayList<>();

	private EditBox investigatorBox;
	private Button awakenButton;
	private Button doneButton;

	public CardScreen(CompoundTag card, boolean readOnly, boolean mainHand, boolean operator,
			UUID ownerId, String ownerName, String skinValue, String skinSig) {
		super(Component.translatable(P + "title"));
		this.card = CardData.normalize(card);
		this.readOnly = readOnly;
		this.mainHand = mainHand;
		this.operator = operator;
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
		formProfile.clear();
		lines.clear();
		fixedWidgets.clear();
		draggingScroll = false;

		panelW = Math.min(width - 20, 540);
		panelH = Math.min(height - 18, 312);
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;

		int leftW = 128;
		skinX = panelX + 12;
		skinY = panelY + 28;
		skinW = 104;
		skinH = panelH - 148;
		formX0 = panelX + leftW + 8;
		formX1 = panelX + panelW - 14;
		formY0 = panelY + 28;
		formY1 = panelY + panelH - 34;

		int cursor = formY0 + 4;
		int textX0 = formX0 + 8;
		int labelW = 6 + Math.max(
			Math.max(font.width(Component.translatable(P + "investigator")), font.width(Component.translatable(P + "emotion"))),
			Math.max(font.width(Component.translatable(P + "emotion_deep")),
				Math.max(font.width(Component.translatable(P + "awakened")), font.width(Component.translatable(P + "ability_name")))));
		int fieldX = textX0 + labelW;
		int glyphW = Math.max(8, font.width("字"));
		int fieldW = glyphW * 12 + 8;
		int emotionW = font.width("000") + 8;
		int fieldH = 14;

		line(textX0, cursor + 5, TEXT, () -> Component.translatable(P + "investigator"));
		investigatorBox = field(fieldX, cursor, fieldW, fieldH);
		investigatorBox.setMaxLength(48);
		investigatorBox.setValue(card.getString(CardData.INVESTIGATOR));
		investigatorBox.setEditable(!readOnly);
		investigatorBox.setResponder(value -> {
			card.putString(CardData.INVESTIGATOR, value);
			updateDoneState();
		});
		addForm(investigatorBox, cursor);
		cursor += 22;

		line(textX0, cursor + 5, TEXT, () -> Component.translatable(P + "emotion"));
		EditBox emotionBox = field(fieldX, cursor, emotionW, fieldH);
		emotionBox.setMaxLength(3);
		emotionBox.setFilter(value -> value.isEmpty()
			|| (value.matches("\\d{0,3}") && Integer.parseInt(value) <= CardData.EMOTION_MAX));
		emotionBox.setValue(String.valueOf(CardData.emotion(card)));
		emotionBox.setEditable(operator);
		emotionBox.setResponder(value -> {
			int emotion = value.isEmpty() ? 0 : Mth.clamp(Integer.parseInt(value), 0, CardData.EMOTION_MAX);
			card.putInt(CardData.EMOTION, emotion);
		});
		addProfile(emotionBox, cursor);
		cursor += 22;

		line(textX0, cursor + 5, TEXT, () -> Component.translatable(P + "emotion_deep"));
		EditBox deepBox = field(fieldX, cursor, fieldW, fieldH);
		deepBox.setMaxLength(64);
		deepBox.setValue(card.getString(CardData.EMOTION_DEEP));
		deepBox.setEditable(operator);
		deepBox.setResponder(value -> card.putString(CardData.EMOTION_DEEP, value));
		addProfile(deepBox, cursor);
		cursor += 24;

		line(textX0, cursor + 6, AWAKE, () -> Component.translatable(P + "awakened"));
		Component checkLabel = Component.translatable(P + "awaken_check");
		Component doneLabel = Component.translatable(P + "done");
		Component resetLabel = Component.translatable(P + "reset");
		Component closeLabel = Component.translatable(P + "close");
		int btnH = 20;
		int btnGap = 6;
		int btnW = font.width(checkLabel);
		btnW = Math.max(btnW, font.width(Component.translatable(P + "awakened_no")));
		btnW = Math.max(btnW, font.width(Component.translatable(P + "awakened_yes")));
		btnW = Math.max(btnW, font.width(doneLabel));
		btnW = Math.max(btnW, font.width(resetLabel));
		btnW = Math.max(btnW, font.width(closeLabel)) + 20;
		awakenButton = evenButton(fieldX, cursor, btnW, btnH, awakenLabel(), button -> {
			card.putBoolean(CardData.AWAKENED, !card.getBoolean(CardData.AWAKENED));
			button.setMessage(awakenLabel());
		}, true);
		awakenButton.active = operator;
		addProfile(awakenButton, cursor);
		if (operator) {
			addProfile(evenButton(fieldX + btnW + btnGap, cursor, btnW, btnH, checkLabel, button -> {
				boolean ok = CardData.tryAwaken(card);
				awakenButton.setMessage(awakenLabel());
				if (minecraft != null && minecraft.player != null) {
					minecraft.player.displayClientMessage(
						Component.translatable(P + (ok ? "awaken_success" : "awaken_fail")), true);
				}
			}), cursor);
		}
		cursor += 24;

		line(textX0, cursor + 5, TEXT, () -> Component.translatable(P + "ability_name"));
		EditBox abilityBox = field(fieldX, cursor, fieldW, fieldH);
		abilityBox.setMaxLength(48);
		abilityBox.setValue(card.getString(CardData.ABILITY_NAME));
		abilityBox.setEditable(operator);
		abilityBox.setResponder(value -> card.putString(CardData.ABILITY_NAME, value));
		addProfile(abilityBox, cursor);
		cursor += 26;

		int nameW = 0;
		for (String name : CardData.ABILITY_NAMES) {
			nameW = Math.max(nameW, font.width(name));
		}
		for (List<String> names : CardData.SKILL_CATEGORIES.values()) {
			for (String name : names) {
				nameW = Math.max(nameW, font.width(name));
			}
		}
		int valueW = font.width("00");
		int indent = 12;
		int childX0 = textX0 + indent;
		int colW = Math.max(1, (formX1 - childX0) / 2);

		line(textX0, cursor + 2, ACCENT, () -> Component.translatable(P + "abilities"));
		cursor += 16;
		for (int i = 0; i < CardData.ABILITY_NAMES.size(); i++) {
			String name = CardData.ABILITY_NAMES.get(i);
			int x = childX0 + (i % 2) * colW;
			int y = cursor + (i / 2) * 18;
			stat(x, y, nameW, valueW, Component.literal(name), NAME,
				() -> String.valueOf(CardData.ability(card, name)),
				() -> decAbility(name), () -> incAbility(name),
				name.equals(CardData.ABILITY_LUCK));
		}
		cursor += 18 * ((CardData.ABILITY_NAMES.size() + 1) / 2) + 8;

		for (Map.Entry<String, List<String>> entry : CardData.SKILL_CATEGORIES.entrySet()) {
			line(textX0, cursor + 2, CATEGORY, () -> Component.literal(entry.getKey()));
			cursor += 14;
			List<String> names = entry.getValue();
			for (int i = 0; i < names.size(); i++) {
				String name = names.get(i);
				int x = childX0 + (i % 2) * colW;
				int y = cursor + (i / 2) * 18;
				boolean base = CardData.BASE_SKILLS.contains(name);
				stat(x, y, nameW, valueW, Component.literal(name), base ? BASE : NAME,
					() -> String.valueOf(CardData.skill(card, name)),
					() -> decSkill(name), () -> incSkill(name), false);
			}
			cursor += 18 * ((names.size() + 1) / 2) + 6;
		}
		contentHeight = cursor - formY0 - 2;

		int footerY = panelY + panelH - 26;
		int closeX = formX1 - btnW;
		int resetX = closeX - btnGap - btnW;
		int doneX = resetX - btnGap - btnW;
		Button close = evenButton(closeX, footerY, btnW, btnH, closeLabel, button -> onClose());
		fixedWidgets.add(close);
		addWidget(close);
		if (operator) {
			Button reset = evenButton(resetX, footerY, btnW, btnH, resetLabel, button -> onReset());
			fixedWidgets.add(reset);
			addWidget(reset);
		}
		if (!readOnly) {
			doneButton = evenButton(operator ? doneX : resetX, footerY, btnW, btnH, doneLabel, button -> onDone());
			fixedWidgets.add(doneButton);
			addWidget(doneButton);
			updateDoneState();
		}

		clampScroll();
	}

	private EditBox field(int x, int y, int w, int h) {
		EditBox box = new EditBox(font, x + 2, y, Math.max(8, w - 4), h, Component.empty()) {
			@Override
			public void setFocused(boolean focused) {
				super.setFocused(focused && active);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				return active && super.mouseClicked(mouseX, mouseY, button);
			}

			@Override
			public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
				int bx = getX() - 2;
				int by = getY() - 2;
				int bw = getWidth() + 4;
				int bh = getHeight() + 4;
				drawInset(graphics, bx, by, bw, bh, FIELD);
				String value = getValue();
				int textX = getX() + 3;
				int maxW = Math.max(1, getWidth() - 4);
				int caret = Mth.clamp(getCursorPosition(), 0, value.length());
				int start = 0;
				while (start < caret && font.width(value.substring(start, caret)) > maxW) {
					start++;
				}
				String shown = font.plainSubstrByWidth(value.substring(start), maxW);
				int textY = getY() + getHeight() - 11;
				graphics.drawString(font, Component.literal(shown), textX, textY, NAME, false);
				if (isFocused() && active && Util.getMillis() / 300L % 2L == 0L) {
					int caretX = textX + font.width(value.substring(start, caret));
					if (caretX + font.width("_") <= getX() + getWidth() + 1) {
						graphics.drawString(font, Component.literal("_"), caretX, textY, NAME, false);
					}
				}
			}
		};
		box.setBordered(false);
		box.setCanLoseFocus(true);
		box.setTextColor(0xFF000000);
		box.setTextColorUneditable(0xFF000000);
		return box;
	}

	private void stat(int x, int y, int nameW, int valueW, Component name, int nameColor,
			Supplier<String> value, Runnable dec, Runnable inc, boolean opOnly) {
		line(x, y + 4, nameColor, () -> name);
		int minusX = x + nameW + 4;
		int slotX = minusX + 16;
		int plusX = slotX + valueW + 8;
		valueLine(slotX, 8 + valueW, y + 4, value);
		if (!readOnly) {
			Button minus = smallButton(minusX, y, "-", dec);
			Button plus = smallButton(plusX, y, "+", inc);
			if (opOnly) {
				addProfile(minus, y);
				addProfile(plus, y);
			} else {
				addForm(minus, y);
				addForm(plus, y);
			}
		}
	}

	private Button evenButton(int x, int y, int w, int h, Component label, Button.OnPress onPress) {
		return evenButton(x, y, w, h, label, onPress, false);
	}

	private Button evenButton(int x, int y, int w, int h, Component label, Button.OnPress onPress, boolean keepLook) {
		EvenButton button = new EvenButton(x, y, w, h, label, onPress, keepLook);
		button.setWidth(w);
		button.setHeight(h);
		return button;
	}

	private Button smallButton(int x, int y, String text, Runnable action) {
		return new MomentaryButton(x, y, 16, 16, Component.literal(text), button -> action.run());
	}

	private Component awakenLabel() {
		return Component.translatable(P + (card.getBoolean(CardData.AWAKENED) ? "awakened_yes" : "awakened_no"));
	}

	private void line(int x, int baseY, int color, Supplier<Component> text) {
		lines.add(new Line(x, baseY, color, text, 0));
	}

	private void valueLine(int slotX, int slotW, int baseY, Supplier<String> value) {
		lines.add(new Line(slotX, baseY, VALUE, () -> Component.literal(value.get()), slotW));
	}

	private void addProfile(AbstractWidget widget, int baseY) {
		addForm(widget, baseY, true);
	}

	private void addForm(AbstractWidget widget, int baseY) {
		addForm(widget, baseY, false);
	}

	private void addForm(AbstractWidget widget, int baseY, boolean profile) {
		widget.setY(baseY + scroll);
		formWidgets.add(widget);
		formBaseY.add(baseY);
		formProfile.add(profile);
		addWidget(widget);
	}

	private void updateDoneState() {
		if (doneButton != null) {
			doneButton.active = CardData.requiredComplete(card);
			doneButton.setWidth(doneButton.getWidth());
			doneButton.setHeight(doneButton.getHeight());
		}
	}

	private void incAbility(String name) {
		if (name.equals(CardData.ABILITY_LUCK) && !operator) {
			return;
		}
		int value = CardData.ability(card, name);
		if (value >= CardData.ABILITY_MAX || CardData.abilitySpent(card) >= CardData.ABILITY_POINTS_TOTAL) {
			return;
		}
		CardData.setAbility(card, name, value + 1);
	}

	private void decAbility(String name) {
		if (name.equals(CardData.ABILITY_LUCK) && !operator) {
			return;
		}
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

	private void onReset() {
		if (!operator) {
			return;
		}
		CardData.resetForm(card);
		rebuildWidgets();
	}

	private void onDone() {
		if (readOnly) {
			return;
		}
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

	private boolean inFormView(AbstractWidget widget) {
		return widget.getY() + widget.getHeight() > formY0 && widget.getY() < formY1;
	}

	private boolean inForm(double mouseX, double mouseY) {
		return mouseX >= formX0 && mouseX <= formX1 && mouseY >= formY0 && mouseY <= formY1;
	}

	private boolean inFooter(double mouseY) {
		return mouseY >= panelY + panelH - 30;
	}

	private void clampScroll() {
		int viewH = formY1 - formY0;
		int min = Math.min(0, viewH - contentHeight);
		scroll = Mth.clamp(scroll, min, 0);
		for (int i = 0; i < formWidgets.size(); i++) {
			AbstractWidget widget = formWidgets.get(i);
			widget.setY(formBaseY.get(i) + scroll);
			boolean show = inFormView(widget);
			widget.visible = show;
			boolean allow = formProfile.get(i) ? operator : !readOnly;
			widget.active = show && allow;
			if (widget instanceof EditBox box) {
				box.setEditable(allow);
			}
			if (!show && getFocused() == widget) {
				setFocused(null);
			}
		}
	}

	private boolean canScroll() {
		return contentHeight > formY1 - formY0;
	}

	private int scrollBarX() {
		return formX1 + 2;
	}

	private boolean overPanel(double mouseX, double mouseY) {
		return mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
	}

	private boolean overScrollBar(double mouseX, double mouseY) {
		return canScroll()
			&& mouseX >= scrollBarX()
			&& mouseX <= scrollBarX() + SCROLL_W
			&& mouseY >= formY0
			&& mouseY <= formY1;
	}

	private void applyWheel(double vertical) {
		int delta = (int) Math.round(vertical * 18.0);
		if (delta == 0 && vertical != 0.0) {
			delta = vertical > 0.0 ? 18 : -18;
		}
		scroll += delta;
		clampScroll();
	}

	private int scrollTrackY0() {
		return formY0 + SCROLL_CAP;
	}

	private int scrollTrackH() {
		return Math.max(1, formY1 - formY0 - SCROLL_CAP * 2);
	}

	private int thumbHeight() {
		int viewH = formY1 - formY0;
		return Math.max(18, scrollTrackH() * viewH / contentHeight);
	}

	private void scrollToMouse(double mouseY) {
		int viewH = formY1 - formY0;
		int thumbH = thumbHeight();
		int travel = Math.max(1, scrollTrackH() - thumbH);
		int maxScroll = contentHeight - viewH;
		double t = Mth.clamp((mouseY - scrollTrackY0() - thumbH / 2.0) / travel, 0.0, 1.0);
		scroll = (int) (-t * maxScroll);
		clampScroll();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (canScroll() && overPanel(mouseX, mouseY)) {
			applyWheel(vertical);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		if (inFooter(mouseY)) {
			for (AbstractWidget widget : fixedWidgets) {
				if (widget.isMouseOver(mouseX, mouseY)) {
					return Optional.of(widget);
				}
			}
			return Optional.empty();
		}
		if (!inForm(mouseX, mouseY)) {
			return Optional.empty();
		}
		Optional<GuiEventListener> child = super.getChildAt(mouseX, mouseY);
		if (child.isPresent() && child.get() instanceof AbstractWidget widget && !widget.active) {
			return Optional.empty();
		}
		return child;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && overScrollBar(mouseX, mouseY)) {
			draggingScroll = true;
			setFocused(null);
			scrollToMouse(mouseY);
			return true;
		}
		if (inFooter(mouseY)) {
			for (AbstractWidget widget : fixedWidgets) {
				if (widget.mouseClicked(mouseX, mouseY, button)) {
					setFocused(widget);
					return true;
				}
			}
			setFocused(null);
			return false;
		}
		if (!inForm(mouseX, mouseY)) {
			setFocused(null);
			return false;
		}
		Optional<GuiEventListener> target = getChildAt(mouseX, mouseY);
		if (target.isEmpty()) {
			setFocused(null);
			return true;
		}
		boolean handled = super.mouseClicked(mouseX, mouseY, button);
		if (!(getFocused() instanceof EditBox)) {
			setFocused(null);
		}
		return handled;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingScroll && button == 0) {
			scrollToMouse(mouseY);
			return true;
		}
		if (inFooter(mouseY) || !inForm(mouseX, mouseY)) {
			return false;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && draggingScroll) {
			draggingScroll = false;
			return true;
		}
		if (inFooter(mouseY)) {
			for (AbstractWidget widget : fixedWidgets) {
				if (widget.mouseReleased(mouseX, mouseY, button)) {
					return true;
				}
			}
			return false;
		}
		if (!inForm(mouseX, mouseY)) {
			return false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		drawRaised(graphics, panelX, panelY, panelW, panelH, PANEL);
		graphics.drawString(font, title, panelX + (panelW - font.width(title)) / 2, panelY + 7, 0xFF000000, false);

		drawInset(graphics, skinX - 3, skinY - 3, skinW + 6, skinH + 6, PANEL);
		CardSkinRenderer.render(graphics, skinX, skinY, skinX + skinW, skinY + skinH,
			ownerId, ownerName, skinValue, skinSig);

		int metaX = skinX;
		int metaY = skinY + skinH + 6;
		drawBar(graphics, metaX, metaY, skinW, CardData.hp(card), 15, HP, Component.translatable(P + "hp"));
		drawBar(graphics, metaX, metaY + 14, skinW, CardData.mp(card), 10, MP, Component.translatable(P + "mp"));
		drawBar(graphics, metaX, metaY + 28, skinW, CardData.abilitySpent(card), CardData.ABILITY_POINTS_TOTAL,
			ABILITY, Component.translatable(P + "ap"));
		drawBar(graphics, metaX, metaY + 42, skinW, CardData.skillSpent(card), CardData.SKILL_POINTS_TOTAL,
			SKILL, Component.translatable(P + "sp"));
		if (readOnly) {
			Component mark = Component.literal("🔒 ").append(
				Component.translatable(P + "readonly_mark").withStyle(ChatFormatting.BOLD));
			graphics.drawString(font, mark, metaX + Math.max(0, (skinW - font.width(mark)) / 2),
				metaY + 70, TEXT, false);
		}

		drawInset(graphics, formX0 - 3, formY0 - 3, formX1 - formX0 + 6, formY1 - formY0 + 6, FORM);
		graphics.enableScissor(formX0, formY0, formX1, formY1);
		for (Line line : lines) {
			int y = line.baseY + scroll;
			if (y < formY0 || y + 8 > formY1) {
				continue;
			}
			Component text = line.text.get();
			int x = line.centerW > 0
				? line.x + Math.max(0, (line.centerW - font.width(text)) / 2)
				: line.x;
			graphics.drawString(font, text, x, y, line.color, false);
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

	private void drawRaised(GuiGraphics graphics, int x, int y, int w, int h, int fill) {
		graphics.fill(x, y, x + w, y + h, OUTLINE);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
		graphics.fill(x + 1, y + 1, x + w - 1, y + 2, HIGHLIGHT);
		graphics.fill(x + 1, y + 1, x + 2, y + h - 1, HIGHLIGHT);
		graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, SHADOW);
		graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, SHADOW);
	}

	private void drawInset(GuiGraphics graphics, int x, int y, int w, int h, int fill) {
		graphics.fill(x, y, x + w, y + h, OUTLINE);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
		graphics.fill(x + 1, y + 1, x + w - 1, y + 2, SHADOW);
		graphics.fill(x + 1, y + 1, x + 2, y + h - 1, SHADOW);
		graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, HIGHLIGHT);
		graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, HIGHLIGHT);
	}

	private void drawBar(GuiGraphics graphics, int x, int y, int w, int value, int max, int color, Component label) {
		drawInset(graphics, x, y, w, 12, TRACK);
		int inner = w - 4;
		int filled = max <= 0 ? 0 : Mth.clamp(inner * value / max, 0, inner);
		if (filled > 0) {
			graphics.fill(x + 2, y + 2, x + 2 + filled, y + 10, color);
			graphics.fill(x + 2, y + 2, x + 2 + filled, y + 3, 0x66FFFFFF);
		}
		graphics.drawString(font, label, x + 3, y + 2, BAR_TEXT, false);
		String numbers = value + "/" + max;
		graphics.drawString(font, numbers, x + w - font.width(numbers) - 3, y + 2, BAR_TEXT, false);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawScrollbar(GuiGraphics graphics) {
		int viewH = formY1 - formY0;
		if (contentHeight <= viewH) {
			return;
		}
		int x = scrollBarX();
		drawInset(graphics, x, formY0, SCROLL_W, viewH, SCROLL_WELL);
		drawRaised(graphics, x, formY0, SCROLL_W, SCROLL_CAP, SCROLL_END);
		drawRaised(graphics, x, formY1 - SCROLL_CAP, SCROLL_W, SCROLL_CAP, SCROLL_END);
		drawCaret(graphics, x + SCROLL_W / 2, formY0 + 3, -1);
		drawCaret(graphics, x + SCROLL_W / 2, formY1 - 4, 1);

		int thumbH = thumbHeight();
		int travel = Math.max(1, scrollTrackH() - thumbH);
		int maxScroll = contentHeight - viewH;
		int thumbY = scrollTrackY0() + (int) ((float) (-scroll) / maxScroll * travel);
		drawRaised(graphics, x, thumbY, SCROLL_W, thumbH, SCROLL_THUMB);
		int grip = thumbY + thumbH / 2;
		graphics.fill(x + 2, grip - 3, x + SCROLL_W - 2, grip - 2, SHADOW);
		graphics.fill(x + 2, grip - 1, x + SCROLL_W - 2, grip, SHADOW);
		graphics.fill(x + 2, grip + 1, x + SCROLL_W - 2, grip + 2, SHADOW);
	}

	private void drawCaret(GuiGraphics graphics, int cx, int cy, int dir) {
		graphics.fill(cx, cy, cx + 1, cy + 1, 0xFF202020);
		graphics.fill(cx - 1, cy + dir, cx + 2, cy + dir + 1, 0xFF202020);
		graphics.fill(cx - 2, cy + dir * 2, cx + 3, cy + dir * 2 + 1, 0xFF202020);
	}

	private record Line(int x, int baseY, int color, Supplier<Component> text, int centerW) {
	}

	private final class EvenButton extends Button {
		private final boolean keepLook;

		EvenButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean keepLook) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
			this.keepLook = keepLook;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			boolean look = active || keepLook;
			int fill = !look ? 0xFF7A7A7A : active && isHovered() ? 0xFFD4D4D4 : 0xFFC6C6C6;
			drawRaised(graphics, getX(), getY(), getWidth(), getHeight(), fill);
			int color = look ? NAME : 0xFF6A6A6A;
			int textX = getX() + Math.max(0, (getWidth() - font.width(getMessage())) / 2);
			int textY = getY() + (getHeight() - 8) / 2;
			graphics.drawString(font, getMessage(), textX, textY, color, false);
		}
	}

	private static final class MomentaryButton extends Button {
		MomentaryButton(int x, int y, int width, int height, Component message, OnPress onPress) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		}

		@Override
		public boolean isFocused() {
			return false;
		}

		@Override
		public void setFocused(boolean focused) {
		}
	}
}
