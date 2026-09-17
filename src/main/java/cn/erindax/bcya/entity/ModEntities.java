package cn.erindax.bcya.entity;

import cn.erindax.bcya.BcyaMod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {

	public static final EntityType<PatrollerEntity> PATROLLER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		BcyaMod.id("patroller"),
		EntityType.Builder.of(PatrollerEntity::new, MobCategory.MISC)
			.sized(0.6F, 1.8F)
			.eyeHeight(1.62F)
			.clientTrackingRange(10)
			.build("patroller"));

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(PATROLLER, PatrollerEntity.createAttributes());
	}
}
