package com.wheelphone.navigator.helpers;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import android.util.Log;

import com.wheelphone.navigator.FragmentNavigator;


public class MotionTrackerOpenCV implements CvCameraViewListener2 {
	//TODO: Set autofocus for preview

	private static final String TAG = MotionTrackerOpenCV.class.getName();
	
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

	private MotionTrackerNativeWrapper mNativeMotionTrackerWrapper;

	private Mat mGray;
	private Mat mOutput;
	private float mRotation;
	private FragmentNavigator mController; 
	
	public MotionTrackerOpenCV(FragmentNavigator controller){
		mController = controller;
	}
	
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mGray = inputFrame.gray();
    	mOutput = mGray.clone();

        long start = System.currentTimeMillis();
    	
    	//TODO: CONTROL THE FPS, AS THE LESS FRAMES THE MORE SENSIBLE TO MOVEMENT
        mRotation = mNativeMotionTrackerWrapper.process(inputFrame.rgba(), mGray, mOutput);
        if (mController.isMoving())
        	mController.setSpeed((int)(20 + 5*mRotation), (int)(20 - 5*mRotation));
        Log.d(TAG, "rotation: " + mRotation);
        
        long end = System.currentTimeMillis(); 
        Log.d(TAG, "time : " + (end-start) + ". At: " + mGray.width() + "x" + mGray.height());
        

        return mOutput;
    }

	@Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mOutput = new Mat();
        mNativeMotionTrackerWrapper = new MotionTrackerNativeWrapper();//OpenCV can't get the right rotation!!! TODO: Do this manually!
//        mNativeMotionTrackerWrapper.setMovementSensibility(120);
	}

	@Override
    public void onCameraViewStopped() {
        mGray.release();
        mOutput.release();
        mNativeMotionTrackerWrapper.release();
    }
	
}