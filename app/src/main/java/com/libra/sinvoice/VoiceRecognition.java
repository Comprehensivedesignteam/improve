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
			-1, 1, -1, -1, 0 };// 注意Encoder中第28 ~29行嗲吗注释
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
	 * 声波解析开始 初始化一些参数 调用process->调用prereg、调用reg
	 */
	public void start() {
		if (STATE_STOP == mState) {

			if (null != mCallback) {
				mState = STATE_START;// 开始状态
				mSamplingPointCount = 0;// 采样点计数清零

				mIsStartCounting = false;// 开始计数设为false
				mStep = STEP1;// 阶段1
				mIsBeginning = false;// 在reg中用
				mStartingDet = false;// 在reg中用
				mStartingDetCount = 0;// reg中使用
				mPreRegCircle = -1;// reg中使用
				if (null != mListener) {
					mListener.onStartRecognition();
				}
				while (STATE_START == mState) {
					BufferData data = mCallback.getRecognitionBuffer();
					if (null != data) {
						if (null != data.mData) {
							process(data);// 解码开始

							mCallback.freeRecognitionBuffer(data);// 清空解析内存
						} else {
							LogHelper.d(TAG, "end input buffer, so stop");
							break;
						}
					} else {
						LogHelper.e(TAG, "get null recognition buffer");
						break;
					}
				}

				mState = STATE_STOP;// 状态结束
				if (null != mListener) {
					mListener.onStopRecognition();
				}
			}
		}
	}

	/*
	 * 结束函数
	 */
	public void stop() {
		if (STATE_START == mState) {
			mState = STATE_STOP;
		}
	}

	/*
	 * 处理函数 process流程 1：获得一个周期中的数据个数mSamplingPointCount
	 * 2：mSamplingPointCount放入prereg函数做解码预处理，获得新mSamplingPointCount
	 * 3：mSamplingPointCount使用reg获得解码后数值
	 * 
	 * 详细流程： 非计数状态，STEP1 1 1 1 1 【-1】 -1 -1 -1 1 1 1 1 -1 -1 -1 -1 1
	 * 从第一个数据读，读到小于0的数值（-1）为止，此时从STEP1转换为STEP2 1 1 1 1 -1 -1 -1 -1 【1】 1 1 1 -1
	 * -1 -1 -1 1
	 * 接着读数据，读到大于0的数值（1）为止，此时进入计数状态，，STEP2转换为STEP1，计数器（SamplingPointCount）清0 1 1
	 * 1 1 -1 -1 -1 -1 1 1 1 1 【-1】 -1 -1 -1 1
	 * 每次读一个数据，每读一个就计数器+1，直到读到小于0的数值（-1），此时计算器为4，STEP1转为换STEP2 1 1 1 1 -1 -1 -1
	 * -1 1 1 1 1 -1 -1 -1 -1 【1】
	 * 继续读数据，每读一个计数器+1，直到读到大于0的数值（1），此时计数器为8，然后做预处理preReg，然后再执行reg获得解码数值
	 * 
	 * 因为每个频率信号有100ms，显然包括多个周期，而reg只在一段信号最后一个周期输出解码数值，因此在一段信号中，假设100ms包括某频率信号10个完整周期
	 * ，那么前9个被reg略过，第10个被reg解码传输出数值
	 */
	private void process(BufferData data) {
		int size = data.getFilledSize() - 1;// 传入数据数组大小
		short sh = 0;
		for (int i = 0; i < size; i++) {
			/*------------不管---------*/
			short sh1 = data.mData[i];
			sh1 &= 0xff;
			short sh2 = data.mData[++i];
			sh2 <<= 8;
			sh = (short) ((sh1) | (sh2));
			// 从传入数组中获得一个sh数据，然后对sh进行以下操作
			/*----------不管-----------*/
			// sh是要使用的有效数据
			if (!mIsStartCounting) {// 是否开始计数，未开始计数状态
				if (STEP1 == mStep) {// 看步骤，是否是第一步骤
					if (sh < 0) {// 当且仅当sh<0进入第二状态
						mStep = STEP2;
					}
				} else if (STEP2 == mStep) {
					if (sh > 0) {// 当且仅当sh>0，结束第二状态，开始进行计数
						mIsStartCounting = true;// 开始计数
						mSamplingPointCount = 0;// 重置计数
						mStep = STEP1;// 状态重新转为step1
					}
				}
			}

			// 以下为已开始计数阶段
			else {// 已开始计数：
				++mSamplingPointCount;// 采样点+1
				if (STEP1 == mStep) {// 当第一阶段
					if (sh < 0) {// 如果sh<0
						mStep = STEP2;// 进入第二阶段
					}
				} else if (STEP2 == mStep) {// 当第二阶段
					if (sh > 0) {// sh>0
						// preprocess the circle
						int samplingPointCount = preReg(mSamplingPointCount);

						// recognise voice
						reg(samplingPointCount);

						mSamplingPointCount = 0;// 清空采样点数
						mStep = STEP1;// 返回step1
					}
				}
			}
		}
	}

	/*
	 * 解码预处理 例：如果一个周期读到数据个数为12个，经过预处理，准确数据个数为10个，这需要测试
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
	 * 解析函数 传入经过预处理的采样点计数 mStartingDet 拆分
	 * 此函数与process中的循环配合，分离各个频段信号，每个频段信号解码为一个数值，注意第302行
	 */
	private void reg(int samplingPointCount) {
		//
		if (!mIsBeginning) {// 是否开始解码
			if (!mStartingDet) {
				if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
					mStartingDet = true;//
					mStartingDetCount = 0;// 拆分计数清0
				}
			}

			else {
				if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
					++mStartingDetCount;// 拆分计数+1

					if (mStartingDetCount >= MIN_REG_CIRCLE_COUNT) {// 拆分计数器比较小
						mIsBeginning = true;//
						mIsRegStart = false;//
						mRegCount = 0;
					}
				} else {
					mStartingDet = false;
				}
			}
		}
		// 开始解码
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
								mListener.onRecognition(mRegIndex);// 将解码数值传送出到主界面
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
