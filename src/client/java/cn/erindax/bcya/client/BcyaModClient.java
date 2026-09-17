package cn.erindax.bcya.client;

import cn.erindax.bcya.block.ModBlocks;
import cn.erindax.bcya.card.net.CardOpenPayload;
import cn.erindax.bcya.card.net.SaveCardResultPayload;
import cn.erindax.bcya.client.gui.CardScreen;
import cn.erindax.bcya.client.gui.KeySkinScreen;
import cn.erindax.bcya.client.gui.LockOwnerScreen;
import cn.erindax.bcya.client.gui.LockPasswordScreen;
import cn.erindax.bcya.client.gui.MaskSkinScreen;
import cn.erindax.bcya.client.gui.MusicNoteScreen;
import cn.erindax.bcya.client.gui.PatrollerListScreen;
import cn.erindax.bcya.client.gui.PatrollerSettingsScreen;
import cn.erindax.bcya.client.lock.ClientLocks;
import cn.erindax.bcya.client.lock.LockRenderer;
import cn.erindax.bcya.client.mask.MaskSlotWidget;
import cn.erindax.bcya.client.music.ClientMusic;
import cn.erindax.bcya.client.music.ClientMusicBlocks;
import cn.erindax.bcya.client.music.MusicBlockRenderer;
import cn.erindax.bcya.client.music.MusicPlayer;
import cn.erindax.bcya.client.music.MusicUploader;
import cn.erindax.bcya.client.render.KeyItemRenderer;
import cn.erindax.bcya.client.render.MaskModelLayer;
import cn.erindax.bcya.client.render.MaskSkins;
import cn.erindax.bcya.client.render.PatrollerModelLayers;
import cn.erindax.bcya.client.render.PatrollerRenderer;
import cn.erindax.bcya.client.render.RemoteTextures;
import cn.erindax.bcya.entity.ModEntities;
import cn.erindax.bcya.entity.net.PatrollerListPayload;
import cn.erindax.bcya.entity.net.PatrollerSettingsPayload;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.lock.LockMode;
import cn.erindax.bcya.lock.LockType;
import cn.erindax.bcya.lock.net.KeySkinListPayload;
import cn.erindax.bcya.lock.net.LockScreenPayload;
import cn.erindax.bcya.lock.net.LockSyncPayload;
import cn.erindax.bcya.lock.net.LockUpdatePayload;
import cn.erindax.bcya.mask.net.MaskSkinEditorPayload;
import cn.erindax.bcya.mask.net.MaskSkinSyncPayload;
import cn.erindax.bcya.mask.slot.MaskSlots;
import cn.erindax.bcya.music.net.MusicBlockSyncPayload;
import cn.erindax.bcya.music.net.MusicBlockUpdatePayload;
import cn.erindax.bcya.music.net.MusicControlPayload;
import cn.erindax.bcya.music.net.MusicDataPayload;
import cn.erindax.bcya.music.net.MusicMenuPayload;
import cn.erindax.bcya.skin.TexturePayload;
import cn.erindax.bcya.util.MaskUtil;
import cn.erindax.bcya.voice.MaskVoiceClientState;
import cn.erindax.bcya.voice.MaskVoiceSyncPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

