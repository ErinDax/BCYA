package cn.erindax.bcya.mask.slot;

import cn.erindax.bcya.BcyaMod;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.world.item.ItemStack;

public final class ModAttachments {

	public static final AttachmentType<ItemStack> MASK = AttachmentRegistry.create(BcyaMod.id("mask"), builder -> builder
		.initializer(() -> ItemStack.EMPTY)
		.persistent(ItemStack.OPTIONAL_CODEC)
		.copyOnDeath()
		.syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.all()));

	private ModAttachments() {
	}

	public static void init() {
	}
}
