package com.wheelphone.recorder;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wheelphone.recorder.helpers.CameraPreviewHolder;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class FragmentRecorder extends Fragment{
	
//	private static final String TAG = FragmentFaceme.class.getName();

	private CameraPreviewHolder mFaceTracking;

	private TextView mOutput;

	private WheelphoneRobot mWheelphone;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_faceme,
				container, false);
		mOutput = (TextView)rootView.findViewById(R.id.output);

		mFaceTracking = (CameraPreviewHolder)rootView.findViewById(R.id.camerapreview);
		mFaceTracking.setController(this);

		//Start robot control:
		mWheelphone = new WheelphoneRobot(getActivity().getApplicationContext(), getActivity().getIntent());
		mWheelphone.startUSBCommunication();
//		mWheelphone.enableSoftAcceleration();
		mWheelphone.enableSpeedControl();
		mWheelphone.disableSoftAcceleration();
//		mWheelphone.disableSpeedControl();
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
