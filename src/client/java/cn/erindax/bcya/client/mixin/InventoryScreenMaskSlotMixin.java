package cn.erindax.bcya.client.mixin;

import cn.erindax.bcya.client.mask.MaskSlotWidget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMaskSlotMixin extends AbstractContainerScreen<InventoryMenu> {

	@Unique
	private static final int SLOT_X = 77;
	@Unique
	private static final int SLOT_Y = 8;

	private InventoryScreenMaskSlotMixin() {
		super(null, null, null);
	}

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void bcya$renderMaskSlot(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		if (this.minecraft.player != null) {
			MaskSlotWidget.render(graphics, this.font, this.minecraft.player,
				this.leftPos + SLOT_X, this.topPos + SLOT_Y, mouseX, mouseY);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void bcya$renderMaskTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.minecraft.player != null) {
			MaskSlotWidget.renderTooltip(graphics, this.font, this.minecraft.player, this.menu,
				this.leftPos + SLOT_X, this.topPos + SLOT_Y, mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void bcya$clickMaskSlot(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (MaskSlotWidget.isClickButton(button)
				&& MaskSlotWidget.isHovering(this.leftPos + SLOT_X, this.topPos + SLOT_Y, mouseX, mouseY)) {
			MaskSlotWidget.clickSurvival();
			cir.setReturnValue(true);
		}
	}
}
