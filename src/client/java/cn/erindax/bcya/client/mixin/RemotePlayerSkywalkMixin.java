package cn.erindax.bcya.client.mixin;

import cn.erindax.bcya.client.skywalk.SkywalkClient;

import net.minecraft.client.player.RemotePlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RemotePlayer.class)
public abstract class RemotePlayerSkywalkMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void bcya$standWhileHovering(CallbackInfo ci) {
		RemotePlayer player = (RemotePlayer) (Object) this;
		if (SkywalkClient.isHovering(player)) {
			SkywalkClient.stand(player);
		}
	}
}
