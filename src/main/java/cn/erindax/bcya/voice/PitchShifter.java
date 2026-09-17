package cn.erindax.bcya.voice;

import java.util.Arrays;

public final class PitchShifter {

	private static final int WINDOW = 1024;
	private static final int CROSSFADE = 256;
	private static final int BUFFER_SIZE = 4096;
	private static final int BUFFER_MASK = BUFFER_SIZE - 1;
	private static final long RESET_GAP_NANOS = 200_000_000L;
	private static final double HALF_PI = Math.PI / 2.0;

	private final float[] buffer = new float[BUFFER_SIZE];
	private final double delayStep;

	private int writePos;
	private double delay;
	private long lastFrameNanos;

	public PitchShifter(double pitchRate) {
		if (!(pitchRate > 0.0 && pitchRate <= 2.0)) {
			throw new IllegalArgumentException("pitchRate must be in (0, 2], got " + pitchRate);
		}
		this.delayStep = 1.0 - pitchRate;
		reset();
	}

	public void process(short[] audio) {
		long now = System.nanoTime();
		if (now - lastFrameNanos > RESET_GAP_NANOS) {
			reset();
		}
		lastFrameNanos = now;

		for (int i = 0; i < audio.length; i++) {
			buffer[writePos] = audio[i];

			double fade = Math.min(delay / CROSSFADE, 1.0);
			float gainNear = (float) Math.sin(fade * HALF_PI);
			float gainFar = (float) Math.cos(fade * HALF_PI);

			float out = gainNear * readInterpolated(writePos - delay)
				+ gainFar * readInterpolated(writePos - delay - WINDOW);
			audio[i] = clampToShort(out);

			writePos = (writePos + 1) & BUFFER_MASK;
			delay += delayStep;
			if (delay >= WINDOW) {
				delay -= WINDOW;
			} else if (delay < 0.0) {
				delay += WINDOW;
			}
		}
	}

	public void reset() {
		Arrays.fill(buffer, 0.0f);
		writePos = 0;
		delay = CROSSFADE;
	}

	private float readInterpolated(double position) {
		int index = (int) Math.floor(position);
		float frac = (float) (position - index);
		float a = buffer[index & BUFFER_MASK];
		float b = buffer[(index + 1) & BUFFER_MASK];
		return a + (b - a) * frac;
	}

	private static short clampToShort(float value) {
		if (value >= Short.MAX_VALUE) {
			return Short.MAX_VALUE;
		}
		if (value <= Short.MIN_VALUE) {
			return Short.MIN_VALUE;
		}
		return (short) Math.round(value);
	}
}
