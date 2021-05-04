package com.example.boundary;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.boundary.R;

public class AuthenActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authen);
		ImageButton button1 = (ImageButton) findViewById(R.id.imageButton1);
		button1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(AuthenActivity.this, AudioReceiver2.class);
				startActivity(intent);
			}
		});

	}

}
