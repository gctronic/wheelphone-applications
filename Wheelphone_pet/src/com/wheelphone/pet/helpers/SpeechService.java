package com.wheelphone.pet.helpers;

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class SpeechService extends Service
{
	private static final String TAG = SpeechService.class.getName();

	protected AudioManager mAudioManager; 
	protected SpeechRecognizer mSpeechRecognizer;
	protected Intent mSpeechRecognizerIntent;

	protected volatile boolean mIsCountDownOn;

	static final int MSG_RECOGNIZER_START_LISTENING = 1;
	static final int MSG_RECOGNIZER_CANCEL = 2;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE); 
		mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
		mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
				this.getPackageName());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
			// turn off beep sound  
			mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
		}
		mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

		Log.d(TAG, "onCreate (service)");        
        
		//		SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(getActivity());
		//		CommandListener listener = new CommandListener();
		//		sr.setRecognitionListener(listener);
		//		sr.startListening(RecognizerIntent.getVoiceDetailsIntent(getActivity()));
	}

/*	protected static class IncomingHandler extends Handler
	{
		private WeakReference<SpeechService> mtarget;

		IncomingHandler(SpeechService target)
		{
			mtarget = new WeakReference<SpeechService>(target);
		}


		@Override
		public void handleMessage(Message msg)
		{
			final SpeechService target = mtarget.get();

			switch (msg.what)
			{
			case MSG_RECOGNIZER_START_LISTENING:

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
					// turn off beep sound  
					target.mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
				}
				if (!target.mIsListening)
				{
					target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
					target.mIsListening = true;
					Log.d(TAG, "message start listening"); //$NON-NLS-1$
				}
				break;

			case MSG_RECOGNIZER_CANCEL:
				target.mSpeechRecognizer.cancel();
				target.mIsListening = false;
				Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
				break;
			}
		} 
	} 
*/
	
	// Count down timer for Jelly Bean work around
	protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000){
		@Override
		public void onTick(long millisUntilFinished){}
		@Override
		public void onFinish(){
			mIsCountDownOn = false;
			mSpeechRecognizer.cancel();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
				// turn off beep sound
				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			}
			mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
		}
	};

	@Override
	public void onDestroy(){
		super.onDestroy();

		if (mIsCountDownOn){
			mNoSpeechCountDown.cancel();
		}
		if (mSpeechRecognizer != null){
			mSpeechRecognizer.destroy();
		}
		Log.d(TAG, "onDestroy (service)");

	}

	protected class SpeechRecognitionListener implements RecognitionListener
	{

		@Override
		public void onBeginningOfSpeech()
		{
			// speech input will be processed, so there is no need for count down anymore
			if (mIsCountDownOn)
			{
				mIsCountDownOn = false;
				mNoSpeechCountDown.cancel();
			}
			Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
		}

		@Override
		public void onBufferReceived(byte[] buffer){
			//			Log.d(TAG, "onBufferReceived");
		}

		@Override
		public void onEndOfSpeech(){
			Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
		}

		@Override
		public void onError(int error)
		{
			if (mIsCountDownOn){
				mIsCountDownOn = false;
				mNoSpeechCountDown.cancel();
			}
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
				// turn off beep sound
				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			}
			mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

			Log.d(TAG, "error = " + error); //$NON-NLS-1$
		}

		@Override
		public void onEvent(int eventType, Bundle params){
			Log.d(TAG, "onEvent");
		}

		@Override
		public void onPartialResults(Bundle partialResults){
			//			Log.d(TAG, "onPartialResults");
			//			ArrayList strlist = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			//			for (int i = 0; i < strlist.size();i++ ) {
			//				Log.d(TAG, "partialResult =" + strlist.get(i));
			//			}
		}

		@Override
		public void onReadyForSpeech(Bundle params)
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			{
				mIsCountDownOn = true;
				mNoSpeechCountDown.start();
				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
			}
			Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$
		}

		@Override
		public void onResults(Bundle results){
			Log.d(TAG, "onResults"); //$NON-NLS-1$
			ArrayList<String> strlist = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			for (int i = 0; i < strlist.size();i++ ) {
				Log.d(TAG, "result=" + strlist.get(i));
			}
			
			//Keep listening
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
				// turn off beep sound  
				mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			}
			mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
		}

		@Override
		public void onRmsChanged(float rmsdB){
			//			Log.d(TAG, "onRmsChanged");
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}