package cn.erindax.bcya.client.skywalk;

import cn.erindax.bcya.skywalk.Skywalk;
import cn.erindax.bcya.skywalk.net.SkywalkSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SkywalkClient {

	private static final double STILL = 1.0E-4;

	private static boolean enabled;
	private static int depth = Skywalk.DEFAULT_DEPTH;
	private static boolean holding;
	private static boolean released;

	private SkywalkClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(SkywalkSyncPayload.TYPE, (payload, context) -> {
			enabled = payload.enabled();
			depth = payload.depth();
			holding = false;
			released = false;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			enabled = false;
			depth = Skywalk.DEFAULT_DEPTH;
			holding = false;
			released = false;
		});
	}

	public static boolean shouldHold(LocalPlayer player) {
		if (!enabled || !Skywalk.canHover(player)) {
			holding = false;
			released = false;
			return false;
		}
		if (released) {
			return false;
		}
		if (player.getXRot() >= Skywalk.LOOK_DOWN_PITCH) {
			holding = false;
			released = true;
			return false;
		}
		if (!Skywalk.hasAirBelow(player.level(), player, depth)) {
			holding = false;
			return false;
		}
		if (!holding && player.getDeltaMovement().y > 0.0) {
			return false;
		}
		holding = true;
		return true;
	}

	public static boolean isHovering(AbstractClientPlayer player) {
		return enabled && Skywalk.canHover(player) && player.getXRot() < Skywalk.LOOK_DOWN_PITCH
			&& Math.abs(player.getX() - player.xo) < STILL && Math.abs(player.getY() - player.yo) < STILL
			&& Math.abs(player.getZ() - player.zo) < STILL && Skywalk.hasAirBelow(player.level(), player, depth);
	}

	public static void stand(LivingEntity entity) {
		entity.walkAnimation.update(0.0F, 1.0F);
		entity.walkAnimation.update(0.0F, 1.0F);
	}
}
