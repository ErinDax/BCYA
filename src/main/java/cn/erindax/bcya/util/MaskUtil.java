package cn.erindax.bcya.util;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.mask.slot.MaskSlots;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

public final class MaskUtil {

	private MaskUtil() {
	}

	public static boolean isWearingAnyMask(LivingEntity entity) {
		return getWornMask(entity) != null;
	}

	@Nullable
	public static MaskItem getWornMask(@Nullable LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return null;
		}
		return MaskSlots.get(player).getItem() instanceof MaskItem mask ? mask : null;
	}
}
