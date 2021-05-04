package com.example.boundary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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

public class AudioReceiver2 extends Activity {

	private boolean isRecording = false;
	private TextView bd;// 最大振幅
	private int sampleRateInHz = 44100;
	private int times = 0;
	//private EditText et;
	private int max;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audioreceiver2);

		bd = (TextView) findViewById(R.id.textView1);
		bd.setMovementMethod(new ScrollingMovementMethod());
		//et = (EditText) findViewById(R.id.editText1);
		bd.setText("点击Record Start开始录音\n点击Rocord Stop停止录音\n录音结束后点击分析按钮判断认证是否通过\nPlay Record按钮可以播放录音来检查录音是否成功");

		// 分析按钮
		Button ana = (Button) findViewById(R.id.ana_button);
		ana.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				double[] px = new double[41];
				px = analyze();

				//max = Integer.parseInt(et.getText().toString());
				max=0;
				boolean flag;
				do {
					flag=false;
					max++;
					File file = new File(Environment.getExternalStorageDirectory()
							.getAbsolutePath() + "/reverseme" + "_"+max + ".pcm");
					if (file.exists())
						flag=true;
				}while (flag);
				max--;
				double[][] pxpx = new double[max][41];

				for (int i = 0; i < max; i++) {
					duwenjian(pxpx[i], i + 1);
				}

				// 算出这次距离哪个点最近
				int n = 0;
				n = func6(px, pxpx, max);

				double var = 0;
				double daikuan = 10.8;

				var = func4(px, pxpx[n], daikuan);

				String s = "";
				if (var > 90.0)
					s = s + "认证失败" + "\n" + String.valueOf(var);
				else
					s = s + "认证成功" + "\n" + String.valueOf(var);
				if (var <= 90.0){
					File file1 = new File(Environment.getExternalStorageDirectory()
							.getAbsolutePath() + "/info" + n + ".txt");
					try{
						BufferedReader br = new BufferedReader(new FileReader(file1));//构造一个BufferedReader类来读取文件
						// 使用readLine方法，一次读一行
						s+="\n"+"学生姓名为："+br.readLine();
						s+="\n"+"学号为："+br.readLine();
						s+="\n"+"手机号为："+br.readLine();
						br.close();
					}catch(Exception e){
						e.printStackTrace();
					}
				}

				bd.setText(s);

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

	// 认证阶段的分析 这边和学习的一样都是返回各个频率的频响 具体看上面的分析按钮
	public double[] analyze() {
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/reverseme"
				+ String.valueOf(times)
				+ ".pcm");

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
						;
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
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/reverseme"
				+ String.valueOf(times)
				+ ".pcm");// 获得pcm文件
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
		times++;

		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/reverseme"
				+ String.valueOf(times)
				+ ".pcm");

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

	// 求累积和
	double func4(double[] data1, double[] data2, double daikuan) {

		double width = daikuan;

		double sum = 0;

		for (int i = 0; i < data1.length; i++) {
			if (Math.abs(data1[i] - data2[i]) < width) {
				sum = sum + 0;
			} else {
				sum = sum + Math.abs(data1[i] - data2[i]);
			}

		}

		return sum;

	}

	// //算出这次距离哪个点最近
	int func6(double[] px, double[][] pxpx, int n) {

		double[] temp = new double[41];

		for (int i = 0; i < 41; i++) {
			temp[i] = 1000;
		}
		int[] count = new int[41];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < 41; j++) {
				if (temp[j] > Math.abs(px[j] - pxpx[i][j])) {
					temp[j] = Math.abs(px[j] - pxpx[i][j]);
					count[j] = i;
				}

			}
		}

		int[] ans = new int[n];
		for (int i = 0; i < 41; i++) {
			ans[count[i]]++;
		}

		int max = 0;
		int ret = 0;
		for (int i = 0; i < n; i++) {
			if (max < ans[i]) {
				max = ans[i];
				ret = i;
			}
		}
		return ret;
	}

	void duwenjian(double[] px2, int n) {

		// 设读到的数据为px2[]
		String name = "";
		name = "_" + String.valueOf(n);

		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/reverseme" + name + ".txt");
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //
		String line; // 一行数据

		// 逐行读取，并将每个数组放入到数组中
		try {
			while ((line = in.readLine()) != null) {
				String[] temp = line.split("\t");
				for (int j = 0; j < temp.length; j++) {
					px2[j] = Double.parseDouble(temp[j]);
				}

			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
