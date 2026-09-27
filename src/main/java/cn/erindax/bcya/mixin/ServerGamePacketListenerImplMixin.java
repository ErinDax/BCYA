package cn.erindax.bcya.mixin;

import cn.erindax.bcya.skywalk.Skywalk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

	@Shadow
	public ServerPlayer player;

	@Shadow
	private int aboveGroundTickCount;

	@Inject(method = "getMaximumFlyingTicks", at = @At("HEAD"))
	private void bcya$keepSkywalkerOnline(Entity entity, CallbackInfoReturnable<Integer> cir) {
		if (entity == player && Skywalk.isHovering(player)) {
			aboveGroundTickCount = 0;
		}
	}
}
