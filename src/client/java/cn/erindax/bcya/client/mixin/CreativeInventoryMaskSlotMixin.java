package cn.erindax.bcya.client.mixin;

import cn.erindax.bcya.client.mask.MaskSlotWidget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryMaskSlotMixin
		extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

	@Unique
	private static final int SLOT_X = 126;
	@Unique
	private static final int SLOT_Y = 6;

	private CreativeInventoryMaskSlotMixin() {
		super(null, null, null);
	}

	@Shadow
	public abstract boolean isInventoryOpen();

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void bcya$renderMaskSlot(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		if (isInventoryOpen() && this.minecraft.player != null) {
			MaskSlotWidget.render(graphics, this.font, this.minecraft.player,
				this.leftPos + SLOT_X, this.topPos + SLOT_Y, mouseX, mouseY);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void bcya$renderMaskTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (isInventoryOpen() && this.minecraft.player != null) {
			MaskSlotWidget.renderTooltip(graphics, this.font, this.minecraft.player, this.menu,
				this.leftPos + SLOT_X, this.topPos + SLOT_Y, mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void bcya$clickMaskSlot(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (isInventoryOpen() && this.minecraft.player != null && MaskSlotWidget.isClickButton(button)
				&& MaskSlotWidget.isHovering(this.leftPos + SLOT_X, this.topPos + SLOT_Y, mouseX, mouseY)) {
			MaskSlotWidget.clickCreative(this.minecraft.player, this.menu);
			cir.setReturnValue(true);
		}
	}
}
