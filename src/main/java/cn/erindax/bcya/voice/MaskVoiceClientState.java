package cn.erindax.bcya.voice;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class MaskVoiceClientState {

	private static volatile Set<ResourceLocation> disabledMasks = Set.of();
	private static volatile Supplier<Item> localWornMaskSupplier = () -> null;

	private MaskVoiceClientState() {
	}

	public static void setDisabledMasks(Collection<ResourceLocation> masks) {
		disabledMasks = Set.copyOf(masks);
	}

	public static void reset() {
		disabledMasks = Set.of();
	}

	public static void setLocalWornMaskSupplier(Supplier<Item> supplier) {
		localWornMaskSupplier = supplier;
	}

	public static boolean isVoiceDisabled(Item mask) {
		return disabledMasks.contains(BuiltInRegistries.ITEM.getKey(mask));
	}

	public static boolean shouldModifyLocalVoice() {
		Item mask = localWornMaskSupplier.get();
		return mask != null && !isVoiceDisabled(mask);
	}
}
