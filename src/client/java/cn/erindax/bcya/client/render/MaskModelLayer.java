package cn.erindax.bcya.client.render;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.mask.slot.MaskSlots;
import cn.erindax.bcya.util.MaskUtil;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MaskModelLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

	private final ItemInHandRenderer itemInHandRenderer;

	public MaskModelLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
			ItemInHandRenderer itemInHandRenderer) {
		super(renderer);
		this.itemInHandRenderer = itemInHandRenderer;
	}

	@Override
	public void render(
			PoseStack poseStack,
			MultiBufferSource multiBufferSource,
			int light,
			AbstractClientPlayer player,
			float limbSwing,
			float limbSwingAmount,
			float partialTick,
			float ageInTicks,
			float netHeadYaw,
			float headPitch
	) {
		MaskItem mask = MaskUtil.getWornMask(player);
		if (mask == null || !mask.rendersOnHead()) {
			return;
		}

		ItemStack stack = MaskSlots.get(player);
		poseStack.pushPose();
		this.getParentModel().getHead().translateAndRotate(poseStack);
		CustomHeadLayer.translateToHead(poseStack, false);
		this.itemInHandRenderer.renderItem(player, stack, ItemDisplayContext.HEAD, false, poseStack, multiBufferSource, light);
		poseStack.popPose();
	}
}
