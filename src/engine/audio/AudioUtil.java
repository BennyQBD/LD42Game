package engine.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.*;
 
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

public class AudioUtil {
	private static class BackgroundMusic extends Thread {
		private final File file;
		private boolean isRunning;
		public BackgroundMusic(File file) {
			this.file = file;
			isRunning = true;
		}
		private AudioFormat getOutFormat(AudioFormat inFormat) {
			final int ch = inFormat.getChannels();
			final float rate = inFormat.getSampleRate();
			return new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
		}
	 
		private void stream(AudioInputStream in, SourceDataLine line) 
			throws IOException {
			final byte[] buffer = new byte[65536];
			for (int n = 0; n != -1; n = in.read(buffer, 0, buffer.length)) {
				if(!isRunning) {
					break;
				}
				line.write(buffer, 0, n);
			}
		}

		public void stopPlaying() {
			isRunning = false;
		}
		@Override
		public void run() {
			while(isRunning) {
				try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
					final AudioFormat outFormat = getOutFormat(in.getFormat());
					final Info info = new Info(SourceDataLine.class, outFormat);
		 
					try (final SourceDataLine line =
							 (SourceDataLine) AudioSystem.getLine(info)) {
						if (line != null) {
//							in.mark(Integer.MAX_VALUE);
							line.open(outFormat, 2048);
							line.start();
//							while(isRunning) { // More efficient way to repeat if markers are supported
								stream(AudioSystem.getAudioInputStream(outFormat, in), line);
								line.drain();
//								in.reset();
//							}
							line.stop();
						}
					}
				} catch (UnsupportedAudioFileException 
					   | LineUnavailableException 
					   | IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

	private static BackgroundMusic backgroundMusic = null;
	public static void playBackgroundMusic(String fileName) {
		stopBackgroundMusic();
		backgroundMusic = new BackgroundMusic(new File(fileName));
		try {
			backgroundMusic.start();
		} catch(Exception e) {
			// If it doesn't work, just play nothing
		}
	}

	public static void stopBackgroundMusic() {
		if(backgroundMusic != null) {
			backgroundMusic.stopPlaying();
		}
	}

	public static AudioUtilClip loadClip(String fileName) {
		return loadClip(fileName, -6.0f);
	}
	public static AudioUtilClip loadClip(String fileName, float volumeDB) {
		try {
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(fileName));
			AudioFormat format = audioStream.getFormat();
			Info info = new Info(Clip.class, format);
			Clip audioClip = (Clip) AudioSystem.getLine(info);
			audioClip.open(audioStream);
			return new AudioUtilClip(audioStream, audioClip, volumeDB);
		} catch(Exception e) {
			return AudioUtilClip.NULL_CLIP;
		}
	}
}
