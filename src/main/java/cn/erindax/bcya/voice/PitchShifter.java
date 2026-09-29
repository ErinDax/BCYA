package cn.erindax.bcya.voice;

import java.util.Arrays;

public final class PitchShifter {

	private static final int BUFFER_SIZE = 4096;
	private static final int BUFFER_MASK = BUFFER_SIZE - 1;
	private static final int WINDOW = 512;
	private static final int CROSSFADE = 512;
	private static final int JUMP_MIN = 480;
	private static final int JUMP_MAX = JUMP_MIN + 800;
	private static final int MARGIN = 4;
	private static final int LAG_MAX = JUMP_MAX + WINDOW + MARGIN;
	private static final int COARSE = 2;
	private static final long RESET_GAP_NANOS = 200_000_000L;
	private static final float[] FADE = new float[CROSSFADE];

	static {
		for (int i = 0; i < CROSSFADE; i++) {
			FADE[i] = (float) (0.5 - 0.5 * Math.cos(Math.PI * (i + 0.5) / CROSSFADE));
		}
	}

	private final float[] buffer = new float[BUFFER_SIZE];
	private final double rate;

	private long writePos;
	private double readPos;
	private double fadePos;
	private int fadeLeft;
	private float fadeCorrelation;
	private long lastFrameNanos;

	public PitchShifter(double pitchRate) {
		if (!(pitchRate > 0.0 && pitchRate <= 1.0)) {
			throw new IllegalArgumentException("pitchRate must be in (0, 1], got " + pitchRate);
		}
		this.rate = pitchRate;
		reset();
	}

	public void process(short[] audio) {
		long now = System.nanoTime();
		if (now - lastFrameNanos > RESET_GAP_NANOS) {
			reset();
		}
		lastFrameNanos = now;

		for (int i = 0; i < audio.length; i++) {
			buffer[(int) (writePos & BUFFER_MASK)] = audio[i];
			writePos++;
			if (fadeLeft == 0 && writePos - readPos >= LAG_MAX) {
				splice();
			}
			float out = read(readPos);
			readPos += rate;
			if (fadeLeft > 0) {
				float fresh = FADE[CROSSFADE - fadeLeft];
				float stale = 1.0F - fresh;
				float power = stale * stale + fresh * fresh + 2.0F * stale * fresh * fadeCorrelation;
				out = (stale * out + fresh * read(fadePos)) / (float) Math.sqrt(power);
				fadePos += rate;
				if (--fadeLeft == 0) {
					readPos = fadePos;
				}
			}
			audio[i] = clampToShort(out);
		}
	}

	public void reset() {
		Arrays.fill(buffer, 0.0F);
		writePos = 0L;
		readPos = -MARGIN;
		fadeLeft = 0;
	}

	private void splice() {
		long base = (long) Math.floor(readPos);
		int limit = (int) Math.min(JUMP_MAX, writePos - base - WINDOW - MARGIN);
		if (limit < JUMP_MIN) {
			return;
		}
		int best = JUMP_MIN;
		double bestScore = Double.NEGATIVE_INFINITY;
		double energy = dot(base, base, COARSE);
		for (int jump = JUMP_MIN; jump <= limit; jump += COARSE) {
			double score = correlation(base, jump, COARSE, energy);
			if (score > bestScore) {
				bestScore = score;
				best = jump;
			}
		}
		int center = best;
		bestScore = Double.NEGATIVE_INFINITY;
		energy = dot(base, base, 1);
		for (int jump = Math.max(JUMP_MIN, center - COARSE + 1); jump <= Math.min(limit, center + COARSE - 1); jump++) {
			double score = correlation(base, jump, 1, energy);
			if (score > bestScore) {
				bestScore = score;
				best = jump;
			}
		}
		fadePos = readPos + best;
		fadeLeft = CROSSFADE;
		fadeCorrelation = (float) Math.max(0.0, Math.min(1.0, bestScore));
	}

	private double correlation(long base, int jump, int step, double energy) {
		double other = dot(base + jump, base + jump, step);
		return dot(base, base + jump, step) / Math.sqrt(energy * other + 1.0);
	}

	private double dot(long a, long b, int step) {
		double sum = 0.0;
		for (int i = 0; i < WINDOW; i += step) {
			sum += (double) buffer[(int) ((a + i) & BUFFER_MASK)] * buffer[(int) ((b + i) & BUFFER_MASK)];
		}
		return sum;
	}

	private float read(double position) {
		long index = (long) Math.floor(position);
		float t = (float) (position - index);
		float y0 = buffer[(int) ((index - 1) & BUFFER_MASK)];
		float y1 = buffer[(int) (index & BUFFER_MASK)];
		float y2 = buffer[(int) ((index + 1) & BUFFER_MASK)];
		float y3 = buffer[(int) ((index + 2) & BUFFER_MASK)];
		float c1 = 0.5F * (y2 - y0);
		float c2 = y0 - 2.5F * y1 + 2.0F * y2 - 0.5F * y3;
		float c3 = 0.5F * (y3 - y0) + 1.5F * (y1 - y2);
		return ((c3 * t + c2) * t + c1) * t + y1;
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
