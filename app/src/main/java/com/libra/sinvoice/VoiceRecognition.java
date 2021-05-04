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

import com.libra.sinvoice.Buffer.BufferData;

public class VoiceRecognition {
	private final static String TAG = "Recognition";

	private final static int STATE_START = 1;
	private final static int STATE_STOP = 2;
	private final static int STEP1 = 1;
	private final static int STEP2 = 2;
	private final static int INDEX[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, 6, -1, -1, -1, -1, 5, -1, -1, -1, 4, -1, -1, 3, -1, -1, 2, -1,
			-1, 1, -1, -1, 0 };// ע��Encoder�е�28 ~29������ע��
	private final static int MAX_SAMPLING_POINT_COUNT = 31;
	private final static int MIN_REG_CIRCLE_COUNT = 10;

	private int mState;
	private Listener mListener;
	private Callback mCallback;

	private int mSamplingPointCount = 0;

	private int mSampleRate;
	private int mChannel;
	private int mBits;

	private boolean mIsStartCounting = false;
	private int mStep;
	private boolean mIsBeginning = false;
	private boolean mStartingDet = false;
	private int mStartingDetCount;

	private int mRegValue;
	private int mRegIndex;
	private int mRegCount;
	private int mPreRegCircle;
	private boolean mIsRegStart = false;

	public static interface Listener {
		void onStartRecognition();

		void onRecognition(int index);

		void onStopRecognition();
	}

	public static interface Callback {
		BufferData getRecognitionBuffer();

		void freeRecognitionBuffer(BufferData buffer);
	}

