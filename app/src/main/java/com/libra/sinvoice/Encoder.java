/*
 * Copyright (C) 2013 gujicheng
 * 
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 * 
 * If you have any question, please contact me.
 * 
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 */
package com.libra.sinvoice;

import java.util.List;

import com.libra.sinvoice.Buffer.BufferData;

public class Encoder implements SinGenerator.Listener, SinGenerator.Callback {
	private final static String TAG = "Encoder";
	private final static int STATE_ENCODING = 1;// 编码状态
	private final static int STATE_STOPED = 2;// 停止状态

	// index 0, 1, 2, 3, 4, 5, 6
	// sampling point Count 31, 28, 25, 22, 19, 15, 10
	// 注意31*1422=28*1575=25*1764 = 44100
	private final static int[] CODE_FREQUENCY = { 1422, 1575, 1764, 2004, 2321,
			2940, 4410 };
	/*
	 * 编码频率 index 对应频率 0： 1422 1： 1575 2： 1764 ....
	 * 数值序列text->从codebook找到index并存到code数组中->code数组对应CODE_FREQUENCY查找出频率
	 */
	private int mState;// 当前状态

	private SinGenerator mSinGenerator;// 正弦产生
	private Listener mListener;
	private Callback mCallback;//

	public static interface Listener {// Listener接口
		void onStartEncode();

		void onEndEncode();
	}

	public static interface Callback {// Callback接口
		void freeEncodeBuffer(BufferData buffer);

		BufferData getEncodeBuffer();
	}

	// 构造函数，
	public Encoder(Callback callback, int sampleRate, int bits, int bufferSize) {
		mCallback = callback;// 获得回调
		mState = STATE_STOPED;// 停止状态
		mSinGenerator = new SinGenerator(this, sampleRate, bits, bufferSize);
		mSinGenerator.setListener(this);
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	// 获得最大编码数
	public final static int getMaxCodeCount() {
		return CODE_FREQUENCY.length;
	}

	public final boolean isStoped() {
		return (STATE_STOPED == mState);
	}

	// content of input from 0 to (CODE_FREQUENCY.length-1)
	public void encode(List<Integer> codes, int duration) {
		encode(codes, duration, 0);
	}

	/*
	 * 编码函数 codes:编码数组 duration：播放时长 muteInterval:时间间隔
	 */
	public void encode(List<Integer> codes, int duration, int muteInterval) {
		if (STATE_STOPED == mState) {// 如果停止状态
			mState = STATE_ENCODING;// 状态设置为编码状态

			if (null != mListener) {//
				mListener.onStartEncode();
			}

			mSinGenerator.start();// 信号产生开始
			// 有效部分+间隔部分
			// 信号产生，有效部分
			for (int index : codes) {// 迭代器，遍历codes
				if (STATE_ENCODING == mState) {
					LogHelper.d(TAG, "encode:" + index);
					if (index >= 0 && index < CODE_FREQUENCY.length) {// 从mcodes中读出的数据位index，index范围小于CODE_FREQUENCY的长度
						mSinGenerator.gen(CODE_FREQUENCY[index], duration);// 根据数值编码（index），在CODE_FREQUENCY数组中找到对应频率，产生该频率的声波信号
					} else {
						LogHelper.e(TAG, "code index error");
					}
				} else {
					LogHelper.d(TAG, "encode force stop");
					break;
				}
			}
			// 间隔产生
			if (STATE_ENCODING == mState) {
				mSinGenerator.gen(0, muteInterval);// 产生间隔0频率信号
			} else {
				LogHelper.d(TAG, "encode force stop");
			}
			stop();

			if (null != mListener) {
				mListener.onEndEncode();
			}
		}
	}

	// 关闭
	public void stop() {
		if (STATE_ENCODING == mState) {
			mState = STATE_STOPED;

			mSinGenerator.stop();// 信号产生停止
		}
	}

	@Override
	public void onStartGen() {
		LogHelper.d(TAG, "start gen codes");
	}

	@Override
	public void onStopGen() {
		LogHelper.d(TAG, "end gen codes");
	}

	@Override
	public BufferData getGenBuffer() {
		if (null != mCallback) {
			return mCallback.getEncodeBuffer();
		}
		return null;
	}

	@Override
	public void freeGenBuffer(BufferData buffer) {
		if (null != mCallback) {
			mCallback.freeEncodeBuffer(buffer);
		}
	}
}
