package cn.erindax.bcya.compat.voicechat;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.voice.MaskVoiceClientState;
import cn.erindax.bcya.voice.VoicePreset;
import cn.erindax.bcya.voice.VoiceProcessor;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BcyaVoicechatPlugin implements VoicechatPlugin {

	private final Map<UUID, VoiceProcessor> processors = new ConcurrentHashMap<>();

	@Override
	public String getPluginId() {
		return BcyaMod.MOD_ID;
	}

	@Override
	public void initialize(VoicechatApi api) {
		BcyaMod.LOGGER.info("Simple Voice Chat detected, mask voice changer enabled.");
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, this::onReceive);
		registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, this::onReceive);
	}

	private void onReceive(ClientReceiveSoundEvent event) {
		UUID speaker = event.getId();
		if (speaker == null) {
			return;
		}
		short[] audio = event.getRawAudio();
		if (audio == null || audio.length == 0) {
			processors.remove(speaker);
			return;
		}
		VoicePreset preset = MaskVoiceClientState.presetFor(speaker);
		if (preset == null || !preset.changesVoice()) {
			processors.remove(speaker);
			return;
		}
		VoiceProcessor processor = processors.get(speaker);
		if (processor == null || !processor.preset().equals(preset)) {
			processor = new VoiceProcessor(preset);
			processors.put(speaker, processor);
		}
		processor.process(audio);
		event.setRawAudio(audio);
	}
}
