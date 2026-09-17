package cn.erindax.bcya.client.music;

import cn.erindax.bcya.BcyaMod;

import java.util.UUID;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.Nullable;

public class MusicSoundInstance extends AbstractTickableSoundInstance {

	public static final String PATH_PREFIX = "music/";

	private final String track;
	private final int range;
	@Nullable
	private final Entity entity;

	public MusicSoundInstance(UUID session, String track, int range, @Nullable Entity entity, BlockPos pos) {
		super(SoundEvent.createVariableRangeEvent(BcyaMod.id(PATH_PREFIX + session)), SoundSource.RECORDS,
			SoundInstance.createUnseededRandom());
		this.track = track;
		this.range = range;
		this.entity = entity;
		this.volume = 1.0F;
		this.pitch = 1.0F;
		this.looping = false;
		this.relative = false;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		if (entity != null) {
			follow();
		} else {
			this.x = pos.getX() + 0.5;
			this.y = pos.getY() + 0.5;
			this.z = pos.getZ() + 0.5;
		}
	}

	public String track() {
		return track;
	}

	@Override
	public WeighedSoundEvents resolve(SoundManager manager) {
		this.sound = new Sound(this.location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE,
			true, false, range);
		return new WeighedSoundEvents(this.location, null);
	}

	@Override
	public void tick() {
		if (entity == null) {
			return;
		}
		if (entity.isRemoved()) {
			stop();
			return;
		}
		follow();
	}

	private void follow() {
		this.x = entity.getX();
		this.y = entity.getEyeY();
		this.z = entity.getZ();
	}
}
