package com.wheelphone.recorder;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wheelphone.recorder.helpers.CameraPreviewHolder;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class FragmentRecorder extends Fragment{
	
	private static final String TAG = FragmentRecorder.class.getName();

	private CameraPreviewHolder mCameraPreview;

	private TextView mOutput;

	private WheelphoneRobot mWheelphone;
	
	private boolean mIsRecording = false;

	private int mSpeed;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_recorder,
				container, false);
				
		mOutput = (TextView)rootView.findViewById(R.id.output);

		mCameraPreview = (CameraPreviewHolder)rootView.findViewById(R.id.camerapreview);
		mCameraPreview.setController(this);

		//Start robot control:
		mWheelphone = new WheelphoneRobot(getActivity().getApplicationContext(), getActivity().getIntent());
		mWheelphone.startUSBCommunication();
		
		readAndSetPreferences();
		
		Button button = (Button) getActivity().findViewById(R.id.button_start_stop);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsRecording){
					mIsRecording = false;
					mCameraPreview.stop();
					((Button)v).setText(getString(R.string.start));
	            	setSpeed(0, 0);
				} else {
					mIsRecording = true;
					mCameraPreview.start();
					((Button)v).setText(getString(R.string.stop));
	            	setSpeed(mSpeed, mSpeed);
				}
				
			}
		});

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
		
		String tPref = sharedPref.getString("pref_speed", null);
		mSpeed = Integer.valueOf(tPref);
	}


	@Override
	public void onResume() {
		mWheelphone.resumeUSBCommunication();
		super.onResume();
	}

	@Override
	public void onPause() {		
		super.onPause();
		//Stop robot before disconnecting:
		mWheelphone.setSpeed(0, 0);

		mWheelphone.pauseUSBCommunication();
	}


	public void showText(String text) {
		if (mOutput != null){
			mOutput.setText(text);
		}
	}

	public void setSpeed(int leftSpeed, int rightSpeed) {
		String status = mWheelphone.isUSBConnected() ? "Connected" : "Disconnected";
		showText(status + ". L: " + leftSpeed + ", R: " + rightSpeed);
		mWheelphone.setSpeed(leftSpeed, rightSpeed);
	}
}
