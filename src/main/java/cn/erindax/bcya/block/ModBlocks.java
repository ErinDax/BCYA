package cn.erindax.bcya.block;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {

	public static final Block BARRIER_PANEL = register("barrier_panel",
		new BarrierPanelBlock(BlockBehaviour.Properties.of()
			.strength(-1.0F, 3600000.8F)
			.noLootTable()
			.noOcclusion()
			.sound(SoundType.STONE)));

	private ModBlocks() {
	}

	private static <T extends Block> T register(String name, T block) {
		return Registry.register(BuiltInRegistries.BLOCK, BcyaMod.id(name), block);
	}

	public static void init() {
	}
}
