package engine.audio;

import java.io.File;
import java.io.IOException;
 
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.FloatControl;

public class AudioUtilClip {
	public static final AudioUtilClip NULL_CLIP = new AudioUtilClip(null, null, 0.0f);
	private Clip clip;
	private AudioInputStream stream;
	private float volumeDB;
	private boolean needsNewVolume;
	private boolean isStarted;

	public AudioUtilClip(AudioInputStream audioStream, Clip audioClip, float volumeDB) {
		this.clip = audioClip;
		this.stream = audioStream;
		this.volumeDB = volumeDB;
		this.needsNewVolume = true;
		this.isStarted = false;
	}

	public void dispose() {
		try {
			if(clip != null) {
				clip.close();
			}
			if(stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			// do nothing
		}
	}

	public void setVolume(float newVolumeDB) {
		this.volumeDB = newVolumeDB;
		needsNewVolume = true;
	}

	public void pause() {
		if(clip == null) {
			return;
		}
		if(clip.isRunning()) {
			clip.stop();
		}
		isStarted = false;
	}

	public void stop() {
		if(clip == null) {
			return;
		}
		pause();
		clip.setFramePosition(0);
	}

	public void resume() {
		if(clip == null) {
			return;
		}
		if(needsNewVolume) {
			FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			volume.setValue(volumeDB);
			needsNewVolume = false;
		}
		if(!isStarted) {
			clip.start();
			isStarted = true;
		}
	}

	public void play() {
		if(clip == null) {
			return;
		}
		stop();
		resume();
	}
}
