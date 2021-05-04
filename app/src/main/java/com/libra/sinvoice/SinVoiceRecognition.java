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

import android.text.TextUtils;

import com.libra.sinvoice.Buffer.BufferData;

public class SinVoiceRecognition implements Record.Listener, Record.Callback,
		VoiceRecognition.Listener, VoiceRecognition.Callback {
	private final static String TAG = "SinVoiceRecognition";

	private final static int STATE_START = 1;// 开始状态
	private final static int STATE_STOP = 2;// 结束状态
	private final static int STATE_PENDING = 3;// 挂起状态

	private Buffer mBuffer;
	private Record mRecord;
	private VoiceRecognition mRecognition;

	private Thread mRecordThread;// 录音线程
	private Thread mRecognitionThread;// 分析线程
	private int mState;// 状态
	private Listener mListener;

	private String mCodeBook;
	private int mMaxCodeIndex;

	public static interface Listener {
		void onRecognitionStart();

		void onRecognition(char ch);

		void onRecognitionEnd();
	}

	public SinVoiceRecognition() {
		this(Common.DEFAULT_CODE_BOOK);
	}

	public SinVoiceRecognition(String codeBook) {
		this(codeBook, Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_SIZE,
				Common.DEFAULT_BUFFER_COUNT);
	}

	/*
	 * 构造函数
	 */
	public SinVoiceRecognition(String codeBook, int sampleRate, int bufferSize,
			int bufferCount) {
		mState = STATE_STOP;
		mBuffer = new Buffer(bufferCount, bufferSize);

		mRecord = new Record(this, sampleRate, Record.CHANNEL_1,
				Record.BITS_16, bufferSize);// 录音对象实例化
		mRecord.setListener(this);
		mRecognition = new VoiceRecognition(this, sampleRate, Record.CHANNEL_1,
				Record.BITS_16);// 编码对象实例化
		mRecognition.setListener(this);

		mMaxCodeIndex = Encoder.getMaxCodeCount() - 2;

		setCodeBook(codeBook);// 设置codeBook
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	// 设置编码对照表
	public void setCodeBook(String codeBook) {
		if (!TextUtils.isEmpty(codeBook) && codeBook.length() <= mMaxCodeIndex) {
			mCodeBook = codeBook;
		}
	}

	// 开始
	public void start() {
		if (STATE_STOP == mState) {
			mState = STATE_PENDING;// 状态挂起

			// 创建解析线程
			mRecognitionThread = new Thread() {// 分析线程
				@Override
				public void run() {
					mRecognition.start();// 分析开始
				}
			};
			if (null != mRecognitionThread) {
				mRecognitionThread.start();// 分先线程开始
			}

			// 创建录音线程
			mRecordThread = new Thread() {
				@Override
				public void run() {
					mRecord.start();// 录音开始，直到被外部停止，就一直在这句代码里运行

					LogHelper.d(TAG, "record thread end");

					LogHelper.d(TAG, "stop recognition start");
					stopRecognition();// 停止数据分析
					LogHelper.d(TAG, "stop recognition end");
				}
			};
			// 开始录音线程
			if (null != mRecordThread) {
				mRecordThread.start();// 开始录音线程
			}

			mState = STATE_START;// 状态设置为start
		}
	}

	// 停止数据解析
	private void stopRecognition() {
		mRecognition.stop();// 解析停止

		// put end buffer
		BufferData data = new BufferData(0);// 清空数据空间
		mBuffer.putFull(data);// 清空数据空间

		// 停止分析线程
		if (null != mRecognitionThread) {
			try {
				mRecognitionThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				mRecognitionThread = null;
			}
		}

		mBuffer.reset();
	}

	// 停止录音，并做状态变更
	public void stop() {
		if (STATE_START == mState) {
			mState = STATE_PENDING;// 转换挂起状态

			LogHelper.d(TAG, "force stop start");
			// 停止录音线程
			mRecord.stop();
			if (null != mRecordThread) {
				try {
					mRecordThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					mRecordThread = null;
				}
			}

			mState = STATE_STOP;// 设置结束状态
			LogHelper.d(TAG, "force stop end");
		}
	}

	@Override
	public void onStartRecord() {
		LogHelper.d(TAG, "start record");
	}

	@Override
	public void onStopRecord() {
		LogHelper.d(TAG, "stop record");
	}

	@Override
	public BufferData getRecordBuffer() {
		BufferData buffer = mBuffer.getEmpty();
		if (null == buffer) {
			LogHelper.e(TAG, "get null empty buffer");
		}
		return buffer;
	}

	@Override
	public void freeRecordBuffer(BufferData buffer) {
		if (null != buffer) {
			if (!mBuffer.putFull(buffer)) {
				LogHelper.e(TAG, "put full buffer failed");
			}
		}
	}

	@Override
	public BufferData getRecognitionBuffer() {
		BufferData buffer = mBuffer.getFull();
		if (null == buffer) {
			LogHelper.e(TAG, "get null full buffer");
		}
		return buffer;
	}

	@Override
	public void freeRecognitionBuffer(BufferData buffer) {
		if (null != buffer) {
			if (!mBuffer.putEmpty(buffer)) {
				LogHelper.e(TAG, "put empty buffer failed");
			}
		}
	}

	@Override
	public void onStartRecognition() {
		LogHelper.d(TAG, "start recognition");
	}

	// 将解析出来的数据传送出去，就在主界面中显示
	// 读一个index，判断是开始字符，还是结束字符，还是有效编码字符
	// index从codebook中找到对应数值（是编码的反过程）
	@Override
	public void onRecognition(int index) {
		LogHelper.d(TAG, "recognition:" + index);

		if (null != mListener) {
			if (Common.START_TOKEN == index) {// 开始字符
				mListener.onRecognitionStart();
			} else if (Common.STOP_TOKEN == index) {// 结束字符
				mListener.onRecognitionEnd();
			} else if (index > 0 && index <= mMaxCodeIndex) {// 有效编码字符
				mListener.onRecognition(mCodeBook.charAt(index - 1));// 查找到对应的数值
			}
		}
	}

	@Override
	public void onStopRecognition() {
		LogHelper.d(TAG, "stop recognition");
	}

}
