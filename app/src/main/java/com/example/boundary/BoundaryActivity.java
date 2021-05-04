package com.example.boundary;

import android.os.Bundle;
import android.app.TabActivity;
import android.content.Intent;
import android.widget.TabHost;
import com.example.boundary.R;

@SuppressWarnings("deprecation")
public class BoundaryActivity extends TabActivity

{

	// 声明TabHost对象

	TabHost mTabHost;

	@Override
	public void onCreate(Bundle savedInstanceState)

	{

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_boundary);

		// 取得TabHost对象

		mTabHost = getTabHost();

		/* 为TabHost添加标签 */

		// 新建一个newTabSpec(newTabSpec)

		// 设置其标签和图标(setIndicator)

		// 设置内容(setContent)

		mTabHost.addTab(mTabHost.newTabSpec("tab_test1")

		.setIndicator("学习", getResources().getDrawable(R.drawable.color1))

		.setContent(new Intent(this, StudyActivity.class)));

		mTabHost.addTab(mTabHost.newTabSpec("tab_test2")

		.setIndicator("认证", getResources().getDrawable(R.drawable.color2))

		.setContent(new Intent(this, AuthenActivity.class)));

		// 设置TabHost的背景颜色

		// mTabHost.setBackgroundColor(Color.argb(150, 22, 70, 150));

		// 设置TabHost的背景图片资源

		// mTabHost.setBackgroundResource(R.drawable.background2);

	}

}
