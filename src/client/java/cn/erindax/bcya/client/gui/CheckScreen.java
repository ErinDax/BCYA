package cn.erindax.bcya.client.gui;

import cn.erindax.bcya.card.CardData;
import cn.erindax.bcya.check.net.CheckRollPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class CheckScreen extends Screen {

	private static final String P = "screen.bcya.check.";
	private static final int PANEL = 0xD8F0F2F4;
	private static final int FORM = 0x55FFFFFF;
	private static final int OUTLINE = 0xFFB4B8BC;
	private static final int HIGHLIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF8A9096;
	private static final int ACCENT = 0xFF047857;
	private static final int TEXT = 0xFF000000;
	private static final int POOL = 0xFF1565C0;
	private static final int FAIL = 0xFFD32F2F;
	private static final int CRITICAL = 0xFFCA8A04;
	private static final int CRITICAL_FAIL = 0xFFB71C1C;
	private static final int NAME = 0xFF000000;
	private static final int MUTED = 0xFFA16207;
	private static final int ROLL_TICKS = 16;

	private final CompoundTag data;
	private final boolean promptMode;
	private final int requestId;
	private final Random random = new Random();

	private CompoundTag result;
	private boolean rolling;
	private int rollingTicks;
	private int[] fakeDice = new int[0];

	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int bodyX;
	private int bodyY;
	private int bodyW;
	private int bodyH;
	private int infoX;
	private int diceX;
	private int diceW;
	private int trayX;
	private int trayY;
	private int trayW;
	private int trayH;

	private boolean rollOpen;
	private final List<AbstractWidget> widgets = new ArrayList<>();

	public CheckScreen(CompoundTag data, boolean promptMode) {
		super(Component.translatable(P + "title"));
		this.data = data;
		this.promptMode = promptMode;
		this.requestId = data.getInt("request_id");
		if (!promptMode) {
			this.result = data;
		}
		this.rollOpen = promptMode && this.result == null;
	}

	public boolean matches(CompoundTag tag) {
		return tag.getInt("request_id") == requestId;
	}

	public void onResult(CompoundTag tag) {
		result = tag;
		rollOpen = false;
	}

	@Override
	protected void init() {
		super.init();
		widgets.clear();
		panelW = Math.min(width - 20, 440);
		panelH = Math.min(height - 18, 236);
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;
		bodyX = panelX + 12;
		bodyY = panelY + 28;
		bodyW = panelW - 24;
		bodyH = panelH - 62;
		infoX = bodyX + 8;
		diceX = bodyX + 176;
		diceW = bodyX + bodyW - 8 - diceX;
		trayX = diceX - 4;
		trayY = bodyY + 4;
		trayW = diceW + 8;
		trayH = bodyH - 8;

		int btnH = 20;
		Component closeLabel = Component.translatable(P + "close");
		int closeW = font.width(closeLabel) + 20;
		int footerY = panelY + panelH - 26;
		Button close = evenButton(panelX + panelW - 12 - closeW, footerY, closeW, btnH, closeLabel, button -> onClose());
		widgets.add(close);
		addRenderableWidget(close);

		int pool = Math.max(1, data.getInt("pool"));
		fakeDice = new int[pool];
		for (int i = 0; i < fakeDice.length; i++) {
			fakeDice[i] = random.nextInt(CardData.DICE_FACES) + 1;
		}
	}

	private void onRoll() {
		if (!rollOpen || rolling) {
			return;
		}
		rollOpen = false;
		rolling = true;
		rollingTicks = ROLL_TICKS;
		for (int i = 0; i < fakeDice.length; i++) {
			fakeDice[i] = random.nextInt(CardData.DICE_FACES) + 1;
		}
		ClientPlayNetworking.send(new CheckRollPayload(requestId));
	}

	private boolean inTray(double mouseX, double mouseY) {
		return mouseX >= trayX && mouseX < trayX + trayW && mouseY >= trayY && mouseY < trayY + trayH;
	}

	@Override
	public void tick() {
		super.tick();
		if (rolling && rollingTicks > 0) {
			rollingTicks--;
			for (int i = 0; i < fakeDice.length; i++) {
				fakeDice[i] = random.nextInt(CardData.DICE_FACES) + 1;
			}
		}
	}

	private boolean resultVisible() {
		return result != null && (!rolling || rollingTicks <= 0);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		boolean trayHot = rollOpen && inTray(mouseX, mouseY);
		drawRaised(graphics, panelX, panelY, panelW, panelH, PANEL);
		graphics.drawString(font, title, panelX + (panelW - font.width(title)) / 2, panelY + 7, TEXT, false);
		drawInset(graphics, bodyX - 3, bodyY - 3, bodyW + 6, bodyH + 6, FORM);
		drawInset(graphics, trayX, trayY, trayW, trayH, trayHot ? 0x99FFF4DC : FORM);

		boolean showResult = resultVisible();
		CompoundTag view = showResult ? result : data;
		int x = infoX;
		int y = bodyY + 8;

		String kp = view.getString("kp");
		if (!kp.isEmpty()) {
			drawLine(graphics, Component.translatable(P + "kp", kp), x, y, TEXT);
			y += 16;
		}
		drawLine(graphics, Component.translatable(P + "skill", view.getString("skill")), x, y, TEXT);
		y += 16;
		int difficulty = view.getInt("difficulty");
		Component difficultyName = difficulty >= 1 && difficulty <= 4
			? Component.translatable(P + "difficulty." + difficulty)
			: Component.literal("-");
		drawLine(graphics, Component.translatable(P + "difficulty", difficultyName), x, y, TEXT);
		y += 16;
		drawLine(graphics, Component.translatable(P + "level", view.getInt("level")), x, y, TEXT);
		y += 16;
		drawLine(graphics, Component.translatable(P + "check_value", view.getInt("check_value")), x, y, TEXT);
		y += 16;
		drawLine(graphics, Component.translatable(P + "modifier", view.getInt("modifier")), x, y, TEXT);
		y += 16;
		drawLine(graphics, Component.translatable(P + "pool", view.getInt("pool")), x, y, POOL);

		if (rollOpen) {
			String label = Component.translatable(P + "roll").getString();
			drawScaledCentered(graphics, label, diceX + diceW / 2, bodyY + bodyH / 2 - 2,
				trayHot ? ACCENT : MUTED, 3f);
		} else if (rolling && rollingTicks > 0) {
			renderDice(graphics, fakeDice, view.getBoolean("unskilled"), false);
		} else if (showResult) {
			if (view.getBoolean("blocked")) {
				drawCentered(graphics, Component.translatable(P + "blocked"), diceX, diceW, bodyY + bodyH / 2 - 4, FAIL);
			} else {
				int[] dice = view.getIntArray("dice");
				renderDice(graphics, dice, view.getBoolean("unskilled"), true);
				drawCentered(graphics, Component.translatable(P + "successes", view.getInt("successes")),
					diceX, diceW, bodyY + bodyH - 36, TEXT);
				String outcomeId = view.getString("outcome");
				if (outcomeId.isEmpty()) {
					outcomeId = view.getBoolean("success") ? "success" : "fail";
				}
				Component outcome = Component.translatable(P + "outcome." + outcomeId)
					.withStyle(ChatFormatting.BOLD);
				drawCentered(graphics, outcome, diceX, diceW, bodyY + bodyH - 20, outcomeColor(outcomeId));
			}
		}

		for (AbstractWidget widget : widgets) {
			if (widget.visible) {
				widget.render(graphics, mouseX, mouseY, partialTick);
			}
		}
	}

	private void renderDice(GuiGraphics graphics, int[] dice, boolean unskilled, boolean markHit) {
		int count = Math.max(1, dice.length);
		float scale = count <= 1 ? 4.5f : count <= 3 ? 3f : 2f;
		int gap = count <= 1 ? 0 : 10;
		int unitW = Math.round(font.width("0") * scale) + gap;
		int unitH = Math.round(8 * scale) + 8;
		int perRow = Math.min(count, Math.max(1, diceW / Math.max(1, unitW)));
		int rows = (count + perRow - 1) / perRow;
		int gridW = perRow * unitW - gap;
		int gridH = rows * unitH - 8;
		int originX = diceX + Math.max(0, (diceW - gridW) / 2);
		int originY = bodyY + Math.max(8, (bodyH - 44 - gridH) / 2);
		for (int i = 0; i < dice.length; i++) {
			int col = i % perRow;
			int row = i / perRow;
			int value = dice[i];
			boolean hit = markHit && (unskilled ? value == CardData.UNTRAINED_SUCCESS : value >= CardData.DICE_SUCCESS_MIN);
			int cx = originX + col * unitW + unitW / 2 - gap / 2;
			int cy = originY + row * unitH + Math.round(4 * scale);
			drawScaledCentered(graphics, String.valueOf(value), cx, cy, hit ? ACCENT : NAME, scale);
		}
	}

	private void drawScaledCentered(GuiGraphics graphics, String text, int cx, int cy, int color, float scale) {
		float w = font.width(text) * scale;
		float h = 8 * scale;
		graphics.pose().pushPose();
		graphics.pose().translate(cx - w / 2f, cy - h / 2f, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, 0, 0, color, false);
		graphics.pose().popPose();
	}

	private static int outcomeColor(String outcomeId) {
		if ("critical_success".equals(outcomeId)) {
			return CRITICAL;
		}
		if ("success".equals(outcomeId)) {
			return ACCENT;
		}
		if ("critical_fail".equals(outcomeId)) {
			return CRITICAL_FAIL;
		}
		return FAIL;
	}

	private void drawLine(GuiGraphics graphics, Component text, int x, int y, int color) {
		graphics.drawString(font, colored(text.getString(), color), x, y, color, false);
	}

	private void drawCentered(GuiGraphics graphics, Component text, int x, int w, int y, int color) {
		String line = text.getString();
		graphics.drawString(font, colored(line, color), x + Math.max(0, (w - font.width(line)) / 2), y, color, false);
	}

	private static Component colored(String line, int color) {
		int rgb = color & 0xFFFFFF;
		if (rgb == 0) {
			rgb = 0x0A0A0A;
		}
		return Component.literal(line).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && rollOpen && inTray(mouseX, mouseY)) {
			onRoll();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
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
		EvenButton(int x, int y, int width, int height, Component message, OnPress onPress) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int fill = !active ? 0xFF7A7A7A : isHovered() ? 0xFFD4D4D4 : 0xFFC6C6C6;
			drawRaised(graphics, getX(), getY(), getWidth(), getHeight(), fill);
			int color = active ? NAME : 0xFF6A6A6A;
			int textX = getX() + Math.max(0, (getWidth() - font.width(getMessage())) / 2);
			int textY = getY() + (getHeight() - 8) / 2;
			graphics.drawString(font, getMessage(), textX, textY, color, false);
		}
	}
}
