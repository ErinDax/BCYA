package cn.erindax.bcya.client.mixin;

import cn.erindax.bcya.client.skywalk.SkywalkClient;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerSkywalkMixin {

	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void bcya$hoverUntilLookingDown(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && SkywalkClient.shouldHold(player)) {
			player.setDeltaMovement(Vec3.ZERO);
			SkywalkClient.stand(player);
			ci.cancel();
		}
	}
}
