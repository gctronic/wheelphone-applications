package com.wheelphone.pet;

/* 
 * Glue between the different components and GUI for the Pet application
 */
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.wheelphone.pet.helpers.Behaviour;
import com.wheelphone.pet.helpers.FaceExpressions;
import com.wheelphone.pet.helpers.FaceTracking;
import com.wheelphone.pet.helpers.LogcatStreamer;
import com.wheelphone.pet.helpers.Talker;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class FragmentPet extends Fragment {

	private static final String TAG = FragmentPet.class.getName();

	private TextView mOutput;

	//Helpers:
	private WheelphoneRobot mWheelphone;
	private FaceExpressions mFaceExpression;
 
	private Talker mTalker;

	private FaceTracking mFaceTracking;  
//	private Intent speechIntent;
	
	private LogcatStreamer mLogcatStreamer;
	
	private Behaviour mBehaviour;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_pet,
				container, false);
		
	
		
		
		mOutput = (TextView)rootView.findViewById(R.id.output);

		//FaceExpressionsView
		mFaceExpression = (FaceExpressions)rootView.findViewById(R.id.face);

		//Start robot control:
		mWheelphone = new WheelphoneRobot(getActivity().getApplicationContext(), getActivity().getIntent());
		mWheelphone.startUSBCommunication();
		readAndSetPreferences();

		//Face tracking: 
		mFaceTracking = (FaceTracking)rootView.findViewById(R.id.camerapreview);
		mFaceTracking.setController(this);

		//Start speech server
//		speechIntent = new Intent(getActivity(), SpeechService.class);
		
		//Start text to speech server:
		mTalker = new Talker(getActivity());
		
		//Behaviour
		//TODO: Move all this code to the Activity onCreate, so that we keep track of the last face and we can parallelize some initialization (now we are too slow to start the app) 
		mBehaviour = new Behaviour(mFaceExpression, mWheelphone, mFaceTracking, mTalker, this);
		
		
		return rootView;
	}

	private void readAndSetPreferences() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean useSpeedControl = sharedPref.getBoolean("pref_speedcontrol", false);
		boolean useSoftAccel = sharedPref.getBoolean("pref_softaccel", false);
		if (useSpeedControl) {
			mWheelphone.enableSpeedControl();
			Toast.makeText(getActivity(), "Speed control ENABLED", Toast.LENGTH_SHORT).show();
		} else {
			mWheelphone.disableSpeedControl();
			Toast.makeText(getActivity(), "Speed control DISABLED", Toast.LENGTH_SHORT).show();
		}
		if (useSoftAccel) {
			mWheelphone.enableSoftAcceleration();
			Toast.makeText(getActivity(), "Soft-acceleration ENABLED", Toast.LENGTH_SHORT).show();
		} else {
			mWheelphone.disableSoftAcceleration();
			Toast.makeText(getActivity(), "Soft-acceleration DISABLED", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		mWheelphone.resumeUSBCommunication();
		mBehaviour.resume();
		//Start logcat streamer
		mLogcatStreamer = new LogcatStreamer();
		mTalker.resume();
//		getActivity().startService(speechIntent);
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		//Stop robot before disconnecting:
		mWheelphone.pauseUSBCommunication();
//		getActivity().stopService(speechIntent);
		mBehaviour.pause();
		//Stop logcat streamer
		mLogcatStreamer.stop();
//		mTalker.pause();
		super.onPause();
	}
	
	
	public void showText(String text) {
		if (mOutput != null){
			mOutput.setText(text);
		}
	}
}
