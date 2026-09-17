package cn.erindax.bcya.client.mixin;

import cn.erindax.bcya.item.CatEyeItem;
import cn.erindax.bcya.util.MaskUtil;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererNameTagMixin {

	@Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
	private void bcya$hideNameWhenWearingMask(
		AbstractClientPlayer player,
		Component component,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int i,
		float f,
		CallbackInfo ci
	) {
		if (MaskUtil.isWearingAnyMask(player) && !CatEyeItem.isActive(Minecraft.getInstance().player)) {
			ci.cancel();
		}
	}
}
