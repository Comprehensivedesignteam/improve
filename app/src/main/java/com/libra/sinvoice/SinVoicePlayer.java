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

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;
import android.text.TextUtils;

import com.libra.sinvoice.Buffer.BufferData;

public class SinVoicePlayer implements Encoder.Listener, Encoder.Callback,
		PcmPlayer.Listener, PcmPlayer.Callback {
	private final static String TAG = "SinVoicePlayer";

	private final static int STATE_START = 1;
	private final static int STATE_STOP = 2;
	private final static int STATE_PENDING = 3;

	private final static int DEFAULT_GEN_DURATION = 100;

	private String mCodeBook;
	private List<Integer> mCodes = new ArrayList<Integer>();

	private Encoder mEncoder;
	private PcmPlayer mPlayer;
	private Buffer mBuffer;

	private int mState;
	private Listener mListener;
	private Thread mPlayThread;
	private Thread mEncodeThread;

	public static interface Listener {
		void onPlayStart();

		void onPlayEnd();
	}

	public SinVoicePlayer() {
		this(Common.DEFAULT_CODE_BOOK);
	}

	public SinVoicePlayer(String codeBook) {
		this(codeBook, Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_SIZE,
				Common.DEFAULT_BUFFER_COUNT);
	}

	public SinVoicePlayer(String codeBook, int sampleRate, int bufferSize,
			int buffCount) {
		mState = STATE_STOP;
		mBuffer = new Buffer(buffCount, bufferSize);

		mEncoder = new Encoder(this, sampleRate, SinGenerator.BITS_16,
				bufferSize);
		mEncoder.setListener(this);
		mPlayer = new PcmPlayer(this, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		mPlayer.setListener(this);

		setCodeBook(codeBook);
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	public void setCodeBook(String codeBook) {
		if (!TextUtils.isEmpty(codeBook)
				&& codeBook.length() < Encoder.getMaxCodeCount() - 1) {
			mCodeBook = codeBook;
		}
	}

	// 将输入数值数据转换为频率编码，根据text中数值转换为频率数值存储在mcode队列中
	private boolean convertTextToCodes(String text) {
		boolean ret = true;// ret用于表示编码转换是否成功

		if (!TextUtils.isEmpty(text)) {// 待编码数值是否为空，不为空则执行以下步骤，为空则退出
			mCodes.clear();// 频率编码队列清空
			mCodes.add(Common.START_TOKEN);// 频率编码队列
			int len = text.length();// 获得待编码数值串长度长度
			for (int i = 0; i < len; ++i) {// 将数值串挨个转换为频率编码
				char ch = text.charAt(i);// 得到数值串中第i个字符
				int index = mCodeBook.indexOf(ch);// 根据传入数值，找到在codebook中的位置，找不到则返回-1
				if (index > -1) {
					mCodes.add(index + 1);// 找到的话，就在编码队列中加入这个编码位置
				} else {// 找不到则表示存在法非数值，并且ret设为false，退出循环
					ret = false;
					LogHelper.d(TAG, "invalidate char:" + ch);
					break;
				}
			}
			if (ret) {
				mCodes.add(Common.STOP_TOKEN);// 在最后添加结束编码
			}
		} else {
			ret = false;
		}

		return ret;
	}

	public void play(final String text) {
		play(text, false, 0);
	}

	/*
	 * 播放函数，将数值串编码，然后播放，其中包括了数值串进行频率编码 text为待编码数值串（信息串） repeat为时否重复播放
	 * muteinterval是间隔时间
	 */
	public void play(final String text, final boolean repeat,
			final int muteInterval) {
		/*
		 * 以下这句，判断当前状态为STOP，mCodeBook编码字典不为空，将待编码数值串text转为编码（编码数值是根据mcodebook中来的）
		 * ，并判断是否成功
		 */
		if (STATE_STOP == mState && null != mCodeBook
				&& convertTextToCodes(text)) {
			mState = STATE_PENDING;// 设置当前状态为挂起状态

			mPlayThread = new Thread() {// 播放线程
				@Override
				public void run() {
					mPlayer.start();// 开启音频播放对象
				}
			};
			if (null != mPlayThread) {
				mPlayThread.start();// 开启播放线程
			}

			mEncodeThread = new Thread() {// 编码线程
				@Override
				public void run() {
					do {
						LogHelper.d(TAG, "encode start");
						mEncoder.encode(mCodes, DEFAULT_GEN_DURATION,
								muteInterval);// 编码，mcodes存的是第一步编码，按顺序存的是在FREQUENCY_LIST中的index
						LogHelper.d(TAG, "encode end");

						mEncoder.stop();
					} while (repeat && STATE_PENDING != mState);// 判断是否重复播放且当前状态并无挂起，则继续
					stopPlayer();// 停止播放
				}
			};
			if (null != mEncodeThread) {
				mEncodeThread.start();// 开启编码线程
			}

			LogHelper.d(TAG, "play");
			mState = STATE_START;// 当前状态设置为开始
		}
	}

	// 停止分两步，stop（）+stopPlayer（）

	/*
	 * 停止，主要是状态挂起与关闭编码线程
	 */
	public void stop() {
		if (STATE_START == mState) {
			mState = STATE_PENDING;// 挂起,挂起后，注意play函数中将会退出循环

			LogHelper.d(TAG, "force stop start");
			mEncoder.stop();// 停止编码
			if (null != mEncodeThread) {
				try {
					mEncodeThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					mEncodeThread = null;// 清空变啊线程
				}
			}

			LogHelper.d(TAG, "force stop end");
		}
	}

	// 停止音频播放
	private void stopPlayer() {
		if (mEncoder.isStoped()) {// 检查编码是否结束
			mPlayer.stop();// 停止播放
		}

		// put end buffer
		mBuffer.putFull(BufferData.getEmptyBuffer());

		if (null != mPlayThread) {
			try {
				mPlayThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				mPlayThread = null;
			}
		}

		mBuffer.reset();
		mState = STATE_STOP;// 设置状态为停止
	}

	@Override
	public void onStartEncode() {
		LogHelper.d(TAG, "onStartGen");
	}

	@Override
	public void freeEncodeBuffer(BufferData buffer) {
		if (null != buffer) {
			mBuffer.putFull(buffer);
		}
	}

	@Override
	public BufferData getEncodeBuffer() {
		return mBuffer.getEmpty();
	}

	@Override
	public void onEndEncode() {
	}

	@Override
	public BufferData getPlayBuffer() {
		return mBuffer.getFull();
	}

	@Override
	public void freePlayData(BufferData data) {
		mBuffer.putEmpty(data);
	}

	@Override
	public void onPlayStart() {
		if (null != mListener) {
			mListener.onPlayStart();
		}
	}

	@Override
	public void onPlayStop() {
		if (null != mListener) {
			mListener.onPlayEnd();
		}
	}

}
