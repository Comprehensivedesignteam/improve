package com.example.boundary;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioSpeaker {

	private static AudioSpeaker as;

	private AudioSpeaker() {

	}

	public static AudioSpeaker getAudioSpeaker() {
		if (as == null) {
			as = new AudioSpeaker();
		}
		return as;
	}

	private static AudioTrack at = null;

	private int Duration = 2;
	private int sampleRateInHz = 44100;

	void start(double random[]) {
		SinGenerator sg = new SinGenerator();// 正弦波产生器
		int buffersize = Duration * sampleRateInHz + 1;

		if (at == null) {
			at = new AudioTrack(
					AudioManager.STREAM_MUSIC,// 流类型
					sampleRateInHz,// sampleRateInHz采样频率
					// AudioFormat.CHANNEL_CONFIGURATION_MONO,//声道数，此为单声道
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,// 音频采样深度8bit或者16bit
					buffersize * 2,// 播放文件长度
					AudioTrack.MODE_STREAM);// 模式，使用流模式
		}

		short[] buffer = new short[buffersize];
		sg.gen(buffer, random);
		at.play();
		at.write(buffer, 0, buffersize);// 从文件流中读音频信息来播放
		at.stop();

	}

}
