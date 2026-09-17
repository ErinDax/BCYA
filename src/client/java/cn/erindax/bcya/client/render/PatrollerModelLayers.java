package cn.erindax.bcya.client.render;

import cn.erindax.bcya.BcyaMod;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class PatrollerModelLayers {

	public static final ModelLayerLocation WIDE = layer("main");
	public static final ModelLayerLocation SLIM = layer("slim");
	public static final ModelLayerLocation INNER_ARMOR = layer("inner_armor");
	public static final ModelLayerLocation OUTER_ARMOR = layer("outer_armor");

	private PatrollerModelLayers() {
	}

	public static void init() {
		EntityModelLayerRegistry.registerModelLayer(WIDE,
			() -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64));
		EntityModelLayerRegistry.registerModelLayer(SLIM,
			() -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64));
		EntityModelLayerRegistry.registerModelLayer(INNER_ARMOR,
			() -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(new CubeDeformation(0.5F)), 64, 32));
		EntityModelLayerRegistry.registerModelLayer(OUTER_ARMOR,
			() -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(new CubeDeformation(1.0F)), 64, 32));
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(BcyaMod.id("patroller"), name);
	}
}
