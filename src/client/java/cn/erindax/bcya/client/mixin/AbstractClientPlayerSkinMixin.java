package cn.erindax.bcya.client.mixin;

import cn.erindax.bcya.client.render.MaskSkins;
import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.util.MaskUtil;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerSkinMixin {

	@Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
	private void bcya$replaceSkinWhenWearingMask(CallbackInfoReturnable<PlayerSkin> cir) {
		MaskItem mask = MaskUtil.getWornMask((AbstractClientPlayer) (Object) this);
		if (mask != null) {
			cir.setReturnValue(MaskSkins.of(mask));
		}
	}
}
