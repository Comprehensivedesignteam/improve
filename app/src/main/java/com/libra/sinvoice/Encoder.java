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
	private final static int STATE_ENCODING = 1;// ����״̬
	private final static int STATE_STOPED = 2;// ֹͣ״̬

	// index 0, 1, 2, 3, 4, 5, 6
	// sampling point Count 31, 28, 25, 22, 19, 15, 10
	// ע��31*1422=28*1575=25*1764 = 44100
	private final static int[] CODE_FREQUENCY = { 1422, 1575, 1764, 2004, 2321,
			2940, 4410 };
	/*
	 * ����Ƶ�� index ��ӦƵ�� 0�� 1422 1�� 1575 2�� 1764 ....
	 * ��ֵ����text->��codebook�ҵ�index���浽code������->code�����ӦCODE_FREQUENCY���ҳ�Ƶ��
	 */
	private int mState;// ��ǰ״̬

	private SinGenerator mSinGenerator;// ���Ҳ���
	private Listener mListener;
	private Callback mCallback;//

	public static interface Listener {// Listener�ӿ�
		void onStartEncode();

		void onEndEncode();
	}

	public static interface Callback {// Callback�ӿ�
		void freeEncodeBuffer(BufferData buffer);

		BufferData getEncodeBuffer();
	}

	// ���캯����
	public Encoder(Callback callback, int sampleRate, int bits, int bufferSize) {
		mCallback = callback;// ��ûص�
		mState = STATE_STOPED;// ֹͣ״̬
		mSinGenerator = new SinGenerator(this, sampleRate, bits, bufferSize);
		mSinGenerator.setListener(this);
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	// �����������
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
	 * ���뺯�� codes:�������� duration������ʱ�� muteInterval:ʱ����
	 */
	public void encode(List<Integer> codes, int duration, int muteInterval) {
		if (STATE_STOPED == mState) {// ���ֹͣ״̬
			mState = STATE_ENCODING;// ״̬����Ϊ����״̬

			if (null != mListener) {//
				mListener.onStartEncode();
			}

			mSinGenerator.start();// �źŲ�����ʼ
			// ��Ч����+�������
			// �źŲ�������Ч����
			for (int index : codes) {// ������������codes
				if (STATE_ENCODING == mState) {
					LogHelper.d(TAG, "encode:" + index);
					if (index >= 0 && index < CODE_FREQUENCY.length) {// ��mcodes�ж���������λindex��index��ΧС��CODE_FREQUENCY�ĳ���
						mSinGenerator.gen(CODE_FREQUENCY[index], duration);// ������ֵ���루index������CODE_FREQUENCY�������ҵ���ӦƵ�ʣ�������Ƶ�ʵ������ź�
					} else {
						LogHelper.e(TAG, "code index error");
					}
				} else {
					LogHelper.d(TAG, "encode force stop");
					break;
				}
			}
			// �������
			if (STATE_ENCODING == mState) {
				mSinGenerator.gen(0, muteInterval);// �������0Ƶ���ź�
			} else {
				LogHelper.d(TAG, "encode force stop");
			}
			stop();

			if (null != mListener) {
				mListener.onEndEncode();
			}
		}
	}

	// �ر�
	public void stop() {
		if (STATE_ENCODING == mState) {
			mState = STATE_STOPED;

			mSinGenerator.stop();// �źŲ���ֹͣ
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
