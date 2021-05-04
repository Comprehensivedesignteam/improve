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

	private final static int STATE_START = 1;// ��ʼ״̬
	private final static int STATE_STOP = 2;// ����״̬
	private final static int STATE_PENDING = 3;// ����״̬

	private Buffer mBuffer;
	private Record mRecord;
	private VoiceRecognition mRecognition;

	private Thread mRecordThread;// ¼���߳�
	private Thread mRecognitionThread;// �����߳�
	private int mState;// ״̬
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
	 * ���캯��
	 */
	public SinVoiceRecognition(String codeBook, int sampleRate, int bufferSize,
			int bufferCount) {
		mState = STATE_STOP;
		mBuffer = new Buffer(bufferCount, bufferSize);

		mRecord = new Record(this, sampleRate, Record.CHANNEL_1,
				Record.BITS_16, bufferSize);// ¼������ʵ����
		mRecord.setListener(this);
		mRecognition = new VoiceRecognition(this, sampleRate, Record.CHANNEL_1,
				Record.BITS_16);// �������ʵ����
		mRecognition.setListener(this);

		mMaxCodeIndex = Encoder.getMaxCodeCount() - 2;

		setCodeBook(codeBook);// ����codeBook
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	// ���ñ�����ձ�
	public void setCodeBook(String codeBook) {
		if (!TextUtils.isEmpty(codeBook) && codeBook.length() <= mMaxCodeIndex) {
			mCodeBook = codeBook;
		}
	}

	// ��ʼ
	public void start() {
		if (STATE_STOP == mState) {
			mState = STATE_PENDING;// ״̬����

			// ���������߳�
			mRecognitionThread = new Thread() {// �����߳�
				@Override
				public void run() {
					mRecognition.start();// ������ʼ
				}
			};
			if (null != mRecognitionThread) {
				mRecognitionThread.start();// �����߳̿�ʼ
			}

			// ����¼���߳�
			mRecordThread = new Thread() {
				@Override
				public void run() {
					mRecord.start();// ¼����ʼ��ֱ�����ⲿֹͣ����һֱ��������������

					LogHelper.d(TAG, "record thread end");

					LogHelper.d(TAG, "stop recognition start");
					stopRecognition();// ֹͣ���ݷ���
					LogHelper.d(TAG, "stop recognition end");
				}
			};
			// ��ʼ¼���߳�
			if (null != mRecordThread) {
				mRecordThread.start();// ��ʼ¼���߳�
			}

			mState = STATE_START;// ״̬����Ϊstart
		}
	}

	// ֹͣ���ݽ���
	private void stopRecognition() {
		mRecognition.stop();// ����ֹͣ

		// put end buffer
		BufferData data = new BufferData(0);// ������ݿռ�
		mBuffer.putFull(data);// ������ݿռ�

		// ֹͣ�����߳�
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

	// ֹͣ¼��������״̬���
	public void stop() {
		if (STATE_START == mState) {
			mState = STATE_PENDING;// ת������״̬

			LogHelper.d(TAG, "force stop start");
			// ֹͣ¼���߳�
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

			mState = STATE_STOP;// ���ý���״̬
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

	// ���������������ݴ��ͳ�ȥ����������������ʾ
	// ��һ��index���ж��ǿ�ʼ�ַ������ǽ����ַ���������Ч�����ַ�
	// index��codebook���ҵ���Ӧ��ֵ���Ǳ���ķ����̣�
	@Override
	public void onRecognition(int index) {
		LogHelper.d(TAG, "recognition:" + index);

		if (null != mListener) {
			if (Common.START_TOKEN == index) {// ��ʼ�ַ�
				mListener.onRecognitionStart();
			} else if (Common.STOP_TOKEN == index) {// �����ַ�
				mListener.onRecognitionEnd();
			} else if (index > 0 && index <= mMaxCodeIndex) {// ��Ч�����ַ�
				mListener.onRecognition(mCodeBook.charAt(index - 1));// ���ҵ���Ӧ����ֵ
			}
		}
	}

	@Override
	public void onStopRecognition() {
		LogHelper.d(TAG, "stop recognition");
	}

}
