package cn.erindax.bcya.voice;

import java.util.UUID;
import java.util.regex.Pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record VoicePreset(String id, String name, float pitch, float robot, float radio, float grit, float echo,
		float volume) {

	public static final int MAX_NAME = 16;
	public static final float MIN_PITCH = 0.5F;
	public static final float MAX_PITCH = 2.0F;
	public static final float MIN_VOLUME = 0.5F;
	public static final float MAX_VOLUME = 2.0F;

	private static final int MAX_ID = 36;
	private static final Pattern ID = Pattern.compile("[0-9a-zA-Z-]{1,36}");
	private static final String DEFAULT_NAME = "尖细";

	public static final StreamCodec<FriendlyByteBuf, VoicePreset> STREAM_CODEC =
		StreamCodec.of(VoicePreset::write, VoicePreset::read);

	public static VoicePreset squeaky() {
		return new VoicePreset(newId(), DEFAULT_NAME, 1.4F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
	}

	public static VoicePreset blank(String name) {
		return new VoicePreset(newId(), name, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
	}

	public static String newId() {
		return UUID.randomUUID().toString();
	}

	public static boolean isValidId(String id) {
		return ID.matcher(id).matches();
	}

	public boolean changesVoice() {
		return Math.abs(pitch - 1.0F) >= 0.01F || robot > 0.0F || radio > 0.0F || grit > 0.0F || echo > 0.0F
			|| Math.abs(volume - 1.0F) >= 0.01F;
	}

	public VoicePreset sanitized(String fallbackName) {
		String cleanName = name.strip();
		if (cleanName.isEmpty()) {
			cleanName = fallbackName;
		}
		if (cleanName.length() > MAX_NAME) {
			cleanName = cleanName.substring(0, MAX_NAME);
		}
		return new VoicePreset(id, cleanName, clamp(pitch, MIN_PITCH, MAX_PITCH), clamp(robot, 0.0F, 1.0F),
			clamp(radio, 0.0F, 1.0F), clamp(grit, 0.0F, 1.0F), clamp(echo, 0.0F, 1.0F),
			clamp(volume, MIN_VOLUME, MAX_VOLUME));
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", id);
		tag.putString("Name", name);
		tag.putFloat("Pitch", pitch);
		tag.putFloat("Robot", robot);
		tag.putFloat("Radio", radio);
		tag.putFloat("Grit", grit);
		tag.putFloat("Echo", echo);
		tag.putFloat("Volume", volume);
		return tag;
	}

	public static VoicePreset load(CompoundTag tag) {
		String id = tag.getString("Id");
		return new VoicePreset(isValidId(id) ? id : newId(), tag.getString("Name"), tag.getFloat("Pitch"),
			tag.getFloat("Robot"), tag.getFloat("Radio"), tag.getFloat("Grit"), tag.getFloat("Echo"),
			tag.contains("Volume") ? tag.getFloat("Volume") : 1.0F).sanitized(DEFAULT_NAME);
	}

	private static float clamp(float value, float min, float max) {
		return Float.isFinite(value) ? Mth.clamp(value, min, max) : min;
	}

	private static void write(FriendlyByteBuf buf, VoicePreset preset) {
		buf.writeUtf(preset.id, MAX_ID);
		buf.writeUtf(preset.name, MAX_NAME * 4);
		buf.writeFloat(preset.pitch);
		buf.writeFloat(preset.robot);
		buf.writeFloat(preset.radio);
		buf.writeFloat(preset.grit);
		buf.writeFloat(preset.echo);
		buf.writeFloat(preset.volume);
	}

	private static VoicePreset read(FriendlyByteBuf buf) {
		return new VoicePreset(buf.readUtf(MAX_ID), buf.readUtf(MAX_NAME * 4), buf.readFloat(), buf.readFloat(),
			buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
	}
}
