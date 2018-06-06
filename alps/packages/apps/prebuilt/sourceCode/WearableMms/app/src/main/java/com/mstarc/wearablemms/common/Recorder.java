package com.mstarc.wearablemms.common;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Recorder {

	private static final String LOG_TAG = Recorder.class.getSimpleName();

	private static final int[] sampleRates = new int[] {48000, 44100, 22050, 11025, 8000};
	public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED};

	private int mBitsPerSample = 16;
	private int mChannelConfig = 1;
	private int mAudioSource;
	private int mSampleRate;
	private int mAudioFormat;
	private int mFramePeriod;
	private int mBufferSize;
	private int mPayloadSize;
	private int mAmplitude;

	private State mState;

	private byte[] mBuffer;

	private AudioRecord mAudioRecord;

	private RandomAccessFile mRandomAccessFile;

	private final int TIME_INTERVAL = 120;
	private String mFilePath;

	private static Recorder mRecorder = null;

	public static Recorder getInstance() {
		if(mRecorder == null) {
			mRecorder = new Recorder(MediaRecorder.AudioSource.MIC, sampleRates[0], AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		} else {
			mRecorder.mState = State.INITIALIZING;
		}
		return mRecorder;
	}

	private Recorder(int source, int sampleRate, int channel, int format) {
		if(format == AudioFormat.ENCODING_PCM_8BIT) {
			mBitsPerSample = 8;
		}
		if(channel == AudioFormat.CHANNEL_IN_STEREO) {
			mChannelConfig = 2;
		}
		mAudioSource = source;
		mSampleRate = sampleRate;
		mAudioFormat = format;
		mFramePeriod = sampleRate * TIME_INTERVAL/1000;
		mBufferSize = mFramePeriod * 2 * mBitsPerSample * mChannelConfig / 8;

		if(mBufferSize < AudioRecord.getMinBufferSize(sampleRate, channel, format)) {
			mBufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, format);
			mFramePeriod = 8 * mBufferSize / (2 * mBitsPerSample * mChannelConfig);
		}
//		Log.d(LOG_TAG, "Buffer size: " + mBufferSize);

		mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize);

		if(mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
			mAudioRecord.setRecordPositionUpdateListener(mRecordUpdatePositionListener);
			mAudioRecord.setPositionNotificationPeriod(mFramePeriod);
			mState = State.INITIALIZING;
			mAmplitude = 0;
			mFilePath = null;
		} else {
			Log.d(LOG_TAG, "Audio record is not initialized");
			mState = State.ERROR;
		}
	}

	private AudioRecord.OnRecordPositionUpdateListener mRecordUpdatePositionListener = new AudioRecord.OnRecordPositionUpdateListener() {
		@Override
		public void onMarkerReached(AudioRecord recorder) {

		}

		@Override
		public void onPeriodicNotification(AudioRecord recorder) {
			recorder.read(mBuffer, 0, mBuffer.length);

			try {
				mRandomAccessFile.write(mBuffer);
				mPayloadSize = mBuffer.length;

				if(mBitsPerSample == 16) { // 16 bit sample
					int currentSample;
					for(int i = 0; i < mBuffer.length / 2; i++) {
						currentSample = getInt(mBuffer[i*2], mBuffer[i*2+1]);
//						Log.d(LOG_TAG, "recorder Amplitude 10000 range: " + currentSample + ", buffer length: " + mBuffer.length);
						if(currentSample > 25000) {
							if(currentSample < 30000) {
								Log.d(LOG_TAG, "recorder amplitude 30000 range: " + currentSample);
							} else if(currentSample < 40000) {
								Log.d(LOG_TAG, "recorder amplitude 40000 range: " + currentSample);
							} else if(currentSample < 50000){
								Log.d(LOG_TAG, "recorder amplitude 50000 range: " + currentSample);
							}
						}
					}

				} else { // 8 bit sample
					for(int i = 0; i < mBuffer.length; i++) {
						if(mBuffer[i] > mAmplitude) {
							mAmplitude = mBuffer[i];
						}
					}
				}
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error occurred in onPeriodNotification while recording");
			}
		}
	};

	public void setOutputFile(String filePath) {
		mFilePath = filePath;
	}

	public void prepare() {
		if(mState == State.INITIALIZING) {
			if(mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED && !(mFilePath == null || mFilePath.isEmpty())) {
				try {
					mRandomAccessFile = new RandomAccessFile(mFilePath, "rw");
					mRandomAccessFile.setLength(0);
					mRandomAccessFile.writeBytes("RIFF");
					mRandomAccessFile.writeInt(0);
					mRandomAccessFile.writeBytes("WAVE");
					mRandomAccessFile.writeBytes("fmt");
					mRandomAccessFile.writeInt(Integer.reverseBytes(16));
					mRandomAccessFile.writeInt(Integer.reverseBytes(1));
					mRandomAccessFile.writeInt(Integer.reverseBytes(mChannelConfig));
					mRandomAccessFile.writeInt(Integer.reverseBytes(mSampleRate));
					mRandomAccessFile.writeInt(Integer.reverseBytes(mSampleRate * mBitsPerSample * mChannelConfig / 8));
					mRandomAccessFile.writeInt(Integer.reverseBytes(mBitsPerSample * mChannelConfig / 8));
					mRandomAccessFile.writeInt(Integer.reverseBytes(mBitsPerSample));
					mRandomAccessFile.writeBytes("data");
					mRandomAccessFile.writeInt(0);

					mBuffer = new byte[mFramePeriod * mBitsPerSample * mChannelConfig / 8];
					mState = State.READY;

				} catch (FileNotFoundException e) {
					e.printStackTrace();
					Log.d(LOG_TAG, "prepare(): File path not found: " + mFilePath);
					mState = State.ERROR;
				} catch (IOException e) {
					Log.d(LOG_TAG, "prepare(): I/O Exception");
					e.printStackTrace();
					mState = State.ERROR;
				}
			} else {
				Log.d(LOG_TAG, "prepare(): Recorder is not initialized");
				mState = State.ERROR;
			}
		} else {
			Log.d(LOG_TAG, "prepare(): Recorder state is not ready");
			release();
			mState = State.ERROR;
		}
	}

	public void release() {
		if(mState == State.RECORDING) {
			stop();
		} else if (mState == State.READY) {
			try {
				mRandomAccessFile.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(LOG_TAG, "release(): I/O exception while closing the file");
			}
			if(new File(mFilePath).delete()) {
				Log.d(LOG_TAG, "Successfully deleted the file");
			} else {
				Log.d(LOG_TAG, "Failed to delete the file");
			}

			if(mAudioRecord != null) {
				mAudioRecord.release();
				mAudioRecord = null;
			}
		}
	}

	public void start() {
		if(mState == State.READY) {
			mPayloadSize = 0;
			mAudioRecord.startRecording();
			mAudioRecord.read(mBuffer, 0, mBuffer.length);
			mState = State.RECORDING;
			Log.d(LOG_TAG, "start(): State: " + mState);
		} else {
			Log.d(LOG_TAG, "start(): called on an illegal state");
			mState = State.ERROR;
		}
	}

	public void stop() {
		if(mState == State.RECORDING) {
			mAudioRecord.stop();
			mAmplitude = 0;
			mState = State.STOPPED;
			Log.d(LOG_TAG, "stop(): State: " + mState.name());

			try {
				mRandomAccessFile.seek(4);
				mRandomAccessFile.writeInt(Integer.reverseBytes(36 + mPayloadSize));
				mRandomAccessFile.seek(40);
				mRandomAccessFile.writeInt(Integer.reverseBytes(mPayloadSize));
				mRandomAccessFile.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(LOG_TAG, "I/O exception occurred while closing the output file");
				mState = State.ERROR;
			}
		} else {
			Log.d(LOG_TAG, "stop(): called on illegal state");
			mState = State.ERROR;
		}
	}

	public void reset() {
		if(mState != State.ERROR) {
			release();
			mFilePath = null;
			mAmplitude = 0;
			mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize);
			mState = State.INITIALIZING;
		}
	}

	public State getState() {
		return mState;
	}

	public int getMaxAmplitude() {
		if(mState == State.RECORDING) {
			int maxAmplitude = mAmplitude;
			mAmplitude = 0;
			Log.d(LOG_TAG, "Max amplitude is: " + maxAmplitude);
			return maxAmplitude;
		}
		Log.d(LOG_TAG, "Trying to get amplitude in illegal state");
		return 0;
	}

	private int getInt(byte byte1, byte byte2) {
		return (byte1 | (byte1 << 8));
	}
}
