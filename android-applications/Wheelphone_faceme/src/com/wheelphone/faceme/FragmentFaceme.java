package com.wheelphone.faceme;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wheelphone.faceme.helpers.FaceTracker;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class FragmentFaceme extends Fragment{
	
//	private static final String TAG = FragmentFaceme.class.getName();

	private FaceTracker mFaceTracker;

	private TextView mOutput;

	private WheelphoneRobot mWheelphone;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_faceme,
				container, false);
		mOutput = (TextView)rootView.findViewById(R.id.output);

		mFaceTracker = (FaceTracker)rootView.findViewById(R.id.camerapreview);
		mFaceTracker.setController(this);

		//Start robot control:
		mWheelphone = new WheelphoneRobot(getActivity().getApplicationContext(), getActivity().getIntent());
		mWheelphone.startUSBCommunication();
//		mWheelphone.enableSoftAcceleration();
		mWheelphone.disableSoftAcceleration();
		mWheelphone.enableSpeedControl();
		return rootView;
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
		mFaceTracker.stopTracking();
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
