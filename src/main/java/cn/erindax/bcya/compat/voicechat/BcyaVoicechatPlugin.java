package cn.erindax.bcya.compat.voicechat;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.voice.MaskVoiceClientState;
import cn.erindax.bcya.voice.PitchShifter;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

public class BcyaVoicechatPlugin implements VoicechatPlugin {

	public static final double MASK_PITCH_RATE = 0.75;

	private final PitchShifter pitchShifter = new PitchShifter(MASK_PITCH_RATE);

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
		registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
	}

	private void onClientSound(ClientSoundEvent event) {
		if (!MaskVoiceClientState.shouldModifyLocalVoice()) {
			return;
		}
		short[] audio = event.getRawAudio();
		if (audio == null || audio.length == 0) {
			return;
		}
		pitchShifter.process(audio);
		event.setRawAudio(audio);
	}
}
