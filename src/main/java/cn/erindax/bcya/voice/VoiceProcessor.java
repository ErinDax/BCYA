package cn.erindax.bcya.voice;

import org.jetbrains.annotations.Nullable;

public final class VoiceProcessor {

	private static final float SAMPLE_RATE = 48000.0F;
	private static final double TWO_PI = Math.PI * 2.0;
	private static final double ROBOT_HZ = 50.0;
	private static final float ROBOT_GAIN = 1.41F;
	private static final int ECHO_SAMPLES = (int) (SAMPLE_RATE * 0.18F);
	private static final float ECHO_FEEDBACK = 0.45F;
	private static final float ECHO_MIX = 0.6F;
	private static final float GRIT_DRIVE = 15.0F;
	private static final float GRIT_REFERENCE = 0.15F;
	private static final float RADIO_GAIN = 1.4F;

	private final VoicePreset preset;
	@Nullable
	private final PitchShifter shifter;
	@Nullable
	private final float[] echo;
	private final Biquad highPass = Biquad.highPass(450.0, 0.707);
	private final Biquad lowPass = Biquad.lowPass(2600.0, 0.707);
	private final double robotStep = TWO_PI * ROBOT_HZ / SAMPLE_RATE;
	private final float drive;
	private final float driveScale;
	private double robotPhase;
	private int echoPos;

	public VoiceProcessor(VoicePreset preset) {
		this.preset = preset;
		this.shifter = Math.abs(preset.pitch() - 1.0F) < 0.01F ? null : new PitchShifter(preset.pitch());
		this.echo = preset.echo() > 0.0F ? new float[ECHO_SAMPLES] : null;
		this.drive = 1.0F + preset.grit() * GRIT_DRIVE;
		this.driveScale = GRIT_REFERENCE / (float) Math.tanh(drive * GRIT_REFERENCE);
	}

	public VoicePreset preset() {
		return preset;
	}

	public void process(short[] audio) {
		if (shifter != null) {
			shifter.process(audio);
		}
		float robot = preset.robot();
		float grit = preset.grit();
		float radio = preset.radio();
		float echoAmount = preset.echo();
		float volume = preset.volume();
		for (int i = 0; i < audio.length; i++) {
			float x = audio[i] / 32768.0F;
			if (robot > 0.0F) {
				float carrier = (float) Math.sin(robotPhase);
				robotPhase += robotStep;
				if (robotPhase >= TWO_PI) {
					robotPhase -= TWO_PI;
				}
				x += (x * carrier * ROBOT_GAIN - x) * robot;
			}
			if (grit > 0.0F) {
				float wet = (float) Math.tanh(x * drive) * driveScale;
				x += (wet - x) * grit;
			}
			if (radio > 0.0F) {
				float wet = lowPass.process(highPass.process(x)) * RADIO_GAIN;
				x += (wet - x) * radio;
			}
			if (echo != null) {
				float delayed = echo[echoPos];
				echo[echoPos] = x + delayed * ECHO_FEEDBACK * echoAmount;
				echoPos = echoPos + 1 == echo.length ? 0 : echoPos + 1;
				x += delayed * ECHO_MIX * echoAmount;
			}
			audio[i] = toShort(x * volume * 32768.0F);
		}
	}

	private static short toShort(float value) {
		if (value >= Short.MAX_VALUE) {
			return Short.MAX_VALUE;
		}
		if (value <= Short.MIN_VALUE) {
			return Short.MIN_VALUE;
		}
		return (short) Math.round(value);
	}

	private static final class Biquad {
		private final float b0;
		private final float b1;
		private final float b2;
		private final float a1;
		private final float a2;
		private float x1;
		private float x2;
		private float y1;
		private float y2;

		private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
			this.b0 = (float) (b0 / a0);
			this.b1 = (float) (b1 / a0);
			this.b2 = (float) (b2 / a0);
			this.a1 = (float) (a1 / a0);
			this.a2 = (float) (a2 / a0);
		}

		private static Biquad lowPass(double frequency, double q) {
			double w = TWO_PI * frequency / SAMPLE_RATE;
			double alpha = Math.sin(w) / (2.0 * q);
			double cos = Math.cos(w);
			return new Biquad((1.0 - cos) / 2.0, 1.0 - cos, (1.0 - cos) / 2.0, 1.0 + alpha, -2.0 * cos, 1.0 - alpha);
		}

		private static Biquad highPass(double frequency, double q) {
			double w = TWO_PI * frequency / SAMPLE_RATE;
			double alpha = Math.sin(w) / (2.0 * q);
			double cos = Math.cos(w);
			return new Biquad((1.0 + cos) / 2.0, -(1.0 + cos), (1.0 + cos) / 2.0, 1.0 + alpha, -2.0 * cos, 1.0 - alpha);
		}

		private float process(float x) {
			float y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
			x2 = x1;
			x1 = x;
			y2 = y1;
			y1 = y;
			return y;
		}
	}
}
