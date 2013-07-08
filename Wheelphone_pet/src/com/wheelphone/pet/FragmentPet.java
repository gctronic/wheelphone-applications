package com.wheelphone.pet;

/* 
 * Glue between the different components and GUI for the Pet application
 */
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wheelphone.pet.helpers.FaceExpressionsView;
import com.wheelphone.pet.helpers.FaceTracking;
import com.wheelphone.wheelphonelibrary.WheelphoneRobot;

public class FragmentPet extends Fragment {

//	private static String TAG = FragmentPet.class.getName();

	private TextView output;

	//Helpers:
	private WheelphoneRobot mWheelphone;
	private FaceExpressionsView mFaceExpression;
	private FaceTracking mFaceTracking;
//	private Intent speechIntent;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_pet,
				container, false);
		output = (TextView)rootView.findViewById(R.id.output);

		//Start robot control:
		mWheelphone = new WheelphoneRobot(getActivity(), getActivity());
		mWheelphone.startUSBCommunication();
		mWheelphone.enableSoftAcceleration();
		mWheelphone.enableSpeedControl();
		
		//Face tracking: 
		mFaceTracking = (FaceTracking)rootView.findViewById(R.id.camerapreview);// TODO: Make sure that the image is not scaled, even for size of 1x1dp, it would be a waste of CPU
		mFaceTracking.setController(this);

		//Start speech server
//		speechIntent = new Intent(getActivity(), SpeechService.class);
		
		//FaceExpressionsView
		mFaceExpression = (FaceExpressionsView)rootView.findViewById(R.id.face);

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		mWheelphone.resumeUSBCommunication();
//		getActivity().startService(speechIntent);
	}

	@Override
	public void onPause() {		
		//Stop robot before disconnecting:
		mWheelphone.setSpeed(0, 0);
		mFaceTracking.stopTracking();

		mWheelphone.pauseUSBCommunication();
//		getActivity().stopService(speechIntent);
		super.onPause();
	}
	
	/*
	 * GLUE CODE:
	 * To wheelphone control
	 */

	public void updateOutput(int leftSpeed, int rightSpeed) {
		if (output != null){
			String status = mWheelphone.isUSBConnected() ? "Connected" : "Disconnected";
			output.setText(status + ". L: " + leftSpeed + ", R: " + rightSpeed);
		}
	}
	
	public void showError(String error) {
		if (output != null){
			output.setText(error);
		}
	}


	public void setSpeed(int leftSpeed, int rightSpeed) {
		mWheelphone.setSpeed(leftSpeed, rightSpeed);
		updateOutput(leftSpeed, rightSpeed);
	}

	/*
	 * GLUE CODE:
	 * To faceExpression
	 */
	public void changeExpression(int expression) {
		mFaceExpression.changeExpression(expression);
	}

	public void setEyesPosition(int x, int y) {
		mFaceExpression.setPupilsPosition(x, y);
	}

}