	public VoiceRecognition(Callback callback, int SampleRate, int channel,
			int bits) {
		mState = STATE_STOP;

		mCallback = callback;
		mSampleRate = SampleRate;
		mChannel = channel;
		mBits = bits;
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	/*
	 * ����������ʼ ��ʼ��һЩ���� ����process->����prereg������reg
	 */
	public void start() {
		if (STATE_STOP == mState) {

			if (null != mCallback) {
				mState = STATE_START;// ��ʼ״̬
				mSamplingPointCount = 0;// �������������

				mIsStartCounting = false;// ��ʼ������Ϊfalse
				mStep = STEP1;// �׶�1
				mIsBeginning = false;// ��reg����
				mStartingDet = false;// ��reg����
				mStartingDetCount = 0;// reg��ʹ��
				mPreRegCircle = -1;// reg��ʹ��
				if (null != mListener) {
					mListener.onStartRecognition();
				}
				while (STATE_START == mState) {
					BufferData data = mCallback.getRecognitionBuffer();
					if (null != data) {
						if (null != data.mData) {
							process(data);// ���뿪ʼ

							mCallback.freeRecognitionBuffer(data);// ��ս����ڴ�
						} else {
							LogHelper.d(TAG, "end input buffer, so stop");
							break;
						}
					} else {
						LogHelper.e(TAG, "get null recognition buffer");
						break;
					}
				}

				mState = STATE_STOP;// ״̬����
				if (null != mListener) {
					mListener.onStopRecognition();
				}
			}
		}
	}

	/*
	 * ��������
	 */
	public void stop() {
		if (STATE_START == mState) {
			mState = STATE_STOP;
		}
	}

	/*
	 * ������ process���� 1�����һ�������е����ݸ���mSamplingPointCount
	 * 2��mSamplingPointCount����prereg����������Ԥ���������mSamplingPointCount
	 * 3��mSamplingPointCountʹ��reg��ý������ֵ
	 * 
	 * ��ϸ���̣� �Ǽ���״̬��STEP1 1 1 1 1 ��-1�� -1 -1 -1 1 1 1 1 -1 -1 -1 -1 1
	 * �ӵ�һ�����ݶ�������С��0����ֵ��-1��Ϊֹ����ʱ��STEP1ת��ΪSTEP2 1 1 1 1 -1 -1 -1 -1 ��1�� 1 1 1 -1
	 * -1 -1 -1 1
	 * ���Ŷ����ݣ���������0����ֵ��1��Ϊֹ����ʱ�������״̬����STEP2ת��ΪSTEP1����������SamplingPointCount����0 1 1
	 * 1 1 -1 -1 -1 -1 1 1 1 1 ��-1�� -1 -1 -1 1
	 * ÿ�ζ�һ�����ݣ�ÿ��һ���ͼ�����+1��ֱ������С��0����ֵ��-1������ʱ������Ϊ4��STEP1תΪ��STEP2 1 1 1 1 -1 -1 -1
	 * -1 1 1 1 1 -1 -1 -1 -1 ��1��
	 * ���������ݣ�ÿ��һ��������+1��ֱ����������0����ֵ��1������ʱ������Ϊ8��Ȼ����Ԥ����preReg��Ȼ����ִ��reg��ý�����ֵ
	 * 
	 * ��Ϊÿ��Ƶ���ź���100ms����Ȼ����������ڣ���regֻ��һ���ź����һ���������������ֵ�������һ���ź��У�����100ms����ĳƵ���ź�10����������
	 * ����ôǰ9����reg�Թ�����10����reg���봫�����ֵ
	 */
	private void process(BufferData data) {
		int size = data.getFilledSize() - 1;// �������������С
		short sh = 0;
		for (int i = 0; i < size; i++) {
			/*------------����---------*/
			short sh1 = data.mData[i];
			sh1 &= 0xff;
			short sh2 = data.mData[++i];
			sh2 <<= 8;
			sh = (short) ((sh1) | (sh2));
			// �Ӵ��������л��һ��sh���ݣ�Ȼ���sh�������²���
			/*----------����-----------*/
			// sh��Ҫʹ�õ���Ч����
			if (!mIsStartCounting) {// �Ƿ�ʼ������δ��ʼ����״̬
				if (STEP1 == mStep) {// �����裬�Ƿ��ǵ�һ����
					if (sh < 0) {// ���ҽ���sh<0����ڶ�״̬
						mStep = STEP2;
					}
				} else if (STEP2 == mStep) {
					if (sh > 0) {// ���ҽ���sh>0�������ڶ�״̬����ʼ���м���
						mIsStartCounting = true;// ��ʼ����
						mSamplingPointCount = 0;// ���ü���
						mStep = STEP1;// ״̬����תΪstep1
					}
				}
			}

			// ����Ϊ�ѿ�ʼ�����׶�
			else {// �ѿ�ʼ������
				++mSamplingPointCount;// ������+1
				if (STEP1 == mStep) {// ����һ�׶�
					if (sh < 0) {// ���sh<0
						mStep = STEP2;// ����ڶ��׶�
					}
				} else if (STEP2 == mStep) {// ���ڶ��׶�
					if (sh > 0) {// sh>0
						// preprocess the circle
						int samplingPointCount = preReg(mSamplingPointCount);

						// recognise voice
						reg(samplingPointCount);

						mSamplingPointCount = 0;// ��ղ�������
						mStep = STEP1;// ����step1
					}
				}
			}
		}
	}

	/*
	 * ����Ԥ���� �������һ�����ڶ������ݸ���Ϊ12��������Ԥ����׼ȷ���ݸ���Ϊ10��������Ҫ����
	 */
	private int preReg(int samplingPointCount) {
		switch (samplingPointCount) {
		case 8:
		case 9:
		case 10:
		case 11:
		case 12:
			samplingPointCount = 10;
			break;

		case 13:
		case 14:
		case 15:
		case 16:
		case 17:
			samplingPointCount = 15;
			break;

		case 18:
		case 19:
		case 20:
			samplingPointCount = 19;
			break;

		case 21:
		case 22:
		case 23:
			samplingPointCount = 22;
			break;

		case 24:
		case 25:
		case 26:
			samplingPointCount = 25;
			break;

		case 27:
		case 28:
		case 29:
			samplingPointCount = 28;
			break;

		case 30:
		case 31:
		case 32:
			samplingPointCount = 31;
			break;

		default:
			samplingPointCount = 0;
			break;
		}

		return samplingPointCount;
	}

	/*
	 * �������� ���뾭��Ԥ����Ĳ�������� mStartingDet ���
	 * �˺�����process�е�ѭ����ϣ��������Ƶ���źţ�ÿ��Ƶ���źŽ���Ϊһ����ֵ��ע���302��
	 */
	private void reg(int samplingPointCount) {
		//
		if (!mIsBeginning) {// �Ƿ�ʼ����
			if (!mStartingDet) {
				if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
					mStartingDet = true;//
					mStartingDetCount = 0;// ��ּ�����0
				}
			}

			else {
				if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
					++mStartingDetCount;// ��ּ���+1

					if (mStartingDetCount >= MIN_REG_CIRCLE_COUNT) {// ��ּ������Ƚ�С
						mIsBeginning = true;//
						mIsRegStart = false;//
						mRegCount = 0;
					}
				} else {
					mStartingDet = false;
				}
			}
		}
		// ��ʼ����
		else {
			if (!mIsRegStart) {
				if (samplingPointCount > 0) {
					mRegValue = samplingPointCount;
					mRegIndex = INDEX[samplingPointCount];
					mIsRegStart = true;
					mRegCount = 1;
				}
			}

			else {
				if (samplingPointCount == mRegValue) {
					++mRegCount;

					if (mRegCount >= MIN_REG_CIRCLE_COUNT) {
						// ok
						if (mRegValue != mPreRegCircle) {
							if (null != mListener) {
								mListener.onRecognition(mRegIndex);// ��������ֵ���ͳ���������
							}
							mPreRegCircle = mRegValue;
						}
						mIsRegStart = false;
					}
				} else {
					mIsRegStart = false;
				}
			}
		}

	}
}
