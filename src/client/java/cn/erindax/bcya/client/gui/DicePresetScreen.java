package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.card.SkillTable;
import cn.erindax.bcya.client.check.DicePreset;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DicePresetScreen extends Screen {

	private static final String P = "screen.bcya.dice.";
	private static final String CHECK = "screen.bcya.check.";
	private static final int PANEL = 0xD8F0F2F4;
	private static final int FORM = 0x55FFFFFF;
	private static final int OUTLINE = 0xFFB4B8BC;
	private static final int HIGHLIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF8A9096;
	private static final int ACCENT = 0xFF166534;
	private static final int TEXT = 0xFF000000;
	private static final int NAME = 0xFF000000;
	private static final int SCROLL_WELL = 0x66A0A4A8;
	private static final int SCROLL_END = 0xE6F0F2F4;
	private static final int SCROLL_THUMB = 0xE6D0D4D8;
	private static final int SCROLL_W = 8;
	private static final int SCROLL_CAP = 7;

	private final List<String> skills = new ArrayList<>(SkillTable.ABILITY_OF.keySet());
	private final List<Button> skillButtons = new ArrayList<>();
	private final List<Integer> skillBaseY = new ArrayList<>();
	private final List<Button> difficultyButtons = new ArrayList<>();
	private final List<AbstractWidget> fixedWidgets = new ArrayList<>();

	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int listX0;
	private int listY0;
	private int listX1;
	private int listY1;
	private int diffX0;
	private int diffY0;
	private int diffX1;
	private int diffY1;
	private int contentHeight;
	private int scroll;
	private boolean draggingScroll;

	public DicePresetScreen() {
		super(Component.translatable(P + "title"));
	}

	@Override
	protected void init() {
		super.init();
		skillButtons.clear();
		skillBaseY.clear();
		difficultyButtons.clear();
		fixedWidgets.clear();
		scroll = 0;
		draggingScroll = false;

		panelW = Math.min(width - 20, 420);
		panelH = Math.min(height - 18, 260);
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;

		listX0 = panelX + 12;
		listX1 = panelX + 188;
		listY0 = panelY + 28;
		listY1 = panelY + panelH - 34;
		diffX0 = listX1 + 18;
		diffX1 = panelX + panelW - 12;
		diffY0 = listY0;
		diffY1 = listY1;

		int y = listY0 + 4;
		int skillW = listX1 - listX0 - 8;
		for (String skill : skills) {
			Button button = evenButton(listX0 + 4, y, skillW, 16, skillLabel(skill), b -> selectSkill(skill));
			skillButtons.add(button);
			skillBaseY.add(y);
			addRenderableWidget(button);
			y += 18;
		}
		contentHeight = y - listY0;

		int diffY = diffY0 + 4;
		int diffW = diffX1 - diffX0 - 8;
		for (int level = 1; level <= 4; level++) {
			final int value = level;
			Button button = evenButton(diffX0 + 4, diffY, diffW, 20, difficultyLabel(level), b -> selectDifficulty(value));
			difficultyButtons.add(button);
			addRenderableWidget(button);
			diffY += 26;
		}

		int btnH = 20;
		Component closeLabel = Component.translatable(P + "close");
		int btnW = font.width(closeLabel) + 20;
		Button close = evenButton(panelX + panelW - 12 - btnW, panelY + panelH - 26, btnW, btnH, closeLabel,
			b -> onClose());
		fixedWidgets.add(close);
		addRenderableWidget(close);
		clampScroll();
	}

	private Component skillLabel(String skill) {
		return Component.literal(skill);
	}

	private Component difficultyLabel(int level) {
		return Component.translatable(CHECK + "difficulty." + level);
	}

	private void selectSkill(String skill) {
		DicePreset.setSkill(skill);
	}

	private void selectDifficulty(int level) {
		DicePreset.setDifficulty(level);
	}

	private void clampScroll() {
		int viewH = listY1 - listY0;
		int min = Math.min(0, viewH - contentHeight);
		scroll = Mth.clamp(scroll, min, 0);
		for (int i = 0; i < skillButtons.size(); i++) {
			Button button = skillButtons.get(i);
			button.setY(skillBaseY.get(i) + scroll);
			button.visible = button.getY() + button.getHeight() > listY0 && button.getY() < listY1;
		}
	}

	private boolean canScroll() {
		return contentHeight > listY1 - listY0;
	}

	private int scrollBarX() {
		return listX1 + 2;
	}

	private int scrollTrackY0() {
		return listY0 + SCROLL_CAP;
	}

	private int scrollTrackH() {
		return Math.max(1, listY1 - listY0 - SCROLL_CAP * 2);
	}

	private int thumbHeight() {
		int viewH = listY1 - listY0;
		return Math.max(18, scrollTrackH() * viewH / contentHeight);
	}

	private void applyWheel(double vertical) {
		int delta = (int) Math.round(vertical * 18.0);
		if (delta == 0 && vertical != 0.0) {
			delta = vertical > 0.0 ? 18 : -18;
		}
		scroll += delta;
		clampScroll();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (canScroll() && mouseX >= listX0 && mouseX <= scrollBarX() + SCROLL_W
			&& mouseY >= listY0 && mouseY <= listY1) {
			applyWheel(vertical);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && overScrollBar(mouseX, mouseY)) {
			draggingScroll = true;
			scrollToMouse(mouseY);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingScroll && button == 0) {
			scrollToMouse(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && draggingScroll) {
			draggingScroll = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		drawRaised(graphics, panelX, panelY, panelW, panelH, PANEL);
		graphics.drawString(font, title, panelX + (panelW - font.width(title)) / 2, panelY + 7, TEXT, false);

		drawInset(graphics, listX0 - 3, listY0 - 3, listX1 - listX0 + 6, listY1 - listY0 + 6, FORM);
		drawInset(graphics, diffX0 - 3, diffY0 - 3, diffX1 - diffX0 + 6, diffY1 - diffY0 + 6, FORM);

		graphics.enableScissor(listX0, listY0, listX1, listY1);
		for (int i = 0; i < skillButtons.size(); i++) {
			Button button = skillButtons.get(i);
			boolean selected = skills.get(i).equals(DicePreset.skill());
			if (button instanceof EvenButton even) {
				even.selected = selected;
			}
			if (button.getY() + button.getHeight() < listY0 || button.getY() > listY1) {
				continue;
			}
			button.render(graphics, mouseX, mouseY, partialTick);
		}
		graphics.disableScissor();
		drawScrollbar(graphics);

		for (int i = 0; i < difficultyButtons.size(); i++) {
			Button button = difficultyButtons.get(i);
			if (button instanceof EvenButton even) {
				even.selected = DicePreset.difficulty() == i + 1;
			}
			button.render(graphics, mouseX, mouseY, partialTick);
		}
		for (AbstractWidget widget : fixedWidgets) {
			widget.render(graphics, mouseX, mouseY, partialTick);
		}
	}

	private void drawScrollbar(GuiGraphics graphics) {
		int viewH = listY1 - listY0;
		if (contentHeight <= viewH) {
			return;
		}
		int x = scrollBarX();
		drawInset(graphics, x, listY0, SCROLL_W, viewH, SCROLL_WELL);
		drawRaised(graphics, x, listY0, SCROLL_W, SCROLL_CAP, SCROLL_END);
		drawRaised(graphics, x, listY1 - SCROLL_CAP, SCROLL_W, SCROLL_CAP, SCROLL_END);
		drawCaret(graphics, x + SCROLL_W / 2, listY0 + 3, -1);
		drawCaret(graphics, x + SCROLL_W / 2, listY1 - 4, 1);

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

	private boolean overScrollBar(double mouseX, double mouseY) {
		return canScroll()
			&& mouseX >= scrollBarX()
			&& mouseX <= scrollBarX() + SCROLL_W
			&& mouseY >= listY0
			&& mouseY <= listY1;
	}

	private void scrollToMouse(double mouseY) {
		int viewH = listY1 - listY0;
		int thumbH = thumbHeight();
		int travel = Math.max(1, scrollTrackH() - thumbH);
		int maxScroll = contentHeight - viewH;
		double t = Mth.clamp((mouseY - scrollTrackY0() - thumbH / 2.0) / travel, 0.0, 1.0);
		scroll = (int) (-t * maxScroll);
		clampScroll();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private EvenButton evenButton(int x, int y, int w, int h, Component message, Button.OnPress onPress) {
		return new EvenButton(x, y, w, h, message, onPress);
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

	private final class EvenButton extends Button {
		private boolean selected;

		EvenButton(int x, int y, int width, int height, Component message, OnPress onPress) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int fill = selected ? 0xFFD4D4D4 : isHovered() ? 0xFFD4D4D4 : 0xFFC6C6C6;
			drawRaised(graphics, getX(), getY(), getWidth(), getHeight(), fill);
			int color = selected ? ACCENT : NAME;
			int textX = getX() + Math.max(0, (getWidth() - font.width(getMessage())) / 2);
			int textY = getY() + (getHeight() - 8) / 2;
			graphics.drawString(font, getMessage(), textX, textY, color, false);
		}
	}
}
