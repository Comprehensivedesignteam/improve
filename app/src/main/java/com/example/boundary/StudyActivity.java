package com.example.boundary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.boundary.R;

public class StudyActivity extends Activity {
	private EditText et1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_study);

		et1 = (EditText) findViewById(R.id.editText1);
		ImageButton button2 = (ImageButton) findViewById(R.id.imageButton3);
		button2.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(StudyActivity.this,
						AudioReceiver.class);
				startActivity(intent);
			}
		});

		ImageButton button3 = (ImageButton) findViewById(R.id.imageButton1);
		button3.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(StudyActivity.this, shakehand.class);
				startActivity(intent);
			}
		});

		ImageButton button1 = (ImageButton) findViewById(R.id.imageButton2);
		button1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				String device = et1.getText().toString();

				double random[] = new double[41];

				if (device.length() == 0) {
					File file1 = new File(Environment
							.getExternalStorageDirectory().getAbsolutePath()
							+ "/reverseme" + "regID" + ".txt");
					if (!file1.exists()) {
						new AlertDialog.Builder(StudyActivity.this)
								.setTitle("标识符错误")
								.setMessage("请先进行握手或者在上方横线中输入对方ID")
								.setPositiveButton("确定", null).show();
						return;
					}
					double[] data = new double[1];
					duwenjian(data, file1);
					device = String.valueOf((int) data[0]);
				}

				File file = new File(Environment.getExternalStorageDirectory()
						.getAbsolutePath() + "/reverseme" + device + ".txt");

				if (file.exists()) {
					duwenjian(random, file);
				} else {
					random = randomcreate();
					FileWriter out = null;
					try {
						out = new FileWriter(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
					for (int i = 0; i < 41; i++) {
						try {
							out.write(random[i] + "\t");
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}

				AudioSpeaker as = AudioSpeaker.getAudioSpeaker();
				as.start(random);
			}
		});
	}

	double[] randomcreate() {
		double random[] = new double[41];
		double sum = 0;
		for (int i = 0; i < 41; ++i) {
			random[i] = Math.random();
			sum = sum + random[i];
		}

		for (int i = 0; i < 41; ++i) {
			random[i] = random[i] / sum;
		}
		return random;
	}

	void duwenjian(double[] data, File file) {
		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String line; // 一行数据

		// 逐行读取，并将每个数组放入到数组中
		try {
			while ((line = in.readLine()) != null) {
				String[] temp = line.split("\t");
				for (int j = 0; j < temp.length; j++) {
					data[j] = Double.parseDouble(temp[j]);
				}

			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
