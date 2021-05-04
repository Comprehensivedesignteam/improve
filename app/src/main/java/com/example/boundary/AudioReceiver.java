package com.example.boundary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import ca.uol.aig.fftpack.RealDoubleFFT;
import com.example.boundary.R;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AudioReceiver extends Activity {

	private boolean isRecording = false;
	private TextView bd;
	private int sampleRateInHz = 44100;
	private int cishu = 0;
	private EditText etna;
	private EditText etid;
	private EditText etph;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		boolean flag;
		do {
			flag=false;
			cishu++;
			File file = new File(Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/reverseme" + "_"+cishu + ".pcm");
			if (file.exists())
				flag=true;
		}while (flag);
		cishu--;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audioreceiver);

		bd = (TextView) findViewById(R.id.textView1);
		bd.setMovementMethod(new ScrollingMovementMethod());
		bd.setText("点击Record Start开始录音\n点击Rocord Stop停止录音\n录音结束后点击分析按钮提取音频指纹并录入指纹库\nPlay Record按钮可以播放录音来检查录音是否成功");
		// 分析按钮
		Button ana = (Button) findViewById(R.id.ana_button);
		ana.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				double[] px = new double[41];
				px = analyze();
				String s = "";
				s = s + "第" + String.valueOf(cishu) + "次学习完成";
				bd.setText(s);

				// 保存到文件
				String ss = "";
				ss = "_" + String.valueOf(cishu);

				File file = new File(Environment.getExternalStorageDirectory()
						.getAbsolutePath() + "/reverseme" + ss + ".txt");
				FileWriter out = null;
				try {
					out = new FileWriter(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // 文件写入流
				File file1 = new File(Environment.getExternalStorageDirectory()
						.getAbsolutePath() + "/info" + cishu + ".txt");
				FileWriter fw = null;
				try {
					fw = new FileWriter(file1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // 文件写入流

				for (int i = 0; i < 41; i++) {

					try {
						out.write(px[i] + "\t");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				etna=(EditText) findViewById(R.id.editName);
				etid=(EditText) findViewById(R.id.editNum);
				etph=(EditText) findViewById(R.id.editPhone);
				String name=etna.getText().toString();
				String id=etid.getText().toString();
				String ph=etph.getText().toString();
				try{
					fw.write(name+"\n"+id+"\n"+ph);
					fw.close();
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		// 录音开始按钮
		Button start = (Button) findViewById(R.id.start_bt);
		start.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {

				// TODO Auto-generated method stub
				Thread thread = new Thread(new Runnable() {
					public void run() {
						record();

					}
				});
				thread.start();

				findViewById(R.id.start_bt).setEnabled(false);
				findViewById(R.id.end_bt).setEnabled(true);
			}
		});

		// 播放按钮
		Button play = (Button) findViewById(R.id.play_bt);
		play.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				play();
			}
		});

		// 停止按钮
		Button stop = (Button) findViewById(R.id.end_bt);
		stop.setEnabled(false);
		stop.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				isRecording = false;
				findViewById(R.id.start_bt).setEnabled(true);
				findViewById(R.id.end_bt).setEnabled(false);
			}
		});
	}

	// 学习部分的分析
	public double[] analyze() {
		String s = "";
		s = "_" + String.valueOf(cishu);
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/reverseme" + s + ".pcm");

		double[] temp = new double[44100 * 10];
		int i = 0;
		try {
			InputStream is1 = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(is1);
			DataInputStream dis = new DataInputStream(bis);

			try {
				while (dis.available() > 0 && i < 44100 * 10) {
					try {

						temp[i] = ((double) dis.readShort());
						i++;

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double[] data = new double[i];
		for (int j = 0; j < i; j++)
			data[j] = temp[j];

		double[] px = new double[41];
		px = func1(data);

		return px;
	}

	// 播放函数
	@SuppressWarnings("deprecation")
	public void play() {
		// Get the file we want to playback.
		String s = "";
		s = "_" + String.valueOf(cishu);
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/reverseme" + s + ".pcm");// 获得pcm文件
		// Get the length of the audio stored in the file (16 bit so 2 bytes per
		// short)
		// and create a short array to store the recorded audio.
		int musicLength = (int) (file.length() / 2);// short是2byte，数组大小 = 文件长度/2
		short[] music = new short[musicLength];
		try {
			// Create a DataInputStream to read the audio data back from the
			// saved file.
			InputStream is = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(is);
			DataInputStream dis = new DataInputStream(bis);
			// Read the file into the music array.
			int i = 0;
			while (dis.available() > 0) {// 将文件中的数据读入到内存冲
				music[i] = dis.readShort();
				i++;
			}
			// Close the input streams.
			dis.close();
			// Create a new AudioTrack object using the same parameters as the
			// AudioRecord
			// object used to create the file.
			// 设置播放
			AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,// 流类型
					sampleRateInHz,// sampleRateInHz采样频率
					AudioFormat.CHANNEL_CONFIGURATION_MONO,// 声道数，此为单声道
					AudioFormat.ENCODING_PCM_16BIT,// 音频采样深度8bit或者16bit
					musicLength * 2,// 播放文件长度
					AudioTrack.MODE_STREAM);// 模式，使用流模式
			// Start playback
			audioTrack.play();// 开始播放
			// Write the music buffer to the AudioTrack object
			audioTrack.write(music, 0, musicLength);// 从文件流中读音频信息来播放
			audioTrack.stop();
		} catch (Throwable t) {
			Log.e("AudioTrack", "Playback Failed");
		}
	}

	@SuppressWarnings("deprecation")
	void record() {
		int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

		cishu++;

		String s = "";
		s = "_" + String.valueOf(cishu);
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/reverseme" + s + ".pcm");

		// Delete any previous recording.
		if (file.exists())
			file.delete();
		// Create the new file.
		try {
			file.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create "
					+ file.toString());
		}
		try {
			// Create a DataOuputStream to write the audio data into the saved
			// file.
			OutputStream os = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(os);
			DataOutputStream dos = new DataOutputStream(bos);
			// Create a new AudioRecord object to record the audio.
			int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,
					channelConfiguration, audioEncoding);
			AudioRecord audioRecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC,// 输入源
					sampleRateInHz, // 采样频率
					channelConfiguration,// 单声道还是多声道
					audioEncoding, // 8bit还是16bit编码
					bufferSize);// 内存大小
			short[] buffer = new short[bufferSize];
			audioRecord.startRecording();// 开始录制
			isRecording = true;// 录制状态
			while (isRecording) {
				int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);// 将录制的内容读入到内存中
				for (int i = 0; i < bufferReadResult; i++) {// 将读入内存的音频信息写入文件
					dos.writeShort(buffer[i]);
				}
			}
			audioRecord.stop();
			dos.close();

		} catch (Throwable t) {
			Log.e("AudioRecord", "Recording Failed");
		}
	}

	// 得到频响
	double[] func1(double[] data) {
		int n = data.length;
		int fs = 44100;

		RealDoubleFFT realdoublefft = new RealDoubleFFT(n);

		realdoublefft.ft(data);

		double[] f = new double[n];
		for (int i = 0; i < n; i++)
			f[i] = i * (double) fs / n;
		double[] px = new double[41];
		int k = 0;
		for (int j = 4000; j <= 20000; j = j + 400) {
			for (int i = 0; i < n; i++) {
				if (Math.abs(f[i] / 2 - j) < 5 && px[k] < Math.abs(data[i])) {
					px[k] = Math.abs(data[i]);
				}
			}
			k++;
		}
		for (int i = 0; i < 41; i++) {
			px[i] = 20 * Math.log10(px[i] * 2 / n);
		}
		return px;

	}

}
