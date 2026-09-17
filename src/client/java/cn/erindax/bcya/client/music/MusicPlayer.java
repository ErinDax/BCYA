package cn.erindax.bcya.client.music;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.client.mixin.SoundEngineAccessor;
import cn.erindax.bcya.client.mixin.SoundManagerAccessor;
import cn.erindax.bcya.music.net.MusicControlPayload;

import com.mojang.blaze3d.audio.Channel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.Nullable;

public final class MusicPlayer {

	private static final String SOUND_PREFIX = "sounds/" + MusicSoundInstance.PATH_PREFIX;
	private static final String SOUND_SUFFIX = ".ogg";

	private static final Map<UUID, MusicSoundInstance> PLAYING = new ConcurrentHashMap<>();
	private static final Map<UUID, MusicControlPayload> WAITING = new HashMap<>();

	private MusicPlayer() {
	}

	public static void handle(MusicControlPayload payload) {
		switch (payload.action()) {
			case PLAY -> play(payload);
			case PAUSE -> withChannel(payload.session(), Channel::pause);
			case RESUME -> withChannel(payload.session(), Channel::unpause);
			case STOP -> stop(payload.session());
		}
	}

	private static void play(MusicControlPayload payload) {
		stop(payload.session());
		if (ClientMusic.has(payload.track(), payload.version())) {
			start(payload);
		} else {
			WAITING.put(payload.session(), payload);
			ClientMusic.request(payload.track());
		}
	}

	static void onTrackReady(String track) {
		List<MusicControlPayload> ready = new ArrayList<>();
		Iterator<MusicControlPayload> it = WAITING.values().iterator();
		while (it.hasNext()) {
			MusicControlPayload payload = it.next();
			if (payload.track().equals(track)) {
				ready.add(payload);
				it.remove();
			}
		}
		ready.forEach(MusicPlayer::start);
	}

	private static void start(MusicControlPayload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}
		Entity entity = null;
		if (payload.followsEntity()) {
			entity = level.getEntity(payload.entityId());
			if (entity == null) {
				return;
			}
		}
		MusicSoundInstance instance = new MusicSoundInstance(payload.session(), payload.track(), payload.range(),
			entity, payload.pos());
		PLAYING.put(payload.session(), instance);
		minecraft.getSoundManager().play(instance);
	}

	public static void stop(UUID session) {
		WAITING.remove(session);
		MusicSoundInstance instance = PLAYING.remove(session);
		if (instance != null) {
			Minecraft.getInstance().getSoundManager().stop(instance);
		}
	}

	public static void clear() {
		WAITING.clear();
		SoundManager manager = Minecraft.getInstance().getSoundManager();
		for (MusicSoundInstance instance : PLAYING.values()) {
			manager.stop(instance);
		}
		PLAYING.clear();
	}

	static boolean isTrackInUse(String track) {
		for (MusicSoundInstance instance : PLAYING.values()) {
			if (instance.track().equals(track)) {
				return true;
			}
		}
		for (MusicControlPayload payload : WAITING.values()) {
			if (payload.track().equals(track)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public static CompletableFuture<AudioStream> openStream(ResourceLocation location) {
		if (!location.getNamespace().equals(BcyaMod.MOD_ID)) {
			return null;
		}
		String path = location.getPath();
		if (!path.startsWith(SOUND_PREFIX) || !path.endsWith(SOUND_SUFFIX)) {
			return null;
		}
		UUID session;
		try {
			session = UUID.fromString(path.substring(SOUND_PREFIX.length(), path.length() - SOUND_SUFFIX.length()));
		} catch (IllegalArgumentException e) {
			return null;
		}
		MusicSoundInstance instance = PLAYING.get(session);
		byte[] data = instance == null ? null : ClientMusic.get(instance.track());
		if (data == null) {
			return CompletableFuture.failedFuture(new IOException("Music data for session " + session + " is unavailable"));
		}
		String track = instance.track();
		return CompletableFuture.supplyAsync(() -> {
			try {
				return open(track, data);
			} catch (IOException e) {
				throw new CompletionException(e);
			}
		}, Util.backgroundExecutor());
	}

	private static AudioStream open(String track, byte[] data) throws IOException {
		if (track.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
			return new Mp3AudioStream(new ByteArrayInputStream(data));
		}
		return new JOrbisAudioStream(new ByteArrayInputStream(data));
	}

	private static void withChannel(UUID session, Consumer<Channel> action) {
		MusicSoundInstance instance = PLAYING.get(session);
		if (instance == null) {
			return;
		}
		SoundEngine engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).bcya$getSoundEngine();
		ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) engine).bcya$getInstanceToChannel().get(instance);
		if (handle != null) {
			handle.execute(action);
		}
	}
}