public class BcyaModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityType == EntityType.PLAYER && entityRenderer instanceof PlayerRenderer playerRenderer) {
				registrationHelper.register(new MaskModelLayer(playerRenderer, context.getItemInHandRenderer()));
			}
		});

		MaskVoiceClientState.setLocalWornMaskSupplier(() -> MaskUtil.getWornMask(Minecraft.getInstance().player));
		MaskSlots.setClientCreativeSync(player -> {
			if (player instanceof LocalPlayer localPlayer) {
				MaskSlotWidget.sendCreative(localPlayer);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(MaskVoiceSyncPayload.TYPE, (payload, context) ->
			MaskVoiceClientState.setDisabledMasks(payload.disabledMasks()));
		ClientPlayNetworking.registerGlobalReceiver(TexturePayload.TYPE, (payload, context) ->
			RemoteTextures.put(payload.kind(), payload.name(), payload.png()));
		ClientPlayNetworking.registerGlobalReceiver(PatrollerSettingsPayload.TYPE, (payload, context) ->
			context.client().setScreen(new PatrollerSettingsScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(PatrollerListPayload.TYPE, (payload, context) ->
			context.client().setScreen(new PatrollerListScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MaskSkinSyncPayload.TYPE, (payload, context) ->
			MaskSkins.setOverrides(payload.overrides()));
		ClientPlayNetworking.registerGlobalReceiver(MaskSkinEditorPayload.TYPE, (payload, context) ->
			context.client().setScreen(new MaskSkinScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(LockSyncPayload.TYPE, (payload, context) ->
			ClientLocks.replace(payload));
		ClientPlayNetworking.registerGlobalReceiver(LockUpdatePayload.TYPE, (payload, context) ->
			ClientLocks.update(payload.pos(), payload.lockType()));
		ClientPlayNetworking.registerGlobalReceiver(LockScreenPayload.TYPE, (payload, context) ->
			context.client().setScreen(switch (payload.kind()) {
				case SETUP_PASSWORD -> new LockPasswordScreen(payload.pos(), true);
				case UNLOCK_PASSWORD -> new LockPasswordScreen(payload.pos(), false);
				case OWNER -> new LockOwnerScreen(payload.pos(), LockType.byIndex(payload.lockType()),
					LockMode.byIndex(payload.mode()));
			}));
		ClientPlayNetworking.registerGlobalReceiver(KeySkinListPayload.TYPE, (payload, context) ->
			context.client().setScreen(new KeySkinScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MusicMenuPayload.TYPE, (payload, context) ->
			context.client().setScreen(new MusicNoteScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MusicControlPayload.TYPE, (payload, context) ->
			MusicPlayer.handle(payload));
		ClientPlayNetworking.registerGlobalReceiver(MusicDataPayload.TYPE, (payload, context) ->
			ClientMusic.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(MusicBlockSyncPayload.TYPE, (payload, context) ->
			ClientMusicBlocks.replace(payload));
		ClientPlayNetworking.registerGlobalReceiver(MusicBlockUpdatePayload.TYPE, (payload, context) ->
			ClientMusicBlocks.update(payload.pos(), payload.present(), payload.track(), payload.range()));
		ClientPlayNetworking.registerGlobalReceiver(CardOpenPayload.TYPE, (payload, context) -> {
			CompoundTag owner = payload.ownerInfo();
			UUID ownerId;
			try {
				ownerId = UUID.fromString(owner.getString("uuid"));
			} catch (IllegalArgumentException exception) {
				ownerId = new UUID(0L, 0L);
			}
			context.client().setScreen(new CardScreen(payload.card(), payload.readOnly(), payload.mainHand(),
				ownerId, owner.getString("name"), owner.getString("skin"), owner.getString("skin_sig")));
		});
		ClientPlayNetworking.registerGlobalReceiver(SaveCardResultPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			if (client.player != null) {
				client.player.displayClientMessage(Component.translatable(payload.messageKey()), false);
			}
			if (payload.success()) {
				client.setScreen(null);
			} else if (client.screen instanceof CardScreen cardScreen) {
				cardScreen.onSaveFailed();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MaskVoiceClientState.reset();
			MaskSkins.reset();
			RemoteTextures.clear();
			ClientLocks.clear();
			MusicPlayer.clear();
			ClientMusic.clear();
			ClientMusicBlocks.clear();
			MusicUploader.reset();
		});

		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BARRIER_PANEL, RenderType.cutout());
		PatrollerModelLayers.init();
		EntityRendererRegistry.register(ModEntities.PATROLLER, PatrollerRenderer::new);
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.KEY, new KeyItemRenderer());
		LockRenderer.init();
		MusicBlockRenderer.init();
		MusicUploader.init();
	}
}
