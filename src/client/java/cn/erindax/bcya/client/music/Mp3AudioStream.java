package cn.erindax.bcya.client.music;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import net.minecraft.client.sounds.AudioStream;

import org.jetbrains.annotations.Nullable;

public class Mp3AudioStream implements AudioStream {

	private static final int MAX_FRAME_BYTES = 1152 * 2 * 2;

	private final Bitstream bitstream;
	private final Decoder decoder = new Decoder();
	private final AudioFormat format;
	@Nullable
	private Header pending;
	private boolean finished;

	public Mp3AudioStream(InputStream input) throws IOException {
		this.bitstream = new Bitstream(input);
		this.pending = readHeader();
		if (pending == null) {
			throw new IOException("MP3 stream contains no audio frames");
		}
		int channels = pending.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
		this.format = new AudioFormat(pending.frequency(), 16, channels, true,
			ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
	}

	@Nullable
	private Header readHeader() throws IOException {
		try {
			return bitstream.readFrame();
		} catch (BitstreamException e) {
			throw new IOException(e);
		}
	}

	@Override
	public AudioFormat getFormat() {
		return format;
	}

	@Override
	public ByteBuffer read(int size) throws IOException {
		ByteBuffer out = ByteBuffer.allocateDirect(size + MAX_FRAME_BYTES).order(ByteOrder.nativeOrder());
		while (!finished && out.position() < size) {
			Header header = pending != null ? pending : readHeader();
			pending = null;
			if (header == null) {
				finished = true;
				break;
			}
			SampleBuffer samples;
			try {
				samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
			} catch (DecoderException e) {
				throw new IOException(e);
			}
			short[] pcm = samples.getBuffer();
			int length = Math.min(samples.getBufferLength(), out.remaining() / 2);
			for (int i = 0; i < length; i++) {
				out.putShort(pcm[i]);
			}
			bitstream.closeFrame();
		}
		out.flip();
		return out;
	}

	@Override
	public void close() throws IOException {
		try {
			bitstream.close();
		} catch (BitstreamException e) {
			throw new IOException(e);
		}
	}
}
