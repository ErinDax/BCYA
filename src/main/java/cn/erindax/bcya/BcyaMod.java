package cn.erindax.bcya;

import cn.erindax.bcya.block.ModBlocks;
import cn.erindax.bcya.card.CardHandler;
import cn.erindax.bcya.check.CheckHandler;
import cn.erindax.bcya.command.BcyaCommands;
import cn.erindax.bcya.command.DifficultyArgumentType;
import cn.erindax.bcya.command.SkillArgumentType;
import cn.erindax.bcya.entity.ModEntities;
import cn.erindax.bcya.entity.PatrolRecorder;
import cn.erindax.bcya.entity.PatrollerEntity;
import cn.erindax.bcya.entity.net.PatrollerSettingsHandler;
import cn.erindax.bcya.item.ModComponents;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.lock.LockHandler;
import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.mask.MaskRulesState;
import cn.erindax.bcya.mask.net.MaskSkinHandler;
import cn.erindax.bcya.mask.slot.MaskSlotEvents;
import cn.erindax.bcya.mask.slot.ModAttachments;
import cn.erindax.bcya.mask.slot.net.MaskSlotHandler;
import cn.erindax.bcya.music.MusicHandler;
import cn.erindax.bcya.skin.TexturePayload;
import cn.erindax.bcya.skin.TextureStore;
import cn.erindax.bcya.voice.MaskVoiceSyncPayload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BcyaMod implements ModInitializer {
	public static final String MOD_ID = "bcya";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SkillArgumentType.register();
		DifficultyArgumentType.register();
		ModComponents.init();
		ModAttachments.init();
		ModBlocks.init();
		ModItems.init();
		ModEntities.init();
		MaskSlotEvents.init();

		PayloadTypeRegistry.playS2C().register(MaskVoiceSyncPayload.TYPE, MaskVoiceSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TexturePayload.TYPE, TexturePayload.STREAM_CODEC);
		MaskSkinHandler.init();
		MaskSlotHandler.init();
		PatrollerSettingsHandler.init();
		PatrolRecorder.init();
		LockHandler.init();
		MusicHandler.init();
		CardHandler.init();
		CheckHandler.init();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sender.sendPacket(MaskRulesState.get(server).toVoicePayload());
			MaskSkinHandler.sendAllTo(handler.player);
		});
		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			if (entity instanceof PatrollerEntity patroller && !patroller.getSkinName().isEmpty()) {
				TextureStore.SKINS.sendTo(player, patroller.getSkinName());
			}
		});
		TextureStore.SKINS.ensureDirectory();
		TextureStore.KEYS.ensureDirectory();
		WandWhitelist.reload();
		BcyaCommands.register();

		LOGGER.info("BCYA loaded, masks registered.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
