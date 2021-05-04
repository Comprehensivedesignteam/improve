package com.example.boundary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.example.boundary.R;
import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoicePlayer;
import com.libra.sinvoice.SinVoiceRecognition;

public class shakehand extends Activity implements
		SinVoiceRecognition.Listener, SinVoicePlayer.Listener {
	private final static String TAG = "MainActivity";
	private final static int MSG_SET_RECG_TEXT = 1;
	private final static int MSG_RECG_START = 2;
	private final static int MSG_RECG_END = 3;
	private TextView bd;
	private EditText et1;
	private final static String CODEBOOK = "12345";

	private Handler mHanlder;// handler用于计算进程将计算结果更新到主界面
	private SinVoicePlayer mSinVoicePlayer;// 声波信号发送对象
	private SinVoiceRecognition mRecognition;// 声波信号分析对象

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et1 = (EditText) findViewById(R.id.editText1);
		bd = (TextView) findViewById(R.id.textView8);
		bd.setMovementMethod(new ScrollingMovementMethod());
		bd.setText("请输入你的ID");
		mSinVoicePlayer = new SinVoicePlayer(CODEBOOK);
		mSinVoicePlayer.setListener(this);

		mRecognition = new SinVoiceRecognition(CODEBOOK);
		mRecognition.setListener(this);

		final TextView playTextView = (TextView) findViewById(R.id.playtext);
		final TextView recognisedTextView = (TextView) findViewById(R.id.regtext);
		mHanlder = new RegHandler(recognisedTextView);

		Button playStart = (Button) this.findViewById(R.id.start_play);
		playStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String text =et1.getText().toString();
				String tmp="";
				if(text.length()<5)
					text="12345";
				else {
					for(int i=0;i<5;i++){
						char a=text.charAt(i);
						if(a>'5')
							a=(char)(a-'5'+'0');
						tmp+=a;
					}text=tmp;}
				playTextView.setText(text);
				mSinVoicePlayer.play(text, true, 1000);
			}
		});

		Button playStop = (Button) this.findViewById(R.id.stop_play);
		playStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mSinVoicePlayer.stop();
			}
		});

		Button recognitionStart = (Button) this.findViewById(R.id.start_reg);
		recognitionStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mRecognition.start();
			}
		});

		Button recognitionStop = (Button) this.findViewById(R.id.stop_reg);
		recognitionStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mRecognition.stop();
				String id = "";
				id = recognisedTextView.getText().toString();
				File file = new File(Environment.getExternalStorageDirectory()
						.getAbsolutePath() + "/reverseme" + "regID" + ".txt");
				writefile(file, id);
			}
		});

	}

	void writefile(File file, String s) {

		FileWriter out = null;

		try {
			out = new FileWriter(file,true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			out.write(s+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// 产生count个数值，作为带传输数据，待编码
	private String genText(int count) {

		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String str_id = tm.getDeviceId();
		int id = Math.abs(str_id.hashCode());

		StringBuilder sb = new StringBuilder();

		int pre = 0;
		while (count > 0) {
			int x = (id % 5) + 1;
			id = id / 10;
			if (Math.abs(x - pre) > 1) {
				sb.append(x);
				--count;
				pre = x;
			} else if (x <= pre) {
				x = (x + 2) % 5 + 1;
				sb.append(x);
				--count;
				pre = x;
			} else if (x > pre) {
				x = (x + 1) % 5 + 1;
				sb.append(x);
				--count;
				pre = x;
			}
		}
		return sb.toString();
	}

	private static class RegHandler extends Handler {
		private StringBuilder mTextBuilder = new StringBuilder();
		private TextView mRecognisedTextView;// 写解码数值的控件

		public RegHandler(TextView textView) {
			mRecognisedTextView = textView;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_SET_RECG_TEXT:// 设置解码的数值
					char ch = (char) msg.arg1;
					mTextBuilder.append(ch);// 将解码数值添加到控件中
					if (null != mRecognisedTextView) {
						mRecognisedTextView.setText(mTextBuilder.toString());
					}
					break;

				case MSG_RECG_START:
					mTextBuilder.delete(0, mTextBuilder.length());
					break;

				case MSG_RECG_END:
					LogHelper.d(TAG, "recognition end");
					break;
			}
			super.handleMessage(msg);
		}
	}

	@Override
	public void onRecognitionStart() {
		mHanlder.sendEmptyMessage(MSG_RECG_START);
	}

	@Override
	public void onRecognition(char ch) {// 将解码后更新到界面中
		mHanlder.sendMessage(mHanlder.obtainMessage(MSG_SET_RECG_TEXT, ch, 0));
	}

	@Override
	public void onRecognitionEnd() {
		mHanlder.sendEmptyMessage(MSG_RECG_END);
	}

	@Override
	public void onPlayStart() {
		LogHelper.d(TAG, "start play");
	}

	@Override
	public void onPlayEnd() {
		LogHelper.d(TAG, "stop play");
	}
}
